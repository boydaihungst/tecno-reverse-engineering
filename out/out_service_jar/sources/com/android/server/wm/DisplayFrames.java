package com.android.server.wm;

import android.graphics.Rect;
import android.util.proto.ProtoOutputStream;
import android.view.DisplayCutout;
import android.view.DisplayInfo;
import android.view.InsetsState;
import android.view.PrivacyIndicatorBounds;
import android.view.RoundedCorners;
import com.android.server.wm.utils.WmDisplayCutout;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
public class DisplayFrames {
    public int mDisplayHeight;
    public final int mDisplayId;
    public int mDisplayWidth;
    public final InsetsState mInsetsState;
    public int mRotation;
    public final Rect mUnrestricted = new Rect();
    public final Rect mDisplayCutoutSafe = new Rect();

    public DisplayFrames(int displayId, InsetsState insetsState, DisplayInfo info, WmDisplayCutout displayCutout, RoundedCorners roundedCorners, PrivacyIndicatorBounds indicatorBounds) {
        this.mDisplayId = displayId;
        this.mInsetsState = insetsState;
        update(info, displayCutout, roundedCorners, indicatorBounds);
    }

    public boolean update(DisplayInfo info, WmDisplayCutout displayCutout, RoundedCorners roundedCorners, PrivacyIndicatorBounds indicatorBounds) {
        InsetsState state = this.mInsetsState;
        Rect safe = this.mDisplayCutoutSafe;
        DisplayCutout cutout = displayCutout.getDisplayCutout();
        if (this.mDisplayWidth == info.logicalWidth && this.mDisplayHeight == info.logicalHeight && this.mRotation == info.rotation && state.getDisplayCutout().equals(cutout) && state.getRoundedCorners().equals(roundedCorners) && state.getPrivacyIndicatorBounds().equals(indicatorBounds)) {
            return false;
        }
        this.mDisplayWidth = info.logicalWidth;
        this.mDisplayHeight = info.logicalHeight;
        this.mRotation = info.rotation;
        Rect unrestricted = this.mUnrestricted;
        unrestricted.set(0, 0, this.mDisplayWidth, this.mDisplayHeight);
        state.setDisplayFrame(unrestricted);
        state.setDisplayCutout(cutout);
        state.setRoundedCorners(roundedCorners);
        state.setPrivacyIndicatorBounds(indicatorBounds);
        state.getDisplayCutoutSafe(safe);
        if (!cutout.isEmpty()) {
            state.getSource(11).setFrame(unrestricted.left, unrestricted.top, safe.left, unrestricted.bottom);
            state.getSource(12).setFrame(unrestricted.left, unrestricted.top, unrestricted.right, safe.top);
            state.getSource(13).setFrame(safe.right, unrestricted.top, unrestricted.right, unrestricted.bottom);
            state.getSource(14).setFrame(unrestricted.left, safe.bottom, unrestricted.right, unrestricted.bottom);
            return true;
        }
        state.removeSource(11);
        state.removeSource(12);
        state.removeSource(13);
        state.removeSource(14);
        return true;
    }

    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.end(token);
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.println(prefix + "DisplayFrames w=" + this.mDisplayWidth + " h=" + this.mDisplayHeight + " r=" + this.mRotation);
    }
}
