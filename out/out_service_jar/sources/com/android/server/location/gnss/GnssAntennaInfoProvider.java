package com.android.server.location.gnss;

import android.location.GnssAntennaInfo;
import android.location.IGnssAntennaInfoListener;
import android.location.util.identity.CallerIdentity;
import android.os.Binder;
import android.os.IBinder;
import com.android.internal.listeners.ListenerExecutor;
import com.android.server.location.gnss.hal.GnssNative;
import com.android.server.location.listeners.BinderListenerRegistration;
import com.android.server.location.listeners.ListenerMultiplexer;
import com.android.server.location.listeners.ListenerRegistration;
import java.util.Collection;
import java.util.List;
/* loaded from: classes.dex */
public class GnssAntennaInfoProvider extends ListenerMultiplexer<IBinder, IGnssAntennaInfoListener, ListenerRegistration<IGnssAntennaInfoListener>, Void> implements GnssNative.BaseCallbacks, GnssNative.AntennaInfoCallbacks {
    private volatile List<GnssAntennaInfo> mAntennaInfos;
    private final GnssNative mGnssNative;

    /* loaded from: classes.dex */
    protected class AntennaInfoListenerRegistration extends BinderListenerRegistration<Void, IGnssAntennaInfoListener> {
        protected AntennaInfoListenerRegistration(CallerIdentity callerIdentity, IGnssAntennaInfoListener listener) {
            super(null, callerIdentity, listener);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.location.listeners.RemovableListenerRegistration
        public GnssAntennaInfoProvider getOwner() {
            return GnssAntennaInfoProvider.this;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public GnssAntennaInfoProvider(GnssNative gnssNative) {
        this.mGnssNative = gnssNative;
        gnssNative.addBaseCallbacks(this);
        gnssNative.addAntennaInfoCallbacks(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<GnssAntennaInfo> getAntennaInfos() {
        return this.mAntennaInfos;
    }

    @Override // com.android.server.location.listeners.ListenerMultiplexer
    public String getTag() {
        return GnssManagerService.TAG;
    }

    public boolean isSupported() {
        return this.mGnssNative.isAntennaInfoSupported();
    }

    public void addListener(CallerIdentity callerIdentity, IGnssAntennaInfoListener listener) {
        long identity = Binder.clearCallingIdentity();
        try {
            putRegistration(listener.asBinder(), new AntennaInfoListenerRegistration(callerIdentity, listener));
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void removeListener(IGnssAntennaInfoListener listener) {
        long identity = Binder.clearCallingIdentity();
        try {
            removeRegistration(listener.asBinder());
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.listeners.ListenerMultiplexer
    public boolean registerWithService(Void merged, Collection<ListenerRegistration<IGnssAntennaInfoListener>> listenerRegistrations) {
        return true;
    }

    @Override // com.android.server.location.listeners.ListenerMultiplexer
    protected void unregisterWithService() {
    }

    @Override // com.android.server.location.listeners.ListenerMultiplexer
    protected boolean isActive(ListenerRegistration<IGnssAntennaInfoListener> registration) {
        return true;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.listeners.ListenerMultiplexer
    public Void mergeRegistrations(Collection<ListenerRegistration<IGnssAntennaInfoListener>> listenerRegistrations) {
        return null;
    }

    @Override // com.android.server.location.gnss.hal.GnssNative.BaseCallbacks
    public void onHalStarted() {
        this.mGnssNative.startAntennaInfoListening();
    }

    @Override // com.android.server.location.gnss.hal.GnssNative.BaseCallbacks
    public void onHalRestarted() {
        this.mGnssNative.startAntennaInfoListening();
    }

    @Override // com.android.server.location.gnss.hal.GnssNative.AntennaInfoCallbacks
    public void onReportAntennaInfo(final List<GnssAntennaInfo> antennaInfos) {
        if (antennaInfos.equals(this.mAntennaInfos)) {
            return;
        }
        this.mAntennaInfos = antennaInfos;
        deliverToListeners(new ListenerExecutor.ListenerOperation() { // from class: com.android.server.location.gnss.GnssAntennaInfoProvider$$ExternalSyntheticLambda0
            public final void operate(Object obj) {
                ((IGnssAntennaInfoListener) obj).onGnssAntennaInfoChanged(antennaInfos);
            }
        });
    }
}
