package com.android.server.am;

import android.content.pm.ApplicationInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.ArrayMap;
import android.util.LongSparseArray;
import android.util.Pair;
import android.util.Slog;
import com.android.internal.compat.IPlatformCompat;
import com.android.server.compat.CompatChange;
import com.android.server.compat.PlatformCompat;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class PlatformCompatCache {
    static final int CACHED_COMPAT_CHANGE_CAMERA_MICROPHONE_CAPABILITY = 1;
    static final long[] CACHED_COMPAT_CHANGE_IDS_MAPPING = {136274596, 136219221, 183972877};
    static final int CACHED_COMPAT_CHANGE_PROCESS_CAPABILITY = 0;
    static final int CACHED_COMPAT_CHANGE_USE_SHORT_FGS_USAGE_INTERACTION_TIME = 2;
    private static PlatformCompatCache sPlatformCompatCache;
    private final boolean mCacheEnabled;
    private final LongSparseArray<CacheItem> mCaches = new LongSparseArray<>();
    private final IPlatformCompat mIPlatformCompatProxy;
    private final PlatformCompat mPlatformCompat;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    @interface CachedCompatChangeId {
    }

    private PlatformCompatCache(long[] compatChanges) {
        IBinder b = ServiceManager.getService("platform_compat");
        if (b instanceof PlatformCompat) {
            this.mPlatformCompat = (PlatformCompat) ServiceManager.getService("platform_compat");
            for (long changeId : compatChanges) {
                this.mCaches.put(changeId, new CacheItem(this.mPlatformCompat, changeId));
            }
            this.mIPlatformCompatProxy = null;
            this.mCacheEnabled = true;
            return;
        }
        this.mIPlatformCompatProxy = IPlatformCompat.Stub.asInterface(b);
        this.mPlatformCompat = null;
        this.mCacheEnabled = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static PlatformCompatCache getInstance() {
        if (sPlatformCompatCache == null) {
            sPlatformCompatCache = new PlatformCompatCache(new long[]{136274596, 136219221, 183972877});
        }
        return sPlatformCompatCache;
    }

    private boolean isChangeEnabled(long changeId, ApplicationInfo app, boolean defaultValue) {
        try {
            return this.mCacheEnabled ? this.mCaches.get(changeId).isChangeEnabled(app) : this.mIPlatformCompatProxy.isChangeEnabled(changeId, app);
        } catch (RemoteException e) {
            Slog.w("ActivityManager", "Error reading platform compat change " + changeId, e);
            return defaultValue;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isChangeEnabled(int cachedCompatChangeId, ApplicationInfo app, boolean defaultValue) {
        return getInstance().isChangeEnabled(CACHED_COMPAT_CHANGE_IDS_MAPPING[cachedCompatChangeId], app, defaultValue);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void invalidate(ApplicationInfo app) {
        for (int i = this.mCaches.size() - 1; i >= 0; i--) {
            this.mCaches.valueAt(i).invalidate(app);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onApplicationInfoChanged(ApplicationInfo app) {
        for (int i = this.mCaches.size() - 1; i >= 0; i--) {
            this.mCaches.valueAt(i).onApplicationInfoChanged(app);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class CacheItem implements CompatChange.ChangeListener {
        private final long mChangeId;
        private final PlatformCompat mPlatformCompat;
        private final Object mLock = new Object();
        private final ArrayMap<String, Pair<Boolean, WeakReference<ApplicationInfo>>> mCache = new ArrayMap<>();

        CacheItem(PlatformCompat platformCompat, long changeId) {
            this.mPlatformCompat = platformCompat;
            this.mChangeId = changeId;
            platformCompat.registerListener(changeId, this);
        }

        boolean isChangeEnabled(ApplicationInfo app) {
            synchronized (this.mLock) {
                int index = this.mCache.indexOfKey(app.packageName);
                if (index < 0) {
                    return fetchLocked(app, index);
                }
                Pair<Boolean, WeakReference<ApplicationInfo>> p = this.mCache.valueAt(index);
                if (((WeakReference) p.second).get() == app) {
                    return ((Boolean) p.first).booleanValue();
                }
                return fetchLocked(app, index);
            }
        }

        void invalidate(ApplicationInfo app) {
            synchronized (this.mLock) {
                this.mCache.remove(app.packageName);
            }
        }

        boolean fetchLocked(ApplicationInfo app, int index) {
            Pair<Boolean, WeakReference<ApplicationInfo>> p = new Pair<>(Boolean.valueOf(this.mPlatformCompat.isChangeEnabledInternalNoLogging(this.mChangeId, app)), new WeakReference(app));
            if (index >= 0) {
                this.mCache.setValueAt(index, p);
            } else {
                this.mCache.put(app.packageName, p);
            }
            return ((Boolean) p.first).booleanValue();
        }

        void onApplicationInfoChanged(ApplicationInfo app) {
            synchronized (this.mLock) {
                int index = this.mCache.indexOfKey(app.packageName);
                if (index >= 0) {
                    fetchLocked(app, index);
                }
            }
        }

        @Override // com.android.server.compat.CompatChange.ChangeListener
        public void onCompatChange(String packageName) {
            synchronized (this.mLock) {
                int index = this.mCache.indexOfKey(packageName);
                if (index >= 0) {
                    ApplicationInfo app = (ApplicationInfo) ((WeakReference) this.mCache.valueAt(index).second).get();
                    if (app != null) {
                        fetchLocked(app, index);
                    } else {
                        this.mCache.removeAt(index);
                    }
                }
            }
        }
    }
}
