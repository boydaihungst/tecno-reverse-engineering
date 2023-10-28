package com.android.server.location.gnss;

import android.content.Context;
import android.content.Intent;
import android.hardware.location.GeofenceHardwareImpl;
import android.location.FusedBatchOptions;
import android.location.GnssAntennaInfo;
import android.location.GnssCapabilities;
import android.location.GnssMeasurementCorrections;
import android.location.GnssMeasurementRequest;
import android.location.IGnssAntennaInfoListener;
import android.location.IGnssMeasurementsListener;
import android.location.IGnssNavigationMessageListener;
import android.location.IGnssNmeaListener;
import android.location.IGnssStatusListener;
import android.location.IGpsGeofenceHardware;
import android.location.Location;
import android.location.util.identity.CallerIdentity;
import android.os.Binder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.IndentingPrintWriter;
import android.util.Log;
import com.android.internal.app.IBatteryStats;
import com.android.server.FgThread;
import com.android.server.location.gnss.GnssManagerService;
import com.android.server.location.gnss.hal.GnssNative;
import com.android.server.location.injector.Injector;
import java.io.FileDescriptor;
import java.util.List;
/* loaded from: classes.dex */
public class GnssManagerService {
    private static final String ATTRIBUTION_ID = "GnssService";
    private final GnssCapabilitiesHalModule mCapabilitiesHalModule;
    final Context mContext;
    private final GnssGeofenceHalModule mGeofenceHalModule;
    private final GnssAntennaInfoProvider mGnssAntennaInfoProvider;
    private final IGpsGeofenceHardware mGnssGeofenceProxy;
    private final GnssLocationProvider mGnssLocationProvider;
    private final GnssMeasurementsProvider mGnssMeasurementsProvider;
    private final GnssMetrics mGnssMetrics;
    private final GnssNative mGnssNative;
    private final GnssNavigationMessageProvider mGnssNavigationMessageProvider;
    private final GnssNmeaProvider mGnssNmeaProvider;
    private final GnssStatusProvider mGnssStatusProvider;
    public static final String TAG = "GnssManager";
    public static final boolean D = Log.isLoggable(TAG, 3);

    public GnssManagerService(Context context, Injector injector, GnssNative gnssNative) {
        Context createAttributionContext = context.createAttributionContext(ATTRIBUTION_ID);
        this.mContext = createAttributionContext;
        this.mGnssNative = gnssNative;
        GnssMetrics gnssMetrics = new GnssMetrics(createAttributionContext, IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats")), gnssNative);
        this.mGnssMetrics = gnssMetrics;
        this.mGnssLocationProvider = new GnssLocationProvider(createAttributionContext, injector, gnssNative, gnssMetrics);
        this.mGnssStatusProvider = new GnssStatusProvider(injector, gnssNative);
        this.mGnssNmeaProvider = new GnssNmeaProvider(injector, gnssNative);
        this.mGnssMeasurementsProvider = new GnssMeasurementsProvider(injector, gnssNative);
        this.mGnssNavigationMessageProvider = new GnssNavigationMessageProvider(injector, gnssNative);
        this.mGnssAntennaInfoProvider = new GnssAntennaInfoProvider(gnssNative);
        this.mGnssGeofenceProxy = new GnssGeofenceProxy(gnssNative);
        this.mGeofenceHalModule = new GnssGeofenceHalModule(gnssNative);
        this.mCapabilitiesHalModule = new GnssCapabilitiesHalModule(gnssNative);
        gnssNative.register();
    }

    public void onSystemReady() {
        this.mGnssLocationProvider.onSystemReady();
    }

    public GnssLocationProvider getGnssLocationProvider() {
        return this.mGnssLocationProvider;
    }

    public void setAutomotiveGnssSuspended(boolean suspended) {
        this.mGnssLocationProvider.setAutomotiveGnssSuspended(suspended);
    }

    public boolean isAutomotiveGnssSuspended() {
        return this.mGnssLocationProvider.isAutomotiveGnssSuspended();
    }

    public IGpsGeofenceHardware getGnssGeofenceProxy() {
        return this.mGnssGeofenceProxy;
    }

    public int getGnssYearOfHardware() {
        return this.mGnssNative.getHardwareYear();
    }

    public String getGnssHardwareModelName() {
        return this.mGnssNative.getHardwareModelName();
    }

    public GnssCapabilities getGnssCapabilities() {
        return this.mGnssNative.getCapabilities();
    }

    public List<GnssAntennaInfo> getGnssAntennaInfos() {
        return this.mGnssAntennaInfoProvider.getAntennaInfos();
    }

    public int getGnssBatchSize() {
        return this.mGnssLocationProvider.getBatchSize();
    }

    public void registerGnssStatusCallback(IGnssStatusListener listener, String packageName, String attributionTag, String listenerId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION", null);
        CallerIdentity identity = CallerIdentity.fromBinder(this.mContext, packageName, attributionTag, listenerId);
        this.mGnssStatusProvider.addListener(identity, listener);
    }

    public void unregisterGnssStatusCallback(IGnssStatusListener listener) {
        this.mGnssStatusProvider.removeListener(listener);
    }

    public void registerGnssNmeaCallback(IGnssNmeaListener listener, String packageName, String attributionTag, String listenerId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION", null);
        CallerIdentity identity = CallerIdentity.fromBinder(this.mContext, packageName, attributionTag, listenerId);
        this.mGnssNmeaProvider.addListener(identity, listener);
    }

    public void unregisterGnssNmeaCallback(IGnssNmeaListener listener) {
        this.mGnssNmeaProvider.removeListener(listener);
    }

    public void addGnssMeasurementsListener(GnssMeasurementRequest request, IGnssMeasurementsListener listener, String packageName, String attributionTag, String listenerId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION", null);
        if (request.isCorrelationVectorOutputsEnabled()) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.LOCATION_HARDWARE", null);
        }
        CallerIdentity identity = CallerIdentity.fromBinder(this.mContext, packageName, attributionTag, listenerId);
        this.mGnssMeasurementsProvider.addListener(request, identity, listener);
    }

    public void injectGnssMeasurementCorrections(GnssMeasurementCorrections corrections) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.LOCATION_HARDWARE", null);
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION", null);
        if (!this.mGnssNative.injectMeasurementCorrections(corrections)) {
            Log.w(TAG, "failed to inject GNSS measurement corrections");
        }
    }

    public void removeGnssMeasurementsListener(IGnssMeasurementsListener listener) {
        this.mGnssMeasurementsProvider.removeListener(listener);
    }

    public void addGnssNavigationMessageListener(IGnssNavigationMessageListener listener, String packageName, String attributionTag, String listenerId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION", null);
        CallerIdentity identity = CallerIdentity.fromBinder(this.mContext, packageName, attributionTag, listenerId);
        this.mGnssNavigationMessageProvider.addListener(identity, listener);
    }

    public void removeGnssNavigationMessageListener(IGnssNavigationMessageListener listener) {
        this.mGnssNavigationMessageProvider.removeListener(listener);
    }

    public void addGnssAntennaInfoListener(IGnssAntennaInfoListener listener, String packageName, String attributionTag, String listenerId) {
        CallerIdentity identity = CallerIdentity.fromBinder(this.mContext, packageName, attributionTag, listenerId);
        this.mGnssAntennaInfoProvider.addListener(identity, listener);
    }

    public void removeGnssAntennaInfoListener(IGnssAntennaInfoListener listener) {
        this.mGnssAntennaInfoProvider.removeListener(listener);
    }

    public void sendNiResponse(int notifId, int userResponse) {
        try {
            this.mGnssLocationProvider.getNetInitiatedListener().sendNiResponse(notifId, userResponse);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void dump(FileDescriptor fd, IndentingPrintWriter ipw, String[] args) {
        if (args.length > 0 && args[0].equals("--gnssmetrics")) {
            ipw.append(this.mGnssMetrics.dumpGnssMetricsAsProtoString());
            return;
        }
        ipw.println("Capabilities: " + this.mGnssNative.getCapabilities());
        if (this.mGnssStatusProvider.isSupported()) {
            ipw.println("Status Provider:");
            ipw.increaseIndent();
            this.mGnssStatusProvider.dump(fd, ipw, args);
            ipw.decreaseIndent();
        }
        if (this.mGnssMeasurementsProvider.isSupported()) {
            ipw.println("Measurements Provider:");
            ipw.increaseIndent();
            this.mGnssMeasurementsProvider.dump(fd, ipw, args);
            ipw.decreaseIndent();
        }
        if (this.mGnssNavigationMessageProvider.isSupported()) {
            ipw.println("Navigation Message Provider:");
            ipw.increaseIndent();
            this.mGnssNavigationMessageProvider.dump(fd, ipw, args);
            ipw.decreaseIndent();
        }
        if (this.mGnssAntennaInfoProvider.isSupported()) {
            ipw.println("Antenna Info Provider:");
            ipw.increaseIndent();
            ipw.println("Antenna Infos: " + this.mGnssAntennaInfoProvider.getAntennaInfos());
            this.mGnssAntennaInfoProvider.dump(fd, ipw, args);
            ipw.decreaseIndent();
        }
        GnssPowerStats powerStats = this.mGnssNative.getPowerStats();
        if (powerStats != null) {
            ipw.println("Last Power Stats:");
            ipw.increaseIndent();
            powerStats.dump(fd, ipw, args, this.mGnssNative.getCapabilities());
            ipw.decreaseIndent();
        }
    }

    /* loaded from: classes.dex */
    private class GnssCapabilitiesHalModule implements GnssNative.BaseCallbacks {
        GnssCapabilitiesHalModule(GnssNative gnssNative) {
            gnssNative.addBaseCallbacks(this);
        }

        @Override // com.android.server.location.gnss.hal.GnssNative.BaseCallbacks
        public void onHalRestarted() {
        }

        @Override // com.android.server.location.gnss.hal.GnssNative.BaseCallbacks
        public void onCapabilitiesChanged(GnssCapabilities oldCapabilities, GnssCapabilities newCapabilities) {
            long ident = Binder.clearCallingIdentity();
            try {
                Intent intent = new Intent("android.location.action.GNSS_CAPABILITIES_CHANGED").putExtra("android.location.extra.GNSS_CAPABILITIES", newCapabilities).addFlags(1073741824).addFlags(268435456);
                GnssManagerService.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class GnssGeofenceHalModule implements GnssNative.GeofenceCallbacks {
        private GeofenceHardwareImpl mGeofenceHardwareImpl;

        GnssGeofenceHalModule(GnssNative gnssNative) {
            gnssNative.setGeofenceCallbacks(this);
        }

        private synchronized GeofenceHardwareImpl getGeofenceHardware() {
            if (this.mGeofenceHardwareImpl == null) {
                this.mGeofenceHardwareImpl = GeofenceHardwareImpl.getInstance(GnssManagerService.this.mContext);
            }
            return this.mGeofenceHardwareImpl;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onReportGeofenceTransition$0$com-android-server-location-gnss-GnssManagerService$GnssGeofenceHalModule  reason: not valid java name */
        public /* synthetic */ void m4375x1e70c9fe(int geofenceId, Location location, int transition, long timestamp) {
            getGeofenceHardware().reportGeofenceTransition(geofenceId, location, transition, timestamp, 0, FusedBatchOptions.SourceTechnologies.GNSS);
        }

        @Override // com.android.server.location.gnss.hal.GnssNative.GeofenceCallbacks
        public void onReportGeofenceTransition(final int geofenceId, final Location location, final int transition, final long timestamp) {
            FgThread.getHandler().post(new Runnable() { // from class: com.android.server.location.gnss.GnssManagerService$GnssGeofenceHalModule$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    GnssManagerService.GnssGeofenceHalModule.this.m4375x1e70c9fe(geofenceId, location, transition, timestamp);
                }
            });
        }

        @Override // com.android.server.location.gnss.hal.GnssNative.GeofenceCallbacks
        public void onReportGeofenceStatus(final int status, final Location location) {
            FgThread.getHandler().post(new Runnable() { // from class: com.android.server.location.gnss.GnssManagerService$GnssGeofenceHalModule$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    GnssManagerService.GnssGeofenceHalModule.this.m4374xc91963dc(status, location);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onReportGeofenceStatus$1$com-android-server-location-gnss-GnssManagerService$GnssGeofenceHalModule  reason: not valid java name */
        public /* synthetic */ void m4374xc91963dc(int status, Location location) {
            int monitorStatus = 1;
            if (status == 2) {
                monitorStatus = 0;
            }
            getGeofenceHardware().reportGeofenceMonitorStatus(0, monitorStatus, location, FusedBatchOptions.SourceTechnologies.GNSS);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onReportGeofenceAddStatus$2$com-android-server-location-gnss-GnssManagerService$GnssGeofenceHalModule  reason: not valid java name */
        public /* synthetic */ void m4370x7548973e(int geofenceId, int status) {
            getGeofenceHardware().reportGeofenceAddStatus(geofenceId, translateGeofenceStatus(status));
        }

        @Override // com.android.server.location.gnss.hal.GnssNative.GeofenceCallbacks
        public void onReportGeofenceAddStatus(final int geofenceId, final int status) {
            FgThread.getHandler().post(new Runnable() { // from class: com.android.server.location.gnss.GnssManagerService$GnssGeofenceHalModule$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    GnssManagerService.GnssGeofenceHalModule.this.m4370x7548973e(geofenceId, status);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onReportGeofenceRemoveStatus$3$com-android-server-location-gnss-GnssManagerService$GnssGeofenceHalModule  reason: not valid java name */
        public /* synthetic */ void m4372x5ed0102(int geofenceId, int status) {
            getGeofenceHardware().reportGeofenceRemoveStatus(geofenceId, translateGeofenceStatus(status));
        }

        @Override // com.android.server.location.gnss.hal.GnssNative.GeofenceCallbacks
        public void onReportGeofenceRemoveStatus(final int geofenceId, final int status) {
            FgThread.getHandler().post(new Runnable() { // from class: com.android.server.location.gnss.GnssManagerService$GnssGeofenceHalModule$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    GnssManagerService.GnssGeofenceHalModule.this.m4372x5ed0102(geofenceId, status);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onReportGeofencePauseStatus$4$com-android-server-location-gnss-GnssManagerService$GnssGeofenceHalModule  reason: not valid java name */
        public /* synthetic */ void m4371xedf662d5(int geofenceId, int status) {
            getGeofenceHardware().reportGeofencePauseStatus(geofenceId, translateGeofenceStatus(status));
        }

        @Override // com.android.server.location.gnss.hal.GnssNative.GeofenceCallbacks
        public void onReportGeofencePauseStatus(final int geofenceId, final int status) {
            FgThread.getHandler().post(new Runnable() { // from class: com.android.server.location.gnss.GnssManagerService$GnssGeofenceHalModule$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    GnssManagerService.GnssGeofenceHalModule.this.m4371xedf662d5(geofenceId, status);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onReportGeofenceResumeStatus$5$com-android-server-location-gnss-GnssManagerService$GnssGeofenceHalModule  reason: not valid java name */
        public /* synthetic */ void m4373xd6a125ed(int geofenceId, int status) {
            getGeofenceHardware().reportGeofenceResumeStatus(geofenceId, translateGeofenceStatus(status));
        }

        @Override // com.android.server.location.gnss.hal.GnssNative.GeofenceCallbacks
        public void onReportGeofenceResumeStatus(final int geofenceId, final int status) {
            FgThread.getHandler().post(new Runnable() { // from class: com.android.server.location.gnss.GnssManagerService$GnssGeofenceHalModule$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    GnssManagerService.GnssGeofenceHalModule.this.m4373xd6a125ed(geofenceId, status);
                }
            });
        }

        private int translateGeofenceStatus(int status) {
            switch (status) {
                case GnssNative.GeofenceCallbacks.GEOFENCE_STATUS_ERROR_GENERIC /* -149 */:
                    return 5;
                case GnssNative.GeofenceCallbacks.GEOFENCE_STATUS_ERROR_INVALID_TRANSITION /* -103 */:
                    return 4;
                case GnssNative.GeofenceCallbacks.GEOFENCE_STATUS_ERROR_ID_UNKNOWN /* -102 */:
                    return 3;
                case GnssNative.GeofenceCallbacks.GEOFENCE_STATUS_ERROR_ID_EXISTS /* -101 */:
                    return 2;
                case 0:
                    return 0;
                case 100:
                    return 1;
                default:
                    return -1;
            }
        }
    }
}
