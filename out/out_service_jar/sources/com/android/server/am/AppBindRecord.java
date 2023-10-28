package com.android.server.am;

import android.util.ArraySet;
import android.util.proto.ProtoOutputStream;
import defpackage.CompanionAppsPermissions;
import java.io.PrintWriter;
/* loaded from: classes.dex */
public final class AppBindRecord {
    final ProcessRecord client;
    final ArraySet<ConnectionRecord> connections = new ArraySet<>();
    final IntentBindRecord intent;
    final ServiceRecord service;

    void dump(PrintWriter pw, String prefix) {
        pw.println(prefix + "service=" + this.service);
        pw.println(prefix + "client=" + this.client);
        dumpInIntentBind(pw, prefix);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpInIntentBind(PrintWriter pw, String prefix) {
        int N = this.connections.size();
        if (N > 0) {
            pw.println(prefix + "Per-process Connections:");
            for (int i = 0; i < N; i++) {
                ConnectionRecord c = this.connections.valueAt(i);
                pw.println(prefix + "  " + c);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppBindRecord(ServiceRecord _service, IntentBindRecord _intent, ProcessRecord _client) {
        this.service = _service;
        this.intent = _intent;
        this.client = _client;
    }

    public String toString() {
        return "AppBindRecord{" + Integer.toHexString(System.identityHashCode(this)) + " " + this.service.shortInstanceName + ":" + this.client.processName + "}";
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, this.service.shortInstanceName);
        proto.write(1138166333442L, this.client.processName);
        int N = this.connections.size();
        for (int i = 0; i < N; i++) {
            ConnectionRecord conn = this.connections.valueAt(i);
            proto.write(CompanionAppsPermissions.AppPermissions.PERMISSION, Integer.toHexString(System.identityHashCode(conn)));
        }
        proto.end(token);
    }
}
