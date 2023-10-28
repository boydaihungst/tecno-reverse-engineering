package com.mediatek.boostfwk.utils;

import android.os.Trace;
import android.util.Log;
import android.util.Slog;
/* loaded from: classes.dex */
public final class LogUtil {
    private static boolean DEBUG = Config.isBoostFwkLogEnable();

    public static void slogi(String tag, String msg) {
        Slog.i(tag, msg);
    }

    public static void slogw(String tag, String msg) {
        Slog.w(tag, msg);
    }

    public static void slogd(String tag, String msg) {
        if (DEBUG) {
            Slog.d(tag, msg);
        }
    }

    public static void sloge(String tag, String msg) {
        Slog.e(tag, msg);
    }

    public static void slogeDebug(String tag, String msg) {
        if (DEBUG) {
            Slog.e(tag, msg);
        }
    }

    public static void traceAndLog(String tag, String msg) {
        if (DEBUG) {
            Trace.traceBegin(8L, msg);
            Slog.d(tag, msg);
            Trace.traceEnd(8L);
        }
    }

    public static void traceBeginAndLog(String tag, String msg) {
        if (DEBUG) {
            Trace.traceBegin(8L, msg);
            Slog.d(tag, msg);
        }
    }

    public static void mLogi(String tag, String msg) {
        Log.i(tag, msg);
    }

    public static void mLogw(String tag, String msg) {
        Log.w(tag, msg);
    }

    public static void mLogd(String tag, String msg) {
        if (DEBUG) {
            Log.d(tag, msg);
        }
    }

    public static void mLoge(String tag, String msg) {
        Log.e(tag, msg);
    }

    public static void mLogeDebug(String tag, String msg) {
        if (DEBUG) {
            Log.e(tag, msg);
        }
    }

    public static void traceAndMLogd(String tag, String msg) {
        if (DEBUG) {
            Trace.traceBegin(8L, msg);
            Log.d(tag, msg);
            Trace.traceEnd(8L);
        }
    }

    public static void traceBeginAndMLogd(String tag, String msg) {
        if (DEBUG) {
            Trace.traceBegin(8L, msg);
            Log.d(tag, msg);
        }
    }

    public static void traceBegin(String msg) {
        if (DEBUG) {
            Trace.traceBegin(8L, msg);
        }
    }

    public static void traceEnd() {
        if (DEBUG) {
            Trace.traceEnd(8L);
        }
    }

    public static void trace(String msg) {
        if (DEBUG) {
            Trace.traceBegin(8L, msg);
            Trace.traceEnd(8L);
        }
    }
}
