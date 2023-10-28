package android.hardware.input;

import android.annotation.SystemApi;
import android.companion.virtual.IVirtualDevice;
import android.graphics.PointF;
import android.os.IBinder;
import android.os.RemoteException;
import java.io.Closeable;
@SystemApi
/* loaded from: classes2.dex */
public class VirtualMouse implements Closeable {
    private final IBinder mToken;
    private final IVirtualDevice mVirtualDevice;

    public VirtualMouse(IVirtualDevice virtualDevice, IBinder token) {
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

    public void sendButtonEvent(VirtualMouseButtonEvent event) {
        try {
            this.mVirtualDevice.sendButtonEvent(this.mToken, event);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void sendScrollEvent(VirtualMouseScrollEvent event) {
        try {
            this.mVirtualDevice.sendScrollEvent(this.mToken, event);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void sendRelativeEvent(VirtualMouseRelativeEvent event) {
        try {
            this.mVirtualDevice.sendRelativeEvent(this.mToken, event);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public PointF getCursorPosition() {
        try {
            return this.mVirtualDevice.getCursorPosition(this.mToken);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
