package vendor.transsion.hardware.hyperion.hap.V1_0;

import java.util.ArrayList;
/* loaded from: classes2.dex */
public final class ImportKeyType {
    public static final int IMPORT_KEY_AES = 1;
    public static final int IMPORT_KEY_HMAC = 2;

    public static final String toString(int o) {
        if (o == 1) {
            return "IMPORT_KEY_AES";
        }
        if (o == 2) {
            return "IMPORT_KEY_HMAC";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        if ((o & 1) == 1) {
            list.add("IMPORT_KEY_AES");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("IMPORT_KEY_HMAC");
            flipped |= 2;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}
