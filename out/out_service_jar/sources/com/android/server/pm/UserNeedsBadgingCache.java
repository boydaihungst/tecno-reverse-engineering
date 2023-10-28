package com.android.server.pm;

import android.content.pm.UserInfo;
import android.os.Binder;
import android.util.SparseBooleanArray;
/* loaded from: classes2.dex */
public class UserNeedsBadgingCache {
    private final Object mLock = new Object();
    private final SparseBooleanArray mUserCache = new SparseBooleanArray();
    private final UserManagerService mUserManager;

    public UserNeedsBadgingCache(UserManagerService userManager) {
        this.mUserManager = userManager;
    }

    public void delete(int userId) {
        synchronized (this.mLock) {
            this.mUserCache.delete(userId);
        }
    }

    public boolean get(int userId) {
        synchronized (this.mLock) {
            int index = this.mUserCache.indexOfKey(userId);
            if (index >= 0) {
                return this.mUserCache.valueAt(index);
            }
            long token = Binder.clearCallingIdentity();
            try {
                UserInfo userInfo = this.mUserManager.getUserInfo(userId);
                boolean b = userInfo != null && userInfo.isManagedProfile();
                synchronized (this.mLock) {
                    this.mUserCache.put(userId, b);
                }
                return b;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }
}
