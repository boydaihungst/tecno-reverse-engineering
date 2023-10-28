package com.android.server.recoverysystem;

import android.content.IntentSender;
import android.os.IRecoverySystem;
import android.os.RemoteException;
import android.os.ShellCommand;
import com.android.server.content.SyncStorageEngine;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
public class RecoverySystemShellCommand extends ShellCommand {
    private final IRecoverySystem mService;

    public RecoverySystemShellCommand(RecoverySystemService service) {
        this.mService = service;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public int onCommand(String cmd) {
        char c;
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        try {
            switch (cmd.hashCode()) {
                case -779212638:
                    if (cmd.equals("clear-lskf")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 1214227142:
                    if (cmd.equals("is-lskf-captured")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 1256867232:
                    if (cmd.equals("request-lskf")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 1405182928:
                    if (cmd.equals("reboot-and-apply")) {
                        c = 3;
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
                    return requestLskf();
                case 1:
                    return clearLskf();
                case 2:
                    return isLskfCaptured();
                case 3:
                    return rebootAndApply();
                default:
                    return handleDefaultCommands(cmd);
            }
        } catch (Exception e) {
            getErrPrintWriter().println("Error while executing command: " + cmd);
            e.printStackTrace(getErrPrintWriter());
            return -1;
        }
    }

    private int requestLskf() throws RemoteException {
        String packageName = getNextArgRequired();
        boolean success = this.mService.requestLskf(packageName, (IntentSender) null);
        PrintWriter pw = getOutPrintWriter();
        Object[] objArr = new Object[2];
        objArr[0] = packageName;
        objArr[1] = success ? SyncStorageEngine.MESG_SUCCESS : "failure";
        pw.printf("Request LSKF for packageName: %s, status: %s\n", objArr);
        return 0;
    }

    private int clearLskf() throws RemoteException {
        String packageName = getNextArgRequired();
        boolean success = this.mService.clearLskf(packageName);
        PrintWriter pw = getOutPrintWriter();
        Object[] objArr = new Object[2];
        objArr[0] = packageName;
        objArr[1] = success ? SyncStorageEngine.MESG_SUCCESS : "failure";
        pw.printf("Clear LSKF for packageName: %s, status: %s\n", objArr);
        return 0;
    }

    private int isLskfCaptured() throws RemoteException {
        String packageName = getNextArgRequired();
        boolean captured = this.mService.isLskfCaptured(packageName);
        PrintWriter pw = getOutPrintWriter();
        Object[] objArr = new Object[2];
        objArr[0] = packageName;
        objArr[1] = captured ? "true" : "false";
        pw.printf("%s LSKF capture status: %s\n", objArr);
        return 0;
    }

    private int rebootAndApply() throws RemoteException {
        String packageName = getNextArgRequired();
        String rebootReason = getNextArgRequired();
        boolean success = this.mService.rebootWithLskf(packageName, rebootReason, false) == 0;
        PrintWriter pw = getOutPrintWriter();
        Object[] objArr = new Object[2];
        objArr[0] = packageName;
        objArr[1] = success ? SyncStorageEngine.MESG_SUCCESS : "failure";
        pw.printf("%s Reboot and apply status: %s\n", objArr);
        return 0;
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Recovery system commands:");
        pw.println("  request-lskf <package_name>");
        pw.println("  clear-lskf");
        pw.println("  is-lskf-captured <package_name>");
        pw.println("  reboot-and-apply <package_name> <reason>");
    }
}
