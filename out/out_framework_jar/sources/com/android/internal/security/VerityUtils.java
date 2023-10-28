package com.android.internal.security;

import android.os.Build;
import android.os.SystemProperties;
import android.system.Os;
import android.system.OsConstants;
import android.util.Slog;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
/* loaded from: classes4.dex */
public abstract class VerityUtils {
    public static final String FSVERITY_SIGNATURE_FILE_EXTENSION = ".fsv_sig";
    private static final int HASH_SIZE_BYTES = 32;
    private static final int MAX_SIGNATURE_FILE_SIZE_BYTES = 8192;
    private static final String TAG = "VerityUtils";

    private static native int enableFsverityNative(String str, byte[] bArr);

    private static native int measureFsverityNative(String str, byte[] bArr);

    private static native int statxForFsverityNative(String str);

    public static boolean isFsVeritySupported() {
        return Build.VERSION.DEVICE_INITIAL_SDK_INT >= 30 || SystemProperties.getInt("ro.apk_verity.mode", 0) == 2;
    }

    public static boolean isFsveritySignatureFile(File file) {
        return file.getName().endsWith(FSVERITY_SIGNATURE_FILE_EXTENSION);
    }

    public static String getFsveritySignatureFilePath(String filePath) {
        return filePath + FSVERITY_SIGNATURE_FILE_EXTENSION;
    }

    public static void setUpFsverity(String filePath, String signaturePath) throws IOException {
        if (Files.size(Paths.get(signaturePath, new String[0])) > 8192) {
            throw new SecurityException("Signature file is unexpectedly large: " + signaturePath);
        }
        setUpFsverity(filePath, Files.readAllBytes(Paths.get(signaturePath, new String[0])));
    }

    public static void setUpFsverity(String filePath, byte[] pkcs7Signature) throws IOException {
        int errno = enableFsverityNative(filePath, pkcs7Signature);
        if (errno != 0) {
            throw new IOException("Failed to enable fs-verity on " + filePath + ": " + Os.strerror(errno));
        }
    }

    public static boolean hasFsverity(String filePath) {
        int retval = statxForFsverityNative(filePath);
        if (retval < 0) {
            Slog.e(TAG, "Failed to check whether fs-verity is enabled, errno " + (-retval) + ": " + filePath);
            return false;
        } else if (retval != 1) {
            return false;
        } else {
            return true;
        }
    }

    public static byte[] getFsverityRootHash(String filePath) {
        byte[] result = new byte[32];
        int retval = measureFsverityNative(filePath, result);
        if (retval < 0) {
            if (retval != (-OsConstants.ENODATA)) {
                Slog.e(TAG, "Failed to measure fs-verity, errno " + (-retval) + ": " + filePath);
                return null;
            }
            return null;
        }
        return result;
    }
}
