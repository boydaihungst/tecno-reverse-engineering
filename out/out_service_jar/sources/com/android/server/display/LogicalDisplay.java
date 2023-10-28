package com.android.server.display;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.ArraySet;
import android.util.SparseArray;
import android.view.Display;
import android.view.DisplayEventReceiver;
import android.view.DisplayInfo;
import android.view.SurfaceControl;
import com.android.server.display.DisplayModeDirector;
import com.android.server.wm.utils.InsetUtils;
import com.transsion.hubcore.server.display.ITranLogicalDisplay;
import com.transsion.hubcore.server.wm.ITranWindowManagerService;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class LogicalDisplay {
    private static final int BLANK_LAYER_STACK = -1;
    static final int DISPLAY_PHASE_DISABLED = -1;
    static final int DISPLAY_PHASE_ENABLED = 1;
    static final int DISPLAY_PHASE_LAYOUT_TRANSITION = 0;
    private static final DisplayInfo EMPTY_DISPLAY_INFO = new DisplayInfo();
    private static final String TAG = "LogicalDisplay";
    private final int mDisplayId;
    private int mDisplayOffsetX;
    private int mDisplayOffsetY;
    private boolean mDisplayScalingDisabled;
    private boolean mDualDisplay;
    private boolean mDualDisplayPowerController;
    private DisplayEventReceiver.FrameRateOverride[] mFrameRateOverrides;
    private boolean mHasContent;
    private final int mLayerStack;
    private DisplayInfo mOverrideDisplayInfo;
    private DisplayDevice mPrimaryDisplayDevice;
    private DisplayDeviceInfo mPrimaryDisplayDeviceInfo;
    private int mRequestedColorMode;
    private boolean mRequestedMinimalPostProcessing;
    private final DisplayInfo mBaseDisplayInfo = new DisplayInfo();
    private int mDisplayGroupId = -1;
    private final DisplayInfoProxy mInfo = new DisplayInfoProxy(null);
    private int[] mUserDisabledHdrTypes = new int[0];
    private DisplayModeDirector.DesiredDisplayModeSpecs mDesiredDisplayModeSpecs = new DisplayModeDirector.DesiredDisplayModeSpecs();
    private final Point mDisplayPosition = new Point();
    private final Rect mTempLayerStackRect = new Rect();
    private final Rect mTempDisplayRect = new Rect();
    private int mPhase = 1;
    private int mDeviceState = -1;
    private ArraySet<Integer> mPendingFrameRateOverrideUids = new ArraySet<>();
    private final SparseArray<Float> mTempFrameRateOverride = new SparseArray<>();

    /* loaded from: classes.dex */
    @interface DisplayPhase {
    }

    public LogicalDisplay(int displayId, int layerStack, DisplayDevice primaryDisplayDevice) {
        this.mDisplayId = displayId;
        this.mLayerStack = layerStack;
        this.mPrimaryDisplayDevice = primaryDisplayDevice;
    }

    public int getDisplayIdLocked() {
        return this.mDisplayId;
    }

    public DisplayDevice getPrimaryDisplayDeviceLocked() {
        return this.mPrimaryDisplayDevice;
    }

    public DisplayInfo getDisplayInfoLocked() {
        if (this.mInfo.get() == null) {
            DisplayInfo info = new DisplayInfo();
            info.copyFrom(this.mBaseDisplayInfo);
            DisplayInfo displayInfo = this.mOverrideDisplayInfo;
            if (displayInfo != null) {
                info.appWidth = displayInfo.appWidth;
                info.appHeight = this.mOverrideDisplayInfo.appHeight;
                info.smallestNominalAppWidth = this.mOverrideDisplayInfo.smallestNominalAppWidth;
                info.smallestNominalAppHeight = this.mOverrideDisplayInfo.smallestNominalAppHeight;
                info.largestNominalAppWidth = this.mOverrideDisplayInfo.largestNominalAppWidth;
                info.largestNominalAppHeight = this.mOverrideDisplayInfo.largestNominalAppHeight;
                info.logicalWidth = this.mOverrideDisplayInfo.logicalWidth;
                info.logicalHeight = this.mOverrideDisplayInfo.logicalHeight;
                info.physicalXDpi = this.mOverrideDisplayInfo.physicalXDpi;
                info.physicalYDpi = this.mOverrideDisplayInfo.physicalYDpi;
                info.rotation = this.mOverrideDisplayInfo.rotation;
                info.displayCutout = this.mOverrideDisplayInfo.displayCutout;
                info.logicalDensityDpi = this.mOverrideDisplayInfo.logicalDensityDpi;
                info.roundedCorners = this.mOverrideDisplayInfo.roundedCorners;
            }
            this.mInfo.set(info);
        }
        return this.mInfo.get();
    }

    public DisplayEventReceiver.FrameRateOverride[] getFrameRateOverrides() {
        return this.mFrameRateOverrides;
    }

    public ArraySet<Integer> getPendingFrameRateOverrideUids() {
        return this.mPendingFrameRateOverrideUids;
    }

    public void clearPendingFrameRateOverrideUids() {
        this.mPendingFrameRateOverrideUids = new ArraySet<>();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getNonOverrideDisplayInfoLocked(DisplayInfo outInfo) {
        outInfo.copyFrom(this.mBaseDisplayInfo);
    }

    public boolean setDisplayInfoOverrideFromWindowManagerLocked(DisplayInfo info) {
        if (info != null) {
            DisplayInfo displayInfo = this.mOverrideDisplayInfo;
            if (displayInfo == null) {
                this.mOverrideDisplayInfo = new DisplayInfo(info);
                this.mInfo.set(null);
                return true;
            } else if (!displayInfo.equals(info)) {
                this.mOverrideDisplayInfo.copyFrom(info);
                this.mInfo.set(null);
                return true;
            } else {
                return false;
            }
        } else if (this.mOverrideDisplayInfo != null) {
            this.mOverrideDisplayInfo = null;
            this.mInfo.set(null);
            return true;
        } else {
            return false;
        }
    }

    public boolean isValidLocked() {
        return this.mPrimaryDisplayDevice != null;
    }

    public void updateDisplayGroupIdLocked(int groupId) {
        if (groupId != this.mDisplayGroupId) {
            this.mDisplayGroupId = groupId;
            this.mBaseDisplayInfo.displayGroupId = groupId;
            this.mInfo.set(null);
        }
    }

    public void updateLocked(DisplayDeviceRepository deviceRepo) {
        DisplayDevice displayDevice = this.mPrimaryDisplayDevice;
        if (displayDevice == null) {
            return;
        }
        if (!deviceRepo.containsLocked(displayDevice)) {
            setPrimaryDisplayDeviceLocked(null);
            return;
        }
        DisplayDeviceInfo deviceInfo = this.mPrimaryDisplayDevice.getDisplayDeviceInfoLocked();
        if (!Objects.equals(this.mPrimaryDisplayDeviceInfo, deviceInfo)) {
            this.mBaseDisplayInfo.layerStack = this.mLayerStack;
            this.mBaseDisplayInfo.flags = 0;
            if ((deviceInfo.flags & 8) != 0) {
                this.mBaseDisplayInfo.flags |= 1;
            }
            if ((deviceInfo.flags & 4) != 0) {
                this.mBaseDisplayInfo.flags |= 2;
            }
            if ((deviceInfo.flags & 16) != 0) {
                this.mBaseDisplayInfo.flags |= 4;
                this.mBaseDisplayInfo.removeMode = 1;
            }
            if ((deviceInfo.flags & 1024) != 0) {
                this.mBaseDisplayInfo.removeMode = 1;
            }
            if ((deviceInfo.flags & 64) != 0) {
                this.mBaseDisplayInfo.flags |= 8;
            }
            if ((deviceInfo.flags & 256) != 0) {
                this.mBaseDisplayInfo.flags |= 16;
            }
            if ((deviceInfo.flags & 512) != 0) {
                this.mBaseDisplayInfo.flags |= 32;
            }
            if ((deviceInfo.flags & 4096) != 0) {
                this.mBaseDisplayInfo.flags |= 64;
            }
            if ((deviceInfo.flags & 8192) != 0) {
                this.mBaseDisplayInfo.flags |= 128;
            }
            if ((deviceInfo.flags & 16384) != 0) {
                this.mBaseDisplayInfo.flags |= 256;
            }
            if ((deviceInfo.flags & 32768) != 0) {
                this.mBaseDisplayInfo.flags |= 512;
            }
            if ((deviceInfo.flags & 65536) != 0) {
                this.mBaseDisplayInfo.flags |= 1024;
            }
            ITranLogicalDisplay.Instance().updateDisplayInfoFlag(deviceInfo.flags, this.mBaseDisplayInfo);
            Rect maskingInsets = getMaskingInsets(deviceInfo);
            int maskedWidth = (deviceInfo.width - maskingInsets.left) - maskingInsets.right;
            int maskedHeight = (deviceInfo.height - maskingInsets.top) - maskingInsets.bottom;
            this.mBaseDisplayInfo.type = deviceInfo.type;
            this.mBaseDisplayInfo.address = deviceInfo.address;
            this.mBaseDisplayInfo.deviceProductInfo = deviceInfo.deviceProductInfo;
            this.mBaseDisplayInfo.name = deviceInfo.name;
            this.mBaseDisplayInfo.uniqueId = deviceInfo.uniqueId;
            this.mBaseDisplayInfo.appWidth = maskedWidth;
            this.mBaseDisplayInfo.appHeight = maskedHeight;
            this.mBaseDisplayInfo.logicalWidth = maskedWidth;
            this.mBaseDisplayInfo.logicalHeight = maskedHeight;
            this.mBaseDisplayInfo.rotation = 0;
            this.mBaseDisplayInfo.modeId = deviceInfo.modeId;
            this.mBaseDisplayInfo.defaultModeId = deviceInfo.defaultModeId;
            this.mBaseDisplayInfo.supportedModes = (Display.Mode[]) Arrays.copyOf(deviceInfo.supportedModes, deviceInfo.supportedModes.length);
            this.mBaseDisplayInfo.colorMode = deviceInfo.colorMode;
            this.mBaseDisplayInfo.supportedColorModes = Arrays.copyOf(deviceInfo.supportedColorModes, deviceInfo.supportedColorModes.length);
            this.mBaseDisplayInfo.hdrCapabilities = deviceInfo.hdrCapabilities;
            this.mBaseDisplayInfo.userDisabledHdrTypes = this.mUserDisabledHdrTypes;
            this.mBaseDisplayInfo.minimalPostProcessingSupported = deviceInfo.allmSupported || deviceInfo.gameContentTypeSupported;
            this.mBaseDisplayInfo.logicalDensityDpi = deviceInfo.densityDpi;
            this.mBaseDisplayInfo.physicalXDpi = deviceInfo.xDpi;
            this.mBaseDisplayInfo.physicalYDpi = deviceInfo.yDpi;
            this.mBaseDisplayInfo.appVsyncOffsetNanos = deviceInfo.appVsyncOffsetNanos;
            this.mBaseDisplayInfo.presentationDeadlineNanos = deviceInfo.presentationDeadlineNanos;
            this.mBaseDisplayInfo.state = deviceInfo.state;
            this.mBaseDisplayInfo.smallestNominalAppWidth = maskedWidth;
            this.mBaseDisplayInfo.smallestNominalAppHeight = maskedHeight;
            this.mBaseDisplayInfo.largestNominalAppWidth = maskedWidth;
            this.mBaseDisplayInfo.largestNominalAppHeight = maskedHeight;
            this.mBaseDisplayInfo.ownerUid = deviceInfo.ownerUid;
            this.mBaseDisplayInfo.ownerPackageName = deviceInfo.ownerPackageName;
            boolean maskCutout = (deviceInfo.flags & 2048) != 0;
            this.mBaseDisplayInfo.displayCutout = maskCutout ? null : deviceInfo.displayCutout;
            this.mBaseDisplayInfo.displayId = this.mDisplayId;
            this.mBaseDisplayInfo.displayGroupId = this.mDisplayGroupId;
            updateFrameRateOverrides(deviceInfo);
            this.mBaseDisplayInfo.brightnessMinimum = deviceInfo.brightnessMinimum;
            this.mBaseDisplayInfo.brightnessMaximum = deviceInfo.brightnessMaximum;
            this.mBaseDisplayInfo.brightnessDefault = deviceInfo.brightnessDefault;
            this.mBaseDisplayInfo.roundedCorners = deviceInfo.roundedCorners;
            this.mBaseDisplayInfo.installOrientation = deviceInfo.installOrientation;
            this.mPrimaryDisplayDeviceInfo = deviceInfo;
            this.mInfo.set(null);
        }
    }

    private void updateFrameRateOverrides(DisplayDeviceInfo deviceInfo) {
        this.mTempFrameRateOverride.clear();
        DisplayEventReceiver.FrameRateOverride[] frameRateOverrideArr = this.mFrameRateOverrides;
        if (frameRateOverrideArr != null) {
            for (DisplayEventReceiver.FrameRateOverride frameRateOverride : frameRateOverrideArr) {
                this.mTempFrameRateOverride.put(frameRateOverride.uid, Float.valueOf(frameRateOverride.frameRateHz));
            }
        }
        DisplayEventReceiver.FrameRateOverride[] frameRateOverrideArr2 = deviceInfo.frameRateOverrides;
        this.mFrameRateOverrides = frameRateOverrideArr2;
        if (frameRateOverrideArr2 != null) {
            for (DisplayEventReceiver.FrameRateOverride frameRateOverride2 : frameRateOverrideArr2) {
                float refreshRate = this.mTempFrameRateOverride.get(frameRateOverride2.uid, Float.valueOf(0.0f)).floatValue();
                if (refreshRate == 0.0f || frameRateOverride2.frameRateHz != refreshRate) {
                    this.mTempFrameRateOverride.put(frameRateOverride2.uid, Float.valueOf(frameRateOverride2.frameRateHz));
                } else {
                    this.mTempFrameRateOverride.delete(frameRateOverride2.uid);
                }
            }
        }
        for (int i = 0; i < this.mTempFrameRateOverride.size(); i++) {
            this.mPendingFrameRateOverrideUids.add(Integer.valueOf(this.mTempFrameRateOverride.keyAt(i)));
        }
    }

    public Rect getInsets() {
        return getMaskingInsets(this.mPrimaryDisplayDeviceInfo);
    }

    private static Rect getMaskingInsets(DisplayDeviceInfo deviceInfo) {
        boolean maskCutout = (deviceInfo.flags & 2048) != 0;
        if (maskCutout && deviceInfo.displayCutout != null) {
            return deviceInfo.displayCutout.getSafeInsets();
        }
        return new Rect();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Point getDisplayPosition() {
        return new Point(this.mDisplayPosition);
    }

    public void configureDisplayLocked(SurfaceControl.Transaction t, DisplayDevice device, boolean isBlanked, boolean bootCompleted) {
        int displayRectWidth;
        int displayRectHeight;
        int displayRectLeft;
        if (bootCompleted || this.mDisplayId == 0) {
            device.setLayerStackLocked(t, isBlanked ? -1 : this.mLayerStack);
        }
        device.setDisplayFlagsLocked(t, device.getDisplayDeviceInfoLocked().touch != 0 ? 1 : 0);
        if (device == this.mPrimaryDisplayDevice) {
            device.setDesiredDisplayModeSpecsLocked(this.mDesiredDisplayModeSpecs);
            device.setRequestedColorModeLocked(this.mRequestedColorMode);
        } else {
            device.setDesiredDisplayModeSpecsLocked(new DisplayModeDirector.DesiredDisplayModeSpecs());
            device.setRequestedColorModeLocked(0);
        }
        device.setAutoLowLatencyModeLocked(this.mRequestedMinimalPostProcessing);
        device.setGameContentTypeLocked(this.mRequestedMinimalPostProcessing);
        DisplayInfo displayInfo = getDisplayInfoLocked();
        DisplayDeviceInfo displayDeviceInfo = device.getDisplayDeviceInfoLocked();
        this.mTempLayerStackRect.set(0, 0, displayInfo.logicalWidth, displayInfo.logicalHeight);
        int orientation = 0;
        if ((displayDeviceInfo.flags & 2) != 0) {
            orientation = displayInfo.rotation;
        }
        int orientation2 = (displayDeviceInfo.rotation + orientation) % 4;
        boolean rotated = orientation2 == 1 || orientation2 == 3;
        int physWidth = rotated ? displayDeviceInfo.height : displayDeviceInfo.width;
        int physHeight = rotated ? displayDeviceInfo.width : displayDeviceInfo.height;
        Rect maskingInsets = getMaskingInsets(displayDeviceInfo);
        InsetUtils.rotateInsets(maskingInsets, orientation2);
        int physWidth2 = physWidth - (maskingInsets.left + maskingInsets.right);
        int physHeight2 = physHeight - (maskingInsets.top + maskingInsets.bottom);
        if ((displayInfo.flags & 1073741824) != 0 || this.mDisplayScalingDisabled) {
            displayRectWidth = displayInfo.logicalWidth;
            displayRectHeight = displayInfo.logicalHeight;
        } else if (displayInfo.logicalHeight * physWidth2 < displayInfo.logicalWidth * physHeight2) {
            displayRectWidth = physWidth2;
            displayRectHeight = (displayInfo.logicalHeight * physWidth2) / displayInfo.logicalWidth;
        } else {
            displayRectWidth = (displayInfo.logicalWidth * physHeight2) / displayInfo.logicalHeight;
            displayRectHeight = physHeight2;
        }
        int displayRectTop = (physHeight2 - displayRectHeight) / 2;
        int displayRectLeft2 = (physWidth2 - displayRectWidth) / 2;
        int[] tempArray = ITranWindowManagerService.Instance().configureDisplayLocked((displayDeviceInfo.flags & 1) != 0, displayRectLeft2, displayRectTop, displayRectWidth, displayRectHeight);
        if (tempArray == null) {
            displayRectLeft = displayRectLeft2;
        } else {
            int displayRectLeft3 = tempArray[0];
            displayRectTop = tempArray[1];
            displayRectWidth = tempArray[2];
            displayRectHeight = tempArray[3];
            displayRectLeft = displayRectLeft3;
        }
        this.mTempDisplayRect.set(displayRectLeft, displayRectTop, displayRectLeft + displayRectWidth, displayRectTop + displayRectHeight);
        this.mTempDisplayRect.offset(maskingInsets.left, maskingInsets.top);
        if (orientation2 == 0) {
            this.mTempDisplayRect.offset(this.mDisplayOffsetX, this.mDisplayOffsetY);
        } else if (orientation2 == 1) {
            this.mTempDisplayRect.offset(this.mDisplayOffsetY, -this.mDisplayOffsetX);
        } else if (orientation2 == 2) {
            this.mTempDisplayRect.offset(-this.mDisplayOffsetX, -this.mDisplayOffsetY);
        } else {
            this.mTempDisplayRect.offset(-this.mDisplayOffsetY, this.mDisplayOffsetX);
        }
        this.mDisplayPosition.set(this.mTempDisplayRect.left, this.mTempDisplayRect.top);
        if (ITranWindowManagerService.Instance().getOneHandCurrentState() != 0 && orientation2 == 2) {
            orientation2 = 0;
        }
        device.setProjectionLocked(t, orientation2, this.mTempLayerStackRect, this.mTempDisplayRect);
    }

    public boolean hasContentLocked() {
        return this.mHasContent;
    }

    public void setHasContentLocked(boolean hasContent) {
        this.mHasContent = hasContent;
    }

    public void setDesiredDisplayModeSpecsLocked(DisplayModeDirector.DesiredDisplayModeSpecs specs) {
        this.mDesiredDisplayModeSpecs = specs;
    }

    public DisplayModeDirector.DesiredDisplayModeSpecs getDesiredDisplayModeSpecsLocked() {
        return this.mDesiredDisplayModeSpecs;
    }

    public void setRequestedColorModeLocked(int colorMode) {
        this.mRequestedColorMode = colorMode;
    }

    public boolean getRequestedMinimalPostProcessingLocked() {
        return this.mRequestedMinimalPostProcessing;
    }

    public void setRequestedMinimalPostProcessingLocked(boolean on) {
        this.mRequestedMinimalPostProcessing = on;
    }

    public int getRequestedColorModeLocked() {
        return this.mRequestedColorMode;
    }

    public int getDisplayOffsetXLocked() {
        return this.mDisplayOffsetX;
    }

    public int getDisplayOffsetYLocked() {
        return this.mDisplayOffsetY;
    }

    public void setDisplayOffsetsLocked(int x, int y) {
        this.mDisplayOffsetX = x;
        this.mDisplayOffsetY = y;
    }

    public boolean isDisplayScalingDisabled() {
        return this.mDisplayScalingDisabled;
    }

    public void setDisplayScalingDisabledLocked(boolean disableScaling) {
        this.mDisplayScalingDisabled = disableScaling;
    }

    public void setUserDisabledHdrTypes(int[] userDisabledHdrTypes) {
        if (this.mUserDisabledHdrTypes != userDisabledHdrTypes) {
            this.mUserDisabledHdrTypes = userDisabledHdrTypes;
            this.mBaseDisplayInfo.userDisabledHdrTypes = userDisabledHdrTypes;
            this.mInfo.set(null);
        }
    }

    public void swapDisplaysLocked(LogicalDisplay targetDisplay) {
        DisplayDevice oldTargetDevice = targetDisplay.setPrimaryDisplayDeviceLocked(this.mPrimaryDisplayDevice);
        setPrimaryDisplayDeviceLocked(oldTargetDevice);
    }

    public DisplayDevice setPrimaryDisplayDeviceLocked(DisplayDevice device) {
        DisplayDevice old = this.mPrimaryDisplayDevice;
        IDisplayManagerServiceLice.Instance().onSetPrimaryDisplayDeviceLocked(this, old, device);
        TranFoldDisplayCustody.instance().setPrimaryDisplayDeviceLocked(this, device);
        this.mPrimaryDisplayDevice = device;
        this.mPrimaryDisplayDeviceInfo = null;
        this.mBaseDisplayInfo.copyFrom(EMPTY_DISPLAY_INFO);
        this.mInfo.set(null);
        return old;
    }

    public void setPhase(int phase) {
        this.mPhase = phase;
    }

    public int getPhase() {
        return this.mPhase;
    }

    public boolean isEnabled() {
        int i = this.mPhase;
        return i == 1 || i == 0;
    }

    public void dumpLocked(PrintWriter pw) {
        pw.println("mDisplayId=" + this.mDisplayId);
        pw.println("mPhase=" + this.mPhase);
        pw.println("mLayerStack=" + this.mLayerStack);
        pw.println("mHasContent=" + this.mHasContent);
        pw.println("mDualDisplay=" + this.mDualDisplay);
        pw.println("mDeviceState=" + this.mDeviceState);
        pw.println("mDesiredDisplayModeSpecs={" + this.mDesiredDisplayModeSpecs + "}");
        pw.println("mRequestedColorMode=" + this.mRequestedColorMode);
        pw.println("mDisplayOffset=(" + this.mDisplayOffsetX + ", " + this.mDisplayOffsetY + ")");
        pw.println("mDisplayScalingDisabled=" + this.mDisplayScalingDisabled);
        StringBuilder append = new StringBuilder().append("mPrimaryDisplayDevice=");
        DisplayDevice displayDevice = this.mPrimaryDisplayDevice;
        pw.println(append.append(displayDevice != null ? displayDevice.getNameLocked() : "null").toString());
        pw.println("mBaseDisplayInfo=" + this.mBaseDisplayInfo);
        pw.println("mOverrideDisplayInfo=" + this.mOverrideDisplayInfo);
        pw.println("mRequestedMinimalPostProcessing=" + this.mRequestedMinimalPostProcessing);
        pw.println("mFrameRateOverrides=" + Arrays.toString(this.mFrameRateOverrides));
        pw.println("mPendingFrameRateOverrideUids=" + this.mPendingFrameRateOverrideUids);
    }

    public String toString() {
        StringWriter sw = new StringWriter();
        dumpLocked(new PrintWriter(sw));
        return sw.toString();
    }

    public void setDeviceState(int state) {
        this.mDeviceState = state;
    }

    public int getDeviceState() {
        return this.mDeviceState;
    }

    public void setAlwaysUnlocked() {
        this.mBaseDisplayInfo.flags |= 512;
    }

    public void setRemoveMode(int removeMode) {
        this.mBaseDisplayInfo.removeMode = removeMode;
    }

    public void setDualDisplay(boolean dualDisplay) {
        this.mDualDisplay = dualDisplay;
    }

    public boolean isDualDisplay() {
        return this.mDualDisplay;
    }

    public void setDualDisplayPowerController(boolean dualDPC) {
        this.mDualDisplayPowerController = dualDPC;
    }

    public boolean isDualDisplayPowerController() {
        return this.mDualDisplayPowerController;
    }
}
