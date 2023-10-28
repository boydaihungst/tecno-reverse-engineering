package com.android.server.wm;

import android.content.res.Configuration;
import android.graphics.Rect;
import android.view.InsetsState;
import android.view.SurfaceControl;
import android.view.SurfaceControlViewHost;
import java.util.ArrayList;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class TrustedOverlayHost {
    final ArrayList<SurfaceControlViewHost.SurfacePackage> mOverlays = new ArrayList<>();
    SurfaceControl mSurfaceControl;
    final WindowManagerService mWmService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public TrustedOverlayHost(WindowManagerService wms) {
        this.mWmService = wms;
    }

    void requireOverlaySurfaceControl() {
        if (this.mSurfaceControl == null) {
            SurfaceControl.Builder b = this.mWmService.makeSurfaceBuilder(null).setContainerLayer().setHidden(true).setName("Overlay Host Leash");
            this.mSurfaceControl = b.build();
            SurfaceControl.Transaction t = this.mWmService.mTransactionFactory.get();
            t.setTrustedOverlay(this.mSurfaceControl, true).apply();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setParent(SurfaceControl.Transaction t, SurfaceControl newParent) {
        SurfaceControl surfaceControl = this.mSurfaceControl;
        if (surfaceControl == null) {
            return;
        }
        t.reparent(surfaceControl, newParent);
        if (newParent != null) {
            t.show(this.mSurfaceControl);
        } else {
            t.hide(this.mSurfaceControl);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLayer(SurfaceControl.Transaction t, int layer) {
        SurfaceControl surfaceControl = this.mSurfaceControl;
        if (surfaceControl != null) {
            t.setLayer(surfaceControl, layer);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addOverlay(SurfaceControlViewHost.SurfacePackage p, SurfaceControl currentParent) {
        requireOverlaySurfaceControl();
        this.mOverlays.add(p);
        this.mWmService.mEmbeddedWindowController.setIsOverlay(p.getInputToken());
        SurfaceControl.Transaction t = this.mWmService.mTransactionFactory.get();
        t.reparent(p.getSurfaceControl(), this.mSurfaceControl).show(p.getSurfaceControl());
        setParent(t, currentParent);
        t.apply();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean removeOverlay(SurfaceControlViewHost.SurfacePackage p) {
        SurfaceControl.Transaction t = this.mWmService.mTransactionFactory.get();
        for (int i = this.mOverlays.size() - 1; i >= 0; i--) {
            SurfaceControlViewHost.SurfacePackage l = this.mOverlays.get(i);
            if (l.getSurfaceControl().isSameSurface(p.getSurfaceControl())) {
                this.mOverlays.remove(i);
                t.reparent(l.getSurfaceControl(), null);
                l.release();
            }
        }
        t.apply();
        return this.mOverlays.size() > 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchConfigurationChanged(Configuration c) {
        for (int i = this.mOverlays.size() - 1; i >= 0; i--) {
            SurfaceControlViewHost.SurfacePackage l = this.mOverlays.get(i);
            try {
                l.getRemoteInterface().onConfigurationChanged(c);
            } catch (Exception e) {
                removeOverlay(l);
            }
        }
    }

    private void dispatchDetachedFromWindow() {
        for (int i = this.mOverlays.size() - 1; i >= 0; i--) {
            SurfaceControlViewHost.SurfacePackage l = this.mOverlays.get(i);
            try {
                l.getRemoteInterface().onDispatchDetachedFromWindow();
            } catch (Exception e) {
            }
            l.release();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchInsetsChanged(InsetsState s, Rect insetFrame) {
        for (int i = this.mOverlays.size() - 1; i >= 0; i--) {
            SurfaceControlViewHost.SurfacePackage l = this.mOverlays.get(i);
            try {
                l.getRemoteInterface().onInsetsChanged(s, insetFrame);
            } catch (Exception e) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void release() {
        dispatchDetachedFromWindow();
        this.mOverlays.clear();
        SurfaceControl.Transaction t = this.mWmService.mTransactionFactory.get();
        t.remove(this.mSurfaceControl).apply();
        this.mSurfaceControl = null;
    }
}
