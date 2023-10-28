package com.android.server.wm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.HardwareBuffer;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.service.displayhash.DisplayHashParams;
import android.service.displayhash.IDisplayHashingService;
import android.util.Size;
import android.util.Slog;
import android.view.MagnificationSpec;
import android.view.SurfaceControl;
import android.view.displayhash.DisplayHash;
import android.view.displayhash.VerifiedDisplayHash;
import com.android.server.wm.DisplayHashController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
/* loaded from: classes2.dex */
public class DisplayHashController {
    private static final boolean DEBUG = false;
    private static final String TAG = "WindowManager";
    private final Context mContext;
    private Map<String, DisplayHashParams> mDisplayHashAlgorithms;
    private long mLastRequestTimeMs;
    private int mLastRequestUid;
    private DisplayHashingServiceConnection mServiceConnection;
    private final Object mServiceConnectionLock = new Object();
    private final Object mDisplayHashAlgorithmsLock = new Object();
    private final float[] mTmpFloat9 = new float[9];
    private final Matrix mTmpMatrix = new Matrix();
    private final RectF mTmpRectF = new RectF();
    private final Object mIntervalBetweenRequestsLock = new Object();
    private int mIntervalBetweenRequestMillis = -1;
    private boolean mDisplayHashThrottlingEnabled = true;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final byte[] mSalt = UUID.randomUUID().toString().getBytes();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public interface Command {
        void run(IDisplayHashingService iDisplayHashingService) throws RemoteException;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayHashController(Context context) {
        this.mContext = context;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String[] getSupportedHashAlgorithms() {
        Map<String, DisplayHashParams> displayHashAlgorithms = getDisplayHashAlgorithms();
        return (String[]) displayHashAlgorithms.keySet().toArray(new String[0]);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public VerifiedDisplayHash verifyDisplayHash(final DisplayHash displayHash) {
        SyncCommand syncCommand = new SyncCommand();
        Bundle results = syncCommand.run(new BiConsumer() { // from class: com.android.server.wm.DisplayHashController$$ExternalSyntheticLambda2
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                DisplayHashController.this.m7947x3701c5f7(displayHash, (IDisplayHashingService) obj, (RemoteCallback) obj2);
            }
        });
        return (VerifiedDisplayHash) results.getParcelable("android.service.displayhash.extra.VERIFIED_DISPLAY_HASH");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$verifyDisplayHash$0$com-android-server-wm-DisplayHashController  reason: not valid java name */
    public /* synthetic */ void m7947x3701c5f7(DisplayHash displayHash, IDisplayHashingService service, RemoteCallback remoteCallback) {
        try {
            service.verifyDisplayHash(this.mSalt, displayHash, remoteCallback);
        } catch (RemoteException e) {
            Slog.e("WindowManager", "Failed to invoke verifyDisplayHash command");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDisplayHashThrottlingEnabled(boolean enable) {
        this.mDisplayHashThrottlingEnabled = enable;
    }

    private void generateDisplayHash(final HardwareBuffer buffer, final Rect bounds, final String hashAlgorithm, final RemoteCallback callback) {
        connectAndRun(new Command() { // from class: com.android.server.wm.DisplayHashController$$ExternalSyntheticLambda0
            @Override // com.android.server.wm.DisplayHashController.Command
            public final void run(IDisplayHashingService iDisplayHashingService) {
                DisplayHashController.this.m7946x2356023c(buffer, bounds, hashAlgorithm, callback, iDisplayHashingService);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$generateDisplayHash$1$com-android-server-wm-DisplayHashController  reason: not valid java name */
    public /* synthetic */ void m7946x2356023c(HardwareBuffer buffer, Rect bounds, String hashAlgorithm, RemoteCallback callback, IDisplayHashingService service) throws RemoteException {
        service.generateDisplayHash(this.mSalt, buffer, bounds, hashAlgorithm, callback);
    }

    private boolean allowedToGenerateHash(int uid) {
        if (this.mDisplayHashThrottlingEnabled) {
            long currentTime = System.currentTimeMillis();
            if (this.mLastRequestUid != uid) {
                this.mLastRequestUid = uid;
                this.mLastRequestTimeMs = currentTime;
                return true;
            }
            int mIntervalBetweenRequestsMs = getIntervalBetweenRequestMillis();
            if (currentTime - this.mLastRequestTimeMs < mIntervalBetweenRequestsMs) {
                return false;
            }
            this.mLastRequestTimeMs = currentTime;
            return true;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void generateDisplayHash(SurfaceControl.LayerCaptureArgs.Builder args, Rect boundsInWindow, String hashAlgorithm, int uid, RemoteCallback callback) {
        if (!allowedToGenerateHash(uid)) {
            sendDisplayHashError(callback, -6);
            return;
        }
        Map<String, DisplayHashParams> displayHashAlgorithmsMap = getDisplayHashAlgorithms();
        DisplayHashParams displayHashParams = displayHashAlgorithmsMap.get(hashAlgorithm);
        if (displayHashParams == null) {
            Slog.w("WindowManager", "Failed to generateDisplayHash. Invalid hashAlgorithm");
            sendDisplayHashError(callback, -5);
            return;
        }
        Size size = displayHashParams.getBufferSize();
        if (size != null && (size.getWidth() > 0 || size.getHeight() > 0)) {
            args.setFrameScale(size.getWidth() / boundsInWindow.width(), size.getHeight() / boundsInWindow.height());
        }
        args.setGrayscale(displayHashParams.isGrayscaleBuffer());
        SurfaceControl.ScreenshotHardwareBuffer screenshotHardwareBuffer = SurfaceControl.captureLayers(args.build());
        if (screenshotHardwareBuffer == null || screenshotHardwareBuffer.getHardwareBuffer() == null) {
            Slog.w("WindowManager", "Failed to generate DisplayHash. Couldn't capture content");
            sendDisplayHashError(callback, -1);
            return;
        }
        generateDisplayHash(screenshotHardwareBuffer.getHardwareBuffer(), boundsInWindow, hashAlgorithm, callback);
    }

    private Map<String, DisplayHashParams> getDisplayHashAlgorithms() {
        synchronized (this.mDisplayHashAlgorithmsLock) {
            Map<String, DisplayHashParams> map = this.mDisplayHashAlgorithms;
            if (map != null) {
                return map;
            }
            SyncCommand syncCommand = new SyncCommand();
            Bundle results = syncCommand.run(new BiConsumer() { // from class: com.android.server.wm.DisplayHashController$$ExternalSyntheticLambda3
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    DisplayHashController.lambda$getDisplayHashAlgorithms$2((IDisplayHashingService) obj, (RemoteCallback) obj2);
                }
            });
            this.mDisplayHashAlgorithms = new HashMap(results.size());
            for (String key : results.keySet()) {
                this.mDisplayHashAlgorithms.put(key, (DisplayHashParams) results.getParcelable(key));
            }
            return this.mDisplayHashAlgorithms;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getDisplayHashAlgorithms$2(IDisplayHashingService service, RemoteCallback remoteCallback) {
        try {
            service.getDisplayHashAlgorithms(remoteCallback);
        } catch (RemoteException e) {
            Slog.e("WindowManager", "Failed to invoke getDisplayHashAlgorithms command", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendDisplayHashError(RemoteCallback callback, int errorCode) {
        Bundle bundle = new Bundle();
        bundle.putInt("DISPLAY_HASH_ERROR_CODE", errorCode);
        callback.sendResult(bundle);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void calculateDisplayHashBoundsLocked(WindowState win, Rect boundsInWindow, Rect outBounds) {
        outBounds.set(boundsInWindow);
        DisplayContent displayContent = win.getDisplayContent();
        if (displayContent == null) {
            return;
        }
        Rect windowBounds = new Rect();
        win.getBounds(windowBounds);
        windowBounds.offsetTo(0, 0);
        outBounds.intersectUnchecked(windowBounds);
        if (outBounds.isEmpty()) {
            return;
        }
        win.getTransformationMatrix(this.mTmpFloat9, this.mTmpMatrix);
        this.mTmpRectF.set(outBounds);
        Matrix matrix = this.mTmpMatrix;
        RectF rectF = this.mTmpRectF;
        matrix.mapRect(rectF, rectF);
        outBounds.set((int) this.mTmpRectF.left, (int) this.mTmpRectF.top, (int) this.mTmpRectF.right, (int) this.mTmpRectF.bottom);
        MagnificationSpec magSpec = displayContent.getMagnificationSpec();
        if (magSpec != null) {
            outBounds.scale(magSpec.scale);
            outBounds.offset((int) magSpec.offsetX, (int) magSpec.offsetY);
        }
        if (outBounds.isEmpty()) {
            return;
        }
        Rect displayBounds = displayContent.getBounds();
        outBounds.intersectUnchecked(displayBounds);
    }

    private int getIntervalBetweenRequestMillis() {
        synchronized (this.mIntervalBetweenRequestsLock) {
            int i = this.mIntervalBetweenRequestMillis;
            if (i != -1) {
                return i;
            }
            SyncCommand syncCommand = new SyncCommand();
            Bundle results = syncCommand.run(new BiConsumer() { // from class: com.android.server.wm.DisplayHashController$$ExternalSyntheticLambda1
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    DisplayHashController.lambda$getIntervalBetweenRequestMillis$3((IDisplayHashingService) obj, (RemoteCallback) obj2);
                }
            });
            int i2 = results.getInt("android.service.displayhash.extra.INTERVAL_BETWEEN_REQUESTS", 0);
            this.mIntervalBetweenRequestMillis = i2;
            return i2;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getIntervalBetweenRequestMillis$3(IDisplayHashingService service, RemoteCallback remoteCallback) {
        try {
            service.getIntervalBetweenRequestsMillis(remoteCallback);
        } catch (RemoteException e) {
            Slog.e("WindowManager", "Failed to invoke getDisplayHashAlgorithms command", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void connectAndRun(Command command) {
        synchronized (this.mServiceConnectionLock) {
            this.mHandler.resetTimeoutMessage();
            if (this.mServiceConnection == null) {
                this.mServiceConnection = new DisplayHashingServiceConnection();
                ComponentName component = getServiceComponentName();
                if (component != null) {
                    Intent intent = new Intent();
                    intent.setComponent(component);
                    long token = Binder.clearCallingIdentity();
                    this.mContext.bindService(intent, this.mServiceConnection, 1);
                    Binder.restoreCallingIdentity(token);
                }
            }
            this.mServiceConnection.runCommandLocked(command);
        }
    }

    private ServiceInfo getServiceInfo() {
        String packageName = this.mContext.getPackageManager().getServicesSystemSharedLibraryPackageName();
        if (packageName == null) {
            Slog.w("WindowManager", "no external services package!");
            return null;
        }
        Intent intent = new Intent("android.service.displayhash.DisplayHashingService");
        intent.setPackage(packageName);
        long token = Binder.clearCallingIdentity();
        try {
            ResolveInfo resolveInfo = this.mContext.getPackageManager().resolveService(intent, 132);
            if (resolveInfo == null || resolveInfo.serviceInfo == null) {
                Slog.w("WindowManager", "No valid components found.");
                return null;
            }
            return resolveInfo.serviceInfo;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private ComponentName getServiceComponentName() {
        ServiceInfo serviceInfo = getServiceInfo();
        if (serviceInfo == null) {
            return null;
        }
        ComponentName name = new ComponentName(serviceInfo.packageName, serviceInfo.name);
        if (!"android.permission.BIND_DISPLAY_HASHING_SERVICE".equals(serviceInfo.permission)) {
            Slog.w("WindowManager", name.flattenToShortString() + " requires permission android.permission.BIND_DISPLAY_HASHING_SERVICE");
            return null;
        }
        return name;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class SyncCommand {
        private static final int WAIT_TIME_S = 5;
        private final CountDownLatch mCountDownLatch;
        private Bundle mResult;

        private SyncCommand() {
            this.mCountDownLatch = new CountDownLatch(1);
        }

        public Bundle run(final BiConsumer<IDisplayHashingService, RemoteCallback> func) {
            DisplayHashController.this.connectAndRun(new Command() { // from class: com.android.server.wm.DisplayHashController$SyncCommand$$ExternalSyntheticLambda0
                @Override // com.android.server.wm.DisplayHashController.Command
                public final void run(IDisplayHashingService iDisplayHashingService) {
                    DisplayHashController.SyncCommand.this.m7950xae4ef1f8(func, iDisplayHashingService);
                }
            });
            try {
                this.mCountDownLatch.await(5L, TimeUnit.SECONDS);
            } catch (Exception e) {
                Slog.e("WindowManager", "Failed to wait for command", e);
            }
            return this.mResult;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$run$1$com-android-server-wm-DisplayHashController$SyncCommand  reason: not valid java name */
        public /* synthetic */ void m7950xae4ef1f8(BiConsumer func, IDisplayHashingService service) throws RemoteException {
            RemoteCallback callback = new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.server.wm.DisplayHashController$SyncCommand$$ExternalSyntheticLambda1
                public final void onResult(Bundle bundle) {
                    DisplayHashController.SyncCommand.this.m7949x88bae8f7(bundle);
                }
            });
            func.accept(service, callback);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$run$0$com-android-server-wm-DisplayHashController$SyncCommand  reason: not valid java name */
        public /* synthetic */ void m7949x88bae8f7(Bundle result) {
            this.mResult = result;
            this.mCountDownLatch.countDown();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class DisplayHashingServiceConnection implements ServiceConnection {
        private ArrayList<Command> mQueuedCommands;
        private IDisplayHashingService mRemoteService;

        private DisplayHashingServiceConnection() {
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (DisplayHashController.this.mServiceConnectionLock) {
                this.mRemoteService = IDisplayHashingService.Stub.asInterface(service);
                ArrayList<Command> arrayList = this.mQueuedCommands;
                if (arrayList != null) {
                    int size = arrayList.size();
                    for (int i = 0; i < size; i++) {
                        Command queuedCommand = this.mQueuedCommands.get(i);
                        try {
                            queuedCommand.run(this.mRemoteService);
                        } catch (RemoteException e) {
                            Slog.w("WindowManager", "exception calling " + name + ": " + e);
                        }
                    }
                    this.mQueuedCommands = null;
                }
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            synchronized (DisplayHashController.this.mServiceConnectionLock) {
                this.mRemoteService = null;
            }
        }

        @Override // android.content.ServiceConnection
        public void onBindingDied(ComponentName name) {
            synchronized (DisplayHashController.this.mServiceConnectionLock) {
                this.mRemoteService = null;
            }
        }

        @Override // android.content.ServiceConnection
        public void onNullBinding(ComponentName name) {
            synchronized (DisplayHashController.this.mServiceConnectionLock) {
                this.mRemoteService = null;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void runCommandLocked(Command command) {
            IDisplayHashingService iDisplayHashingService = this.mRemoteService;
            if (iDisplayHashingService == null) {
                if (this.mQueuedCommands == null) {
                    this.mQueuedCommands = new ArrayList<>(1);
                }
                this.mQueuedCommands.add(command);
                return;
            }
            try {
                command.run(iDisplayHashingService);
            } catch (RemoteException e) {
                Slog.w("WindowManager", "exception calling service: " + e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class Handler extends android.os.Handler {
        static final int MSG_SERVICE_SHUTDOWN_TIMEOUT = 1;
        static final long SERVICE_SHUTDOWN_TIMEOUT_MILLIS = 10000;

        Handler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                synchronized (DisplayHashController.this.mServiceConnectionLock) {
                    if (DisplayHashController.this.mServiceConnection != null) {
                        DisplayHashController.this.mContext.unbindService(DisplayHashController.this.mServiceConnection);
                        DisplayHashController.this.mServiceConnection = null;
                    }
                }
            }
        }

        void resetTimeoutMessage() {
            removeMessages(1);
            sendEmptyMessageDelayed(1, 10000L);
        }
    }
}
