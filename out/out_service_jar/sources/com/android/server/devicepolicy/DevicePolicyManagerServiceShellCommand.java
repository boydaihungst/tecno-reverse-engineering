package com.android.server.devicepolicy;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.UserHandle;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
/* loaded from: classes.dex */
final class DevicePolicyManagerServiceShellCommand extends ShellCommand {
    private static final String CMD_CLEAR_FREEZE_PERIOD_RECORD = "clear-freeze-period-record";
    private static final String CMD_FORCE_NETWORK_LOGS = "force-network-logs";
    private static final String CMD_FORCE_SECURITY_LOGS = "force-security-logs";
    private static final String CMD_IS_SAFE_OPERATION = "is-operation-safe";
    private static final String CMD_IS_SAFE_OPERATION_BY_REASON = "is-operation-safe-by-reason";
    private static final String CMD_LIST_OWNERS = "list-owners";
    private static final String CMD_LIST_POLICY_EXEMPT_APPS = "list-policy-exempt-apps";
    private static final String CMD_MARK_PO_ON_ORG_OWNED_DEVICE = "mark-profile-owner-on-organization-owned-device";
    private static final String CMD_REMOVE_ACTIVE_ADMIN = "remove-active-admin";
    private static final String CMD_SET_ACTIVE_ADMIN = "set-active-admin";
    private static final String CMD_SET_DEVICE_OWNER = "set-device-owner";
    private static final String CMD_SET_PROFILE_OWNER = "set-profile-owner";
    private static final String CMD_SET_SAFE_OPERATION = "set-operation-safe";
    private static final String DO_ONLY_OPTION = "--device-owner-only";
    private static final String NAME_OPTION = "--name";
    private static final String USER_OPTION = "--user";
    private ComponentName mComponent;
    private final DevicePolicyManagerService mService;
    private boolean mSetDoOnly;
    private int mUserId = 0;
    private String mName = "";

    /* JADX INFO: Access modifiers changed from: package-private */
    public DevicePolicyManagerServiceShellCommand(DevicePolicyManagerService service) {
        this.mService = (DevicePolicyManagerService) Objects.requireNonNull(service);
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        try {
            pw.printf("DevicePolicyManager Service (device_policy) commands:\n\n", new Object[0]);
            showHelp(pw);
            if (pw != null) {
                pw.close();
            }
        } catch (Throwable th) {
            if (pw != null) {
                try {
                    pw.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [105=14] */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        char c = 65535;
        try {
            switch (cmd.hashCode()) {
                case -2077120112:
                    if (cmd.equals(CMD_FORCE_NETWORK_LOGS)) {
                        c = '\n';
                        break;
                    }
                    break;
                case -1819296492:
                    if (cmd.equals(CMD_LIST_POLICY_EXEMPT_APPS)) {
                        c = 4;
                        break;
                    }
                    break;
                case -1791908857:
                    if (cmd.equals(CMD_SET_DEVICE_OWNER)) {
                        c = 6;
                        break;
                    }
                    break;
                case -1073491921:
                    if (cmd.equals(CMD_LIST_OWNERS)) {
                        c = 3;
                        break;
                    }
                    break;
                case -905136898:
                    if (cmd.equals(CMD_SET_SAFE_OPERATION)) {
                        c = 2;
                        break;
                    }
                    break;
                case -898547037:
                    if (cmd.equals(CMD_IS_SAFE_OPERATION_BY_REASON)) {
                        c = 1;
                        break;
                    }
                    break;
                case -776610703:
                    if (cmd.equals(CMD_REMOVE_ACTIVE_ADMIN)) {
                        c = '\b';
                        break;
                    }
                    break;
                case -577127626:
                    if (cmd.equals(CMD_IS_SAFE_OPERATION)) {
                        c = 0;
                        break;
                    }
                    break;
                case -536624985:
                    if (cmd.equals(CMD_CLEAR_FREEZE_PERIOD_RECORD)) {
                        c = '\t';
                        break;
                    }
                    break;
                case 547934547:
                    if (cmd.equals(CMD_SET_ACTIVE_ADMIN)) {
                        c = 5;
                        break;
                    }
                    break;
                case 639813476:
                    if (cmd.equals(CMD_SET_PROFILE_OWNER)) {
                        c = 7;
                        break;
                    }
                    break;
                case 1325530298:
                    if (cmd.equals(CMD_FORCE_SECURITY_LOGS)) {
                        c = 11;
                        break;
                    }
                    break;
                case 1509758184:
                    if (cmd.equals(CMD_MARK_PO_ON_ORG_OWNED_DEVICE)) {
                        c = '\f';
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    int runIsSafeOperation = runIsSafeOperation(pw);
                    if (pw != null) {
                        pw.close();
                    }
                    return runIsSafeOperation;
                case 1:
                    int runIsSafeOperationByReason = runIsSafeOperationByReason(pw);
                    if (pw != null) {
                        pw.close();
                    }
                    return runIsSafeOperationByReason;
                case 2:
                    int runSetSafeOperation = runSetSafeOperation(pw);
                    if (pw != null) {
                        pw.close();
                    }
                    return runSetSafeOperation;
                case 3:
                    int runListOwners = runListOwners(pw);
                    if (pw != null) {
                        pw.close();
                    }
                    return runListOwners;
                case 4:
                    int runListPolicyExemptApps = runListPolicyExemptApps(pw);
                    if (pw != null) {
                        pw.close();
                    }
                    return runListPolicyExemptApps;
                case 5:
                    int runSetActiveAdmin = runSetActiveAdmin(pw);
                    if (pw != null) {
                        pw.close();
                    }
                    return runSetActiveAdmin;
                case 6:
                    int runSetDeviceOwner = runSetDeviceOwner(pw);
                    if (pw != null) {
                        pw.close();
                    }
                    return runSetDeviceOwner;
                case 7:
                    int runSetProfileOwner = runSetProfileOwner(pw);
                    if (pw != null) {
                        pw.close();
                    }
                    return runSetProfileOwner;
                case '\b':
                    int runRemoveActiveAdmin = runRemoveActiveAdmin(pw);
                    if (pw != null) {
                        pw.close();
                    }
                    return runRemoveActiveAdmin;
                case '\t':
                    int runClearFreezePeriodRecord = runClearFreezePeriodRecord(pw);
                    if (pw != null) {
                        pw.close();
                    }
                    return runClearFreezePeriodRecord;
                case '\n':
                    int runForceNetworkLogs = runForceNetworkLogs(pw);
                    if (pw != null) {
                        pw.close();
                    }
                    return runForceNetworkLogs;
                case 11:
                    int runForceSecurityLogs = runForceSecurityLogs(pw);
                    if (pw != null) {
                        pw.close();
                    }
                    return runForceSecurityLogs;
                case '\f':
                    int runMarkProfileOwnerOnOrganizationOwnedDevice = runMarkProfileOwnerOnOrganizationOwnedDevice(pw);
                    if (pw != null) {
                        pw.close();
                    }
                    return runMarkProfileOwnerOnOrganizationOwnedDevice;
                default:
                    int onInvalidCommand = onInvalidCommand(pw, cmd);
                    if (pw != null) {
                        pw.close();
                    }
                    return onInvalidCommand;
            }
        } catch (Throwable th) {
            if (pw != null) {
                try {
                    pw.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private int onInvalidCommand(PrintWriter pw, String cmd) {
        if (super.handleDefaultCommands(cmd) == 0) {
            return 0;
        }
        pw.printf("Usage: \n", new Object[0]);
        showHelp(pw);
        return -1;
    }

    private void showHelp(PrintWriter pw) {
        pw.printf("  help\n", new Object[0]);
        pw.printf("    Prints this help text.\n\n", new Object[0]);
        pw.printf("  %s <OPERATION_ID>\n", CMD_IS_SAFE_OPERATION);
        pw.printf("    Checks if the give operation is safe \n\n", new Object[0]);
        pw.printf("  %s <REASON_ID>\n", CMD_IS_SAFE_OPERATION_BY_REASON);
        pw.printf("    Checks if the operations are safe for the given reason\n\n", new Object[0]);
        pw.printf("  %s <OPERATION_ID> <REASON_ID>\n", CMD_SET_SAFE_OPERATION);
        pw.printf("    Emulates the result of the next call to check if the given operation is safe \n\n", new Object[0]);
        pw.printf("  %s\n", CMD_LIST_OWNERS);
        pw.printf("    Lists the device / profile owners per user \n\n", new Object[0]);
        pw.printf("  %s\n", CMD_LIST_POLICY_EXEMPT_APPS);
        pw.printf("    Lists the apps that are exempt from policies\n\n", new Object[0]);
        pw.printf("  %s [ %s <USER_ID> | current ] <COMPONENT>\n", CMD_SET_ACTIVE_ADMIN, USER_OPTION);
        pw.printf("    Sets the given component as active admin for an existing user.\n\n", new Object[0]);
        pw.printf("  %s [ %s <USER_ID> | current *EXPERIMENTAL* ] [ %s <NAME> ] [ %s ]<COMPONENT>\n", CMD_SET_DEVICE_OWNER, USER_OPTION, NAME_OPTION, DO_ONLY_OPTION);
        pw.printf("    Sets the given component as active admin, and its package as device owner.\n\n", new Object[0]);
        pw.printf("  %s [ %s <USER_ID> | current ] [ %s <NAME> ] <COMPONENT>\n", CMD_SET_PROFILE_OWNER, USER_OPTION, NAME_OPTION);
        pw.printf("    Sets the given component as active admin and profile owner for an existing user.\n\n", new Object[0]);
        pw.printf("  %s [ %s <USER_ID> | current ] [ %s <NAME> ] <COMPONENT>\n", CMD_REMOVE_ACTIVE_ADMIN, USER_OPTION, NAME_OPTION);
        pw.printf("    Disables an active admin, the admin must have declared android:testOnly in the application in its manifest. This will also remove device and profile owners.\n\n", new Object[0]);
        pw.printf("  %s\n", CMD_CLEAR_FREEZE_PERIOD_RECORD);
        pw.printf("    Clears framework-maintained record of past freeze periods that the device went through. For use during feature development to prevent triggering restriction on setting freeze periods.\n\n", new Object[0]);
        pw.printf("  %s\n", CMD_FORCE_NETWORK_LOGS);
        pw.printf("    Makes all network logs available to the DPC and triggers DeviceAdminReceiver.onNetworkLogsAvailable() if needed.\n\n", new Object[0]);
        pw.printf("  %s\n", CMD_FORCE_SECURITY_LOGS);
        pw.printf("    Makes all security logs available to the DPC and triggers DeviceAdminReceiver.onSecurityLogsAvailable() if needed.\n\n", new Object[0]);
        pw.printf("  %s [ %s <USER_ID> | current ] <COMPONENT>\n", CMD_MARK_PO_ON_ORG_OWNED_DEVICE, USER_OPTION);
        pw.printf("    Marks the profile owner of the given user as managing an organization-owneddevice. That will give it access to device identifiers (such as serial number, IMEI and MEID), as well as other privileges.\n\n", new Object[0]);
    }

    private int runIsSafeOperation(PrintWriter pw) {
        int operation = Integer.parseInt(getNextArgRequired());
        int reason = this.mService.getUnsafeOperationReason(operation);
        boolean safe = reason == -1;
        pw.printf("Operation %s is %s. Reason: %s\n", DevicePolicyManager.operationToString(operation), safeToString(safe), DevicePolicyManager.operationSafetyReasonToString(reason));
        return 0;
    }

    private int runIsSafeOperationByReason(PrintWriter pw) {
        int reason = Integer.parseInt(getNextArgRequired());
        boolean safe = this.mService.isSafeOperation(reason);
        pw.printf("Operations affected by %s are %s\n", DevicePolicyManager.operationSafetyReasonToString(reason), safeToString(safe));
        return 0;
    }

    private static String safeToString(boolean safe) {
        return safe ? "SAFE" : "UNSAFE";
    }

    private int runSetSafeOperation(PrintWriter pw) {
        int operation = Integer.parseInt(getNextArgRequired());
        int reason = Integer.parseInt(getNextArgRequired());
        this.mService.setNextOperationSafety(operation, reason);
        pw.printf("Next call to check operation %s will return %s\n", DevicePolicyManager.operationToString(operation), DevicePolicyManager.operationSafetyReasonToString(reason));
        return 0;
    }

    private int printAndGetSize(PrintWriter pw, Collection<?> collection, String nameOnSingular) {
        if (collection.isEmpty()) {
            pw.printf("no %ss\n", nameOnSingular);
            return 0;
        }
        int size = collection.size();
        Object[] objArr = new Object[3];
        objArr[0] = Integer.valueOf(size);
        objArr[1] = nameOnSingular;
        objArr[2] = size == 1 ? "" : "s";
        pw.printf("%d %s%s:\n", objArr);
        return size;
    }

    private int runListOwners(PrintWriter pw) {
        List<OwnerShellData> owners = this.mService.listAllOwners();
        int size = printAndGetSize(pw, owners, "owner");
        if (size == 0) {
            return 0;
        }
        for (int i = 0; i < size; i++) {
            OwnerShellData owner = owners.get(i);
            pw.printf("User %2d: admin=%s", Integer.valueOf(owner.userId), owner.admin.flattenToShortString());
            if (owner.isDeviceOwner) {
                pw.print(",DeviceOwner");
            }
            if (owner.isProfileOwner) {
                pw.print(",ProfileOwner");
            }
            if (owner.isManagedProfileOwner) {
                pw.printf(",ManagedProfileOwner(parentUserId=%d)", Integer.valueOf(owner.parentUserId));
            }
            if (owner.isAffiliated) {
                pw.print(",Affiliated");
            }
            pw.println();
        }
        return 0;
    }

    private int runListPolicyExemptApps(PrintWriter pw) {
        List<String> apps = this.mService.listPolicyExemptApps();
        int size = printAndGetSize(pw, apps, "policy exempt app");
        if (size == 0) {
            return 0;
        }
        for (int i = 0; i < size; i++) {
            String app = apps.get(i);
            pw.printf("  %d: %s\n", Integer.valueOf(i), app);
        }
        return 0;
    }

    private int runSetActiveAdmin(PrintWriter pw) {
        parseArgs(false);
        this.mService.setActiveAdmin(this.mComponent, true, this.mUserId);
        pw.printf("Success: Active admin set to component %s\n", this.mComponent.flattenToShortString());
        return 0;
    }

    private int runSetDeviceOwner(PrintWriter pw) {
        boolean z;
        parseArgs(true);
        this.mService.setActiveAdmin(this.mComponent, true, this.mUserId);
        try {
            DevicePolicyManagerService devicePolicyManagerService = this.mService;
            ComponentName componentName = this.mComponent;
            String str = this.mName;
            int i = this.mUserId;
            if (!this.mSetDoOnly) {
                z = true;
            } else {
                z = false;
            }
            if (!devicePolicyManagerService.setDeviceOwner(componentName, str, i, z)) {
                throw new RuntimeException("Can't set package " + this.mComponent + " as device owner.");
            }
            this.mService.setUserProvisioningState(3, this.mUserId);
            pw.printf("Success: Device owner set to package %s\n", this.mComponent.flattenToShortString());
            pw.printf("Active admin set to component %s\n", this.mComponent.flattenToShortString());
            return 0;
        } catch (Exception e) {
            this.mService.removeActiveAdmin(this.mComponent, 0);
            throw e;
        }
    }

    private int runRemoveActiveAdmin(PrintWriter pw) {
        parseArgs(false);
        this.mService.forceRemoveActiveAdmin(this.mComponent, this.mUserId);
        pw.printf("Success: Admin removed %s\n", this.mComponent);
        return 0;
    }

    private int runSetProfileOwner(PrintWriter pw) {
        parseArgs(true);
        this.mService.setActiveAdmin(this.mComponent, true, this.mUserId);
        try {
            if (!this.mService.setProfileOwner(this.mComponent, this.mName, this.mUserId)) {
                throw new RuntimeException("Can't set component " + this.mComponent.flattenToShortString() + " as profile owner for user " + this.mUserId);
            }
            this.mService.setUserProvisioningState(3, this.mUserId);
            pw.printf("Success: Active admin and profile owner set to %s for user %d\n", this.mComponent.flattenToShortString(), Integer.valueOf(this.mUserId));
            return 0;
        } catch (Exception e) {
            this.mService.removeActiveAdmin(this.mComponent, this.mUserId);
            throw e;
        }
    }

    private int runClearFreezePeriodRecord(PrintWriter pw) {
        this.mService.clearSystemUpdatePolicyFreezePeriodRecord();
        pw.printf("Success\n", new Object[0]);
        return 0;
    }

    private int runForceNetworkLogs(PrintWriter pw) {
        while (true) {
            long toWait = this.mService.forceNetworkLogs();
            if (toWait != 0) {
                pw.printf("We have to wait for %d milliseconds...\n", Long.valueOf(toWait));
                SystemClock.sleep(toWait);
            } else {
                pw.printf("Success\n", new Object[0]);
                return 0;
            }
        }
    }

    private int runForceSecurityLogs(PrintWriter pw) {
        while (true) {
            long toWait = this.mService.forceSecurityLogs();
            if (toWait != 0) {
                pw.printf("We have to wait for %d milliseconds...\n", Long.valueOf(toWait));
                SystemClock.sleep(toWait);
            } else {
                pw.printf("Success\n", new Object[0]);
                return 0;
            }
        }
    }

    private int runMarkProfileOwnerOnOrganizationOwnedDevice(PrintWriter pw) {
        parseArgs(false);
        this.mService.setProfileOwnerOnOrganizationOwnedDevice(this.mComponent, this.mUserId, true);
        pw.printf("Success\n", new Object[0]);
        return 0;
    }

    private void parseArgs(boolean canHaveName) {
        String opt;
        while (true) {
            opt = getNextOption();
            if (opt != null) {
                if (USER_OPTION.equals(opt)) {
                    String arg = getNextArgRequired();
                    int parseUserArg = UserHandle.parseUserArg(arg);
                    this.mUserId = parseUserArg;
                    if (parseUserArg == -2) {
                        this.mUserId = ActivityManager.getCurrentUser();
                    }
                } else if (DO_ONLY_OPTION.equals(opt)) {
                    this.mSetDoOnly = true;
                } else if (!canHaveName || !NAME_OPTION.equals(opt)) {
                    break;
                } else {
                    this.mName = getNextArgRequired();
                }
            } else {
                this.mComponent = parseComponentName(getNextArgRequired());
                return;
            }
        }
        throw new IllegalArgumentException("Unknown option: " + opt);
    }

    private ComponentName parseComponentName(String component) {
        ComponentName cn = ComponentName.unflattenFromString(component);
        if (cn == null) {
            throw new IllegalArgumentException("Invalid component " + component);
        }
        return cn;
    }
}
