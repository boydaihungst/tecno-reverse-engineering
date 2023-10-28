package com.android.server.health;

import android.hardware.health.HealthInfo;
import android.hardware.health.IHealth;
import android.os.BatteryProperty;
import android.os.Binder;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IServiceCallback;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.os.Trace;
import android.util.Slog;
import com.android.server.health.HealthServiceWrapperAidl;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class HealthServiceWrapperAidl extends HealthServiceWrapper {
    static final String SERVICE_NAME = IHealth.DESCRIPTOR + "/default";
    private static final String TAG = "HealthServiceWrapperAidl";
    private final HandlerThread mHandlerThread;
    private final AtomicReference<IHealth> mLastService;
    private final HealthRegCallbackAidl mRegCallback;
    private final IServiceCallback mServiceCallback;

    /* loaded from: classes.dex */
    interface ServiceManagerStub {
        default IHealth waitForDeclaredService(String name) {
            return IHealth.Stub.asInterface(ServiceManager.waitForDeclaredService(name));
        }

        default void registerForNotifications(String name, IServiceCallback callback) throws RemoteException {
            ServiceManager.registerForNotifications(name, callback);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HealthServiceWrapperAidl(HealthRegCallbackAidl regCallback, ServiceManagerStub serviceManager) throws RemoteException, NoSuchElementException {
        HandlerThread handlerThread = new HandlerThread("HealthServiceBinder");
        this.mHandlerThread = handlerThread;
        AtomicReference<IHealth> atomicReference = new AtomicReference<>();
        this.mLastService = atomicReference;
        ServiceCallback serviceCallback = new ServiceCallback();
        this.mServiceCallback = serviceCallback;
        traceBegin("HealthInitGetServiceAidl");
        try {
            String str = SERVICE_NAME;
            IHealth newService = serviceManager.waitForDeclaredService(str);
            if (newService == null) {
                throw new NoSuchElementException("IHealth service instance isn't available. Perhaps no permission?");
            }
            atomicReference.set(newService);
            this.mRegCallback = regCallback;
            if (regCallback != null) {
                regCallback.onRegistration(null, newService);
            }
            traceBegin("HealthInitRegisterNotificationAidl");
            handlerThread.start();
            try {
                serviceManager.registerForNotifications(str, serviceCallback);
                traceEnd();
                Slog.i(TAG, "health: HealthServiceWrapper listening to AIDL HAL");
            } finally {
            }
        } finally {
        }
    }

    @Override // com.android.server.health.HealthServiceWrapper
    public HandlerThread getHandlerThread() {
        return this.mHandlerThread;
    }

    @Override // com.android.server.health.HealthServiceWrapper
    public int getProperty(int id, BatteryProperty prop) throws RemoteException {
        traceBegin("HealthGetPropertyAidl");
        try {
            return getPropertyInternal(id, prop);
        } finally {
            traceEnd();
        }
    }

    private int getPropertyInternal(int id, BatteryProperty prop) throws RemoteException {
        IHealth service = this.mLastService.get();
        if (service == null) {
            throw new RemoteException("no health service");
        }
        try {
            switch (id) {
                case 1:
                    prop.setLong(service.getChargeCounterUah());
                    break;
                case 2:
                    prop.setLong(service.getCurrentNowMicroamps());
                    break;
                case 3:
                    prop.setLong(service.getCurrentAverageMicroamps());
                    break;
                case 4:
                    prop.setLong(service.getCapacity());
                    break;
                case 5:
                    prop.setLong(service.getEnergyCounterNwh());
                    break;
                case 6:
                    prop.setLong(service.getChargeStatus());
                    break;
                default:
                    return 0;
            }
            return 0;
        } catch (UnsupportedOperationException e) {
            return -1;
        } catch (ServiceSpecificException e2) {
            return -2;
        }
    }

    @Override // com.android.server.health.HealthServiceWrapper
    public void scheduleUpdate() throws RemoteException {
        getHandlerThread().getThreadHandler().post(new Runnable() { // from class: com.android.server.health.HealthServiceWrapperAidl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                HealthServiceWrapperAidl.this.m3881xb27948b6();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleUpdate$0$com-android-server-health-HealthServiceWrapperAidl  reason: not valid java name */
    public /* synthetic */ void m3881xb27948b6() {
        IHealth service;
        traceBegin("HealthScheduleUpdate");
        try {
            try {
                service = this.mLastService.get();
            } catch (RemoteException | ServiceSpecificException ex) {
                Slog.e(TAG, "Cannot call update on health AIDL HAL", ex);
            }
            if (service == null) {
                Slog.e(TAG, "no health service");
            } else {
                service.update();
            }
        } finally {
            traceEnd();
        }
    }

    @Override // com.android.server.health.HealthServiceWrapper
    public HealthInfo getHealthInfo() throws RemoteException {
        IHealth service = this.mLastService.get();
        if (service == null) {
            return null;
        }
        try {
            return service.getHealthInfo();
        } catch (UnsupportedOperationException | ServiceSpecificException e) {
            return null;
        }
    }

    private static void traceBegin(String name) {
        Trace.traceBegin(524288L, name);
    }

    private static void traceEnd() {
        Trace.traceEnd(524288L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ServiceCallback extends IServiceCallback.Stub {
        private ServiceCallback() {
        }

        public void onRegistration(String name, final IBinder newBinder) throws RemoteException {
            if (HealthServiceWrapperAidl.SERVICE_NAME.equals(name)) {
                HealthServiceWrapperAidl.this.getHandlerThread().getThreadHandler().post(new Runnable() { // from class: com.android.server.health.HealthServiceWrapperAidl$ServiceCallback$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        HealthServiceWrapperAidl.ServiceCallback.this.m3882xc4c1bf14(newBinder);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onRegistration$0$com-android-server-health-HealthServiceWrapperAidl$ServiceCallback  reason: not valid java name */
        public /* synthetic */ void m3882xc4c1bf14(IBinder newBinder) {
            IHealth newService = IHealth.Stub.asInterface(Binder.allowBlocking(newBinder));
            IHealth oldService = (IHealth) HealthServiceWrapperAidl.this.mLastService.getAndSet(newService);
            IBinder oldBinder = oldService != null ? oldService.asBinder() : null;
            if (Objects.equals(newBinder, oldBinder)) {
                return;
            }
            Slog.i(HealthServiceWrapperAidl.TAG, "New health AIDL HAL service registered");
            if (HealthServiceWrapperAidl.this.mRegCallback != null) {
                HealthServiceWrapperAidl.this.mRegCallback.onRegistration(oldService, newService);
            }
        }
    }
}
