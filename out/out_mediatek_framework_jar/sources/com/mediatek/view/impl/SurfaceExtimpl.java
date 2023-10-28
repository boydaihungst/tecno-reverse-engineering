package com.mediatek.view.impl;

import android.content.pm.IPackageManager;
import android.os.Binder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import com.mediatek.appresolutiontuner.ResolutionTunerAppList;
import com.mediatek.boostfwk.policy.refreshrate.RefreshRateInfo;
import com.mediatek.boostfwk.utils.Config;
import com.mediatek.view.SurfaceExt;
/* loaded from: classes.dex */
public class SurfaceExtimpl extends SurfaceExt {
    private static final String TAG = "SurfaceExt";
    private String mPackageName;
    private static final boolean ENABLE_WHITE_LIST = SystemProperties.getBoolean("debug.enable.whitelist", false);
    private static final int ENABLE_RESOLUTION_TUNING = SystemProperties.getInt("ro.vendor.app_resolution_tuner", 0) | SystemProperties.getInt("ro.vendor.tgpa_resolution_tuner", 0);
    private static final String[] WHITE_LIST = {"com.tencent.qqlive"};
    private static ResolutionTunerAppList mApplist = null;
    private static final boolean APP_RESOLUTION_TUNING_AI_ENABLE = Config.USER_CONFIG_DEFAULT_TYPE.equals(SystemProperties.get("ro.vendor.game_aisr_enable"));
    private boolean mIsContainPackageName = false;
    private boolean mIsScaledByGameMode = false;
    private float mXScaleValue = 1.0f;
    private float mYScaleValue = 1.0f;

    public boolean isInWhiteList() {
        if (ENABLE_WHITE_LIST) {
            return true;
        }
        String packageName = getCallerProcessName();
        String[] strArr = WHITE_LIST;
        if (strArr == null || packageName == null) {
            return false;
        }
        for (String item : strArr) {
            if (item.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    public void initResolutionTunner() {
        if (ENABLE_RESOLUTION_TUNING == 1 && mApplist == null) {
            this.mPackageName = getCallerProcessName();
            ResolutionTunerAppList tunerList = getTunerList();
            mApplist = tunerList;
            tunerList.loadTunerAppList();
            this.mIsContainPackageName = mApplist.isScaledBySurfaceView(this.mPackageName);
            this.mIsScaledByGameMode = mApplist.isScaledByGameMode(this.mPackageName);
            if (mApplist.getScaleWidth(this.mPackageName) == RefreshRateInfo.DECELERATION_RATE || mApplist.getScaleHeight(this.mPackageName) == RefreshRateInfo.DECELERATION_RATE) {
                float scaleValue = mApplist.getScaleValue(this.mPackageName);
                this.mXScaleValue = scaleValue;
                this.mYScaleValue = scaleValue;
            } else {
                this.mXScaleValue = mApplist.getScaleWidth(this.mPackageName);
                this.mYScaleValue = mApplist.getScaleHeight(this.mPackageName);
            }
            Log.d(TAG, "initResolutionTunner, mPackageName:" + this.mPackageName + ",mContainPackageName:" + this.mIsContainPackageName + "mXScaleValue:" + this.mXScaleValue + ",mYScaleValue:" + this.mYScaleValue);
        }
    }

    public boolean isResolutionTuningPackage() {
        return this.mIsContainPackageName;
    }

    public boolean isScaledByGameMode() {
        return this.mIsScaledByGameMode;
    }

    public float getXScale() {
        return this.mXScaleValue;
    }

    public float getYScale() {
        return this.mYScaleValue;
    }

    private ResolutionTunerAppList getTunerList() {
        ResolutionTunerAppList applist = ResolutionTunerAppList.getInstance();
        applist.loadTunerAppList();
        return applist;
    }

    private String getCallerProcessName() {
        int binderuid = Binder.getCallingUid();
        IPackageManager pm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        if (pm != null) {
            try {
                String callingApp = pm.getNameForUid(binderuid);
                return callingApp;
            } catch (RemoteException e) {
                Log.e(TAG, "getCallerProcessName exception :" + e);
                return null;
            }
        }
        return null;
    }

    public void updateSurfaceViewScale(String packName, float scale) {
        if (ENABLE_RESOLUTION_TUNING == 1) {
            this.mPackageName = packName;
            ResolutionTunerAppList resolutionTunerAppList = ResolutionTunerAppList.getInstance();
            mApplist = resolutionTunerAppList;
            if (resolutionTunerAppList != null) {
                boolean isScaledBySurfaceView = resolutionTunerAppList.isScaledBySurfaceView(this.mPackageName);
                this.mIsContainPackageName = isScaledBySurfaceView;
                if (isScaledBySurfaceView) {
                    this.mXScaleValue = scale;
                    this.mYScaleValue = scale;
                }
            }
            Log.d(TAG, "SurfaceExtimpl, updateSurfaceViewScale, mPackageName:" + this.mPackageName + ",mIsContainPackageName:" + this.mIsContainPackageName);
        }
    }

    public void setSurfaceViewScale(String packName, String scale) {
        if (ENABLE_RESOLUTION_TUNING == 1) {
            this.mPackageName = packName;
            ResolutionTunerAppList tunerList = getTunerList();
            mApplist = tunerList;
            if (tunerList != null) {
                boolean isScaledBySurfaceView = tunerList.isScaledBySurfaceView(this.mPackageName);
                this.mIsContainPackageName = isScaledBySurfaceView;
                if (isScaledBySurfaceView) {
                    mApplist.reLoadTunerAppList(packName, scale);
                }
            }
            Log.d(TAG, "SurfaceExtimpl, mPackageName:" + this.mPackageName + ",mIsContainPackageName:" + this.mIsContainPackageName);
        }
    }
}
