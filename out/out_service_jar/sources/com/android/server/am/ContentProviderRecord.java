package com.android.server.am;

import android.app.ContentProviderHolder;
import android.app.IApplicationThread;
import android.content.ComponentName;
import android.content.IContentProvider;
import android.content.pm.ApplicationInfo;
import android.content.pm.ProviderInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.app.procstats.AssociationState;
import com.android.internal.app.procstats.ProcessStats;
import com.android.server.slice.SliceClientPermissions;
import com.transsion.hubcore.griffin.lib.app.TranAppInfo;
import com.transsion.hubcore.server.am.ITranContentProviderRecord;
import java.io.PrintWriter;
import java.util.ArrayList;
/* loaded from: classes.dex */
public final class ContentProviderRecord implements ComponentName.WithComponentName {
    static final int MAX_RETRY_COUNT = 3;
    final ApplicationInfo appInfo;
    final ArrayList<ContentProviderConnection> connections = new ArrayList<>();
    int externalProcessNoHandleCount;
    ArrayMap<IBinder, ExternalProcessHandle> externalProcessTokenToHandle;
    public final TranAppInfo grifAppInfo;
    public final ProviderInfo info;
    ProcessRecord launchingApp;
    int mRestartCount;
    final ComponentName name;
    public boolean noReleaseNeeded;
    ProcessRecord proc;
    public IContentProvider provider;
    final ActivityManagerService service;
    String shortStringName;
    final boolean singleton;
    String stringName;
    final int uid;

    public ContentProviderRecord(ActivityManagerService _service, ProviderInfo _info, ApplicationInfo ai, ComponentName _name, boolean _singleton) {
        this.service = _service;
        this.info = _info;
        int i = ai.uid;
        this.uid = i;
        this.appInfo = ai;
        this.name = _name;
        this.singleton = _singleton;
        this.noReleaseNeeded = (i == 0 || i == 1000) && (_name == null || !("com.android.settings".equals(_name.getPackageName()) || "com.transsion.deskclock".equals(_name.getPackageName()) || "com.transsion.ossettingsext".equals(_name.getPackageName())));
        this.grifAppInfo = ITranContentProviderRecord.Instance().hookAppInfoInstance(ai.packageName);
    }

    public ContentProviderRecord(ContentProviderRecord cpr) {
        this.service = cpr.service;
        this.info = cpr.info;
        this.uid = cpr.uid;
        ApplicationInfo applicationInfo = cpr.appInfo;
        this.appInfo = applicationInfo;
        this.name = cpr.name;
        this.singleton = cpr.singleton;
        this.noReleaseNeeded = cpr.noReleaseNeeded;
        this.grifAppInfo = ITranContentProviderRecord.Instance().hookAppInfoCopy(applicationInfo.packageName);
    }

    public ContentProviderHolder newHolder(ContentProviderConnection conn, boolean local) {
        ContentProviderHolder holder = new ContentProviderHolder(this.info);
        holder.provider = this.provider;
        holder.noReleaseNeeded = this.noReleaseNeeded;
        holder.connection = conn;
        holder.mLocal = local;
        return holder;
    }

    public void setProcess(ProcessRecord proc) {
        this.proc = proc;
        for (int iconn = this.connections.size() - 1; iconn >= 0; iconn--) {
            ContentProviderConnection conn = this.connections.get(iconn);
            if (proc != null) {
                conn.startAssociationIfNeeded();
            } else {
                conn.stopAssociation();
            }
        }
        ArrayMap<IBinder, ExternalProcessHandle> arrayMap = this.externalProcessTokenToHandle;
        if (arrayMap != null) {
            for (int iext = arrayMap.size() - 1; iext >= 0; iext--) {
                ExternalProcessHandle handle = this.externalProcessTokenToHandle.valueAt(iext);
                if (proc != null) {
                    handle.startAssociationIfNeeded(this);
                } else {
                    handle.stopAssociation();
                }
            }
        }
    }

    public boolean canRunHere(ProcessRecord app) {
        return (this.info.multiprocess || this.info.processName.equals(app.processName)) && this.uid == app.info.uid;
    }

    public void addExternalProcessHandleLocked(IBinder token, int callingUid, String callingTag) {
        if (token == null) {
            this.externalProcessNoHandleCount++;
            return;
        }
        if (this.externalProcessTokenToHandle == null) {
            this.externalProcessTokenToHandle = new ArrayMap<>();
        }
        ExternalProcessHandle handle = this.externalProcessTokenToHandle.get(token);
        if (handle == null) {
            handle = new ExternalProcessHandle(token, callingUid, callingTag);
            this.externalProcessTokenToHandle.put(token, handle);
            handle.startAssociationIfNeeded(this);
        }
        handle.mAcquisitionCount++;
    }

    public boolean removeExternalProcessHandleLocked(IBinder token) {
        ExternalProcessHandle handle;
        if (hasExternalProcessHandles()) {
            boolean hasHandle = false;
            ArrayMap<IBinder, ExternalProcessHandle> arrayMap = this.externalProcessTokenToHandle;
            if (arrayMap != null && (handle = arrayMap.get(token)) != null) {
                hasHandle = true;
                handle.mAcquisitionCount--;
                if (handle.mAcquisitionCount == 0) {
                    removeExternalProcessHandleInternalLocked(token);
                    return true;
                }
            }
            if (!hasHandle) {
                this.externalProcessNoHandleCount--;
                return true;
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeExternalProcessHandleInternalLocked(IBinder token) {
        ExternalProcessHandle handle = this.externalProcessTokenToHandle.get(token);
        handle.unlinkFromOwnDeathLocked();
        handle.stopAssociation();
        this.externalProcessTokenToHandle.remove(token);
        if (this.externalProcessTokenToHandle.size() == 0) {
            this.externalProcessTokenToHandle = null;
        }
    }

    public boolean hasExternalProcessHandles() {
        return this.externalProcessTokenToHandle != null || this.externalProcessNoHandleCount > 0;
    }

    public boolean hasConnectionOrHandle() {
        return !this.connections.isEmpty() || hasExternalProcessHandles();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onProviderPublishStatusLocked(boolean status) {
        int numOfConns = this.connections.size();
        for (int i = 0; i < numOfConns; i++) {
            ContentProviderConnection conn = this.connections.get(i);
            if (conn.waiting && conn.client != null) {
                ProcessRecord client = conn.client;
                if (!status) {
                    if (this.launchingApp == null) {
                        Slog.w("ActivityManager", "Unable to launch app " + this.appInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + this.appInfo.uid + " for provider " + this.info.authority + ": launching app became null");
                        EventLogTags.writeAmProviderLostProcess(UserHandle.getUserId(this.appInfo.uid), this.appInfo.packageName, this.appInfo.uid, this.info.authority);
                    } else {
                        Slog.wtf("ActivityManager", "Timeout waiting for provider " + this.appInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + this.appInfo.uid + " for provider " + this.info.authority + " caller=" + client);
                    }
                }
                IApplicationThread thread = client.getThread();
                if (thread != null) {
                    try {
                        thread.notifyContentProviderPublishStatus(newHolder(status ? conn : null, false), this.info.authority, conn.mExpectedUserId, status);
                    } catch (RemoteException e) {
                    }
                }
            }
            conn.waiting = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix, boolean full) {
        if (full) {
            pw.print(prefix);
            pw.print("package=");
            pw.print(this.info.applicationInfo.packageName);
            pw.print(" process=");
            pw.println(this.info.processName);
        }
        pw.print(prefix);
        pw.print("proc=");
        pw.println(this.proc);
        if (this.launchingApp != null) {
            pw.print(prefix);
            pw.print("launchingApp=");
            pw.println(this.launchingApp);
        }
        if (full) {
            pw.print(prefix);
            pw.print("uid=");
            pw.print(this.uid);
            pw.print(" provider=");
            pw.println(this.provider);
        }
        if (this.singleton) {
            pw.print(prefix);
            pw.print("singleton=");
            pw.println(this.singleton);
        }
        pw.print(prefix);
        pw.print("authority=");
        pw.println(this.info.authority);
        if (full && (this.info.isSyncable || this.info.multiprocess || this.info.initOrder != 0)) {
            pw.print(prefix);
            pw.print("isSyncable=");
            pw.print(this.info.isSyncable);
            pw.print(" multiprocess=");
            pw.print(this.info.multiprocess);
            pw.print(" initOrder=");
            pw.println(this.info.initOrder);
        }
        if (full) {
            if (hasExternalProcessHandles()) {
                pw.print(prefix);
                pw.print("externals:");
                if (this.externalProcessTokenToHandle != null) {
                    pw.print(" w/token=");
                    pw.print(this.externalProcessTokenToHandle.size());
                }
                if (this.externalProcessNoHandleCount > 0) {
                    pw.print(" notoken=");
                    pw.print(this.externalProcessNoHandleCount);
                }
                pw.println();
            }
        } else if (this.connections.size() > 0 || this.externalProcessNoHandleCount > 0) {
            pw.print(prefix);
            pw.print(this.connections.size());
            pw.print(" connections, ");
            pw.print(this.externalProcessNoHandleCount);
            pw.println(" external handles");
        }
        if (this.connections.size() > 0) {
            if (full) {
                pw.print(prefix);
                pw.println("Connections:");
            }
            for (int i = 0; i < this.connections.size(); i++) {
                ContentProviderConnection conn = this.connections.get(i);
                pw.print(prefix);
                pw.print("  -> ");
                pw.println(conn.toClientString());
                if (conn.provider != this) {
                    pw.print(prefix);
                    pw.print("    *** WRONG PROVIDER: ");
                    pw.println(conn.provider);
                }
            }
        }
    }

    public String toString() {
        String str = this.stringName;
        if (str != null) {
            return str;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("ContentProviderRecord{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" u");
        sb.append(UserHandle.getUserId(this.uid));
        sb.append(' ');
        sb.append(this.name.flattenToShortString());
        sb.append('}');
        String sb2 = sb.toString();
        this.stringName = sb2;
        return sb2;
    }

    public String toShortString() {
        String str = this.shortStringName;
        if (str != null) {
            return str;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append('/');
        sb.append(this.name.flattenToShortString());
        String sb2 = sb.toString();
        this.shortStringName = sb2;
        return sb2;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ExternalProcessHandle implements IBinder.DeathRecipient {
        private static final String LOG_TAG = "ExternalProcessHanldle";
        int mAcquisitionCount;
        AssociationState.SourceState mAssociation;
        final String mOwningProcessName;
        final int mOwningUid;
        private Object mProcStatsLock;
        final IBinder mToken;

        public ExternalProcessHandle(IBinder token, int owningUid, String owningProcessName) {
            this.mToken = token;
            this.mOwningUid = owningUid;
            this.mOwningProcessName = owningProcessName;
            try {
                token.linkToDeath(this, 0);
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Couldn't register for death for token: " + this.mToken, re);
            }
        }

        public void unlinkFromOwnDeathLocked() {
            this.mToken.unlinkToDeath(this, 0);
        }

        public void startAssociationIfNeeded(ContentProviderRecord provider) {
            if (this.mAssociation == null && provider.proc != null) {
                if (provider.appInfo.uid != this.mOwningUid || !provider.info.processName.equals(this.mOwningProcessName)) {
                    ProcessStats.ProcessStateHolder holder = provider.proc.getPkgList().get(provider.name.getPackageName());
                    if (holder == null) {
                        Slog.wtf("ActivityManager", "No package in referenced provider " + provider.name.toShortString() + ": proc=" + provider.proc);
                    } else if (holder.pkg == null) {
                        Slog.wtf("ActivityManager", "Inactive holder in referenced provider " + provider.name.toShortString() + ": proc=" + provider.proc);
                    } else {
                        Object obj = provider.proc.mService.mProcessStats.mLock;
                        this.mProcStatsLock = obj;
                        synchronized (obj) {
                            this.mAssociation = holder.pkg.getAssociationStateLocked(holder.state, provider.name.getClassName()).startSource(this.mOwningUid, this.mOwningProcessName, (String) null);
                        }
                    }
                }
            }
        }

        public void stopAssociation() {
            if (this.mAssociation != null) {
                synchronized (this.mProcStatsLock) {
                    this.mAssociation.stop();
                }
                this.mAssociation = null;
            }
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (ContentProviderRecord.this.service) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    if (ContentProviderRecord.this.hasExternalProcessHandles() && ContentProviderRecord.this.externalProcessTokenToHandle.get(this.mToken) != null) {
                        ContentProviderRecord.this.removeExternalProcessHandleInternalLocked(this.mToken);
                    }
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }
    }

    public ComponentName getComponentName() {
        return this.name;
    }
}
