package com.android.server.am;

import java.lang.ref.WeakReference;
/* loaded from: classes.dex */
public class TranProcessStateRecordProxyExt {
    private final WeakReference<Object> mAncestor;

    public TranProcessStateRecordProxyExt(Object ancestor) {
        this.mAncestor = new WeakReference<>(ancestor);
    }

    private boolean assertAncestor() {
        Object ancestor = this.mAncestor.get();
        if (ancestor != null && (ancestor instanceof ProcessStateRecord)) {
            return true;
        }
        return false;
    }

    public int onGetSetSchedGroup() {
        if (assertAncestor()) {
            ProcessStateRecord processStateRecord = (ProcessStateRecord) this.mAncestor.get();
            return processStateRecord.getSetSchedGroup();
        }
        return 2;
    }
}
