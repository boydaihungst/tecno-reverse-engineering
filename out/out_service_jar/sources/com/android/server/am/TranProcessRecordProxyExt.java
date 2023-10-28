package com.android.server.am;

import java.lang.ref.WeakReference;
/* loaded from: classes.dex */
public class TranProcessRecordProxyExt {
    private final WeakReference<Object> mAncestor;

    public TranProcessRecordProxyExt(Object ancestor) {
        this.mAncestor = new WeakReference<>(ancestor);
    }

    private boolean assertAncestor() {
        Object ancestor = this.mAncestor.get();
        if (ancestor != null && (ancestor instanceof ProcessRecord)) {
            return true;
        }
        return false;
    }

    public boolean onIsPersistent() {
        if (assertAncestor()) {
            ProcessRecord processRecord = (ProcessRecord) this.mAncestor.get();
            return processRecord.isPersistent();
        }
        return false;
    }
}
