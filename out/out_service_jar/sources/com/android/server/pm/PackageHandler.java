package com.android.server.pm;

import android.content.Intent;
import android.content.pm.InstantAppRequest;
import android.content.pm.PackageManagerInternal;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.Trace;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;
import com.android.server.job.controllers.JobStatus;
import java.io.IOException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class PackageHandler extends Handler {
    private final InstallPackageHelper mInstallPackageHelper;
    private final PackageManagerService mPm;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageHandler(Looper looper, PackageManagerService pm) {
        super(looper);
        this.mPm = pm;
        this.mInstallPackageHelper = new InstallPackageHelper(pm);
    }

    @Override // android.os.Handler
    public void handleMessage(Message msg) {
        try {
            doHandleMessage(msg);
        } finally {
            Process.setThreadPriority(0);
        }
    }

    void doHandleMessage(Message msg) {
        boolean killApp;
        VerificationParams params;
        switch (msg.what) {
            case 1:
                this.mInstallPackageHelper.sendPendingBroadcasts();
                return;
            case 2:
            case 3:
            case 4:
            case 6:
            case 7:
            case 8:
            case 10:
            case 11:
            case 12:
            case 17:
            case 18:
            default:
                return;
            case 5:
                HandlerParams params2 = (HandlerParams) msg.obj;
                if (params2 != null) {
                    if (PackageManagerService.DEBUG_INSTALL) {
                        Slog.i("PackageManager", "init_copy: " + params2);
                    }
                    Trace.asyncTraceEnd(262144L, "queueInstall", System.identityHashCode(params2));
                    Trace.traceBegin(262144L, "startCopy");
                    params2.startCopy();
                    Trace.traceEnd(262144L);
                    return;
                }
                return;
            case 9:
                if (PackageManagerService.DEBUG_INSTALL) {
                    Log.v("PackageManager", "Handling post-install for " + msg.arg1);
                }
                PostInstallData data = this.mPm.mRunningInstalls.get(msg.arg1);
                killApp = msg.arg2 != 0;
                this.mPm.mRunningInstalls.delete(msg.arg1);
                if (data != null && data.res.mFreezer != null) {
                    data.res.mFreezer.close();
                }
                if (data != null && data.mPostInstallRunnable != null) {
                    data.mPostInstallRunnable.run();
                } else if (data != null && data.args != null) {
                    this.mInstallPackageHelper.handlePackagePostInstall(data.res, data.args, killApp);
                } else if (PackageManagerService.DEBUG_INSTALL) {
                    Slog.i("PackageManager", "Nothing to do for post-install token " + msg.arg1);
                }
                Trace.asyncTraceEnd(262144L, "postInstall", msg.arg1);
                return;
            case 13:
                this.mPm.writeSettings();
                return;
            case 14:
                this.mPm.writePendingRestrictions();
                return;
            case 15:
                int verificationId = msg.arg1;
                PackageVerificationState state = this.mPm.mPendingVerification.get(verificationId);
                if (state == null) {
                    Slog.w("PackageManager", "Verification with id " + verificationId + " not found. It may be invalid or overridden by integrity verification");
                    return;
                } else if (state.isVerificationComplete()) {
                    Slog.w("PackageManager", "Verification with id " + verificationId + " already complete.");
                    return;
                } else {
                    PackageVerificationResponse response = (PackageVerificationResponse) msg.obj;
                    state.setVerifierResponse(response.callerUid, response.code);
                    if (state.isVerificationComplete()) {
                        VerificationParams params3 = state.getVerificationParams();
                        Uri originUri = Uri.fromFile(params3.mOriginInfo.mResolvedFile);
                        if (state.isInstallAllowed()) {
                            VerificationUtils.broadcastPackageVerified(verificationId, originUri, response.code, null, params3.mDataLoaderType, params3.getUser(), this.mPm.mContext);
                        } else {
                            params3.setReturnCode(-22, "Install not allowed");
                        }
                        if (state.areAllVerificationsComplete()) {
                            this.mPm.mPendingVerification.remove(verificationId);
                        }
                        Trace.asyncTraceEnd(262144L, "verification", verificationId);
                        params3.handleVerificationFinished();
                        return;
                    }
                    return;
                }
            case 16:
                int verificationId2 = msg.arg1;
                killApp = msg.arg2 != 0;
                PackageVerificationState state2 = this.mPm.mPendingVerification.get(verificationId2);
                if (state2 != null && !state2.isVerificationComplete()) {
                    if (killApp || !state2.timeoutExtended()) {
                        PackageVerificationResponse response2 = (PackageVerificationResponse) msg.obj;
                        VerificationParams params4 = state2.getVerificationParams();
                        Uri originUri2 = Uri.fromFile(params4.mOriginInfo.mResolvedFile);
                        String errorMsg = "Verification timed out for " + originUri2;
                        Slog.i("PackageManager", errorMsg);
                        UserHandle user = params4.getUser();
                        if (response2.code == -1) {
                            params = params4;
                            VerificationUtils.broadcastPackageVerified(verificationId2, originUri2, -1, null, params4.mDataLoaderType, user, this.mPm.mContext);
                            params.setReturnCode(-22, errorMsg);
                            state2.setVerifierResponse(response2.callerUid, response2.code);
                        } else {
                            Slog.i("PackageManager", "Continuing with installation of " + originUri2);
                            state2.setVerifierResponse(response2.callerUid, response2.code);
                            VerificationUtils.broadcastPackageVerified(verificationId2, originUri2, 1, null, params4.mDataLoaderType, user, this.mPm.mContext);
                            params = params4;
                        }
                        if (state2.areAllVerificationsComplete()) {
                            this.mPm.mPendingVerification.remove(verificationId2);
                        }
                        Trace.asyncTraceEnd(262144L, "verification", verificationId2);
                        params.handleVerificationFinished();
                        return;
                    }
                    return;
                }
                return;
            case 19:
                this.mPm.writePackageList(msg.arg1);
                return;
            case 20:
                InstantAppResolver.doInstantAppResolutionPhaseTwo(this.mPm.mContext, this.mPm.snapshotComputer(), this.mPm.mUserManager, this.mPm.mInstantAppResolverConnection, (InstantAppRequest) msg.obj, this.mPm.mInstantAppInstallerActivity, this.mPm.mHandler);
                return;
            case 21:
                int enableRollbackToken = msg.arg1;
                int enableRollbackCode = msg.arg2;
                VerificationParams params5 = this.mPm.mPendingEnableRollback.get(enableRollbackToken);
                if (params5 == null) {
                    Slog.w("PackageManager", "Invalid rollback enabled token " + enableRollbackToken + " received");
                    return;
                }
                this.mPm.mPendingEnableRollback.remove(enableRollbackToken);
                if (enableRollbackCode != 1) {
                    Uri originUri3 = Uri.fromFile(params5.mOriginInfo.mResolvedFile);
                    Slog.w("PackageManager", "Failed to enable rollback for " + originUri3);
                    Slog.w("PackageManager", "Continuing with installation of " + originUri3);
                }
                Trace.asyncTraceEnd(262144L, "enable_rollback", enableRollbackToken);
                params5.handleRollbackEnabled();
                return;
            case 22:
                int enableRollbackToken2 = msg.arg1;
                int sessionId = msg.arg2;
                VerificationParams params6 = this.mPm.mPendingEnableRollback.get(enableRollbackToken2);
                if (params6 != null) {
                    Uri originUri4 = Uri.fromFile(params6.mOriginInfo.mResolvedFile);
                    Slog.w("PackageManager", "Enable rollback timed out for " + originUri4);
                    this.mPm.mPendingEnableRollback.remove(enableRollbackToken2);
                    Slog.w("PackageManager", "Continuing with installation of " + originUri4);
                    Trace.asyncTraceEnd(262144L, "enable_rollback", enableRollbackToken2);
                    params6.handleRollbackEnabled();
                    Intent rollbackTimeoutIntent = new Intent("android.intent.action.CANCEL_ENABLE_ROLLBACK");
                    rollbackTimeoutIntent.putExtra(PackageManagerInternal.EXTRA_ENABLE_ROLLBACK_SESSION_ID, sessionId);
                    rollbackTimeoutIntent.addFlags(67108864);
                    this.mPm.mContext.sendBroadcastAsUser(rollbackTimeoutIntent, UserHandle.SYSTEM, "android.permission.PACKAGE_ROLLBACK_AGENT");
                    return;
                }
                return;
            case 23:
                synchronized (this.mPm.mInstallLock) {
                    InstallArgs args = (InstallArgs) msg.obj;
                    if (args != null) {
                        args.doPostDeleteLI(true);
                    }
                }
                return;
            case 24:
            case 29:
                String packageName = (String) msg.obj;
                if (packageName != null) {
                    killApp = msg.what == 29;
                    this.mPm.notifyInstallObserver(packageName, killApp);
                    return;
                }
                return;
            case 25:
                int verificationId3 = msg.arg1;
                PackageVerificationState state3 = this.mPm.mPendingVerification.get(verificationId3);
                if (state3 == null) {
                    Slog.w("PackageManager", "Integrity verification with id " + verificationId3 + " not found. It may be invalid or overridden by verifier");
                    return;
                }
                int response3 = ((Integer) msg.obj).intValue();
                VerificationParams params7 = state3.getVerificationParams();
                Uri originUri5 = Uri.fromFile(params7.mOriginInfo.mResolvedFile);
                state3.setIntegrityVerificationResult(response3);
                if (response3 == 1) {
                    Slog.i("PackageManager", "Integrity check passed for " + originUri5);
                } else {
                    params7.setReturnCode(-22, "Integrity check failed for " + originUri5);
                }
                if (state3.areAllVerificationsComplete()) {
                    this.mPm.mPendingVerification.remove(verificationId3);
                }
                Trace.asyncTraceEnd(262144L, "integrity_verification", verificationId3);
                params7.handleIntegrityVerificationFinished();
                return;
            case 26:
                int messageCode = msg.arg1;
                PackageVerificationState state4 = this.mPm.mPendingVerification.get(messageCode);
                if (state4 != null && !state4.isIntegrityVerificationComplete()) {
                    VerificationParams params8 = state4.getVerificationParams();
                    Uri originUri6 = Uri.fromFile(params8.mOriginInfo.mResolvedFile);
                    String errorMsg2 = "Integrity verification timed out for " + originUri6;
                    Slog.i("PackageManager", errorMsg2);
                    state4.setIntegrityVerificationResult(getDefaultIntegrityVerificationResponse());
                    if (getDefaultIntegrityVerificationResponse() == 1) {
                        Slog.i("PackageManager", "Integrity check times out, continuing with " + originUri6);
                    } else {
                        params8.setReturnCode(-22, errorMsg2);
                    }
                    if (state4.areAllVerificationsComplete()) {
                        this.mPm.mPendingVerification.remove(messageCode);
                    }
                    Trace.asyncTraceEnd(262144L, "integrity_verification", messageCode);
                    params8.handleIntegrityVerificationFinished();
                    return;
                }
                return;
            case 27:
                int messageCode2 = msg.arg1;
                Object object = msg.obj;
                this.mPm.mDomainVerificationManager.runMessage(messageCode2, object);
                return;
            case 28:
                try {
                    this.mPm.mInjector.getSharedLibrariesImpl().pruneUnusedStaticSharedLibraries(this.mPm.snapshotComputer(), JobStatus.NO_LATEST_RUNTIME, Settings.Global.getLong(this.mPm.mContext.getContentResolver(), "unused_static_shared_lib_min_cache_period", PackageManagerService.DEFAULT_UNUSED_STATIC_SHARED_LIB_MIN_CACHE_PERIOD));
                    return;
                } catch (IOException e) {
                    Log.w("PackageManager", "Failed to prune unused static shared libraries :" + e.getMessage());
                    return;
                }
        }
    }

    private int getDefaultIntegrityVerificationResponse() {
        return -1;
    }
}
