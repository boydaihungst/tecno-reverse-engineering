package com.android.server.policy;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.devicestate.DeviceStateManager;
import android.hardware.display.DisplayManagerInternal;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.view.DisplayInfo;
import android.view.IDisplayFoldListener;
import com.android.server.DisplayThread;
import com.android.server.LocalServices;
import com.android.server.wm.WindowManagerInternal;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class DisplayFoldController {
    private final int mDisplayId;
    private final DisplayManagerInternal mDisplayManagerInternal;
    private String mFocusedApp;
    private Boolean mFolded;
    private final Rect mFoldedArea;
    private final Handler mHandler;
    private final WindowManagerInternal mWindowManagerInternal;
    private Rect mOverrideFoldedArea = new Rect();
    private final DisplayInfo mNonOverrideDisplayInfo = new DisplayInfo();
    private final RemoteCallbackList<IDisplayFoldListener> mListeners = new RemoteCallbackList<>();
    private final DisplayFoldDurationLogger mDurationLogger = new DisplayFoldDurationLogger();

    DisplayFoldController(Context context, WindowManagerInternal windowManagerInternal, DisplayManagerInternal displayManagerInternal, int displayId, Rect foldedArea, Handler handler) {
        this.mWindowManagerInternal = windowManagerInternal;
        this.mDisplayManagerInternal = displayManagerInternal;
        this.mDisplayId = displayId;
        this.mFoldedArea = new Rect(foldedArea);
        this.mHandler = handler;
        DeviceStateManager deviceStateManager = (DeviceStateManager) context.getSystemService(DeviceStateManager.class);
        deviceStateManager.registerCallback(new HandlerExecutor(handler), new DeviceStateManager.FoldStateListener(context, new Consumer() { // from class: com.android.server.policy.DisplayFoldController$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DisplayFoldController.this.m5871lambda$new$0$comandroidserverpolicyDisplayFoldController((Boolean) obj);
            }
        }));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-policy-DisplayFoldController  reason: not valid java name */
    public /* synthetic */ void m5871lambda$new$0$comandroidserverpolicyDisplayFoldController(Boolean folded) {
        setDeviceFolded(folded.booleanValue());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void finishedGoingToSleep() {
        this.mDurationLogger.onFinishedGoingToSleep();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void finishedWakingUp() {
        this.mDurationLogger.onFinishedWakingUp(this.mFolded);
    }

    private void setDeviceFolded(boolean folded) {
        Rect foldedArea;
        Boolean bool = this.mFolded;
        if (bool != null && bool.booleanValue() == folded) {
            return;
        }
        if (!this.mOverrideFoldedArea.isEmpty()) {
            foldedArea = this.mOverrideFoldedArea;
        } else {
            Rect foldedArea2 = this.mFoldedArea;
            if (!foldedArea2.isEmpty()) {
                foldedArea = this.mFoldedArea;
            } else {
                foldedArea = null;
            }
        }
        if (foldedArea != null) {
            if (folded) {
                this.mDisplayManagerInternal.getNonOverrideDisplayInfo(this.mDisplayId, this.mNonOverrideDisplayInfo);
                int dx = ((this.mNonOverrideDisplayInfo.logicalWidth - foldedArea.width()) / 2) - foldedArea.left;
                int dy = ((this.mNonOverrideDisplayInfo.logicalHeight - foldedArea.height()) / 2) - foldedArea.top;
                this.mDisplayManagerInternal.setDisplayScalingDisabled(this.mDisplayId, true);
                this.mWindowManagerInternal.setForcedDisplaySize(this.mDisplayId, foldedArea.width(), foldedArea.height());
                this.mDisplayManagerInternal.setDisplayOffsets(this.mDisplayId, -dx, -dy);
            } else {
                this.mDisplayManagerInternal.setDisplayScalingDisabled(this.mDisplayId, false);
                this.mWindowManagerInternal.clearForcedDisplaySize(this.mDisplayId);
                this.mDisplayManagerInternal.setDisplayOffsets(this.mDisplayId, 0, 0);
            }
        }
        this.mDurationLogger.setDeviceFolded(folded);
        this.mDurationLogger.logFocusedAppWithFoldState(folded, this.mFocusedApp);
        this.mFolded = Boolean.valueOf(folded);
        int n = this.mListeners.beginBroadcast();
        for (int i = 0; i < n; i++) {
            try {
                this.mListeners.getBroadcastItem(i).onDisplayFoldChanged(this.mDisplayId, folded);
            } catch (RemoteException e) {
            }
        }
        this.mListeners.finishBroadcast();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerDisplayFoldListener(final IDisplayFoldListener listener) {
        this.mListeners.register(listener);
        if (this.mFolded == null) {
            return;
        }
        this.mHandler.post(new Runnable() { // from class: com.android.server.policy.DisplayFoldController$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                DisplayFoldController.this.m5872x92212de4(listener);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$registerDisplayFoldListener$1$com-android-server-policy-DisplayFoldController  reason: not valid java name */
    public /* synthetic */ void m5872x92212de4(IDisplayFoldListener listener) {
        try {
            listener.onDisplayFoldChanged(this.mDisplayId, this.mFolded.booleanValue());
        } catch (RemoteException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterDisplayFoldListener(IDisplayFoldListener listener) {
        this.mListeners.unregister(listener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setOverrideFoldedArea(Rect area) {
        this.mOverrideFoldedArea.set(area);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Rect getFoldedArea() {
        if (!this.mOverrideFoldedArea.isEmpty()) {
            return this.mOverrideFoldedArea;
        }
        return this.mFoldedArea;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onDefaultDisplayFocusChanged(String pkg) {
        this.mFocusedApp = pkg;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static DisplayFoldController create(Context context, int displayId) {
        Rect foldedArea;
        WindowManagerInternal windowManagerService = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        DisplayManagerInternal displayService = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        String configFoldedArea = context.getResources().getString(17039975);
        if (configFoldedArea == null || configFoldedArea.isEmpty()) {
            foldedArea = new Rect();
        } else {
            foldedArea = Rect.unflattenFromString(configFoldedArea);
        }
        return new DisplayFoldController(context, windowManagerService, displayService, displayId, foldedArea, DisplayThread.getHandler());
    }
}
