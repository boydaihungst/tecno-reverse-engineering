package com.android.server.job.controllers.idle;

import android.app.AlarmManager;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.util.Log;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.server.am.ActivityManagerService;
import com.android.server.job.JobSchedulerService;
import java.io.PrintWriter;
import java.util.Set;
/* loaded from: classes.dex */
public final class DeviceIdlenessTracker extends BroadcastReceiver implements IdlenessTracker {
    private static final boolean DEBUG;
    private static final String TAG = "JobScheduler.DeviceIdlenessTracker";
    private AlarmManager mAlarm;
    private boolean mDockIdle;
    private boolean mIdle;
    private IdlenessListener mIdleListener;
    private long mIdleWindowSlop;
    private long mInactivityIdleThreshold;
    private PowerManager mPowerManager;
    private boolean mProjectionActive;
    private final UiModeManager.OnProjectionStateChangedListener mOnProjectionStateChangedListener = new UiModeManager.OnProjectionStateChangedListener() { // from class: com.android.server.job.controllers.idle.DeviceIdlenessTracker$$ExternalSyntheticLambda0
        public final void onProjectionStateChanged(int i, Set set) {
            DeviceIdlenessTracker.this.onProjectionStateChanged(i, set);
        }
    };
    private AlarmManager.OnAlarmListener mIdleAlarmListener = new AlarmManager.OnAlarmListener() { // from class: com.android.server.job.controllers.idle.DeviceIdlenessTracker$$ExternalSyntheticLambda1
        @Override // android.app.AlarmManager.OnAlarmListener
        public final void onAlarm() {
            DeviceIdlenessTracker.this.m4255x9b03b20f();
        }
    };
    private boolean mScreenOn = true;

    static {
        DEBUG = JobSchedulerService.DEBUG || Log.isLoggable(TAG, 3);
    }

    @Override // com.android.server.job.controllers.idle.IdlenessTracker
    public boolean isIdle() {
        return this.mIdle;
    }

    @Override // com.android.server.job.controllers.idle.IdlenessTracker
    public void startTracking(Context context, IdlenessListener listener) {
        this.mIdleListener = listener;
        this.mInactivityIdleThreshold = context.getResources().getInteger(17694843);
        this.mIdleWindowSlop = context.getResources().getInteger(17694842);
        this.mAlarm = (AlarmManager) context.getSystemService("alarm");
        this.mPowerManager = (PowerManager) context.getSystemService(PowerManager.class);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.DREAMING_STARTED");
        filter.addAction("android.intent.action.DREAMING_STOPPED");
        filter.addAction(ActivityManagerService.ACTION_TRIGGER_IDLE);
        filter.addAction("android.intent.action.DOCK_IDLE");
        filter.addAction("android.intent.action.DOCK_ACTIVE");
        context.registerReceiver(this, filter);
        ((UiModeManager) context.getSystemService(UiModeManager.class)).addOnProjectionStateChangedListener(-1, context.getMainExecutor(), this.mOnProjectionStateChangedListener);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onProjectionStateChanged(int activeProjectionTypes, Set<String> projectingPackages) {
        boolean projectionActive = activeProjectionTypes != 0;
        if (this.mProjectionActive == projectionActive) {
            return;
        }
        if (DEBUG) {
            Slog.v(TAG, "Projection state changed: " + projectionActive);
        }
        this.mProjectionActive = projectionActive;
        if (projectionActive) {
            cancelIdlenessCheck();
            if (this.mIdle) {
                this.mIdle = false;
                this.mIdleListener.reportNewIdleState(false);
                return;
            }
            return;
        }
        maybeScheduleIdlenessCheck("Projection ended");
    }

    @Override // com.android.server.job.controllers.idle.IdlenessTracker
    public void dump(PrintWriter pw) {
        pw.print("  mIdle: ");
        pw.println(this.mIdle);
        pw.print("  mScreenOn: ");
        pw.println(this.mScreenOn);
        pw.print("  mDockIdle: ");
        pw.println(this.mDockIdle);
        pw.print("  mProjectionActive: ");
        pw.println(this.mProjectionActive);
    }

    @Override // com.android.server.job.controllers.idle.IdlenessTracker
    public void dump(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        long diToken = proto.start(1146756268033L);
        proto.write(1133871366145L, this.mIdle);
        proto.write(1133871366146L, this.mScreenOn);
        proto.write(1133871366147L, this.mDockIdle);
        proto.write(1133871366149L, this.mProjectionActive);
        proto.end(diToken);
        proto.end(token);
    }

    /* JADX WARN: Removed duplicated region for block: B:45:0x009b A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:48:0x00a2  */
    /* JADX WARN: Removed duplicated region for block: B:51:0x00ae  */
    /* JADX WARN: Removed duplicated region for block: B:55:? A[RETURN, SYNTHETIC] */
    @Override // android.content.BroadcastReceiver
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        boolean z = DEBUG;
        if (z) {
            Slog.v(TAG, "Received action: " + action);
        }
        char c = 65535;
        switch (action.hashCode()) {
            case -2128145023:
                if (action.equals("android.intent.action.SCREEN_OFF")) {
                    c = 3;
                    break;
                }
                break;
            case -1454123155:
                if (action.equals("android.intent.action.SCREEN_ON")) {
                    c = 2;
                    break;
                }
                break;
            case -905264325:
                if (action.equals("android.intent.action.DOCK_IDLE")) {
                    c = 5;
                    break;
                }
                break;
            case 244891622:
                if (action.equals("android.intent.action.DREAMING_STARTED")) {
                    c = 4;
                    break;
                }
                break;
            case 257757490:
                if (action.equals("android.intent.action.DREAMING_STOPPED")) {
                    c = 1;
                    break;
                }
                break;
            case 1456569541:
                if (action.equals(ActivityManagerService.ACTION_TRIGGER_IDLE)) {
                    c = 6;
                    break;
                }
                break;
            case 1689632941:
                if (action.equals("android.intent.action.DOCK_ACTIVE")) {
                    c = 0;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                if (!this.mScreenOn) {
                    return;
                }
                if (!this.mPowerManager.isInteractive()) {
                    return;
                }
                this.mScreenOn = true;
                this.mDockIdle = false;
                if (z) {
                    Slog.v(TAG, "exiting idle");
                }
                cancelIdlenessCheck();
                if (this.mIdle) {
                    this.mIdle = false;
                    this.mIdleListener.reportNewIdleState(false);
                    return;
                }
                return;
            case 1:
                if (!this.mPowerManager.isInteractive()) {
                }
                this.mScreenOn = true;
                this.mDockIdle = false;
                if (z) {
                }
                cancelIdlenessCheck();
                if (this.mIdle) {
                }
                break;
            case 2:
                this.mScreenOn = true;
                this.mDockIdle = false;
                if (z) {
                }
                cancelIdlenessCheck();
                if (this.mIdle) {
                }
                break;
            case 3:
            case 4:
            case 5:
                if (action.equals("android.intent.action.DOCK_IDLE")) {
                    if (!this.mScreenOn) {
                        return;
                    }
                    this.mDockIdle = true;
                } else {
                    this.mScreenOn = false;
                    this.mDockIdle = false;
                }
                maybeScheduleIdlenessCheck(action);
                return;
            case 6:
                m4255x9b03b20f();
                return;
            default:
                return;
        }
    }

    private void maybeScheduleIdlenessCheck(String reason) {
        if ((!this.mScreenOn || this.mDockIdle) && !this.mProjectionActive) {
            long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
            long when = this.mInactivityIdleThreshold + nowElapsed;
            if (DEBUG) {
                Slog.v(TAG, "Scheduling idle : " + reason + " now:" + nowElapsed + " when=" + when);
            }
            this.mAlarm.setWindow(2, when, this.mIdleWindowSlop, "JS idleness", this.mIdleAlarmListener, null);
        }
    }

    private void cancelIdlenessCheck() {
        this.mAlarm.cancel(this.mIdleAlarmListener);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: handleIdleTrigger */
    public void m4255x9b03b20f() {
        if (!this.mIdle && ((!this.mScreenOn || this.mDockIdle) && !this.mProjectionActive)) {
            if (DEBUG) {
                Slog.v(TAG, "Idle trigger fired @ " + JobSchedulerService.sElapsedRealtimeClock.millis());
            }
            this.mIdle = true;
            this.mIdleListener.reportNewIdleState(true);
        } else if (DEBUG) {
            Slog.v(TAG, "TRIGGER_IDLE received but not changing state; idle=" + this.mIdle + " screen=" + this.mScreenOn + " projection=" + this.mProjectionActive);
        }
    }
}
