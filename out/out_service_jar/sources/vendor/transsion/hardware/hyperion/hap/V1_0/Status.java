package vendor.transsion.hardware.hyperion.hap.V1_0;

import java.util.ArrayList;
/* loaded from: classes2.dex */
public final class Status {
    public static final int CA_ERROR = 3;
    public static final int FRAMEWORK_ERROR = 5;
    public static final int HAL_ERROR = 4;
    public static final int OK = 0;
    public static final int SDK_ERROR = 6;
    public static final int TA_ERROR = 1;

    public static final String toString(int o) {
        if (o == 0) {
            return "OK";
        }
        if (o == 1) {
            return "TA_ERROR";
        }
        if (o == 3) {
            return "CA_ERROR";
        }
        if (o == 4) {
            return "HAL_ERROR";
        }
        if (o == 5) {
            return "FRAMEWORK_ERROR";
        }
        if (o == 6) {
            return "SDK_ERROR";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        list.add("OK");
        if ((o & 1) == 1) {
            list.add("TA_ERROR");
            flipped = 0 | 1;
        }
        if ((o & 3) == 3) {
            list.add("CA_ERROR");
            flipped |= 3;
        }
        if ((o & 4) == 4) {
            list.add("HAL_ERROR");
            flipped |= 4;
        }
        if ((o & 5) == 5) {
            list.add("FRAMEWORK_ERROR");
            flipped |= 5;
        }
        if ((o & 6) == 6) {
            list.add("SDK_ERROR");
            flipped |= 6;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}
