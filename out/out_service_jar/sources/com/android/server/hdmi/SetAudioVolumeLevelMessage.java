package com.android.server.hdmi;
/* loaded from: classes.dex */
public class SetAudioVolumeLevelMessage extends HdmiCecMessage {
    private final int mAudioVolumeLevel;

    private SetAudioVolumeLevelMessage(int source, int destination, byte[] params, int audioVolumeLevel) {
        super(source, destination, 115, params, 0);
        this.mAudioVolumeLevel = audioVolumeLevel;
    }

    public static HdmiCecMessage build(int source, int destination, int audioVolumeLevel) {
        byte[] params = {(byte) (audioVolumeLevel & 255)};
        int addressValidationResult = validateAddress(source, destination);
        if (addressValidationResult == 0) {
            return new SetAudioVolumeLevelMessage(source, destination, params, audioVolumeLevel);
        }
        return new HdmiCecMessage(source, destination, 115, params, addressValidationResult);
    }

    public static HdmiCecMessage build(int source, int destination, byte[] params) {
        if (params.length == 0) {
            return new HdmiCecMessage(source, destination, 115, params, 4);
        }
        int audioVolumeLevel = params[0];
        int addressValidationResult = validateAddress(source, destination);
        if (addressValidationResult == 0) {
            return new SetAudioVolumeLevelMessage(source, destination, params, audioVolumeLevel);
        }
        return new HdmiCecMessage(source, destination, 115, params, addressValidationResult);
    }

    public static int validateAddress(int source, int destination) {
        return HdmiCecMessageValidator.validateAddress(source, destination, 1);
    }

    public int getAudioVolumeLevel() {
        return this.mAudioVolumeLevel;
    }
}
