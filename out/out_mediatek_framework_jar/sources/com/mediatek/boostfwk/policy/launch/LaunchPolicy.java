package com.mediatek.boostfwk.policy.launch;

import android.os.Trace;
import com.mediatek.powerhalmgr.PowerHalMgr;
import com.mediatek.powerhalmgr.PowerHalMgrFactory;
/* loaded from: classes.dex */
public class LaunchPolicy {
    private static final String TAG = "SBE-LaunchPolicy";
    private PowerHalMgr mPowerHalService = PowerHalMgrFactory.getInstance().makePowerHalMgr();
    private static int mPowerHandle = 0;
    private static int mReleaseLaunchDuration = 3000;
    private static int PERF_RES_POWER_END_HINT_HOLD_TIME = 54592256;
    private static int MTKPOWER_HINT_PROCESS_CREATE = 21;

    public void boostEnd(String pkgName) {
        Trace.traceBegin(64L, "SBE boost end");
        int[] perf_lock_rsc = {PERF_RES_POWER_END_HINT_HOLD_TIME, MTKPOWER_HINT_PROCESS_CREATE};
        perfLockAcquire(perf_lock_rsc);
        Trace.traceEnd(64L);
    }

    private void perfLockAcquire(int[] resList) {
        PowerHalMgr powerHalMgr = this.mPowerHalService;
        if (powerHalMgr != null) {
            int perfLockAcquire = powerHalMgr.perfLockAcquire(mPowerHandle, mReleaseLaunchDuration, resList);
            mPowerHandle = perfLockAcquire;
            this.mPowerHalService.perfLockRelease(perfLockAcquire);
        }
    }
}
