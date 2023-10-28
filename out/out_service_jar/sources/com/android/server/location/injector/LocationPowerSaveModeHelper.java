package com.android.server.location.injector;

import android.os.PowerManager;
import android.util.Log;
import com.android.server.location.LocationManagerService;
import com.android.server.location.eventlog.LocationEventLog;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
/* loaded from: classes.dex */
public abstract class LocationPowerSaveModeHelper {
    private final CopyOnWriteArrayList<LocationPowerSaveModeChangedListener> mListeners = new CopyOnWriteArrayList<>();

    /* loaded from: classes.dex */
    public interface LocationPowerSaveModeChangedListener {
        void onLocationPowerSaveModeChanged(int i);
    }

    public abstract int getLocationPowerSaveMode();

    public final void addListener(LocationPowerSaveModeChangedListener listener) {
        this.mListeners.add(listener);
    }

    public final void removeListener(LocationPowerSaveModeChangedListener listener) {
        this.mListeners.remove(listener);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void notifyLocationPowerSaveModeChanged(int locationPowerSaveMode) {
        Log.d(LocationManagerService.TAG, "location power save mode is now " + PowerManager.locationPowerSaveModeToString(locationPowerSaveMode));
        LocationEventLog.EVENT_LOG.logLocationPowerSaveMode(locationPowerSaveMode);
        Iterator<LocationPowerSaveModeChangedListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            LocationPowerSaveModeChangedListener listener = it.next();
            listener.onLocationPowerSaveModeChanged(locationPowerSaveMode);
        }
    }
}
