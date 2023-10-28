package com.android.server.biometrics.sensors.fingerprint.aidl;
/* loaded from: classes.dex */
final class AidlConversionUtils {
    private AidlConversionUtils() {
    }

    public static int toFrameworkError(byte aidlError) {
        if (aidlError == 0) {
            return 17;
        }
        if (aidlError == 1) {
            return 1;
        }
        if (aidlError == 2) {
            return 2;
        }
        if (aidlError == 3) {
            return 3;
        }
        if (aidlError == 4) {
            return 4;
        }
        if (aidlError == 5) {
            return 5;
        }
        if (aidlError == 6) {
            return 6;
        }
        if (aidlError == 7) {
            return 8;
        }
        if (aidlError != 8) {
            return 17;
        }
        return 18;
    }

    public static int toFrameworkAcquiredInfo(byte aidlAcquiredInfo) {
        if (aidlAcquiredInfo == 0) {
            return 8;
        }
        if (aidlAcquiredInfo == 1) {
            return 0;
        }
        if (aidlAcquiredInfo == 2) {
            return 1;
        }
        if (aidlAcquiredInfo == 3) {
            return 2;
        }
        if (aidlAcquiredInfo == 4) {
            return 3;
        }
        if (aidlAcquiredInfo == 5) {
            return 4;
        }
        if (aidlAcquiredInfo == 6) {
            return 5;
        }
        if (aidlAcquiredInfo == 7) {
            return 6;
        }
        if (aidlAcquiredInfo == 8) {
            return 7;
        }
        if (aidlAcquiredInfo == 9) {
            return 8;
        }
        if (aidlAcquiredInfo == 10) {
            return 10;
        }
        if (aidlAcquiredInfo != 11) {
            return 8;
        }
        return 9;
    }
}
