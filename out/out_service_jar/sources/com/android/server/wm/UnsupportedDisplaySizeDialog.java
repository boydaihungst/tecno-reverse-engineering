package com.android.server.wm;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
/* loaded from: classes2.dex */
public class UnsupportedDisplaySizeDialog {
    private final AlertDialog mDialog;
    private final String mPackageName;

    public UnsupportedDisplaySizeDialog(final AppWarnings manager, Context context, ApplicationInfo appInfo) {
        this.mPackageName = appInfo.packageName;
        PackageManager pm = context.getPackageManager();
        CharSequence label = appInfo.loadSafeLabel(pm, 1000.0f, 5);
        CharSequence message = context.getString(17041659, label);
        AlertDialog create = new AlertDialog.Builder(context).setPositiveButton(17039370, (DialogInterface.OnClickListener) null).setMessage(message).setView(17367370).create();
        this.mDialog = create;
        create.create();
        Window window = create.getWindow();
        window.setType(2002);
        window.getAttributes().setTitle("UnsupportedDisplaySizeDialog");
        CheckBox alwaysShow = (CheckBox) create.findViewById(16908788);
        alwaysShow.setChecked(true);
        alwaysShow.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: com.android.server.wm.UnsupportedDisplaySizeDialog$$ExternalSyntheticLambda0
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                UnsupportedDisplaySizeDialog.this.m8449lambda$new$0$comandroidserverwmUnsupportedDisplaySizeDialog(manager, compoundButton, z);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-wm-UnsupportedDisplaySizeDialog  reason: not valid java name */
    public /* synthetic */ void m8449lambda$new$0$comandroidserverwmUnsupportedDisplaySizeDialog(AppWarnings manager, CompoundButton buttonView, boolean isChecked) {
        manager.setPackageFlag(this.mPackageName, 1, !isChecked);
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
