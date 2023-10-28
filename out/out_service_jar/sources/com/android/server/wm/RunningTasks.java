package com.android.server.wm;

import android.app.ActivityManager;
import android.os.UserHandle;
import android.util.ArraySet;
import com.android.internal.util.function.pooled.PooledConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.BiConsumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class RunningTasks {
    static final int FLAG_ALLOWED = 2;
    static final int FLAG_CROSS_USERS = 4;
    static final int FLAG_FILTER_ONLY_VISIBLE_RECENTS = 1;
    static final int FLAG_KEEP_INTENT_EXTRA = 8;
    private static final Comparator<Task> LAST_ACTIVE_TIME_COMPARATOR = new Comparator() { // from class: com.android.server.wm.RunningTasks$$ExternalSyntheticLambda1
        @Override // java.util.Comparator
        public final int compare(Object obj, Object obj2) {
            return RunningTasks.lambda$static$0((Task) obj, (Task) obj2);
        }
    };
    private boolean mAllowed;
    private int mCallingUid;
    private boolean mCrossUser;
    private boolean mFilterOnlyVisibleRecents;
    private boolean mKeepIntentExtra;
    private ArraySet<Integer> mProfileIds;
    private RecentTasks mRecentTasks;
    private final TreeSet<Task> mTmpSortedSet = new TreeSet<>(LAST_ACTIVE_TIME_COMPARATOR);
    private int mUserId;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ int lambda$static$0(Task o1, Task o2) {
        if (o1.lastActiveTime == o2.lastActiveTime) {
            return Integer.signum(o2.mTaskId - o1.mTaskId);
        }
        return Long.signum(o2.lastActiveTime - o1.lastActiveTime);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getTasks(int maxNum, List<ActivityManager.RunningTaskInfo> list, int flags, RootWindowContainer root, int callingUid, ArraySet<Integer> profileIds) {
        if (maxNum <= 0) {
            return;
        }
        this.mTmpSortedSet.clear();
        this.mCallingUid = callingUid;
        this.mUserId = UserHandle.getUserId(callingUid);
        this.mCrossUser = (flags & 4) == 4;
        this.mProfileIds = profileIds;
        this.mAllowed = (flags & 2) == 2;
        this.mFilterOnlyVisibleRecents = (flags & 1) == 1;
        this.mRecentTasks = root.mService.getRecentTasks();
        this.mKeepIntentExtra = (flags & 8) == 8;
        PooledConsumer c = PooledLambda.obtainConsumer(new BiConsumer() { // from class: com.android.server.wm.RunningTasks$$ExternalSyntheticLambda0
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((RunningTasks) obj).processTask((Task) obj2);
            }
        }, this, PooledLambda.__(Task.class));
        root.forAllLeafTasks(c, false);
        c.recycle();
        Iterator<Task> iter = this.mTmpSortedSet.iterator();
        while (iter.hasNext() && maxNum != 0) {
            Task task = iter.next();
            list.add(createRunningTaskInfo(task));
            maxNum--;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void processTask(Task task) {
        if (task.getTopNonFinishingActivity() == null) {
            return;
        }
        if (task.effectiveUid != this.mCallingUid && ((task.mUserId != this.mUserId && !this.mCrossUser && !this.mProfileIds.contains(Integer.valueOf(task.mUserId))) || !this.mAllowed)) {
            return;
        }
        if (this.mFilterOnlyVisibleRecents && task.getActivityType() != 2 && task.getActivityType() != 3 && !this.mRecentTasks.isVisibleRecentTask(task)) {
            return;
        }
        if (task.isVisible()) {
            task.touchActiveTime();
            if (!task.isFocused()) {
                task.lastActiveTime -= this.mTmpSortedSet.size();
            }
        }
        this.mTmpSortedSet.add(task);
    }

    private ActivityManager.RunningTaskInfo createRunningTaskInfo(Task task) {
        ActivityManager.RunningTaskInfo rti = new ActivityManager.RunningTaskInfo();
        task.fillTaskInfo(rti, !this.mKeepIntentExtra);
        rti.id = rti.taskId;
        if (!this.mAllowed) {
            Task.trimIneffectiveInfo(task, rti);
        }
        return rti;
    }
}
