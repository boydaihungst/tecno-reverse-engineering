package com.android.server.wm;

import android.app.admin.DevicePolicyCache;
import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import com.android.server.LocalServices;
import com.android.server.pm.UserManagerInternal;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.utils.UserTokenWatcher;
import com.android.server.wm.LockTaskController;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class KeyguardDisableHandler {
    private static final String TAG = "WindowManager";
    private final UserTokenWatcher mAppTokenWatcher;
    private final UserTokenWatcher.Callback mCallback;
    private int mCurrentUser = 0;
    private Injector mInjector;
    private final UserTokenWatcher mSystemTokenWatcher;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface Injector {
        boolean dpmRequiresPassword(int i);

        void enableKeyguard(boolean z);

        int getProfileParentId(int i);

        boolean isKeyguardSecure(int i);
    }

    KeyguardDisableHandler(Injector injector, Handler handler) {
        UserTokenWatcher.Callback callback = new UserTokenWatcher.Callback() { // from class: com.android.server.wm.KeyguardDisableHandler.1
            @Override // com.android.server.utils.UserTokenWatcher.Callback
            public void acquired(int userId) {
                KeyguardDisableHandler.this.updateKeyguardEnabled(userId);
            }

            @Override // com.android.server.utils.UserTokenWatcher.Callback
            public void released(int userId) {
                KeyguardDisableHandler.this.updateKeyguardEnabled(userId);
            }
        };
        this.mCallback = callback;
        this.mInjector = injector;
        this.mAppTokenWatcher = new UserTokenWatcher(callback, handler, "WindowManager");
        this.mSystemTokenWatcher = new UserTokenWatcher(callback, handler, "WindowManager");
    }

    public void setCurrentUser(int user) {
        synchronized (this) {
            this.mCurrentUser = user;
            updateKeyguardEnabledLocked(-1);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateKeyguardEnabled(int userId) {
        synchronized (this) {
            updateKeyguardEnabledLocked(userId);
        }
    }

    private void updateKeyguardEnabledLocked(int userId) {
        int i = this.mCurrentUser;
        if (i == userId || userId == -1) {
            this.mInjector.enableKeyguard(shouldKeyguardBeEnabled(i));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void disableKeyguard(IBinder token, String tag, int callingUid, int userId) {
        UserTokenWatcher watcherForCaller = watcherForCallingUid(token, callingUid);
        watcherForCaller.acquire(token, tag, this.mInjector.getProfileParentId(userId));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reenableKeyguard(IBinder token, int callingUid, int userId) {
        UserTokenWatcher watcherForCaller = watcherForCallingUid(token, callingUid);
        watcherForCaller.release(token, this.mInjector.getProfileParentId(userId));
    }

    private UserTokenWatcher watcherForCallingUid(IBinder token, int callingUid) {
        if (Process.isApplicationUid(callingUid)) {
            return this.mAppTokenWatcher;
        }
        if (callingUid == 1000 && (token instanceof LockTaskController.LockTaskToken)) {
            return this.mSystemTokenWatcher;
        }
        throw new UnsupportedOperationException("Only apps can use the KeyguardLock API");
    }

    private boolean shouldKeyguardBeEnabled(int userId) {
        boolean dpmRequiresPassword = this.mInjector.dpmRequiresPassword(this.mCurrentUser);
        boolean keyguardSecure = this.mInjector.isKeyguardSecure(this.mCurrentUser);
        boolean allowedFromApps = (dpmRequiresPassword || keyguardSecure) ? false : true;
        boolean allowedFromSystem = !dpmRequiresPassword;
        boolean shouldBeDisabled = (allowedFromApps && this.mAppTokenWatcher.isAcquired(userId)) || (allowedFromSystem && this.mSystemTokenWatcher.isAcquired(userId));
        return !shouldBeDisabled;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static KeyguardDisableHandler create(Context context, final WindowManagerPolicy policy, Handler handler) {
        final UserManagerInternal userManager = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        return new KeyguardDisableHandler(new Injector() { // from class: com.android.server.wm.KeyguardDisableHandler.2
            @Override // com.android.server.wm.KeyguardDisableHandler.Injector
            public boolean dpmRequiresPassword(int userId) {
                return DevicePolicyCache.getInstance().getPasswordQuality(userId) != 0;
            }

            @Override // com.android.server.wm.KeyguardDisableHandler.Injector
            public boolean isKeyguardSecure(int userId) {
                return WindowManagerPolicy.this.isKeyguardSecure(userId);
            }

            @Override // com.android.server.wm.KeyguardDisableHandler.Injector
            public int getProfileParentId(int userId) {
                return userManager.getProfileParentId(userId);
            }

            @Override // com.android.server.wm.KeyguardDisableHandler.Injector
            public void enableKeyguard(boolean enabled) {
                WindowManagerPolicy.this.enableKeyguard(enabled);
            }
        }, handler);
    }
}
