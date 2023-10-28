package com.android.net.module.util;

import android.os.Binder;
/* loaded from: classes4.dex */
public class BinderUtils {

    @FunctionalInterface
    /* loaded from: classes4.dex */
    public interface ThrowingRunnable<T extends Exception> {
        void run() throws Exception;
    }

    public static final <T extends Exception> void withCleanCallingIdentity(ThrowingRunnable<T> action) throws Exception {
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            action.run();
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }
}
