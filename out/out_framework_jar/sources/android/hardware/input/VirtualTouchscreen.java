package android.hardware.input;

import android.annotation.SystemApi;
import android.companion.virtual.IVirtualDevice;
import android.os.IBinder;
import android.os.RemoteException;
import java.io.Closeable;
@SystemApi
/* loaded from: classes2.dex */
public class VirtualTouchscreen implements Closeable {
    private final IBinder mToken;
    private final IVirtualDevice mVirtualDevice;

    public VirtualTouchscreen(IVirtualDevice virtualDevice, IBinder token) {
        this.mVirtualDevice = virtualDevice;
        this.mToken = token;
    }

    @Override // java.io.Closeable, java.lang.AutoCloseable
    public void close() {
        try {
            this.mVirtualDevice.unregisterInputDevice(this.mToken);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void sendTouchEvent(VirtualTouchEvent event) {
        try {
            this.mVirtualDevice.sendTouchEvent(this.mToken, event);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
