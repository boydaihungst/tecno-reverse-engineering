package com.android.server.utils;

import android.os.Build;
import android.util.Log;
import java.lang.reflect.Field;
/* loaded from: classes2.dex */
public interface Watchable {
    void dispatchChange(Watchable watchable);

    boolean isRegisteredObserver(Watcher watcher);

    void registerObserver(Watcher watcher);

    void unregisterObserver(Watcher watcher);

    static void verifyWatchedAttributes(Object base, Watcher observer, boolean logOnly) {
        Field[] declaredFields;
        if (!Build.IS_ENG && !Build.IS_USERDEBUG) {
            return;
        }
        for (Field f : base.getClass().getDeclaredFields()) {
            Watched annotation = (Watched) f.getAnnotation(Watched.class);
            if (annotation != null) {
                String fn = base.getClass().getName() + "." + f.getName();
                try {
                    f.setAccessible(true);
                    Object o = f.get(base);
                    if (o instanceof Watchable) {
                        Watchable attr = (Watchable) o;
                        if (attr != null && !attr.isRegisteredObserver(observer)) {
                            handleVerifyError("Watchable " + fn + " missing an observer", logOnly);
                        }
                    } else if (!annotation.manual()) {
                        handleVerifyError("@Watched annotated field " + fn + " is not a watchable type and is not flagged for manual watching.", logOnly);
                    }
                } catch (IllegalAccessException e) {
                    handleVerifyError("Watchable " + fn + " not visible", logOnly);
                }
            }
        }
    }

    static void handleVerifyError(String errorMessage, boolean logOnly) {
        if (logOnly) {
            Log.e("Watchable", errorMessage);
            return;
        }
        throw new RuntimeException(errorMessage);
    }

    static void verifyWatchedAttributes(Object base, Watcher observer) {
        verifyWatchedAttributes(base, observer, false);
    }
}
