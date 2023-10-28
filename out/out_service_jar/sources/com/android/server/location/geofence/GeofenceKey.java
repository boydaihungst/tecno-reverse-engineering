package com.android.server.location.geofence;

import android.app.PendingIntent;
import android.location.Geofence;
import com.android.server.location.listeners.PendingIntentListenerRegistration;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class GeofenceKey implements PendingIntentListenerRegistration.PendingIntentKey {
    private final Geofence mGeofence;
    private final PendingIntent mPendingIntent;

    /* JADX INFO: Access modifiers changed from: package-private */
    public GeofenceKey(PendingIntent pendingIntent, Geofence geofence) {
        this.mPendingIntent = (PendingIntent) Objects.requireNonNull(pendingIntent);
        this.mGeofence = (Geofence) Objects.requireNonNull(geofence);
    }

    @Override // com.android.server.location.listeners.PendingIntentListenerRegistration.PendingIntentKey
    public PendingIntent getPendingIntent() {
        return this.mPendingIntent;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof GeofenceKey) {
            GeofenceKey that = (GeofenceKey) o;
            return this.mPendingIntent.equals(that.mPendingIntent) && this.mGeofence.equals(that.mGeofence);
        }
        return false;
    }

    public int hashCode() {
        return this.mPendingIntent.hashCode();
    }
}
