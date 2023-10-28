package com.android.server.devicestate;

import android.content.Context;
import android.hardware.devicestate.DeviceStateManager;
import android.hardware.devicestate.DeviceStateRequest;
import android.os.Binder;
import android.os.ShellCommand;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
/* loaded from: classes.dex */
public class DeviceStateManagerShellCommand extends ShellCommand {
    private static DeviceStateRequest sLastRequest;
    private final DeviceStateManager mClient;
    private final DeviceStateManagerService mService;

    public DeviceStateManagerShellCommand(DeviceStateManagerService service) {
        this.mService = service;
        this.mClient = (DeviceStateManager) service.getContext().getSystemService(DeviceStateManager.class);
    }

    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        char c = 65535;
        switch (cmd.hashCode()) {
            case -1422060175:
                if (cmd.equals("print-state")) {
                    c = 1;
                    break;
                }
                break;
            case -1134192350:
                if (cmd.equals("print-states")) {
                    c = 2;
                    break;
                }
                break;
            case -295380803:
                if (cmd.equals("print-states-simple")) {
                    c = 3;
                    break;
                }
                break;
            case 109757585:
                if (cmd.equals("state")) {
                    c = 0;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return runState(pw);
            case 1:
                return runPrintState(pw);
            case 2:
                return runPrintStates(pw);
            case 3:
                return runPrintStatesSimple(pw);
            default:
                return handleDefaultCommands(cmd);
        }
    }

    private void printAllStates(PrintWriter pw) {
        Optional<DeviceState> committedState = this.mService.getCommittedState();
        Optional<DeviceState> baseState = this.mService.getBaseState();
        Optional<DeviceState> overrideState = this.mService.getOverrideState();
        pw.println("Committed state: " + toString(committedState));
        if (overrideState.isPresent()) {
            pw.println("----------------------");
            pw.println("Base state: " + toString(baseState));
            pw.println("Override state: " + overrideState.get());
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [124=5] */
    private int runState(PrintWriter pw) {
        String nextArg = getNextArg();
        if (nextArg == null) {
            printAllStates(pw);
            return 0;
        }
        Context context = this.mService.getContext();
        context.enforceCallingOrSelfPermission("android.permission.CONTROL_DEVICE_STATE", "Permission required to request device state.");
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            if (!"reset".equals(nextArg)) {
                int requestedState = Integer.parseInt(nextArg);
                DeviceStateRequest request = DeviceStateRequest.newBuilder(requestedState).build();
                this.mClient.requestState(request, (Executor) null, (DeviceStateRequest.Callback) null);
                sLastRequest = request;
            } else if (sLastRequest != null) {
                this.mClient.cancelStateRequest();
                sLastRequest = null;
            }
            return 0;
        } catch (NumberFormatException e) {
            getErrPrintWriter().println("Error: requested state should be an integer");
            return -1;
        } catch (IllegalArgumentException e2) {
            getErrPrintWriter().println("Error: " + e2.getMessage());
            getErrPrintWriter().println("-------------------");
            getErrPrintWriter().println("Run:");
            getErrPrintWriter().println("");
            getErrPrintWriter().println("    print-states");
            getErrPrintWriter().println("");
            getErrPrintWriter().println("to get the list of currently supported device states");
            return -1;
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    private int runPrintState(PrintWriter pw) {
        Optional<DeviceState> deviceState = this.mService.getCommittedState();
        if (deviceState.isPresent()) {
            pw.println(deviceState.get().getIdentifier());
            return 0;
        }
        getErrPrintWriter().println("Error: device state not available.");
        return 1;
    }

    private int runPrintStates(PrintWriter pw) {
        DeviceState[] states = this.mService.getSupportedStates();
        pw.print("Supported states: [\n");
        for (int i = 0; i < states.length; i++) {
            pw.print("  " + states[i] + ",\n");
        }
        pw.println("]");
        return 0;
    }

    private int runPrintStatesSimple(PrintWriter pw) {
        pw.print((String) Arrays.stream(this.mService.getSupportedStates()).map(new Function() { // from class: com.android.server.devicestate.DeviceStateManagerShellCommand$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return Integer.valueOf(((DeviceState) obj).getIdentifier());
            }
        }).map(new Function() { // from class: com.android.server.devicestate.DeviceStateManagerShellCommand$$ExternalSyntheticLambda1
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((Integer) obj).toString();
            }
        }).collect(Collectors.joining(",")));
        return 0;
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Device state manager (device_state) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("  state [reset|OVERRIDE_DEVICE_STATE]");
        pw.println("    Return or override device state.");
        pw.println("  print-state");
        pw.println("    Return the current device state.");
        pw.println("  print-states");
        pw.println("    Return list of currently supported device states.");
        pw.println("  print-states-simple");
        pw.println("    Return the currently supported device states in comma separated format.");
    }

    private static String toString(Optional<DeviceState> state) {
        return state.isPresent() ? state.get().toString() : "(none)";
    }
}
