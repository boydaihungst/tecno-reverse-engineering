package com.android.server.os;

import android.app.ApplicationExitInfo;
import android.app.IParcelFileDescriptorRetriever;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.FileObserver;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.UserHandle;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStat;
import android.util.Slog;
import android.util.SparseArray;
import android.util.proto.ProtoInputStream;
import android.util.proto.ProtoParseException;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.BootReceiver;
import com.android.server.ServiceThread;
import com.android.server.os.NativeTombstoneManager;
import defpackage.CompanionAppsPermissions;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import libcore.io.IoUtils;
/* loaded from: classes2.dex */
public final class NativeTombstoneManager {
    private static final String TAG = NativeTombstoneManager.class.getSimpleName();
    private static final File TOMBSTONE_DIR = new File("/data/tombstones");
    private final Context mContext;
    private final Handler mHandler;
    private final Object mLock = new Object();
    private final SparseArray<TombstoneFile> mTombstones = new SparseArray<>();
    private final TombstoneWatcher mWatcher;

    /* JADX INFO: Access modifiers changed from: package-private */
    public NativeTombstoneManager(Context context) {
        this.mContext = context;
        ServiceThread thread = new ServiceThread(TAG + ":tombstoneWatcher", 10, true);
        thread.start();
        this.mHandler = thread.getThreadHandler();
        TombstoneWatcher tombstoneWatcher = new TombstoneWatcher();
        this.mWatcher = tombstoneWatcher;
        tombstoneWatcher.startWatching();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSystemReady() {
        registerForUserRemoval();
        registerForPackageRemoval();
        this.mHandler.post(new Runnable() { // from class: com.android.server.os.NativeTombstoneManager$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                NativeTombstoneManager.this.m5279xa1ff5078();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onSystemReady$0$com-android-server-os-NativeTombstoneManager  reason: not valid java name */
    public /* synthetic */ void m5279xa1ff5078() {
        File[] tombstoneFiles = TOMBSTONE_DIR.listFiles();
        for (int i = 0; tombstoneFiles != null && i < tombstoneFiles.length; i++) {
            if (tombstoneFiles[i].isFile()) {
                handleTombstone(tombstoneFiles[i]);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleTombstone(File path) {
        String filename = path.getName();
        if (!filename.startsWith("tombstone_")) {
            return;
        }
        String processName = "UNKNOWN";
        boolean isProtoFile = filename.endsWith(".pb");
        File protoPath = isProtoFile ? path : new File(path.getAbsolutePath() + ".pb");
        Optional<TombstoneFile> parsedTombstone = handleProtoTombstone(protoPath, isProtoFile);
        if (parsedTombstone.isPresent()) {
            processName = parsedTombstone.get().getProcessName();
        }
        BootReceiver.addTombstoneToDropBox(this.mContext, path, isProtoFile, processName);
    }

    private Optional<TombstoneFile> handleProtoTombstone(File path, boolean addToList) {
        String filename = path.getName();
        if (!filename.endsWith(".pb")) {
            Slog.w(TAG, "unexpected tombstone name: " + path);
            return Optional.empty();
        }
        String suffix = filename.substring("tombstone_".length());
        String numberStr = suffix.substring(0, suffix.length() - 3);
        try {
            int number = Integer.parseInt(numberStr);
            if (number < 0 || number > 99) {
                Slog.w(TAG, "unexpected tombstone name: " + path);
                return Optional.empty();
            }
            try {
                ParcelFileDescriptor pfd = ParcelFileDescriptor.open(path, 805306368);
                Optional<TombstoneFile> parsedTombstone = TombstoneFile.parse(pfd);
                if (!parsedTombstone.isPresent()) {
                    IoUtils.closeQuietly(pfd);
                    return Optional.empty();
                }
                if (addToList) {
                    synchronized (this.mLock) {
                        TombstoneFile previous = this.mTombstones.get(number);
                        if (previous != null) {
                            previous.dispose();
                        }
                        this.mTombstones.put(number, parsedTombstone.get());
                    }
                }
                return parsedTombstone;
            } catch (FileNotFoundException ex) {
                Slog.w(TAG, "failed to open " + path, ex);
                return Optional.empty();
            }
        } catch (NumberFormatException e) {
            Slog.w(TAG, "unexpected tombstone name: " + path);
            return Optional.empty();
        }
    }

    public void purge(final Optional<Integer> userId, final Optional<Integer> appId) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.os.NativeTombstoneManager$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                NativeTombstoneManager.this.m5280lambda$purge$1$comandroidserverosNativeTombstoneManager(userId, appId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$purge$1$com-android-server-os-NativeTombstoneManager  reason: not valid java name */
    public /* synthetic */ void m5280lambda$purge$1$comandroidserverosNativeTombstoneManager(Optional userId, Optional appId) {
        synchronized (this.mLock) {
            for (int i = this.mTombstones.size() - 1; i >= 0; i--) {
                TombstoneFile tombstone = this.mTombstones.valueAt(i);
                if (tombstone.matches(userId, appId)) {
                    tombstone.purge();
                    this.mTombstones.removeAt(i);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void purgePackage(int uid, boolean allUsers) {
        Optional<Integer> userId;
        int appId = UserHandle.getAppId(uid);
        if (allUsers) {
            userId = Optional.empty();
        } else {
            userId = Optional.of(Integer.valueOf(UserHandle.getUserId(uid)));
        }
        purge(userId, Optional.of(Integer.valueOf(appId)));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void purgeUser(int uid) {
        purge(Optional.of(Integer.valueOf(uid)), Optional.empty());
    }

    private void registerForPackageRemoval() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_FULLY_REMOVED");
        filter.addDataScheme("package");
        this.mContext.registerReceiverForAllUsers(new BroadcastReceiver() { // from class: com.android.server.os.NativeTombstoneManager.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                int uid = intent.getIntExtra("android.intent.extra.UID", -10000);
                if (uid == -10000) {
                    return;
                }
                boolean allUsers = intent.getBooleanExtra("android.intent.extra.REMOVED_FOR_ALL_USERS", false);
                NativeTombstoneManager.this.purgePackage(uid, allUsers);
            }
        }, filter, null, this.mHandler);
    }

    private void registerForUserRemoval() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiverForAllUsers(new BroadcastReceiver() { // from class: com.android.server.os.NativeTombstoneManager.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                if (userId < 1) {
                    return;
                }
                NativeTombstoneManager.this.purgeUser(userId);
            }
        }, filter, null, this.mHandler);
    }

    public void collectTombstones(final ArrayList<ApplicationExitInfo> output, int callingUid, final int pid, final int maxNum) {
        final CompletableFuture<Object> future = new CompletableFuture<>();
        if (!UserHandle.isApp(callingUid)) {
            return;
        }
        final int userId = UserHandle.getUserId(callingUid);
        final int appId = UserHandle.getAppId(callingUid);
        this.mHandler.post(new Runnable() { // from class: com.android.server.os.NativeTombstoneManager$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                NativeTombstoneManager.this.m5278xd70f5d02(userId, appId, pid, output, maxNum, future);
            }
        });
        try {
            future.get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$collectTombstones$3$com-android-server-os-NativeTombstoneManager  reason: not valid java name */
    public /* synthetic */ void m5278xd70f5d02(int userId, int appId, int pid, ArrayList output, int maxNum, CompletableFuture future) {
        boolean appendedTombstones = false;
        synchronized (this.mLock) {
            int tombstonesSize = this.mTombstones.size();
            for (int i = 0; i < tombstonesSize; i++) {
                TombstoneFile tombstone = this.mTombstones.valueAt(i);
                if (tombstone.matches(Optional.of(Integer.valueOf(userId)), Optional.of(Integer.valueOf(appId))) && (pid == 0 || tombstone.mPid == pid)) {
                    int outputSize = output.size();
                    int j = 0;
                    while (true) {
                        if (j < outputSize) {
                            ApplicationExitInfo exitInfo = (ApplicationExitInfo) output.get(j);
                            if (!tombstone.matches(exitInfo)) {
                                j++;
                            } else {
                                exitInfo.setNativeTombstoneRetriever(tombstone.getPfdRetriever());
                                break;
                            }
                        } else {
                            int j2 = output.size();
                            if (j2 < maxNum) {
                                appendedTombstones = true;
                                output.add(tombstone.toAppExitInfo());
                            }
                        }
                    }
                }
            }
        }
        if (appendedTombstones) {
            Collections.sort(output, new Comparator() { // from class: com.android.server.os.NativeTombstoneManager$$ExternalSyntheticLambda3
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    return NativeTombstoneManager.lambda$collectTombstones$2((ApplicationExitInfo) obj, (ApplicationExitInfo) obj2);
                }
            });
        }
        future.complete(null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ int lambda$collectTombstones$2(ApplicationExitInfo lhs, ApplicationExitInfo rhs) {
        long diff = rhs.getTimestamp() - lhs.getTimestamp();
        if (diff < 0) {
            return -1;
        }
        if (diff == 0) {
            return 0;
        }
        return 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class TombstoneFile {
        int mAppId;
        String mCrashReason;
        final ParcelFileDescriptor mPfd;
        int mPid;
        String mProcessName;
        boolean mPurged = false;
        final IParcelFileDescriptorRetriever mRetriever = new ParcelFileDescriptorRetriever();
        long mTimestampMs;
        int mUid;
        int mUserId;

        TombstoneFile(ParcelFileDescriptor pfd) {
            this.mPfd = pfd;
        }

        public boolean matches(Optional<Integer> userId, Optional<Integer> appId) {
            if (this.mPurged) {
                return false;
            }
            if (!userId.isPresent() || userId.get().intValue() == this.mUserId) {
                return !appId.isPresent() || appId.get().intValue() == this.mAppId;
            }
            return false;
        }

        public boolean matches(ApplicationExitInfo exitInfo) {
            return exitInfo.getReason() == 5 && exitInfo.getPid() == this.mPid && exitInfo.getRealUid() == this.mUid && Math.abs(exitInfo.getTimestamp() - this.mTimestampMs) <= 5000;
        }

        public String getProcessName() {
            return this.mProcessName;
        }

        public void dispose() {
            IoUtils.closeQuietly(this.mPfd);
        }

        public void purge() {
            if (!this.mPurged) {
                try {
                    Os.ftruncate(this.mPfd.getFileDescriptor(), 0L);
                } catch (ErrnoException ex) {
                    Slog.e(NativeTombstoneManager.TAG, "Failed to truncate tombstone", ex);
                }
                this.mPurged = true;
            }
        }

        static Optional<TombstoneFile> parse(ParcelFileDescriptor pfd) {
            FileInputStream is = new FileInputStream(pfd.getFileDescriptor());
            ProtoInputStream stream = new ProtoInputStream(is);
            String selinuxLabel = "";
            String crashReason = "";
            String crashReason2 = null;
            int uid = 0;
            int uid2 = 0;
            while (true) {
                try {
                    int pid = stream.nextField();
                    if (pid != -1) {
                        switch (stream.getFieldNumber()) {
                            case 5:
                                uid2 = stream.readInt(1155346202629L);
                                break;
                            case 7:
                                uid = stream.readInt(1155346202631L);
                                break;
                            case 8:
                                selinuxLabel = stream.readString(1138166333448L);
                                break;
                            case 9:
                                if (crashReason2 != null) {
                                    break;
                                } else {
                                    crashReason2 = stream.readString(2237677961225L);
                                    break;
                                }
                            case 15:
                                if (!crashReason.equals("")) {
                                    break;
                                } else {
                                    long token = stream.start(2246267895823L);
                                    while (true) {
                                        if (stream.nextField() != -1) {
                                            switch (stream.getFieldNumber()) {
                                                case 1:
                                                    String crashReason3 = stream.readString((long) CompanionAppsPermissions.AppPermissions.PACKAGE_NAME);
                                                    crashReason = crashReason3;
                                                    break;
                                            }
                                        }
                                    }
                                    stream.end(token);
                                    break;
                                }
                        }
                    } else if (!UserHandle.isApp(uid)) {
                        Slog.e(NativeTombstoneManager.TAG, "Tombstone's UID (" + uid + ") not an app, ignoring");
                        return Optional.empty();
                    } else {
                        long timestampMs = 0;
                        try {
                            StructStat stat = Os.fstat(pfd.getFileDescriptor());
                            timestampMs = (stat.st_atim.tv_sec * 1000) + (stat.st_atim.tv_nsec / 1000000);
                        } catch (ErrnoException ex) {
                            Slog.e(NativeTombstoneManager.TAG, "Failed to get timestamp of tombstone", ex);
                        }
                        int userId = UserHandle.getUserId(uid);
                        int appId = UserHandle.getAppId(uid);
                        if (!selinuxLabel.startsWith("u:r:untrusted_app")) {
                            Slog.e(NativeTombstoneManager.TAG, "Tombstone has invalid selinux label (" + selinuxLabel + "), ignoring");
                            return Optional.empty();
                        }
                        TombstoneFile result = new TombstoneFile(pfd);
                        result.mUserId = userId;
                        result.mAppId = appId;
                        result.mPid = uid2;
                        result.mUid = uid;
                        result.mProcessName = crashReason2 != null ? crashReason2 : "";
                        result.mTimestampMs = timestampMs;
                        result.mCrashReason = crashReason;
                        return Optional.of(result);
                    }
                } catch (IOException | ProtoParseException ex2) {
                    Slog.e(NativeTombstoneManager.TAG, "Failed to parse tombstone", ex2);
                    return Optional.empty();
                }
            }
        }

        public IParcelFileDescriptorRetriever getPfdRetriever() {
            return this.mRetriever;
        }

        public ApplicationExitInfo toAppExitInfo() {
            ApplicationExitInfo info = new ApplicationExitInfo();
            info.setPid(this.mPid);
            info.setRealUid(this.mUid);
            info.setPackageUid(this.mUid);
            info.setDefiningUid(this.mUid);
            info.setProcessName(this.mProcessName);
            info.setReason(5);
            info.setStatus(0);
            info.setImportance(1000);
            info.setPackageName("");
            info.setProcessStateSummary(null);
            info.setPss(0L);
            info.setRss(0L);
            info.setTimestamp(this.mTimestampMs);
            info.setDescription(this.mCrashReason);
            info.setSubReason(0);
            info.setNativeTombstoneRetriever(this.mRetriever);
            return info;
        }

        /* loaded from: classes2.dex */
        class ParcelFileDescriptorRetriever extends IParcelFileDescriptorRetriever.Stub {
            ParcelFileDescriptorRetriever() {
            }

            public ParcelFileDescriptor getPfd() {
                if (TombstoneFile.this.mPurged) {
                    return null;
                }
                try {
                    String path = "/proc/self/fd/" + TombstoneFile.this.mPfd.getFd();
                    ParcelFileDescriptor pfd = ParcelFileDescriptor.open(new File(path), 268435456);
                    return pfd;
                } catch (FileNotFoundException ex) {
                    Slog.e(NativeTombstoneManager.TAG, "failed to reopen file descriptor as read-only", ex);
                    return null;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class TombstoneWatcher extends FileObserver {
        TombstoneWatcher() {
            super(NativeTombstoneManager.TOMBSTONE_DIR, (int) FrameworkStatsLog.NON_A11Y_TOOL_SERVICE_WARNING_REPORT);
        }

        @Override // android.os.FileObserver
        public void onEvent(int event, final String path) {
            NativeTombstoneManager.this.mHandler.post(new Runnable() { // from class: com.android.server.os.NativeTombstoneManager$TombstoneWatcher$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    NativeTombstoneManager.TombstoneWatcher.this.m5281x8eaa4299(path);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onEvent$0$com-android-server-os-NativeTombstoneManager$TombstoneWatcher  reason: not valid java name */
        public /* synthetic */ void m5281x8eaa4299(String path) {
            NativeTombstoneManager.this.handleTombstone(new File(NativeTombstoneManager.TOMBSTONE_DIR, path));
        }
    }
}
