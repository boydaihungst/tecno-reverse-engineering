package com.android.server.inputmethod;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Slog;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
/* loaded from: classes.dex */
final class OverlayableSystemBooleanResourceWrapper implements AutoCloseable {
    private static final String SYSTEM_PACKAGE_NAME = "android";
    private static final String TAG = "OverlayableSystemBooleanResourceWrapper";
    private final AtomicReference<Runnable> mCleanerRef;
    private final int mUserId;
    private final AtomicBoolean mValueRef;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static OverlayableSystemBooleanResourceWrapper create(final Context userContext, final int boolResId, Handler handler, final Consumer<OverlayableSystemBooleanResourceWrapper> callback) {
        final AtomicBoolean valueRef = new AtomicBoolean(evaluate(userContext, boolResId));
        AtomicReference<Runnable> cleanerRef = new AtomicReference<>();
        final OverlayableSystemBooleanResourceWrapper object = new OverlayableSystemBooleanResourceWrapper(userContext.getUserId(), valueRef, cleanerRef);
        IntentFilter intentFilter = new IntentFilter("android.intent.action.OVERLAY_CHANGED");
        intentFilter.addDataScheme("package");
        intentFilter.addDataSchemeSpecificPart("android", 0);
        final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.inputmethod.OverlayableSystemBooleanResourceWrapper.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                boolean newValue = OverlayableSystemBooleanResourceWrapper.evaluate(userContext, boolResId);
                if (newValue != valueRef.getAndSet(newValue)) {
                    callback.accept(object);
                }
            }
        };
        userContext.registerReceiver(broadcastReceiver, intentFilter, null, handler, 4);
        cleanerRef.set(new Runnable() { // from class: com.android.server.inputmethod.OverlayableSystemBooleanResourceWrapper$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                userContext.unregisterReceiver(broadcastReceiver);
            }
        });
        valueRef.set(evaluate(userContext, boolResId));
        return object;
    }

    private OverlayableSystemBooleanResourceWrapper(int userId, AtomicBoolean valueRef, AtomicReference<Runnable> cleanerRef) {
        this.mUserId = userId;
        this.mValueRef = valueRef;
        this.mCleanerRef = cleanerRef;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean get() {
        return this.mValueRef.get();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getUserId() {
        return this.mUserId;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean evaluate(Context context, int boolResId) {
        try {
            return context.getPackageManager().getResourcesForApplication("android").getBoolean(boolResId);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "getResourcesForApplication(\"android\") failed", e);
            return false;
        }
    }

    @Override // java.lang.AutoCloseable
    public void close() {
        Runnable cleaner = this.mCleanerRef.getAndSet(null);
        if (cleaner != null) {
            cleaner.run();
        }
    }
}
