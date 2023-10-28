package com.android.server.wm;

import android.window.TaskSnapshot;
import com.android.server.wm.StartingSurfaceController;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class SnapshotStartingData extends StartingData {
    private final WindowManagerService mService;
    private final TaskSnapshot mSnapshot;

    /* JADX INFO: Access modifiers changed from: package-private */
    public SnapshotStartingData(WindowManagerService service, TaskSnapshot snapshot, int typeParams) {
        super(service, typeParams);
        this.mService = service;
        this.mSnapshot = snapshot;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.StartingData
    public StartingSurfaceController.StartingSurface createStartingSurface(ActivityRecord activity) {
        return this.mService.mStartingSurfaceController.createTaskSnapshotSurface(activity, this.mSnapshot);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.StartingData
    public boolean needRevealAnimation() {
        return false;
    }

    @Override // com.android.server.wm.StartingData
    boolean hasImeSurface() {
        return this.mSnapshot.hasImeSurface();
    }
}
