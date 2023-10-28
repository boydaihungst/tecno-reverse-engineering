package com.android.server.location.geofence;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.location.GeofenceHardwareService;
import android.hardware.location.IGeofenceHardware;
import android.location.IGeofenceProvider;
import android.location.IGpsGeofenceHardware;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import com.android.server.servicewatcher.CurrentUserServiceSupplier;
import com.android.server.servicewatcher.ServiceWatcher;
import java.util.Objects;
/* loaded from: classes.dex */
public final class GeofenceProxy implements ServiceWatcher.ServiceListener<CurrentUserServiceSupplier.BoundServiceInfo> {
    private static final String SERVICE_ACTION = "com.android.location.service.GeofenceProvider";
    private static final String TAG = "GeofenceProxy";
    volatile IGeofenceHardware mGeofenceHardware = null;
    final IGpsGeofenceHardware mGpsGeofenceHardware;
    final ServiceWatcher mServiceWatcher;

    public static GeofenceProxy createAndBind(Context context, IGpsGeofenceHardware gpsGeofence) {
        GeofenceProxy proxy = new GeofenceProxy(context, gpsGeofence);
        if (proxy.register(context)) {
            return proxy;
        }
        return null;
    }

    private GeofenceProxy(Context context, IGpsGeofenceHardware gpsGeofence) {
        this.mGpsGeofenceHardware = (IGpsGeofenceHardware) Objects.requireNonNull(gpsGeofence);
        this.mServiceWatcher = ServiceWatcher.create(context, TAG, CurrentUserServiceSupplier.createFromConfig(context, SERVICE_ACTION, 17891633, 17039979), this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateGeofenceHardware(IBinder binder) throws RemoteException {
        IGeofenceProvider.Stub.asInterface(binder).setGeofenceHardware(this.mGeofenceHardware);
    }

    private boolean register(Context context) {
        boolean resolves = this.mServiceWatcher.checkServiceResolves();
        if (resolves) {
            this.mServiceWatcher.register();
            context.bindServiceAsUser(new Intent(context, GeofenceHardwareService.class), new GeofenceProxyServiceConnection(), 1, UserHandle.SYSTEM);
        }
        return resolves;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.servicewatcher.ServiceWatcher.ServiceListener
    public void onBind(IBinder binder, CurrentUserServiceSupplier.BoundServiceInfo boundServiceInfo) throws RemoteException {
        updateGeofenceHardware(binder);
    }

    @Override // com.android.server.servicewatcher.ServiceWatcher.ServiceListener
    public void onUnbind() {
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class GeofenceProxyServiceConnection implements ServiceConnection {
        GeofenceProxyServiceConnection() {
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            IGeofenceHardware geofenceHardware = IGeofenceHardware.Stub.asInterface(service);
            try {
                geofenceHardware.setGpsGeofenceHardware(GeofenceProxy.this.mGpsGeofenceHardware);
                GeofenceProxy.this.mGeofenceHardware = geofenceHardware;
                GeofenceProxy.this.mServiceWatcher.runOnBinder(new GeofenceProxy$GeofenceProxyServiceConnection$$ExternalSyntheticLambda0(GeofenceProxy.this));
            } catch (RemoteException e) {
                Log.w(GeofenceProxy.TAG, "unable to initialize geofence hardware", e);
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            GeofenceProxy.this.mGeofenceHardware = null;
            GeofenceProxy.this.mServiceWatcher.runOnBinder(new GeofenceProxy$GeofenceProxyServiceConnection$$ExternalSyntheticLambda0(GeofenceProxy.this));
        }
    }
}
