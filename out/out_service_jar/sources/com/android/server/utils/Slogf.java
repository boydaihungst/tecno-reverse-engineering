package com.android.server.utils;

import android.util.Log;
import android.util.Slog;
import android.util.TimingsTraceLog;
import java.util.Formatter;
import java.util.Locale;
/* loaded from: classes2.dex */
public final class Slogf {
    private static final Formatter sFormatter;
    private static final StringBuilder sMessageBuilder;

    static {
        TimingsTraceLog t = new TimingsTraceLog("SLog", 524288L);
        t.traceBegin("static_init");
        StringBuilder sb = new StringBuilder();
        sMessageBuilder = sb;
        sFormatter = new Formatter(sb, Locale.ENGLISH);
        t.traceEnd();
    }

    private Slogf() {
        throw new UnsupportedOperationException("provides only static methods");
    }

    public static boolean isLoggable(String tag, int level) {
        return Log.isLoggable(tag, level);
    }

    public static int v(String tag, String msg) {
        return Slog.v(tag, msg);
    }

    public static int v(String tag, String msg, Throwable tr) {
        return Slog.v(tag, msg, tr);
    }

    public static int d(String tag, String msg) {
        return Slog.d(tag, msg);
    }

    public static int d(String tag, String msg, Throwable tr) {
        return Slog.d(tag, msg, tr);
    }

    public static int i(String tag, String msg) {
        return Slog.i(tag, msg);
    }

    public static int i(String tag, String msg, Throwable tr) {
        return Slog.i(tag, msg, tr);
    }

    public static int w(String tag, String msg) {
        return Slog.w(tag, msg);
    }

    public static int w(String tag, String msg, Throwable tr) {
        return Slog.w(tag, msg, tr);
    }

    public static int w(String tag, Throwable tr) {
        return Slog.w(tag, tr);
    }

    public static int e(String tag, String msg) {
        return Slog.e(tag, msg);
    }

    public static int e(String tag, String msg, Throwable tr) {
        return Slog.e(tag, msg, tr);
    }

    public static int wtf(String tag, String msg) {
        return Slog.wtf(tag, msg);
    }

    public static void wtfQuiet(String tag, String msg) {
        Slog.wtfQuiet(tag, msg);
    }

    public static int wtfStack(String tag, String msg) {
        return Slog.wtfStack(tag, msg);
    }

    public static int wtf(String tag, Throwable tr) {
        return Slog.wtf(tag, tr);
    }

    public static int wtf(String tag, String msg, Throwable tr) {
        return Slog.wtf(tag, msg, tr);
    }

    public static int println(int priority, String tag, String msg) {
        return Slog.println(priority, tag, msg);
    }

    public static void v(String tag, String format, Object... args) {
        if (isLoggable(tag, 2)) {
            v(tag, getMessage(format, args));
        }
    }

    public static void d(String tag, String format, Object... args) {
        if (isLoggable(tag, 3)) {
            d(tag, getMessage(format, args));
        }
    }

    public static void i(String tag, String format, Object... args) {
        if (isLoggable(tag, 4)) {
            i(tag, getMessage(format, args));
        }
    }

    public static void w(String tag, String format, Object... args) {
        if (isLoggable(tag, 5)) {
            w(tag, getMessage(format, args));
        }
    }

    public static void w(String tag, Exception exception, String format, Object... args) {
        if (isLoggable(tag, 5)) {
            w(tag, getMessage(format, args), exception);
        }
    }

    public static void e(String tag, String format, Object... args) {
        if (isLoggable(tag, 6)) {
            e(tag, getMessage(format, args));
        }
    }

    public static void e(String tag, Exception exception, String format, Object... args) {
        if (isLoggable(tag, 6)) {
            e(tag, getMessage(format, args), exception);
        }
    }

    public static void wtf(String tag, String format, Object... args) {
        wtf(tag, getMessage(format, args));
    }

    public static void wtf(String tag, Exception exception, String format, Object... args) {
        wtf(tag, getMessage(format, args), exception);
    }

    private static String getMessage(String format, Object... args) {
        String message;
        StringBuilder sb = sMessageBuilder;
        synchronized (sb) {
            sFormatter.format(format, args);
            message = sb.toString();
            sb.setLength(0);
        }
        return message;
    }
}
