package com.transsion.powerhub;

import android.app.Notification;
import android.graphics.Point;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Slog;
import android.view.Display;
import android.view.View;
/* loaded from: classes4.dex */
public class TranLayerControlManager {
    private static final int ERR_ACTIVITY = 268435456;
    private static final int MINI_FRAME_TIME = 30;
    private static final String TAG = "TranLayerControlManager";
    private static final int VIDEO_STOP_TIME = 120;
    private static final int VIEW_MARQEE = 4;
    private static final int VIEW_SKIPSIZE = 1;
    private static final int VIEW_VIDEO = 2;
    private TranPowerhubManager mPowerhubManager;
    private boolean bInVideo = false;
    private long timeVideo = 0;
    private int screenWidth = 0;
    private int screenHeight = 0;
    private String strSkipActivityName = "";
    private float skipSize = 0.0f;
    private String strSkipVideoName = "";
    private String strSkipMarqeeName = "";
    private boolean bSkipEnable = false;
    private long useStartTime = 0;
    private long useEndTime = 0;
    private long optStartTime = 0;
    private long optTotleTime = 0;
    private boolean bHaveSkip = false;
    private int errCode = 0;
    private String strSkipPkgName = "";

    public TranLayerControlManager(TranPowerhubManager powerHub) {
        this.mPowerhubManager = powerHub;
    }

    private int calcDelayTime(int retFlags) {
        boolean isVideo = (retFlags & 2) == 2;
        boolean isSkip = (retFlags & 1) == 1;
        boolean bLastInVideo = this.bInVideo;
        long currentMilliSeconds = System.currentTimeMillis();
        if (isVideo) {
            this.timeVideo = currentMilliSeconds;
            this.bInVideo = true;
        } else if (this.bInVideo && currentMilliSeconds - this.timeVideo > 120) {
            this.bInVideo = false;
        }
        boolean z = this.bInVideo;
        if (bLastInVideo != z) {
            if (z) {
                this.optStartTime = currentMilliSeconds;
            } else {
                long j = this.optStartTime;
                if (j > 0) {
                    this.optTotleTime += currentMilliSeconds - j;
                }
            }
        }
        if (!z || !isSkip || isVideo) {
            return z ? 30 : 0;
        }
        this.bHaveSkip = true;
        return -1;
    }

    public int shouldSkip(ITranPowerhubManager sm, String strPkgName, String strActivityName, View descendant) {
        if (!this.bSkipEnable || this.screenWidth == 0 || this.screenHeight == 0 || strPkgName == null || strActivityName == null || descendant == null) {
            return 0;
        }
        boolean mIsViewSkiplogSupport = SystemProperties.get("debug.product.viewlog.support").equals("1");
        String className = descendant.getClass().getName();
        int nWidth = descendant.getWidth();
        int nHeight = descendant.getHeight();
        int retFlags = analysisView(strPkgName, strActivityName, className, (nWidth * nHeight) / (this.screenWidth * this.screenHeight));
        if (retFlags == 0) {
            if (mIsViewSkiplogSupport) {
                Slog.d(TAG, "shouldSkip retFlags =0 strPkgName:" + strPkgName + " strActivityName:" + strActivityName + " className:" + className + " strPkgName:" + strPkgName + " (nWidth*nHeight)/(screenWidth*screenHeight):" + ((nWidth * nHeight) / (this.screenWidth * this.screenHeight)));
            }
            return 0;
        }
        int delayTime = calcDelayTime(retFlags);
        if (mIsViewSkiplogSupport) {
            int[] locationScreen = new int[2];
            descendant.getLocationOnScreen(locationScreen);
            Slog.d(TAG, "shouldSkip className:" + className + " strActivityName:" + strActivityName + " nWidth:" + nWidth + " nHeight:" + nHeight + " screenX:" + locationScreen[0] + " screenY:" + locationScreen[1] + " screenWidth:" + this.screenWidth + " screenHeight:" + this.screenHeight + " (nWidth*nHeight)/(screenWidth*screenHeight):" + ((nWidth * nHeight) / (this.screenWidth * this.screenHeight)) + " retFlags:" + retFlags + " delayTime:" + delayTime);
        }
        return delayTime;
    }

    public void setDispSize(Display mDisplay) {
        Point size = new Point();
        mDisplay.getRealSize(size);
        this.screenWidth = size.x;
        this.screenHeight = size.y;
    }

    public boolean initSkipInfo(ITranPowerhubManager sm, String strPkgName) {
        Bundle bundle;
        try {
            Bundle viewInfo = new Bundle();
            viewInfo.putString("pkgname", strPkgName);
            bundle = sm.getSkipInfo(viewInfo);
        } catch (RemoteException e) {
            Slog.d(TAG, e.toString());
        }
        if (bundle == null) {
            Slog.d(TAG, "initSkipInfo getSkipInfo fail strPkgName:" + strPkgName);
            return false;
        }
        this.strSkipActivityName = bundle.getString("actname");
        this.skipSize = bundle.getFloat("skipsize");
        this.strSkipVideoName = bundle.getString("videoname");
        this.strSkipMarqeeName = bundle.getString("marqeename");
        if (this.skipSize > 0.0f && !TextUtils.isEmpty(this.strSkipActivityName) && !TextUtils.isEmpty(this.strSkipVideoName)) {
            this.bSkipEnable = true;
        }
        if (this.bSkipEnable) {
            this.strSkipPkgName = strPkgName;
            this.useStartTime = System.currentTimeMillis();
        }
        return this.bSkipEnable;
    }

    private boolean isContainsInList(String strSource, String searchList) {
        if (TextUtils.isEmpty(strSource) || TextUtils.isEmpty(searchList)) {
            return false;
        }
        String[] split = searchList.split(" ");
        for (int i = 0; i < split.length; i++) {
            if (!TextUtils.isEmpty(split[i]) && strSource.contains(split[i])) {
                return true;
            }
        }
        return false;
    }

    public int analysisView(String strPkgName, String strActivityName, String className, float viewSize) {
        int retFlags = 0;
        if (isContainsInList(strActivityName, this.strSkipActivityName)) {
            if (viewSize < this.skipSize) {
                retFlags = 0 | 1;
            }
            if (isContainsInList(className, this.strSkipVideoName)) {
                retFlags |= 2;
            }
            if (isContainsInList(className, this.strSkipMarqeeName)) {
                retFlags |= 4;
            }
        } else {
            this.errCode |= 268435456;
        }
        this.errCode |= retFlags;
        return retFlags;
    }

    public void updateCollectData(boolean bStopped) {
        if (!this.bSkipEnable) {
            Slog.d(TAG, "updateCollectData not enable");
            return;
        }
        long currentTime = System.currentTimeMillis();
        if (!bStopped) {
            this.useStartTime = currentTime;
            return;
        }
        ITranPowerhubManager sm = TranPowerhubManager.getService();
        if (sm == null) {
            Slog.d(TAG, "updateCollectData sm null");
            return;
        }
        long useTime = currentTime - this.useStartTime;
        try {
            Bundle dataInfo = new Bundle();
            dataInfo.putString("pkgname", this.strSkipPkgName);
            dataInfo.putInt("utime", Math.round(((float) useTime) / 60000.0f));
            int err = 0;
            long oTime = this.optTotleTime;
            if (!this.bHaveSkip) {
                err = this.errCode;
                oTime = 0;
            } else if (0 == oTime || oTime > useTime) {
                oTime = useTime;
            }
            dataInfo.putInt("otime", Math.round(((float) oTime) / 60000.0f));
            dataInfo.putInt(Notification.CATEGORY_ERROR, err);
            sm.updateCollectData(dataInfo);
        } catch (RemoteException e) {
            Slog.d(TAG, e.toString());
        }
        this.optStartTime = 0L;
        this.optTotleTime = 0L;
        this.bHaveSkip = false;
        this.errCode = 0;
    }
}
