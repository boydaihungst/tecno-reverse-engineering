package com.android.server.am;

import android.content.Context;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.SparseArray;
import com.android.internal.app.ProcessMap;
import com.android.server.am.BaseAppStateDurationsTracker;
import com.android.server.am.BaseAppStateEvents;
import com.android.server.am.BaseAppStateEventsTracker;
import com.android.server.am.BaseAppStateTracker;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class AppMediaSessionTracker extends BaseAppStateDurationsTracker<AppMediaSessionPolicy, BaseAppStateDurationsTracker.SimplePackageDurations> {
    static final boolean DEBUG_MEDIA_SESSION_TRACKER = false;
    static final String TAG = "ActivityManager";
    private final HandlerExecutor mHandlerExecutor;
    private final MediaSessionManager.OnActiveSessionsChangedListener mSessionsChangedListener;
    private final ProcessMap<Boolean> mTmpMediaControllers;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppMediaSessionTracker(Context context, AppRestrictionController controller) {
        this(context, controller, null, null);
    }

    AppMediaSessionTracker(Context context, AppRestrictionController controller, Constructor<? extends BaseAppStateTracker.Injector<AppMediaSessionPolicy>> injector, Object outerContext) {
        super(context, controller, injector, outerContext);
        this.mSessionsChangedListener = new MediaSessionManager.OnActiveSessionsChangedListener() { // from class: com.android.server.am.AppMediaSessionTracker$$ExternalSyntheticLambda0
            @Override // android.media.session.MediaSessionManager.OnActiveSessionsChangedListener
            public final void onActiveSessionsChanged(List list) {
                AppMediaSessionTracker.this.handleMediaSessionChanged(list);
            }
        };
        this.mTmpMediaControllers = new ProcessMap<>();
        this.mHandlerExecutor = new HandlerExecutor(this.mBgHandler);
        this.mInjector.setPolicy(new AppMediaSessionPolicy(this.mInjector, this));
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.am.BaseAppStateEvents.Factory
    public BaseAppStateDurationsTracker.SimplePackageDurations createAppStateEvents(int uid, String packageName) {
        return new BaseAppStateDurationsTracker.SimplePackageDurations(uid, packageName, (BaseAppStateEvents.MaxTrackingDurationConfig) this.mInjector.getPolicy());
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.am.BaseAppStateEvents.Factory
    public BaseAppStateDurationsTracker.SimplePackageDurations createAppStateEvents(BaseAppStateDurationsTracker.SimplePackageDurations other) {
        return new BaseAppStateDurationsTracker.SimplePackageDurations(other);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onBgMediaSessionMonitorEnabled(boolean enabled) {
        if (enabled) {
            this.mInjector.getMediaSessionManager().addOnActiveSessionsChangedListener(null, UserHandle.ALL, this.mHandlerExecutor, this.mSessionsChangedListener);
        } else {
            this.mInjector.getMediaSessionManager().removeOnActiveSessionsChangedListener(this.mSessionsChangedListener);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleMediaSessionChanged(List<MediaController> controllers) {
        BaseAppStateDurationsTracker.SimplePackageDurations pkg;
        int uid;
        if (controllers != null) {
            synchronized (this.mLock) {
                long now = SystemClock.elapsedRealtime();
                for (MediaController controller : controllers) {
                    String packageName = controller.getPackageName();
                    int uid2 = controller.getSessionToken().getUid();
                    BaseAppStateDurationsTracker.SimplePackageDurations pkg2 = (BaseAppStateDurationsTracker.SimplePackageDurations) this.mPkgEvents.get(uid2, packageName);
                    if (pkg2 != null) {
                        pkg = pkg2;
                    } else {
                        BaseAppStateDurationsTracker.SimplePackageDurations pkg3 = createAppStateEvents(uid2, packageName);
                        this.mPkgEvents.put(uid2, packageName, pkg3);
                        pkg = pkg3;
                    }
                    if (pkg.isActive()) {
                        uid = uid2;
                    } else {
                        pkg.addEvent(true, now);
                        uid = uid2;
                        notifyListenersOnStateChange(pkg.mUid, pkg.mPackageName, true, now, 1);
                    }
                    this.mTmpMediaControllers.put(packageName, uid, Boolean.TRUE);
                }
                SparseArray map = this.mPkgEvents.getMap();
                for (int i = map.size() - 1; i >= 0; i--) {
                    ArrayMap<String, BaseAppStateDurationsTracker.SimplePackageDurations> val = (ArrayMap) map.valueAt(i);
                    for (int j = val.size() - 1; j >= 0; j--) {
                        BaseAppStateDurationsTracker.SimplePackageDurations pkg4 = val.valueAt(j);
                        if (pkg4.isActive() && this.mTmpMediaControllers.get(pkg4.mPackageName, pkg4.mUid) == null) {
                            pkg4.addEvent(false, now);
                            notifyListenersOnStateChange(pkg4.mUid, pkg4.mPackageName, false, now, 1);
                        }
                    }
                }
            }
            this.mTmpMediaControllers.clear();
            return;
        }
        synchronized (this.mLock) {
            SparseArray map2 = this.mPkgEvents.getMap();
            long now2 = SystemClock.elapsedRealtime();
            for (int i2 = map2.size() - 1; i2 >= 0; i2--) {
                ArrayMap<String, BaseAppStateDurationsTracker.SimplePackageDurations> val2 = (ArrayMap) map2.valueAt(i2);
                for (int j2 = val2.size() - 1; j2 >= 0; j2--) {
                    BaseAppStateDurationsTracker.SimplePackageDurations pkg5 = val2.valueAt(j2);
                    if (pkg5.isActive()) {
                        pkg5.addEvent(false, now2);
                        notifyListenersOnStateChange(pkg5.mUid, pkg5.mPackageName, false, now2, 1);
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void trimDurations() {
        long now = SystemClock.elapsedRealtime();
        trim(Math.max(0L, now - ((AppMediaSessionPolicy) this.mInjector.getPolicy()).getMaxTrackingDuration()));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public int getType() {
        return 4;
    }

    @Override // com.android.server.am.BaseAppStateEventsTracker, com.android.server.am.BaseAppStateTracker
    void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.println("APP MEDIA SESSION TRACKER:");
        super.dump(pw, "  " + prefix);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class AppMediaSessionPolicy extends BaseAppStateEventsTracker.BaseAppStateEventsPolicy<AppMediaSessionTracker> {
        static final boolean DEFAULT_BG_MEDIA_SESSION_MONITOR_ENABLED = true;
        static final long DEFAULT_BG_MEDIA_SESSION_MONITOR_MAX_TRACKING_DURATION = 345600000;
        static final String KEY_BG_MEADIA_SESSION_MONITOR_ENABLED = "bg_media_session_monitor_enabled";
        static final String KEY_BG_MEDIA_SESSION_MONITOR_MAX_TRACKING_DURATION = "bg_media_session_monitor_max_tracking_duration";

        AppMediaSessionPolicy(BaseAppStateTracker.Injector injector, AppMediaSessionTracker tracker) {
            super(injector, tracker, KEY_BG_MEADIA_SESSION_MONITOR_ENABLED, true, KEY_BG_MEDIA_SESSION_MONITOR_MAX_TRACKING_DURATION, DEFAULT_BG_MEDIA_SESSION_MONITOR_MAX_TRACKING_DURATION);
        }

        @Override // com.android.server.am.BaseAppStatePolicy
        public void onTrackerEnabled(boolean enabled) {
            ((AppMediaSessionTracker) this.mTracker).onBgMediaSessionMonitorEnabled(enabled);
        }

        @Override // com.android.server.am.BaseAppStateEventsTracker.BaseAppStateEventsPolicy
        public void onMaxTrackingDurationChanged(long maxDuration) {
            Handler handler = ((AppMediaSessionTracker) this.mTracker).mBgHandler;
            final AppMediaSessionTracker appMediaSessionTracker = (AppMediaSessionTracker) this.mTracker;
            Objects.requireNonNull(appMediaSessionTracker);
            handler.post(new Runnable() { // from class: com.android.server.am.AppMediaSessionTracker$AppMediaSessionPolicy$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    AppMediaSessionTracker.this.trimDurations();
                }
            });
        }

        @Override // com.android.server.am.BaseAppStateEventsTracker.BaseAppStateEventsPolicy
        String getExemptionReasonString(String packageName, int uid, int reason) {
            return "n/a";
        }

        @Override // com.android.server.am.BaseAppStateEventsTracker.BaseAppStateEventsPolicy, com.android.server.am.BaseAppStatePolicy
        void dump(PrintWriter pw, String prefix) {
            pw.print(prefix);
            pw.println("APP MEDIA SESSION TRACKER POLICY SETTINGS:");
            super.dump(pw, "  " + prefix);
        }
    }
}
