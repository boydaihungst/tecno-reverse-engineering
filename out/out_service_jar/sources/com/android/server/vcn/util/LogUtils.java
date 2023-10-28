package com.android.server.vcn.util;

import android.os.ParcelUuid;
import com.android.internal.util.HexDump;
/* loaded from: classes2.dex */
public class LogUtils {
    public static String getHashedSubscriptionGroup(ParcelUuid uuid) {
        if (uuid == null) {
            return null;
        }
        return HexDump.toHexString(uuid.hashCode());
    }
}
