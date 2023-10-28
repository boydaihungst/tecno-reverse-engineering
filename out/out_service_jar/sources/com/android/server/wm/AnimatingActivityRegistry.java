package com.android.server.wm;

import android.util.ArrayMap;
import android.util.ArraySet;
import java.io.PrintWriter;
import java.util.ArrayList;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class AnimatingActivityRegistry {
    private boolean mEndingDeferredFinish;
    private ArraySet<ActivityRecord> mAnimatingActivities = new ArraySet<>();
    private ArrayMap<ActivityRecord, Runnable> mFinishedTokens = new ArrayMap<>();
    private ArrayList<Runnable> mTmpRunnableList = new ArrayList<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyStarting(ActivityRecord token) {
        this.mAnimatingActivities.add(token);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyFinished(ActivityRecord activity) {
        this.mAnimatingActivities.remove(activity);
        this.mFinishedTokens.remove(activity);
        if (this.mAnimatingActivities.isEmpty()) {
            endDeferringFinished();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean notifyAboutToFinish(ActivityRecord activity, Runnable endDeferFinishCallback) {
        boolean removed = this.mAnimatingActivities.remove(activity);
        if (!removed) {
            return false;
        }
        if (this.mAnimatingActivities.isEmpty()) {
            endDeferringFinished();
            return false;
        }
        this.mFinishedTokens.put(activity, endDeferFinishCallback);
        return true;
    }

    private void endDeferringFinished() {
        if (this.mEndingDeferredFinish) {
            return;
        }
        try {
            this.mEndingDeferredFinish = true;
            for (int i = this.mFinishedTokens.size() - 1; i >= 0; i--) {
                this.mTmpRunnableList.add(this.mFinishedTokens.valueAt(i));
            }
            this.mFinishedTokens.clear();
            for (int i2 = this.mTmpRunnableList.size() - 1; i2 >= 0; i2--) {
                this.mTmpRunnableList.get(i2).run();
            }
            this.mTmpRunnableList.clear();
        } finally {
            this.mEndingDeferredFinish = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String header, String prefix) {
        if (!this.mAnimatingActivities.isEmpty() || !this.mFinishedTokens.isEmpty()) {
            pw.print(prefix);
            pw.println(header);
            String prefix2 = prefix + "  ";
            pw.print(prefix2);
            pw.print("mAnimatingActivities=");
            pw.println(this.mAnimatingActivities);
            pw.print(prefix2);
            pw.print("mFinishedTokens=");
            pw.println(this.mFinishedTokens);
        }
    }
}
