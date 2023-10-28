package com.android.server.net.watchlist;

import android.content.Context;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.provider.Settings;
import com.android.server.wm.ActivityTaskManagerService;
import java.io.InputStream;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
class NetworkWatchlistShellCommand extends ShellCommand {
    final Context mContext;
    final NetworkWatchlistService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public NetworkWatchlistShellCommand(NetworkWatchlistService service, Context context) {
        this.mContext = context;
        this.mService = service;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public int onCommand(String cmd) {
        boolean z;
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        try {
            switch (cmd.hashCode()) {
                case 1757613042:
                    if (cmd.equals("set-test-config")) {
                        z = false;
                        break;
                    }
                    z = true;
                    break;
                case 1854202282:
                    if (cmd.equals("force-generate-report")) {
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
                    return runSetTestConfig();
                case true:
                    return runForceGenerateReport();
                default:
                    return handleDefaultCommands(cmd);
            }
        } catch (Exception e) {
            pw.println("Exception: " + e);
            return -1;
        }
    }

    private int runSetTestConfig() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        try {
            String configXmlPath = getNextArgRequired();
            ParcelFileDescriptor pfd = openFileForSystem(configXmlPath, ActivityTaskManagerService.DUMP_RECENTS_SHORT_CMD);
            if (pfd == null) {
                pw.println("Error: can't open input file " + configXmlPath);
                return -1;
            }
            InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
            WatchlistConfig.getInstance().setTestMode(inputStream);
            inputStream.close();
            pw.println("Success!");
            return 0;
        } catch (Exception ex) {
            pw.println("Error: " + ex.toString());
            return -1;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [105=5] */
    private int runForceGenerateReport() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        long ident = Binder.clearCallingIdentity();
        try {
            if (WatchlistConfig.getInstance().isConfigSecure()) {
                pw.println("Error: Cannot force generate report under production config");
                return -1;
            }
            Settings.Global.putLong(this.mContext.getContentResolver(), "network_watchlist_last_report_time", 0L);
            this.mService.forceReportWatchlistForTest(System.currentTimeMillis());
            pw.println("Success!");
            Binder.restoreCallingIdentity(ident);
            return 0;
        } catch (Exception ex) {
            pw.println("Error: " + ex);
            return -1;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Network watchlist manager commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("  set-test-config your_watchlist_config.xml");
        pw.println("    Set network watchlist test config file.");
        pw.println("  force-generate-report");
        pw.println("    Force generate watchlist test report.");
    }
}
