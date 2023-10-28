package com.android.server.wm;

import android.content.pm.ActivityInfo;
import android.util.ArraySet;
import android.window.DisplayWindowPolicyController;
import java.io.PrintWriter;
import java.util.List;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class DisplayWindowPolicyControllerHelper {
    private final DisplayContent mDisplayContent;
    private DisplayWindowPolicyController mDisplayWindowPolicyController;
    private ActivityRecord mTopRunningActivity = null;
    private ArraySet<Integer> mRunningUid = new ArraySet<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayWindowPolicyControllerHelper(DisplayContent displayContent) {
        this.mDisplayContent = displayContent;
        this.mDisplayWindowPolicyController = displayContent.mWmService.mDisplayManagerInternal.getDisplayWindowPolicyController(displayContent.mDisplayId);
    }

    public boolean hasController() {
        return this.mDisplayWindowPolicyController != null;
    }

    public boolean canContainActivities(List<ActivityInfo> activities, int windowingMode) {
        DisplayWindowPolicyController displayWindowPolicyController = this.mDisplayWindowPolicyController;
        if (displayWindowPolicyController == null) {
            return true;
        }
        return displayWindowPolicyController.canContainActivities(activities, windowingMode);
    }

    public boolean canActivityBeLaunched(ActivityInfo activityInfo, int windowingMode, int launchingFromDisplayId, boolean isNewTask) {
        DisplayWindowPolicyController displayWindowPolicyController = this.mDisplayWindowPolicyController;
        if (displayWindowPolicyController == null) {
            return true;
        }
        return displayWindowPolicyController.canActivityBeLaunched(activityInfo, windowingMode, launchingFromDisplayId, isNewTask);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean keepActivityOnWindowFlagsChanged(ActivityInfo aInfo, int flagChanges, int privateFlagChanges) {
        DisplayWindowPolicyController displayWindowPolicyController = this.mDisplayWindowPolicyController;
        if (displayWindowPolicyController != null && displayWindowPolicyController.isInterestedWindowFlags(flagChanges, privateFlagChanges)) {
            return this.mDisplayWindowPolicyController.keepActivityOnWindowFlagsChanged(aInfo, flagChanges, privateFlagChanges);
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onRunningActivityChanged() {
        if (this.mDisplayWindowPolicyController == null) {
            return;
        }
        ActivityRecord topActivity = this.mDisplayContent.getTopActivity(false, true);
        if (topActivity != this.mTopRunningActivity) {
            this.mTopRunningActivity = topActivity;
            this.mDisplayWindowPolicyController.onTopActivityChanged(topActivity == null ? null : topActivity.info.getComponentName(), topActivity == null ? -10000 : topActivity.info.applicationInfo.uid);
        }
        final boolean[] notifyChanged = {false};
        final ArraySet<Integer> runningUids = new ArraySet<>();
        this.mDisplayContent.forAllActivities(new Consumer() { // from class: com.android.server.wm.DisplayWindowPolicyControllerHelper$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DisplayWindowPolicyControllerHelper.lambda$onRunningActivityChanged$0(notifyChanged, runningUids, (ActivityRecord) obj);
            }
        });
        if (notifyChanged[0] || this.mRunningUid.size() != runningUids.size()) {
            this.mRunningUid = runningUids;
            this.mDisplayWindowPolicyController.onRunningAppsChanged(runningUids);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$onRunningActivityChanged$0(boolean[] notifyChanged, ArraySet runningUids, ActivityRecord r) {
        if (!r.finishing) {
            notifyChanged[0] = notifyChanged[0] | runningUids.add(Integer.valueOf(r.getUid()));
        }
    }

    public final boolean isWindowingModeSupported(int windowingMode) {
        DisplayWindowPolicyController displayWindowPolicyController = this.mDisplayWindowPolicyController;
        if (displayWindowPolicyController == null) {
            return true;
        }
        return displayWindowPolicyController.isWindowingModeSupported(windowingMode);
    }

    public final boolean canShowTasksInRecents() {
        DisplayWindowPolicyController displayWindowPolicyController = this.mDisplayWindowPolicyController;
        if (displayWindowPolicyController == null) {
            return true;
        }
        return displayWindowPolicyController.canShowTasksInRecents();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(String prefix, PrintWriter pw) {
        if (this.mDisplayWindowPolicyController != null) {
            pw.println();
            this.mDisplayWindowPolicyController.dump(prefix, pw);
        }
    }
}
