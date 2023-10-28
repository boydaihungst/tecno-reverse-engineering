package com.android.server.input;

import android.os.IBinder;
import android.os.InputConstants;
import android.view.InputApplicationHandle;
import android.view.InputChannel;
import android.view.InputWindowHandle;
import android.view.SurfaceControl;
/* loaded from: classes.dex */
class GestureMonitorSpyWindow {
    final InputApplicationHandle mApplicationHandle;
    final InputChannel mClientChannel;
    final SurfaceControl mInputSurface;
    final IBinder mMonitorToken;
    final InputWindowHandle mWindowHandle;

    /* JADX INFO: Access modifiers changed from: package-private */
    public GestureMonitorSpyWindow(IBinder token, String name, int displayId, int pid, int uid, SurfaceControl sc, InputChannel inputChannel) {
        this.mMonitorToken = token;
        this.mClientChannel = inputChannel;
        this.mInputSurface = sc;
        InputApplicationHandle inputApplicationHandle = new InputApplicationHandle((IBinder) null, name, InputConstants.DEFAULT_DISPATCHING_TIMEOUT_MILLIS);
        this.mApplicationHandle = inputApplicationHandle;
        InputWindowHandle inputWindowHandle = new InputWindowHandle(inputApplicationHandle, displayId);
        this.mWindowHandle = inputWindowHandle;
        inputWindowHandle.name = name;
        inputWindowHandle.token = inputChannel.getToken();
        inputWindowHandle.layoutParamsType = 2015;
        inputWindowHandle.dispatchingTimeoutMillis = InputConstants.DEFAULT_DISPATCHING_TIMEOUT_MILLIS;
        inputWindowHandle.ownerPid = pid;
        inputWindowHandle.ownerUid = uid;
        inputWindowHandle.scaleFactor = 1.0f;
        inputWindowHandle.replaceTouchableRegionWithCrop((SurfaceControl) null);
        inputWindowHandle.inputConfig = 16644;
        SurfaceControl.Transaction t = new SurfaceControl.Transaction();
        t.setInputWindowInfo(sc, inputWindowHandle);
        t.setLayer(sc, 2130706433);
        t.setPosition(sc, 0.0f, 0.0f);
        t.setCrop(sc, null);
        t.show(sc);
        t.apply();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void remove() {
        SurfaceControl.Transaction t = new SurfaceControl.Transaction();
        t.hide(this.mInputSurface);
        t.remove(this.mInputSurface);
        t.apply();
        this.mClientChannel.dispose();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String dump() {
        return "name='" + this.mWindowHandle.name + "', inputChannelToken=" + this.mClientChannel.getToken() + " displayId=" + this.mWindowHandle.displayId;
    }
}
