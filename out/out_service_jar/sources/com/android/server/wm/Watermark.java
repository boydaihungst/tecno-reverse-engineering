package com.android.server.wm;

import android.graphics.BLASTBufferQueue;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.SurfaceControl;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class Watermark {
    private static final String TITLE = "WatermarkSurface";
    private final BLASTBufferQueue mBlastBufferQueue;
    private final int mDeltaX;
    private final int mDeltaY;
    private boolean mDrawNeeded;
    private int mLastDH;
    private int mLastDW;
    private final Surface mSurface;
    private final SurfaceControl mSurfaceControl;
    private final String mText;
    private final int mTextHeight;
    private final Paint mTextPaint;
    private final int mTextWidth;

    /* JADX INFO: Access modifiers changed from: package-private */
    public Watermark(DisplayContent dc, DisplayMetrics dm, String[] tokens, SurfaceControl.Transaction t) {
        int c1;
        int c2;
        StringBuilder builder = new StringBuilder(32);
        int len = tokens[0].length();
        int len2 = len & (-2);
        for (int i = 0; i < len2; i += 2) {
            int c12 = tokens[0].charAt(i);
            int c22 = tokens[0].charAt(i + 1);
            if (c12 < 97 || c12 > 102) {
                c1 = (c12 < 65 || c12 > 70) ? c12 - 48 : (c12 - 65) + 10;
            } else {
                c1 = (c12 - 97) + 10;
            }
            if (c22 < 97 || c22 > 102) {
                c2 = (c22 < 65 || c22 > 70) ? c22 - 48 : (c22 - 65) + 10;
            } else {
                c2 = (c22 - 97) + 10;
            }
            builder.append((char) (255 - ((c1 * 16) + c2)));
        }
        String sb = builder.toString();
        this.mText = sb;
        int fontSize = WindowManagerService.getPropertyInt(tokens, 1, 1, 20, dm);
        Paint paint = new Paint(1);
        this.mTextPaint = paint;
        paint.setTextSize(fontSize);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, 1));
        Paint.FontMetricsInt fm = paint.getFontMetricsInt();
        int measureText = (int) paint.measureText(sb);
        this.mTextWidth = measureText;
        int i2 = fm.descent - fm.ascent;
        this.mTextHeight = i2;
        this.mDeltaX = WindowManagerService.getPropertyInt(tokens, 2, 0, measureText * 2, dm);
        this.mDeltaY = WindowManagerService.getPropertyInt(tokens, 3, 0, i2 * 3, dm);
        int shadowColor = WindowManagerService.getPropertyInt(tokens, 4, 0, -1342177280, dm);
        int color = WindowManagerService.getPropertyInt(tokens, 5, 0, 1627389951, dm);
        int shadowRadius = WindowManagerService.getPropertyInt(tokens, 6, 0, 7, dm);
        int shadowDx = WindowManagerService.getPropertyInt(tokens, 8, 0, 0, dm);
        int shadowDy = WindowManagerService.getPropertyInt(tokens, 9, 0, 0, dm);
        paint.setColor(color);
        paint.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor);
        SurfaceControl ctrl = null;
        try {
            ctrl = dc.makeOverlay().setName(TITLE).setBLASTLayer().setFormat(-3).setCallsite(TITLE).build();
            t.setLayer(ctrl, 1000000).setPosition(ctrl, 0.0f, 0.0f).show(ctrl);
            InputMonitor.setTrustedOverlayInputInfo(ctrl, t, dc.getDisplayId(), TITLE);
        } catch (Surface.OutOfResourcesException e) {
        }
        this.mSurfaceControl = ctrl;
        BLASTBufferQueue bLASTBufferQueue = new BLASTBufferQueue(TITLE, ctrl, 1, 1, 1);
        this.mBlastBufferQueue = bLASTBufferQueue;
        this.mSurface = bLASTBufferQueue.createSurface();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void positionSurface(int dw, int dh, SurfaceControl.Transaction t) {
        if (this.mLastDW != dw || this.mLastDH != dh) {
            this.mLastDW = dw;
            this.mLastDH = dh;
            t.setBufferSize(this.mSurfaceControl, dw, dh);
            this.mDrawNeeded = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void drawIfNeeded() {
        if (!this.mDrawNeeded) {
            return;
        }
        int dw = this.mLastDW;
        int dh = this.mLastDH;
        this.mDrawNeeded = false;
        this.mBlastBufferQueue.update(this.mSurfaceControl, dw, dh, 1);
        Canvas c = null;
        try {
            c = this.mSurface.lockCanvas(null);
        } catch (Surface.OutOfResourcesException | IllegalArgumentException e) {
        }
        if (c != null) {
            c.drawColor(0, PorterDuff.Mode.CLEAR);
            int deltaX = this.mDeltaX;
            int deltaY = this.mDeltaY;
            int i = this.mTextWidth;
            int div = (dw + i) / deltaX;
            int rem = (dw + i) - (div * deltaX);
            int qdelta = deltaX / 4;
            if (rem < qdelta || rem > deltaX - qdelta) {
                deltaX += deltaX / 3;
            }
            int y = -this.mTextHeight;
            int x = -i;
            while (y < this.mTextHeight + dh) {
                c.drawText(this.mText, x, y, this.mTextPaint);
                x += deltaX;
                if (x >= dw) {
                    x -= this.mTextWidth + dw;
                    y += deltaY;
                }
            }
            this.mSurface.unlockCanvasAndPost(c);
        }
    }
}
