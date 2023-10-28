package com.mediatek.storage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.mediatek.boostfwk.identify.scroll.ScrollIdentify;
import com.mediatek.boostfwk.utils.Config;
import com.mediatek.internal.R;
import java.io.FileOutputStream;
/* loaded from: classes.dex */
public class LowBatteryOtgIsUseDialog extends AlertActivity implements DialogInterface.OnClickListener {
    private static final int DIALOG_BOTTOM_HEIGHT = 56;
    private static final String INSERT_OTG = "insert_otg";
    private static final String OTG_CTL_PATH = "sys/devices/platform/odm/odm:tran_battery/OTG_CTL";
    private static final String REMOVE_ACTION = "close_low_battery_otg_dialog";
    private static final String TAG = "LowBatteryOtgIsUseDialog";
    private static final boolean TRAN_CHARGER_SW_AIOTG_SUPPORT = Config.USER_CONFIG_DEFAULT_TYPE.equals(SystemProperties.get("ro.vendor.tran.sw_aiotg_support"));
    private IntentFilter filter;
    private String mFlag;
    private FileOutputStream mFos;
    private BroadcastReceiver mReceiver;
    String path = null;
    private Boolean mInsertOtg = false;
    private final String ACTION_CLOSE_OTG_DIALOG = "action_close_otg_dialog";
    private final String PERMISSION_OTG_CONTROL = "com.transsion.permission.OTG_CONTROL";
    private final BroadcastReceiver mOtgStateReceiver = new BroadcastReceiver() { // from class: com.mediatek.storage.LowBatteryOtgIsUseDialog.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(LowBatteryOtgIsUseDialog.REMOVE_ACTION)) {
                LowBatteryOtgIsUseDialog.this.finish();
            }
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        setTheme(101187594);
        super.onCreate(savedInstanceState);
        Log.d("OtgObserver", "LowBatteryOtgIsUseDialog onCreate()");
        getWindow().addFlags(2097152);
        Intent intent = getIntent();
        this.mFlag = intent.getStringExtra("OtgFlag");
        IntentFilter intentFilter = new IntentFilter();
        this.filter = intentFilter;
        intentFilter.addAction(REMOVE_ACTION);
        if (TRAN_CHARGER_SW_AIOTG_SUPPORT) {
            writeFile("0", OTG_CTL_PATH);
        }
        createDialog();
    }

    private void createDialog() {
        Log.d("OtgObserver", "LowBatteryOtgIsUseDialog createDialog()");
        AlertController.AlertParams p = this.mAlertParams;
        p.mTitle = getString(R.string.otg_disable_prompt);
        if (TRAN_CHARGER_SW_AIOTG_SUPPORT) {
            p.mMessage = getString(R.string.otg_onlyfile_message);
        } else {
            p.mMessage = getString(R.string.otg_disable_message);
        }
        p.mPositiveButtonText = getString(17039379);
        p.mPositiveButtonListener = this;
        setupAlert();
        Button positiveButton = this.mAlert.getButton(-1);
        if (positiveButton != null) {
            LinearLayout.LayoutParams positiveButtonLL = (LinearLayout.LayoutParams) positiveButton.getLayoutParams();
            positiveButtonLL.gravity = 17;
            positiveButtonLL.width = -1;
            positiveButton.setLayoutParams(positiveButtonLL);
        }
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.width = -1;
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

    /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: com.mediatek.storage.LowBatteryOtgIsUseDialog */
    /* JADX WARN: Multi-variable type inference failed */
    private View createView() {
        Log.d("OtgObserver", "LowBatteryOtgIsUseDialog createView()");
        TextView messageView = new TextView(this);
        messageView.setTextAppearance(messageView.getContext(), 16973892);
        messageView.setText(134545528);
        return messageView;
    }

    protected void onResume() {
        super.onResume();
        Log.d("OtgObserver", "LowBatteryOtgIsUseDialog onResume()");
        View divider = findViewById(16909618);
        if (divider != null) {
            divider.setVisibility(4);
        }
        View divider2 = findViewById(16908840);
        if ((divider2 instanceof LinearLayout) && divider2 != null) {
            ((LinearLayout) divider2).setDividerDrawable(null);
        }
        registerReceiver(this.mOtgStateReceiver, this.filter, "com.transsion.permission.OTG_CONTROL", null);
    }

    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        unregisterReceiver(this.mOtgStateReceiver);
    }

    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause entry");
    }

    private void onOK() {
        Log.d("OtgObserver", "LowBatteryOtgIsUseDialog onOK()");
        finish();
    }

    /*  JADX ERROR: JadxRuntimeException in pass: RegionMakerVisitor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find top splitter block for handler:B:21:0x0037
        	at jadx.core.utils.BlockUtils.getTopSplitterForHandler(BlockUtils.java:1234)
        	at jadx.core.dex.visitors.regions.RegionMaker.processTryCatchBlocks(RegionMaker.java:1018)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:55)
        */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [242=4, 243=4] */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:22:0x0038 -> B:23:0x003c). Please submit an issue!!! */
    public void writeFile(java.lang.String r4, java.lang.String r5) {
        /*
            r3 = this;
            java.io.FileOutputStream r0 = new java.io.FileOutputStream     // Catch: java.lang.Throwable -> L16 java.io.IOException -> L18 java.io.FileNotFoundException -> L2b
            r0.<init>(r5)     // Catch: java.lang.Throwable -> L16 java.io.IOException -> L18 java.io.FileNotFoundException -> L2b
            r3.mFos = r0     // Catch: java.lang.Throwable -> L16 java.io.IOException -> L18 java.io.FileNotFoundException -> L2b
            byte[] r1 = r4.getBytes()     // Catch: java.lang.Throwable -> L16 java.io.IOException -> L18 java.io.FileNotFoundException -> L2b
            r0.write(r1)     // Catch: java.lang.Throwable -> L16 java.io.IOException -> L18 java.io.FileNotFoundException -> L2b
            java.io.FileOutputStream r0 = r3.mFos     // Catch: java.io.IOException -> L37
            if (r0 == 0) goto L36
            r0.close()     // Catch: java.io.IOException -> L37
            goto L36
        L16:
            r0 = move-exception
            goto L3d
        L18:
            r0 = move-exception
            r0.printStackTrace()     // Catch: java.lang.Throwable -> L16
            java.lang.String r1 = "LowBatteryOtgIsUseDialog"
            java.lang.String r2 = "write fail"
            android.util.Log.d(r1, r2)     // Catch: java.lang.Throwable -> L16
            java.io.FileOutputStream r0 = r3.mFos     // Catch: java.io.IOException -> L37
            if (r0 == 0) goto L36
            r0.close()     // Catch: java.io.IOException -> L37
            goto L36
        L2b:
            r0 = move-exception
            r0.printStackTrace()     // Catch: java.lang.Throwable -> L16
            java.io.FileOutputStream r0 = r3.mFos     // Catch: java.io.IOException -> L37
            if (r0 == 0) goto L36
            r0.close()     // Catch: java.io.IOException -> L37
        L36:
            goto L3c
        L37:
            r0 = move-exception
            r0.printStackTrace()
        L3c:
            return
        L3d:
            java.io.FileOutputStream r1 = r3.mFos     // Catch: java.io.IOException -> L45
            if (r1 == 0) goto L44
            r1.close()     // Catch: java.io.IOException -> L45
        L44:
            goto L49
        L45:
            r1 = move-exception
            r1.printStackTrace()
        L49:
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.mediatek.storage.LowBatteryOtgIsUseDialog.writeFile(java.lang.String, java.lang.String):void");
    }

    private void tellServiceClosed() {
        Intent mBroadcastIntent = new Intent("action_close_otg_dialog");
        sendBroadcast(mBroadcastIntent, "com.transsion.permission.OTG_CONTROL");
    }

    private void onCancel() {
        finish();
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case ScrollIdentify.NO_CHECKED_STATUS /* -1 */:
                onOK();
                return;
            default:
                return;
        }
    }
}
