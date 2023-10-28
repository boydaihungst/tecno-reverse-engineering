package com.mediatek.powerhalwrapper;

import android.os.Binder;
import android.os.Process;
import android.os.Trace;
import android.util.Log;
import com.mediatek.boostfwk.identify.launch.LaunchIdentify;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
/* loaded from: classes.dex */
public class PowerHalWrapper {
    private static final int AMS_BOOST_TIME = 10000;
    public static final int MAX_NETD_IP_FILTER_COUNT = 3;
    public static final int MTKPOWER_CMD_GET_POWER_SCN_TYPE = 105;
    public static final int MTKPOWER_CMD_GET_RILD_CAP = 40;
    private static final int MTKPOWER_HINT_ACT_SWITCH = 23;
    private static final int MTKPOWER_HINT_ALWAYS_ENABLE = 268435455;
    private static final int MTKPOWER_HINT_APP_ROTATE = 24;
    private static final int MTKPOWER_HINT_EXT_LAUNCH = 30;
    private static final int MTKPOWER_HINT_GALLERY_BOOST = 26;
    private static final int MTKPOWER_HINT_PACK_SWITCH = 22;
    private static final int MTKPOWER_HINT_PMS_INSTALL = 29;
    private static final int MTKPOWER_HINT_PROCESS_CREATE = 21;
    private static final int MTKPOWER_HINT_WFD = 28;
    private static final int MTKPOWER_HINT_WIPHY_SPEED_DL = 32;
    private static final int MTKPOWER_STATE_DEAD = 3;
    private static final int MTKPOWER_STATE_DESTORYED = 2;
    private static final int MTKPOWER_STATE_PAUSED = 0;
    private static final int MTKPOWER_STATE_RESUMED = 1;
    private static final int MTKPOWER_STATE_STOPPED = 4;
    public static final int PERF_RES_NET_MD_CRASH_PID = 41992960;
    public static final int PERF_RES_NET_WIFI_SMART_PREDICT = 41959680;
    public static final int PERF_RES_POWERHAL_SCREEN_OFF_STATE = 54525952;
    public static final int POWER_HIDL_SET_SYS_INFO = 0;
    public static final int SCN_PERF_LOCK_HINT = 3;
    public static final int SCN_USER_HINT = 2;
    public static final int SCREEN_OFF_DISABLE = 0;
    public static final int SCREEN_OFF_ENABLE = 1;
    public static final int SCREEN_OFF_WAIT_RESTORE = 2;
    public static final int SETSYS_FOREGROUND_SPORTS = 3;
    public static final int SETSYS_INTERNET_STATUS = 5;
    public static final int SETSYS_MANAGEMENT_PERIODIC = 4;
    public static final int SETSYS_MANAGEMENT_PREDICT = 1;
    public static final int SETSYS_NETD_BOOSTER_CONFIG = 18;
    public static final int SETSYS_NETD_CLEAR_FASTPATH_RULES = 17;
    public static final int SETSYS_NETD_DUPLICATE_PACKET_LINK = 8;
    public static final int SETSYS_NETD_SET_FASTPATH_BY_LINKINFO = 16;
    public static final int SETSYS_NETD_SET_FASTPATH_BY_UID = 15;
    public static final int SETSYS_NETD_STATUS = 6;
    public static final int SETSYS_PACKAGE_VERSION_NAME = 9;
    public static final int SETSYS_PREDICT_INFO = 7;
    public static final int SETSYS_SPORTS_APK = 2;
    private static final String TAG = "PowerHalWrapper";
    private static final int USER_DURATION_MAX = 30000;
    private static final boolean ENG = true;
    private static boolean AMS_BOOST_PROCESS_CREATE = ENG;
    private static boolean AMS_BOOST_PROCESS_CREATE_BOOST = ENG;
    private static boolean AMS_BOOST_PACK_SWITCH = ENG;
    private static boolean AMS_BOOST_ACT_SWITCH = ENG;
    private static boolean EXT_PEAK_PERF_MODE = false;
    private static PowerHalWrapper sInstance = null;
    private static Object lock = new Object();
    private static String mProcessCreatePack = null;
    public List<ScnList> scnlist = new ArrayList();
    private Lock mLock = new ReentrantLock();

    public static native int nativeMtkCusPowerHint(int i, int i2);

    public static native int nativeMtkPowerHint(int i, int i2);

    public static native int nativeNotifyAppState(String str, String str2, int i, int i2, int i3);

    public static native int nativeNotifySbeRescue(int i, int i2, int i3, long j);

    public static native int nativePerfCusLockHint(int i, int i2);

    public static native int nativePerfLockAcq(int i, int i2, int... iArr);

    public static native int nativePerfLockRel(int i);

    public static native int nativeQuerySysInfo(int i, int i2);

    public static native int nativeScnConfig(int i, int i2, int i3, int i4, int i5, int i6);

    public static native int nativeScnDisable(int i);

    public static native int nativeScnEnable(int i, int i2);

    public static native int nativeScnReg();

    public static native int nativeScnUltraCfg(int i, int i2, int i3, int i4, int i5, int i6);

    public static native int nativeScnUnreg(int i);

    public static native int nativeSetSysInfo(String str, int i);

    public static native int nativeSetSysInfoAsync(String str, int i);

    static {
        System.loadLibrary("powerhalwrap_jni");
    }

    public static PowerHalWrapper getInstance() {
        PowerHalWrapper powerHalWrapper;
        log("PowerHalWrapper.getInstance");
        synchronized (lock) {
            if (sInstance == null) {
                sInstance = new PowerHalWrapper();
            }
            powerHalWrapper = sInstance;
        }
        return powerHalWrapper;
    }

    private PowerHalWrapper() {
    }

    public void mtkPowerHint(int hint, int data) {
        nativeMtkPowerHint(hint, data);
    }

    public void mtkCusPowerHint(int hint, int data) {
        nativeMtkCusPowerHint(hint, data);
    }

    public void mtkNotifySbeRescue(int tid, int start, int enhance, long frameId) {
        nativeNotifySbeRescue(tid, start, enhance, frameId);
    }

    public int perfLockAcquire(int handle, int duration, int... list) {
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        int new_hdl = nativePerfLockAcq(handle, duration, list);
        if (new_hdl > 0 && new_hdl != handle && (duration > USER_DURATION_MAX || duration == 0)) {
            this.mLock.lock();
            ScnList user = new ScnList(new_hdl, pid, uid);
            this.scnlist.add(user);
            this.mLock.unlock();
        }
        return new_hdl;
    }

    public void perfLockRelease(int handle) {
        this.mLock.lock();
        List<ScnList> list = this.scnlist;
        if (list != null && list.size() > 0) {
            Iterator<ScnList> iter = this.scnlist.iterator();
            while (iter.hasNext()) {
                ScnList item = iter.next();
                if (item.gethandle() == handle) {
                    iter.remove();
                }
            }
        }
        this.mLock.unlock();
        nativePerfLockRel(handle);
    }

    public int perfCusLockHint(int hint, int duration) {
        return nativePerfCusLockHint(hint, duration);
    }

    public int scnReg() {
        loge("scnReg not support!!!");
        return -1;
    }

    public int scnConfig(int hdl, int cmd, int param_1, int param_2, int param_3, int param_4) {
        loge("scnConfig not support!!!");
        return 0;
    }

    public int scnUnreg(int hdl) {
        loge("scnUnreg not support!!!");
        return 0;
    }

    public int scnEnable(int hdl, int timeout) {
        loge("scnEnable not support!!!");
        return 0;
    }

    public int scnDisable(int hdl) {
        loge("scnDisable not support!!!");
        return 0;
    }

    public int scnUltraCfg(int hdl, int ultracmd, int param_1, int param_2, int param_3, int param_4) {
        loge("scnUltraCfg not support!!!");
        return 0;
    }

    public void getCpuCap() {
        log("getCpuCap");
    }

    public void getGpuCap() {
        log("mGpuCap");
    }

    public void getGpuRTInfo() {
        log("getGpuCap");
    }

    public void getCpuRTInfo() {
        log("mCpuRTInfo");
    }

    public void UpdateManagementPkt(int type, String packet) {
        logd("<UpdateManagementPkt> type:" + type + ", packet:" + packet);
        switch (type) {
            case 1:
                nativeSetSysInfo(packet, 1);
                return;
            case 4:
                nativeSetSysInfo(packet, 4);
                return;
            default:
                return;
        }
    }

    public int setSysInfo(int type, String data) {
        return nativeSetSysInfo(data, type);
    }

    public void setSysInfoAsync(int type, String data) {
        nativeSetSysInfoAsync(data, type);
    }

    public int querySysInfo(int cmd, int param) {
        logd("<querySysInfo> cmd:" + cmd + " param:" + param);
        return nativeQuerySysInfo(cmd, param);
    }

    public void galleryBoostEnable(int timeoutMs) {
        log("<galleryBoostEnable> do boost with " + timeoutMs + "ms");
        nativeMtkPowerHint(MTKPOWER_HINT_GALLERY_BOOST, timeoutMs);
    }

    public void setRotationBoost(int boostTime) {
        log("<setRotation> do boost with " + boostTime + "ms");
        nativeMtkPowerHint(MTKPOWER_HINT_APP_ROTATE, boostTime);
    }

    public void setSpeedDownload(int timeoutMs) {
        log("<setSpeedDownload> do boost with " + timeoutMs + "ms");
        nativeMtkPowerHint(32, timeoutMs);
    }

    public void setWFD(boolean enable) {
        log("<setWFD> enable:" + enable);
        if (enable) {
            nativeMtkPowerHint(MTKPOWER_HINT_WFD, MTKPOWER_HINT_ALWAYS_ENABLE);
        } else {
            nativeMtkPowerHint(MTKPOWER_HINT_WFD, 0);
        }
    }

    public void setSportsApk(String pack) {
        log("<setSportsApk> pack:" + pack);
        nativeSetSysInfo(pack, 2);
    }

    public void NotifyAppCrash(int pid, int uid, String packageName) {
        int found = 0;
        int myPid = Process.myPid();
        if (myPid == pid) {
            log("<NotifyAppCrash> pack:" + packageName + " ,pid:" + packageName + " == myPid:" + myPid);
            return;
        }
        nativeNotifyAppState(packageName, packageName, pid, 3, uid);
        this.mLock.lock();
        List<ScnList> list = this.scnlist;
        if (list != null && list.size() > 0) {
            Iterator<ScnList> iter = this.scnlist.iterator();
            while (iter.hasNext()) {
                ScnList item = iter.next();
                if (item.getpid() == pid) {
                    nativePerfLockRel(item.gethandle());
                    log("<NotifyAppCrash> pid:" + item.getpid() + " uid:" + item.getuid() + " handle:" + item.gethandle());
                    iter.remove();
                    found++;
                }
            }
        }
        this.mLock.unlock();
    }

    public boolean getRildCap(int uid) {
        if (nativeQuerySysInfo(40, uid) == 1) {
            return ENG;
        }
        return false;
    }

    public void setInstallationBoost(boolean enable) {
        log("<setInstallationBoost> enable:" + enable);
        if (enable) {
            nativeMtkPowerHint(MTKPOWER_HINT_PMS_INSTALL, 15000);
        } else {
            nativeMtkPowerHint(MTKPOWER_HINT_PMS_INSTALL, 0);
        }
    }

    public void amsBoostResume(String lastResumedPackageName, String nextResumedPackageName) {
        log("<amsBoostResume> last:" + lastResumedPackageName + ", next:" + nextResumedPackageName);
        Trace.asyncTraceBegin(64L, "amPerfBoost", 0);
        nativeMtkPowerHint(MTKPOWER_HINT_EXT_LAUNCH, 0);
        if (lastResumedPackageName == null || !lastResumedPackageName.equalsIgnoreCase(nextResumedPackageName)) {
            AMS_BOOST_PACK_SWITCH = ENG;
            nativeMtkPowerHint(MTKPOWER_HINT_PACK_SWITCH, AMS_BOOST_TIME);
            return;
        }
        AMS_BOOST_ACT_SWITCH = ENG;
        nativeMtkPowerHint(MTKPOWER_HINT_ACT_SWITCH, AMS_BOOST_TIME);
    }

    public void amsBoostProcessCreate(String hostingType, String packageName) {
        if (hostingType != null && hostingType.contains(LaunchIdentify.HOSTTYPE_ACTIVITY)) {
            log("amsBoostProcessCreate package:" + packageName);
            Trace.asyncTraceBegin(64L, "amPerfBoost", 0);
            AMS_BOOST_PROCESS_CREATE = ENG;
            AMS_BOOST_PROCESS_CREATE_BOOST = ENG;
            mProcessCreatePack = packageName;
            nativeMtkPowerHint(MTKPOWER_HINT_EXT_LAUNCH, 0);
            nativeMtkPowerHint(MTKPOWER_HINT_PROCESS_CREATE, AMS_BOOST_TIME);
        }
    }

    public void amsBoostStop() {
        log("amsBoostStop AMS_BOOST_PACK_SWITCH:" + AMS_BOOST_PACK_SWITCH + ", AMS_BOOST_ACT_SWITCH:" + AMS_BOOST_ACT_SWITCH + ", AMS_BOOST_PROCESS_CREATE:" + AMS_BOOST_PROCESS_CREATE);
        if (AMS_BOOST_PACK_SWITCH) {
            AMS_BOOST_PACK_SWITCH = false;
            nativeMtkPowerHint(MTKPOWER_HINT_PACK_SWITCH, 0);
        }
        if (AMS_BOOST_ACT_SWITCH) {
            AMS_BOOST_ACT_SWITCH = false;
            nativeMtkPowerHint(MTKPOWER_HINT_ACT_SWITCH, 0);
        }
        if (AMS_BOOST_PROCESS_CREATE) {
            AMS_BOOST_PROCESS_CREATE = false;
            nativeMtkPowerHint(MTKPOWER_HINT_PROCESS_CREATE, 0);
        }
        Trace.asyncTraceEnd(64L, "amPerfBoost", 0);
    }

    public void amsBoostNotify(int pid, String activityName, String packageName, int uid, int state) {
        log("amsBoostNotify pid:" + pid + ",activity:" + activityName + ", package:" + packageName + ", mProcessCreatePack" + mProcessCreatePack);
        log("state: " + state);
        nativeNotifyAppState(packageName, activityName, pid, state, uid);
        log("amsBoostNotify AMS_BOOST_PROCESS_CREATE_BOOST:" + AMS_BOOST_PROCESS_CREATE_BOOST);
        String str = mProcessCreatePack;
        if (str != null && packageName != null && AMS_BOOST_PROCESS_CREATE_BOOST && !str.equals(packageName) && state == 1) {
            AMS_BOOST_PROCESS_CREATE_BOOST = false;
        }
    }

    private static void log(String info) {
        Log.i(TAG, info + " ");
    }

    private static void logd(String info) {
        Log.d(TAG, info + " ");
    }

    private static void loge(String info) {
        Log.e(TAG, "ERR: " + info + " ");
    }
}
