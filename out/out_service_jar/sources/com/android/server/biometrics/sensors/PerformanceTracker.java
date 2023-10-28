package com.android.server.biometrics.sensors;

import android.util.SparseArray;
/* loaded from: classes.dex */
public class PerformanceTracker {
    private static final String TAG = "PerformanceTracker";
    private static SparseArray<PerformanceTracker> sTrackers;
    private final SparseArray<Info> mAllUsersInfo = new SparseArray<>();
    private int mHALDeathCount;

    public static PerformanceTracker getInstanceForSensorId(int sensorId) {
        if (sTrackers == null) {
            sTrackers = new SparseArray<>();
        }
        if (!sTrackers.contains(sensorId)) {
            sTrackers.put(sensorId, new PerformanceTracker());
        }
        return sTrackers.get(sensorId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Info {
        int mAccept;
        int mAcceptCrypto;
        int mAcquire;
        int mAcquireCrypto;
        int mPermanentLockout;
        int mReject;
        int mRejectCrypto;
        int mTimedLockout;

        private Info() {
        }
    }

    private PerformanceTracker() {
    }

    private void createUserEntryIfNecessary(int userId) {
        if (!this.mAllUsersInfo.contains(userId)) {
            this.mAllUsersInfo.put(userId, new Info());
        }
    }

    public void incrementAuthForUser(int userId, boolean accepted) {
        createUserEntryIfNecessary(userId);
        if (accepted) {
            this.mAllUsersInfo.get(userId).mAccept++;
            return;
        }
        this.mAllUsersInfo.get(userId).mReject++;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void incrementCryptoAuthForUser(int userId, boolean accepted) {
        createUserEntryIfNecessary(userId);
        if (accepted) {
            this.mAllUsersInfo.get(userId).mAcceptCrypto++;
            return;
        }
        this.mAllUsersInfo.get(userId).mRejectCrypto++;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void incrementAcquireForUser(int userId, boolean isCrypto) {
        createUserEntryIfNecessary(userId);
        if (isCrypto) {
            this.mAllUsersInfo.get(userId).mAcquireCrypto++;
            return;
        }
        this.mAllUsersInfo.get(userId).mAcquire++;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void incrementTimedLockoutForUser(int userId) {
        createUserEntryIfNecessary(userId);
        this.mAllUsersInfo.get(userId).mTimedLockout++;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void incrementPermanentLockoutForUser(int userId) {
        createUserEntryIfNecessary(userId);
        this.mAllUsersInfo.get(userId).mPermanentLockout++;
    }

    public void incrementHALDeathCount() {
        this.mHALDeathCount++;
    }

    public void clear() {
        this.mAllUsersInfo.clear();
        this.mHALDeathCount = 0;
    }

    public int getAcceptForUser(int userId) {
        if (this.mAllUsersInfo.contains(userId)) {
            return this.mAllUsersInfo.get(userId).mAccept;
        }
        return 0;
    }

    public int getRejectForUser(int userId) {
        if (this.mAllUsersInfo.contains(userId)) {
            return this.mAllUsersInfo.get(userId).mReject;
        }
        return 0;
    }

    public int getAcquireForUser(int userId) {
        if (this.mAllUsersInfo.contains(userId)) {
            return this.mAllUsersInfo.get(userId).mAcquire;
        }
        return 0;
    }

    public int getAcceptCryptoForUser(int userId) {
        if (this.mAllUsersInfo.contains(userId)) {
            return this.mAllUsersInfo.get(userId).mAcceptCrypto;
        }
        return 0;
    }

    public int getRejectCryptoForUser(int userId) {
        if (this.mAllUsersInfo.contains(userId)) {
            return this.mAllUsersInfo.get(userId).mRejectCrypto;
        }
        return 0;
    }

    public int getAcquireCryptoForUser(int userId) {
        if (this.mAllUsersInfo.contains(userId)) {
            return this.mAllUsersInfo.get(userId).mAcquireCrypto;
        }
        return 0;
    }

    public int getTimedLockoutForUser(int userId) {
        if (this.mAllUsersInfo.contains(userId)) {
            return this.mAllUsersInfo.get(userId).mTimedLockout;
        }
        return 0;
    }

    public int getPermanentLockoutForUser(int userId) {
        if (this.mAllUsersInfo.contains(userId)) {
            return this.mAllUsersInfo.get(userId).mPermanentLockout;
        }
        return 0;
    }

    public int getHALDeathCount() {
        return this.mHALDeathCount;
    }
}
