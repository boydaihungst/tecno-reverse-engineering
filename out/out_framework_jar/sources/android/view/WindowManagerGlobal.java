package android.view;

import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.ArraySet;
import android.util.Log;
import android.view.IWindowManager;
import android.view.IWindowSessionCallback;
import android.view.ViewGroup;
import android.view.ViewRootImpl;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import com.android.internal.util.FastPrintWriter;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
/* loaded from: classes3.dex */
public final class WindowManagerGlobal {
    public static final int ADD_APP_EXITING = -4;
    public static final int ADD_BAD_APP_TOKEN = -1;
    public static final int ADD_BAD_SUBWINDOW_TOKEN = -2;
    public static final int ADD_DUPLICATE_ADD = -5;
    public static final int ADD_FLAG_ALWAYS_CONSUME_SYSTEM_BARS = 4;
    public static final int ADD_FLAG_APP_VISIBLE = 2;
    public static final int ADD_FLAG_IN_TOUCH_MODE = 1;
    public static final int ADD_FLAG_USE_BLAST = 8;
    public static final int ADD_INVALID_DISPLAY = -9;
    public static final int ADD_INVALID_TYPE = -10;
    public static final int ADD_INVALID_USER = -11;
    public static final int ADD_MULTIPLE_SINGLETON = -7;
    public static final int ADD_NOT_APP_TOKEN = -3;
    public static final int ADD_OKAY = 0;
    public static final int ADD_PERMISSION_DENIED = -8;
    public static final int ADD_STARTING_NOT_NEEDED = -6;
    public static final int RELAYOUT_INSETS_PENDING = 1;
    public static final int RELAYOUT_RES_CONSUME_ALWAYS_SYSTEM_BARS = 8;
    public static final int RELAYOUT_RES_FIRST_TIME = 1;
    public static final int RELAYOUT_RES_SURFACE_CHANGED = 2;
    public static final int RELAYOUT_RES_SURFACE_RESIZED = 4;
    private static final String TAG = "WindowManager";
    private static WindowManagerGlobal sDefaultWindowManager;
    private static boolean sUseBLASTAdapter = false;
    private static IWindowManager sWindowManagerService;
    private static IWindowSession sWindowSession;
    private Runnable mSystemPropertyUpdater;
    private final Object mLock = new Object();
    private final ArrayList<View> mViews = new ArrayList<>();
    private final ArrayList<ViewRootImpl> mRoots = new ArrayList<>();
    private final ArrayList<WindowManager.LayoutParams> mParams = new ArrayList<>();
    private final ArraySet<View> mDyingViews = new ArraySet<>();
    private final ArrayList<ViewRootImpl> mWindowlessRoots = new ArrayList<>();

    private WindowManagerGlobal() {
    }

    public static void initialize() {
        getWindowManagerService();
    }

    public static WindowManagerGlobal getInstance() {
        WindowManagerGlobal windowManagerGlobal;
        synchronized (WindowManagerGlobal.class) {
            if (sDefaultWindowManager == null) {
                sDefaultWindowManager = new WindowManagerGlobal();
            }
            windowManagerGlobal = sDefaultWindowManager;
        }
        return windowManagerGlobal;
    }

    public static IWindowManager getWindowManagerService() {
        IWindowManager iWindowManager;
        synchronized (WindowManagerGlobal.class) {
            if (sWindowManagerService == null) {
                IWindowManager asInterface = IWindowManager.Stub.asInterface(ServiceManager.getService(Context.WINDOW_SERVICE));
                sWindowManagerService = asInterface;
                if (asInterface != null) {
                    try {
                        ValueAnimator.setDurationScale(asInterface.getCurrentAnimatorScale());
                        sUseBLASTAdapter = sWindowManagerService.useBLAST();
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
            }
            iWindowManager = sWindowManagerService;
        }
        return iWindowManager;
    }

    public static IWindowSession getWindowSession() {
        IWindowSession iWindowSession;
        synchronized (WindowManagerGlobal.class) {
            if (sWindowSession == null) {
                try {
                    InputMethodManager.ensureDefaultInstanceForDefaultDisplayIfNecessary();
                    IWindowManager windowManager = getWindowManagerService();
                    sWindowSession = windowManager.openSession(new IWindowSessionCallback.Stub() { // from class: android.view.WindowManagerGlobal.1
                        @Override // android.view.IWindowSessionCallback
                        public void onAnimatorScaleChanged(float scale) {
                            ValueAnimator.setDurationScale(scale);
                        }
                    });
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
            iWindowSession = sWindowSession;
        }
        return iWindowSession;
    }

    public static IWindowSession peekWindowSession() {
        IWindowSession iWindowSession;
        synchronized (WindowManagerGlobal.class) {
            iWindowSession = sWindowSession;
        }
        return iWindowSession;
    }

    public static boolean useBLAST() {
        return sUseBLASTAdapter;
    }

    public String[] getViewRootNames() {
        String[] mViewRoots;
        synchronized (this.mLock) {
            int numRoots = this.mRoots.size();
            int windowlessRoots = this.mWindowlessRoots.size();
            mViewRoots = new String[numRoots + windowlessRoots];
            for (int i = 0; i < numRoots; i++) {
                mViewRoots[i] = getWindowName(this.mRoots.get(i));
            }
            for (int i2 = 0; i2 < windowlessRoots; i2++) {
                mViewRoots[i2 + numRoots] = getWindowName(this.mWindowlessRoots.get(i2));
            }
        }
        return mViewRoots;
    }

    public ArrayList<ViewRootImpl> getRootViews(IBinder token) {
        ArrayList<ViewRootImpl> views = new ArrayList<>();
        synchronized (this.mLock) {
            int numRoots = this.mRoots.size();
            for (int i = 0; i < numRoots; i++) {
                WindowManager.LayoutParams params = this.mParams.get(i);
                if (params.token != null) {
                    if (params.token != token) {
                        boolean isChild = false;
                        if (params.type >= 1000 && params.type <= 1999) {
                            int j = 0;
                            while (true) {
                                if (j >= numRoots) {
                                    break;
                                }
                                View viewj = this.mViews.get(j);
                                WindowManager.LayoutParams paramsj = this.mParams.get(j);
                                if (params.token != viewj.getWindowToken() || paramsj.token != token) {
                                    j++;
                                } else {
                                    isChild = true;
                                    break;
                                }
                            }
                        }
                        if (!isChild) {
                        }
                    }
                    views.add(this.mRoots.get(i));
                }
            }
        }
        return views;
    }

    public ArrayList<View> getWindowViews() {
        ArrayList<View> arrayList;
        synchronized (this.mLock) {
            arrayList = new ArrayList<>(this.mViews);
        }
        return arrayList;
    }

    public View getWindowView(IBinder windowToken) {
        synchronized (this.mLock) {
            int numViews = this.mViews.size();
            for (int i = 0; i < numViews; i++) {
                View view = this.mViews.get(i);
                if (view.getWindowToken() == windowToken) {
                    return view;
                }
            }
            return null;
        }
    }

    public View getRootView(String name) {
        synchronized (this.mLock) {
            for (int i = this.mRoots.size() - 1; i >= 0; i--) {
                ViewRootImpl root = this.mRoots.get(i);
                if (name.equals(getWindowName(root))) {
                    return root.getView();
                }
            }
            for (int i2 = this.mWindowlessRoots.size() - 1; i2 >= 0; i2--) {
                ViewRootImpl root2 = this.mWindowlessRoots.get(i2);
                if (name.equals(getWindowName(root2))) {
                    return root2.getView();
                }
            }
            return null;
        }
    }

    public void addView(View view, ViewGroup.LayoutParams params, Display display, Window parentWindow, int userId) {
        ViewRootImpl root;
        if (view == null) {
            throw new IllegalArgumentException("view must not be null");
        }
        if (display == null) {
            throw new IllegalArgumentException("display must not be null");
        }
        if (!(params instanceof WindowManager.LayoutParams)) {
            throw new IllegalArgumentException("Params must be WindowManager.LayoutParams");
        }
        WindowManager.LayoutParams wparams = (WindowManager.LayoutParams) params;
        if (parentWindow != null) {
            parentWindow.adjustLayoutParamsForSubWindow(wparams);
        } else {
            Context context = view.getContext();
            if (context != null && (context.getApplicationInfo().flags & 536870912) != 0) {
                wparams.flags |= 16777216;
            }
        }
        View panelParentView = null;
        synchronized (this.mLock) {
            try {
                try {
                    if (this.mSystemPropertyUpdater == null) {
                        Runnable runnable = new Runnable() { // from class: android.view.WindowManagerGlobal.2
                            @Override // java.lang.Runnable
                            public void run() {
                                synchronized (WindowManagerGlobal.this.mLock) {
                                    for (int i = WindowManagerGlobal.this.mRoots.size() - 1; i >= 0; i--) {
                                        ((ViewRootImpl) WindowManagerGlobal.this.mRoots.get(i)).loadSystemProperties();
                                    }
                                }
                            }
                        };
                        this.mSystemPropertyUpdater = runnable;
                        SystemProperties.addChangeCallback(runnable);
                    }
                    int index = findViewLocked(view, false);
                    if (index >= 0) {
                        if (this.mDyingViews.contains(view)) {
                            this.mRoots.get(index).doDie();
                        } else {
                            throw new IllegalStateException("View " + view + " has already been added to the window manager.");
                        }
                    }
                    if (wparams.type >= 1000 && wparams.type <= 1999) {
                        int count = this.mViews.size();
                        for (int i = 0; i < count; i++) {
                            if (this.mRoots.get(i).mWindow.asBinder() == wparams.token) {
                                panelParentView = this.mViews.get(i);
                            }
                        }
                    }
                    IWindowSession windowlessSession = null;
                    if (wparams.token != null && panelParentView == null) {
                        int i2 = 0;
                        while (true) {
                            if (i2 >= this.mWindowlessRoots.size()) {
                                break;
                            }
                            ViewRootImpl maybeParent = this.mWindowlessRoots.get(i2);
                            if (maybeParent.getWindowToken() != wparams.token) {
                                i2++;
                            } else {
                                windowlessSession = maybeParent.getWindowSession();
                                break;
                            }
                        }
                    }
                    if (windowlessSession == null) {
                        root = new ViewRootImpl(view.getContext(), display);
                    } else {
                        root = new ViewRootImpl(view.getContext(), display, windowlessSession);
                    }
                    view.setLayoutParams(wparams);
                    this.mViews.add(view);
                    this.mRoots.add(root);
                    this.mParams.add(wparams);
                    if (view.getMeasuredWidth() > 65536 || view.getMeasuredHeight() > 65536) {
                        Log.d(TAG, "addView dimension exceeds 65536:MeasuredWidth=" + view.getMeasuredWidth() + "  MeasuredHeighr=" + view.getMeasuredHeight() + "  FLAG_SCALED=" + ((wparams.flags & 16384) != 0));
                    }
                    try {
                        root.setView(view, wparams, panelParentView, userId);
                    } catch (RuntimeException e) {
                        if (index >= 0) {
                            removeViewLocked(index, true);
                        }
                        throw e;
                    }
                } catch (Throwable th) {
                    e = th;
                    throw e;
                }
            } catch (Throwable th2) {
                e = th2;
                throw e;
            }
        }
    }

    public void updateViewLayout(View view, ViewGroup.LayoutParams params) {
        if (view == null) {
            throw new IllegalArgumentException("view must not be null");
        }
        if (!(params instanceof WindowManager.LayoutParams)) {
            throw new IllegalArgumentException("Params must be WindowManager.LayoutParams");
        }
        WindowManager.LayoutParams wparams = (WindowManager.LayoutParams) params;
        view.setLayoutParams(wparams);
        synchronized (this.mLock) {
            int index = findViewLocked(view, true);
            ViewRootImpl root = this.mRoots.get(index);
            this.mParams.remove(index);
            this.mParams.add(index, wparams);
            root.setLayoutParams(wparams, false);
        }
    }

    public void removeView(View view, boolean immediate) {
        if (view == null) {
            throw new IllegalArgumentException("view must not be null");
        }
        synchronized (this.mLock) {
            int index = findViewLocked(view, true);
            View curView = this.mRoots.get(index).getView();
            removeViewLocked(index, immediate);
            if (curView != view) {
                throw new IllegalStateException("Calling with view " + view + " but the ViewAncestor is attached to " + curView);
            }
        }
    }

    public void closeAll(IBinder token, String who, String what) {
        closeAllExceptView(token, null, who, what);
    }

    public void closeAllExceptView(IBinder token, View view, String who, String what) {
        synchronized (this.mLock) {
            int count = this.mViews.size();
            for (int i = 0; i < count; i++) {
                if ((view == null || this.mViews.get(i) != view) && (token == null || this.mParams.get(i).token == token)) {
                    ViewRootImpl root = this.mRoots.get(i);
                    if (who != null) {
                        WindowLeaked leak = new WindowLeaked(what + " " + who + " has leaked window " + root.getView() + " that was originally added here");
                        leak.setStackTrace(root.getLocation().getStackTrace());
                        Log.e(TAG, "", leak);
                    }
                    removeViewLocked(i, false);
                }
            }
        }
    }

    private void removeViewLocked(int index, boolean immediate) {
        ViewRootImpl root = this.mRoots.get(index);
        View view = root.getView();
        if (root != null) {
            root.getImeFocusController().onWindowDismissed();
        }
        boolean deferred = root.die(immediate);
        if (view != null) {
            view.assignParent(null);
            if (deferred) {
                this.mDyingViews.add(view);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void doRemoveView(ViewRootImpl root) {
        boolean allViewsRemoved;
        synchronized (this.mLock) {
            int index = this.mRoots.indexOf(root);
            if (index >= 0) {
                this.mRoots.remove(index);
                this.mParams.remove(index);
                View view = this.mViews.remove(index);
                this.mDyingViews.remove(view);
            }
            allViewsRemoved = this.mRoots.isEmpty();
        }
        if (ThreadedRenderer.sTrimForeground) {
            doTrimForeground();
        }
        if (allViewsRemoved) {
            InsetsAnimationThread.release();
        }
    }

    private int findViewLocked(View view, boolean required) {
        int index = this.mViews.indexOf(view);
        if (required && index < 0) {
            throw new IllegalArgumentException("View=" + view + " not attached to window manager");
        }
        return index;
    }

    public static boolean shouldDestroyEglContext(int trimLevel) {
        if (trimLevel >= 80) {
            return true;
        }
        if (trimLevel >= 60 && !ActivityManager.isHighEndGfx()) {
            return true;
        }
        return false;
    }

    public void trimMemory(int level) {
        if (shouldDestroyEglContext(level)) {
            synchronized (this.mLock) {
                for (int i = this.mRoots.size() - 1; i >= 0; i--) {
                    this.mRoots.get(i).destroyHardwareResources();
                }
            }
            level = 80;
        }
        ThreadedRenderer.trimMemory(level);
        if (ThreadedRenderer.sTrimForeground) {
            doTrimForeground();
        }
    }

    public static void trimForeground() {
        if (ThreadedRenderer.sTrimForeground) {
            WindowManagerGlobal wm = getInstance();
            wm.doTrimForeground();
        }
    }

    private void doTrimForeground() {
        boolean hasVisibleWindows = false;
        synchronized (this.mLock) {
            for (int i = this.mRoots.size() - 1; i >= 0; i--) {
                ViewRootImpl root = this.mRoots.get(i);
                if (root.mView != null && root.getHostVisibility() == 0 && root.mAttachInfo.mThreadedRenderer != null) {
                    hasVisibleWindows = true;
                } else {
                    root.destroyHardwareResources();
                }
            }
        }
        if (!hasVisibleWindows) {
            ThreadedRenderer.trimMemory(80);
        }
    }

    public void dumpGfxInfo(FileDescriptor fd, String[] args) {
        char c;
        char c2;
        FileOutputStream fout = new FileOutputStream(fd);
        PrintWriter pw = new FastPrintWriter(fout);
        try {
            synchronized (this.mLock) {
                try {
                    int count = this.mViews.size();
                    pw.println("Profile data in ms:");
                    int i = 0;
                    while (true) {
                        c = 0;
                        c2 = 1;
                        if (i >= count) {
                            break;
                        }
                        ViewRootImpl root = this.mRoots.get(i);
                        String name = getWindowName(root);
                        pw.printf("\n\t%s (visibility=%d)", name, Integer.valueOf(root.getHostVisibility()));
                        ThreadedRenderer renderer = root.getView().mAttachInfo.mThreadedRenderer;
                        if (renderer != null) {
                            try {
                                renderer.dumpGfxInfo(pw, fd, args);
                            } catch (Throwable th) {
                                th = th;
                                try {
                                    throw th;
                                } catch (Throwable th2) {
                                    th = th2;
                                    pw.flush();
                                    throw th;
                                }
                            }
                        }
                        i++;
                    }
                    pw.println("\nView hierarchy:\n");
                    ViewRootImpl.GfxInfo totals = new ViewRootImpl.GfxInfo();
                    int i2 = 0;
                    while (i2 < count) {
                        ViewRootImpl root2 = this.mRoots.get(i2);
                        ViewRootImpl.GfxInfo info = root2.getGfxInfo();
                        totals.add(info);
                        String name2 = getWindowName(root2);
                        Object[] objArr = new Object[3];
                        objArr[c] = name2;
                        objArr[c2] = Integer.valueOf(info.viewCount);
                        objArr[2] = Float.valueOf(((float) info.renderNodeMemoryUsage) / 1024.0f);
                        pw.printf("  %s\n  %d views, %.2f kB of render nodes", objArr);
                        pw.printf("\n\n", new Object[0]);
                        i2++;
                        c = 0;
                        c2 = 1;
                    }
                    pw.printf("\nTotal %-15s: %d\n", "ViewRootImpl", Integer.valueOf(count));
                    pw.printf("Total %-15s: %d\n", "attached Views", Integer.valueOf(totals.viewCount));
                    pw.printf("Total %-15s: %.2f kB (used) / %.2f kB (capacity)\n\n", "RenderNode", Float.valueOf(((float) totals.renderNodeMemoryUsage) / 1024.0f), Float.valueOf(((float) totals.renderNodeMemoryAllocated) / 1024.0f));
                    pw.flush();
                } catch (Throwable th3) {
                    th = th3;
                }
            }
        } catch (Throwable th4) {
            th = th4;
        }
    }

    private static String getWindowName(ViewRootImpl root) {
        return ((Object) root.mWindowAttributes.getTitle()) + "/" + root.getClass().getName() + '@' + Integer.toHexString(root.hashCode());
    }

    public void setStoppedState(IBinder token, final boolean stopped) {
        ArrayList<ViewRootImpl> nonCurrentThreadRoots = null;
        synchronized (this.mLock) {
            int count = this.mViews.size();
            for (int i = count - 1; i >= 0; i--) {
                if (token == null || this.mParams.get(i).token == token) {
                    ViewRootImpl root = this.mRoots.get(i);
                    if (root.mThread == Thread.currentThread()) {
                        root.setWindowStopped(stopped);
                    } else {
                        if (nonCurrentThreadRoots == null) {
                            nonCurrentThreadRoots = new ArrayList<>();
                        }
                        nonCurrentThreadRoots.add(root);
                    }
                    setStoppedState(root.mAttachInfo.mWindowToken, stopped);
                }
            }
        }
        if (nonCurrentThreadRoots != null) {
            for (int i2 = nonCurrentThreadRoots.size() - 1; i2 >= 0; i2--) {
                final ViewRootImpl root2 = nonCurrentThreadRoots.get(i2);
                root2.mHandler.runWithScissors(new Runnable() { // from class: android.view.WindowManagerGlobal$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        ViewRootImpl.this.setWindowStopped(stopped);
                    }
                }, 0L);
            }
        }
    }

    public void reportNewConfiguration(Configuration config) {
        synchronized (this.mLock) {
            int count = this.mViews.size();
            Configuration config2 = new Configuration(config);
            for (int i = 0; i < count; i++) {
                ViewRootImpl root = this.mRoots.get(i);
                root.requestUpdateConfiguration(config2);
            }
        }
    }

    public void changeCanvasOpacity(IBinder token, boolean opaque) {
        if (token == null) {
            return;
        }
        synchronized (this.mLock) {
            for (int i = this.mParams.size() - 1; i >= 0; i--) {
                if (this.mParams.get(i).token == token) {
                    this.mRoots.get(i).changeCanvasOpacity(opaque);
                    return;
                }
            }
        }
    }

    public SurfaceControl mirrorWallpaperSurface(int displayId) {
        try {
            return getWindowManagerService().mirrorWallpaperSurface(displayId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void addWindowlessRoot(ViewRootImpl impl) {
        synchronized (this.mLock) {
            this.mWindowlessRoots.add(impl);
        }
    }

    public void removeWindowlessRoot(ViewRootImpl impl) {
        synchronized (this.mLock) {
            this.mWindowlessRoots.remove(impl);
        }
    }

    public void setRecentsAppBehindSystemBars(boolean behindSystemBars) {
        try {
            getWindowManagerService().setRecentsAppBehindSystemBars(behindSystemBars);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
