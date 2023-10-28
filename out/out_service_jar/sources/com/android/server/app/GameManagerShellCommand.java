package com.android.server.app;

import android.app.ActivityManager;
import android.app.IGameManagerService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ShellCommand;
import android.util.ArraySet;
import com.android.server.app.GameManagerService;
import com.android.server.wm.CompatModePackages;
import java.io.PrintWriter;
import java.util.Locale;
/* loaded from: classes.dex */
public class GameManagerShellCommand extends ShellCommand {
    private static final ArraySet<Long> DOWNSCALE_CHANGE_IDS = new ArraySet<>(new Long[]{Long.valueOf((long) CompatModePackages.DOWNSCALED), Long.valueOf((long) CompatModePackages.DOWNSCALE_90), Long.valueOf((long) CompatModePackages.DOWNSCALE_85), Long.valueOf((long) CompatModePackages.DOWNSCALE_80), Long.valueOf((long) CompatModePackages.DOWNSCALE_75), Long.valueOf((long) CompatModePackages.DOWNSCALE_70), Long.valueOf((long) CompatModePackages.DOWNSCALE_65), Long.valueOf((long) CompatModePackages.DOWNSCALE_60), Long.valueOf((long) CompatModePackages.DOWNSCALE_55), Long.valueOf((long) CompatModePackages.DOWNSCALE_50), Long.valueOf((long) CompatModePackages.DOWNSCALE_45), Long.valueOf((long) CompatModePackages.DOWNSCALE_40), Long.valueOf((long) CompatModePackages.DOWNSCALE_35), Long.valueOf((long) CompatModePackages.DOWNSCALE_30)});

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public int onCommand(String cmd) {
        char c;
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        try {
            switch (cmd.hashCode()) {
                case 113762:
                    if (cmd.equals("set")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 3322014:
                    if (cmd.equals("list")) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case 3357091:
                    if (cmd.equals(GameManagerService.GamePackageConfiguration.GameModeConfiguration.MODE_KEY)) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 108404047:
                    if (cmd.equals("reset")) {
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
                    return runSetGameMode(pw);
                case 1:
                    return runResetGameMode(pw);
                case 2:
                    return runGameMode(pw);
                case 3:
                    return runGameList(pw);
                default:
                    return handleDefaultCommands(cmd);
            }
        } catch (Exception e) {
            pw.println("Error: " + e);
            return -1;
        }
    }

    private int runGameList(PrintWriter pw) throws ServiceManager.ServiceNotFoundException, RemoteException {
        String packageName = getNextArgRequired();
        GameManagerService gameManagerService = (GameManagerService) ServiceManager.getService("game");
        String listStr = gameManagerService.getInterventionList(packageName);
        if (listStr == null) {
            pw.println("No interventions found for " + packageName);
            return 0;
        }
        pw.println(packageName + " interventions: " + listStr);
        return 0;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private int runGameMode(PrintWriter pw) throws ServiceManager.ServiceNotFoundException, RemoteException {
        char c;
        String option = getNextOption();
        String userIdStr = null;
        if (option != null && option.equals("--user")) {
            userIdStr = getNextArgRequired();
        }
        String gameMode = getNextArgRequired();
        String packageName = getNextArgRequired();
        IGameManagerService service = IGameManagerService.Stub.asInterface(ServiceManager.getServiceOrThrow("game"));
        boolean batteryModeSupported = false;
        boolean perfModeSupported = false;
        int[] modes = service.getAvailableGameModes(packageName);
        for (int mode : modes) {
            if (mode == 2) {
                perfModeSupported = true;
            } else if (mode == 3) {
                batteryModeSupported = true;
            }
        }
        int userId = userIdStr != null ? Integer.parseInt(userIdStr) : ActivityManager.getCurrentUser();
        String lowerCase = gameMode.toLowerCase();
        switch (lowerCase.hashCode()) {
            case -1480388560:
                if (lowerCase.equals("performance")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case -331239923:
                if (lowerCase.equals("battery")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case 49:
                if (lowerCase.equals("1")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 50:
                if (lowerCase.equals("2")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 51:
                if (lowerCase.equals("3")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 1312628413:
                if (lowerCase.equals("standard")) {
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
            case 1:
                service.setGameMode(packageName, 1, userId);
                return 0;
            case 2:
            case 3:
                if (!perfModeSupported) {
                    pw.println("Game mode: " + gameMode + " not supported by " + packageName);
                    return -1;
                }
                service.setGameMode(packageName, 2, userId);
                return 0;
            case 4:
            case 5:
                if (!batteryModeSupported) {
                    pw.println("Game mode: " + gameMode + " not supported by " + packageName);
                    return -1;
                }
                service.setGameMode(packageName, 3, userId);
                return 0;
            default:
                pw.println("Invalid game mode: " + gameMode);
                return -1;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [240=4] */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:13:0x0050, code lost:
        if (r7.equals("--downscale") != false) goto L13;
     */
    /* JADX WARN: Code restructure failed: missing block: B:79:0x01af, code lost:
        if (r7.equals("performance") != false) goto L85;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int runSetGameMode(PrintWriter pw) throws ServiceManager.ServiceNotFoundException, RemoteException {
        String option = getNextArgRequired();
        if (!option.equals("--mode")) {
            pw.println("Invalid option '" + option + "'");
            return -1;
        }
        String gameMode = getNextArgRequired();
        String userIdStr = null;
        String fpsStr = null;
        String downscaleRatio = null;
        while (true) {
            String option2 = getNextOption();
            boolean z = true;
            if (option2 == null) {
                String packageName = getNextArgRequired();
                int userId = userIdStr != null ? Integer.parseInt(userIdStr) : ActivityManager.getCurrentUser();
                GameManagerService gameManagerService = (GameManagerService) ServiceManager.getService("game");
                int[] modes = gameManagerService.getAvailableGameModes(packageName);
                boolean batteryModeSupported = false;
                boolean perfModeSupported = false;
                for (int mode : modes) {
                    if (mode == 2) {
                        perfModeSupported = true;
                    } else if (mode == 3) {
                        batteryModeSupported = true;
                    }
                }
                String lowerCase = gameMode.toLowerCase(Locale.getDefault());
                switch (lowerCase.hashCode()) {
                    case -1480388560:
                        break;
                    case -331239923:
                        if (lowerCase.equals("battery")) {
                            z = true;
                            break;
                        }
                        z = true;
                        break;
                    case 50:
                        if (lowerCase.equals("2")) {
                            z = false;
                            break;
                        }
                        z = true;
                        break;
                    case 51:
                        if (lowerCase.equals("3")) {
                            z = true;
                            break;
                        }
                        z = true;
                        break;
                    default:
                        z = true;
                        break;
                }
                switch (z) {
                    case false:
                    case true:
                        if (!perfModeSupported) {
                            pw.println("Game mode: " + gameMode + " not supported by " + packageName);
                            return -1;
                        }
                        gameManagerService.setGameModeConfigOverride(packageName, userId, 2, fpsStr, downscaleRatio);
                        break;
                    case true:
                    case true:
                        if (!batteryModeSupported) {
                            pw.println("Game mode: " + gameMode + " not supported by " + packageName);
                            return -1;
                        }
                        gameManagerService.setGameModeConfigOverride(packageName, userId, 3, fpsStr, downscaleRatio);
                        break;
                    default:
                        pw.println("Invalid game mode: " + gameMode);
                        return -1;
                }
                return 0;
            }
            switch (option2.hashCode()) {
                case 43000649:
                    if (option2.equals("--fps")) {
                        z = true;
                        break;
                    }
                    z = true;
                    break;
                case 1333469547:
                    if (option2.equals("--user")) {
                        z = false;
                        break;
                    }
                    z = true;
                    break;
                case 1807206472:
                    break;
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    if (userIdStr != null) {
                        pw.println("Duplicate option '" + option2 + "'");
                        return -1;
                    }
                    userIdStr = getNextArgRequired();
                    break;
                case true:
                    if (downscaleRatio != null) {
                        pw.println("Duplicate option '" + option2 + "'");
                        return -1;
                    }
                    String downscaleRatio2 = getNextArgRequired();
                    if (downscaleRatio2 != null && GameManagerService.getCompatChangeId(downscaleRatio2) == 0 && !downscaleRatio2.equals("disable")) {
                        pw.println("Invalid scaling ratio '" + downscaleRatio2 + "'");
                        return -1;
                    }
                    downscaleRatio = downscaleRatio2;
                    break;
                    break;
                case true:
                    if (fpsStr != null) {
                        pw.println("Duplicate option '" + option2 + "'");
                        return -1;
                    }
                    String fpsStr2 = getNextArgRequired();
                    if (fpsStr2 != null && GameManagerService.getFpsInt(fpsStr2) == -1) {
                        pw.println("Invalid frame rate '" + fpsStr2 + "'");
                        return -1;
                    }
                    fpsStr = fpsStr2;
                    break;
                    break;
                default:
                    pw.println("Invalid option '" + option2 + "'");
                    return -1;
            }
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:12:0x0025, code lost:
        if (r3.equals("--mode") != false) goto L9;
     */
    /* JADX WARN: Code restructure failed: missing block: B:49:0x00e1, code lost:
        if (r9.equals("performance") != false) goto L45;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int runResetGameMode(PrintWriter pw) throws ServiceManager.ServiceNotFoundException, RemoteException {
        String gameMode = null;
        String userIdStr = null;
        while (true) {
            String option = getNextOption();
            boolean z = true;
            if (option != null) {
                switch (option.hashCode()) {
                    case 1333227331:
                        break;
                    case 1333469547:
                        if (option.equals("--user")) {
                            z = false;
                            break;
                        }
                        z = true;
                        break;
                    default:
                        z = true;
                        break;
                }
                switch (z) {
                    case false:
                        if (userIdStr == null) {
                            userIdStr = getNextArgRequired();
                            break;
                        } else {
                            pw.println("Duplicate option '" + option + "'");
                            return -1;
                        }
                    case true:
                        if (gameMode == null) {
                            gameMode = getNextArgRequired();
                            break;
                        } else {
                            pw.println("Duplicate option '" + option + "'");
                            return -1;
                        }
                    default:
                        pw.println("Invalid option '" + option + "'");
                        return -1;
                }
            } else {
                String packageName = getNextArgRequired();
                GameManagerService gameManagerService = (GameManagerService) ServiceManager.getService("game");
                int userId = userIdStr != null ? Integer.parseInt(userIdStr) : ActivityManager.getCurrentUser();
                if (gameMode == null) {
                    gameManagerService.resetGameModeConfigOverride(packageName, userId, -1);
                    return 0;
                }
                String lowerCase = gameMode.toLowerCase(Locale.getDefault());
                switch (lowerCase.hashCode()) {
                    case -1480388560:
                        break;
                    case -331239923:
                        if (lowerCase.equals("battery")) {
                            z = true;
                            break;
                        }
                        z = true;
                        break;
                    case 50:
                        if (lowerCase.equals("2")) {
                            z = false;
                            break;
                        }
                        z = true;
                        break;
                    case 51:
                        if (lowerCase.equals("3")) {
                            z = true;
                            break;
                        }
                        z = true;
                        break;
                    default:
                        z = true;
                        break;
                }
                switch (z) {
                    case false:
                    case true:
                        gameManagerService.resetGameModeConfigOverride(packageName, userId, 2);
                        break;
                    case true:
                    case true:
                        gameManagerService.resetGameModeConfigOverride(packageName, userId, 3);
                        break;
                    default:
                        pw.println("Invalid game mode: " + gameMode);
                        return -1;
                }
                return 0;
            }
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Game manager (game) commands:");
        pw.println("  help");
        pw.println("      Print this help text.");
        pw.println("  downscale");
        pw.println("      Deprecated. Please use `set` command.");
        pw.println("  mode [--user <USER_ID>] [1|2|3|standard|performance|battery] <PACKAGE_NAME>");
        pw.println("      Set app to run in the specified game mode, if supported.");
        pw.println("      --user <USER_ID>: apply for the given user,");
        pw.println("                        the current user is used when unspecified.");
        pw.println("  set --mode [2|3|performance|battery] [intervention configs] <PACKAGE_NAME>");
        pw.println("      Set app to run at given game mode with configs, if supported.");
        pw.println("      Intervention configs consists of:");
        pw.println("      --downscale [0.3|0.35|0.4|0.45|0.5|0.55|0.6|0.65");
        pw.println("                  |0.7|0.75|0.8|0.85|0.9|disable]");
        pw.println("      Set app to run at the specified scaling ratio.");
        pw.println("      --fps [30|45|60|90|120|disable]");
        pw.println("      Set app to run at the specified fps, if supported.");
        pw.println("  reset [--mode [2|3|performance|battery] --user <USER_ID>] <PACKAGE_NAME>");
        pw.println("      Resets the game mode of the app to device configuration.");
        pw.println("      --mode [2|3|performance|battery]: apply for the given mode,");
        pw.println("                                        resets all modes when unspecified.");
        pw.println("      --user <USER_ID>: apply for the given user,");
        pw.println("                        the current user is used when unspecified.");
    }
}
