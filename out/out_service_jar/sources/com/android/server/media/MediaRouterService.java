package com.android.server.media;

import android.app.ActivityManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioRoutesInfo;
import android.media.IAudioRoutesObserver;
import android.media.IAudioService;
import android.media.IMediaRouter2;
import android.media.IMediaRouter2Manager;
import android.media.IMediaRouterClient;
import android.media.IMediaRouterService;
import android.media.MediaRoute2Info;
import android.media.MediaRouterClientState;
import android.media.RemoteDisplayState;
import android.media.RouteDiscoveryPreference;
import android.media.RoutingSessionInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.IntArray;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import com.android.internal.util.DumpUtils;
import com.android.server.Watchdog;
import com.android.server.media.AudioPlayerStateMonitor;
import com.android.server.media.RemoteDisplayProviderProxy;
import com.android.server.media.RemoteDisplayProviderWatcher;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
/* loaded from: classes2.dex */
public final class MediaRouterService extends IMediaRouterService.Stub implements Watchdog.Monitor {
    static final long CONNECTED_TIMEOUT = 60000;
    static final long CONNECTING_TIMEOUT = 5000;
    BluetoothDevice mActiveBluetoothDevice;
    private final IntArray mActivePlayerMinPriorityQueue;
    private final IntArray mActivePlayerUidMinPriorityQueue;
    private final AudioPlayerStateMonitor mAudioPlayerStateMonitor;
    int mAudioRouteMainType;
    private final IAudioService mAudioService;
    private final String mBluetoothA2dpRouteId;
    private final Context mContext;
    private final String mDefaultAudioRouteId;
    boolean mGlobalBluetoothA2dpOn;
    private final Handler mHandler;
    private final BroadcastReceiver mReceiver;
    private final MediaRouter2ServiceImpl mService2;
    private static final String TAG = "MediaRouterService";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private final Object mLock = new Object();
    private final SparseArray<UserRecord> mUserRecords = new SparseArray<>();
    private final ArrayMap<IBinder, ClientRecord> mAllClientRecords = new ArrayMap<>();
    private int mCurrentUserId = -1;

    public MediaRouterService(Context context) {
        Handler handler = new Handler();
        this.mHandler = handler;
        this.mActivePlayerMinPriorityQueue = new IntArray();
        this.mActivePlayerUidMinPriorityQueue = new IntArray();
        this.mReceiver = new MediaRouterServiceBroadcastReceiver();
        this.mAudioRouteMainType = 0;
        this.mGlobalBluetoothA2dpOn = false;
        this.mService2 = new MediaRouter2ServiceImpl(context);
        this.mContext = context;
        Watchdog.getInstance().addMonitor(this);
        Resources res = context.getResources();
        this.mDefaultAudioRouteId = res.getString(17040134);
        this.mBluetoothA2dpRouteId = res.getString(17039813);
        IAudioService asInterface = IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
        this.mAudioService = asInterface;
        AudioPlayerStateMonitor audioPlayerStateMonitor = AudioPlayerStateMonitor.getInstance(context);
        this.mAudioPlayerStateMonitor = audioPlayerStateMonitor;
        audioPlayerStateMonitor.registerListener(new AudioPlayerStateMonitor.OnAudioPlayerActiveStateChangedListener() { // from class: com.android.server.media.MediaRouterService.1
            static final long WAIT_MS = 500;
            final Runnable mRestoreBluetoothA2dpRunnable = new Runnable() { // from class: com.android.server.media.MediaRouterService.1.1
                @Override // java.lang.Runnable
                public void run() {
                    MediaRouterService.this.restoreBluetoothA2dp();
                }
            };

            @Override // com.android.server.media.AudioPlayerStateMonitor.OnAudioPlayerActiveStateChangedListener
            public void onAudioPlayerActiveStateChanged(AudioPlaybackConfiguration config, boolean isRemoved) {
                boolean active = !isRemoved && config.isActive();
                int pii = config.getPlayerInterfaceId();
                int uid = config.getClientUid();
                int idx = MediaRouterService.this.mActivePlayerMinPriorityQueue.indexOf(pii);
                if (idx >= 0) {
                    MediaRouterService.this.mActivePlayerMinPriorityQueue.remove(idx);
                    MediaRouterService.this.mActivePlayerUidMinPriorityQueue.remove(idx);
                }
                int restoreUid = -1;
                if (active) {
                    MediaRouterService.this.mActivePlayerMinPriorityQueue.add(config.getPlayerInterfaceId());
                    MediaRouterService.this.mActivePlayerUidMinPriorityQueue.add(uid);
                    restoreUid = uid;
                } else if (MediaRouterService.this.mActivePlayerUidMinPriorityQueue.size() > 0) {
                    restoreUid = MediaRouterService.this.mActivePlayerUidMinPriorityQueue.get(MediaRouterService.this.mActivePlayerUidMinPriorityQueue.size() - 1);
                }
                MediaRouterService.this.mHandler.removeCallbacks(this.mRestoreBluetoothA2dpRunnable);
                if (restoreUid >= 0) {
                    MediaRouterService.this.restoreRoute(restoreUid);
                    if (MediaRouterService.DEBUG) {
                        Slog.d(MediaRouterService.TAG, "onAudioPlayerActiveStateChanged: uid=" + uid + ", active=" + active + ", restoreUid=" + restoreUid);
                        return;
                    }
                    return;
                }
                MediaRouterService.this.mHandler.postDelayed(this.mRestoreBluetoothA2dpRunnable, 500L);
                if (MediaRouterService.DEBUG) {
                    Slog.d(MediaRouterService.TAG, "onAudioPlayerActiveStateChanged: uid=" + uid + ", active=" + active + ", delaying");
                }
            }
        }, handler);
        try {
            asInterface.startWatchingRoutes(new IAudioRoutesObserver.Stub() { // from class: com.android.server.media.MediaRouterService.2
                public void dispatchAudioRoutesChanged(AudioRoutesInfo newRoutes) {
                    synchronized (MediaRouterService.this.mLock) {
                        if (newRoutes.mainType != MediaRouterService.this.mAudioRouteMainType) {
                            boolean z = false;
                            if ((newRoutes.mainType & 19) == 0) {
                                MediaRouterService.this.mGlobalBluetoothA2dpOn = (newRoutes.bluetoothName == null && MediaRouterService.this.mActiveBluetoothDevice == null) ? true : true;
                            } else {
                                MediaRouterService.this.mGlobalBluetoothA2dpOn = false;
                            }
                            MediaRouterService.this.mAudioRouteMainType = newRoutes.mainType;
                        }
                    }
                }
            });
        } catch (RemoteException e) {
            Slog.w(TAG, "RemoteException in the audio service.");
        }
        IntentFilter intentFilter = new IntentFilter("android.bluetooth.a2dp.profile.action.ACTIVE_DEVICE_CHANGED");
        context.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, intentFilter, null, null);
    }

    public void systemRunning() {
        IntentFilter filter = new IntentFilter("android.intent.action.USER_SWITCHED");
        this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.media.MediaRouterService.3
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.intent.action.USER_SWITCHED")) {
                    MediaRouterService.this.switchUser();
                }
            }
        }, filter);
        switchUser();
    }

    @Override // com.android.server.Watchdog.Monitor
    public void monitor() {
        synchronized (this.mLock) {
        }
    }

    public void registerClientAsUser(IMediaRouterClient client, String packageName, int userId) {
        if (client == null) {
            throw new IllegalArgumentException("client must not be null");
        }
        int uid = Binder.getCallingUid();
        if (!validatePackageName(uid, packageName)) {
            throw new SecurityException("packageName must match the calling uid");
        }
        int pid = Binder.getCallingPid();
        int resolvedUserId = ActivityManager.handleIncomingUser(pid, uid, userId, false, true, "registerClientAsUser", packageName);
        boolean trusted = this.mContext.checkCallingOrSelfPermission("android.permission.CONFIGURE_WIFI_DISPLAY") == 0;
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                registerClientLocked(client, uid, pid, packageName, resolvedUserId, trusted);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void registerClientGroupId(IMediaRouterClient client, String groupId) {
        if (client == null) {
            throw new NullPointerException("client must not be null");
        }
        if (this.mContext.checkCallingOrSelfPermission("android.permission.CONFIGURE_WIFI_DISPLAY") != 0) {
            Log.w(TAG, "Ignoring client group request because the client doesn't have the CONFIGURE_WIFI_DISPLAY permission.");
            return;
        }
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                registerClientGroupIdLocked(client, groupId);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void unregisterClient(IMediaRouterClient client) {
        if (client == null) {
            throw new IllegalArgumentException("client must not be null");
        }
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                unregisterClientLocked(client, false);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public MediaRouterClientState getState(IMediaRouterClient client) {
        MediaRouterClientState stateLocked;
        if (client == null) {
            throw new IllegalArgumentException("client must not be null");
        }
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                stateLocked = getStateLocked(client);
            }
            return stateLocked;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public boolean isPlaybackActive(IMediaRouterClient client) {
        ClientRecord clientRecord;
        if (client == null) {
            throw new IllegalArgumentException("client must not be null");
        }
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                clientRecord = this.mAllClientRecords.get(client.asBinder());
            }
            if (clientRecord != null) {
                return this.mAudioPlayerStateMonitor.isPlaybackActive(clientRecord.mUid);
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void setBluetoothA2dpOn(IMediaRouterClient client, boolean on) {
        if (client == null) {
            throw new IllegalArgumentException("client must not be null");
        }
        long token = Binder.clearCallingIdentity();
        try {
            try {
                this.mAudioService.setBluetoothA2dpOn(on);
            } catch (RemoteException e) {
                Slog.w(TAG, "RemoteException while calling setBluetoothA2dpOn. on=" + on);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void setDiscoveryRequest(IMediaRouterClient client, int routeTypes, boolean activeScan) {
        if (client == null) {
            throw new IllegalArgumentException("client must not be null");
        }
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                setDiscoveryRequestLocked(client, routeTypes, activeScan);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void setSelectedRoute(IMediaRouterClient client, String routeId, boolean explicit) {
        if (client == null) {
            throw new IllegalArgumentException("client must not be null");
        }
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                setSelectedRouteLocked(client, routeId, explicit);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void requestSetVolume(IMediaRouterClient client, String routeId, int volume) {
        if (client == null) {
            throw new IllegalArgumentException("client must not be null");
        }
        if (routeId == null) {
            throw new IllegalArgumentException("routeId must not be null");
        }
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                requestSetVolumeLocked(client, routeId, volume);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void requestUpdateVolume(IMediaRouterClient client, String routeId, int direction) {
        if (client == null) {
            throw new IllegalArgumentException("client must not be null");
        }
        if (routeId == null) {
            throw new IllegalArgumentException("routeId must not be null");
        }
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                requestUpdateVolumeLocked(client, routeId, direction);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            pw.println("MEDIA ROUTER SERVICE (dumpsys media_router)");
            pw.println();
            pw.println("Global state");
            pw.println("  mCurrentUserId=" + this.mCurrentUserId);
            synchronized (this.mLock) {
                int count = this.mUserRecords.size();
                for (int i = 0; i < count; i++) {
                    UserRecord userRecord = this.mUserRecords.valueAt(i);
                    pw.println();
                    userRecord.dump(pw, "");
                }
            }
        }
    }

    public void enforceMediaContentControlPermission() {
        this.mService2.enforceMediaContentControlPermission();
    }

    public List<MediaRoute2Info> getSystemRoutes() {
        return this.mService2.getSystemRoutes();
    }

    public RoutingSessionInfo getSystemSessionInfo() {
        return this.mService2.getSystemSessionInfo(null, false);
    }

    public void registerRouter2(IMediaRouter2 router, String packageName) {
        int uid = Binder.getCallingUid();
        if (!validatePackageName(uid, packageName)) {
            throw new SecurityException("packageName must match the calling uid");
        }
        this.mService2.registerRouter2(router, packageName);
    }

    public void unregisterRouter2(IMediaRouter2 router) {
        this.mService2.unregisterRouter2(router);
    }

    public void setDiscoveryRequestWithRouter2(IMediaRouter2 router, RouteDiscoveryPreference request) {
        this.mService2.setDiscoveryRequestWithRouter2(router, request);
    }

    public void setRouteVolumeWithRouter2(IMediaRouter2 router, MediaRoute2Info route, int volume) {
        this.mService2.setRouteVolumeWithRouter2(router, route, volume);
    }

    public void requestCreateSessionWithRouter2(IMediaRouter2 router, int requestId, long managerRequestId, RoutingSessionInfo oldSession, MediaRoute2Info route, Bundle sessionHints) {
        this.mService2.requestCreateSessionWithRouter2(router, requestId, managerRequestId, oldSession, route, sessionHints);
    }

    public void selectRouteWithRouter2(IMediaRouter2 router, String sessionId, MediaRoute2Info route) {
        this.mService2.selectRouteWithRouter2(router, sessionId, route);
    }

    public void deselectRouteWithRouter2(IMediaRouter2 router, String sessionId, MediaRoute2Info route) {
        this.mService2.deselectRouteWithRouter2(router, sessionId, route);
    }

    public void transferToRouteWithRouter2(IMediaRouter2 router, String sessionId, MediaRoute2Info route) {
        this.mService2.transferToRouteWithRouter2(router, sessionId, route);
    }

    public void setSessionVolumeWithRouter2(IMediaRouter2 router, String sessionId, int volume) {
        this.mService2.setSessionVolumeWithRouter2(router, sessionId, volume);
    }

    public void releaseSessionWithRouter2(IMediaRouter2 router, String sessionId) {
        this.mService2.releaseSessionWithRouter2(router, sessionId);
    }

    public List<RoutingSessionInfo> getRemoteSessions(IMediaRouter2Manager manager) {
        return this.mService2.getRemoteSessions(manager);
    }

    public RoutingSessionInfo getSystemSessionInfoForPackage(IMediaRouter2Manager manager, String packageName) {
        int uid = Binder.getCallingUid();
        int userId = UserHandle.getUserHandleForUid(uid).getIdentifier();
        boolean setDeviceRouteSelected = false;
        synchronized (this.mLock) {
            UserRecord userRecord = this.mUserRecords.get(userId);
            List<ClientRecord> userClientRecords = userRecord != null ? userRecord.mClientRecords : Collections.emptyList();
            Iterator<ClientRecord> it = userClientRecords.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ClientRecord clientRecord = it.next();
                if (TextUtils.equals(clientRecord.mPackageName, packageName) && this.mDefaultAudioRouteId.equals(clientRecord.mSelectedRouteId)) {
                    setDeviceRouteSelected = true;
                    break;
                }
            }
        }
        return this.mService2.getSystemSessionInfo(packageName, setDeviceRouteSelected);
    }

    public void registerManager(IMediaRouter2Manager manager, String packageName) {
        int uid = Binder.getCallingUid();
        if (!validatePackageName(uid, packageName)) {
            throw new SecurityException("packageName must match the calling uid");
        }
        this.mService2.registerManager(manager, packageName);
    }

    public void unregisterManager(IMediaRouter2Manager manager) {
        this.mService2.unregisterManager(manager);
    }

    public void startScan(IMediaRouter2Manager manager) {
        this.mService2.startScan(manager);
    }

    public void stopScan(IMediaRouter2Manager manager) {
        this.mService2.stopScan(manager);
    }

    public void setRouteVolumeWithManager(IMediaRouter2Manager manager, int requestId, MediaRoute2Info route, int volume) {
        this.mService2.setRouteVolumeWithManager(manager, requestId, route, volume);
    }

    public void requestCreateSessionWithManager(IMediaRouter2Manager manager, int requestId, RoutingSessionInfo oldSession, MediaRoute2Info route) {
        this.mService2.requestCreateSessionWithManager(manager, requestId, oldSession, route);
    }

    public void selectRouteWithManager(IMediaRouter2Manager manager, int requestId, String sessionId, MediaRoute2Info route) {
        this.mService2.selectRouteWithManager(manager, requestId, sessionId, route);
    }

    public void deselectRouteWithManager(IMediaRouter2Manager manager, int requestId, String sessionId, MediaRoute2Info route) {
        this.mService2.deselectRouteWithManager(manager, requestId, sessionId, route);
    }

    public void transferToRouteWithManager(IMediaRouter2Manager manager, int requestId, String sessionId, MediaRoute2Info route) {
        this.mService2.transferToRouteWithManager(manager, requestId, sessionId, route);
    }

    public void setSessionVolumeWithManager(IMediaRouter2Manager manager, int requestId, String sessionId, int volume) {
        this.mService2.setSessionVolumeWithManager(manager, requestId, sessionId, volume);
    }

    public void releaseSessionWithManager(IMediaRouter2Manager manager, int requestId, String sessionId) {
        this.mService2.releaseSessionWithManager(manager, requestId, sessionId);
    }

    void restoreBluetoothA2dp() {
        boolean a2dpOn;
        BluetoothDevice btDevice;
        try {
            synchronized (this.mLock) {
                a2dpOn = this.mGlobalBluetoothA2dpOn;
                btDevice = this.mActiveBluetoothDevice;
            }
            if (btDevice != null) {
                if (DEBUG) {
                    Slog.d(TAG, "restoreBluetoothA2dp(" + a2dpOn + ")");
                }
                this.mAudioService.setBluetoothA2dpOn(a2dpOn);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "RemoteException while calling setBluetoothA2dpOn.");
        }
    }

    void restoreRoute(int uid) {
        ClientRecord clientRecord = null;
        synchronized (this.mLock) {
            UserRecord userRecord = this.mUserRecords.get(UserHandle.getUserHandleForUid(uid).getIdentifier());
            if (userRecord != null && userRecord.mClientRecords != null) {
                Iterator<ClientRecord> it = userRecord.mClientRecords.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    ClientRecord cr = it.next();
                    if (validatePackageName(uid, cr.mPackageName)) {
                        clientRecord = cr;
                        break;
                    }
                }
            }
        }
        if (clientRecord != null) {
            try {
                clientRecord.mClient.onRestoreRoute();
                return;
            } catch (RemoteException e) {
                Slog.w(TAG, "Failed to call onRestoreRoute. Client probably died.");
                return;
            }
        }
        restoreBluetoothA2dp();
    }

    void switchUser() {
        synchronized (this.mLock) {
            int userId = ActivityManager.getCurrentUser();
            int oldUserId = this.mCurrentUserId;
            if (oldUserId != userId) {
                this.mCurrentUserId = userId;
                UserRecord oldUser = this.mUserRecords.get(oldUserId);
                if (oldUser != null) {
                    oldUser.mHandler.sendEmptyMessage(2);
                    disposeUserIfNeededLocked(oldUser);
                }
                UserRecord newUser = this.mUserRecords.get(userId);
                if (newUser != null) {
                    newUser.mHandler.sendEmptyMessage(1);
                }
            }
        }
        this.mService2.switchUser();
    }

    void clientDied(ClientRecord clientRecord) {
        synchronized (this.mLock) {
            unregisterClientLocked(clientRecord.mClient, true);
        }
    }

    private void registerClientLocked(IMediaRouterClient client, int uid, int pid, String packageName, int userId, boolean trusted) {
        boolean newUser;
        UserRecord userRecord;
        IBinder binder = client.asBinder();
        if (this.mAllClientRecords.get(binder) == null) {
            UserRecord userRecord2 = this.mUserRecords.get(userId);
            if (userRecord2 != null) {
                newUser = false;
                userRecord = userRecord2;
            } else {
                newUser = true;
                userRecord = new UserRecord(userId);
            }
            ClientRecord clientRecord = new ClientRecord(userRecord, client, uid, pid, packageName, trusted);
            try {
                binder.linkToDeath(clientRecord, 0);
                if (newUser) {
                    this.mUserRecords.put(userId, userRecord);
                    initializeUserLocked(userRecord);
                }
                userRecord.mClientRecords.add(clientRecord);
                this.mAllClientRecords.put(binder, clientRecord);
                initializeClientLocked(clientRecord);
            } catch (RemoteException ex) {
                throw new RuntimeException("Media router client died prematurely.", ex);
            }
        }
    }

    private void registerClientGroupIdLocked(IMediaRouterClient client, String groupId) {
        IBinder binder = client.asBinder();
        ClientRecord clientRecord = this.mAllClientRecords.get(binder);
        if (clientRecord == null) {
            Log.w(TAG, "Ignoring group id register request of a unregistered client.");
        } else if (TextUtils.equals(clientRecord.mGroupId, groupId)) {
        } else {
            UserRecord userRecord = clientRecord.mUserRecord;
            if (clientRecord.mGroupId != null) {
                userRecord.removeFromGroup(clientRecord.mGroupId, clientRecord);
            }
            clientRecord.mGroupId = groupId;
            if (groupId != null) {
                userRecord.addToGroup(groupId, clientRecord);
                userRecord.mHandler.obtainMessage(10, groupId).sendToTarget();
            }
        }
    }

    private void unregisterClientLocked(IMediaRouterClient client, boolean died) {
        ClientRecord clientRecord = this.mAllClientRecords.remove(client.asBinder());
        if (clientRecord != null) {
            UserRecord userRecord = clientRecord.mUserRecord;
            userRecord.mClientRecords.remove(clientRecord);
            if (clientRecord.mGroupId != null) {
                userRecord.removeFromGroup(clientRecord.mGroupId, clientRecord);
                clientRecord.mGroupId = null;
            }
            disposeClientLocked(clientRecord, died);
            disposeUserIfNeededLocked(userRecord);
        }
    }

    private MediaRouterClientState getStateLocked(IMediaRouterClient client) {
        ClientRecord clientRecord = this.mAllClientRecords.get(client.asBinder());
        if (clientRecord != null) {
            return clientRecord.getState();
        }
        return null;
    }

    private void setDiscoveryRequestLocked(IMediaRouterClient client, int routeTypes, boolean activeScan) {
        IBinder binder = client.asBinder();
        ClientRecord clientRecord = this.mAllClientRecords.get(binder);
        if (clientRecord != null) {
            if (!clientRecord.mTrusted) {
                routeTypes &= -5;
            }
            if (clientRecord.mRouteTypes != routeTypes || clientRecord.mActiveScan != activeScan) {
                if (DEBUG) {
                    Slog.d(TAG, clientRecord + ": Set discovery request, routeTypes=0x" + Integer.toHexString(routeTypes) + ", activeScan=" + activeScan);
                }
                clientRecord.mRouteTypes = routeTypes;
                clientRecord.mActiveScan = activeScan;
                clientRecord.mUserRecord.mHandler.sendEmptyMessage(3);
            }
        }
    }

    private void setSelectedRouteLocked(IMediaRouterClient client, String routeId, boolean explicit) {
        ClientGroup group;
        ClientRecord clientRecord = this.mAllClientRecords.get(client.asBinder());
        if (clientRecord != null) {
            String oldRouteId = (this.mDefaultAudioRouteId.equals(clientRecord.mSelectedRouteId) || this.mBluetoothA2dpRouteId.equals(clientRecord.mSelectedRouteId)) ? null : clientRecord.mSelectedRouteId;
            clientRecord.mSelectedRouteId = routeId;
            routeId = (this.mDefaultAudioRouteId.equals(routeId) || this.mBluetoothA2dpRouteId.equals(routeId)) ? null : null;
            if (!Objects.equals(routeId, oldRouteId)) {
                if (DEBUG) {
                    Slog.d(TAG, clientRecord + ": Set selected route, routeId=" + routeId + ", oldRouteId=" + oldRouteId + ", explicit=" + explicit);
                }
                if (explicit && clientRecord.mTrusted) {
                    if (oldRouteId != null) {
                        clientRecord.mUserRecord.mHandler.obtainMessage(5, oldRouteId).sendToTarget();
                    }
                    if (routeId != null) {
                        clientRecord.mUserRecord.mHandler.obtainMessage(4, routeId).sendToTarget();
                    }
                    if (clientRecord.mGroupId != null && (group = (ClientGroup) clientRecord.mUserRecord.mClientGroupMap.get(clientRecord.mGroupId)) != null) {
                        group.mSelectedRouteId = routeId;
                        clientRecord.mUserRecord.mHandler.obtainMessage(10, clientRecord.mGroupId).sendToTarget();
                    }
                }
            }
        }
    }

    private void requestSetVolumeLocked(IMediaRouterClient client, String routeId, int volume) {
        IBinder binder = client.asBinder();
        ClientRecord clientRecord = this.mAllClientRecords.get(binder);
        if (clientRecord != null) {
            clientRecord.mUserRecord.mHandler.obtainMessage(6, volume, 0, routeId).sendToTarget();
        }
    }

    private void requestUpdateVolumeLocked(IMediaRouterClient client, String routeId, int direction) {
        IBinder binder = client.asBinder();
        ClientRecord clientRecord = this.mAllClientRecords.get(binder);
        if (clientRecord != null) {
            clientRecord.mUserRecord.mHandler.obtainMessage(7, direction, 0, routeId).sendToTarget();
        }
    }

    private void initializeUserLocked(UserRecord userRecord) {
        if (DEBUG) {
            Slog.d(TAG, userRecord + ": Initialized");
        }
        if (userRecord.mUserId == this.mCurrentUserId) {
            userRecord.mHandler.sendEmptyMessage(1);
        }
    }

    private void disposeUserIfNeededLocked(UserRecord userRecord) {
        if (userRecord.mUserId != this.mCurrentUserId && userRecord.mClientRecords.isEmpty()) {
            if (DEBUG) {
                Slog.d(TAG, userRecord + ": Disposed");
            }
            this.mUserRecords.remove(userRecord.mUserId);
        }
    }

    private void initializeClientLocked(ClientRecord clientRecord) {
        if (DEBUG) {
            Slog.d(TAG, clientRecord + ": Registered");
        }
    }

    private void disposeClientLocked(ClientRecord clientRecord, boolean died) {
        if (DEBUG) {
            if (died) {
                Slog.d(TAG, clientRecord + ": Died!");
            } else {
                Slog.d(TAG, clientRecord + ": Unregistered");
            }
        }
        if (clientRecord.mRouteTypes != 0 || clientRecord.mActiveScan) {
            clientRecord.mUserRecord.mHandler.sendEmptyMessage(3);
        }
        clientRecord.dispose();
    }

    private boolean validatePackageName(int uid, String packageName) {
        String[] packageNames;
        if (packageName != null && (packageNames = this.mContext.getPackageManager().getPackagesForUid(uid)) != null) {
            for (String n : packageNames) {
                if (n.equals(packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /* loaded from: classes2.dex */
    final class MediaRouterServiceBroadcastReceiver extends BroadcastReceiver {
        MediaRouterServiceBroadcastReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.bluetooth.a2dp.profile.action.ACTIVE_DEVICE_CHANGED")) {
                BluetoothDevice btDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                synchronized (MediaRouterService.this.mLock) {
                    MediaRouterService.this.mActiveBluetoothDevice = btDevice;
                    MediaRouterService.this.mGlobalBluetoothA2dpOn = btDevice != null;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class ClientRecord implements IBinder.DeathRecipient {
        public boolean mActiveScan;
        public final IMediaRouterClient mClient;
        public List<String> mControlCategories;
        public String mGroupId;
        public final String mPackageName;
        public final int mPid;
        public int mRouteTypes;
        public String mSelectedRouteId;
        public final boolean mTrusted;
        public final int mUid;
        public final UserRecord mUserRecord;

        public ClientRecord(UserRecord userRecord, IMediaRouterClient client, int uid, int pid, String packageName, boolean trusted) {
            this.mUserRecord = userRecord;
            this.mClient = client;
            this.mUid = uid;
            this.mPid = pid;
            this.mPackageName = packageName;
            this.mTrusted = trusted;
        }

        public void dispose() {
            this.mClient.asBinder().unlinkToDeath(this, 0);
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            MediaRouterService.this.clientDied(this);
        }

        MediaRouterClientState getState() {
            if (this.mTrusted) {
                return this.mUserRecord.mRouterState;
            }
            return null;
        }

        public void dump(PrintWriter pw, String prefix) {
            pw.println(prefix + this);
            String indent = prefix + "  ";
            pw.println(indent + "mTrusted=" + this.mTrusted);
            pw.println(indent + "mRouteTypes=0x" + Integer.toHexString(this.mRouteTypes));
            pw.println(indent + "mActiveScan=" + this.mActiveScan);
            pw.println(indent + "mSelectedRouteId=" + this.mSelectedRouteId);
        }

        public String toString() {
            return "Client " + this.mPackageName + " (pid " + this.mPid + ")";
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class ClientGroup {
        public final List<ClientRecord> mClientRecords = new ArrayList();
        public String mSelectedRouteId;

        ClientGroup() {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class UserRecord {
        public final UserHandler mHandler;
        public MediaRouterClientState mRouterState;
        public final int mUserId;
        public final ArrayList<ClientRecord> mClientRecords = new ArrayList<>();
        private final ArrayMap<String, ClientGroup> mClientGroupMap = new ArrayMap<>();

        public UserRecord(int userId) {
            this.mUserId = userId;
            this.mHandler = new UserHandler(MediaRouterService.this, this);
        }

        public void dump(final PrintWriter pw, String prefix) {
            pw.println(prefix + this);
            final String indent = prefix + "  ";
            int clientCount = this.mClientRecords.size();
            if (clientCount != 0) {
                for (int i = 0; i < clientCount; i++) {
                    this.mClientRecords.get(i).dump(pw, indent);
                }
            } else {
                pw.println(indent + "<no clients>");
            }
            pw.println(indent + "State");
            pw.println(indent + "mRouterState=" + this.mRouterState);
            if (!this.mHandler.runWithScissors(new Runnable() { // from class: com.android.server.media.MediaRouterService.UserRecord.1
                @Override // java.lang.Runnable
                public void run() {
                    UserRecord.this.mHandler.dump(pw, indent);
                }
            }, 1000L)) {
                pw.println(indent + "<could not dump handler state>");
            }
        }

        public void addToGroup(String groupId, ClientRecord clientRecord) {
            ClientGroup group = this.mClientGroupMap.get(groupId);
            if (group == null) {
                group = new ClientGroup();
                this.mClientGroupMap.put(groupId, group);
            }
            group.mClientRecords.add(clientRecord);
        }

        public void removeFromGroup(String groupId, ClientRecord clientRecord) {
            ClientGroup group = this.mClientGroupMap.get(groupId);
            if (group != null) {
                group.mClientRecords.remove(clientRecord);
                if (group.mClientRecords.size() == 0) {
                    this.mClientGroupMap.remove(groupId);
                }
            }
        }

        public String toString() {
            return "User " + this.mUserId;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static final class UserHandler extends Handler implements RemoteDisplayProviderWatcher.Callback, RemoteDisplayProviderProxy.Callback {
        private static final int MSG_CONNECTION_TIMED_OUT = 9;
        private static final int MSG_NOTIFY_GROUP_ROUTE_SELECTED = 10;
        public static final int MSG_REQUEST_SET_VOLUME = 6;
        public static final int MSG_REQUEST_UPDATE_VOLUME = 7;
        public static final int MSG_SELECT_ROUTE = 4;
        public static final int MSG_START = 1;
        public static final int MSG_STOP = 2;
        public static final int MSG_UNSELECT_ROUTE = 5;
        private static final int MSG_UPDATE_CLIENT_STATE = 8;
        public static final int MSG_UPDATE_DISCOVERY_REQUEST = 3;
        private static final int PHASE_CONNECTED = 2;
        private static final int PHASE_CONNECTING = 1;
        private static final int PHASE_NOT_AVAILABLE = -1;
        private static final int PHASE_NOT_CONNECTED = 0;
        private static final int TIMEOUT_REASON_CONNECTION_LOST = 2;
        private static final int TIMEOUT_REASON_NOT_AVAILABLE = 1;
        private static final int TIMEOUT_REASON_WAITING_FOR_CONNECTED = 4;
        private static final int TIMEOUT_REASON_WAITING_FOR_CONNECTING = 3;
        private boolean mClientStateUpdateScheduled;
        private int mConnectionPhase;
        private int mConnectionTimeoutReason;
        private long mConnectionTimeoutStartTime;
        private int mDiscoveryMode;
        private final ArrayList<ProviderRecord> mProviderRecords;
        private boolean mRunning;
        private RouteRecord mSelectedRouteRecord;
        private final MediaRouterService mService;
        private final ArrayList<IMediaRouterClient> mTempClients;
        private final UserRecord mUserRecord;
        private final RemoteDisplayProviderWatcher mWatcher;

        public UserHandler(MediaRouterService service, UserRecord userRecord) {
            super(Looper.getMainLooper(), null, true);
            this.mProviderRecords = new ArrayList<>();
            this.mTempClients = new ArrayList<>();
            this.mDiscoveryMode = 0;
            this.mConnectionPhase = -1;
            this.mService = service;
            this.mUserRecord = userRecord;
            this.mWatcher = new RemoteDisplayProviderWatcher(service.mContext, this, this, userRecord.mUserId);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    start();
                    return;
                case 2:
                    stop();
                    return;
                case 3:
                    updateDiscoveryRequest();
                    return;
                case 4:
                    selectRoute((String) msg.obj);
                    return;
                case 5:
                    unselectRoute((String) msg.obj);
                    return;
                case 6:
                    requestSetVolume((String) msg.obj, msg.arg1);
                    return;
                case 7:
                    requestUpdateVolume((String) msg.obj, msg.arg1);
                    return;
                case 8:
                    updateClientState();
                    return;
                case 9:
                    connectionTimedOut();
                    return;
                case 10:
                    notifyGroupRouteSelected((String) msg.obj);
                    return;
                default:
                    return;
            }
        }

        public void dump(PrintWriter pw, String prefix) {
            pw.println(prefix + "Handler");
            String indent = prefix + "  ";
            pw.println(indent + "mRunning=" + this.mRunning);
            pw.println(indent + "mDiscoveryMode=" + this.mDiscoveryMode);
            pw.println(indent + "mSelectedRouteRecord=" + this.mSelectedRouteRecord);
            pw.println(indent + "mConnectionPhase=" + this.mConnectionPhase);
            pw.println(indent + "mConnectionTimeoutReason=" + this.mConnectionTimeoutReason);
            pw.println(indent + "mConnectionTimeoutStartTime=" + (this.mConnectionTimeoutReason != 0 ? TimeUtils.formatUptime(this.mConnectionTimeoutStartTime) : "<n/a>"));
            this.mWatcher.dump(pw, prefix);
            int providerCount = this.mProviderRecords.size();
            if (providerCount != 0) {
                for (int i = 0; i < providerCount; i++) {
                    this.mProviderRecords.get(i).dump(pw, prefix);
                }
                return;
            }
            pw.println(indent + "<no providers>");
        }

        private void start() {
            if (!this.mRunning) {
                this.mRunning = true;
                this.mWatcher.start();
            }
        }

        private void stop() {
            if (this.mRunning) {
                this.mRunning = false;
                unselectSelectedRoute();
                this.mWatcher.stop();
            }
        }

        private void updateDiscoveryRequest() {
            int newDiscoveryMode;
            int routeTypes = 0;
            boolean activeScan = false;
            synchronized (this.mService.mLock) {
                int count = this.mUserRecord.mClientRecords.size();
                for (int i = 0; i < count; i++) {
                    ClientRecord clientRecord = this.mUserRecord.mClientRecords.get(i);
                    routeTypes |= clientRecord.mRouteTypes;
                    activeScan |= clientRecord.mActiveScan;
                }
            }
            if ((routeTypes & 4) != 0) {
                if (activeScan) {
                    newDiscoveryMode = 2;
                } else {
                    newDiscoveryMode = 1;
                }
            } else {
                newDiscoveryMode = 0;
            }
            if (this.mDiscoveryMode != newDiscoveryMode) {
                this.mDiscoveryMode = newDiscoveryMode;
                int count2 = this.mProviderRecords.size();
                for (int i2 = 0; i2 < count2; i2++) {
                    this.mProviderRecords.get(i2).getProvider().setDiscoveryMode(this.mDiscoveryMode);
                }
            }
        }

        private void selectRoute(String routeId) {
            RouteRecord routeRecord;
            if (routeId != null) {
                RouteRecord routeRecord2 = this.mSelectedRouteRecord;
                if ((routeRecord2 == null || !routeId.equals(routeRecord2.getUniqueId())) && (routeRecord = findRouteRecord(routeId)) != null) {
                    unselectSelectedRoute();
                    Slog.i(MediaRouterService.TAG, "Selected route:" + routeRecord);
                    this.mSelectedRouteRecord = routeRecord;
                    checkSelectedRouteState();
                    routeRecord.getProvider().setSelectedDisplay(routeRecord.getDescriptorId());
                    scheduleUpdateClientState();
                }
            }
        }

        private void unselectRoute(String routeId) {
            RouteRecord routeRecord;
            if (routeId != null && (routeRecord = this.mSelectedRouteRecord) != null && routeId.equals(routeRecord.getUniqueId())) {
                unselectSelectedRoute();
            }
        }

        private void unselectSelectedRoute() {
            if (this.mSelectedRouteRecord != null) {
                Slog.i(MediaRouterService.TAG, "Unselected route:" + this.mSelectedRouteRecord);
                this.mSelectedRouteRecord.getProvider().setSelectedDisplay(null);
                this.mSelectedRouteRecord = null;
                checkSelectedRouteState();
                scheduleUpdateClientState();
            }
        }

        private void requestSetVolume(String routeId, int volume) {
            RouteRecord routeRecord = this.mSelectedRouteRecord;
            if (routeRecord != null && routeId.equals(routeRecord.getUniqueId())) {
                this.mSelectedRouteRecord.getProvider().setDisplayVolume(volume);
            }
        }

        private void requestUpdateVolume(String routeId, int direction) {
            RouteRecord routeRecord = this.mSelectedRouteRecord;
            if (routeRecord != null && routeId.equals(routeRecord.getUniqueId())) {
                this.mSelectedRouteRecord.getProvider().adjustDisplayVolume(direction);
            }
        }

        @Override // com.android.server.media.RemoteDisplayProviderWatcher.Callback
        public void addProvider(RemoteDisplayProviderProxy provider) {
            provider.setCallback(this);
            provider.setDiscoveryMode(this.mDiscoveryMode);
            provider.setSelectedDisplay(null);
            ProviderRecord providerRecord = new ProviderRecord(provider);
            this.mProviderRecords.add(providerRecord);
            providerRecord.updateDescriptor(provider.getDisplayState());
            scheduleUpdateClientState();
        }

        @Override // com.android.server.media.RemoteDisplayProviderWatcher.Callback
        public void removeProvider(RemoteDisplayProviderProxy provider) {
            int index = findProviderRecord(provider);
            if (index >= 0) {
                ProviderRecord providerRecord = this.mProviderRecords.remove(index);
                providerRecord.updateDescriptor(null);
                provider.setCallback(null);
                provider.setDiscoveryMode(0);
                checkSelectedRouteState();
                scheduleUpdateClientState();
            }
        }

        @Override // com.android.server.media.RemoteDisplayProviderProxy.Callback
        public void onDisplayStateChanged(RemoteDisplayProviderProxy provider, RemoteDisplayState state) {
            updateProvider(provider, state);
        }

        private void updateProvider(RemoteDisplayProviderProxy provider, RemoteDisplayState state) {
            int index = findProviderRecord(provider);
            if (index >= 0) {
                ProviderRecord providerRecord = this.mProviderRecords.get(index);
                if (providerRecord.updateDescriptor(state)) {
                    checkSelectedRouteState();
                    scheduleUpdateClientState();
                }
            }
        }

        private void checkSelectedRouteState() {
            RouteRecord routeRecord = this.mSelectedRouteRecord;
            if (routeRecord == null) {
                this.mConnectionPhase = -1;
                updateConnectionTimeout(0);
            } else if (!routeRecord.isValid() || !this.mSelectedRouteRecord.isEnabled()) {
                updateConnectionTimeout(1);
            } else {
                int oldPhase = this.mConnectionPhase;
                int connectionPhase = getConnectionPhase(this.mSelectedRouteRecord.getStatus());
                this.mConnectionPhase = connectionPhase;
                if (oldPhase >= 1 && connectionPhase < 1) {
                    updateConnectionTimeout(2);
                    return;
                }
                switch (connectionPhase) {
                    case 0:
                        updateConnectionTimeout(3);
                        return;
                    case 1:
                        if (oldPhase != 1) {
                            Slog.i(MediaRouterService.TAG, "Connecting to route: " + this.mSelectedRouteRecord);
                        }
                        updateConnectionTimeout(4);
                        return;
                    case 2:
                        if (oldPhase != 2) {
                            Slog.i(MediaRouterService.TAG, "Connected to route: " + this.mSelectedRouteRecord);
                        }
                        updateConnectionTimeout(0);
                        return;
                    default:
                        updateConnectionTimeout(1);
                        return;
                }
            }
        }

        private void updateConnectionTimeout(int reason) {
            int i = this.mConnectionTimeoutReason;
            if (reason != i) {
                if (i != 0) {
                    removeMessages(9);
                }
                this.mConnectionTimeoutReason = reason;
                this.mConnectionTimeoutStartTime = SystemClock.uptimeMillis();
                switch (reason) {
                    case 1:
                    case 2:
                        sendEmptyMessage(9);
                        return;
                    case 3:
                        sendEmptyMessageDelayed(9, MediaRouterService.CONNECTING_TIMEOUT);
                        return;
                    case 4:
                        sendEmptyMessageDelayed(9, 60000L);
                        return;
                    default:
                        return;
                }
            }
        }

        private void connectionTimedOut() {
            int i = this.mConnectionTimeoutReason;
            if (i == 0 || this.mSelectedRouteRecord == null) {
                Log.wtf(MediaRouterService.TAG, "Handled connection timeout for no reason.");
                return;
            }
            switch (i) {
                case 1:
                    Slog.i(MediaRouterService.TAG, "Selected route no longer available: " + this.mSelectedRouteRecord);
                    break;
                case 2:
                    Slog.i(MediaRouterService.TAG, "Selected route connection lost: " + this.mSelectedRouteRecord);
                    break;
                case 3:
                    Slog.i(MediaRouterService.TAG, "Selected route timed out while waiting for connection attempt to begin after " + (SystemClock.uptimeMillis() - this.mConnectionTimeoutStartTime) + " ms: " + this.mSelectedRouteRecord);
                    break;
                case 4:
                    Slog.i(MediaRouterService.TAG, "Selected route timed out while connecting after " + (SystemClock.uptimeMillis() - this.mConnectionTimeoutStartTime) + " ms: " + this.mSelectedRouteRecord);
                    break;
            }
            this.mConnectionTimeoutReason = 0;
            unselectSelectedRoute();
        }

        private void scheduleUpdateClientState() {
            if (!this.mClientStateUpdateScheduled) {
                this.mClientStateUpdateScheduled = true;
                sendEmptyMessage(8);
            }
        }

        private void updateClientState() {
            this.mClientStateUpdateScheduled = false;
            MediaRouterClientState routerState = new MediaRouterClientState();
            int providerCount = this.mProviderRecords.size();
            for (int i = 0; i < providerCount; i++) {
                this.mProviderRecords.get(i).appendClientState(routerState);
            }
            try {
                synchronized (this.mService.mLock) {
                    this.mUserRecord.mRouterState = routerState;
                    int count = this.mUserRecord.mClientRecords.size();
                    for (int i2 = 0; i2 < count; i2++) {
                        this.mTempClients.add(this.mUserRecord.mClientRecords.get(i2).mClient);
                    }
                }
                int count2 = this.mTempClients.size();
                for (int i3 = 0; i3 < count2; i3++) {
                    try {
                        this.mTempClients.get(i3).onStateChanged();
                    } catch (RemoteException e) {
                        Slog.w(MediaRouterService.TAG, "Failed to call onStateChanged. Client probably died.");
                    }
                }
            } finally {
                this.mTempClients.clear();
            }
        }

        private void notifyGroupRouteSelected(String groupId) {
            try {
                synchronized (this.mService.mLock) {
                    ClientGroup group = (ClientGroup) this.mUserRecord.mClientGroupMap.get(groupId);
                    if (group == null) {
                        return;
                    }
                    String selectedRouteId = group.mSelectedRouteId;
                    int count = group.mClientRecords.size();
                    for (int i = 0; i < count; i++) {
                        ClientRecord clientRecord = group.mClientRecords.get(i);
                        if (!TextUtils.equals(selectedRouteId, clientRecord.mSelectedRouteId)) {
                            this.mTempClients.add(clientRecord.mClient);
                        }
                    }
                    int count2 = this.mTempClients.size();
                    for (int i2 = 0; i2 < count2; i2++) {
                        try {
                            this.mTempClients.get(i2).onGroupRouteSelected(selectedRouteId);
                        } catch (RemoteException e) {
                            Slog.w(MediaRouterService.TAG, "Failed to call onSelectedRouteChanged. Client probably died.");
                        }
                    }
                }
            } finally {
                this.mTempClients.clear();
            }
        }

        private int findProviderRecord(RemoteDisplayProviderProxy provider) {
            int count = this.mProviderRecords.size();
            for (int i = 0; i < count; i++) {
                ProviderRecord record = this.mProviderRecords.get(i);
                if (record.getProvider() == provider) {
                    return i;
                }
            }
            return -1;
        }

        private RouteRecord findRouteRecord(String uniqueId) {
            int count = this.mProviderRecords.size();
            for (int i = 0; i < count; i++) {
                RouteRecord record = this.mProviderRecords.get(i).findRouteByUniqueId(uniqueId);
                if (record != null) {
                    return record;
                }
            }
            return null;
        }

        private static int getConnectionPhase(int status) {
            switch (status) {
                case 0:
                case 6:
                    return 2;
                case 1:
                case 3:
                    return 0;
                case 2:
                    return 1;
                case 4:
                case 5:
                default:
                    return -1;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes2.dex */
        public static final class ProviderRecord {
            private RemoteDisplayState mDescriptor;
            private final RemoteDisplayProviderProxy mProvider;
            private final ArrayList<RouteRecord> mRoutes = new ArrayList<>();
            private final String mUniquePrefix;

            public ProviderRecord(RemoteDisplayProviderProxy provider) {
                this.mProvider = provider;
                this.mUniquePrefix = provider.getFlattenedComponentName() + ":";
            }

            public RemoteDisplayProviderProxy getProvider() {
                return this.mProvider;
            }

            public String getUniquePrefix() {
                return this.mUniquePrefix;
            }

            public boolean updateDescriptor(RemoteDisplayState descriptor) {
                boolean changed = false;
                if (this.mDescriptor != descriptor) {
                    this.mDescriptor = descriptor;
                    int targetIndex = 0;
                    if (descriptor != null) {
                        if (descriptor.isValid()) {
                            List<RemoteDisplayState.RemoteDisplayInfo> routeDescriptors = descriptor.displays;
                            int routeCount = routeDescriptors.size();
                            for (int i = 0; i < routeCount; i++) {
                                RemoteDisplayState.RemoteDisplayInfo routeDescriptor = routeDescriptors.get(i);
                                String descriptorId = routeDescriptor.id;
                                int sourceIndex = findRouteByDescriptorId(descriptorId);
                                if (sourceIndex < 0) {
                                    String uniqueId = assignRouteUniqueId(descriptorId);
                                    RouteRecord route = new RouteRecord(this, descriptorId, uniqueId);
                                    this.mRoutes.add(targetIndex, route);
                                    route.updateDescriptor(routeDescriptor);
                                    changed = true;
                                    targetIndex++;
                                } else if (sourceIndex < targetIndex) {
                                    Slog.w(MediaRouterService.TAG, "Ignoring route descriptor with duplicate id: " + routeDescriptor);
                                } else {
                                    RouteRecord route2 = this.mRoutes.get(sourceIndex);
                                    Collections.swap(this.mRoutes, sourceIndex, targetIndex);
                                    changed |= route2.updateDescriptor(routeDescriptor);
                                    targetIndex++;
                                }
                            }
                        } else {
                            Slog.w(MediaRouterService.TAG, "Ignoring invalid descriptor from media route provider: " + this.mProvider.getFlattenedComponentName());
                        }
                    }
                    for (int i2 = this.mRoutes.size() - 1; i2 >= targetIndex; i2--) {
                        RouteRecord route3 = this.mRoutes.remove(i2);
                        route3.updateDescriptor(null);
                        changed = true;
                    }
                }
                return changed;
            }

            public void appendClientState(MediaRouterClientState state) {
                int routeCount = this.mRoutes.size();
                for (int i = 0; i < routeCount; i++) {
                    state.routes.add(this.mRoutes.get(i).getInfo());
                }
            }

            public RouteRecord findRouteByUniqueId(String uniqueId) {
                int routeCount = this.mRoutes.size();
                for (int i = 0; i < routeCount; i++) {
                    RouteRecord route = this.mRoutes.get(i);
                    if (route.getUniqueId().equals(uniqueId)) {
                        return route;
                    }
                }
                return null;
            }

            private int findRouteByDescriptorId(String descriptorId) {
                int routeCount = this.mRoutes.size();
                for (int i = 0; i < routeCount; i++) {
                    RouteRecord route = this.mRoutes.get(i);
                    if (route.getDescriptorId().equals(descriptorId)) {
                        return i;
                    }
                }
                return -1;
            }

            public void dump(PrintWriter pw, String prefix) {
                pw.println(prefix + this);
                String indent = prefix + "  ";
                this.mProvider.dump(pw, indent);
                int routeCount = this.mRoutes.size();
                if (routeCount != 0) {
                    for (int i = 0; i < routeCount; i++) {
                        this.mRoutes.get(i).dump(pw, indent);
                    }
                    return;
                }
                pw.println(indent + "<no routes>");
            }

            public String toString() {
                return "Provider " + this.mProvider.getFlattenedComponentName();
            }

            private String assignRouteUniqueId(String descriptorId) {
                return this.mUniquePrefix + descriptorId;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes2.dex */
        public static final class RouteRecord {
            private RemoteDisplayState.RemoteDisplayInfo mDescriptor;
            private final String mDescriptorId;
            private MediaRouterClientState.RouteInfo mImmutableInfo;
            private final MediaRouterClientState.RouteInfo mMutableInfo;
            private final ProviderRecord mProviderRecord;

            public RouteRecord(ProviderRecord providerRecord, String descriptorId, String uniqueId) {
                this.mProviderRecord = providerRecord;
                this.mDescriptorId = descriptorId;
                this.mMutableInfo = new MediaRouterClientState.RouteInfo(uniqueId);
            }

            public RemoteDisplayProviderProxy getProvider() {
                return this.mProviderRecord.getProvider();
            }

            public ProviderRecord getProviderRecord() {
                return this.mProviderRecord;
            }

            public String getDescriptorId() {
                return this.mDescriptorId;
            }

            public String getUniqueId() {
                return this.mMutableInfo.id;
            }

            public MediaRouterClientState.RouteInfo getInfo() {
                if (this.mImmutableInfo == null) {
                    this.mImmutableInfo = new MediaRouterClientState.RouteInfo(this.mMutableInfo);
                }
                return this.mImmutableInfo;
            }

            public boolean isValid() {
                return this.mDescriptor != null;
            }

            public boolean isEnabled() {
                return this.mMutableInfo.enabled;
            }

            public int getStatus() {
                return this.mMutableInfo.statusCode;
            }

            public boolean updateDescriptor(RemoteDisplayState.RemoteDisplayInfo descriptor) {
                boolean changed = false;
                if (this.mDescriptor != descriptor) {
                    this.mDescriptor = descriptor;
                    if (descriptor != null) {
                        String name = computeName(descriptor);
                        if (!Objects.equals(this.mMutableInfo.name, name)) {
                            this.mMutableInfo.name = name;
                            changed = true;
                        }
                        String description = computeDescription(descriptor);
                        if (!Objects.equals(this.mMutableInfo.description, description)) {
                            this.mMutableInfo.description = description;
                            changed = true;
                        }
                        int supportedTypes = computeSupportedTypes(descriptor);
                        if (this.mMutableInfo.supportedTypes != supportedTypes) {
                            this.mMutableInfo.supportedTypes = supportedTypes;
                            changed = true;
                        }
                        boolean enabled = computeEnabled(descriptor);
                        if (this.mMutableInfo.enabled != enabled) {
                            this.mMutableInfo.enabled = enabled;
                            changed = true;
                        }
                        int statusCode = computeStatusCode(descriptor);
                        if (this.mMutableInfo.statusCode != statusCode) {
                            this.mMutableInfo.statusCode = statusCode;
                            changed = true;
                        }
                        int playbackType = computePlaybackType(descriptor);
                        if (this.mMutableInfo.playbackType != playbackType) {
                            this.mMutableInfo.playbackType = playbackType;
                            changed = true;
                        }
                        int playbackStream = computePlaybackStream(descriptor);
                        if (this.mMutableInfo.playbackStream != playbackStream) {
                            this.mMutableInfo.playbackStream = playbackStream;
                            changed = true;
                        }
                        int volume = computeVolume(descriptor);
                        if (this.mMutableInfo.volume != volume) {
                            this.mMutableInfo.volume = volume;
                            changed = true;
                        }
                        int volumeMax = computeVolumeMax(descriptor);
                        if (this.mMutableInfo.volumeMax != volumeMax) {
                            this.mMutableInfo.volumeMax = volumeMax;
                            changed = true;
                        }
                        int volumeHandling = computeVolumeHandling(descriptor);
                        if (this.mMutableInfo.volumeHandling != volumeHandling) {
                            this.mMutableInfo.volumeHandling = volumeHandling;
                            changed = true;
                        }
                        int presentationDisplayId = computePresentationDisplayId(descriptor);
                        if (this.mMutableInfo.presentationDisplayId != presentationDisplayId) {
                            this.mMutableInfo.presentationDisplayId = presentationDisplayId;
                            changed = true;
                        }
                    }
                }
                if (changed) {
                    this.mImmutableInfo = null;
                }
                return changed;
            }

            public void dump(PrintWriter pw, String prefix) {
                pw.println(prefix + this);
                String indent = prefix + "  ";
                pw.println(indent + "mMutableInfo=" + this.mMutableInfo);
                pw.println(indent + "mDescriptorId=" + this.mDescriptorId);
                pw.println(indent + "mDescriptor=" + this.mDescriptor);
            }

            public String toString() {
                return "Route " + this.mMutableInfo.name + " (" + this.mMutableInfo.id + ")";
            }

            private static String computeName(RemoteDisplayState.RemoteDisplayInfo descriptor) {
                return descriptor.name;
            }

            private static String computeDescription(RemoteDisplayState.RemoteDisplayInfo descriptor) {
                String description = descriptor.description;
                if (TextUtils.isEmpty(description)) {
                    return null;
                }
                return description;
            }

            private static int computeSupportedTypes(RemoteDisplayState.RemoteDisplayInfo descriptor) {
                return 7;
            }

            private static boolean computeEnabled(RemoteDisplayState.RemoteDisplayInfo descriptor) {
                switch (descriptor.status) {
                    case 2:
                    case 3:
                    case 4:
                        return true;
                    default:
                        return false;
                }
            }

            private static int computeStatusCode(RemoteDisplayState.RemoteDisplayInfo descriptor) {
                switch (descriptor.status) {
                    case 0:
                        return 4;
                    case 1:
                        return 5;
                    case 2:
                        return 3;
                    case 3:
                        return 2;
                    case 4:
                        return 6;
                    default:
                        return 0;
                }
            }

            private static int computePlaybackType(RemoteDisplayState.RemoteDisplayInfo descriptor) {
                return 1;
            }

            private static int computePlaybackStream(RemoteDisplayState.RemoteDisplayInfo descriptor) {
                return 3;
            }

            private static int computeVolume(RemoteDisplayState.RemoteDisplayInfo descriptor) {
                int volume = descriptor.volume;
                int volumeMax = descriptor.volumeMax;
                if (volume < 0) {
                    return 0;
                }
                if (volume > volumeMax) {
                    return volumeMax;
                }
                return volume;
            }

            private static int computeVolumeMax(RemoteDisplayState.RemoteDisplayInfo descriptor) {
                int volumeMax = descriptor.volumeMax;
                if (volumeMax > 0) {
                    return volumeMax;
                }
                return 0;
            }

            private static int computeVolumeHandling(RemoteDisplayState.RemoteDisplayInfo descriptor) {
                int volumeHandling = descriptor.volumeHandling;
                switch (volumeHandling) {
                    case 1:
                        return 1;
                    default:
                        return 0;
                }
            }

            private static int computePresentationDisplayId(RemoteDisplayState.RemoteDisplayInfo descriptor) {
                int displayId = descriptor.presentationDisplayId;
                if (displayId < 0) {
                    return -1;
                }
                return displayId;
            }
        }
    }
}
