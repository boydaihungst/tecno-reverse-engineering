package com.android.server.slice;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.ShellCommand;
import android.util.ArraySet;
import com.android.server.wm.ActivityTaskManagerInternal;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
/* loaded from: classes2.dex */
public class SliceShellCommand extends ShellCommand {
    private final SliceManagerService mService;

    public SliceShellCommand(SliceManagerService service) {
        this.mService = service;
    }

    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        char c = 65535;
        switch (cmd.hashCode()) {
            case -185318259:
                if (cmd.equals("get-permissions")) {
                    c = 0;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return runGetPermissions(getNextArgRequired());
            default:
                return 0;
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Status bar commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("");
        pw.println("  get-permissions <authority>");
        pw.println("    List the pkgs that have permission to an authority.");
        pw.println("");
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [106=4] */
    private int runGetPermissions(String authority) {
        String[] allPackagesGranted;
        if (Binder.getCallingUid() != 2000 && Binder.getCallingUid() != 0) {
            getOutPrintWriter().println("Only shell can get permissions");
            return -1;
        }
        Context context = this.mService.getContext();
        long ident = Binder.clearCallingIdentity();
        try {
            Uri uri = new Uri.Builder().scheme(ActivityTaskManagerInternal.ASSIST_KEY_CONTENT).authority(authority).build();
            if (!"vnd.android.slice".equals(context.getContentResolver().getType(uri))) {
                getOutPrintWriter().println(authority + " is not a slice provider");
                return -1;
            }
            Bundle b = context.getContentResolver().call(uri, "get_permissions", (String) null, (Bundle) null);
            if (b == null) {
                getOutPrintWriter().println("An error occurred getting permissions");
                return -1;
            }
            String[] permissions = b.getStringArray("result");
            PrintWriter pw = getOutPrintWriter();
            Set<String> listedPackages = new ArraySet<>();
            if (permissions != null && permissions.length != 0) {
                List<PackageInfo> apps = context.getPackageManager().getPackagesHoldingPermissions(permissions, 0);
                for (PackageInfo app : apps) {
                    pw.println(app.packageName);
                    listedPackages.add(app.packageName);
                }
            }
            for (String pkg : this.mService.getAllPackagesGranted(authority)) {
                if (!listedPackages.contains(pkg)) {
                    pw.println(pkg);
                    listedPackages.add(pkg);
                }
            }
            return 0;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }
}
