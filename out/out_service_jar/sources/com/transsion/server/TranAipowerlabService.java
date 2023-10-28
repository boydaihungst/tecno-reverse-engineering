package com.transsion.server;

import android.content.Context;
import android.util.Slog;
import com.transsion.aipowerlab.ITranAipowerlabManager;
import com.transsion.hubcore.server.power.aipowerlab.ITranAipowerlabService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
public final class TranAipowerlabService extends ITranAipowerlabManager.Stub {
    private static final String TAG = "TranAipowerlabService";
    private Context mContext;

    public TranAipowerlabService(Context context) {
        Slog.i(TAG, "new TranAipowerlabService");
        this.mContext = context;
        init();
    }

    void init() {
        Slog.i(TAG, "init in TranAipowerlabService");
        ITranAipowerlabService.Instance().init(this.mContext);
    }

    public void enable(boolean enable) {
        ITranAipowerlabService.Instance().enable(enable);
    }

    public void systemReady() {
        Slog.i(TAG, "systemReady");
        ITranAipowerlabService.Instance().systemReady();
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        ITranAipowerlabService.Instance().dump(fd, pw, args);
    }
}
