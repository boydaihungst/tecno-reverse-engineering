package com.android.server.am;

import android.app.AnrController;
import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import com.android.server.am.AppErrorDialog;
import com.android.server.am.AppNotRespondingDialog;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class ErrorDialogController {
    private AnrController mAnrController;
    private List<AppNotRespondingDialog> mAnrDialogs;
    private final ProcessRecord mApp;
    private List<AppErrorDialog> mCrashDialogs;
    private final ActivityManagerGlobalLock mProcLock;
    private final ActivityManagerService mService;
    private List<StrictModeViolationDialog> mViolationDialogs;
    private AppWaitingForDebuggerDialog mWaitDialog;

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasCrashDialogs() {
        return this.mCrashDialogs != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<AppErrorDialog> getCrashDialogs() {
        return this.mCrashDialogs;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasAnrDialogs() {
        return this.mAnrDialogs != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<AppNotRespondingDialog> getAnrDialogs() {
        return this.mAnrDialogs;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasViolationDialogs() {
        return this.mViolationDialogs != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasDebugWaitingDialog() {
        return this.mWaitDialog != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearAllErrorDialogs() {
        clearCrashDialogs();
        clearAnrDialogs();
        clearViolationDialogs();
        clearWaitingDialog();
    }

    void clearCrashDialogs() {
        clearCrashDialogs(true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearCrashDialogs(boolean needDismiss) {
        List<AppErrorDialog> list = this.mCrashDialogs;
        if (list == null) {
            return;
        }
        if (needDismiss) {
            scheduleForAllDialogs(list, new ErrorDialogController$$ExternalSyntheticLambda4());
        }
        this.mCrashDialogs = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearAnrDialogs() {
        List<AppNotRespondingDialog> list = this.mAnrDialogs;
        if (list == null) {
            return;
        }
        scheduleForAllDialogs(list, new ErrorDialogController$$ExternalSyntheticLambda4());
        this.mAnrDialogs = null;
        this.mAnrController = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearViolationDialogs() {
        List<StrictModeViolationDialog> list = this.mViolationDialogs;
        if (list == null) {
            return;
        }
        scheduleForAllDialogs(list, new ErrorDialogController$$ExternalSyntheticLambda4());
        this.mViolationDialogs = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearWaitingDialog() {
        if (this.mWaitDialog == null) {
            return;
        }
        final BaseErrorDialog dialog = this.mWaitDialog;
        Handler handler = this.mService.mUiHandler;
        Objects.requireNonNull(dialog);
        handler.post(new Runnable() { // from class: com.android.server.am.ErrorDialogController$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                BaseErrorDialog.this.dismiss();
            }
        });
        this.mWaitDialog = null;
    }

    void scheduleForAllDialogs(final List<? extends BaseErrorDialog> dialogs, final Consumer<BaseErrorDialog> c) {
        this.mService.mUiHandler.post(new Runnable() { // from class: com.android.server.am.ErrorDialogController$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                ErrorDialogController.this.m1429xf739cdf2(dialogs, c);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleForAllDialogs$0$com-android-server-am-ErrorDialogController  reason: not valid java name */
    public /* synthetic */ void m1429xf739cdf2(List dialogs, Consumer c) {
        if (dialogs != null) {
            forAllDialogs(dialogs, c);
        }
    }

    void forAllDialogs(List<? extends BaseErrorDialog> dialogs, Consumer<BaseErrorDialog> c) {
        for (int i = dialogs.size() - 1; i >= 0; i--) {
            c.accept(dialogs.get(i));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void showCrashDialogs(AppErrorDialog.Data data) {
        List<Context> contexts = getDisplayContexts(false);
        this.mCrashDialogs = new ArrayList();
        for (int i = contexts.size() - 1; i >= 0; i--) {
            Context c = contexts.get(i);
            this.mCrashDialogs.add(new AppErrorDialog(c, this.mService, data));
        }
        this.mService.mUiHandler.post(new Runnable() { // from class: com.android.server.am.ErrorDialogController$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                ErrorDialogController.this.m1430x59a3ed12();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$showCrashDialogs$1$com-android-server-am-ErrorDialogController  reason: not valid java name */
    public /* synthetic */ void m1430x59a3ed12() {
        List<AppErrorDialog> dialogs;
        synchronized (this.mProcLock) {
            try {
                ActivityManagerService.boostPriorityForProcLockedSection();
                dialogs = this.mCrashDialogs;
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterProcLockedSection();
        if (dialogs != null) {
            forAllDialogs(dialogs, new ErrorDialogController$$ExternalSyntheticLambda2());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void showAnrDialogs(AppNotRespondingDialog.Data data) {
        List<Context> contexts = getDisplayContexts(this.mApp.mErrorState.isSilentAnr());
        this.mAnrDialogs = new ArrayList();
        for (int i = contexts.size() - 1; i >= 0; i--) {
            Context c = contexts.get(i);
            this.mAnrDialogs.add(new AppNotRespondingDialog(this.mService, c, data));
        }
        scheduleForAllDialogs(this.mAnrDialogs, new ErrorDialogController$$ExternalSyntheticLambda2());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void showViolationDialogs(AppErrorResult res) {
        List<Context> contexts = getDisplayContexts(false);
        this.mViolationDialogs = new ArrayList();
        for (int i = contexts.size() - 1; i >= 0; i--) {
            Context c = contexts.get(i);
            this.mViolationDialogs.add(new StrictModeViolationDialog(c, this.mService, res, this.mApp));
        }
        scheduleForAllDialogs(this.mViolationDialogs, new ErrorDialogController$$ExternalSyntheticLambda2());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void showDebugWaitingDialogs() {
        List<Context> contexts = getDisplayContexts(true);
        Context c = contexts.get(0);
        this.mWaitDialog = new AppWaitingForDebuggerDialog(this.mService, c, this.mApp);
        this.mService.mUiHandler.post(new Runnable() { // from class: com.android.server.am.ErrorDialogController$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                ErrorDialogController.this.m1431x295240ac();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$showDebugWaitingDialogs$2$com-android-server-am-ErrorDialogController  reason: not valid java name */
    public /* synthetic */ void m1431x295240ac() {
        Dialog dialog;
        synchronized (this.mProcLock) {
            try {
                ActivityManagerService.boostPriorityForProcLockedSection();
                dialog = this.mWaitDialog;
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterProcLockedSection();
        if (dialog != null) {
            dialog.show();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AnrController getAnrController() {
        return this.mAnrController;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAnrController(AnrController controller) {
        this.mAnrController = controller;
    }

    private List<Context> getDisplayContexts(boolean lastUsedOnly) {
        Context context;
        List<Context> displayContexts = new ArrayList<>();
        if (!lastUsedOnly) {
            this.mApp.getWindowProcessController().getDisplayContextsWithErrorDialogs(displayContexts);
        }
        if (displayContexts.isEmpty() || lastUsedOnly) {
            if (this.mService.mWmInternal != null) {
                context = this.mService.mWmInternal.getTopFocusedDisplayUiContext();
            } else {
                context = this.mService.mUiContext;
            }
            displayContexts.add(context);
        }
        return displayContexts;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ErrorDialogController(ProcessRecord app) {
        this.mApp = app;
        ActivityManagerService activityManagerService = app.mService;
        this.mService = activityManagerService;
        this.mProcLock = activityManagerService.mProcLock;
    }
}
