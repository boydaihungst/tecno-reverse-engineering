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
public final class AppBindServiceEventsTracker extends BaseAppStateTimeSlotEventsTracker<AppBindServiceEventsPolicy, BaseAppStateTimeSlotEventsTracker.SimpleAppStateTimeslotEvents> implements ActivityManagerInternal.BindServiceEventListener {
    static final boolean DEBUG_APP_STATE_BIND_SERVICE_EVENT_TRACKER = false;
    static final String TAG = "ActivityManager";

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppBindServiceEventsTracker(Context context, AppRestrictionController controller) {
        this(context, controller, null, null);
    }

    AppBindServiceEventsTracker(Context context, AppRestrictionController controller, Constructor<? extends BaseAppStateTracker.Injector<AppBindServiceEventsPolicy>> injector, Object outerContext) {
        super(context, controller, injector, outerContext);
        this.mInjector.setPolicy(new AppBindServiceEventsPolicy(this.mInjector, this));
    }

    public void onBindingService(String packageName, int uid) {
        if (((AppBindServiceEventsPolicy) this.mInjector.getPolicy()).isEnabled()) {
            onNewEvent(packageName, uid);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public int getType() {
        return 7;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public void onSystemReady() {
        super.onSystemReady();
        this.mInjector.getActivityManagerInternal().addBindServiceEventListener(this);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.am.BaseAppStateEvents.Factory
    public BaseAppStateTimeSlotEventsTracker.SimpleAppStateTimeslotEvents createAppStateEvents(int uid, String packageName) {
        return new BaseAppStateTimeSlotEventsTracker.SimpleAppStateTimeslotEvents(uid, packageName, ((AppBindServiceEventsPolicy) this.mInjector.getPolicy()).getTimeSlotSize(), TAG, (BaseAppStateEvents.MaxTrackingDurationConfig) this.mInjector.getPolicy());
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
        int numOfBindRequests = getTotalEventsLocked(uid, now);
        if (numOfBindRequests == 0) {
            return null;
        }
        ProtoOutputStream proto = new ProtoOutputStream();
        proto.write(CompanionMessage.MESSAGE_ID, numOfBindRequests);
        proto.flush();
        return proto.getBytes();
    }

    @Override // com.android.server.am.BaseAppStateEventsTracker, com.android.server.am.BaseAppStateTracker
    void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.println("APP BIND SERVICE EVENT TRACKER:");
        super.dump(pw, "  " + prefix);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class AppBindServiceEventsPolicy extends BaseAppStateTimeSlotEventsTracker.BaseAppStateTimeSlotEventsPolicy<AppBindServiceEventsTracker> {
        static final boolean DEFAULT_BG_BIND_SVC_MONITOR_ENABLED = true;
        static final long DEFAULT_BG_BIND_SVC_WINDOW = 86400000;
        static final int DEFAULT_BG_EX_BIND_SVC_THRESHOLD = 10000;
        static final String KEY_BG_BIND_SVC_MONITOR_ENABLED = "bg_bind_svc_monitor_enabled";
        static final String KEY_BG_BIND_SVC_WINDOW = "bg_bind_svc_window";
        static final String KEY_BG_EX_BIND_SVC_THRESHOLD = "bg_ex_bind_svc_threshold";

        AppBindServiceEventsPolicy(BaseAppStateTracker.Injector injector, AppBindServiceEventsTracker tracker) {
            super(injector, tracker, KEY_BG_BIND_SVC_MONITOR_ENABLED, true, KEY_BG_BIND_SVC_WINDOW, 86400000L, KEY_BG_EX_BIND_SVC_THRESHOLD, 10000);
        }

        @Override // com.android.server.am.BaseAppStateTimeSlotEventsTracker.BaseAppStateTimeSlotEventsPolicy
        String getEventName() {
            return "bindservice";
        }

        @Override // com.android.server.am.BaseAppStateTimeSlotEventsTracker.BaseAppStateTimeSlotEventsPolicy, com.android.server.am.BaseAppStateEventsTracker.BaseAppStateEventsPolicy, com.android.server.am.BaseAppStatePolicy
        void dump(PrintWriter pw, String prefix) {
            pw.print(prefix);
            pw.println("APP BIND SERVICE EVENT TRACKER POLICY SETTINGS:");
            super.dump(pw, "  " + prefix);
        }
    }
}
