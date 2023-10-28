package com.android.server.biometrics;

import android.hardware.keymaster.HardwareAuthToken;
import android.hardware.keymaster.Timestamp;
import java.nio.ByteOrder;
/* loaded from: classes.dex */
public class HardwareAuthTokenUtils {
    public static byte[] toByteArray(HardwareAuthToken hat) {
        byte[] array = new byte[69];
        array[0] = 0;
        writeLong(hat.challenge, array, 1);
        writeLong(hat.userId, array, 9);
        writeLong(hat.authenticatorId, array, 17);
        writeInt(flipIfNativelyLittle(hat.authenticatorType), array, 25);
        writeLong(flipIfNativelyLittle(hat.timestamp.milliSeconds), array, 29);
        System.arraycopy(hat.mac, 0, array, 37, hat.mac.length);
        return array;
    }

    public static HardwareAuthToken toHardwareAuthToken(byte[] array) {
        HardwareAuthToken hardwareAuthToken = new HardwareAuthToken();
        hardwareAuthToken.challenge = getLong(array, 1);
        hardwareAuthToken.userId = getLong(array, 9);
        hardwareAuthToken.authenticatorId = getLong(array, 17);
        hardwareAuthToken.authenticatorType = flipIfNativelyLittle(getInt(array, 25));
        Timestamp timestamp = new Timestamp();
        timestamp.milliSeconds = flipIfNativelyLittle(getLong(array, 29));
        hardwareAuthToken.timestamp = timestamp;
        hardwareAuthToken.mac = new byte[32];
        System.arraycopy(array, 37, hardwareAuthToken.mac, 0, 32);
        return hardwareAuthToken;
    }

    private static long flipIfNativelyLittle(long l) {
        if (ByteOrder.LITTLE_ENDIAN == ByteOrder.nativeOrder()) {
            return Long.reverseBytes(l);
        }
        return l;
    }

    private static int flipIfNativelyLittle(int i) {
        if (ByteOrder.LITTLE_ENDIAN == ByteOrder.nativeOrder()) {
            return Integer.reverseBytes(i);
        }
        return i;
    }

    private static void writeLong(long l, byte[] dest, int offset) {
        dest[offset + 0] = (byte) l;
        dest[offset + 1] = (byte) (l >> 8);
        dest[offset + 2] = (byte) (l >> 16);
        dest[offset + 3] = (byte) (l >> 24);
        dest[offset + 4] = (byte) (l >> 32);
        dest[offset + 5] = (byte) (l >> 40);
        dest[offset + 6] = (byte) (l >> 48);
        dest[offset + 7] = (byte) (l >> 56);
    }

    private static void writeInt(int i, byte[] dest, int offset) {
        dest[offset + 0] = (byte) i;
        dest[offset + 1] = (byte) (i >> 8);
        dest[offset + 2] = (byte) (i >> 16);
        dest[offset + 3] = (byte) (i >> 24);
    }

    private static long getLong(byte[] array, int offset) {
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result += (array[i + offset] & 255) << (i * 8);
        }
        return result;
    }

    private static int getInt(byte[] array, int offset) {
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result += (array[i + offset] & 255) << (i * 8);
        }
        return result;
    }
}
