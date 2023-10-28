package com.android.internal.widget;

import android.app.ActivityTaskManager;
import android.app.WindowConfiguration;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.android.internal.R;
import com.android.internal.policy.DecorView;
import com.android.internal.policy.PhoneWindow;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
/* loaded from: classes4.dex */
public class TranMultiDecorCaptionViewV3 extends DecorCaptionView {
    private static final int INACTIVE_MULTI_WINDOW_LONGCLICK = 10;
    private static final int INACTIVE_MULTI_WINDOW_LONGCLICK_TIME = 1500;
    private static final String TAG = "TranMultiDecorCaptionView";
    public static final String TAG_MULTI = "TranMultiWindow";
    private static final int TRAN_COVER_LINE_VIEW = 0;
    private static final int TRAN_COVER_TOUCH_MAX = 1;
    private static final int TRAN_TOP_CAPTION_VIEW = 2;
    private static Set<String> mDontNeedBoundSet = new HashSet<String>() { // from class: com.android.internal.widget.TranMultiDecorCaptionViewV3.1
        {
            add("VoipActivityV2");
            add("NotesList");
            add("FileSendActivity");
        }
    };
    private boolean VIEW_DEBUG;
    private boolean isHor;
    private View mActiveChangeMax;
    private View mActiveChangeSmall;
    private View mActiveMute;
    private ImageView mActiveMuteImageView;
    private ActivityTaskManager mActivityTaskManager;
    private int mCaptionHeight;
    private CaptionViewClickListener mCaptionViewClickListener;
    private boolean mCheckForDragging;
    private View mClickTarget;
    private View mClose;
    private View mContent;
    private Context mContext;
    private View mCoverActiveMoreFun;
    private View mCoverActivieMoreFunBubble;
    private View mCoverIcon;
    private View mCoverTouchLine;
    private View mCoverTouchMax;
    private View mCoverView;
    private boolean mDownInCover;
    private int mDragSlop;
    private boolean mDragging;
    private GestureDetector mGestureDetector;
    private Handler mHandler;
    private boolean mHasMove;
    private int mHeight;
    private View mInActiveClose;
    private View mInActiveCloseHor;
    private View mInActiveMute;
    private View mInActiveMuteHor;
    private int mLastTopInset;
    private View mMaximize;
    private Rect mMoreFunRect;
    private View mMoreFunction;
    private ImageView mMoreFunctionButton;
    private ImageView mMoreFunctionChangeMaxImageView;
    private ImageView mMoreFunctionChangeSmallImageView;
    private boolean mMultiWindowActive;
    private boolean mMuteState;
    private boolean mOverlayWithAppContent;
    private PhoneWindow mOwner;
    private PackageManager mPackageManager;
    private int mRootScrollY;
    private int mSetStatusBarColor;
    private boolean mShow;
    private int mStatusBarColor;
    private View mTopCaption;
    private ArrayList<View> mTouchDispatchList;
    private int mTouchDownRawX;
    private int mTouchDownRawY;
    private int mTouchDownX;
    private int mTouchDownY;
    private int mWidth;
    private WindowConfiguration mWindowConfig;
    private boolean needPushDown;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes4.dex */
    public class CaptionHandler extends Handler {
        public CaptionHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
        }
    }

    public TranMultiDecorCaptionViewV3(Context context) {
        super(context);
        this.mOwner = null;
        this.mShow = false;
        this.mDragging = false;
        this.mOverlayWithAppContent = false;
        this.mMoreFunRect = new Rect();
        this.mTouchDispatchList = new ArrayList<>(3);
        this.mMultiWindowActive = false;
        this.mWindowConfig = new WindowConfiguration();
        this.mLastTopInset = 0;
        this.needPushDown = false;
        this.isHor = false;
        this.VIEW_DEBUG = SystemProperties.getBoolean("sys.tran.debug", false);
        this.mSetStatusBarColor = -1;
        init(context);
    }

    public TranMultiDecorCaptionViewV3(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mOwner = null;
        this.mShow = false;
        this.mDragging = false;
        this.mOverlayWithAppContent = false;
        this.mMoreFunRect = new Rect();
        this.mTouchDispatchList = new ArrayList<>(3);
        this.mMultiWindowActive = false;
        this.mWindowConfig = new WindowConfiguration();
        this.mLastTopInset = 0;
        this.needPushDown = false;
        this.isHor = false;
        this.VIEW_DEBUG = SystemProperties.getBoolean("sys.tran.debug", false);
        this.mSetStatusBarColor = -1;
        init(context);
    }

    public TranMultiDecorCaptionViewV3(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mOwner = null;
        this.mShow = false;
        this.mDragging = false;
        this.mOverlayWithAppContent = false;
        this.mMoreFunRect = new Rect();
        this.mTouchDispatchList = new ArrayList<>(3);
        this.mMultiWindowActive = false;
        this.mWindowConfig = new WindowConfiguration();
        this.mLastTopInset = 0;
        this.needPushDown = false;
        this.isHor = false;
        this.VIEW_DEBUG = SystemProperties.getBoolean("sys.tran.debug", false);
        this.mSetStatusBarColor = -1;
        init(context);
    }

    private void init(Context context) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        this.mHandler = new CaptionHandler(this.mContext.getMainLooper());
        this.mActivityTaskManager = (ActivityTaskManager) this.mContext.getSystemService(Context.ACTIVITY_TASK_SERVICE);
        this.mCaptionViewClickListener = new CaptionViewClickListener();
        this.mDragSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.mGestureDetector = new GestureDetector(context, this);
        setContentDescription(context.getString(R.string.accessibility_freeform_caption, context.getPackageManager().getApplicationLabel(context.getApplicationInfo())));
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.internal.widget.DecorCaptionView, android.view.View
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mTopCaption = getChildAt(1);
        View childAt = getChildAt(0);
        this.mCoverView = childAt;
        this.mCoverTouchLine = childAt.findViewById(R.id.zzzz_cover_touch_line);
        this.mCoverTouchMax = this.mCoverView.findViewById(R.id.zzzz_cover_touch_max);
        this.mCoverIcon = this.mCoverView.findViewById(R.id.zzzz_cover_icon);
        this.mCoverActiveMoreFun = this.mCoverView.findViewById(R.id.zzzz_active_more_function);
        this.mCoverActivieMoreFunBubble = this.mCoverView.findViewById(R.id.zzzz_active_more_function_bubble);
        this.mMaximize = this.mTopCaption.findViewById(R.id.zzzz_maximize_window);
        this.mClose = this.mTopCaption.findViewById(R.id.zzzz_close_window);
        this.mMoreFunction = this.mTopCaption.findViewById(R.id.zzzz_more_function);
        this.mActiveChangeMax = this.mCoverView.findViewById(R.id.zzzz_active_change_max);
        this.mActiveChangeSmall = this.mCoverView.findViewById(R.id.zzzz_active_change_small);
        this.mActiveMute = this.mCoverView.findViewById(R.id.zzzz_active_mute);
        this.mMoreFunctionButton = (ImageView) this.mTopCaption.findViewById(R.id.zzzz_more_function_button);
        this.mActiveMuteImageView = (ImageView) this.mCoverView.findViewById(R.id.zzzz_active_mute_imageview);
        this.mMoreFunctionChangeMaxImageView = (ImageView) this.mCoverView.findViewById(R.id.zzzz_active_change_max_imageview);
        this.mMoreFunctionChangeSmallImageView = (ImageView) this.mCoverView.findViewById(R.id.zzzz_active_change_small_imageview);
        this.mCoverView.bringToFront();
    }

    @Override // com.android.internal.widget.DecorCaptionView
    public void setPhoneWindow(PhoneWindow owner, boolean show) {
        this.mOwner = owner;
        this.mShow = show;
        this.mOverlayWithAppContent = owner.isOverlayWithDecorCaptionEnabled();
        DecorView decor = null;
        if (this.mOwner.getDecorView() instanceof DecorView) {
            decor = (DecorView) this.mOwner.getDecorView();
        }
        boolean needBounds = decor == null || !mDontNeedBoundSet.contains(decor.getTitleLast());
        ViewOutlineProvider outlineProvider = needBounds ? ViewOutlineProvider.BOUNDS : ViewOutlineProvider.TRANS_BOUNDS;
        this.mOwner.getDecorView().setOutlineProvider(outlineProvider);
        Log.d(TAG, "TranMultiDecorCaptionView titleLast=" + (decor != null ? decor.getTitleLast() : null));
    }

    @Override // com.android.internal.widget.DecorCaptionView, android.view.ViewGroup
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == 0) {
            this.mClickTarget = null;
            int x = (int) ev.getX();
            int y = (int) ev.getY();
            if (this.mMoreFunRect.contains(x, y - this.mRootScrollY)) {
                this.mClickTarget = this.mMoreFunction;
                return false;
            }
            return false;
        }
        return false;
    }

    @Override // com.android.internal.widget.DecorCaptionView, android.view.View.OnTouchListener
    public boolean onTouch(View v, MotionEvent e) {
        int x = (int) e.getX();
        int y = (int) e.getY();
        int rawX = (int) e.getRawX();
        int rawY = (int) e.getRawY();
        boolean fromMouse = e.getToolType(e.getActionIndex()) == 3;
        boolean primaryButton = (e.getButtonState() & 1) != 0;
        int actionMasked = e.getActionMasked();
        switch (actionMasked) {
            case 0:
                if (!this.mShow) {
                    return false;
                }
                this.mCheckForDragging = false;
                if (!fromMouse || primaryButton) {
                    this.mHasMove = false;
                    if (v == this.mMoreFunction) {
                        this.mTouchDownX = v.getLeft() + x;
                        this.mTouchDownY = v.getTop() + y;
                        this.mTouchDownRawX = rawX;
                        this.mTouchDownRawY = rawY;
                        this.mCheckForDragging = true;
                        break;
                    } else if (v == this.mCoverView && this.mMultiWindowActive && this.mClickTarget == null) {
                        this.mCoverActiveMoreFun.setVisibility(8);
                        this.mCheckForDragging = false;
                        break;
                    }
                }
                break;
            case 1:
            case 3:
                if (v == this.mMoreFunction && actionMasked == 1 && !this.mHasMove) {
                    if (this.mCoverActiveMoreFun.getVisibility() == 0) {
                        this.mCoverActiveMoreFun.setVisibility(8);
                        break;
                    } else {
                        View view = this.mCoverActivieMoreFunBubble;
                        if (view != null) {
                            if (this.isHor) {
                                view.setBackground(this.mContext.getResources().getDrawable(R.drawable.zzzz_tran_decor_morefunction_bg_landscapeV3));
                            } else {
                                view.setBackground(this.mContext.getResources().getDrawable(R.drawable.zzzz_tran_decor_morefunction_button_bgV3));
                            }
                        }
                        ImageView imageView = this.mMoreFunctionChangeMaxImageView;
                        if (imageView != null) {
                            imageView.setImageDrawable(this.mContext.getResources().getDrawable(R.drawable.zzzz_tran_decor_morefunction_maximize_button_selectorV3));
                        }
                        ImageView imageView2 = this.mMoreFunctionChangeSmallImageView;
                        if (imageView2 != null) {
                            imageView2.setImageDrawable(this.mContext.getResources().getDrawable(R.drawable.zzzz_tran_decor_morefunction_minimize_button_selectorV3));
                        }
                        boolean muteState = this.mActivityTaskManager.getMuteState();
                        this.mMuteState = muteState;
                        if (muteState) {
                            this.mActiveMuteImageView.setImageDrawable(this.mContext.getResources().getDrawable(R.drawable.zzzz_tran_decor_morefunction_unmute_button_selectorV3));
                        } else {
                            this.mActiveMuteImageView.setImageDrawable(this.mContext.getResources().getDrawable(R.drawable.zzzz_tran_decor_morefunction_mute_button_selectorV3));
                        }
                        this.mCoverActiveMoreFun.setVisibility(0);
                        this.mCoverActiveMoreFun.bringToFront();
                        break;
                    }
                }
                break;
            case 2:
                if (v == this.mMoreFunction && ((Math.abs(rawX - this.mTouchDownRawX) >= 15 || Math.abs(rawY - this.mTouchDownRawY) >= 15) && !this.mHasMove)) {
                    this.mHasMove = true;
                    this.mActivityTaskManager.hookActiveMultiWindowMoveStartV3();
                    break;
                }
                break;
        }
        return this.mCheckForDragging || !this.mMultiWindowActive;
    }

    @Override // com.android.internal.widget.DecorCaptionView, android.view.ViewGroup
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    private boolean passedSlop(int x, int y) {
        return Math.abs(x - this.mTouchDownX) > this.mDragSlop || Math.abs(y - this.mTouchDownY) > this.mDragSlop;
    }

    @Override // com.android.internal.widget.DecorCaptionView
    public void onConfigurationChanged(boolean show) {
        this.mShow = show;
    }

    @Override // com.android.internal.widget.DecorCaptionView, android.view.ViewGroup
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (!(params instanceof ViewGroup.MarginLayoutParams)) {
            throw new IllegalArgumentException("params " + params + " must subclass MarginLayoutParams");
        }
        if (index > 4 || getChildCount() > 4) {
            throw new IllegalStateException("TranMultiDecorCaptionView can only handle 1 client view");
        }
        super.addView(child, 0, params);
        this.mContent = child;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.internal.widget.DecorCaptionView, android.view.View
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int captionHeight;
        boolean z;
        if (this.mTopCaption.getVisibility() != 8) {
            measureChildWithMargins(this.mTopCaption, widthMeasureSpec, 0, heightMeasureSpec, 0);
            captionHeight = this.mTopCaption.getMeasuredHeight();
        } else {
            captionHeight = 0;
        }
        boolean z2 = false;
        if (this.mContent != null) {
            int value = (!this.needPushDown || this.isHor) ? 0 : captionHeight;
            if (this.VIEW_DEBUG) {
                Log.d("view_debug", " onMeasure value=" + value + " needPushDown=" + this.needPushDown + " isHor=" + this.isHor);
            }
            measureChildWithMargins(this.mContent, widthMeasureSpec, 0, heightMeasureSpec, value);
        }
        View view = this.mCoverView;
        if (view != null) {
            measureChildWithMargins(view, widthMeasureSpec, 0, heightMeasureSpec, 0);
        }
        setMeasuredDimension(View.MeasureSpec.getSize(widthMeasureSpec), View.MeasureSpec.getSize(heightMeasureSpec));
        WindowManager.LayoutParams lp = this.mOwner.getAttributes();
        boolean ownerIsFullScreen = (lp.flags & 1024) == 1024;
        boolean mSystemUiVisibilityIsFullScreen = (this.mOwner.getDecorView().getSystemUiVisibility() & 1024) == 1024;
        boolean statusBarIsLight = (this.mOwner.getDecorView().getSystemUiVisibility() & 8192) == 8192;
        boolean isLayoutStable = (this.mOwner.getDecorView().getSystemUiVisibility() & 256) == 256;
        if (ownerIsFullScreen) {
            z = true;
        } else {
            z = mSystemUiVisibilityIsFullScreen;
        }
        this.needPushDown = z;
        boolean z3 = (z && statusBarIsLight) ? statusBarIsLight : false;
        this.needPushDown = z3;
        if (z3 && !isLayoutStable) {
            z2 = true;
        }
        this.needPushDown = z2;
        if (this.VIEW_DEBUG) {
            Log.d("view_debug", " entring setPhoneWindow : mOwner=" + this.mOwner + " " + Log.getStackTraceString(new Throwable()));
            Log.d("view_debug", " needPushDown :" + this.needPushDown + " lp.flags:0x" + Integer.toHexString(lp.flags) + "  WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS:0x" + Integer.toHexString(512) + " featureId=");
            Log.d("view_debug", " mSystemUiVisibility:0x" + Integer.toHexString(this.mOwner.getDecorView().getSystemUiVisibility()) + " ownerIsFullScreen=" + ownerIsFullScreen + " mSystemUiVisibilityIsFullScreen:" + mSystemUiVisibilityIsFullScreen + " statusBarIsLight=" + statusBarIsLight + " isLayoutStable=" + isLayoutStable);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.internal.widget.DecorCaptionView, android.view.ViewGroup, android.view.View
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int captionHeight;
        if (this.mTopCaption.getVisibility() != 8) {
            View view = this.mTopCaption;
            view.layout(0, 0, view.getMeasuredWidth(), this.mTopCaption.getMeasuredHeight());
            captionHeight = this.mTopCaption.getBottom() - this.mTopCaption.getTop();
            this.mCaptionHeight = this.mTopCaption.getMeasuredHeight();
            this.mMoreFunction.getHitRect(this.mMoreFunRect);
        } else {
            captionHeight = 0;
            this.mMoreFunRect.setEmpty();
        }
        if (this.mContent != null) {
            int value = (!this.needPushDown || this.isHor) ? 0 : captionHeight;
            if (this.VIEW_DEBUG) {
                Log.d("view_debug", " onLayoout hhhhh value=" + value + " needPushDown=" + this.needPushDown + " isHor=" + this.isHor);
            }
            View view2 = this.mContent;
            view2.layout(0, value, view2.getMeasuredWidth(), this.mContent.getMeasuredHeight() + value);
        }
        View view3 = this.mCoverView;
        if (view3 != null) {
            view3.layout(0, 0, view3.getMeasuredWidth(), this.mCoverView.getMeasuredHeight());
        }
        this.mWidth = getMeasuredWidth();
        this.mHeight = getMeasuredHeight();
        ((DecorView) this.mOwner.getDecorView()).notifyCaptionHeightChanged();
        this.mOwner.notifyRestrictedCaptionAreaCallback(this.mMaximize.getLeft(), this.mMaximize.getTop(), this.mClose.getRight(), this.mClose.getBottom());
        if (this.VIEW_DEBUG) {
            Log.e("view_debug", "onLayout: " + getChildCount() + " " + Log.getStackTraceString(new Throwable()));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes4.dex */
    public class CaptionViewClickListener implements View.OnClickListener {
        private CaptionViewClickListener() {
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View v) {
            if (v.equals(TranMultiDecorCaptionViewV3.this.mMaximize) || v.equals(TranMultiDecorCaptionViewV3.this.mActiveChangeMax)) {
                TranMultiDecorCaptionViewV3.this.mActivityTaskManager.hookMultiWindowToMaxV3(TranMultiDecorCaptionViewV3.this.getWindow());
            } else if (v.equals(TranMultiDecorCaptionViewV3.this.mClose)) {
                TranMultiDecorCaptionViewV3.this.mActivityTaskManager.hookMultiWindowToCloseV3(TranMultiDecorCaptionViewV3.this.getWindow());
            } else if (v.equals(TranMultiDecorCaptionViewV3.this.mActiveChangeSmall)) {
                TranMultiDecorCaptionViewV3.this.mActivityTaskManager.hookMultiWindowToSmallV3(TranMultiDecorCaptionViewV3.this.getWindow());
            } else if (v.equals(TranMultiDecorCaptionViewV3.this.mActiveMute)) {
                TranMultiDecorCaptionViewV3 tranMultiDecorCaptionViewV3 = TranMultiDecorCaptionViewV3.this;
                tranMultiDecorCaptionViewV3.mMuteState = tranMultiDecorCaptionViewV3.mActivityTaskManager.getMuteState();
                TranMultiDecorCaptionViewV3.this.mActivityTaskManager.setMuteState(!TranMultiDecorCaptionViewV3.this.mMuteState);
                TranMultiDecorCaptionViewV3.this.mCoverActiveMoreFun.setVisibility(8);
            }
        }
    }

    @Override // com.android.internal.widget.DecorCaptionView
    public void updateConfiguration(Configuration config) {
        View view;
        if (!isAttachedToWindow()) {
            Log.i("TranMultiWindow", "DecorCaptionView not attach, dont update config");
            return;
        }
        WindowConfiguration windowConfiguration = this.mWindowConfig;
        if (windowConfiguration != null && config != null && windowConfiguration.diff(config.windowConfiguration, true) == 0 && (view = this.mTopCaption) != null && view.getVisibility() == 0) {
            return;
        }
        this.mWindowConfig.updateFrom(config.windowConfiguration);
        if (config.windowConfiguration.getMultiWindowingMode() == 2 || config.windowConfiguration.getMultiWindowingMode() == 6) {
            this.mTopCaption.setVisibility(0);
            updateCationChild(0);
            if (config.windowConfiguration.getMultiWindowingMode() == 2) {
                this.mMaximize.setVisibility(0);
                this.mActiveChangeMax.setVisibility(8);
            } else {
                this.mActiveChangeMax.setVisibility(0);
                this.mMaximize.setVisibility(4);
            }
            this.mCoverTouchLine.setVisibility(8);
            this.mCoverTouchMax.setVisibility(8);
            this.mCoverIcon.setVisibility(8);
            this.mMultiWindowActive = true;
        } else if (config.windowConfiguration.getMultiWindowingMode() == 3 || config.windowConfiguration.getMultiWindowingMode() == 7) {
            this.mTopCaption.setVisibility(0);
            updateCationChild(0);
            if (config.windowConfiguration.getMultiWindowingMode() == 3) {
                this.mMaximize.setVisibility(0);
                this.mActiveChangeMax.setVisibility(8);
            } else {
                this.mActiveChangeMax.setVisibility(0);
                this.mMaximize.setVisibility(4);
            }
            this.mCoverTouchLine.setVisibility(0);
            this.mCoverTouchMax.setVisibility(8);
            this.mCoverIcon.setVisibility(8);
            this.mMultiWindowActive = true;
        } else if (config.windowConfiguration.getMultiWindowingMode() == 5 || config.windowConfiguration.getMultiWindowingMode() == 9) {
            this.mTopCaption.setVisibility(0);
            updateCationChild(4);
            this.mMaximize.setVisibility(4);
            this.mCoverTouchLine.setVisibility(0);
            this.mCoverTouchMax.setVisibility(0);
            this.mCoverIcon.setVisibility(8);
            this.mMultiWindowActive = false;
        } else if (config.windowConfiguration.getMultiWindowingMode() == 4 || config.windowConfiguration.getMultiWindowingMode() == 8) {
            this.mTopCaption.setVisibility(0);
            updateCationChild(4);
            this.mMaximize.setVisibility(4);
            this.mCoverTouchLine.setVisibility(8);
            this.mCoverTouchMax.setVisibility(8);
            this.mCoverIcon.setVisibility(8);
            this.mMultiWindowActive = false;
        } else if (config.windowConfiguration.getMultiWindowingMode() == 10) {
            updateCationChild(4);
            this.mMaximize.setVisibility(4);
            this.mCoverTouchLine.setVisibility(8);
            this.mCoverTouchMax.setVisibility(8);
            this.mCoverIcon.setVisibility(0);
            try {
                ApplicationInfo info = this.mPackageManager.getApplicationInfo(this.mContext.getPackageName(), 0);
                if (info != null) {
                    this.mCoverIcon.setBackground(info.loadIcon(this.mPackageManager));
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (config != null && config.windowConfiguration.isThunderbackWindow()) {
            Rect appRect = config.windowConfiguration.getBounds();
            this.isHor = appRect.width() > appRect.height();
            View view2 = this.mCoverActiveMoreFun;
            if (view2 != null) {
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) view2.getLayoutParams();
                if (this.isHor) {
                    layoutParams.width = dip2px(320.0f);
                } else {
                    layoutParams.width = dip2px(240.0f);
                }
                this.mCoverActiveMoreFun.setLayoutParams(layoutParams);
            }
            View view3 = this.mTopCaption;
            if (view3 != null) {
                if (this.isHor) {
                    view3.setBackgroundColor(0);
                } else {
                    view3.setBackgroundColor(this.mSetStatusBarColor);
                }
            }
        }
        updateCaptionVisibility();
    }

    @Override // com.android.internal.widget.DecorCaptionView
    public void setStatusBarColor(int color) {
        this.mSetStatusBarColor = color;
        if (this.mTopCaption != null) {
            setMoreFunctionButtonBackground();
            if (this.isHor) {
                this.mTopCaption.setBackgroundColor(0);
            } else {
                this.mTopCaption.setBackgroundColor(this.mSetStatusBarColor);
            }
        }
    }

    private void updateCationChild(int visibility) {
        this.mClose.setVisibility(visibility);
        this.mMoreFunction.setVisibility(visibility);
        this.mCoverActiveMoreFun.setVisibility(8);
    }

    private void updateCaptionVisibility() {
        this.mMaximize.setOnClickListener(this.mCaptionViewClickListener);
        this.mClose.setOnClickListener(this.mCaptionViewClickListener);
        this.mActiveChangeMax.setOnClickListener(this.mCaptionViewClickListener);
        this.mActiveChangeSmall.setOnClickListener(this.mCaptionViewClickListener);
        this.mActiveMute.setOnClickListener(this.mCaptionViewClickListener);
        this.mMoreFunction.setOnTouchListener(this);
        this.mCoverView.setOnTouchListener(this);
        setMoreFunctionButtonBackground();
    }

    @Override // com.android.internal.widget.DecorCaptionView
    public boolean isCaptionShowing() {
        return this.mShow;
    }

    @Override // com.android.internal.widget.DecorCaptionView
    public int getCaptionHeight() {
        return this.mCaptionHeight;
    }

    @Override // com.android.internal.widget.DecorCaptionView
    public void removeContentView() {
        View view = this.mContent;
        if (view != null && !this.mCoverView.equals(view)) {
            removeView(this.mContent);
            this.mContent = null;
        }
    }

    @Override // com.android.internal.widget.DecorCaptionView
    public View getCaption() {
        return this.mTopCaption;
    }

    @Override // com.android.internal.widget.DecorCaptionView, android.view.ViewGroup
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new ViewGroup.MarginLayoutParams(getContext(), attrs);
    }

    @Override // com.android.internal.widget.DecorCaptionView, android.view.ViewGroup
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new ViewGroup.MarginLayoutParams(-1, -1);
    }

    @Override // com.android.internal.widget.DecorCaptionView, android.view.ViewGroup
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new ViewGroup.MarginLayoutParams(p);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.internal.widget.DecorCaptionView, android.view.ViewGroup
    public boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof ViewGroup.MarginLayoutParams;
    }

    @Override // com.android.internal.widget.DecorCaptionView, android.view.GestureDetector.OnGestureListener
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override // com.android.internal.widget.DecorCaptionView, android.view.GestureDetector.OnGestureListener
    public void onShowPress(MotionEvent e) {
    }

    @Override // com.android.internal.widget.DecorCaptionView, android.view.GestureDetector.OnGestureListener
    public boolean onSingleTapUp(MotionEvent e) {
        return true;
    }

    @Override // com.android.internal.widget.DecorCaptionView, android.view.GestureDetector.OnGestureListener
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override // com.android.internal.widget.DecorCaptionView, android.view.GestureDetector.OnGestureListener
    public void onLongPress(MotionEvent e) {
    }

    @Override // com.android.internal.widget.DecorCaptionView, android.view.GestureDetector.OnGestureListener
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override // com.android.internal.widget.DecorCaptionView
    public void onRootViewScrollYChanged(int scrollY) {
        View view = this.mTopCaption;
        if (view != null) {
            this.mRootScrollY = scrollY;
            view.setTranslationY(scrollY);
        }
    }

    public void initCaptionAnimation() {
    }

    public void setMoreFunctionButtonBackground() {
        int statusBarColor = this.mOwner.getStatusBarColor();
        this.mStatusBarColor = statusBarColor;
        int a = Color.alpha(statusBarColor);
        int r = Color.red(this.mStatusBarColor);
        int g = Color.green(this.mStatusBarColor);
        int b = Color.blue(this.mStatusBarColor);
        if (this.VIEW_DEBUG) {
            Log.e("view_debug", "a=" + a + " r= " + r + " g=" + g + " b=" + b + " mStatusBarColor=" + this.mStatusBarColor);
        }
        if (a == 0 && !this.mContext.getPackageName().contains("com.gallery20")) {
            this.mMoreFunctionButton.setImageDrawable(this.mContext.getResources().getDrawable(R.drawable.zzzz_tran_decor_morefunction_button_selectorV3));
        } else if (r + g + b < 500 && a > 100) {
            this.mMoreFunctionButton.setImageDrawable(this.mContext.getResources().getDrawable(R.drawable.zzzz_tran_decor_morefunction_button_selector_darkV3));
        } else {
            this.mMoreFunctionButton.setImageDrawable(this.mContext.getResources().getDrawable(R.drawable.zzzz_tran_decor_morefunction_button_selector_lightV3));
        }
    }

    public int dip2px(float dpValue) {
        float scale = this.mContext.getResources().getDisplayMetrics().density;
        return (int) ((dpValue * scale) + 0.5f);
    }
}
