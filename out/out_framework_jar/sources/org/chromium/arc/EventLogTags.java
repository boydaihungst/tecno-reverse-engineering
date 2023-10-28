package org.chromium.arc;

import android.util.EventLog;
/* loaded from: classes4.dex */
public class EventLogTags {
    public static final int ARC_SYSTEM_EVENT = 300000;

    private EventLogTags() {
    }

    public static void writeArcSystemEvent(String event) {
        EventLog.writeEvent((int) ARC_SYSTEM_EVENT, event);
    }
}
