package com.android.server.wm;

import java.lang.ref.WeakReference;
/* loaded from: classes2.dex */
public class TranTaskProxyExt {
    private final WeakReference<Object> mAncestor;

    public TranTaskProxyExt(Object ancestor) {
        this.mAncestor = new WeakReference<>(ancestor);
    }

    private boolean assertAncestor() {
        Object ancestor = this.mAncestor.get();
        if (ancestor != null && (ancestor instanceof Task)) {
            return true;
        }
        return false;
    }

    public boolean onMarkAdjacentTask() {
        if (assertAncestor()) {
            Task task = (Task) this.mAncestor.get();
            return task.markAdjacentTask();
        }
        return false;
    }

    public Object onGetAdjacentTask() {
        if (assertAncestor()) {
            Task task = (Task) this.mAncestor.get();
            return task.getAdjacentTask();
        }
        return null;
    }

    public void onSetAdjacentTaskForThisTask(Object adjacentTask) {
        if (assertAncestor()) {
            Task task = (Task) this.mAncestor.get();
            if ((adjacentTask instanceof Task) || adjacentTask == null) {
                task.setAdjacentTaskForThisTask((Task) adjacentTask);
            }
        }
    }

    public void onTouchAdjacentTaskActiveTime() {
        if (assertAncestor()) {
            Task task = (Task) this.mAncestor.get();
            task.touchAdjacentTaskActiveTime();
        }
    }
}
