package android.view;

import android.graphics.Rect;
import android.view.HandwritingInitiator;
import android.view.inputmethod.InputMethodManager;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
/* loaded from: classes3.dex */
public class HandwritingInitiator {
    private final InputMethodManager mImm;
    private final int mTouchSlop;
    private State mState = new State();
    private final HandwritingAreaTracker mHandwritingAreasTracker = new HandwritingAreaTracker();
    public WeakReference<View> mConnectedView = null;
    private int mConnectionCount = 0;
    private final long mHandwritingTimeoutInMillis = ViewConfiguration.getLongPressTimeout();

    private void reset() {
        this.mState = new State();
    }

    public HandwritingInitiator(ViewConfiguration viewConfiguration, InputMethodManager inputMethodManager) {
        this.mTouchSlop = viewConfiguration.getScaledTouchSlop();
        this.mImm = inputMethodManager;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public void onTouchEvent(MotionEvent motionEvent) {
        int maskedAction = motionEvent.getActionMasked();
        switch (maskedAction) {
            case 0:
            case 5:
                int actionIndex = motionEvent.getActionIndex();
                int toolType = motionEvent.getToolType(actionIndex);
                if (toolType != 2 && toolType != 4) {
                    return;
                }
                this.mState.mStylusPointerId = motionEvent.getPointerId(actionIndex);
                this.mState.mStylusDownTimeInMillis = motionEvent.getEventTime();
                this.mState.mStylusDownX = motionEvent.getX(actionIndex);
                this.mState.mStylusDownY = motionEvent.getY(actionIndex);
                this.mState.mShouldInitHandwriting = true;
                this.mState.mExceedTouchSlop = false;
                return;
            case 1:
            case 3:
                break;
            case 2:
                if (!this.mState.mShouldInitHandwriting || this.mState.mExceedTouchSlop) {
                    return;
                }
                long timeElapsed = motionEvent.getEventTime() - this.mState.mStylusDownTimeInMillis;
                if (timeElapsed > this.mHandwritingTimeoutInMillis) {
                    reset();
                    return;
                }
                int pointerIndex = motionEvent.findPointerIndex(this.mState.mStylusPointerId);
                float x = motionEvent.getX(pointerIndex);
                float y = motionEvent.getY(pointerIndex);
                if (largerThanTouchSlop(x, y, this.mState.mStylusDownX, this.mState.mStylusDownY)) {
                    this.mState.mExceedTouchSlop = true;
                    View candidateView = findBestCandidateView(this.mState.mStylusDownX, this.mState.mStylusDownY);
                    if (candidateView != null) {
                        if (candidateView == getConnectedView()) {
                            startHandwriting(candidateView);
                            return;
                        } else {
                            candidateView.requestFocus();
                            return;
                        }
                    }
                    return;
                }
                return;
            case 4:
            default:
                return;
            case 6:
                int pointerId = motionEvent.getPointerId(motionEvent.getActionIndex());
                if (pointerId != this.mState.mStylusPointerId) {
                    return;
                }
                break;
        }
        reset();
    }

    private View getConnectedView() {
        WeakReference<View> weakReference = this.mConnectedView;
        if (weakReference == null) {
            return null;
        }
        return weakReference.get();
    }

    private void clearConnectedView() {
        this.mConnectedView = null;
        this.mConnectionCount = 0;
    }

    public void onInputConnectionCreated(View view) {
        if (!view.isAutoHandwritingEnabled()) {
            clearConnectedView();
            return;
        }
        View connectedView = getConnectedView();
        if (connectedView == view) {
            this.mConnectionCount++;
            return;
        }
        this.mConnectedView = new WeakReference<>(view);
        this.mConnectionCount = 1;
        if (this.mState.mShouldInitHandwriting) {
            tryStartHandwriting();
        }
    }

    public void onInputConnectionClosed(View view) {
        View connectedView = getConnectedView();
        if (connectedView == null) {
            return;
        }
        if (connectedView == view) {
            int i = this.mConnectionCount - 1;
            this.mConnectionCount = i;
            if (i == 0) {
                clearConnectedView();
                return;
            }
            return;
        }
        clearConnectedView();
    }

    private void tryStartHandwriting() {
        View connectedView;
        if (!this.mState.mExceedTouchSlop || (connectedView = getConnectedView()) == null) {
            return;
        }
        if (!connectedView.isAutoHandwritingEnabled()) {
            clearConnectedView();
            return;
        }
        Rect handwritingArea = getViewHandwritingArea(connectedView);
        if (contains(handwritingArea, this.mState.mStylusDownX, this.mState.mStylusDownY)) {
            startHandwriting(connectedView);
        } else {
            reset();
        }
    }

    public void startHandwriting(View view) {
        this.mImm.startStylusHandwriting(view);
        reset();
    }

    public void updateHandwritingAreasForView(View view) {
        this.mHandwritingAreasTracker.updateHandwritingAreaForView(view);
    }

    private View findBestCandidateView(float x, float y) {
        View connectedView = getConnectedView();
        if (connectedView != null && connectedView.isAutoHandwritingEnabled()) {
            Rect handwritingArea = getViewHandwritingArea(connectedView);
            if (contains(handwritingArea, x, y)) {
                return connectedView;
            }
        }
        List<HandwritableViewInfo> handwritableViewInfos = this.mHandwritingAreasTracker.computeViewInfos();
        for (HandwritableViewInfo viewInfo : handwritableViewInfos) {
            View view = viewInfo.getView();
            if (view.isAutoHandwritingEnabled() && contains(viewInfo.getHandwritingArea(), x, y)) {
                return viewInfo.getView();
            }
        }
        return null;
    }

    private static Rect getViewHandwritingArea(View view) {
        ViewParent viewParent = view.getParent();
        if (viewParent != null && view.isAttachedToWindow() && view.isAggregatedVisible()) {
            Rect localHandwritingArea = view.getHandwritingArea();
            Rect globalHandwritingArea = new Rect();
            if (localHandwritingArea != null) {
                globalHandwritingArea.set(localHandwritingArea);
            } else {
                globalHandwritingArea.set(0, 0, view.getWidth(), view.getHeight());
            }
            if (viewParent.getChildVisibleRect(view, globalHandwritingArea, null)) {
                return globalHandwritingArea;
            }
        }
        return null;
    }

    private boolean contains(Rect rect, float x, float y) {
        return rect != null && x >= ((float) rect.left) && x < ((float) rect.right) && y >= ((float) rect.top) && y < ((float) rect.bottom);
    }

    private boolean largerThanTouchSlop(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        float f = (dx * dx) + (dy * dy);
        int i = this.mTouchSlop;
        return f > ((float) (i * i));
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public static class State {
        private boolean mExceedTouchSlop;
        private boolean mShouldInitHandwriting;
        private long mStylusDownTimeInMillis;
        private float mStylusDownX;
        private float mStylusDownY;
        private int mStylusPointerId;

        private State() {
            this.mShouldInitHandwriting = false;
            this.mExceedTouchSlop = false;
            this.mStylusPointerId = -1;
            this.mStylusDownTimeInMillis = -1L;
            this.mStylusDownX = Float.NaN;
            this.mStylusDownY = Float.NaN;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isViewActive(View view) {
        return view != null && view.isAttachedToWindow() && view.isAggregatedVisible() && view.isAutoHandwritingEnabled();
    }

    /* loaded from: classes3.dex */
    public static class HandwritingAreaTracker {
        private final List<HandwritableViewInfo> mHandwritableViewInfos = new ArrayList();

        public void updateHandwritingAreaForView(View view) {
            Iterator<HandwritableViewInfo> iterator = this.mHandwritableViewInfos.iterator();
            boolean found = false;
            while (iterator.hasNext()) {
                HandwritableViewInfo handwritableViewInfo = iterator.next();
                View curView = handwritableViewInfo.getView();
                if (!HandwritingInitiator.isViewActive(curView)) {
                    iterator.remove();
                }
                if (curView == view) {
                    found = true;
                    handwritableViewInfo.mIsDirty = true;
                }
            }
            if (!found && HandwritingInitiator.isViewActive(view)) {
                this.mHandwritableViewInfos.add(new HandwritableViewInfo(view));
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ boolean lambda$computeViewInfos$0(HandwritableViewInfo viewInfo) {
            return !viewInfo.update();
        }

        public List<HandwritableViewInfo> computeViewInfos() {
            this.mHandwritableViewInfos.removeIf(new Predicate() { // from class: android.view.HandwritingInitiator$HandwritingAreaTracker$$ExternalSyntheticLambda0
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return HandwritingInitiator.HandwritingAreaTracker.lambda$computeViewInfos$0((HandwritingInitiator.HandwritableViewInfo) obj);
                }
            });
            return this.mHandwritableViewInfos;
        }
    }

    /* loaded from: classes3.dex */
    public static class HandwritableViewInfo {
        Rect mHandwritingArea = null;
        public boolean mIsDirty = true;
        final WeakReference<View> mViewRef;

        public HandwritableViewInfo(View view) {
            this.mViewRef = new WeakReference<>(view);
        }

        public View getView() {
            return this.mViewRef.get();
        }

        public Rect getHandwritingArea() {
            return this.mHandwritingArea;
        }

        public boolean update() {
            View view = getView();
            if (HandwritingInitiator.isViewActive(view)) {
                if (this.mIsDirty) {
                    Rect handwritingArea = view.getHandwritingArea();
                    if (handwritingArea == null) {
                        return false;
                    }
                    ViewParent parent = view.getParent();
                    if (parent != null) {
                        if (this.mHandwritingArea == null) {
                            this.mHandwritingArea = new Rect();
                        }
                        this.mHandwritingArea.set(handwritingArea);
                        if (!parent.getChildVisibleRect(view, this.mHandwritingArea, null)) {
                            this.mHandwritingArea = null;
                        }
                    }
                    this.mIsDirty = false;
                    return true;
                }
                return true;
            }
            return false;
        }
    }
}
