package com.android.server.pm;

import android.apex.ApexInfo;
import android.apex.ApexInfoList;
import android.apex.ApexSessionInfo;
import android.apex.ApexSessionParams;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageInstallObserver2;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.SigningDetails;
import android.content.pm.parsing.result.ParseResult;
import android.content.pm.parsing.result.ParseTypeImpl;
import android.content.rollback.RollbackInfo;
import android.content.rollback.RollbackManager;
import android.hardware.biometrics.fingerprint.V2_1.RequestStatus;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.IntArray;
import android.util.Slog;
import android.util.apk.ApkSignatureVerifier;
import com.android.internal.content.InstallLocationUtils;
import com.android.server.LocalServices;
import com.android.server.SystemConfig;
import com.android.server.pm.StagingManager;
import com.android.server.pm.parsing.PackageParser2;
import com.android.server.pm.parsing.pkg.ParsedPackage;
import com.android.server.pm.pkg.parsing.PackageInfoWithoutStateUtils;
import com.android.server.rollback.RollbackManagerInternal;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class PackageSessionVerifier {
    private static final String TAG = "PackageSessionVerifier";
    private final ApexManager mApexManager;
    private final Context mContext;
    private final Handler mHandler;
    private final Supplier<PackageParser2> mPackageParserSupplier;
    private final PackageManagerService mPm;
    private final List<StagingManager.StagedSession> mStagedSessions;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface Callback {
        void onResult(int i, String str);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageSessionVerifier(Context context, PackageManagerService pm, ApexManager apexManager, Supplier<PackageParser2> packageParserSupplier, Looper looper) {
        this.mStagedSessions = new ArrayList();
        this.mContext = context;
        this.mPm = pm;
        this.mApexManager = apexManager;
        this.mPackageParserSupplier = packageParserSupplier;
        this.mHandler = new Handler(looper);
    }

    PackageSessionVerifier() {
        this.mStagedSessions = new ArrayList();
        this.mContext = null;
        this.mPm = null;
        this.mApexManager = null;
        this.mPackageParserSupplier = null;
        this.mHandler = null;
    }

    public void verify(final PackageInstallerSession session, final Callback callback) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageSessionVerifier$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                PackageSessionVerifier.this.m5589lambda$verify$0$comandroidserverpmPackageSessionVerifier(session, callback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$verify$0$com-android-server-pm-PackageSessionVerifier  reason: not valid java name */
    public /* synthetic */ void m5589lambda$verify$0$comandroidserverpmPackageSessionVerifier(PackageInstallerSession session, Callback callback) {
        try {
            storeSession(session.mStagedSession);
            if (session.isMultiPackage()) {
                for (PackageInstallerSession child : session.getChildSessions()) {
                    checkApexUpdateAllowed(child);
                    checkRebootlessApex(child);
                }
            } else {
                checkApexUpdateAllowed(session);
                checkRebootlessApex(session);
            }
            verifyAPK(session, callback);
        } catch (PackageManagerException e) {
            String errorMessage = PackageManager.installStatusToString(e.error, e.getMessage());
            session.setSessionFailed(e.error, errorMessage);
            callback.onResult(e.error, e.getMessage());
        }
    }

    private void verifyAPK(final PackageInstallerSession session, final Callback callback) throws PackageManagerException {
        VerificationParams verifyingSession = makeVerificationParams(session, new IPackageInstallObserver2.Stub() { // from class: com.android.server.pm.PackageSessionVerifier.1
            public void onUserActionRequired(Intent intent) {
                throw new IllegalStateException();
            }

            public void onPackageInstalled(String basePackageName, int returnCode, String msg, Bundle extras) {
                if (session.isStaged() && returnCode == 1) {
                    PackageSessionVerifier.this.verifyStaged(session.mStagedSession, callback);
                } else if (returnCode != 1) {
                    String errorMessage = PackageManager.installStatusToString(returnCode, msg);
                    session.setSessionFailed(returnCode, errorMessage);
                    callback.onResult(returnCode, msg);
                } else {
                    session.setSessionReady();
                    callback.onResult(1, null);
                }
            }
        });
        if (session.isMultiPackage()) {
            List<PackageInstallerSession> childSessions = session.getChildSessions();
            List<VerificationParams> verifyingChildSessions = new ArrayList<>(childSessions.size());
            for (PackageInstallerSession child : childSessions) {
                verifyingChildSessions.add(makeVerificationParams(child, null));
            }
            verifyingSession.verifyStage(verifyingChildSessions);
            return;
        }
        verifyingSession.verifyStage();
    }

    private VerificationParams makeVerificationParams(PackageInstallerSession session, IPackageInstallObserver2 observer) {
        UserHandle user;
        if ((session.params.installFlags & 64) != 0) {
            user = UserHandle.ALL;
        } else {
            user = new UserHandle(session.userId);
        }
        return new VerificationParams(user, session.stageDir, observer, session.params, session.getInstallSource(), session.getInstallerUid(), session.getSigningDetails(), session.sessionId, session.getPackageLite(), this.mPm);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void verifyStaged(final StagingManager.StagedSession session, final Callback callback) {
        Slog.d(TAG, "Starting preRebootVerification for session " + session.sessionId());
        this.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageSessionVerifier$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                PackageSessionVerifier.this.m5590x27bfd331(session, callback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$verifyStaged$1$com-android-server-pm-PackageSessionVerifier  reason: not valid java name */
    public /* synthetic */ void m5590x27bfd331(StagingManager.StagedSession session, Callback callback) {
        try {
            checkActiveSessions();
            checkRollbacks(session);
            if (session.isMultiPackage()) {
                for (StagingManager.StagedSession child : session.getChildSessions()) {
                    checkOverlaps(session, child);
                }
            } else {
                checkOverlaps(session, session);
            }
            dispatchVerifyApex(session, callback);
        } catch (PackageManagerException e) {
            onVerificationFailure(session, callback, e.error, e.getMessage());
        }
    }

    void storeSession(StagingManager.StagedSession session) {
        if (session != null) {
            this.mStagedSessions.add(session);
        }
    }

    private void onVerificationSuccess(StagingManager.StagedSession session, Callback callback) {
        callback.onResult(1, null);
    }

    private void onVerificationFailure(StagingManager.StagedSession session, Callback callback, int errorCode, String errorMessage) {
        if (!ensureActiveApexSessionIsAborted(session)) {
            Slog.e(TAG, "Failed to abort apex session " + session.sessionId());
        }
        session.setSessionFailed(errorCode, errorMessage);
        callback.onResult(-22, errorMessage);
    }

    private void dispatchVerifyApex(final StagingManager.StagedSession session, final Callback callback) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageSessionVerifier$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                PackageSessionVerifier.this.m5588x7fcc9d7a(session, callback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$dispatchVerifyApex$2$com-android-server-pm-PackageSessionVerifier  reason: not valid java name */
    public /* synthetic */ void m5588x7fcc9d7a(StagingManager.StagedSession session, Callback callback) {
        try {
            verifyApex(session);
            dispatchEndVerification(session, callback);
        } catch (PackageManagerException e) {
            onVerificationFailure(session, callback, e.error, e.getMessage());
        }
    }

    private void dispatchEndVerification(final StagingManager.StagedSession session, final Callback callback) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageSessionVerifier$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                PackageSessionVerifier.this.m5587xa26f9bec(session, callback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$dispatchEndVerification$3$com-android-server-pm-PackageSessionVerifier  reason: not valid java name */
    public /* synthetic */ void m5587xa26f9bec(StagingManager.StagedSession session, Callback callback) {
        try {
            endVerification(session);
            onVerificationSuccess(session, callback);
        } catch (PackageManagerException e) {
            onVerificationFailure(session, callback, e.error, e.getMessage());
        }
    }

    private void verifyApex(StagingManager.StagedSession session) throws PackageManagerException {
        int rollbackId = -1;
        if ((session.sessionParams().installFlags & 262144) != 0) {
            RollbackManagerInternal rm = (RollbackManagerInternal) LocalServices.getService(RollbackManagerInternal.class);
            try {
                rollbackId = rm.notifyStagedSession(session.sessionId());
            } catch (RuntimeException re) {
                Slog.e(TAG, "Failed to notifyStagedSession for session: " + session.sessionId(), re);
            }
        } else if (isRollback(session)) {
            rollbackId = retrieveRollbackIdForCommitSession(session.sessionId());
        }
        boolean hasApex = session.containsApexSession();
        if (hasApex) {
            List<PackageInfo> apexPackages = submitSessionToApexService(session, rollbackId);
            int size = apexPackages.size();
            for (int i = 0; i < size; i++) {
                validateApexSignature(apexPackages.get(i));
            }
            PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            packageManagerInternal.pruneCachedApksInApex(apexPackages);
        }
    }

    private void endVerification(StagingManager.StagedSession session) throws PackageManagerException {
        try {
            if (InstallLocationUtils.getStorageManager().supportsCheckpoint()) {
                InstallLocationUtils.getStorageManager().startCheckpoint(2);
            }
            Slog.d(TAG, "Marking session " + session.sessionId() + " as ready");
            session.setSessionReady();
            if (session.isSessionReady()) {
                boolean hasApex = session.containsApexSession();
                if (hasApex) {
                    this.mApexManager.markStagedSessionReady(session.sessionId());
                }
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failed to get hold of StorageManager", e);
            throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Failed to get hold of StorageManager");
        }
    }

    private void validateApexSignature(PackageInfo newApexPkg) throws PackageManagerException {
        String apexPath = newApexPkg.applicationInfo.sourceDir;
        String packageName = newApexPkg.packageName;
        int minSignatureScheme = ApkSignatureVerifier.getMinimumSignatureSchemeVersionForTargetSdk(newApexPkg.applicationInfo.targetSdkVersion);
        ParseTypeImpl input = ParseTypeImpl.forDefaultParsing();
        ParseResult<SigningDetails> newResult = ApkSignatureVerifier.verify(input.reset(), apexPath, minSignatureScheme);
        if (newResult.isError()) {
            throw new PackageManagerException(-22, "Failed to parse APEX package " + apexPath + " : " + newResult.getException(), newResult.getException());
        }
        SigningDetails newSigningDetails = (SigningDetails) newResult.getResult();
        PackageInfo existingApexPkg = this.mApexManager.getPackageInfo(packageName, 1);
        if (existingApexPkg == null) {
            throw new IllegalStateException("Unknown apex package " + packageName);
        }
        ParseResult<SigningDetails> existingResult = ApkSignatureVerifier.verify(input.reset(), existingApexPkg.applicationInfo.sourceDir, 1);
        if (existingResult.isError()) {
            throw new PackageManagerException(-22, "Failed to parse APEX package " + existingApexPkg.applicationInfo.sourceDir + " : " + existingResult.getException(), existingResult.getException());
        }
        SigningDetails existingSigningDetails = (SigningDetails) existingResult.getResult();
        if (newSigningDetails.checkCapability(existingSigningDetails, 1) || existingSigningDetails.checkCapability(newSigningDetails, 8)) {
            return;
        }
        throw new PackageManagerException(-22, "APK-container signature of APEX package " + packageName + " with version " + newApexPkg.versionCodeMajor + " and path " + apexPath + " is not compatible with the one currently installed on device");
    }

    private List<PackageInfo> submitSessionToApexService(StagingManager.StagedSession session, int rollbackId) throws PackageManagerException {
        IntArray childSessionIds = new IntArray();
        if (session.isMultiPackage()) {
            for (StagingManager.StagedSession s : session.getChildSessions()) {
                if (s.isApexSession()) {
                    childSessionIds.add(s.sessionId());
                }
            }
        }
        ApexSessionParams apexSessionParams = new ApexSessionParams();
        apexSessionParams.sessionId = session.sessionId();
        apexSessionParams.childSessionIds = childSessionIds.toArray();
        if (session.sessionParams().installReason == 5) {
            apexSessionParams.isRollback = true;
            apexSessionParams.rollbackId = rollbackId;
        } else if (rollbackId != -1) {
            apexSessionParams.hasRollbackEnabled = true;
            apexSessionParams.rollbackId = rollbackId;
        }
        ApexInfoList apexInfoList = this.mApexManager.submitStagedSession(apexSessionParams);
        List<PackageInfo> result = new ArrayList<>();
        List<String> apexPackageNames = new ArrayList<>();
        ApexInfo[] apexInfoArr = apexInfoList.apexInfos;
        int length = apexInfoArr.length;
        boolean z = false;
        int i = 0;
        while (i < length) {
            ApexInfo apexInfo = apexInfoArr[i];
            try {
                PackageParser2 packageParser = this.mPackageParserSupplier.get();
                File apexFile = new File(apexInfo.modulePath);
                ParsedPackage parsedPackage = packageParser.parsePackage(apexFile, 128, z);
                PackageInfo packageInfo = PackageInfoWithoutStateUtils.generate(parsedPackage, apexInfo, 128);
                if (packageInfo == null) {
                    throw new PackageManagerException(-22, "Unable to generate package info: " + apexInfo.modulePath);
                }
                if (packageParser != null) {
                    packageParser.close();
                }
                result.add(packageInfo);
                apexPackageNames.add(packageInfo.packageName);
                i++;
                z = false;
            } catch (PackageManagerException e) {
                throw new PackageManagerException(-22, "Failed to parse APEX package " + apexInfo.modulePath + " : " + e, e);
            }
        }
        Slog.d(TAG, "Session " + session.sessionId() + " has following APEX packages: " + apexPackageNames);
        return result;
    }

    private int retrieveRollbackIdForCommitSession(int sessionId) throws PackageManagerException {
        RollbackManager rm = (RollbackManager) this.mContext.getSystemService(RollbackManager.class);
        List<RollbackInfo> rollbacks = rm.getRecentlyCommittedRollbacks();
        int size = rollbacks.size();
        for (int i = 0; i < size; i++) {
            RollbackInfo rollback = rollbacks.get(i);
            if (rollback.getCommittedSessionId() == sessionId) {
                return rollback.getRollbackId();
            }
        }
        throw new PackageManagerException(-22, "Could not find rollback id for commit session: " + sessionId);
    }

    private static boolean isRollback(StagingManager.StagedSession session) {
        return session.sessionParams().installReason == 5;
    }

    private static boolean isApexSessionFinalized(ApexSessionInfo info) {
        return info.isUnknown || info.isActivationFailed || info.isSuccess || info.isReverted;
    }

    private boolean ensureActiveApexSessionIsAborted(StagingManager.StagedSession session) {
        int sessionId;
        ApexSessionInfo apexSession;
        if (!session.containsApexSession() || (apexSession = this.mApexManager.getStagedSessionInfo((sessionId = session.sessionId()))) == null || isApexSessionFinalized(apexSession)) {
            return true;
        }
        return this.mApexManager.abortStagedSession(sessionId);
    }

    private boolean isApexUpdateAllowed(String apexPackageName, String installerPackageName) {
        if (this.mPm.getModuleInfo(apexPackageName, 0) != null) {
            String modulesInstaller = SystemConfig.getInstance().getModulesInstallerPackageName();
            if (modulesInstaller == null) {
                Slog.w(TAG, "No modules installer defined");
                return false;
            }
            return modulesInstaller.equals(installerPackageName);
        }
        String vendorApexInstaller = (String) SystemConfig.getInstance().getAllowedVendorApexes().get(apexPackageName);
        if (vendorApexInstaller == null) {
            Slog.w(TAG, apexPackageName + " is not allowed to be updated");
            return false;
        }
        return vendorApexInstaller.equals(installerPackageName);
    }

    private void checkApexUpdateAllowed(PackageInstallerSession session) throws PackageManagerException {
        if (!session.isApexSession()) {
            return;
        }
        int installFlags = session.params.installFlags;
        if ((4194304 & installFlags) != 0) {
            return;
        }
        String packageName = session.getPackageName();
        String installerPackageName = session.getInstallSource().installerPackageName;
        if (!isApexUpdateAllowed(packageName, installerPackageName)) {
            throw new PackageManagerException(-22, "Update of APEX package " + packageName + " is not allowed for " + installerPackageName);
        }
    }

    void checkRebootlessApex(PackageInstallerSession session) throws PackageManagerException {
        if (session.isStaged() || !session.isApexSession()) {
            return;
        }
        final String packageName = session.getPackageName();
        if (packageName == null) {
            throw new PackageManagerException(-22, "Invalid session " + session.sessionId + " with package name null");
        }
        for (StagingManager.StagedSession stagedSession : this.mStagedSessions) {
            if (!stagedSession.isDestroyed() && !stagedSession.isInTerminalState() && stagedSession.sessionContains(new Predicate() { // from class: com.android.server.pm.PackageSessionVerifier$$ExternalSyntheticLambda0
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    boolean equals;
                    equals = packageName.equals(((StagingManager.StagedSession) obj).getPackageName());
                    return equals;
                }
            })) {
                throw new PackageManagerException(-22, "Staged session " + stagedSession.sessionId() + " already contains " + packageName);
            }
        }
    }

    private void checkActiveSessions() throws PackageManagerException {
        try {
            checkActiveSessions(InstallLocationUtils.getStorageManager().supportsCheckpoint());
        } catch (RemoteException e) {
            throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Can't query fs-checkpoint status : " + e);
        }
    }

    void checkActiveSessions(boolean supportsCheckpoint) throws PackageManagerException {
        int activeSessions = 0;
        for (StagingManager.StagedSession stagedSession : this.mStagedSessions) {
            if (!stagedSession.isDestroyed() && !stagedSession.isInTerminalState()) {
                activeSessions++;
            }
        }
        if (!supportsCheckpoint && activeSessions > 1) {
            throw new PackageManagerException(-119, "Cannot stage multiple sessions without checkpoint support");
        }
    }

    void checkRollbacks(StagingManager.StagedSession session) throws PackageManagerException {
        if (session.isDestroyed() || session.isInTerminalState()) {
            return;
        }
        for (StagingManager.StagedSession stagedSession : this.mStagedSessions) {
            if (!stagedSession.isDestroyed() && !stagedSession.isInTerminalState()) {
                if (isRollback(session) && !isRollback(stagedSession)) {
                    if (!ensureActiveApexSessionIsAborted(stagedSession)) {
                        Slog.e(TAG, "Failed to abort apex session " + stagedSession.sessionId());
                    }
                    stagedSession.setSessionFailed(-119, "Session was failed by rollback session: " + session.sessionId());
                    Slog.i(TAG, "Session " + stagedSession.sessionId() + " is marked failed due to rollback session: " + session.sessionId());
                } else if (!isRollback(session) && isRollback(stagedSession)) {
                    throw new PackageManagerException(-119, "Session was failed by rollback session: " + stagedSession.sessionId());
                }
            }
        }
    }

    void checkOverlaps(StagingManager.StagedSession parent, StagingManager.StagedSession child) throws PackageManagerException {
        if (parent.isDestroyed() || parent.isInTerminalState()) {
            return;
        }
        final String packageName = child.getPackageName();
        if (packageName == null) {
            throw new PackageManagerException(-22, "Cannot stage session " + child.sessionId() + " with package name null");
        }
        for (StagingManager.StagedSession stagedSession : this.mStagedSessions) {
            if (!stagedSession.isDestroyed() && !stagedSession.isInTerminalState() && stagedSession.sessionId() != parent.sessionId() && stagedSession.sessionContains(new Predicate() { // from class: com.android.server.pm.PackageSessionVerifier$$ExternalSyntheticLambda4
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    boolean equals;
                    equals = packageName.equals(((StagingManager.StagedSession) obj).getPackageName());
                    return equals;
                }
            })) {
                if (stagedSession.getCommittedMillis() < parent.getCommittedMillis()) {
                    throw new PackageManagerException(-119, "Package: " + packageName + " in session: " + child.sessionId() + " has been staged already by session: " + stagedSession.sessionId());
                }
                stagedSession.setSessionFailed(-119, "Package: " + packageName + " in session: " + stagedSession.sessionId() + " has been staged already by session: " + child.sessionId());
            }
        }
    }
}
