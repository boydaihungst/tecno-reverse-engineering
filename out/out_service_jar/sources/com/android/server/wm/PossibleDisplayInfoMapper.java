package com.android.server.wm;

import android.hardware.display.DisplayManagerInternal;
import android.util.ArraySet;
import android.util.SparseArray;
import android.view.DisplayInfo;
import java.util.Set;
/* loaded from: classes2.dex */
public class PossibleDisplayInfoMapper {
    private static final boolean DEBUG = false;
    private static final String TAG = "PossibleDisplayInfoMapper";
    private final SparseArray<Set<DisplayInfo>> mDisplayInfos = new SparseArray<>();
    private final DisplayManagerInternal mDisplayManagerInternal;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PossibleDisplayInfoMapper(DisplayManagerInternal displayManagerInternal) {
        this.mDisplayManagerInternal = displayManagerInternal;
    }

    public Set<DisplayInfo> getPossibleDisplayInfos(int displayId) {
        updatePossibleDisplayInfos(displayId);
        if (!this.mDisplayInfos.contains(displayId)) {
            return new ArraySet();
        }
        return Set.copyOf(this.mDisplayInfos.get(displayId));
    }

    public void updatePossibleDisplayInfos(int displayId) {
        Set<DisplayInfo> displayInfos = this.mDisplayManagerInternal.getPossibleDisplayInfo(displayId);
        updateDisplayInfos(displayInfos);
    }

    public void removePossibleDisplayInfos(int displayId) {
        this.mDisplayInfos.remove(displayId);
    }

    private void updateDisplayInfos(Set<DisplayInfo> displayInfos) {
        this.mDisplayInfos.clear();
        for (DisplayInfo di : displayInfos) {
            Set<DisplayInfo> priorDisplayInfos = this.mDisplayInfos.get(di.displayId, new ArraySet());
            priorDisplayInfos.add(di);
            this.mDisplayInfos.put(di.displayId, priorDisplayInfos);
        }
    }
}
