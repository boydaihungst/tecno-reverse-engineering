package com.android.server.wm;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipDescription;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.input.InputManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.InputConstants;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import android.view.Display;
import android.view.DragEvent;
import android.view.InputApplicationHandle;
import android.view.InputChannel;
import android.view.InputWindowHandle;
import android.view.SurfaceControl;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.view.IDragAndDropPermissions;
import com.android.server.LocalServices;
import com.android.server.pm.UserManagerInternal;
import com.android.server.usb.descriptors.UsbACInterface;
import com.android.server.wm.IDragLice;
import com.mediatek.server.wm.WmsExt;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class DragState {
    private static final String ANIMATED_PROPERTY_ALPHA = "alpha";
    private static final String ANIMATED_PROPERTY_SCALE = "scale";
    private static final String ANIMATED_PROPERTY_X = "x";
    private static final String ANIMATED_PROPERTY_Y = "y";
    private static final int DRAG_FLAGS_URI_ACCESS = 3;
    private static final int DRAG_FLAGS_URI_PERMISSIONS = 195;
    private static final long MAX_ANIMATION_DURATION_MS = 375;
    private static final long MIN_ANIMATION_DURATION_MS = 195;
    private ValueAnimator mAnimator;
    boolean mCrossProfileCopyAllowed;
    float mCurrentX;
    float mCurrentY;
    ClipData mData;
    ClipDescription mDataDescription;
    DisplayContent mDisplayContent;
    final DragDropController mDragDropController;
    boolean mDragInProgress;
    boolean mDragResult;
    int mFlags;
    InputInterceptor mInputInterceptor;
    SurfaceControl mInputSurface;
    private boolean mIsClosing;
    IBinder mLocalWin;
    float mOriginalAlpha;
    float mOriginalX;
    float mOriginalY;
    int mPid;
    boolean mRelinquishDragSurfaceToDropTarget;
    final WindowManagerService mService;
    int mSourceUserId;
    SurfaceControl mSurfaceControl;
    float mThumbOffsetX;
    float mThumbOffsetY;
    IBinder mToken;
    int mTouchSource;
    final SurfaceControl.Transaction mTransaction;
    int mUid;
    volatile boolean mAnimationCompleted = false;
    private final Interpolator mCubicEaseOutInterpolator = new DecelerateInterpolator(1.5f);
    private final Point mDisplaySize = new Point();
    private final Rect mTmpClipRect = new Rect();
    ArrayList<WindowState> mNotifiedWindows = new ArrayList<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    public DragState(WindowManagerService service, DragDropController controller, IBinder token, SurfaceControl surface, int flags, IBinder localWin) {
        this.mService = service;
        this.mDragDropController = controller;
        this.mToken = token;
        this.mSurfaceControl = surface;
        this.mFlags = flags;
        this.mLocalWin = localWin;
        this.mTransaction = service.mTransactionFactory.get();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isClosing() {
        return this.mIsClosing;
    }

    private void showInputSurface() {
        if (this.mInputSurface == null) {
            this.mInputSurface = this.mService.makeSurfaceBuilder(this.mDisplayContent.getSession()).setContainerLayer().setName("Drag and Drop Input Consumer").setCallsite("DragState.showInputSurface").setParent(this.mDisplayContent.getOverlayLayer()).build();
        }
        InputWindowHandle h = getInputWindowHandle();
        if (h == null) {
            Slog.w(WmsExt.TAG, "Drag is in progress but there is no drag window handle.");
            return;
        }
        this.mTmpClipRect.set(0, 0, this.mDisplaySize.x, this.mDisplaySize.y);
        this.mTransaction.show(this.mInputSurface).setInputWindowInfo(this.mInputSurface, h).setLayer(this.mInputSurface, Integer.MAX_VALUE).setCrop(this.mInputSurface, this.mTmpClipRect);
        this.mTransaction.syncInputWindows().apply(true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void closeLocked() {
        SurfaceControl dragSurface;
        float y;
        float y2;
        this.mIsClosing = true;
        if (this.mInputInterceptor != null) {
            if (WindowManagerDebugConfig.DEBUG_DRAG) {
                Slog.d(WmsExt.TAG, "unregistering drag input channel");
            }
            this.mDragDropController.sendHandlerMessage(1, this.mInputInterceptor);
            this.mInputInterceptor = null;
        }
        if (this.mDragInProgress) {
            int myPid = Process.myPid();
            if (WindowManagerDebugConfig.DEBUG_DRAG) {
                Slog.d(WmsExt.TAG, "broadcasting DRAG_ENDED");
            }
            Iterator<WindowState> it = this.mNotifiedWindows.iterator();
            while (it.hasNext()) {
                WindowState ws = it.next();
                if (!this.mDragResult && ws.mSession.mPid == this.mPid) {
                    float x = this.mCurrentX;
                    float y3 = this.mCurrentY;
                    if (!relinquishDragSurfaceToDragSource()) {
                        dragSurface = null;
                        y = y3;
                        y2 = x;
                    } else {
                        SurfaceControl dragSurface2 = this.mSurfaceControl;
                        dragSurface = dragSurface2;
                        y = y3;
                        y2 = x;
                    }
                } else {
                    dragSurface = null;
                    y = 0.0f;
                    y2 = 0.0f;
                }
                DragEvent event = DragEvent.obtain(4, y2, y, this.mThumbOffsetX, this.mThumbOffsetY, null, null, null, dragSurface, null, this.mDragResult);
                try {
                    ws.mClient.dispatchDragEvent(event);
                } catch (RemoteException e) {
                    Slog.w(WmsExt.TAG, "Unable to drag-end window " + ws);
                }
                if (myPid != ws.mSession.mPid) {
                    event.recycle();
                }
            }
            this.mNotifiedWindows.clear();
            this.mDragInProgress = false;
        }
        IDragLice.Instance().reportDropStatus(4);
        if (isFromSource(UsbACInterface.FORMAT_III_IEC1937_MPEG1_Layer1)) {
            this.mService.restorePointerIconLocked(this.mDisplayContent, this.mCurrentX, this.mCurrentY);
            this.mTouchSource = 0;
        }
        SurfaceControl surfaceControl = this.mInputSurface;
        if (surfaceControl != null) {
            this.mTransaction.remove(surfaceControl).apply();
            this.mInputSurface = null;
        }
        if (this.mSurfaceControl != null) {
            if (this.mRelinquishDragSurfaceToDropTarget || relinquishDragSurfaceToDragSource()) {
                this.mDragDropController.sendTimeoutMessage(3, this.mSurfaceControl, 5000L);
            } else {
                this.mTransaction.reparent(this.mSurfaceControl, null).apply();
            }
            this.mSurfaceControl = null;
        }
        if (this.mAnimator != null && !this.mAnimationCompleted) {
            Slog.wtf(WmsExt.TAG, "Unexpectedly destroying mSurfaceControl while animation is running");
        }
        this.mFlags = 0;
        this.mLocalWin = null;
        this.mToken = null;
        this.mData = null;
        this.mThumbOffsetY = 0.0f;
        this.mThumbOffsetX = 0.0f;
        this.mNotifiedWindows = null;
        this.mDragDropController.onDragStateClosedLocked(this);
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IGET, IGET]}, finally: {[IGET, IGET, INVOKE, IF] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [367=4] */
    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean reportDropWindowLock(IBinder token, float x, float y) {
        IDragAndDropPermissions iDragAndDropPermissions;
        ClipData clipData;
        if (this.mAnimator != null) {
            return false;
        }
        WindowState touchedWin = this.mService.mInputToWindowMap.get(token);
        if (!isWindowNotified(touchedWin)) {
            this.mDragResult = false;
            endDragLocked();
            if (WindowManagerDebugConfig.DEBUG_DRAG) {
                Slog.d(WmsExt.TAG, "Drop outside a valid window " + touchedWin);
            }
            return false;
        }
        if (WindowManagerDebugConfig.DEBUG_DRAG) {
            Slog.d(WmsExt.TAG, "sending DROP to " + touchedWin);
        }
        final int targetUserId = UserHandle.getUserId(touchedWin.getOwningUid());
        int i = this.mFlags;
        if ((i & 256) == 0 || (i & 3) == 0 || this.mData == null) {
            iDragAndDropPermissions = null;
        } else {
            iDragAndDropPermissions = new DragAndDropPermissionsHandler(this.mService.mGlobalLock, this.mData, this.mUid, touchedWin.getOwningPackage(), this.mFlags & 195, this.mSourceUserId, targetUserId);
            IDragLice.Instance().handleDragAndDropPermissions(iDragAndDropPermissions, touchedWin, new IDragLice.ITrDragAndDropPermissions() { // from class: com.android.server.wm.DragState.1
                @Override // com.android.server.wm.IDragLice.ITrDragAndDropPermissions
                public IDragAndDropPermissions getIDragAndDropPermissions(String packageName) {
                    DragAndDropPermissionsHandler resolverPermission = new DragAndDropPermissionsHandler(DragState.this.mService.mGlobalLock, DragState.this.mData, DragState.this.mUid, packageName, DragState.this.mFlags & 195, DragState.this.mSourceUserId, targetUserId);
                    resolverPermission.setFinalize(true);
                    return resolverPermission;
                }
            });
        }
        int i2 = this.mSourceUserId;
        if (i2 != targetUserId && (clipData = this.mData) != null) {
            clipData.fixUris(i2);
        }
        int myPid = Process.myPid();
        IBinder clientToken = touchedWin.mClient.asBinder();
        DragEvent event = obtainDragEvent(3, x, y, this.mData, targetInterceptsGlobalDrag(touchedWin), iDragAndDropPermissions);
        try {
            try {
                touchedWin.mClient.dispatchDragEvent(event);
                this.mDragDropController.sendTimeoutMessage(0, clientToken, 5000L);
                if (myPid != touchedWin.mSession.mPid) {
                    event.recycle();
                }
                this.mToken = clientToken;
                return true;
            } catch (RemoteException e) {
                Slog.w(WmsExt.TAG, "can't send drop notification to win " + touchedWin);
                IDragLice.Instance().reportDropWindowLockException(touchedWin);
                endDragLocked();
                if (myPid != touchedWin.mSession.mPid) {
                    event.recycle();
                }
                return false;
            }
        } catch (Throwable th) {
            if (myPid != touchedWin.mSession.mPid) {
                event.recycle();
            }
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class InputInterceptor {
        InputChannel mClientChannel;
        InputApplicationHandle mDragApplicationHandle = new InputApplicationHandle(new Binder(), "drag", InputConstants.DEFAULT_DISPATCHING_TIMEOUT_MILLIS);
        InputWindowHandle mDragWindowHandle;
        DragInputEventReceiver mInputEventReceiver;

        InputInterceptor(Display display) {
            this.mClientChannel = DragState.this.mService.mInputManager.createInputChannel("drag");
            this.mInputEventReceiver = new DragInputEventReceiver(this.mClientChannel, DragState.this.mService.mH.getLooper(), DragState.this.mDragDropController);
            InputWindowHandle inputWindowHandle = new InputWindowHandle(this.mDragApplicationHandle, display.getDisplayId());
            this.mDragWindowHandle = inputWindowHandle;
            inputWindowHandle.name = "drag";
            this.mDragWindowHandle.token = this.mClientChannel.getToken();
            this.mDragWindowHandle.layoutParamsType = 2016;
            this.mDragWindowHandle.dispatchingTimeoutMillis = InputConstants.DEFAULT_DISPATCHING_TIMEOUT_MILLIS;
            this.mDragWindowHandle.ownerPid = Process.myPid();
            this.mDragWindowHandle.ownerUid = Process.myUid();
            this.mDragWindowHandle.scaleFactor = 1.0f;
            this.mDragWindowHandle.inputConfig = 16;
            this.mDragWindowHandle.touchableRegion.setEmpty();
            if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_ORIENTATION, -694710814, 0, (String) null, (Object[]) null);
            }
            DragState.this.mService.mRoot.forAllDisplays(new Consumer() { // from class: com.android.server.wm.DragState$InputInterceptor$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((DisplayContent) obj).getDisplayRotation().pause();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void tearDown() {
            DragState.this.mService.mInputManager.removeInputChannel(this.mClientChannel.getToken());
            this.mInputEventReceiver.dispose();
            this.mInputEventReceiver = null;
            this.mClientChannel.dispose();
            this.mClientChannel = null;
            this.mDragWindowHandle = null;
            this.mDragApplicationHandle = null;
            if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_ORIENTATION, 269576220, 0, (String) null, (Object[]) null);
            }
            DragState.this.mService.mRoot.forAllDisplays(new Consumer() { // from class: com.android.server.wm.DragState$InputInterceptor$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((DisplayContent) obj).getDisplayRotation().resume();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InputChannel getInputChannel() {
        InputInterceptor inputInterceptor = this.mInputInterceptor;
        if (inputInterceptor == null) {
            return null;
        }
        return inputInterceptor.mClientChannel;
    }

    InputWindowHandle getInputWindowHandle() {
        InputInterceptor inputInterceptor = this.mInputInterceptor;
        if (inputInterceptor == null) {
            return null;
        }
        return inputInterceptor.mDragWindowHandle;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void register(Display display) {
        display.getRealSize(this.mDisplaySize);
        if (WindowManagerDebugConfig.DEBUG_DRAG) {
            Slog.d(WmsExt.TAG, "registering drag input channel");
        }
        if (this.mInputInterceptor != null) {
            Slog.e(WmsExt.TAG, "Duplicate register of drag input channel");
            return;
        }
        this.mInputInterceptor = new InputInterceptor(display);
        showInputSurface();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void broadcastDragStartedLocked(final float touchX, final float touchY) {
        this.mCurrentX = touchX;
        this.mOriginalX = touchX;
        this.mCurrentY = touchY;
        this.mOriginalY = touchY;
        ClipData clipData = this.mData;
        this.mDataDescription = clipData != null ? clipData.getDescription() : null;
        this.mNotifiedWindows.clear();
        this.mDragInProgress = true;
        this.mSourceUserId = UserHandle.getUserId(this.mUid);
        UserManagerInternal userManager = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        this.mCrossProfileCopyAllowed = true ^ userManager.getUserRestriction(this.mSourceUserId, "no_cross_profile_copy_paste");
        if (WindowManagerDebugConfig.DEBUG_DRAG) {
            Slog.d(WmsExt.TAG, "broadcasting DRAG_STARTED at (" + touchX + ", " + touchY + ")");
        }
        final boolean containsAppExtras = containsApplicationExtras(this.mDataDescription);
        this.mService.mRoot.forAllWindows(new Consumer() { // from class: com.android.server.wm.DragState$$ExternalSyntheticLambda2
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DragState.this.m8010x7ee82a81(touchX, touchY, containsAppExtras, (WindowState) obj);
            }
        }, false);
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[INVOKE, IGET, IGET]}, finally: {[INVOKE, IGET, IGET, INVOKE, IF] complete} */
    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: sendDragStartedLocked */
    public void m8010x7ee82a81(WindowState newWin, float touchX, float touchY, boolean containsAppExtras) {
        boolean interceptsGlobalDrag = targetInterceptsGlobalDrag(newWin);
        if (this.mDragInProgress && isValidDropTarget(newWin, containsAppExtras, interceptsGlobalDrag)) {
            ClipData data = interceptsGlobalDrag ? this.mData.copyForTransferWithActivityInfo() : null;
            DragEvent event = obtainDragEvent(1, newWin.translateToWindowX(touchX), newWin.translateToWindowY(touchY), data, false, null);
            try {
                try {
                    newWin.mClient.dispatchDragEvent(event);
                    this.mNotifiedWindows.add(newWin);
                    if (Process.myPid() == newWin.mSession.mPid) {
                        return;
                    }
                } catch (RemoteException e) {
                    Slog.w(WmsExt.TAG, "Unable to drag-start window " + newWin);
                    if (Process.myPid() == newWin.mSession.mPid) {
                        return;
                    }
                }
                event.recycle();
            } catch (Throwable th) {
                if (Process.myPid() != newWin.mSession.mPid) {
                    event.recycle();
                }
                throw th;
            }
        }
    }

    private boolean containsApplicationExtras(ClipDescription desc) {
        if (desc == null) {
            return false;
        }
        return desc.hasMimeType("application/vnd.android.activity") || desc.hasMimeType("application/vnd.android.shortcut") || desc.hasMimeType("application/vnd.android.task");
    }

    private boolean isValidDropTarget(WindowState targetWin, boolean containsAppExtras, boolean interceptsGlobalDrag) {
        if (targetWin == null) {
            return false;
        }
        boolean isLocalWindow = this.mLocalWin == targetWin.mClient.asBinder();
        if ((!isLocalWindow && !interceptsGlobalDrag && containsAppExtras) || !targetWin.isPotentialDragTarget(interceptsGlobalDrag)) {
            return false;
        }
        if (((this.mFlags & 256) == 0 || !targetWindowSupportsGlobalDrag(targetWin)) && !isLocalWindow) {
            return false;
        }
        return interceptsGlobalDrag || this.mCrossProfileCopyAllowed || this.mSourceUserId == UserHandle.getUserId(targetWin.getOwningUid());
    }

    private boolean targetWindowSupportsGlobalDrag(WindowState targetWin) {
        return targetWin.mActivityRecord == null || targetWin.mActivityRecord.mTargetSdk >= 24;
    }

    public boolean targetInterceptsGlobalDrag(WindowState targetWin) {
        return (targetWin.mAttrs.privateFlags & Integer.MIN_VALUE) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendDragStartedIfNeededLocked(WindowState newWin) {
        if (!this.mDragInProgress || isWindowNotified(newWin)) {
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_DRAG) {
            Slog.d(WmsExt.TAG, "need to send DRAG_STARTED to new window " + newWin);
        }
        m8010x7ee82a81(newWin, this.mCurrentX, this.mCurrentY, containsApplicationExtras(this.mDataDescription));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isWindowNotified(WindowState newWin) {
        Iterator<WindowState> it = this.mNotifiedWindows.iterator();
        while (it.hasNext()) {
            WindowState ws = it.next();
            if (ws == newWin) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void endDragLocked() {
        if (this.mAnimator != null) {
            return;
        }
        if (!this.mDragResult && !isAccessibilityDragDrop() && !relinquishDragSurfaceToDragSource()) {
            this.mAnimator = createReturnAnimationLocked();
        } else {
            closeLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cancelDragLocked(boolean skipAnimation) {
        if (this.mAnimator != null) {
            return;
        }
        if (!this.mDragInProgress || skipAnimation || isAccessibilityDragDrop()) {
            closeLocked();
        } else {
            this.mAnimator = createCancelAnimationLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateDragSurfaceLocked(boolean keepHandling, float x, float y) {
        if (this.mAnimator != null) {
            return;
        }
        this.mCurrentX = x;
        this.mCurrentY = y;
        if (!keepHandling) {
            return;
        }
        if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
            Slog.i(WmsExt.TAG, ">>> OPEN TRANSACTION notifyMoveLocked");
        }
        this.mTransaction.setPosition(this.mSurfaceControl, x - this.mThumbOffsetX, y - this.mThumbOffsetY).apply();
        if (ProtoLogCache.WM_SHOW_TRANSACTIONS_enabled) {
            String protoLogParam0 = String.valueOf(this.mSurfaceControl);
            long protoLogParam1 = (int) (x - this.mThumbOffsetX);
            long protoLogParam2 = (int) (y - this.mThumbOffsetY);
            ProtoLogImpl.i(ProtoLogGroup.WM_SHOW_TRANSACTIONS, 342460966, 20, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1), Long.valueOf(protoLogParam2)});
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isInProgress() {
        return this.mDragInProgress;
    }

    private DragEvent obtainDragEvent(int action, float x, float y, ClipData data, boolean includeDragSurface, IDragAndDropPermissions dragAndDropPermissions) {
        return DragEvent.obtain(action, x, y, this.mThumbOffsetX, this.mThumbOffsetY, null, this.mDataDescription, data, includeDragSurface ? this.mSurfaceControl : null, dragAndDropPermissions, false);
    }

    private ValueAnimator createReturnAnimationLocked() {
        float f = this.mCurrentX;
        float f2 = this.mThumbOffsetX;
        float[] fArr = {f - f2, this.mOriginalX - f2};
        float f3 = this.mCurrentY;
        float f4 = this.mThumbOffsetY;
        float[] fArr2 = {f3 - f4, this.mOriginalY - f4};
        float f5 = this.mOriginalAlpha;
        final ValueAnimator animator = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_X, fArr), PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_Y, fArr2), PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_SCALE, 1.0f, 1.0f), PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_ALPHA, f5, f5 / 2.0f));
        float translateX = this.mOriginalX - this.mCurrentX;
        float translateY = this.mOriginalY - this.mCurrentY;
        double travelDistance = Math.sqrt((translateX * translateX) + (translateY * translateY));
        double displayDiagonal = Math.sqrt((this.mDisplaySize.x * this.mDisplaySize.x) + (this.mDisplaySize.y * this.mDisplaySize.y));
        long duration = ((long) ((travelDistance / displayDiagonal) * 180.0d)) + MIN_ANIMATION_DURATION_MS;
        AnimationListener listener = new AnimationListener();
        animator.setDuration(duration);
        animator.setInterpolator(this.mCubicEaseOutInterpolator);
        animator.addListener(listener);
        animator.addUpdateListener(listener);
        this.mService.mAnimationHandler.post(new Runnable() { // from class: com.android.server.wm.DragState$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                animator.start();
            }
        });
        return animator;
    }

    private ValueAnimator createCancelAnimationLocked() {
        float f = this.mCurrentX;
        float[] fArr = {f - this.mThumbOffsetX, f};
        float f2 = this.mCurrentY;
        final ValueAnimator animator = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_X, fArr), PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_Y, f2 - this.mThumbOffsetY, f2), PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_SCALE, 1.0f, 0.0f), PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_ALPHA, this.mOriginalAlpha, 0.0f));
        AnimationListener listener = new AnimationListener();
        animator.setDuration(MIN_ANIMATION_DURATION_MS);
        animator.setInterpolator(this.mCubicEaseOutInterpolator);
        animator.addListener(listener);
        animator.addUpdateListener(listener);
        this.mService.mAnimationHandler.post(new Runnable() { // from class: com.android.server.wm.DragState$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                animator.start();
            }
        });
        return animator;
    }

    private boolean isFromSource(int source) {
        return (this.mTouchSource & source) == source;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void overridePointerIconLocked(int touchSource) {
        this.mTouchSource = touchSource;
        if (isFromSource(UsbACInterface.FORMAT_III_IEC1937_MPEG1_Layer1)) {
            InputManager.getInstance().setPointerIconType(1021);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class AnimationListener implements ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {
        private AnimationListener() {
        }

        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator animation) {
            SurfaceControl.Transaction transaction = DragState.this.mService.mTransactionFactory.get();
            try {
                transaction.setPosition(DragState.this.mSurfaceControl, ((Float) animation.getAnimatedValue(DragState.ANIMATED_PROPERTY_X)).floatValue(), ((Float) animation.getAnimatedValue(DragState.ANIMATED_PROPERTY_Y)).floatValue());
                transaction.setAlpha(DragState.this.mSurfaceControl, ((Float) animation.getAnimatedValue(DragState.ANIMATED_PROPERTY_ALPHA)).floatValue());
                transaction.setMatrix(DragState.this.mSurfaceControl, ((Float) animation.getAnimatedValue(DragState.ANIMATED_PROPERTY_SCALE)).floatValue(), 0.0f, 0.0f, ((Float) animation.getAnimatedValue(DragState.ANIMATED_PROPERTY_SCALE)).floatValue());
                transaction.apply();
                if (transaction != null) {
                    transaction.close();
                }
            } catch (Throwable th) {
                if (transaction != null) {
                    try {
                        transaction.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationStart(Animator animator) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animator) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationRepeat(Animator animator) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            DragState.this.mAnimationCompleted = true;
            DragState.this.mDragDropController.sendHandlerMessage(2, null);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAccessibilityDragDrop() {
        return (this.mFlags & 1024) != 0;
    }

    private boolean relinquishDragSurfaceToDragSource() {
        return (this.mFlags & 2048) != 0;
    }
}
