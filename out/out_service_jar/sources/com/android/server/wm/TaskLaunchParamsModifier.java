package com.android.server.wm;

import android.app.ActivityOptions;
import android.app.ThunderbackConfig;
import android.app.WindowConfiguration;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.os.SystemProperties;
import android.util.Slog;
import android.window.WindowContainerToken;
import com.android.server.wm.ActivityStarter;
import com.android.server.wm.LaunchParamsController;
import com.transsion.hubcore.multiwindow.ITranMultiWindow;
import com.transsion.hubcore.server.wm.ITranTaskLaunchParamsModifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class TaskLaunchParamsModifier implements LaunchParamsController.LaunchParamsModifier {
    private static final int BOUNDS_CONFLICT_THRESHOLD = 4;
    private static final int CASCADING_OFFSET_DP = 75;
    private static final boolean DEBUG = SystemProperties.getBoolean("debug.multiwindow.log", true);
    private static final int DEFAULT_PORTRAIT_PHONE_HEIGHT_DP = 732;
    private static final int DEFAULT_PORTRAIT_PHONE_WIDTH_DP = 412;
    private static final int EPSILON = 2;
    private static final int MINIMAL_STEP = 1;
    private static final int STEP_DENOMINATOR = 16;
    private static final String TAG = "ActivityTaskManager";
    private StringBuilder mLogBuilder;
    private final ActivityTaskSupervisor mSupervisor;
    private TaskDisplayArea mTmpDisplayArea;
    private final Rect mTmpBounds = new Rect();
    private final Rect mTmpStableBounds = new Rect();
    private final int[] mTmpDirections = new int[2];

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskLaunchParamsModifier(ActivityTaskSupervisor supervisor) {
        this.mSupervisor = supervisor;
    }

    @Override // com.android.server.wm.LaunchParamsController.LaunchParamsModifier
    public int onCalculate(Task task, ActivityInfo.WindowLayout layout, ActivityRecord activity, ActivityRecord source, ActivityOptions options, ActivityStarter.Request request, int phase, LaunchParamsController.LaunchParams currentParams, LaunchParamsController.LaunchParams outParams) {
        initLogBuilder(task, activity);
        int result = calculate(task, layout, activity, source, options, request, phase, currentParams, outParams);
        outputLog();
        return result;
    }

    /* JADX WARN: Removed duplicated region for block: B:130:0x02cb  */
    /* JADX WARN: Removed duplicated region for block: B:131:0x02ce  */
    /* JADX WARN: Removed duplicated region for block: B:134:0x02d3 A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:136:0x02d5  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int calculate(Task task, ActivityInfo.WindowLayout layout, ActivityRecord activity, ActivityRecord source, ActivityOptions options, ActivityStarter.Request request, int phase, LaunchParamsController.LaunchParams currentParams, LaunchParamsController.LaunchParams outParams) {
        ActivityRecord root;
        DisplayContent display;
        TaskDisplayArea suggestedDisplayArea;
        int i;
        int launchMode;
        int activityType;
        int resolvedMode;
        int i2;
        TaskDisplayArea taskDisplayArea;
        int i3;
        if (task != null) {
            root = task.getRootActivity() == null ? activity : task.getRootActivity();
        } else {
            root = activity;
        }
        if (root == null) {
            return 0;
        }
        TaskDisplayArea suggestedDisplayArea2 = getPreferredLaunchTaskDisplayArea(task, options, source, currentParams, activity, request);
        outParams.mPreferredTaskDisplayArea = suggestedDisplayArea2;
        DisplayContent display2 = suggestedDisplayArea2.mDisplayContent;
        boolean z = DEBUG;
        if (z) {
            appendLog("display-id=" + display2.getDisplayId() + " display-windowing-mode=" + display2.getWindowingMode() + " suggested-display-area=" + suggestedDisplayArea2);
        }
        if (phase == 0) {
            return 2;
        }
        int launchMode2 = options != null ? options.getLaunchWindowingMode() : 0;
        if (launchMode2 == 0 && canInheritWindowingModeFromSource(display2, source)) {
            launchMode2 = source.getTask().getWindowingMode();
            if (z) {
                appendLog("inherit-from-source=" + WindowConfiguration.windowingModeToString(launchMode2));
            }
        }
        if (launchMode2 == 0 && task != null && task.getTaskDisplayArea() == suggestedDisplayArea2) {
            launchMode2 = task.getWindowingMode();
            if (z) {
                appendLog("inherit-from-task=" + WindowConfiguration.windowingModeToString(launchMode2));
            }
        }
        boolean hasInitialBounds = false;
        boolean hasInitialBoundsForSuggestedDisplayAreaInFreeformWindow = false;
        boolean canApplyFreeformPolicy = canApplyFreeformWindowPolicy(display2, launchMode2);
        if (this.mSupervisor.canUseActivityOptionsLaunchBounds(options) && (canApplyFreeformPolicy || canApplyPipWindowPolicy(launchMode2))) {
            hasInitialBounds = true;
            if (launchMode2 == 0) {
                i3 = 5;
            } else {
                i3 = launchMode2;
            }
            launchMode2 = i3;
            outParams.mBounds.set(options.getLaunchBounds());
            if (z) {
                appendLog("activity-options-bounds=" + outParams.mBounds);
            }
        } else if (launchMode2 == 2) {
            if (z) {
                appendLog("empty-window-layout-for-pip");
            }
        } else if (launchMode2 == 1) {
            if (z) {
                appendLog("activity-options-fullscreen=" + outParams.mBounds);
            }
        } else if (layout != null && canApplyFreeformPolicy) {
            this.mTmpBounds.set(currentParams.mBounds);
            getLayoutBounds(suggestedDisplayArea2, root, layout, this.mTmpBounds);
            if (!this.mTmpBounds.isEmpty()) {
                launchMode2 = 5;
                outParams.mBounds.set(this.mTmpBounds);
                hasInitialBounds = true;
                hasInitialBoundsForSuggestedDisplayAreaInFreeformWindow = true;
                if (z) {
                    appendLog("bounds-from-layout=" + outParams.mBounds);
                }
            } else if (z) {
                appendLog("empty-window-layout");
            }
        } else if (launchMode2 == 6 && options != null && options.getLaunchBounds() != null) {
            outParams.mBounds.set(options.getLaunchBounds());
            hasInitialBounds = true;
            if (z) {
                appendLog("multiwindow-activity-options-bounds=" + outParams.mBounds);
            }
        }
        boolean hasInitialBounds2 = hasInitialBounds;
        boolean hasInitialBoundsForSuggestedDisplayAreaInFreeformWindow2 = hasInitialBoundsForSuggestedDisplayAreaInFreeformWindow;
        boolean fullyResolvedCurrentParam = false;
        if (!currentParams.isEmpty() && !hasInitialBounds2) {
            if (currentParams.mPreferredTaskDisplayArea == null || currentParams.mPreferredTaskDisplayArea.getDisplayId() == display2.getDisplayId()) {
                if (currentParams.hasWindowingMode() && display2.inFreeformWindowingMode()) {
                    launchMode2 = currentParams.mWindowingMode;
                    fullyResolvedCurrentParam = launchMode2 != 5;
                    if (z) {
                        appendLog("inherit-" + WindowConfiguration.windowingModeToString(launchMode2));
                    }
                }
                if (!currentParams.mBounds.isEmpty()) {
                    outParams.mBounds.set(currentParams.mBounds);
                    if (launchMode2 == 5) {
                        fullyResolvedCurrentParam = true;
                        if (z) {
                            appendLog("inherit-bounds=" + outParams.mBounds);
                        }
                    }
                }
            }
        }
        boolean fullyResolvedCurrentParam2 = fullyResolvedCurrentParam;
        boolean hasInitialBoundsForSuggestedDisplayAreaInFreeformDisplay = false;
        if (display2.inFreeformWindowingMode()) {
            if (launchMode2 == 2) {
                if (z) {
                    appendLog("picture-in-picture");
                }
                display = display2;
                suggestedDisplayArea = suggestedDisplayArea2;
                i = 1;
            } else if (root.isResizeable()) {
                display = display2;
                suggestedDisplayArea = suggestedDisplayArea2;
                i = 1;
            } else if (shouldLaunchUnresizableAppInFreeform(root, suggestedDisplayArea2, options)) {
                if (outParams.mBounds.isEmpty()) {
                    i = 1;
                    display = display2;
                    suggestedDisplayArea = suggestedDisplayArea2;
                    getTaskBounds(root, suggestedDisplayArea2, layout, 5, hasInitialBounds2, outParams.mBounds);
                    hasInitialBoundsForSuggestedDisplayAreaInFreeformDisplay = true;
                } else {
                    display = display2;
                    suggestedDisplayArea = suggestedDisplayArea2;
                    i = 1;
                }
                if (z) {
                    appendLog("unresizable-freeform");
                }
                launchMode = 5;
                outParams.mWindowingMode = launchMode != display.getWindowingMode() ? 0 : launchMode;
                if (phase != i) {
                    return 2;
                }
                final int resolvedMode2 = launchMode != 0 ? launchMode : display.getWindowingMode();
                TaskDisplayArea taskDisplayArea2 = suggestedDisplayArea;
                if (options != null && options.getLaunchTaskDisplayArea() != null) {
                    resolvedMode = resolvedMode2;
                    i2 = 2;
                    taskDisplayArea = taskDisplayArea2;
                } else {
                    final int activityType2 = this.mSupervisor.mRootWindowContainer.resolveActivityType(root, options, task);
                    display.forAllTaskDisplayAreas(new Predicate() { // from class: com.android.server.wm.TaskLaunchParamsModifier$$ExternalSyntheticLambda0
                        @Override // java.util.function.Predicate
                        public final boolean test(Object obj) {
                            return TaskLaunchParamsModifier.this.m8359x4de0cd3b(resolvedMode2, activityType2, (TaskDisplayArea) obj);
                        }
                    });
                    TaskDisplayArea taskDisplayArea3 = this.mTmpDisplayArea;
                    if (taskDisplayArea3 == null) {
                        activityType = activityType2;
                        resolvedMode = resolvedMode2;
                        i2 = 2;
                    } else if (taskDisplayArea3 == suggestedDisplayArea) {
                        activityType = activityType2;
                        resolvedMode = resolvedMode2;
                        i2 = 2;
                    } else if (hasInitialBoundsForSuggestedDisplayAreaInFreeformWindow2) {
                        outParams.mBounds.setEmpty();
                        getLayoutBounds(this.mTmpDisplayArea, root, layout, outParams.mBounds);
                        hasInitialBounds2 = outParams.mBounds.isEmpty() ^ i;
                        activityType = activityType2;
                        resolvedMode = resolvedMode2;
                        i2 = 2;
                    } else if (hasInitialBoundsForSuggestedDisplayAreaInFreeformDisplay) {
                        outParams.mBounds.setEmpty();
                        activityType = activityType2;
                        resolvedMode = resolvedMode2;
                        i2 = 2;
                        getTaskBounds(root, this.mTmpDisplayArea, layout, launchMode, hasInitialBounds2, outParams.mBounds);
                    } else {
                        activityType = activityType2;
                        resolvedMode = resolvedMode2;
                        i2 = 2;
                    }
                    if (this.mTmpDisplayArea == null) {
                        taskDisplayArea = taskDisplayArea2;
                    } else {
                        TaskDisplayArea taskDisplayArea4 = this.mTmpDisplayArea;
                        this.mTmpDisplayArea = null;
                        appendLog("overridden-display-area=[" + WindowConfiguration.activityTypeToString(activityType) + ", " + WindowConfiguration.windowingModeToString(resolvedMode) + ", " + taskDisplayArea4 + "]");
                        taskDisplayArea = taskDisplayArea4;
                    }
                }
                appendLog("display-area=" + taskDisplayArea);
                outParams.mPreferredTaskDisplayArea = taskDisplayArea;
                if (phase == i2) {
                    return i2;
                }
                if (fullyResolvedCurrentParam2) {
                    if (resolvedMode == 5) {
                        if (currentParams.mPreferredTaskDisplayArea != taskDisplayArea) {
                            adjustBoundsToFitInDisplayArea(taskDisplayArea, outParams.mBounds);
                        }
                        adjustBoundsToAvoidConflictInDisplayArea(taskDisplayArea, outParams.mBounds);
                    }
                } else {
                    int resolvedMode3 = resolvedMode;
                    if (taskDisplayArea.inFreeformWindowingMode()) {
                        if (source != null && source.inFreeformWindowingMode() && resolvedMode3 == 5 && outParams.mBounds.isEmpty() && source.getDisplayArea() == taskDisplayArea) {
                            cascadeBounds(source.getConfiguration().windowConfiguration.getBounds(), taskDisplayArea, outParams.mBounds);
                        }
                        getTaskBounds(root, taskDisplayArea, layout, resolvedMode3, hasInitialBounds2, outParams.mBounds);
                    }
                }
                return i2;
            } else {
                display = display2;
                suggestedDisplayArea = suggestedDisplayArea2;
                i = 1;
                launchMode2 = 1;
                outParams.mBounds.setEmpty();
                if (z) {
                    appendLog("unresizable-forced-maximize");
                }
            }
        } else {
            display = display2;
            suggestedDisplayArea = suggestedDisplayArea2;
            i = 1;
            if (z) {
                appendLog("non-freeform-display");
            }
        }
        launchMode = launchMode2;
        outParams.mWindowingMode = launchMode != display.getWindowingMode() ? 0 : launchMode;
        if (phase != i) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$calculate$0$com-android-server-wm-TaskLaunchParamsModifier  reason: not valid java name */
    public /* synthetic */ boolean m8359x4de0cd3b(int resolvedMode, int activityType, TaskDisplayArea displayArea) {
        Task launchRoot = displayArea.getLaunchRootTask(resolvedMode, activityType, null, null, 0);
        if (launchRoot == null) {
            return false;
        }
        this.mTmpDisplayArea = displayArea;
        return true;
    }

    private boolean isInMultiWindowBlackList(ActivityRecord activityRecord) {
        if (activityRecord == null) {
            return false;
        }
        boolean isSpecialPackageName = "com.android.settings".equals(activityRecord.packageName);
        return this.mSupervisor.mService.isInBlackList(activityRecord.packageName) && !isSpecialPackageName;
    }

    /* JADX WARN: Removed duplicated region for block: B:106:0x025d  */
    /* JADX WARN: Removed duplicated region for block: B:114:0x027e  */
    /* JADX WARN: Removed duplicated region for block: B:117:0x0299  */
    /* JADX WARN: Removed duplicated region for block: B:163:0x0386  */
    /* JADX WARN: Removed duplicated region for block: B:170:0x03a7  */
    /* JADX WARN: Removed duplicated region for block: B:180:0x03e3  */
    /* JADX WARN: Removed duplicated region for block: B:182:0x03fb  */
    /* JADX WARN: Removed duplicated region for block: B:183:0x0400  */
    /* JADX WARN: Removed duplicated region for block: B:198:0x043f  */
    /* JADX WARN: Removed duplicated region for block: B:203:0x0449  */
    /* JADX WARN: Removed duplicated region for block: B:209:0x045a  */
    /* JADX WARN: Removed duplicated region for block: B:222:0x04a1  */
    /* JADX WARN: Removed duplicated region for block: B:224:0x04a5  */
    /* JADX WARN: Removed duplicated region for block: B:225:0x04a9  */
    /* JADX WARN: Removed duplicated region for block: B:65:0x0180  */
    /* JADX WARN: Removed duplicated region for block: B:68:0x01a4  */
    /* JADX WARN: Removed duplicated region for block: B:69:0x01ad  */
    /* JADX WARN: Removed duplicated region for block: B:79:0x01c1  */
    /* JADX WARN: Removed duplicated region for block: B:85:0x01e5  */
    /* JADX WARN: Removed duplicated region for block: B:87:0x01e9  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private TaskDisplayArea getPreferredLaunchTaskDisplayArea(Task task, ActivityOptions options, ActivityRecord source, LaunchParamsController.LaunchParams currentParams, ActivityRecord activityRecord, ActivityStarter.Request request) {
        TaskDisplayArea taskDisplayArea;
        TaskDisplayArea taskDisplayArea2;
        Task rootTask;
        boolean isUnsupportMultiWin;
        int callerDisplayId;
        DisplayContent dc;
        TaskDisplayArea sourceDisplayArea;
        boolean z;
        TaskDisplayArea preferMultiDisplayArea;
        TaskDisplayArea taskDisplayArea3 = null;
        WindowContainerToken optionLaunchTaskDisplayAreaToken = options != null ? options.getLaunchTaskDisplayArea() : null;
        boolean isNeedIntoMultiwindow = ITranTaskLaunchParamsModifier.Instance().isNeedIntoMultiwindow(activityRecord, options);
        String pkgName = ITranMultiWindow.Instance().getReadyStartInMultiWindowPackageName();
        TaskDisplayArea preferTaskDisplayArea = null;
        boolean isMultiWindowTaskDisplayArea = false;
        if (ThunderbackConfig.isVersion4()) {
            preferTaskDisplayArea = this.mSupervisor.mRootWindowContainer.getPreferMultiTaskDisplayArea();
            isMultiWindowTaskDisplayArea = preferTaskDisplayArea != null ? preferTaskDisplayArea.isMultiWindow() : false;
            if (DEBUG) {
                Slog.i(TAG, "TaskLaunchParamsModifier: pkgName = " + pkgName + ", isMultiWindowTaskDisplayArea  = " + isMultiWindowTaskDisplayArea);
            }
        }
        if (optionLaunchTaskDisplayAreaToken != null) {
            taskDisplayArea3 = (TaskDisplayArea) WindowContainer.fromBinder(optionLaunchTaskDisplayAreaToken.asBinder());
            if (isInMultiWindowBlackList(activityRecord) && taskDisplayArea3.isMultiWindow()) {
                taskDisplayArea3 = null;
            }
            if (DEBUG) {
                appendLog("display-area-from-option=" + taskDisplayArea3);
            }
        }
        if (taskDisplayArea3 == null && activityRecord != null && (source == null || !source.isVisible())) {
            if (ThunderbackConfig.isVersion4() && isNeedIntoMultiwindow) {
                if (isMultiWindowTaskDisplayArea) {
                    taskDisplayArea3 = this.mSupervisor.mRootWindowContainer.getPreferMultiDisplayArea(pkgName);
                    if (DEBUG) {
                        Slog.i(TAG, "TaskLaunchParamsModifier: getPreferMultiDisplayArea : " + taskDisplayArea3);
                    }
                }
            } else if (ThunderbackConfig.isVersion3() && this.mSupervisor.mService.activityInMultiWindow(activityRecord.packageName)) {
                TaskDisplayArea taskDisplayAreaForMultiWindow = this.mSupervisor.mRootWindowContainer.getMultiDisplayArea();
                Slog.i(TAG, "TaskLaunchParamsModifier: already in multi-window : packageName = " + activityRecord.packageName);
                taskDisplayArea3 = taskDisplayAreaForMultiWindow;
            }
        }
        if (taskDisplayArea3 != null || isNeedIntoMultiwindow) {
            taskDisplayArea = taskDisplayArea3;
        } else {
            int optionLaunchId = options != null ? options.getLaunchDisplayId() : -1;
            if (optionLaunchId == -1) {
                taskDisplayArea = taskDisplayArea3;
            } else {
                DisplayContent dc2 = this.mSupervisor.mRootWindowContainer.getDisplayContent(optionLaunchId);
                if (dc2 == null) {
                    taskDisplayArea = taskDisplayArea3;
                } else {
                    if (this.mSupervisor.mService.activityInMultiWindow(activityRecord.packageName)) {
                        TaskDisplayArea taskDisplayAreaForMultiWindow2 = this.mSupervisor.mRootWindowContainer.getMultiDisplayArea();
                        Slog.i(TAG, "TaskLaunchParamsModifier: already in multi-window: packageName = " + activityRecord.packageName);
                        taskDisplayArea2 = taskDisplayAreaForMultiWindow2;
                    } else {
                        taskDisplayArea2 = dc2.getDefaultTaskDisplayArea();
                    }
                    if (DEBUG) {
                        appendLog("display-from-option=" + optionLaunchId);
                    }
                    if (taskDisplayArea2 == null && isNeedIntoMultiwindow) {
                        TaskDisplayArea taskDisplayAreaForMultiWindow3 = null;
                        if (ThunderbackConfig.isVersion3()) {
                            taskDisplayAreaForMultiWindow3 = this.mSupervisor.mRootWindowContainer.getMultiDisplayArea();
                            Slog.i(TAG, "TaskLaunchParamsModifier:  taskDisplayAreaForMultiWindow: " + taskDisplayAreaForMultiWindow3);
                        }
                        if (!isInMultiWindowBlackList(activityRecord)) {
                            ITranTaskLaunchParamsModifier.Instance().showUnSupportMultiToast();
                            taskDisplayArea2 = null;
                        } else if (ThunderbackConfig.isVersion4()) {
                            if (isMultiWindowTaskDisplayArea) {
                                taskDisplayArea2 = preferTaskDisplayArea;
                            }
                        } else {
                            taskDisplayArea2 = taskDisplayAreaForMultiWindow3;
                        }
                    }
                    if (taskDisplayArea2 == null && activityRecord != null) {
                        z = DEBUG;
                        if (z) {
                            Slog.i(TAG, "TaskLaunchParamsModifier: pkgName = " + pkgName);
                        }
                        if (pkgName != null && pkgName.equals(activityRecord.packageName)) {
                            if (!ThunderbackConfig.isVersion4()) {
                                if (isMultiWindowTaskDisplayArea) {
                                    taskDisplayArea2 = preferTaskDisplayArea;
                                }
                            } else {
                                TaskDisplayArea taskDisplayAreaForMultiWindow4 = this.mSupervisor.mRootWindowContainer.getMultiDisplayArea();
                                Slog.i(TAG, "TaskLaunchParamsModifier: taskDisplayAreaForMultiWindow = " + taskDisplayAreaForMultiWindow4);
                                taskDisplayArea2 = taskDisplayAreaForMultiWindow4;
                            }
                        }
                        if (taskDisplayArea2 == null && ((source == null || 2 != source.getActivityType()) && (preferMultiDisplayArea = this.mSupervisor.mRootWindowContainer.getPreferMultiDisplayArea(activityRecord.packageName)) != null && preferMultiDisplayArea.isMultiWindow())) {
                            if (source == null && !ITranTaskLaunchParamsModifier.Instance().inMultiWindow(source) && "com.android.settings".equals(activityRecord.packageName)) {
                                taskDisplayArea2 = this.mSupervisor.mRootWindowContainer.getDefaultTaskDisplayArea();
                                if (z) {
                                    Slog.i(TAG, "TaskLaunchParamsModifier: preferMultiDisplayArea = " + taskDisplayArea2);
                                }
                            } else {
                                if (z) {
                                    Slog.i(TAG, "TaskLaunchParamsModifier: preferMultiDisplayArea = " + preferMultiDisplayArea);
                                }
                                taskDisplayArea2 = preferMultiDisplayArea;
                            }
                        }
                    }
                    if (taskDisplayArea2 == null && source != null && source.noDisplay) {
                        taskDisplayArea2 = source.mHandoverTaskDisplayArea;
                        if (taskDisplayArea2 == null) {
                            if (DEBUG) {
                                appendLog("display-area-from-no-display-source=" + taskDisplayArea2);
                            }
                        } else {
                            int displayId = source.mHandoverLaunchDisplayId;
                            DisplayContent dc3 = this.mSupervisor.mRootWindowContainer.getDisplayContent(displayId);
                            if (dc3 != null) {
                                taskDisplayArea2 = dc3.getDefaultTaskDisplayArea();
                                if (DEBUG) {
                                    appendLog("display-from-no-display-source=" + displayId);
                                }
                            }
                        }
                    }
                    Task rootPinnedStask = this.mSupervisor.mRootWindowContainer.getDefaultTaskDisplayArea().getRootPinnedTask();
                    if (taskDisplayArea2 == null && source != null) {
                        sourceDisplayArea = source.getDisplayArea();
                        if (!ITranTaskLaunchParamsModifier.Instance().inMultiWindow(source) && activityRecord != null) {
                            if (isInMultiWindowBlackList(activityRecord)) {
                                Slog.i(TAG, "TaskLaunchParamsModifier: source in multiwindow ,but start in blacklist");
                                taskDisplayArea2 = this.mSupervisor.mRootWindowContainer.getDefaultTaskDisplayArea();
                                if ((ThunderbackConfig.isVersion3() && 2 != activityRecord.getActivityType()) || (ThunderbackConfig.isVersion4() && source.packageName != null && !source.packageName.equals(activityRecord.packageName) && 2 != activityRecord.getActivityType())) {
                                    ITranTaskLaunchParamsModifier.Instance().showUnSupportMultiToast();
                                }
                            } else {
                                ActivityRecord pinnedTopActivity = rootPinnedStask != null ? rootPinnedStask.getTopNonFinishingActivity() : null;
                                String pinnedPkg = pinnedTopActivity != null ? pinnedTopActivity.packageName : null;
                                if (pinnedPkg != null && pinnedPkg.equals(activityRecord.packageName)) {
                                    if (DEBUG) {
                                        appendLog("display-area-from-source=null due to pip mode, pkg:" + activityRecord.packageName);
                                    }
                                    ITranTaskLaunchParamsModifier.Instance().showUnSupportMultiToast();
                                    taskDisplayArea2 = null;
                                } else {
                                    if (DEBUG) {
                                        appendLog("display-area-from-source=" + sourceDisplayArea);
                                    }
                                    taskDisplayArea2 = sourceDisplayArea;
                                }
                            }
                        } else {
                            if (DEBUG) {
                                appendLog("display-area-from-source=" + sourceDisplayArea);
                            }
                            taskDisplayArea2 = sourceDisplayArea;
                        }
                    }
                    rootTask = (taskDisplayArea2 == null || task == null) ? null : task.getRootTask();
                    if (rootTask != null) {
                        if (DEBUG) {
                            appendLog("display-from-task=" + rootTask.getDisplayId());
                        }
                        taskDisplayArea2 = rootTask.getDisplayArea();
                    }
                    if (taskDisplayArea2 == null && options != null && (dc = this.mSupervisor.mRootWindowContainer.getDisplayContent((callerDisplayId = options.getCallerDisplayId()))) != null) {
                        taskDisplayArea2 = dc.getDefaultTaskDisplayArea();
                        if (DEBUG) {
                            appendLog("display-from-caller=" + callerDisplayId);
                        }
                    }
                    if (taskDisplayArea2 != null) {
                        taskDisplayArea2 = currentParams.mPreferredTaskDisplayArea;
                    }
                    if (taskDisplayArea2 != null && !this.mSupervisor.mService.mSupportsMultiDisplay && taskDisplayArea2.getDisplayId() != 0) {
                        taskDisplayArea2 = this.mSupervisor.mRootWindowContainer.getDefaultTaskDisplayArea();
                    }
                    if (taskDisplayArea2 != null && activityRecord.isActivityTypeHome() && !this.mSupervisor.mRootWindowContainer.canStartHomeOnDisplayArea(activityRecord.info, taskDisplayArea2, false)) {
                        taskDisplayArea2 = this.mSupervisor.mRootWindowContainer.getDefaultTaskDisplayArea();
                    }
                    isUnsupportMultiWin = false;
                    if (!ThunderbackConfig.isVersion4()) {
                        if (rootPinnedStask != null && activityRecord != null && taskDisplayArea2 == preferTaskDisplayArea && isMultiWindowTaskDisplayArea) {
                            isUnsupportMultiWin = true;
                        }
                    } else {
                        TaskDisplayArea taskDisplayAreaForMultiWindow5 = this.mSupervisor.mRootWindowContainer.getMultiDisplayArea();
                        if (rootPinnedStask != null && taskDisplayArea2 == taskDisplayAreaForMultiWindow5 && activityRecord != null) {
                            isUnsupportMultiWin = true;
                        }
                    }
                    if (isUnsupportMultiWin) {
                        String taskName = rootPinnedStask.realActivity != null ? rootPinnedStask.realActivity.getPackageName() : null;
                        if (taskName != null && taskName.equals(activityRecord.packageName)) {
                            if (DEBUG) {
                                Slog.i(TAG, "TaskLaunchParamsModifier: taskName : " + taskName + " in PIP mode, start in defaultDisplayArea");
                            }
                            ITranTaskLaunchParamsModifier.Instance().showUnSupportMultiToast();
                            taskDisplayArea2 = null;
                        }
                    }
                    return taskDisplayArea2 == null ? taskDisplayArea2 : getFallbackDisplayAreaForActivity(activityRecord, request);
                }
            }
        }
        taskDisplayArea2 = taskDisplayArea;
        if (taskDisplayArea2 == null) {
            TaskDisplayArea taskDisplayAreaForMultiWindow32 = null;
            if (ThunderbackConfig.isVersion3()) {
            }
            if (!isInMultiWindowBlackList(activityRecord)) {
            }
        }
        if (taskDisplayArea2 == null) {
            z = DEBUG;
            if (z) {
            }
            if (pkgName != null) {
                if (!ThunderbackConfig.isVersion4()) {
                }
            }
            if (taskDisplayArea2 == null) {
                if (source == null) {
                }
                if (z) {
                }
                taskDisplayArea2 = preferMultiDisplayArea;
            }
        }
        if (taskDisplayArea2 == null) {
            taskDisplayArea2 = source.mHandoverTaskDisplayArea;
            if (taskDisplayArea2 == null) {
            }
        }
        Task rootPinnedStask2 = this.mSupervisor.mRootWindowContainer.getDefaultTaskDisplayArea().getRootPinnedTask();
        if (taskDisplayArea2 == null) {
            sourceDisplayArea = source.getDisplayArea();
            if (!ITranTaskLaunchParamsModifier.Instance().inMultiWindow(source)) {
            }
            if (DEBUG) {
            }
            taskDisplayArea2 = sourceDisplayArea;
        }
        if (taskDisplayArea2 == null) {
        }
        if (rootTask != null) {
        }
        if (taskDisplayArea2 == null) {
            taskDisplayArea2 = dc.getDefaultTaskDisplayArea();
            if (DEBUG) {
            }
        }
        if (taskDisplayArea2 != null) {
        }
        if (taskDisplayArea2 != null) {
            taskDisplayArea2 = this.mSupervisor.mRootWindowContainer.getDefaultTaskDisplayArea();
        }
        if (taskDisplayArea2 != null) {
            taskDisplayArea2 = this.mSupervisor.mRootWindowContainer.getDefaultTaskDisplayArea();
        }
        isUnsupportMultiWin = false;
        if (!ThunderbackConfig.isVersion4()) {
        }
        if (isUnsupportMultiWin) {
        }
        if (taskDisplayArea2 == null) {
        }
    }

    private TaskDisplayArea getFallbackDisplayAreaForActivity(ActivityRecord activityRecord, ActivityStarter.Request request) {
        TaskDisplayArea displayAreaForRecord;
        WindowProcessController controllerFromRequest;
        WindowProcessController controllerFromLaunchingRecord = this.mSupervisor.mService.getProcessController(activityRecord.launchedFromPid, activityRecord.launchedFromUid);
        TaskDisplayArea displayAreaFromSourceProcess = null;
        TaskDisplayArea displayAreaForLaunchingRecord = controllerFromLaunchingRecord == null ? null : controllerFromLaunchingRecord.getTopActivityDisplayArea();
        if (displayAreaForLaunchingRecord != null) {
            return displayAreaForLaunchingRecord;
        }
        WindowProcessController controllerFromProcess = this.mSupervisor.mService.getProcessController(activityRecord.getProcessName(), activityRecord.getUid());
        if (controllerFromProcess == null) {
            displayAreaForRecord = null;
        } else {
            displayAreaForRecord = controllerFromProcess.getTopActivityDisplayArea();
        }
        if (displayAreaForRecord != null) {
            return displayAreaForRecord;
        }
        if (request == null) {
            controllerFromRequest = null;
        } else {
            controllerFromRequest = this.mSupervisor.mService.getProcessController(request.realCallingPid, request.realCallingUid);
        }
        if (controllerFromRequest != null) {
            displayAreaFromSourceProcess = controllerFromRequest.getTopActivityDisplayArea();
        }
        if (displayAreaFromSourceProcess != null) {
            return displayAreaFromSourceProcess;
        }
        return this.mSupervisor.mRootWindowContainer.getDefaultTaskDisplayArea();
    }

    private boolean canInheritWindowingModeFromSource(DisplayContent display, ActivityRecord source) {
        if (source == null || display.inFreeformWindowingMode()) {
            return false;
        }
        int sourceWindowingMode = source.getWindowingMode();
        if ((sourceWindowingMode != 1 && sourceWindowingMode != 5) || display.getDisplayId() != source.getDisplayId()) {
            return false;
        }
        return true;
    }

    private boolean canApplyFreeformWindowPolicy(DisplayContent display, int launchMode) {
        return this.mSupervisor.mService.mSupportsFreeformWindowManagement && (display.inFreeformWindowingMode() || launchMode == 5);
    }

    private boolean canApplyPipWindowPolicy(int launchMode) {
        return this.mSupervisor.mService.mSupportsPictureInPicture && launchMode == 2;
    }

    private void getLayoutBounds(TaskDisplayArea displayArea, ActivityRecord root, ActivityInfo.WindowLayout windowLayout, Rect inOutBounds) {
        int width;
        int height;
        float fractionOfHorizontalOffset;
        float fractionOfVerticalOffset;
        int verticalGravity = windowLayout.gravity & 112;
        int horizontalGravity = windowLayout.gravity & 7;
        if (!windowLayout.hasSpecifiedSize() && verticalGravity == 0 && horizontalGravity == 0) {
            inOutBounds.setEmpty();
            return;
        }
        Rect stableBounds = this.mTmpStableBounds;
        displayArea.getStableRect(stableBounds);
        int defaultWidth = stableBounds.width();
        int defaultHeight = stableBounds.height();
        if (!windowLayout.hasSpecifiedSize()) {
            if (!inOutBounds.isEmpty()) {
                width = inOutBounds.width();
                height = inOutBounds.height();
            } else {
                getTaskBounds(root, displayArea, windowLayout, 5, false, inOutBounds);
                width = inOutBounds.width();
                height = inOutBounds.height();
            }
        } else {
            width = defaultWidth;
            if (windowLayout.width > 0 && windowLayout.width < defaultWidth) {
                width = windowLayout.width;
            } else if (windowLayout.widthFraction > 0.0f && windowLayout.widthFraction < 1.0f) {
                width = (int) (width * windowLayout.widthFraction);
            }
            height = defaultHeight;
            if (windowLayout.height > 0 && windowLayout.height < defaultHeight) {
                height = windowLayout.height;
            } else if (windowLayout.heightFraction > 0.0f && windowLayout.heightFraction < 1.0f) {
                height = (int) (height * windowLayout.heightFraction);
            }
        }
        switch (horizontalGravity) {
            case 3:
                fractionOfHorizontalOffset = 0.0f;
                break;
            case 4:
            default:
                fractionOfHorizontalOffset = 0.5f;
                break;
            case 5:
                fractionOfHorizontalOffset = 1.0f;
                break;
        }
        switch (verticalGravity) {
            case 48:
                fractionOfVerticalOffset = 0.0f;
                break;
            case 80:
                fractionOfVerticalOffset = 1.0f;
                break;
            default:
                fractionOfVerticalOffset = 0.5f;
                break;
        }
        inOutBounds.set(0, 0, width, height);
        inOutBounds.offset(stableBounds.left, stableBounds.top);
        int xOffset = (int) ((defaultWidth - width) * fractionOfHorizontalOffset);
        int yOffset = (int) ((defaultHeight - height) * fractionOfVerticalOffset);
        inOutBounds.offset(xOffset, yOffset);
    }

    private boolean shouldLaunchUnresizableAppInFreeform(ActivityRecord activity, TaskDisplayArea displayArea, ActivityOptions options) {
        if ((options == null || options.getLaunchWindowingMode() != 1) && activity.supportsFreeformInDisplayArea(displayArea) && !activity.isResizeable()) {
            int displayOrientation = orientationFromBounds(displayArea.getBounds());
            int activityOrientation = resolveOrientation(activity, displayArea, displayArea.getBounds());
            return displayArea.getWindowingMode() == 5 && displayOrientation != activityOrientation;
        }
        return false;
    }

    private int resolveOrientation(ActivityRecord activity) {
        int orientation = activity.info.screenOrientation;
        switch (orientation) {
            case 0:
            case 6:
            case 8:
            case 11:
                if (DEBUG) {
                    appendLog("activity-requested-landscape");
                }
                return 0;
            case 1:
            case 7:
            case 9:
            case 12:
                if (DEBUG) {
                    appendLog("activity-requested-portrait");
                }
                return 1;
            case 2:
            case 3:
            case 4:
            case 10:
            case 13:
            default:
                return -1;
            case 5:
            case 14:
                return 14;
        }
    }

    private void cascadeBounds(Rect srcBounds, TaskDisplayArea displayArea, Rect outBounds) {
        outBounds.set(srcBounds);
        float density = displayArea.getConfiguration().densityDpi / 160.0f;
        int defaultOffset = (int) ((75.0f * density) + 0.5f);
        displayArea.getBounds(this.mTmpBounds);
        int dx = Math.min(defaultOffset, Math.max(0, this.mTmpBounds.right - srcBounds.right));
        int dy = Math.min(defaultOffset, Math.max(0, this.mTmpBounds.bottom - srcBounds.bottom));
        outBounds.offset(dx, dy);
    }

    private void getTaskBounds(ActivityRecord root, TaskDisplayArea displayArea, ActivityInfo.WindowLayout layout, int resolvedMode, boolean hasInitialBounds, Rect inOutBounds) {
        if (resolvedMode == 1) {
            inOutBounds.setEmpty();
            if (DEBUG) {
                appendLog("maximized-bounds");
            }
        } else if (resolvedMode != 5) {
            if (DEBUG) {
                appendLog("skip-bounds-" + WindowConfiguration.windowingModeToString(resolvedMode));
            }
        } else {
            int orientation = resolveOrientation(root, displayArea, inOutBounds);
            if (orientation != 1 && orientation != 0) {
                throw new IllegalStateException("Orientation must be one of portrait or landscape, but it's " + ActivityInfo.screenOrientationToString(orientation));
            }
            getDefaultFreeformSize(root.info, displayArea, layout, orientation, this.mTmpBounds);
            if (hasInitialBounds || sizeMatches(inOutBounds, this.mTmpBounds)) {
                if (orientation == orientationFromBounds(inOutBounds)) {
                    if (DEBUG) {
                        appendLog("freeform-size-orientation-match=" + inOutBounds);
                    }
                } else {
                    centerBounds(displayArea, inOutBounds.height(), inOutBounds.width(), inOutBounds);
                    if (DEBUG) {
                        appendLog("freeform-orientation-mismatch=" + inOutBounds);
                    }
                }
            } else {
                centerBounds(displayArea, this.mTmpBounds.width(), this.mTmpBounds.height(), inOutBounds);
                adjustBoundsToFitInDisplayArea(displayArea, inOutBounds);
                if (DEBUG) {
                    appendLog("freeform-size-mismatch=" + inOutBounds);
                }
            }
            adjustBoundsToAvoidConflictInDisplayArea(displayArea, inOutBounds);
        }
    }

    private int convertOrientationToScreenOrientation(int orientation) {
        switch (orientation) {
            case 1:
                return 1;
            case 2:
                return 0;
            default:
                return -1;
        }
    }

    private int resolveOrientation(ActivityRecord root, TaskDisplayArea displayArea, Rect bounds) {
        int orientationFromBounds;
        int orientation = resolveOrientation(root);
        if (orientation == 14) {
            if (bounds.isEmpty()) {
                orientationFromBounds = convertOrientationToScreenOrientation(displayArea.getConfiguration().orientation);
            } else {
                orientationFromBounds = orientationFromBounds(bounds);
            }
            orientation = orientationFromBounds;
            if (DEBUG) {
                appendLog(bounds.isEmpty() ? "locked-orientation-from-display=" + orientation : "locked-orientation-from-bounds=" + bounds);
            }
        }
        if (orientation == -1) {
            orientation = bounds.isEmpty() ? 1 : orientationFromBounds(bounds);
            if (DEBUG) {
                appendLog(bounds.isEmpty() ? "default-portrait" : "orientation-from-bounds=" + bounds);
            }
        }
        return orientation;
    }

    private void getDefaultFreeformSize(ActivityInfo info, TaskDisplayArea displayArea, ActivityInfo.WindowLayout layout, int orientation, Rect bounds) {
        int layoutMinWidth;
        int adjHeight;
        Rect stableBounds = this.mTmpStableBounds;
        displayArea.getStableRect(stableBounds);
        int portraitHeight = Math.min(stableBounds.width(), stableBounds.height());
        int otherDimension = Math.max(stableBounds.width(), stableBounds.height());
        int portraitWidth = (portraitHeight * portraitHeight) / otherDimension;
        int defaultWidth = orientation == 0 ? portraitHeight : portraitWidth;
        int defaultHeight = orientation == 0 ? portraitWidth : portraitHeight;
        float density = displayArea.getConfiguration().densityDpi / 160.0f;
        int phonePortraitWidth = (int) ((412.0f * density) + 0.5f);
        int phonePortraitHeight = (int) ((732.0f * density) + 0.5f);
        int phoneWidth = orientation == 0 ? phonePortraitHeight : phonePortraitWidth;
        int phoneHeight = orientation == 0 ? phonePortraitWidth : phonePortraitHeight;
        int i = -1;
        if (layout != null) {
            layoutMinWidth = layout.minWidth;
        } else {
            layoutMinWidth = -1;
        }
        if (layout != null) {
            i = layout.minHeight;
        }
        int layoutMinHeight = i;
        float minAspectRatio = info.getMinAspectRatio(orientation);
        float maxAspectRatio = info.getMaxAspectRatio();
        int width = Math.min(defaultWidth, Math.max(phoneWidth, layoutMinWidth));
        int height = Math.min(defaultHeight, Math.max(phoneHeight, layoutMinHeight));
        int layoutMinHeight2 = Math.max(width, height);
        int phoneHeight2 = Math.min(width, height);
        float aspectRatio = layoutMinHeight2 / phoneHeight2;
        int adjWidth = width;
        if (minAspectRatio < 1.0f || aspectRatio >= minAspectRatio) {
            adjHeight = height;
            if (maxAspectRatio >= 1.0f && aspectRatio > maxAspectRatio) {
                if (orientation == 0) {
                    adjHeight = (int) ((adjWidth / maxAspectRatio) + 0.5f);
                } else {
                    adjWidth = (int) ((adjHeight / maxAspectRatio) + 0.5f);
                }
            }
        } else if (orientation == 0) {
            adjHeight = (int) ((adjWidth / minAspectRatio) + 0.5f);
        } else {
            adjHeight = height;
            adjWidth = (int) ((adjHeight / minAspectRatio) + 0.5f);
        }
        bounds.set(0, 0, adjWidth, adjHeight);
        bounds.offset(stableBounds.left, stableBounds.top);
    }

    private void centerBounds(TaskDisplayArea displayArea, int width, int height, Rect inOutBounds) {
        if (inOutBounds.isEmpty()) {
            displayArea.getStableRect(inOutBounds);
        }
        int left = inOutBounds.centerX() - (width / 2);
        int top = inOutBounds.centerY() - (height / 2);
        inOutBounds.set(left, top, left + width, top + height);
    }

    private void adjustBoundsToFitInDisplayArea(TaskDisplayArea displayArea, Rect inOutBounds) {
        int left;
        int dx;
        int dy;
        Rect stableBounds = this.mTmpStableBounds;
        displayArea.getStableRect(stableBounds);
        if (stableBounds.width() < inOutBounds.width() || stableBounds.height() < inOutBounds.height()) {
            int layoutDirection = this.mSupervisor.mRootWindowContainer.getConfiguration().getLayoutDirection();
            if (layoutDirection == 1) {
                left = (stableBounds.right - inOutBounds.right) + inOutBounds.left;
            } else {
                left = stableBounds.left;
            }
            inOutBounds.offsetTo(left, stableBounds.top);
            return;
        }
        if (inOutBounds.right > stableBounds.right) {
            dx = stableBounds.right - inOutBounds.right;
        } else {
            int dx2 = inOutBounds.left;
            if (dx2 < stableBounds.left) {
                dx = stableBounds.left - inOutBounds.left;
            } else {
                dx = 0;
            }
        }
        if (inOutBounds.top < stableBounds.top) {
            dy = stableBounds.top - inOutBounds.top;
        } else {
            int dy2 = inOutBounds.bottom;
            if (dy2 > stableBounds.bottom) {
                dy = stableBounds.bottom - inOutBounds.bottom;
            } else {
                dy = 0;
            }
        }
        inOutBounds.offset(dx, dy);
    }

    private void adjustBoundsToAvoidConflictInDisplayArea(TaskDisplayArea displayArea, Rect inOutBounds) {
        final List<Rect> taskBoundsToCheck = new ArrayList<>();
        displayArea.forAllRootTasks(new Consumer() { // from class: com.android.server.wm.TaskLaunchParamsModifier$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                TaskLaunchParamsModifier.lambda$adjustBoundsToAvoidConflictInDisplayArea$1(taskBoundsToCheck, (Task) obj);
            }
        }, false);
        adjustBoundsToAvoidConflict(displayArea.getBounds(), taskBoundsToCheck, inOutBounds);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$adjustBoundsToAvoidConflictInDisplayArea$1(List taskBoundsToCheck, Task task) {
        if (!task.inFreeformWindowingMode()) {
            return;
        }
        for (int j = 0; j < task.getChildCount(); j++) {
            taskBoundsToCheck.add(task.getChildAt(j).getBounds());
        }
    }

    void adjustBoundsToAvoidConflict(Rect displayAreaBounds, List<Rect> taskBoundsToCheck, Rect inOutBounds) {
        int[] iArr;
        if (!displayAreaBounds.contains(inOutBounds) || !boundsConflict(taskBoundsToCheck, inOutBounds)) {
            return;
        }
        calculateCandidateShiftDirections(displayAreaBounds, inOutBounds);
        for (int direction : this.mTmpDirections) {
            if (direction != 0) {
                this.mTmpBounds.set(inOutBounds);
                while (boundsConflict(taskBoundsToCheck, this.mTmpBounds) && displayAreaBounds.contains(this.mTmpBounds)) {
                    shiftBounds(direction, displayAreaBounds, this.mTmpBounds);
                }
                if (!boundsConflict(taskBoundsToCheck, this.mTmpBounds) && displayAreaBounds.contains(this.mTmpBounds)) {
                    inOutBounds.set(this.mTmpBounds);
                    if (DEBUG) {
                        appendLog("avoid-bounds-conflict=" + inOutBounds);
                        return;
                    }
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void calculateCandidateShiftDirections(Rect availableBounds, Rect initialBounds) {
        int i = 0;
        while (true) {
            int[] iArr = this.mTmpDirections;
            if (i >= iArr.length) {
                break;
            }
            iArr[i] = 0;
            i++;
        }
        int i2 = availableBounds.left;
        int oneThirdWidth = ((i2 * 2) + availableBounds.right) / 3;
        int twoThirdWidth = (availableBounds.left + (availableBounds.right * 2)) / 3;
        int centerX = initialBounds.centerX();
        if (centerX < oneThirdWidth) {
            this.mTmpDirections[0] = 5;
        } else if (centerX > twoThirdWidth) {
            this.mTmpDirections[0] = 3;
        } else {
            int oneThirdHeight = ((availableBounds.top * 2) + availableBounds.bottom) / 3;
            int twoThirdHeight = (availableBounds.top + (availableBounds.bottom * 2)) / 3;
            int centerY = initialBounds.centerY();
            if (centerY < oneThirdHeight || centerY > twoThirdHeight) {
                int[] iArr2 = this.mTmpDirections;
                iArr2[0] = 5;
                iArr2[1] = 3;
                return;
            }
            int[] iArr3 = this.mTmpDirections;
            iArr3[0] = 85;
            iArr3[1] = 51;
        }
    }

    private boolean boundsConflict(List<Rect> taskBoundsToCheck, Rect candidateBounds) {
        Iterator<Rect> it = taskBoundsToCheck.iterator();
        while (true) {
            if (!it.hasNext()) {
                return false;
            }
            Rect taskBounds = it.next();
            boolean leftClose = Math.abs(taskBounds.left - candidateBounds.left) < 4;
            boolean topClose = Math.abs(taskBounds.top - candidateBounds.top) < 4;
            boolean rightClose = Math.abs(taskBounds.right - candidateBounds.right) < 4;
            boolean bottomClose = Math.abs(taskBounds.bottom - candidateBounds.bottom) < 4;
            if ((!leftClose || !topClose) && ((!leftClose || !bottomClose) && ((!rightClose || !topClose) && (!rightClose || !bottomClose)))) {
            }
        }
        return true;
    }

    private void shiftBounds(int direction, Rect availableRect, Rect inOutBounds) {
        int horizontalOffset;
        int verticalOffset;
        switch (direction & 7) {
            case 3:
                horizontalOffset = -Math.max(1, availableRect.width() / 16);
                break;
            case 4:
            default:
                horizontalOffset = 0;
                break;
            case 5:
                int horizontalOffset2 = availableRect.width();
                horizontalOffset = Math.max(1, horizontalOffset2 / 16);
                break;
        }
        switch (direction & 112) {
            case 48:
                verticalOffset = -Math.max(1, availableRect.height() / 16);
                break;
            case 80:
                verticalOffset = Math.max(1, availableRect.height() / 16);
                break;
            default:
                verticalOffset = 0;
                break;
        }
        inOutBounds.offset(horizontalOffset, verticalOffset);
    }

    private void initLogBuilder(Task task, ActivityRecord activity) {
        if (DEBUG) {
            this.mLogBuilder = new StringBuilder("TaskLaunchParamsModifier:task=" + task + " activity=" + activity);
        }
    }

    private void appendLog(String log) {
        if (DEBUG) {
            this.mLogBuilder.append(" ").append(log);
        }
    }

    private void outputLog() {
        if (DEBUG) {
            Slog.d(TAG, this.mLogBuilder.toString());
        }
    }

    private static int orientationFromBounds(Rect bounds) {
        return bounds.width() > bounds.height() ? 0 : 1;
    }

    private static boolean sizeMatches(Rect left, Rect right) {
        return Math.abs(right.width() - left.width()) < 2 && Math.abs(right.height() - left.height()) < 2;
    }
}
