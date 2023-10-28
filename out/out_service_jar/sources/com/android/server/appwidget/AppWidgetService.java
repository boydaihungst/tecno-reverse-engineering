package com.android.server.appwidget;

import android.content.Context;
import com.android.server.AppWidgetBackupBridge;
import com.android.server.SystemService;
/* loaded from: classes.dex */
public class AppWidgetService extends SystemService {
    private final AppWidgetServiceImpl mImpl;

    public AppWidgetService(Context context) {
        super(context);
        this.mImpl = new AppWidgetServiceImpl(context);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        this.mImpl.onStart();
        publishBinderService("appwidget", this.mImpl);
        AppWidgetBackupBridge.register(this.mImpl);
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 550) {
            this.mImpl.setSafeMode(isSafeMode());
            this.mImpl.systemServicesReady();
        }
    }

    @Override // com.android.server.SystemService
    public void onUserStopping(SystemService.TargetUser user) {
        this.mImpl.onUserStopped(user.getUserIdentifier());
    }

    @Override // com.android.server.SystemService
    public void onUserSwitching(SystemService.TargetUser from, SystemService.TargetUser to) {
        this.mImpl.reloadWidgetsMaskedStateForGroup(to.getUserIdentifier());
    }
}
