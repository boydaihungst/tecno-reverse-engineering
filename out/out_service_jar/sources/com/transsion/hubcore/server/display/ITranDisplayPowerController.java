package com.transsion.hubcore.server.display;

import android.content.Context;
import com.transsion.hubcore.server.display.ITranDisplayPowerController;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranDisplayPowerController {
    public static final TranClassInfo<ITranDisplayPowerController> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.display.TranDisplayPowerControllerImpl", ITranDisplayPowerController.class, new Supplier() { // from class: com.transsion.hubcore.server.display.ITranDisplayPowerController$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranDisplayPowerController.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranDisplayPowerController {
    }

    static ITranDisplayPowerController Instance() {
        return (ITranDisplayPowerController) classInfo.getImpl();
    }

    default void hookCurDisplayState(int oldState, int state) {
    }

    default void animateFpDimLayerBrightness(float Target, int displayId) {
    }

    default void flexibleDisplayBrightnessInit(Context context) {
    }

    default boolean flexibleDisplayBrightnessSupport() {
        return false;
    }

    default boolean flexibleDisplayBrightnessSupport(int displayId) {
        return false;
    }

    default boolean flexibleDisplayBrightnessHandleSupport(int displayId) {
        return false;
    }

    default boolean getFlexibleDisplayBrightnessActive() {
        return false;
    }

    default void setFlexibleDisplayBrightnessActive(boolean state) {
    }

    default void putFlexibleDisplayBrightnessSettings(float brightness) {
    }

    default float getFlexibleDisplayBrightnessSettings() {
        return -1.0f;
    }

    default void flexibleDisplayBrightnessStop() {
    }

    default boolean isSourceConnectPolicy(int policy) {
        return false;
    }

    default boolean isConnectPolicyBrightness(int policy, float brightness) {
        return false;
    }

    default void setPowerRequest(int policy) {
    }

    default void hookCurrentPowerRequestPolicy(int policy) {
    }

    default void hookDisplayBrightnessAnimatorState(int state) {
    }

    default void hookOpticalDataAcquisitionState() {
    }

    default float calculateBrightnessUnderBacklightTemperatureControl(float animateValue, float fAmbientLux) {
        return Float.NaN;
    }

    default void initTranBacklightTemperatureController(Context context, Runnable runnable) {
    }

    default void destroyTranBacklightTemperatureController() {
    }

    default void setTranBacklightTemperatureControllerScreenState(int state) {
    }
}
