package com.android.server.audio;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.hardware.audio.common.V2_0.AudioDevice;
import android.media.AudioAttributes;
import android.media.AudioDeviceAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioRoutesInfo;
import android.media.AudioSystem;
import android.media.BluetoothProfileConnectionInfo;
import android.media.IAudioRoutesObserver;
import android.media.ICapturePresetDevicesRoleDispatcher;
import android.media.ICommunicationDeviceDispatcher;
import android.media.IStrategyPreferredDevicesDispatcher;
import android.media.MediaMetrics;
import android.media.audiopolicy.AudioProductStrategy;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.PrintWriterPrinter;
import com.android.server.audio.AudioDeviceBroker;
import com.android.server.audio.AudioDeviceInventory;
import com.android.server.audio.AudioEventLogger;
import com.android.server.audio.AudioServiceEvents;
import com.android.server.audio.BtHelper;
import com.android.server.slice.SliceClientPermissions;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class AudioDeviceBroker {
    private static final long BROKER_WAKELOCK_TIMEOUT_MS = 5000;
    static final int BTA2DP_DOCK_TIMEOUT_MS = 8000;
    private static final int BTA2DP_MUTE_CHECK_DELAY_MS = 100;
    static final int BT_HEADSET_CNCT_TIMEOUT_MS = 3000;
    private static final Set<Integer> MESSAGES_MUTE_MUSIC;
    private static final int MSG_BROADCAST_AUDIO_BECOMING_NOISY = 12;
    private static final int MSG_BT_HEADSET_CNCT_FAILED = 9;
    private static final int MSG_CHECK_MUTE_MUSIC = 35;
    private static final int MSG_IIL_SET_FORCE_BT_A2DP_USE = 5;
    private static final int MSG_IIL_SET_FORCE_USE = 4;
    private static final int MSG_II_SET_HEARING_AID_VOLUME = 14;
    private static final int MSG_II_SET_LE_AUDIO_OUT_VOLUME = 46;
    private static final int MSG_IL_BTA2DP_TIMEOUT = 10;
    private static final int MSG_IL_BT_SERVICE_CONNECTED_PROFILE = 23;
    private static final int MSG_IL_LEAUDIO_TIMEOUT = 48;
    private static final int MSG_IL_SAVE_PREF_DEVICES_FOR_CAPTURE_PRESET = 37;
    private static final int MSG_IL_SAVE_PREF_DEVICES_FOR_STRATEGY = 32;
    private static final int MSG_I_BROADCAST_BT_CONNECTION_STATE = 3;
    private static final int MSG_I_BT_SERVICE_DISCONNECTED_PROFILE = 22;
    private static final int MSG_I_SAVE_CLEAR_PREF_DEVICES_FOR_CAPTURE_PRESET = 38;
    private static final int MSG_I_SAVE_REMOVE_PREF_DEVICES_FOR_STRATEGY = 33;
    private static final int MSG_I_SCO_AUDIO_STATE_CHANGED = 44;
    private static final int MSG_I_SET_AVRCP_ABSOLUTE_VOLUME = 15;
    private static final int MSG_I_SET_MODE_OWNER_PID = 16;
    private static final int MSG_I_SET_PRE_MODE_OWNER_PID = 47;
    private static final int MSG_L_A2DP_DEVICE_CONFIG_CHANGE = 11;
    private static final int MSG_L_A2DP_DEVICE_CONNECTION_CHANGE_EXT = 29;
    private static final int MSG_L_BT_ACTIVE_DEVICE_CHANGE_EXT = 45;
    private static final int MSG_L_COMMUNICATION_ROUTE_CLIENT_DIED = 34;
    private static final int MSG_L_HEARING_AID_DEVICE_CONNECTION_CHANGE_EXT = 31;
    private static final int MSG_L_SET_BT_ACTIVE_DEVICE = 7;
    private static final int MSG_L_SET_COMMUNICATION_ROUTE_FOR_CLIENT = 42;
    private static final int MSG_L_SET_WIRED_DEVICE_CONNECTION_STATE = 2;
    private static final int MSG_L_UPDATE_COMMUNICATION_ROUTE = 39;
    private static final int MSG_L_UPDATE_COMMUNICATION_ROUTE_CLIENT = 43;
    private static final int MSG_REPORT_NEW_ROUTES = 13;
    private static final int MSG_REPORT_NEW_ROUTES_A2DP = 36;
    private static final int MSG_RESTORE_DEVICES = 1;
    private static final int MSG_TOGGLE_HDMI = 6;
    private static final int SENDMSG_NOOP = 1;
    private static final int SENDMSG_QUEUE = 2;
    private static final int SENDMSG_REPLACE = 0;
    private static final String TAG = "AS.AudioDeviceBroker";
    private int mAccessibilityStrategyId;
    private AudioDeviceInfo mActiveCommunicationDevice;
    private final AudioService mAudioService;
    private boolean mBluetoothA2dpEnabled;
    private boolean mBluetoothScoOn;
    private PowerManager.WakeLock mBrokerEventWakeLock;
    private BrokerHandler mBrokerHandler;
    private BrokerThread mBrokerThread;
    private final BtHelper mBtHelper;
    final RemoteCallbackList<ICommunicationDeviceDispatcher> mCommDevDispatchers;
    private final LinkedList<CommunicationRouteClient> mCommunicationRouteClients;
    private int mCommunicationStrategyId;
    private final Context mContext;
    int mCurCommunicationPortId;
    private final AudioDeviceInventory mDeviceInventory;
    private final Object mDeviceStateLock;
    private int mModeOwnerPid;
    private AtomicBoolean mMusicMuted;
    private int mPreModeOwnerPid;
    private AudioDeviceAttributes mPreferredCommunicationDevice;
    final Object mSetModeLock;
    private final SystemServerAdapter mSystemServer;
    private static final Object sLastDeviceConnectionMsgTimeLock = new Object();
    private static long sLastDeviceConnectMsgTime = 0;

    static {
        HashSet hashSet = new HashSet();
        MESSAGES_MUTE_MUSIC = hashSet;
        hashSet.add(7);
        hashSet.add(11);
        hashSet.add(29);
        hashSet.add(5);
        hashSet.add(2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AudioDeviceBroker(Context context, AudioService service) {
        this.mCommunicationStrategyId = -1;
        this.mAccessibilityStrategyId = -1;
        this.mDeviceStateLock = new Object();
        this.mSetModeLock = new Object();
        this.mModeOwnerPid = 0;
        this.mPreModeOwnerPid = 0;
        this.mCommDevDispatchers = new RemoteCallbackList<>();
        this.mCurCommunicationPortId = -1;
        this.mMusicMuted = new AtomicBoolean(false);
        this.mCommunicationRouteClients = new LinkedList<>();
        this.mContext = context;
        this.mAudioService = service;
        this.mBtHelper = new BtHelper(this);
        this.mDeviceInventory = new AudioDeviceInventory(this);
        this.mSystemServer = SystemServerAdapter.getDefaultAdapter(context);
        init();
    }

    AudioDeviceBroker(Context context, AudioService service, AudioDeviceInventory mockDeviceInventory, SystemServerAdapter mockSystemServer) {
        this.mCommunicationStrategyId = -1;
        this.mAccessibilityStrategyId = -1;
        this.mDeviceStateLock = new Object();
        this.mSetModeLock = new Object();
        this.mModeOwnerPid = 0;
        this.mPreModeOwnerPid = 0;
        this.mCommDevDispatchers = new RemoteCallbackList<>();
        this.mCurCommunicationPortId = -1;
        this.mMusicMuted = new AtomicBoolean(false);
        this.mCommunicationRouteClients = new LinkedList<>();
        this.mContext = context;
        this.mAudioService = service;
        this.mBtHelper = new BtHelper(this);
        this.mDeviceInventory = mockDeviceInventory;
        this.mSystemServer = mockSystemServer;
        init();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initRoutingStrategyIds() {
        List<AudioProductStrategy> strategies = AudioProductStrategy.getAudioProductStrategies();
        this.mCommunicationStrategyId = -1;
        this.mAccessibilityStrategyId = -1;
        for (AudioProductStrategy strategy : strategies) {
            if (this.mCommunicationStrategyId == -1 && strategy.getAudioAttributesForLegacyStreamType(0) != null) {
                this.mCommunicationStrategyId = strategy.getId();
            }
            if (this.mAccessibilityStrategyId == -1 && strategy.getAudioAttributesForLegacyStreamType(10) != null) {
                this.mAccessibilityStrategyId = strategy.getId();
            }
        }
    }

    private void init() {
        setupMessaging(this.mContext);
        initRoutingStrategyIds();
        this.mPreferredCommunicationDevice = null;
        updateActiveCommunicationDevice();
        this.mSystemServer.registerUserStartedReceiver(this.mContext);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Context getContext() {
        return this.mContext;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSystemReady() {
        synchronized (this.mSetModeLock) {
            synchronized (this.mDeviceStateLock) {
                int modeOwnerPid = this.mAudioService.getModeOwnerPid();
                this.mModeOwnerPid = modeOwnerPid;
                this.mPreModeOwnerPid = modeOwnerPid;
                this.mBtHelper.onSystemReady();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAudioServerDied() {
        sendMsgNoDelay(1, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setForceUse_Async(int useCase, int config, String eventSource) {
        sendIILMsgNoDelay(4, 2, useCase, config, eventSource);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void toggleHdmiIfConnected_Async() {
        sendMsgNoDelay(6, 2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void disconnectAllBluetoothProfiles() {
        synchronized (this.mDeviceStateLock) {
            this.mBtHelper.disconnectAllBluetoothProfiles();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void receiveBtEvent(Intent intent) {
        synchronized (this.mSetModeLock) {
            synchronized (this.mDeviceStateLock) {
                this.mBtHelper.receiveBtEvent(intent);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setBluetoothA2dpOn_Async(boolean on, String source) {
        synchronized (this.mDeviceStateLock) {
            if (this.mBluetoothA2dpEnabled == on) {
                return;
            }
            this.mBluetoothA2dpEnabled = on;
            this.mBrokerHandler.removeMessages(5);
            sendIILMsgNoDelay(5, 2, 1, this.mBluetoothA2dpEnabled ? 0 : 10, source);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSpeakerphoneOn(IBinder cb, int pid, boolean on, String eventSource) {
        if (AudioService.DEBUG_COMM_RTE) {
            Log.v(TAG, "setSpeakerphoneOn, on: " + on + " pid: " + pid);
        }
        synchronized (this.mSetModeLock) {
            synchronized (this.mDeviceStateLock) {
                AudioDeviceAttributes device = null;
                if (pid != this.mModeOwnerPid) {
                    Log.w(TAG, "mModeOwnerPid " + this.mModeOwnerPid + " is different with calling pid !");
                    this.mModeOwnerPid = this.mAudioService.getModeOwnerPid();
                }
                if (on) {
                    device = new AudioDeviceAttributes(2, "");
                } else {
                    CommunicationRouteClient client = getCommunicationRouteClientForPid(pid);
                    if (client != null && !client.requestsSpeakerphone()) {
                        if (AudioService.DEBUG_COMM_RTE) {
                            Log.d(TAG, "setSpeakerphoneOn, current communication device is not speaker,return");
                        }
                        return;
                    }
                }
                postSetCommunicationRouteForClient(new CommunicationClientInfo(cb, pid, device, -1, eventSource));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setCommunicationDevice(IBinder cb, int pid, AudioDeviceInfo device, String eventSource) {
        if (AudioService.DEBUG_COMM_RTE) {
            Log.v(TAG, "setCommunicationDevice, device: " + device + ", pid: " + pid);
        }
        synchronized (this.mSetModeLock) {
            synchronized (this.mDeviceStateLock) {
                AudioDeviceAttributes deviceAttr = null;
                if (device != null) {
                    deviceAttr = new AudioDeviceAttributes(device);
                }
                postSetCommunicationRouteForClient(new CommunicationClientInfo(cb, pid, deviceAttr, -1, eventSource));
            }
        }
        return true;
    }

    void setCommunicationRouteForClient(IBinder cb, int pid, AudioDeviceAttributes device, int scoAudioMode, String eventSource) {
        CommunicationRouteClient client;
        if (AudioService.DEBUG_COMM_RTE) {
            Log.v(TAG, "setCommunicationRouteForClient: device: " + device);
        }
        AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent("setCommunicationRouteForClient for pid: " + pid + " device: " + device + " from API: " + eventSource).printLog(TAG));
        boolean wasBtScoRequested = isBluetoothScoRequested();
        AudioDeviceAttributes prevClientDevice = null;
        CommunicationRouteClient client2 = getCommunicationRouteClientForPid(pid);
        if (client2 != null) {
            prevClientDevice = client2.getDevice();
        }
        if (device != null) {
            client = addCommunicationRouteClient(cb, pid, device);
            if (client == null) {
                Log.w(TAG, "setCommunicationRouteForClient: could not add client for pid: " + pid + " and device: " + device);
            }
        } else {
            client = removeCommunicationRouteClient(cb, true);
        }
        if (client == null) {
            return;
        }
        boolean isBtScoRequested = isBluetoothScoRequested();
        if (isBtScoRequested && ((!wasBtScoRequested || !isBluetoothScoActive()) && !this.mBtHelper.isBluetoothScoOn())) {
            if (!this.mBtHelper.startBluetoothSco(scoAudioMode, eventSource)) {
                Log.w(TAG, "setCommunicationRouteForClient: failure to start BT SCO for pid: " + pid);
                if (prevClientDevice != null) {
                    addCommunicationRouteClient(cb, pid, prevClientDevice);
                } else {
                    removeCommunicationRouteClient(cb, true);
                }
                postBroadcastScoConnectionState(0);
            }
        } else if (isBtScoRequested && wasBtScoRequested && !this.mBtHelper.isBluetoothScoOn()) {
            Log.w(TAG, "setCommunicationRouteForClient: when isBtScoRequested &: wasBtScoRequestedare true and BT SCO is off" + pid);
            if (!this.mBtHelper.startBluetoothSco(scoAudioMode, eventSource)) {
                Log.w(TAG, "setCommunicationRouteForClient: failure to start BT SCO for pid: " + pid);
                if (prevClientDevice != null) {
                    addCommunicationRouteClient(cb, pid, prevClientDevice);
                } else {
                    removeCommunicationRouteClient(cb, true);
                }
                postBroadcastScoConnectionState(0);
            }
        } else if (!isBtScoRequested && wasBtScoRequested) {
            this.mBtHelper.stopBluetoothSco(eventSource);
        }
        sendLMsgNoDelay(39, 2, eventSource);
    }

    private CommunicationRouteClient topCommunicationRouteClient() {
        Iterator<CommunicationRouteClient> it = this.mCommunicationRouteClients.iterator();
        while (it.hasNext()) {
            CommunicationRouteClient crc = it.next();
            if (crc.getPid() == this.mModeOwnerPid) {
                return crc;
            }
        }
        if (!this.mCommunicationRouteClients.isEmpty() && this.mModeOwnerPid == 0) {
            return this.mCommunicationRouteClients.get(0);
        }
        return null;
    }

    private AudioDeviceAttributes requestedCommunicationDevice() {
        CommunicationRouteClient crc = topCommunicationRouteClient();
        AudioDeviceAttributes device = crc != null ? crc.getDevice() : null;
        if (AudioService.DEBUG_COMM_RTE) {
            Log.v(TAG, "requestedCommunicationDevice, device: " + device + " mode owner pid: " + this.mModeOwnerPid);
        }
        return device;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AudioDeviceInfo getCommunicationDevice() {
        AudioDeviceInfo audioDeviceInfo;
        synchronized (this.mDeviceStateLock) {
            updateActiveCommunicationDevice();
            audioDeviceInfo = this.mActiveCommunicationDevice;
        }
        return audioDeviceInfo;
    }

    void updateActiveCommunicationDevice() {
        AudioDeviceAttributes device = preferredCommunicationDevice();
        if (device == null) {
            AudioAttributes attr = AudioProductStrategy.getAudioAttributesForStrategyWithLegacyStreamType(0);
            List<AudioDeviceAttributes> devices = AudioSystem.getDevicesForAttributes(attr, false);
            if (devices.isEmpty()) {
                if (this.mAudioService.isPlatformVoice()) {
                    Log.w(TAG, "updateActiveCommunicationDevice(): no device for phone strategy");
                }
                this.mActiveCommunicationDevice = null;
                return;
            }
            device = devices.get(0);
        }
        this.mActiveCommunicationDevice = AudioManager.getDeviceInfoFromTypeAndAddress(device.getType(), device.getAddress());
    }

    private boolean isDeviceRequestedForCommunication(int deviceType) {
        boolean z;
        synchronized (this.mDeviceStateLock) {
            AudioDeviceAttributes device = requestedCommunicationDevice();
            z = device != null && device.getType() == deviceType;
        }
        return z;
    }

    private boolean isDeviceOnForCommunication(int deviceType) {
        boolean z;
        synchronized (this.mDeviceStateLock) {
            AudioDeviceAttributes device = preferredCommunicationDevice();
            z = device != null && device.getType() == deviceType;
        }
        return z;
    }

    private boolean isDeviceActiveForCommunication(int deviceType) {
        AudioDeviceAttributes audioDeviceAttributes;
        AudioDeviceInfo audioDeviceInfo = this.mActiveCommunicationDevice;
        return audioDeviceInfo != null && audioDeviceInfo.getType() == deviceType && (audioDeviceAttributes = this.mPreferredCommunicationDevice) != null && audioDeviceAttributes.getType() == deviceType;
    }

    private boolean isSpeakerphoneRequested() {
        return isDeviceRequestedForCommunication(2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSpeakerphoneOn() {
        return isDeviceOnForCommunication(2);
    }

    private boolean isSpeakerphoneActive() {
        return isDeviceActiveForCommunication(2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isBluetoothScoRequested() {
        return isDeviceRequestedForCommunication(7);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isBluetoothScoOn() {
        return isDeviceOnForCommunication(7);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isBluetoothScoActive() {
        return isDeviceActiveForCommunication(7);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDeviceConnected(AudioDeviceAttributes device) {
        boolean isDeviceConnected;
        synchronized (this.mDeviceStateLock) {
            isDeviceConnected = this.mDeviceInventory.isDeviceConnected(device);
        }
        return isDeviceConnected;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setWiredDeviceConnectionState(AudioDeviceAttributes attributes, int state, String caller) {
        synchronized (this.mDeviceStateLock) {
            boolean suppressNoisyIntent = false;
            if (state == 0) {
                synchronized (this.mDeviceStateLock) {
                    suppressNoisyIntent = this.mBtHelper.isNextBtActiveDeviceAvailableForMusic();
                }
            }
            this.mDeviceInventory.setWiredDeviceConnectionState(attributes, state, caller, suppressNoisyIntent);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTestDeviceConnectionState(AudioDeviceAttributes device, int state) {
        synchronized (this.mDeviceStateLock) {
            this.mDeviceInventory.setTestDeviceConnectionState(device, state);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void restartScoInVoipCall() {
        this.mAudioService.getAudioServiceExtInstance().restartScoInVoipCall();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class BleVolumeInfo {
        final int mIndex;
        final int mMaxIndex;
        final int mStreamType;

        BleVolumeInfo(int index, int maxIndex, int streamType) {
            this.mIndex = index;
            this.mMaxIndex = maxIndex;
            this.mStreamType = streamType;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class BtDeviceChangedData {
        final String mEventSource;
        final BluetoothProfileConnectionInfo mInfo;
        final BluetoothDevice mNewDevice;
        final BluetoothDevice mPreviousDevice;

        /* JADX INFO: Access modifiers changed from: package-private */
        public BtDeviceChangedData(BluetoothDevice newDevice, BluetoothDevice previousDevice, BluetoothProfileConnectionInfo info, String eventSource) {
            this.mNewDevice = newDevice;
            this.mPreviousDevice = previousDevice;
            this.mInfo = info;
            this.mEventSource = eventSource;
        }

        public String toString() {
            return "BtDeviceChangedData profile=" + BluetoothProfile.getProfileName(this.mInfo.getProfile()) + ", switch device: [" + this.mPreviousDevice + "] -> [" + this.mNewDevice + "]";
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class BtDeviceInfo {
        final int mAudioSystemDevice;
        final int mCodec;
        final BluetoothDevice mDevice;
        final String mEventSource;
        final boolean mIsLeOutput;
        final int mMusicDevice;
        final int mProfile;
        final int mState;
        final boolean mSupprNoisy;
        final int mVolume;

        BtDeviceInfo(BtDeviceChangedData d, BluetoothDevice device, int state, int audioDevice, int codec) {
            this.mDevice = device;
            this.mState = state;
            this.mProfile = d.mInfo.getProfile();
            this.mSupprNoisy = d.mInfo.isSuppressNoisyIntent();
            this.mVolume = d.mInfo.getVolume();
            this.mIsLeOutput = d.mInfo.isLeOutput();
            this.mEventSource = d.mEventSource;
            this.mAudioSystemDevice = audioDevice;
            this.mMusicDevice = 0;
            this.mCodec = codec;
        }

        BtDeviceInfo(BluetoothDevice device, int profile) {
            this.mDevice = device;
            this.mProfile = profile;
            this.mEventSource = "";
            this.mMusicDevice = 0;
            this.mCodec = 0;
            this.mAudioSystemDevice = 0;
            this.mState = 0;
            this.mSupprNoisy = false;
            this.mVolume = -1;
            this.mIsLeOutput = false;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public BtDeviceInfo(BluetoothDevice device, int profile, int state, int musicDevice, int audioSystemDevice) {
            this.mDevice = device;
            this.mProfile = profile;
            this.mEventSource = "";
            this.mMusicDevice = musicDevice;
            this.mCodec = 0;
            this.mAudioSystemDevice = audioSystemDevice;
            this.mState = state;
            this.mSupprNoisy = false;
            this.mVolume = -1;
            this.mIsLeOutput = false;
        }

        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (this == o) {
                return true;
            }
            if (!(o instanceof BtDeviceInfo) || this.mProfile != ((BtDeviceInfo) o).mProfile || !this.mDevice.equals(((BtDeviceInfo) o).mDevice)) {
                return false;
            }
            return true;
        }
    }

    BtDeviceInfo createBtDeviceInfo(BtDeviceChangedData d, BluetoothDevice device, int state) {
        int audioDevice;
        int codec = 0;
        switch (d.mInfo.getProfile()) {
            case 2:
                audioDevice = 128;
                synchronized (this.mDeviceStateLock) {
                    codec = this.mBtHelper.getA2dpCodec(device);
                    break;
                }
            case 11:
                audioDevice = AudioDevice.IN_BLUETOOTH_A2DP;
                break;
            case 21:
                audioDevice = 134217728;
                break;
            case 22:
                if (d.mInfo.isLeOutput()) {
                    audioDevice = 536870912;
                    break;
                } else {
                    audioDevice = -1610612736;
                    break;
                }
            case 26:
                audioDevice = 536870914;
                break;
            default:
                throw new IllegalArgumentException("Invalid profile " + d.mInfo.getProfile());
        }
        return new BtDeviceInfo(d, device, state, audioDevice, codec);
    }

    private void btMediaMetricRecord(BluetoothDevice device, String state, BtDeviceChangedData data) {
        String name = TextUtils.emptyIfNull(device.getName());
        new MediaMetrics.Item("audio.device.queueOnBluetoothActiveDeviceChanged").set(MediaMetrics.Property.STATE, state).set(MediaMetrics.Property.STATUS, Integer.valueOf(data.mInfo.getProfile())).set(MediaMetrics.Property.NAME, name).record();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void queueOnBluetoothActiveDeviceChanged(BtDeviceChangedData data) {
        if (data.mInfo.getProfile() == 2 && data.mPreviousDevice != null && data.mPreviousDevice.equals(data.mNewDevice)) {
            String name = TextUtils.emptyIfNull(data.mNewDevice.getName());
            new MediaMetrics.Item("audio.device.queueOnBluetoothActiveDeviceChanged_update").set(MediaMetrics.Property.NAME, name).set(MediaMetrics.Property.STATUS, Integer.valueOf(data.mInfo.getProfile())).record();
            synchronized (this.mDeviceStateLock) {
                postBluetoothA2dpDeviceConfigChange(data.mNewDevice);
            }
            return;
        }
        synchronized (this.mDeviceStateLock) {
            if (data.mPreviousDevice != null) {
                btMediaMetricRecord(data.mPreviousDevice, "disconnected", data);
                sendLMsgNoDelay(45, 2, createBtDeviceInfo(data, data.mPreviousDevice, 0));
            }
            if (data.mNewDevice != null) {
                btMediaMetricRecord(data.mNewDevice, "connected", data);
                sendLMsgNoDelay(45, 2, createBtDeviceInfo(data, data.mNewDevice, 2));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetBluetoothScoOfApp() {
        if (AudioService.DEBUG_COMM_RTE) {
            Log.v(TAG, "resetBluetoothScoOfApp");
        }
        this.mAudioService.resetBluetoothScoOfApp();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setBluetoothScoOn(boolean on, String eventSource) {
        if (AudioService.DEBUG_COMM_RTE) {
            Log.v(TAG, "setBluetoothScoOn: " + on + " " + eventSource);
        }
        if (!on) {
            this.mAudioService.resetBluetoothScoOfApp();
        }
        synchronized (this.mDeviceStateLock) {
            this.mBluetoothScoOn = on;
            postUpdateCommunicationRouteClient(eventSource);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AudioRoutesInfo startWatchingRoutes(IAudioRoutesObserver observer) {
        AudioRoutesInfo startWatchingRoutes;
        synchronized (this.mDeviceStateLock) {
            startWatchingRoutes = this.mDeviceInventory.startWatchingRoutes(observer);
        }
        return startWatchingRoutes;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AudioRoutesInfo getCurAudioRoutes() {
        AudioRoutesInfo curAudioRoutes;
        synchronized (this.mDeviceStateLock) {
            curAudioRoutes = this.mDeviceInventory.getCurAudioRoutes();
        }
        return curAudioRoutes;
    }

    boolean isAvrcpAbsoluteVolumeSupported() {
        boolean isAvrcpAbsoluteVolumeSupported;
        synchronized (this.mDeviceStateLock) {
            isAvrcpAbsoluteVolumeSupported = this.mBtHelper.isAvrcpAbsoluteVolumeSupported();
        }
        return isAvrcpAbsoluteVolumeSupported;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isBluetoothA2dpOn() {
        boolean z;
        synchronized (this.mDeviceStateLock) {
            z = this.mBluetoothA2dpEnabled;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postSetAvrcpAbsoluteVolumeIndex(int index) {
        sendIMsgNoDelay(15, 0, index);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postSetHearingAidVolumeIndex(int index, int streamType) {
        sendIIMsgNoDelay(14, 0, index, streamType);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postSetLeAudioVolumeIndex(int index, int maxIndex, int streamType) {
        BleVolumeInfo info = new BleVolumeInfo(index, maxIndex, streamType);
        sendLMsgNoDelay(46, 0, info);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void preSetModeOwnerPid(int pid, int mode) {
        sendIIMsgNoDelay(47, 0, pid, mode);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postSetModeOwnerPid(int pid, int mode) {
        sendIIMsgNoDelay(16, 0, pid, mode);
    }

    void postBluetoothA2dpDeviceConfigChange(BluetoothDevice device) {
        sendLMsgNoDelay(11, 2, device);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startBluetoothScoForClient(IBinder cb, int pid, int scoAudioMode, String eventSource) {
        if (AudioService.DEBUG_COMM_RTE) {
            Log.v(TAG, "startBluetoothScoForClient_Sync, pid: " + pid);
        }
        synchronized (this.mSetModeLock) {
            synchronized (this.mDeviceStateLock) {
                AudioDeviceAttributes device = new AudioDeviceAttributes(16, "");
                postSetCommunicationRouteForClient(new CommunicationClientInfo(cb, pid, device, scoAudioMode, eventSource));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stopBluetoothScoForClient(IBinder cb, int pid, String eventSource) {
        if (AudioService.DEBUG_COMM_RTE) {
            Log.v(TAG, "stopBluetoothScoForClient_Sync, pid: " + pid);
        }
        synchronized (this.mSetModeLock) {
            synchronized (this.mDeviceStateLock) {
                CommunicationRouteClient client = getCommunicationRouteClientForPid(pid);
                if (client != null && client.requestsBluetoothSco()) {
                    postSetCommunicationRouteForClient(new CommunicationClientInfo(cb, pid, null, -1, eventSource));
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int setPreferredDevicesForStrategySync(int strategy, List<AudioDeviceAttributes> devices) {
        return this.mDeviceInventory.setPreferredDevicesForStrategySync(strategy, devices);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int removePreferredDevicesForStrategySync(int strategy) {
        return this.mDeviceInventory.removePreferredDevicesForStrategySync(strategy);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerStrategyPreferredDevicesDispatcher(IStrategyPreferredDevicesDispatcher dispatcher) {
        this.mDeviceInventory.registerStrategyPreferredDevicesDispatcher(dispatcher);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterStrategyPreferredDevicesDispatcher(IStrategyPreferredDevicesDispatcher dispatcher) {
        this.mDeviceInventory.unregisterStrategyPreferredDevicesDispatcher(dispatcher);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int setPreferredDevicesForCapturePresetSync(int capturePreset, List<AudioDeviceAttributes> devices) {
        return this.mDeviceInventory.setPreferredDevicesForCapturePresetSync(capturePreset, devices);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int clearPreferredDevicesForCapturePresetSync(int capturePreset) {
        return this.mDeviceInventory.clearPreferredDevicesForCapturePresetSync(capturePreset);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerCapturePresetDevicesRoleDispatcher(ICapturePresetDevicesRoleDispatcher dispatcher) {
        this.mDeviceInventory.registerCapturePresetDevicesRoleDispatcher(dispatcher);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterCapturePresetDevicesRoleDispatcher(ICapturePresetDevicesRoleDispatcher dispatcher) {
        this.mDeviceInventory.unregisterCapturePresetDevicesRoleDispatcher(dispatcher);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerCommunicationDeviceDispatcher(ICommunicationDeviceDispatcher dispatcher) {
        this.mCommDevDispatchers.register(dispatcher);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterCommunicationDeviceDispatcher(ICommunicationDeviceDispatcher dispatcher) {
        this.mCommDevDispatchers.unregister(dispatcher);
    }

    private void dispatchCommunicationDevice() {
        AudioDeviceInfo audioDeviceInfo = this.mActiveCommunicationDevice;
        int portId = audioDeviceInfo == null ? 0 : audioDeviceInfo.getId();
        if (portId == this.mCurCommunicationPortId) {
            return;
        }
        this.mCurCommunicationPortId = portId;
        int nbDispatchers = this.mCommDevDispatchers.beginBroadcast();
        for (int i = 0; i < nbDispatchers; i++) {
            try {
                this.mCommDevDispatchers.getBroadcastItem(i).dispatchCommunicationDeviceChanged(portId);
            } catch (RemoteException e) {
            }
        }
        this.mCommDevDispatchers.finishBroadcast();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postAccessoryPlugMediaUnmute(int device) {
        this.mAudioService.postAccessoryPlugMediaUnmute(device);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getVssVolumeForDevice(int streamType, int device) {
        return this.mAudioService.getVssVolumeForDevice(streamType, device);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getMaxVssVolumeForStream(int streamType) {
        return this.mAudioService.getMaxVssVolumeForStream(streamType);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getDeviceForStream(int streamType) {
        return this.mAudioService.getDeviceForStream(streamType);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postApplyVolumeOnDevice(int streamType, int device, String caller) {
        this.mAudioService.postApplyVolumeOnDevice(streamType, device, caller);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postSetVolumeIndexOnDevice(int streamType, int vssVolIndex, int device, String caller) {
        this.mAudioService.postSetVolumeIndexOnDevice(streamType, vssVolIndex, device, caller);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postObserveDevicesForAllStreams() {
        this.mAudioService.postObserveDevicesForAllStreams();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isInCommunication() {
        return this.mAudioService.isInCommunication();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasMediaDynamicPolicy() {
        return this.mAudioService.hasMediaDynamicPolicy();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ContentResolver getContentResolver() {
        return this.mAudioService.getContentResolver();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void checkMusicActive(int deviceType, String caller) {
        this.mAudioService.checkMusicActive(deviceType, caller);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void checkVolumeCecOnHdmiConnection(int state, String caller) {
        this.mAudioService.postCheckVolumeCecOnHdmiConnection(state, caller);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasAudioFocusUsers() {
        return this.mAudioService.hasAudioFocusUsers();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postBroadcastScoConnectionState(int state) {
        sendIMsgNoDelay(3, 2, state);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postBroadcastBecomingNoisy() {
        sendMsgNoDelay(12, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postBluetoothActiveDevice(BtDeviceInfo info, int delay) {
        sendLMsg(7, 2, info, delay);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postSetWiredDeviceConnectionState(AudioDeviceInventory.WiredDeviceConnectionState connectionState, int delay) {
        sendLMsg(2, 2, connectionState, delay);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postBtProfileDisconnected(int profile) {
        sendIMsgNoDelay(22, 2, profile);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postBtProfileConnected(int profile, BluetoothProfile proxy) {
        sendILMsgNoDelay(23, 2, profile, proxy);
    }

    void postCommunicationRouteClientDied(CommunicationRouteClient client) {
        sendLMsgNoDelay(34, 2, client);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postSaveSetPreferredDevicesForStrategy(int strategy, List<AudioDeviceAttributes> devices) {
        sendILMsgNoDelay(32, 2, strategy, devices);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postSaveRemovePreferredDevicesForStrategy(int strategy) {
        sendIMsgNoDelay(33, 2, strategy);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postSaveSetPreferredDevicesForCapturePreset(int capturePreset, List<AudioDeviceAttributes> devices) {
        sendILMsgNoDelay(37, 2, capturePreset, devices);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postSaveClearPreferredDevicesForCapturePreset(int capturePreset) {
        sendIMsgNoDelay(38, 2, capturePreset);
    }

    void postUpdateCommunicationRouteClient(String eventSource) {
        sendLMsgNoDelay(43, 2, eventSource);
    }

    void postSetCommunicationRouteForClient(CommunicationClientInfo info) {
        sendLMsgNoDelay(42, 2, info);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postScoAudioStateChanged(int state) {
        sendIMsgNoDelay(44, 2, state);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class CommunicationClientInfo {
        final IBinder mCb;
        final AudioDeviceAttributes mDevice;
        final String mEventSource;
        final int mPid;
        final int mScoAudioMode;

        CommunicationClientInfo(IBinder cb, int pid, AudioDeviceAttributes device, int scoAudioMode, String eventSource) {
            this.mCb = cb;
            this.mPid = pid;
            this.mDevice = device;
            this.mScoAudioMode = scoAudioMode;
            this.mEventSource = eventSource;
        }

        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (this == o) {
                return true;
            }
            if (!(o instanceof CommunicationClientInfo) || !this.mCb.equals(((CommunicationClientInfo) o).mCb) || this.mPid != ((CommunicationClientInfo) o).mPid) {
                return false;
            }
            return true;
        }

        public String toString() {
            return "CommunicationClientInfo mCb=" + this.mCb.toString() + "mPid=" + this.mPid + "mDevice=" + this.mDevice.toString() + "mScoAudioMode=" + this.mScoAudioMode + "mEventSource=" + this.mEventSource;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setBluetoothA2dpOnInt(boolean on, boolean fromA2dp, String source) {
        String eventSource = "setBluetoothA2dpOn(" + on + ") from u/pid:" + Binder.getCallingUid() + SliceClientPermissions.SliceAuthority.DELIMITER + Binder.getCallingPid() + " src:" + source;
        synchronized (this.mDeviceStateLock) {
            this.mBluetoothA2dpEnabled = on;
            this.mBrokerHandler.removeMessages(5);
            onSetForceUse(1, this.mBluetoothA2dpEnabled ? 0 : 10, fromA2dp, eventSource);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean handleDeviceConnection(AudioDeviceAttributes attributes, boolean connect) {
        boolean handleDeviceConnection;
        synchronized (this.mDeviceStateLock) {
            handleDeviceConnection = this.mDeviceInventory.handleDeviceConnection(attributes, connect, false);
        }
        return handleDeviceConnection;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleFailureToConnectToBtHeadsetService(int delay) {
        sendMsg(9, 0, delay);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleCancelFailureToConnectToBtHeadsetService() {
        this.mBrokerHandler.removeMessages(9);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postReportNewRoutes(boolean fromA2dp) {
        sendMsgNoDelay(fromA2dp ? 36 : 13, 1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasScheduledA2dpConnection(BluetoothDevice btDevice) {
        BtDeviceInfo devInfoToCheck = new BtDeviceInfo(btDevice, 2);
        return this.mBrokerHandler.hasEqualMessages(7, devInfoToCheck);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setA2dpTimeout(String address, int a2dpCodec, int delayMs) {
        sendILMsg(10, 2, a2dpCodec, address, delayMs);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAvrcpAbsoluteVolumeSupported(boolean supported) {
        synchronized (this.mDeviceStateLock) {
            this.mBtHelper.setAvrcpAbsoluteVolumeSupported(supported);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearAvrcpAbsoluteVolumeSupported() {
        setAvrcpAbsoluteVolumeSupported(false);
        this.mAudioService.setAvrcpAbsoluteVolumeSupported(false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getBluetoothA2dpEnabled() {
        boolean z;
        synchronized (this.mDeviceStateLock) {
            z = this.mBluetoothA2dpEnabled;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLeAudioTimeout(String address, int device, int delayMs) {
        sendILMsg(48, 2, device, address, delayMs);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void broadcastStickyIntentToCurrentProfileGroup(Intent intent) {
        this.mSystemServer.broadcastStickyIntentToCurrentProfileGroup(intent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(final PrintWriter pw, final String prefix) {
        if (this.mBrokerHandler != null) {
            pw.println(prefix + "Message handler (watch for unhandled messages):");
            this.mBrokerHandler.dump(new PrintWriterPrinter(pw), prefix + "  ");
        } else {
            pw.println("Message handler is null");
        }
        this.mDeviceInventory.dump(pw, prefix);
        pw.println("\n" + prefix + "Communication route clients:");
        this.mCommunicationRouteClients.forEach(new Consumer() { // from class: com.android.server.audio.AudioDeviceBroker$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                AudioDeviceBroker.CommunicationRouteClient communicationRouteClient = (AudioDeviceBroker.CommunicationRouteClient) obj;
                pw.println("  " + prefix + "pid: " + communicationRouteClient.getPid() + " device: " + communicationRouteClient.getDevice() + " cb: " + communicationRouteClient.getBinder());
            }
        });
        pw.println("\n" + prefix + "Computed Preferred communication device: " + preferredCommunicationDevice());
        pw.println("\n" + prefix + "Applied Preferred communication device: " + this.mPreferredCommunicationDevice);
        pw.println(prefix + "Active communication device: " + ((Object) (this.mActiveCommunicationDevice == null ? "None" : new AudioDeviceAttributes(this.mActiveCommunicationDevice))));
        pw.println(prefix + "mCommunicationStrategyId: " + this.mCommunicationStrategyId);
        pw.println(prefix + "mAccessibilityStrategyId: " + this.mAccessibilityStrategyId);
        pw.println("\n" + prefix + "mModeOwnerPid: " + this.mModeOwnerPid);
        this.mBtHelper.dump(pw, prefix);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSetForceUse(int useCase, int config, boolean fromA2dp, String eventSource) {
        if (useCase == 1) {
            postReportNewRoutes(fromA2dp);
        }
        AudioService.sForceUseLogger.log(new AudioServiceEvents.ForceUseEvent(useCase, config, eventSource));
        new MediaMetrics.Item("audio.forceUse." + AudioSystem.forceUseUsageToString(useCase)).set(MediaMetrics.Property.EVENT, "onSetForceUse").set(MediaMetrics.Property.FORCE_USE_DUE_TO, eventSource).set(MediaMetrics.Property.FORCE_USE_MODE, AudioSystem.forceUseConfigToString(config)).record();
        if (AudioService.DEBUG_COMM_RTE) {
            Log.v(TAG, "onSetForceUse(useCase<" + AudioSystem.forceUseUsageToString(useCase) + ">, config<" + AudioSystem.forceUseConfigToString(config) + ">, fromA2dp<" + fromA2dp + ">, eventSource<" + eventSource + ">)");
        }
        AudioSystem.setForceUse(useCase, config);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSendBecomingNoisyIntent() {
        AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent("broadcast ACTION_AUDIO_BECOMING_NOISY").printLog(TAG));
        this.mSystemServer.sendDeviceBecomingNoisyIntent();
    }

    private void setupMessaging(Context ctxt) {
        PowerManager pm = (PowerManager) ctxt.getSystemService("power");
        this.mBrokerEventWakeLock = pm.newWakeLock(1, "handleAudioDeviceEvent");
        BrokerThread brokerThread = new BrokerThread();
        this.mBrokerThread = brokerThread;
        brokerThread.start();
        waitForBrokerHandlerCreation();
    }

    private void waitForBrokerHandlerCreation() {
        synchronized (this) {
            while (this.mBrokerHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Log.e(TAG, "Interruption while waiting on BrokerHandler");
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class BrokerThread extends Thread {
        BrokerThread() {
            super("AudioDeviceBroker");
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            Looper.prepare();
            synchronized (AudioDeviceBroker.this) {
                AudioDeviceBroker.this.mBrokerHandler = new BrokerHandler();
                AudioDeviceBroker.this.notify();
            }
            Looper.loop();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class BrokerHandler extends Handler {
        private BrokerHandler() {
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = -1;
            switch (msg.what) {
                case 1:
                    synchronized (AudioDeviceBroker.this.mSetModeLock) {
                        synchronized (AudioDeviceBroker.this.mDeviceStateLock) {
                            AudioDeviceBroker.this.initRoutingStrategyIds();
                            AudioDeviceBroker.this.updateActiveCommunicationDevice();
                            AudioDeviceBroker.this.mDeviceInventory.onRestoreDevices();
                            AudioDeviceBroker.this.mBtHelper.onAudioServerDiedRestoreA2dp();
                            AudioDeviceBroker.this.onUpdateCommunicationRoute("MSG_RESTORE_DEVICES");
                        }
                        break;
                    }
                case 2:
                    synchronized (AudioDeviceBroker.this.mDeviceStateLock) {
                        AudioDeviceBroker.this.mDeviceInventory.onSetWiredDeviceConnectionState((AudioDeviceInventory.WiredDeviceConnectionState) msg.obj);
                    }
                    break;
                case 3:
                    synchronized (AudioDeviceBroker.this.mDeviceStateLock) {
                        AudioDeviceBroker.this.mBtHelper.onBroadcastScoConnectionState(msg.arg1);
                    }
                    break;
                case 4:
                case 5:
                    AudioDeviceBroker.this.onSetForceUse(msg.arg1, msg.arg2, msg.what == 5, (String) msg.obj);
                    break;
                case 6:
                    synchronized (AudioDeviceBroker.this.mDeviceStateLock) {
                        AudioDeviceBroker.this.mDeviceInventory.onToggleHdmi();
                    }
                    break;
                case 7:
                    synchronized (AudioDeviceBroker.this.mDeviceStateLock) {
                        BtDeviceInfo btInfo = (BtDeviceInfo) msg.obj;
                        AudioDeviceInventory audioDeviceInventory = AudioDeviceBroker.this.mDeviceInventory;
                        if (btInfo.mProfile == 22 && !btInfo.mIsLeOutput) {
                            audioDeviceInventory.onSetBtActiveDevice(btInfo, i);
                        }
                        i = AudioDeviceBroker.this.mAudioService.getBluetoothContextualVolumeStream();
                        audioDeviceInventory.onSetBtActiveDevice(btInfo, i);
                    }
                    break;
                case 8:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 24:
                case 25:
                case 26:
                case 27:
                case 28:
                case 29:
                case 30:
                case 31:
                case 40:
                case 41:
                default:
                    if (AudioDeviceBroker.this.mAudioService.getAudioServiceExtInstance().isBleAudioFeatureSupported()) {
                        AudioDeviceBroker.this.mAudioService.getAudioServiceExtInstance().handleMessageExt(msg);
                        break;
                    } else {
                        Log.wtf(AudioDeviceBroker.TAG, "Invalid message " + msg.what);
                        break;
                    }
                case 9:
                    synchronized (AudioDeviceBroker.this.mSetModeLock) {
                        synchronized (AudioDeviceBroker.this.mDeviceStateLock) {
                            AudioDeviceBroker.this.mBtHelper.resetBluetoothSco();
                        }
                        break;
                    }
                case 10:
                    synchronized (AudioDeviceBroker.this.mDeviceStateLock) {
                        AudioDeviceBroker.this.mDeviceInventory.onMakeA2dpDeviceUnavailableNow((String) msg.obj, msg.arg1);
                    }
                    break;
                case 11:
                    BluetoothDevice btDevice = (BluetoothDevice) msg.obj;
                    synchronized (AudioDeviceBroker.this.mDeviceStateLock) {
                        int a2dpCodec = AudioDeviceBroker.this.mBtHelper.getA2dpCodec(btDevice);
                        AudioDeviceBroker.this.mDeviceInventory.onBluetoothA2dpDeviceConfigChange(new BtHelper.BluetoothA2dpDeviceInfo(btDevice, -1, a2dpCodec), 0);
                    }
                    break;
                case 12:
                    AudioDeviceBroker.this.onSendBecomingNoisyIntent();
                    break;
                case 13:
                case 36:
                    synchronized (AudioDeviceBroker.this.mDeviceStateLock) {
                        AudioDeviceBroker.this.mDeviceInventory.onReportNewRoutes();
                    }
                    break;
                case 14:
                    synchronized (AudioDeviceBroker.this.mDeviceStateLock) {
                        AudioDeviceBroker.this.mBtHelper.setHearingAidVolume(msg.arg1, msg.arg2);
                    }
                    break;
                case 15:
                    synchronized (AudioDeviceBroker.this.mDeviceStateLock) {
                        AudioDeviceBroker.this.mBtHelper.setAvrcpAbsoluteVolumeIndex(msg.arg1);
                    }
                    break;
                case 16:
                    synchronized (AudioDeviceBroker.this.mSetModeLock) {
                        synchronized (AudioDeviceBroker.this.mDeviceStateLock) {
                            AudioDeviceBroker.this.mModeOwnerPid = msg.arg1;
                            AudioDeviceBroker audioDeviceBroker = AudioDeviceBroker.this;
                            audioDeviceBroker.mPreModeOwnerPid = audioDeviceBroker.mModeOwnerPid;
                            if (msg.arg2 != 1) {
                                AudioDeviceBroker.this.onUpdateCommunicationRouteClient("setNewModeOwner");
                            }
                        }
                        break;
                    }
                case 22:
                    if (msg.arg1 != 1) {
                        synchronized (AudioDeviceBroker.this.mDeviceStateLock) {
                            AudioDeviceBroker.this.mDeviceInventory.onBtProfileDisconnected(msg.arg1);
                        }
                        break;
                    } else {
                        synchronized (AudioDeviceBroker.this.mSetModeLock) {
                            synchronized (AudioDeviceBroker.this.mDeviceStateLock) {
                                AudioDeviceBroker.this.mBtHelper.disconnectHeadset();
                            }
                            break;
                        }
                    }
                case 23:
                    if (msg.arg1 != 1) {
                        synchronized (AudioDeviceBroker.this.mDeviceStateLock) {
                            AudioDeviceBroker.this.mBtHelper.onBtProfileConnected(msg.arg1, (BluetoothProfile) msg.obj);
                        }
                        break;
                    } else {
                        synchronized (AudioDeviceBroker.this.mSetModeLock) {
                            synchronized (AudioDeviceBroker.this.mDeviceStateLock) {
                                AudioDeviceBroker.this.mBtHelper.onHeadsetProfileConnected((BluetoothHeadset) msg.obj);
                            }
                            break;
                        }
                    }
                case 32:
                    int strategy = msg.arg1;
                    List<AudioDeviceAttributes> devices = (List) msg.obj;
                    AudioDeviceBroker.this.mDeviceInventory.onSaveSetPreferredDevices(strategy, devices);
                    break;
                case 33:
                    int strategy2 = msg.arg1;
                    AudioDeviceBroker.this.mDeviceInventory.onSaveRemovePreferredDevices(strategy2);
                    break;
                case 34:
                    synchronized (AudioDeviceBroker.this.mSetModeLock) {
                        synchronized (AudioDeviceBroker.this.mDeviceStateLock) {
                            AudioDeviceBroker.this.onCommunicationRouteClientDied((CommunicationRouteClient) msg.obj);
                        }
                        break;
                    }
                case 35:
                    AudioDeviceBroker.this.checkMessagesMuteMusic(0);
                    break;
                case 37:
                    int capturePreset = msg.arg1;
                    List<AudioDeviceAttributes> devices2 = (List) msg.obj;
                    AudioDeviceBroker.this.mDeviceInventory.onSaveSetPreferredDevicesForCapturePreset(capturePreset, devices2);
                    break;
                case 38:
                    int capturePreset2 = msg.arg1;
                    AudioDeviceBroker.this.mDeviceInventory.onSaveClearPreferredDevicesForCapturePreset(capturePreset2);
                    break;
                case 39:
                    synchronized (AudioDeviceBroker.this.mSetModeLock) {
                        synchronized (AudioDeviceBroker.this.mDeviceStateLock) {
                            AudioDeviceBroker.this.onUpdateCommunicationRoute((String) msg.obj);
                        }
                        break;
                    }
                case 42:
                    synchronized (AudioDeviceBroker.this.mSetModeLock) {
                        synchronized (AudioDeviceBroker.this.mDeviceStateLock) {
                            CommunicationClientInfo info = (CommunicationClientInfo) msg.obj;
                            AudioDeviceBroker.this.setCommunicationRouteForClient(info.mCb, info.mPid, info.mDevice, info.mScoAudioMode, info.mEventSource);
                        }
                        break;
                    }
                case 43:
                    synchronized (AudioDeviceBroker.this.mSetModeLock) {
                        synchronized (AudioDeviceBroker.this.mDeviceStateLock) {
                            AudioDeviceBroker.this.onUpdateCommunicationRouteClient((String) msg.obj);
                        }
                        break;
                    }
                case 44:
                    synchronized (AudioDeviceBroker.this.mSetModeLock) {
                        synchronized (AudioDeviceBroker.this.mDeviceStateLock) {
                            AudioDeviceBroker.this.mBtHelper.onScoAudioStateChanged(msg.arg1);
                        }
                        break;
                    }
                case 45:
                    BtDeviceInfo info2 = (BtDeviceInfo) msg.obj;
                    if (info2.mDevice != null) {
                        if (AudioService.DEBUG_DEVICES) {
                            AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent("msg: onBluetoothActiveDeviceChange  state=" + info2.mState + " addr=" + info2.mDevice.getAddress() + " prof=" + info2.mProfile + " supprNoisy=" + info2.mSupprNoisy + " src=" + info2.mEventSource).printLog(AudioDeviceBroker.TAG));
                        }
                        synchronized (AudioDeviceBroker.this.mDeviceStateLock) {
                            AudioDeviceBroker.this.mDeviceInventory.setBluetoothActiveDevice(info2);
                        }
                        break;
                    }
                    break;
                case 46:
                    BleVolumeInfo info3 = (BleVolumeInfo) msg.obj;
                    synchronized (AudioDeviceBroker.this.mDeviceStateLock) {
                        AudioDeviceBroker.this.mBtHelper.setLeAudioVolume(info3.mIndex, info3.mMaxIndex, info3.mStreamType);
                    }
                    break;
                case 47:
                    synchronized (AudioDeviceBroker.this.mSetModeLock) {
                        synchronized (AudioDeviceBroker.this.mDeviceStateLock) {
                            AudioDeviceBroker.this.mPreModeOwnerPid = msg.arg1;
                        }
                        break;
                    }
                case 48:
                    synchronized (AudioDeviceBroker.this.mDeviceStateLock) {
                        AudioDeviceBroker.this.mDeviceInventory.onMakeLeAudioDeviceUnavailableNow((String) msg.obj, msg.arg1);
                    }
                    break;
            }
            if (AudioDeviceBroker.MESSAGES_MUTE_MUSIC.contains(Integer.valueOf(msg.what))) {
                int delay = 0;
                if (msg.what == 7 && ((BtDeviceInfo) msg.obj).mState == 0) {
                    delay = 300;
                }
                delay = (msg.what == 11 || msg.what == 29) ? 400 : 400;
                if (msg.what == 2) {
                    AudioDeviceInventory.WiredDeviceConnectionState wdcs = (AudioDeviceInventory.WiredDeviceConnectionState) msg.obj;
                    if (wdcs.mState == 0) {
                        AudioDeviceBroker.this.sendMsg(35, 0, 400 + 100);
                    }
                } else {
                    if (AudioService.DEBUG_DEVICES) {
                        int totalDelay = delay + 100;
                        Log.d(AudioDeviceBroker.TAG, "Music stream's unmute delay is increased by" + totalDelay + " msfor A2DP/LE/Wired connection changes, msgid=" + msg.what);
                    }
                    AudioDeviceBroker.this.sendMsg(35, 0, delay + 100);
                }
            }
            if (AudioDeviceBroker.isMessageHandledUnderWakelock(msg.what)) {
                try {
                    AudioDeviceBroker.this.mBrokerEventWakeLock.release();
                } catch (Exception e) {
                    Log.e(AudioDeviceBroker.TAG, "Exception releasing wakelock", e);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isMessageHandledUnderWakelock(int msgId) {
        switch (msgId) {
            case 2:
            case 6:
            case 7:
            case 10:
            case 11:
            case 29:
            case 31:
            case 35:
            case 48:
                return true;
            default:
                return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendMsg(int msg, int existingMsgPolicy, int delay) {
        sendIILMsg(msg, existingMsgPolicy, 0, 0, null, delay);
    }

    private void sendILMsg(int msg, int existingMsgPolicy, int arg, Object obj, int delay) {
        sendIILMsg(msg, existingMsgPolicy, arg, 0, obj, delay);
    }

    private void sendLMsg(int msg, int existingMsgPolicy, Object obj, int delay) {
        sendIILMsg(msg, existingMsgPolicy, 0, 0, obj, delay);
    }

    private void sendIMsg(int msg, int existingMsgPolicy, int arg, int delay) {
        sendIILMsg(msg, existingMsgPolicy, arg, 0, null, delay);
    }

    private void sendMsgNoDelay(int msg, int existingMsgPolicy) {
        sendIILMsg(msg, existingMsgPolicy, 0, 0, null, 0);
    }

    private void sendIMsgNoDelay(int msg, int existingMsgPolicy, int arg) {
        sendIILMsg(msg, existingMsgPolicy, arg, 0, null, 0);
    }

    private void sendIIMsgNoDelay(int msg, int existingMsgPolicy, int arg1, int arg2) {
        sendIILMsg(msg, existingMsgPolicy, arg1, arg2, null, 0);
    }

    private void sendILMsgNoDelay(int msg, int existingMsgPolicy, int arg, Object obj) {
        sendIILMsg(msg, existingMsgPolicy, arg, 0, obj, 0);
    }

    private void sendLMsgNoDelay(int msg, int existingMsgPolicy, Object obj) {
        sendIILMsg(msg, existingMsgPolicy, 0, 0, obj, 0);
    }

    private void sendIILMsgNoDelay(int msg, int existingMsgPolicy, int arg1, int arg2, Object obj) {
        sendIILMsg(msg, existingMsgPolicy, arg1, arg2, obj, 0);
    }

    private void sendIILMsg(int msg, int existingMsgPolicy, int arg1, int arg2, Object obj, int delay) {
        if (existingMsgPolicy == 0) {
            this.mBrokerHandler.removeMessages(msg);
        } else if (existingMsgPolicy == 1 && this.mBrokerHandler.hasMessages(msg)) {
            return;
        }
        if (isMessageHandledUnderWakelock(msg)) {
            long identity = Binder.clearCallingIdentity();
            try {
                this.mBrokerEventWakeLock.acquire(BROKER_WAKELOCK_TIMEOUT_MS);
            } catch (Exception e) {
                Log.e(TAG, "Exception acquiring wakelock", e);
            }
            Binder.restoreCallingIdentity(identity);
        }
        if (MESSAGES_MUTE_MUSIC.contains(Integer.valueOf(msg))) {
            if (msg == 2) {
                AudioDeviceInventory.WiredDeviceConnectionState wdcs = (AudioDeviceInventory.WiredDeviceConnectionState) obj;
                if (wdcs.mState == 1) {
                    if (AudioService.DEBUG_DEVICES) {
                        Log.d(TAG, "wired device connected, do not mute");
                    }
                } else if (AudioSystem.isStreamActive(3, 0) && this.mAudioService.getDeviceForStream(3) == wdcs.mAttributes.getInternalType()) {
                    checkMessagesMuteMusic(msg);
                }
            } else {
                checkMessagesMuteMusic(msg);
            }
        }
        synchronized (sLastDeviceConnectionMsgTimeLock) {
            long time = SystemClock.uptimeMillis() + delay;
            switch (msg) {
                case 2:
                case 7:
                case 10:
                case 11:
                case 48:
                    long j = sLastDeviceConnectMsgTime;
                    if (j >= time) {
                        time = j + 30;
                    }
                    sLastDeviceConnectMsgTime = time;
                    break;
            }
            BrokerHandler brokerHandler = this.mBrokerHandler;
            brokerHandler.sendMessageAtTime(brokerHandler.obtainMessage(msg, arg1, arg2, obj), time);
        }
    }

    private static <T> boolean hasIntersection(Set<T> a, Set<T> b) {
        for (T e : a) {
            if (b.contains(e)) {
                return true;
            }
        }
        return false;
    }

    boolean messageMutesMusic(int message) {
        if (message == 0) {
            return false;
        }
        if ((message == 7 || message == 29 || message == 11) && AudioSystem.isStreamActive(3, 0) && hasIntersection(AudioDeviceInventory.DEVICE_OVERRIDE_A2DP_ROUTE_ON_PLUG_SET, this.mAudioService.getDeviceSetForStream(3))) {
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postCheckMessagesMuteMusic() {
        Log.d(TAG, "postCheckMessagesMuteMusic() true");
        checkMessagesMuteMusic(7);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkMessagesMuteMusic(int message) {
        boolean mute = messageMutesMusic(message);
        if (!mute) {
            Iterator<Integer> it = MESSAGES_MUTE_MUSIC.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                int msg = it.next().intValue();
                if (this.mBrokerHandler.hasMessages(msg) && messageMutesMusic(msg)) {
                    mute = true;
                    break;
                }
            }
        }
        if (mute != this.mMusicMuted.getAndSet(mute)) {
            this.mAudioService.setMusicMute(mute);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class CommunicationRouteClient implements IBinder.DeathRecipient {
        private final IBinder mCb;
        private AudioDeviceAttributes mDevice;
        private final int mPid;

        CommunicationRouteClient(IBinder cb, int pid, AudioDeviceAttributes device) {
            this.mCb = cb;
            this.mPid = pid;
            this.mDevice = device;
        }

        public boolean registerDeathRecipient() {
            try {
                this.mCb.linkToDeath(this, 0);
                return true;
            } catch (RemoteException e) {
                Log.w(AudioDeviceBroker.TAG, "CommunicationRouteClient could not link to " + this.mCb + " binder death");
                return false;
            }
        }

        public void unregisterDeathRecipient() {
            try {
                this.mCb.unlinkToDeath(this, 0);
            } catch (NoSuchElementException e) {
                Log.w(AudioDeviceBroker.TAG, "CommunicationRouteClient could not not unregistered to binder");
            }
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            AudioDeviceBroker.this.postCommunicationRouteClientDied(this);
        }

        IBinder getBinder() {
            return this.mCb;
        }

        int getPid() {
            return this.mPid;
        }

        AudioDeviceAttributes getDevice() {
            return this.mDevice;
        }

        boolean requestsBluetoothSco() {
            AudioDeviceAttributes audioDeviceAttributes = this.mDevice;
            return audioDeviceAttributes != null && audioDeviceAttributes.getType() == 7;
        }

        boolean requestsSpeakerphone() {
            AudioDeviceAttributes audioDeviceAttributes = this.mDevice;
            return audioDeviceAttributes != null && audioDeviceAttributes.getType() == 2;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onCommunicationRouteClientDied(CommunicationRouteClient client) {
        if (client == null) {
            return;
        }
        Log.w(TAG, "Communication client died");
        removeCommunicationRouteClient(client.getBinder(), true);
        onUpdateCommunicationRouteClient("onCommunicationRouteClientDied");
    }

    private AudioDeviceAttributes preferredCommunicationDevice() {
        AudioDeviceAttributes device;
        AudioDeviceAttributes device2;
        boolean z = true;
        if (this.mAudioService.getAudioServiceExtInstance().isBleAudioFeatureSupported() && (device2 = this.mAudioService.getAudioServiceExtInstance().preferredCommunicationDevice()) != null) {
            return device2;
        }
        boolean btSCoOn = (this.mBluetoothScoOn && this.mBtHelper.isBluetoothScoOn()) ? false : false;
        if (btSCoOn && (device = this.mBtHelper.getHeadsetAudioDevice()) != null) {
            return device;
        }
        AudioDeviceAttributes device3 = requestedCommunicationDevice();
        if (device3 == null || device3.getType() == 7 || device3.getType() == 26) {
            return null;
        }
        return device3;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUpdateCommunicationRoute(String eventSource) {
        AudioDeviceAttributes preferredCommunicationDevice = preferredCommunicationDevice();
        if (AudioService.DEBUG_COMM_RTE) {
            Log.v(TAG, "onUpdateCommunicationRoute, preferredCommunicationDevice: " + preferredCommunicationDevice + " eventSource: " + eventSource);
        }
        AudioService.sDeviceLogger.log(new AudioEventLogger.StringEvent("onUpdateCommunicationRoute, preferredCommunicationDevice: " + preferredCommunicationDevice + " eventSource: " + eventSource));
        if (preferredCommunicationDevice == null || preferredCommunicationDevice.getType() != 7) {
            AudioSystem.setParameters("BT_SCO=off");
        } else {
            AudioSystem.setParameters("BT_SCO=on");
        }
        if (preferredCommunicationDevice == null) {
            removePreferredDevicesForStrategySync(this.mCommunicationStrategyId);
            removePreferredDevicesForStrategySync(this.mAccessibilityStrategyId);
        } else {
            setPreferredDevicesForStrategySync(this.mCommunicationStrategyId, Arrays.asList(preferredCommunicationDevice));
            setPreferredDevicesForStrategySync(this.mAccessibilityStrategyId, Arrays.asList(preferredCommunicationDevice));
        }
        onUpdatePhoneStrategyDevice(preferredCommunicationDevice);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUpdateCommunicationRouteClient(String eventSource) {
        onUpdateCommunicationRoute(eventSource);
        CommunicationRouteClient crc = topCommunicationRouteClient();
        if (AudioService.DEBUG_COMM_RTE) {
            Log.v(TAG, "onUpdateCommunicationRouteClient, crc: " + crc + " eventSource: " + eventSource);
        }
        if (crc != null) {
            setCommunicationRouteForClient(crc.getBinder(), crc.getPid(), crc.getDevice(), -1, eventSource);
        }
    }

    private void onUpdatePhoneStrategyDevice(AudioDeviceAttributes device) {
        synchronized (this.mSetModeLock) {
            synchronized (this.mDeviceStateLock) {
                boolean wasSpeakerphoneActive = isSpeakerphoneActive();
                this.mPreferredCommunicationDevice = device;
                updateActiveCommunicationDevice();
                if (wasSpeakerphoneActive != isSpeakerphoneActive()) {
                    try {
                        this.mContext.sendBroadcastAsUser(new Intent("android.media.action.SPEAKERPHONE_STATE_CHANGED").setFlags(1073741824), UserHandle.ALL);
                    } catch (Exception e) {
                        Log.w(TAG, "failed to broadcast ACTION_SPEAKERPHONE_STATE_CHANGED: " + e);
                    }
                }
                this.mAudioService.postUpdateRingerModeServiceInt();
                dispatchCommunicationDevice();
            }
        }
    }

    private CommunicationRouteClient removeCommunicationRouteClient(IBinder cb, boolean unregister) {
        Iterator<CommunicationRouteClient> it = this.mCommunicationRouteClients.iterator();
        while (it.hasNext()) {
            CommunicationRouteClient cl = it.next();
            if (cl.getBinder() == cb) {
                if (unregister) {
                    cl.unregisterDeathRecipient();
                }
                this.mCommunicationRouteClients.remove(cl);
                return cl;
            }
        }
        return null;
    }

    private CommunicationRouteClient addCommunicationRouteClient(IBinder cb, int pid, AudioDeviceAttributes device) {
        removeCommunicationRouteClient(cb, true);
        CommunicationRouteClient client = new CommunicationRouteClient(cb, pid, device);
        if (client.registerDeathRecipient()) {
            this.mCommunicationRouteClients.add(0, client);
            return client;
        }
        return null;
    }

    private CommunicationRouteClient getCommunicationRouteClientForPid(int pid) {
        Iterator<CommunicationRouteClient> it = this.mCommunicationRouteClients.iterator();
        while (it.hasNext()) {
            CommunicationRouteClient cl = it.next();
            if (cl.getPid() == pid) {
                return cl;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UUID getDeviceSensorUuid(AudioDeviceAttributes device) {
        UUID deviceSensorUuid;
        synchronized (this.mDeviceStateLock) {
            deviceSensorUuid = this.mDeviceInventory.getDeviceSensorUuid(device);
        }
        return deviceSensorUuid;
    }
}
