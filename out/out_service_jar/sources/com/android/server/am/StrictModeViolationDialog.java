package com.android.server.am;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class StrictModeViolationDialog extends BaseErrorDialog {
    static final int ACTION_OK = 0;
    static final int ACTION_OK_AND_REPORT = 1;
    static final long DISMISS_TIMEOUT = 60000;
    private static final String TAG = "StrictModeViolationDialog";
    private final Handler mHandler;
    private final ProcessRecord mProc;
    private final AppErrorResult mResult;
    private final ActivityManagerService mService;

    public StrictModeViolationDialog(Context context, ActivityManagerService service, AppErrorResult result, ProcessRecord app) {
        super(context);
        CharSequence name;
        Handler handler = new Handler() { // from class: com.android.server.am.StrictModeViolationDialog.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                synchronized (StrictModeViolationDialog.this.mService.mProcLock) {
                    try {
                        ActivityManagerService.boostPriorityForProcLockedSection();
                        if (StrictModeViolationDialog.this.mProc != null) {
                            StrictModeViolationDialog.this.mProc.mErrorState.getDialogController().clearViolationDialogs();
                        }
                    } catch (Throwable th) {
                        ActivityManagerService.resetPriorityAfterProcLockedSection();
                        throw th;
                    }
                }
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                StrictModeViolationDialog.this.mResult.set(msg.what);
                StrictModeViolationDialog.this.dismiss();
            }
        };
        this.mHandler = handler;
        Resources res = context.getResources();
        this.mService = service;
        this.mProc = app;
        this.mResult = result;
        if (app.getPkgList().size() == 1 && (name = context.getPackageManager().getApplicationLabel(app.info)) != null) {
            setMessage(res.getString(17041547, name.toString(), app.info.processName));
        } else {
            setMessage(res.getString(17041548, app.processName.toString()));
        }
        setCancelable(false);
        setButton(-1, res.getText(17040167), handler.obtainMessage(0));
        if (app.mErrorState.getErrorReportReceiver() != null) {
            setButton(-2, res.getText(17041390), handler.obtainMessage(1));
        }
        getWindow().addPrivateFlags(256);
        getWindow().setTitle("Strict Mode Violation: " + app.info.processName);
        handler.sendMessageDelayed(handler.obtainMessage(0), 60000L);
    }

    @Override // com.android.server.am.BaseErrorDialog
    protected void closeDialog() {
        this.mHandler.obtainMessage(0).sendToTarget();
    }
}
