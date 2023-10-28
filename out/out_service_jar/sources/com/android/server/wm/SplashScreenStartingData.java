package com.android.server.wm;

import com.android.server.wm.StartingSurfaceController;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class SplashScreenStartingData extends StartingData {
    private final int mTheme;

    /* JADX INFO: Access modifiers changed from: package-private */
    public SplashScreenStartingData(WindowManagerService service, int theme, int typeParams) {
        super(service, typeParams);
        this.mTheme = theme;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.StartingData
    public StartingSurfaceController.StartingSurface createStartingSurface(ActivityRecord activity) {
        return this.mService.mStartingSurfaceController.createSplashScreenStartingSurface(activity, this.mTheme);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.StartingData
    public boolean needRevealAnimation() {
        return true;
    }
}
