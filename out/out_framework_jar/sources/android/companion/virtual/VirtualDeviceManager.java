package android.companion.virtual;

import android.annotation.SystemApi;
import android.app.PendingIntent;
import android.companion.virtual.IVirtualDeviceActivityListener;
import android.companion.virtual.VirtualDeviceManager;
import android.companion.virtual.audio.VirtualAudioDevice;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Point;
import android.hardware.display.DisplayManagerGlobal;
import android.hardware.display.IVirtualDisplayCallback;
import android.hardware.display.VirtualDisplay;
import android.hardware.display.VirtualDisplayConfig;
import android.hardware.input.VirtualKeyboard;
import android.hardware.input.VirtualMouse;
import android.hardware.input.VirtualTouchscreen;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.util.ArrayMap;
import android.view.Surface;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.Executor;
import java.util.function.IntConsumer;
@SystemApi
/* loaded from: classes.dex */
public final class VirtualDeviceManager {
    private static final boolean DEBUG = false;
    private static final int DEFAULT_VIRTUAL_DISPLAY_FLAGS = 2505;
    public static final int LAUNCH_FAILURE_NO_ACTIVITY = 2;
    public static final int LAUNCH_FAILURE_PENDING_INTENT_CANCELED = 1;
    public static final int LAUNCH_SUCCESS = 0;
    private static final String TAG = "VirtualDeviceManager";
    private final Context mContext;
    private final IVirtualDeviceManager mService;

    /* loaded from: classes.dex */
    public interface ActivityListener {
        void onDisplayEmpty(int i);

        void onTopActivityChanged(int i, ComponentName componentName);
    }

    @Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface PendingIntentLaunchStatus {
    }

    public VirtualDeviceManager(IVirtualDeviceManager service, Context context) {
        this.mService = service;
        this.mContext = context;
    }

    public VirtualDevice createVirtualDevice(int associationId, VirtualDeviceParams params) {
        try {
            return new VirtualDevice(this.mService, this.mContext, associationId, params);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /* loaded from: classes.dex */
    public static class VirtualDevice implements AutoCloseable {
        private final IVirtualDeviceActivityListener mActivityListenerBinder;
        private final ArrayMap<ActivityListener, ActivityListenerDelegate> mActivityListeners;
        private final Context mContext;
        private final IVirtualDeviceManager mService;
        private VirtualAudioDevice mVirtualAudioDevice;
        private final IVirtualDevice mVirtualDevice;

        private VirtualDevice(IVirtualDeviceManager service, Context context, int associationId, VirtualDeviceParams params) throws RemoteException {
            this.mActivityListeners = new ArrayMap<>();
            IVirtualDeviceActivityListener.Stub stub = new IVirtualDeviceActivityListener.Stub() { // from class: android.companion.virtual.VirtualDeviceManager.VirtualDevice.1
                @Override // android.companion.virtual.IVirtualDeviceActivityListener
                public void onTopActivityChanged(int displayId, ComponentName topActivity) {
                    long token = Binder.clearCallingIdentity();
                    for (int i = 0; i < VirtualDevice.this.mActivityListeners.size(); i++) {
                        try {
                            ((ActivityListenerDelegate) VirtualDevice.this.mActivityListeners.valueAt(i)).onTopActivityChanged(displayId, topActivity);
                        } finally {
                            Binder.restoreCallingIdentity(token);
                        }
                    }
                }

                @Override // android.companion.virtual.IVirtualDeviceActivityListener
                public void onDisplayEmpty(int displayId) {
                    long token = Binder.clearCallingIdentity();
                    for (int i = 0; i < VirtualDevice.this.mActivityListeners.size(); i++) {
                        try {
                            ((ActivityListenerDelegate) VirtualDevice.this.mActivityListeners.valueAt(i)).onDisplayEmpty(displayId);
                        } finally {
                            Binder.restoreCallingIdentity(token);
                        }
                    }
                }
            };
            this.mActivityListenerBinder = stub;
            this.mService = service;
            Context applicationContext = context.getApplicationContext();
            this.mContext = applicationContext;
            this.mVirtualDevice = service.createVirtualDevice(new Binder(), applicationContext.getPackageName(), associationId, params, stub);
        }

        public void launchPendingIntent(int displayId, PendingIntent pendingIntent, Executor executor, IntConsumer listener) {
            try {
                this.mVirtualDevice.launchPendingIntent(displayId, pendingIntent, new AnonymousClass2(new Handler(Looper.getMainLooper()), executor, listener));
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
            }
        }

        /* renamed from: android.companion.virtual.VirtualDeviceManager$VirtualDevice$2  reason: invalid class name */
        /* loaded from: classes.dex */
        class AnonymousClass2 extends ResultReceiver {
            final /* synthetic */ Executor val$executor;
            final /* synthetic */ IntConsumer val$listener;

            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            AnonymousClass2(Handler handler, Executor executor, IntConsumer intConsumer) {
                super(handler);
                this.val$executor = executor;
                this.val$listener = intConsumer;
            }

            /* JADX INFO: Access modifiers changed from: protected */
            @Override // android.os.ResultReceiver
            public void onReceiveResult(final int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);
                Executor executor = this.val$executor;
                final IntConsumer intConsumer = this.val$listener;
                executor.execute(new Runnable() { // from class: android.companion.virtual.VirtualDeviceManager$VirtualDevice$2$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        intConsumer.accept(resultCode);
                    }
                });
            }
        }

        public VirtualDisplay createVirtualDisplay(int width, int height, int densityDpi, Surface surface, int flags, Executor executor, VirtualDisplay.Callback callback) {
            VirtualDisplayConfig config = new VirtualDisplayConfig.Builder(getVirtualDisplayName(), width, height, densityDpi).setSurface(surface).setFlags(getVirtualDisplayFlags(flags)).build();
            IVirtualDisplayCallback callbackWrapper = new DisplayManagerGlobal.VirtualDisplayCallback(callback, executor);
            try {
                int displayId = this.mService.createVirtualDisplay(config, callbackWrapper, this.mVirtualDevice, this.mContext.getPackageName());
                DisplayManagerGlobal displayManager = DisplayManagerGlobal.getInstance();
                return displayManager.createVirtualDisplayWrapper(config, this.mContext, callbackWrapper, displayId);
            } catch (RemoteException ex) {
                throw ex.rethrowFromSystemServer();
            }
        }

        @Override // java.lang.AutoCloseable
        public void close() {
            try {
                this.mVirtualDevice.close();
                VirtualAudioDevice virtualAudioDevice = this.mVirtualAudioDevice;
                if (virtualAudioDevice != null) {
                    virtualAudioDevice.close();
                    this.mVirtualAudioDevice = null;
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        public VirtualKeyboard createVirtualKeyboard(VirtualDisplay display, String inputDeviceName, int vendorId, int productId) {
            try {
                IBinder token = new Binder("android.hardware.input.VirtualKeyboard:" + inputDeviceName);
                this.mVirtualDevice.createVirtualKeyboard(display.getDisplay().getDisplayId(), inputDeviceName, vendorId, productId, token);
                return new VirtualKeyboard(this.mVirtualDevice, token);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        public VirtualMouse createVirtualMouse(VirtualDisplay display, String inputDeviceName, int vendorId, int productId) {
            try {
                IBinder token = new Binder("android.hardware.input.VirtualMouse:" + inputDeviceName);
                this.mVirtualDevice.createVirtualMouse(display.getDisplay().getDisplayId(), inputDeviceName, vendorId, productId, token);
                return new VirtualMouse(this.mVirtualDevice, token);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        public VirtualTouchscreen createVirtualTouchscreen(VirtualDisplay display, String inputDeviceName, int vendorId, int productId) {
            try {
                IBinder token = new Binder("android.hardware.input.VirtualTouchscreen:" + inputDeviceName);
                Point size = new Point();
                display.getDisplay().getSize(size);
                this.mVirtualDevice.createVirtualTouchscreen(display.getDisplay().getDisplayId(), inputDeviceName, vendorId, productId, token, size);
                return new VirtualTouchscreen(this.mVirtualDevice, token);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        public VirtualAudioDevice createVirtualAudioDevice(VirtualDisplay display, Executor executor, VirtualAudioDevice.AudioConfigurationChangeCallback callback) {
            if (this.mVirtualAudioDevice == null) {
                this.mVirtualAudioDevice = new VirtualAudioDevice(this.mContext, this.mVirtualDevice, display, executor, callback);
            }
            return this.mVirtualAudioDevice;
        }

        public void setShowPointerIcon(boolean showPointerIcon) {
            try {
                this.mVirtualDevice.setShowPointerIcon(showPointerIcon);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        private int getVirtualDisplayFlags(int flags) {
            return flags | VirtualDeviceManager.DEFAULT_VIRTUAL_DISPLAY_FLAGS;
        }

        private String getVirtualDisplayName() {
            try {
                return "VirtualDevice_" + this.mVirtualDevice.getAssociationId();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        public void addActivityListener(Executor executor, ActivityListener listener) {
            this.mActivityListeners.put(listener, new ActivityListenerDelegate(listener, executor));
        }

        public void removeActivityListener(ActivityListener listener) {
            this.mActivityListeners.remove(listener);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ActivityListenerDelegate {
        private final ActivityListener mActivityListener;
        private final Executor mExecutor;

        ActivityListenerDelegate(ActivityListener listener, Executor executor) {
            this.mActivityListener = listener;
            this.mExecutor = executor;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onTopActivityChanged$0$android-companion-virtual-VirtualDeviceManager$ActivityListenerDelegate  reason: not valid java name */
        public /* synthetic */ void m698x4baf67d3(int displayId, ComponentName topActivity) {
            this.mActivityListener.onTopActivityChanged(displayId, topActivity);
        }

        public void onTopActivityChanged(final int displayId, final ComponentName topActivity) {
            this.mExecutor.execute(new Runnable() { // from class: android.companion.virtual.VirtualDeviceManager$ActivityListenerDelegate$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    VirtualDeviceManager.ActivityListenerDelegate.this.m698x4baf67d3(displayId, topActivity);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onDisplayEmpty$1$android-companion-virtual-VirtualDeviceManager$ActivityListenerDelegate  reason: not valid java name */
        public /* synthetic */ void m697x1e00192f(int displayId) {
            this.mActivityListener.onDisplayEmpty(displayId);
        }

        public void onDisplayEmpty(final int displayId) {
            this.mExecutor.execute(new Runnable() { // from class: android.companion.virtual.VirtualDeviceManager$ActivityListenerDelegate$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    VirtualDeviceManager.ActivityListenerDelegate.this.m697x1e00192f(displayId);
                }
            });
        }
    }
}
