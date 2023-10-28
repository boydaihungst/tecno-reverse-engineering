package com.android.server.voiceinteraction;

import android.os.Bundle;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.util.Slog;
import com.android.internal.app.IVoiceInteractionSessionShowCallback;
import com.android.server.voiceinteraction.VoiceInteractionManagerService;
import java.io.PrintWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
/* loaded from: classes2.dex */
final class VoiceInteractionManagerServiceShellCommand extends ShellCommand {
    private static final String TAG = "VoiceInteractionManager";
    private static final long TIMEOUT_MS = 5000;
    private final VoiceInteractionManagerService.VoiceInteractionManagerServiceStub mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public VoiceInteractionManagerServiceShellCommand(VoiceInteractionManagerService.VoiceInteractionManagerServiceStub service) {
        this.mService = service;
    }

    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        char c = 65535;
        switch (cmd.hashCode()) {
            case -1097066044:
                if (cmd.equals("set-debug-hotword-logging")) {
                    c = 4;
                    break;
                }
                break;
            case 3202370:
                if (cmd.equals("hide")) {
                    c = 1;
                    break;
                }
                break;
            case 3529469:
                if (cmd.equals("show")) {
                    c = 0;
                    break;
                }
                break;
            case 1671308008:
                if (cmd.equals("disable")) {
                    c = 2;
                    break;
                }
                break;
            case 1718895687:
                if (cmd.equals("restart-detection")) {
                    c = 3;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return requestShow(pw);
            case 1:
                return requestHide(pw);
            case 2:
                return requestDisable(pw);
            case 3:
                return requestRestartDetection(pw);
            case 4:
                return setDebugHotwordLogging(pw);
            default:
                return handleDefaultCommands(cmd);
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        try {
            pw.println("VoiceInteraction Service (voiceinteraction) commands:");
            pw.println("  help");
            pw.println("    Prints this help text.");
            pw.println("");
            pw.println("  show");
            pw.println("    Shows a session for the active service");
            pw.println("");
            pw.println("  hide");
            pw.println("    Hides the current session");
            pw.println("");
            pw.println("  disable [true|false]");
            pw.println("    Temporarily disable (when true) service");
            pw.println("");
            pw.println("  restart-detection");
            pw.println("    Force a restart of a hotword detection service");
            pw.println("");
            pw.println("  set-debug-hotword-logging [true|false]");
            pw.println("    Temporarily enable or disable debug logging for hotword result.");
            pw.println("    The debug logging will be reset after one hour from last enable.");
            pw.println("");
            if (pw != null) {
                pw.close();
            }
        } catch (Throwable th) {
            if (pw != null) {
                try {
                    pw.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private int requestShow(final PrintWriter pw) {
        Slog.i(TAG, "requestShow()");
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger result = new AtomicInteger();
        IVoiceInteractionSessionShowCallback callback = new IVoiceInteractionSessionShowCallback.Stub() { // from class: com.android.server.voiceinteraction.VoiceInteractionManagerServiceShellCommand.1
            public void onFailed() throws RemoteException {
                Slog.w(VoiceInteractionManagerServiceShellCommand.TAG, "onFailed()");
                pw.println("callback failed");
                result.set(1);
                latch.countDown();
            }

            public void onShown() throws RemoteException {
                Slog.d(VoiceInteractionManagerServiceShellCommand.TAG, "onShown()");
                result.set(0);
                latch.countDown();
            }
        };
        try {
            Bundle args = new Bundle();
            boolean ok = this.mService.showSessionForActiveService(args, 0, callback, null);
            if (!ok) {
                pw.println("showSessionForActiveService() returned false");
                return 1;
            } else if (latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                return 0;
            } else {
                pw.printf("Callback not called in %d ms\n", Long.valueOf((long) TIMEOUT_MS));
                return 1;
            }
        } catch (Exception e) {
            return handleError(pw, "showSessionForActiveService()", e);
        }
    }

    private int requestHide(PrintWriter pw) {
        Slog.i(TAG, "requestHide()");
        try {
            this.mService.hideCurrentSession();
            return 0;
        } catch (Exception e) {
            return handleError(pw, "requestHide()", e);
        }
    }

    private int requestDisable(PrintWriter pw) {
        boolean disabled = Boolean.parseBoolean(getNextArgRequired());
        Slog.i(TAG, "requestDisable(): " + disabled);
        try {
            this.mService.setDisabled(disabled);
            return 0;
        } catch (Exception e) {
            return handleError(pw, "requestDisable()", e);
        }
    }

    private int requestRestartDetection(PrintWriter pw) {
        Slog.i(TAG, "requestRestartDetection()");
        try {
            this.mService.forceRestartHotwordDetector();
            return 0;
        } catch (Exception e) {
            return handleError(pw, "requestRestartDetection()", e);
        }
    }

    private int setDebugHotwordLogging(PrintWriter pw) {
        boolean logging = Boolean.parseBoolean(getNextArgRequired());
        Slog.i(TAG, "setDebugHotwordLogging(): " + logging);
        try {
            this.mService.setDebugHotwordLogging(logging);
            return 0;
        } catch (Exception e) {
            return handleError(pw, "setDebugHotwordLogging()", e);
        }
    }

    private static int handleError(PrintWriter pw, String message, Exception e) {
        Slog.e(TAG, "error calling " + message, e);
        pw.printf("Error calling %s: %s\n", message, e);
        return 1;
    }
}
