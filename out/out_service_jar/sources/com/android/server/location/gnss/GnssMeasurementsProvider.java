package com.android.server.location.gnss;

import android.location.GnssMeasurementRequest;
import android.location.GnssMeasurementsEvent;
import android.location.IGnssMeasurementsListener;
import android.location.util.identity.CallerIdentity;
import android.os.IBinder;
import android.util.Log;
import com.android.internal.listeners.ListenerExecutor;
import com.android.server.location.gnss.GnssListenerMultiplexer;
import com.android.server.location.gnss.hal.GnssNative;
import com.android.server.location.injector.AppOpsHelper;
import com.android.server.location.injector.Injector;
import com.android.server.location.injector.LocationUsageLogger;
import com.android.server.location.injector.SettingsHelper;
import java.util.Collection;
import java.util.function.Function;
/* loaded from: classes.dex */
public final class GnssMeasurementsProvider extends GnssListenerMultiplexer<GnssMeasurementRequest, IGnssMeasurementsListener, GnssMeasurementRequest> implements SettingsHelper.GlobalSettingChangedListener, GnssNative.BaseCallbacks, GnssNative.MeasurementCallbacks {
    private final AppOpsHelper mAppOpsHelper;
    private final GnssNative mGnssNative;
    private final LocationUsageLogger mLogger;

    @Override // com.android.server.location.gnss.GnssListenerMultiplexer, com.android.server.location.listeners.ListenerMultiplexer
    protected /* bridge */ /* synthetic */ Object mergeRegistrations(Collection collection) {
        return mergeRegistrations((Collection<GnssListenerMultiplexer<GnssMeasurementRequest, IGnssMeasurementsListener, GnssMeasurementRequest>.GnssListenerRegistration>) collection);
    }

    @Override // com.android.server.location.listeners.ListenerMultiplexer
    protected /* bridge */ /* synthetic */ boolean registerWithService(Object obj, Collection collection) {
        return registerWithService((GnssMeasurementRequest) obj, (Collection<GnssListenerMultiplexer<GnssMeasurementRequest, IGnssMeasurementsListener, GnssMeasurementRequest>.GnssListenerRegistration>) collection);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class GnssMeasurementListenerRegistration extends GnssListenerMultiplexer<GnssMeasurementRequest, IGnssMeasurementsListener, GnssMeasurementRequest>.GnssListenerRegistration {
        protected GnssMeasurementListenerRegistration(GnssMeasurementRequest request, CallerIdentity callerIdentity, IGnssMeasurementsListener listener) {
            super(request, callerIdentity, listener);
        }

        @Override // com.android.server.location.gnss.GnssListenerMultiplexer.GnssListenerRegistration
        protected void onGnssListenerRegister() {
            executeOperation(new ListenerExecutor.ListenerOperation() { // from class: com.android.server.location.gnss.GnssMeasurementsProvider$GnssMeasurementListenerRegistration$$ExternalSyntheticLambda0
                public final void operate(Object obj) {
                    ((IGnssMeasurementsListener) obj).onStatusChanged(1);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.location.listeners.ListenerRegistration
        public void onActive() {
            GnssMeasurementsProvider.this.mAppOpsHelper.startOpNoThrow(42, getIdentity());
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.location.listeners.ListenerRegistration
        public void onInactive() {
            GnssMeasurementsProvider.this.mAppOpsHelper.finishOp(42, getIdentity());
        }
    }

    public GnssMeasurementsProvider(Injector injector, GnssNative gnssNative) {
        super(injector);
        this.mAppOpsHelper = injector.getAppOpsHelper();
        this.mLogger = injector.getLocationUsageLogger();
        this.mGnssNative = gnssNative;
        gnssNative.addBaseCallbacks(this);
        gnssNative.addMeasurementCallbacks(this);
    }

    @Override // com.android.server.location.gnss.GnssListenerMultiplexer
    public boolean isSupported() {
        return this.mGnssNative.isMeasurementSupported();
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.location.gnss.GnssListenerMultiplexer
    public void addListener(GnssMeasurementRequest request, CallerIdentity identity, IGnssMeasurementsListener listener) {
        super.addListener((GnssMeasurementsProvider) request, identity, (CallerIdentity) listener);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.gnss.GnssListenerMultiplexer
    public GnssListenerMultiplexer<GnssMeasurementRequest, IGnssMeasurementsListener, GnssMeasurementRequest>.GnssListenerRegistration createRegistration(GnssMeasurementRequest request, CallerIdentity callerIdentity, IGnssMeasurementsListener listener) {
        return new GnssMeasurementListenerRegistration(request, callerIdentity, listener);
    }

    protected boolean registerWithService(GnssMeasurementRequest request, Collection<GnssListenerMultiplexer<GnssMeasurementRequest, IGnssMeasurementsListener, GnssMeasurementRequest>.GnssListenerRegistration> registrations) {
        if (this.mGnssNative.stopMeasurementCollection()) {
            if (GnssManagerService.D) {
                Log.d(GnssManagerService.TAG, "stopping gnss measurements");
            }
        } else {
            Log.e(GnssManagerService.TAG, "error stopping gnss measurements");
        }
        if (this.mGnssNative.startMeasurementCollection(request.isFullTracking(), request.isCorrelationVectorOutputsEnabled(), request.getIntervalMillis())) {
            if (GnssManagerService.D) {
                Log.d(GnssManagerService.TAG, "starting gnss measurements (" + request + ")");
                return true;
            }
            return true;
        }
        Log.e(GnssManagerService.TAG, "error starting gnss measurements");
        return false;
    }

    @Override // com.android.server.location.listeners.ListenerMultiplexer
    protected void unregisterWithService() {
        if (this.mGnssNative.stopMeasurementCollection()) {
            if (GnssManagerService.D) {
                Log.d(GnssManagerService.TAG, "stopping gnss measurements");
                return;
            }
            return;
        }
        Log.e(GnssManagerService.TAG, "error stopping gnss measurements");
    }

    @Override // com.android.server.location.listeners.ListenerMultiplexer
    protected void onActive() {
        this.mSettingsHelper.addOnGnssMeasurementsFullTrackingEnabledChangedListener(this);
    }

    @Override // com.android.server.location.listeners.ListenerMultiplexer
    protected void onInactive() {
        this.mSettingsHelper.removeOnGnssMeasurementsFullTrackingEnabledChangedListener(this);
    }

    @Override // com.android.server.location.injector.SettingsHelper.GlobalSettingChangedListener
    public void onSettingChanged() {
        updateService();
    }

    @Override // com.android.server.location.gnss.GnssListenerMultiplexer, com.android.server.location.listeners.ListenerMultiplexer
    protected GnssMeasurementRequest mergeRegistrations(Collection<GnssListenerMultiplexer<GnssMeasurementRequest, IGnssMeasurementsListener, GnssMeasurementRequest>.GnssListenerRegistration> registrations) {
        boolean fullTracking = false;
        boolean enableCorrVecOutputs = false;
        int intervalMillis = Integer.MAX_VALUE;
        if (this.mSettingsHelper.isGnssMeasurementsFullTrackingEnabled()) {
            fullTracking = true;
        }
        for (GnssListenerMultiplexer<GnssMeasurementRequest, IGnssMeasurementsListener, GnssMeasurementRequest>.GnssListenerRegistration registration : registrations) {
            GnssMeasurementRequest request = registration.getRequest();
            if (request.isFullTracking()) {
                fullTracking = true;
            }
            if (request.isCorrelationVectorOutputsEnabled()) {
                enableCorrVecOutputs = true;
            }
            intervalMillis = Math.min(intervalMillis, request.getIntervalMillis());
        }
        return new GnssMeasurementRequest.Builder().setFullTracking(fullTracking).setCorrelationVectorOutputsEnabled(enableCorrVecOutputs).setIntervalMillis(intervalMillis).build();
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.listeners.ListenerMultiplexer
    public void onRegistrationAdded(IBinder key, GnssListenerMultiplexer<GnssMeasurementRequest, IGnssMeasurementsListener, GnssMeasurementRequest>.GnssListenerRegistration registration) {
        this.mLogger.logLocationApiUsage(0, 2, registration.getIdentity().getPackageName(), registration.getIdentity().getAttributionTag(), null, null, true, false, null, registration.isForeground());
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.listeners.ListenerMultiplexer
    public void onRegistrationRemoved(IBinder key, GnssListenerMultiplexer<GnssMeasurementRequest, IGnssMeasurementsListener, GnssMeasurementRequest>.GnssListenerRegistration registration) {
        this.mLogger.logLocationApiUsage(1, 2, registration.getIdentity().getPackageName(), registration.getIdentity().getAttributionTag(), null, null, true, false, null, registration.isForeground());
    }

    @Override // com.android.server.location.gnss.hal.GnssNative.BaseCallbacks
    public void onHalRestarted() {
        resetService();
    }

    @Override // com.android.server.location.gnss.hal.GnssNative.MeasurementCallbacks
    public void onReportMeasurements(final GnssMeasurementsEvent event) {
        deliverToListeners(new Function() { // from class: com.android.server.location.gnss.GnssMeasurementsProvider$$ExternalSyntheticLambda1
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return GnssMeasurementsProvider.this.m4377xfe79834d(event, (GnssListenerMultiplexer.GnssListenerRegistration) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onReportMeasurements$1$com-android-server-location-gnss-GnssMeasurementsProvider  reason: not valid java name */
    public /* synthetic */ ListenerExecutor.ListenerOperation m4377xfe79834d(final GnssMeasurementsEvent event, GnssListenerMultiplexer.GnssListenerRegistration registration) {
        if (this.mAppOpsHelper.noteOpNoThrow(1, registration.getIdentity())) {
            return new ListenerExecutor.ListenerOperation() { // from class: com.android.server.location.gnss.GnssMeasurementsProvider$$ExternalSyntheticLambda0
                public final void operate(Object obj) {
                    ((IGnssMeasurementsListener) obj).onGnssMeasurementsReceived(event);
                }
            };
        }
        return null;
    }
}
