package com.android.server.wm;

import android.graphics.Rect;
import android.os.Trace;
import android.util.proto.ProtoOutputStream;
import android.view.InsetsSource;
import android.view.InsetsSourceControl;
import android.view.WindowInsets;
import android.window.TaskSnapshot;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import java.io.PrintWriter;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class ImeInsetsSourceProvider extends WindowContainerInsetsSourceProvider {
    private InsetsControlTarget mImeRequester;
    private boolean mImeShowing;
    private boolean mIsImeLayoutDrawn;
    private final InsetsSource mLastSource;
    private Runnable mShowImeRunner;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ImeInsetsSourceProvider(InsetsSource source, InsetsStateController stateController, DisplayContent displayContent) {
        super(source, stateController, displayContent);
        this.mLastSource = new InsetsSource(19);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.InsetsSourceProvider
    public InsetsSourceControl getControl(InsetsControlTarget target) {
        TaskSnapshot snapshot;
        InsetsSourceControl control = super.getControl(target);
        if (control != null && target != null && target.getWindow() != null) {
            WindowState targetWin = target.getWindow();
            boolean z = false;
            if (targetWin.getRootTask() != null) {
                snapshot = targetWin.mWmService.getTaskSnapshot(targetWin.getRootTask().mTaskId, 0, false, false);
            } else {
                snapshot = null;
            }
            if (targetWin.mActivityRecord != null && targetWin.mActivityRecord.hasStartingWindow() && snapshot != null && snapshot.hasImeSurface()) {
                z = true;
            }
            control.setSkipAnimationOnce(z);
        }
        return control;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.InsetsSourceProvider
    public void updateSourceFrame(Rect frame) {
        super.updateSourceFrame(frame);
        onSourceChanged();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.wm.InsetsSourceProvider
    public void updateVisibility() {
        super.updateVisibility();
        onSourceChanged();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.InsetsSourceProvider
    public void updateControlForTarget(InsetsControlTarget target, boolean force) {
        if (target != null && target.getWindow() != null) {
            target = target.getWindow().getImeControlTarget();
        }
        super.updateControlForTarget(target, force);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.wm.InsetsSourceProvider
    public boolean updateClientVisibility(InsetsControlTarget caller) {
        boolean changed = super.updateClientVisibility(caller);
        if (changed && caller.getRequestedVisibility(this.mSource.getType())) {
            reportImeDrawnForOrganizer(caller);
        }
        return changed;
    }

    private void reportImeDrawnForOrganizer(InsetsControlTarget caller) {
        if (caller.getWindow() != null && caller.getWindow().getTask() != null && caller.getWindow().getTask().isOrganized()) {
            this.mWindowContainer.mWmService.mAtmService.mTaskOrganizerController.reportImeDrawnOnTask(caller.getWindow().getTask());
        }
    }

    private void onSourceChanged() {
        if (this.mLastSource.equals(this.mSource)) {
            return;
        }
        this.mLastSource.set(this.mSource);
        this.mDisplayContent.mWmService.mH.obtainMessage(41, this.mDisplayContent).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleShowImePostLayout(InsetsControlTarget imeTarget) {
        boolean targetChanged = isTargetChangedWithinActivity(imeTarget);
        this.mImeRequester = imeTarget;
        if (targetChanged) {
            if (ProtoLogCache.WM_DEBUG_IME_enabled) {
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_IME, 140319294, 0, (String) null, (Object[]) null);
            }
            checkShowImePostLayout();
            return;
        }
        if (ProtoLogCache.WM_DEBUG_IME_enabled) {
            String protoLogParam0 = String.valueOf(this.mImeRequester.getWindow() == null ? this.mImeRequester : this.mImeRequester.getWindow().getName());
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_IME, -1410260105, 0, (String) null, new Object[]{protoLogParam0});
        }
        this.mShowImeRunner = new Runnable() { // from class: com.android.server.wm.ImeInsetsSourceProvider$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ImeInsetsSourceProvider.this.m8013x9b826aae();
            }
        };
        this.mDisplayContent.mWmService.requestTraversal();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleShowImePostLayout$0$com-android-server-wm-ImeInsetsSourceProvider  reason: not valid java name */
    public /* synthetic */ void m8013x9b826aae() {
        if (ProtoLogCache.WM_DEBUG_IME_enabled) {
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_IME, 1928325128, 0, (String) null, (Object[]) null);
        }
        if (isReadyToShowIme()) {
            InsetsControlTarget target = this.mDisplayContent.getImeTarget(2);
            if (ProtoLogCache.WM_DEBUG_IME_enabled) {
                String protoLogParam0 = String.valueOf(target.getWindow() != null ? target.getWindow().getName() : "");
                ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_IME, 585839596, 0, (String) null, new Object[]{protoLogParam0});
            }
            setImeShowing(true);
            target.showInsets(WindowInsets.Type.ime(), true);
            Trace.asyncTraceEnd(32L, "WMS.showImePostLayout", 0);
            InsetsControlTarget insetsControlTarget = this.mImeRequester;
            if (target != insetsControlTarget && insetsControlTarget != null && ProtoLogCache.WM_DEBUG_IME_enabled) {
                String protoLogParam02 = String.valueOf(this.mImeRequester.getWindow() != null ? this.mImeRequester.getWindow().getName() : "");
                ProtoLogImpl.w(ProtoLogGroup.WM_DEBUG_IME, -1554521902, 0, (String) null, new Object[]{protoLogParam02});
            }
        }
        abortShowImePostLayout();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void checkShowImePostLayout() {
        if (this.mWindowContainer == null) {
            return;
        }
        WindowState windowState = this.mWindowContainer.asWindowState();
        if (windowState == null) {
            throw new IllegalArgumentException("IME insets must be provided by a window.");
        }
        if (this.mIsImeLayoutDrawn || (isReadyToShowIme() && windowState.isDrawn() && !windowState.mGivenInsetsPending)) {
            this.mIsImeLayoutDrawn = true;
            Runnable runnable = this.mShowImeRunner;
            if (runnable != null) {
                runnable.run();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void abortShowImePostLayout() {
        if (ProtoLogCache.WM_DEBUG_IME_enabled) {
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_IME, 1373000889, 0, (String) null, (Object[]) null);
        }
        this.mImeRequester = null;
        this.mIsImeLayoutDrawn = false;
        this.mShowImeRunner = null;
    }

    boolean isReadyToShowIme() {
        InsetsControlTarget dcTarget = this.mDisplayContent.getImeTarget(0);
        if (dcTarget == null || this.mImeRequester == null) {
            return false;
        }
        if (ProtoLogCache.WM_DEBUG_IME_enabled) {
            String protoLogParam0 = String.valueOf(dcTarget.getWindow().getName());
            String protoLogParam1 = String.valueOf(this.mImeRequester.getWindow() == null ? this.mImeRequester : this.mImeRequester.getWindow().getName());
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_IME, -856590985, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
        }
        return isImeLayeringTarget(this.mImeRequester, dcTarget) || isAboveImeLayeringTarget(this.mImeRequester, dcTarget) || isImeFallbackTarget(this.mImeRequester) || isImeInputTarget(this.mImeRequester) || sameAsImeControlTarget();
    }

    private static boolean isImeLayeringTarget(InsetsControlTarget target, InsetsControlTarget dcTarget) {
        return !dcTarget.getWindow().isClosing() && target == dcTarget;
    }

    private static boolean isAboveImeLayeringTarget(InsetsControlTarget target, InsetsControlTarget dcTarget) {
        return target.getWindow() != null && dcTarget.getWindow().getParentWindow() == target && dcTarget.getWindow().mSubLayer > target.getWindow().mSubLayer;
    }

    private boolean isImeFallbackTarget(InsetsControlTarget target) {
        return target == this.mDisplayContent.getImeFallback();
    }

    private boolean isImeInputTarget(InsetsControlTarget target) {
        return target == this.mDisplayContent.getImeInputTarget();
    }

    private boolean sameAsImeControlTarget() {
        InsetsControlTarget target = this.mDisplayContent.getImeTarget(2);
        InsetsControlTarget insetsControlTarget = this.mImeRequester;
        return target == insetsControlTarget && (insetsControlTarget.getWindow() == null || !this.mImeRequester.getWindow().isClosing());
    }

    private boolean isTargetChangedWithinActivity(InsetsControlTarget target) {
        InsetsControlTarget insetsControlTarget;
        return (target == null || target.getWindow() == null || (insetsControlTarget = this.mImeRequester) == target || insetsControlTarget == null || this.mShowImeRunner == null || insetsControlTarget.getWindow() == null || this.mImeRequester.getWindow().mActivityRecord != target.getWindow().mActivityRecord) ? false : true;
    }

    @Override // com.android.server.wm.InsetsSourceProvider
    public void dump(PrintWriter pw, String prefix) {
        super.dump(pw, prefix);
        String prefix2 = prefix + "  ";
        pw.print(prefix2);
        pw.print("mImeShowing=");
        pw.print(this.mImeShowing);
        if (this.mImeRequester != null) {
            pw.print(prefix2);
            pw.print("showImePostLayout pending for mImeRequester=");
            pw.print(this.mImeRequester);
            pw.println();
        }
        pw.println();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.InsetsSourceProvider
    public void dumpDebug(ProtoOutputStream proto, long fieldId, int logLevel) {
        long token = proto.start(fieldId);
        super.dumpDebug(proto, 1146756268033L, logLevel);
        InsetsControlTarget insetsControlTarget = this.mImeRequester;
        WindowState imeRequesterWindow = insetsControlTarget != null ? insetsControlTarget.getWindow() : null;
        if (imeRequesterWindow != null) {
            imeRequesterWindow.dumpDebug(proto, 1146756268034L, logLevel);
        }
        proto.write(1133871366147L, this.mIsImeLayoutDrawn);
        proto.end(token);
    }

    public void setImeShowing(boolean imeShowing) {
        this.mImeShowing = imeShowing;
    }

    public boolean isImeShowing() {
        return this.mImeShowing;
    }
}
