package com.transsion.hubcore.server.wm.tranrefreshrate;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import java.io.FileReader;
/* loaded from: classes2.dex */
public class BinderUtils {
    private static final int SURFACE_FLINGER_UPDATE_SCREEN_DIM_STATUS_CODE = 1081;
    private static final String TAG = BinderUtils.class.getSimpleName();
    private static final String[] mAPTempName = {"mtktsAP", "ap_ntc"};
    public static String mMainboardTemperaturePath = null;
    private static IBinder mSurfaceFlinger;

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IF]}, finally: {[IF, INVOKE] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [43=4] */
    public static void setScreenDimToSF() {
        Parcel data = null;
        try {
            try {
                if (mSurfaceFlinger == null) {
                    mSurfaceFlinger = ServiceManager.getService("SurfaceFlinger");
                }
                if (mSurfaceFlinger != null) {
                    data = Parcel.obtain();
                    data.writeInterfaceToken("android.ui.ISurfaceComposer");
                    mSurfaceFlinger.transact(SURFACE_FLINGER_UPDATE_SCREEN_DIM_STATUS_CODE, data, null, 0);
                } else {
                    TranBaseRefreshRatePolicy.logd(TAG, "mSurfaceFlinger is null");
                }
                if (data == null) {
                    return;
                }
            } catch (RemoteException e) {
                TranBaseRefreshRatePolicy.logd(TAG, "setScreenBrightToSF." + e.toString());
                if (0 == 0) {
                    return;
                }
            }
            data.recycle();
        } catch (Throwable th) {
            if (0 != 0) {
                data.recycle();
            }
            throw th;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [72=4, 74=4] */
    /* JADX WARN: Code restructure failed: missing block: B:11:0x0045, code lost:
        com.transsion.hubcore.server.wm.tranrefreshrate.BinderUtils.mMainboardTemperaturePath = "/sys/class/thermal/thermal_zone" + r1 + "/temp";
        com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy.logd(com.transsion.hubcore.server.wm.tranrefreshrate.BinderUtils.TAG, "mMainboardTemperaturePath is: " + com.transsion.hubcore.server.wm.tranrefreshrate.BinderUtils.mMainboardTemperaturePath);
        r2.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:12:0x007d, code lost:
        r0 = null;
     */
    /* JADX WARN: Code restructure failed: missing block: B:13:0x007e, code lost:
        if (0 == 0) goto L26;
     */
    /* JADX WARN: Code restructure failed: missing block: B:14:0x0080, code lost:
        r0.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:16:0x0084, code lost:
        r2 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:17:0x0085, code lost:
        com.transsion.hubcore.server.wm.tranrefreshrate.TranBaseRefreshRatePolicy.logd(com.transsion.hubcore.server.wm.tranrefreshrate.BinderUtils.TAG, r2.getMessage());
     */
    /* JADX WARN: Code restructure failed: missing block: B:18:0x008e, code lost:
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:21:0x0093, code lost:
        r2.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:55:?, code lost:
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:56:?, code lost:
        return;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private static void queryMainboardTemperaturePath() {
        int i = 0;
        while (i < 31) {
            FileReader file = null;
            try {
                try {
                    char[] buffer = new char[1024];
                    String tempPath = "/sys/class/thermal/thermal_zone" + i + "/type";
                    file = new FileReader(tempPath);
                    int len = file.read(buffer, 0, 1024);
                    String type = new String(buffer, 0, len).trim();
                    int j = 0;
                    while (true) {
                        String[] strArr = mAPTempName;
                        if (j >= strArr.length) {
                            try {
                                break;
                            } catch (Exception e) {
                                TranBaseRefreshRatePolicy.logd(TAG, e.getMessage());
                            }
                        } else if (strArr[j].equals(type)) {
                            break;
                        } else {
                            j++;
                        }
                        TranBaseRefreshRatePolicy.logd(TAG, e.getMessage());
                    }
                } catch (Exception e2) {
                    TranBaseRefreshRatePolicy.logd(TAG, e2.getMessage());
                    if (file != null) {
                        file.close();
                    }
                }
                i++;
            } catch (Throwable th) {
                if (file != null) {
                    try {
                        file.close();
                    } catch (Exception e3) {
                        TranBaseRefreshRatePolicy.logd(TAG, e3.getMessage());
                    }
                }
                throw th;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [106=4] */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:16:0x005e -> B:34:0x007a). Please submit an issue!!! */
    public static float queryMainboardTemperature() {
        if (mMainboardTemperaturePath == null) {
            TranBaseRefreshRatePolicy.logd(TAG, "mMainboardTemperaturePath is: " + mMainboardTemperaturePath);
            queryMainboardTemperaturePath();
        }
        if (mMainboardTemperaturePath == null) {
            TranBaseRefreshRatePolicy.logd(TAG, "mMainboardTemperaturePath is null");
            return 0.0f;
        }
        float temp = -1.0f;
        FileReader file = null;
        try {
            try {
                try {
                    char[] buffer = new char[1024];
                    file = new FileReader(mMainboardTemperaturePath);
                    int len = file.read(buffer, 0, 1024);
                    temp = Float.parseFloat(new String(buffer, 0, len).trim());
                    file.close();
                    FileReader file2 = null;
                    if (0 != 0) {
                        file2.close();
                    }
                } catch (Exception e) {
                    TranBaseRefreshRatePolicy.logd(TAG, e.getMessage());
                    if (file != null) {
                        file.close();
                    }
                }
            } catch (Exception e2) {
                TranBaseRefreshRatePolicy.logd(TAG, e2.getMessage());
            }
            float temp2 = temp / 1000.0f;
            TranBaseRefreshRatePolicy.logd(TAG, "queryMainboardTemperature Temperature:" + temp2);
            return temp2;
        } catch (Throwable th) {
            if (file != null) {
                try {
                    file.close();
                } catch (Exception e3) {
                    TranBaseRefreshRatePolicy.logd(TAG, e3.getMessage());
                }
            }
            throw th;
        }
    }
}
