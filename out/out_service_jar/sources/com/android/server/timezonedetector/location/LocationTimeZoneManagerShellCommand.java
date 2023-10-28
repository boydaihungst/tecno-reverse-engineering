package com.android.server.timezonedetector.location;

import android.os.ShellCommand;
import android.util.IndentingPrintWriter;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.server.timedetector.ServerFlags;
import com.android.server.timezonedetector.GeolocationTimeZoneSuggestion;
import com.android.server.timezonedetector.ServiceConfigAccessor;
import com.android.server.timezonedetector.location.LocationTimeZoneProvider;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;
/* loaded from: classes2.dex */
class LocationTimeZoneManagerShellCommand extends ShellCommand {
    private final LocationTimeZoneManagerService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public LocationTimeZoneManagerShellCommand(LocationTimeZoneManagerService service) {
        this.mService = service;
    }

    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        char c = 65535;
        switch (cmd.hashCode()) {
            case -385184143:
                if (cmd.equals("start_with_test_providers")) {
                    c = 1;
                    break;
                }
                break;
            case 3540994:
                if (cmd.equals("stop")) {
                    c = 2;
                    break;
                }
                break;
            case 109757538:
                if (cmd.equals("start")) {
                    c = 0;
                    break;
                }
                break;
            case 248094771:
                if (cmd.equals("clear_recorded_provider_states")) {
                    c = 3;
                    break;
                }
                break;
            case 943200902:
                if (cmd.equals("dump_state")) {
                    c = 4;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return runStart();
            case 1:
                return runStartWithTestProviders();
            case 2:
                return runStop();
            case 3:
                return runClearRecordedProviderStates();
            case 4:
                return runDumpControllerState();
            default:
                return handleDefaultCommands(cmd);
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.printf("Location Time Zone Manager (%s) commands for tests:\n", "location_time_zone_manager");
        pw.printf("  help\n", new Object[0]);
        pw.printf("    Print this help text.\n", new Object[0]);
        pw.printf("  %s\n", "start");
        pw.printf("    Starts the service, creating location time zone providers.\n", new Object[0]);
        pw.printf("  %s <primary package name|%2$s> <secondary package name|%2$s> <record states>\n", "start_with_test_providers", "@null");
        pw.printf("    Starts the service with test provider packages configured / provider permission checks disabled.\n", new Object[0]);
        pw.printf("    <record states> - true|false, determines whether state recording is enabled.\n", new Object[0]);
        pw.printf("    See %s and %s.\n", "dump_state", "clear_recorded_provider_states");
        pw.printf("  %s\n", "stop");
        pw.printf("    Stops the service, destroying location time zone providers.\n", new Object[0]);
        pw.printf("  %s\n", "clear_recorded_provider_states");
        pw.printf("    Clears recorded provider state. See also %s and %s.\n", "start_with_test_providers", "dump_state");
        pw.printf("    Note: This is only intended for use during testing.\n", new Object[0]);
        pw.printf("  %s [%s]\n", "dump_state", "--proto");
        pw.printf("    Dumps service state for tests as text or binary proto form.\n", new Object[0]);
        pw.printf("    See the LocationTimeZoneManagerServiceStateProto definition for details.\n", new Object[0]);
        pw.println();
        pw.printf("This service is also affected by the following device_config flags in the %s namespace:\n", "system_time");
        pw.printf("  %s\n", ServerFlags.KEY_PRIMARY_LTZP_MODE_OVERRIDE);
        pw.printf("    Overrides the mode of the primary provider. Values=%s|%s\n", ServiceConfigAccessor.PROVIDER_MODE_DISABLED, ServiceConfigAccessor.PROVIDER_MODE_ENABLED);
        pw.printf("  %s\n", ServerFlags.KEY_SECONDARY_LTZP_MODE_OVERRIDE);
        pw.printf("    Overrides the mode of the secondary provider. Values=%s|%s\n", ServiceConfigAccessor.PROVIDER_MODE_DISABLED, ServiceConfigAccessor.PROVIDER_MODE_ENABLED);
        pw.printf("  %s\n", ServerFlags.KEY_LOCATION_TIME_ZONE_DETECTION_UNCERTAINTY_DELAY_MILLIS);
        pw.printf("    Sets the amount of time the service waits when uncertain before making an 'uncertain' suggestion to the time zone detector.\n", new Object[0]);
        pw.printf("  %s\n", ServerFlags.KEY_LTZP_INITIALIZATION_TIMEOUT_MILLIS);
        pw.printf("    Sets the initialization time passed to the providers.\n", new Object[0]);
        pw.printf("  %s\n", ServerFlags.KEY_LTZP_INITIALIZATION_TIMEOUT_FUZZ_MILLIS);
        pw.printf("    Sets the amount of extra time added to the providers' initialization time.\n", new Object[0]);
        pw.printf("  %s\n", ServerFlags.KEY_LTZP_EVENT_FILTERING_AGE_THRESHOLD_MILLIS);
        pw.printf("    Sets the amount of time that must pass between equivalent LTZP events before they will be reported to the system server.\n", new Object[0]);
        pw.println();
        pw.printf("Typically, use '%s' to stop the service before setting individual flags and '%s' after to restart it.\n", "stop", "start");
        pw.println();
        pw.printf("See \"adb shell cmd device_config\" for more information on setting flags.\n", new Object[0]);
        pw.println();
        pw.printf("Also see \"adb shell cmd %s help\" for higher-level location time zone commands / settings.\n", "time_zone_detector");
        pw.println();
    }

    private int runStart() {
        try {
            this.mService.start();
            PrintWriter outPrintWriter = getOutPrintWriter();
            outPrintWriter.println("Service started");
            return 0;
        } catch (RuntimeException e) {
            reportError(e);
            return 1;
        }
    }

    private int runStartWithTestProviders() {
        String testPrimaryProviderPackageName = parseProviderPackageName(getNextArgRequired());
        String testSecondaryProviderPackageName = parseProviderPackageName(getNextArgRequired());
        boolean recordProviderStateChanges = Boolean.parseBoolean(getNextArgRequired());
        try {
            this.mService.startWithTestProviders(testPrimaryProviderPackageName, testSecondaryProviderPackageName, recordProviderStateChanges);
            PrintWriter outPrintWriter = getOutPrintWriter();
            outPrintWriter.println("Service started (test mode)");
            return 0;
        } catch (RuntimeException e) {
            reportError(e);
            return 1;
        }
    }

    private int runStop() {
        try {
            this.mService.stop();
            PrintWriter outPrintWriter = getOutPrintWriter();
            outPrintWriter.println("Service stopped");
            return 0;
        } catch (RuntimeException e) {
            reportError(e);
            return 1;
        }
    }

    private int runClearRecordedProviderStates() {
        try {
            this.mService.clearRecordedProviderStates();
            return 0;
        } catch (IllegalStateException e) {
            reportError(e);
            return 2;
        }
    }

    private int runDumpControllerState() {
        DualDumpOutputStream outputStream;
        try {
            LocationTimeZoneManagerServiceState state = this.mService.getStateForTests();
            if (state == null) {
                return 0;
            }
            boolean useProto = Objects.equals("--proto", getNextOption());
            if (useProto) {
                FileDescriptor outFd = getOutFileDescriptor();
                outputStream = new DualDumpOutputStream(new ProtoOutputStream(outFd));
            } else {
                outputStream = new DualDumpOutputStream(new IndentingPrintWriter(getOutPrintWriter(), "  "));
            }
            if (state.getLastSuggestion() != null) {
                GeolocationTimeZoneSuggestion lastSuggestion = state.getLastSuggestion();
                long lastSuggestionToken = outputStream.start("last_suggestion", 1146756268033L);
                for (String zoneId : lastSuggestion.getZoneIds()) {
                    outputStream.write("zone_ids", 2237677961217L, zoneId);
                }
                for (String debugInfo : lastSuggestion.getDebugInfo()) {
                    outputStream.write("debug_info", 2237677961218L, debugInfo);
                }
                outputStream.end(lastSuggestionToken);
            }
            writeControllerStates(outputStream, state.getControllerStates());
            writeProviderStates(outputStream, state.getPrimaryProviderStates(), "primary_provider_states", 2246267895810L);
            writeProviderStates(outputStream, state.getSecondaryProviderStates(), "secondary_provider_states", 2246267895811L);
            outputStream.flush();
            return 0;
        } catch (RuntimeException e) {
            reportError(e);
            return 1;
        }
    }

    private static void writeControllerStates(DualDumpOutputStream outputStream, List<String> states) {
        for (String state : states) {
            outputStream.write("controller_states", 2259152797700L, convertControllerStateToProtoEnum(state));
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private static int convertControllerStateToProtoEnum(String state) {
        char c;
        switch (state.hashCode()) {
            case -1166336595:
                if (state.equals("STOPPED")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case -468307734:
                if (state.equals("PROVIDERS_INITIALIZING")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 433141802:
                if (state.equals("UNKNOWN")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case 478389753:
                if (state.equals("DESTROYED")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case 872357833:
                if (state.equals("UNCERTAIN")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 1386911874:
                if (state.equals("CERTAIN")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 1917201485:
                if (state.equals("INITIALIZING")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 2066319421:
                if (state.equals("FAILED")) {
                    c = 5;
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
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 4;
            case 4:
                return 5;
            case 5:
                return 6;
            case 6:
                return 7;
            default:
                return 0;
        }
    }

    private static void writeProviderStates(DualDumpOutputStream outputStream, List<LocationTimeZoneProvider.ProviderState> providerStates, String fieldName, long fieldId) {
        for (LocationTimeZoneProvider.ProviderState providerState : providerStates) {
            long providerStateToken = outputStream.start(fieldName, fieldId);
            outputStream.write("state", 1159641169921L, convertProviderStateEnumToProtoEnum(providerState.stateEnum));
            outputStream.end(providerStateToken);
        }
    }

    private static int convertProviderStateEnumToProtoEnum(int stateEnum) {
        switch (stateEnum) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            default:
                throw new IllegalArgumentException("Unknown stateEnum=" + stateEnum);
        }
    }

    private void reportError(Throwable e) {
        PrintWriter errPrintWriter = getErrPrintWriter();
        errPrintWriter.println("Error: ");
        e.printStackTrace(errPrintWriter);
    }

    private static String parseProviderPackageName(String providerPackageNameString) {
        if (providerPackageNameString.equals("@null")) {
            return null;
        }
        return providerPackageNameString;
    }
}
