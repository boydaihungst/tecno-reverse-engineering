package com.android.server.media;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHearingAid;
import android.bluetooth.BluetoothLeAudio;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.audio.common.V2_0.AudioDevice;
import android.media.AudioManager;
import android.media.MediaRoute2Info;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class BluetoothRouteProvider {
    private static final String HEARING_AID_ROUTE_ID_PREFIX = "HEARING_AID_";
    private static final String LE_AUDIO_ROUTE_ID_PREFIX = "LE_AUDIO_";
    BluetoothA2dp mA2dpProfile;
    private final AudioManager mAudioManager;
    private final BluetoothAdapter mBluetoothAdapter;
    private final Context mContext;
    BluetoothHearingAid mHearingAidProfile;
    BluetoothLeAudio mLeAudioProfile;
    private final BluetoothRoutesUpdatedListener mListener;
    private static final String TAG = "BTRouteProvider";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    final Map<String, BluetoothRouteInfo> mBluetoothRoutes = new HashMap();
    final List<BluetoothRouteInfo> mActiveRoutes = new ArrayList();
    private final SparseIntArray mVolumeMap = new SparseIntArray();
    private final Map<String, BluetoothEventReceiver> mEventReceiverMap = new HashMap();
    private final IntentFilter mIntentFilter = new IntentFilter();
    private final BroadcastReceiver mBroadcastReceiver = new BluetoothBroadcastReceiver();
    private final BluetoothProfileListener mProfileListener = new BluetoothProfileListener();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public interface BluetoothEventReceiver {
        void onReceive(Context context, Intent intent, BluetoothDevice bluetoothDevice);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface BluetoothRoutesUpdatedListener {
        void onBluetoothRoutesUpdated(List<MediaRoute2Info> list);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static BluetoothRouteProvider createInstance(Context context, BluetoothRoutesUpdatedListener listener) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(listener);
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            return null;
        }
        return new BluetoothRouteProvider(context, btAdapter, listener);
    }

    private BluetoothRouteProvider(Context context, BluetoothAdapter btAdapter, BluetoothRoutesUpdatedListener listener) {
        this.mContext = context;
        this.mBluetoothAdapter = btAdapter;
        this.mListener = listener;
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        buildBluetoothRoutes();
    }

    public void start(UserHandle user) {
        this.mBluetoothAdapter.getProfileProxy(this.mContext, this.mProfileListener, 2);
        this.mBluetoothAdapter.getProfileProxy(this.mContext, this.mProfileListener, 21);
        this.mBluetoothAdapter.getProfileProxy(this.mContext, this.mProfileListener, 22);
        addEventReceiver("android.bluetooth.adapter.action.STATE_CHANGED", new AdapterStateChangedReceiver());
        DeviceStateChangedReceiver deviceStateChangedReceiver = new DeviceStateChangedReceiver();
        addEventReceiver("android.bluetooth.a2dp.profile.action.ACTIVE_DEVICE_CHANGED", deviceStateChangedReceiver);
        addEventReceiver("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED", deviceStateChangedReceiver);
        addEventReceiver("android.bluetooth.hearingaid.profile.action.ACTIVE_DEVICE_CHANGED", deviceStateChangedReceiver);
        addEventReceiver("android.bluetooth.hearingaid.profile.action.CONNECTION_STATE_CHANGED", deviceStateChangedReceiver);
        addEventReceiver("android.bluetooth.action.LE_AUDIO_CONNECTION_STATE_CHANGED", deviceStateChangedReceiver);
        addEventReceiver("android.bluetooth.action.LE_AUDIO_ACTIVE_DEVICE_CHANGED", deviceStateChangedReceiver);
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, user, this.mIntentFilter, null, null);
    }

    public void stop() {
        this.mContext.unregisterReceiver(this.mBroadcastReceiver);
    }

    public void transferTo(String routeId) {
        if (routeId == null) {
            clearActiveDevices();
            return;
        }
        BluetoothRouteInfo btRouteInfo = findBluetoothRouteWithRouteId(routeId);
        if (btRouteInfo == null) {
            Slog.w(TAG, "transferTo: Unknown route. ID=" + routeId);
            return;
        }
        BluetoothAdapter bluetoothAdapter = this.mBluetoothAdapter;
        if (bluetoothAdapter != null) {
            bluetoothAdapter.setActiveDevice(btRouteInfo.btDevice, 0);
        }
    }

    private void clearActiveDevices() {
        BluetoothAdapter bluetoothAdapter = this.mBluetoothAdapter;
        if (bluetoothAdapter != null) {
            bluetoothAdapter.removeActiveDevice(0);
        }
    }

    private void addEventReceiver(String action, BluetoothEventReceiver eventReceiver) {
        this.mEventReceiverMap.put(action, eventReceiver);
        this.mIntentFilter.addAction(action);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void buildBluetoothRoutes() {
        this.mBluetoothRoutes.clear();
        Set<BluetoothDevice> bondedDevices = this.mBluetoothAdapter.getBondedDevices();
        if (bondedDevices != null) {
            for (BluetoothDevice device : bondedDevices) {
                if (device.isConnected()) {
                    BluetoothRouteInfo newBtRoute = createBluetoothRoute(device);
                    if (newBtRoute.connectedProfiles.size() > 0) {
                        this.mBluetoothRoutes.put(device.getAddress(), newBtRoute);
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public MediaRoute2Info getSelectedRoute() {
        if (this.mActiveRoutes.isEmpty()) {
            return null;
        }
        return this.mActiveRoutes.get(0).route;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<MediaRoute2Info> getTransferableRoutes() {
        List<MediaRoute2Info> routes = getAllBluetoothRoutes();
        for (BluetoothRouteInfo btRoute : this.mActiveRoutes) {
            routes.remove(btRoute.route);
        }
        return routes;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<MediaRoute2Info> getAllBluetoothRoutes() {
        List<MediaRoute2Info> routes = new ArrayList<>();
        List<String> routeIds = new ArrayList<>();
        MediaRoute2Info selectedRoute = getSelectedRoute();
        if (selectedRoute != null) {
            routes.add(selectedRoute);
            routeIds.add(selectedRoute.getId());
        }
        for (BluetoothRouteInfo btRoute : this.mBluetoothRoutes.values()) {
            if (!routeIds.contains(btRoute.route.getId())) {
                routes.add(btRoute.route);
                routeIds.add(btRoute.route.getId());
            }
        }
        return routes;
    }

    BluetoothRouteInfo findBluetoothRouteWithRouteId(String routeId) {
        if (routeId == null) {
            return null;
        }
        for (BluetoothRouteInfo btRouteInfo : this.mBluetoothRoutes.values()) {
            if (TextUtils.equals(btRouteInfo.route.getId(), routeId)) {
                return btRouteInfo;
            }
        }
        return null;
    }

    public boolean updateVolumeForDevices(int devices, int volume) {
        int routeType;
        if ((134217728 & devices) != 0) {
            routeType = 23;
        } else {
            int routeType2 = devices & AudioDevice.OUT_ALL_A2DP;
            if (routeType2 != 0) {
                routeType = 8;
            } else if ((536870912 & devices) != 0) {
                routeType = 26;
            } else {
                return false;
            }
        }
        this.mVolumeMap.put(routeType, volume);
        boolean shouldNotify = false;
        for (BluetoothRouteInfo btRoute : this.mActiveRoutes) {
            if (btRoute.route.getType() == routeType) {
                btRoute.route = new MediaRoute2Info.Builder(btRoute.route).setVolume(volume).build();
                shouldNotify = true;
            }
        }
        if (shouldNotify) {
            notifyBluetoothRoutesUpdated();
            return true;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyBluetoothRoutesUpdated() {
        BluetoothRoutesUpdatedListener bluetoothRoutesUpdatedListener = this.mListener;
        if (bluetoothRoutesUpdatedListener != null) {
            bluetoothRoutesUpdatedListener.onBluetoothRoutesUpdated(getAllBluetoothRoutes());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public BluetoothRouteInfo createBluetoothRoute(BluetoothDevice device) {
        BluetoothRouteInfo newBtRoute = new BluetoothRouteInfo();
        newBtRoute.btDevice = device;
        String routeId = device.getAddress();
        String deviceName = device.getName();
        if (TextUtils.isEmpty(deviceName)) {
            deviceName = this.mContext.getResources().getText(17039374).toString();
        }
        int type = 8;
        newBtRoute.connectedProfiles = new SparseBooleanArray();
        BluetoothA2dp bluetoothA2dp = this.mA2dpProfile;
        if (bluetoothA2dp != null && bluetoothA2dp.getConnectedDevices().contains(device)) {
            newBtRoute.connectedProfiles.put(2, true);
        }
        BluetoothHearingAid bluetoothHearingAid = this.mHearingAidProfile;
        if (bluetoothHearingAid != null && bluetoothHearingAid.getConnectedDevices().contains(device)) {
            newBtRoute.connectedProfiles.put(21, true);
            routeId = HEARING_AID_ROUTE_ID_PREFIX + this.mHearingAidProfile.getHiSyncId(device);
            type = 23;
        }
        BluetoothLeAudio bluetoothLeAudio = this.mLeAudioProfile;
        if (bluetoothLeAudio != null && bluetoothLeAudio.getConnectedDevices().contains(device)) {
            newBtRoute.connectedProfiles.put(22, true);
            routeId = LE_AUDIO_ROUTE_ID_PREFIX + this.mLeAudioProfile.getGroupId(device);
            type = 26;
        }
        newBtRoute.route = new MediaRoute2Info.Builder(routeId, deviceName).addFeature("android.media.route.feature.LIVE_AUDIO").addFeature("android.media.route.feature.LOCAL_PLAYBACK").setConnectionState(0).setDescription(this.mContext.getResources().getText(17039814).toString()).setType(type).setVolumeHandling(1).setVolumeMax(this.mAudioManager.getStreamMaxVolume(3)).setAddress(device.getAddress()).build();
        return newBtRoute;
    }

    private void setRouteConnectionState(BluetoothRouteInfo btRoute, int state) {
        if (btRoute == null) {
            Slog.w(TAG, "setRouteConnectionState: route shouldn't be null");
        } else if (btRoute.route.getConnectionState() == state) {
        } else {
            MediaRoute2Info.Builder builder = new MediaRoute2Info.Builder(btRoute.route).setConnectionState(state);
            builder.setType(btRoute.getRouteType());
            if (state == 2) {
                builder.setVolume(this.mVolumeMap.get(btRoute.getRouteType(), 0));
            }
            btRoute.route = builder.build();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addActiveRoute(BluetoothRouteInfo btRoute) {
        if (btRoute == null) {
            Slog.d(TAG, " btRoute is null");
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "Adding active route: " + btRoute.route);
        }
        if (this.mActiveRoutes.contains(btRoute)) {
            Slog.d(TAG, " btRoute is already added.");
            return;
        }
        setRouteConnectionState(btRoute, 2);
        this.mActiveRoutes.add(btRoute);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeActiveRoute(BluetoothRouteInfo btRoute) {
        if (DEBUG) {
            Log.d(TAG, "Removing active route: " + btRoute.route);
        }
        if (this.mActiveRoutes.remove(btRoute)) {
            setRouteConnectionState(btRoute, 0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clearActiveRoutesWithType(int type) {
        if (DEBUG) {
            Log.d(TAG, "Clearing active routes with type. type=" + type);
        }
        Iterator<BluetoothRouteInfo> iter = this.mActiveRoutes.iterator();
        while (iter.hasNext()) {
            BluetoothRouteInfo btRoute = iter.next();
            if (btRoute.route.getType() == type) {
                iter.remove();
                setRouteConnectionState(btRoute, 0);
            }
        }
    }

    private void addActiveDevices(BluetoothDevice device) {
        BluetoothRouteInfo activeBtRoute = this.mBluetoothRoutes.get(device.getAddress());
        if (activeBtRoute == null) {
            activeBtRoute = createBluetoothRoute(device);
            this.mBluetoothRoutes.put(device.getAddress(), activeBtRoute);
        }
        addActiveRoute(activeBtRoute);
        for (BluetoothRouteInfo btRoute : this.mBluetoothRoutes.values()) {
            if (TextUtils.equals(btRoute.route.getId(), activeBtRoute.route.getId()) && !TextUtils.equals(btRoute.btDevice.getAddress(), activeBtRoute.btDevice.getAddress())) {
                addActiveRoute(btRoute);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addActiveHearingAidDevices(BluetoothDevice device) {
        if (DEBUG) {
            Log.d(TAG, "Setting active hearing aid devices. device=" + device);
        }
        addActiveDevices(device);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addActiveLeAudioDevices(BluetoothDevice device) {
        if (DEBUG) {
            Log.d(TAG, "Setting active le audio devices. device=" + device);
        }
        addActiveDevices(device);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class BluetoothRouteInfo {
        public BluetoothDevice btDevice;
        public SparseBooleanArray connectedProfiles;
        public MediaRoute2Info route;

        private BluetoothRouteInfo() {
        }

        int getRouteType() {
            if (this.connectedProfiles.get(21, false)) {
                return 23;
            }
            if (this.connectedProfiles.get(22, false)) {
                return 26;
            }
            return 8;
        }
    }

    /* loaded from: classes2.dex */
    private final class BluetoothProfileListener implements BluetoothProfile.ServiceListener {
        private BluetoothProfileListener() {
        }

        @Override // android.bluetooth.BluetoothProfile.ServiceListener
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            List<BluetoothDevice> activeDevices;
            switch (profile) {
                case 2:
                    BluetoothRouteProvider.this.mA2dpProfile = (BluetoothA2dp) proxy;
                    activeDevices = BluetoothRouteProvider.this.mBluetoothAdapter.getActiveDevices(2);
                    break;
                case 21:
                    BluetoothRouteProvider.this.mHearingAidProfile = (BluetoothHearingAid) proxy;
                    activeDevices = BluetoothRouteProvider.this.mBluetoothAdapter.getActiveDevices(21);
                    break;
                case 22:
                    BluetoothRouteProvider.this.mLeAudioProfile = (BluetoothLeAudio) proxy;
                    activeDevices = BluetoothRouteProvider.this.mBluetoothAdapter.getActiveDevices(22);
                    break;
                default:
                    return;
            }
            for (BluetoothDevice device : proxy.getConnectedDevices()) {
                BluetoothRouteInfo btRoute = BluetoothRouteProvider.this.mBluetoothRoutes.get(device.getAddress());
                if (btRoute == null) {
                    btRoute = BluetoothRouteProvider.this.createBluetoothRoute(device);
                    BluetoothRouteProvider.this.mBluetoothRoutes.put(device.getAddress(), btRoute);
                }
                if (activeDevices.contains(device)) {
                    BluetoothRouteProvider.this.addActiveRoute(btRoute);
                }
            }
            BluetoothRouteProvider.this.notifyBluetoothRoutesUpdated();
        }

        @Override // android.bluetooth.BluetoothProfile.ServiceListener
        public void onServiceDisconnected(int profile) {
            switch (profile) {
                case 2:
                    BluetoothRouteProvider.this.mA2dpProfile = null;
                    return;
                case 21:
                    BluetoothRouteProvider.this.mHearingAidProfile = null;
                    return;
                case 22:
                    BluetoothRouteProvider.this.mLeAudioProfile = null;
                    return;
                default:
                    return;
            }
        }
    }

    /* loaded from: classes2.dex */
    private class BluetoothBroadcastReceiver extends BroadcastReceiver {
        private BluetoothBroadcastReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            BluetoothEventReceiver receiver = (BluetoothEventReceiver) BluetoothRouteProvider.this.mEventReceiverMap.get(action);
            if (receiver != null) {
                receiver.onReceive(context, intent, device);
            }
        }
    }

    /* loaded from: classes2.dex */
    private class AdapterStateChangedReceiver implements BluetoothEventReceiver {
        private AdapterStateChangedReceiver() {
        }

        @Override // com.android.server.media.BluetoothRouteProvider.BluetoothEventReceiver
        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            int state = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", -1);
            if (state == 10 || state == 13) {
                BluetoothRouteProvider.this.mBluetoothRoutes.clear();
                BluetoothRouteProvider.this.notifyBluetoothRoutesUpdated();
            } else if (state == 12) {
                BluetoothRouteProvider.this.buildBluetoothRoutes();
                if (!BluetoothRouteProvider.this.mBluetoothRoutes.isEmpty()) {
                    BluetoothRouteProvider.this.notifyBluetoothRoutesUpdated();
                }
            }
        }
    }

    /* loaded from: classes2.dex */
    private class DeviceStateChangedReceiver implements BluetoothEventReceiver {
        private DeviceStateChangedReceiver() {
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // com.android.server.media.BluetoothRouteProvider.BluetoothEventReceiver
        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            char c;
            String action = intent.getAction();
            switch (action.hashCode()) {
                case -1765714821:
                    if (action.equals("android.bluetooth.action.LE_AUDIO_CONNECTION_STATE_CHANGED")) {
                        c = 5;
                        break;
                    }
                    c = 65535;
                    break;
                case -749511570:
                    if (action.equals("android.bluetooth.action.LE_AUDIO_ACTIVE_DEVICE_CHANGED")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case -612790895:
                    if (action.equals("android.bluetooth.hearingaid.profile.action.CONNECTION_STATE_CHANGED")) {
                        c = 4;
                        break;
                    }
                    c = 65535;
                    break;
                case 487423555:
                    if (action.equals("android.bluetooth.a2dp.profile.action.ACTIVE_DEVICE_CHANGED")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 1176349464:
                    if (action.equals("android.bluetooth.hearingaid.profile.action.ACTIVE_DEVICE_CHANGED")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 1244161670:
                    if (action.equals("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                    BluetoothRouteProvider.this.clearActiveRoutesWithType(8);
                    if (device != null) {
                        BluetoothRouteProvider bluetoothRouteProvider = BluetoothRouteProvider.this;
                        bluetoothRouteProvider.addActiveRoute(bluetoothRouteProvider.mBluetoothRoutes.get(device.getAddress()));
                    }
                    BluetoothRouteProvider.this.notifyBluetoothRoutesUpdated();
                    return;
                case 1:
                    BluetoothRouteProvider.this.clearActiveRoutesWithType(23);
                    if (device != null) {
                        BluetoothRouteProvider.this.addActiveHearingAidDevices(device);
                    }
                    BluetoothRouteProvider.this.notifyBluetoothRoutesUpdated();
                    return;
                case 2:
                    BluetoothRouteProvider.this.clearActiveRoutesWithType(26);
                    if (device != null) {
                        BluetoothRouteProvider.this.addActiveLeAudioDevices(device);
                    }
                    BluetoothRouteProvider.this.notifyBluetoothRoutesUpdated();
                    return;
                case 3:
                    handleConnectionStateChanged(2, intent, device);
                    return;
                case 4:
                    handleConnectionStateChanged(21, intent, device);
                    return;
                case 5:
                    handleConnectionStateChanged(22, intent, device);
                    return;
                default:
                    return;
            }
        }

        private void handleConnectionStateChanged(int profile, Intent intent, BluetoothDevice device) {
            int state = intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1);
            BluetoothRouteInfo btRoute = BluetoothRouteProvider.this.mBluetoothRoutes.get(device.getAddress());
            if (state == 2) {
                if (btRoute == null) {
                    BluetoothRouteInfo btRoute2 = BluetoothRouteProvider.this.createBluetoothRoute(device);
                    if (btRoute2.connectedProfiles.size() > 0) {
                        BluetoothRouteProvider.this.mBluetoothRoutes.put(device.getAddress(), btRoute2);
                        BluetoothRouteProvider.this.notifyBluetoothRoutesUpdated();
                        return;
                    }
                    return;
                }
                btRoute.connectedProfiles.put(profile, true);
            } else if ((state == 3 || state == 0) && btRoute != null) {
                btRoute.connectedProfiles.delete(profile);
                if (btRoute.connectedProfiles.size() == 0) {
                    BluetoothRouteProvider bluetoothRouteProvider = BluetoothRouteProvider.this;
                    bluetoothRouteProvider.removeActiveRoute(bluetoothRouteProvider.mBluetoothRoutes.remove(device.getAddress()));
                    BluetoothRouteProvider.this.notifyBluetoothRoutesUpdated();
                }
            }
        }
    }
}
