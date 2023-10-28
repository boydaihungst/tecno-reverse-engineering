package android.accessibilityservice;

import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Slog;
import java.util.Objects;
/* loaded from: classes.dex */
public final class AccessibilityButtonController {
    private static final String LOG_TAG = "A11yButtonController";
    private ArrayMap<AccessibilityButtonCallback, Handler> mCallbacks;
    private final Object mLock = new Object();
    private final IAccessibilityServiceConnection mServiceConnection;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AccessibilityButtonController(IAccessibilityServiceConnection serviceConnection) {
        this.mServiceConnection = serviceConnection;
    }

    public boolean isAccessibilityButtonAvailable() {
        IAccessibilityServiceConnection iAccessibilityServiceConnection = this.mServiceConnection;
        if (iAccessibilityServiceConnection != null) {
            try {
                return iAccessibilityServiceConnection.isAccessibilityButtonAvailable();
            } catch (RemoteException re) {
                Slog.w(LOG_TAG, "Failed to get accessibility button availability.", re);
                re.rethrowFromSystemServer();
                return false;
            }
        }
        return false;
    }

    public void registerAccessibilityButtonCallback(AccessibilityButtonCallback callback) {
        registerAccessibilityButtonCallback(callback, new Handler(Looper.getMainLooper()));
    }

    public void registerAccessibilityButtonCallback(AccessibilityButtonCallback callback, Handler handler) {
        Objects.requireNonNull(callback);
        Objects.requireNonNull(handler);
        synchronized (this.mLock) {
            if (this.mCallbacks == null) {
                this.mCallbacks = new ArrayMap<>();
            }
            this.mCallbacks.put(callback, handler);
        }
    }

    public void unregisterAccessibilityButtonCallback(AccessibilityButtonCallback callback) {
        Objects.requireNonNull(callback);
        synchronized (this.mLock) {
            ArrayMap<AccessibilityButtonCallback, Handler> arrayMap = this.mCallbacks;
            if (arrayMap == null) {
                return;
            }
            int keyIndex = arrayMap.indexOfKey(callback);
            boolean hasKey = keyIndex >= 0;
            if (hasKey) {
                this.mCallbacks.removeAt(keyIndex);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchAccessibilityButtonClicked() {
        synchronized (this.mLock) {
            ArrayMap<AccessibilityButtonCallback, Handler> arrayMap = this.mCallbacks;
            if (arrayMap != null && !arrayMap.isEmpty()) {
                ArrayMap<AccessibilityButtonCallback, Handler> entries = new ArrayMap<>(this.mCallbacks);
                int count = entries.size();
                for (int i = 0; i < count; i++) {
                    final AccessibilityButtonCallback callback = entries.keyAt(i);
                    Handler handler = entries.valueAt(i);
                    handler.post(new Runnable() { // from class: android.accessibilityservice.AccessibilityButtonController$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            AccessibilityButtonController.this.m1xbbd1c80e(callback);
                        }
                    });
                }
                return;
            }
            Slog.w(LOG_TAG, "Received accessibility button click with no callbacks!");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$dispatchAccessibilityButtonClicked$0$android-accessibilityservice-AccessibilityButtonController  reason: not valid java name */
    public /* synthetic */ void m1xbbd1c80e(AccessibilityButtonCallback callback) {
        callback.onClicked(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchAccessibilityButtonAvailabilityChanged(final boolean available) {
        synchronized (this.mLock) {
            ArrayMap<AccessibilityButtonCallback, Handler> arrayMap = this.mCallbacks;
            if (arrayMap != null && !arrayMap.isEmpty()) {
                ArrayMap<AccessibilityButtonCallback, Handler> entries = new ArrayMap<>(this.mCallbacks);
                int count = entries.size();
                for (int i = 0; i < count; i++) {
                    final AccessibilityButtonCallback callback = entries.keyAt(i);
                    Handler handler = entries.valueAt(i);
                    handler.post(new Runnable() { // from class: android.accessibilityservice.AccessibilityButtonController$$ExternalSyntheticLambda1
                        @Override // java.lang.Runnable
                        public final void run() {
                            AccessibilityButtonController.this.m0x370b827b(callback, available);
                        }
                    });
                }
                return;
            }
            Slog.w(LOG_TAG, "Received accessibility button availability change with no callbacks!");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$dispatchAccessibilityButtonAvailabilityChanged$1$android-accessibilityservice-AccessibilityButtonController  reason: not valid java name */
    public /* synthetic */ void m0x370b827b(AccessibilityButtonCallback callback, boolean available) {
        callback.onAvailabilityChanged(this, available);
    }

    /* loaded from: classes.dex */
    public static abstract class AccessibilityButtonCallback {
        public void onClicked(AccessibilityButtonController controller) {
        }

        public void onAvailabilityChanged(AccessibilityButtonController controller, boolean available) {
        }
    }
}
