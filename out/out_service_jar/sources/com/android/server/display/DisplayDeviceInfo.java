package com.android.server.display;

import android.hardware.display.DeviceProductInfo;
import android.view.Display;
import android.view.DisplayAddress;
import android.view.DisplayCutout;
import android.view.DisplayEventReceiver;
import android.view.RoundedCorners;
import com.android.internal.display.BrightnessSynchronizer;
import java.util.Arrays;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class DisplayDeviceInfo {
    public static final int DIFF_COLOR_MODE = 4;
    public static final int DIFF_OTHER = 2;
    public static final int DIFF_STATE = 1;
    public static final int FLAG_ALLOWED_TO_BE_DEFAULT_DISPLAY = 1;
    public static final int FLAG_ALWAYS_UNLOCKED = 32768;
    public static final int FLAG_CAN_SHOW_WITH_INSECURE_KEYGUARD = 512;
    public static final int FLAG_DESTROY_CONTENT_ON_REMOVAL = 1024;
    public static final int FLAG_MASK_DISPLAY_CUTOUT = 2048;
    public static final int FLAG_NEVER_BLANK = 32;
    public static final int FLAG_OWN_CONTENT_ONLY = 128;
    public static final int FLAG_OWN_DISPLAY_GROUP = 16384;
    public static final int FLAG_PRESENTATION = 64;
    public static final int FLAG_PRIVATE = 16;
    public static final int FLAG_ROTATES_WITH_CONTENT = 2;
    public static final int FLAG_ROUND = 256;
    public static final int FLAG_SECURE = 4;
    public static final int FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS = 4096;
    public static final int FLAG_SUPPORTS_PROTECTED_BUFFERS = 8;
    public static final int FLAG_TOUCH_FEEDBACK_DISABLED = 65536;
    public static final int FLAG_TRUSTED = 8192;
    public static final int TOUCH_EXTERNAL = 2;
    public static final int TOUCH_INTERNAL = 1;
    public static final int TOUCH_NONE = 0;
    public static final int TOUCH_VIRTUAL = 3;
    public DisplayAddress address;
    public boolean allmSupported;
    public long appVsyncOffsetNanos;
    public float brightnessDefault;
    public float brightnessMaximum;
    public float brightnessMinimum;
    public int colorMode;
    public int defaultModeId;
    public int densityDpi;
    public DeviceProductInfo deviceProductInfo;
    public DisplayCutout displayCutout;
    public int flags;
    public boolean gameContentTypeSupported;
    public Display.HdrCapabilities hdrCapabilities;
    public int height;
    public int modeId;
    public String name;
    public String ownerPackageName;
    public int ownerUid;
    public long presentationDeadlineNanos;
    public RoundedCorners roundedCorners;
    public int touch;
    public int type;
    public String uniqueId;
    public int width;
    public float xDpi;
    public float yDpi;
    public Display.Mode[] supportedModes = Display.Mode.EMPTY_ARRAY;
    public int[] supportedColorModes = {0};
    public int rotation = 0;
    public int state = 2;
    public DisplayEventReceiver.FrameRateOverride[] frameRateOverrides = new DisplayEventReceiver.FrameRateOverride[0];
    public int installOrientation = 0;

    public void setAssumedDensityForExternalDisplay(int width, int height) {
        int min = (Math.min(width, height) * 320) / 1080;
        this.densityDpi = min;
        this.xDpi = min;
        this.yDpi = min;
    }

    public boolean equals(Object o) {
        return (o instanceof DisplayDeviceInfo) && equals((DisplayDeviceInfo) o);
    }

    public boolean equals(DisplayDeviceInfo other) {
        return other != null && diff(other) == 0;
    }

    public int diff(DisplayDeviceInfo other) {
        int diff = 0;
        if (this.state != other.state) {
            diff = 0 | 1;
        }
        if (this.colorMode != other.colorMode) {
            diff |= 4;
        }
        if (!Objects.equals(this.name, other.name) || !Objects.equals(this.uniqueId, other.uniqueId) || this.width != other.width || this.height != other.height || this.modeId != other.modeId || this.defaultModeId != other.defaultModeId || !Arrays.equals(this.supportedModes, other.supportedModes) || !Arrays.equals(this.supportedColorModes, other.supportedColorModes) || !Objects.equals(this.hdrCapabilities, other.hdrCapabilities) || this.allmSupported != other.allmSupported || this.gameContentTypeSupported != other.gameContentTypeSupported || this.densityDpi != other.densityDpi || this.xDpi != other.xDpi || this.yDpi != other.yDpi || this.appVsyncOffsetNanos != other.appVsyncOffsetNanos || this.presentationDeadlineNanos != other.presentationDeadlineNanos || this.flags != other.flags || !Objects.equals(this.displayCutout, other.displayCutout) || this.touch != other.touch || this.rotation != other.rotation || this.type != other.type || !Objects.equals(this.address, other.address) || !Objects.equals(this.deviceProductInfo, other.deviceProductInfo) || this.ownerUid != other.ownerUid || !Objects.equals(this.ownerPackageName, other.ownerPackageName) || !Arrays.equals(this.frameRateOverrides, other.frameRateOverrides) || !BrightnessSynchronizer.floatEquals(this.brightnessMinimum, other.brightnessMinimum) || !BrightnessSynchronizer.floatEquals(this.brightnessMaximum, other.brightnessMaximum) || !BrightnessSynchronizer.floatEquals(this.brightnessDefault, other.brightnessDefault) || !Objects.equals(this.roundedCorners, other.roundedCorners) || this.installOrientation != other.installOrientation) {
            return diff | 2;
        }
        return diff;
    }

    public int hashCode() {
        return 0;
    }

    public void copyFrom(DisplayDeviceInfo other) {
        this.name = other.name;
        this.uniqueId = other.uniqueId;
        this.width = other.width;
        this.height = other.height;
        this.modeId = other.modeId;
        this.defaultModeId = other.defaultModeId;
        this.supportedModes = other.supportedModes;
        this.colorMode = other.colorMode;
        this.supportedColorModes = other.supportedColorModes;
        this.hdrCapabilities = other.hdrCapabilities;
        this.allmSupported = other.allmSupported;
        this.gameContentTypeSupported = other.gameContentTypeSupported;
        this.densityDpi = other.densityDpi;
        this.xDpi = other.xDpi;
        this.yDpi = other.yDpi;
        this.appVsyncOffsetNanos = other.appVsyncOffsetNanos;
        this.presentationDeadlineNanos = other.presentationDeadlineNanos;
        this.flags = other.flags;
        this.displayCutout = other.displayCutout;
        this.touch = other.touch;
        this.rotation = other.rotation;
        this.type = other.type;
        this.address = other.address;
        this.deviceProductInfo = other.deviceProductInfo;
        this.state = other.state;
        this.ownerUid = other.ownerUid;
        this.ownerPackageName = other.ownerPackageName;
        this.frameRateOverrides = other.frameRateOverrides;
        this.brightnessMinimum = other.brightnessMinimum;
        this.brightnessMaximum = other.brightnessMaximum;
        this.brightnessDefault = other.brightnessDefault;
        this.roundedCorners = other.roundedCorners;
        this.installOrientation = other.installOrientation;
    }

    public String toString() {
        DisplayEventReceiver.FrameRateOverride[] frameRateOverrideArr;
        StringBuilder sb = new StringBuilder();
        sb.append("DisplayDeviceInfo{\"");
        sb.append(this.name).append("\": uniqueId=\"").append(this.uniqueId).append("\", ");
        sb.append(this.width).append(" x ").append(this.height);
        sb.append(", modeId ").append(this.modeId);
        sb.append(", defaultModeId ").append(this.defaultModeId);
        sb.append(", supportedModes ").append(Arrays.toString(this.supportedModes));
        sb.append(", colorMode ").append(this.colorMode);
        sb.append(", supportedColorModes ").append(Arrays.toString(this.supportedColorModes));
        sb.append(", hdrCapabilities ").append(this.hdrCapabilities);
        sb.append(", allmSupported ").append(this.allmSupported);
        sb.append(", gameContentTypeSupported ").append(this.gameContentTypeSupported);
        sb.append(", density ").append(this.densityDpi);
        sb.append(", ").append(this.xDpi).append(" x ").append(this.yDpi).append(" dpi");
        sb.append(", appVsyncOff ").append(this.appVsyncOffsetNanos);
        sb.append(", presDeadline ").append(this.presentationDeadlineNanos);
        if (this.displayCutout != null) {
            sb.append(", cutout ").append(this.displayCutout);
        }
        sb.append(", touch ").append(touchToString(this.touch));
        sb.append(", rotation ").append(this.rotation);
        sb.append(", type ").append(Display.typeToString(this.type));
        if (this.address != null) {
            sb.append(", address ").append(this.address);
        }
        sb.append(", deviceProductInfo ").append(this.deviceProductInfo);
        sb.append(", state ").append(Display.stateToString(this.state));
        if (this.ownerUid != 0 || this.ownerPackageName != null) {
            sb.append(", owner ").append(this.ownerPackageName);
            sb.append(" (uid ").append(this.ownerUid).append(")");
        }
        sb.append(", frameRateOverride ");
        for (DisplayEventReceiver.FrameRateOverride frameRateOverride : this.frameRateOverrides) {
            sb.append(frameRateOverride).append(" ");
        }
        sb.append(", brightnessMinimum ").append(this.brightnessMinimum);
        sb.append(", brightnessMaximum ").append(this.brightnessMaximum);
        sb.append(", brightnessDefault ").append(this.brightnessDefault);
        if (this.roundedCorners != null) {
            sb.append(", roundedCorners ").append(this.roundedCorners);
        }
        sb.append(flagsToString(this.flags));
        sb.append(", installOrientation ").append(this.installOrientation);
        sb.append("}");
        return sb.toString();
    }

    private static String touchToString(int touch) {
        switch (touch) {
            case 0:
                return "NONE";
            case 1:
                return "INTERNAL";
            case 2:
                return "EXTERNAL";
            case 3:
                return "VIRTUAL";
            default:
                return Integer.toString(touch);
        }
    }

    private static String flagsToString(int flags) {
        StringBuilder msg = new StringBuilder();
        if ((flags & 1) != 0) {
            msg.append(", FLAG_ALLOWED_TO_BE_DEFAULT_DISPLAY");
        }
        if ((flags & 2) != 0) {
            msg.append(", FLAG_ROTATES_WITH_CONTENT");
        }
        if ((flags & 4) != 0) {
            msg.append(", FLAG_SECURE");
        }
        if ((flags & 8) != 0) {
            msg.append(", FLAG_SUPPORTS_PROTECTED_BUFFERS");
        }
        if ((flags & 16) != 0) {
            msg.append(", FLAG_PRIVATE");
        }
        if ((flags & 32) != 0) {
            msg.append(", FLAG_NEVER_BLANK");
        }
        if ((flags & 64) != 0) {
            msg.append(", FLAG_PRESENTATION");
        }
        if ((flags & 128) != 0) {
            msg.append(", FLAG_OWN_CONTENT_ONLY");
        }
        if ((flags & 256) != 0) {
            msg.append(", FLAG_ROUND");
        }
        if ((flags & 512) != 0) {
            msg.append(", FLAG_CAN_SHOW_WITH_INSECURE_KEYGUARD");
        }
        if ((flags & 2048) != 0) {
            msg.append(", FLAG_MASK_DISPLAY_CUTOUT");
        }
        return msg.toString();
    }
}
