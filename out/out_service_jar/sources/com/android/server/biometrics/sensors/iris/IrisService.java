package com.android.server.biometrics.sensors.iris;

import android.content.Context;
import android.hardware.biometrics.IBiometricService;
import android.hardware.biometrics.SensorPropertiesInternal;
import android.hardware.iris.IIrisService;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.biometrics.Utils;
import com.android.server.biometrics.sensors.iris.IrisService;
import com.lucid.propertiesapi.PowerXtendDynamicTuningAPIImpl;
import java.util.Iterator;
import java.util.List;
/* loaded from: classes.dex */
public class IrisService extends SystemService {
    private static final String TAG = "IrisService";
    private final IrisServiceWrapper mServiceWrapper;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class IrisServiceWrapper extends IIrisService.Stub {
        private IrisServiceWrapper() {
        }

        public void registerAuthenticators(final List<SensorPropertiesInternal> hidlSensors) {
            Utils.checkPermission(IrisService.this.getContext(), "android.permission.USE_BIOMETRIC_INTERNAL");
            ServiceThread thread = new ServiceThread(IrisService.TAG, 10, true);
            thread.start();
            Handler handler = new Handler(thread.getLooper());
            handler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.iris.IrisService$IrisServiceWrapper$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    IrisService.IrisServiceWrapper.this.m2550x75d0f70f(hidlSensors);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$registerAuthenticators$0$com-android-server-biometrics-sensors-iris-IrisService$IrisServiceWrapper  reason: not valid java name */
        public /* synthetic */ void m2550x75d0f70f(List hidlSensors) {
            IBiometricService biometricService = IBiometricService.Stub.asInterface(ServiceManager.getService("biometric"));
            Iterator it = hidlSensors.iterator();
            while (it.hasNext()) {
                SensorPropertiesInternal hidlSensor = (SensorPropertiesInternal) it.next();
                int sensorId = hidlSensor.sensorId;
                int strength = Utils.propertyStrengthToAuthenticatorStrength(hidlSensor.sensorStrength);
                IrisAuthenticator authenticator = new IrisAuthenticator(IrisService.this.mServiceWrapper, sensorId);
                try {
                    biometricService.registerAuthenticator(sensorId, 4, strength, authenticator);
                } catch (RemoteException e) {
                    Slog.e(IrisService.TAG, "Remote exception when registering sensorId: " + sensorId);
                }
            }
        }
    }

    public IrisService(Context context) {
        super(context);
        this.mServiceWrapper = new IrisServiceWrapper();
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService(PowerXtendDynamicTuningAPIImpl.IRIS_FILES_EXTENTION, this.mServiceWrapper);
    }
}
