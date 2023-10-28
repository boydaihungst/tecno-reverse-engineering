package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ThunderbackConfig;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Debug;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.RotationUtils;
import android.util.Slog;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.view.IDisplayWindowRotationCallback;
import android.view.Surface;
import android.window.TransitionRequestInfo;
import android.window.WindowContainerTransaction;
import com.android.internal.os.BackgroundThread;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.DisplayThread;
import com.android.server.LocalServices;
import com.android.server.UiModeManagerService;
import com.android.server.UiThread;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.transsion.hubcore.server.wm.ITranDisplayRotation;
import com.transsion.hubcore.server.wm.ITranWindowManagerService;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* loaded from: classes2.dex */
public class DisplayRotation {
    private static final int ALLOW_ALL_ROTATIONS_DISABLED = 0;
    private static final int ALLOW_ALL_ROTATIONS_ENABLED = 1;
    private static final int ALLOW_ALL_ROTATIONS_UNDEFINED = -1;
    private static final int CAMERA_ROTATION_DISABLED = 0;
    private static final int CAMERA_ROTATION_ENABLED = 1;
    private static final int REMOTE_ROTATION_TIMEOUT_MS = 800;
    private static final String TAG = "WindowManager";
    private static int mSavedRotation = -1;
    public final boolean isDefaultDisplay;
    private int mAllowAllRotations;
    private boolean mAllowSeamlessRotationDespiteNavBarMoving;
    private int mCameraRotationMode;
    private final int mCarDockRotation;
    private final Context mContext;
    private int mCurrentAppOrientation;
    private boolean mDefaultFixedToUserRotation;
    private int mDeferredRotationPauseCount;
    private int mDemoHdmiRotation;
    private boolean mDemoHdmiRotationLock;
    private int mDemoRotation;
    private boolean mDemoRotationLock;
    private final int mDeskDockRotation;
    private final DisplayContent mDisplayContent;
    private final DisplayPolicy mDisplayPolicy;
    private final Runnable mDisplayRotationHandlerTimeout;
    private final DisplayWindowSettings mDisplayWindowSettings;
    private int mFixedToUserRotation;
    private boolean mIsWaitingForRemoteRotation;
    int mLandscapeRotation;
    private int mLastOrientation;
    int mLastSensorRotation;
    private final int mLidOpenRotation;
    private final Object mLock;
    private OrientationListener mOrientationListener;
    int mPortraitRotation;
    private final IDisplayWindowRotationCallback mRemoteRotationCallback;
    private boolean mRotatingSeamlessly;
    private int mRotation;
    private final RotationHistory mRotationHistory;
    private int mSeamlessRotationCount;
    int mSeascapeRotation;
    private final WindowManagerService mService;
    private SettingsObserver mSettingsObserver;
    private int mShowRotationSuggestions;
    private StatusBarManagerInternal mStatusBarManagerInternal;
    private final boolean mSupportAutoRotation;
    private final RotationAnimationPair mTmpRotationAnim;
    private final int mUndockedHdmiRotation;
    int mUpsideDownRotation;
    private int mUserRotation;
    private int mUserRotationMode;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    private @interface AllowAllRotations {
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class RotationAnimationPair {
        int mEnter;
        int mExit;

        private RotationAnimationPair() {
        }
    }

    /* renamed from: com.android.server.wm.DisplayRotation$2  reason: invalid class name */
    /* loaded from: classes2.dex */
    class AnonymousClass2 extends IDisplayWindowRotationCallback.Stub {
        AnonymousClass2() {
        }

        public void continueRotateDisplay(int targetRotation, WindowContainerTransaction t) {
            synchronized (DisplayRotation.this.mService.getWindowManagerLock()) {
                DisplayRotation.this.mService.mH.sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.wm.DisplayRotation$2$$ExternalSyntheticLambda0
                    public final void accept(Object obj, Object obj2, Object obj3) {
                        ((DisplayRotation) obj).continueRotation(((Integer) obj2).intValue(), (WindowContainerTransaction) obj3);
                    }
                }, DisplayRotation.this, Integer.valueOf(targetRotation), t));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayRotation(WindowManagerService service, DisplayContent displayContent) {
        this(service, displayContent, displayContent.getDisplayPolicy(), service.mDisplayWindowSettings, service.mContext, service.getWindowManagerLock());
    }

    DisplayRotation(WindowManagerService service, DisplayContent displayContent, DisplayPolicy displayPolicy, DisplayWindowSettings displayWindowSettings, Context context, Object lock) {
        this.mTmpRotationAnim = new RotationAnimationPair();
        this.mRotationHistory = new RotationHistory();
        this.mCurrentAppOrientation = -1;
        this.mLastOrientation = -1;
        this.mLastSensorRotation = -1;
        this.mAllowAllRotations = -1;
        this.mUserRotationMode = 0;
        this.mUserRotation = 0;
        this.mCameraRotationMode = 0;
        this.mFixedToUserRotation = 0;
        this.mIsWaitingForRemoteRotation = false;
        this.mDisplayRotationHandlerTimeout = new Runnable() { // from class: com.android.server.wm.DisplayRotation.1
            @Override // java.lang.Runnable
            public void run() {
                DisplayRotation displayRotation = DisplayRotation.this;
                displayRotation.continueRotation(displayRotation.mRotation, null);
            }
        };
        this.mRemoteRotationCallback = new AnonymousClass2();
        this.mService = service;
        this.mDisplayContent = displayContent;
        this.mDisplayPolicy = displayPolicy;
        this.mDisplayWindowSettings = displayWindowSettings;
        this.mContext = context;
        this.mLock = lock;
        boolean z = displayContent.isDefaultDisplay;
        this.isDefaultDisplay = z;
        this.mSupportAutoRotation = context.getResources().getBoolean(17891769);
        this.mLidOpenRotation = readRotation(17694853);
        this.mCarDockRotation = readRotation(17694769);
        this.mDeskDockRotation = readRotation(17694806);
        this.mUndockedHdmiRotation = readRotation(17694964);
        if (z) {
            Handler uiHandler = UiThread.getHandler();
            OrientationListener orientationListener = new OrientationListener(context, uiHandler);
            this.mOrientationListener = orientationListener;
            orientationListener.setCurrentRotation(this.mRotation);
            this.mSettingsObserver = new SettingsObserver(uiHandler);
            DisplayThread.getHandler().post(new Runnable() { // from class: com.android.server.wm.DisplayRotation$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    DisplayRotation.this.m8004lambda$new$0$comandroidserverwmDisplayRotation();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-wm-DisplayRotation  reason: not valid java name */
    public /* synthetic */ void m8004lambda$new$0$comandroidserverwmDisplayRotation() {
        this.mSettingsObserver.observe();
    }

    private int readRotation(int resID) {
        try {
            int rotation = this.mContext.getResources().getInteger(resID);
            switch (rotation) {
                case 0:
                    return 0;
                case 90:
                    return 1;
                case FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__CREDENTIAL_MANAGEMENT_APP_REQUEST_ACCEPTED /* 180 */:
                    return 2;
                case 270:
                    return 3;
                default:
                    return -1;
            }
        } catch (Resources.NotFoundException e) {
            return -1;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateUserDependentConfiguration(Resources currentUserRes) {
        this.mAllowSeamlessRotationDespiteNavBarMoving = currentUserRes.getBoolean(17891351);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void configure(int width, int height) {
        Resources res = this.mContext.getResources();
        if (width > height) {
            this.mLandscapeRotation = 0;
            this.mSeascapeRotation = 2;
            if (res.getBoolean(17891734)) {
                this.mPortraitRotation = 1;
                this.mUpsideDownRotation = 3;
            } else {
                this.mPortraitRotation = 3;
                this.mUpsideDownRotation = 1;
            }
        } else {
            this.mPortraitRotation = 0;
            this.mUpsideDownRotation = 2;
            if (res.getBoolean(17891734)) {
                this.mLandscapeRotation = 3;
                this.mSeascapeRotation = 1;
            } else {
                this.mLandscapeRotation = 1;
                this.mSeascapeRotation = 3;
            }
        }
        if ("portrait".equals(SystemProperties.get("persist.demo.hdmirotation"))) {
            this.mDemoHdmiRotation = this.mPortraitRotation;
        } else {
            this.mDemoHdmiRotation = this.mLandscapeRotation;
        }
        this.mDemoHdmiRotationLock = SystemProperties.getBoolean("persist.demo.hdmirotationlock", false);
        if ("portrait".equals(SystemProperties.get("persist.demo.remoterotation"))) {
            this.mDemoRotation = this.mPortraitRotation;
        } else {
            this.mDemoRotation = this.mLandscapeRotation;
        }
        this.mDemoRotationLock = SystemProperties.getBoolean("persist.demo.rotationlock", false);
        boolean isCar = this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive");
        boolean isTv = this.mContext.getPackageManager().hasSystemFeature("android.software.leanback");
        this.mDefaultFixedToUserRotation = (isCar || isTv || this.mService.mIsPc || this.mDisplayContent.forceDesktopMode()) && !"true".equals(SystemProperties.get("config.override_forced_orient"));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void applyCurrentRotation(int rotation) {
        this.mRotationHistory.addRecord(this, rotation);
        OrientationListener orientationListener = this.mOrientationListener;
        if (orientationListener != null) {
            orientationListener.setCurrentRotation(rotation);
        }
    }

    void setRotation(int rotation) {
        this.mRotation = rotation;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getRotation() {
        return this.mRotation;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getLastOrientation() {
        return this.mLastOrientation;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateOrientation(int newOrientation, boolean forceUpdate) {
        if (newOrientation == this.mLastOrientation && !forceUpdate) {
            return false;
        }
        this.mLastOrientation = newOrientation;
        if (newOrientation != this.mCurrentAppOrientation) {
            this.mCurrentAppOrientation = newOrientation;
            if (this.isDefaultDisplay) {
                updateOrientationListenerLw();
            }
        }
        return updateRotationUnchecked(forceUpdate);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateRotationAndSendNewConfigIfChanged() {
        boolean changed = updateRotationUnchecked(false);
        if (changed) {
            this.mDisplayContent.sendNewConfiguration();
        }
        if (ThunderbackConfig.isVersion4()) {
            ITranDisplayRotation.Instance().updateRotationFinished();
        }
        return changed;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateRotationUnchecked(boolean forceUpdate) {
        TransitionRequestInfo.DisplayChange displayChange;
        int displayId = this.mDisplayContent.getDisplayId();
        if (!forceUpdate) {
            if (this.mDeferredRotationPauseCount > 0) {
                if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, 1497304204, 0, (String) null, (Object[]) null);
                }
                return false;
            }
            ScreenRotationAnimation screenRotationAnimation = this.mDisplayContent.getRotationAnimation();
            if (screenRotationAnimation != null && screenRotationAnimation.isAnimating()) {
                if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, 292904800, 0, (String) null, (Object[]) null);
                }
                return false;
            } else if (this.mService.mDisplayFrozen) {
                if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, 1947239194, 0, (String) null, (Object[]) null);
                }
                return false;
            } else if (this.mDisplayContent.mFixedRotationTransitionListener.shouldDeferRotation()) {
                this.mLastOrientation = -2;
                return false;
            } else if (ITranDisplayRotation.Instance().isThunderbackTransitionAnimationRunning(this.mDisplayContent.mAppTransitionController.isThunderbackTransitionAnimationRunning())) {
                return false;
            }
        }
        if (!this.mService.mDisplayEnabled) {
            if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, -1117599386, 0, (String) null, (Object[]) null);
            }
            return false;
        }
        int oldRotation = this.mRotation;
        int lastOrientation = this.mLastOrientation;
        int rotation = rotationForOrientation(lastOrientation, oldRotation);
        if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
            String protoLogParam0 = String.valueOf(Surface.rotationToString(rotation));
            long protoLogParam1 = rotation;
            long protoLogParam2 = displayId;
            String protoLogParam3 = String.valueOf(ActivityInfo.screenOrientationToString(lastOrientation));
            long protoLogParam4 = lastOrientation;
            String protoLogParam5 = String.valueOf(Surface.rotationToString(oldRotation));
            long protoLogParam6 = oldRotation;
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, -1263316010, 4372, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1), Long.valueOf(protoLogParam2), protoLogParam3, Long.valueOf(protoLogParam4), protoLogParam5, Long.valueOf(protoLogParam6)});
        }
        if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
            long protoLogParam02 = displayId;
            String protoLogParam12 = String.valueOf(ActivityInfo.screenOrientationToString(lastOrientation));
            long protoLogParam22 = lastOrientation;
            String protoLogParam32 = String.valueOf(Surface.rotationToString(rotation));
            long protoLogParam42 = rotation;
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, -766059044, 273, (String) null, new Object[]{Long.valueOf(protoLogParam02), protoLogParam12, Long.valueOf(protoLogParam22), protoLogParam32, Long.valueOf(protoLogParam42)});
        }
        if (oldRotation == rotation) {
            return false;
        }
        if (ITranWindowManagerService.Instance().getOneHandCurrentState() != 0) {
            if (rotation == 1 || rotation == 3) {
                ITranWindowManagerService.Instance().exitOneHandMode("landscape_rotation");
            } else if (rotation == 0) {
                this.mRotation = rotation;
                if (oldRotation != 0) {
                    return false;
                }
            }
        }
        RecentsAnimationController recentsAnimationController = this.mService.getRecentsAnimationController();
        if (recentsAnimationController != null) {
            recentsAnimationController.cancelAnimationForDisplayChange();
        }
        if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
            long protoLogParam03 = displayId;
            long protoLogParam13 = rotation;
            long protoLogParam23 = oldRotation;
            long protoLogParam33 = lastOrientation;
            displayChange = null;
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, -1730156332, 85, (String) null, new Object[]{Long.valueOf(protoLogParam03), Long.valueOf(protoLogParam13), Long.valueOf(protoLogParam23), Long.valueOf(protoLogParam33)});
        } else {
            displayChange = null;
        }
        Slog.d("WindowManager", "DisplayRotation updateRotationUnchecked id :" + displayId + " rotation changed :" + rotation + " from :" + oldRotation + ", and lastOrientation = " + lastOrientation + ", mUserRotation = " + Surface.rotationToString(this.mUserRotation) + ", mUserRotationMode = " + this.mUserRotationMode);
        if (Build.IS_DEBUG_ENABLE) {
            Slog.d("WindowManager", "DisplayRotation updateRotationUnchecked ,and the callers= " + Debug.getCallers(6));
        }
        if (RotationUtils.deltaRotation(oldRotation, rotation) != 2) {
            this.mDisplayContent.mWaitingForConfig = true;
        }
        this.mRotation = rotation;
        this.mDisplayContent.setLayoutNeeded();
        SplitScreenHelper.onUpdateRotation(this.mDisplayContent);
        if (this.mDisplayContent.mTransitionController.isShellTransitionsEnabled()) {
            boolean wasCollecting = this.mDisplayContent.mTransitionController.isCollecting();
            TransitionRequestInfo.DisplayChange change = wasCollecting ? displayChange : new TransitionRequestInfo.DisplayChange(this.mDisplayContent.getDisplayId(), oldRotation, this.mRotation);
            this.mDisplayContent.requestChangeTransitionIfNeeded(536870912, change);
            if (wasCollecting) {
                startRemoteRotation(oldRotation, this.mRotation);
                return true;
            }
            return true;
        }
        this.mService.mWindowsFreezingScreen = 1;
        this.mService.mH.sendNewMessageDelayed(11, this.mDisplayContent, 2000L);
        if (shouldRotateSeamlessly(oldRotation, rotation, forceUpdate)) {
            prepareSeamlessRotation();
        } else {
            prepareNormalRotationAnimation();
        }
        startRemoteRotation(oldRotation, this.mRotation);
        ITranDisplayRotation.Instance().onNotifySaveRotation(this.mService);
        return true;
    }

    private void notifySaveRotation() {
        BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.wm.DisplayRotation$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                DisplayRotation.this.m8005xd45cf313();
            }
        });
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [641=4, 642=4, 643=4, 644=4, 645=4, 647=11, 648=7, 639=5] */
    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: saveRotationToProcFile */
    public void m8005xd45cf313() {
        File rotationFile;
        int rotation = this.mService.getDefaultDisplayRotation();
        if (rotation == mSavedRotation) {
            return;
        }
        FileWriter fileWritter = null;
        try {
            try {
                rotationFile = new File("/proc/border_suppression");
            } catch (IOException e) {
                Slog.w("WindowManager", "saveRotationToProcFile() fail, exception:" + e.toString());
                e.printStackTrace();
                mSavedRotation = -1;
                if (fileWritter == null) {
                    return;
                }
                try {
                    try {
                        fileWritter.close();
                    } catch (IOException e2) {
                        Slog.w("WindowManager", "saveRotationToProcFile() close fail, exception:" + e2.toString());
                        e2.printStackTrace();
                        mSavedRotation = -1;
                    }
                } finally {
                }
            }
            if (!rotationFile.exists()) {
                if (0 != 0) {
                    try {
                        try {
                            fileWritter.close();
                        } catch (IOException e3) {
                            Slog.w("WindowManager", "saveRotationToProcFile() close fail, exception:" + e3.toString());
                            e3.printStackTrace();
                            mSavedRotation = -1;
                        }
                        return;
                    } finally {
                    }
                }
                return;
            }
            try {
                fileWritter = new FileWriter("/proc/border_suppression", false);
                fileWritter.write(String.valueOf(rotation));
                fileWritter.flush();
                mSavedRotation = rotation;
                Slog.i("WindowManager", "saveRotationToProcFile() success set border_suppression:" + rotation);
                try {
                    fileWritter.close();
                } catch (IOException e4) {
                    Slog.w("WindowManager", "saveRotationToProcFile() close fail, exception:" + e4.toString());
                    e4.printStackTrace();
                    mSavedRotation = -1;
                }
            } finally {
            }
        } catch (Throwable th) {
            try {
                if (fileWritter != null) {
                    try {
                        fileWritter.close();
                    } catch (IOException e5) {
                        Slog.w("WindowManager", "saveRotationToProcFile() close fail, exception:" + e5.toString());
                        e5.printStackTrace();
                        mSavedRotation = -1;
                    }
                }
                throw th;
            } finally {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static DisplayContent getDisplayFromTransition(Transition transition) {
        for (int i = transition.mParticipants.size() - 1; i >= 0; i--) {
            WindowContainer wc = transition.mParticipants.valueAt(i);
            if (wc instanceof DisplayContent) {
                return (DisplayContent) wc;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isWaitingForRemoteRotation() {
        return this.mIsWaitingForRemoteRotation;
    }

    private void startRemoteRotation(int fromRotation, int toRotation) {
        if (this.mService.mDisplayRotationController == null) {
            return;
        }
        this.mIsWaitingForRemoteRotation = true;
        try {
            this.mService.mDisplayRotationController.onRotateDisplay(this.mDisplayContent.getDisplayId(), fromRotation, toRotation, this.mRemoteRotationCallback);
            ITranDisplayRotation.Instance().hookDefaultDisplayRotation(this.mDisplayContent.getDisplayId(), toRotation);
            this.mService.mH.removeCallbacks(this.mDisplayRotationHandlerTimeout);
            this.mService.mH.postDelayed(this.mDisplayRotationHandlerTimeout, 800L);
        } catch (RemoteException e) {
            this.mIsWaitingForRemoteRotation = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void continueRotation(int targetRotation, WindowContainerTransaction t) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (targetRotation == this.mRotation && this.mIsWaitingForRemoteRotation) {
                    this.mService.mH.removeCallbacks(this.mDisplayRotationHandlerTimeout);
                    this.mIsWaitingForRemoteRotation = false;
                    if (this.mDisplayContent.mTransitionController.isShellTransitionsEnabled()) {
                        if (!this.mDisplayContent.mTransitionController.isCollecting()) {
                            throw new IllegalStateException("Trying to rotate outside a transition");
                        }
                        this.mDisplayContent.mTransitionController.collect(this.mDisplayContent);
                        this.mDisplayContent.mTransitionController.collectForDisplayChange(this.mDisplayContent, null);
                    }
                    this.mService.mAtmService.deferWindowLayout();
                    this.mDisplayContent.sendNewConfiguration();
                    if (t != null) {
                        this.mService.mAtmService.mWindowOrganizerController.applyTransaction(t);
                    }
                    this.mService.mAtmService.continueWindowLayout();
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void prepareNormalRotationAnimation() {
        cancelSeamlessRotation();
        RotationAnimationPair anim = selectRotationAnimation();
        this.mService.startFreezingDisplay(anim.mExit, anim.mEnter, this.mDisplayContent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cancelSeamlessRotation() {
        if (!this.mRotatingSeamlessly) {
            return;
        }
        this.mDisplayContent.forAllWindows(new Consumer() { // from class: com.android.server.wm.DisplayRotation$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DisplayRotation.lambda$cancelSeamlessRotation$2((WindowState) obj);
            }
        }, true);
        this.mSeamlessRotationCount = 0;
        this.mRotatingSeamlessly = false;
        this.mDisplayContent.finishAsyncRotationIfPossible();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$cancelSeamlessRotation$2(WindowState w) {
        if (w.mSeamlesslyRotated) {
            w.cancelSeamlessRotation();
            w.mSeamlesslyRotated = false;
        }
    }

    private void prepareSeamlessRotation() {
        this.mSeamlessRotationCount = 0;
        this.mRotatingSeamlessly = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isRotatingSeamlessly() {
        return this.mRotatingSeamlessly;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasSeamlessRotatingWindow() {
        return this.mSeamlessRotationCount > 0;
    }

    boolean shouldRotateSeamlessly(int oldRotation, int newRotation, boolean forceUpdate) {
        if (this.mDisplayContent.hasTopFixedRotationLaunchingApp()) {
            return true;
        }
        WindowState w = this.mDisplayPolicy.getTopFullscreenOpaqueWindow();
        if (w == null || w != this.mDisplayContent.mCurrentFocus || w.getAttrs().rotationAnimation != 3 || w.isAnimatingLw() || !canRotateSeamlessly(oldRotation, newRotation)) {
            return false;
        }
        if ((w.mActivityRecord != null && !w.mActivityRecord.matchParentBounds()) || this.mDisplayContent.getDefaultTaskDisplayArea().hasPinnedTask() || this.mDisplayContent.hasAlertWindowSurfaces()) {
            return false;
        }
        return forceUpdate || this.mDisplayContent.getWindow(new Predicate() { // from class: com.android.server.wm.DisplayRotation$$ExternalSyntheticLambda2
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean z;
                z = ((WindowState) obj).mSeamlesslyRotated;
                return z;
            }
        }) == null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canRotateSeamlessly(int oldRotation, int newRotation) {
        if (this.mAllowSeamlessRotationDespiteNavBarMoving || this.mDisplayPolicy.navigationBarCanMove()) {
            return true;
        }
        return (oldRotation == 2 || newRotation == 2) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void markForSeamlessRotation(WindowState w, boolean seamlesslyRotated) {
        if (seamlesslyRotated == w.mSeamlesslyRotated || w.mForceSeamlesslyRotate) {
            return;
        }
        w.mSeamlesslyRotated = seamlesslyRotated;
        if (seamlesslyRotated) {
            this.mSeamlessRotationCount++;
        } else {
            this.mSeamlessRotationCount--;
        }
        if (this.mSeamlessRotationCount == 0) {
            if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_ORIENTATION, -576070986, 0, (String) null, (Object[]) null);
            }
            this.mRotatingSeamlessly = false;
            this.mDisplayContent.finishAsyncRotationIfPossible();
            updateRotationAndSendNewConfigIfChanged();
        }
    }

    private RotationAnimationPair selectRotationAnimation() {
        boolean forceJumpcut = (this.mDisplayPolicy.isScreenOnFully() && this.mService.mPolicy.okToAnimate(false)) ? false : true;
        WindowState topFullscreen = this.mDisplayPolicy.getTopFullscreenOpaqueWindow();
        if (ProtoLogCache.WM_DEBUG_ANIM_enabled) {
            String protoLogParam0 = String.valueOf(topFullscreen);
            long protoLogParam1 = topFullscreen == null ? 0L : topFullscreen.getAttrs().rotationAnimation;
            boolean protoLogParam2 = forceJumpcut;
            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_ANIM, 2019765997, 52, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1), Boolean.valueOf(protoLogParam2)});
        }
        if (forceJumpcut) {
            this.mTmpRotationAnim.mExit = 17432718;
            this.mTmpRotationAnim.mEnter = 17432717;
            return this.mTmpRotationAnim;
        }
        if (topFullscreen != null && this.mService.isKeyguardLocked()) {
            Slog.d("WindowManager", "selectRotationAnimation keyguardLocked anim set to 0");
            RotationAnimationPair rotationAnimationPair = this.mTmpRotationAnim;
            rotationAnimationPair.mEnter = 0;
            rotationAnimationPair.mExit = 0;
        } else if (topFullscreen != null) {
            int animationHint = topFullscreen.getRotationAnimationHint();
            if (animationHint < 0 && this.mDisplayPolicy.isTopLayoutFullscreen()) {
                animationHint = topFullscreen.getAttrs().rotationAnimation;
            }
            switch (animationHint) {
                case 1:
                case 3:
                    this.mTmpRotationAnim.mExit = 17432719;
                    this.mTmpRotationAnim.mEnter = 17432717;
                    break;
                case 2:
                    this.mTmpRotationAnim.mExit = 17432718;
                    this.mTmpRotationAnim.mEnter = 17432717;
                    break;
                default:
                    RotationAnimationPair rotationAnimationPair2 = this.mTmpRotationAnim;
                    rotationAnimationPair2.mEnter = 0;
                    rotationAnimationPair2.mExit = 0;
                    break;
            }
        } else {
            RotationAnimationPair rotationAnimationPair3 = this.mTmpRotationAnim;
            rotationAnimationPair3.mEnter = 0;
            rotationAnimationPair3.mExit = 0;
        }
        return this.mTmpRotationAnim;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean validateRotationAnimation(int exitAnimId, int enterAnimId, boolean forceDefault) {
        switch (exitAnimId) {
            case 17432718:
            case 17432719:
                if (forceDefault) {
                    return false;
                }
                RotationAnimationPair anim = selectRotationAnimation();
                if (exitAnimId == anim.mExit && enterAnimId == anim.mEnter) {
                    return true;
                }
                return false;
            default:
                return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void restoreSettings(int userRotationMode, int userRotation, int fixedToUserRotation) {
        this.mFixedToUserRotation = fixedToUserRotation;
        if (this.isDefaultDisplay) {
            return;
        }
        if (userRotationMode != 0 && userRotationMode != 1) {
            Slog.w("WindowManager", "Trying to restore an invalid user rotation mode " + userRotationMode + " for " + this.mDisplayContent);
            userRotationMode = 0;
        }
        if (userRotation < 0 || userRotation > 3) {
            Slog.w("WindowManager", "Trying to restore an invalid user rotation " + userRotation + " for " + this.mDisplayContent);
            userRotation = 0;
        }
        this.mUserRotationMode = userRotationMode;
        this.mUserRotation = userRotation;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setFixedToUserRotation(int fixedToUserRotation) {
        if (this.mFixedToUserRotation == fixedToUserRotation) {
            return;
        }
        this.mFixedToUserRotation = fixedToUserRotation;
        this.mDisplayWindowSettings.setFixedToUserRotation(this.mDisplayContent, fixedToUserRotation);
        if (this.mDisplayContent.mFocusedApp != null) {
            DisplayContent displayContent = this.mDisplayContent;
            displayContent.onLastFocusedTaskDisplayAreaChanged(displayContent.mFocusedApp.getDisplayArea());
        }
        this.mDisplayContent.updateOrientation();
    }

    void setUserRotation(int userRotationMode, int userRotation) {
        if (this.isDefaultDisplay) {
            ContentResolver res = this.mContext.getContentResolver();
            int accelerometerRotation = userRotationMode != 1 ? 1 : 0;
            Settings.System.putIntForUser(res, "accelerometer_rotation", accelerometerRotation, -2);
            Settings.System.putIntForUser(res, "user_rotation", userRotation, -2);
            return;
        }
        boolean changed = false;
        if (this.mUserRotationMode != userRotationMode) {
            this.mUserRotationMode = userRotationMode;
            changed = true;
        }
        if (this.mUserRotation != userRotation) {
            this.mUserRotation = userRotation;
            changed = true;
        }
        this.mDisplayWindowSettings.setUserRotation(this.mDisplayContent, userRotationMode, userRotation);
        if (changed) {
            this.mService.updateRotation(true, false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void freezeRotation(int rotation) {
        setUserRotation(1, rotation == -1 ? this.mRotation : rotation);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void thawRotation() {
        setUserRotation(0, this.mUserRotation);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isRotationFrozen() {
        return !this.isDefaultDisplay ? this.mUserRotationMode == 1 : Settings.System.getIntForUser(this.mContext.getContentResolver(), "accelerometer_rotation", 0, -2) == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFixedToUserRotation() {
        switch (this.mFixedToUserRotation) {
            case 1:
                return false;
            case 2:
                return true;
            default:
                return this.mDefaultFixedToUserRotation;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getFixedToUserRotationMode() {
        return this.mFixedToUserRotation;
    }

    public int getLandscapeRotation() {
        return this.mLandscapeRotation;
    }

    public int getSeascapeRotation() {
        return this.mSeascapeRotation;
    }

    public int getPortraitRotation() {
        return this.mPortraitRotation;
    }

    public int getUpsideDownRotation() {
        return this.mUpsideDownRotation;
    }

    public int getCurrentAppOrientation() {
        return this.mCurrentAppOrientation;
    }

    public DisplayPolicy getDisplayPolicy() {
        return this.mDisplayPolicy;
    }

    public WindowOrientationListener getOrientationListener() {
        return this.mOrientationListener;
    }

    public int getUserRotation() {
        return this.mUserRotation;
    }

    public int getUserRotationMode() {
        return this.mUserRotationMode;
    }

    public void updateOrientationListener() {
        synchronized (this.mLock) {
            updateOrientationListenerLw();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void pause() {
        this.mDeferredRotationPauseCount++;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resume() {
        int i = this.mDeferredRotationPauseCount;
        if (i <= 0) {
            return;
        }
        int i2 = i - 1;
        this.mDeferredRotationPauseCount = i2;
        if (i2 == 0) {
            updateRotationAndSendNewConfigIfChanged();
        }
    }

    private void updateOrientationListenerLw() {
        OrientationListener orientationListener = this.mOrientationListener;
        if (orientationListener == null || !orientationListener.canDetectOrientation()) {
            return;
        }
        boolean screenOnEarly = this.mDisplayPolicy.isScreenOnEarly();
        boolean awake = this.mDisplayPolicy.isAwake();
        boolean keyguardDrawComplete = this.mDisplayPolicy.isKeyguardDrawComplete();
        boolean windowManagerDrawComplete = this.mDisplayPolicy.isWindowManagerDrawComplete();
        if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
            long protoLogParam2 = this.mCurrentAppOrientation;
            boolean protoLogParam3 = this.mOrientationListener.mEnabled;
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, -1868124841, 4063, (String) null, new Object[]{Boolean.valueOf(screenOnEarly), Boolean.valueOf(awake), Long.valueOf(protoLogParam2), Boolean.valueOf(protoLogParam3), Boolean.valueOf(keyguardDrawComplete), Boolean.valueOf(windowManagerDrawComplete)});
        }
        boolean disable = true;
        if (screenOnEarly && ((awake || this.mOrientationListener.shouldStayEnabledWhileDreaming()) && keyguardDrawComplete && windowManagerDrawComplete && needSensorRunning())) {
            disable = false;
            if (!this.mOrientationListener.mEnabled) {
                this.mOrientationListener.enable();
            }
        }
        if (disable) {
            this.mOrientationListener.disable();
        }
    }

    private boolean needSensorRunning() {
        int i;
        if (isFixedToUserRotation()) {
            return false;
        }
        if (this.mSupportAutoRotation && ((i = this.mCurrentAppOrientation) == 4 || i == 10 || i == 7 || i == 6)) {
            return true;
        }
        int dockMode = this.mDisplayPolicy.getDockMode();
        if ((this.mDisplayPolicy.isCarDockEnablesAccelerometer() && dockMode == 2) || (this.mDisplayPolicy.isDeskDockEnablesAccelerometer() && (dockMode == 1 || dockMode == 3 || dockMode == 4))) {
            return true;
        }
        if (this.mUserRotationMode == 1) {
            return this.mSupportAutoRotation && this.mShowRotationSuggestions == 1;
        }
        return this.mSupportAutoRotation;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean needsUpdate() {
        int oldRotation = this.mRotation;
        int rotation = rotationForOrientation(this.mLastOrientation, oldRotation);
        return oldRotation != rotation;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetAllowAllRotations() {
        this.mAllowAllRotations = -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int rotationForOrientation(int orientation, int lastRotation) {
        int sensorRotation;
        int preferredRotation;
        if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
            String protoLogParam0 = String.valueOf(ActivityInfo.screenOrientationToString(orientation));
            long protoLogParam1 = orientation;
            String protoLogParam2 = String.valueOf(Surface.rotationToString(lastRotation));
            long protoLogParam3 = lastRotation;
            String protoLogParam4 = String.valueOf(Surface.rotationToString(this.mUserRotation));
            long protoLogParam5 = this.mUserRotation;
            String protoLogParam6 = String.valueOf(this.mUserRotationMode == 1 ? "USER_ROTATION_LOCKED" : "");
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, 202263690, 1092, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1), protoLogParam2, Long.valueOf(protoLogParam3), protoLogParam4, Long.valueOf(protoLogParam5), protoLogParam6});
        }
        if (isFixedToUserRotation()) {
            return this.mUserRotation;
        }
        OrientationListener orientationListener = this.mOrientationListener;
        if (orientationListener != null) {
            sensorRotation = orientationListener.getProposedRotation();
        } else {
            sensorRotation = -1;
        }
        this.mLastSensorRotation = sensorRotation;
        if (sensorRotation < 0) {
            sensorRotation = lastRotation;
        }
        int lidState = this.mDisplayPolicy.getLidState();
        int dockMode = this.mDisplayPolicy.getDockMode();
        boolean hdmiPlugged = this.mDisplayPolicy.isHdmiPlugged();
        boolean carDockEnablesAccelerometer = this.mDisplayPolicy.isCarDockEnablesAccelerometer();
        boolean deskDockEnablesAccelerometer = this.mDisplayPolicy.isDeskDockEnablesAccelerometer();
        if (!this.isDefaultDisplay) {
            preferredRotation = this.mUserRotation;
        } else if (lidState == 1 && this.mLidOpenRotation >= 0) {
            preferredRotation = this.mLidOpenRotation;
        } else if (dockMode == 2 && (carDockEnablesAccelerometer || this.mCarDockRotation >= 0)) {
            preferredRotation = carDockEnablesAccelerometer ? sensorRotation : this.mCarDockRotation;
        } else if ((dockMode == 1 || dockMode == 3 || dockMode == 4) && (deskDockEnablesAccelerometer || this.mDeskDockRotation >= 0)) {
            preferredRotation = deskDockEnablesAccelerometer ? sensorRotation : this.mDeskDockRotation;
        } else if (hdmiPlugged && this.mDemoHdmiRotationLock) {
            preferredRotation = this.mDemoHdmiRotation;
        } else if (hdmiPlugged && dockMode == 0 && this.mUndockedHdmiRotation >= 0) {
            preferredRotation = this.mUndockedHdmiRotation;
        } else if (this.mDemoRotationLock) {
            preferredRotation = this.mDemoRotation;
        } else if (this.mDisplayPolicy.isPersistentVrModeEnabled()) {
            preferredRotation = this.mPortraitRotation;
        } else if (orientation == 14) {
            preferredRotation = lastRotation;
        } else if (!this.mSupportAutoRotation) {
            preferredRotation = -1;
        } else {
            int i = this.mUserRotationMode;
            if ((i == 0 && (orientation == 2 || orientation == -1 || orientation == 11 || orientation == 12 || orientation == 13)) || orientation == 4 || orientation == 10 || orientation == 6 || orientation == 7) {
                if (sensorRotation != 2 || getAllowAllRotations() == 1 || orientation == 10 || orientation == 13) {
                    preferredRotation = sensorRotation;
                } else {
                    preferredRotation = lastRotation;
                }
            } else if (i == 1 && orientation != 5 && orientation != 0 && orientation != 1 && orientation != 8 && orientation != 9) {
                preferredRotation = this.mUserRotation;
            } else {
                preferredRotation = -1;
            }
        }
        switch (orientation) {
            case 0:
                if (isLandscapeOrSeascape(preferredRotation)) {
                    return preferredRotation;
                }
                return this.mLandscapeRotation;
            case 1:
                if (isAnyPortrait(preferredRotation)) {
                    return preferredRotation;
                }
                return this.mPortraitRotation;
            case 2:
            case 3:
            case 4:
            case 5:
            case 10:
            default:
                if (preferredRotation >= 0) {
                    return preferredRotation;
                }
                return 0;
            case 6:
            case 11:
                if (isLandscapeOrSeascape(preferredRotation)) {
                    return preferredRotation;
                }
                return isLandscapeOrSeascape(lastRotation) ? lastRotation : this.mLandscapeRotation;
            case 7:
            case 12:
                if (isAnyPortrait(preferredRotation)) {
                    return preferredRotation;
                }
                return isAnyPortrait(lastRotation) ? lastRotation : this.mPortraitRotation;
            case 8:
                if (isLandscapeOrSeascape(preferredRotation)) {
                    return preferredRotation;
                }
                return this.mSeascapeRotation;
            case 9:
                if (isAnyPortrait(preferredRotation)) {
                    return preferredRotation;
                }
                return this.mUpsideDownRotation;
        }
    }

    private int getAllowAllRotations() {
        int i;
        if (this.mAllowAllRotations == -1) {
            if (this.mContext.getResources().getBoolean(17891345)) {
                i = 1;
            } else {
                i = 0;
            }
            this.mAllowAllRotations = i;
        }
        return this.mAllowAllRotations;
    }

    private boolean isLandscapeOrSeascape(int rotation) {
        return rotation == this.mLandscapeRotation || rotation == this.mSeascapeRotation;
    }

    private boolean isAnyPortrait(int rotation) {
        return rotation == this.mPortraitRotation || rotation == this.mUpsideDownRotation;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isValidRotationChoice(int preferredRotation) {
        switch (this.mCurrentAppOrientation) {
            case -1:
            case 2:
                return getAllowAllRotations() == 1 ? preferredRotation >= 0 : preferredRotation >= 0 && preferredRotation != 2;
            case 11:
                return isLandscapeOrSeascape(preferredRotation);
            case 12:
                return preferredRotation == this.mPortraitRotation;
            case 13:
                return preferredRotation >= 0;
            default:
                return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isRotationChoicePossible(int orientation) {
        int dockMode;
        if (this.mUserRotationMode == 1 && !isFixedToUserRotation()) {
            int lidState = this.mDisplayPolicy.getLidState();
            if ((lidState != 1 || this.mLidOpenRotation < 0) && (dockMode = this.mDisplayPolicy.getDockMode()) != 2) {
                boolean deskDockEnablesAccelerometer = this.mDisplayPolicy.isDeskDockEnablesAccelerometer();
                if ((dockMode == 1 || dockMode == 3 || dockMode == 4) && !deskDockEnablesAccelerometer) {
                    return false;
                }
                boolean hdmiPlugged = this.mDisplayPolicy.isHdmiPlugged();
                if (hdmiPlugged && this.mDemoHdmiRotationLock) {
                    return false;
                }
                if ((hdmiPlugged && dockMode == 0 && this.mUndockedHdmiRotation >= 0) || this.mDemoRotationLock || this.mDisplayPolicy.isPersistentVrModeEnabled() || !this.mSupportAutoRotation) {
                    return false;
                }
                switch (orientation) {
                    case -1:
                    case 2:
                    case 11:
                    case 12:
                    case 13:
                        return true;
                    default:
                        return false;
                }
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendProposedRotationChangeToStatusBarInternal(int rotation, boolean isValid) {
        if (this.mStatusBarManagerInternal == null) {
            this.mStatusBarManagerInternal = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
        }
        StatusBarManagerInternal statusBarManagerInternal = this.mStatusBarManagerInternal;
        if (statusBarManagerInternal != null) {
            statusBarManagerInternal.onProposedRotationChanged(rotation, isValid);
        }
    }

    private static String allowAllRotationsToString(int allowAll) {
        switch (allowAll) {
            case -1:
                return UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
            case 0:
                return "false";
            case 1:
                return "true";
            default:
                return Integer.toString(allowAll);
        }
    }

    public void onUserSwitch() {
        SettingsObserver settingsObserver = this.mSettingsObserver;
        if (settingsObserver != null) {
            settingsObserver.onChange(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean updateSettings() {
        int showRotationSuggestions;
        ContentResolver resolver = this.mContext.getContentResolver();
        boolean shouldUpdateRotation = false;
        synchronized (this.mLock) {
            boolean shouldUpdateOrientationListener = false;
            if (ActivityManager.isLowRamDeviceStatic()) {
                showRotationSuggestions = 0;
            } else {
                showRotationSuggestions = Settings.Secure.getIntForUser(resolver, "show_rotation_suggestions", 1, -2);
            }
            if (this.mShowRotationSuggestions != showRotationSuggestions) {
                this.mShowRotationSuggestions = showRotationSuggestions;
                shouldUpdateOrientationListener = true;
            }
            int userRotation = Settings.System.getIntForUser(resolver, "user_rotation", 0, -2);
            if (this.mUserRotation != userRotation) {
                this.mUserRotation = userRotation;
                shouldUpdateRotation = true;
            }
            int userRotationMode = Settings.System.getIntForUser(resolver, "accelerometer_rotation", 0, -2) != 0 ? 0 : 1;
            if (this.mUserRotationMode != userRotationMode) {
                this.mUserRotationMode = userRotationMode;
                shouldUpdateOrientationListener = true;
                shouldUpdateRotation = true;
            }
            if (shouldUpdateOrientationListener) {
                updateOrientationListenerLw();
            }
            int cameraRotationMode = Settings.Secure.getIntForUser(resolver, "camera_autorotate", 0, -2);
            if (this.mCameraRotationMode != cameraRotationMode) {
                this.mCameraRotationMode = cameraRotationMode;
                shouldUpdateRotation = true;
            }
        }
        return shouldUpdateRotation;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(String prefix, PrintWriter pw) {
        pw.println(prefix + "DisplayRotation");
        pw.println(prefix + "  mCurrentAppOrientation=" + ActivityInfo.screenOrientationToString(this.mCurrentAppOrientation));
        pw.println(prefix + "  mLastOrientation=" + this.mLastOrientation);
        pw.print(prefix + "  mRotation=" + this.mRotation);
        pw.println(" mDeferredRotationPauseCount=" + this.mDeferredRotationPauseCount);
        pw.print(prefix + "  mLandscapeRotation=" + Surface.rotationToString(this.mLandscapeRotation));
        pw.println(" mSeascapeRotation=" + Surface.rotationToString(this.mSeascapeRotation));
        pw.print(prefix + "  mPortraitRotation=" + Surface.rotationToString(this.mPortraitRotation));
        pw.println(" mUpsideDownRotation=" + Surface.rotationToString(this.mUpsideDownRotation));
        pw.println(prefix + "  mSupportAutoRotation=" + this.mSupportAutoRotation);
        OrientationListener orientationListener = this.mOrientationListener;
        if (orientationListener != null) {
            orientationListener.dump(pw, prefix + "  ");
        }
        pw.println();
        pw.print(prefix + "  mCarDockRotation=" + Surface.rotationToString(this.mCarDockRotation));
        pw.println(" mDeskDockRotation=" + Surface.rotationToString(this.mDeskDockRotation));
        pw.print(prefix + "  mUserRotationMode=" + WindowManagerPolicy.userRotationModeToString(this.mUserRotationMode));
        pw.print(" mUserRotation=" + Surface.rotationToString(this.mUserRotation));
        pw.print(" mCameraRotationMode=" + this.mCameraRotationMode);
        pw.println(" mAllowAllRotations=" + allowAllRotationsToString(this.mAllowAllRotations));
        pw.print(prefix + "  mDemoHdmiRotation=" + Surface.rotationToString(this.mDemoHdmiRotation));
        pw.print(" mDemoHdmiRotationLock=" + this.mDemoHdmiRotationLock);
        pw.println(" mUndockedHdmiRotation=" + Surface.rotationToString(this.mUndockedHdmiRotation));
        pw.println(prefix + "  mLidOpenRotation=" + Surface.rotationToString(this.mLidOpenRotation));
        pw.println(prefix + "  mFixedToUserRotation=" + isFixedToUserRotation());
        if (!this.mRotationHistory.mRecords.isEmpty()) {
            pw.println();
            pw.println(prefix + "  RotationHistory");
            String prefix2 = "    " + prefix;
            Iterator<RotationHistory.Record> it = this.mRotationHistory.mRecords.iterator();
            while (it.hasNext()) {
                RotationHistory.Record r = it.next();
                r.dump(prefix2, pw);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(CompanionMessage.MESSAGE_ID, getRotation());
        proto.write(1133871366146L, isRotationFrozen());
        proto.write(1120986464259L, getUserRotation());
        proto.write(1120986464260L, this.mFixedToUserRotation);
        proto.write(1120986464261L, this.mLastOrientation);
        proto.write(1133871366150L, isFixedToUserRotation());
        proto.end(token);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class OrientationListener extends WindowOrientationListener implements Runnable {
        transient boolean mEnabled;

        OrientationListener(Context context, Handler handler) {
            super(context, handler);
        }

        @Override // com.android.server.wm.WindowOrientationListener
        public boolean isKeyguardLocked() {
            return DisplayRotation.this.mService.isKeyguardLocked();
        }

        @Override // com.android.server.wm.WindowOrientationListener
        public boolean isRotationResolverEnabled() {
            return DisplayRotation.this.mUserRotationMode == 0 && DisplayRotation.this.mCameraRotationMode == 1 && !DisplayRotation.this.mService.mPowerManager.isPowerSaveMode();
        }

        @Override // com.android.server.wm.WindowOrientationListener
        public void onProposedRotationChanged(int rotation) {
            if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                long protoLogParam0 = rotation;
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, 2128917433, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0)});
            }
            DisplayRotation.this.mService.mPowerManagerInternal.setPowerBoost(0, 0);
            DisplayRotation displayRotation = DisplayRotation.this;
            if (displayRotation.isRotationChoicePossible(displayRotation.mCurrentAppOrientation)) {
                boolean isValid = DisplayRotation.this.isValidRotationChoice(rotation);
                DisplayRotation.this.sendProposedRotationChangeToStatusBarInternal(rotation, isValid);
                return;
            }
            DisplayRotation.this.mService.updateRotation(false, false);
        }

        @Override // com.android.server.wm.WindowOrientationListener
        public void enable() {
            this.mEnabled = true;
            getHandler().post(this);
            if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, -1568331821, 0, (String) null, (Object[]) null);
            }
        }

        @Override // com.android.server.wm.WindowOrientationListener
        public void disable() {
            this.mEnabled = false;
            getHandler().post(this);
            if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, -439951996, 0, (String) null, (Object[]) null);
            }
        }

        @Override // java.lang.Runnable
        public void run() {
            if (this.mEnabled) {
                super.enable();
            } else {
                super.disable();
            }
        }
    }

    /* loaded from: classes2.dex */
    private class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = DisplayRotation.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.Secure.getUriFor("show_rotation_suggestions"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("accelerometer_rotation"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("user_rotation"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("camera_autorotate"), false, this, -1);
            DisplayRotation.this.updateSettings();
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            if (DisplayRotation.this.updateSettings()) {
                DisplayRotation.this.mService.updateRotation(true, false);
            }
        }
    }

    /* loaded from: classes2.dex */
    private static class RotationHistory {
        private static final int MAX_SIZE = 8;
        final ArrayDeque<Record> mRecords;

        private RotationHistory() {
            this.mRecords = new ArrayDeque<>(8);
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public static class Record {
            final int mFromRotation;
            final boolean mIgnoreOrientationRequest;
            final String mLastOrientationSource;
            final String mNonDefaultRequestingTaskDisplayArea;
            final int mSensorRotation;
            final int mSourceOrientation;
            final long mTimestamp = System.currentTimeMillis();
            final int mToRotation;
            final int mUserRotation;
            final int mUserRotationMode;

            Record(DisplayRotation dr, int fromRotation, int toRotation) {
                int i;
                String str;
                this.mFromRotation = fromRotation;
                this.mToRotation = toRotation;
                this.mUserRotation = dr.mUserRotation;
                this.mUserRotationMode = dr.mUserRotationMode;
                OrientationListener listener = dr.mOrientationListener;
                if (listener == null || !listener.mEnabled) {
                    i = -2;
                } else {
                    i = dr.mLastSensorRotation;
                }
                this.mSensorRotation = i;
                DisplayContent dc = dr.mDisplayContent;
                this.mIgnoreOrientationRequest = dc.getIgnoreOrientationRequest();
                TaskDisplayArea requestingTda = dc.getOrientationRequestingTaskDisplayArea();
                if (requestingTda == null) {
                    str = "none";
                } else if (requestingTda == dc.getDefaultTaskDisplayArea()) {
                    str = null;
                } else {
                    str = requestingTda.toString();
                }
                this.mNonDefaultRequestingTaskDisplayArea = str;
                WindowContainer<?> source = dc.getLastOrientationSource();
                if (source != null) {
                    this.mLastOrientationSource = source.toString();
                    this.mSourceOrientation = source.mOrientation;
                    return;
                }
                this.mLastOrientationSource = null;
                this.mSourceOrientation = -2;
            }

            void dump(String prefix, PrintWriter pw) {
                pw.println(prefix + TimeUtils.logTimeOfDay(this.mTimestamp) + " " + Surface.rotationToString(this.mFromRotation) + " to " + Surface.rotationToString(this.mToRotation));
                pw.println(prefix + "  source=" + this.mLastOrientationSource + " " + ActivityInfo.screenOrientationToString(this.mSourceOrientation));
                pw.println(prefix + "  mode=" + WindowManagerPolicy.userRotationModeToString(this.mUserRotationMode) + " user=" + Surface.rotationToString(this.mUserRotation) + " sensor=" + Surface.rotationToString(this.mSensorRotation));
                if (this.mIgnoreOrientationRequest) {
                    pw.println(prefix + "  ignoreRequest=true");
                }
                if (this.mNonDefaultRequestingTaskDisplayArea != null) {
                    pw.println(prefix + "  requestingTda=" + this.mNonDefaultRequestingTaskDisplayArea);
                }
            }
        }

        void addRecord(DisplayRotation dr, int toRotation) {
            if (this.mRecords.size() >= 8) {
                this.mRecords.removeFirst();
            }
            int fromRotation = dr.mDisplayContent.getWindowConfiguration().getRotation();
            this.mRecords.addLast(new Record(dr, fromRotation, toRotation));
        }
    }
}
