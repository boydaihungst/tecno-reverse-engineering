package com.android.server.vibrator;

import android.os.CombinedVibration;
import android.os.DynamicEffect;
import android.os.DynamicEffectParam;
import android.os.SystemClock;
import android.os.Trace;
import android.os.VibrationEffect;
import android.os.VibratorInfo;
import android.os.vibrator.PrebakedSegment;
import android.os.vibrator.PrimitiveSegment;
import android.os.vibrator.StepSegment;
import android.os.vibrator.VibrationEffectSegment;
import android.util.SparseArray;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes2.dex */
final class StartSequentialEffectStep extends Step {
    public final int currentIndex;
    private long mVibratorsOnMaxDuration;
    public final CombinedVibration.Sequential sequentialEffect;

    /* JADX INFO: Access modifiers changed from: package-private */
    public StartSequentialEffectStep(VibrationStepConductor conductor, CombinedVibration.Sequential effect) {
        this(conductor, SystemClock.uptimeMillis() + ((Integer) effect.getDelays().get(0)).intValue(), effect, 0);
    }

    private StartSequentialEffectStep(VibrationStepConductor conductor, long startTime, CombinedVibration.Sequential effect, int index) {
        super(conductor, startTime);
        this.sequentialEffect = effect;
        this.currentIndex = index;
    }

    @Override // com.android.server.vibrator.Step
    public long getVibratorOnDuration() {
        return this.mVibratorsOnMaxDuration;
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IGET, CMP_L]}, finally: {[IGET, CMP_L, INVOKE, INVOKE, INVOKE, IF, CONSTRUCTOR, INVOKE, INVOKE, INVOKE, IF, INVOKE, IF, CMP_L, INVOKE, IF] complete} */
    @Override // com.android.server.vibrator.Step
    public List<Step> play() {
        Trace.traceBegin(8388608L, "StartSequentialEffectStep");
        List<Step> nextSteps = new ArrayList<>();
        this.mVibratorsOnMaxDuration = -1L;
        try {
            CombinedVibration.Mono mono = (CombinedVibration) this.sequentialEffect.getEffects().get(this.currentIndex);
            boolean isDynamicEffect = false;
            if (mono instanceof CombinedVibration.Mono) {
                DynamicEffect effect = mono.getEffect();
                if (effect instanceof DynamicEffect) {
                    SparseArray<VibratorController> vibrators = this.conductor.getVibrators();
                    isDynamicEffect = true;
                    DynamicEffectParam param = effect.getDynamicEffectParam();
                    if (param != null) {
                        for (int i = 0; i < vibrators.size(); i++) {
                            VibratorController controller = vibrators.valueAt(i);
                            controller.doDynamicEffectOn(param);
                        }
                    }
                    this.mVibratorsOnMaxDuration = effect.getDuration();
                }
            }
            if (!isDynamicEffect) {
                DeviceEffectMap effectMapping = createEffectToVibratorMapping(mono);
                if (effectMapping == null) {
                    return nextSteps;
                }
                long startVibrating = startVibrating(effectMapping, nextSteps);
                this.mVibratorsOnMaxDuration = startVibrating;
                if (startVibrating > 0) {
                    this.conductor.vibratorManagerHooks.noteVibratorOn(this.conductor.getVibration().uid, this.mVibratorsOnMaxDuration);
                }
            }
            long j = this.mVibratorsOnMaxDuration;
            if (j >= 0) {
                Step nextStep = j > 0 ? new FinishSequentialEffectStep(this) : nextStep();
                if (nextStep != null) {
                    nextSteps.add(nextStep);
                }
            }
            Trace.traceEnd(8388608L);
            return nextSteps;
        } finally {
            long j2 = this.mVibratorsOnMaxDuration;
            if (j2 >= 0) {
                Step nextStep2 = j2 > 0 ? new FinishSequentialEffectStep(this) : nextStep();
                if (nextStep2 != null) {
                    nextSteps.add(nextStep2);
                }
            }
            Trace.traceEnd(8388608L);
        }
    }

    @Override // com.android.server.vibrator.Step
    public List<Step> cancel() {
        return VibrationStepConductor.EMPTY_STEP_LIST;
    }

    @Override // com.android.server.vibrator.Step
    public void cancelImmediately() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Step nextStep() {
        int nextIndex = this.currentIndex + 1;
        if (nextIndex >= this.sequentialEffect.getEffects().size()) {
            return null;
        }
        long nextEffectDelay = ((Integer) this.sequentialEffect.getDelays().get(nextIndex)).intValue();
        long nextStartTime = SystemClock.uptimeMillis() + nextEffectDelay;
        return new StartSequentialEffectStep(this.conductor, nextStartTime, this.sequentialEffect, nextIndex);
    }

    private DeviceEffectMap createEffectToVibratorMapping(CombinedVibration effect) {
        if (effect instanceof CombinedVibration.Mono) {
            return new DeviceEffectMap((CombinedVibration.Mono) effect);
        }
        if (effect instanceof CombinedVibration.Stereo) {
            return new DeviceEffectMap((CombinedVibration.Stereo) effect);
        }
        return null;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [259=4, 266=6] */
    private long startVibrating(DeviceEffectMap effectMapping, List<Step> nextSteps) {
        boolean hasPrepared;
        int vibratorCount = effectMapping.size();
        if (vibratorCount == 0) {
            return 0L;
        }
        AbstractVibratorStep[] steps = new AbstractVibratorStep[vibratorCount];
        long vibrationStartTime = SystemClock.uptimeMillis();
        for (int i = 0; i < vibratorCount; i++) {
            steps[i] = this.conductor.nextVibrateStep(vibrationStartTime, this.conductor.getVibrators().get(effectMapping.vibratorIdAt(i)), effectMapping.effectAt(i), 0, 0L);
        }
        int i2 = steps.length;
        if (i2 == 1) {
            return startVibrating(steps[0], nextSteps);
        }
        boolean hasPrepared2 = false;
        boolean hasTriggered = false;
        long maxDuration = 0;
        try {
            hasPrepared2 = this.conductor.vibratorManagerHooks.prepareSyncedVibration(effectMapping.getRequiredSyncCapabilities(), effectMapping.getVibratorIds());
            try {
                int length = steps.length;
                int i3 = 0;
                while (i3 < length) {
                    AbstractVibratorStep step = steps[i3];
                    long duration = startVibrating(step, nextSteps);
                    hasPrepared = hasPrepared2;
                    if (duration < 0) {
                        if (hasPrepared && 0 == 0) {
                            this.conductor.vibratorManagerHooks.cancelSyncedVibration();
                            nextSteps.clear();
                        } else if (-1 < 0) {
                            for (int i4 = nextSteps.size() - 1; i4 >= 0; i4--) {
                                nextSteps.remove(i4).cancelImmediately();
                            }
                        }
                        return -1L;
                    }
                    try {
                        maxDuration = Math.max(maxDuration, duration);
                        i3++;
                        hasPrepared2 = hasPrepared;
                    } catch (Throwable th) {
                        th = th;
                        hasPrepared2 = hasPrepared;
                        if (hasPrepared2 && 0 == 0) {
                            this.conductor.vibratorManagerHooks.cancelSyncedVibration();
                            nextSteps.clear();
                        } else if (maxDuration < 0) {
                            for (int i5 = nextSteps.size() - 1; i5 >= 0; i5--) {
                                nextSteps.remove(i5).cancelImmediately();
                            }
                        }
                        throw th;
                    }
                }
                hasPrepared = hasPrepared2;
                if (hasPrepared && maxDuration > 0) {
                    hasTriggered = this.conductor.vibratorManagerHooks.triggerSyncedVibration(getVibration().id);
                }
                if (hasPrepared && !hasTriggered) {
                    this.conductor.vibratorManagerHooks.cancelSyncedVibration();
                    nextSteps.clear();
                } else if (maxDuration < 0) {
                    for (int i6 = nextSteps.size() - 1; i6 >= 0; i6--) {
                        nextSteps.remove(i6).cancelImmediately();
                    }
                }
                return maxDuration;
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (Throwable th3) {
            th = th3;
        }
    }

    private long startVibrating(AbstractVibratorStep step, List<Step> nextSteps) {
        nextSteps.addAll(step.play());
        long stepDuration = step.getVibratorOnDuration();
        if (stepDuration < 0) {
            return stepDuration;
        }
        return Math.max(stepDuration, step.effect.getDuration());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class DeviceEffectMap {
        private final long mRequiredSyncCapabilities;
        private final SparseArray<VibrationEffect.Composed> mVibratorEffects;
        private final int[] mVibratorIds;

        DeviceEffectMap(CombinedVibration.Mono mono) {
            SparseArray<VibratorController> vibrators = StartSequentialEffectStep.this.conductor.getVibrators();
            this.mVibratorEffects = new SparseArray<>(vibrators.size());
            this.mVibratorIds = new int[vibrators.size()];
            for (int i = 0; i < vibrators.size(); i++) {
                int vibratorId = vibrators.keyAt(i);
                VibratorInfo vibratorInfo = vibrators.valueAt(i).getVibratorInfo();
                VibrationEffect.Composed apply = StartSequentialEffectStep.this.conductor.deviceEffectAdapter.apply(mono.getEffect(), vibratorInfo);
                if (apply instanceof VibrationEffect.Composed) {
                    this.mVibratorEffects.put(vibratorId, apply);
                    this.mVibratorIds[i] = vibratorId;
                }
            }
            this.mRequiredSyncCapabilities = calculateRequiredSyncCapabilities(this.mVibratorEffects);
        }

        DeviceEffectMap(CombinedVibration.Stereo stereo) {
            SparseArray<VibratorController> vibrators = StartSequentialEffectStep.this.conductor.getVibrators();
            SparseArray<VibrationEffect> stereoEffects = stereo.getEffects();
            this.mVibratorEffects = new SparseArray<>();
            for (int i = 0; i < stereoEffects.size(); i++) {
                int vibratorId = stereoEffects.keyAt(i);
                if (vibrators.contains(vibratorId)) {
                    VibratorInfo vibratorInfo = vibrators.valueAt(i).getVibratorInfo();
                    VibrationEffect.Composed apply = StartSequentialEffectStep.this.conductor.deviceEffectAdapter.apply(stereoEffects.valueAt(i), vibratorInfo);
                    if (apply instanceof VibrationEffect.Composed) {
                        this.mVibratorEffects.put(vibratorId, apply);
                    }
                }
            }
            this.mVibratorIds = new int[this.mVibratorEffects.size()];
            for (int i2 = 0; i2 < this.mVibratorEffects.size(); i2++) {
                this.mVibratorIds[i2] = this.mVibratorEffects.keyAt(i2);
            }
            this.mRequiredSyncCapabilities = calculateRequiredSyncCapabilities(this.mVibratorEffects);
        }

        public int size() {
            return this.mVibratorIds.length;
        }

        public long getRequiredSyncCapabilities() {
            return this.mRequiredSyncCapabilities;
        }

        public int[] getVibratorIds() {
            return this.mVibratorIds;
        }

        public int vibratorIdAt(int index) {
            return this.mVibratorEffects.keyAt(index);
        }

        public VibrationEffect.Composed effectAt(int index) {
            return this.mVibratorEffects.valueAt(index);
        }

        private long calculateRequiredSyncCapabilities(SparseArray<VibrationEffect.Composed> effects) {
            long prepareCap = 0;
            for (int i = 0; i < effects.size(); i++) {
                VibrationEffectSegment firstSegment = (VibrationEffectSegment) effects.valueAt(i).getSegments().get(0);
                if (firstSegment instanceof StepSegment) {
                    prepareCap |= 2;
                } else if (firstSegment instanceof PrebakedSegment) {
                    prepareCap |= 4;
                } else if (firstSegment instanceof PrimitiveSegment) {
                    prepareCap |= 8;
                }
            }
            int triggerCap = 0;
            if (requireMixedTriggerCapability(prepareCap, 2L)) {
                triggerCap = 0 | 16;
            }
            if (requireMixedTriggerCapability(prepareCap, 4L)) {
                triggerCap |= 32;
            }
            if (requireMixedTriggerCapability(prepareCap, 8L)) {
                triggerCap |= 64;
            }
            return 1 | prepareCap | triggerCap;
        }

        private boolean requireMixedTriggerCapability(long prepareCapabilities, long capability) {
            return ((prepareCapabilities & capability) == 0 || ((~capability) & prepareCapabilities) == 0) ? false : true;
        }
    }
}
