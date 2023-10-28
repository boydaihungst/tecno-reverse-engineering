package com.android.server.hdmi;

import com.android.internal.util.FrameworkStatsLog;
import java.util.Arrays;
import java.util.Objects;
import libcore.util.EmptyArray;
/* loaded from: classes.dex */
public class HdmiCecMessage {
    public static final byte[] EMPTY_PARAM = EmptyArray.BYTE;
    private final int mDestination;
    private final int mOpcode;
    private final byte[] mParams;
    private final int mSource;
    private final int mValidationResult;

    /* JADX INFO: Access modifiers changed from: protected */
    public HdmiCecMessage(int source, int destination, int opcode, byte[] params, int validationResult) {
        this.mSource = source;
        this.mDestination = destination;
        this.mOpcode = opcode & 255;
        this.mParams = Arrays.copyOf(params, params.length);
        this.mValidationResult = validationResult;
    }

    private HdmiCecMessage(int source, int destination, int opcode, byte[] params) {
        this(source, destination, opcode, params, HdmiCecMessageValidator.validate(source, destination, opcode & 255, params));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage build(int source, int destination, int opcode, byte[] params) {
        switch (opcode & 255) {
            case 115:
                return SetAudioVolumeLevelMessage.build(source, destination, params);
            case 166:
                return ReportFeaturesMessage.build(source, destination, params);
            default:
                return new HdmiCecMessage(source, destination, opcode & 255, params);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage build(int source, int destination, int opcode) {
        return new HdmiCecMessage(source, destination, opcode, EMPTY_PARAM);
    }

    public boolean equals(Object message) {
        if (message instanceof HdmiCecMessage) {
            HdmiCecMessage that = (HdmiCecMessage) message;
            return this.mSource == that.getSource() && this.mDestination == that.getDestination() && this.mOpcode == that.getOpcode() && Arrays.equals(this.mParams, that.getParams()) && this.mValidationResult == that.getValidationResult();
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mSource), Integer.valueOf(this.mDestination), Integer.valueOf(this.mOpcode), Integer.valueOf(Arrays.hashCode(this.mParams)));
    }

    public int getSource() {
        return this.mSource;
    }

    public int getDestination() {
        return this.mDestination;
    }

    public int getOpcode() {
        return this.mOpcode;
    }

    public byte[] getParams() {
        return this.mParams;
    }

    public int getValidationResult() {
        return this.mValidationResult;
    }

    public String toString() {
        byte[] bArr;
        StringBuilder s = new StringBuilder();
        s.append(String.format("<%s> %X%X:%02X", opcodeToString(this.mOpcode), Integer.valueOf(this.mSource), Integer.valueOf(this.mDestination), Integer.valueOf(this.mOpcode)));
        if (this.mParams.length > 0) {
            if (filterMessageParameters(this.mOpcode)) {
                s.append(String.format(" <Redacted len=%d>", Integer.valueOf(this.mParams.length)));
            } else if (isUserControlPressedMessage(this.mOpcode)) {
                s.append(String.format(" <Keycode type = %s>", HdmiCecKeycode.getKeycodeType(this.mParams[0])));
            } else {
                for (byte data : this.mParams) {
                    s.append(String.format(":%02X", Byte.valueOf(data)));
                }
            }
        }
        int i = this.mValidationResult;
        if (i != 0) {
            s.append(String.format(" <Validation error: %s>", validationResultToString(i)));
        }
        return s.toString();
    }

    private static String validationResultToString(int validationResult) {
        switch (validationResult) {
            case 0:
                return "ok";
            case 1:
                return "invalid source";
            case 2:
                return "invalid destination";
            case 3:
                return "invalid parameters";
            case 4:
                return "short parameters";
            default:
                return "unknown error";
        }
    }

    private static String opcodeToString(int opcode) {
        switch (opcode) {
            case 0:
                return "Feature Abort";
            case 4:
                return "Image View On";
            case 5:
                return "Tuner Step Increment";
            case 6:
                return "Tuner Step Decrement";
            case 7:
                return "Tuner Device Status";
            case 8:
                return "Give Tuner Device Status";
            case 9:
                return "Record On";
            case 10:
                return "Record Status";
            case 11:
                return "Record Off";
            case 13:
                return "Text View On";
            case 15:
                return "Record Tv Screen";
            case 26:
                return "Give Deck Status";
            case 27:
                return "Deck Status";
            case 50:
                return "Set Menu Language";
            case 51:
                return "Clear Analog Timer";
            case 52:
                return "Set Analog Timer";
            case 53:
                return "Timer Status";
            case 54:
                return "Standby";
            case 65:
                return "Play";
            case 66:
                return "Deck Control";
            case 67:
                return "Timer Cleared Status";
            case 68:
                return "User Control Pressed";
            case 69:
                return "User Control Release";
            case 70:
                return "Give Osd Name";
            case 71:
                return "Set Osd Name";
            case 100:
                return "Set Osd String";
            case 103:
                return "Set Timer Program Title";
            case 112:
                return "System Audio Mode Request";
            case 113:
                return "Give Audio Status";
            case 114:
                return "Set System Audio Mode";
            case 115:
                return "Set Audio Volume Level";
            case 122:
                return "Report Audio Status";
            case 125:
                return "Give System Audio Mode Status";
            case 126:
                return "System Audio Mode Status";
            case 128:
                return "Routing Change";
            case 129:
                return "Routing Information";
            case 130:
                return "Active Source";
            case 131:
                return "Give Physical Address";
            case 132:
                return "Report Physical Address";
            case 133:
                return "Request Active Source";
            case 134:
                return "Set Stream Path";
            case 135:
                return "Device Vendor Id";
            case 137:
                return "Vendor Command";
            case 138:
                return "Vendor Remote Button Down";
            case 139:
                return "Vendor Remote Button Up";
            case 140:
                return "Give Device Vendor Id";
            case 141:
                return "Menu Request";
            case 142:
                return "Menu Status";
            case 143:
                return "Give Device Power Status";
            case 144:
                return "Report Power Status";
            case 145:
                return "Get Menu Language";
            case 146:
                return "Select Analog Service";
            case 147:
                return "Select Digital Service";
            case 151:
                return "Set Digital Timer";
            case 153:
                return "Clear Digital Timer";
            case 154:
                return "Set Audio Rate";
            case 157:
                return "InActive Source";
            case 158:
                return "Cec Version";
            case 159:
                return "Get Cec Version";
            case 160:
                return "Vendor Command With Id";
            case 161:
                return "Clear External Timer";
            case 162:
                return "Set External Timer";
            case 163:
                return "Report Short Audio Descriptor";
            case 164:
                return "Request Short Audio Descriptor";
            case 165:
                return "Give Features";
            case 166:
                return "Report Features";
            case 167:
                return "Request Current Latency";
            case 168:
                return "Report Current Latency";
            case 192:
                return "Initiate ARC";
            case 193:
                return "Report ARC Initiated";
            case 194:
                return "Report ARC Terminated";
            case 195:
                return "Request ARC Initiation";
            case 196:
                return "Request ARC Termination";
            case 197:
                return "Terminate ARC";
            case FrameworkStatsLog.INTEGRITY_RULES_PUSHED /* 248 */:
                return "Cdc Message";
            case 255:
                return "Abort";
            default:
                return String.format("Opcode: %02X", Integer.valueOf(opcode));
        }
    }

    private static boolean filterMessageParameters(int opcode) {
        switch (opcode) {
            case 69:
            case 71:
            case 100:
            case 137:
            case 138:
            case 139:
            case 160:
                return true;
            default:
                return false;
        }
    }

    private static boolean isUserControlPressedMessage(int opcode) {
        return 68 == opcode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isCecTransportMessage(int opcode) {
        switch (opcode) {
            case 167:
            case 168:
            case FrameworkStatsLog.INTEGRITY_RULES_PUSHED /* 248 */:
                return true;
            default:
                return false;
        }
    }
}
