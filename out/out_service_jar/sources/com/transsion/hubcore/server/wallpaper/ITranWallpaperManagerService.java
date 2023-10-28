package com.transsion.hubcore.server.wallpaper;

import android.app.WallpaperInfo;
import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import com.transsion.hubcore.server.wallpaper.ITranWallpaperManagerService;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranWallpaperManagerService {
    public static final boolean DEBUG = true;
    public static final String TAG = "ITranWallpaperManagerService";
    public static final TranClassInfo<ITranWallpaperManagerService> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.wallpaper.TranWallpaperManagerServiceImpl", ITranWallpaperManagerService.class, new Supplier() { // from class: com.transsion.hubcore.server.wallpaper.ITranWallpaperManagerService$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranWallpaperManagerService.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranWallpaperManagerService {
    }

    static ITranWallpaperManagerService Instance() {
        return (ITranWallpaperManagerService) classInfo.getImpl();
    }

    default void systemReady(Context context, Handler handler) {
    }

    default void onServiceConnected(WallpaperInfo mInfo, ComponentName name) {
    }
}
