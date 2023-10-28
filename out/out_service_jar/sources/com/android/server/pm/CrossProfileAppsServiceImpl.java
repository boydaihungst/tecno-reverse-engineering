package com.android.server.pm;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IApplicationThread;
import android.app.admin.DevicePolicyEventLogger;
import android.app.admin.DevicePolicyManagerInternal;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.PermissionChecker;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.CrossProfileAppsInternal;
import android.content.pm.ICrossProfileApps;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ResolveInfo;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FunctionalUtils;
import com.android.server.LocalServices;
import com.android.server.pm.permission.PermissionManagerService;
import com.android.server.wm.ActivityTaskManagerInternal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
/* loaded from: classes2.dex */
public class CrossProfileAppsServiceImpl extends ICrossProfileApps.Stub {
    private static final String TAG = "CrossProfileAppsService";
    private Context mContext;
    private Injector mInjector;
    private final LocalService mLocalService;

    /* loaded from: classes2.dex */
    public interface Injector {
        int checkComponentPermission(String str, int i, int i2, boolean z);

        long clearCallingIdentity();

        ActivityManagerInternal getActivityManagerInternal();

        ActivityTaskManagerInternal getActivityTaskManagerInternal();

        AppOpsManager getAppOpsManager();

        int getCallingPid();

        int getCallingUid();

        UserHandle getCallingUserHandle();

        int getCallingUserId();

        DevicePolicyManagerInternal getDevicePolicyManagerInternal();

        IPackageManager getIPackageManager();

        PackageManager getPackageManager();

        PackageManagerInternal getPackageManagerInternal();

        UserManager getUserManager();

        void killUid(int i);

        void restoreCallingIdentity(long j);

        void sendBroadcastAsUser(Intent intent, UserHandle userHandle);

        <T> T withCleanCallingIdentity(FunctionalUtils.ThrowingSupplier<T> throwingSupplier);

        void withCleanCallingIdentity(FunctionalUtils.ThrowingRunnable throwingRunnable);
    }

    public CrossProfileAppsServiceImpl(Context context) {
        this(context, new InjectorImpl(context));
    }

    CrossProfileAppsServiceImpl(Context context, Injector injector) {
        this.mLocalService = new LocalService();
        this.mContext = context;
        this.mInjector = injector;
    }

    public List<UserHandle> getTargetUserProfiles(String callingPackage) {
        Objects.requireNonNull(callingPackage);
        verifyCallingPackage(callingPackage);
        DevicePolicyEventLogger.createEvent(125).setStrings(new String[]{callingPackage}).write();
        return getTargetUserProfilesUnchecked(callingPackage, this.mInjector.getCallingUserId());
    }

    public void startActivityAsUser(IApplicationThread caller, String callingPackage, String callingFeatureId, ComponentName component, int userId, boolean launchMainActivity, IBinder targetTask, Bundle options) throws RemoteException {
        Bundle options2;
        Objects.requireNonNull(callingPackage);
        Objects.requireNonNull(component);
        verifyCallingPackage(callingPackage);
        DevicePolicyEventLogger.createEvent(126).setStrings(new String[]{callingPackage}).write();
        int callerUserId = this.mInjector.getCallingUserId();
        int callingUid = this.mInjector.getCallingUid();
        int callingPid = this.mInjector.getCallingPid();
        List<UserHandle> allowedTargetUsers = getTargetUserProfilesUnchecked(callingPackage, callerUserId);
        if (!allowedTargetUsers.contains(UserHandle.of(userId))) {
            throw new SecurityException(callingPackage + " cannot access unrelated user " + userId);
        }
        if (!callingPackage.equals(component.getPackageName())) {
            throw new SecurityException(callingPackage + " attempts to start an activity in other package - " + component.getPackageName());
        }
        Intent launchIntent = new Intent();
        if (launchMainActivity) {
            launchIntent.setAction("android.intent.action.MAIN");
            launchIntent.addCategory("android.intent.category.LAUNCHER");
            if (targetTask == null) {
                launchIntent.addFlags(270532608);
            } else {
                launchIntent.addFlags(2097152);
            }
            launchIntent.setPackage(component.getPackageName());
        } else {
            if (callerUserId != userId) {
                if (!hasInteractAcrossProfilesPermission(callingPackage, callingUid, callingPid) && !isPermissionGranted("android.permission.START_CROSS_PROFILE_ACTIVITIES", callingUid)) {
                    throw new SecurityException("Attempt to launch activity without one of the required android.permission.INTERACT_ACROSS_PROFILES or android.permission.START_CROSS_PROFILE_ACTIVITIES permissions.");
                }
                if (!isSameProfileGroup(callerUserId, userId)) {
                    throw new SecurityException("Attempt to launch activity when target user is not in the same profile group.");
                }
            }
            launchIntent.setComponent(component);
        }
        verifyActivityCanHandleIntentAndExported(launchIntent, component, callingUid, userId);
        if (options == null) {
            options2 = ActivityOptions.makeOpenCrossProfileAppsAnimation().toBundle();
        } else {
            options.putAll(ActivityOptions.makeOpenCrossProfileAppsAnimation().toBundle());
            options2 = options;
        }
        launchIntent.setPackage(null);
        launchIntent.setComponent(component);
        this.mInjector.getActivityTaskManagerInternal().startActivityAsUser(caller, callingPackage, callingFeatureId, launchIntent, targetTask, 0, options2, userId);
    }

    public void startActivityAsUserByIntent(IApplicationThread caller, String callingPackage, String callingFeatureId, Intent intent, int userId, IBinder callingActivity, Bundle options) throws RemoteException {
        Objects.requireNonNull(callingPackage);
        Objects.requireNonNull(intent);
        Objects.requireNonNull(intent.getComponent(), "The intent must have a Component set");
        verifyCallingPackage(callingPackage);
        int callerUserId = this.mInjector.getCallingUserId();
        int callingUid = this.mInjector.getCallingUid();
        List<UserHandle> allowedTargetUsers = getTargetUserProfilesUnchecked(callingPackage, callerUserId);
        if (callerUserId != userId && !allowedTargetUsers.contains(UserHandle.of(userId))) {
            throw new SecurityException(callingPackage + " cannot access unrelated user " + userId);
        }
        Intent launchIntent = new Intent(intent);
        launchIntent.setPackage(callingPackage);
        if (!callingPackage.equals(launchIntent.getComponent().getPackageName())) {
            throw new SecurityException(callingPackage + " attempts to start an activity in other package - " + launchIntent.getComponent().getPackageName());
        }
        if (callerUserId != userId && !hasCallerGotInteractAcrossProfilesPermission(callingPackage)) {
            throw new SecurityException("Attempt to launch activity without required android.permission.INTERACT_ACROSS_PROFILES permission or target user is not in the same profile group.");
        }
        verifyActivityCanHandleIntent(launchIntent, callingUid, userId);
        this.mInjector.getActivityTaskManagerInternal().startActivityAsUser(caller, callingPackage, callingFeatureId, launchIntent, callingActivity, 0, options, userId);
        logStartActivityByIntent(callingPackage);
    }

    private void logStartActivityByIntent(String packageName) {
        DevicePolicyEventLogger.createEvent(150).setStrings(new String[]{packageName}).setBoolean(isCallingUserAManagedProfile()).write();
    }

    public boolean canRequestInteractAcrossProfiles(String callingPackage) {
        Objects.requireNonNull(callingPackage);
        verifyCallingPackage(callingPackage);
        return canRequestInteractAcrossProfilesUnchecked(callingPackage);
    }

    private boolean canRequestInteractAcrossProfilesUnchecked(String packageName) {
        int[] enabledProfileIds = this.mInjector.getUserManager().getEnabledProfileIds(this.mInjector.getCallingUserId());
        if (enabledProfileIds.length >= 2 && !isProfileOwner(packageName, enabledProfileIds)) {
            return hasRequestedAppOpPermission(AppOpsManager.opToPermission(93), packageName);
        }
        return false;
    }

    private boolean hasRequestedAppOpPermission(String permission, String packageName) {
        try {
            String[] packages = this.mInjector.getIPackageManager().getAppOpPermissionPackages(permission);
            return ArrayUtils.contains(packages, packageName);
        } catch (RemoteException e) {
            Slog.e(TAG, "PackageManager dead. Cannot get permission info");
            return false;
        }
    }

    public boolean canInteractAcrossProfiles(String callingPackage) {
        Objects.requireNonNull(callingPackage);
        verifyCallingPackage(callingPackage);
        List<UserHandle> targetUserProfiles = getTargetUserProfilesUnchecked(callingPackage, this.mInjector.getCallingUserId());
        return !targetUserProfiles.isEmpty() && hasCallerGotInteractAcrossProfilesPermission(callingPackage) && haveProfilesGotInteractAcrossProfilesPermission(callingPackage, targetUserProfiles);
    }

    private boolean hasCallerGotInteractAcrossProfilesPermission(String callingPackage) {
        return hasInteractAcrossProfilesPermission(callingPackage, this.mInjector.getCallingUid(), this.mInjector.getCallingPid());
    }

    /* JADX WARN: Removed duplicated region for block: B:5:0x000a  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private boolean haveProfilesGotInteractAcrossProfilesPermission(final String packageName, List<UserHandle> profiles) {
        for (final UserHandle profile : profiles) {
            int uid = ((Integer) this.mInjector.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.pm.CrossProfileAppsServiceImpl$$ExternalSyntheticLambda8
                public final Object getOrThrow() {
                    return CrossProfileAppsServiceImpl.this.m5392xfc7789bd(packageName, profile);
                }
            })).intValue();
            if (uid == -1 || !hasInteractAcrossProfilesPermission(packageName, uid, -1)) {
                return false;
            }
            while (r0.hasNext()) {
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$haveProfilesGotInteractAcrossProfilesPermission$0$com-android-server-pm-CrossProfileAppsServiceImpl  reason: not valid java name */
    public /* synthetic */ Integer m5392xfc7789bd(String packageName, UserHandle profile) throws Exception {
        try {
            return Integer.valueOf(this.mInjector.getPackageManager().getPackageUidAsUser(packageName, 0, profile.getIdentifier()));
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    private boolean isCrossProfilePackageAllowlisted(final String packageName) {
        return ((Boolean) this.mInjector.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.pm.CrossProfileAppsServiceImpl$$ExternalSyntheticLambda1
            public final Object getOrThrow() {
                return CrossProfileAppsServiceImpl.this.m5393x81a5e7c3(packageName);
            }
        })).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isCrossProfilePackageAllowlisted$1$com-android-server-pm-CrossProfileAppsServiceImpl  reason: not valid java name */
    public /* synthetic */ Boolean m5393x81a5e7c3(String packageName) throws Exception {
        return Boolean.valueOf(this.mInjector.getDevicePolicyManagerInternal().getAllCrossProfilePackages().contains(packageName));
    }

    private boolean isCrossProfilePackageAllowlistedByDefault(final String packageName) {
        return ((Boolean) this.mInjector.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.pm.CrossProfileAppsServiceImpl$$ExternalSyntheticLambda9
            public final Object getOrThrow() {
                return CrossProfileAppsServiceImpl.this.m5394xdf0537ae(packageName);
            }
        })).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isCrossProfilePackageAllowlistedByDefault$2$com-android-server-pm-CrossProfileAppsServiceImpl  reason: not valid java name */
    public /* synthetic */ Boolean m5394xdf0537ae(String packageName) throws Exception {
        return Boolean.valueOf(this.mInjector.getDevicePolicyManagerInternal().getDefaultCrossProfilePackages().contains(packageName));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public List<UserHandle> getTargetUserProfilesUnchecked(final String packageName, final int userId) {
        return (List) this.mInjector.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.pm.CrossProfileAppsServiceImpl$$ExternalSyntheticLambda12
            public final Object getOrThrow() {
                return CrossProfileAppsServiceImpl.this.m5390xe67f4144(userId, packageName);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getTargetUserProfilesUnchecked$3$com-android-server-pm-CrossProfileAppsServiceImpl  reason: not valid java name */
    public /* synthetic */ List m5390xe67f4144(int userId, String packageName) throws Exception {
        int[] enabledProfileIds = this.mInjector.getUserManager().getEnabledProfileIds(userId);
        List<UserHandle> targetProfiles = new ArrayList<>();
        for (int profileId : enabledProfileIds) {
            if (profileId != userId && isPackageEnabled(packageName, profileId)) {
                targetProfiles.add(UserHandle.of(profileId));
            }
        }
        return targetProfiles;
    }

    private boolean isPackageEnabled(final String packageName, final int userId) {
        final int callingUid = this.mInjector.getCallingUid();
        return ((Boolean) this.mInjector.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.pm.CrossProfileAppsServiceImpl$$ExternalSyntheticLambda14
            public final Object getOrThrow() {
                return CrossProfileAppsServiceImpl.this.m5396xda06ded8(packageName, callingUid, userId);
            }
        })).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isPackageEnabled$4$com-android-server-pm-CrossProfileAppsServiceImpl  reason: not valid java name */
    public /* synthetic */ Boolean m5396xda06ded8(String packageName, int callingUid, int userId) throws Exception {
        PackageInfo info = this.mInjector.getPackageManagerInternal().getPackageInfo(packageName, 786432L, callingUid, userId);
        return Boolean.valueOf(info != null && info.applicationInfo.enabled);
    }

    private void verifyActivityCanHandleIntent(final Intent launchIntent, final int callingUid, final int userId) {
        this.mInjector.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.pm.CrossProfileAppsServiceImpl$$ExternalSyntheticLambda11
            public final void runOrThrow() {
                CrossProfileAppsServiceImpl.this.m5401xfe2263e6(launchIntent, callingUid, userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$verifyActivityCanHandleIntent$5$com-android-server-pm-CrossProfileAppsServiceImpl  reason: not valid java name */
    public /* synthetic */ void m5401xfe2263e6(Intent launchIntent, int callingUid, int userId) throws Exception {
        List<ResolveInfo> activities = this.mInjector.getPackageManagerInternal().queryIntentActivities(launchIntent, launchIntent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 786432L, callingUid, userId);
        if (!activities.isEmpty()) {
            return;
        }
        throw new SecurityException("Activity cannot handle intent");
    }

    private void verifyActivityCanHandleIntentAndExported(final Intent launchIntent, final ComponentName component, final int callingUid, final int userId) {
        this.mInjector.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.pm.CrossProfileAppsServiceImpl$$ExternalSyntheticLambda2
            public final void runOrThrow() {
                CrossProfileAppsServiceImpl.this.m5402x4c1af493(launchIntent, callingUid, userId, component);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$verifyActivityCanHandleIntentAndExported$6$com-android-server-pm-CrossProfileAppsServiceImpl  reason: not valid java name */
    public /* synthetic */ void m5402x4c1af493(Intent launchIntent, int callingUid, int userId, ComponentName component) throws Exception {
        List<ResolveInfo> apps = this.mInjector.getPackageManagerInternal().queryIntentActivities(launchIntent, launchIntent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 786432L, callingUid, userId);
        int size = apps.size();
        for (int i = 0; i < size; i++) {
            ActivityInfo activityInfo = apps.get(i).activityInfo;
            if (TextUtils.equals(activityInfo.packageName, component.getPackageName()) && TextUtils.equals(activityInfo.name, component.getClassName()) && activityInfo.exported) {
                return;
            }
        }
        throw new SecurityException("Attempt to launch activity without  category Intent.CATEGORY_LAUNCHER or activity is not exported" + component);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* renamed from: setInteractAcrossProfilesAppOp */
    public void m5388xfaa3a0a(String packageName, int newMode) {
        setInteractAcrossProfilesAppOp(packageName, newMode, this.mInjector.getCallingUserId());
    }

    private void setInteractAcrossProfilesAppOp(String packageName, int newMode, int userId) {
        int callingUid = this.mInjector.getCallingUid();
        if (!isPermissionGranted("android.permission.INTERACT_ACROSS_USERS_FULL", callingUid) && !isPermissionGranted("android.permission.INTERACT_ACROSS_USERS", callingUid)) {
            throw new SecurityException("INTERACT_ACROSS_USERS or INTERACT_ACROSS_USERS_FULL is required to set the app-op for interacting across profiles.");
        }
        if (!isPermissionGranted("android.permission.MANAGE_APP_OPS_MODES", callingUid) && !isPermissionGranted("android.permission.CONFIGURE_INTERACT_ACROSS_PROFILES", callingUid)) {
            throw new SecurityException("MANAGE_APP_OPS_MODES or CONFIGURE_INTERACT_ACROSS_PROFILES is required to set the app-op for interacting across profiles.");
        }
        setInteractAcrossProfilesAppOpUnchecked(packageName, newMode, userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setInteractAcrossProfilesAppOpUnchecked(String packageName, int newMode, int userId) {
        if (newMode == 0 && !canConfigureInteractAcrossProfiles(packageName, userId)) {
            Slog.e(TAG, "Tried to turn on the appop for interacting across profiles for invalid app " + packageName);
            return;
        }
        int[] profileIds = this.mInjector.getUserManager().getProfileIds(userId, false);
        int length = profileIds.length;
        for (int i = 0; i < length; i++) {
            int profileId = profileIds[i];
            if (isPackageInstalled(packageName, profileId)) {
                setInteractAcrossProfilesAppOpForProfile(packageName, newMode, profileId, profileId == userId);
            }
        }
    }

    private boolean isPackageInstalled(final String packageName, final int userId) {
        return ((Boolean) this.mInjector.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.pm.CrossProfileAppsServiceImpl$$ExternalSyntheticLambda0
            public final Object getOrThrow() {
                return CrossProfileAppsServiceImpl.this.m5397xb48d9594(packageName, userId);
            }
        })).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isPackageInstalled$7$com-android-server-pm-CrossProfileAppsServiceImpl  reason: not valid java name */
    public /* synthetic */ Boolean m5397xb48d9594(String packageName, int userId) throws Exception {
        PackageInfo info = this.mInjector.getPackageManagerInternal().getPackageInfo(packageName, 786432L, this.mInjector.getCallingUid(), userId);
        return Boolean.valueOf(info != null);
    }

    private void setInteractAcrossProfilesAppOpForProfile(String packageName, int newMode, int profileId, boolean logMetrics) {
        try {
            setInteractAcrossProfilesAppOpForProfileOrThrow(packageName, newMode, profileId, logMetrics);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "Missing package " + packageName + " on profile user ID " + profileId, e);
        }
    }

    private void setInteractAcrossProfilesAppOpForProfileOrThrow(String packageName, final int newMode, int profileId, boolean logMetrics) throws PackageManager.NameNotFoundException {
        final int uid = this.mInjector.getPackageManager().getPackageUidAsUser(packageName, 0, profileId);
        if (currentModeEquals(newMode, packageName, uid)) {
            Slog.i(TAG, "Attempt to set mode to existing value of " + newMode + " for " + packageName + " on profile user ID " + profileId);
            return;
        }
        boolean hadPermission = hasInteractAcrossProfilesPermission(packageName, uid, -1);
        if (isPermissionGranted("android.permission.CONFIGURE_INTERACT_ACROSS_PROFILES", this.mInjector.getCallingUid())) {
            this.mInjector.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.pm.CrossProfileAppsServiceImpl$$ExternalSyntheticLambda13
                public final void runOrThrow() {
                    CrossProfileAppsServiceImpl.this.m5400x59a74c5f(uid, newMode);
                }
            });
        } else {
            this.mInjector.getAppOpsManager().setUidMode(93, uid, newMode);
        }
        maybeKillUid(packageName, uid, hadPermission);
        sendCanInteractAcrossProfilesChangedBroadcast(packageName, UserHandle.of(profileId));
        maybeLogSetInteractAcrossProfilesAppOp(packageName, newMode, logMetrics);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setInteractAcrossProfilesAppOpForProfileOrThrow$8$com-android-server-pm-CrossProfileAppsServiceImpl  reason: not valid java name */
    public /* synthetic */ void m5400x59a74c5f(int uid, int newMode) throws Exception {
        this.mInjector.getAppOpsManager().setUidMode(93, uid, newMode);
    }

    private void maybeKillUid(String packageName, int uid, boolean hadPermission) {
        if (!hadPermission || hasInteractAcrossProfilesPermission(packageName, uid, -1)) {
            return;
        }
        this.mInjector.killUid(uid);
    }

    private void maybeLogSetInteractAcrossProfilesAppOp(String packageName, int newMode, boolean logMetrics) {
        if (!logMetrics) {
            return;
        }
        DevicePolicyEventLogger.createEvent(139).setStrings(new String[]{packageName}).setInt(newMode).setBoolean(appDeclaresCrossProfileAttribute(packageName)).write();
    }

    private boolean currentModeEquals(final int otherMode, final String packageName, final int uid) {
        final String op = AppOpsManager.permissionToOp("android.permission.INTERACT_ACROSS_PROFILES");
        return ((Boolean) this.mInjector.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.pm.CrossProfileAppsServiceImpl$$ExternalSyntheticLambda3
            public final Object getOrThrow() {
                return CrossProfileAppsServiceImpl.this.m5389xe2ce8879(otherMode, op, uid, packageName);
            }
        })).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$currentModeEquals$9$com-android-server-pm-CrossProfileAppsServiceImpl  reason: not valid java name */
    public /* synthetic */ Boolean m5389xe2ce8879(int otherMode, String op, int uid, String packageName) throws Exception {
        return Boolean.valueOf(otherMode == this.mInjector.getAppOpsManager().unsafeCheckOpNoThrow(op, uid, packageName));
    }

    private void sendCanInteractAcrossProfilesChangedBroadcast(String packageName, UserHandle userHandle) {
        Intent intent = new Intent("android.content.pm.action.CAN_INTERACT_ACROSS_PROFILES_CHANGED").setPackage(packageName);
        if (appDeclaresCrossProfileAttribute(packageName)) {
            intent.addFlags(AudioFormat.EVRCB);
        } else {
            intent.addFlags(1073741824);
        }
        for (ResolveInfo receiver : findBroadcastReceiversForUser(intent, userHandle)) {
            intent.setComponent(receiver.getComponentInfo().getComponentName());
            this.mInjector.sendBroadcastAsUser(intent, userHandle);
        }
    }

    private List<ResolveInfo> findBroadcastReceiversForUser(Intent intent, UserHandle userHandle) {
        return this.mInjector.getPackageManager().queryBroadcastReceiversAsUser(intent, 0, userHandle);
    }

    private boolean appDeclaresCrossProfileAttribute(String packageName) {
        return this.mInjector.getPackageManagerInternal().getPackage(packageName).isCrossProfile();
    }

    public boolean canConfigureInteractAcrossProfiles(String packageName) {
        return canConfigureInteractAcrossProfiles(packageName, this.mInjector.getCallingUserId());
    }

    private boolean canConfigureInteractAcrossProfiles(String packageName, int userId) {
        if (canUserAttemptToConfigureInteractAcrossProfiles(packageName, userId) && hasOtherProfileWithPackageInstalled(packageName, userId) && hasRequestedAppOpPermission(AppOpsManager.opToPermission(93), packageName)) {
            return isCrossProfilePackageAllowlisted(packageName);
        }
        return false;
    }

    public boolean canUserAttemptToConfigureInteractAcrossProfiles(String packageName) {
        return canUserAttemptToConfigureInteractAcrossProfiles(packageName, this.mInjector.getCallingUserId());
    }

    private boolean canUserAttemptToConfigureInteractAcrossProfiles(String packageName, int userId) {
        int[] profileIds = this.mInjector.getUserManager().getProfileIds(userId, false);
        if (profileIds.length >= 2 && !isProfileOwner(packageName, profileIds) && hasRequestedAppOpPermission(AppOpsManager.opToPermission(93), packageName)) {
            return !isPlatformSignedAppWithNonUserConfigurablePermission(packageName, profileIds);
        }
        return false;
    }

    private boolean isPlatformSignedAppWithNonUserConfigurablePermission(String packageName, int[] profileIds) {
        return !isCrossProfilePackageAllowlistedByDefault(packageName) && isPlatformSignedAppWithAutomaticProfilesPermission(packageName, profileIds);
    }

    private boolean isPlatformSignedAppWithAutomaticProfilesPermission(String packageName, int[] profileIds) {
        for (int userId : profileIds) {
            int uid = this.mInjector.getPackageManagerInternal().getPackageUid(packageName, 0L, userId);
            if (uid != -1 && isPermissionGranted("android.permission.INTERACT_ACROSS_PROFILES", uid)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasOtherProfileWithPackageInstalled(final String packageName, final int userId) {
        return ((Boolean) this.mInjector.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.pm.CrossProfileAppsServiceImpl$$ExternalSyntheticLambda15
            public final Object getOrThrow() {
                return CrossProfileAppsServiceImpl.this.m5391xeae15779(userId, packageName);
            }
        })).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$hasOtherProfileWithPackageInstalled$10$com-android-server-pm-CrossProfileAppsServiceImpl  reason: not valid java name */
    public /* synthetic */ Boolean m5391xeae15779(int userId, String packageName) throws Exception {
        int[] profileIds = this.mInjector.getUserManager().getProfileIds(userId, false);
        for (int profileId : profileIds) {
            if (profileId != userId && isPackageInstalled(packageName, profileId)) {
                return true;
            }
        }
        return false;
    }

    public void resetInteractAcrossProfilesAppOps(List<String> packageNames) {
        packageNames.forEach(new Consumer() { // from class: com.android.server.pm.CrossProfileAppsServiceImpl$$ExternalSyntheticLambda6
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                CrossProfileAppsServiceImpl.this.resetInteractAcrossProfilesAppOp((String) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetInteractAcrossProfilesAppOp(String packageName) {
        if (canConfigureInteractAcrossProfiles(packageName)) {
            Slog.w(TAG, "Not resetting app-op for package " + packageName + " since it is still configurable by users.");
            return;
        }
        String op = AppOpsManager.permissionToOp("android.permission.INTERACT_ACROSS_PROFILES");
        m5388xfaa3a0a(packageName, AppOpsManager.opToDefaultMode(op));
    }

    public void clearInteractAcrossProfilesAppOps() {
        final int defaultMode = AppOpsManager.opToDefaultMode(AppOpsManager.permissionToOp("android.permission.INTERACT_ACROSS_PROFILES"));
        findAllPackageNames().forEach(new Consumer() { // from class: com.android.server.pm.CrossProfileAppsServiceImpl$$ExternalSyntheticLambda4
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                CrossProfileAppsServiceImpl.this.m5388xfaa3a0a(defaultMode, (String) obj);
            }
        });
    }

    private List<String> findAllPackageNames() {
        return (List) this.mInjector.getPackageManagerInternal().getInstalledApplications(0L, this.mInjector.getCallingUserId(), this.mInjector.getCallingUid()).stream().map(new Function() { // from class: com.android.server.pm.CrossProfileAppsServiceImpl$$ExternalSyntheticLambda7
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                String str;
                str = ((ApplicationInfo) obj).packageName;
                return str;
            }
        }).collect(Collectors.toList());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public CrossProfileAppsInternal getLocalService() {
        return this.mLocalService;
    }

    private boolean isSameProfileGroup(final int callerUserId, final int userId) {
        return ((Boolean) this.mInjector.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.pm.CrossProfileAppsServiceImpl$$ExternalSyntheticLambda5
            public final Object getOrThrow() {
                return CrossProfileAppsServiceImpl.this.m5399xbcac2ca9(callerUserId, userId);
            }
        })).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isSameProfileGroup$13$com-android-server-pm-CrossProfileAppsServiceImpl  reason: not valid java name */
    public /* synthetic */ Boolean m5399xbcac2ca9(int callerUserId, int userId) throws Exception {
        return Boolean.valueOf(this.mInjector.getUserManager().isSameProfileGroup(callerUserId, userId));
    }

    private void verifyCallingPackage(String callingPackage) {
        this.mInjector.getAppOpsManager().checkPackage(this.mInjector.getCallingUid(), callingPackage);
    }

    private boolean isPermissionGranted(String permission, int uid) {
        return this.mInjector.checkComponentPermission(permission, uid, -1, true) == 0;
    }

    private boolean isCallingUserAManagedProfile() {
        return isManagedProfile(this.mInjector.getCallingUserId());
    }

    private boolean isManagedProfile(final int userId) {
        return ((Boolean) this.mInjector.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.pm.CrossProfileAppsServiceImpl$$ExternalSyntheticLambda16
            public final Object getOrThrow() {
                return CrossProfileAppsServiceImpl.this.m5395xa805963c(userId);
            }
        })).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isManagedProfile$14$com-android-server-pm-CrossProfileAppsServiceImpl  reason: not valid java name */
    public /* synthetic */ Boolean m5395xa805963c(int userId) throws Exception {
        return Boolean.valueOf(((UserManager) this.mContext.getSystemService(UserManager.class)).isManagedProfile(userId));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean hasInteractAcrossProfilesPermission(String packageName, int uid, int pid) {
        return isPermissionGranted("android.permission.INTERACT_ACROSS_USERS_FULL", uid) || isPermissionGranted("android.permission.INTERACT_ACROSS_USERS", uid) || PermissionChecker.checkPermissionForPreflight(this.mContext, "android.permission.INTERACT_ACROSS_PROFILES", pid, uid, packageName) == 0;
    }

    private boolean isProfileOwner(String packageName, int[] userIds) {
        for (int userId : userIds) {
            if (isProfileOwner(packageName, userId)) {
                return true;
            }
        }
        return false;
    }

    private boolean isProfileOwner(String packageName, final int userId) {
        ComponentName profileOwner = (ComponentName) this.mInjector.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.pm.CrossProfileAppsServiceImpl$$ExternalSyntheticLambda10
            public final Object getOrThrow() {
                return CrossProfileAppsServiceImpl.this.m5398xf8c59cbd(userId);
            }
        });
        if (profileOwner == null) {
            return false;
        }
        return profileOwner.getPackageName().equals(packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isProfileOwner$15$com-android-server-pm-CrossProfileAppsServiceImpl  reason: not valid java name */
    public /* synthetic */ ComponentName m5398xf8c59cbd(int userId) throws Exception {
        return this.mInjector.getDevicePolicyManagerInternal().getProfileOwnerAsUser(userId);
    }

    /* loaded from: classes2.dex */
    private static class InjectorImpl implements Injector {
        private Context mContext;

        public InjectorImpl(Context context) {
            this.mContext = context;
        }

        @Override // com.android.server.pm.CrossProfileAppsServiceImpl.Injector
        public int getCallingUid() {
            return Binder.getCallingUid();
        }

        @Override // com.android.server.pm.CrossProfileAppsServiceImpl.Injector
        public int getCallingPid() {
            return Binder.getCallingPid();
        }

        @Override // com.android.server.pm.CrossProfileAppsServiceImpl.Injector
        public int getCallingUserId() {
            return UserHandle.getCallingUserId();
        }

        @Override // com.android.server.pm.CrossProfileAppsServiceImpl.Injector
        public UserHandle getCallingUserHandle() {
            return Binder.getCallingUserHandle();
        }

        @Override // com.android.server.pm.CrossProfileAppsServiceImpl.Injector
        public long clearCallingIdentity() {
            return Binder.clearCallingIdentity();
        }

        @Override // com.android.server.pm.CrossProfileAppsServiceImpl.Injector
        public void restoreCallingIdentity(long token) {
            Binder.restoreCallingIdentity(token);
        }

        @Override // com.android.server.pm.CrossProfileAppsServiceImpl.Injector
        public void withCleanCallingIdentity(FunctionalUtils.ThrowingRunnable action) {
            Binder.withCleanCallingIdentity(action);
        }

        @Override // com.android.server.pm.CrossProfileAppsServiceImpl.Injector
        public final <T> T withCleanCallingIdentity(FunctionalUtils.ThrowingSupplier<T> action) {
            return (T) Binder.withCleanCallingIdentity(action);
        }

        @Override // com.android.server.pm.CrossProfileAppsServiceImpl.Injector
        public UserManager getUserManager() {
            return (UserManager) this.mContext.getSystemService(UserManager.class);
        }

        @Override // com.android.server.pm.CrossProfileAppsServiceImpl.Injector
        public PackageManagerInternal getPackageManagerInternal() {
            return (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        }

        @Override // com.android.server.pm.CrossProfileAppsServiceImpl.Injector
        public PackageManager getPackageManager() {
            return this.mContext.getPackageManager();
        }

        @Override // com.android.server.pm.CrossProfileAppsServiceImpl.Injector
        public AppOpsManager getAppOpsManager() {
            return (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        }

        @Override // com.android.server.pm.CrossProfileAppsServiceImpl.Injector
        public ActivityManagerInternal getActivityManagerInternal() {
            return (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        }

        @Override // com.android.server.pm.CrossProfileAppsServiceImpl.Injector
        public ActivityTaskManagerInternal getActivityTaskManagerInternal() {
            return (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
        }

        @Override // com.android.server.pm.CrossProfileAppsServiceImpl.Injector
        public IPackageManager getIPackageManager() {
            return AppGlobals.getPackageManager();
        }

        @Override // com.android.server.pm.CrossProfileAppsServiceImpl.Injector
        public DevicePolicyManagerInternal getDevicePolicyManagerInternal() {
            return (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        }

        @Override // com.android.server.pm.CrossProfileAppsServiceImpl.Injector
        public void sendBroadcastAsUser(Intent intent, UserHandle user) {
            this.mContext.sendBroadcastAsUser(intent, user);
        }

        @Override // com.android.server.pm.CrossProfileAppsServiceImpl.Injector
        public int checkComponentPermission(String permission, int uid, int owningUid, boolean exported) {
            return ActivityManager.checkComponentPermission(permission, uid, owningUid, exported);
        }

        @Override // com.android.server.pm.CrossProfileAppsServiceImpl.Injector
        public void killUid(int uid) {
            PermissionManagerService.killUid(UserHandle.getAppId(uid), UserHandle.getUserId(uid), "permissions revoked");
        }
    }

    /* loaded from: classes2.dex */
    class LocalService extends CrossProfileAppsInternal {
        LocalService() {
        }

        public boolean verifyPackageHasInteractAcrossProfilePermission(String packageName, int userId) throws PackageManager.NameNotFoundException {
            int uid = ((ApplicationInfo) Objects.requireNonNull(CrossProfileAppsServiceImpl.this.mInjector.getPackageManager().getApplicationInfoAsUser((String) Objects.requireNonNull(packageName), 0, userId))).uid;
            return verifyUidHasInteractAcrossProfilePermission(packageName, uid);
        }

        public boolean verifyUidHasInteractAcrossProfilePermission(String packageName, int uid) {
            Objects.requireNonNull(packageName);
            return CrossProfileAppsServiceImpl.this.hasInteractAcrossProfilesPermission(packageName, uid, -1);
        }

        public List<UserHandle> getTargetUserProfiles(String packageName, int userId) {
            return CrossProfileAppsServiceImpl.this.getTargetUserProfilesUnchecked(packageName, userId);
        }

        public void setInteractAcrossProfilesAppOp(String packageName, int newMode, int userId) {
            CrossProfileAppsServiceImpl.this.setInteractAcrossProfilesAppOpUnchecked(packageName, newMode, userId);
        }
    }
}
