package com.android.server.am;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.WindowManager;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class AppWaitingForDebuggerDialog extends BaseErrorDialog {
    private CharSequence mAppName;
    private final Handler mHandler;
    final ProcessRecord mProc;
    final ActivityManagerService mService;

    public AppWaitingForDebuggerDialog(ActivityManagerService service, Context context, ProcessRecord app) {
        super(context);
        Handler handler = new Handler() { // from class: com.android.server.am.AppWaitingForDebuggerDialog.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        AppWaitingForDebuggerDialog.this.mService.killAppAtUsersRequest(AppWaitingForDebuggerDialog.this.mProc);
                        return;
                    default:
                        return;
                }
            }
        };
        this.mHandler = handler;
        this.mService = service;
        this.mProc = app;
        this.mAppName = context.getPackageManager().getApplicationLabel(app.info);
        setCancelable(false);
        StringBuilder text = new StringBuilder();
        CharSequence charSequence = this.mAppName;
        if (charSequence != null && charSequence.length() > 0) {
            text.append("Application ");
            text.append(this.mAppName);
            text.append(" (process ");
            text.append(app.processName);
            text.append(")");
        } else {
            text.append("Process ");
            text.append(app.processName);
        }
        text.append(" is waiting for the debugger to attach.");
        setMessage(text.toString());
        setButton(-1, "Force Close", handler.obtainMessage(1, app));
        setTitle("Waiting For Debugger");
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.setTitle("Waiting For Debugger: " + app.info.processName);
        getWindow().setAttributes(attrs);
    }

    @Override // com.android.server.am.BaseErrorDialog
    protected void closeDialog() {
    }
}
