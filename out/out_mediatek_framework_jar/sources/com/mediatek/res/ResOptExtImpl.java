package com.mediatek.res;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
/* loaded from: classes.dex */
public class ResOptExtImpl extends ResOptExt {
    private static ResOptExtImpl sInstance = null;
    private static Object lock = new Object();

    public static ResOptExtImpl getInstance() {
        if (sInstance == null) {
            synchronized (lock) {
                if (sInstance == null) {
                    sInstance = new ResOptExtImpl();
                }
            }
        }
        return sInstance;
    }

    public void preloadRes(Context context, Resources res) {
        AsyncDrawableCache.getInstance().preloadRes(context, res);
    }

    public Drawable getCachedDrawable(Resources wrapper, long key, int origId) {
        return AsyncDrawableCache.getInstance().getCachedDrawable(wrapper, key, origId);
    }

    public void putCacheList(long key, Drawable dr, int origResId, Context context) {
        AsyncDrawableCache.getInstance().putCacheList(key, dr, origResId, context);
    }
}
