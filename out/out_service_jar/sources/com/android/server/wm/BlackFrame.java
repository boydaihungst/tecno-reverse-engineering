package com.android.server.wm;

import android.graphics.Rect;
import android.view.Surface;
import android.view.SurfaceControl;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import java.io.PrintWriter;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public class BlackFrame {
    private final BlackSurface[] mBlackSurfaces;
    private final Rect mInnerRect;
    private final Rect mOuterRect;
    private final Supplier<SurfaceControl.Transaction> mTransactionFactory;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class BlackSurface {
        final int layer;
        final int left;
        final SurfaceControl surface;
        final int top;

        BlackSurface(SurfaceControl.Transaction transaction, int layer, int l, int t, int r, int b, DisplayContent dc, SurfaceControl surfaceControl) throws Surface.OutOfResourcesException {
            this.left = l;
            this.top = t;
            this.layer = layer;
            int w = r - l;
            int h = b - t;
            SurfaceControl build = dc.makeOverlay().setName("BlackSurface").setColorLayer().setParent(surfaceControl).setCallsite("BlackSurface").build();
            this.surface = build;
            transaction.setWindowCrop(build, w, h);
            transaction.setAlpha(build, 1.0f);
            transaction.setLayer(build, layer);
            transaction.setPosition(build, l, t);
            transaction.show(build);
            if (ProtoLogCache.WM_SHOW_SURFACE_ALLOC_enabled) {
                String protoLogParam0 = String.valueOf(build);
                long protoLogParam1 = layer;
                ProtoLogImpl.i(ProtoLogGroup.WM_SHOW_SURFACE_ALLOC, 152914409, 4, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1)});
            }
        }
    }

    public void printTo(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("Outer: ");
        this.mOuterRect.printShortString(pw);
        pw.print(" / Inner: ");
        this.mInnerRect.printShortString(pw);
        pw.println();
        int i = 0;
        while (true) {
            BlackSurface[] blackSurfaceArr = this.mBlackSurfaces;
            if (i < blackSurfaceArr.length) {
                BlackSurface bs = blackSurfaceArr[i];
                pw.print(prefix);
                pw.print("#");
                pw.print(i);
                pw.print(": ");
                pw.print(bs.surface);
                pw.print(" left=");
                pw.print(bs.left);
                pw.print(" top=");
                pw.println(bs.top);
                i++;
            } else {
                return;
            }
        }
    }

    public BlackFrame(Supplier<SurfaceControl.Transaction> factory, SurfaceControl.Transaction t, Rect outer, Rect inner, int layer, DisplayContent dc, boolean forceDefaultOrientation, SurfaceControl surfaceControl) throws Surface.OutOfResourcesException {
        BlackSurface[] blackSurfaceArr = new BlackSurface[4];
        this.mBlackSurfaces = blackSurfaceArr;
        boolean success = false;
        this.mTransactionFactory = factory;
        this.mOuterRect = new Rect(outer);
        this.mInnerRect = new Rect(inner);
        try {
            if (outer.top < inner.top) {
                blackSurfaceArr[0] = new BlackSurface(t, layer, outer.left, outer.top, inner.right, inner.top, dc, surfaceControl);
            }
            if (outer.left < inner.left) {
                blackSurfaceArr[1] = new BlackSurface(t, layer, outer.left, inner.top, inner.left, outer.bottom, dc, surfaceControl);
            }
            if (outer.bottom > inner.bottom) {
                blackSurfaceArr[2] = new BlackSurface(t, layer, inner.left, inner.bottom, outer.right, outer.bottom, dc, surfaceControl);
            }
            if (outer.right > inner.right) {
                blackSurfaceArr[3] = new BlackSurface(t, layer, inner.right, outer.top, outer.right, inner.bottom, dc, surfaceControl);
            }
            success = true;
        } finally {
            if (!success) {
                kill();
            }
        }
    }

    public void kill() {
        SurfaceControl.Transaction t = this.mTransactionFactory.get();
        int i = 0;
        while (true) {
            BlackSurface[] blackSurfaceArr = this.mBlackSurfaces;
            if (i < blackSurfaceArr.length) {
                if (blackSurfaceArr[i] != null) {
                    if (ProtoLogCache.WM_SHOW_SURFACE_ALLOC_enabled) {
                        String protoLogParam0 = String.valueOf(this.mBlackSurfaces[i].surface);
                        ProtoLogImpl.i(ProtoLogGroup.WM_SHOW_SURFACE_ALLOC, 51200510, 0, (String) null, new Object[]{protoLogParam0});
                    }
                    t.remove(this.mBlackSurfaces[i].surface);
                    this.mBlackSurfaces[i] = null;
                }
                i++;
            } else {
                t.apply();
                return;
            }
        }
    }
}
