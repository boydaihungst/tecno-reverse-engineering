package com.android.net.module.util;

import android.util.SparseIntArray;
/* loaded from: classes4.dex */
public class PerUidCounter {
    private final int mMaxCountPerUid;
    final SparseIntArray mUidToCount = new SparseIntArray();

    public PerUidCounter(int maxCountPerUid) {
        if (maxCountPerUid <= 0) {
            throw new IllegalArgumentException("Maximum counter value must be positive");
        }
        this.mMaxCountPerUid = maxCountPerUid;
    }

    public void incrementCountOrThrow(int uid) {
        incrementCountOrThrow(uid, 1);
    }

    public synchronized void incrementCountOrThrow(int uid, int numToIncrement) {
        try {
            if (numToIncrement <= 0) {
                throw new IllegalArgumentException("Increment count must be positive");
            }
            long newCount = this.mUidToCount.get(uid, 0) + numToIncrement;
            if (newCount > this.mMaxCountPerUid) {
                throw new IllegalStateException("Uid " + uid + " exceeded its allowed limit");
            }
            this.mUidToCount.put(uid, (int) newCount);
        } catch (Throwable th) {
            throw th;
        }
    }

    public void decrementCountOrThrow(int uid) {
        decrementCountOrThrow(uid, 1);
    }

    public synchronized void decrementCountOrThrow(int uid, int numToDecrement) {
        try {
            if (numToDecrement <= 0) {
                throw new IllegalArgumentException("Decrement count must be positive");
            }
            int newCount = this.mUidToCount.get(uid, 0) - numToDecrement;
            if (newCount < 0) {
                throw new IllegalStateException("BUG: too small count " + newCount + " for UID " + uid);
            }
            if (newCount == 0) {
                this.mUidToCount.delete(uid);
            } else {
                this.mUidToCount.put(uid, newCount);
            }
        } catch (Throwable th) {
            throw th;
        }
    }
}
