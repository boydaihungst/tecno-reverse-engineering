package com.transsion.server.trancloudconfig;

import android.content.Context;
/* loaded from: classes2.dex */
public class TranCloudConfigManagerExt {
    private static TranCloudConfigManagerExt sTranCloudConfigManagerExt;

    public static TranCloudConfigManagerExt getInstance() {
        if (sTranCloudConfigManagerExt == null) {
            sTranCloudConfigManagerExt = new TranCloudConfigManagerExt();
        }
        return sTranCloudConfigManagerExt;
    }

    public void init(Context context) {
    }

    public void onSystemReady() {
    }

    public void destroy() {
    }
}
