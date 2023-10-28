package android.hardware.boot.V1_1;

import java.util.ArrayList;
/* loaded from: classes.dex */
public final class MergeStatus {
    public static final int CANCELLED = 4;
    public static final int MERGING = 3;
    public static final int NONE = 0;
    public static final int SNAPSHOTTED = 2;
    public static final int UNKNOWN = 1;

    public static final String toString(int o) {
        if (o == 0) {
            return "NONE";
        }
        if (o == 1) {
            return "UNKNOWN";
        }
        if (o == 2) {
            return "SNAPSHOTTED";
        }
        if (o == 3) {
            return "MERGING";
        }
        if (o == 4) {
            return "CANCELLED";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        list.add("NONE");
        if ((o & 1) == 1) {
            list.add("UNKNOWN");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("SNAPSHOTTED");
            flipped |= 2;
        }
        if ((o & 3) == 3) {
            list.add("MERGING");
            flipped |= 3;
        }
        if ((o & 4) == 4) {
            list.add("CANCELLED");
            flipped |= 4;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}
