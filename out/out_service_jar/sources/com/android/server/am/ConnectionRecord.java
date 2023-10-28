package com.android.server.am;

import android.app.IServiceConnection;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.os.SystemClock;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.util.proto.ProtoUtils;
import com.android.internal.app.procstats.AssociationState;
import com.android.internal.app.procstats.ProcessStats;
import com.android.server.wm.ActivityServiceConnectionsHolder;
import defpackage.CompanionAppsPermissions;
import java.io.PrintWriter;
/* loaded from: classes.dex */
public final class ConnectionRecord {
    private static final int[] BIND_ORIG_ENUMS = {1, 2, 4, 8388608, 8, 16, 32, 64, 128, 33554432, 67108864, 134217728, 268435456, 536870912, 1073741824, 256, 4096};
    private static final int[] BIND_PROTO_ENUMS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 16, 17};
    final ActivityServiceConnectionsHolder<ConnectionRecord> activity;
    final ComponentName aliasComponent;
    public AssociationState.SourceState association;
    final AppBindRecord binding;
    final PendingIntent clientIntent;
    final int clientLabel;
    final String clientPackageName;
    final String clientProcessName;
    final int clientUid;
    final IServiceConnection conn;
    final int flags;
    private Object mProcStatsLock;
    boolean serviceDead;
    String stringName;

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        pw.println(prefix + "binding=" + this.binding);
        ActivityServiceConnectionsHolder<ConnectionRecord> activityServiceConnectionsHolder = this.activity;
        if (activityServiceConnectionsHolder != null) {
            activityServiceConnectionsHolder.dump(pw, prefix);
        }
        pw.println(prefix + "conn=" + this.conn.asBinder() + " flags=0x" + Integer.toHexString(this.flags));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ConnectionRecord(AppBindRecord _binding, ActivityServiceConnectionsHolder<ConnectionRecord> _activity, IServiceConnection _conn, int _flags, int _clientLabel, PendingIntent _clientIntent, int _clientUid, String _clientProcessName, String _clientPackageName, ComponentName _aliasComponent) {
        this.binding = _binding;
        this.activity = _activity;
        this.conn = _conn;
        this.flags = _flags;
        this.clientLabel = _clientLabel;
        this.clientIntent = _clientIntent;
        this.clientUid = _clientUid;
        this.clientProcessName = _clientProcessName;
        this.clientPackageName = _clientPackageName;
        this.aliasComponent = _aliasComponent;
    }

    public boolean hasFlag(int flag) {
        return (this.flags & flag) != 0;
    }

    public boolean notHasFlag(int flag) {
        return (this.flags & flag) == 0;
    }

    public void startAssociationIfNeeded() {
        if (this.association == null && this.binding.service.app != null) {
            if (this.binding.service.appInfo.uid != this.clientUid || !this.binding.service.processName.equals(this.clientProcessName)) {
                ProcessStats.ProcessStateHolder holder = this.binding.service.app.getPkgList().get(this.binding.service.instanceName.getPackageName());
                if (holder == null) {
                    Slog.wtf("ActivityManager", "No package in referenced service " + this.binding.service.shortInstanceName + ": proc=" + this.binding.service.app);
                } else if (holder.pkg == null) {
                    Slog.wtf("ActivityManager", "Inactive holder in referenced service " + this.binding.service.shortInstanceName + ": proc=" + this.binding.service.app);
                } else {
                    Object obj = this.binding.service.app.mService.mProcessStats.mLock;
                    this.mProcStatsLock = obj;
                    synchronized (obj) {
                        this.association = holder.pkg.getAssociationStateLocked(holder.state, this.binding.service.instanceName.getClassName()).startSource(this.clientUid, this.clientProcessName, this.clientPackageName);
                    }
                }
            }
        }
    }

    public void trackProcState(int procState, int seq) {
        if (this.association != null) {
            synchronized (this.mProcStatsLock) {
                this.association.trackProcState(procState, seq, SystemClock.uptimeMillis());
            }
        }
    }

    public void stopAssociation() {
        if (this.association != null) {
            synchronized (this.mProcStatsLock) {
                this.association.stop();
            }
            this.association = null;
        }
    }

    public String toString() {
        String str = this.stringName;
        if (str != null) {
            return str;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("ConnectionRecord{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" u");
        sb.append(this.binding.client.userId);
        sb.append(' ');
        if ((this.flags & 1) != 0) {
            sb.append("CR ");
        }
        if ((this.flags & 2) != 0) {
            sb.append("DBG ");
        }
        if ((this.flags & 4) != 0) {
            sb.append("!FG ");
        }
        if ((this.flags & 8388608) != 0) {
            sb.append("IMPB ");
        }
        if ((this.flags & 8) != 0) {
            sb.append("ABCLT ");
        }
        if ((this.flags & 16) != 0) {
            sb.append("OOM ");
        }
        if ((32 & this.flags) != 0) {
            sb.append("WPRI ");
        }
        if ((this.flags & 64) != 0) {
            sb.append("IMP ");
        }
        if ((128 & this.flags) != 0) {
            sb.append("WACT ");
        }
        if ((this.flags & 33554432) != 0) {
            sb.append("FGSA ");
        }
        if ((this.flags & 67108864) != 0) {
            sb.append("FGS ");
        }
        if ((this.flags & 134217728) != 0) {
            sb.append("LACT ");
        }
        if ((this.flags & 524288) != 0) {
            sb.append("SLTA ");
        }
        if ((this.flags & 268435456) != 0) {
            sb.append("VFGS ");
        }
        if ((this.flags & 536870912) != 0) {
            sb.append("UI ");
        }
        if ((this.flags & 1073741824) != 0) {
            sb.append("!VIS ");
        }
        if ((this.flags & 256) != 0) {
            sb.append("!PRCP ");
        }
        if ((this.flags & 4096) != 0) {
            sb.append("CAPS ");
        }
        if (this.serviceDead) {
            sb.append("DEAD ");
        }
        sb.append(this.binding.service.shortInstanceName);
        sb.append(":@");
        sb.append(Integer.toHexString(System.identityHashCode(this.conn.asBinder())));
        sb.append('}');
        String sb2 = sb.toString();
        this.stringName = sb2;
        return sb2;
    }

    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        if (this.binding == null) {
            return;
        }
        long token = proto.start(fieldId);
        proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, Integer.toHexString(System.identityHashCode(this)));
        if (this.binding.client != null) {
            proto.write(1120986464258L, this.binding.client.userId);
        }
        ProtoUtils.writeBitWiseFlagsToProtoEnum(proto, 2259152797699L, this.flags, BIND_ORIG_ENUMS, BIND_PROTO_ENUMS);
        if (this.serviceDead) {
            proto.write(2259152797699L, 15);
        }
        if (this.binding.service != null) {
            proto.write(1138166333444L, this.binding.service.shortInstanceName);
        }
        proto.end(token);
    }

    public boolean match(ConnectionRecord c) {
        if (c == null) {
            return false;
        }
        int connHash = System.identityHashCode(this.conn.asBinder());
        int cconnHash = System.identityHashCode(c.conn.asBinder());
        if (this.binding == c.binding && this.activity == c.activity && connHash == cconnHash && this.flags == c.flags && this.clientLabel == c.clientLabel) {
            PendingIntent pendingIntent = this.clientIntent;
            return (pendingIntent == null && c.clientIntent == null) || (pendingIntent != null && pendingIntent.equals(c.clientIntent));
        }
        return false;
    }
}
