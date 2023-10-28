package com.android.server.display;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.hardware.display.DisplayManagerInternal;
import android.view.DisplayInfo;
import android.view.SurfaceControl;
import com.android.server.display.DisplayModeDirector;
import com.android.server.display.IDisplayManagerServiceLice;
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.io.PrintWriter;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_SERVICES)
/* loaded from: classes.dex */
public interface IDisplayManagerServiceLice {
    public static final LiceInfo<IDisplayManagerServiceLice> sLiceInfo = new LiceInfo<>("com.transsion.server.display.DisplayManagerServiceLice", IDisplayManagerServiceLice.class, new Supplier() { // from class: com.android.server.display.IDisplayManagerServiceLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new IDisplayManagerServiceLice.DefaultImpl();
        }
    });

    /* loaded from: classes.dex */
    public static class DefaultImpl implements IDisplayManagerServiceLice {
    }

    static IDisplayManagerServiceLice Instance() {
        return (IDisplayManagerServiceLice) sLiceInfo.getImpl();
    }

    default Boolean onColorFadePrepare(Context context, Object colorFade, DisplayInfo displayInfo) {
        return null;
    }

    default String onColorFadeLoadShader(Context context, int displayId, int resourceId, int type) {
        return null;
    }

    default void onColorFadeInitGLShaders(Context context, int displayId, int program) {
    }

    default Boolean onColorFadeInitGLBuffers(int displayId, float[] projMatrix, int[] texNames, int[] gLBuffers) {
        return null;
    }

    default void onColorFadeDismiss(int displayId) {
    }

    default Boolean onColorFadeDraw(int displayId, float level) {
        return null;
    }

    default boolean onColorFadeDrawFaded(int displayId, int program, int projMatrixLoc, float[] projMatrix, int texMatrixLoc, float[] texMatrix, int opacityLoc, float opacity, int gammaLoc, float gamma, int[] texNames, int[] gLBuffers, int vertexLoc, int texCoordLoc) {
        return false;
    }

    default SurfaceControl.ScreenshotHardwareBuffer onColorFadeCaptureScreen(int displayId, DisplayManagerInternal displayManagerInternal) {
        return null;
    }

    default void onColorFadeDump(int displayId, PrintWriter pw) {
    }

    default boolean useColorFadeOnAnimation(boolean defaultValue) {
        return defaultValue;
    }

    default Boolean onSkipColorFadeFadesConfig(int displayId, boolean colorFadeEnabled) {
        return null;
    }

    default void onInitializeColorFadeAnimator(int displayId, ObjectAnimator colorFadeOnAnimator, ObjectAnimator colorFadeOffAnimator) {
    }

    default boolean isReadyToFadeOn(int displayId) {
        return true;
    }

    default void onSetDisplayPowerMode(Object localDisplayDevice, int mode) {
    }

    default boolean onSetDesiredDisplayModeSpecsLocked(Object localDisplayDevice, DisplayModeDirector.DesiredDisplayModeSpecs oldDisplayModeSpecs, DisplayModeDirector.DesiredDisplayModeSpecs newDisplayModeSpecs) {
        return false;
    }

    default void onSetLayerStackLocked(Object displayDevice, int oldLayerStack, int newLayerStack) {
    }

    default void onAddDisplayPowerControllerLocked(Object logicalDisplay, Object displayPowerController) {
    }

    default void onHandleLogicalDisplayRemovedLocked(Object logicalDisplay, Object displayPowerController) {
    }

    default void onSetPrimaryDisplayDeviceLocked(Object logicalDisplay, Object oldDisplayDevice, Object newDisplayDevice) {
    }

    default void setDeviceStateLockedEnter(int state, int pendingDeviceState, int deviceState, boolean interactive, boolean bootCompleted) {
    }

    default void animateScreenStateChangeLeave(int displayId, int target, float level) {
    }

    default void onColorFadeShowSurface(int displayId, SurfaceControl.Transaction transaction, SurfaceControl surfaceControl, int layer, float alpha) {
    }
}
