package android.hardware.input;

import android.graphics.PointF;
import android.hardware.display.DisplayViewport;
import android.os.IBinder;
import android.view.InputChannel;
import java.util.List;
/* loaded from: classes2.dex */
public abstract class InputManagerInternal {

    /* loaded from: classes2.dex */
    public interface LidSwitchCallback {
        void notifyLidSwitchChanged(long j, boolean z);
    }

    public abstract InputChannel createInputChannel(String str);

    public abstract PointF getCursorPosition();

    public abstract int getVirtualMousePointerDisplayId();

    public abstract void pilferPointers(IBinder iBinder);

    public abstract void registerLidSwitchCallback(LidSwitchCallback lidSwitchCallback);

    public abstract void setConnectScreenActive(boolean z);

    public abstract void setDisplayEligibilityForPointerCapture(int i, boolean z);

    public abstract void setDisplayViewports(List<DisplayViewport> list);

    public abstract void setInteractive(boolean z);

    public abstract void setPointerAcceleration(float f, int i);

    public abstract void setPointerIconVisible(boolean z, int i);

    public abstract void setPulseGestureEnabled(boolean z);

    public abstract boolean setVirtualMousePointerDisplayId(int i);

    public abstract void toggleCapsLock(int i);

    public abstract boolean transferTouchFocus(IBinder iBinder, IBinder iBinder2);

    public abstract void unregisterLidSwitchCallback(LidSwitchCallback lidSwitchCallback);
}
