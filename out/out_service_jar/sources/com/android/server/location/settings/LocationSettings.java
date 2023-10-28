package com.android.server.location.settings;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Environment;
import android.os.RemoteException;
import android.util.IndentingPrintWriter;
import android.util.SparseArray;
import com.android.server.FgThread;
import com.android.server.location.settings.LocationSettings;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
/* loaded from: classes.dex */
public class LocationSettings {
    private static final String LOCATION_DIRNAME = "location";
    private static final String LOCATION_SETTINGS_FILENAME = "settings";
    final Context mContext;
    private final SparseArray<LocationUserSettingsStore> mUserSettings = new SparseArray<>(1);
    private final CopyOnWriteArrayList<LocationUserSettingsListener> mUserSettingsListeners = new CopyOnWriteArrayList<>();

    /* loaded from: classes.dex */
    public interface LocationUserSettingsListener {
        void onLocationUserSettingsChanged(int i, LocationUserSettings locationUserSettings, LocationUserSettings locationUserSettings2);
    }

    public LocationSettings(Context context) {
        this.mContext = context;
    }

    public final void registerLocationUserSettingsListener(LocationUserSettingsListener listener) {
        this.mUserSettingsListeners.add(listener);
    }

    public final void unregisterLocationUserSettingsListener(LocationUserSettingsListener listener) {
        this.mUserSettingsListeners.remove(listener);
    }

    protected File getUserSettingsDir(int userId) {
        return Environment.getDataSystemDeDirectory(userId);
    }

    protected LocationUserSettingsStore createUserSettingsStore(int userId, File file) {
        return new LocationUserSettingsStore(userId, file);
    }

    private LocationUserSettingsStore getUserSettingsStore(int userId) {
        LocationUserSettingsStore settingsStore;
        synchronized (this.mUserSettings) {
            settingsStore = this.mUserSettings.get(userId);
            if (settingsStore == null) {
                File file = new File(new File(getUserSettingsDir(userId), LOCATION_DIRNAME), LOCATION_SETTINGS_FILENAME);
                settingsStore = createUserSettingsStore(userId, file);
                this.mUserSettings.put(userId, settingsStore);
            }
        }
        return settingsStore;
    }

    public final LocationUserSettings getUserSettings(int userId) {
        return getUserSettingsStore(userId).get();
    }

    public final void updateUserSettings(int userId, Function<LocationUserSettings, LocationUserSettings> updater) {
        getUserSettingsStore(userId).update(updater);
    }

    public final void dump(FileDescriptor fd, IndentingPrintWriter ipw, String[] args) {
        try {
            int[] userIds = ActivityManager.getService().getRunningUserIds();
            if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive")) {
                ipw.print("ADAS Location Setting: ");
                ipw.increaseIndent();
                if (userIds.length > 1) {
                    ipw.println();
                    for (int userId : userIds) {
                        ipw.print("[u");
                        ipw.print(userId);
                        ipw.print("] ");
                        ipw.println(getUserSettings(userId).isAdasGnssLocationEnabled());
                    }
                } else {
                    ipw.println(getUserSettings(userIds[0]).isAdasGnssLocationEnabled());
                }
                ipw.decreaseIndent();
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    final void flushFiles() throws InterruptedException {
        synchronized (this.mUserSettings) {
            int size = this.mUserSettings.size();
            for (int i = 0; i < size; i++) {
                this.mUserSettings.valueAt(i).flushFile();
            }
        }
    }

    final void deleteFiles() throws InterruptedException {
        synchronized (this.mUserSettings) {
            int size = this.mUserSettings.size();
            for (int i = 0; i < size; i++) {
                this.mUserSettings.valueAt(i).deleteFile();
            }
        }
    }

    protected final void fireListeners(int userId, LocationUserSettings oldSettings, LocationUserSettings newSettings) {
        Iterator<LocationUserSettingsListener> it = this.mUserSettingsListeners.iterator();
        while (it.hasNext()) {
            LocationUserSettingsListener listener = it.next();
            listener.onLocationUserSettingsChanged(userId, oldSettings, newSettings);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class LocationUserSettingsStore extends SettingsStore<LocationUserSettings> {
        protected final int mUserId;

        LocationUserSettingsStore(int userId, File file) {
            super(file);
            this.mUserId = userId;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // com.android.server.location.settings.SettingsStore
        public LocationUserSettings read(int version, DataInput in) throws IOException {
            return filterSettings(LocationUserSettings.read(LocationSettings.this.mContext.getResources(), version, in));
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.location.settings.SettingsStore
        public void write(DataOutput out, LocationUserSettings settings) throws IOException {
            settings.write(out);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$update$0$com-android-server-location-settings-LocationSettings$LocationUserSettingsStore  reason: not valid java name */
        public /* synthetic */ LocationUserSettings m4537x555a62b2(Function updater, LocationUserSettings settings) {
            return filterSettings((LocationUserSettings) updater.apply(settings));
        }

        @Override // com.android.server.location.settings.SettingsStore
        public void update(final Function<LocationUserSettings, LocationUserSettings> updater) {
            super.update(new Function() { // from class: com.android.server.location.settings.LocationSettings$LocationUserSettingsStore$$ExternalSyntheticLambda1
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return LocationSettings.LocationUserSettingsStore.this.m4537x555a62b2(updater, (LocationUserSettings) obj);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onChange$1$com-android-server-location-settings-LocationSettings$LocationUserSettingsStore  reason: not valid java name */
        public /* synthetic */ void m4536x630fc199(LocationUserSettings oldSettings, LocationUserSettings newSettings) {
            LocationSettings.this.fireListeners(this.mUserId, oldSettings, newSettings);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.location.settings.SettingsStore
        public void onChange(final LocationUserSettings oldSettings, final LocationUserSettings newSettings) {
            FgThread.getExecutor().execute(new Runnable() { // from class: com.android.server.location.settings.LocationSettings$LocationUserSettingsStore$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    LocationSettings.LocationUserSettingsStore.this.m4536x630fc199(oldSettings, newSettings);
                }
            });
        }

        private LocationUserSettings filterSettings(LocationUserSettings settings) {
            if (settings.isAdasGnssLocationEnabled() && !LocationSettings.this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive")) {
                return settings.withAdasGnssLocationEnabled(false);
            }
            return settings;
        }
    }
}
