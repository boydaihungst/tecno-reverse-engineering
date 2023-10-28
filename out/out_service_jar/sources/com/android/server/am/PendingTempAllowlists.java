package com.android.server.am;

import android.util.SparseArray;
import com.android.server.am.ActivityManagerService;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class PendingTempAllowlists {
    private final SparseArray<ActivityManagerService.PendingTempAllowlist> mPendingTempAllowlist = new SparseArray<>();
    private ActivityManagerService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PendingTempAllowlists(ActivityManagerService service) {
        this.mService = service;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void put(int uid, ActivityManagerService.PendingTempAllowlist value) {
        synchronized (this.mPendingTempAllowlist) {
            this.mPendingTempAllowlist.put(uid, value);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeAt(int index) {
        synchronized (this.mPendingTempAllowlist) {
            this.mPendingTempAllowlist.removeAt(index);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityManagerService.PendingTempAllowlist get(int uid) {
        ActivityManagerService.PendingTempAllowlist pendingTempAllowlist;
        synchronized (this.mPendingTempAllowlist) {
            pendingTempAllowlist = this.mPendingTempAllowlist.get(uid);
        }
        return pendingTempAllowlist;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int size() {
        int size;
        synchronized (this.mPendingTempAllowlist) {
            size = this.mPendingTempAllowlist.size();
        }
        return size;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityManagerService.PendingTempAllowlist valueAt(int index) {
        ActivityManagerService.PendingTempAllowlist valueAt;
        synchronized (this.mPendingTempAllowlist) {
            valueAt = this.mPendingTempAllowlist.valueAt(index);
        }
        return valueAt;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int indexOfKey(int key) {
        int indexOfKey;
        synchronized (this.mPendingTempAllowlist) {
            indexOfKey = this.mPendingTempAllowlist.indexOfKey(key);
        }
        return indexOfKey;
    }
}
