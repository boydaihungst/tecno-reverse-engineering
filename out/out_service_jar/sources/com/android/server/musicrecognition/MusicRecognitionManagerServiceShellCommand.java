package com.android.server.musicrecognition;

import android.os.ShellCommand;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
class MusicRecognitionManagerServiceShellCommand extends ShellCommand {
    private final MusicRecognitionManagerService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public MusicRecognitionManagerServiceShellCommand(MusicRecognitionManagerService service) {
        this.mService = service;
    }

    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        if ("set".equals(cmd)) {
            return requestSet(pw);
        }
        return handleDefaultCommands(cmd);
    }

    private int requestSet(PrintWriter pw) {
        String what = getNextArgRequired();
        if ("temporary-service".equals(what)) {
            return setTemporaryService(pw);
        }
        pw.println("Invalid set: " + what);
        return -1;
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
        pw.println("MusicRecognitionService temporarily set to " + serviceName + " for " + duration + "ms");
        return 0;
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        try {
            pw.println("MusicRecognition Service (music_recognition) commands:");
            pw.println("  help");
            pw.println("    Prints this help text.");
            pw.println("");
            pw.println("  set temporary-service USER_ID [COMPONENT_NAME DURATION]");
            pw.println("    Temporarily (for DURATION ms) changes the service implementation.");
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
