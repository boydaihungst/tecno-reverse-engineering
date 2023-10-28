package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.os.Build;
import android.os.IBinder;
import android.os.Process;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseArray;
import android.view.InputApplicationHandle;
import com.android.server.am.ActivityManagerService;
import com.android.server.criticalevents.CriticalEventLog;
import com.android.server.pm.PackageManagerService;
import com.mediatek.server.wm.WmsExt;
import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
/* loaded from: classes2.dex */
public class AnrController {
    private static final long PRE_DUMP_MIN_INTERVAL_MS = TimeUnit.SECONDS.toMillis(20);
    private static final long PRE_DUMP_MONITOR_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(1);
    private volatile long mLastPreDumpTimeMs;
    private final WindowManagerService mService;
    private final SparseArray<ActivityRecord> mUnresponsiveAppByDisplay = new SparseArray<>();
    private boolean Debug_root = "1".equals(SystemProperties.get("persist.sys.adb.support", "0"));

    /* JADX INFO: Access modifiers changed from: package-private */
    public AnrController(WindowManagerService service) {
        this.mService = service;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyAppUnresponsive(InputApplicationHandle applicationHandle, String reason) {
        if ("Application does not have a focused window".equals(reason) && PackageManagerService.isAppopsState()) {
            ActivityRecord activity = ActivityRecord.forTokenLocked(applicationHandle.token);
            if (activity == null) {
                Slog.e(WmsExt.TAG, "Unknown app appToken:" + applicationHandle.name + ". Dropping notifyNoFocusedWindowAnr request");
                return;
            }
            int pid = activity.getPid();
            Process.killProcess(pid);
            Slog.d(WmsExt.TAG, "killProcess pid = " + pid + " because Application does not have a focused window");
            return;
        }
        try {
            if (this.Debug_root && "Application does not have a focused window".equals(reason) && ActivityManager.getService() != null) {
                Slog.d(WmsExt.TAG, "start TNE dump surfaceflinger and window");
                ActivityManager.getService().startTNE("0x007a0040", 1073741824L, 0, "");
            }
        } catch (Exception e) {
            Slog.d(WmsExt.TAG, "Can't call IActivityManager");
        }
        preDumpIfLockTooSlow();
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord activity2 = ActivityRecord.forTokenLocked(applicationHandle.token);
                if (activity2 == null) {
                    Slog.e(WmsExt.TAG, "Unknown app appToken:" + applicationHandle.name + ". Dropping notifyNoFocusedWindowAnr request");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                Slog.i(WmsExt.TAG, "ANR in " + activity2.getName() + ".  Reason: " + reason);
                dumpAnrStateLocked(activity2, null, reason);
                this.mUnresponsiveAppByDisplay.put(activity2.getDisplayId(), activity2);
                WindowManagerService.resetPriorityAfterLockedSection();
                activity2.inputDispatchingTimedOut(reason, -1);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyWindowUnresponsive(IBinder token, OptionalInt pid, String reason) {
        if (notifyWindowUnresponsive(token, reason)) {
            return;
        }
        if (!pid.isPresent()) {
            Slog.w(WmsExt.TAG, "Failed to notify that window token=" + token + " was unresponsive.");
        } else {
            notifyWindowUnresponsive(pid.getAsInt(), reason);
        }
    }

    private boolean notifyWindowUnresponsive(IBinder inputToken, String reason) {
        preDumpIfLockTooSlow();
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                InputTarget target = this.mService.getInputTargetFromToken(inputToken);
                if (target == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                WindowState windowState = target.getWindowState();
                int pid = target.getPid();
                ActivityRecord activity = windowState.mInputChannelToken == inputToken ? windowState.mActivityRecord : null;
                Slog.i(WmsExt.TAG, "ANR in " + target + ". Reason:" + reason);
                boolean aboveSystem = isWindowAboveSystem(windowState);
                dumpAnrStateLocked(activity, windowState, reason);
                WindowManagerService.resetPriorityAfterLockedSection();
                if (activity != null) {
                    activity.inputDispatchingTimedOut(reason, pid);
                    return true;
                }
                this.mService.mAmInternal.inputDispatchingTimedOut(pid, aboveSystem, reason);
                return true;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private void notifyWindowUnresponsive(int pid, String reason) {
        Slog.i(WmsExt.TAG, "ANR in input window owned by pid=" + pid + ". Reason: " + reason);
        dumpAnrStateLocked(null, null, reason);
        this.mService.mAmInternal.inputDispatchingTimedOut(pid, true, reason);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyWindowResponsive(IBinder token, OptionalInt pid) {
        if (notifyWindowResponsive(token)) {
            return;
        }
        if (!pid.isPresent()) {
            Slog.w(WmsExt.TAG, "Failed to notify that window token=" + token + " was responsive.");
        } else {
            notifyWindowResponsive(pid.getAsInt());
        }
    }

    private boolean notifyWindowResponsive(IBinder inputToken) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                InputTarget target = this.mService.getInputTargetFromToken(inputToken);
                if (target == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                int pid = target.getPid();
                WindowManagerService.resetPriorityAfterLockedSection();
                this.mService.mAmInternal.inputDispatchingResumed(pid);
                return true;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private void notifyWindowResponsive(int pid) {
        this.mService.mAmInternal.inputDispatchingResumed(pid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onFocusChanged(WindowState newFocus) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord unresponsiveApp = this.mUnresponsiveAppByDisplay.get(newFocus.getDisplayId());
                if (unresponsiveApp != null && unresponsiveApp == newFocus.mActivityRecord) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    this.mService.mAmInternal.inputDispatchingResumed(unresponsiveApp.getPid());
                    return;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX WARN: Type inference failed for: r18v0, types: [com.android.server.wm.AnrController$1] */
    private void preDumpIfLockTooSlow() {
        int[] iArr;
        if (!Build.IS_DEBUGGABLE) {
            return;
        }
        final long now = SystemClock.uptimeMillis();
        if (this.mLastPreDumpTimeMs > 0 && now - this.mLastPreDumpTimeMs < PRE_DUMP_MIN_INTERVAL_MS) {
            return;
        }
        final boolean[] shouldDumpSf = {true};
        ArrayMap<String, Runnable> monitors = new ArrayMap<>(2);
        final WindowManagerService windowManagerService = this.mService;
        Objects.requireNonNull(windowManagerService);
        Runnable runnable = new Runnable() { // from class: com.android.server.wm.AnrController$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                WindowManagerService.this.monitor();
            }
        };
        String str = WmsExt.TAG;
        monitors.put(WmsExt.TAG, runnable);
        final ActivityManagerInternal activityManagerInternal = this.mService.mAmInternal;
        Objects.requireNonNull(activityManagerInternal);
        monitors.put("ActivityManager", new Runnable() { // from class: com.android.server.wm.AnrController$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                activityManagerInternal.monitor();
            }
        });
        CountDownLatch latch = new CountDownLatch(monitors.size());
        int i = 0;
        while (i < monitors.size()) {
            final String name = monitors.keyAt(i);
            final Runnable monitor = monitors.valueAt(i);
            final CountDownLatch countDownLatch = latch;
            new Thread() { // from class: com.android.server.wm.AnrController.1
                @Override // java.lang.Thread, java.lang.Runnable
                public void run() {
                    monitor.run();
                    countDownLatch.countDown();
                    long elapsed = SystemClock.uptimeMillis() - now;
                    if (elapsed > AnrController.PRE_DUMP_MONITOR_TIMEOUT_MS) {
                        Slog.i(WmsExt.TAG, "Pre-dump acquired " + name + " in " + elapsed + "ms");
                    } else if (WmsExt.TAG.equals(name)) {
                        shouldDumpSf[0] = false;
                    }
                }
            }.start();
            i++;
            str = str;
            latch = latch;
        }
        String str2 = str;
        try {
            if (latch.await(PRE_DUMP_MONITOR_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                return;
            }
        } catch (InterruptedException e) {
        }
        this.mLastPreDumpTimeMs = now;
        Slog.i(str2, "Pre-dump for unresponsive");
        ArrayList<Integer> firstPids = new ArrayList<>(1);
        firstPids.add(Integer.valueOf(ActivityManagerService.MY_PID));
        ArrayList<Integer> nativePids = null;
        if (shouldDumpSf[0]) {
            iArr = Process.getPidsForCommands(new String[]{"/system/bin/surfaceflinger"});
        } else {
            iArr = null;
        }
        int[] pids = iArr;
        if (pids != null) {
            nativePids = new ArrayList<>(1);
            for (int pid : pids) {
                nativePids.add(Integer.valueOf(pid));
            }
        }
        String criticalEvents = CriticalEventLog.getInstance().logLinesForSystemServerTraceFile();
        File tracesFile = ActivityManagerService.dumpStackTraces(firstPids, null, null, nativePids, null, "Pre-dump", criticalEvents);
        if (tracesFile != null) {
            tracesFile.renameTo(new File(tracesFile.getParent(), tracesFile.getName() + "_pre"));
        }
    }

    private void dumpAnrStateLocked(ActivityRecord activity, WindowState windowState, String reason) {
        this.mService.saveANRStateLocked(activity, windowState, reason);
        this.mService.mAtmService.saveANRState(reason);
    }

    private boolean isWindowAboveSystem(WindowState windowState) {
        int systemAlertLayer = this.mService.mPolicy.getWindowLayerFromTypeLw(2038, windowState.mOwnerCanAddInternalSystemWindow);
        return windowState.mBaseLayer > systemAlertLayer;
    }
}
