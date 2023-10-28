package com.android.server.display;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayViewport;
import android.os.IBinder;
import android.view.Display;
import android.view.DisplayAddress;
import android.view.Surface;
import android.view.SurfaceControl;
import com.android.server.display.DisplayModeDirector;
import java.io.PrintWriter;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public abstract class DisplayDevice {
    private static final Display.Mode EMPTY_DISPLAY_MODE = new Display.Mode.Builder().build();
    private final Context mContext;
    private Rect mCurrentDisplayRect;
    private Rect mCurrentLayerStackRect;
    private Surface mCurrentSurface;
    DisplayDeviceInfo mDebugLastLoggedDeviceInfo;
    private final DisplayAdapter mDisplayAdapter;
    private final IBinder mDisplayToken;
    private final String mUniqueId;
    private int mCurrentLayerStack = -1;
    private int mCurrentFlags = 0;
    private int mCurrentOrientation = -1;
    protected DisplayDeviceConfig mDisplayDeviceConfig = null;

    public abstract DisplayDeviceInfo getDisplayDeviceInfoLocked();

    public abstract boolean hasStableUniqueId();

    public DisplayDevice(DisplayAdapter displayAdapter, IBinder displayToken, String uniqueId, Context context) {
        this.mDisplayAdapter = displayAdapter;
        this.mDisplayToken = displayToken;
        this.mUniqueId = uniqueId;
        this.mContext = context;
    }

    public final DisplayAdapter getAdapterLocked() {
        return this.mDisplayAdapter;
    }

    public DisplayDeviceConfig getDisplayDeviceConfig() {
        if (this.mDisplayDeviceConfig == null) {
            this.mDisplayDeviceConfig = loadDisplayDeviceConfig();
        }
        return this.mDisplayDeviceConfig;
    }

    public final IBinder getDisplayTokenLocked() {
        return this.mDisplayToken;
    }

    public int getDisplayIdToMirrorLocked() {
        return 0;
    }

    public boolean isWindowManagerMirroringLocked() {
        return false;
    }

    public void setWindowManagerMirroringLocked(boolean isMirroring) {
    }

    public Point getDisplaySurfaceDefaultSizeLocked() {
        return null;
    }

    public final String getNameLocked() {
        return getDisplayDeviceInfoLocked().name;
    }

    public final String getUniqueId() {
        return this.mUniqueId;
    }

    public void applyPendingDisplayDeviceInfoChangesLocked() {
    }

    public void performTraversalLocked(SurfaceControl.Transaction t) {
    }

    public Runnable requestDisplayStateLocked(int state, float brightnessState, float sdrBrightnessState) {
        return null;
    }

    public void setDesiredDisplayModeSpecsLocked(DisplayModeDirector.DesiredDisplayModeSpecs displayModeSpecs) {
    }

    public void setUserPreferredDisplayModeLocked(Display.Mode mode) {
    }

    public Display.Mode getUserPreferredDisplayModeLocked() {
        return EMPTY_DISPLAY_MODE;
    }

    public Display.Mode getSystemPreferredDisplayModeLocked() {
        return EMPTY_DISPLAY_MODE;
    }

    public Display.Mode getActiveDisplayModeAtStartLocked() {
        return EMPTY_DISPLAY_MODE;
    }

    public void setRequestedColorModeLocked(int colorMode) {
    }

    public void setAutoLowLatencyModeLocked(boolean on) {
    }

    public void setGameContentTypeLocked(boolean on) {
    }

    public void onOverlayChangedLocked() {
    }

    public final void setLayerStackLocked(SurfaceControl.Transaction t, int layerStack) {
        if (this.mCurrentLayerStack != layerStack) {
            int lastLayerStack = this.mCurrentLayerStack;
            this.mCurrentLayerStack = layerStack;
            t.setDisplayLayerStack(this.mDisplayToken, layerStack);
            IDisplayManagerServiceLice.Instance().onSetLayerStackLocked(this, lastLayerStack, layerStack);
        }
    }

    public final void setDisplayFlagsLocked(SurfaceControl.Transaction t, int flags) {
        if (this.mCurrentFlags != flags) {
            this.mCurrentFlags = flags;
            t.setDisplayFlags(this.mDisplayToken, flags);
        }
    }

    public final void setProjectionLocked(SurfaceControl.Transaction t, int orientation, Rect layerStackRect, Rect displayRect) {
        Rect rect;
        Rect rect2;
        if (this.mCurrentOrientation != orientation || (rect = this.mCurrentLayerStackRect) == null || !rect.equals(layerStackRect) || (rect2 = this.mCurrentDisplayRect) == null || !rect2.equals(displayRect)) {
            this.mCurrentOrientation = orientation;
            if (this.mCurrentLayerStackRect == null) {
                this.mCurrentLayerStackRect = new Rect();
            }
            this.mCurrentLayerStackRect.set(layerStackRect);
            if (this.mCurrentDisplayRect == null) {
                this.mCurrentDisplayRect = new Rect();
            }
            this.mCurrentDisplayRect.set(displayRect);
            t.setDisplayProjection(this.mDisplayToken, orientation, layerStackRect, displayRect);
        }
    }

    public final void setSurfaceLocked(SurfaceControl.Transaction t, Surface surface) {
        if (this.mCurrentSurface != surface) {
            this.mCurrentSurface = surface;
            t.setDisplaySurface(this.mDisplayToken, surface);
        }
    }

    public final void populateViewportLocked(DisplayViewport viewport) {
        viewport.orientation = this.mCurrentOrientation;
        if (this.mCurrentLayerStackRect != null) {
            viewport.logicalFrame.set(this.mCurrentLayerStackRect);
        } else {
            viewport.logicalFrame.setEmpty();
        }
        if (this.mCurrentDisplayRect != null) {
            viewport.physicalFrame.set(this.mCurrentDisplayRect);
        } else {
            viewport.physicalFrame.setEmpty();
        }
        int i = this.mCurrentOrientation;
        boolean z = true;
        if (i != 1 && i != 3) {
            z = false;
        }
        boolean isRotated = z;
        DisplayDeviceInfo info = getDisplayDeviceInfoLocked();
        viewport.deviceWidth = isRotated ? info.height : info.width;
        viewport.deviceHeight = isRotated ? info.width : info.height;
        viewport.uniqueId = info.uniqueId;
        if (info.address instanceof DisplayAddress.Physical) {
            viewport.physicalPort = Integer.valueOf(info.address.getPort());
        } else {
            viewport.physicalPort = null;
        }
    }

    public void dumpLocked(PrintWriter pw) {
        pw.println("mAdapter=" + this.mDisplayAdapter.getName());
        pw.println("mUniqueId=" + this.mUniqueId);
        pw.println("mDisplayToken=" + this.mDisplayToken);
        pw.println("mCurrentLayerStack=" + this.mCurrentLayerStack);
        pw.println("mCurrentFlags=" + this.mCurrentFlags);
        pw.println("mCurrentOrientation=" + this.mCurrentOrientation);
        pw.println("mCurrentLayerStackRect=" + this.mCurrentLayerStackRect);
        pw.println("mCurrentDisplayRect=" + this.mCurrentDisplayRect);
        pw.println("mCurrentSurface=" + this.mCurrentSurface);
    }

    private DisplayDeviceConfig loadDisplayDeviceConfig() {
        return DisplayDeviceConfig.create(this.mContext, false);
    }
}
