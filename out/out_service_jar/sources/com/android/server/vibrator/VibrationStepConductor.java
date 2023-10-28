package com.android.server.vibrator;

import android.os.Build;
import android.os.CombinedVibration;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.vibrator.PrebakedSegment;
import android.os.vibrator.PrimitiveSegment;
import android.os.vibrator.RampSegment;
import android.os.vibrator.VibrationEffectSegment;
import android.util.IntArray;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.vibrator.Vibration;
import com.android.server.vibrator.VibrationThread;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class VibrationStepConductor implements IBinder.DeathRecipient {
    static final long CALLBACKS_EXTRA_TIMEOUT = 1000;
    private static final boolean DEBUG = false;
    static final List<Step> EMPTY_STEP_LIST = new ArrayList();
    static final float RAMP_OFF_AMPLITUDE_MIN = 0.001f;
    private static final String TAG = "VibrationThread";
    public final DeviceVibrationEffectAdapter deviceEffectAdapter;
    private int mPendingVibrateSteps;
    private int mRemainingStartSequentialEffectSteps;
    private final IntArray mSignalVibratorsComplete;
    private int mSuccessfulVibratorOnSteps;
    private final Vibration mVibration;
    public final VibrationSettings vibrationSettings;
    public final VibrationThread.VibratorManagerHooks vibratorManagerHooks;
    private final SparseArray<VibratorController> mVibrators = new SparseArray<>();
    private final PriorityQueue<Step> mNextSteps = new PriorityQueue<>();
    private final Queue<Step> mPendingOnVibratorCompleteSteps = new LinkedList();
    private final Object mLock = new Object();
    private Vibration.Status mSignalCancelStatus = null;
    private boolean mSignalCancelImmediate = false;
    private Vibration.Status mCancelStatus = null;
    private boolean mCancelledImmediately = false;

    /* JADX INFO: Access modifiers changed from: package-private */
    public VibrationStepConductor(Vibration vib, VibrationSettings vibrationSettings, DeviceVibrationEffectAdapter effectAdapter, SparseArray<VibratorController> availableVibrators, VibrationThread.VibratorManagerHooks vibratorManagerHooks) {
        this.mVibration = vib;
        this.vibrationSettings = vibrationSettings;
        this.deviceEffectAdapter = effectAdapter;
        this.vibratorManagerHooks = vibratorManagerHooks;
        CombinedVibration effect = vib.getEffect();
        for (int i = 0; i < availableVibrators.size(); i++) {
            if (effect.hasVibrator(availableVibrators.keyAt(i))) {
                this.mVibrators.put(availableVibrators.keyAt(i), availableVibrators.valueAt(i));
            }
        }
        this.mSignalVibratorsComplete = new IntArray(this.mVibrators.size());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AbstractVibratorStep nextVibrateStep(long startTime, VibratorController controller, VibrationEffect.Composed effect, int segmentIndex, long previousStepVibratorOffTimeout) {
        int segmentIndex2;
        if (Build.IS_DEBUGGABLE) {
            expectIsVibrationThread(true);
        }
        if (segmentIndex < effect.getSegments().size()) {
            segmentIndex2 = segmentIndex;
        } else {
            segmentIndex2 = effect.getRepeatIndex();
        }
        if (segmentIndex2 < 0) {
            return new CompleteEffectVibratorStep(this, startTime, false, controller, previousStepVibratorOffTimeout);
        }
        VibrationEffectSegment segment = (VibrationEffectSegment) effect.getSegments().get(segmentIndex2);
        if (segment instanceof PrebakedSegment) {
            return new PerformPrebakedVibratorStep(this, startTime, controller, effect, segmentIndex2, previousStepVibratorOffTimeout);
        }
        if (segment instanceof PrimitiveSegment) {
            return new ComposePrimitivesVibratorStep(this, startTime, controller, effect, segmentIndex2, previousStepVibratorOffTimeout);
        }
        if (segment instanceof RampSegment) {
            return new ComposePwleVibratorStep(this, startTime, controller, effect, segmentIndex2, previousStepVibratorOffTimeout);
        }
        return new SetAmplitudeVibratorStep(this, startTime, controller, effect, segmentIndex2, previousStepVibratorOffTimeout);
    }

    public void prepareToStart() {
        if (Build.IS_DEBUGGABLE) {
            expectIsVibrationThread(true);
        }
        CombinedVibration.Sequential sequentialEffect = toSequential(this.mVibration.getEffect());
        this.mPendingVibrateSteps++;
        this.mRemainingStartSequentialEffectSteps = sequentialEffect.getEffects().size();
        this.mNextSteps.offer(new StartSequentialEffectStep(this, sequentialEffect));
    }

    public Vibration getVibration() {
        return this.mVibration;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SparseArray<VibratorController> getVibrators() {
        return this.mVibrators;
    }

    public boolean isFinished() {
        if (Build.IS_DEBUGGABLE) {
            expectIsVibrationThread(true);
        }
        if (this.mCancelledImmediately) {
            return true;
        }
        return this.mPendingOnVibratorCompleteSteps.isEmpty() && this.mNextSteps.isEmpty();
    }

    public Vibration.Status calculateVibrationStatus() {
        if (Build.IS_DEBUGGABLE) {
            expectIsVibrationThread(true);
        }
        Vibration.Status status = this.mCancelStatus;
        if (status != null) {
            return status;
        }
        if (this.mPendingVibrateSteps > 0 || this.mRemainingStartSequentialEffectSteps > 0) {
            return Vibration.Status.RUNNING;
        }
        if (this.mSuccessfulVibratorOnSteps > 0) {
            return Vibration.Status.FINISHED;
        }
        return Vibration.Status.IGNORED_UNSUPPORTED;
    }

    public boolean waitUntilNextStepIsDue() {
        Step nextStep;
        if (Build.IS_DEBUGGABLE) {
            expectIsVibrationThread(true);
        }
        processAllNotifySignals();
        if (this.mCancelledImmediately) {
            return false;
        }
        if (this.mPendingOnVibratorCompleteSteps.isEmpty() && (nextStep = this.mNextSteps.peek()) != null) {
            long waitMillis = nextStep.calculateWaitTime();
            if (waitMillis <= 0) {
                return true;
            }
            synchronized (this.mLock) {
                if (hasPendingNotifySignalLocked()) {
                    return false;
                }
                try {
                    this.mLock.wait(waitMillis);
                } catch (InterruptedException e) {
                }
                return false;
            }
        }
        return true;
    }

    private Step pollNext() {
        if (Build.IS_DEBUGGABLE) {
            expectIsVibrationThread(true);
        }
        if (!this.mPendingOnVibratorCompleteSteps.isEmpty()) {
            return this.mPendingOnVibratorCompleteSteps.poll();
        }
        return this.mNextSteps.poll();
    }

    public void runNextStep() {
        if (Build.IS_DEBUGGABLE) {
            expectIsVibrationThread(true);
        }
        Step nextStep = pollNext();
        if (nextStep != null) {
            List<Step> nextSteps = nextStep.play();
            if (nextStep.getVibratorOnDuration() > 0) {
                this.mSuccessfulVibratorOnSteps++;
            }
            if (nextStep instanceof StartSequentialEffectStep) {
                this.mRemainingStartSequentialEffectSteps--;
            }
            if (!nextStep.isCleanUp()) {
                this.mPendingVibrateSteps--;
            }
            for (int i = 0; i < nextSteps.size(); i++) {
                this.mPendingVibrateSteps += !nextSteps.get(i).isCleanUp();
            }
            this.mNextSteps.addAll(nextSteps);
        }
    }

    @Override // android.os.IBinder.DeathRecipient
    public void binderDied() {
        notifyCancelled(Vibration.Status.CANCELLED_BINDER_DIED, false);
    }

    /* JADX WARN: Code restructure failed: missing block: B:13:0x003d, code lost:
        if (r3.mSignalCancelImmediate == false) goto L10;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void notifyCancelled(Vibration.Status cancelStatus, boolean immediate) {
        if (Build.IS_DEBUGGABLE) {
            expectIsVibrationThread(false);
        }
        if (cancelStatus == null || !cancelStatus.name().startsWith("CANCEL")) {
            Slog.w(TAG, "Vibration cancel requested with bad status=" + cancelStatus + ", using CANCELLED_UNKNOWN_REASON to ensure cancellation.");
            cancelStatus = Vibration.Status.CANCELLED_BY_UNKNOWN_REASON;
        }
        synchronized (this.mLock) {
            if (immediate) {
            }
            Vibration.Status status = this.mSignalCancelStatus;
            if (status == null) {
                this.mSignalCancelImmediate |= immediate;
                if (status == null) {
                    this.mSignalCancelStatus = cancelStatus;
                }
                this.mLock.notify();
            }
        }
    }

    public void notifyVibratorComplete(int vibratorId) {
        synchronized (this.mLock) {
            this.mSignalVibratorsComplete.add(vibratorId);
            this.mLock.notify();
        }
    }

    public void notifySyncedVibrationComplete() {
        synchronized (this.mLock) {
            for (int i = 0; i < this.mVibrators.size(); i++) {
                this.mSignalVibratorsComplete.add(this.mVibrators.keyAt(i));
            }
            this.mLock.notify();
        }
    }

    private boolean hasPendingNotifySignalLocked() {
        if (Build.IS_DEBUGGABLE) {
            expectIsVibrationThread(true);
        }
        if (this.mSignalCancelStatus == this.mCancelStatus) {
            return (this.mSignalCancelImmediate && !this.mCancelledImmediately) || this.mSignalVibratorsComplete.size() > 0;
        }
        return true;
    }

    private void processAllNotifySignals() {
        if (Build.IS_DEBUGGABLE) {
            expectIsVibrationThread(true);
        }
        int[] vibratorsToProcess = null;
        Vibration.Status doCancelStatus = null;
        boolean doCancelImmediate = false;
        synchronized (this.mLock) {
            if (this.mSignalCancelImmediate) {
                if (this.mCancelledImmediately) {
                    Slog.wtf(TAG, "Immediate cancellation signal processed twice");
                }
                doCancelImmediate = true;
                doCancelStatus = this.mSignalCancelStatus;
            }
            Vibration.Status status = this.mSignalCancelStatus;
            if (status != this.mCancelStatus) {
                doCancelStatus = status;
            }
            if (!doCancelImmediate && this.mSignalVibratorsComplete.size() > 0) {
                vibratorsToProcess = this.mSignalVibratorsComplete.toArray();
                this.mSignalVibratorsComplete.clear();
            }
        }
        if (doCancelImmediate) {
            processCancelImmediately(doCancelStatus);
            return;
        }
        if (doCancelStatus != null) {
            processCancel(doCancelStatus);
        }
        if (vibratorsToProcess != null) {
            processVibratorsComplete(vibratorsToProcess);
        }
    }

    public void processCancel(Vibration.Status cancelStatus) {
        if (Build.IS_DEBUGGABLE) {
            expectIsVibrationThread(true);
        }
        this.mCancelStatus = cancelStatus;
        List<Step> cleanUpSteps = new ArrayList<>();
        while (true) {
            Step step = pollNext();
            if (step != null) {
                cleanUpSteps.addAll(step.cancel());
            } else {
                this.mPendingVibrateSteps = 0;
                this.mNextSteps.addAll(cleanUpSteps);
                return;
            }
        }
    }

    public void processCancelImmediately(Vibration.Status cancelStatus) {
        if (Build.IS_DEBUGGABLE) {
            expectIsVibrationThread(true);
        }
        this.mCancelledImmediately = true;
        this.mCancelStatus = cancelStatus;
        while (true) {
            Step step = pollNext();
            if (step != null) {
                step.cancelImmediately();
            } else {
                this.mPendingVibrateSteps = 0;
                return;
            }
        }
    }

    private void processVibratorsComplete(int[] vibratorsToProcess) {
        if (Build.IS_DEBUGGABLE) {
            expectIsVibrationThread(true);
        }
        for (int vibratorId : vibratorsToProcess) {
            Iterator<Step> it = this.mNextSteps.iterator();
            while (true) {
                if (it.hasNext()) {
                    Step step = it.next();
                    if (step.acceptVibratorCompleteCallback(vibratorId)) {
                        it.remove();
                        this.mPendingOnVibratorCompleteSteps.offer(step);
                        break;
                    }
                }
            }
        }
    }

    private static CombinedVibration.Sequential toSequential(CombinedVibration effect) {
        if (effect instanceof CombinedVibration.Sequential) {
            return (CombinedVibration.Sequential) effect;
        }
        return CombinedVibration.startSequential().addNext(effect).combine();
    }

    private static void expectIsVibrationThread(boolean isVibrationThread) {
        if ((Thread.currentThread() instanceof VibrationThread) != isVibrationThread) {
            Slog.wtfStack("VibrationStepConductor", "Thread caller assertion failed, expected isVibrationThread=" + isVibrationThread);
        }
    }
}
