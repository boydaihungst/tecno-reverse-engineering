package com.android.server.wm;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.Window;
import com.android.server.utils.AppInstallerUtil;
/* loaded from: classes2.dex */
public class DeprecatedTargetSdkVersionDialog {
    private static final String TAG = "ActivityTaskManager";
    private final AlertDialog mDialog;
    private final String mPackageName;

    public DeprecatedTargetSdkVersionDialog(final AppWarnings manager, final Context context, ApplicationInfo appInfo) {
        this.mPackageName = appInfo.packageName;
        PackageManager pm = context.getPackageManager();
        CharSequence label = appInfo.loadSafeLabel(pm, 1000.0f, 5);
        CharSequence message = context.getString(17040154);
        AlertDialog.Builder builder = new AlertDialog.Builder(context).setPositiveButton(17039370, new DialogInterface.OnClickListener() { // from class: com.android.server.wm.DeprecatedTargetSdkVersionDialog$$ExternalSyntheticLambda0
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                DeprecatedTargetSdkVersionDialog.this.m7886x2177e936(manager, dialogInterface, i);
            }
        }).setMessage(message).setTitle(label);
        final Intent installerIntent = AppInstallerUtil.createIntent(context, appInfo.packageName);
        if (installerIntent != null) {
            builder.setNeutralButton(17040153, new DialogInterface.OnClickListener() { // from class: com.android.server.wm.DeprecatedTargetSdkVersionDialog$$ExternalSyntheticLambda1
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    context.startActivity(installerIntent);
                }
            });
        }
        AlertDialog create = builder.create();
        this.mDialog = create;
        create.create();
        Window window = create.getWindow();
        window.setType(2002);
        window.getAttributes().setTitle("DeprecatedTargetSdkVersionDialog");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-wm-DeprecatedTargetSdkVersionDialog  reason: not valid java name */
    public /* synthetic */ void m7886x2177e936(AppWarnings manager, DialogInterface dialog, int which) {
        manager.setPackageFlag(this.mPackageName, 4, true);
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public void show() {
        Log.w(TAG, "Showing SDK deprecation warning for package " + this.mPackageName);
        this.mDialog.show();
    }

    public void dismiss() {
        this.mDialog.dismiss();
    }
}
