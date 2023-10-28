package com.android.server.inputmethod;

import android.os.IBinder;
import android.os.InputConstants;
import android.os.Process;
import android.view.InputApplicationHandle;
import android.view.InputChannel;
import android.view.InputWindowHandle;
import android.view.SurfaceControl;
/* loaded from: classes.dex */
final class HandwritingEventReceiverSurface {
    static final boolean DEBUG = true;
    private static final int HANDWRITING_SURFACE_LAYER = 2130706432;
    public static final String TAG = HandwritingEventReceiverSurface.class.getSimpleName();
    private final InputChannel mClientChannel;
    private final SurfaceControl mInputSurface;
    private boolean mIsIntercepting;
    private final InputWindowHandle mWindowHandle;

    /* JADX INFO: Access modifiers changed from: package-private */
    public HandwritingEventReceiverSurface(String name, int displayId, SurfaceControl sc, InputChannel inputChannel) {
        this.mClientChannel = inputChannel;
        this.mInputSurface = sc;
        InputWindowHandle inputWindowHandle = new InputWindowHandle(new InputApplicationHandle((IBinder) null, name, InputConstants.DEFAULT_DISPATCHING_TIMEOUT_MILLIS), displayId);
        this.mWindowHandle = inputWindowHandle;
        inputWindowHandle.name = name;
        inputWindowHandle.token = inputChannel.getToken();
        inputWindowHandle.layoutParamsType = 2015;
        inputWindowHandle.dispatchingTimeoutMillis = InputConstants.DEFAULT_DISPATCHING_TIMEOUT_MILLIS;
        inputWindowHandle.ownerPid = Process.myPid();
        inputWindowHandle.ownerUid = Process.myUid();
        inputWindowHandle.scaleFactor = 1.0f;
        inputWindowHandle.inputConfig = 49420;
        inputWindowHandle.replaceTouchableRegionWithCrop((SurfaceControl) null);
        SurfaceControl.Transaction t = new SurfaceControl.Transaction();
        t.setInputWindowInfo(sc, inputWindowHandle);
        t.setLayer(sc, 2130706432);
        t.setPosition(sc, 0.0f, 0.0f);
        t.setCrop(sc, null);
        t.show(sc);
        t.apply();
        this.mIsIntercepting = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startIntercepting(int imePid, int imeUid) {
        this.mWindowHandle.ownerPid = imePid;
        this.mWindowHandle.ownerUid = imeUid;
        this.mWindowHandle.inputConfig &= -16385;
        new SurfaceControl.Transaction().setInputWindowInfo(this.mInputSurface, this.mWindowHandle).apply();
        this.mIsIntercepting = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isIntercepting() {
        return this.mIsIntercepting;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void remove() {
        SurfaceControl.Transaction t = new SurfaceControl.Transaction();
        t.remove(this.mInputSurface);
        t.apply();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InputChannel getInputChannel() {
        return this.mClientChannel;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SurfaceControl getSurface() {
        return this.mInputSurface;
    }

    InputWindowHandle getInputWindowHandle() {
        return this.mWindowHandle;
    }
}
