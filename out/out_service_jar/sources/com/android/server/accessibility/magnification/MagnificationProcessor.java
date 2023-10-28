package com.android.server.accessibility.magnification;

import android.accessibilityservice.MagnificationConfig;
import android.graphics.Region;
import android.view.Display;
import android.view.accessibility.MagnificationAnimationCallback;
import java.io.PrintWriter;
import java.util.ArrayList;
/* loaded from: classes.dex */
public class MagnificationProcessor {
    private static final boolean DEBUG = false;
    private static final String TAG = "MagnificationProcessor";
    private final MagnificationController mController;

    public MagnificationProcessor(MagnificationController controller) {
        this.mController = controller;
    }

    public MagnificationConfig getMagnificationConfig(int displayId) {
        int mode = getControllingMode(displayId);
        MagnificationConfig.Builder builder = new MagnificationConfig.Builder();
        if (mode == 1) {
            FullScreenMagnificationController fullScreenMagnificationController = this.mController.getFullScreenMagnificationController();
            builder.setMode(mode).setScale(fullScreenMagnificationController.getScale(displayId)).setCenterX(fullScreenMagnificationController.getCenterX(displayId)).setCenterY(fullScreenMagnificationController.getCenterY(displayId));
        } else if (mode == 2) {
            WindowMagnificationManager windowMagnificationManager = this.mController.getWindowMagnificationMgr();
            builder.setMode(mode).setScale(windowMagnificationManager.getScale(displayId)).setCenterX(windowMagnificationManager.getCenterX(displayId)).setCenterY(windowMagnificationManager.getCenterY(displayId));
        }
        return builder.build();
    }

    public boolean setMagnificationConfig(int displayId, MagnificationConfig config, boolean animate, int id) {
        if (transitionModeIfNeeded(displayId, config, animate, id)) {
            return true;
        }
        int configMode = config.getMode();
        if (configMode == 0) {
            configMode = getControllingMode(displayId);
        }
        if (configMode == 1) {
            return setScaleAndCenterForFullScreenMagnification(displayId, config.getScale(), config.getCenterX(), config.getCenterY(), animate, id);
        }
        if (configMode == 2) {
            return this.mController.getWindowMagnificationMgr().enableWindowMagnification(displayId, config.getScale(), config.getCenterX(), config.getCenterY(), animate ? MagnificationAnimationCallback.STUB_ANIMATION_CALLBACK : null, id);
        }
        return false;
    }

    private boolean setScaleAndCenterForFullScreenMagnification(int displayId, float scale, float centerX, float centerY, boolean animate, int id) {
        if (!isRegistered(displayId)) {
            register(displayId);
        }
        return this.mController.getFullScreenMagnificationController().setScaleAndCenter(displayId, scale, centerX, centerY, animate, id);
    }

    private boolean transitionModeIfNeeded(int displayId, MagnificationConfig config, boolean animate, int id) {
        int currentMode = getControllingMode(displayId);
        if (config.getMode() == 0) {
            return false;
        }
        if (currentMode != config.getMode() || this.mController.hasDisableMagnificationCallback(displayId)) {
            this.mController.transitionMagnificationConfigMode(displayId, config, animate, id);
            return true;
        }
        return false;
    }

    public float getScale(int displayId) {
        return this.mController.getFullScreenMagnificationController().getScale(displayId);
    }

    public float getCenterX(int displayId, boolean canControlMagnification) {
        boolean registeredJustForThisCall = registerDisplayMagnificationIfNeeded(displayId, canControlMagnification);
        try {
            return this.mController.getFullScreenMagnificationController().getCenterX(displayId);
        } finally {
            if (registeredJustForThisCall) {
                unregister(displayId);
            }
        }
    }

    public float getCenterY(int displayId, boolean canControlMagnification) {
        boolean registeredJustForThisCall = registerDisplayMagnificationIfNeeded(displayId, canControlMagnification);
        try {
            return this.mController.getFullScreenMagnificationController().getCenterY(displayId);
        } finally {
            if (registeredJustForThisCall) {
                unregister(displayId);
            }
        }
    }

    public void getCurrentMagnificationRegion(int displayId, Region outRegion, boolean canControlMagnification) {
        int currentMode = getControllingMode(displayId);
        if (currentMode == 1) {
            getFullscreenMagnificationRegion(displayId, outRegion, canControlMagnification);
        } else if (currentMode == 2) {
            this.mController.getWindowMagnificationMgr().getMagnificationSourceBounds(displayId, outRegion);
        }
    }

    public void getFullscreenMagnificationRegion(int displayId, Region outRegion, boolean canControlMagnification) {
        boolean registeredJustForThisCall = registerDisplayMagnificationIfNeeded(displayId, canControlMagnification);
        try {
            this.mController.getFullScreenMagnificationController().getMagnificationRegion(displayId, outRegion);
        } finally {
            if (registeredJustForThisCall) {
                unregister(displayId);
            }
        }
    }

    public boolean resetCurrentMagnification(int displayId, boolean animate) {
        int mode = getControllingMode(displayId);
        if (mode == 1) {
            return this.mController.getFullScreenMagnificationController().reset(displayId, animate);
        }
        if (mode == 2) {
            return this.mController.getWindowMagnificationMgr().disableWindowMagnification(displayId, false, animate ? MagnificationAnimationCallback.STUB_ANIMATION_CALLBACK : null);
        }
        return false;
    }

    public boolean resetFullscreenMagnification(int displayId, boolean animate) {
        return this.mController.getFullScreenMagnificationController().reset(displayId, animate);
    }

    public void resetAllIfNeeded(int connectionId) {
        this.mController.getFullScreenMagnificationController().resetAllIfNeeded(connectionId);
        this.mController.getWindowMagnificationMgr().resetAllIfNeeded(connectionId);
    }

    public boolean isMagnifying(int displayId) {
        int mode = getControllingMode(displayId);
        if (mode == 1) {
            return this.mController.getFullScreenMagnificationController().isMagnifying(displayId);
        }
        if (mode == 2) {
            return this.mController.getWindowMagnificationMgr().isWindowMagnifierEnabled(displayId);
        }
        return false;
    }

    public int getControllingMode(int displayId) {
        if (this.mController.isActivated(displayId, 2)) {
            return 2;
        }
        return (!this.mController.isActivated(displayId, 1) && this.mController.getLastMagnificationActivatedMode(displayId) == 2) ? 2 : 1;
    }

    private boolean registerDisplayMagnificationIfNeeded(int displayId, boolean canControlMagnification) {
        if (!isRegistered(displayId) && canControlMagnification) {
            register(displayId);
            return true;
        }
        return false;
    }

    private boolean isRegistered(int displayId) {
        return this.mController.getFullScreenMagnificationController().isRegistered(displayId);
    }

    private void register(int displayId) {
        this.mController.getFullScreenMagnificationController().register(displayId);
    }

    private void unregister(int displayId) {
        this.mController.getFullScreenMagnificationController().unregister(displayId);
    }

    public void dump(PrintWriter pw, ArrayList<Display> displaysList) {
        for (int i = 0; i < displaysList.size(); i++) {
            int displayId = displaysList.get(i).getDisplayId();
            MagnificationConfig config = getMagnificationConfig(displayId);
            pw.println("Magnifier on display#" + displayId);
            pw.append((CharSequence) ("    " + config)).println();
            Region region = new Region();
            getCurrentMagnificationRegion(displayId, region, true);
            if (!region.isEmpty()) {
                pw.append("    Magnification region=").append((CharSequence) region.toString()).println();
            }
            pw.append((CharSequence) ("    IdOfLastServiceToMagnify=" + getIdOfLastServiceToMagnify(config.getMode(), displayId))).println();
            dumpTrackingTypingFocusEnabledState(pw, displayId, config.getMode());
        }
        pw.append((CharSequence) ("    SupportWindowMagnification=" + this.mController.supportWindowMagnification())).println();
        pw.append((CharSequence) ("    WindowMagnificationConnectionState=" + this.mController.getWindowMagnificationMgr().getConnectionState())).println();
    }

    private int getIdOfLastServiceToMagnify(int mode, int displayId) {
        if (mode == 1) {
            return this.mController.getFullScreenMagnificationController().getIdOfLastServiceToMagnify(displayId);
        }
        return this.mController.getWindowMagnificationMgr().getIdOfLastServiceToMagnify(displayId);
    }

    private void dumpTrackingTypingFocusEnabledState(PrintWriter pw, int displayId, int mode) {
        if (mode == 2) {
            pw.append((CharSequence) ("    TrackingTypingFocusEnabled=" + this.mController.getWindowMagnificationMgr().isTrackingTypingFocusEnabled(displayId))).println();
        }
    }
}
