package com.android.server.audio;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.hardware.audio.common.V2_0.AudioDevice;
import android.media.AudioDeviceAttributes;
import android.media.AudioDevicePort;
import android.media.AudioFormat;
import android.media.AudioPort;
import android.media.AudioRoutesInfo;
import android.media.AudioSystem;
import android.media.IAudioRoutesObserver;
import android.media.ICapturePresetDevicesRoleDispatcher;
import android.media.IStrategyPreferredDevicesDispatcher;
import android.media.MediaMetrics;
import android.os.Binder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import com.android.server.audio.AudioDeviceBroker;
import com.android.server.audio.AudioDeviceInventory;
import com.android.server.audio.AudioEventLogger;
import com.android.server.audio.AudioServiceEvents;
import com.android.server.audio.BtHelper;
import com.android.server.pm.PackageManagerService;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
/* loaded from: classes.dex */
public class AudioDeviceInventory {
    private static final Set<Integer> BECOMING_NOISY_INTENT_DEVICES_SET;
    private static final String CONNECT_INTENT_KEY_ADDRESS = "address";
    private static final String CONNECT_INTENT_KEY_DEVICE_CLASS = "class";
    private static final String CONNECT_INTENT_KEY_HAS_CAPTURE = "hasCapture";
    private static final String CONNECT_INTENT_KEY_HAS_MIDI = "hasMIDI";
    private static final String CONNECT_INTENT_KEY_HAS_PLAYBACK = "hasPlayback";
    private static final String CONNECT_INTENT_KEY_PORT_NAME = "portName";
    private static final String CONNECT_INTENT_KEY_STATE = "state";
    static final Set<Integer> DEVICE_OVERRIDE_A2DP_ROUTE_ON_PLUG_SET;
    private static final String TAG = "AS.AudioDeviceInventory";
    private static final String mMetricsId = "audio.device.";
    private final ArrayMap<Integer, String> mApmConnectedDevices;
    private final AudioSystemAdapter mAudioSystem;
    private final LinkedHashMap<String, DeviceInfo> mConnectedDevices;
    final AudioRoutesInfo mCurAudioRoutes;
    final RemoteCallbackList<ICapturePresetDevicesRoleDispatcher> mDevRoleCapturePresetDispatchers;
    private AudioDeviceBroker mDeviceBroker;
    private final Object mDevicesLock;
    private int mLastMusicBTdeviceConnected;
    final RemoteCallbackList<IStrategyPreferredDevicesDispatcher> mPrefDevDispatchers;
    private final ArrayMap<Integer, List<AudioDeviceAttributes>> mPreferredDevices;
    private final ArrayMap<Integer, List<AudioDeviceAttributes>> mPreferredDevicesForCapturePreset;
    final RemoteCallbackList<IAudioRoutesObserver> mRoutesObservers;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AudioDeviceInventory(AudioDeviceBroker broker) {
        this.mDevicesLock = new Object();
        this.mLastMusicBTdeviceConnected = 0;
        this.mConnectedDevices = new LinkedHashMap<String, DeviceInfo>() { // from class: com.android.server.audio.AudioDeviceInventory.1
            /* JADX DEBUG: Method merged with bridge method */
            @Override // java.util.HashMap, java.util.AbstractMap, java.util.Map
            public DeviceInfo put(String key, DeviceInfo value) {
                DeviceInfo result = (DeviceInfo) super.put((AnonymousClass1) key, (String) value);
                record("put", true, key, value);
                return result;
            }

            /* JADX DEBUG: Method merged with bridge method */
            @Override // java.util.HashMap, java.util.Map
            public DeviceInfo putIfAbsent(String key, DeviceInfo value) {
                DeviceInfo result = (DeviceInfo) super.putIfAbsent((AnonymousClass1) key, (String) value);
                if (result == null) {
                    record("putIfAbsent", true, key, value);
                }
                return result;
            }

            /* JADX DEBUG: Method merged with bridge method */
            @Override // java.util.HashMap, java.util.AbstractMap, java.util.Map
            public DeviceInfo remove(Object key) {
                DeviceInfo result = (DeviceInfo) super.remove(key);
                if (result != null) {
                    record("remove", false, (String) key, result);
                }
                return result;
            }

            @Override // java.util.HashMap, java.util.Map
            public boolean remove(Object key, Object value) {
                boolean result = super.remove(key, value);
                if (result) {
                    record("remove", false, (String) key, (DeviceInfo) value);
                }
                return result;
            }

            private void record(String event, boolean connected, String key, DeviceInfo value) {
                new MediaMetrics.Item(AudioDeviceInventory.mMetricsId + AudioSystem.getDeviceName(value.mDeviceType)).set(MediaMetrics.Property.ADDRESS, value.mDeviceAddress).set(MediaMetrics.Property.EVENT, event).set(MediaMetrics.Property.NAME, value.mDeviceName).set(MediaMetrics.Property.STATE, connected ? "connected" : "disconnected").record();
            }
        };
        this.mApmConnectedDevices = new ArrayMap<>();
        this.mPreferredDevices = new ArrayMap<>();
        this.mPreferredDevicesForCapturePreset = new ArrayMap<>();
        this.mCurAudioRoutes = new AudioRoutesInfo();
        this.mRoutesObservers = new RemoteCallbackList<>();
        this.mPrefDevDispatchers = new RemoteCallbackList<>();
        this.mDevRoleCapturePresetDispatchers = new RemoteCallbackList<>();
        this.mDeviceBroker = broker;
        this.mAudioSystem = AudioSystemAdapter.getDefaultAdapter();
    }

    AudioDeviceInventory(AudioSystemAdapter audioSystem) {
        this.mDevicesLock = new Object();
        this.mLastMusicBTdeviceConnected = 0;
        this.mConnectedDevices = new LinkedHashMap<String, DeviceInfo>() { // from class: com.android.server.audio.AudioDeviceInventory.1
            /* JADX DEBUG: Method merged with bridge method */
            @Override // java.util.HashMap, java.util.AbstractMap, java.util.Map
            public DeviceInfo put(String key, DeviceInfo value) {
                DeviceInfo result = (DeviceInfo) super.put((AnonymousClass1) key, (String) value);
                record("put", true, key, value);
                return result;
            }

            /* JADX DEBUG: Method merged with bridge method */
            @Override // java.util.HashMap, java.util.Map
            public DeviceInfo putIfAbsent(String key, DeviceInfo value) {
                DeviceInfo result = (DeviceInfo) super.putIfAbsent((AnonymousClass1) key, (String) value);
                if (result == null) {
                    record("putIfAbsent", true, key, value);
                }
                return result;
            }

            /* JADX DEBUG: Method merged with bridge method */
            @Override // java.util.HashMap, java.util.AbstractMap, java.util.Map
            public DeviceInfo remove(Object key) {
                DeviceInfo result = (DeviceInfo) super.remove(key);
                if (result != null) {
                    record("remove", false, (String) key, result);
                }
                return result;
            }

            @Override // java.util.HashMap, java.util.Map
            public boolean remove(Object key, Object value) {
                boolean result = super.remove(key, value);
                if (result) {
                    record("remove", false, (String) key, (DeviceInfo) value);
                }
                return result;
            }

            private void record(String event, boolean connected, String key, DeviceInfo value) {
                new MediaMetrics.Item(AudioDeviceInventory.mMetricsId + AudioSystem.getDeviceName(value.mDeviceType)).set(MediaMetrics.Property.ADDRESS, value.mDeviceAddress).set(MediaMetrics.Property.EVENT, event).set(MediaMetrics.Property.NAME, value.mDeviceName).set(MediaMetrics.Property.STATE, connected ? "connected" : "disconnected").record();
            }
        };
        this.mApmConnectedDevices = new ArrayMap<>();
        this.mPreferredDevices = new ArrayMap<>();
        this.mPreferredDevicesForCapturePreset = new ArrayMap<>();
        this.mCurAudioRoutes = new AudioRoutesInfo();
        this.mRoutesObservers = new RemoteCallbackList<>();
        this.mPrefDevDispatchers = new RemoteCallbackList<>();
        this.mDevRoleCapturePresetDispatchers = new RemoteCallbackList<>();
        this.mDeviceBroker = null;
        this.mAudioSystem = audioSystem;
    }

    void setDeviceBroker(AudioDeviceBroker broker) {
        this.mDeviceBroker = broker;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class DeviceInfo {
        final String mDeviceAddress;
        int mDeviceCodecFormat;
        final String mDeviceName;
        final int mDeviceType;
        final UUID mSensorUuid;

        DeviceInfo(int deviceType, String deviceName, String deviceAddress, int deviceCodecFormat, UUID sensorUuid) {
            this.mDeviceType = deviceType;
            this.mDeviceName = deviceName == null ? "" : deviceName;
            this.mDeviceAddress = deviceAddress != null ? deviceAddress : "";
            this.mDeviceCodecFormat = deviceCodecFormat;
            this.mSensorUuid = sensorUuid;
        }

        DeviceInfo(int deviceType, String deviceName, String deviceAddress, int deviceCodecFormat) {
            this(deviceType, deviceName, deviceAddress, deviceCodecFormat, null);
        }

        public String toString() {
            return "[DeviceInfo: type:0x" + Integer.toHexString(this.mDeviceType) + " (" + AudioSystem.getDeviceName(this.mDeviceType) + ") name:" + this.mDeviceName + " addr:" + this.mDeviceAddress + " codec: " + Integer.toHexString(this.mDeviceCodecFormat) + " sensorUuid: " + Objects.toString(this.mSensorUuid) + "]";
        }

        String getKey() {
            return makeDeviceListKey(this.mDeviceType, this.mDeviceAddress);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static String makeDeviceListKey(int device, String deviceAddress) {
            return "0x" + Integer.toHexString(device) + ":" + deviceAddress;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class WiredDeviceConnectionState {
        public final AudioDeviceAttributes mAttributes;
        public final String mCaller;
        public boolean mForTest = false;
        public final int mState;

        WiredDeviceConnectionState(AudioDeviceAttributes attributes, int state, String caller) {
            this.mAttributes = attributes;
            this.mState = state;
            this.mCaller = caller;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(final PrintWriter pw, final String prefix) {
        pw.println("\n" + prefix + "BECOMING_NOISY_INTENT_DEVICES_SET=");
        BECOMING_NOISY_INTENT_DEVICES_SET.forEach(new Consumer() { // from class: com.android.server.audio.AudioDeviceInventory$$ExternalSyntheticLambda2
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                Integer num = (Integer) obj;
                pw.print(" 0x" + Integer.toHexString(num.intValue()));
            }
        });
        pw.println("\n" + prefix + "Preferred devices for strategy:");
        this.mPreferredDevices.forEach(new BiConsumer() { // from class: com.android.server.audio.AudioDeviceInventory$$ExternalSyntheticLambda3
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                pw.println("  " + prefix + "strategy:" + ((Integer) obj) + " device:" + ((List) obj2));
            }
        });
        pw.println("\n" + prefix + "Connected devices:");
        this.mConnectedDevices.forEach(new BiConsumer() { // from class: com.android.server.audio.AudioDeviceInventory$$ExternalSyntheticLambda4
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                String str = (String) obj;
                AudioDeviceInventory.DeviceInfo deviceInfo = (AudioDeviceInventory.DeviceInfo) obj2;
                pw.println("  " + prefix + deviceInfo.toString());
            }
        });
        pw.println("\n" + prefix + "APM Connected device (A2DP sink only):");
        this.mApmConnectedDevices.forEach(new BiConsumer() { // from class: com.android.server.audio.AudioDeviceInventory$$ExternalSyntheticLambda5
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                Integer num = (Integer) obj;
                pw.println("  " + prefix + " type:0x" + Integer.toHexString(num.intValue()) + " (" + AudioSystem.getDeviceName(num.intValue()) + ") addr:" + ((String) obj2));
            }
        });
        this.mPreferredDevicesForCapturePreset.forEach(new BiConsumer() { // from class: com.android.server.audio.AudioDeviceInventory$$ExternalSyntheticLambda6
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                pw.println("  " + prefix + "capturePreset:" + ((Integer) obj) + " devices:" + ((List) obj2));
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onRestoreDevices() {
        synchronized (this.mDevicesLock) {
            for (DeviceInfo di : this.mConnectedDevices.values()) {
                this.mAudioSystem.setDeviceConnectionState(new AudioDeviceAttributes(di.mDeviceType, di.mDeviceAddress, di.mDeviceName), 1, di.mDeviceCodecFormat);
            }
        }
        synchronized (this.mPreferredDevices) {
            this.mPreferredDevices.forEach(new BiConsumer() { // from class: com.android.server.audio.AudioDeviceInventory$$ExternalSyntheticLambda12
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    AudioDeviceInventory.this.m1789x8c157edf((Integer) obj, (List) obj2);
                }
            });
        }
        synchronized (this.mPreferredDevicesForCapturePreset) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onRestoreDevices$5$com-android-server-audio-AudioDeviceInventory  reason: not valid java name */
    public /* synthetic */ void m1789x8c157edf(Integer strategy, List devices) {
        this.mAudioSystem.setDevicesRoleForStrategy(strategy.intValue(), 1, devices);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSetBtActiveDevice(AudioDeviceBroker.BtDeviceInfo btInfo, int streamType) {
        String address;
        int i;
        if (AudioService.DEBUG_DEVICES) {
            Log.d(TAG, "onSetBtActiveDevice, btDevice=" + btInfo.mDevice + ", profile=" + BluetoothProfile.getProfileName(btInfo.mProfile) + ", state=" + BluetoothProfile.getConnectionStateName(btInfo.mState) + ", device=0x" + Integer.toHexString(btInfo.mAudioSystemDevice) + ", vol=" + btInfo.mVolume + ", streamType=" + streamType);
        }
        String address2 = btInfo.mDevice.getAddress();
        if (BluetoothAdapter.checkBluetoothAddress(address2)) {
            address = address2;
        } else {
            address = "";
        }
        AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent("BT connected: addr=" + address + " profile=" + btInfo.mProfile + " state=" + btInfo.mState + " codec=" + AudioSystem.audioFormatToString(btInfo.mCodec)));
        new MediaMetrics.Item("audio.device.onSetBtActiveDevice").set(MediaMetrics.Property.STATUS, Integer.valueOf(btInfo.mProfile)).set(MediaMetrics.Property.DEVICE, AudioSystem.getDeviceName(btInfo.mAudioSystemDevice)).set(MediaMetrics.Property.ADDRESS, address).set(MediaMetrics.Property.ENCODING, AudioSystem.audioFormatToString(btInfo.mCodec)).set(MediaMetrics.Property.EVENT, "onSetBtActiveDevice").set(MediaMetrics.Property.STREAM_TYPE, AudioSystem.streamToString(streamType)).set(MediaMetrics.Property.STATE, btInfo.mState == 2 ? "connected" : "disconnected").record();
        synchronized (this.mDevicesLock) {
            String key = DeviceInfo.makeDeviceListKey(btInfo.mAudioSystemDevice, address);
            DeviceInfo di = this.mConnectedDevices.get(key);
            boolean z = true;
            boolean isConnected = di != null;
            boolean switchToUnavailable = isConnected && btInfo.mState != 2;
            if (isConnected || btInfo.mState != 2) {
                z = false;
            }
            boolean switchToAvailable = z;
            switch (btInfo.mProfile) {
                case 2:
                    if (switchToUnavailable) {
                        makeA2dpDeviceUnavailableNow(address, di.mDeviceCodecFormat);
                        break;
                    } else if (switchToAvailable) {
                        int a2dpVolIndex = -1;
                        if (this.mLastMusicBTdeviceConnected == 536870912 && btInfo.mVolume == -1) {
                            a2dpVolIndex = this.mDeviceBroker.getVssVolumeForDevice(streamType, 536870912);
                        }
                        if (btInfo.mVolume != -1) {
                            a2dpVolIndex = btInfo.mVolume * 10;
                        }
                        this.mLastMusicBTdeviceConnected = btInfo.mAudioSystemDevice;
                        if (a2dpVolIndex != -1) {
                            this.mDeviceBroker.postSetVolumeIndexOnDevice(3, a2dpVolIndex, btInfo.mAudioSystemDevice, "onSetBtActiveDevice");
                        }
                        makeA2dpDeviceAvailable(address, BtHelper.getName(btInfo.mDevice), "onSetBtActiveDevice", btInfo.mCodec);
                        break;
                    }
                    break;
                case 11:
                    if (switchToUnavailable) {
                        m1785x1cfef7a1(address);
                        break;
                    } else if (switchToAvailable) {
                        makeA2dpSrcAvailable(address);
                        break;
                    }
                    break;
                case 21:
                    if (switchToUnavailable) {
                        m1786xec745126(address);
                        break;
                    } else if (switchToAvailable) {
                        makeHearingAidDeviceAvailable(address, BtHelper.getName(btInfo.mDevice), streamType, "onSetBtActiveDevice");
                        break;
                    }
                    break;
                case 22:
                case 26:
                    if (switchToUnavailable) {
                        makeLeAudioDeviceUnavailable(address, btInfo.mAudioSystemDevice);
                        break;
                    } else if (!switchToAvailable) {
                        break;
                    } else {
                        makeLeAudioDeviceAvailable(address, BtHelper.getName(btInfo.mDevice), streamType, btInfo.mAudioSystemDevice, "onSetBtActiveDevice");
                        if (streamType != -1) {
                            int leAudioVolIndex = -1;
                            if (this.mLastMusicBTdeviceConnected == 128) {
                                i = 536870912;
                                if (btInfo.mAudioSystemDevice == 536870912 && btInfo.mVolume == -1) {
                                    leAudioVolIndex = this.mDeviceBroker.getVssVolumeForDevice(streamType, 128);
                                }
                            } else {
                                i = 536870912;
                            }
                            if (btInfo.mAudioSystemDevice == i) {
                                this.mLastMusicBTdeviceConnected = btInfo.mAudioSystemDevice;
                            }
                            if (btInfo.mVolume != -1) {
                                leAudioVolIndex = btInfo.mVolume * 10;
                            }
                            if (leAudioVolIndex != -1) {
                                int maxIndex = this.mDeviceBroker.getMaxVssVolumeForStream(streamType);
                                this.mDeviceBroker.postSetLeAudioVolumeIndex(leAudioVolIndex, maxIndex, streamType);
                                this.mDeviceBroker.postSetVolumeIndexOnDevice(3, leAudioVolIndex, btInfo.mAudioSystemDevice, "onSetBtActiveDevice");
                            } else {
                                int leAudioVolIndex2 = this.mDeviceBroker.getVssVolumeForDevice(streamType, btInfo.mAudioSystemDevice);
                                int maxIndex2 = this.mDeviceBroker.getMaxVssVolumeForStream(streamType);
                                this.mDeviceBroker.postSetLeAudioVolumeIndex(leAudioVolIndex2, maxIndex2, streamType);
                                this.mDeviceBroker.postApplyVolumeOnDevice(streamType, btInfo.mAudioSystemDevice, "onSetBtActiveDevice");
                            }
                            break;
                        } else {
                            return;
                        }
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Invalid profile " + BluetoothProfile.getProfileName(btInfo.mProfile));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onBluetoothA2dpDeviceConfigChange(BtHelper.BluetoothA2dpDeviceInfo btInfo, int event) {
        String address;
        MediaMetrics.Item mmi = new MediaMetrics.Item("audio.device.onBluetoothA2dpDeviceConfigChange").set(MediaMetrics.Property.EVENT, BtHelper.a2dpDeviceEventToString(event));
        BluetoothDevice btDevice = btInfo.getBtDevice();
        if (btDevice == null) {
            mmi.set(MediaMetrics.Property.EARLY_RETURN, "btDevice null").record();
            return;
        }
        if (AudioService.DEBUG_DEVICES) {
            Log.d(TAG, "onBluetoothA2dpDeviceConfigChange btDevice=" + btDevice);
        }
        int a2dpVolume = btInfo.getVolume();
        int a2dpCodec = btInfo.getCodec();
        String address2 = btDevice.getAddress();
        if (BluetoothAdapter.checkBluetoothAddress(address2)) {
            address = address2;
        } else {
            address = "";
        }
        AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent("onBluetoothA2dpDeviceConfigChange addr=" + address + " event=" + BtHelper.a2dpDeviceEventToString(event)));
        synchronized (this.mDevicesLock) {
            if (this.mDeviceBroker.hasScheduledA2dpConnection(btDevice)) {
                AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent("A2dp config change ignored (scheduled connection change)").printLog(TAG));
                mmi.set(MediaMetrics.Property.EARLY_RETURN, "A2dp config change ignored").record();
                return;
            }
            String key = DeviceInfo.makeDeviceListKey(128, address);
            DeviceInfo di = this.mConnectedDevices.get(key);
            if (di == null) {
                Log.e(TAG, "invalid null DeviceInfo in onBluetoothA2dpDeviceConfigChange");
                mmi.set(MediaMetrics.Property.EARLY_RETURN, "null DeviceInfo").record();
                return;
            }
            mmi.set(MediaMetrics.Property.ADDRESS, address).set(MediaMetrics.Property.ENCODING, AudioSystem.audioFormatToString(a2dpCodec)).set(MediaMetrics.Property.INDEX, Integer.valueOf(a2dpVolume)).set(MediaMetrics.Property.NAME, di.mDeviceName);
            if (event == 1) {
                if (a2dpVolume != -1) {
                    this.mDeviceBroker.postSetVolumeIndexOnDevice(3, a2dpVolume * 10, 128, "onBluetoothA2dpDeviceConfigChange");
                }
            } else if (event == 0 && di.mDeviceCodecFormat != a2dpCodec) {
                di.mDeviceCodecFormat = a2dpCodec;
                this.mConnectedDevices.replace(key, di);
            }
            int res = this.mAudioSystem.handleDeviceConfigChange(128, address, BtHelper.getName(btDevice), a2dpCodec);
            if (res != 0) {
                AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent("APM handleDeviceConfigChange failed for A2DP device addr=" + address + " codec=" + AudioSystem.audioFormatToString(a2dpCodec)).printLog(TAG));
                int musicDevice = this.mDeviceBroker.getDeviceForStream(3);
                setBluetoothActiveDevice(new AudioDeviceBroker.BtDeviceInfo(btDevice, 2, 0, musicDevice, 128));
            } else {
                AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent("APM handleDeviceConfigChange success for A2DP device addr=" + address + " codec=" + AudioSystem.audioFormatToString(a2dpCodec)).printLog(TAG));
            }
            mmi.record();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onMakeA2dpDeviceUnavailableNow(String address, int a2dpCodec) {
        synchronized (this.mDevicesLock) {
            makeA2dpDeviceUnavailableNow(address, a2dpCodec);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onReportNewRoutes() {
        AudioRoutesInfo routes;
        int n = this.mRoutesObservers.beginBroadcast();
        if (n > 0) {
            new MediaMetrics.Item("audio.device.onReportNewRoutes").set(MediaMetrics.Property.OBSERVERS, Integer.valueOf(n)).record();
            synchronized (this.mCurAudioRoutes) {
                routes = new AudioRoutesInfo(this.mCurAudioRoutes);
            }
            while (n > 0) {
                n--;
                IAudioRoutesObserver obs = this.mRoutesObservers.getBroadcastItem(n);
                try {
                    obs.dispatchAudioRoutesChanged(routes);
                } catch (RemoteException e) {
                }
            }
        }
        this.mRoutesObservers.finishBroadcast();
        this.mDeviceBroker.postObserveDevicesForAllStreams();
    }

    static {
        HashSet hashSet = new HashSet();
        DEVICE_OVERRIDE_A2DP_ROUTE_ON_PLUG_SET = hashSet;
        hashSet.add(4);
        hashSet.add(8);
        hashSet.add(131072);
        hashSet.addAll(AudioSystem.DEVICE_OUT_ALL_USB_SET);
        HashSet hashSet2 = new HashSet();
        BECOMING_NOISY_INTENT_DEVICES_SET = hashSet2;
        hashSet2.add(4);
        hashSet2.add(8);
        hashSet2.add(1024);
        hashSet2.add(2048);
        hashSet2.add(131072);
        hashSet2.add(134217728);
        hashSet2.add(536870912);
        hashSet2.add(536870914);
        hashSet2.addAll(AudioSystem.DEVICE_OUT_ALL_A2DP_SET);
        hashSet2.addAll(AudioSystem.DEVICE_OUT_ALL_USB_SET);
        hashSet2.addAll(AudioSystem.DEVICE_OUT_ALL_BLE_SET);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSetWiredDeviceConnectionState(WiredDeviceConnectionState wdcs) {
        int type = wdcs.mAttributes.getInternalType();
        AudioService.sDeviceLogger.log(new AudioServiceEvents.WiredDevConnectEvent(wdcs));
        MediaMetrics.Item mmi = new MediaMetrics.Item("audio.device.onSetWiredDeviceConnectionState").set(MediaMetrics.Property.ADDRESS, wdcs.mAttributes.getAddress()).set(MediaMetrics.Property.DEVICE, AudioSystem.getDeviceName(type)).set(MediaMetrics.Property.STATE, wdcs.mState == 0 ? "disconnected" : "connected");
        synchronized (this.mDevicesLock) {
            boolean z = true;
            if (wdcs.mState == 0 && DEVICE_OVERRIDE_A2DP_ROUTE_ON_PLUG_SET.contains(Integer.valueOf(type))) {
                this.mDeviceBroker.setBluetoothA2dpOnInt(true, false, "onSetWiredDeviceConnectionState state DISCONNECTED");
            }
            AudioDeviceAttributes audioDeviceAttributes = wdcs.mAttributes;
            if (wdcs.mState != 1) {
                z = false;
            }
            if (!handleDeviceConnection(audioDeviceAttributes, z, wdcs.mForTest)) {
                mmi.set(MediaMetrics.Property.EARLY_RETURN, "change of connection state failed").record();
                return;
            }
            if (wdcs.mState != 0) {
                if (DEVICE_OVERRIDE_A2DP_ROUTE_ON_PLUG_SET.contains(Integer.valueOf(type))) {
                    this.mDeviceBroker.setBluetoothA2dpOnInt(false, false, "onSetWiredDeviceConnectionState state not DISCONNECTED");
                }
                this.mDeviceBroker.checkMusicActive(type, wdcs.mCaller);
            }
            if (type == 1024) {
                this.mDeviceBroker.checkVolumeCecOnHdmiConnection(wdcs.mState, wdcs.mCaller);
            }
            sendDeviceConnectionIntent(type, wdcs.mState, wdcs.mAttributes.getAddress(), wdcs.mAttributes.getName());
            updateAudioRoutes(type, wdcs.mState);
            mmi.record();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onToggleHdmi() {
        MediaMetrics.Item mmi = new MediaMetrics.Item("audio.device.onToggleHdmi").set(MediaMetrics.Property.DEVICE, AudioSystem.getDeviceName(1024));
        synchronized (this.mDevicesLock) {
            String key = DeviceInfo.makeDeviceListKey(1024, "");
            DeviceInfo di = this.mConnectedDevices.get(key);
            if (di == null) {
                Log.e(TAG, "invalid null DeviceInfo in onToggleHdmi");
                mmi.set(MediaMetrics.Property.EARLY_RETURN, "invalid null DeviceInfo").record();
                return;
            }
            setWiredDeviceConnectionState(new AudioDeviceAttributes(1024, ""), 0, PackageManagerService.PLATFORM_PACKAGE_NAME, false);
            setWiredDeviceConnectionState(new AudioDeviceAttributes(1024, ""), 1, PackageManagerService.PLATFORM_PACKAGE_NAME, false);
            mmi.record();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSaveSetPreferredDevices(int strategy, List<AudioDeviceAttributes> devices) {
        this.mPreferredDevices.put(Integer.valueOf(strategy), devices);
        dispatchPreferredDevice(strategy, devices);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSaveRemovePreferredDevices(int strategy) {
        this.mPreferredDevices.remove(Integer.valueOf(strategy));
        dispatchPreferredDevice(strategy, new ArrayList());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSaveSetPreferredDevicesForCapturePreset(int capturePreset, List<AudioDeviceAttributes> devices) {
        this.mPreferredDevicesForCapturePreset.put(Integer.valueOf(capturePreset), devices);
        dispatchDevicesRoleForCapturePreset(capturePreset, 1, devices);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSaveClearPreferredDevicesForCapturePreset(int capturePreset) {
        this.mPreferredDevicesForCapturePreset.remove(Integer.valueOf(capturePreset));
        dispatchDevicesRoleForCapturePreset(capturePreset, 1, new ArrayList());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int setPreferredDevicesForStrategySync(int strategy, List<AudioDeviceAttributes> devices) {
        long identity = Binder.clearCallingIdentity();
        AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent("setPreferredDevicesForStrategySync, strategy: " + strategy + " devices: " + devices).printLog(TAG));
        long diff = SystemClock.uptimeMillis();
        int status = this.mAudioSystem.setDevicesRoleForStrategy(strategy, 1, devices);
        Binder.restoreCallingIdentity(identity);
        if (status == 0) {
            AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent("setPreferredDevicesForStrategySync, strategy: " + strategy + ", APM made devices: " + devices + "preferred device in" + (SystemClock.uptimeMillis() - diff) + "ms").printLog(TAG));
            this.mDeviceBroker.postSaveSetPreferredDevicesForStrategy(strategy, devices);
        } else {
            AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent("setPreferredDevicesForStrategySync, strategy: " + strategy + ", APM fail to set devices: " + devices).printLog(TAG));
        }
        return status;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int removePreferredDevicesForStrategySync(int strategy) {
        long identity = Binder.clearCallingIdentity();
        AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent("removePreferredDevicesForStrategySync, strategy: " + strategy).printLog(TAG));
        long diff = SystemClock.uptimeMillis();
        int status = this.mAudioSystem.removeDevicesRoleForStrategy(strategy, 1);
        Binder.restoreCallingIdentity(identity);
        if (status == 0) {
            AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent("removePreferredDevicesForStrategySync APM removed, strategy: " + strategy + ", " + (SystemClock.uptimeMillis() - diff) + "ms").printLog(TAG));
            this.mDeviceBroker.postSaveRemovePreferredDevicesForStrategy(strategy);
        }
        return status;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerStrategyPreferredDevicesDispatcher(IStrategyPreferredDevicesDispatcher dispatcher) {
        this.mPrefDevDispatchers.register(dispatcher);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterStrategyPreferredDevicesDispatcher(IStrategyPreferredDevicesDispatcher dispatcher) {
        this.mPrefDevDispatchers.unregister(dispatcher);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int setPreferredDevicesForCapturePresetSync(int capturePreset, List<AudioDeviceAttributes> devices) {
        long identity = Binder.clearCallingIdentity();
        int status = this.mAudioSystem.setDevicesRoleForCapturePreset(capturePreset, 1, devices);
        Binder.restoreCallingIdentity(identity);
        if (status == 0) {
            this.mDeviceBroker.postSaveSetPreferredDevicesForCapturePreset(capturePreset, devices);
        }
        return status;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int clearPreferredDevicesForCapturePresetSync(int capturePreset) {
        long identity = Binder.clearCallingIdentity();
        int status = this.mAudioSystem.clearDevicesRoleForCapturePreset(capturePreset, 1);
        Binder.restoreCallingIdentity(identity);
        if (status == 0) {
            this.mDeviceBroker.postSaveClearPreferredDevicesForCapturePreset(capturePreset);
        }
        return status;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerCapturePresetDevicesRoleDispatcher(ICapturePresetDevicesRoleDispatcher dispatcher) {
        this.mDevRoleCapturePresetDispatchers.register(dispatcher);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterCapturePresetDevicesRoleDispatcher(ICapturePresetDevicesRoleDispatcher dispatcher) {
        this.mDevRoleCapturePresetDispatchers.unregister(dispatcher);
    }

    public boolean isDeviceConnected(AudioDeviceAttributes device) {
        boolean z;
        String key = DeviceInfo.makeDeviceListKey(device.getInternalType(), device.getAddress());
        synchronized (this.mDevicesLock) {
            z = this.mConnectedDevices.get(key) != null;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean handleDeviceConnection(AudioDeviceAttributes attributes, boolean connect, boolean isForTesting) {
        int res;
        String mDeviceInString;
        int device = attributes.getInternalType();
        String address = attributes.getAddress();
        String deviceName = attributes.getName();
        if (AudioService.DEBUG_DEVICES) {
            Slog.i(TAG, "handleDeviceConnection(" + connect + " dev:" + Integer.toHexString(device) + " address:" + address + " name:" + deviceName + ")");
            if ((device & Integer.MIN_VALUE) == Integer.MIN_VALUE) {
                mDeviceInString = AudioSystem.getInputDeviceName(device);
            } else {
                mDeviceInString = AudioSystem.getOutputDeviceName(device);
            }
            Log.i(TAG, "handleDeviceConnection(" + connect + " dev:" + Integer.toHexString(device) + "[" + mDeviceInString + "] address:" + address + " name:" + deviceName + ")");
        }
        MediaMetrics.Item mmi = new MediaMetrics.Item("audio.device.handleDeviceConnection").set(MediaMetrics.Property.ADDRESS, address).set(MediaMetrics.Property.DEVICE, AudioSystem.getDeviceName(device)).set(MediaMetrics.Property.MODE, connect ? "connect" : "disconnect").set(MediaMetrics.Property.NAME, deviceName);
        synchronized (this.mDevicesLock) {
            String deviceKey = DeviceInfo.makeDeviceListKey(device, address);
            if (AudioService.DEBUG_DEVICES) {
                Slog.i(TAG, "deviceKey:" + deviceKey);
            }
            DeviceInfo di = this.mConnectedDevices.get(deviceKey);
            boolean isConnected = di != null;
            if (AudioService.DEBUG_DEVICES) {
                Slog.i(TAG, "deviceInfo:" + di + " is(already)Connected:" + isConnected);
            }
            if (connect && !isConnected) {
                if (!isForTesting) {
                    res = this.mAudioSystem.setDeviceConnectionState(attributes, 1, 0);
                    if (AudioService.DEBUG_DEVICES) {
                        Log.i(TAG, "handleDeviceConnection(connected dev:" + Integer.toHexString(device) + ", res=" + res + ")");
                    }
                } else {
                    res = 0;
                }
                if (res != 0) {
                    String reason = "not connecting device 0x" + Integer.toHexString(device) + " due to command error " + res;
                    if (AudioService.DEBUG_DEVICES) {
                        Slog.e(TAG, reason);
                    }
                    mmi.set(MediaMetrics.Property.EARLY_RETURN, reason).set(MediaMetrics.Property.STATE, "disconnected").record();
                    return false;
                }
                this.mConnectedDevices.put(deviceKey, new DeviceInfo(device, deviceName, address, 0));
                this.mDeviceBroker.postAccessoryPlugMediaUnmute(device);
                mmi.set(MediaMetrics.Property.STATE, "connected").record();
                return true;
            } else if (!connect && isConnected) {
                int result = this.mAudioSystem.setDeviceConnectionState(attributes, 0, 0);
                if (AudioService.DEBUG_DEVICES) {
                    Log.i(TAG, "handleDeviceConnection(disconnected dev:" + Integer.toHexString(device) + ", res=" + result + ")");
                }
                this.mConnectedDevices.remove(deviceKey);
                mmi.set(MediaMetrics.Property.STATE, "connected").record();
                return true;
            } else {
                if (AudioService.DEBUG_DEVICES) {
                    Log.w(TAG, "handleDeviceConnection() failed, deviceKey=" + deviceKey + ", deviceSpec=" + di + ", connect=" + connect);
                }
                mmi.set(MediaMetrics.Property.STATE, "disconnected").record();
                return false;
            }
        }
    }

    private void disconnectA2dp() {
        synchronized (this.mDevicesLock) {
            final ArraySet<String> toRemove = new ArraySet<>();
            this.mConnectedDevices.values().forEach(new Consumer() { // from class: com.android.server.audio.AudioDeviceInventory$$ExternalSyntheticLambda10
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    AudioDeviceInventory.lambda$disconnectA2dp$6(toRemove, (AudioDeviceInventory.DeviceInfo) obj);
                }
            });
            new MediaMetrics.Item("audio.device.disconnectA2dp").record();
            if (toRemove.size() > 0) {
                final int delay = checkSendBecomingNoisyIntentInt(128, 0, 0);
                toRemove.stream().forEach(new Consumer() { // from class: com.android.server.audio.AudioDeviceInventory$$ExternalSyntheticLambda11
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        AudioDeviceInventory.this.m1784x751a556c(delay, (String) obj);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$disconnectA2dp$6(ArraySet toRemove, DeviceInfo deviceInfo) {
        if (deviceInfo.mDeviceType == 128) {
            toRemove.add(deviceInfo.mDeviceAddress);
        }
    }

    private void disconnectA2dpSink() {
        synchronized (this.mDevicesLock) {
            final ArraySet<String> toRemove = new ArraySet<>();
            this.mConnectedDevices.values().forEach(new Consumer() { // from class: com.android.server.audio.AudioDeviceInventory$$ExternalSyntheticLambda7
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    AudioDeviceInventory.lambda$disconnectA2dpSink$8(toRemove, (AudioDeviceInventory.DeviceInfo) obj);
                }
            });
            new MediaMetrics.Item("audio.device.disconnectA2dpSink").record();
            toRemove.stream().forEach(new Consumer() { // from class: com.android.server.audio.AudioDeviceInventory$$ExternalSyntheticLambda8
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    AudioDeviceInventory.this.m1785x1cfef7a1((String) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$disconnectA2dpSink$8(ArraySet toRemove, DeviceInfo deviceInfo) {
        if (deviceInfo.mDeviceType == -2147352576) {
            toRemove.add(deviceInfo.mDeviceAddress);
        }
    }

    private void disconnectHearingAid() {
        synchronized (this.mDevicesLock) {
            final ArraySet<String> toRemove = new ArraySet<>();
            this.mConnectedDevices.values().forEach(new Consumer() { // from class: com.android.server.audio.AudioDeviceInventory$$ExternalSyntheticLambda13
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    AudioDeviceInventory.lambda$disconnectHearingAid$10(toRemove, (AudioDeviceInventory.DeviceInfo) obj);
                }
            });
            new MediaMetrics.Item("audio.device.disconnectHearingAid").record();
            if (toRemove.size() > 0) {
                checkSendBecomingNoisyIntentInt(134217728, 0, 0);
                toRemove.stream().forEach(new Consumer() { // from class: com.android.server.audio.AudioDeviceInventory$$ExternalSyntheticLambda14
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        AudioDeviceInventory.this.m1786xec745126((String) obj);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$disconnectHearingAid$10(ArraySet toRemove, DeviceInfo deviceInfo) {
        if (deviceInfo.mDeviceType == 134217728) {
            toRemove.add(deviceInfo.mDeviceAddress);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void onBtProfileDisconnected(int profile) {
        try {
            switch (profile) {
                case 2:
                    disconnectA2dp();
                    break;
                case 11:
                    disconnectA2dpSink();
                    break;
                case 21:
                    disconnectHearingAid();
                    break;
                case 22:
                    disconnectLeAudioUnicast();
                    break;
                case 26:
                    disconnectLeAudioBroadcast();
                    break;
                default:
                    Log.e(TAG, "onBtProfileDisconnected: Not a valid profile to disconnect " + BluetoothProfile.getProfileName(profile));
                    break;
            }
        } catch (Throwable th) {
            throw th;
        }
    }

    void disconnectLeAudio(final int device) {
        if (device != 536870912 && device != 536870914) {
            Log.e(TAG, "disconnectLeAudio: Can't disconnect not LE Audio device " + device);
            return;
        }
        synchronized (this.mDevicesLock) {
            final ArraySet<String> toRemove = new ArraySet<>();
            this.mConnectedDevices.values().forEach(new Consumer() { // from class: com.android.server.audio.AudioDeviceInventory$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    AudioDeviceInventory.lambda$disconnectLeAudio$12(device, toRemove, (AudioDeviceInventory.DeviceInfo) obj);
                }
            });
            new MediaMetrics.Item("audio.device.disconnectLeAudio").record();
            if (toRemove.size() > 0) {
                final int delay = checkSendBecomingNoisyIntentInt(device, 0, 0);
                toRemove.stream().forEach(new Consumer() { // from class: com.android.server.audio.AudioDeviceInventory$$ExternalSyntheticLambda1
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        AudioDeviceInventory.this.m1787xe2d42de5(device, delay, (String) obj);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$disconnectLeAudio$12(int device, ArraySet toRemove, DeviceInfo deviceInfo) {
        if (deviceInfo.mDeviceType == device) {
            toRemove.add(deviceInfo.mDeviceAddress);
        }
    }

    void disconnectLeAudioUnicast() {
        disconnectLeAudio(536870912);
    }

    void disconnectLeAudioBroadcast() {
        disconnectLeAudio(536870914);
    }

    int checkSendBecomingNoisyIntent(int device, int state, int musicDevice) {
        int checkSendBecomingNoisyIntentInt;
        synchronized (this.mDevicesLock) {
            checkSendBecomingNoisyIntentInt = checkSendBecomingNoisyIntentInt(device, state, musicDevice);
        }
        return checkSendBecomingNoisyIntentInt;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AudioRoutesInfo startWatchingRoutes(IAudioRoutesObserver observer) {
        AudioRoutesInfo routes;
        synchronized (this.mCurAudioRoutes) {
            routes = new AudioRoutesInfo(this.mCurAudioRoutes);
            this.mRoutesObservers.register(observer);
        }
        return routes;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AudioRoutesInfo getCurAudioRoutes() {
        return this.mCurAudioRoutes;
    }

    public int setBluetoothActiveDevice(AudioDeviceBroker.BtDeviceInfo info) {
        int asState;
        int asState2;
        synchronized (this.mDevicesLock) {
            if (!info.mSupprNoisy && (((info.mProfile == 22 || info.mProfile == 26) && info.mIsLeOutput) || info.mProfile == 21 || info.mProfile == 2)) {
                if (info.mState == 2) {
                    asState2 = 1;
                } else {
                    asState2 = 0;
                }
                asState = checkSendBecomingNoisyIntentInt(info.mAudioSystemDevice, asState2, info.mMusicDevice);
            } else {
                asState = 0;
            }
            if (AudioService.DEBUG_DEVICES) {
                Log.i(TAG, "setBluetoothActiveDevice device: " + info.mDevice + " profile: " + BluetoothProfile.getProfileName(info.mProfile) + " state: " + BluetoothProfile.getConnectionStateName(info.mState) + " delay(ms): " + asState + " codec:" + Integer.toHexString(info.mCodec) + " suppressNoisyIntent: " + info.mSupprNoisy);
            }
            this.mDeviceBroker.postBluetoothActiveDevice(info, asState);
            if (info.mProfile == 21 && info.mState == 2) {
                this.mDeviceBroker.setForceUse_Async(1, 0, "HEARING_AID set to CONNECTED");
            }
        }
        return asState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int setWiredDeviceConnectionState(AudioDeviceAttributes attributes, int state, String caller, boolean suppressNoisyIntent) {
        int delay;
        synchronized (this.mDevicesLock) {
            delay = 0;
            if (!suppressNoisyIntent) {
                delay = checkSendBecomingNoisyIntentInt(attributes.getInternalType(), state, 0);
            }
            Log.i(TAG, "setWiredDeviceConnectionState(), " + suppressNoisyIntent);
            if (AudioService.DEBUG_DEVICES) {
                Log.i(TAG, "setWiredDeviceConnectionState attributes:" + attributes + " state: " + state + " caller: " + caller);
            }
            this.mDeviceBroker.postSetWiredDeviceConnectionState(new WiredDeviceConnectionState(attributes, state, caller), delay);
        }
        return delay;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTestDeviceConnectionState(AudioDeviceAttributes device, int state) {
        WiredDeviceConnectionState connection = new WiredDeviceConnectionState(device, state, "com.android.server.audio");
        connection.mForTest = true;
        onSetWiredDeviceConnectionState(connection);
    }

    private void makeA2dpDeviceAvailable(String address, String name, String eventSource, int a2dpCodec) {
        this.mDeviceBroker.setBluetoothA2dpOnInt(true, true, eventSource);
        long diff = SystemClock.uptimeMillis();
        int res = this.mAudioSystem.setDeviceConnectionState(new AudioDeviceAttributes(128, address, name), 1, a2dpCodec);
        if (res != 0) {
            AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent("APM failed to make available A2DP device addr=" + address + " error=" + res).printLog(TAG));
        } else {
            AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent("A2DP device addr=" + address + " now available" + (SystemClock.uptimeMillis() - diff) + "ms").printLog(TAG));
        }
        this.mAudioSystem.setParameters("A2dpsuspendonly=false");
        UUID sensorUuid = UuidUtils.uuidFromAudioDeviceAttributes(new AudioDeviceAttributes(128, address));
        DeviceInfo di = new DeviceInfo(128, name, address, a2dpCodec, sensorUuid);
        String diKey = di.getKey();
        this.mConnectedDevices.put(diKey, di);
        this.mApmConnectedDevices.put(128, diKey);
        this.mDeviceBroker.postAccessoryPlugMediaUnmute(128);
        setCurrentAudioRouteNameIfPossible(name, true);
        this.mDeviceBroker.checkMusicActive(128, eventSource);
    }

    private void makeA2dpDeviceUnavailableNow(String address, int a2dpCodec) {
        MediaMetrics.Item mmi = new MediaMetrics.Item("audio.device.a2dp." + address).set(MediaMetrics.Property.ENCODING, AudioSystem.audioFormatToString(a2dpCodec)).set(MediaMetrics.Property.EVENT, "makeA2dpDeviceUnavailableNow");
        if (AudioService.DEBUG_DEVICES) {
            Log.d(TAG, "makeA2dpDeviceUnavailableNow address:" + address + " a2dpCodec=" + a2dpCodec);
        }
        if (address == null) {
            mmi.set(MediaMetrics.Property.EARLY_RETURN, "address null").record();
            return;
        }
        String deviceToRemoveKey = DeviceInfo.makeDeviceListKey(128, address);
        this.mConnectedDevices.remove(deviceToRemoveKey);
        if (!deviceToRemoveKey.equals(this.mApmConnectedDevices.get(128))) {
            AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent("A2DP device " + address + " made unavailable, was not used").printLog(TAG));
            mmi.set(MediaMetrics.Property.EARLY_RETURN, "A2DP device made unavailable, was not used").record();
            return;
        }
        this.mDeviceBroker.clearAvrcpAbsoluteVolumeSupported();
        long diff = SystemClock.uptimeMillis();
        int res = this.mAudioSystem.setDeviceConnectionState(new AudioDeviceAttributes(128, address), 0, a2dpCodec);
        if (res != 0) {
            AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent("APM failed to make unavailable A2DP device addr=" + address + " error=" + res).printLog(TAG));
        } else {
            AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent("A2DP device addr=" + address + " made unavailable" + (SystemClock.uptimeMillis() - diff) + "ms").printLog(TAG));
        }
        this.mApmConnectedDevices.remove(128);
        setCurrentAudioRouteNameIfPossible(null, true);
        mmi.record();
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: makeA2dpDeviceUnavailableLater */
    public void m1784x751a556c(String address, int delayMs) {
        this.mAudioSystem.setParameters("A2dpsuspendonly=true");
        String deviceKey = DeviceInfo.makeDeviceListKey(128, address);
        DeviceInfo deviceInfo = this.mConnectedDevices.get(deviceKey);
        int a2dpCodec = deviceInfo != null ? deviceInfo.mDeviceCodecFormat : 0;
        if (AudioService.DEBUG_DEVICES) {
            Log.d(TAG, "makeA2dpDeviceUnavailableLater address:" + address + " a2dpCodec=" + a2dpCodec + " delayMs=" + delayMs);
        }
        this.mConnectedDevices.remove(deviceKey);
        this.mDeviceBroker.setA2dpTimeout(address, a2dpCodec, delayMs);
    }

    private void makeA2dpSrcAvailable(String address) {
        this.mAudioSystem.setDeviceConnectionState(new AudioDeviceAttributes((int) AudioDevice.IN_BLUETOOTH_A2DP, address), 1, 0);
        this.mConnectedDevices.put(DeviceInfo.makeDeviceListKey(AudioDevice.IN_BLUETOOTH_A2DP, address), new DeviceInfo(AudioDevice.IN_BLUETOOTH_A2DP, "", address, 0));
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: makeA2dpSrcUnavailable */
    public void m1785x1cfef7a1(String address) {
        this.mAudioSystem.setDeviceConnectionState(new AudioDeviceAttributes((int) AudioDevice.IN_BLUETOOTH_A2DP, address), 0, 0);
        this.mConnectedDevices.remove(DeviceInfo.makeDeviceListKey(AudioDevice.IN_BLUETOOTH_A2DP, address));
    }

    private void makeHearingAidDeviceAvailable(String address, String name, int streamType, String eventSource) {
        int hearingAidVolIndex = this.mDeviceBroker.getVssVolumeForDevice(streamType, 134217728);
        this.mDeviceBroker.postSetHearingAidVolumeIndex(hearingAidVolIndex, streamType);
        long diff = SystemClock.uptimeMillis();
        int res = this.mAudioSystem.setDeviceConnectionState(new AudioDeviceAttributes(134217728, address, name), 1, 0);
        if (res != 0) {
            AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent("APM failed to make available hearing AID device addr=" + address + " error=" + res).printLog(TAG));
        } else {
            AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent("Hearing Aid device addr=" + address + " now available" + (SystemClock.uptimeMillis() - diff) + "ms").printLog(TAG));
        }
        this.mConnectedDevices.put(DeviceInfo.makeDeviceListKey(134217728, address), new DeviceInfo(134217728, name, address, 0));
        this.mDeviceBroker.postAccessoryPlugMediaUnmute(134217728);
        this.mDeviceBroker.postApplyVolumeOnDevice(streamType, 134217728, "makeHearingAidDeviceAvailable");
        setCurrentAudioRouteNameIfPossible(name, false);
        new MediaMetrics.Item("audio.device.makeHearingAidDeviceAvailable").set(MediaMetrics.Property.ADDRESS, address != null ? address : "").set(MediaMetrics.Property.DEVICE, AudioSystem.getDeviceName(134217728)).set(MediaMetrics.Property.NAME, name).set(MediaMetrics.Property.STREAM_TYPE, AudioSystem.streamToString(streamType)).record();
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: makeHearingAidDeviceUnavailable */
    public void m1786xec745126(String address) {
        this.mAudioSystem.setDeviceConnectionState(new AudioDeviceAttributes(134217728, address), 0, 0);
        this.mConnectedDevices.remove(DeviceInfo.makeDeviceListKey(134217728, address));
        setCurrentAudioRouteNameIfPossible(null, false);
        new MediaMetrics.Item("audio.device.makeHearingAidDeviceUnavailable").set(MediaMetrics.Property.ADDRESS, address != null ? address : "").set(MediaMetrics.Property.DEVICE, AudioSystem.getDeviceName(134217728)).record();
    }

    private void makeLeAudioDeviceAvailable(String address, String name, int streamType, int device, String eventSource) {
        if (device != 0) {
            this.mDeviceBroker.setBluetoothA2dpOnInt(true, false, eventSource);
            long diff = SystemClock.uptimeMillis();
            int res = AudioSystem.setDeviceConnectionState(new AudioDeviceAttributes(device, address, name), 1, 0);
            if (res != 0) {
                AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent("APM failed to make available Le Audio device addr=" + address + " error=" + res).printLog(TAG));
            } else {
                AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent("LeAudio device addr=" + address + " now available" + (SystemClock.uptimeMillis() - diff) + "ms").printLog(TAG));
            }
            this.mConnectedDevices.put(DeviceInfo.makeDeviceListKey(device, address), new DeviceInfo(device, name, address, 0));
            this.mDeviceBroker.postAccessoryPlugMediaUnmute(device);
            setCurrentAudioRouteNameIfPossible(name, false);
        }
    }

    private void makeLeAudioDeviceUnavailable(String address, int device) {
        if (device != 0) {
            long diff = SystemClock.uptimeMillis();
            int res = AudioSystem.setDeviceConnectionState(new AudioDeviceAttributes(device, address), 0, 0);
            if (res != 0) {
                AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent("APM failed to make unavailable LeAudio device addr=" + address + " error=" + res).printLog(TAG));
            } else {
                AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent("LeAudio device addr=" + address + " made unavailable" + (SystemClock.uptimeMillis() - diff) + "ms").printLog(TAG));
            }
            this.mConnectedDevices.remove(DeviceInfo.makeDeviceListKey(device, address));
        }
        setCurrentAudioRouteNameIfPossible(null, false);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: makeLeAudioDeviceUnavailableLater */
    public void m1787xe2d42de5(String address, int device, int delayMs) {
        this.mConnectedDevices.remove(DeviceInfo.makeDeviceListKey(device, address));
        this.mDeviceBroker.setLeAudioTimeout(address, device, delayMs);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onMakeLeAudioDeviceUnavailableNow(String address, int device) {
        synchronized (this.mDevicesLock) {
            makeLeAudioDeviceUnavailable(address, device);
        }
    }

    private void setCurrentAudioRouteNameIfPossible(String name, boolean fromA2dp) {
        synchronized (this.mCurAudioRoutes) {
            if (TextUtils.equals(this.mCurAudioRoutes.bluetoothName, name)) {
                return;
            }
            if (name != null || !isCurrentDeviceConnected()) {
                this.mCurAudioRoutes.bluetoothName = name;
                this.mDeviceBroker.postReportNewRoutes(fromA2dp);
            }
        }
    }

    private boolean isCurrentDeviceConnected() {
        return this.mConnectedDevices.values().stream().anyMatch(new Predicate() { // from class: com.android.server.audio.AudioDeviceInventory$$ExternalSyntheticLambda9
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return AudioDeviceInventory.this.m1788xb39e4129((AudioDeviceInventory.DeviceInfo) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isCurrentDeviceConnected$14$com-android-server-audio-AudioDeviceInventory  reason: not valid java name */
    public /* synthetic */ boolean m1788xb39e4129(DeviceInfo deviceInfo) {
        return TextUtils.equals(deviceInfo.mDeviceName, this.mCurAudioRoutes.bluetoothName);
    }

    private int checkSendBecomingNoisyIntentInt(int device, int state, int musicDevice) {
        MediaMetrics.Item mmi = new MediaMetrics.Item("audio.device.checkSendBecomingNoisyIntentInt").set(MediaMetrics.Property.DEVICE, AudioSystem.getDeviceName(device)).set(MediaMetrics.Property.STATE, state == 1 ? "connected" : "disconnected");
        if (state != 0) {
            Log.i(TAG, "not sending NOISY: state=" + state);
            mmi.set(MediaMetrics.Property.DELAY_MS, 0).record();
            return 0;
        }
        Set<Integer> set = BECOMING_NOISY_INTENT_DEVICES_SET;
        if (!set.contains(Integer.valueOf(device))) {
            Log.i(TAG, "not sending NOISY: device=0x" + Integer.toHexString(device) + " not in set " + set);
            mmi.set(MediaMetrics.Property.DELAY_MS, 0).record();
            return 0;
        }
        int delay = 0;
        Set<Integer> devices = new HashSet<>();
        for (DeviceInfo di : this.mConnectedDevices.values()) {
            if ((di.mDeviceType & Integer.MIN_VALUE) == 0 && BECOMING_NOISY_INTENT_DEVICES_SET.contains(Integer.valueOf(di.mDeviceType))) {
                devices.add(Integer.valueOf(di.mDeviceType));
                Log.i(TAG, "NOISY: adding 0x" + Integer.toHexString(di.mDeviceType));
            }
        }
        if (musicDevice == 0) {
            musicDevice = this.mDeviceBroker.getDeviceForStream(3);
            Log.i(TAG, "NOISY: musicDevice changing from NONE to 0x" + Integer.toHexString(musicDevice));
        }
        boolean inCommunication = this.mDeviceBroker.isInCommunication();
        boolean singleAudioDeviceType = AudioSystem.isSingleAudioDeviceType(devices, device);
        boolean hasMediaDynamicPolicy = this.mDeviceBroker.hasMediaDynamicPolicy();
        if ((device == musicDevice || inCommunication) && singleAudioDeviceType && !hasMediaDynamicPolicy && musicDevice != 32768) {
            if (!this.mAudioSystem.isStreamActive(3, 0) && !this.mDeviceBroker.hasAudioFocusUsers()) {
                AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent("dropping ACTION_AUDIO_BECOMING_NOISY").printLog(TAG));
                mmi.set(MediaMetrics.Property.DELAY_MS, 0).record();
                return 0;
            }
            this.mDeviceBroker.postBroadcastBecomingNoisy();
            delay = 1000;
        } else {
            Log.i(TAG, "not sending NOISY: device:0x" + Integer.toHexString(device) + " musicDevice:0x" + Integer.toHexString(musicDevice) + " inComm:" + inCommunication + " mediaPolicy:" + hasMediaDynamicPolicy + " singleDevice:" + singleAudioDeviceType);
        }
        mmi.set(MediaMetrics.Property.DELAY_MS, Integer.valueOf(delay)).record();
        return delay;
    }

    private void sendDeviceConnectionIntent(int device, int state, String address, String deviceName) {
        String mDeviceInString;
        if (AudioService.DEBUG_DEVICES) {
            Slog.i(TAG, "sendDeviceConnectionIntent(dev:0x" + Integer.toHexString(device) + " state:0x" + Integer.toHexString(state) + " address:" + address + " name:" + deviceName + ");");
            if ((device & Integer.MIN_VALUE) == Integer.MIN_VALUE) {
                mDeviceInString = AudioSystem.getInputDeviceName(device);
            } else {
                mDeviceInString = AudioSystem.getOutputDeviceName(device);
            }
            Log.i(TAG, "sendDeviceConnectionIntent(dev:0x" + Integer.toHexString(device) + " state:0x" + Integer.toHexString(state) + "[" + mDeviceInString + "] address:" + address + " name:" + deviceName + ");");
        }
        Intent intent = new Intent();
        int i = 0;
        switch (device) {
            case AudioDevice.IN_USB_HEADSET /* -2113929216 */:
                if (AudioSystem.getDeviceConnectionState(67108864, "") == 1) {
                    intent.setAction("android.intent.action.HEADSET_PLUG");
                    intent.putExtra("microphone", 1);
                    break;
                } else {
                    return;
                }
            case 4:
                intent.setAction("android.intent.action.HEADSET_PLUG");
                intent.putExtra("microphone", 1);
                break;
            case 8:
            case 131072:
                intent.setAction("android.intent.action.HEADSET_PLUG");
                intent.putExtra("microphone", 0);
                break;
            case 1024:
            case 262144:
            case 262145:
                configureHdmiPlugIntent(intent, state);
                break;
            case 67108864:
                intent.setAction("android.intent.action.HEADSET_PLUG");
                if (AudioSystem.getDeviceConnectionState((int) AudioDevice.IN_USB_HEADSET, "") == 1) {
                    i = 1;
                }
                intent.putExtra("microphone", i);
                break;
        }
        if (intent.getAction() == null) {
            if (AudioService.DEBUG_DEVICES) {
                Log.e(TAG, "Headset Plugged-out broadcast is not send.Action is null");
            }
        } else if (state == 0 && intent.getAction() == "android.intent.action.HEADSET_PLUG" && (AudioSystem.getDeviceConnectionState(67108864, "") == 1 || AudioSystem.getDeviceConnectionState(8, "") == 1 || AudioSystem.getDeviceConnectionState(4, "") == 1)) {
            if (AudioService.DEBUG_DEVICES) {
                Log.e(TAG, "Headset Plugged-out broadcast is not send.Still headset is Plugged-in");
            }
        } else {
            intent.putExtra("state", state);
            intent.putExtra(CONNECT_INTENT_KEY_ADDRESS, address);
            intent.putExtra(CONNECT_INTENT_KEY_PORT_NAME, deviceName);
            intent.addFlags(1073741824);
            long ident = Binder.clearCallingIdentity();
            try {
                this.mDeviceBroker.broadcastStickyIntentToCurrentProfileGroup(intent);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    private void updateAudioRoutes(int device, int state) {
        int newConn;
        int connType = 0;
        switch (device) {
            case 4:
                connType = 1;
                break;
            case 8:
            case 131072:
                connType = 2;
                break;
            case 1024:
            case 262144:
            case 262145:
                connType = 8;
                break;
            case 4096:
                connType = 4;
                break;
            case 16384:
            case 67108864:
                connType = 16;
                break;
        }
        synchronized (this.mCurAudioRoutes) {
            if (connType == 0) {
                return;
            }
            int newConn2 = this.mCurAudioRoutes.mainType;
            if (state != 0) {
                newConn = newConn2 | connType;
            } else {
                newConn = newConn2 & (~connType);
            }
            if (newConn != this.mCurAudioRoutes.mainType) {
                this.mCurAudioRoutes.mainType = newConn;
                this.mDeviceBroker.postReportNewRoutes(false);
            }
        }
    }

    private void configureHdmiPlugIntent(Intent intent, int state) {
        int[] channelMasks;
        intent.setAction("android.media.action.HDMI_AUDIO_PLUG");
        intent.putExtra("android.media.extra.AUDIO_PLUG_STATE", state);
        if (state != 1) {
            return;
        }
        ArrayList<AudioPort> ports = new ArrayList<>();
        int[] portGeneration = new int[1];
        int status = AudioSystem.listAudioPorts(ports, portGeneration);
        if (status != 0) {
            Log.e(TAG, "listAudioPorts error " + status + " in configureHdmiPlugIntent");
            return;
        }
        Iterator<AudioPort> it = ports.iterator();
        while (it.hasNext()) {
            AudioPort next = it.next();
            if (next instanceof AudioDevicePort) {
                AudioDevicePort devicePort = (AudioDevicePort) next;
                if (devicePort.type() == 1024 || devicePort.type() == 262144 || devicePort.type() == 262145) {
                    int[] formats = AudioFormat.filterPublicFormats(devicePort.formats());
                    if (formats.length > 0) {
                        ArrayList<Integer> encodingList = new ArrayList<>(1);
                        for (int format : formats) {
                            if (format != 0) {
                                encodingList.add(Integer.valueOf(format));
                            }
                        }
                        int[] encodingArray = encodingList.stream().mapToInt(new ToIntFunction() { // from class: com.android.server.audio.AudioDeviceInventory$$ExternalSyntheticLambda15
                            @Override // java.util.function.ToIntFunction
                            public final int applyAsInt(Object obj) {
                                int intValue;
                                intValue = ((Integer) obj).intValue();
                                return intValue;
                            }
                        }).toArray();
                        intent.putExtra("android.media.extra.ENCODINGS", encodingArray);
                    }
                    int maxChannels = 0;
                    for (int mask : devicePort.channelMasks()) {
                        int channelCount = AudioFormat.channelCountFromOutChannelMask(mask);
                        if (channelCount > maxChannels) {
                            maxChannels = channelCount;
                        }
                    }
                    intent.putExtra("android.media.extra.MAX_CHANNEL_COUNT", maxChannels);
                }
            }
        }
    }

    private void dispatchPreferredDevice(int strategy, List<AudioDeviceAttributes> devices) {
        int nbDispatchers = this.mPrefDevDispatchers.beginBroadcast();
        for (int i = 0; i < nbDispatchers; i++) {
            try {
                this.mPrefDevDispatchers.getBroadcastItem(i).dispatchPrefDevicesChanged(strategy, devices);
            } catch (RemoteException e) {
            }
        }
        this.mPrefDevDispatchers.finishBroadcast();
    }

    private void dispatchDevicesRoleForCapturePreset(int capturePreset, int role, List<AudioDeviceAttributes> devices) {
        int nbDispatchers = this.mDevRoleCapturePresetDispatchers.beginBroadcast();
        for (int i = 0; i < nbDispatchers; i++) {
            try {
                this.mDevRoleCapturePresetDispatchers.getBroadcastItem(i).dispatchDevicesRoleChanged(capturePreset, role, devices);
            } catch (RemoteException e) {
            }
        }
        this.mDevRoleCapturePresetDispatchers.finishBroadcast();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UUID getDeviceSensorUuid(AudioDeviceAttributes device) {
        String key = DeviceInfo.makeDeviceListKey(device.getInternalType(), device.getAddress());
        synchronized (this.mDevicesLock) {
            DeviceInfo di = this.mConnectedDevices.get(key);
            if (di == null) {
                return null;
            }
            return di.mSensorUuid;
        }
    }

    public boolean isA2dpDeviceConnected(BluetoothDevice device) {
        boolean z;
        String key = DeviceInfo.makeDeviceListKey(128, device.getAddress());
        synchronized (this.mDevicesLock) {
            z = this.mConnectedDevices.get(key) != null;
        }
        return z;
    }
}
