package com.android.server.power;

import android.app.ActivityManager;
import android.app.SynchronousUserSwitchObserver;
import android.attention.AttentionManagerInternal;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.util.Slog;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.LocalServices;
import com.android.server.job.controllers.JobStatus;
import com.android.server.wm.WindowManagerInternal;
import java.io.PrintWriter;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
/* loaded from: classes2.dex */
public class AttentionDetector {
    private static final boolean DEBUG = false;
    static final long DEFAULT_POST_DIM_CHECK_DURATION_MILLIS = 0;
    static final long DEFAULT_PRE_DIM_CHECK_DURATION_MILLIS = 2000;
    static final String KEY_MAX_EXTENSION_MILLIS = "max_extension_millis";
    static final String KEY_POST_DIM_CHECK_DURATION_MILLIS = "post_dim_check_duration_millis";
    static final String KEY_PRE_DIM_CHECK_DURATION_MILLIS = "pre_dim_check_duration_millis";
    private static final String TAG = "AttentionDetector";
    protected AttentionManagerInternal mAttentionManager;
    AttentionCallbackInternalImpl mCallback;
    protected ContentResolver mContentResolver;
    private Context mContext;
    protected long mDefaultMaximumExtensionMillis;
    private long mEffectivePostDimTimeoutMillis;
    private boolean mIsSettingEnabled;
    private long mLastActedOnNextScreenDimming;
    private long mLastUserActivityTime;
    private final Object mLock;
    private long mMaximumExtensionMillis;
    private final Runnable mOnUserAttention;
    protected long mPreDimCheckDurationMillis;
    private long mRequestedPostDimTimeoutMillis;
    protected WindowManagerInternal mWindowManager;
    private AtomicLong mConsecutiveTimeoutExtendedCount = new AtomicLong(0);
    private final AtomicBoolean mRequested = new AtomicBoolean(false);
    protected int mRequestId = 0;
    private int mWakefulness = 1;

    public AttentionDetector(Runnable onUserAttention, Object lock) {
        this.mOnUserAttention = onUserAttention;
        this.mLock = lock;
    }

    void updateEnabledFromSettings(Context context) {
        this.mIsSettingEnabled = Settings.Secure.getIntForUser(context.getContentResolver(), "adaptive_sleep", 0, -2) == 1;
    }

    public void systemReady(final Context context) {
        this.mContext = context;
        updateEnabledFromSettings(context);
        this.mContentResolver = context.getContentResolver();
        this.mAttentionManager = (AttentionManagerInternal) LocalServices.getService(AttentionManagerInternal.class);
        this.mWindowManager = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        this.mDefaultMaximumExtensionMillis = context.getResources().getInteger(17694735);
        try {
            UserSwitchObserver observer = new UserSwitchObserver();
            ActivityManager.getService().registerUserSwitchObserver(observer, TAG);
        } catch (RemoteException e) {
        }
        context.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("adaptive_sleep"), false, new ContentObserver(new Handler(context.getMainLooper())) { // from class: com.android.server.power.AttentionDetector.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                AttentionDetector.this.updateEnabledFromSettings(context);
            }
        }, -1);
        readValuesFromDeviceConfig();
        DeviceConfig.addOnPropertiesChangedListener("attention_manager_service", context.getMainExecutor(), new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.power.AttentionDetector$$ExternalSyntheticLambda0
            public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                AttentionDetector.this.m6031lambda$systemReady$0$comandroidserverpowerAttentionDetector(properties);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$systemReady$0$com-android-server-power-AttentionDetector  reason: not valid java name */
    public /* synthetic */ void m6031lambda$systemReady$0$comandroidserverpowerAttentionDetector(DeviceConfig.Properties properties) {
        onDeviceConfigChange(properties.getKeyset());
    }

    public long updateUserActivity(long nextScreenDimming, long dimDurationMillis) {
        if (nextScreenDimming == this.mLastActedOnNextScreenDimming || !this.mIsSettingEnabled || !isAttentionServiceSupported() || this.mWindowManager.isKeyguardShowingAndNotOccluded()) {
            return nextScreenDimming;
        }
        long now = SystemClock.uptimeMillis();
        long whenToCheck = nextScreenDimming - this.mPreDimCheckDurationMillis;
        long whenToStopExtending = this.mLastUserActivityTime + this.mMaximumExtensionMillis;
        if (now < whenToCheck) {
            return whenToCheck;
        }
        if (whenToStopExtending < whenToCheck) {
            return nextScreenDimming;
        }
        if (this.mRequested.get()) {
            return whenToCheck;
        }
        this.mRequested.set(true);
        this.mRequestId++;
        this.mLastActedOnNextScreenDimming = nextScreenDimming;
        this.mCallback = new AttentionCallbackInternalImpl(this.mRequestId);
        this.mEffectivePostDimTimeoutMillis = Math.min(this.mRequestedPostDimTimeoutMillis, dimDurationMillis);
        Slog.v(TAG, "Checking user attention, ID: " + this.mRequestId);
        boolean sent = this.mAttentionManager.checkAttention(this.mPreDimCheckDurationMillis + this.mEffectivePostDimTimeoutMillis, this.mCallback);
        if (!sent) {
            this.mRequested.set(false);
        }
        return whenToCheck;
    }

    public int onUserActivity(long eventTime, int event) {
        switch (event) {
            case 0:
            case 1:
            case 2:
            case 3:
                cancelCurrentRequestIfAny();
                this.mLastUserActivityTime = eventTime;
                resetConsecutiveExtensionCount();
                return 1;
            case 4:
                this.mConsecutiveTimeoutExtendedCount.incrementAndGet();
                return 0;
            default:
                return -1;
        }
    }

    public void onWakefulnessChangeStarted(int wakefulness) {
        this.mWakefulness = wakefulness;
        if (wakefulness != 1) {
            cancelCurrentRequestIfAny();
            resetConsecutiveExtensionCount();
        }
    }

    private void cancelCurrentRequestIfAny() {
        if (this.mRequested.get()) {
            this.mAttentionManager.cancelAttentionCheck(this.mCallback);
            this.mRequested.set(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetConsecutiveExtensionCount() {
        long previousCount = this.mConsecutiveTimeoutExtendedCount.getAndSet(0L);
        if (previousCount > 0) {
            FrameworkStatsLog.write(168, previousCount);
        }
    }

    boolean isAttentionServiceSupported() {
        AttentionManagerInternal attentionManagerInternal = this.mAttentionManager;
        return attentionManagerInternal != null && attentionManagerInternal.isAttentionServiceSupported();
    }

    public void dump(PrintWriter pw) {
        pw.println("AttentionDetector:");
        pw.println(" mIsSettingEnabled=" + this.mIsSettingEnabled);
        pw.println(" mMaxExtensionMillis=" + this.mMaximumExtensionMillis);
        pw.println(" mPreDimCheckDurationMillis=" + this.mPreDimCheckDurationMillis);
        pw.println(" mEffectivePostDimTimeout=" + this.mEffectivePostDimTimeoutMillis);
        pw.println(" mLastUserActivityTime(excludingAttention)=" + this.mLastUserActivityTime);
        pw.println(" mAttentionServiceSupported=" + isAttentionServiceSupported());
        pw.println(" mRequested=" + this.mRequested);
    }

    protected long getPreDimCheckDurationMillis() {
        long millis = DeviceConfig.getLong("attention_manager_service", KEY_PRE_DIM_CHECK_DURATION_MILLIS, (long) DEFAULT_PRE_DIM_CHECK_DURATION_MILLIS);
        if (millis < 0 || millis > 13000) {
            Slog.w(TAG, "Bad flag value supplied for: pre_dim_check_duration_millis");
            return DEFAULT_PRE_DIM_CHECK_DURATION_MILLIS;
        }
        return millis;
    }

    protected long getPostDimCheckDurationMillis() {
        long millis = DeviceConfig.getLong("attention_manager_service", KEY_POST_DIM_CHECK_DURATION_MILLIS, 0L);
        if (millis < 0 || millis > JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY) {
            Slog.w(TAG, "Bad flag value supplied for: post_dim_check_duration_millis");
            return 0L;
        }
        return millis;
    }

    protected long getMaxExtensionMillis() {
        long millis = DeviceConfig.getLong("attention_manager_service", KEY_MAX_EXTENSION_MILLIS, this.mDefaultMaximumExtensionMillis);
        if (millis < 0 || millis > 3600000) {
            Slog.w(TAG, "Bad flag value supplied for: max_extension_millis");
            return this.mDefaultMaximumExtensionMillis;
        }
        return millis;
    }

    private void onDeviceConfigChange(Set<String> keys) {
        for (String key : keys) {
            char c = 65535;
            switch (key.hashCode()) {
                case -2018189628:
                    if (key.equals(KEY_POST_DIM_CHECK_DURATION_MILLIS)) {
                        c = 1;
                        break;
                    }
                    break;
                case -511526975:
                    if (key.equals(KEY_MAX_EXTENSION_MILLIS)) {
                        c = 0;
                        break;
                    }
                    break;
                case 417901319:
                    if (key.equals(KEY_PRE_DIM_CHECK_DURATION_MILLIS)) {
                        c = 2;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                case 1:
                case 2:
                    readValuesFromDeviceConfig();
                    return;
                default:
                    Slog.i(TAG, "Ignoring change on " + key);
            }
        }
    }

    private void readValuesFromDeviceConfig() {
        this.mMaximumExtensionMillis = getMaxExtensionMillis();
        this.mPreDimCheckDurationMillis = getPreDimCheckDurationMillis();
        this.mRequestedPostDimTimeoutMillis = getPostDimCheckDurationMillis();
        Slog.i(TAG, "readValuesFromDeviceConfig():\nmMaximumExtensionMillis=" + this.mMaximumExtensionMillis + "\nmPreDimCheckDurationMillis=" + this.mPreDimCheckDurationMillis + "\nmRequestedPostDimTimeoutMillis=" + this.mRequestedPostDimTimeoutMillis);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class AttentionCallbackInternalImpl extends AttentionManagerInternal.AttentionCallbackInternal {
        private final int mId;

        AttentionCallbackInternalImpl(int id) {
            this.mId = id;
        }

        public void onSuccess(int result, long timestamp) {
            Slog.v(AttentionDetector.TAG, "onSuccess: " + result + ", ID: " + this.mId);
            if (this.mId == AttentionDetector.this.mRequestId && AttentionDetector.this.mRequested.getAndSet(false)) {
                synchronized (AttentionDetector.this.mLock) {
                    if (AttentionDetector.this.mWakefulness != 1) {
                        return;
                    }
                    if (result == 1) {
                        AttentionDetector.this.mOnUserAttention.run();
                    } else {
                        AttentionDetector.this.resetConsecutiveExtensionCount();
                    }
                }
            }
        }

        public void onFailure(int error) {
            Slog.i(AttentionDetector.TAG, "Failed to check attention: " + error + ", ID: " + this.mId);
            AttentionDetector.this.mRequested.set(false);
        }
    }

    /* loaded from: classes2.dex */
    private final class UserSwitchObserver extends SynchronousUserSwitchObserver {
        private UserSwitchObserver() {
        }

        public void onUserSwitching(int newUserId) throws RemoteException {
            AttentionDetector attentionDetector = AttentionDetector.this;
            attentionDetector.updateEnabledFromSettings(attentionDetector.mContext);
        }
    }
}
