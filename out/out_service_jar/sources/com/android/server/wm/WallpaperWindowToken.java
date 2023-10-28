package com.android.server.wm;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.animation.Animation;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class WallpaperWindowToken extends WindowToken {
    private static final String TAG = "WindowManager";
    private boolean mVisibleRequested;

    WallpaperWindowToken(WindowManagerService service, IBinder token, boolean explicit, DisplayContent dc, boolean ownerCanManageAppTokens) {
        this(service, token, explicit, dc, ownerCanManageAppTokens, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WallpaperWindowToken(WindowManagerService service, IBinder token, boolean explicit, DisplayContent dc, boolean ownerCanManageAppTokens, Bundle options) {
        super(service, token, 2013, explicit, dc, ownerCanManageAppTokens, false, false, options);
        this.mVisibleRequested = false;
        dc.mWallpaperController.addWallpaperToken(this);
        setWindowingMode(1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public WallpaperWindowToken asWallpaperToken() {
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowToken
    public void setExiting(boolean animateExit) {
        super.setExiting(animateExit);
        this.mDisplayContent.mWallpaperController.removeWallpaperToken(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendWindowWallpaperCommand(String action, int x, int y, int z, Bundle extras, boolean sync) {
        for (int wallpaperNdx = this.mChildren.size() - 1; wallpaperNdx >= 0; wallpaperNdx--) {
            WindowState wallpaper = (WindowState) this.mChildren.get(wallpaperNdx);
            try {
                wallpaper.mClient.dispatchWallpaperCommand(action, x, y, z, extras, sync);
                sync = false;
            } catch (RemoteException e) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateWallpaperOffset(boolean sync) {
        WallpaperController wallpaperController = this.mDisplayContent.mWallpaperController;
        for (int wallpaperNdx = this.mChildren.size() - 1; wallpaperNdx >= 0; wallpaperNdx--) {
            WindowState wallpaper = (WindowState) this.mChildren.get(wallpaperNdx);
            if (wallpaperController.updateWallpaperOffset(wallpaper, sync)) {
                sync = false;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startAnimation(Animation anim) {
        for (int ndx = this.mChildren.size() - 1; ndx >= 0; ndx--) {
            WindowState windowState = (WindowState) this.mChildren.get(ndx);
            windowState.startAnimation(anim);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateWallpaperWindows(boolean visible) {
        WindowState wallpaperTarget;
        boolean changed = false;
        if (this.mVisibleRequested != visible) {
            if (ProtoLogCache.WM_DEBUG_WALLPAPER_enabled) {
                String protoLogParam0 = String.valueOf(this.token);
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_WALLPAPER, 733466617, 12, (String) null, new Object[]{protoLogParam0, Boolean.valueOf(visible)});
            }
            setVisibility(visible);
            changed = true;
        }
        if (this.mTransitionController.isShellTransitionsEnabled()) {
            if (!this.mTransitionController.useShellTransitionsRotation() && changed && visible && (wallpaperTarget = this.mDisplayContent.mWallpaperController.getWallpaperTarget()) != null && wallpaperTarget.mToken.hasFixedRotationTransform()) {
                linkFixedRotationTransform(wallpaperTarget.mToken);
            }
            return changed;
        }
        WindowState wallpaperTarget2 = this.mDisplayContent.mWallpaperController.getWallpaperTarget();
        if (visible && wallpaperTarget2 != null) {
            RecentsAnimationController recentsAnimationController = this.mWmService.getRecentsAnimationController();
            if (recentsAnimationController != null && recentsAnimationController.isAnimatingTask(wallpaperTarget2.getTask())) {
                recentsAnimationController.linkFixedRotationTransformIfNeeded(this);
            } else if ((wallpaperTarget2.mActivityRecord == null || wallpaperTarget2.mActivityRecord.mVisibleRequested) && wallpaperTarget2.mToken.hasFixedRotationTransform()) {
                linkFixedRotationTransform(wallpaperTarget2.mToken);
            }
        }
        setVisible(visible);
        return changed;
    }

    private void setVisible(boolean visible) {
        boolean wasClientVisible = isClientVisible();
        setClientVisible(visible);
        if (visible && !wasClientVisible) {
            for (int i = this.mChildren.size() - 1; i >= 0; i--) {
                WindowState wallpaper = (WindowState) this.mChildren.get(i);
                wallpaper.requestUpdateWallpaperIfNeeded();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setVisibility(boolean visible) {
        if (this.mVisibleRequested != visible) {
            this.mTransitionController.collect(this);
            setVisibleRequested(visible);
        }
        if (!visible && (this.mTransitionController.inTransition() || getDisplayContent().mAppTransition.isRunning())) {
            return;
        }
        commitVisibility(visible);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void commitVisibility(boolean visible) {
        if (visible == isVisible()) {
            return;
        }
        if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
            String protoLogParam0 = String.valueOf(this);
            boolean protoLogParam1 = isVisible();
            boolean protoLogParam2 = this.mVisibleRequested;
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, 3593205, 60, (String) null, new Object[]{protoLogParam0, Boolean.valueOf(protoLogParam1), Boolean.valueOf(protoLogParam2)});
        }
        setVisibleRequested(visible);
        setVisible(visible);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasVisibleNotDrawnWallpaper() {
        if (isVisible()) {
            for (int j = this.mChildren.size() - 1; j >= 0; j--) {
                WindowState wallpaper = (WindowState) this.mChildren.get(j);
                if (!wallpaper.isDrawn() && wallpaper.isVisible()) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void forAllWallpaperWindows(Consumer<WallpaperWindowToken> callback) {
        callback.accept(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean fillsParent() {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean showWallpaper() {
        return false;
    }

    void setVisibleRequested(boolean visible) {
        if (this.mVisibleRequested == visible) {
            return;
        }
        this.mVisibleRequested = visible;
        setInsetsFrozen(!visible);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean isVisibleRequested() {
        return this.mVisibleRequested;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean isVisible() {
        return isClientVisible();
    }

    @Override // com.android.server.wm.WindowToken
    public String toString() {
        if (this.stringName == null) {
            this.stringName = "WallpaperWindowToken{" + Integer.toHexString(System.identityHashCode(this)) + " token=" + this.token + '}';
        }
        return this.stringName;
    }
}
