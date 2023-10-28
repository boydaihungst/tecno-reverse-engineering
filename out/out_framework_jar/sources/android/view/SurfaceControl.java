package android.view;

import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.graphics.GraphicBuffer;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.HardwareBuffer;
import android.hardware.SyncFence;
import android.hardware.display.DeviceProductInfo;
import android.hardware.display.DisplayedContentSample;
import android.hardware.display.DisplayedContentSamplingAttributes;
import android.hardware.graphics.common.DisplayDecorationSupport;
import android.media.AudioSystem;
import android.media.MediaMetrics;
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseIntArray;
import android.util.proto.ProtoOutputStream;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceControl;
import com.android.internal.util.Preconditions;
import com.android.internal.util.VirtualRefBasePtr;
import dalvik.system.CloseGuard;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import libcore.util.NativeAllocationRegistry;
/* loaded from: classes3.dex */
public final class SurfaceControl implements Parcelable {
    public static final int BUFFER_TRANSFORM_IDENTITY = 0;
    public static final int BUFFER_TRANSFORM_MIRROR_HORIZONTAL = 1;
    public static final int BUFFER_TRANSFORM_MIRROR_VERTICAL = 2;
    public static final int BUFFER_TRANSFORM_ROTATE_180 = 3;
    public static final int BUFFER_TRANSFORM_ROTATE_270 = 7;
    public static final int BUFFER_TRANSFORM_ROTATE_90 = 4;
    public static final int CURSOR_WINDOW = 8192;
    public static final int DISPLAY_DECORATION = 512;
    public static final int DISPLAY_RECEIVES_INPUT = 1;
    public static final int DUAL_DISPLAY = Integer.MIN_VALUE;
    public static final int ENABLE_BACKPRESSURE = 256;
    public static final int FX_SURFACE_BLAST = 262144;
    public static final int FX_SURFACE_CONTAINER = 524288;
    public static final int FX_SURFACE_EFFECT = 131072;
    public static final int FX_SURFACE_MASK = 983040;
    public static final int FX_SURFACE_NORMAL = 0;
    public static final int HIDDEN = 4;
    private static final int INTERNAL_DATASPACE_DISPLAY_P3 = 143261696;
    private static final int INTERNAL_DATASPACE_SCRGB = 411107328;
    private static final int INTERNAL_DATASPACE_SRGB = 142671872;
    public static final int METADATA_ACCESSIBILITY_ID = 5;
    public static final int METADATA_GAME_MODE = 8;
    public static final int METADATA_MOUSE_CURSOR = 4;
    public static final int METADATA_OWNER_PID = 6;
    public static final int METADATA_OWNER_UID = 1;
    public static final int METADATA_TASK_ID = 3;
    public static final int METADATA_WINDOW_TYPE = 2;
    public static final int MULTI_WINDOW_LAYER = 268435456;
    public static final int NON_PREMULTIPLIED = 256;
    public static final int NO_COLOR_FILL = 16384;
    public static final int OPAQUE = 1024;
    public static final int POWER_MODE_DOZE = 1;
    public static final int POWER_MODE_DOZE_SUSPEND = 3;
    public static final int POWER_MODE_NORMAL = 2;
    public static final int POWER_MODE_OFF = 0;
    public static final int POWER_MODE_ON_SUSPEND = 4;
    public static final int PROTECTED_APP = 2048;
    public static final int SECURE = 128;
    public static final int SKIP_PARENT_INTERSECT = 536870912;
    public static final int SKIP_SCREENSHOT = 64;
    private static final int SURFACE_HIDDEN = 1;
    private static final int SURFACE_OPAQUE = 2;
    public static final int SURFACE_SAFESCREENCAP = 8;
    private static final String TAG = "SurfaceControl";
    static GlobalTransactionWrapper sGlobalTransaction;
    boolean isSurfaceControlUsed;
    private final CloseGuard mCloseGuard;
    private int mHeight;
    private WeakReference<View> mLocalOwnerView;
    private final Object mLock;
    private String mName;
    private long mNativeHandle;
    public long mNativeObject;
    private ArrayList<OnReparentListener> mReparentListeners;
    boolean mSurfaceRelease;
    private int mWidth;
    RuntimeException re;
    private static final boolean LOG_SURFACE_CONTROL = SystemProperties.getBoolean("debug.surfacecontrol.log", false);
    private static final boolean TRAN_USER_ROOT_SUPPORT = "1".equals(SystemProperties.get("persist.sys.adb.support", AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS));
    static long sTransactionNestCount = 0;
    public static final Parcelable.Creator<SurfaceControl> CREATOR = new Parcelable.Creator<SurfaceControl>() { // from class: android.view.SurfaceControl.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SurfaceControl createFromParcel(Parcel in) {
            return new SurfaceControl(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SurfaceControl[] newArray(int size) {
            return new SurfaceControl[size];
        }
    };

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes3.dex */
    public @interface BufferTransform {
    }

    /* loaded from: classes3.dex */
    public static final class CieXyz {
        public float X;
        public float Y;
        public float Z;
    }

    /* loaded from: classes3.dex */
    public static final class DisplayPrimaries {
        public CieXyz blue;
        public CieXyz green;
        public CieXyz red;
        public CieXyz white;
    }

    /* loaded from: classes3.dex */
    public static abstract class OnJankDataListener {
        private final VirtualRefBasePtr mNativePtr = new VirtualRefBasePtr(SurfaceControl.nativeCreateJankDataListenerWrapper(this));

        public abstract void onJankDataAvailable(JankData[] jankDataArr);
    }

    /* loaded from: classes3.dex */
    public interface OnReparentListener {
        void onReparent(Transaction transaction, SurfaceControl surfaceControl);
    }

    /* loaded from: classes3.dex */
    public interface ScreenCaptureListener {
        void onScreenCaptureComplete(ScreenshotHardwareBuffer screenshotHardwareBuffer);
    }

    /* loaded from: classes3.dex */
    public interface TransactionCommittedListener {
        void onTransactionCommitted();
    }

    /* renamed from: -$$Nest$smnativeCreateTransaction  reason: not valid java name */
    static /* bridge */ /* synthetic */ long m4888$$Nest$smnativeCreateTransaction() {
        return nativeCreateTransaction();
    }

    /* renamed from: -$$Nest$smnativeGetNativeTransactionFinalizer  reason: not valid java name */
    static /* bridge */ /* synthetic */ long m4889$$Nest$smnativeGetNativeTransactionFinalizer() {
        return nativeGetNativeTransactionFinalizer();
    }

    private static native void nativeAddJankDataListener(long j, long j2);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeAddTransactionCommittedListener(long j, TransactionCommittedListener transactionCommittedListener);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeApplyTransaction(long j, boolean z);

    private static native int nativeCaptureDisplay(DisplayCaptureArgs displayCaptureArgs, ScreenCaptureListener screenCaptureListener);

    private static native int nativeCaptureLayers(LayerCaptureArgs layerCaptureArgs, ScreenCaptureListener screenCaptureListener);

    private static native boolean nativeClearAnimationFrameStats();

    private static native void nativeClearBootDisplayMode(IBinder iBinder);

    private static native boolean nativeClearContentFrameStats(long j);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeClearTransaction(long j);

    private static native long nativeCopyFromSurfaceControl(long j);

    private static native long nativeCreate(SurfaceSession surfaceSession, String str, int i, int i2, int i3, int i4, long j, Parcel parcel) throws Surface.OutOfResourcesException;

    private static native IBinder nativeCreateDisplay(String str, boolean z);

    /* JADX INFO: Access modifiers changed from: private */
    public static native long nativeCreateJankDataListenerWrapper(OnJankDataListener onJankDataListener);

    private static native long nativeCreateTransaction();

    private static native void nativeDestroyDisplay(IBinder iBinder);

    private static native void nativeDisconnect(long j);

    private static native boolean nativeGetAnimationFrameStats(WindowAnimationFrameStats windowAnimationFrameStats);

    private static native boolean nativeGetBootDisplayModeSupport();

    private static native int[] nativeGetCompositionDataspaces();

    private static native boolean nativeGetContentFrameStats(long j, WindowContentFrameStats windowContentFrameStats);

    private static native DesiredDisplayModeSpecs nativeGetDesiredDisplayModeSpecs(IBinder iBinder);

    private static native boolean nativeGetDisplayBrightnessSupport(IBinder iBinder);

    private static native DisplayDecorationSupport nativeGetDisplayDecorationSupport(IBinder iBinder);

    private static native DisplayPrimaries nativeGetDisplayNativePrimaries(IBinder iBinder);

    private static native DisplayedContentSample nativeGetDisplayedContentSample(IBinder iBinder, long j, long j2);

    private static native DisplayedContentSamplingAttributes nativeGetDisplayedContentSamplingAttributes(IBinder iBinder);

    private static native DynamicDisplayInfo nativeGetDynamicDisplayInfo(IBinder iBinder);

    private static native int nativeGetGPUContextPriority();

    private static native long nativeGetHandle(long j);

    private static native int nativeGetLayerId(long j);

    private static native long nativeGetNativeTransactionFinalizer();

    private static native long[] nativeGetPhysicalDisplayIds();

    private static native IBinder nativeGetPhysicalDisplayToken(long j);

    private static native long nativeGetPrimaryPhysicalDisplayId();

    private static native boolean nativeGetProtectedContentSupport();

    private static native StaticDisplayInfo nativeGetStaticDisplayInfo(IBinder iBinder);

    private static native float nativeGetSurfaceStatsFps(long j);

    private static native int nativeGetTransformHint(long j);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeMergeTransaction(long j, long j2);

    private static native long nativeMirrorSurface(long j);

    private static native void nativeOverrideHdrTypes(IBinder iBinder, int[] iArr);

    private static native long nativeReadFromParcel(Parcel parcel);

    /* JADX INFO: Access modifiers changed from: private */
    public static native long nativeReadTransactionFromParcel(Parcel parcel);

    private static native void nativeRelease(long j);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeRemoveCurrentInputFocus(long j, int i);

    private static native void nativeRemoveJankDataListener(long j);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeReparent(long j, long j2, long j3);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSanitize(long j);

    private static native boolean nativeSetActiveColorMode(IBinder iBinder, int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetAlpha(long j, long j2, float f);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetAnimationTransaction(long j);

    private static native void nativeSetAutoLowLatencyMode(IBinder iBinder, boolean z);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetBackgroundBlurRadius(long j, long j2, int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetBlurRegions(long j, long j2, float[][] fArr, int i);

    private static native void nativeSetBootDisplayMode(IBinder iBinder, int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetBuffer(long j, long j2, HardwareBuffer hardwareBuffer, long j3, Consumer<SyncFence> consumer);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetBufferTransform(long j, long j2, int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetColor(long j, long j2, float[] fArr);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetColorSpaceAgnostic(long j, long j2, boolean z);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetColorTransform(long j, long j2, float[] fArr, float[] fArr2);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetCornerRadius(long j, long j2, float f);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetDamageRegion(long j, long j2, Region region);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetDataSpace(long j, long j2, int i);

    private static native boolean nativeSetDesiredDisplayModeSpecs(IBinder iBinder, DesiredDisplayModeSpecs desiredDisplayModeSpecs);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetDestinationFrame(long j, long j2, int i, int i2, int i3, int i4);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetDimmingEnabled(long j, long j2, boolean z);

    private static native boolean nativeSetDisplayBrightness(IBinder iBinder, float f, float f2, float f3, float f4);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetDisplayFlags(long j, IBinder iBinder, int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetDisplayLayerStack(long j, IBinder iBinder, int i);

    private static native void nativeSetDisplayPowerMode(IBinder iBinder, int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetDisplayProjection(long j, IBinder iBinder, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetDisplaySize(long j, IBinder iBinder, int i, int i2);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetDisplaySurface(long j, IBinder iBinder, long j2);

    private static native boolean nativeSetDisplayedContentSamplingEnabled(IBinder iBinder, boolean z, int i, int i2);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetDropInputMode(long j, long j2, int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetEarlyWakeupEnd(long j);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetEarlyWakeupStart(long j);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetFixedTransformHint(long j, long j2, int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetFlags(long j, long j2, int i, int i2);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetFocusedWindow(long j, IBinder iBinder, String str, IBinder iBinder2, String str2, int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetFrameRate(long j, long j2, float f, int i, int i2);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetFrameRateSelectionPriority(long j, long j2, int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetFrameTimelineVsync(long j, long j2);

    private static native void nativeSetGameContentType(IBinder iBinder, boolean z);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetGeometry(long j, long j2, Rect rect, Rect rect2, long j3);

    private static native void nativeSetGlobalShadowSettings(float[] fArr, float[] fArr2, float f, float f2, float f3);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetInputWindowInfo(long j, long j2, InputWindowHandle inputWindowHandle);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetLayer(long j, long j2, int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetLayerStack(long j, long j2, int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetMatrix(long j, long j2, float f, float f2, float f3, float f4);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetMetadata(long j, long j2, int i, Parcel parcel);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetPosition(long j, long j2, float f, float f2);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetRelativeLayer(long j, long j2, long j3, int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetSaturation(long j, long j2, float f, int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetScale(long j, long j2, float f, float f2);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetShadowRadius(long j, long j2, float f);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetSize(long j, long j2, int i, int i2);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetStretchEffect(long j, long j2, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10);

    private static native void nativeSetTransformHint(long j, int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetTransparentRegionHint(long j, long j2, Region region);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetTrustedOverlay(long j, long j2, boolean z);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetWindowCrop(long j, long j2, int i, int i2, int i3, int i4);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSyncInputWindows(long j);

    private static native void nativeUpdateDefaultBufferSize(long j, int i, int i2);

    private static native void nativeWriteToParcel(long j, Parcel parcel);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeWriteTransactionToParcel(long j, Parcel parcel);

    public static int rotationToBufferTransform(int rotation) {
        switch (rotation) {
            case 0:
                return 0;
            case 1:
                return 4;
            case 2:
                return 3;
            case 3:
                return 7;
            default:
                Log.e(TAG, "Trying to convert unknown rotation=" + rotation);
                return 0;
        }
    }

    /* loaded from: classes3.dex */
    public static class JankData {
        public static final int BUFFER_STUFFING = 64;
        public static final int DISPLAY_HAL = 1;
        public static final int JANK_APP_DEADLINE_MISSED = 8;
        public static final int JANK_NONE = 0;
        public static final int JANK_SURFACEFLINGER_DEADLINE_MISSED = 2;
        public static final int JANK_SURFACEFLINGER_GPU_DEADLINE_MISSED = 4;
        public static final int PREDICTION_ERROR = 16;
        public static final int SURFACE_FLINGER_SCHEDULING = 32;
        public static final int UNKNOWN = 128;
        public final long frameVsyncId;
        public final int jankType;

        @Retention(RetentionPolicy.SOURCE)
        /* loaded from: classes3.dex */
        public @interface JankType {
        }

        public JankData(long frameVsyncId, int jankType) {
            this.frameVsyncId = frameVsyncId;
            this.jankType = jankType;
        }
    }

    public boolean addOnReparentListener(OnReparentListener listener) {
        boolean add;
        synchronized (this.mLock) {
            if (this.mReparentListeners == null) {
                this.mReparentListeners = new ArrayList<>(1);
            }
            add = this.mReparentListeners.add(listener);
        }
        return add;
    }

    public boolean removeOnReparentListener(OnReparentListener listener) {
        boolean removed;
        synchronized (this.mLock) {
            removed = this.mReparentListeners.remove(listener);
            if (this.mReparentListeners.isEmpty()) {
                this.mReparentListeners = null;
            }
        }
        return removed;
    }

    private void assignNativeObject(long nativeObject, String callsite) {
        if (this.mNativeObject != 0) {
            release();
        }
        if (nativeObject != 0) {
            this.mCloseGuard.openWithCallSite("release", callsite);
        }
        this.mNativeObject = nativeObject;
        this.mNativeHandle = nativeObject != 0 ? nativeGetHandle(nativeObject) : 0L;
    }

    public void copyFrom(SurfaceControl other, String callsite) {
        this.mName = other.mName;
        this.mWidth = other.mWidth;
        this.mHeight = other.mHeight;
        this.mLocalOwnerView = other.mLocalOwnerView;
        assignNativeObject(nativeCopyFromSurfaceControl(other.mNativeObject), callsite);
    }

    /* loaded from: classes3.dex */
    public static class ScreenshotHardwareBuffer {
        private final ColorSpace mColorSpace;
        private final boolean mContainsHdrLayers;
        private final boolean mContainsSecureLayers;
        private final HardwareBuffer mHardwareBuffer;

        public ScreenshotHardwareBuffer(HardwareBuffer hardwareBuffer, ColorSpace colorSpace, boolean containsSecureLayers, boolean containsHdrLayers) {
            this.mHardwareBuffer = hardwareBuffer;
            this.mColorSpace = colorSpace;
            this.mContainsSecureLayers = containsSecureLayers;
            this.mContainsHdrLayers = containsHdrLayers;
        }

        private static ScreenshotHardwareBuffer createFromNative(HardwareBuffer hardwareBuffer, int namedColorSpace, boolean containsSecureLayers, boolean containsHdrLayers) {
            ColorSpace colorSpace = ColorSpace.get(ColorSpace.Named.values()[namedColorSpace]);
            return new ScreenshotHardwareBuffer(hardwareBuffer, colorSpace, containsSecureLayers, containsHdrLayers);
        }

        public ColorSpace getColorSpace() {
            return this.mColorSpace;
        }

        public HardwareBuffer getHardwareBuffer() {
            return this.mHardwareBuffer;
        }

        public boolean containsSecureLayers() {
            return this.mContainsSecureLayers;
        }

        public boolean containsHdrLayers() {
            return this.mContainsHdrLayers;
        }

        public Bitmap asBitmap() {
            if (this.mHardwareBuffer == null) {
                Log.w(SurfaceControl.TAG, "Failed to take screenshot. Null screenshot object");
                return null;
            }
            if (SurfaceControl.TRAN_USER_ROOT_SUPPORT) {
                Log.d(SurfaceControl.TAG, "saveBitmap....start");
                SurfaceControl.saveBitmap(Bitmap.wrapHardwareBuffer(this.mHardwareBuffer, this.mColorSpace), "/data/local/test_capturedisplay/captureDisplay");
            }
            return Bitmap.wrapHardwareBuffer(this.mHardwareBuffer, this.mColorSpace);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void saveBitmap(Bitmap bitmap, String path) {
        DateFormat df = new SimpleDateFormat("yyyyMMddhhmmss");
        String filename = path + df.format(new Date()) + MediaMetrics.SEPARATOR + ".JPEG";
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            try {
                File filePic = new File(filename);
                if (!filePic.exists()) {
                    filePic.getParentFile().mkdirs();
                    filePic.createNewFile();
                }
                FileOutputStream fos = new FileOutputStream(filePic);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
                fos.close();
                Log.d(TAG, "saveBitmap success: " + filePic.getAbsolutePath());
                return;
            } catch (IOException e) {
                Log.d(TAG, "saveBitmap: " + e.getMessage());
                return;
            }
        }
        Log.d(TAG, "saveBitmap failure : sdcard not mounted");
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public static class SyncScreenCaptureListener implements ScreenCaptureListener {
        private static final int SCREENSHOT_WAIT_TIME_S = 1;
        private final CountDownLatch mCountDownLatch;
        private ScreenshotHardwareBuffer mScreenshotHardwareBuffer;

        private SyncScreenCaptureListener() {
            this.mCountDownLatch = new CountDownLatch(1);
        }

        @Override // android.view.SurfaceControl.ScreenCaptureListener
        public void onScreenCaptureComplete(ScreenshotHardwareBuffer hardwareBuffer) {
            this.mScreenshotHardwareBuffer = hardwareBuffer;
            this.mCountDownLatch.countDown();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public ScreenshotHardwareBuffer waitForScreenshot() {
            try {
                this.mCountDownLatch.await(1L, TimeUnit.SECONDS);
            } catch (Exception e) {
                Log.e(SurfaceControl.TAG, "Failed to wait for screen capture result", e);
            }
            return this.mScreenshotHardwareBuffer;
        }
    }

    /* loaded from: classes3.dex */
    private static abstract class CaptureArgs {
        private final boolean mAllowProtected;
        private final boolean mCaptureSecureLayers;
        private final float mFrameScaleX;
        private final float mFrameScaleY;
        private final boolean mGrayscale;
        private final int mPixelFormat;
        private final Rect mSourceCrop;
        private final long mUid;

        private CaptureArgs(Builder<? extends Builder<?>> builder) {
            Rect rect = new Rect();
            this.mSourceCrop = rect;
            this.mPixelFormat = ((Builder) builder).mPixelFormat;
            rect.set(((Builder) builder).mSourceCrop);
            this.mFrameScaleX = ((Builder) builder).mFrameScaleX;
            this.mFrameScaleY = ((Builder) builder).mFrameScaleY;
            this.mCaptureSecureLayers = ((Builder) builder).mCaptureSecureLayers;
            this.mAllowProtected = ((Builder) builder).mAllowProtected;
            this.mUid = ((Builder) builder).mUid;
            this.mGrayscale = ((Builder) builder).mGrayscale;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes3.dex */
        public static abstract class Builder<T extends Builder<T>> {
            private boolean mAllowProtected;
            private boolean mCaptureSecureLayers;
            private boolean mGrayscale;
            private int mPixelFormat = 1;
            private final Rect mSourceCrop = new Rect();
            private float mFrameScaleX = 1.0f;
            private float mFrameScaleY = 1.0f;
            private long mUid = -1;

            abstract T getThis();

            Builder() {
            }

            public T setPixelFormat(int pixelFormat) {
                this.mPixelFormat = pixelFormat;
                return getThis();
            }

            public T setSourceCrop(Rect sourceCrop) {
                this.mSourceCrop.set(sourceCrop);
                return getThis();
            }

            public T setFrameScale(float frameScale) {
                this.mFrameScaleX = frameScale;
                this.mFrameScaleY = frameScale;
                return getThis();
            }

            public T setFrameScale(float frameScaleX, float frameScaleY) {
                this.mFrameScaleX = frameScaleX;
                this.mFrameScaleY = frameScaleY;
                return getThis();
            }

            public T setCaptureSecureLayers(boolean captureSecureLayers) {
                this.mCaptureSecureLayers = captureSecureLayers;
                return getThis();
            }

            public T setAllowProtected(boolean allowProtected) {
                this.mAllowProtected = false;
                return getThis();
            }

            public T setUid(long uid) {
                this.mUid = uid;
                return getThis();
            }

            public T setGrayscale(boolean grayscale) {
                this.mGrayscale = grayscale;
                return getThis();
            }
        }
    }

    /* loaded from: classes3.dex */
    public static class DisplayCaptureArgs extends CaptureArgs {
        private final IBinder mDisplayToken;
        private final int mHeight;
        private final boolean mUseIdentityTransform;
        private final int mWidth;

        private DisplayCaptureArgs(Builder builder) {
            super(builder);
            this.mDisplayToken = builder.mDisplayToken;
            this.mWidth = builder.mWidth;
            this.mHeight = builder.mHeight;
            this.mUseIdentityTransform = builder.mUseIdentityTransform;
        }

        /* loaded from: classes3.dex */
        public static class Builder extends CaptureArgs.Builder<Builder> {
            private IBinder mDisplayToken;
            private int mHeight;
            private boolean mUseIdentityTransform;
            private int mWidth;

            /* JADX DEBUG: Return type fixed from 'android.view.SurfaceControl$CaptureArgs$Builder' to match base method */
            /* JADX WARN: Type inference failed for: r1v1, types: [android.view.SurfaceControl$CaptureArgs$Builder, android.view.SurfaceControl$DisplayCaptureArgs$Builder] */
            @Override // android.view.SurfaceControl.CaptureArgs.Builder
            public /* bridge */ /* synthetic */ Builder setAllowProtected(boolean z) {
                return super.setAllowProtected(z);
            }

            /* JADX DEBUG: Return type fixed from 'android.view.SurfaceControl$CaptureArgs$Builder' to match base method */
            /* JADX WARN: Type inference failed for: r1v1, types: [android.view.SurfaceControl$CaptureArgs$Builder, android.view.SurfaceControl$DisplayCaptureArgs$Builder] */
            @Override // android.view.SurfaceControl.CaptureArgs.Builder
            public /* bridge */ /* synthetic */ Builder setCaptureSecureLayers(boolean z) {
                return super.setCaptureSecureLayers(z);
            }

            /* JADX DEBUG: Return type fixed from 'android.view.SurfaceControl$CaptureArgs$Builder' to match base method */
            /* JADX WARN: Type inference failed for: r1v1, types: [android.view.SurfaceControl$CaptureArgs$Builder, android.view.SurfaceControl$DisplayCaptureArgs$Builder] */
            @Override // android.view.SurfaceControl.CaptureArgs.Builder
            public /* bridge */ /* synthetic */ Builder setFrameScale(float f) {
                return super.setFrameScale(f);
            }

            /* JADX DEBUG: Return type fixed from 'android.view.SurfaceControl$CaptureArgs$Builder' to match base method */
            /* JADX WARN: Type inference failed for: r1v1, types: [android.view.SurfaceControl$CaptureArgs$Builder, android.view.SurfaceControl$DisplayCaptureArgs$Builder] */
            @Override // android.view.SurfaceControl.CaptureArgs.Builder
            public /* bridge */ /* synthetic */ Builder setFrameScale(float f, float f2) {
                return super.setFrameScale(f, f2);
            }

            /* JADX DEBUG: Return type fixed from 'android.view.SurfaceControl$CaptureArgs$Builder' to match base method */
            /* JADX WARN: Type inference failed for: r1v1, types: [android.view.SurfaceControl$CaptureArgs$Builder, android.view.SurfaceControl$DisplayCaptureArgs$Builder] */
            @Override // android.view.SurfaceControl.CaptureArgs.Builder
            public /* bridge */ /* synthetic */ Builder setGrayscale(boolean z) {
                return super.setGrayscale(z);
            }

            /* JADX DEBUG: Return type fixed from 'android.view.SurfaceControl$CaptureArgs$Builder' to match base method */
            /* JADX WARN: Type inference failed for: r1v1, types: [android.view.SurfaceControl$CaptureArgs$Builder, android.view.SurfaceControl$DisplayCaptureArgs$Builder] */
            @Override // android.view.SurfaceControl.CaptureArgs.Builder
            public /* bridge */ /* synthetic */ Builder setPixelFormat(int i) {
                return super.setPixelFormat(i);
            }

            /* JADX DEBUG: Return type fixed from 'android.view.SurfaceControl$CaptureArgs$Builder' to match base method */
            /* JADX WARN: Type inference failed for: r1v1, types: [android.view.SurfaceControl$CaptureArgs$Builder, android.view.SurfaceControl$DisplayCaptureArgs$Builder] */
            @Override // android.view.SurfaceControl.CaptureArgs.Builder
            public /* bridge */ /* synthetic */ Builder setSourceCrop(Rect rect) {
                return super.setSourceCrop(rect);
            }

            /* JADX DEBUG: Return type fixed from 'android.view.SurfaceControl$CaptureArgs$Builder' to match base method */
            /* JADX WARN: Type inference failed for: r1v1, types: [android.view.SurfaceControl$CaptureArgs$Builder, android.view.SurfaceControl$DisplayCaptureArgs$Builder] */
            @Override // android.view.SurfaceControl.CaptureArgs.Builder
            public /* bridge */ /* synthetic */ Builder setUid(long j) {
                return super.setUid(j);
            }

            public DisplayCaptureArgs build() {
                if (this.mDisplayToken == null) {
                    throw new IllegalStateException("Can't take screenshot with null display token");
                }
                return new DisplayCaptureArgs(this);
            }

            public Builder(IBinder displayToken) {
                setDisplayToken(displayToken);
            }

            public Builder setDisplayToken(IBinder displayToken) {
                this.mDisplayToken = displayToken;
                return this;
            }

            public Builder setSize(int width, int height) {
                this.mWidth = width;
                this.mHeight = height;
                return this;
            }

            public Builder setUseIdentityTransform(boolean useIdentityTransform) {
                this.mUseIdentityTransform = useIdentityTransform;
                return this;
            }

            /* JADX DEBUG: Method merged with bridge method */
            /* JADX INFO: Access modifiers changed from: package-private */
            @Override // android.view.SurfaceControl.CaptureArgs.Builder
            public Builder getThis() {
                return this;
            }
        }
    }

    /* loaded from: classes3.dex */
    public static class LayerCaptureArgs extends CaptureArgs {
        private final boolean mChildrenOnly;
        private final boolean mIsScreenShotWallPaper;
        private final long[] mNativeExcludeLayers;
        private final long mNativeLayer;

        private LayerCaptureArgs(Builder builder) {
            super(builder);
            this.mChildrenOnly = builder.mChildrenOnly;
            this.mIsScreenShotWallPaper = builder.mIsScreenShotWallPaper;
            this.mNativeLayer = builder.mLayer.mNativeObject;
            if (builder.mExcludeLayers != null) {
                this.mNativeExcludeLayers = new long[builder.mExcludeLayers.length];
                for (int i = 0; i < builder.mExcludeLayers.length; i++) {
                    this.mNativeExcludeLayers[i] = builder.mExcludeLayers[i].mNativeObject;
                }
                return;
            }
            this.mNativeExcludeLayers = null;
        }

        /* loaded from: classes3.dex */
        public static class Builder extends CaptureArgs.Builder<Builder> {
            private SurfaceControl[] mExcludeLayers;
            private SurfaceControl mLayer;
            private boolean mChildrenOnly = true;
            private boolean mIsScreenShotWallPaper = false;

            /* JADX DEBUG: Return type fixed from 'android.view.SurfaceControl$CaptureArgs$Builder' to match base method */
            /* JADX WARN: Type inference failed for: r1v1, types: [android.view.SurfaceControl$CaptureArgs$Builder, android.view.SurfaceControl$LayerCaptureArgs$Builder] */
            @Override // android.view.SurfaceControl.CaptureArgs.Builder
            public /* bridge */ /* synthetic */ Builder setAllowProtected(boolean z) {
                return super.setAllowProtected(z);
            }

            /* JADX DEBUG: Return type fixed from 'android.view.SurfaceControl$CaptureArgs$Builder' to match base method */
            /* JADX WARN: Type inference failed for: r1v1, types: [android.view.SurfaceControl$CaptureArgs$Builder, android.view.SurfaceControl$LayerCaptureArgs$Builder] */
            @Override // android.view.SurfaceControl.CaptureArgs.Builder
            public /* bridge */ /* synthetic */ Builder setCaptureSecureLayers(boolean z) {
                return super.setCaptureSecureLayers(z);
            }

            /* JADX DEBUG: Return type fixed from 'android.view.SurfaceControl$CaptureArgs$Builder' to match base method */
            /* JADX WARN: Type inference failed for: r1v1, types: [android.view.SurfaceControl$CaptureArgs$Builder, android.view.SurfaceControl$LayerCaptureArgs$Builder] */
            @Override // android.view.SurfaceControl.CaptureArgs.Builder
            public /* bridge */ /* synthetic */ Builder setFrameScale(float f) {
                return super.setFrameScale(f);
            }

            /* JADX DEBUG: Return type fixed from 'android.view.SurfaceControl$CaptureArgs$Builder' to match base method */
            /* JADX WARN: Type inference failed for: r1v1, types: [android.view.SurfaceControl$CaptureArgs$Builder, android.view.SurfaceControl$LayerCaptureArgs$Builder] */
            @Override // android.view.SurfaceControl.CaptureArgs.Builder
            public /* bridge */ /* synthetic */ Builder setFrameScale(float f, float f2) {
                return super.setFrameScale(f, f2);
            }

            /* JADX DEBUG: Return type fixed from 'android.view.SurfaceControl$CaptureArgs$Builder' to match base method */
            /* JADX WARN: Type inference failed for: r1v1, types: [android.view.SurfaceControl$CaptureArgs$Builder, android.view.SurfaceControl$LayerCaptureArgs$Builder] */
            @Override // android.view.SurfaceControl.CaptureArgs.Builder
            public /* bridge */ /* synthetic */ Builder setGrayscale(boolean z) {
                return super.setGrayscale(z);
            }

            /* JADX DEBUG: Return type fixed from 'android.view.SurfaceControl$CaptureArgs$Builder' to match base method */
            /* JADX WARN: Type inference failed for: r1v1, types: [android.view.SurfaceControl$CaptureArgs$Builder, android.view.SurfaceControl$LayerCaptureArgs$Builder] */
            @Override // android.view.SurfaceControl.CaptureArgs.Builder
            public /* bridge */ /* synthetic */ Builder setPixelFormat(int i) {
                return super.setPixelFormat(i);
            }

            /* JADX DEBUG: Return type fixed from 'android.view.SurfaceControl$CaptureArgs$Builder' to match base method */
            /* JADX WARN: Type inference failed for: r1v1, types: [android.view.SurfaceControl$CaptureArgs$Builder, android.view.SurfaceControl$LayerCaptureArgs$Builder] */
            @Override // android.view.SurfaceControl.CaptureArgs.Builder
            public /* bridge */ /* synthetic */ Builder setSourceCrop(Rect rect) {
                return super.setSourceCrop(rect);
            }

            /* JADX DEBUG: Return type fixed from 'android.view.SurfaceControl$CaptureArgs$Builder' to match base method */
            /* JADX WARN: Type inference failed for: r1v1, types: [android.view.SurfaceControl$CaptureArgs$Builder, android.view.SurfaceControl$LayerCaptureArgs$Builder] */
            @Override // android.view.SurfaceControl.CaptureArgs.Builder
            public /* bridge */ /* synthetic */ Builder setUid(long j) {
                return super.setUid(j);
            }

            public LayerCaptureArgs build() {
                if (this.mLayer == null) {
                    throw new IllegalStateException("Can't take screenshot with null layer");
                }
                return new LayerCaptureArgs(this);
            }

            public Builder(SurfaceControl layer) {
                setLayer(layer);
            }

            public Builder setLayer(SurfaceControl layer) {
                this.mLayer = layer;
                return this;
            }

            public Builder setExcludeLayers(SurfaceControl[] excludeLayers) {
                this.mExcludeLayers = excludeLayers;
                return this;
            }

            public Builder setChildrenOnly(boolean childrenOnly) {
                this.mChildrenOnly = childrenOnly;
                return this;
            }

            public Builder setIsScreenShotWallPaper(boolean isScreenShotWallPaper) {
                this.mIsScreenShotWallPaper = isScreenShotWallPaper;
                return this;
            }

            /* JADX DEBUG: Method merged with bridge method */
            /* JADX INFO: Access modifiers changed from: package-private */
            @Override // android.view.SurfaceControl.CaptureArgs.Builder
            public Builder getThis() {
                return this;
            }
        }
    }

    /* loaded from: classes3.dex */
    public static class Builder {
        private int mHeight;
        private WeakReference<View> mLocalOwnerView;
        private SparseIntArray mMetadata;
        private String mName;
        private SurfaceControl mParent;
        private SurfaceSession mSession;
        private int mWidth;
        private int mFlags = 4;
        private int mFormat = -1;
        private String mCallsite = "SurfaceControl.Builder";

        public Builder(SurfaceSession session) {
            this.mSession = session;
        }

        public Builder() {
        }

        public SurfaceControl build() {
            int i;
            int i2 = this.mWidth;
            if (i2 < 0 || (i = this.mHeight) < 0) {
                throw new IllegalStateException("width and height must be positive or unset");
            }
            if ((i2 > 0 || i > 0) && (isEffectLayer() || isContainerLayer())) {
                throw new IllegalStateException("Only buffer layers can set a valid buffer size.");
            }
            if ((this.mFlags & SurfaceControl.FX_SURFACE_MASK) == 0) {
                setBLASTLayer();
            }
            return new SurfaceControl(this.mSession, this.mName, this.mWidth, this.mHeight, this.mFormat, this.mFlags, this.mParent, this.mMetadata, this.mLocalOwnerView, this.mCallsite);
        }

        public Builder setName(String name) {
            this.mName = name;
            return this;
        }

        public Builder setLocalOwnerView(View view) {
            this.mLocalOwnerView = new WeakReference<>(view);
            return this;
        }

        public Builder setBufferSize(int width, int height) {
            if (width < 0 || height < 0) {
                throw new IllegalArgumentException("width and height must be positive");
            }
            this.mWidth = width;
            this.mHeight = height;
            return setFlags(0, SurfaceControl.FX_SURFACE_MASK);
        }

        private void unsetBufferSize() {
            this.mWidth = 0;
            this.mHeight = 0;
        }

        public Builder setFormat(int format) {
            this.mFormat = format;
            return this;
        }

        public Builder setProtected(boolean protectedContent) {
            if (protectedContent) {
                this.mFlags |= 2048;
            } else {
                this.mFlags &= -2049;
            }
            return this;
        }

        public Builder setSecure(boolean secure) {
            if (secure) {
                this.mFlags |= 128;
            } else {
                this.mFlags &= -129;
            }
            return this;
        }

        public Builder setOpaque(boolean opaque) {
            if (opaque) {
                this.mFlags |= 1024;
            } else {
                this.mFlags &= -1025;
            }
            return this;
        }

        public Builder setHidden(boolean hidden) {
            if (hidden) {
                this.mFlags |= 4;
            } else {
                this.mFlags &= -5;
            }
            return this;
        }

        public Builder setParent(SurfaceControl parent) {
            this.mParent = parent;
            return this;
        }

        public Builder setMetadata(int key, int data) {
            if (this.mMetadata == null) {
                this.mMetadata = new SparseIntArray();
            }
            this.mMetadata.put(key, data);
            return this;
        }

        public Builder setEffectLayer() {
            this.mFlags |= 16384;
            unsetBufferSize();
            return setFlags(131072, SurfaceControl.FX_SURFACE_MASK);
        }

        public Builder setColorLayer() {
            unsetBufferSize();
            return setFlags(131072, SurfaceControl.FX_SURFACE_MASK);
        }

        private boolean isEffectLayer() {
            return (this.mFlags & 131072) == 131072;
        }

        public Builder setBLASTLayer() {
            return setFlags(262144, SurfaceControl.FX_SURFACE_MASK);
        }

        public Builder setContainerLayer() {
            unsetBufferSize();
            return setFlags(524288, SurfaceControl.FX_SURFACE_MASK);
        }

        private boolean isContainerLayer() {
            return (this.mFlags & 524288) == 524288;
        }

        public Builder setFlags(int flags) {
            this.mFlags = flags;
            return this;
        }

        public Builder setCallsite(String callsite) {
            this.mCallsite = callsite;
            return this;
        }

        private Builder setFlags(int flags, int mask) {
            this.mFlags = (this.mFlags & (~mask)) | flags;
            return this;
        }
    }

    private SurfaceControl(SurfaceSession session, String name, int w, int h, int format, int flags, SurfaceControl parent, SparseIntArray metadata, WeakReference<View> localOwnerView, String callsite) throws Surface.OutOfResourcesException, IllegalArgumentException {
        Parcel metaParcel;
        this.mSurfaceRelease = false;
        this.isSurfaceControlUsed = false;
        this.mCloseGuard = CloseGuard.get();
        this.mLock = new Object();
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        this.mName = name;
        this.mWidth = w;
        this.mHeight = h;
        this.mLocalOwnerView = localOwnerView;
        Parcel metaParcel2 = Parcel.obtain();
        if (metadata != null) {
            try {
                if (metadata.size() > 0) {
                    metaParcel2.writeInt(metadata.size());
                    for (int i = 0; i < metadata.size(); i++) {
                        metaParcel2.writeInt(metadata.keyAt(i));
                        metaParcel2.writeByteArray(ByteBuffer.allocate(4).order(ByteOrder.nativeOrder()).putInt(metadata.valueAt(i)).array());
                    }
                    metaParcel2.setDataPosition(0);
                }
            } catch (Throwable th) {
                th = th;
                metaParcel = metaParcel2;
                metaParcel.recycle();
                throw th;
            }
        }
        metaParcel = metaParcel2;
        try {
            this.mNativeObject = nativeCreate(session, name, w, h, format, flags, parent != null ? parent.mNativeObject : 0L, metaParcel);
            metaParcel.recycle();
            long j = this.mNativeObject;
            if (j != 0) {
                this.mNativeHandle = nativeGetHandle(j);
                this.mCloseGuard.openWithCallSite("release", callsite);
                return;
            }
            Log.d(TAG, "surface " + this.mName + " create failed!");
            throw new Surface.OutOfResourcesException("Couldn't allocate SurfaceControl native object");
        } catch (Throwable th2) {
            th = th2;
            metaParcel.recycle();
            throw th;
        }
    }

    public SurfaceControl(SurfaceControl other, String callsite) {
        this.mSurfaceRelease = false;
        this.isSurfaceControlUsed = false;
        this.mCloseGuard = CloseGuard.get();
        this.mLock = new Object();
        copyFrom(other, callsite);
    }

    private SurfaceControl(Parcel in) {
        this.mSurfaceRelease = false;
        this.isSurfaceControlUsed = false;
        this.mCloseGuard = CloseGuard.get();
        this.mLock = new Object();
        readFromParcel(in);
    }

    public SurfaceControl() {
        this.mSurfaceRelease = false;
        this.isSurfaceControlUsed = false;
        this.mCloseGuard = CloseGuard.get();
        this.mLock = new Object();
    }

    public void readFromParcel(Parcel in) {
        if (in == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        this.mName = in.readString8();
        this.mWidth = in.readInt();
        this.mHeight = in.readInt();
        long object = 0;
        if (in.readInt() != 0) {
            object = nativeReadFromParcel(in);
        }
        assignNativeObject(object, "readFromParcel");
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString8(this.mName);
        dest.writeInt(this.mWidth);
        dest.writeInt(this.mHeight);
        if (this.mNativeObject == 0) {
            dest.writeInt(0);
        } else {
            dest.writeInt(1);
        }
        nativeWriteToParcel(this.mNativeObject, dest);
        if ((flags & 1) != 0) {
            release();
        }
    }

    public boolean isSameSurface(SurfaceControl other) {
        return other.mNativeHandle == this.mNativeHandle;
    }

    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1120986464257L, System.identityHashCode(this));
        proto.write(1138166333442L, this.mName);
        proto.write(1120986464259L, getLayerId());
        proto.end(token);
    }

    protected void finalize() throws Throwable {
        if (LOG_SURFACE_CONTROL) {
            Log.d(TAG, "finalize, mNativeObject:" + this.mNativeObject, new Throwable());
        }
        try {
            CloseGuard closeGuard = this.mCloseGuard;
            if (closeGuard != null) {
                closeGuard.warnIfOpen();
            }
            long j = this.mNativeObject;
            if (j != 0) {
                nativeRelease(j);
                this.mNativeObject = 0L;
                this.mNativeHandle = 0L;
                this.mCloseGuard.close();
            }
        } finally {
            super.finalize();
        }
    }

    public void release() {
        if (LOG_SURFACE_CONTROL) {
            Log.d(TAG, "release, mNativeObject:" + this.mNativeObject, new Throwable());
        }
        if (this.mNativeObject != 0) {
            boolean z = TRAN_USER_ROOT_SUPPORT;
            if (z) {
                this.mSurfaceRelease = true;
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                RuntimeException runtimeException = new RuntimeException(this.mName + " is release " + format.format(new Date()));
                this.re = runtimeException;
                runtimeException.fillInStackTrace();
                if (this.isSurfaceControlUsed) {
                    Log.d(TAG, "SurfaceControlUsed destory by ", this.re);
                }
            }
            nativeRelease(this.mNativeObject);
            this.mNativeObject = 0L;
            this.mNativeHandle = 0L;
            this.mCloseGuard.close();
            if (z) {
                this.mSurfaceRelease = false;
            }
        }
    }

    public void disconnect() {
        if (LOG_SURFACE_CONTROL) {
            Log.d(TAG, "disconnect, mNativeObject:" + this.mNativeObject);
        }
        long j = this.mNativeObject;
        if (j != 0) {
            nativeDisconnect(j);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkNotReleased() {
        if (this.mNativeObject == 0) {
            if (TRAN_USER_ROOT_SUPPORT) {
                Log.d(TAG, "destory by checkNotReleased ", this.re);
            }
            throw new NullPointerException("Invalid " + this + ", mNativeObject is null. Have you called release() already?");
        } else if (TRAN_USER_ROOT_SUPPORT && this.mSurfaceRelease) {
            Log.d(TAG, "The surface has been destroyed by ", this.re);
            Log.d(TAG, "This is dangerous and needs to be checked " + this.mNativeObject, new Throwable());
        }
    }

    public boolean isValid() {
        return this.mNativeObject != 0;
    }

    public static void openTransaction() {
        synchronized (SurfaceControl.class) {
            if (sGlobalTransaction == null) {
                sGlobalTransaction = new GlobalTransactionWrapper();
            }
            synchronized (SurfaceControl.class) {
                sTransactionNestCount++;
            }
        }
    }

    @Deprecated
    public static void mergeToGlobalTransaction(Transaction t) {
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.merge(t);
        }
    }

    public static void closeTransaction() {
        synchronized (SurfaceControl.class) {
            long j = sTransactionNestCount;
            if (j == 0) {
                Log.e(TAG, "Call to SurfaceControl.closeTransaction without matching openTransaction");
            } else {
                long j2 = j - 1;
                sTransactionNestCount = j2;
                if (j2 > 0) {
                    return;
                }
            }
            sGlobalTransaction.applyGlobalTransaction(false);
        }
    }

    public boolean clearContentFrameStats() {
        checkNotReleased();
        return nativeClearContentFrameStats(this.mNativeObject);
    }

    public boolean getContentFrameStats(WindowContentFrameStats outStats) {
        checkNotReleased();
        return nativeGetContentFrameStats(this.mNativeObject, outStats);
    }

    public static boolean clearAnimationFrameStats() {
        return nativeClearAnimationFrameStats();
    }

    public static boolean getAnimationFrameStats(WindowAnimationFrameStats outStats) {
        return nativeGetAnimationFrameStats(outStats);
    }

    public int getWidth() {
        int i;
        synchronized (this.mLock) {
            i = this.mWidth;
        }
        return i;
    }

    public int getHeight() {
        int i;
        synchronized (this.mLock) {
            i = this.mHeight;
        }
        return i;
    }

    public View getLocalOwnerView() {
        WeakReference<View> weakReference = this.mLocalOwnerView;
        if (weakReference != null) {
            return weakReference.get();
        }
        return null;
    }

    public String toString() {
        return "Surface(name=" + this.mName + ")/@0x" + Integer.toHexString(System.identityHashCode(this));
    }

    /* loaded from: classes3.dex */
    public static final class StaticDisplayInfo {
        public float density;
        public DeviceProductInfo deviceProductInfo;
        public int installOrientation;
        public boolean isInternal;
        public boolean secure;

        public String toString() {
            return "StaticDisplayInfo{isInternal=" + this.isInternal + ", density=" + this.density + ", secure=" + this.secure + ", deviceProductInfo=" + this.deviceProductInfo + ", installOrientation=" + this.installOrientation + "}";
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            StaticDisplayInfo that = (StaticDisplayInfo) o;
            if (this.isInternal == that.isInternal && this.density == that.density && this.secure == that.secure && Objects.equals(this.deviceProductInfo, that.deviceProductInfo) && this.installOrientation == that.installOrientation) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(Boolean.valueOf(this.isInternal), Float.valueOf(this.density), Boolean.valueOf(this.secure), this.deviceProductInfo, Integer.valueOf(this.installOrientation));
        }
    }

    /* loaded from: classes3.dex */
    public static final class DynamicDisplayInfo {
        public int activeColorMode;
        public int activeDisplayModeId;
        public boolean autoLowLatencyModeSupported;
        public boolean gameContentTypeSupported;
        public Display.HdrCapabilities hdrCapabilities;
        public int preferredBootDisplayMode;
        public int[] supportedColorModes;
        public DisplayMode[] supportedDisplayModes;

        public String toString() {
            return "DynamicDisplayInfo{supportedDisplayModes=" + Arrays.toString(this.supportedDisplayModes) + ", activeDisplayModeId=" + this.activeDisplayModeId + ", supportedColorModes=" + Arrays.toString(this.supportedColorModes) + ", activeColorMode=" + this.activeColorMode + ", hdrCapabilities=" + this.hdrCapabilities + ", autoLowLatencyModeSupported=" + this.autoLowLatencyModeSupported + ", gameContentTypeSupported" + this.gameContentTypeSupported + ", preferredBootDisplayMode" + this.preferredBootDisplayMode + "}";
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DynamicDisplayInfo that = (DynamicDisplayInfo) o;
            if (Arrays.equals(this.supportedDisplayModes, that.supportedDisplayModes) && this.activeDisplayModeId == that.activeDisplayModeId && Arrays.equals(this.supportedColorModes, that.supportedColorModes) && this.activeColorMode == that.activeColorMode && Objects.equals(this.hdrCapabilities, that.hdrCapabilities) && this.preferredBootDisplayMode == that.preferredBootDisplayMode) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(this.supportedDisplayModes, Integer.valueOf(this.activeDisplayModeId), Integer.valueOf(this.activeDisplayModeId), Integer.valueOf(this.activeColorMode), this.hdrCapabilities);
        }
    }

    /* loaded from: classes3.dex */
    public static final class DisplayMode {
        public long appVsyncOffsetNanos;
        public int group;
        public int height;
        public int id;
        public long presentationDeadlineNanos;
        public float refreshRate;
        public int width;
        public float xDpi;
        public float yDpi;

        public String toString() {
            return "DisplayMode{id=" + this.id + ", width=" + this.width + ", height=" + this.height + ", xDpi=" + this.xDpi + ", yDpi=" + this.yDpi + ", refreshRate=" + this.refreshRate + ", appVsyncOffsetNanos=" + this.appVsyncOffsetNanos + ", presentationDeadlineNanos=" + this.presentationDeadlineNanos + ", group=" + this.group + "}";
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DisplayMode that = (DisplayMode) o;
            if (this.id == that.id && this.width == that.width && this.height == that.height && Float.compare(that.xDpi, this.xDpi) == 0 && Float.compare(that.yDpi, this.yDpi) == 0 && Float.compare(that.refreshRate, this.refreshRate) == 0 && this.appVsyncOffsetNanos == that.appVsyncOffsetNanos && this.presentationDeadlineNanos == that.presentationDeadlineNanos && this.group == that.group) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(Integer.valueOf(this.id), Integer.valueOf(this.width), Integer.valueOf(this.height), Float.valueOf(this.xDpi), Float.valueOf(this.yDpi), Float.valueOf(this.refreshRate), Long.valueOf(this.appVsyncOffsetNanos), Long.valueOf(this.presentationDeadlineNanos), Integer.valueOf(this.group));
        }
    }

    public static void setDisplayPowerMode(IBinder displayToken, int mode) {
        if (displayToken == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        }
        nativeSetDisplayPowerMode(displayToken, mode);
    }

    public static StaticDisplayInfo getStaticDisplayInfo(IBinder displayToken) {
        if (displayToken == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        }
        return nativeGetStaticDisplayInfo(displayToken);
    }

    public static DynamicDisplayInfo getDynamicDisplayInfo(IBinder displayToken) {
        if (displayToken == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        }
        return nativeGetDynamicDisplayInfo(displayToken);
    }

    public static DisplayedContentSamplingAttributes getDisplayedContentSamplingAttributes(IBinder displayToken) {
        if (displayToken == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        }
        return nativeGetDisplayedContentSamplingAttributes(displayToken);
    }

    public static boolean setDisplayedContentSamplingEnabled(IBinder displayToken, boolean enable, int componentMask, int maxFrames) {
        if (displayToken == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        }
        if ((componentMask >> 4) != 0) {
            throw new IllegalArgumentException("invalid componentMask when enabling sampling");
        }
        return nativeSetDisplayedContentSamplingEnabled(displayToken, enable, componentMask, maxFrames);
    }

    public static DisplayedContentSample getDisplayedContentSample(IBinder displayToken, long maxFrames, long timestamp) {
        if (displayToken == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        }
        return nativeGetDisplayedContentSample(displayToken, maxFrames, timestamp);
    }

    /* loaded from: classes3.dex */
    public static final class DesiredDisplayModeSpecs {
        public boolean allowGroupSwitching;
        public float appRequestRefreshRateMax;
        public float appRequestRefreshRateMin;
        public int defaultMode;
        public float primaryRefreshRateMax;
        public float primaryRefreshRateMin;

        public DesiredDisplayModeSpecs() {
        }

        public DesiredDisplayModeSpecs(DesiredDisplayModeSpecs other) {
            copyFrom(other);
        }

        public DesiredDisplayModeSpecs(int defaultMode, boolean allowGroupSwitching, float primaryRefreshRateMin, float primaryRefreshRateMax, float appRequestRefreshRateMin, float appRequestRefreshRateMax) {
            this.defaultMode = defaultMode;
            this.allowGroupSwitching = allowGroupSwitching;
            this.primaryRefreshRateMin = primaryRefreshRateMin;
            this.primaryRefreshRateMax = primaryRefreshRateMax;
            this.appRequestRefreshRateMin = appRequestRefreshRateMin;
            this.appRequestRefreshRateMax = appRequestRefreshRateMax;
        }

        public boolean equals(Object o) {
            return (o instanceof DesiredDisplayModeSpecs) && equals((DesiredDisplayModeSpecs) o);
        }

        public boolean equals(DesiredDisplayModeSpecs other) {
            return other != null && this.defaultMode == other.defaultMode && this.primaryRefreshRateMin == other.primaryRefreshRateMin && this.primaryRefreshRateMax == other.primaryRefreshRateMax && this.appRequestRefreshRateMin == other.appRequestRefreshRateMin && this.appRequestRefreshRateMax == other.appRequestRefreshRateMax;
        }

        public int hashCode() {
            return 0;
        }

        public void copyFrom(DesiredDisplayModeSpecs other) {
            this.defaultMode = other.defaultMode;
            this.primaryRefreshRateMin = other.primaryRefreshRateMin;
            this.primaryRefreshRateMax = other.primaryRefreshRateMax;
            this.appRequestRefreshRateMin = other.appRequestRefreshRateMin;
            this.appRequestRefreshRateMax = other.appRequestRefreshRateMax;
        }

        public String toString() {
            return String.format("defaultConfig=%d primaryRefreshRateRange=[%.0f %.0f] appRequestRefreshRateRange=[%.0f %.0f]", Integer.valueOf(this.defaultMode), Float.valueOf(this.primaryRefreshRateMin), Float.valueOf(this.primaryRefreshRateMax), Float.valueOf(this.appRequestRefreshRateMin), Float.valueOf(this.appRequestRefreshRateMax));
        }
    }

    public static boolean setDesiredDisplayModeSpecs(IBinder displayToken, DesiredDisplayModeSpecs desiredDisplayModeSpecs) {
        if (displayToken == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        }
        if (desiredDisplayModeSpecs == null) {
            throw new IllegalArgumentException("desiredDisplayModeSpecs must not be null");
        }
        if (desiredDisplayModeSpecs.defaultMode < 0) {
            throw new IllegalArgumentException("defaultMode must be non-negative");
        }
        return nativeSetDesiredDisplayModeSpecs(displayToken, desiredDisplayModeSpecs);
    }

    public static DesiredDisplayModeSpecs getDesiredDisplayModeSpecs(IBinder displayToken) {
        if (displayToken == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        }
        return nativeGetDesiredDisplayModeSpecs(displayToken);
    }

    public static DisplayPrimaries getDisplayNativePrimaries(IBinder displayToken) {
        if (displayToken == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        }
        return nativeGetDisplayNativePrimaries(displayToken);
    }

    public static boolean setActiveColorMode(IBinder displayToken, int colorMode) {
        if (displayToken == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        }
        return nativeSetActiveColorMode(displayToken, colorMode);
    }

    public static ColorSpace[] getCompositionColorSpaces() {
        int[] dataspaces = nativeGetCompositionDataspaces();
        ColorSpace srgb = ColorSpace.get(ColorSpace.Named.SRGB);
        ColorSpace[] colorSpaces = new ColorSpace[2];
        colorSpaces[0] = srgb;
        colorSpaces[1] = srgb;
        if (dataspaces.length == 2) {
            for (int i = 0; i < 2; i++) {
                switch (dataspaces[i]) {
                    case 143261696:
                        colorSpaces[i] = ColorSpace.get(ColorSpace.Named.DISPLAY_P3);
                        break;
                    case 411107328:
                        colorSpaces[i] = ColorSpace.get(ColorSpace.Named.EXTENDED_SRGB);
                        break;
                }
            }
        }
        return colorSpaces;
    }

    public static boolean getBootDisplayModeSupport() {
        return nativeGetBootDisplayModeSupport();
    }

    public static void setBootDisplayMode(IBinder displayToken, int displayModeId) {
        if (displayToken == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        }
        nativeSetBootDisplayMode(displayToken, displayModeId);
    }

    public static void clearBootDisplayMode(IBinder displayToken) {
        if (displayToken == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        }
        nativeClearBootDisplayMode(displayToken);
    }

    public static void setAutoLowLatencyMode(IBinder displayToken, boolean on) {
        if (displayToken == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        }
        nativeSetAutoLowLatencyMode(displayToken, on);
    }

    public static void setGameContentType(IBinder displayToken, boolean on) {
        if (displayToken == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        }
        nativeSetGameContentType(displayToken, on);
    }

    public static void setDisplayProjection(IBinder displayToken, int orientation, Rect layerStackRect, Rect displayRect) {
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.setDisplayProjection(displayToken, orientation, layerStackRect, displayRect);
        }
    }

    public static void setDisplayLayerStack(IBinder displayToken, int layerStack) {
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.setDisplayLayerStack(displayToken, layerStack);
        }
    }

    public static void setDisplaySurface(IBinder displayToken, Surface surface) {
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.setDisplaySurface(displayToken, surface);
        }
    }

    public static void setDisplaySize(IBinder displayToken, int width, int height) {
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.setDisplaySize(displayToken, width, height);
        }
    }

    public static void overrideHdrTypes(IBinder displayToken, int[] modes) {
        nativeOverrideHdrTypes(displayToken, modes);
    }

    public static IBinder createDisplay(String name, boolean secure) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        return nativeCreateDisplay(name, secure);
    }

    public static void destroyDisplay(IBinder displayToken) {
        if (displayToken == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        }
        nativeDestroyDisplay(displayToken);
    }

    public static long[] getPhysicalDisplayIds() {
        return nativeGetPhysicalDisplayIds();
    }

    public static long getPrimaryPhysicalDisplayId() {
        return nativeGetPrimaryPhysicalDisplayId();
    }

    public static IBinder getPhysicalDisplayToken(long physicalDisplayId) {
        return nativeGetPhysicalDisplayToken(physicalDisplayId);
    }

    public static IBinder getInternalDisplayToken() {
        long[] physicalDisplayIds = getPhysicalDisplayIds();
        if (physicalDisplayIds.length == 0) {
            return null;
        }
        return getPhysicalDisplayToken(physicalDisplayIds[0]);
    }

    public static int captureDisplay(DisplayCaptureArgs captureArgs, ScreenCaptureListener captureListener) {
        return nativeCaptureDisplay(captureArgs, captureListener);
    }

    public static ScreenshotHardwareBuffer captureDisplay(DisplayCaptureArgs captureArgs) {
        SyncScreenCaptureListener screenCaptureListener = new SyncScreenCaptureListener();
        int status = captureDisplay(captureArgs, screenCaptureListener);
        if (status != 0) {
            return null;
        }
        return screenCaptureListener.waitForScreenshot();
    }

    public static ScreenshotHardwareBuffer captureLayers(SurfaceControl layer, Rect sourceCrop, float frameScale) {
        return captureLayers(layer, sourceCrop, frameScale, 1);
    }

    public static ScreenshotHardwareBuffer captureLayers(SurfaceControl layer, Rect sourceCrop, float frameScale, int format) {
        LayerCaptureArgs captureArgs = ((LayerCaptureArgs.Builder) ((LayerCaptureArgs.Builder) ((LayerCaptureArgs.Builder) new LayerCaptureArgs.Builder(layer).setSourceCrop(sourceCrop)).setFrameScale(frameScale)).setPixelFormat(format)).build();
        return captureLayers(captureArgs);
    }

    public static ScreenshotHardwareBuffer captureLayers(LayerCaptureArgs captureArgs) {
        SyncScreenCaptureListener screenCaptureListener = new SyncScreenCaptureListener();
        int status = captureLayers(captureArgs, screenCaptureListener);
        if (status != 0) {
            return null;
        }
        return screenCaptureListener.waitForScreenshot();
    }

    public static ScreenshotHardwareBuffer captureLayersExcluding(SurfaceControl layer, Rect sourceCrop, float frameScale, int format, SurfaceControl[] exclude) {
        LayerCaptureArgs captureArgs = ((LayerCaptureArgs.Builder) ((LayerCaptureArgs.Builder) ((LayerCaptureArgs.Builder) new LayerCaptureArgs.Builder(layer).setSourceCrop(sourceCrop)).setFrameScale(frameScale)).setPixelFormat(format)).setExcludeLayers(exclude).build();
        return captureLayers(captureArgs);
    }

    public static int captureLayers(LayerCaptureArgs captureArgs, ScreenCaptureListener captureListener) {
        return nativeCaptureLayers(captureArgs, captureListener);
    }

    public static boolean getProtectedContentSupport() {
        return nativeGetProtectedContentSupport();
    }

    public static boolean getDisplayBrightnessSupport(IBinder displayToken) {
        return nativeGetDisplayBrightnessSupport(displayToken);
    }

    public static boolean setDisplayBrightness(IBinder displayToken, float brightness) {
        return setDisplayBrightness(displayToken, brightness, -1.0f, brightness, -1.0f);
    }

    public static boolean setDisplayBrightness(IBinder displayToken, float sdrBrightness, float sdrBrightnessNits, float displayBrightness, float displayBrightnessNits) {
        Objects.requireNonNull(displayToken);
        if (Float.isNaN(displayBrightness) || displayBrightness > 1.0f || (displayBrightness < 0.0f && displayBrightness != -1.0f)) {
            throw new IllegalArgumentException("displayBrightness must be a number between 0.0f  and 1.0f, or -1 to turn the backlight off: " + displayBrightness);
        }
        if (Float.isNaN(sdrBrightness) || sdrBrightness > 1.0f || (sdrBrightness < 0.0f && sdrBrightness != -1.0f)) {
            throw new IllegalArgumentException("sdrBrightness must be a number between 0.0f and 1.0f, or -1 to turn the backlight off: " + sdrBrightness);
        }
        return nativeSetDisplayBrightness(displayToken, sdrBrightness, sdrBrightnessNits, displayBrightness, displayBrightnessNits);
    }

    public static SurfaceControl mirrorSurface(SurfaceControl mirrorOf) {
        long nativeObj = nativeMirrorSurface(mirrorOf.mNativeObject);
        SurfaceControl sc = new SurfaceControl();
        sc.assignNativeObject(nativeObj, "mirrorSurface");
        return sc;
    }

    private static void validateColorArg(float[] color) {
        if (color.length != 4) {
            throw new IllegalArgumentException("Color must be specified as a float array with four values to represent r, g, b, a in range [0..1]");
        }
        for (float c : color) {
            if (c < 0.0f || c > 1.0f) {
                throw new IllegalArgumentException("Color must be specified as a float array with four values to represent r, g, b, a in range [0..1]");
            }
        }
    }

    public static void setGlobalShadowSettings(float[] ambientColor, float[] spotColor, float lightPosY, float lightPosZ, float lightRadius) {
        validateColorArg(ambientColor);
        validateColorArg(spotColor);
        nativeSetGlobalShadowSettings(ambientColor, spotColor, lightPosY, lightPosZ, lightRadius);
    }

    public static DisplayDecorationSupport getDisplayDecorationSupport(IBinder displayToken) {
        return nativeGetDisplayDecorationSupport(displayToken);
    }

    public static void addJankDataListener(OnJankDataListener listener, SurfaceControl surface) {
        nativeAddJankDataListener(listener.mNativePtr.get(), surface.mNativeObject);
    }

    public static void removeJankDataListener(OnJankDataListener listener) {
        nativeRemoveJankDataListener(listener.mNativePtr.get());
    }

    public static int getGPUContextPriority() {
        return nativeGetGPUContextPriority();
    }

    /* loaded from: classes3.dex */
    public static class Transaction implements Closeable, Parcelable {
        public boolean isTransactionUsed;
        Runnable mFreeNativeResources;
        public long mNativeObject;
        private final ArrayMap<SurfaceControl, SurfaceControl> mReparentedSurfaces;
        private final ArrayMap<SurfaceControl, Point> mResizedSurfaces;
        RuntimeException reTransactionClear;
        RuntimeException reTransactionClose;
        public static final NativeAllocationRegistry sRegistry = new NativeAllocationRegistry(Transaction.class.getClassLoader(), SurfaceControl.m4889$$Nest$smnativeGetNativeTransactionFinalizer(), 512);
        private static final float[] INVALID_COLOR = {-1.0f, -1.0f, -1.0f};
        public static final Parcelable.Creator<Transaction> CREATOR = new Parcelable.Creator<Transaction>() { // from class: android.view.SurfaceControl.Transaction.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public Transaction createFromParcel(Parcel in) {
                return new Transaction(in);
            }

            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public Transaction[] newArray(int size) {
                return new Transaction[size];
            }
        };

        protected void checkPreconditions(SurfaceControl sc) {
            sc.checkNotReleased();
        }

        public Transaction() {
            this(SurfaceControl.m4888$$Nest$smnativeCreateTransaction());
        }

        private Transaction(long nativeObject) {
            this.isTransactionUsed = false;
            this.mResizedSurfaces = new ArrayMap<>();
            this.mReparentedSurfaces = new ArrayMap<>();
            this.mNativeObject = nativeObject;
            this.mFreeNativeResources = sRegistry.registerNativeAllocation(this, nativeObject);
        }

        private Transaction(Parcel in) {
            this.isTransactionUsed = false;
            this.mResizedSurfaces = new ArrayMap<>();
            this.mReparentedSurfaces = new ArrayMap<>();
            readFromParcel(in);
        }

        public void apply() {
            apply(false);
        }

        public void clear() {
            if (SurfaceControl.TRAN_USER_ROOT_SUPPORT) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                RuntimeException runtimeException = new RuntimeException("Transaction release here " + format.format(new Date()));
                this.reTransactionClear = runtimeException;
                runtimeException.fillInStackTrace();
                if (this.isTransactionUsed) {
                    Log.d(SurfaceControl.TAG, "TransactionClear by ", this.reTransactionClear);
                }
            }
            this.mResizedSurfaces.clear();
            this.mReparentedSurfaces.clear();
            long j = this.mNativeObject;
            if (j != 0) {
                SurfaceControl.nativeClearTransaction(j);
            }
        }

        @Override // java.io.Closeable, java.lang.AutoCloseable
        public void close() {
            if (SurfaceControl.TRAN_USER_ROOT_SUPPORT) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                RuntimeException runtimeException = new RuntimeException("Transaction release here " + format.format(new Date()));
                this.reTransactionClose = runtimeException;
                runtimeException.fillInStackTrace();
                if (this.isTransactionUsed) {
                    Log.d(SurfaceControl.TAG, "TransactionClose by ", this.reTransactionClose);
                }
            }
            this.mResizedSurfaces.clear();
            this.mReparentedSurfaces.clear();
            this.mFreeNativeResources.run();
            this.mNativeObject = 0L;
        }

        public void apply(boolean sync) {
            applyResizedSurfaces();
            notifyReparentedSurfaces();
            if (SurfaceControl.TRAN_USER_ROOT_SUPPORT) {
                this.isTransactionUsed = true;
            }
            SurfaceControl.nativeApplyTransaction(this.mNativeObject, sync);
            if (SurfaceControl.TRAN_USER_ROOT_SUPPORT) {
                this.isTransactionUsed = false;
            }
        }

        protected void applyResizedSurfaces() {
            for (int i = this.mResizedSurfaces.size() - 1; i >= 0; i--) {
                Point size = this.mResizedSurfaces.valueAt(i);
                SurfaceControl surfaceControl = this.mResizedSurfaces.keyAt(i);
                synchronized (surfaceControl.mLock) {
                    surfaceControl.resize(size.x, size.y);
                }
            }
            this.mResizedSurfaces.clear();
        }

        protected void notifyReparentedSurfaces() {
            int reparentCount = this.mReparentedSurfaces.size();
            for (int i = reparentCount - 1; i >= 0; i--) {
                SurfaceControl child = this.mReparentedSurfaces.keyAt(i);
                synchronized (child.mLock) {
                    int listenerCount = child.mReparentListeners != null ? child.mReparentListeners.size() : 0;
                    for (int j = 0; j < listenerCount; j++) {
                        OnReparentListener listener = (OnReparentListener) child.mReparentListeners.get(j);
                        listener.onReparent(this, this.mReparentedSurfaces.valueAt(i));
                    }
                    this.mReparentedSurfaces.removeAt(i);
                }
            }
        }

        public Transaction setVisibility(SurfaceControl sc, boolean visible) {
            checkPreconditions(sc);
            if (visible) {
                return show(sc);
            }
            return hide(sc);
        }

        public Transaction setFrameRateSelectionPriority(SurfaceControl sc, int priority) {
            sc.checkNotReleased();
            SurfaceControl.nativeSetFrameRateSelectionPriority(this.mNativeObject, sc.mNativeObject, priority);
            return this;
        }

        public Transaction show(SurfaceControl sc) {
            checkPreconditions(sc);
            if (SurfaceControl.TRAN_USER_ROOT_SUPPORT) {
                sc.isSurfaceControlUsed = true;
            }
            SurfaceControl.nativeSetFlags(this.mNativeObject, sc.mNativeObject, 0, 1);
            if (SurfaceControl.TRAN_USER_ROOT_SUPPORT) {
                sc.isSurfaceControlUsed = false;
            }
            return this;
        }

        public Transaction hide(SurfaceControl sc) {
            checkPreconditions(sc);
            if (SurfaceControl.TRAN_USER_ROOT_SUPPORT) {
                sc.isSurfaceControlUsed = true;
            }
            SurfaceControl.nativeSetFlags(this.mNativeObject, sc.mNativeObject, 1, 1);
            if (SurfaceControl.TRAN_USER_ROOT_SUPPORT) {
                sc.isSurfaceControlUsed = false;
            }
            return this;
        }

        public Transaction setPosition(SurfaceControl sc, float x, float y) {
            checkPreconditions(sc);
            if (SurfaceControl.TRAN_USER_ROOT_SUPPORT) {
                sc.isSurfaceControlUsed = true;
            }
            if (x > 1.07374182E9f || y >= 1.07374182E9f) {
                Log.d(SurfaceControl.TAG, "app setPosition is too large, x=" + x + ",y=" + y + Log.getStackTraceString(new Throwable()));
            }
            SurfaceControl.nativeSetPosition(this.mNativeObject, sc.mNativeObject, x, y);
            if (SurfaceControl.TRAN_USER_ROOT_SUPPORT) {
                sc.isSurfaceControlUsed = false;
            }
            return this;
        }

        public Transaction setScale(SurfaceControl sc, float scaleX, float scaleY) {
            checkPreconditions(sc);
            Preconditions.checkArgument(scaleX >= 0.0f, "Negative value passed in for scaleX");
            Preconditions.checkArgument(scaleY >= 0.0f, "Negative value passed in for scaleY");
            SurfaceControl.nativeSetScale(this.mNativeObject, sc.mNativeObject, scaleX, scaleY);
            return this;
        }

        public Transaction setBufferSize(SurfaceControl sc, int w, int h) {
            checkPreconditions(sc);
            this.mResizedSurfaces.put(sc, new Point(w, h));
            SurfaceControl.nativeSetSize(this.mNativeObject, sc.mNativeObject, w, h);
            return this;
        }

        public Transaction setFixedTransformHint(SurfaceControl sc, int transformHint) {
            checkPreconditions(sc);
            SurfaceControl.nativeSetFixedTransformHint(this.mNativeObject, sc.mNativeObject, transformHint);
            return this;
        }

        public Transaction unsetFixedTransformHint(SurfaceControl sc) {
            checkPreconditions(sc);
            SurfaceControl.nativeSetFixedTransformHint(this.mNativeObject, sc.mNativeObject, -1);
            return this;
        }

        public Transaction setLayer(SurfaceControl sc, int z) {
            checkPreconditions(sc);
            SurfaceControl.nativeSetLayer(this.mNativeObject, sc.mNativeObject, z);
            return this;
        }

        public Transaction setRelativeLayer(SurfaceControl sc, SurfaceControl relativeTo, int z) {
            checkPreconditions(sc);
            SurfaceControl.nativeSetRelativeLayer(this.mNativeObject, sc.mNativeObject, relativeTo.mNativeObject, z);
            return this;
        }

        public Transaction setTransparentRegionHint(SurfaceControl sc, Region transparentRegion) {
            checkPreconditions(sc);
            SurfaceControl.nativeSetTransparentRegionHint(this.mNativeObject, sc.mNativeObject, transparentRegion);
            return this;
        }

        public Transaction setAlpha(SurfaceControl sc, float alpha) {
            checkPreconditions(sc);
            if (SurfaceControl.TRAN_USER_ROOT_SUPPORT) {
                sc.isSurfaceControlUsed = true;
            }
            SurfaceControl.nativeSetAlpha(this.mNativeObject, sc.mNativeObject, alpha);
            if (SurfaceControl.TRAN_USER_ROOT_SUPPORT) {
                sc.isSurfaceControlUsed = false;
            }
            return this;
        }

        public Transaction setInputWindowInfo(SurfaceControl sc, InputWindowHandle handle) {
            checkPreconditions(sc);
            if (SurfaceControl.TRAN_USER_ROOT_SUPPORT && (handle.touchableRegion.getBounds().width() >= 1073741823 || handle.touchableRegion.getBounds().height() >= 1073741823)) {
                Log.d(SurfaceControl.TAG, "Bound size is too large width =" + handle.touchableRegion.getBounds().width() + " height =" + handle.touchableRegion.getBounds().height());
            }
            SurfaceControl.nativeSetInputWindowInfo(this.mNativeObject, sc.mNativeObject, handle);
            return this;
        }

        public Transaction syncInputWindows() {
            SurfaceControl.nativeSyncInputWindows(this.mNativeObject);
            return this;
        }

        public Transaction setGeometry(SurfaceControl sc, Rect sourceCrop, Rect destFrame, int orientation) {
            checkPreconditions(sc);
            SurfaceControl.nativeSetGeometry(this.mNativeObject, sc.mNativeObject, sourceCrop, destFrame, orientation);
            return this;
        }

        public Transaction setMatrix(SurfaceControl sc, float dsdx, float dtdx, float dtdy, float dsdy) {
            checkPreconditions(sc);
            SurfaceControl.nativeSetMatrix(this.mNativeObject, sc.mNativeObject, dsdx, dtdx, dtdy, dsdy);
            return this;
        }

        public Transaction setMatrix(SurfaceControl sc, Matrix matrix, float[] float9) {
            matrix.getValues(float9);
            setMatrix(sc, float9[0], float9[3], float9[1], float9[4]);
            setPosition(sc, float9[2], float9[5]);
            return this;
        }

        public Transaction setColorTransform(SurfaceControl sc, float[] matrix, float[] translation) {
            checkPreconditions(sc);
            SurfaceControl.nativeSetColorTransform(this.mNativeObject, sc.mNativeObject, matrix, translation);
            return this;
        }

        public Transaction setColorSpaceAgnostic(SurfaceControl sc, boolean agnostic) {
            checkPreconditions(sc);
            SurfaceControl.nativeSetColorSpaceAgnostic(this.mNativeObject, sc.mNativeObject, agnostic);
            return this;
        }

        @Deprecated
        public Transaction setWindowCrop(SurfaceControl sc, Rect crop) {
            checkPreconditions(sc);
            if (crop != null) {
                SurfaceControl.nativeSetWindowCrop(this.mNativeObject, sc.mNativeObject, crop.left, crop.top, crop.right, crop.bottom);
            } else {
                SurfaceControl.nativeSetWindowCrop(this.mNativeObject, sc.mNativeObject, 0, 0, 0, 0);
            }
            return this;
        }

        public Transaction setCrop(SurfaceControl sc, Rect crop) {
            checkPreconditions(sc);
            if (crop != null) {
                Preconditions.checkArgument(crop.isValid(), "Crop isn't valid.");
                SurfaceControl.nativeSetWindowCrop(this.mNativeObject, sc.mNativeObject, crop.left, crop.top, crop.right, crop.bottom);
            } else {
                SurfaceControl.nativeSetWindowCrop(this.mNativeObject, sc.mNativeObject, 0, 0, 0, 0);
            }
            return this;
        }

        public Transaction setWindowCrop(SurfaceControl sc, int width, int height) {
            checkPreconditions(sc);
            SurfaceControl.nativeSetWindowCrop(this.mNativeObject, sc.mNativeObject, 0, 0, width, height);
            return this;
        }

        public Transaction setCornerRadius(SurfaceControl sc, float cornerRadius) {
            checkPreconditions(sc);
            SurfaceControl.nativeSetCornerRadius(this.mNativeObject, sc.mNativeObject, cornerRadius);
            return this;
        }

        public Transaction setBackgroundBlurRadius(SurfaceControl sc, int radius) {
            checkPreconditions(sc);
            SurfaceControl.nativeSetBackgroundBlurRadius(this.mNativeObject, sc.mNativeObject, radius);
            return this;
        }

        public Transaction setBlurRegions(SurfaceControl sc, float[][] regions) {
            checkPreconditions(sc);
            SurfaceControl.nativeSetBlurRegions(this.mNativeObject, sc.mNativeObject, regions, regions.length);
            return this;
        }

        public Transaction setSaturation(SurfaceControl sc, float saturation, int blurRadius) {
            checkPreconditions(sc);
            SurfaceControl.nativeSetSaturation(this.mNativeObject, sc.mNativeObject, saturation, blurRadius);
            return this;
        }

        public Transaction setStretchEffect(SurfaceControl sc, float width, float height, float vecX, float vecY, float maxStretchAmountX, float maxStretchAmountY, float childRelativeLeft, float childRelativeTop, float childRelativeRight, float childRelativeBottom) {
            checkPreconditions(sc);
            SurfaceControl.nativeSetStretchEffect(this.mNativeObject, sc.mNativeObject, width, height, vecX, vecY, maxStretchAmountX, maxStretchAmountY, childRelativeLeft, childRelativeTop, childRelativeRight, childRelativeBottom);
            return this;
        }

        public Transaction setLayerStack(SurfaceControl sc, int layerStack) {
            checkPreconditions(sc);
            SurfaceControl.nativeSetLayerStack(this.mNativeObject, sc.mNativeObject, layerStack);
            return this;
        }

        public Transaction reparent(SurfaceControl sc, SurfaceControl newParent) {
            checkPreconditions(sc);
            if (SurfaceControl.TRAN_USER_ROOT_SUPPORT) {
                sc.isSurfaceControlUsed = true;
            }
            long otherObject = 0;
            if (newParent != null) {
                newParent.checkNotReleased();
                otherObject = newParent.mNativeObject;
            }
            SurfaceControl.nativeReparent(this.mNativeObject, sc.mNativeObject, otherObject);
            if (SurfaceControl.TRAN_USER_ROOT_SUPPORT) {
                sc.isSurfaceControlUsed = false;
            }
            this.mReparentedSurfaces.put(sc, newParent);
            return this;
        }

        public Transaction setColor(SurfaceControl sc, float[] color) {
            checkPreconditions(sc);
            SurfaceControl.nativeSetColor(this.mNativeObject, sc.mNativeObject, color);
            return this;
        }

        public Transaction unsetColor(SurfaceControl sc) {
            checkPreconditions(sc);
            SurfaceControl.nativeSetColor(this.mNativeObject, sc.mNativeObject, INVALID_COLOR);
            return this;
        }

        public Transaction setSecure(SurfaceControl sc, boolean isSecure) {
            checkPreconditions(sc);
            if (isSecure) {
                SurfaceControl.nativeSetFlags(this.mNativeObject, sc.mNativeObject, 128, 128);
            } else {
                SurfaceControl.nativeSetFlags(this.mNativeObject, sc.mNativeObject, 0, 128);
            }
            return this;
        }

        public Transaction setDisplayDecoration(SurfaceControl sc, boolean displayDecoration) {
            checkPreconditions(sc);
            if (displayDecoration) {
                SurfaceControl.nativeSetFlags(this.mNativeObject, sc.mNativeObject, 512, 512);
            } else {
                SurfaceControl.nativeSetFlags(this.mNativeObject, sc.mNativeObject, 0, 512);
            }
            return this;
        }

        public Transaction setOpaque(SurfaceControl sc, boolean isOpaque) {
            checkPreconditions(sc);
            if (isOpaque) {
                SurfaceControl.nativeSetFlags(this.mNativeObject, sc.mNativeObject, 2, 2);
            } else {
                SurfaceControl.nativeSetFlags(this.mNativeObject, sc.mNativeObject, 0, 2);
            }
            return this;
        }

        public Transaction setDisplaySurface(IBinder displayToken, Surface surface) {
            if (displayToken == null) {
                throw new IllegalArgumentException("displayToken must not be null");
            }
            if (surface != null) {
                synchronized (surface.mLock) {
                    SurfaceControl.nativeSetDisplaySurface(this.mNativeObject, displayToken, surface.mNativeObject);
                }
            } else {
                SurfaceControl.nativeSetDisplaySurface(this.mNativeObject, displayToken, 0L);
            }
            return this;
        }

        public Transaction setDisplayLayerStack(IBinder displayToken, int layerStack) {
            if (displayToken == null) {
                throw new IllegalArgumentException("displayToken must not be null");
            }
            SurfaceControl.nativeSetDisplayLayerStack(this.mNativeObject, displayToken, layerStack);
            return this;
        }

        public Transaction setDisplayFlags(IBinder displayToken, int flags) {
            if (displayToken == null) {
                throw new IllegalArgumentException("displayToken must not be null");
            }
            SurfaceControl.nativeSetDisplayFlags(this.mNativeObject, displayToken, flags);
            return this;
        }

        public Transaction setDisplayProjection(IBinder displayToken, int orientation, Rect layerStackRect, Rect displayRect) {
            if (displayToken == null) {
                throw new IllegalArgumentException("displayToken must not be null");
            }
            if (layerStackRect == null) {
                throw new IllegalArgumentException("layerStackRect must not be null");
            }
            if (displayRect == null) {
                throw new IllegalArgumentException("displayRect must not be null");
            }
            SurfaceControl.nativeSetDisplayProjection(this.mNativeObject, displayToken, orientation, layerStackRect.left, layerStackRect.top, layerStackRect.right, layerStackRect.bottom, displayRect.left, displayRect.top, displayRect.right, displayRect.bottom);
            return this;
        }

        public Transaction setDisplaySize(IBinder displayToken, int width, int height) {
            if (displayToken == null) {
                throw new IllegalArgumentException("displayToken must not be null");
            }
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("width and height must be positive");
            }
            SurfaceControl.nativeSetDisplaySize(this.mNativeObject, displayToken, width, height);
            return this;
        }

        public Transaction setAnimationTransaction() {
            SurfaceControl.nativeSetAnimationTransaction(this.mNativeObject);
            return this;
        }

        public Transaction setEarlyWakeupStart() {
            SurfaceControl.nativeSetEarlyWakeupStart(this.mNativeObject);
            return this;
        }

        public Transaction setEarlyWakeupEnd() {
            SurfaceControl.nativeSetEarlyWakeupEnd(this.mNativeObject);
            return this;
        }

        public Transaction setMetadata(SurfaceControl sc, int key, int data) {
            Parcel parcel = Parcel.obtain();
            parcel.writeInt(data);
            try {
                setMetadata(sc, key, parcel);
                return this;
            } finally {
                parcel.recycle();
            }
        }

        public Transaction setMetadata(SurfaceControl sc, int key, Parcel data) {
            checkPreconditions(sc);
            if (SurfaceControl.TRAN_USER_ROOT_SUPPORT) {
                sc.isSurfaceControlUsed = true;
            }
            SurfaceControl.nativeSetMetadata(this.mNativeObject, sc.mNativeObject, key, data);
            if (SurfaceControl.TRAN_USER_ROOT_SUPPORT) {
                sc.isSurfaceControlUsed = false;
            }
            return this;
        }

        public Transaction setShadowRadius(SurfaceControl sc, float shadowRadius) {
            checkPreconditions(sc);
            SurfaceControl.nativeSetShadowRadius(this.mNativeObject, sc.mNativeObject, shadowRadius);
            return this;
        }

        public Transaction setFrameRate(SurfaceControl sc, float frameRate, int compatibility) {
            return setFrameRate(sc, frameRate, compatibility, 0);
        }

        public Transaction setFrameRate(SurfaceControl sc, float frameRate, int compatibility, int changeFrameRateStrategy) {
            checkPreconditions(sc);
            SurfaceControl.nativeSetFrameRate(this.mNativeObject, sc.mNativeObject, frameRate, compatibility, changeFrameRateStrategy);
            return this;
        }

        public Transaction setFocusedWindow(IBinder token, String windowName, int displayId) {
            SurfaceControl.nativeSetFocusedWindow(this.mNativeObject, token, windowName, null, null, displayId);
            return this;
        }

        public Transaction requestFocusTransfer(IBinder token, String windowName, IBinder focusedToken, String focusedWindowName, int displayId) {
            SurfaceControl.nativeSetFocusedWindow(this.mNativeObject, token, windowName, focusedToken, focusedWindowName, displayId);
            return this;
        }

        public Transaction removeCurrentInputFocus(int displayId) {
            SurfaceControl.nativeRemoveCurrentInputFocus(this.mNativeObject, displayId);
            return this;
        }

        public Transaction setSkipScreenshot(SurfaceControl sc, boolean skipScrenshot) {
            checkPreconditions(sc);
            if (skipScrenshot) {
                SurfaceControl.nativeSetFlags(this.mNativeObject, sc.mNativeObject, 64, 64);
            } else {
                SurfaceControl.nativeSetFlags(this.mNativeObject, sc.mNativeObject, 0, 64);
            }
            return this;
        }

        @Deprecated
        public Transaction setBuffer(SurfaceControl sc, GraphicBuffer buffer) {
            return setBuffer(sc, HardwareBuffer.createFromGraphicBuffer(buffer));
        }

        public Transaction setBuffer(SurfaceControl sc, HardwareBuffer buffer) {
            return setBuffer(sc, buffer, null);
        }

        public Transaction setBuffer(SurfaceControl sc, HardwareBuffer buffer, SyncFence fence) {
            return setBuffer(sc, buffer, fence, null);
        }

        public Transaction setBuffer(SurfaceControl sc, HardwareBuffer buffer, SyncFence fence, Consumer<SyncFence> releaseCallback) {
            checkPreconditions(sc);
            if (fence != null) {
                synchronized (fence.getLock()) {
                    SurfaceControl.nativeSetBuffer(this.mNativeObject, sc.mNativeObject, buffer, fence.getNativeFence(), releaseCallback);
                }
            } else {
                SurfaceControl.nativeSetBuffer(this.mNativeObject, sc.mNativeObject, buffer, 0L, releaseCallback);
            }
            return this;
        }

        public Transaction setBufferTransform(SurfaceControl sc, int transform) {
            checkPreconditions(sc);
            SurfaceControl.nativeSetBufferTransform(this.mNativeObject, sc.mNativeObject, transform);
            return this;
        }

        public Transaction setDamageRegion(SurfaceControl sc, Region region) {
            SurfaceControl.nativeSetDamageRegion(this.mNativeObject, sc.mNativeObject, region);
            return this;
        }

        public Transaction setDimmingEnabled(SurfaceControl sc, boolean dimmingEnabled) {
            checkPreconditions(sc);
            SurfaceControl.nativeSetDimmingEnabled(this.mNativeObject, sc.mNativeObject, dimmingEnabled);
            return this;
        }

        @Deprecated
        public Transaction setColorSpace(SurfaceControl sc, ColorSpace colorSpace) {
            checkPreconditions(sc);
            if (colorSpace.getId() == ColorSpace.Named.DISPLAY_P3.ordinal()) {
                setDataSpace(sc, 143261696);
            } else {
                setDataSpace(sc, 142671872);
            }
            return this;
        }

        public Transaction setDataSpace(SurfaceControl sc, int dataspace) {
            checkPreconditions(sc);
            SurfaceControl.nativeSetDataSpace(this.mNativeObject, sc.mNativeObject, dataspace);
            return this;
        }

        public Transaction setTrustedOverlay(SurfaceControl sc, boolean isTrustedOverlay) {
            checkPreconditions(sc);
            SurfaceControl.nativeSetTrustedOverlay(this.mNativeObject, sc.mNativeObject, isTrustedOverlay);
            return this;
        }

        public Transaction setDropInputMode(SurfaceControl sc, int mode) {
            checkPreconditions(sc);
            if (SurfaceControl.TRAN_USER_ROOT_SUPPORT) {
                sc.isSurfaceControlUsed = true;
            }
            SurfaceControl.nativeSetDropInputMode(this.mNativeObject, sc.mNativeObject, mode);
            if (SurfaceControl.TRAN_USER_ROOT_SUPPORT) {
                sc.isSurfaceControlUsed = false;
            }
            return this;
        }

        public void sanitize() {
            SurfaceControl.nativeSanitize(this.mNativeObject);
        }

        public Transaction setDesintationFrame(SurfaceControl sc, Rect destinationFrame) {
            checkPreconditions(sc);
            SurfaceControl.nativeSetDestinationFrame(this.mNativeObject, sc.mNativeObject, destinationFrame.left, destinationFrame.top, destinationFrame.right, destinationFrame.bottom);
            return this;
        }

        public Transaction setDesintationFrame(SurfaceControl sc, int width, int height) {
            checkPreconditions(sc);
            SurfaceControl.nativeSetDestinationFrame(this.mNativeObject, sc.mNativeObject, 0, 0, width, height);
            return this;
        }

        public Transaction setMultiWindowLayer(SurfaceControl sc, boolean isInMultiWindowLayer) {
            checkPreconditions(sc);
            if (isInMultiWindowLayer) {
                SurfaceControl.nativeSetFlags(this.mNativeObject, sc.mNativeObject, 268435456, 268435456);
            } else {
                SurfaceControl.nativeSetFlags(this.mNativeObject, sc.mNativeObject, 0, 268435456);
            }
            return this;
        }

        public Transaction merge(Transaction other) {
            if (this == other) {
                return this;
            }
            if (SurfaceControl.TRAN_USER_ROOT_SUPPORT) {
                this.isTransactionUsed = true;
            }
            this.mResizedSurfaces.putAll((ArrayMap<? extends SurfaceControl, ? extends Point>) other.mResizedSurfaces);
            other.mResizedSurfaces.clear();
            this.mReparentedSurfaces.putAll((ArrayMap<? extends SurfaceControl, ? extends SurfaceControl>) other.mReparentedSurfaces);
            other.mReparentedSurfaces.clear();
            SurfaceControl.nativeMergeTransaction(this.mNativeObject, other.mNativeObject);
            if (SurfaceControl.TRAN_USER_ROOT_SUPPORT) {
                this.isTransactionUsed = false;
            }
            return this;
        }

        public Transaction remove(SurfaceControl sc) {
            reparent(sc, null);
            sc.release();
            return this;
        }

        public Transaction setFrameTimelineVsync(long frameTimelineVsyncId) {
            SurfaceControl.nativeSetFrameTimelineVsync(this.mNativeObject, frameTimelineVsyncId);
            return this;
        }

        public Transaction addTransactionCommittedListener(final Executor executor, final TransactionCommittedListener listener) {
            TransactionCommittedListener listenerInner = new TransactionCommittedListener() { // from class: android.view.SurfaceControl$Transaction$$ExternalSyntheticLambda0
                @Override // android.view.SurfaceControl.TransactionCommittedListener
                public final void onTransactionCommitted() {
                    SurfaceControl.Transaction.lambda$addTransactionCommittedListener$0(executor, listener);
                }
            };
            SurfaceControl.nativeAddTransactionCommittedListener(this.mNativeObject, listenerInner);
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$addTransactionCommittedListener$0(Executor executor, final TransactionCommittedListener listener) {
            Objects.requireNonNull(listener);
            executor.execute(new Runnable() { // from class: android.view.SurfaceControl$Transaction$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    SurfaceControl.TransactionCommittedListener.this.onTransactionCommitted();
                }
            });
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            if (this.mNativeObject == 0) {
                dest.writeInt(0);
                return;
            }
            dest.writeInt(1);
            SurfaceControl.nativeWriteTransactionToParcel(this.mNativeObject, dest);
            if ((flags & 1) != 0) {
                SurfaceControl.nativeClearTransaction(this.mNativeObject);
            }
        }

        private void readFromParcel(Parcel in) {
            this.mNativeObject = 0L;
            if (in.readInt() != 0) {
                long nativeReadTransactionFromParcel = SurfaceControl.nativeReadTransactionFromParcel(in);
                this.mNativeObject = nativeReadTransactionFromParcel;
                this.mFreeNativeResources = sRegistry.registerNativeAllocation(this, nativeReadTransactionFromParcel);
            }
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        public Transaction setSkipParentIntersect(SurfaceControl sc, boolean skipParentIntersect) {
            checkPreconditions(sc);
            if (skipParentIntersect) {
                SurfaceControl.nativeSetFlags(this.mNativeObject, sc.mNativeObject, 536870912, 536870912);
            } else {
                SurfaceControl.nativeSetFlags(this.mNativeObject, sc.mNativeObject, 0, 536870912);
            }
            return this;
        }
    }

    /* loaded from: classes3.dex */
    public static class LockDebuggingTransaction extends Transaction {
        Object mMonitor;

        public LockDebuggingTransaction(Object o) {
            this.mMonitor = o;
        }

        @Override // android.view.SurfaceControl.Transaction
        protected void checkPreconditions(SurfaceControl sc) {
            super.checkPreconditions(sc);
            if (!Thread.holdsLock(this.mMonitor)) {
                throw new RuntimeException("Unlocked access to synchronized SurfaceControl.Transaction");
            }
        }
    }

    /* loaded from: classes3.dex */
    private static class GlobalTransactionWrapper extends Transaction {
        private GlobalTransactionWrapper() {
        }

        void applyGlobalTransaction(boolean sync) {
            applyResizedSurfaces();
            notifyReparentedSurfaces();
            SurfaceControl.nativeApplyTransaction(this.mNativeObject, sync);
        }

        @Override // android.view.SurfaceControl.Transaction
        public void apply(boolean sync) {
            throw new RuntimeException("Global transaction must be applied from closeTransaction");
        }
    }

    public float getSurfaceStatsFps() {
        return nativeGetSurfaceStatsFps(this.mNativeObject);
    }

    public static Transaction getGlobalTransaction() {
        return sGlobalTransaction;
    }

    public void resize(int w, int h) {
        this.mWidth = w;
        this.mHeight = h;
        nativeUpdateDefaultBufferSize(this.mNativeObject, w, h);
    }

    public int getTransformHint() {
        checkNotReleased();
        return nativeGetTransformHint(this.mNativeObject);
    }

    public void setTransformHint(int transformHint) {
        nativeSetTransformHint(this.mNativeObject, transformHint);
    }

    public int getLayerId() {
        long j = this.mNativeObject;
        if (j != 0) {
            return nativeGetLayerId(j);
        }
        return -1;
    }

    private static void invokeReleaseCallback(Consumer<SyncFence> callback, long nativeFencePtr) {
        SyncFence fence = new SyncFence(nativeFencePtr);
        callback.accept(fence);
    }
}
