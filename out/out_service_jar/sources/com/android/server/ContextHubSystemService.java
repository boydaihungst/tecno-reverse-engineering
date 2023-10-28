package com.android.server;

import android.content.Context;
import android.util.Log;
import com.android.internal.util.ConcurrentUtils;
import com.android.server.SystemService;
import com.android.server.location.contexthub.ContextHubService;
import java.util.concurrent.Future;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class ContextHubSystemService extends SystemService {
    private static final String TAG = "ContextHubSystemService";
    private ContextHubService mContextHubService;
    private Future<?> mInit;

    public ContextHubSystemService(final Context context) {
        super(context);
        this.mInit = SystemServerInitThreadPool.submit(new Runnable() { // from class: com.android.server.ContextHubSystemService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ContextHubSystemService.this.m107lambda$new$0$comandroidserverContextHubSystemService(context);
            }
        }, "Init ContextHubSystemService");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-ContextHubSystemService  reason: not valid java name */
    public /* synthetic */ void m107lambda$new$0$comandroidserverContextHubSystemService(Context context) {
        this.mContextHubService = new ContextHubService(context);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 500) {
            Log.d(TAG, "onBootPhase: PHASE_SYSTEM_SERVICES_READY");
            ConcurrentUtils.waitForFutureNoInterrupt(this.mInit, "Wait for ContextHubSystemService init");
            this.mInit = null;
            publishBinderService("contexthub", this.mContextHubService);
        }
    }

    @Override // com.android.server.SystemService
    public void onUserSwitching(SystemService.TargetUser from, SystemService.TargetUser to) {
        this.mContextHubService.onUserChanged();
    }
}
