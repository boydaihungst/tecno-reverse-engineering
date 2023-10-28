package com.android.server.policy;

import android.content.Context;
import android.os.Handler;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.policy.GlobalActionsProvider;
import com.android.server.policy.WindowManagerPolicy;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class GlobalActions implements GlobalActionsProvider.GlobalActionsListener {
    private static final boolean DEBUG = false;
    private static final String TAG = "GlobalActions";
    private final Context mContext;
    private boolean mDeviceProvisioned;
    private boolean mGlobalActionsAvailable;
    private final GlobalActionsProvider mGlobalActionsProvider;
    private boolean mKeyguardShowing;
    private LegacyGlobalActions mLegacyGlobalActions;
    private boolean mShowing;
    private final WindowManagerPolicy.WindowManagerFuncs mWindowManagerFuncs;
    private final Runnable mShowTimeout = new Runnable() { // from class: com.android.server.policy.GlobalActions.1
        @Override // java.lang.Runnable
        public void run() {
            GlobalActions.this.ensureLegacyCreated();
            GlobalActions.this.mLegacyGlobalActions.showDialog(GlobalActions.this.mKeyguardShowing, GlobalActions.this.mDeviceProvisioned);
        }
    };
    private final Handler mHandler = new Handler();

    public GlobalActions(Context context, WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs) {
        this.mContext = context;
        this.mWindowManagerFuncs = windowManagerFuncs;
        GlobalActionsProvider globalActionsProvider = (GlobalActionsProvider) LocalServices.getService(GlobalActionsProvider.class);
        this.mGlobalActionsProvider = globalActionsProvider;
        if (globalActionsProvider != null) {
            globalActionsProvider.setGlobalActionsListener(this);
        } else {
            Slog.i(TAG, "No GlobalActionsProvider found, defaulting to LegacyGlobalActions");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void ensureLegacyCreated() {
        if (this.mLegacyGlobalActions != null) {
            return;
        }
        this.mLegacyGlobalActions = new LegacyGlobalActions(this.mContext, this.mWindowManagerFuncs, new Runnable() { // from class: com.android.server.policy.GlobalActions$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                GlobalActions.this.onGlobalActionsDismissed();
            }
        });
    }

    public void showDialog(boolean keyguardShowing, boolean deviceProvisioned) {
        GlobalActionsProvider globalActionsProvider = this.mGlobalActionsProvider;
        if (globalActionsProvider != null && globalActionsProvider.isGlobalActionsDisabled()) {
            return;
        }
        this.mKeyguardShowing = keyguardShowing;
        this.mDeviceProvisioned = deviceProvisioned;
        this.mShowing = true;
        if (this.mGlobalActionsAvailable) {
            this.mHandler.postDelayed(this.mShowTimeout, 5000L);
            this.mGlobalActionsProvider.showGlobalActions();
            return;
        }
        ensureLegacyCreated();
        this.mLegacyGlobalActions.showDialog(this.mKeyguardShowing, this.mDeviceProvisioned);
    }

    @Override // com.android.server.policy.GlobalActionsProvider.GlobalActionsListener
    public void onGlobalActionsShown() {
        this.mHandler.removeCallbacks(this.mShowTimeout);
    }

    @Override // com.android.server.policy.GlobalActionsProvider.GlobalActionsListener
    public void onGlobalActionsDismissed() {
        this.mShowing = false;
    }

    @Override // com.android.server.policy.GlobalActionsProvider.GlobalActionsListener
    public void onGlobalActionsAvailableChanged(boolean available) {
        this.mGlobalActionsAvailable = available;
        if (this.mShowing && !available) {
            ensureLegacyCreated();
            this.mLegacyGlobalActions.showDialog(this.mKeyguardShowing, this.mDeviceProvisioned);
        }
    }
}
