package com.android.telephony;

import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import com.android.internal.telephony.util.TelephonyUtils;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
/* loaded from: classes4.dex */
public final class Rlog {
    private static final boolean USER_BUILD = TelephonyUtils.IS_USER;

    private Rlog() {
    }

    private static int log(int priority, String tag, String msg) {
        return Log.logToRadioBuffer(priority, tag, msg);
    }

    public static int v(String tag, String msg) {
        return log(2, tag, msg);
    }

    public static int v(String tag, String msg, Throwable tr) {
        return log(2, tag, msg + '\n' + Log.getStackTraceString(tr));
    }

    public static int d(String tag, String msg) {
        return log(3, tag, msg);
    }

    public static int d(String tag, String msg, Throwable tr) {
        return log(3, tag, msg + '\n' + Log.getStackTraceString(tr));
    }

    public static int i(String tag, String msg) {
        return log(4, tag, msg);
    }

    public static int i(String tag, String msg, Throwable tr) {
        return log(4, tag, msg + '\n' + Log.getStackTraceString(tr));
    }

    public static int w(String tag, String msg) {
        return log(5, tag, msg);
    }

    public static int w(String tag, String msg, Throwable tr) {
        return log(5, tag, msg + '\n' + Log.getStackTraceString(tr));
    }

    public static int w(String tag, Throwable tr) {
        return log(5, tag, Log.getStackTraceString(tr));
    }

    public static int e(String tag, String msg) {
        return log(6, tag, msg);
    }

    public static int e(String tag, String msg, Throwable tr) {
        return log(6, tag, msg + '\n' + Log.getStackTraceString(tr));
    }

    public static int println(int priority, String tag, String msg) {
        return log(priority, tag, msg);
    }

    public static boolean isLoggable(String tag, int level) {
        return Log.isLoggable(tag, level);
    }

    public static String pii(String tag, Object pii) {
        String val = String.valueOf(pii);
        if (pii == null || TextUtils.isEmpty(val) || isLoggable(tag, 2)) {
            return val;
        }
        return NavigationBarInflaterView.SIZE_MOD_START + secureHash(val.getBytes()) + NavigationBarInflaterView.SIZE_MOD_END;
    }

    public static String pii(boolean enablePiiLogging, Object pii) {
        String val = String.valueOf(pii);
        if (pii == null || TextUtils.isEmpty(val) || enablePiiLogging) {
            return val;
        }
        return NavigationBarInflaterView.SIZE_MOD_START + secureHash(val.getBytes()) + NavigationBarInflaterView.SIZE_MOD_END;
    }

    private static String secureHash(byte[] input) {
        if (USER_BUILD) {
            return "****";
        }
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(KeyProperties.DIGEST_SHA1);
            byte[] result = messageDigest.digest(input);
            return Base64.encodeToString(result, 11);
        } catch (NoSuchAlgorithmException e) {
            return "####";
        }
    }
}
