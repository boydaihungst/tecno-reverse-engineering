package com.android.server.pm;

import android.apex.ApexInfo;
import android.apex.ApexSessionInfo;
import android.apex.ApexSessionParams;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApexStagedEvent;
import android.content.pm.IStagedApexObserver;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManagerInternal;
import android.content.pm.StagedApexInfo;
import android.hardware.biometrics.fingerprint.V2_1.RequestStatus;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IntArray;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimingsTraceLog;
import com.android.internal.content.InstallLocationUtils;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.SystemServiceManager;
import com.android.server.pm.StagingManager;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.AndroidPackageUtils;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.PackageStateUtils;
import com.android.server.rollback.RollbackManagerInternal;
import com.android.server.rollback.WatchdogRollbackLogger;
import com.android.server.usb.descriptors.UsbEndpointDescriptor;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
/* loaded from: classes2.dex */
public class StagingManager {
    private static final String TAG = "StagingManager";
    private final ApexManager mApexManager;
    private final CompletableFuture<Void> mBootCompleted;
    private final Context mContext;
    private final List<String> mFailedPackageNames;
    private String mFailureReason;
    private final File mFailureReasonFile;
    private String mNativeFailureReason;
    private final PowerManager mPowerManager;
    private final List<IStagedApexObserver> mStagedApexObservers;
    private final SparseArray<StagedSession> mStagedSessions;
    private final List<Integer> mSuccessfulStagedSessionIds;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface StagedSession {
        void abandon();

        boolean containsApexSession();

        boolean containsApkSession();

        List<StagedSession> getChildSessions();

        long getCommittedMillis();

        String getPackageName();

        int getParentSessionId();

        boolean hasParentSessionId();

        CompletableFuture<Void> installSession();

        boolean isApexSession();

        boolean isCommitted();

        boolean isDestroyed();

        boolean isInTerminalState();

        boolean isMultiPackage();

        boolean isSessionApplied();

        boolean isSessionFailed();

        boolean isSessionReady();

        boolean sessionContains(Predicate<StagedSession> predicate);

        int sessionId();

        PackageInstaller.SessionParams sessionParams();

        void setSessionApplied();

        void setSessionFailed(int i, String str);

        void setSessionReady();

        void verifySession();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public StagingManager(Context context) {
        this(context, ApexManager.getInstance());
    }

    StagingManager(Context context, ApexManager apexManager) {
        File file = new File("/metadata/staged-install/failure_reason.txt");
        this.mFailureReasonFile = file;
        this.mStagedSessions = new SparseArray<>();
        this.mFailedPackageNames = new ArrayList();
        this.mSuccessfulStagedSessionIds = new ArrayList();
        this.mStagedApexObservers = new ArrayList();
        this.mBootCompleted = new CompletableFuture<>();
        this.mContext = context;
        this.mApexManager = apexManager;
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        if (file.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                this.mFailureReason = reader.readLine();
                reader.close();
            } catch (Exception e) {
            }
        }
    }

    /* loaded from: classes2.dex */
    public static final class Lifecycle extends SystemService {
        private static StagingManager sStagingManager;

        public Lifecycle(Context context) {
            super(context);
        }

        void startService(StagingManager stagingManager) {
            sStagingManager = stagingManager;
            ((SystemServiceManager) LocalServices.getService(SystemServiceManager.class)).startService(this);
        }

        @Override // com.android.server.SystemService
        public void onStart() {
        }

        @Override // com.android.server.SystemService
        public void onBootPhase(int phase) {
            StagingManager stagingManager;
            if (phase == 1000 && (stagingManager = sStagingManager) != null) {
                stagingManager.markStagedSessionsAsSuccessful();
                sStagingManager.markBootCompleted();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void markBootCompleted() {
        this.mApexManager.markBootCompleted();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerStagedApexObserver(final IStagedApexObserver observer) {
        if (observer == null) {
            return;
        }
        if (observer.asBinder() != null) {
            try {
                observer.asBinder().linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.pm.StagingManager.1
                    @Override // android.os.IBinder.DeathRecipient
                    public void binderDied() {
                        synchronized (StagingManager.this.mStagedApexObservers) {
                            StagingManager.this.mStagedApexObservers.remove(observer);
                        }
                    }
                }, 0);
            } catch (RemoteException re) {
                Slog.w(TAG, re.getMessage());
            }
        }
        synchronized (this.mStagedApexObservers) {
            this.mStagedApexObservers.add(observer);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterStagedApexObserver(IStagedApexObserver observer) {
        synchronized (this.mStagedApexObservers) {
            this.mStagedApexObservers.remove(observer);
        }
    }

    private void abortCheckpoint(String failureReason, boolean supportsCheckpoint, boolean needsCheckpoint) {
        Slog.e(TAG, failureReason);
        if (supportsCheckpoint && needsCheckpoint) {
            try {
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(this.mFailureReasonFile));
                    writer.write(failureReason);
                    writer.close();
                } catch (Exception e) {
                    Slog.w(TAG, "Failed to save failure reason: ", e);
                }
                if (this.mApexManager.isApexSupported()) {
                    this.mApexManager.revertActiveSessions();
                }
                InstallLocationUtils.getStorageManager().abortChanges("abort-staged-install", false);
            } catch (Exception e2) {
                Slog.wtf(TAG, "Failed to abort checkpoint", e2);
                if (this.mApexManager.isApexSupported()) {
                    this.mApexManager.revertActiveSessions();
                }
                this.mPowerManager.reboot(null);
            }
        }
    }

    private List<StagedSession> extractApexSessions(StagedSession session) {
        List<StagedSession> apexSessions = new ArrayList<>();
        if (session.isMultiPackage()) {
            for (StagedSession s : session.getChildSessions()) {
                if (s.containsApexSession()) {
                    apexSessions.add(s);
                }
            }
        } else {
            apexSessions.add(session);
        }
        return apexSessions;
    }

    private void checkInstallationOfApkInApexSuccessful(StagedSession session) throws PackageManagerException {
        List<StagedSession> apexSessions = extractApexSessions(session);
        if (apexSessions.isEmpty()) {
            return;
        }
        for (StagedSession apexSession : apexSessions) {
            String packageName = apexSession.getPackageName();
            String errorMsg = this.mApexManager.getApkInApexInstallError(packageName);
            if (errorMsg != null) {
                throw new PackageManagerException(UsbEndpointDescriptor.MASK_ENDPOINT_DIRECTION, "Failed to install apk-in-apex of " + packageName + " : " + errorMsg);
            }
        }
    }

    private void snapshotAndRestoreForApexSession(StagedSession session) {
        boolean doSnapshotOrRestore = (session.sessionParams().installFlags & 262144) != 0 || session.sessionParams().installReason == 5;
        if (!doSnapshotOrRestore) {
            return;
        }
        List<StagedSession> apexSessions = extractApexSessions(session);
        if (apexSessions.isEmpty()) {
            return;
        }
        UserManagerInternal um = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        int[] allUsers = um.getUserIds();
        RollbackManagerInternal rm = (RollbackManagerInternal) LocalServices.getService(RollbackManagerInternal.class);
        int sessionsSize = apexSessions.size();
        for (int i = 0; i < sessionsSize; i++) {
            String packageName = apexSessions.get(i).getPackageName();
            snapshotAndRestoreApexUserData(packageName, allUsers, rm);
            List<String> apksInApex = this.mApexManager.getApksInApex(packageName);
            int apksSize = apksInApex.size();
            for (int j = 0; j < apksSize; j++) {
                snapshotAndRestoreApkInApexUserData(apksInApex.get(j), allUsers, rm);
            }
        }
    }

    private void snapshotAndRestoreApexUserData(String packageName, int[] allUsers, RollbackManagerInternal rm) {
        rm.snapshotAndRestoreUserData(packageName, UserHandle.toUserHandles(allUsers), 0, 0L, null, 0);
    }

    private void snapshotAndRestoreApkInApexUserData(String packageName, int[] allUsers, RollbackManagerInternal rm) {
        PackageManagerInternal mPmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        AndroidPackage pkg = mPmi.getPackage(packageName);
        if (pkg == null) {
            Slog.e(TAG, "Could not find package: " + packageName + "for snapshotting/restoring user data.");
            return;
        }
        PackageStateInternal ps = mPmi.getPackageStateInternal(packageName);
        if (ps != null) {
            int appId = ps.getAppId();
            long ceDataInode = ps.getUserStateOrDefault(0).getCeDataInode();
            int[] installedUsers = PackageStateUtils.queryInstalledUsers(ps, allUsers, true);
            String seInfo = AndroidPackageUtils.getSeInfo(pkg, ps);
            rm.snapshotAndRestoreUserData(packageName, UserHandle.toUserHandles(installedUsers), appId, ceDataInode, seInfo, 0);
        }
    }

    private void prepareForLoggingApexdRevert(StagedSession session, String nativeFailureReason) {
        synchronized (this.mFailedPackageNames) {
            this.mNativeFailureReason = nativeFailureReason;
            if (session.getPackageName() != null) {
                this.mFailedPackageNames.add(session.getPackageName());
            }
        }
    }

    private void resumeSession(StagedSession session, boolean supportsCheckpoint, boolean needsCheckpoint) throws PackageManagerException {
        Slog.d(TAG, "Resuming session " + session.sessionId());
        boolean hasApex = session.containsApexSession();
        if (supportsCheckpoint && !needsCheckpoint) {
            String revertMsg = "Reverting back to safe state. Marking " + session.sessionId() + " as failed.";
            String reasonForRevert = getReasonForRevert();
            if (!TextUtils.isEmpty(reasonForRevert)) {
                revertMsg = revertMsg + " Reason for revert: " + reasonForRevert;
            }
            Slog.d(TAG, revertMsg);
            session.setSessionFailed(RequestStatus.SYS_ETIMEDOUT, revertMsg);
            return;
        }
        if (hasApex) {
            checkInstallationOfApkInApexSuccessful(session);
            checkDuplicateApkInApex(session);
            snapshotAndRestoreForApexSession(session);
            Slog.i(TAG, "APEX packages in session " + session.sessionId() + " were successfully activated. Proceeding with APK packages, if any");
        }
        Slog.d(TAG, "Installing APK packages in session " + session.sessionId());
        TimingsTraceLog t = new TimingsTraceLog("StagingManagerTiming", 262144L);
        t.traceBegin("installApksInSession");
        installApksInSession(session);
        t.traceEnd();
        if (hasApex) {
            if (supportsCheckpoint) {
                synchronized (this.mSuccessfulStagedSessionIds) {
                    this.mSuccessfulStagedSessionIds.add(Integer.valueOf(session.sessionId()));
                }
                return;
            }
            this.mApexManager.markStagedSessionSuccessful(session.sessionId());
        }
    }

    void onInstallationFailure(StagedSession session, PackageManagerException e, boolean supportsCheckpoint, boolean needsCheckpoint) {
        session.setSessionFailed(e.error, e.getMessage());
        abortCheckpoint("Failed to install sessionId: " + session.sessionId() + " Error: " + e.getMessage(), supportsCheckpoint, needsCheckpoint);
        if (!session.containsApexSession()) {
            return;
        }
        if (!this.mApexManager.revertActiveSessions()) {
            Slog.e(TAG, "Failed to abort APEXd session");
            return;
        }
        Slog.e(TAG, "Successfully aborted apexd session. Rebooting device in order to revert to the previous state of APEXd.");
        this.mPowerManager.reboot(null);
    }

    private String getReasonForRevert() {
        if (!TextUtils.isEmpty(this.mFailureReason)) {
            return this.mFailureReason;
        }
        if (!TextUtils.isEmpty(this.mNativeFailureReason)) {
            return "Session reverted due to crashing native process: " + this.mNativeFailureReason;
        }
        return "";
    }

    private void checkDuplicateApkInApex(StagedSession session) throws PackageManagerException {
        if (!session.isMultiPackage()) {
            return;
        }
        Set<String> apkNames = new ArraySet<>();
        for (StagedSession s : session.getChildSessions()) {
            if (!s.isApexSession()) {
                apkNames.add(s.getPackageName());
            }
        }
        List<StagedSession> apexSessions = extractApexSessions(session);
        for (StagedSession apexSession : apexSessions) {
            String packageName = apexSession.getPackageName();
            for (String apkInApex : this.mApexManager.getApksInApex(packageName)) {
                if (!apkNames.add(apkInApex)) {
                    throw new PackageManagerException(UsbEndpointDescriptor.MASK_ENDPOINT_DIRECTION, "Package: " + packageName + " in session: " + apexSession.sessionId() + " has duplicate apk-in-apex: " + apkInApex, null);
                }
            }
        }
    }

    private void installApksInSession(StagedSession session) throws PackageManagerException {
        try {
            session.installSession().get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException ee) {
            throw ((PackageManagerException) ee.getCause());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void commitSession(StagedSession session) {
        createSession(session);
        handleCommittedSession(session);
    }

    private void handleCommittedSession(StagedSession session) {
        if (session.isSessionReady() && session.containsApexSession()) {
            notifyStagedApexObservers();
        }
    }

    void createSession(StagedSession sessionInfo) {
        synchronized (this.mStagedSessions) {
            this.mStagedSessions.append(sessionInfo.sessionId(), sessionInfo);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void abortSession(StagedSession session) {
        synchronized (this.mStagedSessions) {
            this.mStagedSessions.remove(session.sessionId());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void abortCommittedSession(StagedSession session) {
        int sessionId = session.sessionId();
        if (session.isInTerminalState()) {
            Slog.w(TAG, "Cannot abort session in final state: " + sessionId);
        } else if (!session.isDestroyed()) {
            throw new IllegalStateException("Committed session must be destroyed before aborting it from StagingManager");
        } else {
            if (getStagedSession(sessionId) == null) {
                Slog.w(TAG, "Session " + sessionId + " has been abandoned already");
                return;
            }
            if (session.isSessionReady()) {
                if (!ensureActiveApexSessionIsAborted(session)) {
                    Slog.e(TAG, "Failed to abort apex session " + session.sessionId());
                }
                if (session.containsApexSession()) {
                    notifyStagedApexObservers();
                }
            }
            abortSession(session);
        }
    }

    private boolean ensureActiveApexSessionIsAborted(StagedSession session) {
        ApexSessionInfo apexSession;
        if (!session.containsApexSession() || (apexSession = this.mApexManager.getStagedSessionInfo(session.sessionId())) == null || isApexSessionFinalized(apexSession)) {
            return true;
        }
        return this.mApexManager.abortStagedSession(session.sessionId());
    }

    private boolean isApexSessionFinalized(ApexSessionInfo session) {
        return session.isUnknown || session.isActivationFailed || session.isSuccess || session.isReverted;
    }

    private static boolean isApexSessionFailed(ApexSessionInfo apexSessionInfo) {
        return apexSessionInfo.isActivationFailed || apexSessionInfo.isUnknown || apexSessionInfo.isReverted || apexSessionInfo.isRevertInProgress || apexSessionInfo.isRevertFailed;
    }

    private void handleNonReadyAndDestroyedSessions(List<StagedSession> sessions) {
        int j = sessions.size();
        int i = 0;
        while (i < j) {
            final StagedSession session = sessions.get(i);
            if (session.isDestroyed()) {
                session.abandon();
                StagedSession session2 = sessions.set(j - 1, session);
                sessions.set(i, session2);
                j--;
            } else if (!session.isSessionReady()) {
                Slog.i(TAG, "Restart verification for session=" + session.sessionId());
                this.mBootCompleted.thenRun(new Runnable() { // from class: com.android.server.pm.StagingManager$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        StagingManager.StagedSession.this.verifySession();
                    }
                });
                StagedSession session22 = sessions.set(j - 1, session);
                sessions.set(i, session22);
                j--;
            } else {
                i++;
            }
        }
        int i2 = sessions.size();
        sessions.subList(j, i2).clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void restoreSessions(List<StagedSession> sessions, boolean isDeviceUpgrading) {
        TimingsTraceLog t = new TimingsTraceLog("StagingManagerTiming", 262144L);
        t.traceBegin("restoreSessions");
        if (SystemProperties.getBoolean("sys.boot_completed", false)) {
            return;
        }
        for (int i = 0; i < sessions.size(); i++) {
            StagedSession session = sessions.get(i);
            Preconditions.checkArgument(!session.hasParentSessionId(), session.sessionId() + " is a child session");
            Preconditions.checkArgument(session.isCommitted(), session.sessionId() + " is not committed");
            Preconditions.checkArgument(true ^ session.isInTerminalState(), session.sessionId() + " is in terminal state");
            createSession(session);
        }
        if (isDeviceUpgrading) {
            for (int i2 = 0; i2 < sessions.size(); i2++) {
                sessions.get(i2).setSessionFailed(UsbEndpointDescriptor.MASK_ENDPOINT_DIRECTION, "Build fingerprint has changed");
            }
            return;
        }
        try {
            boolean supportsCheckpoint = InstallLocationUtils.getStorageManager().supportsCheckpoint();
            boolean needsCheckpoint = InstallLocationUtils.getStorageManager().needsCheckpoint();
            if (sessions.size() > 1 && !supportsCheckpoint) {
                throw new IllegalStateException("Detected multiple staged sessions on a device without fs-checkpoint support");
            }
            handleNonReadyAndDestroyedSessions(sessions);
            SparseArray<ApexSessionInfo> apexSessions = this.mApexManager.getSessions();
            boolean hasFailedApexSession = false;
            boolean hasAppliedApexSession = false;
            for (int i3 = 0; i3 < sessions.size(); i3++) {
                StagedSession session2 = sessions.get(i3);
                if (session2.containsApexSession()) {
                    ApexSessionInfo apexSession = apexSessions.get(session2.sessionId());
                    if (apexSession == null || apexSession.isUnknown) {
                        session2.setSessionFailed(UsbEndpointDescriptor.MASK_ENDPOINT_DIRECTION, "apexd did not know anything about a staged session supposed to be activated");
                        hasFailedApexSession = true;
                    } else if (isApexSessionFailed(apexSession)) {
                        hasFailedApexSession = true;
                        if (!TextUtils.isEmpty(apexSession.crashingNativeProcess)) {
                            prepareForLoggingApexdRevert(session2, apexSession.crashingNativeProcess);
                        }
                        String errorMsg = "APEX activation failed.";
                        String reasonForRevert = getReasonForRevert();
                        if (!TextUtils.isEmpty(reasonForRevert)) {
                            errorMsg = "APEX activation failed. Reason: " + reasonForRevert;
                        } else if (!TextUtils.isEmpty(apexSession.errorMessage)) {
                            errorMsg = "APEX activation failed. Error: " + apexSession.errorMessage;
                        }
                        Slog.d(TAG, errorMsg);
                        session2.setSessionFailed(UsbEndpointDescriptor.MASK_ENDPOINT_DIRECTION, errorMsg);
                    } else if (apexSession.isActivated || apexSession.isSuccess) {
                        hasAppliedApexSession = true;
                    } else if (apexSession.isStaged) {
                        session2.setSessionFailed(UsbEndpointDescriptor.MASK_ENDPOINT_DIRECTION, "Staged session " + session2.sessionId() + " at boot didn't activate nor fail. Marking it as failed anyway.");
                        hasFailedApexSession = true;
                    } else {
                        Slog.w(TAG, "Apex session " + session2.sessionId() + " is in impossible state");
                        session2.setSessionFailed(UsbEndpointDescriptor.MASK_ENDPOINT_DIRECTION, "Impossible state");
                        hasFailedApexSession = true;
                    }
                }
            }
            if (hasAppliedApexSession && hasFailedApexSession) {
                abortCheckpoint("Found both applied and failed apex sessions", supportsCheckpoint, needsCheckpoint);
            } else if (hasFailedApexSession) {
                for (int i4 = 0; i4 < sessions.size(); i4++) {
                    StagedSession session3 = sessions.get(i4);
                    if (!session3.isSessionFailed()) {
                        session3.setSessionFailed(UsbEndpointDescriptor.MASK_ENDPOINT_DIRECTION, "Another apex session failed");
                    }
                }
            } else {
                for (int i5 = 0; i5 < sessions.size(); i5++) {
                    StagedSession session4 = sessions.get(i5);
                    try {
                        resumeSession(session4, supportsCheckpoint, needsCheckpoint);
                    } catch (PackageManagerException e) {
                        onInstallationFailure(session4, e, supportsCheckpoint, needsCheckpoint);
                    } catch (Exception e2) {
                        Slog.e(TAG, "Staged install failed due to unhandled exception", e2);
                        onInstallationFailure(session4, new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Staged install failed due to unhandled exception: " + e2), supportsCheckpoint, needsCheckpoint);
                    }
                }
                t.traceEnd();
            }
        } catch (RemoteException e3) {
            throw new IllegalStateException("Failed to get checkpoint status", e3);
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: logFailedApexSessionsIfNecessary */
    public void m5695x230d6d14() {
        synchronized (this.mFailedPackageNames) {
            if (!this.mFailedPackageNames.isEmpty()) {
                WatchdogRollbackLogger.logApexdRevert(this.mContext, this.mFailedPackageNames, this.mNativeFailureReason);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void markStagedSessionsAsSuccessful() {
        synchronized (this.mSuccessfulStagedSessionIds) {
            for (int i = 0; i < this.mSuccessfulStagedSessionIds.size(); i++) {
                this.mApexManager.markStagedSessionSuccessful(this.mSuccessfulStagedSessionIds.get(i).intValue());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void systemReady() {
        new Lifecycle(this.mContext).startService(this);
        this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.pm.StagingManager.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context ctx, Intent intent) {
                StagingManager.this.onBootCompletedBroadcastReceived();
                ctx.unregisterReceiver(this);
            }
        }, new IntentFilter("android.intent.action.BOOT_COMPLETED"));
        this.mFailureReasonFile.delete();
    }

    void onBootCompletedBroadcastReceived() {
        this.mBootCompleted.complete(null);
        BackgroundThread.getExecutor().execute(new Runnable() { // from class: com.android.server.pm.StagingManager$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                StagingManager.this.m5695x230d6d14();
            }
        });
    }

    private StagedSession getStagedSession(int sessionId) {
        StagedSession session;
        synchronized (this.mStagedSessions) {
            session = this.mStagedSessions.get(sessionId);
        }
        return session;
    }

    Map<String, ApexInfo> getStagedApexInfos(StagedSession session) {
        Preconditions.checkArgument(session != null, "Session is null");
        Preconditions.checkArgument(true ^ session.hasParentSessionId(), session.sessionId() + " session has parent session");
        Preconditions.checkArgument(session.containsApexSession(), session.sessionId() + " session does not contain apex");
        if (!session.isSessionReady() || session.isDestroyed()) {
            return Collections.emptyMap();
        }
        ApexSessionParams params = new ApexSessionParams();
        params.sessionId = session.sessionId();
        IntArray childSessionIds = new IntArray();
        if (session.isMultiPackage()) {
            for (StagedSession s : session.getChildSessions()) {
                if (s.isApexSession()) {
                    childSessionIds.add(s.sessionId());
                }
            }
        }
        params.childSessionIds = childSessionIds.toArray();
        ApexInfo[] infos = this.mApexManager.getStagedApexInfos(params);
        Map<String, ApexInfo> result = new ArrayMap<>();
        for (ApexInfo info : infos) {
            result.put(info.moduleName, info);
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<String> getStagedApexModuleNames() {
        List<String> result = new ArrayList<>();
        synchronized (this.mStagedSessions) {
            for (int i = 0; i < this.mStagedSessions.size(); i++) {
                StagedSession session = this.mStagedSessions.valueAt(i);
                if (session.isSessionReady() && !session.isDestroyed() && !session.hasParentSessionId() && session.containsApexSession()) {
                    result.addAll(getStagedApexInfos(session).keySet());
                }
            }
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public StagedApexInfo getStagedApexInfo(String moduleName) {
        ApexInfo ai;
        synchronized (this.mStagedSessions) {
            for (int i = 0; i < this.mStagedSessions.size(); i++) {
                StagedSession session = this.mStagedSessions.valueAt(i);
                if (session.isSessionReady() && !session.isDestroyed() && !session.hasParentSessionId() && session.containsApexSession() && (ai = getStagedApexInfos(session).get(moduleName)) != null) {
                    StagedApexInfo info = new StagedApexInfo();
                    info.moduleName = ai.moduleName;
                    info.diskImagePath = ai.modulePath;
                    info.versionCode = ai.versionCode;
                    info.versionName = ai.versionName;
                    info.hasClassPathJars = ai.hasClassPathJars;
                    return info;
                }
            }
            return null;
        }
    }

    private void notifyStagedApexObservers() {
        synchronized (this.mStagedApexObservers) {
            for (IStagedApexObserver observer : this.mStagedApexObservers) {
                ApexStagedEvent event = new ApexStagedEvent();
                event.stagedApexModuleNames = (String[]) getStagedApexModuleNames().toArray(new String[0]);
                try {
                    observer.onApexStaged(event);
                } catch (RemoteException re) {
                    Slog.w(TAG, "Failed to contact the observer " + re.getMessage());
                }
            }
        }
    }
}
