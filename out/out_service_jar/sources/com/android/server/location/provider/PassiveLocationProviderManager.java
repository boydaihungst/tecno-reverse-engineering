package com.android.server.location.provider;

import android.content.Context;
import android.location.LocationResult;
import android.location.provider.ProviderRequest;
import android.os.Binder;
import com.android.internal.util.Preconditions;
import com.android.server.location.injector.Injector;
import com.android.server.location.provider.LocationProviderManager;
import java.util.Collection;
/* loaded from: classes.dex */
public class PassiveLocationProviderManager extends LocationProviderManager {
    public PassiveLocationProviderManager(Context context, Injector injector) {
        super(context, injector, "passive", null);
    }

    @Override // com.android.server.location.provider.LocationProviderManager
    public void setRealProvider(AbstractLocationProvider provider) {
        Preconditions.checkArgument(provider instanceof PassiveLocationProvider);
        super.setRealProvider(provider);
    }

    @Override // com.android.server.location.provider.LocationProviderManager
    public void setMockProvider(MockLocationProvider provider) {
        if (provider != null) {
            throw new IllegalArgumentException("Cannot mock the passive provider");
        }
    }

    public void updateLocation(LocationResult locationResult) {
        synchronized (this.mLock) {
            PassiveLocationProvider passive = (PassiveLocationProvider) this.mProvider.getProvider();
            Preconditions.checkState(passive != null);
            long identity = Binder.clearCallingIdentity();
            passive.updateLocation(locationResult);
            Binder.restoreCallingIdentity(identity);
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.android.server.location.provider.LocationProviderManager, com.android.server.location.listeners.ListenerMultiplexer
    public ProviderRequest mergeRegistrations(Collection<LocationProviderManager.Registration> registrations) {
        return new ProviderRequest.Builder().setIntervalMillis(0L).build();
    }

    @Override // com.android.server.location.provider.LocationProviderManager
    protected long calculateRequestDelayMillis(long newIntervalMs, Collection<LocationProviderManager.Registration> registrations) {
        return 0L;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.provider.LocationProviderManager, com.android.server.location.listeners.ListenerMultiplexer
    public String getServiceState() {
        return this.mProvider.getCurrentRequest().isActive() ? "registered" : "unregistered";
    }
}
