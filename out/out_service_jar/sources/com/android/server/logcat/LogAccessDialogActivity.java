package com.android.server.logcat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.broadcastradio.V2_0.Constants;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.util.Slog;
import android.view.ContextThemeWrapper;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.android.server.LocalServices;
import com.android.server.logcat.LogcatManagerService;
/* loaded from: classes.dex */
public class LogAccessDialogActivity extends Activity implements View.OnClickListener {
    private static final int DIALOG_TIME_OUT;
    private static final int MSG_DISMISS_DIALOG = 0;
    private static final String TAG = LogAccessDialogActivity.class.getSimpleName();
    private AlertDialog mAlert;
    private AlertDialog.Builder mAlertDialog;
    private String mAlertTitle;
    private View mAlertView;
    private String mPackageName;
    private int mUid;
    private final LogcatManagerService.LogcatManagerServiceInternal mLogcatManagerInternal = (LogcatManagerService.LogcatManagerServiceInternal) LocalServices.getService(LogcatManagerService.LogcatManagerServiceInternal.class);
    private Handler mHandler = new Handler() { // from class: com.android.server.logcat.LogAccessDialogActivity.1
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    if (LogAccessDialogActivity.this.mAlert != null) {
                        LogAccessDialogActivity.this.mAlert.dismiss();
                        LogAccessDialogActivity.this.mAlert = null;
                        LogAccessDialogActivity.this.declineLogAccess();
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    };

    static {
        DIALOG_TIME_OUT = Build.IS_DEBUGGABLE ? 60000 : Constants.LIST_COMPLETE_TIMEOUT_MS;
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!readIntentInfo(getIntent())) {
            Slog.e(TAG, "Invalid Intent extras, finishing");
            finish();
            return;
        }
        try {
            this.mAlertTitle = getTitleString(this, this.mPackageName, this.mUid);
            boolean isDarkTheme = (getResources().getConfiguration().uiMode & 48) == 32;
            int themeId = isDarkTheme ? 16974545 : 16974546;
            this.mAlertView = createView(themeId);
            AlertDialog.Builder builder = new AlertDialog.Builder(this, themeId);
            this.mAlertDialog = builder;
            builder.setView(this.mAlertView);
            this.mAlertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() { // from class: com.android.server.logcat.LogAccessDialogActivity$$ExternalSyntheticLambda0
                @Override // android.content.DialogInterface.OnCancelListener
                public final void onCancel(DialogInterface dialogInterface) {
                    LogAccessDialogActivity.this.m4609x5bc0704(dialogInterface);
                }
            });
            this.mAlertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() { // from class: com.android.server.logcat.LogAccessDialogActivity$$ExternalSyntheticLambda1
                @Override // android.content.DialogInterface.OnDismissListener
                public final void onDismiss(DialogInterface dialogInterface) {
                    LogAccessDialogActivity.this.m4610x494724c5(dialogInterface);
                }
            });
            AlertDialog create = this.mAlertDialog.create();
            this.mAlert = create;
            create.show();
            this.mHandler.sendEmptyMessageDelayed(0, DIALOG_TIME_OUT);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "Unable to fetch label of package " + this.mPackageName, e);
            declineLogAccess();
            finish();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onCreate$0$com-android-server-logcat-LogAccessDialogActivity  reason: not valid java name */
    public /* synthetic */ void m4609x5bc0704(DialogInterface dialog) {
        declineLogAccess();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onCreate$1$com-android-server-logcat-LogAccessDialogActivity  reason: not valid java name */
    public /* synthetic */ void m4610x494724c5(DialogInterface dialog) {
        finish();
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        AlertDialog alertDialog;
        super.onDestroy();
        if (!isChangingConfigurations() && (alertDialog = this.mAlert) != null && alertDialog.isShowing()) {
            this.mAlert.dismiss();
        }
        this.mAlert = null;
    }

    private boolean readIntentInfo(Intent intent) {
        if (intent == null) {
            Slog.e(TAG, "Intent is null");
            return false;
        }
        String stringExtra = intent.getStringExtra("android.intent.extra.PACKAGE_NAME");
        this.mPackageName = stringExtra;
        if (stringExtra == null || stringExtra.length() == 0) {
            Slog.e(TAG, "Missing package name extra");
            return false;
        } else if (!intent.hasExtra("android.intent.extra.UID")) {
            Slog.e(TAG, "Missing EXTRA_UID");
            return false;
        } else {
            this.mUid = intent.getIntExtra("android.intent.extra.UID", 0);
            return true;
        }
    }

    private String getTitleString(Context context, String callingPackage, int uid) throws PackageManager.NameNotFoundException {
        PackageManager pm = context.getPackageManager();
        CharSequence appLabel = pm.getApplicationInfoAsUser(callingPackage, 268435456, UserHandle.getUserId(uid)).loadLabel(pm);
        String titleString = context.getString(17040661, appLabel);
        return titleString;
    }

    private View createView(int themeId) {
        Context themedContext = new ContextThemeWrapper(getApplicationContext(), themeId);
        View view = LayoutInflater.from(themedContext).inflate(17367203, (ViewGroup) null);
        if (view == null) {
            throw new InflateException();
        }
        ((TextView) view.findViewById(16909194)).setText(this.mAlertTitle);
        Button button_allow = (Button) view.findViewById(16909191);
        button_allow.setOnClickListener(this);
        Button button_deny = (Button) view.findViewById(16909193);
        button_deny.setOnClickListener(this);
        return view;
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        switch (view.getId()) {
            case 16909191:
                this.mLogcatManagerInternal.approveAccessForClient(this.mUid, this.mPackageName);
                finish();
                return;
            case 16909192:
            default:
                return;
            case 16909193:
                declineLogAccess();
                finish();
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void declineLogAccess() {
        this.mLogcatManagerInternal.declineAccessForClient(this.mUid, this.mPackageName);
    }
}
