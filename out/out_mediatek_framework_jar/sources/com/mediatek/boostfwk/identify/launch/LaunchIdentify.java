package com.mediatek.boostfwk.identify.launch;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.WindowManager;
import com.mediatek.boostfwk.identify.BaseIdentify;
import com.mediatek.boostfwk.info.ActivityInfo;
import com.mediatek.boostfwk.policy.launch.LaunchPolicy;
import com.mediatek.boostfwk.scenario.BasicScenario;
import com.mediatek.boostfwk.scenario.launch.LaunchScenario;
import com.mediatek.boostfwk.utils.Config;
import com.mediatek.boostfwk.utils.LogUtil;
import com.mediatek.boostfwk.utils.Util;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
/* loaded from: classes.dex */
public class LaunchIdentify extends BaseIdentify {
    public static final String HOSTTYPE_ACTIVITY = "activity";
    private static final String TAG = "SBE-LaunchIdentify";
    public static final String THREAD_NAME = "launch";
    private String mPkgName;
    private static LaunchIdentify sInstance = null;
    private static Object lock = new Object();
    private HandlerThread mWorkerThread = null;
    private WorkerHandler mWorkerHandler = null;
    private boolean mIsBegin = false;
    private int mCount = 0;
    private List<String> mSpecialPkgNames = new ArrayList();
    private LaunchConfig mLaunchConfig = new LaunchConfig();
    private LaunchPolicy mLaunchPolicy = new LaunchPolicy();

    public static LaunchIdentify getInstance() {
        if (sInstance == null) {
            synchronized (lock) {
                if (sInstance == null) {
                    sInstance = new LaunchIdentify();
                }
            }
        }
        return sInstance;
    }

    private void initThread() {
        HandlerThread handlerThread = this.mWorkerThread;
        if (handlerThread != null && handlerThread.isAlive() && this.mWorkerHandler != null) {
            LogUtil.mLogi(TAG, "re-init");
            return;
        }
        HandlerThread handlerThread2 = new HandlerThread(THREAD_NAME);
        this.mWorkerThread = handlerThread2;
        handlerThread2.start();
        Looper looper = this.mWorkerThread.getLooper();
        if (looper == null) {
            LogUtil.mLogi(TAG, "Thread looper is null");
        } else {
            this.mWorkerHandler = new WorkerHandler(looper);
        }
    }

    public LaunchIdentify() {
        initSpecialMap();
        initThread();
    }

    @Override // com.mediatek.boostfwk.identify.BaseIdentify
    public boolean isMainThreadOnly() {
        return false;
    }

    @Override // com.mediatek.boostfwk.identify.BaseIdentify
    public boolean dispatchScenario(BasicScenario basicScenario) {
        if (basicScenario == null) {
            LogUtil.mLogw(TAG, "No Launch scenario to dispatcher.");
            return false;
        }
        LaunchScenario scenario = (LaunchScenario) basicScenario;
        int action = scenario.getScenarioAction();
        if (Config.isBoostFwkLogEnable()) {
            LogUtil.mLogd(TAG, "Launch action dispatcher to = " + action);
        }
        switch (action) {
            case 1:
                launchHintCheck(scenario.getBoostStatus(), scenario.getHostingType(), scenario.getPackageName(), scenario.getAttrs(), scenario.getActivityName(), scenario.getIsComeFromIdle());
                return true;
            case 2:
            default:
                LogUtil.mLogw(TAG, "Not found dispatcher launch action.");
                return true;
            case 3:
                WeakReference<Context> weakReference = scenario.getActivity();
                if (weakReference != null && weakReference.get() != null) {
                    if (Config.isBoostFwkLogEnable()) {
                        LogUtil.mLogd(TAG, "ACTIVITY_SWITCH set new context -->" + weakReference.get());
                    }
                    ActivityInfo.getInstance().setContext(weakReference.get());
                    weakReference.clear();
                    return true;
                }
                return true;
        }
    }

    public void launchHintCheck(int boostStaus, String hostingType, String pkgName, WindowManager.LayoutParams attrs, String activityName, boolean isComeFromIdle) {
        switch (boostStaus) {
            case 0:
                boostHintBegin(hostingType, pkgName);
                return;
            case 1:
                if (isComeFromIdle) {
                    boostHintEnd(pkgName, attrs);
                    return;
                } else {
                    boostHintEndForSpecial(pkgName, activityName);
                    return;
                }
            default:
                LogUtil.mLogw(TAG, "Not found dispatcher launch action.");
                return;
        }
    }

    public void boostHintBegin(String hostingType, String pkgName) {
        if (Config.isBoostFwkLogEnable()) {
            LogUtil.mLogd(TAG, "boostHintBegin for hostingType= " + hostingType + "; pkgName= " + pkgName);
        }
        if (hostingType != null && hostingType.contains(HOSTTYPE_ACTIVITY) && !Util.isSystemApp(pkgName)) {
            LogUtil.mLogd(TAG, "SBE boost:" + pkgName + " begin");
            this.mPkgName = pkgName;
            this.mIsBegin = true;
            this.mCount = 0;
        }
    }

    public void boostHintEndForSpecial(String pkgName, String activityName) {
        if (Config.isBoostFwkLogEnable()) {
            LogUtil.mLogd(TAG, "boostHintResume for pkgName= " + pkgName + ", activityName= " + activityName + ", mLaunchConfig.isInSpecialList(pkgName) = " + isInSpecialList(pkgName));
        }
        String str = this.mPkgName;
        if (str != null && str.equals(pkgName) && isInSpecialList(pkgName)) {
            this.mCount++;
            int configCount = getActivityCount(pkgName);
            if (this.mCount == configCount) {
                WorkerHandler workerHandler = this.mWorkerHandler;
                workerHandler.sendMessageDelayed(workerHandler.obtainMessage(2, pkgName), 1000L);
                this.mCount = 0;
            }
        }
    }

    public void boostHintEnd(String pkgName, WindowManager.LayoutParams attrs) {
        if (Config.isBoostFwkLogEnable()) {
            LogUtil.mLogd(TAG, "boostHintEnd for pkgName = " + pkgName + ", mPkgName = " + this.mPkgName + ", isGameApp = " + Util.isGameApp(pkgName) + ", isSpecialApp = " + isInSpecialList(pkgName) + ", isFullScreen = " + Util.IsFullScreen(attrs));
        }
        String str = this.mPkgName;
        if (str != null && str.equals(pkgName) && !Util.isSystemApp(pkgName) && !isInSpecialList(pkgName) && !Util.isGameApp(pkgName) && !Util.IsFullScreen(attrs)) {
            this.mWorkerHandler.removeMessages(3, pkgName);
            WorkerHandler workerHandler = this.mWorkerHandler;
            workerHandler.sendMessageDelayed(workerHandler.obtainMessage(3, pkgName), 1000L);
        }
    }

    /* loaded from: classes.dex */
    public class WorkerHandler extends Handler {
        public static final int MSG_ACTIVITY_IDLE = 3;
        public static final int MSG_ACTIVITY_RESUME = 2;
        public static final int MSG_PROCESS_START = 1;

        WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 2:
                case 3:
                    LaunchIdentify.this.boostEnd((String) msg.obj);
                    return;
                default:
                    return;
            }
        }
    }

    public void boostEnd(String pkgName) {
        if (this.mIsBegin) {
            LogUtil.mLogd(TAG, "SBE boost:" + pkgName + " end");
            this.mLaunchPolicy.boostEnd(pkgName);
            this.mIsBegin = false;
            this.mPkgName = null;
            this.mCount = 0;
        }
    }

    public void initSpecialMap() {
        for (Map.Entry<String, String> entry : LaunchConfig.SPECIAL_MAP.entrySet()) {
            this.mSpecialPkgNames.add(entry.getKey());
        }
    }

    private boolean isInSpecialList(String pkgName) {
        List<String> list = this.mSpecialPkgNames;
        if (list != null && list.contains(pkgName)) {
            return true;
        }
        return false;
    }

    private int getActivityCount(String pkgName) {
        if (LaunchConfig.SPECIAL_MAP.get(pkgName) != null) {
            return Integer.parseInt(LaunchConfig.SPECIAL_MAP.get(pkgName));
        }
        return 1;
    }
}
