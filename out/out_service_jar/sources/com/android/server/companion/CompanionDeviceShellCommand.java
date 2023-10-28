package com.android.server.companion;

import android.companion.AssociationInfo;
import android.os.Binder;
import android.os.ShellCommand;
import android.util.Log;
import android.util.Slog;
import com.android.internal.util.FunctionalUtils;
import com.android.server.companion.presence.CompanionDevicePresenceMonitor;
import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;
/* loaded from: classes.dex */
class CompanionDeviceShellCommand extends ShellCommand {
    private static final String TAG = "CompanionDevice_ShellCommand";
    private final AssociationStore mAssociationStore;
    private final CompanionDevicePresenceMonitor mDevicePresenceMonitor;
    private final CompanionDeviceManagerService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public CompanionDeviceShellCommand(CompanionDeviceManagerService service, AssociationStore associationStore, CompanionDevicePresenceMonitor devicePresenceMonitor) {
        this.mService = service;
        this.mAssociationStore = associationStore;
        this.mDevicePresenceMonitor = devicePresenceMonitor;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public int onCommand(String cmd) {
        PrintWriter out = getOutPrintWriter();
        char c = 65535;
        try {
            switch (cmd.hashCode()) {
                case -2105020158:
                    if (cmd.equals("clear-association-memory-cache")) {
                        c = 3;
                        break;
                    }
                    break;
                case -1855910485:
                    if (cmd.equals("remove-inactive-associations")) {
                        c = 6;
                        break;
                    }
                    break;
                case -191868716:
                    if (cmd.equals("simulate-device-disappeared")) {
                        c = 5;
                        break;
                    }
                    break;
                case 3322014:
                    if (cmd.equals("list")) {
                        c = 0;
                        break;
                    }
                    break;
                case 784321104:
                    if (cmd.equals("disassociate")) {
                        c = 2;
                        break;
                    }
                    break;
                case 1586499358:
                    if (cmd.equals("associate")) {
                        c = 1;
                        break;
                    }
                    break;
                case 2001610978:
                    if (cmd.equals("simulate-device-appeared")) {
                        c = 4;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    int userId = getNextIntArgRequired();
                    List<AssociationInfo> associationsForUser = this.mAssociationStore.getAssociationsForUser(userId);
                    for (AssociationInfo association : associationsForUser) {
                        out.println(association.getPackageName() + " " + association.getDeviceMacAddress());
                    }
                    break;
                case 1:
                    int userId2 = getNextIntArgRequired();
                    String packageName = getNextArgRequired();
                    String address = getNextArgRequired();
                    this.mService.legacyCreateAssociation(userId2, address, packageName, null);
                    break;
                case 2:
                    int userId3 = getNextIntArgRequired();
                    String packageName2 = getNextArgRequired();
                    String address2 = getNextArgRequired();
                    AssociationInfo association2 = this.mService.getAssociationWithCallerChecks(userId3, packageName2, address2);
                    if (association2 != null) {
                        this.mService.disassociateInternal(association2.getId());
                    }
                    break;
                case 3:
                    this.mService.persistState();
                    this.mService.loadAssociationsFromDisk();
                    break;
                case 4:
                    int associationId = getNextIntArgRequired();
                    this.mDevicePresenceMonitor.simulateDeviceAppeared(associationId);
                    break;
                case 5:
                    int associationId2 = getNextIntArgRequired();
                    this.mDevicePresenceMonitor.simulateDeviceDisappeared(associationId2);
                    break;
                case 6:
                    final CompanionDeviceManagerService companionDeviceManagerService = this.mService;
                    Objects.requireNonNull(companionDeviceManagerService);
                    Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.companion.CompanionDeviceShellCommand$$ExternalSyntheticLambda0
                        public final void runOrThrow() {
                            CompanionDeviceManagerService.this.removeInactiveSelfManagedAssociations();
                        }
                    });
                    break;
                default:
                    return handleDefaultCommands(cmd);
            }
            return 0;
        } catch (Throwable t) {
            Slog.e(TAG, "Error running a command: $ " + cmd, t);
            getErrPrintWriter().println(Log.getStackTraceString(t));
            return 1;
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Companion Device Manager (companiondevice) commands:");
        pw.println("  help");
        pw.println("      Print this help text.");
        pw.println("  list USER_ID");
        pw.println("      List all Associations for a user.");
        pw.println("  associate USER_ID PACKAGE MAC_ADDRESS");
        pw.println("      Create a new Association.");
        pw.println("  disassociate USER_ID PACKAGE MAC_ADDRESS");
        pw.println("      Remove an existing Association.");
        pw.println("  clear-association-memory-cache");
        pw.println("      Clear the in-memory association cache and reload all association ");
        pw.println("      information from persistent storage. USE FOR DEBUGGING PURPOSES ONLY.");
        pw.println("      USE FOR DEBUGGING AND/OR TESTING PURPOSES ONLY.");
        pw.println("  simulate-device-appeared ASSOCIATION_ID");
        pw.println("      Make CDM act as if the given companion device has appeared.");
        pw.println("      I.e. bind the associated companion application's");
        pw.println("      CompanionDeviceService(s) and trigger onDeviceAppeared() callback.");
        pw.println("      The CDM will consider the devices as present for 60 seconds and then");
        pw.println("      will act as if device disappeared, unless 'simulate-device-disappeared'");
        pw.println("      or 'simulate-device-appeared' is called again before 60 seconds run out.");
        pw.println("      USE FOR DEBUGGING AND/OR TESTING PURPOSES ONLY.");
        pw.println("  simulate-device-disappeared ASSOCIATION_ID");
        pw.println("      Make CDM act as if the given companion device has disappeared.");
        pw.println("      I.e. unbind the associated companion application's");
        pw.println("      CompanionDeviceService(s) and trigger onDeviceDisappeared() callback.");
        pw.println("      NOTE: This will only have effect if 'simulate-device-appeared' was");
        pw.println("      invoked for the same device (same ASSOCIATION_ID) no longer than");
        pw.println("      60 seconds ago.");
        pw.println("      USE FOR DEBUGGING AND/OR TESTING PURPOSES ONLY.");
        pw.println("  remove-inactive-associations");
        pw.println("      Remove self-managed associations that have not been active ");
        pw.println("      for a long time (90 days or as configured via ");
        pw.println("      \"debug.cdm.cdmservice.cleanup_time_window\" system property). ");
        pw.println("      USE FOR DEBUGGING AND/OR TESTING PURPOSES ONLY.");
    }

    private int getNextIntArgRequired() {
        return Integer.parseInt(getNextArgRequired());
    }
}
