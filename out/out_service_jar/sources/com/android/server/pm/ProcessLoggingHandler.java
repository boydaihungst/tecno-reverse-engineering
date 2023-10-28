package com.android.server.pm;

import android.app.admin.SecurityLog;
import android.content.Context;
import android.content.pm.ApkChecksum;
import android.content.pm.IOnChecksumsReadyListener;
import android.content.pm.PackageManagerInternal;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import com.android.server.net.watchlist.WatchlistLoggingHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
/* loaded from: classes2.dex */
public final class ProcessLoggingHandler extends Handler {
    private static final int CHECKSUM_TYPE = 8;
    private static final String TAG = "ProcessLoggingHandler";
    private final Executor mExecutor;
    private final ArrayMap<String, LoggingInfo> mLoggingInfo;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class LoggingInfo {
        public String apkHash = null;
        public List<Bundle> pendingLogEntries = new ArrayList();

        LoggingInfo() {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProcessLoggingHandler() {
        super(BackgroundThread.getHandler().getLooper());
        this.mExecutor = new HandlerExecutor(this);
        this.mLoggingInfo = new ArrayMap<>();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void logAppProcessStart(Context context, PackageManagerInternal pmi, String apkFile, String packageName, String processName, int uid, String seinfo, int pid) {
        boolean requestChecksums;
        final LoggingInfo loggingInfo;
        LoggingInfo loggingInfo2;
        LoggingInfo loggingInfo3;
        Bundle data = new Bundle();
        data.putLong("startTimestamp", System.currentTimeMillis());
        data.putString("processName", processName);
        data.putInt(WatchlistLoggingHandler.WatchlistEventKeys.UID, uid);
        data.putString("seinfo", seinfo);
        data.putInt("pid", pid);
        if (apkFile == null) {
            enqueueSecurityLogEvent(data, "No APK");
            return;
        }
        synchronized (this.mLoggingInfo) {
            LoggingInfo cached = this.mLoggingInfo.get(apkFile);
            requestChecksums = cached == null;
            if (requestChecksums) {
                cached = new LoggingInfo();
                this.mLoggingInfo.put(apkFile, cached);
            }
            loggingInfo = cached;
        }
        synchronized (loggingInfo) {
            try {
                if (!TextUtils.isEmpty(loggingInfo.apkHash)) {
                    try {
                        enqueueSecurityLogEvent(data, loggingInfo.apkHash);
                        return;
                    } catch (Throwable th) {
                        th = th;
                        loggingInfo2 = loggingInfo;
                        while (true) {
                            try {
                                break;
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        }
                        throw th;
                    }
                }
                loggingInfo.pendingLogEntries.add(data);
                if (!requestChecksums) {
                    return;
                }
                try {
                    loggingInfo3 = loggingInfo;
                    try {
                        pmi.requestChecksums(packageName, false, 0, 8, null, new IOnChecksumsReadyListener.Stub() { // from class: com.android.server.pm.ProcessLoggingHandler.1
                            public void onChecksumsReady(List<ApkChecksum> checksums) throws RemoteException {
                                ProcessLoggingHandler.this.processChecksums(loggingInfo, checksums);
                            }
                        }, context.getUserId(), this.mExecutor, this);
                    } catch (Throwable th3) {
                        t = th3;
                        Slog.e(TAG, "requestChecksums() failed", t);
                        enqueueProcessChecksum(loggingInfo3, null);
                    }
                } catch (Throwable th4) {
                    t = th4;
                    loggingInfo3 = loggingInfo;
                }
            } catch (Throwable th5) {
                th = th5;
                loggingInfo2 = loggingInfo;
            }
        }
    }

    void processChecksums(LoggingInfo loggingInfo, List<ApkChecksum> checksums) {
        int size = checksums.size();
        for (int i = 0; i < size; i++) {
            ApkChecksum checksum = checksums.get(i);
            if (checksum.getType() == 8) {
                processChecksum(loggingInfo, checksum.getValue());
                return;
            }
        }
        Slog.e(TAG, "requestChecksums() failed to return SHA256, see logs for details.");
        processChecksum(loggingInfo, null);
    }

    void enqueueProcessChecksum(final LoggingInfo loggingInfo, byte[] hash) {
        post(new Runnable() { // from class: com.android.server.pm.ProcessLoggingHandler$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                ProcessLoggingHandler.this.m5601x9ebb04b9(loggingInfo);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$enqueueProcessChecksum$0$com-android-server-pm-ProcessLoggingHandler  reason: not valid java name */
    public /* synthetic */ void m5601x9ebb04b9(LoggingInfo loggingInfo) {
        processChecksum(loggingInfo, null);
    }

    void processChecksum(LoggingInfo loggingInfo, byte[] hash) {
        String apkHash;
        if (hash != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < hash.length; i++) {
                sb.append(String.format("%02x", Byte.valueOf(hash[i])));
            }
            apkHash = sb.toString();
        } else {
            apkHash = "Failed to count APK hash";
        }
        synchronized (loggingInfo) {
            if (TextUtils.isEmpty(loggingInfo.apkHash)) {
                loggingInfo.apkHash = apkHash;
                List<Bundle> pendingLogEntries = loggingInfo.pendingLogEntries;
                loggingInfo.pendingLogEntries = null;
                if (pendingLogEntries != null) {
                    for (Bundle data : pendingLogEntries) {
                        m5602x30f8dcf4(data, apkHash);
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void invalidateBaseApkHash(String apkFile) {
        synchronized (this.mLoggingInfo) {
            this.mLoggingInfo.remove(apkFile);
        }
    }

    void enqueueSecurityLogEvent(final Bundle data, final String apkHash) {
        post(new Runnable() { // from class: com.android.server.pm.ProcessLoggingHandler$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ProcessLoggingHandler.this.m5602x30f8dcf4(data, apkHash);
            }
        });
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: logSecurityLogEvent */
    public void m5602x30f8dcf4(Bundle bundle, String apkHash) {
        long startTimestamp = bundle.getLong("startTimestamp");
        String processName = bundle.getString("processName");
        int uid = bundle.getInt(WatchlistLoggingHandler.WatchlistEventKeys.UID);
        String seinfo = bundle.getString("seinfo");
        int pid = bundle.getInt("pid");
        SecurityLog.writeEvent(210005, new Object[]{processName, Long.valueOf(startTimestamp), Integer.valueOf(uid), Integer.valueOf(pid), seinfo, apkHash});
    }
}
