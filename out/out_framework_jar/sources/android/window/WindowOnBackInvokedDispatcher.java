package android.window;

import android.content.Context;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import android.view.IWindow;
import android.view.IWindowSession;
import android.window.IOnBackInvokedCallback;
import android.window.ImeOnBackInvokedDispatcher;
import android.window.WindowOnBackInvokedDispatcher;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.TreeMap;
/* loaded from: classes4.dex */
public class WindowOnBackInvokedDispatcher implements OnBackInvokedDispatcher {
    private static final boolean ALWAYS_ENFORCE_PREDICTIVE_BACK;
    private static final boolean ENABLE_PREDICTIVE_BACK;
    private static final String TAG = "WindowOnBackDispatcher";
    private final Checker mChecker;
    private ImeOnBackInvokedDispatcher mImeDispatcher;
    private IWindow mWindow;
    private IWindowSession mWindowSession;
    private final HashMap<OnBackInvokedCallback, Integer> mAllCallbacks = new HashMap<>();
    private final TreeMap<Integer, ArrayList<OnBackInvokedCallback>> mOnBackInvokedCallbacks = new TreeMap<>();

    static {
        ENABLE_PREDICTIVE_BACK = SystemProperties.getInt("persist.wm.debug.predictive_back", 1) != 0;
        ALWAYS_ENFORCE_PREDICTIVE_BACK = SystemProperties.getInt("persist.wm.debug.predictive_back_always_enforce", 0) != 0;
    }

    public WindowOnBackInvokedDispatcher(boolean applicationCallBackEnabled) {
        this.mChecker = new Checker(applicationCallBackEnabled);
    }

    public void attachToWindow(IWindowSession windowSession, IWindow window) {
        this.mWindowSession = windowSession;
        this.mWindow = window;
        if (!this.mAllCallbacks.isEmpty()) {
            setTopOnBackInvokedCallback(getTopCallback());
        }
    }

    public void detachFromWindow() {
        clear();
        this.mWindow = null;
        this.mWindowSession = null;
    }

    @Override // android.window.OnBackInvokedDispatcher
    public void registerOnBackInvokedCallback(int priority, OnBackInvokedCallback callback) {
        if (this.mChecker.checkApplicationCallbackRegistration(priority, callback)) {
            registerOnBackInvokedCallbackUnchecked(callback, priority);
        }
    }

    public void registerOnBackInvokedCallbackUnchecked(OnBackInvokedCallback callback, int priority) {
        ImeOnBackInvokedDispatcher imeOnBackInvokedDispatcher = this.mImeDispatcher;
        if (imeOnBackInvokedDispatcher != null) {
            imeOnBackInvokedDispatcher.registerOnBackInvokedCallback(priority, callback);
            return;
        }
        if (!this.mOnBackInvokedCallbacks.containsKey(Integer.valueOf(priority))) {
            this.mOnBackInvokedCallbacks.put(Integer.valueOf(priority), new ArrayList<>());
        }
        ArrayList<OnBackInvokedCallback> callbacks = this.mOnBackInvokedCallbacks.get(Integer.valueOf(priority));
        if (this.mAllCallbacks.containsKey(callback)) {
            Integer prevPriority = this.mAllCallbacks.get(callback);
            this.mOnBackInvokedCallbacks.get(prevPriority).remove(callback);
        }
        OnBackInvokedCallback previousTopCallback = getTopCallback();
        callbacks.add(callback);
        this.mAllCallbacks.put(callback, Integer.valueOf(priority));
        if (previousTopCallback == null || (previousTopCallback != callback && this.mAllCallbacks.get(previousTopCallback).intValue() <= priority)) {
            setTopOnBackInvokedCallback(callback);
        }
    }

    @Override // android.window.OnBackInvokedDispatcher
    public void unregisterOnBackInvokedCallback(OnBackInvokedCallback callback) {
        ImeOnBackInvokedDispatcher imeOnBackInvokedDispatcher = this.mImeDispatcher;
        if (imeOnBackInvokedDispatcher != null) {
            imeOnBackInvokedDispatcher.unregisterOnBackInvokedCallback(callback);
        } else if (!this.mAllCallbacks.containsKey(callback)) {
        } else {
            OnBackInvokedCallback previousTopCallback = getTopCallback();
            Integer priority = this.mAllCallbacks.get(callback);
            ArrayList<OnBackInvokedCallback> callbacks = this.mOnBackInvokedCallbacks.get(priority);
            callbacks.remove(callback);
            if (callbacks.isEmpty()) {
                this.mOnBackInvokedCallbacks.remove(priority);
            }
            this.mAllCallbacks.remove(callback);
            if (previousTopCallback == callback) {
                setTopOnBackInvokedCallback(getTopCallback());
            }
        }
    }

    @Override // android.window.OnBackInvokedDispatcher
    public void registerSystemOnBackInvokedCallback(OnBackInvokedCallback callback) {
        registerOnBackInvokedCallbackUnchecked(callback, -1);
    }

    public void clear() {
        ImeOnBackInvokedDispatcher imeOnBackInvokedDispatcher = this.mImeDispatcher;
        if (imeOnBackInvokedDispatcher != null) {
            imeOnBackInvokedDispatcher.clear();
            this.mImeDispatcher = null;
        }
        if (!this.mAllCallbacks.isEmpty()) {
            setTopOnBackInvokedCallback(null);
        }
        this.mAllCallbacks.clear();
        this.mOnBackInvokedCallbacks.clear();
    }

    private void setTopOnBackInvokedCallback(OnBackInvokedCallback callback) {
        IOnBackInvokedCallback iCallback;
        if (this.mWindowSession == null || this.mWindow == null) {
            return;
        }
        OnBackInvokedCallbackInfo callbackInfo = null;
        if (callback != null) {
            try {
                int priority = this.mAllCallbacks.get(callback).intValue();
                if (callback instanceof ImeOnBackInvokedDispatcher.ImeOnBackInvokedCallback) {
                    iCallback = ((ImeOnBackInvokedDispatcher.ImeOnBackInvokedCallback) callback).getIOnBackInvokedCallback();
                } else {
                    iCallback = new OnBackInvokedCallbackWrapper(callback);
                }
                callbackInfo = new OnBackInvokedCallbackInfo(iCallback, priority);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to set OnBackInvokedCallback to WM. Error: " + e);
                return;
            }
        }
        this.mWindowSession.setOnBackInvokedCallbackInfo(this.mWindow, callbackInfo);
    }

    public OnBackInvokedCallback getTopCallback() {
        if (this.mAllCallbacks.isEmpty()) {
            return null;
        }
        for (Integer priority : this.mOnBackInvokedCallbacks.descendingKeySet()) {
            ArrayList<OnBackInvokedCallback> callbacks = this.mOnBackInvokedCallbacks.get(priority);
            if (!callbacks.isEmpty()) {
                return callbacks.get(callbacks.size() - 1);
            }
        }
        return null;
    }

    public Checker getChecker() {
        return this.mChecker;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes4.dex */
    public static class OnBackInvokedCallbackWrapper extends IOnBackInvokedCallback.Stub {
        private final WeakReference<OnBackInvokedCallback> mCallback;

        /* JADX INFO: Access modifiers changed from: package-private */
        public OnBackInvokedCallbackWrapper(OnBackInvokedCallback callback) {
            this.mCallback = new WeakReference<>(callback);
        }

        @Override // android.window.IOnBackInvokedCallback
        public void onBackStarted() {
            Handler.getMain().post(new Runnable() { // from class: android.window.WindowOnBackInvokedDispatcher$OnBackInvokedCallbackWrapper$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    WindowOnBackInvokedDispatcher.OnBackInvokedCallbackWrapper.this.m6319x75d1e9d4();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onBackStarted$0$android-window-WindowOnBackInvokedDispatcher$OnBackInvokedCallbackWrapper  reason: not valid java name */
        public /* synthetic */ void m6319x75d1e9d4() {
            OnBackAnimationCallback callback = getBackAnimationCallback();
            if (callback != null) {
                callback.onBackStarted();
            }
        }

        @Override // android.window.IOnBackInvokedCallback
        public void onBackProgressed(final BackEvent backEvent) {
            Handler.getMain().post(new Runnable() { // from class: android.window.WindowOnBackInvokedDispatcher$OnBackInvokedCallbackWrapper$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    WindowOnBackInvokedDispatcher.OnBackInvokedCallbackWrapper.this.m6318x69ad01c6(backEvent);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onBackProgressed$1$android-window-WindowOnBackInvokedDispatcher$OnBackInvokedCallbackWrapper  reason: not valid java name */
        public /* synthetic */ void m6318x69ad01c6(BackEvent backEvent) {
            OnBackAnimationCallback callback = getBackAnimationCallback();
            if (callback != null) {
                callback.onBackProgressed(backEvent);
            }
        }

        @Override // android.window.IOnBackInvokedCallback
        public void onBackCancelled() {
            Handler.getMain().post(new Runnable() { // from class: android.window.WindowOnBackInvokedDispatcher$OnBackInvokedCallbackWrapper$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    WindowOnBackInvokedDispatcher.OnBackInvokedCallbackWrapper.this.m6316xe1995ce6();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onBackCancelled$2$android-window-WindowOnBackInvokedDispatcher$OnBackInvokedCallbackWrapper  reason: not valid java name */
        public /* synthetic */ void m6316xe1995ce6() {
            OnBackAnimationCallback callback = getBackAnimationCallback();
            if (callback != null) {
                callback.onBackCancelled();
            }
        }

        @Override // android.window.IOnBackInvokedCallback
        public void onBackInvoked() throws RemoteException {
            Handler.getMain().post(new Runnable() { // from class: android.window.WindowOnBackInvokedDispatcher$OnBackInvokedCallbackWrapper$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    WindowOnBackInvokedDispatcher.OnBackInvokedCallbackWrapper.this.m6317xfd875402();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onBackInvoked$3$android-window-WindowOnBackInvokedDispatcher$OnBackInvokedCallbackWrapper  reason: not valid java name */
        public /* synthetic */ void m6317xfd875402() {
            OnBackInvokedCallback callback = this.mCallback.get();
            if (callback == null) {
                return;
            }
            callback.onBackInvoked();
        }

        private OnBackAnimationCallback getBackAnimationCallback() {
            OnBackInvokedCallback callback = this.mCallback.get();
            if (callback instanceof OnBackAnimationCallback) {
                return (OnBackAnimationCallback) callback;
            }
            return null;
        }
    }

    public static boolean isOnBackInvokedCallbackEnabled(Context context) {
        boolean featureFlagEnabled = ENABLE_PREDICTIVE_BACK;
        boolean appRequestsPredictiveBack = context != null && context.getApplicationInfo().isOnBackInvokedCallbackEnabled();
        return featureFlagEnabled && (appRequestsPredictiveBack || ALWAYS_ENFORCE_PREDICTIVE_BACK);
    }

    @Override // android.window.OnBackInvokedDispatcher
    public void setImeOnBackInvokedDispatcher(ImeOnBackInvokedDispatcher imeDispatcher) {
        this.mImeDispatcher = imeDispatcher;
    }

    /* loaded from: classes4.dex */
    public static class Checker {
        private final boolean mApplicationCallBackEnabled;

        public Checker(boolean applicationCallBackEnabled) {
            this.mApplicationCallBackEnabled = applicationCallBackEnabled;
        }

        public boolean checkApplicationCallbackRegistration(int priority, OnBackInvokedCallback callback) {
            if (!this.mApplicationCallBackEnabled && !(callback instanceof CompatOnBackInvokedCallback)) {
                Log.w("OnBackInvokedCallback", "OnBackInvokedCallback is not enabled for the application.\nSet 'android:enableOnBackInvokedCallback=\"true\"' in the application manifest.");
                return false;
            } else if (priority < 0) {
                throw new IllegalArgumentException("Application registered OnBackInvokedCallback cannot have negative priority. Priority: " + priority);
            } else {
                Objects.requireNonNull(callback);
                return true;
            }
        }
    }
}
