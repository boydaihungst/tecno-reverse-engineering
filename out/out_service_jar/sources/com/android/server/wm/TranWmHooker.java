package com.android.server.wm;

import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.am.TranAmHooker;
import com.transsion.hubcore.agares.ITranAgaresFeature;
import com.transsion.hubcore.griffin.ITranGriffinFeature;
import com.transsion.hubcore.griffin.lib.app.TranProcessWrapper;
import java.util.Iterator;
/* loaded from: classes2.dex */
public class TranWmHooker {
    private static final String TAG = "Griffin/KeepAlive";

    public static boolean ensureIfNeedKill(String pkg, ArrayMap<String, SparseArray<WindowProcessController>> pmap, boolean killPro) {
        boolean kill = killPro;
        for (int i = 0; i < pmap.size(); i++) {
            SparseArray<WindowProcessController> uids = pmap.valueAt(i);
            int j = 0;
            while (true) {
                if (j >= uids.size()) {
                    break;
                }
                WindowProcessController proc = uids.valueAt(j);
                if (proc != null && proc.hasThread() && proc.getProcessWrapper() != null && proc.mPkgList.contains(pkg) && ITranGriffinFeature.Instance().getPM().isIgnoreKillApp(proc.mName)) {
                    kill = false;
                    long lastPss = proc.getProcessWrapper().lastPss();
                    if (ITranGriffinFeature.Instance().getPM().isOutMaxMem(proc.mName, lastPss)) {
                        if (ITranGriffinFeature.Instance().isGriffinDebugOpen()) {
                            Slog.d(TAG, "OMM: proc " + proc.mName + " pss:" + lastPss + ", kill:" + killPro);
                        }
                        kill = killPro;
                    }
                }
                j++;
            }
        }
        return kill;
    }

    public static boolean ensureIfNeedClear(String pkg, boolean killPro) {
        if (killPro) {
            return true;
        }
        return true ^ ITranGriffinFeature.Instance().getPM().isIgnoreClearApp(pkg);
    }

    public static boolean isAgaresProtectProc(WindowProcessController proc) {
        ITranAgaresFeature agares = ITranAgaresFeature.Instance();
        Iterator<String> it = proc.mPkgList.iterator();
        while (it.hasNext()) {
            String pkgName = it.next();
            if (proc.hasActivities() || proc.mName.equals(pkgName)) {
                if (agares.isAgaresProtectApp(pkgName) && !TranAmHooker.avoidProtect()) {
                    TranProcessWrapper processWrapper = proc.getProcessWrapper();
                    if (processWrapper != null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isGameBoosterProtectProc(WindowProcessController proc) {
        if (proc.getProcessWrapper() != null && ITranGriffinFeature.Instance().getPM().ensureExemptionProc(proc.mName, proc.getProcessWrapper().lastPss())) {
            return true;
        }
        return false;
    }
}
