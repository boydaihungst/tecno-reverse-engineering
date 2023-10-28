package com.android.server.dreams;

import com.android.internal.logging.UiEventLogger;
import com.android.internal.util.FrameworkStatsLog;
/* loaded from: classes.dex */
public class DreamUiEventLoggerImpl implements DreamUiEventLogger {
    final String mLoggableDreamPrefix;

    /* JADX INFO: Access modifiers changed from: package-private */
    public DreamUiEventLoggerImpl(String loggableDreamPrefix) {
        this.mLoggableDreamPrefix = loggableDreamPrefix;
    }

    @Override // com.android.server.dreams.DreamUiEventLogger
    public void log(UiEventLogger.UiEventEnum event, String dreamComponentName) {
        int eventID = event.getId();
        if (eventID <= 0) {
            return;
        }
        boolean isFirstPartyDream = this.mLoggableDreamPrefix.isEmpty() ? false : dreamComponentName.startsWith(this.mLoggableDreamPrefix);
        FrameworkStatsLog.write((int) FrameworkStatsLog.DREAM_UI_EVENT_REPORTED, 0, eventID, 0, isFirstPartyDream ? dreamComponentName : "other");
    }
}
