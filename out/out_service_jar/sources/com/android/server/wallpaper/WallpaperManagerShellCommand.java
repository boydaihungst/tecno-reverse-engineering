package com.android.server.wallpaper;

import android.os.RemoteException;
import android.os.ShellCommand;
import android.util.Log;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
public class WallpaperManagerShellCommand extends ShellCommand {
    private static final String TAG = "WallpaperManagerShellCommand";
    private final WallpaperManagerService mService;

    public WallpaperManagerShellCommand(WallpaperManagerService service) {
        this.mService = service;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:19:0x0035, code lost:
        if (r5.equals("dim-with-uid") != false) goto L11;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public int onCommand(String cmd) {
        char c = 1;
        if (cmd == null) {
            onHelp();
            return 1;
        }
        switch (cmd.hashCode()) {
            case -1462105208:
                if (cmd.equals("set-dim-amount")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case -296046994:
                break;
            case 1499:
                if (cmd.equals("-h")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 3198785:
                if (cmd.equals("help")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 309630996:
                if (cmd.equals("get-dim-amount")) {
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
                return setWallpaperDimAmount();
            case 1:
                return setDimmingWithUid();
            case 2:
                return getWallpaperDimAmount();
            case 3:
            case 4:
                onHelp();
                return 0;
            default:
                return handleDefaultCommands(cmd);
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Wallpaper manager commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println();
        pw.println("  set-dim-amount DIMMING");
        pw.println("    Sets the current dimming value to DIMMING (a number between 0 and 1).");
        pw.println();
        pw.println("  dim-with-uid UID DIMMING");
        pw.println("    Sets the wallpaper dim amount to DIMMING as if an app with uid, UID, called it.");
        pw.println();
        pw.println("  get-dim-amount");
        pw.println("    Get the current wallpaper dim amount.");
    }

    private int setWallpaperDimAmount() {
        float dimAmount = Float.parseFloat(getNextArgRequired());
        try {
            this.mService.setWallpaperDimAmount(dimAmount);
        } catch (RemoteException e) {
            Log.e(TAG, "Can't set wallpaper dim amount");
        }
        getOutPrintWriter().println("Dimming the wallpaper to: " + dimAmount);
        return 0;
    }

    private int getWallpaperDimAmount() {
        float dimAmount = this.mService.getWallpaperDimAmount();
        getOutPrintWriter().println("The current wallpaper dim amount is: " + dimAmount);
        return 0;
    }

    private int setDimmingWithUid() {
        int mockUid = Integer.parseInt(getNextArgRequired());
        float mockDimAmount = Float.parseFloat(getNextArgRequired());
        this.mService.setWallpaperDimAmountForUid(mockUid, mockDimAmount);
        getOutPrintWriter().println("Dimming the wallpaper for UID: " + mockUid + " to: " + mockDimAmount);
        return 0;
    }
}
