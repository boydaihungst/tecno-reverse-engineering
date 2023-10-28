package com.android.server.power;

import android.app.ActivityManagerInternal;
import android.app.AppOpsManager;
import android.app.trust.TrustManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManagerInternal;
import android.hardware.input.InputManagerInternal;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.metrics.LogMaker;
import android.net.Uri;
import android.os.BatteryManagerInternal;
import android.os.Handler;
import android.os.IWakeLockCallback;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManagerInternal;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.WorkSource;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.EventLog;
import android.view.WindowManagerPolicyConstants;
import com.android.internal.app.IBatteryStats;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.inputmethod.InputMethodManagerInternal;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.transsion.hubcore.server.wm.ITranDisplayPolicy;
import java.io.PrintWriter;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
/* loaded from: classes2.dex */
public class Notifier {
    private static final int[] CHARGING_VIBRATION_AMPLITUDE;
    private static final VibrationEffect CHARGING_VIBRATION_EFFECT;
    private static final long[] CHARGING_VIBRATION_TIME;
    private static final boolean DEBUG = false;
    private static final int INTERACTIVE_STATE_ASLEEP = 2;
    private static final int INTERACTIVE_STATE_AWAKE = 1;
    private static final int INTERACTIVE_STATE_UNKNOWN = 0;
    private static final int MSG_BROADCAST = 2;
    private static final int MSG_BROADCAST_ENHANCED_PREDICTION = 4;
    private static final int MSG_PROFILE_TIMED_OUT = 5;
    private static final int MSG_SCREEN_POLICY = 7;
    private static final int MSG_USER_ACTIVITY = 1;
    private static final int MSG_WIRED_CHARGING_STARTED = 6;
    private static final int MSG_WIRELESS_CHARGING_STARTED = 3;
    private static final String TAG = "PowerManagerNotifier";
    private static final int[] TRAN_CHARGING_VIBRATION_AMPLITUDE;
    private static final VibrationEffect TRAN_CHARGING_VIBRATION_EFFECT;
    private static final long[] TRAN_CHARGING_VIBRATION_TIME;
    private final AppOpsManager mAppOps;
    private final Executor mBackgroundExecutor;
    private final IBatteryStats mBatteryStats;
    private boolean mBroadcastInProgress;
    private long mBroadcastStartTime;
    private int mBroadcastedInteractiveState;
    private final Context mContext;
    private final FaceDownDetector mFaceDownDetector;
    private final NotifierHandler mHandler;
    private int mInteractiveChangeReason;
    private long mInteractiveChangeStartTime;
    private boolean mInteractiveChanging;
    private boolean mPendingGoToSleepBroadcast;
    private int mPendingInteractiveState;
    private boolean mPendingWakeUpBroadcast;
    private final WindowManagerPolicy mPolicy;
    private final Intent mScreenOffIntent;
    private final Intent mScreenOnIntent;
    private final ScreenUndimDetector mScreenUndimDetector;
    private final boolean mShowWirelessChargingAnimationConfig;
    private final SuspendBlocker mSuspendBlocker;
    private final boolean mSuspendWhenScreenOffDueToProximityConfig;
    private final TrustManager mTrustManager;
    private boolean mUserActivityPending;
    private final Vibrator mVibrator;
    private final WakeLockLog mWakeLockLog;
    private static final boolean TRAN_VIRRATION_TIME_SUPPORT = "1".equals(SystemProperties.get("ro.tran_viration_time_support", "0"));
    private static final VibrationAttributes HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES = VibrationAttributes.createForUsage(50);
    private final Object mLock = new Object();
    private boolean mInteractive = true;
    private final AtomicBoolean mIsPlayingChargingStartedFeedback = new AtomicBoolean(false);
    private final BroadcastReceiver mWakeUpBroadcastDone = new BroadcastReceiver() { // from class: com.android.server.power.Notifier.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            EventLog.writeEvent((int) EventLogTags.POWER_SCREEN_BROADCAST_DONE, 1, Long.valueOf(SystemClock.uptimeMillis() - Notifier.this.mBroadcastStartTime), 1);
            Notifier.this.sendNextBroadcast();
        }
    };
    private final BroadcastReceiver mGoToSleepBroadcastDone = new BroadcastReceiver() { // from class: com.android.server.power.Notifier.3
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            EventLog.writeEvent((int) EventLogTags.POWER_SCREEN_BROADCAST_DONE, 0, Long.valueOf(SystemClock.uptimeMillis() - Notifier.this.mBroadcastStartTime), 1);
            Notifier.this.sendNextBroadcast();
        }
    };
    private final ActivityManagerInternal mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
    private final InputManagerInternal mInputManagerInternal = (InputManagerInternal) LocalServices.getService(InputManagerInternal.class);
    private final InputMethodManagerInternal mInputMethodManagerInternal = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class);
    private final StatusBarManagerInternal mStatusBarManagerInternal = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
    private final DisplayManagerInternal mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
    private BatteryManagerInternal mBatteryManagerInternal = (BatteryManagerInternal) LocalServices.getService(BatteryManagerInternal.class);

    static {
        long[] jArr = {40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40};
        CHARGING_VIBRATION_TIME = jArr;
        int[] iArr = {1, 4, 11, 25, 44, 67, 91, 114, 123, 103, 79, 55, 34, 17, 7, 2};
        CHARGING_VIBRATION_AMPLITUDE = iArr;
        CHARGING_VIBRATION_EFFECT = VibrationEffect.createWaveform(jArr, iArr, -1);
        long[] jArr2 = {40, 40, 40, 40, 40, 40};
        TRAN_CHARGING_VIBRATION_TIME = jArr2;
        int[] iArr2 = {4, 67, 123, 79, 34, 2};
        TRAN_CHARGING_VIBRATION_AMPLITUDE = iArr2;
        TRAN_CHARGING_VIBRATION_EFFECT = VibrationEffect.createWaveform(jArr2, iArr2, -1);
    }

    public Notifier(Looper looper, Context context, IBatteryStats batteryStats, SuspendBlocker suspendBlocker, WindowManagerPolicy policy, FaceDownDetector faceDownDetector, ScreenUndimDetector screenUndimDetector, Executor backgroundExecutor) {
        this.mContext = context;
        this.mBatteryStats = batteryStats;
        this.mAppOps = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        this.mSuspendBlocker = suspendBlocker;
        this.mPolicy = policy;
        this.mFaceDownDetector = faceDownDetector;
        this.mScreenUndimDetector = screenUndimDetector;
        this.mTrustManager = (TrustManager) context.getSystemService(TrustManager.class);
        this.mVibrator = (Vibrator) context.getSystemService(Vibrator.class);
        this.mHandler = new NotifierHandler(looper);
        this.mBackgroundExecutor = backgroundExecutor;
        Intent intent = new Intent("android.intent.action.SCREEN_ON");
        this.mScreenOnIntent = intent;
        intent.addFlags(1344274432);
        Intent intent2 = new Intent("android.intent.action.SCREEN_OFF");
        this.mScreenOffIntent = intent2;
        intent2.addFlags(1344274432);
        this.mSuspendWhenScreenOffDueToProximityConfig = context.getResources().getBoolean(17891789);
        this.mShowWirelessChargingAnimationConfig = context.getResources().getBoolean(17891747);
        this.mWakeLockLog = new WakeLockLog();
        try {
            batteryStats.noteInteractive(true);
        } catch (RemoteException e) {
        }
        FrameworkStatsLog.write(33, 1);
    }

    public void onWakeLockAcquired(int flags, String tag, String packageName, int ownerUid, int ownerPid, WorkSource workSource, String historyTag, IWakeLockCallback callback) {
        boolean z = true;
        notifyWakeLockListener(callback, true);
        int monitorType = getBatteryStatsWakeLockMonitorType(flags);
        if (monitorType >= 0) {
            boolean unimportantForLogging = (ownerUid != 1000 || (1073741824 & flags) == 0) ? false : false;
            try {
                if (workSource != null) {
                    this.mBatteryStats.noteStartWakelockFromSource(workSource, ownerPid, tag, historyTag, monitorType, unimportantForLogging);
                } else {
                    this.mBatteryStats.noteStartWakelock(ownerUid, ownerPid, tag, historyTag, monitorType, unimportantForLogging);
                    try {
                        this.mAppOps.startOpNoThrow(40, ownerUid, packageName);
                    } catch (RemoteException e) {
                    }
                }
            } catch (RemoteException e2) {
            }
        }
        this.mWakeLockLog.onWakeLockAcquired(tag, ownerUid, flags);
    }

    public void onLongPartialWakeLockStart(String tag, int ownerUid, WorkSource workSource, String historyTag) {
        try {
            if (workSource != null) {
                this.mBatteryStats.noteLongPartialWakelockStartFromSource(tag, historyTag, workSource);
                FrameworkStatsLog.write(11, workSource, tag, historyTag, 1);
            } else {
                this.mBatteryStats.noteLongPartialWakelockStart(tag, historyTag, ownerUid);
                FrameworkStatsLog.write_non_chained(11, ownerUid, (String) null, tag, historyTag, 1);
            }
        } catch (RemoteException e) {
        }
    }

    public void onLongPartialWakeLockFinish(String tag, int ownerUid, WorkSource workSource, String historyTag) {
        try {
            if (workSource != null) {
                this.mBatteryStats.noteLongPartialWakelockFinishFromSource(tag, historyTag, workSource);
                FrameworkStatsLog.write(11, workSource, tag, historyTag, 0);
            } else {
                this.mBatteryStats.noteLongPartialWakelockFinish(tag, historyTag, ownerUid);
                FrameworkStatsLog.write_non_chained(11, ownerUid, (String) null, tag, historyTag, 0);
            }
        } catch (RemoteException e) {
        }
    }

    public void onWakeLockChanging(int flags, String tag, String packageName, int ownerUid, int ownerPid, WorkSource workSource, String historyTag, IWakeLockCallback callback, int newFlags, String newTag, String newPackageName, int newOwnerUid, int newOwnerPid, WorkSource newWorkSource, String newHistoryTag, IWakeLockCallback newCallback) {
        int monitorType = getBatteryStatsWakeLockMonitorType(flags);
        int newMonitorType = getBatteryStatsWakeLockMonitorType(newFlags);
        if (workSource != null && newWorkSource != null && monitorType >= 0 && newMonitorType >= 0) {
            boolean unimportantForLogging = newOwnerUid == 1000 && (1073741824 & newFlags) != 0;
            try {
                this.mBatteryStats.noteChangeWakelockFromSource(workSource, ownerPid, tag, historyTag, monitorType, newWorkSource, newOwnerPid, newTag, newHistoryTag, newMonitorType, unimportantForLogging);
            } catch (RemoteException e) {
            }
        } else if (!PowerManagerService.isSameCallback(callback, newCallback)) {
            onWakeLockReleased(flags, tag, packageName, ownerUid, ownerPid, workSource, historyTag, null);
            onWakeLockAcquired(newFlags, newTag, newPackageName, newOwnerUid, newOwnerPid, newWorkSource, newHistoryTag, newCallback);
        } else {
            onWakeLockReleased(flags, tag, packageName, ownerUid, ownerPid, workSource, historyTag, callback);
            onWakeLockAcquired(newFlags, newTag, newPackageName, newOwnerUid, newOwnerPid, newWorkSource, newHistoryTag, newCallback);
        }
    }

    public void onWakeLockReleased(int flags, String tag, String packageName, int ownerUid, int ownerPid, WorkSource workSource, String historyTag, IWakeLockCallback callback) {
        notifyWakeLockListener(callback, false);
        int monitorType = getBatteryStatsWakeLockMonitorType(flags);
        if (monitorType >= 0) {
            try {
                if (workSource != null) {
                    this.mBatteryStats.noteStopWakelockFromSource(workSource, ownerPid, tag, historyTag, monitorType);
                } else {
                    this.mBatteryStats.noteStopWakelock(ownerUid, ownerPid, tag, historyTag, monitorType);
                    this.mAppOps.finishOp(40, ownerUid, packageName);
                }
            } catch (RemoteException e) {
            }
        }
        this.mWakeLockLog.onWakeLockReleased(tag, ownerUid);
    }

    private int getBatteryStatsWakeLockMonitorType(int flags) {
        switch (65535 & flags) {
            case 1:
                return 0;
            case 6:
            case 10:
                return 1;
            case 32:
                return this.mSuspendWhenScreenOffDueToProximityConfig ? -1 : 0;
            case 64:
                return -1;
            case 128:
                return 18;
            default:
                return -1;
        }
    }

    public void onWakefulnessChangeStarted(final int wakefulness, int reason, long eventTime) {
        int i;
        boolean interactive = PowerManagerInternal.isInteractive(wakefulness);
        this.mHandler.post(new Runnable() { // from class: com.android.server.power.Notifier.1
            @Override // java.lang.Runnable
            public void run() {
                Notifier.this.mActivityManagerInternal.onWakefulnessChanged(wakefulness);
            }
        });
        if (this.mInteractive != interactive) {
            if (this.mInteractiveChanging) {
                handleLateInteractiveChange();
            }
            this.mInputManagerInternal.setInteractive(interactive);
            this.mInputMethodManagerInternal.setInteractive(interactive);
            try {
                this.mBatteryStats.noteInteractive(interactive);
            } catch (RemoteException e) {
            }
            if (interactive) {
                i = 1;
            } else {
                i = 0;
            }
            FrameworkStatsLog.write(33, i);
            this.mInteractive = interactive;
            this.mInteractiveChangeReason = reason;
            this.mInteractiveChangeStartTime = eventTime;
            this.mInteractiveChanging = true;
            handleEarlyInteractiveChange();
        }
    }

    public void onWakefulnessChangeFinished() {
        if (this.mInteractiveChanging) {
            this.mInteractiveChanging = false;
            handleLateInteractiveChange();
        }
    }

    private void handleEarlyInteractiveChange() {
        synchronized (this.mLock) {
            if (this.mInteractive) {
                this.mHandler.post(new Runnable() { // from class: com.android.server.power.Notifier$$ExternalSyntheticLambda5
                    @Override // java.lang.Runnable
                    public final void run() {
                        Notifier.this.m6057xb7f2a12a();
                    }
                });
                this.mPendingInteractiveState = 1;
                this.mPendingWakeUpBroadcast = true;
                updatePendingBroadcastLocked();
                ITranDisplayPolicy.Instance().setAwakeState(true);
            } else {
                this.mHandler.post(new Runnable() { // from class: com.android.server.power.Notifier$$ExternalSyntheticLambda6
                    @Override // java.lang.Runnable
                    public final void run() {
                        Notifier.this.m6058x1ecb60eb();
                    }
                });
                ITranDisplayPolicy.Instance().setAwakeState(false);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleEarlyInteractiveChange$0$com-android-server-power-Notifier  reason: not valid java name */
    public /* synthetic */ void m6057xb7f2a12a() {
        this.mPolicy.startedWakingUp(this.mInteractiveChangeReason);
        this.mDisplayManagerInternal.onEarlyInteractivityChange(true);
        this.mPolicy.startedNotifyFaceunlock(this.mInteractiveChangeReason);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleEarlyInteractiveChange$1$com-android-server-power-Notifier  reason: not valid java name */
    public /* synthetic */ void m6058x1ecb60eb() {
        this.mPolicy.startedGoingToSleep(this.mInteractiveChangeReason);
        this.mDisplayManagerInternal.onEarlyInteractivityChange(false);
    }

    private void handleLateInteractiveChange() {
        synchronized (this.mLock) {
            final int interactiveChangeLatency = (int) (SystemClock.uptimeMillis() - this.mInteractiveChangeStartTime);
            if (this.mInteractive) {
                this.mHandler.post(new Runnable() { // from class: com.android.server.power.Notifier$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        Notifier.this.m6059x6938d99f(interactiveChangeLatency);
                    }
                });
            } else {
                if (this.mUserActivityPending) {
                    this.mUserActivityPending = false;
                    this.mHandler.removeMessages(1);
                }
                final int offReason = WindowManagerPolicyConstants.translateSleepReasonToOffReason(this.mInteractiveChangeReason);
                this.mHandler.post(new Runnable() { // from class: com.android.server.power.Notifier$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        Notifier.this.m6060xd0119960(offReason, interactiveChangeLatency);
                    }
                });
                this.mPendingInteractiveState = 2;
                this.mPendingGoToSleepBroadcast = true;
                updatePendingBroadcastLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleLateInteractiveChange$2$com-android-server-power-Notifier  reason: not valid java name */
    public /* synthetic */ void m6059x6938d99f(int interactiveChangeLatency) {
        LogMaker log = new LogMaker((int) FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_USB_DATA_SIGNALING);
        log.setType(1);
        log.setSubtype(WindowManagerPolicyConstants.translateWakeReasonToOnReason(this.mInteractiveChangeReason));
        log.setLatency(interactiveChangeLatency);
        log.addTaggedData(1694, Integer.valueOf(this.mInteractiveChangeReason));
        MetricsLogger.action(log);
        EventLogTags.writePowerScreenState(1, 0, 0L, 0, interactiveChangeLatency);
        this.mPolicy.finishedWakingUp(this.mInteractiveChangeReason);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleLateInteractiveChange$3$com-android-server-power-Notifier  reason: not valid java name */
    public /* synthetic */ void m6060xd0119960(int offReason, int interactiveChangeLatency) {
        LogMaker log = new LogMaker((int) FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_USB_DATA_SIGNALING);
        log.setType(2);
        log.setSubtype(offReason);
        log.setLatency(interactiveChangeLatency);
        log.addTaggedData(1695, Integer.valueOf(this.mInteractiveChangeReason));
        MetricsLogger.action(log);
        EventLogTags.writePowerScreenState(0, offReason, 0L, 0, interactiveChangeLatency);
        this.mPolicy.finishedGoingToSleep(this.mInteractiveChangeReason);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onPowerGroupWakefulnessChanged$4$com-android-server-power-Notifier  reason: not valid java name */
    public /* synthetic */ void m6061x9b8d8294(int groupId, int groupWakefulness, int changeReason, int globalWakefulness) {
        this.mPolicy.onPowerGroupWakefulnessChanged(groupId, groupWakefulness, changeReason, globalWakefulness);
    }

    public void onPowerGroupWakefulnessChanged(final int groupId, final int groupWakefulness, final int changeReason, final int globalWakefulness) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.power.Notifier$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                Notifier.this.m6061x9b8d8294(groupId, groupWakefulness, changeReason, globalWakefulness);
            }
        });
    }

    public void onUserActivity(int displayGroupId, int event, int uid) {
        try {
            this.mBatteryStats.noteUserActivity(uid, event);
        } catch (RemoteException e) {
        }
        synchronized (this.mLock) {
            if (!this.mUserActivityPending) {
                this.mUserActivityPending = true;
                Message msg = this.mHandler.obtainMessage(1);
                msg.arg1 = displayGroupId;
                msg.arg2 = event;
                msg.setAsynchronous(true);
                this.mHandler.sendMessage(msg);
            }
        }
    }

    public void onWakeUp(int reason, String details, int reasonUid, String opPackageName, int opUid) {
        try {
            this.mBatteryStats.noteWakeUp(details, reasonUid);
            if (opPackageName != null) {
                this.mAppOps.noteOpNoThrow(61, opUid, opPackageName);
            }
        } catch (RemoteException e) {
        }
        FrameworkStatsLog.write((int) FrameworkStatsLog.DISPLAY_WAKE_REPORTED, reason);
    }

    public void onProfileTimeout(int userId) {
        Message msg = this.mHandler.obtainMessage(5);
        msg.setAsynchronous(true);
        msg.arg1 = userId;
        this.mHandler.sendMessage(msg);
    }

    public void onWirelessChargingStarted(int batteryLevel, int userId) {
        this.mSuspendBlocker.acquire();
        Message msg = this.mHandler.obtainMessage(3);
        msg.setAsynchronous(true);
        msg.arg1 = batteryLevel;
        msg.arg2 = userId;
        this.mHandler.sendMessage(msg);
    }

    public void onWiredChargingStarted(int userId) {
        this.mSuspendBlocker.acquire();
        Message msg = this.mHandler.obtainMessage(6);
        msg.setAsynchronous(true);
        msg.arg1 = userId;
        this.mHandler.sendMessage(msg);
    }

    public void onScreenPolicyUpdate(int displayGroupId, int newPolicy) {
        synchronized (this.mLock) {
            Message msg = this.mHandler.obtainMessage(7);
            msg.arg1 = displayGroupId;
            msg.arg2 = newPolicy;
            msg.setAsynchronous(true);
            this.mHandler.sendMessage(msg);
        }
    }

    public void dump(PrintWriter pw) {
        WakeLockLog wakeLockLog = this.mWakeLockLog;
        if (wakeLockLog != null) {
            wakeLockLog.dump(pw);
        }
    }

    private void updatePendingBroadcastLocked() {
        int i;
        if (this.mBroadcastInProgress || (i = this.mPendingInteractiveState) == 0) {
            return;
        }
        if (this.mPendingWakeUpBroadcast || this.mPendingGoToSleepBroadcast || i != this.mBroadcastedInteractiveState) {
            this.mBroadcastInProgress = true;
            this.mSuspendBlocker.acquire();
            Message msg = this.mHandler.obtainMessage(2);
            msg.setAsynchronous(true);
            this.mHandler.sendMessage(msg);
        }
    }

    private void finishPendingBroadcastLocked() {
        this.mBroadcastInProgress = false;
        this.mSuspendBlocker.release();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendUserActivity(int displayGroupId, int event) {
        synchronized (this.mLock) {
            if (this.mUserActivityPending) {
                this.mUserActivityPending = false;
                TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService(TelephonyManager.class);
                tm.notifyUserActivity();
                this.mPolicy.userActivity();
                this.mFaceDownDetector.userActivity(event);
                this.mScreenUndimDetector.userActivity(displayGroupId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postEnhancedDischargePredictionBroadcast(long delayMs) {
        this.mHandler.sendEmptyMessageDelayed(4, delayMs);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendEnhancedDischargePredictionBroadcast() {
        Intent intent = new Intent("android.os.action.ENHANCED_DISCHARGE_PREDICTION_CHANGED").addFlags(1073741824);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendNextBroadcast() {
        synchronized (this.mLock) {
            int i = this.mBroadcastedInteractiveState;
            if (i == 0) {
                switch (this.mPendingInteractiveState) {
                    case 2:
                        this.mPendingGoToSleepBroadcast = false;
                        this.mBroadcastedInteractiveState = 2;
                        break;
                    default:
                        this.mPendingWakeUpBroadcast = false;
                        this.mBroadcastedInteractiveState = 1;
                        break;
                }
            } else if (i == 1) {
                if (!this.mPendingWakeUpBroadcast && !this.mPendingGoToSleepBroadcast && this.mPendingInteractiveState != 2) {
                    finishPendingBroadcastLocked();
                    return;
                }
                this.mPendingGoToSleepBroadcast = false;
                this.mBroadcastedInteractiveState = 2;
            } else {
                if (!this.mPendingWakeUpBroadcast && !this.mPendingGoToSleepBroadcast && this.mPendingInteractiveState != 1) {
                    finishPendingBroadcastLocked();
                    return;
                }
                this.mPendingWakeUpBroadcast = false;
                this.mBroadcastedInteractiveState = 1;
            }
            this.mBroadcastStartTime = SystemClock.uptimeMillis();
            int powerState = this.mBroadcastedInteractiveState;
            EventLog.writeEvent((int) EventLogTags.POWER_SCREEN_BROADCAST_SEND, 1);
            if (powerState == 1) {
                sendWakeUpBroadcast();
            } else {
                sendGoToSleepBroadcast();
            }
        }
    }

    private void sendWakeUpBroadcast() {
        if (this.mActivityManagerInternal.isSystemReady()) {
            this.mContext.sendOrderedBroadcastAsUser(this.mScreenOnIntent, UserHandle.ALL, null, this.mWakeUpBroadcastDone, this.mHandler, 0, null, null);
            return;
        }
        EventLog.writeEvent((int) EventLogTags.POWER_SCREEN_BROADCAST_STOP, 2, 1);
        sendNextBroadcast();
    }

    private void sendGoToSleepBroadcast() {
        if (this.mActivityManagerInternal.isSystemReady()) {
            this.mContext.sendOrderedBroadcastAsUser(this.mScreenOffIntent, UserHandle.ALL, null, this.mGoToSleepBroadcastDone, this.mHandler, 0, null, null);
            return;
        }
        EventLog.writeEvent((int) EventLogTags.POWER_SCREEN_BROADCAST_STOP, 3, 1);
        sendNextBroadcast();
    }

    private void playChargingStartedFeedback(final int userId, final boolean wireless) {
        if (!isChargingFeedbackEnabled(userId) || !this.mIsPlayingChargingStartedFeedback.compareAndSet(false, true)) {
            return;
        }
        this.mBackgroundExecutor.execute(new Runnable() { // from class: com.android.server.power.Notifier$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                Notifier.this.m6062xc600bf1f(userId, wireless);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$playChargingStartedFeedback$5$com-android-server-power-Notifier  reason: not valid java name */
    public /* synthetic */ void m6062xc600bf1f(int userId, boolean wireless) {
        Ringtone sfx;
        boolean vibrate = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "charging_vibration_enabled", 1, userId) != 0;
        int batteryLevel = 0;
        BatteryManagerInternal batteryManagerInternal = this.mBatteryManagerInternal;
        if (batteryManagerInternal != null) {
            batteryLevel = batteryManagerInternal.getBatteryLevel();
        }
        if (vibrate && batteryLevel != 100) {
            this.mVibrator.vibrate(TRAN_VIRRATION_TIME_SUPPORT ? TRAN_CHARGING_VIBRATION_EFFECT : CHARGING_VIBRATION_EFFECT, HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES);
        }
        String soundPath = Settings.Global.getString(this.mContext.getContentResolver(), wireless ? "wireless_charging_started_sound" : "charging_started_sound");
        Uri soundUri = Uri.parse("file://" + soundPath);
        if (soundUri != null && (sfx = RingtoneManager.getRingtone(this.mContext, soundUri)) != null) {
            sfx.setStreamType(5);
            sfx.play();
        }
        this.mIsPlayingChargingStartedFeedback.set(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showWirelessChargingStarted(int batteryLevel, int userId) {
        StatusBarManagerInternal statusBarManagerInternal;
        playChargingStartedFeedback(userId, true);
        if (this.mShowWirelessChargingAnimationConfig && (statusBarManagerInternal = this.mStatusBarManagerInternal) != null) {
            statusBarManagerInternal.showChargingAnimation(batteryLevel);
        }
        this.mSuspendBlocker.release();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showWiredChargingStarted(int userId) {
        playChargingStartedFeedback(userId, false);
        this.mSuspendBlocker.release();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void screenPolicyChanging(int displayGroupId, int screenPolicy) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void lockProfile(int userId) {
        this.mTrustManager.setDeviceLockedForUser(userId, true);
    }

    private boolean isChargingFeedbackEnabled(int userId) {
        boolean enabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "charging_sounds_enabled", 1, userId) != 0;
        boolean dndOff = Settings.Global.getInt(this.mContext.getContentResolver(), "zen_mode", 1) == 0;
        return enabled && dndOff;
    }

    private void notifyWakeLockListener(final IWakeLockCallback callback, final boolean isEnabled) {
        if (callback != null) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.power.Notifier$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    Notifier.lambda$notifyWakeLockListener$6(callback, isEnabled);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$notifyWakeLockListener$6(IWakeLockCallback callback, boolean isEnabled) {
        try {
            callback.onStateChanged(isEnabled);
        } catch (RemoteException e) {
            throw new IllegalArgumentException("Wakelock.mCallback is already dead.", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class NotifierHandler extends Handler {
        public NotifierHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Notifier.this.sendUserActivity(msg.arg1, msg.arg2);
                    return;
                case 2:
                    Notifier.this.sendNextBroadcast();
                    return;
                case 3:
                    Notifier.this.showWirelessChargingStarted(msg.arg1, msg.arg2);
                    return;
                case 4:
                    removeMessages(4);
                    Notifier.this.sendEnhancedDischargePredictionBroadcast();
                    return;
                case 5:
                    Notifier.this.lockProfile(msg.arg1);
                    return;
                case 6:
                    Notifier.this.showWiredChargingStarted(msg.arg1);
                    return;
                case 7:
                    Notifier.this.screenPolicyChanging(msg.arg1, msg.arg2);
                    return;
                default:
                    return;
            }
        }
    }
}
