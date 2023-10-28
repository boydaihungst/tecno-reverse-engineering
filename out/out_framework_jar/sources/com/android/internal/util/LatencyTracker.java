package com.android.internal.util;

import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.os.Trace;
import android.provider.DeviceConfig;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.os.BackgroundThread;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
/* loaded from: classes4.dex */
public class LatencyTracker {
    public static final int ACTION_CHECK_CREDENTIAL = 3;
    public static final int ACTION_CHECK_CREDENTIAL_UNLOCKED = 4;
    public static final int ACTION_EXPAND_PANEL = 0;
    public static final int ACTION_FACE_WAKE_AND_UNLOCK = 7;
    public static final int ACTION_FINGERPRINT_WAKE_AND_UNLOCK = 2;
    public static final int ACTION_LOAD_SHARE_SHEET = 16;
    public static final int ACTION_LOCKSCREEN_UNLOCK = 11;
    public static final int ACTION_ROTATE_SCREEN = 6;
    public static final int ACTION_ROTATE_SCREEN_CAMERA_CHECK = 10;
    public static final int ACTION_ROTATE_SCREEN_SENSOR = 9;
    public static final int ACTION_SHOW_BACK_ARROW = 15;
    public static final int ACTION_START_RECENTS_ANIMATION = 8;
    public static final int ACTION_SWITCH_DISPLAY_UNFOLD = 13;
    public static final int ACTION_TOGGLE_RECENTS = 1;
    public static final int ACTION_TURN_ON_SCREEN = 5;
    public static final int ACTION_UDFPS_ILLUMINATE = 14;
    public static final int ACTION_USER_SWITCH = 12;
    private static final boolean DEBUG = false;
    private static final int DEFAULT_SAMPLING_INTERVAL = 5;
    public static final String SETTINGS_ENABLED_KEY = "enabled";
    private static final String SETTINGS_SAMPLING_INTERVAL_KEY = "sampling_interval";
    private static final String TAG = "LatencyTracker";
    private static LatencyTracker sLatencyTracker;
    private static final boolean DEFAULT_ENABLED = Build.IS_DEBUGGABLE;
    private static final int[] ACTIONS_ALL = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
    private static final int[] STATSD_ACTION = {1, 2, 3, 4, 5, 6, 7, 8, 9, 11, 10, 12, 13, 14, 15, 16, 17};
    private final Object mLock = new Object();
    private final SparseArray<Session> mSessions = new SparseArray<>();
    private final int[] mTraceThresholdPerAction = new int[ACTIONS_ALL.length];
    private boolean mEnabled = DEFAULT_ENABLED;
    private int mSamplingInterval = 5;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes4.dex */
    public @interface Action {
    }

    public static LatencyTracker getInstance(Context context) {
        if (sLatencyTracker == null) {
            synchronized (LatencyTracker.class) {
                if (sLatencyTracker == null) {
                    sLatencyTracker = new LatencyTracker();
                }
            }
        }
        return sLatencyTracker;
    }

    private LatencyTracker() {
        BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.internal.util.LatencyTracker$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                LatencyTracker.this.m6938lambda$new$0$comandroidinternalutilLatencyTracker();
            }
        });
        DeviceConfig.addOnPropertiesChangedListener(DeviceConfig.NAMESPACE_LATENCY_TRACKER, BackgroundThread.getExecutor(), new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.internal.util.LatencyTracker$$ExternalSyntheticLambda2
            @Override // android.provider.DeviceConfig.OnPropertiesChangedListener
            public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                LatencyTracker.this.updateProperties(properties);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-internal-util-LatencyTracker  reason: not valid java name */
    public /* synthetic */ void m6938lambda$new$0$comandroidinternalutilLatencyTracker() {
        updateProperties(DeviceConfig.getProperties(DeviceConfig.NAMESPACE_LATENCY_TRACKER, new String[0]));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateProperties(DeviceConfig.Properties properties) {
        int[] iArr;
        synchronized (this.mLock) {
            this.mSamplingInterval = properties.getInt("sampling_interval", 5);
            this.mEnabled = properties.getBoolean("enabled", DEFAULT_ENABLED);
            for (int action : ACTIONS_ALL) {
                this.mTraceThresholdPerAction[action] = properties.getInt(getNameOfAction(STATSD_ACTION[action]), -1);
            }
        }
    }

    public static String getNameOfAction(int atomsProtoAction) {
        switch (atomsProtoAction) {
            case 0:
                return "UNKNOWN";
            case 1:
                return "ACTION_EXPAND_PANEL";
            case 2:
                return "ACTION_TOGGLE_RECENTS";
            case 3:
                return "ACTION_FINGERPRINT_WAKE_AND_UNLOCK";
            case 4:
                return "ACTION_CHECK_CREDENTIAL";
            case 5:
                return "ACTION_CHECK_CREDENTIAL_UNLOCKED";
            case 6:
                return "ACTION_TURN_ON_SCREEN";
            case 7:
                return "ACTION_ROTATE_SCREEN";
            case 8:
                return "ACTION_FACE_WAKE_AND_UNLOCK";
            case 9:
                return "ACTION_START_RECENTS_ANIMATION";
            case 10:
                return "ACTION_ROTATE_SCREEN_CAMERA_CHECK";
            case 11:
                return "ACTION_ROTATE_SCREEN_SENSOR";
            case 12:
                return "ACTION_LOCKSCREEN_UNLOCK";
            case 13:
                return "ACTION_USER_SWITCH";
            case 14:
                return "ACTION_SWITCH_DISPLAY_UNFOLD";
            case 15:
                return "ACTION_UDFPS_ILLUMINATE";
            case 16:
                return "ACTION_SHOW_BACK_ARROW";
            case 17:
                return "ACTION_LOAD_SHARE_SHEET";
            default:
                throw new IllegalArgumentException("Invalid action");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String getTraceNameOfAction(int action, String tag) {
        if (TextUtils.isEmpty(tag)) {
            return "L<" + getNameOfAction(STATSD_ACTION[action]) + ">";
        }
        return "L<" + getNameOfAction(STATSD_ACTION[action]) + "::" + tag + ">";
    }

    private static String getTraceTriggerNameForAction(int action) {
        return "com.android.telemetry.latency-tracker-" + getNameOfAction(STATSD_ACTION[action]);
    }

    public static boolean isEnabled(Context ctx) {
        return getInstance(ctx).isEnabled();
    }

    public boolean isEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mEnabled;
        }
        return z;
    }

    public void onActionStart(int action) {
        onActionStart(action, null);
    }

    public void onActionStart(final int action, String tag) {
        synchronized (this.mLock) {
            if (isEnabled()) {
                if (this.mSessions.get(action) != null) {
                    return;
                }
                Session session = new Session(action, tag);
                session.begin(new Runnable() { // from class: com.android.internal.util.LatencyTracker$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        LatencyTracker.this.m6939lambda$onActionStart$1$comandroidinternalutilLatencyTracker(action);
                    }
                });
                this.mSessions.put(action, session);
            }
        }
    }

    public void onActionEnd(int action) {
        synchronized (this.mLock) {
            if (isEnabled()) {
                Session session = this.mSessions.get(action);
                if (session == null) {
                    return;
                }
                session.end();
                this.mSessions.delete(action);
                logAction(action, session.duration());
            }
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* renamed from: onActionCancel */
    public void m6939lambda$onActionStart$1$comandroidinternalutilLatencyTracker(int action) {
        synchronized (this.mLock) {
            Session session = this.mSessions.get(action);
            if (session == null) {
                return;
            }
            session.cancel();
            this.mSessions.delete(action);
        }
    }

    public void logAction(int action, int duration) {
        boolean shouldSample;
        int traceThreshold;
        synchronized (this.mLock) {
            shouldSample = ThreadLocalRandom.current().nextInt() % this.mSamplingInterval == 0;
            traceThreshold = this.mTraceThresholdPerAction[action];
        }
        if (traceThreshold > 0 && duration >= traceThreshold) {
            PerfettoTrigger.trigger(getTraceTriggerNameForAction(action));
        }
        logActionDeprecated(action, duration, shouldSample);
    }

    public static void logActionDeprecated(int action, int duration, boolean writeToStatsLog) {
        StringBuilder sb = new StringBuilder();
        int[] iArr = STATSD_ACTION;
        Log.i(TAG, sb.append(getNameOfAction(iArr[action])).append(" latency=").append(duration).toString());
        EventLog.writeEvent(36070, Integer.valueOf(action), Integer.valueOf(duration));
        if (writeToStatsLog) {
            FrameworkStatsLog.write(306, iArr[action], duration);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes4.dex */
    public static class Session {
        private final int mAction;
        private final String mName;
        private final String mTag;
        private Runnable mTimeoutRunnable;
        private long mStartRtc = -1;
        private long mEndRtc = -1;

        Session(int action, String tag) {
            String str;
            this.mAction = action;
            this.mTag = tag;
            if (TextUtils.isEmpty(tag)) {
                str = LatencyTracker.getNameOfAction(LatencyTracker.STATSD_ACTION[action]);
            } else {
                str = LatencyTracker.getNameOfAction(LatencyTracker.STATSD_ACTION[action]) + "::" + tag;
            }
            this.mName = str;
        }

        String name() {
            return this.mName;
        }

        String traceName() {
            return LatencyTracker.getTraceNameOfAction(this.mAction, this.mTag);
        }

        void begin(Runnable timeoutAction) {
            this.mStartRtc = SystemClock.elapsedRealtime();
            Trace.asyncTraceBegin(4096L, traceName(), 0);
            this.mTimeoutRunnable = timeoutAction;
            BackgroundThread.getHandler().postDelayed(this.mTimeoutRunnable, TimeUnit.SECONDS.toMillis(15L));
        }

        void end() {
            this.mEndRtc = SystemClock.elapsedRealtime();
            Trace.asyncTraceEnd(4096L, traceName(), 0);
            BackgroundThread.getHandler().removeCallbacks(this.mTimeoutRunnable);
            this.mTimeoutRunnable = null;
        }

        void cancel() {
            Trace.asyncTraceEnd(4096L, traceName(), 0);
            BackgroundThread.getHandler().removeCallbacks(this.mTimeoutRunnable);
            this.mTimeoutRunnable = null;
        }

        int duration() {
            return (int) (this.mEndRtc - this.mStartRtc);
        }
    }
}
