package com.android.server.am;

import android.util.ArrayMap;
import android.util.TimeUtils;
import java.io.PrintWriter;
import java.util.ArrayList;
/* loaded from: classes.dex */
public final class ProcessProviderRecord {
    final ProcessRecord mApp;
    private long mLastProviderTime;
    private final ActivityManagerService mService;
    private final ArrayMap<String, ContentProviderRecord> mPubProviders = new ArrayMap<>();
    private final ArrayList<ContentProviderConnection> mConProviders = new ArrayList<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastProviderTime() {
        return this.mLastProviderTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastProviderTime(long lastProviderTime) {
        this.mLastProviderTime = lastProviderTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasProvider(String name) {
        return this.mPubProviders.containsKey(name);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ContentProviderRecord getProvider(String name) {
        return this.mPubProviders.get(name);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int numberOfProviders() {
        return this.mPubProviders.size();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ContentProviderRecord getProviderAt(int index) {
        return this.mPubProviders.valueAt(index);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void installProvider(String name, ContentProviderRecord provider) {
        this.mPubProviders.put(name, provider);
    }

    void removeProvider(String name) {
        this.mPubProviders.remove(name);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void ensureProviderCapacity(int capacity) {
        this.mPubProviders.ensureCapacity(capacity);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int numberOfProviderConnections() {
        return this.mConProviders.size();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ContentProviderConnection getProviderConnectionAt(int index) {
        return this.mConProviders.get(index);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addProviderConnection(ContentProviderConnection connection) {
        this.mConProviders.add(connection);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean removeProviderConnection(ContentProviderConnection connection) {
        return this.mConProviders.remove(connection);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProcessProviderRecord(ProcessRecord app) {
        this.mApp = app;
        this.mService = app.mService;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean onCleanupApplicationRecordLocked(boolean allowRestart) {
        boolean restart = false;
        int i = this.mPubProviders.size() - 1;
        while (true) {
            boolean alwaysRemove = false;
            if (i < 0) {
                break;
            }
            ContentProviderRecord cpr = this.mPubProviders.valueAt(i);
            ProcessRecord processRecord = cpr.proc;
            ProcessRecord processRecord2 = this.mApp;
            if (processRecord == processRecord2) {
                alwaysRemove = (processRecord2.mErrorState.isBad() || !allowRestart) ? true : true;
                boolean inLaunching = this.mService.mCpHelper.removeDyingProviderLocked(this.mApp, cpr, alwaysRemove);
                if (!alwaysRemove && inLaunching && cpr.hasConnectionOrHandle()) {
                    restart = true;
                }
                cpr.provider = null;
                cpr.setProcess(null);
            }
            i--;
        }
        this.mPubProviders.clear();
        if (this.mService.mCpHelper.cleanupAppInLaunchingProvidersLocked(this.mApp, false)) {
            this.mService.mProcessList.noteProcessDiedLocked(this.mApp);
            restart = true;
        }
        if (!this.mConProviders.isEmpty()) {
            for (int i2 = this.mConProviders.size() - 1; i2 >= 0; i2--) {
                ContentProviderConnection conn = this.mConProviders.get(i2);
                conn.provider.connections.remove(conn);
                this.mService.stopAssociationLocked(this.mApp.uid, this.mApp.processName, conn.provider.uid, conn.provider.appInfo.longVersionCode, conn.provider.name, conn.provider.info.processName);
            }
            this.mConProviders.clear();
        }
        return restart;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix, long nowUptime) {
        if (this.mLastProviderTime > 0) {
            pw.print(prefix);
            pw.print("lastProviderTime=");
            TimeUtils.formatDuration(this.mLastProviderTime, nowUptime, pw);
            pw.println();
        }
        if (this.mPubProviders.size() > 0) {
            pw.print(prefix);
            pw.println("Published Providers:");
            int size = this.mPubProviders.size();
            for (int i = 0; i < size; i++) {
                pw.print(prefix);
                pw.print("  - ");
                pw.println(this.mPubProviders.keyAt(i));
                pw.print(prefix);
                pw.print("    -> ");
                pw.println(this.mPubProviders.valueAt(i));
            }
        }
        if (this.mConProviders.size() > 0) {
            pw.print(prefix);
            pw.println("Connected Providers:");
            int size2 = this.mConProviders.size();
            for (int i2 = 0; i2 < size2; i2++) {
                pw.print(prefix);
                pw.print("  - ");
                pw.println(this.mConProviders.get(i2).toShortString());
            }
        }
    }
}
