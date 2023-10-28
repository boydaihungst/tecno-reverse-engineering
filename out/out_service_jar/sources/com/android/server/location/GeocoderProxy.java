package com.android.server.location;

import android.content.Context;
import android.location.GeocoderParams;
import android.location.IGeocodeListener;
import android.location.IGeocodeProvider;
import android.os.IBinder;
import android.os.RemoteException;
import com.android.server.servicewatcher.CurrentUserServiceSupplier;
import com.android.server.servicewatcher.ServiceWatcher;
import java.util.Collections;
/* loaded from: classes.dex */
public class GeocoderProxy {
    private static final String SERVICE_ACTION = "com.android.location.service.GeocodeProvider";
    private final ServiceWatcher mServiceWatcher;

    public static GeocoderProxy createAndRegister(Context context) {
        GeocoderProxy proxy = new GeocoderProxy(context);
        if (proxy.register()) {
            return proxy;
        }
        return null;
    }

    private GeocoderProxy(Context context) {
        this.mServiceWatcher = ServiceWatcher.create(context, "GeocoderProxy", CurrentUserServiceSupplier.createFromConfig(context, SERVICE_ACTION, 17891632, 17039978), null);
    }

    private boolean register() {
        boolean resolves = this.mServiceWatcher.checkServiceResolves();
        if (resolves) {
            this.mServiceWatcher.register();
        }
        return resolves;
    }

    public void getFromLocation(final double latitude, final double longitude, final int maxResults, final GeocoderParams params, final IGeocodeListener listener) {
        this.mServiceWatcher.runOnBinder(new ServiceWatcher.BinderOperation() { // from class: com.android.server.location.GeocoderProxy.1
            @Override // com.android.server.servicewatcher.ServiceWatcher.BinderOperation
            public void run(IBinder binder) throws RemoteException {
                IGeocodeProvider provider = IGeocodeProvider.Stub.asInterface(binder);
                provider.getFromLocation(latitude, longitude, maxResults, params, listener);
            }

            @Override // com.android.server.servicewatcher.ServiceWatcher.BinderOperation
            public void onError(Throwable t) {
                try {
                    listener.onResults(t.toString(), Collections.emptyList());
                } catch (RemoteException e) {
                }
            }
        });
    }

    public void getFromLocationName(final String locationName, final double lowerLeftLatitude, final double lowerLeftLongitude, final double upperRightLatitude, final double upperRightLongitude, final int maxResults, final GeocoderParams params, final IGeocodeListener listener) {
        this.mServiceWatcher.runOnBinder(new ServiceWatcher.BinderOperation() { // from class: com.android.server.location.GeocoderProxy.2
            @Override // com.android.server.servicewatcher.ServiceWatcher.BinderOperation
            public void run(IBinder binder) throws RemoteException {
                IGeocodeProvider provider = IGeocodeProvider.Stub.asInterface(binder);
                provider.getFromLocationName(locationName, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude, maxResults, params, listener);
            }

            @Override // com.android.server.servicewatcher.ServiceWatcher.BinderOperation
            public void onError(Throwable t) {
                try {
                    listener.onResults(t.toString(), Collections.emptyList());
                } catch (RemoteException e) {
                }
            }
        });
    }
}
