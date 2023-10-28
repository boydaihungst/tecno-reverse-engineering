package com.android.server.wm;

import android.graphics.Region;
import android.os.IBinder;
import android.view.IWindow;
import android.view.InputApplicationHandle;
import android.view.InputWindowHandle;
import android.view.SurfaceControl;
import java.util.Objects;
/* loaded from: classes2.dex */
class InputWindowHandleWrapper {
    private boolean mChanged = true;
    private final InputWindowHandle mHandle;

    /* JADX INFO: Access modifiers changed from: package-private */
    public InputWindowHandleWrapper(InputWindowHandle handle) {
        this.mHandle = handle;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isChanged() {
        return this.mChanged;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forceChange() {
        this.mChanged = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void applyChangesToSurface(SurfaceControl.Transaction t, SurfaceControl sc) {
        t.setInputWindowInfo(sc, this.mHandle);
        this.mChanged = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getDisplayId() {
        return this.mHandle.displayId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFocusable() {
        return (this.mHandle.inputConfig & 4) == 0;
    }

    boolean isPaused() {
        return (this.mHandle.inputConfig & 128) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTrustedOverlay() {
        return (this.mHandle.inputConfig & 256) != 0;
    }

    boolean hasWallpaper() {
        return (this.mHandle.inputConfig & 32) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InputApplicationHandle getInputApplicationHandle() {
        return this.mHandle.inputApplicationHandle;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setInputApplicationHandle(InputApplicationHandle handle) {
        if (this.mHandle.inputApplicationHandle == handle) {
            return;
        }
        this.mHandle.inputApplicationHandle = handle;
        this.mChanged = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setToken(IBinder token) {
        if (this.mHandle.token == token) {
            return;
        }
        this.mHandle.token = token;
        this.mChanged = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setName(String name) {
        if (Objects.equals(this.mHandle.name, name)) {
            return;
        }
        this.mHandle.name = name;
        this.mChanged = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLayoutParamsFlags(int flags) {
        if (this.mHandle.layoutParamsFlags == flags) {
            return;
        }
        this.mHandle.layoutParamsFlags = flags;
        this.mChanged = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLayoutParamsType(int type) {
        if (this.mHandle.layoutParamsType == type) {
            return;
        }
        this.mHandle.layoutParamsType = type;
        this.mChanged = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDispatchingTimeoutMillis(long timeout) {
        if (this.mHandle.dispatchingTimeoutMillis == timeout) {
            return;
        }
        this.mHandle.dispatchingTimeoutMillis = timeout;
        this.mChanged = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTouchableRegion(Region region) {
        if (this.mHandle.touchableRegion.equals(region)) {
            return;
        }
        this.mHandle.touchableRegion.set(region);
        this.mChanged = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearTouchableRegion() {
        if (this.mHandle.touchableRegion.isEmpty()) {
            return;
        }
        this.mHandle.touchableRegion.setEmpty();
        this.mChanged = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setFocusable(boolean focusable) {
        if (isFocusable() == focusable) {
            return;
        }
        this.mHandle.setInputConfig(4, !focusable);
        this.mChanged = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTouchOcclusionMode(int mode) {
        if (this.mHandle.touchOcclusionMode == mode) {
            return;
        }
        this.mHandle.touchOcclusionMode = mode;
        this.mChanged = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setHasWallpaper(boolean hasWallpaper) {
        if (hasWallpaper() == hasWallpaper) {
            return;
        }
        this.mHandle.setInputConfig(32, hasWallpaper);
        this.mChanged = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPaused(boolean paused) {
        if (isPaused() == paused) {
            return;
        }
        this.mHandle.setInputConfig(128, paused);
        this.mChanged = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTrustedOverlay(boolean trustedOverlay) {
        if (isTrustedOverlay() == trustedOverlay) {
            return;
        }
        this.mHandle.setInputConfig(256, trustedOverlay);
        this.mChanged = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setOwnerPid(int pid) {
        if (this.mHandle.ownerPid == pid) {
            return;
        }
        this.mHandle.ownerPid = pid;
        this.mChanged = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setOwnerUid(int uid) {
        if (this.mHandle.ownerUid == uid) {
            return;
        }
        this.mHandle.ownerUid = uid;
        this.mChanged = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPackageName(String packageName) {
        if (Objects.equals(this.mHandle.packageName, packageName)) {
            return;
        }
        this.mHandle.packageName = packageName;
        this.mChanged = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDisplayId(int displayId) {
        if (this.mHandle.displayId == displayId) {
            return;
        }
        this.mHandle.displayId = displayId;
        this.mChanged = true;
    }

    void setFrame(int left, int top, int right, int bottom) {
        if (this.mHandle.frameLeft == left && this.mHandle.frameTop == top && this.mHandle.frameRight == right && this.mHandle.frameBottom == bottom) {
            return;
        }
        this.mHandle.frameLeft = left;
        this.mHandle.frameTop = top;
        this.mHandle.frameRight = right;
        this.mHandle.frameBottom = bottom;
        this.mChanged = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSurfaceInset(int inset) {
        if (this.mHandle.surfaceInset == inset) {
            return;
        }
        this.mHandle.surfaceInset = inset;
        this.mChanged = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setScaleFactor(float scale) {
        if (this.mHandle.scaleFactor == scale) {
            return;
        }
        this.mHandle.scaleFactor = scale;
        this.mChanged = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTouchableRegionCrop(SurfaceControl bounds) {
        if (this.mHandle.touchableRegionSurfaceControl.get() == bounds) {
            return;
        }
        this.mHandle.setTouchableRegionCrop(bounds);
        this.mChanged = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setReplaceTouchableRegionWithCrop(boolean replace) {
        if (this.mHandle.replaceTouchableRegionWithCrop == replace) {
            return;
        }
        this.mHandle.replaceTouchableRegionWithCrop = replace;
        this.mChanged = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setWindowToken(IWindow windowToken) {
        if (this.mHandle.getWindow() == windowToken) {
            return;
        }
        this.mHandle.setWindowToken(windowToken);
        this.mChanged = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setInputConfigMasked(int inputConfig, int mask) {
        int inputConfigMasked = inputConfig & mask;
        if (inputConfigMasked == (this.mHandle.inputConfig & mask)) {
            return;
        }
        this.mHandle.inputConfig &= ~mask;
        this.mHandle.inputConfig |= inputConfigMasked;
        this.mChanged = true;
    }

    public String toString() {
        return this.mHandle + ", changed=" + this.mChanged;
    }
}
