package com.android.server.vr;

import android.app.ActivityManagerInternal;
import android.app.Vr2dDisplayProperties;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.hardware.display.VirtualDisplayConfig;
import android.media.ImageReader;
import android.os.Handler;
import android.os.RemoteException;
import android.service.vr.IPersistentVrStateCallbacks;
import android.service.vr.IVrManager;
import android.util.Log;
import android.view.Surface;
import com.android.server.LocalServices;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.WindowManagerInternal;
/* loaded from: classes2.dex */
class Vr2dDisplay {
    private static final boolean DEBUG = false;
    private static final String DEBUG_ACTION_SET_MODE = "com.android.server.vr.Vr2dDisplay.SET_MODE";
    private static final String DEBUG_ACTION_SET_SURFACE = "com.android.server.vr.Vr2dDisplay.SET_SURFACE";
    private static final String DEBUG_EXTRA_MODE_ON = "com.android.server.vr.Vr2dDisplay.EXTRA_MODE_ON";
    private static final String DEBUG_EXTRA_SURFACE = "com.android.server.vr.Vr2dDisplay.EXTRA_SURFACE";
    public static final int DEFAULT_VIRTUAL_DISPLAY_DPI = 320;
    public static final int DEFAULT_VIRTUAL_DISPLAY_HEIGHT = 1800;
    public static final int DEFAULT_VIRTUAL_DISPLAY_WIDTH = 1400;
    private static final String DISPLAY_NAME = "VR 2D Display";
    public static final int MIN_VR_DISPLAY_DPI = 1;
    public static final int MIN_VR_DISPLAY_HEIGHT = 1;
    public static final int MIN_VR_DISPLAY_WIDTH = 1;
    private static final int STOP_VIRTUAL_DISPLAY_DELAY_MILLIS = 2000;
    private static final String TAG = "Vr2dDisplay";
    private static final String UNIQUE_DISPLAY_ID = "277f1a09-b88d-4d1e-8716-796f114d080b";
    private final ActivityManagerInternal mActivityManagerInternal;
    private final DisplayManager mDisplayManager;
    private ImageReader mImageReader;
    private boolean mIsPersistentVrModeEnabled;
    private boolean mIsVrModeOverrideEnabled;
    private Runnable mStopVDRunnable;
    private Surface mSurface;
    private VirtualDisplay mVirtualDisplay;
    private final IVrManager mVrManager;
    private final WindowManagerInternal mWindowManagerInternal;
    private final Object mVdLock = new Object();
    private final Handler mHandler = new Handler();
    private final IPersistentVrStateCallbacks mVrStateCallbacks = new IPersistentVrStateCallbacks.Stub() { // from class: com.android.server.vr.Vr2dDisplay.1
        public void onPersistentVrStateChanged(boolean enabled) {
            if (enabled != Vr2dDisplay.this.mIsPersistentVrModeEnabled) {
                Vr2dDisplay.this.mIsPersistentVrModeEnabled = enabled;
                Vr2dDisplay.this.updateVirtualDisplay();
            }
        }
    };
    private boolean mIsVirtualDisplayAllowed = true;
    private boolean mBootsToVr = false;
    private int mVirtualDisplayWidth = DEFAULT_VIRTUAL_DISPLAY_WIDTH;
    private int mVirtualDisplayHeight = 1800;
    private int mVirtualDisplayDpi = 320;

    public Vr2dDisplay(DisplayManager displayManager, ActivityManagerInternal activityManagerInternal, WindowManagerInternal windowManagerInternal, IVrManager vrManager) {
        this.mDisplayManager = displayManager;
        this.mActivityManagerInternal = activityManagerInternal;
        this.mWindowManagerInternal = windowManagerInternal;
        this.mVrManager = vrManager;
    }

    public void init(Context context, boolean bootsToVr) {
        startVrModeListener();
        startDebugOnlyBroadcastReceiver(context);
        this.mBootsToVr = bootsToVr;
        if (bootsToVr) {
            updateVirtualDisplay();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateVirtualDisplay() {
        if (shouldRunVirtualDisplay()) {
            Log.i(TAG, "Attempting to start virtual display");
            startVirtualDisplay();
            return;
        }
        stopVirtualDisplay();
    }

    private void startDebugOnlyBroadcastReceiver(Context context) {
    }

    private void startVrModeListener() {
        IVrManager iVrManager = this.mVrManager;
        if (iVrManager != null) {
            try {
                iVrManager.registerPersistentVrStateListener(this.mVrStateCallbacks);
            } catch (RemoteException e) {
                Log.e(TAG, "Could not register VR State listener.", e);
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:13:0x007f A[Catch: all -> 0x00af, TryCatch #0 {, blocks: (B:4:0x0003, B:9:0x0018, B:11:0x0078, B:13:0x007f, B:17:0x008c, B:20:0x0092, B:22:0x0096, B:23:0x00aa, B:24:0x00ad, B:14:0x0082, B:16:0x0089, B:10:0x004c), top: B:29:0x0003 }] */
    /* JADX WARN: Removed duplicated region for block: B:14:0x0082 A[Catch: all -> 0x00af, TryCatch #0 {, blocks: (B:4:0x0003, B:9:0x0018, B:11:0x0078, B:13:0x007f, B:17:0x008c, B:20:0x0092, B:22:0x0096, B:23:0x00aa, B:24:0x00ad, B:14:0x0082, B:16:0x0089, B:10:0x004c), top: B:29:0x0003 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void setVirtualDisplayProperties(Vr2dDisplayProperties displayProperties) {
        VirtualDisplay virtualDisplay;
        synchronized (this.mVdLock) {
            int width = displayProperties.getWidth();
            int height = displayProperties.getHeight();
            int dpi = displayProperties.getDpi();
            boolean resized = false;
            if (width >= 1 && height >= 1 && dpi >= 1) {
                Log.i(TAG, "Setting width/height/dpi to " + width + "," + height + "," + dpi);
                this.mVirtualDisplayWidth = width;
                this.mVirtualDisplayHeight = height;
                this.mVirtualDisplayDpi = dpi;
                resized = true;
                if ((displayProperties.getAddedFlags() & 1) == 1) {
                    if ((displayProperties.getRemovedFlags() & 1) == 1) {
                        this.mIsVirtualDisplayAllowed = false;
                    }
                } else {
                    this.mIsVirtualDisplayAllowed = true;
                }
                virtualDisplay = this.mVirtualDisplay;
                if (virtualDisplay != null && resized && this.mIsVirtualDisplayAllowed) {
                    virtualDisplay.resize(this.mVirtualDisplayWidth, this.mVirtualDisplayHeight, this.mVirtualDisplayDpi);
                    ImageReader oldImageReader = this.mImageReader;
                    this.mImageReader = null;
                    startImageReader();
                    oldImageReader.close();
                }
                updateVirtualDisplay();
            }
            Log.i(TAG, "Ignoring Width/Height/Dpi values of " + width + "," + height + "," + dpi);
            if ((displayProperties.getAddedFlags() & 1) == 1) {
            }
            virtualDisplay = this.mVirtualDisplay;
            if (virtualDisplay != null) {
                virtualDisplay.resize(this.mVirtualDisplayWidth, this.mVirtualDisplayHeight, this.mVirtualDisplayDpi);
                ImageReader oldImageReader2 = this.mImageReader;
                this.mImageReader = null;
                startImageReader();
                oldImageReader2.close();
            }
            updateVirtualDisplay();
        }
    }

    public int getVirtualDisplayId() {
        synchronized (this.mVdLock) {
            VirtualDisplay virtualDisplay = this.mVirtualDisplay;
            if (virtualDisplay != null) {
                int virtualDisplayId = virtualDisplay.getDisplay().getDisplayId();
                return virtualDisplayId;
            }
            return -1;
        }
    }

    private void startVirtualDisplay() {
        if (this.mDisplayManager == null) {
            Log.w(TAG, "Cannot create virtual display because mDisplayManager == null");
            return;
        }
        synchronized (this.mVdLock) {
            if (this.mVirtualDisplay != null) {
                Log.i(TAG, "VD already exists, ignoring request");
                return;
            }
            int flags = 64 | 128;
            VirtualDisplayConfig.Builder builder = new VirtualDisplayConfig.Builder(DISPLAY_NAME, this.mVirtualDisplayWidth, this.mVirtualDisplayHeight, this.mVirtualDisplayDpi);
            builder.setUniqueId(UNIQUE_DISPLAY_ID);
            builder.setFlags(flags | 1 | 8 | 256 | 4 | 1024);
            VirtualDisplay createVirtualDisplay = this.mDisplayManager.createVirtualDisplay(null, builder.build(), null, null, null);
            this.mVirtualDisplay = createVirtualDisplay;
            if (createVirtualDisplay != null) {
                updateDisplayId(createVirtualDisplay.getDisplay().getDisplayId());
                startImageReader();
                Log.i(TAG, "VD created: " + this.mVirtualDisplay);
                return;
            }
            Log.w(TAG, "Virtual display id is null after createVirtualDisplay");
            updateDisplayId(-1);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDisplayId(int displayId) {
        ((ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class)).setVr2dDisplayId(displayId);
        this.mWindowManagerInternal.setVr2dDisplayId(displayId);
    }

    private void stopVirtualDisplay() {
        if (this.mStopVDRunnable == null) {
            this.mStopVDRunnable = new Runnable() { // from class: com.android.server.vr.Vr2dDisplay.3
                @Override // java.lang.Runnable
                public void run() {
                    if (Vr2dDisplay.this.shouldRunVirtualDisplay()) {
                        Log.i(Vr2dDisplay.TAG, "Virtual Display destruction stopped: VrMode is back on.");
                        return;
                    }
                    Log.i(Vr2dDisplay.TAG, "Stopping Virtual Display");
                    synchronized (Vr2dDisplay.this.mVdLock) {
                        Vr2dDisplay.this.updateDisplayId(-1);
                        Vr2dDisplay.this.setSurfaceLocked(null);
                        if (Vr2dDisplay.this.mVirtualDisplay != null) {
                            Vr2dDisplay.this.mVirtualDisplay.release();
                            Vr2dDisplay.this.mVirtualDisplay = null;
                        }
                        Vr2dDisplay.this.stopImageReader();
                    }
                }
            };
        }
        this.mHandler.removeCallbacks(this.mStopVDRunnable);
        this.mHandler.postDelayed(this.mStopVDRunnable, 2000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setSurfaceLocked(Surface surface) {
        if (this.mSurface != surface) {
            if (surface == null || surface.isValid()) {
                Log.i(TAG, "Setting the new surface from " + this.mSurface + " to " + surface);
                VirtualDisplay virtualDisplay = this.mVirtualDisplay;
                if (virtualDisplay != null) {
                    virtualDisplay.setSurface(surface);
                }
                Surface surface2 = this.mSurface;
                if (surface2 != null) {
                    surface2.release();
                }
                this.mSurface = surface;
            }
        }
    }

    private void startImageReader() {
        if (this.mImageReader == null) {
            this.mImageReader = ImageReader.newInstance(this.mVirtualDisplayWidth, this.mVirtualDisplayHeight, 1, 2);
            Log.i(TAG, "VD startImageReader: res = " + this.mVirtualDisplayWidth + "X" + this.mVirtualDisplayHeight + ", dpi = " + this.mVirtualDisplayDpi);
        }
        synchronized (this.mVdLock) {
            setSurfaceLocked(this.mImageReader.getSurface());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopImageReader() {
        ImageReader imageReader = this.mImageReader;
        if (imageReader != null) {
            imageReader.close();
            this.mImageReader = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean shouldRunVirtualDisplay() {
        return this.mIsVirtualDisplayAllowed && (this.mBootsToVr || this.mIsPersistentVrModeEnabled || this.mIsVrModeOverrideEnabled);
    }
}
