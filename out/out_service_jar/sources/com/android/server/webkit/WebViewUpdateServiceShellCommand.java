package com.android.server.webkit;

import android.os.RemoteException;
import android.os.ShellCommand;
import android.webkit.IWebViewUpdateService;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
class WebViewUpdateServiceShellCommand extends ShellCommand {
    final IWebViewUpdateService mInterface;

    /* JADX INFO: Access modifiers changed from: package-private */
    public WebViewUpdateServiceShellCommand(IWebViewUpdateService service) {
        this.mInterface = service;
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
                case -1857752288:
                    if (cmd.equals("enable-multiprocess")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case -1381305903:
                    if (cmd.equals("set-webview-implementation")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 436183515:
                    if (cmd.equals("disable-multiprocess")) {
                        c = 2;
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
                    return setWebViewImplementation();
                case 1:
                    return enableMultiProcess(true);
                case 2:
                    return enableMultiProcess(false);
                default:
                    return handleDefaultCommands(cmd);
            }
        } catch (RemoteException e) {
            pw.println("Remote exception: " + e);
            return -1;
        }
    }

    private int setWebViewImplementation() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        String shellChosenPackage = getNextArg();
        if (shellChosenPackage == null) {
            pw.println("Failed to switch, no PACKAGE provided.");
            pw.println("");
            helpSetWebViewImplementation();
            return 1;
        }
        String newPackage = this.mInterface.changeProviderAndSetting(shellChosenPackage);
        if (!shellChosenPackage.equals(newPackage)) {
            pw.println(String.format("Failed to switch to %s, the WebView implementation is now provided by %s.", shellChosenPackage, newPackage));
            return 1;
        }
        pw.println("Success");
        return 0;
    }

    private int enableMultiProcess(boolean enable) throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        this.mInterface.enableMultiProcess(enable);
        pw.println("Success");
        return 0;
    }

    public void helpSetWebViewImplementation() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("  set-webview-implementation PACKAGE");
        pw.println("    Set the WebView implementation to the specified package.");
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("WebView updater commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("");
        helpSetWebViewImplementation();
        pw.println("  enable-multiprocess");
        pw.println("    Enable multi-process mode for WebView");
        pw.println("  disable-multiprocess");
        pw.println("    Disable multi-process mode for WebView");
        pw.println();
    }
}
