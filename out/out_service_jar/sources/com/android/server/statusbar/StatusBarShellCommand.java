package com.android.server.statusbar;

import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.service.quicksettings.TileService;
import android.util.Pair;
import com.android.server.wm.ActivityTaskManagerService;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
public class StatusBarShellCommand extends ShellCommand {
    private static final IBinder sToken = new StatusBarShellCommandToken();
    private final Context mContext;
    private final StatusBarManagerService mInterface;

    public StatusBarShellCommand(StatusBarManagerService service, Context context) {
        this.mInterface = service;
        this.mContext = context;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public int onCommand(String cmd) {
        char c = 1;
        if (cmd == null) {
            onHelp();
            return 1;
        }
        try {
            switch (cmd.hashCode()) {
                case -1282000806:
                    if (cmd.equals("add-tile")) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case -1239176554:
                    if (cmd.equals("get-status-icons")) {
                        c = 7;
                        break;
                    }
                    c = 65535;
                    break;
                case -1067396926:
                    if (cmd.equals("tracing")) {
                        c = '\n';
                        break;
                    }
                    c = 65535;
                    break;
                case -1052548778:
                    if (cmd.equals("send-disable-flag")) {
                        c = '\t';
                        break;
                    }
                    c = 65535;
                    break;
                case -919868578:
                    if (cmd.equals("run-gc")) {
                        c = 11;
                        break;
                    }
                    c = 65535;
                    break;
                case -823073837:
                    if (cmd.equals("click-tile")) {
                        c = 5;
                        break;
                    }
                    c = 65535;
                    break;
                case -632085587:
                    if (cmd.equals("collapse")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case -339726761:
                    if (cmd.equals("remove-tile")) {
                        c = 4;
                        break;
                    }
                    c = 65535;
                    break;
                case 1499:
                    if (cmd.equals("-h")) {
                        c = '\f';
                        break;
                    }
                    c = 65535;
                    break;
                case 3095028:
                    if (cmd.equals("dump")) {
                        c = 14;
                        break;
                    }
                    c = 65535;
                    break;
                case 3198785:
                    if (cmd.equals("help")) {
                        c = '\r';
                        break;
                    }
                    c = 65535;
                    break;
                case 901899220:
                    if (cmd.equals("disable-for-setup")) {
                        c = '\b';
                        break;
                    }
                    c = 65535;
                    break;
                case 1612300298:
                    if (cmd.equals("check-support")) {
                        c = 6;
                        break;
                    }
                    c = 65535;
                    break;
                case 1629310709:
                    if (cmd.equals("expand-notifications")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 1672031734:
                    if (cmd.equals("expand-settings")) {
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
                    return runExpandNotifications();
                case 1:
                    return runExpandSettings();
                case 2:
                    return runCollapse();
                case 3:
                    return runAddTile();
                case 4:
                    return runRemoveTile();
                case 5:
                    return runClickTile();
                case 6:
                    PrintWriter pw = getOutPrintWriter();
                    pw.println(String.valueOf(TileService.isQuickSettingsSupported()));
                    return 0;
                case 7:
                    return runGetStatusIcons();
                case '\b':
                    return runDisableForSetup();
                case '\t':
                    return runSendDisableFlag();
                case '\n':
                    return runTracing();
                case 11:
                    return runGc();
                case '\f':
                case '\r':
                    onHelp();
                    return 0;
                case 14:
                    return super.handleDefaultCommands(cmd);
                default:
                    return runPassArgsToStatusBar();
            }
        } catch (RemoteException e) {
            PrintWriter pw2 = getOutPrintWriter();
            pw2.println("Remote exception: " + e);
            return -1;
        }
    }

    private int runAddTile() throws RemoteException {
        this.mInterface.addTile(ComponentName.unflattenFromString(getNextArgRequired()));
        return 0;
    }

    private int runRemoveTile() throws RemoteException {
        this.mInterface.remTile(ComponentName.unflattenFromString(getNextArgRequired()));
        return 0;
    }

    private int runClickTile() throws RemoteException {
        this.mInterface.clickTile(ComponentName.unflattenFromString(getNextArgRequired()));
        return 0;
    }

    private int runCollapse() throws RemoteException {
        this.mInterface.collapsePanels();
        return 0;
    }

    private int runExpandSettings() throws RemoteException {
        this.mInterface.expandSettingsPanel(null);
        return 0;
    }

    private int runExpandNotifications() throws RemoteException {
        this.mInterface.expandNotificationsPanel();
        return 0;
    }

    private int runGetStatusIcons() {
        String[] statusBarIcons;
        PrintWriter pw = getOutPrintWriter();
        for (String icon : this.mInterface.getStatusBarIcons()) {
            pw.println(icon);
        }
        return 0;
    }

    private int runDisableForSetup() {
        String arg = getNextArgRequired();
        String pkg = this.mContext.getPackageName();
        boolean disable = Boolean.parseBoolean(arg);
        if (disable) {
            StatusBarManagerService statusBarManagerService = this.mInterface;
            IBinder iBinder = sToken;
            statusBarManagerService.disable(61145088, iBinder, pkg);
            this.mInterface.disable2(0, iBinder, pkg);
        } else {
            StatusBarManagerService statusBarManagerService2 = this.mInterface;
            IBinder iBinder2 = sToken;
            statusBarManagerService2.disable(0, iBinder2, pkg);
            this.mInterface.disable2(0, iBinder2, pkg);
        }
        return 0;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:27:0x0065, code lost:
        if (r4.equals("search") != false) goto L9;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int runSendDisableFlag() {
        String pkg = this.mContext.getPackageName();
        StatusBarManager.DisableInfo info = new StatusBarManager.DisableInfo();
        String arg = getNextArg();
        while (true) {
            char c = 0;
            if (arg != null) {
                switch (arg.hashCode()) {
                    case -1786496516:
                        if (arg.equals("system-icons")) {
                            c = 5;
                            break;
                        }
                        c = 65535;
                        break;
                    case -906336856:
                        break;
                    case -755976775:
                        if (arg.equals("notification-alerts")) {
                            c = 3;
                            break;
                        }
                        c = 65535;
                        break;
                    case 3208415:
                        if (arg.equals("home")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    case 94755854:
                        if (arg.equals("clock")) {
                            c = 6;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1011652819:
                        if (arg.equals("statusbar-expansion")) {
                            c = 4;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1082295672:
                        if (arg.equals(ActivityTaskManagerService.DUMP_RECENTS_CMD)) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1368216504:
                        if (arg.equals("notification-icons")) {
                            c = 7;
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
                        info.setSearchDisabled(true);
                        break;
                    case 1:
                        info.setNagivationHomeDisabled(true);
                        break;
                    case 2:
                        info.setRecentsDisabled(true);
                        break;
                    case 3:
                        info.setNotificationPeekingDisabled(true);
                        break;
                    case 4:
                        info.setStatusBarExpansionDisabled(true);
                        break;
                    case 5:
                        info.setSystemIconsDisabled(true);
                        break;
                    case 6:
                        info.setClockDisabled(true);
                        break;
                    case 7:
                        info.setNotificationIconsDisabled(true);
                        break;
                }
                arg = getNextArg();
            } else {
                Pair<Integer, Integer> flagPair = info.toFlags();
                StatusBarManagerService statusBarManagerService = this.mInterface;
                int intValue = ((Integer) flagPair.first).intValue();
                IBinder iBinder = sToken;
                statusBarManagerService.disable(intValue, iBinder, pkg);
                this.mInterface.disable2(((Integer) flagPair.second).intValue(), iBinder, pkg);
                return 0;
            }
        }
    }

    private int runPassArgsToStatusBar() {
        this.mInterface.passThroughShellCommand(getAllArgs(), getOutFileDescriptor());
        return 0;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private int runTracing() {
        char c;
        String nextArg = getNextArg();
        switch (nextArg.hashCode()) {
            case 3540994:
                if (nextArg.equals("stop")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 109757538:
                if (nextArg.equals("start")) {
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
                this.mInterface.startTracing();
                break;
            case 1:
                this.mInterface.stopTracing();
                break;
        }
        return 0;
    }

    private int runGc() {
        this.mInterface.runGcForTest();
        return 0;
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Status bar commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("");
        pw.println("  expand-notifications");
        pw.println("    Open the notifications panel.");
        pw.println("");
        pw.println("  expand-settings");
        pw.println("    Open the notifications panel and expand quick settings if present.");
        pw.println("");
        pw.println("  collapse");
        pw.println("    Collapse the notifications and settings panel.");
        pw.println("");
        pw.println("  add-tile COMPONENT");
        pw.println("    Add a TileService of the specified component");
        pw.println("");
        pw.println("  remove-tile COMPONENT");
        pw.println("    Remove a TileService of the specified component");
        pw.println("");
        pw.println("  click-tile COMPONENT");
        pw.println("    Click on a TileService of the specified component");
        pw.println("");
        pw.println("  check-support");
        pw.println("    Check if this device supports QS + APIs");
        pw.println("");
        pw.println("  get-status-icons");
        pw.println("    Print the list of status bar icons and the order they appear in");
        pw.println("");
        pw.println("  disable-for-setup DISABLE");
        pw.println("    If true, disable status bar components unsuitable for device setup");
        pw.println("");
        pw.println("  send-disable-flag FLAG...");
        pw.println("    Send zero or more disable flags (parsed individually) to StatusBarManager");
        pw.println("    Valid options:");
        pw.println("        <blank>             - equivalent to \"none\"");
        pw.println("        none                - re-enables all components");
        pw.println("        search              - disable search");
        pw.println("        home                - disable naviagation home");
        pw.println("        recents             - disable recents/overview");
        pw.println("        notification-peek   - disable notification peeking");
        pw.println("        statusbar-expansion - disable status bar expansion");
        pw.println("        system-icons        - disable system icons appearing in status bar");
        pw.println("        clock               - disable clock appearing in status bar");
        pw.println("        notification-icons  - disable notification icons from status bar");
        pw.println("");
        pw.println("  tracing (start | stop)");
        pw.println("    Start or stop SystemUI tracing");
        pw.println("");
        pw.println("  NOTE: any command not listed here will be passed through to IStatusBar");
        pw.println("");
        pw.println("  Commands implemented in SystemUI:");
        pw.flush();
        this.mInterface.passThroughShellCommand(new String[0], getOutFileDescriptor());
    }

    /* loaded from: classes2.dex */
    private static final class StatusBarShellCommandToken extends Binder {
        private StatusBarShellCommandToken() {
        }
    }
}
