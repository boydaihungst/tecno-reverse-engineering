package android.view;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.style.AccessibilityClickableSpan;
import android.text.style.ClickableSpan;
import android.util.LongSparseArray;
import android.util.Slog;
import android.view.accessibility.AccessibilityInteractionClient;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeIdManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.accessibility.AccessibilityRequestPreparer;
import android.view.accessibility.IAccessibilityInteractionConnectionCallback;
import com.android.internal.os.SomeArgs;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Predicate;
/* loaded from: classes3.dex */
public final class AccessibilityInteractionController {
    private static final boolean CONSIDER_REQUEST_PREPARERS = false;
    private static final boolean ENFORCE_NODE_TREE_CONSISTENT = false;
    private static final int FLAGS_AFFECTING_REPORTED_DATA = 384;
    private static final boolean IGNORE_REQUEST_PREPARERS = true;
    private static final String LOG_TAG = "AccessibilityInteractionController";
    private static final long REQUEST_PREPARER_TIMEOUT_MS = 500;
    private final AccessibilityManager mA11yManager;
    private int mActiveRequestPreparerId;
    private AddNodeInfosForViewId mAddNodeInfosForViewId;
    private final PrivateHandler mHandler;
    private List<MessageHolder> mMessagesWaitingForRequestPreparer;
    private final long mMyLooperThreadId;
    private final int mMyProcessId;
    private int mNumActiveRequestPreparers;
    private ArrayList<Message> mPendingFindNodeByIdMessages;
    private final AccessibilityNodePrefetcher mPrefetcher;
    private final ViewRootImpl mViewRootImpl;
    private final ArrayList<AccessibilityNodeInfo> mTempAccessibilityNodeInfoList = new ArrayList<>();
    private final Object mLock = new Object();
    private final ArrayList<View> mTempArrayList = new ArrayList<>();
    private final Rect mTempRect = new Rect();
    private final RectF mTempRectF = new RectF();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public interface DequeNode {
        void addChildren(AccessibilityNodeInfo accessibilityNodeInfo, PrefetchDeque prefetchDeque);

        AccessibilityNodeInfo getA11yNodeInfo();
    }

    public AccessibilityInteractionController(ViewRootImpl viewRootImpl) {
        Looper looper = viewRootImpl.mHandler.getLooper();
        this.mMyLooperThreadId = looper.getThread().getId();
        this.mMyProcessId = Process.myPid();
        this.mHandler = new PrivateHandler(looper);
        this.mViewRootImpl = viewRootImpl;
        this.mPrefetcher = new AccessibilityNodePrefetcher();
        this.mA11yManager = (AccessibilityManager) viewRootImpl.mContext.getSystemService(AccessibilityManager.class);
        this.mPendingFindNodeByIdMessages = new ArrayList<>();
    }

    private void scheduleMessage(Message message, int interrogatingPid, long interrogatingTid, boolean ignoreRequestPreparers) {
        if (ignoreRequestPreparers || !holdOffMessageIfNeeded(message, interrogatingPid, interrogatingTid)) {
            if (interrogatingPid == this.mMyProcessId && interrogatingTid == this.mMyLooperThreadId && this.mHandler.hasAccessibilityCallback(message)) {
                AccessibilityInteractionClient.getInstanceForThread(interrogatingTid).setSameThreadMessage(message);
            } else if (!this.mHandler.hasAccessibilityCallback(message) && Thread.currentThread().getId() == this.mMyLooperThreadId) {
                this.mHandler.handleMessage(message);
            } else {
                this.mHandler.sendMessage(message);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isShown(View view) {
        return view != null && view.getWindowVisibility() == 0 && view.isShown();
    }

    public void findAccessibilityNodeInfoByAccessibilityIdClientThread(long accessibilityNodeId, Region interactiveRegion, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec, float[] matrixValues, Bundle arguments) {
        Message message = this.mHandler.obtainMessage();
        message.what = 2;
        message.arg1 = flags;
        SomeArgs args = SomeArgs.obtain();
        args.argi1 = AccessibilityNodeInfo.getAccessibilityViewId(accessibilityNodeId);
        args.argi2 = AccessibilityNodeInfo.getVirtualDescendantId(accessibilityNodeId);
        args.argi3 = interactionId;
        args.arg1 = callback;
        args.arg2 = spec;
        args.arg3 = interactiveRegion;
        args.arg4 = arguments;
        args.arg5 = matrixValues;
        message.obj = args;
        synchronized (this.mLock) {
            try {
                try {
                    this.mPendingFindNodeByIdMessages.add(message);
                    scheduleMessage(message, interrogatingPid, interrogatingTid, false);
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    private boolean holdOffMessageIfNeeded(Message originalMessage, int callingPid, long callingTid) {
        synchronized (this.mLock) {
            if (this.mNumActiveRequestPreparers != 0) {
                queueMessageToHandleOncePrepared(originalMessage, callingPid, callingTid);
                return true;
            } else if (originalMessage.what != 2) {
                return false;
            } else {
                SomeArgs originalMessageArgs = (SomeArgs) originalMessage.obj;
                Bundle requestArguments = (Bundle) originalMessageArgs.arg4;
                if (requestArguments == null) {
                    return false;
                }
                int accessibilityViewId = originalMessageArgs.argi1;
                List<AccessibilityRequestPreparer> preparers = this.mA11yManager.getRequestPreparersForAccessibilityId(accessibilityViewId);
                if (preparers == null) {
                    return false;
                }
                String extraDataKey = requestArguments.getString(AccessibilityNodeInfo.EXTRA_DATA_REQUESTED_KEY);
                if (extraDataKey == null) {
                    return false;
                }
                this.mNumActiveRequestPreparers = preparers.size();
                for (int i = 0; i < preparers.size(); i++) {
                    Message requestPreparerMessage = this.mHandler.obtainMessage(7);
                    SomeArgs requestPreparerArgs = SomeArgs.obtain();
                    requestPreparerArgs.argi1 = originalMessageArgs.argi2 == Integer.MAX_VALUE ? -1 : originalMessageArgs.argi2;
                    requestPreparerArgs.arg1 = preparers.get(i);
                    requestPreparerArgs.arg2 = extraDataKey;
                    requestPreparerArgs.arg3 = requestArguments;
                    Message preparationFinishedMessage = this.mHandler.obtainMessage(8);
                    int i2 = this.mActiveRequestPreparerId + 1;
                    this.mActiveRequestPreparerId = i2;
                    preparationFinishedMessage.arg1 = i2;
                    requestPreparerArgs.arg4 = preparationFinishedMessage;
                    requestPreparerMessage.obj = requestPreparerArgs;
                    scheduleMessage(requestPreparerMessage, callingPid, callingTid, true);
                    this.mHandler.obtainMessage(9);
                    this.mHandler.sendEmptyMessageDelayed(9, REQUEST_PREPARER_TIMEOUT_MS);
                }
                queueMessageToHandleOncePrepared(originalMessage, callingPid, callingTid);
                return true;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void prepareForExtraDataRequestUiThread(Message message) {
        SomeArgs args = (SomeArgs) message.obj;
        int virtualDescendantId = args.argi1;
        AccessibilityRequestPreparer preparer = (AccessibilityRequestPreparer) args.arg1;
        String extraDataKey = (String) args.arg2;
        Bundle requestArguments = (Bundle) args.arg3;
        Message preparationFinishedMessage = (Message) args.arg4;
        preparer.onPrepareExtraData(virtualDescendantId, extraDataKey, requestArguments, preparationFinishedMessage);
    }

    private void queueMessageToHandleOncePrepared(Message message, int interrogatingPid, long interrogatingTid) {
        if (this.mMessagesWaitingForRequestPreparer == null) {
            this.mMessagesWaitingForRequestPreparer = new ArrayList(1);
        }
        MessageHolder messageHolder = new MessageHolder(message, interrogatingPid, interrogatingTid);
        this.mMessagesWaitingForRequestPreparer.add(messageHolder);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void requestPreparerDoneUiThread(Message message) {
        synchronized (this.mLock) {
            if (message.arg1 != this.mActiveRequestPreparerId) {
                Slog.e(LOG_TAG, "Surprising AccessibilityRequestPreparer callback (likely late)");
                return;
            }
            int i = this.mNumActiveRequestPreparers - 1;
            this.mNumActiveRequestPreparers = i;
            if (i <= 0) {
                this.mHandler.removeMessages(9);
                scheduleAllMessagesWaitingForRequestPreparerLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void requestPreparerTimeoutUiThread() {
        synchronized (this.mLock) {
            Slog.e(LOG_TAG, "AccessibilityRequestPreparer timed out");
            scheduleAllMessagesWaitingForRequestPreparerLocked();
        }
    }

    private void scheduleAllMessagesWaitingForRequestPreparerLocked() {
        int numMessages = this.mMessagesWaitingForRequestPreparer.size();
        int i = 0;
        while (true) {
            boolean z = false;
            if (i < numMessages) {
                MessageHolder request = this.mMessagesWaitingForRequestPreparer.get(i);
                Message message = request.mMessage;
                int i2 = request.mInterrogatingPid;
                long j = request.mInterrogatingTid;
                if (i == 0) {
                    z = true;
                }
                scheduleMessage(message, i2, j, z);
                i++;
            } else {
                this.mMessagesWaitingForRequestPreparer.clear();
                this.mNumActiveRequestPreparers = 0;
                this.mActiveRequestPreparerId = -1;
                return;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [391=6, 377=5] */
    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:39:0x00d6  */
    /* JADX WARN: Removed duplicated region for block: B:47:0x0100  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void findAccessibilityNodeInfoByAccessibilityIdUiThread(Message message) {
        boolean interruptPrefetch;
        AccessibilityNodeInfo requestedNode;
        float[] matrixValues;
        Region interactiveRegion;
        ArrayList<AccessibilityNodeInfo> infos;
        MagnificationSpec spec;
        AccessibilityNodeInfo accessibilityNodeInfo;
        float[] matrixValues2;
        int i;
        boolean interruptPrefetch2;
        ArrayList<AccessibilityNodeInfo> infos2;
        MagnificationSpec spec2;
        Region interactiveRegion2;
        float[] matrixValues3;
        synchronized (this.mLock) {
            this.mPendingFindNodeByIdMessages.remove(message);
        }
        int flags = message.arg1;
        SomeArgs args = (SomeArgs) message.obj;
        int accessibilityViewId = args.argi1;
        int virtualDescendantId = args.argi2;
        int interactionId = args.argi3;
        IAccessibilityInteractionConnectionCallback callback = (IAccessibilityInteractionConnectionCallback) args.arg1;
        MagnificationSpec spec3 = (MagnificationSpec) args.arg2;
        Region interactiveRegion3 = (Region) args.arg3;
        Bundle arguments = (Bundle) args.arg4;
        float[] matrixValues4 = (float[]) args.arg5;
        args.recycle();
        AccessibilityNodeInfo requestedNode2 = null;
        boolean interruptPrefetch3 = (flags & 32) == 0;
        ArrayList<AccessibilityNodeInfo> infos3 = this.mTempAccessibilityNodeInfoList;
        infos3.clear();
        try {
            if (this.mViewRootImpl.mView != null) {
                try {
                    if (this.mViewRootImpl.mAttachInfo == null) {
                        interruptPrefetch2 = interruptPrefetch3;
                        infos2 = infos3;
                        spec2 = spec3;
                        interactiveRegion2 = interactiveRegion3;
                        matrixValues3 = matrixValues4;
                    } else {
                        this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = flags;
                        View requestedView = findViewByAccessibilityId(accessibilityViewId);
                        if (requestedView != null) {
                            try {
                                if (isShown(requestedView)) {
                                    requestedNode2 = populateAccessibilityNodeInfoForView(requestedView, arguments, virtualDescendantId);
                                    this.mPrefetcher.mInterruptPrefetch = interruptPrefetch3;
                                    matrixValues2 = matrixValues4;
                                    try {
                                        this.mPrefetcher.mFetchFlags = flags & 63;
                                        if (interruptPrefetch3) {
                                            i = 0;
                                        } else {
                                            infos3.add(requestedNode2);
                                            this.mPrefetcher.prefetchAccessibilityNodeInfos(requestedView, requestedNode2 == null ? null : new AccessibilityNodeInfo(requestedNode2), infos3);
                                            i = 0;
                                            this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
                                        }
                                        AccessibilityNodeInfo requestedNode3 = requestedNode2;
                                        if (interruptPrefetch3) {
                                            updateInfosForViewportAndReturnFindNodeResult(infos3, callback, interactionId, spec3, matrixValues2, interactiveRegion3);
                                            SatisfiedFindAccessibilityNodeByAccessibilityIdRequest satisfiedRequest = getSatisfiedRequestInPrefetch(requestedNode3 == null ? null : requestedNode3, infos3, flags);
                                            if (satisfiedRequest != null) {
                                                returnFindNodeResult(satisfiedRequest);
                                                return;
                                            }
                                            return;
                                        }
                                        float[] matrixValues5 = matrixValues2;
                                        int i2 = i;
                                        updateInfoForViewportAndReturnFindNodeResult(requestedNode3 == null ? null : new AccessibilityNodeInfo(requestedNode3), callback, interactionId, spec3, matrixValues5, interactiveRegion3);
                                        this.mPrefetcher.prefetchAccessibilityNodeInfos(requestedView, requestedNode3 == null ? null : new AccessibilityNodeInfo(requestedNode3), infos3);
                                        this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = i2;
                                        updateInfosForViewPort(infos3, spec3, matrixValues5, interactiveRegion3);
                                        SatisfiedFindAccessibilityNodeByAccessibilityIdRequest satisfiedRequest2 = getSatisfiedRequestInPrefetch(requestedNode3 == null ? null : requestedNode3, infos3, flags);
                                        returnPrefetchResult(interactionId, infos3, callback);
                                        if (satisfiedRequest2 != null) {
                                            returnFindNodeResult(satisfiedRequest2);
                                            return;
                                        }
                                        return;
                                    } catch (Throwable th) {
                                        th = th;
                                        interruptPrefetch = interruptPrefetch3;
                                        interactiveRegion = interactiveRegion3;
                                        matrixValues = matrixValues2;
                                        requestedNode = requestedNode2;
                                        spec = spec3;
                                        accessibilityNodeInfo = null;
                                        infos = infos3;
                                        if (interruptPrefetch) {
                                            updateInfoForViewportAndReturnFindNodeResult(requestedNode == null ? accessibilityNodeInfo : new AccessibilityNodeInfo(requestedNode), callback, interactionId, spec, matrixValues, interactiveRegion);
                                            throw th;
                                        }
                                        AccessibilityNodeInfo accessibilityNodeInfo2 = accessibilityNodeInfo;
                                        updateInfosForViewportAndReturnFindNodeResult(infos, callback, interactionId, spec, matrixValues, interactiveRegion);
                                        SatisfiedFindAccessibilityNodeByAccessibilityIdRequest satisfiedRequest3 = getSatisfiedRequestInPrefetch(requestedNode == null ? accessibilityNodeInfo2 : requestedNode, infos, flags);
                                        if (satisfiedRequest3 != null) {
                                            returnFindNodeResult(satisfiedRequest3);
                                            return;
                                        }
                                        return;
                                    }
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                interruptPrefetch = interruptPrefetch3;
                                matrixValues = matrixValues4;
                                interactiveRegion = interactiveRegion3;
                                infos = infos3;
                                requestedNode = requestedNode2;
                                spec = spec3;
                                accessibilityNodeInfo = null;
                            }
                        }
                        matrixValues2 = matrixValues4;
                        i = 0;
                        AccessibilityNodeInfo requestedNode32 = requestedNode2;
                        if (interruptPrefetch3) {
                        }
                    }
                } catch (Throwable th3) {
                    th = th3;
                    interruptPrefetch = interruptPrefetch3;
                    infos = infos3;
                    spec = spec3;
                    requestedNode = null;
                    matrixValues = matrixValues4;
                    interactiveRegion = interactiveRegion3;
                    accessibilityNodeInfo = null;
                }
            } else {
                interruptPrefetch2 = interruptPrefetch3;
                infos2 = infos3;
                spec2 = spec3;
                interactiveRegion2 = interactiveRegion3;
                matrixValues3 = matrixValues4;
            }
            if (interruptPrefetch2) {
                updateInfoForViewportAndReturnFindNodeResult(0 == 0 ? null : new AccessibilityNodeInfo((AccessibilityNodeInfo) null), callback, interactionId, spec2, matrixValues3, interactiveRegion2);
                return;
            }
            updateInfosForViewportAndReturnFindNodeResult(infos2, callback, interactionId, spec2, matrixValues3, interactiveRegion2);
            SatisfiedFindAccessibilityNodeByAccessibilityIdRequest satisfiedRequest4 = getSatisfiedRequestInPrefetch(null, infos2, flags);
            if (satisfiedRequest4 != null) {
                returnFindNodeResult(satisfiedRequest4);
            }
        } catch (Throwable th4) {
            th = th4;
            interruptPrefetch = interruptPrefetch3;
            requestedNode = null;
            matrixValues = matrixValues4;
            interactiveRegion = interactiveRegion3;
            infos = infos3;
            spec = spec3;
            accessibilityNodeInfo = null;
        }
    }

    private AccessibilityNodeInfo populateAccessibilityNodeInfoForView(View view, Bundle arguments, int virtualViewId) {
        AccessibilityNodeInfo root;
        AccessibilityNodeProvider provider = view.getAccessibilityNodeProvider();
        String extraDataRequested = arguments == null ? null : arguments.getString(AccessibilityNodeInfo.EXTRA_DATA_REQUESTED_KEY);
        if (provider == null) {
            root = view.createAccessibilityNodeInfo();
            if (root != null && extraDataRequested != null) {
                view.addExtraDataToAccessibilityNodeInfo(root, extraDataRequested, arguments);
            }
        } else {
            root = provider.createAccessibilityNodeInfo(virtualViewId);
            if (root != null && extraDataRequested != null) {
                provider.addExtraDataToAccessibilityNodeInfo(virtualViewId, root, extraDataRequested, arguments);
            }
        }
        return root;
    }

    public void findAccessibilityNodeInfosByViewIdClientThread(long accessibilityNodeId, String viewId, Region interactiveRegion, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec, float[] matrixValues) {
        Message message = this.mHandler.obtainMessage();
        message.what = 3;
        message.arg1 = flags;
        message.arg2 = AccessibilityNodeInfo.getAccessibilityViewId(accessibilityNodeId);
        SomeArgs args = SomeArgs.obtain();
        args.argi1 = interactionId;
        args.arg1 = callback;
        args.arg2 = spec;
        args.arg3 = viewId;
        args.arg4 = interactiveRegion;
        args.arg5 = matrixValues;
        message.obj = args;
        scheduleMessage(message, interrogatingPid, interrogatingTid, false);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [496=5, 497=4] */
    /* JADX INFO: Access modifiers changed from: private */
    public void findAccessibilityNodeInfosByViewIdUiThread(Message message) {
        List<AccessibilityNodeInfo> infos;
        List<AccessibilityNodeInfo> infos2;
        int flags = message.arg1;
        int accessibilityViewId = message.arg2;
        SomeArgs args = (SomeArgs) message.obj;
        int interactionId = args.argi1;
        IAccessibilityInteractionConnectionCallback callback = (IAccessibilityInteractionConnectionCallback) args.arg1;
        MagnificationSpec spec = (MagnificationSpec) args.arg2;
        String viewId = (String) args.arg3;
        Region interactiveRegion = (Region) args.arg4;
        float[] matrixValues = (float[]) args.arg5;
        args.recycle();
        List<AccessibilityNodeInfo> infos3 = this.mTempAccessibilityNodeInfoList;
        infos3.clear();
        try {
            if (this.mViewRootImpl.mView == null || this.mViewRootImpl.mAttachInfo == null) {
                infos2 = infos3;
            } else if (viewId != null) {
                this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = flags;
                View root = findViewByAccessibilityId(accessibilityViewId);
                if (root != null) {
                    int resolvedViewId = root.getContext().getResources().getIdentifier(viewId, null, null);
                    if (resolvedViewId <= 0) {
                        this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
                        updateInfosForViewportAndReturnFindNodeResult(infos3, callback, interactionId, spec, matrixValues, interactiveRegion);
                        return;
                    }
                    infos = infos3;
                    try {
                        if (this.mAddNodeInfosForViewId == null) {
                            this.mAddNodeInfosForViewId = new AddNodeInfosForViewId();
                        }
                        this.mAddNodeInfosForViewId.init(resolvedViewId, infos);
                        root.findViewByPredicate(this.mAddNodeInfosForViewId);
                        this.mAddNodeInfosForViewId.reset();
                    } catch (Throwable th) {
                        th = th;
                        this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
                        updateInfosForViewportAndReturnFindNodeResult(infos, callback, interactionId, spec, matrixValues, interactiveRegion);
                        throw th;
                    }
                } else {
                    infos = infos3;
                }
                this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
                updateInfosForViewportAndReturnFindNodeResult(infos, callback, interactionId, spec, matrixValues, interactiveRegion);
                return;
            } else {
                infos2 = infos3;
            }
            this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
            updateInfosForViewportAndReturnFindNodeResult(infos2, callback, interactionId, spec, matrixValues, interactiveRegion);
        } catch (Throwable th2) {
            th = th2;
            infos = infos3;
        }
    }

    public void findAccessibilityNodeInfosByTextClientThread(long accessibilityNodeId, String text, Region interactiveRegion, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec, float[] matrixValues) {
        Message message = this.mHandler.obtainMessage();
        message.what = 4;
        message.arg1 = flags;
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = text;
        args.arg2 = callback;
        args.arg3 = spec;
        args.argi1 = AccessibilityNodeInfo.getAccessibilityViewId(accessibilityNodeId);
        args.argi2 = AccessibilityNodeInfo.getVirtualDescendantId(accessibilityNodeId);
        args.argi3 = interactionId;
        args.arg4 = interactiveRegion;
        args.arg5 = matrixValues;
        message.obj = args;
        scheduleMessage(message, interrogatingPid, interrogatingTid, false);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [581=5] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:54:0x004f */
    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r22v0, types: [android.view.AccessibilityInteractionController] */
    /* JADX WARN: Type inference failed for: r6v0, types: [int] */
    /* JADX WARN: Type inference failed for: r6v3 */
    /* JADX WARN: Type inference failed for: r6v4, types: [float[]] */
    /* JADX WARN: Type inference failed for: r7v0, types: [int] */
    /* JADX WARN: Type inference failed for: r7v3 */
    /* JADX WARN: Type inference failed for: r7v4, types: [android.graphics.Region] */
    public void findAccessibilityNodeInfosByTextUiThread(Message message) {
        int interactionId;
        int interactionId2;
        int viewCount;
        int flags = message.arg1;
        SomeArgs args = (SomeArgs) message.obj;
        String text = (String) args.arg1;
        IAccessibilityInteractionConnectionCallback callback = (IAccessibilityInteractionConnectionCallback) args.arg2;
        MagnificationSpec spec = (MagnificationSpec) args.arg3;
        int accessibilityViewId = args.argi1;
        ?? r7 = args.argi2;
        ?? r6 = args.argi3;
        Region interactiveRegion = (Region) args.arg4;
        float[] matrixValues = (float[]) args.arg5;
        args.recycle();
        List<AccessibilityNodeInfo> infos = null;
        try {
            if (this.mViewRootImpl.mView == null) {
                interactionId2 = r6;
            } else if (this.mViewRootImpl.mAttachInfo != null) {
                this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = flags;
                View root = findViewByAccessibilityId(accessibilityViewId);
                if (root != null) {
                    try {
                        if (isShown(root)) {
                            AccessibilityNodeProvider provider = root.getAccessibilityNodeProvider();
                            if (provider != null) {
                                infos = provider.findAccessibilityNodeInfosByText(text, r7);
                            } else if (r7 == -1) {
                                ArrayList<View> foundViews = this.mTempArrayList;
                                foundViews.clear();
                                root.findViewsWithText(foundViews, text, 7);
                                if (!foundViews.isEmpty()) {
                                    List<AccessibilityNodeInfo> infos2 = this.mTempAccessibilityNodeInfoList;
                                    try {
                                        infos2.clear();
                                        int viewCount2 = foundViews.size();
                                        int i = 0;
                                        while (true) {
                                            int viewCount3 = viewCount2;
                                            if (i >= viewCount3) {
                                                break;
                                            }
                                            View foundView = foundViews.get(i);
                                            View root2 = root;
                                            if (isShown(foundView)) {
                                                AccessibilityNodeProvider provider2 = foundView.getAccessibilityNodeProvider();
                                                if (provider2 != null) {
                                                    viewCount = viewCount3;
                                                    List<AccessibilityNodeInfo> infosFromProvider = provider2.findAccessibilityNodeInfosByText(text, -1);
                                                    if (infosFromProvider != null) {
                                                        infos2.addAll(infosFromProvider);
                                                    }
                                                } else {
                                                    viewCount = viewCount3;
                                                    infos2.add(foundView.createAccessibilityNodeInfo());
                                                }
                                            } else {
                                                viewCount = viewCount3;
                                            }
                                            i++;
                                            viewCount2 = viewCount;
                                            root = root2;
                                        }
                                        infos = infos2;
                                    } catch (Throwable th) {
                                        th = th;
                                        infos = infos2;
                                        interactionId = r6;
                                        this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
                                        updateInfosForViewportAndReturnFindNodeResult(infos, callback, interactionId, spec, matrixValues, interactiveRegion);
                                        throw th;
                                    }
                                }
                            }
                            this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
                            r6 = matrixValues;
                            r7 = interactiveRegion;
                            updateInfosForViewportAndReturnFindNodeResult(infos, callback, r6, spec, r6, r7);
                            return;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        interactionId = r6;
                    }
                }
                this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
                r6 = matrixValues;
                r7 = interactiveRegion;
                updateInfosForViewportAndReturnFindNodeResult(infos, callback, r6, spec, r6, r7);
                return;
            } else {
                interactionId2 = r6;
            }
            this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
            updateInfosForViewportAndReturnFindNodeResult(null, callback, interactionId2, spec, matrixValues, interactiveRegion);
        } catch (Throwable th3) {
            th = th3;
            interactionId = r6;
        }
    }

    public void findFocusClientThread(long accessibilityNodeId, int focusType, Region interactiveRegion, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec, float[] matrixValues) {
        Message message = this.mHandler.obtainMessage();
        message.what = 5;
        message.arg1 = flags;
        message.arg2 = focusType;
        SomeArgs args = SomeArgs.obtain();
        args.argi1 = interactionId;
        args.argi2 = AccessibilityNodeInfo.getAccessibilityViewId(accessibilityNodeId);
        args.argi3 = AccessibilityNodeInfo.getVirtualDescendantId(accessibilityNodeId);
        args.arg1 = callback;
        args.arg2 = spec;
        args.arg3 = interactiveRegion;
        args.arg4 = matrixValues;
        message.obj = args;
        scheduleMessage(message, interrogatingPid, interrogatingTid, false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void findFocusUiThread(Message message) {
        int flags = message.arg1;
        int focusType = message.arg2;
        SomeArgs args = (SomeArgs) message.obj;
        int interactionId = args.argi1;
        int accessibilityViewId = args.argi2;
        int virtualDescendantId = args.argi3;
        IAccessibilityInteractionConnectionCallback callback = (IAccessibilityInteractionConnectionCallback) args.arg1;
        MagnificationSpec spec = (MagnificationSpec) args.arg2;
        Region interactiveRegion = (Region) args.arg3;
        float[] matrixValues = (float[]) args.arg4;
        args.recycle();
        AccessibilityNodeInfo focused = null;
        try {
            if (this.mViewRootImpl.mView != null && this.mViewRootImpl.mAttachInfo != null) {
                this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = flags;
                View root = findViewByAccessibilityId(accessibilityViewId);
                if (root != null && isShown(root)) {
                    switch (focusType) {
                        case 1:
                            View target = root.findFocus();
                            if (!isShown(target)) {
                                break;
                            } else {
                                AccessibilityNodeProvider provider = target.getAccessibilityNodeProvider();
                                if (provider != null) {
                                    focused = provider.findFocus(focusType);
                                }
                                if (focused == null) {
                                    focused = target.createAccessibilityNodeInfo();
                                }
                                break;
                            }
                        case 2:
                            View host = this.mViewRootImpl.mAccessibilityFocusedHost;
                            if (host != null && ViewRootImpl.isViewDescendantOf(host, root) && isShown(host)) {
                                AccessibilityNodeProvider provider2 = host.getAccessibilityNodeProvider();
                                if (provider2 != null) {
                                    AccessibilityNodeInfo focusNode = this.mViewRootImpl.mAccessibilityFocusedVirtualView;
                                    if (focusNode != null) {
                                        int virtualNodeId = AccessibilityNodeInfo.getVirtualDescendantId(focusNode.getSourceNodeId());
                                        focused = provider2.createAccessibilityNodeInfo(virtualNodeId);
                                    }
                                } else if (virtualDescendantId == -1) {
                                    focused = host.createAccessibilityNodeInfo();
                                }
                                break;
                            }
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown focus type: " + focusType);
                    }
                }
                this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
                updateInfoForViewportAndReturnFindNodeResult(focused, callback, interactionId, spec, matrixValues, interactiveRegion);
            }
        } finally {
            this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
            updateInfoForViewportAndReturnFindNodeResult(null, callback, interactionId, spec, matrixValues, interactiveRegion);
        }
    }

    public void focusSearchClientThread(long accessibilityNodeId, int direction, Region interactiveRegion, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec, float[] matrixValues) {
        Message message = this.mHandler.obtainMessage();
        message.what = 6;
        message.arg1 = flags;
        message.arg2 = AccessibilityNodeInfo.getAccessibilityViewId(accessibilityNodeId);
        SomeArgs args = SomeArgs.obtain();
        args.argi2 = direction;
        args.argi3 = interactionId;
        args.arg1 = callback;
        args.arg2 = spec;
        args.arg3 = interactiveRegion;
        args.arg4 = matrixValues;
        message.obj = args;
        scheduleMessage(message, interrogatingPid, interrogatingTid, false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void focusSearchUiThread(Message message) {
        View nextView;
        int flags = message.arg1;
        int accessibilityViewId = message.arg2;
        SomeArgs args = (SomeArgs) message.obj;
        int direction = args.argi2;
        int interactionId = args.argi3;
        IAccessibilityInteractionConnectionCallback callback = (IAccessibilityInteractionConnectionCallback) args.arg1;
        MagnificationSpec spec = (MagnificationSpec) args.arg2;
        Region interactiveRegion = (Region) args.arg3;
        float[] matrixValues = (float[]) args.arg4;
        args.recycle();
        AccessibilityNodeInfo next = null;
        try {
            if (this.mViewRootImpl.mView != null && this.mViewRootImpl.mAttachInfo != null) {
                this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = flags;
                View root = findViewByAccessibilityId(accessibilityViewId);
                if (root != null && isShown(root) && (nextView = root.focusSearch(direction)) != null) {
                    next = nextView.createAccessibilityNodeInfo();
                }
                this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
                updateInfoForViewportAndReturnFindNodeResult(next, callback, interactionId, spec, matrixValues, interactiveRegion);
            }
        } finally {
            this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
            updateInfoForViewportAndReturnFindNodeResult(null, callback, interactionId, spec, matrixValues, interactiveRegion);
        }
    }

    public void performAccessibilityActionClientThread(long accessibilityNodeId, int action, Bundle arguments, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid) {
        Message message = this.mHandler.obtainMessage();
        message.what = 1;
        message.arg1 = flags;
        message.arg2 = AccessibilityNodeInfo.getAccessibilityViewId(accessibilityNodeId);
        SomeArgs args = SomeArgs.obtain();
        args.argi1 = AccessibilityNodeInfo.getVirtualDescendantId(accessibilityNodeId);
        args.argi2 = action;
        args.argi3 = interactionId;
        args.arg1 = callback;
        args.arg2 = arguments;
        message.obj = args;
        scheduleMessage(message, interrogatingPid, interrogatingTid, false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void performAccessibilityActionUiThread(Message message) {
        int flags = message.arg1;
        int accessibilityViewId = message.arg2;
        SomeArgs args = (SomeArgs) message.obj;
        int virtualDescendantId = args.argi1;
        int action = args.argi2;
        int interactionId = args.argi3;
        IAccessibilityInteractionConnectionCallback callback = (IAccessibilityInteractionConnectionCallback) args.arg1;
        Bundle arguments = (Bundle) args.arg2;
        args.recycle();
        boolean succeeded = false;
        try {
            if (this.mViewRootImpl.mView != null && this.mViewRootImpl.mAttachInfo != null && !this.mViewRootImpl.mStopped && !this.mViewRootImpl.mPausedForTransition) {
                this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = flags;
                View target = findViewByAccessibilityId(accessibilityViewId);
                if (target != null && isShown(target)) {
                    this.mA11yManager.notifyPerformingAction(action);
                    if (action == 16908692) {
                        succeeded = handleClickableSpanActionUiThread(target, virtualDescendantId, arguments);
                    } else {
                        AccessibilityNodeProvider provider = target.getAccessibilityNodeProvider();
                        if (provider != null) {
                            succeeded = provider.performAction(virtualDescendantId, action, arguments);
                        } else if (virtualDescendantId == -1) {
                            succeeded = target.performAccessibilityAction(action, arguments);
                        }
                    }
                    this.mA11yManager.notifyPerformingAction(0);
                }
                try {
                    this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
                    callback.setPerformAccessibilityActionResult(succeeded, interactionId);
                    return;
                } catch (RemoteException e) {
                    return;
                }
            }
            try {
                this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
                callback.setPerformAccessibilityActionResult(false, interactionId);
            } catch (RemoteException e2) {
            }
        } catch (Throwable th) {
            try {
                this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
                callback.setPerformAccessibilityActionResult(succeeded, interactionId);
            } catch (RemoteException e3) {
            }
            throw th;
        }
    }

    public void clearAccessibilityFocusClientThread() {
        Message message = this.mHandler.obtainMessage();
        message.what = 101;
        scheduleMessage(message, 0, 0L, false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clearAccessibilityFocusUiThread() {
        if (this.mViewRootImpl.mView == null || this.mViewRootImpl.mAttachInfo == null) {
            return;
        }
        try {
            this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 128;
            View root = this.mViewRootImpl.mView;
            if (root != null && isShown(root)) {
                View host = this.mViewRootImpl.mAccessibilityFocusedHost;
                if (host != null && ViewRootImpl.isViewDescendantOf(host, root)) {
                    AccessibilityNodeProvider provider = host.getAccessibilityNodeProvider();
                    AccessibilityNodeInfo focusNode = this.mViewRootImpl.mAccessibilityFocusedVirtualView;
                    if (provider != null && focusNode != null) {
                        int virtualNodeId = AccessibilityNodeInfo.getVirtualDescendantId(focusNode.getSourceNodeId());
                        provider.performAction(virtualNodeId, AccessibilityNodeInfo.AccessibilityAction.ACTION_CLEAR_ACCESSIBILITY_FOCUS.getId(), null);
                    } else {
                        host.performAccessibilityAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLEAR_ACCESSIBILITY_FOCUS.getId(), null);
                    }
                }
            }
        } finally {
            this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
        }
    }

    public void notifyOutsideTouchClientThread() {
        Message message = this.mHandler.obtainMessage();
        message.what = 102;
        scheduleMessage(message, 0, 0L, false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyOutsideTouchUiThread() {
        View root;
        if (this.mViewRootImpl.mView != null && this.mViewRootImpl.mAttachInfo != null && !this.mViewRootImpl.mStopped && !this.mViewRootImpl.mPausedForTransition && (root = this.mViewRootImpl.mView) != null && isShown(root)) {
            long now = SystemClock.uptimeMillis();
            MotionEvent event = MotionEvent.obtain(now, now, 4, 0.0f, 0.0f, 0);
            event.setSource(4098);
            this.mViewRootImpl.dispatchInputEvent(event);
        }
    }

    private View findViewByAccessibilityId(int accessibilityId) {
        if (accessibilityId == 2147483646) {
            return this.mViewRootImpl.mView;
        }
        return AccessibilityNodeIdManager.getInstance().findView(accessibilityId);
    }

    private void adjustIsVisibleToUserIfNeeded(AccessibilityNodeInfo info, Region interactiveRegion, MagnificationSpec spec) {
        if (interactiveRegion == null || info == null) {
            return;
        }
        Rect boundsInScreen = this.mTempRect;
        info.getBoundsInScreen(boundsInScreen);
        if (spec != null && !spec.isNop()) {
            boundsInScreen.offset((int) (-spec.offsetX), (int) (-spec.offsetY));
            boundsInScreen.scale(1.0f / spec.scale);
        }
        if (interactiveRegion.quickReject(boundsInScreen) && !shouldBypassAdjustIsVisible()) {
            info.setVisibleToUser(false);
        }
    }

    private boolean shouldBypassAdjustIsVisible() {
        int windowType = this.mViewRootImpl.mOrigWindowType;
        if (windowType == 2011) {
            return true;
        }
        return false;
    }

    private void applyHostWindowMatrixIfNeeded(AccessibilityNodeInfo info) {
        if (info == null || shouldBypassApplyWindowMatrix()) {
            return;
        }
        Rect boundsInScreen = this.mTempRect;
        RectF transformedBounds = this.mTempRectF;
        Matrix windowMatrix = this.mViewRootImpl.mAttachInfo.mWindowMatrixInEmbeddedHierarchy;
        info.getBoundsInScreen(boundsInScreen);
        transformedBounds.set(boundsInScreen);
        windowMatrix.mapRect(transformedBounds);
        boundsInScreen.set((int) transformedBounds.left, (int) transformedBounds.top, (int) transformedBounds.right, (int) transformedBounds.bottom);
        info.setBoundsInScreen(boundsInScreen);
    }

    private boolean shouldBypassApplyWindowMatrix() {
        Matrix windowMatrix = this.mViewRootImpl.mAttachInfo.mWindowMatrixInEmbeddedHierarchy;
        return windowMatrix == null || windowMatrix.isIdentity();
    }

    private void associateLeashedParentIfNeeded(AccessibilityNodeInfo info) {
        if (info == null || shouldBypassAssociateLeashedParent() || this.mViewRootImpl.mView.getAccessibilityViewId() != AccessibilityNodeInfo.getAccessibilityViewId(info.getSourceNodeId())) {
            return;
        }
        info.setLeashedParent(this.mViewRootImpl.mAttachInfo.mLeashedParentToken, this.mViewRootImpl.mAttachInfo.mLeashedParentAccessibilityViewId);
    }

    private boolean shouldBypassAssociateLeashedParent() {
        return this.mViewRootImpl.mAttachInfo.mLeashedParentToken == null && this.mViewRootImpl.mAttachInfo.mLeashedParentAccessibilityViewId == -1;
    }

    private void applyAppScaleAndMagnificationSpecIfNeeded(AccessibilityNodeInfo info, MagnificationSpec spec) {
        if (info == null) {
            return;
        }
        float applicationScale = this.mViewRootImpl.mAttachInfo.mApplicationScale;
        if (!shouldApplyAppScaleAndMagnificationSpec(applicationScale, spec)) {
            return;
        }
        Rect boundsInParent = this.mTempRect;
        info.getBoundsInParent(boundsInParent);
        if (applicationScale != 1.0f) {
            boundsInParent.scale(applicationScale);
        }
        if (spec != null) {
            boundsInParent.scale(spec.scale);
        }
        info.setBoundsInParent(boundsInParent);
    }

    private boolean shouldApplyAppScaleAndMagnificationSpec(float appScale, MagnificationSpec spec) {
        return (appScale == 1.0f && (spec == null || spec.isNop())) ? false : true;
    }

    private void updateInfosForViewPort(List<AccessibilityNodeInfo> infos, MagnificationSpec spec, float[] matrixValues, Region interactiveRegion) {
        for (int i = 0; i < infos.size(); i++) {
            updateInfoForViewPort(infos.get(i), spec, matrixValues, interactiveRegion);
        }
    }

    private void updateInfoForViewPort(AccessibilityNodeInfo info, MagnificationSpec spec, float[] matrixValues, Region interactiveRegion) {
        associateLeashedParentIfNeeded(info);
        applyHostWindowMatrixIfNeeded(info);
        transformBoundsWithScreenMatrix(info, matrixValues);
        adjustIsVisibleToUserIfNeeded(info, interactiveRegion, spec);
        applyAppScaleAndMagnificationSpecIfNeeded(info, spec);
    }

    private void transformBoundsWithScreenMatrix(AccessibilityNodeInfo info, float[] matrixValues) {
        if (info == null || matrixValues == null) {
            return;
        }
        Rect boundInScreen = this.mTempRect;
        RectF transformedBounds = this.mTempRectF;
        info.getBoundsInScreen(boundInScreen);
        transformedBounds.set(boundInScreen);
        Matrix transformMatrix = new Matrix();
        transformMatrix.setValues(matrixValues);
        float applicationScale = this.mViewRootImpl.mAttachInfo.mApplicationScale;
        if (applicationScale != 1.0f) {
            transformMatrix.preScale(applicationScale, applicationScale);
        }
        if (this.mViewRootImpl.mAttachInfo.mWindowMatrixInEmbeddedHierarchy == null) {
            transformMatrix.preTranslate(-this.mViewRootImpl.mAttachInfo.mWindowLeft, -this.mViewRootImpl.mAttachInfo.mWindowTop);
        }
        if (transformMatrix.isIdentity()) {
            return;
        }
        transformMatrix.mapRect(transformedBounds);
        transformedBounds.offset(0.5f, 0.5f);
        boundInScreen.set((int) transformedBounds.left, (int) transformedBounds.top, (int) transformedBounds.right, (int) transformedBounds.bottom);
        info.setBoundsInScreen(boundInScreen);
        if (info.hasExtras()) {
            Bundle extras = info.getExtras();
            RectF[] textLocations = (RectF[]) extras.getParcelableArray(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY, RectF.class);
            if (textLocations != null) {
                for (RectF textLocation : textLocations) {
                    if (textLocation != null) {
                        transformMatrix.mapRect(textLocation);
                    }
                }
            }
        }
    }

    private void updateInfosForViewportAndReturnFindNodeResult(List<AccessibilityNodeInfo> infos, IAccessibilityInteractionConnectionCallback callback, int interactionId, MagnificationSpec spec, float[] matrixValues, Region interactiveRegion) {
        if (infos != null) {
            updateInfosForViewPort(infos, spec, matrixValues, interactiveRegion);
        }
        returnFindNodesResult(infos, callback, interactionId);
    }

    private void returnFindNodeResult(AccessibilityNodeInfo info, IAccessibilityInteractionConnectionCallback callback, int interactionId) {
        try {
            callback.setFindAccessibilityNodeInfoResult(info, interactionId);
        } catch (RemoteException e) {
        }
    }

    private void returnFindNodeResult(SatisfiedFindAccessibilityNodeByAccessibilityIdRequest satisfiedRequest) {
        try {
            AccessibilityNodeInfo info = satisfiedRequest.mSatisfiedRequestNode;
            IAccessibilityInteractionConnectionCallback callback = satisfiedRequest.mSatisfiedRequestCallback;
            int interactionId = satisfiedRequest.mSatisfiedRequestInteractionId;
            callback.setFindAccessibilityNodeInfoResult(info, interactionId);
        } catch (RemoteException e) {
        }
    }

    private void returnFindNodesResult(List<AccessibilityNodeInfo> infos, IAccessibilityInteractionConnectionCallback callback, int interactionId) {
        try {
            callback.setFindAccessibilityNodeInfosResult(infos, interactionId);
            if (infos != null) {
                infos.clear();
            }
        } catch (RemoteException e) {
        }
    }

    private SatisfiedFindAccessibilityNodeByAccessibilityIdRequest getSatisfiedRequestInPrefetch(AccessibilityNodeInfo requestedNode, List<AccessibilityNodeInfo> infos, int flags) {
        SatisfiedFindAccessibilityNodeByAccessibilityIdRequest satisfiedRequest = null;
        synchronized (this.mLock) {
            int i = 0;
            while (true) {
                if (i >= this.mPendingFindNodeByIdMessages.size()) {
                    break;
                }
                Message pendingMessage = this.mPendingFindNodeByIdMessages.get(i);
                int pendingFlags = pendingMessage.arg1;
                if ((pendingFlags & 384) == (flags & 384)) {
                    SomeArgs args = (SomeArgs) pendingMessage.obj;
                    int accessibilityViewId = args.argi1;
                    int virtualDescendantId = args.argi2;
                    AccessibilityNodeInfo satisfiedRequestNode = nodeWithIdFromList(requestedNode, infos, AccessibilityNodeInfo.makeNodeId(accessibilityViewId, virtualDescendantId));
                    if (satisfiedRequestNode != null) {
                        this.mHandler.removeMessages(2, pendingMessage.obj);
                        IAccessibilityInteractionConnectionCallback satisfiedRequestCallback = (IAccessibilityInteractionConnectionCallback) args.arg1;
                        int satisfiedRequestInteractionId = args.argi3;
                        satisfiedRequest = new SatisfiedFindAccessibilityNodeByAccessibilityIdRequest(satisfiedRequestNode, satisfiedRequestCallback, satisfiedRequestInteractionId);
                        args.recycle();
                        break;
                    }
                }
                i++;
            }
            this.mPendingFindNodeByIdMessages.clear();
            if (satisfiedRequest != null && satisfiedRequest.mSatisfiedRequestNode != requestedNode) {
                infos.remove(satisfiedRequest.mSatisfiedRequestNode);
            }
        }
        return satisfiedRequest;
    }

    private AccessibilityNodeInfo nodeWithIdFromList(AccessibilityNodeInfo requestedNode, List<AccessibilityNodeInfo> infos, long nodeId) {
        if (requestedNode != null && requestedNode.getSourceNodeId() == nodeId) {
            return requestedNode;
        }
        for (int j = 0; j < infos.size(); j++) {
            AccessibilityNodeInfo info = infos.get(j);
            if (info.getSourceNodeId() == nodeId) {
                return info;
            }
        }
        return null;
    }

    private void returnPrefetchResult(int interactionId, List<AccessibilityNodeInfo> infos, IAccessibilityInteractionConnectionCallback callback) {
        if (infos.size() > 0) {
            try {
                callback.setPrefetchAccessibilityNodeInfoResult(infos, interactionId);
            } catch (RemoteException e) {
            }
        }
    }

    private void updateInfoForViewportAndReturnFindNodeResult(AccessibilityNodeInfo info, IAccessibilityInteractionConnectionCallback callback, int interactionId, MagnificationSpec spec, float[] matrixValues, Region interactiveRegion) {
        updateInfoForViewPort(info, spec, matrixValues, interactiveRegion);
        returnFindNodeResult(info, callback, interactionId);
    }

    private boolean handleClickableSpanActionUiThread(View view, int virtualDescendantId, Bundle arguments) {
        ClickableSpan clickableSpan;
        Parcelable span = arguments.getParcelable(AccessibilityNodeInfo.ACTION_ARGUMENT_ACCESSIBLE_CLICKABLE_SPAN);
        if (span instanceof AccessibilityClickableSpan) {
            AccessibilityNodeInfo infoWithSpan = null;
            AccessibilityNodeProvider provider = view.getAccessibilityNodeProvider();
            if (provider != null) {
                infoWithSpan = provider.createAccessibilityNodeInfo(virtualDescendantId);
            } else if (virtualDescendantId == -1) {
                infoWithSpan = view.createAccessibilityNodeInfo();
            }
            if (infoWithSpan == null || (clickableSpan = ((AccessibilityClickableSpan) span).findClickableSpan(infoWithSpan.getOriginalText())) == null) {
                return false;
            }
            clickableSpan.onClick(view);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public class AccessibilityNodePrefetcher {
        private int mFetchFlags;
        private boolean mInterruptPrefetch;
        private final ArrayList<View> mTempViewList;

        private AccessibilityNodePrefetcher() {
            this.mTempViewList = new ArrayList<>();
        }

        public void prefetchAccessibilityNodeInfos(View view, AccessibilityNodeInfo root, List<AccessibilityNodeInfo> outInfos) {
            if (root == null) {
                return;
            }
            AccessibilityNodeProvider provider = view.getAccessibilityNodeProvider();
            boolean prefetchPredecessors = isFlagSet(1);
            if (provider == null) {
                if (prefetchPredecessors) {
                    prefetchPredecessorsOfRealNode(view, outInfos);
                }
                if (isFlagSet(2)) {
                    prefetchSiblingsOfRealNode(view, outInfos, prefetchPredecessors);
                }
                if (isFlagSet(4)) {
                    prefetchDescendantsOfRealNode(view, outInfos);
                }
            } else {
                if (prefetchPredecessors) {
                    prefetchPredecessorsOfVirtualNode(root, view, provider, outInfos);
                }
                if (isFlagSet(2)) {
                    prefetchSiblingsOfVirtualNode(root, view, provider, outInfos, prefetchPredecessors);
                }
                if (isFlagSet(4)) {
                    prefetchDescendantsOfVirtualNode(root, provider, outInfos);
                }
            }
            if ((!isFlagSet(8) && !isFlagSet(16)) || shouldStopPrefetching(outInfos)) {
                return;
            }
            PrefetchDeque<DequeNode> deque = new PrefetchDeque<>(this.mFetchFlags & 28, outInfos);
            addChildrenOfRoot(view, root, provider, deque);
            deque.performTraversalAndPrefetch();
        }

        private void addChildrenOfRoot(View root, AccessibilityNodeInfo rootInfo, AccessibilityNodeProvider rootProvider, PrefetchDeque deque) {
            DequeNode rootDequeNode;
            if (rootProvider == null) {
                rootDequeNode = new ViewNode(root);
            } else {
                rootDequeNode = new VirtualNode(-1L, rootProvider);
            }
            rootDequeNode.addChildren(rootInfo, deque);
        }

        private boolean isFlagSet(int strategy) {
            return (this.mFetchFlags & strategy) != 0;
        }

        public boolean shouldStopPrefetching(List prefetchedInfos) {
            return (AccessibilityInteractionController.this.mHandler.hasUserInteractiveMessagesWaiting() && this.mInterruptPrefetch) || prefetchedInfos.size() >= 50;
        }

        private void enforceNodeTreeConsistent(AccessibilityNodeInfo root, List<AccessibilityNodeInfo> nodes) {
            LongSparseArray<AccessibilityNodeInfo> nodeMap = new LongSparseArray<>();
            int nodeCount = nodes.size();
            for (int i = 0; i < nodeCount; i++) {
                AccessibilityNodeInfo node = nodes.get(i);
                nodeMap.put(node.getSourceNodeId(), node);
            }
            for (AccessibilityNodeInfo parent = root; parent != null; parent = nodeMap.get(parent.getParentNodeId())) {
                root = parent;
            }
            AccessibilityNodeInfo accessFocus = null;
            AccessibilityNodeInfo inputFocus = null;
            HashSet<AccessibilityNodeInfo> seen = new HashSet<>();
            Queue<AccessibilityNodeInfo> fringe = new LinkedList<>();
            fringe.add(root);
            while (!fringe.isEmpty()) {
                AccessibilityNodeInfo current = fringe.poll();
                if (!seen.add(current)) {
                    throw new IllegalStateException("Duplicate node: " + current + " in window:" + AccessibilityInteractionController.this.mViewRootImpl.mAttachInfo.mAccessibilityWindowId);
                }
                if (current.isAccessibilityFocused()) {
                    if (accessFocus != null) {
                        throw new IllegalStateException("Duplicate accessibility focus:" + current + " in window:" + AccessibilityInteractionController.this.mViewRootImpl.mAttachInfo.mAccessibilityWindowId);
                    }
                    accessFocus = current;
                }
                if (current.isFocused()) {
                    if (inputFocus != null) {
                        throw new IllegalStateException("Duplicate input focus: " + current + " in window:" + AccessibilityInteractionController.this.mViewRootImpl.mAttachInfo.mAccessibilityWindowId);
                    }
                    inputFocus = current;
                }
                int childCount = current.getChildCount();
                for (int j = 0; j < childCount; j++) {
                    long childId = current.getChildId(j);
                    AccessibilityNodeInfo child = nodeMap.get(childId);
                    if (child != null) {
                        fringe.add(child);
                    }
                }
            }
            for (int j2 = nodeMap.size() - 1; j2 >= 0; j2--) {
                AccessibilityNodeInfo info = nodeMap.valueAt(j2);
                if (!seen.contains(info)) {
                    throw new IllegalStateException("Disconnected node: " + info);
                }
            }
        }

        private void prefetchPredecessorsOfRealNode(View view, List<AccessibilityNodeInfo> outInfos) {
            if (shouldStopPrefetching(outInfos)) {
                return;
            }
            for (ViewParent parent = view.getParentForAccessibility(); (parent instanceof View) && !shouldStopPrefetching(outInfos); parent = parent.getParentForAccessibility()) {
                View parentView = (View) parent;
                AccessibilityNodeInfo info = parentView.createAccessibilityNodeInfo();
                if (info != null) {
                    outInfos.add(info);
                }
            }
        }

        private void prefetchSiblingsOfRealNode(View current, List<AccessibilityNodeInfo> outInfos, boolean predecessorsPrefetched) {
            AccessibilityNodeInfo info;
            if (shouldStopPrefetching(outInfos)) {
                return;
            }
            ViewParent parent = current.getParentForAccessibility();
            if (parent instanceof ViewGroup) {
                ViewGroup parentGroup = (ViewGroup) parent;
                ArrayList<View> children = this.mTempViewList;
                children.clear();
                if (!predecessorsPrefetched) {
                    try {
                        AccessibilityNodeInfo parentInfo = ((ViewGroup) parent).createAccessibilityNodeInfo();
                        if (parentInfo != null) {
                            outInfos.add(parentInfo);
                        }
                    } finally {
                        children.clear();
                    }
                }
                parentGroup.addChildrenForAccessibility(children);
                int childCount = children.size();
                for (int i = 0; i < childCount; i++) {
                    if (shouldStopPrefetching(outInfos)) {
                        return;
                    }
                    View child = children.get(i);
                    if (child.getAccessibilityViewId() != current.getAccessibilityViewId() && AccessibilityInteractionController.this.isShown(child)) {
                        AccessibilityNodeProvider provider = child.getAccessibilityNodeProvider();
                        if (provider == null) {
                            info = child.createAccessibilityNodeInfo();
                        } else {
                            info = provider.createAccessibilityNodeInfo(-1);
                        }
                        if (info != null) {
                            outInfos.add(info);
                        }
                    }
                }
            }
        }

        private void prefetchDescendantsOfRealNode(View root, List<AccessibilityNodeInfo> outInfos) {
            if (shouldStopPrefetching(outInfos) || !(root instanceof ViewGroup)) {
                return;
            }
            LinkedHashMap<View, AccessibilityNodeInfo> addedChildren = new LinkedHashMap<>();
            ArrayList<View> children = this.mTempViewList;
            children.clear();
            try {
                root.addChildrenForAccessibility(children);
                int childCount = children.size();
                for (int i = 0; i < childCount; i++) {
                    if (shouldStopPrefetching(outInfos)) {
                        return;
                    }
                    View child = children.get(i);
                    if (AccessibilityInteractionController.this.isShown(child)) {
                        AccessibilityNodeProvider provider = child.getAccessibilityNodeProvider();
                        if (provider == null) {
                            AccessibilityNodeInfo info = child.createAccessibilityNodeInfo();
                            if (info != null) {
                                outInfos.add(info);
                                addedChildren.put(child, null);
                            }
                        } else {
                            AccessibilityNodeInfo info2 = provider.createAccessibilityNodeInfo(-1);
                            if (info2 != null) {
                                outInfos.add(info2);
                                addedChildren.put(child, info2);
                            }
                        }
                    }
                }
                children.clear();
                if (!shouldStopPrefetching(outInfos)) {
                    for (Map.Entry<View, AccessibilityNodeInfo> entry : addedChildren.entrySet()) {
                        View addedChild = entry.getKey();
                        AccessibilityNodeInfo virtualRoot = entry.getValue();
                        if (virtualRoot == null) {
                            prefetchDescendantsOfRealNode(addedChild, outInfos);
                        } else {
                            prefetchDescendantsOfVirtualNode(virtualRoot, addedChild.getAccessibilityNodeProvider(), outInfos);
                        }
                    }
                }
            } finally {
                children.clear();
            }
        }

        private void prefetchPredecessorsOfVirtualNode(AccessibilityNodeInfo root, View providerHost, AccessibilityNodeProvider provider, List<AccessibilityNodeInfo> outInfos) {
            int initialResultSize = outInfos.size();
            long parentNodeId = root.getParentNodeId();
            int accessibilityViewId = AccessibilityNodeInfo.getAccessibilityViewId(parentNodeId);
            while (accessibilityViewId != Integer.MAX_VALUE && !shouldStopPrefetching(outInfos)) {
                int virtualDescendantId = AccessibilityNodeInfo.getVirtualDescendantId(parentNodeId);
                if (virtualDescendantId != -1 || accessibilityViewId == providerHost.getAccessibilityViewId()) {
                    AccessibilityNodeInfo parent = provider.createAccessibilityNodeInfo(virtualDescendantId);
                    if (parent == null) {
                        int currentResultSize = outInfos.size();
                        for (int i = currentResultSize - 1; i >= initialResultSize; i--) {
                            outInfos.remove(i);
                        }
                        return;
                    }
                    outInfos.add(parent);
                    parentNodeId = parent.getParentNodeId();
                    accessibilityViewId = AccessibilityNodeInfo.getAccessibilityViewId(parentNodeId);
                } else {
                    prefetchPredecessorsOfRealNode(providerHost, outInfos);
                    return;
                }
            }
        }

        private void prefetchSiblingsOfVirtualNode(AccessibilityNodeInfo current, View providerHost, AccessibilityNodeProvider provider, List<AccessibilityNodeInfo> outInfos, boolean predecessorsPrefetched) {
            long parentNodeId = current.getParentNodeId();
            int parentAccessibilityViewId = AccessibilityNodeInfo.getAccessibilityViewId(parentNodeId);
            int parentVirtualDescendantId = AccessibilityNodeInfo.getVirtualDescendantId(parentNodeId);
            if (parentVirtualDescendantId == -1 && parentAccessibilityViewId != providerHost.getAccessibilityViewId()) {
                prefetchSiblingsOfRealNode(providerHost, outInfos, predecessorsPrefetched);
                return;
            }
            AccessibilityNodeInfo parent = provider.createAccessibilityNodeInfo(parentVirtualDescendantId);
            if (parent != null) {
                if (!predecessorsPrefetched) {
                    outInfos.add(parent);
                }
                int childCount = parent.getChildCount();
                for (int i = 0; i < childCount && !shouldStopPrefetching(outInfos); i++) {
                    long childNodeId = parent.getChildId(i);
                    if (childNodeId != current.getSourceNodeId()) {
                        int childVirtualDescendantId = AccessibilityNodeInfo.getVirtualDescendantId(childNodeId);
                        AccessibilityNodeInfo child = provider.createAccessibilityNodeInfo(childVirtualDescendantId);
                        if (child != null) {
                            outInfos.add(child);
                        }
                    }
                }
            }
        }

        private void prefetchDescendantsOfVirtualNode(AccessibilityNodeInfo root, AccessibilityNodeProvider provider, List<AccessibilityNodeInfo> outInfos) {
            int initialOutInfosSize = outInfos.size();
            int childCount = root.getChildCount();
            for (int i = 0; i < childCount; i++) {
                if (shouldStopPrefetching(outInfos)) {
                    return;
                }
                long childNodeId = root.getChildId(i);
                AccessibilityNodeInfo child = provider.createAccessibilityNodeInfo(AccessibilityNodeInfo.getVirtualDescendantId(childNodeId));
                if (child != null) {
                    outInfos.add(child);
                }
            }
            if (!shouldStopPrefetching(outInfos)) {
                int addedChildCount = outInfos.size() - initialOutInfosSize;
                for (int i2 = 0; i2 < addedChildCount; i2++) {
                    prefetchDescendantsOfVirtualNode(outInfos.get(initialOutInfosSize + i2), provider, outInfos);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public class PrivateHandler extends Handler {
        private static final int FIRST_NO_ACCESSIBILITY_CALLBACK_MSG = 100;
        private static final int MSG_APP_PREPARATION_FINISHED = 8;
        private static final int MSG_APP_PREPARATION_TIMEOUT = 9;
        private static final int MSG_CLEAR_ACCESSIBILITY_FOCUS = 101;
        private static final int MSG_FIND_ACCESSIBILITY_NODE_INFOS_BY_VIEW_ID = 3;
        private static final int MSG_FIND_ACCESSIBILITY_NODE_INFO_BY_ACCESSIBILITY_ID = 2;
        private static final int MSG_FIND_ACCESSIBILITY_NODE_INFO_BY_TEXT = 4;
        private static final int MSG_FIND_FOCUS = 5;
        private static final int MSG_FOCUS_SEARCH = 6;
        private static final int MSG_NOTIFY_OUTSIDE_TOUCH = 102;
        private static final int MSG_PERFORM_ACCESSIBILITY_ACTION = 1;
        private static final int MSG_PREPARE_FOR_EXTRA_DATA_REQUEST = 7;

        public PrivateHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public String getMessageName(Message message) {
            int type = message.what;
            switch (type) {
                case 1:
                    return "MSG_PERFORM_ACCESSIBILITY_ACTION";
                case 2:
                    return "MSG_FIND_ACCESSIBILITY_NODE_INFO_BY_ACCESSIBILITY_ID";
                case 3:
                    return "MSG_FIND_ACCESSIBILITY_NODE_INFOS_BY_VIEW_ID";
                case 4:
                    return "MSG_FIND_ACCESSIBILITY_NODE_INFO_BY_TEXT";
                case 5:
                    return "MSG_FIND_FOCUS";
                case 6:
                    return "MSG_FOCUS_SEARCH";
                case 7:
                    return "MSG_PREPARE_FOR_EXTRA_DATA_REQUEST";
                case 8:
                    return "MSG_APP_PREPARATION_FINISHED";
                case 9:
                    return "MSG_APP_PREPARATION_TIMEOUT";
                case 101:
                    return "MSG_CLEAR_ACCESSIBILITY_FOCUS";
                case 102:
                    return "MSG_NOTIFY_OUTSIDE_TOUCH";
                default:
                    throw new IllegalArgumentException("Unknown message type: " + type);
            }
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            int type = message.what;
            switch (type) {
                case 1:
                    AccessibilityInteractionController.this.performAccessibilityActionUiThread(message);
                    return;
                case 2:
                    AccessibilityInteractionController.this.findAccessibilityNodeInfoByAccessibilityIdUiThread(message);
                    return;
                case 3:
                    AccessibilityInteractionController.this.findAccessibilityNodeInfosByViewIdUiThread(message);
                    return;
                case 4:
                    AccessibilityInteractionController.this.findAccessibilityNodeInfosByTextUiThread(message);
                    return;
                case 5:
                    AccessibilityInteractionController.this.findFocusUiThread(message);
                    return;
                case 6:
                    AccessibilityInteractionController.this.focusSearchUiThread(message);
                    return;
                case 7:
                    AccessibilityInteractionController.this.prepareForExtraDataRequestUiThread(message);
                    return;
                case 8:
                    AccessibilityInteractionController.this.requestPreparerDoneUiThread(message);
                    return;
                case 9:
                    AccessibilityInteractionController.this.requestPreparerTimeoutUiThread();
                    return;
                case 101:
                    AccessibilityInteractionController.this.clearAccessibilityFocusUiThread();
                    return;
                case 102:
                    AccessibilityInteractionController.this.notifyOutsideTouchUiThread();
                    return;
                default:
                    throw new IllegalArgumentException("Unknown message type: " + type);
            }
        }

        boolean hasAccessibilityCallback(Message message) {
            return message.what < 100;
        }

        boolean hasUserInteractiveMessagesWaiting() {
            return hasMessagesOrCallbacks();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public final class AddNodeInfosForViewId implements Predicate<View> {
        private List<AccessibilityNodeInfo> mInfos;
        private int mViewId;

        private AddNodeInfosForViewId() {
            this.mViewId = -1;
        }

        public void init(int viewId, List<AccessibilityNodeInfo> infos) {
            this.mViewId = viewId;
            this.mInfos = infos;
        }

        public void reset() {
            this.mViewId = -1;
            this.mInfos = null;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Predicate
        public boolean test(View view) {
            if (view.getId() == this.mViewId && AccessibilityInteractionController.this.isShown(view)) {
                this.mInfos.add(view.createAccessibilityNodeInfo());
                return false;
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public static final class MessageHolder {
        final int mInterrogatingPid;
        final long mInterrogatingTid;
        final Message mMessage;

        MessageHolder(Message message, int interrogatingPid, long interrogatingTid) {
            this.mMessage = message;
            this.mInterrogatingPid = interrogatingPid;
            this.mInterrogatingTid = interrogatingTid;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public static class SatisfiedFindAccessibilityNodeByAccessibilityIdRequest {
        final IAccessibilityInteractionConnectionCallback mSatisfiedRequestCallback;
        final int mSatisfiedRequestInteractionId;
        final AccessibilityNodeInfo mSatisfiedRequestNode;

        SatisfiedFindAccessibilityNodeByAccessibilityIdRequest(AccessibilityNodeInfo satisfiedRequestNode, IAccessibilityInteractionConnectionCallback satisfiedRequestCallback, int satisfiedRequestInteractionId) {
            this.mSatisfiedRequestNode = satisfiedRequestNode;
            this.mSatisfiedRequestCallback = satisfiedRequestCallback;
            this.mSatisfiedRequestInteractionId = satisfiedRequestInteractionId;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public class PrefetchDeque<E extends DequeNode> extends ArrayDeque<E> {
        List<AccessibilityNodeInfo> mPrefetchOutput;
        int mStrategy;

        PrefetchDeque(int strategy, List<AccessibilityNodeInfo> output) {
            this.mStrategy = strategy;
            this.mPrefetchOutput = output;
        }

        void performTraversalAndPrefetch() {
            while (!isEmpty()) {
                try {
                    E child = getNext();
                    AccessibilityNodeInfo childInfo = child.getA11yNodeInfo();
                    if (childInfo != null) {
                        this.mPrefetchOutput.add(childInfo);
                    }
                    if (AccessibilityInteractionController.this.mPrefetcher.shouldStopPrefetching(this.mPrefetchOutput)) {
                        return;
                    }
                    child.addChildren(childInfo, this);
                } finally {
                    clear();
                }
            }
        }

        E getNext() {
            if (isStack()) {
                return (E) pop();
            }
            return (E) removeLast();
        }

        boolean isStack() {
            return (this.mStrategy & 8) != 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public class ViewNode implements DequeNode {
        private final ArrayList<View> mTempViewList = new ArrayList<>();
        View mView;

        ViewNode(View view) {
            this.mView = view;
        }

        @Override // android.view.AccessibilityInteractionController.DequeNode
        public AccessibilityNodeInfo getA11yNodeInfo() {
            View view = this.mView;
            if (view == null) {
                return null;
            }
            return view.createAccessibilityNodeInfo();
        }

        @Override // android.view.AccessibilityInteractionController.DequeNode
        public void addChildren(AccessibilityNodeInfo virtualRoot, PrefetchDeque deque) {
            View view = this.mView;
            if (view == null || !(view instanceof ViewGroup)) {
                return;
            }
            ArrayList<View> children = this.mTempViewList;
            children.clear();
            try {
                this.mView.addChildrenForAccessibility(children);
                int childCount = children.size();
                if (deque.isStack()) {
                    for (int i = childCount - 1; i >= 0; i--) {
                        addChild(deque, children.get(i));
                    }
                } else {
                    for (int i2 = 0; i2 < childCount; i2++) {
                        addChild(deque, children.get(i2));
                    }
                }
            } finally {
                children.clear();
            }
        }

        private void addChild(ArrayDeque deque, View child) {
            if (AccessibilityInteractionController.this.isShown(child)) {
                AccessibilityNodeProvider provider = child.getAccessibilityNodeProvider();
                if (provider == null) {
                    deque.push(new ViewNode(child));
                } else {
                    deque.push(new VirtualNode(-1L, provider));
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public class VirtualNode implements DequeNode {
        long mInfoId;
        AccessibilityNodeProvider mProvider;

        VirtualNode(long id, AccessibilityNodeProvider provider) {
            this.mInfoId = id;
            this.mProvider = provider;
        }

        @Override // android.view.AccessibilityInteractionController.DequeNode
        public AccessibilityNodeInfo getA11yNodeInfo() {
            AccessibilityNodeProvider accessibilityNodeProvider = this.mProvider;
            if (accessibilityNodeProvider == null) {
                return null;
            }
            return accessibilityNodeProvider.createAccessibilityNodeInfo(AccessibilityNodeInfo.getVirtualDescendantId(this.mInfoId));
        }

        @Override // android.view.AccessibilityInteractionController.DequeNode
        public void addChildren(AccessibilityNodeInfo virtualRoot, PrefetchDeque deque) {
            if (virtualRoot == null) {
                return;
            }
            int childCount = virtualRoot.getChildCount();
            if (deque.isStack()) {
                for (int i = childCount - 1; i >= 0; i--) {
                    long childNodeId = virtualRoot.getChildId(i);
                    deque.push(new VirtualNode(childNodeId, this.mProvider));
                }
                return;
            }
            for (int i2 = 0; i2 < childCount; i2++) {
                long childNodeId2 = virtualRoot.getChildId(i2);
                deque.push(new VirtualNode(childNodeId2, this.mProvider));
            }
        }
    }
}
