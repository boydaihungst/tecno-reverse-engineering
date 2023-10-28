package com.android.server.net;

import android.content.Context;
import android.net.NetworkPolicyManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.os.ShellCommand;
import com.android.server.timezonedetector.ServiceConfigAccessor;
import java.io.PrintWriter;
import java.util.List;
/* loaded from: classes2.dex */
class NetworkPolicyManagerShellCommand extends ShellCommand {
    private final NetworkPolicyManagerService mInterface;
    private final WifiManager mWifiManager;

    /* JADX INFO: Access modifiers changed from: package-private */
    public NetworkPolicyManagerShellCommand(Context context, NetworkPolicyManagerService service) {
        this.mInterface = service;
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public int onCommand(String cmd) {
        char c;
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        try {
            switch (cmd.hashCode()) {
                case -1544226370:
                    if (cmd.equals("start-watching")) {
                        c = 5;
                        break;
                    }
                    c = 65535;
                    break;
                case -934610812:
                    if (cmd.equals("remove")) {
                        c = 4;
                        break;
                    }
                    c = 65535;
                    break;
                case 96417:
                    if (cmd.equals("add")) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case 102230:
                    if (cmd.equals("get")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 113762:
                    if (cmd.equals("set")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 3322014:
                    if (cmd.equals("list")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 1093388830:
                    if (cmd.equals("stop-watching")) {
                        c = 6;
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
                    return runGet();
                case 1:
                    return runSet();
                case 2:
                    return runList();
                case 3:
                    return runAdd();
                case 4:
                    return runRemove();
                case 5:
                    return runStartWatching();
                case 6:
                    return runStopWatching();
                default:
                    return handleDefaultCommands(cmd);
            }
        } catch (RemoteException e) {
            pw.println("Remote exception: " + e);
            return -1;
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Network policy manager (netpolicy) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("");
        pw.println("  add restrict-background-whitelist UID");
        pw.println("    Adds a UID to the whitelist for restrict background usage.");
        pw.println("  add restrict-background-blacklist UID");
        pw.println("    Adds a UID to the blacklist for restrict background usage.");
        pw.println("  add app-idle-whitelist UID");
        pw.println("    Adds a UID to the temporary app idle whitelist.");
        pw.println("  get restrict-background");
        pw.println("    Gets the global restrict background usage status.");
        pw.println("  list wifi-networks [true|false]");
        pw.println("    Lists all saved wifi networks and whether they are metered or not.");
        pw.println("    If a boolean argument is passed, filters just the metered (or unmetered)");
        pw.println("    networks.");
        pw.println("  list restrict-background-whitelist");
        pw.println("    Lists UIDs that are whitelisted for restrict background usage.");
        pw.println("  list restrict-background-blacklist");
        pw.println("    Lists UIDs that are blacklisted for restrict background usage.");
        pw.println("  remove restrict-background-whitelist UID");
        pw.println("    Removes a UID from the whitelist for restrict background usage.");
        pw.println("  remove restrict-background-blacklist UID");
        pw.println("    Removes a UID from the blacklist for restrict background usage.");
        pw.println("  remove app-idle-whitelist UID");
        pw.println("    Removes a UID from the temporary app idle whitelist.");
        pw.println("  set metered-network ID [undefined|true|false]");
        pw.println("    Toggles whether the given wi-fi network is metered.");
        pw.println("  set restrict-background BOOLEAN");
        pw.println("    Sets the global restrict background usage status.");
        pw.println("  set sub-plan-owner subId [packageName]");
        pw.println("    Sets the data plan owner package for subId.");
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private int runGet() throws RemoteException {
        boolean z;
        PrintWriter pw = getOutPrintWriter();
        String type = getNextArg();
        if (type != null) {
            switch (type.hashCode()) {
                case -747095841:
                    if (type.equals("restrict-background")) {
                        z = false;
                        break;
                    }
                    z = true;
                    break;
                case 909005781:
                    if (type.equals("restricted-mode")) {
                        z = true;
                        break;
                    }
                    z = true;
                    break;
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    return getRestrictBackground();
                case true:
                    return getRestrictedModeState();
                default:
                    pw.println("Error: unknown get type '" + type + "'");
                    return -1;
            }
        }
        pw.println("Error: didn't specify type of data to get");
        return -1;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private int runSet() throws RemoteException {
        char c;
        PrintWriter pw = getOutPrintWriter();
        String type = getNextArg();
        if (type != null) {
            switch (type.hashCode()) {
                case -983249079:
                    if (type.equals("metered-network")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case -747095841:
                    if (type.equals("restrict-background")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 1846940860:
                    if (type.equals("sub-plan-owner")) {
                        c = 2;
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
                    return setMeteredWifiNetwork();
                case 1:
                    return setRestrictBackground();
                case 2:
                    return setSubPlanOwner();
                default:
                    pw.println("Error: unknown set type '" + type + "'");
                    return -1;
            }
        }
        pw.println("Error: didn't specify type of data to set");
        return -1;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private int runList() throws RemoteException {
        char c;
        PrintWriter pw = getOutPrintWriter();
        String type = getNextArg();
        if (type != null) {
            switch (type.hashCode()) {
                case -1683867974:
                    if (type.equals("app-idle-whitelist")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case -668534353:
                    if (type.equals("restrict-background-blacklist")) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case -363534403:
                    if (type.equals("wifi-networks")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 639570137:
                    if (type.equals("restrict-background-whitelist")) {
                        c = 2;
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
                    return listAppIdleWhitelist();
                case 1:
                    return listWifiNetworks();
                case 2:
                    return listRestrictBackgroundWhitelist();
                case 3:
                    return listRestrictBackgroundBlacklist();
                default:
                    pw.println("Error: unknown list type '" + type + "'");
                    return -1;
            }
        }
        pw.println("Error: didn't specify type of data to list");
        return -1;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private int runAdd() throws RemoteException {
        char c;
        PrintWriter pw = getOutPrintWriter();
        String type = getNextArg();
        if (type != null) {
            switch (type.hashCode()) {
                case -1683867974:
                    if (type.equals("app-idle-whitelist")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case -668534353:
                    if (type.equals("restrict-background-blacklist")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 639570137:
                    if (type.equals("restrict-background-whitelist")) {
                        c = 0;
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
                    return addRestrictBackgroundWhitelist();
                case 1:
                    return addRestrictBackgroundBlacklist();
                case 2:
                    return addAppIdleWhitelist();
                default:
                    pw.println("Error: unknown add type '" + type + "'");
                    return -1;
            }
        }
        pw.println("Error: didn't specify type of data to add");
        return -1;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private int runRemove() throws RemoteException {
        char c;
        PrintWriter pw = getOutPrintWriter();
        String type = getNextArg();
        if (type != null) {
            switch (type.hashCode()) {
                case -1683867974:
                    if (type.equals("app-idle-whitelist")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case -668534353:
                    if (type.equals("restrict-background-blacklist")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 639570137:
                    if (type.equals("restrict-background-whitelist")) {
                        c = 0;
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
                    return removeRestrictBackgroundWhitelist();
                case 1:
                    return removeRestrictBackgroundBlacklist();
                case 2:
                    return removeAppIdleWhitelist();
                default:
                    pw.println("Error: unknown remove type '" + type + "'");
                    return -1;
            }
        }
        pw.println("Error: didn't specify type of data to remove");
        return -1;
    }

    private int runStartWatching() {
        int uid = Integer.parseInt(getNextArgRequired());
        if (uid < 0) {
            PrintWriter pw = getOutPrintWriter();
            pw.print("Invalid UID: ");
            pw.println(uid);
            return -1;
        }
        this.mInterface.setDebugUid(uid);
        return 0;
    }

    private int runStopWatching() {
        this.mInterface.setDebugUid(-1);
        return 0;
    }

    private int listUidPolicies(String msg, int policy) throws RemoteException {
        int[] uids = this.mInterface.getUidsWithPolicy(policy);
        return listUidList(msg, uids);
    }

    private int listUidList(String msg, int[] uids) {
        PrintWriter pw = getOutPrintWriter();
        pw.print(msg);
        pw.print(": ");
        if (uids.length == 0) {
            pw.println("none");
        } else {
            for (int uid : uids) {
                pw.print(uid);
                pw.print(' ');
            }
        }
        pw.println();
        return 0;
    }

    private int listRestrictBackgroundWhitelist() throws RemoteException {
        return listUidPolicies("Restrict background whitelisted UIDs", 4);
    }

    private int listRestrictBackgroundBlacklist() throws RemoteException {
        return listUidPolicies("Restrict background blacklisted UIDs", 1);
    }

    private int listAppIdleWhitelist() throws RemoteException {
        getOutPrintWriter();
        int[] uids = this.mInterface.getAppIdleWhitelist();
        return listUidList("App Idle whitelisted UIDs", uids);
    }

    private int getRestrictedModeState() {
        PrintWriter pw = getOutPrintWriter();
        pw.print("Restricted mode status: ");
        pw.println(this.mInterface.isRestrictedModeEnabled() ? ServiceConfigAccessor.PROVIDER_MODE_ENABLED : ServiceConfigAccessor.PROVIDER_MODE_DISABLED);
        return 0;
    }

    private int getRestrictBackground() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        pw.print("Restrict background status: ");
        pw.println(this.mInterface.getRestrictBackground() ? ServiceConfigAccessor.PROVIDER_MODE_ENABLED : ServiceConfigAccessor.PROVIDER_MODE_DISABLED);
        return 0;
    }

    private int setRestrictBackground() throws RemoteException {
        int enabled = getNextBooleanArg();
        if (enabled < 0) {
            return enabled;
        }
        this.mInterface.setRestrictBackground(enabled > 0);
        return 0;
    }

    private int setSubPlanOwner() throws RemoteException {
        int subId = Integer.parseInt(getNextArgRequired());
        String packageName = getNextArg();
        this.mInterface.setSubscriptionPlansOwner(subId, packageName);
        return 0;
    }

    private int setUidPolicy(int policy) throws RemoteException {
        int uid = getUidFromNextArg();
        if (uid < 0) {
            return uid;
        }
        this.mInterface.setUidPolicy(uid, policy);
        return 0;
    }

    private int resetUidPolicy(String errorMessage, int expectedPolicy) throws RemoteException {
        int uid = getUidFromNextArg();
        if (uid < 0) {
            return uid;
        }
        int actualPolicy = this.mInterface.getUidPolicy(uid);
        if (actualPolicy != expectedPolicy) {
            PrintWriter pw = getOutPrintWriter();
            pw.print("Error: UID ");
            pw.print(uid);
            pw.print(' ');
            pw.println(errorMessage);
            return -1;
        }
        this.mInterface.setUidPolicy(uid, 0);
        return 0;
    }

    private int addRestrictBackgroundWhitelist() throws RemoteException {
        return setUidPolicy(4);
    }

    private int removeRestrictBackgroundWhitelist() throws RemoteException {
        return resetUidPolicy("not whitelisted", 4);
    }

    private int addRestrictBackgroundBlacklist() throws RemoteException {
        return setUidPolicy(1);
    }

    private int removeRestrictBackgroundBlacklist() throws RemoteException {
        return resetUidPolicy("not blacklisted", 1);
    }

    private int setAppIdleWhitelist(boolean isWhitelisted) {
        int uid = getUidFromNextArg();
        if (uid < 0) {
            return uid;
        }
        this.mInterface.setAppIdleWhitelist(uid, isWhitelisted);
        return 0;
    }

    private int addAppIdleWhitelist() throws RemoteException {
        return setAppIdleWhitelist(true);
    }

    private int removeAppIdleWhitelist() throws RemoteException {
        return setAppIdleWhitelist(false);
    }

    private int listWifiNetworks() {
        int match;
        PrintWriter pw = getOutPrintWriter();
        String arg = getNextArg();
        if (arg == null) {
            match = 0;
        } else if (Boolean.parseBoolean(arg)) {
            match = 1;
        } else {
            match = 2;
        }
        List<WifiConfiguration> configs = this.mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration config : configs) {
            if (arg == null || config.meteredOverride == match) {
                pw.print(NetworkPolicyManager.resolveNetworkId(config));
                pw.print(';');
                pw.println(overrideToString(config.meteredOverride));
            }
        }
        return 0;
    }

    private int setMeteredWifiNetwork() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        String networkId = getNextArg();
        if (networkId == null) {
            pw.println("Error: didn't specify networkId");
            return -1;
        }
        String arg = getNextArg();
        if (arg == null) {
            pw.println("Error: didn't specify meteredOverride");
            return -1;
        }
        this.mInterface.setWifiMeteredOverride(NetworkPolicyManager.resolveNetworkId(networkId), stringToOverride(arg));
        return -1;
    }

    private static String overrideToString(int override) {
        switch (override) {
            case 1:
                return "true";
            case 2:
                return "false";
            default:
                return "none";
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private static int stringToOverride(String override) {
        char c;
        switch (override.hashCode()) {
            case 3569038:
                if (override.equals("true")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 97196323:
                if (override.equals("false")) {
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
                return 1;
            case 1:
                return 2;
            default:
                return 0;
        }
    }

    private int getNextBooleanArg() {
        PrintWriter pw = getOutPrintWriter();
        String arg = getNextArg();
        if (arg == null) {
            pw.println("Error: didn't specify BOOLEAN");
            return -1;
        }
        return Boolean.valueOf(arg).booleanValue() ? 1 : 0;
    }

    private int getUidFromNextArg() {
        PrintWriter pw = getOutPrintWriter();
        String arg = getNextArg();
        if (arg == null) {
            pw.println("Error: didn't specify UID");
            return -1;
        }
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            pw.println("Error: UID (" + arg + ") should be a number");
            return -2;
        }
    }
}
