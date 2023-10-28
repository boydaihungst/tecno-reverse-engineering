package com.android.server.wm;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
/* loaded from: classes2.dex */
class DisplayAreaGroup extends RootDisplayArea {
    DisplayAreaGroup(WindowManagerService wms, String name, int featureId) {
        super(wms, name, featureId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.RootDisplayArea
    public boolean isOrientationDifferentFromDisplay() {
        return isOrientationDifferentFromDisplay(getBounds());
    }

    private boolean isOrientationDifferentFromDisplay(Rect bounds) {
        if (this.mDisplayContent == null) {
            return false;
        }
        Rect displayBounds = this.mDisplayContent.getBounds();
        return (bounds.width() < bounds.height()) != (displayBounds.width() < displayBounds.height());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer
    public int getOrientation(int candidate) {
        int orientation = super.getOrientation(candidate);
        return isOrientationDifferentFromDisplay() ? ActivityInfo.reverseOrientation(orientation) : orientation;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.DisplayArea, com.android.server.wm.ConfigurationContainer
    public void resolveOverrideConfiguration(Configuration newParentConfiguration) {
        Rect overrideBounds;
        super.resolveOverrideConfiguration(newParentConfiguration);
        Configuration resolvedConfig = getResolvedOverrideConfiguration();
        if (resolvedConfig.orientation != 0) {
            return;
        }
        Rect overrideBounds2 = resolvedConfig.windowConfiguration.getBounds();
        if (overrideBounds2.isEmpty()) {
            overrideBounds = newParentConfiguration.windowConfiguration.getBounds();
        } else {
            overrideBounds = overrideBounds2;
        }
        if (isOrientationDifferentFromDisplay(overrideBounds)) {
            if (newParentConfiguration.orientation == 1) {
                resolvedConfig.orientation = 2;
            } else if (newParentConfiguration.orientation == 2) {
                resolvedConfig.orientation = 1;
            }
        }
    }
}
