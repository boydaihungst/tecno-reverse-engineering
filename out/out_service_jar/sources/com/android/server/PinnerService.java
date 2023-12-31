package com.android.server;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.IActivityManager;
import android.app.IUidObserver;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.SystemProperties;
import android.os.UserManager;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.app.ResolverActivity;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.function.QuadConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.SystemService;
import com.android.server.wm.ActivityTaskManagerInternal;
import dalvik.system.DexFile;
import dalvik.system.VMRuntime;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
/* loaded from: classes.dex */
public final class PinnerService extends SystemService {
    private static final boolean DEBUG = false;
    private static final int KEY_ASSISTANT = 2;
    private static final int KEY_CAMERA = 0;
    private static final int KEY_HOME = 1;
    private static final int MATCH_FLAGS = 851968;
    private static final int MAX_ASSISTANT_PIN_SIZE = 62914560;
    private static final int MAX_CAMERA_PIN_SIZE = 83886080;
    private static final int MAX_HOME_PIN_SIZE = 6291456;
    private static final String PIN_META_FILENAME = "pinlist.meta";
    private static final String TAG = "PinnerService";
    private final IActivityManager mAm;
    private final ActivityManagerInternal mAmInternal;
    private final ActivityTaskManagerInternal mAtmInternal;
    private BinderService mBinderService;
    private final BroadcastReceiver mBroadcastReceiver;
    private final boolean mConfiguredToPinAssistant;
    private final boolean mConfiguredToPinCamera;
    private final boolean mConfiguredToPinHome;
    private final Context mContext;
    private final ArrayMap<Integer, Integer> mPendingRepin;
    private ArraySet<Integer> mPinKeys;
    private final ArrayMap<Integer, PinnedApp> mPinnedApps;
    private final ArrayList<PinnedFile> mPinnedFiles;
    private PinnerHandler mPinnerHandler;
    private SearchManager mSearchManager;
    private final UserManager mUserManager;
    private static final int PAGE_SIZE = (int) Os.sysconf(OsConstants._SC_PAGESIZE);
    private static boolean PROP_PIN_PINLIST = SystemProperties.getBoolean("pinner.use_pinlist", true);
    private static boolean PROP_PIN_ODEX = SystemProperties.getBoolean("pinner.whole_odex", true);

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface AppKey {
    }

    public PinnerService(Context context) {
        super(context);
        this.mPinnedFiles = new ArrayList<>();
        this.mPinnedApps = new ArrayMap<>();
        this.mPendingRepin = new ArrayMap<>();
        this.mPinnerHandler = null;
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.PinnerService.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if ("android.intent.action.PACKAGE_REPLACED".equals(intent.getAction())) {
                    Uri packageUri = intent.getData();
                    String packageName = packageUri.getSchemeSpecificPart();
                    ArraySet<String> updatedPackages = new ArraySet<>();
                    updatedPackages.add(packageName);
                    PinnerService.this.update(updatedPackages, true);
                }
            }
        };
        this.mBroadcastReceiver = broadcastReceiver;
        this.mContext = context;
        this.mConfiguredToPinCamera = context.getResources().getBoolean(17891722);
        this.mConfiguredToPinHome = context.getResources().getBoolean(17891723);
        this.mConfiguredToPinAssistant = context.getResources().getBoolean(17891721);
        this.mPinKeys = createPinKeys();
        this.mPinnerHandler = new PinnerHandler(BackgroundThread.get().getLooper());
        this.mAtmInternal = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
        this.mAmInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        this.mAm = ActivityManager.getService();
        this.mUserManager = (UserManager) context.getSystemService(UserManager.class);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_REPLACED");
        filter.addDataScheme("package");
        context.registerReceiver(broadcastReceiver, filter);
        registerUidListener();
        registerUserSetupCompleteListener();
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        BinderService binderService = new BinderService();
        this.mBinderService = binderService;
        publishBinderService("pinner", binderService);
        publishLocalService(PinnerService.class, this);
        this.mPinnerHandler.obtainMessage(4001).sendToTarget();
        sendPinAppsMessage(0);
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 500) {
            this.mSearchManager = (SearchManager) this.mContext.getSystemService("search");
            sendPinAppsMessage(0);
        }
    }

    @Override // com.android.server.SystemService
    public void onUserSwitching(SystemService.TargetUser from, SystemService.TargetUser to) {
        int userId = to.getUserIdentifier();
        if (!this.mUserManager.isManagedProfile(userId)) {
            sendPinAppsMessage(userId);
        }
    }

    @Override // com.android.server.SystemService
    public void onUserUnlocking(SystemService.TargetUser user) {
        int userId = user.getUserIdentifier();
        if (!this.mUserManager.isManagedProfile(userId)) {
            sendPinAppsMessage(userId);
        }
    }

    public void update(ArraySet<String> updatedPackages, boolean force) {
        ArraySet<Integer> pinKeys = getPinKeys();
        int currentUser = ActivityManager.getCurrentUser();
        for (int i = pinKeys.size() - 1; i >= 0; i--) {
            int key = pinKeys.valueAt(i).intValue();
            ApplicationInfo info = getInfoForKey(key, currentUser);
            if (info != null && updatedPackages.contains(info.packageName)) {
                Slog.i(TAG, "Updating pinned files for " + info.packageName + " force=" + force);
                sendPinAppMessage(key, currentUser, force);
            }
        }
    }

    public List<PinnedFileStats> dumpDataForStatsd() {
        List<PinnedFileStats> pinnedFileStats = new ArrayList<>();
        synchronized (this) {
            Iterator<PinnedFile> it = this.mPinnedFiles.iterator();
            while (it.hasNext()) {
                PinnedFile pinnedFile = it.next();
                pinnedFileStats.add(new PinnedFileStats(1000, pinnedFile));
            }
            for (Integer num : this.mPinnedApps.keySet()) {
                int key = num.intValue();
                PinnedApp app = this.mPinnedApps.get(Integer.valueOf(key));
                Iterator<PinnedFile> it2 = this.mPinnedApps.get(Integer.valueOf(key)).mFiles.iterator();
                while (it2.hasNext()) {
                    PinnedFile pinnedFile2 = it2.next();
                    pinnedFileStats.add(new PinnedFileStats(app.uid, pinnedFile2));
                }
            }
        }
        return pinnedFileStats;
    }

    /* loaded from: classes.dex */
    public static class PinnedFileStats {
        public final String filename;
        public final int sizeKb;
        public final int uid;

        protected PinnedFileStats(int uid, PinnedFile file) {
            this.uid = uid;
            this.filename = file.fileName.substring(file.fileName.lastIndexOf(47) + 1);
            this.sizeKb = file.bytesPinned / 1024;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePinOnStart() {
        String[] filesToPin = this.mContext.getResources().getStringArray(17236024);
        int length = filesToPin.length;
        boolean z = false;
        int i = 0;
        while (i < length) {
            String fileToPin = filesToPin[i];
            PinnedFile pf = pinFile(fileToPin, Integer.MAX_VALUE, z);
            if (pf == null) {
                Slog.e(TAG, "Failed to pin file = " + fileToPin);
            } else {
                synchronized (this) {
                    this.mPinnedFiles.add(pf);
                }
                if (fileToPin.endsWith(".jar") || fileToPin.endsWith(".apk")) {
                    String arch = VMRuntime.getInstructionSet(Build.SUPPORTED_ABIS[z ? 1 : 0]);
                    String[] files = null;
                    try {
                        files = DexFile.getDexFileOutputPaths(fileToPin, arch);
                    } catch (IOException e) {
                    }
                    if (files == null) {
                        continue;
                    } else {
                        int length2 = files.length;
                        int i2 = z ? 1 : 0;
                        while (i2 < length2) {
                            String file = files[i2];
                            PinnedFile df = pinFile(file, Integer.MAX_VALUE, z);
                            if (df == null) {
                                Slog.i(TAG, "Failed to pin ART file = " + file);
                                continue;
                            } else {
                                synchronized (this) {
                                    this.mPinnedFiles.add(df);
                                }
                                continue;
                            }
                            i2++;
                            z = false;
                        }
                        continue;
                    }
                } else {
                    continue;
                }
            }
            i++;
            z = false;
        }
    }

    private void registerUserSetupCompleteListener() {
        final Uri userSetupCompleteUri = Settings.Secure.getUriFor("user_setup_complete");
        this.mContext.getContentResolver().registerContentObserver(userSetupCompleteUri, false, new ContentObserver(null) { // from class: com.android.server.PinnerService.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (userSetupCompleteUri.equals(uri)) {
                    PinnerService.this.sendPinAppMessage(1, ActivityManager.getCurrentUser(), true);
                }
            }
        }, -1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.PinnerService$3  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass3 extends IUidObserver.Stub {
        AnonymousClass3() {
        }

        public void onUidGone(int uid, boolean disabled) throws RemoteException {
            PinnerService.this.mPinnerHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.PinnerService$3$$ExternalSyntheticLambda0
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    ((PinnerService) obj).handleUidGone(((Integer) obj2).intValue());
                }
            }, PinnerService.this, Integer.valueOf(uid)));
        }

        public void onUidActive(int uid) throws RemoteException {
            PinnerService.this.mPinnerHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.PinnerService$3$$ExternalSyntheticLambda1
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    ((PinnerService) obj).handleUidActive(((Integer) obj2).intValue());
                }
            }, PinnerService.this, Integer.valueOf(uid)));
        }

        public void onUidIdle(int uid, boolean disabled) throws RemoteException {
        }

        public void onUidStateChanged(int uid, int procState, long procStateSeq, int capability) throws RemoteException {
        }

        public void onUidCachedChanged(int uid, boolean cached) throws RemoteException {
        }

        public void onUidProcAdjChanged(int uid) throws RemoteException {
        }
    }

    private void registerUidListener() {
        try {
            this.mAm.registerUidObserver(new AnonymousClass3(), 10, 0, (String) null);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to register uid observer", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleUidGone(int uid) {
        updateActiveState(uid, false);
        synchronized (this) {
            int key = this.mPendingRepin.getOrDefault(Integer.valueOf(uid), -1).intValue();
            if (key == -1) {
                return;
            }
            this.mPendingRepin.remove(Integer.valueOf(uid));
            pinApp(key, ActivityManager.getCurrentUser(), false);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleUidActive(int uid) {
        updateActiveState(uid, true);
    }

    private void updateActiveState(int uid, boolean active) {
        synchronized (this) {
            for (int i = this.mPinnedApps.size() - 1; i >= 0; i--) {
                PinnedApp app = this.mPinnedApps.valueAt(i);
                if (app.uid == uid) {
                    app.active = active;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unpinApps() {
        ArraySet<Integer> pinKeys = getPinKeys();
        for (int i = pinKeys.size() - 1; i >= 0; i--) {
            int key = pinKeys.valueAt(i).intValue();
            unpinApp(key);
        }
    }

    private void unpinApp(int key) {
        synchronized (this) {
            PinnedApp app = this.mPinnedApps.get(Integer.valueOf(key));
            if (app == null) {
                return;
            }
            this.mPinnedApps.remove(Integer.valueOf(key));
            ArrayList<PinnedFile> pinnedAppFiles = new ArrayList<>(app.mFiles);
            Iterator<PinnedFile> it = pinnedAppFiles.iterator();
            while (it.hasNext()) {
                PinnedFile pinnedFile = it.next();
                pinnedFile.close();
            }
        }
    }

    private boolean isResolverActivity(ActivityInfo info) {
        return ResolverActivity.class.getName().equals(info.name);
    }

    private ApplicationInfo getCameraInfo(int userHandle) {
        Intent cameraIntent = new Intent("android.media.action.STILL_IMAGE_CAMERA");
        ApplicationInfo info = getApplicationInfoForIntent(cameraIntent, userHandle, false);
        if (info == null) {
            Intent cameraIntent2 = new Intent("android.media.action.STILL_IMAGE_CAMERA_SECURE");
            info = getApplicationInfoForIntent(cameraIntent2, userHandle, false);
        }
        if (info == null) {
            Intent cameraIntent3 = new Intent("android.media.action.STILL_IMAGE_CAMERA");
            return getApplicationInfoForIntent(cameraIntent3, userHandle, true);
        }
        return info;
    }

    private ApplicationInfo getHomeInfo(int userHandle) {
        Intent intent = this.mAtmInternal.getHomeIntent();
        return getApplicationInfoForIntent(intent, userHandle, false);
    }

    private ApplicationInfo getAssistantInfo(int userHandle) {
        SearchManager searchManager = this.mSearchManager;
        if (searchManager != null) {
            Intent intent = searchManager.getAssistIntent(false);
            return getApplicationInfoForIntent(intent, userHandle, true);
        }
        return null;
    }

    private ApplicationInfo getApplicationInfoForIntent(Intent intent, int userHandle, boolean defaultToSystemApp) {
        ResolveInfo resolveInfo;
        if (intent == null || (resolveInfo = this.mContext.getPackageManager().resolveActivityAsUser(intent, MATCH_FLAGS, userHandle)) == null) {
            return null;
        }
        if (!isResolverActivity(resolveInfo.activityInfo)) {
            return resolveInfo.activityInfo.applicationInfo;
        }
        if (!defaultToSystemApp) {
            return null;
        }
        List<ResolveInfo> infoList = this.mContext.getPackageManager().queryIntentActivitiesAsUser(intent, MATCH_FLAGS, userHandle);
        ApplicationInfo systemAppInfo = null;
        for (ResolveInfo info : infoList) {
            if ((info.activityInfo.applicationInfo.flags & 1) != 0) {
                if (systemAppInfo != null) {
                    return null;
                }
                systemAppInfo = info.activityInfo.applicationInfo;
            }
        }
        return systemAppInfo;
    }

    private void sendPinAppsMessage(int userHandle) {
        this.mPinnerHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.PinnerService$$ExternalSyntheticLambda3
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((PinnerService) obj).pinApps(((Integer) obj2).intValue());
            }
        }, this, Integer.valueOf(userHandle)));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendPinAppsWithUpdatedKeysMessage(int userHandle) {
        this.mPinnerHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.PinnerService$$ExternalSyntheticLambda1
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((PinnerService) obj).pinAppsWithUpdatedKeys(((Integer) obj2).intValue());
            }
        }, this, Integer.valueOf(userHandle)));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendUnpinAppsMessage() {
        this.mPinnerHandler.sendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.PinnerService$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((PinnerService) obj).unpinApps();
            }
        }, this));
    }

    private ArraySet<Integer> createPinKeys() {
        ArraySet<Integer> pinKeys = new ArraySet<>();
        boolean shouldPinCamera = this.mConfiguredToPinCamera && DeviceConfig.getBoolean("runtime_native_boot", "pin_camera", SystemProperties.getBoolean("pinner.pin_camera", true));
        if (shouldPinCamera) {
            pinKeys.add(0);
        }
        if (this.mConfiguredToPinHome) {
            pinKeys.add(1);
        }
        if (this.mConfiguredToPinAssistant) {
            pinKeys.add(2);
        }
        return pinKeys;
    }

    private synchronized ArraySet<Integer> getPinKeys() {
        return this.mPinKeys;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void pinApps(int userHandle) {
        pinAppsInternal(userHandle, false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void pinAppsWithUpdatedKeys(int userHandle) {
        pinAppsInternal(userHandle, true);
    }

    private void pinAppsInternal(int userHandle, boolean updateKeys) {
        if (updateKeys) {
            ArraySet<Integer> newKeys = createPinKeys();
            synchronized (this) {
                if (!this.mPinnedApps.isEmpty()) {
                    Slog.e(TAG, "Attempted to update a list of apps, but apps were already pinned. Skipping.");
                    return;
                }
                this.mPinKeys = newKeys;
            }
        }
        ArraySet<Integer> currentPinKeys = getPinKeys();
        for (int i = currentPinKeys.size() - 1; i >= 0; i--) {
            int key = currentPinKeys.valueAt(i).intValue();
            pinApp(key, userHandle, true);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendPinAppMessage(int key, int userHandle, boolean force) {
        this.mPinnerHandler.sendMessage(PooledLambda.obtainMessage(new QuadConsumer() { // from class: com.android.server.PinnerService$$ExternalSyntheticLambda2
            public final void accept(Object obj, Object obj2, Object obj3, Object obj4) {
                ((PinnerService) obj).pinApp(((Integer) obj2).intValue(), ((Integer) obj3).intValue(), ((Boolean) obj4).booleanValue());
            }
        }, this, Integer.valueOf(key), Integer.valueOf(userHandle), Boolean.valueOf(force)));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void pinApp(int key, int userHandle, boolean force) {
        int uid = getUidForKey(key);
        if (!force && uid != -1) {
            synchronized (this) {
                this.mPendingRepin.put(Integer.valueOf(uid), Integer.valueOf(key));
            }
            return;
        }
        unpinApp(key);
        ApplicationInfo info = getInfoForKey(key, userHandle);
        if (info != null) {
            pinApp(key, info);
        }
    }

    private int getUidForKey(int key) {
        int i;
        synchronized (this) {
            PinnedApp existing = this.mPinnedApps.get(Integer.valueOf(key));
            if (existing != null && existing.active) {
                i = existing.uid;
            } else {
                i = -1;
            }
        }
        return i;
    }

    private ApplicationInfo getInfoForKey(int key, int userHandle) {
        switch (key) {
            case 0:
                return getCameraInfo(userHandle);
            case 1:
                return getHomeInfo(userHandle);
            case 2:
                return getAssistantInfo(userHandle);
            default:
                return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getNameForKey(int key) {
        switch (key) {
            case 0:
                return "Camera";
            case 1:
                return "Home";
            case 2:
                return "Assistant";
            default:
                return null;
        }
    }

    private int getSizeLimitForKey(int key) {
        switch (key) {
            case 0:
                return 83886080;
            case 1:
                return MAX_HOME_PIN_SIZE;
            case 2:
                return MAX_ASSISTANT_PIN_SIZE;
            default:
                return 0;
        }
    }

    private void pinApp(int key, ApplicationInfo appInfo) {
        String[] strArr;
        if (appInfo != null) {
            PinnedApp pinnedApp = new PinnedApp(appInfo);
            synchronized (this) {
                this.mPinnedApps.put(Integer.valueOf(key), pinnedApp);
            }
            int pinSizeLimit = getSizeLimitForKey(key);
            List<String> apks = new ArrayList<>();
            apks.add(appInfo.sourceDir);
            if (appInfo.splitSourceDirs != null) {
                for (String splitApk : appInfo.splitSourceDirs) {
                    apks.add(splitApk);
                }
            }
            int apkPinSizeLimit = pinSizeLimit;
            for (String apk : apks) {
                if (apkPinSizeLimit <= 0) {
                    Slog.w(TAG, "Reached to the pin size limit. Skipping: " + apk);
                } else {
                    PinnedFile pf = pinFile(apk, apkPinSizeLimit, true);
                    if (pf == null) {
                        Slog.e(TAG, "Failed to pin " + apk);
                    } else {
                        synchronized (this) {
                            pinnedApp.mFiles.add(pf);
                        }
                        apkPinSizeLimit -= pf.bytesPinned;
                    }
                }
            }
            String abi = appInfo.primaryCpuAbi != null ? appInfo.primaryCpuAbi : Build.SUPPORTED_ABIS[0];
            String arch = VMRuntime.getInstructionSet(abi);
            String baseCodePath = appInfo.getBaseCodePath();
            String[] files = null;
            try {
                files = DexFile.getDexFileOutputPaths(baseCodePath, arch);
            } catch (IOException e) {
            }
            if (files == null) {
                return;
            }
            for (String file : files) {
                PinnedFile pf2 = pinFile(file, pinSizeLimit, false);
                if (pf2 != null) {
                    synchronized (this) {
                        if (PROP_PIN_ODEX) {
                            pinnedApp.mFiles.add(pf2);
                        }
                    }
                }
            }
        }
    }

    private static PinnedFile pinFile(String fileToPin, int maxBytesToPin, boolean attemptPinIntrospection) {
        PinRangeSource pinRangeSource;
        ZipFile fileAsZip = null;
        InputStream pinRangeStream = null;
        if (attemptPinIntrospection) {
            try {
                fileAsZip = maybeOpenZip(fileToPin);
            } catch (Throwable th) {
                safeClose(pinRangeStream);
                safeClose(fileAsZip);
                throw th;
            }
        }
        if (fileAsZip != null) {
            pinRangeStream = maybeOpenPinMetaInZip(fileAsZip, fileToPin);
        }
        Slog.d(TAG, "pinRangeStream: " + pinRangeStream);
        if (pinRangeStream != null) {
            pinRangeSource = new PinRangeSourceStream(pinRangeStream);
        } else {
            pinRangeSource = new PinRangeSourceStatic(0, Integer.MAX_VALUE);
        }
        PinnedFile pinFileRanges = pinFileRanges(fileToPin, maxBytesToPin, pinRangeSource);
        safeClose(pinRangeStream);
        safeClose(fileAsZip);
        return pinFileRanges;
    }

    private static ZipFile maybeOpenZip(String fileName) {
        try {
            ZipFile zip = new ZipFile(fileName);
            return zip;
        } catch (IOException ex) {
            Slog.w(TAG, String.format("could not open \"%s\" as zip: pinning as blob", fileName), ex);
            return null;
        }
    }

    private static InputStream maybeOpenPinMetaInZip(ZipFile zipFile, String fileName) {
        ZipEntry pinMetaEntry;
        if (!PROP_PIN_PINLIST || (pinMetaEntry = zipFile.getEntry(PIN_META_FILENAME)) == null) {
            return null;
        }
        try {
            InputStream pinMetaStream = zipFile.getInputStream(pinMetaEntry);
            return pinMetaStream;
        } catch (IOException ex) {
            Slog.w(TAG, String.format("error reading pin metadata \"%s\": pinning as blob", fileName), ex);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static abstract class PinRangeSource {
        abstract boolean read(PinRange pinRange);

        private PinRangeSource() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class PinRangeSourceStatic extends PinRangeSource {
        private boolean mDone;
        private final int mPinLength;
        private final int mPinStart;

        PinRangeSourceStatic(int pinStart, int pinLength) {
            super();
            this.mDone = false;
            this.mPinStart = pinStart;
            this.mPinLength = pinLength;
        }

        @Override // com.android.server.PinnerService.PinRangeSource
        boolean read(PinRange outPinRange) {
            outPinRange.start = this.mPinStart;
            outPinRange.length = this.mPinLength;
            boolean done = this.mDone;
            this.mDone = true;
            return !done;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class PinRangeSourceStream extends PinRangeSource {
        private boolean mDone;
        private final DataInputStream mStream;

        PinRangeSourceStream(InputStream stream) {
            super();
            this.mDone = false;
            this.mStream = new DataInputStream(stream);
        }

        @Override // com.android.server.PinnerService.PinRangeSource
        boolean read(PinRange outPinRange) {
            if (!this.mDone) {
                try {
                    outPinRange.start = this.mStream.readInt();
                    outPinRange.length = this.mStream.readInt();
                } catch (IOException e) {
                    this.mDone = true;
                }
            }
            return !this.mDone;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1026=8, 1030=11] */
    /* JADX WARN: Removed duplicated region for block: B:66:0x0153  */
    /* JADX WARN: Removed duplicated region for block: B:71:0x0162  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private static PinnedFile pinFileRanges(String fileToPin, int maxBytesToPin, PinRangeSource pinRangeSource) {
        int bytesPinned;
        int maxBytesToPin2;
        FileDescriptor fd = new FileDescriptor();
        long address = -1;
        int mapSize = 0;
        try {
            int openFlags = OsConstants.O_RDONLY | OsConstants.O_CLOEXEC;
            FileDescriptor fd2 = Os.open(fileToPin, openFlags, 0);
            try {
                int mapSize2 = (int) Math.min(Os.fstat(fd2).st_size, 2147483647L);
                try {
                    long address2 = Os.mmap(0L, mapSize2, OsConstants.PROT_READ, OsConstants.MAP_SHARED, fd2, 0L);
                    try {
                        PinRange pinRange = new PinRange();
                        int i = PAGE_SIZE;
                        if (maxBytesToPin % i != 0) {
                            try {
                                bytesPinned = 0;
                                maxBytesToPin2 = maxBytesToPin - (maxBytesToPin % i);
                            } catch (ErrnoException e) {
                                ex = e;
                                mapSize = mapSize2;
                                address = address2;
                                fd = fd2;
                                try {
                                    Slog.e(TAG, "Could not pin file " + fileToPin, ex);
                                    safeClose(fd);
                                    if (address >= 0) {
                                        safeMunmap(address, mapSize);
                                    }
                                    return null;
                                } catch (Throwable th) {
                                    ex = th;
                                    safeClose(fd);
                                    if (address >= 0) {
                                        safeMunmap(address, mapSize);
                                    }
                                    throw ex;
                                }
                            } catch (Throwable th2) {
                                ex = th2;
                                mapSize = mapSize2;
                                address = address2;
                                fd = fd2;
                                safeClose(fd);
                                if (address >= 0) {
                                }
                                throw ex;
                            }
                        } else {
                            maxBytesToPin2 = maxBytesToPin;
                            bytesPinned = 0;
                        }
                        try {
                            while (bytesPinned < maxBytesToPin2) {
                                try {
                                    if (pinRangeSource.read(pinRange)) {
                                        int pinStart = pinRange.start;
                                        int pinLength = pinRange.length;
                                        int pinStart2 = clamp(0, pinStart, mapSize2);
                                        int pinLength2 = Math.min(maxBytesToPin2 - bytesPinned, clamp(0, pinLength, mapSize2 - pinStart2));
                                        int i2 = PAGE_SIZE;
                                        int pinLength3 = pinLength2 + (pinStart2 % i2);
                                        int pinStart3 = pinStart2 - (pinStart2 % i2);
                                        if (pinLength3 % i2 != 0) {
                                            pinLength3 += i2 - (pinLength3 % i2);
                                        }
                                        int pinLength4 = clamp(0, pinLength3, maxBytesToPin2 - bytesPinned);
                                        if (pinLength4 > 0) {
                                            Os.mlock(pinStart3 + address2, pinLength4);
                                        }
                                        bytesPinned += pinLength4;
                                    }
                                    break;
                                } catch (ErrnoException e2) {
                                    ex = e2;
                                    mapSize = mapSize2;
                                    address = address2;
                                    fd = fd2;
                                    Slog.e(TAG, "Could not pin file " + fileToPin, ex);
                                    safeClose(fd);
                                    if (address >= 0) {
                                    }
                                    return null;
                                } catch (Throwable th3) {
                                    ex = th3;
                                    mapSize = mapSize2;
                                    address = address2;
                                    fd = fd2;
                                    safeClose(fd);
                                    if (address >= 0) {
                                    }
                                    throw ex;
                                }
                            }
                            break;
                            try {
                                PinnedFile pinnedFile = new PinnedFile(address2, mapSize2, fileToPin, bytesPinned);
                                safeClose(fd2);
                                if (-1 >= 0) {
                                    safeMunmap(-1L, mapSize2);
                                }
                                return pinnedFile;
                            } catch (ErrnoException e3) {
                                ex = e3;
                                mapSize = mapSize2;
                                address = address2;
                                fd = fd2;
                                Slog.e(TAG, "Could not pin file " + fileToPin, ex);
                                safeClose(fd);
                                if (address >= 0) {
                                }
                                return null;
                            } catch (Throwable th4) {
                                ex = th4;
                                mapSize = mapSize2;
                                address = address2;
                                fd = fd2;
                                safeClose(fd);
                                if (address >= 0) {
                                }
                                throw ex;
                            }
                        } catch (ErrnoException e4) {
                            ex = e4;
                            mapSize = mapSize2;
                            address = address2;
                            fd = fd2;
                        } catch (Throwable th5) {
                            ex = th5;
                            mapSize = mapSize2;
                            address = address2;
                            fd = fd2;
                        }
                    } catch (ErrnoException e5) {
                        ex = e5;
                        mapSize = mapSize2;
                        address = address2;
                        fd = fd2;
                    } catch (Throwable th6) {
                        ex = th6;
                        mapSize = mapSize2;
                        address = address2;
                        fd = fd2;
                    }
                } catch (ErrnoException e6) {
                    ex = e6;
                    mapSize = mapSize2;
                    fd = fd2;
                } catch (Throwable th7) {
                    ex = th7;
                    mapSize = mapSize2;
                    fd = fd2;
                }
            } catch (ErrnoException e7) {
                ex = e7;
                fd = fd2;
            } catch (Throwable th8) {
                ex = th8;
                fd = fd2;
            }
        } catch (ErrnoException e8) {
            ex = e8;
        } catch (Throwable th9) {
            ex = th9;
        }
    }

    private static int clamp(int min, int value, int max) {
        return Math.max(min, Math.min(value, max));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void safeMunmap(long address, long mapSize) {
        try {
            Os.munmap(address, mapSize);
        } catch (ErrnoException ex) {
            Slog.w(TAG, "ignoring error in unmap", ex);
        }
    }

    private static void safeClose(FileDescriptor fd) {
        if (fd != null && fd.valid()) {
            try {
                Os.close(fd);
            } catch (ErrnoException ex) {
                if (ex.errno == OsConstants.EBADF) {
                    throw new AssertionError(ex);
                }
            }
        }
    }

    private static void safeClose(Closeable thing) {
        if (thing != null) {
            try {
                thing.close();
            } catch (IOException ex) {
                Slog.w(TAG, "ignoring error closing resource: " + thing, ex);
            }
        }
    }

    /* loaded from: classes.dex */
    private final class BinderService extends Binder {
        private BinderService() {
        }

        @Override // android.os.Binder
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(PinnerService.this.mContext, PinnerService.TAG, pw)) {
                synchronized (PinnerService.this) {
                    long totalSize = 0;
                    Iterator it = PinnerService.this.mPinnedFiles.iterator();
                    while (it.hasNext()) {
                        PinnedFile pinnedFile = (PinnedFile) it.next();
                        pw.format("%s %s\n", pinnedFile.fileName, Integer.valueOf(pinnedFile.bytesPinned));
                        totalSize += pinnedFile.bytesPinned;
                    }
                    pw.println();
                    for (Integer num : PinnerService.this.mPinnedApps.keySet()) {
                        int key = num.intValue();
                        PinnedApp app = (PinnedApp) PinnerService.this.mPinnedApps.get(Integer.valueOf(key));
                        pw.print(PinnerService.this.getNameForKey(key));
                        pw.print(" uid=");
                        pw.print(app.uid);
                        pw.print(" active=");
                        pw.print(app.active);
                        pw.println();
                        Iterator<PinnedFile> it2 = ((PinnedApp) PinnerService.this.mPinnedApps.get(Integer.valueOf(key))).mFiles.iterator();
                        while (it2.hasNext()) {
                            PinnedFile pf = it2.next();
                            pw.print("  ");
                            pw.format("%s %s\n", pf.fileName, Integer.valueOf(pf.bytesPinned));
                            totalSize += pf.bytesPinned;
                        }
                    }
                    pw.format("Total size: %s\n", Long.valueOf(totalSize));
                    pw.println();
                    if (!PinnerService.this.mPendingRepin.isEmpty()) {
                        pw.print("Pending repin: ");
                        for (Integer num2 : PinnerService.this.mPendingRepin.values()) {
                            pw.print(PinnerService.this.getNameForKey(num2.intValue()));
                            pw.print(' ');
                        }
                        pw.println();
                    }
                }
            }
        }

        private void repin() {
            PinnerService.this.sendUnpinAppsMessage();
            PinnerService.this.sendPinAppsWithUpdatedKeysMessage(0);
        }

        private void printError(FileDescriptor out, String message) {
            PrintWriter writer = new PrintWriter(new FileOutputStream(out));
            writer.println(message);
            writer.flush();
        }

        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            boolean z;
            if (args.length < 1) {
                printError(out, "Command is not given.");
                resultReceiver.send(-1, null);
                return;
            }
            String command = args[0];
            switch (command.hashCode()) {
                case 108401282:
                    if (command.equals("repin")) {
                        z = false;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    repin();
                    resultReceiver.send(0, null);
                    return;
                default:
                    printError(out, String.format("Unknown pinner command: %s. Supported commands: repin", command));
                    resultReceiver.send(-1, null);
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class PinnedFile implements AutoCloseable {
        final int bytesPinned;
        final String fileName;
        private long mAddress;
        final int mapSize;

        PinnedFile(long address, int mapSize, String fileName, int bytesPinned) {
            this.mAddress = address;
            this.mapSize = mapSize;
            this.fileName = fileName;
            this.bytesPinned = bytesPinned;
        }

        @Override // java.lang.AutoCloseable
        public void close() {
            long j = this.mAddress;
            if (j >= 0) {
                PinnerService.safeMunmap(j, this.mapSize);
                this.mAddress = -1L;
            }
        }

        public void finalize() {
            close();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class PinRange {
        int length;
        int start;

        PinRange() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class PinnedApp {
        boolean active;
        final ArrayList<PinnedFile> mFiles;
        final int uid;

        private PinnedApp(ApplicationInfo appInfo) {
            this.mFiles = new ArrayList<>();
            int i = appInfo.uid;
            this.uid = i;
            this.active = PinnerService.this.mAmInternal.isUidActive(i);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class PinnerHandler extends Handler {
        static final int PIN_ONSTART_MSG = 4001;

        public PinnerHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PIN_ONSTART_MSG /* 4001 */:
                    PinnerService.this.handlePinOnStart();
                    return;
                default:
                    super.handleMessage(msg);
                    return;
            }
        }
    }
}
