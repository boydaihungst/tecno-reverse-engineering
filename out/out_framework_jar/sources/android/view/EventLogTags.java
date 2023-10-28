package android.view;

import android.util.EventLog;
/* loaded from: classes3.dex */
public class EventLogTags {
    public static final int VIEW_ENQUEUE_INPUT_EVENT = 62002;

    private EventLogTags() {
    }

    public static void writeViewEnqueueInputEvent(String eventtype, String action) {
        EventLog.writeEvent((int) VIEW_ENQUEUE_INPUT_EVENT, eventtype, action);
    }
}
