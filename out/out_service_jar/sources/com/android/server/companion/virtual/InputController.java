package com.android.server.companion.virtual;

import android.graphics.Point;
import android.graphics.PointF;
import android.hardware.display.DisplayManagerInternal;
import android.hardware.input.InputDeviceIdentifier;
import android.hardware.input.InputManager;
import android.hardware.input.InputManagerInternal;
import android.hardware.input.VirtualKeyEvent;
import android.hardware.input.VirtualMouseButtonEvent;
import android.hardware.input.VirtualMouseRelativeEvent;
import android.hardware.input.VirtualMouseScrollEvent;
import android.hardware.input.VirtualTouchEvent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Slog;
import android.view.InputDevice;
import android.view.WindowManager;
import com.android.server.LocalServices;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class InputController {
    static final String PHYS_TYPE_KEYBOARD = "Keyboard";
    static final String PHYS_TYPE_MOUSE = "Mouse";
    static final String PHYS_TYPE_TOUCHSCREEN = "Touchscreen";
    private static final String TAG = "VirtualInputController";
    private static final AtomicLong sNextPhysId = new AtomicLong(1);
    private final DisplayManagerInternal mDisplayManagerInternal;
    private final Handler mHandler;
    final Map<IBinder, InputDeviceDescriptor> mInputDeviceDescriptors;
    private final InputManagerInternal mInputManagerInternal;
    private final Object mLock;
    private final NativeWrapper mNativeWrapper;
    private final DeviceCreationThreadVerifier mThreadVerifier;
    private final WindowManager mWindowManager;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface DeviceCreationThreadVerifier {
        boolean isValidThread();
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    @interface PhysType {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean nativeCloseUinput(int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native int nativeOpenUinputKeyboard(String str, int i, int i2, String str2);

    /* JADX INFO: Access modifiers changed from: private */
    public static native int nativeOpenUinputMouse(String str, int i, int i2, String str2);

    /* JADX INFO: Access modifiers changed from: private */
    public static native int nativeOpenUinputTouchscreen(String str, int i, int i2, String str2, int i3, int i4);

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean nativeWriteButtonEvent(int i, int i2, int i3);

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean nativeWriteKeyEvent(int i, int i2, int i3);

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean nativeWriteRelativeEvent(int i, float f, float f2);

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean nativeWriteScrollEvent(int i, float f, float f2);

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean nativeWriteTouchEvent(int i, int i2, int i3, int i4, float f, float f2, float f3, float f4);

    /* JADX INFO: Access modifiers changed from: package-private */
    public InputController(Object lock, final Handler handler, WindowManager windowManager) {
        this(lock, new NativeWrapper(), handler, windowManager, new DeviceCreationThreadVerifier() { // from class: com.android.server.companion.virtual.InputController$$ExternalSyntheticLambda0
            @Override // com.android.server.companion.virtual.InputController.DeviceCreationThreadVerifier
            public final boolean isValidThread() {
                return InputController.lambda$new$0(handler);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$new$0(Handler handler) {
        return !handler.getLooper().isCurrentThread();
    }

    InputController(Object lock, NativeWrapper nativeWrapper, Handler handler, WindowManager windowManager, DeviceCreationThreadVerifier threadVerifier) {
        this.mInputDeviceDescriptors = new ArrayMap();
        this.mLock = lock;
        this.mHandler = handler;
        this.mNativeWrapper = nativeWrapper;
        this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        this.mInputManagerInternal = (InputManagerInternal) LocalServices.getService(InputManagerInternal.class);
        this.mWindowManager = windowManager;
        this.mThreadVerifier = threadVerifier;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void close() {
        synchronized (this.mLock) {
            Iterator<Map.Entry<IBinder, InputDeviceDescriptor>> iterator = this.mInputDeviceDescriptors.entrySet().iterator();
            if (iterator.hasNext()) {
                Map.Entry<IBinder, InputDeviceDescriptor> entry = iterator.next();
                IBinder token = entry.getKey();
                InputDeviceDescriptor inputDeviceDescriptor = entry.getValue();
                iterator.remove();
                closeInputDeviceDescriptorLocked(token, inputDeviceDescriptor);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void createKeyboard(final String deviceName, final int vendorId, final int productId, IBinder deviceToken, int displayId) {
        final String phys = createPhys(PHYS_TYPE_KEYBOARD);
        try {
            createDeviceInternal(1, deviceName, vendorId, productId, deviceToken, displayId, phys, new Supplier() { // from class: com.android.server.companion.virtual.InputController$$ExternalSyntheticLambda3
                @Override // java.util.function.Supplier
                public final Object get() {
                    return InputController.this.m2726xc6ae4cfa(deviceName, vendorId, productId, phys);
                }
            });
        } catch (DeviceCreationException e) {
            throw new RuntimeException("Failed to create virtual keyboard device '" + deviceName + "'.", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$createKeyboard$1$com-android-server-companion-virtual-InputController  reason: not valid java name */
    public /* synthetic */ Integer m2726xc6ae4cfa(String deviceName, int vendorId, int productId, String phys) {
        return Integer.valueOf(this.mNativeWrapper.openUinputKeyboard(deviceName, vendorId, productId, phys));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void createMouse(final String deviceName, final int vendorId, final int productId, IBinder deviceToken, int displayId) {
        final String phys = createPhys(PHYS_TYPE_MOUSE);
        try {
            createDeviceInternal(2, deviceName, vendorId, productId, deviceToken, displayId, phys, new Supplier() { // from class: com.android.server.companion.virtual.InputController$$ExternalSyntheticLambda2
                @Override // java.util.function.Supplier
                public final Object get() {
                    return InputController.this.m2727x867671ed(deviceName, vendorId, productId, phys);
                }
            });
            this.mInputManagerInternal.setVirtualMousePointerDisplayId(displayId);
        } catch (DeviceCreationException e) {
            throw new RuntimeException("Failed to create virtual mouse device: '" + deviceName + "'.", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$createMouse$2$com-android-server-companion-virtual-InputController  reason: not valid java name */
    public /* synthetic */ Integer m2727x867671ed(String deviceName, int vendorId, int productId, String phys) {
        return Integer.valueOf(this.mNativeWrapper.openUinputMouse(deviceName, vendorId, productId, phys));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void createTouchscreen(final String deviceName, final int vendorId, final int productId, IBinder deviceToken, int displayId, final Point screenSize) {
        final String phys = createPhys(PHYS_TYPE_TOUCHSCREEN);
        try {
            createDeviceInternal(3, deviceName, vendorId, productId, deviceToken, displayId, phys, new Supplier() { // from class: com.android.server.companion.virtual.InputController$$ExternalSyntheticLambda1
                @Override // java.util.function.Supplier
                public final Object get() {
                    return InputController.this.m2728x801e7b46(deviceName, vendorId, productId, phys, screenSize);
                }
            });
        } catch (DeviceCreationException e) {
            throw new RuntimeException("Failed to create virtual touchscreen device '" + deviceName + "'.", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$createTouchscreen$3$com-android-server-companion-virtual-InputController  reason: not valid java name */
    public /* synthetic */ Integer m2728x801e7b46(String deviceName, int vendorId, int productId, String phys, Point screenSize) {
        return Integer.valueOf(this.mNativeWrapper.openUinputTouchscreen(deviceName, vendorId, productId, phys, screenSize.y, screenSize.x));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterInputDevice(IBinder token) {
        synchronized (this.mLock) {
            InputDeviceDescriptor inputDeviceDescriptor = this.mInputDeviceDescriptors.remove(token);
            if (inputDeviceDescriptor == null) {
                throw new IllegalArgumentException("Could not unregister input device for given token");
            }
            closeInputDeviceDescriptorLocked(token, inputDeviceDescriptor);
        }
    }

    private void closeInputDeviceDescriptorLocked(IBinder token, InputDeviceDescriptor inputDeviceDescriptor) {
        token.unlinkToDeath(inputDeviceDescriptor.getDeathRecipient(), 0);
        this.mNativeWrapper.closeUinput(inputDeviceDescriptor.getFileDescriptor());
        InputManager.getInstance().removeUniqueIdAssociation(inputDeviceDescriptor.getPhys());
        if (inputDeviceDescriptor.isMouse() && this.mInputManagerInternal.getVirtualMousePointerDisplayId() == inputDeviceDescriptor.getDisplayId()) {
            updateActivePointerDisplayIdLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setShowPointerIcon(boolean visible, int displayId) {
        this.mInputManagerInternal.setPointerIconVisible(visible, displayId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPointerAcceleration(float pointerAcceleration, int displayId) {
        this.mInputManagerInternal.setPointerAcceleration(pointerAcceleration, displayId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDisplayEligibilityForPointerCapture(boolean isEligible, int displayId) {
        this.mInputManagerInternal.setDisplayEligibilityForPointerCapture(displayId, isEligible);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLocalIme(int displayId) {
        if ((this.mDisplayManagerInternal.getDisplayInfo(displayId).flags & 128) == 128) {
            this.mWindowManager.setDisplayImePolicy(displayId, 0);
        }
    }

    private void updateActivePointerDisplayIdLocked() {
        InputDeviceDescriptor mostRecentlyCreatedMouse = null;
        for (InputDeviceDescriptor otherInputDeviceDescriptor : this.mInputDeviceDescriptors.values()) {
            if (otherInputDeviceDescriptor.isMouse() && (mostRecentlyCreatedMouse == null || otherInputDeviceDescriptor.getCreationOrderNumber() > mostRecentlyCreatedMouse.getCreationOrderNumber())) {
                mostRecentlyCreatedMouse = otherInputDeviceDescriptor;
            }
        }
        if (mostRecentlyCreatedMouse != null) {
            this.mInputManagerInternal.setVirtualMousePointerDisplayId(mostRecentlyCreatedMouse.getDisplayId());
        } else {
            this.mInputManagerInternal.setVirtualMousePointerDisplayId(-1);
        }
    }

    private static String createPhys(String type) {
        return String.format("virtual%s:%d", type, Long.valueOf(sNextPhysId.getAndIncrement()));
    }

    private void setUniqueIdAssociation(int displayId, String phys) {
        String displayUniqueId = this.mDisplayManagerInternal.getDisplayInfo(displayId).uniqueId;
        InputManager.getInstance().addUniqueIdAssociation(phys, displayUniqueId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean sendKeyEvent(IBinder token, VirtualKeyEvent event) {
        boolean writeKeyEvent;
        synchronized (this.mLock) {
            InputDeviceDescriptor inputDeviceDescriptor = this.mInputDeviceDescriptors.get(token);
            if (inputDeviceDescriptor == null) {
                throw new IllegalArgumentException("Could not send key event to input device for given token");
            }
            writeKeyEvent = this.mNativeWrapper.writeKeyEvent(inputDeviceDescriptor.getFileDescriptor(), event.getKeyCode(), event.getAction());
        }
        return writeKeyEvent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean sendButtonEvent(IBinder token, VirtualMouseButtonEvent event) {
        boolean writeButtonEvent;
        synchronized (this.mLock) {
            InputDeviceDescriptor inputDeviceDescriptor = this.mInputDeviceDescriptors.get(token);
            if (inputDeviceDescriptor == null) {
                throw new IllegalArgumentException("Could not send button event to input device for given token");
            }
            if (inputDeviceDescriptor.getDisplayId() != this.mInputManagerInternal.getVirtualMousePointerDisplayId()) {
                throw new IllegalStateException("Display id associated with this mouse is not currently targetable");
            }
            writeButtonEvent = this.mNativeWrapper.writeButtonEvent(inputDeviceDescriptor.getFileDescriptor(), event.getButtonCode(), event.getAction());
        }
        return writeButtonEvent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean sendTouchEvent(IBinder token, VirtualTouchEvent event) {
        boolean writeTouchEvent;
        synchronized (this.mLock) {
            InputDeviceDescriptor inputDeviceDescriptor = this.mInputDeviceDescriptors.get(token);
            if (inputDeviceDescriptor == null) {
                throw new IllegalArgumentException("Could not send touch event to input device for given token");
            }
            writeTouchEvent = this.mNativeWrapper.writeTouchEvent(inputDeviceDescriptor.getFileDescriptor(), event.getPointerId(), event.getToolType(), event.getAction(), event.getX(), event.getY(), event.getPressure(), event.getMajorAxisSize());
        }
        return writeTouchEvent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean sendRelativeEvent(IBinder token, VirtualMouseRelativeEvent event) {
        boolean writeRelativeEvent;
        synchronized (this.mLock) {
            InputDeviceDescriptor inputDeviceDescriptor = this.mInputDeviceDescriptors.get(token);
            if (inputDeviceDescriptor == null) {
                throw new IllegalArgumentException("Could not send relative event to input device for given token");
            }
            if (inputDeviceDescriptor.getDisplayId() != this.mInputManagerInternal.getVirtualMousePointerDisplayId()) {
                throw new IllegalStateException("Display id associated with this mouse is not currently targetable");
            }
            writeRelativeEvent = this.mNativeWrapper.writeRelativeEvent(inputDeviceDescriptor.getFileDescriptor(), event.getRelativeX(), event.getRelativeY());
        }
        return writeRelativeEvent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean sendScrollEvent(IBinder token, VirtualMouseScrollEvent event) {
        boolean writeScrollEvent;
        synchronized (this.mLock) {
            InputDeviceDescriptor inputDeviceDescriptor = this.mInputDeviceDescriptors.get(token);
            if (inputDeviceDescriptor == null) {
                throw new IllegalArgumentException("Could not send scroll event to input device for given token");
            }
            if (inputDeviceDescriptor.getDisplayId() != this.mInputManagerInternal.getVirtualMousePointerDisplayId()) {
                throw new IllegalStateException("Display id associated with this mouse is not currently targetable");
            }
            writeScrollEvent = this.mNativeWrapper.writeScrollEvent(inputDeviceDescriptor.getFileDescriptor(), event.getXAxisMovement(), event.getYAxisMovement());
        }
        return writeScrollEvent;
    }

    public PointF getCursorPosition(IBinder token) {
        PointF cursorPosition;
        synchronized (this.mLock) {
            InputDeviceDescriptor inputDeviceDescriptor = this.mInputDeviceDescriptors.get(token);
            if (inputDeviceDescriptor == null) {
                throw new IllegalArgumentException("Could not get cursor position for input device for given token");
            }
            if (inputDeviceDescriptor.getDisplayId() != this.mInputManagerInternal.getVirtualMousePointerDisplayId()) {
                throw new IllegalStateException("Display id associated with this mouse is not currently targetable");
            }
            cursorPosition = ((InputManagerInternal) LocalServices.getService(InputManagerInternal.class)).getCursorPosition();
        }
        return cursorPosition;
    }

    public void dump(PrintWriter fout) {
        fout.println("    InputController: ");
        synchronized (this.mLock) {
            fout.println("      Active descriptors: ");
            for (InputDeviceDescriptor inputDeviceDescriptor : this.mInputDeviceDescriptors.values()) {
                fout.println("        fd: " + inputDeviceDescriptor.getFileDescriptor());
                fout.println("          displayId: " + inputDeviceDescriptor.getDisplayId());
                fout.println("          creationOrder: " + inputDeviceDescriptor.getCreationOrderNumber());
                fout.println("          type: " + inputDeviceDescriptor.getType());
                fout.println("          phys: " + inputDeviceDescriptor.getPhys());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public static class NativeWrapper {
        protected NativeWrapper() {
        }

        public int openUinputKeyboard(String deviceName, int vendorId, int productId, String phys) {
            return InputController.nativeOpenUinputKeyboard(deviceName, vendorId, productId, phys);
        }

        public int openUinputMouse(String deviceName, int vendorId, int productId, String phys) {
            return InputController.nativeOpenUinputMouse(deviceName, vendorId, productId, phys);
        }

        public int openUinputTouchscreen(String deviceName, int vendorId, int productId, String phys, int height, int width) {
            return InputController.nativeOpenUinputTouchscreen(deviceName, vendorId, productId, phys, height, width);
        }

        public boolean closeUinput(int fd) {
            return InputController.nativeCloseUinput(fd);
        }

        public boolean writeKeyEvent(int fd, int androidKeyCode, int action) {
            return InputController.nativeWriteKeyEvent(fd, androidKeyCode, action);
        }

        public boolean writeButtonEvent(int fd, int buttonCode, int action) {
            return InputController.nativeWriteButtonEvent(fd, buttonCode, action);
        }

        public boolean writeTouchEvent(int fd, int pointerId, int toolType, int action, float locationX, float locationY, float pressure, float majorAxisSize) {
            return InputController.nativeWriteTouchEvent(fd, pointerId, toolType, action, locationX, locationY, pressure, majorAxisSize);
        }

        public boolean writeRelativeEvent(int fd, float relativeX, float relativeY) {
            return InputController.nativeWriteRelativeEvent(fd, relativeX, relativeY);
        }

        public boolean writeScrollEvent(int fd, float xAxisMovement, float yAxisMovement) {
            return InputController.nativeWriteScrollEvent(fd, xAxisMovement, yAxisMovement);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class InputDeviceDescriptor {
        static final int TYPE_KEYBOARD = 1;
        static final int TYPE_MOUSE = 2;
        static final int TYPE_TOUCHSCREEN = 3;
        private static final AtomicLong sNextCreationOrderNumber = new AtomicLong(1);
        private final long mCreationOrderNumber = sNextCreationOrderNumber.getAndIncrement();
        private final IBinder.DeathRecipient mDeathRecipient;
        private final int mDisplayId;
        private final int mFd;
        private final String mPhys;
        private final int mType;

        @Retention(RetentionPolicy.SOURCE)
        /* loaded from: classes.dex */
        @interface Type {
        }

        InputDeviceDescriptor(int fd, IBinder.DeathRecipient deathRecipient, int type, int displayId, String phys) {
            this.mFd = fd;
            this.mDeathRecipient = deathRecipient;
            this.mType = type;
            this.mDisplayId = displayId;
            this.mPhys = phys;
        }

        public int getFileDescriptor() {
            return this.mFd;
        }

        public int getType() {
            return this.mType;
        }

        public boolean isMouse() {
            return this.mType == 2;
        }

        public IBinder.DeathRecipient getDeathRecipient() {
            return this.mDeathRecipient;
        }

        public int getDisplayId() {
            return this.mDisplayId;
        }

        public long getCreationOrderNumber() {
            return this.mCreationOrderNumber;
        }

        public String getPhys() {
            return this.mPhys;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class BinderDeathRecipient implements IBinder.DeathRecipient {
        private final IBinder mDeviceToken;

        BinderDeathRecipient(IBinder deviceToken) {
            this.mDeviceToken = deviceToken;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Slog.e(InputController.TAG, "Virtual input controller binder died");
            InputController.this.unregisterInputDevice(this.mDeviceToken);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class WaitForDevice implements AutoCloseable {
        private final CountDownLatch mDeviceAddedLatch = new CountDownLatch(1);
        private final InputManager.InputDeviceListener mListener;

        WaitForDevice(final String deviceName, final int vendorId, final int productId) {
            InputManager.InputDeviceListener inputDeviceListener = new InputManager.InputDeviceListener() { // from class: com.android.server.companion.virtual.InputController.WaitForDevice.1
                @Override // android.hardware.input.InputManager.InputDeviceListener
                public void onInputDeviceAdded(int deviceId) {
                    InputDevice device = InputManager.getInstance().getInputDevice(deviceId);
                    Objects.requireNonNull(device, "Newly added input device was null.");
                    if (!device.getName().equals(deviceName)) {
                        return;
                    }
                    InputDeviceIdentifier id = device.getIdentifier();
                    if (id.getVendorId() != vendorId || id.getProductId() != productId) {
                        return;
                    }
                    WaitForDevice.this.mDeviceAddedLatch.countDown();
                }

                @Override // android.hardware.input.InputManager.InputDeviceListener
                public void onInputDeviceRemoved(int deviceId) {
                }

                @Override // android.hardware.input.InputManager.InputDeviceListener
                public void onInputDeviceChanged(int deviceId) {
                }
            };
            this.mListener = inputDeviceListener;
            InputManager.getInstance().registerInputDeviceListener(inputDeviceListener, InputController.this.mHandler);
        }

        void waitForDeviceCreation() throws DeviceCreationException {
            try {
                if (!this.mDeviceAddedLatch.await(1L, TimeUnit.MINUTES)) {
                    throw new DeviceCreationException("Timed out waiting for virtual device to be created.");
                }
            } catch (InterruptedException e) {
                throw new DeviceCreationException("Interrupted while waiting for virtual device to be created.", e);
            }
        }

        @Override // java.lang.AutoCloseable
        public void close() {
            InputManager.getInstance().unregisterInputDeviceListener(this.mListener);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class DeviceCreationException extends Exception {
        DeviceCreationException(String message) {
            super(message);
        }

        DeviceCreationException(String message, Exception cause) {
            super(message, cause);
        }
    }

    private void createDeviceInternal(int type, String deviceName, int vendorId, int productId, IBinder deviceToken, int displayId, String phys, Supplier<Integer> deviceOpener) throws DeviceCreationException {
        if (!this.mThreadVerifier.isValidThread()) {
            throw new IllegalStateException("Virtual device creation should happen on an auxiliary thread (e.g. binder thread) and not from the handler's thread.");
        }
        setUniqueIdAssociation(displayId, phys);
        try {
        } catch (DeviceCreationException e) {
            e = e;
        }
        try {
            WaitForDevice waiter = new WaitForDevice(deviceName, vendorId, productId);
            int fd = deviceOpener.get().intValue();
            if (fd < 0) {
                throw new DeviceCreationException("A native error occurred when creating touchscreen: " + (-fd));
            }
            try {
                waiter.waitForDeviceCreation();
                BinderDeathRecipient binderDeathRecipient = new BinderDeathRecipient(deviceToken);
                try {
                    deviceToken.linkToDeath(binderDeathRecipient, 0);
                    waiter.close();
                    synchronized (this.mLock) {
                        try {
                            try {
                                this.mInputDeviceDescriptors.put(deviceToken, new InputDeviceDescriptor(fd, binderDeathRecipient, type, displayId, phys));
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                } catch (RemoteException e2) {
                    throw new DeviceCreationException("Client died before virtual device could be created.", e2);
                }
            } catch (DeviceCreationException e3) {
                this.mNativeWrapper.closeUinput(fd);
                throw e3;
            }
        } catch (DeviceCreationException e4) {
            e = e4;
            InputManager.getInstance().removeUniqueIdAssociation(phys);
            throw e;
        }
    }
}
