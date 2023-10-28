package com.android.server.content;

import android.content.IContentService;
import android.os.RemoteException;
import android.os.ShellCommand;
import java.io.PrintWriter;
/* loaded from: classes.dex */
public class ContentShellCommand extends ShellCommand {
    final IContentService mInterface;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ContentShellCommand(IContentService service) {
        this.mInterface = service;
    }

    public int onCommand(String cmd) {
        boolean z;
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        try {
            switch (cmd.hashCode()) {
                case -796331115:
                    if (cmd.equals("reset-today-stats")) {
                        z = false;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    return runResetTodayStats();
                default:
                    return handleDefaultCommands(cmd);
            }
        } catch (RemoteException e) {
            pw.println("Remote exception: " + e);
            return -1;
        }
    }

    private int runResetTodayStats() throws RemoteException {
        this.mInterface.resetTodayStats();
        return 0;
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Content service commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("");
        pw.println("  reset-today-stats");
        pw.println("    Reset 1-day sync stats.");
        pw.println();
    }
}
