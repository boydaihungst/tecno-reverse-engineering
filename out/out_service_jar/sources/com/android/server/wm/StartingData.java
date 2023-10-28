package com.android.server.wm;

import com.android.server.wm.StartingSurfaceController;
/* loaded from: classes2.dex */
public abstract class StartingData {
    Task mAssociatedTask;
    boolean mIsDisplayed;
    boolean mIsTransitionForward;
    protected final WindowManagerService mService;
    protected final int mTypeParams;

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract StartingSurfaceController.StartingSurface createStartingSurface(ActivityRecord activityRecord);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract boolean needRevealAnimation();

    /* JADX INFO: Access modifiers changed from: protected */
    public StartingData(WindowManagerService service, int typeParams) {
        this.mService = service;
        this.mTypeParams = typeParams;
    }

    boolean hasImeSurface() {
        return false;
    }
}
