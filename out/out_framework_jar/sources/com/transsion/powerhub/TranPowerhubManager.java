package com.transsion.powerhub;

import android.content.Context;
import android.os.IBinder;
import android.os.ServiceManager;
import android.util.Singleton;
import android.util.Slog;
import android.view.Display;
import android.view.View;
import com.transsion.powerhub.ITranPowerhubManager;
/* loaded from: classes4.dex */
public class TranPowerhubManager {
    private static final Singleton<ITranPowerhubManager> ITranPowerhubManagerSingleton = new Singleton<ITranPowerhubManager>() { // from class: com.transsion.powerhub.TranPowerhubManager.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.util.Singleton
        public ITranPowerhubManager create() {
            IBinder b = ServiceManager.getService(Context.TRAN_POWERHUB_SERVICE);
            ITranPowerhubManager sm = ITranPowerhubManager.Stub.asInterface(b);
            return sm;
        }
    };
    private static final String TAG = "TranPowerhubManager";
    private static ITranPowerhubManager mService;
    private TranLayerControlManager mTranLayerControlManager;

    public TranPowerhubManager(Context context, ITranPowerhubManager service) {
        this.mTranLayerControlManager = null;
        mService = service;
        this.mTranLayerControlManager = new TranLayerControlManager(this);
    }

    public void setDispSize(Display mDisplay) {
        TranLayerControlManager tranLayerControlManager = this.mTranLayerControlManager;
        if (tranLayerControlManager == null) {
            return;
        }
        tranLayerControlManager.setDispSize(mDisplay);
    }

    public boolean initSkipInfo(String strPkgName) {
        ITranPowerhubManager sm;
        if (this.mTranLayerControlManager == null || (sm = getService()) == null) {
            return false;
        }
        return this.mTranLayerControlManager.initSkipInfo(sm, strPkgName);
    }

    public int shouldSkip(String strPkgName, String strActivityName, View descendant) {
        TranLayerControlManager tranLayerControlManager;
        ITranPowerhubManager sm = getService();
        if (sm == null || (tranLayerControlManager = this.mTranLayerControlManager) == null) {
            return 0;
        }
        return tranLayerControlManager.shouldSkip(sm, strPkgName, strActivityName, descendant);
    }

    public void updateCollectData(boolean bStopped) {
        TranLayerControlManager tranLayerControlManager = this.mTranLayerControlManager;
        if (tranLayerControlManager == null) {
            return;
        }
        tranLayerControlManager.updateCollectData(bStopped);
    }

    public static ITranPowerhubManager getService() {
        return ITranPowerhubManagerSingleton.get();
    }

    private void logd(String msg) {
        Slog.d(TAG, msg);
    }
}
