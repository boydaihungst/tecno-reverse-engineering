package com.android.server.location.gnss;

import android.location.IGpsGeofenceHardware;
import android.util.SparseArray;
import com.android.server.location.gnss.hal.GnssNative;
/* loaded from: classes.dex */
class GnssGeofenceProxy extends IGpsGeofenceHardware.Stub implements GnssNative.BaseCallbacks {
    private final GnssNative mGnssNative;
    private final Object mLock = new Object();
    private final SparseArray<GeofenceEntry> mGeofenceEntries = new SparseArray<>();

    /* loaded from: classes.dex */
    private static class GeofenceEntry {
        public int geofenceId;
        public int lastTransition;
        public double latitude;
        public double longitude;
        public int monitorTransitions;
        public int notificationResponsiveness;
        public boolean paused;
        public double radius;
        public int unknownTimer;

        private GeofenceEntry() {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public GnssGeofenceProxy(GnssNative gnssNative) {
        this.mGnssNative = gnssNative;
        gnssNative.addBaseCallbacks(this);
    }

    public boolean isHardwareGeofenceSupported() {
        boolean isGeofencingSupported;
        synchronized (this.mLock) {
            isGeofencingSupported = this.mGnssNative.isGeofencingSupported();
        }
        return isGeofencingSupported;
    }

    public boolean addCircularHardwareGeofence(int geofenceId, double latitude, double longitude, double radius, int lastTransition, int monitorTransitions, int notificationResponsiveness, int unknownTimer) {
        synchronized (this.mLock) {
            try {
                try {
                    boolean added = this.mGnssNative.addGeofence(geofenceId, latitude, longitude, radius, lastTransition, monitorTransitions, notificationResponsiveness, unknownTimer);
                    if (added) {
                        GeofenceEntry entry = new GeofenceEntry();
                        entry.geofenceId = geofenceId;
                        try {
                            entry.latitude = latitude;
                            try {
                                entry.longitude = longitude;
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            throw th;
                        }
                        try {
                            entry.radius = radius;
                            try {
                                entry.lastTransition = lastTransition;
                                try {
                                    entry.monitorTransitions = monitorTransitions;
                                    try {
                                        entry.notificationResponsiveness = notificationResponsiveness;
                                        entry.unknownTimer = unknownTimer;
                                        this.mGeofenceEntries.put(geofenceId, entry);
                                    } catch (Throwable th3) {
                                        th = th3;
                                        throw th;
                                    }
                                } catch (Throwable th4) {
                                    th = th4;
                                    throw th;
                                }
                            } catch (Throwable th5) {
                                th = th5;
                                throw th;
                            }
                        } catch (Throwable th6) {
                            th = th6;
                            throw th;
                        }
                    }
                    return added;
                } catch (Throwable th7) {
                    th = th7;
                }
            } catch (Throwable th8) {
                th = th8;
            }
        }
    }

    public boolean removeHardwareGeofence(int geofenceId) {
        boolean removed;
        synchronized (this.mLock) {
            removed = this.mGnssNative.removeGeofence(geofenceId);
            if (removed) {
                this.mGeofenceEntries.remove(geofenceId);
            }
        }
        return removed;
    }

    public boolean pauseHardwareGeofence(int geofenceId) {
        boolean paused;
        GeofenceEntry entry;
        synchronized (this.mLock) {
            paused = this.mGnssNative.pauseGeofence(geofenceId);
            if (paused && (entry = this.mGeofenceEntries.get(geofenceId)) != null) {
                entry.paused = true;
            }
        }
        return paused;
    }

    public boolean resumeHardwareGeofence(int geofenceId, int monitorTransitions) {
        boolean resumed;
        GeofenceEntry entry;
        synchronized (this.mLock) {
            resumed = this.mGnssNative.resumeGeofence(geofenceId, monitorTransitions);
            if (resumed && (entry = this.mGeofenceEntries.get(geofenceId)) != null) {
                entry.paused = false;
                entry.monitorTransitions = monitorTransitions;
            }
        }
        return resumed;
    }

    @Override // com.android.server.location.gnss.hal.GnssNative.BaseCallbacks
    public void onHalRestarted() {
        synchronized (this.mLock) {
            for (int i = 0; i < this.mGeofenceEntries.size(); i++) {
                GeofenceEntry entry = this.mGeofenceEntries.valueAt(i);
                boolean added = this.mGnssNative.addGeofence(entry.geofenceId, entry.latitude, entry.longitude, entry.radius, entry.lastTransition, entry.monitorTransitions, entry.notificationResponsiveness, entry.unknownTimer);
                if (added && entry.paused) {
                    this.mGnssNative.pauseGeofence(entry.geofenceId);
                }
            }
        }
    }
}
