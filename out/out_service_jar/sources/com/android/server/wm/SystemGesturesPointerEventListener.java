package com.android.server.wm;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.display.DisplayManagerGlobal;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Slog;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.DisplayInfo;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.WindowManagerPolicyConstants;
import android.widget.OverScroller;
import com.android.server.usb.descriptors.UsbACInterface;
import com.transsion.hubcore.server.wm.ITranWindowManagerService;
import java.io.PrintWriter;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class SystemGesturesPointerEventListener implements WindowManagerPolicyConstants.PointerEventListener {
    private static final boolean DEBUG = false;
    private static final int MAX_FLING_TIME_MILLIS = 5000;
    private static final int MAX_TRACKED_POINTERS = 32;
    private static final int SWIPE_FROM_BOTTOM = 2;
    private static final int SWIPE_FROM_LEFT = 4;
    private static final int SWIPE_FROM_RIGHT = 3;
    private static final int SWIPE_FROM_TOP = 1;
    private static final int SWIPE_NONE = 0;
    private static final long SWIPE_TIMEOUT_MS = 500;
    private static final String TAG = "SystemGestures";
    private static final int UNTRACKED_POINTER = -1;
    private final Callbacks mCallbacks;
    private final Context mContext;
    private boolean mDebugFireable;
    private int mDisplayCutoutTouchableRegionSize;
    private int mDownPointers;
    private GestureDetector mGestureDetector;
    private final Handler mHandler;
    private long mLastFlingTime;
    private boolean mMouseHoveringAtEdge;
    private int mSwipeDistanceThreshold;
    private boolean mSwipeFireable;
    int screenHeight;
    int screenWidth;
    private final Rect mSwipeStartThreshold = new Rect();
    private final int[] mDownPointerId = new int[32];
    private final float[] mDownX = new float[32];
    private final float[] mDownY = new float[32];
    private final long[] mDownTime = new long[32];

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface Callbacks {
        void onDebug();

        void onDown();

        void onFling(int i);

        void onMouseHoverAtBottom();

        void onMouseHoverAtTop();

        void onMouseLeaveFromEdge();

        void onSwipeFromBottom();

        void onSwipeFromLeft();

        void onSwipeFromRight();

        void onSwipeFromTop();

        void onUpOrCancel();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SystemGesturesPointerEventListener(Context context, Handler handler, Callbacks callbacks) {
        this.mContext = (Context) checkNull("context", context);
        this.mHandler = handler;
        this.mCallbacks = (Callbacks) checkNull("callbacks", callbacks);
        onConfigurationChanged();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onDisplayInfoChanged(DisplayInfo info) {
        this.screenWidth = info.logicalWidth;
        this.screenHeight = info.logicalHeight;
        onConfigurationChanged();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onConfigurationChanged() {
        Resources r = this.mContext.getResources();
        int defaultThreshold = r.getDimensionPixelSize(17105572);
        this.mSwipeStartThreshold.set(defaultThreshold, defaultThreshold, defaultThreshold, defaultThreshold);
        this.mSwipeDistanceThreshold = defaultThreshold;
        Display display = DisplayManagerGlobal.getInstance().getRealDisplay(0);
        DisplayCutout displayCutout = display.getCutout();
        if (displayCutout != null) {
            this.mDisplayCutoutTouchableRegionSize = r.getDimensionPixelSize(17105202);
            Rect[] bounds = displayCutout.getBoundingRectsAll();
            if (bounds[0] != null) {
                Rect rect = this.mSwipeStartThreshold;
                rect.left = Math.max(rect.left, bounds[0].width() + this.mDisplayCutoutTouchableRegionSize);
            }
            if (bounds[1] != null) {
                Rect rect2 = this.mSwipeStartThreshold;
                rect2.top = Math.max(rect2.top, bounds[1].height() + this.mDisplayCutoutTouchableRegionSize);
            }
            if (bounds[2] != null) {
                Rect rect3 = this.mSwipeStartThreshold;
                rect3.right = Math.max(rect3.right, bounds[2].width() + this.mDisplayCutoutTouchableRegionSize);
            }
            if (bounds[3] != null) {
                Rect rect4 = this.mSwipeStartThreshold;
                rect4.bottom = Math.max(rect4.bottom, bounds[3].height() + this.mDisplayCutoutTouchableRegionSize);
            }
        }
    }

    private static <T> T checkNull(String name, T arg) {
        if (arg == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return arg;
    }

    public void systemReady() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.SystemGesturesPointerEventListener$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                SystemGesturesPointerEventListener.this.m8264x26be85a2();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$systemReady$0$com-android-server-wm-SystemGesturesPointerEventListener  reason: not valid java name */
    public /* synthetic */ void m8264x26be85a2() {
        int displayId = this.mContext.getDisplayId();
        DisplayInfo info = DisplayManagerGlobal.getInstance().getDisplayInfo(displayId);
        if (info == null) {
            Slog.w(TAG, "Cannot create GestureDetector, display removed:" + displayId);
        } else {
            this.mGestureDetector = new GestureDetector(this.mContext, new FlingGestureDetector(), this.mHandler) { // from class: com.android.server.wm.SystemGesturesPointerEventListener.1
            };
        }
    }

    public void onPointerEvent(MotionEvent event) {
        if (this.mGestureDetector != null && event.isTouchEvent()) {
            this.mGestureDetector.onTouchEvent(event);
        }
        switch (event.getActionMasked()) {
            case 0:
                this.mSwipeFireable = true;
                this.mDebugFireable = true;
                this.mDownPointers = 0;
                captureDown(event, 0);
                if (this.mMouseHoveringAtEdge) {
                    this.mMouseHoveringAtEdge = false;
                    this.mCallbacks.onMouseLeaveFromEdge();
                }
                this.mCallbacks.onDown();
                return;
            case 1:
            case 3:
                this.mSwipeFireable = false;
                this.mDebugFireable = false;
                this.mCallbacks.onUpOrCancel();
                return;
            case 2:
                if (this.mSwipeFireable) {
                    int swipe = detectSwipe(event);
                    ITranWindowManagerService.Instance().onSwipeFired(swipe);
                    this.mSwipeFireable = swipe == 0;
                    if (swipe == 1) {
                        this.mCallbacks.onSwipeFromTop();
                        return;
                    } else if (swipe == 2) {
                        this.mCallbacks.onSwipeFromBottom();
                        return;
                    } else if (swipe == 3) {
                        this.mCallbacks.onSwipeFromRight();
                        return;
                    } else if (swipe == 4) {
                        this.mCallbacks.onSwipeFromLeft();
                        return;
                    } else {
                        return;
                    }
                }
                return;
            case 4:
            case 6:
            default:
                return;
            case 5:
                captureDown(event, event.getActionIndex());
                if (this.mDebugFireable) {
                    boolean z = event.getPointerCount() < 5;
                    this.mDebugFireable = z;
                    if (!z) {
                        this.mCallbacks.onDebug();
                        return;
                    }
                    return;
                }
                return;
            case 7:
                if (event.isFromSource(UsbACInterface.FORMAT_III_IEC1937_MPEG1_Layer1)) {
                    if (!this.mMouseHoveringAtEdge && event.getY() == 0.0f) {
                        this.mCallbacks.onMouseHoverAtTop();
                        this.mMouseHoveringAtEdge = true;
                        return;
                    } else if (!this.mMouseHoveringAtEdge && event.getY() >= this.screenHeight - 1) {
                        this.mCallbacks.onMouseHoverAtBottom();
                        this.mMouseHoveringAtEdge = true;
                        return;
                    } else if (this.mMouseHoveringAtEdge && event.getY() > 0.0f && event.getY() < this.screenHeight - 1) {
                        this.mCallbacks.onMouseLeaveFromEdge();
                        this.mMouseHoveringAtEdge = false;
                        return;
                    } else {
                        return;
                    }
                }
                return;
        }
    }

    private void captureDown(MotionEvent event, int pointerIndex) {
        int pointerId = event.getPointerId(pointerIndex);
        int i = findIndex(pointerId);
        if (i != -1) {
            this.mDownX[i] = event.getX(pointerIndex);
            this.mDownY[i] = event.getY(pointerIndex);
            this.mDownTime[i] = event.getEventTime();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean currentGestureStartedInRegion(Region r) {
        return r.contains((int) this.mDownX[0], (int) this.mDownY[0]);
    }

    private int findIndex(int pointerId) {
        int i = 0;
        while (true) {
            int i2 = this.mDownPointers;
            if (i < i2) {
                if (this.mDownPointerId[i] != pointerId) {
                    i++;
                } else {
                    return i;
                }
            } else if (i2 == 32 || pointerId == -1) {
                return -1;
            } else {
                int[] iArr = this.mDownPointerId;
                int i3 = i2 + 1;
                this.mDownPointers = i3;
                iArr[i2] = pointerId;
                return i3 - 1;
            }
        }
    }

    private int detectSwipe(MotionEvent move) {
        int historySize = move.getHistorySize();
        int pointerCount = move.getPointerCount();
        for (int p = 0; p < pointerCount; p++) {
            int pointerId = move.getPointerId(p);
            int i = findIndex(pointerId);
            if (i != -1) {
                for (int h = 0; h < historySize; h++) {
                    long time = move.getHistoricalEventTime(h);
                    float x = move.getHistoricalX(p, h);
                    float y = move.getHistoricalY(p, h);
                    int swipe = detectSwipe(i, time, x, y);
                    if (swipe != 0) {
                        return swipe;
                    }
                }
                int swipe2 = detectSwipe(i, move.getEventTime(), move.getX(p), move.getY(p));
                if (swipe2 != 0) {
                    return swipe2;
                }
            }
        }
        return 0;
    }

    private int detectSwipe(int i, long time, float x, float y) {
        float fromX = this.mDownX[i];
        float fromY = this.mDownY[i];
        long elapsed = time - this.mDownTime[i];
        if (fromY <= this.mSwipeStartThreshold.top && y > this.mSwipeDistanceThreshold + fromY && elapsed < 500) {
            return 1;
        }
        if (fromY >= this.screenHeight - this.mSwipeStartThreshold.bottom && y < fromY - this.mSwipeDistanceThreshold && elapsed < 500) {
            return 2;
        }
        if (fromX >= this.screenWidth - this.mSwipeStartThreshold.right && x < fromX - this.mSwipeDistanceThreshold && elapsed < 500) {
            return 3;
        }
        if (fromX <= this.mSwipeStartThreshold.left && x > this.mSwipeDistanceThreshold + fromX && elapsed < 500) {
            return 4;
        }
        return 0;
    }

    public void dump(PrintWriter pw, String prefix) {
        String inner = prefix + "  ";
        pw.println(prefix + TAG + ":");
        pw.print(inner);
        pw.print("mDisplayCutoutTouchableRegionSize=");
        pw.println(this.mDisplayCutoutTouchableRegionSize);
        pw.print(inner);
        pw.print("mSwipeStartThreshold=");
        pw.println(this.mSwipeStartThreshold);
        pw.print(inner);
        pw.print("mSwipeDistanceThreshold=");
        pw.println(this.mSwipeDistanceThreshold);
    }

    /* loaded from: classes2.dex */
    private final class FlingGestureDetector extends GestureDetector.SimpleOnGestureListener {
        private OverScroller mOverscroller;

        FlingGestureDetector() {
            this.mOverscroller = new OverScroller(SystemGesturesPointerEventListener.this.mContext);
        }

        @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
        public boolean onSingleTapUp(MotionEvent e) {
            if (!this.mOverscroller.isFinished()) {
                this.mOverscroller.forceFinished(true);
            }
            return true;
        }

        @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
        public boolean onFling(MotionEvent down, MotionEvent up, float velocityX, float velocityY) {
            this.mOverscroller.computeScrollOffset();
            long now = SystemClock.uptimeMillis();
            if (SystemGesturesPointerEventListener.this.mLastFlingTime != 0 && now > SystemGesturesPointerEventListener.this.mLastFlingTime + 5000) {
                this.mOverscroller.forceFinished(true);
            }
            this.mOverscroller.fling(0, 0, (int) velocityX, (int) velocityY, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
            int duration = this.mOverscroller.getDuration();
            if (duration > 5000) {
                duration = 5000;
            }
            SystemGesturesPointerEventListener.this.mLastFlingTime = now;
            SystemGesturesPointerEventListener.this.mCallbacks.onFling(duration);
            return true;
        }
    }
}
