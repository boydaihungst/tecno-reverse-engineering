package com.android.server.soundtrigger_middleware;

import android.hardware.soundtrigger3.ISoundTriggerHw;
import android.os.HwBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
/* loaded from: classes2.dex */
class DefaultHalFactory implements HalFactory {
    private static final String TAG = "SoundTriggerMiddlewareDefaultHalFactory";
    private static final int USE_DEFAULT_HAL = 0;
    private static final int USE_MOCK_HAL_V2 = 2;
    private static final int USE_MOCK_HAL_V3 = 3;
    private static final ICaptureStateNotifier mCaptureStateNotifier = new ExternalCaptureStateTracker();

    @Override // com.android.server.soundtrigger_middleware.HalFactory
    public ISoundTriggerHal create() {
        try {
            int mockHal = SystemProperties.getInt("debug.soundtrigger_middleware.use_mock_hal", 0);
            if (mockHal == 0) {
                String aidlServiceName = ISoundTriggerHw.class.getCanonicalName() + "/default";
                if (ServiceManager.isDeclared(aidlServiceName)) {
                    Log.i(TAG, "Connecting to default soundtrigger3.ISoundTriggerHw");
                    return new SoundTriggerHw3Compat(ServiceManager.waitForService(aidlServiceName), new Runnable() { // from class: com.android.server.soundtrigger_middleware.DefaultHalFactory$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            SystemProperties.set("sys.audio.restart.hal", "1");
                        }
                    });
                }
                Log.i(TAG, "Connecting to default soundtrigger-V2.x.ISoundTriggerHw");
                return SoundTriggerHw2Compat.create(android.hardware.soundtrigger.V2_0.ISoundTriggerHw.getService(true), new Runnable() { // from class: com.android.server.soundtrigger_middleware.DefaultHalFactory$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        SystemProperties.set("sys.audio.restart.hal", "1");
                    }
                }, mCaptureStateNotifier);
            } else if (mockHal == 2) {
                Log.i(TAG, "Connecting to mock soundtrigger-V2.x.ISoundTriggerHw");
                HwBinder.setTrebleTestingOverride(true);
                final android.hardware.soundtrigger.V2_0.ISoundTriggerHw driver = android.hardware.soundtrigger.V2_0.ISoundTriggerHw.getService("mock", true);
                ISoundTriggerHal create = SoundTriggerHw2Compat.create(driver, new Runnable() { // from class: com.android.server.soundtrigger_middleware.DefaultHalFactory$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        DefaultHalFactory.lambda$create$2(android.hardware.soundtrigger.V2_0.ISoundTriggerHw.this);
                    }
                }, mCaptureStateNotifier);
                HwBinder.setTrebleTestingOverride(false);
                return create;
            } else if (mockHal == 3) {
                final String aidlServiceName2 = ISoundTriggerHw.class.getCanonicalName() + "/mock";
                Log.i(TAG, "Connecting to mock soundtrigger3.ISoundTriggerHw");
                return new SoundTriggerHw3Compat(ServiceManager.waitForService(aidlServiceName2), new Runnable() { // from class: com.android.server.soundtrigger_middleware.DefaultHalFactory$$ExternalSyntheticLambda3
                    @Override // java.lang.Runnable
                    public final void run() {
                        DefaultHalFactory.lambda$create$3(aidlServiceName2);
                    }
                });
            } else {
                throw new RuntimeException("Unknown HAL mock version: " + mockHal);
            }
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$create$2(android.hardware.soundtrigger.V2_0.ISoundTriggerHw driver) {
        try {
            driver.debug(null, new ArrayList<>(Arrays.asList("reboot")));
        } catch (Exception e) {
            Log.e(TAG, "Failed to reboot mock HAL", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$create$3(String aidlServiceName) {
        try {
            ServiceManager.waitForService(aidlServiceName).shellCommand(null, null, null, new String[]{"reboot"}, null, null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to reboot mock HAL", e);
        }
    }
}
