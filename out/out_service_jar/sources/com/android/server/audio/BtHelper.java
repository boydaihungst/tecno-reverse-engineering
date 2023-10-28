package com.android.server.audio;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothHearingAid;
import android.bluetooth.BluetoothLeAudio;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.media.AudioDeviceAttributes;
import android.media.AudioSystem;
import android.media.BluetoothProfileConnectionInfo;
import android.os.Binder;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.audio.AudioDeviceBroker;
import com.android.server.audio.AudioEventLogger;
import com.android.server.audio.AudioServiceEvents;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
/* loaded from: classes.dex */
public class BtHelper {
    private static final int BT_HEARING_AID_GAIN_MIN = -128;
    private static final int BT_LE_AUDIO_MAX_VOL = 255;
    private static final int BT_LE_AUDIO_MIN_VOL = 0;
    static final int EVENT_ACTIVE_DEVICE_CHANGE = 1;
    static final int EVENT_DEVICE_CONFIG_CHANGE = 0;
    private static final int SCO_MODE_MAX = 2;
    static final int SCO_MODE_UNDEFINED = -1;
    static final int SCO_MODE_VIRTUAL_CALL = 0;
    private static final int SCO_MODE_VR = 2;
    private static final int SCO_STATE_ACTIVATE_REQ = 1;
    private static final int SCO_STATE_ACTIVE_EXTERNAL = 2;
    private static final int SCO_STATE_ACTIVE_INTERNAL = 3;
    private static final int SCO_STATE_DEACTIVATE_REQ = 4;
    private static final int SCO_STATE_DEACTIVATING = 5;
    private static final int SCO_STATE_INACTIVE = 0;
    private static final String TAG = "AS.BtHelper";
    private BluetoothA2dp mA2dp;
    private BluetoothHeadset mBluetoothHeadset;
    private BluetoothDevice mBluetoothHeadsetDevice;
    private final AudioDeviceBroker mDeviceBroker;
    private BluetoothHearingAid mHearingAid;
    private BluetoothLeAudio mLeAudio;
    private int mScoAudioMode;
    private int mScoAudioState;
    private int mScoConnectionState;
    private boolean mAvrcpAbsVolSupported = false;
    private BluetoothProfile.ServiceListener mBluetoothProfileServiceListener = new BluetoothProfile.ServiceListener() { // from class: com.android.server.audio.BtHelper.1
        @Override // android.bluetooth.BluetoothProfile.ServiceListener
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            switch (profile) {
                case 1:
                case 2:
                case 11:
                case 21:
                case 22:
                    AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent("BT profile service: connecting " + BluetoothProfile.getProfileName(profile) + " profile").printLog(BtHelper.TAG));
                    BtHelper.this.mDeviceBroker.postBtProfileConnected(profile, proxy);
                    return;
                default:
                    return;
            }
        }

        @Override // android.bluetooth.BluetoothProfile.ServiceListener
        public void onServiceDisconnected(int profile) {
            switch (profile) {
                case 1:
                case 2:
                case 11:
                case 21:
                case 22:
                case 26:
                    AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent("BT profile service: disconnecting " + BluetoothProfile.getProfileName(profile) + " profile").printLog(BtHelper.TAG));
                    BtHelper.this.mDeviceBroker.postBtProfileDisconnected(profile);
                    return;
                default:
                    return;
            }
        }
    };

    /* JADX INFO: Access modifiers changed from: package-private */
    public BtHelper(AudioDeviceBroker broker) {
        this.mDeviceBroker = broker;
    }

    public static String scoAudioModeToString(int scoAudioMode) {
        switch (scoAudioMode) {
            case -1:
                return "SCO_MODE_UNDEFINED";
            case 0:
                return "SCO_MODE_VIRTUAL_CALL";
            case 1:
            default:
                return "SCO_MODE_(" + scoAudioMode + ")";
            case 2:
                return "SCO_MODE_VR";
        }
    }

    public static String scoAudioStateToString(int scoAudioState) {
        switch (scoAudioState) {
            case 0:
                return "SCO_STATE_INACTIVE";
            case 1:
                return "SCO_STATE_ACTIVATE_REQ";
            case 2:
                return "SCO_STATE_ACTIVE_EXTERNAL";
            case 3:
                return "SCO_STATE_ACTIVE_INTERNAL";
            case 4:
            default:
                return "SCO_STATE_(" + scoAudioState + ")";
            case 5:
                return "SCO_STATE_DEACTIVATING";
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class BluetoothA2dpDeviceInfo {
        private final BluetoothDevice mBtDevice;
        private final int mCodec;
        private final int mVolume;

        BluetoothA2dpDeviceInfo(BluetoothDevice btDevice) {
            this(btDevice, -1, 0);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public BluetoothA2dpDeviceInfo(BluetoothDevice btDevice, int volume, int codec) {
            this.mBtDevice = btDevice;
            this.mVolume = volume;
            this.mCodec = codec;
        }

        public BluetoothDevice getBtDevice() {
            return this.mBtDevice;
        }

        public int getVolume() {
            return this.mVolume;
        }

        public int getCodec() {
            return this.mCodec;
        }

        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (this == o) {
                return true;
            }
            if (!(o instanceof BluetoothA2dpDeviceInfo)) {
                return false;
            }
            return this.mBtDevice.equals(((BluetoothA2dpDeviceInfo) o).getBtDevice());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String a2dpDeviceEventToString(int event) {
        switch (event) {
            case 0:
                return "DEVICE_CONFIG_CHANGE";
            case 1:
                return "ACTIVE_DEVICE_CHANGE";
            default:
                return new String("invalid event:" + event);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String getName(BluetoothDevice device) {
        String deviceName = device.getName();
        if (deviceName == null) {
            return "";
        }
        return deviceName;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void onSystemReady() {
        this.mScoConnectionState = -1;
        resetBluetoothSco();
        getBluetoothHeadset();
        Intent newIntent = new Intent("android.media.SCO_AUDIO_STATE_CHANGED");
        newIntent.putExtra("android.media.extra.SCO_AUDIO_STATE", 0);
        sendStickyBroadcastToAll(newIntent);
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            adapter.getProfileProxy(this.mDeviceBroker.getContext(), this.mBluetoothProfileServiceListener, 2);
            adapter.getProfileProxy(this.mDeviceBroker.getContext(), this.mBluetoothProfileServiceListener, 21);
            adapter.getProfileProxy(this.mDeviceBroker.getContext(), this.mBluetoothProfileServiceListener, 22);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void onAudioServerDiedRestoreA2dp() {
        int forMed = this.mDeviceBroker.getBluetoothA2dpEnabled() ? 0 : 10;
        this.mDeviceBroker.setForceUse_Async(1, forMed, "onAudioServerDied()");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized boolean isAvrcpAbsoluteVolumeSupported() {
        boolean z;
        if (this.mA2dp != null) {
            z = this.mAvrcpAbsVolSupported;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void setAvrcpAbsoluteVolumeSupported(boolean supported) {
        this.mAvrcpAbsVolSupported = supported;
        Log.i(TAG, "setAvrcpAbsoluteVolumeSupported supported=" + supported);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void setAvrcpAbsoluteVolumeIndex(int index) {
        if (this.mA2dp == null && AudioService.DEBUG_VOL) {
            AudioService.sVolumeLogger.log(new AudioEventLogger.StringEvent("setAvrcpAbsoluteVolumeIndex: bailing due to null mA2dp").printLog(TAG));
        } else if (!this.mAvrcpAbsVolSupported) {
            AudioService.sVolumeLogger.log(new AudioEventLogger.StringEvent("setAvrcpAbsoluteVolumeIndex: abs vol not supported ").printLog(TAG));
        } else {
            if (AudioService.DEBUG_VOL) {
                Log.i(TAG, "setAvrcpAbsoluteVolumeIndex index=" + index);
            }
            AudioService.sVolumeLogger.log(new AudioServiceEvents.VolumeEvent(4, index));
            this.mA2dp.setAvrcpAbsoluteVolume(index);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isNextBtActiveDeviceAvailableForMusic() {
        boolean status = false;
        BluetoothA2dp bluetoothA2dp = this.mA2dp;
        if (bluetoothA2dp != null) {
            status = bluetoothA2dp.isNextAutoSwtichActiveDevice();
            if (AudioService.DEBUG_DEVICES) {
                Log.i(TAG, "A2DP isNextAutoSwtichActiveDevice()=" + status);
            }
            if (status) {
                return status;
            }
        }
        BluetoothLeAudio bluetoothLeAudio = this.mLeAudio;
        if (bluetoothLeAudio != null) {
            status = bluetoothLeAudio.isNextAutoSwtichActiveDevice();
            if (AudioService.DEBUG_DEVICES) {
                Log.i(TAG, "mLeAudio isNextAutoSwtichActiveDevice()=" + status);
            }
        }
        return status;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized int getA2dpCodec(BluetoothDevice device) {
        BluetoothA2dp bluetoothA2dp = this.mA2dp;
        if (bluetoothA2dp == null) {
            return 0;
        }
        BluetoothCodecStatus btCodecStatus = bluetoothA2dp.getCodecStatus(device);
        if (btCodecStatus == null) {
            return 0;
        }
        BluetoothCodecConfig btCodecConfig = btCodecStatus.getCodecConfig();
        if (btCodecConfig == null) {
            return 0;
        }
        return AudioSystem.bluetoothCodecToAudioFormat(btCodecConfig.getCodecType());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void receiveBtEvent(Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "receiveBtEvent action: " + action + " mScoAudioState: " + this.mScoAudioState);
        if (action.equals("android.bluetooth.headset.profile.action.ACTIVE_DEVICE_CHANGED")) {
            String broadcastType = intent.getStringExtra("android.bluetooth.device.extra.NAME");
            if (broadcastType != null && "fake_hfp_broadcast".equals(broadcastType)) {
                if (AudioService.DEBUG_DEVICES) {
                    Log.d(TAG, "Fake HFP active device broadcast,return");
                }
                return;
            }
            BluetoothDevice btDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            if (AudioService.DEBUG_DEVICES) {
                Log.d(TAG, "receiveBtEvent() BTactiveDeviceChanged, btDevice=" + btDevice);
            }
            setBtScoActiveDevice(btDevice);
        } else if (action.equals("android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED")) {
            int btState = intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1);
            String btStateInfo = btState == 12 ? "AudioConnected" : "AudioDisconnected";
            Log.i(TAG, "receiveBtEvent ACTION_AUDIO_STATE_CHANGED: btState=" + btState + "{" + btStateInfo + "}");
            this.mDeviceBroker.postScoAudioStateChanged(btState);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onScoAudioStateChanged(int state) {
        BluetoothHeadset bluetoothHeadset;
        BluetoothDevice bluetoothDevice;
        boolean broadcast = false;
        int scoAudioState = -1;
        switch (state) {
            case 10:
                this.mDeviceBroker.setBluetoothScoOn(false, "BtHelper.receiveBtEvent");
                scoAudioState = 0;
                if (this.mScoAudioState == 1 && (bluetoothHeadset = this.mBluetoothHeadset) != null && (bluetoothDevice = this.mBluetoothHeadsetDevice) != null && connectBluetoothScoAudioHelper(bluetoothHeadset, bluetoothDevice, this.mScoAudioMode)) {
                    this.mScoAudioState = 3;
                    scoAudioState = 2;
                    broadcast = true;
                    break;
                } else {
                    if (this.mScoAudioState != 2) {
                        broadcast = true;
                    }
                    this.mScoAudioState = 0;
                    break;
                }
                break;
            case 11:
                int i = this.mScoAudioState;
                if (i != 3 && i != 4) {
                    this.mScoAudioState = 2;
                    break;
                }
                break;
            case 12:
                scoAudioState = 1;
                int i2 = this.mScoAudioState;
                if (i2 != 3 && i2 != 4) {
                    this.mScoAudioState = 2;
                } else if (this.mDeviceBroker.isBluetoothScoRequested()) {
                    broadcast = true;
                }
                this.mDeviceBroker.setBluetoothScoOn(true, "BtHelper.receiveBtEvent");
                break;
        }
        if (broadcast) {
            broadcastScoConnectionState(scoAudioState);
            if (AudioService.DEBUG_DEVICES) {
                Log.d(TAG, "receiveBtEvent(): BR SCOAudioStateChanged, scoAudioState=" + scoAudioState);
            }
            Intent newIntent = new Intent("android.media.SCO_AUDIO_STATE_CHANGED");
            newIntent.putExtra("android.media.extra.SCO_AUDIO_STATE", scoAudioState);
            sendStickyBroadcastToAll(newIntent);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized boolean isBluetoothScoOn() {
        BluetoothDevice bluetoothDevice;
        BluetoothHeadset bluetoothHeadset = this.mBluetoothHeadset;
        if (bluetoothHeadset != null && (bluetoothDevice = this.mBluetoothHeadsetDevice) != null) {
            return bluetoothHeadset.getAudioState(bluetoothDevice) == 12;
        }
        return false;
    }

    synchronized boolean isInbandRingingEnabled() {
        boolean status;
        status = false;
        BluetoothHeadset bluetoothHeadset = this.mBluetoothHeadset;
        if (bluetoothHeadset != null && this.mBluetoothHeadsetDevice != null) {
            status = bluetoothHeadset.isInbandRingingEnabled();
        }
        if (AudioService.DEBUG_DEVICES) {
            Log.d(TAG, "isInbandRingingEnabled() = " + status);
        }
        return status;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized boolean startBluetoothSco(int scoAudioMode, String eventSource) {
        AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent(eventSource));
        return requestScoState(12, scoAudioMode);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized boolean stopBluetoothSco(String eventSource) {
        AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent(eventSource));
        return requestScoState(10, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void setLeAudioVolume(int index, int maxIndex, int streamType) {
        if (this.mLeAudio == null) {
            if (AudioService.DEBUG_VOL) {
                Log.i(TAG, "setLeAudioVolume: null mLeAudio");
            }
            return;
        }
        int volume = (index * 255) / maxIndex;
        if (AudioService.DEBUG_VOL) {
            Log.i(TAG, "setLeAudioVolume: calling mLeAudio.setVolume idx=" + index + " volume=" + volume);
        }
        AudioService.sVolumeLogger.log(new AudioServiceEvents.VolumeEvent(10, index, maxIndex));
        this.mLeAudio.setVolume(volume);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void setHearingAidVolume(int index, int streamType) {
        if (this.mHearingAid == null) {
            if (AudioService.DEBUG_VOL) {
                Log.i(TAG, "setHearingAidVolume: null mHearingAid");
            }
            return;
        }
        int gainDB = (int) AudioSystem.getStreamVolumeDB(streamType, index / 10, 134217728);
        if (gainDB < -128) {
            gainDB = -128;
        }
        if (AudioService.DEBUG_VOL) {
            Log.i(TAG, "setHearingAidVolume: calling mHearingAid.setVolume idx=" + index + " gain=" + gainDB);
        }
        AudioService.sVolumeLogger.log(new AudioServiceEvents.VolumeEvent(3, index, gainDB));
        this.mHearingAid.setVolume(gainDB);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void onBroadcastScoConnectionState(int state) {
        if (state == this.mScoConnectionState) {
            return;
        }
        Intent newIntent = new Intent("android.media.ACTION_SCO_AUDIO_STATE_UPDATED");
        newIntent.putExtra("android.media.extra.SCO_AUDIO_STATE", state);
        newIntent.putExtra("android.media.extra.SCO_AUDIO_PREVIOUS_STATE", this.mScoConnectionState);
        sendStickyBroadcastToAll(newIntent);
        this.mScoConnectionState = state;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void disconnectAllBluetoothProfiles() {
        if (AudioService.DEBUG_DEVICES) {
            Log.d(TAG, "disconnectAllBluetoothProfiles()");
        }
        this.mDeviceBroker.postBtProfileDisconnected(2);
        this.mDeviceBroker.postBtProfileDisconnected(11);
        this.mDeviceBroker.postBtProfileDisconnected(1);
        this.mDeviceBroker.postBtProfileDisconnected(21);
        this.mDeviceBroker.postBtProfileDisconnected(22);
        this.mDeviceBroker.postBtProfileDisconnected(26);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void resetBluetoothSco() {
        this.mScoAudioState = 0;
        broadcastScoConnectionState(0);
        AudioSystem.setParameters("BTAudiosuspend=false");
        this.mDeviceBroker.setBluetoothScoOn(false, "resetBluetoothSco");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void disconnectHeadset() {
        setBtScoActiveDevice(null);
        this.mBluetoothHeadset = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void onBtProfileConnected(int profile, BluetoothProfile proxy) {
        if (profile == 1) {
            onHeadsetProfileConnected((BluetoothHeadset) proxy);
            return;
        }
        if (profile == 2) {
            this.mA2dp = (BluetoothA2dp) proxy;
        } else if (profile == 21) {
            this.mHearingAid = (BluetoothHearingAid) proxy;
        } else if (profile == 22) {
            this.mLeAudio = (BluetoothLeAudio) proxy;
        }
        List<BluetoothDevice> deviceList = proxy.getConnectedDevices();
        if (deviceList.isEmpty()) {
            return;
        }
        BluetoothDevice btDevice = deviceList.get(0);
        if (proxy.getConnectionState(btDevice) == 2) {
            this.mDeviceBroker.queueOnBluetoothActiveDeviceChanged(new AudioDeviceBroker.BtDeviceChangedData(btDevice, null, new BluetoothProfileConnectionInfo(profile), "mBluetoothProfileServiceListener"));
        } else {
            this.mDeviceBroker.queueOnBluetoothActiveDeviceChanged(new AudioDeviceBroker.BtDeviceChangedData(null, btDevice, new BluetoothProfileConnectionInfo(profile), "mBluetoothProfileServiceListener"));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void onHeadsetProfileConnected(BluetoothHeadset headset) {
        BluetoothDevice bluetoothDevice;
        this.mDeviceBroker.handleCancelFailureToConnectToBtHeadsetService();
        this.mBluetoothHeadset = headset;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        List<BluetoothDevice> activeDevices = Collections.emptyList();
        if (adapter != null) {
            activeDevices = adapter.getActiveDevices(1);
        }
        setBtScoActiveDevice(activeDevices.size() > 0 ? activeDevices.get(0) : null);
        checkScoAudioState();
        int i = this.mScoAudioState;
        if (i == 1 || i == 4) {
            boolean status = false;
            BluetoothHeadset bluetoothHeadset = this.mBluetoothHeadset;
            if (bluetoothHeadset != null && (bluetoothDevice = this.mBluetoothHeadsetDevice) != null) {
                switch (i) {
                    case 1:
                        status = connectBluetoothScoAudioHelper(bluetoothHeadset, bluetoothDevice, this.mScoAudioMode);
                        if (status) {
                            this.mScoAudioState = 3;
                            break;
                        }
                        break;
                    case 4:
                        status = disconnectBluetoothScoAudioHelper(bluetoothHeadset, bluetoothDevice, this.mScoAudioMode);
                        if (status) {
                            this.mScoAudioState = 5;
                            break;
                        }
                        break;
                }
            }
            if (!status) {
                this.mScoAudioState = 0;
                broadcastScoConnectionState(0);
            }
        }
    }

    private void broadcastScoConnectionState(int state) {
        this.mDeviceBroker.postBroadcastScoConnectionState(state);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AudioDeviceAttributes getHeadsetAudioDevice() {
        BluetoothDevice bluetoothDevice = this.mBluetoothHeadsetDevice;
        if (bluetoothDevice == null) {
            return null;
        }
        return btHeadsetDeviceToAudioDevice(bluetoothDevice);
    }

    private AudioDeviceAttributes btHeadsetDeviceToAudioDevice(BluetoothDevice btDevice) {
        if (btDevice == null) {
            return new AudioDeviceAttributes(16, "");
        }
        String address = btDevice.getAddress();
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            address = "";
        }
        BluetoothClass btClass = btDevice.getBluetoothClass();
        int nativeType = 16;
        if (btClass != null) {
            switch (btClass.getDeviceClass()) {
                case UsbTerminalTypes.TERMINAL_BIDIR_SKRPHONE_SUPRESS /* 1028 */:
                case 1032:
                    nativeType = 32;
                    break;
                case 1056:
                    nativeType = 64;
                    break;
            }
        }
        if (AudioService.DEBUG_DEVICES) {
            Log.i(TAG, "btHeadsetDeviceToAudioDevice btDevice: " + btDevice + " btClass: " + (btClass == null ? "Unknown" : btClass) + " nativeType: " + nativeType + " address: " + address);
        }
        return new AudioDeviceAttributes(nativeType, address);
    }

    private boolean handleBtScoActiveDeviceChange(BluetoothDevice btDevice, boolean isActive) {
        if (btDevice == null) {
            return true;
        }
        AudioDeviceAttributes audioDevice = btHeadsetDeviceToAudioDevice(btDevice);
        String btDeviceName = getName(btDevice);
        boolean result = false;
        if (isActive) {
            result = false | this.mDeviceBroker.handleDeviceConnection(new AudioDeviceAttributes(audioDevice.getInternalType(), audioDevice.getAddress(), btDeviceName), isActive);
        } else {
            int[] outDeviceTypes = {16, 32, 64};
            for (int outDeviceType : outDeviceTypes) {
                result |= this.mDeviceBroker.handleDeviceConnection(new AudioDeviceAttributes(outDeviceType, audioDevice.getAddress(), btDeviceName), isActive);
            }
        }
        if (this.mDeviceBroker.handleDeviceConnection(new AudioDeviceAttributes(-2147483640, audioDevice.getAddress(), btDeviceName), isActive) && result) {
            return true;
        }
        return false;
    }

    private String getAnonymizedAddress(BluetoothDevice btDevice) {
        return btDevice == null ? "(null)" : btDevice.getAnonymizedAddress();
    }

    private void setBtScoActiveDevice(BluetoothDevice btDevice) {
        Log.i(TAG, "setBtScoActiveDevice: " + getAnonymizedAddress(this.mBluetoothHeadsetDevice) + " -> " + getAnonymizedAddress(btDevice));
        BluetoothDevice previousActiveDevice = this.mBluetoothHeadsetDevice;
        if (Objects.equals(btDevice, previousActiveDevice)) {
            return;
        }
        if (!Objects.equals(btDevice, previousActiveDevice)) {
            this.mDeviceBroker.resetBluetoothScoOfApp();
        }
        if (!handleBtScoActiveDeviceChange(previousActiveDevice, false)) {
            Log.w(TAG, "setBtScoActiveDevice() failed to remove previous device " + getAnonymizedAddress(previousActiveDevice));
        }
        if (!handleBtScoActiveDeviceChange(btDevice, true)) {
            Log.e(TAG, "setBtScoActiveDevice() failed to add new device " + getAnonymizedAddress(btDevice));
            btDevice = null;
        }
        this.mBluetoothHeadsetDevice = btDevice;
        if (btDevice == null) {
            this.mDeviceBroker.resetBluetoothScoOfApp();
            resetBluetoothSco();
            return;
        }
        this.mDeviceBroker.restartScoInVoipCall();
    }

    private boolean requestScoState(int state, int scoAudioMode) {
        checkScoAudioState();
        if (state == 12) {
            broadcastScoConnectionState(2);
            switch (this.mScoAudioState) {
                case 0:
                    this.mScoAudioMode = scoAudioMode;
                    if (scoAudioMode == -1) {
                        this.mScoAudioMode = 0;
                        if (this.mBluetoothHeadsetDevice != null) {
                            int i = Settings.Global.getInt(this.mDeviceBroker.getContentResolver(), "bluetooth_sco_channel_" + this.mBluetoothHeadsetDevice.getAddress(), 0);
                            this.mScoAudioMode = i;
                            if (i > 2 || i < 0) {
                                this.mScoAudioMode = 0;
                            }
                        }
                    }
                    BluetoothHeadset bluetoothHeadset = this.mBluetoothHeadset;
                    if (bluetoothHeadset == null) {
                        if (getBluetoothHeadset()) {
                            this.mScoAudioState = 1;
                            break;
                        } else {
                            Log.w(TAG, "requestScoState: getBluetoothHeadset failed during connection, mScoAudioMode=" + this.mScoAudioMode);
                            broadcastScoConnectionState(0);
                            return false;
                        }
                    } else {
                        BluetoothDevice bluetoothDevice = this.mBluetoothHeadsetDevice;
                        if (bluetoothDevice == null) {
                            Log.w(TAG, "requestScoState: no active device while connecting, mScoAudioMode=" + this.mScoAudioMode);
                            broadcastScoConnectionState(0);
                            return false;
                        } else if (connectBluetoothScoAudioHelper(bluetoothHeadset, bluetoothDevice, this.mScoAudioMode)) {
                            this.mScoAudioState = 3;
                            break;
                        } else {
                            Log.w(TAG, "requestScoState: connect to " + getAnonymizedAddress(this.mBluetoothHeadsetDevice) + " failed, mScoAudioMode=" + this.mScoAudioMode);
                            broadcastScoConnectionState(0);
                            return false;
                        }
                    }
                case 1:
                default:
                    Log.w(TAG, "requestScoState: failed to connect in state " + this.mScoAudioState + ", scoAudioMode=" + scoAudioMode);
                    broadcastScoConnectionState(0);
                    return false;
                case 2:
                    broadcastScoConnectionState(1);
                    break;
                case 3:
                    Log.w(TAG, "requestScoState: already in ACTIVE mode, simply return");
                    break;
                case 4:
                    this.mScoAudioState = 3;
                    broadcastScoConnectionState(1);
                    break;
                case 5:
                    this.mScoAudioState = 1;
                    break;
            }
        } else if (state == 10) {
            switch (this.mScoAudioState) {
                case 1:
                    this.mScoAudioState = 0;
                    broadcastScoConnectionState(0);
                    break;
                case 2:
                default:
                    Log.w(TAG, "requestScoState: failed to disconnect in state " + this.mScoAudioState + ", scoAudioMode=" + scoAudioMode);
                    broadcastScoConnectionState(0);
                    return false;
                case 3:
                    BluetoothHeadset bluetoothHeadset2 = this.mBluetoothHeadset;
                    if (bluetoothHeadset2 == null) {
                        if (getBluetoothHeadset()) {
                            this.mScoAudioState = 4;
                            break;
                        } else {
                            Log.w(TAG, "requestScoState: getBluetoothHeadset failed during disconnection, mScoAudioMode=" + this.mScoAudioMode);
                            this.mScoAudioState = 0;
                            broadcastScoConnectionState(0);
                            return false;
                        }
                    } else {
                        BluetoothDevice bluetoothDevice2 = this.mBluetoothHeadsetDevice;
                        if (bluetoothDevice2 == null) {
                            this.mScoAudioState = 0;
                            broadcastScoConnectionState(0);
                            break;
                        } else if (disconnectBluetoothScoAudioHelper(bluetoothHeadset2, bluetoothDevice2, this.mScoAudioMode)) {
                            this.mScoAudioState = 5;
                            break;
                        } else {
                            this.mScoAudioState = 0;
                            broadcastScoConnectionState(0);
                            break;
                        }
                    }
            }
        }
        return true;
    }

    private void sendStickyBroadcastToAll(Intent intent) {
        intent.addFlags(268435456);
        long ident = Binder.clearCallingIdentity();
        try {
            this.mDeviceBroker.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private static boolean disconnectBluetoothScoAudioHelper(BluetoothHeadset bluetoothHeadset, BluetoothDevice device, int scoAudioMode) {
        switch (scoAudioMode) {
            case 0:
                return bluetoothHeadset.stopScoUsingVirtualVoiceCall();
            case 1:
            default:
                return false;
            case 2:
                return bluetoothHeadset.stopVoiceRecognition(device);
        }
    }

    private static boolean connectBluetoothScoAudioHelper(BluetoothHeadset bluetoothHeadset, BluetoothDevice device, int scoAudioMode) {
        switch (scoAudioMode) {
            case 0:
                return bluetoothHeadset.startScoUsingVirtualVoiceCall();
            case 1:
            default:
                return false;
            case 2:
                return bluetoothHeadset.startVoiceRecognition(device);
        }
    }

    private void checkScoAudioState() {
        BluetoothDevice bluetoothDevice;
        BluetoothHeadset bluetoothHeadset = this.mBluetoothHeadset;
        if (bluetoothHeadset != null && (bluetoothDevice = this.mBluetoothHeadsetDevice) != null && this.mScoAudioState == 0 && bluetoothHeadset.getAudioState(bluetoothDevice) != 10) {
            this.mScoAudioState = 2;
        }
    }

    private boolean getBluetoothHeadset() {
        boolean result = false;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            result = adapter.getProfileProxy(this.mDeviceBroker.getContext(), this.mBluetoothProfileServiceListener, 1);
        }
        this.mDeviceBroker.handleFailureToConnectToBtHeadsetService(result ? 3000 : 0);
        return result;
    }

    public static String bluetoothCodecToEncodingString(int btCodecType) {
        switch (btCodecType) {
            case 0:
                return "ENCODING_SBC";
            case 1:
                return "ENCODING_AAC";
            case 2:
                return "ENCODING_APTX";
            case 3:
                return "ENCODING_APTX_HD";
            case 4:
                return "ENCODING_LDAC";
            default:
                return "ENCODING_BT_CODEC_TYPE(" + btCodecType + ")";
        }
    }

    public static String btDeviceClassToString(int btDeviceClass) {
        switch (btDeviceClass) {
            case 1024:
                return "AUDIO_VIDEO_UNCATEGORIZED";
            case UsbTerminalTypes.TERMINAL_BIDIR_SKRPHONE_SUPRESS /* 1028 */:
                return "AUDIO_VIDEO_WEARABLE_HEADSET";
            case 1032:
                return "AUDIO_VIDEO_HANDSFREE";
            case 1036:
                return "AUDIO_VIDEO_RESERVED_0x040C";
            case 1040:
                return "AUDIO_VIDEO_MICROPHONE";
            case 1044:
                return "AUDIO_VIDEO_LOUDSPEAKER";
            case 1048:
                return "AUDIO_VIDEO_HEADPHONES";
            case 1052:
                return "AUDIO_VIDEO_PORTABLE_AUDIO";
            case 1056:
                return "AUDIO_VIDEO_CAR_AUDIO";
            case 1060:
                return "AUDIO_VIDEO_SET_TOP_BOX";
            case 1064:
                return "AUDIO_VIDEO_HIFI_AUDIO";
            case 1068:
                return "AUDIO_VIDEO_VCR";
            case 1072:
                return "AUDIO_VIDEO_VIDEO_CAMERA";
            case 1076:
                return "AUDIO_VIDEO_CAMCORDER";
            case 1080:
                return "AUDIO_VIDEO_VIDEO_MONITOR";
            case 1084:
                return "AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER";
            case 1088:
                return "AUDIO_VIDEO_VIDEO_CONFERENCING";
            case 1092:
                return "AUDIO_VIDEO_RESERVED_0x0444";
            case 1096:
                return "AUDIO_VIDEO_VIDEO_GAMING_TOY";
            default:
                return TextUtils.formatSimple("0x%04x", new Object[]{Integer.valueOf(btDeviceClass)});
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        BluetoothClass bluetoothClass;
        pw.println("\n" + prefix + "mBluetoothHeadset: " + this.mBluetoothHeadset);
        pw.println(prefix + "mBluetoothHeadsetDevice: " + this.mBluetoothHeadsetDevice);
        BluetoothDevice bluetoothDevice = this.mBluetoothHeadsetDevice;
        if (bluetoothDevice != null && (bluetoothClass = bluetoothDevice.getBluetoothClass()) != null) {
            pw.println(prefix + "mBluetoothHeadsetDevice.DeviceClass: " + btDeviceClassToString(bluetoothClass.getDeviceClass()));
        }
        pw.println(prefix + "mScoAudioState: " + scoAudioStateToString(this.mScoAudioState));
        pw.println(prefix + "mScoAudioMode: " + scoAudioModeToString(this.mScoAudioMode));
        pw.println("\n" + prefix + "mHearingAid: " + this.mHearingAid);
        pw.println("\n" + prefix + "mLeAudio: " + this.mLeAudio);
        pw.println(prefix + "mA2dp: " + this.mA2dp);
        pw.println(prefix + "mAvrcpAbsVolSupported: " + this.mAvrcpAbsVolSupported);
    }
}
