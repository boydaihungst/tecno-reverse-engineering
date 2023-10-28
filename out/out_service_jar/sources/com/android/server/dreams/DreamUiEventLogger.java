package com.android.server.dreams;

import com.android.internal.logging.UiEventLogger;
/* loaded from: classes.dex */
public interface DreamUiEventLogger {
    void log(UiEventLogger.UiEventEnum uiEventEnum, String str);

    /* loaded from: classes.dex */
    public enum DreamUiEventEnum implements UiEventLogger.UiEventEnum {
        DREAM_START(577),
        DREAM_STOP(578);
        
        private final int mId;

        DreamUiEventEnum(int id) {
            this.mId = id;
        }

        public int getId() {
            return this.mId;
        }
    }
}
