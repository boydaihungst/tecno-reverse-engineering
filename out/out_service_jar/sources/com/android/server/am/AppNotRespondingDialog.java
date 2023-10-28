package com.android.server.am;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.BidiFormatter;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.FrameworkStatsLog;
/* loaded from: classes.dex */
public final class AppNotRespondingDialog extends BaseErrorDialog implements View.OnClickListener {
    public static final int ALREADY_SHOWING = -2;
    public static final int CANT_SHOW = -1;
    static final int FORCE_CLOSE = 1;
    private static final String TAG = "AppNotRespondingDialog";
    static final int WAIT = 2;
    static final int WAIT_AND_REPORT = 3;
    private final Data mData;
    private final Handler mHandler;
    private final ProcessRecord mProc;
    private final ActivityManagerService mService;

    /* JADX WARN: Removed duplicated region for block: B:18:0x0064  */
    /* JADX WARN: Removed duplicated region for block: B:19:0x0080  */
    /* JADX WARN: Removed duplicated region for block: B:22:0x0097  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public AppNotRespondingDialog(ActivityManagerService service, Context context, Data data) {
        super(context);
        CharSequence name1;
        int resid;
        this.mHandler = new Handler() { // from class: com.android.server.am.AppNotRespondingDialog.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                Intent appErrorIntent = null;
                MetricsLogger.action(AppNotRespondingDialog.this.getContext(), (int) FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_MEDIA_SESSION_CALLBACK, msg.what);
                switch (msg.what) {
                    case 1:
                        AppNotRespondingDialog.this.mService.killAppAtUsersRequest(AppNotRespondingDialog.this.mProc);
                        break;
                    case 2:
                    case 3:
                        synchronized (AppNotRespondingDialog.this.mService) {
                            try {
                                ActivityManagerService.boostPriorityForLockedSection();
                                ProcessRecord app = AppNotRespondingDialog.this.mProc;
                                ProcessErrorStateRecord errState = app.mErrorState;
                                if (msg.what == 3) {
                                    appErrorIntent = AppNotRespondingDialog.this.mService.mAppErrors.createAppErrorIntentLOSP(app, System.currentTimeMillis(), null);
                                }
                                synchronized (AppNotRespondingDialog.this.mService.mProcLock) {
                                    ActivityManagerService.boostPriorityForProcLockedSection();
                                    errState.setNotResponding(false);
                                    errState.getDialogController().clearAnrDialogs();
                                }
                                ActivityManagerService.resetPriorityAfterProcLockedSection();
                                AppNotRespondingDialog.this.mService.mServices.scheduleServiceTimeoutLocked(app);
                                AppNotRespondingDialog.this.mService.mInternal.rescheduleAnrDialog(AppNotRespondingDialog.this.mData);
                            } catch (Throwable th) {
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                throw th;
                            }
                        }
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        break;
                }
                if (appErrorIntent != null) {
                    try {
                        AppNotRespondingDialog.this.getContext().startActivity(appErrorIntent);
                    } catch (ActivityNotFoundException e) {
                        Slog.w(AppNotRespondingDialog.TAG, "bug report receiver dissappeared", e);
                    }
                }
                AppNotRespondingDialog.this.dismiss();
            }
        };
        this.mService = service;
        ProcessRecord processRecord = data.proc;
        this.mProc = processRecord;
        this.mData = data;
        Resources res = context.getResources();
        setCancelable(false);
        if (data.aInfo != null) {
            name1 = data.aInfo.loadLabel(context.getPackageManager());
        } else {
            name1 = null;
        }
        CharSequence name2 = null;
        if (processRecord.getPkgList().size() == 1) {
            CharSequence applicationLabel = context.getPackageManager().getApplicationLabel(processRecord.info);
            name2 = applicationLabel;
            if (applicationLabel != null) {
                if (name1 != null) {
                    resid = 17039672;
                } else {
                    name1 = name2;
                    name2 = processRecord.processName;
                    resid = 17039674;
                }
                BidiFormatter bidi = BidiFormatter.getInstance();
                setTitle(name2 == null ? res.getString(resid, bidi.unicodeWrap(name1.toString()), bidi.unicodeWrap(name2.toString())) : res.getString(resid, bidi.unicodeWrap(name1.toString())));
                if (data.aboveSystem) {
                    getWindow().setType(2010);
                }
                WindowManager.LayoutParams attrs = getWindow().getAttributes();
                attrs.setTitle("Application Not Responding: " + processRecord.info.processName);
                attrs.privateFlags = 272;
                getWindow().setAttributes(attrs);
            }
        }
        if (name1 != null) {
            name2 = processRecord.processName;
            resid = 17039673;
        } else {
            name1 = processRecord.processName;
            resid = 17039675;
        }
        BidiFormatter bidi2 = BidiFormatter.getInstance();
        setTitle(name2 == null ? res.getString(resid, bidi2.unicodeWrap(name1.toString()), bidi2.unicodeWrap(name2.toString())) : res.getString(resid, bidi2.unicodeWrap(name1.toString())));
        if (data.aboveSystem) {
        }
        WindowManager.LayoutParams attrs2 = getWindow().getAttributes();
        attrs2.setTitle("Application Not Responding: " + processRecord.info.processName);
        attrs2.privateFlags = 272;
        getWindow().setAttributes(attrs2);
    }

    @Override // android.app.AlertDialog, android.app.Dialog
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout frame = (FrameLayout) findViewById(16908331);
        Context context = getContext();
        LayoutInflater.from(context).inflate(17367095, (ViewGroup) frame, true);
        TextView report = (TextView) findViewById(16908756);
        report.setOnClickListener(this);
        boolean hasReceiver = this.mProc.mErrorState.getErrorReportReceiver() != null;
        report.setVisibility(hasReceiver ? 0 : 8);
        TextView close = (TextView) findViewById(16908754);
        close.setOnClickListener(this);
        TextView wait = (TextView) findViewById(16908758);
        wait.setOnClickListener(this);
        findViewById(16908935).setVisibility(0);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View v) {
        switch (v.getId()) {
            case 16908754:
                this.mHandler.obtainMessage(1).sendToTarget();
                return;
            case 16908755:
            case 16908757:
            default:
                return;
            case 16908756:
                this.mHandler.obtainMessage(3).sendToTarget();
                return;
            case 16908758:
                this.mHandler.obtainMessage(2).sendToTarget();
                return;
        }
    }

    @Override // com.android.server.am.BaseErrorDialog
    protected void closeDialog() {
        this.mHandler.obtainMessage(1).sendToTarget();
    }

    /* loaded from: classes.dex */
    public static class Data {
        final ApplicationInfo aInfo;
        final boolean aboveSystem;
        final ProcessRecord proc;

        public Data(ProcessRecord proc, ApplicationInfo aInfo, boolean aboveSystem) {
            this.proc = proc;
            this.aInfo = aInfo;
            this.aboveSystem = aboveSystem;
        }
    }
}
