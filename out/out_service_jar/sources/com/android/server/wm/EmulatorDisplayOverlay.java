package com.android.server.wm;

import android.content.Context;
import android.graphics.BLASTBufferQueue;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceControl;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class EmulatorDisplayOverlay {
    private static final String TAG = "WindowManager";
    private static final String TITLE = "EmulatorDisplayOverlay";
    private final BLASTBufferQueue mBlastBufferQueue;
    private boolean mDrawNeeded;
    private int mLastDH;
    private int mLastDW;
    private final Drawable mOverlay;
    private int mRotation;
    private Point mScreenSize;
    private final Surface mSurface;
    private final SurfaceControl mSurfaceControl;
    private boolean mVisible;

    /* JADX INFO: Access modifiers changed from: package-private */
    public EmulatorDisplayOverlay(Context context, DisplayContent dc, int zOrder, SurfaceControl.Transaction t) {
        Display display = dc.getDisplay();
        Point point = new Point();
        this.mScreenSize = point;
        display.getSize(point);
        SurfaceControl ctrl = null;
        try {
            ctrl = dc.makeOverlay().setName(TITLE).setBLASTLayer().setFormat(-3).setCallsite(TITLE).build();
            t.setLayer(ctrl, zOrder);
            t.setPosition(ctrl, 0.0f, 0.0f);
            t.show(ctrl);
            InputMonitor.setTrustedOverlayInputInfo(ctrl, t, dc.getDisplayId(), TITLE);
        } catch (Surface.OutOfResourcesException e) {
        }
        this.mSurfaceControl = ctrl;
        this.mDrawNeeded = true;
        this.mOverlay = context.getDrawable(17302230);
        BLASTBufferQueue bLASTBufferQueue = new BLASTBufferQueue(TITLE, ctrl, this.mScreenSize.x, this.mScreenSize.y, 1);
        this.mBlastBufferQueue = bLASTBufferQueue;
        this.mSurface = bLASTBufferQueue.createSurface();
    }

    private void drawIfNeeded(SurfaceControl.Transaction t) {
        if (!this.mDrawNeeded || !this.mVisible) {
            return;
        }
        this.mDrawNeeded = false;
        Canvas c = null;
        try {
            c = this.mSurface.lockCanvas(null);
        } catch (Surface.OutOfResourcesException | IllegalArgumentException e) {
        }
        if (c != null) {
            c.drawColor(0, PorterDuff.Mode.SRC);
            t.setPosition(this.mSurfaceControl, 0.0f, 0.0f);
            int size = Math.max(this.mScreenSize.x, this.mScreenSize.y);
            this.mOverlay.setBounds(0, 0, size, size);
            this.mOverlay.draw(c);
            this.mSurface.unlockCanvasAndPost(c);
        }
    }

    public void setVisibility(boolean on, SurfaceControl.Transaction t) {
        if (this.mSurfaceControl == null) {
            return;
        }
        this.mVisible = on;
        drawIfNeeded(t);
        if (on) {
            t.show(this.mSurfaceControl);
        } else {
            t.hide(this.mSurfaceControl);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void positionSurface(int dw, int dh, int rotation, SurfaceControl.Transaction t) {
        if (this.mLastDW == dw && this.mLastDH == dh && this.mRotation == rotation) {
            return;
        }
        this.mLastDW = dw;
        this.mLastDH = dh;
        this.mDrawNeeded = true;
        this.mRotation = rotation;
        drawIfNeeded(t);
    }
}
