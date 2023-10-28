package com.android.server.wm;

import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IntArray;
import android.util.Slog;
import com.android.server.job.controllers.JobStatus;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.function.IntPredicate;
/* loaded from: classes2.dex */
class BackgroundLaunchProcessController {
    private static final String TAG = "ActivityTaskManager";
    private final BackgroundActivityStartCallback mBackgroundActivityStartCallback;
    private ArrayMap<Binder, IBinder> mBackgroundActivityStartTokens;
    private IntArray mBoundClientUids;
    private final IntPredicate mUidHasActiveVisibleWindowPredicate;

    /* JADX INFO: Access modifiers changed from: package-private */
    public BackgroundLaunchProcessController(IntPredicate uidHasActiveVisibleWindowPredicate, BackgroundActivityStartCallback callback) {
        this.mUidHasActiveVisibleWindowPredicate = uidHasActiveVisibleWindowPredicate;
        this.mBackgroundActivityStartCallback = callback;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean areBackgroundActivityStartsAllowed(int pid, int uid, String packageName, int appSwitchState, boolean isCheckingForFgsStart, boolean hasActivityInVisibleTask, boolean hasBackgroundActivityStartPrivileges, long lastStopAppSwitchesTime, long lastActivityLaunchTime, long lastActivityFinishTime) {
        if (appSwitchState == 2) {
            long now = SystemClock.uptimeMillis();
            if (now - lastActivityLaunchTime < JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY || now - lastActivityFinishTime < JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY) {
                if (lastActivityLaunchTime > lastStopAppSwitchesTime || lastActivityFinishTime > lastStopAppSwitchesTime) {
                    if (ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS) {
                        Slog.d(TAG, "[Process(" + pid + ")] Activity start allowed: within " + JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY + "ms grace period");
                    }
                    return true;
                } else if (ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS) {
                    Slog.d(TAG, "[Process(" + pid + ")] Activity start within " + JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY + "ms grace period but also within stop app switch window");
                }
            }
        }
        if (hasBackgroundActivityStartPrivileges) {
            if (ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS) {
                Slog.d(TAG, "[Process(" + pid + ")] Activity start allowed: process instrumenting with background activity starts privileges");
            }
            return true;
        } else if (hasActivityInVisibleTask && (appSwitchState == 2 || appSwitchState == 1)) {
            if (ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS) {
                Slog.d(TAG, "[Process(" + pid + ")] Activity start allowed: process has activity in foreground task");
            }
            return true;
        } else if (isBoundByForegroundUid()) {
            if (ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS) {
                Slog.d(TAG, "[Process(" + pid + ")] Activity start allowed: process bound by foreground uid");
            }
            return true;
        } else if (isBackgroundStartAllowedByToken(uid, packageName, isCheckingForFgsStart)) {
            if (ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS) {
                Slog.d(TAG, "[Process(" + pid + ")] Activity start allowed: process allowed by token");
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean isBackgroundStartAllowedByToken(int uid, String packageName, boolean isCheckingForFgsStart) {
        synchronized (this) {
            ArrayMap<Binder, IBinder> arrayMap = this.mBackgroundActivityStartTokens;
            if (arrayMap != null && !arrayMap.isEmpty()) {
                if (isCheckingForFgsStart) {
                    return true;
                }
                BackgroundActivityStartCallback backgroundActivityStartCallback = this.mBackgroundActivityStartCallback;
                if (backgroundActivityStartCallback == null) {
                    return true;
                }
                return backgroundActivityStartCallback.isActivityStartAllowed(this.mBackgroundActivityStartTokens.values(), uid, packageName);
            }
            return false;
        }
    }

    private boolean isBoundByForegroundUid() {
        synchronized (this) {
            IntArray intArray = this.mBoundClientUids;
            if (intArray != null) {
                for (int i = intArray.size() - 1; i >= 0; i--) {
                    if (this.mUidHasActiveVisibleWindowPredicate.test(this.mBoundClientUids.get(i))) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setBoundClientUids(ArraySet<Integer> boundClientUids) {
        synchronized (this) {
            if (boundClientUids != null) {
                if (!boundClientUids.isEmpty()) {
                    IntArray intArray = this.mBoundClientUids;
                    if (intArray == null) {
                        this.mBoundClientUids = new IntArray();
                    } else {
                        intArray.clear();
                    }
                    for (int i = boundClientUids.size() - 1; i >= 0; i--) {
                        this.mBoundClientUids.add(boundClientUids.valueAt(i).intValue());
                    }
                    return;
                }
            }
            this.mBoundClientUids = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addOrUpdateAllowBackgroundActivityStartsToken(Binder entity, IBinder originatingToken) {
        synchronized (this) {
            if (this.mBackgroundActivityStartTokens == null) {
                this.mBackgroundActivityStartTokens = new ArrayMap<>();
            }
            this.mBackgroundActivityStartTokens.put(entity, originatingToken);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeAllowBackgroundActivityStartsToken(Binder entity) {
        synchronized (this) {
            ArrayMap<Binder, IBinder> arrayMap = this.mBackgroundActivityStartTokens;
            if (arrayMap != null) {
                arrayMap.remove(entity);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canCloseSystemDialogsByToken(int uid) {
        if (this.mBackgroundActivityStartCallback == null) {
            return false;
        }
        synchronized (this) {
            ArrayMap<Binder, IBinder> arrayMap = this.mBackgroundActivityStartTokens;
            if (arrayMap != null && !arrayMap.isEmpty()) {
                return this.mBackgroundActivityStartCallback.canCloseSystemDialogs(this.mBackgroundActivityStartTokens.values(), uid);
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        synchronized (this) {
            ArrayMap<Binder, IBinder> arrayMap = this.mBackgroundActivityStartTokens;
            if (arrayMap != null && !arrayMap.isEmpty()) {
                pw.print(prefix);
                pw.println("Background activity start tokens (token: originating token):");
                for (int i = this.mBackgroundActivityStartTokens.size() - 1; i >= 0; i--) {
                    pw.print(prefix);
                    pw.print("  - ");
                    pw.print(this.mBackgroundActivityStartTokens.keyAt(i));
                    pw.print(": ");
                    pw.println(this.mBackgroundActivityStartTokens.valueAt(i));
                }
            }
            IntArray intArray = this.mBoundClientUids;
            if (intArray != null && intArray.size() > 0) {
                pw.print(prefix);
                pw.print("BoundClientUids:");
                pw.println(Arrays.toString(this.mBoundClientUids.toArray()));
            }
        }
    }
}
