package com.android.server.om;

import android.content.Context;
import android.content.om.FabricatedOverlay;
import android.content.om.IOverlayManager;
import android.content.om.OverlayIdentifier;
import android.content.om.OverlayInfo;
import android.content.om.OverlayManagerTransaction;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Binder;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.os.UserHandle;
import android.util.TypedValue;
import com.android.server.vibrator.VibratorManagerService;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/* loaded from: classes2.dex */
final class OverlayManagerShellCommand extends ShellCommand {
    private final Context mContext;
    private final IOverlayManager mInterface;

    /* JADX INFO: Access modifiers changed from: package-private */
    public OverlayManagerShellCommand(Context ctx, IOverlayManager iom) {
        this.mContext = ctx;
        this.mInterface = iom;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public int onCommand(String cmd) {
        char c;
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter err = getErrPrintWriter();
        try {
            switch (cmd.hashCode()) {
                case -1361113425:
                    if (cmd.equals("set-priority")) {
                        c = 4;
                        break;
                    }
                    c = 65535;
                    break;
                case -1298848381:
                    if (cmd.equals("enable")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case -1097094790:
                    if (cmd.equals("lookup")) {
                        c = 5;
                        break;
                    }
                    c = 65535;
                    break;
                case -794624300:
                    if (cmd.equals("enable-exclusive")) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case 3322014:
                    if (cmd.equals("list")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 1671308008:
                    if (cmd.equals("disable")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 2016903117:
                    if (cmd.equals("fabricate")) {
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
                    return runList();
                case 1:
                    return runEnableDisable(true);
                case 2:
                    return runEnableDisable(false);
                case 3:
                    return runEnableExclusive();
                case 4:
                    return runSetPriority();
                case 5:
                    return runLookup();
                case 6:
                    return runFabricate();
                default:
                    return handleDefaultCommands(cmd);
            }
        } catch (RemoteException e) {
            err.println("Remote exception: " + e);
            return -1;
        } catch (IllegalArgumentException e2) {
            err.println("Error: " + e2.getMessage());
            return -1;
        }
    }

    public void onHelp() {
        PrintWriter out = getOutPrintWriter();
        out.println("Overlay manager (overlay) commands:");
        out.println("  help");
        out.println("    Print this help text.");
        out.println("  dump [--verbose] [--user USER_ID] [[FIELD] PACKAGE[:NAME]]");
        out.println("    Print debugging information about the overlay manager.");
        out.println("    With optional parameters PACKAGE and NAME, limit output to the specified");
        out.println("    overlay or target. With optional parameter FIELD, limit output to");
        out.println("    the corresponding SettingsItem field. Field names are all lower case");
        out.println("    and omit the m prefix, i.e. 'userid' for SettingsItem.mUserId.");
        out.println("  list [--user USER_ID] [PACKAGE[:NAME]]");
        out.println("    Print information about target and overlay packages.");
        out.println("    Overlay packages are printed in priority order. With optional");
        out.println("    parameters PACKAGE and NAME, limit output to the specified overlay or");
        out.println("    target.");
        out.println("  enable [--user USER_ID] PACKAGE[:NAME]");
        out.println("    Enable overlay within or owned by PACKAGE with optional unique NAME.");
        out.println("  disable [--user USER_ID] PACKAGE[:NAME]");
        out.println("    Disable overlay within or owned by PACKAGE with optional unique NAME.");
        out.println("  enable-exclusive [--user USER_ID] [--category] PACKAGE");
        out.println("    Enable overlay within or owned by PACKAGE and disable all other overlays");
        out.println("    for its target package. If the --category option is given, only disables");
        out.println("    other overlays in the same category.");
        out.println("  set-priority [--user USER_ID] PACKAGE PARENT|lowest|highest");
        out.println("    Change the priority of the overlay to be just higher than");
        out.println("    the priority of PARENT If PARENT is the special keyword");
        out.println("    'lowest', change priority of PACKAGE to the lowest priority.");
        out.println("    If PARENT is the special keyword 'highest', change priority of");
        out.println("    PACKAGE to the highest priority.");
        out.println("  lookup [--user USER_ID] [--verbose] PACKAGE-TO-LOAD PACKAGE:TYPE/NAME");
        out.println("    Load a package and print the value of a given resource");
        out.println("    applying the current configuration and enabled overlays.");
        out.println("    For a more fine-grained alternative, use 'idmap2 lookup'.");
        out.println("  fabricate [--user USER_ID] [--target-name OVERLAYABLE] --target PACKAGE");
        out.println("            --name NAME PACKAGE:TYPE/NAME ENCODED-TYPE-ID ENCODED-VALUE");
        out.println("    Create an overlay from a single resource. Caller must be root. Example:");
        out.println("      fabricate --target android --name LighterGray \\");
        out.println("                android:color/lighter_gray 0x1c 0xffeeeeee");
    }

    /* JADX WARN: Code restructure failed: missing block: B:9:0x0020, code lost:
        if (r3.equals("--user") != false) goto L8;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int runList() throws RemoteException {
        PrintWriter out = getOutPrintWriter();
        PrintWriter err = getErrPrintWriter();
        int userId = 0;
        while (true) {
            String opt = getNextOption();
            boolean z = false;
            if (opt != null) {
                switch (opt.hashCode()) {
                    case 1333469547:
                        break;
                    default:
                        z = true;
                        break;
                }
                switch (z) {
                    case false:
                        userId = UserHandle.parseUserArg(getNextArgRequired());
                    default:
                        err.println("Error: Unknown option: " + opt);
                        return 1;
                }
            } else {
                String packageName = getNextArg();
                if (packageName != null) {
                    List<OverlayInfo> overlaysForTarget = this.mInterface.getOverlayInfosForTarget(packageName, userId);
                    if (overlaysForTarget.isEmpty()) {
                        OverlayInfo info = this.mInterface.getOverlayInfo(packageName, userId);
                        if (info != null) {
                            printListOverlay(out, info);
                        }
                        return 0;
                    }
                    out.println(packageName);
                    int n = overlaysForTarget.size();
                    for (int i = 0; i < n; i++) {
                        printListOverlay(out, overlaysForTarget.get(i));
                    }
                    return 0;
                }
                Map<String, List<OverlayInfo>> allOverlays = this.mInterface.getAllOverlays(userId);
                for (String targetPackageName : allOverlays.keySet()) {
                    out.println(targetPackageName);
                    List<OverlayInfo> overlaysForTarget2 = allOverlays.get(targetPackageName);
                    int n2 = overlaysForTarget2.size();
                    for (int i2 = 0; i2 < n2; i2++) {
                        printListOverlay(out, overlaysForTarget2.get(i2));
                    }
                    out.println();
                }
                return 0;
            }
        }
    }

    private void printListOverlay(PrintWriter out, OverlayInfo oi) {
        String status;
        switch (oi.state) {
            case 2:
                status = "[ ]";
                break;
            case 3:
            case 6:
                status = "[x]";
                break;
            case 4:
            case 5:
            default:
                status = "---";
                break;
        }
        out.println(String.format("%s %s", status, oi.getOverlayIdentifier()));
    }

    /* JADX WARN: Code restructure failed: missing block: B:9:0x001c, code lost:
        if (r2.equals("--user") != false) goto L8;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int runEnableDisable(boolean enable) throws RemoteException {
        PrintWriter err = getErrPrintWriter();
        int userId = 0;
        while (true) {
            String opt = getNextOption();
            boolean z = false;
            if (opt != null) {
                switch (opt.hashCode()) {
                    case 1333469547:
                        break;
                    default:
                        z = true;
                        break;
                }
                switch (z) {
                    case false:
                        userId = UserHandle.parseUserArg(getNextArgRequired());
                    default:
                        err.println("Error: Unknown option: " + opt);
                        return 1;
                }
            } else {
                OverlayIdentifier overlay = OverlayIdentifier.fromString(getNextArgRequired());
                this.mInterface.commit(new OverlayManagerTransaction.Builder().setEnabled(overlay, enable, userId).build());
                return 0;
            }
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private int runFabricate() throws RemoteException {
        int type;
        int data;
        char c;
        PrintWriter err = getErrPrintWriter();
        if (Binder.getCallingUid() != 0) {
            err.println("Error: must be root to fabricate overlays through the shell");
            return 1;
        }
        String targetPackage = "";
        String targetOverlayable = "";
        String name = "";
        while (true) {
            String opt = getNextOption();
            if (opt != null) {
                switch (opt.hashCode()) {
                    case -935414873:
                        if (opt.equals("--target-name")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1333243947:
                        if (opt.equals("--name")) {
                            c = 3;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1333469547:
                        if (opt.equals("--user")) {
                            c = 0;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1519107889:
                        if (opt.equals("--target")) {
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
                        UserHandle.parseUserArg(getNextArgRequired());
                        break;
                    case 1:
                        targetPackage = getNextArgRequired();
                        break;
                    case 2:
                        targetOverlayable = getNextArgRequired();
                        break;
                    case 3:
                        name = getNextArgRequired();
                        break;
                    default:
                        err.println("Error: Unknown option: " + opt);
                        return 1;
                }
            } else if (name.isEmpty()) {
                err.println("Error: Missing required arg '--name'");
                return 1;
            } else if (targetPackage.isEmpty()) {
                err.println("Error: Missing required arg '--target'");
                return 1;
            } else {
                String resourceName = getNextArgRequired();
                String typeStr = getNextArgRequired();
                if (typeStr.startsWith("0x")) {
                    type = Integer.parseUnsignedInt(typeStr.substring(2), 16);
                } else {
                    type = Integer.parseUnsignedInt(typeStr);
                }
                String dataStr = getNextArgRequired();
                if (dataStr.startsWith("0x")) {
                    data = Integer.parseUnsignedInt(dataStr.substring(2), 16);
                } else {
                    data = Integer.parseUnsignedInt(dataStr);
                }
                PackageManager pm = this.mContext.getPackageManager();
                if (pm == null) {
                    err.println("Error: failed to get package manager");
                    return 1;
                }
                FabricatedOverlay overlay = new FabricatedOverlay.Builder(VibratorManagerService.VibratorManagerShellCommand.SHELL_PACKAGE_NAME, name, targetPackage).setTargetOverlayable(targetOverlayable).setResourceValue(resourceName, type, data).build();
                this.mInterface.commit(new OverlayManagerTransaction.Builder().registerFabricatedOverlay(overlay).build());
                return 0;
            }
        }
    }

    private int runEnableExclusive() throws RemoteException {
        PrintWriter err = getErrPrintWriter();
        int userId = 0;
        boolean inCategory = false;
        while (true) {
            String opt = getNextOption();
            if (opt != null) {
                char c = 65535;
                switch (opt.hashCode()) {
                    case 66265758:
                        if (opt.equals("--category")) {
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
                        userId = UserHandle.parseUserArg(getNextArgRequired());
                        break;
                    case 1:
                        inCategory = true;
                        break;
                    default:
                        err.println("Error: Unknown option: " + opt);
                        return 1;
                }
            } else {
                String overlay = getNextArgRequired();
                return inCategory ? 1 ^ (this.mInterface.setEnabledExclusiveInCategory(overlay, userId) ? 1 : 0) : 1 ^ (this.mInterface.setEnabledExclusive(overlay, true, userId) ? 1 : 0);
            }
        }
    }

    private int runSetPriority() throws RemoteException {
        PrintWriter err = getErrPrintWriter();
        int userId = 0;
        while (true) {
            String opt = getNextOption();
            if (opt != null) {
                char c = 65535;
                switch (opt.hashCode()) {
                    case 1333469547:
                        if (opt.equals("--user")) {
                            c = 0;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                        userId = UserHandle.parseUserArg(getNextArgRequired());
                    default:
                        err.println("Error: Unknown option: " + opt);
                        return 1;
                }
            } else {
                String packageName = getNextArgRequired();
                String newParentPackageName = getNextArgRequired();
                return "highest".equals(newParentPackageName) ? 1 ^ (this.mInterface.setHighestPriority(packageName, userId) ? 1 : 0) : "lowest".equals(newParentPackageName) ? 1 ^ (this.mInterface.setLowestPriority(packageName, userId) ? 1 : 0) : 1 ^ (this.mInterface.setPriority(packageName, newParentPackageName, userId) ? 1 : 0);
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [462=8] */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:12:0x002e, code lost:
        if (r0.equals("--user") != false) goto L9;
     */
    /* JADX WARN: Removed duplicated region for block: B:60:0x011d A[Catch: all -> 0x0171, NotFoundException -> 0x0175, TRY_LEAVE, TryCatch #11 {NotFoundException -> 0x0175, all -> 0x0171, blocks: (B:58:0x0109, B:60:0x011d, B:70:0x0138), top: B:101:0x0109 }] */
    /* JADX WARN: Removed duplicated region for block: B:79:0x0163  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int runLookup() throws RemoteException {
        int resid;
        PrintWriter out = getOutPrintWriter();
        PrintWriter err = getErrPrintWriter();
        boolean verbose = false;
        int userId = 0;
        while (true) {
            String opt = getNextOption();
            boolean z = false;
            if (opt != null) {
                switch (opt.hashCode()) {
                    case 1333469547:
                        break;
                    case 1737088994:
                        if (opt.equals("--verbose")) {
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
                        int userId2 = UserHandle.parseUserArg(getNextArgRequired());
                        userId = userId2;
                        break;
                    case true:
                        verbose = true;
                        break;
                    default:
                        err.println("Error: Unknown option: " + opt);
                        return 1;
                }
            } else {
                String packageToLoad = getNextArgRequired();
                String fullyQualifiedResourceName = getNextArgRequired();
                Pattern regex = Pattern.compile("(.*?):(.*?)/(.*?)");
                Matcher matcher = regex.matcher(fullyQualifiedResourceName);
                if (!matcher.matches()) {
                    err.println("Error: bad resource name, doesn't match package:type/name");
                    return 1;
                }
                try {
                    Resources res = this.mContext.createContextAsUser(UserHandle.of(userId), 0).getPackageManager().getResourcesForApplication(packageToLoad);
                    AssetManager assets = res.getAssets();
                    try {
                        try {
                            assets.setResourceResolutionLoggingEnabled(true);
                            try {
                                TypedValue value = new TypedValue();
                                res.getValue(fullyQualifiedResourceName, value, false);
                                CharSequence valueString = value.coerceToString();
                                String resolution = assets.getLastResourceResolution();
                                res.getValue(fullyQualifiedResourceName, value, true);
                                CharSequence resolvedString = value.coerceToString();
                                if (verbose) {
                                    try {
                                        out.println(resolution);
                                    } catch (Resources.NotFoundException e) {
                                        try {
                                            try {
                                                String pkg = matcher.group(1);
                                                String type = matcher.group(2);
                                                String name = matcher.group(3);
                                                resid = res.getIdentifier(name, type, pkg);
                                                try {
                                                    if (resid != 0) {
                                                    }
                                                } catch (Resources.NotFoundException e2) {
                                                }
                                            } catch (Throwable th) {
                                                e = th;
                                                assets.setResourceResolutionLoggingEnabled(false);
                                                throw e;
                                            }
                                        } catch (Resources.NotFoundException e3) {
                                        } catch (Throwable th2) {
                                            e = th2;
                                            assets.setResourceResolutionLoggingEnabled(false);
                                            throw e;
                                        }
                                    } catch (Throwable th3) {
                                        e = th3;
                                        assets.setResourceResolutionLoggingEnabled(false);
                                        throw e;
                                    }
                                }
                                if (valueString.equals(resolvedString)) {
                                    out.println(valueString);
                                } else {
                                    try {
                                        out.println(((Object) valueString) + " -> " + ((Object) resolvedString));
                                    } catch (Resources.NotFoundException e4) {
                                        String pkg2 = matcher.group(1);
                                        String type2 = matcher.group(2);
                                        String name2 = matcher.group(3);
                                        resid = res.getIdentifier(name2, type2, pkg2);
                                        if (resid != 0) {
                                            throw new Resources.NotFoundException();
                                        }
                                        TypedArray array = res.obtainTypedArray(resid);
                                        if (verbose) {
                                            try {
                                                String pkg3 = assets.getLastResourceResolution();
                                                out.println(pkg3);
                                            } catch (Resources.NotFoundException e5) {
                                                err.println("Error: failed to get the resource " + fullyQualifiedResourceName);
                                                assets.setResourceResolutionLoggingEnabled(false);
                                                return 1;
                                            }
                                        }
                                        TypedValue tv = new TypedValue();
                                        int i = 0;
                                        while (true) {
                                            String type3 = type2;
                                            if (i >= array.length()) {
                                                array.recycle();
                                                assets.setResourceResolutionLoggingEnabled(false);
                                                return 0;
                                            }
                                            array.getValue(i, tv);
                                            out.println(tv.coerceToString());
                                            i++;
                                            type2 = type3;
                                        }
                                    }
                                }
                                assets.setResourceResolutionLoggingEnabled(false);
                                return 0;
                            } catch (Resources.NotFoundException e6) {
                            } catch (Throwable th4) {
                                e = th4;
                            }
                        } catch (Throwable th5) {
                            e = th5;
                        }
                    } catch (Throwable th6) {
                        e = th6;
                    }
                } catch (PackageManager.NameNotFoundException e7) {
                    err.println(String.format("Error: failed to get resources for package %s for user %d", packageToLoad, Integer.valueOf(userId)));
                    return 1;
                }
            }
        }
    }
}
