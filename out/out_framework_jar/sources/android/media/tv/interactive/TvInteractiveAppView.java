package android.media.tv.interactive;

import android.annotation.NonNull;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.tv.TvInputManager;
import android.media.tv.TvTrackInfo;
import android.media.tv.TvView;
import android.media.tv.interactive.TvInteractiveAppManager;
import android.media.tv.interactive.TvInteractiveAppView;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewRootImpl;
import com.android.internal.util.AnnotationValidations;
import java.util.List;
import java.util.concurrent.Executor;
/* loaded from: classes2.dex */
public class TvInteractiveAppView extends ViewGroup {
    public static final String BI_INTERACTIVE_APP_KEY_ALIAS = "alias";
    public static final String BI_INTERACTIVE_APP_KEY_CERTIFICATE = "certificate";
    public static final String BI_INTERACTIVE_APP_KEY_HTTP_ADDITIONAL_HEADERS = "http_additional_headers";
    public static final String BI_INTERACTIVE_APP_KEY_HTTP_USER_AGENT = "http_user_agent";
    public static final String BI_INTERACTIVE_APP_KEY_PRIVATE_KEY = "private_key";
    private static final boolean DEBUG = false;
    public static final String ERROR_KEY_METHOD_NAME = "method_name";
    private static final int SET_TVVIEW_FAIL = 2;
    private static final int SET_TVVIEW_SUCCESS = 1;
    private static final String TAG = "TvInteractiveAppView";
    private static final int UNSET_TVVIEW_FAIL = 4;
    private static final int UNSET_TVVIEW_SUCCESS = 3;
    private final AttributeSet mAttrs;
    private TvInteractiveAppCallback mCallback;
    private Executor mCallbackExecutor;
    private final Object mCallbackLock;
    private final int mDefStyleAttr;
    private final TvInteractiveAppManager.Session.FinishedInputEventCallback mFinishedInputEventCallback;
    private final Handler mHandler;
    private boolean mMediaViewCreated;
    private Rect mMediaViewFrame;
    private OnUnhandledInputEventListener mOnUnhandledInputEventListener;
    private final XmlResourceParser mParser;
    private TvInteractiveAppManager.Session mSession;
    private MySessionCallback mSessionCallback;
    private Surface mSurface;
    private boolean mSurfaceChanged;
    private int mSurfaceFormat;
    private int mSurfaceHeight;
    private final SurfaceHolder.Callback mSurfaceHolderCallback;
    private SurfaceView mSurfaceView;
    private int mSurfaceViewBottom;
    private int mSurfaceViewLeft;
    private int mSurfaceViewRight;
    private int mSurfaceViewTop;
    private int mSurfaceWidth;
    private final TvInteractiveAppManager mTvInteractiveAppManager;
    private boolean mUseRequestedSurfaceLayout;

    /* loaded from: classes2.dex */
    public interface OnUnhandledInputEventListener {
        boolean onUnhandledInputEvent(InputEvent inputEvent);
    }

    public TvInteractiveAppView(Context context) {
        this(context, null, 0);
    }

    public TvInteractiveAppView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TvInteractiveAppView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mHandler = new Handler();
        this.mCallbackLock = new Object();
        this.mSurfaceHolderCallback = new SurfaceHolder.Callback() { // from class: android.media.tv.interactive.TvInteractiveAppView.1
            @Override // android.view.SurfaceHolder.Callback
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                TvInteractiveAppView.this.mSurfaceFormat = format;
                TvInteractiveAppView.this.mSurfaceWidth = width;
                TvInteractiveAppView.this.mSurfaceHeight = height;
                TvInteractiveAppView.this.mSurfaceChanged = true;
                TvInteractiveAppView tvInteractiveAppView = TvInteractiveAppView.this;
                tvInteractiveAppView.dispatchSurfaceChanged(tvInteractiveAppView.mSurfaceFormat, TvInteractiveAppView.this.mSurfaceWidth, TvInteractiveAppView.this.mSurfaceHeight);
            }

            @Override // android.view.SurfaceHolder.Callback
            public void surfaceCreated(SurfaceHolder holder) {
                TvInteractiveAppView.this.mSurface = holder.getSurface();
                TvInteractiveAppView tvInteractiveAppView = TvInteractiveAppView.this;
                tvInteractiveAppView.setSessionSurface(tvInteractiveAppView.mSurface);
            }

            @Override // android.view.SurfaceHolder.Callback
            public void surfaceDestroyed(SurfaceHolder holder) {
                TvInteractiveAppView.this.mSurface = null;
                TvInteractiveAppView.this.mSurfaceChanged = false;
                TvInteractiveAppView.this.setSessionSurface(null);
            }
        };
        this.mFinishedInputEventCallback = new TvInteractiveAppManager.Session.FinishedInputEventCallback() { // from class: android.media.tv.interactive.TvInteractiveAppView.3
            @Override // android.media.tv.interactive.TvInteractiveAppManager.Session.FinishedInputEventCallback
            public void onFinishedInputEvent(Object token, boolean handled) {
                ViewRootImpl viewRootImpl;
                if (handled) {
                    return;
                }
                InputEvent event = (InputEvent) token;
                if (!TvInteractiveAppView.this.dispatchUnhandledInputEvent(event) && (viewRootImpl = TvInteractiveAppView.this.getViewRootImpl()) != null) {
                    viewRootImpl.dispatchUnhandledInputEvent(event);
                }
            }
        };
        int sourceResId = Resources.getAttributeSetSourceResId(attrs);
        if (sourceResId != 0) {
            Log.d(TAG, "Build local AttributeSet");
            XmlResourceParser xml = context.getResources().getXml(sourceResId);
            this.mParser = xml;
            this.mAttrs = Xml.asAttributeSet(xml);
        } else {
            Log.d(TAG, "Use passed in AttributeSet");
            this.mParser = null;
            this.mAttrs = attrs;
        }
        this.mDefStyleAttr = defStyleAttr;
        resetSurfaceView();
        this.mTvInteractiveAppManager = (TvInteractiveAppManager) getContext().getSystemService(Context.TV_INTERACTIVE_APP_SERVICE);
    }

    public void setCallback(Executor executor, TvInteractiveAppCallback callback) {
        AnnotationValidations.validate((Class<NonNull>) NonNull.class, (NonNull) null, (Object) callback);
        synchronized (this.mCallbackLock) {
            this.mCallbackExecutor = executor;
            this.mCallback = callback;
        }
    }

    public void clearCallback() {
        synchronized (this.mCallbackLock) {
            this.mCallback = null;
            this.mCallbackExecutor = null;
        }
    }

    @Override // android.view.ViewGroup, android.view.View
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        createSessionMediaView();
    }

    @Override // android.view.ViewGroup, android.view.View
    public void onDetachedFromWindow() {
        removeSessionMediaView();
        super.onDetachedFromWindow();
    }

    @Override // android.view.ViewGroup, android.view.View
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.mUseRequestedSurfaceLayout) {
            this.mSurfaceView.layout(this.mSurfaceViewLeft, this.mSurfaceViewTop, this.mSurfaceViewRight, this.mSurfaceViewBottom);
        } else {
            this.mSurfaceView.layout(0, 0, right - left, bottom - top);
        }
    }

    @Override // android.view.View
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        this.mSurfaceView.measure(widthMeasureSpec, heightMeasureSpec);
        int width = this.mSurfaceView.getMeasuredWidth();
        int height = this.mSurfaceView.getMeasuredHeight();
        int childState = this.mSurfaceView.getMeasuredState();
        setMeasuredDimension(resolveSizeAndState(width, widthMeasureSpec, childState), resolveSizeAndState(height, heightMeasureSpec, childState << 16));
    }

    @Override // android.view.View
    public void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        this.mSurfaceView.setVisibility(visibility);
        if (visibility == 0) {
            createSessionMediaView();
        } else {
            removeSessionMediaView();
        }
    }

    private void resetSurfaceView() {
        SurfaceView surfaceView = this.mSurfaceView;
        if (surfaceView != null) {
            surfaceView.getHolder().removeCallback(this.mSurfaceHolderCallback);
            removeView(this.mSurfaceView);
        }
        this.mSurface = null;
        SurfaceView surfaceView2 = new SurfaceView(getContext(), this.mAttrs, this.mDefStyleAttr) { // from class: android.media.tv.interactive.TvInteractiveAppView.2
            /* JADX INFO: Access modifiers changed from: protected */
            @Override // android.view.SurfaceView
            public void updateSurface() {
                super.updateSurface();
                TvInteractiveAppView.this.relayoutSessionMediaView();
            }
        };
        this.mSurfaceView = surfaceView2;
        surfaceView2.setSecure(true);
        this.mSurfaceView.getHolder().addCallback(this.mSurfaceHolderCallback);
        this.mSurfaceView.getHolder().setFormat(-3);
        addView(this.mSurfaceView);
    }

    public void reset() {
        resetInternal();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void createSessionMediaView() {
        if (this.mSession == null || !isAttachedToWindow() || this.mMediaViewCreated) {
            return;
        }
        Rect viewFrameOnScreen = getViewFrameOnScreen();
        this.mMediaViewFrame = viewFrameOnScreen;
        this.mSession.createMediaView(this, viewFrameOnScreen);
        this.mMediaViewCreated = true;
    }

    private void removeSessionMediaView() {
        TvInteractiveAppManager.Session session = this.mSession;
        if (session == null || !this.mMediaViewCreated) {
            return;
        }
        session.removeMediaView();
        this.mMediaViewCreated = false;
        this.mMediaViewFrame = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void relayoutSessionMediaView() {
        if (this.mSession == null || !isAttachedToWindow() || !this.mMediaViewCreated) {
            return;
        }
        Rect viewFrame = getViewFrameOnScreen();
        if (viewFrame.equals(this.mMediaViewFrame)) {
            return;
        }
        this.mSession.relayoutMediaView(viewFrame);
        this.mMediaViewFrame = viewFrame;
    }

    private Rect getViewFrameOnScreen() {
        Rect frame = new Rect();
        getGlobalVisibleRect(frame);
        RectF frameF = new RectF(frame);
        getMatrix().mapRect(frameF);
        frameF.round(frame);
        return frame;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setSessionSurface(Surface surface) {
        TvInteractiveAppManager.Session session = this.mSession;
        if (session == null) {
            return;
        }
        session.setSurface(surface);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchSurfaceChanged(int format, int width, int height) {
        TvInteractiveAppManager.Session session = this.mSession;
        if (session == null) {
            return;
        }
        session.dispatchSurfaceChanged(format, width, height);
    }

    public boolean dispatchUnhandledInputEvent(InputEvent event) {
        OnUnhandledInputEventListener onUnhandledInputEventListener = this.mOnUnhandledInputEventListener;
        if (onUnhandledInputEventListener != null && onUnhandledInputEventListener.onUnhandledInputEvent(event)) {
            return true;
        }
        return onUnhandledInputEvent(event);
    }

    public boolean onUnhandledInputEvent(InputEvent event) {
        return false;
    }

    public void setOnUnhandledInputEventListener(Executor executor, OnUnhandledInputEventListener listener) {
        this.mOnUnhandledInputEventListener = listener;
    }

    public OnUnhandledInputEventListener getOnUnhandledInputEventListener() {
        return this.mOnUnhandledInputEventListener;
    }

    public void clearOnUnhandledInputEventListener() {
        this.mOnUnhandledInputEventListener = null;
    }

    @Override // android.view.ViewGroup, android.view.View
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (super.dispatchKeyEvent(event)) {
            return true;
        }
        if (this.mSession == null) {
            return false;
        }
        InputEvent copiedEvent = event.copy();
        int ret = this.mSession.dispatchInputEvent(copiedEvent, copiedEvent, this.mFinishedInputEventCallback, this.mHandler);
        return ret != 0;
    }

    public void prepareInteractiveApp(String iAppServiceId, int type) {
        MySessionCallback mySessionCallback = new MySessionCallback(iAppServiceId, type);
        this.mSessionCallback = mySessionCallback;
        TvInteractiveAppManager tvInteractiveAppManager = this.mTvInteractiveAppManager;
        if (tvInteractiveAppManager != null) {
            tvInteractiveAppManager.createSession(iAppServiceId, type, mySessionCallback, this.mHandler);
        }
    }

    public void startInteractiveApp() {
        TvInteractiveAppManager.Session session = this.mSession;
        if (session != null) {
            session.startInteractiveApp();
        }
    }

    public void stopInteractiveApp() {
        TvInteractiveAppManager.Session session = this.mSession;
        if (session != null) {
            session.stopInteractiveApp();
        }
    }

    public void resetInteractiveApp() {
        TvInteractiveAppManager.Session session = this.mSession;
        if (session != null) {
            session.resetInteractiveApp();
        }
    }

    public void sendCurrentChannelUri(Uri channelUri) {
        TvInteractiveAppManager.Session session = this.mSession;
        if (session != null) {
            session.sendCurrentChannelUri(channelUri);
        }
    }

    public void sendCurrentChannelLcn(int lcn) {
        TvInteractiveAppManager.Session session = this.mSession;
        if (session != null) {
            session.sendCurrentChannelLcn(lcn);
        }
    }

    public void sendStreamVolume(float volume) {
        TvInteractiveAppManager.Session session = this.mSession;
        if (session != null) {
            session.sendStreamVolume(volume);
        }
    }

    public void sendTrackInfoList(List<TvTrackInfo> tracks) {
        TvInteractiveAppManager.Session session = this.mSession;
        if (session != null) {
            session.sendTrackInfoList(tracks);
        }
    }

    public void sendCurrentTvInputId(String inputId) {
        TvInteractiveAppManager.Session session = this.mSession;
        if (session != null) {
            session.sendCurrentTvInputId(inputId);
        }
    }

    public void sendSigningResult(String signingId, byte[] result) {
        TvInteractiveAppManager.Session session = this.mSession;
        if (session != null) {
            session.sendSigningResult(signingId, result);
        }
    }

    public void notifyError(String errMsg, Bundle params) {
        TvInteractiveAppManager.Session session = this.mSession;
        if (session != null) {
            session.notifyError(errMsg, params);
        }
    }

    private void resetInternal() {
        this.mSessionCallback = null;
        if (this.mSession != null) {
            setSessionSurface(null);
            removeSessionMediaView();
            this.mUseRequestedSurfaceLayout = false;
            this.mSession.release();
            this.mSession = null;
            resetSurfaceView();
        }
    }

    public void createBiInteractiveApp(Uri biIAppUri, Bundle params) {
        TvInteractiveAppManager.Session session = this.mSession;
        if (session != null) {
            session.createBiInteractiveApp(biIAppUri, params);
        }
    }

    public void destroyBiInteractiveApp(String biIAppId) {
        TvInteractiveAppManager.Session session = this.mSession;
        if (session != null) {
            session.destroyBiInteractiveApp(biIAppId);
        }
    }

    public TvInteractiveAppManager.Session getInteractiveAppSession() {
        return this.mSession;
    }

    public int setTvView(TvView tvView) {
        TvInteractiveAppManager.Session session;
        if (tvView == null) {
            return unsetTvView();
        }
        TvInputManager.Session inputSession = tvView.getInputSession();
        if (inputSession == null || (session = this.mSession) == null) {
            return 2;
        }
        session.setInputSession(inputSession);
        inputSession.setInteractiveAppSession(this.mSession);
        return 1;
    }

    private int unsetTvView() {
        TvInteractiveAppManager.Session session = this.mSession;
        if (session == null || session.getInputSession() == null) {
            return 4;
        }
        this.mSession.getInputSession().setInteractiveAppSession(null);
        this.mSession.setInputSession(null);
        return 3;
    }

    public void setTeletextAppEnabled(boolean enable) {
        TvInteractiveAppManager.Session session = this.mSession;
        if (session != null) {
            session.setTeletextAppEnabled(enable);
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class TvInteractiveAppCallback {
        public void onPlaybackCommandRequest(String iAppServiceId, String cmdType, Bundle parameters) {
        }

        public void onStateChanged(String iAppServiceId, int state, int err) {
        }

        public void onBiInteractiveAppCreated(String iAppServiceId, Uri biIAppUri, String biIAppId) {
        }

        public void onTeletextAppStateChanged(String iAppServiceId, int state) {
        }

        public void onSetVideoBounds(String iAppServiceId, Rect rect) {
        }

        public void onRequestCurrentChannelUri(String iAppServiceId) {
        }

        public void onRequestCurrentChannelLcn(String iAppServiceId) {
        }

        public void onRequestStreamVolume(String iAppServiceId) {
        }

        public void onRequestTrackInfoList(String iAppServiceId) {
        }

        public void onRequestCurrentTvInputId(String iAppServiceId) {
        }

        public void onRequestSigning(String iAppServiceId, String signingId, String algorithm, String alias, byte[] data) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class MySessionCallback extends TvInteractiveAppManager.SessionCallback {
        final String mIAppServiceId;
        int mType;

        MySessionCallback(String iAppServiceId, int type) {
            this.mIAppServiceId = iAppServiceId;
            this.mType = type;
        }

        @Override // android.media.tv.interactive.TvInteractiveAppManager.SessionCallback
        public void onSessionCreated(TvInteractiveAppManager.Session session) {
            if (this != TvInteractiveAppView.this.mSessionCallback) {
                Log.w(TvInteractiveAppView.TAG, "onSessionCreated - session already created");
                if (session != null) {
                    session.release();
                    return;
                }
                return;
            }
            TvInteractiveAppView.this.mSession = session;
            if (session != null) {
                if (TvInteractiveAppView.this.mSurface != null) {
                    TvInteractiveAppView tvInteractiveAppView = TvInteractiveAppView.this;
                    tvInteractiveAppView.setSessionSurface(tvInteractiveAppView.mSurface);
                    if (TvInteractiveAppView.this.mSurfaceChanged) {
                        TvInteractiveAppView tvInteractiveAppView2 = TvInteractiveAppView.this;
                        tvInteractiveAppView2.dispatchSurfaceChanged(tvInteractiveAppView2.mSurfaceFormat, TvInteractiveAppView.this.mSurfaceWidth, TvInteractiveAppView.this.mSurfaceHeight);
                    }
                }
                TvInteractiveAppView.this.createSessionMediaView();
                return;
            }
            TvInteractiveAppView.this.mSessionCallback = null;
        }

        @Override // android.media.tv.interactive.TvInteractiveAppManager.SessionCallback
        public void onSessionReleased(TvInteractiveAppManager.Session session) {
            if (this != TvInteractiveAppView.this.mSessionCallback) {
                Log.w(TvInteractiveAppView.TAG, "onSessionReleased - session not created");
                return;
            }
            TvInteractiveAppView.this.mMediaViewCreated = false;
            TvInteractiveAppView.this.mMediaViewFrame = null;
            TvInteractiveAppView.this.mSessionCallback = null;
            TvInteractiveAppView.this.mSession = null;
        }

        @Override // android.media.tv.interactive.TvInteractiveAppManager.SessionCallback
        public void onLayoutSurface(TvInteractiveAppManager.Session session, int left, int top, int right, int bottom) {
            if (this != TvInteractiveAppView.this.mSessionCallback) {
                Log.w(TvInteractiveAppView.TAG, "onLayoutSurface - session not created");
                return;
            }
            TvInteractiveAppView.this.mSurfaceViewLeft = left;
            TvInteractiveAppView.this.mSurfaceViewTop = top;
            TvInteractiveAppView.this.mSurfaceViewRight = right;
            TvInteractiveAppView.this.mSurfaceViewBottom = bottom;
            TvInteractiveAppView.this.mUseRequestedSurfaceLayout = true;
            TvInteractiveAppView.this.requestLayout();
        }

        @Override // android.media.tv.interactive.TvInteractiveAppManager.SessionCallback
        public void onCommandRequest(TvInteractiveAppManager.Session session, final String cmdType, final Bundle parameters) {
            if (this != TvInteractiveAppView.this.mSessionCallback) {
                Log.w(TvInteractiveAppView.TAG, "onCommandRequest - session not created");
                return;
            }
            synchronized (TvInteractiveAppView.this.mCallbackLock) {
                if (TvInteractiveAppView.this.mCallbackExecutor != null) {
                    TvInteractiveAppView.this.mCallbackExecutor.execute(new Runnable() { // from class: android.media.tv.interactive.TvInteractiveAppView$MySessionCallback$$ExternalSyntheticLambda4
                        @Override // java.lang.Runnable
                        public final void run() {
                            TvInteractiveAppView.MySessionCallback.this.m2635x1270068(cmdType, parameters);
                        }
                    });
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onCommandRequest$0$android-media-tv-interactive-TvInteractiveAppView$MySessionCallback  reason: not valid java name */
        public /* synthetic */ void m2635x1270068(String cmdType, Bundle parameters) {
            synchronized (TvInteractiveAppView.this.mCallbackLock) {
                if (TvInteractiveAppView.this.mCallback != null) {
                    TvInteractiveAppView.this.mCallback.onPlaybackCommandRequest(this.mIAppServiceId, cmdType, parameters);
                }
            }
        }

        @Override // android.media.tv.interactive.TvInteractiveAppManager.SessionCallback
        public void onSessionStateChanged(TvInteractiveAppManager.Session session, final int state, final int err) {
            if (this != TvInteractiveAppView.this.mSessionCallback) {
                Log.w(TvInteractiveAppView.TAG, "onSessionStateChanged - session not created");
                return;
            }
            synchronized (TvInteractiveAppView.this.mCallbackLock) {
                if (TvInteractiveAppView.this.mCallbackExecutor != null) {
                    TvInteractiveAppView.this.mCallbackExecutor.execute(new Runnable() { // from class: android.media.tv.interactive.TvInteractiveAppView$MySessionCallback$$ExternalSyntheticLambda2
                        @Override // java.lang.Runnable
                        public final void run() {
                            TvInteractiveAppView.MySessionCallback.this.m2640xc6c6db86(state, err);
                        }
                    });
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onSessionStateChanged$1$android-media-tv-interactive-TvInteractiveAppView$MySessionCallback  reason: not valid java name */
        public /* synthetic */ void m2640xc6c6db86(int state, int err) {
            synchronized (TvInteractiveAppView.this.mCallbackLock) {
                if (TvInteractiveAppView.this.mCallback != null) {
                    TvInteractiveAppView.this.mCallback.onStateChanged(this.mIAppServiceId, state, err);
                }
            }
        }

        @Override // android.media.tv.interactive.TvInteractiveAppManager.SessionCallback
        public void onBiInteractiveAppCreated(TvInteractiveAppManager.Session session, final Uri biIAppUri, final String biIAppId) {
            if (this != TvInteractiveAppView.this.mSessionCallback) {
                Log.w(TvInteractiveAppView.TAG, "onBiInteractiveAppCreated - session not created");
                return;
            }
            synchronized (TvInteractiveAppView.this.mCallbackLock) {
                if (TvInteractiveAppView.this.mCallbackExecutor != null) {
                    TvInteractiveAppView.this.mCallbackExecutor.execute(new Runnable() { // from class: android.media.tv.interactive.TvInteractiveAppView$MySessionCallback$$ExternalSyntheticLambda7
                        @Override // java.lang.Runnable
                        public final void run() {
                            TvInteractiveAppView.MySessionCallback.this.m2634xee05abb0(biIAppUri, biIAppId);
                        }
                    });
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onBiInteractiveAppCreated$2$android-media-tv-interactive-TvInteractiveAppView$MySessionCallback  reason: not valid java name */
        public /* synthetic */ void m2634xee05abb0(Uri biIAppUri, String biIAppId) {
            synchronized (TvInteractiveAppView.this.mCallbackLock) {
                if (TvInteractiveAppView.this.mCallback != null) {
                    TvInteractiveAppView.this.mCallback.onBiInteractiveAppCreated(this.mIAppServiceId, biIAppUri, biIAppId);
                }
            }
        }

        @Override // android.media.tv.interactive.TvInteractiveAppManager.SessionCallback
        public void onTeletextAppStateChanged(TvInteractiveAppManager.Session session, int state) {
            if (this != TvInteractiveAppView.this.mSessionCallback) {
                Log.w(TvInteractiveAppView.TAG, "onTeletextAppStateChanged - session not created");
            } else if (TvInteractiveAppView.this.mCallback != null) {
                TvInteractiveAppView.this.mCallback.onTeletextAppStateChanged(this.mIAppServiceId, state);
            }
        }

        @Override // android.media.tv.interactive.TvInteractiveAppManager.SessionCallback
        public void onSetVideoBounds(TvInteractiveAppManager.Session session, final Rect rect) {
            if (this != TvInteractiveAppView.this.mSessionCallback) {
                Log.w(TvInteractiveAppView.TAG, "onSetVideoBounds - session not created");
                return;
            }
            synchronized (TvInteractiveAppView.this.mCallbackLock) {
                if (TvInteractiveAppView.this.mCallbackExecutor != null) {
                    TvInteractiveAppView.this.mCallbackExecutor.execute(new Runnable() { // from class: android.media.tv.interactive.TvInteractiveAppView$MySessionCallback$$ExternalSyntheticLambda3
                        @Override // java.lang.Runnable
                        public final void run() {
                            TvInteractiveAppView.MySessionCallback.this.m2641xc7ed87f5(rect);
                        }
                    });
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onSetVideoBounds$3$android-media-tv-interactive-TvInteractiveAppView$MySessionCallback  reason: not valid java name */
        public /* synthetic */ void m2641xc7ed87f5(Rect rect) {
            synchronized (TvInteractiveAppView.this.mCallbackLock) {
                if (TvInteractiveAppView.this.mCallback != null) {
                    TvInteractiveAppView.this.mCallback.onSetVideoBounds(this.mIAppServiceId, rect);
                }
            }
        }

        @Override // android.media.tv.interactive.TvInteractiveAppManager.SessionCallback
        public void onRequestCurrentChannelUri(TvInteractiveAppManager.Session session) {
            if (this != TvInteractiveAppView.this.mSessionCallback) {
                Log.w(TvInteractiveAppView.TAG, "onRequestCurrentChannelUri - session not created");
                return;
            }
            synchronized (TvInteractiveAppView.this.mCallbackLock) {
                if (TvInteractiveAppView.this.mCallbackExecutor != null) {
                    TvInteractiveAppView.this.mCallbackExecutor.execute(new Runnable() { // from class: android.media.tv.interactive.TvInteractiveAppView$MySessionCallback$$ExternalSyntheticLambda1
                        @Override // java.lang.Runnable
                        public final void run() {
                            TvInteractiveAppView.MySessionCallback.this.m2637x4e48ac5b();
                        }
                    });
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onRequestCurrentChannelUri$4$android-media-tv-interactive-TvInteractiveAppView$MySessionCallback  reason: not valid java name */
        public /* synthetic */ void m2637x4e48ac5b() {
            synchronized (TvInteractiveAppView.this.mCallbackLock) {
                if (TvInteractiveAppView.this.mCallback != null) {
                    TvInteractiveAppView.this.mCallback.onRequestCurrentChannelUri(this.mIAppServiceId);
                }
            }
        }

        @Override // android.media.tv.interactive.TvInteractiveAppManager.SessionCallback
        public void onRequestCurrentChannelLcn(TvInteractiveAppManager.Session session) {
            if (this != TvInteractiveAppView.this.mSessionCallback) {
                Log.w(TvInteractiveAppView.TAG, "onRequestCurrentChannelLcn - session not created");
                return;
            }
            synchronized (TvInteractiveAppView.this.mCallbackLock) {
                if (TvInteractiveAppView.this.mCallbackExecutor != null) {
                    TvInteractiveAppView.this.mCallbackExecutor.execute(new Runnable() { // from class: android.media.tv.interactive.TvInteractiveAppView$MySessionCallback$$ExternalSyntheticLambda6
                        @Override // java.lang.Runnable
                        public final void run() {
                            TvInteractiveAppView.MySessionCallback.this.m2636xda90407();
                        }
                    });
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onRequestCurrentChannelLcn$5$android-media-tv-interactive-TvInteractiveAppView$MySessionCallback  reason: not valid java name */
        public /* synthetic */ void m2636xda90407() {
            synchronized (TvInteractiveAppView.this.mCallbackLock) {
                if (TvInteractiveAppView.this.mCallback != null) {
                    TvInteractiveAppView.this.mCallback.onRequestCurrentChannelLcn(this.mIAppServiceId);
                }
            }
        }

        @Override // android.media.tv.interactive.TvInteractiveAppManager.SessionCallback
        public void onRequestStreamVolume(TvInteractiveAppManager.Session session) {
            if (this != TvInteractiveAppView.this.mSessionCallback) {
                Log.w(TvInteractiveAppView.TAG, "onRequestStreamVolume - session not created");
                return;
            }
            synchronized (TvInteractiveAppView.this.mCallbackLock) {
                if (TvInteractiveAppView.this.mCallbackExecutor != null) {
                    TvInteractiveAppView.this.mCallbackExecutor.execute(new Runnable() { // from class: android.media.tv.interactive.TvInteractiveAppView$MySessionCallback$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            TvInteractiveAppView.MySessionCallback.this.m2638x5089515b();
                        }
                    });
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onRequestStreamVolume$6$android-media-tv-interactive-TvInteractiveAppView$MySessionCallback  reason: not valid java name */
        public /* synthetic */ void m2638x5089515b() {
            synchronized (TvInteractiveAppView.this.mCallbackLock) {
                if (TvInteractiveAppView.this.mCallback != null) {
                    TvInteractiveAppView.this.mCallback.onRequestStreamVolume(this.mIAppServiceId);
                }
            }
        }

        @Override // android.media.tv.interactive.TvInteractiveAppManager.SessionCallback
        public void onRequestTrackInfoList(TvInteractiveAppManager.Session session) {
            if (this != TvInteractiveAppView.this.mSessionCallback) {
                Log.w(TvInteractiveAppView.TAG, "onRequestTrackInfoList - session not created");
                return;
            }
            synchronized (TvInteractiveAppView.this.mCallbackLock) {
                if (TvInteractiveAppView.this.mCallbackExecutor != null) {
                    TvInteractiveAppView.this.mCallbackExecutor.execute(new Runnable() { // from class: android.media.tv.interactive.TvInteractiveAppView$MySessionCallback$$ExternalSyntheticLambda5
                        @Override // java.lang.Runnable
                        public final void run() {
                            TvInteractiveAppView.MySessionCallback.this.m2639x9fe886b3();
                        }
                    });
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onRequestTrackInfoList$7$android-media-tv-interactive-TvInteractiveAppView$MySessionCallback  reason: not valid java name */
        public /* synthetic */ void m2639x9fe886b3() {
            synchronized (TvInteractiveAppView.this.mCallbackLock) {
                if (TvInteractiveAppView.this.mCallback != null) {
                    TvInteractiveAppView.this.mCallback.onRequestTrackInfoList(this.mIAppServiceId);
                }
            }
        }

        @Override // android.media.tv.interactive.TvInteractiveAppManager.SessionCallback
        public void onRequestCurrentTvInputId(TvInteractiveAppManager.Session session) {
            if (this != TvInteractiveAppView.this.mSessionCallback) {
                Log.w(TvInteractiveAppView.TAG, "onRequestCurrentTvInputId - session not created");
            } else if (TvInteractiveAppView.this.mCallback != null) {
                TvInteractiveAppView.this.mCallback.onRequestCurrentTvInputId(this.mIAppServiceId);
            }
        }

        @Override // android.media.tv.interactive.TvInteractiveAppManager.SessionCallback
        public void onRequestSigning(TvInteractiveAppManager.Session session, String id, String algorithm, String alias, byte[] data) {
            if (this != TvInteractiveAppView.this.mSessionCallback) {
                Log.w(TvInteractiveAppView.TAG, "onRequestSigning - session not created");
            } else if (TvInteractiveAppView.this.mCallback != null) {
                TvInteractiveAppView.this.mCallback.onRequestSigning(this.mIAppServiceId, id, algorithm, alias, data);
            }
        }
    }
}
