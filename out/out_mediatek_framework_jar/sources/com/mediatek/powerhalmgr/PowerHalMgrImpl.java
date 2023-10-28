package com.mediatek.powerhalmgr;

import android.content.Context;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.mediatek.powerhalmgr.IPowerHalMgr;
/* loaded from: classes.dex */
public class PowerHalMgrImpl extends PowerHalMgr {
    private static final String TAG = "PowerHalMgrImpl";
    private Context mContext;
    private static PowerHalMgrImpl sInstance = null;
    private static Object lock = new Object();
    private IPowerHalMgr sService = null;
    private int inited = 0;
    private int setTid = 0;
    private long mPreviousTime = 0;

    public static native int nativeGetPid();

    public static native int nativeGetTid();

    private void init() {
        IBinder b;
        if (this.inited == 0 && (b = ServiceManager.checkService("power_hal_mgr_service")) != null) {
            IPowerHalMgr asInterface = IPowerHalMgr.Stub.asInterface(b);
            this.sService = asInterface;
            if (asInterface != null) {
                this.inited = 1;
            } else {
                loge("ERR: getService() sService is still null..");
            }
        }
    }

    public static PowerHalMgrImpl getInstance() {
        PowerHalMgrImpl powerHalMgrImpl;
        synchronized (lock) {
            if (sInstance == null) {
                sInstance = new PowerHalMgrImpl();
            }
            powerHalMgrImpl = sInstance;
        }
        return powerHalMgrImpl;
    }

    public int scnReg() {
        try {
            init();
            IPowerHalMgr iPowerHalMgr = this.sService;
            if (iPowerHalMgr == null) {
                return -1;
            }
            int handle = iPowerHalMgr.scnReg();
            return handle;
        } catch (RemoteException e) {
            loge("ERR: RemoteException in scnReg:" + e);
            return -1;
        }
    }

    public void scnConfig(int handle, int cmd, int param_1, int param_2, int param_3, int param_4) {
        try {
            init();
            IPowerHalMgr iPowerHalMgr = this.sService;
            if (iPowerHalMgr != null) {
                iPowerHalMgr.scnConfig(handle, cmd, param_1, param_2, param_3, param_4);
            }
        } catch (RemoteException e) {
            loge("ERR: RemoteException in scnConfig:" + e);
        }
    }

    public void scnUnreg(int handle) {
        try {
            init();
            IPowerHalMgr iPowerHalMgr = this.sService;
            if (iPowerHalMgr != null) {
                iPowerHalMgr.scnUnreg(handle);
            }
        } catch (RemoteException e) {
            loge("ERR: RemoteException in scnUnreg:" + e);
        }
    }

    public void scnEnable(int handle, int timeout) {
        try {
            init();
            IPowerHalMgr iPowerHalMgr = this.sService;
            if (iPowerHalMgr != null) {
                iPowerHalMgr.scnEnable(handle, timeout);
            }
        } catch (RemoteException e) {
            loge("ERR: RemoteException in scnEnable:" + e);
        }
    }

    public void scnDisable(int handle) {
        try {
            init();
            IPowerHalMgr iPowerHalMgr = this.sService;
            if (iPowerHalMgr != null) {
                iPowerHalMgr.scnDisable(handle);
            }
        } catch (RemoteException e) {
            loge("ERR: RemoteException in scnDisable:" + e);
        }
    }

    public void scnUltraCfg(int handle, int ultracmd, int param_1, int param_2, int param_3, int param_4) {
        try {
            init();
            IPowerHalMgr iPowerHalMgr = this.sService;
            if (iPowerHalMgr != null) {
                iPowerHalMgr.scnUltraCfg(handle, ultracmd, param_1, param_2, param_3, param_4);
            }
        } catch (RemoteException e) {
            loge("ERR: RemoteException in scnConfig:" + e);
        }
    }

    public void mtkCusPowerHint(int hint, int data) {
        try {
            init();
            IPowerHalMgr iPowerHalMgr = this.sService;
            if (iPowerHalMgr != null) {
                iPowerHalMgr.mtkCusPowerHint(hint, data);
            }
        } catch (RemoteException e) {
            loge("ERR: RemoteException in mtkCusPowerHint:" + e);
        }
    }

    public void getCpuCap() {
        log("getCpuCap");
    }

    public void getGpuCap() {
        log("getGpuCap");
    }

    public void getGpuRTInfo() {
        log("getGpuRTInfo");
    }

    public void getCpuRTInfo() {
        log("getCpuRTInfo");
    }

    public void UpdateManagementPkt(int type, String packet) {
        try {
            init();
            IPowerHalMgr iPowerHalMgr = this.sService;
            if (iPowerHalMgr != null) {
                iPowerHalMgr.UpdateManagementPkt(type, packet);
            }
        } catch (RemoteException e) {
            loge("ERR: RemoteException in UpdateManagementPkt:" + e);
        }
    }

    public void setForegroundSports() {
        try {
            init();
            IPowerHalMgr iPowerHalMgr = this.sService;
            if (iPowerHalMgr != null) {
                iPowerHalMgr.setForegroundSports();
            }
        } catch (RemoteException e) {
            loge("ERR: RemoteException in setForegroundSports:" + e);
        }
    }

    public void setSysInfo(int type, String data) {
        try {
            init();
            IPowerHalMgr iPowerHalMgr = this.sService;
            if (iPowerHalMgr != null) {
                iPowerHalMgr.setSysInfo(type, data);
            }
        } catch (RemoteException e) {
            loge("ERR: RemoteException in setSysInfo:" + e);
        }
    }

    public boolean startDuplicatePacketPrediction() {
        logd("startDuplicatePacketPrediction()");
        try {
            init();
            IPowerHalMgr iPowerHalMgr = this.sService;
            if (iPowerHalMgr == null) {
                return false;
            }
            boolean status = iPowerHalMgr.startDuplicatePacketPrediction();
            return status;
        } catch (RemoteException e) {
            loge("ERR: RemoteException in startDuplicatePacketPrediction:" + e);
            return false;
        }
    }

    public boolean stopDuplicatePacketPrediction() {
        logd("stopDuplicatePacketPrediction()");
        try {
            init();
            IPowerHalMgr iPowerHalMgr = this.sService;
            if (iPowerHalMgr == null) {
                return false;
            }
            boolean status = iPowerHalMgr.stopDuplicatePacketPrediction();
            return status;
        } catch (RemoteException e) {
            loge("ERR: RemoteException in stopDuplicatePacketPrediction:" + e);
            return false;
        }
    }

    public boolean isDupPacketPredictionStarted() {
        try {
            init();
            IPowerHalMgr iPowerHalMgr = this.sService;
            if (iPowerHalMgr != null) {
                boolean enable = iPowerHalMgr.isDupPacketPredictionStarted();
                logd("isDupPacketPredictionStarted() enable:" + enable);
                return enable;
            }
            return false;
        } catch (RemoteException e) {
            loge("ERR: RemoteException in isDupPacketPredictionStarted:" + e);
            return false;
        }
    }

    public boolean registerDuplicatePacketPredictionEvent(IRemoteCallback listener) {
        logd("registerDuplicatePacketPredictionEvent() " + listener.getClass().toString());
        try {
            init();
            IPowerHalMgr iPowerHalMgr = this.sService;
            if (iPowerHalMgr == null) {
                return false;
            }
            boolean status = iPowerHalMgr.registerDuplicatePacketPredictionEvent(listener);
            return status;
        } catch (RemoteException e) {
            loge("ERR: RemoteException in registerDuplicatePacketPredictionEvent:" + e);
            return false;
        }
    }

    public boolean unregisterDuplicatePacketPredictionEvent(IRemoteCallback listener) {
        logd("unregisterDuplicatePacketPredictionEvent() " + listener.getClass().toString());
        try {
            init();
            IPowerHalMgr iPowerHalMgr = this.sService;
            if (iPowerHalMgr == null) {
                return false;
            }
            boolean status = iPowerHalMgr.unregisterDuplicatePacketPredictionEvent(listener);
            return status;
        } catch (RemoteException e) {
            loge("ERR: RemoteException in unregisterDuplicatePacketPredictionEvent:" + e);
            return false;
        }
    }

    public boolean updateMultiDuplicatePacketLink(DupLinkInfo[] linkList) {
        try {
            init();
            IPowerHalMgr iPowerHalMgr = this.sService;
            if (iPowerHalMgr == null) {
                return false;
            }
            boolean status = iPowerHalMgr.updateMultiDuplicatePacketLink(linkList);
            return status;
        } catch (RemoteException e) {
            loge("ERR: RemoteException in updateMultiDuplicatePacketLink:" + e);
            return false;
        }
    }

    public boolean setPriorityByUid(int action, int uid) {
        try {
            init();
            IPowerHalMgr iPowerHalMgr = this.sService;
            if (iPowerHalMgr == null) {
                return false;
            }
            boolean status = iPowerHalMgr.setPriorityByUid(action, uid);
            return status;
        } catch (RemoteException e) {
            loge("ERR: RemoteException in setPriorityByUid:" + e);
            return false;
        }
    }

    public boolean setPriorityByLinkinfo(int action, DupLinkInfo linkinfo) {
        try {
            init();
            IPowerHalMgr iPowerHalMgr = this.sService;
            if (iPowerHalMgr == null) {
                return false;
            }
            boolean status = iPowerHalMgr.setPriorityByLinkinfo(action, linkinfo);
            return status;
        } catch (RemoteException e) {
            loge("ERR: RemoteException in setPriorityByLinkinfo:" + e);
            return false;
        }
    }

    public boolean flushPriorityRules(int type) {
        try {
            init();
            IPowerHalMgr iPowerHalMgr = this.sService;
            if (iPowerHalMgr == null) {
                return false;
            }
            boolean status = iPowerHalMgr.flushPriorityRules(type);
            return status;
        } catch (RemoteException e) {
            loge("ERR: RemoteException in flushPriorityRules:" + e);
            return false;
        }
    }

    public boolean configBoosterInfo(BoosterInfo info) {
        if (info == null) {
            loge("[Booster]: info == null");
            return false;
        } else if (info.getGroup() > BoosterInfo.BOOSTER_GROUP_MAX || info.getGroup() <= BoosterInfo.BOOSTER_GROUP_BASE) {
            loge("[Booster]: Unsupported group " + info.getGroup());
            return false;
        } else {
            try {
                init();
                IPowerHalMgr iPowerHalMgr = this.sService;
                if (iPowerHalMgr == null) {
                    return false;
                }
                boolean status = iPowerHalMgr.configBoosterInfo(info);
                return status;
            } catch (RemoteException e) {
                loge("[Booster]: RemoteException in configBoosterInfo: " + e);
                return false;
            }
        }
    }

    public void setPredictInfo(String pack_name, int uid) {
        try {
            init();
            IPowerHalMgr iPowerHalMgr = this.sService;
            if (iPowerHalMgr != null) {
                iPowerHalMgr.setPredictInfo(pack_name, uid);
            }
        } catch (RemoteException e) {
            loge("ERR: RemoteException in setPredictInfo:" + e);
        }
    }

    public int perfLockAcquire(int handle, int duration, int[] list) {
        try {
            init();
            IPowerHalMgr iPowerHalMgr = this.sService;
            if (iPowerHalMgr != null) {
                return iPowerHalMgr.perfLockAcquire(handle, duration, list);
            }
            return handle;
        } catch (RemoteException e) {
            loge("ERR: RemoteException in perfLockAcquire:" + e);
            return handle;
        }
    }

    public void perfLockRelease(int handle) {
        try {
            init();
            IPowerHalMgr iPowerHalMgr = this.sService;
            if (iPowerHalMgr != null) {
                iPowerHalMgr.perfLockRelease(handle);
            }
        } catch (RemoteException e) {
            loge("ERR: RemoteException in perfLockRelease:" + e);
        }
    }

    public int perfCusLockHint(int hint, int duration) {
        try {
            init();
            IPowerHalMgr iPowerHalMgr = this.sService;
            if (iPowerHalMgr == null) {
                return -1;
            }
            int handle = iPowerHalMgr.perfCusLockHint(hint, duration);
            return handle;
        } catch (RemoteException e) {
            loge("ERR: RemoteException in perfCusLockHint:" + e);
            return -1;
        }
    }

    public int querySysInfo(int cmd, int param) {
        try {
            init();
            IPowerHalMgr iPowerHalMgr = this.sService;
            if (iPowerHalMgr == null) {
                return -1;
            }
            int value = iPowerHalMgr.querySysInfo(cmd, param);
            return value;
        } catch (RemoteException e) {
            loge("ERR: RemoteException in perfLockAcquire:" + e);
            return -1;
        }
    }

    public void mtkPowerHint(int hint, int data) {
        try {
            init();
            IPowerHalMgr iPowerHalMgr = this.sService;
            if (iPowerHalMgr != null) {
                iPowerHalMgr.mtkPowerHint(hint, data);
            }
        } catch (RemoteException e) {
            loge("ERR: RemoteException in mtkPowerHint:" + e);
        }
    }

    public int setSysInfoSync(int type, String data) {
        try {
            init();
            IPowerHalMgr iPowerHalMgr = this.sService;
            if (iPowerHalMgr == null) {
                return -1;
            }
            int ret = iPowerHalMgr.setSysInfoSync(type, data);
            return ret;
        } catch (RemoteException e) {
            loge("ERR: RemoteException in setPredictInfo:" + e);
            return -1;
        }
    }

    private void log(String info) {
        Log.i(TAG, info + " ");
    }

    private void logd(String info) {
        Log.d(TAG, info + " ");
    }

    private void loge(String info) {
        Log.e(TAG, "ERR: " + info + " ");
    }
}
