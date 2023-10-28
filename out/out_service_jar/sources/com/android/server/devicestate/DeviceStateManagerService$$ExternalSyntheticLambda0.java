package com.android.server.devicestate;
/* compiled from: D8$$SyntheticClass */
/* loaded from: classes.dex */
public final /* synthetic */ class DeviceStateManagerService$$ExternalSyntheticLambda0 implements Runnable {
    public final /* synthetic */ DeviceStateManagerService f$0;

    /* JADX DEBUG: Marked for inline */
    /* JADX DEBUG: Method not inlined, still used in: [com.android.server.devicestate.DeviceStateManagerService.commitPendingState():void, com.android.server.devicestate.DeviceStateManagerService.onOverrideRequestStatusChangedLocked(com.android.server.devicestate.OverrideRequest, int):void, com.android.server.devicestate.DeviceStateManagerService.setBaseState(int):void, com.android.server.devicestate.DeviceStateManagerService.updateSupportedStates(com.android.server.devicestate.DeviceState[]):void] */
    public /* synthetic */ DeviceStateManagerService$$ExternalSyntheticLambda0(DeviceStateManagerService deviceStateManagerService) {
        this.f$0 = deviceStateManagerService;
    }

    @Override // java.lang.Runnable
    public final void run() {
        DeviceStateManagerService.m3165$r8$lambda$wIWgFzEDmfWTNtQtPpan3TstOU(this.f$0);
    }
}
