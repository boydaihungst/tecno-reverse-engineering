package com.android.server.wallpaper;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.ILocalWallpaperColorConsumer;
import android.app.IWallpaperManager;
import android.app.IWallpaperManagerCallback;
import android.app.PendingIntent;
import android.app.UserSwitchObserver;
import android.app.WallpaperColors;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.display.DisplayManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.FileObserver;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.IRemoteCallback;
import android.os.ParcelFileDescriptor;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SELinux;
import android.os.ShellCallback;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.service.wallpaper.IWallpaperConnection;
import android.service.wallpaper.IWallpaperEngine;
import android.service.wallpaper.IWallpaperService;
import android.system.ErrnoException;
import android.system.Os;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.EventLog;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import android.view.Display;
import android.view.DisplayInfo;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FunctionalUtils;
import com.android.internal.util.JournaledFile;
import com.android.server.EventLogTags;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.NVUtils;
import com.android.server.SystemService;
import com.android.server.am.HostingRecord;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserManagerInternal;
import com.android.server.utils.TimingsTraceAndSlog;
import com.android.server.wallpaper.WallpaperManagerService;
import com.android.server.wm.WindowManagerInternal;
import com.transsion.hubcore.server.wallpaper.ITranWallpaperManagerService;
import com.transsion.wallpaper.TranWallpaperExtFacotry;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class WallpaperManagerService extends IWallpaperManager.Stub implements IWallpaperManagerService {
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_CROP = true;
    private static final boolean DEBUG_LIVE = true;
    private static final int MAX_WALLPAPER_COMPONENT_LOG_LENGTH = 128;
    private static final long MIN_WALLPAPER_CRASH_TIME = 10000;
    private static final String RECORD_FILE = "decode_record";
    private static final String RECORD_LOCK_FILE = "decode_lock_record";
    private static final String TAG = "WallpaperManagerService";
    private final ActivityManager mActivityManager;
    private final AppOpsManager mAppOpsManager;
    private WallpaperColors mCacheDefaultImageWallpaperColors;
    private final SparseArray<SparseArray<RemoteCallbackList<IWallpaperManagerCallback>>> mColorsChangedListeners;
    private final Context mContext;
    private int mCurrentUserId;
    private final ComponentName mDefaultWallpaperComponent;
    private Runnable mDestoryEngineRunnable;
    private SparseArray<DisplayData> mDisplayDatas;
    private final DisplayManager.DisplayListener mDisplayListener;
    private final DisplayManager mDisplayManager;
    protected WallpaperData mFallbackWallpaper;
    private final IPackageManager mIPackageManager;
    private final ComponentName mImageWallpaper;
    private boolean mInAmbientMode;
    private IWallpaperManagerCallback mKeyguardListener;
    private int mLastDisplayId;
    private IWallpaperEngine mLastEngine;
    private Binder mLastToken;
    protected WallpaperData mLastWallpaper;
    private LocalColorRepository mLocalColorRepo;
    private final Object mLock = new Object();
    private final SparseArray<WallpaperData> mLockWallpaperMap;
    private final MyPackageMonitor mMonitor;
    private boolean mShuttingDown;
    private final SparseBooleanArray mUserRestorecon;
    private boolean mWaitingForUnlock;
    private int mWallpaperId;
    private final SparseArray<WallpaperData> mWallpaperMap;
    private final WindowManagerInternal mWindowManagerInternal;
    private static final RectF LOCAL_COLOR_BOUNDS = new RectF(0.0f, 0.0f, 1.0f, 1.0f);
    private static final boolean DEFAULT_THEME_LOCAL_SUPPORT = "1".equals(SystemProperties.get("ro.product.default_theme_local_support", "0"));
    private static final boolean OS_SET_DEFAULT_DYNAMIC_WALLPAPER_SUPPORT = "1".equals(SystemProperties.get("ro.os_set_default_dynamic_wallpaper_support", "0"));
    static final String WALLPAPER = "wallpaper_orig";
    static final String WALLPAPER_CROP = "wallpaper";
    static final String WALLPAPER_LOCK_ORIG = "wallpaper_lock_orig";
    static final String WALLPAPER_LOCK_CROP = "wallpaper_lock";
    static final String WALLPAPER_INFO = "wallpaper_info.xml";
    private static final String[] sPerUserFiles = {WALLPAPER, WALLPAPER_CROP, WALLPAPER_LOCK_ORIG, WALLPAPER_LOCK_CROP, WALLPAPER_INFO};
    private static final HashMap<Integer, String> sWallpaperType = new HashMap<Integer, String>() { // from class: com.android.server.wallpaper.WallpaperManagerService.5
        {
            put(1, WallpaperManagerService.RECORD_FILE);
            put(2, WallpaperManagerService.RECORD_LOCK_FILE);
        }
    };

    /* loaded from: classes2.dex */
    public static class Lifecycle extends SystemService {
        private IWallpaperManagerService mService;

        public Lifecycle(Context context) {
            super(context);
        }

        @Override // com.android.server.SystemService
        public void onStart() {
            try {
                IWallpaperManagerService iWallpaperManagerService = (IWallpaperManagerService) Class.forName(getContext().getResources().getString(17040059)).getConstructor(Context.class).newInstance(getContext());
                this.mService = iWallpaperManagerService;
                publishBinderService(WallpaperManagerService.WALLPAPER_CROP, iWallpaperManagerService);
            } catch (Exception exp) {
                Slog.wtf(WallpaperManagerService.TAG, "Failed to instantiate WallpaperManagerService", exp);
            }
        }

        @Override // com.android.server.SystemService
        public void onBootPhase(int phase) {
            IWallpaperManagerService iWallpaperManagerService = this.mService;
            if (iWallpaperManagerService != null) {
                iWallpaperManagerService.onBootPhase(phase);
            }
        }

        @Override // com.android.server.SystemService
        public void onUserUnlocking(SystemService.TargetUser user) {
            IWallpaperManagerService iWallpaperManagerService = this.mService;
            if (iWallpaperManagerService != null) {
                iWallpaperManagerService.onUnlockUser(user.getUserIdentifier());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class WallpaperObserver extends FileObserver {
        final int mUserId;
        final WallpaperData mWallpaper;
        final File mWallpaperDir;
        final File mWallpaperFile;
        final File mWallpaperLockFile;

        public WallpaperObserver(WallpaperData wallpaper) {
            super(WallpaperManagerService.this.getWallpaperDir(wallpaper.userId).getAbsolutePath(), 1672);
            this.mUserId = wallpaper.userId;
            File wallpaperDir = WallpaperManagerService.this.getWallpaperDir(wallpaper.userId);
            this.mWallpaperDir = wallpaperDir;
            this.mWallpaper = wallpaper;
            this.mWallpaperFile = new File(wallpaperDir, WallpaperManagerService.WALLPAPER);
            this.mWallpaperLockFile = new File(wallpaperDir, WallpaperManagerService.WALLPAPER_LOCK_ORIG);
        }

        WallpaperData dataForEvent(boolean sysChanged, boolean lockChanged) {
            WallpaperData wallpaper = null;
            synchronized (WallpaperManagerService.this.mLock) {
                if (lockChanged) {
                    try {
                        wallpaper = (WallpaperData) WallpaperManagerService.this.mLockWallpaperMap.get(this.mUserId);
                    } catch (Throwable th) {
                        throw th;
                    }
                }
                if (wallpaper == null) {
                    wallpaper = (WallpaperData) WallpaperManagerService.this.mWallpaperMap.get(this.mUserId);
                }
            }
            return wallpaper != null ? wallpaper : this.mWallpaper;
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [378=4] */
        @Override // android.os.FileObserver
        public void onEvent(int event, String path) {
            WallpaperData wallpaper;
            int i;
            WallpaperData wallpaper2;
            if (path == null) {
                return;
            }
            boolean moved = event == 128;
            boolean written = event == 8 || moved;
            File changedFile = new File(this.mWallpaperDir, path);
            boolean sysWallpaperChanged = this.mWallpaperFile.equals(changedFile);
            boolean lockWallpaperChanged = this.mWallpaperLockFile.equals(changedFile);
            int notifyColorsWhich = 0;
            final WallpaperData wallpaper3 = dataForEvent(sysWallpaperChanged, lockWallpaperChanged);
            if (moved && lockWallpaperChanged) {
                SELinux.restorecon(changedFile);
                WallpaperManagerService.this.notifyLockWallpaperChanged();
                WallpaperManagerService.this.notifyWallpaperColorsChanged(wallpaper3, 2);
                return;
            }
            synchronized (WallpaperManagerService.this.mLock) {
                try {
                    if (sysWallpaperChanged || lockWallpaperChanged) {
                        try {
                            WallpaperManagerService.this.notifyCallbacksLocked(wallpaper3);
                            if (wallpaper3.wallpaperComponent != null && event == 8) {
                                try {
                                    if (!wallpaper3.imageWallpaperPending) {
                                        wallpaper = wallpaper3;
                                    }
                                } catch (Throwable th) {
                                    th = th;
                                    throw th;
                                }
                            }
                            if (written) {
                                SELinux.restorecon(changedFile);
                                if (moved) {
                                    WallpaperManagerService.this.loadSettingsLocked(wallpaper3.userId, true);
                                }
                                WallpaperManagerService.this.generateCrop(wallpaper3);
                                wallpaper3.imageWallpaperPending = false;
                                if (sysWallpaperChanged) {
                                    try {
                                        IRemoteCallback iRemoteCallback = new IRemoteCallback.Stub() { // from class: com.android.server.wallpaper.WallpaperManagerService.WallpaperObserver.1
                                            public void sendResult(Bundle data) throws RemoteException {
                                                WallpaperManagerService.this.notifyWallpaperChanged(wallpaper3);
                                            }
                                        };
                                        WallpaperManagerService wallpaperManagerService = WallpaperManagerService.this;
                                        i = 2;
                                        wallpaper2 = wallpaper3;
                                        try {
                                            wallpaperManagerService.bindWallpaperComponentLocked(wallpaperManagerService.mImageWallpaper, true, false, wallpaper2, iRemoteCallback);
                                            notifyColorsWhich = 0 | 1;
                                        } catch (Throwable th2) {
                                            th = th2;
                                            throw th;
                                        }
                                    } catch (Throwable th3) {
                                        th = th3;
                                    }
                                } else {
                                    i = 2;
                                    wallpaper2 = wallpaper3;
                                }
                                if (lockWallpaperChanged) {
                                    wallpaper = wallpaper2;
                                } else {
                                    wallpaper = wallpaper2;
                                    if ((wallpaper.whichPending & i) != 0) {
                                    }
                                    WallpaperManagerService.this.saveSettingsLocked(wallpaper.userId);
                                    if (lockWallpaperChanged && !sysWallpaperChanged) {
                                        WallpaperManagerService.this.notifyWallpaperChanged(wallpaper);
                                    }
                                }
                                if (!lockWallpaperChanged) {
                                    WallpaperManagerService.this.mLockWallpaperMap.remove(wallpaper.userId);
                                }
                                WallpaperManagerService.this.notifyLockWallpaperChanged();
                                notifyColorsWhich |= 2;
                                WallpaperManagerService.this.saveSettingsLocked(wallpaper.userId);
                                if (lockWallpaperChanged) {
                                    WallpaperManagerService.this.notifyWallpaperChanged(wallpaper);
                                }
                            } else {
                                wallpaper = wallpaper3;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                        }
                    } else {
                        wallpaper = wallpaper3;
                    }
                    if (notifyColorsWhich != 0) {
                        WallpaperManagerService.this.notifyWallpaperColorsChanged(wallpaper, notifyColorsWhich);
                    }
                } catch (Throwable th5) {
                    th = th5;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyWallpaperChanged(WallpaperData wallpaper) {
        if (wallpaper.setComplete != null) {
            try {
                wallpaper.setComplete.onWallpaperChanged();
            } catch (RemoteException e) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyLockWallpaperChanged() {
        IWallpaperManagerCallback cb = this.mKeyguardListener;
        if (cb != null) {
            try {
                cb.onWallpaperChanged();
            } catch (RemoteException e) {
            }
        }
    }

    void notifyWallpaperColorsChanged(WallpaperData wallpaper, int which) {
        notifyWallpaperColorsChanged(wallpaper, which, false);
    }

    void notifyWallpaperColorsChanged(final WallpaperData wallpaper, final int which, final boolean cleanDefault) {
        if (wallpaper.connection != null) {
            wallpaper.connection.forEachDisplayConnector(new Consumer() { // from class: com.android.server.wallpaper.WallpaperManagerService$$ExternalSyntheticLambda18
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    WallpaperManagerService.this.m7695xd9c7fcf(wallpaper, which, cleanDefault, (WallpaperManagerService.WallpaperConnection.DisplayConnector) obj);
                }
            });
        } else {
            notifyWallpaperColorsChangedOnDisplay(wallpaper, which, 0, cleanDefault);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyWallpaperColorsChanged$0$com-android-server-wallpaper-WallpaperManagerService  reason: not valid java name */
    public /* synthetic */ void m7695xd9c7fcf(WallpaperData wallpaper, int which, boolean cleanDefault, WallpaperConnection.DisplayConnector connector) {
        notifyWallpaperColorsChangedOnDisplay(wallpaper, which, connector.mDisplayId, cleanDefault);
    }

    private RemoteCallbackList<IWallpaperManagerCallback> getWallpaperCallbacks(int userId, int displayId) {
        SparseArray<RemoteCallbackList<IWallpaperManagerCallback>> displayListeners = this.mColorsChangedListeners.get(userId);
        if (displayListeners == null) {
            return null;
        }
        RemoteCallbackList<IWallpaperManagerCallback> listeners = displayListeners.get(displayId);
        return listeners;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyWallpaperColorsChangedOnDisplay(WallpaperData wallpaper, int which, int displayId) {
        notifyWallpaperColorsChangedOnDisplay(wallpaper, which, displayId, false);
    }

    /* JADX WARN: Removed duplicated region for block: B:20:0x002d  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void notifyWallpaperColorsChangedOnDisplay(WallpaperData wallpaper, int which, int displayId, boolean cleanDefault) {
        boolean z;
        boolean needsExtraction;
        synchronized (this.mLock) {
            RemoteCallbackList<IWallpaperManagerCallback> currentUserColorListeners = getWallpaperCallbacks(wallpaper.userId, displayId);
            RemoteCallbackList<IWallpaperManagerCallback> userAllColorListeners = getWallpaperCallbacks(-1, displayId);
            if (emptyCallbackList(currentUserColorListeners) && emptyCallbackList(userAllColorListeners)) {
                return;
            }
            if (wallpaper.primaryColors != null && !wallpaper.mIsColorExtractedFromDim) {
                z = false;
                needsExtraction = z;
                if (needsExtraction) {
                    extractColors(wallpaper);
                }
                notifyColorListeners(getAdjustedWallpaperColorsOnDimming(wallpaper), which, wallpaper.userId, displayId);
            }
            z = true;
            needsExtraction = z;
            if (needsExtraction) {
            }
            notifyColorListeners(getAdjustedWallpaperColorsOnDimming(wallpaper), which, wallpaper.userId, displayId);
        }
    }

    private static <T extends IInterface> boolean emptyCallbackList(RemoteCallbackList<T> list) {
        return list == null || list.getRegisteredCallbackCount() == 0;
    }

    private void notifyColorListeners(WallpaperColors wallpaperColors, int which, int userId, int displayId) {
        IWallpaperManagerCallback keyguardListener;
        ArrayList<IWallpaperManagerCallback> colorListeners = new ArrayList<>();
        synchronized (this.mLock) {
            RemoteCallbackList<IWallpaperManagerCallback> currentUserColorListeners = getWallpaperCallbacks(userId, displayId);
            RemoteCallbackList<IWallpaperManagerCallback> userAllColorListeners = getWallpaperCallbacks(-1, displayId);
            keyguardListener = this.mKeyguardListener;
            if (currentUserColorListeners != null) {
                int count = currentUserColorListeners.beginBroadcast();
                for (int i = 0; i < count; i++) {
                    colorListeners.add(currentUserColorListeners.getBroadcastItem(i));
                }
                currentUserColorListeners.finishBroadcast();
            }
            if (userAllColorListeners != null) {
                int count2 = userAllColorListeners.beginBroadcast();
                for (int i2 = 0; i2 < count2; i2++) {
                    colorListeners.add(userAllColorListeners.getBroadcastItem(i2));
                }
                userAllColorListeners.finishBroadcast();
            }
        }
        int count3 = colorListeners.size();
        for (int i3 = 0; i3 < count3; i3++) {
            try {
                colorListeners.get(i3).onWallpaperColorsChanged(wallpaperColors, which, userId);
            } catch (RemoteException e) {
            }
        }
        if (keyguardListener != null && displayId == 0) {
            try {
                keyguardListener.onWallpaperColorsChanged(wallpaperColors, which, userId);
            } catch (RemoteException e2) {
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:58:0x008a  */
    /* JADX WARN: Removed duplicated region for block: B:60:0x0092  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void extractColors(WallpaperData wallpaper) {
        int wallpaperId;
        float dimAmount;
        WallpaperColors colors;
        String cropFile = null;
        boolean defaultImageWallpaper = false;
        synchronized (this.mLock) {
            boolean z = false;
            wallpaper.mIsColorExtractedFromDim = false;
        }
        if (wallpaper.equals(this.mFallbackWallpaper)) {
            synchronized (this.mLock) {
                if (this.mFallbackWallpaper.primaryColors != null) {
                    return;
                }
                WallpaperColors colors2 = extractDefaultImageWallpaperColors(wallpaper);
                synchronized (this.mLock) {
                    this.mFallbackWallpaper.primaryColors = colors2;
                }
                return;
            }
        }
        synchronized (this.mLock) {
            boolean imageWallpaper = (this.mImageWallpaper.equals(wallpaper.wallpaperComponent) || wallpaper.wallpaperComponent == null) ? true : true;
            if (imageWallpaper && wallpaper.cropFile != null && wallpaper.cropFile.exists()) {
                cropFile = wallpaper.cropFile.getAbsolutePath();
            } else if (imageWallpaper && !wallpaper.cropExists() && !wallpaper.sourceExists()) {
                defaultImageWallpaper = true;
            }
            wallpaperId = wallpaper.wallpaperId;
            dimAmount = wallpaper.mWallpaperDimAmount;
        }
        WallpaperColors colors3 = null;
        if (cropFile != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(cropFile);
            if (bitmap != null) {
                colors3 = WallpaperColors.fromBitmap(bitmap, dimAmount);
                bitmap.recycle();
            }
        } else if (defaultImageWallpaper) {
            colors = extractDefaultImageWallpaperColors(wallpaper);
            if (colors != null) {
                Slog.w(TAG, "Cannot extract colors because wallpaper could not be read.");
                return;
            }
            synchronized (this.mLock) {
                if (wallpaper.wallpaperId == wallpaperId) {
                    wallpaper.primaryColors = colors;
                    saveSettingsLocked(wallpaper.userId);
                } else {
                    Slog.w(TAG, "Not setting primary colors since wallpaper changed");
                }
            }
            return;
        }
        colors = colors3;
        if (colors != null) {
        }
    }

    private WallpaperColors extractDefaultImageWallpaperColors(WallpaperData wallpaper) {
        WallpaperColors colors;
        InputStream is;
        synchronized (this.mLock) {
            WallpaperColors wallpaperColors = this.mCacheDefaultImageWallpaperColors;
            if (wallpaperColors != null) {
                return wallpaperColors;
            }
            float dimAmount = wallpaper.mWallpaperDimAmount;
            WallpaperColors colors2 = null;
            try {
                is = WallpaperManager.openDefaultWallpaper(this.mContext, 1);
                try {
                } catch (Throwable th) {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                    }
                    throw th;
                }
            } catch (IOException e) {
                Slog.w(TAG, "Can't close default wallpaper stream", e);
                colors = null;
            } catch (OutOfMemoryError e2) {
                Slog.w(TAG, "Can't decode default wallpaper stream", e2);
            }
            if (is == null) {
                Slog.w(TAG, "Can't open default wallpaper stream");
                if (is != null) {
                    is.close();
                }
                return null;
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
            if (bitmap != null) {
                colors2 = WallpaperColors.fromBitmap(bitmap, dimAmount);
                bitmap.recycle();
            }
            if (is != null) {
                is.close();
            }
            colors = colors2;
            if (colors == null) {
                Slog.e(TAG, "Extract default image wallpaper colors failed");
            } else {
                synchronized (this.mLock) {
                    this.mCacheDefaultImageWallpaperColors = colors;
                }
            }
            return colors;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [835=13, 840=15] */
    /* JADX WARN: Removed duplicated region for block: B:154:0x0498  */
    /* JADX WARN: Removed duplicated region for block: B:157:0x04aa  */
    /* JADX WARN: Removed duplicated region for block: B:185:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    void generateCrop(WallpaperData wallpaper) {
        boolean success;
        boolean success2;
        BufferedOutputStream bos;
        FileOutputStream f;
        DisplayData wpData = getDisplayDataOrCreate(0);
        Rect cropHint = new Rect(wallpaper.cropHint);
        DisplayInfo displayInfo = new DisplayInfo();
        this.mDisplayManager.getDisplay(0).getDisplayInfo(displayInfo);
        if (wpData.mWidth != displayInfo.logicalWidth || wpData.mHeight != displayInfo.logicalHeight) {
            wpData.mWidth = displayInfo.logicalWidth;
            wpData.mHeight = displayInfo.logicalHeight;
            Slog.v(TAG, "now force changing the wpData");
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(wallpaper.wallpaperFile.getAbsolutePath(), options);
        if (options.outWidth > 0 && options.outHeight > 0) {
            boolean needCrop = false;
            if (cropHint.isEmpty()) {
                cropHint.top = 0;
                cropHint.left = 0;
                cropHint.right = options.outWidth;
                cropHint.bottom = options.outHeight;
            } else {
                cropHint.offset(cropHint.right > options.outWidth ? options.outWidth - cropHint.right : 0, cropHint.bottom > options.outHeight ? options.outHeight - cropHint.bottom : 0);
                if (cropHint.left < 0) {
                    cropHint.left = 0;
                }
                if (cropHint.top < 0) {
                    cropHint.top = 0;
                }
                needCrop = options.outHeight > cropHint.height() || options.outWidth > cropHint.width();
            }
            boolean needScale = cropHint.height() > wpData.mHeight || cropHint.height() > GLHelper.getMaxTextureSize() || cropHint.width() > GLHelper.getMaxTextureSize();
            if (needScale) {
                float scaleByHeight = wpData.mHeight / cropHint.height();
                if (((int) (cropHint.width() * scaleByHeight)) < displayInfo.logicalWidth) {
                    float screenAspectRatio = displayInfo.logicalHeight / displayInfo.logicalWidth;
                    cropHint.bottom = (int) (cropHint.width() * screenAspectRatio);
                    needCrop = true;
                }
            }
            Slog.v(TAG, "crop: w=" + cropHint.width() + " h=" + cropHint.height());
            Slog.v(TAG, "dims: w=" + wpData.mWidth + " h=" + wpData.mHeight);
            Slog.v(TAG, "meas: w=" + options.outWidth + " h=" + options.outHeight);
            Slog.v(TAG, "crop?=" + needCrop + " scale?=" + needScale);
            if (needCrop || needScale) {
                FileOutputStream f2 = null;
                BufferedOutputStream bos2 = null;
                try {
                    int actualScale = cropHint.height() / wpData.mHeight;
                    int scale = 1;
                    while (scale * 2 <= actualScale) {
                        scale *= 2;
                    }
                    options.inSampleSize = scale;
                    options.inJustDecodeBounds = false;
                    final Rect estimateCrop = new Rect(cropHint);
                    success2 = false;
                    try {
                        estimateCrop.scale(1.0f / options.inSampleSize);
                        try {
                            float hRatio = wpData.mHeight / estimateCrop.height();
                            int destHeight = (int) (estimateCrop.height() * hRatio);
                            try {
                                int destWidth = (int) (estimateCrop.width() * hRatio);
                                try {
                                    if (destWidth > GLHelper.getMaxTextureSize()) {
                                        try {
                                            int newHeight = (int) (wpData.mHeight / hRatio);
                                            try {
                                                int newWidth = (int) (wpData.mWidth / hRatio);
                                                estimateCrop.set(cropHint);
                                                try {
                                                    estimateCrop.left += (cropHint.width() - newWidth) / 2;
                                                    estimateCrop.top += (cropHint.height() - newHeight) / 2;
                                                    estimateCrop.right = estimateCrop.left + newWidth;
                                                    estimateCrop.bottom = estimateCrop.top + newHeight;
                                                    cropHint.set(estimateCrop);
                                                    estimateCrop.scale(1.0f / options.inSampleSize);
                                                } catch (Exception e) {
                                                    f2 = null;
                                                    IoUtils.closeQuietly(bos2);
                                                    IoUtils.closeQuietly(f2);
                                                    success = success2;
                                                    if (!success) {
                                                    }
                                                    if (wallpaper.cropFile.exists()) {
                                                    }
                                                } catch (Throwable th) {
                                                    th = th;
                                                    f2 = null;
                                                    IoUtils.closeQuietly(bos2);
                                                    IoUtils.closeQuietly(f2);
                                                    throw th;
                                                }
                                            } catch (Exception e2) {
                                                f2 = null;
                                            } catch (Throwable th2) {
                                                th = th2;
                                                f2 = null;
                                            }
                                        } catch (Exception e3) {
                                            f2 = null;
                                        } catch (Throwable th3) {
                                            th = th3;
                                            f2 = null;
                                        }
                                    }
                                    try {
                                        int safeHeight = (int) (estimateCrop.height() * hRatio);
                                        int safeWidth = (int) (estimateCrop.width() * hRatio);
                                        Slog.v(TAG, "Decode parameters:");
                                        try {
                                            Slog.v(TAG, "  cropHint=" + cropHint + ", estimateCrop=" + estimateCrop);
                                            Slog.v(TAG, "  down sampling=" + options.inSampleSize + ", hRatio=" + hRatio);
                                            Slog.v(TAG, "  dest=" + destWidth + "x" + destHeight);
                                            Slog.v(TAG, "  safe=" + safeWidth + "x" + safeHeight);
                                            Slog.v(TAG, "  maxTextureSize=" + GLHelper.getMaxTextureSize());
                                            String recordName = wallpaper.wallpaperFile.getName().equals(WALLPAPER) ? RECORD_FILE : RECORD_LOCK_FILE;
                                            File record = new File(getWallpaperDir(wallpaper.userId), recordName);
                                            record.createNewFile();
                                            Slog.v(TAG, "record path =" + record.getPath() + ", record name =" + record.getName());
                                            ImageDecoder.Source srcData = ImageDecoder.createSource(wallpaper.wallpaperFile);
                                            final int sampleSize = scale;
                                            Bitmap cropped = ImageDecoder.decodeBitmap(srcData, new ImageDecoder.OnHeaderDecodedListener() { // from class: com.android.server.wallpaper.WallpaperManagerService$$ExternalSyntheticLambda7
                                                @Override // android.graphics.ImageDecoder.OnHeaderDecodedListener
                                                public final void onHeaderDecoded(ImageDecoder imageDecoder, ImageDecoder.ImageInfo imageInfo, ImageDecoder.Source source) {
                                                    WallpaperManagerService.lambda$generateCrop$1(sampleSize, estimateCrop, imageDecoder, imageInfo, source);
                                                }
                                            });
                                            record.delete();
                                            if (cropped == null) {
                                                Slog.e(TAG, "Could not decode new wallpaper");
                                                success = false;
                                                bos = null;
                                                f = null;
                                            } else {
                                                Bitmap finalCrop = Bitmap.createScaledBitmap(cropped, safeWidth, safeHeight, true);
                                                FileOutputStream f3 = new FileOutputStream(wallpaper.cropFile);
                                                try {
                                                    BufferedOutputStream bos3 = new BufferedOutputStream(f3, 32768);
                                                    try {
                                                        try {
                                                            finalCrop.compress(Bitmap.CompressFormat.PNG, 100, bos3);
                                                            bos3.flush();
                                                            bos = bos3;
                                                            f = f3;
                                                            success = true;
                                                        } catch (Exception e4) {
                                                            bos2 = bos3;
                                                            f2 = f3;
                                                            IoUtils.closeQuietly(bos2);
                                                            IoUtils.closeQuietly(f2);
                                                            success = success2;
                                                            if (!success) {
                                                            }
                                                            if (wallpaper.cropFile.exists()) {
                                                            }
                                                        } catch (Throwable th4) {
                                                            th = th4;
                                                            bos2 = bos3;
                                                            f2 = f3;
                                                            IoUtils.closeQuietly(bos2);
                                                            IoUtils.closeQuietly(f2);
                                                            throw th;
                                                        }
                                                    } catch (Exception e5) {
                                                        bos2 = bos3;
                                                        f2 = f3;
                                                    } catch (Throwable th5) {
                                                        th = th5;
                                                        bos2 = bos3;
                                                        f2 = f3;
                                                    }
                                                } catch (Exception e6) {
                                                    bos2 = null;
                                                    f2 = f3;
                                                } catch (Throwable th6) {
                                                    th = th6;
                                                    bos2 = null;
                                                    f2 = f3;
                                                }
                                            }
                                            IoUtils.closeQuietly(bos);
                                            IoUtils.closeQuietly(f);
                                        } catch (Exception e7) {
                                            bos2 = null;
                                            f2 = null;
                                        } catch (Throwable th7) {
                                            th = th7;
                                            bos2 = null;
                                            f2 = null;
                                        }
                                    } catch (Exception e8) {
                                        f2 = null;
                                    } catch (Throwable th8) {
                                        th = th8;
                                        f2 = null;
                                    }
                                } catch (Exception e9) {
                                    f2 = null;
                                } catch (Throwable th9) {
                                    th = th9;
                                    f2 = null;
                                }
                            } catch (Exception e10) {
                                f2 = null;
                            } catch (Throwable th10) {
                                th = th10;
                                f2 = null;
                            }
                        } catch (Exception e11) {
                        } catch (Throwable th11) {
                            th = th11;
                        }
                    } catch (Exception e12) {
                    } catch (Throwable th12) {
                        th = th12;
                    }
                } catch (Exception e13) {
                    success2 = false;
                } catch (Throwable th13) {
                    th = th13;
                }
            } else {
                success = FileUtils.copyFile(wallpaper.wallpaperFile, wallpaper.cropFile);
                if (!success) {
                    wallpaper.cropFile.delete();
                }
            }
            if (!success) {
                Slog.e(TAG, "Unable to apply new wallpaper");
                wallpaper.cropFile.delete();
            }
            if (wallpaper.cropFile.exists()) {
                return;
            }
            SELinux.restorecon(wallpaper.cropFile.getAbsoluteFile());
            return;
        }
        Slog.w(TAG, "Invalid wallpaper data");
        success = false;
        if (!success) {
        }
        if (wallpaper.cropFile.exists()) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$generateCrop$1(int sampleSize, Rect estimateCrop, ImageDecoder decoder, ImageDecoder.ImageInfo info, ImageDecoder.Source src) {
        decoder.setTargetSampleSize(sampleSize);
        decoder.setCrop(estimateCrop);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class WallpaperData {
        boolean allowBackup;
        WallpaperConnection connection;
        final File cropFile;
        public boolean fromForegroundApp;
        boolean imageWallpaperPending;
        long lastDiedTime;
        boolean mIsColorExtractedFromDim;
        ComponentName nextWallpaperComponent;
        WallpaperColors primaryColors;
        IWallpaperManagerCallback setComplete;
        int userId;
        ComponentName wallpaperComponent;
        final File wallpaperFile;
        int wallpaperId;
        WallpaperObserver wallpaperObserver;
        boolean wallpaperUpdating;
        int whichPending;
        String name = "";
        float mWallpaperDimAmount = 0.0f;
        ArrayMap<Integer, Float> mUidToDimAmount = new ArrayMap<>();
        private RemoteCallbackList<IWallpaperManagerCallback> callbacks = new RemoteCallbackList<>();
        final Rect cropHint = new Rect(0, 0, 0, 0);

        WallpaperData(int userId, File wallpaperDir, String inputFileName, String cropFileName) {
            this.userId = userId;
            this.wallpaperFile = new File(wallpaperDir, inputFileName);
            this.cropFile = new File(wallpaperDir, cropFileName);
        }

        boolean cropExists() {
            return this.cropFile.exists();
        }

        boolean sourceExists() {
            return this.wallpaperFile.exists();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static final class DisplayData {
        final int mDisplayId;
        int mWidth = -1;
        int mHeight = -1;
        final Rect mPadding = new Rect(0, 0, 0, 0);

        DisplayData(int displayId) {
            this.mDisplayId = displayId;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeDisplayData(int displayId) {
        this.mDisplayDatas.remove(displayId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public DisplayData getDisplayDataOrCreate(int displayId) {
        DisplayData wpdData = this.mDisplayDatas.get(displayId);
        if (wpdData == null) {
            DisplayData wpdData2 = new DisplayData(displayId);
            ensureSaneWallpaperDisplaySize(wpdData2, displayId);
            this.mDisplayDatas.append(displayId, wpdData2);
            return wpdData2;
        }
        return wpdData;
    }

    private void ensureSaneWallpaperDisplaySize(DisplayData wpdData, int displayId) {
        int baseSize = getMaximumSizeDimension(displayId);
        if (wpdData.mWidth < baseSize) {
            wpdData.mWidth = baseSize;
        }
        if (wpdData.mHeight < baseSize) {
            wpdData.mHeight = baseSize;
        }
    }

    private int getMaximumSizeDimension(int displayId) {
        Display display = this.mDisplayManager.getDisplay(displayId);
        if (display == null) {
            Slog.w(TAG, "Invalid displayId=" + displayId + " " + Debug.getCallers(4));
            display = this.mDisplayManager.getDisplay(0);
        }
        return display.getMaximumSizeDimension();
    }

    void forEachDisplayData(Consumer<DisplayData> action) {
        for (int i = this.mDisplayDatas.size() - 1; i >= 0; i--) {
            DisplayData wpdData = this.mDisplayDatas.valueAt(i);
            action.accept(wpdData);
        }
    }

    int makeWallpaperIdLocked() {
        int i;
        do {
            i = this.mWallpaperId + 1;
            this.mWallpaperId = i;
        } while (i == 0);
        return i;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean supportsMultiDisplay(WallpaperConnection connection) {
        if (connection != null) {
            return connection.mInfo == null || connection.mInfo.supportsMultipleDisplays();
        }
        return false;
    }

    private void updateFallbackConnection() {
        WallpaperData wallpaperData = this.mLastWallpaper;
        if (wallpaperData == null || this.mFallbackWallpaper == null) {
            return;
        }
        WallpaperConnection systemConnection = wallpaperData.connection;
        final WallpaperConnection fallbackConnection = this.mFallbackWallpaper.connection;
        if (fallbackConnection == null) {
            Slog.w(TAG, "Fallback wallpaper connection has not been created yet!!");
        } else if (supportsMultiDisplay(systemConnection)) {
            if (fallbackConnection.mDisplayConnector.size() != 0) {
                fallbackConnection.forEachDisplayConnector(new Consumer() { // from class: com.android.server.wallpaper.WallpaperManagerService$$ExternalSyntheticLambda12
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        WallpaperManagerService.lambda$updateFallbackConnection$2((WallpaperManagerService.WallpaperConnection.DisplayConnector) obj);
                    }
                });
                fallbackConnection.mDisplayConnector.clear();
            }
        } else {
            fallbackConnection.appendConnectorWithCondition(new Predicate() { // from class: com.android.server.wallpaper.WallpaperManagerService$$ExternalSyntheticLambda13
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return WallpaperManagerService.lambda$updateFallbackConnection$3(WallpaperManagerService.WallpaperConnection.this, (Display) obj);
                }
            });
            fallbackConnection.forEachDisplayConnector(new Consumer() { // from class: com.android.server.wallpaper.WallpaperManagerService$$ExternalSyntheticLambda14
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    WallpaperManagerService.this.m7699x4459c0ed(fallbackConnection, (WallpaperManagerService.WallpaperConnection.DisplayConnector) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$updateFallbackConnection$2(WallpaperConnection.DisplayConnector connector) {
        if (connector.mEngine != null) {
            connector.disconnectLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$updateFallbackConnection$3(WallpaperConnection fallbackConnection, Display display) {
        return (!fallbackConnection.isUsableDisplay(display) || display.getDisplayId() == 0 || fallbackConnection.containsDisplay(display.getDisplayId())) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateFallbackConnection$4$com-android-server-wallpaper-WallpaperManagerService  reason: not valid java name */
    public /* synthetic */ void m7699x4459c0ed(WallpaperConnection fallbackConnection, WallpaperConnection.DisplayConnector connector) {
        if (connector.mEngine == null) {
            connector.connectLocked(fallbackConnection, this.mFallbackWallpaper);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class WallpaperConnection extends IWallpaperConnection.Stub implements ServiceConnection {
        private static final long WALLPAPER_RECONNECT_TIMEOUT_MS = 10000;
        final int mClientUid;
        final WallpaperInfo mInfo;
        IRemoteCallback mReply;
        IWallpaperService mService;
        WallpaperData mWallpaper;
        private SparseArray<DisplayConnector> mDisplayConnector = new SparseArray<>();
        private Runnable mResetRunnable = new Runnable() { // from class: com.android.server.wallpaper.WallpaperManagerService$WallpaperConnection$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                WallpaperManagerService.WallpaperConnection.this.m7705x33d11a90();
            }
        };
        private Runnable mTryToRebindRunnable = new Runnable() { // from class: com.android.server.wallpaper.WallpaperManagerService$WallpaperConnection$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                WallpaperManagerService.WallpaperConnection.this.tryToRebind();
            }
        };
        private Runnable mDisconnectRunnable = new Runnable() { // from class: com.android.server.wallpaper.WallpaperManagerService$WallpaperConnection$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                WallpaperManagerService.WallpaperConnection.this.m7706x9d94ff0c();
            }
        };

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes2.dex */
        public final class DisplayConnector {
            boolean mDimensionsChanged;
            final int mDisplayId;
            IWallpaperEngine mEngine;
            boolean mPaddingChanged;
            final Binder mToken = new Binder();

            DisplayConnector(int displayId) {
                this.mDisplayId = displayId;
            }

            void ensureStatusHandled() {
                DisplayData wpdData = WallpaperManagerService.this.getDisplayDataOrCreate(this.mDisplayId);
                if (this.mDimensionsChanged) {
                    try {
                        this.mEngine.setDesiredSize(wpdData.mWidth, wpdData.mHeight);
                    } catch (RemoteException e) {
                        Slog.w(WallpaperManagerService.TAG, "Failed to set wallpaper dimensions", e);
                    }
                    this.mDimensionsChanged = false;
                }
                if (this.mPaddingChanged) {
                    try {
                        this.mEngine.setDisplayPadding(wpdData.mPadding);
                    } catch (RemoteException e2) {
                        Slog.w(WallpaperManagerService.TAG, "Failed to set wallpaper padding", e2);
                    }
                    this.mPaddingChanged = false;
                }
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            public void connectLocked(WallpaperConnection connection, WallpaperData wallpaper) {
                if (connection.mService == null) {
                    Slog.w(WallpaperManagerService.TAG, "WallpaperService is not connected yet");
                    return;
                }
                WallpaperManagerService.this.mWindowManagerInternal.addWindowToken(this.mToken, 2013, this.mDisplayId, null);
                DisplayData wpdData = WallpaperManagerService.this.getDisplayDataOrCreate(this.mDisplayId);
                try {
                    connection.mService.attach(connection, this.mToken, 2013, false, wpdData.mWidth, wpdData.mHeight, wpdData.mPadding, this.mDisplayId);
                } catch (RemoteException e) {
                    Slog.w(WallpaperManagerService.TAG, "Failed attaching wallpaper on display", e);
                    if (wallpaper != null && !wallpaper.wallpaperUpdating && connection.getConnectedEngineSize() == 0) {
                        WallpaperManagerService.this.bindWallpaperComponentLocked(null, false, false, wallpaper, null);
                    }
                }
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            public void disconnectLocked() {
                if (WallpaperManagerService.this.mContext.getMainThreadHandler().hasCallbacks(WallpaperManagerService.this.mDestoryEngineRunnable)) {
                    WallpaperManagerService.this.mContext.getMainThreadHandler().removeCallbacks(WallpaperManagerService.this.mDestoryEngineRunnable);
                    WallpaperManagerService.this.mDestoryEngineRunnable.run();
                }
                WallpaperManagerService.this.mLastEngine = this.mEngine;
                WallpaperManagerService.this.mLastToken = this.mToken;
                WallpaperManagerService.this.mLastDisplayId = this.mDisplayId;
                WallpaperManagerService.this.mContext.getMainThreadHandler().postDelayed(WallpaperManagerService.this.mDestoryEngineRunnable, 500L);
                this.mEngine = null;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$new$0$com-android-server-wallpaper-WallpaperManagerService$WallpaperConnection  reason: not valid java name */
        public /* synthetic */ void m7705x33d11a90() {
            synchronized (WallpaperManagerService.this.mLock) {
                if (WallpaperManagerService.this.mShuttingDown) {
                    Slog.i(WallpaperManagerService.TAG, "Ignoring relaunch timeout during shutdown");
                    return;
                }
                if (!this.mWallpaper.wallpaperUpdating && this.mWallpaper.userId == WallpaperManagerService.this.mCurrentUserId) {
                    Slog.w(WallpaperManagerService.TAG, "Wallpaper reconnect timed out for " + this.mWallpaper.wallpaperComponent + ", reverting to built-in wallpaper!");
                    WallpaperManagerService.this.clearWallpaperLocked(true, 1, this.mWallpaper.userId, null);
                }
            }
        }

        WallpaperConnection(WallpaperInfo info, WallpaperData wallpaper, int clientUid) {
            this.mInfo = info;
            this.mWallpaper = wallpaper;
            this.mClientUid = clientUid;
            initDisplayState();
        }

        private void initDisplayState() {
            if (!this.mWallpaper.equals(WallpaperManagerService.this.mFallbackWallpaper)) {
                if (WallpaperManagerService.this.supportsMultiDisplay(this)) {
                    appendConnectorWithCondition(new Predicate() { // from class: com.android.server.wallpaper.WallpaperManagerService$WallpaperConnection$$ExternalSyntheticLambda6
                        @Override // java.util.function.Predicate
                        public final boolean test(Object obj) {
                            return WallpaperManagerService.WallpaperConnection.this.isUsableDisplay((Display) obj);
                        }
                    });
                } else {
                    this.mDisplayConnector.append(0, new DisplayConnector(0));
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void appendConnectorWithCondition(Predicate<Display> tester) {
            Display[] displays = WallpaperManagerService.this.mDisplayManager.getDisplays();
            for (Display display : displays) {
                if (tester.test(display)) {
                    int displayId = display.getDisplayId();
                    DisplayConnector connector = this.mDisplayConnector.get(displayId);
                    if (connector == null) {
                        this.mDisplayConnector.append(displayId, new DisplayConnector(displayId));
                    }
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public boolean isUsableDisplay(Display display) {
            if (display == null || !display.hasAccess(this.mClientUid)) {
                return false;
            }
            int displayId = display.getDisplayId();
            if (displayId == 0) {
                return true;
            }
            long ident = Binder.clearCallingIdentity();
            try {
                return WallpaperManagerService.this.mWindowManagerInternal.shouldShowSystemDecorOnDisplay(displayId);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        void forEachDisplayConnector(Consumer<DisplayConnector> action) {
            for (int i = this.mDisplayConnector.size() - 1; i >= 0; i--) {
                DisplayConnector connector = this.mDisplayConnector.valueAt(i);
                action.accept(connector);
            }
        }

        int getConnectedEngineSize() {
            int engineSize = 0;
            for (int i = this.mDisplayConnector.size() - 1; i >= 0; i--) {
                DisplayConnector connector = this.mDisplayConnector.valueAt(i);
                if (connector.mEngine != null) {
                    engineSize++;
                }
            }
            return engineSize;
        }

        DisplayConnector getDisplayConnectorOrCreate(int displayId) {
            DisplayConnector connector = this.mDisplayConnector.get(displayId);
            if (connector == null) {
                Display display = WallpaperManagerService.this.mDisplayManager.getDisplay(displayId);
                if (isUsableDisplay(display)) {
                    DisplayConnector connector2 = new DisplayConnector(displayId);
                    this.mDisplayConnector.append(displayId, connector2);
                    return connector2;
                }
                return connector;
            }
            return connector;
        }

        boolean containsDisplay(int displayId) {
            return this.mDisplayConnector.get(displayId) != null;
        }

        void removeDisplayConnector(int displayId) {
            DisplayConnector connector = this.mDisplayConnector.get(displayId);
            if (connector != null) {
                this.mDisplayConnector.remove(displayId);
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (this.mWallpaper.connection == this) {
                    this.mService = IWallpaperService.Stub.asInterface(service);
                    WallpaperManagerService.this.attachServiceLocked(this, this.mWallpaper);
                    if (!this.mWallpaper.equals(WallpaperManagerService.this.mFallbackWallpaper)) {
                        WallpaperManagerService.this.saveSettingsLocked(this.mWallpaper.userId);
                    }
                    FgThread.getHandler().removeCallbacks(this.mResetRunnable);
                    WallpaperManagerService.this.mContext.getMainThreadHandler().removeCallbacks(this.mTryToRebindRunnable);
                    ITranWallpaperManagerService.Instance().onServiceConnected(this.mInfo, name);
                }
            }
        }

        public void onLocalWallpaperColorsChanged(final RectF area, final WallpaperColors colors, final int displayId) {
            forEachDisplayConnector(new Consumer() { // from class: com.android.server.wallpaper.WallpaperManagerService$WallpaperConnection$$ExternalSyntheticLambda5
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    WallpaperManagerService.WallpaperConnection.this.m7707x5d741b40(area, colors, displayId, (WallpaperManagerService.WallpaperConnection.DisplayConnector) obj);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onLocalWallpaperColorsChanged$2$com-android-server-wallpaper-WallpaperManagerService$WallpaperConnection  reason: not valid java name */
        public /* synthetic */ void m7707x5d741b40(final RectF area, final WallpaperColors colors, int displayId, DisplayConnector displayConnector) {
            Consumer<ILocalWallpaperColorConsumer> callback = new Consumer() { // from class: com.android.server.wallpaper.WallpaperManagerService$WallpaperConnection$$ExternalSyntheticLambda3
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    WallpaperManagerService.WallpaperConnection.lambda$onLocalWallpaperColorsChanged$1(area, colors, (ILocalWallpaperColorConsumer) obj);
                }
            };
            synchronized (WallpaperManagerService.this.mLock) {
                WallpaperManagerService.this.mLocalColorRepo.forEachCallback(callback, area, displayId);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$onLocalWallpaperColorsChanged$1(RectF area, WallpaperColors colors, ILocalWallpaperColorConsumer cb) {
            try {
                cb.onColorsChanged(area, colors);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            synchronized (WallpaperManagerService.this.mLock) {
                Slog.w(WallpaperManagerService.TAG, "Wallpaper service gone: " + name);
                if (!Objects.equals(name, this.mWallpaper.wallpaperComponent)) {
                    Slog.e(WallpaperManagerService.TAG, "Does not match expected wallpaper component " + this.mWallpaper.wallpaperComponent);
                }
                this.mService = null;
                forEachDisplayConnector(new Consumer() { // from class: com.android.server.wallpaper.WallpaperManagerService$WallpaperConnection$$ExternalSyntheticLambda4
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((WallpaperManagerService.WallpaperConnection.DisplayConnector) obj).mEngine = null;
                    }
                });
                if (this.mWallpaper.connection == this && !this.mWallpaper.wallpaperUpdating) {
                    WallpaperManagerService.this.mContext.getMainThreadHandler().postDelayed(this.mDisconnectRunnable, 1000L);
                }
            }
        }

        private void scheduleTimeoutLocked() {
            Handler fgHandler = FgThread.getHandler();
            fgHandler.removeCallbacks(this.mResetRunnable);
            fgHandler.postDelayed(this.mResetRunnable, 10000L);
            Slog.i(WallpaperManagerService.TAG, "Started wallpaper reconnect timeout for " + this.mWallpaper.wallpaperComponent);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void tryToRebind() {
            synchronized (WallpaperManagerService.this.mLock) {
                if (this.mWallpaper.wallpaperUpdating) {
                    return;
                }
                ComponentName wpService = this.mWallpaper.wallpaperComponent;
                if (WallpaperManagerService.this.bindWallpaperComponentLocked(wpService, true, false, this.mWallpaper, null)) {
                    this.mWallpaper.connection.scheduleTimeoutLocked();
                } else if (SystemClock.uptimeMillis() - this.mWallpaper.lastDiedTime < 10000) {
                    Slog.w(WallpaperManagerService.TAG, "Rebind fail! Try again later");
                    WallpaperManagerService.this.mContext.getMainThreadHandler().postDelayed(this.mTryToRebindRunnable, 1000L);
                } else {
                    Slog.w(WallpaperManagerService.TAG, "Reverting to built-in wallpaper!");
                    WallpaperManagerService.this.clearWallpaperLocked(true, 1, this.mWallpaper.userId, null);
                    String flattened = wpService.flattenToString();
                    EventLog.writeEvent((int) EventLogTags.WP_WALLPAPER_CRASHED, flattened.substring(0, Math.min(flattened.length(), 128)));
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$new$4$com-android-server-wallpaper-WallpaperManagerService$WallpaperConnection  reason: not valid java name */
        public /* synthetic */ void m7706x9d94ff0c() {
            synchronized (WallpaperManagerService.this.mLock) {
                if (this == this.mWallpaper.connection) {
                    ComponentName wpService = this.mWallpaper.wallpaperComponent;
                    if (!this.mWallpaper.wallpaperUpdating && this.mWallpaper.userId == WallpaperManagerService.this.mCurrentUserId && !Objects.equals(WallpaperManagerService.this.mDefaultWallpaperComponent, wpService) && !Objects.equals(WallpaperManagerService.this.mImageWallpaper, wpService)) {
                        if (this.mWallpaper.lastDiedTime != 0 && this.mWallpaper.lastDiedTime + 10000 > SystemClock.uptimeMillis()) {
                            Slog.w(WallpaperManagerService.TAG, "Reverting to built-in wallpaper!");
                            WallpaperManagerService.this.clearWallpaperLocked(true, 1, this.mWallpaper.userId, null);
                        } else {
                            this.mWallpaper.lastDiedTime = SystemClock.uptimeMillis();
                            tryToRebind();
                        }
                    }
                } else {
                    Slog.i(WallpaperManagerService.TAG, "Wallpaper changed during disconnect tracking; ignoring");
                }
            }
        }

        public void onWallpaperColorsChanged(WallpaperColors primaryColors, int displayId) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (WallpaperManagerService.this.mImageWallpaper.equals(this.mWallpaper.wallpaperComponent)) {
                    return;
                }
                this.mWallpaper.primaryColors = primaryColors;
                int which = 1;
                if (displayId == 0) {
                    WallpaperData lockedWallpaper = (WallpaperData) WallpaperManagerService.this.mLockWallpaperMap.get(this.mWallpaper.userId);
                    if (lockedWallpaper == null) {
                        which = 1 | 2;
                    }
                }
                if (which != 0) {
                    WallpaperManagerService.this.notifyWallpaperColorsChangedOnDisplay(this.mWallpaper, which, displayId);
                }
            }
        }

        public void attachEngine(IWallpaperEngine engine, int displayId) {
            synchronized (WallpaperManagerService.this.mLock) {
                DisplayConnector connector = getDisplayConnectorOrCreate(displayId);
                if (connector == null) {
                    try {
                        engine.destroy();
                    } catch (RemoteException e) {
                        Slog.w(WallpaperManagerService.TAG, "Failed to destroy engine", e);
                    }
                    return;
                }
                connector.mEngine = engine;
                connector.ensureStatusHandled();
                WallpaperInfo wallpaperInfo = this.mInfo;
                if (wallpaperInfo != null && wallpaperInfo.supportsAmbientMode() && displayId == 0) {
                    try {
                        connector.mEngine.setInAmbientMode(WallpaperManagerService.this.mInAmbientMode, 0L);
                    } catch (RemoteException e2) {
                        Slog.w(WallpaperManagerService.TAG, "Failed to set ambient mode state", e2);
                    }
                }
                try {
                    connector.mEngine.requestWallpaperColors();
                } catch (RemoteException e3) {
                    Slog.w(WallpaperManagerService.TAG, "Failed to request wallpaper colors", e3);
                }
                List<RectF> areas = WallpaperManagerService.this.mLocalColorRepo.getAreasByDisplayId(displayId);
                if (areas != null && areas.size() != 0) {
                    try {
                        connector.mEngine.addLocalColorsAreas(areas);
                    } catch (RemoteException e4) {
                        Slog.w(WallpaperManagerService.TAG, "Failed to register local colors areas", e4);
                    }
                }
                if (this.mWallpaper.mWallpaperDimAmount != 0.0f) {
                    try {
                        connector.mEngine.applyDimming(this.mWallpaper.mWallpaperDimAmount);
                    } catch (RemoteException e5) {
                        Slog.w(WallpaperManagerService.TAG, "Failed to dim wallpaper", e5);
                    }
                }
                return;
            }
        }

        public void engineShown(IWallpaperEngine engine) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (this.mReply != null) {
                    long ident = Binder.clearCallingIdentity();
                    try {
                        this.mReply.sendResult((Bundle) null);
                    } catch (RemoteException e) {
                        Binder.restoreCallingIdentity(ident);
                    }
                    this.mReply = null;
                }
            }
            if (WallpaperManagerService.this.mContext.getMainThreadHandler().hasCallbacks(WallpaperManagerService.this.mDestoryEngineRunnable)) {
                WallpaperManagerService.this.mContext.getMainThreadHandler().removeCallbacks(WallpaperManagerService.this.mDestoryEngineRunnable);
                WallpaperManagerService.this.mDestoryEngineRunnable.run();
            }
        }

        public ParcelFileDescriptor setWallpaper(String name) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (this.mWallpaper.connection == this) {
                    return WallpaperManagerService.this.updateWallpaperBitmapLocked(name, this.mWallpaper, null);
                }
                return null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class MyPackageMonitor extends PackageMonitor {
        MyPackageMonitor() {
        }

        public void onPackageUpdateFinished(String packageName, int uid) {
            ComponentName wpService;
            synchronized (WallpaperManagerService.this.mLock) {
                if (WallpaperManagerService.this.mCurrentUserId != getChangingUserId()) {
                    return;
                }
                WallpaperData wallpaper = (WallpaperData) WallpaperManagerService.this.mWallpaperMap.get(WallpaperManagerService.this.mCurrentUserId);
                if (wallpaper != null && (wpService = wallpaper.wallpaperComponent) != null && wpService.getPackageName().equals(packageName)) {
                    Slog.i(WallpaperManagerService.TAG, "Wallpaper " + wpService + " update has finished");
                    wallpaper.wallpaperUpdating = false;
                    WallpaperManagerService.this.clearWallpaperComponentLocked(wallpaper);
                    if (!WallpaperManagerService.this.bindWallpaperComponentLocked(wpService, false, false, wallpaper, null)) {
                        Slog.w(WallpaperManagerService.TAG, "Wallpaper " + wpService + " no longer available; reverting to default");
                        WallpaperManagerService.this.clearWallpaperLocked(false, 1, wallpaper.userId, null);
                    }
                }
            }
        }

        public void onPackageModified(String packageName) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (WallpaperManagerService.this.mCurrentUserId != getChangingUserId()) {
                    return;
                }
                WallpaperData wallpaper = (WallpaperData) WallpaperManagerService.this.mWallpaperMap.get(WallpaperManagerService.this.mCurrentUserId);
                if (wallpaper != null) {
                    if (wallpaper.wallpaperComponent != null && wallpaper.wallpaperComponent.getPackageName().equals(packageName)) {
                        doPackagesChangedLocked(true, wallpaper);
                    }
                }
            }
        }

        public void onPackageUpdateStarted(String packageName, int uid) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (WallpaperManagerService.this.mCurrentUserId != getChangingUserId()) {
                    return;
                }
                WallpaperData wallpaper = (WallpaperData) WallpaperManagerService.this.mWallpaperMap.get(WallpaperManagerService.this.mCurrentUserId);
                if (wallpaper != null && wallpaper.wallpaperComponent != null && wallpaper.wallpaperComponent.getPackageName().equals(packageName)) {
                    Slog.i(WallpaperManagerService.TAG, "Wallpaper service " + wallpaper.wallpaperComponent + " is updating");
                    wallpaper.wallpaperUpdating = true;
                    if (wallpaper.connection != null) {
                        FgThread.getHandler().removeCallbacks(wallpaper.connection.mResetRunnable);
                    }
                }
            }
        }

        public boolean onHandleForceStop(Intent intent, String[] packages, int uid, boolean doit) {
            synchronized (WallpaperManagerService.this.mLock) {
                boolean changed = false;
                if (WallpaperManagerService.this.mCurrentUserId != getChangingUserId()) {
                    return false;
                }
                WallpaperData wallpaper = (WallpaperData) WallpaperManagerService.this.mWallpaperMap.get(WallpaperManagerService.this.mCurrentUserId);
                if (wallpaper != null) {
                    boolean res = doPackagesChangedLocked(doit, wallpaper);
                    changed = false | res;
                }
                return changed;
            }
        }

        public void onSomePackagesChanged() {
            synchronized (WallpaperManagerService.this.mLock) {
                if (WallpaperManagerService.this.mCurrentUserId != getChangingUserId()) {
                    return;
                }
                WallpaperData wallpaper = (WallpaperData) WallpaperManagerService.this.mWallpaperMap.get(WallpaperManagerService.this.mCurrentUserId);
                if (wallpaper != null) {
                    doPackagesChangedLocked(true, wallpaper);
                }
            }
        }

        boolean doPackagesChangedLocked(boolean doit, WallpaperData wallpaper) {
            int change;
            int change2;
            boolean changed = false;
            if (wallpaper.wallpaperComponent != null && ((change2 = isPackageDisappearing(wallpaper.wallpaperComponent.getPackageName())) == 3 || change2 == 2)) {
                changed = true;
                if (doit) {
                    Slog.w(WallpaperManagerService.TAG, "Wallpaper uninstalled, removing: " + wallpaper.wallpaperComponent);
                    WallpaperManagerService.this.clearWallpaperLocked(false, 1, wallpaper.userId, null);
                }
            }
            if (wallpaper.nextWallpaperComponent != null && ((change = isPackageDisappearing(wallpaper.nextWallpaperComponent.getPackageName())) == 3 || change == 2)) {
                wallpaper.nextWallpaperComponent = null;
            }
            if (wallpaper.wallpaperComponent != null && isPackageModified(wallpaper.wallpaperComponent.getPackageName())) {
                try {
                    WallpaperManagerService.this.mContext.getPackageManager().getServiceInfo(wallpaper.wallpaperComponent, 786432);
                } catch (PackageManager.NameNotFoundException e) {
                    Slog.w(WallpaperManagerService.TAG, "Wallpaper component gone, removing: " + wallpaper.wallpaperComponent);
                    WallpaperManagerService.this.clearWallpaperLocked(false, 1, wallpaper.userId, null);
                }
            }
            if (wallpaper.nextWallpaperComponent != null && isPackageModified(wallpaper.nextWallpaperComponent.getPackageName())) {
                try {
                    WallpaperManagerService.this.mContext.getPackageManager().getServiceInfo(wallpaper.nextWallpaperComponent, 786432);
                } catch (PackageManager.NameNotFoundException e2) {
                    wallpaper.nextWallpaperComponent = null;
                }
            }
            return changed;
        }
    }

    WallpaperData getCurrentWallpaperData(int which, int userId) {
        WallpaperData wallpaperData;
        synchronized (this.mLock) {
            SparseArray<WallpaperData> wallpaperDataMap = which == 1 ? this.mWallpaperMap : this.mLockWallpaperMap;
            wallpaperData = wallpaperDataMap.get(userId);
        }
        return wallpaperData;
    }

    public WallpaperManagerService(Context context) {
        DisplayManager.DisplayListener displayListener = new DisplayManager.DisplayListener() { // from class: com.android.server.wallpaper.WallpaperManagerService.1
            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayAdded(int displayId) {
            }

            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayRemoved(int displayId) {
                synchronized (WallpaperManagerService.this.mLock) {
                    if (WallpaperManagerService.this.mLastWallpaper != null && WallpaperManagerService.this.mLastWallpaper.connection != null) {
                        WallpaperData targetWallpaper = null;
                        if (WallpaperManagerService.this.mLastWallpaper.connection.containsDisplay(displayId)) {
                            targetWallpaper = WallpaperManagerService.this.mLastWallpaper;
                        } else if (WallpaperManagerService.this.mFallbackWallpaper.connection.containsDisplay(displayId)) {
                            targetWallpaper = WallpaperManagerService.this.mFallbackWallpaper;
                        }
                        if (targetWallpaper == null) {
                            return;
                        }
                        WallpaperConnection.DisplayConnector connector = targetWallpaper.connection.getDisplayConnectorOrCreate(displayId);
                        if (connector == null) {
                            return;
                        }
                        connector.disconnectLocked();
                        targetWallpaper.connection.removeDisplayConnector(displayId);
                        WallpaperManagerService.this.removeDisplayData(displayId);
                    }
                    for (int i = WallpaperManagerService.this.mColorsChangedListeners.size() - 1; i >= 0; i--) {
                        SparseArray<RemoteCallbackList<IWallpaperManagerCallback>> callbacks = (SparseArray) WallpaperManagerService.this.mColorsChangedListeners.valueAt(i);
                        callbacks.delete(displayId);
                    }
                }
            }

            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayChanged(int displayId) {
            }
        };
        this.mDisplayListener = displayListener;
        this.mWallpaperMap = new SparseArray<>();
        this.mLockWallpaperMap = new SparseArray<>();
        this.mDisplayDatas = new SparseArray<>();
        this.mUserRestorecon = new SparseBooleanArray();
        this.mCurrentUserId = -10000;
        this.mLocalColorRepo = new LocalColorRepository();
        this.mDestoryEngineRunnable = new Runnable() { // from class: com.android.server.wallpaper.WallpaperManagerService$$ExternalSyntheticLambda15
            @Override // java.lang.Runnable
            public final void run() {
                WallpaperManagerService.this.m7694xd100cbbc();
            }
        };
        this.mContext = context;
        this.mShuttingDown = false;
        this.mImageWallpaper = ComponentName.unflattenFromString(context.getResources().getString(17040476));
        this.mDefaultWallpaperComponent = WallpaperManager.getDefaultWallpaperComponent(context);
        this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        this.mIPackageManager = AppGlobals.getPackageManager();
        this.mAppOpsManager = (AppOpsManager) context.getSystemService("appops");
        DisplayManager displayManager = (DisplayManager) context.getSystemService(DisplayManager.class);
        this.mDisplayManager = displayManager;
        displayManager.registerDisplayListener(displayListener, null);
        this.mActivityManager = (ActivityManager) context.getSystemService(ActivityManager.class);
        this.mMonitor = new MyPackageMonitor();
        this.mColorsChangedListeners = new SparseArray<>();
        LocalServices.addService(WallpaperManagerInternal.class, new LocalService());
    }

    /* loaded from: classes2.dex */
    private final class LocalService extends WallpaperManagerInternal {
        private LocalService() {
        }

        @Override // com.android.server.wallpaper.WallpaperManagerInternal
        public void onDisplayReady(int displayId) {
            WallpaperManagerService.this.onDisplayReadyInternal(displayId);
        }
    }

    void initialize() {
        this.mMonitor.register(this.mContext, null, UserHandle.ALL, true);
        getWallpaperDir(0).mkdirs();
        loadSettingsLocked(0, false);
        getWallpaperSafeLocked(0, 1);
    }

    File getWallpaperDir(int userId) {
        return Environment.getUserSystemDirectory(userId);
    }

    protected void finalize() throws Throwable {
        super.finalize();
        for (int i = 0; i < this.mWallpaperMap.size(); i++) {
            WallpaperData wallpaper = this.mWallpaperMap.valueAt(i);
            wallpaper.wallpaperObserver.stopWatching();
        }
    }

    void systemReady() {
        initialize();
        WallpaperData wallpaper = this.mWallpaperMap.get(0);
        if (this.mImageWallpaper.equals(wallpaper.nextWallpaperComponent)) {
            if (!wallpaper.cropExists()) {
                generateCrop(wallpaper);
            }
            if (!wallpaper.cropExists()) {
                clearWallpaperLocked(false, 1, 0, null);
            }
        }
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.wallpaper.WallpaperManagerService.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.USER_REMOVED".equals(action)) {
                    WallpaperManagerService.this.onRemoveUser(intent.getIntExtra("android.intent.extra.user_handle", -10000));
                }
            }
        }, userFilter);
        IntentFilter shutdownFilter = new IntentFilter("android.intent.action.ACTION_SHUTDOWN");
        this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.wallpaper.WallpaperManagerService.3
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.ACTION_SHUTDOWN".equals(intent.getAction())) {
                    synchronized (WallpaperManagerService.this.mLock) {
                        WallpaperManagerService.this.mShuttingDown = true;
                    }
                }
            }
        }, shutdownFilter);
        try {
            ActivityManager.getService().registerUserSwitchObserver(new UserSwitchObserver() { // from class: com.android.server.wallpaper.WallpaperManagerService.4
                public void onUserSwitching(int newUserId, IRemoteCallback reply) {
                    WallpaperManagerService.this.errorCheck(newUserId);
                    WallpaperManagerService.this.switchUser(newUserId, reply);
                }
            }, TAG);
        } catch (RemoteException e) {
            e.rethrowAsRuntimeException();
        }
        if (DEFAULT_THEME_LOCAL_SUPPORT) {
            TranWallpaperExtFacotry.getInstance().getTranWallpaperExt().systemReady(this.mContext, new Handler());
            try {
                TranWallpaperExtFacotry.getInstance().getTranWallpaperExt().setCmfColor(new NVUtils().readNVColor());
            } catch (Exception e2) {
                Slog.e(TAG, e2.toString());
            }
        }
    }

    public String getName() {
        String str;
        if (Binder.getCallingUid() != 1000) {
            throw new RuntimeException("getName() can only be called from the system process");
        }
        synchronized (this.mLock) {
            str = this.mWallpaperMap.get(0).name;
        }
        return str;
    }

    void stopObserver(WallpaperData wallpaper) {
        if (wallpaper != null && wallpaper.wallpaperObserver != null) {
            wallpaper.wallpaperObserver.stopWatching();
            wallpaper.wallpaperObserver = null;
        }
    }

    void stopObserversLocked(int userId) {
        stopObserver(this.mWallpaperMap.get(userId));
        stopObserver(this.mLockWallpaperMap.get(userId));
        this.mWallpaperMap.remove(userId);
        this.mLockWallpaperMap.remove(userId);
    }

    @Override // com.android.server.wallpaper.IWallpaperManagerService
    public void onBootPhase(int phase) {
        errorCheck(0);
        if (phase == 550) {
            systemReady();
        } else if (phase == 600) {
            switchUser(0, null);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void errorCheck(final int userID) {
        sWallpaperType.forEach(new BiConsumer() { // from class: com.android.server.wallpaper.WallpaperManagerService$$ExternalSyntheticLambda8
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                WallpaperManagerService.this.m7693xe4bbf15(userID, (Integer) obj, (String) obj2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$errorCheck$5$com-android-server-wallpaper-WallpaperManagerService  reason: not valid java name */
    public /* synthetic */ void m7693xe4bbf15(int userID, Integer type, String filename) {
        File record = new File(getWallpaperDir(userID), filename);
        if (record.exists()) {
            Slog.w(TAG, "User:" + userID + ", wallpaper tyep = " + type + ", wallpaper fail detect!! reset to default wallpaper");
            clearWallpaperData(userID, type.intValue());
            record.delete();
        }
    }

    private void clearWallpaperData(int userID, int wallpaperType) {
        WallpaperData wallpaper = new WallpaperData(userID, getWallpaperDir(userID), wallpaperType == 2 ? WALLPAPER_LOCK_ORIG : WALLPAPER, wallpaperType == 2 ? WALLPAPER_LOCK_CROP : WALLPAPER_CROP);
        if (wallpaper.sourceExists()) {
            wallpaper.wallpaperFile.delete();
        }
        if (wallpaper.cropExists()) {
            wallpaper.cropFile.delete();
        }
    }

    @Override // com.android.server.wallpaper.IWallpaperManagerService
    public void onUnlockUser(final int userId) {
        synchronized (this.mLock) {
            if (this.mCurrentUserId == userId) {
                if (this.mWaitingForUnlock) {
                    WallpaperData systemWallpaper = getWallpaperSafeLocked(userId, 1);
                    switchWallpaper(systemWallpaper, null);
                    notifyCallbacksLocked(systemWallpaper);
                }
                if (!this.mUserRestorecon.get(userId)) {
                    this.mUserRestorecon.put(userId, true);
                    Runnable relabeler = new Runnable() { // from class: com.android.server.wallpaper.WallpaperManagerService$$ExternalSyntheticLambda4
                        @Override // java.lang.Runnable
                        public final void run() {
                            WallpaperManagerService.this.m7696xe9f74a6(userId);
                        }
                    };
                    BackgroundThread.getHandler().post(relabeler);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onUnlockUser$6$com-android-server-wallpaper-WallpaperManagerService  reason: not valid java name */
    public /* synthetic */ void m7696xe9f74a6(int userId) {
        String[] strArr;
        TimingsTraceAndSlog t = new TimingsTraceAndSlog(TAG);
        t.traceBegin("Wallpaper_selinux_restorecon-" + userId);
        try {
            File wallpaperDir = getWallpaperDir(userId);
            for (String filename : sPerUserFiles) {
                File f = new File(wallpaperDir, filename);
                if (f.exists()) {
                    SELinux.restorecon(f);
                }
            }
        } finally {
            t.traceEnd();
        }
    }

    void onRemoveUser(int userId) {
        String[] strArr;
        if (userId < 1) {
            return;
        }
        File wallpaperDir = getWallpaperDir(userId);
        synchronized (this.mLock) {
            stopObserversLocked(userId);
            for (String filename : sPerUserFiles) {
                new File(wallpaperDir, filename).delete();
            }
            this.mUserRestorecon.delete(userId);
        }
    }

    void switchUser(int userId, IRemoteCallback reply) {
        TimingsTraceAndSlog t = new TimingsTraceAndSlog(TAG);
        t.traceBegin("Wallpaper_switch-user-" + userId);
        try {
            synchronized (this.mLock) {
                if (this.mCurrentUserId == userId) {
                    return;
                }
                this.mCurrentUserId = userId;
                final WallpaperData systemWallpaper = getWallpaperSafeLocked(userId, 1);
                WallpaperData tmpLockWallpaper = this.mLockWallpaperMap.get(userId);
                final WallpaperData lockWallpaper = tmpLockWallpaper == null ? systemWallpaper : tmpLockWallpaper;
                if (systemWallpaper.wallpaperObserver == null) {
                    systemWallpaper.wallpaperObserver = new WallpaperObserver(systemWallpaper);
                    systemWallpaper.wallpaperObserver.startWatching();
                }
                switchWallpaper(systemWallpaper, reply);
                FgThread.getHandler().post(new Runnable() { // from class: com.android.server.wallpaper.WallpaperManagerService$$ExternalSyntheticLambda17
                    @Override // java.lang.Runnable
                    public final void run() {
                        WallpaperManagerService.this.m7698xcd24c34(systemWallpaper, lockWallpaper);
                    }
                });
            }
        } finally {
            t.traceEnd();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$switchUser$7$com-android-server-wallpaper-WallpaperManagerService  reason: not valid java name */
    public /* synthetic */ void m7698xcd24c34(WallpaperData systemWallpaper, WallpaperData lockWallpaper) {
        notifyWallpaperColorsChanged(systemWallpaper, 1);
        notifyWallpaperColorsChanged(lockWallpaper, 2);
        notifyWallpaperColorsChanged(this.mFallbackWallpaper, 1);
    }

    void switchWallpaper(WallpaperData wallpaper, IRemoteCallback reply) {
        ServiceInfo si;
        synchronized (this.mLock) {
            try {
                try {
                    this.mWaitingForUnlock = false;
                    ComponentName cname = wallpaper.wallpaperComponent != null ? wallpaper.wallpaperComponent : wallpaper.nextWallpaperComponent;
                    if (!bindWallpaperComponentLocked(cname, true, false, wallpaper, reply)) {
                        try {
                            ServiceInfo si2 = this.mIPackageManager.getServiceInfo(cname, 262144L, wallpaper.userId);
                            si = si2;
                        } catch (RemoteException e) {
                            si = null;
                        }
                        if (si == null) {
                            Slog.w(TAG, "Failure starting previous wallpaper; clearing");
                            clearWallpaperLocked(false, 1, wallpaper.userId, reply);
                        } else {
                            Slog.w(TAG, "Wallpaper isn't direct boot aware; using fallback until unlocked");
                            wallpaper.wallpaperComponent = wallpaper.nextWallpaperComponent;
                            WallpaperData fallback = new WallpaperData(wallpaper.userId, getWallpaperDir(wallpaper.userId), WALLPAPER_LOCK_ORIG, WALLPAPER_LOCK_CROP);
                            ensureSaneWallpaperData(fallback);
                            bindWallpaperComponentLocked(this.mImageWallpaper, true, false, fallback, reply);
                            this.mWaitingForUnlock = true;
                        }
                    }
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public void clearOsWallpaper(String callingPackage, int which, int userId) {
        checkPermission("android.permission.SET_WALLPAPER");
        if (!isWallpaperSupported(callingPackage) || !isSetWallpaperAllowed(callingPackage)) {
            return;
        }
        int userId2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, "clearWallpaper", null);
        WallpaperData data = null;
        synchronized (this.mLock) {
            this.mCacheDefaultImageWallpaperColors = null;
            WallpaperData datawallpaper = this.mWallpaperMap.get(userId2);
            if (datawallpaper != null) {
                notifyCallbacksLocked(datawallpaper, true);
            }
            clearWallpaperLocked(false, which, userId2, null);
            if (which == 2) {
                data = this.mLockWallpaperMap.get(userId2);
                notifyLockWallpaperChanged();
            }
            if (which == 1 || data == null) {
                data = this.mWallpaperMap.get(userId2);
            }
        }
        if (data != null) {
            notifyWallpaperColorsChanged(data, which, true);
            notifyWallpaperColorsChanged(this.mFallbackWallpaper, 1, true);
        }
    }

    public void clearWallpaper(String callingPackage, int which, int userId) {
        checkPermission("android.permission.SET_WALLPAPER");
        if (!isWallpaperSupported(callingPackage) || !isSetWallpaperAllowed(callingPackage)) {
            return;
        }
        int userId2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, "clearWallpaper", null);
        WallpaperData data = null;
        synchronized (this.mLock) {
            clearWallpaperLocked(false, which, userId2, null);
            if (which == 2) {
                data = this.mLockWallpaperMap.get(userId2);
            }
            if (which == 1 || data == null) {
                data = this.mWallpaperMap.get(userId2);
            }
        }
        if (data != null) {
            notifyWallpaperColorsChanged(data, which);
            notifyWallpaperColorsChanged(this.mFallbackWallpaper, 1);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2249=5] */
    void clearWallpaperLocked(boolean defaultFailed, int which, int userId, IRemoteCallback reply) {
        WallpaperData wallpaper;
        if (which != 1 && which != 2) {
            throw new IllegalArgumentException("Must specify exactly one kind of wallpaper to clear");
        }
        if (which == 2) {
            WallpaperData wallpaper2 = this.mLockWallpaperMap.get(userId);
            if (wallpaper2 == null) {
                return;
            }
            wallpaper = wallpaper2;
        } else {
            WallpaperData wallpaper3 = this.mWallpaperMap.get(userId);
            if (wallpaper3 == null) {
                loadSettingsLocked(userId, false);
                wallpaper = this.mWallpaperMap.get(userId);
            } else {
                wallpaper = wallpaper3;
            }
        }
        if (wallpaper == null) {
            return;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            if (wallpaper.wallpaperFile.exists()) {
                wallpaper.wallpaperFile.delete();
                wallpaper.cropFile.delete();
                if (which == 2) {
                    this.mLockWallpaperMap.remove(userId);
                    IWallpaperManagerCallback cb = this.mKeyguardListener;
                    if (cb != null) {
                        try {
                            cb.onWallpaperChanged();
                        } catch (RemoteException e) {
                        }
                    }
                    saveSettingsLocked(userId);
                    return;
                }
            }
            RuntimeException e2 = null;
            try {
                wallpaper.primaryColors = null;
                wallpaper.imageWallpaperPending = false;
            } catch (IllegalArgumentException e1) {
                e2 = e1;
            }
            if (userId != this.mCurrentUserId) {
                return;
            }
            if (bindWallpaperComponentLocked(defaultFailed ? this.mImageWallpaper : null, true, false, wallpaper, reply)) {
                return;
            }
            Slog.e(TAG, "Default wallpaper component not found!", e2);
            clearWallpaperComponentLocked(wallpaper);
            if (reply != null) {
                try {
                    reply.sendResult((Bundle) null);
                } catch (RemoteException e3) {
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private boolean hasCrossUserPermission() {
        int interactPermission = this.mContext.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL");
        return interactPermission == 0;
    }

    public boolean hasNamedWallpaper(String name) {
        int callingUser = UserHandle.getCallingUserId();
        boolean allowCrossUser = hasCrossUserPermission();
        synchronized (this.mLock) {
            long ident = Binder.clearCallingIdentity();
            List<UserInfo> users = ((UserManager) this.mContext.getSystemService("user")).getUsers();
            Binder.restoreCallingIdentity(ident);
            for (UserInfo user : users) {
                if (allowCrossUser || callingUser == user.id) {
                    if (!user.isManagedProfile()) {
                        WallpaperData wd = this.mWallpaperMap.get(user.id);
                        if (wd == null) {
                            loadSettingsLocked(user.id, false);
                            wd = this.mWallpaperMap.get(user.id);
                        }
                        if (wd != null && name.equals(wd.name)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    private boolean isValidDisplay(int displayId) {
        return this.mDisplayManager.getDisplay(displayId) != null;
    }

    public void setDimensionHints(int width, int height, String callingPackage, int displayId) throws RemoteException {
        checkPermission("android.permission.SET_WALLPAPER_HINTS");
        if (!isWallpaperSupported(callingPackage)) {
            return;
        }
        int width2 = Math.min(width, GLHelper.getMaxTextureSize());
        int height2 = Math.min(height, GLHelper.getMaxTextureSize());
        synchronized (this.mLock) {
            int userId = UserHandle.getCallingUserId();
            WallpaperData wallpaper = getWallpaperSafeLocked(userId, 1);
            if (width2 <= 0 || height2 <= 0) {
                throw new IllegalArgumentException("width and height must be > 0");
            }
            if (!isValidDisplay(displayId)) {
                throw new IllegalArgumentException("Cannot find display with id=" + displayId);
            }
            DisplayData wpdData = getDisplayDataOrCreate(displayId);
            if (width2 != wpdData.mWidth || height2 != wpdData.mHeight) {
                wpdData.mWidth = width2;
                wpdData.mHeight = height2;
                if (displayId == 0) {
                    saveSettingsLocked(userId);
                }
                if (this.mCurrentUserId != userId) {
                    return;
                }
                if (wallpaper.connection != null) {
                    WallpaperConnection.DisplayConnector connector = wallpaper.connection.getDisplayConnectorOrCreate(displayId);
                    IWallpaperEngine engine = connector != null ? connector.mEngine : null;
                    if (engine != null) {
                        try {
                            engine.setDesiredSize(width2, height2);
                        } catch (RemoteException e) {
                        }
                        notifyCallbacksLocked(wallpaper);
                    } else if (wallpaper.connection.mService != null && connector != null) {
                        connector.mDimensionsChanged = true;
                    }
                }
            }
        }
    }

    public int getWidthHint(int displayId) throws RemoteException {
        synchronized (this.mLock) {
            if (!isValidDisplay(displayId)) {
                throw new IllegalArgumentException("Cannot find display with id=" + displayId);
            }
            WallpaperData wallpaper = this.mWallpaperMap.get(UserHandle.getCallingUserId());
            if (wallpaper != null) {
                DisplayData wpdData = getDisplayDataOrCreate(displayId);
                return wpdData.mWidth;
            }
            return 0;
        }
    }

    public int getHeightHint(int displayId) throws RemoteException {
        synchronized (this.mLock) {
            if (!isValidDisplay(displayId)) {
                throw new IllegalArgumentException("Cannot find display with id=" + displayId);
            }
            WallpaperData wallpaper = this.mWallpaperMap.get(UserHandle.getCallingUserId());
            if (wallpaper != null) {
                DisplayData wpdData = getDisplayDataOrCreate(displayId);
                return wpdData.mHeight;
            }
            return 0;
        }
    }

    public void setDisplayPadding(Rect padding, String callingPackage, int displayId) {
        checkPermission("android.permission.SET_WALLPAPER_HINTS");
        if (!isWallpaperSupported(callingPackage)) {
            return;
        }
        synchronized (this.mLock) {
            if (!isValidDisplay(displayId)) {
                throw new IllegalArgumentException("Cannot find display with id=" + displayId);
            }
            int userId = UserHandle.getCallingUserId();
            WallpaperData wallpaper = getWallpaperSafeLocked(userId, 1);
            if (padding.left < 0 || padding.top < 0 || padding.right < 0 || padding.bottom < 0) {
                throw new IllegalArgumentException("padding must be positive: " + padding);
            }
            int maxSize = getMaximumSizeDimension(displayId);
            int paddingWidth = padding.left + padding.right;
            int paddingHeight = padding.top + padding.bottom;
            if (paddingWidth > maxSize) {
                throw new IllegalArgumentException("padding width " + paddingWidth + " exceeds max width " + maxSize);
            }
            if (paddingHeight > maxSize) {
                throw new IllegalArgumentException("padding height " + paddingHeight + " exceeds max height " + maxSize);
            }
            DisplayData wpdData = getDisplayDataOrCreate(displayId);
            if (!padding.equals(wpdData.mPadding)) {
                wpdData.mPadding.set(padding);
                if (displayId == 0) {
                    saveSettingsLocked(userId);
                }
                if (this.mCurrentUserId != userId) {
                    return;
                }
                if (wallpaper.connection != null) {
                    WallpaperConnection.DisplayConnector connector = wallpaper.connection.getDisplayConnectorOrCreate(displayId);
                    IWallpaperEngine engine = connector != null ? connector.mEngine : null;
                    if (engine != null) {
                        try {
                            engine.setDisplayPadding(padding);
                        } catch (RemoteException e) {
                        }
                        notifyCallbacksLocked(wallpaper);
                    } else if (wallpaper.connection.mService != null && connector != null) {
                        connector.mPaddingChanged = true;
                    }
                }
            }
        }
    }

    @Deprecated
    public ParcelFileDescriptor getWallpaper(String callingPkg, IWallpaperManagerCallback cb, int which, Bundle outParams, int wallpaperUserId) {
        return getWallpaperWithFeature(callingPkg, null, cb, which, outParams, wallpaperUserId);
    }

    public ParcelFileDescriptor getWallpaperWithFeature(String callingPkg, String callingFeatureId, IWallpaperManagerCallback cb, int which, Bundle outParams, int wallpaperUserId) {
        boolean hasPrivilege = hasPermission("android.permission.READ_WALLPAPER_INTERNAL");
        if (!hasPrivilege) {
            ((StorageManager) this.mContext.getSystemService(StorageManager.class)).checkPermissionReadImages(true, Binder.getCallingPid(), Binder.getCallingUid(), callingPkg, callingFeatureId);
        }
        int wallpaperUserId2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), wallpaperUserId, false, true, "getWallpaper", null);
        if (which != 1 && which != 2) {
            throw new IllegalArgumentException("Must specify exactly one kind of wallpaper to read");
        }
        synchronized (this.mLock) {
            SparseArray<WallpaperData> whichSet = which == 2 ? this.mLockWallpaperMap : this.mWallpaperMap;
            WallpaperData wallpaper = whichSet.get(wallpaperUserId2);
            if (wallpaper != null) {
                DisplayData wpdData = getDisplayDataOrCreate(0);
                if (outParams != null) {
                    try {
                        outParams.putInt("width", wpdData.mWidth);
                        outParams.putInt("height", wpdData.mHeight);
                    } catch (FileNotFoundException e) {
                        Slog.w(TAG, "Error getting wallpaper", e);
                        return null;
                    }
                }
                if (cb != null) {
                    wallpaper.callbacks.register(cb);
                }
                if (!wallpaper.cropFile.exists()) {
                    return null;
                }
                return ParcelFileDescriptor.open(wallpaper.cropFile, 268435456);
            }
            return null;
        }
    }

    private boolean hasPermission(String permission) {
        return this.mContext.checkCallingOrSelfPermission(permission) == 0;
    }

    public WallpaperInfo getWallpaperInfo(int userId) {
        boolean allow = hasPermission("android.permission.READ_WALLPAPER_INTERNAL") || hasPermission("android.permission.QUERY_ALL_PACKAGES");
        if (allow) {
            int userId2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, "getWallpaperInfo", null);
            synchronized (this.mLock) {
                WallpaperData wallpaper = this.mWallpaperMap.get(userId2);
                if (wallpaper != null && wallpaper.connection != null) {
                    return wallpaper.connection.mInfo;
                }
                return null;
            }
        }
        return null;
    }

    public int getWallpaperIdForUser(int which, int userId) {
        int userId2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, "getWallpaperIdForUser", null);
        if (which != 1 && which != 2) {
            throw new IllegalArgumentException("Must specify exactly one kind of wallpaper");
        }
        SparseArray<WallpaperData> map = which == 2 ? this.mLockWallpaperMap : this.mWallpaperMap;
        synchronized (this.mLock) {
            WallpaperData wallpaper = map.get(userId2);
            if (wallpaper != null) {
                return wallpaper.wallpaperId;
            }
            return -1;
        }
    }

    public void registerWallpaperColorsCallback(IWallpaperManagerCallback cb, int userId, int displayId) {
        int userId2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, true, true, "registerWallpaperColorsCallback", null);
        synchronized (this.mLock) {
            SparseArray<RemoteCallbackList<IWallpaperManagerCallback>> userDisplayColorsChangedListeners = this.mColorsChangedListeners.get(userId2);
            if (userDisplayColorsChangedListeners == null) {
                userDisplayColorsChangedListeners = new SparseArray<>();
                this.mColorsChangedListeners.put(userId2, userDisplayColorsChangedListeners);
            }
            RemoteCallbackList<IWallpaperManagerCallback> displayChangedListeners = userDisplayColorsChangedListeners.get(displayId);
            if (displayChangedListeners == null) {
                displayChangedListeners = new RemoteCallbackList<>();
                userDisplayColorsChangedListeners.put(displayId, displayChangedListeners);
            }
            displayChangedListeners.register(cb);
        }
    }

    public void unregisterWallpaperColorsCallback(IWallpaperManagerCallback cb, int userId, int displayId) {
        RemoteCallbackList<IWallpaperManagerCallback> displayChangedListeners;
        int userId2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, true, true, "unregisterWallpaperColorsCallback", null);
        synchronized (this.mLock) {
            SparseArray<RemoteCallbackList<IWallpaperManagerCallback>> userDisplayColorsChangedListeners = this.mColorsChangedListeners.get(userId2);
            if (userDisplayColorsChangedListeners != null && (displayChangedListeners = userDisplayColorsChangedListeners.get(displayId)) != null) {
                displayChangedListeners.unregister(cb);
            }
        }
    }

    public void setInAmbientMode(boolean inAmbientMode, long animationDuration) {
        IWallpaperEngine engine;
        synchronized (this.mLock) {
            this.mInAmbientMode = inAmbientMode;
            WallpaperData data = this.mWallpaperMap.get(this.mCurrentUserId);
            if (data != null && data.connection != null && (data.connection.mInfo == null || data.connection.mInfo.supportsAmbientMode())) {
                engine = data.connection.getDisplayConnectorOrCreate(0).mEngine;
            } else {
                engine = null;
            }
        }
        if (engine != null) {
            try {
                engine.setInAmbientMode(inAmbientMode, animationDuration);
            } catch (RemoteException e) {
            }
        }
    }

    public void notifyWakingUp(final int x, final int y, final Bundle extras) {
        synchronized (this.mLock) {
            WallpaperData data = this.mWallpaperMap.get(this.mCurrentUserId);
            if (data != null && data.connection != null) {
                data.connection.forEachDisplayConnector(new Consumer() { // from class: com.android.server.wallpaper.WallpaperManagerService$$ExternalSyntheticLambda11
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        WallpaperManagerService.lambda$notifyWakingUp$8(x, y, extras, (WallpaperManagerService.WallpaperConnection.DisplayConnector) obj);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$notifyWakingUp$8(int x, int y, Bundle extras, WallpaperConnection.DisplayConnector displayConnector) {
        if (displayConnector.mEngine != null) {
            try {
                displayConnector.mEngine.dispatchWallpaperCommand("android.wallpaper.wakingup", x, y, -1, extras);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void notifyGoingToSleep(final int x, final int y, final Bundle extras) {
        synchronized (this.mLock) {
            WallpaperData data = this.mWallpaperMap.get(this.mCurrentUserId);
            if (data != null && data.connection != null) {
                data.connection.forEachDisplayConnector(new Consumer() { // from class: com.android.server.wallpaper.WallpaperManagerService$$ExternalSyntheticLambda9
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        WallpaperManagerService.lambda$notifyGoingToSleep$9(x, y, extras, (WallpaperManagerService.WallpaperConnection.DisplayConnector) obj);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$notifyGoingToSleep$9(int x, int y, Bundle extras, WallpaperConnection.DisplayConnector displayConnector) {
        if (displayConnector.mEngine != null) {
            try {
                displayConnector.mEngine.dispatchWallpaperCommand("android.wallpaper.goingtosleep", x, y, -1, extras);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean setLockWallpaperCallback(IWallpaperManagerCallback cb) {
        checkPermission("android.permission.INTERNAL_SYSTEM_WINDOW");
        synchronized (this.mLock) {
            this.mKeyguardListener = cb;
        }
        return true;
    }

    private IWallpaperEngine getEngine(int which, int userId, int displayId) {
        WallpaperConnection connection;
        WallpaperData wallpaperData = findWallpaperAtDisplay(userId, displayId);
        if (wallpaperData == null || (connection = wallpaperData.connection) == null) {
            return null;
        }
        IWallpaperEngine engine = null;
        synchronized (this.mLock) {
            for (int i = 0; i < connection.mDisplayConnector.size(); i++) {
                int id = ((WallpaperConnection.DisplayConnector) connection.mDisplayConnector.get(i)).mDisplayId;
                int currentWhich = ((WallpaperConnection.DisplayConnector) connection.mDisplayConnector.get(i)).mDisplayId;
                if (id == displayId || currentWhich == which) {
                    engine = ((WallpaperConnection.DisplayConnector) connection.mDisplayConnector.get(i)).mEngine;
                    break;
                }
            }
        }
        return engine;
    }

    public void addOnLocalColorsChangedListener(ILocalWallpaperColorConsumer callback, List<RectF> regions, int which, int userId, int displayId) throws RemoteException {
        if (which != 2 && which != 1) {
            throw new IllegalArgumentException("which should be either FLAG_LOCK or FLAG_SYSTEM");
        }
        IWallpaperEngine engine = getEngine(which, userId, displayId);
        if (engine == null) {
            return;
        }
        synchronized (this.mLock) {
            this.mLocalColorRepo.addAreas(callback, regions, displayId);
        }
        engine.addLocalColorsAreas(regions);
    }

    public void removeOnLocalColorsChangedListener(ILocalWallpaperColorConsumer callback, List<RectF> removeAreas, int which, int userId, int displayId) throws RemoteException {
        if (which != 2 && which != 1) {
            throw new IllegalArgumentException("which should be either FLAG_LOCK or FLAG_SYSTEM");
        }
        UserHandle callingUser = Binder.getCallingUserHandle();
        if (callingUser.getIdentifier() != userId) {
            throw new SecurityException("calling user id does not match");
        }
        long identity = Binder.clearCallingIdentity();
        List<RectF> purgeAreas = null;
        try {
            synchronized (this.mLock) {
                purgeAreas = this.mLocalColorRepo.removeAreas(callback, removeAreas, displayId);
            }
        } catch (Exception e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
        Binder.restoreCallingIdentity(identity);
        IWallpaperEngine engine = getEngine(which, userId, displayId);
        if (engine == null || purgeAreas == null || purgeAreas.size() <= 0) {
            return;
        }
        engine.removeLocalColorsAreas(purgeAreas);
    }

    public boolean lockScreenWallpaperExists() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mLockWallpaperMap.get(this.mCurrentUserId) != null;
        }
        return z;
    }

    public void setWallpaperDimAmount(float dimAmount) throws RemoteException {
        setWallpaperDimAmountForUid(Binder.getCallingUid(), dimAmount);
    }

    public void setWallpaperDimAmountForUid(int uid, float dimAmount) {
        checkPermission("android.permission.SET_WALLPAPER_DIM_AMOUNT");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                WallpaperData wallpaper = this.mWallpaperMap.get(this.mCurrentUserId);
                WallpaperData lockWallpaper = this.mLockWallpaperMap.get(this.mCurrentUserId);
                if (dimAmount == 0.0f) {
                    wallpaper.mUidToDimAmount.remove(Integer.valueOf(uid));
                } else {
                    wallpaper.mUidToDimAmount.put(Integer.valueOf(uid), Float.valueOf(dimAmount));
                }
                final float maxDimAmount = getHighestDimAmountFromMap(wallpaper.mUidToDimAmount);
                wallpaper.mWallpaperDimAmount = maxDimAmount;
                if (lockWallpaper != null) {
                    lockWallpaper.mWallpaperDimAmount = maxDimAmount;
                }
                if (wallpaper.connection != null) {
                    wallpaper.connection.forEachDisplayConnector(new Consumer() { // from class: com.android.server.wallpaper.WallpaperManagerService$$ExternalSyntheticLambda6
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            WallpaperManagerService.lambda$setWallpaperDimAmountForUid$10(maxDimAmount, (WallpaperManagerService.WallpaperConnection.DisplayConnector) obj);
                        }
                    });
                    wallpaper.mIsColorExtractedFromDim = true;
                    notifyWallpaperColorsChanged(wallpaper, 1);
                    if (lockWallpaper != null) {
                        lockWallpaper.mIsColorExtractedFromDim = true;
                        notifyWallpaperColorsChanged(lockWallpaper, 2);
                    }
                    saveSettingsLocked(wallpaper.userId);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$setWallpaperDimAmountForUid$10(float maxDimAmount, WallpaperConnection.DisplayConnector connector) {
        if (connector.mEngine != null) {
            try {
                connector.mEngine.applyDimming(maxDimAmount);
            } catch (RemoteException e) {
                Slog.w(TAG, "Can't apply dimming on wallpaper display connector", e);
            }
        }
    }

    public float getWallpaperDimAmount() {
        float f;
        checkPermission("android.permission.SET_WALLPAPER_DIM_AMOUNT");
        synchronized (this.mLock) {
            WallpaperData data = this.mWallpaperMap.get(this.mCurrentUserId);
            f = data.mWallpaperDimAmount;
        }
        return f;
    }

    private float getHighestDimAmountFromMap(ArrayMap<Integer, Float> uidToDimAmountMap) {
        float maxDimAmount = 0.0f;
        for (Map.Entry<Integer, Float> entry : uidToDimAmountMap.entrySet()) {
            if (entry.getValue().floatValue() > maxDimAmount) {
                maxDimAmount = entry.getValue().floatValue();
            }
        }
        return maxDimAmount;
    }

    public WallpaperColors getWallpaperColors(int which, int userId, int displayId) throws RemoteException {
        boolean shouldExtract = true;
        if (which != 2 && which != 1) {
            throw new IllegalArgumentException("which should be either FLAG_LOCK or FLAG_SYSTEM");
        }
        int userId2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, "getWallpaperColors", null);
        WallpaperData wallpaperData = null;
        synchronized (this.mLock) {
            if (which == 2) {
                try {
                    wallpaperData = this.mLockWallpaperMap.get(userId2);
                } catch (Throwable th) {
                    throw th;
                }
            }
            if (wallpaperData == null) {
                wallpaperData = findWallpaperAtDisplay(userId2, displayId);
            }
            if (wallpaperData == null) {
                return null;
            }
            if (wallpaperData.primaryColors != null && !wallpaperData.mIsColorExtractedFromDim) {
                shouldExtract = false;
            }
            if (shouldExtract) {
                extractColors(wallpaperData);
            }
            return getAdjustedWallpaperColorsOnDimming(wallpaperData);
        }
    }

    WallpaperColors getAdjustedWallpaperColorsOnDimming(WallpaperData wallpaperData) {
        synchronized (this.mLock) {
            WallpaperColors wallpaperColors = wallpaperData.primaryColors;
            if (wallpaperColors == null || (wallpaperColors.getColorHints() & 4) != 0 || wallpaperData.mWallpaperDimAmount == 0.0f) {
                return wallpaperColors;
            }
            int adjustedColorHints = wallpaperColors.getColorHints() & (-2) & (-3);
            return new WallpaperColors(wallpaperColors.getPrimaryColor(), wallpaperColors.getSecondaryColor(), wallpaperColors.getTertiaryColor(), adjustedColorHints);
        }
    }

    private WallpaperData findWallpaperAtDisplay(int userId, int displayId) {
        WallpaperData wallpaperData = this.mFallbackWallpaper;
        if (wallpaperData != null && wallpaperData.connection != null && this.mFallbackWallpaper.connection.containsDisplay(displayId)) {
            return this.mFallbackWallpaper;
        }
        return this.mWallpaperMap.get(userId);
    }

    public ParcelFileDescriptor setWallpaper(String name, final String callingPackage, Rect cropHint, boolean allowBackup, Bundle extras, int which, IWallpaperManagerCallback completion, int userId) {
        Rect cropHint2;
        ParcelFileDescriptor pfd;
        int userId2 = ActivityManager.handleIncomingUser(getCallingPid(), getCallingUid(), userId, false, true, "changing wallpaper", null);
        checkPermission("android.permission.SET_WALLPAPER");
        if ((which & 3) == 0) {
            Slog.e(TAG, "Must specify a valid wallpaper category to set");
            throw new IllegalArgumentException("Must specify a valid wallpaper category to set");
        } else if (!isWallpaperSupported(callingPackage) || !isSetWallpaperAllowed(callingPackage)) {
            return null;
        } else {
            if (cropHint == null) {
                cropHint2 = new Rect(0, 0, 0, 0);
            } else if (cropHint.width() < 0 || cropHint.height() < 0 || cropHint.left < 0 || cropHint.top < 0) {
                throw new IllegalArgumentException("Invalid crop rect supplied: " + cropHint);
            } else {
                cropHint2 = cropHint;
            }
            boolean fromForegroundApp = ((Boolean) Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.wallpaper.WallpaperManagerService$$ExternalSyntheticLambda10
                public final Object getOrThrow() {
                    return WallpaperManagerService.this.m7697x1bf3303c(callingPackage);
                }
            })).booleanValue();
            synchronized (this.mLock) {
                if (which == 1) {
                    if (this.mLockWallpaperMap.get(userId2) == null) {
                        Slog.i(TAG, "Migrating current wallpaper to be lock-only beforeupdating system wallpaper");
                        migrateSystemToLockWallpaperLocked(userId2);
                    }
                }
                WallpaperData wallpaper = getWallpaperSafeLocked(userId2, which);
                long ident = Binder.clearCallingIdentity();
                pfd = updateWallpaperBitmapLocked(name, wallpaper, extras);
                if (pfd != null) {
                    wallpaper.imageWallpaperPending = true;
                    wallpaper.whichPending = which;
                    wallpaper.setComplete = completion;
                    wallpaper.fromForegroundApp = fromForegroundApp;
                    wallpaper.cropHint.set(cropHint2);
                    wallpaper.allowBackup = allowBackup;
                    wallpaper.mWallpaperDimAmount = getWallpaperDimAmount();
                }
                Binder.restoreCallingIdentity(ident);
            }
            return pfd;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setWallpaper$11$com-android-server-wallpaper-WallpaperManagerService  reason: not valid java name */
    public /* synthetic */ Boolean m7697x1bf3303c(String callingPackage) throws Exception {
        return Boolean.valueOf(this.mActivityManager.getPackageImportance(callingPackage) == 100);
    }

    private void migrateSystemToLockWallpaperLocked(int userId) {
        WallpaperData sysWP = this.mWallpaperMap.get(userId);
        if (sysWP == null) {
            return;
        }
        WallpaperData lockWP = new WallpaperData(userId, getWallpaperDir(userId), WALLPAPER_LOCK_ORIG, WALLPAPER_LOCK_CROP);
        lockWP.wallpaperId = sysWP.wallpaperId;
        lockWP.cropHint.set(sysWP.cropHint);
        lockWP.allowBackup = sysWP.allowBackup;
        lockWP.primaryColors = sysWP.primaryColors;
        lockWP.mWallpaperDimAmount = sysWP.mWallpaperDimAmount;
        try {
            Os.rename(sysWP.wallpaperFile.getAbsolutePath(), lockWP.wallpaperFile.getAbsolutePath());
            Os.rename(sysWP.cropFile.getAbsolutePath(), lockWP.cropFile.getAbsolutePath());
            this.mLockWallpaperMap.put(userId, lockWP);
        } catch (ErrnoException e) {
            Slog.e(TAG, "Can't migrate system wallpaper: " + e.getMessage());
            lockWP.wallpaperFile.delete();
            lockWP.cropFile.delete();
        }
    }

    ParcelFileDescriptor updateWallpaperBitmapLocked(String name, WallpaperData wallpaper, Bundle extras) {
        if (name == null) {
            name = "";
        }
        try {
            File dir = getWallpaperDir(wallpaper.userId);
            if (!dir.exists()) {
                dir.mkdir();
                FileUtils.setPermissions(dir.getPath(), 505, -1, -1);
            }
            ParcelFileDescriptor fd = ParcelFileDescriptor.open(wallpaper.wallpaperFile, 1006632960);
            if (!SELinux.restorecon(wallpaper.wallpaperFile)) {
                Slog.w(TAG, "restorecon failed for wallpaper file: " + wallpaper.wallpaperFile.getPath());
                return null;
            }
            wallpaper.name = name;
            wallpaper.wallpaperId = makeWallpaperIdLocked();
            if (extras != null) {
                extras.putInt("android.service.wallpaper.extra.ID", wallpaper.wallpaperId);
            }
            wallpaper.primaryColors = null;
            Slog.v(TAG, "updateWallpaperBitmapLocked() : id=" + wallpaper.wallpaperId + " name=" + name + " file=" + wallpaper.wallpaperFile.getName());
            return fd;
        } catch (FileNotFoundException e) {
            Slog.w(TAG, "Error setting wallpaper", e);
            return null;
        }
    }

    public void setWallpaperComponentChecked(ComponentName name, String callingPackage, int userId) {
        if (isWallpaperSupported(callingPackage) && isSetWallpaperAllowed(callingPackage)) {
            setWallpaperComponent(name, userId);
        }
    }

    public void setWallpaperComponent(ComponentName name) {
        setWallpaperComponent(name, UserHandle.getCallingUserId());
    }

    private void setWallpaperComponent(ComponentName name, int userId) {
        WallpaperData wallpaper;
        int userId2 = ActivityManager.handleIncomingUser(getCallingPid(), getCallingUid(), userId, false, true, "changing live wallpaper", null);
        checkPermission("android.permission.SET_WALLPAPER_COMPONENT");
        int which = 1;
        boolean shouldNotifyColors = false;
        synchronized (this.mLock) {
            Slog.v(TAG, "setWallpaperComponent name=" + name);
            wallpaper = this.mWallpaperMap.get(userId2);
            if (wallpaper == null) {
                throw new IllegalStateException("Wallpaper not yet initialized for user " + userId2);
            }
            long ident = Binder.clearCallingIdentity();
            if (this.mImageWallpaper.equals(wallpaper.wallpaperComponent) && this.mLockWallpaperMap.get(userId2) == null) {
                Slog.i(TAG, "Migrating current wallpaper to be lock-only beforeupdating system wallpaper");
                migrateSystemToLockWallpaperLocked(userId2);
            }
            if (this.mLockWallpaperMap.get(userId2) == null) {
                which = 1 | 2;
            }
            wallpaper.imageWallpaperPending = false;
            boolean same = changingToSame(name, wallpaper);
            if (bindWallpaperComponentLocked(name, false, true, wallpaper, null)) {
                if (!same) {
                    wallpaper.primaryColors = null;
                } else if (wallpaper.connection != null) {
                    wallpaper.connection.forEachDisplayConnector(new Consumer() { // from class: com.android.server.wallpaper.WallpaperManagerService$$ExternalSyntheticLambda16
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            WallpaperManagerService.lambda$setWallpaperComponent$12((WallpaperManagerService.WallpaperConnection.DisplayConnector) obj);
                        }
                    });
                }
                wallpaper.wallpaperId = makeWallpaperIdLocked();
                notifyCallbacksLocked(wallpaper);
                shouldNotifyColors = true;
            }
            Binder.restoreCallingIdentity(ident);
        }
        if (shouldNotifyColors) {
            notifyWallpaperColorsChanged(wallpaper, which);
            notifyWallpaperColorsChanged(this.mFallbackWallpaper, 1);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$setWallpaperComponent$12(WallpaperConnection.DisplayConnector displayConnector) {
        try {
            if (displayConnector.mEngine != null) {
                displayConnector.mEngine.dispatchWallpaperCommand("android.wallpaper.reapply", 0, 0, 0, (Bundle) null);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Error sending apply message to wallpaper", e);
        }
    }

    private boolean changingToSame(ComponentName componentName, WallpaperData wallpaper) {
        if (wallpaper.connection != null) {
            return wallpaper.wallpaperComponent == null ? componentName == null : wallpaper.wallpaperComponent.equals(componentName);
        }
        return false;
    }

    /* JADX WARN: Code restructure failed: missing block: B:46:0x010a, code lost:
        r14 = new android.app.WallpaperInfo(r22.mContext, r7.get(r8));
     */
    /* JADX WARN: Removed duplicated region for block: B:109:0x0254  */
    /* JADX WARN: Removed duplicated region for block: B:111:0x0258  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    boolean bindWallpaperComponentLocked(ComponentName componentName, boolean force, boolean fromUser, WallpaperData wallpaper, IRemoteCallback reply) {
        ComponentName componentName2 = componentName;
        Slog.v(TAG, "bindWallpaperComponentLocked: componentName=" + componentName2);
        if (!force && changingToSame(componentName2, wallpaper)) {
            return true;
        }
        if (componentName2 == null) {
            try {
                componentName2 = this.mDefaultWallpaperComponent;
                try {
                    if ((OS_SET_DEFAULT_DYNAMIC_WALLPAPER_SUPPORT && wallpaper.userId != 0) || !TextUtils.isEmpty(Settings.Global.getStringForUser(this.mContext.getContentResolver(), "transsion_default_wallpaper", wallpaper.userId))) {
                        componentName2 = null;
                    }
                    if (componentName2 == null) {
                        componentName2 = this.mImageWallpaper;
                        Slog.v(TAG, "No default component; using image wallpaper");
                    }
                } catch (RemoteException e) {
                    e = e;
                    String msg = "Remote exception for " + componentName2 + "\n" + e;
                    if (fromUser) {
                    }
                }
            } catch (RemoteException e2) {
                e = e2;
                String msg2 = "Remote exception for " + componentName2 + "\n" + e;
                if (fromUser) {
                }
            }
        }
        int serviceUserId = wallpaper.userId;
        ServiceInfo si = this.mIPackageManager.getServiceInfo(componentName2, 4224L, serviceUserId);
        if (si == null) {
            Slog.w(TAG, "Attempted wallpaper " + componentName2 + " is unavailable");
            return false;
        } else if (!"android.permission.BIND_WALLPAPER".equals(si.permission)) {
            String msg3 = "Selected service does not have android.permission.BIND_WALLPAPER: " + componentName2;
            if (fromUser) {
                throw new SecurityException(msg3);
            }
            Slog.w(TAG, msg3);
            return false;
        } else {
            WallpaperInfo wi = null;
            Intent intent = new Intent("android.service.wallpaper.WallpaperService");
            if (componentName2 != null && !componentName2.equals(this.mImageWallpaper)) {
                List<ResolveInfo> ris = this.mIPackageManager.queryIntentServices(intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 128L, serviceUserId).getList();
                int i = 0;
                while (true) {
                    if (i >= ris.size()) {
                        break;
                    }
                    ServiceInfo rsi = ris.get(i).serviceInfo;
                    if (!rsi.name.equals(si.name) || !rsi.packageName.equals(si.packageName)) {
                        i++;
                    } else {
                        try {
                            break;
                        } catch (IOException e3) {
                            if (fromUser) {
                                throw new IllegalArgumentException(e3);
                            }
                            Slog.w(TAG, e3);
                            return false;
                        } catch (XmlPullParserException e4) {
                            if (fromUser) {
                                throw new IllegalArgumentException(e4);
                            }
                            Slog.w(TAG, e4);
                            return false;
                        }
                    }
                }
                if (wi == null) {
                    String msg4 = "Selected service is not a wallpaper: " + componentName2;
                    if (fromUser) {
                        throw new SecurityException(msg4);
                    }
                    Slog.w(TAG, msg4);
                    return false;
                }
            }
            if (wi != null && wi.supportsAmbientMode()) {
                int hasPrivilege = this.mIPackageManager.checkPermission("android.permission.AMBIENT_WALLPAPER", wi.getPackageName(), serviceUserId);
                if (hasPrivilege != 0) {
                    String msg5 = "Selected service does not have android.permission.AMBIENT_WALLPAPER: " + componentName2;
                    if (fromUser) {
                        throw new SecurityException(msg5);
                    }
                    Slog.w(TAG, msg5);
                    return false;
                }
            }
            int componentUid = this.mIPackageManager.getPackageUid(componentName2.getPackageName(), 268435456L, wallpaper.userId);
            WallpaperConnection newConn = new WallpaperConnection(wi, wallpaper, componentUid);
            intent.setComponent(componentName2);
            intent.putExtra("android.intent.extra.client_label", 17041722);
            intent.putExtra("android.intent.extra.client_intent", PendingIntent.getActivityAsUser(this.mContext, 0, Intent.createChooser(new Intent("android.intent.action.SET_WALLPAPER"), this.mContext.getText(17039867)), 67108864, null, new UserHandle(serviceUserId)));
            if (this.mContext.bindServiceAsUser(intent, newConn, 570429441, new UserHandle(serviceUserId))) {
                if (wallpaper.userId == this.mCurrentUserId && this.mLastWallpaper != null && !wallpaper.equals(this.mFallbackWallpaper)) {
                    detachWallpaperLocked(this.mLastWallpaper);
                }
                wallpaper.wallpaperComponent = componentName2;
                wallpaper.connection = newConn;
                try {
                    newConn.mReply = reply;
                    if (wallpaper.userId == this.mCurrentUserId && !wallpaper.equals(this.mFallbackWallpaper)) {
                        this.mLastWallpaper = wallpaper;
                    }
                    updateFallbackConnection();
                    return true;
                } catch (RemoteException e5) {
                    e = e5;
                    String msg22 = "Remote exception for " + componentName2 + "\n" + e;
                    if (fromUser) {
                        throw new IllegalArgumentException(msg22);
                    }
                    Slog.w(TAG, msg22);
                    return false;
                }
            }
            String msg6 = "Unable to bind service: " + componentName2;
            if (fromUser) {
                throw new IllegalArgumentException(msg6);
            }
            Slog.w(TAG, msg6);
            return false;
        }
    }

    private void detachWallpaperLocked(WallpaperData wallpaper) {
        if (wallpaper.connection != null) {
            if (wallpaper.connection.mReply != null) {
                try {
                    wallpaper.connection.mReply.sendResult((Bundle) null);
                } catch (RemoteException e) {
                }
                wallpaper.connection.mReply = null;
            }
            try {
                if (wallpaper.connection.mService != null) {
                    wallpaper.connection.mService.detach();
                }
            } catch (RemoteException e2) {
                Slog.w(TAG, "Failed detaching wallpaper service ", e2);
            }
            this.mContext.unbindService(wallpaper.connection);
            wallpaper.connection.forEachDisplayConnector(new Consumer() { // from class: com.android.server.wallpaper.WallpaperManagerService$$ExternalSyntheticLambda5
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((WallpaperManagerService.WallpaperConnection.DisplayConnector) obj).disconnectLocked();
                }
            });
            wallpaper.connection.mService = null;
            wallpaper.connection.mDisplayConnector.clear();
            FgThread.getHandler().removeCallbacks(wallpaper.connection.mResetRunnable);
            this.mContext.getMainThreadHandler().removeCallbacks(wallpaper.connection.mDisconnectRunnable);
            this.mContext.getMainThreadHandler().removeCallbacks(wallpaper.connection.mTryToRebindRunnable);
            wallpaper.connection = null;
            if (wallpaper == this.mLastWallpaper) {
                this.mLastWallpaper = null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clearWallpaperComponentLocked(WallpaperData wallpaper) {
        wallpaper.wallpaperComponent = null;
        detachWallpaperLocked(wallpaper);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void attachServiceLocked(final WallpaperConnection conn, final WallpaperData wallpaper) {
        conn.forEachDisplayConnector(new Consumer() { // from class: com.android.server.wallpaper.WallpaperManagerService$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((WallpaperManagerService.WallpaperConnection.DisplayConnector) obj).connectLocked(WallpaperManagerService.WallpaperConnection.this, wallpaper);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyCallbacksLocked(WallpaperData wallpaper) {
        notifyCallbacksLocked(wallpaper, false);
    }

    private void notifyCallbacksLocked(WallpaperData wallpaper, boolean cleanDefault) {
        int n = wallpaper.callbacks.beginBroadcast();
        for (int i = 0; i < n; i++) {
            try {
                wallpaper.callbacks.getBroadcastItem(i).onWallpaperChanged();
            } catch (RemoteException e) {
            }
        }
        wallpaper.callbacks.finishBroadcast();
        Intent intent = new Intent("android.intent.action.WALLPAPER_CHANGED");
        intent.putExtra("android.service.wallpaper.extra.FROM_FOREGROUND_APP", wallpaper.fromForegroundApp || cleanDefault);
        this.mContext.sendBroadcastAsUser(intent, new UserHandle(this.mCurrentUserId));
    }

    private void checkPermission(String permission) {
        if (!hasPermission(permission)) {
            throw new SecurityException("Access denied to process: " + Binder.getCallingPid() + ", must have permission " + permission);
        }
    }

    private boolean packageBelongsToUid(String packageName, int uid) {
        int userId = UserHandle.getUserId(uid);
        try {
            int packageUid = this.mContext.getPackageManager().getPackageUidAsUser(packageName, userId);
            return packageUid == uid;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void enforcePackageBelongsToUid(String packageName, int uid) {
        if (!packageBelongsToUid(packageName, uid)) {
            throw new IllegalArgumentException("Invalid package or package does not belong to uid:" + uid);
        }
    }

    public boolean isWallpaperSupported(String callingPackage) {
        int callingUid = Binder.getCallingUid();
        enforcePackageBelongsToUid(callingPackage, callingUid);
        return this.mAppOpsManager.checkOpNoThrow(48, callingUid, callingPackage) == 0;
    }

    public boolean isSetWallpaperAllowed(String callingPackage) {
        PackageManager pm = this.mContext.getPackageManager();
        String[] uidPackages = pm.getPackagesForUid(Binder.getCallingUid());
        boolean uidMatchPackage = Arrays.asList(uidPackages).contains(callingPackage);
        if (!uidMatchPackage) {
            return false;
        }
        DevicePolicyManagerInternal dpmi = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        if (dpmi != null && dpmi.isDeviceOrProfileOwnerInCallingUser(callingPackage)) {
            return true;
        }
        int callingUserId = UserHandle.getCallingUserId();
        long ident = Binder.clearCallingIdentity();
        try {
            UserManagerInternal umi = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
            return true ^ umi.hasUserRestriction("no_set_wallpaper", callingUserId);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean isWallpaperBackupEligible(int which, int userId) {
        WallpaperData wallpaper;
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call isWallpaperBackupEligible");
        }
        if (which == 2) {
            wallpaper = this.mLockWallpaperMap.get(userId);
        } else {
            wallpaper = this.mWallpaperMap.get(userId);
        }
        if (wallpaper != null) {
            return wallpaper.allowBackup;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onDisplayReadyInternal(int displayId) {
        synchronized (this.mLock) {
            WallpaperData wallpaperData = this.mLastWallpaper;
            if (wallpaperData == null) {
                return;
            }
            if (supportsMultiDisplay(wallpaperData.connection)) {
                WallpaperConnection.DisplayConnector connector = this.mLastWallpaper.connection.getDisplayConnectorOrCreate(displayId);
                if (connector == null) {
                    return;
                }
                connector.connectLocked(this.mLastWallpaper.connection, this.mLastWallpaper);
                return;
            }
            WallpaperData wallpaperData2 = this.mFallbackWallpaper;
            if (wallpaperData2 != null) {
                WallpaperConnection.DisplayConnector connector2 = wallpaperData2.connection.getDisplayConnectorOrCreate(displayId);
                if (connector2 == null) {
                    return;
                }
                connector2.connectLocked(this.mFallbackWallpaper.connection, this.mFallbackWallpaper);
            } else {
                Slog.w(TAG, "No wallpaper can be added to the new display");
            }
        }
    }

    private JournaledFile makeJournaledFile(int userId) {
        String base = new File(getWallpaperDir(userId), WALLPAPER_INFO).getAbsolutePath();
        return new JournaledFile(new File(base), new File(base + ".tmp"));
    }

    void saveSettingsLocked(int userId) {
        JournaledFile journal = makeJournaledFile(userId);
        FileOutputStream fstream = null;
        try {
            fstream = new FileOutputStream(journal.chooseForWrite(), false);
            TypedXmlSerializer out = Xml.resolveSerializer(fstream);
            out.startDocument((String) null, true);
            WallpaperData wallpaper = this.mWallpaperMap.get(userId);
            if (wallpaper != null) {
                writeWallpaperAttributes(out, "wp", wallpaper);
            }
            WallpaperData wallpaper2 = this.mLockWallpaperMap.get(userId);
            if (wallpaper2 != null) {
                writeWallpaperAttributes(out, "kwp", wallpaper2);
            }
            out.endDocument();
            fstream.flush();
            FileUtils.sync(fstream);
            fstream.close();
            journal.commit();
        } catch (IOException e) {
            IoUtils.closeQuietly(fstream);
            journal.rollback();
        }
    }

    void writeWallpaperAttributes(TypedXmlSerializer out, String tag, WallpaperData wallpaper) throws IllegalArgumentException, IllegalStateException, IOException {
        DisplayData wpdData = getDisplayDataOrCreate(0);
        out.startTag((String) null, tag);
        out.attributeInt((String) null, "id", wallpaper.wallpaperId);
        out.attributeInt((String) null, "width", wpdData.mWidth);
        out.attributeInt((String) null, "height", wpdData.mHeight);
        out.attributeInt((String) null, "cropLeft", wallpaper.cropHint.left);
        out.attributeInt((String) null, "cropTop", wallpaper.cropHint.top);
        out.attributeInt((String) null, "cropRight", wallpaper.cropHint.right);
        out.attributeInt((String) null, "cropBottom", wallpaper.cropHint.bottom);
        if (wpdData.mPadding.left != 0) {
            out.attributeInt((String) null, "paddingLeft", wpdData.mPadding.left);
        }
        if (wpdData.mPadding.top != 0) {
            out.attributeInt((String) null, "paddingTop", wpdData.mPadding.top);
        }
        if (wpdData.mPadding.right != 0) {
            out.attributeInt((String) null, "paddingRight", wpdData.mPadding.right);
        }
        if (wpdData.mPadding.bottom != 0) {
            out.attributeInt((String) null, "paddingBottom", wpdData.mPadding.bottom);
        }
        out.attributeFloat((String) null, "dimAmount", wallpaper.mWallpaperDimAmount);
        int dimAmountsCount = wallpaper.mUidToDimAmount.size();
        out.attributeInt((String) null, "dimAmountsCount", dimAmountsCount);
        if (dimAmountsCount > 0) {
            int index = 0;
            for (Map.Entry<Integer, Float> entry : wallpaper.mUidToDimAmount.entrySet()) {
                out.attributeInt((String) null, "dimUID" + index, entry.getKey().intValue());
                out.attributeFloat((String) null, "dimValue" + index, entry.getValue().floatValue());
                index++;
            }
        }
        if (wallpaper.primaryColors != null) {
            int colorsCount = wallpaper.primaryColors.getMainColors().size();
            out.attributeInt((String) null, "colorsCount", colorsCount);
            if (colorsCount > 0) {
                for (int i = 0; i < colorsCount; i++) {
                    Color wc = (Color) wallpaper.primaryColors.getMainColors().get(i);
                    out.attributeInt((String) null, "colorValue" + i, wc.toArgb());
                }
            }
            int allColorsCount = wallpaper.primaryColors.getAllColors().size();
            out.attributeInt((String) null, "allColorsCount", allColorsCount);
            if (allColorsCount > 0) {
                int index2 = 0;
                for (Map.Entry<Integer, Integer> entry2 : wallpaper.primaryColors.getAllColors().entrySet()) {
                    out.attributeInt((String) null, "allColorsValue" + index2, entry2.getKey().intValue());
                    out.attributeInt((String) null, "allColorsPopulation" + index2, entry2.getValue().intValue());
                    index2++;
                }
            }
            out.attributeInt((String) null, "colorHints", wallpaper.primaryColors.getColorHints());
        }
        out.attribute((String) null, "name", wallpaper.name);
        if (wallpaper.wallpaperComponent != null && !wallpaper.wallpaperComponent.equals(this.mImageWallpaper)) {
            out.attribute((String) null, "component", wallpaper.wallpaperComponent.flattenToShortString());
        }
        if (wallpaper.allowBackup) {
            out.attributeBoolean((String) null, HostingRecord.HOSTING_TYPE_BACKUP, true);
        }
        out.endTag((String) null, tag);
    }

    private void migrateFromOld() {
        File preNWallpaper = new File(getWallpaperDir(0), WALLPAPER_CROP);
        File originalWallpaper = new File("/data/data/com.android.settings/files/wallpaper");
        File newWallpaper = new File(getWallpaperDir(0), WALLPAPER);
        if (preNWallpaper.exists()) {
            if (!newWallpaper.exists()) {
                FileUtils.copyFile(preNWallpaper, newWallpaper);
            }
        } else if (originalWallpaper.exists()) {
            File oldInfo = new File("/data/system/wallpaper_info.xml");
            if (oldInfo.exists()) {
                File newInfo = new File(getWallpaperDir(0), WALLPAPER_INFO);
                oldInfo.renameTo(newInfo);
            }
            FileUtils.copyFile(originalWallpaper, preNWallpaper);
            originalWallpaper.renameTo(newWallpaper);
        }
    }

    private int getAttributeInt(TypedXmlPullParser parser, String name, int defValue) {
        return parser.getAttributeInt((String) null, name, defValue);
    }

    private float getAttributeFloat(TypedXmlPullParser parser, String name, float defValue) {
        return parser.getAttributeFloat((String) null, name, defValue);
    }

    WallpaperData getWallpaperSafeLocked(int userId, int which) {
        SparseArray<WallpaperData> whichSet = which == 2 ? this.mLockWallpaperMap : this.mWallpaperMap;
        WallpaperData wallpaper = whichSet.get(userId);
        if (wallpaper == null) {
            loadSettingsLocked(userId, false);
            WallpaperData wallpaper2 = whichSet.get(userId);
            if (wallpaper2 == null) {
                if (which == 2) {
                    WallpaperData wallpaper3 = new WallpaperData(userId, getWallpaperDir(userId), WALLPAPER_LOCK_ORIG, WALLPAPER_LOCK_CROP);
                    this.mLockWallpaperMap.put(userId, wallpaper3);
                    ensureSaneWallpaperData(wallpaper3);
                    return wallpaper3;
                }
                Slog.wtf(TAG, "Didn't find wallpaper in non-lock case!");
                WallpaperData wallpaper4 = new WallpaperData(userId, getWallpaperDir(userId), WALLPAPER, WALLPAPER_CROP);
                this.mWallpaperMap.put(userId, wallpaper4);
                ensureSaneWallpaperData(wallpaper4);
                return wallpaper4;
            }
            return wallpaper2;
        }
        return wallpaper;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3740=5, 3742=5, 3744=5, 3746=5, 3748=5, 3750=5] */
    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:119:0x021c  */
    /* JADX WARN: Removed duplicated region for block: B:120:0x0231  */
    /* JADX WARN: Removed duplicated region for block: B:125:0x024c  */
    /* JADX WARN: Removed duplicated region for block: B:138:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void loadSettingsLocked(int userId, boolean keepDimensionHints) {
        WallpaperData wallpaper;
        WallpaperData lockWallpaper;
        JournaledFile journal;
        FileInputStream stream;
        JournaledFile journal2 = makeJournaledFile(userId);
        FileInputStream stream2 = null;
        File file = journal2.chooseForRead();
        WallpaperData wallpaper2 = this.mWallpaperMap.get(userId);
        if (wallpaper2 == null) {
            migrateFromOld();
            WallpaperData wallpaper3 = new WallpaperData(userId, getWallpaperDir(userId), WALLPAPER, WALLPAPER_CROP);
            wallpaper3.allowBackup = true;
            this.mWallpaperMap.put(userId, wallpaper3);
            if (!wallpaper3.cropExists()) {
                if (wallpaper3.sourceExists()) {
                    generateCrop(wallpaper3);
                } else {
                    Slog.i(TAG, "No static wallpaper imagery; defaults will be shown");
                }
            }
            initializeFallbackWallpaper();
            wallpaper = wallpaper3;
        } else {
            wallpaper = wallpaper2;
        }
        boolean success = false;
        DisplayData wpdData = getDisplayDataOrCreate(0);
        try {
            stream2 = new FileInputStream(file);
            try {
                TypedXmlPullParser parser = Xml.resolvePullParser(stream2);
                while (true) {
                    int type = parser.next();
                    if (type == 2) {
                        String tag = parser.getName();
                        if ("wp".equals(tag)) {
                            parseWallpaperAttributes(parser, wallpaper, keepDimensionHints);
                            journal = journal2;
                            ComponentName componentName = null;
                            try {
                                String comp = parser.getAttributeValue((String) null, "component");
                                if (comp != null) {
                                    try {
                                        componentName = ComponentName.unflattenFromString(comp);
                                    } catch (FileNotFoundException e) {
                                        Slog.w(TAG, "no current wallpaper -- first boot?");
                                        IoUtils.closeQuietly(stream2);
                                        if (!success) {
                                        }
                                        ensureSaneWallpaperDisplaySize(wpdData, 0);
                                        ensureSaneWallpaperData(wallpaper);
                                        lockWallpaper = this.mLockWallpaperMap.get(userId);
                                        if (lockWallpaper != null) {
                                        }
                                    } catch (IOException e2) {
                                        e = e2;
                                        Slog.w(TAG, "failed parsing " + file + " " + e);
                                        IoUtils.closeQuietly(stream2);
                                        if (!success) {
                                        }
                                        ensureSaneWallpaperDisplaySize(wpdData, 0);
                                        ensureSaneWallpaperData(wallpaper);
                                        lockWallpaper = this.mLockWallpaperMap.get(userId);
                                        if (lockWallpaper != null) {
                                        }
                                    } catch (IndexOutOfBoundsException e3) {
                                        e = e3;
                                        Slog.w(TAG, "failed parsing " + file + " " + e);
                                        IoUtils.closeQuietly(stream2);
                                        if (!success) {
                                        }
                                        ensureSaneWallpaperDisplaySize(wpdData, 0);
                                        ensureSaneWallpaperData(wallpaper);
                                        lockWallpaper = this.mLockWallpaperMap.get(userId);
                                        if (lockWallpaper != null) {
                                        }
                                    } catch (NullPointerException e4) {
                                        e = e4;
                                        Slog.w(TAG, "failed parsing " + file + " " + e);
                                        IoUtils.closeQuietly(stream2);
                                        if (!success) {
                                        }
                                        ensureSaneWallpaperDisplaySize(wpdData, 0);
                                        ensureSaneWallpaperData(wallpaper);
                                        lockWallpaper = this.mLockWallpaperMap.get(userId);
                                        if (lockWallpaper != null) {
                                        }
                                    } catch (NumberFormatException e5) {
                                        e = e5;
                                        Slog.w(TAG, "failed parsing " + file + " " + e);
                                        IoUtils.closeQuietly(stream2);
                                        if (!success) {
                                        }
                                        ensureSaneWallpaperDisplaySize(wpdData, 0);
                                        ensureSaneWallpaperData(wallpaper);
                                        lockWallpaper = this.mLockWallpaperMap.get(userId);
                                        if (lockWallpaper != null) {
                                        }
                                    } catch (XmlPullParserException e6) {
                                        e = e6;
                                        Slog.w(TAG, "failed parsing " + file + " " + e);
                                        IoUtils.closeQuietly(stream2);
                                        if (!success) {
                                        }
                                        ensureSaneWallpaperDisplaySize(wpdData, 0);
                                        ensureSaneWallpaperData(wallpaper);
                                        lockWallpaper = this.mLockWallpaperMap.get(userId);
                                        if (lockWallpaper != null) {
                                        }
                                    }
                                }
                                wallpaper.nextWallpaperComponent = componentName;
                                if (wallpaper.nextWallpaperComponent != null) {
                                    stream = stream2;
                                    try {
                                        if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(wallpaper.nextWallpaperComponent.getPackageName())) {
                                        }
                                    } catch (FileNotFoundException e7) {
                                        stream2 = stream;
                                        Slog.w(TAG, "no current wallpaper -- first boot?");
                                        IoUtils.closeQuietly(stream2);
                                        if (!success) {
                                        }
                                        ensureSaneWallpaperDisplaySize(wpdData, 0);
                                        ensureSaneWallpaperData(wallpaper);
                                        lockWallpaper = this.mLockWallpaperMap.get(userId);
                                        if (lockWallpaper != null) {
                                        }
                                    } catch (IOException e8) {
                                        e = e8;
                                        stream2 = stream;
                                        Slog.w(TAG, "failed parsing " + file + " " + e);
                                        IoUtils.closeQuietly(stream2);
                                        if (!success) {
                                        }
                                        ensureSaneWallpaperDisplaySize(wpdData, 0);
                                        ensureSaneWallpaperData(wallpaper);
                                        lockWallpaper = this.mLockWallpaperMap.get(userId);
                                        if (lockWallpaper != null) {
                                        }
                                    } catch (IndexOutOfBoundsException e9) {
                                        e = e9;
                                        stream2 = stream;
                                        Slog.w(TAG, "failed parsing " + file + " " + e);
                                        IoUtils.closeQuietly(stream2);
                                        if (!success) {
                                        }
                                        ensureSaneWallpaperDisplaySize(wpdData, 0);
                                        ensureSaneWallpaperData(wallpaper);
                                        lockWallpaper = this.mLockWallpaperMap.get(userId);
                                        if (lockWallpaper != null) {
                                        }
                                    } catch (NullPointerException e10) {
                                        e = e10;
                                        stream2 = stream;
                                        Slog.w(TAG, "failed parsing " + file + " " + e);
                                        IoUtils.closeQuietly(stream2);
                                        if (!success) {
                                        }
                                        ensureSaneWallpaperDisplaySize(wpdData, 0);
                                        ensureSaneWallpaperData(wallpaper);
                                        lockWallpaper = this.mLockWallpaperMap.get(userId);
                                        if (lockWallpaper != null) {
                                        }
                                    } catch (NumberFormatException e11) {
                                        e = e11;
                                        stream2 = stream;
                                        Slog.w(TAG, "failed parsing " + file + " " + e);
                                        IoUtils.closeQuietly(stream2);
                                        if (!success) {
                                        }
                                        ensureSaneWallpaperDisplaySize(wpdData, 0);
                                        ensureSaneWallpaperData(wallpaper);
                                        lockWallpaper = this.mLockWallpaperMap.get(userId);
                                        if (lockWallpaper != null) {
                                        }
                                    } catch (XmlPullParserException e12) {
                                        e = e12;
                                        stream2 = stream;
                                        Slog.w(TAG, "failed parsing " + file + " " + e);
                                        IoUtils.closeQuietly(stream2);
                                        if (!success) {
                                        }
                                        ensureSaneWallpaperDisplaySize(wpdData, 0);
                                        ensureSaneWallpaperData(wallpaper);
                                        lockWallpaper = this.mLockWallpaperMap.get(userId);
                                        if (lockWallpaper != null) {
                                        }
                                    }
                                } else {
                                    stream = stream2;
                                }
                                wallpaper.nextWallpaperComponent = this.mImageWallpaper;
                            } catch (FileNotFoundException e13) {
                            } catch (IOException e14) {
                                e = e14;
                            } catch (IndexOutOfBoundsException e15) {
                                e = e15;
                            } catch (NullPointerException e16) {
                                e = e16;
                            } catch (NumberFormatException e17) {
                                e = e17;
                            } catch (XmlPullParserException e18) {
                                e = e18;
                            }
                        } else {
                            journal = journal2;
                            stream = stream2;
                            if ("kwp".equals(tag)) {
                                WallpaperData lockWallpaper2 = this.mLockWallpaperMap.get(userId);
                                if (lockWallpaper2 == null) {
                                    lockWallpaper2 = new WallpaperData(userId, getWallpaperDir(userId), WALLPAPER_LOCK_ORIG, WALLPAPER_LOCK_CROP);
                                    this.mLockWallpaperMap.put(userId, lockWallpaper2);
                                }
                                parseWallpaperAttributes(parser, lockWallpaper2, false);
                            }
                        }
                    } else {
                        journal = journal2;
                        stream = stream2;
                    }
                    if (type == 1) {
                        break;
                    }
                    journal2 = journal;
                    stream2 = stream;
                }
                success = true;
                stream2 = stream;
            } catch (FileNotFoundException e19) {
            } catch (IOException e20) {
                e = e20;
            } catch (IndexOutOfBoundsException e21) {
                e = e21;
            } catch (NullPointerException e22) {
                e = e22;
            } catch (NumberFormatException e23) {
                e = e23;
            } catch (XmlPullParserException e24) {
                e = e24;
            }
        } catch (FileNotFoundException e25) {
        } catch (IOException e26) {
            e = e26;
        } catch (IndexOutOfBoundsException e27) {
            e = e27;
        } catch (NullPointerException e28) {
            e = e28;
        } catch (NumberFormatException e29) {
            e = e29;
        } catch (XmlPullParserException e30) {
            e = e30;
        }
        IoUtils.closeQuietly(stream2);
        if (!success) {
            wallpaper.cropHint.set(0, 0, 0, 0);
            wpdData.mPadding.set(0, 0, 0, 0);
            wallpaper.name = "";
            this.mLockWallpaperMap.remove(userId);
        } else if (wallpaper.wallpaperId <= 0) {
            wallpaper.wallpaperId = makeWallpaperIdLocked();
        }
        ensureSaneWallpaperDisplaySize(wpdData, 0);
        ensureSaneWallpaperData(wallpaper);
        lockWallpaper = this.mLockWallpaperMap.get(userId);
        if (lockWallpaper != null) {
            ensureSaneWallpaperData(lockWallpaper);
        }
    }

    private void initializeFallbackWallpaper() {
        if (this.mFallbackWallpaper == null) {
            WallpaperData wallpaperData = new WallpaperData(0, getWallpaperDir(0), WALLPAPER, WALLPAPER_CROP);
            this.mFallbackWallpaper = wallpaperData;
            wallpaperData.allowBackup = false;
            this.mFallbackWallpaper.wallpaperId = makeWallpaperIdLocked();
            bindWallpaperComponentLocked(this.mImageWallpaper, true, false, this.mFallbackWallpaper, null);
        }
    }

    private void ensureSaneWallpaperData(WallpaperData wallpaper) {
        if (wallpaper.cropHint.width() < 0 || wallpaper.cropHint.height() < 0) {
            wallpaper.cropHint.set(0, 0, 0, 0);
        }
    }

    void parseWallpaperAttributes(TypedXmlPullParser parser, WallpaperData wallpaper, boolean keepDimensionHints) throws XmlPullParserException {
        int id = parser.getAttributeInt((String) null, "id", -1);
        if (id != -1) {
            wallpaper.wallpaperId = id;
            if (id > this.mWallpaperId) {
                this.mWallpaperId = id;
            }
        } else {
            wallpaper.wallpaperId = makeWallpaperIdLocked();
        }
        DisplayData wpData = getDisplayDataOrCreate(0);
        if (!keepDimensionHints) {
            wpData.mWidth = parser.getAttributeInt((String) null, "width");
            wpData.mHeight = parser.getAttributeInt((String) null, "height");
        }
        wallpaper.cropHint.left = getAttributeInt(parser, "cropLeft", 0);
        wallpaper.cropHint.top = getAttributeInt(parser, "cropTop", 0);
        wallpaper.cropHint.right = getAttributeInt(parser, "cropRight", 0);
        wallpaper.cropHint.bottom = getAttributeInt(parser, "cropBottom", 0);
        wpData.mPadding.left = getAttributeInt(parser, "paddingLeft", 0);
        wpData.mPadding.top = getAttributeInt(parser, "paddingTop", 0);
        wpData.mPadding.right = getAttributeInt(parser, "paddingRight", 0);
        wpData.mPadding.bottom = getAttributeInt(parser, "paddingBottom", 0);
        wallpaper.mWallpaperDimAmount = getAttributeFloat(parser, "dimAmount", 0.0f);
        int dimAmountsCount = getAttributeInt(parser, "dimAmountsCount", 0);
        if (dimAmountsCount > 0) {
            ArrayMap<Integer, Float> allDimAmounts = new ArrayMap<>(dimAmountsCount);
            for (int i = 0; i < dimAmountsCount; i++) {
                int uid = getAttributeInt(parser, "dimUID" + i, 0);
                float dimValue = getAttributeFloat(parser, "dimValue" + i, 0.0f);
                allDimAmounts.put(Integer.valueOf(uid), Float.valueOf(dimValue));
            }
            wallpaper.mUidToDimAmount = allDimAmounts;
        }
        int colorsCount = getAttributeInt(parser, "colorsCount", 0);
        int allColorsCount = getAttributeInt(parser, "allColorsCount", 0);
        if (allColorsCount > 0) {
            Map<Integer, Integer> allColors = new HashMap<>(allColorsCount);
            for (int i2 = 0; i2 < allColorsCount; i2++) {
                int colorInt = getAttributeInt(parser, "allColorsValue" + i2, 0);
                int population = getAttributeInt(parser, "allColorsPopulation" + i2, 0);
                allColors.put(Integer.valueOf(colorInt), Integer.valueOf(population));
            }
            int colorHints = getAttributeInt(parser, "colorHints", 0);
            wallpaper.primaryColors = new WallpaperColors(allColors, colorHints);
        } else if (colorsCount > 0) {
            Color primary = null;
            Color secondary = null;
            Color tertiary = null;
            for (int i3 = 0; i3 < colorsCount; i3++) {
                Color color = Color.valueOf(getAttributeInt(parser, "colorValue" + i3, 0));
                if (i3 == 0) {
                    primary = color;
                } else if (i3 == 1) {
                    secondary = color;
                } else if (i3 != 2) {
                    break;
                } else {
                    tertiary = color;
                }
            }
            int colorHints2 = getAttributeInt(parser, "colorHints", 0);
            wallpaper.primaryColors = new WallpaperColors(primary, secondary, tertiary, colorHints2);
        }
        wallpaper.name = parser.getAttributeValue((String) null, "name");
        wallpaper.allowBackup = parser.getAttributeBoolean((String) null, HostingRecord.HOSTING_TYPE_BACKUP, false);
    }

    public void settingsRestored() {
        WallpaperData wallpaper;
        boolean success;
        if (Binder.getCallingUid() != 1000) {
            throw new RuntimeException("settingsRestored() can only be called from the system process");
        }
        synchronized (this.mLock) {
            loadSettingsLocked(0, false);
            wallpaper = this.mWallpaperMap.get(0);
            wallpaper.wallpaperId = makeWallpaperIdLocked();
            wallpaper.allowBackup = true;
            if (wallpaper.nextWallpaperComponent != null && !wallpaper.nextWallpaperComponent.equals(this.mImageWallpaper)) {
                if (!bindWallpaperComponentLocked(wallpaper.nextWallpaperComponent, false, false, wallpaper, null)) {
                    bindWallpaperComponentLocked(null, false, false, wallpaper, null);
                }
                success = true;
            } else {
                if ("".equals(wallpaper.name)) {
                    success = true;
                } else {
                    success = restoreNamedResourceLocked(wallpaper);
                }
                if (success) {
                    generateCrop(wallpaper);
                    bindWallpaperComponentLocked(wallpaper.nextWallpaperComponent, true, false, wallpaper, null);
                }
            }
        }
        if (!success) {
            Slog.e(TAG, "Failed to restore wallpaper: '" + wallpaper.name + "'");
            wallpaper.name = "";
            getWallpaperDir(0).delete();
        }
        synchronized (this.mLock) {
            saveSettingsLocked(0);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3988=4, 3989=5, 3990=6, 3992=5, 3993=6, 3995=4, 3996=4] */
    /* JADX WARN: Code restructure failed: missing block: B:53:0x0139, code lost:
        if (0 != 0) goto L61;
     */
    /* JADX WARN: Code restructure failed: missing block: B:54:0x013b, code lost:
        android.os.FileUtils.sync(null);
     */
    /* JADX WARN: Code restructure failed: missing block: B:55:0x013e, code lost:
        libcore.io.IoUtils.closeQuietly((java.lang.AutoCloseable) null);
        libcore.io.IoUtils.closeQuietly((java.lang.AutoCloseable) null);
     */
    /* JADX WARN: Code restructure failed: missing block: B:61:0x0165, code lost:
        if (0 != 0) goto L61;
     */
    /* JADX WARN: Code restructure failed: missing block: B:68:0x018e, code lost:
        if (0 != 0) goto L61;
     */
    /* JADX WARN: Code restructure failed: missing block: B:83:?, code lost:
        return false;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private boolean restoreNamedResourceLocked(WallpaperData wallpaper) {
        String pkg;
        String ident;
        String type;
        if (wallpaper.name.length() <= 4 || !"res:".equals(wallpaper.name.substring(0, 4))) {
            return false;
        }
        String resName = wallpaper.name.substring(4);
        int colon = resName.indexOf(58);
        if (colon > 0) {
            String pkg2 = resName.substring(0, colon);
            pkg = pkg2;
        } else {
            pkg = null;
        }
        int slash = resName.lastIndexOf(47);
        if (slash > 0) {
            String ident2 = resName.substring(slash + 1);
            ident = ident2;
        } else {
            ident = null;
        }
        if (colon <= 0 || slash <= 0 || slash - colon <= 1) {
            type = null;
        } else {
            String type2 = resName.substring(colon + 1, slash);
            type = type2;
        }
        if (pkg == null || ident == null || type == null) {
            return false;
        }
        try {
            try {
                Context c = this.mContext.createPackageContext(pkg, 4);
                Resources r = c.getResources();
                int resId = r.getIdentifier(resName, null, null);
                if (resId == 0) {
                    Slog.e(TAG, "couldn't resolve identifier pkg=" + pkg + " type=" + type + " ident=" + ident);
                    IoUtils.closeQuietly((AutoCloseable) null);
                    if (0 != 0) {
                        FileUtils.sync(null);
                    }
                    if (0 != 0) {
                        FileUtils.sync(null);
                    }
                    IoUtils.closeQuietly((AutoCloseable) null);
                    IoUtils.closeQuietly((AutoCloseable) null);
                    return false;
                }
                InputStream res = r.openRawResource(resId);
                if (wallpaper.wallpaperFile.exists()) {
                    wallpaper.wallpaperFile.delete();
                    wallpaper.cropFile.delete();
                }
                FileOutputStream fos = new FileOutputStream(wallpaper.wallpaperFile);
                FileOutputStream cos = new FileOutputStream(wallpaper.cropFile);
                byte[] buffer = new byte[32768];
                while (true) {
                    int amt = res.read(buffer);
                    if (amt <= 0) {
                        Slog.v(TAG, "Restored wallpaper: " + resName);
                        IoUtils.closeQuietly(res);
                        FileUtils.sync(fos);
                        FileUtils.sync(cos);
                        IoUtils.closeQuietly(fos);
                        IoUtils.closeQuietly(cos);
                        return true;
                    }
                    fos.write(buffer, 0, amt);
                    cos.write(buffer, 0, amt);
                }
            } catch (PackageManager.NameNotFoundException e) {
                Slog.e(TAG, "Package name " + pkg + " not found");
                IoUtils.closeQuietly((AutoCloseable) null);
                if (0 != 0) {
                    FileUtils.sync(null);
                }
            } catch (Resources.NotFoundException e2) {
                Slog.e(TAG, "Resource not found: -1");
                IoUtils.closeQuietly((AutoCloseable) null);
                if (0 != 0) {
                    FileUtils.sync(null);
                }
            } catch (IOException e3) {
                Slog.e(TAG, "IOException while restoring wallpaper ", e3);
                IoUtils.closeQuietly((AutoCloseable) null);
                if (0 != 0) {
                    FileUtils.sync(null);
                }
            }
        } catch (Throwable th) {
            IoUtils.closeQuietly((AutoCloseable) null);
            if (0 != 0) {
                FileUtils.sync(null);
            }
            if (0 != 0) {
                FileUtils.sync(null);
            }
            IoUtils.closeQuietly((AutoCloseable) null);
            IoUtils.closeQuietly((AutoCloseable) null);
            throw th;
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.wallpaper.WallpaperManagerService */
    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new WallpaperManagerShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
    }

    protected void dump(FileDescriptor fd, final PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            pw.print("mDefaultWallpaperComponent=");
            pw.println(this.mDefaultWallpaperComponent);
            pw.print("mImageWallpaper=");
            pw.println(this.mImageWallpaper);
            synchronized (this.mLock) {
                pw.println("System wallpaper state:");
                for (int i = 0; i < this.mWallpaperMap.size(); i++) {
                    WallpaperData wallpaper = this.mWallpaperMap.valueAt(i);
                    pw.print(" User ");
                    pw.print(wallpaper.userId);
                    pw.print(": id=");
                    pw.println(wallpaper.wallpaperId);
                    pw.println(" Display state:");
                    forEachDisplayData(new Consumer() { // from class: com.android.server.wallpaper.WallpaperManagerService$$ExternalSyntheticLambda1
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            WallpaperManagerService.lambda$dump$14(pw, (WallpaperManagerService.DisplayData) obj);
                        }
                    });
                    pw.print("  mCropHint=");
                    pw.println(wallpaper.cropHint);
                    pw.print("  mName=");
                    pw.println(wallpaper.name);
                    pw.print("  mAllowBackup=");
                    pw.println(wallpaper.allowBackup);
                    pw.print("  mWallpaperComponent=");
                    pw.println(wallpaper.wallpaperComponent);
                    pw.print("  mWallpaperDimAmount=");
                    pw.println(wallpaper.mWallpaperDimAmount);
                    pw.print("  isColorExtracted=");
                    pw.println(wallpaper.mIsColorExtractedFromDim);
                    pw.println("  mUidToDimAmount:");
                    for (Map.Entry<Integer, Float> entry : wallpaper.mUidToDimAmount.entrySet()) {
                        pw.print("    UID=");
                        pw.print(entry.getKey());
                        pw.print(" dimAmount=");
                        pw.println(entry.getValue());
                    }
                    if (wallpaper.connection != null) {
                        WallpaperConnection conn = wallpaper.connection;
                        pw.print("  Wallpaper connection ");
                        pw.print(conn);
                        pw.println(":");
                        if (conn.mInfo != null) {
                            pw.print("    mInfo.component=");
                            pw.println(conn.mInfo.getComponent());
                        }
                        conn.forEachDisplayConnector(new Consumer() { // from class: com.android.server.wallpaper.WallpaperManagerService$$ExternalSyntheticLambda2
                            @Override // java.util.function.Consumer
                            public final void accept(Object obj) {
                                WallpaperManagerService.lambda$dump$15(pw, (WallpaperManagerService.WallpaperConnection.DisplayConnector) obj);
                            }
                        });
                        pw.print("    mService=");
                        pw.println(conn.mService);
                        pw.print("    mLastDiedTime=");
                        pw.println(wallpaper.lastDiedTime - SystemClock.uptimeMillis());
                    }
                }
                pw.println("Lock wallpaper state:");
                for (int i2 = 0; i2 < this.mLockWallpaperMap.size(); i2++) {
                    WallpaperData wallpaper2 = this.mLockWallpaperMap.valueAt(i2);
                    pw.print(" User ");
                    pw.print(wallpaper2.userId);
                    pw.print(": id=");
                    pw.println(wallpaper2.wallpaperId);
                    pw.print("  mCropHint=");
                    pw.println(wallpaper2.cropHint);
                    pw.print("  mName=");
                    pw.println(wallpaper2.name);
                    pw.print("  mAllowBackup=");
                    pw.println(wallpaper2.allowBackup);
                    pw.print("  mWallpaperDimAmount=");
                    pw.println(wallpaper2.mWallpaperDimAmount);
                }
                pw.println("Fallback wallpaper state:");
                pw.print(" User ");
                pw.print(this.mFallbackWallpaper.userId);
                pw.print(": id=");
                pw.println(this.mFallbackWallpaper.wallpaperId);
                pw.print("  mCropHint=");
                pw.println(this.mFallbackWallpaper.cropHint);
                pw.print("  mName=");
                pw.println(this.mFallbackWallpaper.name);
                pw.print("  mAllowBackup=");
                pw.println(this.mFallbackWallpaper.allowBackup);
                if (this.mFallbackWallpaper.connection != null) {
                    WallpaperConnection conn2 = this.mFallbackWallpaper.connection;
                    pw.print("  Fallback Wallpaper connection ");
                    pw.print(conn2);
                    pw.println(":");
                    if (conn2.mInfo != null) {
                        pw.print("    mInfo.component=");
                        pw.println(conn2.mInfo.getComponent());
                    }
                    conn2.forEachDisplayConnector(new Consumer() { // from class: com.android.server.wallpaper.WallpaperManagerService$$ExternalSyntheticLambda3
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            WallpaperManagerService.lambda$dump$16(pw, (WallpaperManagerService.WallpaperConnection.DisplayConnector) obj);
                        }
                    });
                    pw.print("    mService=");
                    pw.println(conn2.mService);
                    pw.print("    mLastDiedTime=");
                    pw.println(this.mFallbackWallpaper.lastDiedTime - SystemClock.uptimeMillis());
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dump$14(PrintWriter pw, DisplayData wpSize) {
        pw.print("  displayId=");
        pw.println(wpSize.mDisplayId);
        pw.print("  mWidth=");
        pw.print(wpSize.mWidth);
        pw.print("  mHeight=");
        pw.println(wpSize.mHeight);
        pw.print("  mPadding=");
        pw.println(wpSize.mPadding);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dump$15(PrintWriter pw, WallpaperConnection.DisplayConnector connector) {
        pw.print("     mDisplayId=");
        pw.println(connector.mDisplayId);
        pw.print("     mToken=");
        pw.println(connector.mToken);
        pw.print("     mEngine=");
        pw.println(connector.mEngine);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dump$16(PrintWriter pw, WallpaperConnection.DisplayConnector connector) {
        pw.print("     mDisplayId=");
        pw.println(connector.mDisplayId);
        pw.print("     mToken=");
        pw.println(connector.mToken);
        pw.print("     mEngine=");
        pw.println(connector.mEngine);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$17$com-android-server-wallpaper-WallpaperManagerService  reason: not valid java name */
    public /* synthetic */ void m7694xd100cbbc() {
        if (this.mLastToken != null) {
            ((WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class)).removeWindowToken(this.mLastToken, false, false, this.mLastDisplayId);
        }
        try {
            if (this.mLastEngine != null) {
                Slog.d(TAG, "destory engine run");
                this.mLastEngine.destroy();
            }
        } catch (RemoteException e) {
        }
        this.mLastEngine = null;
        this.mLastToken = null;
    }
}
