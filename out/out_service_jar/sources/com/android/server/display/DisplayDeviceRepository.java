package com.android.server.display;

import android.os.Trace;
import android.util.Slog;
import android.view.Display;
import android.view.DisplayAddress;
import com.android.server.display.DisplayAdapter;
import com.android.server.display.DisplayManagerService;
import com.transsion.hubcore.griffin.ITranGriffinFeature;
import com.transsion.hubcore.server.display.ITranDisplayDeviceRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class DisplayDeviceRepository implements DisplayAdapter.Listener {
    private static final Boolean DEBUG = false;
    public static final int DISPLAY_DEVICE_EVENT_ADDED = 1;
    public static final int DISPLAY_DEVICE_EVENT_CHANGED = 2;
    public static final int DISPLAY_DEVICE_EVENT_REMOVED = 3;
    private static final String TAG = "DisplayDeviceRepository";
    private final List<DisplayDevice> mDisplayDevices = new ArrayList();
    private final List<Listener> mListeners = new ArrayList();
    private final PersistentDataStore mPersistentDataStore;
    private final DisplayManagerService.SyncRoot mSyncRoot;

    /* loaded from: classes.dex */
    public interface Listener {
        void onDisplayDeviceEventLocked(DisplayDevice displayDevice, int i);

        void onTraversalRequested();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayDeviceRepository(DisplayManagerService.SyncRoot syncRoot, PersistentDataStore persistentDataStore) {
        this.mSyncRoot = syncRoot;
        this.mPersistentDataStore = persistentDataStore;
    }

    public void addListener(Listener listener) {
        this.mListeners.add(listener);
    }

    @Override // com.android.server.display.DisplayAdapter.Listener
    public void onDisplayDeviceEvent(DisplayDevice device, int event) {
        String tag = null;
        Boolean bool = DEBUG;
        if (bool.booleanValue()) {
            tag = "DisplayDeviceRepository#onDisplayDeviceEvent (event=" + event + ")";
            Trace.beginAsyncSection(tag, 0);
        }
        switch (event) {
            case 1:
                handleDisplayDeviceAdded(device);
                break;
            case 2:
                handleDisplayDeviceChanged(device);
                break;
            case 3:
                handleDisplayDeviceRemoved(device);
                break;
        }
        if (bool.booleanValue()) {
            Trace.endAsyncSection(tag, 0);
        }
    }

    @Override // com.android.server.display.DisplayAdapter.Listener
    public void onTraversalRequested() {
        int size = this.mListeners.size();
        for (int i = 0; i < size; i++) {
            this.mListeners.get(i).onTraversalRequested();
        }
    }

    public boolean containsLocked(DisplayDevice d) {
        return this.mDisplayDevices.contains(d);
    }

    public int sizeLocked() {
        return this.mDisplayDevices.size();
    }

    public void forEachLocked(Consumer<DisplayDevice> consumer) {
        int count = this.mDisplayDevices.size();
        for (int i = 0; i < count; i++) {
            consumer.accept(this.mDisplayDevices.get(i));
        }
    }

    public DisplayDevice getByAddressLocked(DisplayAddress address) {
        for (int i = this.mDisplayDevices.size() - 1; i >= 0; i--) {
            DisplayDevice device = this.mDisplayDevices.get(i);
            if (address.equals(device.getDisplayDeviceInfoLocked().address)) {
                return device;
            }
        }
        return null;
    }

    public DisplayDevice getByUniqueIdLocked(String uniqueId) {
        for (int i = this.mDisplayDevices.size() - 1; i >= 0; i--) {
            DisplayDevice displayDevice = this.mDisplayDevices.get(i);
            if (displayDevice.getUniqueId().equals(uniqueId)) {
                return displayDevice;
            }
        }
        return null;
    }

    private void handleDisplayDeviceAdded(DisplayDevice device) {
        synchronized (this.mSyncRoot) {
            DisplayDeviceInfo info = device.getDisplayDeviceInfoLocked();
            if (this.mDisplayDevices.contains(device)) {
                Slog.w(TAG, "Attempted to add already added display device: " + info);
                return;
            }
            Slog.i(TAG, "Display device added: " + info);
            device.mDebugLastLoggedDeviceInfo = info;
            this.mDisplayDevices.add(device);
            sendEventLocked(device, 1);
            if (ITranGriffinFeature.Instance().isGriffinSupport()) {
                ITranDisplayDeviceRepository.Instance().hookVirtualDisplayChanged(info.ownerUid, info.ownerPackageName, 1);
            }
        }
    }

    private void handleDisplayDeviceChanged(DisplayDevice device) {
        synchronized (this.mSyncRoot) {
            DisplayDeviceInfo info = device.getDisplayDeviceInfoLocked();
            if (!this.mDisplayDevices.contains(device)) {
                Slog.w(TAG, "Attempted to change non-existent display device: " + info);
                return;
            }
            Boolean bool = DEBUG;
            if (bool.booleanValue()) {
                Trace.beginSection("handleDisplayDeviceChanged");
            }
            int diff = device.mDebugLastLoggedDeviceInfo.diff(info);
            if (diff == 1) {
                Slog.i(TAG, "Display device changed state: \"" + info.name + "\", " + Display.stateToString(info.state));
            } else if (diff != 0) {
                Slog.i(TAG, "Display device changed: " + info);
            }
            if ((diff & 4) != 0) {
                this.mPersistentDataStore.setColorMode(device, info.colorMode);
                this.mPersistentDataStore.saveIfNeeded();
            }
            device.mDebugLastLoggedDeviceInfo = info;
            device.applyPendingDisplayDeviceInfoChangesLocked();
            sendEventLocked(device, 2);
            if (ITranGriffinFeature.Instance().isGriffinSupport()) {
                ITranDisplayDeviceRepository.Instance().hookVirtualDisplayChanged(info.ownerUid, info.ownerPackageName, 2);
            }
            if (bool.booleanValue()) {
                Trace.endSection();
            }
        }
    }

    private void handleDisplayDeviceRemoved(DisplayDevice device) {
        synchronized (this.mSyncRoot) {
            DisplayDeviceInfo info = device.getDisplayDeviceInfoLocked();
            if (!this.mDisplayDevices.remove(device)) {
                Slog.w(TAG, "Attempted to remove non-existent display device: " + info);
                return;
            }
            Slog.i(TAG, "Display device removed: " + info);
            device.mDebugLastLoggedDeviceInfo = info;
            sendEventLocked(device, 3);
            if (ITranGriffinFeature.Instance().isGriffinSupport()) {
                ITranDisplayDeviceRepository.Instance().hookVirtualDisplayChanged(info.ownerUid, info.ownerPackageName, 3);
            }
        }
    }

    private void sendEventLocked(DisplayDevice device, int event) {
        int size = this.mListeners.size();
        for (int i = 0; i < size; i++) {
            this.mListeners.get(i).onDisplayDeviceEventLocked(device, event);
        }
    }
}
