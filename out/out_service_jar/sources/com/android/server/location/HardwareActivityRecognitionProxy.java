package com.android.server.location;

import android.content.Context;
import android.hardware.location.ActivityRecognitionHardware;
import android.hardware.location.IActivityRecognitionHardwareClient;
import android.hardware.location.IActivityRecognitionHardwareWatcher;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.servicewatcher.CurrentUserServiceSupplier;
import com.android.server.servicewatcher.ServiceWatcher;
/* loaded from: classes.dex */
public class HardwareActivityRecognitionProxy implements ServiceWatcher.ServiceListener<CurrentUserServiceSupplier.BoundServiceInfo> {
    private static final String SERVICE_ACTION = "com.android.location.service.ActivityRecognitionProvider";
    private static final String TAG = "ARProxy";
    private final ActivityRecognitionHardware mInstance;
    private final boolean mIsSupported;
    private final ServiceWatcher mServiceWatcher;

    public static HardwareActivityRecognitionProxy createAndRegister(Context context) {
        HardwareActivityRecognitionProxy arProxy = new HardwareActivityRecognitionProxy(context);
        if (arProxy.register()) {
            return arProxy;
        }
        return null;
    }

    private HardwareActivityRecognitionProxy(Context context) {
        boolean isSupported = ActivityRecognitionHardware.isSupported();
        this.mIsSupported = isSupported;
        if (isSupported) {
            this.mInstance = ActivityRecognitionHardware.getInstance(context);
        } else {
            this.mInstance = null;
        }
        this.mServiceWatcher = ServiceWatcher.create(context, "HardwareActivityRecognitionProxy", CurrentUserServiceSupplier.createFromConfig(context, SERVICE_ACTION, 17891624, 17039880), this);
    }

    private boolean register() {
        boolean resolves = this.mServiceWatcher.checkServiceResolves();
        if (resolves) {
            this.mServiceWatcher.register();
        }
        return resolves;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.servicewatcher.ServiceWatcher.ServiceListener
    public void onBind(IBinder binder, CurrentUserServiceSupplier.BoundServiceInfo boundServiceInfo) throws RemoteException {
        String descriptor = binder.getInterfaceDescriptor();
        if (IActivityRecognitionHardwareWatcher.class.getCanonicalName().equals(descriptor)) {
            IActivityRecognitionHardwareWatcher watcher = IActivityRecognitionHardwareWatcher.Stub.asInterface(binder);
            ActivityRecognitionHardware activityRecognitionHardware = this.mInstance;
            if (activityRecognitionHardware != null) {
                watcher.onInstanceChanged(activityRecognitionHardware);
            }
        } else if (IActivityRecognitionHardwareClient.class.getCanonicalName().equals(descriptor)) {
            IActivityRecognitionHardwareClient client = IActivityRecognitionHardwareClient.Stub.asInterface(binder);
            client.onAvailabilityChanged(this.mIsSupported, this.mInstance);
        } else {
            Log.e(TAG, "Unknown descriptor: " + descriptor);
        }
    }

    @Override // com.android.server.servicewatcher.ServiceWatcher.ServiceListener
    public void onUnbind() {
    }
}
