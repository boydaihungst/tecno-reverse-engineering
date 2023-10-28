package com.android.server.cloudsearch;

import android.os.ShellCommand;
import java.io.PrintWriter;
/* loaded from: classes.dex */
public class CloudSearchManagerServiceShellCommand extends ShellCommand {
    private static final String TAG = CloudSearchManagerServiceShellCommand.class.getSimpleName();
    private final CloudSearchManagerService mService;

    public CloudSearchManagerServiceShellCommand(CloudSearchManagerService service) {
        this.mService = service;
    }

    public int onCommand(String cmd) {
        boolean z;
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        char c = 65535;
        switch (cmd.hashCode()) {
            case 113762:
                if (cmd.equals("set")) {
                    z = false;
                    break;
                }
            default:
                z = true;
                break;
        }
        switch (z) {
            case false:
                String what = getNextArgRequired();
                switch (what.hashCode()) {
                    case 2003978041:
                        if (what.equals("temporary-service")) {
                            c = 0;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                        int userId = Integer.parseInt(getNextArgRequired());
                        String serviceName = getNextArg();
                        if (serviceName == null) {
                            this.mService.resetTemporaryService(userId);
                            pw.println("CloudSearchService temporarily reset. ");
                            return 0;
                        }
                        int duration = Integer.parseInt(getNextArgRequired());
                        String[] services = serviceName.split(";");
                        if (services.length != 0) {
                            this.mService.setTemporaryServices(userId, services, duration);
                            pw.println("CloudSearchService temporarily set to " + serviceName + " for " + duration + "ms");
                            break;
                        } else {
                            return 0;
                        }
                }
                return 0;
            default:
                return handleDefaultCommands(cmd);
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        try {
            pw.println("CloudSearchManagerService commands:");
            pw.println("  help");
            pw.println("    Prints this help text.");
            pw.println("");
            pw.println("  set temporary-service USER_ID [COMPONENT_NAME DURATION]");
            pw.println("    Temporarily (for DURATION ms) changes the service implemtation.");
            pw.println("    To reset, call with just the USER_ID argument.");
            pw.println("");
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
}
