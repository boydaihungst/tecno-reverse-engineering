package com.android.server.location.injector;

import android.location.util.identity.CallerIdentity;
import com.android.server.location.LocationManagerService;
import com.android.server.location.LocationPermissions;
import com.android.server.location.injector.AppOpsHelper;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
/* loaded from: classes.dex */
public abstract class LocationPermissionsHelper {
    private final AppOpsHelper mAppOps;
    private final CopyOnWriteArrayList<LocationPermissionsListener> mListeners = new CopyOnWriteArrayList<>();

    /* loaded from: classes.dex */
    public interface LocationPermissionsListener {
        void onLocationPermissionsChanged(int i);

        void onLocationPermissionsChanged(String str);
    }

    protected abstract boolean hasPermission(String str, CallerIdentity callerIdentity);

    public LocationPermissionsHelper(AppOpsHelper appOps) {
        this.mAppOps = appOps;
        appOps.addListener(new AppOpsHelper.LocationAppOpListener() { // from class: com.android.server.location.injector.LocationPermissionsHelper$$ExternalSyntheticLambda0
            @Override // com.android.server.location.injector.AppOpsHelper.LocationAppOpListener
            public final void onAppOpsChanged(String str) {
                LocationPermissionsHelper.this.onAppOpsChanged(str);
            }
        });
    }

    protected final void notifyLocationPermissionsChanged(String packageName) {
        Iterator<LocationPermissionsListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            LocationPermissionsListener listener = it.next();
            listener.onLocationPermissionsChanged(packageName);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void notifyLocationPermissionsChanged(int uid) {
        Iterator<LocationPermissionsListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            LocationPermissionsListener listener = it.next();
            listener.onLocationPermissionsChanged(uid);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onAppOpsChanged(String packageName) {
        notifyLocationPermissionsChanged(packageName);
    }

    public final void addListener(LocationPermissionsListener listener) {
        this.mListeners.add(listener);
    }

    public final void removeListener(LocationPermissionsListener listener) {
        this.mListeners.remove(listener);
    }

    public final boolean hasLocationPermissions(int permissionLevel, CallerIdentity identity) {
        if (permissionLevel == 0 || !hasPermission(LocationPermissions.asPermission(permissionLevel), identity)) {
            return false;
        }
        if (!this.mAppOps.checkOpNoThrow(LocationPermissions.asAppOp(permissionLevel), identity) && !LocationManagerService.mtkCheckCoarseLocationAccess(identity)) {
            return false;
        }
        return true;
    }
}
