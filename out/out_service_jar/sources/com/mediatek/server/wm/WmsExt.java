package com.mediatek.server.wm;

import android.view.DisplayInfo;
import android.view.WindowManager;
import com.android.server.wm.WindowState;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
public class WmsExt {
    public static final float DEFAULT_REFRESH_RATE = 999.0f;
    public static final String MSYNCTAG = "MSyncForWMS";
    public static final String TAG = "WindowManager";

    public boolean isAppResolutionTunerSupport() {
        return false;
    }

    public boolean isAppResolutionTunerAISupport() {
        return false;
    }

    public float getWindowScaleForAI(String packageName) {
        return 1.0f;
    }

    public void loadResolutionTunerAppList() {
    }

    public void setWindowScaleByWL(WindowState win, DisplayInfo displayInfo, WindowManager.LayoutParams attrs, int requestedWidth, int requestedHeight) {
    }

    public float getPreferredRefreshRate(String pkgName, String activityName) {
        return 0.0f;
    }

    public float getSpecialRefreshRate(int type, int action, String pkgName, String activityName) {
        return 0.0f;
    }

    public void loadMSyncCtrlTable() {
    }

    public float[] getRemoteRefreshRate(int type, int action, String pkgName, String activityName) {
        return new float[]{999.0f, 999.0f};
    }

    public float getGlobalFPS() {
        return 0.0f;
    }

    public float getMaxRefreshRate(int type, int action, String pkgName, String activityName) {
        return 0.0f;
    }

    public void setRtEnable(PrintWriter pw, boolean enable) {
    }
}
