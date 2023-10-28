package com.android.server.wm;

import android.app.ActivityOptions;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import com.android.server.wm.ActivityStarter;
import com.transsion.hubcore.server.wm.ITranWindowManagerService;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class LaunchParamsController {
    private final LaunchParamsPersister mPersister;
    private final ActivityTaskManagerService mService;
    private final List<LaunchParamsModifier> mModifiers = new ArrayList();
    private final LaunchParams mTmpParams = new LaunchParams();
    private final LaunchParams mTmpCurrent = new LaunchParams();
    private final LaunchParams mTmpResult = new LaunchParams();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface LaunchParamsModifier {
        public static final int PHASE_BOUNDS = 3;
        public static final int PHASE_DISPLAY = 0;
        public static final int PHASE_DISPLAY_AREA = 2;
        public static final int PHASE_WINDOWING_MODE = 1;
        public static final int RESULT_CONTINUE = 2;
        public static final int RESULT_DONE = 1;
        public static final int RESULT_SKIP = 0;

        @Retention(RetentionPolicy.SOURCE)
        /* loaded from: classes2.dex */
        public @interface Phase {
        }

        @Retention(RetentionPolicy.SOURCE)
        /* loaded from: classes2.dex */
        public @interface Result {
        }

        int onCalculate(Task task, ActivityInfo.WindowLayout windowLayout, ActivityRecord activityRecord, ActivityRecord activityRecord2, ActivityOptions activityOptions, ActivityStarter.Request request, int i, LaunchParams launchParams, LaunchParams launchParams2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public LaunchParamsController(ActivityTaskManagerService service, LaunchParamsPersister persister) {
        this.mService = service;
        this.mPersister = persister;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerDefaultModifiers(ActivityTaskSupervisor supervisor) {
        registerModifier(new TaskLaunchParamsModifier(supervisor));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void calculate(Task task, ActivityInfo.WindowLayout layout, ActivityRecord activity, ActivityRecord source, ActivityOptions options, ActivityStarter.Request request, int phase, LaunchParams result) {
        result.reset();
        if (task != null || activity != null) {
            this.mPersister.getLaunchParams(task, activity, result);
        }
        for (int i = this.mModifiers.size() - 1; i >= 0; i--) {
            this.mTmpCurrent.set(result);
            this.mTmpResult.reset();
            LaunchParamsModifier modifier = this.mModifiers.get(i);
            switch (modifier.onCalculate(task, layout, activity, source, options, request, phase, this.mTmpCurrent, this.mTmpResult)) {
                case 1:
                    result.set(this.mTmpResult);
                    return;
                case 2:
                    result.set(this.mTmpResult);
                    break;
            }
        }
        if (activity != null && activity.requestedVrComponent != null) {
            result.mPreferredTaskDisplayArea = this.mService.mRootWindowContainer.getDefaultTaskDisplayArea();
        } else if (this.mService.mVr2dDisplayId != -1) {
            result.mPreferredTaskDisplayArea = this.mService.mRootWindowContainer.getDisplayContent(this.mService.mVr2dDisplayId).getDefaultTaskDisplayArea();
        }
        ITranWindowManagerService.Instance().fixPersistLaunchParams(task, layout, activity, source, options, result, 0);
    }

    boolean layoutTask(Task task, ActivityInfo.WindowLayout layout) {
        return layoutTask(task, layout, null, null, null);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [166=4] */
    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean layoutTask(Task task, ActivityInfo.WindowLayout layout, ActivityRecord activity, ActivityRecord source, ActivityOptions options) {
        calculate(task, layout, activity, source, options, null, 3, this.mTmpParams);
        if (this.mTmpParams.isEmpty()) {
            return false;
        }
        this.mService.deferWindowLayout();
        try {
            if (this.mTmpParams.mBounds.isEmpty()) {
                return false;
            }
            if (task.getRootTask().inMultiWindowMode()) {
                task.setBounds(this.mTmpParams.mBounds);
                return true;
            }
            task.setLastNonFullscreenBounds(this.mTmpParams.mBounds);
            return false;
        } finally {
            this.mService.continueWindowLayout();
        }
    }

    void registerModifier(LaunchParamsModifier modifier) {
        if (this.mModifiers.contains(modifier)) {
            return;
        }
        this.mModifiers.add(modifier);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class LaunchParams {
        final Rect mBounds = new Rect();
        TaskDisplayArea mPreferredTaskDisplayArea;
        int mWindowingMode;

        /* JADX INFO: Access modifiers changed from: package-private */
        public void reset() {
            this.mBounds.setEmpty();
            this.mPreferredTaskDisplayArea = null;
            this.mWindowingMode = 0;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void set(LaunchParams params) {
            this.mBounds.set(params.mBounds);
            this.mPreferredTaskDisplayArea = params.mPreferredTaskDisplayArea;
            this.mWindowingMode = params.mWindowingMode;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public boolean isEmpty() {
            return this.mBounds.isEmpty() && this.mPreferredTaskDisplayArea == null && this.mWindowingMode == 0;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public boolean hasWindowingMode() {
            return this.mWindowingMode != 0;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public boolean hasPreferredTaskDisplayArea() {
            return this.mPreferredTaskDisplayArea != null;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            LaunchParams that = (LaunchParams) o;
            if (this.mPreferredTaskDisplayArea != that.mPreferredTaskDisplayArea || this.mWindowingMode != that.mWindowingMode) {
                return false;
            }
            Rect rect = this.mBounds;
            if (rect != null) {
                return rect.equals(that.mBounds);
            }
            if (that.mBounds == null) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            Rect rect = this.mBounds;
            int result = rect != null ? rect.hashCode() : 0;
            int i = result * 31;
            TaskDisplayArea taskDisplayArea = this.mPreferredTaskDisplayArea;
            int result2 = i + (taskDisplayArea != null ? taskDisplayArea.hashCode() : 0);
            return (result2 * 31) + this.mWindowingMode;
        }
    }
}
