package com.android.server.power;

import android.content.Intent;
import android.os.RemoteException;
import android.os.ShellCommand;
import com.android.server.power.PowerManagerService;
import java.io.PrintWriter;
import java.util.List;
/* loaded from: classes2.dex */
class PowerManagerShellCommand extends ShellCommand {
    private static final int LOW_POWER_MODE_ON = 1;
    final PowerManagerService.BinderService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PowerManagerShellCommand(PowerManagerService.BinderService service) {
        this.mService = service;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public int onCommand(String cmd) {
        char c;
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        try {
            switch (cmd.hashCode()) {
                case -531688203:
                    if (cmd.equals("set-adaptive-power-saver-enabled")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 584761923:
                    if (cmd.equals("list-ambient-display-suppression-tokens")) {
                        c = 4;
                        break;
                    }
                    c = 65535;
                    break;
                case 774730613:
                    if (cmd.equals("suppress-ambient-display")) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case 1032507032:
                    if (cmd.equals("set-fixed-performance-mode-enabled")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 1369181230:
                    if (cmd.equals("set-mode")) {
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
                    return runSetAdaptiveEnabled();
                case 1:
                    return runSetMode();
                case 2:
                    return runSetFixedPerformanceModeEnabled();
                case 3:
                    return runSuppressAmbientDisplay();
                case 4:
                    return runListAmbientDisplaySuppressionTokens();
                default:
                    return handleDefaultCommands(cmd);
            }
        } catch (RemoteException e) {
            pw.println("Remote exception: " + e);
            return -1;
        }
    }

    private int runSetAdaptiveEnabled() throws RemoteException {
        this.mService.setAdaptivePowerSaveEnabled(Boolean.parseBoolean(getNextArgRequired()));
        return 0;
    }

    private int runSetMode() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        try {
            int mode = Integer.parseInt(getNextArgRequired());
            this.mService.setPowerSaveModeEnabled(mode == 1);
            return 0;
        } catch (RuntimeException ex) {
            pw.println("Error: " + ex.toString());
            return -1;
        }
    }

    private int runSetFixedPerformanceModeEnabled() throws RemoteException {
        boolean success = this.mService.setPowerModeChecked(3, Boolean.parseBoolean(getNextArgRequired()));
        if (!success) {
            PrintWriter ew = getErrPrintWriter();
            ew.println("Failed to set FIXED_PERFORMANCE mode");
            ew.println("This is likely because Power HAL AIDL is not implemented on this device");
        }
        return success ? 0 : -1;
    }

    private int runSuppressAmbientDisplay() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        try {
            String token = getNextArgRequired();
            boolean enabled = Boolean.parseBoolean(getNextArgRequired());
            this.mService.suppressAmbientDisplay(token, enabled);
            return 0;
        } catch (RuntimeException ex) {
            pw.println("Error: " + ex.toString());
            return -1;
        }
    }

    private int runListAmbientDisplaySuppressionTokens() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        List<String> tokens = this.mService.getAmbientDisplaySuppressionTokens();
        if (tokens.isEmpty()) {
            pw.println("none");
        } else {
            pw.println(String.format("[%s]", String.join(", ", tokens)));
        }
        return 0;
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Power manager (power) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("");
        pw.println("  set-adaptive-power-saver-enabled [true|false]");
        pw.println("    enables or disables adaptive power saver.");
        pw.println("  set-mode MODE");
        pw.println("    sets the power mode of the device to MODE.");
        pw.println("    1 turns low power mode on and 0 turns low power mode off.");
        pw.println("  set-fixed-performance-mode-enabled [true|false]");
        pw.println("    enables or disables fixed performance mode");
        pw.println("    note: this will affect system performance and should only be used");
        pw.println("          during development");
        pw.println("  suppress-ambient-display <token> [true|false]");
        pw.println("    suppresses the current ambient display configuration and disables");
        pw.println("    ambient display");
        pw.println("  list-ambient-display-suppression-tokens");
        pw.println("    prints the tokens used to suppress ambient display");
        pw.println();
        Intent.printIntentArgsHelp(pw, "");
    }
}
