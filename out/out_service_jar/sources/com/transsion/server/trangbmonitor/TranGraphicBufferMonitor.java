package com.transsion.server.trangbmonitor;

import android.content.Context;
/* loaded from: classes2.dex */
public class TranGraphicBufferMonitor {
    private static TranGraphicBufferMonitor sInstance;

    private TranGraphicBufferMonitor(Context context, Object amsLocked, Object pidsLocked) {
    }

    public TranGraphicBufferMonitor() {
    }

    public static TranGraphicBufferMonitor getInstance(Context context, Object amsLocked, Object pidsLocked) {
        if (sInstance == null) {
            sInstance = new TranGraphicBufferMonitor();
        }
        return sInstance;
    }

    public int getSFPid() {
        return 0;
    }

    public boolean reclaimGB(boolean check) {
        return false;
    }
}
