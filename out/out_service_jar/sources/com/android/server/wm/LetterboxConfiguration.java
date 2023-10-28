package com.android.server.wm;

import android.content.Context;
import android.graphics.Color;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/* loaded from: classes2.dex */
final class LetterboxConfiguration {
    static final int LETTERBOX_BACKGROUND_APP_COLOR_BACKGROUND = 1;
    static final int LETTERBOX_BACKGROUND_APP_COLOR_BACKGROUND_FLOATING = 2;
    static final int LETTERBOX_BACKGROUND_SOLID_COLOR = 0;
    static final int LETTERBOX_BACKGROUND_WALLPAPER = 3;
    static final int LETTERBOX_REACHABILITY_POSITION_CENTER = 1;
    static final int LETTERBOX_REACHABILITY_POSITION_LEFT = 0;
    static final int LETTERBOX_REACHABILITY_POSITION_RIGHT = 2;
    static final float MIN_FIXED_ORIENTATION_LETTERBOX_ASPECT_RATIO = 1.0f;
    final Context mContext;
    private int mDefaultPositionForReachability;
    private float mFixedOrientationLetterboxAspectRatio;
    private boolean mIsEducationEnabled;
    private boolean mIsReachabilityEnabled;
    private int mLetterboxActivityCornersRadius;
    private Color mLetterboxBackgroundColorOverride;
    private Integer mLetterboxBackgroundColorResourceIdOverride;
    private int mLetterboxBackgroundType;
    private int mLetterboxBackgroundWallpaperBlurRadius;
    private float mLetterboxBackgroundWallpaperDarkScrimAlpha;
    private float mLetterboxHorizontalPositionMultiplier;
    private volatile int mLetterboxPositionForReachability;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface LetterboxBackgroundType {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface LetterboxReachabilityPosition {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public LetterboxConfiguration(Context systemUiContext) {
        this.mContext = systemUiContext;
        this.mFixedOrientationLetterboxAspectRatio = systemUiContext.getResources().getFloat(17105077);
        this.mLetterboxActivityCornersRadius = systemUiContext.getResources().getInteger(17694848);
        this.mLetterboxBackgroundType = readLetterboxBackgroundTypeFromConfig(systemUiContext);
        this.mLetterboxBackgroundWallpaperBlurRadius = systemUiContext.getResources().getDimensionPixelSize(17105083);
        this.mLetterboxBackgroundWallpaperDarkScrimAlpha = systemUiContext.getResources().getFloat(17105082);
        this.mLetterboxHorizontalPositionMultiplier = systemUiContext.getResources().getFloat(17105084);
        this.mIsReachabilityEnabled = systemUiContext.getResources().getBoolean(17891692);
        int readLetterboxReachabilityPositionFromConfig = readLetterboxReachabilityPositionFromConfig(systemUiContext);
        this.mDefaultPositionForReachability = readLetterboxReachabilityPositionFromConfig;
        this.mLetterboxPositionForReachability = readLetterboxReachabilityPositionFromConfig;
        this.mIsEducationEnabled = systemUiContext.getResources().getBoolean(17891691);
    }

    void setFixedOrientationLetterboxAspectRatio(float aspectRatio) {
        this.mFixedOrientationLetterboxAspectRatio = aspectRatio;
    }

    void resetFixedOrientationLetterboxAspectRatio() {
        this.mFixedOrientationLetterboxAspectRatio = this.mContext.getResources().getFloat(17105077);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float getFixedOrientationLetterboxAspectRatio() {
        return this.mFixedOrientationLetterboxAspectRatio;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isLetterboxActivityCornersRounded() {
        return getLetterboxActivityCornersRadius() != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getLetterboxActivityCornersRadius() {
        return this.mLetterboxActivityCornersRadius;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Color getLetterboxBackgroundColor() {
        int colorId;
        Color color = this.mLetterboxBackgroundColorOverride;
        if (color != null) {
            return color;
        }
        Integer num = this.mLetterboxBackgroundColorResourceIdOverride;
        if (num != null) {
            colorId = num.intValue();
        } else {
            colorId = 17170815;
        }
        return Color.valueOf(this.mContext.getResources().getColor(colorId));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getLetterboxBackgroundType() {
        return this.mLetterboxBackgroundType;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String letterboxBackgroundTypeToString(int backgroundType) {
        switch (backgroundType) {
            case 0:
                return "LETTERBOX_BACKGROUND_SOLID_COLOR";
            case 1:
                return "LETTERBOX_BACKGROUND_APP_COLOR_BACKGROUND";
            case 2:
                return "LETTERBOX_BACKGROUND_APP_COLOR_BACKGROUND_FLOATING";
            case 3:
                return "LETTERBOX_BACKGROUND_WALLPAPER";
            default:
                return "unknown=" + backgroundType;
        }
    }

    private static int readLetterboxBackgroundTypeFromConfig(Context context) {
        int backgroundType = context.getResources().getInteger(17694849);
        if (backgroundType == 0 || backgroundType == 1 || backgroundType == 2 || backgroundType == 3) {
            return backgroundType;
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float getLetterboxBackgroundWallpaperDarkScrimAlpha() {
        return this.mLetterboxBackgroundWallpaperDarkScrimAlpha;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getLetterboxBackgroundWallpaperBlurRadius() {
        return this.mLetterboxBackgroundWallpaperBlurRadius;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float getLetterboxHorizontalPositionMultiplier() {
        float f = this.mLetterboxHorizontalPositionMultiplier;
        if (f < 0.0f || f > 1.0f) {
            return 0.5f;
        }
        return f;
    }

    void setLetterboxHorizontalPositionMultiplier(float multiplier) {
        this.mLetterboxHorizontalPositionMultiplier = multiplier;
    }

    void resetLetterboxHorizontalPositionMultiplier() {
        this.mLetterboxHorizontalPositionMultiplier = this.mContext.getResources().getFloat(17105084);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getIsReachabilityEnabled() {
        return this.mIsReachabilityEnabled;
    }

    void setIsReachabilityEnabled(boolean enabled) {
        this.mIsReachabilityEnabled = enabled;
    }

    void resetIsReachabilityEnabled() {
        this.mIsReachabilityEnabled = this.mContext.getResources().getBoolean(17891692);
    }

    int getDefaultPositionForReachability() {
        return this.mDefaultPositionForReachability;
    }

    private static int readLetterboxReachabilityPositionFromConfig(Context context) {
        int position = context.getResources().getInteger(17694850);
        if (position == 0 || position == 1 || position == 2) {
            return position;
        }
        return 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float getHorizontalMultiplierForReachability() {
        switch (this.mLetterboxPositionForReachability) {
            case 0:
                return 0.0f;
            case 1:
                return 0.5f;
            case 2:
                return 1.0f;
            default:
                throw new AssertionError("Unexpected letterbox position type: " + this.mLetterboxPositionForReachability);
        }
    }

    static String letterboxReachabilityPositionToString(int position) {
        switch (position) {
            case 0:
                return "LETTERBOX_REACHABILITY_POSITION_LEFT";
            case 1:
                return "LETTERBOX_REACHABILITY_POSITION_CENTER";
            case 2:
                return "LETTERBOX_REACHABILITY_POSITION_RIGHT";
            default:
                throw new AssertionError("Unexpected letterbox position type: " + position);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void movePositionForReachabilityToNextRightStop() {
        this.mLetterboxPositionForReachability = Math.min(this.mLetterboxPositionForReachability + 1, 2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void movePositionForReachabilityToNextLeftStop() {
        this.mLetterboxPositionForReachability = Math.max(this.mLetterboxPositionForReachability - 1, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getIsEducationEnabled() {
        return this.mIsEducationEnabled;
    }

    void setIsEducationEnabled(boolean enabled) {
        this.mIsEducationEnabled = enabled;
    }
}
