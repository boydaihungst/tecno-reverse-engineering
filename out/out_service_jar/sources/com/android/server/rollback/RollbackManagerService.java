package com.android.server.rollback;

import android.content.Context;
import com.android.server.LocalServices;
import com.android.server.SystemService;
/* loaded from: classes2.dex */
public final class RollbackManagerService extends SystemService {
    private RollbackManagerServiceImpl mService;

    public RollbackManagerService(Context context) {
        super(context);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: com.android.server.rollback.RollbackManagerService */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v0, types: [com.android.server.rollback.RollbackManagerServiceImpl, android.os.IBinder] */
    @Override // com.android.server.SystemService
    public void onStart() {
        ?? rollbackManagerServiceImpl = new RollbackManagerServiceImpl(getContext());
        this.mService = rollbackManagerServiceImpl;
        publishBinderService("rollback", rollbackManagerServiceImpl);
        LocalServices.addService(RollbackManagerInternal.class, this.mService);
    }

    @Override // com.android.server.SystemService
    public void onUserUnlocking(SystemService.TargetUser user) {
        this.mService.onUnlockUser(user.getUserIdentifier());
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 1000) {
            this.mService.onBootCompleted();
        }
    }
}
