package com.android.internal.telephony.util;

import android.Manifest;
import android.app.PendingIntent$$ExternalSyntheticLambda1;
import android.content.Context;
import android.content.pm.ComponentInfo;
import android.content.pm.ResolveInfo;
import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public final class TelephonyUtils {
    public static final Executor DIRECT_EXECUTOR;
    public static boolean IS_DEBUGGABLE;
    public static boolean IS_USER = "user".equals(Build.TYPE);

    static {
        IS_DEBUGGABLE = SystemProperties.getInt("ro.debuggable", 0) == 1;
        DIRECT_EXECUTOR = new PendingIntent$$ExternalSyntheticLambda1();
    }

    public static boolean checkDumpPermission(Context context, String tag, PrintWriter pw) {
        if (context.checkCallingOrSelfPermission(Manifest.permission.DUMP) != 0) {
            pw.println("Permission Denial: can't dump " + tag + " from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " due to missing android.permission.DUMP permission");
            return false;
        }
        return true;
    }

    public static String emptyIfNull(String str) {
        return str == null ? "" : str;
    }

    public static <T> List<T> emptyIfNull(List<T> cur) {
        return cur == null ? Collections.emptyList() : cur;
    }

    public static ComponentInfo getComponentInfo(ResolveInfo resolveInfo) {
        if (resolveInfo.activityInfo != null) {
            return resolveInfo.activityInfo;
        }
        if (resolveInfo.serviceInfo != null) {
            return resolveInfo.serviceInfo;
        }
        if (resolveInfo.providerInfo != null) {
            return resolveInfo.providerInfo;
        }
        throw new IllegalStateException("Missing ComponentInfo!");
    }

    public static void runWithCleanCallingIdentity(Runnable action) {
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            action.run();
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    public static void runWithCleanCallingIdentity(final Runnable action, Executor executor) {
        if (action != null) {
            if (executor != null) {
                executor.execute(new Runnable() { // from class: com.android.internal.telephony.util.TelephonyUtils$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        TelephonyUtils.runWithCleanCallingIdentity(action);
                    }
                });
            } else {
                runWithCleanCallingIdentity(action);
            }
        }
    }

    public static <T> T runWithCleanCallingIdentity(Supplier<T> action) {
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            return action.get();
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    public static Bundle filterValues(Bundle bundle) {
        Bundle ret = new Bundle(bundle);
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            if (!(value instanceof Integer) && !(value instanceof Long) && !(value instanceof Double) && !(value instanceof String) && !(value instanceof int[]) && !(value instanceof long[]) && !(value instanceof double[]) && !(value instanceof String[]) && !(value instanceof PersistableBundle) && value != null && !(value instanceof Boolean) && !(value instanceof boolean[])) {
                if (value instanceof Bundle) {
                    ret.putBundle(key, filterValues((Bundle) value));
                } else if (!value.getClass().getName().startsWith("android.")) {
                    ret.remove(key);
                }
            }
        }
        return ret;
    }

    public static void waitUntilReady(CountDownLatch latch, long timeoutMs) {
        try {
            latch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }
    }

    public static String dataStateToString(int state) {
        switch (state) {
            case -1:
                return "UNKNOWN";
            case 0:
                return "DISCONNECTED";
            case 1:
                return "CONNECTING";
            case 2:
                return "CONNECTED";
            case 3:
                return "SUSPENDED";
            case 4:
                return "DISCONNECTING";
            default:
                return "UNKNOWN(" + state + NavigationBarInflaterView.KEY_CODE_END;
        }
    }
}
