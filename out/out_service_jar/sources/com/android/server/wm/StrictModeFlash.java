package com.android.server.wm;

import android.graphics.BLASTBufferQueue;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.Surface;
import android.view.SurfaceControl;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class StrictModeFlash {
    private static final String TAG = "WindowManager";
    private static final String TITLE = "StrictModeFlash";
    private final BLASTBufferQueue mBlastBufferQueue;
    private boolean mDrawNeeded;
    private int mLastDH;
    private int mLastDW;
    private final Surface mSurface;
    private final SurfaceControl mSurfaceControl;
    private final int mThickness = 20;

    /* JADX INFO: Access modifiers changed from: package-private */
    public StrictModeFlash(DisplayContent dc, SurfaceControl.Transaction t) {
        SurfaceControl ctrl = null;
        try {
            ctrl = dc.makeOverlay().setName(TITLE).setBLASTLayer().setFormat(-3).setCallsite(TITLE).build();
            t.setLayer(ctrl, 1010000);
            t.setPosition(ctrl, 0.0f, 0.0f);
            t.show(ctrl);
            InputMonitor.setTrustedOverlayInputInfo(ctrl, t, dc.getDisplayId(), TITLE);
        } catch (Surface.OutOfResourcesException e) {
        }
        this.mSurfaceControl = ctrl;
        this.mDrawNeeded = true;
        BLASTBufferQueue bLASTBufferQueue = new BLASTBufferQueue(TITLE, ctrl, 1, 1, 1);
        this.mBlastBufferQueue = bLASTBufferQueue;
        this.mSurface = bLASTBufferQueue.createSurface();
    }

    private void drawIfNeeded() {
        if (!this.mDrawNeeded) {
            return;
        }
        this.mDrawNeeded = false;
        int dw = this.mLastDW;
        int dh = this.mLastDH;
        this.mBlastBufferQueue.update(this.mSurfaceControl, dw, dh, 1);
        Canvas c = null;
        try {
            c = this.mSurface.lockCanvas(null);
        } catch (Surface.OutOfResourcesException | IllegalArgumentException e) {
        }
        if (c == null) {
            return;
        }
        c.save();
        c.clipRect(new Rect(0, 0, dw, 20));
        c.drawColor(-65536);
        c.restore();
        c.save();
        c.clipRect(new Rect(0, 0, 20, dh));
        c.drawColor(-65536);
        c.restore();
        c.save();
        c.clipRect(new Rect(dw - 20, 0, dw, dh));
        c.drawColor(-65536);
        c.restore();
        c.save();
        c.clipRect(new Rect(0, dh - 20, dw, dh));
        c.drawColor(-65536);
        c.restore();
        this.mSurface.unlockCanvasAndPost(c);
    }

    public void setVisibility(boolean on, SurfaceControl.Transaction t) {
        if (this.mSurfaceControl == null) {
            return;
        }
        drawIfNeeded();
        if (on) {
            t.show(this.mSurfaceControl);
        } else {
            t.hide(this.mSurfaceControl);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void positionSurface(int dw, int dh, SurfaceControl.Transaction t) {
        if (this.mLastDW == dw && this.mLastDH == dh) {
            return;
        }
        this.mLastDW = dw;
        this.mLastDH = dh;
        t.setBufferSize(this.mSurfaceControl, dw, dh);
        this.mDrawNeeded = true;
    }
}
