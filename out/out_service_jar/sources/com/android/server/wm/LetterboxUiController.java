package com.android.server.wm;

import android.app.ActivityManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.os.Build;
import android.util.Slog;
import android.view.InsetsSource;
import android.view.InsetsState;
import android.view.RoundedCorner;
import android.view.SurfaceControl;
import android.view.WindowManager;
import java.io.PrintWriter;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class LetterboxUiController {
    private static final String TAG = "ActivityTaskManager";
    private final ActivityRecord mActivityRecord;
    private Letterbox mLetterbox;
    private final LetterboxConfiguration mLetterboxConfiguration;
    private boolean mShowWallpaperForLetterboxBackground;
    private final Point mTmpPoint = new Point();
    private float mExpandedTaskBarHeight = getResources().getDimensionPixelSize(17105574);

    /* JADX INFO: Access modifiers changed from: package-private */
    public LetterboxUiController(WindowManagerService wmService, ActivityRecord activityRecord) {
        this.mLetterboxConfiguration = wmService.mLetterboxConfiguration;
        this.mActivityRecord = activityRecord;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void destroy() {
        Letterbox letterbox = this.mLetterbox;
        if (letterbox != null) {
            letterbox.destroy();
            this.mLetterbox = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onMovedToDisplay(int displayId) {
        Letterbox letterbox = this.mLetterbox;
        if (letterbox != null) {
            letterbox.onMovedToDisplay(displayId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasWallpaperBackgroudForLetterbox() {
        return this.mShowWallpaperForLetterboxBackground;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Rect getLetterboxInsets() {
        Letterbox letterbox = this.mLetterbox;
        if (letterbox != null) {
            return letterbox.getInsets();
        }
        return new Rect();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getLetterboxInnerBounds(Rect outBounds) {
        Letterbox letterbox = this.mLetterbox;
        if (letterbox != null) {
            outBounds.set(letterbox.getInnerFrame());
        } else {
            outBounds.setEmpty();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFullyTransparentBarAllowed(Rect rect) {
        Letterbox letterbox = this.mLetterbox;
        return letterbox == null || letterbox.notIntersectsOrFullyContains(rect);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateLetterboxSurface(WindowState winHint) {
        WindowState w = this.mActivityRecord.findMainWindow();
        if (w != winHint && winHint != null && w != null) {
            return;
        }
        layoutLetterbox(winHint);
        Letterbox letterbox = this.mLetterbox;
        if (letterbox != null && letterbox.needsApplySurfaceChanges()) {
            this.mLetterbox.applySurfaceChanges(this.mActivityRecord.getSyncTransaction());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void layoutLetterbox(WindowState winHint) {
        Rect spaceToFill;
        WindowState w = this.mActivityRecord.findMainWindow();
        if (w != null) {
            if (winHint != null && w != winHint) {
                return;
            }
            updateRoundedCorners(w);
            WindowState nonStartingAppW = this.mActivityRecord.findMainWindow(false);
            if (nonStartingAppW != null && nonStartingAppW != w) {
                updateRoundedCorners(nonStartingAppW);
            }
            updateWallpaperForLetterbox(w);
            if (shouldShowLetterboxUi(w)) {
                if (this.mLetterbox == null) {
                    Letterbox letterbox = new Letterbox(new Supplier() { // from class: com.android.server.wm.LetterboxUiController$$ExternalSyntheticLambda0
                        @Override // java.util.function.Supplier
                        public final Object get() {
                            return LetterboxUiController.this.m8109xd2579a04();
                        }
                    }, this.mActivityRecord.mWmService.mTransactionFactory, new Supplier() { // from class: com.android.server.wm.LetterboxUiController$$ExternalSyntheticLambda1
                        @Override // java.util.function.Supplier
                        public final Object get() {
                            boolean shouldLetterboxHaveRoundedCorners;
                            shouldLetterboxHaveRoundedCorners = LetterboxUiController.this.shouldLetterboxHaveRoundedCorners();
                            return Boolean.valueOf(shouldLetterboxHaveRoundedCorners);
                        }
                    }, new Supplier() { // from class: com.android.server.wm.LetterboxUiController$$ExternalSyntheticLambda2
                        @Override // java.util.function.Supplier
                        public final Object get() {
                            Color letterboxBackgroundColor;
                            letterboxBackgroundColor = LetterboxUiController.this.getLetterboxBackgroundColor();
                            return letterboxBackgroundColor;
                        }
                    }, new Supplier() { // from class: com.android.server.wm.LetterboxUiController$$ExternalSyntheticLambda3
                        @Override // java.util.function.Supplier
                        public final Object get() {
                            return Boolean.valueOf(LetterboxUiController.this.hasWallpaperBackgroudForLetterbox());
                        }
                    }, new Supplier() { // from class: com.android.server.wm.LetterboxUiController$$ExternalSyntheticLambda4
                        @Override // java.util.function.Supplier
                        public final Object get() {
                            int letterboxWallpaperBlurRadius;
                            letterboxWallpaperBlurRadius = LetterboxUiController.this.getLetterboxWallpaperBlurRadius();
                            return Integer.valueOf(letterboxWallpaperBlurRadius);
                        }
                    }, new Supplier() { // from class: com.android.server.wm.LetterboxUiController$$ExternalSyntheticLambda5
                        @Override // java.util.function.Supplier
                        public final Object get() {
                            float letterboxWallpaperDarkScrimAlpha;
                            letterboxWallpaperDarkScrimAlpha = LetterboxUiController.this.getLetterboxWallpaperDarkScrimAlpha();
                            return Float.valueOf(letterboxWallpaperDarkScrimAlpha);
                        }
                    }, new IntConsumer() { // from class: com.android.server.wm.LetterboxUiController$$ExternalSyntheticLambda6
                        @Override // java.util.function.IntConsumer
                        public final void accept(int i) {
                            LetterboxUiController.this.handleDoubleTap(i);
                        }
                    });
                    this.mLetterbox = letterbox;
                    letterbox.attachInput(w);
                    if (!Build.IS_USER) {
                        String prefix = (("letterboxBackgroundColor=" + Integer.toHexString(getLetterboxBackgroundColor().toArgb())) + ", letterboxBackgroundType=" + LetterboxConfiguration.letterboxBackgroundTypeToString(this.mLetterboxConfiguration.getLetterboxBackgroundType())) + ", letterboxCornerRadius=" + getRoundedCorners(w.getInsetsState());
                        if (this.mLetterboxConfiguration.getLetterboxBackgroundType() == 3) {
                            prefix = ((prefix + ", isLetterboxWallpaperBlurSupported=" + isLetterboxWallpaperBlurSupported()) + ", letterboxBackgroundWallpaperDarkScrimAlpha=" + getLetterboxWallpaperDarkScrimAlpha()) + ", letterboxBackgroundWallpaperBlurRadius=" + getLetterboxWallpaperBlurRadius();
                        }
                        Slog.d(TAG, "Show LetterBox: " + (((prefix + ", isReachabilityEnabled=" + isReachabilityEnabled()) + ", letterboxHorizontalPositionMultiplier=" + getHorizontalPositionMultiplier(this.mActivityRecord.getParent().getConfiguration())) + ", fixedOrientationLetterboxAspectRatio=" + getFixedOrientationLetterboxAspectRatio(this.mActivityRecord.getParent().getConfiguration())));
                    }
                }
                this.mActivityRecord.getPosition(this.mTmpPoint);
                Rect transformedBounds = this.mActivityRecord.getFixedRotationTransformDisplayBounds();
                if (transformedBounds != null) {
                    spaceToFill = transformedBounds;
                } else if (this.mActivityRecord.inMultiWindowMode()) {
                    spaceToFill = this.mActivityRecord.getTask().getBounds();
                } else {
                    spaceToFill = this.mActivityRecord.getRootTask().getParent().getBounds();
                }
                Rect innerFrame = this.mActivityRecord.mWindowIsFloating ? this.mActivityRecord.getBounds() : w.getFrame();
                this.mLetterbox.layout(spaceToFill, innerFrame, this.mTmpPoint);
                return;
            }
            Letterbox letterbox2 = this.mLetterbox;
            if (letterbox2 != null) {
                letterbox2.hide();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$layoutLetterbox$0$com-android-server-wm-LetterboxUiController  reason: not valid java name */
    public /* synthetic */ SurfaceControl.Builder m8109xd2579a04() {
        return this.mActivityRecord.makeChildSurface(null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean shouldLetterboxHaveRoundedCorners() {
        return this.mLetterboxConfiguration.isLetterboxActivityCornersRounded() && this.mActivityRecord.fillsParent();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float getHorizontalPositionMultiplier(Configuration parentConfiguration) {
        if (isReachabilityEnabled(parentConfiguration)) {
            return this.mLetterboxConfiguration.getHorizontalMultiplierForReachability();
        }
        return this.mLetterboxConfiguration.getLetterboxHorizontalPositionMultiplier();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float getFixedOrientationLetterboxAspectRatio(Configuration parentConfiguration) {
        if (!isReachabilityEnabled(parentConfiguration)) {
            return this.mLetterboxConfiguration.getFixedOrientationLetterboxAspectRatio();
        }
        int dividerWindowWidth = getResources().getDimensionPixelSize(17105204);
        int dividerInsets = getResources().getDimensionPixelSize(17105203);
        int dividerSize = dividerWindowWidth - (dividerInsets * 2);
        Rect bounds = new Rect(parentConfiguration.windowConfiguration.getAppBounds());
        bounds.inset(dividerSize, 0);
        bounds.right = bounds.centerX();
        return ActivityRecord.computeAspectRatio(bounds);
    }

    Resources getResources() {
        return this.mActivityRecord.mWmService.mContext.getResources();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleDoubleTap(int x) {
        if (!isReachabilityEnabled() || this.mActivityRecord.isInTransition()) {
            return;
        }
        if (this.mLetterbox.getInnerFrame().left <= x && this.mLetterbox.getInnerFrame().right >= x) {
            return;
        }
        if (this.mLetterbox.getInnerFrame().left > x) {
            this.mLetterboxConfiguration.movePositionForReachabilityToNextLeftStop();
        } else if (this.mLetterbox.getInnerFrame().right < x) {
            this.mLetterboxConfiguration.movePositionForReachabilityToNextRightStop();
        }
        this.mActivityRecord.recomputeConfiguration();
    }

    private boolean isReachabilityEnabled(Configuration parentConfiguration) {
        return this.mLetterboxConfiguration.getIsReachabilityEnabled() && parentConfiguration.windowConfiguration.getWindowingMode() == 1 && parentConfiguration.orientation == 2 && this.mActivityRecord.getRequestedConfigurationOrientation() == 1;
    }

    private boolean isReachabilityEnabled() {
        return isReachabilityEnabled(this.mActivityRecord.getParent().getConfiguration());
    }

    boolean shouldShowLetterboxUi(WindowState mainWindow) {
        if (isSurfaceReadyAndVisible(mainWindow) && mainWindow.areAppWindowBoundsLetterboxed()) {
            ActivityTaskManagerService activityTaskManagerService = this.mActivityRecord.mAtmService;
            if (!ActivityTaskManagerService.AJUST_LAUNCH_TIME && (mainWindow.mAttrs.flags & 1048576) == 0) {
                return true;
            }
        }
        return false;
    }

    boolean isSurfaceReadyAndVisible(WindowState mainWindow) {
        boolean surfaceReady = mainWindow.isDrawn() || mainWindow.isDragResizeChanged();
        if (surfaceReady) {
            return this.mActivityRecord.isVisible() || this.mActivityRecord.isVisibleRequested();
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Color getLetterboxBackgroundColor() {
        WindowState w = this.mActivityRecord.findMainWindow();
        if (w == null || w.isLetterboxedForDisplayCutout()) {
            return Color.valueOf((int) AudioFormat.MAIN_MASK);
        }
        int letterboxBackgroundType = this.mLetterboxConfiguration.getLetterboxBackgroundType();
        ActivityManager.TaskDescription taskDescription = this.mActivityRecord.taskDescription;
        switch (letterboxBackgroundType) {
            case 0:
                return this.mLetterboxConfiguration.getLetterboxBackgroundColor();
            case 1:
                if (taskDescription != null && taskDescription.getBackgroundColor() != 0) {
                    return Color.valueOf(taskDescription.getBackgroundColor());
                }
                break;
            case 2:
                if (taskDescription != null && taskDescription.getBackgroundColorFloating() != 0) {
                    return Color.valueOf(taskDescription.getBackgroundColorFloating());
                }
                break;
            case 3:
                if (hasWallpaperBackgroudForLetterbox()) {
                    return Color.valueOf((int) AudioFormat.MAIN_MASK);
                }
                Slog.w(TAG, "Wallpaper option is selected for letterbox background but blur is not supported by a device or not supported in the current window configuration or both alpha scrim and blur radius aren't provided so using solid color background");
                break;
            default:
                throw new AssertionError("Unexpected letterbox background type: " + letterboxBackgroundType);
        }
        return this.mLetterboxConfiguration.getLetterboxBackgroundColor();
    }

    private void updateRoundedCorners(WindowState mainWindow) {
        SurfaceControl windowSurface = mainWindow.getClientViewRootSurface();
        if (windowSurface != null && windowSurface.isValid()) {
            SurfaceControl.Transaction transaction = this.mActivityRecord.getSyncTransaction();
            InsetsState insetsState = mainWindow.getInsetsState();
            InsetsSource taskbarInsetsSource = insetsState.peekSource(21);
            if (!isLetterboxedNotForDisplayCutout(mainWindow) || !this.mLetterboxConfiguration.isLetterboxActivityCornersRounded() || taskbarInsetsSource == null) {
                transaction.setWindowCrop(windowSurface, null).setCornerRadius(windowSurface, 0.0f);
                return;
            }
            Rect cropBounds = null;
            if (taskbarInsetsSource.getFrame().height() >= this.mExpandedTaskBarHeight && taskbarInsetsSource.isVisible()) {
                cropBounds = new Rect(this.mActivityRecord.getBounds());
                cropBounds.offsetTo(0, 0);
                cropBounds.bottom = Math.min(cropBounds.bottom, taskbarInsetsSource.getFrame().top);
                if (this.mActivityRecord.inSizeCompatMode() && this.mActivityRecord.getSizeCompatScale() < 1.0f) {
                    cropBounds.scale(1.0f / this.mActivityRecord.getSizeCompatScale());
                }
            }
            transaction.setWindowCrop(windowSurface, cropBounds).setCornerRadius(windowSurface, getRoundedCorners(insetsState));
        }
    }

    private int getRoundedCorners(InsetsState insetsState) {
        if (this.mLetterboxConfiguration.getLetterboxActivityCornersRadius() >= 0) {
            return this.mLetterboxConfiguration.getLetterboxActivityCornersRadius();
        }
        return Math.min(getInsetsStateCornerRadius(insetsState, 3), getInsetsStateCornerRadius(insetsState, 2));
    }

    private int getInsetsStateCornerRadius(InsetsState insetsState, int position) {
        RoundedCorner corner = insetsState.getRoundedCorners().getRoundedCorner(position);
        if (corner == null) {
            return 0;
        }
        return corner.getRadius();
    }

    private boolean isLetterboxedNotForDisplayCutout(WindowState mainWindow) {
        return shouldShowLetterboxUi(mainWindow) && !mainWindow.isLetterboxedForDisplayCutout();
    }

    private void updateWallpaperForLetterbox(WindowState mainWindow) {
        int letterboxBackgroundType = this.mLetterboxConfiguration.getLetterboxBackgroundType();
        boolean wallpaperShouldBeShown = letterboxBackgroundType == 3 && isLetterboxedNotForDisplayCutout(mainWindow) && (getLetterboxWallpaperBlurRadius() > 0 || getLetterboxWallpaperDarkScrimAlpha() > 0.0f) && (getLetterboxWallpaperBlurRadius() <= 0 || isLetterboxWallpaperBlurSupported());
        if (this.mShowWallpaperForLetterboxBackground != wallpaperShouldBeShown) {
            this.mShowWallpaperForLetterboxBackground = wallpaperShouldBeShown;
            this.mActivityRecord.requestUpdateWallpaperIfNeeded();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getLetterboxWallpaperBlurRadius() {
        int blurRadius = this.mLetterboxConfiguration.getLetterboxBackgroundWallpaperBlurRadius();
        if (blurRadius < 0) {
            return 0;
        }
        return blurRadius;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public float getLetterboxWallpaperDarkScrimAlpha() {
        float alpha = this.mLetterboxConfiguration.getLetterboxBackgroundWallpaperDarkScrimAlpha();
        if (alpha < 0.0f || alpha >= 1.0f) {
            return 0.0f;
        }
        return alpha;
    }

    private boolean isLetterboxWallpaperBlurSupported() {
        return ((WindowManager) this.mLetterboxConfiguration.mContext.getSystemService(WindowManager.class)).isCrossWindowBlurEnabled();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        WindowState mainWin = this.mActivityRecord.findMainWindow();
        if (mainWin == null) {
            return;
        }
        boolean areBoundsLetterboxed = mainWin.areAppWindowBoundsLetterboxed();
        pw.println(prefix + "areBoundsLetterboxed=" + areBoundsLetterboxed);
        if (!areBoundsLetterboxed) {
            return;
        }
        pw.println(prefix + "  letterboxReason=" + getLetterboxReasonString(mainWin));
        pw.println(prefix + "  activityAspectRatio=" + ActivityRecord.computeAspectRatio(this.mActivityRecord.getBounds()));
        boolean shouldShowLetterboxUi = shouldShowLetterboxUi(mainWin);
        pw.println(prefix + "shouldShowLetterboxUi=" + shouldShowLetterboxUi);
        if (!shouldShowLetterboxUi) {
            return;
        }
        pw.println(prefix + "  letterboxBackgroundColor=" + Integer.toHexString(getLetterboxBackgroundColor().toArgb()));
        pw.println(prefix + "  letterboxBackgroundType=" + LetterboxConfiguration.letterboxBackgroundTypeToString(this.mLetterboxConfiguration.getLetterboxBackgroundType()));
        pw.println(prefix + "  letterboxCornerRadius=" + getRoundedCorners(mainWin.getInsetsState()));
        if (this.mLetterboxConfiguration.getLetterboxBackgroundType() == 3) {
            pw.println(prefix + "  isLetterboxWallpaperBlurSupported=" + isLetterboxWallpaperBlurSupported());
            pw.println(prefix + "  letterboxBackgroundWallpaperDarkScrimAlpha=" + getLetterboxWallpaperDarkScrimAlpha());
            pw.println(prefix + "  letterboxBackgroundWallpaperBlurRadius=" + getLetterboxWallpaperBlurRadius());
        }
        pw.println(prefix + "  isReachabilityEnabled=" + isReachabilityEnabled());
        pw.println(prefix + "  letterboxHorizontalPositionMultiplier=" + getHorizontalPositionMultiplier(this.mActivityRecord.getParent().getConfiguration()));
        pw.println(prefix + "  fixedOrientationLetterboxAspectRatio=" + getFixedOrientationLetterboxAspectRatio(this.mActivityRecord.getParent().getConfiguration()));
    }

    private String getLetterboxReasonString(WindowState mainWin) {
        if (this.mActivityRecord.inSizeCompatMode()) {
            return "SIZE_COMPAT_MODE";
        }
        if (this.mActivityRecord.isLetterboxedForFixedOrientationAndAspectRatio()) {
            return "FIXED_ORIENTATION";
        }
        if (mainWin.isLetterboxedForDisplayCutout()) {
            return "DISPLAY_CUTOUT";
        }
        return "UNKNOWN_REASON";
    }
}
