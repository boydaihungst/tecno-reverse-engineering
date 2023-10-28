package com.android.server.am;

import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import java.util.function.BiConsumer;
/* loaded from: classes.dex */
public class FgsTempAllowList<E> {
    private static final int DEFAULT_MAX_SIZE = 100;
    private final Object mLock;
    private int mMaxSize;
    private final SparseArray<Pair<Long, E>> mTempAllowList;

    public FgsTempAllowList() {
        this.mTempAllowList = new SparseArray<>();
        this.mMaxSize = 100;
        this.mLock = new Object();
    }

    public FgsTempAllowList(int maxSize) {
        this.mTempAllowList = new SparseArray<>();
        this.mMaxSize = 100;
        this.mLock = new Object();
        if (maxSize <= 0) {
            Slog.e("ActivityManager", "Invalid FgsTempAllowList maxSize:" + maxSize + ", force default maxSize:100");
            this.mMaxSize = 100;
            return;
        }
        this.mMaxSize = maxSize;
    }

    public void add(int uid, long durationMs, E entry) {
        synchronized (this.mLock) {
            if (durationMs <= 0) {
                Slog.e("ActivityManager", "FgsTempAllowList bad duration:" + durationMs + " key: " + uid);
                return;
            }
            long now = SystemClock.elapsedRealtime();
            int size = this.mTempAllowList.size();
            if (size > this.mMaxSize) {
                Slog.w("ActivityManager", "FgsTempAllowList length:" + size + " exceeds maxSize" + this.mMaxSize);
                for (int index = size - 1; index >= 0; index--) {
                    if (((Long) this.mTempAllowList.valueAt(index).first).longValue() < now) {
                        this.mTempAllowList.removeAt(index);
                    }
                }
            }
            Pair<Long, E> existing = this.mTempAllowList.get(uid);
            long expirationTime = now + durationMs;
            if (existing == null || ((Long) existing.first).longValue() < expirationTime) {
                this.mTempAllowList.put(uid, new Pair<>(Long.valueOf(expirationTime), entry));
            }
        }
    }

    public Pair<Long, E> get(int uid) {
        synchronized (this.mLock) {
            int index = this.mTempAllowList.indexOfKey(uid);
            if (index < 0) {
                return null;
            }
            if (((Long) this.mTempAllowList.valueAt(index).first).longValue() < SystemClock.elapsedRealtime()) {
                this.mTempAllowList.removeAt(index);
                return null;
            }
            return this.mTempAllowList.valueAt(index);
        }
    }

    public boolean isAllowed(int uid) {
        Pair<Long, E> entry = get(uid);
        return entry != null;
    }

    public void removeUid(int uid) {
        synchronized (this.mLock) {
            this.mTempAllowList.remove(uid);
        }
    }

    public void removeAppId(int appId) {
        synchronized (this.mLock) {
            for (int i = this.mTempAllowList.size() - 1; i >= 0; i--) {
                int uid = this.mTempAllowList.keyAt(i);
                if (UserHandle.getAppId(uid) == appId) {
                    this.mTempAllowList.removeAt(i);
                }
            }
        }
    }

    public void forEach(BiConsumer<Integer, Pair<Long, E>> callback) {
        synchronized (this.mLock) {
            for (int i = 0; i < this.mTempAllowList.size(); i++) {
                int uid = this.mTempAllowList.keyAt(i);
                Pair<Long, E> entry = this.mTempAllowList.valueAt(i);
                if (entry != null) {
                    callback.accept(Integer.valueOf(uid), entry);
                }
            }
        }
    }
}
