package com.android.server.location.gnss;

import android.location.GnssNavigationMessage;
import android.location.IGnssNavigationMessageListener;
import android.location.util.identity.CallerIdentity;
import android.util.Log;
import com.android.internal.listeners.ListenerExecutor;
import com.android.server.location.gnss.GnssListenerMultiplexer;
import com.android.server.location.gnss.hal.GnssNative;
import com.android.server.location.injector.AppOpsHelper;
import com.android.server.location.injector.Injector;
import java.util.Collection;
import java.util.function.Function;
/* loaded from: classes.dex */
public class GnssNavigationMessageProvider extends GnssListenerMultiplexer<Void, IGnssNavigationMessageListener, Void> implements GnssNative.BaseCallbacks, GnssNative.NavigationMessageCallbacks {
    private final AppOpsHelper mAppOpsHelper;
    private final GnssNative mGnssNative;

    @Override // com.android.server.location.listeners.ListenerMultiplexer
    protected /* bridge */ /* synthetic */ boolean registerWithService(Object obj, Collection collection) {
        return registerWithService((Void) obj, (Collection<GnssListenerMultiplexer<Void, IGnssNavigationMessageListener, Void>.GnssListenerRegistration>) collection);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class GnssNavigationMessageListenerRegistration extends GnssListenerMultiplexer<Void, IGnssNavigationMessageListener, Void>.GnssListenerRegistration {
        protected GnssNavigationMessageListenerRegistration(CallerIdentity callerIdentity, IGnssNavigationMessageListener listener) {
            super(null, callerIdentity, listener);
        }

        @Override // com.android.server.location.gnss.GnssListenerMultiplexer.GnssListenerRegistration
        protected void onGnssListenerRegister() {
            executeOperation(new ListenerExecutor.ListenerOperation() { // from class: com.android.server.location.gnss.GnssNavigationMessageProvider$GnssNavigationMessageListenerRegistration$$ExternalSyntheticLambda0
                public final void operate(Object obj) {
                    ((IGnssNavigationMessageListener) obj).onStatusChanged(1);
                }
            });
        }
    }

    public GnssNavigationMessageProvider(Injector injector, GnssNative gnssNative) {
        super(injector);
        this.mAppOpsHelper = injector.getAppOpsHelper();
        this.mGnssNative = gnssNative;
        gnssNative.addBaseCallbacks(this);
        gnssNative.addNavigationMessageCallbacks(this);
    }

    @Override // com.android.server.location.gnss.GnssListenerMultiplexer
    public boolean isSupported() {
        return this.mGnssNative.isNavigationMessageCollectionSupported();
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.location.gnss.GnssListenerMultiplexer
    public void addListener(CallerIdentity identity, IGnssNavigationMessageListener listener) {
        super.addListener(identity, (CallerIdentity) listener);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.gnss.GnssListenerMultiplexer
    public GnssListenerMultiplexer<Void, IGnssNavigationMessageListener, Void>.GnssListenerRegistration createRegistration(Void request, CallerIdentity callerIdentity, IGnssNavigationMessageListener listener) {
        return new GnssNavigationMessageListenerRegistration(callerIdentity, listener);
    }

    protected boolean registerWithService(Void ignored, Collection<GnssListenerMultiplexer<Void, IGnssNavigationMessageListener, Void>.GnssListenerRegistration> registrations) {
        if (this.mGnssNative.startNavigationMessageCollection()) {
            if (GnssManagerService.D) {
                Log.d(GnssManagerService.TAG, "starting gnss navigation messages");
                return true;
            }
            return true;
        }
        Log.e(GnssManagerService.TAG, "error starting gnss navigation messages");
        return false;
    }

    @Override // com.android.server.location.listeners.ListenerMultiplexer
    protected void unregisterWithService() {
        if (this.mGnssNative.stopNavigationMessageCollection()) {
            if (GnssManagerService.D) {
                Log.d(GnssManagerService.TAG, "stopping gnss navigation messages");
                return;
            }
            return;
        }
        Log.e(GnssManagerService.TAG, "error stopping gnss navigation messages");
    }

    @Override // com.android.server.location.gnss.hal.GnssNative.BaseCallbacks
    public void onHalRestarted() {
        resetService();
    }

    @Override // com.android.server.location.gnss.hal.GnssNative.NavigationMessageCallbacks
    public void onReportNavigationMessage(final GnssNavigationMessage event) {
        deliverToListeners(new Function() { // from class: com.android.server.location.gnss.GnssNavigationMessageProvider$$ExternalSyntheticLambda1
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return GnssNavigationMessageProvider.this.m4379x5fb17883(event, (GnssListenerMultiplexer.GnssListenerRegistration) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onReportNavigationMessage$1$com-android-server-location-gnss-GnssNavigationMessageProvider  reason: not valid java name */
    public /* synthetic */ ListenerExecutor.ListenerOperation m4379x5fb17883(final GnssNavigationMessage event, GnssListenerMultiplexer.GnssListenerRegistration registration) {
        if (this.mAppOpsHelper.noteOpNoThrow(1, registration.getIdentity())) {
            return new ListenerExecutor.ListenerOperation() { // from class: com.android.server.location.gnss.GnssNavigationMessageProvider$$ExternalSyntheticLambda0
                public final void operate(Object obj) {
                    ((IGnssNavigationMessageListener) obj).onGnssNavigationMessageReceived(event);
                }
            };
        }
        return null;
    }
}
