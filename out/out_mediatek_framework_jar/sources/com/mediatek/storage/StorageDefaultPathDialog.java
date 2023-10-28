package com.mediatek.storage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.mediatek.boostfwk.identify.scroll.ScrollIdentify;
/* loaded from: classes.dex */
public class StorageDefaultPathDialog extends AlertActivity implements DialogInterface.OnClickListener {
    private static final String INSERT_OTG = "insert_otg";
    private static final String SD_ACTION = "android.intent.action.MEDIA_BAD_REMOVAL";
    private static final String TAG = "StorageDefaultPathDialog";
    private BroadcastReceiver mReceiver;
    private IntentFilter mSDCardStateFilter;
    String path = null;
    private Boolean mInsertOtg = false;
    private final BroadcastReceiver mSDStateReceiver = new BroadcastReceiver() { // from class: com.mediatek.storage.StorageDefaultPathDialog.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(StorageDefaultPathDialog.SD_ACTION)) {
                StorageDefaultPathDialog.this.finish();
            }
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "StorageDefaultPathDialog onCreate()");
        IntentFilter intentFilter = new IntentFilter(SD_ACTION);
        this.mSDCardStateFilter = intentFilter;
        intentFilter.addDataScheme("file");
        this.mReceiver = this.mSDStateReceiver;
        this.mInsertOtg = Boolean.valueOf(getIntent().getBooleanExtra(INSERT_OTG, false));
        createDialog();
    }

    private void createDialog() {
        AlertController.AlertParams p = this.mAlertParams;
        p.mTitle = this.mInsertOtg.booleanValue() ? getString(134545539) : getString(134545527);
        p.mView = createView();
        p.mViewSpacingSpecified = true;
        p.mViewSpacingLeft = 15;
        p.mViewSpacingRight = 15;
        p.mViewSpacingTop = 5;
        p.mViewSpacingBottom = 5;
        p.mPositiveButtonText = getString(17039379);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(17039369);
        p.mNegativeButtonListener = this;
        setupAlert();
    }

    /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: com.mediatek.storage.StorageDefaultPathDialog */
    /* JADX WARN: Multi-variable type inference failed */
    private View createView() {
        TextView messageView = new TextView(this);
        messageView.setTextAppearance(messageView.getContext(), 16973892);
        messageView.setText(134545528);
        return messageView;
    }

    protected void onResume() {
        super.onResume();
        registerReceiver(this.mReceiver, this.mSDCardStateFilter);
    }

    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
    }

    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause entry");
        unregisterReceiver(this.mReceiver);
    }

    private void onOK() {
        Intent intent = new Intent();
        intent.setAction("android.settings.INTERNAL_STORAGE_SETTINGS");
        intent.setFlags(1409286144);
        Log.d(TAG, "onOK() start activity");
        startActivity(intent);
        finish();
    }

    private void onCancel() {
        finish();
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case -2:
                onCancel();
                return;
            case ScrollIdentify.NO_CHECKED_STATUS /* -1 */:
                onOK();
                return;
            default:
                return;
        }
    }
}
