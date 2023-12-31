package com.android.server.display;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.ShellCommand;
import android.util.Slog;
import android.view.Display;
import java.io.PrintWriter;
import java.util.Arrays;
/* loaded from: classes.dex */
class DisplayManagerShellCommand extends ShellCommand {
    private static final String TAG = "DisplayManagerShellCommand";
    private final DisplayManagerService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayManagerShellCommand(DisplayManagerService service) {
        this.mService = service;
    }

    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        getOutPrintWriter();
        char c = 65535;
        switch (cmd.hashCode()) {
            case -1505467592:
                if (cmd.equals("reset-brightness-configuration")) {
                    c = 1;
                    break;
                }
                break;
            case -1459563384:
                if (cmd.equals("get-displays")) {
                    c = 17;
                    break;
                }
                break;
            case -1021080420:
                if (cmd.equals("get-user-disabled-hdr-types")) {
                    c = 16;
                    break;
                }
                break;
            case -840680372:
                if (cmd.equals("undock")) {
                    c = 19;
                    break;
                }
                break;
            case -731435249:
                if (cmd.equals("dwb-logging-enable")) {
                    c = 4;
                    break;
                }
                break;
            case -687141135:
                if (cmd.equals("set-user-preferred-display-mode")) {
                    c = '\t';
                    break;
                }
                break;
            case -601773083:
                if (cmd.equals("get-user-preferred-display-mode")) {
                    c = 11;
                    break;
                }
                break;
            case 3088947:
                if (cmd.equals("dock")) {
                    c = 18;
                    break;
                }
                break;
            case 483509981:
                if (cmd.equals("ab-logging-enable")) {
                    c = 2;
                    break;
                }
                break;
            case 847215243:
                if (cmd.equals("dwb-set-cct")) {
                    c = '\b';
                    break;
                }
                break;
            case 1089842382:
                if (cmd.equals("ab-logging-disable")) {
                    c = 3;
                    break;
                }
                break;
            case 1265268983:
                if (cmd.equals("get-active-display-mode-at-start")) {
                    c = '\f';
                    break;
                }
                break;
            case 1428935945:
                if (cmd.equals("set-match-content-frame-rate-pref")) {
                    c = '\r';
                    break;
                }
                break;
            case 1604823708:
                if (cmd.equals("set-brightness")) {
                    c = 0;
                    break;
                }
                break;
            case 1863255293:
                if (cmd.equals("get-match-content-frame-rate-pref")) {
                    c = 14;
                    break;
                }
                break;
            case 1873686952:
                if (cmd.equals("dmd-logging-disable")) {
                    c = 7;
                    break;
                }
                break;
            case 1894268611:
                if (cmd.equals("dmd-logging-enable")) {
                    c = 6;
                    break;
                }
                break;
            case 1928353192:
                if (cmd.equals("set-user-disabled-hdr-types")) {
                    c = 15;
                    break;
                }
                break;
            case 2076592732:
                if (cmd.equals("clear-user-preferred-display-mode")) {
                    c = '\n';
                    break;
                }
                break;
            case 2081245916:
                if (cmd.equals("dwb-logging-disable")) {
                    c = 5;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return setBrightness();
            case 1:
                return resetBrightnessConfiguration();
            case 2:
                return setAutoBrightnessLoggingEnabled(true);
            case 3:
                return setAutoBrightnessLoggingEnabled(false);
            case 4:
                return setDisplayWhiteBalanceLoggingEnabled(true);
            case 5:
                return setDisplayWhiteBalanceLoggingEnabled(false);
            case 6:
                return setDisplayModeDirectorLoggingEnabled(true);
            case 7:
                return setDisplayModeDirectorLoggingEnabled(false);
            case '\b':
                return setAmbientColorTemperatureOverride();
            case '\t':
                return setUserPreferredDisplayMode();
            case '\n':
                return clearUserPreferredDisplayMode();
            case 11:
                return getUserPreferredDisplayMode();
            case '\f':
                return getActiveDisplayModeAtStart();
            case '\r':
                return setMatchContentFrameRateUserPreference();
            case 14:
                return getMatchContentFrameRateUserPreference();
            case 15:
                return setUserDisabledHdrTypes();
            case 16:
                return getUserDisabledHdrTypes();
            case 17:
                return getDisplays();
            case 18:
                return setDockedAndIdle();
            case 19:
                return unsetDockedAndIdle();
            default:
                return handleDefaultCommands(cmd);
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Display manager commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println();
        pw.println("  set-brightness BRIGHTNESS");
        pw.println("    Sets the current brightness to BRIGHTNESS (a number between 0 and 1).");
        pw.println("  reset-brightness-configuration");
        pw.println("    Reset the brightness to its default configuration.");
        pw.println("  ab-logging-enable");
        pw.println("    Enable auto-brightness logging.");
        pw.println("  ab-logging-disable");
        pw.println("    Disable auto-brightness logging.");
        pw.println("  dwb-logging-enable");
        pw.println("    Enable display white-balance logging.");
        pw.println("  dwb-logging-disable");
        pw.println("    Disable display white-balance logging.");
        pw.println("  dmd-logging-enable");
        pw.println("    Enable display mode director logging.");
        pw.println("  dmd-logging-disable");
        pw.println("    Disable display mode director logging.");
        pw.println("  dwb-set-cct CCT");
        pw.println("    Sets the ambient color temperature override to CCT (use -1 to disable).");
        pw.println("  set-user-preferred-display-mode WIDTH HEIGHT REFRESH-RATE DISPLAY_ID (optional)");
        pw.println("    Sets the user preferred display mode which has fields WIDTH, HEIGHT and REFRESH-RATE. If DISPLAY_ID is passed, the mode change is applied to displaywith id = DISPLAY_ID, else mode change is applied globally.");
        pw.println("  clear-user-preferred-display-mode DISPLAY_ID (optional)");
        pw.println("    Clears the user preferred display mode. If DISPLAY_ID is passed, the mode is cleared for  display with id = DISPLAY_ID, else mode is cleared globally.");
        pw.println("  get-user-preferred-display-mode DISPLAY_ID (optional)");
        pw.println("    Returns the user preferred display mode or null if no mode is set by user.If DISPLAY_ID is passed, the mode for display with id = DISPLAY_ID is returned, else global display mode is returned.");
        pw.println("  get-active-display-mode-at-start DISPLAY_ID");
        pw.println("    Returns the display mode which was found at boot time of display with id = DISPLAY_ID");
        pw.println("  set-match-content-frame-rate-pref PREFERENCE");
        pw.println("    Sets the match content frame rate preference as PREFERENCE ");
        pw.println("  get-match-content-frame-rate-pref");
        pw.println("    Returns the match content frame rate preference");
        pw.println("  set-user-disabled-hdr-types TYPES...");
        pw.println("    Sets the user disabled HDR types as TYPES");
        pw.println("  get-user-disabled-hdr-types");
        pw.println("    Returns the user disabled HDR types");
        pw.println("  get-displays [CATEGORY]");
        pw.println("    Returns the current displays. Can specify string category among");
        pw.println("    DisplayManager.DISPLAY_CATEGORY_*; must use the actual string value.");
        pw.println("  dock");
        pw.println("    Sets brightness to docked + idle screen brightness mode");
        pw.println("  undock");
        pw.println("    Sets brightness to active (normal) screen brightness mode");
        pw.println();
        Intent.printIntentArgsHelp(pw, "");
    }

    private int getDisplays() {
        String category = getNextArg();
        DisplayManager dm = (DisplayManager) this.mService.getContext().getSystemService(DisplayManager.class);
        Display[] displays = dm.getDisplays(category);
        PrintWriter out = getOutPrintWriter();
        out.println("Displays:");
        for (int i = 0; i < displays.length; i++) {
            out.println("  " + displays[i]);
        }
        return 0;
    }

    private int setBrightness() {
        String brightnessText = getNextArg();
        if (brightnessText == null) {
            getErrPrintWriter().println("Error: no brightness specified");
            return 1;
        }
        float brightness = -1.0f;
        try {
            brightness = Float.parseFloat(brightnessText);
        } catch (NumberFormatException e) {
        }
        if (brightness < 0.0f || brightness > 1.0f) {
            getErrPrintWriter().println("Error: brightness should be a number between 0 and 1");
            return 1;
        }
        Context context = this.mService.getContext();
        DisplayManager dm = (DisplayManager) context.getSystemService(DisplayManager.class);
        dm.setBrightness(0, brightness);
        return 0;
    }

    private int resetBrightnessConfiguration() {
        this.mService.resetBrightnessConfigurations();
        return 0;
    }

    private int setAutoBrightnessLoggingEnabled(boolean enabled) {
        this.mService.setAutoBrightnessLoggingEnabled(enabled);
        return 0;
    }

    private int setDisplayWhiteBalanceLoggingEnabled(boolean enabled) {
        this.mService.setDisplayWhiteBalanceLoggingEnabled(enabled);
        return 0;
    }

    private int setDisplayModeDirectorLoggingEnabled(boolean enabled) {
        this.mService.setDisplayModeDirectorLoggingEnabled(enabled);
        return 0;
    }

    private int setAmbientColorTemperatureOverride() {
        String cctText = getNextArg();
        if (cctText == null) {
            getErrPrintWriter().println("Error: no cct specified");
            return 1;
        }
        try {
            float cct = Float.parseFloat(cctText);
            this.mService.setAmbientColorTemperatureOverride(cct);
            return 0;
        } catch (NumberFormatException e) {
            getErrPrintWriter().println("Error: cct should be a number");
            return 1;
        }
    }

    private int setUserPreferredDisplayMode() {
        String widthText = getNextArg();
        if (widthText == null) {
            getErrPrintWriter().println("Error: no width specified");
            return 1;
        }
        String heightText = getNextArg();
        if (heightText == null) {
            getErrPrintWriter().println("Error: no height specified");
            return 1;
        }
        String refreshRateText = getNextArg();
        if (refreshRateText == null) {
            getErrPrintWriter().println("Error: no refresh-rate specified");
            return 1;
        }
        try {
            int width = Integer.parseInt(widthText);
            int height = Integer.parseInt(heightText);
            float refreshRate = Float.parseFloat(refreshRateText);
            if ((width < 0 || height < 0) && refreshRate <= 0.0f) {
                getErrPrintWriter().println("Error: invalid value of resolution (width, height) and refresh rate");
                return 1;
            }
            String displayIdText = getNextArg();
            int displayId = -1;
            if (displayIdText != null) {
                try {
                    displayId = Integer.parseInt(displayIdText);
                } catch (NumberFormatException e) {
                    getErrPrintWriter().println("Error: invalid format of display ID");
                    return 1;
                }
            }
            this.mService.setUserPreferredDisplayModeInternal(displayId, new Display.Mode(width, height, refreshRate));
            return 0;
        } catch (NumberFormatException e2) {
            getErrPrintWriter().println("Error: invalid format of width, height or refresh rate");
            return 1;
        }
    }

    private int clearUserPreferredDisplayMode() {
        String displayIdText = getNextArg();
        int displayId = -1;
        if (displayIdText != null) {
            try {
                displayId = Integer.parseInt(displayIdText);
            } catch (NumberFormatException e) {
                getErrPrintWriter().println("Error: invalid format of display ID");
                return 1;
            }
        }
        this.mService.setUserPreferredDisplayModeInternal(displayId, null);
        return 0;
    }

    private int getUserPreferredDisplayMode() {
        String displayIdText = getNextArg();
        int displayId = -1;
        if (displayIdText != null) {
            try {
                displayId = Integer.parseInt(displayIdText);
            } catch (NumberFormatException e) {
                getErrPrintWriter().println("Error: invalid format of display ID");
                return 1;
            }
        }
        Display.Mode mode = this.mService.getUserPreferredDisplayModeInternal(displayId);
        if (mode == null) {
            getOutPrintWriter().println("User preferred display mode: null");
            return 0;
        }
        getOutPrintWriter().println("User preferred display mode: " + mode.getPhysicalWidth() + " " + mode.getPhysicalHeight() + " " + mode.getRefreshRate());
        return 0;
    }

    private int getActiveDisplayModeAtStart() {
        String displayIdText = getNextArg();
        if (displayIdText == null) {
            getErrPrintWriter().println("Error: no displayId specified");
            return 1;
        }
        try {
            int displayId = Integer.parseInt(displayIdText);
            Display.Mode mode = this.mService.getActiveDisplayModeAtStart(displayId);
            if (mode == null) {
                getOutPrintWriter().println("Boot display mode: null");
                return 0;
            }
            getOutPrintWriter().println("Boot display mode: " + mode.getPhysicalWidth() + " " + mode.getPhysicalHeight() + " " + mode.getRefreshRate());
            return 0;
        } catch (NumberFormatException e) {
            getErrPrintWriter().println("Error: invalid displayId");
            return 1;
        }
    }

    private int setMatchContentFrameRateUserPreference() {
        String matchContentFrameRatePrefText = getNextArg();
        if (matchContentFrameRatePrefText == null) {
            getErrPrintWriter().println("Error: no matchContentFrameRatePref specified");
            return 1;
        }
        try {
            int matchContentFrameRatePreference = Integer.parseInt(matchContentFrameRatePrefText);
            if (matchContentFrameRatePreference < 0) {
                getErrPrintWriter().println("Error: invalid value of matchContentFrameRatePreference");
                return 1;
            }
            Context context = this.mService.getContext();
            DisplayManager dm = (DisplayManager) context.getSystemService(DisplayManager.class);
            int refreshRateSwitchingType = toRefreshRateSwitchingType(matchContentFrameRatePreference);
            dm.setRefreshRateSwitchingType(refreshRateSwitchingType);
            return 0;
        } catch (NumberFormatException e) {
            getErrPrintWriter().println("Error: invalid format of matchContentFrameRatePreference");
            return 1;
        }
    }

    private int getMatchContentFrameRateUserPreference() {
        Context context = this.mService.getContext();
        DisplayManager dm = (DisplayManager) context.getSystemService(DisplayManager.class);
        getOutPrintWriter().println("Match content frame rate type: " + dm.getMatchContentFrameRateUserPreference());
        return 0;
    }

    private int setUserDisabledHdrTypes() {
        String[] userDisabledHdrTypesText = peekRemainingArgs();
        if (userDisabledHdrTypesText == null) {
            getErrPrintWriter().println("Error: no userDisabledHdrTypes specified");
            return 1;
        }
        int[] userDisabledHdrTypes = new int[userDisabledHdrTypesText.length];
        int index = 0;
        try {
            int length = userDisabledHdrTypesText.length;
            int i = 0;
            while (i < length) {
                String userDisabledHdrType = userDisabledHdrTypesText[i];
                int index2 = index + 1;
                userDisabledHdrTypes[index] = Integer.parseInt(userDisabledHdrType);
                i++;
                index = index2;
            }
            Context context = this.mService.getContext();
            DisplayManager dm = (DisplayManager) context.getSystemService(DisplayManager.class);
            dm.setUserDisabledHdrTypes(userDisabledHdrTypes);
            return 0;
        } catch (NumberFormatException e) {
            getErrPrintWriter().println("Error: invalid format of userDisabledHdrTypes");
            return 1;
        }
    }

    private int getUserDisabledHdrTypes() {
        Context context = this.mService.getContext();
        DisplayManager dm = (DisplayManager) context.getSystemService(DisplayManager.class);
        int[] userDisabledHdrTypes = dm.getUserDisabledHdrTypes();
        getOutPrintWriter().println("User disabled HDR types: " + Arrays.toString(userDisabledHdrTypes));
        return 0;
    }

    private int toRefreshRateSwitchingType(int matchContentFrameRateType) {
        switch (matchContentFrameRateType) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                Slog.e(TAG, matchContentFrameRateType + " is not a valid value of matchContentFrameRate type.");
                return -1;
        }
    }

    private int setDockedAndIdle() {
        this.mService.setDockedAndIdleEnabled(true, 0);
        return 0;
    }

    private int unsetDockedAndIdle() {
        this.mService.setDockedAndIdleEnabled(false, 0);
        return 0;
    }
}
