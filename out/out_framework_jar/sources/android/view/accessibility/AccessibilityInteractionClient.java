package android.view.accessibility;

import android.accessibilityservice.IAccessibilityServiceConnection;
import android.content.Context;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.util.SparseLongArray;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityCache;
import android.view.accessibility.IAccessibilityInteractionConnectionCallback;
import com.android.internal.util.ArrayUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
/* loaded from: classes3.dex */
public final class AccessibilityInteractionClient extends IAccessibilityInteractionConnectionCallback.Stub {
    public static final String CALL_STACK = "call_stack";
    private static final boolean CHECK_INTEGRITY = true;
    private static final boolean DEBUG = false;
    public static final String IGNORE_CALL_STACK = "ignore_call_stack";
    private static final String LOG_TAG = "AccessibilityInteractionClient";
    public static final int NO_ID = -1;
    private static final long TIMEOUT_INTERACTION_MILLIS = 5000;
    private final AccessibilityManager mAccessibilityManager;
    private List<StackTraceElement> mCallStackOfCallback;
    private volatile int mCallingUid;
    private int mConnectionIdWaitingForPrefetchResult;
    private AccessibilityNodeInfo mFindAccessibilityNodeInfoResult;
    private List<AccessibilityNodeInfo> mFindAccessibilityNodeInfosResult;
    private final Object mInstanceLock;
    private volatile int mInteractionId;
    private final AtomicInteger mInteractionIdCounter;
    private int mInteractionIdWaitingForPrefetchResult;
    private String[] mPackageNamesForNextPrefetchResult;
    private boolean mPerformAccessibilityActionResult;
    private Message mSameThreadMessage;
    private static final long DISABLE_PREFETCHING_FOR_SCROLLING_MILLIS = (long) (ViewConfiguration.getSendRecurringAccessibilityEventsInterval() * 1.5d);
    private static final Object sStaticLock = new Object();
    private static final LongSparseArray<AccessibilityInteractionClient> sClients = new LongSparseArray<>();
    private static final SparseArray<IAccessibilityServiceConnection> sConnectionCache = new SparseArray<>();
    private static final SparseLongArray sScrollingWindows = new SparseLongArray();
    private static SparseArray<AccessibilityCache> sCaches = new SparseArray<>();

    public static AccessibilityInteractionClient getInstance() {
        long threadId = Thread.currentThread().getId();
        return getInstanceForThread(threadId);
    }

    public static AccessibilityInteractionClient getInstanceForThread(long threadId) {
        AccessibilityInteractionClient client;
        synchronized (sStaticLock) {
            LongSparseArray<AccessibilityInteractionClient> longSparseArray = sClients;
            client = longSparseArray.get(threadId);
            if (client == null) {
                client = new AccessibilityInteractionClient();
                longSparseArray.put(threadId, client);
            }
        }
        return client;
    }

    public static AccessibilityInteractionClient getInstance(Context context) {
        long threadId = Thread.currentThread().getId();
        if (context != null) {
            return getInstanceForThread(threadId, context);
        }
        return getInstanceForThread(threadId);
    }

    public static AccessibilityInteractionClient getInstanceForThread(long threadId, Context context) {
        AccessibilityInteractionClient client;
        synchronized (sStaticLock) {
            LongSparseArray<AccessibilityInteractionClient> longSparseArray = sClients;
            client = longSparseArray.get(threadId);
            if (client == null) {
                client = new AccessibilityInteractionClient(context);
                longSparseArray.put(threadId, client);
            }
        }
        return client;
    }

    public static IAccessibilityServiceConnection getConnection(int connectionId) {
        IAccessibilityServiceConnection iAccessibilityServiceConnection;
        SparseArray<IAccessibilityServiceConnection> sparseArray = sConnectionCache;
        synchronized (sparseArray) {
            iAccessibilityServiceConnection = sparseArray.get(connectionId);
        }
        return iAccessibilityServiceConnection;
    }

    public static void addConnection(int connectionId, IAccessibilityServiceConnection connection, boolean initializeCache) {
        if (connectionId == -1) {
            return;
        }
        SparseArray<IAccessibilityServiceConnection> sparseArray = sConnectionCache;
        synchronized (sparseArray) {
            sparseArray.put(connectionId, connection);
            if (initializeCache) {
                sCaches.put(connectionId, new AccessibilityCache(new AccessibilityCache.AccessibilityNodeRefresher()));
            }
        }
    }

    public static AccessibilityCache getCache(int connectionId) {
        AccessibilityCache accessibilityCache;
        synchronized (sConnectionCache) {
            accessibilityCache = sCaches.get(connectionId);
        }
        return accessibilityCache;
    }

    public static void removeConnection(int connectionId) {
        SparseArray<IAccessibilityServiceConnection> sparseArray = sConnectionCache;
        synchronized (sparseArray) {
            sparseArray.remove(connectionId);
            sCaches.remove(connectionId);
        }
    }

    public static void setCache(int connectionId, AccessibilityCache cache) {
        synchronized (sConnectionCache) {
            sCaches.put(connectionId, cache);
        }
    }

    private AccessibilityInteractionClient() {
        this.mInteractionIdCounter = new AtomicInteger();
        this.mInstanceLock = new Object();
        this.mInteractionId = -1;
        this.mCallingUid = -1;
        this.mInteractionIdWaitingForPrefetchResult = -1;
        this.mAccessibilityManager = null;
    }

    private AccessibilityInteractionClient(Context context) {
        this.mInteractionIdCounter = new AtomicInteger();
        this.mInstanceLock = new Object();
        this.mInteractionId = -1;
        this.mCallingUid = -1;
        this.mInteractionIdWaitingForPrefetchResult = -1;
        this.mAccessibilityManager = (AccessibilityManager) context.getSystemService(AccessibilityManager.class);
    }

    public void setSameThreadMessage(Message message) {
        synchronized (this.mInstanceLock) {
            this.mSameThreadMessage = message;
            this.mInstanceLock.notifyAll();
        }
    }

    public AccessibilityNodeInfo getRootInActiveWindow(int connectionId, int strategy) {
        return findAccessibilityNodeInfoByAccessibilityId(connectionId, Integer.MAX_VALUE, AccessibilityNodeInfo.ROOT_NODE_ID, false, strategy, (Bundle) null);
    }

    public AccessibilityWindowInfo getWindow(int connectionId, int accessibilityWindowId) {
        return getWindow(connectionId, accessibilityWindowId, false);
    }

    public AccessibilityWindowInfo getWindow(int connectionId, int accessibilityWindowId, boolean bypassCache) {
        AccessibilityWindowInfo window;
        try {
            IAccessibilityServiceConnection connection = getConnection(connectionId);
            if (connection != null) {
                AccessibilityCache cache = getCache(connectionId);
                if (cache != null && !bypassCache && (window = cache.getWindow(accessibilityWindowId)) != null) {
                    if (shouldTraceClient()) {
                        logTraceClient(connection, "getWindow cache", "connectionId=" + connectionId + ";accessibilityWindowId=" + accessibilityWindowId + ";bypassCache=false");
                    }
                    return window;
                }
                long identityToken = Binder.clearCallingIdentity();
                AccessibilityWindowInfo window2 = connection.getWindow(accessibilityWindowId);
                Binder.restoreCallingIdentity(identityToken);
                if (shouldTraceClient()) {
                    logTraceClient(connection, "getWindow", "connectionId=" + connectionId + ";accessibilityWindowId=" + accessibilityWindowId + ";bypassCache=" + bypassCache);
                }
                if (window2 != null) {
                    if (!bypassCache && cache != null) {
                        cache.addWindow(window2);
                    }
                    return window2;
                }
                return null;
            }
            return null;
        } catch (RemoteException re) {
            Log.e("AccessibilityInteractionClient", "Error while calling remote getWindow", re);
            return null;
        }
    }

    public List<AccessibilityWindowInfo> getWindows(int connectionId) {
        SparseArray<List<AccessibilityWindowInfo>> windows = getWindowsOnAllDisplays(connectionId);
        if (windows.size() > 0) {
            return windows.valueAt(0);
        }
        return Collections.emptyList();
    }

    public SparseArray<List<AccessibilityWindowInfo>> getWindowsOnAllDisplays(int connectionId) {
        SparseArray<List<AccessibilityWindowInfo>> windows;
        try {
            IAccessibilityServiceConnection connection = getConnection(connectionId);
            if (connection != null) {
                AccessibilityCache cache = getCache(connectionId);
                if (cache != null && (windows = cache.getWindowsOnAllDisplays()) != null) {
                    if (shouldTraceClient()) {
                        logTraceClient(connection, "getWindows cache", "connectionId=" + connectionId);
                    }
                    return windows;
                }
                long identityToken = Binder.clearCallingIdentity();
                long populationTimeStamp = SystemClock.uptimeMillis();
                SparseArray<List<AccessibilityWindowInfo>> windows2 = connection.getWindows();
                Binder.restoreCallingIdentity(identityToken);
                if (shouldTraceClient()) {
                    logTraceClient(connection, "getWindows", "connectionId=" + connectionId);
                }
                if (windows2 != null) {
                    if (cache != null) {
                        cache.setWindowsOnAllDisplays(windows2, populationTimeStamp);
                    }
                    return windows2;
                }
            }
        } catch (RemoteException re) {
            Log.e("AccessibilityInteractionClient", "Error while calling remote getWindowsOnAllDisplays", re);
        }
        SparseArray<List<AccessibilityWindowInfo>> emptyWindows = new SparseArray<>();
        return emptyWindows;
    }

    public AccessibilityNodeInfo findAccessibilityNodeInfoByAccessibilityId(int connectionId, IBinder leashToken, long accessibilityNodeId, boolean bypassCache, int prefetchFlags, Bundle arguments) {
        if (leashToken == null) {
            return null;
        }
        int windowId = -1;
        try {
            IAccessibilityServiceConnection connection = getConnection(connectionId);
            if (connection != null) {
                windowId = connection.getWindowIdForLeashToken(leashToken);
            }
        } catch (RemoteException re) {
            Log.e("AccessibilityInteractionClient", "Error while calling remote getWindowIdForLeashToken", re);
        }
        if (windowId == -1) {
            return null;
        }
        return findAccessibilityNodeInfoByAccessibilityId(connectionId, windowId, accessibilityNodeId, bypassCache, prefetchFlags, arguments);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [647=6] */
    /* JADX WARN: Removed duplicated region for block: B:103:0x00b0 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:87:0x01f7 A[Catch: RemoteException -> 0x0203, TryCatch #1 {RemoteException -> 0x0203, blocks: (B:53:0x014e, B:58:0x015f, B:60:0x0169, B:61:0x018d, B:63:0x0192, B:65:0x0198, B:68:0x01a1, B:70:0x01ad, B:71:0x01d1, B:74:0x01d7, B:75:0x01da, B:83:0x01ed, B:84:0x01f1, B:87:0x01f7, B:88:0x0202), top: B:100:0x00ae }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public AccessibilityNodeInfo findAccessibilityNodeInfoByAccessibilityId(int connectionId, int accessibilityWindowId, long accessibilityNodeId, boolean bypassCache, int prefetchFlags, Bundle arguments) {
        int prefetchFlags2;
        int prefetchFlags3;
        int descendantPrefetchFlags;
        String str;
        IAccessibilityServiceConnection connection;
        AccessibilityInteractionClient accessibilityInteractionClient;
        int prefetchFlags4 = prefetchFlags;
        try {
            IAccessibilityServiceConnection connection2 = getConnection(connectionId);
            if (connection2 == null) {
                return null;
            }
            if (bypassCache) {
                prefetchFlags4 &= -64;
            } else {
                try {
                    AccessibilityCache cache = getCache(connectionId);
                    if (cache != null) {
                        AccessibilityNodeInfo cachedInfo = cache.getNode(accessibilityWindowId, accessibilityNodeId);
                        if (cachedInfo != null) {
                            if (shouldTraceClient()) {
                                logTraceClient(connection2, "findAccessibilityNodeInfoByAccessibilityId cache", "connectionId=" + connectionId + ";accessibilityWindowId=" + accessibilityWindowId + ";accessibilityNodeId=" + accessibilityNodeId + ";bypassCache=" + bypassCache + ";prefetchFlags=" + prefetchFlags4 + ";arguments=" + arguments);
                            }
                            return cachedInfo;
                        } else if (!cache.isEnabled()) {
                            prefetchFlags4 &= -64;
                        }
                    }
                } catch (RemoteException e) {
                    re = e;
                    prefetchFlags2 = prefetchFlags4;
                    Log.e("AccessibilityInteractionClient", "Error while calling remote findAccessibilityNodeInfoByAccessibilityId", re);
                    return null;
                }
            }
            try {
                if ((prefetchFlags4 & 63) != 0) {
                    try {
                        if (isWindowScrolling(accessibilityWindowId)) {
                            prefetchFlags3 = prefetchFlags4 & (-64);
                            descendantPrefetchFlags = prefetchFlags3 & 28;
                            if ((descendantPrefetchFlags & (descendantPrefetchFlags - 1)) == 0) {
                                throw new IllegalArgumentException("There can be no more than one descendant prefetching strategy");
                            }
                            try {
                                int interactionId = this.mInteractionIdCounter.getAndIncrement();
                                if (shouldTraceClient()) {
                                    try {
                                        accessibilityInteractionClient = this;
                                        str = "findAccessibilityNodeInfoByAccessibilityId";
                                        connection = connection2;
                                        try {
                                            accessibilityInteractionClient.logTraceClient(connection, str, "InteractionId:" + interactionId + "connectionId=" + connectionId + ";accessibilityWindowId=" + accessibilityWindowId + ";accessibilityNodeId=" + accessibilityNodeId + ";bypassCache=" + bypassCache + ";prefetchFlags=" + prefetchFlags3 + ";arguments=" + arguments);
                                        } catch (RemoteException e2) {
                                            re = e2;
                                            prefetchFlags2 = prefetchFlags3;
                                            Log.e("AccessibilityInteractionClient", "Error while calling remote findAccessibilityNodeInfoByAccessibilityId", re);
                                            return null;
                                        }
                                    } catch (RemoteException e3) {
                                        re = e3;
                                        prefetchFlags2 = prefetchFlags3;
                                    }
                                } else {
                                    str = "findAccessibilityNodeInfoByAccessibilityId";
                                    connection = connection2;
                                    accessibilityInteractionClient = this;
                                }
                                long identityToken = Binder.clearCallingIdentity();
                                try {
                                    String str2 = str;
                                    IAccessibilityServiceConnection connection3 = connection;
                                    int prefetchFlags5 = prefetchFlags3;
                                    try {
                                        String[] packageNames = connection.findAccessibilityNodeInfoByAccessibilityId(accessibilityWindowId, accessibilityNodeId, interactionId, this, prefetchFlags3, Thread.currentThread().getId(), arguments);
                                        Binder.restoreCallingIdentity(identityToken);
                                        if (packageNames != null) {
                                            if ((prefetchFlags5 & 32) == 0) {
                                                AccessibilityNodeInfo info = accessibilityInteractionClient.getFindAccessibilityNodeInfoResultAndClear(interactionId);
                                                if (shouldTraceCallback()) {
                                                    accessibilityInteractionClient.logTraceCallback(connection3, str2, "InteractionId:" + interactionId + ";connectionId=" + connectionId + ";Result: " + info);
                                                }
                                                if ((prefetchFlags5 & 63) != 0 && info != null) {
                                                    accessibilityInteractionClient.setInteractionWaitingForPrefetchResult(interactionId, connectionId, packageNames);
                                                }
                                                accessibilityInteractionClient.finalizeAndCacheAccessibilityNodeInfo(info, connectionId, bypassCache, packageNames);
                                                return info;
                                            }
                                            List<AccessibilityNodeInfo> infos = accessibilityInteractionClient.getFindAccessibilityNodeInfosResultAndClear(interactionId);
                                            if (shouldTraceCallback()) {
                                                accessibilityInteractionClient.logTraceCallback(connection3, str2, "InteractionId:" + interactionId + ";connectionId=" + connectionId + ";Result: " + infos);
                                            }
                                            accessibilityInteractionClient.finalizeAndCacheAccessibilityNodeInfos(infos, connectionId, bypassCache, packageNames);
                                            if (infos != null && !infos.isEmpty()) {
                                                return infos.get(0);
                                            }
                                        }
                                        return null;
                                    } catch (Throwable th) {
                                        th = th;
                                        Binder.restoreCallingIdentity(identityToken);
                                        throw th;
                                    }
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                            } catch (RemoteException e4) {
                                re = e4;
                                prefetchFlags2 = prefetchFlags3;
                            }
                        }
                    } catch (RemoteException e5) {
                        re = e5;
                        prefetchFlags2 = prefetchFlags4;
                        Log.e("AccessibilityInteractionClient", "Error while calling remote findAccessibilityNodeInfoByAccessibilityId", re);
                        return null;
                    }
                }
                if ((descendantPrefetchFlags & (descendantPrefetchFlags - 1)) == 0) {
                }
            } catch (RemoteException e6) {
                re = e6;
            }
            prefetchFlags3 = prefetchFlags4;
            descendantPrefetchFlags = prefetchFlags3 & 28;
        } catch (RemoteException e7) {
            re = e7;
            prefetchFlags2 = prefetchFlags4;
        }
    }

    private void setInteractionWaitingForPrefetchResult(int interactionId, int connectionId, String[] packageNames) {
        synchronized (this.mInstanceLock) {
            this.mInteractionIdWaitingForPrefetchResult = interactionId;
            this.mConnectionIdWaitingForPrefetchResult = connectionId;
            this.mPackageNamesForNextPrefetchResult = packageNames;
        }
    }

    private static String idToString(int accessibilityWindowId, long accessibilityNodeId) {
        return accessibilityWindowId + "/" + AccessibilityNodeInfo.idToString(accessibilityNodeId);
    }

    public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByViewId(int connectionId, int accessibilityWindowId, long accessibilityNodeId, String viewId) {
        try {
            IAccessibilityServiceConnection connection = getConnection(connectionId);
            if (connection != null) {
                int interactionId = this.mInteractionIdCounter.getAndIncrement();
                long identityToken = Binder.clearCallingIdentity();
                try {
                    if (shouldTraceClient()) {
                        try {
                            try {
                                try {
                                    try {
                                        logTraceClient(connection, "findAccessibilityNodeInfosByViewId", "InteractionId=" + interactionId + ";connectionId=" + connectionId + ";accessibilityWindowId=" + accessibilityWindowId + ";accessibilityNodeId=" + accessibilityNodeId + ";viewId=" + viewId);
                                    } catch (Throwable th) {
                                        th = th;
                                        Binder.restoreCallingIdentity(identityToken);
                                        throw th;
                                    }
                                } catch (Throwable th2) {
                                    th = th2;
                                    Binder.restoreCallingIdentity(identityToken);
                                    throw th;
                                }
                            } catch (Throwable th3) {
                                th = th3;
                                Binder.restoreCallingIdentity(identityToken);
                                throw th;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                        }
                    }
                    try {
                        String[] packageNames = connection.findAccessibilityNodeInfosByViewId(accessibilityWindowId, accessibilityNodeId, viewId, interactionId, this, Thread.currentThread().getId());
                        Binder.restoreCallingIdentity(identityToken);
                        if (packageNames != null) {
                            List<AccessibilityNodeInfo> infos = getFindAccessibilityNodeInfosResultAndClear(interactionId);
                            if (shouldTraceCallback()) {
                                logTraceCallback(connection, "findAccessibilityNodeInfosByViewId", "InteractionId=" + interactionId + ";connectionId=" + connectionId + ":Result: " + infos);
                            }
                            if (infos != null) {
                                finalizeAndCacheAccessibilityNodeInfos(infos, connectionId, false, packageNames);
                                return infos;
                            }
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        Binder.restoreCallingIdentity(identityToken);
                        throw th;
                    }
                } catch (Throwable th6) {
                    th = th6;
                }
            }
        } catch (RemoteException re) {
            Log.w("AccessibilityInteractionClient", "Error while calling remote findAccessibilityNodeInfoByViewIdInActiveWindow", re);
        }
        return Collections.emptyList();
    }

    public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(int connectionId, int accessibilityWindowId, long accessibilityNodeId, String text) {
        try {
            IAccessibilityServiceConnection connection = getConnection(connectionId);
            if (connection != null) {
                int interactionId = this.mInteractionIdCounter.getAndIncrement();
                if (shouldTraceClient()) {
                    try {
                        try {
                        } catch (RemoteException e) {
                            re = e;
                            Log.w("AccessibilityInteractionClient", "Error while calling remote findAccessibilityNodeInfosByViewText", re);
                            return Collections.emptyList();
                        }
                    } catch (RemoteException e2) {
                        re = e2;
                    }
                    try {
                    } catch (RemoteException e3) {
                        re = e3;
                        Log.w("AccessibilityInteractionClient", "Error while calling remote findAccessibilityNodeInfosByViewText", re);
                        return Collections.emptyList();
                    }
                    try {
                        logTraceClient(connection, "findAccessibilityNodeInfosByText", "InteractionId:" + interactionId + "connectionId=" + connectionId + ";accessibilityWindowId=" + accessibilityWindowId + ";accessibilityNodeId=" + accessibilityNodeId + ";text=" + text);
                    } catch (RemoteException e4) {
                        re = e4;
                        Log.w("AccessibilityInteractionClient", "Error while calling remote findAccessibilityNodeInfosByViewText", re);
                        return Collections.emptyList();
                    }
                }
                long identityToken = Binder.clearCallingIdentity();
                String[] packageNames = connection.findAccessibilityNodeInfosByText(accessibilityWindowId, accessibilityNodeId, text, interactionId, this, Thread.currentThread().getId());
                Binder.restoreCallingIdentity(identityToken);
                if (packageNames != null) {
                    List<AccessibilityNodeInfo> infos = getFindAccessibilityNodeInfosResultAndClear(interactionId);
                    if (shouldTraceCallback()) {
                        logTraceCallback(connection, "findAccessibilityNodeInfosByText", "InteractionId=" + interactionId + ";connectionId=" + connectionId + ";Result: " + infos);
                    }
                    if (infos != null) {
                        finalizeAndCacheAccessibilityNodeInfos(infos, connectionId, false, packageNames);
                        return infos;
                    }
                }
            }
        } catch (RemoteException e5) {
            re = e5;
        }
        return Collections.emptyList();
    }

    public AccessibilityNodeInfo findFocus(int connectionId, int accessibilityWindowId, long accessibilityNodeId, int focusType) {
        AccessibilityNodeInfo cachedInfo;
        try {
            IAccessibilityServiceConnection connection = getConnection(connectionId);
            if (connection != null) {
                AccessibilityCache cache = getCache(connectionId);
                if (cache != null && (cachedInfo = cache.getFocus(focusType, accessibilityNodeId, accessibilityWindowId)) != null) {
                    return cachedInfo;
                }
                int interactionId = this.mInteractionIdCounter.getAndIncrement();
                if (shouldTraceClient()) {
                    logTraceClient(connection, "findFocus", "InteractionId:" + interactionId + "connectionId=" + connectionId + ";accessibilityWindowId=" + accessibilityWindowId + ";accessibilityNodeId=" + accessibilityNodeId + ";focusType=" + focusType);
                }
                long identityToken = Binder.clearCallingIdentity();
                try {
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    String[] packageNames = connection.findFocus(accessibilityWindowId, accessibilityNodeId, focusType, interactionId, this, Thread.currentThread().getId());
                    Binder.restoreCallingIdentity(identityToken);
                    if (packageNames != null) {
                        AccessibilityNodeInfo info = getFindAccessibilityNodeInfoResultAndClear(interactionId);
                        if (shouldTraceCallback()) {
                            logTraceCallback(connection, "findFocus", "InteractionId=" + interactionId + ";connectionId=" + connectionId + ";Result:" + info);
                        }
                        finalizeAndCacheAccessibilityNodeInfo(info, connectionId, false, packageNames);
                        return info;
                    }
                    return null;
                } catch (Throwable th2) {
                    th = th2;
                    Binder.restoreCallingIdentity(identityToken);
                    throw th;
                }
            }
            return null;
        } catch (RemoteException re) {
            Log.w("AccessibilityInteractionClient", "Error while calling remote findFocus", re);
            return null;
        }
    }

    public AccessibilityNodeInfo focusSearch(int connectionId, int accessibilityWindowId, long accessibilityNodeId, int direction) {
        try {
            IAccessibilityServiceConnection connection = getConnection(connectionId);
            if (connection != null) {
                int interactionId = this.mInteractionIdCounter.getAndIncrement();
                if (shouldTraceClient()) {
                    try {
                        try {
                        } catch (RemoteException e) {
                            re = e;
                            Log.w("AccessibilityInteractionClient", "Error while calling remote accessibilityFocusSearch", re);
                            return null;
                        }
                    } catch (RemoteException e2) {
                        re = e2;
                    }
                    try {
                    } catch (RemoteException e3) {
                        re = e3;
                        Log.w("AccessibilityInteractionClient", "Error while calling remote accessibilityFocusSearch", re);
                        return null;
                    }
                    try {
                        logTraceClient(connection, "focusSearch", "InteractionId:" + interactionId + "connectionId=" + connectionId + ";accessibilityWindowId=" + accessibilityWindowId + ";accessibilityNodeId=" + accessibilityNodeId + ";direction=" + direction);
                    } catch (RemoteException e4) {
                        re = e4;
                        Log.w("AccessibilityInteractionClient", "Error while calling remote accessibilityFocusSearch", re);
                        return null;
                    }
                }
                long identityToken = Binder.clearCallingIdentity();
                String[] packageNames = connection.focusSearch(accessibilityWindowId, accessibilityNodeId, direction, interactionId, this, Thread.currentThread().getId());
                Binder.restoreCallingIdentity(identityToken);
                if (packageNames != null) {
                    AccessibilityNodeInfo info = getFindAccessibilityNodeInfoResultAndClear(interactionId);
                    finalizeAndCacheAccessibilityNodeInfo(info, connectionId, false, packageNames);
                    if (shouldTraceCallback()) {
                        logTraceCallback(connection, "focusSearch", "InteractionId=" + interactionId + ";connectionId=" + connectionId + ";Result:" + info);
                    }
                    return info;
                }
                return null;
            }
            return null;
        } catch (RemoteException e5) {
            re = e5;
        }
    }

    public boolean performAccessibilityAction(int connectionId, int accessibilityWindowId, long accessibilityNodeId, int action, Bundle arguments) {
        try {
            IAccessibilityServiceConnection connection = getConnection(connectionId);
            if (connection != null) {
                int interactionId = this.mInteractionIdCounter.getAndIncrement();
                if (shouldTraceClient()) {
                    try {
                        try {
                        } catch (RemoteException e) {
                            re = e;
                            Log.w("AccessibilityInteractionClient", "Error while calling remote performAccessibilityAction", re);
                            return false;
                        }
                    } catch (RemoteException e2) {
                        re = e2;
                    }
                    try {
                        try {
                            logTraceClient(connection, "performAccessibilityAction", "InteractionId:" + interactionId + "connectionId=" + connectionId + ";accessibilityWindowId=" + accessibilityWindowId + ";accessibilityNodeId=" + accessibilityNodeId + ";action=" + action + ";arguments=" + arguments);
                        } catch (RemoteException e3) {
                            re = e3;
                            Log.w("AccessibilityInteractionClient", "Error while calling remote performAccessibilityAction", re);
                            return false;
                        }
                    } catch (RemoteException e4) {
                        re = e4;
                        Log.w("AccessibilityInteractionClient", "Error while calling remote performAccessibilityAction", re);
                        return false;
                    }
                }
                long identityToken = Binder.clearCallingIdentity();
                boolean success = connection.performAccessibilityAction(accessibilityWindowId, accessibilityNodeId, action, arguments, interactionId, this, Thread.currentThread().getId());
                Binder.restoreCallingIdentity(identityToken);
                if (success) {
                    boolean result = getPerformAccessibilityActionResultAndClear(interactionId);
                    if (shouldTraceCallback()) {
                        logTraceCallback(connection, "performAccessibilityAction", "InteractionId=" + interactionId + ";connectionId=" + connectionId + ";Result: " + result);
                    }
                    return result;
                }
                return false;
            }
            return false;
        } catch (RemoteException e5) {
            re = e5;
        }
    }

    public void clearCache(int connectionId) {
        AccessibilityCache cache = getCache(connectionId);
        if (cache == null) {
            return;
        }
        cache.clear();
    }

    public void onAccessibilityEvent(AccessibilityEvent event, int connectionId) {
        switch (event.getEventType()) {
            case 4096:
                updateScrollingWindow(event.getWindowId(), SystemClock.uptimeMillis());
                break;
            case 4194304:
                if (event.getWindowChanges() == 2) {
                    deleteScrollingWindow(event.getWindowId());
                    break;
                }
                break;
        }
        AccessibilityCache cache = getCache(connectionId);
        if (cache == null) {
            return;
        }
        cache.onAccessibilityEvent(event);
    }

    private AccessibilityNodeInfo getFindAccessibilityNodeInfoResultAndClear(int interactionId) {
        AccessibilityNodeInfo result;
        synchronized (this.mInstanceLock) {
            boolean success = waitForResultTimedLocked(interactionId);
            result = success ? this.mFindAccessibilityNodeInfoResult : null;
            clearResultLocked();
        }
        return result;
    }

    @Override // android.view.accessibility.IAccessibilityInteractionConnectionCallback
    public void setFindAccessibilityNodeInfoResult(AccessibilityNodeInfo info, int interactionId) {
        synchronized (this.mInstanceLock) {
            if (interactionId > this.mInteractionId) {
                this.mFindAccessibilityNodeInfoResult = info;
                this.mInteractionId = interactionId;
                this.mCallingUid = Binder.getCallingUid();
                this.mCallStackOfCallback = new ArrayList(Arrays.asList(Thread.currentThread().getStackTrace()));
            }
            this.mInstanceLock.notifyAll();
        }
    }

    private List<AccessibilityNodeInfo> getFindAccessibilityNodeInfosResultAndClear(int interactionId) {
        List<AccessibilityNodeInfo> result;
        synchronized (this.mInstanceLock) {
            boolean success = waitForResultTimedLocked(interactionId);
            if (success) {
                result = this.mFindAccessibilityNodeInfosResult;
            } else {
                result = Collections.emptyList();
            }
            clearResultLocked();
            if (Build.IS_DEBUGGABLE) {
                checkFindAccessibilityNodeInfoResultIntegrity(result);
            }
        }
        return result;
    }

    @Override // android.view.accessibility.IAccessibilityInteractionConnectionCallback
    public void setFindAccessibilityNodeInfosResult(List<AccessibilityNodeInfo> infos, int interactionId) {
        synchronized (this.mInstanceLock) {
            if (interactionId > this.mInteractionId) {
                if (infos != null) {
                    boolean isIpcCall = Binder.getCallingPid() != Process.myPid();
                    if (!isIpcCall) {
                        this.mFindAccessibilityNodeInfosResult = new ArrayList(infos);
                    } else {
                        this.mFindAccessibilityNodeInfosResult = infos;
                    }
                } else {
                    this.mFindAccessibilityNodeInfosResult = Collections.emptyList();
                }
                this.mInteractionId = interactionId;
                this.mCallingUid = Binder.getCallingUid();
                this.mCallStackOfCallback = new ArrayList(Arrays.asList(Thread.currentThread().getStackTrace()));
            }
            this.mInstanceLock.notifyAll();
        }
    }

    @Override // android.view.accessibility.IAccessibilityInteractionConnectionCallback
    public void setPrefetchAccessibilityNodeInfoResult(List<AccessibilityNodeInfo> infos, int interactionId) {
        int interactionIdWaitingForPrefetchResultCopy = -1;
        int connectionIdWaitingForPrefetchResultCopy = -1;
        String[] packageNamesForNextPrefetchResultCopy = null;
        if (infos.isEmpty()) {
            return;
        }
        synchronized (this.mInstanceLock) {
            int i = this.mInteractionIdWaitingForPrefetchResult;
            if (i == interactionId) {
                interactionIdWaitingForPrefetchResultCopy = i;
                connectionIdWaitingForPrefetchResultCopy = this.mConnectionIdWaitingForPrefetchResult;
                String[] strArr = this.mPackageNamesForNextPrefetchResult;
                if (strArr != null) {
                    packageNamesForNextPrefetchResultCopy = new String[strArr.length];
                    int i2 = 0;
                    while (true) {
                        String[] strArr2 = this.mPackageNamesForNextPrefetchResult;
                        if (i2 >= strArr2.length) {
                            break;
                        }
                        packageNamesForNextPrefetchResultCopy[i2] = strArr2[i2];
                        i2++;
                    }
                }
            }
        }
        if (interactionIdWaitingForPrefetchResultCopy == interactionId) {
            finalizeAndCacheAccessibilityNodeInfos(infos, connectionIdWaitingForPrefetchResultCopy, false, packageNamesForNextPrefetchResultCopy);
            if (shouldTraceCallback()) {
                logTrace(getConnection(connectionIdWaitingForPrefetchResultCopy), "setPrefetchAccessibilityNodeInfoResult", "InteractionId:" + interactionId + ";connectionId=" + connectionIdWaitingForPrefetchResultCopy + ";Result: " + infos, Binder.getCallingUid(), Arrays.asList(Thread.currentThread().getStackTrace()), new HashSet<>(Collections.singletonList("getStackTrace")), 32L);
            }
        }
    }

    private boolean getPerformAccessibilityActionResultAndClear(int interactionId) {
        boolean result;
        synchronized (this.mInstanceLock) {
            boolean success = waitForResultTimedLocked(interactionId);
            result = success ? this.mPerformAccessibilityActionResult : false;
            clearResultLocked();
        }
        return result;
    }

    @Override // android.view.accessibility.IAccessibilityInteractionConnectionCallback
    public void setPerformAccessibilityActionResult(boolean succeeded, int interactionId) {
        synchronized (this.mInstanceLock) {
            if (interactionId > this.mInteractionId) {
                this.mPerformAccessibilityActionResult = succeeded;
                this.mInteractionId = interactionId;
                this.mCallingUid = Binder.getCallingUid();
                this.mCallStackOfCallback = new ArrayList(Arrays.asList(Thread.currentThread().getStackTrace()));
            }
            this.mInstanceLock.notifyAll();
        }
    }

    private void clearResultLocked() {
        this.mInteractionId = -1;
        this.mFindAccessibilityNodeInfoResult = null;
        this.mFindAccessibilityNodeInfosResult = null;
        this.mPerformAccessibilityActionResult = false;
    }

    private boolean waitForResultTimedLocked(int interactionId) {
        long startTimeMillis = SystemClock.uptimeMillis();
        while (true) {
            try {
                Message sameProcessMessage = getSameProcessMessageAndClear();
                if (sameProcessMessage != null) {
                    sameProcessMessage.getTarget().handleMessage(sameProcessMessage);
                }
            } catch (InterruptedException e) {
            }
            if (this.mInteractionId == interactionId) {
                return true;
            }
            if (this.mInteractionId > interactionId) {
                return false;
            }
            long elapsedTimeMillis = SystemClock.uptimeMillis() - startTimeMillis;
            long waitTimeMillis = 5000 - elapsedTimeMillis;
            if (waitTimeMillis <= 0) {
                return false;
            }
            this.mInstanceLock.wait(waitTimeMillis);
        }
    }

    private void finalizeAndCacheAccessibilityNodeInfo(AccessibilityNodeInfo info, int connectionId, boolean bypassCache, String[] packageNames) {
        AccessibilityCache cache;
        CharSequence packageName;
        if (info != null) {
            info.setConnectionId(connectionId);
            if (!ArrayUtils.isEmpty(packageNames) && ((packageName = info.getPackageName()) == null || !ArrayUtils.contains(packageNames, packageName.toString()))) {
                info.setPackageName(packageNames[0]);
            }
            info.setSealed(true);
            if (bypassCache || (cache = getCache(connectionId)) == null) {
                return;
            }
            cache.add(info);
        }
    }

    private void finalizeAndCacheAccessibilityNodeInfos(List<AccessibilityNodeInfo> infos, int connectionId, boolean bypassCache, String[] packageNames) {
        if (infos != null) {
            int infosCount = infos.size();
            for (int i = 0; i < infosCount; i++) {
                AccessibilityNodeInfo info = infos.get(i);
                finalizeAndCacheAccessibilityNodeInfo(info, connectionId, bypassCache, packageNames);
            }
        }
    }

    private Message getSameProcessMessageAndClear() {
        Message result;
        synchronized (this.mInstanceLock) {
            result = this.mSameThreadMessage;
            this.mSameThreadMessage = null;
        }
        return result;
    }

    private void checkFindAccessibilityNodeInfoResultIntegrity(List<AccessibilityNodeInfo> infos) {
        if (infos.size() == 0) {
            return;
        }
        AccessibilityNodeInfo root = infos.get(0);
        int infoCount = infos.size();
        for (int i = 1; i < infoCount; i++) {
            int j = i;
            while (true) {
                if (j < infoCount) {
                    AccessibilityNodeInfo candidate = infos.get(j);
                    if (root.getParentNodeId() != candidate.getSourceNodeId()) {
                        j++;
                    } else {
                        root = candidate;
                        break;
                    }
                }
            }
        }
        if (root == null) {
            Log.e("AccessibilityInteractionClient", "No root.");
        }
        HashSet<AccessibilityNodeInfo> seen = new HashSet<>();
        Queue<AccessibilityNodeInfo> fringe = new ArrayDeque<>();
        fringe.add(root);
        while (!fringe.isEmpty()) {
            AccessibilityNodeInfo current = fringe.poll();
            if (!seen.add(current)) {
                Log.e("AccessibilityInteractionClient", "Duplicate node.");
                return;
            }
            int childCount = current.getChildCount();
            for (int i2 = 0; i2 < childCount; i2++) {
                long childId = current.getChildId(i2);
                for (int j2 = 0; j2 < infoCount; j2++) {
                    AccessibilityNodeInfo child = infos.get(j2);
                    if (child.getSourceNodeId() == childId) {
                        fringe.add(child);
                    }
                }
            }
        }
        int disconnectedCount = infos.size() - seen.size();
        if (disconnectedCount > 0) {
            Log.e("AccessibilityInteractionClient", disconnectedCount + " Disconnected nodes.");
        }
    }

    private void updateScrollingWindow(int windowId, long uptimeMillis) {
        SparseLongArray sparseLongArray = sScrollingWindows;
        synchronized (sparseLongArray) {
            sparseLongArray.put(windowId, uptimeMillis);
        }
    }

    private void deleteScrollingWindow(int windowId) {
        SparseLongArray sparseLongArray = sScrollingWindows;
        synchronized (sparseLongArray) {
            sparseLongArray.delete(windowId);
        }
    }

    private boolean isWindowScrolling(int windowId) {
        SparseLongArray sparseLongArray = sScrollingWindows;
        synchronized (sparseLongArray) {
            long latestScrollingTime = sparseLongArray.get(windowId);
            if (latestScrollingTime == 0) {
                return false;
            }
            long currentUptime = SystemClock.uptimeMillis();
            if (currentUptime > DISABLE_PREFETCHING_FOR_SCROLLING_MILLIS + latestScrollingTime) {
                sparseLongArray.delete(windowId);
                return false;
            }
            return true;
        }
    }

    private boolean shouldTraceClient() {
        AccessibilityManager accessibilityManager = this.mAccessibilityManager;
        return accessibilityManager != null && accessibilityManager.isA11yInteractionClientTraceEnabled();
    }

    private boolean shouldTraceCallback() {
        AccessibilityManager accessibilityManager = this.mAccessibilityManager;
        return accessibilityManager != null && accessibilityManager.isA11yInteractionConnectionCBTraceEnabled();
    }

    private void logTrace(IAccessibilityServiceConnection connection, String method, String params, int callingUid, List<StackTraceElement> callStack, HashSet<String> ignoreSet, long logTypes) {
        try {
            Bundle b = new Bundle();
            try {
                b.putSerializable(CALL_STACK, new ArrayList(callStack));
                if (ignoreSet != null) {
                    b.putSerializable(IGNORE_CALL_STACK, ignoreSet);
                }
                try {
                    connection.logTrace(SystemClock.elapsedRealtimeNanos(), "AccessibilityInteractionClient." + method, logTypes, params, Process.myPid(), Thread.currentThread().getId(), callingUid, b);
                } catch (RemoteException e) {
                    e = e;
                    Log.e("AccessibilityInteractionClient", "Failed to log trace. " + e);
                }
            } catch (RemoteException e2) {
                e = e2;
            }
        } catch (RemoteException e3) {
            e = e3;
        }
    }

    private void logTraceCallback(IAccessibilityServiceConnection connection, String method, String params) {
        logTrace(connection, method + " callback", params, this.mCallingUid, this.mCallStackOfCallback, new HashSet<>(Arrays.asList("getStackTrace")), 32L);
    }

    private void logTraceClient(IAccessibilityServiceConnection connection, String method, String params) {
        logTrace(connection, method, params, Binder.getCallingUid(), Arrays.asList(Thread.currentThread().getStackTrace()), new HashSet<>(Arrays.asList("getStackTrace", "logTraceClient")), 262144L);
    }
}
