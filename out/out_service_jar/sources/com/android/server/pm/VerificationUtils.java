package com.android.server.pm;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.Settings;
/* loaded from: classes2.dex */
final class VerificationUtils {
    private static final long DEFAULT_STREAMING_VERIFICATION_TIMEOUT = 3000;
    private static final long DEFAULT_VERIFICATION_TIMEOUT = 10000;

    VerificationUtils() {
    }

    public static long getVerificationTimeout(Context context, boolean streaming) {
        if (streaming) {
            return getDefaultStreamingVerificationTimeout(context);
        }
        return getDefaultVerificationTimeout(context);
    }

    public static long getDefaultVerificationTimeout(Context context) {
        long timeout = Settings.Global.getLong(context.getContentResolver(), "verifier_timeout", 10000L);
        return Math.max(timeout, 10000L);
    }

    public static long getDefaultStreamingVerificationTimeout(Context context) {
        long timeout = Settings.Global.getLong(context.getContentResolver(), "streaming_verifier_timeout", 3000L);
        return Math.max(timeout, 3000L);
    }

    public static void broadcastPackageVerified(int verificationId, Uri packageUri, int verificationCode, String rootHashString, int dataLoaderType, UserHandle user, Context context) {
        Intent intent = new Intent("android.intent.action.PACKAGE_VERIFIED");
        intent.setDataAndType(packageUri, "application/vnd.android.package-archive");
        intent.addFlags(1);
        intent.putExtra("android.content.pm.extra.VERIFICATION_ID", verificationId);
        intent.putExtra("android.content.pm.extra.VERIFICATION_RESULT", verificationCode);
        if (rootHashString != null) {
            intent.putExtra("android.content.pm.extra.VERIFICATION_ROOT_HASH", rootHashString);
        }
        intent.putExtra("android.content.pm.extra.DATA_LOADER_TYPE", dataLoaderType);
        context.sendBroadcastAsUser(intent, user, "android.permission.PACKAGE_VERIFICATION_AGENT");
    }
}
