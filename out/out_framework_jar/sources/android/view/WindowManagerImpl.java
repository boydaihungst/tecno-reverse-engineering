package android.view;

import android.app.ResourcesManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.StrictMode;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowManagerImpl;
import android.window.ITaskFpsCallback;
import android.window.TaskFpsCallback;
import android.window.WindowProvider;
import android.window.WindowProviderService;
import com.android.internal.os.IResultReceiver;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
/* loaded from: classes3.dex */
public final class WindowManagerImpl implements WindowManager {
    public final Context mContext;
    private IBinder mDefaultToken;
    private final WindowManagerGlobal mGlobal;
    private final ArrayList<OnFpsCallbackListenerProxy> mOnFpsCallbackListenerProxies;
    private final Window mParentWindow;
    private final IBinder mWindowContextToken;

    public WindowManagerImpl(Context context) {
        this(context, null, null);
    }

    private WindowManagerImpl(Context context, Window parentWindow, IBinder windowContextToken) {
        this.mGlobal = WindowManagerGlobal.getInstance();
        this.mOnFpsCallbackListenerProxies = new ArrayList<>();
        this.mContext = context;
        this.mParentWindow = parentWindow;
        this.mWindowContextToken = windowContextToken;
    }

    public WindowManagerImpl createLocalWindowManager(Window parentWindow) {
        return new WindowManagerImpl(this.mContext, parentWindow, this.mWindowContextToken);
    }

    public WindowManagerImpl createPresentationWindowManager(Context displayContext) {
        return new WindowManagerImpl(displayContext, this.mParentWindow, this.mWindowContextToken);
    }

    public static WindowManager createWindowContextWindowManager(Context context) {
        IBinder clientToken = context.getWindowContextToken();
        return new WindowManagerImpl(context, null, clientToken);
    }

    public void setDefaultToken(IBinder token) {
        this.mDefaultToken = token;
    }

    @Override // android.view.ViewManager
    public void addView(View view, ViewGroup.LayoutParams params) {
        applyTokens(params);
        this.mGlobal.addView(view, params, this.mContext.getDisplayNoVerify(), this.mParentWindow, this.mContext.getUserId());
    }

    @Override // android.view.ViewManager
    public void updateViewLayout(View view, ViewGroup.LayoutParams params) {
        applyTokens(params);
        this.mGlobal.updateViewLayout(view, params);
    }

    private void applyTokens(ViewGroup.LayoutParams params) {
        if (!(params instanceof WindowManager.LayoutParams)) {
            throw new IllegalArgumentException("Params must be WindowManager.LayoutParams");
        }
        WindowManager.LayoutParams wparams = (WindowManager.LayoutParams) params;
        assertWindowContextTypeMatches(wparams.type);
        if (this.mDefaultToken != null && this.mParentWindow == null && wparams.token == null) {
            wparams.token = this.mDefaultToken;
        }
        wparams.mWindowContextToken = this.mWindowContextToken;
    }

    private void assertWindowContextTypeMatches(int windowType) {
        Context context = this.mContext;
        if (!(context instanceof WindowProvider)) {
            return;
        }
        if (windowType >= 1000 && windowType <= 1999) {
            return;
        }
        WindowProvider windowProvider = (WindowProvider) context;
        if (windowProvider.getWindowType() == windowType) {
            return;
        }
        IllegalArgumentException exception = new IllegalArgumentException("Window type mismatch. Window Context's window type is " + windowProvider.getWindowType() + ", while LayoutParams' type is set to " + windowType + ". Please create another Window Context via createWindowContext(getDisplay(), " + windowType + ", null) to add window with type:" + windowType);
        if (!WindowProviderService.isWindowProviderService(windowProvider.getWindowContextOptions())) {
            throw exception;
        }
        StrictMode.onIncorrectContextUsed("WindowContext's window type must match type in WindowManager.LayoutParams", exception);
    }

    @Override // android.view.ViewManager
    public void removeView(View view) {
        this.mGlobal.removeView(view, false);
    }

    @Override // android.view.WindowManager
    public void removeViewImmediate(View view) {
        this.mGlobal.removeView(view, true);
    }

    @Override // android.view.WindowManager
    public void requestAppKeyboardShortcuts(final WindowManager.KeyboardShortcutsReceiver receiver, int deviceId) {
        IResultReceiver resultReceiver = new IResultReceiver.Stub() { // from class: android.view.WindowManagerImpl.1
            @Override // com.android.internal.os.IResultReceiver
            public void send(int resultCode, Bundle resultData) throws RemoteException {
                List<KeyboardShortcutGroup> result = resultData.getParcelableArrayList(WindowManager.PARCEL_KEY_SHORTCUTS_ARRAY);
                receiver.onKeyboardShortcutsReceived(result);
            }
        };
        try {
            WindowManagerGlobal.getWindowManagerService().requestAppKeyboardShortcuts(resultReceiver, deviceId);
        } catch (RemoteException e) {
        }
    }

    @Override // android.view.WindowManager
    public Display getDefaultDisplay() {
        return this.mContext.getDisplayNoVerify();
    }

    @Override // android.view.WindowManager
    public Region getCurrentImeTouchRegion() {
        try {
            return WindowManagerGlobal.getWindowManagerService().getCurrentImeTouchRegion();
        } catch (RemoteException e) {
            return null;
        }
    }

    @Override // android.view.WindowManager
    public void setShouldShowWithInsecureKeyguard(int displayId, boolean shouldShow) {
        try {
            WindowManagerGlobal.getWindowManagerService().setShouldShowWithInsecureKeyguard(displayId, shouldShow);
        } catch (RemoteException e) {
        }
    }

    @Override // android.view.WindowManager
    public void setShouldShowSystemDecors(int displayId, boolean shouldShow) {
        try {
            WindowManagerGlobal.getWindowManagerService().setShouldShowSystemDecors(displayId, shouldShow);
        } catch (RemoteException e) {
        }
    }

    @Override // android.view.WindowManager
    public boolean shouldShowSystemDecors(int displayId) {
        try {
            return WindowManagerGlobal.getWindowManagerService().shouldShowSystemDecors(displayId);
        } catch (RemoteException e) {
            return false;
        }
    }

    @Override // android.view.WindowManager
    public void setDisplayImePolicy(int displayId, int imePolicy) {
        try {
            WindowManagerGlobal.getWindowManagerService().setDisplayImePolicy(displayId, imePolicy);
        } catch (RemoteException e) {
        }
    }

    @Override // android.view.WindowManager
    public int getDisplayImePolicy(int displayId) {
        try {
            return WindowManagerGlobal.getWindowManagerService().getDisplayImePolicy(displayId);
        } catch (RemoteException e) {
            return 1;
        }
    }

    @Override // android.view.WindowManager
    public WindowMetrics getCurrentWindowMetrics() {
        Window window = this.mParentWindow;
        Context context = window != null ? window.getContext() : this.mContext;
        Rect bounds = getCurrentBounds(context);
        return new WindowMetrics(bounds, computeWindowInsets(bounds));
    }

    private static Rect getCurrentBounds(Context context) {
        Rect bounds;
        synchronized (ResourcesManager.getInstance()) {
            bounds = context.getResources().getConfiguration().windowConfiguration.getBounds();
        }
        return bounds;
    }

    @Override // android.view.WindowManager
    public WindowMetrics getMaximumWindowMetrics() {
        Window window = this.mParentWindow;
        Context context = window != null ? window.getContext() : this.mContext;
        Rect maxBounds = getMaximumBounds(context);
        return new WindowMetrics(maxBounds, computeWindowInsets(maxBounds));
    }

    private static Rect getMaximumBounds(Context context) {
        Rect maxBounds;
        synchronized (ResourcesManager.getInstance()) {
            maxBounds = context.getResources().getConfiguration().windowConfiguration.getMaxBounds();
        }
        return maxBounds;
    }

    private WindowInsets computeWindowInsets(Rect bounds) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        Window window = this.mParentWindow;
        Context context = window != null ? window.getContext() : this.mContext;
        params.token = Context.getToken(context);
        return getWindowInsetsFromServerForCurrentDisplay(params, bounds);
    }

    private WindowInsets getWindowInsetsFromServerForCurrentDisplay(WindowManager.LayoutParams attrs, Rect bounds) {
        Configuration config = this.mContext.getResources().getConfiguration();
        return getWindowInsetsFromServerForDisplay(this.mContext.getDisplayId(), attrs, bounds, config.isScreenRound(), config.windowConfiguration.getWindowingMode());
    }

    private static WindowInsets getWindowInsetsFromServerForDisplay(int displayId, WindowManager.LayoutParams attrs, Rect bounds, boolean isScreenRound, int windowingMode) {
        try {
            InsetsState insetsState = new InsetsState();
            try {
                boolean alwaysConsumeSystemBars = WindowManagerGlobal.getWindowManagerService().getWindowInsets(attrs, displayId, insetsState);
                return insetsState.calculateInsets(bounds, null, isScreenRound, alwaysConsumeSystemBars, 48, attrs.flags, 0, attrs.type, windowingMode, null);
            } catch (RemoteException e) {
                e = e;
                throw e.rethrowFromSystemServer();
            }
        } catch (RemoteException e2) {
            e = e2;
        }
    }

    @Override // android.view.WindowManager
    public Set<WindowMetrics> getPossibleMaximumWindowMetrics(int displayId) {
        try {
            List<DisplayInfo> possibleDisplayInfos = WindowManagerGlobal.getWindowManagerService().getPossibleDisplayInfo(displayId, this.mContext.getPackageName());
            Set<WindowMetrics> maxMetrics = new HashSet<>();
            WindowManager.LayoutParams params = new WindowManager.LayoutParams();
            for (int i = 0; i < possibleDisplayInfos.size(); i++) {
                DisplayInfo currentDisplayInfo = possibleDisplayInfos.get(i);
                Rect maxBounds = new Rect(0, 0, currentDisplayInfo.logicalWidth, currentDisplayInfo.logicalHeight);
                boolean isScreenRound = (currentDisplayInfo.flags & 16) != 0;
                WindowInsets windowInsets = getWindowInsetsFromServerForDisplay(currentDisplayInfo.displayId, params, new Rect(0, 0, currentDisplayInfo.getNaturalWidth(), currentDisplayInfo.getNaturalHeight()), isScreenRound, 1);
                maxMetrics.add(new WindowMetrics(maxBounds, new WindowInsets.Builder(windowInsets).setRoundedCorners(currentDisplayInfo.roundedCorners).setDisplayCutout(currentDisplayInfo.displayCutout).build()));
            }
            return maxMetrics;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override // android.view.WindowManager
    public void holdLock(IBinder token, int durationMs) {
        try {
            WindowManagerGlobal.getWindowManagerService().holdLock(token, durationMs);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override // android.view.WindowManager
    public boolean isCrossWindowBlurEnabled() {
        return CrossWindowBlurListeners.getInstance().isCrossWindowBlurEnabled();
    }

    @Override // android.view.WindowManager
    public void addCrossWindowBlurEnabledListener(Consumer<Boolean> listener) {
        addCrossWindowBlurEnabledListener(this.mContext.getMainExecutor(), listener);
    }

    @Override // android.view.WindowManager
    public void addCrossWindowBlurEnabledListener(Executor executor, Consumer<Boolean> listener) {
        CrossWindowBlurListeners.getInstance().addListener(executor, listener);
    }

    @Override // android.view.WindowManager
    public void removeCrossWindowBlurEnabledListener(Consumer<Boolean> listener) {
        CrossWindowBlurListeners.getInstance().removeListener(listener);
    }

    @Override // android.view.WindowManager
    public boolean isTaskSnapshotSupported() {
        try {
            return WindowManagerGlobal.getWindowManagerService().isTaskSnapshotSupported();
        } catch (RemoteException e) {
            return false;
        }
    }

    @Override // android.view.WindowManager
    public void registerTaskFpsCallback(int taskId, Executor executor, TaskFpsCallback callback) {
        OnFpsCallbackListenerProxy onFpsCallbackListenerProxy = new OnFpsCallbackListenerProxy(executor, callback);
        try {
            WindowManagerGlobal.getWindowManagerService().registerTaskFpsCallback(taskId, onFpsCallbackListenerProxy);
            synchronized (this.mOnFpsCallbackListenerProxies) {
                this.mOnFpsCallbackListenerProxies.add(onFpsCallbackListenerProxy);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override // android.view.WindowManager
    public void unregisterTaskFpsCallback(TaskFpsCallback callback) {
        synchronized (this.mOnFpsCallbackListenerProxies) {
            Iterator<OnFpsCallbackListenerProxy> iterator = this.mOnFpsCallbackListenerProxies.iterator();
            while (iterator.hasNext()) {
                OnFpsCallbackListenerProxy proxy = iterator.next();
                if (proxy.mCallback == callback) {
                    try {
                        WindowManagerGlobal.getWindowManagerService().unregisterTaskFpsCallback(proxy);
                        iterator.remove();
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public static class OnFpsCallbackListenerProxy extends ITaskFpsCallback.Stub {
        private final TaskFpsCallback mCallback;
        private final Executor mExecutor;

        private OnFpsCallbackListenerProxy(Executor executor, TaskFpsCallback callback) {
            this.mExecutor = executor;
            this.mCallback = callback;
        }

        @Override // android.window.ITaskFpsCallback
        public void onFpsReported(final float fps) {
            this.mExecutor.execute(new Runnable() { // from class: android.view.WindowManagerImpl$OnFpsCallbackListenerProxy$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    WindowManagerImpl.OnFpsCallbackListenerProxy.this.m5169x5de374e4(fps);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onFpsReported$0$android-view-WindowManagerImpl$OnFpsCallbackListenerProxy  reason: not valid java name */
        public /* synthetic */ void m5169x5de374e4(float fps) {
            this.mCallback.onFpsReported(fps);
        }
    }

    @Override // android.view.WindowManager
    public Bitmap snapshotTaskForRecents(int taskId) {
        try {
            return WindowManagerGlobal.getWindowManagerService().snapshotTaskForRecents(taskId);
        } catch (RemoteException e) {
            e.rethrowAsRuntimeException();
            return null;
        }
    }
}
