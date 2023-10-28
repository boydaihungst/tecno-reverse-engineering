package android.app;

import android.annotation.SystemApi;
import android.app.IOnProjectionStateChangedListener;
import android.app.IUiModeManager;
import android.app.UiModeManager;
import android.content.Context;
import android.os.Binder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
/* loaded from: classes.dex */
public class UiModeManager {
    @SystemApi
    public static final String ACTION_ENTER_CAR_MODE_PRIORITIZED = "android.app.action.ENTER_CAR_MODE_PRIORITIZED";
    @SystemApi
    public static final String ACTION_EXIT_CAR_MODE_PRIORITIZED = "android.app.action.EXIT_CAR_MODE_PRIORITIZED";
    @SystemApi
    public static final int DEFAULT_PRIORITY = 0;
    public static final int DISABLE_CAR_MODE_ALL_PRIORITIES = 2;
    public static final int DISABLE_CAR_MODE_GO_HOME = 1;
    public static final int ENABLE_CAR_MODE_ALLOW_SLEEP = 2;
    public static final int ENABLE_CAR_MODE_GO_CAR_HOME = 1;
    @SystemApi
    public static final String EXTRA_CALLING_PACKAGE = "android.app.extra.CALLING_PACKAGE";
    @SystemApi
    public static final String EXTRA_PRIORITY = "android.app.extra.PRIORITY";
    public static final int MODE_NIGHT_AUTO = 0;
    public static final int MODE_NIGHT_CUSTOM = 3;
    @SystemApi
    public static final int MODE_NIGHT_CUSTOM_TYPE_BEDTIME = 1;
    @SystemApi
    public static final int MODE_NIGHT_CUSTOM_TYPE_SCHEDULE = 0;
    @SystemApi
    public static final int MODE_NIGHT_CUSTOM_TYPE_UNKNOWN = -1;
    public static final int MODE_NIGHT_NO = 1;
    public static final int MODE_NIGHT_YES = 2;
    @SystemApi
    public static final int PROJECTION_TYPE_ALL = -1;
    @SystemApi
    public static final int PROJECTION_TYPE_AUTOMOTIVE = 1;
    @SystemApi
    public static final int PROJECTION_TYPE_NONE = 0;
    private static final String TAG = "UiModeManager";
    private Context mContext;
    private final Object mLock;
    private final OnProjectionStateChangedListenerResourceManager mOnProjectionStateChangedListenerResourceManager;
    private final Map<OnProjectionStateChangedListener, InnerListener> mProjectionStateListenerMap;
    private IUiModeManager mService;
    public static String ACTION_ENTER_CAR_MODE = "android.app.action.ENTER_CAR_MODE";
    public static String ACTION_EXIT_CAR_MODE = "android.app.action.EXIT_CAR_MODE";
    public static String ACTION_ENTER_DESK_MODE = "android.app.action.ENTER_DESK_MODE";
    public static String ACTION_EXIT_DESK_MODE = "android.app.action.EXIT_DESK_MODE";

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface DisableCarMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface EnableCarMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface NightMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface NightModeCustomReturnType {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface NightModeCustomType {
    }

    @SystemApi
    /* loaded from: classes.dex */
    public interface OnProjectionStateChangedListener {
        void onProjectionStateChanged(int i, Set<String> set);
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface ProjectionType {
    }

    UiModeManager() throws ServiceManager.ServiceNotFoundException {
        this(null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UiModeManager(Context context) throws ServiceManager.ServiceNotFoundException {
        this.mLock = new Object();
        this.mProjectionStateListenerMap = new ArrayMap();
        this.mOnProjectionStateChangedListenerResourceManager = new OnProjectionStateChangedListenerResourceManager();
        this.mService = IUiModeManager.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.UI_MODE_SERVICE));
        this.mContext = context;
    }

    public void enableCarMode(int flags) {
        enableCarMode(0, flags);
    }

    @SystemApi
    public void enableCarMode(int priority, int flags) {
        IUiModeManager iUiModeManager = this.mService;
        if (iUiModeManager != null) {
            try {
                Context context = this.mContext;
                iUiModeManager.enableCarMode(flags, priority, context == null ? null : context.getOpPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void disableCarMode(int flags) {
        IUiModeManager iUiModeManager = this.mService;
        if (iUiModeManager != null) {
            try {
                Context context = this.mContext;
                iUiModeManager.disableCarModeByCallingPackage(flags, context == null ? null : context.getOpPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public int getCurrentModeType() {
        IUiModeManager iUiModeManager = this.mService;
        if (iUiModeManager != null) {
            try {
                return iUiModeManager.getCurrentModeType();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return 1;
    }

    public void setNightMode(int mode) {
        IUiModeManager iUiModeManager = this.mService;
        if (iUiModeManager != null) {
            try {
                iUiModeManager.setNightMode(mode);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    @SystemApi
    public void setNightModeCustomType(int nightModeCustomType) {
        IUiModeManager iUiModeManager = this.mService;
        if (iUiModeManager != null) {
            try {
                iUiModeManager.setNightModeCustomType(nightModeCustomType);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    @SystemApi
    public int getNightModeCustomType() {
        IUiModeManager iUiModeManager = this.mService;
        if (iUiModeManager != null) {
            try {
                return iUiModeManager.getNightModeCustomType();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return -1;
    }

    public void setApplicationNightMode(int mode) {
        IUiModeManager iUiModeManager = this.mService;
        if (iUiModeManager != null) {
            try {
                iUiModeManager.setApplicationNightMode(mode);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public int getNightMode() {
        IUiModeManager iUiModeManager = this.mService;
        if (iUiModeManager != null) {
            try {
                return iUiModeManager.getNightMode();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return -1;
    }

    public boolean isUiModeLocked() {
        IUiModeManager iUiModeManager = this.mService;
        if (iUiModeManager != null) {
            try {
                return iUiModeManager.isUiModeLocked();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return true;
    }

    public boolean isNightModeLocked() {
        IUiModeManager iUiModeManager = this.mService;
        if (iUiModeManager != null) {
            try {
                return iUiModeManager.isNightModeLocked();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return true;
    }

    @SystemApi
    public boolean setNightModeActivatedForCustomMode(int nightModeCustomType, boolean active) {
        IUiModeManager iUiModeManager = this.mService;
        if (iUiModeManager != null) {
            try {
                return iUiModeManager.setNightModeActivatedForCustomMode(nightModeCustomType, active);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public boolean setNightModeActivated(boolean active) {
        IUiModeManager iUiModeManager = this.mService;
        if (iUiModeManager != null) {
            try {
                return iUiModeManager.setNightModeActivated(active);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public LocalTime getCustomNightModeStart() {
        IUiModeManager iUiModeManager = this.mService;
        if (iUiModeManager != null) {
            try {
                return LocalTime.ofNanoOfDay(iUiModeManager.getCustomNightModeStart() * 1000);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return LocalTime.MIDNIGHT;
    }

    public void setCustomNightModeStart(LocalTime time) {
        IUiModeManager iUiModeManager = this.mService;
        if (iUiModeManager != null) {
            try {
                iUiModeManager.setCustomNightModeStart(time.toNanoOfDay() / 1000);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public LocalTime getCustomNightModeEnd() {
        IUiModeManager iUiModeManager = this.mService;
        if (iUiModeManager != null) {
            try {
                return LocalTime.ofNanoOfDay(iUiModeManager.getCustomNightModeEnd() * 1000);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return LocalTime.MIDNIGHT;
    }

    public void setCustomNightModeEnd(LocalTime time) {
        IUiModeManager iUiModeManager = this.mService;
        if (iUiModeManager != null) {
            try {
                iUiModeManager.setCustomNightModeEnd(time.toNanoOfDay() / 1000);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    @SystemApi
    public boolean requestProjection(int projectionType) {
        IUiModeManager iUiModeManager = this.mService;
        if (iUiModeManager != null) {
            try {
                return iUiModeManager.requestProjection(new Binder(), projectionType, this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    @SystemApi
    public boolean releaseProjection(int projectionType) {
        IUiModeManager iUiModeManager = this.mService;
        if (iUiModeManager != null) {
            try {
                return iUiModeManager.releaseProjection(projectionType, this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    @SystemApi
    public Set<String> getProjectingPackages(int projectionType) {
        if (this.mService != null) {
            try {
                return new ArraySet(this.mService.getProjectingPackages(projectionType));
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return Set.of();
    }

    @SystemApi
    public int getActiveProjectionTypes() {
        IUiModeManager iUiModeManager = this.mService;
        if (iUiModeManager != null) {
            try {
                return iUiModeManager.getActiveProjectionTypes();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return 0;
    }

    @SystemApi
    public void addOnProjectionStateChangedListener(int projectionType, Executor executor, OnProjectionStateChangedListener listener) {
        synchronized (this.mLock) {
            if (this.mProjectionStateListenerMap.containsKey(listener)) {
                Slog.i(TAG, "Attempted to add listener that was already added.");
                return;
            }
            if (this.mService != null) {
                InnerListener innerListener = new InnerListener(executor, listener, this.mOnProjectionStateChangedListenerResourceManager);
                try {
                    this.mService.addOnProjectionStateChangedListener(innerListener, projectionType);
                    this.mProjectionStateListenerMap.put(listener, innerListener);
                } catch (RemoteException e) {
                    this.mOnProjectionStateChangedListenerResourceManager.remove(innerListener);
                    throw e.rethrowFromSystemServer();
                }
            }
        }
    }

    @SystemApi
    public void removeOnProjectionStateChangedListener(OnProjectionStateChangedListener listener) {
        synchronized (this.mLock) {
            InnerListener innerListener = this.mProjectionStateListenerMap.get(listener);
            if (innerListener == null) {
                Slog.i(TAG, "Attempted to remove listener that was not added.");
                return;
            }
            IUiModeManager iUiModeManager = this.mService;
            if (iUiModeManager != null) {
                try {
                    iUiModeManager.removeOnProjectionStateChangedListener(innerListener);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
            this.mProjectionStateListenerMap.remove(listener);
            this.mOnProjectionStateChangedListenerResourceManager.remove(innerListener);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class InnerListener extends IOnProjectionStateChangedListener.Stub {
        private final WeakReference<OnProjectionStateChangedListenerResourceManager> mResourceManager;

        private InnerListener(Executor executor, OnProjectionStateChangedListener outerListener, OnProjectionStateChangedListenerResourceManager resourceManager) {
            resourceManager.put(this, executor, outerListener);
            this.mResourceManager = new WeakReference<>(resourceManager);
        }

        @Override // android.app.IOnProjectionStateChangedListener
        public void onProjectionStateChanged(int activeProjectionTypes, List<String> projectingPackages) {
            OnProjectionStateChangedListenerResourceManager resourceManager = this.mResourceManager.get();
            if (resourceManager == null) {
                Slog.w(UiModeManager.TAG, "Can't execute onProjectionStateChanged, resource manager is gone.");
                return;
            }
            OnProjectionStateChangedListener outerListener = resourceManager.getOuterListener(this);
            Executor executor = resourceManager.getExecutor(this);
            if (outerListener == null || executor == null) {
                Slog.w(UiModeManager.TAG, "Can't execute onProjectionStatechanged, references are null.");
            } else {
                executor.execute(PooledLambda.obtainRunnable(new TriConsumer() { // from class: android.app.UiModeManager$InnerListener$$ExternalSyntheticLambda0
                    @Override // com.android.internal.util.function.TriConsumer
                    public final void accept(Object obj, Object obj2, Object obj3) {
                        ((UiModeManager.OnProjectionStateChangedListener) obj).onProjectionStateChanged(((Integer) obj2).intValue(), (ArraySet) obj3);
                    }
                }, outerListener, Integer.valueOf(activeProjectionTypes), new ArraySet(projectingPackages)).recycleOnUse());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class OnProjectionStateChangedListenerResourceManager {
        private final Map<InnerListener, Executor> mExecutorMap;
        private final Map<InnerListener, OnProjectionStateChangedListener> mOuterListenerMap;

        private OnProjectionStateChangedListenerResourceManager() {
            this.mOuterListenerMap = new ArrayMap(1);
            this.mExecutorMap = new ArrayMap(1);
        }

        void put(InnerListener innerListener, Executor executor, OnProjectionStateChangedListener outerListener) {
            this.mOuterListenerMap.put(innerListener, outerListener);
            this.mExecutorMap.put(innerListener, executor);
        }

        void remove(InnerListener innerListener) {
            this.mOuterListenerMap.remove(innerListener);
            this.mExecutorMap.remove(innerListener);
        }

        OnProjectionStateChangedListener getOuterListener(InnerListener innerListener) {
            return this.mOuterListenerMap.get(innerListener);
        }

        Executor getExecutor(InnerListener innerListener) {
            return this.mExecutorMap.get(innerListener);
        }
    }
}
