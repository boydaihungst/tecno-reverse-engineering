package com.android.server.location.injector;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.util.identity.CallerIdentity;
import android.os.Binder;
import com.android.server.FgThread;
/* loaded from: classes.dex */
public class SystemLocationPermissionsHelper extends LocationPermissionsHelper {
    private final Context mContext;
    private boolean mInited;

    public SystemLocationPermissionsHelper(Context context, AppOpsHelper appOps) {
        super(appOps);
        this.mContext = context;
    }

    public void onSystemReady() {
        if (this.mInited) {
            return;
        }
        this.mContext.getPackageManager().addOnPermissionsChangeListener(new PackageManager.OnPermissionsChangedListener() { // from class: com.android.server.location.injector.SystemLocationPermissionsHelper$$ExternalSyntheticLambda0
            public final void onPermissionsChanged(int i) {
                SystemLocationPermissionsHelper.this.m4509x8bfd8eec(i);
            }
        });
        this.mInited = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onSystemReady$0$com-android-server-location-injector-SystemLocationPermissionsHelper  reason: not valid java name */
    public /* synthetic */ void m4508x531d2e4d(int uid) {
        notifyLocationPermissionsChanged(uid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onSystemReady$1$com-android-server-location-injector-SystemLocationPermissionsHelper  reason: not valid java name */
    public /* synthetic */ void m4509x8bfd8eec(final int uid) {
        FgThread.getHandler().post(new Runnable() { // from class: com.android.server.location.injector.SystemLocationPermissionsHelper$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                SystemLocationPermissionsHelper.this.m4508x531d2e4d(uid);
            }
        });
    }

    @Override // com.android.server.location.injector.LocationPermissionsHelper
    protected boolean hasPermission(String permission, CallerIdentity callerIdentity) {
        long identity = Binder.clearCallingIdentity();
        try {
            return this.mContext.checkPermission(permission, callerIdentity.getPid(), callerIdentity.getUid()) == 0;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }
}
