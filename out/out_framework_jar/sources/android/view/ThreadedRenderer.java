package android.view;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.BLASTBufferQueue;
import android.graphics.FrameInfo;
import android.graphics.HardwareRenderer;
import android.graphics.Picture;
import android.graphics.Point;
import android.graphics.RecordingCanvas;
import android.graphics.Rect;
import android.graphics.RenderNode;
import android.os.Trace;
import android.util.Log;
import android.util.TimeUtils;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.ThreadedRenderer;
import android.view.View;
import android.view.animation.AnimationUtils;
import com.android.internal.R;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
/* loaded from: classes3.dex */
public final class ThreadedRenderer extends HardwareRenderer {
    public static final String DEBUG_DIRTY_REGIONS_PROPERTY = "debug.hwui.show_dirty_regions";
    public static final String DEBUG_FORCE_DARK = "debug.hwui.force_dark";
    public static final String DEBUG_FPS_DIVISOR = "debug.hwui.fps_divisor";
    public static final String DEBUG_OVERDRAW_PROPERTY = "debug.hwui.overdraw";
    public static final String DEBUG_SHOW_LAYERS_UPDATES_PROPERTY = "debug.hwui.show_layers_updates";
    public static final String DEBUG_SHOW_NON_RECTANGULAR_CLIP_PROPERTY = "debug.hwui.show_non_rect_clip";
    public static final String OVERDRAW_PROPERTY_SHOW = "show";
    static final String PRINT_CONFIG_PROPERTY = "debug.hwui.print_config";
    static final String PROFILE_MAXFRAMES_PROPERTY = "debug.hwui.profile.maxframes";
    public static final String PROFILE_PROPERTY = "debug.hwui.profile";
    private boolean mEnabled;
    private int mHeight;
    private int mInsetLeft;
    private int mInsetTop;
    private final float mLightRadius;
    private final float mLightY;
    private final float mLightZ;
    private ArrayList<HardwareRenderer.FrameDrawingCallback> mNextRtFrameCallbacks;
    private boolean mRootNodeNeedsUpdate;
    private int mSurfaceHeight;
    private int mSurfaceWidth;
    private int mWidth;
    public static int EGL_CONTEXT_PRIORITY_REALTIME_NV = 13143;
    public static int EGL_CONTEXT_PRIORITY_HIGH_IMG = 12545;
    public static int EGL_CONTEXT_PRIORITY_MEDIUM_IMG = 12546;
    public static int EGL_CONTEXT_PRIORITY_LOW_IMG = 12547;
    public static boolean sRendererEnabled = true;
    public static boolean sTrimForeground = false;
    public static final String PROFILE_PROPERTY_VISUALIZE_BARS = "visual_bars";
    private static final String[] VISUALIZERS = {PROFILE_PROPERTY_VISUALIZE_BARS};
    private boolean mInitialized = false;
    private boolean mRequested = true;
    private final WebViewOverlayProvider mWebViewOverlayProvider = new WebViewOverlayProvider();
    private boolean mWebViewOverlaysEnabled = false;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public interface DrawCallbacks {
        void onPostDraw(RecordingCanvas recordingCanvas);

        void onPreDraw(RecordingCanvas recordingCanvas);
    }

    static /* synthetic */ boolean access$000() {
        return isWebViewOverlaysEnabled();
    }

    public static void enableForegroundTrimming() {
        sTrimForeground = true;
    }

    public static void initForSystemProcess() {
        if (!ActivityManager.isHighEndGfx()) {
            sRendererEnabled = false;
        } else {
            enableForegroundTrimming();
        }
    }

    public static ThreadedRenderer create(Context context, boolean translucent, String name) {
        return new ThreadedRenderer(context, translucent, name);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public static final class WebViewOverlayProvider implements HardwareRenderer.PrepareSurfaceControlForWebviewCallback, HardwareRenderer.ASurfaceTransactionCallback {
        private static final boolean sOverlaysAreEnabled = ThreadedRenderer.access$000();
        private BLASTBufferQueue mBLASTBufferQueue;
        private boolean mHasWebViewOverlays;
        private SurfaceControl mSurfaceControl;
        private final SurfaceControl.Transaction mTransaction;

        private WebViewOverlayProvider() {
            this.mTransaction = new SurfaceControl.Transaction();
            this.mHasWebViewOverlays = false;
        }

        public boolean setSurfaceControlOpaque(boolean opaque) {
            synchronized (this) {
                if (this.mHasWebViewOverlays) {
                    return false;
                }
                this.mTransaction.setOpaque(this.mSurfaceControl, opaque).apply();
                return opaque;
            }
        }

        public boolean shouldEnableOverlaySupport() {
            return (!sOverlaysAreEnabled || this.mSurfaceControl == null || this.mBLASTBufferQueue == null) ? false : true;
        }

        public void setSurfaceControl(SurfaceControl surfaceControl) {
            synchronized (this) {
                this.mSurfaceControl = surfaceControl;
                if (surfaceControl != null && this.mHasWebViewOverlays) {
                    this.mTransaction.setOpaque(surfaceControl, false).apply();
                }
            }
        }

        public void setBLASTBufferQueue(BLASTBufferQueue bufferQueue) {
            synchronized (this) {
                this.mBLASTBufferQueue = bufferQueue;
            }
        }

        @Override // android.graphics.HardwareRenderer.PrepareSurfaceControlForWebviewCallback
        public void prepare() {
            synchronized (this) {
                this.mHasWebViewOverlays = true;
                SurfaceControl surfaceControl = this.mSurfaceControl;
                if (surfaceControl != null) {
                    this.mTransaction.setOpaque(surfaceControl, false).apply();
                }
            }
        }

        @Override // android.graphics.HardwareRenderer.ASurfaceTransactionCallback
        public boolean onMergeTransaction(long nativeTransactionObj, long aSurfaceControlNativeObj, long frameNr) {
            synchronized (this) {
                BLASTBufferQueue bLASTBufferQueue = this.mBLASTBufferQueue;
                if (bLASTBufferQueue == null) {
                    return false;
                }
                bLASTBufferQueue.mergeWithNextTransaction(nativeTransactionObj, frameNr);
                return true;
            }
        }
    }

    ThreadedRenderer(Context context, boolean translucent, String name) {
        setName(name);
        setOpaque(!translucent);
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.Lighting, 0, 0);
        this.mLightY = a.getDimension(3, 0.0f);
        this.mLightZ = a.getDimension(4, 0.0f);
        this.mLightRadius = a.getDimension(2, 0.0f);
        float ambientShadowAlpha = a.getFloat(0, 0.0f);
        float spotShadowAlpha = a.getFloat(1, 0.0f);
        a.recycle();
        setLightSourceAlpha(ambientShadowAlpha, spotShadowAlpha);
    }

    @Override // android.graphics.HardwareRenderer
    public void destroy() {
        this.mInitialized = false;
        updateEnabledState(null);
        super.destroy();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isEnabled() {
        return this.mEnabled;
    }

    void setEnabled(boolean enabled) {
        this.mEnabled = enabled;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isRequested() {
        return this.mRequested;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setRequested(boolean requested) {
        this.mRequested = requested;
    }

    private void updateEnabledState(Surface surface) {
        if (surface == null || !surface.isValid()) {
            setEnabled(false);
        } else {
            setEnabled(this.mInitialized);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean initialize(Surface surface) throws Surface.OutOfResourcesException {
        boolean status = !this.mInitialized;
        this.mInitialized = true;
        updateEnabledState(surface);
        setSurface(surface);
        return status;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean initializeIfNeeded(int width, int height, View.AttachInfo attachInfo, Surface surface, Rect surfaceInsets) throws Surface.OutOfResourcesException {
        if (isRequested() && !isEnabled() && initialize(surface)) {
            setup(width, height, attachInfo, surfaceInsets);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateSurface(Surface surface) throws Surface.OutOfResourcesException {
        updateEnabledState(surface);
        setSurface(surface);
    }

    @Override // android.graphics.HardwareRenderer
    public void setSurface(Surface surface) {
        if (surface != null && surface.isValid()) {
            super.setSurface(surface);
        } else {
            super.setSurface(null);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerRtFrameCallback(HardwareRenderer.FrameDrawingCallback callback) {
        if (this.mNextRtFrameCallbacks == null) {
            this.mNextRtFrameCallbacks = new ArrayList<>();
        }
        this.mNextRtFrameCallbacks.add(callback);
    }

    void unregisterRtFrameCallback(HardwareRenderer.FrameDrawingCallback callback) {
        ArrayList<HardwareRenderer.FrameDrawingCallback> arrayList = this.mNextRtFrameCallbacks;
        if (arrayList == null) {
            return;
        }
        arrayList.remove(callback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void destroyHardwareResources(View view) {
        destroyResources(view);
        clearContent();
    }

    private static void destroyResources(View view) {
        view.destroyHardwareResources();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setup(int width, int height, View.AttachInfo attachInfo, Rect surfaceInsets) {
        this.mWidth = width;
        this.mHeight = height;
        if (surfaceInsets != null && (surfaceInsets.left != 0 || surfaceInsets.right != 0 || surfaceInsets.top != 0 || surfaceInsets.bottom != 0)) {
            this.mInsetLeft = surfaceInsets.left;
            this.mInsetTop = surfaceInsets.top;
            this.mSurfaceWidth = this.mInsetLeft + width + surfaceInsets.right;
            this.mSurfaceHeight = this.mInsetTop + height + surfaceInsets.bottom;
            setOpaque(false);
        } else {
            this.mInsetLeft = 0;
            this.mInsetTop = 0;
            this.mSurfaceWidth = width;
            this.mSurfaceHeight = height;
        }
        this.mRootNode.setLeftTopRightBottom(-this.mInsetLeft, -this.mInsetTop, this.mSurfaceWidth, this.mSurfaceHeight);
        setLightCenter(attachInfo);
    }

    public boolean rendererOwnsSurfaceControlOpacity() {
        return this.mWebViewOverlayProvider.mSurfaceControl != null;
    }

    public boolean setSurfaceControlOpaque(boolean opaque) {
        return this.mWebViewOverlayProvider.setSurfaceControlOpaque(opaque);
    }

    private void updateWebViewOverlayCallbacks() {
        boolean shouldEnable = this.mWebViewOverlayProvider.shouldEnableOverlaySupport();
        if (shouldEnable != this.mWebViewOverlaysEnabled) {
            this.mWebViewOverlaysEnabled = shouldEnable;
            if (shouldEnable) {
                setASurfaceTransactionCallback(this.mWebViewOverlayProvider);
                setPrepareSurfaceControlForWebviewCallback(this.mWebViewOverlayProvider);
                return;
            }
            setASurfaceTransactionCallback(null);
            setPrepareSurfaceControlForWebviewCallback(null);
        }
    }

    @Override // android.graphics.HardwareRenderer
    public void setSurfaceControl(SurfaceControl surfaceControl) {
        super.setSurfaceControl(surfaceControl);
        this.mWebViewOverlayProvider.setSurfaceControl(surfaceControl);
        updateWebViewOverlayCallbacks();
    }

    public void setBlastBufferQueue(BLASTBufferQueue blastBufferQueue) {
        this.mWebViewOverlayProvider.setBLASTBufferQueue(blastBufferQueue);
        updateWebViewOverlayCallbacks();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLightCenter(View.AttachInfo attachInfo) {
        Point displaySize = attachInfo.mPoint;
        attachInfo.mDisplay.getRealSize(displaySize);
        float lightX = (displaySize.x / 2.0f) - attachInfo.mWindowLeft;
        float lightY = this.mLightY - attachInfo.mWindowTop;
        setLightSourceGeometry(lightX, lightY, this.mLightZ, this.mLightRadius);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getWidth() {
        return this.mWidth;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getHeight() {
        return this.mHeight;
    }

    private static int dumpArgsToFlags(String[] args) {
        if (args == null || args.length == 0) {
            return 1;
        }
        int flags = 0;
        for (String str : args) {
            char c = 65535;
            switch (str.hashCode()) {
                case -252053678:
                    if (str.equals("framestats")) {
                        c = 0;
                        break;
                    }
                    break;
                case 1492:
                    if (str.equals("-a")) {
                        c = 2;
                        break;
                    }
                    break;
                case 108404047:
                    if (str.equals("reset")) {
                        c = 1;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    flags |= 1;
                    break;
                case 1:
                    flags |= 2;
                    break;
                case 2:
                    flags = 1;
                    break;
            }
        }
        return flags;
    }

    public static void handleDumpGfxInfo(FileDescriptor fd, String[] args) {
        dumpGlobalProfileInfo(fd, dumpArgsToFlags(args));
        WindowManagerGlobal.getInstance().dumpGfxInfo(fd, args);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpGfxInfo(PrintWriter pw, FileDescriptor fd, String[] args) {
        pw.flush();
        dumpProfileInfo(fd, dumpArgsToFlags(args));
    }

    Picture captureRenderingCommands() {
        return null;
    }

    @Override // android.graphics.HardwareRenderer
    public boolean loadSystemProperties() {
        boolean changed = super.loadSystemProperties();
        if (changed) {
            invalidateRoot();
        }
        return changed;
    }

    private void updateViewTreeDisplayList(View view) {
        view.mPrivateFlags |= 32;
        view.mRecreateDisplayList = (view.mPrivateFlags & Integer.MIN_VALUE) == Integer.MIN_VALUE;
        view.mPrivateFlags &= Integer.MAX_VALUE;
        view.updateDisplayListIfDirty();
        view.mRecreateDisplayList = false;
    }

    private void updateRootDisplayList(View view, DrawCallbacks callbacks) {
        Trace.traceBegin(8L, "Record View#draw()");
        updateViewTreeDisplayList(view);
        if (this.mNextRtFrameCallbacks != null) {
            ArrayList<HardwareRenderer.FrameDrawingCallback> frameCallbacks = this.mNextRtFrameCallbacks;
            this.mNextRtFrameCallbacks = null;
            setFrameCallback(new AnonymousClass1(frameCallbacks));
        }
        if (this.mRootNodeNeedsUpdate || !this.mRootNode.hasDisplayList()) {
            RecordingCanvas canvas = this.mRootNode.beginRecording(this.mSurfaceWidth, this.mSurfaceHeight);
            try {
                int saveCount = canvas.save();
                canvas.translate(this.mInsetLeft, this.mInsetTop);
                callbacks.onPreDraw(canvas);
                canvas.enableZ();
                canvas.drawRenderNode(view.updateDisplayListIfDirty());
                canvas.disableZ();
                callbacks.onPostDraw(canvas);
                canvas.restoreToCount(saveCount);
                this.mRootNodeNeedsUpdate = false;
            } finally {
                this.mRootNode.endRecording();
            }
        }
        Trace.traceEnd(8L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: android.view.ThreadedRenderer$1  reason: invalid class name */
    /* loaded from: classes3.dex */
    public class AnonymousClass1 implements HardwareRenderer.FrameDrawingCallback {
        final /* synthetic */ ArrayList val$frameCallbacks;

        AnonymousClass1(ArrayList arrayList) {
            this.val$frameCallbacks = arrayList;
        }

        @Override // android.graphics.HardwareRenderer.FrameDrawingCallback
        public void onFrameDraw(long frame) {
        }

        @Override // android.graphics.HardwareRenderer.FrameDrawingCallback
        public HardwareRenderer.FrameCommitCallback onFrameDraw(int syncResult, long frame) {
            final ArrayList<HardwareRenderer.FrameCommitCallback> frameCommitCallbacks = new ArrayList<>();
            for (int i = 0; i < this.val$frameCallbacks.size(); i++) {
                HardwareRenderer.FrameCommitCallback frameCommitCallback = ((HardwareRenderer.FrameDrawingCallback) this.val$frameCallbacks.get(i)).onFrameDraw(syncResult, frame);
                if (frameCommitCallback != null) {
                    frameCommitCallbacks.add(frameCommitCallback);
                }
            }
            if (frameCommitCallbacks.isEmpty()) {
                return null;
            }
            return new HardwareRenderer.FrameCommitCallback() { // from class: android.view.ThreadedRenderer$1$$ExternalSyntheticLambda0
                @Override // android.graphics.HardwareRenderer.FrameCommitCallback
                public final void onFrameCommit(boolean z) {
                    ThreadedRenderer.AnonymousClass1.lambda$onFrameDraw$0(frameCommitCallbacks, z);
                }
            };
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$onFrameDraw$0(ArrayList frameCommitCallbacks, boolean didProduceBuffer) {
            for (int i = 0; i < frameCommitCallbacks.size(); i++) {
                ((HardwareRenderer.FrameCommitCallback) frameCommitCallbacks.get(i)).onFrameCommit(didProduceBuffer);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void invalidateRoot() {
        this.mRootNodeNeedsUpdate = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void draw(View view, View.AttachInfo attachInfo, DrawCallbacks callbacks) {
        attachInfo.mViewRootImpl.mViewFrameInfo.markDrawStart();
        updateRootDisplayList(view, callbacks);
        if (attachInfo.mPendingAnimatingRenderNodes != null) {
            int count = attachInfo.mPendingAnimatingRenderNodes.size();
            for (int i = 0; i < count; i++) {
                registerAnimatingRenderNode(attachInfo.mPendingAnimatingRenderNodes.get(i));
            }
            attachInfo.mPendingAnimatingRenderNodes.clear();
            attachInfo.mPendingAnimatingRenderNodes = null;
        }
        FrameInfo frameInfo = attachInfo.mViewRootImpl.getUpdatedFrameInfo();
        int syncResult = syncAndDrawFrame(frameInfo);
        if ((syncResult & 2) != 0) {
            Log.w("OpenGLRenderer", "Surface lost, forcing relayout");
            attachInfo.mViewRootImpl.mForceNextWindowRelayout = true;
            attachInfo.mViewRootImpl.requestLayout();
        }
        if ((syncResult & 1) != 0) {
            attachInfo.mViewRootImpl.invalidate();
        }
    }

    public RenderNode getRootNode() {
        return this.mRootNode;
    }

    /* loaded from: classes3.dex */
    public static class SimpleRenderer extends HardwareRenderer {
        private final float mLightRadius;
        private final float mLightY;
        private final float mLightZ;

        public SimpleRenderer(Context context, String name, Surface surface) {
            setName(name);
            setOpaque(false);
            setSurface(surface);
            TypedArray a = context.obtainStyledAttributes(null, R.styleable.Lighting, 0, 0);
            this.mLightY = a.getDimension(3, 0.0f);
            this.mLightZ = a.getDimension(4, 0.0f);
            this.mLightRadius = a.getDimension(2, 0.0f);
            float ambientShadowAlpha = a.getFloat(0, 0.0f);
            float spotShadowAlpha = a.getFloat(1, 0.0f);
            a.recycle();
            setLightSourceAlpha(ambientShadowAlpha, spotShadowAlpha);
        }

        public void setLightCenter(Display display, int windowLeft, int windowTop) {
            Point displaySize = new Point();
            display.getRealSize(displaySize);
            float lightX = (displaySize.x / 2.0f) - windowLeft;
            float lightY = this.mLightY - windowTop;
            setLightSourceGeometry(lightX, lightY, this.mLightZ, this.mLightRadius);
        }

        public RenderNode getRootNode() {
            return this.mRootNode;
        }

        public void draw(HardwareRenderer.FrameDrawingCallback callback) {
            long vsync = AnimationUtils.currentAnimationTimeMillis() * TimeUtils.NANOS_PER_MS;
            if (callback != null) {
                setFrameCallback(callback);
            }
            createRenderRequest().setVsyncTime(vsync).syncAndDraw();
        }
    }
}
