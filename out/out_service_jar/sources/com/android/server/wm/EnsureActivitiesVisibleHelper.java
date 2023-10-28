package com.android.server.wm;

import android.util.Slog;
import com.android.server.wm.ActivityRecord;
import java.util.ArrayList;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class EnsureActivitiesVisibleHelper {
    private boolean mAboveTop;
    private boolean mBehindFullyOccludedContainer;
    private int mConfigChanges;
    private boolean mContainerShouldBeVisible;
    private boolean mNotifyClients;
    private boolean mPreserveWindows;
    private ActivityRecord mStarting;
    private final TaskFragment mTaskFragment;
    private ActivityRecord mTop;

    /* JADX INFO: Access modifiers changed from: package-private */
    public EnsureActivitiesVisibleHelper(TaskFragment container) {
        this.mTaskFragment = container;
    }

    void reset(ActivityRecord starting, int configChanges, boolean preserveWindows, boolean notifyClients) {
        this.mStarting = starting;
        ActivityRecord activityRecord = this.mTaskFragment.topRunningActivity();
        this.mTop = activityRecord;
        this.mAboveTop = activityRecord != null;
        boolean shouldBeVisible = this.mTaskFragment.shouldBeVisible(this.mStarting);
        this.mContainerShouldBeVisible = shouldBeVisible;
        this.mBehindFullyOccludedContainer = !shouldBeVisible;
        this.mConfigChanges = configChanges;
        this.mPreserveWindows = preserveWindows;
        this.mNotifyClients = notifyClients;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void process(ActivityRecord starting, int configChanges, boolean preserveWindows, boolean notifyClients) {
        reset(starting, configChanges, preserveWindows, notifyClients);
        if (ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v(Task.TAG_VISIBILITY, "ensureActivitiesVisible behind " + this.mTop + " configChanges=0x" + Integer.toHexString(configChanges));
        }
        if (this.mTop != null && this.mTaskFragment.asTask() != null) {
            this.mTaskFragment.asTask().checkTranslucentActivityWaiting(this.mTop);
        }
        ActivityRecord activityRecord = this.mTop;
        boolean resumeTopActivity = activityRecord != null && !activityRecord.mLaunchTaskBehind && this.mTaskFragment.canBeResumed(starting) && (starting == null || !starting.isDescendantOf(this.mTaskFragment));
        ArrayList<TaskFragment> adjacentTaskFragments = null;
        for (int i = this.mTaskFragment.mChildren.size() - 1; i >= 0; i--) {
            WindowContainer child = (WindowContainer) this.mTaskFragment.mChildren.get(i);
            TaskFragment childTaskFragment = child.asTaskFragment();
            if (childTaskFragment != null && childTaskFragment.getTopNonFinishingActivity() != null) {
                childTaskFragment.updateActivityVisibilities(starting, configChanges, preserveWindows, notifyClients);
                this.mBehindFullyOccludedContainer |= childTaskFragment.getBounds().equals(this.mTaskFragment.getBounds()) && !childTaskFragment.isTranslucent(starting);
                if (this.mAboveTop && this.mTop.getTaskFragment() == childTaskFragment) {
                    this.mAboveTop = false;
                }
                if (!this.mBehindFullyOccludedContainer) {
                    if (adjacentTaskFragments != null && adjacentTaskFragments.contains(childTaskFragment)) {
                        if (!childTaskFragment.isTranslucent(starting) && !childTaskFragment.getAdjacentTaskFragment().isTranslucent(starting)) {
                            this.mBehindFullyOccludedContainer = true;
                        }
                    } else {
                        TaskFragment adjacentTaskFrag = childTaskFragment.getAdjacentTaskFragment();
                        if (adjacentTaskFrag != null) {
                            if (adjacentTaskFragments == null) {
                                adjacentTaskFragments = new ArrayList<>();
                            }
                            adjacentTaskFragments.add(adjacentTaskFrag);
                        }
                    }
                }
            } else if (child.asActivityRecord() != null) {
                setActivityVisibilityState(child.asActivityRecord(), starting, resumeTopActivity);
            }
        }
    }

    private void setActivityVisibilityState(ActivityRecord r, ActivityRecord starting, boolean resumeTopActivity) {
        boolean isTop = r == this.mTop;
        if (this.mAboveTop && !isTop) {
            r.makeInvisible();
            return;
        }
        this.mAboveTop = false;
        r.updateVisibilityIgnoringKeyguard(this.mBehindFullyOccludedContainer);
        boolean reallyVisible = r.shouldBeVisibleUnchecked();
        if (r.visibleIgnoringKeyguard) {
            if (r.occludesParent()) {
                if (ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY) {
                    Slog.v(Task.TAG_VISIBILITY, "Fullscreen: at " + r + " containerVisible=" + this.mContainerShouldBeVisible + " behindFullyOccluded=" + this.mBehindFullyOccludedContainer);
                }
                this.mBehindFullyOccludedContainer = true;
            } else {
                this.mBehindFullyOccludedContainer = false;
            }
        } else if (r.isState(ActivityRecord.State.INITIALIZING)) {
            r.cancelInitializing();
        }
        if (reallyVisible) {
            if (r.finishing) {
                return;
            }
            if (ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v(Task.TAG_VISIBILITY, "Make visible? " + r + " finishing=" + r.finishing + " state=" + r.getState());
            }
            if (r != this.mStarting && this.mNotifyClients) {
                r.ensureActivityConfiguration(0, this.mPreserveWindows, true);
            }
            if (!r.attachedToProcess()) {
                makeVisibleAndRestartIfNeeded(this.mStarting, this.mConfigChanges, isTop, resumeTopActivity && isTop, r);
            } else if (r.mVisibleRequested) {
                if (ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY) {
                    Slog.v(Task.TAG_VISIBILITY, "Skipping: already visible at " + r);
                }
                if (r.mClientVisibilityDeferred && this.mNotifyClients) {
                    r.makeActiveIfNeeded(r.mClientVisibilityDeferred ? null : starting);
                    r.mClientVisibilityDeferred = false;
                }
                r.handleAlreadyVisible();
                if (this.mNotifyClients) {
                    r.makeActiveIfNeeded(this.mStarting);
                }
            } else {
                r.makeVisibleIfNeeded(this.mStarting, this.mNotifyClients);
            }
            this.mConfigChanges |= r.configChangeFlags;
        } else {
            if (ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v(Task.TAG_VISIBILITY, "Make invisible? " + r + " finishing=" + r.finishing + " state=" + r.getState() + " containerShouldBeVisible=" + this.mContainerShouldBeVisible + " behindFullyOccludedContainer=" + this.mBehindFullyOccludedContainer + " mLaunchTaskBehind=" + r.mLaunchTaskBehind);
            }
            r.makeInvisible();
        }
        if (!this.mBehindFullyOccludedContainer && this.mTaskFragment.isActivityTypeHome() && r.isRootOfTask()) {
            if (ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v(Task.TAG_VISIBILITY, "Home task: at " + this.mTaskFragment + " containerShouldBeVisible=" + this.mContainerShouldBeVisible + " behindOccludedParentContainer=" + this.mBehindFullyOccludedContainer);
            }
            this.mBehindFullyOccludedContainer = true;
        }
    }

    private void makeVisibleAndRestartIfNeeded(ActivityRecord starting, int configChanges, boolean isTop, boolean andResume, ActivityRecord r) {
        if (!isTop && r.mVisibleRequested && !r.isState(ActivityRecord.State.INITIALIZING)) {
            Slog.d(Task.TAG_VISIBILITY, "is not top but request is true, remove this in displaycontent.opening list r = " + r + "; r.isVisible = " + r.isVisible());
            r.removeOpeningApps();
            return;
        }
        if (ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v(Task.TAG_VISIBILITY, "Start and freeze screen for " + r);
        }
        if (r != starting) {
            r.startFreezingScreenLocked(configChanges);
        }
        if (!r.mVisibleRequested || r.mLaunchTaskBehind) {
            if (ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v(Task.TAG_VISIBILITY, "Starting and making visible: " + r);
            }
            r.setVisibility(true);
        }
        if (r != starting) {
            this.mTaskFragment.mTaskSupervisor.startSpecificActivity(r, andResume, true);
        }
    }
}
