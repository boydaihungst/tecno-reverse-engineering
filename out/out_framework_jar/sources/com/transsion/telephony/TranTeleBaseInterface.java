package com.transsion.telephony;

import com.android.telephony.Rlog;
/* loaded from: classes4.dex */
public class TranTeleBaseInterface {
    private static final boolean DBG = true;
    public static final int INVALID_SS_LEVEL = -3;
    private static final String TAG = "TranTeleBaseInterface";
    public static final int TYPE_2G = 11;
    public static final int TYPE_3G = 12;
    public static final int TYPE_4G = 13;
    public static final int TYPE_5G = 14;

    public int getCustomSignalStrengthLevel(int dbm, int type) {
        tranLog("[getCustomSignalStrengthLevel] default execute");
        return -3;
    }

    public static void tranLog(String msg) {
        Rlog.d(TAG, msg);
    }
}
