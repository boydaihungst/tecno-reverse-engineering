package com.android.server.wm;

import android.graphics.Point;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import android.view.DisplayInfo;
import android.view.IWindow;
import android.view.SurfaceControl;
import android.view.animation.Animation;
import com.android.server.job.controllers.JobStatus;
import com.android.server.wm.WindowToken;
/* loaded from: classes2.dex */
public class ShellRoot {
    private static final String TAG = "ShellRoot";
    private IWindow mAccessibilityWindow;
    private IBinder.DeathRecipient mAccessibilityWindowDeath;
    private IWindow mClient;
    private final IBinder.DeathRecipient mDeathRecipient;
    private final DisplayContent mDisplayContent;
    private final int mShellRootLayer;
    private SurfaceControl mSurfaceControl;
    private WindowToken mToken;
    private int mWindowType;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ShellRoot(IWindow client, DisplayContent dc, final int shellRootLayer) {
        this.mSurfaceControl = null;
        this.mDisplayContent = dc;
        this.mShellRootLayer = shellRootLayer;
        IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() { // from class: com.android.server.wm.ShellRoot$$ExternalSyntheticLambda0
            @Override // android.os.IBinder.DeathRecipient
            public final void binderDied() {
                ShellRoot.this.m8227lambda$new$0$comandroidserverwmShellRoot(shellRootLayer);
            }
        };
        this.mDeathRecipient = deathRecipient;
        try {
            client.asBinder().linkToDeath(deathRecipient, 0);
            this.mClient = client;
            switch (shellRootLayer) {
                case 0:
                    this.mWindowType = 2034;
                    break;
                case 1:
                    this.mWindowType = 2038;
                    break;
                default:
                    throw new IllegalArgumentException(shellRootLayer + " is not an acceptable shell root layer.");
            }
            WindowToken build = new WindowToken.Builder(dc.mWmService, client.asBinder(), this.mWindowType).setDisplayContent(dc).setPersistOnEmpty(true).setOwnerCanManageAppTokens(true).build();
            this.mToken = build;
            this.mSurfaceControl = build.makeChildSurface(null).setContainerLayer().setName("Shell Root Leash " + dc.getDisplayId()).setCallsite(TAG).build();
            this.mToken.getPendingTransaction().show(this.mSurfaceControl);
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to add shell root layer " + shellRootLayer + " on display " + dc.getDisplayId(), e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-wm-ShellRoot  reason: not valid java name */
    public /* synthetic */ void m8227lambda$new$0$comandroidserverwmShellRoot(int shellRootLayer) {
        this.mDisplayContent.removeShellRoot(shellRootLayer);
    }

    int getWindowType() {
        return this.mWindowType;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clear() {
        IWindow iWindow = this.mClient;
        if (iWindow != null) {
            iWindow.asBinder().unlinkToDeath(this.mDeathRecipient, 0);
            this.mClient = null;
        }
        WindowToken windowToken = this.mToken;
        if (windowToken != null) {
            windowToken.removeImmediately();
            this.mToken = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SurfaceControl getSurfaceControl() {
        return this.mSurfaceControl;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IWindow getClient() {
        return this.mClient;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startAnimation(Animation anim) {
        if (this.mToken.windowType != 2034) {
            return;
        }
        DisplayInfo displayInfo = this.mToken.getFixedRotationTransformDisplayInfo();
        if (displayInfo == null) {
            displayInfo = this.mDisplayContent.getDisplayInfo();
        }
        anim.initialize(displayInfo.logicalWidth, displayInfo.logicalHeight, displayInfo.appWidth, displayInfo.appHeight);
        anim.restrictDuration(JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        anim.scaleCurrentDuration(this.mDisplayContent.mWmService.getWindowAnimationScaleLocked());
        AnimationAdapter adapter = new LocalAnimationAdapter(new WindowAnimationSpec(anim, new Point(0, 0), false, 0.0f), this.mDisplayContent.mWmService.mSurfaceAnimationRunner);
        WindowToken windowToken = this.mToken;
        windowToken.startAnimation(windowToken.getPendingTransaction(), adapter, false, 16);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IBinder getAccessibilityWindowToken() {
        IWindow iWindow = this.mAccessibilityWindow;
        if (iWindow != null) {
            return iWindow.asBinder();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAccessibilityWindow(IWindow window) {
        IWindow iWindow = this.mAccessibilityWindow;
        if (iWindow != null) {
            iWindow.asBinder().unlinkToDeath(this.mAccessibilityWindowDeath, 0);
        }
        this.mAccessibilityWindow = window;
        if (window != null) {
            try {
                this.mAccessibilityWindowDeath = new IBinder.DeathRecipient() { // from class: com.android.server.wm.ShellRoot$$ExternalSyntheticLambda1
                    @Override // android.os.IBinder.DeathRecipient
                    public final void binderDied() {
                        ShellRoot.this.m8228lambda$setAccessibilityWindow$1$comandroidserverwmShellRoot();
                    }
                };
                this.mAccessibilityWindow.asBinder().linkToDeath(this.mAccessibilityWindowDeath, 0);
            } catch (RemoteException e) {
                this.mAccessibilityWindow = null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setAccessibilityWindow$1$com-android-server-wm-ShellRoot  reason: not valid java name */
    public /* synthetic */ void m8228lambda$setAccessibilityWindow$1$comandroidserverwmShellRoot() {
        synchronized (this.mDisplayContent.mWmService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mAccessibilityWindow = null;
                setAccessibilityWindow(null);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }
}
