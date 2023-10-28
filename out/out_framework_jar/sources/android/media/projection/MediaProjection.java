package android.media.projection;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.hardware.display.VirtualDisplayConfig;
import android.media.projection.IMediaProjectionCallback;
import android.media.projection.IMediaProjectionManager;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.ArrayMap;
import android.util.Log;
import android.view.ContentRecordingSession;
import android.view.Surface;
import android.window.WindowContainerToken;
import java.util.Map;
/* loaded from: classes2.dex */
public final class MediaProjection {
    private static final String TAG = "MediaProjection";
    private final Context mContext;
    private final IMediaProjection mImpl;
    private IMediaProjectionManager mProjectionService = null;
    private final Map<Callback, CallbackRecord> mCallbacks = new ArrayMap();

    public MediaProjection(Context context, IMediaProjection impl) {
        this.mContext = context;
        this.mImpl = impl;
        try {
            impl.start(new MediaProjectionCallback());
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to start media projection", e);
        }
    }

    public void registerCallback(Callback callback, Handler handler) {
        if (callback == null) {
            throw new IllegalArgumentException("callback should not be null");
        }
        if (handler == null) {
            handler = new Handler();
        }
        this.mCallbacks.put(callback, new CallbackRecord(callback, handler));
    }

    public void unregisterCallback(Callback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback should not be null");
        }
        this.mCallbacks.remove(callback);
    }

    public VirtualDisplay createVirtualDisplay(String name, int width, int height, int dpi, boolean isSecure, Surface surface, VirtualDisplay.Callback callback, Handler handler) {
        int flags = 18;
        if (isSecure) {
            flags = 18 | 4;
        }
        VirtualDisplayConfig.Builder builder = new VirtualDisplayConfig.Builder(name, width, height, dpi).setFlags(flags);
        if (surface != null) {
            builder.setSurface(surface);
        }
        VirtualDisplay virtualDisplay = createVirtualDisplay(builder, callback, handler);
        return virtualDisplay;
    }

    public VirtualDisplay createVirtualDisplay(String name, int width, int height, int dpi, int flags, Surface surface, VirtualDisplay.Callback callback, Handler handler) {
        VirtualDisplayConfig.Builder builder = new VirtualDisplayConfig.Builder(name, width, height, dpi).setFlags(flags);
        if (surface != null) {
            builder.setSurface(surface);
        }
        VirtualDisplay virtualDisplay = createVirtualDisplay(builder, callback, handler);
        return virtualDisplay;
    }

    public VirtualDisplay createVirtualDisplay(VirtualDisplayConfig.Builder virtualDisplayConfig, VirtualDisplay.Callback callback, Handler handler) {
        ContentRecordingSession session;
        try {
            WindowContainerToken taskWindowContainerToken = this.mImpl.getTaskRecordingWindowContainerToken();
            Context windowContext = null;
            if (taskWindowContainerToken == null) {
                Context context = this.mContext;
                windowContext = context.createWindowContext(context.getDisplayNoVerify(), 2, null);
                session = ContentRecordingSession.createDisplaySession(windowContext.getWindowContextToken());
            } else {
                session = ContentRecordingSession.createTaskSession(taskWindowContainerToken.asBinder());
            }
            virtualDisplayConfig.setWindowManagerMirroring(true);
            DisplayManager dm = (DisplayManager) this.mContext.getSystemService(DisplayManager.class);
            VirtualDisplay virtualDisplay = dm.createVirtualDisplay(this, virtualDisplayConfig.build(), callback, handler, windowContext);
            if (virtualDisplay == null) {
                return null;
            }
            session.setDisplayId(virtualDisplay.getDisplay().getDisplayId());
            getProjectionService().setContentRecordingSession(session, this.mImpl);
            return virtualDisplay;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private IMediaProjectionManager getProjectionService() {
        if (this.mProjectionService == null) {
            this.mProjectionService = IMediaProjectionManager.Stub.asInterface(ServiceManager.getService(Context.MEDIA_PROJECTION_SERVICE));
        }
        return this.mProjectionService;
    }

    public void stop() {
        try {
            this.mImpl.stop();
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to stop projection", e);
        }
    }

    public IMediaProjection getProjection() {
        return this.mImpl;
    }

    /* loaded from: classes2.dex */
    public static abstract class Callback {
        public void onStop() {
        }
    }

    /* loaded from: classes2.dex */
    private final class MediaProjectionCallback extends IMediaProjectionCallback.Stub {
        private MediaProjectionCallback() {
        }

        @Override // android.media.projection.IMediaProjectionCallback
        public void onStop() {
            for (CallbackRecord cbr : MediaProjection.this.mCallbacks.values()) {
                cbr.onStop();
            }
        }
    }

    /* loaded from: classes2.dex */
    private static final class CallbackRecord {
        private final Callback mCallback;
        private final Handler mHandler;

        public CallbackRecord(Callback callback, Handler handler) {
            this.mCallback = callback;
            this.mHandler = handler;
        }

        public void onStop() {
            this.mHandler.post(new Runnable() { // from class: android.media.projection.MediaProjection.CallbackRecord.1
                @Override // java.lang.Runnable
                public void run() {
                    CallbackRecord.this.mCallback.onStop();
                }
            });
        }
    }
}
