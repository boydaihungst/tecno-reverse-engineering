package com.android.server.policy.keyguard;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.policy.IKeyguardDrawnCallback;
import com.android.internal.policy.IKeyguardExitCallback;
import com.android.internal.policy.IKeyguardService;
import com.android.internal.policy.IKeyguardStateCallback;
import com.android.server.policy.keyguard.KeyguardStateMonitor;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
public class KeyguardServiceWrapper implements IKeyguardService {
    private String TAG = "KeyguardServiceWrapper";
    private KeyguardStateMonitor mKeyguardStateMonitor;
    private IKeyguardService mService;

    public KeyguardServiceWrapper(Context context, IKeyguardService service, KeyguardStateMonitor.StateCallback callback) {
        this.mService = service;
        this.mKeyguardStateMonitor = new KeyguardStateMonitor(context, service, callback);
    }

    public void verifyUnlock(IKeyguardExitCallback callback) {
        try {
            this.mService.verifyUnlock(callback);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void setOccluded(boolean isOccluded, boolean animate) {
        try {
            this.mService.setOccluded(isOccluded, animate);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void addStateMonitorCallback(IKeyguardStateCallback callback) {
        try {
            this.mService.addStateMonitorCallback(callback);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void dismiss(IKeyguardDismissCallback callback, CharSequence message) {
        try {
            this.mService.dismiss(callback, message);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onDreamingStarted() {
        try {
            this.mService.onDreamingStarted();
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onDreamingStopped() {
        try {
            this.mService.onDreamingStopped();
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onStartedGoingToSleep(int pmSleepReason) {
        try {
            this.mService.onStartedGoingToSleep(pmSleepReason);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onFinishedGoingToSleep(int pmSleepReason, boolean cameraGestureTriggered) {
        try {
            this.mService.onFinishedGoingToSleep(pmSleepReason, cameraGestureTriggered);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onStartedWakingUp(int pmWakeReason, boolean cameraGestureTriggered) {
        try {
            this.mService.onStartedWakingUp(pmWakeReason, cameraGestureTriggered);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onFinishedWakingUp() {
        try {
            this.mService.onFinishedWakingUp();
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onScreenTurningOn(IKeyguardDrawnCallback callback) {
        try {
            this.mService.onScreenTurningOn(callback);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onScreenTurnedOn() {
        try {
            this.mService.onScreenTurnedOn();
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onScreenTurningOff() {
        try {
            this.mService.onScreenTurningOff();
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onScreenTurnedOff() {
        try {
            this.mService.onScreenTurnedOff();
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void setKeyguardEnabled(boolean enabled) {
        try {
            this.mService.setKeyguardEnabled(enabled);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onSystemReady() {
        try {
            this.mService.onSystemReady();
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void doKeyguardTimeout(Bundle options) {
        int userId = this.mKeyguardStateMonitor.getCurrentUser();
        if (this.mKeyguardStateMonitor.isSecure(userId)) {
            this.mKeyguardStateMonitor.onShowingStateChanged(true, userId);
        }
        try {
            this.mService.doKeyguardTimeout(options);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void setSwitchingUser(boolean switching) {
        try {
            this.mService.setSwitchingUser(switching);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void setCurrentUser(int userId) {
        this.mKeyguardStateMonitor.setCurrentUser(userId);
        try {
            this.mService.setCurrentUser(userId);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onBootCompleted() {
        try {
            this.mService.onBootCompleted();
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void startKeyguardExitAnimation(long startTime, long fadeoutDuration) {
        try {
            this.mService.startKeyguardExitAnimation(startTime, fadeoutDuration);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onShortPowerPressedGoHome() {
        try {
            this.mService.onShortPowerPressedGoHome();
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void dismissKeyguardToLaunch(Intent intentToLaunch) {
        try {
            this.mService.dismissKeyguardToLaunch(intentToLaunch);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public void onSystemKeyPressed(int keycode) {
        try {
            this.mService.onSystemKeyPressed(keycode);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "Remote Exception", e);
        }
    }

    public IBinder asBinder() {
        return this.mService.asBinder();
    }

    public boolean isShowing() {
        return this.mKeyguardStateMonitor.isShowing();
    }

    public boolean isTrusted() {
        return this.mKeyguardStateMonitor.isTrusted();
    }

    public boolean isSecure(int userId) {
        return this.mKeyguardStateMonitor.isSecure(userId);
    }

    public boolean isInputRestricted() {
        return this.mKeyguardStateMonitor.isInputRestricted();
    }

    public void dump(String prefix, PrintWriter pw) {
        this.mKeyguardStateMonitor.dump(prefix, pw);
    }

    public void onStartedNotifyFaceunlock(int reason) {
        try {
            this.mService.onStartedNotifyFaceunlock(reason);
        } catch (RemoteException e) {
            Slog.w(this.TAG, "onStartedNotifyFaceunlock Exception", e);
        }
    }

    public void onStartFaceUnlock() {
        try {
            this.mService.onStartFaceUnlock();
        } catch (RemoteException e) {
            Slog.w(this.TAG, "onStartFaceUnlock Exception", e);
        }
    }
}
