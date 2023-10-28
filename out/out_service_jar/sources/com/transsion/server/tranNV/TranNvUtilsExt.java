package com.transsion.server.tranNV;

import android.content.Context;
/* loaded from: classes2.dex */
public class TranNvUtilsExt {
    private static volatile TranNvUtilsExt singleton;

    protected TranNvUtilsExt() {
    }

    public static TranNvUtilsExt getInstance() {
        if (singleton == null) {
            synchronized (TranNvUtilsExt.class) {
                if (singleton == null) {
                    singleton = new TranNvUtilsExt();
                }
            }
        }
        return singleton;
    }

    public int rlk_writeDate(int flag, int value) {
        return 0;
    }

    public int rlk_readData(int flag) {
        return -1;
    }

    public int getDemoPhoneNV() {
        return 0;
    }

    public boolean isOOBE(Context context) {
        return false;
    }

    public String readNVColor() {
        return "";
    }
}
