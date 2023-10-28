package com.android.server.apphibernation;

import android.os.ShellCommand;
import android.os.UserHandle;
import java.io.PrintWriter;
/* loaded from: classes.dex */
final class AppHibernationShellCommand extends ShellCommand {
    private static final int ERROR = -1;
    private static final String GLOBAL_OPT = "--global";
    private static final int SUCCESS = 0;
    private static final String USER_OPT = "--user";
    private final AppHibernationService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppHibernationShellCommand(AppHibernationService service) {
        this.mService = service;
    }

    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        char c = 65535;
        switch (cmd.hashCode()) {
            case -499367066:
                if (cmd.equals("set-state")) {
                    c = 0;
                    break;
                }
                break;
            case -284749990:
                if (cmd.equals("get-state")) {
                    c = 1;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return runSetState();
            case 1:
                return runGetState();
            default:
                return handleDefaultCommands(cmd);
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:9:0x0019, code lost:
        if (r2.equals(com.android.server.apphibernation.AppHibernationShellCommand.USER_OPT) != false) goto L8;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int runSetState() {
        boolean setsGlobal = false;
        int userId = -2;
        while (true) {
            String opt = getNextOption();
            boolean z = false;
            if (opt != null) {
                switch (opt.hashCode()) {
                    case 1156993347:
                        if (opt.equals(GLOBAL_OPT)) {
                            z = true;
                            break;
                        }
                        z = true;
                        break;
                    case 1333469547:
                        break;
                    default:
                        z = true;
                        break;
                }
                switch (z) {
                    case false:
                        userId = UserHandle.parseUserArg(getNextArgRequired());
                        break;
                    case true:
                        setsGlobal = true;
                        break;
                    default:
                        getErrPrintWriter().println("Error: Unknown option: " + opt);
                        break;
                }
            } else {
                String pkg = getNextArgRequired();
                if (pkg == null) {
                    getErrPrintWriter().println("Error: no package specified");
                    return -1;
                }
                String newStateRaw = getNextArgRequired();
                if (newStateRaw == null) {
                    getErrPrintWriter().println("Error: No state to set specified");
                    return -1;
                }
                boolean newState = Boolean.parseBoolean(newStateRaw);
                if (setsGlobal) {
                    this.mService.setHibernatingGlobally(pkg, newState);
                } else {
                    this.mService.setHibernatingForUser(pkg, userId, newState);
                }
                return 0;
            }
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:9:0x0019, code lost:
        if (r2.equals(com.android.server.apphibernation.AppHibernationShellCommand.USER_OPT) != false) goto L8;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int runGetState() {
        boolean requestsGlobal = false;
        int userId = -2;
        while (true) {
            String opt = getNextOption();
            boolean z = false;
            if (opt != null) {
                switch (opt.hashCode()) {
                    case 1156993347:
                        if (opt.equals(GLOBAL_OPT)) {
                            z = true;
                            break;
                        }
                        z = true;
                        break;
                    case 1333469547:
                        break;
                    default:
                        z = true;
                        break;
                }
                switch (z) {
                    case false:
                        userId = UserHandle.parseUserArg(getNextArgRequired());
                        break;
                    case true:
                        requestsGlobal = true;
                        break;
                    default:
                        getErrPrintWriter().println("Error: Unknown option: " + opt);
                        break;
                }
            } else {
                String pkg = getNextArgRequired();
                if (pkg == null) {
                    getErrPrintWriter().println("Error: No package specified");
                    return -1;
                }
                boolean isHibernating = requestsGlobal ? this.mService.isHibernatingGlobally(pkg) : this.mService.isHibernatingForUser(pkg, userId);
                PrintWriter pw = getOutPrintWriter();
                pw.println(isHibernating);
                return 0;
            }
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("App hibernation (app_hibernation) commands: ");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("");
        pw.println("  set-state [--user USER_ID] [--global] PACKAGE true|false");
        pw.println("    Sets the hibernation state of the package to value specified. Optionally");
        pw.println("    may specify a user id or set global hibernation state.");
        pw.println("");
        pw.println("  get-state [--user USER_ID] [--global] PACKAGE");
        pw.println("    Gets the hibernation state of the package. Optionally may specify a user");
        pw.println("    id or request global hibernation state.");
        pw.println("");
    }
}
