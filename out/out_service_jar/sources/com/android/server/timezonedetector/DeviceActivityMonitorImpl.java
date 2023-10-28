package com.android.server.timezonedetector;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.util.IndentingPrintWriter;
import android.util.Slog;
import com.android.server.timezonedetector.DeviceActivityMonitor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
/* loaded from: classes2.dex */
class DeviceActivityMonitorImpl implements DeviceActivityMonitor {
    private static final boolean DBG = false;
    private static final String LOG_TAG = "time_zone_detector";
    private final List<DeviceActivityMonitor.Listener> mListeners = new ArrayList();

    /* JADX INFO: Access modifiers changed from: package-private */
    public static DeviceActivityMonitor create(Context context, Handler handler) {
        return new DeviceActivityMonitorImpl(context, handler);
    }

    private DeviceActivityMonitorImpl(Context context, Handler handler) {
        final ContentResolver contentResolver = context.getContentResolver();
        ContentObserver airplaneModeObserver = new ContentObserver(handler) { // from class: com.android.server.timezonedetector.DeviceActivityMonitorImpl.1
            @Override // android.database.ContentObserver
            public void onChange(boolean unused) {
                try {
                    int state = Settings.Global.getInt(contentResolver, "airplane_mode_on");
                    if (state == 0) {
                        DeviceActivityMonitorImpl.this.notifyFlightComplete();
                    }
                } catch (Settings.SettingNotFoundException e) {
                    Slog.e(DeviceActivityMonitorImpl.LOG_TAG, "Unable to read airplane mode state", e);
                }
            }
        };
        contentResolver.registerContentObserver(Settings.Global.getUriFor("airplane_mode_on"), true, airplaneModeObserver);
    }

    @Override // com.android.server.timezonedetector.DeviceActivityMonitor
    public synchronized void addListener(DeviceActivityMonitor.Listener listener) {
        Objects.requireNonNull(listener);
        this.mListeners.add(listener);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void notifyFlightComplete() {
        for (DeviceActivityMonitor.Listener listener : this.mListeners) {
            listener.onFlightComplete();
        }
    }

    @Override // com.android.server.timezonedetector.Dumpable
    public void dump(IndentingPrintWriter pw, String[] args) {
    }
}
