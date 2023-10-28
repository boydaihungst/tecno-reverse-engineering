package com.android.server;

import android.app.ActivityThread;
import android.app.ContextImpl;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.util.Slog;
/* loaded from: classes.dex */
public class WallpaperUpdateReceiver extends BroadcastReceiver {
    private static final boolean DEBUG = false;
    private static final String TAG = "WallpaperUpdateReceiver";

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        if (intent != null && "android.intent.action.DEVICE_CUSTOMIZATION_READY".equals(intent.getAction())) {
            AsyncTask.execute(new Runnable() { // from class: com.android.server.WallpaperUpdateReceiver$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    WallpaperUpdateReceiver.this.updateWallpaper();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateWallpaper() {
        try {
            ActivityThread currentActivityThread = ActivityThread.currentActivityThread();
            ContextImpl systemUiContext = currentActivityThread.getSystemUiContext();
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(systemUiContext);
            if (isUserSetWallpaper(wallpaperManager, systemUiContext)) {
                Slog.i(TAG, "User has set wallpaper, skip to resetting");
                return;
            }
            Bitmap blank = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8);
            wallpaperManager.setBitmap(blank);
            wallpaperManager.setResource(17302150);
        } catch (Exception e) {
            Slog.w(TAG, "Failed to customize system wallpaper." + e);
        }
    }

    private boolean isUserSetWallpaper(WallpaperManager wm, Context context) {
        WallpaperInfo info = wm.getWallpaperInfo();
        if (info == null) {
            ParcelFileDescriptor sysWallpaper = wm.getWallpaperFile(1);
            ParcelFileDescriptor lockWallpaper = wm.getWallpaperFile(2);
            if (sysWallpaper != null || lockWallpaper != null) {
                return true;
            }
            return false;
        }
        ComponentName currCN = info.getComponent();
        ComponentName defaultCN = WallpaperManager.getDefaultWallpaperComponent(context);
        if (!currCN.equals(defaultCN)) {
            return true;
        }
        return false;
    }
}
