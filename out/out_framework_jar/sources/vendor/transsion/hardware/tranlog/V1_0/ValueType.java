package vendor.transsion.hardware.tranlog.V1_0;

import java.util.ArrayList;
/* loaded from: classes4.dex */
public final class ValueType {
    public static final int TYPE_BOOL = 3;
    public static final int TYPE_INT = 1;
    public static final int TYPE_LL = 5;
    public static final int TYPE_LONG = 4;
    public static final int TYPE_STRING = 2;

    public static final String toString(int o) {
        if (o == 1) {
            return "TYPE_INT";
        }
        if (o == 2) {
            return "TYPE_STRING";
        }
        if (o == 3) {
            return "TYPE_BOOL";
        }
        if (o == 4) {
            return "TYPE_LONG";
        }
        if (o == 5) {
            return "TYPE_LL";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        if ((o & 1) == 1) {
            list.add("TYPE_INT");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("TYPE_STRING");
            flipped |= 2;
        }
        if ((o & 3) == 3) {
            list.add("TYPE_BOOL");
            flipped |= 3;
        }
        if ((o & 4) == 4) {
            list.add("TYPE_LONG");
            flipped |= 4;
        }
        if ((o & 5) == 5) {
            list.add("TYPE_LL");
            flipped |= 5;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}
