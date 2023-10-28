package android.widget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.RemotableViewMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import android.widget.SlideViewAdapter;
import com.android.internal.R;
import java.util.ArrayList;
import java.util.HashMap;
@RemoteViews.RemoteView
/* loaded from: classes4.dex */
public class SlideAdapterView extends AdapterView<Adapter> implements SlideViewAdapter.RemoteAdapterConnectionCallback {
    private static final int DEFAULT_ANIMATION_DURATION = 350;
    private static final int DEFAULT_INTERVAL = 5000;
    private static final String TAG = "SlideAdapterView";
    static final int TOUCH_MODE_DOWN_IN_CURRENT_VIEW = 1;
    static final int TOUCH_MODE_HANDLED = 2;
    static final int TOUCH_MODE_NONE = 0;
    private final BasicCuberInterpolator interpolator;
    int itemCount;
    int mActiveOffset;
    Adapter mAdapter;
    private boolean mAutoStart;
    AdapterView<Adapter>.AdapterDataSetObserver mDataSetObserver;
    boolean mDeferNotifyDataSetChanged;
    private int mFlipInterval;
    private final Runnable mFlipRunnable;
    ObjectAnimator mInAnimation;
    private int mLastItemCount;
    boolean mLoopViews;
    int mMaxNumActiveViews;
    ObjectAnimator mOutAlphaAnim;
    ObjectAnimator mOutTranslationAnim;
    ArrayList<View> mPreviousViews;
    int mReferenceChildHeight;
    int mReferenceChildWidth;
    private SlideViewAdapter mRemoteAdapter;
    private int mRestoreWhichChild;
    private boolean mRunning;
    private boolean mStarted;
    private int mTouchMode;
    HashMap<Integer, ViewAndMetaData> mViewsMap;
    private boolean mVisible;
    private int mVisibleWindowIndex;
    int mWhichChild;
    private final String maskViewTag;
    private final RoundHelper roundHelper;

    public SlideAdapterView(Context context) {
        this(context, null);
    }

    public SlideAdapterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlideAdapterView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SlideAdapterView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.maskViewTag = "MASK_VIEW";
        this.mWhichChild = 0;
        this.mRestoreWhichChild = -1;
        this.mActiveOffset = 0;
        this.mMaxNumActiveViews = 1;
        this.mViewsMap = new HashMap<>();
        this.mDeferNotifyDataSetChanged = false;
        this.mLoopViews = true;
        this.mReferenceChildWidth = -1;
        this.mReferenceChildHeight = -1;
        this.mTouchMode = 0;
        this.mAutoStart = false;
        this.mRunning = false;
        this.mStarted = false;
        this.mVisible = false;
        this.mFlipInterval = 5000;
        this.mVisibleWindowIndex = -1;
        RoundHelper roundHelper = new RoundHelper();
        this.roundHelper = roundHelper;
        this.mLastItemCount = 0;
        this.mFlipRunnable = new Runnable() { // from class: android.widget.SlideAdapterView$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                SlideAdapterView.this.m6049lambda$new$0$androidwidgetSlideAdapterView();
            }
        };
        this.mAutoStart = true;
        initViewAnimator();
        roundHelper.init(context, attrs, this);
        this.interpolator = new BasicCuberInterpolator(0.25f, 0.0f, 0.0f, 1.0f);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AdapterViewFlipper, defStyleAttr, defStyleRes);
        this.mFlipInterval = a.getInt(0, 5000);
        a.recycle();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$android-widget-SlideAdapterView  reason: not valid java name */
    public /* synthetic */ void m6049lambda$new$0$androidwidgetSlideAdapterView() {
        if (this.mRunning) {
            showNext();
        }
    }

    private void initViewAnimator() {
        this.mPreviousViews = new ArrayList<>();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes4.dex */
    public class ViewAndMetaData {
        int adapterPosition;
        long itemId;
        int relativeIndex;
        View view;

        ViewAndMetaData(View view, int relativeIndex, int adapterPosition, long itemId) {
            this.view = view;
            this.relativeIndex = relativeIndex;
            this.adapterPosition = adapterPosition;
            this.itemId = itemId;
        }
    }

    @Override // android.view.ViewGroup, android.view.View
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mAutoStart) {
            Log.d(TAG, "<onAttachedToWindow> auto start");
            startFlipping();
        }
    }

    @Override // android.widget.AdapterView, android.view.ViewGroup, android.view.View
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mVisible = false;
        Log.d(TAG, "<onDetachedFromWindow>");
        updateRunning();
    }

    public void startFlipping() {
        this.mStarted = true;
        updateRunning();
    }

    public void stopFlipping() {
        this.mStarted = false;
        updateRunning();
    }

    @Override // android.view.View
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.roundHelper.onSizeChanged(w, h);
    }

    @Override // android.view.View
    public void draw(Canvas canvas) {
        this.roundHelper.preDraw(canvas);
        super.draw(canvas);
        this.roundHelper.drawPath(canvas);
    }

    private void updateRunning() {
        updateRunning(true);
    }

    private void updateRunning(boolean flipNow) {
        boolean running = this.mVisible && this.mStarted && this.mAdapter != null;
        Log.d(TAG, "<updateRunning> mVisible= " + this.mVisible + ", mStarted= " + this.mStarted + ", mRunning= " + this.mRunning);
        if (running != this.mRunning) {
            this.mRunning = running;
            if (running) {
                reShowCurrent(flipNow, false);
            } else {
                removeCallbacks(this.mFlipRunnable);
            }
        }
    }

    @Override // android.view.View
    public void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        this.mVisible = visibility == 0;
        Log.d(TAG, "<onWindowVisibilityChanged> mVisible: " + this.mVisible);
        updateRunning(false);
    }

    void transformViewForTransition(int fromIndex, int toIndex, View view) {
        ObjectAnimator objectAnimator;
        ObjectAnimator objectAnimator2;
        if (fromIndex == -1 && (objectAnimator2 = this.mInAnimation) != null) {
            objectAnimator2.setTarget(view);
            this.mInAnimation.start();
        } else if (toIndex == -1 && (objectAnimator = this.mOutTranslationAnim) != null && this.mOutAlphaAnim != null) {
            objectAnimator.setTarget(view);
            this.mOutTranslationAnim.start();
            View maskView = view.findViewWithTag("MASK_VIEW");
            if (maskView != null) {
                this.mOutAlphaAnim.setTarget(maskView);
                this.mOutAlphaAnim.start();
            }
        }
    }

    ObjectAnimator getDefaultInAnimation() {
        ObjectAnimator anim = ObjectAnimator.ofFloat((Object) null, "translationX", getMeasuredWidth(), 0.0f);
        anim.setInterpolator(this.interpolator);
        anim.setDuration(350L);
        return anim;
    }

    ObjectAnimator getDefaultOutAnimation1() {
        ObjectAnimator anim = ObjectAnimator.ofFloat((Object) null, "translationX", 0.0f, (-getMeasuredWidth()) / 2.0f);
        anim.setInterpolator(this.interpolator);
        anim.setDuration(350L);
        return anim;
    }

    ObjectAnimator getDefaultOutAnimation2() {
        ObjectAnimator anim = ObjectAnimator.ofFloat((Object) null, "alpha", 0.0f, 0.4f);
        anim.setInterpolator(this.interpolator);
        anim.setDuration(350L);
        return anim;
    }

    public void setDisplayedChild(int whichChild) {
        setDisplayedChild(whichChild, true, false);
    }

    private void setDisplayedChild(int whichChild, boolean animate, boolean dateChanged) {
        if (this.mAdapter != null) {
            int toShowChild = getNextPosition(whichChild);
            Log.d(TAG, "<setDisplayedChild> toShowChild: " + toShowChild + ", mLoopViews: " + this.mLoopViews + ", dateChanged: " + dateChanged);
            if (toShowChild >= 0) {
                this.mWhichChild = toShowChild;
                boolean hasFocus = getFocusedChild() != null;
                showOnly(this.mWhichChild, animate, dateChanged);
                if (hasFocus) {
                    requestFocus(2);
                }
            }
        }
    }

    private int getNextPosition(int nextPosition) {
        int count = getCount();
        if (nextPosition < 0 || count <= 0) {
            return -1;
        }
        if (nextPosition >= count) {
            return nextPosition % count;
        }
        return nextPosition;
    }

    public int getDisplayedChild() {
        return this.mWhichChild;
    }

    public void showNext() {
        if (!this.mLoopViews && this.mWhichChild == getCount() - 1) {
            Log.d(TAG, "<showNext> not show next child for no loopview: " + this.mWhichChild);
            if (this.mRunning) {
                removeCallbacks(this.mFlipRunnable);
                return;
            }
            return;
        }
        if (this.mRunning) {
            removeCallbacks(this.mFlipRunnable);
            postDelayed(this.mFlipRunnable, this.mFlipInterval);
        }
        Log.d(TAG, "<showNext> mWhichChild: " + (this.mWhichChild + 1));
        setDisplayedChild(this.mWhichChild + 1);
    }

    public void reShowCurrent(boolean animate, boolean dateChanged) {
        if (dateChanged && getCount() <= 0) {
            Log.d(TAG, "<reShowCurrent> empty dates so delete all views");
            if (this.mRunning) {
                removeCallbacks(this.mFlipRunnable);
            }
            clearAllCaches();
        } else if (!this.mLoopViews && this.mWhichChild == getCount() - 1) {
            Log.d(TAG, "<reShowCurrent> not show next child for no loopview: " + this.mWhichChild);
            if (this.mRunning) {
                removeCallbacks(this.mFlipRunnable);
            }
        } else if (this.mRunning) {
            removeCallbacks(this.mFlipRunnable);
            postDelayed(this.mFlipRunnable, this.mFlipInterval);
        }
        setDisplayedChild(this.mWhichChild, animate, dateChanged);
    }

    public void clearAllCaches() {
        for (int i = 0; i < this.mPreviousViews.size(); i++) {
            View viewToRemove = this.mPreviousViews.get(i);
            viewToRemove.clearAnimation();
            if (viewToRemove instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) viewToRemove;
                vg.removeAllViewsInLayout();
            }
            removeViewInLayout(viewToRemove);
        }
        this.mPreviousViews.clear();
        int showingCount = this.mViewsMap.size();
        for (int i2 = 0; i2 < showingCount; i2++) {
            ViewAndMetaData metaData = this.mViewsMap.get(Integer.valueOf(i2));
            if (metaData != null) {
                View viewToRemove2 = metaData.view;
                viewToRemove2.clearAnimation();
                if (viewToRemove2 instanceof ViewGroup) {
                    ViewGroup vg2 = (ViewGroup) viewToRemove2;
                    vg2.removeAllViewsInLayout();
                }
                removeViewInLayout(viewToRemove2);
            }
        }
        this.mViewsMap.clear();
    }

    int modulo(int pos, int size) {
        if (size > 0) {
            return ((pos % size) + size) % size;
        }
        return 0;
    }

    View getViewAtRelativeIndex(int relativeIndex) {
        if (relativeIndex >= 0 && relativeIndex <= getNumActiveViews() - 1 && this.mAdapter != null) {
            int i = modulo(relativeIndex, getWindowSize());
            if (this.mViewsMap.get(Integer.valueOf(i)) != null) {
                return this.mViewsMap.get(Integer.valueOf(i)).view;
            }
            return null;
        }
        return null;
    }

    int getNumActiveViews() {
        if (this.mAdapter != null) {
            return Math.min(getCount() + 1, this.mMaxNumActiveViews);
        }
        return this.mMaxNumActiveViews;
    }

    int getWindowSize() {
        if (this.mAdapter != null) {
            int adapterCount = getCount();
            if (adapterCount <= getNumActiveViews() && this.mLoopViews) {
                return this.mMaxNumActiveViews * adapterCount;
            }
            return adapterCount;
        }
        return 0;
    }

    @Override // android.widget.AdapterView
    public int getCount() {
        return this.itemCount;
    }

    private ViewAndMetaData getMetaDataForChild(View child) {
        for (ViewAndMetaData vm : this.mViewsMap.values()) {
            if (vm.view == child) {
                return vm;
            }
        }
        return null;
    }

    ViewGroup.LayoutParams createOrReuseLayoutParams(View v) {
        ViewGroup.LayoutParams currentLp = v.getLayoutParams();
        if (currentLp != null) {
            return currentLp;
        }
        return new ViewGroup.LayoutParams(0, 0);
    }

    FrameLayout getFrameForChild() {
        return new FrameLayout(getContext());
    }

    void showOnly(int childIndex, boolean animate, boolean dateChanged) {
        if (this.mAdapter == null) {
            return;
        }
        int adapterCount = getCount();
        if (adapterCount == 0) {
            return;
        }
        for (int i = 0; i < this.mPreviousViews.size(); i++) {
            View viewToRemove = this.mPreviousViews.get(i);
            viewToRemove.clearAnimation();
            if (viewToRemove instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) viewToRemove;
                vg.removeAllViewsInLayout();
            }
            removeViewInLayout(viewToRemove);
        }
        this.mPreviousViews.clear();
        int realIndex = modulo(childIndex, getCount());
        if (dateChanged) {
            Log.d(TAG, "<showOnly> date changed, load new view");
        }
        boolean isVisible = !dateChanged && this.mViewsMap.containsKey(Integer.valueOf(realIndex));
        if (!isVisible) {
            View newView = this.mAdapter.getView(realIndex, null, this);
            if (newView == null) {
                if (this.mRunning) {
                    removeCallbacks(this.mFlipRunnable);
                    this.mWhichChild = realIndex - 1;
                    postDelayed(this.mFlipRunnable, this.mFlipInterval);
                }
                Log.e(TAG, "<showOnly> failed to load remoteView position: " + realIndex + ", so retry load it next time");
                return;
            }
            for (Integer index : this.mViewsMap.keySet()) {
                boolean remove = false;
                if (index.intValue() != realIndex) {
                    remove = true;
                }
                if (remove) {
                    View previousView = this.mViewsMap.get(index).view;
                    int oldRelativeIndex = this.mViewsMap.get(index).relativeIndex;
                    this.mPreviousViews.add(previousView);
                    this.mViewsMap.remove(index);
                    if (animate) {
                        transformViewForTransition(oldRelativeIndex, -1, previousView);
                    }
                }
                if (index.intValue() == realIndex) {
                    this.mPreviousViews.add(this.mViewsMap.get(index).view);
                    this.mViewsMap.remove(index);
                }
            }
            long itemId = this.mAdapter.getItemId(realIndex);
            FrameLayout fl = getFrameForChild();
            fl.addView(newView);
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-1, -1);
            View maskView = new View(getContext());
            maskView.setBackgroundColor(-16777216);
            maskView.setAlpha(0.0f);
            maskView.setTag("MASK_VIEW");
            fl.addView(maskView, layoutParams);
            this.mViewsMap.put(Integer.valueOf(realIndex), new ViewAndMetaData(fl, realIndex, realIndex, itemId));
            addChild(fl);
            Log.d(TAG, "<showOnly> load view index: " + realIndex);
            if (animate) {
                transformViewForTransition(-1, 0, fl);
            }
            this.mViewsMap.get(Integer.valueOf(realIndex)).view.bringToFront();
            SlideViewAdapter slideViewAdapter = this.mRemoteAdapter;
            if (slideViewAdapter != null) {
                slideViewAdapter.setVisibleHint(realIndex);
            }
            setVisibleHint(realIndex);
            requestLayout();
            invalidate();
            return;
        }
        this.mViewsMap.get(Integer.valueOf(realIndex)).view.bringToFront();
        SlideViewAdapter slideViewAdapter2 = this.mRemoteAdapter;
        if (slideViewAdapter2 != null) {
            slideViewAdapter2.setVisibleHint(realIndex);
        }
        setVisibleHint(realIndex);
        requestLayout();
        invalidate();
        Log.d(TAG, "<showOnly> this item is in the current window, so do not need create new one");
    }

    public void setVisibleHint(int showingIndex) {
        this.mVisibleWindowIndex = showingIndex;
    }

    private void addChild(View child) {
        addViewInLayout(child, -1, createOrReuseLayoutParams(child));
        if (this.mReferenceChildWidth == -1 || this.mReferenceChildHeight == -1) {
            int measureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
            child.measure(measureSpec, measureSpec);
            this.mReferenceChildWidth = child.getMeasuredWidth();
            this.mReferenceChildHeight = child.getMeasuredHeight();
        }
    }

    void showTapFeedback(View v) {
        v.setPressed(true);
    }

    void hideTapFeedback(View v) {
        v.setPressed(false);
    }

    void cancelHandleClick() {
        View v = getCurrentView();
        if (v != null) {
            hideTapFeedback(v);
        }
        this.mTouchMode = 0;
    }

    /* loaded from: classes4.dex */
    final class CheckForTap implements Runnable {
        CheckForTap() {
        }

        @Override // java.lang.Runnable
        public void run() {
            if (SlideAdapterView.this.mTouchMode == 1) {
                View v = SlideAdapterView.this.getCurrentView();
                SlideAdapterView.this.showTapFeedback(v);
            }
        }
    }

    private void measureChildren() {
        int count = getChildCount();
        int childWidth = (getMeasuredWidth() - getPaddingLeft()) - getPaddingRight();
        int childHeight = (getMeasuredHeight() - getPaddingTop()) - getPaddingBottom();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            child.measure(View.MeasureSpec.makeMeasureSpec(childWidth, 1073741824), View.MeasureSpec.makeMeasureSpec(childHeight, 1073741824));
        }
    }

    @Override // android.view.View
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecSize = View.MeasureSpec.getSize(heightMeasureSpec);
        int widthSpecMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int heightSpecMode = View.MeasureSpec.getMode(heightMeasureSpec);
        boolean haveChildRefSize = (this.mReferenceChildWidth == -1 || this.mReferenceChildHeight == -1) ? false : true;
        if (heightSpecMode == 0) {
            heightSpecSize = haveChildRefSize ? this.mReferenceChildHeight + getPaddingTop() + getPaddingBottom() : 0;
        } else if (heightSpecMode == Integer.MIN_VALUE && haveChildRefSize) {
            int height = this.mReferenceChildHeight + getPaddingTop() + getPaddingBottom();
            heightSpecSize = height > heightSpecSize ? heightSpecSize | 16777216 : height;
        }
        if (widthSpecMode == 0) {
            widthSpecSize = haveChildRefSize ? getPaddingRight() + this.mReferenceChildWidth + getPaddingLeft() : 0;
        } else if (heightSpecMode == Integer.MIN_VALUE && haveChildRefSize) {
            int width = this.mReferenceChildWidth + getPaddingLeft() + getPaddingRight();
            widthSpecSize = width > widthSpecSize ? widthSpecSize | 16777216 : width;
        }
        setMeasuredDimension(widthSpecSize, heightSpecSize);
        measureChildren();
        this.mInAnimation = getDefaultInAnimation();
        this.mOutTranslationAnim = getDefaultOutAnimation1();
        this.mOutAlphaAnim = getDefaultOutAnimation2();
    }

    @Override // android.widget.AdapterView, android.view.ViewGroup, android.view.View
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            int childRight = getPaddingLeft() + child.getMeasuredWidth();
            int childBottom = getPaddingTop() + child.getMeasuredHeight();
            child.layout(getPaddingLeft(), getPaddingTop(), childRight, childBottom);
        }
    }

    void checkForAndHandleDataChanged() {
        if (this.mAdapter == null) {
            return;
        }
        post(new Runnable() { // from class: android.widget.SlideAdapterView.1
            @Override // java.lang.Runnable
            public void run() {
                SlideAdapterView.this.handleDataChanged();
                SlideAdapterView.this.mWhichChild = 0;
                SlideAdapterView slideAdapterView = SlideAdapterView.this;
                slideAdapterView.itemCount = slideAdapterView.mAdapter.getCount();
                if (SlideAdapterView.this.itemCount <= 1) {
                    SlideAdapterView.this.mLoopViews = false;
                } else {
                    SlideAdapterView.this.mLoopViews = true;
                }
                boolean dataSizeChange = SlideAdapterView.this.mLastItemCount != SlideAdapterView.this.itemCount;
                Log.d(SlideAdapterView.TAG, "<checkForAndHandleDataChanged> lastCount: " + SlideAdapterView.this.mLastItemCount + ", itemCount: " + SlideAdapterView.this.itemCount);
                SlideAdapterView slideAdapterView2 = SlideAdapterView.this;
                slideAdapterView2.mLastItemCount = slideAdapterView2.itemCount;
                SlideAdapterView.this.mVisibleWindowIndex = -1;
                SlideAdapterView.this.reShowCurrent(false, dataSizeChange);
                SlideAdapterView.this.requestLayout();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes4.dex */
    public static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() { // from class: android.widget.SlideAdapterView.SavedState.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        int whichChild;

        SavedState(Parcelable superState, int whichChild) {
            super(superState);
            this.whichChild = whichChild;
        }

        private SavedState(Parcel in) {
            super(in);
            this.whichChild = in.readInt();
        }

        @Override // android.view.View.BaseSavedState, android.view.AbsSavedState, android.os.Parcelable
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.whichChild);
        }

        public String toString() {
            return "AdapterViewAnimator.SavedState{ whichChild = " + this.whichChild + " }";
        }
    }

    @Override // android.view.View
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, this.mWhichChild);
    }

    @Override // android.view.View
    public void onRestoreInstanceState(Parcelable state) {
        if (state == null) {
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        int i = ss.whichChild;
        this.mWhichChild = i;
        if (this.mAdapter != null) {
            this.mRestoreWhichChild = i;
        }
    }

    public View getCurrentView() {
        return getViewAtRelativeIndex(this.mActiveOffset);
    }

    @Override // android.view.View
    public int getBaseline() {
        return getCurrentView() != null ? getCurrentView().getBaseline() : super.getBaseline();
    }

    @Override // android.widget.AdapterView
    public Adapter getAdapter() {
        return this.mAdapter;
    }

    @Override // android.widget.AdapterView
    public void setAdapter(Adapter adapter) {
        AdapterView<Adapter>.AdapterDataSetObserver adapterDataSetObserver;
        Adapter adapter2 = this.mAdapter;
        if (adapter2 != null && (adapterDataSetObserver = this.mDataSetObserver) != null) {
            adapter2.unregisterDataSetObserver(adapterDataSetObserver);
        }
        this.mAdapter = adapter;
        checkFocus();
        if (this.mAdapter != null) {
            AdapterView<Adapter>.AdapterDataSetObserver adapterDataSetObserver2 = new AdapterView.AdapterDataSetObserver();
            this.mDataSetObserver = adapterDataSetObserver2;
            this.mAdapter.registerDataSetObserver(adapterDataSetObserver2);
            int count = this.mAdapter.getCount();
            this.itemCount = count;
            if (count <= 1) {
                this.mLoopViews = false;
            } else {
                this.mLoopViews = true;
            }
        }
        setFocusable(true);
        this.mWhichChild = 0;
        this.mVisibleWindowIndex = -1;
        this.mLastItemCount = 0;
        showOnly(0, false, false);
        updateRunning();
    }

    @RemotableViewMethod(asyncImpl = "setRemoteViewsAdapterAsync")
    public void setRemoteViewsAdapter(Intent intent) {
        setRemoteViewsAdapter(intent, false);
    }

    @Override // android.widget.SlideViewAdapter.RemoteAdapterConnectionCallback
    public boolean onRemoteAdapterConnected() {
        SlideViewAdapter slideViewAdapter = this.mRemoteAdapter;
        if (slideViewAdapter != this.mAdapter) {
            Log.d(TAG, "<onRemoteAdapterConnected>");
            setAdapter(this.mRemoteAdapter);
            int i = this.mRestoreWhichChild;
            if (i > -1) {
                setDisplayedChild(i, false, false);
                this.mRestoreWhichChild = -1;
            }
            return false;
        } else if (slideViewAdapter != null) {
            slideViewAdapter.superNotifyDataSetChanged();
            return true;
        } else {
            return false;
        }
    }

    @Override // android.widget.SlideViewAdapter.RemoteAdapterConnectionCallback
    public void onRemoteAdapterDisconnected() {
    }

    @Override // android.widget.SlideViewAdapter.RemoteAdapterConnectionCallback
    public void setRemoteViewsAdapter(Intent intent, boolean isAsync) {
        if (this.mRemoteAdapter != null) {
            Intent.FilterComparison fcNew = new Intent.FilterComparison(intent);
            Intent.FilterComparison fcOld = new Intent.FilterComparison(this.mRemoteAdapter.createRemoteViewsServiceIntent());
            if (fcNew.equals(fcOld)) {
                return;
            }
        }
        Log.d(TAG, "<setRemoteViewsAdapter>");
        this.mDeferNotifyDataSetChanged = false;
        this.mRemoteAdapter = new SlideViewAdapter(getContext(), intent, isAsync, this);
    }

    @Override // android.widget.SlideViewAdapter.RemoteAdapterConnectionCallback
    public void onDataChanged() {
        checkForAndHandleDataChanged();
    }

    @Override // android.widget.AdapterView
    public void setSelection(int position) {
        setDisplayedChild(position);
    }

    @Override // android.widget.AdapterView
    public View getSelectedView() {
        return getViewAtRelativeIndex(this.mActiveOffset);
    }

    public void setRemoteViewsOnClickHandler(RemoteViews.InteractionHandler handler) {
        SlideViewAdapter slideViewAdapter = this.mRemoteAdapter;
        if (slideViewAdapter != null) {
            slideViewAdapter.setRemoteViewsInteractionHandler(handler);
        }
    }

    @Override // android.widget.AdapterView, android.view.ViewGroup, android.view.View
    public CharSequence getAccessibilityClassName() {
        return AdapterViewAnimator.class.getName();
    }
}
