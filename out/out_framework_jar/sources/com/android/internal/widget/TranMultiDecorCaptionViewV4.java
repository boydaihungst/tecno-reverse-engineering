package com.android.internal.widget;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.ActivityTaskManager;
import android.app.WindowConfiguration;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
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
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.PathInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.android.internal.R;
import com.android.internal.policy.DecorView;
import com.android.internal.policy.PhoneWindow;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/* loaded from: classes4.dex */
public class TranMultiDecorCaptionViewV4 extends DecorCaptionView {
    private static final int INACTIVE_MULTI_WINDOW_LONGCLICK = 10;
    private static final int INACTIVE_MULTI_WINDOW_LONGCLICK_TIME = 1500;
    private static final int MORE_FUNCTION_LONG_PRESS_TIME = 1000;
    private static final String TAG = "TranMultiDecorCaptionView";
    public static final String TAG_MULTI = "TranMultiWindow";
    private static final int TOUCH_TIMEOUT = 500;
    private static final int TRAN_COVER_LINE_VIEW = 0;
    private static final int TRAN_COVER_TOUCH_MAX = 1;
    private static final int TRAN_TOP_CAPTION_VIEW = 2;
    private boolean VIEW_DEBUG;
    private View mActiveChangeMax;
    private View mActiveChangeSmall;
    private View mActiveChangeSplit;
    private View mActiveMute;
    private ImageView mActiveMuteImageView;
    private ActivityTaskManager mActivityTaskManager;
    private AnimatorSet mBubbleAnimator;
    private int mCaptionHeight;
    private CaptionViewClickListener mCaptionViewClickListener;
    private boolean mCheckForDragging;
    private View mClickTarget;
    private View mClose;
    private View mCloseIcon;
    private Configuration mConfig;
    private View mContent;
    private Context mContext;
    private View mCoverActivieMoreFunBubble;
    private View mCoverNightLine;
    private View mCoverTouchLine;
    private View mCoverView;
    private long mCurrentTime;
    private boolean mDownInCover;
    private int mDragSlop;
    private boolean mDragging;
    private boolean mFocusOn;
    private GestureDetector mGestureDetector;
    private Handler mHandler;
    private boolean mHasMove;
    private int mHeight;
    private boolean mIgnore;
    private View mInActiveClose;
    private View mInActiveCloseHor;
    private View mInActiveMute;
    private View mInActiveMuteHor;
    private int mInLargeScreen;
    private boolean mIsActiveChangeMaxVisible;
    private boolean mIsActiveChangeSmallVisible;
    private boolean mIsActiveChangeSplitVisible;
    private boolean mIsActiveMuteVisible;
    private boolean mIsCoverViewVisible;
    private boolean mIsNightMode;
    private View mMaximize;
    private View mMaximizeIcon;
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
    private ImageView mSplitImageView;
    private int mStatusBarColor;
    private IBinder mToken;
    private View mTopCaption;
    private View mTopCaptionBg;
    private ArrayList<View> mTouchDispatchList;
    private int mTouchDownRawX;
    private int mTouchDownRawY;
    private int mTouchDownX;
    private int mTouchDownY;
    private int mWidth;
    private WindowConfiguration mWindowConfig;
    private static final int DEFAULT_NIGHT_MODE_STATUS_COLOR = Color.parseColor("#000000");
    private static final int DEFAULT_HIOS_LINGHT_MODE_STATUS_COLOR = Color.parseColor("#F7F7F7");
    private static final int DEFAULT_XOS_LINGHT_MODE_STATUS_COLOR = Color.parseColor("#FDFDFD");
    private static Set<String> mDontNeedBoundSet = new HashSet<String>() { // from class: com.android.internal.widget.TranMultiDecorCaptionViewV4.1
        {
            add("VoipActivityV2");
            add("NotesList");
            add("FileSendActivity");
        }
    };

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

    public TranMultiDecorCaptionViewV4(Context context) {
        super(context);
        this.mCurrentTime = 0L;
        this.mOwner = null;
        this.mShow = false;
        this.mDragging = false;
        this.mOverlayWithAppContent = false;
        this.mIsActiveMuteVisible = true;
        this.mMoreFunRect = new Rect();
        this.mTouchDispatchList = new ArrayList<>(3);
        this.mMultiWindowActive = false;
        this.mConfig = new Configuration();
        this.mWindowConfig = new WindowConfiguration();
        this.VIEW_DEBUG = SystemProperties.getBoolean("sys.tran.debug", false);
        this.mSetStatusBarColor = -1;
        this.mToken = new Binder();
        init(context);
    }

    public TranMultiDecorCaptionViewV4(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mCurrentTime = 0L;
        this.mOwner = null;
        this.mShow = false;
        this.mDragging = false;
        this.mOverlayWithAppContent = false;
        this.mIsActiveMuteVisible = true;
        this.mMoreFunRect = new Rect();
        this.mTouchDispatchList = new ArrayList<>(3);
        this.mMultiWindowActive = false;
        this.mConfig = new Configuration();
        this.mWindowConfig = new WindowConfiguration();
        this.VIEW_DEBUG = SystemProperties.getBoolean("sys.tran.debug", false);
        this.mSetStatusBarColor = -1;
        this.mToken = new Binder();
        init(context);
    }

    public TranMultiDecorCaptionViewV4(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mCurrentTime = 0L;
        this.mOwner = null;
        this.mShow = false;
        this.mDragging = false;
        this.mOverlayWithAppContent = false;
        this.mIsActiveMuteVisible = true;
        this.mMoreFunRect = new Rect();
        this.mTouchDispatchList = new ArrayList<>(3);
        this.mMultiWindowActive = false;
        this.mConfig = new Configuration();
        this.mWindowConfig = new WindowConfiguration();
        this.VIEW_DEBUG = SystemProperties.getBoolean("sys.tran.debug", false);
        this.mSetStatusBarColor = -1;
        this.mToken = new Binder();
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

    @Override // com.android.internal.widget.DecorCaptionView, android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mTopCaption = findViewById(R.id.zzzz_top_caption);
        this.mTopCaptionBg = findViewById(R.id.zzzz_top_caption_bg);
        View findViewById = findViewById(R.id.zzzz_cover_layout);
        this.mCoverView = findViewById;
        this.mCoverTouchLine = findViewById.findViewById(R.id.zzzz_cover_touch_line);
        this.mCoverNightLine = this.mCoverView.findViewById(R.id.zzzz_night_show_line);
        this.mCoverActivieMoreFunBubble = this.mCoverView.findViewById(R.id.zzzz_active_more_function_bubble);
        this.mMaximize = this.mTopCaption.findViewById(R.id.zzzz_maximize_window);
        this.mMaximizeIcon = this.mTopCaption.findViewById(R.id.zzzz_maximize_window_icon);
        this.mClose = this.mTopCaption.findViewById(R.id.zzzz_close_window);
        this.mCloseIcon = this.mTopCaption.findViewById(R.id.zzzz_close_window_icon);
        this.mMoreFunction = this.mTopCaption.findViewById(R.id.zzzz_more_function);
        this.mActiveChangeMax = this.mCoverView.findViewById(R.id.zzzz_active_change_max);
        this.mActiveChangeSmall = this.mCoverView.findViewById(R.id.zzzz_active_change_small);
        this.mActiveChangeSplit = this.mCoverView.findViewById(R.id.zzzz_active_split);
        this.mActiveMute = this.mCoverView.findViewById(R.id.zzzz_active_mute);
        this.mMoreFunctionButton = (ImageView) this.mTopCaption.findViewById(R.id.zzzz_more_function_button);
        this.mActiveMuteImageView = (ImageView) this.mCoverView.findViewById(R.id.zzzz_active_mute_imageview);
        this.mMoreFunctionChangeMaxImageView = (ImageView) this.mCoverView.findViewById(R.id.zzzz_active_change_max_imageview);
        this.mMoreFunctionChangeSmallImageView = (ImageView) this.mCoverView.findViewById(R.id.zzzz_active_change_small_imageview);
        this.mSplitImageView = (ImageView) findViewById(R.id.zzzz_active_split_imageview);
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
        boolean z;
        boolean z2;
        int actionMasked;
        int multiWindowMode;
        int x = (int) e.getX();
        int y = (int) e.getY();
        int rawX = (int) e.getRawX();
        int rawY = (int) e.getRawY();
        boolean fromMouse = e.getToolType(e.getActionIndex()) == 3;
        boolean primaryButton = (e.getButtonState() & 1) != 0;
        int actionMasked2 = e.getActionMasked();
        long currentTime = System.currentTimeMillis();
        switch (actionMasked2) {
            case 0:
                Log.i(TAG, "TranMultiDecorCaptionView onTouch ACTION_DOWN view: " + v + " mMultiWindowActive: " + this.mMultiWindowActive + " mShow: " + this.mShow + " fromMouse: " + fromMouse + " primaryButton: " + primaryButton + " mWindowConfig: " + this.mWindowConfig);
                int childCount = getChildCount();
                if (childCount >= 2) {
                    View childAt = getChildAt(childCount - 2);
                    View view = this.mTopCaption;
                    if (childAt != view) {
                        view.bringToFront();
                    }
                    View childAt2 = getChildAt(childCount - 1);
                    View view2 = this.mCoverView;
                    if (childAt2 != view2) {
                        view2.bringToFront();
                    }
                }
                if (this.mShow) {
                    this.mIgnore = false;
                    this.mCheckForDragging = false;
                    if (fromMouse && !primaryButton) {
                        z = true;
                        z2 = false;
                        break;
                    } else {
                        this.mHasMove = false;
                        if (v == this.mMoreFunction) {
                            long j = this.mCurrentTime;
                            if (currentTime - j < 500 && j != 0) {
                                this.mIgnore = true;
                                z = true;
                                z2 = false;
                                break;
                            } else {
                                this.mCurrentTime = currentTime;
                                this.mTouchDownX = x + v.getLeft();
                                this.mTouchDownY = y + v.getTop();
                                this.mTouchDownRawX = rawX;
                                this.mTouchDownRawY = rawY;
                                z = true;
                                this.mCheckForDragging = true;
                                this.mActivityTaskManager.hookActiveMultiWindowMoveStartV4(this.mWindowConfig.getMultiWindowingMode(), this.mWindowConfig.getMultiWindowingId());
                                z2 = false;
                                break;
                            }
                        } else {
                            z = true;
                            if (v != this.mCoverView) {
                                z2 = false;
                                break;
                            } else if (!this.mMultiWindowActive || this.mClickTarget != null) {
                                z2 = false;
                                break;
                            } else {
                                z2 = false;
                                startBubbleAnimation(false);
                                this.mCheckForDragging = false;
                                break;
                            }
                        }
                    }
                } else {
                    return false;
                }
                break;
            case 1:
            case 3:
                if (v != this.mMoreFunction) {
                    z = true;
                    z2 = false;
                    break;
                } else if (!this.mIgnore) {
                    if (actionMasked2 == 1 && !this.mHasMove) {
                        if (this.mCoverActivieMoreFunBubble.getVisibility() == 0) {
                            startBubbleAnimation(false);
                            z2 = false;
                            z = true;
                            break;
                        } else {
                            boolean muteStateV4 = this.mActivityTaskManager.getMuteStateV4(this.mWindowConfig.getMultiWindowingId());
                            this.mMuteState = muteStateV4;
                            if (muteStateV4) {
                                if (this.mIsNightMode) {
                                    this.mActiveMuteImageView.setImageDrawable(this.mContext.getResources().getDrawable(R.drawable.zzzz_tran_decor_morefunction_unmute_button_selector_darkV4));
                                } else {
                                    this.mActiveMuteImageView.setImageDrawable(this.mContext.getResources().getDrawable(R.drawable.zzzz_tran_decor_morefunction_unmute_button_selector_lightV4));
                                }
                            } else if (this.mIsNightMode) {
                                this.mActiveMuteImageView.setImageDrawable(this.mContext.getResources().getDrawable(R.drawable.zzzz_tran_decor_morefunction_mute_button_selector_darkV4));
                            } else {
                                this.mActiveMuteImageView.setImageDrawable(this.mContext.getResources().getDrawable(R.drawable.zzzz_tran_decor_morefunction_mute_button_selector_lightV4));
                            }
                            startBubbleAnimation(true);
                            this.mCoverActivieMoreFunBubble.bringToFront();
                            z = true;
                            z2 = false;
                            break;
                        }
                    } else if (!this.mHasMove) {
                        z = true;
                        z2 = false;
                        break;
                    } else {
                        int multiWindowMode2 = this.mWindowConfig.getMultiWindowingMode();
                        if ((98304 & multiWindowMode2) == 0) {
                            z = true;
                            z2 = false;
                            break;
                        } else {
                            z2 = false;
                            this.mActivityTaskManager.hookActiveMultiWindowEndMove(multiWindowMode2, 0, e);
                            z = true;
                            break;
                        }
                    }
                } else {
                    z = true;
                    z2 = false;
                    break;
                }
                break;
            case 2:
                if (v != this.mMoreFunction) {
                    z = true;
                    z2 = false;
                    break;
                } else if (!this.mIgnore) {
                    int multiWindowMode3 = this.mWindowConfig.getMultiWindowingMode();
                    if ((98304 & multiWindowMode3) == 0) {
                        actionMasked = actionMasked2;
                        if ((Math.abs(rawX - this.mTouchDownRawX) >= 5 || Math.abs(rawY - this.mTouchDownRawY) >= 5) && !this.mHasMove) {
                            this.mHasMove = true;
                        }
                    } else {
                        if (Math.abs(rawX - this.mTouchDownRawX) > 5 || Math.abs(rawY - this.mTouchDownRawY) > 5) {
                            multiWindowMode = multiWindowMode3;
                            actionMasked = actionMasked2;
                            this.mCurrentTime = currentTime;
                        } else if (!this.mHasMove && currentTime - this.mCurrentTime > 1000) {
                            this.mHasMove = true;
                            multiWindowMode = multiWindowMode3;
                            actionMasked = actionMasked2;
                            this.mActivityTaskManager.hookActiveMultiWindowStartToMove(this.mToken, multiWindowMode, 0, e, new Point(this.mTouchDownRawX, this.mTouchDownRawY));
                        } else {
                            multiWindowMode = multiWindowMode3;
                            actionMasked = actionMasked2;
                        }
                        if (this.mHasMove) {
                            this.mActivityTaskManager.hookActiveMultiWindowMove(multiWindowMode, 0, e);
                        }
                    }
                    z = true;
                    z2 = false;
                    break;
                } else {
                    z = true;
                    z2 = false;
                    break;
                }
                break;
            default:
                z = true;
                z2 = false;
                break;
        }
        return (this.mCheckForDragging || !this.mMultiWindowActive) ? z : z2;
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
        if (index >= 2 || getChildCount() >= 2) {
            throw new IllegalStateException("TranMultiDecorCaptionView can only handle 1 client view");
        }
        Log.i(TAG, "addview child: " + child);
        super.addView(child, -1, params);
    }

    @Override // com.android.internal.widget.DecorCaptionView, android.view.View
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.mTopCaption.getVisibility() != 8) {
            measureChildWithMargins(this.mTopCaption, widthMeasureSpec, 0, heightMeasureSpec, 0);
            this.mTopCaption.getMeasuredHeight();
        }
        View view = this.mCoverView;
        if (view != null) {
            measureChildWithMargins(view, widthMeasureSpec, 0, heightMeasureSpec, 0);
        }
        setMeasuredDimension(View.MeasureSpec.getSize(widthMeasureSpec), View.MeasureSpec.getSize(heightMeasureSpec));
    }

    @Override // com.android.internal.widget.DecorCaptionView, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.mTopCaption.getVisibility() != 8) {
            View view = this.mTopCaption;
            view.layout(0, 0, view.getMeasuredWidth(), this.mTopCaption.getMeasuredHeight());
            int bottom2 = this.mTopCaption.getBottom() - this.mTopCaption.getTop();
            this.mCaptionHeight = this.mTopCaption.getMeasuredHeight();
            this.mMoreFunction.getHitRect(this.mMoreFunRect);
        } else {
            this.mMoreFunRect.setEmpty();
        }
        View view2 = this.mCoverView;
        if (view2 != null) {
            view2.layout(0, 0, view2.getMeasuredWidth(), this.mCoverView.getMeasuredHeight());
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
            Log.i("touch_debug", "TranMultiDecorCaptionView onClick view: " + v);
            if (v.equals(TranMultiDecorCaptionViewV4.this.mMaximize) || v.equals(TranMultiDecorCaptionViewV4.this.mActiveChangeMax)) {
                TranMultiDecorCaptionViewV4.this.mActivityTaskManager.hookReparentToDefaultDisplay(TranMultiDecorCaptionViewV4.this.mWindowConfig.getMultiWindowingMode(), TranMultiDecorCaptionViewV4.this.mWindowConfig.getMultiWindowingId());
            } else if (v.equals(TranMultiDecorCaptionViewV4.this.mClose)) {
                TranMultiDecorCaptionViewV4.this.closeInputMethod();
                TranMultiDecorCaptionViewV4.this.mActivityTaskManager.hookMultiWindowToCloseV4(TranMultiDecorCaptionViewV4.this.mWindowConfig.getMultiWindowingMode(), TranMultiDecorCaptionViewV4.this.mWindowConfig.getMultiWindowingId());
            } else if (v.equals(TranMultiDecorCaptionViewV4.this.mActiveChangeSmall)) {
                TranMultiDecorCaptionViewV4.this.mActivityTaskManager.hookMultiWindowToSmallV4(TranMultiDecorCaptionViewV4.this.mWindowConfig.getMultiWindowingMode(), TranMultiDecorCaptionViewV4.this.mWindowConfig.getMultiWindowingId());
            } else if (v.equals(TranMultiDecorCaptionViewV4.this.mActiveChangeSplit)) {
                TranMultiDecorCaptionViewV4.this.mActivityTaskManager.setMuteStateV4(false, TranMultiDecorCaptionViewV4.this.mWindowConfig.getMultiWindowingId());
                TranMultiDecorCaptionViewV4.this.mActivityTaskManager.hookMultiWindowToSplit(TranMultiDecorCaptionViewV4.this.mWindowConfig.getMultiWindowingMode(), TranMultiDecorCaptionViewV4.this.mWindowConfig.getMultiWindowingId());
                TranMultiDecorCaptionViewV4.this.startBubbleAnimation(false);
            } else if (v.equals(TranMultiDecorCaptionViewV4.this.mActiveMute)) {
                TranMultiDecorCaptionViewV4 tranMultiDecorCaptionViewV4 = TranMultiDecorCaptionViewV4.this;
                tranMultiDecorCaptionViewV4.mMuteState = tranMultiDecorCaptionViewV4.mActivityTaskManager.getMuteStateV4(TranMultiDecorCaptionViewV4.this.mWindowConfig.getMultiWindowingId());
                Log.e("mute-debug", "mWindowConfig.getMultiWindowingId() = " + TranMultiDecorCaptionViewV4.this.mWindowConfig.getMultiWindowingId());
                if (!TranMultiDecorCaptionViewV4.this.mMuteState) {
                    TranMultiDecorCaptionViewV4.this.mActivityTaskManager.hookMultiWindowMuteAethenV4(1);
                } else {
                    TranMultiDecorCaptionViewV4.this.mActivityTaskManager.hookMultiWindowMuteAethenV4(2);
                }
                TranMultiDecorCaptionViewV4.this.mActivityTaskManager.setMuteStateV4(true ^ TranMultiDecorCaptionViewV4.this.mMuteState, TranMultiDecorCaptionViewV4.this.mWindowConfig.getMultiWindowingId());
                TranMultiDecorCaptionViewV4.this.startBubbleAnimation(false);
            } else if (v.equals(TranMultiDecorCaptionViewV4.this.mCoverActivieMoreFunBubble)) {
                TranMultiDecorCaptionViewV4.this.startBubbleAnimation(false);
            }
        }
    }

    @Override // com.android.internal.widget.DecorCaptionView
    public Configuration getConfiguration() {
        return this.mConfig;
    }

    @Override // com.android.internal.widget.DecorCaptionView
    public void updateConfiguration(Configuration config) {
        Log.i("config_debug", "TranMultiDecorCaptionView updateConfiguration config: " + config);
        WindowConfiguration windowConfiguration = config.windowConfiguration;
        int multiWindowMode = windowConfiguration.getMultiWindowingMode();
        boolean showCaption = (windowConfiguration.getWindowResizable() & 4) != 0;
        int currentNightMode = config.uiMode & 48;
        boolean isNightMode = currentNightMode == 32;
        View view = this.mCoverNightLine;
        if (view != null && this.mIsNightMode != isNightMode) {
            if (showCaption && isNightMode) {
                view.setVisibility(0);
                this.mIsNightMode = isNightMode;
                GradientDrawable drawable = (GradientDrawable) this.mCoverNightLine.getBackground();
                if (this.mIsNightMode) {
                    if (drawable != null) {
                        drawable.setStroke(dip2px(2.0f), Color.argb(77, 161, 161, 161));
                    }
                } else if (drawable != null) {
                    drawable.setStroke(dip2px(2.0f), Color.argb(31, 161, 161, 161));
                }
            } else {
                view.setVisibility(8);
            }
        }
        if (this.mIsNightMode) {
            this.mMoreFunctionChangeMaxImageView.setImageDrawable(this.mContext.getResources().getDrawable(R.drawable.zzzz_tran_decor_morefunction_maximize_button_selector_darkV4));
            this.mMoreFunctionChangeSmallImageView.setImageDrawable(this.mContext.getResources().getDrawable(R.drawable.zzzz_tran_decor_morefunction_minimize_button_selector_darkV4));
            this.mSplitImageView.setImageDrawable(this.mContext.getResources().getDrawable(R.drawable.zzzz_tran_decor_morefunction_split_button_selector_darkV4));
        } else {
            this.mMoreFunctionChangeMaxImageView.setImageDrawable(this.mContext.getResources().getDrawable(R.drawable.zzzz_tran_decor_morefunction_maximize_button_selector_lightV4));
            this.mMoreFunctionChangeSmallImageView.setImageDrawable(this.mContext.getResources().getDrawable(R.drawable.zzzz_tran_decor_morefunction_minimize_button_selector_lightV4));
            this.mSplitImageView.setImageDrawable(this.mContext.getResources().getDrawable(R.drawable.zzzz_tran_decor_morefunction_split_button_selector_lightV4));
        }
        this.mInLargeScreen = windowConfiguration.getInLargeScreen();
        this.mConfig.updateFrom(config);
        this.mWindowConfig.updateFrom(windowConfiguration);
        Log.i(TAG, "TranMultiDecorCaptionView isMultiWindow: " + ((multiWindowMode & 2) != 0) + " showCaption: " + showCaption + " isActive: " + ((multiWindowMode & 8) != 0) + " isBig: " + ((multiWindowMode & 16) != 0));
        if ((98304 & multiWindowMode) != 0) {
            this.mTopCaption.setVisibility(0);
            this.mMaximize.setVisibility(8);
            this.mClose.setVisibility(8);
            this.mIsActiveChangeMaxVisible = false;
            this.mIsActiveChangeSmallVisible = false;
            this.mIsActiveChangeSplitVisible = false;
            startBubbleAnimation(false);
        } else if ((multiWindowMode & 2) != 0) {
            if (!showCaption) {
                if ((multiWindowMode & 8) != 0) {
                    this.mMultiWindowActive = true;
                } else {
                    this.mMultiWindowActive = false;
                }
            } else {
                if (this.mInLargeScreen == 0) {
                    this.mIsActiveChangeSplitVisible = true;
                } else {
                    this.mIsActiveChangeSplitVisible = false;
                }
                this.mTopCaption.setVisibility(0);
                this.mIsActiveChangeSmallVisible = true;
                if ((multiWindowMode & 16) != 0) {
                    updateCationChild(0);
                    if ((multiWindowMode & 4) != 0) {
                        if ((multiWindowMode & 8) != 0) {
                            this.mMultiWindowActive = true;
                            startShowClose();
                        } else {
                            this.mMultiWindowActive = false;
                            startHideClose();
                        }
                        this.mMaximize.setVisibility(8);
                        this.mIsActiveChangeMaxVisible = true;
                    } else {
                        if ((multiWindowMode & 8) != 0) {
                            this.mMultiWindowActive = true;
                            startShowClose();
                            startShowMax();
                        } else {
                            this.mMultiWindowActive = false;
                            startHideClose();
                            startHideMax();
                        }
                        this.mIsActiveChangeMaxVisible = false;
                    }
                } else {
                    this.mMultiWindowActive = false;
                    this.mMoreFunction.setVisibility(4);
                    this.mCoverActivieMoreFunBubble.setVisibility(8);
                    startHideClose();
                    startHideMax();
                }
                if ((multiWindowMode & 128) != 0) {
                    this.mCoverTouchLine.setVisibility(0);
                } else {
                    this.mCoverTouchLine.setVisibility(8);
                }
            }
        } else {
            this.mMultiWindowActive = true;
            this.mTopCaption.setVisibility(8);
            this.mCoverView.setVisibility(8);
        }
        WindowConfiguration windowConfiguration2 = this.mWindowConfig;
        if (windowConfiguration2 != null && (windowConfiguration2.isThunderbackWindow() || this.mWindowConfig.isSplitScreenWindow())) {
            this.mWindowConfig.getBounds();
            this.mWindowConfig.getMaxBounds();
            View view2 = this.mTopCaption;
            if (view2 != null) {
                view2.setBackgroundColor(calcluteBackgroundColor(this.mStatusBarColor));
            }
        }
        updateCaptionVisibility();
        if ((windowConfiguration.getWindowResizable() & 2) == 0) {
            this.mActiveChangeSplit.setClickable(false);
            if (this.mIsNightMode) {
                this.mSplitImageView.setImageResource(R.drawable.zzzz_tran_decor_morefunction_unsplit_button_selector_darkV4);
            } else {
                this.mSplitImageView.setImageResource(R.drawable.zzzz_tran_decor_morefunction_unsplit_button_selector_lightV4);
            }
        }
        GradientDrawable drawable2 = (GradientDrawable) this.mCoverTouchLine.getBackground();
        if (drawable2 != null) {
            if (isHIOS()) {
                drawable2.setStroke(dip2px(2.0f), Color.argb(255, 34, 120, 255));
            } else {
                drawable2.setStroke(dip2px(2.0f), Color.argb(255, 38, 199, 110));
            }
        }
    }

    @Override // com.android.internal.widget.DecorCaptionView
    public void setStatusBarColor(int color) {
        this.mSetStatusBarColor = color;
        if (this.mTopCaption != null) {
            setMoreFunctionButtonBackground();
            this.mTopCaption.setBackgroundColor(calcluteBackgroundColor(this.mStatusBarColor));
        }
    }

    private void updateCationChild(int visibility) {
        if (visibility == 0) {
            this.mMoreFunction.setVisibility(visibility);
        }
        startBubbleAnimation(false);
    }

    private void updateCaptionVisibility() {
        if (this.mMoreFunction == null) {
            Log.i(TAG, "updateCaptionVisibility mMoreFunction: " + this.mMoreFunction);
            return;
        }
        this.mMaximize.setOnClickListener(this.mCaptionViewClickListener);
        this.mClose.setOnClickListener(this.mCaptionViewClickListener);
        this.mActiveChangeMax.setOnClickListener(this.mCaptionViewClickListener);
        this.mActiveChangeSmall.setOnClickListener(this.mCaptionViewClickListener);
        this.mActiveChangeSplit.setOnClickListener(this.mCaptionViewClickListener);
        this.mActiveMute.setOnClickListener(this.mCaptionViewClickListener);
        this.mCoverActivieMoreFunBubble.setOnClickListener(this.mCaptionViewClickListener);
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

    @Override // com.android.internal.widget.DecorCaptionView, android.view.ViewGroup
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
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

    @Override // com.android.internal.widget.DecorCaptionView, android.view.View
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (this.mFocusOn == hasWindowFocus) {
            return;
        }
        updateCaptionVisibility();
        this.mFocusOn = hasWindowFocus;
        updateDrawableOnFocus();
    }

    private void updateDrawableOnFocus() {
        if (this.mFocusOn) {
            if (isHIOS()) {
                this.mMoreFunctionButton.setImageDrawable(this.mContext.getResources().getDrawable(R.drawable.zzzz_tran_decor_morefunction_button_focus_hiosV4));
                return;
            } else {
                this.mMoreFunctionButton.setImageDrawable(this.mContext.getResources().getDrawable(R.drawable.zzzz_tran_decor_morefunction_button_focus_xosV4));
                return;
            }
        }
        setMoreFunctionButtonBackground();
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
            this.mMoreFunctionButton.setImageDrawable(this.mContext.getResources().getDrawable(R.drawable.zzzz_tran_decor_morefunction_button_selectorV4));
        } else if (r + g + b < 500 && a > 100) {
            this.mMoreFunctionButton.setImageDrawable(this.mContext.getResources().getDrawable(R.drawable.zzzz_tran_decor_morefunction_button_selector_darkV4));
        } else {
            this.mMoreFunctionButton.setImageDrawable(this.mContext.getResources().getDrawable(R.drawable.zzzz_tran_decor_morefunction_button_selector_lightV4));
        }
        if (this.mFocusOn) {
            updateDrawableOnFocus();
        }
    }

    public int dip2px(float dpValue) {
        float scale = this.mContext.getResources().getDisplayMetrics().density;
        return (int) ((dpValue * scale) + 0.5f);
    }

    private void startShowClose() {
        if (this.mClose.getVisibility() == 0) {
            return;
        }
        Animation animationSet = AnimationUtils.loadAnimation(this.mContext, R.anim.multiwindow_change_to_activeV4);
        if (animationSet instanceof AnimationSet) {
            List<Animation> animationList = ((AnimationSet) animationSet).getAnimations();
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) this.mClose.getLayoutParams();
            int x = params.leftMargin;
            int y = params.topMargin;
            if (animationList != null) {
                for (int i = 0; i < animationList.size(); i++) {
                    Animation animation = animationList.get(i);
                    if (animation instanceof TranslateAnimation) {
                        TranslateAnimation translateAnim = (TranslateAnimation) animation;
                        translateAnim.setFromXDelta(x - 100);
                        translateAnim.setFromYDelta(y);
                        translateAnim.setToXDelta(x);
                        translateAnim.setToYDelta(y);
                    }
                }
            }
            int i2 = params.width;
            animationSet.initialize(i2, params.height, params.width, params.height);
        }
        this.mClose.setVisibility(0);
        this.mClose.startAnimation(animationSet);
    }

    private void startShowMax() {
        if (this.mMaximize.getVisibility() == 0) {
            return;
        }
        Animation animationSet = AnimationUtils.loadAnimation(this.mContext, R.anim.multiwindow_change_to_activeV4);
        if (animationSet instanceof AnimationSet) {
            List<Animation> animationList = ((AnimationSet) animationSet).getAnimations();
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) this.mMaximize.getLayoutParams();
            int x = params.leftMargin;
            int y = params.topMargin;
            if (animationList != null) {
                for (int i = 0; i < animationList.size(); i++) {
                    Animation animation = animationList.get(i);
                    if (animation instanceof TranslateAnimation) {
                        TranslateAnimation translateAnim = (TranslateAnimation) animation;
                        translateAnim.setFromXDelta(x + 100);
                        translateAnim.setFromYDelta(y);
                        translateAnim.setToXDelta(x);
                        translateAnim.setToYDelta(y);
                    }
                }
            }
            int i2 = params.width;
            animationSet.initialize(i2, params.height, params.width, params.height);
        }
        this.mMaximize.setVisibility(0);
        this.mMaximize.startAnimation(animationSet);
    }

    private void startHideClose() {
        if (this.mClose.getVisibility() == 4 || this.mClose.getVisibility() == 8) {
            return;
        }
        Animation animationSet = AnimationUtils.loadAnimation(this.mContext, R.anim.multiwindow_change_to_inactiveV4);
        if (animationSet instanceof AnimationSet) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) this.mClose.getLayoutParams();
            animationSet.setAnimationListener(new Animation.AnimationListener() { // from class: com.android.internal.widget.TranMultiDecorCaptionViewV4.2
                @Override // android.view.animation.Animation.AnimationListener
                public void onAnimationStart(Animation animation) {
                }

                @Override // android.view.animation.Animation.AnimationListener
                public void onAnimationEnd(Animation animation) {
                    if ((TranMultiDecorCaptionViewV4.this.mWindowConfig.getMultiWindowingMode() & 8) == 0) {
                        TranMultiDecorCaptionViewV4.this.mClose.setVisibility(4);
                    }
                }

                @Override // android.view.animation.Animation.AnimationListener
                public void onAnimationRepeat(Animation animation) {
                }
            });
            animationSet.initialize(params.width, params.height, params.width, params.height);
        }
        this.mClose.startAnimation(animationSet);
    }

    private void startHideMax() {
        if (this.mMaximize.getVisibility() == 4 || this.mMaximize.getVisibility() == 8) {
            return;
        }
        Animation animationSet = AnimationUtils.loadAnimation(this.mContext, R.anim.multiwindow_change_to_inactiveV4);
        if (animationSet instanceof AnimationSet) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) this.mMaximize.getLayoutParams();
            animationSet.setAnimationListener(new Animation.AnimationListener() { // from class: com.android.internal.widget.TranMultiDecorCaptionViewV4.3
                @Override // android.view.animation.Animation.AnimationListener
                public void onAnimationStart(Animation animation) {
                }

                @Override // android.view.animation.Animation.AnimationListener
                public void onAnimationEnd(Animation animation) {
                    if ((TranMultiDecorCaptionViewV4.this.mWindowConfig.getMultiWindowingMode() & 8) == 0) {
                        TranMultiDecorCaptionViewV4.this.mMaximize.setVisibility(4);
                    }
                }

                @Override // android.view.animation.Animation.AnimationListener
                public void onAnimationRepeat(Animation animation) {
                }
            });
            animationSet.initialize(params.width, params.height, params.width, params.height);
        }
        this.mMaximize.startAnimation(animationSet);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startBubbleAnimation(final boolean isShow) {
        ValueAnimator animatorMoreFunction;
        float y_start;
        float y_end;
        int paddingTop;
        int paddingBottom;
        int paddingLeft;
        int paddingRight;
        int iconMarginEnd;
        ValueAnimator animatorBubble;
        ValueAnimator animatorIcon;
        if (this.mIsCoverViewVisible == isShow) {
            return;
        }
        this.mIsCoverViewVisible = isShow;
        AnimatorSet animatorSet = this.mBubbleAnimator;
        if (animatorSet != null) {
            animatorSet.cancel();
        }
        this.mBubbleAnimator = new AnimatorSet();
        if (isShow) {
            ValueAnimator animatorMoreFunction2 = ValueAnimator.ofFloat(0.0f, 1.0f);
            animatorMoreFunction2.setStartDelay(0L);
            animatorMoreFunction = animatorMoreFunction2;
        } else {
            ValueAnimator animatorMoreFunction3 = ValueAnimator.ofFloat(1.0f, 0.0f);
            animatorMoreFunction3.setStartDelay(150L);
            animatorMoreFunction = animatorMoreFunction3;
        }
        animatorMoreFunction.setInterpolator(new PathInterpolator(0.2f, 0.0f, 0.1f, 1.0f));
        animatorMoreFunction.setDuration(100L);
        animatorMoreFunction.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.internal.widget.TranMultiDecorCaptionViewV4.4
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float value = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                float alpha = TranMultiDecorCaptionViewV4.this.getValueByProgress(value, 1.0f, 0.3f);
                float scale = TranMultiDecorCaptionViewV4.this.getValueByProgress(value, 1.0f, 1.3f);
                TranMultiDecorCaptionViewV4.this.mMoreFunction.setAlpha(alpha);
                TranMultiDecorCaptionViewV4.this.mMoreFunction.setScaleX(scale);
                TranMultiDecorCaptionViewV4.this.mMoreFunction.setScaleY(scale);
            }
        });
        animatorMoreFunction.addListener(new Animator.AnimatorListener() { // from class: com.android.internal.widget.TranMultiDecorCaptionViewV4.5
            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animation) {
                if (!isShow) {
                    TranMultiDecorCaptionViewV4.this.mMoreFunction.setVisibility(0);
                }
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animation) {
                if (isShow) {
                    TranMultiDecorCaptionViewV4.this.mMoreFunction.setVisibility(4);
                }
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationRepeat(Animator animation) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animation) {
            }
        });
        ViewGroup.LayoutParams params = this.mCoverActivieMoreFunBubble.getLayoutParams();
        final float x = this.mCoverActivieMoreFunBubble.getX();
        if (this.mInLargeScreen == 0) {
            float y_start2 = (this.mContext.getResources().getDimensionPixelSize(R.dimen.zzzz_tran_caption_morefun_large_margintopV4) - (this.mContext.getResources().getDimensionPixelSize(R.dimen.zzzz_tran_caption_morefun_large_heightV4) / 2)) - 11.0f;
            y_start = y_start2;
            y_end = this.mContext.getResources().getDimensionPixelSize(R.dimen.zzzz_tran_caption_morefun_large_margintopV4);
        } else {
            float y_start3 = (this.mContext.getResources().getDimensionPixelSize(R.dimen.zzzz_tran_caption_morefun_margintopV4) - (this.mContext.getResources().getDimensionPixelSize(R.dimen.zzzz_tran_caption_morefun_heightV4) / 2)) - 11.0f;
            y_start = y_start3;
            y_end = this.mContext.getResources().getDimensionPixelSize(R.dimen.zzzz_tran_caption_morefun_margintopV4);
        }
        if (this.mInLargeScreen == 0) {
            int paddingTop2 = this.mContext.getResources().getDimensionPixelSize(R.dimen.zzzz_tran_caption_morefun_large_paddingtopV4);
            int paddingLeft2 = this.mContext.getResources().getDimensionPixelSize(R.dimen.zzzz_tran_caption_morefun_large_paddingstartV4);
            int paddingRight2 = this.mContext.getResources().getDimensionPixelSize(R.dimen.zzzz_tran_caption_morefun_large_paddingendV4);
            int paddingBottom2 = (this.mContext.getResources().getDimensionPixelSize(R.dimen.zzzz_tran_caption_morefun_large_heightV4) - paddingTop2) - this.mContext.getResources().getDimensionPixelSize(R.dimen.zzzz_tran_multi_window_button_large_sizeV4);
            paddingBottom = paddingBottom2;
            paddingTop = paddingTop2;
            paddingLeft = paddingLeft2;
            paddingRight = paddingRight2;
            iconMarginEnd = this.mContext.getResources().getDimensionPixelSize(R.dimen.zzzz_tran_multi_window_button_large_marginV4);
        } else {
            int paddingTop3 = this.mContext.getResources().getDimensionPixelSize(R.dimen.zzzz_tran_caption_morefun_paddingtopV4);
            int paddingLeft3 = this.mContext.getResources().getDimensionPixelSize(R.dimen.zzzz_tran_caption_morefun_paddingstartV4);
            int paddingRight3 = this.mContext.getResources().getDimensionPixelSize(R.dimen.zzzz_tran_caption_morefun_paddingendV4);
            int paddingBottom3 = (this.mContext.getResources().getDimensionPixelSize(R.dimen.zzzz_tran_caption_morefun_heightV4) - paddingTop3) - this.mContext.getResources().getDimensionPixelSize(R.dimen.zzzz_tran_multi_window_button_sizeV4);
            paddingTop = paddingTop3;
            paddingBottom = paddingBottom3;
            paddingLeft = paddingLeft3;
            paddingRight = paddingRight3;
            iconMarginEnd = this.mContext.getResources().getDimensionPixelSize(R.dimen.zzzz_tran_multi_window_button_marginV4);
        }
        final int width = params.width;
        int i = params.height;
        if (isShow) {
            ValueAnimator animatorBubble2 = ValueAnimator.ofFloat(0.0f, 1.0f);
            animatorBubble2.setStartDelay(100L);
            animatorBubble = animatorBubble2;
        } else {
            ValueAnimator animatorBubble3 = ValueAnimator.ofFloat(1.0f, 0.0f);
            animatorBubble3.setStartDelay(0L);
            animatorBubble = animatorBubble3;
        }
        animatorBubble.setInterpolator(new PathInterpolator(0.2f, 0.0f, 0.1f, 1.0f));
        animatorBubble.setDuration(150L);
        ValueAnimator animatorBubble4 = animatorBubble;
        final float f = y_start;
        final float f2 = y_end;
        animatorBubble4.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.internal.widget.TranMultiDecorCaptionViewV4.6
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float value = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                float bubbleScaleX = TranMultiDecorCaptionViewV4.this.getValueByProgress(value, 0.15f, 1.0f);
                float bubbleScaleY = TranMultiDecorCaptionViewV4.this.getValueByProgress(value, 0.1f, 1.0f);
                TranMultiDecorCaptionViewV4 tranMultiDecorCaptionViewV4 = TranMultiDecorCaptionViewV4.this;
                float f3 = x;
                tranMultiDecorCaptionViewV4.getValueByProgress(value, (float) (f3 + ((width * 0.15d) / 2.0d)), f3);
                float py = TranMultiDecorCaptionViewV4.this.getValueByProgress(value, f, f2);
                float alpha = TranMultiDecorCaptionViewV4.this.getValueByProgress(value, 0.3f, 1.0f);
                TranMultiDecorCaptionViewV4.this.mCoverActivieMoreFunBubble.setScaleX(bubbleScaleX);
                TranMultiDecorCaptionViewV4.this.mCoverActivieMoreFunBubble.setScaleY(bubbleScaleY);
                TranMultiDecorCaptionViewV4.this.mCoverActivieMoreFunBubble.setAlpha(alpha);
                RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) TranMultiDecorCaptionViewV4.this.mCoverActivieMoreFunBubble.getLayoutParams();
                params1.topMargin = (int) py;
                TranMultiDecorCaptionViewV4.this.mCoverActivieMoreFunBubble.invalidate();
                TranMultiDecorCaptionViewV4.this.mCoverActivieMoreFunBubble.setLayoutParams(params1);
                TranMultiDecorCaptionViewV4.this.getValueByProgress(value, 0.15f, 1.0f);
                TranMultiDecorCaptionViewV4.this.mActiveChangeMax.setScaleX(value);
                TranMultiDecorCaptionViewV4.this.mActiveChangeMax.setScaleY(value);
                TranMultiDecorCaptionViewV4.this.mActiveChangeSmall.setScaleX(value);
                TranMultiDecorCaptionViewV4.this.mActiveChangeSmall.setScaleY(value);
                TranMultiDecorCaptionViewV4.this.mActiveChangeSplit.setScaleX(value);
                TranMultiDecorCaptionViewV4.this.mActiveChangeSplit.setScaleY(value);
                TranMultiDecorCaptionViewV4.this.mActiveMute.setScaleX(value);
                TranMultiDecorCaptionViewV4.this.mActiveMute.setScaleY(value);
            }
        });
        final int i2 = paddingLeft;
        final int i3 = paddingTop;
        final int i4 = paddingRight;
        final int i5 = paddingBottom;
        final int i6 = iconMarginEnd;
        animatorBubble4.addListener(new Animator.AnimatorListener() { // from class: com.android.internal.widget.TranMultiDecorCaptionViewV4.7
            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animation) {
                if (isShow) {
                    if (TranMultiDecorCaptionViewV4.this.mIsNightMode) {
                        TranMultiDecorCaptionViewV4.this.mCoverActivieMoreFunBubble.setBackgroundResource(R.drawable.zzzz_tran_decor_morefunction_button_bg_darkV4);
                    } else {
                        TranMultiDecorCaptionViewV4.this.mCoverActivieMoreFunBubble.setBackgroundResource(R.drawable.zzzz_tran_decor_morefunction_button_bg_lightV4);
                    }
                    TranMultiDecorCaptionViewV4.this.mCoverActivieMoreFunBubble.setPadding(i2, i3, i4, i5);
                    TranMultiDecorCaptionViewV4.this.mCoverActivieMoreFunBubble.setVisibility(0);
                    if (TranMultiDecorCaptionViewV4.this.mIsActiveChangeMaxVisible) {
                        LinearLayout.LayoutParams params2 = (LinearLayout.LayoutParams) TranMultiDecorCaptionViewV4.this.mActiveChangeMax.getLayoutParams();
                        params2.setMarginEnd(i6);
                        TranMultiDecorCaptionViewV4.this.mActiveChangeMax.setLayoutParams(params2);
                        TranMultiDecorCaptionViewV4.this.mActiveChangeMax.setAlpha(0.0f);
                        TranMultiDecorCaptionViewV4.this.mActiveChangeMax.setVisibility(0);
                    }
                    if (TranMultiDecorCaptionViewV4.this.mIsActiveChangeSmallVisible) {
                        LinearLayout.LayoutParams params3 = (LinearLayout.LayoutParams) TranMultiDecorCaptionViewV4.this.mActiveChangeSmall.getLayoutParams();
                        params3.setMarginEnd(i6);
                        TranMultiDecorCaptionViewV4.this.mActiveChangeSmall.setLayoutParams(params3);
                        TranMultiDecorCaptionViewV4.this.mActiveChangeSmall.setAlpha(0.0f);
                        TranMultiDecorCaptionViewV4.this.mActiveChangeSmall.setVisibility(0);
                    }
                    if (TranMultiDecorCaptionViewV4.this.mIsActiveChangeSplitVisible) {
                        LinearLayout.LayoutParams params4 = (LinearLayout.LayoutParams) TranMultiDecorCaptionViewV4.this.mActiveChangeSplit.getLayoutParams();
                        params4.setMarginEnd(i6);
                        TranMultiDecorCaptionViewV4.this.mActiveChangeSplit.setLayoutParams(params4);
                        TranMultiDecorCaptionViewV4.this.mActiveChangeSplit.setAlpha(0.0f);
                        TranMultiDecorCaptionViewV4.this.mActiveChangeSplit.setVisibility(0);
                    }
                    if (TranMultiDecorCaptionViewV4.this.mIsActiveMuteVisible) {
                        TranMultiDecorCaptionViewV4.this.mActiveMute.setAlpha(0.0f);
                        TranMultiDecorCaptionViewV4.this.mActiveMute.setVisibility(0);
                    }
                }
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animation) {
                if (!isShow) {
                    TranMultiDecorCaptionViewV4.this.mCoverActivieMoreFunBubble.setVisibility(4);
                    if (!TranMultiDecorCaptionViewV4.this.mIsActiveChangeMaxVisible) {
                        TranMultiDecorCaptionViewV4.this.mActiveChangeMax.setVisibility(8);
                    }
                    if (!TranMultiDecorCaptionViewV4.this.mIsActiveChangeSmallVisible) {
                        TranMultiDecorCaptionViewV4.this.mActiveChangeSmall.setVisibility(8);
                    }
                    if (!TranMultiDecorCaptionViewV4.this.mIsActiveChangeSplitVisible) {
                        TranMultiDecorCaptionViewV4.this.mActiveChangeSplit.setVisibility(8);
                    }
                    if (!TranMultiDecorCaptionViewV4.this.mIsActiveMuteVisible) {
                        TranMultiDecorCaptionViewV4.this.mActiveMute.setVisibility(8);
                    }
                }
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationRepeat(Animator animation) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animation) {
            }
        });
        if (isShow) {
            animatorIcon = ValueAnimator.ofFloat(0.0f, 1.0f);
            animatorIcon.setDuration(84L);
            animatorIcon.setStartDelay(166L);
        } else {
            animatorIcon = ValueAnimator.ofFloat(1.0f, 0.0f);
            animatorIcon.setDuration(100L);
            animatorIcon.setStartDelay(0L);
        }
        animatorIcon.setInterpolator(new PathInterpolator(0.33f, 0.0f, 0.67f, 1.0f));
        animatorIcon.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.internal.widget.TranMultiDecorCaptionViewV4.8
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float value = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                TranMultiDecorCaptionViewV4.this.mActiveChangeMax.setAlpha(value);
                TranMultiDecorCaptionViewV4.this.mActiveChangeSmall.setAlpha(value);
                TranMultiDecorCaptionViewV4.this.mActiveChangeSplit.setAlpha(value);
                TranMultiDecorCaptionViewV4.this.mActiveMute.setAlpha(value);
            }
        });
        this.mBubbleAnimator.playTogether(animatorBubble4, animatorIcon, animatorMoreFunction);
        this.mBubbleAnimator.start();
    }

    public boolean isHIOS() {
        return SystemProperties.get("ro.tranos.type").equals("hios") || SystemProperties.get("ro.tranos.type").equals("hios_lite");
    }

    public boolean isXOS() {
        return SystemProperties.get("ro.tranos.type").equals("xos") || SystemProperties.get("ro.tranos.type").equals("xos_lite");
    }

    private int calcluteBackgroundColor(int statusBarColor) {
        ((ImageView) this.mMaximizeIcon).setImageResource(R.drawable.zzzz_tran_decor_maximize_button_selectorV4);
        ((ImageView) this.mCloseIcon).setImageResource(R.drawable.zzzz_tran_decor_close_button_selectorV4);
        this.mTopCaptionBg.setBackgroundColor(0);
        boolean isFullScreenBySystemUi = (this.mOwner.getDecorView().getSystemUiVisibility() & 4) == 4;
        boolean isFillingScreen = isFillingScreen(getContext().getResources().getConfiguration());
        if (isFullScreenBySystemUi || isFillingScreen) {
            ((ImageView) this.mMaximizeIcon).setImageResource(R.drawable.zzzz_tran_decor_maximize_button_white_selectorV4);
            ((ImageView) this.mCloseIcon).setImageResource(R.drawable.zzzz_tran_decor_close_button_white_selectorV4);
            this.mTopCaptionBg.setBackgroundResource(R.drawable.zzzz_tran_decor_multiwindow_caption_transparent_bgV4);
            return 0;
        } else if (statusBarColor != 0) {
            return statusBarColor;
        } else {
            if (this.mIsNightMode) {
                int tempStatusBarColor = DEFAULT_NIGHT_MODE_STATUS_COLOR;
                return tempStatusBarColor;
            } else if (isHIOS()) {
                int tempStatusBarColor2 = DEFAULT_HIOS_LINGHT_MODE_STATUS_COLOR;
                return tempStatusBarColor2;
            } else if (!isXOS()) {
                return statusBarColor;
            } else {
                int tempStatusBarColor3 = DEFAULT_XOS_LINGHT_MODE_STATUS_COLOR;
                return tempStatusBarColor3;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public float getValueByProgress(float progress, float srcValue, float dstValue) {
        return ((dstValue - srcValue) * progress) + srcValue;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void closeInputMethod() {
        PhoneWindow phoneWindow = this.mOwner;
        if (phoneWindow == null || phoneWindow.getCurrentFocus() == null || this.mOwner.getCurrentFocus().getWindowToken() == null) {
            return;
        }
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(this.mOwner.getCurrentFocus().getWindowToken(), 2);
    }

    private boolean isFillingScreen(Configuration config) {
        PhoneWindow phoneWindow;
        if (config == null || config.windowConfiguration == null || (phoneWindow = this.mOwner) == null || phoneWindow.getDecorView() == null) {
            return false;
        }
        boolean isFullscreen = config.windowConfiguration.getWindowingMode() == 1;
        return isFullscreen && ((this.mOwner.getDecorView().getWindowSystemUiVisibility() | this.mOwner.getDecorView().getSystemUiVisibility()) & 4) != 0;
    }
}
