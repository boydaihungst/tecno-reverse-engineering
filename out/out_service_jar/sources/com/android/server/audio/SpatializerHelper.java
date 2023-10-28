package com.android.server.audio;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.audio.common.V2_0.AudioChannelMask;
import android.media.AudioAttributes;
import android.media.AudioDeviceAttributes;
import android.media.AudioFormat;
import android.media.AudioSystem;
import android.media.INativeSpatializerCallback;
import android.media.ISpatializer;
import android.media.ISpatializerCallback;
import android.media.ISpatializerHeadToSoundStagePoseCallback;
import android.media.ISpatializerHeadTrackerAvailableCallback;
import android.media.ISpatializerHeadTrackingCallback;
import android.media.ISpatializerHeadTrackingModeCallback;
import android.media.ISpatializerOutputCallback;
import android.media.MediaMetrics;
import android.media.Spatializer;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.SparseIntArray;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
/* loaded from: classes.dex */
public class SpatializerHelper {
    private static final boolean DEBUG = true;
    private static final boolean DEBUG_MORE = false;
    private static final String METRICS_DEVICE_PREFIX = "audio.spatializer.device.";
    private static final int STATE_DISABLED_AVAILABLE = 6;
    private static final int STATE_DISABLED_UNAVAILABLE = 3;
    private static final int STATE_ENABLED_AVAILABLE = 5;
    private static final int STATE_ENABLED_UNAVAILABLE = 4;
    private static final int STATE_NOT_SUPPORTED = 1;
    private static final int STATE_UNINITIALIZED = 0;
    private static final String TAG = "AS.SpatializerHelper";
    private final AudioSystemAdapter mASA;
    private final AudioService mAudioService;
    private HelperDynamicSensorCallback mDynSensorCallback;
    private SensorManager mSensorManager;
    private ISpatializer mSpat;
    private SpatializerCallback mSpatCallback;
    private static final SparseIntArray SPAT_MODE_FOR_DEVICE_TYPE = new SparseIntArray(15) { // from class: com.android.server.audio.SpatializerHelper.1
        {
            append(2, 1);
            append(3, 0);
            append(4, 0);
            append(8, 0);
            append(13, 1);
            append(12, 1);
            append(11, 1);
            append(22, 0);
            append(5, 1);
            append(6, 1);
            append(19, 1);
            append(23, 0);
            append(26, 0);
            append(27, 1);
            append(30, 0);
        }
    };
    private static final int[] WIRELESS_TYPES = {7, 8, 26, 27, 30};
    private static final int[] WIRELESS_SPEAKER_TYPES = {27};
    private static final AudioAttributes DEFAULT_ATTRIBUTES = new AudioAttributes.Builder().setUsage(1).build();
    private static final AudioFormat DEFAULT_FORMAT = new AudioFormat.Builder().setEncoding(2).setSampleRate(48000).setChannelMask(AudioChannelMask.IN_6).build();
    private static final AudioDeviceAttributes[] ROUTING_DEVICES = new AudioDeviceAttributes[1];
    private int mState = 0;
    private boolean mFeatureEnabled = false;
    private int mSpatLevel = 0;
    private int mCapableSpatLevel = 0;
    private boolean mTransauralSupported = false;
    private boolean mBinauralSupported = false;
    private boolean mIsHeadTrackingSupported = false;
    private int[] mSupportedHeadTrackingModes = new int[0];
    private int mActualHeadTrackingMode = -2;
    private int mDesiredHeadTrackingMode = 1;
    private boolean mHeadTrackerAvailable = false;
    private int mDesiredHeadTrackingModeWhenEnabled = 1;
    private int mSpatOutput = 0;
    private SpatializerHeadTrackingCallback mSpatHeadTrackingCallback = new SpatializerHeadTrackingCallback();
    private final ArrayList<Integer> mSACapableDeviceTypes = new ArrayList<>(0);
    private final ArrayList<SADeviceState> mSADevices = new ArrayList<>(0);
    final RemoteCallbackList<ISpatializerCallback> mStateCallbacks = new RemoteCallbackList<>();
    final RemoteCallbackList<ISpatializerHeadTrackingModeCallback> mHeadTrackingModeCallbacks = new RemoteCallbackList<>();
    final RemoteCallbackList<ISpatializerHeadTrackerAvailableCallback> mHeadTrackerCallbacks = new RemoteCallbackList<>();
    final RemoteCallbackList<ISpatializerHeadToSoundStagePoseCallback> mHeadPoseCallbacks = new RemoteCallbackList<>();
    final RemoteCallbackList<ISpatializerOutputCallback> mOutputCallbacks = new RemoteCallbackList<>();

    private static void logd(String s) {
        Log.i(TAG, s);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SpatializerHelper(AudioService mother, AudioSystemAdapter asa) {
        this.mAudioService = mother;
        this.mASA = asa;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [298=6, 300=5, 301=4] */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:111:0x01b3 A[Catch: all -> 0x01f3, TRY_LEAVE, TryCatch #2 {, blocks: (B:3:0x0001, B:5:0x001b, B:8:0x0025, B:10:0x0029, B:12:0x0037, B:15:0x0041, B:64:0x0138, B:89:0x018a, B:109:0x01af, B:111:0x01b3, B:115:0x01b8, B:95:0x019a, B:106:0x01aa, B:120:0x01cc, B:124:0x01d2, B:125:0x01d3, B:126:0x01f2), top: B:132:0x0001 }] */
    /* JADX WARN: Removed duplicated region for block: B:114:0x01b7  */
    /* JADX WARN: Removed duplicated region for block: B:130:0x01cc A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public synchronized void init(boolean effectExpected) {
        byte[] levels;
        loglogi("init effectExpected=" + effectExpected);
        if (!effectExpected) {
            loglogi("init(): setting state to STATE_NOT_SUPPORTED due to effect not expected");
            this.mState = 1;
        } else if (this.mState != 0) {
            throw new IllegalStateException(logloge("init() called in state " + this.mState));
        } else {
            SpatializerCallback spatializerCallback = new SpatializerCallback();
            this.mSpatCallback = spatializerCallback;
            ISpatializer spat = AudioSystem.getSpatializer(spatializerCallback);
            if (spat == null) {
                loglogi("init(): No Spatializer found");
                this.mState = 1;
                return;
            }
            resetCapabilities();
            try {
                try {
                    levels = spat.getSupportedLevels();
                } catch (RemoteException e) {
                }
            } catch (RemoteException e2) {
            } catch (Throwable th) {
                th = th;
                if (spat != null) {
                }
                throw th;
            }
            if (levels != null) {
                try {
                    try {
                    } catch (RemoteException e3) {
                        resetCapabilities();
                        if (spat != null) {
                            spat.release();
                        }
                        if (this.mCapableSpatLevel != 0) {
                        }
                    }
                    if (levels.length != 0 && (levels.length != 1 || levels[0] != 0)) {
                        int length = levels.length;
                        int i = 0;
                        while (true) {
                            if (i >= length) {
                                break;
                            }
                            byte level = levels[i];
                            loglogi("init(): found support for level: " + ((int) level));
                            if (level == 1) {
                                loglogi("init(): setting capable level to LEVEL_MULTICHANNEL");
                                this.mCapableSpatLevel = level;
                                break;
                            }
                            i++;
                        }
                        boolean isHeadTrackingSupported = spat.isHeadTrackingSupported();
                        this.mIsHeadTrackingSupported = isHeadTrackingSupported;
                        if (isHeadTrackingSupported) {
                            byte[] values = spat.getSupportedHeadTrackingModes();
                            ArrayList<Integer> list = new ArrayList<>(0);
                            for (byte value : values) {
                                switch (value) {
                                    case 0:
                                    case 1:
                                        break;
                                    case 2:
                                    case 3:
                                        list.add(Integer.valueOf(headTrackingModeTypeToSpatializerInt(value)));
                                        break;
                                    default:
                                        Log.e(TAG, "Unexpected head tracking mode:" + ((int) value), new IllegalArgumentException("invalid mode"));
                                        break;
                                }
                            }
                            this.mSupportedHeadTrackingModes = new int[list.size()];
                            for (int i2 = 0; i2 < list.size(); i2++) {
                                this.mSupportedHeadTrackingModes[i2] = list.get(i2).intValue();
                            }
                            this.mActualHeadTrackingMode = headTrackingModeTypeToSpatializerInt(spat.getActualHeadTrackingMode());
                        } else {
                            this.mDesiredHeadTrackingModeWhenEnabled = -2;
                            this.mDesiredHeadTrackingMode = -2;
                        }
                        byte[] spatModes = spat.getSupportedModes();
                        for (byte mode : spatModes) {
                            switch (mode) {
                                case 0:
                                    this.mBinauralSupported = true;
                                    break;
                                case 1:
                                    this.mTransauralSupported = true;
                                    break;
                                default:
                                    logloge("init(): Spatializer reports unknown supported mode:" + ((int) mode));
                                    break;
                            }
                        }
                        if (!this.mBinauralSupported && !this.mTransauralSupported) {
                            this.mState = 1;
                            if (spat != null) {
                                try {
                                    spat.release();
                                } catch (RemoteException e4) {
                                }
                            }
                            return;
                        }
                        int i3 = 0;
                        while (true) {
                            SparseIntArray sparseIntArray = SPAT_MODE_FOR_DEVICE_TYPE;
                            if (i3 >= sparseIntArray.size()) {
                                if (this.mTransauralSupported) {
                                    addCompatibleAudioDevice(new AudioDeviceAttributes(2, ""), false);
                                }
                                if (this.mBinauralSupported) {
                                    addCompatibleAudioDevice(new AudioDeviceAttributes(8, ""), false);
                                }
                                if (spat != null) {
                                    spat.release();
                                }
                                if (this.mCapableSpatLevel != 0) {
                                    this.mState = 1;
                                    return;
                                }
                                this.mState = 3;
                                this.mASA.getDevicesForAttributes(DEFAULT_ATTRIBUTES, false).toArray(ROUTING_DEVICES);
                                return;
                            }
                            int mode2 = sparseIntArray.valueAt(i3);
                            if ((mode2 == 0 && this.mBinauralSupported) || (mode2 == 1 && this.mTransauralSupported)) {
                                this.mSACapableDeviceTypes.add(Integer.valueOf(sparseIntArray.keyAt(i3)));
                            }
                            i3++;
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    if (spat != null) {
                        try {
                            spat.release();
                        } catch (RemoteException e5) {
                        }
                    }
                    throw th;
                }
            }
            logloge("init(): found Spatializer is useless");
            this.mState = 1;
            if (spat != null) {
                try {
                    spat.release();
                } catch (RemoteException e6) {
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void reset(boolean featureEnabled) {
        loglogi("Resetting featureEnabled=" + featureEnabled);
        releaseSpat();
        this.mState = 0;
        this.mSpatLevel = 0;
        this.mActualHeadTrackingMode = -2;
        init(true);
        setSpatializerEnabledInt(featureEnabled);
    }

    private void resetCapabilities() {
        this.mCapableSpatLevel = 0;
        this.mBinauralSupported = false;
        this.mTransauralSupported = false;
        this.mIsHeadTrackingSupported = false;
        this.mSupportedHeadTrackingModes = new int[0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void onRoutingUpdated() {
        byte level;
        if (this.mFeatureEnabled) {
            switch (this.mState) {
                case 0:
                case 1:
                    return;
                default:
                    AudioSystemAdapter audioSystemAdapter = this.mASA;
                    AudioAttributes audioAttributes = DEFAULT_ATTRIBUTES;
                    ArrayList<AudioDeviceAttributes> devicesForAttributes = audioSystemAdapter.getDevicesForAttributes(audioAttributes, false);
                    AudioDeviceAttributes[] audioDeviceAttributesArr = ROUTING_DEVICES;
                    devicesForAttributes.toArray(audioDeviceAttributesArr);
                    if (isWireless(audioDeviceAttributesArr[0].getType())) {
                        addWirelessDeviceIfNew(audioDeviceAttributesArr[0]);
                    }
                    Pair<Boolean, Boolean> enabledAvailable = evaluateState(audioDeviceAttributesArr[0]);
                    boolean able = false;
                    if (((Boolean) enabledAvailable.second).booleanValue()) {
                        able = canBeSpatializedOnDevice(audioAttributes, DEFAULT_FORMAT, audioDeviceAttributesArr);
                        loglogi("onRoutingUpdated: can spatialize media 5.1:" + able + " on device:" + audioDeviceAttributesArr[0]);
                        setDispatchAvailableState(able);
                    } else {
                        loglogi("onRoutingUpdated: device:" + audioDeviceAttributesArr[0] + " not available for Spatial Audio");
                        setDispatchAvailableState(false);
                    }
                    boolean enabled = able && ((Boolean) enabledAvailable.first).booleanValue();
                    if (enabled) {
                        loglogi("Enabling Spatial Audio since enabled for media device:" + audioDeviceAttributesArr[0]);
                    } else {
                        loglogi("Disabling Spatial Audio since disabled for media device:" + audioDeviceAttributesArr[0]);
                    }
                    if (this.mSpat != null) {
                        if (enabled) {
                            level = 1;
                        } else {
                            level = 0;
                        }
                        loglogi("Setting spatialization level to: " + ((int) level));
                        try {
                            this.mSpat.setLevel(level);
                        } catch (RemoteException e) {
                            Log.e(TAG, "Can't set spatializer level", e);
                            this.mState = 1;
                            this.mCapableSpatLevel = 0;
                            enabled = false;
                        }
                    }
                    setDispatchFeatureEnabledState(enabled, "onRoutingUpdated");
                    int i = this.mDesiredHeadTrackingMode;
                    if (i != -2 && i != -1) {
                        postInitSensors();
                    }
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SpatializerCallback extends INativeSpatializerCallback.Stub {
        private SpatializerCallback() {
        }

        public void onLevelChanged(byte level) {
            SpatializerHelper.loglogi("SpatializerCallback.onLevelChanged level:" + ((int) level));
            synchronized (SpatializerHelper.this) {
                SpatializerHelper.this.mSpatLevel = SpatializerHelper.spatializationLevelToSpatializerInt(level);
            }
            SpatializerHelper.this.postInitSensors();
        }

        public void onOutputChanged(int output) {
            int oldOutput;
            SpatializerHelper.loglogi("SpatializerCallback.onOutputChanged output:" + output);
            synchronized (SpatializerHelper.this) {
                oldOutput = SpatializerHelper.this.mSpatOutput;
                SpatializerHelper.this.mSpatOutput = output;
            }
            if (oldOutput != output) {
                SpatializerHelper.this.dispatchOutputUpdate(output);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SpatializerHeadTrackingCallback extends ISpatializerHeadTrackingCallback.Stub {
        private SpatializerHeadTrackingCallback() {
        }

        public void onHeadTrackingModeChanged(byte mode) {
            int oldMode;
            int newMode;
            synchronized (this) {
                oldMode = SpatializerHelper.this.mActualHeadTrackingMode;
                SpatializerHelper.this.mActualHeadTrackingMode = SpatializerHelper.headTrackingModeTypeToSpatializerInt(mode);
                newMode = SpatializerHelper.this.mActualHeadTrackingMode;
            }
            SpatializerHelper.loglogi("SpatializerHeadTrackingCallback.onHeadTrackingModeChanged mode:" + Spatializer.headtrackingModeToString(newMode));
            if (oldMode != newMode) {
                SpatializerHelper.this.dispatchActualHeadTrackingMode(newMode);
            }
        }

        public void onHeadToSoundStagePoseUpdated(float[] headToStage) {
            if (headToStage == null) {
                Log.e(SpatializerHelper.TAG, "SpatializerHeadTrackingCallback.onHeadToStagePoseUpdatednull transform");
            } else if (headToStage.length != 6) {
                Log.e(SpatializerHelper.TAG, "SpatializerHeadTrackingCallback.onHeadToStagePoseUpdated invalid transform length" + headToStage.length);
            } else {
                SpatializerHelper.this.dispatchPoseUpdate(headToStage);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class HelperDynamicSensorCallback extends SensorManager.DynamicSensorCallback {
        private HelperDynamicSensorCallback() {
        }

        @Override // android.hardware.SensorManager.DynamicSensorCallback
        public void onDynamicSensorConnected(Sensor sensor) {
            SpatializerHelper.this.postInitSensors();
        }

        @Override // android.hardware.SensorManager.DynamicSensorCallback
        public void onDynamicSensorDisconnected(Sensor sensor) {
            SpatializerHelper.this.postInitSensors();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized List<AudioDeviceAttributes> getCompatibleAudioDevices() {
        ArrayList<AudioDeviceAttributes> compatList;
        compatList = new ArrayList<>();
        Iterator<SADeviceState> it = this.mSADevices.iterator();
        while (it.hasNext()) {
            SADeviceState dev = it.next();
            if (dev.mEnabled) {
                compatList.add(new AudioDeviceAttributes(2, dev.mDeviceType, dev.mDeviceAddress == null ? "" : dev.mDeviceAddress));
            }
        }
        return compatList;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void addCompatibleAudioDevice(AudioDeviceAttributes ada) {
        addCompatibleAudioDevice(ada, true);
    }

    /* JADX WARN: Removed duplicated region for block: B:15:0x004f  */
    /* JADX WARN: Removed duplicated region for block: B:21:0x0067  */
    /* JADX WARN: Removed duplicated region for block: B:30:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void addCompatibleAudioDevice(AudioDeviceAttributes ada, boolean forceEnable) {
        loglogi("addCompatibleAudioDevice: dev=" + ada);
        int deviceType = ada.getType();
        boolean wireless = isWireless(deviceType);
        boolean isInList = false;
        SADeviceState deviceUpdated = null;
        Iterator<SADeviceState> it = this.mSADevices.iterator();
        while (it.hasNext()) {
            SADeviceState deviceState = it.next();
            if (deviceType == deviceState.mDeviceType && (!wireless || ada.getAddress().equals(deviceState.mDeviceAddress))) {
                isInList = true;
                if (forceEnable) {
                    deviceState.mEnabled = true;
                    deviceUpdated = deviceState;
                }
                if (!isInList) {
                    SADeviceState dev = new SADeviceState(deviceType, wireless ? ada.getAddress() : "");
                    dev.mEnabled = true;
                    this.mSADevices.add(dev);
                    deviceUpdated = dev;
                }
                if (deviceUpdated == null) {
                    onRoutingUpdated();
                    this.mAudioService.persistSpatialAudioDeviceSettings();
                    logDeviceState(deviceUpdated, "addCompatibleAudioDevice");
                    return;
                }
                return;
            }
        }
        if (!isInList) {
        }
        if (deviceUpdated == null) {
        }
    }

    private void logDeviceState(SADeviceState deviceState, String event) {
        String deviceName = AudioSystem.getDeviceName(deviceState.mDeviceType);
        new MediaMetrics.Item(METRICS_DEVICE_PREFIX + deviceName).set(MediaMetrics.Property.ADDRESS, deviceState.mDeviceAddress).set(MediaMetrics.Property.ENABLED, deviceState.mEnabled ? "true" : "false").set(MediaMetrics.Property.EVENT, TextUtils.emptyIfNull(event)).set(MediaMetrics.Property.HAS_HEAD_TRACKER, deviceState.mHasHeadTracker ? "true" : "false").set(MediaMetrics.Property.HEAD_TRACKER_ENABLED, deviceState.mHeadTrackerEnabled ? "true" : "false").record();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void removeCompatibleAudioDevice(AudioDeviceAttributes ada) {
        loglogi("removeCompatibleAudioDevice: dev=" + ada);
        int deviceType = ada.getType();
        boolean wireless = isWireless(deviceType);
        SADeviceState deviceUpdated = null;
        Iterator<SADeviceState> it = this.mSADevices.iterator();
        while (it.hasNext()) {
            SADeviceState deviceState = it.next();
            if (deviceType == deviceState.mDeviceType && (!wireless || ada.getAddress().equals(deviceState.mDeviceAddress))) {
                deviceState.mEnabled = false;
                deviceUpdated = deviceState;
                break;
            }
        }
        if (deviceUpdated != null) {
            onRoutingUpdated();
            this.mAudioService.persistSpatialAudioDeviceSettings();
            logDeviceState(deviceUpdated, "removeCompatibleAudioDevice");
        }
    }

    private synchronized Pair<Boolean, Boolean> evaluateState(AudioDeviceAttributes ada) {
        int deviceType = ada.getType();
        boolean wireless = isWireless(deviceType);
        if (!wireless) {
            if (!this.mSACapableDeviceTypes.contains(Integer.valueOf(deviceType))) {
                Log.i(TAG, "Device incompatible with Spatial Audio dev:" + ada);
                return new Pair<>(false, false);
            }
            int spatMode = SPAT_MODE_FOR_DEVICE_TYPE.get(deviceType, Integer.MIN_VALUE);
            if (spatMode == Integer.MIN_VALUE) {
                Log.e(TAG, "no spatialization mode found for device type:" + deviceType);
                return new Pair<>(false, false);
            } else if (spatMode == 1) {
                deviceType = 2;
            } else {
                deviceType = 4;
            }
        } else if (isWirelessSpeaker(deviceType) && !this.mTransauralSupported) {
            Log.i(TAG, "Device incompatible with Spatial Audio (no transaural) dev:" + ada);
            return new Pair<>(false, false);
        } else if (!this.mBinauralSupported) {
            Log.i(TAG, "Device incompatible with Spatial Audio (no binaural) dev:" + ada);
            return new Pair<>(false, false);
        }
        boolean enabled = false;
        boolean available = false;
        Iterator<SADeviceState> it = this.mSADevices.iterator();
        while (it.hasNext()) {
            SADeviceState deviceState = it.next();
            if ((deviceType == deviceState.mDeviceType && wireless && ada.getAddress().equals(deviceState.mDeviceAddress)) || !wireless) {
                available = true;
                enabled = deviceState.mEnabled;
                break;
            }
        }
        return new Pair<>(Boolean.valueOf(enabled), Boolean.valueOf(available));
    }

    private synchronized void addWirelessDeviceIfNew(AudioDeviceAttributes ada) {
        boolean knownDevice = false;
        Iterator<SADeviceState> it = this.mSADevices.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            SADeviceState deviceState = it.next();
            if (ada.getType() == deviceState.mDeviceType && ada.getAddress().equals(deviceState.mDeviceAddress)) {
                knownDevice = true;
                break;
            }
        }
        if (!knownDevice) {
            SADeviceState deviceState2 = new SADeviceState(ada.getType(), ada.getAddress());
            this.mSADevices.add(deviceState2);
            this.mAudioService.persistSpatialAudioDeviceSettings();
            logDeviceState(deviceState2, "addWirelessDeviceIfNew");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized boolean isEnabled() {
        switch (this.mState) {
            case 0:
            case 1:
            case 3:
            case 6:
                return false;
            case 2:
            case 4:
            case 5:
            default:
                return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized boolean isAvailable() {
        switch (this.mState) {
            case 0:
            case 1:
            case 3:
            case 4:
                return false;
            case 2:
            default:
                return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:11:0x001f A[Catch: all -> 0x003f, TryCatch #0 {, blocks: (B:3:0x0001, B:8:0x000b, B:9:0x0019, B:11:0x001f, B:14:0x002b), top: B:26:0x0001 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public synchronized boolean isAvailableForDevice(AudioDeviceAttributes ada) {
        if (ada.getRole() != 2) {
            return false;
        }
        int deviceType = ada.getType();
        boolean wireless = isWireless(deviceType);
        Iterator<SADeviceState> it = this.mSADevices.iterator();
        while (it.hasNext()) {
            SADeviceState deviceState = it.next();
            if (deviceType != deviceState.mDeviceType || !wireless || !ada.getAddress().equals(deviceState.mDeviceAddress)) {
                while (it.hasNext()) {
                }
            }
            return true;
        }
        return false;
    }

    private synchronized boolean canBeSpatializedOnDevice(AudioAttributes attributes, AudioFormat format, AudioDeviceAttributes[] devices) {
        byte modeForDevice = (byte) SPAT_MODE_FOR_DEVICE_TYPE.get(devices[0].getType(), 0);
        if ((modeForDevice == 0 && this.mBinauralSupported) || (modeForDevice == 1 && this.mTransauralSupported)) {
            return AudioSystem.canBeSpatialized(attributes, format, devices);
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void setFeatureEnabled(boolean enabled) {
        loglogi("setFeatureEnabled(" + enabled + ") was featureEnabled:" + this.mFeatureEnabled);
        if (this.mFeatureEnabled == enabled) {
            return;
        }
        this.mFeatureEnabled = enabled;
        if (enabled) {
            int i = this.mState;
            if (i == 1) {
                Log.e(TAG, "Can't enabled Spatial Audio, unsupported");
                return;
            }
            if (i == 0) {
                init(true);
            }
            setSpatializerEnabledInt(true);
        } else {
            setSpatializerEnabledInt(false);
        }
    }

    synchronized void setSpatializerEnabledInt(boolean enabled) {
        switch (this.mState) {
            case 0:
                if (enabled) {
                    throw new IllegalStateException("Can't enable when uninitialized");
                }
                return;
            case 1:
                if (enabled) {
                    Log.e(TAG, "Can't enable when unsupported");
                }
                return;
            case 3:
            case 6:
                if (enabled) {
                    createSpat();
                    onRoutingUpdated();
                    break;
                } else {
                    return;
                }
            case 4:
            case 5:
                if (!enabled) {
                    releaseSpat();
                    break;
                } else {
                    return;
                }
        }
        setDispatchFeatureEnabledState(enabled, "setSpatializerEnabledInt");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized int getCapableImmersiveAudioLevel() {
        return this.mCapableSpatLevel;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void registerStateCallback(ISpatializerCallback callback) {
        this.mStateCallbacks.register(callback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void unregisterStateCallback(ISpatializerCallback callback) {
        this.mStateCallbacks.unregister(callback);
    }

    private synchronized void setDispatchFeatureEnabledState(boolean featureEnabled, String source) {
        if (featureEnabled) {
            switch (this.mState) {
                case 3:
                    this.mState = 4;
                    break;
                case 4:
                case 5:
                    loglogi("setDispatchFeatureEnabledState(" + featureEnabled + ") no dispatch: mState:" + spatStateString(this.mState) + " src:" + source);
                    return;
                case 6:
                    this.mState = 5;
                    break;
                default:
                    throw new IllegalStateException("Invalid mState:" + this.mState + " for enabled true");
            }
        } else {
            switch (this.mState) {
                case 3:
                case 6:
                    loglogi("setDispatchFeatureEnabledState(" + featureEnabled + ") no dispatch: mState:" + spatStateString(this.mState) + " src:" + source);
                    return;
                case 4:
                    this.mState = 3;
                    break;
                case 5:
                    this.mState = 6;
                    break;
                default:
                    throw new IllegalStateException("Invalid mState:" + this.mState + " for enabled false");
            }
        }
        loglogi("setDispatchFeatureEnabledState(" + featureEnabled + ") mState:" + spatStateString(this.mState));
        int nbCallbacks = this.mStateCallbacks.beginBroadcast();
        for (int i = 0; i < nbCallbacks; i++) {
            try {
                this.mStateCallbacks.getBroadcastItem(i).dispatchSpatializerEnabledChanged(featureEnabled);
            } catch (RemoteException e) {
                Log.e(TAG, "Error in dispatchSpatializerEnabledChanged", e);
            }
        }
        this.mStateCallbacks.finishBroadcast();
    }

    private synchronized void setDispatchAvailableState(boolean available) {
        switch (this.mState) {
            case 0:
            case 1:
                throw new IllegalStateException("Should not update available state in state:" + this.mState);
            case 3:
                if (available) {
                    this.mState = 6;
                    break;
                } else {
                    loglogi("setDispatchAvailableState(" + available + ") no dispatch: mState:" + spatStateString(this.mState));
                    return;
                }
            case 4:
                if (available) {
                    this.mState = 5;
                    break;
                } else {
                    loglogi("setDispatchAvailableState(" + available + ") no dispatch: mState:" + spatStateString(this.mState));
                    return;
                }
            case 5:
                if (available) {
                    loglogi("setDispatchAvailableState(" + available + ") no dispatch: mState:" + spatStateString(this.mState));
                    return;
                } else {
                    this.mState = 4;
                    break;
                }
            case 6:
                if (available) {
                    loglogi("setDispatchAvailableState(" + available + ") no dispatch: mState:" + spatStateString(this.mState));
                    return;
                } else {
                    this.mState = 3;
                    break;
                }
        }
        loglogi("setDispatchAvailableState(" + available + ") mState:" + spatStateString(this.mState));
        int nbCallbacks = this.mStateCallbacks.beginBroadcast();
        for (int i = 0; i < nbCallbacks; i++) {
            try {
                this.mStateCallbacks.getBroadcastItem(i).dispatchSpatializerAvailableChanged(available);
            } catch (RemoteException e) {
                Log.e(TAG, "Error in dispatchSpatializerEnabledChanged", e);
            }
        }
        this.mStateCallbacks.finishBroadcast();
    }

    private void createSpat() {
        if (this.mSpat == null) {
            SpatializerCallback spatializerCallback = new SpatializerCallback();
            this.mSpatCallback = spatializerCallback;
            ISpatializer spatializer = AudioSystem.getSpatializer(spatializerCallback);
            this.mSpat = spatializer;
            try {
                if (this.mIsHeadTrackingSupported) {
                    this.mActualHeadTrackingMode = headTrackingModeTypeToSpatializerInt(spatializer.getActualHeadTrackingMode());
                    this.mSpat.registerHeadTrackingCallback(this.mSpatHeadTrackingCallback);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Can't configure head tracking", e);
                this.mState = 1;
                this.mCapableSpatLevel = 0;
                this.mActualHeadTrackingMode = -2;
            }
        }
    }

    private void releaseSpat() {
        ISpatializer iSpatializer = this.mSpat;
        if (iSpatializer != null) {
            this.mSpatCallback = null;
            try {
                if (this.mIsHeadTrackingSupported) {
                    iSpatializer.registerHeadTrackingCallback((ISpatializerHeadTrackingCallback) null);
                }
                this.mHeadTrackerAvailable = false;
                this.mSpat.release();
            } catch (RemoteException e) {
                Log.e(TAG, "Can't set release spatializer cleanly", e);
            }
            this.mSpat = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized boolean canBeSpatialized(AudioAttributes attributes, AudioFormat format) {
        switch (this.mState) {
            case 0:
            case 1:
            case 3:
            case 4:
                logd("canBeSpatialized false due to state:" + this.mState);
                return false;
            case 2:
            default:
                switch (attributes.getUsage()) {
                    case 1:
                    case 14:
                        AudioDeviceAttributes[] devices = new AudioDeviceAttributes[1];
                        this.mASA.getDevicesForAttributes(attributes, false).toArray(devices);
                        boolean able = canBeSpatializedOnDevice(attributes, format, devices);
                        logd("canBeSpatialized usage:" + attributes.getUsage() + " format:" + format.toLogFriendlyString() + " returning " + able);
                        return able;
                    default:
                        logd("canBeSpatialized false due to usage:" + attributes.getUsage());
                        return false;
                }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void registerHeadTrackingModeCallback(ISpatializerHeadTrackingModeCallback callback) {
        this.mHeadTrackingModeCallbacks.register(callback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void unregisterHeadTrackingModeCallback(ISpatializerHeadTrackingModeCallback callback) {
        this.mHeadTrackingModeCallbacks.unregister(callback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void registerHeadTrackerAvailableCallback(ISpatializerHeadTrackerAvailableCallback cb, boolean register) {
        if (register) {
            this.mHeadTrackerCallbacks.register(cb);
        } else {
            this.mHeadTrackerCallbacks.unregister(cb);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized int[] getSupportedHeadTrackingModes() {
        return this.mSupportedHeadTrackingModes;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized int getActualHeadTrackingMode() {
        return this.mActualHeadTrackingMode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized int getDesiredHeadTrackingMode() {
        return this.mDesiredHeadTrackingMode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void setGlobalTransform(float[] transform) {
        if (transform.length != 6) {
            throw new IllegalArgumentException("invalid array size" + transform.length);
        }
        if (checkSpatForHeadTracking("setGlobalTransform")) {
            try {
                this.mSpat.setGlobalTransform(transform);
            } catch (RemoteException e) {
                Log.e(TAG, "Error calling setGlobalTransform", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void recenterHeadTracker() {
        if (checkSpatForHeadTracking("recenterHeadTracker")) {
            try {
                this.mSpat.recenterHeadTracker();
            } catch (RemoteException e) {
                Log.e(TAG, "Error calling recenterHeadTracker", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void setDesiredHeadTrackingMode(int mode) {
        if (checkSpatForHeadTracking("setDesiredHeadTrackingMode")) {
            if (mode != -1) {
                this.mDesiredHeadTrackingModeWhenEnabled = mode;
            }
            try {
                if (this.mDesiredHeadTrackingMode != mode) {
                    this.mDesiredHeadTrackingMode = mode;
                    dispatchDesiredHeadTrackingMode(mode);
                }
                Log.i(TAG, "setDesiredHeadTrackingMode(" + Spatializer.headtrackingModeToString(mode) + ")");
                this.mSpat.setDesiredHeadTrackingMode(spatializerIntToHeadTrackingModeType(mode));
            } catch (RemoteException e) {
                Log.e(TAG, "Error calling setDesiredHeadTrackingMode", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:27:0x00d3 A[Catch: all -> 0x00dc, TryCatch #0 {, blocks: (B:3:0x0001, B:5:0x0005, B:6:0x0028, B:7:0x0036, B:9:0x003c, B:12:0x0048, B:15:0x0056, B:17:0x005a, B:20:0x0084, B:22:0x00b6, B:24:0x00c1, B:27:0x00d3, B:29:0x00d7), top: B:35:0x0001 }] */
    /* JADX WARN: Removed duplicated region for block: B:28:0x00d6  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public synchronized void setHeadTrackerEnabled(boolean enabled, AudioDeviceAttributes ada) {
        AudioDeviceAttributes[] audioDeviceAttributesArr;
        if (!this.mIsHeadTrackingSupported) {
            Log.v(TAG, "no headtracking support, ignoring setHeadTrackerEnabled to " + enabled + " for " + ada);
        }
        int deviceType = ada.getType();
        boolean wireless = isWireless(deviceType);
        Iterator<SADeviceState> it = this.mSADevices.iterator();
        while (it.hasNext()) {
            SADeviceState deviceState = it.next();
            if ((deviceType == deviceState.mDeviceType && wireless && ada.getAddress().equals(deviceState.mDeviceAddress)) || !wireless) {
                if (!deviceState.mHasHeadTracker) {
                    Log.e(TAG, "Called setHeadTrackerEnabled enabled:" + enabled + " device:" + ada + " on a device without headtracker");
                    return;
                }
                Log.i(TAG, "setHeadTrackerEnabled enabled:" + enabled + " device:" + ada);
                deviceState.mHeadTrackerEnabled = enabled;
                this.mAudioService.persistSpatialAudioDeviceSettings();
                logDeviceState(deviceState, "setHeadTrackerEnabled");
                audioDeviceAttributesArr = ROUTING_DEVICES;
                if (audioDeviceAttributesArr[0].getType() == deviceType && audioDeviceAttributesArr[0].getAddress().equals(ada.getAddress())) {
                    setDesiredHeadTrackingMode(!enabled ? this.mDesiredHeadTrackingModeWhenEnabled : -1);
                }
            }
        }
        audioDeviceAttributesArr = ROUTING_DEVICES;
        if (audioDeviceAttributesArr[0].getType() == deviceType) {
            setDesiredHeadTrackingMode(!enabled ? this.mDesiredHeadTrackingModeWhenEnabled : -1);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:11:0x0035 A[Catch: all -> 0x0056, TryCatch #0 {, blocks: (B:3:0x0001, B:5:0x0006, B:8:0x0021, B:9:0x002f, B:11:0x0035, B:14:0x0041, B:17:0x004f), top: B:26:0x0001 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public synchronized boolean hasHeadTracker(AudioDeviceAttributes ada) {
        if (!this.mIsHeadTrackingSupported) {
            Log.v(TAG, "no headtracking support, hasHeadTracker always false for " + ada);
            return false;
        }
        int deviceType = ada.getType();
        boolean wireless = isWireless(deviceType);
        Iterator<SADeviceState> it = this.mSADevices.iterator();
        while (it.hasNext()) {
            SADeviceState deviceState = it.next();
            if (deviceType != deviceState.mDeviceType || !wireless || !ada.getAddress().equals(deviceState.mDeviceAddress)) {
                while (it.hasNext()) {
                }
            }
            return deviceState.mHasHeadTracker;
        }
        return false;
    }

    /* JADX WARN: Removed duplicated region for block: B:11:0x0035 A[Catch: all -> 0x0081, TryCatch #0 {, blocks: (B:3:0x0001, B:5:0x0006, B:8:0x0021, B:9:0x002f, B:11:0x0035, B:14:0x0041, B:17:0x004f, B:19:0x0053, B:20:0x0061, B:24:0x0066), top: B:30:0x0001 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    synchronized boolean setHasHeadTracker(AudioDeviceAttributes ada) {
        if (!this.mIsHeadTrackingSupported) {
            Log.v(TAG, "no headtracking support, setHasHeadTracker always false for " + ada);
            return false;
        }
        int deviceType = ada.getType();
        boolean wireless = isWireless(deviceType);
        Iterator<SADeviceState> it = this.mSADevices.iterator();
        while (it.hasNext()) {
            SADeviceState deviceState = it.next();
            if (deviceType != deviceState.mDeviceType || !wireless || !ada.getAddress().equals(deviceState.mDeviceAddress)) {
                while (it.hasNext()) {
                }
            }
            if (!deviceState.mHasHeadTracker) {
                deviceState.mHasHeadTracker = true;
                this.mAudioService.persistSpatialAudioDeviceSettings();
                logDeviceState(deviceState, "setHasHeadTracker");
            }
            return deviceState.mHeadTrackerEnabled;
        }
        Log.e(TAG, "setHasHeadTracker: device not found for:" + ada);
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:11:0x0035 A[Catch: all -> 0x005c, TryCatch #0 {, blocks: (B:3:0x0001, B:5:0x0006, B:8:0x0021, B:9:0x002f, B:11:0x0035, B:14:0x0041, B:17:0x004f, B:21:0x0055), top: B:30:0x0001 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public synchronized boolean isHeadTrackerEnabled(AudioDeviceAttributes ada) {
        if (!this.mIsHeadTrackingSupported) {
            Log.v(TAG, "no headtracking support, isHeadTrackerEnabled always false for " + ada);
            return false;
        }
        int deviceType = ada.getType();
        boolean wireless = isWireless(deviceType);
        Iterator<SADeviceState> it = this.mSADevices.iterator();
        while (it.hasNext()) {
            SADeviceState deviceState = it.next();
            if (deviceType != deviceState.mDeviceType || !wireless || !ada.getAddress().equals(deviceState.mDeviceAddress)) {
                while (it.hasNext()) {
                }
            }
            if (deviceState.mHasHeadTracker) {
                return deviceState.mHeadTrackerEnabled;
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized boolean isHeadTrackerAvailable() {
        return this.mHeadTrackerAvailable;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private boolean checkSpatForHeadTracking(String funcName) {
        switch (this.mState) {
            case 0:
            case 1:
                return false;
            case 3:
            case 4:
            case 5:
            case 6:
                if (this.mSpat == null) {
                    throw new IllegalStateException("null Spatializer when calling " + funcName);
                }
                break;
        }
        return this.mIsHeadTrackingSupported;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchActualHeadTrackingMode(int newMode) {
        int nbCallbacks = this.mHeadTrackingModeCallbacks.beginBroadcast();
        for (int i = 0; i < nbCallbacks; i++) {
            try {
                this.mHeadTrackingModeCallbacks.getBroadcastItem(i).dispatchSpatializerActualHeadTrackingModeChanged(newMode);
            } catch (RemoteException e) {
                Log.e(TAG, "Error in dispatchSpatializerActualHeadTrackingModeChanged(" + newMode + ")", e);
            }
        }
        this.mHeadTrackingModeCallbacks.finishBroadcast();
    }

    private void dispatchDesiredHeadTrackingMode(int newMode) {
        int nbCallbacks = this.mHeadTrackingModeCallbacks.beginBroadcast();
        for (int i = 0; i < nbCallbacks; i++) {
            try {
                this.mHeadTrackingModeCallbacks.getBroadcastItem(i).dispatchSpatializerDesiredHeadTrackingModeChanged(newMode);
            } catch (RemoteException e) {
                Log.e(TAG, "Error in dispatchSpatializerDesiredHeadTrackingModeChanged(" + newMode + ")", e);
            }
        }
        this.mHeadTrackingModeCallbacks.finishBroadcast();
    }

    private void dispatchHeadTrackerAvailable(boolean available) {
        int nbCallbacks = this.mHeadTrackerCallbacks.beginBroadcast();
        for (int i = 0; i < nbCallbacks; i++) {
            try {
                this.mHeadTrackerCallbacks.getBroadcastItem(i).dispatchSpatializerHeadTrackerAvailable(available);
            } catch (RemoteException e) {
                Log.e(TAG, "Error in dispatchSpatializerHeadTrackerAvailable(" + available + ")", e);
            }
        }
        this.mHeadTrackerCallbacks.finishBroadcast();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void registerHeadToSoundstagePoseCallback(ISpatializerHeadToSoundStagePoseCallback callback) {
        this.mHeadPoseCallbacks.register(callback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void unregisterHeadToSoundstagePoseCallback(ISpatializerHeadToSoundStagePoseCallback callback) {
        this.mHeadPoseCallbacks.unregister(callback);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchPoseUpdate(float[] pose) {
        int nbCallbacks = this.mHeadPoseCallbacks.beginBroadcast();
        for (int i = 0; i < nbCallbacks; i++) {
            try {
                this.mHeadPoseCallbacks.getBroadcastItem(i).dispatchPoseChanged(pose);
            } catch (RemoteException e) {
                Log.e(TAG, "Error in dispatchPoseChanged", e);
            }
        }
        this.mHeadPoseCallbacks.finishBroadcast();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void setEffectParameter(int key, byte[] value) {
        switch (this.mState) {
            case 0:
            case 1:
                throw new IllegalStateException("Can't set parameter key:" + key + " without a spatializer");
            case 3:
            case 4:
            case 5:
            case 6:
                if (this.mSpat == null) {
                    throw new IllegalStateException("null Spatializer for setParameter for key:" + key);
                }
                break;
        }
        try {
            this.mSpat.setParameter(key, value);
        } catch (RemoteException e) {
            Log.e(TAG, "Error in setParameter for key:" + key, e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void getEffectParameter(int key, byte[] value) {
        switch (this.mState) {
            case 0:
            case 1:
                throw new IllegalStateException("Can't get parameter key:" + key + " without a spatializer");
            case 3:
            case 4:
            case 5:
            case 6:
                if (this.mSpat == null) {
                    throw new IllegalStateException("null Spatializer for getParameter for key:" + key);
                }
                break;
        }
        try {
            this.mSpat.getParameter(key, value);
        } catch (RemoteException e) {
            Log.e(TAG, "Error in getParameter for key:" + key, e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized int getOutput() {
        switch (this.mState) {
            case 0:
            case 1:
                throw new IllegalStateException("Can't get output without a spatializer");
            case 3:
            case 4:
            case 5:
            case 6:
                if (this.mSpat != null) {
                    break;
                } else {
                    throw new IllegalStateException("null Spatializer for getOutput");
                }
        }
        try {
        } catch (RemoteException e) {
            Log.e(TAG, "Error in getOutput", e);
            return 0;
        }
        return this.mSpat.getOutput();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void registerSpatializerOutputCallback(ISpatializerOutputCallback callback) {
        this.mOutputCallbacks.register(callback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void unregisterSpatializerOutputCallback(ISpatializerOutputCallback callback) {
        this.mOutputCallbacks.unregister(callback);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchOutputUpdate(int output) {
        int nbCallbacks = this.mOutputCallbacks.beginBroadcast();
        for (int i = 0; i < nbCallbacks; i++) {
            try {
                this.mOutputCallbacks.getBroadcastItem(i).dispatchSpatializerOutputChanged(output);
            } catch (RemoteException e) {
                Log.e(TAG, "Error in dispatchOutputUpdate", e);
            }
        }
        this.mOutputCallbacks.finishBroadcast();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void postInitSensors() {
        this.mAudioService.postInitSpatializerHeadTrackingSensors();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void onInitSensors() {
        HelperDynamicSensorCallback helperDynamicSensorCallback;
        boolean z = true;
        boolean init = this.mFeatureEnabled && this.mSpatLevel != 0;
        String action = init ? "initializing" : "releasing";
        if (this.mSpat == null) {
            logloge("not " + action + " sensors, null spatializer");
        } else if (!this.mIsHeadTrackingSupported) {
            logloge("not " + action + " sensors, spatializer doesn't support headtracking");
        } else {
            int headHandle = -1;
            int screenHandle = -1;
            if (init) {
                if (this.mSensorManager == null) {
                    try {
                        this.mSensorManager = (SensorManager) this.mAudioService.mContext.getSystemService("sensor");
                        HelperDynamicSensorCallback helperDynamicSensorCallback2 = new HelperDynamicSensorCallback();
                        this.mDynSensorCallback = helperDynamicSensorCallback2;
                        this.mSensorManager.registerDynamicSensorCallback(helperDynamicSensorCallback2);
                    } catch (Exception e) {
                        Log.e(TAG, "Error with SensorManager, can't initialize sensors", e);
                        this.mSensorManager = null;
                        this.mDynSensorCallback = null;
                        return;
                    }
                }
                headHandle = getHeadSensorHandleUpdateTracker();
                loglogi("head tracker sensor handle initialized to " + headHandle);
                screenHandle = getScreenSensorHandle();
                Log.i(TAG, "found screen sensor handle initialized to " + screenHandle);
            } else {
                SensorManager sensorManager = this.mSensorManager;
                if (sensorManager != null && (helperDynamicSensorCallback = this.mDynSensorCallback) != null) {
                    sensorManager.unregisterDynamicSensorCallback(helperDynamicSensorCallback);
                    this.mSensorManager = null;
                    this.mDynSensorCallback = null;
                }
            }
            try {
                Log.i(TAG, "setScreenSensor:" + screenHandle);
                this.mSpat.setScreenSensor(screenHandle);
            } catch (Exception e2) {
                Log.e(TAG, "Error calling setScreenSensor:" + screenHandle, e2);
            }
            try {
                Log.i(TAG, "setHeadSensor:" + headHandle);
                this.mSpat.setHeadSensor(headHandle);
                if (this.mHeadTrackerAvailable != (headHandle != -1)) {
                    if (headHandle == -1) {
                        z = false;
                    }
                    this.mHeadTrackerAvailable = z;
                    dispatchHeadTrackerAvailable(z);
                }
            } catch (Exception e3) {
                Log.e(TAG, "Error calling setHeadSensor:" + headHandle, e3);
            }
            setDesiredHeadTrackingMode(this.mDesiredHeadTrackingMode);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int headTrackingModeTypeToSpatializerInt(byte mode) {
        switch (mode) {
            case 0:
                return 0;
            case 1:
                return -1;
            case 2:
                return 1;
            case 3:
                return 2;
            default:
                throw new IllegalArgumentException("Unexpected head tracking mode:" + ((int) mode));
        }
    }

    private static byte spatializerIntToHeadTrackingModeType(int sdkMode) {
        switch (sdkMode) {
            case -1:
                return (byte) 1;
            case 0:
                return (byte) 0;
            case 1:
                return (byte) 2;
            case 2:
                return (byte) 3;
            default:
                throw new IllegalArgumentException("Unexpected head tracking mode:" + sdkMode);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int spatializationLevelToSpatializerInt(byte level) {
        switch (level) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                throw new IllegalArgumentException("Unexpected spatializer level:" + ((int) level));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw) {
        int[] iArr;
        pw.println("SpatializerHelper:");
        pw.println("\tmState:" + this.mState);
        pw.println("\tmSpatLevel:" + this.mSpatLevel);
        pw.println("\tmCapableSpatLevel:" + this.mCapableSpatLevel);
        pw.println("\tmIsHeadTrackingSupported:" + this.mIsHeadTrackingSupported);
        StringBuilder modesString = new StringBuilder();
        for (int mode : this.mSupportedHeadTrackingModes) {
            modesString.append(Spatializer.headtrackingModeToString(mode)).append(" ");
        }
        pw.println("\tsupported head tracking modes:" + ((Object) modesString));
        pw.println("\tmDesiredHeadTrackingMode:" + Spatializer.headtrackingModeToString(this.mDesiredHeadTrackingMode));
        pw.println("\tmActualHeadTrackingMode:" + Spatializer.headtrackingModeToString(this.mActualHeadTrackingMode));
        pw.println("\theadtracker available:" + this.mHeadTrackerAvailable);
        pw.println("\tsupports binaural:" + this.mBinauralSupported + " / transaural:" + this.mTransauralSupported);
        pw.println("\tmSpatOutput:" + this.mSpatOutput);
        pw.println("\tdevices:");
        Iterator<SADeviceState> it = this.mSADevices.iterator();
        while (it.hasNext()) {
            SADeviceState device = it.next();
            pw.println("\t\t" + device);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class SADeviceState {
        static final String SETTING_DEVICE_SEPARATOR = "\\|";
        static final String SETTING_DEVICE_SEPARATOR_CHAR = "|";
        static final String SETTING_FIELD_SEPARATOR = ",";
        final String mDeviceAddress;
        final int mDeviceType;
        boolean mEnabled = true;
        boolean mHasHeadTracker = false;
        boolean mHeadTrackerEnabled = true;

        SADeviceState(int deviceType, String address) {
            this.mDeviceType = deviceType;
            this.mDeviceAddress = (String) Objects.requireNonNull(address);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            SADeviceState sads = (SADeviceState) obj;
            if (this.mDeviceType == sads.mDeviceType && this.mDeviceAddress.equals(sads.mDeviceAddress) && this.mEnabled == sads.mEnabled && this.mHasHeadTracker == sads.mHasHeadTracker && this.mHeadTrackerEnabled == sads.mHeadTrackerEnabled) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(Integer.valueOf(this.mDeviceType), this.mDeviceAddress, Boolean.valueOf(this.mEnabled), Boolean.valueOf(this.mHasHeadTracker), Boolean.valueOf(this.mHeadTrackerEnabled));
        }

        public String toString() {
            return "type:" + this.mDeviceType + " addr:" + this.mDeviceAddress + " enabled:" + this.mEnabled + " HT:" + this.mHasHeadTracker + " HTenabled:" + this.mHeadTrackerEnabled;
        }

        String toPersistableString() {
            return this.mDeviceType + SETTING_FIELD_SEPARATOR + this.mDeviceAddress + SETTING_FIELD_SEPARATOR + (this.mEnabled ? "1" : "0") + SETTING_FIELD_SEPARATOR + (this.mHasHeadTracker ? "1" : "0") + SETTING_FIELD_SEPARATOR + (this.mHeadTrackerEnabled ? "1" : "0");
        }

        static SADeviceState fromPersistedString(String persistedString) {
            if (persistedString == null || persistedString.isEmpty()) {
                return null;
            }
            String[] fields = TextUtils.split(persistedString, SETTING_FIELD_SEPARATOR);
            if (fields.length != 5) {
                return null;
            }
            try {
                int deviceType = Integer.parseInt(fields[0]);
                SADeviceState deviceState = new SADeviceState(deviceType, fields[1]);
                deviceState.mEnabled = Integer.parseInt(fields[2]) == 1;
                deviceState.mHasHeadTracker = Integer.parseInt(fields[3]) == 1;
                deviceState.mHeadTrackerEnabled = Integer.parseInt(fields[4]) == 1;
                return deviceState;
            } catch (NumberFormatException e) {
                Log.e(SpatializerHelper.TAG, "unable to parse setting for SADeviceState: " + persistedString, e);
                return null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized String getSADeviceSettings() {
        StringBuilder settingsBuilder;
        settingsBuilder = new StringBuilder(this.mSADevices.size() * 25);
        for (int i = 0; i < this.mSADevices.size(); i++) {
            settingsBuilder.append(this.mSADevices.get(i).toPersistableString());
            if (i != this.mSADevices.size() - 1) {
                settingsBuilder.append("|");
            }
        }
        return settingsBuilder.toString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void setSADeviceSettings(String persistedSettings) {
        String[] devSettings = TextUtils.split((String) Objects.requireNonNull(persistedSettings), "\\|");
        for (String setting : devSettings) {
            SADeviceState devState = SADeviceState.fromPersistedString(setting);
            if (devState != null) {
                this.mSADevices.add(devState);
                logDeviceState(devState, "setSADeviceSettings");
            }
        }
    }

    private static String spatStateString(int state) {
        switch (state) {
            case 0:
                return "STATE_UNINITIALIZED";
            case 1:
                return "STATE_NOT_SUPPORTED";
            case 2:
            default:
                return "invalid state";
            case 3:
                return "STATE_DISABLED_UNAVAILABLE";
            case 4:
                return "STATE_ENABLED_UNAVAILABLE";
            case 5:
                return "STATE_ENABLED_AVAILABLE";
            case 6:
                return "STATE_DISABLED_AVAILABLE";
        }
    }

    private static boolean isWireless(int deviceType) {
        int[] iArr;
        for (int type : WIRELESS_TYPES) {
            if (type == deviceType) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWirelessSpeaker(int deviceType) {
        int[] iArr;
        for (int type : WIRELESS_SPEAKER_TYPES) {
            if (type == deviceType) {
                return true;
            }
        }
        return false;
    }

    private int getHeadSensorHandleUpdateTracker() {
        int headHandle = -1;
        UUID routingDeviceUuid = this.mAudioService.getDeviceSensorUuid(ROUTING_DEVICES[0]);
        List<Sensor> sensors = this.mSensorManager.getDynamicSensorList(37);
        for (Sensor sensor : sensors) {
            UUID uuid = sensor.getUuid();
            if (uuid.equals(routingDeviceUuid)) {
                int headHandle2 = sensor.getHandle();
                if (!setHasHeadTracker(ROUTING_DEVICES[0])) {
                    return -1;
                }
                return headHandle2;
            } else if (uuid.equals(UuidUtils.STANDALONE_UUID)) {
                headHandle = sensor.getHandle();
            }
        }
        return headHandle;
    }

    private int getScreenSensorHandle() {
        Sensor screenSensor = this.mSensorManager.getDefaultSensor(11);
        if (screenSensor == null) {
            return -1;
        }
        int screenHandle = screenSensor.getHandle();
        return screenHandle;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void loglogi(String msg) {
        AudioService.sSpatialLogger.loglogi(msg, TAG);
    }

    private static String logloge(String msg) {
        AudioService.sSpatialLogger.loglog(msg, 1, TAG);
        return msg;
    }

    void clearSADevices() {
        this.mSADevices.clear();
    }
}
