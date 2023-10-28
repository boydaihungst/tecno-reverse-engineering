package com.transsion.hubcore.server.wm;

import android.content.Context;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.DisplayPolicy;
import com.android.server.wm.WindowState;
import com.transsion.hubcore.server.wm.ITranDisplayPolicy;
import com.transsion.hubcore.utils.TranClassInfo;
import java.io.PrintWriter;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranDisplayPolicy {
    public static final TranClassInfo<ITranDisplayPolicy> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.wm.TranDisplayPolicyImpl", ITranDisplayPolicy.class, new Supplier() { // from class: com.transsion.hubcore.server.wm.ITranDisplayPolicy$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranDisplayPolicy.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranDisplayPolicy {
    }

    static ITranDisplayPolicy Instance() {
        return (ITranDisplayPolicy) classInfo.getImpl();
    }

    default boolean isNeedAodTransparent() {
        return false;
    }

    default boolean isAodWallpaperFeatureEnabled() {
        return false;
    }

    default boolean isDreamAnimationShowingState() {
        return false;
    }

    default boolean currentWindowIsShowWallpaperWindow(WindowState win) {
        return false;
    }

    default boolean isDreamAnimationFeatureEnable() {
        return false;
    }

    default boolean isThunderbackWindow(WindowState win) {
        return false;
    }

    default void initSupportMaxRefreshRate(Context context) {
    }

    default void notifyKeyguardReadyToGo(Runnable runnable, boolean isReadyToGo, long duration, String tag) {
    }

    default void hideKeyguardWindow(WindowState notificationShade, WindowManagerPolicy policy, Runnable runnable, String tag) {
    }

    default void setAwakeState(boolean awake) {
    }

    default boolean isAwakeState() {
        return false;
    }

    default void dumpRefreshPolicy(DisplayPolicy policy, PrintWriter pw) {
    }

    default void dumpsyRefreshPolicyDebug(DisplayPolicy policy, PrintWriter pw, boolean debugOn) {
    }

    default boolean isMaxSupport90HZRefreshRate() {
        return false;
    }

    default boolean isMaxSupport120HZRefreshRate() {
        return false;
    }
}
