package android.os;

import android.annotation.SystemApi;
import android.os.ServiceManager;
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
/* loaded from: classes2.dex */
public class BluetoothServiceManager {
    public static final String BLUETOOTH_MANAGER_SERVICE = "bluetooth_manager";

    /* loaded from: classes2.dex */
    public static final class ServiceRegisterer {
        private final String mServiceName;

        public ServiceRegisterer(String serviceName) {
            this.mServiceName = serviceName;
        }

        public void register(IBinder service) {
            ServiceManager.addService(this.mServiceName, service);
        }

        public IBinder get() {
            return ServiceManager.getService(this.mServiceName);
        }

        public IBinder getOrThrow() throws ServiceNotFoundException {
            try {
                return ServiceManager.getServiceOrThrow(this.mServiceName);
            } catch (ServiceManager.ServiceNotFoundException e) {
                throw new ServiceNotFoundException(this.mServiceName);
            }
        }

        public IBinder tryGet() {
            return ServiceManager.checkService(this.mServiceName);
        }
    }

    /* loaded from: classes2.dex */
    public static class ServiceNotFoundException extends ServiceManager.ServiceNotFoundException {
        public ServiceNotFoundException(String name) {
            super(name);
        }
    }

    public ServiceRegisterer getBluetoothManagerServiceRegisterer() {
        return new ServiceRegisterer(BLUETOOTH_MANAGER_SERVICE);
    }
}
