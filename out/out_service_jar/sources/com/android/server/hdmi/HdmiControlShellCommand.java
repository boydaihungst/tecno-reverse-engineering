package com.android.server.hdmi;

import android.hardware.hdmi.IHdmiControlCallback;
import android.hardware.hdmi.IHdmiControlService;
import android.net.util.NetworkConstants;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.util.Slog;
import java.io.PrintWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
/* loaded from: classes.dex */
final class HdmiControlShellCommand extends ShellCommand {
    private static final String TAG = "HdmiShellCommand";
    private final IHdmiControlService.Stub mBinderService;
    final CountDownLatch mLatch = new CountDownLatch(1);
    AtomicInteger mCecResult = new AtomicInteger();
    IHdmiControlCallback.Stub mHdmiControlCallback = new IHdmiControlCallback.Stub() { // from class: com.android.server.hdmi.HdmiControlShellCommand.1
        public void onComplete(int result) {
            HdmiControlShellCommand.this.getOutPrintWriter().println(" done (" + HdmiControlShellCommand.this.getResultString(result) + ")");
            HdmiControlShellCommand.this.mCecResult.set(result);
            HdmiControlShellCommand.this.mLatch.countDown();
        }
    };

    /* JADX INFO: Access modifiers changed from: package-private */
    public HdmiControlShellCommand(IHdmiControlService.Stub binderService) {
        this.mBinderService = binderService;
    }

    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        try {
            return handleShellCommand(cmd);
        } catch (Exception e) {
            getErrPrintWriter().println("Caught error for command '" + cmd + "': " + e.getMessage());
            Slog.e(TAG, "Error handling hdmi_control shell command: " + cmd, e);
            return 1;
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("HdmiControlManager (hdmi_control) commands:");
        pw.println("  help");
        pw.println("      Print this help text.");
        pw.println("  onetouchplay, otp");
        pw.println("      Send the \"One Touch Play\" feature from a source to the TV");
        pw.println("  vendorcommand --device_type <originating device type>");
        pw.println("                --destination <destination device>");
        pw.println("                --args <vendor specific arguments>");
        pw.println("                [--id <true if vendor command should be sent with vendor id>]");
        pw.println("      Send a Vendor Command to the given target device");
        pw.println("  cec_setting get <setting name>");
        pw.println("      Get the current value of a CEC setting");
        pw.println("  cec_setting set <setting name> <value>");
        pw.println("      Set the value of a CEC setting");
        pw.println("  setsystemaudiomode, setsam [on|off]");
        pw.println("      Sets the System Audio Mode feature on or off on TV devices");
        pw.println("  setarc [on|off]");
        pw.println("      Sets the ARC feature on or off on TV devices");
        pw.println("  deviceselect <device id>");
        pw.println("      Switch to device with given id");
        pw.println("      The device's id is represented by its logical address.");
        pw.println("  history_size get");
        pw.println("      Gets the number of messages that can be stored in dumpsys history");
        pw.println("  history_size set <new_size>");
        pw.println("      Changes the number of messages that can be stored in dumpsys history to new_size");
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private int handleShellCommand(String cmd) throws RemoteException {
        char c;
        PrintWriter pw = getOutPrintWriter();
        switch (cmd.hashCode()) {
            case -1962118964:
                if (cmd.equals("history_size")) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case -956246195:
                if (cmd.equals("onetouchplay")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case -905786704:
                if (cmd.equals("setarc")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case -905769923:
                if (cmd.equals("setsam")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case -467124088:
                if (cmd.equals("setsystemaudiomode")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case -25969966:
                if (cmd.equals("deviceselect")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case 110379:
                if (cmd.equals("otp")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 621295875:
                if (cmd.equals("vendorcommand")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 1322280018:
                if (cmd.equals("cec_setting")) {
                    c = 3;
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
                return oneTouchPlay(pw);
            case 2:
                return vendorCommand(pw);
            case 3:
                return cecSetting(pw);
            case 4:
            case 5:
                return setSystemAudioMode(pw);
            case 6:
                return setArcMode(pw);
            case 7:
                return deviceSelect(pw);
            case '\b':
                return historySize(pw);
            default:
                getErrPrintWriter().println("Unhandled command: " + cmd);
                return 1;
        }
    }

    private int deviceSelect(PrintWriter pw) throws RemoteException {
        if (getRemainingArgsCount() != 1) {
            throw new IllegalArgumentException("Expected exactly 1 argument.");
        }
        int deviceId = Integer.parseInt(getNextArg());
        pw.print("Sending Device Select...");
        this.mBinderService.deviceSelect(deviceId, this.mHdmiControlCallback);
        return (receiveCallback("Device Select") && this.mCecResult.get() == 0) ? 0 : 1;
    }

    private int oneTouchPlay(PrintWriter pw) throws RemoteException {
        pw.print("Sending One Touch Play...");
        this.mBinderService.oneTouchPlay(this.mHdmiControlCallback);
        return (receiveCallback("One Touch Play") && this.mCecResult.get() == 0) ? 0 : 1;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:17:0x0036, code lost:
        if (r5.equals("-t") != false) goto L11;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int vendorCommand(PrintWriter pw) throws RemoteException {
        if (6 > getRemainingArgsCount()) {
            throw new IllegalArgumentException("Expected 3 arguments.");
        }
        int deviceType = -1;
        int destination = -1;
        String parameters = "";
        boolean hasVendorId = false;
        String arg = getNextOption();
        while (true) {
            char c = 0;
            if (arg != null) {
                switch (arg.hashCode()) {
                    case -347347485:
                        if (arg.equals("--device_type")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    case -234325394:
                        if (arg.equals("--destination")) {
                            c = 3;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1492:
                        if (arg.equals("-a")) {
                            c = 4;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1495:
                        if (arg.equals("-d")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case NetworkConstants.ETHER_MTU /* 1500 */:
                        if (arg.equals("-i")) {
                            c = 6;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1511:
                        break;
                    case 1387195:
                        if (arg.equals("--id")) {
                            c = 7;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1332872829:
                        if (arg.equals("--args")) {
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
                    case 1:
                        deviceType = Integer.parseInt(getNextArgRequired());
                        break;
                    case 2:
                    case 3:
                        destination = Integer.parseInt(getNextArgRequired());
                        break;
                    case 4:
                    case 5:
                        parameters = getNextArgRequired();
                        break;
                    case 6:
                    case 7:
                        hasVendorId = Boolean.parseBoolean(getNextArgRequired());
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown argument: " + arg);
                }
                arg = getNextArg();
            } else {
                String[] parts = parameters.split(":");
                byte[] params = new byte[parts.length];
                for (int i = 0; i < params.length; i++) {
                    params[i] = (byte) Integer.parseInt(parts[i], 16);
                }
                pw.println("Sending <Vendor Command>");
                this.mBinderService.sendVendorCommand(deviceType, destination, params, hasVendorId);
                return 0;
            }
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:8:0x001c, code lost:
        if (r0.equals("set") != false) goto L7;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int cecSetting(PrintWriter pw) throws RemoteException {
        boolean z = true;
        if (getRemainingArgsCount() < 1) {
            throw new IllegalArgumentException("Expected at least 1 argument (operation).");
        }
        String operation = getNextArgRequired();
        switch (operation.hashCode()) {
            case 102230:
                if (operation.equals("get")) {
                    z = false;
                    break;
                }
                z = true;
                break;
            case 113762:
                break;
            default:
                z = true;
                break;
        }
        switch (z) {
            case false:
                String setting = getNextArgRequired();
                try {
                    pw.println(setting + " = " + this.mBinderService.getCecSettingStringValue(setting));
                } catch (IllegalArgumentException e) {
                    pw.println(setting + " = " + this.mBinderService.getCecSettingIntValue(setting));
                }
                return 0;
            case true:
                String setting2 = getNextArgRequired();
                String value = getNextArgRequired();
                try {
                    this.mBinderService.setCecSettingStringValue(setting2, value);
                    pw.println(setting2 + " = " + value);
                } catch (IllegalArgumentException e2) {
                    int intValue = Integer.parseInt(value);
                    this.mBinderService.setCecSettingIntValue(setting2, intValue);
                    pw.println(setting2 + " = " + intValue);
                }
                return 0;
            default:
                throw new IllegalArgumentException("Unknown operation: " + operation);
        }
    }

    private int setSystemAudioMode(PrintWriter pw) throws RemoteException {
        if (1 > getRemainingArgsCount()) {
            throw new IllegalArgumentException("Please indicate if System Audio Mode should be turned \"on\" or \"off\".");
        }
        String arg = getNextArg();
        if (arg.equals("on")) {
            pw.println("Setting System Audio Mode on");
            this.mBinderService.setSystemAudioMode(true, this.mHdmiControlCallback);
        } else if (arg.equals("off")) {
            pw.println("Setting System Audio Mode off");
            this.mBinderService.setSystemAudioMode(false, this.mHdmiControlCallback);
        } else {
            throw new IllegalArgumentException("Please indicate if System Audio Mode should be turned \"on\" or \"off\".");
        }
        return (receiveCallback("Set System Audio Mode") && this.mCecResult.get() == 0) ? 0 : 1;
    }

    private int setArcMode(PrintWriter pw) throws RemoteException {
        if (1 > getRemainingArgsCount()) {
            throw new IllegalArgumentException("Please indicate if ARC mode should be turned \"on\" or \"off\".");
        }
        String arg = getNextArg();
        if (arg.equals("on")) {
            pw.println("Setting ARC mode on");
            this.mBinderService.setArcMode(true);
        } else if (arg.equals("off")) {
            pw.println("Setting ARC mode off");
            this.mBinderService.setArcMode(false);
        } else {
            throw new IllegalArgumentException("Please indicate if ARC mode should be turned \"on\" or \"off\".");
        }
        return 0;
    }

    private int historySize(PrintWriter pw) throws RemoteException {
        if (1 > getRemainingArgsCount()) {
            throw new IllegalArgumentException("Use 'set' or 'get' for the command action");
        }
        String operation = getNextArgRequired();
        char c = 65535;
        switch (operation.hashCode()) {
            case 102230:
                if (operation.equals("get")) {
                    c = 0;
                    break;
                }
                break;
            case 113762:
                if (operation.equals("set")) {
                    c = 1;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                pw.println("CEC dumpsys message history size = " + this.mBinderService.getMessageHistorySize());
                return 0;
            case 1:
                String arg = getNextArgRequired();
                try {
                    int value = Integer.parseInt(arg);
                    if (this.mBinderService.setMessageHistorySize(value)) {
                        pw.println("Setting CEC dumpsys message history size to " + value);
                    } else {
                        pw.println("Message history size not changed, was it lower than the minimum size?");
                    }
                    return 0;
                } catch (NumberFormatException e) {
                    pw.println("Cannot set CEC dumpsys message history size to " + arg);
                    return 1;
                }
            default:
                throw new IllegalArgumentException("Unknown operation: " + operation);
        }
    }

    private boolean receiveCallback(String command) {
        try {
            if (!this.mLatch.await(2000L, TimeUnit.MILLISECONDS)) {
                getErrPrintWriter().println(command + " timed out.");
                return false;
            }
            return true;
        } catch (InterruptedException e) {
            getErrPrintWriter().println("Caught InterruptedException");
            Thread.currentThread().interrupt();
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getResultString(int result) {
        switch (result) {
            case 0:
                return "Success";
            case 1:
                return "Timeout";
            case 2:
                return "Source not available";
            case 3:
                return "Target not available";
            case 4:
            default:
                return Integer.toString(result);
            case 5:
                return "Exception";
            case 6:
                return "Incorrect mode";
            case 7:
                return "Communication Failed";
        }
    }
}
