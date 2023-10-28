package com.android.server.os;

import android.content.Context;
import com.android.server.LocalServices;
import com.android.server.SystemService;
/* loaded from: classes2.dex */
public class NativeTombstoneManagerService extends SystemService {
    private static final String TAG = "NativeTombstoneManagerService";
    private NativeTombstoneManager mManager;

    public NativeTombstoneManagerService(Context context) {
        super(context);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        NativeTombstoneManager nativeTombstoneManager = new NativeTombstoneManager(getContext());
        this.mManager = nativeTombstoneManager;
        LocalServices.addService(NativeTombstoneManager.class, nativeTombstoneManager);
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 550) {
            this.mManager.onSystemReady();
        }
    }
}
