package com.android.server.tare;

import android.util.IndentingPrintWriter;
import java.text.SimpleDateFormat;
import java.time.Clock;
/* loaded from: classes2.dex */
class TareUtils {
    private static final SimpleDateFormat sDumpDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    static Clock sSystemClock = Clock.systemUTC();

    TareUtils() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void dumpTime(IndentingPrintWriter pw, long time) {
        pw.print(sDumpDateFormat.format(Long.valueOf(time)));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static long getCurrentTimeMillis() {
        return sSystemClock.millis();
    }

    static int cakeToArc(long cakes) {
        return (int) (cakes / 1000000000);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String cakeToString(long cakes) {
        if (cakes == 0) {
            return "0 ARCs";
        }
        long sub = cakes % 1000000000;
        long arcs = cakeToArc(cakes);
        if (arcs == 0) {
            if (sub == 1) {
                return sub + " cake";
            }
            return sub + " cakes";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(arcs);
        if (sub != 0) {
            sb.append(".").append(String.format("%03d", Long.valueOf(Math.abs(sub) / 1000000)));
        }
        sb.append(" ARC");
        if (arcs != 1 || sub != 0) {
            sb.append("s");
        }
        return sb.toString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String appToString(int userId, String pkgName) {
        return "<" + userId + ">" + pkgName;
    }
}
