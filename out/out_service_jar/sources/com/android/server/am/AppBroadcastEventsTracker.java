package com.android.server.am;

import android.app.ActivityManagerInternal;
import android.content.Context;
import android.os.SystemClock;
import android.util.proto.ProtoOutputStream;
import com.android.server.am.BaseAppStateEvents;
import com.android.server.am.BaseAppStateTimeSlotEventsTracker;
import com.android.server.am.BaseAppStateTracker;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class AppBroadcastEventsTracker extends BaseAppStateTimeSlotEventsTracker<AppBroadcastEventsPolicy, BaseAppStateTimeSlotEventsTracker.SimpleAppStateTimeslotEvents> implements ActivityManagerInternal.BroadcastEventListener {
    static final boolean DEBUG_APP_STATE_BROADCAST_EVENT_TRACKER = false;
    static final String TAG = "ActivityManager";

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppBroadcastEventsTracker(Context context, AppRestrictionController controller) {
        this(context, controller, null, null);
    }

    AppBroadcastEventsTracker(Context context, AppRestrictionController controller, Constructor<? extends BaseAppStateTracker.Injector<AppBroadcastEventsPolicy>> injector, Object outerContext) {
        super(context, controller, injector, outerContext);
        this.mInjector.setPolicy(new AppBroadcastEventsPolicy(this.mInjector, this));
    }

    public void onSendingBroadcast(String packageName, int uid) {
        if (((AppBroadcastEventsPolicy) this.mInjector.getPolicy()).isEnabled()) {
            onNewEvent(packageName, uid);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public int getType() {
        return 6;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public void onSystemReady() {
        super.onSystemReady();
        this.mInjector.getActivityManagerInternal().addBroadcastEventListener(this);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.am.BaseAppStateEvents.Factory
    public BaseAppStateTimeSlotEventsTracker.SimpleAppStateTimeslotEvents createAppStateEvents(int uid, String packageName) {
        return new BaseAppStateTimeSlotEventsTracker.SimpleAppStateTimeslotEvents(uid, packageName, ((AppBroadcastEventsPolicy) this.mInjector.getPolicy()).getTimeSlotSize(), TAG, (BaseAppStateEvents.MaxTrackingDurationConfig) this.mInjector.getPolicy());
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.am.BaseAppStateEvents.Factory
    public BaseAppStateTimeSlotEventsTracker.SimpleAppStateTimeslotEvents createAppStateEvents(BaseAppStateTimeSlotEventsTracker.SimpleAppStateTimeslotEvents other) {
        return new BaseAppStateTimeSlotEventsTracker.SimpleAppStateTimeslotEvents(other);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public byte[] getTrackerInfoForStatsd(int uid) {
        long now = SystemClock.elapsedRealtime();
        int numOfBroadcasts = getTotalEventsLocked(uid, now);
        if (numOfBroadcasts == 0) {
            return null;
        }
        ProtoOutputStream proto = new ProtoOutputStream();
        proto.write(CompanionMessage.MESSAGE_ID, numOfBroadcasts);
        proto.flush();
        return proto.getBytes();
    }

    @Override // com.android.server.am.BaseAppStateEventsTracker, com.android.server.am.BaseAppStateTracker
    void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.println("APP BROADCAST EVENT TRACKER:");
        super.dump(pw, "  " + prefix);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class AppBroadcastEventsPolicy extends BaseAppStateTimeSlotEventsTracker.BaseAppStateTimeSlotEventsPolicy<AppBroadcastEventsTracker> {
        static final boolean DEFAULT_BG_BROADCAST_MONITOR_ENABLED = true;
        static final long DEFAULT_BG_BROADCAST_WINDOW = 86400000;
        static final int DEFAULT_BG_EX_BROADCAST_THRESHOLD = 10000;
        static final String KEY_BG_BROADCAST_MONITOR_ENABLED = "bg_broadcast_monitor_enabled";
        static final String KEY_BG_BROADCAST_WINDOW = "bg_broadcast_window";
        static final String KEY_BG_EX_BROADCAST_THRESHOLD = "bg_ex_broadcast_threshold";

        AppBroadcastEventsPolicy(BaseAppStateTracker.Injector injector, AppBroadcastEventsTracker tracker) {
            super(injector, tracker, KEY_BG_BROADCAST_MONITOR_ENABLED, true, KEY_BG_BROADCAST_WINDOW, 86400000L, KEY_BG_EX_BROADCAST_THRESHOLD, 10000);
        }

        @Override // com.android.server.am.BaseAppStateTimeSlotEventsTracker.BaseAppStateTimeSlotEventsPolicy
        String getEventName() {
            return "broadcast";
        }

        @Override // com.android.server.am.BaseAppStateTimeSlotEventsTracker.BaseAppStateTimeSlotEventsPolicy, com.android.server.am.BaseAppStateEventsTracker.BaseAppStateEventsPolicy, com.android.server.am.BaseAppStatePolicy
        void dump(PrintWriter pw, String prefix) {
            pw.print(prefix);
            pw.println("APP BROADCAST EVENT TRACKER POLICY SETTINGS:");
            super.dump(pw, "  " + prefix);
        }
    }
}
