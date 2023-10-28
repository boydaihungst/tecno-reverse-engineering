package com.android.server.location.provider;

import android.location.Location;
import android.location.LocationResult;
import android.location.provider.ProviderProperties;
import android.location.provider.ProviderRequest;
import android.location.util.identity.CallerIdentity;
import android.os.Bundle;
import com.android.internal.util.ConcurrentUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Set;
/* loaded from: classes.dex */
public class MockLocationProvider extends AbstractLocationProvider {
    private Location mLocation;

    public MockLocationProvider(ProviderProperties properties, CallerIdentity identity, Set<String> extraAttributionTags) {
        super(ConcurrentUtils.DIRECT_EXECUTOR, identity, properties, extraAttributionTags);
    }

    public void setProviderAllowed(boolean allowed) {
        setAllowed(allowed);
    }

    public void setProviderLocation(Location l) {
        Location location = new Location(l);
        location.setIsFromMockProvider(true);
        this.mLocation = location;
        reportLocation(LocationResult.wrap(new Location[]{location}).validate());
    }

    @Override // com.android.server.location.provider.AbstractLocationProvider
    public void onSetRequest(ProviderRequest request) {
    }

    @Override // com.android.server.location.provider.AbstractLocationProvider
    protected void onFlush(Runnable callback) {
        callback.run();
    }

    @Override // com.android.server.location.provider.AbstractLocationProvider
    protected void onExtraCommand(int uid, int pid, String command, Bundle extras) {
    }

    @Override // com.android.server.location.provider.AbstractLocationProvider
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("last mock location=" + this.mLocation);
    }
}
