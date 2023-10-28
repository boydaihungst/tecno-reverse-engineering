package com.android.server.wm;

import android.util.ArrayMap;
import com.android.internal.os.BackgroundThread;
import java.io.PrintWriter;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class VisibleActivityProcessTracker {
    final ActivityTaskManagerService mAtms;
    private final ArrayMap<WindowProcessController, CpuTimeRecord> mProcMap = new ArrayMap<>();
    final Executor mBgExecutor = BackgroundThread.getExecutor();

    /* JADX INFO: Access modifiers changed from: package-private */
    public VisibleActivityProcessTracker(ActivityTaskManagerService atms) {
        this.mAtms = atms;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAnyActivityVisible(WindowProcessController wpc) {
        CpuTimeRecord r = new CpuTimeRecord(wpc);
        synchronized (this.mProcMap) {
            this.mProcMap.put(wpc, r);
        }
        if (wpc.hasResumedActivity()) {
            r.mShouldGetCpuTime = true;
            this.mBgExecutor.execute(r);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAllActivitiesInvisible(WindowProcessController wpc) {
        CpuTimeRecord r = removeProcess(wpc);
        if (r != null && r.mShouldGetCpuTime) {
            this.mBgExecutor.execute(r);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onActivityResumedWhileVisible(WindowProcessController wpc) {
        CpuTimeRecord r;
        synchronized (this.mProcMap) {
            r = this.mProcMap.get(wpc);
        }
        if (r != null && !r.mShouldGetCpuTime) {
            r.mShouldGetCpuTime = true;
            this.mBgExecutor.execute(r);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasResumedActivity(int uid) {
        return match(uid, new Predicate() { // from class: com.android.server.wm.VisibleActivityProcessTracker$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return ((WindowProcessController) obj).hasResumedActivity();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasVisibleActivity(int uid) {
        return match(uid, null);
    }

    private boolean match(int uid, Predicate<WindowProcessController> predicate) {
        synchronized (this.mProcMap) {
            for (int i = this.mProcMap.size() - 1; i >= 0; i--) {
                WindowProcessController wpc = this.mProcMap.keyAt(i);
                if (wpc.mUid == uid && (predicate == null || predicate.test(wpc))) {
                    return true;
                }
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public CpuTimeRecord removeProcess(WindowProcessController wpc) {
        CpuTimeRecord remove;
        synchronized (this.mProcMap) {
            remove = this.mProcMap.remove(wpc);
        }
        return remove;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix + "VisibleActivityProcess:[");
        synchronized (this.mProcMap) {
            for (int i = this.mProcMap.size() - 1; i >= 0; i--) {
                pw.print(" " + this.mProcMap.keyAt(i));
            }
        }
        pw.println("]");
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class CpuTimeRecord implements Runnable {
        private long mCpuTime;
        private boolean mHasStartCpuTime;
        private final WindowProcessController mProc;
        boolean mShouldGetCpuTime;

        CpuTimeRecord(WindowProcessController wpc) {
            this.mProc = wpc;
        }

        @Override // java.lang.Runnable
        public void run() {
            if (this.mProc.getPid() == 0) {
                return;
            }
            if (!this.mHasStartCpuTime) {
                this.mHasStartCpuTime = true;
                this.mCpuTime = this.mProc.getCpuTime();
                return;
            }
            long diff = this.mProc.getCpuTime() - this.mCpuTime;
            if (diff > 0) {
                VisibleActivityProcessTracker.this.mAtms.mAmInternal.updateForegroundTimeIfOnBattery(this.mProc.mInfo.packageName, this.mProc.mInfo.uid, diff);
            }
        }
    }
}
