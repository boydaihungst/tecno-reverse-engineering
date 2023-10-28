package com.android.server.pm;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.app.IApplicationThread;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.app.usage.UsageStatsManagerInternal;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.LocusId;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ILauncherApps;
import android.content.pm.IOnAppsChangedListener;
import android.content.pm.IPackageInstallerCallback;
import android.content.pm.IPackageManager;
import android.content.pm.IShortcutChangeCallback;
import android.content.pm.IncrementalStatesInfo;
import android.content.pm.LauncherActivityInfoInternal;
import android.content.pm.LauncherApps;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutQueryWrapper;
import android.content.pm.ShortcutServiceInternal;
import android.content.pm.UserInfo;
import android.graphics.Rect;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IInterface;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import com.android.internal.content.PackageMonitor;
import com.android.internal.infra.AndroidFuture;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.pm.LauncherAppsService;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.mediatek.server.MtkSystemServiceFactory;
import com.mediatek.server.pm.PmsExt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
/* loaded from: classes2.dex */
public class LauncherAppsService extends SystemService {
    private final LauncherAppsImpl mLauncherAppsImpl;

    /* loaded from: classes2.dex */
    public static abstract class LauncherAppsServiceInternal {
        public abstract boolean startShortcut(int i, int i2, String str, String str2, String str3, String str4, Rect rect, Bundle bundle, int i3);
    }

    public LauncherAppsService(Context context) {
        super(context);
        this.mLauncherAppsImpl = new LauncherAppsImpl(context);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("launcherapps", this.mLauncherAppsImpl);
        this.mLauncherAppsImpl.registerLoadingProgressForIncrementalApps();
        LocalServices.addService(LauncherAppsServiceInternal.class, this.mLauncherAppsImpl.mInternal);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class BroadcastCookie {
        public final int callingPid;
        public final int callingUid;
        public final String packageName;
        public final UserHandle user;

        BroadcastCookie(UserHandle userHandle, String packageName, int callingPid, int callingUid) {
            this.user = userHandle;
            this.packageName = packageName;
            this.callingUid = callingUid;
            this.callingPid = callingPid;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class LauncherAppsImpl extends ILauncherApps.Stub {
        private static final boolean DEBUG = false;
        private static final String TAG = "LauncherAppsService";
        private final ActivityManagerInternal mActivityManagerInternal;
        private final ActivityTaskManagerInternal mActivityTaskManagerInternal;
        private final Handler mCallbackHandler;
        private final Context mContext;
        private final DevicePolicyManager mDpm;
        private final IPackageManager mIPM;
        final LauncherAppsServiceInternal mInternal;
        private boolean mIsWatchingPackageBroadcasts;
        private PackageInstallerService mPackageInstallerService;
        private final PackageManagerInternal mPackageManagerInternal;
        private final MyPackageMonitor mPackageMonitor;
        private PmsExt mPmsExt;
        private final ShortcutChangeHandler mShortcutChangeHandler;
        private final ShortcutServiceInternal mShortcutServiceInternal;
        private final UserManager mUm;
        private final UsageStatsManagerInternal mUsageStatsManagerInternal;
        private final UserManagerInternal mUserManagerInternal;
        private final PackageCallbackList<IOnAppsChangedListener> mListeners = new PackageCallbackList<>();
        private final PackageRemovedListener mPackageRemovedListener = new PackageRemovedListener();

        public LauncherAppsImpl(Context context) {
            MyPackageMonitor myPackageMonitor = new MyPackageMonitor();
            this.mPackageMonitor = myPackageMonitor;
            this.mIsWatchingPackageBroadcasts = false;
            this.mPmsExt = MtkSystemServiceFactory.getInstance().makePmsExt();
            this.mContext = context;
            this.mIPM = AppGlobals.getPackageManager();
            this.mUm = (UserManager) context.getSystemService("user");
            UserManagerInternal userManagerInternal = (UserManagerInternal) Objects.requireNonNull((UserManagerInternal) LocalServices.getService(UserManagerInternal.class));
            this.mUserManagerInternal = userManagerInternal;
            this.mUsageStatsManagerInternal = (UsageStatsManagerInternal) Objects.requireNonNull((UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class));
            this.mActivityManagerInternal = (ActivityManagerInternal) Objects.requireNonNull((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class));
            this.mActivityTaskManagerInternal = (ActivityTaskManagerInternal) Objects.requireNonNull((ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class));
            ShortcutServiceInternal shortcutServiceInternal = (ShortcutServiceInternal) Objects.requireNonNull((ShortcutServiceInternal) LocalServices.getService(ShortcutServiceInternal.class));
            this.mShortcutServiceInternal = shortcutServiceInternal;
            this.mPackageManagerInternal = (PackageManagerInternal) Objects.requireNonNull((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class));
            shortcutServiceInternal.addListener(myPackageMonitor);
            ShortcutChangeHandler shortcutChangeHandler = new ShortcutChangeHandler(userManagerInternal);
            this.mShortcutChangeHandler = shortcutChangeHandler;
            shortcutServiceInternal.addShortcutChangeCallback(shortcutChangeHandler);
            this.mCallbackHandler = BackgroundThread.getHandler();
            this.mDpm = (DevicePolicyManager) context.getSystemService("device_policy");
            this.mInternal = new LocalService();
        }

        int injectBinderCallingUid() {
            return getCallingUid();
        }

        int injectBinderCallingPid() {
            return getCallingPid();
        }

        final int injectCallingUserId() {
            return UserHandle.getUserId(injectBinderCallingUid());
        }

        long injectClearCallingIdentity() {
            return Binder.clearCallingIdentity();
        }

        void injectRestoreCallingIdentity(long token) {
            Binder.restoreCallingIdentity(token);
        }

        private int getCallingUserId() {
            return UserHandle.getUserId(injectBinderCallingUid());
        }

        public void addOnAppsChangedListener(String callingPackage, IOnAppsChangedListener listener) throws RemoteException {
            verifyCallingPackage(callingPackage);
            synchronized (this.mListeners) {
                if (this.mListeners.getRegisteredCallbackCount() == 0) {
                    startWatchingPackageBroadcasts();
                }
                this.mListeners.unregister(listener);
                this.mListeners.register(listener, new BroadcastCookie(UserHandle.of(getCallingUserId()), callingPackage, injectBinderCallingPid(), injectBinderCallingUid()));
            }
        }

        public void removeOnAppsChangedListener(IOnAppsChangedListener listener) throws RemoteException {
            synchronized (this.mListeners) {
                this.mListeners.unregister(listener);
                if (this.mListeners.getRegisteredCallbackCount() == 0) {
                    stopWatchingPackageBroadcasts();
                }
            }
        }

        public void registerPackageInstallerCallback(String callingPackage, IPackageInstallerCallback callback) {
            verifyCallingPackage(callingPackage);
            final UserHandle callingIdUserHandle = new UserHandle(getCallingUserId());
            getPackageInstallerService().registerCallback(callback, new IntPredicate() { // from class: com.android.server.pm.LauncherAppsService$LauncherAppsImpl$$ExternalSyntheticLambda1
                @Override // java.util.function.IntPredicate
                public final boolean test(int i) {
                    return LauncherAppsService.LauncherAppsImpl.this.m5449x8c5b3974(callingIdUserHandle, i);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$registerPackageInstallerCallback$0$com-android-server-pm-LauncherAppsService$LauncherAppsImpl  reason: not valid java name */
        public /* synthetic */ boolean m5449x8c5b3974(UserHandle callingIdUserHandle, int eventUserId) {
            return isEnabledProfileOf(callingIdUserHandle, new UserHandle(eventUserId), "shouldReceiveEvent");
        }

        public ParceledListSlice<PackageInstaller.SessionInfo> getAllSessions(String callingPackage) {
            verifyCallingPackage(callingPackage);
            List<PackageInstaller.SessionInfo> sessionInfos = new ArrayList<>();
            int[] userIds = this.mUm.getEnabledProfileIds(getCallingUserId());
            final int callingUid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                for (int userId : userIds) {
                    sessionInfos.addAll(getPackageInstallerService().getAllSessions(userId).getList());
                }
                Binder.restoreCallingIdentity(token);
                sessionInfos.removeIf(new Predicate() { // from class: com.android.server.pm.LauncherAppsService$LauncherAppsImpl$$ExternalSyntheticLambda3
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return LauncherAppsService.LauncherAppsImpl.this.m5447xe627c635(callingUid, (PackageInstaller.SessionInfo) obj);
                    }
                });
                return new ParceledListSlice<>(sessionInfos);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
                throw th;
            }
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: private */
        /* renamed from: shouldFilterSession */
        public boolean m5447xe627c635(int uid, PackageInstaller.SessionInfo session) {
            return (session == null || uid == session.getInstallerUid() || this.mPackageManagerInternal.canQueryPackage(uid, session.getAppPackageName())) ? false : true;
        }

        private PackageInstallerService getPackageInstallerService() {
            if (this.mPackageInstallerService == null) {
                try {
                    this.mPackageInstallerService = ServiceManager.getService("package").getPackageInstaller();
                } catch (RemoteException e) {
                    Slog.wtf(TAG, "Error gettig IPackageInstaller", e);
                }
            }
            return this.mPackageInstallerService;
        }

        private void startWatchingPackageBroadcasts() {
            if (!this.mIsWatchingPackageBroadcasts) {
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.PACKAGE_REMOVED_INTERNAL");
                filter.addDataScheme("package");
                this.mContext.registerReceiverAsUser(this.mPackageRemovedListener, UserHandle.ALL, filter, null, this.mCallbackHandler);
                this.mPackageMonitor.register(this.mContext, UserHandle.ALL, true, this.mCallbackHandler);
                this.mIsWatchingPackageBroadcasts = true;
            }
        }

        private void stopWatchingPackageBroadcasts() {
            if (this.mIsWatchingPackageBroadcasts) {
                this.mContext.unregisterReceiver(this.mPackageRemovedListener);
                this.mPackageMonitor.unregister();
                this.mIsWatchingPackageBroadcasts = false;
            }
        }

        void checkCallbackCount() {
            synchronized (this.mListeners) {
                if (this.mListeners.getRegisteredCallbackCount() == 0) {
                    stopWatchingPackageBroadcasts();
                }
            }
        }

        private boolean canAccessProfile(int targetUserId, String message) {
            return canAccessProfile(injectBinderCallingUid(), injectCallingUserId(), injectBinderCallingPid(), targetUserId, message);
        }

        private boolean canAccessProfile(int callingUid, int callingUserId, int callingPid, int targetUserId, String message) {
            if (targetUserId == callingUserId || injectHasInteractAcrossUsersFullPermission(callingPid, callingUid)) {
                return true;
            }
            long ident = injectClearCallingIdentity();
            try {
                UserInfo callingUserInfo = this.mUm.getUserInfo(callingUserId);
                if (callingUserInfo != null && callingUserInfo.isProfile()) {
                    Slog.w(TAG, message + " for another profile " + targetUserId + " from " + callingUserId + " not allowed");
                    return false;
                }
                injectRestoreCallingIdentity(ident);
                return this.mUserManagerInternal.isProfileAccessible(callingUserId, targetUserId, message, true);
            } finally {
                injectRestoreCallingIdentity(ident);
            }
        }

        private void verifyCallingPackage(String callingPackage) {
            verifyCallingPackage(callingPackage, injectBinderCallingUid());
        }

        void verifyCallingPackage(String callingPackage, int callerUid) {
            int packageUid = -1;
            try {
                packageUid = this.mIPM.getPackageUid(callingPackage, 794624L, UserHandle.getUserId(callerUid));
            } catch (RemoteException e) {
            }
            if (packageUid < 0) {
                Log.e(TAG, "Package not found: " + callingPackage);
            }
            if (packageUid != callerUid) {
                throw new SecurityException("Calling package name mismatch");
            }
        }

        private LauncherActivityInfoInternal getHiddenAppActivityInfo(String packageName, int callingUid, UserHandle user) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(packageName, PackageManager.APP_DETAILS_ACTIVITY_CLASS_NAME));
            List<LauncherActivityInfoInternal> apps = queryIntentLauncherActivities(intent, callingUid, user);
            if (apps.size() > 0) {
                return apps.get(0);
            }
            return null;
        }

        public boolean shouldHideFromSuggestions(String packageName, UserHandle user) {
            int userId = user.getIdentifier();
            if (canAccessProfile(userId, "cannot get shouldHideFromSuggestions") && !this.mPackageManagerInternal.filterAppAccess(packageName, Binder.getCallingUid(), userId)) {
                int flags = this.mPackageManagerInternal.getDistractingPackageRestrictions(packageName, userId);
                return (flags & 1) != 0;
            }
            return false;
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [539=6] */
        public ParceledListSlice<LauncherActivityInfoInternal> getLauncherActivities(String callingPackage, String packageName, UserHandle user) throws RemoteException {
            LauncherActivityInfoInternal info;
            ParceledListSlice<LauncherActivityInfoInternal> launcherActivities = queryActivitiesForUser(callingPackage, new Intent("android.intent.action.MAIN").addCategory("android.intent.category.LAUNCHER").setPackage(packageName), user);
            if (Settings.Global.getInt(this.mContext.getContentResolver(), "show_hidden_icon_apps_enabled", 1) == 0) {
                return launcherActivities;
            }
            if (launcherActivities == null) {
                return null;
            }
            int callingUid = injectBinderCallingUid();
            long ident = injectClearCallingIdentity();
            try {
                if (this.mUm.getUserInfo(user.getIdentifier()).isManagedProfile()) {
                    return launcherActivities;
                }
                if (this.mDpm.getDeviceOwnerComponentOnAnyUser() != null) {
                    return launcherActivities;
                }
                ArrayList<LauncherActivityInfoInternal> result = new ArrayList<>(launcherActivities.getList());
                if (packageName != null) {
                    if (result.size() > 0) {
                        return launcherActivities;
                    }
                    ApplicationInfo appInfo = this.mPackageManagerInternal.getApplicationInfo(packageName, 0L, callingUid, user.getIdentifier());
                    if (shouldShowSyntheticActivity(user, appInfo) && (info = getHiddenAppActivityInfo(packageName, callingUid, user)) != null) {
                        result.add(info);
                    }
                    return new ParceledListSlice<>(result);
                }
                HashSet<String> visiblePackages = new HashSet<>();
                Iterator<LauncherActivityInfoInternal> it = result.iterator();
                while (it.hasNext()) {
                    visiblePackages.add(it.next().getActivityInfo().packageName);
                }
                List<ApplicationInfo> installedPackages = this.mPackageManagerInternal.getInstalledApplications(0L, user.getIdentifier(), callingUid);
                for (ApplicationInfo applicationInfo : installedPackages) {
                    if (!visiblePackages.contains(applicationInfo.packageName)) {
                        if (shouldShowSyntheticActivity(user, applicationInfo)) {
                            LauncherActivityInfoInternal info2 = getHiddenAppActivityInfo(applicationInfo.packageName, callingUid, user);
                            if (info2 != null) {
                                result.add(info2);
                            }
                        }
                    }
                }
                return new ParceledListSlice<>(result);
            } finally {
                injectRestoreCallingIdentity(ident);
            }
        }

        private boolean shouldShowSyntheticActivity(UserHandle user, ApplicationInfo appInfo) {
            AndroidPackage pkg;
            return (appInfo == null || appInfo.isSystemApp() || appInfo.isUpdatedSystemApp() || isManagedProfileAdmin(user, appInfo.packageName) || (pkg = this.mPackageManagerInternal.getPackage(appInfo.packageName)) == null || !requestsPermissions(pkg) || !hasDefaultEnableLauncherActivity(appInfo.packageName)) ? false : true;
        }

        private boolean requestsPermissions(AndroidPackage pkg) {
            return !ArrayUtils.isEmpty(pkg.getRequestedPermissions());
        }

        private boolean hasDefaultEnableLauncherActivity(String packageName) {
            Intent matchIntent = new Intent("android.intent.action.MAIN");
            matchIntent.addCategory("android.intent.category.LAUNCHER");
            matchIntent.setPackage(packageName);
            List<ResolveInfo> infoList = this.mPackageManagerInternal.queryIntentActivities(matchIntent, matchIntent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 512L, Binder.getCallingUid(), getCallingUserId());
            int size = infoList.size();
            for (int i = 0; i < size; i++) {
                if (infoList.get(i).activityInfo.enabled) {
                    return true;
                }
            }
            return false;
        }

        private boolean isManagedProfileAdmin(UserHandle user, String packageName) {
            ComponentName componentName;
            List<UserInfo> userInfoList = this.mUm.getProfiles(user.getIdentifier());
            for (int i = 0; i < userInfoList.size(); i++) {
                UserInfo userInfo = userInfoList.get(i);
                if (userInfo.isManagedProfile() && (componentName = this.mDpm.getProfileOwnerAsUser(userInfo.getUserHandle())) != null && componentName.getPackageName().equals(packageName)) {
                    return true;
                }
            }
            return false;
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [636=5] */
        public LauncherActivityInfoInternal resolveLauncherActivityInternal(String callingPackage, ComponentName component, UserHandle user) throws RemoteException {
            if (canAccessProfile(user.getIdentifier(), "Cannot resolve activity")) {
                int callingUid = injectBinderCallingUid();
                long ident = Binder.clearCallingIdentity();
                try {
                    ActivityInfo activityInfo = this.mPackageManagerInternal.getActivityInfo(component, 786432L, callingUid, user.getIdentifier());
                    if (activityInfo == null) {
                        return null;
                    }
                    if (component != null && component.getPackageName() != null) {
                        IncrementalStatesInfo incrementalStatesInfo = this.mPackageManagerInternal.getIncrementalStatesInfo(component.getPackageName(), callingUid, user.getIdentifier());
                        if (incrementalStatesInfo == null) {
                            return null;
                        }
                        LauncherActivityInfoInternal retInfo = new LauncherActivityInfoInternal(activityInfo, incrementalStatesInfo);
                        Binder.restoreCallingIdentity(ident);
                        this.mPmsExt.updateActivityInfoForRemovable(activityInfo);
                        return retInfo;
                    }
                    return null;
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
            return null;
        }

        public ParceledListSlice getShortcutConfigActivities(String callingPackage, String packageName, UserHandle user) throws RemoteException {
            return queryActivitiesForUser(callingPackage, new Intent("android.intent.action.CREATE_SHORTCUT").setPackage(packageName), user);
        }

        private ParceledListSlice<LauncherActivityInfoInternal> queryActivitiesForUser(String callingPackage, Intent intent, UserHandle user) throws RemoteException {
            if (!canAccessProfile(user.getIdentifier(), "Cannot retrieve activities")) {
                return null;
            }
            int callingUid = injectBinderCallingUid();
            long ident = injectClearCallingIdentity();
            try {
                List<LauncherActivityInfoInternal> apps = queryIntentLauncherActivities(intent, callingUid, user);
                injectRestoreCallingIdentity(ident);
                this.mPmsExt.updateResolveInfoListForRemovable(apps);
                return new ParceledListSlice<>(apps);
            } catch (Throwable th) {
                injectRestoreCallingIdentity(ident);
                throw th;
            }
        }

        private List<LauncherActivityInfoInternal> queryIntentLauncherActivities(Intent intent, int callingUid, UserHandle user) {
            IncrementalStatesInfo incrementalStatesInfo;
            List<ResolveInfo> apps = this.mPackageManagerInternal.queryIntentActivities(intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 786432L, callingUid, user.getIdentifier());
            int numResolveInfos = apps.size();
            List<LauncherActivityInfoInternal> results = new ArrayList<>();
            for (int i = 0; i < numResolveInfos; i++) {
                ResolveInfo ri = apps.get(i);
                String packageName = ri.activityInfo.packageName;
                if (packageName != null && (incrementalStatesInfo = this.mPackageManagerInternal.getIncrementalStatesInfo(packageName, callingUid, user.getIdentifier())) != null) {
                    if ((!ri.activityInfo.isEnabled() && IPackageManagerServiceLice.Instance().needSkipAppInfo(packageName, user.getIdentifier())) || IPackageManagerServiceLice.Instance().isTmpUnHideApp(packageName)) {
                        Slog.w(TAG, "queryIntentLauncherActivities() packageName=" + packageName + ", is hidden app, ignore!!!");
                    } else {
                        results.add(new LauncherActivityInfoInternal(ri.activityInfo, incrementalStatesInfo));
                    }
                }
            }
            return results;
        }

        public IntentSender getShortcutConfigActivityIntent(String callingPackage, final ComponentName component, UserHandle user) throws RemoteException {
            ensureShortcutPermission(callingPackage);
            IntentSender intentSender = null;
            if (canAccessProfile(user.getIdentifier(), "Cannot check package")) {
                Objects.requireNonNull(component);
                int callingUid = injectBinderCallingUid();
                long identity = Binder.clearCallingIdentity();
                try {
                    Intent packageIntent = new Intent("android.intent.action.CREATE_SHORTCUT").setPackage(component.getPackageName());
                    List<ResolveInfo> apps = this.mPackageManagerInternal.queryIntentActivities(packageIntent, packageIntent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 786432L, callingUid, user.getIdentifier());
                    if (apps.stream().anyMatch(new Predicate() { // from class: com.android.server.pm.LauncherAppsService$LauncherAppsImpl$$ExternalSyntheticLambda0
                        @Override // java.util.function.Predicate
                        public final boolean test(Object obj) {
                            boolean equals;
                            equals = component.getClassName().equals(((ResolveInfo) obj).activityInfo.name);
                            return equals;
                        }
                    })) {
                        Intent intent = new Intent("android.intent.action.CREATE_SHORTCUT").setComponent(component);
                        PendingIntent pi = PendingIntent.getActivityAsUser(this.mContext, 0, intent, 1409286144, null, user);
                        if (pi != null) {
                            intentSender = pi.getIntentSender();
                        }
                        return intentSender;
                    }
                    return null;
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
            return null;
        }

        public PendingIntent getShortcutIntent(String callingPackage, String packageName, String shortcutId, Bundle opts, UserHandle user) throws RemoteException {
            Objects.requireNonNull(callingPackage);
            Objects.requireNonNull(packageName);
            Objects.requireNonNull(shortcutId);
            Objects.requireNonNull(user);
            ensureShortcutPermission(callingPackage);
            if (canAccessProfile(user.getIdentifier(), "Cannot get shortcuts")) {
                AndroidFuture<Intent[]> ret = new AndroidFuture<>();
                this.mShortcutServiceInternal.createShortcutIntentsAsync(getCallingUserId(), callingPackage, packageName, shortcutId, user.getIdentifier(), injectBinderCallingPid(), injectBinderCallingUid(), ret);
                try {
                    Intent[] intents = (Intent[]) ret.get();
                    if (intents != null && intents.length != 0) {
                        long ident = Binder.clearCallingIdentity();
                        try {
                            try {
                                PendingIntent injectCreatePendingIntent = injectCreatePendingIntent(0, intents, AudioFormat.DTS_HD, opts, packageName, this.mPackageManagerInternal.getPackageUid(packageName, 268435456L, user.getIdentifier()));
                                Binder.restoreCallingIdentity(ident);
                                return injectCreatePendingIntent;
                            } catch (Throwable th) {
                                th = th;
                                Binder.restoreCallingIdentity(ident);
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                    return null;
                } catch (InterruptedException | ExecutionException e) {
                    return null;
                }
            }
            return null;
        }

        public boolean isPackageEnabled(String callingPackage, String packageName, UserHandle user) throws RemoteException {
            boolean z = false;
            if (canAccessProfile(user.getIdentifier(), "Cannot check package")) {
                int callingUid = injectBinderCallingUid();
                long ident = Binder.clearCallingIdentity();
                try {
                    PackageInfo info = this.mPackageManagerInternal.getPackageInfo(packageName, 786432L, callingUid, user.getIdentifier());
                    if (info != null) {
                        if (info.applicationInfo.enabled) {
                            z = true;
                        }
                    }
                    return z;
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
            return false;
        }

        public Bundle getSuspendedPackageLauncherExtras(String packageName, UserHandle user) {
            int callingUid = injectBinderCallingUid();
            int userId = user.getIdentifier();
            if (canAccessProfile(userId, "Cannot get launcher extras") && !this.mPackageManagerInternal.filterAppAccess(packageName, callingUid, userId)) {
                return this.mPackageManagerInternal.getSuspendedPackageLauncherExtras(packageName, userId);
            }
            return null;
        }

        public ApplicationInfo getApplicationInfo(String callingPackage, String packageName, int flags, UserHandle user) throws RemoteException {
            if (!canAccessProfile(user.getIdentifier(), "Cannot check package")) {
                return null;
            }
            int callingUid = injectBinderCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                ApplicationInfo info = this.mPackageManagerInternal.getApplicationInfo(packageName, flags, callingUid, user.getIdentifier());
                Binder.restoreCallingIdentity(ident);
                return this.mPmsExt.updateApplicationInfoForRemovable(AppGlobals.getPackageManager().getNameForUid(Binder.getCallingUid()), info);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        }

        public LauncherApps.AppUsageLimit getAppUsageLimit(String callingPackage, String packageName, UserHandle user) {
            verifyCallingPackage(callingPackage);
            if (canAccessProfile(user.getIdentifier(), "Cannot access usage limit")) {
                if (!this.mActivityTaskManagerInternal.isCallerRecents(Binder.getCallingUid())) {
                    throw new SecurityException("Caller is not the recents app");
                }
                UsageStatsManagerInternal.AppUsageLimitData data = this.mUsageStatsManagerInternal.getAppUsageLimit(packageName, user);
                if (data == null) {
                    return null;
                }
                return new LauncherApps.AppUsageLimit(data.getTotalUsageLimit(), data.getUsageRemaining());
            }
            return null;
        }

        private void ensureShortcutPermission(String callingPackage) {
            ensureShortcutPermission(injectBinderCallingUid(), injectBinderCallingPid(), callingPackage);
        }

        private void ensureShortcutPermission(int callerUid, int callerPid, String callingPackage) {
            verifyCallingPackage(callingPackage, callerUid);
            if (!this.mShortcutServiceInternal.hasShortcutHostPermission(UserHandle.getUserId(callerUid), callingPackage, callerPid, callerUid)) {
                throw new SecurityException("Caller can't access shortcut information");
            }
        }

        private void ensureStrictAccessShortcutsPermission(String callingPackage) {
            verifyCallingPackage(callingPackage);
            if (!injectHasAccessShortcutsPermission(injectBinderCallingPid(), injectBinderCallingUid())) {
                throw new SecurityException("Caller can't access shortcut information");
            }
        }

        boolean injectHasAccessShortcutsPermission(int callingPid, int callingUid) {
            return this.mContext.checkPermission("android.permission.ACCESS_SHORTCUTS", callingPid, callingUid) == 0;
        }

        boolean injectHasInteractAcrossUsersFullPermission(int callingPid, int callingUid) {
            return this.mContext.checkPermission("android.permission.INTERACT_ACROSS_USERS_FULL", callingPid, callingUid) == 0;
        }

        PendingIntent injectCreatePendingIntent(int requestCode, Intent[] intents, int flags, Bundle options, String ownerPackage, int ownerUserId) {
            return this.mActivityManagerInternal.getPendingIntentActivityAsApp(requestCode, intents, flags, (Bundle) null, ownerPackage, ownerUserId);
        }

        public ParceledListSlice getShortcuts(String callingPackage, ShortcutQueryWrapper query, UserHandle targetUser) {
            ensureShortcutPermission(callingPackage);
            if (!canAccessProfile(targetUser.getIdentifier(), "Cannot get shortcuts")) {
                return new ParceledListSlice(Collections.EMPTY_LIST);
            }
            long changedSince = query.getChangedSince();
            String packageName = query.getPackage();
            List<String> shortcutIds = query.getShortcutIds();
            List<LocusId> locusIds = query.getLocusIds();
            ComponentName componentName = query.getActivity();
            int flags = query.getQueryFlags();
            if (shortcutIds != null && packageName == null) {
                throw new IllegalArgumentException("To query by shortcut ID, package name must also be set");
            }
            if (locusIds != null && packageName == null) {
                throw new IllegalArgumentException("To query by locus ID, package name must also be set");
            }
            if ((query.getQueryFlags() & 2048) != 0) {
                ensureStrictAccessShortcutsPermission(callingPackage);
            }
            return new ParceledListSlice(this.mShortcutServiceInternal.getShortcuts(getCallingUserId(), callingPackage, changedSince, packageName, shortcutIds, locusIds, componentName, flags, targetUser.getIdentifier(), injectBinderCallingPid(), injectBinderCallingUid()));
        }

        public void getShortcutsAsync(String callingPackage, ShortcutQueryWrapper query, UserHandle targetUser, AndroidFuture<List<ShortcutInfo>> cb) {
            ensureShortcutPermission(callingPackage);
            if (!canAccessProfile(targetUser.getIdentifier(), "Cannot get shortcuts")) {
                cb.complete(Collections.EMPTY_LIST);
                return;
            }
            long changedSince = query.getChangedSince();
            String packageName = query.getPackage();
            List<String> shortcutIds = query.getShortcutIds();
            List<LocusId> locusIds = query.getLocusIds();
            ComponentName componentName = query.getActivity();
            int flags = query.getQueryFlags();
            if (shortcutIds != null && packageName == null) {
                throw new IllegalArgumentException("To query by shortcut ID, package name must also be set");
            }
            if (locusIds != null && packageName == null) {
                throw new IllegalArgumentException("To query by locus ID, package name must also be set");
            }
            if ((query.getQueryFlags() & 2048) != 0) {
                ensureStrictAccessShortcutsPermission(callingPackage);
            }
            this.mShortcutServiceInternal.getShortcutsAsync(getCallingUserId(), callingPackage, changedSince, packageName, shortcutIds, locusIds, componentName, flags, targetUser.getIdentifier(), injectBinderCallingPid(), injectBinderCallingUid(), cb);
        }

        public void registerShortcutChangeCallback(String callingPackage, ShortcutQueryWrapper query, IShortcutChangeCallback callback) {
            ensureShortcutPermission(callingPackage);
            if (query.getShortcutIds() != null && query.getPackage() == null) {
                throw new IllegalArgumentException("To query by shortcut ID, package name must also be set");
            }
            if (query.getLocusIds() != null && query.getPackage() == null) {
                throw new IllegalArgumentException("To query by locus ID, package name must also be set");
            }
            UserHandle user = UserHandle.of(injectCallingUserId());
            if (injectHasInteractAcrossUsersFullPermission(injectBinderCallingPid(), injectBinderCallingUid())) {
                user = null;
            }
            this.mShortcutChangeHandler.addShortcutChangeCallback(callback, query, user);
        }

        public void unregisterShortcutChangeCallback(String callingPackage, IShortcutChangeCallback callback) {
            ensureShortcutPermission(callingPackage);
            this.mShortcutChangeHandler.removeShortcutChangeCallback(callback);
        }

        public void pinShortcuts(String callingPackage, String packageName, List<String> ids, UserHandle targetUser) {
            ensureShortcutPermission(callingPackage);
            if (!canAccessProfile(targetUser.getIdentifier(), "Cannot pin shortcuts")) {
                return;
            }
            this.mShortcutServiceInternal.pinShortcuts(getCallingUserId(), callingPackage, packageName, ids, targetUser.getIdentifier());
        }

        public void cacheShortcuts(String callingPackage, String packageName, List<String> ids, UserHandle targetUser, int cacheFlags) {
            ensureStrictAccessShortcutsPermission(callingPackage);
            if (!canAccessProfile(targetUser.getIdentifier(), "Cannot cache shortcuts")) {
                return;
            }
            this.mShortcutServiceInternal.cacheShortcuts(getCallingUserId(), callingPackage, packageName, ids, targetUser.getIdentifier(), toShortcutsCacheFlags(cacheFlags));
        }

        public void uncacheShortcuts(String callingPackage, String packageName, List<String> ids, UserHandle targetUser, int cacheFlags) {
            ensureStrictAccessShortcutsPermission(callingPackage);
            if (!canAccessProfile(targetUser.getIdentifier(), "Cannot uncache shortcuts")) {
                return;
            }
            this.mShortcutServiceInternal.uncacheShortcuts(getCallingUserId(), callingPackage, packageName, ids, targetUser.getIdentifier(), toShortcutsCacheFlags(cacheFlags));
        }

        public int getShortcutIconResId(String callingPackage, String packageName, String id, int targetUserId) {
            ensureShortcutPermission(callingPackage);
            if (!canAccessProfile(targetUserId, "Cannot access shortcuts")) {
                return 0;
            }
            return this.mShortcutServiceInternal.getShortcutIconResId(getCallingUserId(), callingPackage, packageName, id, targetUserId);
        }

        public ParcelFileDescriptor getShortcutIconFd(String callingPackage, String packageName, String id, int targetUserId) {
            ensureShortcutPermission(callingPackage);
            if (!canAccessProfile(targetUserId, "Cannot access shortcuts")) {
                return null;
            }
            AndroidFuture<ParcelFileDescriptor> ret = new AndroidFuture<>();
            this.mShortcutServiceInternal.getShortcutIconFdAsync(getCallingUserId(), callingPackage, packageName, id, targetUserId, ret);
            try {
                return (ParcelFileDescriptor) ret.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        public String getShortcutIconUri(String callingPackage, String packageName, String shortcutId, int userId) {
            ensureShortcutPermission(callingPackage);
            if (!canAccessProfile(userId, "Cannot access shortcuts")) {
                return null;
            }
            AndroidFuture<String> ret = new AndroidFuture<>();
            this.mShortcutServiceInternal.getShortcutIconUriAsync(getCallingUserId(), callingPackage, packageName, shortcutId, userId, ret);
            try {
                return (String) ret.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        public boolean hasShortcutHostPermission(String callingPackage) {
            verifyCallingPackage(callingPackage);
            return this.mShortcutServiceInternal.hasShortcutHostPermission(getCallingUserId(), callingPackage, injectBinderCallingPid(), injectBinderCallingUid());
        }

        public boolean startShortcut(String callingPackage, String packageName, String featureId, String shortcutId, Rect sourceBounds, Bundle startActivityOptions, int targetUserId) {
            return startShortcutInner(injectBinderCallingUid(), injectBinderCallingPid(), injectCallingUserId(), callingPackage, packageName, featureId, shortcutId, sourceBounds, startActivityOptions, targetUserId);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean startShortcutInner(int callerUid, int callerPid, int callingUserId, String callingPackage, String packageName, String featureId, String shortcutId, Rect sourceBounds, Bundle startActivityOptions, int targetUserId) {
            Bundle startActivityOptions2;
            Bundle startActivityOptions3;
            verifyCallingPackage(callingPackage, callerUid);
            if (canAccessProfile(targetUserId, "Cannot start activity")) {
                if (!this.mShortcutServiceInternal.isPinnedByCaller(callingUserId, callingPackage, packageName, shortcutId, targetUserId)) {
                    ensureShortcutPermission(callerUid, callerPid, callingPackage);
                }
                AndroidFuture<Intent[]> ret = new AndroidFuture<>();
                this.mShortcutServiceInternal.createShortcutIntentsAsync(getCallingUserId(), callingPackage, packageName, shortcutId, targetUserId, injectBinderCallingPid(), injectBinderCallingUid(), ret);
                try {
                    Intent[] intents = (Intent[]) ret.get();
                    if (intents != null && intents.length != 0) {
                        ActivityOptions options = ActivityOptions.fromBundle(startActivityOptions);
                        if (options != null && options.isApplyActivityFlagsForBubbles()) {
                            intents[0].addFlags(524288);
                            intents[0].addFlags(134217728);
                        }
                        intents[0].addFlags(268435456);
                        intents[0].setSourceBounds(sourceBounds);
                        String splashScreenThemeResName = this.mShortcutServiceInternal.getShortcutStartingThemeResName(callingUserId, callingPackage, packageName, shortcutId, targetUserId);
                        if (splashScreenThemeResName != null && !splashScreenThemeResName.isEmpty()) {
                            if (startActivityOptions != null) {
                                startActivityOptions3 = startActivityOptions;
                            } else {
                                startActivityOptions3 = new Bundle();
                            }
                            startActivityOptions3.putString("android.activity.splashScreenTheme", splashScreenThemeResName);
                            startActivityOptions2 = startActivityOptions3;
                        } else {
                            startActivityOptions2 = startActivityOptions;
                        }
                        return startShortcutIntentsAsPublisher(intents, packageName, featureId, startActivityOptions2, targetUserId);
                    }
                    return false;
                } catch (InterruptedException | ExecutionException e) {
                    return false;
                }
            }
            return false;
        }

        private boolean startShortcutIntentsAsPublisher(Intent[] intents, String publisherPackage, String publishedFeatureId, Bundle startActivityOptions, int userId) {
            try {
                int code = this.mActivityTaskManagerInternal.startActivitiesAsPackage(publisherPackage, publishedFeatureId, userId, intents, startActivityOptions);
                if (ActivityManager.isStartResultSuccessful(code)) {
                    return true;
                }
                Log.e(TAG, "Couldn't start activity, code=" + code);
                return false;
            } catch (SecurityException e) {
                return false;
            }
        }

        public boolean isActivityEnabled(String callingPackage, ComponentName component, UserHandle user) throws RemoteException {
            boolean z = false;
            if (canAccessProfile(user.getIdentifier(), "Cannot check component")) {
                int callingUid = injectBinderCallingUid();
                int state = this.mPackageManagerInternal.getComponentEnabledSetting(component, callingUid, user.getIdentifier());
                switch (state) {
                    case 0:
                    default:
                        long ident = Binder.clearCallingIdentity();
                        try {
                            ActivityInfo info = this.mPackageManagerInternal.getActivityInfo(component, 786432L, callingUid, user.getIdentifier());
                            if (info != null) {
                                if (info.isEnabled()) {
                                    z = true;
                                }
                            }
                            return z;
                        } finally {
                            Binder.restoreCallingIdentity(ident);
                        }
                    case 1:
                        return true;
                    case 2:
                    case 3:
                    case 4:
                        return false;
                }
            }
            return false;
        }

        public void startSessionDetailsActivityAsUser(IApplicationThread caller, String callingPackage, String callingFeatureId, PackageInstaller.SessionInfo sessionInfo, Rect sourceBounds, Bundle opts, UserHandle userHandle) throws RemoteException {
            int userId = userHandle.getIdentifier();
            if (!canAccessProfile(userId, "Cannot start details activity")) {
                return;
            }
            Intent i = new Intent("android.intent.action.VIEW").setData(new Uri.Builder().scheme("market").authority("details").appendQueryParameter("id", sessionInfo.appPackageName).build()).putExtra("android.intent.extra.REFERRER", new Uri.Builder().scheme("android-app").authority(callingPackage).build());
            i.setSourceBounds(sourceBounds);
            this.mActivityTaskManagerInternal.startActivityAsUser(caller, callingPackage, callingFeatureId, i, null, 268435456, opts, userId);
        }

        public PendingIntent getActivityLaunchIntent(String callingPackage, ComponentName component, UserHandle user) {
            ensureShortcutPermission(callingPackage);
            if (!canAccessProfile(user.getIdentifier(), "Cannot start activity")) {
                throw new ActivityNotFoundException("Activity could not be found");
            }
            Intent launchIntent = getMainActivityLaunchIntent(component, user);
            if (launchIntent == null) {
                throw new SecurityException("Attempt to launch activity without  category Intent.CATEGORY_LAUNCHER " + component);
            }
            long ident = Binder.clearCallingIdentity();
            try {
                return PendingIntent.getActivityAsUser(this.mContext, 0, launchIntent, 33554432, null, user);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void startActivityAsUser(IApplicationThread caller, String callingPackage, String callingFeatureId, ComponentName component, Rect sourceBounds, Bundle opts, UserHandle user) throws RemoteException {
            if (!canAccessProfile(user.getIdentifier(), "Cannot start activity")) {
                return;
            }
            Intent launchIntent = getMainActivityLaunchIntent(component, user);
            if (launchIntent == null) {
                throw new SecurityException("Attempt to launch activity without  category Intent.CATEGORY_LAUNCHER " + component);
            }
            launchIntent.setSourceBounds(sourceBounds);
            this.mActivityTaskManagerInternal.startActivityAsUser(caller, callingPackage, callingFeatureId, launchIntent, null, 268435456, opts, user.getIdentifier());
        }

        /* JADX WARN: Code restructure failed: missing block: B:16:0x008a, code lost:
            if (r1 != false) goto L18;
         */
        /* JADX WARN: Code restructure failed: missing block: B:18:0x0090, code lost:
            return null;
         */
        /* JADX WARN: Code restructure failed: missing block: B:20:0x0095, code lost:
            return r0;
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        private Intent getMainActivityLaunchIntent(ComponentName component, UserHandle user) {
            Intent launchIntent = new Intent("android.intent.action.MAIN");
            launchIntent.addCategory("android.intent.category.LAUNCHER");
            launchIntent.addFlags(270532608);
            launchIntent.setPackage(component.getPackageName());
            boolean canLaunch = false;
            int callingUid = injectBinderCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                List<ResolveInfo> apps = this.mPackageManagerInternal.queryIntentActivities(launchIntent, launchIntent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 786432L, callingUid, user.getIdentifier());
                int size = apps.size();
                int i = 0;
                while (true) {
                    if (i >= size) {
                        break;
                    }
                    ActivityInfo activityInfo = apps.get(i).activityInfo;
                    if (!activityInfo.packageName.equals(component.getPackageName()) || !activityInfo.name.equals(component.getClassName())) {
                        i++;
                    } else if (!activityInfo.exported) {
                        throw new SecurityException("Cannot launch non-exported components " + component);
                    } else {
                        launchIntent.setPackage(null);
                        launchIntent.setComponent(component);
                        canLaunch = true;
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void showAppDetailsAsUser(IApplicationThread caller, String callingPackage, String callingFeatureId, ComponentName component, Rect sourceBounds, Bundle opts, UserHandle user) throws RemoteException {
            if (!canAccessProfile(user.getIdentifier(), "Cannot show app details")) {
                return;
            }
            long ident = Binder.clearCallingIdentity();
            try {
                String packageName = component.getPackageName();
                int uId = -1;
                try {
                    uId = this.mContext.getPackageManager().getApplicationInfo(packageName, 4194304).uid;
                } catch (PackageManager.NameNotFoundException e) {
                    Log.d(TAG, "package not found: " + e);
                }
                Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.fromParts("package", packageName, null));
                intent.putExtra("uId", uId);
                intent.setFlags(268468224);
                try {
                    intent.setSourceBounds(sourceBounds);
                    Binder.restoreCallingIdentity(ident);
                    this.mActivityTaskManagerInternal.startActivityAsUser(caller, callingPackage, callingFeatureId, intent, null, 268435456, opts, user.getIdentifier());
                } catch (Throwable th) {
                    th = th;
                    Binder.restoreCallingIdentity(ident);
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean isEnabledProfileOf(UserHandle listeningUser, UserHandle user, String debugMsg) {
            return this.mUserManagerInternal.isProfileAccessible(listeningUser.getIdentifier(), user.getIdentifier(), debugMsg, false);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean isPackageVisibleToListener(String packageName, BroadcastCookie cookie) {
            return !this.mPackageManagerInternal.filterAppAccess(packageName, cookie.callingUid, cookie.user.getIdentifier());
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static boolean isCallingAppIdAllowed(int[] appIdAllowList, int appId) {
            return appIdAllowList == null || appId < 10000 || Arrays.binarySearch(appIdAllowList, appId) > -1;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public String[] getFilteredPackageNames(String[] packageNames, BroadcastCookie cookie) {
            List<String> filteredPackageNames = new ArrayList<>();
            for (String packageName : packageNames) {
                if (isPackageVisibleToListener(packageName, cookie)) {
                    filteredPackageNames.add(packageName);
                }
            }
            return (String[]) filteredPackageNames.toArray(new String[filteredPackageNames.size()]);
        }

        private int toShortcutsCacheFlags(int cacheFlags) {
            int ret = 0;
            if (cacheFlags == 0) {
                ret = 16384;
            } else if (cacheFlags == 1) {
                ret = 1073741824;
            } else if (cacheFlags == 2) {
                ret = 536870912;
            }
            Preconditions.checkArgumentPositive(ret, "Invalid cache owner");
            return ret;
        }

        void postToPackageMonitorHandler(Runnable r) {
            this.mCallbackHandler.post(r);
        }

        void registerLoadingProgressForIncrementalApps() {
            List<UserHandle> users = this.mUm.getUserProfiles();
            if (users == null) {
                return;
            }
            for (final UserHandle user : users) {
                this.mPackageManagerInternal.forEachInstalledPackage(new Consumer() { // from class: com.android.server.pm.LauncherAppsService$LauncherAppsImpl$$ExternalSyntheticLambda2
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        LauncherAppsService.LauncherAppsImpl.this.m5448x71c5332c(user, (AndroidPackage) obj);
                    }
                }, user.getIdentifier());
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$registerLoadingProgressForIncrementalApps$3$com-android-server-pm-LauncherAppsService$LauncherAppsImpl  reason: not valid java name */
        public /* synthetic */ void m5448x71c5332c(UserHandle user, AndroidPackage pkg) {
            String packageName = pkg.getPackageName();
            if (this.mPackageManagerInternal.getIncrementalStatesInfo(packageName, Process.myUid(), user.getIdentifier()).isLoading()) {
                this.mPackageManagerInternal.registerInstalledLoadingProgressCallback(packageName, new PackageLoadingProgressCallback(packageName, user), user.getIdentifier());
            }
        }

        /* loaded from: classes2.dex */
        public static class ShortcutChangeHandler implements LauncherApps.ShortcutChangeCallback {
            private final RemoteCallbackList<IShortcutChangeCallback> mCallbacks = new RemoteCallbackList<>();
            private final UserManagerInternal mUserManagerInternal;

            ShortcutChangeHandler(UserManagerInternal userManager) {
                this.mUserManagerInternal = userManager;
            }

            public synchronized void addShortcutChangeCallback(IShortcutChangeCallback callback, ShortcutQueryWrapper query, UserHandle user) {
                this.mCallbacks.unregister(callback);
                this.mCallbacks.register(callback, new Pair(query, user));
            }

            public synchronized void removeShortcutChangeCallback(IShortcutChangeCallback callback) {
                this.mCallbacks.unregister(callback);
            }

            public void onShortcutsAddedOrUpdated(String packageName, List<ShortcutInfo> shortcuts, UserHandle user) {
                onShortcutEvent(packageName, shortcuts, user, false);
            }

            public void onShortcutsRemoved(String packageName, List<ShortcutInfo> shortcuts, UserHandle user) {
                onShortcutEvent(packageName, shortcuts, user, true);
            }

            private void onShortcutEvent(String packageName, List<ShortcutInfo> shortcuts, UserHandle user, boolean shortcutsRemoved) {
                int count = this.mCallbacks.beginBroadcast();
                for (int i = 0; i < count; i++) {
                    IShortcutChangeCallback callback = this.mCallbacks.getBroadcastItem(i);
                    Pair<ShortcutQueryWrapper, UserHandle> cookie = (Pair) this.mCallbacks.getBroadcastCookie(i);
                    UserHandle callbackUser = (UserHandle) cookie.second;
                    if (callbackUser == null || hasUserAccess(callbackUser, user)) {
                        List<ShortcutInfo> matchedList = filterShortcutsByQuery(packageName, shortcuts, (ShortcutQueryWrapper) cookie.first, shortcutsRemoved);
                        if (!CollectionUtils.isEmpty(matchedList)) {
                            if (shortcutsRemoved) {
                                try {
                                    callback.onShortcutsRemoved(packageName, matchedList, user);
                                } catch (RemoteException e) {
                                }
                            } else {
                                callback.onShortcutsAddedOrUpdated(packageName, matchedList, user);
                            }
                        }
                    }
                }
                this.mCallbacks.finishBroadcast();
            }

            public static List<ShortcutInfo> filterShortcutsByQuery(String packageName, List<ShortcutInfo> shortcuts, ShortcutQueryWrapper query, boolean shortcutsRemoved) {
                int flags;
                long changedSince = query.getChangedSince();
                String queryPackage = query.getPackage();
                List<String> shortcutIds = query.getShortcutIds();
                List<LocusId> locusIds = query.getLocusIds();
                ComponentName activity = query.getActivity();
                int flags2 = query.getQueryFlags();
                if (queryPackage != null && !queryPackage.equals(packageName)) {
                    return null;
                }
                List<ShortcutInfo> matches = new ArrayList<>();
                boolean matchDynamic = (flags2 & 1) != 0;
                boolean matchPinned = (flags2 & 2) != 0;
                boolean matchManifest = (flags2 & 8) != 0;
                boolean matchCached = (flags2 & 16) != 0;
                int shortcutFlags = (matchDynamic ? 1 : 0) | (matchPinned ? 2 : 0) | (matchManifest ? 32 : 0) | (matchCached ? 1610629120 : 0);
                int i = 0;
                while (i < shortcuts.size()) {
                    String queryPackage2 = queryPackage;
                    ShortcutInfo si = shortcuts.get(i);
                    if (activity != null) {
                        flags = flags2;
                        if (!activity.equals(si.getActivity())) {
                            i++;
                            flags2 = flags;
                            queryPackage = queryPackage2;
                        }
                    } else {
                        flags = flags2;
                    }
                    if ((changedSince == 0 || changedSince <= si.getLastChangedTimestamp()) && ((shortcutIds == null || shortcutIds.contains(si.getId())) && ((locusIds == null || locusIds.contains(si.getLocusId())) && (shortcutsRemoved || (si.getFlags() & shortcutFlags) != 0)))) {
                        matches.add(si);
                    }
                    i++;
                    flags2 = flags;
                    queryPackage = queryPackage2;
                }
                return matches;
            }

            private boolean hasUserAccess(UserHandle callbackUser, UserHandle shortcutUser) {
                int callbackUserId = callbackUser.getIdentifier();
                int shortcutUserId = shortcutUser.getIdentifier();
                if (shortcutUser == callbackUser) {
                    return true;
                }
                return this.mUserManagerInternal.isProfileAccessible(callbackUserId, shortcutUserId, null, false);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public class PackageRemovedListener extends BroadcastReceiver {
            private PackageRemovedListener() {
            }

            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                if (userId == -10000) {
                    Slog.w(LauncherAppsImpl.TAG, "Intent broadcast does not contain user handle: " + intent);
                    return;
                }
                String action = intent.getAction();
                if ("android.intent.action.PACKAGE_REMOVED_INTERNAL".equals(action)) {
                    String packageName = getPackageName(intent);
                    int[] appIdAllowList = intent.getIntArrayExtra("android.intent.extra.VISIBILITY_ALLOW_LIST");
                    if (packageName != null && !intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                        UserHandle user = new UserHandle(userId);
                        int n = LauncherAppsImpl.this.mListeners.beginBroadcast();
                        for (int i = 0; i < n; i++) {
                            try {
                                IOnAppsChangedListener listener = (IOnAppsChangedListener) LauncherAppsImpl.this.mListeners.getBroadcastItem(i);
                                BroadcastCookie cookie = (BroadcastCookie) LauncherAppsImpl.this.mListeners.getBroadcastCookie(i);
                                if (LauncherAppsImpl.this.isEnabledProfileOf(cookie.user, user, "onPackageRemoved") && LauncherAppsImpl.isCallingAppIdAllowed(appIdAllowList, UserHandle.getAppId(cookie.callingUid))) {
                                    try {
                                        listener.onPackageRemoved(user, packageName);
                                    } catch (RemoteException re) {
                                        Slog.d(LauncherAppsImpl.TAG, "Callback failed ", re);
                                    }
                                }
                            } finally {
                                LauncherAppsImpl.this.mListeners.finishBroadcast();
                            }
                        }
                    }
                }
            }

            private String getPackageName(Intent intent) {
                Uri uri = intent.getData();
                if (uri != null) {
                    String pkg = uri.getSchemeSpecificPart();
                    return pkg;
                }
                return null;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public class MyPackageMonitor extends PackageMonitor implements ShortcutServiceInternal.ShortcutChangeListener {
            private MyPackageMonitor() {
            }

            public void onPackageAdded(String packageName, int uid) {
                UserHandle user = new UserHandle(getChangingUserId());
                int n = LauncherAppsImpl.this.mListeners.beginBroadcast();
                for (int i = 0; i < n; i++) {
                    try {
                        IOnAppsChangedListener listener = (IOnAppsChangedListener) LauncherAppsImpl.this.mListeners.getBroadcastItem(i);
                        BroadcastCookie cookie = (BroadcastCookie) LauncherAppsImpl.this.mListeners.getBroadcastCookie(i);
                        if (LauncherAppsImpl.this.isEnabledProfileOf(cookie.user, user, "onPackageAdded") && LauncherAppsImpl.this.isPackageVisibleToListener(packageName, cookie)) {
                            try {
                                listener.onPackageAdded(user, packageName);
                            } catch (RemoteException re) {
                                Slog.d(LauncherAppsImpl.TAG, "Callback failed ", re);
                            }
                        }
                    } catch (Throwable th) {
                        LauncherAppsImpl.this.mListeners.finishBroadcast();
                        throw th;
                    }
                }
                LauncherAppsImpl.this.mListeners.finishBroadcast();
                super.onPackageAdded(packageName, uid);
                LauncherAppsImpl.this.mPackageManagerInternal.registerInstalledLoadingProgressCallback(packageName, new PackageLoadingProgressCallback(packageName, user), user.getIdentifier());
            }

            public void onPackageModified(String packageName) {
                onPackageChanged(packageName);
                super.onPackageModified(packageName);
            }

            private void onPackageChanged(String packageName) {
                UserHandle user = new UserHandle(getChangingUserId());
                int n = LauncherAppsImpl.this.mListeners.beginBroadcast();
                for (int i = 0; i < n; i++) {
                    try {
                        IOnAppsChangedListener listener = (IOnAppsChangedListener) LauncherAppsImpl.this.mListeners.getBroadcastItem(i);
                        BroadcastCookie cookie = (BroadcastCookie) LauncherAppsImpl.this.mListeners.getBroadcastCookie(i);
                        if (LauncherAppsImpl.this.isEnabledProfileOf(cookie.user, user, "onPackageModified") && LauncherAppsImpl.this.isPackageVisibleToListener(packageName, cookie)) {
                            try {
                                listener.onPackageChanged(user, packageName);
                            } catch (RemoteException re) {
                                Slog.d(LauncherAppsImpl.TAG, "Callback failed ", re);
                            }
                        }
                    } finally {
                        LauncherAppsImpl.this.mListeners.finishBroadcast();
                    }
                }
            }

            public void onPackagesAvailable(String[] packages) {
                UserHandle user = new UserHandle(getChangingUserId());
                int n = LauncherAppsImpl.this.mListeners.beginBroadcast();
                for (int i = 0; i < n; i++) {
                    try {
                        IOnAppsChangedListener listener = (IOnAppsChangedListener) LauncherAppsImpl.this.mListeners.getBroadcastItem(i);
                        BroadcastCookie cookie = (BroadcastCookie) LauncherAppsImpl.this.mListeners.getBroadcastCookie(i);
                        if (LauncherAppsImpl.this.isEnabledProfileOf(cookie.user, user, "onPackagesAvailable")) {
                            String[] filteredPackages = LauncherAppsImpl.this.getFilteredPackageNames(packages, cookie);
                            if (!ArrayUtils.isEmpty(filteredPackages)) {
                                try {
                                    listener.onPackagesAvailable(user, filteredPackages, isReplacing());
                                } catch (RemoteException re) {
                                    Slog.d(LauncherAppsImpl.TAG, "Callback failed ", re);
                                }
                            }
                        }
                    } catch (Throwable th) {
                        LauncherAppsImpl.this.mListeners.finishBroadcast();
                        throw th;
                    }
                }
                LauncherAppsImpl.this.mListeners.finishBroadcast();
                super.onPackagesAvailable(packages);
            }

            public void onPackagesUnavailable(String[] packages) {
                UserHandle user = new UserHandle(getChangingUserId());
                int n = LauncherAppsImpl.this.mListeners.beginBroadcast();
                for (int i = 0; i < n; i++) {
                    try {
                        IOnAppsChangedListener listener = (IOnAppsChangedListener) LauncherAppsImpl.this.mListeners.getBroadcastItem(i);
                        BroadcastCookie cookie = (BroadcastCookie) LauncherAppsImpl.this.mListeners.getBroadcastCookie(i);
                        if (LauncherAppsImpl.this.isEnabledProfileOf(cookie.user, user, "onPackagesUnavailable")) {
                            String[] filteredPackages = LauncherAppsImpl.this.getFilteredPackageNames(packages, cookie);
                            if (!ArrayUtils.isEmpty(filteredPackages)) {
                                try {
                                    listener.onPackagesUnavailable(user, filteredPackages, isReplacing());
                                } catch (RemoteException re) {
                                    Slog.d(LauncherAppsImpl.TAG, "Callback failed ", re);
                                }
                            }
                        }
                    } catch (Throwable th) {
                        LauncherAppsImpl.this.mListeners.finishBroadcast();
                        throw th;
                    }
                }
                LauncherAppsImpl.this.mListeners.finishBroadcast();
                super.onPackagesUnavailable(packages);
            }

            public void onPackagesSuspended(String[] packages) {
                UserHandle user = new UserHandle(getChangingUserId());
                ArrayList<Pair<String, Bundle>> packagesWithExtras = new ArrayList<>();
                ArrayList<String> packagesWithoutExtras = new ArrayList<>();
                for (String pkg : packages) {
                    Bundle launcherExtras = LauncherAppsImpl.this.mPackageManagerInternal.getSuspendedPackageLauncherExtras(pkg, user.getIdentifier());
                    if (launcherExtras != null) {
                        packagesWithExtras.add(new Pair<>(pkg, launcherExtras));
                    } else {
                        packagesWithoutExtras.add(pkg);
                    }
                }
                String[] packagesNullExtras = (String[]) packagesWithoutExtras.toArray(new String[packagesWithoutExtras.size()]);
                int n = LauncherAppsImpl.this.mListeners.beginBroadcast();
                for (int i = 0; i < n; i++) {
                    try {
                        IOnAppsChangedListener listener = (IOnAppsChangedListener) LauncherAppsImpl.this.mListeners.getBroadcastItem(i);
                        BroadcastCookie cookie = (BroadcastCookie) LauncherAppsImpl.this.mListeners.getBroadcastCookie(i);
                        if (LauncherAppsImpl.this.isEnabledProfileOf(cookie.user, user, "onPackagesSuspended")) {
                            String[] filteredPackagesWithoutExtras = LauncherAppsImpl.this.getFilteredPackageNames(packagesNullExtras, cookie);
                            try {
                                if (!ArrayUtils.isEmpty(filteredPackagesWithoutExtras)) {
                                    listener.onPackagesSuspended(user, filteredPackagesWithoutExtras, (Bundle) null);
                                }
                                for (int idx = 0; idx < packagesWithExtras.size(); idx++) {
                                    Pair<String, Bundle> packageExtraPair = packagesWithExtras.get(idx);
                                    if (LauncherAppsImpl.this.isPackageVisibleToListener((String) packageExtraPair.first, cookie)) {
                                        listener.onPackagesSuspended(user, new String[]{(String) packageExtraPair.first}, (Bundle) packageExtraPair.second);
                                    }
                                }
                            } catch (RemoteException re) {
                                Slog.d(LauncherAppsImpl.TAG, "Callback failed ", re);
                            }
                        }
                    } finally {
                        LauncherAppsImpl.this.mListeners.finishBroadcast();
                    }
                }
            }

            public void onPackagesUnsuspended(String[] packages) {
                UserHandle user = new UserHandle(getChangingUserId());
                int n = LauncherAppsImpl.this.mListeners.beginBroadcast();
                for (int i = 0; i < n; i++) {
                    try {
                        IOnAppsChangedListener listener = (IOnAppsChangedListener) LauncherAppsImpl.this.mListeners.getBroadcastItem(i);
                        BroadcastCookie cookie = (BroadcastCookie) LauncherAppsImpl.this.mListeners.getBroadcastCookie(i);
                        if (LauncherAppsImpl.this.isEnabledProfileOf(cookie.user, user, "onPackagesUnsuspended")) {
                            String[] filteredPackages = LauncherAppsImpl.this.getFilteredPackageNames(packages, cookie);
                            if (!ArrayUtils.isEmpty(filteredPackages)) {
                                try {
                                    listener.onPackagesUnsuspended(user, filteredPackages);
                                } catch (RemoteException re) {
                                    Slog.d(LauncherAppsImpl.TAG, "Callback failed ", re);
                                }
                            }
                        }
                    } catch (Throwable th) {
                        LauncherAppsImpl.this.mListeners.finishBroadcast();
                        throw th;
                    }
                }
                LauncherAppsImpl.this.mListeners.finishBroadcast();
                super.onPackagesUnsuspended(packages);
            }

            public void onShortcutChanged(final String packageName, final int userId) {
                LauncherAppsImpl.this.postToPackageMonitorHandler(new Runnable() { // from class: com.android.server.pm.LauncherAppsService$LauncherAppsImpl$MyPackageMonitor$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        LauncherAppsService.LauncherAppsImpl.MyPackageMonitor.this.m5450xd9b5d5db(packageName, userId);
                    }
                });
            }

            /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1897=5] */
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX INFO: Access modifiers changed from: private */
            /* renamed from: onShortcutChangedInner */
            public void m5450xd9b5d5db(String packageName, int userId) {
                String str;
                int i;
                int n;
                String str2;
                UserHandle user;
                MyPackageMonitor myPackageMonitor = this;
                String str3 = LauncherAppsImpl.TAG;
                int n2 = LauncherAppsImpl.this.mListeners.beginBroadcast();
                try {
                    UserHandle user2 = UserHandle.of(userId);
                    int i2 = 0;
                    while (i2 < n2) {
                        try {
                            IOnAppsChangedListener listener = (IOnAppsChangedListener) LauncherAppsImpl.this.mListeners.getBroadcastItem(i2);
                            BroadcastCookie cookie = (BroadcastCookie) LauncherAppsImpl.this.mListeners.getBroadcastCookie(i2);
                            if (!LauncherAppsImpl.this.isEnabledProfileOf(cookie.user, user2, "onShortcutChanged")) {
                                i = i2;
                                user = user2;
                                n = n2;
                                str2 = str3;
                            } else if (LauncherAppsImpl.this.isPackageVisibleToListener(packageName, cookie)) {
                                int launcherUserId = cookie.user.getIdentifier();
                                if (LauncherAppsImpl.this.mShortcutServiceInternal.hasShortcutHostPermission(launcherUserId, cookie.packageName, cookie.callingPid, cookie.callingUid)) {
                                    ShortcutServiceInternal shortcutServiceInternal = LauncherAppsImpl.this.mShortcutServiceInternal;
                                    i = i2;
                                    UserHandle user3 = user2;
                                    n = n2;
                                    str2 = str3;
                                    try {
                                        List<ShortcutInfo> list = shortcutServiceInternal.getShortcuts(launcherUserId, cookie.packageName, 0L, packageName, (List) null, (List) null, (ComponentName) null, 1055, userId, cookie.callingPid, cookie.callingUid);
                                        try {
                                            user = user3;
                                            try {
                                                listener.onShortcutChanged(user, packageName, new ParceledListSlice(list));
                                            } catch (RemoteException e) {
                                                re = e;
                                                Slog.d(str2, "Callback failed ", re);
                                                i2 = i + 1;
                                                str3 = str2;
                                                user2 = user;
                                                n2 = n;
                                                myPackageMonitor = this;
                                            }
                                        } catch (RemoteException e2) {
                                            re = e2;
                                            user = user3;
                                        }
                                    } catch (RuntimeException e3) {
                                        e = e3;
                                        str = str2;
                                        myPackageMonitor = this;
                                        try {
                                            Log.w(str, e.getMessage(), e);
                                            LauncherAppsImpl.this.mListeners.finishBroadcast();
                                        } catch (Throwable th) {
                                            th = th;
                                            LauncherAppsImpl.this.mListeners.finishBroadcast();
                                            throw th;
                                        }
                                    } catch (Throwable th2) {
                                        th = th2;
                                        myPackageMonitor = this;
                                        LauncherAppsImpl.this.mListeners.finishBroadcast();
                                        throw th;
                                    }
                                } else {
                                    i = i2;
                                    user = user2;
                                    n = n2;
                                    str2 = str3;
                                }
                            } else {
                                i = i2;
                                user = user2;
                                n = n2;
                                str2 = str3;
                            }
                            i2 = i + 1;
                            str3 = str2;
                            user2 = user;
                            n2 = n;
                            myPackageMonitor = this;
                        } catch (RuntimeException e4) {
                            e = e4;
                            myPackageMonitor = this;
                            str = str3;
                            Log.w(str, e.getMessage(), e);
                            LauncherAppsImpl.this.mListeners.finishBroadcast();
                        } catch (Throwable th3) {
                            th = th3;
                            myPackageMonitor = this;
                        }
                    }
                    myPackageMonitor = this;
                } catch (RuntimeException e5) {
                    e = e5;
                } catch (Throwable th4) {
                    th = th4;
                }
                LauncherAppsImpl.this.mListeners.finishBroadcast();
            }

            public void onPackageStateChanged(String packageName, int uid) {
                onPackageChanged(packageName);
                super.onPackageStateChanged(packageName, uid);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes2.dex */
        public class PackageCallbackList<T extends IInterface> extends RemoteCallbackList<T> {
            PackageCallbackList() {
            }

            @Override // android.os.RemoteCallbackList
            public void onCallbackDied(T callback, Object cookie) {
                LauncherAppsImpl.this.checkCallbackCount();
            }
        }

        /* loaded from: classes2.dex */
        class PackageLoadingProgressCallback extends PackageManagerInternal.InstalledLoadingProgressCallback {
            private String mPackageName;
            private UserHandle mUser;

            PackageLoadingProgressCallback(String packageName, UserHandle user) {
                super(LauncherAppsImpl.this.mCallbackHandler);
                this.mPackageName = packageName;
                this.mUser = user;
            }

            @Override // android.content.pm.PackageManagerInternal.InstalledLoadingProgressCallback
            public void onLoadingProgressChanged(float progress) {
                int n = LauncherAppsImpl.this.mListeners.beginBroadcast();
                for (int i = 0; i < n; i++) {
                    try {
                        IOnAppsChangedListener listener = (IOnAppsChangedListener) LauncherAppsImpl.this.mListeners.getBroadcastItem(i);
                        BroadcastCookie cookie = (BroadcastCookie) LauncherAppsImpl.this.mListeners.getBroadcastCookie(i);
                        if (LauncherAppsImpl.this.isEnabledProfileOf(cookie.user, this.mUser, "onLoadingProgressChanged") && LauncherAppsImpl.this.isPackageVisibleToListener(this.mPackageName, cookie)) {
                            try {
                                listener.onPackageLoadingProgressChanged(this.mUser, this.mPackageName, progress);
                            } catch (RemoteException re) {
                                Slog.d(LauncherAppsImpl.TAG, "Callback failed ", re);
                            }
                        }
                    } finally {
                        LauncherAppsImpl.this.mListeners.finishBroadcast();
                    }
                }
            }
        }

        /* loaded from: classes2.dex */
        final class LocalService extends LauncherAppsServiceInternal {
            LocalService() {
            }

            @Override // com.android.server.pm.LauncherAppsService.LauncherAppsServiceInternal
            public boolean startShortcut(int callerUid, int callerPid, String callingPackage, String packageName, String featureId, String shortcutId, Rect sourceBounds, Bundle startActivityOptions, int targetUserId) {
                return LauncherAppsImpl.this.startShortcutInner(callerUid, callerPid, UserHandle.getUserId(callerUid), callingPackage, packageName, featureId, shortcutId, sourceBounds, startActivityOptions, targetUserId);
            }
        }
    }
}
