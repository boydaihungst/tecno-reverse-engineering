package com.mediatek.server.wm;

import android.view.SurfaceControl;
import android.view.WindowManager;
import android.window.ClientWindowFrames;
import com.android.server.wm.WindowState;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
public class WindowManagerDebugger {
    public static final String TAG = "WindowManagerDebugger";
    public static boolean WMS_DEBUG_ENG = false;
    public static boolean WMS_DEBUG_USER_DEBUG = false;
    public static boolean WMS_DEBUG_USER = false;
    public static boolean WMS_DEBUG_LOG_OFF = false;

    public void runDebug(PrintWriter pw, String[] args, int opti) {
    }

    public void debugInterceptKeyBeforeQueueing(String tag, int keycode, boolean interactive, boolean keyguardActive, int policyFlags, boolean down, boolean canceled, boolean isWakeKey, int result, boolean useHapticFeedback, boolean isInjected) {
    }

    public void debugApplyPostLayoutPolicyLw(String tag, WindowState win, WindowManager.LayoutParams attrs, WindowState mTopFullscreenOpaqueWindowState, WindowState attached, WindowState imeTarget, boolean dreamingLockscreen, boolean showingDream) {
    }

    public void debugLayoutWindowLw(String tag, int adjust, int type, int fl, boolean canHideNavigationBar, int sysUiFl) {
    }

    public void debugGetOrientation(String tag, boolean displayFrozen, int lastWindowForcedOrientation, int lastKeyguardForcedOrientation) {
    }

    public void debugGetOrientingWindow(String tag, WindowState w, WindowManager.LayoutParams attrs, boolean isVisible, boolean policyVisibilityAfterAnim, int policyVisibility, boolean destroying) {
    }

    public void debugPrepareSurfaceLocked(String tag, boolean isWallpaper, WindowState win, boolean isParentWindowHidden, boolean isOnScreen, int policyVisibility, boolean hasSurface, boolean destroying, boolean lastHidden) {
    }

    public void debugRelayoutWindow(String tag, WindowState win, int originType, int changeType) {
    }

    public void debugInputAttr(String tag, WindowManager.LayoutParams attrs) {
    }

    public void debugViewVisibility(String tag, WindowState win, int viewVisibility, int oldVisibility, boolean focusMayChange, int requestedWidth, int requestedHeight, ClientWindowFrames outFrames, SurfaceControl outSurfaceControl) {
    }
}
