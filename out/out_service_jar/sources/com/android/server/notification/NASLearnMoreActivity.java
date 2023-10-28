package com.android.server.notification;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
/* loaded from: classes2.dex */
public class NASLearnMoreActivity extends Activity {
    @Override // android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showLearnMoreDialog();
    }

    private void showLearnMoreDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog alertDialog = builder.setMessage(17040843).setPositiveButton(17039370, new DialogInterface.OnClickListener() { // from class: com.android.server.notification.NASLearnMoreActivity.1
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog, int which) {
                NASLearnMoreActivity.this.finish();
            }
        }).create();
        alertDialog.getWindow().setType(2003);
        alertDialog.show();
    }
}
