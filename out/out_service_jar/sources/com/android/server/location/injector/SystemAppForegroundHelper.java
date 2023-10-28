package com.android.server.location.injector;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Binder;
import com.android.internal.util.Preconditions;
import com.android.server.FgThread;
import java.util.Objects;
/* loaded from: classes.dex */
public class SystemAppForegroundHelper extends AppForegroundHelper {
    private ActivityManager mActivityManager;
    private final Context mContext;

    public SystemAppForegroundHelper(Context context) {
        this.mContext = context;
    }

    public void onSystemReady() {
        if (this.mActivityManager != null) {
            return;
        }
        ActivityManager activityManager = (ActivityManager) Objects.requireNonNull((ActivityManager) this.mContext.getSystemService(ActivityManager.class));
        this.mActivityManager = activityManager;
        activityManager.addOnUidImportanceListener(new ActivityManager.OnUidImportanceListener() { // from class: com.android.server.location.injector.SystemAppForegroundHelper$$ExternalSyntheticLambda1
            public final void onUidImportance(int i, int i2) {
                SystemAppForegroundHelper.this.onAppForegroundChanged(i, i2);
            }
        }, 125);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onAppForegroundChanged(final int uid, int importance) {
        final boolean foreground = isForeground(importance);
        FgThread.getHandler().post(new Runnable() { // from class: com.android.server.location.injector.SystemAppForegroundHelper$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                SystemAppForegroundHelper.this.m4505xcd39532e(uid, foreground);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onAppForegroundChanged$0$com-android-server-location-injector-SystemAppForegroundHelper  reason: not valid java name */
    public /* synthetic */ void m4505xcd39532e(int uid, boolean foreground) {
        notifyAppForeground(uid, foreground);
    }

    @Override // com.android.server.location.injector.AppForegroundHelper
    public boolean isAppForeground(int uid) {
        Preconditions.checkState(this.mActivityManager != null);
        long identity = Binder.clearCallingIdentity();
        try {
            return isForeground(this.mActivityManager.getUidImportance(uid));
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }
}
