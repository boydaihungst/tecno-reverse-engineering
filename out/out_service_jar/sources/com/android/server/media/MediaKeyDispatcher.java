package com.android.server.media;

import android.app.PendingIntent;
import android.content.Context;
import android.media.session.MediaSession;
import android.view.KeyEvent;
import com.android.internal.util.FrameworkStatsLog;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;
/* loaded from: classes2.dex */
public abstract class MediaKeyDispatcher {
    static final int KEY_EVENT_DOUBLE_TAP = 2;
    static final int KEY_EVENT_LONG_PRESS = 8;
    static final int KEY_EVENT_SINGLE_TAP = 1;
    static final int KEY_EVENT_TRIPLE_TAP = 4;
    private Map<Integer, Integer> mOverriddenKeyEvents;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface KeyEventType {
    }

    public MediaKeyDispatcher(Context context) {
        HashMap hashMap = new HashMap();
        this.mOverriddenKeyEvents = hashMap;
        hashMap.put(126, 0);
        this.mOverriddenKeyEvents.put(Integer.valueOf((int) FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_AUTO_TIME), 0);
        this.mOverriddenKeyEvents.put(85, 0);
        this.mOverriddenKeyEvents.put(91, 0);
        this.mOverriddenKeyEvents.put(79, 0);
        this.mOverriddenKeyEvents.put(86, 0);
        this.mOverriddenKeyEvents.put(87, 0);
        this.mOverriddenKeyEvents.put(88, 0);
        this.mOverriddenKeyEvents.put(25, 0);
        this.mOverriddenKeyEvents.put(24, 0);
        this.mOverriddenKeyEvents.put(164, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public MediaSession.Token getMediaSession(KeyEvent keyEvent, int uid, boolean asSystemService) {
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PendingIntent getMediaButtonReceiver(KeyEvent keyEvent, int uid, boolean asSystemService) {
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Map<Integer, Integer> getOverriddenKeyEvents() {
        return this.mOverriddenKeyEvents;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isSingleTapOverridden(int overriddenKeyEvents) {
        return (overriddenKeyEvents & 1) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isDoubleTapOverridden(int overriddenKeyEvents) {
        return (overriddenKeyEvents & 2) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isTripleTapOverridden(int overriddenKeyEvents) {
        return (overriddenKeyEvents & 4) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isLongPressOverridden(int overriddenKeyEvents) {
        return (overriddenKeyEvents & 8) != 0;
    }

    void setOverriddenKeyEvents(int keyCode, int keyEventType) {
        this.mOverriddenKeyEvents.put(Integer.valueOf(keyCode), Integer.valueOf(keyEventType));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSingleTap(KeyEvent keyEvent) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onDoubleTap(KeyEvent keyEvent) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onTripleTap(KeyEvent keyEvent) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onLongPress(KeyEvent keyEvent) {
    }
}
