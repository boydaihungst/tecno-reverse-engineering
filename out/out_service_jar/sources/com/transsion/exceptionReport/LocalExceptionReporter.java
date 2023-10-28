package com.transsion.exceptionReport;

import android.app.ApplicationErrorReport;
import android.content.Context;
import android.os.Bundle;
import com.android.server.am.ProcessRecord;
/* loaded from: classes2.dex */
public class LocalExceptionReporter {
    private static LocalExceptionReporter mInstance;
    public static final Object mReportExpInitLock = new Object();

    public LocalExceptionReporter() {
    }

    public LocalExceptionReporter(Context context) {
    }

    public static LocalExceptionReporter getInstance(Context context) {
        LocalExceptionReporter localExceptionReporter;
        synchronized (mReportExpInitLock) {
            if (mInstance == null) {
                mInstance = new LocalExceptionReporter(context);
            }
            localExceptionReporter = mInstance;
        }
        return localExceptionReporter;
    }

    public void reportException(ProcessRecord r, ApplicationErrorReport.CrashInfo crashInfo, String activity, String logPath, String exceptionType) {
    }

    public void reportException(String eventType, ProcessRecord process, String processName, String subject, String logPath, ApplicationErrorReport.CrashInfo crashInfo) {
    }

    private void reportException(Bundle bundle) {
    }

    public void reportReboot(String info) {
    }

    public void reportNe(String path) {
    }

    public void reportJE() {
    }
}
