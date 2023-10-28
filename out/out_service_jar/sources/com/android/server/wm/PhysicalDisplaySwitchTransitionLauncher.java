package com.android.server.wm;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.devicestate.DeviceStateManager;
import android.os.HandlerExecutor;
import android.window.TransitionRequestInfo;
import java.util.function.Consumer;
/* loaded from: classes2.dex */
public class PhysicalDisplaySwitchTransitionLauncher {
    private final Context mContext;
    private DeviceStateListener mDeviceStateListener;
    private final DeviceStateManager mDeviceStateManager;
    private final DisplayContent mDisplayContent;
    private boolean mIsFolded;
    private Transition mTransition;
    private final TransitionController mTransitionController;

    public PhysicalDisplaySwitchTransitionLauncher(DisplayContent displayContent, TransitionController transitionController) {
        this.mDisplayContent = displayContent;
        Context context = displayContent.mWmService.mContext;
        this.mContext = context;
        this.mTransitionController = transitionController;
        DeviceStateManager deviceStateManager = (DeviceStateManager) context.getSystemService(DeviceStateManager.class);
        this.mDeviceStateManager = deviceStateManager;
        if (deviceStateManager != null) {
            this.mDeviceStateListener = new DeviceStateListener(context);
            deviceStateManager.registerCallback(new HandlerExecutor(displayContent.mWmService.mH), this.mDeviceStateListener);
        }
    }

    public void destroy() {
        DeviceStateManager deviceStateManager = this.mDeviceStateManager;
        if (deviceStateManager != null) {
            deviceStateManager.unregisterCallback(this.mDeviceStateListener);
        }
    }

    public void requestDisplaySwitchTransitionIfNeeded(int displayId, int oldDisplayWidth, int oldDisplayHeight, int newDisplayWidth, int newDisplayHeight) {
        if (this.mTransitionController.isShellTransitionsEnabled() && this.mDisplayContent.getLastHasContent()) {
            boolean shouldRequestUnfoldTransition = !this.mIsFolded && this.mContext.getResources().getBoolean(17891803) && ValueAnimator.areAnimatorsEnabled();
            if (!shouldRequestUnfoldTransition) {
                return;
            }
            TransitionRequestInfo.DisplayChange displayChange = new TransitionRequestInfo.DisplayChange(displayId);
            Rect startAbsBounds = new Rect(0, 0, oldDisplayWidth, oldDisplayHeight);
            displayChange.setStartAbsBounds(startAbsBounds);
            Rect endAbsBounds = new Rect(0, 0, newDisplayWidth, newDisplayHeight);
            displayChange.setEndAbsBounds(endAbsBounds);
            displayChange.setPhysicalDisplayChanged(true);
            TransitionController transitionController = this.mTransitionController;
            DisplayContent displayContent = this.mDisplayContent;
            Transition t = transitionController.requestTransitionIfNeeded(6, 0, displayContent, displayContent, null, displayChange);
            if (t != null) {
                this.mDisplayContent.mAtmService.startLaunchPowerMode(2);
                this.mTransitionController.collectForDisplayChange(this.mDisplayContent, t);
                this.mTransition = t;
            }
        }
    }

    public void onDisplayUpdated() {
        Transition transition = this.mTransition;
        if (transition != null) {
            transition.setAllReady();
            this.mTransition = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class DeviceStateListener extends DeviceStateManager.FoldStateListener {
        DeviceStateListener(Context context) {
            super(context, new Consumer() { // from class: com.android.server.wm.PhysicalDisplaySwitchTransitionLauncher$DeviceStateListener$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    PhysicalDisplaySwitchTransitionLauncher.this.mIsFolded = ((Boolean) obj).booleanValue();
                }
            });
        }
    }
}
