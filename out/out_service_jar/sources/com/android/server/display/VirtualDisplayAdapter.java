package com.android.server.display;

import android.content.Context;
import android.graphics.Point;
import android.hardware.display.IVirtualDisplayCallback;
import android.hardware.display.VirtualDisplayConfig;
import android.media.projection.IMediaProjection;
import android.media.projection.IMediaProjectionCallback;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.Slog;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceControl;
import com.android.server.display.DisplayAdapter;
import com.android.server.display.DisplayManagerService;
import com.transsion.hubcore.server.display.ITranVirtualDisplayAdapter;
import java.io.PrintWriter;
/* loaded from: classes.dex */
public class VirtualDisplayAdapter extends DisplayAdapter {
    static final boolean DEBUG = false;
    static final String TAG = "VirtualDisplayAdapter";
    static final String UNIQUE_ID_PREFIX = "virtual:";
    private final Handler mHandler;
    public IMediaProjection mIMediaProjection;
    public MediaProjectionCallback mMediaProjectionCallback;
    private final SurfaceControlDisplayFactory mSurfaceControlDisplayFactory;
    private final ArrayMap<IBinder, VirtualDisplayDevice> mVirtualDisplayDevices;

    /* loaded from: classes.dex */
    public interface SurfaceControlDisplayFactory {
        IBinder createDisplay(String str, boolean z);
    }

    @Override // com.android.server.display.DisplayAdapter
    public /* bridge */ /* synthetic */ void dumpLocked(PrintWriter printWriter) {
        super.dumpLocked(printWriter);
    }

    @Override // com.android.server.display.DisplayAdapter
    public /* bridge */ /* synthetic */ void registerLocked() {
        super.registerLocked();
    }

    public VirtualDisplayAdapter(DisplayManagerService.SyncRoot syncRoot, Context context, Handler handler, DisplayAdapter.Listener listener) {
        this(syncRoot, context, handler, listener, new SurfaceControlDisplayFactory() { // from class: com.android.server.display.VirtualDisplayAdapter$$ExternalSyntheticLambda0
            @Override // com.android.server.display.VirtualDisplayAdapter.SurfaceControlDisplayFactory
            public final IBinder createDisplay(String str, boolean z) {
                IBinder createDisplay;
                createDisplay = SurfaceControl.createDisplay(str, z);
                return createDisplay;
            }
        });
    }

    VirtualDisplayAdapter(DisplayManagerService.SyncRoot syncRoot, Context context, Handler handler, DisplayAdapter.Listener listener, SurfaceControlDisplayFactory surfaceControlDisplayFactory) {
        super(syncRoot, context, handler, listener, TAG);
        this.mVirtualDisplayDevices = new ArrayMap<>();
        this.mHandler = handler;
        this.mSurfaceControlDisplayFactory = surfaceControlDisplayFactory;
    }

    public DisplayDevice createVirtualDisplayLocked(IVirtualDisplayCallback callback, IMediaProjection projection, int ownerUid, String ownerPackageName, Surface surface, int flags, VirtualDisplayConfig virtualDisplayConfig) {
        String uniqueId;
        boolean z;
        String name = virtualDisplayConfig.getName();
        boolean secure = (flags & 4) != 0;
        IBinder appToken = callback.asBinder();
        IBinder displayToken = this.mSurfaceControlDisplayFactory.createDisplay(name, secure);
        String baseUniqueId = UNIQUE_ID_PREFIX + ownerPackageName + "," + ownerUid + "," + name + ",";
        int uniqueIndex = getNextUniqueIndex(baseUniqueId);
        String uniqueId2 = virtualDisplayConfig.getUniqueId();
        if (uniqueId2 == null) {
            uniqueId = baseUniqueId + uniqueIndex;
        } else {
            uniqueId = UNIQUE_ID_PREFIX + ownerPackageName + ":" + uniqueId2;
        }
        VirtualDisplayDevice device = new VirtualDisplayDevice(displayToken, appToken, ownerUid, ownerPackageName, surface, flags, new Callback(callback, this.mHandler), uniqueId, uniqueIndex, virtualDisplayConfig);
        this.mVirtualDisplayDevices.put(appToken, device);
        if (projection != null) {
            try {
                MediaProjectionCallback mediaProjectionCallback = new MediaProjectionCallback(appToken);
                this.mMediaProjectionCallback = mediaProjectionCallback;
                projection.registerCallback(mediaProjectionCallback);
                this.mIMediaProjection = projection;
            } catch (RemoteException e) {
                z = false;
                this.mVirtualDisplayDevices.remove(appToken);
                device.destroyLocked(z);
                return null;
            }
        }
        z = false;
        try {
            appToken.linkToDeath(device, 0);
            return device;
        } catch (RemoteException e2) {
            this.mVirtualDisplayDevices.remove(appToken);
            device.destroyLocked(z);
            return null;
        }
    }

    public void resizeVirtualDisplayLocked(IBinder appToken, int width, int height, int densityDpi) {
        VirtualDisplayDevice device = this.mVirtualDisplayDevices.get(appToken);
        if (device != null) {
            device.resizeLocked(width, height, densityDpi);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Surface getVirtualDisplaySurfaceLocked(IBinder appToken) {
        VirtualDisplayDevice device = this.mVirtualDisplayDevices.get(appToken);
        if (device != null) {
            return device.getSurfaceLocked();
        }
        return null;
    }

    public void setVirtualDisplaySurfaceLocked(IBinder appToken, Surface surface) {
        VirtualDisplayDevice device = this.mVirtualDisplayDevices.get(appToken);
        if (device != null) {
            device.setSurfaceLocked(surface);
        }
    }

    public DisplayDevice releaseVirtualDisplayLocked(IBinder appToken) {
        MediaProjectionCallback mediaProjectionCallback;
        VirtualDisplayDevice device = this.mVirtualDisplayDevices.remove(appToken);
        try {
            IMediaProjection iMediaProjection = this.mIMediaProjection;
            if (iMediaProjection != null && (mediaProjectionCallback = this.mMediaProjectionCallback) != null) {
                iMediaProjection.unregisterCallback(mediaProjectionCallback);
                this.mIMediaProjection = null;
                this.mMediaProjectionCallback = null;
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to unregisterCallback MediaProjectionCallback.", e);
        }
        if (device != null) {
            device.destroyLocked(true);
            appToken.unlinkToDeath(device, 0);
        }
        return device;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setVirtualDisplayStateLocked(IBinder appToken, boolean isOn) {
        VirtualDisplayDevice device = this.mVirtualDisplayDevices.get(appToken);
        if (device != null) {
            device.setDisplayState(isOn);
        }
    }

    private int getNextUniqueIndex(String uniqueIdPrefix) {
        if (this.mVirtualDisplayDevices.isEmpty()) {
            return 0;
        }
        int nextUniqueIndex = 0;
        for (VirtualDisplayDevice device : this.mVirtualDisplayDevices.values()) {
            if (device.getUniqueId().startsWith(uniqueIdPrefix) && device.mUniqueIndex >= nextUniqueIndex) {
                nextUniqueIndex = device.mUniqueIndex + 1;
            }
        }
        return nextUniqueIndex;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleBinderDiedLocked(IBinder appToken) {
        this.mVirtualDisplayDevices.remove(appToken);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleMediaProjectionStoppedLocked(IBinder appToken) {
        VirtualDisplayDevice device = this.mVirtualDisplayDevices.get(appToken);
        if (device != null) {
            Slog.i(TAG, "Virtual display device released because media projection stopped: " + device.mName);
            device.stopLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class VirtualDisplayDevice extends DisplayDevice implements IBinder.DeathRecipient {
        private static final int PENDING_RESIZE = 2;
        private static final int PENDING_SURFACE_CHANGE = 1;
        private static final float REFRESH_RATE = 60.0f;
        private final IBinder mAppToken;
        private final Callback mCallback;
        private int mDensityDpi;
        private int mDisplayIdToMirror;
        private int mDisplayState;
        private final int mFlags;
        private int mHeight;
        private DisplayDeviceInfo mInfo;
        private boolean mIsDisplayOn;
        private boolean mIsWindowManagerMirroring;
        private Display.Mode mMode;
        final String mName;
        final String mOwnerPackageName;
        private final int mOwnerUid;
        private int mPendingChanges;
        private boolean mStopped;
        private Surface mSurface;
        private int mUniqueIndex;
        private int mWidth;

        public VirtualDisplayDevice(IBinder displayToken, IBinder appToken, int ownerUid, String ownerPackageName, Surface surface, int flags, Callback callback, String uniqueId, int uniqueIndex, VirtualDisplayConfig virtualDisplayConfig) {
            super(VirtualDisplayAdapter.this, displayToken, uniqueId, VirtualDisplayAdapter.this.getContext());
            this.mAppToken = appToken;
            this.mOwnerUid = ownerUid;
            this.mOwnerPackageName = ownerPackageName;
            this.mName = virtualDisplayConfig.getName();
            this.mWidth = virtualDisplayConfig.getWidth();
            int height = virtualDisplayConfig.getHeight();
            this.mHeight = height;
            this.mMode = DisplayAdapter.createMode(this.mWidth, height, REFRESH_RATE);
            this.mDensityDpi = virtualDisplayConfig.getDensityDpi();
            this.mSurface = surface;
            this.mFlags = flags;
            this.mCallback = callback;
            this.mDisplayState = 0;
            this.mPendingChanges |= 1;
            this.mUniqueIndex = uniqueIndex;
            this.mIsDisplayOn = surface != null;
            this.mDisplayIdToMirror = virtualDisplayConfig.getDisplayIdToMirror();
            this.mIsWindowManagerMirroring = virtualDisplayConfig.isWindowManagerMirroring();
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (VirtualDisplayAdapter.this.getSyncRoot()) {
                VirtualDisplayAdapter.this.handleBinderDiedLocked(this.mAppToken);
                Slog.i(VirtualDisplayAdapter.TAG, "Virtual display device released because application token died: " + this.mOwnerPackageName);
                destroyLocked(false);
                VirtualDisplayAdapter.this.sendDisplayDeviceEventLocked(this, 3);
            }
        }

        public void destroyLocked(boolean binderAlive) {
            Surface surface = this.mSurface;
            if (surface != null) {
                surface.release();
                this.mSurface = null;
            }
            SurfaceControl.destroyDisplay(getDisplayTokenLocked());
            if (binderAlive) {
                this.mCallback.dispatchDisplayStopped();
            }
        }

        @Override // com.android.server.display.DisplayDevice
        public int getDisplayIdToMirrorLocked() {
            return this.mDisplayIdToMirror;
        }

        @Override // com.android.server.display.DisplayDevice
        public boolean isWindowManagerMirroringLocked() {
            return this.mIsWindowManagerMirroring;
        }

        @Override // com.android.server.display.DisplayDevice
        public void setWindowManagerMirroringLocked(boolean mirroring) {
            if (this.mIsWindowManagerMirroring != mirroring) {
                this.mIsWindowManagerMirroring = mirroring;
                VirtualDisplayAdapter.this.sendDisplayDeviceEventLocked(this, 2);
                VirtualDisplayAdapter.this.sendTraversalRequestLocked();
            }
        }

        @Override // com.android.server.display.DisplayDevice
        public Point getDisplaySurfaceDefaultSizeLocked() {
            Surface surface = this.mSurface;
            if (surface == null) {
                return null;
            }
            return surface.getDefaultSize();
        }

        Surface getSurfaceLocked() {
            return this.mSurface;
        }

        @Override // com.android.server.display.DisplayDevice
        public boolean hasStableUniqueId() {
            return false;
        }

        @Override // com.android.server.display.DisplayDevice
        public Runnable requestDisplayStateLocked(int state, float brightnessState, float sdrBrightnessState) {
            if (state != this.mDisplayState) {
                this.mDisplayState = state;
                if (state == 1) {
                    this.mCallback.dispatchDisplayPaused();
                    return null;
                }
                this.mCallback.dispatchDisplayResumed();
                return null;
            }
            return null;
        }

        @Override // com.android.server.display.DisplayDevice
        public void performTraversalLocked(SurfaceControl.Transaction t) {
            if ((this.mPendingChanges & 2) != 0) {
                t.setDisplaySize(getDisplayTokenLocked(), this.mWidth, this.mHeight);
            }
            if ((this.mPendingChanges & 1) != 0) {
                setSurfaceLocked(t, this.mSurface);
            }
            this.mPendingChanges = 0;
        }

        public void setSurfaceLocked(Surface surface) {
            Surface surface2;
            if (!this.mStopped && (surface2 = this.mSurface) != surface) {
                if ((surface2 != null) != (surface != null)) {
                    VirtualDisplayAdapter.this.sendDisplayDeviceEventLocked(this, 2);
                }
                VirtualDisplayAdapter.this.sendTraversalRequestLocked();
                this.mSurface = surface;
                this.mInfo = null;
                this.mPendingChanges |= 1;
            }
        }

        public void resizeLocked(int width, int height, int densityDpi) {
            if (this.mWidth != width || this.mHeight != height || this.mDensityDpi != densityDpi) {
                VirtualDisplayAdapter.this.sendDisplayDeviceEventLocked(this, 2);
                VirtualDisplayAdapter.this.sendTraversalRequestLocked();
                this.mWidth = width;
                this.mHeight = height;
                this.mMode = DisplayAdapter.createMode(width, height, REFRESH_RATE);
                this.mDensityDpi = densityDpi;
                this.mInfo = null;
                this.mPendingChanges |= 2;
            }
        }

        void setDisplayState(boolean isOn) {
            if (this.mIsDisplayOn != isOn) {
                this.mIsDisplayOn = isOn;
                this.mInfo = null;
                VirtualDisplayAdapter.this.sendDisplayDeviceEventLocked(this, 2);
            }
        }

        public void stopLocked() {
            setSurfaceLocked(null);
            this.mStopped = true;
        }

        @Override // com.android.server.display.DisplayDevice
        public void dumpLocked(PrintWriter pw) {
            super.dumpLocked(pw);
            pw.println("mFlags=" + this.mFlags);
            pw.println("mDisplayState=" + Display.stateToString(this.mDisplayState));
            pw.println("mStopped=" + this.mStopped);
            pw.println("mDisplayIdToMirror=" + this.mDisplayIdToMirror);
            pw.println("mWindowManagerMirroring=" + this.mIsWindowManagerMirroring);
        }

        @Override // com.android.server.display.DisplayDevice
        public DisplayDeviceInfo getDisplayDeviceInfoLocked() {
            if (this.mInfo == null) {
                DisplayDeviceInfo displayDeviceInfo = new DisplayDeviceInfo();
                this.mInfo = displayDeviceInfo;
                displayDeviceInfo.name = this.mName;
                this.mInfo.uniqueId = getUniqueId();
                this.mInfo.width = this.mWidth;
                this.mInfo.height = this.mHeight;
                this.mInfo.modeId = this.mMode.getModeId();
                this.mInfo.defaultModeId = this.mMode.getModeId();
                this.mInfo.supportedModes = new Display.Mode[]{this.mMode};
                this.mInfo.densityDpi = this.mDensityDpi;
                this.mInfo.xDpi = this.mDensityDpi;
                this.mInfo.yDpi = this.mDensityDpi;
                this.mInfo.presentationDeadlineNanos = 16666666L;
                this.mInfo.flags = 0;
                if ((this.mFlags & 1) == 0) {
                    this.mInfo.flags |= 48;
                }
                if ((this.mFlags & 16) != 0) {
                    this.mInfo.flags &= -33;
                } else {
                    this.mInfo.flags |= 128;
                    if ((this.mFlags & 2048) != 0) {
                        this.mInfo.flags |= 16384;
                    }
                }
                if ((this.mFlags & 4) != 0) {
                    this.mInfo.flags |= 4;
                }
                if ((this.mFlags & 2) != 0) {
                    this.mInfo.flags |= 64;
                    if ((this.mFlags & 1) != 0 && "portrait".equals(SystemProperties.get("persist.demo.remoterotation"))) {
                        this.mInfo.rotation = 3;
                    }
                }
                if ((this.mFlags & 32) != 0) {
                    this.mInfo.flags |= 512;
                }
                if ((this.mFlags & 128) != 0) {
                    this.mInfo.flags |= 2;
                }
                if ((this.mFlags & 256) != 0) {
                    this.mInfo.flags |= 1024;
                }
                if ((this.mFlags & 512) != 0) {
                    this.mInfo.flags |= 4096;
                }
                if ((this.mFlags & 1024) != 0) {
                    this.mInfo.flags |= 8192;
                }
                if ((this.mFlags & 4096) != 0 && (this.mInfo.flags & 16384) != 0) {
                    this.mInfo.flags |= 32768;
                }
                if ((this.mFlags & 8192) != 0) {
                    this.mInfo.flags |= 65536;
                }
                this.mInfo.flags |= ITranVirtualDisplayAdapter.Instance().updateDeviceInfoFlag(this.mFlags);
                this.mInfo.type = 5;
                this.mInfo.touch = (this.mFlags & 64) != 0 ? 3 : 0;
                this.mInfo.state = this.mIsDisplayOn ? 2 : 1;
                this.mInfo.ownerUid = this.mOwnerUid;
                this.mInfo.ownerPackageName = this.mOwnerPackageName;
            }
            return this.mInfo;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Callback extends Handler {
        private static final int MSG_ON_DISPLAY_PAUSED = 0;
        private static final int MSG_ON_DISPLAY_RESUMED = 1;
        private static final int MSG_ON_DISPLAY_STOPPED = 2;
        private final IVirtualDisplayCallback mCallback;

        public Callback(IVirtualDisplayCallback callback, Handler handler) {
            super(handler.getLooper());
            this.mCallback = callback;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case 0:
                        this.mCallback.onPaused();
                        break;
                    case 1:
                        this.mCallback.onResumed();
                        break;
                    case 2:
                        this.mCallback.onStopped();
                        break;
                }
            } catch (RemoteException e) {
                Slog.w(VirtualDisplayAdapter.TAG, "Failed to notify listener of virtual display event.", e);
            }
        }

        public void dispatchDisplayPaused() {
            sendEmptyMessage(0);
        }

        public void dispatchDisplayResumed() {
            sendEmptyMessage(1);
        }

        public void dispatchDisplayStopped() {
            sendEmptyMessage(2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class MediaProjectionCallback extends IMediaProjectionCallback.Stub {
        private IBinder mAppToken;

        public MediaProjectionCallback(IBinder appToken) {
            this.mAppToken = appToken;
        }

        public void onStop() {
            synchronized (VirtualDisplayAdapter.this.getSyncRoot()) {
                VirtualDisplayAdapter.this.handleMediaProjectionStoppedLocked(this.mAppToken);
            }
        }
    }
}
