package vendor.transsion.hardware.tne.tneengine.V1_0;

import java.util.ArrayList;
/* loaded from: classes2.dex */
public final class TNEValueType {
    public static final long TYPE_ALL = 1;

    public static final String toString(long o) {
        if (o == 1) {
            return "TYPE_ALL";
        }
        return "0x" + Long.toHexString(o);
    }

    public static final String dumpBitfield(long o) {
        ArrayList<String> list = new ArrayList<>();
        long flipped = 0;
        if ((o & 1) == 1) {
            list.add("TYPE_ALL");
            flipped = 0 | 1;
        }
        if (o != flipped) {
            list.add("0x" + Long.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}
