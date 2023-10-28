package com.android.server.accessibility;

import android.app.ActivityManager;
import android.os.Binder;
import android.os.ShellCommand;
import android.os.UserHandle;
import com.android.server.LocalServices;
import com.android.server.wm.WindowManagerInternal;
import java.io.PrintWriter;
/* loaded from: classes.dex */
final class AccessibilityShellCommand extends ShellCommand {
    final AccessibilityManagerService mService;
    final SystemActionPerformer mSystemActionPerformer;
    final WindowManagerInternal mWindowManagerService = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);

    /* JADX INFO: Access modifiers changed from: package-private */
    public AccessibilityShellCommand(AccessibilityManagerService service, SystemActionPerformer systemActionPerformer) {
        this.mService = service;
        this.mSystemActionPerformer = systemActionPerformer;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public int onCommand(String cmd) {
        char c;
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        switch (cmd.hashCode()) {
            case -859068373:
                if (cmd.equals("get-bind-instant-service-allowed")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 789489311:
                if (cmd.equals("set-bind-instant-service-allowed")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 1340897306:
                if (cmd.equals("start-trace")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 1748820581:
                if (cmd.equals("call-system-action")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 1857979322:
                if (cmd.equals("stop-trace")) {
                    c = 4;
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
                return runGetBindInstantServiceAllowed();
            case 1:
                return runSetBindInstantServiceAllowed();
            case 2:
                return runCallSystemAction();
            case 3:
            case 4:
                return this.mService.getTraceManager().onShellCommand(cmd, this);
            default:
                return -1;
        }
    }

    private int runGetBindInstantServiceAllowed() {
        Integer userId = parseUserId();
        if (userId == null) {
            return -1;
        }
        getOutPrintWriter().println(Boolean.toString(this.mService.getBindInstantServiceAllowed(userId.intValue())));
        return 0;
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
        this.mService.setBindInstantServiceAllowed(userId.intValue(), Boolean.parseBoolean(allowed));
        return 0;
    }

    private int runCallSystemAction() {
        String option;
        int callingUid = Binder.getCallingUid();
        if ((callingUid != 0 && callingUid != 1000 && callingUid != 2000) || (option = getNextArg()) == null) {
            return -1;
        }
        int actionId = Integer.parseInt(option);
        this.mSystemActionPerformer.performSystemAction(actionId);
        return 0;
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
        return Integer.valueOf(ActivityManager.getCurrentUser());
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Accessibility service (accessibility) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("  set-bind-instant-service-allowed [--user <USER_ID>] true|false ");
        pw.println("    Set whether binding to services provided by instant apps is allowed.");
        pw.println("  get-bind-instant-service-allowed [--user <USER_ID>]");
        pw.println("    Get whether binding to services provided by instant apps is allowed.");
        pw.println("  call-system-action <ACTION_ID>");
        pw.println("    Calls the system action with the given action id.");
        this.mService.getTraceManager().onHelp(pw);
    }
}
