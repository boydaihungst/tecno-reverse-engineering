package com.mediatek.res;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.SystemProperties;
import android.os.UserManager;
import android.util.ArrayMap;
import android.util.Log;
import com.mediatek.boostfwk.utils.Config;
import com.mediatek.custom.CustomProperties;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
/* loaded from: classes.dex */
class AsyncDrawableCache {
    private static final String TAG = "AsyncDrawableCache";
    private static final long sClearCacheTime = 10000;
    private static final String sPerfName = "perf_img_scale";
    private static boolean isDEBUG = false;
    private static ArrayList<String> sPreloadList = new ArrayList<String>() { // from class: com.mediatek.res.AsyncDrawableCache.1
        {
            add("com.bbl.mobilebanking");
            add("air.tv.douyu.android");
        }
    };
    private static AsyncDrawableCache mAsyncDrawableCache = null;
    private static final String sResolutionEnableProp = "ro.vendor.pref_scale_enable_cfg";
    private static String sFeatureConfig = SystemProperties.get(sResolutionEnableProp, Config.USER_CONFIG_DEFAULT_TYPE);
    private static final String sDefResolution = "720";
    private static String sResolution = sDefResolution;
    private static final ArrayMap<Long, Drawable.ConstantState> sDrawableCache = new ArrayMap<>();
    private static ArrayMap<String, Integer> sResolutionList = new ArrayMap<>();
    private static boolean isPreloaded = false;
    private static Object lock = new Object();

    AsyncDrawableCache() {
        sResolutionList.put("480", 307200);
        sResolutionList.put(sDefResolution, 921600);
        sResolutionList.put("1080", 2073600);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static AsyncDrawableCache getInstance() {
        if (mAsyncDrawableCache == null) {
            synchronized (AsyncDrawableCache.class) {
                if (mAsyncDrawableCache == null) {
                    mAsyncDrawableCache = new AsyncDrawableCache();
                }
            }
        }
        return mAsyncDrawableCache;
    }

    boolean skipPreload(Context context) {
        String pkg;
        return context == null || (pkg = context.getPackageName()) == null || sPreloadList.contains(pkg);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void preloadRes(final Context context, Resources res) {
        final SharedPreferences prefs;
        if (!isEnableFeature() || skipPreload(context) || !isUserUnlocked(context) || (prefs = context.getSharedPreferences(sPerfName, 0)) == null || prefs.getAll().size() == 0) {
            return;
        }
        isPreloaded = true;
        AsyncTask.execute(new Runnable() { // from class: com.mediatek.res.AsyncDrawableCache$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                AsyncDrawableCache.lambda$preloadRes$0(prefs, context);
            }
        });
        clearCacheAfterPreload();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$preloadRes$0(SharedPreferences prefs, Context context) {
        Map<String, ?> map = prefs.getAll();
        for (Map.Entry<String, ?> stringEntry : map.entrySet()) {
            stringEntry.getKey();
            int value = ((Integer) stringEntry.getValue()).intValue();
            if (isDEBUG) {
                Log.d(TAG, "resource=" + value + ", res obj=" + context.getResources().getImpl());
            }
            try {
                context.getDrawable(value);
            } catch (Resources.NotFoundException e) {
                Log.w(TAG, "can not found res: " + value + ", maybe dynamic res id.");
            }
        }
        if (isDEBUG) {
            Log.d(TAG, "preloadRes, end of preloadRes");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Drawable getCachedDrawable(Resources wrapper, long key, int origId) {
        Drawable.ConstantState boostCache;
        if (isEnableFeature()) {
            synchronized (lock) {
                boostCache = sDrawableCache.get(Long.valueOf(key));
            }
            if (boostCache != null) {
                return boostCache.newDrawable(wrapper);
            }
            return null;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void putCacheList(long key, Drawable dr, int origResId, Context context) {
        ArrayMap<Long, Drawable.ConstantState> arrayMap;
        Drawable.ConstantState boostCache;
        if (!isEnableFeature() || skipPreload(context) || context.getApplicationInfo().processName.equals(CustomProperties.MODULE_SYSTEM) || context.getApplicationInfo().isSystemApp()) {
            return;
        }
        synchronized (lock) {
            arrayMap = sDrawableCache;
            boostCache = arrayMap.get(Long.valueOf(key));
        }
        if (boostCache == null && context != null && needCacheDrawable(dr)) {
            if (isPreloaded) {
                synchronized (lock) {
                    arrayMap.put(Long.valueOf(key), dr.getConstantState());
                }
                if (isDEBUG) {
                    Log.d(TAG, "putCacheList, put cache, size:" + arrayMap.size());
                }
            }
            if (isDEBUG) {
                Log.d(TAG, "putCacheList, key:" + key + ", origResId:" + origResId);
            }
            storeDrawableId(origResId, context);
        }
    }

    private boolean needCacheDrawable(Drawable dr) {
        Integer scaleResolution = sResolutionList.get(sResolution);
        int drResolution = dr.getMinimumWidth() * dr.getMinimumHeight();
        if (drResolution < scaleResolution.intValue()) {
            return false;
        }
        if (isDEBUG) {
            Log.d(TAG, "computeResolution, drResolution:" + drResolution + ", scaleResolution:" + scaleResolution);
        }
        return true;
    }

    private void storeDrawableId(int origResId, Context context) {
        SharedPreferences prefs;
        if (context == null) {
            Log.w(TAG, "storeDrawableId got the context is null, id:" + origResId + " cannot save");
        } else if (isUserUnlocked(context) && (prefs = context.getSharedPreferences(sPerfName, 0)) != null && !prefs.contains(String.valueOf(origResId))) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(String.valueOf(origResId), origResId);
            editor.commit();
            if (isDEBUG) {
                Log.d(TAG, "storeDrawableId, id:" + origResId);
            }
        }
    }

    private boolean isEnableFeature() {
        if (sFeatureConfig.equals("0")) {
            isDEBUG = false;
            return false;
        } else if (sFeatureConfig.equals(Config.USER_CONFIG_DEFAULT_TYPE)) {
            isDEBUG = false;
            return true;
        } else if (!sFeatureConfig.equals("2")) {
            return false;
        } else {
            isDEBUG = true;
            return true;
        }
    }

    private boolean isUserUnlocked(Context context) {
        UserManager userManager = (UserManager) context.getSystemService("user");
        if (!userManager.isUserUnlocked()) {
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clearCache() {
        ArrayMap<Long, Drawable.ConstantState> arrayMap = sDrawableCache;
        if (arrayMap != null && arrayMap.size() != 0) {
            synchronized (lock) {
                arrayMap.clear();
            }
        }
        if (isDEBUG) {
            Log.d(TAG, "clearCache, cache size:" + arrayMap.size());
        }
    }

    private void clearCacheAfterPreload() {
        if (isPreloaded) {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() { // from class: com.mediatek.res.AsyncDrawableCache.2
                @Override // java.util.TimerTask, java.lang.Runnable
                public void run() {
                    AsyncDrawableCache.this.clearCache();
                    AsyncDrawableCache.isPreloaded = false;
                }
            }, sClearCacheTime);
        }
    }
}
