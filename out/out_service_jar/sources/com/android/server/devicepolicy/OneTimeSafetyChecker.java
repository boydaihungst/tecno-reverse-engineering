package com.android.server.devicepolicy;

import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyManagerLiteInternal;
import android.app.admin.DevicePolicySafetyChecker;
import android.os.Handler;
import android.os.Looper;
import android.util.Slog;
import com.android.internal.os.IResultReceiver;
import com.android.server.LocalServices;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class OneTimeSafetyChecker implements DevicePolicySafetyChecker {
    private static final long SELF_DESTRUCT_TIMEOUT_MS = 10000;
    private static final String TAG = OneTimeSafetyChecker.class.getSimpleName();
    private boolean mDone;
    private final Handler mHandler;
    private final int mOperation;
    private final DevicePolicySafetyChecker mRealSafetyChecker;
    private final int mReason;
    private final DevicePolicyManagerService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public OneTimeSafetyChecker(DevicePolicyManagerService service, int operation, int reason) {
        Handler handler = new Handler(Looper.getMainLooper());
        this.mHandler = handler;
        this.mService = (DevicePolicyManagerService) Objects.requireNonNull(service);
        this.mOperation = operation;
        this.mReason = reason;
        DevicePolicySafetyChecker devicePolicySafetyChecker = service.getDevicePolicySafetyChecker();
        this.mRealSafetyChecker = devicePolicySafetyChecker;
        Slog.i(TAG, "OneTimeSafetyChecker constructor: operation=" + DevicePolicyManager.operationToString(operation) + ", reason=" + DevicePolicyManager.operationSafetyReasonToString(reason) + ", realChecker=" + devicePolicySafetyChecker + ", maxDuration=10000ms");
        handler.postDelayed(new Runnable() { // from class: com.android.server.devicepolicy.OneTimeSafetyChecker$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                OneTimeSafetyChecker.this.m3154xdd2f3d8a();
            }
        }, 10000L);
    }

    public int getUnsafeOperationReason(int operation) {
        String name = DevicePolicyManager.operationToString(operation);
        String str = TAG;
        Slog.i(str, "getUnsafeOperationReason(" + name + ")");
        int reason = -1;
        if (operation == this.mOperation) {
            reason = this.mReason;
        } else {
            Slog.wtf(str, "invalid call to isDevicePolicyOperationSafe(): asked for " + name + ", should be " + DevicePolicyManager.operationToString(this.mOperation));
        }
        String reasonName = DevicePolicyManager.operationSafetyReasonToString(reason);
        DevicePolicyManagerLiteInternal dpmi = (DevicePolicyManagerLiteInternal) LocalServices.getService(DevicePolicyManagerLiteInternal.class);
        Slog.i(str, "notifying " + reasonName + " is UNSAFE");
        dpmi.notifyUnsafeOperationStateChanged(this, reason, false);
        Slog.i(str, "notifying " + reasonName + " is SAFE");
        dpmi.notifyUnsafeOperationStateChanged(this, reason, true);
        Slog.i(str, "returning " + reasonName);
        disableSelf();
        return reason;
    }

    public boolean isSafeOperation(int reason) {
        boolean safe = this.mReason != reason;
        Slog.i(TAG, "isSafeOperation(" + DevicePolicyManager.operationSafetyReasonToString(reason) + "): " + safe);
        disableSelf();
        return safe;
    }

    public void onFactoryReset(IResultReceiver callback) {
        throw new UnsupportedOperationException();
    }

    private void disableSelf() {
        if (this.mDone) {
            Slog.w(TAG, "disableSelf(): already disabled");
            return;
        }
        Slog.i(TAG, "restoring DevicePolicySafetyChecker to " + this.mRealSafetyChecker);
        this.mService.setDevicePolicySafetyCheckerUnchecked(this.mRealSafetyChecker);
        this.mDone = true;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: selfDestruct */
    public void m3154xdd2f3d8a() {
        if (this.mDone) {
            return;
        }
        Slog.e(TAG, "Self destructing " + this + ", as it was not automatically disabled");
        disableSelf();
    }

    public String toString() {
        return "OneTimeSafetyChecker[id=" + System.identityHashCode(this) + ", reason=" + DevicePolicyManager.operationSafetyReasonToString(this.mReason) + ", operation=" + DevicePolicyManager.operationToString(this.mOperation) + ']';
    }
}
