package com.android.server.am;

import com.android.server.wm.WindowManagerService;
/* compiled from: D8$$SyntheticClass */
/* loaded from: classes.dex */
public final /* synthetic */ class ActivityManagerService$$ExternalSyntheticLambda12 implements Runnable {
    public final /* synthetic */ WindowManagerService f$0;

    /* JADX DEBUG: Marked for inline */
    /* JADX DEBUG: Method not inlined, still used in: [com.android.server.am.ActivityManagerService.setWindowManager(com.android.server.wm.WindowManagerService):void, com.android.server.am.ActivityManagerService.updateApplicationInfoLOSP(java.util.List<java.lang.String>, boolean, int):void] */
    public /* synthetic */ ActivityManagerService$$ExternalSyntheticLambda12(WindowManagerService windowManagerService) {
        this.f$0 = windowManagerService;
    }

    @Override // java.lang.Runnable
    public final void run() {
        this.f$0.onOverlayChanged();
    }
}
