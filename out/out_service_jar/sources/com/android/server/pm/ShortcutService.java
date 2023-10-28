package com.android.server.pm;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AppGlobals;
import android.app.IUidObserver;
import android.app.IUriGrantsManager;
import android.app.UriGrantsManager;
import android.app.role.OnRoleHoldersChangedListener;
import android.app.role.RoleManager;
import android.app.usage.UsageStatsManagerInternal;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.LocusId;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.IPackageManager;
import android.content.pm.IShortcutService;
import android.content.pm.LauncherApps;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.pm.ShortcutServiceInternal;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.LocaleList;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.TimeMigrationUtils;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.EventLog;
import android.util.KeyValueListParser;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;
import android.util.TypedValue;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import android.view.IWindowManager;
import com.android.internal.infra.AndroidFuture;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.Preconditions;
import com.android.internal.util.StatLogger;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.pm.ShortcutService;
import com.android.server.pm.ShortcutUser;
import com.android.server.pm.verify.domain.DomainVerificationLegacySettings;
import com.android.server.uri.UriGrantsManagerInternal;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import libcore.io.IoUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class ShortcutService extends IShortcutService.Stub {
    private static final String ATTR_VALUE = "value";
    private static final long CALLBACK_DELAY = 100;
    static final boolean DEBUG = false;
    static final boolean DEBUG_LOAD = false;
    static final boolean DEBUG_PROCSTATE = false;
    static final boolean DEBUG_REBOOT = false;
    static final int DEFAULT_ICON_PERSIST_QUALITY = 100;
    static final int DEFAULT_MAX_ICON_DIMENSION_DP = 96;
    static final int DEFAULT_MAX_ICON_DIMENSION_LOWRAM_DP = 48;
    static final int DEFAULT_MAX_SHORTCUTS_PER_ACTIVITY = 15;
    static final int DEFAULT_MAX_SHORTCUTS_PER_APP = 100;
    static final int DEFAULT_MAX_UPDATES_PER_INTERVAL = 10;
    static final long DEFAULT_RESET_INTERVAL_SEC = 86400;
    static final int DEFAULT_SAVE_DELAY_MS = 3000;
    static final String DIRECTORY_BITMAPS = "bitmaps";
    static final String DIRECTORY_DUMP = "shortcut_dump";
    static final String DIRECTORY_PER_USER = "shortcut_service";
    private static final String DUMMY_MAIN_ACTIVITY = "android.__dummy__";
    static final String FILENAME_BASE_STATE = "shortcut_service.xml";
    static final String FILENAME_USER_PACKAGES = "shortcuts.xml";
    private static final String KEY_ICON_SIZE = "iconSize";
    private static final String KEY_LOW_RAM = "lowRam";
    private static final String KEY_SHORTCUT = "shortcut";
    private static final String LAUNCHER_INTENT_CATEGORY = "android.intent.category.LAUNCHER";
    static final int OPERATION_ADD = 1;
    static final int OPERATION_SET = 0;
    static final int OPERATION_UPDATE = 2;
    private static final int PACKAGE_MATCH_FLAGS = 795136;
    private static final int PROCESS_STATE_FOREGROUND_THRESHOLD = 5;
    private static final int SYSTEM_APP_MASK = 129;
    static final String TAG = "ShortcutService";
    private static final String TAG_LAST_RESET_TIME = "last_reset_time";
    private static final String TAG_ROOT = "root";
    private final ActivityManagerInternal mActivityManagerInternal;
    private final AtomicBoolean mBootCompleted;
    private ComponentName mChooserActivity;
    final Context mContext;
    private List<Integer> mDirtyUserIds;
    private final Handler mHandler;
    private final IPackageManager mIPackageManager;
    private Bitmap.CompressFormat mIconPersistFormat;
    private int mIconPersistQuality;
    private final boolean mIsAppSearchEnabled;
    private int mLastLockedUser;
    private Exception mLastWtfStacktrace;
    private final ArrayList<ShortcutServiceInternal.ShortcutChangeListener> mListeners;
    private final Object mLock;
    private int mMaxIconDimension;
    private int mMaxShortcuts;
    private int mMaxShortcutsPerApp;
    int mMaxUpdatesPerInterval;
    private final MetricsLogger mMetricsLogger;
    private final Object mNonPersistentUsersLock;
    private final OnRoleHoldersChangedListener mOnRoleHoldersChangedListener;
    private final PackageManagerInternal mPackageManagerInternal;
    final BroadcastReceiver mPackageMonitor;
    private long mRawLastResetTime;
    final BroadcastReceiver mReceiver;
    private long mResetInterval;
    private final RoleManager mRoleManager;
    private int mSaveDelayMillis;
    private final Runnable mSaveDirtyInfoRunner;
    private final ArrayList<LauncherApps.ShortcutChangeCallback> mShortcutChangeCallbacks;
    private final ShortcutDumpFiles mShortcutDumpFiles;
    private final SparseArray<ShortcutNonPersistentUser> mShortcutNonPersistentUsers;
    private final ShortcutRequestPinProcessor mShortcutRequestPinProcessor;
    private final AtomicBoolean mShutdown;
    private final BroadcastReceiver mShutdownReceiver;
    private final StatLogger mStatLogger;
    final SparseLongArray mUidLastForegroundElapsedTime;
    private final IUidObserver mUidObserver;
    final SparseIntArray mUidState;
    final SparseBooleanArray mUnlockedUsers;
    private final IUriGrantsManager mUriGrantsManager;
    private final UriGrantsManagerInternal mUriGrantsManagerInternal;
    private final IBinder mUriPermissionOwner;
    private final UsageStatsManagerInternal mUsageStatsManagerInternal;
    final UserManagerInternal mUserManagerInternal;
    private final SparseArray<ShortcutUser> mUsers;
    private int mWtfCount;
    static final String DEFAULT_ICON_PERSIST_FORMAT = Bitmap.CompressFormat.PNG.name();
    private static List<ResolveInfo> EMPTY_RESOLVE_INFO = new ArrayList(0);
    private static Predicate<ResolveInfo> ACTIVITY_NOT_EXPORTED = new Predicate<ResolveInfo>() { // from class: com.android.server.pm.ShortcutService.1
        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Predicate
        public boolean test(ResolveInfo ri) {
            return !ri.activityInfo.exported;
        }
    };
    private static Predicate<ResolveInfo> ACTIVITY_NOT_INSTALLED = new Predicate() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda22
        @Override // java.util.function.Predicate
        public final boolean test(Object obj) {
            return ShortcutService.lambda$static$0((ResolveInfo) obj);
        }
    };
    private static Predicate<PackageInfo> PACKAGE_NOT_INSTALLED = new Predicate<PackageInfo>() { // from class: com.android.server.pm.ShortcutService.2
        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Predicate
        public boolean test(PackageInfo pi) {
            return !ShortcutService.isInstalled(pi);
        }
    };

    /* loaded from: classes2.dex */
    interface ConfigConstants {
        public static final String KEY_ICON_FORMAT = "icon_format";
        public static final String KEY_ICON_QUALITY = "icon_quality";
        public static final String KEY_MAX_ICON_DIMENSION_DP = "max_icon_dimension_dp";
        public static final String KEY_MAX_ICON_DIMENSION_DP_LOWRAM = "max_icon_dimension_dp_lowram";
        public static final String KEY_MAX_SHORTCUTS = "max_shortcuts";
        public static final String KEY_MAX_SHORTCUTS_PER_APP = "max_shortcuts_per_app";
        public static final String KEY_MAX_UPDATES_PER_INTERVAL = "max_updates_per_interval";
        public static final String KEY_RESET_INTERVAL_SEC = "reset_interval_sec";
        public static final String KEY_SAVE_DELAY_MILLIS = "save_delay_ms";
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface ShortcutOperation {
    }

    /* loaded from: classes2.dex */
    interface Stats {
        public static final int ASYNC_PRELOAD_USER_DELAY = 15;
        public static final int CHECK_LAUNCHER_ACTIVITY = 12;
        public static final int CHECK_PACKAGE_CHANGES = 8;
        public static final int CLEANUP_DANGLING_BITMAPS = 5;
        public static final int COUNT = 17;
        public static final int GET_ACTIVITY_WITH_METADATA = 6;
        public static final int GET_APPLICATION_INFO = 3;
        public static final int GET_APPLICATION_RESOURCES = 9;
        public static final int GET_DEFAULT_HOME = 0;
        public static final int GET_DEFAULT_LAUNCHER = 16;
        public static final int GET_INSTALLED_PACKAGES = 7;
        public static final int GET_LAUNCHER_ACTIVITY = 11;
        public static final int GET_PACKAGE_INFO = 1;
        public static final int GET_PACKAGE_INFO_WITH_SIG = 2;
        public static final int IS_ACTIVITY_ENABLED = 13;
        public static final int LAUNCHER_PERMISSION_CHECK = 4;
        public static final int PACKAGE_UPDATE_CHECK = 14;
        public static final int RESOURCE_NAME_LOOKUP = 10;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$static$0(ResolveInfo ri) {
        return !isInstalled(ri.activityInfo);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class InvalidFileFormatException extends Exception {
        public InvalidFileFormatException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public ShortcutService(Context context) {
        this(context, BackgroundThread.get().getLooper(), false);
    }

    ShortcutService(Context context, Looper looper, boolean onlyForPackageManagerApis) {
        Object obj = new Object();
        this.mLock = obj;
        this.mNonPersistentUsersLock = new Object();
        boolean z = true;
        this.mListeners = new ArrayList<>(1);
        this.mShortcutChangeCallbacks = new ArrayList<>(1);
        this.mUsers = new SparseArray<>();
        this.mShortcutNonPersistentUsers = new SparseArray<>();
        this.mUidState = new SparseIntArray();
        this.mUidLastForegroundElapsedTime = new SparseLongArray();
        this.mDirtyUserIds = new ArrayList();
        this.mBootCompleted = new AtomicBoolean();
        this.mShutdown = new AtomicBoolean();
        this.mUnlockedUsers = new SparseBooleanArray();
        this.mStatLogger = new StatLogger(new String[]{"getHomeActivities()", "Launcher permission check", "getPackageInfo()", "getPackageInfo(SIG)", "getApplicationInfo", "cleanupDanglingBitmaps", "getActivity+metadata", "getInstalledPackages", "checkPackageChanges", "getApplicationResources", "resourceNameLookup", "getLauncherActivity", "checkLauncherActivity", "isActivityEnabled", "packageUpdateCheck", "asyncPreloadUserDelay", "getDefaultLauncher()"});
        this.mWtfCount = 0;
        this.mMetricsLogger = new MetricsLogger();
        AnonymousClass3 anonymousClass3 = new AnonymousClass3();
        this.mOnRoleHoldersChangedListener = anonymousClass3;
        AnonymousClass4 anonymousClass4 = new AnonymousClass4();
        this.mUidObserver = anonymousClass4;
        this.mSaveDirtyInfoRunner = new Runnable() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda15
            @Override // java.lang.Runnable
            public final void run() {
                ShortcutService.this.saveDirtyInfo();
            }
        };
        this.mLastLockedUser = -1;
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.pm.ShortcutService.5
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if (!ShortcutService.this.mBootCompleted.get()) {
                    return;
                }
                try {
                    if ("android.intent.action.LOCALE_CHANGED".equals(intent.getAction())) {
                        ShortcutService.this.handleLocaleChanged();
                    }
                } catch (Exception e) {
                    ShortcutService.this.wtf("Exception in mReceiver.onReceive", e);
                }
            }
        };
        this.mReceiver = broadcastReceiver;
        BroadcastReceiver broadcastReceiver2 = new BroadcastReceiver() { // from class: com.android.server.pm.ShortcutService.6
            /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3765=5] */
            /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                if (userId == -10000) {
                    Slog.w(ShortcutService.TAG, "Intent broadcast does not contain user handle: " + intent);
                    return;
                }
                String action = intent.getAction();
                long token = ShortcutService.this.injectClearCallingIdentity();
                try {
                    try {
                    } catch (Exception e) {
                        ShortcutService.this.wtf("Exception in mPackageMonitor.onReceive", e);
                    }
                    synchronized (ShortcutService.this.mLock) {
                        if (ShortcutService.this.isUserUnlockedL(userId)) {
                            Uri intentUri = intent.getData();
                            String packageName = intentUri != null ? intentUri.getSchemeSpecificPart() : null;
                            if (packageName == null) {
                                Slog.w(ShortcutService.TAG, "Intent broadcast does not contain package name: " + intent);
                                return;
                            }
                            char c = 0;
                            boolean replacing = intent.getBooleanExtra("android.intent.extra.REPLACING", false);
                            switch (action.hashCode()) {
                                case 172491798:
                                    if (action.equals("android.intent.action.PACKAGE_CHANGED")) {
                                        c = 2;
                                        break;
                                    }
                                    c = 65535;
                                    break;
                                case 267468725:
                                    if (action.equals("android.intent.action.PACKAGE_DATA_CLEARED")) {
                                        c = 3;
                                        break;
                                    }
                                    c = 65535;
                                    break;
                                case 525384130:
                                    if (action.equals("android.intent.action.PACKAGE_REMOVED")) {
                                        c = 1;
                                        break;
                                    }
                                    c = 65535;
                                    break;
                                case 1544582882:
                                    if (action.equals("android.intent.action.PACKAGE_ADDED")) {
                                        break;
                                    }
                                    c = 65535;
                                    break;
                                default:
                                    c = 65535;
                                    break;
                            }
                            switch (c) {
                                case 0:
                                    if (!replacing) {
                                        ShortcutService.this.handlePackageAdded(packageName, userId);
                                        break;
                                    } else {
                                        ShortcutService.this.handlePackageUpdateFinished(packageName, userId);
                                        break;
                                    }
                                case 1:
                                    if (!replacing) {
                                        ShortcutService.this.handlePackageRemoved(packageName, userId);
                                        break;
                                    }
                                    break;
                                case 2:
                                    ShortcutService.this.handlePackageChanged(packageName, userId);
                                    break;
                                case 3:
                                    ShortcutService.this.handlePackageDataCleared(packageName, userId);
                                    break;
                            }
                        }
                    }
                } finally {
                    ShortcutService.this.injectRestoreCallingIdentity(token);
                }
            }
        };
        this.mPackageMonitor = broadcastReceiver2;
        BroadcastReceiver broadcastReceiver3 = new BroadcastReceiver() { // from class: com.android.server.pm.ShortcutService.7
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                synchronized (ShortcutService.this.mLock) {
                    if (ShortcutService.this.mHandler.hasCallbacks(ShortcutService.this.mSaveDirtyInfoRunner)) {
                        ShortcutService.this.mHandler.removeCallbacks(ShortcutService.this.mSaveDirtyInfoRunner);
                        ShortcutService.this.forEachLoadedUserLocked(new Consumer() { // from class: com.android.server.pm.ShortcutService$7$$ExternalSyntheticLambda0
                            @Override // java.util.function.Consumer
                            public final void accept(Object obj2) {
                                ((ShortcutUser) obj2).cancelAllInFlightTasks();
                            }
                        });
                        ShortcutService.this.saveDirtyInfo();
                    }
                    ShortcutService.this.mShutdown.set(true);
                }
            }
        };
        this.mShutdownReceiver = broadcastReceiver3;
        Context context2 = (Context) Objects.requireNonNull(context);
        this.mContext = context2;
        LocalServices.addService(ShortcutServiceInternal.class, new LocalService());
        Handler handler = new Handler(looper);
        this.mHandler = handler;
        this.mIPackageManager = AppGlobals.getPackageManager();
        this.mPackageManagerInternal = (PackageManagerInternal) Objects.requireNonNull((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class));
        this.mUserManagerInternal = (UserManagerInternal) Objects.requireNonNull((UserManagerInternal) LocalServices.getService(UserManagerInternal.class));
        this.mUsageStatsManagerInternal = (UsageStatsManagerInternal) Objects.requireNonNull((UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class));
        this.mActivityManagerInternal = (ActivityManagerInternal) Objects.requireNonNull((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class));
        this.mUriGrantsManager = (IUriGrantsManager) Objects.requireNonNull(UriGrantsManager.getService());
        UriGrantsManagerInternal uriGrantsManagerInternal = (UriGrantsManagerInternal) Objects.requireNonNull((UriGrantsManagerInternal) LocalServices.getService(UriGrantsManagerInternal.class));
        this.mUriGrantsManagerInternal = uriGrantsManagerInternal;
        this.mUriPermissionOwner = uriGrantsManagerInternal.newUriPermissionOwner(TAG);
        this.mRoleManager = (RoleManager) Objects.requireNonNull((RoleManager) context2.getSystemService(RoleManager.class));
        this.mShortcutRequestPinProcessor = new ShortcutRequestPinProcessor(this, obj);
        this.mShortcutDumpFiles = new ShortcutDumpFiles(this);
        this.mIsAppSearchEnabled = (!DeviceConfig.getBoolean("systemui", "shortcut_appsearch_integration", true) || injectIsLowRamDevice()) ? false : z;
        if (onlyForPackageManagerApis) {
            return;
        }
        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction("android.intent.action.PACKAGE_ADDED");
        packageFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        packageFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        packageFilter.addAction("android.intent.action.PACKAGE_DATA_CLEARED");
        packageFilter.addDataScheme("package");
        packageFilter.setPriority(1000);
        context2.registerReceiverAsUser(broadcastReceiver2, UserHandle.ALL, packageFilter, null, handler);
        IntentFilter localeFilter = new IntentFilter();
        localeFilter.addAction("android.intent.action.LOCALE_CHANGED");
        localeFilter.setPriority(1000);
        context2.registerReceiverAsUser(broadcastReceiver, UserHandle.ALL, localeFilter, null, handler);
        IntentFilter shutdownFilter = new IntentFilter();
        shutdownFilter.addAction("android.intent.action.ACTION_SHUTDOWN");
        shutdownFilter.setPriority(1000);
        context2.registerReceiverAsUser(broadcastReceiver3, UserHandle.SYSTEM, shutdownFilter, null, handler);
        injectRegisterUidObserver(anonymousClass4, 3);
        injectRegisterRoleHoldersListener(anonymousClass3);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAppSearchEnabled() {
        return this.mIsAppSearchEnabled;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getStatStartTime() {
        return this.mStatLogger.getTime();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void logDurationStat(int statId, long start) {
        this.mStatLogger.logDurationStat(statId, start);
    }

    public String injectGetLocaleTagsForUser(int userId) {
        return LocaleList.getDefault().toLanguageTags();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.pm.ShortcutService$3  reason: invalid class name */
    /* loaded from: classes2.dex */
    public class AnonymousClass3 implements OnRoleHoldersChangedListener {
        AnonymousClass3() {
        }

        public void onRoleHoldersChanged(String roleName, final UserHandle user) {
            if ("android.app.role.HOME".equals(roleName)) {
                ShortcutService.this.injectPostToHandler(new Runnable() { // from class: com.android.server.pm.ShortcutService$3$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        ShortcutService.AnonymousClass3.this.m5677x30cd88b5(user);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onRoleHoldersChanged$0$com-android-server-pm-ShortcutService$3  reason: not valid java name */
        public /* synthetic */ void m5677x30cd88b5(UserHandle user) {
            ShortcutService.this.handleOnDefaultLauncherChanged(user.getIdentifier());
        }
    }

    void handleOnDefaultLauncherChanged(int userId) {
        this.mUriGrantsManagerInternal.revokeUriPermissionFromOwner(this.mUriPermissionOwner, null, -1, 0);
        synchronized (this.mLock) {
            if (isUserLoadedLocked(userId)) {
                getUserShortcutsLocked(userId).setCachedLauncher(null);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.pm.ShortcutService$4  reason: invalid class name */
    /* loaded from: classes2.dex */
    public class AnonymousClass4 extends IUidObserver.Stub {
        AnonymousClass4() {
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onUidStateChanged$0$com-android-server-pm-ShortcutService$4  reason: not valid java name */
        public /* synthetic */ void m5679xc74e690e(int uid, int procState) {
            ShortcutService.this.handleOnUidStateChanged(uid, procState);
        }

        public void onUidStateChanged(final int uid, final int procState, long procStateSeq, int capability) {
            ShortcutService.this.injectPostToHandler(new Runnable() { // from class: com.android.server.pm.ShortcutService$4$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ShortcutService.AnonymousClass4.this.m5679xc74e690e(uid, procState);
                }
            });
        }

        public void onUidGone(final int uid, boolean disabled) {
            ShortcutService.this.injectPostToHandler(new Runnable() { // from class: com.android.server.pm.ShortcutService$4$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    ShortcutService.AnonymousClass4.this.m5678lambda$onUidGone$1$comandroidserverpmShortcutService$4(uid);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onUidGone$1$com-android-server-pm-ShortcutService$4  reason: not valid java name */
        public /* synthetic */ void m5678lambda$onUidGone$1$comandroidserverpmShortcutService$4(int uid) {
            ShortcutService.this.handleOnUidStateChanged(uid, 20);
        }

        public void onUidActive(int uid) {
        }

        public void onUidIdle(int uid, boolean disabled) {
        }

        public void onUidCachedChanged(int uid, boolean cached) {
        }

        public void onUidProcAdjChanged(int uid) {
        }
    }

    void handleOnUidStateChanged(int uid, int procState) {
        Trace.traceBegin(524288L, "shortcutHandleOnUidStateChanged");
        synchronized (this.mLock) {
            this.mUidState.put(uid, procState);
            if (isProcessStateForeground(procState)) {
                this.mUidLastForegroundElapsedTime.put(uid, injectElapsedRealtime());
            }
        }
        Trace.traceEnd(524288L);
    }

    private boolean isProcessStateForeground(int processState) {
        return processState <= 5;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isUidForegroundLocked(int uid) {
        if (uid == 1000 || isProcessStateForeground(this.mUidState.get(uid, 20))) {
            return true;
        }
        return isProcessStateForeground(this.mActivityManagerInternal.getUidProcessState(uid));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getUidLastForegroundElapsedTimeLocked(int uid) {
        return this.mUidLastForegroundElapsedTime.get(uid);
    }

    /* loaded from: classes2.dex */
    public static final class Lifecycle extends SystemService {
        final ShortcutService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new ShortcutService(context);
        }

        @Override // com.android.server.SystemService
        public void onStart() {
            publishBinderService(ShortcutService.KEY_SHORTCUT, this.mService);
        }

        @Override // com.android.server.SystemService
        public void onBootPhase(int phase) {
            this.mService.onBootPhase(phase);
        }

        @Override // com.android.server.SystemService
        public void onUserStopping(SystemService.TargetUser user) {
            this.mService.handleStopUser(user.getUserIdentifier());
        }

        @Override // com.android.server.SystemService
        public void onUserUnlocking(SystemService.TargetUser user) {
            this.mService.handleUnlockUser(user.getUserIdentifier());
        }
    }

    void onBootPhase(int phase) {
        switch (phase) {
            case SystemService.PHASE_LOCK_SETTINGS_READY /* 480 */:
                initialize();
                return;
            case 1000:
                this.mBootCompleted.set(true);
                return;
            default:
                return;
        }
    }

    void handleUnlockUser(final int userId) {
        synchronized (this.mUnlockedUsers) {
            this.mUnlockedUsers.put(userId, true);
        }
        final long start = getStatStartTime();
        injectRunOnNewThread(new Runnable() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                ShortcutService.this.m5671lambda$handleUnlockUser$1$comandroidserverpmShortcutService(start, userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleUnlockUser$1$com-android-server-pm-ShortcutService  reason: not valid java name */
    public /* synthetic */ void m5671lambda$handleUnlockUser$1$comandroidserverpmShortcutService(long start, int userId) {
        Trace.traceBegin(524288L, "shortcutHandleUnlockUser");
        synchronized (this.mLock) {
            logDurationStat(15, start);
            getUserShortcutsLocked(userId);
        }
        Trace.traceEnd(524288L);
    }

    void handleStopUser(int userId) {
        Trace.traceBegin(524288L, "shortcutHandleStopUser");
        synchronized (this.mLock) {
            unloadUserLocked(userId);
            synchronized (this.mUnlockedUsers) {
                this.mUnlockedUsers.put(userId, false);
            }
        }
        Trace.traceEnd(524288L);
    }

    private void unloadUserLocked(int userId) {
        getUserShortcutsLocked(userId).cancelAllInFlightTasks();
        saveDirtyInfo();
        this.mUsers.delete(userId);
    }

    private AtomicFile getBaseStateFile() {
        File path = new File(injectSystemDataPath(), FILENAME_BASE_STATE);
        path.mkdirs();
        return new AtomicFile(path);
    }

    private void initialize() {
        synchronized (this.mLock) {
            loadConfigurationLocked();
            loadBaseStateLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loadConfigurationLocked() {
        updateConfigurationLocked(injectShortcutManagerConstants());
    }

    boolean updateConfigurationLocked(String config) {
        int i;
        boolean result = true;
        KeyValueListParser parser = new KeyValueListParser(',');
        try {
            parser.setString(config);
        } catch (IllegalArgumentException e) {
            Slog.e(TAG, "Bad shortcut manager settings", e);
            result = false;
        }
        this.mSaveDelayMillis = Math.max(0, (int) parser.getLong(ConfigConstants.KEY_SAVE_DELAY_MILLIS, (long) BackupAgentTimeoutParameters.DEFAULT_QUOTA_EXCEEDED_TIMEOUT_MILLIS));
        this.mResetInterval = Math.max(1L, parser.getLong(ConfigConstants.KEY_RESET_INTERVAL_SEC, (long) DEFAULT_RESET_INTERVAL_SEC) * 1000);
        this.mMaxUpdatesPerInterval = Math.max(0, (int) parser.getLong(ConfigConstants.KEY_MAX_UPDATES_PER_INTERVAL, 10L));
        this.mMaxShortcuts = Math.max(0, (int) parser.getLong(ConfigConstants.KEY_MAX_SHORTCUTS, 15L));
        this.mMaxShortcutsPerApp = Math.max(0, (int) parser.getLong(ConfigConstants.KEY_MAX_SHORTCUTS_PER_APP, (long) CALLBACK_DELAY));
        if (injectIsLowRamDevice()) {
            i = (int) parser.getLong(ConfigConstants.KEY_MAX_ICON_DIMENSION_DP_LOWRAM, 48L);
        } else {
            i = (int) parser.getLong(ConfigConstants.KEY_MAX_ICON_DIMENSION_DP, 96L);
        }
        int iconDimensionDp = Math.max(1, i);
        this.mMaxIconDimension = injectDipToPixel(iconDimensionDp);
        this.mIconPersistFormat = Bitmap.CompressFormat.valueOf(parser.getString(ConfigConstants.KEY_ICON_FORMAT, DEFAULT_ICON_PERSIST_FORMAT));
        this.mIconPersistQuality = (int) parser.getLong(ConfigConstants.KEY_ICON_QUALITY, (long) CALLBACK_DELAY);
        return result;
    }

    String injectShortcutManagerConstants() {
        return Settings.Global.getString(this.mContext.getContentResolver(), "shortcut_manager_constants");
    }

    int injectDipToPixel(int dip) {
        return (int) TypedValue.applyDimension(1, dip, this.mContext.getResources().getDisplayMetrics());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String parseStringAttribute(TypedXmlPullParser parser, String attribute) {
        return parser.getAttributeValue((String) null, attribute);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean parseBooleanAttribute(TypedXmlPullParser parser, String attribute) {
        return parseLongAttribute(parser, attribute) == 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean parseBooleanAttribute(TypedXmlPullParser parser, String attribute, boolean def) {
        return parseLongAttribute(parser, attribute, def ? 1L : 0L) == 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int parseIntAttribute(TypedXmlPullParser parser, String attribute) {
        return (int) parseLongAttribute(parser, attribute);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int parseIntAttribute(TypedXmlPullParser parser, String attribute, int def) {
        return (int) parseLongAttribute(parser, attribute, def);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static long parseLongAttribute(TypedXmlPullParser parser, String attribute) {
        return parseLongAttribute(parser, attribute, 0L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static long parseLongAttribute(TypedXmlPullParser parser, String attribute, long def) {
        String value = parseStringAttribute(parser, attribute);
        if (TextUtils.isEmpty(value)) {
            return def;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            Slog.e(TAG, "Error parsing long " + value);
            return def;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ComponentName parseComponentNameAttribute(TypedXmlPullParser parser, String attribute) {
        String value = parseStringAttribute(parser, attribute);
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        return ComponentName.unflattenFromString(value);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Intent parseIntentAttributeNoDefault(TypedXmlPullParser parser, String attribute) {
        String value = parseStringAttribute(parser, attribute);
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        try {
            Intent parsed = Intent.parseUri(value, 0);
            return parsed;
        } catch (URISyntaxException e) {
            Slog.e(TAG, "Error parsing intent", e);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Intent parseIntentAttribute(TypedXmlPullParser parser, String attribute) {
        Intent parsed = parseIntentAttributeNoDefault(parser, attribute);
        if (parsed == null) {
            return new Intent("android.intent.action.VIEW");
        }
        return parsed;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void writeTagValue(TypedXmlSerializer out, String tag, String value) throws IOException {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        out.startTag((String) null, tag);
        out.attribute((String) null, ATTR_VALUE, value);
        out.endTag((String) null, tag);
    }

    static void writeTagValue(TypedXmlSerializer out, String tag, long value) throws IOException {
        writeTagValue(out, tag, Long.toString(value));
    }

    static void writeTagValue(TypedXmlSerializer out, String tag, ComponentName name) throws IOException {
        if (name == null) {
            return;
        }
        writeTagValue(out, tag, name.flattenToString());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void writeTagExtra(TypedXmlSerializer out, String tag, PersistableBundle bundle) throws IOException, XmlPullParserException {
        if (bundle == null) {
            return;
        }
        out.startTag((String) null, tag);
        bundle.saveToXml(out);
        out.endTag((String) null, tag);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void writeAttr(TypedXmlSerializer out, String name, CharSequence value) throws IOException {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        out.attribute((String) null, name, value.toString());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void writeAttr(TypedXmlSerializer out, String name, long value) throws IOException {
        writeAttr(out, name, String.valueOf(value));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void writeAttr(TypedXmlSerializer out, String name, boolean value) throws IOException {
        if (value) {
            writeAttr(out, name, "1");
        } else {
            writeAttr(out, name, "0");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void writeAttr(TypedXmlSerializer out, String name, ComponentName comp) throws IOException {
        if (comp == null) {
            return;
        }
        writeAttr(out, name, comp.flattenToString());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void writeAttr(TypedXmlSerializer out, String name, Intent intent) throws IOException {
        if (intent == null) {
            return;
        }
        writeAttr(out, name, intent.toUri(0));
    }

    void saveBaseStateLocked() {
        AtomicFile file = getBaseStateFile();
        FileOutputStream outs = null;
        try {
            outs = file.startWrite();
            TypedXmlSerializer out = Xml.resolveSerializer(outs);
            out.startDocument((String) null, true);
            out.startTag((String) null, TAG_ROOT);
            writeTagValue(out, TAG_LAST_RESET_TIME, this.mRawLastResetTime);
            out.endTag((String) null, TAG_ROOT);
            out.endDocument();
            file.finishWrite(outs);
        } catch (IOException e) {
            Slog.e(TAG, "Failed to write to file " + file.getBaseFile(), e);
            file.failWrite(outs);
        }
    }

    private void loadBaseStateLocked() {
        this.mRawLastResetTime = 0L;
        AtomicFile file = getBaseStateFile();
        try {
            try {
                FileInputStream in = file.openRead();
                try {
                    TypedXmlPullParser parser = Xml.resolvePullParser(in);
                    while (true) {
                        int type = parser.next();
                        if (type != 1) {
                            if (type == 2) {
                                int depth = parser.getDepth();
                                String tag = parser.getName();
                                if (depth == 1) {
                                    if (!TAG_ROOT.equals(tag)) {
                                        Slog.e(TAG, "Invalid root tag: " + tag);
                                        if (in != null) {
                                            in.close();
                                            return;
                                        }
                                        return;
                                    }
                                } else {
                                    char c = 65535;
                                    switch (tag.hashCode()) {
                                        case -68726522:
                                            if (tag.equals(TAG_LAST_RESET_TIME)) {
                                                c = 0;
                                                break;
                                            }
                                    }
                                    switch (c) {
                                        case 0:
                                            this.mRawLastResetTime = parseLongAttribute(parser, ATTR_VALUE);
                                            continue;
                                        default:
                                            Slog.e(TAG, "Invalid tag: " + tag);
                                            continue;
                                    }
                                }
                            }
                        } else if (in != null) {
                            in.close();
                        }
                    }
                } catch (Throwable th) {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                    }
                    throw th;
                }
            } catch (FileNotFoundException e) {
            }
        } catch (IOException | XmlPullParserException e2) {
            Slog.e(TAG, "Failed to read file " + file.getBaseFile(), e2);
            this.mRawLastResetTime = 0L;
        }
        getLastResetTimeLocked();
    }

    final File getUserFile(int userId) {
        return new File(injectUserDataPath(userId), FILENAME_USER_PACKAGES);
    }

    private void saveUserLocked(int userId) {
        File path = getUserFile(userId);
        path.getParentFile().mkdirs();
        AtomicFile file = new AtomicFile(path);
        FileOutputStream os = null;
        try {
            os = file.startWrite();
            saveUserInternalLocked(userId, os, false);
            file.finishWrite(os);
            cleanupDanglingBitmapDirectoriesLocked(userId);
        } catch (IOException | XmlPullParserException e) {
            Slog.e(TAG, "Failed to write to file " + file.getBaseFile(), e);
            file.failWrite(os);
        }
        getUserShortcutsLocked(userId).logSharingShortcutStats(this.mMetricsLogger);
    }

    private void saveUserInternalLocked(int userId, OutputStream os, boolean forBackup) throws IOException, XmlPullParserException {
        TypedXmlSerializer out;
        if (forBackup) {
            out = Xml.newFastSerializer();
            out.setOutput(os, StandardCharsets.UTF_8.name());
        } else {
            out = Xml.resolveSerializer(os);
        }
        out.startDocument((String) null, true);
        getUserShortcutsLocked(userId).saveToXml(out, forBackup);
        out.endDocument();
        os.flush();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static IOException throwForInvalidTag(int depth, String tag) throws IOException {
        throw new IOException(String.format("Invalid tag '%s' found at depth %d", tag, Integer.valueOf(depth)));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void warnForInvalidTag(int depth, String tag) throws IOException {
        Slog.w(TAG, String.format("Invalid tag '%s' found at depth %d", tag, Integer.valueOf(depth)));
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1153=4] */
    private ShortcutUser loadUserLocked(int userId) {
        File path = getUserFile(userId);
        AtomicFile file = new AtomicFile(path);
        try {
            FileInputStream in = file.openRead();
            try {
                ShortcutUser ret = loadUserInternal(userId, in, false);
                return ret;
            } catch (InvalidFileFormatException | IOException | XmlPullParserException e) {
                Slog.e(TAG, "Failed to read file " + file.getBaseFile(), e);
                return null;
            } finally {
                IoUtils.closeQuietly(in);
            }
        } catch (FileNotFoundException e2) {
            return null;
        }
    }

    private ShortcutUser loadUserInternal(int userId, InputStream is, boolean fromBackup) throws XmlPullParserException, IOException, InvalidFileFormatException {
        TypedXmlPullParser parser;
        ShortcutUser ret = null;
        if (fromBackup) {
            parser = Xml.newFastPullParser();
            parser.setInput(is, StandardCharsets.UTF_8.name());
        } else {
            parser = Xml.resolvePullParser(is);
        }
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type == 2) {
                    int depth = parser.getDepth();
                    String tag = parser.getName();
                    if (depth == 1 && "user".equals(tag)) {
                        ret = ShortcutUser.loadFromXml(this, parser, userId, fromBackup);
                    } else {
                        throwForInvalidTag(depth, tag);
                    }
                }
            } else {
                return ret;
            }
        }
    }

    private void scheduleSaveBaseState() {
        scheduleSaveInner(-10000);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleSaveUser(int userId) {
        scheduleSaveInner(userId);
    }

    private void scheduleSaveInner(int userId) {
        synchronized (this.mLock) {
            if (!this.mDirtyUserIds.contains(Integer.valueOf(userId))) {
                this.mDirtyUserIds.add(Integer.valueOf(userId));
            }
        }
        this.mHandler.removeCallbacks(this.mSaveDirtyInfoRunner);
        this.mHandler.postDelayed(this.mSaveDirtyInfoRunner, this.mSaveDelayMillis);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void saveDirtyInfo() {
        if (this.mShutdown.get()) {
            return;
        }
        try {
            try {
                Trace.traceBegin(524288L, "shortcutSaveDirtyInfo");
                synchronized (this.mLock) {
                    for (int i = this.mDirtyUserIds.size() - 1; i >= 0; i--) {
                        int userId = this.mDirtyUserIds.get(i).intValue();
                        if (userId == -10000) {
                            saveBaseStateLocked();
                        } else {
                            saveUserLocked(userId);
                        }
                    }
                    this.mDirtyUserIds.clear();
                }
            } catch (Exception e) {
                wtf("Exception in saveDirtyInfo", e);
            }
        } finally {
            Trace.traceEnd(524288L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastResetTimeLocked() {
        updateTimesLocked();
        return this.mRawLastResetTime;
    }

    long getNextResetTimeLocked() {
        updateTimesLocked();
        return this.mRawLastResetTime + this.mResetInterval;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isClockValid(long time) {
        return time >= 1420070400;
    }

    private void updateTimesLocked() {
        long now = injectCurrentTimeMillis();
        long prevLastResetTime = this.mRawLastResetTime;
        long j = this.mRawLastResetTime;
        if (j == 0) {
            this.mRawLastResetTime = now;
        } else if (now < j) {
            if (isClockValid(now)) {
                Slog.w(TAG, "Clock rewound");
                this.mRawLastResetTime = now;
            }
        } else {
            long j2 = this.mResetInterval;
            if (j + j2 <= now) {
                long offset = j % j2;
                this.mRawLastResetTime = ((now / j2) * j2) + offset;
            }
        }
        long offset2 = this.mRawLastResetTime;
        if (prevLastResetTime != offset2) {
            scheduleSaveBaseState();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isUserUnlockedL(int userId) {
        synchronized (this.mUnlockedUsers) {
            if (this.mUnlockedUsers.get(userId)) {
                return true;
            }
            return this.mUserManagerInternal.isUserUnlockingOrUnlocked(userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void throwIfUserLockedL(int userId) {
        if (!isUserUnlockedL(userId)) {
            throw new IllegalStateException("User " + userId + " is locked or not running");
        }
    }

    private boolean isUserLoadedLocked(int userId) {
        return this.mUsers.get(userId) != null;
    }

    ShortcutUser getUserShortcutsLocked(int userId) {
        if (!isUserUnlockedL(userId)) {
            if (userId != this.mLastLockedUser) {
                wtf("User still locked");
                this.mLastLockedUser = userId;
            }
        } else {
            this.mLastLockedUser = -1;
        }
        ShortcutUser userPackages = this.mUsers.get(userId);
        if (userPackages == null) {
            userPackages = loadUserLocked(userId);
            if (userPackages == null) {
                userPackages = new ShortcutUser(this, userId);
            }
            this.mUsers.put(userId, userPackages);
            checkPackageChanges(userId);
        }
        return userPackages;
    }

    ShortcutNonPersistentUser getNonPersistentUserLocked(int userId) {
        ShortcutNonPersistentUser ret = this.mShortcutNonPersistentUsers.get(userId);
        if (ret == null) {
            ShortcutNonPersistentUser ret2 = new ShortcutNonPersistentUser(this, userId);
            this.mShortcutNonPersistentUsers.put(userId, ret2);
            return ret2;
        }
        return ret;
    }

    void forEachLoadedUserLocked(Consumer<ShortcutUser> c) {
        for (int i = this.mUsers.size() - 1; i >= 0; i--) {
            c.accept(this.mUsers.valueAt(i));
        }
    }

    ShortcutPackage getPackageShortcutsLocked(String packageName, int userId) {
        return getUserShortcutsLocked(userId).getPackageShortcuts(packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ShortcutPackage getPackageShortcutsForPublisherLocked(String packageName, int userId) {
        ShortcutPackage ret = getUserShortcutsLocked(userId).getPackageShortcuts(packageName);
        ret.getUser().onCalledByPublisher(packageName);
        return ret;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ShortcutLauncher getLauncherShortcutsLocked(String packageName, int ownerUserId, int launcherUserId) {
        return getUserShortcutsLocked(ownerUserId).getLauncherShortcuts(packageName, launcherUserId);
    }

    public void cleanupBitmapsForPackage(int userId, String packageName) {
        File packagePath = new File(getUserBitmapFilePath(userId), packageName);
        if (!packagePath.isDirectory()) {
            return;
        }
        if (!FileUtils.deleteContents(packagePath) || !packagePath.delete()) {
            Slog.w(TAG, "Unable to remove directory " + packagePath);
        }
    }

    private void cleanupDanglingBitmapDirectoriesLocked(int userId) {
        long start = getStatStartTime();
        ShortcutUser user = getUserShortcutsLocked(userId);
        File bitmapDir = getUserBitmapFilePath(userId);
        File[] children = bitmapDir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                String packageName = child.getName();
                if (!user.hasPackage(packageName)) {
                    cleanupBitmapsForPackage(userId, packageName);
                } else {
                    user.getPackageShortcuts(packageName).cleanupDanglingBitmapFiles(child);
                }
            }
        }
        logDurationStat(5, start);
    }

    /* loaded from: classes2.dex */
    static class FileOutputStreamWithPath extends FileOutputStream {
        private final File mFile;

        public FileOutputStreamWithPath(File file) throws FileNotFoundException {
            super(file);
            this.mFile = file;
        }

        public File getFile() {
            return this.mFile;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public FileOutputStreamWithPath openIconFileForWrite(int userId, ShortcutInfo shortcut) throws IOException {
        File packagePath = new File(getUserBitmapFilePath(userId), shortcut.getPackage());
        if (!packagePath.isDirectory()) {
            packagePath.mkdirs();
            if (!packagePath.isDirectory()) {
                throw new IOException("Unable to create directory " + packagePath);
            }
            SELinux.restorecon(packagePath);
        }
        String baseName = String.valueOf(injectCurrentTimeMillis());
        int suffix = 0;
        while (true) {
            String filename = (suffix == 0 ? baseName : baseName + "_" + suffix) + ".png";
            File file = new File(packagePath, filename);
            if (file.exists()) {
                suffix++;
            } else {
                return new FileOutputStreamWithPath(file);
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1549=5, 1552=6] */
    /* JADX INFO: Access modifiers changed from: package-private */
    public void saveIconAndFixUpShortcutLocked(ShortcutPackage p, ShortcutInfo shortcut) {
        if (shortcut.hasIconFile() || shortcut.hasIconResource() || shortcut.hasIconUri()) {
            return;
        }
        long token = injectClearCallingIdentity();
        try {
            p.removeIcon(shortcut);
            Icon icon = shortcut.getIcon();
            if (icon == null) {
                return;
            }
            int maxIconDimension = this.mMaxIconDimension;
            switch (icon.getType()) {
                case 1:
                    icon.getBitmap();
                    break;
                case 2:
                    injectValidateIconResPackage(shortcut, icon);
                    shortcut.setIconResourceId(icon.getResId());
                    shortcut.addFlags(4);
                    shortcut.clearIcon();
                    return;
                case 3:
                default:
                    throw ShortcutInfo.getInvalidIconException();
                case 4:
                    shortcut.setIconUri(icon.getUriString());
                    shortcut.addFlags(32768);
                    shortcut.clearIcon();
                    return;
                case 5:
                    icon.getBitmap();
                    maxIconDimension = (int) (maxIconDimension * ((AdaptiveIconDrawable.getExtraInsetFraction() * 2.0f) + 1.0f));
                    break;
                case 6:
                    shortcut.setIconUri(icon.getUriString());
                    shortcut.addFlags(33280);
                    shortcut.clearIcon();
                    return;
            }
            p.saveBitmap(shortcut, maxIconDimension, this.mIconPersistFormat, this.mIconPersistQuality);
            shortcut.clearIcon();
        } finally {
            injectRestoreCallingIdentity(token);
        }
    }

    void injectValidateIconResPackage(ShortcutInfo shortcut, Icon icon) {
        if (!shortcut.getPackage().equals(icon.getResPackage())) {
            throw new IllegalArgumentException("Icon resource must reside in shortcut owner package");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Bitmap shrinkBitmap(Bitmap in, int maxSize) {
        int ow = in.getWidth();
        int oh = in.getHeight();
        if (ow <= maxSize && oh <= maxSize) {
            return in;
        }
        int longerDimension = Math.max(ow, oh);
        int nw = (ow * maxSize) / longerDimension;
        int nh = (oh * maxSize) / longerDimension;
        Bitmap scaledBitmap = Bitmap.createBitmap(nw, nh, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(scaledBitmap);
        RectF dst = new RectF(0.0f, 0.0f, nw, nh);
        c.drawBitmap(in, (Rect) null, dst, (Paint) null);
        return scaledBitmap;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void fixUpShortcutResourceNamesAndValues(ShortcutInfo si) {
        Resources publisherRes = injectGetResourcesForApplicationAsUser(si.getPackage(), si.getUserId());
        if (publisherRes != null) {
            long start = getStatStartTime();
            try {
                si.lookupAndFillInResourceNames(publisherRes);
                logDurationStat(10, start);
                si.resolveResourceStrings(publisherRes);
            } catch (Throwable th) {
                logDurationStat(10, start);
                throw th;
            }
        }
    }

    private boolean isCallerSystem() {
        int callingUid = injectBinderCallingUid();
        return UserHandle.isSameApp(callingUid, 1000);
    }

    private boolean isCallerShell() {
        int callingUid = injectBinderCallingUid();
        return callingUid == 2000 || callingUid == 0;
    }

    ComponentName injectChooserActivity() {
        if (this.mChooserActivity == null) {
            this.mChooserActivity = ComponentName.unflattenFromString(this.mContext.getResources().getString(17039902));
        }
        return this.mChooserActivity;
    }

    private boolean isCallerChooserActivity() {
        int callingUid = injectBinderCallingUid();
        ComponentName systemChooser = injectChooserActivity();
        if (systemChooser == null) {
            return false;
        }
        int uid = injectGetPackageUid(systemChooser.getPackageName(), 0);
        return uid == callingUid;
    }

    private void enforceSystemOrShell() {
        if (!isCallerSystem() && !isCallerShell()) {
            throw new SecurityException("Caller must be system or shell");
        }
    }

    private void enforceShell() {
        if (!isCallerShell()) {
            throw new SecurityException("Caller must be shell");
        }
    }

    private void enforceSystem() {
        if (!isCallerSystem()) {
            throw new SecurityException("Caller must be system");
        }
    }

    private void enforceResetThrottlingPermission() {
        if (isCallerSystem()) {
            return;
        }
        enforceCallingOrSelfPermission("android.permission.RESET_SHORTCUT_MANAGER_THROTTLING", null);
    }

    private void enforceCallingOrSelfPermission(String permission, String message) {
        if (isCallerSystem()) {
            return;
        }
        injectEnforceCallingPermission(permission, message);
    }

    void injectEnforceCallingPermission(String permission, String message) {
        this.mContext.enforceCallingPermission(permission, message);
    }

    private void verifyCallerUserId(int userId) {
        if (isCallerSystem()) {
            return;
        }
        int callingUid = injectBinderCallingUid();
        if (UserHandle.getUserId(callingUid) != userId) {
            throw new SecurityException("Invalid user-ID");
        }
    }

    private void verifyCaller(String packageName, int userId) {
        Preconditions.checkStringNotEmpty(packageName, DomainVerificationLegacySettings.ATTR_PACKAGE_NAME);
        if ("com.transsion.resolver".equals(packageName) || isCallerSystem() || injectBinderCallingUid() == PackageManager.tpmsUid) {
            return;
        }
        int callingUid = injectBinderCallingUid();
        if (UserHandle.getUserId(callingUid) != userId) {
            throw new SecurityException("Invalid user-ID");
        }
        if (injectGetPackageUid(packageName, userId) != callingUid) {
            throw new SecurityException("Calling package name mismatch");
        }
        Preconditions.checkState(!isEphemeralApp(packageName, userId), "Ephemeral apps can't use ShortcutManager");
    }

    private void verifyShortcutInfoPackage(String callerPackage, ShortcutInfo si) {
        if (si != null && !Objects.equals(callerPackage, si.getPackage())) {
            EventLog.writeEvent(1397638484, "109824443", -1, "");
            throw new SecurityException("Shortcut package name mismatch");
        }
    }

    private void verifyShortcutInfoPackages(String callerPackage, List<ShortcutInfo> list) {
        int size = list.size();
        for (int i = 0; i < size; i++) {
            verifyShortcutInfoPackage(callerPackage, list.get(i));
        }
    }

    void injectPostToHandler(Runnable r) {
        this.mHandler.post(r);
    }

    void injectRunOnNewThread(Runnable r) {
        new Thread(r).start();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void injectPostToHandlerDebounced(Object token, Runnable r) {
        Objects.requireNonNull(token);
        Objects.requireNonNull(r);
        synchronized (this.mLock) {
            this.mHandler.removeCallbacksAndMessages(token);
            this.mHandler.postDelayed(r, token, CALLBACK_DELAY);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void enforceMaxActivityShortcuts(int numShortcuts) {
        if (numShortcuts > this.mMaxShortcuts) {
            throw new IllegalArgumentException("Max number of dynamic shortcuts exceeded");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getMaxActivityShortcuts() {
        return this.mMaxShortcuts;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getMaxAppShortcuts() {
        return this.mMaxShortcutsPerApp;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void packageShortcutsChanged(ShortcutPackage sp, List<ShortcutInfo> changedShortcuts, List<ShortcutInfo> removedShortcuts) {
        Objects.requireNonNull(sp);
        String packageName = sp.getPackageName();
        int userId = sp.getPackageUserId();
        injectPostToHandlerDebounced(sp, notifyListenerRunnable(packageName, userId));
        notifyShortcutChangeCallbacks(packageName, userId, changedShortcuts, removedShortcuts);
        sp.scheduleSave();
    }

    private void notifyListeners(String packageName, int userId) {
        injectPostToHandler(notifyListenerRunnable(packageName, userId));
    }

    private Runnable notifyListenerRunnable(final String packageName, final int userId) {
        return new Runnable() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda24
            @Override // java.lang.Runnable
            public final void run() {
                ShortcutService.this.m5672xf4aa8780(userId, packageName);
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyListenerRunnable$2$com-android-server-pm-ShortcutService  reason: not valid java name */
    public /* synthetic */ void m5672xf4aa8780(int userId, String packageName) {
        try {
            synchronized (this.mLock) {
                if (isUserUnlockedL(userId)) {
                    ArrayList<ShortcutServiceInternal.ShortcutChangeListener> copy = new ArrayList<>(this.mListeners);
                    for (int i = copy.size() - 1; i >= 0; i--) {
                        copy.get(i).onShortcutChanged(packageName, userId);
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    private void notifyShortcutChangeCallbacks(final String packageName, final int userId, List<ShortcutInfo> changedShortcuts, List<ShortcutInfo> removedShortcuts) {
        final List<ShortcutInfo> changedList = removeNonKeyFields(changedShortcuts);
        final List<ShortcutInfo> removedList = removeNonKeyFields(removedShortcuts);
        final UserHandle user = UserHandle.of(userId);
        injectPostToHandler(new Runnable() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda12
            @Override // java.lang.Runnable
            public final void run() {
                ShortcutService.this.m5673x8782cd1c(userId, changedList, packageName, user, removedList);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyShortcutChangeCallbacks$3$com-android-server-pm-ShortcutService  reason: not valid java name */
    public /* synthetic */ void m5673x8782cd1c(int userId, List changedList, String packageName, UserHandle user, List removedList) {
        try {
            synchronized (this.mLock) {
                if (isUserUnlockedL(userId)) {
                    ArrayList<LauncherApps.ShortcutChangeCallback> copy = new ArrayList<>(this.mShortcutChangeCallbacks);
                    for (int i = copy.size() - 1; i >= 0; i--) {
                        if (!CollectionUtils.isEmpty(changedList)) {
                            copy.get(i).onShortcutsAddedOrUpdated(packageName, changedList, user);
                        }
                        if (!CollectionUtils.isEmpty(removedList)) {
                            copy.get(i).onShortcutsRemoved(packageName, removedList, user);
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    private List<ShortcutInfo> removeNonKeyFields(List<ShortcutInfo> shortcutInfos) {
        if (CollectionUtils.isEmpty(shortcutInfos)) {
            return shortcutInfos;
        }
        int size = shortcutInfos.size();
        List<ShortcutInfo> keyFieldOnlyShortcuts = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ShortcutInfo si = shortcutInfos.get(i);
            if (si.hasKeyFieldsOnly()) {
                keyFieldOnlyShortcuts.add(si);
            } else {
                keyFieldOnlyShortcuts.add(si.clone(4));
            }
        }
        return keyFieldOnlyShortcuts;
    }

    private void fixUpIncomingShortcutInfo(ShortcutInfo shortcut, boolean forUpdate, boolean forPinRequest) {
        if (shortcut.isReturnedByServer()) {
            Log.w(TAG, "Re-publishing ShortcutInfo returned by server is not supported. Some information such as icon may lost from shortcut.");
        }
        Objects.requireNonNull(shortcut, "Null shortcut detected");
        if (shortcut.getActivity() != null) {
            Preconditions.checkState(shortcut.getPackage().equals(shortcut.getActivity().getPackageName()), "Cannot publish shortcut: activity " + shortcut.getActivity() + " does not belong to package " + shortcut.getPackage());
            Preconditions.checkState(injectIsMainActivity(shortcut.getActivity(), shortcut.getUserId()), "Cannot publish shortcut: activity " + shortcut.getActivity() + " is not main activity");
        }
        if (!forUpdate) {
            shortcut.enforceMandatoryFields(forPinRequest);
            if (!forPinRequest) {
                Preconditions.checkState(shortcut.getActivity() != null, "Cannot publish shortcut: target activity is not set");
            }
        }
        if (shortcut.getIcon() != null) {
            ShortcutInfo.validateIcon(shortcut.getIcon());
        }
        shortcut.replaceFlags(shortcut.getFlags() & 8192);
    }

    private void fixUpIncomingShortcutInfo(ShortcutInfo shortcut, boolean forUpdate) {
        fixUpIncomingShortcutInfo(shortcut, forUpdate, false);
    }

    public void validateShortcutForPinRequest(ShortcutInfo shortcut) {
        fixUpIncomingShortcutInfo(shortcut, false, true);
    }

    private void fillInDefaultActivity(List<ShortcutInfo> shortcuts) {
        ComponentName defaultActivity = null;
        for (int i = shortcuts.size() - 1; i >= 0; i--) {
            ShortcutInfo si = shortcuts.get(i);
            if (si.getActivity() == null) {
                if (defaultActivity == null) {
                    defaultActivity = injectGetDefaultMainActivity(si.getPackage(), si.getUserId());
                    Preconditions.checkState(defaultActivity != null, "Launcher activity not found for package " + si.getPackage());
                }
                si.setActivity(defaultActivity);
            }
        }
    }

    private void assignImplicitRanks(List<ShortcutInfo> shortcuts) {
        for (int i = shortcuts.size() - 1; i >= 0; i--) {
            shortcuts.get(i).setImplicitRank(i);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public List<ShortcutInfo> setReturnedByServer(List<ShortcutInfo> shortcuts) {
        for (int i = shortcuts.size() - 1; i >= 0; i--) {
            shortcuts.get(i).setReturnedByServer();
        }
        return shortcuts;
    }

    public boolean setDynamicShortcuts(String packageName, ParceledListSlice shortcutInfoList, int userId) {
        verifyCaller(packageName, userId);
        boolean unlimited = injectHasUnlimitedShortcutsApiCallsPermission(injectBinderCallingPid(), injectBinderCallingUid());
        List<ShortcutInfo> newShortcuts = shortcutInfoList.getList();
        verifyShortcutInfoPackages(packageName, newShortcuts);
        int size = newShortcuts.size();
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            ShortcutPackage ps = getPackageShortcutsForPublisherLocked(packageName, userId);
            ps.ensureImmutableShortcutsNotIncluded(newShortcuts, true);
            ps.ensureNoBitmapIconIfShortcutIsLongLived(newShortcuts);
            fillInDefaultActivity(newShortcuts);
            ps.enforceShortcutCountsBeforeOperation(newShortcuts, 0);
            if (!ps.tryApiCall(unlimited)) {
                return false;
            }
            ps.clearAllImplicitRanks();
            assignImplicitRanks(newShortcuts);
            for (int i = 0; i < size; i++) {
                fixUpIncomingShortcutInfo(newShortcuts.get(i), false);
            }
            ArrayList<ShortcutInfo> cachedOrPinned = new ArrayList<>();
            ps.findAll(cachedOrPinned, new Predicate() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda6
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return ShortcutService.lambda$setDynamicShortcuts$4((ShortcutInfo) obj);
                }
            }, 4);
            List<ShortcutInfo> removedShortcuts = ps.deleteAllDynamicShortcuts();
            for (int i2 = 0; i2 < size; i2++) {
                ShortcutInfo newShortcut = newShortcuts.get(i2);
                ps.addOrReplaceDynamicShortcut(newShortcut);
            }
            ps.adjustRanks();
            List<ShortcutInfo> changedShortcuts = prepareChangedShortcuts(cachedOrPinned, newShortcuts, removedShortcuts, ps);
            packageShortcutsChanged(ps, changedShortcuts, removedShortcuts);
            verifyStates();
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$setDynamicShortcuts$4(ShortcutInfo si) {
        return si.isVisibleToPublisher() && si.isDynamic() && (si.isCached() || si.isPinned());
    }

    public boolean updateShortcuts(String packageName, ParceledListSlice shortcutInfoList, int userId) {
        verifyCaller(packageName, userId);
        boolean unlimited = injectHasUnlimitedShortcutsApiCallsPermission(injectBinderCallingPid(), injectBinderCallingUid());
        List<ShortcutInfo> newShortcuts = shortcutInfoList.getList();
        verifyShortcutInfoPackages(packageName, newShortcuts);
        int size = newShortcuts.size();
        final List<ShortcutInfo> changedShortcuts = new ArrayList<>(1);
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            final ShortcutPackage ps = getPackageShortcutsForPublisherLocked(packageName, userId);
            ps.ensureImmutableShortcutsNotIncluded(newShortcuts, true);
            ps.ensureNoBitmapIconIfShortcutIsLongLived(newShortcuts);
            ps.ensureAllShortcutsVisibleToLauncher(newShortcuts);
            ps.enforceShortcutCountsBeforeOperation(newShortcuts, 2);
            if (!ps.tryApiCall(unlimited)) {
                return false;
            }
            ps.clearAllImplicitRanks();
            assignImplicitRanks(newShortcuts);
            for (int i = 0; i < size; i++) {
                final ShortcutInfo source = newShortcuts.get(i);
                fixUpIncomingShortcutInfo(source, true);
                ps.mutateShortcut(source.getId(), null, new Consumer() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda14
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ShortcutService.this.m5676lambda$updateShortcuts$5$comandroidserverpmShortcutService(source, ps, changedShortcuts, (ShortcutInfo) obj);
                    }
                });
            }
            ps.adjustRanks();
            packageShortcutsChanged(ps, changedShortcuts.isEmpty() ? null : changedShortcuts, null);
            verifyStates();
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateShortcuts$5$com-android-server-pm-ShortcutService  reason: not valid java name */
    public /* synthetic */ void m5676lambda$updateShortcuts$5$comandroidserverpmShortcutService(ShortcutInfo source, ShortcutPackage ps, List changedShortcuts, ShortcutInfo target) {
        if (target == null || !target.isVisibleToPublisher()) {
            return;
        }
        if (target.isEnabled() != source.isEnabled()) {
            Slog.w(TAG, "ShortcutInfo.enabled cannot be changed with updateShortcuts()");
        }
        if (target.isLongLived() != source.isLongLived()) {
            Slog.w(TAG, "ShortcutInfo.longLived cannot be changed with updateShortcuts()");
        }
        if (source.hasRank()) {
            target.setRankChanged();
            target.setImplicitRank(source.getImplicitRank());
        }
        boolean replacingIcon = source.getIcon() != null;
        if (replacingIcon) {
            ps.removeIcon(target);
        }
        target.copyNonNullFieldsFrom(source);
        target.setTimestamp(injectCurrentTimeMillis());
        if (replacingIcon) {
            saveIconAndFixUpShortcutLocked(ps, target);
        }
        if (replacingIcon || source.hasStringResources()) {
            fixUpShortcutResourceNamesAndValues(target);
        }
        changedShortcuts.add(target);
    }

    public boolean addDynamicShortcuts(String packageName, ParceledListSlice shortcutInfoList, int userId) {
        verifyCaller(packageName, userId);
        boolean unlimited = injectHasUnlimitedShortcutsApiCallsPermission(injectBinderCallingPid(), injectBinderCallingUid());
        List<ShortcutInfo> newShortcuts = shortcutInfoList.getList();
        verifyShortcutInfoPackages(packageName, newShortcuts);
        int size = newShortcuts.size();
        List<ShortcutInfo> changedShortcuts = null;
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            ShortcutPackage ps = getPackageShortcutsForPublisherLocked(packageName, userId);
            ps.ensureImmutableShortcutsNotIncluded(newShortcuts, true);
            ps.ensureNoBitmapIconIfShortcutIsLongLived(newShortcuts);
            fillInDefaultActivity(newShortcuts);
            ps.enforceShortcutCountsBeforeOperation(newShortcuts, 1);
            ps.clearAllImplicitRanks();
            assignImplicitRanks(newShortcuts);
            if (ps.tryApiCall(unlimited)) {
                for (int i = 0; i < size; i++) {
                    ShortcutInfo newShortcut = newShortcuts.get(i);
                    fixUpIncomingShortcutInfo(newShortcut, false);
                    newShortcut.setRankChanged();
                    ps.addOrReplaceDynamicShortcut(newShortcut);
                    if (changedShortcuts == null) {
                        changedShortcuts = new ArrayList<>(1);
                    }
                    changedShortcuts.add(newShortcut);
                }
                ps.adjustRanks();
                packageShortcutsChanged(ps, changedShortcuts, null);
                verifyStates();
                return true;
            }
            return false;
        }
    }

    public void pushDynamicShortcut(String packageName, ShortcutInfo shortcut, int userId) {
        verifyCaller(packageName, userId);
        verifyShortcutInfoPackage(packageName, shortcut);
        List<ShortcutInfo> changedShortcuts = new ArrayList<>();
        List<ShortcutInfo> removedShortcuts = null;
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            ShortcutPackage ps = getPackageShortcutsForPublisherLocked(packageName, userId);
            ps.ensureNotImmutable(shortcut.getId(), true);
            fillInDefaultActivity(Arrays.asList(shortcut));
            if (!shortcut.hasRank()) {
                shortcut.setRank(0);
            }
            ps.clearAllImplicitRanks();
            shortcut.setImplicitRank(0);
            fixUpIncomingShortcutInfo(shortcut, false);
            shortcut.setRankChanged();
            boolean deleted = ps.pushDynamicShortcut(shortcut, changedShortcuts);
            if (deleted) {
                if (changedShortcuts.isEmpty()) {
                    return;
                }
                removedShortcuts = Collections.singletonList(changedShortcuts.get(0));
                changedShortcuts.clear();
            }
            changedShortcuts.add(shortcut);
            ps.adjustRanks();
            packageShortcutsChanged(ps, changedShortcuts, removedShortcuts);
            reportShortcutUsedInternal(packageName, shortcut.getId(), userId);
            verifyStates();
        }
    }

    public void requestPinShortcut(String packageName, ShortcutInfo shortcut, IntentSender resultIntent, int userId, AndroidFuture<String> ret) {
        Objects.requireNonNull(shortcut);
        Preconditions.checkArgument(shortcut.isEnabled(), "Shortcut must be enabled");
        Preconditions.checkArgument(true ^ shortcut.isExcludedFromSurfaces(1), "Shortcut excluded from launcher cannot be pinned");
        ret.complete(String.valueOf(requestPinItem(packageName, userId, shortcut, null, null, resultIntent)));
    }

    public void createShortcutResultIntent(String packageName, ShortcutInfo shortcut, int userId, AndroidFuture<Intent> ret) throws RemoteException {
        Intent intent;
        Objects.requireNonNull(shortcut);
        Preconditions.checkArgument(shortcut.isEnabled(), "Shortcut must be enabled");
        verifyCaller(packageName, userId);
        verifyShortcutInfoPackage(packageName, shortcut);
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            intent = this.mShortcutRequestPinProcessor.createShortcutResultIntent(shortcut, userId);
        }
        verifyStates();
        ret.complete(intent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean requestPinItem(String callingPackage, int userId, ShortcutInfo shortcut, AppWidgetProviderInfo appWidget, Bundle extras, IntentSender resultIntent) {
        return requestPinItem(callingPackage, userId, shortcut, appWidget, extras, resultIntent, injectBinderCallingPid(), injectBinderCallingUid());
    }

    /* JADX WARN: Removed duplicated region for block: B:24:0x0020 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private boolean requestPinItem(String callingPackage, int userId, ShortcutInfo shortcut, AppWidgetProviderInfo appWidget, Bundle extras, IntentSender resultIntent, int callingPid, int callingUid) {
        boolean ret;
        verifyCaller(callingPackage, userId);
        if (shortcut != null && injectHasAccessShortcutsPermission(callingPid, callingUid)) {
            synchronized (this.mLock) {
                throwIfUserLockedL(userId);
                Preconditions.checkState(isUidForegroundLocked(callingUid), "Calling application must have a foreground activity or a foreground service");
                if (shortcut != null) {
                    String shortcutPackage = shortcut.getPackage();
                    ShortcutPackage ps = getPackageShortcutsForPublisherLocked(shortcutPackage, userId);
                    String id = shortcut.getId();
                    if (ps.isShortcutExistsAndInvisibleToPublisher(id)) {
                        ps.updateInvisibleShortcutForPinRequestWith(shortcut);
                        packageShortcutsChanged(ps, Collections.singletonList(shortcut), null);
                    }
                }
                ret = this.mShortcutRequestPinProcessor.requestPinItemLocked(shortcut, appWidget, extras, userId, resultIntent);
            }
            verifyStates();
            return ret;
        }
        verifyShortcutInfoPackage(callingPackage, shortcut);
        synchronized (this.mLock) {
        }
    }

    public void disableShortcuts(String packageName, List shortcutIds, CharSequence disabledMessage, int disabledMessageResId, int userId) {
        ShortcutPackage ps;
        int i;
        verifyCaller(packageName, userId);
        Objects.requireNonNull(shortcutIds, "shortcutIds must be provided");
        List<ShortcutInfo> changedShortcuts = null;
        List<ShortcutInfo> removedShortcuts = null;
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            ps = getPackageShortcutsForPublisherLocked(packageName, userId);
            ps.ensureImmutableShortcutsNotIncludedWithIds(shortcutIds, true);
            String disabledMessageString = disabledMessage == null ? null : disabledMessage.toString();
            int i2 = shortcutIds.size() - 1;
            while (i2 >= 0) {
                String id = (String) Preconditions.checkStringNotEmpty((String) shortcutIds.get(i2));
                if (!ps.isShortcutExistsAndVisibleToPublisher(id)) {
                    i = i2;
                } else {
                    i = i2;
                    ShortcutInfo deleted = ps.disableWithId(id, disabledMessageString, disabledMessageResId, false, true, 1);
                    if (deleted == null) {
                        if (changedShortcuts == null) {
                            changedShortcuts = new ArrayList<>(1);
                        }
                        changedShortcuts.add(ps.findShortcutById(id));
                    } else {
                        if (removedShortcuts == null) {
                            removedShortcuts = new ArrayList<>(1);
                        }
                        removedShortcuts.add(deleted);
                    }
                }
                i2 = i - 1;
            }
            ps.adjustRanks();
        }
        packageShortcutsChanged(ps, changedShortcuts, removedShortcuts);
        verifyStates();
    }

    public void enableShortcuts(String packageName, List shortcutIds, int userId) {
        ShortcutPackage ps;
        verifyCaller(packageName, userId);
        Objects.requireNonNull(shortcutIds, "shortcutIds must be provided");
        List<ShortcutInfo> changedShortcuts = null;
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            ps = getPackageShortcutsForPublisherLocked(packageName, userId);
            ps.ensureImmutableShortcutsNotIncludedWithIds(shortcutIds, true);
            for (int i = shortcutIds.size() - 1; i >= 0; i--) {
                String id = (String) Preconditions.checkStringNotEmpty((String) shortcutIds.get(i));
                if (ps.isShortcutExistsAndVisibleToPublisher(id)) {
                    ps.enableWithId(id);
                    if (changedShortcuts == null) {
                        changedShortcuts = new ArrayList<>(1);
                    }
                    changedShortcuts.add(ps.findShortcutById(id));
                }
            }
        }
        packageShortcutsChanged(ps, changedShortcuts, null);
        verifyStates();
    }

    public void removeDynamicShortcuts(String packageName, List<String> shortcutIds, int userId) {
        ShortcutPackage ps;
        verifyCaller(packageName, userId);
        Objects.requireNonNull(shortcutIds, "shortcutIds must be provided");
        List<ShortcutInfo> changedShortcuts = null;
        List<ShortcutInfo> removedShortcuts = null;
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            ps = getPackageShortcutsForPublisherLocked(packageName, userId);
            ps.ensureImmutableShortcutsNotIncludedWithIds(shortcutIds, true);
            for (int i = shortcutIds.size() - 1; i >= 0; i--) {
                String id = (String) Preconditions.checkStringNotEmpty(shortcutIds.get(i));
                if (ps.isShortcutExistsAndVisibleToPublisher(id)) {
                    ShortcutInfo removed = ps.deleteDynamicWithId(id, true, false);
                    if (removed == null) {
                        if (changedShortcuts == null) {
                            changedShortcuts = new ArrayList<>(1);
                        }
                        changedShortcuts.add(ps.findShortcutById(id));
                    } else {
                        if (removedShortcuts == null) {
                            removedShortcuts = new ArrayList<>(1);
                        }
                        removedShortcuts.add(removed);
                    }
                }
            }
            ps.adjustRanks();
        }
        packageShortcutsChanged(ps, changedShortcuts, removedShortcuts);
        verifyStates();
    }

    public void removeAllDynamicShortcuts(String packageName, int userId) {
        ShortcutPackage ps;
        List<ShortcutInfo> removedShortcuts;
        List<ShortcutInfo> changedShortcuts;
        verifyCaller(packageName, userId);
        List<ShortcutInfo> changedShortcuts2 = new ArrayList<>();
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            ps = getPackageShortcutsForPublisherLocked(packageName, userId);
            ps.findAll(changedShortcuts2, new Predicate() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda27
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return ShortcutService.lambda$removeAllDynamicShortcuts$6((ShortcutInfo) obj);
                }
            }, 4);
            removedShortcuts = ps.deleteAllDynamicShortcuts();
            changedShortcuts = prepareChangedShortcuts(changedShortcuts2, (List<ShortcutInfo>) null, removedShortcuts, ps);
        }
        packageShortcutsChanged(ps, changedShortcuts, removedShortcuts);
        verifyStates();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$removeAllDynamicShortcuts$6(ShortcutInfo si) {
        return si.isVisibleToPublisher() && si.isDynamic() && (si.isCached() || si.isPinned());
    }

    public void removeLongLivedShortcuts(String packageName, List shortcutIds, int userId) {
        ShortcutPackage ps;
        verifyCaller(packageName, userId);
        Objects.requireNonNull(shortcutIds, "shortcutIds must be provided");
        List<ShortcutInfo> changedShortcuts = null;
        List<ShortcutInfo> removedShortcuts = null;
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            ps = getPackageShortcutsForPublisherLocked(packageName, userId);
            ps.ensureImmutableShortcutsNotIncludedWithIds(shortcutIds, true);
            for (int i = shortcutIds.size() - 1; i >= 0; i--) {
                String id = (String) Preconditions.checkStringNotEmpty((String) shortcutIds.get(i));
                if (ps.isShortcutExistsAndVisibleToPublisher(id)) {
                    ShortcutInfo removed = ps.deleteLongLivedWithId(id, true);
                    if (removed != null) {
                        if (removedShortcuts == null) {
                            removedShortcuts = new ArrayList<>(1);
                        }
                        removedShortcuts.add(removed);
                    } else {
                        if (changedShortcuts == null) {
                            changedShortcuts = new ArrayList<>(1);
                        }
                        changedShortcuts.add(ps.findShortcutById(id));
                    }
                }
            }
            ps.adjustRanks();
        }
        packageShortcutsChanged(ps, changedShortcuts, removedShortcuts);
        verifyStates();
    }

    public ParceledListSlice<ShortcutInfo> getShortcuts(String packageName, int matchFlags, int userId) {
        ParceledListSlice<ShortcutInfo> shortcutsWithQueryLocked;
        verifyCaller(packageName, userId);
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            int i = 1;
            boolean matchDynamic = (matchFlags & 2) != 0;
            boolean matchPinned = (matchFlags & 4) != 0;
            boolean matchManifest = (matchFlags & 1) != 0;
            boolean matchCached = (matchFlags & 8) != 0;
            if (!matchDynamic) {
                i = 0;
            }
            final int shortcutFlags = i | (matchPinned ? 2 : 0) | (matchManifest ? 32 : 0) | (matchCached ? 1610629120 : 0);
            shortcutsWithQueryLocked = getShortcutsWithQueryLocked(packageName, userId, 9, new Predicate() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda29
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return ShortcutService.lambda$getShortcuts$7(shortcutFlags, (ShortcutInfo) obj);
                }
            });
        }
        return shortcutsWithQueryLocked;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getShortcuts$7(int shortcutFlags, ShortcutInfo si) {
        return si.isVisibleToPublisher() && (si.getFlags() & shortcutFlags) != 0;
    }

    public ParceledListSlice getShareTargets(String packageName, final IntentFilter filter, int userId) {
        ParceledListSlice parceledListSlice;
        Preconditions.checkStringNotEmpty(packageName, DomainVerificationLegacySettings.ATTR_PACKAGE_NAME);
        Objects.requireNonNull(filter, "intentFilter");
        if (!isCallerChooserActivity()) {
            verifyCaller(packageName, userId);
        }
        enforceCallingOrSelfPermission("android.permission.MANAGE_APP_PREDICTIONS", "getShareTargets");
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            final List<ShortcutManager.ShareShortcutInfo> shortcutInfoList = new ArrayList<>();
            ShortcutUser user = getUserShortcutsLocked(userId);
            user.forAllPackages(new Consumer() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda16
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    shortcutInfoList.addAll(((ShortcutPackage) obj).getMatchingShareTargets(filter));
                }
            });
            parceledListSlice = new ParceledListSlice(shortcutInfoList);
        }
        return parceledListSlice;
    }

    public boolean hasShareTargets(String packageName, String packageToCheck, int userId) {
        boolean hasShareTargets;
        verifyCaller(packageName, userId);
        enforceCallingOrSelfPermission("android.permission.MANAGE_APP_PREDICTIONS", "hasShareTargets");
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            hasShareTargets = getPackageShortcutsLocked(packageToCheck, userId).hasShareTargets();
        }
        return hasShareTargets;
    }

    public boolean isSharingShortcut(int callingUserId, String callingPackage, String packageName, String shortcutId, int userId, IntentFilter filter) {
        verifyCaller(callingPackage, callingUserId);
        enforceCallingOrSelfPermission("android.permission.MANAGE_APP_PREDICTIONS", "isSharingShortcut");
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            throwIfUserLockedL(callingUserId);
            List<ShortcutManager.ShareShortcutInfo> matchedTargets = getPackageShortcutsLocked(packageName, userId).getMatchingShareTargets(filter);
            int matchedSize = matchedTargets.size();
            for (int i = 0; i < matchedSize; i++) {
                if (matchedTargets.get(i).getShortcutInfo().getId().equals(shortcutId)) {
                    return true;
                }
            }
            return false;
        }
    }

    private ParceledListSlice<ShortcutInfo> getShortcutsWithQueryLocked(String packageName, int userId, int cloneFlags, Predicate<ShortcutInfo> filter) {
        ArrayList<ShortcutInfo> ret = new ArrayList<>();
        ShortcutPackage ps = getPackageShortcutsForPublisherLocked(packageName, userId);
        ps.findAll(ret, filter, cloneFlags);
        return new ParceledListSlice<>(setReturnedByServer(ret));
    }

    public int getMaxShortcutCountPerActivity(String packageName, int userId) throws RemoteException {
        verifyCaller(packageName, userId);
        return this.mMaxShortcuts;
    }

    public int getRemainingCallCount(String packageName, int userId) {
        int apiCallCount;
        verifyCaller(packageName, userId);
        boolean unlimited = injectHasUnlimitedShortcutsApiCallsPermission(injectBinderCallingPid(), injectBinderCallingUid());
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            ShortcutPackage ps = getPackageShortcutsForPublisherLocked(packageName, userId);
            apiCallCount = this.mMaxUpdatesPerInterval - ps.getApiCallCount(unlimited);
        }
        return apiCallCount;
    }

    public long getRateLimitResetTime(String packageName, int userId) {
        long nextResetTimeLocked;
        verifyCaller(packageName, userId);
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            nextResetTimeLocked = getNextResetTimeLocked();
        }
        return nextResetTimeLocked;
    }

    public int getIconMaxDimensions(String packageName, int userId) {
        int i;
        verifyCaller(packageName, userId);
        synchronized (this.mLock) {
            i = this.mMaxIconDimension;
        }
        return i;
    }

    public void reportShortcutUsed(String packageName, String shortcutId, int userId) {
        verifyCaller(packageName, userId);
        Objects.requireNonNull(shortcutId);
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            ShortcutPackage ps = getPackageShortcutsForPublisherLocked(packageName, userId);
            if (ps.findShortcutById(shortcutId) == null) {
                Log.w(TAG, String.format("reportShortcutUsed: package %s doesn't have shortcut %s", packageName, shortcutId));
            } else {
                reportShortcutUsedInternal(packageName, shortcutId, userId);
            }
        }
    }

    private void reportShortcutUsedInternal(String packageName, String shortcutId, int userId) {
        long token = injectClearCallingIdentity();
        try {
            this.mUsageStatsManagerInternal.reportShortcutUsage(packageName, shortcutId, userId);
        } finally {
            injectRestoreCallingIdentity(token);
        }
    }

    public boolean isRequestPinItemSupported(int callingUserId, int requestType) {
        verifyCallerUserId(callingUserId);
        long token = injectClearCallingIdentity();
        try {
            return this.mShortcutRequestPinProcessor.isRequestPinItemSupported(callingUserId, requestType);
        } finally {
            injectRestoreCallingIdentity(token);
        }
    }

    public void resetThrottling() {
        enforceSystemOrShell();
        resetThrottlingInner(getCallingUserId());
    }

    void resetThrottlingInner(int userId) {
        synchronized (this.mLock) {
            if (!isUserUnlockedL(userId)) {
                Log.w(TAG, "User " + userId + " is locked or not running");
                return;
            }
            getUserShortcutsLocked(userId).resetThrottling();
            scheduleSaveUser(userId);
            Slog.i(TAG, "ShortcutManager: throttling counter reset for user " + userId);
        }
    }

    void resetAllThrottlingInner() {
        synchronized (this.mLock) {
            this.mRawLastResetTime = injectCurrentTimeMillis();
        }
        scheduleSaveBaseState();
        Slog.i(TAG, "ShortcutManager: throttling counter reset for all users");
    }

    public void onApplicationActive(String packageName, int userId) {
        enforceResetThrottlingPermission();
        synchronized (this.mLock) {
            if (isUserUnlockedL(userId)) {
                getPackageShortcutsLocked(packageName, userId).resetRateLimitingForCommandLineNoSaving();
                saveUserLocked(userId);
            }
        }
    }

    boolean hasShortcutHostPermission(String callingPackage, int userId, int callingPid, int callingUid) {
        if (canSeeAnyPinnedShortcut(callingPackage, userId, callingPid, callingUid)) {
            return true;
        }
        long start = getStatStartTime();
        try {
            return hasShortcutHostPermissionInner(callingPackage, userId);
        } finally {
            logDurationStat(4, start);
        }
    }

    boolean canSeeAnyPinnedShortcut(String callingPackage, int userId, int callingPid, int callingUid) {
        boolean hasHostPackage;
        if (injectHasAccessShortcutsPermission(callingPid, callingUid)) {
            return true;
        }
        synchronized (this.mNonPersistentUsersLock) {
            hasHostPackage = getNonPersistentUserLocked(userId).hasHostPackage(callingPackage);
        }
        return hasHostPackage;
    }

    boolean injectHasAccessShortcutsPermission(int callingPid, int callingUid) {
        return this.mContext.checkPermission("android.permission.ACCESS_SHORTCUTS", callingPid, callingUid) == 0;
    }

    boolean injectHasUnlimitedShortcutsApiCallsPermission(int callingPid, int callingUid) {
        return this.mContext.checkPermission("android.permission.UNLIMITED_SHORTCUTS_API_CALLS", callingPid, callingUid) == 0;
    }

    boolean hasShortcutHostPermissionInner(String packageName, int userId) {
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            String defaultLauncher = getDefaultLauncher(userId);
            if (defaultLauncher != null) {
                return defaultLauncher.equals(packageName);
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getDefaultLauncher(int userId) {
        long start = getStatStartTime();
        long token = injectClearCallingIdentity();
        try {
            synchronized (this.mLock) {
                throwIfUserLockedL(userId);
                ShortcutUser user = getUserShortcutsLocked(userId);
                String cachedLauncher = user.getCachedLauncher();
                if (cachedLauncher != null) {
                    return cachedLauncher;
                }
                long startGetHomeRoleHoldersAsUser = getStatStartTime();
                String defaultLauncher = injectGetHomeRoleHolderAsUser(getParentOrSelfUserId(userId));
                logDurationStat(0, startGetHomeRoleHoldersAsUser);
                if (defaultLauncher != null) {
                    user.setCachedLauncher(defaultLauncher);
                } else {
                    Slog.e(TAG, "Default launcher not found. user: " + userId);
                }
                return defaultLauncher;
            }
        } finally {
            injectRestoreCallingIdentity(token);
            logDurationStat(16, start);
        }
    }

    public void setShortcutHostPackage(String type, String packageName, int userId) {
        synchronized (this.mNonPersistentUsersLock) {
            getNonPersistentUserLocked(userId).setShortcutHostPackage(type, packageName);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cleanUpPackageForAllLoadedUsers(final String packageName, final int packageUserId, final boolean appStillExists) {
        synchronized (this.mLock) {
            forEachLoadedUserLocked(new Consumer() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda18
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ShortcutService.this.m5670xe5d3b29c(packageName, packageUserId, appStillExists, (ShortcutUser) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$cleanUpPackageForAllLoadedUsers$9$com-android-server-pm-ShortcutService  reason: not valid java name */
    public /* synthetic */ void m5670xe5d3b29c(String packageName, int packageUserId, boolean appStillExists, ShortcutUser user) {
        cleanUpPackageLocked(packageName, user.getUserId(), packageUserId, appStillExists);
    }

    void cleanUpPackageLocked(final String packageName, int owningUserId, final int packageUserId, boolean appStillExists) {
        boolean wasUserLoaded = isUserLoadedLocked(owningUserId);
        ShortcutUser user = getUserShortcutsLocked(owningUserId);
        boolean doNotify = false;
        final ShortcutPackage sp = packageUserId == owningUserId ? user.removePackage(packageName) : null;
        if (sp != null) {
            doNotify = true;
        }
        user.removeLauncher(packageUserId, packageName);
        user.forAllLaunchers(new Consumer() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda19
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((ShortcutLauncher) obj).cleanUpPackage(packageName, packageUserId);
            }
        });
        user.forAllPackages(new Consumer() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda20
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((ShortcutPackage) obj).refreshPinnedFlags();
            }
        });
        if (doNotify) {
            notifyListeners(packageName, owningUserId);
        }
        if (appStillExists && packageUserId == owningUserId) {
            user.rescanPackageIfNeeded(packageName, true);
        }
        if (!appStillExists && packageUserId == owningUserId && sp != null) {
            injectPostToHandler(new Runnable() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda21
                @Override // java.lang.Runnable
                public final void run() {
                    ShortcutPackage.this.removeShortcutPackageItem();
                }
            });
        }
        if (!wasUserLoaded) {
            unloadUserLocked(owningUserId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class LocalService extends ShortcutServiceInternal {
        private LocalService() {
        }

        public List<ShortcutInfo> getShortcuts(final int launcherUserId, final String callingPackage, final long changedSince, String packageName, List<String> shortcutIds, final List<LocusId> locusIds, final ComponentName componentName, final int queryFlags, final int userId, final int callingPid, final int callingUid) {
            int flags;
            List<String> shortcutIds2;
            final ArrayList<ShortcutInfo> ret;
            LocalService localService;
            ArrayList<ShortcutInfo> ret2 = new ArrayList<>();
            if ((queryFlags & 4) != 0) {
                flags = 4;
            } else if ((queryFlags & 2048) == 0) {
                flags = 27;
            } else {
                int flags2 = 27 & (-17);
                flags = flags2;
            }
            final int cloneFlag = flags;
            if (packageName != null) {
                shortcutIds2 = shortcutIds;
            } else {
                shortcutIds2 = null;
            }
            synchronized (ShortcutService.this.mLock) {
                try {
                    ShortcutService.this.throwIfUserLockedL(userId);
                    ShortcutService.this.throwIfUserLockedL(launcherUserId);
                    ShortcutService.this.getLauncherShortcutsLocked(callingPackage, userId, launcherUserId).attemptToRestoreIfNeededAndSave();
                    if (packageName != null) {
                        ret = ret2;
                        try {
                            getShortcutsInnerLocked(launcherUserId, callingPackage, packageName, shortcutIds2, locusIds, changedSince, componentName, queryFlags, userId, ret, cloneFlag, callingPid, callingUid);
                            localService = this;
                        } catch (Throwable th) {
                            th = th;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                            }
                            throw th;
                        }
                    } else {
                        ret = ret2;
                        final List<String> shortcutIdsF = shortcutIds2;
                        localService = this;
                        try {
                            try {
                                ShortcutService.this.getUserShortcutsLocked(userId).forAllPackages(new Consumer() { // from class: com.android.server.pm.ShortcutService$LocalService$$ExternalSyntheticLambda1
                                    @Override // java.util.function.Consumer
                                    public final void accept(Object obj) {
                                        ShortcutService.LocalService.this.m5682xf47f55d3(launcherUserId, callingPackage, shortcutIdsF, locusIds, changedSince, componentName, queryFlags, userId, ret, cloneFlag, callingPid, callingUid, (ShortcutPackage) obj);
                                    }
                                });
                            } catch (Throwable th3) {
                                th = th3;
                                while (true) {
                                    break;
                                    break;
                                }
                                throw th;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            while (true) {
                                break;
                                break;
                            }
                            throw th;
                        }
                    }
                    return ShortcutService.this.setReturnedByServer(ret);
                } catch (Throwable th5) {
                    th = th5;
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getShortcuts$0$com-android-server-pm-ShortcutService$LocalService  reason: not valid java name */
        public /* synthetic */ void m5682xf47f55d3(int launcherUserId, String callingPackage, List shortcutIdsF, List locusIdsF, long changedSince, ComponentName componentName, int queryFlags, int userId, ArrayList ret, int cloneFlag, int callingPid, int callingUid, ShortcutPackage p) {
            getShortcutsInnerLocked(launcherUserId, callingPackage, p.getPackageName(), shortcutIdsF, locusIdsF, changedSince, componentName, queryFlags, userId, ret, cloneFlag, callingPid, callingUid);
        }

        private void getShortcutsInnerLocked(int launcherUserId, String callingPackage, String packageName, List<String> shortcutIds, List<LocusId> locusIds, long changedSince, ComponentName componentName, int queryFlags, int userId, ArrayList<ShortcutInfo> ret, int cloneFlag, int callingPid, int callingUid) {
            ArraySet<String> ids = shortcutIds == null ? null : new ArraySet<>(shortcutIds);
            ShortcutUser user = ShortcutService.this.getUserShortcutsLocked(userId);
            ShortcutPackage p = user.getPackageShortcutsIfExists(packageName);
            if (p == null) {
                return;
            }
            boolean canAccessAllShortcuts = ShortcutService.this.canSeeAnyPinnedShortcut(callingPackage, launcherUserId, callingPid, callingUid);
            boolean getPinnedByAnyLauncher = canAccessAllShortcuts && (queryFlags & 1024) != 0;
            Predicate<ShortcutInfo> filter = getFilterFromQuery(ids, locusIds, changedSince, componentName, queryFlags | (getPinnedByAnyLauncher ? 2 : 0), getPinnedByAnyLauncher);
            p.findAll(ret, filter, cloneFlag, callingPackage, launcherUserId, getPinnedByAnyLauncher);
        }

        private Predicate<ShortcutInfo> getFilterFromQuery(final ArraySet<String> ids, List<LocusId> locusIds, final long changedSince, final ComponentName componentName, int queryFlags, final boolean getPinnedByAnyLauncher) {
            final ArraySet arraySet = locusIds == null ? null : new ArraySet(locusIds);
            final boolean matchDynamic = (queryFlags & 1) != 0;
            final boolean matchPinned = (queryFlags & 2) != 0;
            final boolean matchManifest = (queryFlags & 8) != 0;
            final boolean matchCached = (queryFlags & 16) != 0;
            return new Predicate() { // from class: com.android.server.pm.ShortcutService$LocalService$$ExternalSyntheticLambda0
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return ShortcutService.LocalService.lambda$getFilterFromQuery$1(changedSince, ids, arraySet, componentName, matchDynamic, matchPinned, getPinnedByAnyLauncher, matchManifest, matchCached, (ShortcutInfo) obj);
                }
            };
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ boolean lambda$getFilterFromQuery$1(long changedSince, ArraySet ids, ArraySet locIds, ComponentName componentName, boolean matchDynamic, boolean matchPinned, boolean getPinnedByAnyLauncher, boolean matchManifest, boolean matchCached, ShortcutInfo si) {
            if (si.getLastChangedTimestamp() < changedSince) {
                return false;
            }
            if (ids == null || ids.contains(si.getId())) {
                if (locIds == null || locIds.contains(si.getLocusId())) {
                    if (componentName == null || si.getActivity() == null || si.getActivity().equals(componentName)) {
                        if (matchDynamic && si.isDynamic()) {
                            return true;
                        }
                        if ((matchPinned || getPinnedByAnyLauncher) && si.isPinned()) {
                            return true;
                        }
                        if (matchManifest && si.isDeclaredInManifest()) {
                            return true;
                        }
                        return matchCached && si.isCached();
                    }
                    return false;
                }
                return false;
            }
            return false;
        }

        public void getShortcutsAsync(int launcherUserId, String callingPackage, long changedSince, String packageName, List<String> shortcutIds, List<LocusId> locusIds, ComponentName componentName, int queryFlags, int userId, int callingPid, int callingUid, final AndroidFuture<List<ShortcutInfo>> cb) {
            final List<ShortcutInfo> ret = getShortcuts(launcherUserId, callingPackage, changedSince, packageName, shortcutIds, locusIds, componentName, queryFlags, userId, callingPid, callingUid);
            if (shortcutIds != null && packageName != null) {
                if (ret.size() < shortcutIds.size()) {
                    synchronized (ShortcutService.this.mLock) {
                        try {
                            try {
                                ShortcutPackage p = ShortcutService.this.getUserShortcutsLocked(userId).getPackageShortcutsIfExists(packageName);
                                if (p == null) {
                                    cb.complete(ret);
                                    return;
                                }
                                final ArraySet<String> ids = new ArraySet<>(shortcutIds);
                                Objects.requireNonNull(ids);
                                ((List) ret.stream().map(new Function() { // from class: com.android.server.pm.ShortcutService$LocalService$$ExternalSyntheticLambda9
                                    @Override // java.util.function.Function
                                    public final Object apply(Object obj) {
                                        return ((ShortcutInfo) obj).getId();
                                    }
                                }).collect(Collectors.toList())).forEach(new Consumer() { // from class: com.android.server.pm.ShortcutService$LocalService$$ExternalSyntheticLambda10
                                    @Override // java.util.function.Consumer
                                    public final void accept(Object obj) {
                                        ids.remove((String) obj);
                                    }
                                });
                                int flags = 27;
                                if ((queryFlags & 4) != 0) {
                                    flags = 4;
                                } else if ((queryFlags & 2048) != 0) {
                                    flags = 27 & (-17);
                                }
                                final int cloneFlag = flags;
                                p.getShortcutByIdsAsync(ids, new Consumer() { // from class: com.android.server.pm.ShortcutService$LocalService$$ExternalSyntheticLambda11
                                    @Override // java.util.function.Consumer
                                    public final void accept(Object obj) {
                                        ShortcutService.LocalService.lambda$getShortcutsAsync$3(cloneFlag, ret, cb, (List) obj);
                                    }
                                });
                                return;
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            throw th;
                        }
                    }
                }
            }
            cb.complete(ret);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$getShortcutsAsync$3(final int cloneFlag, List ret, AndroidFuture cb, List shortcuts) {
            if (shortcuts != null) {
                Stream map = shortcuts.stream().map(new Function() { // from class: com.android.server.pm.ShortcutService$LocalService$$ExternalSyntheticLambda6
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        ShortcutInfo clone;
                        clone = ((ShortcutInfo) obj).clone(cloneFlag);
                        return clone;
                    }
                });
                Objects.requireNonNull(ret);
                map.forEach(new ShortcutPackage$$ExternalSyntheticLambda46(ret));
            }
            cb.complete(ret);
        }

        public boolean isPinnedByCaller(int launcherUserId, String callingPackage, String packageName, String shortcutId, int userId) {
            boolean z;
            Preconditions.checkStringNotEmpty(packageName, DomainVerificationLegacySettings.ATTR_PACKAGE_NAME);
            Preconditions.checkStringNotEmpty(shortcutId, "shortcutId");
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.throwIfUserLockedL(userId);
                ShortcutService.this.throwIfUserLockedL(launcherUserId);
                ShortcutService.this.getLauncherShortcutsLocked(callingPackage, userId, launcherUserId).attemptToRestoreIfNeededAndSave();
                ShortcutInfo si = getShortcutInfoLocked(launcherUserId, callingPackage, packageName, shortcutId, userId, false);
                z = si != null && si.isPinned();
            }
            return z;
        }

        private ShortcutInfo getShortcutInfoLocked(int launcherUserId, String callingPackage, String packageName, final String shortcutId, int userId, boolean getPinnedByAnyLauncher) {
            Preconditions.checkStringNotEmpty(packageName, DomainVerificationLegacySettings.ATTR_PACKAGE_NAME);
            Preconditions.checkStringNotEmpty(shortcutId, "shortcutId");
            ShortcutService.this.throwIfUserLockedL(userId);
            ShortcutService.this.throwIfUserLockedL(launcherUserId);
            ShortcutPackage p = ShortcutService.this.getUserShortcutsLocked(userId).getPackageShortcutsIfExists(packageName);
            if (p == null) {
                return null;
            }
            ArrayList<ShortcutInfo> list = new ArrayList<>(1);
            p.findAll(list, new Predicate() { // from class: com.android.server.pm.ShortcutService$LocalService$$ExternalSyntheticLambda3
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    boolean equals;
                    equals = shortcutId.equals(((ShortcutInfo) obj).getId());
                    return equals;
                }
            }, 0, callingPackage, launcherUserId, getPinnedByAnyLauncher);
            if (list.size() == 0) {
                return null;
            }
            return list.get(0);
        }

        private void getShortcutInfoAsync(int launcherUserId, String packageName, String shortcutId, int userId, final Consumer<ShortcutInfo> cb) {
            ShortcutPackage p;
            Preconditions.checkStringNotEmpty(packageName, DomainVerificationLegacySettings.ATTR_PACKAGE_NAME);
            Preconditions.checkStringNotEmpty(shortcutId, "shortcutId");
            ShortcutService.this.throwIfUserLockedL(userId);
            ShortcutService.this.throwIfUserLockedL(launcherUserId);
            synchronized (ShortcutService.this.mLock) {
                p = ShortcutService.this.getUserShortcutsLocked(userId).getPackageShortcutsIfExists(packageName);
            }
            if (p == null) {
                cb.accept(null);
            } else {
                p.getShortcutByIdsAsync(Collections.singleton(shortcutId), new Consumer() { // from class: com.android.server.pm.ShortcutService$LocalService$$ExternalSyntheticLambda2
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        cb.accept((shortcuts == null || shortcuts.isEmpty()) ? null : (ShortcutInfo) ((List) obj).get(0));
                    }
                });
            }
        }

        public void pinShortcuts(int launcherUserId, String callingPackage, String packageName, List<String> shortcutIds, int userId) {
            ShortcutPackage sp;
            Preconditions.checkStringNotEmpty(packageName, DomainVerificationLegacySettings.ATTR_PACKAGE_NAME);
            Objects.requireNonNull(shortcutIds, "shortcutIds");
            List<ShortcutInfo> removedShortcuts = null;
            synchronized (ShortcutService.this.mLock) {
                try {
                    try {
                        ShortcutService.this.throwIfUserLockedL(userId);
                        ShortcutService.this.throwIfUserLockedL(launcherUserId);
                        ShortcutLauncher launcher = ShortcutService.this.getLauncherShortcutsLocked(callingPackage, userId, launcherUserId);
                        launcher.attemptToRestoreIfNeededAndSave();
                        ShortcutPackage sp2 = ShortcutService.this.getUserShortcutsLocked(userId).getPackageShortcutsIfExists(packageName);
                        if (sp2 == null) {
                            sp = sp2;
                        } else {
                            List<ShortcutInfo> removedShortcuts2 = new ArrayList<>();
                            try {
                                sp = sp2;
                                sp2.findAll(removedShortcuts2, new Predicate() { // from class: com.android.server.pm.ShortcutService$LocalService$$ExternalSyntheticLambda8
                                    @Override // java.util.function.Predicate
                                    public final boolean test(Object obj) {
                                        return ShortcutService.LocalService.lambda$pinShortcuts$6((ShortcutInfo) obj);
                                    }
                                }, 4, callingPackage, launcherUserId, false);
                                removedShortcuts = removedShortcuts2;
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        }
                        ArraySet<String> oldPinnedIds = launcher.getPinnedShortcutIds(packageName, userId);
                        launcher.pinShortcuts(userId, packageName, shortcutIds, false);
                        if (oldPinnedIds != null && removedShortcuts != null) {
                            for (int i = 0; i < removedShortcuts.size(); i++) {
                                oldPinnedIds.remove(removedShortcuts.get(i).getId());
                            }
                        }
                        List<ShortcutInfo> changedShortcuts = ShortcutService.this.prepareChangedShortcuts(oldPinnedIds, new ArraySet(shortcutIds), removedShortcuts, sp);
                        if (sp != null) {
                            ShortcutService.this.packageShortcutsChanged(sp, changedShortcuts, removedShortcuts);
                        }
                        ShortcutService.this.verifyStates();
                    } catch (Throwable th2) {
                        th = th2;
                    }
                } catch (Throwable th3) {
                    th = th3;
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ boolean lambda$pinShortcuts$6(ShortcutInfo si) {
            return (!si.isVisibleToPublisher() || !si.isPinned() || si.isCached() || si.isDynamic() || si.isDeclaredInManifest()) ? false : true;
        }

        public void cacheShortcuts(int launcherUserId, String callingPackage, String packageName, List<String> shortcutIds, int userId, int cacheFlags) {
            updateCachedShortcutsInternal(launcherUserId, callingPackage, packageName, shortcutIds, userId, cacheFlags, true);
        }

        public void uncacheShortcuts(int launcherUserId, String callingPackage, String packageName, List<String> shortcutIds, int userId, int cacheFlags) {
            updateCachedShortcutsInternal(launcherUserId, callingPackage, packageName, shortcutIds, userId, cacheFlags, false);
        }

        public List<ShortcutManager.ShareShortcutInfo> getShareTargets(String callingPackage, IntentFilter intentFilter, int userId) {
            return ShortcutService.this.getShareTargets(callingPackage, intentFilter, userId).getList();
        }

        public boolean isSharingShortcut(int callingUserId, String callingPackage, String packageName, String shortcutId, int userId, IntentFilter filter) {
            Preconditions.checkStringNotEmpty(callingPackage, "callingPackage");
            Preconditions.checkStringNotEmpty(packageName, DomainVerificationLegacySettings.ATTR_PACKAGE_NAME);
            Preconditions.checkStringNotEmpty(shortcutId, "shortcutId");
            return ShortcutService.this.isSharingShortcut(callingUserId, callingPackage, packageName, shortcutId, userId, filter);
        }

        private void updateCachedShortcutsInternal(int launcherUserId, String callingPackage, String packageName, List<String> shortcutIds, int userId, int cacheFlags, boolean doCache) {
            int idSize;
            Preconditions.checkStringNotEmpty(packageName, DomainVerificationLegacySettings.ATTR_PACKAGE_NAME);
            Objects.requireNonNull(shortcutIds, "shortcutIds");
            Preconditions.checkState((1610629120 & cacheFlags) != 0, "invalid cacheFlags");
            List<ShortcutInfo> changedShortcuts = null;
            List<ShortcutInfo> removedShortcuts = null;
            synchronized (ShortcutService.this.mLock) {
                try {
                    try {
                        ShortcutService.this.throwIfUserLockedL(userId);
                        ShortcutService.this.throwIfUserLockedL(launcherUserId);
                        int idSize2 = shortcutIds.size();
                        ShortcutPackage sp = ShortcutService.this.getUserShortcutsLocked(userId).getPackageShortcutsIfExists(packageName);
                        if (idSize2 != 0 && sp != null) {
                            int i = 0;
                            while (i < idSize2) {
                                String id = (String) Preconditions.checkStringNotEmpty(shortcutIds.get(i));
                                ShortcutInfo si = sp.findShortcutById(id);
                                if (si == null) {
                                    idSize = idSize2;
                                } else if (doCache == si.hasFlags(cacheFlags)) {
                                    idSize = idSize2;
                                } else if (doCache) {
                                    if (si.isLongLived()) {
                                        si.addFlags(cacheFlags);
                                        if (changedShortcuts != null) {
                                            idSize = idSize2;
                                        } else {
                                            idSize = idSize2;
                                            changedShortcuts = new ArrayList<>(1);
                                        }
                                        changedShortcuts.add(si);
                                    } else {
                                        idSize = idSize2;
                                        Log.w(ShortcutService.TAG, "Only long lived shortcuts can get cached. Ignoring id " + si.getId());
                                    }
                                } else {
                                    idSize = idSize2;
                                    ShortcutInfo removed = null;
                                    si.clearFlags(cacheFlags);
                                    if (!si.isDynamic() && !si.isCached()) {
                                        removed = sp.deleteLongLivedWithId(id, true);
                                    }
                                    if (removed == null) {
                                        if (changedShortcuts == null) {
                                            changedShortcuts = new ArrayList<>(1);
                                        }
                                        changedShortcuts.add(si);
                                    } else {
                                        if (removedShortcuts == null) {
                                            removedShortcuts = new ArrayList<>(1);
                                        }
                                        removedShortcuts.add(removed);
                                    }
                                }
                                i++;
                                idSize2 = idSize;
                            }
                            ShortcutService.this.packageShortcutsChanged(sp, changedShortcuts, removedShortcuts);
                            ShortcutService.this.verifyStates();
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

        public Intent[] createShortcutIntents(int launcherUserId, String callingPackage, String packageName, String shortcutId, int userId, int callingPid, int callingUid) {
            Preconditions.checkStringNotEmpty(packageName, "packageName can't be empty");
            Preconditions.checkStringNotEmpty(shortcutId, "shortcutId can't be empty");
            synchronized (ShortcutService.this.mLock) {
                try {
                    try {
                        ShortcutService.this.throwIfUserLockedL(userId);
                        ShortcutService.this.throwIfUserLockedL(launcherUserId);
                        ShortcutService.this.getLauncherShortcutsLocked(callingPackage, userId, launcherUserId).attemptToRestoreIfNeededAndSave();
                        boolean getPinnedByAnyLauncher = ShortcutService.this.canSeeAnyPinnedShortcut(callingPackage, launcherUserId, callingPid, callingUid);
                        ShortcutInfo si = getShortcutInfoLocked(launcherUserId, callingPackage, packageName, shortcutId, userId, getPinnedByAnyLauncher);
                        if (si != null && si.isEnabled() && (si.isAlive() || getPinnedByAnyLauncher)) {
                            return si.getIntents();
                        }
                        Log.e(ShortcutService.TAG, "Shortcut " + shortcutId + " does not exist or disabled");
                        return null;
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }

        public void createShortcutIntentsAsync(int launcherUserId, String callingPackage, String packageName, String shortcutId, int userId, int callingPid, int callingUid, final AndroidFuture<Intent[]> cb) {
            Preconditions.checkStringNotEmpty(packageName, "packageName can't be empty");
            Preconditions.checkStringNotEmpty(shortcutId, "shortcutId can't be empty");
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.throwIfUserLockedL(userId);
                ShortcutService.this.throwIfUserLockedL(launcherUserId);
                ShortcutService.this.getLauncherShortcutsLocked(callingPackage, userId, launcherUserId).attemptToRestoreIfNeededAndSave();
                boolean getPinnedByAnyLauncher = ShortcutService.this.canSeeAnyPinnedShortcut(callingPackage, launcherUserId, callingPid, callingUid);
                ShortcutInfo si = getShortcutInfoLocked(launcherUserId, callingPackage, packageName, shortcutId, userId, getPinnedByAnyLauncher);
                if (si != null) {
                    if (si.isEnabled() && (si.isAlive() || getPinnedByAnyLauncher)) {
                        cb.complete(si.getIntents());
                        return;
                    }
                    Log.e(ShortcutService.TAG, "Shortcut " + shortcutId + " does not exist or disabled");
                    cb.complete((Object) null);
                    return;
                }
                getShortcutInfoAsync(launcherUserId, packageName, shortcutId, userId, new Consumer() { // from class: com.android.server.pm.ShortcutService$LocalService$$ExternalSyntheticLambda7
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        cb.complete(si == null ? null : ((ShortcutInfo) obj).getIntents());
                    }
                });
            }
        }

        public void addListener(ShortcutServiceInternal.ShortcutChangeListener listener) {
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.mListeners.add((ShortcutServiceInternal.ShortcutChangeListener) Objects.requireNonNull(listener));
            }
        }

        public void addShortcutChangeCallback(LauncherApps.ShortcutChangeCallback callback) {
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.mShortcutChangeCallbacks.add((LauncherApps.ShortcutChangeCallback) Objects.requireNonNull(callback));
            }
        }

        public int getShortcutIconResId(int launcherUserId, String callingPackage, String packageName, String shortcutId, int userId) {
            Objects.requireNonNull(callingPackage, "callingPackage");
            Objects.requireNonNull(packageName, DomainVerificationLegacySettings.ATTR_PACKAGE_NAME);
            Objects.requireNonNull(shortcutId, "shortcutId");
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.throwIfUserLockedL(userId);
                ShortcutService.this.throwIfUserLockedL(launcherUserId);
                ShortcutService.this.getLauncherShortcutsLocked(callingPackage, userId, launcherUserId).attemptToRestoreIfNeededAndSave();
                ShortcutPackage p = ShortcutService.this.getUserShortcutsLocked(userId).getPackageShortcutsIfExists(packageName);
                int i = 0;
                if (p == null) {
                    return 0;
                }
                ShortcutInfo shortcutInfo = p.findShortcutById(shortcutId);
                if (shortcutInfo != null && shortcutInfo.hasIconResource()) {
                    i = shortcutInfo.getIconResourceId();
                }
                return i;
            }
        }

        public String getShortcutStartingThemeResName(int launcherUserId, String callingPackage, String packageName, String shortcutId, int userId) {
            Objects.requireNonNull(callingPackage, "callingPackage");
            Objects.requireNonNull(packageName, DomainVerificationLegacySettings.ATTR_PACKAGE_NAME);
            Objects.requireNonNull(shortcutId, "shortcutId");
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.throwIfUserLockedL(userId);
                ShortcutService.this.throwIfUserLockedL(launcherUserId);
                ShortcutService.this.getLauncherShortcutsLocked(callingPackage, userId, launcherUserId).attemptToRestoreIfNeededAndSave();
                ShortcutPackage p = ShortcutService.this.getUserShortcutsLocked(userId).getPackageShortcutsIfExists(packageName);
                if (p == null) {
                    return null;
                }
                ShortcutInfo shortcutInfo = p.findShortcutById(shortcutId);
                return shortcutInfo != null ? shortcutInfo.getStartingThemeResName() : null;
            }
        }

        public ParcelFileDescriptor getShortcutIconFd(int launcherUserId, String callingPackage, String packageName, String shortcutId, int userId) {
            Objects.requireNonNull(callingPackage, "callingPackage");
            Objects.requireNonNull(packageName, DomainVerificationLegacySettings.ATTR_PACKAGE_NAME);
            Objects.requireNonNull(shortcutId, "shortcutId");
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.throwIfUserLockedL(userId);
                ShortcutService.this.throwIfUserLockedL(launcherUserId);
                ShortcutService.this.getLauncherShortcutsLocked(callingPackage, userId, launcherUserId).attemptToRestoreIfNeededAndSave();
                ShortcutPackage p = ShortcutService.this.getUserShortcutsLocked(userId).getPackageShortcutsIfExists(packageName);
                if (p == null) {
                    return null;
                }
                ShortcutInfo shortcutInfo = p.findShortcutById(shortcutId);
                if (shortcutInfo == null) {
                    return null;
                }
                return getShortcutIconParcelFileDescriptor(p, shortcutInfo);
            }
        }

        public void getShortcutIconFdAsync(int launcherUserId, String callingPackage, String packageName, String shortcutId, int userId, final AndroidFuture<ParcelFileDescriptor> cb) {
            Objects.requireNonNull(callingPackage, "callingPackage");
            Objects.requireNonNull(packageName, DomainVerificationLegacySettings.ATTR_PACKAGE_NAME);
            Objects.requireNonNull(shortcutId, "shortcutId");
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.throwIfUserLockedL(userId);
                ShortcutService.this.throwIfUserLockedL(launcherUserId);
                ShortcutService.this.getLauncherShortcutsLocked(callingPackage, userId, launcherUserId).attemptToRestoreIfNeededAndSave();
                final ShortcutPackage p = ShortcutService.this.getUserShortcutsLocked(userId).getPackageShortcutsIfExists(packageName);
                if (p == null) {
                    cb.complete((Object) null);
                    return;
                }
                ShortcutInfo shortcutInfo = p.findShortcutById(shortcutId);
                if (shortcutInfo != null) {
                    cb.complete(getShortcutIconParcelFileDescriptor(p, shortcutInfo));
                } else {
                    getShortcutInfoAsync(launcherUserId, packageName, shortcutId, userId, new Consumer() { // from class: com.android.server.pm.ShortcutService$LocalService$$ExternalSyntheticLambda4
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ShortcutService.LocalService.this.m5680xa0651039(cb, p, (ShortcutInfo) obj);
                        }
                    });
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getShortcutIconFdAsync$8$com-android-server-pm-ShortcutService$LocalService  reason: not valid java name */
        public /* synthetic */ void m5680xa0651039(AndroidFuture cb, ShortcutPackage p, ShortcutInfo si) {
            cb.complete(getShortcutIconParcelFileDescriptor(p, si));
        }

        private ParcelFileDescriptor getShortcutIconParcelFileDescriptor(ShortcutPackage p, ShortcutInfo shortcutInfo) {
            if (p == null || shortcutInfo == null || !shortcutInfo.hasIconFile()) {
                return null;
            }
            String path = p.getBitmapPathMayWait(shortcutInfo);
            if (path == null) {
                Slog.w(ShortcutService.TAG, "null bitmap detected in getShortcutIconFd()");
                return null;
            }
            try {
                return ParcelFileDescriptor.open(new File(path), 268435456);
            } catch (FileNotFoundException e) {
                Slog.e(ShortcutService.TAG, "Icon file not found: " + path);
                return null;
            }
        }

        public String getShortcutIconUri(int launcherUserId, String launcherPackage, String packageName, String shortcutId, int userId) {
            Objects.requireNonNull(launcherPackage, "launcherPackage");
            Objects.requireNonNull(packageName, DomainVerificationLegacySettings.ATTR_PACKAGE_NAME);
            Objects.requireNonNull(shortcutId, "shortcutId");
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.throwIfUserLockedL(userId);
                ShortcutService.this.throwIfUserLockedL(launcherUserId);
                ShortcutService.this.getLauncherShortcutsLocked(launcherPackage, userId, launcherUserId).attemptToRestoreIfNeededAndSave();
                ShortcutPackage p = ShortcutService.this.getUserShortcutsLocked(userId).getPackageShortcutsIfExists(packageName);
                if (p == null) {
                    return null;
                }
                ShortcutInfo shortcutInfo = p.findShortcutById(shortcutId);
                if (shortcutInfo == null) {
                    return null;
                }
                return getShortcutIconUriInternal(launcherUserId, launcherPackage, packageName, shortcutInfo, userId);
            }
        }

        public void getShortcutIconUriAsync(final int launcherUserId, final String launcherPackage, final String packageName, String shortcutId, final int userId, final AndroidFuture<String> cb) {
            Objects.requireNonNull(launcherPackage, "launcherPackage");
            Objects.requireNonNull(packageName, DomainVerificationLegacySettings.ATTR_PACKAGE_NAME);
            Objects.requireNonNull(shortcutId, "shortcutId");
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.throwIfUserLockedL(userId);
                ShortcutService.this.throwIfUserLockedL(launcherUserId);
                ShortcutService.this.getLauncherShortcutsLocked(launcherPackage, userId, launcherUserId).attemptToRestoreIfNeededAndSave();
                ShortcutPackage p = ShortcutService.this.getUserShortcutsLocked(userId).getPackageShortcutsIfExists(packageName);
                if (p == null) {
                    cb.complete((Object) null);
                    return;
                }
                ShortcutInfo shortcutInfo = p.findShortcutById(shortcutId);
                if (shortcutInfo != null) {
                    cb.complete(getShortcutIconUriInternal(launcherUserId, launcherPackage, packageName, shortcutInfo, userId));
                } else {
                    getShortcutInfoAsync(launcherUserId, packageName, shortcutId, userId, new Consumer() { // from class: com.android.server.pm.ShortcutService$LocalService$$ExternalSyntheticLambda5
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ShortcutService.LocalService.this.m5681xa9955596(cb, launcherUserId, launcherPackage, packageName, userId, (ShortcutInfo) obj);
                        }
                    });
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getShortcutIconUriAsync$9$com-android-server-pm-ShortcutService$LocalService  reason: not valid java name */
        public /* synthetic */ void m5681xa9955596(AndroidFuture cb, int launcherUserId, String launcherPackage, String packageName, int userId, ShortcutInfo si) {
            cb.complete(getShortcutIconUriInternal(launcherUserId, launcherPackage, packageName, si, userId));
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3618=5] */
        private String getShortcutIconUriInternal(int launcherUserId, String launcherPackage, String packageName, ShortcutInfo shortcutInfo, int userId) {
            if (!shortcutInfo.hasIconUri()) {
                return null;
            }
            String uri = shortcutInfo.getIconUri();
            if (uri == null) {
                Slog.w(ShortcutService.TAG, "null uri detected in getShortcutIconUri()");
                return null;
            }
            long token = Binder.clearCallingIdentity();
            try {
                try {
                    try {
                        int packageUid = ShortcutService.this.mPackageManagerInternal.getPackageUid(packageName, 268435456L, userId);
                        ShortcutService.this.mUriGrantsManager.grantUriPermissionFromOwner(ShortcutService.this.mUriPermissionOwner, packageUid, launcherPackage, Uri.parse(uri), 1, userId, launcherUserId);
                        Binder.restoreCallingIdentity(token);
                        return uri;
                    } catch (Exception e) {
                        e = e;
                        try {
                            Slog.e(ShortcutService.TAG, "Failed to grant uri access to " + launcherPackage + " for " + uri, e);
                            Binder.restoreCallingIdentity(token);
                            return null;
                        } catch (Throwable th) {
                            th = th;
                            Binder.restoreCallingIdentity(token);
                            throw th;
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    Binder.restoreCallingIdentity(token);
                    throw th;
                }
            } catch (Exception e2) {
                e = e2;
            } catch (Throwable th3) {
                th = th3;
                Binder.restoreCallingIdentity(token);
                throw th;
            }
        }

        public boolean hasShortcutHostPermission(int launcherUserId, String callingPackage, int callingPid, int callingUid) {
            return ShortcutService.this.hasShortcutHostPermission(callingPackage, launcherUserId, callingPid, callingUid);
        }

        public void setShortcutHostPackage(String type, String packageName, int userId) {
            ShortcutService.this.setShortcutHostPackage(type, packageName, userId);
        }

        public boolean requestPinAppWidget(String callingPackage, AppWidgetProviderInfo appWidget, Bundle extras, IntentSender resultIntent, int userId) {
            Objects.requireNonNull(appWidget);
            return ShortcutService.this.requestPinItem(callingPackage, userId, null, appWidget, extras, resultIntent);
        }

        public boolean isRequestPinItemSupported(int callingUserId, int requestType) {
            return ShortcutService.this.isRequestPinItemSupported(callingUserId, requestType);
        }

        public boolean isForegroundDefaultLauncher(String callingPackage, int callingUid) {
            Objects.requireNonNull(callingPackage);
            int userId = UserHandle.getUserId(callingUid);
            String defaultLauncher = ShortcutService.this.getDefaultLauncher(userId);
            if (defaultLauncher == null || !callingPackage.equals(defaultLauncher)) {
                return false;
            }
            synchronized (ShortcutService.this.mLock) {
                if (!ShortcutService.this.isUidForegroundLocked(callingUid)) {
                    return false;
                }
                return true;
            }
        }
    }

    void handleLocaleChanged() {
        scheduleSaveBaseState();
        synchronized (this.mLock) {
            long token = injectClearCallingIdentity();
            forEachLoadedUserLocked(new Consumer() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda17
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((ShortcutUser) obj).detectLocaleChange();
                }
            });
            injectRestoreCallingIdentity(token);
        }
    }

    void checkPackageChanges(int ownerUserId) {
        if (injectIsSafeModeEnabled()) {
            Slog.i(TAG, "Safe mode, skipping checkPackageChanges()");
            return;
        }
        long start = getStatStartTime();
        try {
            final ArrayList<ShortcutUser.PackageWithUser> gonePackages = new ArrayList<>();
            synchronized (this.mLock) {
                ShortcutUser user = getUserShortcutsLocked(ownerUserId);
                user.forAllPackageItems(new Consumer() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda28
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ShortcutService.this.m5669xc00a3c3a(gonePackages, (ShortcutPackageItem) obj);
                    }
                });
                if (gonePackages.size() > 0) {
                    for (int i = gonePackages.size() - 1; i >= 0; i--) {
                        ShortcutUser.PackageWithUser pu = gonePackages.get(i);
                        cleanUpPackageLocked(pu.packageName, ownerUserId, pu.userId, false);
                    }
                }
                rescanUpdatedPackagesLocked(ownerUserId, user.getLastAppScanTime());
            }
            logDurationStat(8, start);
            verifyStates();
        } catch (Throwable th) {
            logDurationStat(8, start);
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$checkPackageChanges$14$com-android-server-pm-ShortcutService  reason: not valid java name */
    public /* synthetic */ void m5669xc00a3c3a(ArrayList gonePackages, ShortcutPackageItem spi) {
        if (!spi.getPackageInfo().isShadow() && !isPackageInstalled(spi.getPackageName(), spi.getPackageUserId())) {
            gonePackages.add(ShortcutUser.PackageWithUser.of(spi));
        }
    }

    private void rescanUpdatedPackagesLocked(final int userId, long lastScanTime) {
        final ShortcutUser user = getUserShortcutsLocked(userId);
        long now = injectCurrentTimeMillis();
        boolean afterOta = !injectBuildFingerprint().equals(user.getLastAppScanOsFingerprint());
        forUpdatedPackages(userId, lastScanTime, afterOta, new Consumer() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda23
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ShortcutService.this.m5675xe5f4ef3e(user, userId, (ApplicationInfo) obj);
            }
        });
        user.setLastAppScanTime(now);
        user.setLastAppScanOsFingerprint(injectBuildFingerprint());
        scheduleSaveUser(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$rescanUpdatedPackagesLocked$15$com-android-server-pm-ShortcutService  reason: not valid java name */
    public /* synthetic */ void m5675xe5f4ef3e(ShortcutUser user, int userId, ApplicationInfo ai) {
        user.attemptToRestoreIfNeededAndSave(this, ai.packageName, userId);
        user.rescanPackageIfNeeded(ai.packageName, true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePackageAdded(String packageName, int userId) {
        synchronized (this.mLock) {
            ShortcutUser user = getUserShortcutsLocked(userId);
            user.attemptToRestoreIfNeededAndSave(this, packageName, userId);
            user.rescanPackageIfNeeded(packageName, true);
        }
        verifyStates();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePackageUpdateFinished(String packageName, int userId) {
        synchronized (this.mLock) {
            ShortcutUser user = getUserShortcutsLocked(userId);
            user.attemptToRestoreIfNeededAndSave(this, packageName, userId);
            if (isPackageInstalled(packageName, userId)) {
                user.rescanPackageIfNeeded(packageName, true);
            }
        }
        verifyStates();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePackageRemoved(String packageName, int packageUserId) {
        cleanUpPackageForAllLoadedUsers(packageName, packageUserId, false);
        verifyStates();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePackageDataCleared(String packageName, int packageUserId) {
        cleanUpPackageForAllLoadedUsers(packageName, packageUserId, true);
        verifyStates();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePackageChanged(String packageName, int packageUserId) {
        if (!isPackageInstalled(packageName, packageUserId)) {
            handlePackageRemoved(packageName, packageUserId);
            return;
        }
        synchronized (this.mLock) {
            ShortcutUser user = getUserShortcutsLocked(packageUserId);
            user.rescanPackageIfNeeded(packageName, true);
        }
        verifyStates();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final PackageInfo getPackageInfoWithSignatures(String packageName, int userId) {
        return getPackageInfo(packageName, userId, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final PackageInfo getPackageInfo(String packageName, int userId) {
        return getPackageInfo(packageName, userId, false);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3965=4] */
    /* JADX INFO: Access modifiers changed from: package-private */
    public int injectGetPackageUid(String packageName, int userId) {
        long token = injectClearCallingIdentity();
        try {
            return this.mIPackageManager.getPackageUid(packageName, 795136L, userId);
        } catch (RemoteException e) {
            Slog.wtf(TAG, "RemoteException", e);
            return -1;
        } finally {
            injectRestoreCallingIdentity(token);
        }
    }

    final PackageInfo getPackageInfo(String packageName, int userId, boolean getSignatures) {
        return isInstalledOrNull(injectPackageInfoWithUninstalled(packageName, userId, getSignatures));
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[INVOKE]}, finally: {[INVOKE, INVOKE, MOVE, INVOKE, IF] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3997=4] */
    PackageInfo injectPackageInfoWithUninstalled(String packageName, int userId, boolean getSignatures) {
        long start = getStatStartTime();
        long token = injectClearCallingIdentity();
        try {
            try {
                PackageInfo packageInfo = this.mIPackageManager.getPackageInfo(packageName, PACKAGE_MATCH_FLAGS | (getSignatures ? 134217728 : 0), userId);
                injectRestoreCallingIdentity(token);
                logDurationStat(getSignatures ? 2 : 1, start);
                return packageInfo;
            } catch (RemoteException e) {
                Slog.wtf(TAG, "RemoteException", e);
                injectRestoreCallingIdentity(token);
                logDurationStat(getSignatures ? 2 : 1, start);
                return null;
            }
        } catch (Throwable th) {
            injectRestoreCallingIdentity(token);
            logDurationStat(getSignatures ? 2 : 1, start);
            throw th;
        }
    }

    final ApplicationInfo getApplicationInfo(String packageName, int userId) {
        return isInstalledOrNull(injectApplicationInfoWithUninstalled(packageName, userId));
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4030=4] */
    ApplicationInfo injectApplicationInfoWithUninstalled(String packageName, int userId) {
        long start = getStatStartTime();
        long token = injectClearCallingIdentity();
        try {
            return this.mIPackageManager.getApplicationInfo(packageName, 795136L, userId);
        } catch (RemoteException e) {
            Slog.wtf(TAG, "RemoteException", e);
            return null;
        } finally {
            injectRestoreCallingIdentity(token);
            logDurationStat(3, start);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final ActivityInfo getActivityInfoWithMetadata(ComponentName activity, int userId) {
        return isInstalledOrNull(injectGetActivityInfoWithMetadataWithUninstalled(activity, userId));
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4062=4] */
    ActivityInfo injectGetActivityInfoWithMetadataWithUninstalled(ComponentName activity, int userId) {
        long start = getStatStartTime();
        long token = injectClearCallingIdentity();
        try {
            return this.mIPackageManager.getActivityInfo(activity, 795264L, userId);
        } catch (RemoteException e) {
            Slog.wtf(TAG, "RemoteException", e);
            return null;
        } finally {
            injectRestoreCallingIdentity(token);
            logDurationStat(6, start);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4087=4] */
    final List<PackageInfo> getInstalledPackages(int userId) {
        long start = getStatStartTime();
        long token = injectClearCallingIdentity();
        try {
            List<PackageInfo> all = injectGetPackagesWithUninstalled(userId);
            all.removeIf(PACKAGE_NOT_INSTALLED);
            return all;
        } catch (RemoteException e) {
            Slog.wtf(TAG, "RemoteException", e);
            return null;
        } finally {
            injectRestoreCallingIdentity(token);
            logDurationStat(7, start);
        }
    }

    List<PackageInfo> injectGetPackagesWithUninstalled(int userId) throws RemoteException {
        ParceledListSlice<PackageInfo> parceledList = this.mIPackageManager.getInstalledPackages(795136L, userId);
        if (parceledList == null) {
            return Collections.emptyList();
        }
        return parceledList.getList();
    }

    private void forUpdatedPackages(int userId, long lastScanTime, boolean afterOta, Consumer<ApplicationInfo> callback) {
        List<PackageInfo> list = getInstalledPackages(userId);
        for (int i = list.size() - 1; i >= 0; i--) {
            PackageInfo pi = list.get(i);
            if (afterOta || pi.lastUpdateTime >= lastScanTime) {
                callback.accept(pi.applicationInfo);
            }
        }
    }

    private boolean isApplicationFlagSet(String packageName, int userId, int flags) {
        ApplicationInfo ai = injectApplicationInfoWithUninstalled(packageName, userId);
        return ai != null && (ai.flags & flags) == flags;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4153=4] */
    private boolean isEnabled(ActivityInfo ai, int userId) {
        if (ai == null) {
            return false;
        }
        long token = injectClearCallingIdentity();
        try {
            try {
                int enabledFlag = this.mIPackageManager.getComponentEnabledSetting(ai.getComponentName(), userId);
                injectRestoreCallingIdentity(token);
                return (enabledFlag == 0 && ai.enabled) || enabledFlag == 1;
            } catch (RemoteException e) {
                Slog.wtf(TAG, "RemoteException", e);
                injectRestoreCallingIdentity(token);
                return false;
            }
        } catch (Throwable th) {
            injectRestoreCallingIdentity(token);
            throw th;
        }
    }

    private static boolean isSystem(ActivityInfo ai) {
        return ai != null && isSystem(ai.applicationInfo);
    }

    private static boolean isSystem(ApplicationInfo ai) {
        return (ai == null || (ai.flags & 129) == 0) ? false : true;
    }

    private static boolean isInstalled(ApplicationInfo ai) {
        return (ai == null || !ai.enabled || (ai.flags & 8388608) == 0) ? false : true;
    }

    private static boolean isEphemeralApp(ApplicationInfo ai) {
        return ai != null && ai.isInstantApp();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isInstalled(PackageInfo pi) {
        return pi != null && isInstalled(pi.applicationInfo);
    }

    private static boolean isInstalled(ActivityInfo ai) {
        return ai != null && isInstalled(ai.applicationInfo);
    }

    private static ApplicationInfo isInstalledOrNull(ApplicationInfo ai) {
        if (isInstalled(ai)) {
            return ai;
        }
        return null;
    }

    private static PackageInfo isInstalledOrNull(PackageInfo pi) {
        if (isInstalled(pi)) {
            return pi;
        }
        return null;
    }

    private static ActivityInfo isInstalledOrNull(ActivityInfo ai) {
        if (isInstalled(ai)) {
            return ai;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPackageInstalled(String packageName, int userId) {
        return getApplicationInfo(packageName, userId) != null;
    }

    boolean isEphemeralApp(String packageName, int userId) {
        return isEphemeralApp(getApplicationInfo(packageName, userId));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public XmlResourceParser injectXmlMetaData(ActivityInfo activityInfo, String key) {
        return activityInfo.loadXmlMetaData(this.mContext.getPackageManager(), key);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4224=4] */
    /* JADX INFO: Access modifiers changed from: package-private */
    public Resources injectGetResourcesForApplicationAsUser(String packageName, int userId) {
        long start = getStatStartTime();
        long token = injectClearCallingIdentity();
        try {
            return this.mContext.createContextAsUser(UserHandle.of(userId), 0).getPackageManager().getResourcesForApplication(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "Resources of package " + packageName + " for user " + userId + " not found");
            return null;
        } finally {
            injectRestoreCallingIdentity(token);
            logDurationStat(9, start);
        }
    }

    private Intent getMainActivityIntent() {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory(LAUNCHER_INTENT_CATEGORY);
        return intent;
    }

    List<ResolveInfo> queryActivities(Intent baseIntent, String packageName, ComponentName activity, int userId) {
        baseIntent.setPackage((String) Objects.requireNonNull(packageName));
        if (activity != null) {
            baseIntent.setComponent(activity);
        }
        return queryActivities(baseIntent, userId, true);
    }

    List<ResolveInfo> queryActivities(Intent intent, final int userId, boolean exportedOnly) {
        long token = injectClearCallingIdentity();
        try {
            List<ResolveInfo> resolved = this.mContext.getPackageManager().queryIntentActivitiesAsUser(intent, PACKAGE_MATCH_FLAGS, userId);
            if (resolved == null || resolved.size() == 0) {
                return EMPTY_RESOLVE_INFO;
            }
            resolved.removeIf(ACTIVITY_NOT_INSTALLED);
            resolved.removeIf(new Predicate() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda13
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return ShortcutService.this.m5674lambda$queryActivities$16$comandroidserverpmShortcutService(userId, (ResolveInfo) obj);
                }
            });
            if (exportedOnly) {
                resolved.removeIf(ACTIVITY_NOT_EXPORTED);
            }
            return resolved;
        } finally {
            injectRestoreCallingIdentity(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$queryActivities$16$com-android-server-pm-ShortcutService  reason: not valid java name */
    public /* synthetic */ boolean m5674lambda$queryActivities$16$comandroidserverpmShortcutService(int userId, ResolveInfo ri) {
        ActivityInfo ai = ri.activityInfo;
        return (isSystem(ai) || isEnabled(ai, userId)) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ComponentName injectGetDefaultMainActivity(String packageName, int userId) {
        long start = getStatStartTime();
        try {
            List<ResolveInfo> resolved = queryActivities(getMainActivityIntent(), packageName, null, userId);
            return resolved.size() != 0 ? resolved.get(0).activityInfo.getComponentName() : null;
        } finally {
            logDurationStat(11, start);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4311=4] */
    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean injectIsMainActivity(ComponentName activity, int userId) {
        long start = getStatStartTime();
        try {
            if (activity == null) {
                wtf("null activity detected");
                return false;
            } else if (DUMMY_MAIN_ACTIVITY.equals(activity.getClassName())) {
                return true;
            } else {
                List<ResolveInfo> resolved = queryActivities(getMainActivityIntent(), activity.getPackageName(), activity, userId);
                return resolved.size() > 0;
            }
        } finally {
            logDurationStat(12, start);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ComponentName getDummyMainActivity(String packageName) {
        return new ComponentName(packageName, DUMMY_MAIN_ACTIVITY);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDummyMainActivity(ComponentName name) {
        return name != null && DUMMY_MAIN_ACTIVITY.equals(name.getClassName());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<ResolveInfo> injectGetMainActivities(String packageName, int userId) {
        long start = getStatStartTime();
        try {
            return queryActivities(getMainActivityIntent(), packageName, null, userId);
        } finally {
            logDurationStat(12, start);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean injectIsActivityEnabledAndExported(ComponentName activity, int userId) {
        long start = getStatStartTime();
        try {
            return queryActivities(new Intent(), activity.getPackageName(), activity, userId).size() > 0;
        } finally {
            logDurationStat(13, start);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ComponentName injectGetPinConfirmationActivity(String launcherPackageName, int launcherUserId, int requestType) {
        String action;
        Objects.requireNonNull(launcherPackageName);
        if (requestType == 1) {
            action = "android.content.pm.action.CONFIRM_PIN_SHORTCUT";
        } else {
            action = "android.content.pm.action.CONFIRM_PIN_APPWIDGET";
        }
        Intent confirmIntent = new Intent(action).setPackage(launcherPackageName);
        List<ResolveInfo> candidates = queryActivities(confirmIntent, launcherUserId, false);
        Iterator<ResolveInfo> it = candidates.iterator();
        if (it.hasNext()) {
            ResolveInfo ri = it.next();
            return ri.activityInfo.getComponentName();
        }
        return null;
    }

    boolean injectIsSafeModeEnabled() {
        long token = injectClearCallingIdentity();
        try {
            return IWindowManager.Stub.asInterface(ServiceManager.getService("window")).isSafeModeEnabled();
        } catch (RemoteException e) {
            return false;
        } finally {
            injectRestoreCallingIdentity(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getParentOrSelfUserId(int userId) {
        return this.mUserManagerInternal.getProfileParentId(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void injectSendIntentSender(IntentSender intentSender, Intent extras) {
        if (intentSender == null) {
            return;
        }
        try {
            intentSender.sendIntent(this.mContext, 0, extras, null, null);
        } catch (IntentSender.SendIntentException e) {
            Slog.w(TAG, "sendIntent failed().", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldBackupApp(String packageName, int userId) {
        return isApplicationFlagSet(packageName, userId, 32768);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean shouldBackupApp(PackageInfo pi) {
        return (pi.applicationInfo.flags & 32768) != 0;
    }

    public byte[] getBackupPayload(int userId) {
        enforceSystem();
        synchronized (this.mLock) {
            if (!isUserUnlockedL(userId)) {
                wtf("Can't backup: user " + userId + " is locked or not running");
                return null;
            }
            ShortcutUser user = getUserShortcutsLocked(userId);
            if (user == null) {
                wtf("Can't backup: user not found: id=" + userId);
                return null;
            }
            user.forAllPackageItems(new Consumer() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((ShortcutPackageItem) obj).refreshPackageSignatureAndSave();
                }
            });
            user.forAllPackages(new Consumer() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda2
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((ShortcutPackage) obj).rescanPackageIfNeeded(false, true);
                }
            });
            user.forAllLaunchers(new Consumer() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda3
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((ShortcutLauncher) obj).ensurePackageInfo();
                }
            });
            scheduleSaveUser(userId);
            saveDirtyInfo();
            ByteArrayOutputStream os = new ByteArrayOutputStream(32768);
            try {
                saveUserInternalLocked(userId, os, true);
                byte[] payload = os.toByteArray();
                this.mShortcutDumpFiles.save("backup-1-payload.txt", payload);
                return payload;
            } catch (IOException | XmlPullParserException e) {
                Slog.w(TAG, "Backup failed.", e);
                return null;
            }
        }
    }

    public void applyRestore(byte[] payload, int userId) {
        enforceSystem();
        synchronized (this.mLock) {
            if (!isUserUnlockedL(userId)) {
                wtf("Can't restore: user " + userId + " is locked or not running");
                return;
            }
            this.mShortcutDumpFiles.save("restore-0-start.txt", new Consumer() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda9
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ShortcutService.this.m5667lambda$applyRestore$20$comandroidserverpmShortcutService((PrintWriter) obj);
                }
            });
            this.mShortcutDumpFiles.save("restore-1-payload.xml", payload);
            ByteArrayInputStream is = new ByteArrayInputStream(payload);
            try {
                ShortcutUser restored = loadUserInternal(userId, is, true);
                this.mShortcutDumpFiles.save("restore-2.txt", new Consumer() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda10
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ShortcutService.this.dumpInner((PrintWriter) obj);
                    }
                });
                getUserShortcutsLocked(userId).mergeRestoredFile(restored);
                this.mShortcutDumpFiles.save("restore-3.txt", new Consumer() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda10
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ShortcutService.this.dumpInner((PrintWriter) obj);
                    }
                });
                rescanUpdatedPackagesLocked(userId, 0L);
                this.mShortcutDumpFiles.save("restore-4.txt", new Consumer() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda10
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ShortcutService.this.dumpInner((PrintWriter) obj);
                    }
                });
                this.mShortcutDumpFiles.save("restore-5-finish.txt", new Consumer() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda11
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ShortcutService.this.m5668lambda$applyRestore$21$comandroidserverpmShortcutService((PrintWriter) obj);
                    }
                });
                saveUserLocked(userId);
            } catch (InvalidFileFormatException | IOException | XmlPullParserException e) {
                Slog.w(TAG, "Restoration failed.", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$applyRestore$20$com-android-server-pm-ShortcutService  reason: not valid java name */
    public /* synthetic */ void m5667lambda$applyRestore$20$comandroidserverpmShortcutService(PrintWriter pw) {
        pw.print("Start time: ");
        dumpCurrentTime(pw);
        pw.println();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$applyRestore$21$com-android-server-pm-ShortcutService  reason: not valid java name */
    public /* synthetic */ void m5668lambda$applyRestore$21$comandroidserverpmShortcutService(PrintWriter pw) {
        pw.print("Finish time: ");
        dumpCurrentTime(pw);
        pw.println();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpAndUsageStatsPermission(this.mContext, TAG, pw)) {
            dumpNoCheck(fd, pw, args);
        }
    }

    void dumpNoCheck(FileDescriptor fd, PrintWriter pw, String[] args) {
        DumpFilter filter = parseDumpArgs(args);
        if (filter.shouldDumpCheckIn()) {
            dumpCheckin(pw, filter.shouldCheckInClear());
            return;
        }
        if (filter.shouldDumpMain()) {
            dumpInner(pw, filter);
            pw.println();
        }
        if (filter.shouldDumpUid()) {
            dumpUid(pw);
            pw.println();
        }
        if (filter.shouldDumpFiles()) {
            dumpDumpFiles(pw);
            pw.println();
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:66:0x0103, code lost:
        r2 = r6.length;
     */
    /* JADX WARN: Code restructure failed: missing block: B:67:0x0104, code lost:
        if (r1 >= r2) goto L44;
     */
    /* JADX WARN: Code restructure failed: missing block: B:68:0x0106, code lost:
        r0.addPackage(r6[r1]);
        r1 = r1 + 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:69:0x010f, code lost:
        return r0;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private static DumpFilter parseDumpArgs(String[] args) {
        DumpFilter filter = new DumpFilter();
        if (args == null) {
            return filter;
        }
        int argIndex = 0;
        while (true) {
            if (argIndex >= args.length) {
                break;
            }
            int argIndex2 = argIndex + 1;
            String arg = args[argIndex];
            if ("-c".equals(arg)) {
                filter.setDumpCheckIn(true);
            } else if ("--checkin".equals(arg)) {
                filter.setDumpCheckIn(true);
                filter.setCheckInClear(true);
            } else if ("-a".equals(arg) || "--all".equals(arg)) {
                filter.setDumpUid(true);
                filter.setDumpFiles(true);
            } else if ("-u".equals(arg) || "--uid".equals(arg)) {
                filter.setDumpUid(true);
            } else if ("-f".equals(arg) || "--files".equals(arg)) {
                filter.setDumpFiles(true);
            } else if ("-n".equals(arg) || "--no-main".equals(arg)) {
                filter.setDumpMain(false);
            } else if ("--user".equals(arg)) {
                if (argIndex2 >= args.length) {
                    throw new IllegalArgumentException("Missing user ID for --user");
                }
                int argIndex3 = argIndex2 + 1;
                try {
                    filter.addUser(Integer.parseInt(args[argIndex2]));
                    argIndex = argIndex3;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid user ID", e);
                }
            } else if ("-p".equals(arg) || "--package".equals(arg)) {
                if (argIndex2 >= args.length) {
                    throw new IllegalArgumentException("Missing package name for --package");
                }
                filter.addPackageRegex(args[argIndex2]);
                filter.setDumpDetails(false);
                argIndex = argIndex2 + 1;
            } else if (arg.startsWith("-")) {
                throw new IllegalArgumentException("Unknown option " + arg);
            } else {
                argIndex = argIndex2;
            }
            argIndex = argIndex2;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class DumpFilter {
        private boolean mDumpCheckIn = false;
        private boolean mCheckInClear = false;
        private boolean mDumpMain = true;
        private boolean mDumpUid = false;
        private boolean mDumpFiles = false;
        private boolean mDumpDetails = true;
        private List<Pattern> mPackagePatterns = new ArrayList();
        private List<Integer> mUsers = new ArrayList();

        DumpFilter() {
        }

        void addPackageRegex(String regex) {
            this.mPackagePatterns.add(Pattern.compile(regex));
        }

        public void addPackage(String packageName) {
            addPackageRegex(Pattern.quote(packageName));
        }

        void addUser(int userId) {
            this.mUsers.add(Integer.valueOf(userId));
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public boolean isPackageMatch(String packageName) {
            if (this.mPackagePatterns.size() == 0) {
                return true;
            }
            for (int i = 0; i < this.mPackagePatterns.size(); i++) {
                if (this.mPackagePatterns.get(i).matcher(packageName).find()) {
                    return true;
                }
            }
            return false;
        }

        boolean isUserMatch(int userId) {
            if (this.mUsers.size() == 0) {
                return true;
            }
            for (int i = 0; i < this.mUsers.size(); i++) {
                if (this.mUsers.get(i).intValue() == userId) {
                    return true;
                }
            }
            return false;
        }

        public boolean shouldDumpCheckIn() {
            return this.mDumpCheckIn;
        }

        public void setDumpCheckIn(boolean dumpCheckIn) {
            this.mDumpCheckIn = dumpCheckIn;
        }

        public boolean shouldCheckInClear() {
            return this.mCheckInClear;
        }

        public void setCheckInClear(boolean checkInClear) {
            this.mCheckInClear = checkInClear;
        }

        public boolean shouldDumpMain() {
            return this.mDumpMain;
        }

        public void setDumpMain(boolean dumpMain) {
            this.mDumpMain = dumpMain;
        }

        public boolean shouldDumpUid() {
            return this.mDumpUid;
        }

        public void setDumpUid(boolean dumpUid) {
            this.mDumpUid = dumpUid;
        }

        public boolean shouldDumpFiles() {
            return this.mDumpFiles;
        }

        public void setDumpFiles(boolean dumpFiles) {
            this.mDumpFiles = dumpFiles;
        }

        public boolean shouldDumpDetails() {
            return this.mDumpDetails;
        }

        public void setDumpDetails(boolean dumpDetails) {
            this.mDumpDetails = dumpDetails;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpInner(PrintWriter pw) {
        dumpInner(pw, new DumpFilter());
    }

    private void dumpInner(PrintWriter pw, DumpFilter filter) {
        synchronized (this.mLock) {
            if (filter.shouldDumpDetails()) {
                long now = injectCurrentTimeMillis();
                pw.print("Now: [");
                pw.print(now);
                pw.print("] ");
                pw.print(formatTime(now));
                pw.print("  Raw last reset: [");
                pw.print(this.mRawLastResetTime);
                pw.print("] ");
                pw.print(formatTime(this.mRawLastResetTime));
                long last = getLastResetTimeLocked();
                pw.print("  Last reset: [");
                pw.print(last);
                pw.print("] ");
                pw.print(formatTime(last));
                long next = getNextResetTimeLocked();
                pw.print("  Next reset: [");
                pw.print(next);
                pw.print("] ");
                pw.print(formatTime(next));
                pw.println();
                pw.println();
                pw.print("  Config:");
                pw.print("    Max icon dim: ");
                pw.println(this.mMaxIconDimension);
                pw.print("    Icon format: ");
                pw.println(this.mIconPersistFormat);
                pw.print("    Icon quality: ");
                pw.println(this.mIconPersistQuality);
                pw.print("    saveDelayMillis: ");
                pw.println(this.mSaveDelayMillis);
                pw.print("    resetInterval: ");
                pw.println(this.mResetInterval);
                pw.print("    maxUpdatesPerInterval: ");
                pw.println(this.mMaxUpdatesPerInterval);
                pw.print("    maxShortcutsPerActivity: ");
                pw.println(this.mMaxShortcuts);
                pw.println();
                this.mStatLogger.dump(pw, "  ");
                pw.println();
                pw.print("  #Failures: ");
                pw.println(this.mWtfCount);
                if (this.mLastWtfStacktrace != null) {
                    pw.print("  Last failure stack trace: ");
                    pw.println(Log.getStackTraceString(this.mLastWtfStacktrace));
                }
                pw.println();
            }
            for (int i = 0; i < this.mUsers.size(); i++) {
                ShortcutUser user = this.mUsers.valueAt(i);
                if (filter.isUserMatch(user.getUserId())) {
                    user.dump(pw, "  ", filter);
                    pw.println();
                }
            }
            for (int i2 = 0; i2 < this.mShortcutNonPersistentUsers.size(); i2++) {
                ShortcutNonPersistentUser user2 = this.mShortcutNonPersistentUsers.valueAt(i2);
                if (filter.isUserMatch(user2.getUserId())) {
                    user2.dump(pw, "  ", filter);
                    pw.println();
                }
            }
        }
    }

    private void dumpUid(PrintWriter pw) {
        synchronized (this.mLock) {
            pw.println("** SHORTCUT MANAGER UID STATES (dumpsys shortcut -n -u)");
            for (int i = 0; i < this.mUidState.size(); i++) {
                int uid = this.mUidState.keyAt(i);
                int state = this.mUidState.valueAt(i);
                pw.print("    UID=");
                pw.print(uid);
                pw.print(" state=");
                pw.print(state);
                if (isProcessStateForeground(state)) {
                    pw.print("  [FG]");
                }
                pw.print("  last FG=");
                pw.print(this.mUidLastForegroundElapsedTime.get(uid));
                pw.println();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String formatTime(long time) {
        return TimeMigrationUtils.formatMillisWithFixedFormat(time);
    }

    private void dumpCurrentTime(PrintWriter pw) {
        pw.print(formatTime(injectCurrentTimeMillis()));
    }

    private void dumpCheckin(PrintWriter pw, boolean clear) {
        synchronized (this.mLock) {
            try {
                JSONArray users = new JSONArray();
                for (int i = 0; i < this.mUsers.size(); i++) {
                    users.put(this.mUsers.valueAt(i).dumpCheckin(clear));
                }
                JSONObject result = new JSONObject();
                result.put(KEY_SHORTCUT, users);
                result.put("lowRam", injectIsLowRamDevice());
                result.put(KEY_ICON_SIZE, this.mMaxIconDimension);
                pw.println(result.toString(1));
            } catch (JSONException e) {
                Slog.e(TAG, "Unable to write in json", e);
            }
        }
    }

    private void dumpDumpFiles(PrintWriter pw) {
        synchronized (this.mLock) {
            pw.println("** SHORTCUT MANAGER FILES (dumpsys shortcut -n -f)");
            this.mShortcutDumpFiles.dumpAll(pw);
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r12v0, resolved type: com.android.server.pm.ShortcutService */
    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        enforceShell();
        long token = injectClearCallingIdentity();
        try {
            int status = new MyShellCommand().exec(this, in, out, err, args, callback, resultReceiver);
            try {
                resultReceiver.send(status, null);
                injectRestoreCallingIdentity(token);
            } catch (Throwable th) {
                th = th;
                injectRestoreCallingIdentity(token);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class CommandException extends Exception {
        public CommandException(String message) {
            super(message);
        }
    }

    /* loaded from: classes2.dex */
    private class MyShellCommand extends ShellCommand {
        private int mShortcutMatchFlags;
        private int mUserId;

        private MyShellCommand() {
            this.mUserId = 0;
            this.mShortcutMatchFlags = 15;
        }

        private void parseOptionsLocked(boolean takeUser) throws CommandException {
            while (true) {
                String opt = getNextOption();
                if (opt != null) {
                    char c = 65535;
                    switch (opt.hashCode()) {
                        case -1626182425:
                            if (opt.equals("--flags")) {
                                c = 1;
                                break;
                            }
                            break;
                        case 1333469547:
                            if (opt.equals("--user")) {
                                c = 0;
                                break;
                            }
                            break;
                    }
                    switch (c) {
                        case 0:
                            if (!takeUser) {
                                break;
                            } else {
                                int parseUserArg = UserHandle.parseUserArg(getNextArgRequired());
                                this.mUserId = parseUserArg;
                                if (!ShortcutService.this.isUserUnlockedL(parseUserArg)) {
                                    throw new CommandException("User " + this.mUserId + " is not running or locked");
                                }
                                continue;
                            }
                        case 1:
                            break;
                        default:
                            throw new CommandException("Unknown option: " + opt);
                    }
                    this.mShortcutMatchFlags = Integer.parseInt(getNextArgRequired());
                } else {
                    return;
                }
            }
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        public int onCommand(String cmd) {
            if (cmd == null) {
                return handleDefaultCommands(cmd);
            }
            PrintWriter pw = getOutPrintWriter();
            char c = 65535;
            try {
                switch (cmd.hashCode()) {
                    case -1610733672:
                        if (cmd.equals("has-shortcut-access")) {
                            c = '\t';
                            break;
                        }
                        break;
                    case -1117067818:
                        if (cmd.equals("verify-states")) {
                            c = '\b';
                            break;
                        }
                        break;
                    case -749565587:
                        if (cmd.equals("clear-shortcuts")) {
                            c = 6;
                            break;
                        }
                        break;
                    case -276993226:
                        if (cmd.equals("get-shortcuts")) {
                            c = 7;
                            break;
                        }
                        break;
                    case -139706031:
                        if (cmd.equals("reset-all-throttling")) {
                            c = 1;
                            break;
                        }
                        break;
                    case -76794781:
                        if (cmd.equals("override-config")) {
                            c = 2;
                            break;
                        }
                        break;
                    case 188791973:
                        if (cmd.equals("reset-throttling")) {
                            c = 0;
                            break;
                        }
                        break;
                    case 1190495043:
                        if (cmd.equals("get-default-launcher")) {
                            c = 4;
                            break;
                        }
                        break;
                    case 1411888601:
                        if (cmd.equals("unload-user")) {
                            c = 5;
                            break;
                        }
                        break;
                    case 1964247424:
                        if (cmd.equals("reset-config")) {
                            c = 3;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                        handleResetThrottling();
                        break;
                    case 1:
                        handleResetAllThrottling();
                        break;
                    case 2:
                        handleOverrideConfig();
                        break;
                    case 3:
                        handleResetConfig();
                        break;
                    case 4:
                        handleGetDefaultLauncher();
                        break;
                    case 5:
                        handleUnloadUser();
                        break;
                    case 6:
                        handleClearShortcuts();
                        break;
                    case 7:
                        handleGetShortcuts();
                        break;
                    case '\b':
                        handleVerifyStates();
                        break;
                    case '\t':
                        handleHasShortcutAccess();
                        break;
                    default:
                        return handleDefaultCommands(cmd);
                }
                pw.println("Success");
                return 0;
            } catch (CommandException e) {
                pw.println("Error: " + e.getMessage());
                return 1;
            }
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("Usage: cmd shortcut COMMAND [options ...]");
            pw.println();
            pw.println("cmd shortcut reset-throttling [--user USER_ID]");
            pw.println("    Reset throttling for all packages and users");
            pw.println();
            pw.println("cmd shortcut reset-all-throttling");
            pw.println("    Reset the throttling state for all users");
            pw.println();
            pw.println("cmd shortcut override-config CONFIG");
            pw.println("    Override the configuration for testing (will last until reboot)");
            pw.println();
            pw.println("cmd shortcut reset-config");
            pw.println("    Reset the configuration set with \"update-config\"");
            pw.println();
            pw.println("[Deprecated] cmd shortcut get-default-launcher [--user USER_ID]");
            pw.println("    Show the default launcher");
            pw.println("    Note: This command is deprecated. Callers should query the default launcher from RoleManager instead.");
            pw.println();
            pw.println("cmd shortcut unload-user [--user USER_ID]");
            pw.println("    Unload a user from the memory");
            pw.println("    (This should not affect any observable behavior)");
            pw.println();
            pw.println("cmd shortcut clear-shortcuts [--user USER_ID] PACKAGE");
            pw.println("    Remove all shortcuts from a package, including pinned shortcuts");
            pw.println();
            pw.println("cmd shortcut get-shortcuts [--user USER_ID] [--flags FLAGS] PACKAGE");
            pw.println("    Show the shortcuts for a package that match the given flags");
            pw.println();
            pw.println("cmd shortcut has-shortcut-access [--user USER_ID] PACKAGE");
            pw.println("    Prints \"true\" if the package can access shortcuts, \"false\" otherwise");
            pw.println();
        }

        private void handleResetThrottling() throws CommandException {
            synchronized (ShortcutService.this.mLock) {
                parseOptionsLocked(true);
                Slog.i("ShellCommand", "cmd: handleResetThrottling: user=" + this.mUserId);
                ShortcutService.this.resetThrottlingInner(this.mUserId);
            }
        }

        private void handleResetAllThrottling() {
            Slog.i("ShellCommand", "cmd: handleResetAllThrottling");
            ShortcutService.this.resetAllThrottlingInner();
        }

        private void handleOverrideConfig() throws CommandException {
            String config = getNextArgRequired();
            Slog.i("ShellCommand", "cmd: handleOverrideConfig: " + config);
            synchronized (ShortcutService.this.mLock) {
                if (!ShortcutService.this.updateConfigurationLocked(config)) {
                    throw new CommandException("override-config failed.  See logcat for details.");
                }
            }
        }

        private void handleResetConfig() {
            Slog.i("ShellCommand", "cmd: handleResetConfig");
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.loadConfigurationLocked();
            }
        }

        private void handleGetDefaultLauncher() throws CommandException {
            synchronized (ShortcutService.this.mLock) {
                parseOptionsLocked(true);
                String defaultLauncher = ShortcutService.this.getDefaultLauncher(this.mUserId);
                if (defaultLauncher == null) {
                    throw new CommandException("Failed to get the default launcher for user " + this.mUserId);
                }
                List<ResolveInfo> allHomeCandidates = new ArrayList<>();
                ShortcutService.this.mPackageManagerInternal.getHomeActivitiesAsUser(allHomeCandidates, ShortcutService.this.getParentOrSelfUserId(this.mUserId));
                Iterator<ResolveInfo> it = allHomeCandidates.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    ResolveInfo ri = it.next();
                    ComponentInfo ci = ri.getComponentInfo();
                    if (ci.packageName.equals(defaultLauncher)) {
                        getOutPrintWriter().println("Launcher: " + ci.getComponentName());
                        break;
                    }
                }
            }
        }

        private void handleUnloadUser() throws CommandException {
            synchronized (ShortcutService.this.mLock) {
                parseOptionsLocked(true);
                Slog.i("ShellCommand", "cmd: handleUnloadUser: user=" + this.mUserId);
                ShortcutService.this.handleStopUser(this.mUserId);
            }
        }

        private void handleClearShortcuts() throws CommandException {
            synchronized (ShortcutService.this.mLock) {
                parseOptionsLocked(true);
                String packageName = getNextArgRequired();
                Slog.i("ShellCommand", "cmd: handleClearShortcuts: user" + this.mUserId + ", " + packageName);
                ShortcutService.this.cleanUpPackageForAllLoadedUsers(packageName, this.mUserId, true);
            }
        }

        private void handleGetShortcuts() throws CommandException {
            synchronized (ShortcutService.this.mLock) {
                parseOptionsLocked(true);
                String packageName = getNextArgRequired();
                Slog.i("ShellCommand", "cmd: handleGetShortcuts: user=" + this.mUserId + ", flags=" + this.mShortcutMatchFlags + ", package=" + packageName);
                ShortcutUser user = ShortcutService.this.getUserShortcutsLocked(this.mUserId);
                ShortcutPackage p = user.getPackageShortcutsIfExists(packageName);
                if (p == null) {
                    return;
                }
                p.dumpShortcuts(getOutPrintWriter(), this.mShortcutMatchFlags);
            }
        }

        private void handleVerifyStates() throws CommandException {
            try {
                ShortcutService.this.verifyStatesForce();
            } catch (Throwable th) {
                throw new CommandException(th.getMessage() + "\n" + Log.getStackTraceString(th));
            }
        }

        private void handleHasShortcutAccess() throws CommandException {
            synchronized (ShortcutService.this.mLock) {
                parseOptionsLocked(true);
                String packageName = getNextArgRequired();
                boolean shortcutAccess = ShortcutService.this.hasShortcutHostPermissionInner(packageName, this.mUserId);
                getOutPrintWriter().println(Boolean.toString(shortcutAccess));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long injectCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long injectElapsedRealtime() {
        return SystemClock.elapsedRealtime();
    }

    long injectUptimeMillis() {
        return SystemClock.uptimeMillis();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int injectBinderCallingUid() {
        return getCallingUid();
    }

    int injectBinderCallingPid() {
        return getCallingPid();
    }

    private int getCallingUserId() {
        return UserHandle.getUserId(injectBinderCallingUid());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long injectClearCallingIdentity() {
        return Binder.clearCallingIdentity();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void injectRestoreCallingIdentity(long token) {
        Binder.restoreCallingIdentity(token);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String injectBuildFingerprint() {
        return Build.FINGERPRINT;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void wtf(String message) {
        wtf(message, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void wtf(String message, Throwable e) {
        if (e == null) {
            e = new RuntimeException("Stacktrace");
        }
        synchronized (this.mLock) {
            this.mWtfCount++;
            this.mLastWtfStacktrace = new Exception("Last failure was logged here:");
        }
        Slog.wtf(TAG, message, e);
    }

    File injectSystemDataPath() {
        return Environment.getDataSystemDirectory();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public File injectUserDataPath(int userId) {
        return new File(Environment.getDataSystemCeDirectory(userId), DIRECTORY_PER_USER);
    }

    public File getDumpPath() {
        return new File(injectUserDataPath(0), DIRECTORY_DUMP);
    }

    boolean injectIsLowRamDevice() {
        return ActivityManager.isLowRamDeviceStatic();
    }

    void injectRegisterUidObserver(IUidObserver observer, int which) {
        try {
            ActivityManager.getService().registerUidObserver(observer, which, -1, (String) null);
        } catch (RemoteException e) {
        }
    }

    void injectRegisterRoleHoldersListener(OnRoleHoldersChangedListener listener) {
        this.mRoleManager.addOnRoleHoldersChangedListenerAsUser(this.mContext.getMainExecutor(), listener, UserHandle.ALL);
    }

    String injectGetHomeRoleHolderAsUser(int userId) {
        List<String> roleHolders = this.mRoleManager.getRoleHoldersAsUser("android.app.role.HOME", UserHandle.of(userId));
        if (roleHolders.isEmpty()) {
            return null;
        }
        return roleHolders.get(0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public File getUserBitmapFilePath(int userId) {
        return new File(injectUserDataPath(userId), DIRECTORY_BITMAPS);
    }

    SparseArray<ShortcutUser> getShortcutsForTest() {
        return this.mUsers;
    }

    int getMaxShortcutsForTest() {
        return this.mMaxShortcuts;
    }

    int getMaxUpdatesPerIntervalForTest() {
        return this.mMaxUpdatesPerInterval;
    }

    long getResetIntervalForTest() {
        return this.mResetInterval;
    }

    int getMaxIconDimensionForTest() {
        return this.mMaxIconDimension;
    }

    Bitmap.CompressFormat getIconPersistFormatForTest() {
        return this.mIconPersistFormat;
    }

    int getIconPersistQualityForTest() {
        return this.mIconPersistQuality;
    }

    ShortcutPackage getPackageShortcutForTest(String packageName, int userId) {
        synchronized (this.mLock) {
            ShortcutUser user = this.mUsers.get(userId);
            if (user == null) {
                return null;
            }
            return user.getAllPackagesForTest().get(packageName);
        }
    }

    ShortcutInfo getPackageShortcutForTest(String packageName, String shortcutId, int userId) {
        synchronized (this.mLock) {
            ShortcutPackage pkg = getPackageShortcutForTest(packageName, userId);
            if (pkg == null) {
                return null;
            }
            return pkg.findShortcutById(shortcutId);
        }
    }

    void updatePackageShortcutForTest(String packageName, String shortcutId, int userId, Consumer<ShortcutInfo> cb) {
        synchronized (this.mLock) {
            ShortcutPackage pkg = getPackageShortcutForTest(packageName, userId);
            if (pkg == null) {
                return;
            }
            cb.accept(pkg.findShortcutById(shortcutId));
        }
    }

    ShortcutLauncher getLauncherShortcutForTest(String packageName, int userId) {
        synchronized (this.mLock) {
            ShortcutUser user = this.mUsers.get(userId);
            if (user == null) {
                return null;
            }
            return user.getAllLaunchersForTest().get(ShortcutUser.PackageWithUser.of(userId, packageName));
        }
    }

    ShortcutRequestPinProcessor getShortcutRequestPinProcessorForTest() {
        return this.mShortcutRequestPinProcessor;
    }

    boolean injectShouldPerformVerification() {
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void verifyStates() {
        if (injectShouldPerformVerification()) {
            verifyStatesInner();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void verifyStatesForce() {
        verifyStatesInner();
    }

    private void verifyStatesInner() {
        synchronized (this.mLock) {
            forEachLoadedUserLocked(new Consumer() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda7
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((ShortcutUser) obj).forAllPackageItems(new Consumer() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda26
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj2) {
                            ((ShortcutPackageItem) obj2).verifyStates();
                        }
                    });
                }
            });
        }
    }

    void waitForBitmapSavesForTest() {
        synchronized (this.mLock) {
            forEachLoadedUserLocked(new Consumer() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((ShortcutUser) obj).forAllPackageItems(new Consumer() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda25
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj2) {
                            ((ShortcutPackageItem) obj2).waitForBitmapSaves();
                        }
                    });
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public List<ShortcutInfo> prepareChangedShortcuts(ArraySet<String> changedIds, ArraySet<String> newIds, List<ShortcutInfo> deletedList, ShortcutPackage ps) {
        if (ps == null) {
            return null;
        }
        if (CollectionUtils.isEmpty(changedIds) && CollectionUtils.isEmpty(newIds)) {
            return null;
        }
        final ArraySet<String> resultIds = new ArraySet<>();
        if (!CollectionUtils.isEmpty(changedIds)) {
            resultIds.addAll((ArraySet<? extends String>) changedIds);
        }
        if (!CollectionUtils.isEmpty(newIds)) {
            resultIds.addAll((ArraySet<? extends String>) newIds);
        }
        if (!CollectionUtils.isEmpty(deletedList)) {
            deletedList.removeIf(new Predicate() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda4
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    boolean contains;
                    contains = resultIds.contains(((ShortcutInfo) obj).getId());
                    return contains;
                }
            });
        }
        List<ShortcutInfo> result = new ArrayList<>();
        ps.findAll(result, new Predicate() { // from class: com.android.server.pm.ShortcutService$$ExternalSyntheticLambda5
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean contains;
                contains = resultIds.contains(((ShortcutInfo) obj).getId());
                return contains;
            }
        }, 4);
        return result;
    }

    private List<ShortcutInfo> prepareChangedShortcuts(List<ShortcutInfo> changedList, List<ShortcutInfo> newList, List<ShortcutInfo> deletedList, ShortcutPackage ps) {
        ArraySet<String> changedIds = new ArraySet<>();
        addShortcutIdsToSet(changedIds, changedList);
        ArraySet<String> newIds = new ArraySet<>();
        addShortcutIdsToSet(newIds, newList);
        return prepareChangedShortcuts(changedIds, newIds, deletedList, ps);
    }

    private void addShortcutIdsToSet(ArraySet<String> ids, List<ShortcutInfo> shortcuts) {
        if (CollectionUtils.isEmpty(shortcuts)) {
            return;
        }
        int size = shortcuts.size();
        for (int i = 0; i < size; i++) {
            ids.add(shortcuts.get(i).getId());
        }
    }
}
