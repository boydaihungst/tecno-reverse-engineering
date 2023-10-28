package com.android.server.wm;

import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.provider.DeviceConfig;
import android.util.Slog;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class SplashScreenExceptionList {
    private static final boolean DEBUG = Build.isDebuggable();
    private static final String KEY_SPLASH_SCREEN_EXCEPTION_LIST = "splash_screen_exception_list";
    private static final String LOG_TAG = "SplashScreenExceptionList";
    private static final String NAMESPACE = "window_manager";
    private static final String OPT_OUT_METADATA_FLAG = "android.splashscreen.exception_opt_out";
    private final HashSet<String> mDeviceConfigExcludedPackages = new HashSet<>();
    private final Object mLock = new Object();
    final DeviceConfig.OnPropertiesChangedListener mOnPropertiesChangedListener;

    /* JADX INFO: Access modifiers changed from: package-private */
    public SplashScreenExceptionList(Executor executor) {
        updateDeviceConfig(DeviceConfig.getString(NAMESPACE, KEY_SPLASH_SCREEN_EXCEPTION_LIST, ""));
        DeviceConfig.OnPropertiesChangedListener onPropertiesChangedListener = new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.wm.SplashScreenExceptionList$$ExternalSyntheticLambda0
            public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                SplashScreenExceptionList.this.m8242lambda$new$0$comandroidserverwmSplashScreenExceptionList(properties);
            }
        };
        this.mOnPropertiesChangedListener = onPropertiesChangedListener;
        DeviceConfig.addOnPropertiesChangedListener(NAMESPACE, executor, onPropertiesChangedListener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-wm-SplashScreenExceptionList  reason: not valid java name */
    public /* synthetic */ void m8242lambda$new$0$comandroidserverwmSplashScreenExceptionList(DeviceConfig.Properties properties) {
        updateDeviceConfig(properties.getString(KEY_SPLASH_SCREEN_EXCEPTION_LIST, ""));
    }

    void updateDeviceConfig(String values) {
        parseDeviceConfigPackageList(values);
    }

    public boolean isException(String packageName, int targetSdk, Supplier<ApplicationInfo> infoSupplier) {
        if (targetSdk > 33) {
            return false;
        }
        synchronized (this.mLock) {
            if (DEBUG) {
                Slog.v(LOG_TAG, String.format(Locale.US, "SplashScreen checking exception for package %s (target sdk:%d) -> %s", packageName, Integer.valueOf(targetSdk), Boolean.valueOf(this.mDeviceConfigExcludedPackages.contains(packageName))));
            }
            if (!this.mDeviceConfigExcludedPackages.contains(packageName)) {
                return false;
            }
            return !isOptedOut(infoSupplier);
        }
    }

    private static boolean isOptedOut(Supplier<ApplicationInfo> infoProvider) {
        ApplicationInfo info;
        return (infoProvider == null || (info = infoProvider.get()) == null || info.metaData == null || !info.metaData.getBoolean(OPT_OUT_METADATA_FLAG, false)) ? false : true;
    }

    private void parseDeviceConfigPackageList(String rawList) {
        synchronized (this.mLock) {
            this.mDeviceConfigExcludedPackages.clear();
            String[] packages = rawList.split(",");
            for (String packageName : packages) {
                String packageNameTrimmed = packageName.trim();
                if (!packageNameTrimmed.isEmpty()) {
                    this.mDeviceConfigExcludedPackages.add(packageNameTrimmed);
                }
            }
        }
    }
}
