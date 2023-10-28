package android.view;

import android.content.Context;
import android.content.res.CompatibilityInfo;
import android.graphics.BLASTBufferQueue;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.RenderNode;
import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceControl;
import android.view.SurfaceControlViewHost;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewRootImpl;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.IAccessibilityEmbeddedConnection;
import android.window.SurfaceSyncer;
import com.android.internal.view.SurfaceCallbackHelper;
import com.mediatek.boostfwk.BoostFwkFactory;
import com.mediatek.boostfwk.scenario.refreshrate.EventScenario;
import com.mediatek.view.SurfaceExt;
import com.mediatek.view.SurfaceFactory;
import com.transsion.hubcore.view.ITranSurface;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
/* loaded from: classes3.dex */
public class SurfaceView extends View implements ViewRootImpl.SurfaceChangedCallback {
    private static final boolean DEBUG = SystemProperties.getBoolean("debug.surfaceview.log", false);
    private static final boolean DEBUG_POSITION = SystemProperties.getBoolean("debug.surfaceview.log", false);
    private static final long GET_STATS_FPS_INTERVAL = 2000;
    private static final String TAG = "SurfaceView";
    private boolean isUseResolutiontuner;
    private boolean mAttachedToWindow;
    int mBackgroundColor;
    SurfaceControl mBackgroundControl;
    private BLASTBufferQueue mBlastBufferQueue;
    private SurfaceControl mBlastSurfaceControl;
    final ArrayList<SurfaceHolder.Callback> mCallbacks;
    boolean mClipSurfaceToBounds;
    float mCornerRadius;
    private boolean mDisableBackgroundLayer;
    boolean mDrawFinished;
    private final ViewTreeObserver.OnPreDrawListener mDrawListener;
    boolean mDrawingStopped;
    private EventScenario mEventScenario;
    int mFormat;
    private FpsRunnable mFpsRunnable;
    private final SurfaceControl.Transaction mFrameCallbackTransaction;
    private Handler mGetFpsHandler;
    private int mGetStatsFpsCounter;
    private boolean mGlobalListenersAdded;
    boolean mHaveFrame;
    boolean mIsCreating;
    private boolean mIsVariableRefreshRateEnabled;
    long mLastLockTime;
    int mLastSurfaceHeight;
    int mLastSurfaceWidth;
    boolean mLastWindowVisibility;
    final int[] mLocation;
    private int mParentSurfaceSequenceId;
    private SurfaceViewPositionUpdateListener mPositionListener;
    private final Rect mRTLastReportedPosition;
    private final Point mRTLastReportedSurfaceSize;
    private RemoteAccessibilityController mRemoteAccessibilityController;
    int mRequestedFormat;
    int mRequestedHeight;
    boolean mRequestedVisible;
    int mRequestedWidth;
    Paint mRoundedViewportPaint;
    private final SurfaceControl.Transaction mRtTransaction;
    final Rect mScreenRect;
    private final ViewTreeObserver.OnScrollChangedListener mScrollChangedListener;
    int mSubLayer;
    final Surface mSurface;
    float mSurfaceAlpha;
    SurfaceControl mSurfaceControl;
    final Object mSurfaceControlLock;
    boolean mSurfaceCreated;
    private SurfaceExt mSurfaceExt;
    private int mSurfaceFlags;
    final Rect mSurfaceFrame;
    int mSurfaceHeight;
    private final SurfaceHolder mSurfaceHolder;
    final ReentrantLock mSurfaceLock;
    SurfaceControlViewHost.SurfacePackage mSurfacePackage;
    private final SurfaceSession mSurfaceSession;
    private final SurfaceSyncer mSurfaceSyncer;
    int mSurfaceWidth;
    private final ArraySet<Integer> mSyncIds;
    private final Matrix mTmpMatrix;
    final Rect mTmpRect;
    TranFoldInnerCustody mTranFoldInnerCustody;
    int mTransformHint;
    boolean mUseAlpha;
    boolean mViewVisibility;
    boolean mVisible;
    int mWindowSpaceLeft;
    int mWindowSpaceTop;
    boolean mWindowStopped;
    boolean mWindowVisibility;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$android-view-SurfaceView  reason: not valid java name */
    public /* synthetic */ boolean m4985lambda$new$0$androidviewSurfaceView() {
        this.mHaveFrame = getWidth() > 0 && getHeight() > 0;
        updateSurface();
        return true;
    }

    public SurfaceView(Context context) {
        this(context, null);
    }

    public SurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this(context, attrs, defStyleAttr, defStyleRes, false);
    }

    public SurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes, boolean disableBackgroundLayer) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mCallbacks = new ArrayList<>();
        this.mLocation = new int[2];
        this.mSurfaceLock = new ReentrantLock(true);
        this.mSurface = new Surface();
        this.mDrawingStopped = true;
        this.mDrawFinished = false;
        this.mScreenRect = new Rect();
        this.mSurfaceSession = new SurfaceSession();
        this.mDisableBackgroundLayer = false;
        this.mSurfaceControlLock = new Object();
        this.mTmpRect = new Rect();
        this.mSubLayer = -2;
        this.mIsCreating = false;
        this.mScrollChangedListener = new ViewTreeObserver.OnScrollChangedListener() { // from class: android.view.SurfaceView$$ExternalSyntheticLambda4
            @Override // android.view.ViewTreeObserver.OnScrollChangedListener
            public final void onScrollChanged() {
                SurfaceView.this.updateSurface();
            }
        };
        this.mSurfaceExt = SurfaceFactory.getInstance().getSurfaceExt();
        this.isUseResolutiontuner = false;
        this.mDrawListener = new ViewTreeObserver.OnPreDrawListener() { // from class: android.view.SurfaceView$$ExternalSyntheticLambda5
            @Override // android.view.ViewTreeObserver.OnPreDrawListener
            public final boolean onPreDraw() {
                return SurfaceView.this.m4985lambda$new$0$androidviewSurfaceView();
            }
        };
        this.mRequestedVisible = false;
        this.mWindowVisibility = false;
        this.mLastWindowVisibility = false;
        this.mViewVisibility = false;
        this.mWindowStopped = false;
        this.mRequestedWidth = -1;
        this.mRequestedHeight = -1;
        this.mRequestedFormat = 4;
        this.mUseAlpha = false;
        this.mSurfaceAlpha = 1.0f;
        this.mBackgroundColor = -16777216;
        this.mHaveFrame = false;
        this.mSurfaceCreated = false;
        this.mLastLockTime = 0L;
        this.mVisible = false;
        this.mWindowSpaceLeft = -1;
        this.mWindowSpaceTop = -1;
        this.mSurfaceWidth = -1;
        this.mSurfaceHeight = -1;
        this.mFormat = -1;
        this.mSurfaceFrame = new Rect();
        this.mLastSurfaceWidth = -1;
        this.mLastSurfaceHeight = -1;
        this.mTransformHint = 0;
        this.mSurfaceFlags = 4;
        this.mSurfaceSyncer = new SurfaceSyncer();
        this.mSyncIds = new ArraySet<>();
        this.mRtTransaction = new SurfaceControl.Transaction();
        this.mFrameCallbackTransaction = new SurfaceControl.Transaction();
        this.mRemoteAccessibilityController = new RemoteAccessibilityController(this);
        this.mTmpMatrix = new Matrix();
        this.mIsVariableRefreshRateEnabled = false;
        this.mGetStatsFpsCounter = 0;
        this.mGetFpsHandler = null;
        this.mFpsRunnable = null;
        this.mRTLastReportedPosition = new Rect();
        this.mRTLastReportedSurfaceSize = new Point();
        this.mPositionListener = null;
        this.mSurfaceHolder = new AnonymousClass1();
        this.mTranFoldInnerCustody = new TranFoldInnerCustody();
        setWillNotDraw(true);
        this.mDisableBackgroundLayer = disableBackgroundLayer;
        this.mSurfaceExt.initResolutionTunner();
        ITranSurface.Instance().constructSurface(context);
        OSSurfaceView.getInstance().onSurfaceViewInit(context.getApplicationContext());
    }

    public SurfaceHolder getHolder() {
        return this.mSurfaceHolder;
    }

    private void updateRequestedVisibility() {
        this.mRequestedVisible = this.mViewVisibility && this.mWindowVisibility && !this.mWindowStopped;
    }

    private void setWindowStopped(boolean stopped) {
        if (DEBUG) {
            Log.i(TAG, "setWindowStopped,stopped:" + this.mWindowStopped + ",stopped:" + stopped);
        }
        this.mWindowStopped = stopped;
        updateRequestedVisibility();
        updateSurface();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.view.View
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (DEBUG) {
            Log.i(TAG, "onAttachedToWindow");
        }
        getViewRootImpl().addSurfaceChangedCallback(this);
        this.mWindowStopped = false;
        this.mViewVisibility = getVisibility() == 0;
        updateRequestedVisibility();
        this.mAttachedToWindow = true;
        this.mParent.requestTransparentRegion(this);
        if (!this.mGlobalListenersAdded) {
            ViewTreeObserver observer = getViewTreeObserver();
            observer.addOnScrollChangedListener(this.mScrollChangedListener);
            observer.addOnPreDrawListener(this.mDrawListener);
            this.mGlobalListenersAdded = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.view.View
    public void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        this.mWindowVisibility = visibility == 0;
        updateRequestedVisibility();
        updateSurface();
    }

    @Override // android.view.View
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        boolean newRequestedVisible = true;
        boolean z = visibility == 0;
        this.mViewVisibility = z;
        if (!this.mWindowVisibility || !z || this.mWindowStopped) {
            newRequestedVisible = false;
        }
        if (newRequestedVisible != this.mRequestedVisible) {
            requestLayout();
        }
        this.mRequestedVisible = newRequestedVisible;
        updateSurface();
    }

    public void setUseAlpha() {
        if (!this.mUseAlpha) {
            this.mUseAlpha = true;
            updateSurfaceAlpha();
        }
    }

    @Override // android.view.View
    public void setAlpha(float alpha) {
        if (DEBUG) {
            Log.d(TAG, System.identityHashCode(this) + " setAlpha: mUseAlpha = " + this.mUseAlpha + " alpha=" + alpha);
        }
        super.setAlpha(alpha);
        updateSurfaceAlpha();
    }

    private float getFixedAlpha() {
        float alpha = getAlpha();
        if (!this.mUseAlpha || (this.mSubLayer <= 0 && alpha != 0.0f)) {
            return 1.0f;
        }
        return alpha;
    }

    private void updateSurfaceAlpha() {
        if (!this.mUseAlpha || !this.mHaveFrame || this.mSurfaceControl == null) {
            return;
        }
        float viewAlpha = getAlpha();
        if (this.mSubLayer < 0 && 0.0f < viewAlpha && viewAlpha < 1.0f) {
            Log.w(TAG, System.identityHashCode(this) + " updateSurfaceAlpha: translucent color is not supported for a surface placed z-below.");
        }
        ViewRootImpl viewRoot = getViewRootImpl();
        if (viewRoot == null) {
            return;
        }
        float alpha = getFixedAlpha();
        if (alpha != this.mSurfaceAlpha) {
            SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
            transaction.setAlpha(this.mSurfaceControl, alpha);
            viewRoot.applyTransactionOnDraw(transaction);
            damageInParent();
            this.mSurfaceAlpha = alpha;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void performDrawFinished() {
        this.mDrawFinished = true;
        if (this.mAttachedToWindow) {
            this.mParent.requestTransparentRegion(this);
            invalidate();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.view.View
    public void onDetachedFromWindow() {
        ViewRootImpl viewRoot = getViewRootImpl();
        if (viewRoot != null) {
            viewRoot.removeSurfaceChangedCallback(this);
        }
        if (DEBUG) {
            Log.i(TAG, "onDetachedFromWindow");
        }
        ITranSurface.Instance().onDetachedFromWindow(new TranSurfaceView(this));
        this.mAttachedToWindow = false;
        if (this.mGlobalListenersAdded) {
            ViewTreeObserver observer = getViewTreeObserver();
            observer.removeOnScrollChangedListener(this.mScrollChangedListener);
            observer.removeOnPreDrawListener(this.mDrawListener);
            this.mGlobalListenersAdded = false;
        }
        this.mRequestedVisible = false;
        updateSurface();
        releaseSurfaces(true);
        this.mHaveFrame = false;
        super.onDetachedFromWindow();
        this.mTranFoldInnerCustody.onDetachedFromWindow();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.view.View
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width;
        int height;
        int i = this.mRequestedWidth;
        if (i >= 0) {
            width = resolveSizeAndState(i, widthMeasureSpec, 0);
        } else {
            width = getDefaultSize(0, widthMeasureSpec);
        }
        int i2 = this.mRequestedHeight;
        if (i2 >= 0) {
            height = resolveSizeAndState(i2, heightMeasureSpec, 0);
        } else {
            height = getDefaultSize(0, heightMeasureSpec);
        }
        setMeasuredDimension(width, height);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.view.View
    public boolean setFrame(int left, int top, int right, int bottom) {
        boolean result = super.setFrame(left, top, right, bottom);
        updateSurface();
        return result;
    }

    @Override // android.view.View
    public boolean gatherTransparentRegion(Region region) {
        if (isAboveParent() || !this.mDrawFinished) {
            return super.gatherTransparentRegion(region);
        }
        boolean opaque = true;
        if ((this.mPrivateFlags & 128) == 0) {
            opaque = super.gatherTransparentRegion(region);
        } else if (region != null) {
            int w = getWidth();
            int h = getHeight();
            if (w > 0 && h > 0) {
                getLocationInWindow(this.mLocation);
                int[] iArr = this.mLocation;
                int l = iArr[0];
                int t = iArr[1];
                region.op(l, t, l + w, t + h, Region.Op.UNION);
            }
        }
        if (PixelFormat.formatHasAlpha(this.mRequestedFormat)) {
            return false;
        }
        return opaque;
    }

    @Override // android.view.View
    public void draw(Canvas canvas) {
        if (this.mDrawFinished && !isAboveParent() && (this.mPrivateFlags & 128) == 0) {
            clearSurfaceViewPort(canvas);
        }
        super.draw(canvas);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.view.View
    public void dispatchDraw(Canvas canvas) {
        if (this.mDrawFinished && !isAboveParent() && (this.mPrivateFlags & 128) == 128) {
            clearSurfaceViewPort(canvas);
        }
        super.dispatchDraw(canvas);
    }

    public void setEnableSurfaceClipping(boolean enabled) {
        this.mClipSurfaceToBounds = enabled;
        invalidate();
    }

    @Override // android.view.View
    public void setClipBounds(Rect clipBounds) {
        super.setClipBounds(clipBounds);
        if (!this.mClipSurfaceToBounds || this.mSurfaceControl == null) {
            return;
        }
        if (this.mCornerRadius > 0.0f && !isAboveParent()) {
            invalidate();
        }
        if (this.mClipBounds != null) {
            this.mTmpRect.set(this.mClipBounds);
        } else {
            this.mTmpRect.set(0, 0, this.mSurfaceWidth, this.mSurfaceHeight);
        }
        SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
        transaction.setWindowCrop(this.mSurfaceControl, this.mTmpRect);
        applyTransactionOnVriDraw(transaction);
        invalidate();
    }

    private void clearSurfaceViewPort(Canvas canvas) {
        if (this.mCornerRadius > 0.0f) {
            canvas.getClipBounds(this.mTmpRect);
            if (this.mClipSurfaceToBounds && this.mClipBounds != null) {
                this.mTmpRect.intersect(this.mClipBounds);
            }
            float f = this.mCornerRadius;
            canvas.punchHole(this.mTmpRect.left, this.mTmpRect.top, this.mTmpRect.right, this.mTmpRect.bottom, f, f);
            return;
        }
        canvas.punchHole(0.0f, 0.0f, getWidth(), getHeight(), 0.0f, 0.0f);
    }

    public void setCornerRadius(float cornerRadius) {
        this.mCornerRadius = cornerRadius;
        if (cornerRadius > 0.0f && this.mRoundedViewportPaint == null) {
            Paint paint = new Paint(1);
            this.mRoundedViewportPaint = paint;
            paint.setBlendMode(BlendMode.CLEAR);
            this.mRoundedViewportPaint.setColor(0);
        }
        invalidate();
    }

    public float getCornerRadius() {
        return this.mCornerRadius;
    }

    public void setZOrderMediaOverlay(boolean isMediaOverlay) {
        this.mSubLayer = isMediaOverlay ? -1 : -2;
    }

    public void setZOrderOnTop(boolean onTop) {
        boolean allowDynamicChange = getContext().getApplicationInfo().targetSdkVersion > 29;
        setZOrderedOnTop(onTop, allowDynamicChange);
    }

    public boolean isZOrderedOnTop() {
        return this.mSubLayer > 0;
    }

    public boolean setZOrderedOnTop(boolean onTop, boolean allowDynamicChange) {
        int subLayer;
        ViewRootImpl viewRoot;
        if (onTop) {
            subLayer = 1;
        } else {
            subLayer = -2;
        }
        if (this.mSubLayer == subLayer) {
            return false;
        }
        this.mSubLayer = subLayer;
        if (allowDynamicChange) {
            if (this.mSurfaceControl == null || (viewRoot = getViewRootImpl()) == null) {
                return true;
            }
            SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
            updateRelativeZ(transaction);
            viewRoot.applyTransactionOnDraw(transaction);
            invalidate();
            return true;
        }
        return false;
    }

    public void setDualDisplay() {
        this.mSurfaceFlags |= Integer.MIN_VALUE;
    }

    public void setSecure(boolean isSecure) {
        if (isSecure) {
            this.mSurfaceFlags |= 128;
        } else {
            this.mSurfaceFlags &= -129;
        }
    }

    private void updateOpaqueFlag() {
        if (!PixelFormat.formatHasAlpha(this.mRequestedFormat)) {
            this.mSurfaceFlags |= 1024;
        } else {
            this.mSurfaceFlags &= -1025;
        }
    }

    private void updateBackgroundVisibility(SurfaceControl.Transaction t) {
        SurfaceControl surfaceControl = this.mBackgroundControl;
        if (surfaceControl == null) {
            return;
        }
        if (this.mSubLayer < 0 && (this.mSurfaceFlags & 1024) != 0 && !this.mDisableBackgroundLayer) {
            t.show(surfaceControl);
        } else {
            t.hide(surfaceControl);
        }
    }

    private SurfaceControl.Transaction updateBackgroundColor(SurfaceControl.Transaction t) {
        float[] colorComponents = {Color.red(this.mBackgroundColor) / 255.0f, Color.green(this.mBackgroundColor) / 255.0f, Color.blue(this.mBackgroundColor) / 255.0f};
        t.setColor(this.mBackgroundControl, colorComponents);
        return t;
    }

    private void releaseSurfaces(boolean releaseSurfacePackage) {
        SurfaceControlViewHost.SurfacePackage surfacePackage;
        this.mSurfaceAlpha = 1.0f;
        this.mSurface.destroy();
        synchronized (this.mSurfaceControlLock) {
            BLASTBufferQueue bLASTBufferQueue = this.mBlastBufferQueue;
            if (bLASTBufferQueue != null) {
                bLASTBufferQueue.destroy();
                this.mBlastBufferQueue = null;
            }
            SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
            SurfaceControl surfaceControl = this.mSurfaceControl;
            if (surfaceControl != null) {
                transaction.remove(surfaceControl);
                this.mSurfaceControl = null;
            }
            SurfaceControl surfaceControl2 = this.mBackgroundControl;
            if (surfaceControl2 != null) {
                transaction.remove(surfaceControl2);
                this.mBackgroundControl = null;
            }
            SurfaceControl surfaceControl3 = this.mBlastSurfaceControl;
            if (surfaceControl3 != null) {
                transaction.remove(surfaceControl3);
                this.mBlastSurfaceControl = null;
            }
            if (releaseSurfacePackage && (surfacePackage = this.mSurfacePackage) != null) {
                surfacePackage.release();
                this.mSurfacePackage = null;
            }
            applyTransactionOnVriDraw(transaction);
        }
    }

    private void replacePositionUpdateListener(int surfaceWidth, int surfaceHeight) {
        if (this.mPositionListener != null) {
            this.mRenderNode.removePositionUpdateListener(this.mPositionListener);
        }
        this.mPositionListener = new SurfaceViewPositionUpdateListener(surfaceWidth, surfaceHeight);
        this.mRenderNode.addPositionUpdateListener(this.mPositionListener);
    }

    private boolean performSurfaceTransaction(ViewRootImpl viewRoot, CompatibilityInfo.Translator translator, boolean creating, boolean sizeChanged, boolean hintChanged, SurfaceControl.Transaction surfaceUpdateTransaction) {
        boolean z = true;
        this.mDrawingStopped = !this.mVisible;
        if (DEBUG) {
            Log.i(TAG, System.identityHashCode(this) + " Cur surface: " + this.mSurface);
        }
        if (creating) {
            updateRelativeZ(surfaceUpdateTransaction);
            SurfaceControlViewHost.SurfacePackage surfacePackage = this.mSurfacePackage;
            if (surfacePackage != null) {
                reparentSurfacePackage(surfaceUpdateTransaction, surfacePackage);
            }
        }
        this.mParentSurfaceSequenceId = viewRoot.getSurfaceSequenceId();
        if (this.mViewVisibility) {
            surfaceUpdateTransaction.show(this.mSurfaceControl);
        } else {
            surfaceUpdateTransaction.hide(this.mSurfaceControl);
        }
        updateBackgroundVisibility(surfaceUpdateTransaction);
        updateBackgroundColor(surfaceUpdateTransaction);
        if (this.mUseAlpha) {
            float alpha = getFixedAlpha();
            surfaceUpdateTransaction.setAlpha(this.mSurfaceControl, alpha);
            this.mSurfaceAlpha = alpha;
        }
        surfaceUpdateTransaction.setCornerRadius(this.mSurfaceControl, this.mCornerRadius);
        if ((sizeChanged || hintChanged) && !creating) {
            setBufferSize(surfaceUpdateTransaction);
        }
        if (sizeChanged || creating || !isHardwareAccelerated()) {
            if (!this.mClipSurfaceToBounds || this.mClipBounds == null) {
                surfaceUpdateTransaction.setWindowCrop(this.mSurfaceControl, this.mSurfaceWidth, this.mSurfaceHeight);
            } else {
                surfaceUpdateTransaction.setWindowCrop(this.mSurfaceControl, this.mClipBounds);
            }
            surfaceUpdateTransaction.setDesintationFrame(this.mBlastSurfaceControl, this.mSurfaceWidth, this.mSurfaceHeight);
            if (isHardwareAccelerated()) {
                replacePositionUpdateListener(this.mSurfaceWidth, this.mSurfaceHeight);
            } else {
                onSetSurfacePositionAndScale(surfaceUpdateTransaction, this.mSurfaceControl, this.mScreenRect.left, this.mScreenRect.top, this.mScreenRect.width() / this.mSurfaceWidth, this.mScreenRect.height() / this.mSurfaceHeight);
            }
            if (DEBUG_POSITION) {
                Object[] objArr = new Object[8];
                objArr[0] = Integer.valueOf(System.identityHashCode(this));
                objArr[1] = isHardwareAccelerated() ? "RenderWorker" : "UI Thread";
                objArr[2] = Integer.valueOf(this.mScreenRect.left);
                objArr[3] = Integer.valueOf(this.mScreenRect.top);
                objArr[4] = Integer.valueOf(this.mScreenRect.right);
                objArr[5] = Integer.valueOf(this.mScreenRect.bottom);
                objArr[6] = Integer.valueOf(this.mSurfaceWidth);
                objArr[7] = Integer.valueOf(this.mSurfaceHeight);
                Log.d(TAG, String.format("%d performSurfaceTransaction %s position = [%d, %d, %d, %d] surfaceSize = %dx%d", objArr));
            }
        }
        applyTransactionOnVriDraw(surfaceUpdateTransaction);
        updateEmbeddedAccessibilityMatrix(false);
        this.mSurfaceFrame.left = 0;
        this.mSurfaceFrame.top = 0;
        if (translator == null) {
            this.mSurfaceFrame.right = this.mSurfaceWidth;
            this.mSurfaceFrame.bottom = this.mSurfaceHeight;
        } else {
            float appInvertedScale = translator.applicationInvertedScale;
            this.mSurfaceFrame.right = (int) ((this.mSurfaceWidth * appInvertedScale) + 0.5f);
            this.mSurfaceFrame.bottom = (int) ((this.mSurfaceHeight * appInvertedScale) + 0.5f);
        }
        int surfaceWidth = this.mSurfaceFrame.right;
        int surfaceHeight = this.mSurfaceFrame.bottom;
        if (this.mLastSurfaceWidth == surfaceWidth && this.mLastSurfaceHeight == surfaceHeight) {
            z = false;
        }
        boolean realSizeChanged = z;
        this.mLastSurfaceWidth = surfaceWidth;
        this.mLastSurfaceHeight = surfaceHeight;
        return realSizeChanged;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1138=8, 1143=4] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:244:0x0507 */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:276:0x0579 */
    /* JADX DEBUG: Multi-variable search result rejected for r33v0, resolved type: android.view.SurfaceView */
    /* JADX DEBUG: Multi-variable search result rejected for r7v32, resolved type: java.lang.String */
    /* JADX DEBUG: Multi-variable search result rejected for r7v4, resolved type: java.lang.String */
    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX WARN: Can't wrap try/catch for region: R(14:95|(5:97|(1:99)(1:313)|100|(1:102)(1:312)|103)(1:314)|(10:104|105|106|107|108|(5:295|296|297|298|299)(1:110)|111|112|113|(4:114|115|116|(2:288|289)(2:118|(1:120))))|(15:(24:129|130|(2:134|(20:136|137|(9:264|265|266|267|268|269|270|271|272)(1:139)|140|141|142|143|144|145|(2:(3:158|159|(0))|148)|162|163|164|(1:253)(4:168|(9:172|173|174|(1:176)|177|(1:179)|180|181|(1:183))|(9:215|(8:238|239|240|241|242|243|244|245)(1:217)|(2:219|220)|224|225|226|(2:228|229)|232|233)(1:193)|(4:195|(1:197)|(1:199)|(1:201)(1:202)))|203|204|(1:208)|209|210|(2:212|213)(1:214)))|285|137|(0)(0)|140|141|142|143|144|145|(0)|162|163|164|(1:166)|253|203|204|(2:206|208)|209|210|(0)(0))|144|145|(0)|162|163|164|(0)|253|203|204|(0)|209|210|(0)(0))|286|130|(3:132|134|(0))|285|137|(0)(0)|140|141|142|143) */
    /* JADX WARN: Can't wrap try/catch for region: R(15:(24:129|130|(2:134|(20:136|137|(9:264|265|266|267|268|269|270|271|272)(1:139)|140|141|142|143|144|145|(2:(3:158|159|(0))|148)|162|163|164|(1:253)(4:168|(9:172|173|174|(1:176)|177|(1:179)|180|181|(1:183))|(9:215|(8:238|239|240|241|242|243|244|245)(1:217)|(2:219|220)|224|225|226|(2:228|229)|232|233)(1:193)|(4:195|(1:197)|(1:199)|(1:201)(1:202)))|203|204|(1:208)|209|210|(2:212|213)(1:214)))|285|137|(0)(0)|140|141|142|143|144|145|(0)|162|163|164|(1:166)|253|203|204|(2:206|208)|209|210|(0)(0))|144|145|(0)|162|163|164|(0)|253|203|204|(0)|209|210|(0)(0)) */
    /* JADX WARN: Code restructure failed: missing block: B:185:0x03f4, code lost:
        if (r4 != false) goto L148;
     */
    /* JADX WARN: Code restructure failed: missing block: B:276:0x0579, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:278:0x057c, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:279:0x057d, code lost:
        r7 = " h=";
        r6 = " w=";
     */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:160:0x0369  */
    /* JADX WARN: Removed duplicated region for block: B:176:0x03c2  */
    /* JADX WARN: Removed duplicated region for block: B:182:0x03ee  */
    /* JADX WARN: Removed duplicated region for block: B:193:0x0415 A[Catch: all -> 0x0555, TryCatch #11 {all -> 0x0555, blocks: (B:191:0x040e, B:193:0x0415, B:195:0x041d, B:220:0x048a), top: B:317:0x040e }] */
    /* JADX WARN: Removed duplicated region for block: B:260:0x054b A[Catch: Exception -> 0x0579, TryCatch #14 {Exception -> 0x0579, blocks: (B:269:0x0568, B:271:0x056f, B:273:0x0573, B:275:0x0578, B:258:0x0545, B:260:0x054b, B:262:0x054f), top: B:323:0x03ea }] */
    /* JADX WARN: Removed duplicated region for block: B:271:0x056f A[Catch: Exception -> 0x0579, TryCatch #14 {Exception -> 0x0579, blocks: (B:269:0x0568, B:271:0x056f, B:273:0x0573, B:275:0x0578, B:258:0x0545, B:260:0x054b, B:262:0x054f), top: B:323:0x03ea }] */
    /* JADX WARN: Removed duplicated region for block: B:291:0x05d7  */
    /* JADX WARN: Removed duplicated region for block: B:311:0x0371 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:339:? A[RETURN, SYNTHETIC] */
    /* JADX WARN: Type inference failed for: r6v10 */
    /* JADX WARN: Type inference failed for: r6v11 */
    /* JADX WARN: Type inference failed for: r6v12 */
    /* JADX WARN: Type inference failed for: r6v13 */
    /* JADX WARN: Type inference failed for: r6v14, types: [boolean] */
    /* JADX WARN: Type inference failed for: r6v15 */
    /* JADX WARN: Type inference failed for: r6v2 */
    /* JADX WARN: Type inference failed for: r6v3 */
    /* JADX WARN: Type inference failed for: r6v5 */
    /* JADX WARN: Type inference failed for: r6v6 */
    /* JADX WARN: Type inference failed for: r6v7 */
    /* JADX WARN: Type inference failed for: r6v8 */
    /* JADX WARN: Type inference failed for: r6v9 */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void updateSurface() {
        int myWidth;
        int myHeight;
        boolean formatChanged;
        SurfaceControl.Transaction transaction;
        ?? r6;
        String str;
        String str2;
        CompatibilityInfo.Translator translator;
        SurfaceControl.Transaction surfaceUpdateTransaction;
        boolean z;
        boolean redrawNeeded;
        boolean z2;
        boolean shouldSyncBuffer;
        boolean sizeChanged;
        boolean creating;
        SyncBufferTransactionCallback syncBufferTransactionCallback;
        String str3;
        String str4;
        String str5;
        String str6;
        if (!this.mHaveFrame) {
            if (DEBUG) {
                Log.d(TAG, System.identityHashCode(this) + " updateSurface: has no frame");
                return;
            }
            return;
        }
        ViewRootImpl viewRoot = getViewRootImpl();
        if (viewRoot == null) {
            return;
        }
        if (viewRoot.mSurface != null && viewRoot.mSurface.isValid()) {
            CompatibilityInfo.Translator translator2 = viewRoot.mTranslator;
            if (translator2 != null) {
                this.mSurface.setCompatibilityTranslator(translator2);
            }
            int myWidth2 = this.mRequestedWidth;
            if (myWidth2 <= 0) {
                myWidth2 = getWidth();
            }
            int myHeight2 = this.mRequestedHeight;
            if (myHeight2 <= 0) {
                myHeight2 = getHeight();
            }
            if (myWidth2 <= 200 || myHeight2 <= 200 || !this.mSurfaceExt.isResolutionTuningPackage() || !viewRoot.isRtEnable()) {
                myWidth = myWidth2;
                myHeight = myHeight2;
            } else {
                Log.i(TAG, "updateSurface,myWidth=" + myWidth2 + ",myHeight=" + myHeight2 + ",xScale=" + this.mSurfaceExt.getXScale() + ",yScale=" + this.mSurfaceExt.getYScale());
                int myWidth3 = (int) (myWidth2 / this.mSurfaceExt.getXScale());
                this.isUseResolutiontuner = true;
                myWidth = myWidth3;
                myHeight = (int) (myHeight2 / this.mSurfaceExt.getYScale());
            }
            if (this.mEventScenario == null) {
                this.mEventScenario = new EventScenario();
                this.mIsVariableRefreshRateEnabled = true;
            }
            if (this.mIsVariableRefreshRateEnabled && !this.mEventScenario.getIsMarked()) {
                BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mEventScenario.setScenarioAction(1));
                this.mIsVariableRefreshRateEnabled = this.mEventScenario.getVariableRefreshRateEnabled();
            }
            float alpha = getFixedAlpha();
            boolean formatChanged2 = this.mFormat != this.mRequestedFormat;
            boolean z3 = this.mVisible;
            boolean z4 = this.mRequestedVisible;
            boolean visibleChanged = z3 != z4;
            boolean alphaChanged = this.mSurfaceAlpha != alpha;
            boolean creating2 = (this.mSurfaceControl == null || formatChanged2 || visibleChanged) && z4;
            boolean sizeChanged2 = (this.mSurfaceWidth == myWidth && this.mSurfaceHeight == myHeight) ? false : true;
            boolean windowVisibleChanged = this.mWindowVisibility != this.mLastWindowVisibility;
            getLocationInSurface(this.mLocation);
            int i = this.mWindowSpaceLeft;
            int[] iArr = this.mLocation;
            boolean positionChanged = (i == iArr[0] && this.mWindowSpaceTop == iArr[1]) ? false : true;
            boolean layoutSizeChanged = (getWidth() == this.mScreenRect.width() && getHeight() == this.mScreenRect.height()) ? false : true;
            boolean hintChanged = viewRoot.getBufferTransformHint() != this.mTransformHint && this.mRequestedVisible;
            ITranSurface.Instance().updateSurface(new TranSurfaceView(this));
            if (!creating2 && !formatChanged2 && !sizeChanged2 && !visibleChanged && ((!this.mUseAlpha || !alphaChanged) && !windowVisibleChanged && !positionChanged && !layoutSizeChanged && !hintChanged)) {
                return;
            }
            getLocationInWindow(this.mLocation);
            boolean z5 = DEBUG;
            if (z5) {
                formatChanged = formatChanged2;
                Log.i(TAG, System.identityHashCode(this) + " Changes: creating=" + creating2 + " format=" + formatChanged2 + " size=" + sizeChanged2 + " visible=" + visibleChanged + " alpha=" + alphaChanged + " hint=" + hintChanged + " mUseAlpha=" + this.mUseAlpha + " visible=" + visibleChanged + " left=" + (this.mWindowSpaceLeft != this.mLocation[0]) + " top=" + (this.mWindowSpaceTop != this.mLocation[1]));
            } else {
                formatChanged = formatChanged2;
            }
            try {
                this.mVisible = this.mRequestedVisible;
                int[] iArr2 = this.mLocation;
                this.mWindowSpaceLeft = iArr2[0];
                this.mWindowSpaceTop = iArr2[1];
                this.mSurfaceWidth = myWidth;
                this.mSurfaceHeight = myHeight;
                this.mFormat = this.mRequestedFormat;
                try {
                    OSSurfaceView.getInstance().onSurfaceSizeChanged(this.mSurfaceWidth, this.mSurfaceHeight, this.mFormat);
                    this.mLastWindowVisibility = this.mWindowVisibility;
                    this.mTransformHint = viewRoot.getBufferTransformHint();
                    this.mScreenRect.left = this.mWindowSpaceLeft;
                    this.mScreenRect.top = this.mWindowSpaceTop;
                    this.mScreenRect.right = this.mWindowSpaceLeft + getWidth();
                    this.mScreenRect.bottom = this.mWindowSpaceTop + getHeight();
                    if (translator2 != null) {
                        try {
                            translator = translator2;
                        } catch (Exception e) {
                            ex = e;
                            transaction = " h=";
                            r6 = " w=";
                        }
                        try {
                            translator.translateRectInAppWindowToScreen(this.mScreenRect);
                        } catch (Exception e2) {
                            ex = e2;
                            transaction = " h=";
                            r6 = " w=";
                            Log.e(TAG, "Exception configuring surface", ex);
                            str2 = r6;
                            str = transaction;
                            if (DEBUG) {
                            }
                        }
                    } else {
                        translator = translator2;
                    }
                    try {
                        Rect surfaceInsets = viewRoot.mWindowAttributes.surfaceInsets;
                        try {
                            this.mScreenRect.offset(surfaceInsets.left, surfaceInsets.top);
                            surfaceUpdateTransaction = new SurfaceControl.Transaction();
                            if (creating2) {
                                try {
                                    updateOpaqueFlag();
                                    String name = "SurfaceView[" + viewRoot.getTitle().toString() + NavigationBarInflaterView.SIZE_MOD_END;
                                    createBlastSurfaceControls(viewRoot, name, surfaceUpdateTransaction);
                                } catch (Exception e3) {
                                    ex = e3;
                                    transaction = " h=";
                                    r6 = " w=";
                                    Log.e(TAG, "Exception configuring surface", ex);
                                    str2 = r6;
                                    str = transaction;
                                    if (DEBUG) {
                                    }
                                }
                            } else if (this.mSurfaceControl == null) {
                                return;
                            }
                        } catch (Exception e4) {
                            ex = e4;
                            transaction = " h=";
                            r6 = " w=";
                        }
                    } catch (Exception e5) {
                        ex = e5;
                        transaction = " h=";
                        r6 = " w=";
                    }
                } catch (Exception e6) {
                    ex = e6;
                    transaction = " h=";
                    r6 = " w=";
                }
            } catch (Exception e7) {
                ex = e7;
                transaction = " h=";
                r6 = " w=";
            }
            try {
                try {
                    if (!sizeChanged2 && !creating2 && !hintChanged && (!this.mVisible || this.mDrawFinished)) {
                        z = false;
                        redrawNeeded = z;
                        if (redrawNeeded && viewRoot.wasRelayoutRequested()) {
                            if (viewRoot.isInLocalSync()) {
                                z2 = true;
                                shouldSyncBuffer = z2;
                                if (shouldSyncBuffer) {
                                    try {
                                        sizeChanged = sizeChanged2;
                                        try {
                                            final SyncBufferTransactionCallback syncBufferTransactionCallback2 = new SyncBufferTransactionCallback();
                                            BLASTBufferQueue bLASTBufferQueue = this.mBlastBufferQueue;
                                            Objects.requireNonNull(syncBufferTransactionCallback2);
                                            creating = creating2;
                                            try {
                                                bLASTBufferQueue.syncNextTransaction(false, new Consumer() { // from class: android.view.SurfaceView$$ExternalSyntheticLambda7
                                                    @Override // java.util.function.Consumer
                                                    public final void accept(Object obj) {
                                                        SurfaceView.SyncBufferTransactionCallback.this.onTransactionReady((SurfaceControl.Transaction) obj);
                                                    }
                                                });
                                                syncBufferTransactionCallback = syncBufferTransactionCallback2;
                                            } catch (Exception e8) {
                                                ex = e8;
                                                transaction = " h=";
                                                r6 = " w=";
                                                Log.e(TAG, "Exception configuring surface", ex);
                                                str2 = r6;
                                                str = transaction;
                                                if (DEBUG) {
                                                }
                                            }
                                        } catch (Exception e9) {
                                            ex = e9;
                                            transaction = " h=";
                                            r6 = " w=";
                                        }
                                    } catch (Exception e10) {
                                        ex = e10;
                                        transaction = " h=";
                                        r6 = " w=";
                                    }
                                } else {
                                    sizeChanged = sizeChanged2;
                                    creating = creating2;
                                    syncBufferTransactionCallback = null;
                                }
                                boolean sizeChanged3 = sizeChanged;
                                boolean creating3 = creating;
                                CompatibilityInfo.Translator translator3 = translator;
                                SyncBufferTransactionCallback syncBufferTransactionCallback3 = syncBufferTransactionCallback;
                                int myHeight3 = myHeight;
                                r6 = hintChanged;
                                int myWidth4 = myWidth;
                                transaction = surfaceUpdateTransaction;
                                boolean realSizeChanged = performSurfaceTransaction(viewRoot, translator3, creating3, sizeChanged3, r6, transaction);
                                SurfaceHolder.Callback[] callbacks = null;
                                boolean surfaceChanged = creating3;
                                if (this.mSurfaceCreated) {
                                    if (!surfaceChanged) {
                                        try {
                                            if (!this.mVisible) {
                                            }
                                        } catch (Throwable th) {
                                            th = th;
                                            this.mIsCreating = false;
                                            if (this.mSurfaceControl != null) {
                                            }
                                            throw th;
                                        }
                                    }
                                    this.mSurfaceCreated = false;
                                    notifySurfaceDestroyed();
                                }
                                copySurface(creating3, sizeChanged3);
                                if (this.mVisible || !this.mSurface.isValid()) {
                                    str3 = " h=";
                                    str4 = " w=";
                                } else {
                                    if (!this.mSurfaceCreated && (surfaceChanged || visibleChanged)) {
                                        try {
                                            this.mSurfaceCreated = true;
                                            this.mIsCreating = true;
                                            if (z5) {
                                                Log.i(TAG, System.identityHashCode(this) + " visibleChanged -- surfaceCreated");
                                            }
                                            callbacks = getSurfaceCallbacks();
                                            int i2 = 0;
                                            for (int length = callbacks.length; i2 < length; length = length) {
                                                SurfaceHolder.Callback c = callbacks[i2];
                                                c.surfaceCreated(this.mSurfaceHolder);
                                                i2++;
                                            }
                                            if (this.isUseResolutiontuner) {
                                                getViewRootImpl().setSurfaceViewCreated(true);
                                            }
                                        } catch (Throwable th2) {
                                            th = th2;
                                            this.mIsCreating = false;
                                            if (this.mSurfaceControl != null) {
                                            }
                                            throw th;
                                        }
                                    }
                                    if (creating3 || formatChanged || sizeChanged3 || hintChanged || visibleChanged || realSizeChanged) {
                                        if (DEBUG) {
                                            try {
                                                str5 = " w=";
                                                try {
                                                    str6 = " h=";
                                                } catch (Throwable th3) {
                                                    th = th3;
                                                    this.mIsCreating = false;
                                                    if (this.mSurfaceControl != null) {
                                                        releaseSurfaces(false);
                                                    }
                                                    throw th;
                                                }
                                            } catch (Throwable th4) {
                                                th = th4;
                                            }
                                            try {
                                                Log.i(TAG, System.identityHashCode(this) + " surfaceChanged -- format=" + this.mFormat + str5 + myWidth4 + str6 + myHeight3);
                                            } catch (Throwable th5) {
                                                th = th5;
                                                this.mIsCreating = false;
                                                if (this.mSurfaceControl != null) {
                                                }
                                                throw th;
                                            }
                                        } else {
                                            str6 = " h=";
                                            str5 = " w=";
                                        }
                                        if (callbacks == null) {
                                            callbacks = getSurfaceCallbacks();
                                        }
                                        try {
                                            int length2 = callbacks.length;
                                            int i3 = 0;
                                            while (i3 < length2) {
                                                try {
                                                    SurfaceHolder.Callback c2 = callbacks[i3];
                                                    c2.surfaceChanged(this.mSurfaceHolder, this.mFormat, myWidth4, myHeight3);
                                                    i3++;
                                                    length2 = length2;
                                                    callbacks = callbacks;
                                                    surfaceChanged = surfaceChanged;
                                                } catch (Throwable th6) {
                                                    th = th6;
                                                    this.mIsCreating = false;
                                                    if (this.mSurfaceControl != null && !this.mSurfaceCreated) {
                                                        releaseSurfaces(false);
                                                    }
                                                    throw th;
                                                }
                                            }
                                            str4 = str5;
                                            str3 = str6;
                                        } catch (Throwable th7) {
                                            th = th7;
                                            this.mIsCreating = false;
                                            if (this.mSurfaceControl != null) {
                                            }
                                            throw th;
                                        }
                                    } else {
                                        str3 = " h=";
                                        str4 = " w=";
                                    }
                                    if (redrawNeeded) {
                                        if (DEBUG) {
                                            Log.i(TAG, System.identityHashCode(this) + " surfaceRedrawNeeded");
                                        }
                                        if (callbacks == null) {
                                            callbacks = getSurfaceCallbacks();
                                        }
                                        if (shouldSyncBuffer) {
                                            handleSyncBufferCallback(callbacks, syncBufferTransactionCallback3);
                                        } else {
                                            handleSyncNoBuffer(callbacks);
                                        }
                                    }
                                }
                                this.mIsCreating = false;
                                if (this.mSurfaceControl != null && !this.mSurfaceCreated) {
                                    releaseSurfaces(false);
                                }
                                str2 = str4;
                                str = str3;
                                if (DEBUG) {
                                    Log.v(TAG, "Layout: x=" + this.mScreenRect.left + " y=" + this.mScreenRect.top + str2 + this.mScreenRect.width() + str + this.mScreenRect.height() + ", frame=" + this.mSurfaceFrame);
                                    return;
                                }
                                return;
                            }
                        }
                        z2 = false;
                        shouldSyncBuffer = z2;
                        if (shouldSyncBuffer) {
                        }
                        boolean sizeChanged32 = sizeChanged;
                        boolean creating32 = creating;
                        CompatibilityInfo.Translator translator32 = translator;
                        SyncBufferTransactionCallback syncBufferTransactionCallback32 = syncBufferTransactionCallback;
                        int myHeight32 = myHeight;
                        r6 = hintChanged;
                        int myWidth42 = myWidth;
                        transaction = surfaceUpdateTransaction;
                        boolean realSizeChanged2 = performSurfaceTransaction(viewRoot, translator32, creating32, sizeChanged32, r6, transaction);
                        SurfaceHolder.Callback[] callbacks2 = null;
                        boolean surfaceChanged2 = creating32;
                        if (this.mSurfaceCreated) {
                        }
                        copySurface(creating32, sizeChanged32);
                        if (this.mVisible) {
                        }
                        str3 = " h=";
                        str4 = " w=";
                        this.mIsCreating = false;
                        if (this.mSurfaceControl != null) {
                            releaseSurfaces(false);
                        }
                        str2 = str4;
                        str = str3;
                        if (DEBUG) {
                        }
                    }
                    copySurface(creating32, sizeChanged32);
                    if (this.mVisible) {
                    }
                    str3 = " h=";
                    str4 = " w=";
                    this.mIsCreating = false;
                    if (this.mSurfaceControl != null) {
                    }
                    str2 = str4;
                    str = str3;
                    if (DEBUG) {
                    }
                } catch (Throwable th8) {
                    th = th8;
                }
                if (this.mSurfaceCreated) {
                }
            } catch (Throwable th9) {
                th = th9;
            }
            z = true;
            redrawNeeded = z;
            if (redrawNeeded) {
                if (viewRoot.isInLocalSync()) {
                }
            }
            z2 = false;
            shouldSyncBuffer = z2;
            if (shouldSyncBuffer) {
            }
            boolean sizeChanged322 = sizeChanged;
            boolean creating322 = creating;
            CompatibilityInfo.Translator translator322 = translator;
            SyncBufferTransactionCallback syncBufferTransactionCallback322 = syncBufferTransactionCallback;
            int myHeight322 = myHeight;
            r6 = hintChanged;
            int myWidth422 = myWidth;
            transaction = surfaceUpdateTransaction;
            boolean realSizeChanged22 = performSurfaceTransaction(viewRoot, translator322, creating322, sizeChanged322, r6, transaction);
            SurfaceHolder.Callback[] callbacks22 = null;
            boolean surfaceChanged22 = creating322;
        }
        notifySurfaceDestroyed();
        releaseSurfaces(false);
    }

    private void handleSyncBufferCallback(final SurfaceHolder.Callback[] callbacks, final SyncBufferTransactionCallback syncBufferTransactionCallback) {
        getViewRootImpl().addToSync(new SurfaceSyncer.SyncTarget() { // from class: android.view.SurfaceView$$ExternalSyntheticLambda6
            @Override // android.window.SurfaceSyncer.SyncTarget
            public final void onReadyToSync(SurfaceSyncer.SyncBufferCallback syncBufferCallback) {
                SurfaceView.this.m4982lambda$handleSyncBufferCallback$2$androidviewSurfaceView(callbacks, syncBufferTransactionCallback, syncBufferCallback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleSyncBufferCallback$2$android-view-SurfaceView  reason: not valid java name */
    public /* synthetic */ void m4982lambda$handleSyncBufferCallback$2$androidviewSurfaceView(SurfaceHolder.Callback[] callbacks, final SyncBufferTransactionCallback syncBufferTransactionCallback, final SurfaceSyncer.SyncBufferCallback syncBufferCallback) {
        redrawNeededAsync(callbacks, new Runnable() { // from class: android.view.SurfaceView$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                SurfaceView.this.m4981lambda$handleSyncBufferCallback$1$androidviewSurfaceView(syncBufferTransactionCallback, syncBufferCallback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleSyncBufferCallback$1$android-view-SurfaceView  reason: not valid java name */
    public /* synthetic */ void m4981lambda$handleSyncBufferCallback$1$androidviewSurfaceView(SyncBufferTransactionCallback syncBufferTransactionCallback, SurfaceSyncer.SyncBufferCallback syncBufferCallback) {
        SurfaceControl.Transaction t = null;
        BLASTBufferQueue bLASTBufferQueue = this.mBlastBufferQueue;
        if (bLASTBufferQueue != null) {
            bLASTBufferQueue.stopContinuousSyncTransaction();
            t = syncBufferTransactionCallback.waitForTransaction();
        }
        syncBufferCallback.onBufferReady(t);
        onDrawFinished();
    }

    private void handleSyncNoBuffer(final SurfaceHolder.Callback[] callbacks) {
        final int syncId = this.mSurfaceSyncer.setupSync(new Runnable() { // from class: android.view.SurfaceView$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                SurfaceView.this.onDrawFinished();
            }
        });
        this.mSurfaceSyncer.addToSync(syncId, new SurfaceSyncer.SyncTarget() { // from class: android.view.SurfaceView$$ExternalSyntheticLambda2
            @Override // android.window.SurfaceSyncer.SyncTarget
            public final void onReadyToSync(SurfaceSyncer.SyncBufferCallback syncBufferCallback) {
                SurfaceView.this.m4984lambda$handleSyncNoBuffer$4$androidviewSurfaceView(callbacks, syncId, syncBufferCallback);
            }
        });
        this.mSurfaceSyncer.markSyncReady(syncId);
        synchronized (this.mSyncIds) {
            this.mSyncIds.add(Integer.valueOf(syncId));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleSyncNoBuffer$4$android-view-SurfaceView  reason: not valid java name */
    public /* synthetic */ void m4984lambda$handleSyncNoBuffer$4$androidviewSurfaceView(SurfaceHolder.Callback[] callbacks, final int syncId, final SurfaceSyncer.SyncBufferCallback syncBufferCallback) {
        redrawNeededAsync(callbacks, new Runnable() { // from class: android.view.SurfaceView$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                SurfaceView.this.m4983lambda$handleSyncNoBuffer$3$androidviewSurfaceView(syncBufferCallback, syncId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleSyncNoBuffer$3$android-view-SurfaceView  reason: not valid java name */
    public /* synthetic */ void m4983lambda$handleSyncNoBuffer$3$androidviewSurfaceView(SurfaceSyncer.SyncBufferCallback syncBufferCallback, int syncId) {
        syncBufferCallback.onBufferReady(null);
        synchronized (this.mSyncIds) {
            this.mSyncIds.remove(Integer.valueOf(syncId));
        }
    }

    private void redrawNeededAsync(SurfaceHolder.Callback[] callbacks, Runnable callbacksCollected) {
        SurfaceCallbackHelper sch = new SurfaceCallbackHelper(this.mTranFoldInnerCustody.redrawNeededAsync(callbacksCollected));
        sch.dispatchSurfaceRedrawNeededAsync(this.mSurfaceHolder, callbacks);
    }

    @Override // android.view.ViewRootImpl.SurfaceChangedCallback
    public void surfaceSyncStarted() {
        ViewRootImpl viewRoot = getViewRootImpl();
        if (viewRoot != null) {
            synchronized (this.mSyncIds) {
                Iterator<Integer> it = this.mSyncIds.iterator();
                while (it.hasNext()) {
                    int syncId = it.next().intValue();
                    viewRoot.mergeSync(syncId, this.mSurfaceSyncer);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public static class SyncBufferTransactionCallback {
        private final CountDownLatch mCountDownLatch;
        private SurfaceControl.Transaction mTransaction;

        private SyncBufferTransactionCallback() {
            this.mCountDownLatch = new CountDownLatch(1);
        }

        SurfaceControl.Transaction waitForTransaction() {
            try {
                this.mCountDownLatch.await();
            } catch (InterruptedException e) {
            }
            return this.mTransaction;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void onTransactionReady(SurfaceControl.Transaction t) {
            this.mTransaction = t;
            this.mCountDownLatch.countDown();
        }
    }

    private void copySurface(boolean surfaceControlCreated, boolean bufferSizeChanged) {
        BLASTBufferQueue bLASTBufferQueue;
        boolean needsWorkaround = bufferSizeChanged && getContext().getApplicationInfo().targetSdkVersion < 26;
        if (!surfaceControlCreated && !needsWorkaround) {
            return;
        }
        this.mSurfaceLock.lock();
        if (surfaceControlCreated) {
            try {
                this.mSurface.copyFrom(this.mBlastBufferQueue);
            } catch (Throwable th) {
                this.mSurfaceLock.unlock();
                throw th;
            }
        }
        if (needsWorkaround && (bLASTBufferQueue = this.mBlastBufferQueue) != null) {
            this.mSurface.transferFrom(bLASTBufferQueue.createSurfaceWithHandle());
        }
        this.mSurfaceLock.unlock();
    }

    private void setBufferSize(SurfaceControl.Transaction transaction) {
        this.mBlastSurfaceControl.setTransformHint(this.mTransformHint);
        BLASTBufferQueue bLASTBufferQueue = this.mBlastBufferQueue;
        if (bLASTBufferQueue != null) {
            bLASTBufferQueue.update(this.mBlastSurfaceControl, this.mSurfaceWidth, this.mSurfaceHeight, this.mFormat);
        }
    }

    private void createBlastSurfaceControls(ViewRootImpl viewRoot, String name, SurfaceControl.Transaction surfaceUpdateTransaction) {
        if (this.mSurfaceControl == null) {
            this.mSurfaceControl = new SurfaceControl.Builder(this.mSurfaceSession).setName(name).setLocalOwnerView(this).setParent(viewRoot.getBoundsLayer()).setCallsite("SurfaceView.updateSurface").setContainerLayer().build();
        }
        SurfaceControl surfaceControl = this.mBlastSurfaceControl;
        if (surfaceControl == null) {
            if (isDualDisplayComponent()) {
                this.mSurfaceFlags |= Integer.MIN_VALUE;
            }
            this.mBlastSurfaceControl = new SurfaceControl.Builder(this.mSurfaceSession).setName(name + "(BLAST)").setLocalOwnerView(this).setParent(this.mSurfaceControl).setFlags(this.mSurfaceFlags).setHidden(false).setBLASTLayer().setCallsite("SurfaceView.updateSurface").build();
        } else {
            surfaceUpdateTransaction.setOpaque(surfaceControl, (this.mSurfaceFlags & 1024) != 0).setSecure(this.mBlastSurfaceControl, (this.mSurfaceFlags & 128) != 0).show(this.mBlastSurfaceControl);
        }
        if (this.mBackgroundControl == null) {
            this.mBackgroundControl = new SurfaceControl.Builder(this.mSurfaceSession).setName("Background for " + name).setLocalOwnerView(this).setOpaque(true).setColorLayer().setParent(this.mSurfaceControl).setCallsite("SurfaceView.updateSurface").build();
        }
        BLASTBufferQueue bLASTBufferQueue = this.mBlastBufferQueue;
        if (bLASTBufferQueue != null) {
            bLASTBufferQueue.destroy();
        }
        int bufferTransformHint = viewRoot.getBufferTransformHint();
        this.mTransformHint = bufferTransformHint;
        this.mBlastSurfaceControl.setTransformHint(bufferTransformHint);
        BLASTBufferQueue bLASTBufferQueue2 = new BLASTBufferQueue(name, false);
        this.mBlastBufferQueue = bLASTBufferQueue2;
        bLASTBufferQueue2.update(this.mBlastSurfaceControl, this.mSurfaceWidth, this.mSurfaceHeight, this.mFormat);
        this.mBlastBufferQueue.setTransactionHangCallback(ViewRootImpl.sTransactionHangCallback);
        OSSurfaceView.getInstance().onBLASTBufferQueueCreated(this.mBlastSurfaceControl, this.mBlastBufferQueue);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onDrawFinished() {
        if (DEBUG) {
            Log.i(TAG, System.identityHashCode(this) + " finishedDrawing");
        }
        runOnUiThread(new Runnable() { // from class: android.view.SurfaceView$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                SurfaceView.this.performDrawFinished();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void onSetSurfacePositionAndScale(SurfaceControl.Transaction transaction, SurfaceControl surface, int positionLeft, int positionTop, float postScaleX, float postScaleY) {
        transaction.setPosition(surface, positionLeft, positionTop);
        transaction.setMatrix(surface, postScaleX, 0.0f, 0.0f, postScaleY);
    }

    public void requestUpdateSurfacePositionAndScale() {
        if (this.mSurfaceControl == null) {
            return;
        }
        SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
        onSetSurfacePositionAndScale(transaction, this.mSurfaceControl, this.mScreenRect.left, this.mScreenRect.top, this.mScreenRect.width() / this.mSurfaceWidth, this.mScreenRect.height() / this.mSurfaceHeight);
        applyTransactionOnVriDraw(transaction);
        invalidate();
    }

    public Rect getSurfaceRenderPosition() {
        return this.mRTLastReportedPosition;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void applyOrMergeTransaction(SurfaceControl.Transaction t, long frameNumber) {
        ViewRootImpl viewRoot = getViewRootImpl();
        if (viewRoot != null) {
            viewRoot.m5128lambda$applyTransactionOnDraw$11$androidviewViewRootImpl(t, frameNumber);
        } else {
            t.apply();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public class SurfaceViewPositionUpdateListener implements RenderNode.PositionUpdateListener {
        private final int mRtSurfaceHeight;
        private final int mRtSurfaceWidth;
        private boolean mRtFirst = true;
        private final SurfaceControl.Transaction mPositionChangedTransaction = new SurfaceControl.Transaction();

        SurfaceViewPositionUpdateListener(int surfaceWidth, int surfaceHeight) {
            this.mRtSurfaceWidth = surfaceWidth;
            this.mRtSurfaceHeight = surfaceHeight;
        }

        @Override // android.graphics.RenderNode.PositionUpdateListener
        public void positionChanged(long frameNumber, int left, int top, int right, int bottom) {
            if (this.mRtFirst || SurfaceView.this.mRTLastReportedPosition.left != left || SurfaceView.this.mRTLastReportedPosition.top != top || SurfaceView.this.mRTLastReportedPosition.right != right || SurfaceView.this.mRTLastReportedPosition.bottom != bottom || SurfaceView.this.mRTLastReportedSurfaceSize.x != this.mRtSurfaceWidth || SurfaceView.this.mRTLastReportedSurfaceSize.y != this.mRtSurfaceHeight) {
                this.mRtFirst = false;
                try {
                    if (SurfaceView.DEBUG_POSITION) {
                        Log.d(SurfaceView.TAG, String.format("%d updateSurfacePosition RenderWorker, frameNr = %d, position = [%d, %d, %d, %d] surfaceSize = %dx%d", Integer.valueOf(System.identityHashCode(SurfaceView.this)), Long.valueOf(frameNumber), Integer.valueOf(left), Integer.valueOf(top), Integer.valueOf(right), Integer.valueOf(bottom), Integer.valueOf(this.mRtSurfaceWidth), Integer.valueOf(this.mRtSurfaceHeight)));
                    }
                    synchronized (SurfaceView.this.mSurfaceControlLock) {
                        try {
                            if (SurfaceView.this.mSurfaceControl == null) {
                                return;
                            }
                            SurfaceView.this.mRTLastReportedPosition.set(left, top, right, bottom);
                            SurfaceView.this.mRTLastReportedSurfaceSize.set(this.mRtSurfaceWidth, this.mRtSurfaceHeight);
                            SurfaceView surfaceView = SurfaceView.this;
                            surfaceView.onSetSurfacePositionAndScale(this.mPositionChangedTransaction, surfaceView.mSurfaceControl, SurfaceView.this.mRTLastReportedPosition.left, SurfaceView.this.mRTLastReportedPosition.top, SurfaceView.this.mRTLastReportedPosition.width() / this.mRtSurfaceWidth, SurfaceView.this.mRTLastReportedPosition.height() / this.mRtSurfaceHeight);
                            if (SurfaceView.this.mViewVisibility) {
                                this.mPositionChangedTransaction.show(SurfaceView.this.mSurfaceControl);
                            }
                            SurfaceView.this.applyOrMergeTransaction(this.mPositionChangedTransaction, frameNumber);
                        } catch (Exception e) {
                            ex = e;
                            Log.e(SurfaceView.TAG, "Exception from repositionChild", ex);
                        }
                    }
                } catch (Exception e2) {
                    ex = e2;
                }
            }
        }

        @Override // android.graphics.RenderNode.PositionUpdateListener
        public void applyStretch(long frameNumber, float width, float height, float vecX, float vecY, float maxStretchX, float maxStretchY, float childRelativeLeft, float childRelativeTop, float childRelativeRight, float childRelativeBottom) {
            SurfaceView.this.mRtTransaction.setStretchEffect(SurfaceView.this.mSurfaceControl, width, height, vecX, vecY, maxStretchX, maxStretchY, childRelativeLeft, childRelativeTop, childRelativeRight, childRelativeBottom);
            SurfaceView surfaceView = SurfaceView.this;
            surfaceView.applyOrMergeTransaction(surfaceView.mRtTransaction, frameNumber);
        }

        @Override // android.graphics.RenderNode.PositionUpdateListener
        public void positionLost(long frameNumber) {
            if (SurfaceView.DEBUG_POSITION) {
                Log.d(SurfaceView.TAG, String.format("%d windowPositionLost, frameNr = %d", Integer.valueOf(System.identityHashCode(this)), Long.valueOf(frameNumber)));
            }
            SurfaceView.this.mRTLastReportedPosition.setEmpty();
            SurfaceView.this.mRTLastReportedSurfaceSize.set(-1, -1);
            synchronized (SurfaceView.this.mSurfaceControlLock) {
                if (SurfaceView.this.mSurfaceControl == null) {
                    return;
                }
                SurfaceView.this.mRtTransaction.hide(SurfaceView.this.mSurfaceControl);
                SurfaceView surfaceView = SurfaceView.this;
                surfaceView.applyOrMergeTransaction(surfaceView.mRtTransaction, frameNumber);
            }
        }
    }

    private SurfaceHolder.Callback[] getSurfaceCallbacks() {
        SurfaceHolder.Callback[] callbacks;
        synchronized (this.mCallbacks) {
            callbacks = new SurfaceHolder.Callback[this.mCallbacks.size()];
            this.mCallbacks.toArray(callbacks);
        }
        return callbacks;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void runOnUiThread(Runnable runnable) {
        Handler handler = getHandler();
        if (handler != null && handler.getLooper() != Looper.myLooper()) {
            handler.post(runnable);
        } else {
            runnable.run();
        }
    }

    public boolean isFixedSize() {
        return (this.mRequestedWidth == -1 && this.mRequestedHeight == -1) ? false : true;
    }

    private boolean isAboveParent() {
        return this.mSubLayer >= 0;
    }

    public void setResizeBackgroundColor(int bgColor) {
        SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
        setResizeBackgroundColor(transaction, bgColor);
        applyTransactionOnVriDraw(transaction);
        invalidate();
    }

    public void setResizeBackgroundColor(SurfaceControl.Transaction t, int bgColor) {
        if (this.mBackgroundControl == null) {
            return;
        }
        this.mBackgroundColor = bgColor;
        updateBackgroundColor(t);
    }

    public void isRequestChangeScale(String packName, String scale) {
        if (packName != null && scale != null) {
            this.mSurfaceExt.setSurfaceViewScale(packName, scale);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: android.view.SurfaceView$1  reason: invalid class name */
    /* loaded from: classes3.dex */
    public class AnonymousClass1 implements SurfaceHolder {
        private static final String LOG_TAG = "SurfaceHolder";

        AnonymousClass1() {
        }

        @Override // android.view.SurfaceHolder
        public boolean isCreating() {
            return SurfaceView.this.mIsCreating;
        }

        @Override // android.view.SurfaceHolder
        public void addCallback(SurfaceHolder.Callback callback) {
            synchronized (SurfaceView.this.mCallbacks) {
                if (!SurfaceView.this.mCallbacks.contains(callback)) {
                    SurfaceView.this.mCallbacks.add(callback);
                }
            }
        }

        @Override // android.view.SurfaceHolder
        public void removeCallback(SurfaceHolder.Callback callback) {
            synchronized (SurfaceView.this.mCallbacks) {
                SurfaceView.this.mCallbacks.remove(callback);
            }
        }

        @Override // android.view.SurfaceHolder
        public void setFixedSize(int width, int height) {
            if (SurfaceView.this.mRequestedWidth != width || SurfaceView.this.mRequestedHeight != height) {
                if (SurfaceView.DEBUG_POSITION) {
                    Log.d(SurfaceView.TAG, String.format("%d setFixedSize %dx%d -> %dx%d", Integer.valueOf(System.identityHashCode(this)), Integer.valueOf(SurfaceView.this.mRequestedWidth), Integer.valueOf(SurfaceView.this.mRequestedHeight), Integer.valueOf(width), Integer.valueOf(height)));
                }
                SurfaceView.this.mRequestedWidth = width;
                SurfaceView.this.mRequestedHeight = height;
                SurfaceView.this.requestLayout();
            }
        }

        @Override // android.view.SurfaceHolder
        public void setSizeFromLayout() {
            if (SurfaceView.this.mRequestedWidth != -1 || SurfaceView.this.mRequestedHeight != -1) {
                if (SurfaceView.DEBUG_POSITION) {
                    Log.d(SurfaceView.TAG, String.format("%d setSizeFromLayout was %dx%d", Integer.valueOf(System.identityHashCode(this)), Integer.valueOf(SurfaceView.this.mRequestedWidth), Integer.valueOf(SurfaceView.this.mRequestedHeight)));
                }
                SurfaceView surfaceView = SurfaceView.this;
                surfaceView.mRequestedHeight = -1;
                surfaceView.mRequestedWidth = -1;
                SurfaceView.this.requestLayout();
            }
        }

        @Override // android.view.SurfaceHolder
        public void setFormat(int format) {
            if (format == -1) {
                format = 4;
            }
            SurfaceView.this.mRequestedFormat = format;
            if (SurfaceView.this.mSurfaceControl != null) {
                SurfaceView.this.updateSurface();
            }
        }

        @Override // android.view.SurfaceHolder
        @Deprecated
        public void setType(int type) {
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$setKeepScreenOn$0$android-view-SurfaceView$1  reason: not valid java name */
        public /* synthetic */ void m4986lambda$setKeepScreenOn$0$androidviewSurfaceView$1(boolean screenOn) {
            SurfaceView.this.setKeepScreenOn(screenOn);
        }

        @Override // android.view.SurfaceHolder
        public void setKeepScreenOn(final boolean screenOn) {
            SurfaceView.this.runOnUiThread(new Runnable() { // from class: android.view.SurfaceView$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    SurfaceView.AnonymousClass1.this.m4986lambda$setKeepScreenOn$0$androidviewSurfaceView$1(screenOn);
                }
            });
        }

        @Override // android.view.SurfaceHolder
        public Canvas lockCanvas() {
            return internalLockCanvas(null, false);
        }

        @Override // android.view.SurfaceHolder
        public Canvas lockCanvas(Rect inOutDirty) {
            return internalLockCanvas(inOutDirty, false);
        }

        @Override // android.view.SurfaceHolder
        public Canvas lockHardwareCanvas() {
            return internalLockCanvas(null, true);
        }

        private Canvas internalLockCanvas(Rect dirty, boolean hardware) {
            SurfaceView.this.mSurfaceLock.lock();
            if (SurfaceView.DEBUG) {
                Log.i(SurfaceView.TAG, System.identityHashCode(this) + " Locking canvas... stopped=" + SurfaceView.this.mDrawingStopped + ", surfaceControl=" + SurfaceView.this.mSurfaceControl);
            }
            Canvas c = null;
            if (!SurfaceView.this.mDrawingStopped && SurfaceView.this.mSurfaceControl != null) {
                try {
                    if (hardware) {
                        c = SurfaceView.this.mSurface.lockHardwareCanvas();
                    } else {
                        c = SurfaceView.this.mSurface.lockCanvas(dirty);
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Exception locking surface", e);
                }
            }
            if (SurfaceView.DEBUG) {
                Log.i(SurfaceView.TAG, System.identityHashCode(this) + " Returned canvas: " + c);
            }
            if (c != null) {
                SurfaceView.this.mLastLockTime = SystemClock.uptimeMillis();
                return c;
            }
            long now = SystemClock.uptimeMillis();
            long nextTime = SurfaceView.this.mLastLockTime + 100;
            if (nextTime > now) {
                try {
                    Thread.sleep(nextTime - now);
                } catch (InterruptedException e2) {
                }
                now = SystemClock.uptimeMillis();
            }
            SurfaceView.this.mLastLockTime = now;
            SurfaceView.this.mSurfaceLock.unlock();
            return null;
        }

        @Override // android.view.SurfaceHolder
        public void unlockCanvasAndPost(Canvas canvas) {
            if (SurfaceView.DEBUG) {
                Log.i(SurfaceView.TAG, System.identityHashCode(this) + "[unlockCanvasAndPost] canvas:" + canvas);
            }
            try {
                SurfaceView.this.mSurface.unlockCanvasAndPost(canvas);
            } finally {
                SurfaceView.this.mSurfaceLock.unlock();
            }
        }

        @Override // android.view.SurfaceHolder
        public Surface getSurface() {
            return SurfaceView.this.mSurface;
        }

        @Override // android.view.SurfaceHolder
        public Rect getSurfaceFrame() {
            return SurfaceView.this.mSurfaceFrame;
        }
    }

    public SurfaceControl getSurfaceControl() {
        return this.mSurfaceControl;
    }

    public IBinder getHostToken() {
        ViewRootImpl viewRoot = getViewRootImpl();
        if (viewRoot == null) {
            return null;
        }
        return viewRoot.getInputToken();
    }

    @Override // android.view.ViewRootImpl.SurfaceChangedCallback
    public void surfaceCreated(SurfaceControl.Transaction t) {
        ITranSurface.Instance().surfaceCreated(t, new TranSurfaceView(this));
        setWindowStopped(false);
        OSSurfaceView.getInstance().onSurfaceCreated();
    }

    @Override // android.view.ViewRootImpl.SurfaceChangedCallback
    public void surfaceDestroyed() {
        ITranSurface.Instance().surfaceDestroyed(new TranSurfaceView(this));
        setWindowStopped(true);
        this.mRemoteAccessibilityController.disassosciateHierarchy();
        OSSurfaceView.getInstance().onSurfaceDestroyed();
    }

    @Override // android.view.ViewRootImpl.SurfaceChangedCallback
    public void surfaceReplaced(SurfaceControl.Transaction t) {
        if (this.mSurfaceControl != null && this.mBackgroundControl != null) {
            updateRelativeZ(t);
        }
    }

    private void updateRelativeZ(SurfaceControl.Transaction t) {
        ViewRootImpl viewRoot = getViewRootImpl();
        if (viewRoot == null) {
            return;
        }
        SurfaceControl viewRootControl = viewRoot.getSurfaceControl();
        t.setRelativeLayer(this.mBackgroundControl, viewRootControl, Integer.MIN_VALUE);
        t.setRelativeLayer(this.mSurfaceControl, viewRootControl, this.mSubLayer);
    }

    public void setChildSurfacePackage(SurfaceControlViewHost.SurfacePackage p) {
        SurfaceControlViewHost.SurfacePackage surfacePackage = this.mSurfacePackage;
        SurfaceControl lastSc = surfacePackage != null ? surfacePackage.getSurfaceControl() : null;
        SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
        if (this.mSurfaceControl != null) {
            if (lastSc != null) {
                transaction.reparent(lastSc, null);
                this.mSurfacePackage.release();
            }
            reparentSurfacePackage(transaction, p);
            applyTransactionOnVriDraw(transaction);
        }
        this.mSurfacePackage = p;
        invalidate();
    }

    private void reparentSurfacePackage(SurfaceControl.Transaction t, SurfaceControlViewHost.SurfacePackage p) {
        SurfaceControl sc = p.getSurfaceControl();
        if (sc == null || !sc.isValid()) {
            return;
        }
        initEmbeddedHierarchyForAccessibility(p);
        t.reparent(sc, this.mBlastSurfaceControl).show(sc);
    }

    @Override // android.view.View
    public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfoInternal(info);
        if (!this.mRemoteAccessibilityController.connected()) {
            return;
        }
        info.addChild(this.mRemoteAccessibilityController.getLeashToken());
    }

    @Override // android.view.View
    public int getImportantForAccessibility() {
        int mode = super.getImportantForAccessibility();
        RemoteAccessibilityController remoteAccessibilityController = this.mRemoteAccessibilityController;
        if ((remoteAccessibilityController != null && !remoteAccessibilityController.connected()) || mode != 0) {
            return mode;
        }
        return 1;
    }

    private void initEmbeddedHierarchyForAccessibility(SurfaceControlViewHost.SurfacePackage p) {
        IAccessibilityEmbeddedConnection connection = p.getAccessibilityEmbeddedConnection();
        if (this.mRemoteAccessibilityController.alreadyAssociated(connection)) {
            return;
        }
        this.mRemoteAccessibilityController.assosciateHierarchy(connection, getViewRootImpl().mLeashToken, getAccessibilityViewId());
        updateEmbeddedAccessibilityMatrix(true);
    }

    private void notifySurfaceDestroyed() {
        if (this.mSurface.isValid()) {
            if (DEBUG) {
                Log.i(TAG, System.identityHashCode(this) + " surfaceDestroyed");
            }
            SurfaceHolder.Callback[] callbacks = getSurfaceCallbacks();
            for (SurfaceHolder.Callback c : callbacks) {
                c.surfaceDestroyed(this.mSurfaceHolder);
            }
            if (this.isUseResolutiontuner) {
                getViewRootImpl().setSurfaceViewCreated(false);
            }
            if (this.mSurface.isValid()) {
                this.mSurface.forceScopedDisconnect();
            }
        }
    }

    void updateEmbeddedAccessibilityMatrix(boolean force) {
        if (!this.mRemoteAccessibilityController.connected()) {
            return;
        }
        getBoundsOnScreen(this.mTmpRect);
        this.mTmpRect.offset(-this.mAttachInfo.mWindowLeft, -this.mAttachInfo.mWindowTop);
        this.mTmpMatrix.reset();
        this.mTmpMatrix.setTranslate(this.mTmpRect.left, this.mTmpRect.top);
        this.mTmpMatrix.postScale(this.mScreenRect.width() / this.mSurfaceWidth, this.mScreenRect.height() / this.mSurfaceHeight);
        this.mRemoteAccessibilityController.setWindowMatrix(this.mTmpMatrix, force);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.view.View
    public void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        ViewRootImpl viewRoot = getViewRootImpl();
        if (this.mSurfacePackage == null || viewRoot == null) {
            return;
        }
        try {
            viewRoot.mWindowSession.grantEmbeddedWindowFocus(viewRoot.mWindow, this.mSurfacePackage.getInputToken(), gainFocus);
        } catch (Exception e) {
            Log.e(TAG, System.identityHashCode(this) + "Exception requesting focus on embedded window", e);
        }
    }

    private void applyTransactionOnVriDraw(SurfaceControl.Transaction t) {
        ViewRootImpl viewRoot = getViewRootImpl();
        if (viewRoot != null) {
            viewRoot.applyTransactionOnDraw(t);
        } else {
            t.apply();
        }
    }

    public void syncNextFrame(Consumer<SurfaceControl.Transaction> t) {
        this.mBlastBufferQueue.syncNextTransaction(t);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startFetchFps() {
        if (Looper.myLooper() == null) {
            Log.d(TAG, "Looper.myLooper() is null!");
            return;
        }
        this.mGetStatsFpsCounter = 0;
        if (this.mGetFpsHandler == null) {
            this.mGetFpsHandler = new Handler();
        }
        FpsRunnable fpsRunnable = new FpsRunnable();
        this.mFpsRunnable = fpsRunnable;
        this.mGetFpsHandler.postDelayed(fpsRunnable, GET_STATS_FPS_INTERVAL);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopFetchFps() {
        if (this.mGetFpsHandler != null && this.mFpsRunnable != null && Looper.myLooper() != null) {
            this.mGetFpsHandler.removeCallbacks(this.mFpsRunnable);
        }
        this.mGetFpsHandler = null;
        this.mFpsRunnable = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public class FpsRunnable implements Runnable {
        private static final long GET_STATS_FPS_COUNTER_THRESHOLD = 5;
        private static final int VALID_STATS_FPS = 10;

        private FpsRunnable() {
        }

        @Override // java.lang.Runnable
        public void run() {
            float fps = -2.0f;
            SurfaceView.this.mGetStatsFpsCounter++;
            if (SurfaceView.this.mBlastSurfaceControl != null) {
                fps = SurfaceView.this.mBlastSurfaceControl.getSurfaceStatsFps();
            }
            if (ITranSurface.Instance().debugFullScreen()) {
                Log.d(SurfaceView.TAG, "surfaceUpdate() scc getSurfaceStatsFps fps " + fps + " surfaceView : " + SurfaceView.this);
            }
            if (fps > 10.0f) {
                ITranSurface.Instance().hookGetFps(fps);
            } else if (SurfaceView.this.mGetStatsFpsCounter < 5 && SurfaceView.this.mGetFpsHandler != null && SurfaceView.this.mFpsRunnable != null) {
                SurfaceView.this.mGetFpsHandler.postDelayed(SurfaceView.this.mFpsRunnable, SurfaceView.GET_STATS_FPS_INTERVAL);
            }
        }
    }

    /* loaded from: classes3.dex */
    public static class TranSurfaceView {
        private final SurfaceView mSurfaceView;

        public TranSurfaceView(SurfaceView surfaceView) {
            this.mSurfaceView = surfaceView;
        }

        public int getRequestWidth() {
            SurfaceView surfaceView = this.mSurfaceView;
            if (surfaceView == null) {
                return -1;
            }
            int width = surfaceView.mRequestedWidth;
            return width;
        }

        public int getRequestHeight() {
            SurfaceView surfaceView = this.mSurfaceView;
            if (surfaceView == null) {
                return -1;
            }
            int height = surfaceView.mSurfaceHeight;
            return height;
        }

        public boolean getAttachedToWindow() {
            SurfaceView surfaceView = this.mSurfaceView;
            if (surfaceView == null) {
                return false;
            }
            boolean isAttachedToWindow = surfaceView.mAttachedToWindow;
            return isAttachedToWindow;
        }

        public boolean getRequestedVisible() {
            SurfaceView surfaceView = this.mSurfaceView;
            if (surfaceView == null) {
                return false;
            }
            boolean isRequestedVisible = surfaceView.mRequestedVisible;
            return isRequestedVisible;
        }

        public boolean getVisible() {
            SurfaceView surfaceView = this.mSurfaceView;
            if (surfaceView == null) {
                return false;
            }
            boolean isVisible = surfaceView.mVisible;
            return isVisible;
        }

        public boolean getViewVisibility() {
            SurfaceView surfaceView = this.mSurfaceView;
            if (surfaceView == null) {
                return false;
            }
            boolean viewVisibility = surfaceView.mViewVisibility;
            return viewVisibility;
        }

        public SurfaceView getSurfaceView() {
            return this.mSurfaceView;
        }

        public int[] getLocation() {
            int[] location = new int[2];
            SurfaceView surfaceView = this.mSurfaceView;
            if (surfaceView != null) {
                location[0] = surfaceView.mLocation[0];
                location[1] = this.mSurfaceView.mLocation[1];
            }
            return location;
        }

        public boolean getWindowStopped() {
            SurfaceView surfaceView = this.mSurfaceView;
            if (surfaceView == null) {
                return false;
            }
            boolean windowStopped = surfaceView.mWindowStopped;
            return windowStopped;
        }

        public SurfaceControl getSurfaceControl() {
            SurfaceView surfaceView = this.mSurfaceView;
            if (surfaceView == null) {
                return null;
            }
            SurfaceControl surfaceControl = surfaceView.mBlastSurfaceControl;
            return surfaceControl;
        }

        public void startFetchFps() {
            SurfaceView surfaceView = this.mSurfaceView;
            if (surfaceView != null) {
                surfaceView.startFetchFps();
            }
        }

        public void stopFetchFps() {
            SurfaceView surfaceView = this.mSurfaceView;
            if (surfaceView != null) {
                surfaceView.stopFetchFps();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public class TranFoldInnerCustody {
        private ArraySet<Runnable> mSyncBufferCallbacks;
        private Object mSyncBufferCallbacksLock;

        private TranFoldInnerCustody() {
            this.mSyncBufferCallbacksLock = new Object();
            this.mSyncBufferCallbacks = new ArraySet<>();
        }

        public Runnable redrawNeededAsync(final Runnable callbacksCollected) {
            if (disable()) {
                return callbacksCollected;
            }
            synchronized (this.mSyncBufferCallbacksLock) {
                this.mSyncBufferCallbacks.add(callbacksCollected);
            }
            return new Runnable() { // from class: android.view.SurfaceView$TranFoldInnerCustody$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    SurfaceView.TranFoldInnerCustody.this.m4987x7ffe665e(callbacksCollected);
                }
            };
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$redrawNeededAsync$0$android-view-SurfaceView$TranFoldInnerCustody  reason: not valid java name */
        public /* synthetic */ void m4987x7ffe665e(Runnable callbacksCollected) {
            synchronized (this.mSyncBufferCallbacksLock) {
                this.mSyncBufferCallbacks.remove(callbacksCollected);
            }
            callbacksCollected.run();
        }

        public void onDetachedFromWindow() {
            if (disable()) {
                return;
            }
            synchronized (this.mSyncBufferCallbacksLock) {
                if (this.mSyncBufferCallbacks.isEmpty()) {
                    return;
                }
                if (debug()) {
                    Log.d(tag(), System.identityHashCode(this) + "  need clean SyncBufferCallbacks size:" + this.mSyncBufferCallbacks.size());
                }
                ArraySet<Runnable> callbacks = new ArraySet<>(this.mSyncBufferCallbacks);
                this.mSyncBufferCallbacks.clear();
                Iterator<Runnable> it = callbacks.iterator();
                while (it.hasNext()) {
                    Runnable r = it.next();
                    r.run();
                }
            }
        }

        private boolean disable() {
            return TranFoldViewCustody.disable();
        }

        private boolean debug() {
            return true;
        }

        private String tag() {
            return "os.fold.SurfaceView";
        }
    }
}
