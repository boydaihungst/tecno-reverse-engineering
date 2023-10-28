package com.android.server.wm;

import android.graphics.Rect;
import java.util.function.Consumer;
/* loaded from: classes2.dex */
public class DockedTaskDividerController {
    private final DisplayContent mDisplayContent;
    private boolean mResizing;
    private final Rect mTouchRegion = new Rect();

    /* JADX INFO: Access modifiers changed from: package-private */
    public DockedTaskDividerController(DisplayContent displayContent) {
        this.mDisplayContent = displayContent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isResizing() {
        return this.mResizing;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setResizing(boolean resizing) {
        if (this.mResizing != resizing) {
            this.mResizing = resizing;
            resetDragResizingChangeReported();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTouchRegion(Rect touchRegion) {
        this.mTouchRegion.set(touchRegion);
        if (this.mDisplayContent.mWmService.mAccessibilityController.hasCallbacks()) {
            this.mDisplayContent.mWmService.mAccessibilityController.onSomeWindowResizedOrMoved(this.mDisplayContent.getDisplayId());
        }
    }

    void getTouchRegion(Rect outRegion) {
        outRegion.set(this.mTouchRegion);
    }

    private void resetDragResizingChangeReported() {
        this.mDisplayContent.forAllWindows(new Consumer() { // from class: com.android.server.wm.DockedTaskDividerController$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((WindowState) obj).resetDragResizingChangeReported();
            }
        }, true);
    }
}
