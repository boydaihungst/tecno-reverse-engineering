package com.android.server.os;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.IDumpstate;
import android.os.IDumpstateListener;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.telephony.TelephonyManager;
import android.util.ArraySet;
import android.util.Slog;
import com.android.server.SystemConfig;
import java.io.FileDescriptor;
import java.util.Objects;
/* loaded from: classes2.dex */
class BugreportManagerServiceImpl extends IDumpstate.Stub {
    private static final String BUGREPORT_SERVICE = "bugreportd";
    private static final long DEFAULT_BUGREPORT_SERVICE_TIMEOUT_MILLIS = 30000;
    private static final String TAG = "BugreportManagerService";
    private final AppOpsManager mAppOps;
    private final Context mContext;
    private final TelephonyManager mTelephonyManager;
    private final Object mLock = new Object();
    private final ArraySet<String> mBugreportWhitelistedPackages = SystemConfig.getInstance().getBugreportWhitelistedPackages();

    /* JADX INFO: Access modifiers changed from: package-private */
    public BugreportManagerServiceImpl(Context context) {
        this.mContext = context;
        this.mAppOps = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        this.mTelephonyManager = (TelephonyManager) context.getSystemService(TelephonyManager.class);
    }

    @Override // android.os.IDumpstate
    public void startBugreport(int callingUidUnused, String callingPackage, FileDescriptor bugreportFd, FileDescriptor screenshotFd, int bugreportMode, IDumpstateListener listener, boolean isScreenshotRequested) {
        Objects.requireNonNull(callingPackage);
        Objects.requireNonNull(bugreportFd);
        Objects.requireNonNull(listener);
        validateBugreportMode(bugreportMode);
        int callingUid = Binder.getCallingUid();
        enforcePermission(callingPackage, callingUid, bugreportMode == 4);
        long identity = Binder.clearCallingIdentity();
        try {
            ensureUserCanTakeBugReport(bugreportMode);
            Binder.restoreCallingIdentity(identity);
            synchronized (this.mLock) {
                startBugreportLocked(callingUid, callingPackage, bugreportFd, screenshotFd, bugreportMode, listener, isScreenshotRequested);
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
    }

    @Override // android.os.IDumpstate
    public void cancelBugreport(int callingUidUnused, String callingPackage) {
        int callingUid = Binder.getCallingUid();
        enforcePermission(callingPackage, callingUid, true);
        synchronized (this.mLock) {
            IDumpstate ds = getDumpstateBinderServiceLocked();
            if (ds == null) {
                Slog.w(TAG, "cancelBugreport: Could not find native dumpstate service");
                return;
            }
            try {
                ds.cancelBugreport(callingUid, callingPackage);
            } catch (RemoteException e) {
                Slog.e(TAG, "RemoteException in cancelBugreport", e);
            }
            SystemProperties.set("ctl.stop", BUGREPORT_SERVICE);
        }
    }

    private void validateBugreportMode(int mode) {
        if (mode != 0 && mode != 1 && mode != 2 && mode != 3 && mode != 4 && mode != 5) {
            Slog.w(TAG, "Unknown bugreport mode: " + mode);
            throw new IllegalArgumentException("Unknown bugreport mode: " + mode);
        }
    }

    private void enforcePermission(String callingPackage, int callingUid, boolean checkCarrierPrivileges) {
        this.mAppOps.checkPackage(callingUid, callingPackage);
        if (this.mBugreportWhitelistedPackages.contains(callingPackage) && this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") == 0) {
            return;
        }
        long token = Binder.clearCallingIdentity();
        if (checkCarrierPrivileges) {
            try {
                if (this.mTelephonyManager.checkCarrierPrivilegesForPackageAnyPhone(callingPackage) == 1) {
                    return;
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
        Binder.restoreCallingIdentity(token);
        String message = callingPackage + " does not hold the DUMP permission or is not bugreport-whitelisted " + (checkCarrierPrivileges ? "and does not have carrier privileges " : "") + "to request a bugreport";
        Slog.w(TAG, message);
        throw new SecurityException(message);
    }

    private void ensureUserCanTakeBugReport(int bugreportMode) {
        UserInfo currentUser = null;
        try {
            currentUser = ActivityManager.getService().getCurrentUser();
        } catch (RemoteException e) {
        }
        UserInfo primaryUser = UserManager.get(this.mContext).getPrimaryUser();
        if (currentUser == null) {
            logAndThrow("No current user. Only primary user is allowed to take bugreports.");
        }
        if (primaryUser == null) {
            logAndThrow("No primary user. Only primary user is allowed to take bugreports.");
        }
        if (primaryUser.id != currentUser.id) {
            if (bugreportMode == 2 && isCurrentUserAffiliated(currentUser.id)) {
                return;
            }
            logAndThrow("Current user not primary user. Only primary user is allowed to take bugreports.");
        }
    }

    private boolean isCurrentUserAffiliated(int currentUserId) {
        DevicePolicyManager dpm = (DevicePolicyManager) this.mContext.getSystemService(DevicePolicyManager.class);
        int deviceOwnerUid = dpm.getDeviceOwnerUserId();
        if (deviceOwnerUid == -10000) {
            return false;
        }
        int callingUserId = UserHandle.getUserId(Binder.getCallingUid());
        Slog.i(TAG, "callingUid: " + callingUserId + " deviceOwnerUid: " + deviceOwnerUid + " currentUserId: " + currentUserId);
        if (callingUserId != deviceOwnerUid) {
            logAndThrow("Caller is not device owner on provisioned device.");
        }
        if (!dpm.isAffiliatedUser(currentUserId)) {
            logAndThrow("Current user is not affiliated to the device owner.");
            return true;
        }
        return true;
    }

    private void startBugreportLocked(int callingUid, String callingPackage, FileDescriptor bugreportFd, FileDescriptor screenshotFd, int bugreportMode, IDumpstateListener listener, boolean isScreenshotRequested) {
        if (isDumpstateBinderServiceRunningLocked()) {
            Slog.w(TAG, "'dumpstate' is already running. Cannot start a new bugreport while another one is currently in progress.");
            reportError(listener, 5);
            return;
        }
        IDumpstate ds = startAndGetDumpstateBinderServiceLocked();
        if (ds == null) {
            Slog.w(TAG, "Unable to get bugreport service");
            reportError(listener, 2);
            return;
        }
        IDumpstateListener myListener = new DumpstateListener(listener, ds);
        try {
            ds.startBugreport(callingUid, callingPackage, bugreportFd, screenshotFd, bugreportMode, myListener, isScreenshotRequested);
        } catch (RemoteException e) {
            cancelBugreport(callingUid, callingPackage);
        }
    }

    private boolean isDumpstateBinderServiceRunningLocked() {
        return getDumpstateBinderServiceLocked() != null;
    }

    private IDumpstate getDumpstateBinderServiceLocked() {
        return IDumpstate.Stub.asInterface(ServiceManager.getService("dumpstate"));
    }

    private IDumpstate startAndGetDumpstateBinderServiceLocked() {
        SystemProperties.set("ctl.start", BUGREPORT_SERVICE);
        IDumpstate ds = null;
        boolean timedOut = false;
        int totalTimeWaitedMillis = 0;
        int seedWaitTimeMillis = 500;
        while (true) {
            if (timedOut) {
                break;
            }
            ds = getDumpstateBinderServiceLocked();
            if (ds != null) {
                Slog.i(TAG, "Got bugreport service handle.");
                break;
            }
            SystemClock.sleep(seedWaitTimeMillis);
            Slog.i(TAG, "Waiting to get dumpstate service handle (" + totalTimeWaitedMillis + "ms)");
            totalTimeWaitedMillis += seedWaitTimeMillis;
            seedWaitTimeMillis *= 2;
            timedOut = ((long) totalTimeWaitedMillis) > 30000;
        }
        if (timedOut) {
            Slog.w(TAG, "Timed out waiting to get dumpstate service handle (" + totalTimeWaitedMillis + "ms)");
        }
        return ds;
    }

    private void reportError(IDumpstateListener listener, int errorCode) {
        try {
            listener.onError(errorCode);
        } catch (RemoteException e) {
            Slog.w(TAG, "onError() transaction threw RemoteException: " + e.getMessage());
        }
    }

    private void logAndThrow(String message) {
        Slog.w(TAG, message);
        throw new IllegalArgumentException(message);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class DumpstateListener extends IDumpstateListener.Stub implements IBinder.DeathRecipient {
        private boolean mDone = false;
        private final IDumpstate mDs;
        private final IDumpstateListener mListener;

        DumpstateListener(IDumpstateListener listener, IDumpstate ds) {
            this.mListener = listener;
            this.mDs = ds;
            try {
                ds.asBinder().linkToDeath(this, 0);
            } catch (RemoteException e) {
                Slog.e(BugreportManagerServiceImpl.TAG, "Unable to register Death Recipient for IDumpstate", e);
            }
        }

        @Override // android.os.IDumpstateListener
        public void onProgress(int progress) throws RemoteException {
            this.mListener.onProgress(progress);
        }

        @Override // android.os.IDumpstateListener
        public void onError(int errorCode) throws RemoteException {
            synchronized (BugreportManagerServiceImpl.this.mLock) {
                this.mDone = true;
            }
            this.mListener.onError(errorCode);
        }

        @Override // android.os.IDumpstateListener
        public void onFinished() throws RemoteException {
            synchronized (BugreportManagerServiceImpl.this.mLock) {
                this.mDone = true;
            }
            this.mListener.onFinished();
        }

        @Override // android.os.IDumpstateListener
        public void onScreenshotTaken(boolean success) throws RemoteException {
            this.mListener.onScreenshotTaken(success);
        }

        @Override // android.os.IDumpstateListener
        public void onUiIntensiveBugreportDumpsFinished() throws RemoteException {
            this.mListener.onUiIntensiveBugreportDumpsFinished();
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
            }
            synchronized (BugreportManagerServiceImpl.this.mLock) {
                if (!this.mDone) {
                    Slog.e(BugreportManagerServiceImpl.TAG, "IDumpstate likely crashed. Notifying listener");
                    try {
                        this.mListener.onError(2);
                    } catch (RemoteException e2) {
                    }
                }
            }
            this.mDs.asBinder().unlinkToDeath(this, 0);
        }
    }
}
