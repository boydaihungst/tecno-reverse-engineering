package com.android.server.wm.utils;

import android.graphics.Rect;
import android.util.Size;
import android.view.DisplayCutout;
import java.util.Objects;
/* loaded from: classes2.dex */
public class WmDisplayCutout {
    public static final WmDisplayCutout NO_CUTOUT = new WmDisplayCutout(DisplayCutout.NO_CUTOUT, null);
    private final Size mFrameSize;
    private final DisplayCutout mInner;

    public WmDisplayCutout(DisplayCutout inner, Size frameSize) {
        this.mInner = inner;
        this.mFrameSize = frameSize;
    }

    public static WmDisplayCutout computeSafeInsets(DisplayCutout inner, int displayWidth, int displayHeight) {
        if (inner == DisplayCutout.NO_CUTOUT) {
            return NO_CUTOUT;
        }
        Size displaySize = new Size(displayWidth, displayHeight);
        Rect safeInsets = DisplayCutout.computeSafeInsets(displayWidth, displayHeight, inner);
        return new WmDisplayCutout(inner.replaceSafeInsets(safeInsets), displaySize);
    }

    public WmDisplayCutout computeSafeInsets(int width, int height) {
        return computeSafeInsets(this.mInner, width, height);
    }

    public DisplayCutout getDisplayCutout() {
        return this.mInner;
    }

    public boolean equals(Object o) {
        if (o instanceof WmDisplayCutout) {
            WmDisplayCutout that = (WmDisplayCutout) o;
            return Objects.equals(this.mInner, that.mInner) && Objects.equals(this.mFrameSize, that.mFrameSize);
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.mInner, this.mFrameSize);
    }

    public String toString() {
        return "WmDisplayCutout{" + this.mInner + ", mFrameSize=" + this.mFrameSize + '}';
    }
}
