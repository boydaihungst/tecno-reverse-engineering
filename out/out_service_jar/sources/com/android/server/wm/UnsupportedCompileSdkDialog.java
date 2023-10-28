package com.android.server.wm;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import com.android.server.utils.AppInstallerUtil;
/* loaded from: classes2.dex */
public class UnsupportedCompileSdkDialog {
    private final AlertDialog mDialog;
    private final String mPackageName;

    public UnsupportedCompileSdkDialog(final AppWarnings manager, final Context context, ApplicationInfo appInfo) {
        this.mPackageName = appInfo.packageName;
        PackageManager pm = context.getPackageManager();
        CharSequence label = appInfo.loadSafeLabel(pm, 1000.0f, 5);
        CharSequence message = context.getString(17041657, label);
        AlertDialog.Builder builder = new AlertDialog.Builder(context).setPositiveButton(17039370, (DialogInterface.OnClickListener) null).setMessage(message).setView(17367369);
        final Intent installerIntent = AppInstallerUtil.createIntent(context, appInfo.packageName);
        if (installerIntent != null) {
            builder.setNeutralButton(17041656, new DialogInterface.OnClickListener() { // from class: com.android.server.wm.UnsupportedCompileSdkDialog$$ExternalSyntheticLambda0
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
        window.getAttributes().setTitle("UnsupportedCompileSdkDialog");
        CheckBox alwaysShow = (CheckBox) create.findViewById(16908788);
        alwaysShow.setChecked(true);
        alwaysShow.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: com.android.server.wm.UnsupportedCompileSdkDialog$$ExternalSyntheticLambda1
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                UnsupportedCompileSdkDialog.this.m8448lambda$new$1$comandroidserverwmUnsupportedCompileSdkDialog(manager, compoundButton, z);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$1$com-android-server-wm-UnsupportedCompileSdkDialog  reason: not valid java name */
    public /* synthetic */ void m8448lambda$new$1$comandroidserverwmUnsupportedCompileSdkDialog(AppWarnings manager, CompoundButton buttonView, boolean isChecked) {
        manager.setPackageFlag(this.mPackageName, 2, !isChecked);
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public void show() {
        this.mDialog.show();
    }

    public void dismiss() {
        this.mDialog.dismiss();
    }
}
