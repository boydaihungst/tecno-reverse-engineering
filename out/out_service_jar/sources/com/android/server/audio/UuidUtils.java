package com.android.server.audio;

import android.media.AudioDeviceAttributes;
import android.util.Slog;
import java.util.UUID;
/* loaded from: classes.dex */
class UuidUtils {
    private static final long LSB_PREFIX_BT = 4779445104546938880L;
    private static final long LSB_PREFIX_MASK = -281474976710656L;
    private static final long LSB_SUFFIX_MASK = 281474976710655L;
    public static final UUID STANDALONE_UUID = new UUID(0, 0);
    private static final String TAG = "AudioService.UuidUtils";

    UuidUtils() {
    }

    public static UUID uuidFromAudioDeviceAttributes(AudioDeviceAttributes device) {
        switch (device.getInternalType()) {
            case 128:
                String address = device.getAddress().replace(":", "");
                if (address.length() != 12) {
                    return null;
                }
                try {
                    long lsb = LSB_PREFIX_BT | Long.decode("0x" + address).longValue();
                    if (AudioService.DEBUG_DEVICES) {
                        Slog.i(TAG, "uuidFromAudioDeviceAttributes lsb: " + Long.toHexString(lsb));
                    }
                    return new UUID(0L, lsb);
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                return null;
        }
    }
}
