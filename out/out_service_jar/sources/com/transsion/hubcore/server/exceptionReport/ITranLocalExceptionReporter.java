package com.transsion.hubcore.server.exceptionReport;

import android.app.ApplicationErrorReport;
import android.content.Context;
import android.os.Bundle;
import com.android.server.am.ProcessRecord;
import com.transsion.hubcore.server.exceptionReport.ITranLocalExceptionReporter;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranLocalExceptionReporter {
    public static final TranClassInfo<ITranLocalExceptionReporter> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.exceptionReport.TranLocalExceptionReporterImpl", ITranLocalExceptionReporter.class, new Supplier() { // from class: com.transsion.hubcore.server.exceptionReport.ITranLocalExceptionReporter$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranLocalExceptionReporter.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranLocalExceptionReporter {
    }

    static ITranLocalExceptionReporter Instance() {
        return (ITranLocalExceptionReporter) classInfo.getImpl();
    }

    default void reportException(Bundle bundle) {
    }

    default void reportReboot(String info) {
    }

    default void reportNe(String path) {
    }

    default void reportJE() {
    }

    default void init(Context context) {
    }

    default void reportException(String eventType, ProcessRecord process, String processName, String subject, String logPath, ApplicationErrorReport.CrashInfo crashInfo) {
    }
}
