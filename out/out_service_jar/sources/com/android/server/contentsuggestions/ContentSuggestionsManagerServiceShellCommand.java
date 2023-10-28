package com.android.server.contentsuggestions;

import android.os.ShellCommand;
import java.io.PrintWriter;
/* loaded from: classes.dex */
public class ContentSuggestionsManagerServiceShellCommand extends ShellCommand {
    private static final String TAG = ContentSuggestionsManagerServiceShellCommand.class.getSimpleName();
    private final ContentSuggestionsManagerService mService;

    public ContentSuggestionsManagerServiceShellCommand(ContentSuggestionsManagerService service) {
        this.mService = service;
    }

    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        char c = 65535;
        switch (cmd.hashCode()) {
            case 102230:
                if (cmd.equals("get")) {
                    c = 1;
                    break;
                }
                break;
            case 113762:
                if (cmd.equals("set")) {
                    c = 0;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return requestSet(pw);
            case 1:
                return requestGet(pw);
            default:
                return handleDefaultCommands(cmd);
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        try {
            pw.println("ContentSuggestionsManagerService commands:");
            pw.println("  help");
            pw.println("    Prints this help text.");
            pw.println("");
            pw.println("  set temporary-service USER_ID [COMPONENT_NAME DURATION]");
            pw.println("    Temporarily (for DURATION ms) changes the service implementation.");
            pw.println("    To reset, call with just the USER_ID argument.");
            pw.println("");
            pw.println("  set default-service-enabled USER_ID [true|false]");
            pw.println("    Enable / disable the default service for the user.");
            pw.println("");
            pw.println("  get default-service-enabled USER_ID");
            pw.println("    Checks whether the default service is enabled for the user.");
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

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private int requestSet(PrintWriter pw) {
        boolean z;
        String what = getNextArgRequired();
        switch (what.hashCode()) {
            case 529654941:
                if (what.equals("default-service-enabled")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            case 2003978041:
                if (what.equals("temporary-service")) {
                    z = false;
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
                return setTemporaryService(pw);
            case true:
                return setDefaultServiceEnabled();
            default:
                pw.println("Invalid set: " + what);
                return -1;
        }
    }

    private int requestGet(PrintWriter pw) {
        boolean z;
        String what = getNextArgRequired();
        switch (what.hashCode()) {
            case 529654941:
                if (what.equals("default-service-enabled")) {
                    z = false;
                    break;
                }
            default:
                z = true;
                break;
        }
        switch (z) {
            case false:
                return getDefaultServiceEnabled(pw);
            default:
                pw.println("Invalid get: " + what);
                return -1;
        }
    }

    private int setTemporaryService(PrintWriter pw) {
        int userId = Integer.parseInt(getNextArgRequired());
        String serviceName = getNextArg();
        if (serviceName == null) {
            this.mService.resetTemporaryService(userId);
            return 0;
        }
        int duration = Integer.parseInt(getNextArgRequired());
        this.mService.setTemporaryService(userId, serviceName, duration);
        pw.println("ContentSuggestionsService temporarily set to " + serviceName + " for " + duration + "ms");
        return 0;
    }

    private int setDefaultServiceEnabled() {
        int userId = getNextIntArgRequired();
        boolean enabled = Boolean.parseBoolean(getNextArg());
        this.mService.setDefaultServiceEnabled(userId, enabled);
        return 0;
    }

    private int getDefaultServiceEnabled(PrintWriter pw) {
        int userId = getNextIntArgRequired();
        boolean enabled = this.mService.isDefaultServiceEnabled(userId);
        pw.println(enabled);
        return 0;
    }

    private int getNextIntArgRequired() {
        return Integer.parseInt(getNextArgRequired());
    }
}
