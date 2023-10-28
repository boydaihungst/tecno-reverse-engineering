package com.android.server.contentcapture;

import android.os.Bundle;
import android.os.ShellCommand;
import android.os.UserHandle;
import com.android.internal.os.IResultReceiver;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
/* loaded from: classes.dex */
public final class ContentCaptureManagerServiceShellCommand extends ShellCommand {
    private final ContentCaptureManagerService mService;

    public ContentCaptureManagerServiceShellCommand(ContentCaptureManagerService service) {
        this.mService = service;
    }

    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        char c = 65535;
        switch (cmd.hashCode()) {
            case 102230:
                if (cmd.equals("get")) {
                    c = 2;
                    break;
                }
                break;
            case 113762:
                if (cmd.equals("set")) {
                    c = 3;
                    break;
                }
                break;
            case 3322014:
                if (cmd.equals("list")) {
                    c = 0;
                    break;
                }
                break;
            case 1557372922:
                if (cmd.equals("destroy")) {
                    c = 1;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return requestList(pw);
            case 1:
                return requestDestroy(pw);
            case 2:
                return requestGet(pw);
            case 3:
                return requestSet(pw);
            default:
                return handleDefaultCommands(cmd);
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        try {
            pw.println("ContentCapture Service (content_capture) commands:");
            pw.println("  help");
            pw.println("    Prints this help text.");
            pw.println("");
            pw.println("  get bind-instant-service-allowed");
            pw.println("    Gets whether binding to services provided by instant apps is allowed");
            pw.println("");
            pw.println("  set bind-instant-service-allowed [true | false]");
            pw.println("    Sets whether binding to services provided by instant apps is allowed");
            pw.println("");
            pw.println("  set temporary-service USER_ID [COMPONENT_NAME DURATION]");
            pw.println("    Temporarily (for DURATION ms) changes the service implemtation.");
            pw.println("    To reset, call with just the USER_ID argument.");
            pw.println("");
            pw.println("  set default-service-enabled USER_ID [true|false]");
            pw.println("    Enable / disable the default service for the user.");
            pw.println("");
            pw.println("  get default-service-enabled USER_ID");
            pw.println("    Checks whether the default service is enabled for the user.");
            pw.println("");
            pw.println("  list sessions [--user USER_ID]");
            pw.println("    Lists all pending sessions.");
            pw.println("");
            pw.println("  destroy sessions [--user USER_ID]");
            pw.println("    Destroys all pending sessions.");
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

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private int requestGet(PrintWriter pw) {
        boolean z;
        String what = getNextArgRequired();
        switch (what.hashCode()) {
            case 529654941:
                if (what.equals("default-service-enabled")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            case 809633044:
                if (what.equals("bind-instant-service-allowed")) {
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
                return getBindInstantService(pw);
            case true:
                return getDefaultServiceEnabled(pw);
            default:
                pw.println("Invalid set: " + what);
                return -1;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private int requestSet(PrintWriter pw) {
        char c;
        String what = getNextArgRequired();
        switch (what.hashCode()) {
            case 529654941:
                if (what.equals("default-service-enabled")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 809633044:
                if (what.equals("bind-instant-service-allowed")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 2003978041:
                if (what.equals("temporary-service")) {
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
                return setBindInstantService(pw);
            case 1:
                return setTemporaryService(pw);
            case 2:
                return setDefaultServiceEnabled(pw);
            default:
                pw.println("Invalid set: " + what);
                return -1;
        }
    }

    private int getBindInstantService(PrintWriter pw) {
        if (this.mService.getAllowInstantService()) {
            pw.println("true");
            return 0;
        }
        pw.println("false");
        return 0;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private int setBindInstantService(PrintWriter pw) {
        boolean z;
        String mode = getNextArgRequired();
        String lowerCase = mode.toLowerCase();
        switch (lowerCase.hashCode()) {
            case 3569038:
                if (lowerCase.equals("true")) {
                    z = false;
                    break;
                }
                z = true;
                break;
            case 97196323:
                if (lowerCase.equals("false")) {
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
                this.mService.setAllowInstantService(true);
                return 0;
            case true:
                this.mService.setAllowInstantService(false);
                return 0;
            default:
                pw.println("Invalid mode: " + mode);
                return -1;
        }
    }

    private int setTemporaryService(PrintWriter pw) {
        int userId = getNextIntArgRequired();
        String serviceName = getNextArg();
        if (serviceName == null) {
            this.mService.resetTemporaryService(userId);
            return 0;
        }
        int duration = getNextIntArgRequired();
        this.mService.setTemporaryService(userId, serviceName, duration);
        pw.println("ContentCaptureService temporarily set to " + serviceName + " for " + duration + "ms");
        return 0;
    }

    private int setDefaultServiceEnabled(PrintWriter pw) {
        int userId = getNextIntArgRequired();
        boolean enabled = Boolean.parseBoolean(getNextArgRequired());
        boolean changed = this.mService.setDefaultServiceEnabled(userId, enabled);
        if (!changed) {
            pw.println("already " + enabled);
            return 0;
        }
        return 0;
    }

    private int getDefaultServiceEnabled(PrintWriter pw) {
        int userId = getNextIntArgRequired();
        boolean enabled = this.mService.isDefaultServiceEnabled(userId);
        pw.println(enabled);
        return 0;
    }

    private int requestDestroy(PrintWriter pw) {
        if (!isNextArgSessions(pw)) {
            return -1;
        }
        final int userId = getUserIdFromArgsOrAllUsers();
        final CountDownLatch latch = new CountDownLatch(1);
        final IResultReceiver.Stub stub = new IResultReceiver.Stub() { // from class: com.android.server.contentcapture.ContentCaptureManagerServiceShellCommand.1
            public void send(int resultCode, Bundle resultData) {
                latch.countDown();
            }
        };
        return requestSessionCommon(pw, latch, new Runnable() { // from class: com.android.server.contentcapture.ContentCaptureManagerServiceShellCommand$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ContentCaptureManagerServiceShellCommand.this.m2904x6f0c88a0(userId, stub);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$requestDestroy$0$com-android-server-contentcapture-ContentCaptureManagerServiceShellCommand  reason: not valid java name */
    public /* synthetic */ void m2904x6f0c88a0(int userId, IResultReceiver receiver) {
        this.mService.destroySessions(userId, receiver);
    }

    private int requestList(final PrintWriter pw) {
        if (!isNextArgSessions(pw)) {
            return -1;
        }
        final int userId = getUserIdFromArgsOrAllUsers();
        final CountDownLatch latch = new CountDownLatch(1);
        final IResultReceiver.Stub stub = new IResultReceiver.Stub() { // from class: com.android.server.contentcapture.ContentCaptureManagerServiceShellCommand.2
            public void send(int resultCode, Bundle resultData) {
                ArrayList<String> sessions = resultData.getStringArrayList("sessions");
                Iterator<String> it = sessions.iterator();
                while (it.hasNext()) {
                    String session = it.next();
                    pw.println(session);
                }
                latch.countDown();
            }
        };
        return requestSessionCommon(pw, latch, new Runnable() { // from class: com.android.server.contentcapture.ContentCaptureManagerServiceShellCommand$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                ContentCaptureManagerServiceShellCommand.this.m2905x4706a577(userId, stub);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$requestList$1$com-android-server-contentcapture-ContentCaptureManagerServiceShellCommand  reason: not valid java name */
    public /* synthetic */ void m2905x4706a577(int userId, IResultReceiver receiver) {
        this.mService.listSessions(userId, receiver);
    }

    private boolean isNextArgSessions(PrintWriter pw) {
        String type = getNextArgRequired();
        if (!type.equals("sessions")) {
            pw.println("Error: invalid list type");
            return false;
        }
        return true;
    }

    private int requestSessionCommon(PrintWriter pw, CountDownLatch latch, Runnable command) {
        command.run();
        return waitForLatch(pw, latch);
    }

    private int waitForLatch(PrintWriter pw, CountDownLatch latch) {
        try {
            boolean received = latch.await(5L, TimeUnit.SECONDS);
            if (!received) {
                pw.println("Timed out after 5 seconds");
                return -1;
            }
            return 0;
        } catch (InterruptedException e) {
            pw.println("System call interrupted");
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    private int getUserIdFromArgsOrAllUsers() {
        if ("--user".equals(getNextArg())) {
            return UserHandle.parseUserArg(getNextArgRequired());
        }
        return -1;
    }

    private int getNextIntArgRequired() {
        return Integer.parseInt(getNextArgRequired());
    }
}
