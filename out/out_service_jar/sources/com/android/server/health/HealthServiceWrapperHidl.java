package com.android.server.health;

import android.hardware.health.HealthInfo;
import android.hardware.health.Translate;
import android.hardware.health.V2_0.IHealth;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
import android.os.BatteryProperty;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.os.Trace;
import android.util.MutableInt;
import android.util.Slog;
import com.android.server.health.HealthServiceWrapperHidl;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class HealthServiceWrapperHidl extends HealthServiceWrapper {
    public static final String INSTANCE_VENDOR = "default";
    private static final String TAG = "HealthServiceWrapperHidl";
    private Callback mCallback;
    private IHealthSupplier mHealthSupplier;
    private String mInstanceName;
    private final IServiceNotification mNotification = new Notification();
    private final HandlerThread mHandlerThread = new HandlerThread("HealthServiceHwbinder");
    private final AtomicReference<IHealth> mLastService = new AtomicReference<>();

    /* loaded from: classes.dex */
    interface Callback {
        void onRegistration(IHealth iHealth, IHealth iHealth2, String str);
    }

    private static void traceBegin(String name) {
        Trace.traceBegin(524288L, name);
    }

    private static void traceEnd() {
        Trace.traceEnd(524288L);
    }

    @Override // com.android.server.health.HealthServiceWrapper
    public int getProperty(int id, final BatteryProperty prop) throws RemoteException {
        traceBegin("HealthGetProperty");
        try {
            IHealth service = this.mLastService.get();
            if (service == null) {
                throw new RemoteException("no health service");
            }
            final MutableInt outResult = new MutableInt(1);
            switch (id) {
                case 1:
                    service.getChargeCounter(new IHealth.getChargeCounterCallback() { // from class: com.android.server.health.HealthServiceWrapperHidl$$ExternalSyntheticLambda0
                        @Override // android.hardware.health.V2_0.IHealth.getChargeCounterCallback
                        public final void onValues(int i, int i2) {
                            HealthServiceWrapperHidl.lambda$getProperty$0(outResult, prop, i, i2);
                        }
                    });
                    break;
                case 2:
                    service.getCurrentNow(new IHealth.getCurrentNowCallback() { // from class: com.android.server.health.HealthServiceWrapperHidl$$ExternalSyntheticLambda1
                        @Override // android.hardware.health.V2_0.IHealth.getCurrentNowCallback
                        public final void onValues(int i, int i2) {
                            HealthServiceWrapperHidl.lambda$getProperty$1(outResult, prop, i, i2);
                        }
                    });
                    break;
                case 3:
                    service.getCurrentAverage(new IHealth.getCurrentAverageCallback() { // from class: com.android.server.health.HealthServiceWrapperHidl$$ExternalSyntheticLambda2
                        @Override // android.hardware.health.V2_0.IHealth.getCurrentAverageCallback
                        public final void onValues(int i, int i2) {
                            HealthServiceWrapperHidl.lambda$getProperty$2(outResult, prop, i, i2);
                        }
                    });
                    break;
                case 4:
                    service.getCapacity(new IHealth.getCapacityCallback() { // from class: com.android.server.health.HealthServiceWrapperHidl$$ExternalSyntheticLambda3
                        @Override // android.hardware.health.V2_0.IHealth.getCapacityCallback
                        public final void onValues(int i, int i2) {
                            HealthServiceWrapperHidl.lambda$getProperty$3(outResult, prop, i, i2);
                        }
                    });
                    break;
                case 5:
                    service.getEnergyCounter(new IHealth.getEnergyCounterCallback() { // from class: com.android.server.health.HealthServiceWrapperHidl$$ExternalSyntheticLambda5
                        @Override // android.hardware.health.V2_0.IHealth.getEnergyCounterCallback
                        public final void onValues(int i, long j) {
                            HealthServiceWrapperHidl.lambda$getProperty$5(outResult, prop, i, j);
                        }
                    });
                    break;
                case 6:
                    service.getChargeStatus(new IHealth.getChargeStatusCallback() { // from class: com.android.server.health.HealthServiceWrapperHidl$$ExternalSyntheticLambda4
                        @Override // android.hardware.health.V2_0.IHealth.getChargeStatusCallback
                        public final void onValues(int i, int i2) {
                            HealthServiceWrapperHidl.lambda$getProperty$4(outResult, prop, i, i2);
                        }
                    });
                    break;
            }
            return outResult.value;
        } finally {
            traceEnd();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getProperty$0(MutableInt outResult, BatteryProperty prop, int result, int value) {
        outResult.value = result;
        if (result == 0) {
            prop.setLong(value);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getProperty$1(MutableInt outResult, BatteryProperty prop, int result, int value) {
        outResult.value = result;
        if (result == 0) {
            prop.setLong(value);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getProperty$2(MutableInt outResult, BatteryProperty prop, int result, int value) {
        outResult.value = result;
        if (result == 0) {
            prop.setLong(value);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getProperty$3(MutableInt outResult, BatteryProperty prop, int result, int value) {
        outResult.value = result;
        if (result == 0) {
            prop.setLong(value);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getProperty$4(MutableInt outResult, BatteryProperty prop, int result, int value) {
        outResult.value = result;
        if (result == 0) {
            prop.setLong(value);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getProperty$5(MutableInt outResult, BatteryProperty prop, int result, long value) {
        outResult.value = result;
        if (result == 0) {
            prop.setLong(value);
        }
    }

    @Override // com.android.server.health.HealthServiceWrapper
    public void scheduleUpdate() throws RemoteException {
        getHandlerThread().getThreadHandler().post(new Runnable() { // from class: com.android.server.health.HealthServiceWrapperHidl$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                HealthServiceWrapperHidl.this.m3888xc5901589();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleUpdate$6$com-android-server-health-HealthServiceWrapperHidl  reason: not valid java name */
    public /* synthetic */ void m3888xc5901589() {
        IHealth service;
        traceBegin("HealthScheduleUpdate");
        try {
            try {
                service = this.mLastService.get();
            } catch (RemoteException ex) {
                Slog.e(TAG, "Cannot call update on health HAL", ex);
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

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Mutable<T> {
        public T value;

        private Mutable() {
        }
    }

    @Override // com.android.server.health.HealthServiceWrapper
    public HealthInfo getHealthInfo() throws RemoteException {
        IHealth service = this.mLastService.get();
        if (service == null) {
            return null;
        }
        final Mutable<HealthInfo> ret = new Mutable<>();
        service.getHealthInfo(new IHealth.getHealthInfoCallback() { // from class: com.android.server.health.HealthServiceWrapperHidl$$ExternalSyntheticLambda6
            @Override // android.hardware.health.V2_0.IHealth.getHealthInfoCallback
            public final void onValues(int i, android.hardware.health.V2_0.HealthInfo healthInfo) {
                HealthServiceWrapperHidl.lambda$getHealthInfo$7(HealthServiceWrapperHidl.Mutable.this, i, healthInfo);
            }
        });
        return (HealthInfo) ret.value;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Type inference failed for: r0v1, types: [T, android.hardware.health.HealthInfo] */
    public static /* synthetic */ void lambda$getHealthInfo$7(Mutable ret, int result, android.hardware.health.V2_0.HealthInfo value) {
        if (result == 0) {
            ret.value = Translate.h2aTranslate(value.legacy);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HealthServiceWrapperHidl(Callback callback, IServiceManagerSupplier managerSupplier, IHealthSupplier healthSupplier) throws RemoteException, NoSuchElementException, NullPointerException {
        if (managerSupplier == null || healthSupplier == null) {
            throw new NullPointerException();
        }
        this.mHealthSupplier = healthSupplier;
        IHealth newService = null;
        traceBegin("HealthInitGetService_default");
        try {
            newService = healthSupplier.get(INSTANCE_VENDOR);
        } catch (NoSuchElementException e) {
        } catch (Throwable th) {
            throw th;
        }
        if (newService != null) {
            this.mInstanceName = INSTANCE_VENDOR;
            this.mLastService.set(newService);
        }
        String str = this.mInstanceName;
        if (str == null || newService == null) {
            throw new NoSuchElementException(String.format("IHealth service instance %s isn't available. Perhaps no permission?", INSTANCE_VENDOR));
        }
        if (callback != null) {
            this.mCallback = callback;
            callback.onRegistration(null, newService, str);
        }
        traceBegin("HealthInitRegisterNotification");
        this.mHandlerThread.start();
        try {
            managerSupplier.get().registerForNotifications(IHealth.kInterfaceName, this.mInstanceName, this.mNotification);
            traceEnd();
            Slog.i(TAG, "health: HealthServiceWrapper listening to instance " + this.mInstanceName);
        } finally {
            traceEnd();
        }
    }

    @Override // com.android.server.health.HealthServiceWrapper
    public HandlerThread getHandlerThread() {
        return this.mHandlerThread;
    }

    /* loaded from: classes.dex */
    interface IServiceManagerSupplier {
        default IServiceManager get() throws NoSuchElementException, RemoteException {
            return IServiceManager.getService();
        }
    }

    /* loaded from: classes.dex */
    interface IHealthSupplier {
        default IHealth get(String name) throws NoSuchElementException, RemoteException {
            return IHealth.getService(name, true);
        }
    }

    /* loaded from: classes.dex */
    private class Notification extends IServiceNotification.Stub {
        private Notification() {
        }

        @Override // android.hidl.manager.V1_0.IServiceNotification
        public final void onRegistration(String interfaceName, String instanceName, boolean preexisting) {
            if (IHealth.kInterfaceName.equals(interfaceName) && HealthServiceWrapperHidl.this.mInstanceName.equals(instanceName)) {
                HealthServiceWrapperHidl.this.mHandlerThread.getThreadHandler().post(new Runnable() { // from class: com.android.server.health.HealthServiceWrapperHidl.Notification.1
                    @Override // java.lang.Runnable
                    public void run() {
                        try {
                            IHealth newService = HealthServiceWrapperHidl.this.mHealthSupplier.get(HealthServiceWrapperHidl.this.mInstanceName);
                            IHealth oldService = (IHealth) HealthServiceWrapperHidl.this.mLastService.getAndSet(newService);
                            if (Objects.equals(newService, oldService)) {
                                return;
                            }
                            Slog.i(HealthServiceWrapperHidl.TAG, "health: new instance registered " + HealthServiceWrapperHidl.this.mInstanceName);
                            if (HealthServiceWrapperHidl.this.mCallback == null) {
                                return;
                            }
                            HealthServiceWrapperHidl.this.mCallback.onRegistration(oldService, newService, HealthServiceWrapperHidl.this.mInstanceName);
                        } catch (RemoteException | NoSuchElementException ex) {
                            Slog.e(HealthServiceWrapperHidl.TAG, "health: Cannot get instance '" + HealthServiceWrapperHidl.this.mInstanceName + "': " + ex.getMessage() + ". Perhaps no permission?");
                        }
                    }
                });
            }
        }
    }
}
