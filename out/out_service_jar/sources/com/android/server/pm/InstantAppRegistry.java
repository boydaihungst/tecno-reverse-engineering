package com.android.server.pm;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.InstantAppInfo;
import android.content.pm.PermissionInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.permission.PermissionManager;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.PackageUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.InstantAppRegistry;
import com.android.server.pm.parsing.PackageInfoUtils;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.AndroidPackageUtils;
import com.android.server.pm.permission.PermissionManagerServiceInternal;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.PackageStateUtils;
import com.android.server.pm.pkg.PackageUserStateInternal;
import com.android.server.utils.Snappable;
import com.android.server.utils.SnapshotCache;
import com.android.server.utils.Watchable;
import com.android.server.utils.WatchableImpl;
import com.android.server.utils.Watched;
import com.android.server.utils.WatchedSparseArray;
import com.android.server.utils.WatchedSparseBooleanArray;
import com.android.server.utils.Watcher;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import libcore.io.IoUtils;
import libcore.util.HexEncoding;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class InstantAppRegistry implements Watchable, Snappable {
    private static final String ATTR_GRANTED = "granted";
    private static final String ATTR_LABEL = "label";
    private static final String ATTR_NAME = "name";
    private static final boolean DEBUG = false;
    private static final long DEFAULT_INSTALLED_INSTANT_APP_MAX_CACHE_PERIOD = 15552000000L;
    static final long DEFAULT_INSTALLED_INSTANT_APP_MIN_CACHE_PERIOD = 604800000;
    private static final long DEFAULT_UNINSTALLED_INSTANT_APP_MAX_CACHE_PERIOD = 15552000000L;
    static final long DEFAULT_UNINSTALLED_INSTANT_APP_MIN_CACHE_PERIOD = 604800000;
    private static final String INSTANT_APPS_FOLDER = "instant";
    private static final String INSTANT_APP_ANDROID_ID_FILE = "android_id";
    private static final String INSTANT_APP_COOKIE_FILE_PREFIX = "cookie_";
    private static final String INSTANT_APP_COOKIE_FILE_SIFFIX = ".dat";
    private static final String INSTANT_APP_ICON_FILE = "icon.png";
    private static final String INSTANT_APP_METADATA_FILE = "metadata.xml";
    private static final String LOG_TAG = "InstantAppRegistry";
    private static final String TAG_PACKAGE = "package";
    private static final String TAG_PERMISSION = "permission";
    private static final String TAG_PERMISSIONS = "permissions";
    private final Context mContext;
    private final CookiePersistence mCookiePersistence;
    private final DeletePackageHelper mDeletePackageHelper;
    @Watched
    private final WatchedSparseArray<WatchedSparseBooleanArray> mInstalledInstantAppUids;
    @Watched
    private final WatchedSparseArray<WatchedSparseArray<WatchedSparseBooleanArray>> mInstantGrants;
    private final Object mLock;
    private final Watcher mObserver;
    private final PermissionManagerServiceInternal mPermissionManager;
    private final SnapshotCache<InstantAppRegistry> mSnapshot;
    @Watched
    private final WatchedSparseArray<List<UninstalledInstantAppState>> mUninstalledInstantApps;
    private final UserManagerInternal mUserManager;
    private final WatchableImpl mWatchable;

    @Override // com.android.server.utils.Watchable
    public void registerObserver(Watcher observer) {
        this.mWatchable.registerObserver(observer);
    }

    @Override // com.android.server.utils.Watchable
    public void unregisterObserver(Watcher observer) {
        this.mWatchable.unregisterObserver(observer);
    }

    @Override // com.android.server.utils.Watchable
    public boolean isRegisteredObserver(Watcher observer) {
        return this.mWatchable.isRegisteredObserver(observer);
    }

    @Override // com.android.server.utils.Watchable
    public void dispatchChange(Watchable what) {
        this.mWatchable.dispatchChange(what);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onChanged() {
        dispatchChange(this);
    }

    private SnapshotCache<InstantAppRegistry> makeCache() {
        return new SnapshotCache<InstantAppRegistry>(this, this) { // from class: com.android.server.pm.InstantAppRegistry.2
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // com.android.server.utils.SnapshotCache
            public InstantAppRegistry createSnapshot() {
                InstantAppRegistry s = new InstantAppRegistry();
                s.mWatchable.seal();
                return s;
            }
        };
    }

    public InstantAppRegistry(Context context, PermissionManagerServiceInternal permissionManager, UserManagerInternal userManager, DeletePackageHelper deletePackageHelper) {
        this.mLock = new Object();
        this.mWatchable = new WatchableImpl();
        Watcher watcher = new Watcher() { // from class: com.android.server.pm.InstantAppRegistry.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable what) {
                InstantAppRegistry.this.onChanged();
            }
        };
        this.mObserver = watcher;
        this.mContext = context;
        this.mPermissionManager = permissionManager;
        this.mUserManager = userManager;
        this.mDeletePackageHelper = deletePackageHelper;
        this.mCookiePersistence = new CookiePersistence(BackgroundThread.getHandler().getLooper());
        WatchedSparseArray<List<UninstalledInstantAppState>> watchedSparseArray = new WatchedSparseArray<>();
        this.mUninstalledInstantApps = watchedSparseArray;
        WatchedSparseArray<WatchedSparseArray<WatchedSparseBooleanArray>> watchedSparseArray2 = new WatchedSparseArray<>();
        this.mInstantGrants = watchedSparseArray2;
        WatchedSparseArray<WatchedSparseBooleanArray> watchedSparseArray3 = new WatchedSparseArray<>();
        this.mInstalledInstantAppUids = watchedSparseArray3;
        watchedSparseArray.registerObserver(watcher);
        watchedSparseArray2.registerObserver(watcher);
        watchedSparseArray3.registerObserver(watcher);
        Watchable.verifyWatchedAttributes(this, watcher);
        this.mSnapshot = makeCache();
    }

    private InstantAppRegistry(InstantAppRegistry r) {
        this.mLock = new Object();
        this.mWatchable = new WatchableImpl();
        this.mObserver = new Watcher() { // from class: com.android.server.pm.InstantAppRegistry.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable what) {
                InstantAppRegistry.this.onChanged();
            }
        };
        this.mContext = r.mContext;
        this.mPermissionManager = r.mPermissionManager;
        this.mUserManager = r.mUserManager;
        this.mDeletePackageHelper = r.mDeletePackageHelper;
        this.mCookiePersistence = null;
        this.mUninstalledInstantApps = new WatchedSparseArray<>(r.mUninstalledInstantApps);
        this.mInstantGrants = new WatchedSparseArray<>(r.mInstantGrants);
        this.mInstalledInstantAppUids = new WatchedSparseArray<>(r.mInstalledInstantAppUids);
        this.mSnapshot = null;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.utils.Snappable
    public InstantAppRegistry snapshot() {
        return this.mSnapshot.snapshot();
    }

    public byte[] getInstantAppCookie(AndroidPackage pkg, int userId) {
        synchronized (this.mLock) {
            byte[] pendingCookie = this.mCookiePersistence.getPendingPersistCookieLPr(pkg, userId);
            if (pendingCookie != null) {
                return pendingCookie;
            }
            File cookieFile = peekInstantCookieFile(pkg.getPackageName(), userId);
            if (cookieFile != null && cookieFile.exists()) {
                try {
                    return IoUtils.readFileAsByteArray(cookieFile.toString());
                } catch (IOException e) {
                    Slog.w(LOG_TAG, "Error reading cookie file: " + cookieFile);
                }
            }
            return null;
        }
    }

    public boolean setInstantAppCookie(AndroidPackage pkg, byte[] cookie, int instantAppCookieMaxBytes, int userId) {
        synchronized (this.mLock) {
            if (cookie != null) {
                if (cookie.length > 0 && cookie.length > instantAppCookieMaxBytes) {
                    Slog.e(LOG_TAG, "Instant app cookie for package " + pkg.getPackageName() + " size " + cookie.length + " bytes while max size is " + instantAppCookieMaxBytes);
                    return false;
                }
            }
            this.mCookiePersistence.schedulePersistLPw(userId, pkg, cookie);
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void persistInstantApplicationCookie(byte[] cookie, String packageName, File cookieFile, int userId) {
        synchronized (this.mLock) {
            File appDir = getInstantApplicationDir(packageName, userId);
            if (!appDir.exists() && !appDir.mkdirs()) {
                Slog.e(LOG_TAG, "Cannot create instant app cookie directory");
                return;
            }
            if (cookieFile.exists() && !cookieFile.delete()) {
                Slog.e(LOG_TAG, "Cannot delete instant app cookie file");
            }
            if (cookie != null && cookie.length > 0) {
                try {
                    FileOutputStream fos = new FileOutputStream(cookieFile);
                    fos.write(cookie, 0, cookie.length);
                    fos.close();
                } catch (IOException e) {
                    Slog.e(LOG_TAG, "Error writing instant app cookie file: " + cookieFile, e);
                }
            }
        }
    }

    public Bitmap getInstantAppIcon(String packageName, int userId) {
        synchronized (this.mLock) {
            File iconFile = new File(getInstantApplicationDir(packageName, userId), INSTANT_APP_ICON_FILE);
            if (iconFile.exists()) {
                return BitmapFactory.decodeFile(iconFile.toString());
            }
            return null;
        }
    }

    public String getInstantAppAndroidId(String packageName, int userId) {
        FileOutputStream fos;
        synchronized (this.mLock) {
            File idFile = new File(getInstantApplicationDir(packageName, userId), INSTANT_APP_ANDROID_ID_FILE);
            if (idFile.exists()) {
                try {
                    return IoUtils.readFileAsString(idFile.getAbsolutePath());
                } catch (IOException e) {
                    Slog.e(LOG_TAG, "Failed to read instant app android id file: " + idFile, e);
                }
            }
            byte[] randomBytes = new byte[8];
            new SecureRandom().nextBytes(randomBytes);
            String id = HexEncoding.encodeToString(randomBytes, false);
            File appDir = getInstantApplicationDir(packageName, userId);
            if (!appDir.exists() && !appDir.mkdirs()) {
                Slog.e(LOG_TAG, "Cannot create instant app cookie directory");
                return id;
            }
            File idFile2 = new File(getInstantApplicationDir(packageName, userId), INSTANT_APP_ANDROID_ID_FILE);
            try {
                fos = new FileOutputStream(idFile2);
            } catch (IOException e2) {
                Slog.e(LOG_TAG, "Error writing instant app android id file: " + idFile2, e2);
            }
            try {
                fos.write(id.getBytes());
                fos.close();
                return id;
            } catch (Throwable th) {
                try {
                    fos.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        }
    }

    public List<InstantAppInfo> getInstantApps(Computer computer, int userId) {
        List<InstantAppInfo> installedApps = getInstalledInstantApplications(computer, userId);
        List<InstantAppInfo> uninstalledApps = getUninstalledInstantApplications(computer, userId);
        if (installedApps != null) {
            if (uninstalledApps != null) {
                installedApps.addAll(uninstalledApps);
            }
            return installedApps;
        }
        return uninstalledApps;
    }

    public void onPackageInstalled(Computer computer, String packageName, int[] userIds) {
        PackageStateInternal ps = computer.getPackageStateInternal(packageName);
        final AndroidPackage pkg = ps == null ? null : ps.getPkg();
        if (pkg == null) {
            return;
        }
        synchronized (this.mLock) {
            for (int userId : userIds) {
                if (ps.getUserStateOrDefault(userId).isInstalled()) {
                    propagateInstantAppPermissionsIfNeeded(pkg, userId);
                    if (ps.getUserStateOrDefault(userId).isInstantApp()) {
                        addInstantApp(userId, ps.getAppId());
                    }
                    removeUninstalledInstantAppStateLPw(new Predicate() { // from class: com.android.server.pm.InstantAppRegistry$$ExternalSyntheticLambda3
                        @Override // java.util.function.Predicate
                        public final boolean test(Object obj) {
                            boolean equals;
                            equals = ((InstantAppRegistry.UninstalledInstantAppState) obj).mInstantAppInfo.getPackageName().equals(AndroidPackage.this.getPackageName());
                            return equals;
                        }
                    }, userId);
                    File instantAppDir = getInstantApplicationDir(pkg.getPackageName(), userId);
                    new File(instantAppDir, INSTANT_APP_METADATA_FILE).delete();
                    new File(instantAppDir, INSTANT_APP_ICON_FILE).delete();
                    File currentCookieFile = peekInstantCookieFile(pkg.getPackageName(), userId);
                    if (currentCookieFile != null) {
                        String cookieName = currentCookieFile.getName();
                        String currentCookieSha256 = cookieName.substring(INSTANT_APP_COOKIE_FILE_PREFIX.length(), cookieName.length() - INSTANT_APP_COOKIE_FILE_SIFFIX.length());
                        if (pkg.getSigningDetails().checkCapability(currentCookieSha256, 1)) {
                            return;
                        }
                        String[] signaturesSha256Digests = PackageUtils.computeSignaturesSha256Digests(pkg.getSigningDetails().getSignatures());
                        for (String s : signaturesSha256Digests) {
                            if (s.equals(currentCookieSha256)) {
                                return;
                            }
                        }
                        Slog.i(LOG_TAG, "Signature for package " + pkg.getPackageName() + " changed - dropping cookie");
                        this.mCookiePersistence.cancelPendingPersistLPw(pkg, userId);
                        currentCookieFile.delete();
                    }
                }
            }
        }
    }

    public void onPackageUninstalled(AndroidPackage pkg, PackageSetting ps, int[] userIds, boolean packageInstalledForSomeUsers) {
        if (ps == null) {
            return;
        }
        synchronized (this.mLock) {
            for (int userId : userIds) {
                if (!packageInstalledForSomeUsers || !ps.getInstalled(userId)) {
                    if (ps.getInstantApp(userId)) {
                        addUninstalledInstantAppLPw(ps, userId);
                        removeInstantAppLPw(userId, ps.getAppId());
                    } else {
                        deleteDir(getInstantApplicationDir(pkg.getPackageName(), userId));
                        this.mCookiePersistence.cancelPendingPersistLPw(pkg, userId);
                        removeAppLPw(userId, ps.getAppId());
                    }
                }
            }
        }
    }

    public void onUserRemoved(int userId) {
        synchronized (this.mLock) {
            this.mUninstalledInstantApps.remove(userId);
            this.mInstalledInstantAppUids.remove(userId);
            this.mInstantGrants.remove(userId);
            deleteDir(getInstantApplicationsDir(userId));
        }
    }

    public boolean isInstantAccessGranted(int userId, int targetAppId, int instantAppId) {
        synchronized (this.mLock) {
            WatchedSparseArray<WatchedSparseBooleanArray> targetAppList = this.mInstantGrants.get(userId);
            if (targetAppList == null) {
                return false;
            }
            WatchedSparseBooleanArray instantGrantList = targetAppList.get(targetAppId);
            if (instantGrantList == null) {
                return false;
            }
            return instantGrantList.get(instantAppId);
        }
    }

    public boolean grantInstantAccess(int userId, Intent intent, int recipientUid, int instantAppId) {
        Set<String> categories;
        synchronized (this.mLock) {
            WatchedSparseArray<WatchedSparseBooleanArray> watchedSparseArray = this.mInstalledInstantAppUids;
            if (watchedSparseArray == null) {
                return false;
            }
            WatchedSparseBooleanArray instantAppList = watchedSparseArray.get(userId);
            if (instantAppList != null && instantAppList.get(instantAppId)) {
                if (instantAppList.get(recipientUid)) {
                    return false;
                }
                if (intent == null || !"android.intent.action.VIEW".equals(intent.getAction()) || (categories = intent.getCategories()) == null || !categories.contains("android.intent.category.BROWSABLE")) {
                    WatchedSparseArray<WatchedSparseBooleanArray> targetAppList = this.mInstantGrants.get(userId);
                    if (targetAppList == null) {
                        targetAppList = new WatchedSparseArray<>();
                        this.mInstantGrants.put(userId, targetAppList);
                    }
                    WatchedSparseBooleanArray instantGrantList = targetAppList.get(recipientUid);
                    if (instantGrantList == null) {
                        instantGrantList = new WatchedSparseBooleanArray();
                        targetAppList.put(recipientUid, instantGrantList);
                    }
                    instantGrantList.put(instantAppId, true);
                    return true;
                }
                return false;
            }
            return false;
        }
    }

    public void addInstantApp(int userId, int instantAppId) {
        synchronized (this.mLock) {
            WatchedSparseBooleanArray instantAppList = this.mInstalledInstantAppUids.get(userId);
            if (instantAppList == null) {
                instantAppList = new WatchedSparseBooleanArray();
                this.mInstalledInstantAppUids.put(userId, instantAppList);
            }
            instantAppList.put(instantAppId, true);
        }
        onChanged();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [582=4] */
    private void removeInstantAppLPw(int userId, int instantAppId) {
        WatchedSparseBooleanArray instantAppList;
        WatchedSparseArray<WatchedSparseBooleanArray> watchedSparseArray = this.mInstalledInstantAppUids;
        if (watchedSparseArray == null || (instantAppList = watchedSparseArray.get(userId)) == null) {
            return;
        }
        try {
            instantAppList.delete(instantAppId);
            WatchedSparseArray<WatchedSparseArray<WatchedSparseBooleanArray>> watchedSparseArray2 = this.mInstantGrants;
            if (watchedSparseArray2 == null) {
                return;
            }
            WatchedSparseArray<WatchedSparseBooleanArray> targetAppList = watchedSparseArray2.get(userId);
            if (targetAppList == null) {
                return;
            }
            for (int i = targetAppList.size() - 1; i >= 0; i--) {
                targetAppList.valueAt(i).delete(instantAppId);
            }
        } finally {
            onChanged();
        }
    }

    private void removeAppLPw(int userId, int targetAppId) {
        WatchedSparseArray<WatchedSparseBooleanArray> targetAppList;
        WatchedSparseArray<WatchedSparseArray<WatchedSparseBooleanArray>> watchedSparseArray = this.mInstantGrants;
        if (watchedSparseArray == null || (targetAppList = watchedSparseArray.get(userId)) == null) {
            return;
        }
        targetAppList.delete(targetAppId);
        onChanged();
    }

    private void addUninstalledInstantAppLPw(PackageStateInternal packageState, int userId) {
        InstantAppInfo uninstalledApp = createInstantAppInfoForPackage(packageState, userId, false);
        if (uninstalledApp == null) {
            return;
        }
        List<UninstalledInstantAppState> uninstalledAppStates = this.mUninstalledInstantApps.get(userId);
        if (uninstalledAppStates == null) {
            uninstalledAppStates = new ArrayList();
            this.mUninstalledInstantApps.put(userId, uninstalledAppStates);
        }
        UninstalledInstantAppState uninstalledAppState = new UninstalledInstantAppState(uninstalledApp, System.currentTimeMillis());
        uninstalledAppStates.add(uninstalledAppState);
        writeUninstalledInstantAppMetadata(uninstalledApp, userId);
        writeInstantApplicationIconLPw(packageState.getPkg(), userId);
    }

    private void writeInstantApplicationIconLPw(AndroidPackage pkg, int userId) {
        Bitmap bitmap;
        File appDir = getInstantApplicationDir(pkg.getPackageName(), userId);
        if (!appDir.exists()) {
            return;
        }
        Drawable icon = AndroidPackageUtils.generateAppInfoWithoutState(pkg).loadIcon(this.mContext.getPackageManager());
        if (icon instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) icon).getBitmap();
        } else {
            bitmap = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
            icon.draw(canvas);
        }
        File iconFile = new File(getInstantApplicationDir(pkg.getPackageName(), userId), INSTANT_APP_ICON_FILE);
        try {
            FileOutputStream out = new FileOutputStream(iconFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();
        } catch (Exception e) {
            Slog.e(LOG_TAG, "Error writing instant app icon", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasInstantApplicationMetadata(String packageName, int userId) {
        return hasUninstalledInstantAppState(packageName, userId) || hasInstantAppMetadata(packageName, userId);
    }

    public void deleteInstantApplicationMetadata(final String packageName, int userId) {
        synchronized (this.mLock) {
            removeUninstalledInstantAppStateLPw(new Predicate() { // from class: com.android.server.pm.InstantAppRegistry$$ExternalSyntheticLambda2
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    boolean equals;
                    equals = ((InstantAppRegistry.UninstalledInstantAppState) obj).mInstantAppInfo.getPackageName().equals(packageName);
                    return equals;
                }
            }, userId);
            File instantAppDir = getInstantApplicationDir(packageName, userId);
            new File(instantAppDir, INSTANT_APP_METADATA_FILE).delete();
            new File(instantAppDir, INSTANT_APP_ICON_FILE).delete();
            new File(instantAppDir, INSTANT_APP_ANDROID_ID_FILE).delete();
            File cookie = peekInstantCookieFile(packageName, userId);
            if (cookie != null) {
                cookie.delete();
            }
        }
    }

    private void removeUninstalledInstantAppStateLPw(Predicate<UninstalledInstantAppState> criteria, int userId) {
        List<UninstalledInstantAppState> uninstalledAppStates;
        WatchedSparseArray<List<UninstalledInstantAppState>> watchedSparseArray = this.mUninstalledInstantApps;
        if (watchedSparseArray == null || (uninstalledAppStates = watchedSparseArray.get(userId)) == null) {
            return;
        }
        int appCount = uninstalledAppStates.size();
        for (int i = appCount - 1; i >= 0; i--) {
            UninstalledInstantAppState uninstalledAppState = uninstalledAppStates.get(i);
            if (criteria.test(uninstalledAppState)) {
                uninstalledAppStates.remove(i);
                if (uninstalledAppStates.isEmpty()) {
                    this.mUninstalledInstantApps.remove(userId);
                    onChanged();
                    return;
                }
            }
        }
    }

    private boolean hasUninstalledInstantAppState(String packageName, int userId) {
        synchronized (this.mLock) {
            WatchedSparseArray<List<UninstalledInstantAppState>> watchedSparseArray = this.mUninstalledInstantApps;
            if (watchedSparseArray == null) {
                return false;
            }
            List<UninstalledInstantAppState> uninstalledAppStates = watchedSparseArray.get(userId);
            if (uninstalledAppStates == null) {
                return false;
            }
            int appCount = uninstalledAppStates.size();
            for (int i = 0; i < appCount; i++) {
                UninstalledInstantAppState uninstalledAppState = uninstalledAppStates.get(i);
                if (packageName.equals(uninstalledAppState.mInstantAppInfo.getPackageName())) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean hasInstantAppMetadata(String packageName, int userId) {
        File instantAppDir = getInstantApplicationDir(packageName, userId);
        return new File(instantAppDir, INSTANT_APP_METADATA_FILE).exists() || new File(instantAppDir, INSTANT_APP_ICON_FILE).exists() || new File(instantAppDir, INSTANT_APP_ANDROID_ID_FILE).exists() || peekInstantCookieFile(packageName, userId) != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void pruneInstantApps(Computer computer) {
        long maxInstalledCacheDuration = Settings.Global.getLong(this.mContext.getContentResolver(), "installed_instant_app_max_cache_period", 15552000000L);
        long maxUninstalledCacheDuration = Settings.Global.getLong(this.mContext.getContentResolver(), "uninstalled_instant_app_max_cache_period", 15552000000L);
        try {
            pruneInstantApps(computer, JobStatus.NO_LATEST_RUNTIME, maxInstalledCacheDuration, maxUninstalledCacheDuration);
        } catch (IOException e) {
            Slog.e(LOG_TAG, "Error pruning installed and uninstalled instant apps", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean pruneInstalledInstantApps(Computer computer, long neededSpace, long maxInstalledCacheDuration) {
        try {
            return pruneInstantApps(computer, neededSpace, maxInstalledCacheDuration, JobStatus.NO_LATEST_RUNTIME);
        } catch (IOException e) {
            Slog.e(LOG_TAG, "Error pruning installed instant apps", e);
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean pruneUninstalledInstantApps(Computer computer, long neededSpace, long maxUninstalledCacheDuration) {
        try {
            return pruneInstantApps(computer, neededSpace, JobStatus.NO_LATEST_RUNTIME, maxUninstalledCacheDuration);
        } catch (IOException e) {
            Slog.e(LOG_TAG, "Error pruning uninstalled instant apps", e);
            return false;
        }
    }

    private boolean pruneInstantApps(Computer computer, long neededSpace, long maxInstalledCacheDuration, final long maxUninstalledCacheDuration) throws IOException {
        int[] iArr;
        int i;
        long now;
        File[] files;
        int i2;
        long now2;
        StorageManager storage;
        InstantAppRegistry instantAppRegistry = this;
        StorageManager storage2 = (StorageManager) instantAppRegistry.mContext.getSystemService(StorageManager.class);
        File file = storage2.findPathForUuid(StorageManager.UUID_PRIVATE_INTERNAL);
        if (file.getUsableSpace() >= neededSpace) {
            return true;
        }
        long now3 = System.currentTimeMillis();
        int[] allUsers = instantAppRegistry.mUserManager.getUserIds();
        final ArrayMap<String, ? extends PackageStateInternal> packageStates = computer.getPackageStates();
        int packageStateCount = packageStates.size();
        List<String> packagesToDelete = null;
        int i3 = 0;
        while (i3 < packageStateCount) {
            PackageStateInternal ps = packageStates.valueAt(i3);
            AndroidPackage pkg = ps == null ? null : ps.getPkg();
            if (pkg == null) {
                storage = storage2;
            } else if (now3 - ps.getTransientState().getLatestPackageUseTimeInMills() < maxInstalledCacheDuration) {
                storage = storage2;
            } else {
                boolean installedOnlyAsInstantApp = false;
                int length = allUsers.length;
                int i4 = 0;
                while (true) {
                    if (i4 >= length) {
                        storage = storage2;
                        break;
                    }
                    storage = storage2;
                    PackageUserStateInternal userState = ps.getUserStateOrDefault(allUsers[i4]);
                    if (userState.isInstalled()) {
                        if (userState.isInstantApp()) {
                            installedOnlyAsInstantApp = true;
                        } else {
                            installedOnlyAsInstantApp = false;
                            break;
                        }
                    }
                    i4++;
                    storage2 = storage;
                }
                if (installedOnlyAsInstantApp) {
                    if (packagesToDelete == null) {
                        packagesToDelete = new ArrayList<>();
                    }
                    packagesToDelete.add(pkg.getPackageName());
                }
            }
            i3++;
            storage2 = storage;
        }
        if (packagesToDelete != null) {
            packagesToDelete.sort(new Comparator() { // from class: com.android.server.pm.InstantAppRegistry$$ExternalSyntheticLambda0
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    return InstantAppRegistry.lambda$pruneInstantApps$2(packageStates, (String) obj, (String) obj2);
                }
            });
        }
        synchronized (instantAppRegistry.mLock) {
            if (packagesToDelete != null) {
                try {
                    int packageCount = packagesToDelete.size();
                    for (int i5 = 0; i5 < packageCount; i5++) {
                        String packageToDelete = packagesToDelete.get(i5);
                        if (instantAppRegistry.mDeletePackageHelper.deletePackageX(packageToDelete, -1L, 0, 2, true) == 1 && file.getUsableSpace() >= neededSpace) {
                            return true;
                        }
                    }
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            }
            try {
                int[] userIds = instantAppRegistry.mUserManager.getUserIds();
                int length2 = userIds.length;
                int i6 = 0;
                while (i6 < length2) {
                    int userId = userIds[i6];
                    instantAppRegistry.removeUninstalledInstantAppStateLPw(new Predicate() { // from class: com.android.server.pm.InstantAppRegistry$$ExternalSyntheticLambda1
                        @Override // java.util.function.Predicate
                        public final boolean test(Object obj) {
                            return InstantAppRegistry.lambda$pruneInstantApps$3(maxUninstalledCacheDuration, (InstantAppRegistry.UninstalledInstantAppState) obj);
                        }
                    }, userId);
                    File instantAppsDir = getInstantApplicationsDir(userId);
                    if (!instantAppsDir.exists()) {
                        iArr = userIds;
                        i = length2;
                        now = now3;
                    } else {
                        iArr = userIds;
                        File[] files2 = instantAppsDir.listFiles();
                        if (files2 == null) {
                            i = length2;
                            now = now3;
                        } else {
                            int length3 = files2.length;
                            i = length2;
                            int i7 = 0;
                            while (i7 < length3) {
                                File instantDir = files2[i7];
                                if (!instantDir.isDirectory()) {
                                    files = files2;
                                    i2 = length3;
                                    now2 = now3;
                                } else {
                                    files = files2;
                                    i2 = length3;
                                    now2 = now3;
                                    File metadataFile = new File(instantDir, INSTANT_APP_METADATA_FILE);
                                    if (metadataFile.exists()) {
                                        long elapsedCachingMillis = System.currentTimeMillis() - metadataFile.lastModified();
                                        if (elapsedCachingMillis > maxUninstalledCacheDuration) {
                                            deleteDir(instantDir);
                                            if (file.getUsableSpace() >= neededSpace) {
                                                return true;
                                            }
                                        }
                                    }
                                }
                                i7++;
                                files2 = files;
                                length3 = i2;
                                now3 = now2;
                            }
                            now = now3;
                        }
                    }
                    try {
                        i6++;
                        instantAppRegistry = this;
                        userIds = iArr;
                        length2 = i;
                        now3 = now;
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                }
                return false;
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ int lambda$pruneInstantApps$2(ArrayMap packageStates, String lhs, String rhs) {
        PackageStateInternal rhsPs;
        PackageStateInternal lhsPkgState = (PackageStateInternal) packageStates.get(lhs);
        PackageStateInternal rhsPkgState = (PackageStateInternal) packageStates.get(rhs);
        AndroidPackage lhsPkg = lhsPkgState == null ? null : lhsPkgState.getPkg();
        AndroidPackage rhsPkg = rhsPkgState != null ? rhsPkgState.getPkg() : null;
        if (lhsPkg == null && rhsPkg == null) {
            return 0;
        }
        if (lhsPkg == null) {
            return -1;
        }
        if (rhsPkg == null) {
            return 1;
        }
        PackageStateInternal lhsPs = (PackageStateInternal) packageStates.get(lhsPkg.getPackageName());
        if (lhsPs == null || (rhsPs = (PackageStateInternal) packageStates.get(rhsPkg.getPackageName())) == null) {
            return 0;
        }
        if (lhsPs.getTransientState().getLatestPackageUseTimeInMills() > rhsPs.getTransientState().getLatestPackageUseTimeInMills()) {
            return 1;
        }
        if (lhsPs.getTransientState().getLatestPackageUseTimeInMills() < rhsPs.getTransientState().getLatestPackageUseTimeInMills() || PackageStateUtils.getEarliestFirstInstallTime(lhsPs.getUserStates()) <= PackageStateUtils.getEarliestFirstInstallTime(rhsPs.getUserStates())) {
            return -1;
        }
        return 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$pruneInstantApps$3(long maxUninstalledCacheDuration, UninstalledInstantAppState state) {
        long elapsedCachingMillis = System.currentTimeMillis() - state.mTimestamp;
        return elapsedCachingMillis > maxUninstalledCacheDuration;
    }

    private List<InstantAppInfo> getInstalledInstantApplications(Computer computer, int userId) {
        InstantAppInfo info;
        List<InstantAppInfo> result = null;
        ArrayMap<String, ? extends PackageStateInternal> packageStates = computer.getPackageStates();
        int packageCount = packageStates.size();
        for (int i = 0; i < packageCount; i++) {
            PackageStateInternal ps = packageStates.valueAt(i);
            if (ps != null && ps.getUserStateOrDefault(userId).isInstantApp() && (info = createInstantAppInfoForPackage(ps, userId, true)) != null) {
                if (result == null) {
                    result = new ArrayList<>();
                }
                result.add(info);
            }
        }
        return result;
    }

    private InstantAppInfo createInstantAppInfoForPackage(PackageStateInternal ps, int userId, boolean addApplicationInfo) {
        AndroidPackage pkg = ps.getPkg();
        if (pkg == null || !ps.getUserStateOrDefault(userId).isInstalled()) {
            return null;
        }
        String[] requestedPermissions = new String[pkg.getRequestedPermissions().size()];
        pkg.getRequestedPermissions().toArray(requestedPermissions);
        Set<String> permissions = this.mPermissionManager.getGrantedPermissions(pkg.getPackageName(), userId);
        String[] grantedPermissions = new String[permissions.size()];
        permissions.toArray(grantedPermissions);
        ApplicationInfo appInfo = PackageInfoUtils.generateApplicationInfo(ps.getPkg(), 0L, ps.getUserStateOrDefault(userId), userId, ps);
        if (addApplicationInfo) {
            return new InstantAppInfo(appInfo, requestedPermissions, grantedPermissions);
        }
        return new InstantAppInfo(appInfo.packageName, appInfo.loadLabel(this.mContext.getPackageManager()), requestedPermissions, grantedPermissions);
    }

    private List<InstantAppInfo> getUninstalledInstantApplications(Computer computer, int userId) {
        List<UninstalledInstantAppState> uninstalledAppStates = getUninstalledInstantAppStates(userId);
        if (uninstalledAppStates == null || uninstalledAppStates.isEmpty()) {
            return null;
        }
        List<InstantAppInfo> uninstalledApps = null;
        int stateCount = uninstalledAppStates.size();
        for (int i = 0; i < stateCount; i++) {
            UninstalledInstantAppState uninstalledAppState = uninstalledAppStates.get(i);
            if (uninstalledApps == null) {
                uninstalledApps = new ArrayList<>();
            }
            uninstalledApps.add(uninstalledAppState.mInstantAppInfo);
        }
        return uninstalledApps;
    }

    private void propagateInstantAppPermissionsIfNeeded(AndroidPackage pkg, int userId) {
        String[] grantedPermissions;
        InstantAppInfo appInfo = peekOrParseUninstalledInstantAppInfo(pkg.getPackageName(), userId);
        if (appInfo == null || ArrayUtils.isEmpty(appInfo.getGrantedPermissions())) {
            return;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            for (String grantedPermission : appInfo.getGrantedPermissions()) {
                boolean propagatePermission = canPropagatePermission(grantedPermission);
                if (propagatePermission && pkg.getRequestedPermissions().contains(grantedPermission)) {
                    ((PermissionManager) this.mContext.getSystemService(PermissionManager.class)).grantRuntimePermission(pkg.getPackageName(), grantedPermission, UserHandle.of(userId));
                }
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private boolean canPropagatePermission(String permissionName) {
        PermissionManager permissionManager = (PermissionManager) this.mContext.getSystemService(PermissionManager.class);
        PermissionInfo permissionInfo = permissionManager.getPermissionInfo(permissionName, 0);
        if (permissionInfo == null) {
            return false;
        }
        if ((permissionInfo.getProtection() != 1 && (permissionInfo.getProtectionFlags() & 32) == 0) || (permissionInfo.getProtectionFlags() & 4096) == 0) {
            return false;
        }
        return true;
    }

    private InstantAppInfo peekOrParseUninstalledInstantAppInfo(String packageName, int userId) {
        List<UninstalledInstantAppState> uninstalledAppStates;
        synchronized (this.mLock) {
            WatchedSparseArray<List<UninstalledInstantAppState>> watchedSparseArray = this.mUninstalledInstantApps;
            if (watchedSparseArray != null && (uninstalledAppStates = watchedSparseArray.get(userId)) != null) {
                int appCount = uninstalledAppStates.size();
                for (int i = 0; i < appCount; i++) {
                    UninstalledInstantAppState uninstalledAppState = uninstalledAppStates.get(i);
                    if (uninstalledAppState.mInstantAppInfo.getPackageName().equals(packageName)) {
                        return uninstalledAppState.mInstantAppInfo;
                    }
                }
            }
            File metadataFile = new File(getInstantApplicationDir(packageName, userId), INSTANT_APP_METADATA_FILE);
            UninstalledInstantAppState uninstalledAppState2 = parseMetadataFile(metadataFile);
            if (uninstalledAppState2 == null) {
                return null;
            }
            return uninstalledAppState2.mInstantAppInfo;
        }
    }

    private List<UninstalledInstantAppState> getUninstalledInstantAppStates(int userId) {
        List<UninstalledInstantAppState> uninstalledAppStates;
        File[] files;
        List<UninstalledInstantAppState> uninstalledAppStates2 = null;
        synchronized (this.mLock) {
            WatchedSparseArray<List<UninstalledInstantAppState>> watchedSparseArray = this.mUninstalledInstantApps;
            if (watchedSparseArray == null || (uninstalledAppStates2 = watchedSparseArray.get(userId)) == null) {
                File instantAppsDir = getInstantApplicationsDir(userId);
                if (instantAppsDir.exists() && (files = instantAppsDir.listFiles()) != null) {
                    for (File instantDir : files) {
                        if (instantDir.isDirectory()) {
                            File metadataFile = new File(instantDir, INSTANT_APP_METADATA_FILE);
                            UninstalledInstantAppState uninstalledAppState = parseMetadataFile(metadataFile);
                            if (uninstalledAppState != null) {
                                if (uninstalledAppStates2 == null) {
                                    uninstalledAppStates2 = new ArrayList<>();
                                }
                                uninstalledAppStates2.add(uninstalledAppState);
                            }
                        }
                    }
                    uninstalledAppStates = uninstalledAppStates2;
                } else {
                    uninstalledAppStates = uninstalledAppStates2;
                }
                synchronized (this.mLock) {
                    this.mUninstalledInstantApps.put(userId, uninstalledAppStates);
                }
                return uninstalledAppStates;
            }
            return uninstalledAppStates2;
        }
    }

    private static UninstalledInstantAppState parseMetadataFile(File metadataFile) {
        if (metadataFile.exists()) {
            try {
                FileInputStream in = new AtomicFile(metadataFile).openRead();
                File instantDir = metadataFile.getParentFile();
                long timestamp = metadataFile.lastModified();
                String packageName = instantDir.getName();
                try {
                    try {
                        TypedXmlPullParser parser = Xml.resolvePullParser(in);
                        return new UninstalledInstantAppState(parseMetadata(parser, packageName), timestamp);
                    } catch (IOException | XmlPullParserException e) {
                        throw new IllegalStateException("Failed parsing instant metadata file: " + metadataFile, e);
                    }
                } finally {
                    IoUtils.closeQuietly(in);
                }
            } catch (FileNotFoundException e2) {
                Slog.i(LOG_TAG, "No instant metadata file");
                return null;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static File computeInstantCookieFile(String packageName, String sha256Digest, int userId) {
        File appDir = getInstantApplicationDir(packageName, userId);
        String cookieFile = INSTANT_APP_COOKIE_FILE_PREFIX + sha256Digest + INSTANT_APP_COOKIE_FILE_SIFFIX;
        return new File(appDir, cookieFile);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static File peekInstantCookieFile(String packageName, int userId) {
        File[] files;
        File appDir = getInstantApplicationDir(packageName, userId);
        if (appDir.exists() && (files = appDir.listFiles()) != null) {
            for (File file : files) {
                if (!file.isDirectory() && file.getName().startsWith(INSTANT_APP_COOKIE_FILE_PREFIX) && file.getName().endsWith(INSTANT_APP_COOKIE_FILE_SIFFIX)) {
                    return file;
                }
            }
            return null;
        }
        return null;
    }

    private static InstantAppInfo parseMetadata(TypedXmlPullParser parser, String packageName) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if ("package".equals(parser.getName())) {
                return parsePackage(parser, packageName);
            }
        }
        return null;
    }

    private static InstantAppInfo parsePackage(TypedXmlPullParser parser, String packageName) throws IOException, XmlPullParserException {
        String label = parser.getAttributeValue((String) null, ATTR_LABEL);
        List<String> outRequestedPermissions = new ArrayList<>();
        List<String> outGrantedPermissions = new ArrayList<>();
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if (TAG_PERMISSIONS.equals(parser.getName())) {
                parsePermissions(parser, outRequestedPermissions, outGrantedPermissions);
            }
        }
        String[] requestedPermissions = new String[outRequestedPermissions.size()];
        outRequestedPermissions.toArray(requestedPermissions);
        String[] grantedPermissions = new String[outGrantedPermissions.size()];
        outGrantedPermissions.toArray(grantedPermissions);
        return new InstantAppInfo(packageName, label, requestedPermissions, grantedPermissions);
    }

    private static void parsePermissions(TypedXmlPullParser parser, List<String> outRequestedPermissions, List<String> outGrantedPermissions) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if ("permission".equals(parser.getName())) {
                String permission = XmlUtils.readStringAttribute(parser, "name");
                outRequestedPermissions.add(permission);
                if (parser.getAttributeBoolean((String) null, ATTR_GRANTED, false)) {
                    outGrantedPermissions.add(permission);
                }
            }
        }
    }

    private void writeUninstalledInstantAppMetadata(InstantAppInfo instantApp, int userId) {
        TypedXmlSerializer serializer;
        boolean z;
        File appDir = getInstantApplicationDir(instantApp.getPackageName(), userId);
        if (!appDir.exists() && !appDir.mkdirs()) {
            return;
        }
        File metadataFile = new File(appDir, INSTANT_APP_METADATA_FILE);
        AtomicFile destination = new AtomicFile(metadataFile);
        FileOutputStream out = null;
        try {
            out = destination.startWrite();
            serializer = Xml.resolveSerializer(out);
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startDocument((String) null, true);
            serializer.startTag((String) null, "package");
        } catch (Throwable th) {
            t = th;
        }
        try {
            try {
                serializer.attribute((String) null, ATTR_LABEL, instantApp.loadLabel(this.mContext.getPackageManager()).toString());
                serializer.startTag((String) null, TAG_PERMISSIONS);
                String[] requestedPermissions = instantApp.getRequestedPermissions();
                int length = requestedPermissions.length;
                int i = 0;
                while (i < length) {
                    String permission = requestedPermissions[i];
                    serializer.startTag((String) null, "permission");
                    File appDir2 = appDir;
                    try {
                        serializer.attribute((String) null, "name", permission);
                        if (ArrayUtils.contains(instantApp.getGrantedPermissions(), permission)) {
                            z = true;
                            serializer.attributeBoolean((String) null, ATTR_GRANTED, true);
                        } else {
                            z = true;
                        }
                        serializer.endTag((String) null, "permission");
                        i++;
                        appDir = appDir2;
                    } catch (Throwable th2) {
                        t = th2;
                        try {
                            Slog.wtf(LOG_TAG, "Failed to write instant state, restoring backup", t);
                            destination.failWrite(out);
                        } finally {
                            IoUtils.closeQuietly(out);
                        }
                    }
                }
                serializer.endTag((String) null, TAG_PERMISSIONS);
                serializer.endTag((String) null, "package");
                serializer.endDocument();
                destination.finishWrite(out);
            } catch (Throwable th3) {
                t = th3;
                Slog.wtf(LOG_TAG, "Failed to write instant state, restoring backup", t);
                destination.failWrite(out);
            }
        } catch (Throwable th4) {
            t = th4;
            Slog.wtf(LOG_TAG, "Failed to write instant state, restoring backup", t);
            destination.failWrite(out);
        }
    }

    private static File getInstantApplicationsDir(int userId) {
        return new File(Environment.getUserSystemDirectory(userId), INSTANT_APPS_FOLDER);
    }

    private static File getInstantApplicationDir(String packageName, int userId) {
        return new File(getInstantApplicationsDir(userId), packageName);
    }

    private static void deleteDir(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                deleteDir(file);
            }
        }
        dir.delete();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class UninstalledInstantAppState {
        final InstantAppInfo mInstantAppInfo;
        final long mTimestamp;

        public UninstalledInstantAppState(InstantAppInfo instantApp, long timestamp) {
            this.mInstantAppInfo = instantApp;
            this.mTimestamp = timestamp;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class CookiePersistence extends Handler {
        private static final long PERSIST_COOKIE_DELAY_MILLIS = 1000;
        private final SparseArray<ArrayMap<String, SomeArgs>> mPendingPersistCookies;

        public CookiePersistence(Looper looper) {
            super(looper);
            this.mPendingPersistCookies = new SparseArray<>();
        }

        public void schedulePersistLPw(int userId, AndroidPackage pkg, byte[] cookie) {
            File newCookieFile = InstantAppRegistry.computeInstantCookieFile(pkg.getPackageName(), PackageUtils.computeSignaturesSha256Digest(pkg.getSigningDetails().getSignatures()), userId);
            if (!pkg.getSigningDetails().hasSignatures()) {
                Slog.wtf(InstantAppRegistry.LOG_TAG, "Parsed Instant App contains no valid signatures!");
            }
            File oldCookieFile = InstantAppRegistry.peekInstantCookieFile(pkg.getPackageName(), userId);
            if (oldCookieFile != null && !newCookieFile.equals(oldCookieFile)) {
                oldCookieFile.delete();
            }
            cancelPendingPersistLPw(pkg, userId);
            addPendingPersistCookieLPw(userId, pkg, cookie, newCookieFile);
            sendMessageDelayed(obtainMessage(userId, pkg), 1000L);
        }

        public byte[] getPendingPersistCookieLPr(AndroidPackage pkg, int userId) {
            SomeArgs state;
            ArrayMap<String, SomeArgs> pendingWorkForUser = this.mPendingPersistCookies.get(userId);
            if (pendingWorkForUser != null && (state = pendingWorkForUser.get(pkg.getPackageName())) != null) {
                return (byte[]) state.arg1;
            }
            return null;
        }

        public void cancelPendingPersistLPw(AndroidPackage pkg, int userId) {
            removeMessages(userId, pkg);
            SomeArgs state = removePendingPersistCookieLPr(pkg, userId);
            if (state != null) {
                state.recycle();
            }
        }

        private void addPendingPersistCookieLPw(int userId, AndroidPackage pkg, byte[] cookie, File cookieFile) {
            ArrayMap<String, SomeArgs> pendingWorkForUser = this.mPendingPersistCookies.get(userId);
            if (pendingWorkForUser == null) {
                pendingWorkForUser = new ArrayMap<>();
                this.mPendingPersistCookies.put(userId, pendingWorkForUser);
            }
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = cookie;
            args.arg2 = cookieFile;
            pendingWorkForUser.put(pkg.getPackageName(), args);
        }

        private SomeArgs removePendingPersistCookieLPr(AndroidPackage pkg, int userId) {
            ArrayMap<String, SomeArgs> pendingWorkForUser = this.mPendingPersistCookies.get(userId);
            SomeArgs state = null;
            if (pendingWorkForUser != null) {
                SomeArgs state2 = pendingWorkForUser.remove(pkg.getPackageName());
                state = state2;
                if (pendingWorkForUser.isEmpty()) {
                    this.mPendingPersistCookies.remove(userId);
                }
            }
            return state;
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            int userId = message.what;
            AndroidPackage pkg = (AndroidPackage) message.obj;
            SomeArgs state = removePendingPersistCookieLPr(pkg, userId);
            if (state == null) {
                return;
            }
            byte[] cookie = (byte[]) state.arg1;
            File cookieFile = (File) state.arg2;
            state.recycle();
            InstantAppRegistry.this.persistInstantApplicationCookie(cookie, pkg.getPackageName(), cookieFile, userId);
        }
    }
}
