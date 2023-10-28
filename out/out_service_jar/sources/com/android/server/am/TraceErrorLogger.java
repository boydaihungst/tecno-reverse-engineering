package com.android.server.am;

import android.os.Trace;
import java.util.UUID;
/* loaded from: classes.dex */
public class TraceErrorLogger {
    private static final String COUNTER_PREFIX = "ErrorId:";
    private static final int PLACEHOLDER_VALUE = 1;

    public boolean isAddErrorIdEnabled() {
        return true;
    }

    public UUID generateErrorId() {
        return UUID.randomUUID();
    }

    public void addErrorIdToTrace(String processName, UUID errorId) {
        Trace.traceCounter(64L, COUNTER_PREFIX + processName + "#" + errorId.toString(), 1);
    }

    public void addSubjectToTrace(String subject, UUID errorId) {
        Trace.traceCounter(64L, String.format("Subject(for ErrorId %s):%s", errorId.toString(), subject), 1);
    }
}
