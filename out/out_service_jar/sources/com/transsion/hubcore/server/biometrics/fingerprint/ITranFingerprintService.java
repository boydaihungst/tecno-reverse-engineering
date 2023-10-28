package com.transsion.hubcore.server.biometrics.fingerprint;

import android.content.ComponentName;
import android.content.Context;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback;
import android.hardware.fingerprint.Fingerprint;
import com.android.server.biometrics.sensors.BaseClientMonitor;
import com.android.server.biometrics.sensors.BiometricSchedulerOperation;
import com.transsion.hubcore.server.biometrics.fingerprint.ITranFingerprintService;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.Deque;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranFingerprintService {
    public static final TranClassInfo<ITranFingerprintService> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.biometrics.fingerprint.TranFingerprintServiceImpl", ITranFingerprintService.class, new Supplier() { // from class: com.transsion.hubcore.server.biometrics.fingerprint.ITranFingerprintService$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranFingerprintService.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranFingerprintService {
    }

    static ITranFingerprintService Instance() {
        return (ITranFingerprintService) classInfo.getImpl();
    }

    default void notifyModeChange(int mode, String callingPackage) {
    }

    default boolean isTheftAlertRinging(Context context) {
        return false;
    }

    default void updatePowerDownAndInteractive(boolean down, boolean interactive) {
    }

    default void setContext(Context context) {
    }

    default void setDaemonCallback(IBiometricsFingerprintClientCallback callback) {
    }

    default IBiometricsFingerprintClientCallback getDaemonCallback() {
        return null;
    }

    default void allowWakeupAndUnlock(boolean allow) {
    }

    default void onAuthenticatedHook(String currentClientPackageName, boolean authenticated) {
    }

    default void onAcquireHook(String currentClientPackageName, int acquiredInfo, int vendorCode) {
    }

    default void setCurrentUserId(int userId) {
    }

    default void fingerHwServiceDeath() {
    }

    default void updateScreenOnUnlockCount(long unlockTime) {
    }

    default void updateUnlockTime(long unlockTime) {
    }

    default void dispatchFingerprintKey(boolean down) {
    }

    default void setClientOwner(String owner) {
    }

    default void notifyKeyguardShowing(boolean keyguardShowing) {
    }

    default void notifyActivityOccludedChange(boolean occluded) {
    }

    default void notifyKeyguardGoingAway(boolean keyguardGoingAway) {
    }

    default void updateClientMonitor(BaseClientMonitor clientMonitor) {
    }

    default void updateClientStatus(boolean forceUpdateUiStatus) {
    }

    default void setKeyguardClientVisible(String opPackageName, boolean visible) {
    }

    default void setMyClientVisible(String opPackageName, boolean visible) {
    }

    default void notifyActivateFingerprint(boolean activate) {
    }

    default void notifyScreenState(int mode) {
    }

    default void notifyScreenLightChange(float rate) {
    }

    default void notifyFingerprintTouchPoint(int x, int y) {
    }

    default boolean isAuthenticating() {
        return false;
    }

    default boolean isEnrolling() {
        return false;
    }

    default void setEnrollState(boolean isenrolling) {
    }

    default void scheduleClientMonitor(BaseClientMonitor clientMonitor) {
    }

    default void cancelInternal(Deque<BiometricSchedulerOperation> pendingOperation) {
    }

    default void onClientFinished(BaseClientMonitor clientMonitor, boolean success, Deque<BiometricSchedulerOperation> pendingOperation) {
    }

    default void reset() {
    }

    default void onEnrollResult(int remaining) {
    }

    default void onAuthenticated(boolean authenticated) {
    }

    default void setAppBiometrics(int fingerId, int groupId, String packagename, int appUserId) {
    }

    default String getAppPackagename(int fingerId, int userId, String opPackageName, String attributionTag) {
        return null;
    }

    default Fingerprint getAddFingerprint(int userId, String opPackageName, String attributionTag) {
        return null;
    }

    default boolean hasAppPackagename(int userId, String opPackageName, String attributionTag) {
        return false;
    }

    default boolean checkName(int userId, String opPackageName, String name, String attributionTag) {
        return false;
    }

    default void startAppForFp(int userId, int fingerId) {
    }

    default void notifyAppResumeForFp(int userId, String packagename, String name) {
    }

    default void notifyAppPauseForFp(int userId, String packagename, String name) {
    }

    default int getAppUserId(int fingerId, int groupId) {
        return 0;
    }

    default void onUpdateFocusedApp(String oldPackageName, ComponentName oldComponent, String newPackageName, ComponentName newComponent) {
    }

    default void registerPackageReceiver(Context context) {
    }
}
