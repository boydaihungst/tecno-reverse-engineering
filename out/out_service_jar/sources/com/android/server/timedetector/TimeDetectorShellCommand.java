package com.android.server.timedetector;

import android.os.ShellCommand;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
class TimeDetectorShellCommand extends ShellCommand {
    private final TimeDetectorService mInterface;

    /* JADX INFO: Access modifiers changed from: package-private */
    public TimeDetectorShellCommand(TimeDetectorService timeDetectorService) {
        this.mInterface = timeDetectorService;
    }

    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        char c = 65535;
        switch (cmd.hashCode()) {
            case -1316904020:
                if (cmd.equals("is_auto_detection_enabled")) {
                    c = 0;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return runIsAutoDetectionEnabled();
            default:
                return handleDefaultCommands(cmd);
        }
    }

    private int runIsAutoDetectionEnabled() {
        PrintWriter pw = getOutPrintWriter();
        boolean enabled = this.mInterface.getCapabilitiesAndConfig().getTimeConfiguration().isAutoDetectionEnabled();
        pw.println(enabled);
        return 0;
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.printf("Time Detector (%s) commands:\n", "time_detector");
        pw.printf("  help\n", new Object[0]);
        pw.printf("    Print this help text.\n", new Object[0]);
        pw.printf("  %s\n", "is_auto_detection_enabled");
        pw.printf("    Prints true/false according to the automatic time detection setting.\n", new Object[0]);
        pw.println();
        pw.printf("This service is also affected by the following device_config flags in the %s namespace:\n", "system_time");
        pw.printf("  %s\n", ServerFlags.KEY_TIME_DETECTOR_LOWER_BOUND_MILLIS_OVERRIDE);
        pw.printf("    The lower bound used to validate time suggestions when they are received.\n", new Object[0]);
        pw.printf("    Specified in milliseconds since the start of the Unix epoch.\n", new Object[0]);
        pw.printf("  %s\n", ServerFlags.KEY_TIME_DETECTOR_ORIGIN_PRIORITIES_OVERRIDE);
        pw.printf("    A comma separated list of origins. See TimeDetectorStrategy for details.\n", new Object[0]);
        pw.println();
        pw.printf("See \"adb shell cmd device_config\" for more information on setting flags.\n", new Object[0]);
        pw.println();
    }
}
