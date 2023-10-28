package com.android.server.audio;

import android.content.Context;
import android.media.AudioManager;
import android.os.ShellCommand;
import java.io.PrintWriter;
/* loaded from: classes.dex */
class AudioManagerShellCommand extends ShellCommand {
    private static final String TAG = "AudioManagerShellCommand";
    private final AudioService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AudioManagerShellCommand(AudioService service) {
        this.mService = service;
    }

    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        getOutPrintWriter();
        char c = 65535;
        switch (cmd.hashCode()) {
            case -1873164504:
                if (cmd.equals("set-encoded-surround-mode")) {
                    c = 2;
                    break;
                }
                break;
            case -1340000401:
                if (cmd.equals("set-surround-format-enabled")) {
                    c = 0;
                    break;
                }
                break;
            case 937504014:
                if (cmd.equals("get-is-surround-format-enabled")) {
                    c = 1;
                    break;
                }
                break;
            case 1578511132:
                if (cmd.equals("get-encoded-surround-mode")) {
                    c = 3;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return setSurroundFormatEnabled();
            case 1:
                return getIsSurroundFormatEnabled();
            case 2:
                return setEncodedSurroundMode();
            case 3:
                return getEncodedSurroundMode();
            default:
                return 0;
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Audio manager commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println();
        pw.println("  set-surround-format-enabled SURROUND_FORMAT IS_ENABLED");
        pw.println("    Enables/disabled the SURROUND_FORMAT based on IS_ENABLED");
        pw.println("  get-is-surround-format-enabled SURROUND_FORMAT");
        pw.println("    Returns if the SURROUND_FORMAT is enabled");
        pw.println("  set-encoded-surround-mode SURROUND_SOUND_MODE");
        pw.println("    Sets the encoded surround sound mode to SURROUND_SOUND_MODE");
        pw.println("  get-encoded-surround-mode");
        pw.println("    Returns the encoded surround sound mode");
    }

    private int setSurroundFormatEnabled() {
        String surroundFormatText = getNextArg();
        String isSurroundFormatEnabledText = getNextArg();
        if (surroundFormatText == null) {
            getErrPrintWriter().println("Error: no surroundFormat specified");
            return 1;
        } else if (isSurroundFormatEnabledText == null) {
            getErrPrintWriter().println("Error: no enabled value for surroundFormat specified");
            return 1;
        } else {
            try {
                int surroundFormat = Integer.parseInt(surroundFormatText);
                boolean isSurroundFormatEnabled = Boolean.parseBoolean(isSurroundFormatEnabledText);
                if (surroundFormat < 0) {
                    getErrPrintWriter().println("Error: invalid value of surroundFormat");
                    return 1;
                }
                Context context = this.mService.mContext;
                AudioManager am = (AudioManager) context.getSystemService(AudioManager.class);
                am.setSurroundFormatEnabled(surroundFormat, isSurroundFormatEnabled);
                return 0;
            } catch (NumberFormatException e) {
                getErrPrintWriter().println("Error: wrong format specified for surroundFormat");
                return 1;
            }
        }
    }

    private int getIsSurroundFormatEnabled() {
        String surroundFormatText = getNextArg();
        if (surroundFormatText == null) {
            getErrPrintWriter().println("Error: no surroundFormat specified");
            return 1;
        }
        try {
            int surroundFormat = Integer.parseInt(surroundFormatText);
            if (surroundFormat < 0) {
                getErrPrintWriter().println("Error: invalid value of surroundFormat");
                return 1;
            }
            Context context = this.mService.mContext;
            AudioManager am = (AudioManager) context.getSystemService(AudioManager.class);
            getOutPrintWriter().println("Value of enabled for " + surroundFormat + " is: " + am.isSurroundFormatEnabled(surroundFormat));
            return 0;
        } catch (NumberFormatException e) {
            getErrPrintWriter().println("Error: wrong format specified for surroundFormat");
            return 1;
        }
    }

    private int setEncodedSurroundMode() {
        String encodedSurroundModeText = getNextArg();
        if (encodedSurroundModeText == null) {
            getErrPrintWriter().println("Error: no encodedSurroundMode specified");
            return 1;
        }
        try {
            int encodedSurroundMode = Integer.parseInt(encodedSurroundModeText);
            if (encodedSurroundMode < 0) {
                getErrPrintWriter().println("Error: invalid value of encodedSurroundMode");
                return 1;
            }
            Context context = this.mService.mContext;
            AudioManager am = (AudioManager) context.getSystemService(AudioManager.class);
            am.setEncodedSurroundMode(encodedSurroundMode);
            return 0;
        } catch (NumberFormatException e) {
            getErrPrintWriter().println("Error: wrong format specified for encoded surround mode");
            return 1;
        }
    }

    private int getEncodedSurroundMode() {
        Context context = this.mService.mContext;
        AudioManager am = (AudioManager) context.getSystemService(AudioManager.class);
        getOutPrintWriter().println("Encoded surround mode: " + am.getEncodedSurroundMode());
        return 0;
    }
}
