package com.android.internal.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyResources;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import com.android.internal.R;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public class UnlaunchableAppActivity extends Activity implements DialogInterface.OnDismissListener, DialogInterface.OnClickListener {
    private static final String EXTRA_UNLAUNCHABLE_REASON = "unlaunchable_reason";
    private static final String TAG = "UnlaunchableAppActivity";
    private static final int UNLAUNCHABLE_REASON_QUIET_MODE = 1;
    private int mReason;
    private IntentSender mTarget;
    private int mUserId;

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.app.Activity
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(1);
        Intent intent = getIntent();
        this.mReason = intent.getIntExtra(EXTRA_UNLAUNCHABLE_REASON, -1);
        this.mUserId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, -10000);
        this.mTarget = (IntentSender) intent.getParcelableExtra(Intent.EXTRA_INTENT);
        if (this.mUserId != -10000) {
            if (this.mReason == 1) {
                String dialogTitle = getDialogTitle();
                String dialogMessage = getDialogMessage();
                AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(dialogTitle).setMessage(dialogMessage).setOnDismissListener(this);
                if (this.mReason == 1) {
                    builder.setPositiveButton(R.string.work_mode_turn_on, this).setNegativeButton(17039360, (DialogInterface.OnClickListener) null);
                } else {
                    builder.setPositiveButton(17039370, (DialogInterface.OnClickListener) null);
                }
                AlertDialog dialog = builder.create();
                dialog.create();
                getWindow().setHideOverlayWindows(true);
                dialog.getButton(-1).setFilterTouchesWhenObscured(true);
                dialog.show();
                return;
            }
            Log.wtf(TAG, "Invalid unlaunchable type: " + this.mReason);
            finish();
            return;
        }
        Log.wtf(TAG, "Invalid user id: " + this.mUserId + ". Stopping.");
        finish();
    }

    private String getDialogTitle() {
        return ((DevicePolicyManager) getSystemService(DevicePolicyManager.class)).getResources().getString(DevicePolicyResources.Strings.Core.UNLAUNCHABLE_APP_WORK_PAUSED_TITLE, new Supplier() { // from class: com.android.internal.app.UnlaunchableAppActivity$$ExternalSyntheticLambda0
            @Override // java.util.function.Supplier
            public final Object get() {
                return UnlaunchableAppActivity.this.m6509x20990899();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getDialogTitle$0$com-android-internal-app-UnlaunchableAppActivity  reason: not valid java name */
    public /* synthetic */ String m6509x20990899() {
        return getString(R.string.work_mode_off_title);
    }

    private String getDialogMessage() {
        return ((DevicePolicyManager) getSystemService(DevicePolicyManager.class)).getResources().getString(DevicePolicyResources.Strings.Core.UNLAUNCHABLE_APP_WORK_PAUSED_MESSAGE, new Supplier() { // from class: com.android.internal.app.UnlaunchableAppActivity$$ExternalSyntheticLambda1
            @Override // java.util.function.Supplier
            public final Object get() {
                return UnlaunchableAppActivity.this.m6508x54fe0c69();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getDialogMessage$1$com-android-internal-app-UnlaunchableAppActivity  reason: not valid java name */
    public /* synthetic */ String m6508x54fe0c69() {
        return getString(R.string.work_mode_off_message);
    }

    @Override // android.content.DialogInterface.OnDismissListener
    public void onDismiss(DialogInterface dialog) {
        finish();
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialog, int which) {
        if (this.mReason == 1 && which == -1) {
            final UserManager userManager = UserManager.get(this);
            new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: com.android.internal.app.UnlaunchableAppActivity$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    UnlaunchableAppActivity.this.m6510x67941ee2(userManager);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$2$com-android-internal-app-UnlaunchableAppActivity  reason: not valid java name */
    public /* synthetic */ void m6510x67941ee2(UserManager userManager) {
        userManager.requestQuietModeEnabled(false, UserHandle.of(this.mUserId), this.mTarget);
    }

    private static final Intent createBaseIntent() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("android", UnlaunchableAppActivity.class.getName()));
        intent.setFlags(276824064);
        return intent;
    }

    public static Intent createInQuietModeDialogIntent(int userId) {
        Intent intent = createBaseIntent();
        intent.putExtra(EXTRA_UNLAUNCHABLE_REASON, 1);
        intent.putExtra(Intent.EXTRA_USER_HANDLE, userId);
        return intent;
    }

    public static Intent createInQuietModeDialogIntent(int userId, IntentSender target) {
        Intent intent = createInQuietModeDialogIntent(userId);
        intent.putExtra(Intent.EXTRA_INTENT, target);
        return intent;
    }
}
