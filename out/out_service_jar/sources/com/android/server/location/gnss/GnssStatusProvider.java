package com.android.server.location.gnss;

import android.location.GnssStatus;
import android.location.IGnssStatusListener;
import android.location.util.identity.CallerIdentity;
import android.os.IBinder;
import android.util.Log;
import com.android.internal.listeners.ListenerExecutor;
import com.android.server.location.gnss.GnssListenerMultiplexer;
import com.android.server.location.gnss.hal.GnssNative;
import com.android.server.location.injector.AppOpsHelper;
import com.android.server.location.injector.Injector;
import com.android.server.location.injector.LocationUsageLogger;
import java.util.Collection;
import java.util.function.Function;
/* loaded from: classes.dex */
public class GnssStatusProvider extends GnssListenerMultiplexer<Void, IGnssStatusListener, Void> implements GnssNative.BaseCallbacks, GnssNative.StatusCallbacks, GnssNative.SvStatusCallbacks {
    private final AppOpsHelper mAppOpsHelper;
    private final GnssNative mGnssNative;
    private boolean mIsNavigating;
    private final LocationUsageLogger mLogger;

    @Override // com.android.server.location.listeners.ListenerMultiplexer
    protected /* bridge */ /* synthetic */ boolean registerWithService(Object obj, Collection collection) {
        return registerWithService((Void) obj, (Collection<GnssListenerMultiplexer<Void, IGnssStatusListener, Void>.GnssListenerRegistration>) collection);
    }

    public GnssStatusProvider(Injector injector, GnssNative gnssNative) {
        super(injector);
        this.mIsNavigating = false;
        this.mAppOpsHelper = injector.getAppOpsHelper();
        this.mLogger = injector.getLocationUsageLogger();
        this.mGnssNative = gnssNative;
        gnssNative.addBaseCallbacks(this);
        gnssNative.addStatusCallbacks(this);
        gnssNative.addSvStatusCallbacks(this);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.location.gnss.GnssListenerMultiplexer
    public void addListener(CallerIdentity identity, IGnssStatusListener listener) {
        super.addListener(identity, (CallerIdentity) listener);
    }

    protected boolean registerWithService(Void ignored, Collection<GnssListenerMultiplexer<Void, IGnssStatusListener, Void>.GnssListenerRegistration> registrations) {
        if (this.mGnssNative.startSvStatusCollection()) {
            if (GnssManagerService.D) {
                Log.d(GnssManagerService.TAG, "starting gnss sv status");
                return true;
            }
            return true;
        }
        Log.e(GnssManagerService.TAG, "error starting gnss sv status");
        return false;
    }

    @Override // com.android.server.location.listeners.ListenerMultiplexer
    protected void unregisterWithService() {
        if (this.mGnssNative.stopSvStatusCollection()) {
            if (GnssManagerService.D) {
                Log.d(GnssManagerService.TAG, "stopping gnss sv status");
                return;
            }
            return;
        }
        Log.e(GnssManagerService.TAG, "error stopping gnss sv status");
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.listeners.ListenerMultiplexer
    public void onRegistrationAdded(IBinder key, GnssListenerMultiplexer<Void, IGnssStatusListener, Void>.GnssListenerRegistration registration) {
        this.mLogger.logLocationApiUsage(0, 3, registration.getIdentity().getPackageName(), registration.getIdentity().getAttributionTag(), null, null, true, false, null, registration.isForeground());
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.listeners.ListenerMultiplexer
    public void onRegistrationRemoved(IBinder key, GnssListenerMultiplexer<Void, IGnssStatusListener, Void>.GnssListenerRegistration registration) {
        this.mLogger.logLocationApiUsage(1, 3, registration.getIdentity().getPackageName(), registration.getIdentity().getAttributionTag(), null, null, true, false, null, registration.isForeground());
    }

    @Override // com.android.server.location.gnss.hal.GnssNative.BaseCallbacks
    public void onHalRestarted() {
        resetService();
    }

    @Override // com.android.server.location.gnss.hal.GnssNative.StatusCallbacks
    public void onReportStatus(int gnssStatus) {
        boolean isNavigating;
        switch (gnssStatus) {
            case 1:
                isNavigating = true;
                break;
            case 2:
            case 4:
                isNavigating = false;
                break;
            case 3:
            default:
                isNavigating = this.mIsNavigating;
                break;
        }
        if (isNavigating != this.mIsNavigating) {
            this.mIsNavigating = isNavigating;
            if (isNavigating) {
                deliverToListeners(new ListenerExecutor.ListenerOperation() { // from class: com.android.server.location.gnss.GnssStatusProvider$$ExternalSyntheticLambda1
                    public final void operate(Object obj) {
                        ((IGnssStatusListener) obj).onGnssStarted();
                    }
                });
            } else {
                deliverToListeners(new ListenerExecutor.ListenerOperation() { // from class: com.android.server.location.gnss.GnssStatusProvider$$ExternalSyntheticLambda2
                    public final void operate(Object obj) {
                        ((IGnssStatusListener) obj).onGnssStopped();
                    }
                });
            }
        }
    }

    @Override // com.android.server.location.gnss.hal.GnssNative.StatusCallbacks
    public void onReportFirstFix(final int ttff) {
        deliverToListeners(new ListenerExecutor.ListenerOperation() { // from class: com.android.server.location.gnss.GnssStatusProvider$$ExternalSyntheticLambda0
            public final void operate(Object obj) {
                ((IGnssStatusListener) obj).onFirstFix(ttff);
            }
        });
    }

    @Override // com.android.server.location.gnss.hal.GnssNative.SvStatusCallbacks
    public void onReportSvStatus(final GnssStatus gnssStatus) {
        deliverToListeners(new Function() { // from class: com.android.server.location.gnss.GnssStatusProvider$$ExternalSyntheticLambda3
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return GnssStatusProvider.this.m4406x4db8bac7(gnssStatus, (GnssListenerMultiplexer.GnssListenerRegistration) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onReportSvStatus$2$com-android-server-location-gnss-GnssStatusProvider  reason: not valid java name */
    public /* synthetic */ ListenerExecutor.ListenerOperation m4406x4db8bac7(final GnssStatus gnssStatus, GnssListenerMultiplexer.GnssListenerRegistration registration) {
        if (this.mAppOpsHelper.noteOpNoThrow(1, registration.getIdentity())) {
            return new ListenerExecutor.ListenerOperation() { // from class: com.android.server.location.gnss.GnssStatusProvider$$ExternalSyntheticLambda4
                public final void operate(Object obj) {
                    ((IGnssStatusListener) obj).onSvStatusChanged(gnssStatus);
                }
            };
        }
        return null;
    }
}
