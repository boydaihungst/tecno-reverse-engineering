package com.android.server.wm;

import android.util.ArraySet;
import android.util.Slog;
import com.android.server.wm.ActivityRecord;
import java.io.PrintWriter;
import java.util.function.Consumer;
/* loaded from: classes2.dex */
public class ActivityServiceConnectionsHolder<T> {
    private final ActivityRecord mActivity;
    private ArraySet<T> mConnections;
    private volatile boolean mIsDisconnecting;
    private final ActivityTaskManagerService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityServiceConnectionsHolder(ActivityTaskManagerService service, ActivityRecord activity) {
        this.mService = service;
        this.mActivity = activity;
    }

    public void addConnection(T c) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mIsDisconnecting) {
                    if (ActivityTaskManagerDebugConfig.DEBUG_CLEANUP) {
                        Slog.e("ActivityTaskManager", "Skip adding connection " + c + " to a disconnecting holder of " + this.mActivity);
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                if (this.mConnections == null) {
                    this.mConnections = new ArraySet<>();
                }
                this.mConnections.add(c);
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void removeConnection(T c) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mConnections == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                if (ActivityTaskManagerDebugConfig.DEBUG_CLEANUP && this.mIsDisconnecting) {
                    Slog.v("ActivityTaskManager", "Remove pending disconnecting " + c + " of " + this.mActivity);
                }
                this.mConnections.remove(c);
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public boolean isActivityVisible() {
        boolean z;
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                z = this.mActivity.mVisibleRequested || this.mActivity.isState(ActivityRecord.State.RESUMED, ActivityRecord.State.PAUSING);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return z;
    }

    public int getActivityPid() {
        int pid;
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                pid = this.mActivity.hasProcess() ? this.mActivity.app.getPid() : -1;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return pid;
    }

    public void forEachConnection(Consumer<T> consumer) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ArraySet<T> arraySet = this.mConnections;
                if (arraySet != null && !arraySet.isEmpty()) {
                    for (int i = this.mConnections.size() - 1; i >= 0; i--) {
                        consumer.accept(this.mConnections.valueAt(i));
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void disconnectActivityFromServices() {
        ArraySet<T> arraySet = this.mConnections;
        if (arraySet == null || arraySet.isEmpty() || this.mIsDisconnecting) {
            return;
        }
        this.mIsDisconnecting = true;
        this.mService.mH.post(new Runnable() { // from class: com.android.server.wm.ActivityServiceConnectionsHolder$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ActivityServiceConnectionsHolder.this.m7791x4ad2c2fa();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$disconnectActivityFromServices$0$com-android-server-wm-ActivityServiceConnectionsHolder  reason: not valid java name */
    public /* synthetic */ void m7791x4ad2c2fa() {
        this.mService.mAmInternal.disconnectActivityFromServices(this);
        this.mIsDisconnecting = false;
    }

    public void dump(PrintWriter pw, String prefix) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                pw.println(prefix + "activity=" + this.mActivity);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public String toString() {
        return String.valueOf(this.mConnections);
    }
}
