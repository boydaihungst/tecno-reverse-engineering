package com.android.server.storage;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.os.Binder;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.DeviceConfig;
import android.util.ArrayMap;
import android.util.DataUnit;
import android.util.Slog;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.utils.PriorityDump;
import com.transsion.hubcore.server.ITranStorageManager;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
/* loaded from: classes2.dex */
public class DeviceStorageMonitorService extends SystemService {
    public static final String EXTRA_SEQUENCE = "seq";
    private static final long HIGH_CHECK_INTERVAL = 36000000;
    private static final long LOW_CHECK_INTERVAL = 60000;
    private static final int MSG_CHECK_HIGH = 2;
    private static final int MSG_CHECK_LOW = 1;
    static final int OPTION_FORCE_UPDATE = 1;
    static final String SERVICE = "devicestoragemonitor";
    private static final String TAG = "DeviceStorageMonitorService";
    private static final String TV_NOTIFICATION_CHANNEL_ID = "devicestoragemonitor.tv";
    private CacheFileDeletedObserver mCacheFileDeletedObserver;
    private volatile int mForceLevel;
    private final Handler mHandler;
    private final HandlerThread mHandlerThread;
    private final DeviceStorageMonitorInternal mLocalService;
    private NotificationManager mNotifManager;
    private final Binder mRemoteService;
    private final AtomicInteger mSeq;
    private final ArrayMap<UUID, State> mStates;
    private static final long DEFAULT_LOG_DELTA_BYTES = DataUnit.MEBIBYTES.toBytes(64);
    private static final long BOOT_IMAGE_STORAGE_REQUIREMENT = DataUnit.MEBIBYTES.toBytes(250);

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class State {
        private static final int LEVEL_FULL = 2;
        private static final int LEVEL_LOW = 1;
        private static final int LEVEL_NORMAL = 0;
        private static final int LEVEL_UNKNOWN = -1;
        public long lastUsableBytes;
        public int level;

        private State() {
            this.level = 0;
            this.lastUsableBytes = JobStatus.NO_LATEST_RUNTIME;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static boolean isEntering(int level, int oldLevel, int newLevel) {
            return newLevel >= level && (oldLevel < level || oldLevel == -1);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static boolean isLeaving(int level, int oldLevel, int newLevel) {
            return newLevel < level && (oldLevel >= level || oldLevel == -1);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static String levelToString(int level) {
            switch (level) {
                case -1:
                    return "UNKNOWN";
                case 0:
                    return PriorityDump.PRIORITY_ARG_NORMAL;
                case 1:
                    return "LOW";
                case 2:
                    return "FULL";
                default:
                    return Integer.toString(level);
            }
        }
    }

    private State findOrCreateState(UUID uuid) {
        State state = this.mStates.get(uuid);
        if (state == null) {
            State state2 = new State();
            this.mStates.put(uuid, state2);
            return state2;
        }
        return state;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkLow() {
        int oldLevel;
        int newLevel;
        StorageManager storage = (StorageManager) getContext().getSystemService(StorageManager.class);
        int seq = this.mSeq.get();
        Iterator it = storage.getWritablePrivateVolumes().iterator();
        while (it.hasNext()) {
            VolumeInfo vol = (VolumeInfo) it.next();
            File file = vol.getPath();
            long fullBytes = storage.getStorageFullBytes(file);
            long lowBytes = storage.getStorageLowBytes(file);
            if (file.getUsableSpace() < (3 * lowBytes) / 2) {
                PackageManagerInternal pm = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                try {
                    pm.freeStorage(vol.getFsUuid(), 2 * lowBytes, 0);
                } catch (IOException e) {
                    Slog.w(TAG, e);
                }
            }
            UUID uuid = StorageManager.convert(vol.getFsUuid());
            State state = findOrCreateState(uuid);
            long totalBytes = file.getTotalSpace();
            long usableBytes = file.getUsableSpace();
            int oldLevel2 = state.level;
            if (this.mForceLevel != -1) {
                oldLevel = -1;
                newLevel = this.mForceLevel;
            } else {
                int newLevel2 = (usableBytes > fullBytes ? 1 : (usableBytes == fullBytes ? 0 : -1));
                if (newLevel2 <= 0) {
                    oldLevel = oldLevel2;
                    newLevel = 2;
                } else {
                    int newLevel3 = (usableBytes > lowBytes ? 1 : (usableBytes == lowBytes ? 0 : -1));
                    if (newLevel3 <= 0) {
                        oldLevel = oldLevel2;
                        newLevel = 1;
                    } else if (StorageManager.UUID_DEFAULT.equals(uuid) && usableBytes < BOOT_IMAGE_STORAGE_REQUIREMENT) {
                        oldLevel = oldLevel2;
                        newLevel = 1;
                    } else {
                        oldLevel = oldLevel2;
                        newLevel = 0;
                    }
                }
            }
            if (Math.abs(state.lastUsableBytes - usableBytes) > DEFAULT_LOG_DELTA_BYTES || oldLevel != newLevel) {
                EventLogTags.writeStorageState(uuid.toString(), oldLevel, newLevel, usableBytes, totalBytes);
                state.lastUsableBytes = usableBytes;
            }
            ITranStorageManager.Instance().onDeviceStorageMonitorServiceConstruct(getContext());
            int newLevel4 = newLevel;
            Iterator it2 = it;
            int oldLevel3 = oldLevel;
            ITranStorageManager.Instance().onCheckStorageLevel(getContext(), vol, usableBytes, lowBytes);
            updateNotifications(vol, oldLevel3, newLevel4);
            updateBroadcasts(vol, oldLevel3, newLevel4, seq);
            state.level = newLevel4;
            storage = storage;
            it = it2;
        }
        if (!this.mHandler.hasMessages(1)) {
            Handler handler = this.mHandler;
            handler.sendMessageDelayed(handler.obtainMessage(1), 60000L);
        }
        if (!this.mHandler.hasMessages(2)) {
            Handler handler2 = this.mHandler;
            handler2.sendMessageDelayed(handler2.obtainMessage(2), HIGH_CHECK_INTERVAL);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkHigh() {
        StorageManager storage = (StorageManager) getContext().getSystemService(StorageManager.class);
        int storageThresholdPercentHigh = DeviceConfig.getInt("storage_native_boot", "storage_threshold_percent_high", 20);
        for (VolumeInfo vol : storage.getWritablePrivateVolumes()) {
            File file = vol.getPath();
            if (file.getUsableSpace() < (file.getTotalSpace() * storageThresholdPercentHigh) / 100) {
                PackageManagerInternal pm = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                try {
                    pm.freeAllAppCacheAboveQuota(vol.getFsUuid());
                } catch (IOException e) {
                    Slog.w(TAG, e);
                }
            }
        }
        if (!this.mHandler.hasMessages(2)) {
            Handler handler = this.mHandler;
            handler.sendMessageDelayed(handler.obtainMessage(2), HIGH_CHECK_INTERVAL);
        }
    }

    public DeviceStorageMonitorService(Context context) {
        super(context);
        this.mSeq = new AtomicInteger(1);
        this.mForceLevel = -1;
        this.mStates = new ArrayMap<>();
        this.mLocalService = new DeviceStorageMonitorInternal() { // from class: com.android.server.storage.DeviceStorageMonitorService.2
            @Override // com.android.server.storage.DeviceStorageMonitorInternal
            public void checkMemory() {
                DeviceStorageMonitorService.this.mHandler.removeMessages(1);
                DeviceStorageMonitorService.this.mHandler.obtainMessage(1).sendToTarget();
            }

            @Override // com.android.server.storage.DeviceStorageMonitorInternal
            public boolean isMemoryLow() {
                return Environment.getDataDirectory().getUsableSpace() < getMemoryLowThreshold();
            }

            @Override // com.android.server.storage.DeviceStorageMonitorInternal
            public long getMemoryLowThreshold() {
                return ((StorageManager) DeviceStorageMonitorService.this.getContext().getSystemService(StorageManager.class)).getStorageLowBytes(Environment.getDataDirectory());
            }
        };
        this.mRemoteService = new Binder() { // from class: com.android.server.storage.DeviceStorageMonitorService.3
            @Override // android.os.Binder
            protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
                if (DumpUtils.checkDumpPermission(DeviceStorageMonitorService.this.getContext(), DeviceStorageMonitorService.TAG, pw)) {
                    DeviceStorageMonitorService.this.dumpImpl(fd, pw, args);
                }
            }

            public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
                new Shell().exec(this, in, out, err, args, callback, resultReceiver);
            }
        };
        HandlerThread handlerThread = new HandlerThread(TAG, 10);
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mHandler = new Handler(handlerThread.getLooper()) { // from class: com.android.server.storage.DeviceStorageMonitorService.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        DeviceStorageMonitorService.this.checkLow();
                        return;
                    case 2:
                        DeviceStorageMonitorService.this.checkHigh();
                        return;
                    default:
                        return;
                }
            }
        };
        ITranStorageManager.Instance().onDeviceStorageMonitorServiceConstruct(getContext());
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        Context context = getContext();
        this.mNotifManager = (NotificationManager) context.getSystemService(NotificationManager.class);
        CacheFileDeletedObserver cacheFileDeletedObserver = new CacheFileDeletedObserver();
        this.mCacheFileDeletedObserver = cacheFileDeletedObserver;
        cacheFileDeletedObserver.startWatching();
        PackageManager packageManager = context.getPackageManager();
        boolean isTv = packageManager.hasSystemFeature("android.software.leanback");
        if (isTv) {
            this.mNotifManager.createNotificationChannel(new NotificationChannel(TV_NOTIFICATION_CHANNEL_ID, context.getString(17040158), 4));
        }
        publishBinderService(SERVICE, this.mRemoteService);
        publishLocalService(DeviceStorageMonitorInternal.class, this.mLocalService);
        this.mHandler.removeMessages(1);
        this.mHandler.obtainMessage(1).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class Shell extends ShellCommand {
        Shell() {
        }

        public int onCommand(String cmd) {
            return DeviceStorageMonitorService.this.onShellCommand(this, cmd);
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            DeviceStorageMonitorService.dumpHelp(pw);
        }
    }

    int parseOptions(Shell shell) {
        int opts = 0;
        while (true) {
            String opt = shell.getNextOption();
            if (opt != null) {
                if ("-f".equals(opt)) {
                    opts |= 1;
                }
            } else {
                return opts;
            }
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    int onShellCommand(Shell shell, String cmd) {
        char c;
        if (cmd == null) {
            return shell.handleDefaultCommands(cmd);
        }
        PrintWriter pw = shell.getOutPrintWriter();
        switch (cmd.hashCode()) {
            case 108404047:
                if (cmd.equals("reset")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 1526871410:
                if (cmd.equals("force-low")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 1692300408:
                if (cmd.equals("force-not-low")) {
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
                int opts = parseOptions(shell);
                getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                this.mForceLevel = 1;
                int seq = this.mSeq.incrementAndGet();
                if ((opts & 1) != 0) {
                    this.mHandler.removeMessages(1);
                    this.mHandler.obtainMessage(1).sendToTarget();
                    pw.println(seq);
                    break;
                }
                break;
            case 1:
                int opts2 = parseOptions(shell);
                getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                this.mForceLevel = 0;
                int seq2 = this.mSeq.incrementAndGet();
                if ((opts2 & 1) != 0) {
                    this.mHandler.removeMessages(1);
                    this.mHandler.obtainMessage(1).sendToTarget();
                    pw.println(seq2);
                    break;
                }
                break;
            case 2:
                int opts3 = parseOptions(shell);
                getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                this.mForceLevel = -1;
                int seq3 = this.mSeq.incrementAndGet();
                if ((opts3 & 1) != 0) {
                    this.mHandler.removeMessages(1);
                    this.mHandler.obtainMessage(1).sendToTarget();
                    pw.println(seq3);
                    break;
                }
                break;
            default:
                return shell.handleDefaultCommands(cmd);
        }
        return 0;
    }

    static void dumpHelp(PrintWriter pw) {
        pw.println("Device storage monitor service (devicestoragemonitor) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("  force-low [-f]");
        pw.println("    Force storage to be low, freezing storage state.");
        pw.println("    -f: force a storage change broadcast be sent, prints new sequence.");
        pw.println("  force-not-low [-f]");
        pw.println("    Force storage to not be low, freezing storage state.");
        pw.println("    -f: force a storage change broadcast be sent, prints new sequence.");
        pw.println("  reset [-f]");
        pw.println("    Unfreeze storage state, returning to current real values.");
        pw.println("    -f: force a storage change broadcast be sent, prints new sequence.");
    }

    void dumpImpl(FileDescriptor fd, PrintWriter _pw, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(_pw, "  ");
        if (args == null || args.length == 0 || "-a".equals(args[0])) {
            StorageManager storage = (StorageManager) getContext().getSystemService(StorageManager.class);
            pw.println("Known volumes:");
            pw.increaseIndent();
            for (int i = 0; i < this.mStates.size(); i++) {
                UUID uuid = this.mStates.keyAt(i);
                State state = this.mStates.valueAt(i);
                if (StorageManager.UUID_DEFAULT.equals(uuid)) {
                    pw.println("Default:");
                } else {
                    pw.println(uuid + ":");
                }
                pw.increaseIndent();
                pw.printPair("level", State.levelToString(state.level));
                pw.printPair("lastUsableBytes", Long.valueOf(state.lastUsableBytes));
                pw.println();
                Iterator it = storage.getWritablePrivateVolumes().iterator();
                while (true) {
                    if (it.hasNext()) {
                        VolumeInfo vol = (VolumeInfo) it.next();
                        File file = vol.getPath();
                        UUID innerUuid = StorageManager.convert(vol.getFsUuid());
                        if (Objects.equals(uuid, innerUuid)) {
                            pw.print("lowBytes=");
                            pw.print(storage.getStorageLowBytes(file));
                            pw.print(" fullBytes=");
                            pw.println(storage.getStorageFullBytes(file));
                            pw.print("path=");
                            pw.println(file);
                            break;
                        }
                    }
                }
                pw.decreaseIndent();
            }
            pw.decreaseIndent();
            pw.println();
            pw.printPair("mSeq", Integer.valueOf(this.mSeq.get()));
            pw.printPair("mForceState", State.levelToString(this.mForceLevel));
            pw.println();
            pw.println();
            return;
        }
        Shell shell = new Shell();
        shell.exec(this.mRemoteService, null, fd, null, args, null, new ResultReceiver(null));
    }

    private void updateNotifications(VolumeInfo vol, int oldLevel, int newLevel) {
        Context context = getContext();
        UUID uuid = StorageManager.convert(vol.getFsUuid());
        if (State.isEntering(1, oldLevel, newLevel)) {
            Intent lowMemIntent = new Intent("android.os.storage.action.MANAGE_STORAGE");
            lowMemIntent.putExtra("android.os.storage.extra.UUID", uuid);
            lowMemIntent.addFlags(268435456);
            CharSequence title = context.getText(17040665);
            ITranStorageManager.Instance().onEnterLowStorage(getContext(), uuid, lowMemIntent, title);
            FrameworkStatsLog.write(130, Objects.toString(vol.getDescription()), 2);
        } else if (State.isLeaving(1, oldLevel, newLevel)) {
            this.mNotifManager.cancelAsUser(uuid.toString(), 23, UserHandle.ALL);
            FrameworkStatsLog.write(130, Objects.toString(vol.getDescription()), 1);
        }
    }

    private void updateBroadcasts(VolumeInfo vol, int oldLevel, int newLevel, int seq) {
        if (!Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, vol.getFsUuid())) {
            return;
        }
        Intent lowIntent = new Intent("android.intent.action.DEVICE_STORAGE_LOW").addFlags(85983232).putExtra(EXTRA_SEQUENCE, seq);
        Intent notLowIntent = new Intent("android.intent.action.DEVICE_STORAGE_OK").addFlags(85983232).putExtra(EXTRA_SEQUENCE, seq);
        if (State.isEntering(1, oldLevel, newLevel)) {
            getContext().sendStickyBroadcastAsUser(lowIntent, UserHandle.ALL);
        } else if (State.isLeaving(1, oldLevel, newLevel)) {
            getContext().removeStickyBroadcastAsUser(lowIntent, UserHandle.ALL);
            getContext().sendBroadcastAsUser(notLowIntent, UserHandle.ALL);
        }
        Intent fullIntent = new Intent("android.intent.action.DEVICE_STORAGE_FULL").addFlags(67108864).putExtra(EXTRA_SEQUENCE, seq);
        Intent notFullIntent = new Intent("android.intent.action.DEVICE_STORAGE_NOT_FULL").addFlags(67108864).putExtra(EXTRA_SEQUENCE, seq);
        if (State.isEntering(2, oldLevel, newLevel)) {
            getContext().sendStickyBroadcastAsUser(fullIntent, UserHandle.ALL);
        } else if (State.isLeaving(2, oldLevel, newLevel)) {
            getContext().removeStickyBroadcastAsUser(fullIntent, UserHandle.ALL);
            getContext().sendBroadcastAsUser(notFullIntent, UserHandle.ALL);
        }
    }

    /* loaded from: classes2.dex */
    private static class CacheFileDeletedObserver extends FileObserver {
        public CacheFileDeletedObserver() {
            super(Environment.getDownloadCacheDirectory().getAbsolutePath(), 512);
        }

        @Override // android.os.FileObserver
        public void onEvent(int event, String path) {
            EventLogTags.writeCacheFileDeleted(path);
        }
    }
}
