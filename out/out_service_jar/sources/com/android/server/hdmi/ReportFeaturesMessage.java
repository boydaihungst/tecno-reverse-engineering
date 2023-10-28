package com.android.server.hdmi;

import android.hardware.hdmi.DeviceFeatures;
import com.android.server.location.gnss.hal.GnssNative;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
/* loaded from: classes.dex */
public class ReportFeaturesMessage extends HdmiCecMessage {
    private final int mCecVersion;
    private final DeviceFeatures mDeviceFeatures;

    private ReportFeaturesMessage(int source, int destination, byte[] params, int cecVersion, DeviceFeatures deviceFeatures) {
        super(source, destination, 166, params, 0);
        this.mCecVersion = cecVersion;
        this.mDeviceFeatures = deviceFeatures;
    }

    public static HdmiCecMessage build(int source, int cecVersion, List<Integer> allDeviceTypes, int rcProfile, List<Integer> rcFeatures, DeviceFeatures deviceFeatures) {
        byte rcProfileByte;
        byte cecVersionByte = (byte) (cecVersion & 255);
        byte deviceTypes = 0;
        for (Integer deviceType : allDeviceTypes) {
            deviceTypes = (byte) (((byte) (1 << hdmiDeviceInfoDeviceTypeToShiftValue(deviceType.intValue()))) | deviceTypes);
        }
        byte rcProfileByte2 = (byte) (((byte) (rcProfile << 6)) | 0);
        if (rcProfile == 1) {
            for (Integer rcFeature : rcFeatures) {
                rcProfileByte2 = (byte) (((byte) (1 << rcFeature.intValue())) | rcProfileByte2);
            }
            rcProfileByte = rcProfileByte2;
        } else {
            byte rcProfileTv = (byte) (rcFeatures.get(0).intValue() & GnssNative.GNSS_AIDING_TYPE_ALL);
            rcProfileByte = (byte) (rcProfileByte2 | rcProfileTv);
        }
        byte[] fixedOperands = {cecVersionByte, deviceTypes, rcProfileByte};
        byte[] deviceFeaturesBytes = deviceFeatures.toOperand();
        byte[] params = Arrays.copyOf(fixedOperands, fixedOperands.length + deviceFeaturesBytes.length);
        System.arraycopy(deviceFeaturesBytes, 0, params, fixedOperands.length, deviceFeaturesBytes.length);
        int addressValidationResult = validateAddress(source, 15);
        if (addressValidationResult != 0) {
            return new HdmiCecMessage(source, 15, 166, params, addressValidationResult);
        }
        return new ReportFeaturesMessage(source, 15, params, cecVersion, deviceFeatures);
    }

    private static int hdmiDeviceInfoDeviceTypeToShiftValue(int deviceType) {
        switch (deviceType) {
            case 0:
                return 7;
            case 1:
                return 6;
            case 2:
            default:
                throw new IllegalArgumentException("Unhandled device type: " + deviceType);
            case 3:
                return 5;
            case 4:
                return 4;
            case 5:
                return 3;
            case 6:
                return 2;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage build(final int source, final int destination, final byte[] params) {
        Function<Integer, HdmiCecMessage> invalidMessage = new Function() { // from class: com.android.server.hdmi.ReportFeaturesMessage$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ReportFeaturesMessage.lambda$build$0(source, destination, params, (Integer) obj);
            }
        };
        int addressValidationResult = validateAddress(source, destination);
        if (addressValidationResult != 0) {
            return invalidMessage.apply(Integer.valueOf(addressValidationResult));
        }
        if (params.length >= 4) {
            int cecVersion = Byte.toUnsignedInt(params[0]);
            int rcProfileEnd = HdmiUtils.getEndOfSequence(params, 2);
            if (rcProfileEnd != -1) {
                int deviceFeaturesEnd = HdmiUtils.getEndOfSequence(params, rcProfileEnd + 1);
                if (deviceFeaturesEnd == -1) {
                    return invalidMessage.apply(4);
                }
                int deviceFeaturesStart = HdmiUtils.getEndOfSequence(params, 2) + 1;
                byte[] deviceFeaturesBytes = Arrays.copyOfRange(params, deviceFeaturesStart, params.length);
                DeviceFeatures deviceFeatures = DeviceFeatures.fromOperand(deviceFeaturesBytes);
                return new ReportFeaturesMessage(source, destination, params, cecVersion, deviceFeatures);
            }
            return invalidMessage.apply(4);
        }
        return invalidMessage.apply(4);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ HdmiCecMessage lambda$build$0(int source, int destination, byte[] params, Integer validationResult) {
        return new HdmiCecMessage(source, destination, 166, params, validationResult.intValue());
    }

    public static int validateAddress(int source, int destination) {
        return HdmiCecMessageValidator.validateAddress(source, destination, 2);
    }

    public int getCecVersion() {
        return this.mCecVersion;
    }

    public DeviceFeatures getDeviceFeatures() {
        return this.mDeviceFeatures;
    }
}
