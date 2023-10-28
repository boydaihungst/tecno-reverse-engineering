package com.android.server.media;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioRoutesInfo;
import android.media.IAudioRoutesObserver;
import android.media.IAudioService;
import android.media.MediaRoute2Info;
import android.media.MediaRoute2ProviderInfo;
import android.media.RouteDiscoveryPreference;
import android.media.RoutingSessionInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import com.android.server.media.BluetoothRouteProvider;
import com.android.server.media.MediaRoute2Provider;
import com.android.server.media.SystemMediaRoute2Provider;
import java.util.List;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class SystemMediaRoute2Provider extends MediaRoute2Provider {
    static final String DEFAULT_ROUTE_ID = "DEFAULT_ROUTE";
    static final String DEVICE_ROUTE_ID = "DEVICE_ROUTE";
    static final String SYSTEM_SESSION_ID = "SYSTEM_SESSION";
    private final AudioManager mAudioManager;
    private final AudioManagerBroadcastReceiver mAudioReceiver;
    final IAudioRoutesObserver.Stub mAudioRoutesObserver;
    private final IAudioService mAudioService;
    private final BluetoothRouteProvider mBtRouteProvider;
    private final Context mContext;
    final AudioRoutesInfo mCurAudioRoutesInfo;
    MediaRoute2Info mDefaultRoute;
    RoutingSessionInfo mDefaultSessionInfo;
    MediaRoute2Info mDeviceRoute;
    int mDeviceVolume;
    private final Handler mHandler;
    private volatile SessionCreationRequest mPendingSessionCreationRequest;
    private final Object mRequestLock;
    private String mSelectedRouteId;
    private final UserHandle mUser;
    private static final String TAG = "MR2SystemProvider";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static ComponentName sComponentName = new ComponentName(SystemMediaRoute2Provider.class.getPackage().getName(), SystemMediaRoute2Provider.class.getName());

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.media.SystemMediaRoute2Provider$1  reason: invalid class name */
    /* loaded from: classes2.dex */
    public class AnonymousClass1 extends IAudioRoutesObserver.Stub {
        AnonymousClass1() {
        }

        public void dispatchAudioRoutesChanged(final AudioRoutesInfo newRoutes) {
            SystemMediaRoute2Provider.this.mHandler.post(new Runnable() { // from class: com.android.server.media.SystemMediaRoute2Provider$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    SystemMediaRoute2Provider.AnonymousClass1.this.m4836x4c36b0e0(newRoutes);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$dispatchAudioRoutesChanged$0$com-android-server-media-SystemMediaRoute2Provider$1  reason: not valid java name */
        public /* synthetic */ void m4836x4c36b0e0(AudioRoutesInfo newRoutes) {
            SystemMediaRoute2Provider.this.updateDeviceRoute(newRoutes);
            SystemMediaRoute2Provider.this.notifyProviderState();
            if (SystemMediaRoute2Provider.this.updateSessionInfosIfNeeded()) {
                SystemMediaRoute2Provider.this.notifySessionInfoUpdated();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SystemMediaRoute2Provider(Context context, UserHandle user) {
        super(sComponentName);
        this.mCurAudioRoutesInfo = new AudioRoutesInfo();
        this.mAudioReceiver = new AudioManagerBroadcastReceiver();
        this.mRequestLock = new Object();
        AnonymousClass1 anonymousClass1 = new AnonymousClass1();
        this.mAudioRoutesObserver = anonymousClass1;
        this.mIsSystemRouteProvider = true;
        this.mContext = context;
        this.mUser = user;
        this.mHandler = new Handler(Looper.getMainLooper());
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        IAudioService asInterface = IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
        this.mAudioService = asInterface;
        AudioRoutesInfo newAudioRoutes = null;
        try {
            newAudioRoutes = asInterface.startWatchingRoutes(anonymousClass1);
        } catch (RemoteException e) {
        }
        updateDeviceRoute(newAudioRoutes);
        this.mBtRouteProvider = BluetoothRouteProvider.createInstance(context, new BluetoothRouteProvider.BluetoothRoutesUpdatedListener() { // from class: com.android.server.media.SystemMediaRoute2Provider$$ExternalSyntheticLambda2
            @Override // com.android.server.media.BluetoothRouteProvider.BluetoothRoutesUpdatedListener
            public final void onBluetoothRoutesUpdated(List list) {
                SystemMediaRoute2Provider.this.m4833lambda$new$0$comandroidservermediaSystemMediaRoute2Provider(list);
            }
        });
        updateSessionInfosIfNeeded();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-media-SystemMediaRoute2Provider  reason: not valid java name */
    public /* synthetic */ void m4833lambda$new$0$comandroidservermediaSystemMediaRoute2Provider(List routes) {
        publishProviderState();
        if (updateSessionInfosIfNeeded()) {
            notifySessionInfoUpdated();
        }
    }

    public void start() {
        IntentFilter intentFilter = new IntentFilter("android.media.VOLUME_CHANGED_ACTION");
        intentFilter.addAction("android.media.STREAM_DEVICES_CHANGED_ACTION");
        this.mContext.registerReceiverAsUser(this.mAudioReceiver, this.mUser, intentFilter, null, null);
        if (this.mBtRouteProvider != null) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.media.SystemMediaRoute2Provider$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    SystemMediaRoute2Provider.this.m4834x8b7f5b58();
                }
            });
        }
        updateVolume();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$start$1$com-android-server-media-SystemMediaRoute2Provider  reason: not valid java name */
    public /* synthetic */ void m4834x8b7f5b58() {
        this.mBtRouteProvider.start(this.mUser);
        notifyProviderState();
    }

    public void stop() {
        this.mContext.unregisterReceiver(this.mAudioReceiver);
        if (this.mBtRouteProvider != null) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.media.SystemMediaRoute2Provider$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    SystemMediaRoute2Provider.this.m4835lambda$stop$2$comandroidservermediaSystemMediaRoute2Provider();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$stop$2$com-android-server-media-SystemMediaRoute2Provider  reason: not valid java name */
    public /* synthetic */ void m4835lambda$stop$2$comandroidservermediaSystemMediaRoute2Provider() {
        this.mBtRouteProvider.stop();
        notifyProviderState();
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void setCallback(MediaRoute2Provider.Callback callback) {
        super.setCallback(callback);
        notifyProviderState();
        notifySessionInfoUpdated();
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void requestCreateSession(long requestId, String packageName, String routeId, Bundle sessionHints) {
        if (TextUtils.equals(routeId, DEFAULT_ROUTE_ID)) {
            this.mCallback.onSessionCreated(this, requestId, this.mDefaultSessionInfo);
        } else if (TextUtils.equals(routeId, this.mSelectedRouteId)) {
            this.mCallback.onSessionCreated(this, requestId, this.mSessionInfos.get(0));
        } else {
            synchronized (this.mRequestLock) {
                if (this.mPendingSessionCreationRequest != null) {
                    this.mCallback.onRequestFailed(this, this.mPendingSessionCreationRequest.mRequestId, 0);
                }
                this.mPendingSessionCreationRequest = new SessionCreationRequest(requestId, routeId);
            }
            transferToRoute(requestId, SYSTEM_SESSION_ID, routeId);
        }
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void releaseSession(long requestId, String sessionId) {
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void updateDiscoveryPreference(RouteDiscoveryPreference discoveryPreference) {
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void selectRoute(long requestId, String sessionId, String routeId) {
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void deselectRoute(long requestId, String sessionId, String routeId) {
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void transferToRoute(long requestId, String sessionId, String routeId) {
        if (!TextUtils.equals(routeId, DEFAULT_ROUTE_ID) && this.mBtRouteProvider != null) {
            if (TextUtils.equals(routeId, this.mDeviceRoute.getId())) {
                this.mBtRouteProvider.transferTo(null);
            } else {
                this.mBtRouteProvider.transferTo(routeId);
            }
        }
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void setRouteVolume(long requestId, String routeId, int volume) {
        if (!TextUtils.equals(routeId, this.mSelectedRouteId)) {
            return;
        }
        this.mAudioManager.setStreamVolume(3, volume, 0);
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void setSessionVolume(long requestId, String sessionId, int volume) {
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void prepareReleaseSession(String sessionId) {
    }

    public MediaRoute2Info getDefaultRoute() {
        return this.mDefaultRoute;
    }

    public RoutingSessionInfo getDefaultSessionInfo() {
        return this.mDefaultSessionInfo;
    }

    public RoutingSessionInfo generateDeviceRouteSelectedSessionInfo(String packageName) {
        synchronized (this.mLock) {
            if (this.mSessionInfos.isEmpty()) {
                return null;
            }
            RoutingSessionInfo.Builder builder = new RoutingSessionInfo.Builder(SYSTEM_SESSION_ID, packageName).setSystemSession(true);
            builder.addSelectedRoute(this.mDeviceRoute.getId());
            BluetoothRouteProvider bluetoothRouteProvider = this.mBtRouteProvider;
            if (bluetoothRouteProvider != null) {
                for (MediaRoute2Info route : bluetoothRouteProvider.getAllBluetoothRoutes()) {
                    builder.addTransferableRoute(route.getId());
                }
            }
            return builder.setProviderId(this.mUniqueId).build();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDeviceRoute(AudioRoutesInfo newRoutes) {
        int name = 17040135;
        int type = 2;
        int i = 1;
        if (newRoutes != null) {
            this.mCurAudioRoutesInfo.mainType = newRoutes.mainType;
            if ((newRoutes.mainType & 2) == 0) {
                if ((newRoutes.mainType & 1) != 0) {
                    type = 3;
                    name = 17040138;
                } else if ((newRoutes.mainType & 4) != 0) {
                    type = 13;
                    name = 17040136;
                } else if ((newRoutes.mainType & 8) != 0) {
                    type = 9;
                    name = 17040137;
                } else if ((newRoutes.mainType & 16) != 0) {
                    type = 11;
                    name = 17040139;
                }
            } else {
                type = 4;
                name = 17040138;
            }
        }
        MediaRoute2Info.Builder builder = new MediaRoute2Info.Builder(DEVICE_ROUTE_ID, this.mContext.getResources().getText(name).toString());
        if (this.mAudioManager.isVolumeFixed()) {
            i = 0;
        }
        this.mDeviceRoute = builder.setVolumeHandling(i).setVolume(this.mDeviceVolume).setVolumeMax(this.mAudioManager.getStreamMaxVolume(3)).setType(type).addFeature("android.media.route.feature.LIVE_AUDIO").addFeature("android.media.route.feature.LIVE_VIDEO").addFeature("android.media.route.feature.LOCAL_PLAYBACK").setConnectionState(2).build();
        updateProviderState();
    }

    private void updateProviderState() {
        MediaRoute2ProviderInfo.Builder builder = new MediaRoute2ProviderInfo.Builder();
        builder.addRoute(this.mDeviceRoute);
        BluetoothRouteProvider bluetoothRouteProvider = this.mBtRouteProvider;
        if (bluetoothRouteProvider != null) {
            for (MediaRoute2Info route : bluetoothRouteProvider.getAllBluetoothRoutes()) {
                builder.addRoute(route);
            }
        }
        MediaRoute2ProviderInfo providerInfo = builder.build();
        setProviderState(providerInfo);
        if (DEBUG) {
            Slog.d(TAG, "Updating system provider info : " + providerInfo);
        }
    }

    boolean updateSessionInfosIfNeeded() {
        SessionCreationRequest sessionCreationRequest;
        MediaRoute2Info selectedBtRoute;
        synchronized (this.mLock) {
            RoutingSessionInfo oldSessionInfo = this.mSessionInfos.isEmpty() ? null : this.mSessionInfos.get(0);
            RoutingSessionInfo.Builder builder = new RoutingSessionInfo.Builder(SYSTEM_SESSION_ID, "").setSystemSession(true);
            MediaRoute2Info selectedRoute = this.mDeviceRoute;
            BluetoothRouteProvider bluetoothRouteProvider = this.mBtRouteProvider;
            if (bluetoothRouteProvider != null && (selectedBtRoute = bluetoothRouteProvider.getSelectedRoute()) != null) {
                selectedRoute = selectedBtRoute;
                builder.addTransferableRoute(this.mDeviceRoute.getId());
            }
            this.mSelectedRouteId = selectedRoute.getId();
            this.mDefaultRoute = new MediaRoute2Info.Builder(DEFAULT_ROUTE_ID, selectedRoute).setSystemRoute(true).setProviderId(this.mUniqueId).build();
            builder.addSelectedRoute(this.mSelectedRouteId);
            BluetoothRouteProvider bluetoothRouteProvider2 = this.mBtRouteProvider;
            if (bluetoothRouteProvider2 != null) {
                for (MediaRoute2Info route : bluetoothRouteProvider2.getTransferableRoutes()) {
                    builder.addTransferableRoute(route.getId());
                }
            }
            RoutingSessionInfo newSessionInfo = builder.setProviderId(this.mUniqueId).build();
            if (this.mPendingSessionCreationRequest != null) {
                synchronized (this.mRequestLock) {
                    sessionCreationRequest = this.mPendingSessionCreationRequest;
                    this.mPendingSessionCreationRequest = null;
                }
                if (sessionCreationRequest != null) {
                    if (TextUtils.equals(this.mSelectedRouteId, sessionCreationRequest.mRouteId)) {
                        this.mCallback.onSessionCreated(this, sessionCreationRequest.mRequestId, newSessionInfo);
                    } else {
                        this.mCallback.onRequestFailed(this, sessionCreationRequest.mRequestId, 0);
                    }
                }
            }
            if (Objects.equals(oldSessionInfo, newSessionInfo)) {
                return false;
            }
            if (DEBUG) {
                Slog.d(TAG, "Updating system routing session info : " + newSessionInfo);
            }
            this.mSessionInfos.clear();
            this.mSessionInfos.add(newSessionInfo);
            this.mDefaultSessionInfo = new RoutingSessionInfo.Builder(SYSTEM_SESSION_ID, "").setProviderId(this.mUniqueId).setSystemSession(true).addSelectedRoute(DEFAULT_ROUTE_ID).build();
            return true;
        }
    }

    void publishProviderState() {
        updateProviderState();
        notifyProviderState();
    }

    void notifySessionInfoUpdated() {
        RoutingSessionInfo sessionInfo;
        if (this.mCallback == null) {
            return;
        }
        synchronized (this.mLock) {
            sessionInfo = this.mSessionInfos.get(0);
        }
        this.mCallback.onSessionUpdated(this, sessionInfo);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class SessionCreationRequest {
        final long mRequestId;
        final String mRouteId;

        SessionCreationRequest(long requestId, String routeId) {
            this.mRequestId = requestId;
            this.mRouteId = routeId;
        }
    }

    void updateVolume() {
        int devices = this.mAudioManager.getDevicesForStream(3);
        int volume = this.mAudioManager.getStreamVolume(3);
        if (this.mDefaultRoute.getVolume() != volume) {
            this.mDefaultRoute = new MediaRoute2Info.Builder(this.mDefaultRoute).setVolume(volume).build();
        }
        BluetoothRouteProvider bluetoothRouteProvider = this.mBtRouteProvider;
        if (bluetoothRouteProvider != null && bluetoothRouteProvider.updateVolumeForDevices(devices, volume)) {
            return;
        }
        if (this.mDeviceVolume != volume) {
            this.mDeviceVolume = volume;
            this.mDeviceRoute = new MediaRoute2Info.Builder(this.mDeviceRoute).setVolume(volume).build();
        }
        publishProviderState();
    }

    /* loaded from: classes2.dex */
    private class AudioManagerBroadcastReceiver extends BroadcastReceiver {
        private AudioManagerBroadcastReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION") && !intent.getAction().equals("android.media.STREAM_DEVICES_CHANGED_ACTION")) {
                return;
            }
            int streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
            if (streamType != 3) {
                return;
            }
            SystemMediaRoute2Provider.this.updateVolume();
        }
    }
}
