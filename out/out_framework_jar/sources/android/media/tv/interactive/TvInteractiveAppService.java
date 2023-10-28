package android.media.tv.interactive;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.media.tv.AdRequest;
import android.media.tv.AdResponse;
import android.media.tv.BroadcastInfoRequest;
import android.media.tv.BroadcastInfoResponse;
import android.media.tv.TvContentRating;
import android.media.tv.TvTrackInfo;
import android.media.tv.interactive.ITvInteractiveAppService;
import android.media.tv.interactive.ITvInteractiveAppSession;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import com.android.internal.os.SomeArgs;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes2.dex */
public abstract class TvInteractiveAppService extends Service {
    public static final String COMMAND_PARAMETER_KEY_CHANGE_CHANNEL_QUIETLY = "command_change_channel_quietly";
    public static final String COMMAND_PARAMETER_KEY_CHANNEL_URI = "command_channel_uri";
    public static final String COMMAND_PARAMETER_KEY_INPUT_ID = "command_input_id";
    public static final String COMMAND_PARAMETER_KEY_TRACK_ID = "command_track_id";
    public static final String COMMAND_PARAMETER_KEY_TRACK_TYPE = "command_track_type";
    public static final String COMMAND_PARAMETER_KEY_VOLUME = "command_volume";
    private static final boolean DEBUG = false;
    private static final int DETACH_MEDIA_VIEW_TIMEOUT_MS = 5000;
    public static final String PLAYBACK_COMMAND_TYPE_SELECT_TRACK = "select_track";
    public static final String PLAYBACK_COMMAND_TYPE_SET_STREAM_VOLUME = "set_stream_volume";
    public static final String PLAYBACK_COMMAND_TYPE_STOP = "stop";
    public static final String PLAYBACK_COMMAND_TYPE_TUNE = "tune";
    public static final String PLAYBACK_COMMAND_TYPE_TUNE_NEXT = "tune_next";
    public static final String PLAYBACK_COMMAND_TYPE_TUNE_PREV = "tune_previous";
    public static final String SERVICE_INTERFACE = "android.media.tv.interactive.TvInteractiveAppService";
    public static final String SERVICE_META_DATA = "android.media.tv.interactive.app";
    private static final String TAG = "TvInteractiveAppService";
    private final Handler mServiceHandler = new ServiceHandler();
    private final RemoteCallbackList<ITvInteractiveAppServiceCallback> mCallbacks = new RemoteCallbackList<>();

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface PlaybackCommandType {
    }

    public abstract Session onCreateSession(String str, int i);

    @Override // android.app.Service
    public final IBinder onBind(Intent intent) {
        ITvInteractiveAppService.Stub tvIAppServiceBinder = new ITvInteractiveAppService.Stub() { // from class: android.media.tv.interactive.TvInteractiveAppService.1
            @Override // android.media.tv.interactive.ITvInteractiveAppService
            public void registerCallback(ITvInteractiveAppServiceCallback cb) {
                if (cb != null) {
                    TvInteractiveAppService.this.mCallbacks.register(cb);
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppService
            public void unregisterCallback(ITvInteractiveAppServiceCallback cb) {
                if (cb != null) {
                    TvInteractiveAppService.this.mCallbacks.unregister(cb);
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppService
            public void createSession(InputChannel channel, ITvInteractiveAppSessionCallback cb, String iAppServiceId, int type) {
                if (cb == null) {
                    return;
                }
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = channel;
                args.arg2 = cb;
                args.arg3 = iAppServiceId;
                args.arg4 = Integer.valueOf(type);
                TvInteractiveAppService.this.mServiceHandler.obtainMessage(1, args).sendToTarget();
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppService
            public void registerAppLinkInfo(AppLinkInfo appLinkInfo) {
                TvInteractiveAppService.this.onRegisterAppLinkInfo(appLinkInfo);
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppService
            public void unregisterAppLinkInfo(AppLinkInfo appLinkInfo) {
                TvInteractiveAppService.this.onUnregisterAppLinkInfo(appLinkInfo);
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppService
            public void sendAppLinkCommand(Bundle command) {
                TvInteractiveAppService.this.onAppLinkCommand(command);
            }
        };
        return tvIAppServiceBinder;
    }

    public void onRegisterAppLinkInfo(AppLinkInfo appLinkInfo) {
    }

    public void onUnregisterAppLinkInfo(AppLinkInfo appLinkInfo) {
    }

    public void onAppLinkCommand(Bundle command) {
    }

    public final void notifyStateChanged(int type, int state, int error) {
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = Integer.valueOf(type);
        args.arg2 = Integer.valueOf(state);
        args.arg3 = Integer.valueOf(error);
        this.mServiceHandler.obtainMessage(3, args).sendToTarget();
    }

    /* loaded from: classes2.dex */
    public static abstract class Session implements KeyEvent.Callback {
        private final Context mContext;
        final Handler mHandler;
        private Rect mMediaFrame;
        private View mMediaView;
        private MediaViewCleanUpTask mMediaViewCleanUpTask;
        private FrameLayout mMediaViewContainer;
        private boolean mMediaViewEnabled;
        private ITvInteractiveAppSessionCallback mSessionCallback;
        private Surface mSurface;
        private final WindowManager mWindowManager;
        private WindowManager.LayoutParams mWindowParams;
        private IBinder mWindowToken;
        private final KeyEvent.DispatcherState mDispatcherState = new KeyEvent.DispatcherState();
        private final Object mLock = new Object();
        private final List<Runnable> mPendingActions = new ArrayList();

        public abstract void onRelease();

        public abstract boolean onSetSurface(Surface surface);

        public Session(Context context) {
            this.mContext = context;
            this.mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            this.mHandler = new Handler(context.getMainLooper());
        }

        public void setMediaViewEnabled(final boolean enable) {
            this.mHandler.post(new Runnable() { // from class: android.media.tv.interactive.TvInteractiveAppService.Session.1
                @Override // java.lang.Runnable
                public void run() {
                    if (enable == Session.this.mMediaViewEnabled) {
                        return;
                    }
                    Session.this.mMediaViewEnabled = enable;
                    if (enable) {
                        if (Session.this.mWindowToken != null) {
                            Session session = Session.this;
                            session.createMediaView(session.mWindowToken, Session.this.mMediaFrame);
                            return;
                        }
                        return;
                    }
                    Session.this.removeMediaView(false);
                }
            });
        }

        public boolean isMediaViewEnabled() {
            return this.mMediaViewEnabled;
        }

        public void onStartInteractiveApp() {
        }

        public void onStopInteractiveApp() {
        }

        public void onResetInteractiveApp() {
        }

        public void onCreateBiInteractiveAppRequest(Uri biIAppUri, Bundle params) {
        }

        public void onDestroyBiInteractiveAppRequest(String biIAppId) {
        }

        public void onSetTeletextAppEnabled(boolean enable) {
        }

        public void onCurrentChannelUri(Uri channelUri) {
        }

        public void onCurrentChannelLcn(int lcn) {
        }

        public void onStreamVolume(float volume) {
        }

        public void onTrackInfoList(List<TvTrackInfo> tracks) {
        }

        public void onCurrentTvInputId(String inputId) {
        }

        public void onSigningResult(String signingId, byte[] result) {
        }

        public void onError(String errMsg, Bundle params) {
        }

        public void onSurfaceChanged(int format, int width, int height) {
        }

        public void onMediaViewSizeChanged(int width, int height) {
        }

        public View onCreateMediaView() {
            return null;
        }

        public void onTuned(Uri channelUri) {
        }

        public void onTrackSelected(int type, String trackId) {
        }

        public void onTracksChanged(List<TvTrackInfo> tracks) {
        }

        public void onVideoAvailable() {
        }

        public void onVideoUnavailable(int reason) {
        }

        public void onContentAllowed() {
        }

        public void onContentBlocked(TvContentRating rating) {
        }

        public void onSignalStrength(int strength) {
        }

        public void onBroadcastInfoResponse(BroadcastInfoResponse response) {
        }

        public void onAdResponse(AdResponse response) {
        }

        @Override // android.view.KeyEvent.Callback
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            return false;
        }

        @Override // android.view.KeyEvent.Callback
        public boolean onKeyLongPress(int keyCode, KeyEvent event) {
            return false;
        }

        @Override // android.view.KeyEvent.Callback
        public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
            return false;
        }

        @Override // android.view.KeyEvent.Callback
        public boolean onKeyUp(int keyCode, KeyEvent event) {
            return false;
        }

        public boolean onTouchEvent(MotionEvent event) {
            return false;
        }

        public boolean onTrackballEvent(MotionEvent event) {
            return false;
        }

        public boolean onGenericMotionEvent(MotionEvent event) {
            return false;
        }

        public void layoutSurface(final int left, final int top, final int right, final int bottom) {
            if (left > right || top > bottom) {
                throw new IllegalArgumentException("Invalid parameter");
            }
            executeOrPostRunnableOnMainThread(new Runnable() { // from class: android.media.tv.interactive.TvInteractiveAppService.Session.2
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onLayoutSurface(left, top, right, bottom);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInteractiveAppService.TAG, "error in layoutSurface", e);
                    }
                }
            });
        }

        public void requestBroadcastInfo(final BroadcastInfoRequest request) {
            executeOrPostRunnableOnMainThread(new Runnable() { // from class: android.media.tv.interactive.TvInteractiveAppService.Session.3
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onBroadcastInfoRequest(request);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInteractiveAppService.TAG, "error in requestBroadcastInfo", e);
                    }
                }
            });
        }

        public void removeBroadcastInfo(final int requestId) {
            executeOrPostRunnableOnMainThread(new Runnable() { // from class: android.media.tv.interactive.TvInteractiveAppService.Session.4
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onRemoveBroadcastInfo(requestId);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInteractiveAppService.TAG, "error in removeBroadcastInfo", e);
                    }
                }
            });
        }

        public void sendPlaybackCommandRequest(final String cmdType, final Bundle parameters) {
            executeOrPostRunnableOnMainThread(new Runnable() { // from class: android.media.tv.interactive.TvInteractiveAppService.Session.5
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onCommandRequest(cmdType, parameters);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInteractiveAppService.TAG, "error in requestCommand", e);
                    }
                }
            });
        }

        public void setVideoBounds(final Rect rect) {
            executeOrPostRunnableOnMainThread(new Runnable() { // from class: android.media.tv.interactive.TvInteractiveAppService.Session.6
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onSetVideoBounds(rect);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInteractiveAppService.TAG, "error in setVideoBounds", e);
                    }
                }
            });
        }

        public void requestCurrentChannelUri() {
            executeOrPostRunnableOnMainThread(new Runnable() { // from class: android.media.tv.interactive.TvInteractiveAppService.Session.7
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onRequestCurrentChannelUri();
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInteractiveAppService.TAG, "error in requestCurrentChannelUri", e);
                    }
                }
            });
        }

        public void requestCurrentChannelLcn() {
            executeOrPostRunnableOnMainThread(new Runnable() { // from class: android.media.tv.interactive.TvInteractiveAppService.Session.8
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onRequestCurrentChannelLcn();
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInteractiveAppService.TAG, "error in requestCurrentChannelLcn", e);
                    }
                }
            });
        }

        public void requestStreamVolume() {
            executeOrPostRunnableOnMainThread(new Runnable() { // from class: android.media.tv.interactive.TvInteractiveAppService.Session.9
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onRequestStreamVolume();
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInteractiveAppService.TAG, "error in requestStreamVolume", e);
                    }
                }
            });
        }

        public void requestTrackInfoList() {
            executeOrPostRunnableOnMainThread(new Runnable() { // from class: android.media.tv.interactive.TvInteractiveAppService.Session.10
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onRequestTrackInfoList();
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInteractiveAppService.TAG, "error in requestTrackInfoList", e);
                    }
                }
            });
        }

        public void requestCurrentTvInputId() {
            executeOrPostRunnableOnMainThread(new Runnable() { // from class: android.media.tv.interactive.TvInteractiveAppService.Session.11
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onRequestCurrentTvInputId();
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInteractiveAppService.TAG, "error in requestCurrentTvInputId", e);
                    }
                }
            });
        }

        public void requestSigning(final String signingId, final String algorithm, final String alias, final byte[] data) {
            executeOrPostRunnableOnMainThread(new Runnable() { // from class: android.media.tv.interactive.TvInteractiveAppService.Session.12
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onRequestSigning(signingId, algorithm, alias, data);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInteractiveAppService.TAG, "error in requestSigning", e);
                    }
                }
            });
        }

        public void requestAd(final AdRequest request) {
            executeOrPostRunnableOnMainThread(new Runnable() { // from class: android.media.tv.interactive.TvInteractiveAppService.Session.13
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onAdRequest(request);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInteractiveAppService.TAG, "error in requestAd", e);
                    }
                }
            });
        }

        void startInteractiveApp() {
            onStartInteractiveApp();
        }

        void stopInteractiveApp() {
            onStopInteractiveApp();
        }

        void resetInteractiveApp() {
            onResetInteractiveApp();
        }

        void createBiInteractiveApp(Uri biIAppUri, Bundle params) {
            onCreateBiInteractiveAppRequest(biIAppUri, params);
        }

        void destroyBiInteractiveApp(String biIAppId) {
            onDestroyBiInteractiveAppRequest(biIAppId);
        }

        void setTeletextAppEnabled(boolean enable) {
            onSetTeletextAppEnabled(enable);
        }

        void sendCurrentChannelUri(Uri channelUri) {
            onCurrentChannelUri(channelUri);
        }

        void sendCurrentChannelLcn(int lcn) {
            onCurrentChannelLcn(lcn);
        }

        void sendStreamVolume(float volume) {
            onStreamVolume(volume);
        }

        void sendTrackInfoList(List<TvTrackInfo> tracks) {
            onTrackInfoList(tracks);
        }

        void sendCurrentTvInputId(String inputId) {
            onCurrentTvInputId(inputId);
        }

        void sendSigningResult(String signingId, byte[] result) {
            onSigningResult(signingId, result);
        }

        void notifyError(String errMsg, Bundle params) {
            onError(errMsg, params);
        }

        void release() {
            onRelease();
            Surface surface = this.mSurface;
            if (surface != null) {
                surface.release();
                this.mSurface = null;
            }
            synchronized (this.mLock) {
                this.mSessionCallback = null;
                this.mPendingActions.clear();
            }
            removeMediaView(true);
        }

        void notifyTuned(Uri channelUri) {
            onTuned(channelUri);
        }

        void notifyTrackSelected(int type, String trackId) {
            onTrackSelected(type, trackId);
        }

        void notifyTracksChanged(List<TvTrackInfo> tracks) {
            onTracksChanged(tracks);
        }

        void notifyVideoAvailable() {
            onVideoAvailable();
        }

        void notifyVideoUnavailable(int reason) {
            onVideoUnavailable(reason);
        }

        void notifyContentAllowed() {
            onContentAllowed();
        }

        void notifyContentBlocked(TvContentRating rating) {
            onContentBlocked(rating);
        }

        void notifySignalStrength(int strength) {
            onSignalStrength(strength);
        }

        void notifyBroadcastInfoResponse(BroadcastInfoResponse response) {
            onBroadcastInfoResponse(response);
        }

        void notifyAdResponse(AdResponse response) {
            onAdResponse(response);
        }

        public void notifySessionStateChanged(final int state, final int err) {
            executeOrPostRunnableOnMainThread(new Runnable() { // from class: android.media.tv.interactive.TvInteractiveAppService.Session.14
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onSessionStateChanged(state, err);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInteractiveAppService.TAG, "error in notifySessionStateChanged", e);
                    }
                }
            });
        }

        public final void notifyBiInteractiveAppCreated(final Uri biIAppUri, final String biIAppId) {
            executeOrPostRunnableOnMainThread(new Runnable() { // from class: android.media.tv.interactive.TvInteractiveAppService.Session.15
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onBiInteractiveAppCreated(biIAppUri, biIAppId);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInteractiveAppService.TAG, "error in notifyBiInteractiveAppCreated", e);
                    }
                }
            });
        }

        public final void notifyTeletextAppStateChanged(final int state) {
            executeOrPostRunnableOnMainThread(new Runnable() { // from class: android.media.tv.interactive.TvInteractiveAppService.Session.16
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onTeletextAppStateChanged(state);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInteractiveAppService.TAG, "error in notifyTeletextAppState", e);
                    }
                }
            });
        }

        int dispatchInputEvent(InputEvent event, InputEventReceiver receiver) {
            if (event instanceof KeyEvent) {
                KeyEvent keyEvent = (KeyEvent) event;
                return keyEvent.dispatch(this, this.mDispatcherState, this) ? 1 : 0;
            } else if (event instanceof MotionEvent) {
                MotionEvent motionEvent = (MotionEvent) event;
                int source = motionEvent.getSource();
                return motionEvent.isTouchEvent() ? onTouchEvent(motionEvent) ? 1 : 0 : (source & 4) != 0 ? onTrackballEvent(motionEvent) ? 1 : 0 : onGenericMotionEvent(motionEvent) ? 1 : 0;
            } else {
                return 0;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void initialize(ITvInteractiveAppSessionCallback callback) {
            synchronized (this.mLock) {
                this.mSessionCallback = callback;
                for (Runnable runnable : this.mPendingActions) {
                    runnable.run();
                }
                this.mPendingActions.clear();
            }
        }

        void setSurface(Surface surface) {
            onSetSurface(surface);
            Surface surface2 = this.mSurface;
            if (surface2 != null) {
                surface2.release();
            }
            this.mSurface = surface;
        }

        void dispatchSurfaceChanged(int format, int width, int height) {
            onSurfaceChanged(format, width, height);
        }

        private void executeOrPostRunnableOnMainThread(Runnable action) {
            synchronized (this.mLock) {
                if (this.mSessionCallback == null) {
                    this.mPendingActions.add(action);
                } else if (this.mHandler.getLooper().isCurrentThread()) {
                    action.run();
                } else {
                    this.mHandler.post(action);
                }
            }
        }

        void createMediaView(IBinder windowToken, Rect frame) {
            if (this.mMediaViewContainer != null) {
                removeMediaView(false);
            }
            this.mWindowToken = windowToken;
            this.mMediaFrame = frame;
            onMediaViewSizeChanged(frame.right - frame.left, frame.bottom - frame.top);
            if (!this.mMediaViewEnabled) {
                return;
            }
            View onCreateMediaView = onCreateMediaView();
            this.mMediaView = onCreateMediaView;
            if (onCreateMediaView == null) {
                return;
            }
            MediaViewCleanUpTask mediaViewCleanUpTask = this.mMediaViewCleanUpTask;
            if (mediaViewCleanUpTask != null) {
                mediaViewCleanUpTask.cancel(true);
                this.mMediaViewCleanUpTask = null;
            }
            FrameLayout frameLayout = new FrameLayout(this.mContext.getApplicationContext());
            this.mMediaViewContainer = frameLayout;
            frameLayout.addView(this.mMediaView);
            int flags = ActivityManager.isHighEndGfx() ? 536 | 16777216 : 536;
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(frame.right - frame.left, frame.bottom - frame.top, frame.left, frame.top, 1001, flags, -2);
            this.mWindowParams = layoutParams;
            layoutParams.privateFlags |= 64;
            this.mWindowParams.gravity = 8388659;
            this.mWindowParams.token = windowToken;
            this.mWindowManager.addView(this.mMediaViewContainer, this.mWindowParams);
        }

        void relayoutMediaView(Rect frame) {
            Rect rect = this.mMediaFrame;
            if (rect == null || rect.width() != frame.width() || this.mMediaFrame.height() != frame.height()) {
                onMediaViewSizeChanged(frame.right - frame.left, frame.bottom - frame.top);
            }
            this.mMediaFrame = frame;
            if (!this.mMediaViewEnabled || this.mMediaViewContainer == null) {
                return;
            }
            this.mWindowParams.x = frame.left;
            this.mWindowParams.y = frame.top;
            this.mWindowParams.width = frame.right - frame.left;
            this.mWindowParams.height = frame.bottom - frame.top;
            this.mWindowManager.updateViewLayout(this.mMediaViewContainer, this.mWindowParams);
        }

        void removeMediaView(boolean clearWindowToken) {
            if (clearWindowToken) {
                this.mWindowToken = null;
                this.mMediaFrame = null;
            }
            FrameLayout frameLayout = this.mMediaViewContainer;
            if (frameLayout != null) {
                frameLayout.removeView(this.mMediaView);
                this.mMediaView = null;
                this.mWindowManager.removeView(this.mMediaViewContainer);
                this.mMediaViewContainer = null;
                this.mWindowParams = null;
            }
        }

        void scheduleMediaViewCleanup() {
            View mediaViewParent = this.mMediaViewContainer;
            if (mediaViewParent != null) {
                MediaViewCleanUpTask mediaViewCleanUpTask = new MediaViewCleanUpTask();
                this.mMediaViewCleanUpTask = mediaViewCleanUpTask;
                mediaViewCleanUpTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mediaViewParent);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class MediaViewCleanUpTask extends AsyncTask<View, Void, Void> {
        private MediaViewCleanUpTask() {
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public Void doInBackground(View... views) {
            View mediaViewParent = views[0];
            try {
                Thread.sleep(5000L);
                if (!isCancelled() && mediaViewParent.isAttachedToWindow()) {
                    Log.e(TvInteractiveAppService.TAG, "Time out on releasing media view. Killing " + mediaViewParent.getContext().getPackageName());
                    Process.killProcess(Process.myPid());
                }
                return null;
            } catch (InterruptedException e) {
                return null;
            }
        }
    }

    /* loaded from: classes2.dex */
    public static class ITvInteractiveAppSessionWrapper extends ITvInteractiveAppSession.Stub {
        private InputChannel mChannel;
        private TvInteractiveAppEventReceiver mReceiver;
        private final Session mSessionImpl;

        public ITvInteractiveAppSessionWrapper(Context context, Session mSessionImpl, InputChannel channel) {
            this.mSessionImpl = mSessionImpl;
            this.mChannel = channel;
            if (channel != null) {
                this.mReceiver = new TvInteractiveAppEventReceiver(channel, context.getMainLooper());
            }
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void startInteractiveApp() {
            this.mSessionImpl.startInteractiveApp();
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void stopInteractiveApp() {
            this.mSessionImpl.stopInteractiveApp();
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void resetInteractiveApp() {
            this.mSessionImpl.resetInteractiveApp();
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void createBiInteractiveApp(Uri biIAppUri, Bundle params) {
            this.mSessionImpl.createBiInteractiveApp(biIAppUri, params);
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void setTeletextAppEnabled(boolean enable) {
            this.mSessionImpl.setTeletextAppEnabled(enable);
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void destroyBiInteractiveApp(String biIAppId) {
            this.mSessionImpl.destroyBiInteractiveApp(biIAppId);
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void sendCurrentChannelUri(Uri channelUri) {
            this.mSessionImpl.sendCurrentChannelUri(channelUri);
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void sendCurrentChannelLcn(int lcn) {
            this.mSessionImpl.sendCurrentChannelLcn(lcn);
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void sendStreamVolume(float volume) {
            this.mSessionImpl.sendStreamVolume(volume);
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void sendTrackInfoList(List<TvTrackInfo> tracks) {
            this.mSessionImpl.sendTrackInfoList(tracks);
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void sendCurrentTvInputId(String inputId) {
            this.mSessionImpl.sendCurrentTvInputId(inputId);
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void sendSigningResult(String signingId, byte[] result) {
            this.mSessionImpl.sendSigningResult(signingId, result);
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void notifyError(String errMsg, Bundle params) {
            this.mSessionImpl.notifyError(errMsg, params);
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void release() {
            this.mSessionImpl.scheduleMediaViewCleanup();
            this.mSessionImpl.release();
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void notifyTuned(Uri channelUri) {
            this.mSessionImpl.notifyTuned(channelUri);
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void notifyTrackSelected(int type, String trackId) {
            this.mSessionImpl.notifyTrackSelected(type, trackId);
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void notifyTracksChanged(List<TvTrackInfo> tracks) {
            this.mSessionImpl.notifyTracksChanged(tracks);
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void notifyVideoAvailable() {
            this.mSessionImpl.notifyVideoAvailable();
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void notifyVideoUnavailable(int reason) {
            this.mSessionImpl.notifyVideoUnavailable(reason);
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void notifyContentAllowed() {
            this.mSessionImpl.notifyContentAllowed();
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void notifyContentBlocked(String rating) {
            this.mSessionImpl.notifyContentBlocked(TvContentRating.unflattenFromString(rating));
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void notifySignalStrength(int strength) {
            this.mSessionImpl.notifySignalStrength(strength);
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void setSurface(Surface surface) {
            this.mSessionImpl.setSurface(surface);
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void dispatchSurfaceChanged(int format, int width, int height) {
            this.mSessionImpl.dispatchSurfaceChanged(format, width, height);
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void notifyBroadcastInfoResponse(BroadcastInfoResponse response) {
            this.mSessionImpl.notifyBroadcastInfoResponse(response);
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void notifyAdResponse(AdResponse response) {
            this.mSessionImpl.notifyAdResponse(response);
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void createMediaView(IBinder windowToken, Rect frame) {
            this.mSessionImpl.createMediaView(windowToken, frame);
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void relayoutMediaView(Rect frame) {
            this.mSessionImpl.relayoutMediaView(frame);
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void removeMediaView() {
            this.mSessionImpl.removeMediaView(true);
        }

        /* loaded from: classes2.dex */
        private final class TvInteractiveAppEventReceiver extends InputEventReceiver {
            TvInteractiveAppEventReceiver(InputChannel inputChannel, Looper looper) {
                super(inputChannel, looper);
            }

            @Override // android.view.InputEventReceiver
            public void onInputEvent(InputEvent event) {
                if (ITvInteractiveAppSessionWrapper.this.mSessionImpl == null) {
                    finishInputEvent(event, false);
                    return;
                }
                int handled = ITvInteractiveAppSessionWrapper.this.mSessionImpl.dispatchInputEvent(event, this);
                if (handled != -1) {
                    finishInputEvent(event, handled == 1);
                }
            }
        }
    }

    /* loaded from: classes2.dex */
    private final class ServiceHandler extends Handler {
        private static final int DO_CREATE_SESSION = 1;
        private static final int DO_NOTIFY_RTE_STATE_CHANGED = 3;
        private static final int DO_NOTIFY_SESSION_CREATED = 2;

        private ServiceHandler() {
        }

        private void broadcastRteStateChanged(int type, int state, int error) {
            int n = TvInteractiveAppService.this.mCallbacks.beginBroadcast();
            for (int i = 0; i < n; i++) {
                try {
                    ((ITvInteractiveAppServiceCallback) TvInteractiveAppService.this.mCallbacks.getBroadcastItem(i)).onStateChanged(type, state, error);
                } catch (RemoteException e) {
                    Log.e(TvInteractiveAppService.TAG, "error in broadcastRteStateChanged", e);
                }
            }
            TvInteractiveAppService.this.mCallbacks.finishBroadcast();
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    SomeArgs args = (SomeArgs) msg.obj;
                    InputChannel channel = (InputChannel) args.arg1;
                    ITvInteractiveAppSessionCallback cb = (ITvInteractiveAppSessionCallback) args.arg2;
                    String iAppServiceId = (String) args.arg3;
                    int type = ((Integer) args.arg4).intValue();
                    args.recycle();
                    Session sessionImpl = TvInteractiveAppService.this.onCreateSession(iAppServiceId, type);
                    if (sessionImpl == null) {
                        try {
                            cb.onSessionCreated(null);
                            return;
                        } catch (RemoteException e) {
                            Log.e(TvInteractiveAppService.TAG, "error in onSessionCreated", e);
                            return;
                        }
                    }
                    ITvInteractiveAppSession stub = new ITvInteractiveAppSessionWrapper(TvInteractiveAppService.this, sessionImpl, channel);
                    SomeArgs someArgs = SomeArgs.obtain();
                    someArgs.arg1 = sessionImpl;
                    someArgs.arg2 = stub;
                    someArgs.arg3 = cb;
                    TvInteractiveAppService.this.mServiceHandler.obtainMessage(2, someArgs).sendToTarget();
                    return;
                case 2:
                    SomeArgs args2 = (SomeArgs) msg.obj;
                    Session sessionImpl2 = (Session) args2.arg1;
                    ITvInteractiveAppSession stub2 = (ITvInteractiveAppSession) args2.arg2;
                    ITvInteractiveAppSessionCallback cb2 = (ITvInteractiveAppSessionCallback) args2.arg3;
                    try {
                        cb2.onSessionCreated(stub2);
                    } catch (RemoteException e2) {
                        Log.e(TvInteractiveAppService.TAG, "error in onSessionCreated", e2);
                    }
                    if (sessionImpl2 != null) {
                        sessionImpl2.initialize(cb2);
                    }
                    args2.recycle();
                    return;
                case 3:
                    SomeArgs args3 = (SomeArgs) msg.obj;
                    int type2 = ((Integer) args3.arg1).intValue();
                    int state = ((Integer) args3.arg2).intValue();
                    int error = ((Integer) args3.arg3).intValue();
                    broadcastRteStateChanged(type2, state, error);
                    return;
                default:
                    Log.w(TvInteractiveAppService.TAG, "Unhandled message code: " + msg.what);
                    return;
            }
        }
    }
}
