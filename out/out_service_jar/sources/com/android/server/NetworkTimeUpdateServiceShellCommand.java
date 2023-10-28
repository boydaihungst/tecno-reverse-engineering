package com.android.server;

import android.os.ShellCommand;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.Objects;
/* loaded from: classes.dex */
class NetworkTimeUpdateServiceShellCommand extends ShellCommand {
    private static final String SET_SERVER_CONFIG_HOSTNAME_ARG = "--hostname";
    private static final String SET_SERVER_CONFIG_PORT_ARG = "--port";
    private static final String SET_SERVER_CONFIG_TIMEOUT_ARG = "--timeout_millis";
    private static final String SHELL_COMMAND_CLEAR_TIME = "clear_time";
    private static final String SHELL_COMMAND_FORCE_REFRESH = "force_refresh";
    private static final String SHELL_COMMAND_SERVICE_NAME = "network_time_update_service";
    private static final String SHELL_COMMAND_SET_SERVER_CONFIG = "set_server_config";
    private final NetworkTimeUpdateService mNetworkTimeUpdateService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public NetworkTimeUpdateServiceShellCommand(NetworkTimeUpdateService networkTimeUpdateService) {
        this.mNetworkTimeUpdateService = (NetworkTimeUpdateService) Objects.requireNonNull(networkTimeUpdateService);
    }

    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        char c = 65535;
        switch (cmd.hashCode()) {
            case -732807809:
                if (cmd.equals(SHELL_COMMAND_CLEAR_TIME)) {
                    c = 0;
                    break;
                }
                break;
            case -349279583:
                if (cmd.equals(SHELL_COMMAND_SET_SERVER_CONFIG)) {
                    c = 2;
                    break;
                }
                break;
            case 1891346823:
                if (cmd.equals(SHELL_COMMAND_FORCE_REFRESH)) {
                    c = 1;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return runClearTime();
            case 1:
                return runForceRefresh();
            case 2:
                return runSetServerConfig();
            default:
                return handleDefaultCommands(cmd);
        }
    }

    private int runClearTime() {
        this.mNetworkTimeUpdateService.clearTimeForTests();
        return 0;
    }

    private int runForceRefresh() {
        boolean success = this.mNetworkTimeUpdateService.forceRefreshForTests();
        getOutPrintWriter().println(success);
        return 0;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:12:0x0024, code lost:
        if (r3.equals(com.android.server.NetworkTimeUpdateServiceShellCommand.SET_SERVER_CONFIG_HOSTNAME_ARG) != false) goto L9;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int runSetServerConfig() {
        String hostname = null;
        Integer port = null;
        Duration timeout = null;
        while (true) {
            String opt = getNextArg();
            char c = 0;
            if (opt != null) {
                switch (opt.hashCode()) {
                    case -975021948:
                        if (opt.equals(SET_SERVER_CONFIG_TIMEOUT_ARG)) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case -284048941:
                        break;
                    case 1333317153:
                        if (opt.equals(SET_SERVER_CONFIG_PORT_ARG)) {
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
                        hostname = getNextArgRequired();
                        break;
                    case 1:
                        port = Integer.valueOf(Integer.parseInt(getNextArgRequired()));
                        break;
                    case 2:
                        timeout = Duration.ofMillis(Integer.parseInt(getNextArgRequired()));
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown option: " + opt);
                }
            } else {
                this.mNetworkTimeUpdateService.setServerConfigForTests(hostname, port, timeout);
                return 0;
            }
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.printf("Network Time Update Service (%s) commands:\n", SHELL_COMMAND_SERVICE_NAME);
        pw.printf("  help\n", new Object[0]);
        pw.printf("    Print this help text.\n", new Object[0]);
        pw.printf("  %s\n", SHELL_COMMAND_CLEAR_TIME);
        pw.printf("    Clears the latest time.\n", new Object[0]);
        pw.printf("  %s\n", SHELL_COMMAND_FORCE_REFRESH);
        pw.printf("    Refreshes the latest time. Prints whether it was successful.\n", new Object[0]);
        pw.printf("  %s\n", SHELL_COMMAND_SET_SERVER_CONFIG);
        pw.printf("    Sets the NTP server config for tests. The config is not persisted.\n", new Object[0]);
        pw.printf("      Options: [%s <hostname>] [%s <port>] [%s <millis>]\n", SET_SERVER_CONFIG_HOSTNAME_ARG, SET_SERVER_CONFIG_PORT_ARG, SET_SERVER_CONFIG_TIMEOUT_ARG);
        pw.printf("      Each key/value is optional and must be specified to override the\n", new Object[0]);
        pw.printf("      normal value, not specifying a key causes it to reset to the original.\n", new Object[0]);
        pw.println();
    }
}
