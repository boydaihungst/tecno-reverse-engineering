package com.android.server.wm;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.IBinder;
import android.os.InputConstants;
import android.os.Process;
import android.util.Slog;
import android.view.GestureDetector;
import android.view.InputApplicationHandle;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.InputWindowHandle;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import com.android.server.UiThread;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public class Letterbox {
    private static final Rect EMPTY_RECT = new Rect();
    private static final Point ZERO_POINT = new Point(0, 0);
    private final Supplier<Boolean> mAreCornersRounded;
    private final Supplier<Integer> mBlurRadiusSupplier;
    private final LetterboxSurface mBottom;
    private final Supplier<Color> mColorSupplier;
    private final Supplier<Float> mDarkScrimAlphaSupplier;
    private final IntConsumer mDoubleTapCallback;
    private final LetterboxSurface mFullWindowSurface;
    private final Supplier<Boolean> mHasWallpaperBackgroundSupplier;
    private final LetterboxSurface mLeft;
    private final LetterboxSurface mRight;
    private final Supplier<SurfaceControl.Builder> mSurfaceControlFactory;
    private final LetterboxSurface[] mSurfaces;
    private final LetterboxSurface mTop;
    private final Supplier<SurfaceControl.Transaction> mTransactionFactory;
    private final Rect mOuter = new Rect();
    private final Rect mInner = new Rect();

    public Letterbox(Supplier<SurfaceControl.Builder> surfaceControlFactory, Supplier<SurfaceControl.Transaction> transactionFactory, Supplier<Boolean> areCornersRounded, Supplier<Color> colorSupplier, Supplier<Boolean> hasWallpaperBackgroundSupplier, Supplier<Integer> blurRadiusSupplier, Supplier<Float> darkScrimAlphaSupplier, IntConsumer doubleTapCallback) {
        LetterboxSurface letterboxSurface = new LetterboxSurface("top");
        this.mTop = letterboxSurface;
        LetterboxSurface letterboxSurface2 = new LetterboxSurface("left");
        this.mLeft = letterboxSurface2;
        LetterboxSurface letterboxSurface3 = new LetterboxSurface("bottom");
        this.mBottom = letterboxSurface3;
        LetterboxSurface letterboxSurface4 = new LetterboxSurface("right");
        this.mRight = letterboxSurface4;
        this.mFullWindowSurface = new LetterboxSurface("fullWindow");
        this.mSurfaces = new LetterboxSurface[]{letterboxSurface2, letterboxSurface, letterboxSurface4, letterboxSurface3};
        this.mSurfaceControlFactory = surfaceControlFactory;
        this.mTransactionFactory = transactionFactory;
        this.mAreCornersRounded = areCornersRounded;
        this.mColorSupplier = colorSupplier;
        this.mHasWallpaperBackgroundSupplier = hasWallpaperBackgroundSupplier;
        this.mBlurRadiusSupplier = blurRadiusSupplier;
        this.mDarkScrimAlphaSupplier = darkScrimAlphaSupplier;
        this.mDoubleTapCallback = doubleTapCallback;
    }

    public void layout(Rect outer, Rect inner, Point surfaceOrigin) {
        this.mOuter.set(outer);
        this.mInner.set(inner);
        this.mTop.layout(outer.left, outer.top, outer.right, inner.top, surfaceOrigin);
        this.mLeft.layout(outer.left, outer.top, inner.left, outer.bottom, surfaceOrigin);
        this.mBottom.layout(outer.left, inner.bottom, outer.right, outer.bottom, surfaceOrigin);
        this.mRight.layout(inner.right, outer.top, outer.right, outer.bottom, surfaceOrigin);
        this.mFullWindowSurface.layout(outer.left, outer.top, outer.right, outer.bottom, surfaceOrigin);
    }

    public Rect getInsets() {
        return new Rect(this.mLeft.getWidth(), this.mTop.getHeight(), this.mRight.getWidth(), this.mBottom.getHeight());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Rect getInnerFrame() {
        return this.mInner;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean notIntersectsOrFullyContains(Rect rect) {
        LetterboxSurface[] letterboxSurfaceArr;
        int emptyCount = 0;
        int noOverlappingCount = 0;
        for (LetterboxSurface surface : this.mSurfaces) {
            Rect surfaceRect = surface.mLayoutFrameGlobal;
            if (surfaceRect.isEmpty()) {
                emptyCount++;
            } else if (!Rect.intersects(surfaceRect, rect)) {
                noOverlappingCount++;
            } else if (surfaceRect.contains(rect)) {
                return true;
            }
        }
        return emptyCount + noOverlappingCount == this.mSurfaces.length;
    }

    public void hide() {
        Rect rect = EMPTY_RECT;
        layout(rect, rect, ZERO_POINT);
    }

    public void destroy() {
        LetterboxSurface[] letterboxSurfaceArr;
        this.mOuter.setEmpty();
        this.mInner.setEmpty();
        for (LetterboxSurface surface : this.mSurfaces) {
            surface.remove();
        }
        this.mFullWindowSurface.remove();
    }

    public boolean needsApplySurfaceChanges() {
        LetterboxSurface[] letterboxSurfaceArr;
        if (useFullWindowSurface()) {
            return this.mFullWindowSurface.needsApplySurfaceChanges();
        }
        for (LetterboxSurface surface : this.mSurfaces) {
            if (surface.needsApplySurfaceChanges()) {
                return true;
            }
        }
        return false;
    }

    public void applySurfaceChanges(SurfaceControl.Transaction t) {
        int i = 0;
        if (useFullWindowSurface()) {
            this.mFullWindowSurface.applySurfaceChanges(t);
            LetterboxSurface[] letterboxSurfaceArr = this.mSurfaces;
            int length = letterboxSurfaceArr.length;
            while (i < length) {
                LetterboxSurface surface = letterboxSurfaceArr[i];
                surface.remove();
                i++;
            }
            return;
        }
        LetterboxSurface[] letterboxSurfaceArr2 = this.mSurfaces;
        int length2 = letterboxSurfaceArr2.length;
        while (i < length2) {
            LetterboxSurface surface2 = letterboxSurfaceArr2[i];
            surface2.applySurfaceChanges(t);
            i++;
        }
        this.mFullWindowSurface.remove();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void attachInput(WindowState win) {
        LetterboxSurface[] letterboxSurfaceArr;
        if (useFullWindowSurface()) {
            this.mFullWindowSurface.attachInput(win);
            return;
        }
        for (LetterboxSurface surface : this.mSurfaces) {
            surface.attachInput(win);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onMovedToDisplay(int displayId) {
        LetterboxSurface[] letterboxSurfaceArr;
        for (LetterboxSurface surface : this.mSurfaces) {
            if (surface.mInputInterceptor != null) {
                surface.mInputInterceptor.mWindowHandle.displayId = displayId;
            }
        }
        if (this.mFullWindowSurface.mInputInterceptor != null) {
            this.mFullWindowSurface.mInputInterceptor.mWindowHandle.displayId = displayId;
        }
    }

    private boolean useFullWindowSurface() {
        return this.mAreCornersRounded.get().booleanValue() || this.mHasWallpaperBackgroundSupplier.get().booleanValue();
    }

    /* loaded from: classes2.dex */
    private final class TapEventReceiver extends InputEventReceiver {
        private final GestureDetector mDoubleTapDetector;
        private final DoubleTapListener mDoubleTapListener;

        TapEventReceiver(InputChannel inputChannel, Context context) {
            super(inputChannel, UiThread.getHandler().getLooper());
            DoubleTapListener doubleTapListener = new DoubleTapListener();
            this.mDoubleTapListener = doubleTapListener;
            this.mDoubleTapDetector = new GestureDetector(context, doubleTapListener, UiThread.getHandler());
        }

        public void onInputEvent(InputEvent event) {
            MotionEvent motionEvent = (MotionEvent) event;
            finishInputEvent(event, this.mDoubleTapDetector.onTouchEvent(motionEvent));
        }
    }

    /* loaded from: classes2.dex */
    private class DoubleTapListener extends GestureDetector.SimpleOnGestureListener {
        private DoubleTapListener() {
        }

        @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnDoubleTapListener
        public boolean onDoubleTapEvent(MotionEvent e) {
            if (e.getAction() == 1) {
                Letterbox.this.mDoubleTapCallback.accept((int) e.getX());
                return true;
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class InputInterceptor {
        private final InputChannel mClientChannel;
        private final InputEventReceiver mInputEventReceiver;
        private final IBinder mToken;
        private final InputWindowHandle mWindowHandle;
        private final WindowManagerService mWmService;

        InputInterceptor(String namePrefix, WindowState win) {
            WindowManagerService windowManagerService = win.mWmService;
            this.mWmService = windowManagerService;
            String name = namePrefix + (win.mActivityRecord != null ? win.mActivityRecord : win);
            InputChannel createInputChannel = windowManagerService.mInputManager.createInputChannel(name);
            this.mClientChannel = createInputChannel;
            this.mInputEventReceiver = new TapEventReceiver(createInputChannel, windowManagerService.mContext);
            IBinder token = createInputChannel.getToken();
            this.mToken = token;
            InputWindowHandle inputWindowHandle = new InputWindowHandle((InputApplicationHandle) null, win.getDisplayId());
            this.mWindowHandle = inputWindowHandle;
            inputWindowHandle.name = name;
            inputWindowHandle.token = token;
            inputWindowHandle.layoutParamsType = 2022;
            inputWindowHandle.dispatchingTimeoutMillis = InputConstants.DEFAULT_DISPATCHING_TIMEOUT_MILLIS;
            inputWindowHandle.ownerPid = Process.myPid();
            inputWindowHandle.ownerUid = Process.myUid();
            inputWindowHandle.scaleFactor = 1.0f;
            inputWindowHandle.inputConfig = UsbTerminalTypes.TERMINAL_BIDIR_SKRPHONE_SUPRESS;
            if (!Build.IS_USER) {
                Slog.d("LetterBox", "new input channel for " + name);
            }
        }

        void updateTouchableRegion(Rect frame) {
            if (frame.isEmpty()) {
                this.mWindowHandle.token = null;
                return;
            }
            this.mWindowHandle.token = this.mToken;
            this.mWindowHandle.touchableRegion.set(frame);
            this.mWindowHandle.touchableRegion.translate(-frame.left, -frame.top);
        }

        void dispose() {
            this.mWmService.mInputManager.removeInputChannel(this.mToken);
            this.mInputEventReceiver.dispose();
            this.mClientChannel.dispose();
            if (!Build.IS_USER) {
                Slog.d("LetterBox", "dispose input channel for " + this.mWindowHandle.name);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class LetterboxSurface {
        private Color mColor;
        private boolean mHasWallpaperBackground;
        private InputInterceptor mInputInterceptor;
        private SurfaceControl mSurface;
        private final String mType;
        private final Rect mSurfaceFrameRelative = new Rect();
        private final Rect mLayoutFrameGlobal = new Rect();
        private final Rect mLayoutFrameRelative = new Rect();

        public LetterboxSurface(String type) {
            this.mType = type;
        }

        public void layout(int left, int top, int right, int bottom, Point surfaceOrigin) {
            this.mLayoutFrameGlobal.set(left, top, right, bottom);
            this.mLayoutFrameRelative.set(this.mLayoutFrameGlobal);
            this.mLayoutFrameRelative.offset(-surfaceOrigin.x, -surfaceOrigin.y);
        }

        private void createSurface(SurfaceControl.Transaction t) {
            SurfaceControl build = ((SurfaceControl.Builder) Letterbox.this.mSurfaceControlFactory.get()).setName("Letterbox - " + this.mType).setFlags(4).setColorLayer().setCallsite("LetterboxSurface.createSurface").build();
            this.mSurface = build;
            t.setLayer(build, -1).setColorSpaceAgnostic(this.mSurface, true);
        }

        void attachInput(WindowState win) {
            InputInterceptor inputInterceptor = this.mInputInterceptor;
            if (inputInterceptor != null) {
                inputInterceptor.dispose();
            }
            this.mInputInterceptor = new InputInterceptor("Letterbox_" + this.mType + "_", win);
        }

        boolean isRemoved() {
            return (this.mSurface == null && this.mInputInterceptor == null) ? false : true;
        }

        public void remove() {
            if (this.mSurface != null) {
                ((SurfaceControl.Transaction) Letterbox.this.mTransactionFactory.get()).remove(this.mSurface).apply();
                this.mSurface = null;
            }
            InputInterceptor inputInterceptor = this.mInputInterceptor;
            if (inputInterceptor != null) {
                inputInterceptor.dispose();
                this.mInputInterceptor = null;
            }
        }

        public int getWidth() {
            return Math.max(0, this.mLayoutFrameGlobal.width());
        }

        public int getHeight() {
            return Math.max(0, this.mLayoutFrameGlobal.height());
        }

        public void applySurfaceChanges(SurfaceControl.Transaction t) {
            InputInterceptor inputInterceptor;
            if (!needsApplySurfaceChanges()) {
                return;
            }
            this.mSurfaceFrameRelative.set(this.mLayoutFrameRelative);
            if (!this.mSurfaceFrameRelative.isEmpty()) {
                if (this.mSurface == null) {
                    createSurface(t);
                }
                this.mColor = (Color) Letterbox.this.mColorSupplier.get();
                t.setColor(this.mSurface, getRgbColorArray());
                t.setPosition(this.mSurface, this.mSurfaceFrameRelative.left, this.mSurfaceFrameRelative.top);
                t.setWindowCrop(this.mSurface, this.mSurfaceFrameRelative.width(), this.mSurfaceFrameRelative.height());
                this.mHasWallpaperBackground = ((Boolean) Letterbox.this.mHasWallpaperBackgroundSupplier.get()).booleanValue();
                updateAlphaAndBlur(t);
                t.show(this.mSurface);
            } else {
                SurfaceControl surfaceControl = this.mSurface;
                if (surfaceControl != null) {
                    t.hide(surfaceControl);
                }
            }
            if (this.mSurface != null && (inputInterceptor = this.mInputInterceptor) != null) {
                inputInterceptor.updateTouchableRegion(this.mSurfaceFrameRelative);
                t.setInputWindowInfo(this.mSurface, this.mInputInterceptor.mWindowHandle);
            }
        }

        private void updateAlphaAndBlur(SurfaceControl.Transaction t) {
            if (!this.mHasWallpaperBackground) {
                t.setAlpha(this.mSurface, 1.0f);
                t.setBackgroundBlurRadius(this.mSurface, 0);
                return;
            }
            float alpha = ((Float) Letterbox.this.mDarkScrimAlphaSupplier.get()).floatValue();
            t.setAlpha(this.mSurface, alpha);
            if (((Integer) Letterbox.this.mBlurRadiusSupplier.get()).intValue() <= 0) {
                t.setBackgroundBlurRadius(this.mSurface, 0);
            } else {
                t.setBackgroundBlurRadius(this.mSurface, ((Integer) Letterbox.this.mBlurRadiusSupplier.get()).intValue());
            }
        }

        private float[] getRgbColorArray() {
            float[] rgbTmpFloat = {this.mColor.red(), this.mColor.green(), this.mColor.blue()};
            return rgbTmpFloat;
        }

        public boolean needsApplySurfaceChanges() {
            return (this.mSurfaceFrameRelative.equals(this.mLayoutFrameRelative) && (this.mSurfaceFrameRelative.isEmpty() || (((Boolean) Letterbox.this.mHasWallpaperBackgroundSupplier.get()).booleanValue() == this.mHasWallpaperBackground && ((Color) Letterbox.this.mColorSupplier.get()).equals(this.mColor)))) ? false : true;
        }
    }
}
