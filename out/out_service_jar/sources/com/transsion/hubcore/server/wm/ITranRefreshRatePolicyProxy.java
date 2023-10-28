package com.transsion.hubcore.server.wm;

import android.hardware.display.DisplayManagerInternal;
import com.android.server.wm.WindowState;
import com.transsion.hubcore.server.wm.tranrefreshrate.TranRefreshRatePolicy;
import com.transsion.hubcore.server.wm.tranrefreshrate.TranRefreshRatePolicy120;
import java.util.Set;
/* loaded from: classes2.dex */
public interface ITranRefreshRatePolicyProxy {
    Set<String> getNonHighRefreshPackages();

    int getPreferredModeId(WindowState windowState);

    float getPreferredRefreshRate(WindowState windowState);

    DisplayManagerInternal.RefreshRateRange getRefreshRateRange(String str);

    TranRefreshRatePolicy getTranRefreshRatePolicy();

    TranRefreshRatePolicy120 getTranRefreshRatePolicy120();

    boolean inNonHighRefreshPackages(String str);

    boolean isAnimating(WindowState windowState);
}
