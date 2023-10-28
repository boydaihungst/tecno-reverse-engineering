package android.view;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.BLASTBufferQueue;
import android.hardware.display.DisplayManager;
import android.view.ViewRootImpl;
/* loaded from: classes3.dex */
public final class OSSurfaceView implements ViewRootImpl.ConfigChangedCallback {
    private static final boolean DEBUG = false;
    private static final String TAG = "SurfaceView";
    private BLASTBufferQueue mBlastBufferQueue;
    private SurfaceControl mBlastSurfaceControl;
    private final DisplayManager.DisplayListener mDisplayListener;
    int mFormat;
    private Configuration mLastConfiguration;
    private int mSurfaceHeight;
    private int mSurfaceWidth;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public static class OSSurfaceViewHolder {
        private static final OSSurfaceView INSTANCE = new OSSurfaceView();

        private OSSurfaceViewHolder() {
        }
    }

    private OSSurfaceView() {
        this.mSurfaceWidth = -1;
        this.mSurfaceHeight = -1;
        this.mFormat = -1;
        this.mDisplayListener = new DisplayManager.DisplayListener() { // from class: android.view.OSSurfaceView.1
            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayChanged(int displayId) {
            }

            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayRemoved(int displayId) {
            }

            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayAdded(int displayId) {
            }

            private int toViewScreenState(int displayState) {
                return 0;
            }

            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplaySwapped(int displayId) {
            }
        };
    }

    public static final OSSurfaceView getInstance() {
        return OSSurfaceViewHolder.INSTANCE;
    }

    public void onSurfaceViewInit(Context context) {
    }

    public void onBLASTBufferQueueCreated(SurfaceControl surfaceControl, BLASTBufferQueue blastBufferQueue) {
        this.mBlastSurfaceControl = surfaceControl;
        this.mBlastBufferQueue = blastBufferQueue;
    }

    public void onSurfaceCreated() {
        addDisplayEventListener();
    }

    public void onSurfaceDestroyed() {
        removeDisplayEventListener();
        this.mBlastSurfaceControl = null;
        this.mBlastBufferQueue = null;
    }

    public void onSurfaceSizeChanged(int surfaceWidth, int surfaceHeight, int format) {
        this.mSurfaceWidth = surfaceWidth;
        this.mSurfaceHeight = surfaceHeight;
        this.mFormat = format;
    }

    private void forceUpdateBufferSize(int surfaceWidth, int surfaceHeight) {
        SurfaceControl surfaceControl;
        BLASTBufferQueue bLASTBufferQueue = this.mBlastBufferQueue;
        if (bLASTBufferQueue != null && (surfaceControl = this.mBlastSurfaceControl) != null && this.mSurfaceWidth != surfaceWidth && this.mSurfaceHeight != surfaceHeight) {
            bLASTBufferQueue.update(surfaceControl, surfaceWidth, surfaceHeight, this.mFormat);
        }
    }

    private void addDisplayEventListener() {
        ViewRootImpl.addConfigCallback(this);
    }

    private void removeDisplayEventListener() {
        ViewRootImpl.removeConfigCallback(this);
    }

    @Override // android.view.ViewRootImpl.ConfigChangedCallback
    public void onConfigurationChanged(Configuration globalConfig) {
        int i;
        int lastAppWidth = this.mSurfaceWidth;
        int lastAppHeight = this.mSurfaceHeight;
        int appWidth = this.mSurfaceWidth;
        int appHeight = this.mSurfaceHeight;
        Configuration configuration = this.mLastConfiguration;
        if (configuration != null && configuration.windowConfiguration != null && this.mLastConfiguration.windowConfiguration.getAppBounds() != null) {
            lastAppWidth = this.mLastConfiguration.windowConfiguration.getAppBounds().width();
            lastAppHeight = this.mLastConfiguration.windowConfiguration.getAppBounds().height();
        }
        if (globalConfig != null && globalConfig.windowConfiguration != null && globalConfig.windowConfiguration.getAppBounds() != null) {
            appWidth = globalConfig.windowConfiguration.getAppBounds().width();
            appHeight = globalConfig.windowConfiguration.getAppBounds().height();
        }
        int i2 = this.mSurfaceWidth;
        if (lastAppWidth == i2 && lastAppHeight == (i = this.mSurfaceHeight) && i2 != appWidth && i != appHeight) {
            this.mSurfaceWidth = appWidth;
            this.mSurfaceHeight = appHeight;
            forceUpdateBufferSize(appWidth, appHeight);
        }
        this.mLastConfiguration = globalConfig;
    }
}
