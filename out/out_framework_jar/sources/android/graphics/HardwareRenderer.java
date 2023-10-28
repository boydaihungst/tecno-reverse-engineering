package android.graphics;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ColorSpace;
import android.graphics.HardwareRenderer;
import android.hardware.display.DisplayManager;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.TimeUtils;
import android.view.Display;
import android.view.IGraphicsStats;
import android.view.IGraphicsStatsCallback;
import android.view.NativeVectorDrawableAnimator;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.animation.AnimationUtils;
import java.io.File;
import java.io.FileDescriptor;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import sun.misc.Cleaner;
/* loaded from: classes.dex */
public class HardwareRenderer {
    private static final String CACHE_PATH_SHADERS = "com.android.opengl.shaders_cache";
    private static final String CACHE_PATH_SKIASHADERS = "com.android.skia.shaders_cache";
    public static final int FLAG_DUMP_ALL = 1;
    public static final int FLAG_DUMP_FRAMESTATS = 1;
    public static final int FLAG_DUMP_RESET = 2;
    private static final String LOG_TAG = "HardwareRenderer";
    public static final int SYNC_CONTEXT_IS_STOPPED = 4;
    public static final int SYNC_FRAME_DROPPED = 8;
    public static final int SYNC_LOST_SURFACE_REWARD_IF_FOUND = 2;
    public static final int SYNC_OK = 0;
    public static final int SYNC_REDRAW_REQUESTED = 1;
    private static int sDensityDpi = 0;
    private final long mNativeProxy;
    protected RenderNode mRootNode;
    private boolean mOpaque = true;
    private boolean mForceDark = false;
    private int mColorMode = 0;
    private FrameRenderRequest mRenderRequest = new FrameRenderRequest();

    /* loaded from: classes.dex */
    public interface ASurfaceTransactionCallback {
        boolean onMergeTransaction(long j, long j2, long j3);
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface DumpFlags {
    }

    /* loaded from: classes.dex */
    public interface FrameCommitCallback {
        void onFrameCommit(boolean z);
    }

    /* loaded from: classes.dex */
    public interface FrameCompleteCallback {
        void onFrameComplete();
    }

    /* loaded from: classes.dex */
    public interface PictureCapturedCallback {
        void onPictureCaptured(Picture picture);
    }

    /* loaded from: classes.dex */
    public interface PrepareSurfaceControlForWebviewCallback {
        void prepare();
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface SyncAndDrawResult {
    }

    public static native void disableVsync();

    /* JADX INFO: Access modifiers changed from: protected */
    public static native boolean isWebViewOverlaysEnabled();

    private static native void nAddObserver(long j, long j2);

    private static native void nAddRenderNode(long j, long j2, boolean z);

    private static native void nAllocateBuffers(long j);

    private static native void nBuildLayer(long j, long j2);

    private static native void nCancelLayerUpdate(long j, long j2);

    private static native boolean nCopyLayerInto(long j, long j2, long j3);

    private static native int nCopySurfaceInto(Surface surface, int i, int i2, int i3, int i4, long j);

    private static native Bitmap nCreateHardwareBitmap(long j, int i, int i2);

    private static native long nCreateProxy(boolean z, long j);

    private static native long nCreateRootRenderNode();

    private static native long nCreateTextureLayer(long j);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nDeleteProxy(long j);

    private static native void nDestroy(long j, long j2);

    private static native void nDestroyHardwareResources(long j);

    private static native void nDetachSurfaceTexture(long j, long j2);

    private static native void nDrawRenderNode(long j, long j2);

    private static native void nDumpGlobalProfileInfo(FileDescriptor fileDescriptor, int i);

    private static native void nDumpProfileInfo(long j, FileDescriptor fileDescriptor, int i);

    private static native void nFence(long j);

    private static native void nForceDrawNextFrame(long j);

    /* JADX INFO: Access modifiers changed from: private */
    public static native int nGetRenderThreadTid(long j);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nInitDisplayInfo(int i, int i2, float f, int i3, long j, long j2);

    private static native boolean nIsDrawingEnabled();

    private static native boolean nLoadSystemProperties(long j);

    private static native void nNotifyFramePending(long j);

    private static native void nOverrideProperty(String str, String str2);

    private static native boolean nPause(long j);

    private static native void nPushLayerUpdate(long j, long j2);

    private static native void nRegisterAnimatingRenderNode(long j, long j2);

    private static native void nRegisterVectorDrawableAnimator(long j, long j2);

    private static native void nRemoveObserver(long j, long j2);

    private static native void nRemoveRenderNode(long j, long j2);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nRotateProcessStatsBuffer();

    private static native void nSetASurfaceTransactionCallback(long j, ASurfaceTransactionCallback aSurfaceTransactionCallback);

    private static native void nSetColorMode(long j, int i);

    private static native void nSetContentDrawBounds(long j, int i, int i2, int i3, int i4);

    private static native void nSetContextPriority(int i);

    private static native void nSetDebuggingEnabled(boolean z);

    private static native void nSetDisplayDensityDpi(int i);

    private static native void nSetDrawingEnabled(boolean z);

    private static native void nSetForceDark(long j, boolean z);

    private static native void nSetFrameCallback(long j, FrameDrawingCallback frameDrawingCallback);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nSetFrameCommitCallback(long j, FrameCommitCallback frameCommitCallback);

    private static native void nSetFrameCompleteCallback(long j, FrameCompleteCallback frameCompleteCallback);

    private static native void nSetHighContrastText(boolean z);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nSetIsHighEndGfx(boolean z);

    private static native void nSetIsolatedProcess(boolean z);

    private static native void nSetLightAlpha(long j, float f, float f2);

    private static native void nSetLightGeometry(long j, float f, float f2, float f3, float f4);

    private static native void nSetName(long j, String str);

    private static native void nSetOpaque(long j, boolean z);

    private static native void nSetPictureCaptureCallback(long j, PictureCapturedCallback pictureCapturedCallback);

    private static native void nSetPrepareSurfaceControlForWebviewCallback(long j, PrepareSurfaceControlForWebviewCallback prepareSurfaceControlForWebviewCallback);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nSetProcessStatsBuffer(int i);

    private static native void nSetRtAnimationsEnabled(boolean z);

    private static native void nSetSdrWhitePoint(long j, float f);

    private static native void nSetStopped(long j, boolean z);

    private static native void nSetSurface(long j, Surface surface, boolean z);

    private static native void nSetSurfaceControl(long j, long j2);

    private static native void nStopDrawing(long j);

    private static native int nSyncAndDrawFrame(long j, long[] jArr, int i);

    private static native void nTrimMemory(int i);

    public static native void needFrameCompleteHint(boolean z);

    public static native void preload();

    protected static native void setupShadersDiskCache(String str, String str2);

    public HardwareRenderer() {
        ProcessInitializer.sInstance.initUsingContext();
        RenderNode adopt = RenderNode.adopt(nCreateRootRenderNode());
        this.mRootNode = adopt;
        adopt.setClipToBounds(false);
        long nCreateProxy = nCreateProxy(true ^ this.mOpaque, this.mRootNode.mNativeRenderNode);
        this.mNativeProxy = nCreateProxy;
        if (nCreateProxy == 0) {
            throw new OutOfMemoryError("Unable to create hardware renderer");
        }
        Cleaner.create(this, new DestroyContextRunnable(nCreateProxy));
        ProcessInitializer.sInstance.init(nCreateProxy);
    }

    public void destroy() {
        nDestroy(this.mNativeProxy, this.mRootNode.mNativeRenderNode);
    }

    public void setName(String name) {
        nSetName(this.mNativeProxy, name);
    }

    public void setLightSourceGeometry(float lightX, float lightY, float lightZ, float lightRadius) {
        validateFinite(lightX, "lightX");
        validateFinite(lightY, "lightY");
        validatePositive(lightZ, "lightZ");
        validatePositive(lightRadius, "lightRadius");
        nSetLightGeometry(this.mNativeProxy, lightX, lightY, lightZ, lightRadius);
    }

    public void setLightSourceAlpha(float ambientShadowAlpha, float spotShadowAlpha) {
        validateAlpha(ambientShadowAlpha, "ambientShadowAlpha");
        validateAlpha(spotShadowAlpha, "spotShadowAlpha");
        nSetLightAlpha(this.mNativeProxy, ambientShadowAlpha, spotShadowAlpha);
    }

    public void setContentRoot(RenderNode content) {
        RecordingCanvas canvas = this.mRootNode.beginRecording();
        if (content != null) {
            canvas.drawRenderNode(content);
        }
        this.mRootNode.endRecording();
    }

    public void setSurface(Surface surface) {
        setSurface(surface, false);
    }

    public void setSurface(Surface surface, boolean discardBuffer) {
        if (surface != null && !surface.isValid()) {
            throw new IllegalArgumentException("Surface is invalid. surface.isValid() == false.");
        }
        nSetSurface(this.mNativeProxy, surface, discardBuffer);
    }

    public void setSurfaceControl(SurfaceControl surfaceControl) {
        nSetSurfaceControl(this.mNativeProxy, surfaceControl != null ? surfaceControl.mNativeObject : 0L);
    }

    /* loaded from: classes.dex */
    public final class FrameRenderRequest {
        private FrameInfo mFrameInfo;
        private boolean mWaitForPresent;

        private FrameRenderRequest() {
            this.mFrameInfo = new FrameInfo();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void reset() {
            this.mWaitForPresent = false;
            HardwareRenderer.this.mRenderRequest.setVsyncTime(AnimationUtils.currentAnimationTimeMillis() * TimeUtils.NANOS_PER_MS);
        }

        public void setFrameInfo(FrameInfo info) {
            System.arraycopy(info.frameInfo, 0, this.mFrameInfo.frameInfo, 0, info.frameInfo.length);
        }

        public FrameRenderRequest setVsyncTime(long vsyncTime) {
            this.mFrameInfo.setVsync(vsyncTime, vsyncTime, -1L, Long.MAX_VALUE, vsyncTime, -1L);
            this.mFrameInfo.addFlags(4L);
            return this;
        }

        public FrameRenderRequest setFrameCommitCallback(final Executor executor, final Runnable frameCommitCallback) {
            HardwareRenderer.nSetFrameCommitCallback(HardwareRenderer.this.mNativeProxy, new FrameCommitCallback() { // from class: android.graphics.HardwareRenderer$FrameRenderRequest$$ExternalSyntheticLambda0
                @Override // android.graphics.HardwareRenderer.FrameCommitCallback
                public final void onFrameCommit(boolean z) {
                    executor.execute(frameCommitCallback);
                }
            });
            return this;
        }

        public FrameRenderRequest setWaitForPresent(boolean shouldWait) {
            this.mWaitForPresent = shouldWait;
            return this;
        }

        public int syncAndDraw() {
            int syncResult = HardwareRenderer.this.syncAndDrawFrame(this.mFrameInfo);
            if (this.mWaitForPresent && (syncResult & 8) == 0) {
                HardwareRenderer.this.fence();
            }
            return syncResult;
        }
    }

    public FrameRenderRequest createRenderRequest() {
        this.mRenderRequest.reset();
        return this.mRenderRequest;
    }

    public int syncAndDrawFrame(FrameInfo frameInfo) {
        return nSyncAndDrawFrame(this.mNativeProxy, frameInfo.frameInfo, frameInfo.frameInfo.length);
    }

    public boolean pause() {
        return nPause(this.mNativeProxy);
    }

    public void setStopped(boolean stopped) {
        nSetStopped(this.mNativeProxy, stopped);
    }

    public void stop() {
        nSetStopped(this.mNativeProxy, true);
    }

    public void start() {
        nSetStopped(this.mNativeProxy, false);
    }

    public void clearContent() {
        nDestroyHardwareResources(this.mNativeProxy);
    }

    public boolean setForceDark(boolean enable) {
        if (this.mForceDark != enable) {
            this.mForceDark = enable;
            nSetForceDark(this.mNativeProxy, enable);
            return true;
        }
        return false;
    }

    public void allocateBuffers() {
        nAllocateBuffers(this.mNativeProxy);
    }

    public void notifyFramePending() {
        nNotifyFramePending(this.mNativeProxy);
    }

    public void setOpaque(boolean opaque) {
        if (this.mOpaque != opaque) {
            this.mOpaque = opaque;
            nSetOpaque(this.mNativeProxy, opaque);
        }
    }

    public boolean isOpaque() {
        return this.mOpaque;
    }

    public void setFrameCommitCallback(FrameCommitCallback callback) {
        nSetFrameCommitCallback(this.mNativeProxy, callback);
    }

    public void setFrameCompleteCallback(FrameCompleteCallback callback) {
        nSetFrameCompleteCallback(this.mNativeProxy, callback);
    }

    public void addObserver(HardwareRendererObserver observer) {
        nAddObserver(this.mNativeProxy, observer.getNativeInstance());
    }

    public void removeObserver(HardwareRendererObserver observer) {
        nRemoveObserver(this.mNativeProxy, observer.getNativeInstance());
    }

    public void setColorMode(int colorMode) {
        if (this.mColorMode != colorMode) {
            this.mColorMode = colorMode;
            nSetColorMode(this.mNativeProxy, colorMode);
        }
    }

    public void setColorMode(int colorMode, float whitePoint) {
        nSetSdrWhitePoint(this.mNativeProxy, whitePoint);
        this.mColorMode = colorMode;
        nSetColorMode(this.mNativeProxy, colorMode);
    }

    public void fence() {
        nFence(this.mNativeProxy);
    }

    public void registerAnimatingRenderNode(RenderNode animator) {
        nRegisterAnimatingRenderNode(this.mRootNode.mNativeRenderNode, animator.mNativeRenderNode);
    }

    public void registerVectorDrawableAnimator(NativeVectorDrawableAnimator animator) {
        nRegisterVectorDrawableAnimator(this.mRootNode.mNativeRenderNode, animator.getAnimatorNativePtr());
    }

    public void stopDrawing() {
        nStopDrawing(this.mNativeProxy);
    }

    public TextureLayer createTextureLayer() {
        long layer = nCreateTextureLayer(this.mNativeProxy);
        return TextureLayer.adoptTextureLayer(this, layer);
    }

    public void detachSurfaceTexture(long hardwareLayer) {
        nDetachSurfaceTexture(this.mNativeProxy, hardwareLayer);
    }

    public void buildLayer(RenderNode node) {
        if (node.hasDisplayList()) {
            nBuildLayer(this.mNativeProxy, node.mNativeRenderNode);
        }
    }

    public boolean copyLayerInto(TextureLayer layer, Bitmap bitmap) {
        return nCopyLayerInto(this.mNativeProxy, layer.getDeferredLayerUpdater(), bitmap.getNativeInstance());
    }

    public void pushLayerUpdate(TextureLayer layer) {
        nPushLayerUpdate(this.mNativeProxy, layer.getDeferredLayerUpdater());
    }

    public void onLayerDestroyed(TextureLayer layer) {
        nCancelLayerUpdate(this.mNativeProxy, layer.getDeferredLayerUpdater());
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setASurfaceTransactionCallback(ASurfaceTransactionCallback callback) {
        nSetASurfaceTransactionCallback(this.mNativeProxy, callback);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setPrepareSurfaceControlForWebviewCallback(PrepareSurfaceControlForWebviewCallback callback) {
        nSetPrepareSurfaceControlForWebviewCallback(this.mNativeProxy, callback);
    }

    public void setFrameCallback(FrameDrawingCallback callback) {
        nSetFrameCallback(this.mNativeProxy, callback);
    }

    public void addRenderNode(RenderNode node, boolean placeFront) {
        nAddRenderNode(this.mNativeProxy, node.mNativeRenderNode, placeFront);
    }

    public void removeRenderNode(RenderNode node) {
        nRemoveRenderNode(this.mNativeProxy, node.mNativeRenderNode);
    }

    public void drawRenderNode(RenderNode node) {
        nDrawRenderNode(this.mNativeProxy, node.mNativeRenderNode);
    }

    public boolean loadSystemProperties() {
        return nLoadSystemProperties(this.mNativeProxy);
    }

    public static void dumpGlobalProfileInfo(FileDescriptor fd, int dumpFlags) {
        nDumpGlobalProfileInfo(fd, dumpFlags);
    }

    public void dumpProfileInfo(FileDescriptor fd, int dumpFlags) {
        nDumpProfileInfo(this.mNativeProxy, fd, dumpFlags);
    }

    public void setContentDrawBounds(int left, int top, int right, int bottom) {
        nSetContentDrawBounds(this.mNativeProxy, left, top, right, bottom);
    }

    public void forceDrawNextFrame() {
        nForceDrawNextFrame(this.mNativeProxy);
    }

    public void setPictureCaptureCallback(PictureCapturedCallback callback) {
        nSetPictureCaptureCallback(this.mNativeProxy, callback);
    }

    static void invokePictureCapturedCallback(long picturePtr, PictureCapturedCallback callback) {
        Picture picture = new Picture(picturePtr);
        callback.onPictureCaptured(picture);
    }

    /* loaded from: classes.dex */
    public interface FrameDrawingCallback {
        void onFrameDraw(long j);

        default FrameCommitCallback onFrameDraw(int syncResult, long frame) {
            onFrameDraw(frame);
            return null;
        }
    }

    private static void validateAlpha(float alpha, String argumentName) {
        if (alpha < 0.0f || alpha > 1.0f) {
            throw new IllegalArgumentException(argumentName + " must be a valid alpha, " + alpha + " is not in the range of 0.0f to 1.0f");
        }
    }

    private static void validatePositive(float f, String argumentName) {
        if (!Float.isFinite(f) || f < 0.0f) {
            throw new IllegalArgumentException(argumentName + " must be a finite positive, given=" + f);
        }
    }

    private static void validateFinite(float f, String argumentName) {
        if (!Float.isFinite(f)) {
            throw new IllegalArgumentException(argumentName + " must be finite, given=" + f);
        }
    }

    public static void setFPSDivisor(int divisor) {
        nSetRtAnimationsEnabled(divisor <= 1);
    }

    public static void setContextPriority(int priority) {
        nSetContextPriority(priority);
    }

    public static void setHighContrastText(boolean highContrastText) {
        nSetHighContrastText(highContrastText);
    }

    public static void setIsolatedProcess(boolean isIsolated) {
        nSetIsolatedProcess(isIsolated);
        ProcessInitializer.sInstance.setIsolated(isIsolated);
    }

    public static void sendDeviceConfigurationForDebugging(Configuration config) {
        if (config.densityDpi != 0 && config.densityDpi != sDensityDpi) {
            sDensityDpi = config.densityDpi;
            nSetDisplayDensityDpi(config.densityDpi);
        }
    }

    public static void setDebuggingEnabled(boolean enable) {
        nSetDebuggingEnabled(enable);
    }

    public static int copySurfaceInto(Surface surface, Rect srcRect, Bitmap bitmap) {
        if (srcRect == null) {
            return nCopySurfaceInto(surface, 0, 0, 0, 0, bitmap.getNativeInstance());
        }
        return nCopySurfaceInto(surface, srcRect.left, srcRect.top, srcRect.right, srcRect.bottom, bitmap.getNativeInstance());
    }

    public static Bitmap createHardwareBitmap(RenderNode node, int width, int height) {
        return nCreateHardwareBitmap(node.mNativeRenderNode, width, height);
    }

    public static void trimMemory(int level) {
        nTrimMemory(level);
    }

    public static void overrideProperty(String name, String value) {
        if (name == null || value == null) {
            throw new IllegalArgumentException("name and value must be non-null");
        }
        nOverrideProperty(name, value);
    }

    public static void setupDiskCache(File cacheDir) {
        setupShadersDiskCache(new File(cacheDir, CACHE_PATH_SHADERS).getAbsolutePath(), new File(cacheDir, CACHE_PATH_SKIASHADERS).getAbsolutePath());
    }

    public static void setPackageName(String packageName) {
        ProcessInitializer.sInstance.setPackageName(packageName);
    }

    public static void setContextForInit(Context context) {
        ProcessInitializer.sInstance.setContext(context);
    }

    public static boolean isDrawingEnabled() {
        return nIsDrawingEnabled();
    }

    public static void setDrawingEnabled(boolean drawingEnabled) {
        nSetDrawingEnabled(drawingEnabled);
    }

    public static void setRtAnimationsEnabled(boolean enabled) {
        nSetRtAnimationsEnabled(enabled);
    }

    /* loaded from: classes.dex */
    private static final class DestroyContextRunnable implements Runnable {
        private final long mNativeInstance;

        DestroyContextRunnable(long nativeInstance) {
            this.mNativeInstance = nativeInstance;
        }

        @Override // java.lang.Runnable
        public void run() {
            HardwareRenderer.nDeleteProxy(this.mNativeInstance);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ProcessInitializer {
        private static final int INTERNAL_DATASPACE_DISPLAY_P3 = 143261696;
        private static final int INTERNAL_DATASPACE_SCRGB = 411107328;
        private static final int INTERNAL_DATASPACE_SRGB = 142671872;
        static ProcessInitializer sInstance = new ProcessInitializer();
        private Context mContext;
        private IGraphicsStats mGraphicsStatsService;
        private String mPackageName;
        private boolean mInitialized = false;
        private boolean mDisplayInitialized = false;
        private boolean mIsolated = false;
        private IGraphicsStatsCallback mGraphicsStatsCallback = new IGraphicsStatsCallback.Stub() { // from class: android.graphics.HardwareRenderer.ProcessInitializer.1
            @Override // android.view.IGraphicsStatsCallback
            public void onRotateGraphicsStatsBuffer() throws RemoteException {
                ProcessInitializer.this.rotateBuffer();
            }
        };

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public enum Dataspace {
            DISPLAY_P3(ColorSpace.Named.DISPLAY_P3, 143261696),
            SCRGB(ColorSpace.Named.EXTENDED_SRGB, 411107328),
            SRGB(ColorSpace.Named.SRGB, 142671872);
            
            private final ColorSpace.Named mColorSpace;
            private final int mNativeDataspace;

            Dataspace(ColorSpace.Named colorSpace, int nativeDataspace) {
                this.mColorSpace = colorSpace;
                this.mNativeDataspace = nativeDataspace;
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            public static Optional<Dataspace> find(final ColorSpace colorSpace) {
                return Stream.of((Object[]) values()).filter(new Predicate() { // from class: android.graphics.HardwareRenderer$ProcessInitializer$Dataspace$$ExternalSyntheticLambda0
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        boolean equals;
                        equals = ColorSpace.get(((HardwareRenderer.ProcessInitializer.Dataspace) obj).mColorSpace).equals(ColorSpace.this);
                        return equals;
                    }
                }).findFirst();
            }
        }

        private ProcessInitializer() {
        }

        synchronized void setPackageName(String name) {
            if (this.mInitialized) {
                return;
            }
            this.mPackageName = name;
        }

        synchronized void setIsolated(boolean isolated) {
            if (this.mInitialized) {
                return;
            }
            this.mIsolated = isolated;
        }

        synchronized void setContext(Context context) {
            if (this.mInitialized) {
                return;
            }
            this.mContext = context;
        }

        synchronized void init(long renderProxy) {
            if (this.mInitialized) {
                return;
            }
            this.mInitialized = true;
            initSched(renderProxy);
            initGraphicsStats();
        }

        private void initSched(long renderProxy) {
            try {
                int tid = HardwareRenderer.nGetRenderThreadTid(renderProxy);
                ActivityManager.getService().setRenderThread(tid);
            } catch (Throwable t) {
                Log.w(HardwareRenderer.LOG_TAG, "Failed to set scheduler for RenderThread", t);
            }
        }

        private void initGraphicsStats() {
            if (this.mPackageName == null) {
                return;
            }
            try {
                IBinder binder = ServiceManager.getService(GraphicsStatsService.GRAPHICS_STATS_SERVICE);
                if (binder == null) {
                    return;
                }
                this.mGraphicsStatsService = IGraphicsStats.Stub.asInterface(binder);
                requestBuffer();
            } catch (Throwable t) {
                Log.w(HardwareRenderer.LOG_TAG, "Could not acquire gfx stats buffer", t);
            }
        }

        synchronized void initUsingContext() {
            if (this.mContext == null) {
                return;
            }
            initDisplayInfo();
            HardwareRenderer.nSetIsHighEndGfx(ActivityManager.isHighEndGfx());
            this.mContext = null;
        }

        private void initDisplayInfo() {
            if (this.mDisplayInitialized) {
                return;
            }
            if (this.mIsolated) {
                this.mDisplayInitialized = true;
                return;
            }
            DisplayManager dm = (DisplayManager) this.mContext.getSystemService(Context.DISPLAY_SERVICE);
            if (dm == null) {
                Log.d(HardwareRenderer.LOG_TAG, "Failed to find DisplayManager for display-based configuration");
                return;
            }
            Display display = dm.getDisplay(0);
            if (display == null) {
                Log.d(HardwareRenderer.LOG_TAG, "Failed to find default display for display-based configuration");
                return;
            }
            Dataspace wideColorDataspace = (Dataspace) Optional.ofNullable(display.getPreferredWideGamutColorSpace()).flatMap(new Function() { // from class: android.graphics.HardwareRenderer$ProcessInitializer$$ExternalSyntheticLambda0
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return HardwareRenderer.ProcessInitializer.Dataspace.find((ColorSpace) obj);
                }
            }).orElse(Dataspace.SRGB);
            Display.Mode activeMode = display.getMode();
            HardwareRenderer.nInitDisplayInfo(activeMode.getPhysicalWidth(), activeMode.getPhysicalHeight(), display.getRefreshRate(), wideColorDataspace.mNativeDataspace, display.getAppVsyncOffsetNanos(), display.getPresentationDeadlineNanos());
            this.mDisplayInitialized = true;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void rotateBuffer() {
            HardwareRenderer.nRotateProcessStatsBuffer();
            requestBuffer();
        }

        private void requestBuffer() {
            try {
                ParcelFileDescriptor pfd = this.mGraphicsStatsService.requestBufferForProcess(this.mPackageName, this.mGraphicsStatsCallback);
                HardwareRenderer.nSetProcessStatsBuffer(pfd.getFd());
                pfd.close();
            } catch (Throwable t) {
                Log.w(HardwareRenderer.LOG_TAG, "Could not acquire gfx stats buffer", t);
            }
        }
    }
}
