package com.android.server.media;

import android.content.Context;
import android.os.Binder;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
class MediaServerUtils {
    MediaServerUtils() {
    }

    public static boolean checkDumpPermission(Context context, String tag, PrintWriter pw) {
        if (context.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump " + tag + " from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " due to missing android.permission.DUMP permission");
            return false;
        }
        return true;
    }
}
