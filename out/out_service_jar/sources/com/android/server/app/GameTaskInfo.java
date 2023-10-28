package com.android.server.app;

import android.content.ComponentName;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class GameTaskInfo {
    final ComponentName mComponentName;
    final boolean mIsGameTask;
    final int mTaskId;

    /* JADX INFO: Access modifiers changed from: package-private */
    public GameTaskInfo(int taskId, boolean isGameTask, ComponentName componentName) {
        this.mTaskId = taskId;
        this.mIsGameTask = isGameTask;
        this.mComponentName = componentName;
    }

    public String toString() {
        return "GameTaskInfo{mTaskId=" + this.mTaskId + ", mIsGameTask=" + this.mIsGameTask + ", mComponentName=" + this.mComponentName + '}';
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof GameTaskInfo) {
            GameTaskInfo that = (GameTaskInfo) o;
            return this.mTaskId == that.mTaskId && this.mIsGameTask == that.mIsGameTask && this.mComponentName.equals(that.mComponentName);
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mTaskId), Boolean.valueOf(this.mIsGameTask), this.mComponentName);
    }
}
