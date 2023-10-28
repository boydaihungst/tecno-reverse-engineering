package com.android.server.soundtrigger_middleware;

import android.hardware.soundtrigger.V2_0.ISoundTriggerHw;
import android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback;
import android.hardware.soundtrigger.V2_1.ISoundTriggerHw;
import android.hardware.soundtrigger.V2_1.ISoundTriggerHwCallback;
import android.hardware.soundtrigger.V2_3.ISoundTriggerHw;
import android.hardware.soundtrigger.V2_3.OptionalModelParameterRange;
import android.media.soundtrigger.ModelParameterRange;
import android.media.soundtrigger.PhraseSoundModel;
import android.media.soundtrigger.Properties;
import android.media.soundtrigger.RecognitionConfig;
import android.media.soundtrigger.SoundModel;
import android.os.IBinder;
import android.os.IHwBinder;
import android.os.RemoteException;
import android.system.OsConstants;
import android.util.Log;
import com.android.server.soundtrigger_middleware.ISoundTriggerHal;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
/* loaded from: classes2.dex */
final class SoundTriggerHw2Compat implements ISoundTriggerHal {
    private static final String TAG = "SoundTriggerHw2Compat";
    private final IHwBinder mBinder;
    private final Properties mProperties;
    private final Runnable mRebootRunnable;
    private ISoundTriggerHw mUnderlying_2_0;
    private android.hardware.soundtrigger.V2_1.ISoundTriggerHw mUnderlying_2_1;
    private android.hardware.soundtrigger.V2_2.ISoundTriggerHw mUnderlying_2_2;
    private android.hardware.soundtrigger.V2_3.ISoundTriggerHw mUnderlying_2_3;
    private final ConcurrentMap<Integer, ISoundTriggerHal.ModelCallback> mModelCallbacks = new ConcurrentHashMap();
    private final Map<IBinder.DeathRecipient, IHwBinder.DeathRecipient> mDeathRecipientMap = new HashMap();

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ISoundTriggerHal create(ISoundTriggerHw underlying, Runnable rebootRunnable, ICaptureStateNotifier notifier) {
        return create(underlying.asBinder(), rebootRunnable, notifier);
    }

    static ISoundTriggerHal create(IHwBinder binder, Runnable rebootRunnable, ICaptureStateNotifier notifier) {
        SoundTriggerHw2Compat compat = new SoundTriggerHw2Compat(binder, rebootRunnable);
        ISoundTriggerHal result = new SoundTriggerHalMaxModelLimiter(compat, compat.mProperties.maxSoundModels);
        return !compat.mProperties.concurrentCapture ? new SoundTriggerHalConcurrentCaptureHandler(result, notifier) : result;
    }

    private SoundTriggerHw2Compat(IHwBinder binder, Runnable rebootRunnable) {
        this.mRebootRunnable = (Runnable) Objects.requireNonNull(rebootRunnable);
        this.mBinder = (IHwBinder) Objects.requireNonNull(binder);
        initUnderlying(binder);
        this.mProperties = (Properties) Objects.requireNonNull(getPropertiesInternal());
    }

    private void initUnderlying(IHwBinder binder) {
        android.hardware.soundtrigger.V2_3.ISoundTriggerHw as2_3 = android.hardware.soundtrigger.V2_3.ISoundTriggerHw.asInterface(binder);
        if (as2_3 != null) {
            this.mUnderlying_2_3 = as2_3;
            this.mUnderlying_2_2 = as2_3;
            this.mUnderlying_2_1 = as2_3;
            this.mUnderlying_2_0 = as2_3;
            return;
        }
        android.hardware.soundtrigger.V2_2.ISoundTriggerHw as2_2 = android.hardware.soundtrigger.V2_2.ISoundTriggerHw.asInterface(binder);
        if (as2_2 != null) {
            this.mUnderlying_2_2 = as2_2;
            this.mUnderlying_2_1 = as2_2;
            this.mUnderlying_2_0 = as2_2;
            this.mUnderlying_2_3 = null;
            return;
        }
        android.hardware.soundtrigger.V2_1.ISoundTriggerHw as2_1 = android.hardware.soundtrigger.V2_1.ISoundTriggerHw.asInterface(binder);
        if (as2_1 != null) {
            this.mUnderlying_2_1 = as2_1;
            this.mUnderlying_2_0 = as2_1;
            this.mUnderlying_2_3 = null;
            this.mUnderlying_2_2 = null;
            return;
        }
        ISoundTriggerHw as2_0 = ISoundTriggerHw.asInterface(binder);
        if (as2_0 != null) {
            this.mUnderlying_2_0 = as2_0;
            this.mUnderlying_2_3 = null;
            this.mUnderlying_2_2 = null;
            this.mUnderlying_2_1 = null;
            return;
        }
        throw new RuntimeException("Binder doesn't support ISoundTriggerHw@2.0");
    }

    private static void handleHalStatus(int status, String methodName) {
        if (status != 0) {
            throw new HalException(status, methodName);
        }
    }

    private static void handleHalStatusAllowBusy(int status, String methodName) {
        if (status == (-OsConstants.EBUSY)) {
            throw new RecoverableException(1);
        }
        handleHalStatus(status, methodName);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void reboot() {
        this.mRebootRunnable.run();
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void detach() {
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public Properties getProperties() {
        return this.mProperties;
    }

    private Properties getPropertiesInternal() {
        try {
            final AtomicInteger retval = new AtomicInteger(-1);
            final AtomicReference<android.hardware.soundtrigger.V2_3.Properties> properties = new AtomicReference<>();
            try {
                as2_3().getProperties_2_3(new ISoundTriggerHw.getProperties_2_3Callback() { // from class: com.android.server.soundtrigger_middleware.SoundTriggerHw2Compat$$ExternalSyntheticLambda1
                    @Override // android.hardware.soundtrigger.V2_3.ISoundTriggerHw.getProperties_2_3Callback
                    public final void onValues(int i, android.hardware.soundtrigger.V2_3.Properties properties2) {
                        SoundTriggerHw2Compat.lambda$getPropertiesInternal$0(retval, properties, i, properties2);
                    }
                });
                handleHalStatus(retval.get(), "getProperties_2_3");
                return ConversionUtil.hidl2aidlProperties(properties.get());
            } catch (NotSupported e) {
                return getProperties_2_0();
            }
        } catch (RemoteException e2) {
            throw e2.rethrowAsRuntimeException();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getPropertiesInternal$0(AtomicInteger retval, AtomicReference properties, int r, android.hardware.soundtrigger.V2_3.Properties p) {
        retval.set(r);
        properties.set(p);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void registerCallback(ISoundTriggerHal.GlobalCallback callback) {
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IGET]}, finally: {[IGET, INVOKE, MOVE_EXCEPTION, IGET, INVOKE, INVOKE, MOVE_EXCEPTION, IF] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [236=4] */
    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public int loadSoundModel(SoundModel soundModel, ISoundTriggerHal.ModelCallback callback) {
        ISoundTriggerHw.SoundModel hidlModel = ConversionUtil.aidl2hidlSoundModel(soundModel);
        try {
            try {
                final AtomicInteger retval = new AtomicInteger(-1);
                final AtomicInteger handle = new AtomicInteger(0);
                try {
                    as2_1().loadSoundModel_2_1(hidlModel, new ModelCallbackWrapper(callback), 0, new ISoundTriggerHw.loadSoundModel_2_1Callback() { // from class: com.android.server.soundtrigger_middleware.SoundTriggerHw2Compat$$ExternalSyntheticLambda3
                        @Override // android.hardware.soundtrigger.V2_1.ISoundTriggerHw.loadSoundModel_2_1Callback
                        public final void onValues(int i, int i2) {
                            SoundTriggerHw2Compat.lambda$loadSoundModel$1(retval, handle, i, i2);
                        }
                    });
                    handleHalStatus(retval.get(), "loadSoundModel_2_1");
                    this.mModelCallbacks.put(Integer.valueOf(handle.get()), callback);
                    return handle.get();
                } catch (NotSupported e) {
                    int loadSoundModel_2_0 = loadSoundModel_2_0(hidlModel, callback);
                    if (hidlModel.data != null) {
                        try {
                            hidlModel.data.close();
                        } catch (IOException e2) {
                            Log.e(TAG, "Failed to close file", e2);
                        }
                    }
                    return loadSoundModel_2_0;
                }
            } finally {
                if (hidlModel.data != null) {
                    try {
                        hidlModel.data.close();
                    } catch (IOException e3) {
                        Log.e(TAG, "Failed to close file", e3);
                    }
                }
            }
        } catch (RemoteException e4) {
            throw e4.rethrowAsRuntimeException();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$loadSoundModel$1(AtomicInteger retval, AtomicInteger handle, int r, int h) {
        retval.set(r);
        handle.set(h);
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IGET, IGET]}, finally: {[IGET, IGET, INVOKE, MOVE_EXCEPTION, IGET, IGET, INVOKE, INVOKE, MOVE_EXCEPTION, IF] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [272=4] */
    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public int loadPhraseSoundModel(PhraseSoundModel soundModel, ISoundTriggerHal.ModelCallback callback) {
        ISoundTriggerHw.PhraseSoundModel hidlModel = ConversionUtil.aidl2hidlPhraseSoundModel(soundModel);
        try {
            try {
                final AtomicInteger retval = new AtomicInteger(-1);
                final AtomicInteger handle = new AtomicInteger(0);
                try {
                    as2_1().loadPhraseSoundModel_2_1(hidlModel, new ModelCallbackWrapper(callback), 0, new ISoundTriggerHw.loadPhraseSoundModel_2_1Callback() { // from class: com.android.server.soundtrigger_middleware.SoundTriggerHw2Compat$$ExternalSyntheticLambda7
                        @Override // android.hardware.soundtrigger.V2_1.ISoundTriggerHw.loadPhraseSoundModel_2_1Callback
                        public final void onValues(int i, int i2) {
                            SoundTriggerHw2Compat.lambda$loadPhraseSoundModel$2(retval, handle, i, i2);
                        }
                    });
                    handleHalStatus(retval.get(), "loadPhraseSoundModel_2_1");
                    this.mModelCallbacks.put(Integer.valueOf(handle.get()), callback);
                    return handle.get();
                } catch (NotSupported e) {
                    int loadPhraseSoundModel_2_0 = loadPhraseSoundModel_2_0(hidlModel, callback);
                    if (hidlModel.common.data != null) {
                        try {
                            hidlModel.common.data.close();
                        } catch (IOException e2) {
                            Log.e(TAG, "Failed to close file", e2);
                        }
                    }
                    return loadPhraseSoundModel_2_0;
                }
            } catch (RemoteException e3) {
                throw e3.rethrowAsRuntimeException();
            }
        } finally {
            if (hidlModel.common.data != null) {
                try {
                    hidlModel.common.data.close();
                } catch (IOException e4) {
                    Log.e(TAG, "Failed to close file", e4);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$loadPhraseSoundModel$2(AtomicInteger retval, AtomicInteger handle, int r, int h) {
        retval.set(r);
        handle.set(h);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void unloadSoundModel(int modelHandle) {
        try {
            this.mModelCallbacks.remove(Integer.valueOf(modelHandle));
            int retval = as2_0().unloadSoundModel(modelHandle);
            handleHalStatus(retval, "unloadSoundModel");
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void stopRecognition(int modelHandle) {
        try {
            int retval = as2_0().stopRecognition(modelHandle);
            handleHalStatus(retval, "stopRecognition");
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void startRecognition(int modelHandle, int deviceHandle, int ioHandle, RecognitionConfig config) {
        android.hardware.soundtrigger.V2_3.RecognitionConfig hidlConfig = ConversionUtil.aidl2hidlRecognitionConfig(config, deviceHandle, ioHandle);
        try {
            try {
                int retval = as2_3().startRecognition_2_3(modelHandle, hidlConfig);
                handleHalStatus(retval, "startRecognition_2_3");
            } catch (NotSupported e) {
                startRecognition_2_1(modelHandle, hidlConfig);
            }
        } catch (RemoteException e2) {
            throw e2.rethrowAsRuntimeException();
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void forceRecognitionEvent(int modelHandle) {
        try {
            int retval = as2_2().getModelState(modelHandle);
            handleHalStatus(retval, "getModelState");
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        } catch (NotSupported e2) {
            throw e2.throwAsRecoverableException();
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public int getModelParameter(int modelHandle, int param) {
        final AtomicInteger status = new AtomicInteger(-1);
        final AtomicInteger value = new AtomicInteger(0);
        try {
            as2_3().getParameter(modelHandle, param, new ISoundTriggerHw.getParameterCallback() { // from class: com.android.server.soundtrigger_middleware.SoundTriggerHw2Compat$$ExternalSyntheticLambda2
                @Override // android.hardware.soundtrigger.V2_3.ISoundTriggerHw.getParameterCallback
                public final void onValues(int i, int i2) {
                    SoundTriggerHw2Compat.lambda$getModelParameter$3(status, value, i, i2);
                }
            });
            handleHalStatus(status.get(), "getParameter");
            return value.get();
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        } catch (NotSupported e2) {
            throw e2.throwAsRecoverableException();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getModelParameter$3(AtomicInteger status, AtomicInteger value, int s, int v) {
        status.set(s);
        value.set(v);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void setModelParameter(int modelHandle, int param, int value) {
        try {
            int retval = as2_3().setParameter(modelHandle, param, value);
            handleHalStatus(retval, "setParameter");
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        } catch (NotSupported e2) {
            throw e2.throwAsRecoverableException();
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public ModelParameterRange queryParameter(int modelHandle, int param) {
        final AtomicInteger status = new AtomicInteger(-1);
        final AtomicReference<OptionalModelParameterRange> optionalRange = new AtomicReference<>();
        try {
            as2_3().queryParameter(modelHandle, param, new ISoundTriggerHw.queryParameterCallback() { // from class: com.android.server.soundtrigger_middleware.SoundTriggerHw2Compat$$ExternalSyntheticLambda8
                @Override // android.hardware.soundtrigger.V2_3.ISoundTriggerHw.queryParameterCallback
                public final void onValues(int i, OptionalModelParameterRange optionalModelParameterRange) {
                    SoundTriggerHw2Compat.lambda$queryParameter$4(status, optionalRange, i, optionalModelParameterRange);
                }
            });
            handleHalStatus(status.get(), "queryParameter");
            if (optionalRange.get().getDiscriminator() != 1) {
                return null;
            }
            return ConversionUtil.hidl2aidlModelParameterRange(optionalRange.get().range());
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        } catch (NotSupported e2) {
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$queryParameter$4(AtomicInteger status, AtomicReference optionalRange, int s, OptionalModelParameterRange r) {
        status.set(s);
        optionalRange.set(r);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void linkToDeath(final IBinder.DeathRecipient recipient) {
        IHwBinder.DeathRecipient wrapper = new IHwBinder.DeathRecipient() { // from class: com.android.server.soundtrigger_middleware.SoundTriggerHw2Compat$$ExternalSyntheticLambda5
            public final void serviceDied(long j) {
                recipient.binderDied();
            }
        };
        this.mDeathRecipientMap.put(recipient, wrapper);
        this.mBinder.linkToDeath(wrapper, 0L);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void unlinkToDeath(IBinder.DeathRecipient recipient) {
        this.mBinder.unlinkToDeath(this.mDeathRecipientMap.remove(recipient));
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public String interfaceDescriptor() {
        try {
            return as2_0().interfaceDescriptor();
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void flushCallbacks() {
    }

    private Properties getProperties_2_0() throws RemoteException {
        final AtomicInteger retval = new AtomicInteger(-1);
        final AtomicReference<ISoundTriggerHw.Properties> properties = new AtomicReference<>();
        as2_0().getProperties(new ISoundTriggerHw.getPropertiesCallback() { // from class: com.android.server.soundtrigger_middleware.SoundTriggerHw2Compat$$ExternalSyntheticLambda6
            @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw.getPropertiesCallback
            public final void onValues(int i, ISoundTriggerHw.Properties properties2) {
                SoundTriggerHw2Compat.lambda$getProperties_2_0$6(retval, properties, i, properties2);
            }
        });
        handleHalStatus(retval.get(), "getProperties");
        return ConversionUtil.hidl2aidlProperties(Hw2CompatUtil.convertProperties_2_0_to_2_3(properties.get()));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getProperties_2_0$6(AtomicInteger retval, AtomicReference properties, int r, ISoundTriggerHw.Properties p) {
        retval.set(r);
        properties.set(p);
    }

    private int loadSoundModel_2_0(ISoundTriggerHw.SoundModel soundModel, ISoundTriggerHal.ModelCallback callback) throws RemoteException {
        ISoundTriggerHw.SoundModel model_2_0 = Hw2CompatUtil.convertSoundModel_2_1_to_2_0(soundModel);
        final AtomicInteger retval = new AtomicInteger(-1);
        final AtomicInteger handle = new AtomicInteger(0);
        as2_0().loadSoundModel(model_2_0, new ModelCallbackWrapper(callback), 0, new ISoundTriggerHw.loadSoundModelCallback() { // from class: com.android.server.soundtrigger_middleware.SoundTriggerHw2Compat$$ExternalSyntheticLambda4
            @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw.loadSoundModelCallback
            public final void onValues(int i, int i2) {
                SoundTriggerHw2Compat.lambda$loadSoundModel_2_0$7(retval, handle, i, i2);
            }
        });
        handleHalStatus(retval.get(), "loadSoundModel");
        this.mModelCallbacks.put(Integer.valueOf(handle.get()), callback);
        return handle.get();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$loadSoundModel_2_0$7(AtomicInteger retval, AtomicInteger handle, int r, int h) {
        retval.set(r);
        handle.set(h);
    }

    private int loadPhraseSoundModel_2_0(ISoundTriggerHw.PhraseSoundModel soundModel, ISoundTriggerHal.ModelCallback callback) throws RemoteException {
        ISoundTriggerHw.PhraseSoundModel model_2_0 = Hw2CompatUtil.convertPhraseSoundModel_2_1_to_2_0(soundModel);
        final AtomicInteger retval = new AtomicInteger(-1);
        final AtomicInteger handle = new AtomicInteger(0);
        as2_0().loadPhraseSoundModel(model_2_0, new ModelCallbackWrapper(callback), 0, new ISoundTriggerHw.loadPhraseSoundModelCallback() { // from class: com.android.server.soundtrigger_middleware.SoundTriggerHw2Compat$$ExternalSyntheticLambda0
            @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw.loadPhraseSoundModelCallback
            public final void onValues(int i, int i2) {
                SoundTriggerHw2Compat.lambda$loadPhraseSoundModel_2_0$8(retval, handle, i, i2);
            }
        });
        handleHalStatus(retval.get(), "loadSoundModel");
        this.mModelCallbacks.put(Integer.valueOf(handle.get()), callback);
        return handle.get();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$loadPhraseSoundModel_2_0$8(AtomicInteger retval, AtomicInteger handle, int r, int h) {
        retval.set(r);
        handle.set(h);
    }

    private void startRecognition_2_1(int modelHandle, android.hardware.soundtrigger.V2_3.RecognitionConfig config) {
        try {
            try {
                ISoundTriggerHw.RecognitionConfig config_2_1 = Hw2CompatUtil.convertRecognitionConfig_2_3_to_2_1(config);
                int retval = as2_1().startRecognition_2_1(modelHandle, config_2_1, new ModelCallbackWrapper(this.mModelCallbacks.get(Integer.valueOf(modelHandle))), 0);
                handleHalStatus(retval, "startRecognition_2_1");
            } catch (NotSupported e) {
                startRecognition_2_0(modelHandle, config);
            }
        } catch (RemoteException e2) {
            throw e2.rethrowAsRuntimeException();
        }
    }

    private void startRecognition_2_0(int modelHandle, android.hardware.soundtrigger.V2_3.RecognitionConfig config) throws RemoteException {
        ISoundTriggerHw.RecognitionConfig config_2_0 = Hw2CompatUtil.convertRecognitionConfig_2_3_to_2_0(config);
        int retval = as2_0().startRecognition(modelHandle, config_2_0, new ModelCallbackWrapper(this.mModelCallbacks.get(Integer.valueOf(modelHandle))), 0);
        handleHalStatus(retval, "startRecognition");
    }

    private android.hardware.soundtrigger.V2_0.ISoundTriggerHw as2_0() {
        return this.mUnderlying_2_0;
    }

    private android.hardware.soundtrigger.V2_1.ISoundTriggerHw as2_1() throws NotSupported {
        android.hardware.soundtrigger.V2_1.ISoundTriggerHw iSoundTriggerHw = this.mUnderlying_2_1;
        if (iSoundTriggerHw == null) {
            throw new NotSupported("Underlying driver version < 2.1");
        }
        return iSoundTriggerHw;
    }

    private android.hardware.soundtrigger.V2_2.ISoundTriggerHw as2_2() throws NotSupported {
        android.hardware.soundtrigger.V2_2.ISoundTriggerHw iSoundTriggerHw = this.mUnderlying_2_2;
        if (iSoundTriggerHw == null) {
            throw new NotSupported("Underlying driver version < 2.2");
        }
        return iSoundTriggerHw;
    }

    private android.hardware.soundtrigger.V2_3.ISoundTriggerHw as2_3() throws NotSupported {
        android.hardware.soundtrigger.V2_3.ISoundTriggerHw iSoundTriggerHw = this.mUnderlying_2_3;
        if (iSoundTriggerHw == null) {
            throw new NotSupported("Underlying driver version < 2.3");
        }
        return iSoundTriggerHw;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class NotSupported extends Exception {
        NotSupported(String message) {
            super(message);
        }

        RecoverableException throwAsRecoverableException() {
            throw new RecoverableException(2, getMessage());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class ModelCallbackWrapper extends ISoundTriggerHwCallback.Stub {
        private final ISoundTriggerHal.ModelCallback mDelegate;

        private ModelCallbackWrapper(ISoundTriggerHal.ModelCallback delegate) {
            this.mDelegate = (ISoundTriggerHal.ModelCallback) Objects.requireNonNull(delegate);
        }

        @Override // android.hardware.soundtrigger.V2_1.ISoundTriggerHwCallback
        public void recognitionCallback_2_1(ISoundTriggerHwCallback.RecognitionEvent event, int cookie) {
            this.mDelegate.recognitionCallback(event.header.model, ConversionUtil.hidl2aidlRecognitionEvent(event));
        }

        @Override // android.hardware.soundtrigger.V2_1.ISoundTriggerHwCallback
        public void phraseRecognitionCallback_2_1(ISoundTriggerHwCallback.PhraseRecognitionEvent event, int cookie) {
            this.mDelegate.phraseRecognitionCallback(event.common.header.model, ConversionUtil.hidl2aidlPhraseRecognitionEvent(event));
        }

        @Override // android.hardware.soundtrigger.V2_1.ISoundTriggerHwCallback
        public void soundModelCallback_2_1(ISoundTriggerHwCallback.ModelEvent event, int cookie) {
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback
        public void recognitionCallback(ISoundTriggerHwCallback.RecognitionEvent event, int cookie) {
            ISoundTriggerHwCallback.RecognitionEvent event_2_1 = Hw2CompatUtil.convertRecognitionEvent_2_0_to_2_1(event);
            recognitionCallback_2_1(event_2_1, cookie);
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback
        public void phraseRecognitionCallback(ISoundTriggerHwCallback.PhraseRecognitionEvent event, int cookie) {
            ISoundTriggerHwCallback.PhraseRecognitionEvent event_2_1 = Hw2CompatUtil.convertPhraseRecognitionEvent_2_0_to_2_1(event);
            phraseRecognitionCallback_2_1(event_2_1, cookie);
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback
        public void soundModelCallback(ISoundTriggerHwCallback.ModelEvent event, int cookie) {
        }
    }
}
