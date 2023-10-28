package com.android.server.adb;

import com.android.modules.utils.BasicShellCommandHandler;
import java.io.PrintWriter;
import java.util.Objects;
/* loaded from: classes.dex */
class AdbShellCommand extends BasicShellCommandHandler {
    private final AdbService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AdbShellCommand(AdbService service) {
        this.mService = (AdbService) Objects.requireNonNull(service);
    }

    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(null);
        }
        PrintWriter pw = getOutPrintWriter();
        char c = 65535;
        switch (cmd.hashCode()) {
            case -138263081:
                if (cmd.equals("is-wifi-qr-supported")) {
                    c = 1;
                    break;
                }
                break;
            case 434812665:
                if (cmd.equals("is-wifi-supported")) {
                    c = 0;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                pw.println(Boolean.toString(this.mService.isAdbWifiSupported()));
                return 0;
            case 1:
                pw.println(Boolean.toString(this.mService.isAdbWifiQrSupported()));
                return 0;
            default:
                return handleDefaultCommands(cmd);
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Adb service commands:");
        pw.println("  help or -h");
        pw.println("    Print this help text.");
        pw.println("  is-wifi-supported");
        pw.println("    Returns \"true\" if adb over wifi is supported.");
        pw.println("  is-wifi-qr-supported");
        pw.println("    Returns \"true\" if adb over wifi + QR pairing is supported.");
        pw.println();
    }
}
