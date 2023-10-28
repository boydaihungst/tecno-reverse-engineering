package com.android.server.am;

import android.content.IntentFilter;
import android.util.PrintWriterPrinter;
import android.util.Printer;
import android.util.proto.ProtoOutputStream;
import java.io.PrintWriter;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class BroadcastFilter extends IntentFilter {
    final boolean exported;
    final String featureId;
    final boolean instantApp;
    final int owningUid;
    final int owningUserId;
    final String packageName;
    final String receiverId;
    final ReceiverList receiverList;
    final String requiredPermission;
    final boolean visibleToInstantApp;

    /* JADX INFO: Access modifiers changed from: package-private */
    public BroadcastFilter(IntentFilter _filter, ReceiverList _receiverList, String _packageName, String _featureId, String _receiverId, String _requiredPermission, int _owningUid, int _userId, boolean _instantApp, boolean _visibleToInstantApp, boolean _exported) {
        super(_filter);
        this.receiverList = _receiverList;
        this.packageName = _packageName;
        this.featureId = _featureId;
        this.receiverId = _receiverId;
        this.requiredPermission = _requiredPermission;
        this.owningUid = _owningUid;
        this.owningUserId = _userId;
        this.instantApp = _instantApp;
        this.visibleToInstantApp = _visibleToInstantApp;
        this.exported = _exported;
    }

    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        super.dumpDebug(proto, 1146756268033L);
        String str = this.requiredPermission;
        if (str != null) {
            proto.write(1138166333442L, str);
        }
        proto.write(1138166333443L, Integer.toHexString(System.identityHashCode(this)));
        proto.write(1120986464260L, this.owningUserId);
        proto.end(token);
    }

    public void dump(PrintWriter pw, String prefix) {
        dumpInReceiverList(pw, new PrintWriterPrinter(pw), prefix);
        this.receiverList.dumpLocal(pw, prefix);
    }

    public void dumpBrief(PrintWriter pw, String prefix) {
        dumpBroadcastFilterState(pw, prefix);
    }

    public void dumpInReceiverList(PrintWriter pw, Printer pr, String prefix) {
        super.dump(pr, prefix);
        dumpBroadcastFilterState(pw, prefix);
    }

    void dumpBroadcastFilterState(PrintWriter pw, String prefix) {
        if (this.requiredPermission != null) {
            pw.print(prefix);
            pw.print("requiredPermission=");
            pw.println(this.requiredPermission);
        }
    }

    public String toString() {
        return "BroadcastFilter{" + Integer.toHexString(System.identityHashCode(this)) + ' ' + this.owningUid + "/u" + this.owningUserId + ' ' + this.receiverList + '}';
    }
}
