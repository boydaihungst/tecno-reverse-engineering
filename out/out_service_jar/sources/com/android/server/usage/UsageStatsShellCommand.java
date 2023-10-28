package com.android.server.usage;

import android.app.ActivityManager;
import android.os.ShellCommand;
import android.os.UserHandle;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
class UsageStatsShellCommand extends ShellCommand {
    private final UsageStatsService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public UsageStatsShellCommand(UsageStatsService usageStatsService) {
        this.mService = usageStatsService;
    }

    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(null);
        }
        char c = 65535;
        switch (cmd.hashCode()) {
            case 2135796854:
                if (cmd.equals("clear-last-used-timestamps")) {
                    c = 0;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return runClearLastUsedTimestamps();
            default:
                return handleDefaultCommands(cmd);
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("UsageStats service (usagestats) commands:");
        pw.println("help");
        pw.println("    Print this help text.");
        pw.println();
        pw.println("clear-last-used-timestamps PACKAGE_NAME [-u | --user USER_ID]");
        pw.println("    Clears any existing usage data for the given package.");
        pw.println();
    }

    private int runClearLastUsedTimestamps() {
        String packageName = getNextArgRequired();
        int userId = -2;
        while (true) {
            String opt = getNextOption();
            if (opt != null) {
                if ("-u".equals(opt) || "--user".equals(opt)) {
                    userId = UserHandle.parseUserArg(getNextArgRequired());
                } else {
                    getErrPrintWriter().println("Error: unknown option: " + opt);
                    return -1;
                }
            } else {
                if (userId == -2) {
                    userId = ActivityManager.getCurrentUser();
                }
                this.mService.clearLastUsedTimestamps(packageName, userId);
                return 0;
            }
        }
    }
}
