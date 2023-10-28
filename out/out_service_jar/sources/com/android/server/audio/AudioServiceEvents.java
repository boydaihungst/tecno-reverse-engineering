package com.android.server.audio;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.media.MediaMetrics;
import android.net.INetd;
import com.android.server.audio.AudioDeviceInventory;
import com.android.server.audio.AudioEventLogger;
import com.android.server.pm.verify.domain.DomainVerificationPersistence;
/* loaded from: classes.dex */
public class AudioServiceEvents {

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class PhoneStateEvent extends AudioEventLogger.Event {
        static final int MODE_IN_COMMUNICATION_TIMEOUT = 1;
        static final int MODE_SET = 0;
        private static final String mMetricsId = "audio.mode";
        final int mActualMode;
        final int mOp;
        final int mOwnerPid;
        final String mPackage;
        final int mRequestedMode;
        final int mRequesterPid;

        /* JADX INFO: Access modifiers changed from: package-private */
        public PhoneStateEvent(String callingPackage, int requesterPid, int requestedMode, int ownerPid, int actualMode) {
            this.mOp = 0;
            this.mPackage = callingPackage;
            this.mRequesterPid = requesterPid;
            this.mRequestedMode = requestedMode;
            this.mOwnerPid = ownerPid;
            this.mActualMode = actualMode;
            logMetricEvent();
        }

        PhoneStateEvent(String callingPackage, int ownerPid) {
            this.mOp = 1;
            this.mPackage = callingPackage;
            this.mOwnerPid = ownerPid;
            this.mRequesterPid = 0;
            this.mRequestedMode = 0;
            this.mActualMode = 0;
            logMetricEvent();
        }

        @Override // com.android.server.audio.AudioEventLogger.Event
        public String eventToString() {
            switch (this.mOp) {
                case 0:
                    return "setMode(" + AudioSystem.modeToString(this.mRequestedMode) + ") from package=" + this.mPackage + " pid=" + this.mRequesterPid + " selected mode=" + AudioSystem.modeToString(this.mActualMode) + " by pid=" + this.mOwnerPid;
                case 1:
                    return "mode IN COMMUNICATION timeout for package=" + this.mPackage + " pid=" + this.mOwnerPid;
                default:
                    return "FIXME invalid op:" + this.mOp;
            }
        }

        private void logMetricEvent() {
            switch (this.mOp) {
                case 0:
                    new MediaMetrics.Item(mMetricsId).set(MediaMetrics.Property.EVENT, "set").set(MediaMetrics.Property.REQUESTED_MODE, AudioSystem.modeToString(this.mRequestedMode)).set(MediaMetrics.Property.MODE, AudioSystem.modeToString(this.mActualMode)).set(MediaMetrics.Property.CALLING_PACKAGE, this.mPackage).record();
                    return;
                case 1:
                    new MediaMetrics.Item(mMetricsId).set(MediaMetrics.Property.EVENT, "inCommunicationTimeout").set(MediaMetrics.Property.CALLING_PACKAGE, this.mPackage).record();
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class WiredDevConnectEvent extends AudioEventLogger.Event {
        final AudioDeviceInventory.WiredDeviceConnectionState mState;

        /* JADX INFO: Access modifiers changed from: package-private */
        public WiredDevConnectEvent(AudioDeviceInventory.WiredDeviceConnectionState state) {
            this.mState = state;
        }

        @Override // com.android.server.audio.AudioEventLogger.Event
        public String eventToString() {
            return "setWiredDeviceConnectionState( type:" + Integer.toHexString(this.mState.mAttributes.getInternalType()) + " state:" + AudioSystem.deviceStateToString(this.mState.mState) + " addr:" + this.mState.mAttributes.getAddress() + " name:" + this.mState.mAttributes.getName() + ") from " + this.mState.mCaller;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class ForceUseEvent extends AudioEventLogger.Event {
        final int mConfig;
        final String mReason;
        final int mUsage;

        /* JADX INFO: Access modifiers changed from: package-private */
        public ForceUseEvent(int usage, int config, String reason) {
            this.mUsage = usage;
            this.mConfig = config;
            this.mReason = reason;
        }

        @Override // com.android.server.audio.AudioEventLogger.Event
        public String eventToString() {
            return "setForceUse(" + AudioSystem.forceUseUsageToString(this.mUsage) + ", " + AudioSystem.forceUseConfigToString(this.mConfig) + ") due to " + this.mReason;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class VolumeEvent extends AudioEventLogger.Event {
        static final int VOL_ADJUST_STREAM_VOL = 1;
        static final int VOL_ADJUST_SUGG_VOL = 0;
        static final int VOL_ADJUST_VOL_UID = 5;
        static final int VOL_MODE_CHANGE_HEARING_AID = 7;
        static final int VOL_MUTE_STREAM_INT = 9;
        static final int VOL_SET_AVRCP_VOL = 4;
        static final int VOL_SET_GROUP_VOL = 8;
        static final int VOL_SET_HEARING_AID_VOL = 3;
        static final int VOL_SET_LE_AUDIO_VOL = 10;
        static final int VOL_SET_STREAM_VOL = 2;
        static final int VOL_VOICE_ACTIVITY_HEARING_AID = 6;
        private static final String mMetricsId = "audio.volume.event";
        final AudioAttributes mAudioAttributes;
        final String mCaller;
        final String mGroupName;
        final int mOp;
        final int mStream;
        final int mVal1;
        final int mVal2;

        /* JADX INFO: Access modifiers changed from: package-private */
        public VolumeEvent(int op, int stream, int val1, int val2, String caller) {
            this.mOp = op;
            this.mStream = stream;
            this.mVal1 = val1;
            this.mVal2 = val2;
            this.mCaller = caller;
            this.mGroupName = null;
            this.mAudioAttributes = null;
            logMetricEvent();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public VolumeEvent(int op, int index, int gainDb) {
            this.mOp = op;
            this.mVal1 = index;
            this.mVal2 = gainDb;
            this.mStream = -1;
            this.mCaller = null;
            this.mGroupName = null;
            this.mAudioAttributes = null;
            logMetricEvent();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public VolumeEvent(int op, int index) {
            this.mOp = op;
            this.mVal1 = index;
            this.mVal2 = 0;
            this.mStream = -1;
            this.mCaller = null;
            this.mGroupName = null;
            this.mAudioAttributes = null;
            logMetricEvent();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public VolumeEvent(int op, boolean voiceActive, int stream, int index) {
            this.mOp = op;
            this.mStream = stream;
            this.mVal1 = index;
            this.mVal2 = voiceActive ? 1 : 0;
            this.mCaller = null;
            this.mGroupName = null;
            this.mAudioAttributes = null;
            logMetricEvent();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public VolumeEvent(int op, int mode, int stream, int index) {
            this.mOp = op;
            this.mStream = stream;
            this.mVal1 = index;
            this.mVal2 = mode;
            this.mCaller = null;
            this.mGroupName = null;
            this.mAudioAttributes = null;
            logMetricEvent();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public VolumeEvent(int op, AudioAttributes aa, String group, int index, int flags, String caller) {
            this.mOp = op;
            this.mStream = -1;
            this.mVal1 = index;
            this.mVal2 = flags;
            this.mCaller = caller;
            this.mGroupName = group;
            this.mAudioAttributes = aa;
            logMetricEvent();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public VolumeEvent(int op, int stream, boolean state) {
            this.mOp = op;
            this.mStream = stream;
            this.mVal1 = state ? 1 : 0;
            this.mVal2 = 0;
            this.mCaller = null;
            this.mGroupName = null;
            this.mAudioAttributes = null;
            logMetricEvent();
        }

        private void logMetricEvent() {
            String eventName;
            int i = this.mOp;
            switch (i) {
                case 0:
                case 1:
                case 5:
                    switch (i) {
                        case 0:
                            eventName = "adjustSuggestedStreamVolume";
                            break;
                        case 1:
                            eventName = "adjustStreamVolume";
                            break;
                        case 5:
                            eventName = "adjustStreamVolumeForUid";
                            break;
                        default:
                            return;
                    }
                    new MediaMetrics.Item(mMetricsId).set(MediaMetrics.Property.CALLING_PACKAGE, this.mCaller).set(MediaMetrics.Property.DIRECTION, this.mVal1 > 0 ? INetd.IF_STATE_UP : INetd.IF_STATE_DOWN).set(MediaMetrics.Property.EVENT, eventName).set(MediaMetrics.Property.FLAGS, Integer.valueOf(this.mVal2)).set(MediaMetrics.Property.STREAM_TYPE, AudioSystem.streamToString(this.mStream)).record();
                    return;
                case 2:
                    new MediaMetrics.Item(mMetricsId).set(MediaMetrics.Property.CALLING_PACKAGE, this.mCaller).set(MediaMetrics.Property.EVENT, "setStreamVolume").set(MediaMetrics.Property.FLAGS, Integer.valueOf(this.mVal2)).set(MediaMetrics.Property.INDEX, Integer.valueOf(this.mVal1)).set(MediaMetrics.Property.STREAM_TYPE, AudioSystem.streamToString(this.mStream)).record();
                    return;
                case 3:
                    new MediaMetrics.Item(mMetricsId).set(MediaMetrics.Property.EVENT, "setHearingAidVolume").set(MediaMetrics.Property.GAIN_DB, Double.valueOf(this.mVal2)).set(MediaMetrics.Property.INDEX, Integer.valueOf(this.mVal1)).record();
                    return;
                case 4:
                    new MediaMetrics.Item(mMetricsId).set(MediaMetrics.Property.EVENT, "setAvrcpVolume").set(MediaMetrics.Property.INDEX, Integer.valueOf(this.mVal1)).record();
                    return;
                case 6:
                    new MediaMetrics.Item(mMetricsId).set(MediaMetrics.Property.EVENT, "voiceActivityHearingAid").set(MediaMetrics.Property.INDEX, Integer.valueOf(this.mVal1)).set(MediaMetrics.Property.STATE, this.mVal2 == 1 ? DomainVerificationPersistence.TAG_ACTIVE : "inactive").set(MediaMetrics.Property.STREAM_TYPE, AudioSystem.streamToString(this.mStream)).record();
                    return;
                case 7:
                    new MediaMetrics.Item(mMetricsId).set(MediaMetrics.Property.EVENT, "modeChangeHearingAid").set(MediaMetrics.Property.INDEX, Integer.valueOf(this.mVal1)).set(MediaMetrics.Property.MODE, AudioSystem.modeToString(this.mVal2)).set(MediaMetrics.Property.STREAM_TYPE, AudioSystem.streamToString(this.mStream)).record();
                    return;
                case 8:
                    new MediaMetrics.Item(mMetricsId).set(MediaMetrics.Property.ATTRIBUTES, this.mAudioAttributes.toString()).set(MediaMetrics.Property.CALLING_PACKAGE, this.mCaller).set(MediaMetrics.Property.EVENT, "setVolumeIndexForAttributes").set(MediaMetrics.Property.FLAGS, Integer.valueOf(this.mVal2)).set(MediaMetrics.Property.GROUP, this.mGroupName).set(MediaMetrics.Property.INDEX, Integer.valueOf(this.mVal1)).record();
                    return;
                case 9:
                    return;
                case 10:
                    new MediaMetrics.Item(mMetricsId).set(MediaMetrics.Property.EVENT, "setLeAudioVolume").set(MediaMetrics.Property.INDEX, Integer.valueOf(this.mVal1)).set(MediaMetrics.Property.MAX_INDEX, Integer.valueOf(this.mVal2)).record();
                    return;
                default:
                    return;
            }
        }

        @Override // com.android.server.audio.AudioEventLogger.Event
        public String eventToString() {
            switch (this.mOp) {
                case 0:
                    return "adjustSuggestedStreamVolume(sugg:" + AudioSystem.streamToString(this.mStream) + " dir:" + AudioManager.adjustToString(this.mVal1) + " flags:0x" + Integer.toHexString(this.mVal2) + ") from " + this.mCaller;
                case 1:
                    return "adjustStreamVolume(stream:" + AudioSystem.streamToString(this.mStream) + " dir:" + AudioManager.adjustToString(this.mVal1) + " flags:0x" + Integer.toHexString(this.mVal2) + ") from " + this.mCaller;
                case 2:
                    return "setStreamVolume(stream:" + AudioSystem.streamToString(this.mStream) + " index:" + this.mVal1 + " flags:0x" + Integer.toHexString(this.mVal2) + ") from " + this.mCaller;
                case 3:
                    return "setHearingAidVolume: index:" + this.mVal1 + " gain dB:" + this.mVal2;
                case 4:
                    return "setAvrcpVolume: index:" + this.mVal1;
                case 5:
                    return "adjustStreamVolumeForUid(stream:" + AudioSystem.streamToString(this.mStream) + " dir:" + AudioManager.adjustToString(this.mVal1) + " flags:0x" + Integer.toHexString(this.mVal2) + ") from " + this.mCaller;
                case 6:
                    return "Voice activity change (" + (this.mVal2 == 1 ? DomainVerificationPersistence.TAG_ACTIVE : "inactive") + ") causes setting HEARING_AID volume to idx:" + this.mVal1 + " stream:" + AudioSystem.streamToString(this.mStream);
                case 7:
                    return "setMode(" + AudioSystem.modeToString(this.mVal2) + ") causes setting HEARING_AID volume to idx:" + this.mVal1 + " stream:" + AudioSystem.streamToString(this.mStream);
                case 8:
                    return "setVolumeIndexForAttributes(attr:" + this.mAudioAttributes.toString() + " group: " + this.mGroupName + " index:" + this.mVal1 + " flags:0x" + Integer.toHexString(this.mVal2) + ") from " + this.mCaller;
                case 9:
                    return "VolumeStreamState.muteInternally(stream:" + AudioSystem.streamToString(this.mStream) + (this.mVal1 == 1 ? ", muted)" : ", unmuted)");
                case 10:
                    return "setLeAudioVolume: index:" + this.mVal1 + " gain dB:" + this.mVal2;
                default:
                    return "FIXME invalid op:" + this.mOp;
            }
        }
    }
}
