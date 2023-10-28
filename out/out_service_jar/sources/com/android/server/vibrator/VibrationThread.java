package com.android.server.vibrator;

import android.os.CombinedVibration;
import android.os.DynamicEffect;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.VibrationEffect;
import android.os.WorkSource;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.vibrator.Vibration;
import java.util.NoSuchElementException;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class VibrationThread extends Thread {
    static final boolean DEBUG = false;
    static final String TAG = "VibrationThread";
    private VibrationStepConductor mExecutingConductor;
    private VibrationStepConductor mRequestedActiveConductor;
    private final VibratorManagerHooks mVibratorManagerHooks;
    private final PowerManager.WakeLock mWakeLock;
    private final Object mLock = new Object();
    private boolean mCalledVibrationCompleteCallback = false;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface VibratorManagerHooks {
        void cancelSyncedVibration();

        void noteVibratorOff(int i);

        void noteVibratorOn(int i, long j);

        void onVibrationCompleted(long j, Vibration.Status status);

        void onVibrationThreadReleased(long j);

        boolean prepareSyncedVibration(long j, int[] iArr);

        boolean triggerSyncedVibration(long j);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public VibrationThread(PowerManager.WakeLock wakeLock, VibratorManagerHooks vibratorManagerHooks) {
        this.mWakeLock = wakeLock;
        this.mVibratorManagerHooks = vibratorManagerHooks;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean runVibrationOnVibrationThread(VibrationStepConductor conductor) {
        synchronized (this.mLock) {
            if (this.mRequestedActiveConductor != null) {
                Slog.wtf(TAG, "Attempt to start vibration when one already running");
                return false;
            }
            this.mRequestedActiveConductor = conductor;
            this.mLock.notifyAll();
            return true;
        }
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        Process.setThreadPriority(-8);
        while (true) {
            this.mExecutingConductor = (VibrationStepConductor) Objects.requireNonNull(waitForVibrationRequest());
            this.mCalledVibrationCompleteCallback = false;
            runCurrentVibrationWithWakeLock();
            if (!this.mExecutingConductor.isFinished()) {
                Slog.wtf(TAG, "VibrationThread terminated with unfinished vibration");
            }
            synchronized (this.mLock) {
                this.mRequestedActiveConductor = null;
            }
            this.mVibratorManagerHooks.onVibrationThreadReleased(this.mExecutingConductor.getVibration().id);
            synchronized (this.mLock) {
                this.mLock.notifyAll();
            }
            this.mExecutingConductor = null;
        }
    }

    private boolean isDynamicEffect() {
        CombinedVibration.Mono effect = this.mExecutingConductor.getVibration().getEffect();
        if (effect instanceof CombinedVibration.Mono) {
            VibrationEffect eft = effect.getEffect();
            return eft instanceof DynamicEffect;
        }
        return false;
    }

    public void update_vib_info(int infocase, int info) {
        if (!isDynamicEffect()) {
            Slog.e(TAG, "Not dynamic effect");
            return;
        }
        SparseArray<VibratorController> vibrators = this.mExecutingConductor.getVibrators();
        for (int i = 0; i < vibrators.size(); i++) {
            VibratorController controller = vibrators.valueAt(i);
            controller.update_vib_info(infocase, info);
        }
    }

    public void stopDynamicEffect() {
        if (!isDynamicEffect()) {
            Slog.e(TAG, "Not dynamic effect");
            return;
        }
        SparseArray<VibratorController> vibrators = this.mExecutingConductor.getVibrators();
        for (int i = 0; i < vibrators.size(); i++) {
            VibratorController controller = vibrators.valueAt(i);
            controller.stopDynamicEffect();
        }
        this.mExecutingConductor.notifyCancelled(Vibration.Status.CANCELLED_BY_USER, true);
    }

    public boolean waitForThreadIdle(long maxWaitMillis) {
        long now = SystemClock.elapsedRealtime();
        long deadline = now + maxWaitMillis;
        synchronized (this.mLock) {
            while (this.mRequestedActiveConductor != null) {
                if (now >= deadline) {
                    return false;
                }
                try {
                    this.mLock.wait(deadline - now);
                } catch (InterruptedException e) {
                    Slog.w(TAG, "VibrationThread interrupted waiting to stop, continuing");
                }
                now = SystemClock.elapsedRealtime();
            }
            return true;
        }
    }

    private VibrationStepConductor waitForVibrationRequest() {
        while (true) {
            synchronized (this.mLock) {
                VibrationStepConductor vibrationStepConductor = this.mRequestedActiveConductor;
                if (vibrationStepConductor != null) {
                    return vibrationStepConductor;
                }
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    Slog.w(TAG, "VibrationThread interrupted waiting to start, continuing");
                }
            }
        }
    }

    boolean isRunningVibrationId(long id) {
        boolean z;
        synchronized (this.mLock) {
            VibrationStepConductor vibrationStepConductor = this.mRequestedActiveConductor;
            z = vibrationStepConductor != null && vibrationStepConductor.getVibration().id == id;
        }
        return z;
    }

    private void runCurrentVibrationWithWakeLock() {
        WorkSource workSource = new WorkSource(this.mExecutingConductor.getVibration().uid);
        this.mWakeLock.setWorkSource(workSource);
        this.mWakeLock.acquire();
        try {
            runCurrentVibrationWithWakeLockAndDeathLink();
            clientVibrationCompleteIfNotAlready(Vibration.Status.FINISHED_UNEXPECTED);
        } finally {
            this.mWakeLock.release();
            this.mWakeLock.setWorkSource(null);
        }
    }

    private void runCurrentVibrationWithWakeLockAndDeathLink() {
        IBinder vibrationBinderToken = this.mExecutingConductor.getVibration().token;
        try {
            vibrationBinderToken.linkToDeath(this.mExecutingConductor, 0);
            try {
                playVibration();
                try {
                    vibrationBinderToken.unlinkToDeath(this.mExecutingConductor, 0);
                } catch (NoSuchElementException e) {
                    Slog.wtf(TAG, "Failed to unlink token", e);
                }
            } catch (Throwable th) {
                try {
                    vibrationBinderToken.unlinkToDeath(this.mExecutingConductor, 0);
                } catch (NoSuchElementException e2) {
                    Slog.wtf(TAG, "Failed to unlink token", e2);
                }
                throw th;
            }
        } catch (RemoteException e3) {
            Slog.e(TAG, "Error linking vibration to token death", e3);
            clientVibrationCompleteIfNotAlready(Vibration.Status.IGNORED_ERROR_TOKEN);
        }
    }

    private void clientVibrationCompleteIfNotAlready(Vibration.Status completedStatus) {
        if (!this.mCalledVibrationCompleteCallback) {
            this.mCalledVibrationCompleteCallback = true;
            this.mVibratorManagerHooks.onVibrationCompleted(this.mExecutingConductor.getVibration().id, completedStatus);
        }
    }

    private void playVibration() {
        Trace.traceBegin(8388608L, "playVibration");
        try {
            this.mExecutingConductor.prepareToStart();
            while (!this.mExecutingConductor.isFinished()) {
                boolean readyToRun = this.mExecutingConductor.waitUntilNextStepIsDue();
                if (readyToRun) {
                    this.mExecutingConductor.runNextStep();
                }
                Vibration.Status status = this.mExecutingConductor.calculateVibrationStatus();
                if (status != Vibration.Status.RUNNING && !this.mCalledVibrationCompleteCallback) {
                    clientVibrationCompleteIfNotAlready(status);
                }
            }
        } finally {
            Trace.traceEnd(8388608L);
        }
    }
}
