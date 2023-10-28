package com.transsion.aipowerlab;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Singleton;
import android.util.Slog;
import com.transsion.aipowerlab.ITranAipowerlabManager;
/* loaded from: classes4.dex */
public class TranAipowerlabManager {
    private static final Singleton<ITranAipowerlabManager> ITranAipowerlabManagerSingleton = new Singleton<ITranAipowerlabManager>() { // from class: com.transsion.aipowerlab.TranAipowerlabManager.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.util.Singleton
        public ITranAipowerlabManager create() {
            IBinder b = ServiceManager.getService(Context.TRAN_AIPOWERLAB_SERVICE);
            ITranAipowerlabManager sm = ITranAipowerlabManager.Stub.asInterface(b);
            return sm;
        }
    };
    private static final String TAG = "TranAipowerlabManager";
    private static ITranAipowerlabManager mService;

    public TranAipowerlabManager(Context context, ITranAipowerlabManager service) {
        mService = service;
    }

    public void enable(boolean enable) {
        ITranAipowerlabManager sm = getService();
        if (sm == null) {
            return;
        }
        try {
            sm.enable(enable);
        } catch (RemoteException e) {
            Slog.d(TAG, e.toString());
        }
    }

    public static ITranAipowerlabManager getService() {
        return ITranAipowerlabManagerSingleton.get();
    }

    private void logd(String msg) {
        Slog.d(TAG, msg);
    }
}
