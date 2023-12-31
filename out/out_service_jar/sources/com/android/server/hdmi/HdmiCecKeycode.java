package com.android.server.hdmi;

import com.android.internal.util.FrameworkStatsLog;
import java.util.Arrays;
import libcore.util.EmptyArray;
/* loaded from: classes.dex */
final class HdmiCecKeycode {
    public static final int CEC_KEYCODE_ANGLE = 80;
    public static final int CEC_KEYCODE_BACKWARD = 76;
    public static final int CEC_KEYCODE_CHANNEL_DOWN = 49;
    public static final int CEC_KEYCODE_CHANNEL_UP = 48;
    public static final int CEC_KEYCODE_CLEAR = 44;
    public static final int CEC_KEYCODE_CONTENTS_MENU = 11;
    public static final int CEC_KEYCODE_DATA = 118;
    public static final int CEC_KEYCODE_DISPLAY_INFORMATION = 53;
    public static final int CEC_KEYCODE_DOT = 42;
    public static final int CEC_KEYCODE_DOWN = 2;
    public static final int CEC_KEYCODE_EJECT = 74;
    public static final int CEC_KEYCODE_ELECTRONIC_PROGRAM_GUIDE = 83;
    public static final int CEC_KEYCODE_ENTER = 43;
    public static final int CEC_KEYCODE_EXIT = 13;
    public static final int CEC_KEYCODE_F1_BLUE = 113;
    public static final int CEC_KEYCODE_F2_RED = 114;
    public static final int CEC_KEYCODE_F3_GREEN = 115;
    public static final int CEC_KEYCODE_F4_YELLOW = 116;
    public static final int CEC_KEYCODE_F5 = 117;
    public static final int CEC_KEYCODE_FAST_FORWARD = 73;
    public static final int CEC_KEYCODE_FAVORITE_MENU = 12;
    public static final int CEC_KEYCODE_FORWARD = 75;
    public static final int CEC_KEYCODE_HELP = 54;
    public static final int CEC_KEYCODE_INITIAL_CONFIGURATION = 85;
    public static final int CEC_KEYCODE_INPUT_SELECT = 52;
    public static final int CEC_KEYCODE_LEFT = 3;
    public static final int CEC_KEYCODE_LEFT_DOWN = 8;
    public static final int CEC_KEYCODE_LEFT_UP = 7;
    public static final int CEC_KEYCODE_MEDIA_CONTEXT_SENSITIVE_MENU = 17;
    public static final int CEC_KEYCODE_MEDIA_TOP_MENU = 16;
    public static final int CEC_KEYCODE_MUTE = 67;
    public static final int CEC_KEYCODE_MUTE_FUNCTION = 101;
    public static final int CEC_KEYCODE_NEXT_FAVORITE = 47;
    public static final int CEC_KEYCODE_NUMBERS_1 = 33;
    public static final int CEC_KEYCODE_NUMBERS_2 = 34;
    public static final int CEC_KEYCODE_NUMBERS_3 = 35;
    public static final int CEC_KEYCODE_NUMBERS_4 = 36;
    public static final int CEC_KEYCODE_NUMBERS_5 = 37;
    public static final int CEC_KEYCODE_NUMBERS_6 = 38;
    public static final int CEC_KEYCODE_NUMBERS_7 = 39;
    public static final int CEC_KEYCODE_NUMBERS_8 = 40;
    public static final int CEC_KEYCODE_NUMBERS_9 = 41;
    public static final int CEC_KEYCODE_NUMBER_0_OR_NUMBER_10 = 32;
    public static final int CEC_KEYCODE_NUMBER_11 = 30;
    public static final int CEC_KEYCODE_NUMBER_12 = 31;
    public static final int CEC_KEYCODE_NUMBER_ENTRY_MODE = 29;
    public static final int CEC_KEYCODE_PAGE_DOWN = 56;
    public static final int CEC_KEYCODE_PAGE_UP = 55;
    public static final int CEC_KEYCODE_PAUSE = 70;
    public static final int CEC_KEYCODE_PAUSE_PLAY_FUNCTION = 97;
    public static final int CEC_KEYCODE_PAUSE_RECORD = 78;
    public static final int CEC_KEYCODE_PAUSE_RECORD_FUNCTION = 99;
    public static final int CEC_KEYCODE_PLAY = 68;
    public static final int CEC_KEYCODE_PLAY_FUNCTION = 96;
    public static final int CEC_KEYCODE_POWER = 64;
    public static final int CEC_KEYCODE_POWER_OFF_FUNCTION = 108;
    public static final int CEC_KEYCODE_POWER_ON_FUNCTION = 109;
    public static final int CEC_KEYCODE_POWER_TOGGLE_FUNCTION = 107;
    public static final int CEC_KEYCODE_PREVIOUS_CHANNEL = 50;
    public static final int CEC_KEYCODE_RECORD = 71;
    public static final int CEC_KEYCODE_RECORD_FUNCTION = 98;
    public static final int CEC_KEYCODE_RESERVED = 79;
    public static final int CEC_KEYCODE_RESTORE_VOLUME_FUNCTION = 102;
    public static final int CEC_KEYCODE_REWIND = 72;
    public static final int CEC_KEYCODE_RIGHT = 4;
    public static final int CEC_KEYCODE_RIGHT_DOWN = 6;
    public static final int CEC_KEYCODE_RIGHT_UP = 5;
    public static final int CEC_KEYCODE_ROOT_MENU = 9;
    public static final int CEC_KEYCODE_SELECT = 0;
    public static final int CEC_KEYCODE_SELECT_AUDIO_INPUT_FUNCTION = 106;
    public static final int CEC_KEYCODE_SELECT_AV_INPUT_FUNCTION = 105;
    public static final int CEC_KEYCODE_SELECT_BROADCAST_TYPE = 86;
    public static final int CEC_KEYCODE_SELECT_MEDIA_FUNCTION = 104;
    public static final int CEC_KEYCODE_SELECT_SOUND_PRESENTATION = 87;
    public static final int CEC_KEYCODE_SETUP_MENU = 10;
    public static final int CEC_KEYCODE_SOUND_SELECT = 51;
    public static final int CEC_KEYCODE_STOP = 69;
    public static final int CEC_KEYCODE_STOP_FUNCTION = 100;
    public static final int CEC_KEYCODE_STOP_RECORD = 77;
    public static final int CEC_KEYCODE_SUB_PICTURE = 81;
    public static final int CEC_KEYCODE_TIMER_PROGRAMMING = 84;
    public static final int CEC_KEYCODE_TUNE_FUNCTION = 103;
    public static final int CEC_KEYCODE_UP = 1;
    public static final int CEC_KEYCODE_VIDEO_ON_DEMAND = 82;
    public static final int CEC_KEYCODE_VOLUME_DOWN = 66;
    public static final int CEC_KEYCODE_VOLUME_UP = 65;
    private static final KeycodeEntry[] KEYCODE_ENTRIES = {new KeycodeEntry(23, 0), new KeycodeEntry(19, 1), new KeycodeEntry(20, 2), new KeycodeEntry(21, 3), new KeycodeEntry(22, 4), new KeycodeEntry(-1, 5), new KeycodeEntry(-1, 6), new KeycodeEntry(-1, 7), new KeycodeEntry(-1, 8), new KeycodeEntry(3, 9), new KeycodeEntry(82, 9), new KeycodeEntry((int) FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__DOCSUI_PICK_RESULT, 10), new KeycodeEntry(256, 11, false), new KeycodeEntry(-1, 12), new KeycodeEntry(4, 13), new KeycodeEntry(111, 13), new KeycodeEntry(226, 16), new KeycodeEntry(257, 17), new KeycodeEntry(234, 29), new KeycodeEntry((int) FrameworkStatsLog.CAMERA_ACTION_EVENT, 30), new KeycodeEntry((int) FrameworkStatsLog.APP_COMPATIBILITY_CHANGE_REPORTED, 31), new KeycodeEntry(7, 32), new KeycodeEntry(8, 33), new KeycodeEntry(9, 34), new KeycodeEntry(10, 35), new KeycodeEntry(11, 36), new KeycodeEntry(12, 37), new KeycodeEntry(13, 38), new KeycodeEntry(14, 39), new KeycodeEntry(15, 40), new KeycodeEntry(16, 41), new KeycodeEntry(56, 42), new KeycodeEntry(160, 43), new KeycodeEntry(28, 44), new KeycodeEntry(-1, 47), new KeycodeEntry(166, 48), new KeycodeEntry(167, 49), new KeycodeEntry(229, 50), new KeycodeEntry(222, 51), new KeycodeEntry(178, 52), new KeycodeEntry(165, 53), new KeycodeEntry(-1, 54), new KeycodeEntry(92, 55), new KeycodeEntry(93, 56), new KeycodeEntry(26, 64, false), new KeycodeEntry(24, 65), new KeycodeEntry(25, 66), new KeycodeEntry(164, 67, false), new KeycodeEntry(126, 68), new KeycodeEntry(86, 69), new KeycodeEntry((int) FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_AUTO_TIME, 70), new KeycodeEntry(85, 70), new KeycodeEntry(130, 71), new KeycodeEntry(89, 72), new KeycodeEntry(90, 73), new KeycodeEntry(129, 74), new KeycodeEntry(87, 75), new KeycodeEntry(88, 76), new KeycodeEntry(-1, 77), new KeycodeEntry(-1, 78), new KeycodeEntry(-1, 79), new KeycodeEntry(-1, 80), new KeycodeEntry((int) FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__DOCSUI_LAUNCH_OTHER_APP, 81), new KeycodeEntry(-1, 82), new KeycodeEntry((int) FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__CROSS_PROFILE_SETTINGS_PAGE_PERMISSION_REVOKED, 83), new KeycodeEntry(258, 84), new KeycodeEntry(-1, 85), new KeycodeEntry(-1, 86), new KeycodeEntry(235, 86, true, intToSingleByteArray(16)), new KeycodeEntry(236, 86, true, intToSingleByteArray(96)), new KeycodeEntry(FrameworkStatsLog.REBOOT_ESCROW_RECOVERY_REPORTED, 86, true, intToSingleByteArray(128)), new KeycodeEntry(239, 86, true, intToSingleByteArray(144)), new KeycodeEntry(241, 86, true, intToSingleByteArray(1)), new KeycodeEntry(-1, 87), new KeycodeEntry(-1, 96, false), new KeycodeEntry(-1, 97, false), new KeycodeEntry(-1, 98, false), new KeycodeEntry(-1, 99, false), new KeycodeEntry(-1, 100, false), new KeycodeEntry(-1, 101, false), new KeycodeEntry(-1, 102, false), new KeycodeEntry(-1, 103, false), new KeycodeEntry(-1, 104, false), new KeycodeEntry(-1, 105, false), new KeycodeEntry(-1, 106, false), new KeycodeEntry(-1, 107, false), new KeycodeEntry(-1, 108, false), new KeycodeEntry(-1, 109, false), new KeycodeEntry(186, 113), new KeycodeEntry(183, 114), new KeycodeEntry(184, 115), new KeycodeEntry(185, 116), new KeycodeEntry(135, 117), new KeycodeEntry((int) FrameworkStatsLog.APP_PROCESS_DIED__IMPORTANCE__IMPORTANCE_PERCEPTIBLE, 118)};
    public static final int NO_PARAM = -1;
    public static final int UI_BROADCAST_ANALOGUE = 16;
    public static final int UI_BROADCAST_ANALOGUE_CABLE = 48;
    public static final int UI_BROADCAST_ANALOGUE_SATELLITE = 64;
    public static final int UI_BROADCAST_ANALOGUE_TERRESTRIAL = 32;
    public static final int UI_BROADCAST_DIGITAL = 80;
    public static final int UI_BROADCAST_DIGITAL_CABLE = 112;
    public static final int UI_BROADCAST_DIGITAL_COMMNICATIONS_SATELLITE = 144;
    public static final int UI_BROADCAST_DIGITAL_COMMNICATIONS_SATELLITE_2 = 145;
    public static final int UI_BROADCAST_DIGITAL_SATELLITE = 128;
    public static final int UI_BROADCAST_DIGITAL_TERRESTRIAL = 96;
    public static final int UI_BROADCAST_IP = 160;
    public static final int UI_BROADCAST_TOGGLE_ALL = 0;
    public static final int UI_BROADCAST_TOGGLE_ANALOGUE_DIGITAL = 1;
    public static final int UI_SOUND_PRESENTATION_BASS_NEUTRAL = 178;
    public static final int UI_SOUND_PRESENTATION_BASS_STEP_MINUS = 179;
    public static final int UI_SOUND_PRESENTATION_BASS_STEP_PLUS = 177;
    public static final int UI_SOUND_PRESENTATION_SELECT_AUDIO_AUTO_EQUALIZER = 160;
    public static final int UI_SOUND_PRESENTATION_SELECT_AUDIO_AUTO_REVERBERATION = 144;
    public static final int UI_SOUND_PRESENTATION_SELECT_AUDIO_DOWN_MIX = 128;
    public static final int UI_SOUND_PRESENTATION_SOUND_MIX_DUAL_MONO = 32;
    public static final int UI_SOUND_PRESENTATION_SOUND_MIX_KARAOKE = 48;
    public static final int UI_SOUND_PRESENTATION_TREBLE_NEUTRAL = 194;
    public static final int UI_SOUND_PRESENTATION_TREBLE_STEP_MINUS = 195;
    public static final int UI_SOUND_PRESENTATION_TREBLE_STEP_PLUS = 193;
    public static final int UNSUPPORTED_KEYCODE = -1;

    private HdmiCecKeycode() {
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class KeycodeEntry {
        private final int mAndroidKeycode;
        private final byte[] mCecKeycodeAndParams;
        private final boolean mIsRepeatable;

        private KeycodeEntry(int androidKeycode, int cecKeycode, boolean isRepeatable, byte[] cecParams) {
            this.mAndroidKeycode = androidKeycode;
            this.mIsRepeatable = isRepeatable;
            byte[] bArr = new byte[cecParams.length + 1];
            this.mCecKeycodeAndParams = bArr;
            System.arraycopy(cecParams, 0, bArr, 1, cecParams.length);
            bArr[0] = (byte) (cecKeycode & 255);
        }

        private KeycodeEntry(int androidKeycode, int cecKeycode, boolean isRepeatable) {
            this(androidKeycode, cecKeycode, isRepeatable, EmptyArray.BYTE);
        }

        private KeycodeEntry(int androidKeycode, int cecKeycode, byte[] cecParams) {
            this(androidKeycode, cecKeycode, true, cecParams);
        }

        private KeycodeEntry(int androidKeycode, int cecKeycode) {
            this(androidKeycode, cecKeycode, true, EmptyArray.BYTE);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public byte[] toCecKeycodeAndParamIfMatched(int androidKeycode) {
            if (this.mAndroidKeycode == androidKeycode) {
                return this.mCecKeycodeAndParams;
            }
            return null;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public int toAndroidKeycodeIfMatched(byte[] cecKeycodeAndParams) {
            if (Arrays.equals(this.mCecKeycodeAndParams, cecKeycodeAndParams)) {
                return this.mAndroidKeycode;
            }
            return -1;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Boolean isRepeatableIfMatched(int androidKeycode) {
            if (this.mAndroidKeycode == androidKeycode) {
                return Boolean.valueOf(this.mIsRepeatable);
            }
            return null;
        }
    }

    private static byte[] intToSingleByteArray(int value) {
        return new byte[]{(byte) (value & 255)};
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static byte[] androidKeyToCecKey(int keycode) {
        int i = 0;
        while (true) {
            KeycodeEntry[] keycodeEntryArr = KEYCODE_ENTRIES;
            if (i < keycodeEntryArr.length) {
                byte[] cecKeycodeAndParams = keycodeEntryArr[i].toCecKeycodeAndParamIfMatched(keycode);
                if (cecKeycodeAndParams == null) {
                    i++;
                } else {
                    return cecKeycodeAndParams;
                }
            } else {
                return null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int cecKeycodeAndParamsToAndroidKey(byte[] cecKeycodeAndParams) {
        int i = 0;
        while (true) {
            KeycodeEntry[] keycodeEntryArr = KEYCODE_ENTRIES;
            if (i >= keycodeEntryArr.length) {
                return -1;
            }
            int androidKey = keycodeEntryArr[i].toAndroidKeycodeIfMatched(cecKeycodeAndParams);
            if (androidKey == -1) {
                i++;
            } else {
                return androidKey;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isRepeatableKey(int androidKeycode) {
        int i = 0;
        while (true) {
            KeycodeEntry[] keycodeEntryArr = KEYCODE_ENTRIES;
            if (i < keycodeEntryArr.length) {
                Boolean isRepeatable = keycodeEntryArr[i].isRepeatableIfMatched(androidKeycode);
                if (isRepeatable == null) {
                    i++;
                } else {
                    return isRepeatable.booleanValue();
                }
            } else {
                return false;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isSupportedKeycode(int androidKeycode) {
        return androidKeyToCecKey(androidKeycode) != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isVolumeKeycode(int androidKeycode) {
        int cecKeyCode = androidKeyToCecKey(androidKeycode)[0];
        if (isSupportedKeycode(androidKeycode)) {
            return cecKeyCode == 65 || cecKeyCode == 66 || cecKeyCode == 67 || cecKeyCode == 101 || cecKeyCode == 102;
        }
        return false;
    }

    public static int getMuteKey(boolean muting) {
        return 67;
    }

    public static String getKeycodeType(byte keycode) {
        switch (keycode) {
            case 0:
            case 51:
            case 52:
            case 86:
            case 87:
                return "Select";
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 13:
            case 55:
            case 56:
                return "Navigation";
            case 9:
            case 10:
            case 11:
            case 12:
            case 16:
            case 17:
                return "Menu";
            case 14:
            case 15:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 45:
            case 46:
            case 57:
            case 58:
            case 59:
            case 60:
            case 61:
            case 62:
            case 63:
            case 79:
            case 88:
            case 89:
            case 90:
            case 91:
            case 92:
            case 93:
            case 94:
            case 95:
            case 110:
            case 111:
            case 112:
            default:
                return "Unknown";
            case 29:
            case 42:
            case 43:
            case 44:
            case 53:
            case 54:
            case 85:
            case 118:
                return "General";
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37:
            case 38:
            case 39:
            case 40:
            case 41:
                return "Number";
            case 47:
            case 48:
            case 49:
            case 50:
                return "Channel";
            case 64:
                return "Power";
            case 65:
                return "Volume up";
            case 66:
                return "Volume down";
            case 67:
                return "Volume mute";
            case 68:
            case 69:
            case 70:
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77:
            case 78:
            case 80:
            case 81:
            case 82:
                return "Media";
            case 83:
            case 84:
                return "Timer";
            case 96:
            case 97:
            case 98:
            case 99:
            case 100:
            case 101:
            case 102:
            case 103:
            case 104:
            case 105:
            case 106:
                return "Functional";
            case 107:
                return "Power toggle";
            case 108:
                return "Power off";
            case 109:
                return "Power on";
            case 113:
            case 114:
            case 115:
            case 116:
            case 117:
                return "Function key";
        }
    }
}
