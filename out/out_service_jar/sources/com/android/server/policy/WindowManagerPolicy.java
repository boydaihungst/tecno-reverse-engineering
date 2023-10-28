package com.android.server.policy;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.Display;
import android.view.IDisplayFoldListener;
import android.view.IWindowManager;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.WindowManagerPolicyConstants;
import android.view.animation.Animation;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.policy.IShortcutService;
import com.android.server.notification.NotificationShellCmd;
import com.android.server.wm.DisplayRotation;
import com.mediatek.server.wm.WmsExt;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/* loaded from: classes2.dex */
public interface WindowManagerPolicy extends WindowManagerPolicyConstants {
    public static final int ACTION_PASS_TO_USER = 1;
    public static final int COLOR_FADE_LAYER = 1073741825;
    public static final int FINISH_LAYOUT_REDO_ANIM = 8;
    public static final int FINISH_LAYOUT_REDO_CONFIG = 2;
    public static final int FINISH_LAYOUT_REDO_LAYOUT = 1;
    public static final int FINISH_LAYOUT_REDO_WALLPAPER = 4;
    public static final int INPUT_DISPLAY_OVERLAY_LAYER = 2130706432;
    public static final int SCREEN_DECOR_DISPLAY_OVERLAY_LAYER = 2130706431;
    public static final int TRANSIT_ENTER = 1;
    public static final int TRANSIT_EXIT = 2;
    public static final int TRANSIT_HIDE = 4;
    public static final int TRANSIT_PREVIEW_DONE = 5;
    public static final int TRANSIT_SHOW = 3;
    public static final int USER_ROTATION_FREE = 0;
    public static final int USER_ROTATION_LOCKED = 1;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface AltBarPosition {
    }

    /* loaded from: classes2.dex */
    public interface DisplayContentInfo {
        Display getDisplay();

        DisplayRotation getDisplayRotation();
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface NavigationBarPosition {
    }

    /* loaded from: classes2.dex */
    public interface OnKeyguardExitResult {
        void onKeyguardExitResult(boolean z);
    }

    /* loaded from: classes2.dex */
    public interface ScreenOffListener {
        void onScreenOff();
    }

    /* loaded from: classes2.dex */
    public interface ScreenOnListener {
        void onScreenOn();
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface UserRotationMode {
    }

    void adjustConfigurationLw(Configuration configuration, int i, int i2);

    void applyAodPolicyLw(WindowState windowState);

    int applyKeyguardOcclusionChange(boolean z);

    boolean canDismissBootAnimation();

    int checkAddPermission(int i, boolean z, String str, int[] iArr);

    Animation createHiddenByKeyguardExit(boolean z, boolean z2, boolean z3);

    Animation createKeyguardWallpaperExit(boolean z);

    void dismissKeyguardLw(IKeyguardDismissCallback iKeyguardDismissCallback, CharSequence charSequence);

    KeyEvent dispatchUnhandledKey(IBinder iBinder, KeyEvent keyEvent, int i);

    void dump(String str, PrintWriter printWriter, String[] strArr);

    void dumpDebug(ProtoOutputStream protoOutputStream, long j);

    void enableKeyguard(boolean z);

    void enableScreenAfterBoot();

    void exitKeyguardSecurely(OnKeyguardExitResult onKeyguardExitResult);

    void finishedGoingToSleep(int i);

    void finishedWakingUp(int i);

    int getUiMode();

    boolean hasNavigationBar();

    void hideBootMessages();

    boolean inKeyguardRestrictedKeyInputMode();

    void init(Context context, IWindowManager iWindowManager, WindowManagerFuncs windowManagerFuncs);

    long interceptKeyBeforeDispatching(IBinder iBinder, KeyEvent keyEvent, int i);

    int interceptKeyBeforeQueueing(KeyEvent keyEvent, int i);

    int interceptMotionBeforeQueueingNonInteractive(int i, long j, int i2);

    boolean isKeyguardDrawnLw();

    boolean isKeyguardHostWindow(WindowManager.LayoutParams layoutParams);

    boolean isKeyguardLocked();

    boolean isKeyguardOccluded();

    boolean isKeyguardSecure(int i);

    boolean isKeyguardShowing();

    boolean isKeyguardShowingAndNotOccluded();

    boolean isKeyguardTrustedLw();

    boolean isProximityPowerKeyDown();

    boolean isScreenOn();

    boolean isUserSetupComplete();

    void keepScreenOnStartedLw();

    void keepScreenOnStoppedLw();

    void lockNow(Bundle bundle);

    void notifyCameraLensCoverSwitchChanged(long j, boolean z);

    void notifyLidSwitchChanged(long j, boolean z);

    boolean okToAnimate(boolean z);

    void onKeyguardOccludedChangedLw(boolean z);

    void onPowerGroupWakefulnessChanged(int i, int i2, int i3, int i4);

    void onSystemUiStarted();

    boolean performHapticFeedback(int i, String str, int i2, boolean z, String str2);

    void registerShortcutKey(long j, IShortcutService iShortcutService) throws RemoteException;

    void resetDreamAnimationFeatureEnabledState();

    void screenTurnedOff(int i);

    void screenTurnedOn(int i);

    void screenTurningOff(int i, ScreenOffListener screenOffListener, boolean z);

    void screenTurningOn(int i, ScreenOnListener screenOnListener);

    void screenTurningOn(int i, ScreenOnListener screenOnListener, int i2, boolean z);

    void setAllowLockscreenWhenOn(int i, boolean z);

    void setConnectScreenActive(boolean z, boolean z2);

    void setCurrentUserLw(int i);

    void setDefaultDisplay(DisplayContentInfo displayContentInfo);

    void setNavBarVirtualKeyHapticFeedbackEnabledLw(boolean z);

    void setPipVisibilityLw(boolean z);

    void setProximityPowerKeyDown(boolean z);

    void setRecentsVisibilityLw(boolean z);

    void setSafeMode(boolean z);

    void setSwitchingUser(boolean z);

    void setTopFocusedDisplay(int i);

    void showBootMessage(CharSequence charSequence, boolean z);

    void showGlobalActions();

    void showRecentApps();

    void startFaceUnlock();

    void startKeyguardExitAnimation(long j, long j2);

    void startedGoingToSleep(int i);

    void startedWakingUp(int i);

    void systemBooted();

    void systemReady();

    void userActivity();

    /* loaded from: classes2.dex */
    public interface WindowState {
        boolean canShowWhenLocked();

        int getBaseType();

        String getOwningPackage();

        boolean hideLw(boolean z);

        boolean isAnimatingLw();

        boolean showLw(boolean z);

        default boolean canAddInternalSystemWindow() {
            return false;
        }
    }

    /* loaded from: classes2.dex */
    public interface WindowManagerFuncs {
        public static final int CAMERA_LENS_COVERED = 1;
        public static final int CAMERA_LENS_COVER_ABSENT = -1;
        public static final int CAMERA_LENS_UNCOVERED = 0;
        public static final int LID_ABSENT = -1;
        public static final int LID_BEHAVIOR_LOCK = 2;
        public static final int LID_BEHAVIOR_NONE = 0;
        public static final int LID_BEHAVIOR_SLEEP = 1;
        public static final int LID_CLOSED = 0;
        public static final int LID_OPEN = 1;

        int getCameraLensCoverState();

        int getLidState();

        Object getWindowManagerLock();

        boolean isAppTransitionStateIdle();

        void lockDeviceNow();

        void moveDisplayToTop(int i);

        void notifyKeyguardTrustedChanged();

        void onKeyguardShowingAndNotOccludedChanged();

        void onPowerKeyDown(boolean z);

        void onUserSwitched();

        void reboot(boolean z);

        void rebootSafeMode(boolean z);

        void registerPointerEventListener(WindowManagerPolicyConstants.PointerEventListener pointerEventListener, int i);

        void screenTurningOff(int i, ScreenOffListener screenOffListener, boolean z);

        void shutdown(boolean z);

        void switchKeyboardLayout(int i, int i2);

        void triggerAnimationFailsafe();

        void unregisterPointerEventListener(WindowManagerPolicyConstants.PointerEventListener pointerEventListener, int i);

        static String lidStateToString(int lid) {
            switch (lid) {
                case -1:
                    return "LID_ABSENT";
                case 0:
                    return "LID_CLOSED";
                case 1:
                    return "LID_OPEN";
                default:
                    return Integer.toString(lid);
            }
        }

        static String cameraLensStateToString(int lens) {
            switch (lens) {
                case -1:
                    return "CAMERA_LENS_COVER_ABSENT";
                case 0:
                    return "CAMERA_LENS_UNCOVERED";
                case 1:
                    return "CAMERA_LENS_COVERED";
                default:
                    return Integer.toString(lens);
            }
        }
    }

    default int getWindowLayerLw(WindowState win) {
        return getWindowLayerFromTypeLw(win.getBaseType(), win.canAddInternalSystemWindow());
    }

    default int getWindowLayerFromTypeLw(int type) {
        if (WindowManager.LayoutParams.isSystemAlertWindowType(type)) {
            throw new IllegalArgumentException("Use getWindowLayerFromTypeLw() or getWindowLayerLw() for alert window types");
        }
        return getWindowLayerFromTypeLw(type, false);
    }

    default int getWindowLayerFromTypeLw(int type, boolean canAddInternalSystemWindow) {
        return getWindowLayerFromTypeLw(type, canAddInternalSystemWindow, false);
    }

    default int getWindowLayerFromTypeLw(int type, boolean canAddInternalSystemWindow, boolean roundedCornerOverlay) {
        if (roundedCornerOverlay && canAddInternalSystemWindow) {
            return getMaxWindowLayer();
        }
        if (type >= 1 && type <= 99) {
            return 2;
        }
        switch (type) {
            case 2000:
                return 15;
            case 2001:
                return 4;
            case 2002:
            case 2030:
            case 2034:
            case 2035:
            case 2037:
                return 3;
            case 2003:
                return canAddInternalSystemWindow ? 12 : 9;
            case 2004:
            case 2014:
            case 2023:
            case 2025:
            case 2028:
            case 2029:
            case 2042:
            default:
                Slog.e(WmsExt.TAG, "Unknown window type: " + type);
                return 3;
            case 2005:
                return 7;
            case 2006:
                return canAddInternalSystemWindow ? 23 : 10;
            case 2007:
                return 8;
            case 2008:
                return 6;
            case 2009:
                return 19;
            case 2010:
                return canAddInternalSystemWindow ? 27 : 9;
            case 2011:
                return 13;
            case 2012:
                return 14;
            case 2013:
                return 1;
            case 2015:
                return 33;
            case 2016:
                return 30;
            case 2017:
                return 18;
            case 2018:
                return 35;
            case 2019:
                return 24;
            case NotificationShellCmd.NOTIFICATION_ID /* 2020 */:
                return 22;
            case 2021:
            case 2043:
                return 34;
            case 2022:
                return 5;
            case 2024:
                return 25;
            case 2026:
                return 29;
            case 2027:
                return 28;
            case 2031:
                return 21;
            case 2032:
                return 31;
            case 2033:
                return 20;
            case 2036:
                return 26;
            case 2038:
                return 11;
            case 2039:
                return 32;
            case 2040:
                return 17;
            case 2041:
                return 16;
            case 2044:
                return 28;
            case 2045:
                return 29;
        }
    }

    default int getMaxWindowLayer() {
        return 36;
    }

    default int getSubWindowLayerFromTypeLw(int type) {
        switch (type) {
            case 1000:
            case 1003:
                return 1;
            case 1001:
                return -2;
            case 1002:
                return 2;
            case 1004:
                return -1;
            case 1005:
                return 3;
            default:
                Slog.e(WmsExt.TAG, "Unknown sub-window type: " + type);
                return 0;
        }
    }

    default boolean isKeyguardUnoccluding() {
        return false;
    }

    default void setDismissImeOnBackKeyPressed(boolean newValue) {
    }

    static String userRotationModeToString(int mode) {
        switch (mode) {
            case 0:
                return "USER_ROTATION_FREE";
            case 1:
                return "USER_ROTATION_LOCKED";
            default:
                return Integer.toString(mode);
        }
    }

    default void registerDisplayFoldListener(IDisplayFoldListener listener) {
    }

    default void unregisterDisplayFoldListener(IDisplayFoldListener listener) {
    }

    default void setOverrideFoldedArea(Rect area) {
    }

    default Rect getFoldedArea() {
        return new Rect();
    }

    default void onDefaultDisplayFocusChangedLw(WindowState newFocus) {
    }

    default void startedNotifyFaceunlock(int reason) {
    }
}
