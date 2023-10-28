package com.android.server.wm;

import android.provider.DeviceConfig;
import android.provider.DeviceConfigInterface;
import com.android.server.pm.PackageManagerService;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class WindowManagerConstants {
    static final String KEY_SYSTEM_GESTURE_EXCLUSION_LOG_DEBOUNCE_MILLIS = "system_gesture_exclusion_log_debounce_millis";
    private static final int MIN_GESTURE_EXCLUSION_LIMIT_DP = 200;
    private final DeviceConfigInterface mDeviceConfig;
    private final WindowManagerGlobalLock mGlobalLock;
    private final DeviceConfig.OnPropertiesChangedListener mListenerAndroid;
    private final DeviceConfig.OnPropertiesChangedListener mListenerWindowManager;
    boolean mSystemGestureExcludedByPreQStickyImmersive;
    int mSystemGestureExclusionLimitDp;
    long mSystemGestureExclusionLogDebounceTimeoutMillis;
    private final Runnable mUpdateSystemGestureExclusionCallback;

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowManagerConstants(final WindowManagerService service, DeviceConfigInterface deviceConfig) {
        this(service.mGlobalLock, new Runnable() { // from class: com.android.server.wm.WindowManagerConstants$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                WindowManagerService.this.mRoot.forAllDisplays(new Consumer() { // from class: com.android.server.wm.WindowManagerConstants$$ExternalSyntheticLambda3
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((DisplayContent) obj).updateSystemGestureExclusionLimit();
                    }
                });
            }
        }, deviceConfig);
    }

    WindowManagerConstants(WindowManagerGlobalLock globalLock, Runnable updateSystemGestureExclusionCallback, DeviceConfigInterface deviceConfig) {
        this.mGlobalLock = (WindowManagerGlobalLock) Objects.requireNonNull(globalLock);
        this.mUpdateSystemGestureExclusionCallback = (Runnable) Objects.requireNonNull(updateSystemGestureExclusionCallback);
        this.mDeviceConfig = deviceConfig;
        this.mListenerAndroid = new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.wm.WindowManagerConstants$$ExternalSyntheticLambda0
            public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                WindowManagerConstants.this.onAndroidPropertiesChanged(properties);
            }
        };
        this.mListenerWindowManager = new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.wm.WindowManagerConstants$$ExternalSyntheticLambda1
            public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                WindowManagerConstants.this.onWindowPropertiesChanged(properties);
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void start(Executor executor) {
        this.mDeviceConfig.addOnPropertiesChangedListener(PackageManagerService.PLATFORM_PACKAGE_NAME, executor, this.mListenerAndroid);
        this.mDeviceConfig.addOnPropertiesChangedListener("window_manager", executor, this.mListenerWindowManager);
        updateSystemGestureExclusionLogDebounceMillis();
        updateSystemGestureExclusionLimitDp();
        updateSystemGestureExcludedByPreQStickyImmersive();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public void onAndroidPropertiesChanged(DeviceConfig.Properties properties) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                boolean updateSystemGestureExclusionLimit = false;
                for (String name : properties.getKeyset()) {
                    if (name == null) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    char c = 65535;
                    switch (name.hashCode()) {
                        case -1271675449:
                            if (name.equals("system_gestures_excluded_by_pre_q_sticky_immersive")) {
                                c = 1;
                                break;
                            }
                            break;
                        case 316878247:
                            if (name.equals("system_gesture_exclusion_limit_dp")) {
                                c = 0;
                                break;
                            }
                            break;
                    }
                    switch (c) {
                        case 0:
                            updateSystemGestureExclusionLimitDp();
                            updateSystemGestureExclusionLimit = true;
                            break;
                        case 1:
                            updateSystemGestureExcludedByPreQStickyImmersive();
                            updateSystemGestureExclusionLimit = true;
                            break;
                    }
                }
                if (updateSystemGestureExclusionLimit) {
                    this.mUpdateSystemGestureExclusionCallback.run();
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onWindowPropertiesChanged(DeviceConfig.Properties properties) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                for (String name : properties.getKeyset()) {
                    if (name == null) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    char c = 65535;
                    switch (name.hashCode()) {
                        case -125834358:
                            if (name.equals(KEY_SYSTEM_GESTURE_EXCLUSION_LOG_DEBOUNCE_MILLIS)) {
                                c = 0;
                                break;
                            }
                    }
                    switch (c) {
                        case 0:
                            updateSystemGestureExclusionLogDebounceMillis();
                            break;
                    }
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private void updateSystemGestureExclusionLogDebounceMillis() {
        this.mSystemGestureExclusionLogDebounceTimeoutMillis = this.mDeviceConfig.getLong("window_manager", KEY_SYSTEM_GESTURE_EXCLUSION_LOG_DEBOUNCE_MILLIS, 0L);
    }

    private void updateSystemGestureExclusionLimitDp() {
        this.mSystemGestureExclusionLimitDp = Math.max(200, this.mDeviceConfig.getInt(PackageManagerService.PLATFORM_PACKAGE_NAME, "system_gesture_exclusion_limit_dp", 0));
    }

    private void updateSystemGestureExcludedByPreQStickyImmersive() {
        this.mSystemGestureExcludedByPreQStickyImmersive = this.mDeviceConfig.getBoolean(PackageManagerService.PLATFORM_PACKAGE_NAME, "system_gestures_excluded_by_pre_q_sticky_immersive", false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw) {
        pw.println("WINDOW MANAGER CONSTANTS (dumpsys window constants):");
        pw.print("  ");
        pw.print(KEY_SYSTEM_GESTURE_EXCLUSION_LOG_DEBOUNCE_MILLIS);
        pw.print("=");
        pw.println(this.mSystemGestureExclusionLogDebounceTimeoutMillis);
        pw.print("  ");
        pw.print("system_gesture_exclusion_limit_dp");
        pw.print("=");
        pw.println(this.mSystemGestureExclusionLimitDp);
        pw.print("  ");
        pw.print("system_gestures_excluded_by_pre_q_sticky_immersive");
        pw.print("=");
        pw.println(this.mSystemGestureExcludedByPreQStickyImmersive);
        pw.println();
    }
}
