package com.android.server.print;

import android.os.RemoteException;
import android.os.ShellCommand;
import android.os.UserHandle;
import android.print.IPrintManager;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
final class PrintShellCommand extends ShellCommand {
    final IPrintManager mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PrintShellCommand(IPrintManager service) {
        this.mService = service;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public int onCommand(String cmd) {
        boolean z;
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        switch (cmd.hashCode()) {
            case -859068373:
                if (cmd.equals("get-bind-instant-service-allowed")) {
                    z = false;
                    break;
                }
                z = true;
                break;
            case 789489311:
                if (cmd.equals("set-bind-instant-service-allowed")) {
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
                return runGetBindInstantServiceAllowed();
            case true:
                return runSetBindInstantServiceAllowed();
            default:
                return -1;
        }
    }

    private int runGetBindInstantServiceAllowed() {
        Integer userId = parseUserId();
        if (userId == null) {
            return -1;
        }
        try {
            getOutPrintWriter().println(Boolean.toString(this.mService.getBindInstantServiceAllowed(userId.intValue())));
            return 0;
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return 0;
        }
    }

    private int runSetBindInstantServiceAllowed() {
        Integer userId = parseUserId();
        if (userId == null) {
            return -1;
        }
        String allowed = getNextArgRequired();
        if (allowed == null) {
            getErrPrintWriter().println("Error: no true/false specified");
            return -1;
        }
        try {
            this.mService.setBindInstantServiceAllowed(userId.intValue(), Boolean.parseBoolean(allowed));
            return 0;
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return 0;
        }
    }

    private Integer parseUserId() {
        String option = getNextOption();
        if (option != null) {
            if (option.equals("--user")) {
                return Integer.valueOf(UserHandle.parseUserArg(getNextArgRequired()));
            }
            getErrPrintWriter().println("Unknown option: " + option);
            return null;
        }
        return 0;
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Print service commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("  set-bind-instant-service-allowed [--user <USER_ID>] true|false ");
        pw.println("    Set whether binding to print services provided by instant apps is allowed.");
        pw.println("  get-bind-instant-service-allowed [--user <USER_ID>]");
        pw.println("    Get whether binding to print services provided by instant apps is allowed.");
    }
}
