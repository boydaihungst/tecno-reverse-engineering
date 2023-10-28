package com.android.server.power;

import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.android.internal.statusbar.IStatusBarService;
/* loaded from: classes2.dex */
public class InattentiveSleepWarningController {
    private static final String TAG = "InattentiveSleepWarning";
    private final Handler mHandler = new Handler();
    private boolean mIsShown;
    private IStatusBarService mStatusBarService;

    public boolean isShown() {
        return this.mIsShown;
    }

    public void show() {
        if (isShown()) {
            return;
        }
        this.mHandler.post(new Runnable() { // from class: com.android.server.power.InattentiveSleepWarningController$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                InattentiveSleepWarningController.this.showInternal();
            }
        });
        this.mIsShown = true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showInternal() {
        try {
            getStatusBar().showInattentiveSleepWarning();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to show inattentive sleep warning", e);
            this.mIsShown = false;
        }
    }

    public void dismiss(final boolean animated) {
        if (!isShown()) {
            return;
        }
        this.mHandler.post(new Runnable() { // from class: com.android.server.power.InattentiveSleepWarningController$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                InattentiveSleepWarningController.this.m6038x1353d50d(animated);
            }
        });
        this.mIsShown = false;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: dismissInternal */
    public void m6038x1353d50d(boolean animated) {
        try {
            getStatusBar().dismissInattentiveSleepWarning(animated);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to dismiss inattentive sleep warning", e);
        }
    }

    private IStatusBarService getStatusBar() {
        if (this.mStatusBarService == null) {
            this.mStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
        }
        return this.mStatusBarService;
    }
}
