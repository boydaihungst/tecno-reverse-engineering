package com.android.server;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.DropBoxManager;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.StatFs;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;
import android.text.TextUtils;
import android.text.format.TimeMigrationUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.os.IDropBoxManagerService;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.ObjectUtils;
import com.android.server.DropBoxManagerInternal;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.PackageManagerService;
import com.android.server.timezonedetector.ServiceConfigAccessor;
import defpackage.CompanionAppsPermissions;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.GZIPOutputStream;
import libcore.io.IoUtils;
/* loaded from: classes.dex */
public final class DropBoxManagerService extends SystemService {
    private static final long COMPRESS_THRESHOLD_BYTES = 16384;
    private static final int DEFAULT_AGE_SECONDS = 259200;
    private static final int DEFAULT_MAX_FILES = 1000;
    private static final int DEFAULT_MAX_FILES_LOWRAM = 300;
    private static final int DEFAULT_QUOTA_KB = 10240;
    private static final int DEFAULT_QUOTA_PERCENT = 10;
    private static final int DEFAULT_RESERVE_PERCENT = 10;
    private static final boolean PROFILE_DUMP = false;
    private static final int PROTO_MAX_DATA_BYTES = 262144;
    private static final int QUOTA_RESCAN_MILLIS = 5000;
    private static final String TAG = "DropBoxManagerService";
    private FileList mAllFiles;
    private int mBlockSize;
    private volatile boolean mBooted;
    private int mCachedQuotaBlocks;
    private long mCachedQuotaUptimeMillis;
    private final ContentResolver mContentResolver;
    private final File mDropBoxDir;
    private ArrayMap<String, FileList> mFilesByTag;
    private final DropBoxManagerBroadcastHandler mHandler;
    private long mLowPriorityRateLimitPeriod;
    private ArraySet<String> mLowPriorityTags;
    private int mMaxFiles;
    private final BroadcastReceiver mReceiver;
    private StatFs mStatFs;
    private final IDropBoxManagerService.Stub mStub;

    /* loaded from: classes.dex */
    private class ShellCmd extends ShellCommand {
        private ShellCmd() {
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        public int onCommand(String cmd) {
            if (cmd == null) {
                return handleDefaultCommands(cmd);
            }
            PrintWriter pw = getOutPrintWriter();
            char c = 65535;
            try {
                switch (cmd.hashCode()) {
                    case -1412652367:
                        if (cmd.equals("restore-defaults")) {
                            c = 3;
                            break;
                        }
                        break;
                    case -529247831:
                        if (cmd.equals("add-low-priority")) {
                            c = 1;
                            break;
                        }
                        break;
                    case -444925274:
                        if (cmd.equals("remove-low-priority")) {
                            c = 2;
                            break;
                        }
                        break;
                    case 1936917209:
                        if (cmd.equals("set-rate-limit")) {
                            c = 0;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                        String addedTag = getNextArgRequired();
                        long period = Long.parseLong(addedTag);
                        DropBoxManagerService.this.setLowPriorityRateLimit(period);
                        break;
                    case 1:
                        String addedTag2 = getNextArgRequired();
                        DropBoxManagerService.this.addLowPriorityTag(addedTag2);
                        break;
                    case 2:
                        String removeTag = getNextArgRequired();
                        DropBoxManagerService.this.removeLowPriorityTag(removeTag);
                        break;
                    case 3:
                        DropBoxManagerService.this.restoreDefaults();
                        break;
                    default:
                        return handleDefaultCommands(cmd);
                }
            } catch (Exception e) {
                pw.println(e);
            }
            return 0;
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("Dropbox manager service commands:");
            pw.println("  help");
            pw.println("    Print this help text.");
            pw.println("  set-rate-limit PERIOD");
            pw.println("    Sets low priority broadcast rate limit period to PERIOD ms");
            pw.println("  add-low-priority TAG");
            pw.println("    Add TAG to dropbox low priority list");
            pw.println("  remove-low-priority TAG");
            pw.println("    Remove TAG from dropbox low priority list");
            pw.println("  restore-defaults");
            pw.println("    restore dropbox settings to defaults");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class DropBoxManagerBroadcastHandler extends Handler {
        static final int MSG_SEND_BROADCAST = 1;
        static final int MSG_SEND_DEFERRED_BROADCAST = 2;
        private final ArrayMap<String, Intent> mDeferredMap;
        private final Object mLock;

        DropBoxManagerBroadcastHandler(Looper looper) {
            super(looper);
            this.mLock = new Object();
            this.mDeferredMap = new ArrayMap<>();
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            Intent deferredIntent;
            switch (msg.what) {
                case 1:
                    prepareAndSendBroadcast((Intent) msg.obj);
                    return;
                case 2:
                    synchronized (this.mLock) {
                        deferredIntent = this.mDeferredMap.remove((String) msg.obj);
                    }
                    if (deferredIntent != null) {
                        prepareAndSendBroadcast(deferredIntent);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }

        private void prepareAndSendBroadcast(Intent intent) {
            if (!DropBoxManagerService.this.mBooted) {
                intent.addFlags(1073741824);
            }
            DropBoxManagerService.this.getContext().sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.READ_LOGS");
        }

        private Intent createIntent(String tag, long time) {
            Intent dropboxIntent = new Intent("android.intent.action.DROPBOX_ENTRY_ADDED");
            dropboxIntent.putExtra("tag", tag);
            dropboxIntent.putExtra("time", time);
            return dropboxIntent;
        }

        public void sendBroadcast(String tag, long time) {
            sendMessage(obtainMessage(1, createIntent(tag, time)));
        }

        public void maybeDeferBroadcast(String tag, long time) {
            synchronized (this.mLock) {
                Intent intent = this.mDeferredMap.get(tag);
                if (intent == null) {
                    this.mDeferredMap.put(tag, createIntent(tag, time));
                    sendMessageDelayed(obtainMessage(2, tag), DropBoxManagerService.this.mLowPriorityRateLimitPeriod);
                    return;
                }
                intent.putExtra("time", time);
                int dropped = intent.getIntExtra("android.os.extra.DROPPED_COUNT", 0);
                intent.putExtra("android.os.extra.DROPPED_COUNT", dropped + 1);
            }
        }
    }

    public DropBoxManagerService(Context context) {
        this(context, new File("/data/system/dropbox"), FgThread.get().getLooper());
    }

    public DropBoxManagerService(Context context, File path, Looper looper) {
        super(context);
        this.mAllFiles = null;
        this.mFilesByTag = null;
        this.mLowPriorityRateLimitPeriod = 0L;
        this.mLowPriorityTags = null;
        this.mStatFs = null;
        this.mBlockSize = 0;
        this.mCachedQuotaBlocks = 0;
        this.mCachedQuotaUptimeMillis = 0L;
        this.mBooted = false;
        this.mMaxFiles = -1;
        this.mReceiver = new BroadcastReceiver() { // from class: com.android.server.DropBoxManagerService.1
            /* JADX WARN: Type inference failed for: r0v1, types: [com.android.server.DropBoxManagerService$1$1] */
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                DropBoxManagerService.this.mCachedQuotaUptimeMillis = 0L;
                new Thread() { // from class: com.android.server.DropBoxManagerService.1.1
                    @Override // java.lang.Thread, java.lang.Runnable
                    public void run() {
                        try {
                            DropBoxManagerService.this.init();
                            DropBoxManagerService.this.trimToFit();
                        } catch (IOException e) {
                            Slog.e(DropBoxManagerService.TAG, "Can't init", e);
                        }
                    }
                }.start();
            }
        };
        this.mStub = new IDropBoxManagerService.Stub() { // from class: com.android.server.DropBoxManagerService.2
            public void addData(String tag, byte[] data, int flags) {
                DropBoxManagerService.this.addData(tag, data, flags);
            }

            public void addFile(String tag, ParcelFileDescriptor fd, int flags) {
                DropBoxManagerService.this.addFile(tag, fd, flags);
            }

            public boolean isTagEnabled(String tag) {
                return DropBoxManagerService.this.isTagEnabled(tag);
            }

            public DropBoxManager.Entry getNextEntry(String tag, long millis, String callingPackage) {
                return getNextEntryWithAttribution(tag, millis, callingPackage, null);
            }

            public DropBoxManager.Entry getNextEntryWithAttribution(String tag, long millis, String callingPackage, String callingAttributionTag) {
                return DropBoxManagerService.this.getNextEntry(tag, millis, callingPackage, callingAttributionTag);
            }

            public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
                DropBoxManagerService.this.dump(fd, pw, args);
            }

            /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.DropBoxManagerService$2 */
            /* JADX WARN: Multi-variable type inference failed */
            public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
                new ShellCmd().exec(this, in, out, err, args, callback, resultReceiver);
            }
        };
        this.mDropBoxDir = path;
        this.mContentResolver = getContext().getContentResolver();
        this.mHandler = new DropBoxManagerBroadcastHandler(looper);
        LocalServices.addService(DropBoxManagerInternal.class, new DropBoxManagerInternalImpl());
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("dropbox", this.mStub);
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        switch (phase) {
            case 500:
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.DEVICE_STORAGE_LOW");
                getContext().registerReceiver(this.mReceiver, filter);
                this.mContentResolver.registerContentObserver(Settings.Global.CONTENT_URI, true, new ContentObserver(new Handler()) { // from class: com.android.server.DropBoxManagerService.3
                    @Override // android.database.ContentObserver
                    public void onChange(boolean selfChange) {
                        DropBoxManagerService.this.mReceiver.onReceive(DropBoxManagerService.this.getContext(), null);
                    }
                });
                getLowPriorityResourceConfigs();
                return;
            case 1000:
                this.mBooted = true;
                return;
            default:
                return;
        }
    }

    public IDropBoxManagerService getServiceStub() {
        return this.mStub;
    }

    public void addData(String tag, byte[] data, int flags) {
        addEntry(tag, new ByteArrayInputStream(data), data.length, flags);
    }

    public void addFile(String tag, ParcelFileDescriptor fd, int flags) {
        try {
            StructStat stat = Os.fstat(fd.getFileDescriptor());
            if (!OsConstants.S_ISREG(stat.st_mode)) {
                throw new IllegalArgumentException(tag + " entry must be real file");
            }
            addEntry(tag, new ParcelFileDescriptor.AutoCloseInputStream(fd), stat.st_size, flags);
        } catch (ErrnoException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void addEntry(String tag, InputStream in, long length, int flags) {
        boolean forceCompress = false;
        if ((flags & 4) == 0 && length > COMPRESS_THRESHOLD_BYTES) {
            forceCompress = true;
            flags |= 4;
        }
        addEntry(tag, new SimpleEntrySource(in, length, forceCompress), flags);
    }

    /* loaded from: classes.dex */
    public static class SimpleEntrySource implements DropBoxManagerInternal.EntrySource {
        private final boolean forceCompress;
        private final InputStream in;
        private final long length;

        public SimpleEntrySource(InputStream in, long length, boolean forceCompress) {
            this.in = in;
            this.length = length;
            this.forceCompress = forceCompress;
        }

        @Override // com.android.server.DropBoxManagerInternal.EntrySource
        public long length() {
            return this.length;
        }

        @Override // com.android.server.DropBoxManagerInternal.EntrySource
        public void writeTo(FileDescriptor fd) throws IOException {
            if (this.forceCompress) {
                GZIPOutputStream gzipOutputStream = new GZIPOutputStream(new FileOutputStream(fd));
                FileUtils.copy(this.in, gzipOutputStream);
                gzipOutputStream.close();
                return;
            }
            FileUtils.copy(this.in, new FileOutputStream(fd));
        }

        @Override // java.io.Closeable, java.lang.AutoCloseable
        public void close() throws IOException {
            FileUtils.closeQuietly(this.in);
        }
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[INVOKE]}, finally: {[INVOKE, INVOKE, IF] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [510=5, 511=4] */
    public void addEntry(String tag, DropBoxManagerInternal.EntrySource entry, int flags) {
        File temp = null;
        try {
            try {
                Slog.i(TAG, "add tag=" + tag + " isTagEnabled=" + isTagEnabled(tag) + " flags=0x" + Integer.toHexString(flags));
            } catch (IOException e) {
                Slog.e(TAG, "Can't write: " + tag, e);
                logDropboxDropped(5, tag, 0L);
                IoUtils.closeQuietly(entry);
                if (0 == 0) {
                    return;
                }
            }
            if ((flags & 1) != 0) {
                throw new IllegalArgumentException();
            }
            init();
            if (!isTagEnabled(tag)) {
                IoUtils.closeQuietly(entry);
                if (0 != 0) {
                    temp.delete();
                    return;
                }
                return;
            }
            long length = entry.length();
            long max = trimToFit();
            if (length > max) {
                Slog.w(TAG, "Dropping: " + tag + " (" + length + " > " + max + " bytes)");
            } else {
                temp = new File(this.mDropBoxDir, "drop" + Thread.currentThread().getId() + ".tmp");
                FileOutputStream out = new FileOutputStream(temp);
                try {
                    entry.writeTo(out.getFD());
                    out.close();
                } catch (Throwable th) {
                    try {
                        out.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            }
            long time = createEntry(temp, tag, flags);
            temp = null;
            ArraySet<String> arraySet = this.mLowPriorityTags;
            if (arraySet == null || !arraySet.contains(tag)) {
                this.mHandler.sendBroadcast(tag, time);
            } else {
                this.mHandler.maybeDeferBroadcast(tag, time);
            }
            IoUtils.closeQuietly(entry);
            if (0 == 0) {
                return;
            }
            temp.delete();
        } catch (Throwable th3) {
            IoUtils.closeQuietly(entry);
            if (0 != 0) {
                temp.delete();
            }
            throw th3;
        }
    }

    private void logDropboxDropped(int reason, String tag, long entryAge) {
        FrameworkStatsLog.write((int) FrameworkStatsLog.DROPBOX_ENTRY_DROPPED, reason, tag, entryAge);
    }

    public boolean isTagEnabled(String tag) {
        long token = Binder.clearCallingIdentity();
        try {
            return !ServiceConfigAccessor.PROVIDER_MODE_DISABLED.equals(Settings.Global.getString(this.mContentResolver, "dropbox:" + tag));
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private boolean checkPermission(int callingUid, String callingPackage, String callingAttributionTag) {
        if (getContext().checkCallingPermission("android.permission.PEEK_DROPBOX_DATA") == 0) {
            return true;
        }
        getContext().enforceCallingOrSelfPermission("android.permission.READ_LOGS", TAG);
        switch (((AppOpsManager) getContext().getSystemService(AppOpsManager.class)).noteOp(43, callingUid, callingPackage, callingAttributionTag, (String) null)) {
            case 0:
                return true;
            case 3:
                getContext().enforceCallingOrSelfPermission("android.permission.PACKAGE_USAGE_STATS", TAG);
                return true;
            default:
                return false;
        }
    }

    public synchronized DropBoxManager.Entry getNextEntry(String tag, long millis, String callingPackage, String callingAttributionTag) {
        if (checkPermission(Binder.getCallingUid(), callingPackage, callingAttributionTag)) {
            try {
                init();
                FileList list = tag == null ? this.mAllFiles : this.mFilesByTag.get(tag);
                if (list == null) {
                    return null;
                }
                for (EntryFile entry : list.contents.tailSet(new EntryFile(millis + 1))) {
                    if (entry.tag != null) {
                        if ((entry.flags & 1) != 0) {
                            return new DropBoxManager.Entry(entry.tag, entry.timestampMillis);
                        }
                        File file = entry.getFile(this.mDropBoxDir);
                        try {
                            return new DropBoxManager.Entry(entry.tag, entry.timestampMillis, file, entry.flags);
                        } catch (IOException e) {
                            Slog.wtf(TAG, "Can't read: " + file, e);
                        }
                    }
                }
                return null;
            } catch (IOException e2) {
                Slog.e(TAG, "Can't init", e2);
                return null;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void setLowPriorityRateLimit(long period) {
        this.mLowPriorityRateLimitPeriod = period;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void addLowPriorityTag(String tag) {
        this.mLowPriorityTags.add(tag);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void removeLowPriorityTag(String tag) {
        this.mLowPriorityTags.remove(tag);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void restoreDefaults() {
        getLowPriorityResourceConfigs();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [732=6, 736=9, 737=4] */
    /* JADX WARN: Removed duplicated region for block: B:182:0x0376 A[Catch: all -> 0x03df, TRY_ENTER, TRY_LEAVE, TryCatch #18 {, blocks: (B:4:0x0007, B:8:0x0015, B:9:0x0019, B:11:0x0031, B:13:0x0034, B:15:0x003e, B:18:0x004a, B:20:0x0054, B:23:0x005f, B:26:0x006b, B:28:0x0075, B:31:0x0080, B:33:0x008a, B:34:0x009c, B:35:0x00a2, B:43:0x00d2, B:47:0x00d9, B:49:0x0128, B:50:0x0131, B:52:0x0137, B:53:0x0147, B:54:0x014c, B:55:0x015a, B:57:0x0160, B:60:0x016d, B:62:0x0171, B:63:0x0176, B:67:0x0190, B:69:0x019c, B:70:0x01a9, B:72:0x01b1, B:73:0x01be, B:75:0x01c9, B:76:0x01ce, B:80:0x01da, B:83:0x01f8, B:88:0x0212, B:190:0x0385, B:140:0x02d4, B:142:0x02d9, B:182:0x0376, B:184:0x037b, B:187:0x0380, B:171:0x0362, B:173:0x0367, B:86:0x0200, B:87:0x0205, B:66:0x018e, B:195:0x039c, B:197:0x03a3, B:202:0x03b2, B:200:0x03a8, B:201:0x03ad, B:207:0x03c0), top: B:225:0x0007, inners: #16 }] */
    /* JADX WARN: Removed duplicated region for block: B:190:0x0385 A[Catch: all -> 0x03df, TryCatch #18 {, blocks: (B:4:0x0007, B:8:0x0015, B:9:0x0019, B:11:0x0031, B:13:0x0034, B:15:0x003e, B:18:0x004a, B:20:0x0054, B:23:0x005f, B:26:0x006b, B:28:0x0075, B:31:0x0080, B:33:0x008a, B:34:0x009c, B:35:0x00a2, B:43:0x00d2, B:47:0x00d9, B:49:0x0128, B:50:0x0131, B:52:0x0137, B:53:0x0147, B:54:0x014c, B:55:0x015a, B:57:0x0160, B:60:0x016d, B:62:0x0171, B:63:0x0176, B:67:0x0190, B:69:0x019c, B:70:0x01a9, B:72:0x01b1, B:73:0x01be, B:75:0x01c9, B:76:0x01ce, B:80:0x01da, B:83:0x01f8, B:88:0x0212, B:190:0x0385, B:140:0x02d4, B:142:0x02d9, B:182:0x0376, B:184:0x037b, B:187:0x0380, B:171:0x0362, B:173:0x0367, B:86:0x0200, B:87:0x0205, B:66:0x018e, B:195:0x039c, B:197:0x03a3, B:202:0x03b2, B:200:0x03a8, B:201:0x03ad, B:207:0x03c0), top: B:225:0x0007, inners: #16 }] */
    /* JADX WARN: Removed duplicated region for block: B:219:0x037b A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public synchronized void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        ArrayList<String> searchArgs;
        boolean dumpProto;
        boolean doFile;
        Throwable th;
        File file;
        int i;
        if (DumpUtils.checkDumpAndUsageStatsPermission(getContext(), TAG, pw)) {
            try {
                init();
                StringBuilder out = new StringBuilder();
                ArrayList<String> searchArgs2 = new ArrayList<>();
                boolean doFile2 = false;
                boolean dumpProto2 = false;
                boolean doFile3 = false;
                for (int i2 = 0; args != null && i2 < args.length; i2++) {
                    if (!args[i2].equals("-p") && !args[i2].equals("--print")) {
                        if (!args[i2].equals("-f") && !args[i2].equals("--file")) {
                            if (!args[i2].equals("--proto")) {
                                if (!args[i2].equals("-h") && !args[i2].equals("--help")) {
                                    if (args[i2].startsWith("-")) {
                                        out.append("Unknown argument: ").append(args[i2]).append("\n");
                                    } else {
                                        searchArgs2.add(args[i2]);
                                    }
                                }
                                pw.println("Dropbox (dropbox) dump options:");
                                pw.println("  [-h|--help] [-p|--print] [-f|--file] [timestamp]");
                                pw.println("    -h|--help: print this help");
                                pw.println("    -p|--print: print full contents of each entry");
                                pw.println("    -f|--file: print path of each entry's file");
                                pw.println("    --proto: dump data to proto");
                                pw.println("  [timestamp] optionally filters to only those entries.");
                                return;
                            }
                            dumpProto2 = true;
                        }
                        doFile3 = true;
                    }
                    doFile2 = true;
                }
                if (dumpProto2) {
                    dumpProtoLocked(fd, searchArgs2);
                    return;
                }
                out.append("Drop box contents: ").append(this.mAllFiles.contents.size()).append(" entries\n");
                out.append("Max entries: ").append(this.mMaxFiles).append("\n");
                out.append("Low priority rate limit period: ");
                out.append(this.mLowPriorityRateLimitPeriod).append(" ms\n");
                out.append("Low priority tags: ").append(this.mLowPriorityTags).append("\n");
                if (!searchArgs2.isEmpty()) {
                    out.append("Searching for:");
                    Iterator<String> it = searchArgs2.iterator();
                    while (it.hasNext()) {
                        String a = it.next();
                        out.append(" ").append(a);
                    }
                    out.append("\n");
                }
                int numFound = 0;
                out.append("\n");
                Iterator<EntryFile> it2 = this.mAllFiles.contents.iterator();
                while (it2.hasNext()) {
                    EntryFile entry = it2.next();
                    if (matchEntry(entry, searchArgs2)) {
                        int numFound2 = numFound + 1;
                        if (doFile2) {
                            out.append("========================================\n");
                        }
                        String date = TimeMigrationUtils.formatMillisWithFixedFormat(entry.timestampMillis);
                        out.append(date).append(" ").append(entry.tag == null ? "(no tag)" : entry.tag);
                        File file2 = entry.getFile(this.mDropBoxDir);
                        if (file2 == null) {
                            out.append(" (no file)\n");
                            doFile = doFile3;
                            searchArgs = searchArgs2;
                            dumpProto = dumpProto2;
                        } else {
                            boolean z = true;
                            if ((entry.flags & 1) != 0) {
                                out.append(" (contents lost)\n");
                                doFile = doFile3;
                                searchArgs = searchArgs2;
                                dumpProto = dumpProto2;
                            } else {
                                out.append(" (");
                                if ((entry.flags & 4) != 0) {
                                    out.append("compressed ");
                                }
                                out.append((entry.flags & 2) != 0 ? "text" : "data");
                                searchArgs = searchArgs2;
                                dumpProto = dumpProto2;
                                out.append(", ").append(file2.length()).append(" bytes)\n");
                                if (doFile3 || (doFile2 && (entry.flags & 2) == 0)) {
                                    if (!doFile2) {
                                        out.append("    ");
                                    }
                                    out.append(file2.getPath()).append("\n");
                                }
                                if ((entry.flags & 2) == 0) {
                                    doFile = doFile3;
                                } else if (doFile2 || !doFile3) {
                                    DropBoxManager.Entry dbe = null;
                                    InputStreamReader isr = null;
                                    try {
                                        doFile = doFile3;
                                        try {
                                            try {
                                                file = file2;
                                                try {
                                                    dbe = new DropBoxManager.Entry(entry.tag, entry.timestampMillis, file, entry.flags);
                                                    int i3 = 0;
                                                    if (doFile2) {
                                                        try {
                                                            isr = new InputStreamReader(dbe.getInputStream());
                                                            try {
                                                                char[] buf = new char[4096];
                                                                boolean newline = false;
                                                                while (true) {
                                                                    int n = isr.read(buf);
                                                                    if (n <= 0) {
                                                                        break;
                                                                    }
                                                                    out.append(buf, i3, n);
                                                                    newline = buf[n + (-1)] == '\n';
                                                                    if (out.length() > 65536) {
                                                                        pw.write(out.toString());
                                                                        i = 0;
                                                                        out.setLength(0);
                                                                    } else {
                                                                        i = 0;
                                                                    }
                                                                    i3 = i;
                                                                }
                                                                if (!newline) {
                                                                    out.append("\n");
                                                                }
                                                            } catch (IOException e) {
                                                                e = e;
                                                                try {
                                                                    out.append("*** ").append(e.toString()).append("\n");
                                                                    try {
                                                                        Slog.e(TAG, "Can't read: " + file, e);
                                                                        if (dbe != null) {
                                                                            dbe.close();
                                                                        }
                                                                        if (isr != null) {
                                                                            try {
                                                                                isr.close();
                                                                            } catch (IOException e2) {
                                                                            }
                                                                        }
                                                                        if (doFile2) {
                                                                        }
                                                                        numFound = numFound2;
                                                                        searchArgs2 = searchArgs;
                                                                        dumpProto2 = dumpProto;
                                                                        doFile3 = doFile;
                                                                    } catch (Throwable th2) {
                                                                        th = th2;
                                                                        if (dbe != null) {
                                                                            dbe.close();
                                                                        }
                                                                        if (isr != null) {
                                                                            try {
                                                                                isr.close();
                                                                            } catch (IOException e3) {
                                                                            }
                                                                        }
                                                                        throw th;
                                                                    }
                                                                } catch (Throwable th3) {
                                                                    th = th3;
                                                                }
                                                            } catch (Throwable th4) {
                                                                th = th4;
                                                                if (dbe != null) {
                                                                }
                                                                if (isr != null) {
                                                                }
                                                                throw th;
                                                            }
                                                        } catch (IOException e4) {
                                                            e = e4;
                                                            isr = null;
                                                        } catch (Throwable th5) {
                                                            th = th5;
                                                            isr = null;
                                                        }
                                                    } else {
                                                        String text = dbe.getText(70);
                                                        out.append("    ");
                                                        if (text == null) {
                                                            out.append("[null]");
                                                        } else {
                                                            if (text.length() != 70) {
                                                                z = false;
                                                            }
                                                            boolean truncated = z;
                                                            out.append(text.trim().replace('\n', '/'));
                                                            if (truncated) {
                                                                out.append(" ...");
                                                            }
                                                        }
                                                        out.append("\n");
                                                        isr = null;
                                                    }
                                                    dbe.close();
                                                    if (isr != null) {
                                                        try {
                                                            isr.close();
                                                        } catch (IOException e5) {
                                                        }
                                                    }
                                                } catch (IOException e6) {
                                                    e = e6;
                                                    dbe = null;
                                                    isr = null;
                                                } catch (Throwable th6) {
                                                    th = th6;
                                                    dbe = null;
                                                    isr = null;
                                                }
                                            } catch (IOException e7) {
                                                e = e7;
                                                file = file2;
                                                dbe = null;
                                                isr = null;
                                            } catch (Throwable th7) {
                                                th = th7;
                                                dbe = null;
                                                isr = null;
                                            }
                                        } catch (IOException e8) {
                                            e = e8;
                                            file = file2;
                                            dbe = null;
                                        } catch (Throwable th8) {
                                            th = th8;
                                            dbe = null;
                                        }
                                    } catch (IOException e9) {
                                        e = e9;
                                        doFile = doFile3;
                                        file = file2;
                                    } catch (Throwable th9) {
                                        th = th9;
                                    }
                                } else {
                                    doFile = doFile3;
                                }
                                if (doFile2) {
                                    out.append("\n");
                                }
                            }
                        }
                        numFound = numFound2;
                        searchArgs2 = searchArgs;
                        dumpProto2 = dumpProto;
                        doFile3 = doFile;
                    }
                }
                if (numFound == 0) {
                    out.append("(No entries found.)\n");
                }
                if (args == null || args.length == 0) {
                    if (!doFile2) {
                        out.append("\n");
                    }
                    out.append("Usage: dumpsys dropbox [--print|--file] [YYYY-mm-dd] [HH:MM:SS] [tag]\n");
                }
                pw.write(out.toString());
            } catch (IOException e10) {
                pw.println("Can't initialize: " + e10);
                Slog.e(TAG, "Can't init", e10);
            }
        }
    }

    private boolean matchEntry(EntryFile entry, ArrayList<String> searchArgs) {
        String date = TimeMigrationUtils.formatMillisWithFixedFormat(entry.timestampMillis);
        boolean match = true;
        int numArgs = searchArgs.size();
        for (int i = 0; i < numArgs && match; i++) {
            String arg = searchArgs.get(i);
            match = date.contains(arg) || arg.equals(entry.tag);
        }
        return match;
    }

    private void dumpProtoLocked(FileDescriptor fd, ArrayList<String> searchArgs) {
        DropBoxManagerService dropBoxManagerService = this;
        ProtoOutputStream proto = new ProtoOutputStream(fd);
        Iterator<EntryFile> it = dropBoxManagerService.mAllFiles.contents.iterator();
        while (it.hasNext()) {
            EntryFile entry = it.next();
            if (dropBoxManagerService.matchEntry(entry, searchArgs)) {
                File file = entry.getFile(dropBoxManagerService.mDropBoxDir);
                if (file == null) {
                    dropBoxManagerService = this;
                } else if ((entry.flags & 1) != 0) {
                    continue;
                } else {
                    long bToken = proto.start(CompanionAppsPermissions.APP_PERMISSIONS);
                    proto.write(1112396529665L, entry.timestampMillis);
                    try {
                        DropBoxManager.Entry dbe = new DropBoxManager.Entry(entry.tag, entry.timestampMillis, file, entry.flags);
                        InputStream is = dbe.getInputStream();
                        if (is != null) {
                            try {
                                byte[] buf = new byte[262144];
                                int readBytes = 0;
                                int n = 0;
                                while (n >= 0) {
                                    int i = readBytes + n;
                                    readBytes = i;
                                    if (i >= 262144) {
                                        break;
                                    }
                                    n = is.read(buf, readBytes, 262144 - readBytes);
                                }
                                proto.write(CompanionAppsPermissions.AppPermissions.CERTIFICATES, Arrays.copyOf(buf, readBytes));
                            } catch (Throwable th) {
                                if (is != null) {
                                    try {
                                        is.close();
                                    } catch (Throwable th2) {
                                        th.addSuppressed(th2);
                                    }
                                }
                                throw th;
                                break;
                            }
                        }
                        if (is != null) {
                            is.close();
                        }
                        dbe.close();
                    } catch (IOException e) {
                        Slog.e(TAG, "Can't read: " + file, e);
                    }
                    proto.end(bToken);
                    dropBoxManagerService = this;
                }
            }
        }
        proto.flush();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class FileList implements Comparable<FileList> {
        public int blocks;
        public final TreeSet<EntryFile> contents;

        private FileList() {
            this.blocks = 0;
            this.contents = new TreeSet<>();
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.lang.Comparable
        public final int compareTo(FileList o) {
            int i = this.blocks;
            int i2 = o.blocks;
            if (i != i2) {
                return i2 - i;
            }
            if (this == o) {
                return 0;
            }
            if (hashCode() < o.hashCode()) {
                return -1;
            }
            return hashCode() > o.hashCode() ? 1 : 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class EntryFile implements Comparable<EntryFile> {
        public final int blocks;
        public final int flags;
        public final String tag;
        public final long timestampMillis;

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.lang.Comparable
        public final int compareTo(EntryFile o) {
            int comp = Long.compare(this.timestampMillis, o.timestampMillis);
            if (comp != 0) {
                return comp;
            }
            int comp2 = ObjectUtils.compare(this.tag, o.tag);
            if (comp2 != 0) {
                return comp2;
            }
            int comp3 = Integer.compare(this.flags, o.flags);
            return comp3 != 0 ? comp3 : Integer.compare(hashCode(), o.hashCode());
        }

        public EntryFile(File temp, File dir, String tag, long timestampMillis, int flags, int blockSize) throws IOException {
            if ((flags & 1) != 0) {
                throw new IllegalArgumentException();
            }
            this.tag = TextUtils.safeIntern(tag);
            this.timestampMillis = timestampMillis;
            this.flags = flags;
            File file = getFile(dir);
            if (!temp.renameTo(file)) {
                throw new IOException("Can't rename " + temp + " to " + file);
            }
            this.blocks = (int) (((file.length() + blockSize) - 1) / blockSize);
        }

        public EntryFile(File dir, String tag, long timestampMillis) throws IOException {
            this.tag = TextUtils.safeIntern(tag);
            this.timestampMillis = timestampMillis;
            this.flags = 1;
            this.blocks = 0;
            new FileOutputStream(getFile(dir)).close();
        }

        public EntryFile(File file, int blockSize) {
            boolean parseFailure = false;
            String name = file.getName();
            int flags = 0;
            String tag = null;
            long millis = 0;
            int at = name.lastIndexOf(64);
            if (at < 0) {
                parseFailure = true;
            } else {
                tag = Uri.decode(name.substring(0, at));
                if (name.endsWith(PackageManagerService.COMPRESSED_EXTENSION)) {
                    flags = 0 | 4;
                    name = name.substring(0, name.length() - 3);
                }
                if (name.endsWith(".lost")) {
                    flags |= 1;
                    name = name.substring(at + 1, name.length() - 5);
                } else if (name.endsWith(".txt")) {
                    flags |= 2;
                    name = name.substring(at + 1, name.length() - 4);
                } else if (name.endsWith(".dat")) {
                    name = name.substring(at + 1, name.length() - 4);
                } else {
                    parseFailure = true;
                }
                if (!parseFailure) {
                    try {
                        millis = Long.parseLong(name);
                    } catch (NumberFormatException e) {
                        parseFailure = true;
                    }
                }
            }
            if (parseFailure) {
                Slog.wtf(DropBoxManagerService.TAG, "Invalid filename: " + file);
                file.delete();
                this.tag = null;
                this.flags = 1;
                this.timestampMillis = 0L;
                this.blocks = 0;
                return;
            }
            this.blocks = (int) (((file.length() + blockSize) - 1) / blockSize);
            this.tag = TextUtils.safeIntern(tag);
            this.flags = flags;
            this.timestampMillis = millis;
        }

        public EntryFile(long millis) {
            this.tag = null;
            this.timestampMillis = millis;
            this.flags = 1;
            this.blocks = 0;
        }

        public boolean hasFile() {
            return this.tag != null;
        }

        private String getExtension() {
            if ((this.flags & 1) != 0) {
                return ".lost";
            }
            return ((this.flags & 2) != 0 ? ".txt" : ".dat") + ((this.flags & 4) != 0 ? PackageManagerService.COMPRESSED_EXTENSION : "");
        }

        public String getFilename() {
            if (hasFile()) {
                return Uri.encode(this.tag) + "@" + this.timestampMillis + getExtension();
            }
            return null;
        }

        public File getFile(File dir) {
            if (hasFile()) {
                return new File(dir, getFilename());
            }
            return null;
        }

        public void deleteFile(File dir) {
            if (hasFile()) {
                getFile(dir).delete();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void init() throws IOException {
        if (this.mStatFs == null) {
            if (!this.mDropBoxDir.isDirectory() && !this.mDropBoxDir.mkdirs()) {
                throw new IOException("Can't mkdir: " + this.mDropBoxDir);
            }
            try {
                StatFs statFs = new StatFs(this.mDropBoxDir.getPath());
                this.mStatFs = statFs;
                this.mBlockSize = statFs.getBlockSize();
            } catch (IllegalArgumentException e) {
                throw new IOException("Can't statfs: " + this.mDropBoxDir);
            }
        }
        if (this.mAllFiles == null) {
            File[] files = this.mDropBoxDir.listFiles();
            if (files == null) {
                throw new IOException("Can't list files: " + this.mDropBoxDir);
            }
            this.mAllFiles = new FileList();
            this.mFilesByTag = new ArrayMap<>();
            for (File file : files) {
                if (file.getName().endsWith(".tmp")) {
                    Slog.i(TAG, "Cleaning temp file: " + file);
                    file.delete();
                } else {
                    EntryFile entry = new EntryFile(file, this.mBlockSize);
                    if (entry.hasFile()) {
                        enrollEntry(entry);
                    }
                }
            }
        }
    }

    private synchronized void enrollEntry(EntryFile entry) {
        this.mAllFiles.contents.add(entry);
        this.mAllFiles.blocks += entry.blocks;
        if (entry.hasFile() && entry.blocks > 0) {
            FileList tagFiles = this.mFilesByTag.get(entry.tag);
            if (tagFiles == null) {
                tagFiles = new FileList();
                this.mFilesByTag.put(TextUtils.safeIntern(entry.tag), tagFiles);
            }
            tagFiles.contents.add(entry);
            tagFiles.blocks += entry.blocks;
        }
    }

    private synchronized long createEntry(File temp, String tag, int flags) throws IOException {
        long t;
        SortedSet<EntryFile> tail;
        long j;
        t = System.currentTimeMillis();
        SortedSet<EntryFile> tail2 = this.mAllFiles.contents.tailSet(new EntryFile(JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY + t));
        EntryFile[] future = null;
        if (!tail2.isEmpty()) {
            future = (EntryFile[]) tail2.toArray(new EntryFile[tail2.size()]);
            tail2.clear();
        }
        long j2 = 1;
        if (!this.mAllFiles.contents.isEmpty()) {
            t = Math.max(t, this.mAllFiles.contents.last().timestampMillis + 1);
        }
        if (future != null) {
            int length = future.length;
            int i = 0;
            long t2 = t;
            while (i < length) {
                EntryFile late = future[i];
                this.mAllFiles.blocks -= late.blocks;
                FileList tagFiles = this.mFilesByTag.get(late.tag);
                if (tagFiles != null && tagFiles.contents.remove(late)) {
                    tagFiles.blocks -= late.blocks;
                }
                if ((late.flags & 1) == 0) {
                    tail = tail2;
                    enrollEntry(new EntryFile(late.getFile(this.mDropBoxDir), this.mDropBoxDir, late.tag, t2, late.flags, this.mBlockSize));
                    t2 += j2;
                    j = 1;
                } else {
                    tail = tail2;
                    j = 1;
                    enrollEntry(new EntryFile(this.mDropBoxDir, late.tag, t2));
                    t2++;
                }
                i++;
                j2 = j;
                tail2 = tail;
            }
            t = t2;
        }
        if (temp == null) {
            enrollEntry(new EntryFile(this.mDropBoxDir, tag, t));
        } else {
            enrollEntry(new EntryFile(temp, this.mDropBoxDir, tag, t, flags, this.mBlockSize));
        }
        return t;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized long trimToFit() throws IOException {
        DropBoxManagerService dropBoxManagerService;
        int ageSeconds;
        long curTimeMillis;
        DropBoxManagerService dropBoxManagerService2 = this;
        synchronized (this) {
            try {
                int ageSeconds2 = Settings.Global.getInt(dropBoxManagerService2.mContentResolver, "dropbox_age_seconds", DEFAULT_AGE_SECONDS);
                dropBoxManagerService2.mMaxFiles = Settings.Global.getInt(dropBoxManagerService2.mContentResolver, "dropbox_max_files", ActivityManager.isLowRamDeviceStatic() ? 300 : 1000);
                long curTimeMillis2 = System.currentTimeMillis();
                long cutoffMillis = curTimeMillis2 - (ageSeconds2 * 1000);
                while (!dropBoxManagerService2.mAllFiles.contents.isEmpty()) {
                    EntryFile entry = dropBoxManagerService2.mAllFiles.contents.first();
                    if (entry.timestampMillis > cutoffMillis && dropBoxManagerService2.mAllFiles.contents.size() < dropBoxManagerService2.mMaxFiles) {
                        break;
                    }
                    dropBoxManagerService2.logDropboxDropped(4, entry.tag, curTimeMillis2 - entry.timestampMillis);
                    FileList tag = dropBoxManagerService2.mFilesByTag.get(entry.tag);
                    if (tag != null && tag.contents.remove(entry)) {
                        tag.blocks -= entry.blocks;
                    }
                    if (dropBoxManagerService2.mAllFiles.contents.remove(entry)) {
                        dropBoxManagerService2.mAllFiles.blocks -= entry.blocks;
                    }
                    entry.deleteFile(dropBoxManagerService2.mDropBoxDir);
                }
                long uptimeMillis = SystemClock.uptimeMillis();
                if (uptimeMillis > dropBoxManagerService2.mCachedQuotaUptimeMillis + 5000) {
                    int quotaPercent = Settings.Global.getInt(dropBoxManagerService2.mContentResolver, "dropbox_quota_percent", 10);
                    int reservePercent = Settings.Global.getInt(dropBoxManagerService2.mContentResolver, "dropbox_reserve_percent", 10);
                    int quotaKb = Settings.Global.getInt(dropBoxManagerService2.mContentResolver, "dropbox_quota_kb", DEFAULT_QUOTA_KB);
                    String dirPath = dropBoxManagerService2.mDropBoxDir.getPath();
                    try {
                        dropBoxManagerService2.mStatFs.restat(dirPath);
                        long available = dropBoxManagerService2.mStatFs.getAvailableBlocksLong();
                        long cutoffMillis2 = reservePercent;
                        long nonreserved = available - ((dropBoxManagerService2.mStatFs.getBlockCountLong() * cutoffMillis2) / 100);
                        long maxAvailableLong = (quotaPercent * nonreserved) / 100;
                        int maxAvailable = Math.toIntExact(Math.max(0L, Math.min(maxAvailableLong, 2147483647L)));
                        int maximum = (quotaKb * 1024) / dropBoxManagerService2.mBlockSize;
                        dropBoxManagerService2.mCachedQuotaBlocks = Math.min(maximum, maxAvailable);
                        dropBoxManagerService2.mCachedQuotaUptimeMillis = uptimeMillis;
                    } catch (IllegalArgumentException e) {
                        throw new IOException("Can't restat: " + dropBoxManagerService2.mDropBoxDir);
                    }
                }
                if (dropBoxManagerService2.mAllFiles.blocks <= dropBoxManagerService2.mCachedQuotaBlocks) {
                    dropBoxManagerService = dropBoxManagerService2;
                } else {
                    int unsqueezed = dropBoxManagerService2.mAllFiles.blocks;
                    TreeSet<FileList> tags = new TreeSet<>(dropBoxManagerService2.mFilesByTag.values());
                    Iterator<FileList> it = tags.iterator();
                    int squeezed = 0;
                    int squeezed2 = unsqueezed;
                    while (it.hasNext()) {
                        FileList tag2 = it.next();
                        if (squeezed > 0 && tag2.blocks <= (dropBoxManagerService2.mCachedQuotaBlocks - squeezed2) / squeezed) {
                            break;
                        }
                        squeezed2 -= tag2.blocks;
                        squeezed++;
                    }
                    int tagQuota = (dropBoxManagerService2.mCachedQuotaBlocks - squeezed2) / squeezed;
                    Iterator<FileList> it2 = tags.iterator();
                    while (true) {
                        if (!it2.hasNext()) {
                            dropBoxManagerService = dropBoxManagerService2;
                            break;
                        }
                        FileList tag3 = it2.next();
                        if (dropBoxManagerService2.mAllFiles.blocks < dropBoxManagerService2.mCachedQuotaBlocks) {
                            dropBoxManagerService = dropBoxManagerService2;
                            break;
                        }
                        while (tag3.blocks > tagQuota && !tag3.contents.isEmpty()) {
                            EntryFile entry2 = tag3.contents.first();
                            try {
                                ageSeconds = ageSeconds2;
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                            try {
                                logDropboxDropped(3, entry2.tag, curTimeMillis2 - entry2.timestampMillis);
                                if (tag3.contents.remove(entry2)) {
                                    tag3.blocks -= entry2.blocks;
                                }
                                if (this.mAllFiles.contents.remove(entry2)) {
                                    this.mAllFiles.blocks -= entry2.blocks;
                                }
                                try {
                                    entry2.deleteFile(this.mDropBoxDir);
                                    curTimeMillis = curTimeMillis2;
                                } catch (IOException e2) {
                                    e = e2;
                                    curTimeMillis = curTimeMillis2;
                                }
                                try {
                                    enrollEntry(new EntryFile(this.mDropBoxDir, entry2.tag, entry2.timestampMillis));
                                } catch (IOException e3) {
                                    e = e3;
                                    Slog.e(TAG, "Can't write tombstone file", e);
                                    dropBoxManagerService2 = this;
                                    ageSeconds2 = ageSeconds;
                                    curTimeMillis2 = curTimeMillis;
                                }
                                dropBoxManagerService2 = this;
                                ageSeconds2 = ageSeconds;
                                curTimeMillis2 = curTimeMillis;
                            } catch (Throwable th2) {
                                th = th2;
                                throw th;
                            }
                        }
                        dropBoxManagerService2 = dropBoxManagerService2;
                        ageSeconds2 = ageSeconds2;
                        curTimeMillis2 = curTimeMillis2;
                    }
                }
                return dropBoxManagerService.mCachedQuotaBlocks * dropBoxManagerService.mBlockSize;
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    private void getLowPriorityResourceConfigs() {
        this.mLowPriorityRateLimitPeriod = Resources.getSystem().getInteger(17694827);
        String[] lowPrioritytags = Resources.getSystem().getStringArray(17236058);
        int size = lowPrioritytags.length;
        if (size == 0) {
            this.mLowPriorityTags = null;
            return;
        }
        this.mLowPriorityTags = new ArraySet<>(size);
        for (String str : lowPrioritytags) {
            this.mLowPriorityTags.add(str);
        }
    }

    /* loaded from: classes.dex */
    private final class DropBoxManagerInternalImpl extends DropBoxManagerInternal {
        private DropBoxManagerInternalImpl() {
        }

        @Override // com.android.server.DropBoxManagerInternal
        public void addEntry(String tag, DropBoxManagerInternal.EntrySource entry, int flags) {
            DropBoxManagerService.this.addEntry(tag, entry, flags);
        }
    }
}
