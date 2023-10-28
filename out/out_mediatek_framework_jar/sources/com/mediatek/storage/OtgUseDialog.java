package com.mediatek.storage;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.mediatek.boostfwk.identify.scroll.ScrollIdentify;
import com.mediatek.internal.R;
/* loaded from: classes.dex */
public class OtgUseDialog extends AlertActivity implements DialogInterface.OnClickListener {
    private static final String ACTION_HEADSET_PLUG = "android.intent.action.HEADSET_PLUG";
    private static final int DIALOG_BOTTOM_HEIGHT = 56;
    private static final int DIALOG_WIDTH = 360;
    private static final String REMOVE_ACTION = "close_low_battery_otg_dialog";
    private static final String TAG = "OtgUseDialog";
    private IntentFilter filter;
    private IntentFilter mHeadSetfilter;
    private final String PERMISSION_OTG_CONTROL = "com.transsion.permission.OTG_CONTROL";
    private final BroadcastReceiver mOtgStateReceiver = new BroadcastReceiver() { // from class: com.mediatek.storage.OtgUseDialog.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(OtgUseDialog.REMOVE_ACTION)) {
                OtgUseDialog.this.finish();
            }
        }
    };
    private final BroadcastReceiver mHeadSetReceiver = new BroadcastReceiver() { // from class: com.mediatek.storage.OtgUseDialog.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(OtgUseDialog.ACTION_HEADSET_PLUG) && intent.getIntExtra("state", 0) == 1) {
                Settings.Global.putInt(OtgUseDialog.this.getContentResolver(), "otg_mode", 1);
                OtgUseDialog.this.finish();
            }
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        setTheme(101187594);
        super.onCreate(savedInstanceState);
        createDialog();
        this.filter = new IntentFilter();
        this.mHeadSetfilter = new IntentFilter();
        this.filter.addAction(REMOVE_ACTION);
        this.mHeadSetfilter.addAction(ACTION_HEADSET_PLUG);
        registerReceiver(this.mOtgStateReceiver, this.filter, "com.transsion.permission.OTG_CONTROL", null);
        registerReceiver(this.mHeadSetReceiver, this.mHeadSetfilter);
    }

    private void createDialog() {
        Log.d(TAG, "createDialog()");
        AlertController.AlertParams p = this.mAlertParams;
        p.mTitle = getString(R.string.otg_open_tips);
        p.mMessage = getString(R.string.otg_open_switch);
        p.mPositiveButtonText = getString(R.string.otg_open_setting);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(R.string.otg_open_cancel);
        p.mNegativeButtonListener = this;
        setupAlert();
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.width = dp2px(DIALOG_WIDTH);
        lp.gravity = 80;
        boolean isPortrait = getResources().getConfiguration().orientation == 1;
        if (isPortrait) {
            lp.y = dp2px(DIALOG_BOTTOM_HEIGHT) - getResources().getDimensionPixelSize(17105362);
        } else {
            lp.y = dp2px(DIALOG_BOTTOM_HEIGHT);
        }
        getWindow().setAttributes(lp);
    }

    private int dp2px(int dpValues) {
        return (int) TypedValue.applyDimension(1, dpValues, getResources().getDisplayMetrics());
    }

    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        View divider = findViewById(16909618);
        if (divider != null) {
            divider.setVisibility(4);
        }
        View divider2 = findViewById(16908840);
        if ((divider2 instanceof LinearLayout) && divider2 != null) {
            ((LinearLayout) divider2).setDividerDrawable(null);
        }
    }

    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        BroadcastReceiver broadcastReceiver = this.mOtgStateReceiver;
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }
        BroadcastReceiver broadcastReceiver2 = this.mHeadSetReceiver;
        if (broadcastReceiver2 != null) {
            unregisterReceiver(broadcastReceiver2);
        }
        super.onDestroy();
    }

    private void onOK() {
        try {
            Log.d(TAG, "onOK()");
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", "com.android.settings.Settings$ConnectedDeviceDashboardActivity");
            intent.setFlags(872415232);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
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
