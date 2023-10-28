package com.android.server.wallpapereffectsgeneration;

import android.os.ShellCommand;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
public class WallpaperEffectsGenerationManagerServiceShellCommand extends ShellCommand {
    private static final String TAG = WallpaperEffectsGenerationManagerServiceShellCommand.class.getSimpleName();
    private final WallpaperEffectsGenerationManagerService mService;

    public WallpaperEffectsGenerationManagerServiceShellCommand(WallpaperEffectsGenerationManagerService service) {
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
                            pw.println("WallpaperEffectsGenerationService temporarily reset. ");
                            return 0;
                        }
                        int duration = Integer.parseInt(getNextArgRequired());
                        this.mService.setTemporaryService(userId, serviceName, duration);
                        pw.println("WallpaperEffectsGenerationService temporarily set to " + serviceName + " for " + duration + "ms");
                        break;
                }
                return 0;
            default:
                return handleDefaultCommands(cmd);
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        try {
            pw.println("WallpaperEffectsGenerationService commands:");
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
