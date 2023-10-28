package com.android.server.location.contexthub;

import android.content.Context;
import android.os.ShellCommand;
import java.io.PrintWriter;
/* loaded from: classes.dex */
public class ContextHubShellCommand extends ShellCommand {
    private final Context mContext;
    private final ContextHubService mInternal;

    public ContextHubShellCommand(Context context, ContextHubService service) {
        this.mInternal = service;
        this.mContext = context;
    }

    public int onCommand(String cmd) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_CONTEXT_HUB", "ContextHubShellCommand");
        if ("deny".equals(cmd)) {
            return runDisableAuth();
        }
        return handleDefaultCommands(cmd);
    }

    private int runDisableAuth() {
        int contextHubId = Integer.decode(getNextArgRequired()).intValue();
        String packageName = getNextArgRequired();
        long nanoAppId = Long.decode(getNextArgRequired()).longValue();
        this.mInternal.denyClientAuthState(contextHubId, packageName, nanoAppId);
        return 0;
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("ContextHub commands:");
        pw.println("  help");
        pw.println("      Print this help text.");
        pw.println("  deny [contextHubId] [packageName] [nanoAppId]");
        pw.println("    Immediately transitions the package's authentication state to denied so");
        pw.println("    can no longer communciate with the nanoapp.");
    }
}
