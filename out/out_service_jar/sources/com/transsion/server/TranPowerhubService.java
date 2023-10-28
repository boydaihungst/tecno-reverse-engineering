package com.transsion.server;

import android.content.Context;
import android.os.Bundle;
import com.transsion.hubcore.server.power.powerhub.ITranPowerhubService;
import com.transsion.powerhub.ITranPowerhubManager;
/* loaded from: classes2.dex */
public final class TranPowerhubService extends ITranPowerhubManager.Stub {
    private Context mContext;

    public TranPowerhubService(Context context) {
        this.mContext = context;
        init();
    }

    void init() {
        ITranPowerhubService.Instance().init(this.mContext);
    }

    public void systemReady() {
        ITranPowerhubService.Instance().systemReady();
    }

    public Bundle getSkipInfo(Bundle viewInfo) {
        return ITranPowerhubService.Instance().getSkipInfo(viewInfo);
    }

    public void updateCollectData(Bundle dataInfo) {
        ITranPowerhubService.Instance().updateCollectData(dataInfo);
    }
}
