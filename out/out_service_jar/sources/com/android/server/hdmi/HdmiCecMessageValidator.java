package com.android.server.hdmi;

import android.util.SparseArray;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.location.gnss.hal.GnssNative;
/* loaded from: classes.dex */
public class HdmiCecMessageValidator {
    public static final int DEST_ALL = 3;
    public static final int DEST_BROADCAST = 2;
    public static final int DEST_DIRECT = 1;
    static final int ERROR_DESTINATION = 2;
    static final int ERROR_PARAMETER = 3;
    static final int ERROR_PARAMETER_SHORT = 4;
    static final int ERROR_SOURCE = 1;
    static final int OK = 0;
    public static final int SRC_UNREGISTERED = 4;
    private static final String TAG = "HdmiCecMessageValidator";
    private static final SparseArray<ValidationInfo> sValidationInfo = new SparseArray<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface ParameterValidator {
        int isValid(byte[] bArr);
    }

    /* loaded from: classes.dex */
    public @interface ValidationResult {
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ValidationInfo {
        public final int addressType;
        public final ParameterValidator parameterValidator;

        public ValidationInfo(ParameterValidator validator, int type) {
            this.parameterValidator = validator;
            this.addressType = type;
        }
    }

    private HdmiCecMessageValidator() {
    }

    static {
        PhysicalAddressValidator physicalAddressValidator = new PhysicalAddressValidator();
        addValidationInfo(130, physicalAddressValidator, 6);
        addValidationInfo(157, physicalAddressValidator, 1);
        addValidationInfo(132, new ReportPhysicalAddressValidator(), 6);
        addValidationInfo(128, new RoutingChangeValidator(), 6);
        addValidationInfo(129, physicalAddressValidator, 6);
        addValidationInfo(134, physicalAddressValidator, 2);
        addValidationInfo(112, new SystemAudioModeRequestValidator(), 1);
        FixedLengthValidator noneValidator = new FixedLengthValidator(0);
        addValidationInfo(255, noneValidator, 1);
        addValidationInfo(159, noneValidator, 1);
        addValidationInfo(145, noneValidator, 5);
        addValidationInfo(113, noneValidator, 1);
        addValidationInfo(143, noneValidator, 1);
        addValidationInfo(140, noneValidator, 5);
        addValidationInfo(70, noneValidator, 1);
        addValidationInfo(131, noneValidator, 5);
        addValidationInfo(125, noneValidator, 1);
        addValidationInfo(4, noneValidator, 1);
        addValidationInfo(192, noneValidator, 1);
        addValidationInfo(11, noneValidator, 1);
        addValidationInfo(15, noneValidator, 1);
        addValidationInfo(193, noneValidator, 1);
        addValidationInfo(194, noneValidator, 1);
        addValidationInfo(195, noneValidator, 1);
        addValidationInfo(196, noneValidator, 1);
        addValidationInfo(133, noneValidator, 6);
        addValidationInfo(54, noneValidator, 7);
        addValidationInfo(197, noneValidator, 1);
        addValidationInfo(13, noneValidator, 1);
        addValidationInfo(6, noneValidator, 1);
        addValidationInfo(5, noneValidator, 1);
        addValidationInfo(69, noneValidator, 1);
        addValidationInfo(139, noneValidator, 3);
        addValidationInfo(9, new VariableLengthValidator(1, 8), 1);
        addValidationInfo(10, new RecordStatusInfoValidator(), 1);
        addValidationInfo(51, new AnalogueTimerValidator(), 1);
        addValidationInfo(153, new DigitalTimerValidator(), 1);
        addValidationInfo(161, new ExternalTimerValidator(), 1);
        addValidationInfo(52, new AnalogueTimerValidator(), 1);
        addValidationInfo(151, new DigitalTimerValidator(), 1);
        addValidationInfo(162, new ExternalTimerValidator(), 1);
        addValidationInfo(103, new AsciiValidator(1, 14), 1);
        addValidationInfo(67, new TimerClearedStatusValidator(), 1);
        addValidationInfo(53, new TimerStatusValidator(), 1);
        FixedLengthValidator oneByteValidator = new FixedLengthValidator(1);
        addValidationInfo(158, oneByteValidator, 1);
        addValidationInfo(50, new AsciiValidator(3), 2);
        ParameterValidator statusRequestValidator = new OneByteRangeValidator(1, 3);
        addValidationInfo(66, new OneByteRangeValidator(1, 4), 1);
        addValidationInfo(27, new OneByteRangeValidator(17, 31), 1);
        addValidationInfo(26, statusRequestValidator, 1);
        addValidationInfo(65, new PlayModeValidator(), 1);
        addValidationInfo(8, statusRequestValidator, 1);
        addValidationInfo(146, new SelectAnalogueServiceValidator(), 1);
        addValidationInfo(147, new SelectDigitalServiceValidator(), 1);
        addValidationInfo(7, new TunerDeviceStatusValidator(), 1);
        VariableLengthValidator maxLengthValidator = new VariableLengthValidator(0, 14);
        addValidationInfo(135, new FixedLengthValidator(3), 2);
        addValidationInfo(137, new VariableLengthValidator(1, 14), 5);
        addValidationInfo(160, new VariableLengthValidator(4, 14), 7);
        addValidationInfo(138, maxLengthValidator, 7);
        addValidationInfo(100, new OsdStringValidator(), 1);
        addValidationInfo(71, new AsciiValidator(1, 14), 1);
        addValidationInfo(141, new OneByteRangeValidator(0, 2), 1);
        addValidationInfo(142, new OneByteRangeValidator(0, 1), 1);
        addValidationInfo(68, new UserControlPressedValidator(), 1);
        addValidationInfo(144, new OneByteRangeValidator(0, 3), 3);
        addValidationInfo(0, new FixedLengthValidator(2), 1);
        addValidationInfo(122, oneByteValidator, 1);
        addValidationInfo(163, new FixedLengthValidator(3), 1);
        addValidationInfo(164, oneByteValidator, 1);
        addValidationInfo(114, new OneByteRangeValidator(0, 1), 3);
        addValidationInfo(126, new OneByteRangeValidator(0, 1), 1);
        addValidationInfo(154, new OneByteRangeValidator(0, 6), 1);
        addValidationInfo(165, noneValidator, 5);
        addValidationInfo(167, physicalAddressValidator, 2);
        addValidationInfo(168, new VariableLengthValidator(4, 14), 2);
        addValidationInfo(FrameworkStatsLog.INTEGRITY_RULES_PUSHED, maxLengthValidator, 6);
    }

    private static void addValidationInfo(int opcode, ParameterValidator validator, int addrType) {
        sValidationInfo.append(opcode, new ValidationInfo(validator, addrType));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int validate(int source, int destination, int opcode, byte[] params) {
        ValidationInfo info = sValidationInfo.get(opcode);
        if (info == null) {
            HdmiLogger.warning("No validation information for the opcode: " + opcode, new Object[0]);
            return 0;
        }
        int addressValidationResult = validateAddress(source, destination, info.addressType);
        if (addressValidationResult != 0) {
            return addressValidationResult;
        }
        int errorCode = info.parameterValidator.isValid(params);
        if (errorCode == 0) {
            return 0;
        }
        return errorCode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int validateAddress(int source, int destination, int addressType) {
        if (source != 15 || (addressType & 4) != 0) {
            if (destination == 15) {
                if ((addressType & 2) == 0) {
                    return 2;
                }
                return 0;
            } else if ((addressType & 1) == 0) {
                return 2;
            } else {
                return 0;
            }
        }
        return 1;
    }

    /* loaded from: classes.dex */
    private static class FixedLengthValidator implements ParameterValidator {
        private final int mLength;

        public FixedLengthValidator(int length) {
            this.mLength = length;
        }

        @Override // com.android.server.hdmi.HdmiCecMessageValidator.ParameterValidator
        public int isValid(byte[] params) {
            return params.length < this.mLength ? 4 : 0;
        }
    }

    /* loaded from: classes.dex */
    private static class VariableLengthValidator implements ParameterValidator {
        private final int mMaxLength;
        private final int mMinLength;

        public VariableLengthValidator(int minLength, int maxLength) {
            this.mMinLength = minLength;
            this.mMaxLength = maxLength;
        }

        @Override // com.android.server.hdmi.HdmiCecMessageValidator.ParameterValidator
        public int isValid(byte[] params) {
            return params.length < this.mMinLength ? 4 : 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isValidPhysicalAddress(byte[] params, int offset) {
        int physicalAddress = HdmiUtils.twoBytesToInt(params, offset);
        while (physicalAddress != 0) {
            int maskedAddress = 61440 & physicalAddress;
            physicalAddress = (physicalAddress << 4) & GnssNative.GNSS_AIDING_TYPE_ALL;
            if (maskedAddress == 0 && physicalAddress != 0) {
                return false;
            }
        }
        return true;
    }

    static boolean isValidType(int type) {
        return type >= 0 && type <= 7 && type != 2;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int toErrorCode(boolean success) {
        return success ? 0 : 3;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isWithinRange(int value, int min, int max) {
        int value2 = value & 255;
        return value2 >= min && value2 <= max;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isValidDisplayControl(int value) {
        int value2 = value & 255;
        return value2 == 0 || value2 == 64 || value2 == 128 || value2 == 192;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isValidAsciiString(byte[] params, int offset, int maxLength) {
        for (int i = offset; i < params.length && i < maxLength; i++) {
            if (!isWithinRange(params[i], 32, 126)) {
                return false;
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isValidDayOfMonth(int value) {
        return isWithinRange(value, 1, 31);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isValidMonthOfYear(int value) {
        return isWithinRange(value, 1, 12);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isValidHour(int value) {
        return isWithinRange(value, 0, 23);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isValidMinute(int value) {
        return isWithinRange(value, 0, 59);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isValidDurationHours(int value) {
        return isWithinRange(value, 0, 99);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isValidRecordingSequence(int value) {
        int value2 = value & 255;
        return (value2 & 128) == 0 && Integer.bitCount(value2) <= 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isValidAnalogueBroadcastType(int value) {
        return isWithinRange(value, 0, 2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isValidAnalogueFrequency(int value) {
        int value2 = value & GnssNative.GNSS_AIDING_TYPE_ALL;
        return (value2 == 0 || value2 == 65535) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isValidBroadcastSystem(int value) {
        return isWithinRange(value, 0, 31);
    }

    private static boolean isAribDbs(int value) {
        return value == 0 || isWithinRange(value, 8, 10);
    }

    private static boolean isAtscDbs(int value) {
        return value == 1 || isWithinRange(value, 16, 18);
    }

    private static boolean isDvbDbs(int value) {
        return value == 2 || isWithinRange(value, 24, 27);
    }

    private static boolean isValidDigitalBroadcastSystem(int value) {
        return isAribDbs(value) || isAtscDbs(value) || isDvbDbs(value);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isValidChannelIdentifier(byte[] params, int offset) {
        int channelNumberFormat = params[offset] & 252;
        return channelNumberFormat == 4 ? params.length - offset >= 3 : channelNumberFormat == 8 && params.length - offset >= 4;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isValidDigitalServiceIdentification(byte[] params, int offset) {
        int serviceIdentificationMethod = params[offset] & 128;
        int digitalBroadcastSystem = params[offset] & Byte.MAX_VALUE;
        int offset2 = offset + 1;
        if (serviceIdentificationMethod == 0) {
            if (isAribDbs(digitalBroadcastSystem)) {
                if (params.length - offset2 >= 6) {
                    return true;
                }
                return false;
            } else if (isAtscDbs(digitalBroadcastSystem)) {
                if (params.length - offset2 >= 4) {
                    return true;
                }
                return false;
            } else if (isDvbDbs(digitalBroadcastSystem) && params.length - offset2 >= 6) {
                return true;
            } else {
                return false;
            }
        } else if (serviceIdentificationMethod == 128 && isValidDigitalBroadcastSystem(digitalBroadcastSystem)) {
            return isValidChannelIdentifier(params, offset2);
        }
        return false;
    }

    private static boolean isValidExternalPlug(int value) {
        return isWithinRange(value, 1, 255);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isValidExternalSource(byte[] params, int offset) {
        int externalSourceSpecifier = params[offset];
        int offset2 = offset + 1;
        if (externalSourceSpecifier == 4) {
            return isValidExternalPlug(params[offset2]);
        }
        if (externalSourceSpecifier == 5 && params.length - offset2 >= 2) {
            return isValidPhysicalAddress(params, offset2);
        }
        return false;
    }

    private static boolean isValidProgrammedInfo(int programedInfo) {
        return isWithinRange(programedInfo, 0, 11);
    }

    private static boolean isValidNotProgrammedErrorInfo(int nonProgramedErrorInfo) {
        return isWithinRange(nonProgramedErrorInfo, 0, 14);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isValidTimerStatusData(byte[] params, int offset) {
        int programedIndicator = params[offset] & 16;
        boolean durationAvailable = false;
        if (programedIndicator == 16) {
            int programedInfo = params[offset] & 15;
            if (isValidProgrammedInfo(programedInfo)) {
                if (programedInfo != 9 && programedInfo != 11) {
                    return true;
                }
                durationAvailable = true;
            }
        } else {
            int nonProgramedErrorInfo = params[offset] & 15;
            if (isValidNotProgrammedErrorInfo(nonProgramedErrorInfo)) {
                if (nonProgramedErrorInfo != 14) {
                    return true;
                }
                durationAvailable = true;
            }
        }
        int offset2 = offset + 1;
        if (!durationAvailable || params.length - offset2 < 2) {
            return false;
        }
        if (isValidDurationHours(params[offset2]) && isValidMinute(params[offset2 + 1])) {
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isValidPlayMode(int value) {
        return isWithinRange(value, 5, 7) || isWithinRange(value, 9, 11) || isWithinRange(value, 21, 23) || isWithinRange(value, 25, 27) || isWithinRange(value, 36, 37) || value == 32;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isValidUiBroadcastType(int value) {
        return value == 0 || value == 1 || value == 16 || value == 32 || value == 48 || value == 64 || value == 80 || value == 96 || value == 112 || value == 128 || value == 144 || value == 145 || value == 160;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isValidUiSoundPresenationControl(int value) {
        int value2 = value & 255;
        return value2 == 32 || value2 == 48 || value2 == 128 || value2 == 144 || value2 == 160 || isWithinRange(value2, 177, 179) || isWithinRange(value2, 193, 195);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isValidTunerDeviceInfo(byte[] params) {
        int tunerDisplayInfo = params[0] & Byte.MAX_VALUE;
        if (tunerDisplayInfo == 0) {
            if (params.length >= 5) {
                return isValidDigitalServiceIdentification(params, 1);
            }
        } else if (tunerDisplayInfo == 1) {
            return true;
        } else {
            return tunerDisplayInfo == 2 && params.length >= 5 && isValidAnalogueBroadcastType(params[1]) && isValidAnalogueFrequency(HdmiUtils.twoBytesToInt(params, 2)) && isValidBroadcastSystem(params[4]);
        }
        return false;
    }

    /* loaded from: classes.dex */
    private static class PhysicalAddressValidator implements ParameterValidator {
        private PhysicalAddressValidator() {
        }

        @Override // com.android.server.hdmi.HdmiCecMessageValidator.ParameterValidator
        public int isValid(byte[] params) {
            if (params.length < 2) {
                return 4;
            }
            return HdmiCecMessageValidator.toErrorCode(HdmiCecMessageValidator.isValidPhysicalAddress(params, 0));
        }
    }

    /* loaded from: classes.dex */
    private static class SystemAudioModeRequestValidator extends PhysicalAddressValidator {
        private SystemAudioModeRequestValidator() {
            super();
        }

        @Override // com.android.server.hdmi.HdmiCecMessageValidator.PhysicalAddressValidator, com.android.server.hdmi.HdmiCecMessageValidator.ParameterValidator
        public int isValid(byte[] params) {
            if (params.length == 0) {
                return 0;
            }
            return super.isValid(params);
        }
    }

    /* loaded from: classes.dex */
    private static class ReportPhysicalAddressValidator implements ParameterValidator {
        private ReportPhysicalAddressValidator() {
        }

        @Override // com.android.server.hdmi.HdmiCecMessageValidator.ParameterValidator
        public int isValid(byte[] params) {
            if (params.length < 3) {
                return 4;
            }
            boolean z = false;
            if (HdmiCecMessageValidator.isValidPhysicalAddress(params, 0) && HdmiCecMessageValidator.isValidType(params[2])) {
                z = true;
            }
            return HdmiCecMessageValidator.toErrorCode(z);
        }
    }

    /* loaded from: classes.dex */
    private static class RoutingChangeValidator implements ParameterValidator {
        private RoutingChangeValidator() {
        }

        @Override // com.android.server.hdmi.HdmiCecMessageValidator.ParameterValidator
        public int isValid(byte[] params) {
            if (params.length < 4) {
                return 4;
            }
            boolean z = false;
            if (HdmiCecMessageValidator.isValidPhysicalAddress(params, 0) && HdmiCecMessageValidator.isValidPhysicalAddress(params, 2)) {
                z = true;
            }
            return HdmiCecMessageValidator.toErrorCode(z);
        }
    }

    /* loaded from: classes.dex */
    private static class RecordStatusInfoValidator implements ParameterValidator {
        private RecordStatusInfoValidator() {
        }

        @Override // com.android.server.hdmi.HdmiCecMessageValidator.ParameterValidator
        public int isValid(byte[] params) {
            boolean z = true;
            if (params.length < 1) {
                return 4;
            }
            if (!HdmiCecMessageValidator.isWithinRange(params[0], 1, 7) && !HdmiCecMessageValidator.isWithinRange(params[0], 9, 14) && !HdmiCecMessageValidator.isWithinRange(params[0], 16, 23) && !HdmiCecMessageValidator.isWithinRange(params[0], 26, 27) && params[0] != 31) {
                z = false;
            }
            return HdmiCecMessageValidator.toErrorCode(z);
        }
    }

    /* loaded from: classes.dex */
    private static class AsciiValidator implements ParameterValidator {
        private final int mMaxLength;
        private final int mMinLength;

        AsciiValidator(int length) {
            this.mMinLength = length;
            this.mMaxLength = length;
        }

        AsciiValidator(int minLength, int maxLength) {
            this.mMinLength = minLength;
            this.mMaxLength = maxLength;
        }

        @Override // com.android.server.hdmi.HdmiCecMessageValidator.ParameterValidator
        public int isValid(byte[] params) {
            if (params.length < this.mMinLength) {
                return 4;
            }
            return HdmiCecMessageValidator.toErrorCode(HdmiCecMessageValidator.isValidAsciiString(params, 0, this.mMaxLength));
        }
    }

    /* loaded from: classes.dex */
    private static class OsdStringValidator implements ParameterValidator {
        private OsdStringValidator() {
        }

        @Override // com.android.server.hdmi.HdmiCecMessageValidator.ParameterValidator
        public int isValid(byte[] params) {
            if (params.length < 2) {
                return 4;
            }
            boolean z = false;
            if (HdmiCecMessageValidator.isValidDisplayControl(params[0]) && HdmiCecMessageValidator.isValidAsciiString(params, 1, 14)) {
                z = true;
            }
            return HdmiCecMessageValidator.toErrorCode(z);
        }
    }

    /* loaded from: classes.dex */
    private static class OneByteRangeValidator implements ParameterValidator {
        private final int mMaxValue;
        private final int mMinValue;

        OneByteRangeValidator(int minValue, int maxValue) {
            this.mMinValue = minValue;
            this.mMaxValue = maxValue;
        }

        @Override // com.android.server.hdmi.HdmiCecMessageValidator.ParameterValidator
        public int isValid(byte[] params) {
            if (params.length < 1) {
                return 4;
            }
            return HdmiCecMessageValidator.toErrorCode(HdmiCecMessageValidator.isWithinRange(params[0], this.mMinValue, this.mMaxValue));
        }
    }

    /* loaded from: classes.dex */
    private static class AnalogueTimerValidator implements ParameterValidator {
        private AnalogueTimerValidator() {
        }

        @Override // com.android.server.hdmi.HdmiCecMessageValidator.ParameterValidator
        public int isValid(byte[] params) {
            if (params.length < 11) {
                return 4;
            }
            boolean z = false;
            if (HdmiCecMessageValidator.isValidDayOfMonth(params[0]) && HdmiCecMessageValidator.isValidMonthOfYear(params[1]) && HdmiCecMessageValidator.isValidHour(params[2]) && HdmiCecMessageValidator.isValidMinute(params[3]) && HdmiCecMessageValidator.isValidDurationHours(params[4]) && HdmiCecMessageValidator.isValidMinute(params[5]) && HdmiCecMessageValidator.isValidRecordingSequence(params[6]) && HdmiCecMessageValidator.isValidAnalogueBroadcastType(params[7]) && HdmiCecMessageValidator.isValidAnalogueFrequency(HdmiUtils.twoBytesToInt(params, 8)) && HdmiCecMessageValidator.isValidBroadcastSystem(params[10])) {
                z = true;
            }
            return HdmiCecMessageValidator.toErrorCode(z);
        }
    }

    /* loaded from: classes.dex */
    private static class DigitalTimerValidator implements ParameterValidator {
        private DigitalTimerValidator() {
        }

        @Override // com.android.server.hdmi.HdmiCecMessageValidator.ParameterValidator
        public int isValid(byte[] params) {
            if (params.length < 11) {
                return 4;
            }
            boolean z = false;
            if (HdmiCecMessageValidator.isValidDayOfMonth(params[0]) && HdmiCecMessageValidator.isValidMonthOfYear(params[1]) && HdmiCecMessageValidator.isValidHour(params[2]) && HdmiCecMessageValidator.isValidMinute(params[3]) && HdmiCecMessageValidator.isValidDurationHours(params[4]) && HdmiCecMessageValidator.isValidMinute(params[5]) && HdmiCecMessageValidator.isValidRecordingSequence(params[6]) && HdmiCecMessageValidator.isValidDigitalServiceIdentification(params, 7)) {
                z = true;
            }
            return HdmiCecMessageValidator.toErrorCode(z);
        }
    }

    /* loaded from: classes.dex */
    private static class ExternalTimerValidator implements ParameterValidator {
        private ExternalTimerValidator() {
        }

        @Override // com.android.server.hdmi.HdmiCecMessageValidator.ParameterValidator
        public int isValid(byte[] params) {
            if (params.length < 9) {
                return 4;
            }
            boolean z = false;
            if (HdmiCecMessageValidator.isValidDayOfMonth(params[0]) && HdmiCecMessageValidator.isValidMonthOfYear(params[1]) && HdmiCecMessageValidator.isValidHour(params[2]) && HdmiCecMessageValidator.isValidMinute(params[3]) && HdmiCecMessageValidator.isValidDurationHours(params[4]) && HdmiCecMessageValidator.isValidMinute(params[5]) && HdmiCecMessageValidator.isValidRecordingSequence(params[6]) && HdmiCecMessageValidator.isValidExternalSource(params, 7)) {
                z = true;
            }
            return HdmiCecMessageValidator.toErrorCode(z);
        }
    }

    /* loaded from: classes.dex */
    private static class TimerClearedStatusValidator implements ParameterValidator {
        private TimerClearedStatusValidator() {
        }

        @Override // com.android.server.hdmi.HdmiCecMessageValidator.ParameterValidator
        public int isValid(byte[] params) {
            boolean z = true;
            if (params.length < 1) {
                return 4;
            }
            if (!HdmiCecMessageValidator.isWithinRange(params[0], 0, 2) && (params[0] & 255) != 128) {
                z = false;
            }
            return HdmiCecMessageValidator.toErrorCode(z);
        }
    }

    /* loaded from: classes.dex */
    private static class TimerStatusValidator implements ParameterValidator {
        private TimerStatusValidator() {
        }

        @Override // com.android.server.hdmi.HdmiCecMessageValidator.ParameterValidator
        public int isValid(byte[] params) {
            if (params.length < 1) {
                return 4;
            }
            return HdmiCecMessageValidator.toErrorCode(HdmiCecMessageValidator.isValidTimerStatusData(params, 0));
        }
    }

    /* loaded from: classes.dex */
    private static class PlayModeValidator implements ParameterValidator {
        private PlayModeValidator() {
        }

        @Override // com.android.server.hdmi.HdmiCecMessageValidator.ParameterValidator
        public int isValid(byte[] params) {
            if (params.length < 1) {
                return 4;
            }
            return HdmiCecMessageValidator.toErrorCode(HdmiCecMessageValidator.isValidPlayMode(params[0]));
        }
    }

    /* loaded from: classes.dex */
    private static class SelectAnalogueServiceValidator implements ParameterValidator {
        private SelectAnalogueServiceValidator() {
        }

        @Override // com.android.server.hdmi.HdmiCecMessageValidator.ParameterValidator
        public int isValid(byte[] params) {
            if (params.length < 4) {
                return 4;
            }
            boolean z = false;
            if (HdmiCecMessageValidator.isValidAnalogueBroadcastType(params[0]) && HdmiCecMessageValidator.isValidAnalogueFrequency(HdmiUtils.twoBytesToInt(params, 1)) && HdmiCecMessageValidator.isValidBroadcastSystem(params[3])) {
                z = true;
            }
            return HdmiCecMessageValidator.toErrorCode(z);
        }
    }

    /* loaded from: classes.dex */
    private static class SelectDigitalServiceValidator implements ParameterValidator {
        private SelectDigitalServiceValidator() {
        }

        @Override // com.android.server.hdmi.HdmiCecMessageValidator.ParameterValidator
        public int isValid(byte[] params) {
            if (params.length < 4) {
                return 4;
            }
            return HdmiCecMessageValidator.toErrorCode(HdmiCecMessageValidator.isValidDigitalServiceIdentification(params, 0));
        }
    }

    /* loaded from: classes.dex */
    private static class TunerDeviceStatusValidator implements ParameterValidator {
        private TunerDeviceStatusValidator() {
        }

        @Override // com.android.server.hdmi.HdmiCecMessageValidator.ParameterValidator
        public int isValid(byte[] params) {
            if (params.length < 1) {
                return 4;
            }
            return HdmiCecMessageValidator.toErrorCode(HdmiCecMessageValidator.isValidTunerDeviceInfo(params));
        }
    }

    /* loaded from: classes.dex */
    private static class UserControlPressedValidator implements ParameterValidator {
        private UserControlPressedValidator() {
        }

        @Override // com.android.server.hdmi.HdmiCecMessageValidator.ParameterValidator
        public int isValid(byte[] params) {
            if (params.length < 1) {
                return 4;
            }
            if (params.length == 1) {
                return 0;
            }
            int uiCommand = params[0];
            switch (uiCommand) {
                case 86:
                    return HdmiCecMessageValidator.toErrorCode(HdmiCecMessageValidator.isValidUiBroadcastType(params[1]));
                case 87:
                    return HdmiCecMessageValidator.toErrorCode(HdmiCecMessageValidator.isValidUiSoundPresenationControl(params[1]));
                case 96:
                    return HdmiCecMessageValidator.toErrorCode(HdmiCecMessageValidator.isValidPlayMode(params[1]));
                case 103:
                    if (params.length >= 4) {
                        return HdmiCecMessageValidator.toErrorCode(HdmiCecMessageValidator.isValidChannelIdentifier(params, 1));
                    }
                    return 4;
                default:
                    return 0;
            }
        }
    }
}
