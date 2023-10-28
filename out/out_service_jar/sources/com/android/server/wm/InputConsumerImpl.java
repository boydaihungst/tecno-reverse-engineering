package com.android.server.wm;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Binder;
import android.os.IBinder;
import android.os.InputConstants;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.view.InputApplicationHandle;
import android.view.InputChannel;
import android.view.InputWindowHandle;
import android.view.SurfaceControl;
import java.io.PrintWriter;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class InputConsumerImpl implements IBinder.DeathRecipient {
    final InputApplicationHandle mApplicationHandle;
    final InputChannel mClientChannel;
    final int mClientPid;
    final UserHandle mClientUser;
    final SurfaceControl mInputSurface;
    final String mName;
    final WindowManagerService mService;
    final IBinder mToken;
    final InputWindowHandle mWindowHandle;
    Rect mTmpClipRect = new Rect();
    private final Rect mTmpRect = new Rect();
    private final Point mOldPosition = new Point();
    private final Rect mOldWindowCrop = new Rect();

    /* JADX INFO: Access modifiers changed from: package-private */
    public InputConsumerImpl(WindowManagerService service, IBinder token, String name, InputChannel inputChannel, int clientPid, UserHandle clientUser, int displayId) {
        this.mService = service;
        this.mToken = token;
        this.mName = name;
        this.mClientPid = clientPid;
        this.mClientUser = clientUser;
        InputChannel createInputChannel = service.mInputManager.createInputChannel(name);
        this.mClientChannel = createInputChannel;
        if (inputChannel != null) {
            createInputChannel.copyTo(inputChannel);
        }
        InputApplicationHandle inputApplicationHandle = new InputApplicationHandle(new Binder(), name, InputConstants.DEFAULT_DISPATCHING_TIMEOUT_MILLIS);
        this.mApplicationHandle = inputApplicationHandle;
        InputWindowHandle inputWindowHandle = new InputWindowHandle(inputApplicationHandle, displayId);
        this.mWindowHandle = inputWindowHandle;
        inputWindowHandle.name = name;
        inputWindowHandle.token = createInputChannel.getToken();
        inputWindowHandle.layoutParamsType = 2022;
        inputWindowHandle.dispatchingTimeoutMillis = InputConstants.DEFAULT_DISPATCHING_TIMEOUT_MILLIS;
        inputWindowHandle.ownerPid = Process.myPid();
        inputWindowHandle.ownerUid = Process.myUid();
        inputWindowHandle.scaleFactor = 1.0f;
        inputWindowHandle.inputConfig = 260;
        this.mInputSurface = service.makeSurfaceBuilder(service.mRoot.getDisplayContent(displayId).getSession()).setContainerLayer().setName("Input Consumer " + name).setCallsite("InputConsumerImpl").build();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void linkToDeathRecipient() {
        IBinder iBinder = this.mToken;
        if (iBinder == null) {
            return;
        }
        try {
            iBinder.linkToDeath(this, 0);
        } catch (RemoteException e) {
        }
    }

    void unlinkFromDeathRecipient() {
        IBinder iBinder = this.mToken;
        if (iBinder == null) {
            return;
        }
        iBinder.unlinkToDeath(this, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void layout(SurfaceControl.Transaction t, int dw, int dh) {
        this.mTmpRect.set(0, 0, dw, dh);
        layout(t, this.mTmpRect);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void layout(SurfaceControl.Transaction t, Rect r) {
        this.mTmpClipRect.set(0, 0, r.width(), r.height());
        if (this.mOldPosition.equals(r.left, r.top) && this.mOldWindowCrop.equals(this.mTmpClipRect)) {
            return;
        }
        t.setPosition(this.mInputSurface, r.left, r.top);
        t.setWindowCrop(this.mInputSurface, this.mTmpClipRect);
        this.mOldPosition.set(r.left, r.top);
        this.mOldWindowCrop.set(this.mTmpClipRect);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void hide(SurfaceControl.Transaction t) {
        t.hide(this.mInputSurface);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void show(SurfaceControl.Transaction t, WindowContainer w) {
        t.show(this.mInputSurface);
        t.setInputWindowInfo(this.mInputSurface, this.mWindowHandle);
        t.setRelativeLayer(this.mInputSurface, w.getSurfaceControl(), 1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void show(SurfaceControl.Transaction t, int layer) {
        t.show(this.mInputSurface);
        t.setInputWindowInfo(this.mInputSurface, this.mWindowHandle);
        t.setLayer(this.mInputSurface, layer);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reparent(SurfaceControl.Transaction t, WindowContainer wc) {
        t.reparent(this.mInputSurface, wc.getSurfaceControl());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void disposeChannelsLw(SurfaceControl.Transaction t) {
        this.mService.mInputManager.removeInputChannel(this.mClientChannel.getToken());
        this.mClientChannel.dispose();
        t.remove(this.mInputSurface);
        unlinkFromDeathRecipient();
    }

    @Override // android.os.IBinder.DeathRecipient
    public void binderDied() {
        synchronized (this.mService.getWindowManagerLock()) {
            DisplayContent dc = this.mService.mRoot.getDisplayContent(this.mWindowHandle.displayId);
            if (dc == null) {
                return;
            }
            dc.getInputMonitor().destroyInputConsumer(this.mName);
            unlinkFromDeathRecipient();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String name, String prefix) {
        pw.println(prefix + "  name=" + name + " pid=" + this.mClientPid + " user=" + this.mClientUser);
    }
}
