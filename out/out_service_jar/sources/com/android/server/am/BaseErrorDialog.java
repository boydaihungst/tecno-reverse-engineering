package com.android.server.am;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Slog;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Button;
/* loaded from: classes.dex */
public class BaseErrorDialog extends AlertDialog {
    private static final int DISABLE_BUTTONS = 1;
    private static final int ENABLE_BUTTONS = 0;
    private boolean mConsuming;
    private Handler mHandler;
    private BroadcastReceiver mReceiver;

    public BaseErrorDialog(Context context) {
        super(context, 101187595);
        this.mConsuming = true;
        this.mHandler = new Handler() { // from class: com.android.server.am.BaseErrorDialog.2
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                if (msg.what == 0) {
                    BaseErrorDialog.this.mConsuming = false;
                    BaseErrorDialog.this.setEnabled(true);
                } else if (msg.what == 1) {
                    BaseErrorDialog.this.setEnabled(false);
                }
            }
        };
        context.assertRuntimeOverlayThemable();
        getWindow().setType(2003);
        getWindow().setFlags(131072, 131072);
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.setTitle("Error Dialog");
        getWindow().setAttributes(attrs);
    }

    @Override // android.app.Dialog
    public void onStart() {
        super.onStart();
        this.mHandler.sendEmptyMessage(1);
        Handler handler = this.mHandler;
        handler.sendMessageDelayed(handler.obtainMessage(0), 1000L);
        if (this.mReceiver == null) {
            this.mReceiver = new BroadcastReceiver() { // from class: com.android.server.am.BaseErrorDialog.1
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context, Intent intent) {
                    if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                        BaseErrorDialog.this.closeDialog();
                    }
                }
            };
            getContext().registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"), 2);
        }
    }

    @Override // android.app.Dialog
    protected void onStop() {
        super.onStop();
        if (this.mReceiver != null) {
            try {
                getContext().unregisterReceiver(this.mReceiver);
            } catch (IllegalArgumentException e) {
                Slog.e("BaseErrorDialog", "unregisterReceiver threw exception: " + e.getMessage());
            }
            this.mReceiver = null;
        }
    }

    @Override // android.app.Dialog, android.view.Window.Callback
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (this.mConsuming) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setEnabled(boolean enabled) {
        Button b = (Button) findViewById(16908313);
        if (b != null) {
            b.setEnabled(enabled);
        }
        Button b2 = (Button) findViewById(16908314);
        if (b2 != null) {
            b2.setEnabled(enabled);
        }
        Button b3 = (Button) findViewById(16908315);
        if (b3 != null) {
            b3.setEnabled(enabled);
        }
    }

    protected void closeDialog() {
        if (this.mCancelable) {
            cancel();
        } else {
            dismiss();
        }
    }
}
