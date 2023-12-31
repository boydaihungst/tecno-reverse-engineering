package com.android.server.integrity;

import android.content.Context;
import com.android.server.SystemService;
/* loaded from: classes.dex */
public class AppIntegrityManagerService extends SystemService {
    private Context mContext;
    private AppIntegrityManagerServiceImpl mService;

    public AppIntegrityManagerService(Context context) {
        super(context);
        this.mContext = context;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: com.android.server.integrity.AppIntegrityManagerService */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v1, types: [com.android.server.integrity.AppIntegrityManagerServiceImpl, android.os.IBinder] */
    @Override // com.android.server.SystemService
    public void onStart() {
        ?? create = AppIntegrityManagerServiceImpl.create(this.mContext);
        this.mService = create;
        publishBinderService("app_integrity", create);
    }
}
