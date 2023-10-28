package com.android.server;

import android.content.Context;
import com.transsion.server.TranSystemServiceFactory;
import com.transsion.server.tranNV.TranNvUtilsExt;
/* loaded from: classes.dex */
public class NVUtils {
    private static final String TAG = "NVUtils";
    private static TranNvUtilsExt mTranNvUtilsExt;

    public NVUtils() {
        mTranNvUtilsExt = TranSystemServiceFactory.getInstance().makeNVUtils();
    }

    public int rlk_writeDate(int flag, int value) {
        return mTranNvUtilsExt.rlk_writeDate(flag, value);
    }

    public int rlk_readData(int flag) {
        return mTranNvUtilsExt.rlk_readData(flag);
    }

    public static int getDemoPhoneNV() {
        return mTranNvUtilsExt.getDemoPhoneNV();
    }

    public String readNVColor() {
        return mTranNvUtilsExt.readNVColor();
    }

    public static boolean isOOBE(Context context) {
        return mTranNvUtilsExt.isOOBE(context);
    }
}
