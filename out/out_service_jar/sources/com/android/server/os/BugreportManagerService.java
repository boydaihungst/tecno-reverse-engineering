package com.android.server.os;

import android.content.Context;
import com.android.server.SystemService;
/* loaded from: classes2.dex */
public class BugreportManagerService extends SystemService {
    private static final String TAG = "BugreportManagerService";
    private BugreportManagerServiceImpl mService;

    public BugreportManagerService(Context context) {
        super(context);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        BugreportManagerServiceImpl bugreportManagerServiceImpl = new BugreportManagerServiceImpl(getContext());
        this.mService = bugreportManagerServiceImpl;
        publishBinderService("bugreport", bugreportManagerServiceImpl);
    }
}
