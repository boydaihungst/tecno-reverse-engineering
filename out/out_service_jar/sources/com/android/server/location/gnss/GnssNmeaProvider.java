package com.android.server.location.gnss;

import android.location.IGnssNmeaListener;
import android.location.util.identity.CallerIdentity;
import android.util.Log;
import com.android.internal.listeners.ListenerExecutor;
import com.android.server.location.gnss.GnssNmeaProvider;
import com.android.server.location.gnss.hal.GnssNative;
import com.android.server.location.injector.AppOpsHelper;
import com.android.server.location.injector.Injector;
import java.util.Collection;
import java.util.function.Function;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class GnssNmeaProvider extends GnssListenerMultiplexer<Void, IGnssNmeaListener, Void> implements GnssNative.BaseCallbacks, GnssNative.NmeaCallbacks {
    private final AppOpsHelper mAppOpsHelper;
    private final GnssNative mGnssNative;
    private final byte[] mNmeaBuffer;

    @Override // com.android.server.location.listeners.ListenerMultiplexer
    protected /* bridge */ /* synthetic */ boolean registerWithService(Object obj, Collection collection) {
        return registerWithService((Void) obj, (Collection<GnssListenerMultiplexer<Void, IGnssNmeaListener, Void>.GnssListenerRegistration>) collection);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public GnssNmeaProvider(Injector injector, GnssNative gnssNative) {
        super(injector);
        this.mNmeaBuffer = new byte[120];
        this.mAppOpsHelper = injector.getAppOpsHelper();
        this.mGnssNative = gnssNative;
        gnssNative.addBaseCallbacks(this);
        gnssNative.addNmeaCallbacks(this);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.location.gnss.GnssListenerMultiplexer
    public void addListener(CallerIdentity identity, IGnssNmeaListener listener) {
        super.addListener(identity, (CallerIdentity) listener);
    }

    protected boolean registerWithService(Void ignored, Collection<GnssListenerMultiplexer<Void, IGnssNmeaListener, Void>.GnssListenerRegistration> registrations) {
        if (this.mGnssNative.startNmeaMessageCollection()) {
            if (GnssManagerService.D) {
                Log.d(GnssManagerService.TAG, "starting gnss nmea messages collection");
                return true;
            }
            return true;
        }
        Log.e(GnssManagerService.TAG, "error starting gnss nmea messages collection");
        return false;
    }

    @Override // com.android.server.location.listeners.ListenerMultiplexer
    protected void unregisterWithService() {
        if (this.mGnssNative.stopNmeaMessageCollection()) {
            if (GnssManagerService.D) {
                Log.d(GnssManagerService.TAG, "stopping gnss nmea messages collection");
                return;
            }
            return;
        }
        Log.e(GnssManagerService.TAG, "error stopping gnss nmea messages collection");
    }

    @Override // com.android.server.location.gnss.hal.GnssNative.BaseCallbacks
    public void onHalRestarted() {
        resetService();
    }

    @Override // com.android.server.location.gnss.hal.GnssNative.NmeaCallbacks
    public void onReportNmea(long timestamp) {
        deliverToListeners(new AnonymousClass1(timestamp));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.location.gnss.GnssNmeaProvider$1  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass1 implements Function<GnssListenerMultiplexer<Void, IGnssNmeaListener, Void>.GnssListenerRegistration, ListenerExecutor.ListenerOperation<IGnssNmeaListener>> {
        private String mNmea;
        final /* synthetic */ long val$timestamp;

        AnonymousClass1(long j) {
            this.val$timestamp = j;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Function
        public ListenerExecutor.ListenerOperation<IGnssNmeaListener> apply(GnssListenerMultiplexer<Void, IGnssNmeaListener, Void>.GnssListenerRegistration registration) {
            if (GnssNmeaProvider.this.mAppOpsHelper.noteOpNoThrow(1, registration.getIdentity())) {
                if (this.mNmea == null) {
                    int length = GnssNmeaProvider.this.mGnssNative.readNmea(GnssNmeaProvider.this.mNmeaBuffer, GnssNmeaProvider.this.mNmeaBuffer.length);
                    this.mNmea = new String(GnssNmeaProvider.this.mNmeaBuffer, 0, length);
                }
                final long j = this.val$timestamp;
                return new ListenerExecutor.ListenerOperation() { // from class: com.android.server.location.gnss.GnssNmeaProvider$1$$ExternalSyntheticLambda0
                    public final void operate(Object obj) {
                        GnssNmeaProvider.AnonymousClass1.this.m4405x5f595dc7(j, (IGnssNmeaListener) obj);
                    }
                };
            }
            return null;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$apply$0$com-android-server-location-gnss-GnssNmeaProvider$1  reason: not valid java name */
        public /* synthetic */ void m4405x5f595dc7(long timestamp, IGnssNmeaListener listener) throws Exception {
            listener.onNmeaReceived(timestamp, this.mNmea);
        }
    }
}
