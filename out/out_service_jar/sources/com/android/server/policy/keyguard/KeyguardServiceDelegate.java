package com.android.server.policy.keyguard;

import android.app.ActivityTaskManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.WindowManagerPolicyConstants;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.policy.IKeyguardDrawnCallback;
import com.android.internal.policy.IKeyguardExitCallback;
import com.android.internal.policy.IKeyguardService;
import com.android.server.UiThread;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.policy.keyguard.KeyguardStateMonitor;
import com.mediatek.server.MtkSystemServer;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
public class KeyguardServiceDelegate {
    private static final boolean DEBUG = false;
    private static final int INTERACTIVE_STATE_AWAKE = 2;
    private static final int INTERACTIVE_STATE_GOING_TO_SLEEP = 3;
    private static final int INTERACTIVE_STATE_SLEEP = 0;
    private static final int INTERACTIVE_STATE_WAKING = 1;
    private static final int SCREEN_STATE_OFF = 0;
    private static final int SCREEN_STATE_ON = 2;
    private static final int SCREEN_STATE_TURNING_OFF = 3;
    private static final int SCREEN_STATE_TURNING_ON = 1;
    private static final String TAG = "KeyguardServiceDelegate";
    private static MtkSystemServer sMtkSystemServer = MtkSystemServer.getInstance();
    private final KeyguardStateMonitor.StateCallback mCallback;
    private final Context mContext;
    private DrawnListener mDrawnListenerWhenConnect;
    protected KeyguardServiceWrapper mKeyguardService;
    private final KeyguardState mKeyguardState = new KeyguardState();
    private final ServiceConnection mKeyguardConnection = new AnonymousClass1();
    private final Handler mHandler = UiThread.getHandler();

    /* loaded from: classes2.dex */
    public interface DrawnListener {
        void onDrawn();
    }

    /* loaded from: classes2.dex */
    private static final class KeyguardState {
        public boolean bootCompleted;
        public int currentUser;
        boolean deviceHasKeyguard;
        boolean dreaming;
        public boolean enabled;
        boolean inputRestricted;
        public int interactiveState;
        volatile boolean occluded;
        public int offReason;
        public int screenState;
        boolean secure;
        boolean showing;
        boolean showingAndNotOccluded;
        boolean systemIsReady;

        KeyguardState() {
            reset();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void reset() {
            this.showing = true;
            this.showingAndNotOccluded = true;
            this.secure = true;
            this.deviceHasKeyguard = true;
            this.enabled = true;
            this.currentUser = -10000;
        }
    }

    /* loaded from: classes2.dex */
    private final class KeyguardShowDelegate extends IKeyguardDrawnCallback.Stub {
        private DrawnListener mDrawnListener;

        KeyguardShowDelegate(DrawnListener drawnListener) {
            this.mDrawnListener = drawnListener;
        }

        public void onDrawn() throws RemoteException {
            KeyguardServiceDelegate.sMtkSystemServer.addBootEvent("Keyguard onDrawn");
            DrawnListener drawnListener = this.mDrawnListener;
            if (drawnListener != null) {
                drawnListener.onDrawn();
            }
        }
    }

    /* loaded from: classes2.dex */
    private final class KeyguardExitDelegate extends IKeyguardExitCallback.Stub {
        private WindowManagerPolicy.OnKeyguardExitResult mOnKeyguardExitResult;

        KeyguardExitDelegate(WindowManagerPolicy.OnKeyguardExitResult onKeyguardExitResult) {
            this.mOnKeyguardExitResult = onKeyguardExitResult;
        }

        public void onKeyguardExitResult(boolean success) throws RemoteException {
            WindowManagerPolicy.OnKeyguardExitResult onKeyguardExitResult = this.mOnKeyguardExitResult;
            if (onKeyguardExitResult != null) {
                onKeyguardExitResult.onKeyguardExitResult(success);
            }
        }
    }

    public KeyguardServiceDelegate(Context context, KeyguardStateMonitor.StateCallback callback) {
        this.mContext = context;
        this.mCallback = callback;
    }

    public void bindService(Context context) {
        Intent intent = new Intent();
        Resources resources = context.getApplicationContext().getResources();
        ComponentName keyguardComponent = ComponentName.unflattenFromString(resources.getString(17039991));
        intent.addFlags(256);
        intent.setComponent(keyguardComponent);
        sMtkSystemServer.addBootEvent("Keyguard bindService");
        if (!context.bindServiceAsUser(intent, this.mKeyguardConnection, 1, this.mHandler, UserHandle.SYSTEM)) {
            Log.v(TAG, "*** Keyguard: can't bind to " + keyguardComponent);
            this.mKeyguardState.showing = false;
            this.mKeyguardState.showingAndNotOccluded = false;
            this.mKeyguardState.secure = false;
            synchronized (this.mKeyguardState) {
                this.mKeyguardState.deviceHasKeyguard = false;
            }
            return;
        }
        sMtkSystemServer.addBootEvent("Keyguard started");
    }

    /* renamed from: com.android.server.policy.keyguard.KeyguardServiceDelegate$1  reason: invalid class name */
    /* loaded from: classes2.dex */
    class AnonymousClass1 implements ServiceConnection {
        AnonymousClass1() {
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            KeyguardServiceDelegate.sMtkSystemServer.addBootEvent("Keyguard connected");
            KeyguardServiceDelegate keyguardServiceDelegate = KeyguardServiceDelegate.this;
            keyguardServiceDelegate.mKeyguardService = new KeyguardServiceWrapper(keyguardServiceDelegate.mContext, IKeyguardService.Stub.asInterface(service), KeyguardServiceDelegate.this.mCallback);
            if (KeyguardServiceDelegate.this.mKeyguardState.systemIsReady) {
                KeyguardServiceDelegate.this.mKeyguardService.onSystemReady();
                if (KeyguardServiceDelegate.this.mKeyguardState.currentUser != -10000) {
                    KeyguardServiceDelegate.this.mKeyguardService.setCurrentUser(KeyguardServiceDelegate.this.mKeyguardState.currentUser);
                }
                if (KeyguardServiceDelegate.this.mKeyguardState.interactiveState == 2 || KeyguardServiceDelegate.this.mKeyguardState.interactiveState == 1) {
                    KeyguardServiceDelegate.this.mKeyguardService.onStartedWakingUp(0, false);
                }
                if (KeyguardServiceDelegate.this.mKeyguardState.interactiveState == 2) {
                    KeyguardServiceDelegate.this.mKeyguardService.onFinishedWakingUp();
                }
                if (KeyguardServiceDelegate.this.mKeyguardState.screenState == 2 || KeyguardServiceDelegate.this.mKeyguardState.screenState == 1) {
                    KeyguardServiceWrapper keyguardServiceWrapper = KeyguardServiceDelegate.this.mKeyguardService;
                    KeyguardServiceDelegate keyguardServiceDelegate2 = KeyguardServiceDelegate.this;
                    keyguardServiceWrapper.onScreenTurningOn(new KeyguardShowDelegate(keyguardServiceDelegate2.mDrawnListenerWhenConnect));
                }
                if (KeyguardServiceDelegate.this.mKeyguardState.screenState == 2) {
                    KeyguardServiceDelegate.this.mKeyguardService.onScreenTurnedOn();
                }
                KeyguardServiceDelegate.this.mDrawnListenerWhenConnect = null;
            }
            if (KeyguardServiceDelegate.this.mKeyguardState.bootCompleted) {
                KeyguardServiceDelegate.this.mKeyguardService.onBootCompleted();
            }
            if (KeyguardServiceDelegate.this.mKeyguardState.occluded) {
                if (Build.IS_DEBUG_ENABLE) {
                    Slog.d(KeyguardServiceDelegate.TAG, "onServiceConnected setOccluded mKeyguardState.occluded = " + KeyguardServiceDelegate.this.mKeyguardState.occluded);
                }
                KeyguardServiceDelegate.this.mKeyguardService.setOccluded(KeyguardServiceDelegate.this.mKeyguardState.occluded, false);
            }
            if (!KeyguardServiceDelegate.this.mKeyguardState.enabled) {
                KeyguardServiceDelegate.this.mKeyguardService.setKeyguardEnabled(KeyguardServiceDelegate.this.mKeyguardState.enabled);
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            KeyguardServiceDelegate.this.mKeyguardService = null;
            KeyguardServiceDelegate.this.mKeyguardState.reset();
            KeyguardServiceDelegate.this.mHandler.post(new Runnable() { // from class: com.android.server.policy.keyguard.KeyguardServiceDelegate$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ActivityTaskManager.getService().setLockScreenShown(true, false);
                }
            });
        }
    }

    public boolean isShowing() {
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            this.mKeyguardState.showing = keyguardServiceWrapper.isShowing();
        }
        return this.mKeyguardState.showing;
    }

    public boolean isTrusted() {
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            return keyguardServiceWrapper.isTrusted();
        }
        return false;
    }

    public boolean hasKeyguard() {
        return this.mKeyguardState.deviceHasKeyguard;
    }

    public boolean isInputRestricted() {
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            this.mKeyguardState.inputRestricted = keyguardServiceWrapper.isInputRestricted();
        }
        return this.mKeyguardState.inputRestricted;
    }

    public void verifyUnlock(WindowManagerPolicy.OnKeyguardExitResult onKeyguardExitResult) {
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            keyguardServiceWrapper.verifyUnlock(new KeyguardExitDelegate(onKeyguardExitResult));
        }
    }

    public void setOccluded(boolean isOccluded, boolean animate, boolean notify) {
        if (this.mKeyguardService != null && notify) {
            if (Build.IS_DEBUG_ENABLE) {
                Log.v(TAG, "setOccluded(" + isOccluded + ") animate=" + animate);
            }
            this.mKeyguardService.setOccluded(isOccluded, animate);
        }
        this.mKeyguardState.occluded = isOccluded;
    }

    public boolean isOccluded() {
        return this.mKeyguardState.occluded;
    }

    public void dismiss(IKeyguardDismissCallback callback, CharSequence message) {
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            keyguardServiceWrapper.dismiss(callback, message);
        }
    }

    public boolean isSecure(int userId) {
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            this.mKeyguardState.secure = keyguardServiceWrapper.isSecure(userId);
        }
        return this.mKeyguardState.secure;
    }

    public void onDreamingStarted() {
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            keyguardServiceWrapper.onDreamingStarted();
        }
        this.mKeyguardState.dreaming = true;
    }

    public void onDreamingStopped() {
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            keyguardServiceWrapper.onDreamingStopped();
        }
        this.mKeyguardState.dreaming = false;
    }

    public void onStartedWakingUp(int pmWakeReason, boolean cameraGestureTriggered) {
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            keyguardServiceWrapper.onStartedWakingUp(pmWakeReason, cameraGestureTriggered);
        }
        this.mKeyguardState.interactiveState = 1;
    }

    public void onFinishedWakingUp() {
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            keyguardServiceWrapper.onFinishedWakingUp();
        }
        this.mKeyguardState.interactiveState = 2;
    }

    public void onScreenTurningOff() {
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            keyguardServiceWrapper.onScreenTurningOff();
        }
        this.mKeyguardState.screenState = 3;
    }

    public void onScreenTurnedOff() {
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            keyguardServiceWrapper.onScreenTurnedOff();
        }
        this.mKeyguardState.screenState = 0;
    }

    public void onScreenTurningOn(DrawnListener drawnListener) {
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            keyguardServiceWrapper.onScreenTurningOn(new KeyguardShowDelegate(drawnListener));
        } else {
            Slog.w(TAG, "onScreenTurningOn(): no keyguard service!");
            this.mDrawnListenerWhenConnect = drawnListener;
        }
        this.mKeyguardState.screenState = 1;
    }

    public void onScreenTurnedOn() {
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            keyguardServiceWrapper.onScreenTurnedOn();
        }
        this.mKeyguardState.screenState = 2;
    }

    public void onStartedGoingToSleep(int pmSleepReason) {
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            keyguardServiceWrapper.onStartedGoingToSleep(pmSleepReason);
        }
        this.mKeyguardState.offReason = WindowManagerPolicyConstants.translateSleepReasonToOffReason(pmSleepReason);
        this.mKeyguardState.interactiveState = 3;
    }

    public void onFinishedGoingToSleep(int pmSleepReason, boolean cameraGestureTriggered) {
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            keyguardServiceWrapper.onFinishedGoingToSleep(pmSleepReason, cameraGestureTriggered);
        }
        this.mKeyguardState.interactiveState = 0;
    }

    public void setKeyguardEnabled(boolean enabled) {
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            keyguardServiceWrapper.setKeyguardEnabled(enabled);
        }
        this.mKeyguardState.enabled = enabled;
    }

    public void onSystemReady() {
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            keyguardServiceWrapper.onSystemReady();
        } else {
            this.mKeyguardState.systemIsReady = true;
        }
    }

    public void doKeyguardTimeout(Bundle options) {
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            keyguardServiceWrapper.doKeyguardTimeout(options);
        }
    }

    public void setCurrentUser(int newUserId) {
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            keyguardServiceWrapper.setCurrentUser(newUserId);
        }
        this.mKeyguardState.currentUser = newUserId;
    }

    public void setSwitchingUser(boolean switching) {
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            keyguardServiceWrapper.setSwitchingUser(switching);
        }
    }

    public void startKeyguardExitAnimation(long startTime, long fadeoutDuration) {
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            keyguardServiceWrapper.startKeyguardExitAnimation(startTime, fadeoutDuration);
        }
    }

    public void onBootCompleted() {
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            keyguardServiceWrapper.onBootCompleted();
        }
        this.mKeyguardState.bootCompleted = true;
    }

    public void onShortPowerPressedGoHome() {
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            keyguardServiceWrapper.onShortPowerPressedGoHome();
        }
    }

    public void dismissKeyguardToLaunch(Intent intentToLaunch) {
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            keyguardServiceWrapper.dismissKeyguardToLaunch(intentToLaunch);
        }
    }

    public void onSystemKeyPressed(int keycode) {
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            keyguardServiceWrapper.onSystemKeyPressed(keycode);
        }
    }

    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1133871366145L, this.mKeyguardState.showing);
        proto.write(1133871366146L, this.mKeyguardState.occluded);
        proto.write(1133871366147L, this.mKeyguardState.secure);
        proto.write(1159641169924L, this.mKeyguardState.screenState);
        proto.write(1159641169925L, this.mKeyguardState.interactiveState);
        proto.end(token);
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.println(prefix + TAG);
        String prefix2 = prefix + "  ";
        pw.println(prefix2 + "showing=" + this.mKeyguardState.showing);
        pw.println(prefix2 + "showingAndNotOccluded=" + this.mKeyguardState.showingAndNotOccluded);
        pw.println(prefix2 + "inputRestricted=" + this.mKeyguardState.inputRestricted);
        pw.println(prefix2 + "occluded=" + this.mKeyguardState.occluded);
        pw.println(prefix2 + "secure=" + this.mKeyguardState.secure);
        pw.println(prefix2 + "dreaming=" + this.mKeyguardState.dreaming);
        pw.println(prefix2 + "systemIsReady=" + this.mKeyguardState.systemIsReady);
        pw.println(prefix2 + "deviceHasKeyguard=" + this.mKeyguardState.deviceHasKeyguard);
        pw.println(prefix2 + "enabled=" + this.mKeyguardState.enabled);
        pw.println(prefix2 + "offReason=" + WindowManagerPolicyConstants.offReasonToString(this.mKeyguardState.offReason));
        pw.println(prefix2 + "currentUser=" + this.mKeyguardState.currentUser);
        pw.println(prefix2 + "bootCompleted=" + this.mKeyguardState.bootCompleted);
        pw.println(prefix2 + "screenState=" + screenStateToString(this.mKeyguardState.screenState));
        pw.println(prefix2 + "interactiveState=" + interactiveStateToString(this.mKeyguardState.interactiveState));
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            keyguardServiceWrapper.dump(prefix2, pw);
        }
    }

    private static String screenStateToString(int screen) {
        switch (screen) {
            case 0:
                return "SCREEN_STATE_OFF";
            case 1:
                return "SCREEN_STATE_TURNING_ON";
            case 2:
                return "SCREEN_STATE_ON";
            case 3:
                return "SCREEN_STATE_TURNING_OFF";
            default:
                return Integer.toString(screen);
        }
    }

    private static String interactiveStateToString(int interactive) {
        switch (interactive) {
            case 0:
                return "INTERACTIVE_STATE_SLEEP";
            case 1:
                return "INTERACTIVE_STATE_WAKING";
            case 2:
                return "INTERACTIVE_STATE_AWAKE";
            case 3:
                return "INTERACTIVE_STATE_GOING_TO_SLEEP";
            default:
                return Integer.toString(interactive);
        }
    }

    public void onStartedNotifyFaceunlock(int reason) {
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            keyguardServiceWrapper.onStartedNotifyFaceunlock(reason);
        }
    }

    public void onStartFaceUnlock() {
        KeyguardServiceWrapper keyguardServiceWrapper = this.mKeyguardService;
        if (keyguardServiceWrapper != null) {
            keyguardServiceWrapper.onStartFaceUnlock();
        }
    }
}
