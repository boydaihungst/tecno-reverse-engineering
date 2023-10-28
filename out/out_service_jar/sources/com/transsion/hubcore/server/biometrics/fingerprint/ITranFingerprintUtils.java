package com.transsion.hubcore.server.biometrics.fingerprint;

import android.content.ComponentName;
import android.content.Context;
import com.android.server.biometrics.sensors.fingerprint.FingerprintUtils;
import com.transsion.hubcore.server.biometrics.fingerprint.ITranFingerprintUtils;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranFingerprintUtils {
    public static final TranClassInfo<ITranFingerprintUtils> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.biometrics.fingerprint.TranFingerprintUtilsImpl", ITranFingerprintUtils.class, new Supplier() { // from class: com.transsion.hubcore.server.biometrics.fingerprint.ITranFingerprintUtils$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranFingerprintUtils.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranFingerprintUtils {
    }

    static ITranFingerprintUtils Instance() {
        return (ITranFingerprintUtils) classInfo.getImpl();
    }

    default void vibrateFingerprintError(Context context) {
    }

    default void vibrateFingerprintSuccess(Context context) {
    }

    default void startAppForFp(Context ctx, int fingerId, int userId, FingerprintUtils fingerprintUtils) {
    }

    default void notifyAppResumeForFp(Context ctx, int userId, String packagename, String name, FingerprintUtils fingerprintUtils) {
    }

    default void notifyAppPauseForFp(Context ctx, int userId, String packagename, String name) {
    }

    default void onRetrieveSettings(Context ctx) {
    }

    default void onUpdateFocusedApp(Context ctx, String oldPackageName, ComponentName oldActivityComponent, String newPackageName, ComponentName newActivityComponent, FingerprintUtils fingerprintUtils) {
    }

    default void registerUltraPowerContentResolver(Context ctx) {
    }

    default void getHomeForFp(Context ctx) {
    }

    default boolean isInLauncher(String clientPackage) {
        return false;
    }

    default boolean isLauncher(String clientPackage, String packageName) {
        return false;
    }

    default void setVibrateState(boolean state) {
    }

    default boolean getVibrateState() {
        return false;
    }
}
