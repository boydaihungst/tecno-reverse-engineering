package com.transsion.hubcore.server.power;

import android.content.Context;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.power.PowerManagerService;
import com.transsion.hubcore.server.power.ITranPowerManagerService;
import com.transsion.hubcore.utils.TranClassInfo;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranPowerManagerService {
    public static final String IMPL_CLASS_NAME = "com.transsion.hubcore.server.power.TranPowerManagerServiceImpl";
    public static final TranClassInfo<ITranPowerManagerService> classInfo = new TranClassInfo<>(IMPL_CLASS_NAME, ITranPowerManagerService.class, new Supplier() { // from class: com.transsion.hubcore.server.power.ITranPowerManagerService$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranPowerManagerService.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranPowerManagerService {
    }

    static ITranPowerManagerService Instance() {
        return (ITranPowerManagerService) classInfo.getImpl();
    }

    default void onConstruct(PowerManagerService self, Context context) {
    }

    default void onBeforeSendShutdownBroadcast(Context context) {
    }

    default void onPowerManagerExtSingleton() {
    }

    default void onPowerManagerExtInit() {
    }

    default void startedNotifyFaceunlock(WindowManagerPolicy policy, int reason) {
    }

    default void startFaceUnlock(WindowManagerPolicy policy) {
    }

    default void enableTorch(boolean enable) {
    }

    default void setWakeLockApp(String packageName, boolean enable) {
    }

    default boolean isAllowAcquireWakeLock(boolean isInteractive, String packageName, int flags) {
        return false;
    }

    default Map getWakeLockAppMap() {
        return null;
    }

    default List<String> getAcquirableWakeLockAppList() {
        return null;
    }

    default List<String> getUnacquirableWakeLockAppList() {
        return null;
    }

    default void setScreenOnManagementEnable(boolean enable) {
    }

    default boolean isScreenOnManagementEnabled() {
        return false;
    }

    default void boostScreenOnLocked(String state) {
    }

    default void disconnectCameraClient() {
    }

    default void disconnectAudioClient() {
    }

    default void handleScreenOnBoostMessage() {
    }

    default void doWakePowerGroupLocked() {
    }

    default boolean doDump(FileDescriptor fd, PrintWriter pw, String[] args) {
        return false;
    }

    default void hookScreenStateFromOnToDoze() {
    }

    default void hookScreenStateFromDozeToOn() {
    }

    default boolean isNeedAodTransparent() {
        return false;
    }

    default void notifySourceConnectDied(int uid, String pkgName) {
    }

    default boolean getIsConnectSource() {
        return false;
    }

    default void setIsConnectSource(boolean isConnectSource) {
    }

    default long getConnectDimDuration() {
        return 0L;
    }

    default long getConnectUserActivityTime() {
        return 0L;
    }

    default boolean getIsConnectingState() {
        return false;
    }

    default boolean getOldConnectingState() {
        return false;
    }

    default void setOldConnectingState(boolean oldState) {
    }

    default void notifyChangeConnectState(boolean connect) {
    }

    default void isConnectSource(boolean connect) {
    }

    default void makeScreenOnByPower() {
    }

    default boolean isEnabledAbnormalHandler() {
        return false;
    }

    default boolean isEnabledForceStop() {
        return false;
    }

    default List getForceStopPackages() {
        return new ArrayList();
    }

    default HashMap getBlackPackages() {
        return new HashMap();
    }

    default void hookLongWLForceStop(String packageName) {
    }

    default boolean isSupportForgroundForeStop(String packageName) {
        return false;
    }

    default boolean isSupportForceRemoveForgroundWl(String packageName) {
        return false;
    }

    default boolean isSupportForceStopForegroundTopPackage(String packageName) {
        return false;
    }

    default int getForegroundTopForceStopInterval() {
        return 60;
    }
}
