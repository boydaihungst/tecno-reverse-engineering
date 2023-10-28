package com.android.server.policy.keyguard;

import android.app.ActivityManager;
import android.content.Context;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.policy.IKeyguardService;
import com.android.internal.policy.IKeyguardStateCallback;
import com.android.internal.widget.LockPatternUtils;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
public class KeyguardStateMonitor extends IKeyguardStateCallback.Stub {
    private static final String TAG = "KeyguardStateMonitor";
    private final StateCallback mCallback;
    private final LockPatternUtils mLockPatternUtils;
    private volatile boolean mIsShowing = true;
    private volatile boolean mSimSecure = true;
    private volatile boolean mInputRestricted = true;
    private volatile boolean mTrusted = false;
    private int mCurrentUserId = ActivityManager.getCurrentUser();

    /* loaded from: classes2.dex */
    public interface StateCallback {
        void onShowingChanged();

        void onTrustedChanged();
    }

    public KeyguardStateMonitor(Context context, IKeyguardService service, StateCallback callback) {
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mCallback = callback;
        try {
            service.addStateMonitorCallback(this);
        } catch (RemoteException e) {
            Slog.w(TAG, "Remote Exception", e);
        }
    }

    public boolean isShowing() {
        return this.mIsShowing;
    }

    public boolean isSecure(int userId) {
        return this.mLockPatternUtils.isSecure(userId) || this.mSimSecure;
    }

    public boolean isInputRestricted() {
        return this.mInputRestricted;
    }

    public boolean isTrusted() {
        return this.mTrusted;
    }

    public int getCurrentUser() {
        return this.mCurrentUserId;
    }

    public void onShowingStateChanged(boolean showing, int userId) {
        if (userId != this.mCurrentUserId) {
            return;
        }
        this.mIsShowing = showing;
        this.mCallback.onShowingChanged();
    }

    public void onSimSecureStateChanged(boolean simSecure) {
        this.mSimSecure = simSecure;
    }

    public synchronized void setCurrentUser(int userId) {
        this.mCurrentUserId = userId;
    }

    public void onInputRestrictedStateChanged(boolean inputRestricted) {
        this.mInputRestricted = inputRestricted;
    }

    public void onTrustedChanged(boolean trusted) {
        this.mTrusted = trusted;
        this.mCallback.onTrustedChanged();
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.println(prefix + TAG);
        String prefix2 = prefix + "  ";
        pw.println(prefix2 + "mIsShowing=" + this.mIsShowing);
        pw.println(prefix2 + "mSimSecure=" + this.mSimSecure);
        pw.println(prefix2 + "mInputRestricted=" + this.mInputRestricted);
        pw.println(prefix2 + "mTrusted=" + this.mTrusted);
        pw.println(prefix2 + "mCurrentUserId=" + this.mCurrentUserId);
    }
}
