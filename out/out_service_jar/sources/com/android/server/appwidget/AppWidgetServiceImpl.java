package com.android.server.appwidget;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityOptions;
import android.app.AlarmManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.AppOpsManagerInternal;
import android.app.IApplicationThread;
import android.app.IServiceConnection;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManagerInternal;
import android.app.usage.UsageStatsManagerInternal;
import android.appwidget.AppWidgetManagerInternal;
import android.appwidget.AppWidgetProviderInfo;
import android.appwidget.PendingHostUpdate;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.ShortcutServiceInternal;
import android.content.pm.SuspendDialogInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Point;
import android.graphics.drawable.Icon;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.hardware.usb.gadget.V1_2.GadgetFunction;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.DeviceConfig;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.AttributeSet;
import android.util.IntArray;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;
import android.util.TypedValue;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import android.util.proto.ProtoOutputStream;
import android.view.Display;
import android.widget.RemoteViews;
import com.android.internal.R;
import com.android.internal.app.SuspendedAppActivity;
import com.android.internal.app.UnlaunchableAppActivity;
import com.android.internal.appwidget.IAppWidgetHost;
import com.android.internal.appwidget.IAppWidgetService;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.widget.IRemoteViewsFactory;
import com.android.server.LocalServices;
import com.android.server.WidgetBackupProvider;
import com.android.server.appwidget.AppWidgetServiceImpl;
import com.android.server.pm.verify.domain.DomainVerificationLegacySettings;
import com.transsion.hubcore.appwidget.ITranAppWidgetServiceImpl;
import com.transsion.hubcore.griffin.ITranGriffinFeature;
import com.transsion.hubcore.griffin.lib.app.TranWidgetInfo;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class AppWidgetServiceImpl extends IAppWidgetService.Stub implements WidgetBackupProvider, DevicePolicyManagerInternal.OnCrossProfileWidgetProvidersChangeListener {
    private static final int CURRENT_VERSION = 1;
    private static final boolean DEBUG = false;
    static final boolean DEBUG_PROVIDER_INFO_CACHE = true;
    private static final int ID_PROVIDER_CHANGED = 1;
    private static final int ID_VIEWS_UPDATE = 0;
    private static final int KEYGUARD_HOST_ID = 1262836039;
    private static final int MIN_UPDATE_PERIOD = 1800000;
    private static final String NEW_KEYGUARD_HOST_PACKAGE = "com.android.keyguard";
    private static final String OLD_KEYGUARD_HOST_PACKAGE = "android";
    private static final String STATE_FILENAME = "appwidgets.xml";
    private static final String TAG = "AppWidgetServiceImpl";
    private static final int TAG_UNDEFINED = -1;
    private static final int UNKNOWN_UID = -1;
    private static final int UNKNOWN_USER_ID = -10;
    private static final AtomicLong UPDATE_COUNTER = new AtomicLong();
    private ActivityManagerInternal mActivityManagerInternal;
    private AlarmManager mAlarmManager;
    private AppOpsManager mAppOpsManager;
    private AppOpsManagerInternal mAppOpsManagerInternal;
    private BackupRestoreController mBackupRestoreController;
    private Handler mCallbackHandler;
    private final Context mContext;
    private DevicePolicyManagerInternal mDevicePolicyManagerInternal;
    private boolean mIsProviderInfoPersisted;
    private KeyguardManager mKeyguardManager;
    private int mMaxWidgetBitmapMemory;
    private IPackageManager mPackageManager;
    private PackageManagerInternal mPackageManagerInternal;
    private boolean mSafeMode;
    private Handler mSaveStateHandler;
    private SecurityPolicy mSecurityPolicy;
    private UsageStatsManagerInternal mUsageStatsManagerInternal;
    private UserManager mUserManager;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.appwidget.AppWidgetServiceImpl.1
        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            char c;
            String action = intent.getAction();
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
            switch (action.hashCode()) {
                case -1238404651:
                    if (action.equals("android.intent.action.MANAGED_PROFILE_UNAVAILABLE")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case -1001645458:
                    if (action.equals("android.intent.action.PACKAGES_SUSPENDED")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case -864107122:
                    if (action.equals("android.intent.action.MANAGED_PROFILE_AVAILABLE")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 1290767157:
                    if (action.equals("android.intent.action.PACKAGES_UNSUSPENDED")) {
                        c = 3;
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
                case 1:
                    synchronized (AppWidgetServiceImpl.this.mLock) {
                        AppWidgetServiceImpl.this.reloadWidgetsMaskedState(userId);
                    }
                    return;
                case 2:
                    AppWidgetServiceImpl.this.onPackageBroadcastReceived(intent, getSendingUserId());
                    AppWidgetServiceImpl.this.updateWidgetPackageSuspensionMaskedState(intent, true, getSendingUserId());
                    return;
                case 3:
                    AppWidgetServiceImpl.this.onPackageBroadcastReceived(intent, getSendingUserId());
                    AppWidgetServiceImpl.this.updateWidgetPackageSuspensionMaskedState(intent, false, getSendingUserId());
                    return;
                default:
                    AppWidgetServiceImpl.this.onPackageBroadcastReceived(intent, getSendingUserId());
                    return;
            }
        }
    };
    private final HashMap<Pair<Integer, Intent.FilterComparison>, HashSet<Integer>> mRemoteViewsServicesAppWidgets = new HashMap<>();
    private final Object mLock = new Object();
    private final ArrayList<Widget> mWidgets = new ArrayList<>();
    private final ArrayList<Host> mHosts = new ArrayList<>();
    private final ArrayList<Provider> mProviders = new ArrayList<>();
    private final ArraySet<Pair<Integer, String>> mPackagesWithBindWidgetPermission = new ArraySet<>();
    private final SparseBooleanArray mLoadedUserIds = new SparseBooleanArray();
    private final Object mWidgetPackagesLock = new Object();
    private final SparseArray<ArraySet<String>> mWidgetPackages = new SparseArray<>();
    private final SparseIntArray mNextAppWidgetIds = new SparseIntArray();

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppWidgetServiceImpl(Context context) {
        this.mContext = context;
    }

    public void onStart() {
        this.mPackageManager = AppGlobals.getPackageManager();
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mAppOpsManager = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        this.mDevicePolicyManagerInternal = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mSaveStateHandler = BackgroundThread.getHandler();
        this.mCallbackHandler = new CallbackHandler(this.mContext.getMainLooper());
        this.mBackupRestoreController = new BackupRestoreController();
        this.mSecurityPolicy = new SecurityPolicy();
        boolean z = true;
        z = (ActivityManager.isLowRamDeviceStatic() || !DeviceConfig.getBoolean("systemui", "persists_widget_provider_info", true)) ? false : false;
        this.mIsProviderInfoPersisted = z;
        if (!z) {
            Slog.d(TAG, "App widget provider info will not be persisted on this device");
        }
        computeMaximumWidgetBitmapMemory();
        registerBroadcastReceiver();
        registerOnCrossProfileProvidersChangedListener();
        LocalServices.addService(AppWidgetManagerInternal.class, new AppWidgetManagerLocal());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void systemServicesReady() {
        this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        this.mAppOpsManagerInternal = (AppOpsManagerInternal) LocalServices.getService(AppOpsManagerInternal.class);
        this.mUsageStatsManagerInternal = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
    }

    private void computeMaximumWidgetBitmapMemory() {
        Display display = this.mContext.getDisplayNoVerify();
        Point size = new Point();
        display.getRealSize(size);
        this.mMaxWidgetBitmapMemory = size.x * 6 * size.y;
    }

    private void registerBroadcastReceiver() {
        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction("android.intent.action.PACKAGE_ADDED");
        packageFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        packageFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        packageFilter.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, packageFilter, null, null);
        IntentFilter sdFilter = new IntentFilter();
        sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
        sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, sdFilter, null, null);
        IntentFilter offModeFilter = new IntentFilter();
        offModeFilter.addAction("android.intent.action.MANAGED_PROFILE_AVAILABLE");
        offModeFilter.addAction("android.intent.action.MANAGED_PROFILE_UNAVAILABLE");
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, offModeFilter, null, null);
        IntentFilter suspendPackageFilter = new IntentFilter();
        suspendPackageFilter.addAction("android.intent.action.PACKAGES_SUSPENDED");
        suspendPackageFilter.addAction("android.intent.action.PACKAGES_UNSUSPENDED");
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, suspendPackageFilter, null, null);
    }

    private void registerOnCrossProfileProvidersChangedListener() {
        DevicePolicyManagerInternal devicePolicyManagerInternal = this.mDevicePolicyManagerInternal;
        if (devicePolicyManagerInternal != null) {
            devicePolicyManagerInternal.addOnCrossProfileWidgetProvidersChangeListener(this);
        }
    }

    public void setSafeMode(boolean safeMode) {
        this.mSafeMode = safeMode;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:51:0x00a2, code lost:
        r9 = r4.length;
     */
    /* JADX WARN: Code restructure failed: missing block: B:52:0x00a3, code lost:
        if (r6 >= r9) goto L39;
     */
    /* JADX WARN: Code restructure failed: missing block: B:53:0x00a5, code lost:
        r3 = r3 | removeHostsAndProvidersForPackageLocked(r4[r6], r14);
        r6 = r6 + 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:55:0x00b1, code lost:
        if (r8 == null) goto L48;
     */
    /* JADX WARN: Code restructure failed: missing block: B:57:0x00b9, code lost:
        if (r8.getBoolean("android.intent.extra.REPLACING", false) != false) goto L63;
     */
    /* JADX WARN: Removed duplicated region for block: B:71:0x00db A[Catch: all -> 0x00ea, TryCatch #0 {, blocks: (B:35:0x0076, B:37:0x007e, B:40:0x0086, B:45:0x0094, B:51:0x00a2, B:53:0x00a5, B:71:0x00db, B:72:0x00e6, B:56:0x00b3, B:60:0x00bd, B:62:0x00c0, B:65:0x00cc, B:67:0x00d2, B:68:0x00d5, B:74:0x00e8), top: B:80:0x0076 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void onPackageBroadcastReceived(Intent intent, int userId) {
        char c;
        String[] pkgList;
        int uid;
        String pkgName;
        String action = intent.getAction();
        boolean added = false;
        boolean changed = false;
        boolean componentsModified = false;
        boolean packageRemovedPermanently = true;
        int i = 0;
        switch (action.hashCode()) {
            case -1403934493:
                if (action.equals("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case -1338021860:
                if (action.equals("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -1001645458:
                if (action.equals("android.intent.action.PACKAGES_SUSPENDED")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 1290767157:
                if (action.equals("android.intent.action.PACKAGES_UNSUSPENDED")) {
                    c = 1;
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
            case 1:
                pkgList = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                changed = true;
                break;
            case 2:
                added = true;
            case 3:
                pkgList = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                break;
            default:
                Uri uri = intent.getData();
                if (uri == null || (pkgName = uri.getSchemeSpecificPart()) == null) {
                    return;
                }
                String[] pkgList2 = {pkgName};
                added = "android.intent.action.PACKAGE_ADDED".equals(action);
                changed = "android.intent.action.PACKAGE_CHANGED".equals(action);
                pkgList = pkgList2;
                break;
                break;
        }
        if (pkgList == null || pkgList.length == 0) {
            return;
        }
        synchronized (this.mLock) {
            if (this.mUserManager.isUserUnlockingOrUnlocked(userId) && !isProfileWithLockedParent(userId)) {
                ensureGroupStateLoadedLocked(userId, false);
                Bundle extras = intent.getExtras();
                if (!added && !changed) {
                    if (extras != null && extras.getBoolean("android.intent.extra.REPLACING", false)) {
                        packageRemovedPermanently = false;
                    }
                    if (componentsModified) {
                        saveGroupStateAsync(userId);
                        scheduleNotifyGroupHostsForProvidersChangedLocked(userId);
                        this.mBackupRestoreController.widgetComponentsChanged(userId);
                    }
                }
                packageRemovedPermanently = false;
                int length = pkgList.length;
                while (i < length) {
                    String pkgName2 = pkgList[i];
                    componentsModified |= updateProvidersForPackageLocked(pkgName2, userId, null);
                    if (packageRemovedPermanently && userId == 0 && (uid = getUidForPackage(pkgName2, userId)) >= 0) {
                        resolveHostUidLocked(pkgName2, uid);
                    }
                    i++;
                }
                if (componentsModified) {
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reloadWidgetsMaskedStateForGroup(int userId) {
        if (!this.mUserManager.isUserUnlockingOrUnlocked(userId)) {
            return;
        }
        synchronized (this.mLock) {
            reloadWidgetsMaskedState(userId);
            int[] profileIds = this.mUserManager.getEnabledProfileIds(userId);
            for (int profileId : profileIds) {
                reloadWidgetsMaskedState(profileId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reloadWidgetsMaskedState(int userId) {
        boolean suspended;
        long identity = Binder.clearCallingIdentity();
        try {
            UserInfo user = this.mUserManager.getUserInfo(userId);
            boolean lockedProfile = !this.mUserManager.isUserUnlockingOrUnlocked(userId);
            boolean quietProfile = user.isQuietModeEnabled();
            int N = this.mProviders.size();
            for (int i = 0; i < N; i++) {
                Provider provider = this.mProviders.get(i);
                int providerUserId = provider.getUserId();
                if (providerUserId == userId) {
                    boolean changed = provider.setMaskedByLockedProfileLocked(lockedProfile) | provider.setMaskedByQuietProfileLocked(quietProfile);
                    try {
                        try {
                            suspended = this.mPackageManager.isPackageSuspendedForUser(provider.id.componentName.getPackageName(), provider.getUserId());
                        } catch (RemoteException e) {
                            Slog.e(TAG, "Failed to query application info", e);
                        }
                    } catch (IllegalArgumentException e2) {
                        suspended = false;
                    }
                    changed |= provider.setMaskedBySuspendedPackageLocked(suspended);
                    if (changed) {
                        if (provider.isMaskedLocked()) {
                            maskWidgetsViewsLocked(provider, null);
                        } else {
                            unmaskWidgetsViewsLocked(provider);
                        }
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateWidgetPackageSuspensionMaskedState(Intent intent, boolean suspended, int profileId) {
        String[] packagesArray = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
        if (packagesArray == null) {
            return;
        }
        Set<String> packages = new ArraySet<>(Arrays.asList(packagesArray));
        synchronized (this.mLock) {
            int N = this.mProviders.size();
            for (int i = 0; i < N; i++) {
                Provider provider = this.mProviders.get(i);
                int providerUserId = provider.getUserId();
                if (providerUserId == profileId && packages.contains(provider.id.componentName.getPackageName()) && provider.setMaskedBySuspendedPackageLocked(suspended)) {
                    if (provider.isMaskedLocked()) {
                        maskWidgetsViewsLocked(provider, null);
                    } else {
                        unmaskWidgetsViewsLocked(provider);
                    }
                }
            }
        }
    }

    private void maskWidgetsViewsLocked(Provider provider, Widget targetWidget) {
        boolean showBadge;
        Intent onClickIntent;
        Icon icon;
        int widgetCount = provider.widgets.size();
        if (widgetCount == 0) {
            return;
        }
        RemoteViews views = new RemoteViews(this.mContext.getPackageName(), 17367377);
        ApplicationInfo appInfo = provider.info.providerInfo.applicationInfo;
        int appUserId = provider.getUserId();
        long identity = Binder.clearCallingIdentity();
        try {
            if (provider.maskedBySuspendedPackage) {
                showBadge = this.mUserManager.hasBadge(appUserId);
                String suspendingPackage = this.mPackageManagerInternal.getSuspendingPackage(appInfo.packageName, appUserId);
                if ("android".equals(suspendingPackage)) {
                    onClickIntent = this.mDevicePolicyManagerInternal.createShowAdminSupportIntent(appUserId, true);
                } else {
                    SuspendDialogInfo dialogInfo = this.mPackageManagerInternal.getSuspendedDialogInfo(appInfo.packageName, suspendingPackage, appUserId);
                    onClickIntent = SuspendedAppActivity.createSuspendedAppInterceptIntent(appInfo.packageName, suspendingPackage, dialogInfo, (Bundle) null, (IntentSender) null, appUserId);
                }
            } else if (provider.maskedByQuietProfile) {
                showBadge = true;
                onClickIntent = UnlaunchableAppActivity.createInQuietModeDialogIntent(appUserId);
            } else {
                showBadge = true;
                onClickIntent = this.mKeyguardManager.createConfirmDeviceCredentialIntent(null, null, appUserId);
                if (onClickIntent != null) {
                    onClickIntent.setFlags(276824064);
                }
            }
            if (onClickIntent != null) {
                views.setOnClickPendingIntent(16908288, PendingIntent.getActivity(this.mContext, 0, onClickIntent, AudioFormat.DTS_HD));
            }
            if (appInfo.icon != 0) {
                icon = Icon.createWithResource(appInfo.packageName, appInfo.icon);
            } else {
                icon = Icon.createWithResource(this.mContext, 17301651);
            }
            views.setImageViewIcon(16909715, icon);
            if (!showBadge) {
                views.setViewVisibility(16909716, 4);
            }
            for (int j = 0; j < widgetCount; j++) {
                Widget widget = provider.widgets.get(j);
                if ((targetWidget == null || targetWidget == widget) && widget.replaceWithMaskedViewsLocked(views)) {
                    scheduleNotifyUpdateAppWidgetLocked(widget, widget.getEffectiveViewsLocked());
                }
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void unmaskWidgetsViewsLocked(Provider provider) {
        int widgetCount = provider.widgets.size();
        for (int j = 0; j < widgetCount; j++) {
            Widget widget = provider.widgets.get(j);
            if (widget.clearMaskedViewsLocked()) {
                scheduleNotifyUpdateAppWidgetLocked(widget, widget.getEffectiveViewsLocked());
            }
        }
    }

    private void resolveHostUidLocked(String pkg, int uid) {
        int N = this.mHosts.size();
        for (int i = 0; i < N; i++) {
            Host host = this.mHosts.get(i);
            if (host.id.uid == -1 && pkg.equals(host.id.packageName)) {
                host.id = new HostId(uid, host.id.hostId, host.id.packageName);
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void ensureGroupStateLoadedLocked(int userId) {
        ensureGroupStateLoadedLocked(userId, true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void ensureGroupStateLoadedLocked(int userId, boolean enforceUserUnlockingOrUnlocked) {
        if (enforceUserUnlockingOrUnlocked && !isUserRunningAndUnlocked(userId)) {
            throw new IllegalStateException("User " + userId + " must be unlocked for widgets to be available");
        }
        if (enforceUserUnlockingOrUnlocked && isProfileWithLockedParent(userId)) {
            throw new IllegalStateException("Profile " + userId + " must have unlocked parent");
        }
        int[] profileIds = this.mSecurityPolicy.getEnabledGroupProfileIds(userId);
        IntArray newIds = new IntArray(1);
        for (int profileId : profileIds) {
            if (!this.mLoadedUserIds.get(profileId)) {
                this.mLoadedUserIds.put(profileId, true);
                newIds.add(profileId);
            }
        }
        if (newIds.size() <= 0) {
            return;
        }
        int[] newProfileIds = newIds.toArray();
        clearProvidersAndHostsTagsLocked();
        loadGroupWidgetProvidersLocked(newProfileIds);
        loadGroupStateLocked(newProfileIds);
    }

    private boolean isUserRunningAndUnlocked(int userId) {
        return this.mUserManager.isUserUnlockingOrUnlocked(userId);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            synchronized (this.mLock) {
                if (args.length > 0 && "--proto".equals(args[0])) {
                    dumpProto(fd);
                } else {
                    dumpInternalLocked(pw);
                }
            }
        }
    }

    private void dumpProto(FileDescriptor fd) {
        Slog.i(TAG, "dump proto for " + this.mWidgets.size() + " widgets");
        ProtoOutputStream proto = new ProtoOutputStream(fd);
        int N = this.mWidgets.size();
        for (int i = 0; i < N; i++) {
            dumpProtoWidget(proto, this.mWidgets.get(i));
        }
        proto.flush();
    }

    private void dumpProtoWidget(ProtoOutputStream proto, Widget widget) {
        if (widget.host == null || widget.provider == null) {
            Slog.d(TAG, "skip dumping widget because host or provider is null: widget.host=" + widget.host + " widget.provider=" + widget.provider);
            return;
        }
        long token = proto.start(CompanionAppsPermissions.APP_PERMISSIONS);
        proto.write(1133871366145L, widget.host.getUserId() != widget.provider.getUserId());
        proto.write(1133871366146L, widget.host.callbacks == null);
        proto.write(1138166333443L, widget.host.id.packageName);
        proto.write(1138166333444L, widget.provider.id.componentName.getPackageName());
        proto.write(1138166333445L, widget.provider.id.componentName.getClassName());
        if (widget.options != null) {
            proto.write(1133871366154L, widget.options.getBoolean("appWidgetRestoreCompleted"));
            proto.write(1120986464262L, widget.options.getInt("appWidgetMinWidth", 0));
            proto.write(1120986464263L, widget.options.getInt("appWidgetMinHeight", 0));
            proto.write(1120986464264L, widget.options.getInt("appWidgetMaxWidth", 0));
            proto.write(1120986464265L, widget.options.getInt("appWidgetMaxHeight", 0));
        }
        proto.end(token);
    }

    private void dumpInternalLocked(PrintWriter pw) {
        int N = this.mProviders.size();
        pw.println("Providers:");
        for (int i = 0; i < N; i++) {
            dumpProviderLocked(this.mProviders.get(i), i, pw);
        }
        int N2 = this.mWidgets.size();
        pw.println(" ");
        pw.println("Widgets:");
        for (int i2 = 0; i2 < N2; i2++) {
            dumpWidget(this.mWidgets.get(i2), i2, pw);
        }
        int N3 = this.mHosts.size();
        pw.println(" ");
        pw.println("Hosts:");
        for (int i3 = 0; i3 < N3; i3++) {
            dumpHost(this.mHosts.get(i3), i3, pw);
        }
        int N4 = this.mPackagesWithBindWidgetPermission.size();
        pw.println(" ");
        pw.println("Grants:");
        for (int i4 = 0; i4 < N4; i4++) {
            Pair<Integer, String> grant = this.mPackagesWithBindWidgetPermission.valueAt(i4);
            dumpGrant(grant, i4, pw);
        }
    }

    public ParceledListSlice<PendingHostUpdate> startListening(IAppWidgetHost callbacks, String callingPackage, int hostId, int[] appWidgetIds) {
        int userId = UserHandle.getCallingUserId();
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            try {
                try {
                    if (this.mSecurityPolicy.isInstantAppLocked(callingPackage, userId)) {
                        Slog.w(TAG, "Instant package " + callingPackage + " cannot host app widgets");
                        return ParceledListSlice.emptyList();
                    }
                    ensureGroupStateLoadedLocked(userId);
                    try {
                        HostId id = new HostId(Binder.getCallingUid(), hostId, callingPackage);
                        Host host = lookupOrAddHostLocked(id);
                        host.callbacks = callbacks;
                        long updateSequenceNo = UPDATE_COUNTER.incrementAndGet();
                        int N = appWidgetIds.length;
                        ArrayList<PendingHostUpdate> outUpdates = new ArrayList<>(N);
                        LongSparseArray<PendingHostUpdate> updatesMap = new LongSparseArray<>();
                        int i = 0;
                        while (i < N) {
                            updatesMap.clear();
                            HostId id2 = id;
                            host.getPendingUpdatesForIdLocked(this.mContext, appWidgetIds[i], updatesMap);
                            int j = 0;
                            for (int m = updatesMap.size(); j < m; m = m) {
                                outUpdates.add(updatesMap.valueAt(j));
                                j++;
                            }
                            i++;
                            id = id2;
                        }
                        host.lastWidgetUpdateSequenceNo = updateSequenceNo;
                        return new ParceledListSlice<>(outUpdates);
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    public void stopListening(String callingPackage, int hostId) {
        int userId = UserHandle.getCallingUserId();
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId, false);
            HostId id = new HostId(Binder.getCallingUid(), hostId, callingPackage);
            Host host = lookupHostLocked(id);
            if (host != null) {
                host.callbacks = null;
                pruneHostLocked(host);
                this.mAppOpsManagerInternal.updateAppWidgetVisibility(host.getWidgetUids(), false);
            }
        }
    }

    public int allocateAppWidgetId(String callingPackage, int hostId) {
        int userId = UserHandle.getCallingUserId();
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            if (this.mSecurityPolicy.isInstantAppLocked(callingPackage, userId)) {
                Slog.w(TAG, "Instant package " + callingPackage + " cannot host app widgets");
                return 0;
            }
            ensureGroupStateLoadedLocked(userId);
            if (this.mNextAppWidgetIds.indexOfKey(userId) < 0) {
                this.mNextAppWidgetIds.put(userId, 1);
            }
            int appWidgetId = incrementAndGetAppWidgetIdLocked(userId);
            HostId id = new HostId(Binder.getCallingUid(), hostId, callingPackage);
            Host host = lookupOrAddHostLocked(id);
            Widget widget = new Widget();
            widget.appWidgetId = appWidgetId;
            widget.host = host;
            host.widgets.add(widget);
            addWidgetLocked(widget);
            saveGroupStateAsync(userId);
            return appWidgetId;
        }
    }

    public void deleteAppWidgetId(String callingPackage, int appWidgetId) {
        int userId = UserHandle.getCallingUserId();
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            Widget widget = lookupWidgetLocked(appWidgetId, Binder.getCallingUid(), callingPackage);
            if (widget == null) {
                return;
            }
            deleteAppWidgetLocked(widget);
            saveGroupStateAsync(userId);
        }
    }

    public boolean hasBindAppWidgetPermission(String packageName, int grantId) {
        this.mSecurityPolicy.enforceModifyAppWidgetBindPermissions(packageName);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(grantId);
            int packageUid = getUidForPackage(packageName, grantId);
            if (packageUid < 0) {
                return false;
            }
            Pair<Integer, String> packageId = Pair.create(Integer.valueOf(grantId), packageName);
            return this.mPackagesWithBindWidgetPermission.contains(packageId);
        }
    }

    public void setBindAppWidgetPermission(String packageName, int grantId, boolean grantPermission) {
        this.mSecurityPolicy.enforceModifyAppWidgetBindPermissions(packageName);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(grantId);
            int packageUid = getUidForPackage(packageName, grantId);
            if (packageUid < 0) {
                return;
            }
            Pair<Integer, String> packageId = Pair.create(Integer.valueOf(grantId), packageName);
            if (grantPermission) {
                this.mPackagesWithBindWidgetPermission.add(packageId);
            } else {
                this.mPackagesWithBindWidgetPermission.remove(packageId);
            }
            saveGroupStateAsync(grantId);
        }
    }

    public IntentSender createAppWidgetConfigIntentSender(String callingPackage, int appWidgetId, int intentFlags) {
        int userId = UserHandle.getCallingUserId();
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            try {
                try {
                    ensureGroupStateLoadedLocked(userId);
                    Widget widget = lookupWidgetLocked(appWidgetId, Binder.getCallingUid(), callingPackage);
                    if (widget == null) {
                        throw new IllegalArgumentException("Bad widget id " + appWidgetId);
                    }
                    Provider provider = widget.provider;
                    if (provider == null) {
                        throw new IllegalArgumentException("Widget not bound " + appWidgetId);
                    }
                    int secureFlags = intentFlags & (-196);
                    Intent intent = new Intent("android.appwidget.action.APPWIDGET_CONFIGURE");
                    intent.putExtra("appWidgetId", appWidgetId);
                    intent.setComponent(provider.getInfoLocked(this.mContext).configure);
                    intent.setFlags(secureFlags);
                    ActivityOptions options = ActivityOptions.makeBasic();
                    options.setIgnorePendingIntentCreatorForegroundState(true);
                    long identity = Binder.clearCallingIdentity();
                    try {
                        try {
                            IntentSender intentSender = PendingIntent.getActivityAsUser(this.mContext, 0, intent, 1409286144, options.toBundle(), new UserHandle(provider.getUserId())).getIntentSender();
                            Binder.restoreCallingIdentity(identity);
                            return intentSender;
                        } catch (Throwable th) {
                            th = th;
                            Binder.restoreCallingIdentity(identity);
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
                throw th;
            }
        }
    }

    public boolean bindAppWidgetId(String callingPackage, int appWidgetId, int providerProfileId, ComponentName providerComponent, Bundle options) {
        int userId = UserHandle.getCallingUserId();
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        if (this.mSecurityPolicy.isEnabledGroupProfile(providerProfileId) && this.mSecurityPolicy.isProviderInCallerOrInProfileAndWhitelListed(providerComponent.getPackageName(), providerProfileId)) {
            synchronized (this.mLock) {
                ensureGroupStateLoadedLocked(userId);
                if (this.mSecurityPolicy.hasCallerBindPermissionOrBindWhiteListedLocked(callingPackage)) {
                    Widget widget = lookupWidgetLocked(appWidgetId, Binder.getCallingUid(), callingPackage);
                    if (widget == null) {
                        Slog.e(TAG, "Bad widget id " + appWidgetId);
                        return false;
                    } else if (widget.provider != null) {
                        Slog.e(TAG, "Widget id " + appWidgetId + " already bound to: " + widget.provider.id);
                        return false;
                    } else {
                        int providerUid = getUidForPackage(providerComponent.getPackageName(), providerProfileId);
                        if (providerUid < 0) {
                            Slog.e(TAG, "Package " + providerComponent.getPackageName() + " not installed  for profile " + providerProfileId);
                            return false;
                        }
                        ProviderId providerId = new ProviderId(providerUid, providerComponent);
                        Provider provider = lookupProviderLocked(providerId);
                        if (provider == null) {
                            Slog.e(TAG, "No widget provider " + providerComponent + " for profile " + providerProfileId);
                            return false;
                        } else if (provider.zombie) {
                            Slog.e(TAG, "Can't bind to a 3rd party provider in safe mode " + provider);
                            return false;
                        } else {
                            widget.provider = provider;
                            widget.options = options != null ? cloneIfLocalBinder(options) : new Bundle();
                            if (!widget.options.containsKey("appWidgetCategory")) {
                                widget.options.putInt("appWidgetCategory", 1);
                            }
                            provider.widgets.add(widget);
                            onWidgetProviderAddedOrChangedLocked(widget);
                            hookAppWidget(widget, 0, "bind app widget");
                            int widgetCount = provider.widgets.size();
                            if (widgetCount == 1) {
                                sendEnableIntentLocked(provider);
                            }
                            sendUpdateIntentLocked(provider, new int[]{appWidgetId});
                            registerForBroadcastsLocked(provider, getWidgetIds(provider.widgets));
                            saveGroupStateAsync(userId);
                            return true;
                        }
                    }
                }
                return false;
            }
        }
        return false;
    }

    public int[] getAppWidgetIds(ComponentName componentName) {
        int userId = UserHandle.getCallingUserId();
        this.mSecurityPolicy.enforceCallFromPackage(componentName.getPackageName());
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            ProviderId providerId = new ProviderId(Binder.getCallingUid(), componentName);
            Provider provider = lookupProviderLocked(providerId);
            if (provider != null) {
                return getWidgetIds(provider.widgets);
            }
            return new int[0];
        }
    }

    public int[] getAppWidgetIdsForHost(String callingPackage, int hostId) {
        int userId = UserHandle.getCallingUserId();
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            HostId id = new HostId(Binder.getCallingUid(), hostId, callingPackage);
            Host host = lookupHostLocked(id);
            if (host != null) {
                return getWidgetIds(host.widgets);
            }
            return new int[0];
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1252=4] */
    public boolean bindRemoteViewsService(String callingPackage, int appWidgetId, Intent intent, IApplicationThread caller, IBinder activtiyToken, IServiceConnection connection, int flags) {
        int userId = UserHandle.getCallingUserId();
        synchronized (this.mLock) {
            try {
                try {
                    ensureGroupStateLoadedLocked(userId);
                    try {
                        Widget widget = lookupWidgetLocked(appWidgetId, Binder.getCallingUid(), callingPackage);
                        if (widget != null) {
                            if (widget.provider != null) {
                                ComponentName componentName = intent.getComponent();
                                String providerPackage = widget.provider.id.componentName.getPackageName();
                                String servicePackage = componentName.getPackageName();
                                if (servicePackage.equals(providerPackage)) {
                                    this.mSecurityPolicy.enforceServiceExistsAndRequiresBindRemoteViewsPermission(componentName, widget.provider.getUserId());
                                    long callingIdentity = Binder.clearCallingIdentity();
                                    try {
                                        try {
                                            if (ActivityManager.getService().bindService(caller, activtiyToken, intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), connection, flags & 33554433, this.mContext.getOpPackageName(), widget.provider.getUserId()) != 0) {
                                                incrementAppWidgetServiceRefCount(appWidgetId, Pair.create(Integer.valueOf(widget.provider.id.uid), new Intent.FilterComparison(intent)));
                                                Binder.restoreCallingIdentity(callingIdentity);
                                                return true;
                                            }
                                        } catch (RemoteException e) {
                                        } catch (Throwable th) {
                                            th = th;
                                            Binder.restoreCallingIdentity(callingIdentity);
                                            throw th;
                                        }
                                    } catch (RemoteException e2) {
                                    } catch (Throwable th2) {
                                        th = th2;
                                    }
                                    Binder.restoreCallingIdentity(callingIdentity);
                                    return false;
                                }
                                throw new SecurityException("The taget service not in the same package as the widget provider");
                            }
                            throw new IllegalArgumentException("No provider for widget " + appWidgetId);
                        }
                        throw new IllegalArgumentException("Bad widget id");
                    } catch (Throwable th3) {
                        th = th3;
                        throw th;
                    }
                } catch (Throwable th4) {
                    th = th4;
                }
            } catch (Throwable th5) {
                th = th5;
            }
        }
    }

    public void deleteHost(String callingPackage, int hostId) {
        int userId = UserHandle.getCallingUserId();
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            HostId id = new HostId(Binder.getCallingUid(), hostId, callingPackage);
            Host host = lookupHostLocked(id);
            if (host == null) {
                return;
            }
            deleteHostLocked(host);
            saveGroupStateAsync(userId);
        }
    }

    public void deleteAllHosts() {
        int userId = UserHandle.getCallingUserId();
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            boolean changed = false;
            int N = this.mHosts.size();
            for (int i = N - 1; i >= 0; i--) {
                Host host = this.mHosts.get(i);
                if (host.id.uid == Binder.getCallingUid()) {
                    deleteHostLocked(host);
                    changed = true;
                }
            }
            if (changed) {
                saveGroupStateAsync(userId);
            }
        }
    }

    public AppWidgetProviderInfo getAppWidgetInfo(String callingPackage, int appWidgetId) {
        int userId = UserHandle.getCallingUserId();
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            Widget widget = lookupWidgetLocked(appWidgetId, Binder.getCallingUid(), callingPackage);
            if (widget != null && widget.provider != null && !widget.provider.zombie) {
                return cloneIfLocalBinder(widget.provider.getInfoLocked(this.mContext));
            }
            return null;
        }
    }

    public RemoteViews getAppWidgetViews(String callingPackage, int appWidgetId) {
        int userId = UserHandle.getCallingUserId();
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            Widget widget = lookupWidgetLocked(appWidgetId, Binder.getCallingUid(), callingPackage);
            if (widget != null) {
                return cloneIfLocalBinder(widget.getEffectiveViewsLocked());
            }
            return null;
        }
    }

    public void updateAppWidgetOptions(String callingPackage, int appWidgetId, Bundle options) {
        int userId = UserHandle.getCallingUserId();
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            Widget widget = lookupWidgetLocked(appWidgetId, Binder.getCallingUid(), callingPackage);
            if (widget == null) {
                return;
            }
            widget.options.putAll(options);
            sendOptionsChangedIntentLocked(widget);
            saveGroupStateAsync(userId);
        }
    }

    public Bundle getAppWidgetOptions(String callingPackage, int appWidgetId) {
        int userId = UserHandle.getCallingUserId();
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            Widget widget = lookupWidgetLocked(appWidgetId, Binder.getCallingUid(), callingPackage);
            if (widget != null && widget.options != null) {
                return cloneIfLocalBinder(widget.options);
            }
            return Bundle.EMPTY;
        }
    }

    public void updateAppWidgetIds(String callingPackage, int[] appWidgetIds, RemoteViews views) {
        updateAppWidgetIds(callingPackage, appWidgetIds, views, false);
    }

    public void partiallyUpdateAppWidgetIds(String callingPackage, int[] appWidgetIds, RemoteViews views) {
        updateAppWidgetIds(callingPackage, appWidgetIds, views, true);
    }

    public void notifyAppWidgetViewDataChanged(String callingPackage, int[] appWidgetIds, int viewId) {
        int userId = UserHandle.getCallingUserId();
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        if (appWidgetIds == null || appWidgetIds.length == 0) {
            return;
        }
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            for (int appWidgetId : appWidgetIds) {
                Widget widget = lookupWidgetLocked(appWidgetId, Binder.getCallingUid(), callingPackage);
                if (widget != null) {
                    scheduleNotifyAppWidgetViewDataChanged(widget, viewId);
                }
            }
        }
    }

    public void updateAppWidgetProvider(ComponentName componentName, RemoteViews views) {
        int userId = UserHandle.getCallingUserId();
        this.mSecurityPolicy.enforceCallFromPackage(componentName.getPackageName());
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            ProviderId providerId = new ProviderId(Binder.getCallingUid(), componentName);
            Provider provider = lookupProviderLocked(providerId);
            if (provider == null) {
                Slog.w(TAG, "Provider doesn't exist " + providerId);
                return;
            }
            ArrayList<Widget> instances = provider.widgets;
            int N = instances.size();
            for (int i = 0; i < N; i++) {
                Widget widget = instances.get(i);
                updateAppWidgetInstanceLocked(widget, views, false);
            }
        }
    }

    public void updateAppWidgetProviderInfo(ComponentName componentName, String metadataKey) {
        int userId = UserHandle.getCallingUserId();
        this.mSecurityPolicy.enforceCallFromPackage(componentName.getPackageName());
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            ProviderId providerId = new ProviderId(Binder.getCallingUid(), componentName);
            Provider provider = lookupProviderLocked(providerId);
            if (provider == null) {
                throw new IllegalArgumentException(componentName + " is not a valid AppWidget provider");
            }
            if (Objects.equals(provider.infoTag, metadataKey)) {
                return;
            }
            String keyToUse = metadataKey == null ? "android.appwidget.provider" : metadataKey;
            AppWidgetProviderInfo info = parseAppWidgetProviderInfo(this.mContext, providerId, provider.getPartialInfoLocked().providerInfo, keyToUse);
            if (info == null) {
                throw new IllegalArgumentException("Unable to parse " + keyToUse + " meta-data to a valid AppWidget provider");
            }
            provider.setInfoLocked(info);
            provider.infoTag = metadataKey;
            int N = provider.widgets.size();
            for (int i = 0; i < N; i++) {
                Widget widget = provider.widgets.get(i);
                scheduleNotifyProviderChangedLocked(widget);
                updateAppWidgetInstanceLocked(widget, widget.views, false);
            }
            saveGroupStateAsync(userId);
            scheduleNotifyGroupHostsForProvidersChangedLocked(userId);
        }
    }

    public boolean isRequestPinAppWidgetSupported() {
        synchronized (this.mLock) {
            if (this.mSecurityPolicy.isCallerInstantAppLocked()) {
                Slog.w(TAG, "Instant uid " + Binder.getCallingUid() + " query information about app widgets");
                return false;
            }
            return ((ShortcutServiceInternal) LocalServices.getService(ShortcutServiceInternal.class)).isRequestPinItemSupported(UserHandle.getCallingUserId(), 2);
        }
    }

    public boolean requestPinAppWidget(String callingPackage, ComponentName componentName, Bundle extras, IntentSender resultSender) {
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getUserId(callingUid);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            Provider provider = lookupProviderLocked(new ProviderId(callingUid, componentName));
            if (provider != null && !provider.zombie) {
                AppWidgetProviderInfo info = provider.getInfoLocked(this.mContext);
                if ((info.widgetCategory & 1) == 0) {
                    return false;
                }
                return ((ShortcutServiceInternal) LocalServices.getService(ShortcutServiceInternal.class)).requestPinAppWidget(callingPackage, info, extras, resultSender, userId);
            }
            return false;
        }
    }

    public ParceledListSlice<AppWidgetProviderInfo> getInstalledProvidersForProfile(int categoryFilter, int profileId, String packageName) {
        boolean inPackage;
        AppWidgetProviderInfo info;
        int providerProfileId;
        int userId = UserHandle.getCallingUserId();
        int callingUid = Binder.getCallingUid();
        if (!this.mSecurityPolicy.isEnabledGroupProfile(profileId)) {
            return null;
        }
        synchronized (this.mLock) {
            if (this.mSecurityPolicy.isCallerInstantAppLocked()) {
                Slog.w(TAG, "Instant uid " + callingUid + " cannot access widget providers");
                return ParceledListSlice.emptyList();
            }
            ensureGroupStateLoadedLocked(userId);
            ArrayList<AppWidgetProviderInfo> result = new ArrayList<>();
            int providerCount = this.mProviders.size();
            for (int i = 0; i < providerCount; i++) {
                Provider provider = this.mProviders.get(i);
                String providerPackageName = provider.id.componentName.getPackageName();
                if (packageName != null && !providerPackageName.equals(packageName)) {
                    inPackage = false;
                    if (!provider.zombie && inPackage) {
                        info = provider.getInfoLocked(this.mContext);
                        if ((info.widgetCategory & categoryFilter) != 0 && (providerProfileId = info.getProfile().getIdentifier()) == profileId && this.mSecurityPolicy.isProviderInCallerOrInProfileAndWhitelListed(providerPackageName, providerProfileId) && !this.mPackageManagerInternal.filterAppAccess(providerPackageName, callingUid, userId)) {
                            result.add(cloneIfLocalBinder(info));
                        }
                    }
                }
                inPackage = true;
                if (!provider.zombie) {
                    info = provider.getInfoLocked(this.mContext);
                    if ((info.widgetCategory & categoryFilter) != 0) {
                        result.add(cloneIfLocalBinder(info));
                    }
                }
            }
            return new ParceledListSlice<>(result);
        }
    }

    private void updateAppWidgetIds(String callingPackage, int[] appWidgetIds, RemoteViews views, boolean partially) {
        int userId = UserHandle.getCallingUserId();
        if (appWidgetIds == null || appWidgetIds.length == 0) {
            return;
        }
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            for (int appWidgetId : appWidgetIds) {
                Widget widget = lookupWidgetLocked(appWidgetId, Binder.getCallingUid(), callingPackage);
                if (widget != null) {
                    updateAppWidgetInstanceLocked(widget, views, partially);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int incrementAndGetAppWidgetIdLocked(int userId) {
        int appWidgetId = peekNextAppWidgetIdLocked(userId) + 1;
        this.mNextAppWidgetIds.put(userId, appWidgetId);
        return appWidgetId;
    }

    private void setMinAppWidgetIdLocked(int userId, int minWidgetId) {
        int nextAppWidgetId = peekNextAppWidgetIdLocked(userId);
        if (nextAppWidgetId < minWidgetId) {
            this.mNextAppWidgetIds.put(userId, minWidgetId);
        }
    }

    private int peekNextAppWidgetIdLocked(int userId) {
        if (this.mNextAppWidgetIds.indexOfKey(userId) < 0) {
            return 1;
        }
        return this.mNextAppWidgetIds.get(userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Host lookupOrAddHostLocked(HostId id) {
        Host host = lookupHostLocked(id);
        if (host != null) {
            return host;
        }
        Host host2 = new Host();
        host2.id = id;
        this.mHosts.add(host2);
        return host2;
    }

    private void deleteHostLocked(Host host) {
        int N = host.widgets.size();
        for (int i = N - 1; i >= 0; i--) {
            Widget widget = host.widgets.remove(i);
            deleteAppWidgetLocked(widget);
        }
        this.mHosts.remove(host);
        host.callbacks = null;
    }

    private void deleteAppWidgetLocked(Widget widget) {
        decrementAppWidgetServiceRefCount(widget);
        Host host = widget.host;
        host.widgets.remove(widget);
        pruneHostLocked(host);
        removeWidgetLocked(widget);
        Provider provider = widget.provider;
        if (provider != null) {
            provider.widgets.remove(widget);
            if (!provider.zombie) {
                sendDeletedIntentLocked(widget);
                if (provider.widgets.isEmpty()) {
                    cancelBroadcastsLocked(provider);
                    sendDisabledIntentLocked(provider);
                }
            }
        }
    }

    private void cancelBroadcastsLocked(Provider provider) {
        if (provider.broadcast != null) {
            final PendingIntent broadcast = provider.broadcast;
            this.mSaveStateHandler.post(new Runnable() { // from class: com.android.server.appwidget.AppWidgetServiceImpl$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    AppWidgetServiceImpl.this.m1733x10038e53(broadcast);
                }
            });
            provider.broadcast = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$cancelBroadcastsLocked$0$com-android-server-appwidget-AppWidgetServiceImpl  reason: not valid java name */
    public /* synthetic */ void m1733x10038e53(PendingIntent broadcast) {
        this.mAlarmManager.cancel(broadcast);
        broadcast.cancel();
    }

    private void destroyRemoteViewsService(final Intent intent, Widget widget) {
        ServiceConnection conn = new ServiceConnection() { // from class: com.android.server.appwidget.AppWidgetServiceImpl.2
            @Override // android.content.ServiceConnection
            public void onServiceConnected(ComponentName name, IBinder service) {
                IRemoteViewsFactory cb = IRemoteViewsFactory.Stub.asInterface(service);
                try {
                    cb.onDestroy(intent);
                } catch (RemoteException re) {
                    Slog.e(AppWidgetServiceImpl.TAG, "Error calling remove view factory", re);
                }
                AppWidgetServiceImpl.this.mContext.unbindService(this);
            }

            @Override // android.content.ServiceConnection
            public void onServiceDisconnected(ComponentName name) {
            }
        };
        long token = Binder.clearCallingIdentity();
        try {
            this.mContext.bindServiceAsUser(intent, conn, 33554433, widget.provider.id.getProfile());
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void incrementAppWidgetServiceRefCount(int appWidgetId, Pair<Integer, Intent.FilterComparison> serviceId) {
        HashSet<Integer> appWidgetIds;
        if (this.mRemoteViewsServicesAppWidgets.containsKey(serviceId)) {
            appWidgetIds = this.mRemoteViewsServicesAppWidgets.get(serviceId);
        } else {
            appWidgetIds = new HashSet<>();
            this.mRemoteViewsServicesAppWidgets.put(serviceId, appWidgetIds);
        }
        appWidgetIds.add(Integer.valueOf(appWidgetId));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void decrementAppWidgetServiceRefCount(Widget widget) {
        Iterator<Pair<Integer, Intent.FilterComparison>> it = this.mRemoteViewsServicesAppWidgets.keySet().iterator();
        while (it.hasNext()) {
            Pair<Integer, Intent.FilterComparison> key = it.next();
            HashSet<Integer> ids = this.mRemoteViewsServicesAppWidgets.get(key);
            if (ids.remove(Integer.valueOf(widget.appWidgetId)) && ids.isEmpty()) {
                destroyRemoteViewsService(((Intent.FilterComparison) key.second).getIntent(), widget);
                it.remove();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void saveGroupStateAsync(int groupId) {
        this.mSaveStateHandler.post(new SaveStateRunnable(groupId));
    }

    private void updateAppWidgetInstanceLocked(Widget widget, RemoteViews views, boolean isPartialUpdate) {
        int memoryUsage;
        if (widget != null && widget.provider != null && !widget.provider.zombie && !widget.host.zombie) {
            if (isPartialUpdate && widget.views != null) {
                widget.views.mergeRemoteViews(views);
            } else {
                widget.views = views;
            }
            if (UserHandle.getAppId(Binder.getCallingUid()) != 1000 && widget.views != null && (memoryUsage = widget.views.estimateMemoryUsage()) > this.mMaxWidgetBitmapMemory) {
                widget.views = null;
                throw new IllegalArgumentException("RemoteViews for widget update exceeds maximum bitmap memory usage (used: " + memoryUsage + ", max: " + this.mMaxWidgetBitmapMemory + ")");
            }
            if (widget.views == null) {
                hookAppWidget(widget, 2, "widget.views is null, remove this widget");
            } else {
                hookAppWidget(widget, 1, null);
            }
            scheduleNotifyUpdateAppWidgetLocked(widget, widget.getEffectiveViewsLocked());
        }
    }

    private void scheduleNotifyAppWidgetViewDataChanged(Widget widget, int viewId) {
        if (viewId == 0 || viewId == 1) {
            return;
        }
        long requestId = UPDATE_COUNTER.incrementAndGet();
        if (widget != null) {
            widget.updateSequenceNos.put(viewId, requestId);
        }
        if (widget == null || widget.host == null || widget.host.zombie || widget.host.callbacks == null || widget.provider == null || widget.provider.zombie) {
            return;
        }
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = widget.host;
        args.arg2 = widget.host.callbacks;
        args.arg3 = Long.valueOf(requestId);
        args.argi1 = widget.appWidgetId;
        args.argi2 = viewId;
        this.mCallbackHandler.obtainMessage(4, args).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleNotifyAppWidgetViewDataChanged(Host host, IAppWidgetHost callbacks, int appWidgetId, int viewId, long requestId) {
        try {
            callbacks.viewDataChanged(appWidgetId, viewId);
            host.lastWidgetUpdateSequenceNo = requestId;
        } catch (RemoteException e) {
            callbacks = null;
        }
        synchronized (this.mLock) {
            if (callbacks == null) {
                host.callbacks = null;
                Set<Pair<Integer, Intent.FilterComparison>> keys = this.mRemoteViewsServicesAppWidgets.keySet();
                for (Pair<Integer, Intent.FilterComparison> key : keys) {
                    if (this.mRemoteViewsServicesAppWidgets.get(key).contains(Integer.valueOf(appWidgetId))) {
                        ServiceConnection connection = new ServiceConnection() { // from class: com.android.server.appwidget.AppWidgetServiceImpl.3
                            @Override // android.content.ServiceConnection
                            public void onServiceConnected(ComponentName name, IBinder service) {
                                IRemoteViewsFactory cb = IRemoteViewsFactory.Stub.asInterface(service);
                                try {
                                    cb.onDataSetChangedAsync();
                                } catch (RemoteException e2) {
                                    Slog.e(AppWidgetServiceImpl.TAG, "Error calling onDataSetChangedAsync()", e2);
                                }
                                AppWidgetServiceImpl.this.mContext.unbindService(this);
                            }

                            @Override // android.content.ServiceConnection
                            public void onServiceDisconnected(ComponentName name) {
                            }
                        };
                        int userId = UserHandle.getUserId(((Integer) key.first).intValue());
                        Intent intent = ((Intent.FilterComparison) key.second).getIntent();
                        bindService(intent, connection, new UserHandle(userId));
                    }
                }
            }
        }
    }

    private void scheduleNotifyUpdateAppWidgetLocked(Widget widget, RemoteViews updateViews) {
        long requestId = UPDATE_COUNTER.incrementAndGet();
        if (widget != null) {
            if (widget.trackingUpdate) {
                widget.trackingUpdate = false;
                Log.i(TAG, "Widget update received " + widget.toString());
                Trace.asyncTraceEnd(64L, "appwidget update-intent " + widget.provider.id.toString(), widget.appWidgetId);
            }
            widget.updateSequenceNos.put(0, requestId);
        }
        if (widget == null || widget.provider == null || widget.provider.zombie || widget.host.callbacks == null || widget.host.zombie) {
            return;
        }
        if (updateViews != null) {
            updateViews = new RemoteViews(updateViews);
            updateViews.setProviderInstanceId(requestId);
        }
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = widget.host;
        args.arg2 = widget.host.callbacks;
        args.arg3 = updateViews;
        args.arg4 = Long.valueOf(requestId);
        args.argi1 = widget.appWidgetId;
        this.mCallbackHandler.obtainMessage(1, args).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleNotifyUpdateAppWidget(Host host, IAppWidgetHost callbacks, int appWidgetId, RemoteViews views, long requestId) {
        try {
            callbacks.updateAppWidget(appWidgetId, views);
            host.lastWidgetUpdateSequenceNo = requestId;
        } catch (RemoteException re) {
            synchronized (this.mLock) {
                Slog.e(TAG, "Widget host dead: " + host.id, re);
                host.callbacks = null;
            }
        }
    }

    private void scheduleNotifyProviderChangedLocked(Widget widget) {
        long requestId = UPDATE_COUNTER.incrementAndGet();
        if (widget != null) {
            widget.updateSequenceNos.clear();
            widget.updateSequenceNos.append(1, requestId);
        }
        if (widget == null || widget.provider == null || widget.provider.zombie || widget.host.callbacks == null || widget.host.zombie) {
            return;
        }
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = widget.host;
        args.arg2 = widget.host.callbacks;
        args.arg3 = widget.provider.getInfoLocked(this.mContext);
        args.arg4 = Long.valueOf(requestId);
        args.argi1 = widget.appWidgetId;
        this.mCallbackHandler.obtainMessage(2, args).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleNotifyProviderChanged(Host host, IAppWidgetHost callbacks, int appWidgetId, AppWidgetProviderInfo info, long requestId) {
        try {
            callbacks.providerChanged(appWidgetId, info);
            host.lastWidgetUpdateSequenceNo = requestId;
        } catch (RemoteException re) {
            synchronized (this.mLock) {
                Slog.e(TAG, "Widget host dead: " + host.id, re);
                host.callbacks = null;
            }
        }
    }

    private void scheduleNotifyAppWidgetRemovedLocked(Widget widget) {
        long requestId = UPDATE_COUNTER.incrementAndGet();
        if (widget != null) {
            if (widget.trackingUpdate) {
                widget.trackingUpdate = false;
                Log.i(TAG, "Widget removed " + widget.toString());
                Trace.asyncTraceEnd(64L, "appwidget update-intent " + widget.provider.id.toString(), widget.appWidgetId);
            }
            widget.updateSequenceNos.clear();
        }
        if (widget == null || widget.provider == null || widget.provider.zombie || widget.host.callbacks == null || widget.host.zombie) {
            return;
        }
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = widget.host;
        args.arg2 = widget.host.callbacks;
        args.arg3 = Long.valueOf(requestId);
        args.argi1 = widget.appWidgetId;
        this.mCallbackHandler.obtainMessage(5, args).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleNotifyAppWidgetRemoved(Host host, IAppWidgetHost callbacks, int appWidgetId, long requestId) {
        try {
            callbacks.appWidgetRemoved(appWidgetId);
            host.lastWidgetUpdateSequenceNo = requestId;
        } catch (RemoteException re) {
            synchronized (this.mLock) {
                Slog.e(TAG, "Widget host dead: " + host.id, re);
                host.callbacks = null;
            }
        }
    }

    private void scheduleNotifyGroupHostsForProvidersChangedLocked(int userId) {
        int[] profileIds = this.mSecurityPolicy.getEnabledGroupProfileIds(userId);
        int N = this.mHosts.size();
        for (int i = N - 1; i >= 0; i--) {
            Host host = this.mHosts.get(i);
            boolean hostInGroup = false;
            int M = profileIds.length;
            int j = 0;
            while (true) {
                if (j >= M) {
                    break;
                }
                int profileId = profileIds[j];
                if (host.getUserId() != profileId) {
                    j++;
                } else {
                    hostInGroup = true;
                    break;
                }
            }
            if (hostInGroup && host != null && !host.zombie && host.callbacks != null) {
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = host;
                args.arg2 = host.callbacks;
                this.mCallbackHandler.obtainMessage(3, args).sendToTarget();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleNotifyProvidersChanged(Host host, IAppWidgetHost callbacks) {
        try {
            callbacks.providersChanged();
        } catch (RemoteException re) {
            synchronized (this.mLock) {
                Slog.e(TAG, "Widget host dead: " + host.id, re);
                host.callbacks = null;
            }
        }
    }

    private static boolean isLocalBinder() {
        return Process.myPid() == Binder.getCallingPid();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static RemoteViews cloneIfLocalBinder(RemoteViews rv) {
        if (isLocalBinder() && rv != null) {
            return rv.clone();
        }
        return rv;
    }

    private static AppWidgetProviderInfo cloneIfLocalBinder(AppWidgetProviderInfo info) {
        if (isLocalBinder() && info != null) {
            return info.clone();
        }
        return info;
    }

    private static Bundle cloneIfLocalBinder(Bundle bundle) {
        if (isLocalBinder() && bundle != null) {
            return (Bundle) bundle.clone();
        }
        return bundle;
    }

    private Widget lookupWidgetLocked(int appWidgetId, int uid, String packageName) {
        int N = this.mWidgets.size();
        for (int i = 0; i < N; i++) {
            Widget widget = this.mWidgets.get(i);
            if (widget.appWidgetId == appWidgetId && this.mSecurityPolicy.canAccessAppWidget(widget, uid, packageName)) {
                return widget;
            }
        }
        return null;
    }

    private Provider lookupProviderLocked(ProviderId id) {
        int N = this.mProviders.size();
        for (int i = 0; i < N; i++) {
            Provider provider = this.mProviders.get(i);
            if (provider.id.equals(id)) {
                return provider;
            }
        }
        return null;
    }

    private Host lookupHostLocked(HostId hostId) {
        int N = this.mHosts.size();
        for (int i = 0; i < N; i++) {
            Host host = this.mHosts.get(i);
            if (host.id.equals(hostId)) {
                return host;
            }
        }
        return null;
    }

    private void pruneHostLocked(Host host) {
        if (host.widgets.size() == 0 && host.callbacks == null) {
            this.mHosts.remove(host);
        }
    }

    private void loadGroupWidgetProvidersLocked(int[] profileIds) {
        List<ResolveInfo> allReceivers = null;
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_UPDATE");
        for (int profileId : profileIds) {
            List<ResolveInfo> receivers = queryIntentReceivers(intent, profileId);
            if (receivers != null && !receivers.isEmpty()) {
                if (allReceivers == null) {
                    allReceivers = new ArrayList<>();
                }
                allReceivers.addAll(receivers);
            }
        }
        int N = allReceivers == null ? 0 : allReceivers.size();
        for (int i = 0; i < N; i++) {
            ResolveInfo receiver = allReceivers.get(i);
            addProviderLocked(receiver);
        }
    }

    private boolean addProviderLocked(ResolveInfo ri) {
        if ((ri.activityInfo.applicationInfo.flags & 262144) != 0) {
            return false;
        }
        ComponentName componentName = new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
        ProviderId providerId = new ProviderId(ri.activityInfo.applicationInfo.uid, componentName);
        Provider existing = lookupProviderLocked(providerId);
        if (existing == null) {
            ProviderId restoredProviderId = new ProviderId(-1, componentName);
            existing = lookupProviderLocked(restoredProviderId);
        }
        AppWidgetProviderInfo info = createPartialProviderInfo(providerId, ri, existing);
        if (info != null) {
            if (existing != null) {
                if (existing.zombie && !this.mSafeMode) {
                    existing.id = providerId;
                    existing.zombie = false;
                    existing.setPartialInfoLocked(info);
                    return true;
                }
                return true;
            }
            Provider provider = new Provider();
            provider.id = providerId;
            provider.setPartialInfoLocked(info);
            this.mProviders.add(provider);
            return true;
        }
        return false;
    }

    private void deleteWidgetsLocked(Provider provider, int userId) {
        int N = provider.widgets.size();
        for (int i = N - 1; i >= 0; i--) {
            Widget widget = provider.widgets.get(i);
            if (userId == -1 || userId == widget.host.getUserId()) {
                provider.widgets.remove(i);
                updateAppWidgetInstanceLocked(widget, null, false);
                widget.host.widgets.remove(widget);
                removeWidgetLocked(widget);
                widget.provider = null;
                pruneHostLocked(widget.host);
                widget.host = null;
            }
        }
    }

    private void deleteProviderLocked(Provider provider) {
        deleteWidgetsLocked(provider, -1);
        this.mProviders.remove(provider);
        cancelBroadcastsLocked(provider);
    }

    private void sendEnableIntentLocked(Provider p) {
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_ENABLED");
        intent.setComponent(p.id.componentName);
        sendBroadcastAsUser(intent, p.id.getProfile());
    }

    private void sendUpdateIntentLocked(Provider provider, int[] appWidgetIds) {
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_UPDATE");
        intent.putExtra("appWidgetIds", appWidgetIds);
        intent.setComponent(provider.id.componentName);
        sendBroadcastAsUser(intent, provider.id.getProfile());
    }

    private void sendDeletedIntentLocked(Widget widget) {
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_DELETED");
        intent.setComponent(widget.provider.id.componentName);
        intent.putExtra("appWidgetId", widget.appWidgetId);
        sendBroadcastAsUser(intent, widget.provider.id.getProfile());
    }

    private void sendDisabledIntentLocked(Provider provider) {
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_DISABLED");
        intent.setComponent(provider.id.componentName);
        sendBroadcastAsUser(intent, provider.id.getProfile());
    }

    public void sendOptionsChangedIntentLocked(Widget widget) {
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_UPDATE_OPTIONS");
        intent.setComponent(widget.provider.id.componentName);
        intent.putExtra("appWidgetId", widget.appWidgetId);
        intent.putExtra("appWidgetOptions", widget.options);
        sendBroadcastAsUser(intent, widget.provider.id.getProfile());
    }

    private void registerForBroadcastsLocked(Provider provider, int[] appWidgetIds) {
        AppWidgetProviderInfo info = provider.getInfoLocked(this.mContext);
        if (info.updatePeriodMillis > 0) {
            boolean alreadyRegistered = provider.broadcast != null;
            Intent intent = new Intent("android.appwidget.action.APPWIDGET_UPDATE");
            intent.putExtra("appWidgetIds", appWidgetIds);
            intent.setComponent(info.provider);
            long token = Binder.clearCallingIdentity();
            try {
                provider.broadcast = PendingIntent.getBroadcastAsUser(this.mContext, 1, intent, AudioFormat.DTS_HD, info.getProfile());
                if (!alreadyRegistered) {
                    final long period = Math.max(info.updatePeriodMillis, (int) MIN_UPDATE_PERIOD);
                    final PendingIntent broadcast = provider.broadcast;
                    this.mSaveStateHandler.post(new Runnable() { // from class: com.android.server.appwidget.AppWidgetServiceImpl$$ExternalSyntheticLambda1
                        @Override // java.lang.Runnable
                        public final void run() {
                            AppWidgetServiceImpl.this.m1734x5eb192e6(period, broadcast);
                        }
                    });
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$registerForBroadcastsLocked$1$com-android-server-appwidget-AppWidgetServiceImpl  reason: not valid java name */
    public /* synthetic */ void m1734x5eb192e6(long period, PendingIntent broadcast) {
        this.mAlarmManager.setInexactRepeating(2, SystemClock.elapsedRealtime() + period, period, broadcast);
    }

    private static int[] getWidgetIds(ArrayList<Widget> widgets) {
        int instancesSize = widgets.size();
        int[] appWidgetIds = new int[instancesSize];
        for (int i = 0; i < instancesSize; i++) {
            appWidgetIds[i] = widgets.get(i).appWidgetId;
        }
        return appWidgetIds;
    }

    private static void dumpProviderLocked(Provider provider, int index, PrintWriter pw) {
        AppWidgetProviderInfo info = provider.getPartialInfoLocked();
        pw.print("  [");
        pw.print(index);
        pw.print("] provider ");
        pw.println(provider.id);
        pw.print("    min=(");
        pw.print(info.minWidth);
        pw.print("x");
        pw.print(info.minHeight);
        pw.print(")   minResize=(");
        pw.print(info.minResizeWidth);
        pw.print("x");
        pw.print(info.minResizeHeight);
        pw.print(") updatePeriodMillis=");
        pw.print(info.updatePeriodMillis);
        pw.print(" resizeMode=");
        pw.print(info.resizeMode);
        pw.print(" widgetCategory=");
        pw.print(info.widgetCategory);
        pw.print(" autoAdvanceViewId=");
        pw.print(info.autoAdvanceViewId);
        pw.print(" initialLayout=#");
        pw.print(Integer.toHexString(info.initialLayout));
        pw.print(" initialKeyguardLayout=#");
        pw.print(Integer.toHexString(info.initialKeyguardLayout));
        pw.print("   zombie=");
        pw.println(provider.zombie);
    }

    private static void dumpHost(Host host, int index, PrintWriter pw) {
        pw.print("  [");
        pw.print(index);
        pw.print("] hostId=");
        pw.println(host.id);
        pw.print("    callbacks=");
        pw.println(host.callbacks);
        pw.print("    widgets.size=");
        pw.print(host.widgets.size());
        pw.print(" zombie=");
        pw.println(host.zombie);
    }

    private static void dumpGrant(Pair<Integer, String> grant, int index, PrintWriter pw) {
        pw.print("  [");
        pw.print(index);
        pw.print(']');
        pw.print(" user=");
        pw.print(grant.first);
        pw.print(" package=");
        pw.println((String) grant.second);
    }

    private static void dumpWidget(Widget widget, int index, PrintWriter pw) {
        pw.print("  [");
        pw.print(index);
        pw.print("] id=");
        pw.println(widget.appWidgetId);
        pw.print("    host=");
        pw.println(widget.host.id);
        if (widget.provider != null) {
            pw.print("    provider=");
            pw.println(widget.provider.id);
        }
        if (widget.host != null) {
            pw.print("    host.callbacks=");
            pw.println(widget.host.callbacks);
        }
        if (widget.views != null) {
            pw.print("    views=");
            pw.println(widget.views);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void serializeProvider(TypedXmlSerializer out, Provider p) throws IOException {
        Objects.requireNonNull(out);
        Objects.requireNonNull(p);
        serializeProviderInner(out, p, false);
    }

    private static void serializeProviderWithProviderInfo(TypedXmlSerializer out, Provider p) throws IOException {
        Objects.requireNonNull(out);
        Objects.requireNonNull(p);
        serializeProviderInner(out, p, true);
    }

    private static void serializeProviderInner(TypedXmlSerializer out, Provider p, boolean persistsProviderInfo) throws IOException {
        Objects.requireNonNull(out);
        Objects.requireNonNull(p);
        out.startTag((String) null, "p");
        out.attribute((String) null, "pkg", p.id.componentName.getPackageName());
        out.attribute((String) null, "cl", p.id.componentName.getClassName());
        out.attributeIntHex((String) null, "tag", p.tag);
        if (!TextUtils.isEmpty(p.infoTag)) {
            out.attribute((String) null, "info_tag", p.infoTag);
        }
        if (persistsProviderInfo && !p.mInfoParsed) {
            Slog.d(TAG, "Provider info from " + p.id.componentName + " won't be persisted.");
        }
        if (persistsProviderInfo && p.mInfoParsed) {
            AppWidgetXmlUtil.writeAppWidgetProviderInfoLocked(out, p.info);
        }
        out.endTag((String) null, "p");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void serializeHost(TypedXmlSerializer out, Host host) throws IOException {
        out.startTag((String) null, "h");
        out.attribute((String) null, "pkg", host.id.packageName);
        out.attributeIntHex((String) null, "id", host.id.hostId);
        out.attributeIntHex((String) null, "tag", host.tag);
        out.endTag((String) null, "h");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void serializeAppWidget(TypedXmlSerializer out, Widget widget, boolean saveRestoreCompleted) throws IOException {
        out.startTag((String) null, "g");
        out.attributeIntHex((String) null, "id", widget.appWidgetId);
        out.attributeIntHex((String) null, "rid", widget.restoredId);
        out.attributeIntHex((String) null, "h", widget.host.tag);
        if (widget.provider != null) {
            out.attributeIntHex((String) null, "p", widget.provider.tag);
        }
        if (widget.options != null) {
            int minWidth = widget.options.getInt("appWidgetMinWidth");
            int minHeight = widget.options.getInt("appWidgetMinHeight");
            int maxWidth = widget.options.getInt("appWidgetMaxWidth");
            int maxHeight = widget.options.getInt("appWidgetMaxHeight");
            out.attributeIntHex((String) null, "min_width", minWidth > 0 ? minWidth : 0);
            out.attributeIntHex((String) null, "min_height", minHeight > 0 ? minHeight : 0);
            out.attributeIntHex((String) null, "max_width", maxWidth > 0 ? maxWidth : 0);
            out.attributeIntHex((String) null, "max_height", maxHeight > 0 ? maxHeight : 0);
            out.attributeIntHex((String) null, "host_category", widget.options.getInt("appWidgetCategory"));
            if (saveRestoreCompleted) {
                boolean restoreCompleted = widget.options.getBoolean("appWidgetRestoreCompleted");
                out.attributeBoolean((String) null, "restore_completed", restoreCompleted);
            }
        }
        out.endTag((String) null, "g");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Bundle parseWidgetIdOptions(TypedXmlPullParser parser) {
        Bundle options = new Bundle();
        boolean restoreCompleted = parser.getAttributeBoolean((String) null, "restore_completed", false);
        if (restoreCompleted) {
            options.putBoolean("appWidgetRestoreCompleted", true);
        }
        int minWidth = parser.getAttributeIntHex((String) null, "min_width", -1);
        if (minWidth != -1) {
            options.putInt("appWidgetMinWidth", minWidth);
        }
        int minHeight = parser.getAttributeIntHex((String) null, "min_height", -1);
        if (minHeight != -1) {
            options.putInt("appWidgetMinHeight", minHeight);
        }
        int maxWidth = parser.getAttributeIntHex((String) null, "max_width", -1);
        if (maxWidth != -1) {
            options.putInt("appWidgetMaxWidth", maxWidth);
        }
        int maxHeight = parser.getAttributeIntHex((String) null, "max_height", -1);
        if (maxHeight != -1) {
            options.putInt("appWidgetMaxHeight", maxHeight);
        }
        int category = parser.getAttributeIntHex((String) null, "host_category", -1);
        if (category != -1) {
            options.putInt("appWidgetCategory", category);
        }
        return options;
    }

    public List<String> getWidgetParticipants(int userId) {
        return this.mBackupRestoreController.getWidgetParticipants(userId);
    }

    public byte[] getWidgetState(String packageName, int userId) {
        return this.mBackupRestoreController.getWidgetState(packageName, userId);
    }

    public void systemRestoreStarting(int userId) {
        this.mBackupRestoreController.systemRestoreStarting(userId);
    }

    public void restoreWidgetState(String packageName, byte[] restoredState, int userId) {
        this.mBackupRestoreController.restoreWidgetState(packageName, restoredState, userId);
    }

    public void systemRestoreFinished(int userId) {
        this.mBackupRestoreController.systemRestoreFinished(userId);
    }

    private AppWidgetProviderInfo createPartialProviderInfo(ProviderId providerId, ResolveInfo ri, Provider provider) {
        boolean hasXmlDefinition = false;
        Bundle metaData = ri.activityInfo.metaData;
        if (metaData == null) {
            return null;
        }
        if (provider != null && !TextUtils.isEmpty(provider.infoTag)) {
            hasXmlDefinition = metaData.getInt(provider.infoTag) != 0;
        }
        if (!(hasXmlDefinition | (metaData.getInt("android.appwidget.provider") != 0))) {
            return null;
        }
        AppWidgetProviderInfo info = new AppWidgetProviderInfo();
        info.provider = providerId.componentName;
        info.providerInfo = ri.activityInfo;
        return info;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2706=4] */
    /* JADX INFO: Access modifiers changed from: private */
    public static AppWidgetProviderInfo parseAppWidgetProviderInfo(Context context, ProviderId providerId, ActivityInfo activityInfo, String metadataKey) {
        PackageManager pm = context.getPackageManager();
        try {
            XmlResourceParser parser = activityInfo.loadXmlMetaData(pm, metadataKey);
            if (parser == null) {
                Slog.w(TAG, "No " + metadataKey + " meta-data for AppWidget provider '" + providerId + '\'');
                if (parser != null) {
                    parser.close();
                }
                return null;
            }
            AttributeSet attrs = Xml.asAttributeSet(parser);
            while (true) {
                int type = parser.next();
                if (type == 1 || type == 2) {
                    break;
                }
            }
            String nodeName = parser.getName();
            if (!"appwidget-provider".equals(nodeName)) {
                Slog.w(TAG, "Meta-data does not start with appwidget-provider tag for AppWidget provider " + providerId.componentName + " for user " + providerId.uid);
                if (parser != null) {
                    parser.close();
                }
                return null;
            }
            AppWidgetProviderInfo info = new AppWidgetProviderInfo();
            info.provider = providerId.componentName;
            info.providerInfo = activityInfo;
            long identity = Binder.clearCallingIdentity();
            int userId = UserHandle.getUserId(providerId.uid);
            ApplicationInfo app = pm.getApplicationInfoAsUser(activityInfo.packageName, 0, userId);
            Resources resources = pm.getResourcesForApplication(app);
            Binder.restoreCallingIdentity(identity);
            TypedArray sa = resources.obtainAttributes(attrs, R.styleable.AppWidgetProviderInfo);
            TypedValue value = sa.peekValue(1);
            info.minWidth = value != null ? value.data : 0;
            TypedValue value2 = sa.peekValue(2);
            info.minHeight = value2 != null ? value2.data : 0;
            TypedValue value3 = sa.peekValue(9);
            info.minResizeWidth = value3 != null ? value3.data : info.minWidth;
            TypedValue value4 = sa.peekValue(10);
            info.minResizeHeight = value4 != null ? value4.data : info.minHeight;
            TypedValue value5 = sa.peekValue(15);
            info.maxResizeWidth = value5 != null ? value5.data : 0;
            TypedValue value6 = sa.peekValue(16);
            info.maxResizeHeight = value6 != null ? value6.data : 0;
            info.targetCellWidth = sa.getInt(17, 0);
            info.targetCellHeight = sa.getInt(18, 0);
            info.updatePeriodMillis = sa.getInt(3, 0);
            info.initialLayout = sa.getResourceId(4, 0);
            info.initialKeyguardLayout = sa.getResourceId(11, 0);
            String className = sa.getString(5);
            if (className != null) {
                info.configure = new ComponentName(providerId.componentName.getPackageName(), className);
            }
            info.label = activityInfo.loadLabel(pm).toString();
            info.icon = activityInfo.getIconResource();
            info.previewImage = sa.getResourceId(6, 0);
            info.previewLayout = sa.getResourceId(14, 0);
            info.autoAdvanceViewId = sa.getResourceId(7, -1);
            info.resizeMode = sa.getInt(8, 0);
            info.widgetCategory = sa.getInt(12, 1);
            info.widgetFeatures = sa.getInt(13, 0);
            info.descriptionRes = sa.getResourceId(0, 0);
            sa.recycle();
            if (parser != null) {
                parser.close();
            }
            return info;
        } catch (PackageManager.NameNotFoundException | IOException | XmlPullParserException e) {
            Slog.w(TAG, "XML parsing failed for AppWidget provider " + providerId.componentName + " for user " + providerId.uid, e);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getUidForPackage(String packageName, int userId) {
        PackageInfo pkgInfo = null;
        long identity = Binder.clearCallingIdentity();
        try {
            pkgInfo = this.mPackageManager.getPackageInfo(packageName, 0L, userId);
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
        Binder.restoreCallingIdentity(identity);
        if (pkgInfo == null || pkgInfo.applicationInfo == null) {
            return -1;
        }
        return pkgInfo.applicationInfo.uid;
    }

    private ActivityInfo getProviderInfo(ComponentName componentName, int userId) {
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_UPDATE");
        intent.setComponent(componentName);
        List<ResolveInfo> receivers = queryIntentReceivers(intent, userId);
        if (!receivers.isEmpty()) {
            return receivers.get(0).activityInfo;
        }
        return null;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2775=4] */
    private List<ResolveInfo> queryIntentReceivers(Intent intent, int userId) {
        long identity = Binder.clearCallingIdentity();
        int flags = 128 | 268435456;
        try {
            if (isProfileWithUnlockedParent(userId)) {
                flags |= 786432;
            }
            return this.mPackageManager.queryIntentReceivers(intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), flags | 1024, userId).getList();
        } catch (RemoteException e) {
            return Collections.emptyList();
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    void handleUserUnlocked(int userId) {
        if (isProfileWithLockedParent(userId)) {
            return;
        }
        if (!this.mUserManager.isUserUnlockingOrUnlocked(userId)) {
            Slog.w(TAG, "User " + userId + " is no longer unlocked - exiting");
            return;
        }
        long time = SystemClock.elapsedRealtime();
        synchronized (this.mLock) {
            Trace.traceBegin(64L, "appwidget ensure");
            ensureGroupStateLoadedLocked(userId);
            Trace.traceEnd(64L);
            Trace.traceBegin(64L, "appwidget reload");
            reloadWidgetsMaskedStateForGroup(this.mSecurityPolicy.getGroupParent(userId));
            Trace.traceEnd(64L);
            int N = this.mProviders.size();
            for (int i = 0; i < N; i++) {
                final Provider provider = this.mProviders.get(i);
                if (provider.getUserId() == userId && provider.widgets.size() > 0) {
                    Trace.traceBegin(64L, "appwidget init " + provider.id.componentName.getPackageName());
                    sendEnableIntentLocked(provider);
                    provider.widgets.forEach(new Consumer() { // from class: com.android.server.appwidget.AppWidgetServiceImpl$$ExternalSyntheticLambda0
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            AppWidgetServiceImpl.lambda$handleUserUnlocked$2(AppWidgetServiceImpl.Provider.this, (AppWidgetServiceImpl.Widget) obj);
                        }
                    });
                    int[] appWidgetIds = getWidgetIds(provider.widgets);
                    sendUpdateIntentLocked(provider, appWidgetIds);
                    registerForBroadcastsLocked(provider, appWidgetIds);
                    Trace.traceEnd(64L);
                }
            }
        }
        Slog.i(TAG, "Processing of handleUserUnlocked u" + userId + " took " + (SystemClock.elapsedRealtime() - time) + " ms");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$handleUserUnlocked$2(Provider provider, Widget widget) {
        widget.trackingUpdate = true;
        Trace.asyncTraceBegin(64L, "appwidget update-intent " + provider.id.toString(), widget.appWidgetId);
        Log.i(TAG, "Widget update scheduled on unlock " + widget.toString());
    }

    private void loadGroupStateLocked(int[] profileIds) {
        List<LoadedWidgetState> loadedWidgets = new ArrayList<>();
        int version = 0;
        for (int profileId : profileIds) {
            AtomicFile file = getSavedStateFile(profileId);
            try {
                FileInputStream stream = file.openRead();
                version = readProfileStateFromFileLocked(stream, profileId, loadedWidgets);
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
                Slog.w(TAG, "Failed to read state: " + e);
            }
        }
        if (version >= 0) {
            bindLoadedWidgetsLocked(loadedWidgets);
            performUpgradeLocked(version);
            return;
        }
        Slog.w(TAG, "Failed to read state, clearing widgets and hosts.");
        clearWidgetsLocked();
        this.mHosts.clear();
        int N = this.mProviders.size();
        for (int i = 0; i < N; i++) {
            this.mProviders.get(i).widgets.clear();
        }
    }

    private void bindLoadedWidgetsLocked(List<LoadedWidgetState> loadedWidgets) {
        int loadedWidgetCount = loadedWidgets.size();
        for (int i = loadedWidgetCount - 1; i >= 0; i--) {
            LoadedWidgetState loadedWidget = loadedWidgets.remove(i);
            Widget widget = loadedWidget.widget;
            widget.provider = findProviderByTag(loadedWidget.providerTag);
            if (widget.provider != null) {
                widget.host = findHostByTag(loadedWidget.hostTag);
                if (widget.host != null) {
                    widget.provider.widgets.add(widget);
                    widget.host.widgets.add(widget);
                    addWidgetLocked(widget);
                }
            }
        }
    }

    private Provider findProviderByTag(int tag) {
        if (tag < 0) {
            return null;
        }
        int providerCount = this.mProviders.size();
        for (int i = 0; i < providerCount; i++) {
            Provider provider = this.mProviders.get(i);
            if (provider.tag == tag) {
                return provider;
            }
        }
        return null;
    }

    private Host findHostByTag(int tag) {
        if (tag < 0) {
            return null;
        }
        int hostCount = this.mHosts.size();
        for (int i = 0; i < hostCount; i++) {
            Host host = this.mHosts.get(i);
            if (host.tag == tag) {
                return host;
            }
        }
        return null;
    }

    void addWidgetLocked(Widget widget) {
        this.mWidgets.add(widget);
        onWidgetProviderAddedOrChangedLocked(widget);
        hookAppWidget(widget, 0, null);
    }

    void onWidgetProviderAddedOrChangedLocked(Widget widget) {
        if (widget.provider == null) {
            return;
        }
        int userId = widget.provider.getUserId();
        synchronized (this.mWidgetPackagesLock) {
            ArraySet<String> packages = this.mWidgetPackages.get(userId);
            if (packages == null) {
                SparseArray<ArraySet<String>> sparseArray = this.mWidgetPackages;
                ArraySet<String> arraySet = new ArraySet<>();
                packages = arraySet;
                sparseArray.put(userId, arraySet);
            }
            packages.add(widget.provider.id.componentName.getPackageName());
        }
        if (widget.provider.isMaskedLocked()) {
            maskWidgetsViewsLocked(widget.provider, widget);
        } else {
            widget.clearMaskedViewsLocked();
        }
    }

    void removeWidgetLocked(Widget widget) {
        this.mWidgets.remove(widget);
        onWidgetRemovedLocked(widget);
        scheduleNotifyAppWidgetRemovedLocked(widget);
        hookAppWidget(widget, 2, "remove widget[" + widget.appWidgetId + "]");
    }

    private void onWidgetRemovedLocked(Widget widget) {
        if (widget.provider == null) {
            return;
        }
        int userId = widget.provider.getUserId();
        String packageName = widget.provider.id.componentName.getPackageName();
        synchronized (this.mWidgetPackagesLock) {
            ArraySet<String> packages = this.mWidgetPackages.get(userId);
            if (packages == null) {
                return;
            }
            int N = this.mWidgets.size();
            for (int i = 0; i < N; i++) {
                Widget w = this.mWidgets.get(i);
                if (w.provider != null && w.provider.getUserId() == userId && packageName.equals(w.provider.id.componentName.getPackageName())) {
                    return;
                }
            }
            packages.remove(packageName);
        }
    }

    void clearWidgetsLocked() {
        this.mWidgets.clear();
        onWidgetsClearedLocked();
        ITranAppWidgetServiceImpl.Instance().hookClearWidgetsLocked();
    }

    private void onWidgetsClearedLocked() {
        synchronized (this.mWidgetPackagesLock) {
            this.mWidgetPackages.clear();
        }
    }

    public boolean isBoundWidgetPackage(String packageName, int userId) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system process can call this");
        }
        synchronized (this.mWidgetPackagesLock) {
            ArraySet<String> packages = this.mWidgetPackages.get(userId);
            if (packages != null) {
                return packages.contains(packageName);
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void saveStateLocked(int userId) {
        tagProvidersAndHosts();
        int[] profileIds = this.mSecurityPolicy.getEnabledGroupProfileIds(userId);
        for (int profileId : profileIds) {
            AtomicFile file = getSavedStateFile(profileId);
            try {
                FileOutputStream stream = file.startWrite();
                if (writeProfileStateToFileLocked(stream, profileId)) {
                    file.finishWrite(stream);
                } else {
                    file.failWrite(stream);
                    Slog.w(TAG, "Failed to save state, restoring backup.");
                }
            } catch (IOException e) {
                Slog.w(TAG, "Failed open state file for write: " + e);
            }
        }
    }

    private void tagProvidersAndHosts() {
        int providerCount = this.mProviders.size();
        for (int i = 0; i < providerCount; i++) {
            Provider provider = this.mProviders.get(i);
            provider.tag = i;
        }
        int hostCount = this.mHosts.size();
        for (int i2 = 0; i2 < hostCount; i2++) {
            Host host = this.mHosts.get(i2);
            host.tag = i2;
        }
    }

    private void clearProvidersAndHostsTagsLocked() {
        int providerCount = this.mProviders.size();
        for (int i = 0; i < providerCount; i++) {
            Provider provider = this.mProviders.get(i);
            provider.tag = -1;
        }
        int hostCount = this.mHosts.size();
        for (int i2 = 0; i2 < hostCount; i2++) {
            Host host = this.mHosts.get(i2);
            host.tag = -1;
        }
    }

    private boolean writeProfileStateToFileLocked(FileOutputStream stream, int userId) {
        try {
            TypedXmlSerializer out = Xml.resolveSerializer(stream);
            out.startDocument((String) null, true);
            out.startTag((String) null, "gs");
            out.attributeInt((String) null, "version", 1);
            int N = this.mProviders.size();
            for (int i = 0; i < N; i++) {
                Provider provider = this.mProviders.get(i);
                if (provider.getUserId() == userId) {
                    if (this.mIsProviderInfoPersisted) {
                        serializeProviderWithProviderInfo(out, provider);
                    } else if (provider.shouldBePersisted()) {
                        serializeProvider(out, provider);
                    }
                }
            }
            int N2 = this.mHosts.size();
            for (int i2 = 0; i2 < N2; i2++) {
                Host host = this.mHosts.get(i2);
                if (host.getUserId() == userId) {
                    serializeHost(out, host);
                }
            }
            int N3 = this.mWidgets.size();
            for (int i3 = 0; i3 < N3; i3++) {
                Widget widget = this.mWidgets.get(i3);
                if (widget.host.getUserId() == userId) {
                    serializeAppWidget(out, widget, true);
                }
            }
            Iterator<Pair<Integer, String>> it = this.mPackagesWithBindWidgetPermission.iterator();
            while (it.hasNext()) {
                Pair<Integer, String> binding = it.next();
                if (((Integer) binding.first).intValue() == userId) {
                    out.startTag((String) null, "b");
                    out.attribute((String) null, DomainVerificationLegacySettings.ATTR_PACKAGE_NAME, (String) binding.second);
                    out.endTag((String) null, "b");
                }
            }
            out.endTag((String) null, "gs");
            out.endDocument();
            return true;
        } catch (IOException e) {
            Slog.w(TAG, "Failed to write state: " + e);
            return false;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3278=4] */
    private int readProfileStateFromFileLocked(FileInputStream stream, int userId, List<LoadedWidgetState> outLoadedWidgets) {
        int version;
        int type;
        int version2 = -1;
        try {
            TypedXmlPullParser parser = Xml.resolvePullParser(stream);
            int legacyProviderIndex = -1;
            int legacyHostIndex = -1;
            do {
                try {
                    int type2 = parser.next();
                    if (type2 == 2) {
                        String tag = parser.getName();
                        if ("gs".equals(tag)) {
                            version2 = parser.getAttributeInt((String) null, "version", 0);
                            type = type2;
                        } else if ("p".equals(tag)) {
                            legacyProviderIndex++;
                            try {
                                String pkg = parser.getAttributeValue((String) null, "pkg");
                                String cl = parser.getAttributeValue((String) null, "cl");
                                String pkg2 = getCanonicalPackageName(pkg, cl, userId);
                                if (pkg2 == null) {
                                    version = version2;
                                    type = type2;
                                } else {
                                    int uid = getUidForPackage(pkg2, userId);
                                    if (uid < 0) {
                                        version = version2;
                                        type = type2;
                                    } else {
                                        ComponentName componentName = new ComponentName(pkg2, cl);
                                        ActivityInfo providerInfo = getProviderInfo(componentName, userId);
                                        version = version2;
                                        if (providerInfo == null) {
                                            type = type2;
                                        } else {
                                            try {
                                                ProviderId providerId = new ProviderId(uid, componentName);
                                                Provider provider = lookupProviderLocked(providerId);
                                                if (provider != null) {
                                                    type = type2;
                                                } else if (this.mSafeMode) {
                                                    AppWidgetProviderInfo info = new AppWidgetProviderInfo();
                                                    info.provider = providerId.componentName;
                                                    info.providerInfo = providerInfo;
                                                    type = type2;
                                                    provider = new Provider();
                                                    provider.setPartialInfoLocked(info);
                                                    provider.zombie = true;
                                                    provider.id = providerId;
                                                    this.mProviders.add(provider);
                                                    int providerTag = parser.getAttributeIntHex((String) null, "tag", legacyProviderIndex);
                                                    provider.tag = providerTag;
                                                    provider.infoTag = parser.getAttributeValue((String) null, "info_tag");
                                                } else {
                                                    type = type2;
                                                }
                                                if (this.mIsProviderInfoPersisted) {
                                                    AppWidgetProviderInfo info2 = AppWidgetXmlUtil.readAppWidgetProviderInfoLocked(parser);
                                                    if (info2 == null) {
                                                        Slog.d(TAG, "Unable to load widget provider info from xml for " + providerId.componentName);
                                                    }
                                                    if (info2 != null) {
                                                        info2.provider = providerId.componentName;
                                                        info2.providerInfo = providerInfo;
                                                        provider.setInfoLocked(info2);
                                                    }
                                                }
                                                int providerTag2 = parser.getAttributeIntHex((String) null, "tag", legacyProviderIndex);
                                                provider.tag = providerTag2;
                                                provider.infoTag = parser.getAttributeValue((String) null, "info_tag");
                                            } catch (IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e) {
                                                e = e;
                                                Slog.w(TAG, "failed parsing " + e);
                                                return -1;
                                            }
                                        }
                                    }
                                }
                                version2 = version;
                            } catch (IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e2) {
                                e = e2;
                                Slog.w(TAG, "failed parsing " + e);
                                return -1;
                            }
                        } else {
                            version = version2;
                            type = type2;
                            if ("h".equals(tag)) {
                                legacyHostIndex++;
                                Host host = new Host();
                                String pkg3 = parser.getAttributeValue((String) null, "pkg");
                                int uid2 = getUidForPackage(pkg3, userId);
                                if (uid2 < 0) {
                                    host.zombie = true;
                                }
                                if (!host.zombie || this.mSafeMode) {
                                    int hostId = parser.getAttributeIntHex((String) null, "id");
                                    int hostTag = parser.getAttributeIntHex((String) null, "tag", legacyHostIndex);
                                    host.tag = hostTag;
                                    host.id = new HostId(uid2, hostId, pkg3);
                                    this.mHosts.add(host);
                                }
                                version2 = version;
                            } else if ("b".equals(tag)) {
                                String packageName = parser.getAttributeValue((String) null, DomainVerificationLegacySettings.ATTR_PACKAGE_NAME);
                                if (getUidForPackage(packageName, userId) >= 0) {
                                    Pair<Integer, String> packageId = Pair.create(Integer.valueOf(userId), packageName);
                                    this.mPackagesWithBindWidgetPermission.add(packageId);
                                }
                            } else if ("g".equals(tag)) {
                                Widget widget = new Widget();
                                widget.appWidgetId = parser.getAttributeIntHex((String) null, "id");
                                setMinAppWidgetIdLocked(userId, widget.appWidgetId + 1);
                                widget.restoredId = parser.getAttributeIntHex((String) null, "rid", 0);
                                widget.options = parseWidgetIdOptions(parser);
                                int hostTag2 = parser.getAttributeIntHex((String) null, "h");
                                String providerString = parser.getAttributeValue((String) null, "p");
                                int providerTag3 = providerString != null ? parser.getAttributeIntHex((String) null, "p") : -1;
                                LoadedWidgetState loadedWidgets = new LoadedWidgetState(widget, hostTag2, providerTag3);
                                try {
                                    outLoadedWidgets.add(loadedWidgets);
                                } catch (IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e3) {
                                    e = e3;
                                    Slog.w(TAG, "failed parsing " + e);
                                    return -1;
                                }
                            }
                        }
                    } else {
                        version = version2;
                        type = type2;
                    }
                    version2 = version;
                } catch (IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e4) {
                    e = e4;
                }
            } while (type != 1);
            return version2;
        } catch (IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e5) {
            e = e5;
        }
    }

    private void performUpgradeLocked(int fromVersion) {
        int uid;
        if (fromVersion < 1) {
            Slog.v(TAG, "Upgrading widget database from " + fromVersion + " to 1");
        }
        int version = fromVersion;
        if (version == 0) {
            HostId oldHostId = new HostId(Process.myUid(), KEYGUARD_HOST_ID, "android");
            Host host = lookupHostLocked(oldHostId);
            if (host != null && (uid = getUidForPackage(NEW_KEYGUARD_HOST_PACKAGE, 0)) >= 0) {
                host.id = new HostId(uid, KEYGUARD_HOST_ID, NEW_KEYGUARD_HOST_PACKAGE);
            }
            version = 1;
        }
        if (version != 1) {
            throw new IllegalStateException("Failed to upgrade widget database");
        }
    }

    private static File getStateFile(int userId) {
        return new File(Environment.getUserSystemDirectory(userId), STATE_FILENAME);
    }

    private static AtomicFile getSavedStateFile(int userId) {
        File dir = Environment.getUserSystemDirectory(userId);
        File settingsFile = getStateFile(userId);
        if (!settingsFile.exists() && userId == 0) {
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File oldFile = new File("/data/system/appwidgets.xml");
            oldFile.renameTo(settingsFile);
        }
        return new AtomicFile(settingsFile);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onUserStopped(int userId) {
        synchronized (this.mLock) {
            boolean crossProfileWidgetsChanged = false;
            int widgetCount = this.mWidgets.size();
            int i = widgetCount - 1;
            while (true) {
                boolean providerInUser = false;
                if (i < 0) {
                    break;
                }
                Widget widget = this.mWidgets.get(i);
                boolean hostInUser = widget.host.getUserId() == userId;
                boolean hasProvider = widget.provider != null;
                if (hasProvider && widget.provider.getUserId() == userId) {
                    providerInUser = true;
                }
                if (hostInUser && (!hasProvider || providerInUser)) {
                    removeWidgetLocked(widget);
                    widget.host.widgets.remove(widget);
                    widget.host = null;
                    if (hasProvider) {
                        widget.provider.widgets.remove(widget);
                        widget.provider = null;
                    }
                }
                i--;
            }
            int hostCount = this.mHosts.size();
            for (int i2 = hostCount - 1; i2 >= 0; i2--) {
                Host host = this.mHosts.get(i2);
                if (host.getUserId() == userId) {
                    crossProfileWidgetsChanged |= !host.widgets.isEmpty();
                    deleteHostLocked(host);
                }
            }
            int grantCount = this.mPackagesWithBindWidgetPermission.size();
            for (int i3 = grantCount - 1; i3 >= 0; i3--) {
                Pair<Integer, String> packageId = this.mPackagesWithBindWidgetPermission.valueAt(i3);
                if (((Integer) packageId.first).intValue() == userId) {
                    this.mPackagesWithBindWidgetPermission.removeAt(i3);
                }
            }
            int userIndex = this.mLoadedUserIds.indexOfKey(userId);
            if (userIndex >= 0) {
                this.mLoadedUserIds.removeAt(userIndex);
            }
            int nextIdIndex = this.mNextAppWidgetIds.indexOfKey(userId);
            if (nextIdIndex >= 0) {
                this.mNextAppWidgetIds.removeAt(nextIdIndex);
            }
            if (crossProfileWidgetsChanged) {
                saveGroupStateAsync(userId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void applyResourceOverlaysToWidgetsLocked(Set<String> packageNames, int userId, boolean updateFrameworkRes) {
        ApplicationInfo oldAppInfo;
        int N = this.mProviders.size();
        for (int i = 0; i < N; i++) {
            Provider provider = this.mProviders.get(i);
            if (provider.getUserId() == userId) {
                String packageName = provider.id.componentName.getPackageName();
                if (updateFrameworkRes || packageNames.contains(packageName)) {
                    ApplicationInfo newAppInfo = null;
                    try {
                        newAppInfo = this.mPackageManager.getApplicationInfo(packageName, (long) GadgetFunction.NCM, userId);
                    } catch (RemoteException e) {
                        Slog.w(TAG, "Failed to retrieve app info for " + packageName + " userId=" + userId, e);
                    }
                    if (newAppInfo != null && provider.info != null && provider.info.providerInfo != null && (oldAppInfo = provider.info.providerInfo.applicationInfo) != null && newAppInfo.sourceDir.equals(oldAppInfo.sourceDir)) {
                        ApplicationInfo oldAppInfo2 = new ApplicationInfo(oldAppInfo);
                        oldAppInfo2.overlayPaths = newAppInfo.overlayPaths == null ? null : (String[]) newAppInfo.overlayPaths.clone();
                        oldAppInfo2.resourceDirs = newAppInfo.resourceDirs != null ? (String[]) newAppInfo.resourceDirs.clone() : null;
                        provider.info.providerInfo.applicationInfo = oldAppInfo2;
                        int M = provider.widgets.size();
                        for (int j = 0; j < M; j++) {
                            Widget widget = provider.widgets.get(j);
                            if (widget.views != null) {
                                widget.views.updateAppInfo(oldAppInfo2);
                            }
                            if (widget.maskedViews != null) {
                                widget.maskedViews.updateAppInfo(oldAppInfo2);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean updateProvidersForPackageLocked(String packageName, int userId, Set<ProviderId> removedProviders) {
        boolean providersUpdated;
        Intent intent;
        List<ResolveInfo> broadcastReceivers;
        int N;
        boolean providersUpdated2 = false;
        HashSet<ProviderId> keep = new HashSet<>();
        Intent intent2 = new Intent("android.appwidget.action.APPWIDGET_UPDATE");
        intent2.setPackage(packageName);
        List<ResolveInfo> broadcastReceivers2 = queryIntentReceivers(intent2, userId);
        int N2 = broadcastReceivers2 == null ? 0 : broadcastReceivers2.size();
        int i = 0;
        while (i < N2) {
            ResolveInfo ri = broadcastReceivers2.get(i);
            ActivityInfo ai = ri.activityInfo;
            if ((ai.applicationInfo.flags & 262144) != 0) {
                providersUpdated = providersUpdated2;
                intent = intent2;
                broadcastReceivers = broadcastReceivers2;
                N = N2;
            } else if (!packageName.equals(ai.packageName)) {
                providersUpdated = providersUpdated2;
                intent = intent2;
                broadcastReceivers = broadcastReceivers2;
                N = N2;
            } else {
                providersUpdated = providersUpdated2;
                ProviderId providerId = new ProviderId(ai.applicationInfo.uid, new ComponentName(ai.packageName, ai.name));
                Provider provider = lookupProviderLocked(providerId);
                if (provider == null) {
                    if (!addProviderLocked(ri)) {
                        intent = intent2;
                        broadcastReceivers = broadcastReceivers2;
                        N = N2;
                    } else {
                        keep.add(providerId);
                        providersUpdated2 = true;
                        intent = intent2;
                        broadcastReceivers = broadcastReceivers2;
                        N = N2;
                    }
                } else {
                    AppWidgetProviderInfo info = createPartialProviderInfo(providerId, ri, provider);
                    if (info == null) {
                        intent = intent2;
                        broadcastReceivers = broadcastReceivers2;
                        N = N2;
                    } else {
                        keep.add(providerId);
                        provider.setPartialInfoLocked(info);
                        int M = provider.widgets.size();
                        if (M <= 0) {
                            intent = intent2;
                            broadcastReceivers = broadcastReceivers2;
                            N = N2;
                        } else {
                            int[] appWidgetIds = getWidgetIds(provider.widgets);
                            cancelBroadcastsLocked(provider);
                            registerForBroadcastsLocked(provider, appWidgetIds);
                            intent = intent2;
                            int j = 0;
                            while (j < M) {
                                List<ResolveInfo> broadcastReceivers3 = broadcastReceivers2;
                                Widget widget = provider.widgets.get(j);
                                widget.views = null;
                                scheduleNotifyProviderChangedLocked(widget);
                                j++;
                                broadcastReceivers2 = broadcastReceivers3;
                                N2 = N2;
                            }
                            broadcastReceivers = broadcastReceivers2;
                            N = N2;
                            sendUpdateIntentLocked(provider, appWidgetIds);
                        }
                    }
                    providersUpdated2 = true;
                }
                i++;
                broadcastReceivers2 = broadcastReceivers;
                intent2 = intent;
                N2 = N;
            }
            providersUpdated2 = providersUpdated;
            i++;
            broadcastReceivers2 = broadcastReceivers;
            intent2 = intent;
            N2 = N;
        }
        boolean providersUpdated3 = providersUpdated2;
        int N3 = this.mProviders.size();
        for (int i2 = N3 - 1; i2 >= 0; i2--) {
            Provider provider2 = this.mProviders.get(i2);
            if (packageName.equals(provider2.id.componentName.getPackageName()) && provider2.getUserId() == userId && !keep.contains(provider2.id)) {
                if (removedProviders != null) {
                    removedProviders.add(provider2.id);
                }
                deleteProviderLocked(provider2);
                providersUpdated3 = true;
            }
        }
        return providersUpdated3;
    }

    private void removeWidgetsForPackageLocked(String pkgName, int userId, int parentUserId) {
        int N = this.mProviders.size();
        for (int i = 0; i < N; i++) {
            Provider provider = this.mProviders.get(i);
            if (pkgName.equals(provider.id.componentName.getPackageName()) && provider.getUserId() == userId && provider.widgets.size() > 0) {
                deleteWidgetsLocked(provider, parentUserId);
            }
        }
    }

    private boolean removeProvidersForPackageLocked(String pkgName, int userId) {
        boolean removed = false;
        int N = this.mProviders.size();
        for (int i = N - 1; i >= 0; i--) {
            Provider provider = this.mProviders.get(i);
            if (pkgName.equals(provider.id.componentName.getPackageName()) && provider.getUserId() == userId) {
                deleteProviderLocked(provider);
                removed = true;
            }
        }
        return removed;
    }

    private boolean removeHostsAndProvidersForPackageLocked(String pkgName, int userId) {
        boolean removed = removeProvidersForPackageLocked(pkgName, userId);
        int N = this.mHosts.size();
        for (int i = N - 1; i >= 0; i--) {
            Host host = this.mHosts.get(i);
            if (pkgName.equals(host.id.packageName) && host.getUserId() == userId) {
                deleteHostLocked(host);
                removed = true;
            }
        }
        return removed;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3611=5] */
    private String getCanonicalPackageName(String packageName, String className, int userId) {
        long identity = Binder.clearCallingIdentity();
        try {
            AppGlobals.getPackageManager().getReceiverInfo(new ComponentName(packageName, className), 0L, userId);
            return packageName;
        } catch (RemoteException e) {
            String[] packageNames = this.mContext.getPackageManager().currentToCanonicalPackageNames(new String[]{packageName});
            if (packageNames == null || packageNames.length <= 0) {
                Binder.restoreCallingIdentity(identity);
                return null;
            }
            return packageNames[0];
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendBroadcastAsUser(Intent intent, UserHandle userHandle) {
        long identity = Binder.clearCallingIdentity();
        try {
            this.mContext.sendBroadcastAsUser(intent, userHandle);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void bindService(Intent intent, ServiceConnection connection, UserHandle userHandle) {
        long token = Binder.clearCallingIdentity();
        try {
            this.mContext.bindServiceAsUser(intent, connection, 33554433, userHandle);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void unbindService(ServiceConnection connection) {
        long token = Binder.clearCallingIdentity();
        try {
            this.mContext.unbindService(connection);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void onCrossProfileWidgetProvidersChanged(int userId, List<String> packages) {
        int parentId = this.mSecurityPolicy.getProfileParent(userId);
        if (parentId != userId) {
            synchronized (this.mLock) {
                boolean providersChanged = false;
                ArraySet<String> previousPackages = new ArraySet<>();
                int providerCount = this.mProviders.size();
                for (int i = 0; i < providerCount; i++) {
                    Provider provider = this.mProviders.get(i);
                    if (provider.getUserId() == userId) {
                        previousPackages.add(provider.id.componentName.getPackageName());
                    }
                }
                int packageCount = packages.size();
                for (int i2 = 0; i2 < packageCount; i2++) {
                    String packageName = packages.get(i2);
                    previousPackages.remove(packageName);
                    providersChanged |= updateProvidersForPackageLocked(packageName, userId, null);
                }
                int removedCount = previousPackages.size();
                for (int i3 = 0; i3 < removedCount; i3++) {
                    removeWidgetsForPackageLocked(previousPackages.valueAt(i3), userId, parentId);
                }
                if (providersChanged || removedCount > 0) {
                    saveGroupStateAsync(userId);
                    scheduleNotifyGroupHostsForProvidersChangedLocked(userId);
                }
            }
        }
    }

    public List<ComponentName> getAppWidgetOfHost(String pkg, int uid) {
        List<ComponentName> hostProviders = new ArrayList<>();
        for (int i = 0; i < this.mHosts.size(); i++) {
            Host host = this.mHosts.get(i);
            if (UserHandle.getUserId(host.id.uid) == uid && pkg.equals(host.id.packageName)) {
                for (int j = 0; j < host.widgets.size(); j++) {
                    if (host.widgets.get(j).provider != null) {
                        hostProviders.add(host.widgets.get(j).provider.id.componentName);
                    }
                }
            }
        }
        return hostProviders;
    }

    private boolean isProfileWithLockedParent(int userId) {
        UserInfo parentInfo;
        long token = Binder.clearCallingIdentity();
        try {
            UserInfo userInfo = this.mUserManager.getUserInfo(userId);
            if (userInfo != null && userInfo.isProfile() && (parentInfo = this.mUserManager.getProfileParent(userId)) != null) {
                if (!isUserRunningAndUnlocked(parentInfo.getUserHandle().getIdentifier())) {
                    return true;
                }
            }
            Binder.restoreCallingIdentity(token);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private boolean isProfileWithUnlockedParent(int userId) {
        UserInfo parentInfo;
        UserInfo userInfo = this.mUserManager.getUserInfo(userId);
        if (userInfo != null && userInfo.isProfile() && (parentInfo = this.mUserManager.getProfileParent(userId)) != null && this.mUserManager.isUserUnlockingOrUnlocked(parentInfo.getUserHandle())) {
            return true;
        }
        return false;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3769=5] */
    public void noteAppWidgetTapped(String callingPackage, int appWidgetId) {
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        int callingUid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        try {
            int procState = this.mActivityManagerInternal.getUidProcessState(callingUid);
            if (procState > 2) {
                return;
            }
            synchronized (this.mLock) {
                Widget widget = lookupWidgetLocked(appWidgetId, callingUid, callingPackage);
                if (widget == null) {
                    return;
                }
                ProviderId providerId = widget.provider.id;
                String packageName = providerId.componentName.getPackageName();
                if (packageName == null) {
                    return;
                }
                SparseArray<String> uid2PackageName = new SparseArray<>();
                uid2PackageName.put(providerId.uid, packageName);
                this.mAppOpsManagerInternal.updateAppWidgetVisibility(uid2PackageName, true);
                this.mUsageStatsManagerInternal.reportEvent(packageName, UserHandle.getUserId(providerId.uid), 7);
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* loaded from: classes.dex */
    private final class CallbackHandler extends Handler {
        public static final int MSG_NOTIFY_APP_WIDGET_REMOVED = 5;
        public static final int MSG_NOTIFY_PROVIDERS_CHANGED = 3;
        public static final int MSG_NOTIFY_PROVIDER_CHANGED = 2;
        public static final int MSG_NOTIFY_UPDATE_APP_WIDGET = 1;
        public static final int MSG_NOTIFY_VIEW_DATA_CHANGED = 4;

        public CallbackHandler(Looper looper) {
            super(looper, null, false);
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    SomeArgs args = (SomeArgs) message.obj;
                    Host host = (Host) args.arg1;
                    IAppWidgetHost callbacks = (IAppWidgetHost) args.arg2;
                    RemoteViews views = (RemoteViews) args.arg3;
                    long requestId = ((Long) args.arg4).longValue();
                    int appWidgetId = args.argi1;
                    args.recycle();
                    AppWidgetServiceImpl.this.handleNotifyUpdateAppWidget(host, callbacks, appWidgetId, views, requestId);
                    return;
                case 2:
                    SomeArgs args2 = (SomeArgs) message.obj;
                    Host host2 = (Host) args2.arg1;
                    IAppWidgetHost callbacks2 = (IAppWidgetHost) args2.arg2;
                    AppWidgetProviderInfo info = (AppWidgetProviderInfo) args2.arg3;
                    long requestId2 = ((Long) args2.arg4).longValue();
                    int appWidgetId2 = args2.argi1;
                    args2.recycle();
                    AppWidgetServiceImpl.this.handleNotifyProviderChanged(host2, callbacks2, appWidgetId2, info, requestId2);
                    return;
                case 3:
                    SomeArgs args3 = (SomeArgs) message.obj;
                    Host host3 = (Host) args3.arg1;
                    IAppWidgetHost callbacks3 = (IAppWidgetHost) args3.arg2;
                    args3.recycle();
                    AppWidgetServiceImpl.this.handleNotifyProvidersChanged(host3, callbacks3);
                    return;
                case 4:
                    SomeArgs args4 = (SomeArgs) message.obj;
                    Host host4 = (Host) args4.arg1;
                    IAppWidgetHost callbacks4 = (IAppWidgetHost) args4.arg2;
                    long requestId3 = ((Long) args4.arg3).longValue();
                    int appWidgetId3 = args4.argi1;
                    int viewId = args4.argi2;
                    args4.recycle();
                    AppWidgetServiceImpl.this.handleNotifyAppWidgetViewDataChanged(host4, callbacks4, appWidgetId3, viewId, requestId3);
                    return;
                case 5:
                    SomeArgs args5 = (SomeArgs) message.obj;
                    Host host5 = (Host) args5.arg1;
                    IAppWidgetHost callbacks5 = (IAppWidgetHost) args5.arg2;
                    long requestId4 = ((Long) args5.arg3).longValue();
                    int appWidgetId4 = args5.argi1;
                    args5.recycle();
                    AppWidgetServiceImpl.this.handleNotifyAppWidgetRemoved(host5, callbacks5, appWidgetId4, requestId4);
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SecurityPolicy {
        private SecurityPolicy() {
        }

        public boolean isEnabledGroupProfile(int profileId) {
            int parentId = UserHandle.getCallingUserId();
            return isParentOrProfile(parentId, profileId) && isProfileEnabled(profileId);
        }

        public int[] getEnabledGroupProfileIds(int userId) {
            int parentId = getGroupParent(userId);
            long identity = Binder.clearCallingIdentity();
            try {
                return AppWidgetServiceImpl.this.mUserManager.getEnabledProfileIds(parentId);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void enforceServiceExistsAndRequiresBindRemoteViewsPermission(ComponentName componentName, int userId) {
            ServiceInfo serviceInfo;
            long identity = Binder.clearCallingIdentity();
            try {
                serviceInfo = AppWidgetServiceImpl.this.mPackageManager.getServiceInfo(componentName, 4096L, userId);
            } catch (RemoteException e) {
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
                throw th;
            }
            if (serviceInfo != null) {
                if (!"android.permission.BIND_REMOTEVIEWS".equals(serviceInfo.permission)) {
                    throw new SecurityException("Service " + componentName + " in user " + userId + "does not require android.permission.BIND_REMOTEVIEWS");
                }
                Binder.restoreCallingIdentity(identity);
                return;
            }
            throw new SecurityException("Service " + componentName + " not installed for user " + userId);
        }

        public void enforceModifyAppWidgetBindPermissions(String packageName) {
            AppWidgetServiceImpl.this.mContext.enforceCallingPermission("android.permission.MODIFY_APPWIDGET_BIND_PERMISSIONS", "hasBindAppWidgetPermission packageName=" + packageName);
        }

        public boolean isCallerInstantAppLocked() {
            int callingUid = Binder.getCallingUid();
            long identity = Binder.clearCallingIdentity();
            try {
                String[] uidPackages = AppWidgetServiceImpl.this.mPackageManager.getPackagesForUid(callingUid);
                if (!ArrayUtils.isEmpty(uidPackages)) {
                    boolean isInstantApp = AppWidgetServiceImpl.this.mPackageManager.isInstantApp(uidPackages[0], UserHandle.getUserId(callingUid));
                    Binder.restoreCallingIdentity(identity);
                    return isInstantApp;
                }
            } catch (RemoteException e) {
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
                throw th;
            }
            Binder.restoreCallingIdentity(identity);
            return false;
        }

        public boolean isInstantAppLocked(String packageName, int userId) {
            long identity = Binder.clearCallingIdentity();
            try {
                boolean isInstantApp = AppWidgetServiceImpl.this.mPackageManager.isInstantApp(packageName, userId);
                Binder.restoreCallingIdentity(identity);
                return isInstantApp;
            } catch (RemoteException e) {
                Binder.restoreCallingIdentity(identity);
                return false;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
                throw th;
            }
        }

        public void enforceCallFromPackage(String packageName) {
            AppWidgetServiceImpl.this.mAppOpsManager.checkPackage(Binder.getCallingUid(), packageName);
        }

        public boolean hasCallerBindPermissionOrBindWhiteListedLocked(String packageName) {
            try {
                AppWidgetServiceImpl.this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_APPWIDGET", null);
                return true;
            } catch (SecurityException e) {
                if (!isCallerBindAppWidgetWhiteListedLocked(packageName)) {
                    return false;
                }
                return true;
            }
        }

        private boolean isCallerBindAppWidgetWhiteListedLocked(String packageName) {
            int userId = UserHandle.getCallingUserId();
            int packageUid = AppWidgetServiceImpl.this.getUidForPackage(packageName, userId);
            if (packageUid < 0) {
                throw new IllegalArgumentException("No package " + packageName + " for user " + userId);
            }
            synchronized (AppWidgetServiceImpl.this.mLock) {
                AppWidgetServiceImpl.this.ensureGroupStateLoadedLocked(userId);
                Pair<Integer, String> packageId = Pair.create(Integer.valueOf(userId), packageName);
                if (AppWidgetServiceImpl.this.mPackagesWithBindWidgetPermission.contains(packageId)) {
                    return true;
                }
                return false;
            }
        }

        public boolean canAccessAppWidget(Widget widget, int uid, String packageName) {
            if (isHostInPackageForUid(widget.host, uid, packageName) || isProviderInPackageForUid(widget.provider, uid, packageName) || isHostAccessingProvider(widget.host, widget.provider, uid, packageName)) {
                return true;
            }
            int userId = UserHandle.getUserId(uid);
            return (widget.host.getUserId() == userId || (widget.provider != null && widget.provider.getUserId() == userId)) && AppWidgetServiceImpl.this.mContext.checkCallingPermission("android.permission.BIND_APPWIDGET") == 0;
        }

        private boolean isParentOrProfile(int parentId, int profileId) {
            return parentId == profileId || getProfileParent(profileId) == parentId;
        }

        public boolean isProviderInCallerOrInProfileAndWhitelListed(String packageName, int profileId) {
            int callerId = UserHandle.getCallingUserId();
            if (profileId == callerId) {
                return true;
            }
            int parentId = getProfileParent(profileId);
            if (parentId != callerId) {
                return false;
            }
            return isProviderWhiteListed(packageName, profileId);
        }

        public boolean isProviderWhiteListed(String packageName, int profileId) {
            if (AppWidgetServiceImpl.this.mDevicePolicyManagerInternal == null) {
                return false;
            }
            List<String> crossProfilePackages = AppWidgetServiceImpl.this.mDevicePolicyManagerInternal.getCrossProfileWidgetProviders(profileId);
            return crossProfilePackages.contains(packageName);
        }

        public int getProfileParent(int profileId) {
            long identity = Binder.clearCallingIdentity();
            try {
                UserInfo parent = AppWidgetServiceImpl.this.mUserManager.getProfileParent(profileId);
                if (parent != null) {
                    return parent.getUserHandle().getIdentifier();
                }
                Binder.restoreCallingIdentity(identity);
                return AppWidgetServiceImpl.UNKNOWN_USER_ID;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public int getGroupParent(int profileId) {
            int parentId = AppWidgetServiceImpl.this.mSecurityPolicy.getProfileParent(profileId);
            return parentId != AppWidgetServiceImpl.UNKNOWN_USER_ID ? parentId : profileId;
        }

        public boolean isHostInPackageForUid(Host host, int uid, String packageName) {
            return host.id.uid == uid && host.id.packageName.equals(packageName);
        }

        public boolean isProviderInPackageForUid(Provider provider, int uid, String packageName) {
            return provider != null && provider.id.uid == uid && provider.id.componentName.getPackageName().equals(packageName);
        }

        public boolean isHostAccessingProvider(Host host, Provider provider, int uid, String packageName) {
            return host.id.uid == uid && provider != null && provider.id.componentName.getPackageName().equals(packageName);
        }

        private boolean isProfileEnabled(int profileId) {
            long identity = Binder.clearCallingIdentity();
            try {
                UserInfo userInfo = AppWidgetServiceImpl.this.mUserManager.getUserInfo(profileId);
                if (userInfo != null) {
                    if (userInfo.isEnabled()) {
                        Binder.restoreCallingIdentity(identity);
                        return true;
                    }
                }
                return false;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class Provider {
        PendingIntent broadcast;
        ProviderId id;
        AppWidgetProviderInfo info;
        String infoTag;
        boolean mInfoParsed;
        boolean maskedByLockedProfile;
        boolean maskedByQuietProfile;
        boolean maskedBySuspendedPackage;
        int tag;
        ArrayList<Widget> widgets;
        boolean zombie;

        private Provider() {
            this.widgets = new ArrayList<>();
            this.mInfoParsed = false;
            this.tag = -1;
        }

        public int getUserId() {
            return UserHandle.getUserId(this.id.uid);
        }

        public boolean isInPackageForUser(String packageName, int userId) {
            return getUserId() == userId && this.id.componentName.getPackageName().equals(packageName);
        }

        public boolean hostedByPackageForUser(String packageName, int userId) {
            int N = this.widgets.size();
            for (int i = 0; i < N; i++) {
                Widget widget = this.widgets.get(i);
                if (packageName.equals(widget.host.id.packageName) && widget.host.getUserId() == userId) {
                    return true;
                }
            }
            return false;
        }

        public AppWidgetProviderInfo getInfoLocked(Context context) {
            if (!this.mInfoParsed) {
                if (!this.zombie) {
                    AppWidgetProviderInfo newInfo = null;
                    if (!TextUtils.isEmpty(this.infoTag)) {
                        newInfo = AppWidgetServiceImpl.parseAppWidgetProviderInfo(context, this.id, this.info.providerInfo, this.infoTag);
                    }
                    if (newInfo == null) {
                        newInfo = AppWidgetServiceImpl.parseAppWidgetProviderInfo(context, this.id, this.info.providerInfo, "android.appwidget.provider");
                    }
                    if (newInfo != null) {
                        this.info = newInfo;
                    }
                }
                this.mInfoParsed = true;
            }
            return this.info;
        }

        public AppWidgetProviderInfo getPartialInfoLocked() {
            return this.info;
        }

        public void setPartialInfoLocked(AppWidgetProviderInfo info) {
            this.info = info;
            this.mInfoParsed = false;
        }

        public void setInfoLocked(AppWidgetProviderInfo info) {
            this.info = info;
            this.mInfoParsed = true;
        }

        public String toString() {
            return "Provider{" + this.id + (this.zombie ? " Z" : "") + '}';
        }

        public boolean setMaskedByQuietProfileLocked(boolean masked) {
            boolean oldState = this.maskedByQuietProfile;
            this.maskedByQuietProfile = masked;
            return masked != oldState;
        }

        public boolean setMaskedByLockedProfileLocked(boolean masked) {
            boolean oldState = this.maskedByLockedProfile;
            this.maskedByLockedProfile = masked;
            return masked != oldState;
        }

        public boolean setMaskedBySuspendedPackageLocked(boolean masked) {
            boolean oldState = this.maskedBySuspendedPackage;
            this.maskedBySuspendedPackage = masked;
            return masked != oldState;
        }

        public boolean isMaskedLocked() {
            return this.maskedByQuietProfile || this.maskedByLockedProfile || this.maskedBySuspendedPackage;
        }

        public boolean shouldBePersisted() {
            return (this.widgets.isEmpty() && TextUtils.isEmpty(this.infoTag)) ? false : true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class ProviderId {
        final ComponentName componentName;
        final int uid;

        private ProviderId(int uid, ComponentName componentName) {
            this.uid = uid;
            this.componentName = componentName;
        }

        public UserHandle getProfile() {
            return UserHandle.getUserHandleForUid(this.uid);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            ProviderId other = (ProviderId) obj;
            if (this.uid != other.uid) {
                return false;
            }
            ComponentName componentName = this.componentName;
            if (componentName == null) {
                if (other.componentName != null) {
                    return false;
                }
            } else if (!componentName.equals(other.componentName)) {
                return false;
            }
            return true;
        }

        public int hashCode() {
            int result = this.uid;
            int i = result * 31;
            ComponentName componentName = this.componentName;
            int result2 = i + (componentName != null ? componentName.hashCode() : 0);
            return result2;
        }

        public String toString() {
            return "ProviderId{user:" + UserHandle.getUserId(this.uid) + ", app:" + UserHandle.getAppId(this.uid) + ", cmp:" + this.componentName + '}';
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class Host {
        IAppWidgetHost callbacks;
        HostId id;
        long lastWidgetUpdateSequenceNo;
        int tag;
        ArrayList<Widget> widgets;
        boolean zombie;

        private Host() {
            this.widgets = new ArrayList<>();
            this.tag = -1;
        }

        public int getUserId() {
            return UserHandle.getUserId(this.id.uid);
        }

        public boolean isInPackageForUser(String packageName, int userId) {
            return getUserId() == userId && this.id.packageName.equals(packageName);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean hostsPackageForUser(String pkg, int userId) {
            int N = this.widgets.size();
            for (int i = 0; i < N; i++) {
                Provider provider = this.widgets.get(i).provider;
                if (provider != null && provider.getUserId() == userId && pkg.equals(provider.id.componentName.getPackageName())) {
                    return true;
                }
            }
            return false;
        }

        public void getPendingUpdatesForIdLocked(Context context, int appWidgetId, LongSparseArray<PendingHostUpdate> outUpdates) {
            PendingHostUpdate update;
            long updateSequenceNo = this.lastWidgetUpdateSequenceNo;
            int N = this.widgets.size();
            for (int i = 0; i < N; i++) {
                Widget widget = this.widgets.get(i);
                if (widget.appWidgetId == appWidgetId) {
                    for (int j = widget.updateSequenceNos.size() - 1; j >= 0; j--) {
                        long requestId = widget.updateSequenceNos.valueAt(j);
                        if (requestId > updateSequenceNo) {
                            int id = widget.updateSequenceNos.keyAt(j);
                            switch (id) {
                                case 0:
                                    update = PendingHostUpdate.updateAppWidget(appWidgetId, AppWidgetServiceImpl.cloneIfLocalBinder(widget.getEffectiveViewsLocked()));
                                    break;
                                case 1:
                                    update = PendingHostUpdate.providerChanged(appWidgetId, widget.provider.getInfoLocked(context));
                                    break;
                                default:
                                    update = PendingHostUpdate.viewDataChanged(appWidgetId, id);
                                    break;
                            }
                            outUpdates.put(requestId, update);
                        }
                    }
                    return;
                }
            }
            outUpdates.put(this.lastWidgetUpdateSequenceNo, PendingHostUpdate.appWidgetRemoved(appWidgetId));
        }

        public SparseArray<String> getWidgetUids() {
            SparseArray<String> uids = new SparseArray<>();
            for (int i = this.widgets.size() - 1; i >= 0; i--) {
                Widget widget = this.widgets.get(i);
                ProviderId providerId = widget.provider.id;
                uids.put(providerId.uid, providerId.componentName.getPackageName());
            }
            return uids;
        }

        public String toString() {
            return "Host{" + this.id + (this.zombie ? " Z" : "") + '}';
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class HostId {
        final int hostId;
        final String packageName;
        final int uid;

        public HostId(int uid, int hostId, String packageName) {
            this.uid = uid;
            this.hostId = hostId;
            this.packageName = packageName;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            HostId other = (HostId) obj;
            if (this.uid != other.uid || this.hostId != other.hostId) {
                return false;
            }
            String str = this.packageName;
            if (str == null) {
                if (other.packageName != null) {
                    return false;
                }
            } else if (!str.equals(other.packageName)) {
                return false;
            }
            return true;
        }

        public int hashCode() {
            int result = this.uid;
            int result2 = ((result * 31) + this.hostId) * 31;
            String str = this.packageName;
            return result2 + (str != null ? str.hashCode() : 0);
        }

        public String toString() {
            return "HostId{user:" + UserHandle.getUserId(this.uid) + ", app:" + UserHandle.getAppId(this.uid) + ", hostId:" + this.hostId + ", pkg:" + this.packageName + '}';
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class Widget {
        int appWidgetId;
        Host host;
        RemoteViews maskedViews;
        Bundle options;
        Provider provider;
        int restoredId;
        boolean trackingUpdate;
        SparseLongArray updateSequenceNos;
        RemoteViews views;

        private Widget() {
            this.updateSequenceNos = new SparseLongArray(2);
            this.trackingUpdate = false;
        }

        public String toString() {
            return "AppWidgetId{" + this.appWidgetId + ':' + this.host + ':' + this.provider + '}';
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean replaceWithMaskedViewsLocked(RemoteViews views) {
            this.maskedViews = views;
            return true;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean clearMaskedViewsLocked() {
            if (this.maskedViews != null) {
                this.maskedViews = null;
                return true;
            }
            return false;
        }

        public RemoteViews getEffectiveViewsLocked() {
            RemoteViews remoteViews = this.maskedViews;
            return remoteViews != null ? remoteViews : this.views;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class LoadedWidgetState {
        final int hostTag;
        final int providerTag;
        final Widget widget;

        public LoadedWidgetState(Widget widget, int hostTag, int providerTag) {
            this.widget = widget;
            this.hostTag = hostTag;
            this.providerTag = providerTag;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SaveStateRunnable implements Runnable {
        final int mUserId;

        public SaveStateRunnable(int userId) {
            this.mUserId = userId;
        }

        @Override // java.lang.Runnable
        public void run() {
            synchronized (AppWidgetServiceImpl.this.mLock) {
                AppWidgetServiceImpl.this.ensureGroupStateLoadedLocked(this.mUserId, false);
                AppWidgetServiceImpl.this.saveStateLocked(this.mUserId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class BackupRestoreController {
        private static final boolean DEBUG = true;
        private static final String TAG = "BackupRestoreController";
        private static final int WIDGET_STATE_VERSION = 2;
        private boolean mHasSystemRestoreFinished;
        private final SparseArray<Set<String>> mPrunedAppsPerUser;
        private final HashMap<Host, ArrayList<RestoreUpdateRecord>> mUpdatesByHost;
        private final HashMap<Provider, ArrayList<RestoreUpdateRecord>> mUpdatesByProvider;

        private BackupRestoreController() {
            this.mPrunedAppsPerUser = new SparseArray<>();
            this.mUpdatesByProvider = new HashMap<>();
            this.mUpdatesByHost = new HashMap<>();
        }

        public List<String> getWidgetParticipants(int userId) {
            Slog.i(TAG, "Getting widget participants for user: " + userId);
            HashSet<String> packages = new HashSet<>();
            synchronized (AppWidgetServiceImpl.this.mLock) {
                int N = AppWidgetServiceImpl.this.mWidgets.size();
                for (int i = 0; i < N; i++) {
                    Widget widget = (Widget) AppWidgetServiceImpl.this.mWidgets.get(i);
                    if (isProviderAndHostInUser(widget, userId)) {
                        packages.add(widget.host.id.packageName);
                        Provider provider = widget.provider;
                        if (provider != null) {
                            packages.add(provider.id.componentName.getPackageName());
                        }
                    }
                }
            }
            return new ArrayList(packages);
        }

        public byte[] getWidgetState(String backedupPackage, int userId) {
            Slog.i(TAG, "Getting widget state for user: " + userId);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            synchronized (AppWidgetServiceImpl.this.mLock) {
                if (packageNeedsWidgetBackupLocked(backedupPackage, userId)) {
                    try {
                        TypedXmlSerializer out = Xml.newFastSerializer();
                        out.setOutput(stream, StandardCharsets.UTF_8.name());
                        out.startDocument((String) null, true);
                        out.startTag((String) null, "ws");
                        out.attributeInt((String) null, "version", 2);
                        out.attribute((String) null, "pkg", backedupPackage);
                        int index = 0;
                        int N = AppWidgetServiceImpl.this.mProviders.size();
                        for (int i = 0; i < N; i++) {
                            Provider provider = (Provider) AppWidgetServiceImpl.this.mProviders.get(i);
                            if (provider.shouldBePersisted() && (provider.isInPackageForUser(backedupPackage, userId) || provider.hostedByPackageForUser(backedupPackage, userId))) {
                                provider.tag = index;
                                AppWidgetServiceImpl.serializeProvider(out, provider);
                                index++;
                            }
                        }
                        int N2 = AppWidgetServiceImpl.this.mHosts.size();
                        int index2 = 0;
                        for (int i2 = 0; i2 < N2; i2++) {
                            Host host = (Host) AppWidgetServiceImpl.this.mHosts.get(i2);
                            if (!host.widgets.isEmpty() && (host.isInPackageForUser(backedupPackage, userId) || host.hostsPackageForUser(backedupPackage, userId))) {
                                host.tag = index2;
                                AppWidgetServiceImpl.serializeHost(out, host);
                                index2++;
                            }
                        }
                        int N3 = AppWidgetServiceImpl.this.mWidgets.size();
                        for (int i3 = 0; i3 < N3; i3++) {
                            Widget widget = (Widget) AppWidgetServiceImpl.this.mWidgets.get(i3);
                            Provider provider2 = widget.provider;
                            if (widget.host.isInPackageForUser(backedupPackage, userId) || (provider2 != null && provider2.isInPackageForUser(backedupPackage, userId))) {
                                AppWidgetServiceImpl.serializeAppWidget(out, widget, false);
                            }
                        }
                        out.endTag((String) null, "ws");
                        out.endDocument();
                        return stream.toByteArray();
                    } catch (IOException e) {
                        Slog.w(TAG, "Unable to save widget state for " + backedupPackage);
                        return null;
                    }
                }
                return null;
            }
        }

        public void systemRestoreStarting(int userId) {
            Slog.i(TAG, "System restore starting for user: " + userId);
            synchronized (AppWidgetServiceImpl.this.mLock) {
                this.mHasSystemRestoreFinished = false;
                getPrunedAppsLocked(userId).clear();
                this.mUpdatesByProvider.clear();
                this.mUpdatesByHost.clear();
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4738=7, 4742=5] */
        public void restoreWidgetState(String packageName, byte[] restoredState, int userId) {
            ByteArrayInputStream stream;
            ArrayList<Provider> restoredProviders;
            Provider p;
            Slog.i(TAG, "Restoring widget state for user:" + userId + " package: " + packageName);
            ByteArrayInputStream stream2 = new ByteArrayInputStream(restoredState);
            try {
                try {
                    ArrayList<Provider> restoredProviders2 = new ArrayList<>();
                    ArrayList<Host> restoredHosts = new ArrayList<>();
                    TypedXmlPullParser parser = Xml.newFastPullParser();
                    parser.setInput(stream2, StandardCharsets.UTF_8.name());
                    synchronized (AppWidgetServiceImpl.this.mLock) {
                        while (true) {
                            try {
                                int type = parser.next();
                                if (type == 2) {
                                    String tag = parser.getName();
                                    if ("ws".equals(tag)) {
                                        try {
                                            int versionNumber = parser.getAttributeInt((String) null, "version");
                                            if (versionNumber > 2) {
                                                Slog.w(TAG, "Unable to process state version " + versionNumber);
                                                AppWidgetServiceImpl.this.saveGroupStateAsync(userId);
                                                return;
                                            }
                                            String pkg = parser.getAttributeValue((String) null, "pkg");
                                            if (!packageName.equals(pkg)) {
                                                Slog.w(TAG, "Package mismatch in ws");
                                                AppWidgetServiceImpl.this.saveGroupStateAsync(userId);
                                                return;
                                            }
                                            stream = stream2;
                                            restoredProviders = restoredProviders2;
                                        } catch (Throwable th) {
                                            th = th;
                                            try {
                                                throw th;
                                            } catch (IOException | XmlPullParserException e) {
                                                Slog.w(TAG, "Unable to restore widget state for " + packageName);
                                                AppWidgetServiceImpl.this.saveGroupStateAsync(userId);
                                                return;
                                            }
                                        }
                                    } else if ("p".equals(tag)) {
                                        try {
                                            String pkg2 = parser.getAttributeValue((String) null, "pkg");
                                            String cl = parser.getAttributeValue((String) null, "cl");
                                            ComponentName componentName = new ComponentName(pkg2, cl);
                                            Provider p2 = findProviderLocked(componentName, userId);
                                            if (p2 == null) {
                                                AppWidgetProviderInfo info = new AppWidgetProviderInfo();
                                                info.provider = componentName;
                                                p = new Provider();
                                                stream = stream2;
                                                try {
                                                    p.id = new ProviderId(-1, componentName);
                                                    p.setPartialInfoLocked(info);
                                                    p.zombie = true;
                                                    AppWidgetServiceImpl.this.mProviders.add(p);
                                                } catch (Throwable th2) {
                                                    th = th2;
                                                    throw th;
                                                }
                                            } else {
                                                stream = stream2;
                                                p = p2;
                                            }
                                            Slog.i(TAG, "   provider " + p.id);
                                            restoredProviders2.add(p);
                                            restoredProviders = restoredProviders2;
                                        } catch (Throwable th3) {
                                            th = th3;
                                        }
                                    } else {
                                        stream = stream2;
                                        try {
                                            if ("h".equals(tag)) {
                                                String pkg3 = parser.getAttributeValue((String) null, "pkg");
                                                int uid = AppWidgetServiceImpl.this.getUidForPackage(pkg3, userId);
                                                int hostId = parser.getAttributeIntHex((String) null, "id");
                                                Host h = AppWidgetServiceImpl.this.lookupOrAddHostLocked(new HostId(uid, hostId, pkg3));
                                                restoredHosts.add(h);
                                                Slog.i(TAG, "   host[" + restoredHosts.size() + "]: {" + h.id + "}");
                                                restoredProviders = restoredProviders2;
                                            } else if ("g".equals(tag)) {
                                                int restoredId = parser.getAttributeIntHex((String) null, "id");
                                                int hostIndex = parser.getAttributeIntHex((String) null, "h");
                                                Host host = restoredHosts.get(hostIndex);
                                                int which = parser.getAttributeIntHex((String) null, "p", -1);
                                                Provider p3 = which != -1 ? restoredProviders2.get(which) : null;
                                                pruneWidgetStateLocked(host.id.packageName, userId);
                                                if (p3 != null) {
                                                    pruneWidgetStateLocked(p3.id.componentName.getPackageName(), userId);
                                                }
                                                Widget id = findRestoredWidgetLocked(restoredId, host, p3);
                                                if (id == null) {
                                                    id = new Widget();
                                                    id.appWidgetId = AppWidgetServiceImpl.this.incrementAndGetAppWidgetIdLocked(userId);
                                                    id.restoredId = restoredId;
                                                    id.options = AppWidgetServiceImpl.parseWidgetIdOptions(parser);
                                                    id.host = host;
                                                    id.host.widgets.add(id);
                                                    id.provider = p3;
                                                    if (id.provider != null) {
                                                        id.provider.widgets.add(id);
                                                    }
                                                    restoredProviders = restoredProviders2;
                                                    try {
                                                        Slog.i(TAG, "New restored id " + restoredId + " now " + id);
                                                        AppWidgetServiceImpl.this.addWidgetLocked(id);
                                                    } catch (Throwable th4) {
                                                        th = th4;
                                                        throw th;
                                                    }
                                                } else {
                                                    restoredProviders = restoredProviders2;
                                                }
                                                if (id.provider == null || id.provider.getPartialInfoLocked() == null) {
                                                    Slog.w(TAG, "Missing provider for restored widget " + id);
                                                } else {
                                                    stashProviderRestoreUpdateLocked(id.provider, restoredId, id.appWidgetId);
                                                }
                                                stashHostRestoreUpdateLocked(id.host, restoredId, id.appWidgetId);
                                                Slog.i(TAG, "   instance: " + restoredId + " -> " + id.appWidgetId + " :: p=" + id.provider);
                                            } else {
                                                restoredProviders = restoredProviders2;
                                            }
                                        } catch (Throwable th5) {
                                            th = th5;
                                        }
                                    }
                                } else {
                                    stream = stream2;
                                    restoredProviders = restoredProviders2;
                                }
                                if (type == 1) {
                                    break;
                                }
                                restoredProviders2 = restoredProviders;
                                stream2 = stream;
                            } catch (Throwable th6) {
                                th = th6;
                            }
                        }
                    }
                } catch (Throwable th7) {
                    th = th7;
                    AppWidgetServiceImpl.this.saveGroupStateAsync(userId);
                    throw th;
                }
            } catch (IOException | XmlPullParserException e2) {
            } catch (Throwable th8) {
                th = th8;
                AppWidgetServiceImpl.this.saveGroupStateAsync(userId);
                throw th;
            }
        }

        public void systemRestoreFinished(int userId) {
            Slog.i(TAG, "systemRestoreFinished for " + userId);
            synchronized (AppWidgetServiceImpl.this.mLock) {
                this.mHasSystemRestoreFinished = true;
                maybeSendWidgetRestoreBroadcastsLocked(userId);
            }
        }

        public void widgetComponentsChanged(int userId) {
            synchronized (AppWidgetServiceImpl.this.mLock) {
                if (this.mHasSystemRestoreFinished) {
                    maybeSendWidgetRestoreBroadcastsLocked(userId);
                }
            }
        }

        private void maybeSendWidgetRestoreBroadcastsLocked(int userId) {
            String str;
            String str2;
            Set<Map.Entry<Host, ArrayList<RestoreUpdateRecord>>> hostEntries;
            String str3;
            String str4;
            Iterator<Map.Entry<Host, ArrayList<RestoreUpdateRecord>>> it;
            String str5;
            String str6;
            int[] newIds;
            String str7 = TAG;
            Slog.i(TAG, "maybeSendWidgetRestoreBroadcasts for " + userId);
            UserHandle userHandle = new UserHandle(userId);
            Set<Map.Entry<Provider, ArrayList<RestoreUpdateRecord>>> providerEntries = this.mUpdatesByProvider.entrySet();
            Iterator<Map.Entry<Provider, ArrayList<RestoreUpdateRecord>>> it2 = providerEntries.iterator();
            while (true) {
                str = "   ";
                str2 = " pending: ";
                if (!it2.hasNext()) {
                    break;
                }
                Map.Entry<Provider, ArrayList<RestoreUpdateRecord>> e = it2.next();
                Provider provider = e.getKey();
                if (!provider.zombie) {
                    ArrayList<RestoreUpdateRecord> updates = e.getValue();
                    int pending = countPendingUpdates(updates);
                    Slog.i(TAG, "Provider " + provider + " pending: " + pending);
                    if (pending > 0) {
                        int[] oldIds = new int[pending];
                        int[] newIds2 = new int[pending];
                        int N = updates.size();
                        int nextPending = 0;
                        int nextPending2 = 0;
                        while (nextPending2 < N) {
                            RestoreUpdateRecord r = updates.get(nextPending2);
                            int N2 = N;
                            if (r.notified) {
                                newIds = newIds2;
                            } else {
                                r.notified = true;
                                oldIds[nextPending] = r.oldId;
                                newIds2[nextPending] = r.newId;
                                nextPending++;
                                newIds = newIds2;
                                Slog.i(TAG, "   " + r.oldId + " => " + r.newId);
                            }
                            nextPending2++;
                            N = N2;
                            newIds2 = newIds;
                        }
                        sendWidgetRestoreBroadcastLocked("android.appwidget.action.APPWIDGET_RESTORED", provider, null, oldIds, newIds2, userHandle);
                    }
                }
            }
            Set<Map.Entry<Host, ArrayList<RestoreUpdateRecord>>> hostEntries2 = this.mUpdatesByHost.entrySet();
            Iterator<Map.Entry<Host, ArrayList<RestoreUpdateRecord>>> it3 = hostEntries2.iterator();
            while (it3.hasNext()) {
                Map.Entry<Host, ArrayList<RestoreUpdateRecord>> e2 = it3.next();
                Host host = e2.getKey();
                if (host.id.uid == -1) {
                    hostEntries = hostEntries2;
                    str3 = str;
                    str4 = str2;
                    it = it3;
                    str5 = str7;
                } else {
                    ArrayList<RestoreUpdateRecord> updates2 = e2.getValue();
                    int pending2 = countPendingUpdates(updates2);
                    Slog.i(str7, "Host " + host + str2 + pending2);
                    if (pending2 <= 0) {
                        hostEntries = hostEntries2;
                        str3 = str;
                        str4 = str2;
                        it = it3;
                        str5 = str7;
                    } else {
                        int[] oldIds2 = new int[pending2];
                        int[] newIds3 = new int[pending2];
                        int N3 = updates2.size();
                        int nextPending3 = 0;
                        hostEntries = hostEntries2;
                        int i = 0;
                        while (i < N3) {
                            String str8 = str2;
                            RestoreUpdateRecord r2 = updates2.get(i);
                            Iterator<Map.Entry<Host, ArrayList<RestoreUpdateRecord>>> it4 = it3;
                            if (r2.notified) {
                                str6 = str;
                            } else {
                                r2.notified = true;
                                oldIds2[nextPending3] = r2.oldId;
                                newIds3[nextPending3] = r2.newId;
                                nextPending3++;
                                str6 = str;
                                Slog.i(str7, str + r2.oldId + " => " + r2.newId);
                            }
                            i++;
                            it3 = it4;
                            str2 = str8;
                            str = str6;
                        }
                        str3 = str;
                        str4 = str2;
                        it = it3;
                        str5 = str7;
                        sendWidgetRestoreBroadcastLocked("android.appwidget.action.APPWIDGET_HOST_RESTORED", null, host, oldIds2, newIds3, userHandle);
                    }
                }
                hostEntries2 = hostEntries;
                it3 = it;
                str2 = str4;
                str = str3;
                str7 = str5;
            }
        }

        private Provider findProviderLocked(ComponentName componentName, int userId) {
            int providerCount = AppWidgetServiceImpl.this.mProviders.size();
            for (int i = 0; i < providerCount; i++) {
                Provider provider = (Provider) AppWidgetServiceImpl.this.mProviders.get(i);
                if (provider.getUserId() == userId && provider.id.componentName.equals(componentName)) {
                    return provider;
                }
            }
            return null;
        }

        private Widget findRestoredWidgetLocked(int restoredId, Host host, Provider p) {
            Slog.i(TAG, "Find restored widget: id=" + restoredId + " host=" + host + " provider=" + p);
            if (p == null || host == null) {
                return null;
            }
            int N = AppWidgetServiceImpl.this.mWidgets.size();
            for (int i = 0; i < N; i++) {
                Widget widget = (Widget) AppWidgetServiceImpl.this.mWidgets.get(i);
                if (widget.restoredId == restoredId && widget.host.id.equals(host.id) && widget.provider.id.equals(p.id)) {
                    Slog.i(TAG, "   Found at " + i + " : " + widget);
                    return widget;
                }
            }
            return null;
        }

        private boolean packageNeedsWidgetBackupLocked(String packageName, int userId) {
            int N = AppWidgetServiceImpl.this.mWidgets.size();
            for (int i = 0; i < N; i++) {
                Widget widget = (Widget) AppWidgetServiceImpl.this.mWidgets.get(i);
                if (isProviderAndHostInUser(widget, userId)) {
                    if (widget.host.isInPackageForUser(packageName, userId)) {
                        return true;
                    }
                    Provider provider = widget.provider;
                    if (provider != null && provider.isInPackageForUser(packageName, userId)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private void stashProviderRestoreUpdateLocked(Provider provider, int oldId, int newId) {
            ArrayList<RestoreUpdateRecord> r = this.mUpdatesByProvider.get(provider);
            if (r == null) {
                r = new ArrayList<>();
                this.mUpdatesByProvider.put(provider, r);
            } else if (alreadyStashed(r, oldId, newId)) {
                Slog.i(TAG, "ID remap " + oldId + " -> " + newId + " already stashed for " + provider);
                return;
            }
            r.add(new RestoreUpdateRecord(oldId, newId));
        }

        private boolean alreadyStashed(ArrayList<RestoreUpdateRecord> stash, int oldId, int newId) {
            int N = stash.size();
            for (int i = 0; i < N; i++) {
                RestoreUpdateRecord r = stash.get(i);
                if (r.oldId == oldId && r.newId == newId) {
                    return true;
                }
            }
            return false;
        }

        private void stashHostRestoreUpdateLocked(Host host, int oldId, int newId) {
            ArrayList<RestoreUpdateRecord> r = this.mUpdatesByHost.get(host);
            if (r == null) {
                r = new ArrayList<>();
                this.mUpdatesByHost.put(host, r);
            } else if (alreadyStashed(r, oldId, newId)) {
                Slog.i(TAG, "ID remap " + oldId + " -> " + newId + " already stashed for " + host);
                return;
            }
            r.add(new RestoreUpdateRecord(oldId, newId));
        }

        private void sendWidgetRestoreBroadcastLocked(String action, Provider provider, Host host, int[] oldIds, int[] newIds, UserHandle userHandle) {
            Intent intent = new Intent(action);
            intent.putExtra("appWidgetOldIds", oldIds);
            intent.putExtra("appWidgetIds", newIds);
            if (provider != null) {
                intent.setComponent(provider.id.componentName);
                AppWidgetServiceImpl.this.sendBroadcastAsUser(intent, userHandle);
            }
            if (host != null) {
                intent.setComponent(null);
                intent.setPackage(host.id.packageName);
                intent.putExtra("hostId", host.id.hostId);
                AppWidgetServiceImpl.this.sendBroadcastAsUser(intent, userHandle);
            }
        }

        private void pruneWidgetStateLocked(String pkg, int userId) {
            Set<String> prunedApps = getPrunedAppsLocked(userId);
            if (!prunedApps.contains(pkg)) {
                Slog.i(TAG, "pruning widget state for restoring package " + pkg);
                for (int i = AppWidgetServiceImpl.this.mWidgets.size() - 1; i >= 0; i--) {
                    Widget widget = (Widget) AppWidgetServiceImpl.this.mWidgets.get(i);
                    Host host = widget.host;
                    Provider provider = widget.provider;
                    if (host.hostsPackageForUser(pkg, userId) || (provider != null && provider.isInPackageForUser(pkg, userId))) {
                        host.widgets.remove(widget);
                        provider.widgets.remove(widget);
                        AppWidgetServiceImpl.this.decrementAppWidgetServiceRefCount(widget);
                        AppWidgetServiceImpl.this.removeWidgetLocked(widget);
                    }
                }
                prunedApps.add(pkg);
                return;
            }
            Slog.i(TAG, "already pruned " + pkg + ", continuing normally");
        }

        private Set<String> getPrunedAppsLocked(int userId) {
            if (!this.mPrunedAppsPerUser.contains(userId)) {
                this.mPrunedAppsPerUser.set(userId, new ArraySet());
            }
            return this.mPrunedAppsPerUser.get(userId);
        }

        private boolean isProviderAndHostInUser(Widget widget, int userId) {
            return widget.host.getUserId() == userId && (widget.provider == null || widget.provider.getUserId() == userId);
        }

        private int countPendingUpdates(ArrayList<RestoreUpdateRecord> updates) {
            int pending = 0;
            int N = updates.size();
            for (int i = 0; i < N; i++) {
                RestoreUpdateRecord r = updates.get(i);
                if (!r.notified) {
                    pending++;
                }
            }
            return pending;
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public class RestoreUpdateRecord {
            public int newId;
            public boolean notified = false;
            public int oldId;

            public RestoreUpdateRecord(int theOldId, int theNewId) {
                this.oldId = theOldId;
                this.newId = theNewId;
            }
        }
    }

    /* loaded from: classes.dex */
    private class AppWidgetManagerLocal extends AppWidgetManagerInternal {
        private AppWidgetManagerLocal() {
        }

        public ArraySet<String> getHostedWidgetPackages(int uid) {
            ArraySet<String> widgetPackages;
            synchronized (AppWidgetServiceImpl.this.mLock) {
                widgetPackages = null;
                int widgetCount = AppWidgetServiceImpl.this.mWidgets.size();
                for (int i = 0; i < widgetCount; i++) {
                    Widget widget = (Widget) AppWidgetServiceImpl.this.mWidgets.get(i);
                    if (widget.host.id.uid == uid && widget.provider != null) {
                        if (widgetPackages == null) {
                            widgetPackages = new ArraySet<>();
                        }
                        widgetPackages.add(widget.provider.id.componentName.getPackageName());
                    }
                }
            }
            return widgetPackages;
        }

        public void unlockUser(int userId) {
            AppWidgetServiceImpl.this.handleUserUnlocked(userId);
        }

        public void applyResourceOverlaysToWidgets(Set<String> packageNames, int userId, boolean updateFrameworkRes) {
            synchronized (AppWidgetServiceImpl.this.mLock) {
                AppWidgetServiceImpl.this.applyResourceOverlaysToWidgetsLocked(new HashSet(packageNames), userId, updateFrameworkRes);
            }
        }
    }

    public List<String> getBoundAppWidgets(String launcher) {
        List<String> widgets = new ArrayList<>();
        if (launcher == null) {
            return widgets;
        }
        synchronized (this.mLock) {
            int N = this.mHosts.size();
            for (int i = 0; i < N; i++) {
                Host host = this.mHosts.get(i);
                if (launcher.equals(host.id.packageName) && host.callbacks != null) {
                    int M = host.widgets.size();
                    for (int j = 0; j < M; j++) {
                        Widget widget = host.widgets.get(j);
                        if (widget.provider != null) {
                            String packageName = widget.provider.id.componentName.getPackageName();
                            if (!widgets.contains(packageName)) {
                                widgets.add(packageName);
                            }
                        }
                    }
                }
            }
        }
        return widgets;
    }

    private void hookAppWidget(Widget widget, int type, String message) {
        if (widget == null || !ITranGriffinFeature.Instance().isGriffinSupport()) {
            return;
        }
        if (ITranGriffinFeature.Instance().isGriffinDebugOpen() && message != null) {
            Slog.d("TranGriffin/AppWidget", message);
        }
        long startTime = SystemClock.elapsedRealtime();
        Slog.d("TranGriffin/AppWidget", "Start to hook widget[" + widget.appWidgetId + "], operation type: " + type);
        TranWidgetInfo widgetInfo = new TranWidgetInfo(widget.appWidgetId);
        widgetInfo.lastUpdateTime = SystemClock.elapsedRealtime();
        if (widget.provider != null && widget.provider.id != null) {
            Objects.requireNonNull(widgetInfo);
            TranWidgetInfo.WidgetProvider widgetProvider = new TranWidgetInfo.WidgetProvider(widget.provider.id.uid, widget.provider.id.componentName);
            widgetInfo.provider = widgetProvider;
            Slog.d("TranGriffin/AppWidget", "widget provider: " + widget.provider.id);
        }
        if (widget.host != null && widget.host.id != null) {
            Objects.requireNonNull(widgetInfo);
            TranWidgetInfo.WidgetHost widgetHost = new TranWidgetInfo.WidgetHost(widget.host.id.uid, widget.host.id.hostId, widget.host.id.packageName);
            widgetInfo.host = widgetHost;
            Slog.d("TranGriffin/AppWidget", "widget host: " + widget.host.id);
        }
        ITranAppWidgetServiceImpl.Instance().hookAppWidgetChanged(widgetInfo, type);
        Slog.d("TranGriffin/AppWidget", "End to hook appWidget coast " + (SystemClock.elapsedRealtime() - startTime) + "(ms)");
    }
}
