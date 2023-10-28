package com.android.internal.telephony;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;
/* loaded from: classes4.dex */
public class CellBroadcastUtils {
    private static final String TAG = "CellBroadcastUtils";
    private static final boolean VDBG = false;

    public static String getDefaultCellBroadcastReceiverPackageName(Context context) {
        PackageManager packageManager = context.getPackageManager();
        ResolveInfo resolveInfo = packageManager.resolveActivity(new Intent(Telephony.Sms.Intents.SMS_CB_RECEIVED_ACTION), 1048576);
        if (resolveInfo == null) {
            Log.e(TAG, "getDefaultCellBroadcastReceiverPackageName: no package found");
            return null;
        }
        String packageName = resolveInfo.activityInfo.applicationInfo.packageName;
        if (TextUtils.isEmpty(packageName) || packageManager.checkPermission(Manifest.permission.READ_CELL_BROADCASTS, packageName) == -1) {
            Log.e(TAG, "getDefaultCellBroadcastReceiverPackageName: returning null; permission check failed for : " + packageName);
            return null;
        }
        return packageName;
    }

    public static ComponentName getDefaultCellBroadcastAlertDialogComponent(Context context) {
        String cellBroadcastReceiverPackageName = getDefaultCellBroadcastReceiverPackageName(context);
        if (TextUtils.isEmpty(cellBroadcastReceiverPackageName)) {
            return null;
        }
        return ComponentName.createRelative(cellBroadcastReceiverPackageName, "com.android.cellbroadcastreceiver.CellBroadcastAlertDialog");
    }
}
