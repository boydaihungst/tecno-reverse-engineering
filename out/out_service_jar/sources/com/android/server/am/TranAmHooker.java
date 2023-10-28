package com.android.server.am;

import android.content.IIntentReceiver;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.wm.TranWmHooker;
import com.android.server.wm.WindowProcessController;
import com.transsion.hubcore.agares.ITranAgaresFeature;
import com.transsion.hubcore.griffin.ITranGriffinFeature;
import com.transsion.hubcore.server.am.ITranAmHooker;
import com.transsion.hubsdk.resmonitor.TranResMonitorManager;
import com.transsion.hubsdk.trancare.cloudengine.ITranCloudEngineCallback;
import com.transsion.hubsdk.trancare.trancare.TranTrancareManager;
import com.transsion.util.SystemConfigUtil;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class TranAmHooker {
    private static final String CLOUD_TRAN_FREEZER_SWITCH_KEY = "1001000040";
    private static final long REG_FREEZER_CLOUD_ENGINE_DELAY_TIME = 1000;
    public static final String RES_AVAIL_MEM_KEY = "avail";
    public static final String RES_MEMORY_KEY = "memory";
    private static final String TAG = "Griffin/KeepAlive";
    public static final String TAG_AGARES = "Agares/KeepAlive";
    private static final String TAG_FREEZER = "Freezer/CloudSwitch";
    public static final String TAG_GAMEBOOSTER = "GameBooster/KeepAlive";
    private final CachedAppOptimizer mCachedAppOptimizer;
    private boolean mCloudFreezerSwitch = true;
    final Runnable mHandleRegCloudRunnable;

    public static boolean isKeepAliveSupport() {
        if (ITranGriffinFeature.Instance().isGriffinDebugOpen()) {
            Slog.d(TAG, "The project total ram is " + SystemConfigUtil.getRamLevel() + "G");
        }
        return ITranGriffinFeature.Instance().getFlagGriffinKeepAliveSupport() && ITranGriffinFeature.Instance().isKeepAliveEnable() && SystemConfigUtil.getRamLevel() >= 6;
    }

    public static void improveAdjIfNeed(ProcessRecord r) {
        if (ITranGriffinFeature.Instance().getPM().isPromoteToBS(r.processName)) {
            if (ITranGriffinFeature.Instance().isGriffinDebugOpen()) {
                Slog.d(TAG, "setMaxOomAdj proc:" + r.processName + " from " + r.mState.getMaxAdj() + " to 800");
            }
            r.mState.setMaxAdj(800);
        }
    }

    public static void recoveryAdjIfNeed(ProcessProfileRecord profile) {
        ProcessRecord r = profile.mApp;
        if (ITranGriffinFeature.Instance().getPM().isPromoteToBS(r.processName) && r.mState.getMaxAdj() == 800 && ITranGriffinFeature.Instance().getPM().isOutMaxMem(r.processName, profile.getLastPss())) {
            int fixedMaxAdj = ITranGriffinFeature.Instance().getPM().setMaxOomAdj(r.processWrapper, 1001);
            r.mState.setMaxAdj(fixedMaxAdj);
            Slog.d(TAG, "recordPssSampleLocked set MaxAdj : UNKNOWN_ADJ / procName: " + r.processName);
        }
    }

    public static boolean ifNeedImproveAdj(ProcessRecord r) {
        return r.mState.getMaxAdj() > 800 && isKeepAliveSupport();
    }

    public static boolean checkApmPermissionLocked(ProcessRecord callerApp) {
        if (callerApp == null) {
            return false;
        }
        boolean isCallerPlatform = callerApp.uid <= 1000 || callerApp.uid == 2000 || (callerApp.info != null && (callerApp.info.isSystemApp() || callerApp.info.isUpdatedSystemApp() || callerApp.info.isSignedWithPlatformKey() || (callerApp.info.flags & 8) != 0));
        if (isCallerPlatform) {
            return true;
        }
        Slog.d(TAG, "Non-system applications are not allowed to call this method, callerApp: " + callerApp);
        return false;
    }

    public static boolean isAgaresSupport() {
        return ITranAgaresFeature.Instance().isEnable("") && ITranAgaresFeature.Instance().isCustomerEnable();
    }

    public static boolean ifImproveAdjForAgares(ProcessRecord r) {
        WindowProcessController proc = r.getWindowProcessController();
        if (proc != null) {
            return ITranAgaresFeature.Instance().isEnable("") && r.mState.getMaxAdj() > 800 && TranWmHooker.isAgaresProtectProc(proc);
        }
        if (ITranAgaresFeature.Instance().isDebug()) {
            Slog.d(TAG_AGARES, "proc == null,ifImproveAdjForAgares() return false");
        }
        return false;
    }

    public static void improveAdjForAgares(ProcessRecord r) {
        if (ifImproveAdjForAgares(r)) {
            r.mState.setMaxAdj(800);
            r.mState.setAgaresImproMaxAdj(true);
            Slog.d(TAG_AGARES, " improve this proc max Adj = 800");
        }
    }

    public static boolean ifRecoveryAdjForAgares(ProcessProfileRecord profile) {
        ProcessRecord r = profile.mApp;
        if (r == null) {
            if (ITranAgaresFeature.Instance().isDebug()) {
                Slog.d(TAG_AGARES, "profile.mApp == null,ifRecoveryAdjForAgares() return false");
            }
            return false;
        }
        WindowProcessController proc = r.getWindowProcessController();
        if (proc == null) {
            if (ITranAgaresFeature.Instance().isDebug()) {
                Slog.d(TAG_AGARES, "r.getWindowProcessController() == null,ifRecoveryAdjForAgares() return false");
            }
            return false;
        } else if (isAgaresSupport()) {
            return (!TranWmHooker.isAgaresProtectProc(proc) && r.mState.isAgaresImprovedMaxAdj()) || (r.mState.getMaxAdj() == 800 && avoidProtect() && TranWmHooker.isAgaresProtectProc(proc));
        } else {
            return false;
        }
    }

    public static void recoveryAdjforAgares(ProcessProfileRecord profile) {
        if (ifRecoveryAdjForAgares(profile)) {
            ProcessRecord r = profile.mApp;
            if (r == null) {
                if (ITranAgaresFeature.Instance().isDebug()) {
                    Slog.d(TAG_AGARES, "profile.mApp == null,recovery MaxAdj failed!");
                    return;
                }
                return;
            }
            r.mState.setMaxAdj(1001);
            r.mState.setAgaresImproMaxAdj(false);
            Slog.d(TAG_AGARES, "recovery MaxAdj : UNKNOWN_ADJ / procName: " + r.processName);
        }
    }

    public static boolean avoidProtect() {
        return getAvailableMem() < ITranAgaresFeature.Instance().getAvailMemThreshold();
    }

    private static long getAvailableMem() {
        String ret = getMemInfo();
        if (TextUtils.isEmpty(ret)) {
            return -1L;
        }
        try {
            JSONObject rootJson = new JSONObject(ret);
            if (rootJson.has(RES_MEMORY_KEY)) {
                JSONObject memoryJson = rootJson.getJSONObject(RES_MEMORY_KEY);
                if (memoryJson.has(RES_AVAIL_MEM_KEY)) {
                    long availMem = memoryJson.optLong(RES_AVAIL_MEM_KEY);
                    return availMem;
                }
                return -1L;
            }
            return -1L;
        } catch (JSONException e) {
            e.printStackTrace();
            return -1L;
        }
    }

    private static String getMemInfo() {
        String ret = "";
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("cache", true);
            ret = TranResMonitorManager.getEventStatic(2, jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (ITranAgaresFeature.Instance().isDebug()) {
            Slog.d(TAG_AGARES, "get result = " + ret);
        }
        return ret;
    }

    public static int getProcAdj(ProcessRecord proc) {
        return proc.mState.getSetAdj();
    }

    public static String getProcName(ProcessRecord proc) {
        return proc.packageName;
    }

    public static boolean isGameBooseterSupport() {
        return ITranAmHooker.Instance().isGameBooseterSupport();
    }

    public static boolean isGameBoosterEnable() {
        return ITranAmHooker.Instance().isEnable() && isGameBooseterSupport();
    }

    public static boolean isTargetGameApp(String packageName) {
        return ITranAmHooker.Instance().isTargetGameApp(packageName);
    }

    public static void recoveryAdjforGame(ProcessProfileRecord profile) {
        ProcessRecord r;
        if (!isNeedRecoveryForGame(profile) || (r = profile.mApp) == null) {
            return;
        }
        r.mState.setMaxAdj(1001);
        r.mState.setAgaresImproMaxAdj(false);
        Slog.d(TAG_GAMEBOOSTER, "recovery MaxAdj : UNKNOWN_ADJ / procName: " + r.processName);
    }

    private static boolean isNeedRecoveryForGame(ProcessProfileRecord profile) {
        ProcessRecord r = profile.mApp;
        if (r == null) {
            if (ITranAmHooker.Instance().isDebug()) {
                Slog.d(TAG_GAMEBOOSTER, "profile.mApp == null,isNeedRecoveryForGame() return false");
            }
            return false;
        }
        WindowProcessController proc = r.getWindowProcessController();
        if (proc != null) {
            return isGameBoosterEnable() && r.mState.getMaxAdj() == 800 && avoidProtectForGame() && TranWmHooker.isGameBoosterProtectProc(proc);
        }
        if (ITranAmHooker.Instance().isDebug()) {
            Slog.d(TAG_GAMEBOOSTER, "r.getWindowProcessController() == null,isNeedRecoveryForGame() return false");
        }
        return false;
    }

    public static boolean avoidProtectForGame() {
        return getAvailableMem() < ITranAmHooker.Instance().getAvailMemThreshold();
    }

    public static void improveAdjForGame(ProcessRecord r) {
        if (shouldImproveForGame(r)) {
            r.mState.setMaxAdj(800);
            r.mState.setAgaresImproMaxAdj(true);
            Slog.d(TAG_GAMEBOOSTER, " improve this proc" + r.processName + "/ max Adj = 800");
        }
    }

    private static boolean shouldImproveForGame(ProcessRecord r) {
        if (isGameBoosterEnable()) {
            WindowProcessController proc = r.getWindowProcessController();
            if (proc != null) {
                return r.mState.getMaxAdj() > 800 && TranWmHooker.isGameBoosterProtectProc(proc);
            }
            if (ITranAmHooker.Instance().isDebug()) {
                Slog.d(TAG_AGARES, "proc == null,shouldImproveForGame() return false");
            }
            return false;
        }
        return false;
    }

    public static void reportCrashForGame(String packageName) {
        ITranAmHooker.Instance().onAppCrash(packageName);
    }

    public static void onActivityStart(String packageName, String activity, int launchType, boolean isAgares) {
        ITranAmHooker.Instance().onActivityStart(packageName, activity, launchType, isAgares);
    }

    public static boolean isPreloadedForGame(String packageName) {
        return ITranAmHooker.Instance().isPreloadedApp(packageName);
    }

    public static void onPredictWrong(String packageName) {
        ITranAmHooker.Instance().onPredictWrong(packageName);
    }

    public static void boostPreloadStart(int uid, String packageName) {
        ITranAmHooker.Instance().boosterCpu(uid, packageName);
    }

    public static void boostPreloadStop() {
        ITranAmHooker.Instance().boosterCpuStop();
    }

    public static boolean isGameBoosterDebug() {
        return ITranAmHooker.Instance().isDebug();
    }

    public static boolean isPreloaded(String packageName) {
        return ITranAmHooker.Instance().isPreloadedApp(packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-am-TranAmHooker  reason: not valid java name */
    public /* synthetic */ void m1504lambda$new$0$comandroidserveramTranAmHooker() {
        Slog.d(TAG_FREEZER, "mHandleRegCloudRunnable start");
        regCloudEngine();
    }

    public TranAmHooker(CachedAppOptimizer optimizer) {
        Runnable runnable = new Runnable() { // from class: com.android.server.am.TranAmHooker$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                TranAmHooker.this.m1504lambda$new$0$comandroidserveramTranAmHooker();
            }
        };
        this.mHandleRegCloudRunnable = runnable;
        this.mCachedAppOptimizer = optimizer;
        optimizer.regFreezerCloudEngine(runnable, 1000L);
        Slog.d(TAG_FREEZER, "regCloudEngine ready");
    }

    public void regCloudEngine() {
        mCallback callback = new mCallback();
        TranTrancareManager.regCloudEngineCallback(CLOUD_TRAN_FREEZER_SWITCH_KEY, "v1.1", callback);
        Slog.d(TAG_FREEZER, "regCloudEngine finish");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class mCallback extends ITranCloudEngineCallback.Stub {
        mCallback() {
        }

        public void onUpdate(String key, boolean fileState, String config) {
            Slog.d(TranAmHooker.TAG_FREEZER, "[onUpdate] key = " + key + " fileState = " + fileState + " config = " + config);
            if (TranAmHooker.CLOUD_TRAN_FREEZER_SWITCH_KEY.equals(key)) {
                TranTrancareManager.feedBack(key, true);
                TranAmHooker.this.updateFreezerCloudSwitch();
            }
        }
    }

    public void updateFreezerCloudSwitch() {
        boolean switchState = TranTrancareManager.getEnabled(CLOUD_TRAN_FREEZER_SWITCH_KEY, true);
        Slog.d(TAG_FREEZER, "getEnabled: " + switchState);
        this.mCloudFreezerSwitch = switchState;
        this.mCachedAppOptimizer.updateUseCloudFreezerHook(switchState);
    }

    public boolean getFreezerCloudSwitch() {
        Slog.d(TAG_FREEZER, "getFreezerCloudSwitch" + this.mCloudFreezerSwitch);
        return this.mCloudFreezerSwitch;
    }

    public static boolean hasBroadcastFinishCallback(IIntentReceiver resultTo) {
        return resultTo != null;
    }
}
