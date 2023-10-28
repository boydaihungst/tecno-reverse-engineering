package com.android.server.display.color;

import android.content.pm.PackageManagerInternal;
import android.os.ShellCommand;
import com.android.server.LocalServices;
/* loaded from: classes.dex */
class ColorDisplayShellCommand extends ShellCommand {
    private static final int ERROR = -1;
    private static final int SUCCESS = 0;
    private static final String USAGE = "usage: cmd color_display SUBCOMMAND [ARGS]\n    help\n      Shows this message.\n    set-saturation LEVEL\n      Sets the device saturation to the given LEVEL, 0-100 inclusive.\n    set-layer-saturation LEVEL CALLER_PACKAGE TARGET_PACKAGE\n      Sets the saturation LEVEL for all layers of the TARGET_PACKAGE, attributed\n      to the CALLER_PACKAGE. The lowest LEVEL from any CALLER_PACKAGE is applied.\n";
    private final ColorDisplayService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ColorDisplayShellCommand(ColorDisplayService service) {
        this.mService = service;
    }

    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        char c = 65535;
        switch (cmd.hashCode()) {
            case 245833689:
                if (cmd.equals("set-layer-saturation")) {
                    c = 1;
                    break;
                }
                break;
            case 726170141:
                if (cmd.equals("set-saturation")) {
                    c = 0;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return setSaturation();
            case 1:
                return setLayerSaturation();
            default:
                return handleDefaultCommands(cmd);
        }
    }

    private int setSaturation() {
        int level = getLevel();
        if (level == -1) {
            return -1;
        }
        this.mService.setSaturationLevelInternal(level);
        return 0;
    }

    private int setLayerSaturation() {
        int level = getLevel();
        if (level == -1) {
            return -1;
        }
        String callerPackageName = getPackageName();
        if (callerPackageName == null) {
            getErrPrintWriter().println("Error: CALLER_PACKAGE must be an installed package name");
            return -1;
        }
        String targetPackageName = getPackageName();
        if (targetPackageName == null) {
            getErrPrintWriter().println("Error: TARGET_PACKAGE must be an installed package name");
            return -1;
        }
        this.mService.setAppSaturationLevelInternal(callerPackageName, targetPackageName, level);
        return 0;
    }

    private String getPackageName() {
        String packageNameArg = getNextArg();
        if (((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getPackage(packageNameArg) == null) {
            return null;
        }
        return packageNameArg;
    }

    private int getLevel() {
        String levelArg = getNextArg();
        if (levelArg == null) {
            getErrPrintWriter().println("Error: Required argument LEVEL is unspecified");
            return -1;
        }
        try {
            int level = Integer.parseInt(levelArg);
            if (level < 0 || level > 100) {
                getErrPrintWriter().println("Error: LEVEL argument must be an integer between 0 and 100");
                return -1;
            }
            return level;
        } catch (NumberFormatException e) {
            getErrPrintWriter().println("Error: LEVEL argument is not an integer");
            return -1;
        }
    }

    public void onHelp() {
        getOutPrintWriter().print(USAGE);
    }
}
