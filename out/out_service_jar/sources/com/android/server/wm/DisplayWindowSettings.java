package com.android.server.wm;

import android.provider.Settings;
import android.view.DisplayInfo;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class DisplayWindowSettings {
    private final WindowManagerService mService;
    private final SettingsProvider mSettingsProvider;

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayWindowSettings(WindowManagerService service, SettingsProvider settingsProvider) {
        this.mService = service;
        this.mSettingsProvider = settingsProvider;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setUserRotation(DisplayContent displayContent, int rotationMode, int rotation) {
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        SettingsProvider.SettingsEntry overrideSettings = this.mSettingsProvider.getOverrideSettings(displayInfo);
        overrideSettings.mUserRotationMode = Integer.valueOf(rotationMode);
        overrideSettings.mUserRotation = Integer.valueOf(rotation);
        this.mSettingsProvider.updateOverrideSettings(displayInfo, overrideSettings);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setForcedSize(DisplayContent displayContent, int width, int height) {
        if (displayContent.isDefaultDisplay) {
            String sizeString = (width == 0 || height == 0) ? "" : width + "," + height;
            Settings.Global.putString(this.mService.mContext.getContentResolver(), "display_size_forced", sizeString);
        }
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        SettingsProvider.SettingsEntry overrideSettings = this.mSettingsProvider.getOverrideSettings(displayInfo);
        overrideSettings.mForcedWidth = width;
        overrideSettings.mForcedHeight = height;
        this.mSettingsProvider.updateOverrideSettings(displayInfo, overrideSettings);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setForcedDensity(DisplayContent displayContent, int density, int userId) {
        if (displayContent.isDefaultDisplay) {
            String densityString = density == 0 ? "" : Integer.toString(density);
            Settings.Secure.putStringForUser(this.mService.mContext.getContentResolver(), "display_density_forced", densityString, userId);
        }
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        SettingsProvider.SettingsEntry overrideSettings = this.mSettingsProvider.getOverrideSettings(displayInfo);
        overrideSettings.mForcedDensity = density;
        this.mSettingsProvider.updateOverrideSettings(displayInfo, overrideSettings);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setForcedScalingMode(DisplayContent displayContent, int mode) {
        if (displayContent.isDefaultDisplay) {
            Settings.Global.putInt(this.mService.mContext.getContentResolver(), "display_scaling_force", mode);
        }
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        SettingsProvider.SettingsEntry overrideSettings = this.mSettingsProvider.getOverrideSettings(displayInfo);
        overrideSettings.mForcedScalingMode = Integer.valueOf(mode);
        this.mSettingsProvider.updateOverrideSettings(displayInfo, overrideSettings);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setFixedToUserRotation(DisplayContent displayContent, int fixedToUserRotation) {
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        SettingsProvider.SettingsEntry overrideSettings = this.mSettingsProvider.getOverrideSettings(displayInfo);
        overrideSettings.mFixedToUserRotation = Integer.valueOf(fixedToUserRotation);
        this.mSettingsProvider.updateOverrideSettings(displayInfo, overrideSettings);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setIgnoreOrientationRequest(DisplayContent displayContent, boolean ignoreOrientationRequest) {
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        SettingsProvider.SettingsEntry overrideSettings = this.mSettingsProvider.getOverrideSettings(displayInfo);
        overrideSettings.mIgnoreOrientationRequest = Boolean.valueOf(ignoreOrientationRequest);
        this.mSettingsProvider.updateOverrideSettings(displayInfo, overrideSettings);
    }

    private int getWindowingModeLocked(SettingsProvider.SettingsEntry settings, DisplayContent dc) {
        int windowingMode = settings.mWindowingMode;
        int windowingMode2 = 1;
        if (windowingMode == 5 && !this.mService.mAtmService.mSupportsFreeformWindowManagement) {
            return 1;
        }
        if (windowingMode == 0) {
            if (this.mService.mAtmService.mSupportsFreeformWindowManagement && (this.mService.mIsPc || dc.forceDesktopMode())) {
                windowingMode2 = 5;
            }
            return windowingMode2;
        }
        return windowingMode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getWindowingModeLocked(DisplayContent dc) {
        DisplayInfo displayInfo = dc.getDisplayInfo();
        SettingsProvider.SettingsEntry settings = this.mSettingsProvider.getSettings(displayInfo);
        return getWindowingModeLocked(settings, dc);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setWindowingModeLocked(DisplayContent dc, int mode) {
        DisplayInfo displayInfo = dc.getDisplayInfo();
        SettingsProvider.SettingsEntry overrideSettings = this.mSettingsProvider.getOverrideSettings(displayInfo);
        overrideSettings.mWindowingMode = mode;
        dc.setWindowingMode(mode);
        this.mSettingsProvider.updateOverrideSettings(displayInfo, overrideSettings);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getRemoveContentModeLocked(DisplayContent dc) {
        DisplayInfo displayInfo = dc.getDisplayInfo();
        SettingsProvider.SettingsEntry settings = this.mSettingsProvider.getSettings(displayInfo);
        if (settings.mRemoveContentMode == 0) {
            if (dc.isPrivate()) {
                return 2;
            }
            return 1;
        }
        return settings.mRemoveContentMode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setRemoveContentModeLocked(DisplayContent dc, int mode) {
        DisplayInfo displayInfo = dc.getDisplayInfo();
        SettingsProvider.SettingsEntry overrideSettings = this.mSettingsProvider.getOverrideSettings(displayInfo);
        overrideSettings.mRemoveContentMode = mode;
        this.mSettingsProvider.updateOverrideSettings(displayInfo, overrideSettings);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldShowWithInsecureKeyguardLocked(DisplayContent dc) {
        DisplayInfo displayInfo = dc.getDisplayInfo();
        SettingsProvider.SettingsEntry settings = this.mSettingsProvider.getSettings(displayInfo);
        if (settings.mShouldShowWithInsecureKeyguard != null) {
            return settings.mShouldShowWithInsecureKeyguard.booleanValue();
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setShouldShowWithInsecureKeyguardLocked(DisplayContent dc, boolean shouldShow) {
        if (!dc.isPrivate() && shouldShow) {
            throw new IllegalArgumentException("Public display can't be allowed to show content when locked");
        }
        DisplayInfo displayInfo = dc.getDisplayInfo();
        SettingsProvider.SettingsEntry overrideSettings = this.mSettingsProvider.getOverrideSettings(displayInfo);
        overrideSettings.mShouldShowWithInsecureKeyguard = Boolean.valueOf(shouldShow);
        this.mSettingsProvider.updateOverrideSettings(displayInfo, overrideSettings);
    }

    void setDontMoveToTop(DisplayContent dc, boolean dontMoveToTop) {
        DisplayInfo displayInfo = dc.getDisplayInfo();
        SettingsProvider.SettingsEntry overrideSettings = this.mSettingsProvider.getSettings(displayInfo);
        overrideSettings.mDontMoveToTop = Boolean.valueOf(dontMoveToTop);
        this.mSettingsProvider.updateOverrideSettings(displayInfo, overrideSettings);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldShowSystemDecorsLocked(DisplayContent dc) {
        if (dc.getDisplayId() == 0) {
            return true;
        }
        DisplayInfo displayInfo = dc.getDisplayInfo();
        SettingsProvider.SettingsEntry settings = this.mSettingsProvider.getSettings(displayInfo);
        if (settings.mShouldShowSystemDecors != null) {
            return settings.mShouldShowSystemDecors.booleanValue();
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setShouldShowSystemDecorsLocked(DisplayContent dc, boolean shouldShow) {
        DisplayInfo displayInfo = dc.getDisplayInfo();
        SettingsProvider.SettingsEntry overrideSettings = this.mSettingsProvider.getOverrideSettings(displayInfo);
        overrideSettings.mShouldShowSystemDecors = Boolean.valueOf(shouldShow);
        this.mSettingsProvider.updateOverrideSettings(displayInfo, overrideSettings);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getImePolicyLocked(DisplayContent dc) {
        if (dc.getDisplayId() == 0) {
            return 0;
        }
        DisplayInfo displayInfo = dc.getDisplayInfo();
        SettingsProvider.SettingsEntry settings = this.mSettingsProvider.getSettings(displayInfo);
        if (settings.mImePolicy != null) {
            return settings.mImePolicy.intValue();
        }
        return 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDisplayImePolicy(DisplayContent dc, int imePolicy) {
        DisplayInfo displayInfo = dc.getDisplayInfo();
        SettingsProvider.SettingsEntry overrideSettings = this.mSettingsProvider.getOverrideSettings(displayInfo);
        overrideSettings.mImePolicy = Integer.valueOf(imePolicy);
        this.mSettingsProvider.updateOverrideSettings(displayInfo, overrideSettings);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void applySettingsToDisplayLocked(DisplayContent dc) {
        applySettingsToDisplayLocked(dc, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void applySettingsToDisplayLocked(DisplayContent dc, boolean includeRotationSettings) {
        DisplayInfo displayInfo = dc.getDisplayInfo();
        SettingsProvider.SettingsEntry settings = this.mSettingsProvider.getSettings(displayInfo);
        int windowingMode = getWindowingModeLocked(settings, dc);
        dc.setWindowingMode(windowingMode);
        int userRotationMode = settings.mUserRotationMode != null ? settings.mUserRotationMode.intValue() : 0;
        int userRotation = settings.mUserRotation != null ? settings.mUserRotation.intValue() : 0;
        int mFixedToUserRotation = settings.mFixedToUserRotation != null ? settings.mFixedToUserRotation.intValue() : 0;
        dc.getDisplayRotation().restoreSettings(userRotationMode, userRotation, mFixedToUserRotation);
        boolean hasDensityOverride = settings.mForcedDensity != 0;
        boolean hasSizeOverride = (settings.mForcedWidth == 0 || settings.mForcedHeight == 0) ? false : true;
        dc.mIsDensityForced = hasDensityOverride;
        dc.mIsSizeForced = hasSizeOverride;
        boolean ignoreDisplayCutout = settings.mIgnoreDisplayCutout != null ? settings.mIgnoreDisplayCutout.booleanValue() : false;
        dc.mIgnoreDisplayCutout = ignoreDisplayCutout;
        int width = hasSizeOverride ? settings.mForcedWidth : dc.mInitialDisplayWidth;
        int height = hasSizeOverride ? settings.mForcedHeight : dc.mInitialDisplayHeight;
        int density = hasDensityOverride ? settings.mForcedDensity : dc.mInitialDisplayDensity;
        dc.updateBaseDisplayMetrics(width, height, density, dc.mBaseDisplayPhysicalXDpi, dc.mBaseDisplayPhysicalYDpi);
        int forcedScalingMode = settings.mForcedScalingMode != null ? settings.mForcedScalingMode.intValue() : 0;
        dc.mDisplayScalingDisabled = forcedScalingMode == 1;
        boolean dontMoveToTop = settings.mDontMoveToTop != null ? settings.mDontMoveToTop.booleanValue() : false;
        dc.mDontMoveToTop = dontMoveToTop;
        if (includeRotationSettings) {
            applyRotationSettingsToDisplayLocked(dc);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void applyRotationSettingsToDisplayLocked(DisplayContent dc) {
        DisplayInfo displayInfo = dc.getDisplayInfo();
        SettingsProvider.SettingsEntry settings = this.mSettingsProvider.getSettings(displayInfo);
        boolean ignoreOrientationRequest = settings.mIgnoreOrientationRequest != null ? settings.mIgnoreOrientationRequest.booleanValue() : false;
        dc.setIgnoreOrientationRequest(ignoreOrientationRequest);
        dc.getDisplayRotation().resetAllowAllRotations();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateSettingsForDisplay(DisplayContent dc) {
        if (dc.getWindowingMode() != getWindowingModeLocked(dc)) {
            dc.setWindowingMode(getWindowingModeLocked(dc));
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface SettingsProvider {
        SettingsEntry getOverrideSettings(DisplayInfo displayInfo);

        SettingsEntry getSettings(DisplayInfo displayInfo);

        void updateOverrideSettings(DisplayInfo displayInfo, SettingsEntry settingsEntry);

        /* loaded from: classes2.dex */
        public static class SettingsEntry {
            Boolean mDontMoveToTop;
            Integer mFixedToUserRotation;
            int mForcedDensity;
            int mForcedHeight;
            Integer mForcedScalingMode;
            int mForcedWidth;
            Boolean mIgnoreDisplayCutout;
            Boolean mIgnoreOrientationRequest;
            Integer mImePolicy;
            Boolean mShouldShowSystemDecors;
            Boolean mShouldShowWithInsecureKeyguard;
            Integer mUserRotation;
            Integer mUserRotationMode;
            int mWindowingMode = 0;
            int mRemoveContentMode = 0;

            /* JADX INFO: Access modifiers changed from: package-private */
            public SettingsEntry() {
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            public SettingsEntry(SettingsEntry copyFrom) {
                setTo(copyFrom);
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            public boolean setTo(SettingsEntry other) {
                boolean changed = false;
                int i = other.mWindowingMode;
                if (i != this.mWindowingMode) {
                    this.mWindowingMode = i;
                    changed = true;
                }
                if (!Objects.equals(other.mUserRotationMode, this.mUserRotationMode)) {
                    this.mUserRotationMode = other.mUserRotationMode;
                    changed = true;
                }
                if (!Objects.equals(other.mUserRotation, this.mUserRotation)) {
                    this.mUserRotation = other.mUserRotation;
                    changed = true;
                }
                int i2 = other.mForcedWidth;
                if (i2 != this.mForcedWidth) {
                    this.mForcedWidth = i2;
                    changed = true;
                }
                int i3 = other.mForcedHeight;
                if (i3 != this.mForcedHeight) {
                    this.mForcedHeight = i3;
                    changed = true;
                }
                int i4 = other.mForcedDensity;
                if (i4 != this.mForcedDensity) {
                    this.mForcedDensity = i4;
                    changed = true;
                }
                if (!Objects.equals(other.mForcedScalingMode, this.mForcedScalingMode)) {
                    this.mForcedScalingMode = other.mForcedScalingMode;
                    changed = true;
                }
                int i5 = other.mRemoveContentMode;
                if (i5 != this.mRemoveContentMode) {
                    this.mRemoveContentMode = i5;
                    changed = true;
                }
                Boolean bool = other.mShouldShowWithInsecureKeyguard;
                if (bool != this.mShouldShowWithInsecureKeyguard) {
                    this.mShouldShowWithInsecureKeyguard = bool;
                    changed = true;
                }
                Boolean bool2 = other.mShouldShowSystemDecors;
                if (bool2 != this.mShouldShowSystemDecors) {
                    this.mShouldShowSystemDecors = bool2;
                    changed = true;
                }
                if (!Objects.equals(other.mImePolicy, this.mImePolicy)) {
                    this.mImePolicy = other.mImePolicy;
                    changed = true;
                }
                if (!Objects.equals(other.mFixedToUserRotation, this.mFixedToUserRotation)) {
                    this.mFixedToUserRotation = other.mFixedToUserRotation;
                    changed = true;
                }
                Boolean bool3 = other.mIgnoreOrientationRequest;
                if (bool3 != this.mIgnoreOrientationRequest) {
                    this.mIgnoreOrientationRequest = bool3;
                    changed = true;
                }
                Boolean bool4 = other.mIgnoreDisplayCutout;
                if (bool4 != this.mIgnoreDisplayCutout) {
                    this.mIgnoreDisplayCutout = bool4;
                    changed = true;
                }
                Boolean bool5 = other.mDontMoveToTop;
                if (bool5 != this.mDontMoveToTop) {
                    this.mDontMoveToTop = bool5;
                    return true;
                }
                return changed;
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            public boolean updateFrom(SettingsEntry delta) {
                boolean changed = false;
                int i = delta.mWindowingMode;
                if (i != 0 && i != this.mWindowingMode) {
                    this.mWindowingMode = i;
                    changed = true;
                }
                Integer num = delta.mUserRotationMode;
                if (num != null && !Objects.equals(num, this.mUserRotationMode)) {
                    this.mUserRotationMode = delta.mUserRotationMode;
                    changed = true;
                }
                Integer num2 = delta.mUserRotation;
                if (num2 != null && !Objects.equals(num2, this.mUserRotation)) {
                    this.mUserRotation = delta.mUserRotation;
                    changed = true;
                }
                int i2 = delta.mForcedWidth;
                if (i2 != 0 && i2 != this.mForcedWidth) {
                    this.mForcedWidth = i2;
                    changed = true;
                }
                int i3 = delta.mForcedHeight;
                if (i3 != 0 && i3 != this.mForcedHeight) {
                    this.mForcedHeight = i3;
                    changed = true;
                }
                int i4 = delta.mForcedDensity;
                if (i4 != 0 && i4 != this.mForcedDensity) {
                    this.mForcedDensity = i4;
                    changed = true;
                }
                Integer num3 = delta.mForcedScalingMode;
                if (num3 != null && !Objects.equals(num3, this.mForcedScalingMode)) {
                    this.mForcedScalingMode = delta.mForcedScalingMode;
                    changed = true;
                }
                int i5 = delta.mRemoveContentMode;
                if (i5 != 0 && i5 != this.mRemoveContentMode) {
                    this.mRemoveContentMode = i5;
                    changed = true;
                }
                Boolean bool = delta.mShouldShowWithInsecureKeyguard;
                if (bool != null && bool != this.mShouldShowWithInsecureKeyguard) {
                    this.mShouldShowWithInsecureKeyguard = bool;
                    changed = true;
                }
                Boolean bool2 = delta.mShouldShowSystemDecors;
                if (bool2 != null && bool2 != this.mShouldShowSystemDecors) {
                    this.mShouldShowSystemDecors = bool2;
                    changed = true;
                }
                Integer num4 = delta.mImePolicy;
                if (num4 != null && !Objects.equals(num4, this.mImePolicy)) {
                    this.mImePolicy = delta.mImePolicy;
                    changed = true;
                }
                Integer num5 = delta.mFixedToUserRotation;
                if (num5 != null && !Objects.equals(num5, this.mFixedToUserRotation)) {
                    this.mFixedToUserRotation = delta.mFixedToUserRotation;
                    changed = true;
                }
                Boolean bool3 = delta.mIgnoreOrientationRequest;
                if (bool3 != null && bool3 != this.mIgnoreOrientationRequest) {
                    this.mIgnoreOrientationRequest = bool3;
                    changed = true;
                }
                Boolean bool4 = delta.mIgnoreDisplayCutout;
                if (bool4 != null && bool4 != this.mIgnoreDisplayCutout) {
                    this.mIgnoreDisplayCutout = bool4;
                    changed = true;
                }
                Boolean bool5 = delta.mDontMoveToTop;
                if (bool5 != null && bool5 != this.mDontMoveToTop) {
                    this.mDontMoveToTop = bool5;
                    return true;
                }
                return changed;
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            public boolean isEmpty() {
                return this.mWindowingMode == 0 && this.mUserRotationMode == null && this.mUserRotation == null && this.mForcedWidth == 0 && this.mForcedHeight == 0 && this.mForcedDensity == 0 && this.mForcedScalingMode == null && this.mRemoveContentMode == 0 && this.mShouldShowWithInsecureKeyguard == null && this.mShouldShowSystemDecors == null && this.mImePolicy == null && this.mFixedToUserRotation == null && this.mIgnoreOrientationRequest == null && this.mIgnoreDisplayCutout == null && this.mDontMoveToTop == null;
            }

            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                SettingsEntry that = (SettingsEntry) o;
                if (this.mWindowingMode == that.mWindowingMode && this.mForcedWidth == that.mForcedWidth && this.mForcedHeight == that.mForcedHeight && this.mForcedDensity == that.mForcedDensity && this.mRemoveContentMode == that.mRemoveContentMode && Objects.equals(this.mUserRotationMode, that.mUserRotationMode) && Objects.equals(this.mUserRotation, that.mUserRotation) && Objects.equals(this.mForcedScalingMode, that.mForcedScalingMode) && Objects.equals(this.mShouldShowWithInsecureKeyguard, that.mShouldShowWithInsecureKeyguard) && Objects.equals(this.mShouldShowSystemDecors, that.mShouldShowSystemDecors) && Objects.equals(this.mImePolicy, that.mImePolicy) && Objects.equals(this.mFixedToUserRotation, that.mFixedToUserRotation) && Objects.equals(this.mIgnoreOrientationRequest, that.mIgnoreOrientationRequest) && Objects.equals(this.mIgnoreDisplayCutout, that.mIgnoreDisplayCutout) && Objects.equals(this.mDontMoveToTop, that.mDontMoveToTop)) {
                    return true;
                }
                return false;
            }

            public int hashCode() {
                return Objects.hash(Integer.valueOf(this.mWindowingMode), this.mUserRotationMode, this.mUserRotation, Integer.valueOf(this.mForcedWidth), Integer.valueOf(this.mForcedHeight), Integer.valueOf(this.mForcedDensity), this.mForcedScalingMode, Integer.valueOf(this.mRemoveContentMode), this.mShouldShowWithInsecureKeyguard, this.mShouldShowSystemDecors, this.mImePolicy, this.mFixedToUserRotation, this.mIgnoreOrientationRequest, this.mIgnoreDisplayCutout, this.mDontMoveToTop);
            }

            public String toString() {
                return "SettingsEntry{mWindowingMode=" + this.mWindowingMode + ", mUserRotationMode=" + this.mUserRotationMode + ", mUserRotation=" + this.mUserRotation + ", mForcedWidth=" + this.mForcedWidth + ", mForcedHeight=" + this.mForcedHeight + ", mForcedDensity=" + this.mForcedDensity + ", mForcedScalingMode=" + this.mForcedScalingMode + ", mRemoveContentMode=" + this.mRemoveContentMode + ", mShouldShowWithInsecureKeyguard=" + this.mShouldShowWithInsecureKeyguard + ", mShouldShowSystemDecors=" + this.mShouldShowSystemDecors + ", mShouldShowIme=" + this.mImePolicy + ", mFixedToUserRotation=" + this.mFixedToUserRotation + ", mIgnoreOrientationRequest=" + this.mIgnoreOrientationRequest + ", mIgnoreDisplayCutout=" + this.mIgnoreDisplayCutout + ", mDontMoveToTop=" + this.mDontMoveToTop + '}';
            }
        }
    }
}
