package com.android.server.location.injector;

import android.app.AppOpsManager;
import android.content.Context;
import android.location.util.identity.CallerIdentity;
import android.os.Binder;
import android.util.Log;
import com.android.internal.util.Preconditions;
import com.android.server.FgThread;
import com.android.server.location.LocationManagerService;
import java.util.Objects;
/* loaded from: classes.dex */
public class SystemAppOpsHelper extends AppOpsHelper {
    private AppOpsManager mAppOps;
    private final Context mContext;

    public SystemAppOpsHelper(Context context) {
        this.mContext = context;
    }

    public void onSystemReady() {
        if (this.mAppOps != null) {
            return;
        }
        AppOpsManager appOpsManager = (AppOpsManager) Objects.requireNonNull((AppOpsManager) this.mContext.getSystemService(AppOpsManager.class));
        this.mAppOps = appOpsManager;
        appOpsManager.startWatchingMode(0, (String) null, 1, new AppOpsManager.OnOpChangedListener() { // from class: com.android.server.location.injector.SystemAppOpsHelper$$ExternalSyntheticLambda1
            @Override // android.app.AppOpsManager.OnOpChangedListener
            public final void onOpChanged(String str, String str2) {
                SystemAppOpsHelper.this.m4507x4b4202b0(str, str2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onSystemReady$0$com-android-server-location-injector-SystemAppOpsHelper  reason: not valid java name */
    public /* synthetic */ void m4506x25adf9af(String packageName) {
        notifyAppOpChanged(packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onSystemReady$1$com-android-server-location-injector-SystemAppOpsHelper  reason: not valid java name */
    public /* synthetic */ void m4507x4b4202b0(String op, final String packageName) {
        FgThread.getHandler().post(new Runnable() { // from class: com.android.server.location.injector.SystemAppOpsHelper$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                SystemAppOpsHelper.this.m4506x25adf9af(packageName);
            }
        });
    }

    @Override // com.android.server.location.injector.AppOpsHelper
    public boolean startOpNoThrow(int appOp, CallerIdentity callerIdentity) {
        Preconditions.checkState(this.mAppOps != null);
        long identity = Binder.clearCallingIdentity();
        try {
            return this.mAppOps.startOpNoThrow(appOp, callerIdentity.getUid(), callerIdentity.getPackageName(), false, callerIdentity.getAttributionTag(), callerIdentity.getListenerId()) == 0;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    @Override // com.android.server.location.injector.AppOpsHelper
    public void finishOp(int appOp, CallerIdentity callerIdentity) {
        Preconditions.checkState(this.mAppOps != null);
        long identity = Binder.clearCallingIdentity();
        try {
            this.mAppOps.finishOp(appOp, callerIdentity.getUid(), callerIdentity.getPackageName(), callerIdentity.getAttributionTag());
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    @Override // com.android.server.location.injector.AppOpsHelper
    public boolean checkOpNoThrow(int appOp, CallerIdentity callerIdentity) {
        Preconditions.checkState(this.mAppOps != null);
        long identity = Binder.clearCallingIdentity();
        try {
            return this.mAppOps.checkOpNoThrow(appOp, callerIdentity.getUid(), callerIdentity.getPackageName()) == 0;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    @Override // com.android.server.location.injector.AppOpsHelper
    public boolean noteOp(int appOp, CallerIdentity callerIdentity) {
        Preconditions.checkState(this.mAppOps != null);
        long identity = Binder.clearCallingIdentity();
        try {
            return this.mAppOps.noteOp(appOp, callerIdentity.getUid(), callerIdentity.getPackageName(), callerIdentity.getAttributionTag(), callerIdentity.getListenerId()) == 0;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    @Override // com.android.server.location.injector.AppOpsHelper
    public boolean noteOpNoThrow(int appOp, CallerIdentity callerIdentity) {
        Preconditions.checkState(this.mAppOps != null);
        long identity = Binder.clearCallingIdentity();
        try {
            return this.mAppOps.noteOpNoThrow(appOp, callerIdentity.getUid(), callerIdentity.getPackageName(), callerIdentity.getAttributionTag(), callerIdentity.getListenerId()) == 0;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public boolean mtkCheckCoarseLocationAccess(CallerIdentity callerIdentity) {
        boolean ret;
        synchronized (this) {
            ret = true;
            Preconditions.checkState(this.mAppOps != null);
        }
        long identity = Binder.clearCallingIdentity();
        try {
            int mode = this.mAppOps.checkOpNoThrow(0, callerIdentity.getUid(), callerIdentity.getPackageName());
            if (mode != 0) {
                ret = false;
            }
            if (LocationManagerService.D) {
                Log.v(LocationManagerService.TAG, "AppOpsHelper.mtkCheckCoarseLocationAccess uid=" + callerIdentity.getUid() + " appop=0 package=" + callerIdentity.getPackageName() + " mode=" + mode + " ret=" + ret);
            }
            return ret;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public boolean mtkNoteCoarseLocationAccess(CallerIdentity callerIdentity) {
        boolean ret;
        synchronized (this) {
            ret = true;
            Preconditions.checkState(this.mAppOps != null);
        }
        long identity = Binder.clearCallingIdentity();
        try {
            int mode = this.mAppOps.noteOpNoThrow(0, callerIdentity.getUid(), callerIdentity.getPackageName(), callerIdentity.getAttributionTag(), callerIdentity.getListenerId());
            if (mode != 0) {
                ret = false;
            }
            if (LocationManagerService.D) {
                Log.v(LocationManagerService.TAG, "AppOpsHelper.mtkNoteCoarseLocationAccess uid=" + callerIdentity.getUid() + " appop=0 package=" + callerIdentity.getPackageName() + " mode=" + mode + " ret=" + ret);
            }
            return ret;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }
}
