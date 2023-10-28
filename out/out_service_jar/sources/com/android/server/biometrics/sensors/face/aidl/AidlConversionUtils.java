package com.android.server.biometrics.sensors.face.aidl;

import android.hardware.biometrics.face.AuthenticationFrame;
import android.hardware.biometrics.face.BaseFrame;
import android.hardware.biometrics.face.Cell;
import android.hardware.biometrics.face.EnrollmentFrame;
import android.hardware.face.FaceAuthenticationFrame;
import android.hardware.face.FaceDataFrame;
import android.hardware.face.FaceEnrollCell;
import android.hardware.face.FaceEnrollFrame;
import android.util.Slog;
/* loaded from: classes.dex */
final class AidlConversionUtils {
    private static final String TAG = "AidlConversionUtils";

    private AidlConversionUtils() {
    }

    public static int toFrameworkError(byte aidlError) {
        switch (aidlError) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            case 7:
                return 8;
            case 8:
                return 16;
            default:
                return 17;
        }
    }

    public static int toFrameworkAcquiredInfo(byte aidlAcquiredInfo) {
        switch (aidlAcquiredInfo) {
            case 1:
                return 0;
            case 2:
                return 1;
            case 3:
                return 2;
            case 4:
                return 3;
            case 5:
                return 4;
            case 6:
                return 5;
            case 7:
                return 6;
            case 8:
                return 7;
            case 9:
                return 8;
            case 10:
                return 9;
            case 11:
                return 10;
            case 12:
                return 11;
            case 13:
                return 12;
            case 14:
                return 13;
            case 15:
                return 14;
            case 16:
                return 15;
            case 17:
                return 16;
            case 18:
                return 17;
            case 19:
                return 18;
            case 20:
                return 19;
            case 21:
                return 20;
            case 22:
                return 21;
            case 23:
                return 22;
            case 24:
                return 24;
            case 25:
                return 25;
            case 26:
                return 26;
            default:
                return 23;
        }
    }

    public static int toFrameworkEnrollmentStage(int aidlEnrollmentStage) {
        switch (aidlEnrollmentStage) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            default:
                return 0;
        }
    }

    public static FaceAuthenticationFrame toFrameworkAuthenticationFrame(AuthenticationFrame frame) {
        return new FaceAuthenticationFrame(toFrameworkBaseFrame(frame.data));
    }

    public static FaceEnrollFrame toFrameworkEnrollmentFrame(EnrollmentFrame frame) {
        return new FaceEnrollFrame(toFrameworkCell(frame.cell), toFrameworkEnrollmentStage(frame.stage), toFrameworkBaseFrame(frame.data));
    }

    public static FaceDataFrame toFrameworkBaseFrame(BaseFrame frame) {
        return new FaceDataFrame(toFrameworkAcquiredInfo(frame.acquiredInfo), frame.vendorCode, frame.pan, frame.tilt, frame.distance, frame.isCancellable);
    }

    public static FaceEnrollCell toFrameworkCell(Cell cell) {
        if (cell == null) {
            return null;
        }
        return new FaceEnrollCell(cell.x, cell.y, cell.z);
    }

    public static byte convertFrameworkToAidlFeature(int feature) throws IllegalArgumentException {
        switch (feature) {
            case 1:
                return (byte) 0;
            case 2:
                return (byte) 1;
            default:
                Slog.e(TAG, "Unsupported feature : " + feature);
                throw new IllegalArgumentException();
        }
    }

    public static int convertAidlToFrameworkFeature(byte feature) throws IllegalArgumentException {
        switch (feature) {
            case 0:
                return 1;
            case 1:
                return 2;
            default:
                Slog.e(TAG, "Unsupported feature : " + ((int) feature));
                throw new IllegalArgumentException();
        }
    }
}
