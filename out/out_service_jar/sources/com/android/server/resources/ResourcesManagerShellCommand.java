package com.android.server.resources;

import android.content.res.IResourcesManager;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.util.Slog;
import java.io.IOException;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
public class ResourcesManagerShellCommand extends ShellCommand {
    private static final String TAG = "ResourcesManagerShellCommand";
    private final IResourcesManager mInterface;

    public ResourcesManagerShellCommand(IResourcesManager anInterface) {
        this.mInterface = anInterface;
    }

    public int onCommand(String cmd) {
        boolean z;
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter err = getErrPrintWriter();
        try {
            switch (cmd.hashCode()) {
                case 3095028:
                    if (cmd.equals("dump")) {
                        z = false;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    return dumpResources();
                default:
                    return handleDefaultCommands(cmd);
            }
        } catch (RemoteException e) {
            err.println("Remote exception: " + e);
            return -1;
        } catch (IllegalArgumentException e2) {
            err.println("Error: " + e2.getMessage());
            return -1;
        }
    }

    private int dumpResources() throws RemoteException {
        String processId = getNextArgRequired();
        try {
            final ConditionVariable lock = new ConditionVariable();
            RemoteCallback finishCallback = new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.server.resources.ResourcesManagerShellCommand$$ExternalSyntheticLambda0
                public final void onResult(Bundle bundle) {
                    lock.open();
                }
            }, (Handler) null);
            if (!this.mInterface.dumpResources(processId, ParcelFileDescriptor.dup(getOutFileDescriptor()), finishCallback)) {
                getErrPrintWriter().println("RESOURCES DUMP FAILED on process " + processId);
                return -1;
            }
            lock.block(5000L);
            return 0;
        } catch (IOException e) {
            Slog.e(TAG, "Exception while dumping resources", e);
            getErrPrintWriter().println("Exception while dumping resources: " + e.getMessage());
            return -1;
        }
    }

    public void onHelp() {
        PrintWriter out = getOutPrintWriter();
        out.println("Resources manager commands:");
        out.println("  help");
        out.println("    Print this help text.");
        out.println("  dump <PROCESS>");
        out.println("    Dump the Resources objects in use as well as the history of Resources");
    }
}
