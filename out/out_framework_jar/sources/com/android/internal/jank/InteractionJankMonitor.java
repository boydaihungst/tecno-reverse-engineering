package com.android.internal.jank;

import android.content.Context;
import android.os.Build;
import android.os.HandlerExecutor;
import android.os.HandlerThread;
import android.provider.DeviceConfig;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.Choreographer;
import android.view.SurfaceControl;
import android.view.View;
import com.android.internal.jank.FrameTracker;
import com.android.internal.jank.InteractionJankMonitor;
import com.android.internal.util.PerfettoTrigger;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
/* loaded from: classes4.dex */
public class InteractionJankMonitor {
    private static final String ACTION_PREFIX;
    public static final String ACTION_SESSION_CANCEL;
    public static final String ACTION_SESSION_END;
    public static final int CUJ_BIOMETRIC_PROMPT_TRANSITION = 56;
    public static final int CUJ_LAUNCHER_ALL_APPS_SCROLL = 26;
    public static final int CUJ_LAUNCHER_APP_CLOSE_TO_HOME = 9;
    public static final int CUJ_LAUNCHER_APP_CLOSE_TO_PIP = 10;
    public static final int CUJ_LAUNCHER_APP_LAUNCH_FROM_ICON = 8;
    public static final int CUJ_LAUNCHER_APP_LAUNCH_FROM_RECENTS = 7;
    public static final int CUJ_LAUNCHER_APP_LAUNCH_FROM_WIDGET = 27;
    public static final int CUJ_LAUNCHER_OPEN_ALL_APPS = 25;
    public static final int CUJ_LAUNCHER_QUICK_SWITCH = 11;
    public static final int CUJ_LOCKSCREEN_LAUNCH_CAMERA = 51;
    public static final int CUJ_LOCKSCREEN_PASSWORD_APPEAR = 17;
    public static final int CUJ_LOCKSCREEN_PASSWORD_DISAPPEAR = 20;
    public static final int CUJ_LOCKSCREEN_PATTERN_APPEAR = 18;
    public static final int CUJ_LOCKSCREEN_PATTERN_DISAPPEAR = 21;
    public static final int CUJ_LOCKSCREEN_PIN_APPEAR = 19;
    public static final int CUJ_LOCKSCREEN_PIN_DISAPPEAR = 22;
    public static final int CUJ_LOCKSCREEN_TRANSITION_FROM_AOD = 23;
    public static final int CUJ_LOCKSCREEN_TRANSITION_TO_AOD = 24;
    public static final int CUJ_LOCKSCREEN_UNLOCK_ANIMATION = 29;
    public static final int CUJ_NOTIFICATION_ADD = 14;
    public static final int CUJ_NOTIFICATION_APP_START = 16;
    public static final int CUJ_NOTIFICATION_HEADS_UP_APPEAR = 12;
    public static final int CUJ_NOTIFICATION_HEADS_UP_DISAPPEAR = 13;
    public static final int CUJ_NOTIFICATION_REMOVE = 15;
    public static final int CUJ_NOTIFICATION_SHADE_EXPAND_COLLAPSE = 0;
    public static final int CUJ_NOTIFICATION_SHADE_EXPAND_COLLAPSE_LOCK = 1;
    public static final int CUJ_NOTIFICATION_SHADE_QS_EXPAND_COLLAPSE = 5;
    public static final int CUJ_NOTIFICATION_SHADE_QS_SCROLL_SWIPE = 6;
    public static final int CUJ_NOTIFICATION_SHADE_ROW_EXPAND = 3;
    public static final int CUJ_NOTIFICATION_SHADE_ROW_SWIPE = 4;
    public static final int CUJ_NOTIFICATION_SHADE_SCROLL_FLING = 2;
    public static final int CUJ_ONE_HANDED_ENTER_TRANSITION = 42;
    public static final int CUJ_ONE_HANDED_EXIT_TRANSITION = 43;
    public static final int CUJ_PIP_TRANSITION = 35;
    public static final int CUJ_SCREEN_OFF = 40;
    public static final int CUJ_SCREEN_OFF_SHOW_AOD = 41;
    public static final int CUJ_SETTINGS_PAGE_SCROLL = 28;
    public static final int CUJ_SETTINGS_SLIDER = 53;
    public static final int CUJ_SETTINGS_TOGGLE = 57;
    public static final int CUJ_SHADE_APP_LAUNCH_FROM_HISTORY_BUTTON = 30;
    public static final int CUJ_SHADE_APP_LAUNCH_FROM_MEDIA_PLAYER = 31;
    public static final int CUJ_SHADE_APP_LAUNCH_FROM_QS_TILE = 32;
    public static final int CUJ_SHADE_APP_LAUNCH_FROM_SETTINGS_BUTTON = 33;
    public static final int CUJ_SPLASHSCREEN_AVD = 38;
    public static final int CUJ_SPLASHSCREEN_EXIT_ANIM = 39;
    public static final int CUJ_SPLIT_SCREEN_ENTER = 49;
    public static final int CUJ_SPLIT_SCREEN_EXIT = 50;
    public static final int CUJ_SPLIT_SCREEN_RESIZE = 52;
    public static final int CUJ_STATUS_BAR_APP_LAUNCH_FROM_CALL_CHIP = 34;
    public static final int CUJ_SUW_LOADING_SCREEN_FOR_STATUS = 48;
    public static final int CUJ_SUW_LOADING_TO_NEXT_FLOW = 47;
    public static final int CUJ_SUW_LOADING_TO_SHOW_INFO_WITH_ACTIONS = 45;
    public static final int CUJ_SUW_SHOW_FUNCTION_SCREEN_WITH_ACTIONS = 46;
    public static final int CUJ_TAKE_SCREENSHOT = 54;
    public static final int[] CUJ_TO_STATSD_INTERACTION_TYPE;
    public static final int CUJ_UNFOLD_ANIM = 44;
    public static final int CUJ_USER_SWITCH = 37;
    public static final int CUJ_VOLUME_CONTROL = 55;
    public static final int CUJ_WALLPAPER_TRANSITION = 36;
    private static final boolean DEBUG = false;
    private static final boolean DEFAULT_ENABLED;
    private static final int DEFAULT_SAMPLING_INTERVAL = 1;
    private static final long DEFAULT_TIMEOUT_MS;
    private static final int DEFAULT_TRACE_THRESHOLD_FRAME_TIME_MILLIS = 64;
    private static final int DEFAULT_TRACE_THRESHOLD_MISSED_FRAMES = 3;
    private static final String DEFAULT_WORKER_NAME;
    private static final int NO_STATSD_LOGGING = -1;
    private static final String SETTINGS_ENABLED_KEY = "enabled";
    private static final String SETTINGS_SAMPLING_INTERVAL_KEY = "sampling_interval";
    private static final String SETTINGS_THRESHOLD_FRAME_TIME_MILLIS_KEY = "trace_threshold_frame_time_millis";
    private static final String SETTINGS_THRESHOLD_MISSED_FRAMES_KEY = "trace_threshold_missed_frames";
    private static final String TAG;
    private static volatile InteractionJankMonitor sInstance;
    private boolean mEnabled;
    private final Object mLock;
    private final FrameTracker.FrameMetricsWrapper mMetrics;
    private final DeviceConfig.OnPropertiesChangedListener mPropertiesChangedListener;
    private final SparseArray<FrameTracker> mRunningTrackers;
    private int mSamplingInterval;
    private final SparseArray<Runnable> mTimeoutActions;
    private int mTraceThresholdFrameTimeMillis;
    private int mTraceThresholdMissedFrames;
    private final HandlerThread mWorker;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes4.dex */
    public @interface CujType {
    }

    static {
        String simpleName = InteractionJankMonitor.class.getSimpleName();
        TAG = simpleName;
        String canonicalName = InteractionJankMonitor.class.getCanonicalName();
        ACTION_PREFIX = canonicalName;
        DEFAULT_WORKER_NAME = simpleName + "-Worker";
        DEFAULT_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(2L);
        DEFAULT_ENABLED = Build.IS_DEBUGGABLE;
        ACTION_SESSION_END = canonicalName + ".ACTION_SESSION_END";
        ACTION_SESSION_CANCEL = canonicalName + ".ACTION_SESSION_CANCEL";
        CUJ_TO_STATSD_INTERACTION_TYPE = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58};
    }

    public static InteractionJankMonitor getInstance() {
        if (sInstance == null) {
            synchronized (InteractionJankMonitor.class) {
                if (sInstance == null) {
                    sInstance = new InteractionJankMonitor(new HandlerThread(DEFAULT_WORKER_NAME));
                }
            }
        }
        return sInstance;
    }

    public InteractionJankMonitor(HandlerThread worker) {
        DeviceConfig.OnPropertiesChangedListener onPropertiesChangedListener = new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.internal.jank.InteractionJankMonitor$$ExternalSyntheticLambda0
            @Override // android.provider.DeviceConfig.OnPropertiesChangedListener
            public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                InteractionJankMonitor.this.updateProperties(properties);
            }
        };
        this.mPropertiesChangedListener = onPropertiesChangedListener;
        this.mLock = new Object();
        boolean z = DEFAULT_ENABLED;
        this.mEnabled = z;
        this.mSamplingInterval = 1;
        this.mTraceThresholdMissedFrames = 3;
        this.mTraceThresholdFrameTimeMillis = 64;
        this.mRunningTrackers = new SparseArray<>();
        this.mTimeoutActions = new SparseArray<>();
        this.mWorker = worker;
        this.mMetrics = new FrameTracker.FrameMetricsWrapper();
        worker.start();
        this.mEnabled = z;
        this.mSamplingInterval = 1;
        worker.getThreadHandler().post(new Runnable() { // from class: com.android.internal.jank.InteractionJankMonitor$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                InteractionJankMonitor.this.m6685lambda$new$0$comandroidinternaljankInteractionJankMonitor();
            }
        });
        DeviceConfig.addOnPropertiesChangedListener(DeviceConfig.NAMESPACE_INTERACTION_JANK_MONITOR, new HandlerExecutor(worker.getThreadHandler()), onPropertiesChangedListener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-internal-jank-InteractionJankMonitor  reason: not valid java name */
    public /* synthetic */ void m6685lambda$new$0$comandroidinternaljankInteractionJankMonitor() {
        this.mPropertiesChangedListener.onPropertiesChanged(DeviceConfig.getProperties(DeviceConfig.NAMESPACE_INTERACTION_JANK_MONITOR, new String[0]));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Object getLock() {
        return this.mLock;
    }

    public FrameTracker createFrameTracker(Configuration config, Session session) {
        FrameTracker frameTracker;
        View view = config.mView;
        FrameTracker.ThreadedRendererWrapper threadedRenderer = view == null ? null : new FrameTracker.ThreadedRendererWrapper(view.getThreadedRenderer());
        FrameTracker.ViewRootWrapper viewRoot = view != null ? new FrameTracker.ViewRootWrapper(view.getViewRootImpl()) : null;
        FrameTracker.SurfaceControlWrapper surfaceControl = new FrameTracker.SurfaceControlWrapper();
        FrameTracker.ChoreographerWrapper choreographer = new FrameTracker.ChoreographerWrapper(Choreographer.getInstance());
        synchronized (this.mLock) {
            FrameTracker.FrameTrackerListener eventsListener = new FrameTracker.FrameTrackerListener() { // from class: com.android.internal.jank.InteractionJankMonitor$$ExternalSyntheticLambda4
                @Override // com.android.internal.jank.FrameTracker.FrameTrackerListener
                public final void onCujEvents(InteractionJankMonitor.Session session2, String str) {
                    InteractionJankMonitor.this.m6684xa036dbda(session2, str);
                }
            };
            frameTracker = new FrameTracker(session, this.mWorker.getThreadHandler(), threadedRenderer, viewRoot, surfaceControl, choreographer, this.mMetrics, new FrameTracker.StatsLogWrapper(), this.mTraceThresholdMissedFrames, this.mTraceThresholdFrameTimeMillis, eventsListener, config);
        }
        return frameTracker;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: handleCujEvents */
    public void m6684xa036dbda(String action, Session session) {
        if (needRemoveTasks(action, session)) {
            removeTimeout(session.getCuj());
            removeTracker(session.getCuj());
        }
    }

    private boolean needRemoveTasks(String action, Session session) {
        boolean badEnd = action.equals(ACTION_SESSION_END) && session.getReason() != 0;
        boolean badCancel = (!action.equals(ACTION_SESSION_CANCEL) || session.getReason() == 16 || session.getReason() == 19) ? false : true;
        return badEnd || badCancel;
    }

    private void removeTimeout(int cujType) {
        synchronized (this.mLock) {
            Runnable timeout = this.mTimeoutActions.get(cujType);
            if (timeout != null) {
                this.mWorker.getThreadHandler().removeCallbacks(timeout);
                this.mTimeoutActions.remove(cujType);
            }
        }
    }

    public boolean isInstrumenting(int cujType) {
        boolean contains;
        synchronized (this.mLock) {
            contains = this.mRunningTrackers.contains(cujType);
        }
        return contains;
    }

    public boolean begin(View v, int cujType) {
        try {
            return beginInternal(Configuration.Builder.withView(cujType, v).build());
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "Build configuration failed!", ex);
            return false;
        }
    }

    public boolean begin(Configuration.Builder builder) {
        try {
            return beginInternal(builder.build());
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "Build configuration failed!", ex);
            return false;
        }
    }

    private boolean beginInternal(Configuration conf) {
        synchronized (this.mLock) {
            final int cujType = conf.mCujType;
            if (shouldMonitor(cujType)) {
                if (getTracker(cujType) != null) {
                    return false;
                }
                FrameTracker tracker = createFrameTracker(conf, new Session(cujType, conf.mTag));
                this.mRunningTrackers.put(cujType, tracker);
                tracker.begin();
                scheduleTimeoutAction(cujType, conf.mTimeout, new Runnable() { // from class: com.android.internal.jank.InteractionJankMonitor$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        InteractionJankMonitor.this.m6683x4d0cf994(cujType);
                    }
                });
                return true;
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$beginInternal$2$com-android-internal-jank-InteractionJankMonitor  reason: not valid java name */
    public /* synthetic */ void m6683x4d0cf994(int cujType) {
        cancel(cujType, 19);
    }

    public boolean shouldMonitor(int cujType) {
        boolean shouldSample = ThreadLocalRandom.current().nextInt() % this.mSamplingInterval == 0;
        return this.mEnabled && shouldSample;
    }

    public void scheduleTimeoutAction(int cuj, long timeout, Runnable action) {
        this.mTimeoutActions.put(cuj, action);
        this.mWorker.getThreadHandler().postDelayed(action, timeout);
    }

    public boolean end(int cujType) {
        try {
            synchronized (this.mLock) {
                removeTimeout(cujType);
                FrameTracker tracker = getTracker(cujType);
                if (tracker == null) {
                    return false;
                }
                if (tracker.end(0)) {
                    removeTracker(cujType);
                }
                return true;
            }
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, ex.toString());
            return false;
        }
    }

    public boolean cancel(int cujType) {
        return cancel(cujType, 16);
    }

    public boolean cancel(int cujType, int reason) {
        synchronized (this.mLock) {
            removeTimeout(cujType);
            FrameTracker tracker = getTracker(cujType);
            if (tracker == null) {
                return false;
            }
            if (tracker.cancel(reason)) {
                removeTracker(cujType);
            }
            return true;
        }
    }

    private FrameTracker getTracker(int cuj) {
        return this.mRunningTrackers.get(cuj);
    }

    private void removeTracker(int cuj) {
        this.mRunningTrackers.remove(cuj);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateProperties(DeviceConfig.Properties properties) {
        synchronized (this.mLock) {
            this.mSamplingInterval = properties.getInt("sampling_interval", 1);
            this.mEnabled = properties.getBoolean("enabled", DEFAULT_ENABLED);
            this.mTraceThresholdMissedFrames = properties.getInt(SETTINGS_THRESHOLD_MISSED_FRAMES_KEY, 3);
            this.mTraceThresholdFrameTimeMillis = properties.getInt(SETTINGS_THRESHOLD_FRAME_TIME_MILLIS_KEY, 64);
        }
    }

    public DeviceConfig.OnPropertiesChangedListener getPropertiesChangedListener() {
        return this.mPropertiesChangedListener;
    }

    public void trigger(final Session session) {
        this.mWorker.getThreadHandler().post(new Runnable() { // from class: com.android.internal.jank.InteractionJankMonitor$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                PerfettoTrigger.trigger(InteractionJankMonitor.Session.this.getPerfettoTrigger());
            }
        });
    }

    public static String getNameOfInteraction(int interactionType) {
        return getNameOfCuj(getCujTypeFromInteraction(interactionType));
    }

    private static int getCujTypeFromInteraction(int interactionType) {
        return interactionType - 1;
    }

    public static String getNameOfCuj(int cujType) {
        switch (cujType) {
            case 0:
                return "SHADE_EXPAND_COLLAPSE";
            case 1:
                return "SHADE_EXPAND_COLLAPSE_LOCK";
            case 2:
                return "SHADE_SCROLL_FLING";
            case 3:
                return "SHADE_ROW_EXPAND";
            case 4:
                return "SHADE_ROW_SWIPE";
            case 5:
                return "SHADE_QS_EXPAND_COLLAPSE";
            case 6:
                return "SHADE_QS_SCROLL_SWIPE";
            case 7:
                return "LAUNCHER_APP_LAUNCH_FROM_RECENTS";
            case 8:
                return "LAUNCHER_APP_LAUNCH_FROM_ICON";
            case 9:
                return "LAUNCHER_APP_CLOSE_TO_HOME";
            case 10:
                return "LAUNCHER_APP_CLOSE_TO_PIP";
            case 11:
                return "LAUNCHER_QUICK_SWITCH";
            case 12:
                return "NOTIFICATION_HEADS_UP_APPEAR";
            case 13:
                return "NOTIFICATION_HEADS_UP_DISAPPEAR";
            case 14:
                return "NOTIFICATION_ADD";
            case 15:
                return "NOTIFICATION_REMOVE";
            case 16:
                return "NOTIFICATION_APP_START";
            case 17:
                return "LOCKSCREEN_PASSWORD_APPEAR";
            case 18:
                return "LOCKSCREEN_PATTERN_APPEAR";
            case 19:
                return "LOCKSCREEN_PIN_APPEAR";
            case 20:
                return "LOCKSCREEN_PASSWORD_DISAPPEAR";
            case 21:
                return "LOCKSCREEN_PATTERN_DISAPPEAR";
            case 22:
                return "LOCKSCREEN_PIN_DISAPPEAR";
            case 23:
                return "LOCKSCREEN_TRANSITION_FROM_AOD";
            case 24:
                return "LOCKSCREEN_TRANSITION_TO_AOD";
            case 25:
                return "LAUNCHER_OPEN_ALL_APPS";
            case 26:
                return "LAUNCHER_ALL_APPS_SCROLL";
            case 27:
                return "LAUNCHER_APP_LAUNCH_FROM_WIDGET";
            case 28:
                return "SETTINGS_PAGE_SCROLL";
            case 29:
                return "LOCKSCREEN_UNLOCK_ANIMATION";
            case 30:
                return "SHADE_APP_LAUNCH_FROM_HISTORY_BUTTON";
            case 31:
                return "SHADE_APP_LAUNCH_FROM_MEDIA_PLAYER";
            case 32:
                return "SHADE_APP_LAUNCH_FROM_QS_TILE";
            case 33:
                return "SHADE_APP_LAUNCH_FROM_SETTINGS_BUTTON";
            case 34:
                return "STATUS_BAR_APP_LAUNCH_FROM_CALL_CHIP";
            case 35:
                return "PIP_TRANSITION";
            case 36:
                return "WALLPAPER_TRANSITION";
            case 37:
                return "USER_SWITCH";
            case 38:
                return "SPLASHSCREEN_AVD";
            case 39:
                return "SPLASHSCREEN_EXIT_ANIM";
            case 40:
                return "SCREEN_OFF";
            case 41:
                return "SCREEN_OFF_SHOW_AOD";
            case 42:
                return "ONE_HANDED_ENTER_TRANSITION";
            case 43:
                return "ONE_HANDED_EXIT_TRANSITION";
            case 44:
                return "UNFOLD_ANIM";
            case 45:
                return "SUW_LOADING_TO_SHOW_INFO_WITH_ACTIONS";
            case 46:
                return "SUW_SHOW_FUNCTION_SCREEN_WITH_ACTIONS";
            case 47:
                return "SUW_LOADING_TO_NEXT_FLOW";
            case 48:
                return "SUW_LOADING_SCREEN_FOR_STATUS";
            case 49:
                return "SPLIT_SCREEN_ENTER";
            case 50:
                return "SPLIT_SCREEN_EXIT";
            case 51:
                return "CUJ_LOCKSCREEN_LAUNCH_CAMERA";
            case 52:
                return "CUJ_SPLIT_SCREEN_RESIZE";
            case 53:
                return "SETTINGS_SLIDER";
            case 54:
                return "TAKE_SCREENSHOT";
            case 55:
                return "VOLUME_CONTROL";
            case 56:
                return "BIOMETRIC_PROMPT_TRANSITION";
            case 57:
                return "SETTINGS_TOGGLE";
            default:
                return "UNKNOWN";
        }
    }

    /* loaded from: classes4.dex */
    public static class Configuration {
        private final Context mContext;
        private final int mCujType;
        private final boolean mDeferMonitor;
        private final SurfaceControl mSurfaceControl;
        private final boolean mSurfaceOnly;
        private final String mTag;
        private final long mTimeout;
        private final View mView;

        /* loaded from: classes4.dex */
        public static class Builder {
            private int mAttrCujType;
            private SurfaceControl mAttrSurfaceControl;
            private boolean mAttrSurfaceOnly;
            private View mAttrView = null;
            private Context mAttrContext = null;
            private long mAttrTimeout = InteractionJankMonitor.DEFAULT_TIMEOUT_MS;
            private String mAttrTag = "";
            private boolean mAttrDeferMonitor = true;

            public static Builder withSurface(int cuj, Context context, SurfaceControl surfaceControl) {
                return new Builder(cuj).setContext(context).setSurfaceControl(surfaceControl).setSurfaceOnly(true);
            }

            public static Builder withView(int cuj, View view) {
                return new Builder(cuj).setView(view).setContext(view.getContext());
            }

            private Builder(int cuj) {
                this.mAttrCujType = cuj;
            }

            private Builder setView(View view) {
                this.mAttrView = view;
                return this;
            }

            public Builder setTimeout(long timeout) {
                this.mAttrTimeout = timeout;
                return this;
            }

            public Builder setTag(String tag) {
                this.mAttrTag = tag;
                return this;
            }

            private Builder setSurfaceOnly(boolean surfaceOnly) {
                this.mAttrSurfaceOnly = surfaceOnly;
                return this;
            }

            private Builder setContext(Context context) {
                this.mAttrContext = context;
                return this;
            }

            private Builder setSurfaceControl(SurfaceControl surfaceControl) {
                this.mAttrSurfaceControl = surfaceControl;
                return this;
            }

            public Builder setDeferMonitorForAnimationStart(boolean defer) {
                this.mAttrDeferMonitor = defer;
                return this;
            }

            public Configuration build() throws IllegalArgumentException {
                return new Configuration(this.mAttrCujType, this.mAttrView, this.mAttrTag, this.mAttrTimeout, this.mAttrSurfaceOnly, this.mAttrContext, this.mAttrSurfaceControl, this.mAttrDeferMonitor);
            }
        }

        private Configuration(int cuj, View view, String tag, long timeout, boolean surfaceOnly, Context context, SurfaceControl surfaceControl, boolean deferMonitor) {
            Context applicationContext;
            this.mCujType = cuj;
            this.mTag = tag;
            this.mTimeout = timeout;
            this.mView = view;
            this.mSurfaceOnly = surfaceOnly;
            if (context != null) {
                applicationContext = context;
            } else {
                applicationContext = view != null ? view.getContext().getApplicationContext() : null;
            }
            this.mContext = applicationContext;
            this.mSurfaceControl = surfaceControl;
            this.mDeferMonitor = deferMonitor;
            validate();
        }

        private void validate() {
            boolean shouldThrow = false;
            StringBuilder msg = new StringBuilder();
            if (this.mTag == null) {
                shouldThrow = true;
                msg.append("Invalid tag; ");
            }
            if (this.mTimeout < 0) {
                shouldThrow = true;
                msg.append("Invalid timeout value; ");
            }
            if (this.mSurfaceOnly) {
                if (this.mContext == null) {
                    shouldThrow = true;
                    msg.append("Must pass in a context if only instrument surface; ");
                }
                SurfaceControl surfaceControl = this.mSurfaceControl;
                if (surfaceControl == null || !surfaceControl.isValid()) {
                    shouldThrow = true;
                    msg.append("Must pass in a valid surface control if only instrument surface; ");
                }
            } else {
                View view = this.mView;
                if (view == null || !view.isAttachedToWindow()) {
                    shouldThrow = true;
                    msg.append("Null view or unattached view while instrumenting view; ");
                }
            }
            if (shouldThrow) {
                throw new IllegalArgumentException(msg.toString());
            }
        }

        public boolean isSurfaceOnly() {
            return this.mSurfaceOnly;
        }

        public SurfaceControl getSurfaceControl() {
            return this.mSurfaceControl;
        }

        View getView() {
            return this.mView;
        }

        Context getContext() {
            return this.mContext;
        }

        public boolean shouldDeferMonitor() {
            return this.mDeferMonitor;
        }
    }

    /* loaded from: classes4.dex */
    public static class Session {
        private final int mCujType;
        private final String mName;
        private int mReason = -1;
        private final long mTimeStamp = System.nanoTime();

        public Session(int cujType, String postfix) {
            this.mCujType = cujType;
            this.mName = TextUtils.isEmpty(postfix) ? String.format("J<%s>", InteractionJankMonitor.getNameOfCuj(cujType)) : String.format("J<%s::%s>", InteractionJankMonitor.getNameOfCuj(cujType), postfix);
        }

        public int getCuj() {
            return this.mCujType;
        }

        public int getStatsdInteractionType() {
            return InteractionJankMonitor.CUJ_TO_STATSD_INTERACTION_TYPE[this.mCujType];
        }

        public boolean logToStatsd() {
            return getStatsdInteractionType() != -1;
        }

        public String getPerfettoTrigger() {
            return String.format(Locale.US, "com.android.telemetry.interaction-jank-monitor-%d", Integer.valueOf(this.mCujType));
        }

        public String getName() {
            return this.mName;
        }

        public long getTimeStamp() {
            return this.mTimeStamp;
        }

        public void setReason(int reason) {
            this.mReason = reason;
        }

        public int getReason() {
            return this.mReason;
        }
    }
}
