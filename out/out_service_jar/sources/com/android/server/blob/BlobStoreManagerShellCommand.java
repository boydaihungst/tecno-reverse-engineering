package com.android.server.blob;

import android.app.ActivityManager;
import android.app.blob.BlobHandle;
import android.os.ShellCommand;
import java.io.PrintWriter;
import java.util.Base64;
/* loaded from: classes.dex */
class BlobStoreManagerShellCommand extends ShellCommand {
    private final BlobStoreManagerService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public BlobStoreManagerShellCommand(BlobStoreManagerService blobStoreManagerService) {
        this.mService = blobStoreManagerService;
    }

    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(null);
        }
        PrintWriter pw = getOutPrintWriter();
        char c = 65535;
        switch (cmd.hashCode()) {
            case -1168531841:
                if (cmd.equals("delete-blob")) {
                    c = 2;
                    break;
                }
                break;
            case -971115831:
                if (cmd.equals("clear-all-sessions")) {
                    c = 0;
                    break;
                }
                break;
            case -258166326:
                if (cmd.equals("clear-all-blobs")) {
                    c = 1;
                    break;
                }
                break;
            case 712607671:
                if (cmd.equals("query-blob-existence")) {
                    c = 4;
                    break;
                }
                break;
            case 1861559962:
                if (cmd.equals("idle-maintenance")) {
                    c = 3;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return runClearAllSessions(pw);
            case 1:
                return runClearAllBlobs(pw);
            case 2:
                return runDeleteBlob(pw);
            case 3:
                return runIdleMaintenance(pw);
            case 4:
                return runQueryBlobExistence(pw);
            default:
                return handleDefaultCommands(cmd);
        }
    }

    private int runClearAllSessions(PrintWriter pw) {
        ParsedArgs args = new ParsedArgs();
        args.userId = -1;
        if (parseOptions(pw, args) < 0) {
            return -1;
        }
        this.mService.runClearAllSessions(args.userId);
        return 0;
    }

    private int runClearAllBlobs(PrintWriter pw) {
        ParsedArgs args = new ParsedArgs();
        args.userId = -1;
        if (parseOptions(pw, args) < 0) {
            return -1;
        }
        this.mService.runClearAllBlobs(args.userId);
        return 0;
    }

    private int runDeleteBlob(PrintWriter pw) {
        ParsedArgs args = new ParsedArgs();
        if (parseOptions(pw, args) < 0) {
            return -1;
        }
        this.mService.deleteBlob(args.getBlobHandle(), args.userId);
        return 0;
    }

    private int runIdleMaintenance(PrintWriter pw) {
        this.mService.runIdleMaintenance();
        return 0;
    }

    private int runQueryBlobExistence(PrintWriter pw) {
        ParsedArgs args = new ParsedArgs();
        if (parseOptions(pw, args) < 0) {
            return -1;
        }
        pw.println(this.mService.isBlobAvailable(args.blobId, args.userId) ? 1 : 0);
        return 0;
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("BlobStore service (blob_store) commands:");
        pw.println("help");
        pw.println("    Print this help text.");
        pw.println();
        pw.println("clear-all-sessions [-u | --user USER_ID]");
        pw.println("    Remove all sessions.");
        pw.println("    Options:");
        pw.println("      -u or --user: specify which user's sessions to be removed.");
        pw.println("                    If not specified, sessions in all users are removed.");
        pw.println();
        pw.println("clear-all-blobs [-u | --user USER_ID]");
        pw.println("    Remove all blobs.");
        pw.println("    Options:");
        pw.println("      -u or --user: specify which user's blobs to be removed.");
        pw.println("                    If not specified, blobs in all users are removed.");
        pw.println("delete-blob [-u | --user USER_ID] [--digest DIGEST] [--expiry EXPIRY_TIME] [--label LABEL] [--tag TAG]");
        pw.println("    Delete a blob.");
        pw.println("    Options:");
        pw.println("      -u or --user: specify which user's blobs to be removed;");
        pw.println("                    If not specified, blobs in all users are removed.");
        pw.println("      --digest: Base64 encoded digest of the blob to delete.");
        pw.println("      --expiry: Expiry time of the blob to delete, in milliseconds.");
        pw.println("      --label: Label of the blob to delete.");
        pw.println("      --tag: Tag of the blob to delete.");
        pw.println("idle-maintenance");
        pw.println("    Run idle maintenance which takes care of removing stale data.");
        pw.println("query-blob-existence [-b BLOB_ID]");
        pw.println("    Prints 1 if blob exists, otherwise 0.");
        pw.println();
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:23:0x0049, code lost:
        if (r0.equals("-u") != false) goto L8;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int parseOptions(PrintWriter pw, ParsedArgs args) {
        while (true) {
            String opt = getNextOption();
            char c = 0;
            if (opt != null) {
                switch (opt.hashCode()) {
                    case -1620968108:
                        if (opt.equals("--label")) {
                            c = 4;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1493:
                        if (opt.equals("-b")) {
                            c = 7;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1512:
                        break;
                    case 43013626:
                        if (opt.equals("--tag")) {
                            c = 6;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1068100452:
                        if (opt.equals("--digest")) {
                            c = 3;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1110854355:
                        if (opt.equals("--expiry")) {
                            c = 5;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1332867059:
                        if (opt.equals("--algo")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1333469547:
                        if (opt.equals("--user")) {
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
                    case 1:
                        args.userId = Integer.parseInt(getNextArgRequired());
                        break;
                    case 2:
                        args.algorithm = getNextArgRequired();
                        break;
                    case 3:
                        args.digest = Base64.getDecoder().decode(getNextArgRequired());
                        break;
                    case 4:
                        args.label = getNextArgRequired();
                        break;
                    case 5:
                        args.expiryTimeMillis = Long.parseLong(getNextArgRequired());
                        break;
                    case 6:
                        args.tag = getNextArgRequired();
                        break;
                    case 7:
                        args.blobId = Long.parseLong(getNextArgRequired());
                        break;
                    default:
                        pw.println("Error: unknown option '" + opt + "'");
                        return -1;
                }
            } else {
                if (args.userId == -2) {
                    args.userId = ActivityManager.getCurrentUser();
                }
                return 0;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ParsedArgs {
        public String algorithm;
        public long blobId;
        public byte[] digest;
        public long expiryTimeMillis;
        public CharSequence label;
        public String tag;
        public int userId;

        private ParsedArgs() {
            this.userId = -2;
            this.algorithm = "SHA-256";
        }

        public BlobHandle getBlobHandle() {
            return BlobHandle.create(this.algorithm, this.digest, this.label, this.expiryTimeMillis, this.tag);
        }
    }
}
