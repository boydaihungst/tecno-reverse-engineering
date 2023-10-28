package com.android.server.wm;

import android.graphics.Rect;
import android.util.proto.ProtoOutputStream;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
public class WindowFrames {
    private static final StringBuilder sTmpSB = new StringBuilder();
    private boolean mContentChanged;
    private boolean mInsetsChanged;
    private boolean mParentFrameWasClippedByDisplayCutout;
    public final Rect mParentFrame = new Rect();
    public final Rect mDisplayFrame = new Rect();
    final Rect mFrame = new Rect();
    final Rect mLastFrame = new Rect();
    final Rect mRelFrame = new Rect();
    final Rect mLastRelFrame = new Rect();
    private boolean mFrameSizeChanged = false;
    final Rect mCompatFrame = new Rect();
    boolean mLastForceReportingResized = false;
    boolean mForceReportingResized = false;

    public void setFrames(Rect parentFrame, Rect displayFrame) {
        this.mParentFrame.set(parentFrame);
        this.mDisplayFrame.set(displayFrame);
    }

    public void setParentFrameWasClippedByDisplayCutout(boolean parentFrameWasClippedByDisplayCutout) {
        this.mParentFrameWasClippedByDisplayCutout = parentFrameWasClippedByDisplayCutout;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean parentFrameWasClippedByDisplayCutout() {
        return this.mParentFrameWasClippedByDisplayCutout;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean didFrameSizeChange() {
        return (this.mLastFrame.width() == this.mFrame.width() && this.mLastFrame.height() == this.mFrame.height()) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setReportResizeHints() {
        this.mLastForceReportingResized |= this.mForceReportingResized;
        boolean didFrameSizeChange = this.mFrameSizeChanged | didFrameSizeChange();
        this.mFrameSizeChanged = didFrameSizeChange;
        return this.mLastForceReportingResized || didFrameSizeChange;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFrameSizeChangeReported() {
        return this.mFrameSizeChanged || didFrameSizeChange();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearReportResizeHints() {
        this.mLastForceReportingResized = false;
        this.mFrameSizeChanged = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onResizeHandled() {
        this.mForceReportingResized = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forceReportingResized() {
        this.mForceReportingResized = true;
    }

    public void setContentChanged(boolean contentChanged) {
        this.mContentChanged = contentChanged;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasContentChanged() {
        return this.mContentChanged;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setInsetsChanged(boolean insetsChanged) {
        this.mInsetsChanged = insetsChanged;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasInsetsChanged() {
        return this.mInsetsChanged;
    }

    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        this.mParentFrame.dumpDebug(proto, 1146756268040L);
        this.mDisplayFrame.dumpDebug(proto, 1146756268036L);
        this.mFrame.dumpDebug(proto, 1146756268037L);
        this.mCompatFrame.dumpDebug(proto, 1146756268048L);
        proto.end(token);
    }

    public void dump(PrintWriter pw, String prefix) {
        StringBuilder append = new StringBuilder().append(prefix).append("Frames: parent=");
        Rect rect = this.mParentFrame;
        StringBuilder sb = sTmpSB;
        pw.println(append.append(rect.toShortString(sb)).append(" display=").append(this.mDisplayFrame.toShortString(sb)).append(" frame=").append(this.mFrame.toShortString(sb)).append(" last=").append(this.mLastFrame.toShortString(sb)).append(" insetsChanged=").append(this.mInsetsChanged).toString());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getInsetsChangedInfo() {
        return "forceReportingResized=" + this.mLastForceReportingResized;
    }
}
