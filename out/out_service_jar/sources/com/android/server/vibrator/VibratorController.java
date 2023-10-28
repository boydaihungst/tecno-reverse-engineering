package com.android.server.vibrator;

import android.hardware.usb.gadget.V1_2.GadgetFunction;
import android.os.Binder;
import android.os.DynamicEffectParam;
import android.os.IVibratorStateListener;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.Trace;
import android.os.VibratorInfo;
import android.os.vibrator.PrebakedSegment;
import android.os.vibrator.PrimitiveSegment;
import android.os.vibrator.RampSegment;
import android.util.Slog;
import java.util.function.Consumer;
import libcore.util.NativeAllocationRegistry;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class VibratorController {
    private static final String TAG = "VibratorController";
    private volatile float mCurrentAmplitude;
    private volatile boolean mIsUnderExternalControl;
    private volatile boolean mIsVibrating;
    private final Object mLock;
    private final NativeWrapper mNativeWrapper;
    private volatile VibratorInfo mVibratorInfo;
    private volatile boolean mVibratorInfoLoadSuccessful;
    private final RemoteCallbackList<IVibratorStateListener> mVibratorStateListeners;

    /* loaded from: classes2.dex */
    public interface OnVibrationCompleteListener {
        void onComplete(int i, long j);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public VibratorController(int vibratorId, OnVibrationCompleteListener listener) {
        this(vibratorId, listener, new NativeWrapper());
    }

    VibratorController(int vibratorId, OnVibrationCompleteListener listener, NativeWrapper nativeWrapper) {
        this.mLock = new Object();
        this.mVibratorStateListeners = new RemoteCallbackList<>();
        this.mNativeWrapper = nativeWrapper;
        nativeWrapper.init(vibratorId, listener);
        VibratorInfo.Builder vibratorInfoBuilder = new VibratorInfo.Builder(vibratorId);
        this.mVibratorInfoLoadSuccessful = nativeWrapper.getInfo(vibratorInfoBuilder);
        this.mVibratorInfo = vibratorInfoBuilder.build();
        if (!this.mVibratorInfoLoadSuccessful) {
            Slog.e(TAG, "Vibrator controller initialization failed to load some HAL info for vibrator " + vibratorId);
        }
    }

    public boolean registerVibratorStateListener(IVibratorStateListener listener) {
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                if (!this.mVibratorStateListeners.register(listener)) {
                    return false;
                }
                m7527x4660016e(listener, this.mIsVibrating);
                return true;
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public boolean unregisterVibratorStateListener(IVibratorStateListener listener) {
        long token = Binder.clearCallingIdentity();
        try {
            return this.mVibratorStateListeners.unregister(listener);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void reloadVibratorInfoIfNeeded() {
        if (this.mVibratorInfoLoadSuccessful) {
            return;
        }
        synchronized (this.mLock) {
            if (this.mVibratorInfoLoadSuccessful) {
                return;
            }
            int vibratorId = this.mVibratorInfo.getId();
            VibratorInfo.Builder vibratorInfoBuilder = new VibratorInfo.Builder(vibratorId);
            this.mVibratorInfoLoadSuccessful = this.mNativeWrapper.getInfo(vibratorInfoBuilder);
            this.mVibratorInfo = vibratorInfoBuilder.build();
            if (!this.mVibratorInfoLoadSuccessful) {
                Slog.e(TAG, "Failed retry of HAL getInfo for vibrator " + vibratorId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isVibratorInfoLoadSuccessful() {
        return this.mVibratorInfoLoadSuccessful;
    }

    public VibratorInfo getVibratorInfo() {
        return this.mVibratorInfo;
    }

    public boolean isVibrating() {
        return this.mIsVibrating;
    }

    public float getCurrentAmplitude() {
        return this.mCurrentAmplitude;
    }

    public boolean isUnderExternalControl() {
        return this.mIsUnderExternalControl;
    }

    public boolean hasCapability(long capability) {
        return this.mVibratorInfo.hasCapability(capability);
    }

    public boolean isAvailable() {
        boolean isAvailable;
        synchronized (this.mLock) {
            isAvailable = this.mNativeWrapper.isAvailable();
        }
        return isAvailable;
    }

    public void setExternalControl(boolean externalControl) {
        if (!this.mVibratorInfo.hasCapability(8L)) {
            return;
        }
        synchronized (this.mLock) {
            this.mIsUnderExternalControl = externalControl;
            this.mNativeWrapper.setExternalControl(externalControl);
        }
    }

    public void updateAlwaysOn(int id, PrebakedSegment prebaked) {
        if (!this.mVibratorInfo.hasCapability(64L)) {
            return;
        }
        synchronized (this.mLock) {
            if (prebaked == null) {
                this.mNativeWrapper.alwaysOnDisable(id);
            } else {
                this.mNativeWrapper.alwaysOnEnable(id, prebaked.getEffectId(), prebaked.getEffectStrength());
            }
        }
    }

    public void setAmplitude(float amplitude) {
        synchronized (this.mLock) {
            if (this.mVibratorInfo.hasCapability(4L)) {
                this.mNativeWrapper.setAmplitude(amplitude);
            }
            if (this.mIsVibrating) {
                this.mCurrentAmplitude = amplitude;
            }
        }
    }

    public long on(long milliseconds, long vibrationId) {
        long duration;
        synchronized (this.mLock) {
            duration = this.mNativeWrapper.on(milliseconds, vibrationId);
            if (duration > 0) {
                this.mCurrentAmplitude = -1.0f;
                notifyListenerOnVibrating(true);
            }
        }
        return duration;
    }

    public long on(PrebakedSegment prebaked, long vibrationId) {
        long duration;
        synchronized (this.mLock) {
            duration = this.mNativeWrapper.perform(prebaked.getEffectId(), prebaked.getEffectStrength(), vibrationId);
            if (duration > 0) {
                this.mCurrentAmplitude = -1.0f;
                notifyListenerOnVibrating(true);
            }
        }
        return duration;
    }

    public long on(PrimitiveSegment[] primitives, long vibrationId) {
        long duration;
        if (this.mVibratorInfo.hasCapability(32L)) {
            synchronized (this.mLock) {
                duration = this.mNativeWrapper.compose(primitives, vibrationId);
                if (duration > 0) {
                    this.mCurrentAmplitude = -1.0f;
                    notifyListenerOnVibrating(true);
                }
            }
            return duration;
        }
        return 0L;
    }

    public long on(RampSegment[] primitives, long vibrationId) {
        long duration;
        if (this.mVibratorInfo.hasCapability((long) GadgetFunction.NCM)) {
            synchronized (this.mLock) {
                int braking = this.mVibratorInfo.getDefaultBraking();
                duration = this.mNativeWrapper.composePwle(primitives, braking, vibrationId);
                if (duration > 0) {
                    this.mCurrentAmplitude = -1.0f;
                    notifyListenerOnVibrating(true);
                }
            }
            return duration;
        }
        return 0L;
    }

    public void off() {
        synchronized (this.mLock) {
            this.mNativeWrapper.off();
            this.mCurrentAmplitude = 0.0f;
            notifyListenerOnVibrating(false);
        }
    }

    public void doDynamicEffectOn(DynamicEffectParam eft) {
        Trace.traceBegin(8388608L, "doDynamicEffectOn");
        try {
            synchronized (this.mLock) {
                this.mNativeWrapper.doDynamicEffectOn(eft);
            }
        } finally {
            Trace.traceEnd(8388608L);
        }
    }

    public boolean isSupportDynamicEffect() {
        int state = this.mNativeWrapper.isSupportDynamicEffect();
        if (state != 0) {
            return false;
        }
        return true;
    }

    public void update_vib_info(int infocase, int info) {
        this.mNativeWrapper.update_vib_info(infocase, info);
    }

    public void stopDynamicEffect() {
        this.mNativeWrapper.stopDynamicEffect();
    }

    public void reset() {
        setExternalControl(false);
        off();
    }

    public String toString() {
        return "VibratorController{mVibratorInfo=" + this.mVibratorInfo + ", mVibratorInfoLoadSuccessful=" + this.mVibratorInfoLoadSuccessful + ", mIsVibrating=" + this.mIsVibrating + ", mCurrentAmplitude=" + this.mCurrentAmplitude + ", mIsUnderExternalControl=" + this.mIsUnderExternalControl + ", mVibratorStateListeners count=" + this.mVibratorStateListeners.getRegisteredCallbackCount() + '}';
    }

    private void notifyListenerOnVibrating(final boolean isVibrating) {
        if (this.mIsVibrating != isVibrating) {
            this.mIsVibrating = isVibrating;
            this.mVibratorStateListeners.broadcast(new Consumer() { // from class: com.android.server.vibrator.VibratorController$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    VibratorController.this.m7527x4660016e(isVibrating, (IVibratorStateListener) obj);
                }
            });
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: notifyStateListener */
    public void m7527x4660016e(IVibratorStateListener listener, boolean isVibrating) {
        try {
            listener.onVibrating(isVibrating);
        } catch (RemoteException | RuntimeException e) {
            Slog.e(TAG, "Vibrator state listener failed to call", e);
        }
    }

    /* loaded from: classes2.dex */
    public static class NativeWrapper {
        private long mNativePtr = 0;

        private static native void alwaysOnDisable(long j, long j2);

        private static native void alwaysOnEnable(long j, long j2, long j3, long j4);

        private static native int dynamicEffectAvailable();

        private static native void dynamicEffectOff();

        private static native void dynamicEffectOn(DynamicEffectParam dynamicEffectParam);

        private static native void dynamicEffectUpdate_vib_info(int i, int i2);

        private static native boolean getInfo(long j, VibratorInfo.Builder builder);

        private static native long getNativeFinalizer();

        private static native boolean isAvailable(long j);

        private static native long nativeInit(int i, OnVibrationCompleteListener onVibrationCompleteListener);

        private static native void off(long j);

        private static native long on(long j, long j2, long j3);

        private static native long performComposedEffect(long j, PrimitiveSegment[] primitiveSegmentArr, long j2);

        private static native long performEffect(long j, long j2, long j3, long j4);

        private static native long performPwleEffect(long j, RampSegment[] rampSegmentArr, int i, long j2);

        private static native void setAmplitude(long j, float f);

        private static native void setExternalControl(long j, boolean z);

        public void init(int vibratorId, OnVibrationCompleteListener listener) {
            this.mNativePtr = nativeInit(vibratorId, listener);
            long finalizerPtr = getNativeFinalizer();
            if (finalizerPtr != 0) {
                NativeAllocationRegistry registry = NativeAllocationRegistry.createMalloced(VibratorController.class.getClassLoader(), finalizerPtr);
                registry.registerNativeAllocation(this, this.mNativePtr);
            }
        }

        public boolean isAvailable() {
            return isAvailable(this.mNativePtr);
        }

        public long on(long milliseconds, long vibrationId) {
            return on(this.mNativePtr, milliseconds, vibrationId);
        }

        public void off() {
            off(this.mNativePtr);
        }

        public void setAmplitude(float amplitude) {
            setAmplitude(this.mNativePtr, amplitude);
        }

        public long perform(long effect, long strength, long vibrationId) {
            return performEffect(this.mNativePtr, effect, strength, vibrationId);
        }

        public long compose(PrimitiveSegment[] primitives, long vibrationId) {
            return performComposedEffect(this.mNativePtr, primitives, vibrationId);
        }

        public long composePwle(RampSegment[] primitives, int braking, long vibrationId) {
            return performPwleEffect(this.mNativePtr, primitives, braking, vibrationId);
        }

        public void setExternalControl(boolean enabled) {
            setExternalControl(this.mNativePtr, enabled);
        }

        public void alwaysOnEnable(long id, long effect, long strength) {
            alwaysOnEnable(this.mNativePtr, id, effect, strength);
        }

        public void alwaysOnDisable(long id) {
            alwaysOnDisable(this.mNativePtr, id);
        }

        public boolean getInfo(VibratorInfo.Builder infoBuilder) {
            return getInfo(this.mNativePtr, infoBuilder);
        }

        public void doDynamicEffectOn(DynamicEffectParam eft) {
            dynamicEffectOn(eft);
        }

        public int isSupportDynamicEffect() {
            return dynamicEffectAvailable();
        }

        public void update_vib_info(int infocase, int info) {
            dynamicEffectUpdate_vib_info(infocase, info);
        }

        public void stopDynamicEffect() {
            dynamicEffectOff();
        }
    }
}
