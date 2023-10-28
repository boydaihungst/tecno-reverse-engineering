package com.android.server.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.appwidget.AppWidgetManagerInternal;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArraySet;
import android.util.Slog;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodInfo;
import com.android.internal.util.ArrayUtils;
import com.android.server.inputmethod.InputMethodManagerInternal;
import com.android.settingslib.RestrictedLockUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import libcore.util.EmptyArray;
/* loaded from: classes.dex */
public class AccessibilitySecurityPolicy {
    private static final int KEEP_SOURCE_EVENT_TYPES = 4438463;
    private static final String LOG_TAG = "AccessibilitySecurityPolicy";
    private static final int OWN_PROCESS_ID = Process.myPid();
    private final AccessibilityUserManager mAccessibilityUserManager;
    private AccessibilityWindowManager mAccessibilityWindowManager;
    private final AppOpsManager mAppOpsManager;
    private AppWidgetManagerInternal mAppWidgetService;
    private final Context mContext;
    private final PackageManager mPackageManager;
    private final PolicyWarningUIController mPolicyWarningUIController;
    private final UserManager mUserManager;
    private final ArraySet<ComponentName> mNonA11yCategoryServices = new ArraySet<>();
    private int mCurrentUserId = -10000;
    private boolean mSendNonA11yToolNotificationEnabled = false;

    /* loaded from: classes.dex */
    public interface AccessibilityUserManager {
        int getCurrentUserIdLocked();
    }

    public AccessibilitySecurityPolicy(PolicyWarningUIController policyWarningUIController, Context context, AccessibilityUserManager a11yUserManager) {
        this.mContext = context;
        this.mAccessibilityUserManager = a11yUserManager;
        this.mPackageManager = context.getPackageManager();
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mAppOpsManager = (AppOpsManager) context.getSystemService("appops");
        this.mPolicyWarningUIController = policyWarningUIController;
    }

    public void setSendingNonA11yToolNotificationLocked(boolean enable) {
        if (enable == this.mSendNonA11yToolNotificationEnabled) {
            return;
        }
        this.mSendNonA11yToolNotificationEnabled = enable;
        this.mPolicyWarningUIController.enableSendingNonA11yToolNotification(enable);
        if (enable) {
            for (int i = 0; i < this.mNonA11yCategoryServices.size(); i++) {
                ComponentName service = this.mNonA11yCategoryServices.valueAt(i);
                this.mPolicyWarningUIController.onNonA11yCategoryServiceBound(this.mCurrentUserId, service);
            }
        }
    }

    public void setAccessibilityWindowManager(AccessibilityWindowManager awm) {
        this.mAccessibilityWindowManager = awm;
    }

    public void setAppWidgetManager(AppWidgetManagerInternal appWidgetManager) {
        this.mAppWidgetService = appWidgetManager;
    }

    public boolean canDispatchAccessibilityEventLocked(int userId, AccessibilityEvent event) {
        int eventType = event.getEventType();
        switch (eventType) {
            case 32:
            case 64:
            case 128:
            case 256:
            case 512:
            case 1024:
            case 16384:
            case 262144:
            case 524288:
            case 1048576:
            case 2097152:
            case 4194304:
            case 16777216:
                return true;
            default:
                return isRetrievalAllowingWindowLocked(userId, event.getWindowId());
        }
    }

    public String resolveValidReportedPackageLocked(CharSequence packageName, int appId, int userId, int pid) {
        if (packageName == null) {
            return null;
        }
        if (appId == 1000) {
            return packageName.toString();
        }
        String packageNameStr = packageName.toString();
        int resolvedUid = UserHandle.getUid(userId, appId);
        if (isValidPackageForUid(packageNameStr, resolvedUid)) {
            return packageName.toString();
        }
        AppWidgetManagerInternal appWidgetManagerInternal = this.mAppWidgetService;
        if (appWidgetManagerInternal != null && ArrayUtils.contains(appWidgetManagerInternal.getHostedWidgetPackages(resolvedUid), packageNameStr)) {
            return packageName.toString();
        }
        if (this.mContext.checkPermission("android.permission.ACT_AS_PACKAGE_FOR_ACCESSIBILITY", pid, resolvedUid) == 0) {
            return packageName.toString();
        }
        String[] packageNames = this.mPackageManager.getPackagesForUid(resolvedUid);
        if (ArrayUtils.isEmpty(packageNames)) {
            return null;
        }
        return packageNames[0];
    }

    public String[] computeValidReportedPackages(String targetPackage, int targetUid) {
        ArraySet<String> widgetPackages;
        if (UserHandle.getAppId(targetUid) == 1000) {
            return EmptyArray.STRING;
        }
        String[] uidPackages = {targetPackage};
        AppWidgetManagerInternal appWidgetManagerInternal = this.mAppWidgetService;
        if (appWidgetManagerInternal != null && (widgetPackages = appWidgetManagerInternal.getHostedWidgetPackages(targetUid)) != null && !widgetPackages.isEmpty()) {
            String[] validPackages = new String[uidPackages.length + widgetPackages.size()];
            System.arraycopy(uidPackages, 0, validPackages, 0, uidPackages.length);
            int widgetPackageCount = widgetPackages.size();
            for (int i = 0; i < widgetPackageCount; i++) {
                validPackages[uidPackages.length + i] = widgetPackages.valueAt(i);
            }
            return validPackages;
        }
        return uidPackages;
    }

    public void updateEventSourceLocked(AccessibilityEvent event) {
        if ((event.getEventType() & KEEP_SOURCE_EVENT_TYPES) == 0) {
            event.setSource(null);
        }
    }

    public boolean canGetAccessibilityNodeInfoLocked(int userId, AbstractAccessibilityServiceConnection service, int windowId) {
        return canRetrieveWindowContentLocked(service) && isRetrievalAllowingWindowLocked(userId, windowId);
    }

    public boolean canRetrieveWindowsLocked(AbstractAccessibilityServiceConnection service) {
        return canRetrieveWindowContentLocked(service) && service.mRetrieveInteractiveWindows;
    }

    public boolean canRetrieveWindowContentLocked(AbstractAccessibilityServiceConnection service) {
        return (service.getCapabilities() & 1) != 0;
    }

    public boolean canControlMagnification(AbstractAccessibilityServiceConnection service) {
        return (service.getCapabilities() & 16) != 0;
    }

    public boolean canPerformGestures(AccessibilityServiceConnection service) {
        return (service.getCapabilities() & 32) != 0;
    }

    public boolean canCaptureFingerprintGestures(AccessibilityServiceConnection service) {
        return (service.getCapabilities() & 64) != 0;
    }

    public boolean canTakeScreenshotLocked(AbstractAccessibilityServiceConnection service) {
        return (service.getCapabilities() & 128) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int canEnableDisableInputMethod(String imeId, AbstractAccessibilityServiceConnection service) throws SecurityException {
        String servicePackageName = service.getComponentName().getPackageName();
        int callingUserId = UserHandle.getCallingUserId();
        InputMethodInfo inputMethodInfo = null;
        List<InputMethodInfo> inputMethodInfoList = InputMethodManagerInternal.get().getInputMethodListAsUser(callingUserId);
        if (inputMethodInfoList != null) {
            Iterator<InputMethodInfo> it = inputMethodInfoList.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                InputMethodInfo info = it.next();
                if (info.getId().equals(imeId)) {
                    inputMethodInfo = info;
                    break;
                }
            }
        }
        if (inputMethodInfo == null || !inputMethodInfo.getPackageName().equals(servicePackageName)) {
            throw new SecurityException("The input method is in a different package with the accessibility service");
        }
        if (checkIfInputMethodDisallowed(this.mContext, inputMethodInfo.getPackageName(), callingUserId) != null) {
            return 1;
        }
        return 0;
    }

    private static UserHandle getUserHandleOf(int userId) {
        if (userId == -10000) {
            return null;
        }
        return UserHandle.of(userId);
    }

    private static int getManagedProfileId(Context context, int userId) {
        UserManager um = (UserManager) context.getSystemService(UserManager.class);
        List<UserInfo> userProfiles = um.getProfiles(userId);
        for (UserInfo uInfo : userProfiles) {
            if (uInfo.id != userId && uInfo.isManagedProfile()) {
                return uInfo.id;
            }
        }
        return -10000;
    }

    private static RestrictedLockUtils.EnforcedAdmin checkIfInputMethodDisallowed(Context context, String packageName, int userId) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(DevicePolicyManager.class);
        if (dpm == null) {
            return null;
        }
        RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.getProfileOrDeviceOwner(context, getUserHandleOf(userId));
        boolean permitted = true;
        if (admin != null) {
            permitted = dpm.isInputMethodPermittedByAdmin(admin.component, packageName, userId);
        }
        boolean permittedByParentAdmin = true;
        RestrictedLockUtils.EnforcedAdmin profileAdmin = null;
        int managedProfileId = getManagedProfileId(context, userId);
        if (managedProfileId != -10000 && (profileAdmin = RestrictedLockUtils.getProfileOrDeviceOwner(context, getUserHandleOf(managedProfileId))) != null && dpm.isOrganizationOwnedDeviceWithManagedProfile()) {
            DevicePolicyManager parentDpm = dpm.getParentProfileInstance(UserManager.get(context).getUserInfo(managedProfileId));
            permittedByParentAdmin = parentDpm.isInputMethodPermittedByAdmin(profileAdmin.component, packageName, managedProfileId);
        }
        if (!permitted && !permittedByParentAdmin) {
            return RestrictedLockUtils.EnforcedAdmin.MULTIPLE_ENFORCED_ADMIN;
        }
        if (!permitted) {
            return admin;
        }
        if (permittedByParentAdmin) {
            return null;
        }
        return profileAdmin;
    }

    public int resolveProfileParentLocked(int userId) {
        if (userId != this.mAccessibilityUserManager.getCurrentUserIdLocked()) {
            long identity = Binder.clearCallingIdentity();
            try {
                UserInfo parent = this.mUserManager.getProfileParent(userId);
                if (parent != null) {
                    return parent.getUserHandle().getIdentifier();
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
        return userId;
    }

    public int resolveCallingUserIdEnforcingPermissionsLocked(int userId) {
        int callingUid = Binder.getCallingUid();
        int currentUserId = this.mAccessibilityUserManager.getCurrentUserIdLocked();
        if (callingUid == 0 || callingUid == 1000 || callingUid == 2000) {
            if (userId == -2 || userId == -3) {
                return currentUserId;
            }
            return resolveProfileParentLocked(userId);
        }
        int callingUserId = UserHandle.getUserId(callingUid);
        if (callingUserId == userId) {
            return resolveProfileParentLocked(userId);
        }
        int callingUserParentId = resolveProfileParentLocked(callingUserId);
        if (callingUserParentId == currentUserId && (userId == -2 || userId == -3)) {
            return currentUserId;
        }
        if (!hasPermission("android.permission.INTERACT_ACROSS_USERS") && !hasPermission("android.permission.INTERACT_ACROSS_USERS_FULL")) {
            throw new SecurityException("Call from user " + callingUserId + " as user " + userId + " without permission INTERACT_ACROSS_USERS or INTERACT_ACROSS_USERS_FULL not allowed.");
        }
        if (userId == -2 || userId == -3) {
            return currentUserId;
        }
        return resolveProfileParentLocked(userId);
    }

    public boolean isCallerInteractingAcrossUsers(int userId) {
        int callingUid = Binder.getCallingUid();
        return Binder.getCallingPid() == Process.myPid() || callingUid == 2000 || userId == -2 || userId == -3;
    }

    private boolean isValidPackageForUid(String packageName, int uid) {
        long token = Binder.clearCallingIdentity();
        try {
            return uid == this.mPackageManager.getPackageUidAsUser(packageName, 4194304, UserHandle.getUserId(uid));
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private boolean isRetrievalAllowingWindowLocked(int userId, int windowId) {
        if (Binder.getCallingUid() == 1000) {
            return true;
        }
        if (Binder.getCallingUid() != 2000 || isShellAllowedToRetrieveWindowLocked(userId, windowId)) {
            return this.mAccessibilityWindowManager.resolveParentWindowIdLocked(windowId) == this.mAccessibilityWindowManager.getActiveWindowId(userId) || this.mAccessibilityWindowManager.findA11yWindowInfoByIdLocked(windowId) != null;
        }
        return false;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [620=4] */
    private boolean isShellAllowedToRetrieveWindowLocked(int userId, int windowId) {
        long token = Binder.clearCallingIdentity();
        try {
            IBinder windowToken = this.mAccessibilityWindowManager.getWindowTokenForUserAndWindowIdLocked(userId, windowId);
            if (windowToken == null) {
                return false;
            }
            int windowOwnerUserId = this.mAccessibilityWindowManager.getWindowOwnerUserId(windowToken);
            if (windowOwnerUserId == -10000) {
                return false;
            }
            return !this.mUserManager.hasUserRestriction("no_debugging_features", UserHandle.of(windowOwnerUserId));
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void enforceCallingPermission(String permission, String function) {
        if (OWN_PROCESS_ID != Binder.getCallingPid() && !hasPermission(permission)) {
            throw new SecurityException("You do not have " + permission + " required to call " + function + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
        }
    }

    public boolean hasPermission(String permission) {
        return this.mContext.checkCallingPermission(permission) == 0;
    }

    public boolean canRegisterService(ServiceInfo serviceInfo) {
        if (!"android.permission.BIND_ACCESSIBILITY_SERVICE".equals(serviceInfo.permission)) {
            Slog.w(LOG_TAG, "Skipping accessibility service " + new ComponentName(serviceInfo.packageName, serviceInfo.name).flattenToShortString() + ": it does not require the permission android.permission.BIND_ACCESSIBILITY_SERVICE");
            return false;
        } else if ((serviceInfo.flags & 4) != 0) {
            Slog.w(LOG_TAG, "Skipping accessibility service " + new ComponentName(serviceInfo.packageName, serviceInfo.name).flattenToShortString() + ": the service is the external one and doesn't allow to register as an accessibility service ");
            return false;
        } else {
            int servicePackageUid = serviceInfo.applicationInfo.uid;
            if (this.mAppOpsManager.noteOpNoThrow("android:bind_accessibility_service", servicePackageUid, serviceInfo.packageName, null, null) != 0) {
                Slog.w(LOG_TAG, "Skipping accessibility service " + new ComponentName(serviceInfo.packageName, serviceInfo.name).flattenToShortString() + ": disallowed by AppOps");
                return false;
            }
            return true;
        }
    }

    public boolean checkAccessibilityAccess(AbstractAccessibilityServiceConnection service) {
        String packageName = service.getComponentName().getPackageName();
        ResolveInfo resolveInfo = service.getServiceInfo().getResolveInfo();
        if (resolveInfo == null) {
            return true;
        }
        int servicePackageUid = resolveInfo.serviceInfo.applicationInfo.uid;
        int callingPid = Binder.getCallingPid();
        long identityToken = Binder.clearCallingIdentity();
        String attributionTag = service.getAttributionTag();
        try {
            if (OWN_PROCESS_ID == callingPid) {
                return this.mAppOpsManager.noteOpNoThrow("android:access_accessibility", servicePackageUid, packageName, attributionTag, null) == 0;
            }
            return this.mAppOpsManager.noteOp("android:access_accessibility", servicePackageUid, packageName, attributionTag, null) == 0;
        } finally {
            Binder.restoreCallingIdentity(identityToken);
        }
    }

    public void enforceCallingOrSelfPermission(String permission) {
        if (this.mContext.checkCallingOrSelfPermission(permission) != 0) {
            throw new SecurityException("Caller does not hold permission " + permission);
        }
    }

    public void onBoundServicesChangedLocked(int userId, ArrayList<AccessibilityServiceConnection> boundServices) {
        if (this.mAccessibilityUserManager.getCurrentUserIdLocked() != userId) {
            return;
        }
        ArraySet<ComponentName> tempNonA11yCategoryServices = new ArraySet<>();
        for (int i = 0; i < boundServices.size(); i++) {
            AccessibilityServiceInfo a11yServiceInfo = boundServices.get(i).getServiceInfo();
            ComponentName service = a11yServiceInfo.getComponentName().clone();
            if (!a11yServiceInfo.isAccessibilityTool()) {
                tempNonA11yCategoryServices.add(service);
                if (this.mNonA11yCategoryServices.contains(service)) {
                    this.mNonA11yCategoryServices.remove(service);
                } else if (this.mSendNonA11yToolNotificationEnabled) {
                    this.mPolicyWarningUIController.onNonA11yCategoryServiceBound(userId, service);
                }
            }
        }
        for (int i2 = 0; i2 < this.mNonA11yCategoryServices.size(); i2++) {
            this.mPolicyWarningUIController.onNonA11yCategoryServiceUnbound(userId, this.mNonA11yCategoryServices.valueAt(i2));
        }
        this.mNonA11yCategoryServices.clear();
        this.mNonA11yCategoryServices.addAll((ArraySet<? extends ComponentName>) tempNonA11yCategoryServices);
    }

    public void onSwitchUserLocked(int userId, Set<ComponentName> enabledServices) {
        if (this.mCurrentUserId == userId) {
            return;
        }
        this.mPolicyWarningUIController.onSwitchUser(userId, new ArraySet(enabledServices));
        for (int i = 0; i < this.mNonA11yCategoryServices.size(); i++) {
            this.mPolicyWarningUIController.onNonA11yCategoryServiceUnbound(this.mCurrentUserId, this.mNonA11yCategoryServices.valueAt(i));
        }
        this.mNonA11yCategoryServices.clear();
        this.mCurrentUserId = userId;
    }

    public void onEnabledServicesChangedLocked(int userId, Set<ComponentName> enabledServices) {
        if (this.mAccessibilityUserManager.getCurrentUserIdLocked() != userId) {
            return;
        }
        this.mPolicyWarningUIController.onEnabledServicesChanged(userId, new ArraySet(enabledServices));
    }
}
