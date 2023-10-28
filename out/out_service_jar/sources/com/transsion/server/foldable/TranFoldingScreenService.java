package com.transsion.server.foldable;

import android.content.Context;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.transsion.foldable.ITranFoldingScreen;
/* loaded from: classes2.dex */
public final class TranFoldingScreenService extends ITranFoldingScreen.Stub {
    private static final String TAG = "TranFoldingScreenService";
    private ActivityTaskManagerInternal mAtmInternal;
    private Context mContext;

    public TranFoldingScreenService(Context context) {
        this.mContext = context;
        init();
    }

    void init() {
        Slog.i(TAG, "init");
        TranFoldingScreenController.initCompatibleModeDefaultValueData();
        this.mAtmInternal = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
    }

    public int getCompatibleModeDefaultValue(String packageName) {
        return TranFoldingScreenController.getCompatibleModeDefaultValue(packageName);
    }

    public void removeTaskWhileModeChanged(String packageName, int userId, String reason) {
        this.mAtmInternal.removeRecentTasksByPackageName(packageName, userId, true, false, reason);
    }
}
