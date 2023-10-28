package com.transsion.hubcore.server.policy;

import android.content.Context;
import android.telecom.TelecomManager;
import android.util.Pair;
import android.view.KeyEvent;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.policy.keyguard.KeyguardServiceDelegate;
import com.android.server.wm.DisplayPolicy;
import com.transsion.hubcore.server.policy.ITranPhoneWindowManager;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranPhoneWindowManager {
    public static final TranClassInfo<ITranPhoneWindowManager> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.policy.TranPhoneWindowManagerImpl", ITranPhoneWindowManager.class, new Supplier() { // from class: com.transsion.hubcore.server.policy.ITranPhoneWindowManager$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranPhoneWindowManager.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranPhoneWindowManager {
    }

    static ITranPhoneWindowManager Instance() {
        return (ITranPhoneWindowManager) classInfo.getImpl();
    }

    default void onStartedGoingToSleep(KeyguardServiceDelegate mKeyguardDelegate) {
    }

    default void onFinishedGoingToSleep() {
    }

    default void onFinishedWakingUp(KeyguardServiceDelegate mKeyguardDelegate) {
    }

    default void onScreenTurnedOff() {
    }

    default void onScreenTurningOn(KeyguardServiceDelegate mKeyguardDelegate) {
    }

    default void onScreenTurningOnLock(KeyguardServiceDelegate mKeyguardDelegate) {
    }

    default void onScreenTurnedOn() {
    }

    default void onSystemBooted() {
    }

    default void onSystemBootedEnd() {
    }

    default void onInterceptScrollShot(KeyEvent event) {
    }

    default Pair<Boolean, Integer> onInterceptKeyBeforeQueueing(KeyEvent event) {
        return new Pair<>(false, null);
    }

    default void onInit(Context context) {
    }

    default Pair<Boolean, Long> onInterceptKeyBeforeDispatchingInner(KeyEvent event, int policyFlags) {
        return new Pair<>(false, null);
    }

    default Pair<Boolean, Integer> getWindowLayerFromTypeLw(int type, boolean canAddInternalSystemWindow, boolean roundedCornerOverlay) {
        return null;
    }

    default void onKeyguardHideComplete(WindowManagerPolicy policy) {
    }

    default void setProximityPowerKeyDown(WindowManagerPolicy policy, boolean powerKeyDown) {
    }

    default boolean isProximityPowerKeyDown(WindowManagerPolicy policy) {
        return false;
    }

    default void onSystemUiStarted() {
    }

    default boolean isFpInterceptPower(KeyEvent keyEvent) {
        return false;
    }

    default boolean isPreWakeupInProgress() {
        return false;
    }

    default void onFinishKeyguardDrawn() {
    }

    default void onFinishKeyguardDrawnLock() {
    }

    default void onStartedWakingUp() {
    }

    default void onSetCurrentUserLw(int newUserId) {
    }

    default boolean isWindowAnimationForDream() {
        return false;
    }

    default void onInterceptRingerToggleChord(Context mContext, KeyguardServiceDelegate mKeyguardDelegate, int mCurrentUserId) {
    }

    default void onInterceptPowerKeyDown(WindowManagerPolicy policy) {
    }

    default void onInterceptPowerKeyUp(WindowManagerPolicy policy) {
    }

    default void onInterceptPowerKeyBeforeQueueing(KeyEvent event, boolean interactive) {
    }

    default void onInterceptVolumeKeyBeforeQueueing(WindowManagerPolicy policy, KeyEvent event, boolean interactive) {
    }

    default void onInterceptFingerprintKeyBeforeQueueing(KeyEvent event) {
    }

    default void onInterceptFingerprintKeyByPowerBeforeQueueing(KeyEvent event) {
    }

    default boolean isInterceptPowerShortPressGoToSleep() {
        return false;
    }

    default void onPowerLongPressGlobal() {
    }

    default void onHandleAIVA() {
    }

    default boolean canHideByFingerprint() {
        return false;
    }

    default long onHandleStartTransitionForKeyguardLw(DisplayPolicy mDefaultDisplayPolicy, long duration) {
        return duration;
    }

    default boolean isInMidtest(KeyEvent event) {
        return false;
    }

    default boolean isKeysTest(boolean powerKeyDown) {
        return false;
    }

    default void setConnectScreenActive(boolean connectActive) {
    }

    default boolean makeScreenOnByPower(boolean down) {
        return true;
    }

    default boolean interceptMotionBeforeQueueingNonInteractive(int policyFlags) {
        return false;
    }

    default boolean handleVolumeKey(Context context, KeyEvent event, TelecomManager telecomMgr, boolean interactive) {
        return false;
    }

    default boolean isDisableLauncherUnlockAnimation() {
        return false;
    }

    default void stopPreWakeupInProgress(WindowManagerPolicy policy) {
    }

    default void startPreWakeupInProgress(WindowManagerPolicy policy, boolean prewakeup) {
    }
}
