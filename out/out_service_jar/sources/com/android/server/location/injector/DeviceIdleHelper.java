package com.android.server.location.injector;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
/* loaded from: classes.dex */
public abstract class DeviceIdleHelper {
    private final CopyOnWriteArrayList<DeviceIdleListener> mListeners = new CopyOnWriteArrayList<>();

    /* loaded from: classes.dex */
    public interface DeviceIdleListener {
        void onDeviceIdleChanged(boolean z);
    }

    public abstract boolean isDeviceIdle();

    protected abstract void registerInternal();

    protected abstract void unregisterInternal();

    public final synchronized void addListener(DeviceIdleListener listener) {
        if (this.mListeners.add(listener) && this.mListeners.size() == 1) {
            registerInternal();
        }
    }

    public final synchronized void removeListener(DeviceIdleListener listener) {
        if (this.mListeners.remove(listener) && this.mListeners.isEmpty()) {
            unregisterInternal();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void notifyDeviceIdleChanged() {
        boolean deviceIdle = isDeviceIdle();
        Iterator<DeviceIdleListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            DeviceIdleListener listener = it.next();
            listener.onDeviceIdleChanged(deviceIdle);
        }
    }
}
