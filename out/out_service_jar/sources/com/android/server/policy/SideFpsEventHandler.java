package com.android.server.policy;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.biometrics.BiometricStateListener;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.hardware.fingerprint.IFingerprintAuthenticatorsRegisteredCallback;
import android.os.Handler;
import android.os.PowerManager;
import com.android.server.policy.SideFpsEventHandler;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public class SideFpsEventHandler {
    private static final int DEBOUNCE_DELAY_MILLIS = 500;
    private int mBiometricState;
    private final Context mContext;
    private Dialog mDialog;
    private final DialogInterface.OnDismissListener mDialogDismissListener;
    private final Supplier<AlertDialog.Builder> mDialogSupplier;
    private final Handler mHandler;
    private final PowerManager mPowerManager;
    private final AtomicBoolean mSideFpsEventHandlerReady;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-policy-SideFpsEventHandler  reason: not valid java name */
    public /* synthetic */ void m6003lambda$new$0$comandroidserverpolicySideFpsEventHandler(DialogInterface dialog) {
        if (this.mDialog == dialog) {
            this.mDialog = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SideFpsEventHandler(final Context context, Handler handler, PowerManager powerManager) {
        this(context, handler, powerManager, new Supplier() { // from class: com.android.server.policy.SideFpsEventHandler$$ExternalSyntheticLambda0
            @Override // java.util.function.Supplier
            public final Object get() {
                return SideFpsEventHandler.lambda$new$1(context);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ AlertDialog.Builder lambda$new$1(Context context) {
        return new AlertDialog.Builder(context);
    }

    SideFpsEventHandler(Context context, Handler handler, PowerManager powerManager, Supplier<AlertDialog.Builder> dialogSupplier) {
        this.mDialogDismissListener = new DialogInterface.OnDismissListener() { // from class: com.android.server.policy.SideFpsEventHandler$$ExternalSyntheticLambda1
            @Override // android.content.DialogInterface.OnDismissListener
            public final void onDismiss(DialogInterface dialogInterface) {
                SideFpsEventHandler.this.m6003lambda$new$0$comandroidserverpolicySideFpsEventHandler(dialogInterface);
            }
        };
        this.mContext = context;
        this.mHandler = handler;
        this.mPowerManager = powerManager;
        this.mDialogSupplier = dialogSupplier;
        this.mBiometricState = 0;
        this.mSideFpsEventHandlerReady = new AtomicBoolean(false);
        context.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.policy.SideFpsEventHandler.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if (SideFpsEventHandler.this.mDialog != null) {
                    SideFpsEventHandler.this.mDialog.dismiss();
                    SideFpsEventHandler.this.mDialog = null;
                }
            }
        }, new IntentFilter("android.intent.action.SCREEN_OFF"));
    }

    public boolean onSinglePressDetected(final long eventTime) {
        if (this.mSideFpsEventHandlerReady.get()) {
            switch (this.mBiometricState) {
                case 1:
                case 3:
                    this.mHandler.post(new Runnable() { // from class: com.android.server.policy.SideFpsEventHandler$$ExternalSyntheticLambda2
                        @Override // java.lang.Runnable
                        public final void run() {
                            SideFpsEventHandler.this.m6004x52d2da52(eventTime);
                        }
                    });
                    return true;
                case 2:
                default:
                    return false;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onSinglePressDetected$2$com-android-server-policy-SideFpsEventHandler  reason: not valid java name */
    public /* synthetic */ void m6004x52d2da52(long eventTime) {
        Dialog dialog = this.mDialog;
        if (dialog != null) {
            dialog.dismiss();
        }
        this.mDialog = showConfirmDialog(this.mDialogSupplier.get(), this.mPowerManager, eventTime, this.mBiometricState, this.mDialogDismissListener);
    }

    private static Dialog showConfirmDialog(AlertDialog.Builder dialogBuilder, final PowerManager powerManager, final long eventTime, int biometricState, DialogInterface.OnDismissListener dismissListener) {
        boolean enrolling = biometricState == 1;
        int title = enrolling ? 17040397 : 17040393;
        int message = enrolling ? 17040394 : 17040390;
        int positiveText = enrolling ? 17040396 : 17040392;
        int negativeText = enrolling ? 17040395 : 17040391;
        Dialog confirmScreenOffDialog = dialogBuilder.setTitle(title).setMessage(message).setPositiveButton(positiveText, new DialogInterface.OnClickListener() { // from class: com.android.server.policy.SideFpsEventHandler$$ExternalSyntheticLambda3
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                SideFpsEventHandler.lambda$showConfirmDialog$3(powerManager, eventTime, dialogInterface, i);
            }
        }).setNegativeButton(negativeText, new DialogInterface.OnClickListener() { // from class: com.android.server.policy.SideFpsEventHandler$$ExternalSyntheticLambda4
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        }).setOnDismissListener(dismissListener).setCancelable(false).create();
        confirmScreenOffDialog.getWindow().setType(2017);
        confirmScreenOffDialog.show();
        return confirmScreenOffDialog;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$showConfirmDialog$3(PowerManager powerManager, long eventTime, DialogInterface dialog, int which) {
        dialog.dismiss();
        powerManager.goToSleep(eventTime, 4, 0);
    }

    public void onFingerprintSensorReady() {
        PackageManager pm = this.mContext.getPackageManager();
        if (!pm.hasSystemFeature("android.hardware.fingerprint")) {
            return;
        }
        final FingerprintManager fingerprintManager = (FingerprintManager) this.mContext.getSystemService(FingerprintManager.class);
        fingerprintManager.addAuthenticatorsRegisteredCallback(new IFingerprintAuthenticatorsRegisteredCallback.Stub() { // from class: com.android.server.policy.SideFpsEventHandler.2
            public void onAllAuthenticatorsRegistered(List<FingerprintSensorPropertiesInternal> sensors) {
                if (fingerprintManager.isPowerbuttonFps()) {
                    fingerprintManager.registerBiometricStateListener(new AnonymousClass1());
                    SideFpsEventHandler.this.mSideFpsEventHandlerReady.set(true);
                }
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            /* renamed from: com.android.server.policy.SideFpsEventHandler$2$1  reason: invalid class name */
            /* loaded from: classes2.dex */
            public class AnonymousClass1 extends BiometricStateListener {
                private Runnable mStateRunnable = null;

                AnonymousClass1() {
                }

                public void onStateChanged(final int newState) {
                    if (this.mStateRunnable != null) {
                        SideFpsEventHandler.this.mHandler.removeCallbacks(this.mStateRunnable);
                        this.mStateRunnable = null;
                    }
                    if (newState == 0) {
                        this.mStateRunnable = new Runnable() { // from class: com.android.server.policy.SideFpsEventHandler$2$1$$ExternalSyntheticLambda0
                            @Override // java.lang.Runnable
                            public final void run() {
                                SideFpsEventHandler.AnonymousClass2.AnonymousClass1.this.m6005x2fcb3c49(newState);
                            }
                        };
                        SideFpsEventHandler.this.mHandler.postDelayed(this.mStateRunnable, 500L);
                        return;
                    }
                    SideFpsEventHandler.this.mBiometricState = newState;
                }

                /* JADX INFO: Access modifiers changed from: package-private */
                /* renamed from: lambda$onStateChanged$0$com-android-server-policy-SideFpsEventHandler$2$1  reason: not valid java name */
                public /* synthetic */ void m6005x2fcb3c49(int newState) {
                    SideFpsEventHandler.this.mBiometricState = newState;
                }
            }
        });
    }
}
