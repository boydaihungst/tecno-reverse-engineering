package com.android.server.wm;

import android.os.Build;
import android.os.Debug;
import android.os.Trace;
import android.util.EventLog;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.SurfaceControl;
import android.view.WindowContentFrameStats;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import java.io.PrintWriter;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class WindowSurfaceController {
    static final String TAG = "WindowManager";
    final WindowStateAnimator mAnimator;
    boolean mChildrenDetached;
    private boolean mHideLayer;
    private final WindowManagerService mService;
    SurfaceControl mSurfaceControl;
    private final Session mWindowSession;
    private final int mWindowType;
    private final String title;
    private boolean mSurfaceShown = false;
    private float mSurfaceX = 0.0f;
    private float mSurfaceY = 0.0f;
    private float mLastDsdx = 1.0f;
    private float mLastDtdx = 0.0f;
    private float mLastDsdy = 0.0f;
    private float mLastDtdy = 1.0f;
    private float mSurfaceAlpha = 0.0f;
    private int mSurfaceLayer = 0;

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowSurfaceController(String name, int format, int flags, WindowStateAnimator animator, int windowType) {
        boolean useBLAST = false;
        this.mAnimator = animator;
        this.title = name;
        WindowManagerService windowManagerService = animator.mService;
        this.mService = windowManagerService;
        WindowState win = animator.mWin;
        this.mWindowType = windowType;
        Session session = win.mSession;
        this.mWindowSession = session;
        if (win.getOwningPackage() != null && win.getOwningPackage().equals("com.transsion.sk")) {
            flags |= 8;
        }
        Trace.traceBegin(32L, "new SurfaceControl");
        SurfaceControl.Builder b = win.makeSurface().setParent(win.getSurfaceControl()).setName(name).setFormat(format).setFlags(flags).setMetadata(2, windowType).setMetadata(1, session.mUid).setMetadata(6, session.mPid).setCallsite("WindowSurfaceController");
        if (windowManagerService.mUseBLAST && (win.getAttrs().privateFlags & 33554432) != 0) {
            useBLAST = true;
        }
        if (useBLAST) {
            b.setBLASTLayer();
        }
        this.mSurfaceControl = b.build();
        Trace.traceEnd(32L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void hide(SurfaceControl.Transaction transaction, String reason) {
        if (ProtoLogCache.WM_SHOW_TRANSACTIONS_enabled) {
            String protoLogParam0 = String.valueOf(reason);
            String protoLogParam1 = String.valueOf(this.title);
            ProtoLogImpl.i(ProtoLogGroup.WM_SHOW_TRANSACTIONS, -1259022216, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
        }
        if (this.mSurfaceShown) {
            hideSurface(transaction);
        }
    }

    private void hideSurface(SurfaceControl.Transaction transaction) {
        if (this.mSurfaceControl == null) {
            return;
        }
        setShown(false);
        try {
            transaction.hide(this.mSurfaceControl);
            if (this.mAnimator.mIsWallpaper) {
                EventLog.writeEvent((int) EventLogTags.WM_WALLPAPER_SURFACE, Integer.valueOf(this.mAnimator.mWin.getDisplayId()), 0);
            }
        } catch (RuntimeException e) {
            Slog.w("WindowManager", "Exception hiding surface in " + this);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void destroy(SurfaceControl.Transaction t) {
        if (ProtoLogCache.WM_SHOW_SURFACE_ALLOC_enabled) {
            String protoLogParam0 = String.valueOf(this);
            String protoLogParam1 = String.valueOf(Debug.getCallers(8));
            ProtoLogImpl.i(ProtoLogGroup.WM_SHOW_SURFACE_ALLOC, -861707633, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
        }
        try {
            try {
                SurfaceControl surfaceControl = this.mSurfaceControl;
                if (surfaceControl != null) {
                    t.remove(surfaceControl);
                }
            } catch (RuntimeException e) {
                Slog.w("WindowManager", "Error destroying surface in: " + this, e);
            }
        } finally {
            setShown(false);
            this.mSurfaceControl = null;
        }
    }

    void setPosition(SurfaceControl.Transaction t, float left, float top) {
        boolean surfaceMoved = (this.mSurfaceX == left && this.mSurfaceY == top) ? false : true;
        if (!surfaceMoved) {
            return;
        }
        this.mSurfaceX = left;
        this.mSurfaceY = top;
        if (ProtoLogCache.WM_SHOW_TRANSACTIONS_enabled) {
            double protoLogParam0 = left;
            double protoLogParam1 = top;
            String protoLogParam2 = String.valueOf(this.title);
            ProtoLogImpl.i(ProtoLogGroup.WM_SHOW_TRANSACTIONS, 633654009, 10, (String) null, new Object[]{Double.valueOf(protoLogParam0), Double.valueOf(protoLogParam1), protoLogParam2});
        }
        t.setPosition(this.mSurfaceControl, left, top);
    }

    void setMatrix(SurfaceControl.Transaction t, float dsdx, float dtdx, float dtdy, float dsdy) {
        boolean matrixChanged = (this.mLastDsdx == dsdx && this.mLastDtdx == dtdx && this.mLastDtdy == dtdy && this.mLastDsdy == dsdy) ? false : true;
        if (!matrixChanged) {
            return;
        }
        this.mLastDsdx = dsdx;
        this.mLastDtdx = dtdx;
        this.mLastDtdy = dtdy;
        this.mLastDsdy = dsdy;
        if (ProtoLogCache.WM_SHOW_TRANSACTIONS_enabled) {
            double protoLogParam0 = dsdx;
            double protoLogParam1 = dtdx;
            double protoLogParam2 = dtdy;
            double protoLogParam3 = dsdy;
            String protoLogParam4 = String.valueOf(this.title);
            ProtoLogImpl.i(ProtoLogGroup.WM_SHOW_TRANSACTIONS, 309039362, 170, (String) null, new Object[]{Double.valueOf(protoLogParam0), Double.valueOf(protoLogParam1), Double.valueOf(protoLogParam2), Double.valueOf(protoLogParam3), protoLogParam4});
        }
        t.setMatrix(this.mSurfaceControl, dsdx, dtdx, dtdy, dsdy);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean prepareToShowInTransaction(SurfaceControl.Transaction t, float alpha) {
        SurfaceControl surfaceControl = this.mSurfaceControl;
        if (surfaceControl == null) {
            return false;
        }
        this.mSurfaceAlpha = alpha;
        t.setAlpha(surfaceControl, alpha);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setOpaque(boolean isOpaque) {
        if (ProtoLogCache.WM_SHOW_TRANSACTIONS_enabled) {
            String protoLogParam1 = String.valueOf(this.title);
            ProtoLogImpl.i(ProtoLogGroup.WM_SHOW_TRANSACTIONS, 558823034, 3, (String) null, new Object[]{Boolean.valueOf(isOpaque), protoLogParam1});
        }
        if (this.mSurfaceControl == null) {
            return;
        }
        if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
            Slog.i("WindowManager", ">>> OPEN TRANSACTION setOpaqueLocked");
        }
        this.mService.openSurfaceTransaction();
        try {
            SurfaceControl.getGlobalTransaction().setOpaque(this.mSurfaceControl, isOpaque);
        } finally {
            this.mService.closeSurfaceTransaction("setOpaqueLocked");
            if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                Slog.i("WindowManager", "<<< CLOSE TRANSACTION setOpaqueLocked");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSecure(boolean isSecure) {
        if (ProtoLogCache.WM_SHOW_TRANSACTIONS_enabled) {
            String protoLogParam1 = String.valueOf(this.title);
            ProtoLogImpl.i(ProtoLogGroup.WM_SHOW_TRANSACTIONS, -1176488860, 3, (String) null, new Object[]{Boolean.valueOf(isSecure), protoLogParam1});
        }
        if (this.mSurfaceControl == null) {
            return;
        }
        if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
            Slog.i("WindowManager", ">>> OPEN TRANSACTION setSecureLocked");
        }
        this.mService.openSurfaceTransaction();
        try {
            SurfaceControl.getGlobalTransaction().setSecure(this.mSurfaceControl, isSecure);
            DisplayContent dc = this.mAnimator.mWin.mDisplayContent;
            if (dc != null) {
                dc.refreshImeSecureFlag(SurfaceControl.getGlobalTransaction());
            }
        } finally {
            this.mService.closeSurfaceTransaction("setSecure");
            if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                Slog.i("WindowManager", "<<< CLOSE TRANSACTION setSecureLocked");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setColorSpaceAgnostic(boolean agnostic) {
        if (ProtoLogCache.WM_SHOW_TRANSACTIONS_enabled) {
            String protoLogParam1 = String.valueOf(this.title);
            ProtoLogImpl.i(ProtoLogGroup.WM_SHOW_TRANSACTIONS, 585096182, 3, (String) null, new Object[]{Boolean.valueOf(agnostic), protoLogParam1});
        }
        if (this.mSurfaceControl == null) {
            return;
        }
        if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
            Slog.i("WindowManager", ">>> OPEN TRANSACTION setColorSpaceAgnosticLocked");
        }
        this.mService.openSurfaceTransaction();
        try {
            SurfaceControl.getGlobalTransaction().setColorSpaceAgnostic(this.mSurfaceControl, agnostic);
        } finally {
            this.mService.closeSurfaceTransaction("setColorSpaceAgnostic");
            if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                Slog.i("WindowManager", "<<< CLOSE TRANSACTION setColorSpaceAgnosticLocked");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean showRobustly(SurfaceControl.Transaction t) {
        if (ProtoLogCache.WM_SHOW_TRANSACTIONS_enabled) {
            String protoLogParam0 = String.valueOf(this.title);
            ProtoLogImpl.i(ProtoLogGroup.WM_SHOW_TRANSACTIONS, -1089874824, 0, (String) null, new Object[]{protoLogParam0});
        }
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v("WindowManager", "Showing " + this + " during relayout");
        }
        if (this.mSurfaceShown) {
            return true;
        }
        if (Build.IS_DEBUG_ENABLE) {
            Slog.d("WindowManager", "System monitor showSurface win : " + this.mAnimator.mWin);
        }
        setShown(true);
        if (!this.mHideLayer) {
            t.show(this.mSurfaceControl);
            if (this.mAnimator.mIsWallpaper) {
                EventLog.writeEvent((int) EventLogTags.WM_WALLPAPER_SURFACE, Integer.valueOf(this.mAnimator.mWin.getDisplayId()), 1);
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean clearWindowContentFrameStats() {
        SurfaceControl surfaceControl = this.mSurfaceControl;
        if (surfaceControl == null) {
            return false;
        }
        return surfaceControl.clearContentFrameStats();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getWindowContentFrameStats(WindowContentFrameStats outStats) {
        SurfaceControl surfaceControl = this.mSurfaceControl;
        if (surfaceControl == null) {
            return false;
        }
        return surfaceControl.getContentFrameStats(outStats);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasSurface() {
        return this.mSurfaceControl != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getSurfaceControl(SurfaceControl outSurfaceControl) {
        outSurfaceControl.copyFrom(this.mSurfaceControl, "WindowSurfaceController.getSurfaceControl");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getShown() {
        return this.mSurfaceShown;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setShown(boolean surfaceShown) {
        this.mSurfaceShown = surfaceShown;
        this.mService.updateNonSystemOverlayWindowsVisibilityIfNeeded(this.mAnimator.mWin, surfaceShown);
        this.mAnimator.mWin.onSurfaceShownChanged(surfaceShown);
        Session session = this.mWindowSession;
        if (session != null) {
            session.onWindowSurfaceVisibilityChanged(this, this.mSurfaceShown, this.mWindowType);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void hideLayer() {
        if (this.mSurfaceControl == null || this.mHideLayer) {
            return;
        }
        Slog.i("WindowManager", "     hideLayer with mSurfaceControl=" + this.mSurfaceControl);
        if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
            Slog.i("WindowManager", ">>> OPEN TRANSACTION hideLayerLocked");
        }
        this.mService.openSurfaceTransaction();
        try {
            if (this.mSurfaceShown) {
                SurfaceControl.getGlobalTransaction().hide(this.mSurfaceControl);
            }
            this.mHideLayer = true;
        } finally {
            this.mService.closeSurfaceTransaction("hideLayerLocked");
            if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                Slog.i("WindowManager", "<<< CLOSE TRANSACTION hideLayerLocked");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetLayer() {
        if (this.mSurfaceControl == null || !this.mHideLayer) {
            this.mHideLayer = false;
            return;
        }
        Slog.i("WindowManager", "     resetLayer with mSurfaceControl=" + this.mSurfaceControl);
        if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
            Slog.i("WindowManager", ">>> OPEN TRANSACTION resetLayerLocked");
        }
        this.mService.openSurfaceTransaction();
        try {
            if (this.mSurfaceShown) {
                SurfaceControl.getGlobalTransaction().show(this.mSurfaceControl);
            }
            this.mHideLayer = false;
        } finally {
            this.mService.closeSurfaceTransaction("resetLayerLocked");
            if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                Slog.i("WindowManager", "<<< CLOSE TRANSACTION resetLayerLocked");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1133871366145L, this.mSurfaceShown);
        proto.write(1120986464258L, this.mSurfaceLayer);
        proto.end(token);
    }

    public void dump(PrintWriter pw, String prefix, boolean dumpAll) {
        if (dumpAll) {
            pw.print(prefix);
            pw.print("mSurface=");
            pw.println(this.mSurfaceControl);
        }
        pw.print(prefix);
        pw.print("Surface: shown=");
        pw.print(this.mSurfaceShown);
        pw.print(" layer=");
        pw.print(this.mSurfaceLayer);
        pw.print(" alpha=");
        pw.print(this.mSurfaceAlpha);
        pw.print(" rect=(");
        pw.print(this.mSurfaceX);
        pw.print(",");
        pw.print(this.mSurfaceY);
        pw.print(") ");
        pw.print(" transform=(");
        pw.print(this.mLastDsdx);
        pw.print(", ");
        pw.print(this.mLastDtdx);
        pw.print(", ");
        pw.print(this.mLastDsdy);
        pw.print(", ");
        pw.print(this.mLastDtdy);
        pw.println(")");
    }

    public String toString() {
        return this.mSurfaceControl.toString();
    }
}
