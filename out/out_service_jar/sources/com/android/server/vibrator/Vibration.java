package com.android.server.vibrator;

import android.os.CombinedVibration;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.vibrator.PrebakedSegment;
import android.os.vibrator.PrimitiveSegment;
import android.os.vibrator.RampSegment;
import android.os.vibrator.StepSegment;
import android.os.vibrator.VibrationEffectSegment;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import com.android.server.job.controllers.JobStatus;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class Vibration {
    private static final SimpleDateFormat DEBUG_DATE_FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
    private static final String TAG = "Vibration";
    public final VibrationAttributes attrs;
    public final long id;
    private CombinedVibration mEffect;
    private long mEndTimeDebug;
    private long mEndUptimeMillis;
    private CombinedVibration mOriginalEffect;
    public final String opPkg;
    public final String reason;
    public final IBinder token;
    public final int uid;
    public final SparseArray<VibrationEffect> mFallbacks = new SparseArray<>();
    private final CountDownLatch mCompletionLatch = new CountDownLatch(1);
    public final long startUptimeMillis = SystemClock.uptimeMillis();
    private final long mStartTimeDebug = System.currentTimeMillis();
    private Status mStatus = Status.RUNNING;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public enum Status {
        RUNNING,
        FINISHED,
        FINISHED_UNEXPECTED,
        FORWARDED_TO_INPUT_DEVICES,
        CANCELLED_BINDER_DIED,
        CANCELLED_BY_SCREEN_OFF,
        CANCELLED_BY_SETTINGS_UPDATE,
        CANCELLED_BY_USER,
        CANCELLED_BY_UNKNOWN_REASON,
        CANCELLED_SUPERSEDED,
        IGNORED_ERROR_APP_OPS,
        IGNORED_ERROR_CANCELLING,
        IGNORED_ERROR_SCHEDULING,
        IGNORED_ERROR_TOKEN,
        IGNORED_APP_OPS,
        IGNORED_BACKGROUND,
        IGNORED_UNKNOWN_VIBRATION,
        IGNORED_UNSUPPORTED,
        IGNORED_FOR_ALARM,
        IGNORED_FOR_EXTERNAL,
        IGNORED_FOR_ONGOING,
        IGNORED_FOR_POWER,
        IGNORED_FOR_RINGER_MODE,
        IGNORED_FOR_RINGTONE,
        IGNORED_FOR_SETTINGS,
        IGNORED_SUPERSEDED
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Vibration(IBinder token, int id, CombinedVibration effect, VibrationAttributes attrs, int uid, String opPkg, String reason) {
        this.token = token;
        this.mEffect = effect;
        this.id = id;
        this.attrs = attrs;
        this.uid = uid;
        this.opPkg = opPkg;
        this.reason = reason;
    }

    public void end(Status status) {
        if (hasEnded()) {
            return;
        }
        this.mStatus = status;
        this.mEndUptimeMillis = SystemClock.uptimeMillis();
        this.mEndTimeDebug = System.currentTimeMillis();
        this.mCompletionLatch.countDown();
    }

    public void waitForEnd() throws InterruptedException {
        this.mCompletionLatch.await();
    }

    public VibrationEffect getFallback(int effectId) {
        return this.mFallbacks.get(effectId);
    }

    public void addFallback(int effectId, VibrationEffect effect) {
        this.mFallbacks.put(effectId, effect);
    }

    public void updateEffects(Function<VibrationEffect, VibrationEffect> updateFn) {
        CombinedVibration newEffect = transformCombinedEffect(this.mEffect, updateFn);
        if (!newEffect.equals(this.mEffect)) {
            if (this.mOriginalEffect == null) {
                this.mOriginalEffect = this.mEffect;
            }
            this.mEffect = newEffect;
        }
        for (int i = 0; i < this.mFallbacks.size(); i++) {
            SparseArray<VibrationEffect> sparseArray = this.mFallbacks;
            sparseArray.setValueAt(i, updateFn.apply(sparseArray.valueAt(i)));
        }
    }

    private static CombinedVibration transformCombinedEffect(CombinedVibration combinedEffect, Function<VibrationEffect, VibrationEffect> fn) {
        if (combinedEffect instanceof CombinedVibration.Mono) {
            VibrationEffect effect = ((CombinedVibration.Mono) combinedEffect).getEffect();
            return CombinedVibration.createParallel(fn.apply(effect));
        } else if (combinedEffect instanceof CombinedVibration.Stereo) {
            SparseArray<VibrationEffect> effects = ((CombinedVibration.Stereo) combinedEffect).getEffects();
            CombinedVibration.ParallelCombination combination = CombinedVibration.startParallel();
            for (int i = 0; i < effects.size(); i++) {
                combination.addVibrator(effects.keyAt(i), fn.apply(effects.valueAt(i)));
            }
            return combination.combine();
        } else if (combinedEffect instanceof CombinedVibration.Sequential) {
            List<CombinedVibration> effects2 = ((CombinedVibration.Sequential) combinedEffect).getEffects();
            CombinedVibration.SequentialCombination combination2 = CombinedVibration.startSequential();
            for (CombinedVibration effect2 : effects2) {
                combination2.addNext(transformCombinedEffect(effect2, fn));
            }
            return combination2.combine();
        } else {
            return combinedEffect;
        }
    }

    public boolean hasEnded() {
        return this.mStatus != Status.RUNNING;
    }

    public boolean isRepeating() {
        return this.mEffect.getDuration() == JobStatus.NO_LATEST_RUNTIME;
    }

    public CombinedVibration getEffect() {
        return this.mEffect;
    }

    public DebugInfo getDebugInfo() {
        long durationMs = hasEnded() ? this.mEndUptimeMillis - this.startUptimeMillis : -1L;
        return new DebugInfo(this.mStartTimeDebug, this.mEndTimeDebug, durationMs, this.mEffect, this.mOriginalEffect, 0.0f, this.attrs, this.uid, this.opPkg, this.reason, this.mStatus);
    }

    /* loaded from: classes2.dex */
    static final class DebugInfo {
        private final VibrationAttributes mAttrs;
        private final long mDurationMs;
        private final CombinedVibration mEffect;
        private final long mEndTimeDebug;
        private final String mOpPkg;
        private final CombinedVibration mOriginalEffect;
        private final String mReason;
        private final float mScale;
        private final long mStartTimeDebug;
        private final Status mStatus;
        private final int mUid;

        /* JADX INFO: Access modifiers changed from: package-private */
        public DebugInfo(long startTimeDebug, long endTimeDebug, long durationMs, CombinedVibration effect, CombinedVibration originalEffect, float scale, VibrationAttributes attrs, int uid, String opPkg, String reason, Status status) {
            this.mStartTimeDebug = startTimeDebug;
            this.mEndTimeDebug = endTimeDebug;
            this.mDurationMs = durationMs;
            this.mEffect = effect;
            this.mOriginalEffect = originalEffect;
            this.mScale = scale;
            this.mAttrs = attrs;
            this.mUid = uid;
            this.mOpPkg = opPkg;
            this.mReason = reason;
            this.mStatus = status;
        }

        public String toString() {
            return "startTime: " + Vibration.DEBUG_DATE_FORMAT.format(new Date(this.mStartTimeDebug)) + ", endTime: " + (this.mEndTimeDebug == 0 ? null : Vibration.DEBUG_DATE_FORMAT.format(new Date(this.mEndTimeDebug))) + ", durationMs: " + this.mDurationMs + ", status: " + this.mStatus.name().toLowerCase() + ", effect: " + this.mEffect + ", originalEffect: " + this.mOriginalEffect + ", scale: " + String.format("%.2f", Float.valueOf(this.mScale)) + ", attrs: " + this.mAttrs + ", uid: " + this.mUid + ", opPkg: " + this.mOpPkg + ", reason: " + this.mReason;
        }

        public void dumpProto(ProtoOutputStream proto, long fieldId) {
            long token = proto.start(fieldId);
            proto.write(1112396529665L, this.mStartTimeDebug);
            proto.write(1112396529666L, this.mEndTimeDebug);
            proto.write(1112396529671L, this.mDurationMs);
            proto.write(1120986464262L, this.mStatus.ordinal());
            long attrsToken = proto.start(1146756268037L);
            proto.write(CompanionMessage.MESSAGE_ID, this.mAttrs.getUsage());
            proto.write(1120986464258L, this.mAttrs.getAudioUsage());
            proto.write(1120986464259L, this.mAttrs.getFlags());
            proto.end(attrsToken);
            CombinedVibration combinedVibration = this.mEffect;
            if (combinedVibration != null) {
                dumpEffect(proto, 1146756268035L, combinedVibration);
            }
            CombinedVibration combinedVibration2 = this.mOriginalEffect;
            if (combinedVibration2 != null) {
                dumpEffect(proto, 1146756268036L, combinedVibration2);
            }
            proto.end(token);
        }

        private void dumpEffect(ProtoOutputStream proto, long fieldId, CombinedVibration effect) {
            dumpEffect(proto, fieldId, (CombinedVibration.Sequential) CombinedVibration.startSequential().addNext(effect).combine());
        }

        private void dumpEffect(ProtoOutputStream proto, long fieldId, CombinedVibration.Sequential effect) {
            long token = proto.start(fieldId);
            for (int i = 0; i < effect.getEffects().size(); i++) {
                CombinedVibration nestedEffect = (CombinedVibration) effect.getEffects().get(i);
                if (nestedEffect instanceof CombinedVibration.Mono) {
                    dumpEffect(proto, CompanionAppsPermissions.APP_PERMISSIONS, (CombinedVibration.Mono) nestedEffect);
                } else if (nestedEffect instanceof CombinedVibration.Stereo) {
                    dumpEffect(proto, CompanionAppsPermissions.APP_PERMISSIONS, (CombinedVibration.Stereo) nestedEffect);
                }
                proto.write(2220498092034L, ((Integer) effect.getDelays().get(i)).intValue());
            }
            proto.end(token);
        }

        private void dumpEffect(ProtoOutputStream proto, long fieldId, CombinedVibration.Mono effect) {
            long token = proto.start(fieldId);
            dumpEffect(proto, CompanionAppsPermissions.APP_PERMISSIONS, effect.getEffect());
            proto.end(token);
        }

        private void dumpEffect(ProtoOutputStream proto, long fieldId, CombinedVibration.Stereo effect) {
            long token = proto.start(fieldId);
            for (int i = 0; i < effect.getEffects().size(); i++) {
                proto.write(2220498092034L, effect.getEffects().keyAt(i));
                dumpEffect(proto, CompanionAppsPermissions.APP_PERMISSIONS, (VibrationEffect) effect.getEffects().valueAt(i));
            }
            proto.end(token);
        }

        private void dumpEffect(ProtoOutputStream proto, long fieldId, VibrationEffect effect) {
            if (!(effect instanceof VibrationEffect.Composed)) {
                return;
            }
            long token = proto.start(fieldId);
            VibrationEffect.Composed composed = (VibrationEffect.Composed) effect;
            for (VibrationEffectSegment segment : composed.getSegments()) {
                dumpEffect(proto, 1146756268033L, segment);
            }
            proto.write(1120986464258L, composed.getRepeatIndex());
            proto.end(token);
        }

        private void dumpEffect(ProtoOutputStream proto, long fieldId, VibrationEffectSegment segment) {
            long token = proto.start(fieldId);
            if (segment instanceof StepSegment) {
                dumpEffect(proto, 1146756268035L, (StepSegment) segment);
            } else if (segment instanceof RampSegment) {
                dumpEffect(proto, 1146756268036L, (RampSegment) segment);
            } else if (segment instanceof PrebakedSegment) {
                dumpEffect(proto, 1146756268033L, (PrebakedSegment) segment);
            } else if (segment instanceof PrimitiveSegment) {
                dumpEffect(proto, 1146756268034L, (PrimitiveSegment) segment);
            }
            proto.end(token);
        }

        private void dumpEffect(ProtoOutputStream proto, long fieldId, StepSegment segment) {
            long token = proto.start(fieldId);
            proto.write(CompanionMessage.MESSAGE_ID, segment.getDuration());
            proto.write(1108101562370L, segment.getAmplitude());
            proto.write(1108101562371L, segment.getFrequencyHz());
            proto.end(token);
        }

        private void dumpEffect(ProtoOutputStream proto, long fieldId, RampSegment segment) {
            long token = proto.start(fieldId);
            proto.write(CompanionMessage.MESSAGE_ID, segment.getDuration());
            proto.write(1108101562370L, segment.getStartAmplitude());
            proto.write(1108101562371L, segment.getEndAmplitude());
            proto.write(1108101562372L, segment.getStartFrequencyHz());
            proto.write(1108101562373L, segment.getEndFrequencyHz());
            proto.end(token);
        }

        private void dumpEffect(ProtoOutputStream proto, long fieldId, PrebakedSegment segment) {
            long token = proto.start(fieldId);
            proto.write(CompanionMessage.MESSAGE_ID, segment.getEffectId());
            proto.write(1120986464258L, segment.getEffectStrength());
            proto.write(1120986464259L, segment.shouldFallback());
            proto.end(token);
        }

        private void dumpEffect(ProtoOutputStream proto, long fieldId, PrimitiveSegment segment) {
            long token = proto.start(fieldId);
            proto.write(CompanionMessage.MESSAGE_ID, segment.getPrimitiveId());
            proto.write(1108101562370L, segment.getScale());
            proto.write(1120986464259L, segment.getDelay());
            proto.end(token);
        }
    }
}
