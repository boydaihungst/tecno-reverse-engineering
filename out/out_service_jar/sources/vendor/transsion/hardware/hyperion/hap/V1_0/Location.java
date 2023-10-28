package vendor.transsion.hardware.hyperion.hap.V1_0;

import java.util.ArrayList;
/* loaded from: classes2.dex */
public final class Location {
    public static final int PERSIST = 2;
    public static final int SECURE = 1;

    public static final String toString(int o) {
        if (o == 1) {
            return "SECURE";
        }
        if (o == 2) {
            return "PERSIST";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        if ((o & 1) == 1) {
            list.add("SECURE");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("PERSIST");
            flipped |= 2;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}
