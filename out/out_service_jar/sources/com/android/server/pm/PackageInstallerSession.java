package com.android.server.pm;

import android.app.AppOpsManager;
import android.app.BroadcastOptions;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyEventLogger;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyManagerInternal;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.Checksum;
import android.content.pm.DataLoaderManager;
import android.content.pm.DataLoaderParams;
import android.content.pm.DataLoaderParamsParcel;
import android.content.pm.FileSystemControlParcel;
import android.content.pm.IDataLoader;
import android.content.pm.IDataLoaderStatusListener;
import android.content.pm.IOnChecksumsReadyListener;
import android.content.pm.IPackageInstallObserver2;
import android.content.pm.IPackageInstallerSession;
import android.content.pm.IPackageInstallerSessionFileSystemConnector;
import android.content.pm.IPackageLoadingProgressCallback;
import android.content.pm.InstallSourceInfo;
import android.content.pm.InstallationFile;
import android.content.pm.InstallationFileParcel;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.SigningDetails;
import android.content.pm.dex.DexMetadataHelper;
import android.content.pm.parsing.ApkLite;
import android.content.pm.parsing.ApkLiteParseUtils;
import android.content.pm.parsing.PackageLite;
import android.content.pm.parsing.result.ParseResult;
import android.content.pm.parsing.result.ParseTypeImpl;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.biometrics.fingerprint.V2_1.RequestStatus;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.FileBridge;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.ParcelableException;
import android.os.RemoteException;
import android.os.RevocableFileDescriptor;
import android.os.SELinux;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.incremental.IStorageHealthListener;
import android.os.incremental.IncrementalFileStorages;
import android.os.incremental.IncrementalManager;
import android.os.incremental.PerUidReadTimeouts;
import android.os.incremental.StorageHealthCheckParams;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.system.ErrnoException;
import android.system.Int64Ref;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.ExceptionUtils;
import android.util.IntArray;
import android.util.MathUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.apk.ApkSignatureVerifier;
import com.android.internal.content.InstallLocationUtils;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.os.SomeArgs;
import com.android.internal.security.VerityUtils;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.server.LocalServices;
import com.android.server.am.BatteryExternalStatsWorker$$ExternalSyntheticLambda4;
import com.android.server.location.gnss.hal.GnssNative;
import com.android.server.pm.Installer;
import com.android.server.pm.PackageInstallerService;
import com.android.server.pm.PackageSessionVerifier;
import com.android.server.pm.StagingManager;
import com.android.server.pm.dex.DexManager;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.pkg.PackageStateInternal;
import com.transsion.hubcore.server.pm.ITranPackageManagerService;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import libcore.io.IoUtils;
import libcore.util.EmptyArray;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class PackageInstallerSession extends IPackageInstallerSession.Stub {
    private static final String APEX_FILE_EXTENSION = ".apex";
    private static final String ATTR_ABI_OVERRIDE = "abiOverride";
    @Deprecated
    private static final String ATTR_APP_ICON = "appIcon";
    private static final String ATTR_APP_LABEL = "appLabel";
    private static final String ATTR_APP_PACKAGE_NAME = "appPackageName";
    private static final String ATTR_CHECKSUM_KIND = "checksumKind";
    private static final String ATTR_CHECKSUM_VALUE = "checksumValue";
    private static final String ATTR_COMMITTED = "committed";
    private static final String ATTR_COMMITTED_MILLIS = "committedMillis";
    private static final String ATTR_CREATED_MILLIS = "createdMillis";
    private static final String ATTR_DATALOADER_ARGUMENTS = "dataLoaderArguments";
    private static final String ATTR_DATALOADER_CLASS_NAME = "dataLoaderClassName";
    private static final String ATTR_DATALOADER_PACKAGE_NAME = "dataLoaderPackageName";
    private static final String ATTR_DATALOADER_TYPE = "dataLoaderType";
    private static final String ATTR_DESTROYED = "destroyed";
    private static final String ATTR_INITIATING_PACKAGE_NAME = "installInitiatingPackageName";
    private static final String ATTR_INSTALLER_ATTRIBUTION_TAG = "installerAttributionTag";
    private static final String ATTR_INSTALLER_PACKAGE_NAME = "installerPackageName";
    private static final String ATTR_INSTALLER_UID = "installerUid";
    private static final String ATTR_INSTALL_FLAGS = "installFlags";
    private static final String ATTR_INSTALL_LOCATION = "installLocation";
    private static final String ATTR_INSTALL_REASON = "installRason";
    private static final String ATTR_IS_APPLIED = "isApplied";
    private static final String ATTR_IS_DATALOADER = "isDataLoader";
    private static final String ATTR_IS_FAILED = "isFailed";
    private static final String ATTR_IS_READY = "isReady";
    private static final String ATTR_LENGTH_BYTES = "lengthBytes";
    private static final String ATTR_LOCATION = "location";
    private static final String ATTR_METADATA = "metadata";
    private static final String ATTR_MODE = "mode";
    private static final String ATTR_MULTI_PACKAGE = "multiPackage";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_ORIGINATING_PACKAGE_NAME = "installOriginatingPackageName";
    private static final String ATTR_ORIGINATING_UID = "originatingUid";
    private static final String ATTR_ORIGINATING_URI = "originatingUri";
    private static final String ATTR_PACKAGE_SOURCE = "packageSource";
    private static final String ATTR_PARENT_SESSION_ID = "parentSessionId";
    private static final String ATTR_PREPARED = "prepared";
    private static final String ATTR_REFERRER_URI = "referrerUri";
    private static final String ATTR_SEALED = "sealed";
    private static final String ATTR_SESSION_ERROR_CODE = "errorCode";
    private static final String ATTR_SESSION_ERROR_MESSAGE = "errorMessage";
    private static final String ATTR_SESSION_ID = "sessionId";
    private static final String ATTR_SESSION_STAGE_CID = "sessionStageCid";
    private static final String ATTR_SESSION_STAGE_DIR = "sessionStageDir";
    private static final String ATTR_SIGNATURE = "signature";
    private static final String ATTR_SIZE_BYTES = "sizeBytes";
    private static final String ATTR_STAGED_SESSION = "stagedSession";
    private static final String ATTR_UPDATED_MILLIS = "updatedMillis";
    private static final String ATTR_USER_ID = "userId";
    private static final String ATTR_VOLUME_UUID = "volumeUuid";
    private static final int INCREMENTAL_STORAGE_BLOCKED_TIMEOUT_MS = 2000;
    private static final int INCREMENTAL_STORAGE_UNHEALTHY_MONITORING_MS = 60000;
    private static final int INCREMENTAL_STORAGE_UNHEALTHY_TIMEOUT_MS = 7000;
    private static final int INVALID_TARGET_SDK_VERSION = Integer.MAX_VALUE;
    private static final boolean LOGD = true;
    private static final int MSG_INSTALL = 3;
    private static final int MSG_ON_PACKAGE_INSTALLED = 4;
    private static final int MSG_ON_SESSION_SEALED = 1;
    private static final int MSG_SESSION_VALIDATION_FAILURE = 5;
    private static final int MSG_STREAM_VALIDATE_AND_COMMIT = 2;
    private static final String PROPERTY_NAME_INHERIT_NATIVE = "pi.inherit_native_on_dont_kill";
    private static final String REMOVE_MARKER_EXTENSION = ".removed";
    private static final String SYSTEM_DATA_LOADER_PACKAGE = "android";
    private static final String TAG = "PackageInstallerSession";
    private static final String TAG_AUTO_REVOKE_PERMISSIONS_MODE = "auto-revoke-permissions-mode";
    static final String TAG_CHILD_SESSION = "childSession";
    private static final String TAG_GRANTED_RUNTIME_PERMISSION = "granted-runtime-permission";
    static final String TAG_SESSION = "session";
    static final String TAG_SESSION_CHECKSUM = "sessionChecksum";
    static final String TAG_SESSION_CHECKSUM_SIGNATURE = "sessionChecksumSignature";
    static final String TAG_SESSION_FILE = "sessionFile";
    private static final String TAG_WHITELISTED_RESTRICTED_PERMISSION = "whitelisted-restricted-permission";
    private static final int USER_ACTION_NOT_NEEDED = 0;
    private static final int USER_ACTION_PENDING_APK_PARSING = 2;
    private static final int USER_ACTION_REQUIRED = 1;
    private long committedMillis;
    final long createdMillis;
    private final PackageInstallerService.InternalCallback mCallback;
    private final Context mContext;
    private volatile boolean mDestroyed;
    private String mFinalMessage;
    private int mFinalStatus;
    private final Handler mHandler;
    private final Handler.Callback mHandlerCallback;
    private boolean mHasDeviceAdminReceiver;
    private IncrementalFileStorages mIncrementalFileStorages;
    private File mInheritedFilesBase;
    private InstallSource mInstallSource;
    private final Installer mInstaller;
    private volatile int mInstallerUid;
    private final String mOriginalInstallerPackageName;
    private final int mOriginalInstallerUid;
    private PackageLite mPackageLite;
    private String mPackageName;
    private int mParentSessionId;
    private Runnable mPendingAbandonCallback;
    private final PackageManagerService mPm;
    private boolean mPrepared;
    private IntentSender mRemoteStatusReceiver;
    private File mResolvedBaseFile;
    private boolean mSessionApplied;
    private int mSessionErrorCode;
    private String mSessionErrorMessage;
    private boolean mSessionFailed;
    private final PackageSessionProvider mSessionProvider;
    private boolean mSessionReady;
    private boolean mShouldBeSealed;
    private SigningDetails mSigningDetails;
    private final SilentUpdatePolicy mSilentUpdatePolicy;
    final StagedSession mStagedSession;
    private final StagingManager mStagingManager;
    private boolean mVerityFoundForApks;
    private long mVersionCode;
    final PackageInstaller.SessionParams params;
    final int sessionId;
    final String stageCid;
    final File stageDir;
    private long updatedMillis;
    final int userId;
    private static final int[] EMPTY_CHILD_SESSION_ARRAY = EmptyArray.INT;
    private static final InstallationFile[] EMPTY_INSTALLATION_FILE_ARRAY = new InstallationFile[0];
    private static final FileFilter sAddedApkFilter = new FileFilter() { // from class: com.android.server.pm.PackageInstallerSession.1
        @Override // java.io.FileFilter
        public boolean accept(File file) {
            return (file.isDirectory() || file.getName().endsWith(PackageInstallerSession.REMOVE_MARKER_EXTENSION) || DexMetadataHelper.isDexMetadataFile(file) || VerityUtils.isFsveritySignatureFile(file) || ApkChecksums.isDigestOrDigestSignatureFile(file)) ? false : true;
        }
    };
    private static final FileFilter sAddedFilter = new FileFilter() { // from class: com.android.server.pm.PackageInstallerSession.2
        @Override // java.io.FileFilter
        public boolean accept(File file) {
            return (file.isDirectory() || file.getName().endsWith(PackageInstallerSession.REMOVE_MARKER_EXTENSION)) ? false : true;
        }
    };
    private static final FileFilter sRemovedFilter = new FileFilter() { // from class: com.android.server.pm.PackageInstallerSession.3
        @Override // java.io.FileFilter
        public boolean accept(File file) {
            return !file.isDirectory() && file.getName().endsWith(PackageInstallerSession.REMOVE_MARKER_EXTENSION);
        }
    };
    private final AtomicInteger mActiveCount = new AtomicInteger();
    private final Object mLock = new Object();
    private final AtomicBoolean mTransactionLock = new AtomicBoolean(false);
    private final Object mProgressLock = new Object();
    private float mClientProgress = 0.0f;
    private float mInternalProgress = 0.0f;
    private float mProgress = 0.0f;
    private float mReportedProgress = -1.0f;
    private float mIncrementalProgress = 0.0f;
    private boolean mSealed = false;
    private final AtomicBoolean mCommitted = new AtomicBoolean(false);
    private boolean mStageDirInUse = false;
    private boolean mPermissionsManuallyAccepted = false;
    private final ArrayList<RevocableFileDescriptor> mFds = new ArrayList<>();
    private final ArrayList<FileBridge> mBridges = new ArrayList<>();
    private SparseArray<PackageInstallerSession> mChildSessions = new SparseArray<>();
    private ArraySet<FileEntry> mFiles = new ArraySet<>();
    private ArrayMap<String, PerFileChecksum> mChecksums = new ArrayMap<>();
    private final List<File> mResolvedStagedFiles = new ArrayList();
    private final List<File> mResolvedInheritedFiles = new ArrayList();
    private final List<String> mResolvedInstructionSets = new ArrayList();
    private final List<String> mResolvedNativeLibPaths = new ArrayList();
    private volatile boolean mDataLoaderFinished = false;
    private int mValidatedTargetSdk = Integer.MAX_VALUE;

    /* loaded from: classes2.dex */
    @interface UserActionRequirement {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class FileEntry {
        private final InstallationFile mFile;
        private final int mIndex;

        FileEntry(int index, InstallationFile file) {
            this.mIndex = index;
            this.mFile = file;
        }

        int getIndex() {
            return this.mIndex;
        }

        InstallationFile getFile() {
            return this.mFile;
        }

        public boolean equals(Object obj) {
            if (obj instanceof FileEntry) {
                FileEntry rhs = (FileEntry) obj;
                return this.mFile.getLocation() == rhs.mFile.getLocation() && TextUtils.equals(this.mFile.getName(), rhs.mFile.getName());
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(Integer.valueOf(this.mFile.getLocation()), this.mFile.getName());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class PerFileChecksum {
        private final Checksum[] mChecksums;
        private final byte[] mSignature;

        PerFileChecksum(Checksum[] checksums, byte[] signature) {
            this.mChecksums = checksums;
            this.mSignature = signature;
        }

        Checksum[] getChecksums() {
            return this.mChecksums;
        }

        byte[] getSignature() {
            return this.mSignature;
        }
    }

    /* loaded from: classes2.dex */
    public class StagedSession implements StagingManager.StagedSession {
        public StagedSession() {
        }

        @Override // com.android.server.pm.StagingManager.StagedSession
        public List<StagingManager.StagedSession> getChildSessions() {
            List<StagingManager.StagedSession> childSessions;
            if (!PackageInstallerSession.this.params.isMultiPackage) {
                return Collections.EMPTY_LIST;
            }
            synchronized (PackageInstallerSession.this.mLock) {
                int size = PackageInstallerSession.this.mChildSessions.size();
                childSessions = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    childSessions.add(((PackageInstallerSession) PackageInstallerSession.this.mChildSessions.valueAt(i)).mStagedSession);
                }
            }
            return childSessions;
        }

        @Override // com.android.server.pm.StagingManager.StagedSession
        public PackageInstaller.SessionParams sessionParams() {
            return PackageInstallerSession.this.params;
        }

        @Override // com.android.server.pm.StagingManager.StagedSession
        public boolean isMultiPackage() {
            return PackageInstallerSession.this.params.isMultiPackage;
        }

        @Override // com.android.server.pm.StagingManager.StagedSession
        public boolean isApexSession() {
            return (PackageInstallerSession.this.params.installFlags & 131072) != 0;
        }

        @Override // com.android.server.pm.StagingManager.StagedSession
        public int sessionId() {
            return PackageInstallerSession.this.sessionId;
        }

        @Override // com.android.server.pm.StagingManager.StagedSession
        public boolean containsApexSession() {
            return sessionContains(new Predicate() { // from class: com.android.server.pm.PackageInstallerSession$StagedSession$$ExternalSyntheticLambda0
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    boolean isApexSession;
                    isApexSession = ((StagingManager.StagedSession) obj).isApexSession();
                    return isApexSession;
                }
            });
        }

        @Override // com.android.server.pm.StagingManager.StagedSession
        public String getPackageName() {
            return PackageInstallerSession.this.getPackageName();
        }

        @Override // com.android.server.pm.StagingManager.StagedSession
        public void setSessionReady() {
            PackageInstallerSession.this.setSessionReady();
        }

        @Override // com.android.server.pm.StagingManager.StagedSession
        public void setSessionFailed(int errorCode, String errorMessage) {
            PackageInstallerSession.this.setSessionFailed(errorCode, errorMessage);
        }

        @Override // com.android.server.pm.StagingManager.StagedSession
        public void setSessionApplied() {
            PackageInstallerSession.this.setSessionApplied();
        }

        @Override // com.android.server.pm.StagingManager.StagedSession
        public boolean containsApkSession() {
            return PackageInstallerSession.this.containsApkSession();
        }

        @Override // com.android.server.pm.StagingManager.StagedSession
        public CompletableFuture<Void> installSession() {
            PackageInstallerSession.this.assertCallerIsOwnerOrRootOrSystem();
            PackageInstallerSession.this.assertNotChild("StagedSession#installSession");
            Preconditions.checkArgument(isCommitted() && isSessionReady());
            return PackageInstallerSession.this.install();
        }

        @Override // com.android.server.pm.StagingManager.StagedSession
        public boolean hasParentSessionId() {
            return PackageInstallerSession.this.hasParentSessionId();
        }

        @Override // com.android.server.pm.StagingManager.StagedSession
        public int getParentSessionId() {
            return PackageInstallerSession.this.getParentSessionId();
        }

        @Override // com.android.server.pm.StagingManager.StagedSession
        public boolean isCommitted() {
            return PackageInstallerSession.this.isCommitted();
        }

        @Override // com.android.server.pm.StagingManager.StagedSession
        public boolean isInTerminalState() {
            return PackageInstallerSession.this.isInTerminalState();
        }

        @Override // com.android.server.pm.StagingManager.StagedSession
        public boolean isDestroyed() {
            return PackageInstallerSession.this.isDestroyed();
        }

        @Override // com.android.server.pm.StagingManager.StagedSession
        public long getCommittedMillis() {
            return PackageInstallerSession.this.getCommittedMillis();
        }

        @Override // com.android.server.pm.StagingManager.StagedSession
        public boolean sessionContains(final Predicate<StagingManager.StagedSession> filter) {
            return PackageInstallerSession.this.sessionContains(new Predicate() { // from class: com.android.server.pm.PackageInstallerSession$StagedSession$$ExternalSyntheticLambda1
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    boolean test;
                    test = filter.test(((PackageInstallerSession) obj).mStagedSession);
                    return test;
                }
            });
        }

        @Override // com.android.server.pm.StagingManager.StagedSession
        public boolean isSessionReady() {
            return PackageInstallerSession.this.isSessionReady();
        }

        @Override // com.android.server.pm.StagingManager.StagedSession
        public boolean isSessionApplied() {
            return PackageInstallerSession.this.isSessionApplied();
        }

        @Override // com.android.server.pm.StagingManager.StagedSession
        public boolean isSessionFailed() {
            return PackageInstallerSession.this.isSessionFailed();
        }

        @Override // com.android.server.pm.StagingManager.StagedSession
        public void abandon() {
            PackageInstallerSession.this.abandon();
        }

        @Override // com.android.server.pm.StagingManager.StagedSession
        public void verifySession() {
            PackageInstallerSession.this.assertCallerIsOwnerOrRootOrSystem();
            Preconditions.checkArgument(isCommitted());
            Preconditions.checkArgument(!isInTerminalState());
            PackageInstallerSession.this.verify();
        }
    }

    static boolean isDataLoaderInstallation(PackageInstaller.SessionParams params) {
        return params.dataLoaderParams != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isSystemDataLoaderInstallation(PackageInstaller.SessionParams params) {
        if (!isDataLoaderInstallation(params)) {
            return false;
        }
        return "android".equals(params.dataLoaderParams.getComponentName().getPackageName());
    }

    private boolean isDataLoaderInstallation() {
        return isDataLoaderInstallation(this.params);
    }

    private boolean isStreamingInstallation() {
        return isDataLoaderInstallation() && this.params.dataLoaderParams.getType() == 1;
    }

    private boolean isIncrementalInstallation() {
        return isDataLoaderInstallation() && this.params.dataLoaderParams.getType() == 2;
    }

    private boolean isSystemDataLoaderInstallation() {
        return isSystemDataLoaderInstallation(this.params);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isInstallerDeviceOwnerOrAffiliatedProfileOwner() {
        DevicePolicyManagerInternal dpmi;
        assertNotLocked("isInstallerDeviceOwnerOrAffiliatedProfileOwner");
        assertSealed("isInstallerDeviceOwnerOrAffiliatedProfileOwner");
        return this.userId == UserHandle.getUserId(this.mInstallerUid) && (dpmi = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class)) != null && dpmi.canSilentlyInstallPackage(getInstallSource().installerPackageName, this.mInstallerUid);
    }

    private int computeUserActionRequirement() {
        InstallSourceInfo existingInstallSourceInfo;
        synchronized (this.mLock) {
            if (this.mPermissionsManuallyAccepted) {
                return 0;
            }
            String packageName = this.mPackageName;
            boolean hasDeviceAdminReceiver = this.mHasDeviceAdminReceiver;
            boolean forcePermissionPrompt = (this.params.installFlags & 1024) != 0 || this.params.requireUserAction == 1;
            if (forcePermissionPrompt) {
                return 1;
            }
            Computer snapshot = this.mPm.snapshotComputer();
            boolean isInstallPermissionGranted = snapshot.checkUidPermission("android.permission.INSTALL_PACKAGES", this.mInstallerUid) == 0;
            boolean isSelfUpdatePermissionGranted = snapshot.checkUidPermission("android.permission.INSTALL_SELF_UPDATES", this.mInstallerUid) == 0;
            boolean isUpdatePermissionGranted = snapshot.checkUidPermission("android.permission.INSTALL_PACKAGE_UPDATES", this.mInstallerUid) == 0;
            boolean isUpdateWithoutUserActionPermissionGranted = snapshot.checkUidPermission("android.permission.UPDATE_PACKAGES_WITHOUT_USER_ACTION", this.mInstallerUid) == 0;
            boolean isInstallDpcPackagesPermissionGranted = snapshot.checkUidPermission("android.permission.INSTALL_DPC_PACKAGES", this.mInstallerUid) == 0;
            int targetPackageUid = snapshot.getPackageUid(packageName, 0L, this.userId);
            boolean isUpdate = targetPackageUid != -1 || isApexSession();
            String existingInstallerPackageName = null;
            if (isUpdate) {
                existingInstallSourceInfo = snapshot.getInstallSourceInfo(packageName);
            } else {
                existingInstallSourceInfo = null;
            }
            if (existingInstallSourceInfo != null) {
                existingInstallerPackageName = existingInstallSourceInfo.getInstallingPackageName();
            }
            boolean isInstallerOfRecord = isUpdate && Objects.equals(existingInstallerPackageName, getInstallerPackageName());
            boolean isSelfUpdate = targetPackageUid == this.mInstallerUid;
            boolean isPermissionGranted = isInstallPermissionGranted || (isUpdatePermissionGranted && isUpdate) || ((isSelfUpdatePermissionGranted && isSelfUpdate) || (isInstallDpcPackagesPermissionGranted && hasDeviceAdminReceiver));
            boolean isInstallerRoot = this.mInstallerUid == 0;
            boolean isInstallerSystem = this.mInstallerUid == 1000;
            boolean noUserActionNecessary = isPermissionGranted || isInstallerRoot || isInstallerSystem || isInstallerDeviceOwnerOrAffiliatedProfileOwner();
            if (!noUserActionNecessary) {
                if (!snapshot.isInstallDisabledForPackage(getInstallerPackageName(), this.mInstallerUid, this.userId) && this.params.requireUserAction == 2 && isUpdateWithoutUserActionPermissionGranted) {
                    return (isInstallerOfRecord || isSelfUpdate) ? 2 : 1;
                }
                return 1;
            }
            return 0;
        }
    }

    public PackageInstallerSession(PackageInstallerService.InternalCallback callback, Context context, PackageManagerService pm, PackageSessionProvider sessionProvider, SilentUpdatePolicy silentUpdatePolicy, Looper looper, StagingManager stagingManager, int sessionId, int userId, int installerUid, InstallSource installSource, PackageInstaller.SessionParams params, long createdMillis, long committedMillis, File stageDir, String stageCid, InstallationFile[] files, ArrayMap<String, PerFileChecksum> checksums, boolean prepared, boolean committed, boolean destroyed, boolean sealed, int[] childSessionIds, int parentSessionId, boolean isReady, boolean isFailed, boolean isApplied, int sessionErrorCode, String sessionErrorMessage) {
        this.mPrepared = false;
        this.mShouldBeSealed = false;
        this.mSessionErrorCode = 0;
        this.mDestroyed = false;
        Handler.Callback callback2 = new Handler.Callback() { // from class: com.android.server.pm.PackageInstallerSession.4
            @Override // android.os.Handler.Callback
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        PackageInstallerSession.this.handleSessionSealed();
                        break;
                    case 2:
                        PackageInstallerSession.this.handleStreamValidateAndCommit();
                        break;
                    case 3:
                        PackageInstallerSession.this.handleInstall();
                        break;
                    case 4:
                        SomeArgs args = (SomeArgs) msg.obj;
                        String packageName = (String) args.arg1;
                        String message = (String) args.arg2;
                        Bundle extras = (Bundle) args.arg3;
                        IntentSender statusReceiver = (IntentSender) args.arg4;
                        int returnCode = args.argi1;
                        args.recycle();
                        PackageInstallerSession.sendOnPackageInstalled(PackageInstallerSession.this.mContext, statusReceiver, PackageInstallerSession.this.sessionId, PackageInstallerSession.this.isInstallerDeviceOwnerOrAffiliatedProfileOwner() && ITranPackageManagerService.Instance().shouldShowNotificationWhenPackageInstalled(PackageInstallerSession.this.getInstallerPackageName(), packageName), PackageInstallerSession.this.userId, packageName, returnCode, message, extras);
                        ITranPackageManagerService.Instance().handlerPISessionInstalled(PackageInstallerSession.this.mPackageLite, PackageInstallerSession.this.params, PackageInstallerSession.this.sessionId, PackageInstallerSession.this.getInstallSource() != null ? PackageInstallerSession.this.getInstallSource().initiatingPackageName : "", PackageInstallerSession.this.getInstallSource() != null ? PackageInstallerSession.this.getInstallSource().originatingPackageName : "", packageName, PackageInstallerSession.this.mVersionCode, statusReceiver, returnCode, message);
                        break;
                    case 5:
                        int error = msg.arg1;
                        String detailMessage = (String) msg.obj;
                        PackageInstallerSession.this.onSessionValidationFailure(error, detailMessage);
                        break;
                }
                ITranPackageManagerService.Instance().handlerPackageInstallerSession(msg, PackageInstallerSession.this.sessionId);
                return true;
            }
        };
        this.mHandlerCallback = callback2;
        this.mCallback = callback;
        this.mContext = context;
        this.mPm = pm;
        this.mInstaller = pm != null ? pm.mInstaller : null;
        this.mSessionProvider = sessionProvider;
        this.mSilentUpdatePolicy = silentUpdatePolicy;
        this.mHandler = new Handler(looper, callback2);
        this.mStagingManager = stagingManager;
        this.sessionId = sessionId;
        this.userId = userId;
        this.mOriginalInstallerUid = installerUid;
        this.mInstallerUid = installerUid;
        InstallSource installSource2 = (InstallSource) Objects.requireNonNull(installSource);
        this.mInstallSource = installSource2;
        this.mOriginalInstallerPackageName = installSource2.installerPackageName;
        this.params = params;
        this.createdMillis = createdMillis;
        this.updatedMillis = createdMillis;
        this.committedMillis = committedMillis;
        this.stageDir = stageDir;
        this.stageCid = stageCid;
        this.mShouldBeSealed = sealed;
        if (childSessionIds != null) {
            int length = childSessionIds.length;
            int i = 0;
            while (i < length) {
                int i2 = length;
                int childSessionId = childSessionIds[i];
                this.mChildSessions.put(childSessionId, null);
                i++;
                length = i2;
            }
        }
        this.mParentSessionId = parentSessionId;
        if (files != null) {
            this.mFiles.ensureCapacity(files.length);
            int i3 = 0;
            int size = files.length;
            while (i3 < size) {
                InstallationFile file = files[i3];
                int size2 = size;
                if (this.mFiles.add(new FileEntry(i3, file))) {
                    i3++;
                    size = size2;
                } else {
                    throw new IllegalArgumentException("Trying to add a duplicate installation file");
                }
            }
        }
        if (checksums != null) {
            this.mChecksums.putAll((ArrayMap<? extends String, ? extends PerFileChecksum>) checksums);
        }
        if (!params.isMultiPackage) {
            if ((stageDir == null) == (stageCid == null)) {
                throw new IllegalArgumentException("Exactly one of stageDir or stageCid stage must be set");
            }
        }
        this.mPrepared = prepared;
        this.mCommitted.set(committed);
        this.mDestroyed = destroyed;
        this.mSessionReady = isReady;
        this.mSessionApplied = isApplied;
        this.mSessionFailed = isFailed;
        this.mSessionErrorCode = sessionErrorCode;
        this.mSessionErrorMessage = sessionErrorMessage != null ? sessionErrorMessage : "";
        this.mStagedSession = params.isStaged ? new StagedSession() : null;
        if (isDataLoaderInstallation()) {
            if (isApexSession()) {
                throw new IllegalArgumentException("DataLoader installation of APEX modules is not allowed.");
            }
            if (isSystemDataLoaderInstallation() && this.mContext.checkCallingOrSelfPermission("com.android.permission.USE_SYSTEM_DATA_LOADERS") != 0) {
                throw new SecurityException("You need the com.android.permission.USE_SYSTEM_DATA_LOADERS permission to use system data loaders");
            }
        }
        if (isIncrementalInstallation() && !IncrementalManager.isAllowed()) {
            throw new IllegalArgumentException("Incremental installation not allowed.");
        }
    }

    private boolean shouldScrubData(int callingUid) {
        return callingUid >= 10000 && getInstallerUid() != callingUid;
    }

    public PackageInstaller.SessionInfo generateInfoForCaller(boolean includeIcon, int callingUid) {
        return generateInfoInternal(includeIcon, shouldScrubData(callingUid));
    }

    public PackageInstaller.SessionInfo generateInfoScrubbed(boolean includeIcon) {
        return generateInfoInternal(includeIcon, true);
    }

    private PackageInstaller.SessionInfo generateInfoInternal(boolean includeIcon, boolean scrubData) {
        float progress;
        PackageInstaller.SessionInfo info = new PackageInstaller.SessionInfo();
        synchronized (this.mProgressLock) {
            progress = this.mProgress;
        }
        synchronized (this.mLock) {
            info.sessionId = this.sessionId;
            info.userId = this.userId;
            info.installerPackageName = this.mInstallSource.installerPackageName;
            info.installerAttributionTag = this.mInstallSource.installerAttributionTag;
            File file = this.mResolvedBaseFile;
            info.resolvedBaseCodePath = file != null ? file.getAbsolutePath() : null;
            info.progress = progress;
            info.sealed = this.mSealed;
            info.isCommitted = this.mCommitted.get();
            info.active = this.mActiveCount.get() > 0;
            info.mode = this.params.mode;
            info.installReason = this.params.installReason;
            info.installScenario = this.params.installScenario;
            info.sizeBytes = this.params.sizeBytes;
            String str = this.mPackageName;
            if (str == null) {
                str = this.params.appPackageName;
            }
            info.appPackageName = str;
            if (includeIcon) {
                info.appIcon = this.params.appIcon;
            }
            info.appLabel = this.params.appLabel;
            info.installLocation = this.params.installLocation;
            if (!scrubData) {
                info.originatingUri = this.params.originatingUri;
            }
            info.originatingUid = this.params.originatingUid;
            if (!scrubData) {
                info.referrerUri = this.params.referrerUri;
            }
            info.grantedRuntimePermissions = this.params.grantedRuntimePermissions;
            info.whitelistedRestrictedPermissions = this.params.whitelistedRestrictedPermissions;
            info.autoRevokePermissionsMode = this.params.autoRevokePermissionsMode;
            info.installFlags = this.params.installFlags;
            info.isMultiPackage = this.params.isMultiPackage;
            info.isStaged = this.params.isStaged;
            info.rollbackDataPolicy = this.params.rollbackDataPolicy;
            info.parentSessionId = this.mParentSessionId;
            info.childSessionIds = getChildSessionIdsLocked();
            info.isSessionApplied = this.mSessionApplied;
            info.isSessionReady = this.mSessionReady;
            info.isSessionFailed = this.mSessionFailed;
            info.setSessionErrorCode(this.mSessionErrorCode, this.mSessionErrorMessage);
            info.createdMillis = this.createdMillis;
            info.updatedMillis = this.updatedMillis;
            info.requireUserAction = this.params.requireUserAction;
            info.installerUid = this.mInstallerUid;
            info.packageSource = this.params.packageSource;
        }
        return info;
    }

    public boolean isPrepared() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mPrepared;
        }
        return z;
    }

    public boolean isSealed() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mSealed;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isCommitted() {
        return this.mCommitted.get();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDestroyed() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mDestroyed;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isInTerminalState() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mSessionApplied || this.mSessionFailed;
        }
        return z;
    }

    public boolean isStagedAndInTerminalState() {
        return this.params.isStaged && isInTerminalState();
    }

    private void assertNotLocked(String cookie) {
        if (Thread.holdsLock(this.mLock)) {
            throw new IllegalStateException(cookie + " is holding mLock");
        }
    }

    private void assertSealed(String cookie) {
        if (!isSealed()) {
            throw new IllegalStateException(cookie + " before sealing");
        }
    }

    private void assertPreparedAndNotSealedLocked(String cookie) {
        assertPreparedAndNotCommittedOrDestroyedLocked(cookie);
        if (this.mSealed) {
            throw new SecurityException(cookie + " not allowed after sealing");
        }
    }

    private void assertPreparedAndNotCommittedOrDestroyedLocked(String cookie) {
        assertPreparedAndNotDestroyedLocked(cookie);
        if (this.mCommitted.get()) {
            throw new SecurityException(cookie + " not allowed after commit");
        }
    }

    private void assertPreparedAndNotDestroyedLocked(String cookie) {
        if (!this.mPrepared) {
            throw new IllegalStateException(cookie + " before prepared");
        }
        if (this.mDestroyed) {
            throw new SecurityException(cookie + " not allowed after destruction");
        }
    }

    private void setClientProgressLocked(float progress) {
        boolean forcePublish = this.mClientProgress == 0.0f;
        this.mClientProgress = progress;
        computeProgressLocked(forcePublish);
    }

    public void setClientProgress(float progress) {
        assertCallerIsOwnerOrRoot();
        synchronized (this.mProgressLock) {
            setClientProgressLocked(progress);
        }
    }

    public void addClientProgress(float progress) {
        assertCallerIsOwnerOrRoot();
        synchronized (this.mProgressLock) {
            setClientProgressLocked(this.mClientProgress + progress);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void computeProgressLocked(boolean forcePublish) {
        if (!isIncrementalInstallation() || !this.mCommitted.get()) {
            this.mProgress = MathUtils.constrain(this.mClientProgress * 0.8f, 0.0f, 0.8f) + MathUtils.constrain(this.mInternalProgress * 0.2f, 0.0f, 0.2f);
        } else {
            float f = this.mIncrementalProgress;
            if (f - this.mProgress >= 0.01d) {
                this.mProgress = f;
            }
        }
        if (forcePublish || this.mProgress - this.mReportedProgress >= 0.01d) {
            float f2 = this.mProgress;
            this.mReportedProgress = f2;
            this.mCallback.onSessionProgressChanged(this, f2);
        }
    }

    public String[] getNames() {
        assertCallerIsOwnerRootOrVerifier();
        synchronized (this.mLock) {
            assertPreparedAndNotDestroyedLocked("getNames");
            if (!this.mCommitted.get()) {
                return getNamesLocked();
            }
            return getStageDirContentsLocked();
        }
    }

    private String[] getStageDirContentsLocked() {
        String[] result = this.stageDir.list();
        if (result == null) {
            return EmptyArray.STRING;
        }
        return result;
    }

    private String[] getNamesLocked() {
        if (!isDataLoaderInstallation()) {
            return getStageDirContentsLocked();
        }
        InstallationFile[] files = getInstallationFilesLocked();
        String[] result = new String[files.length];
        int size = files.length;
        for (int i = 0; i < size; i++) {
            result[i] = files[i].getName();
        }
        return result;
    }

    private InstallationFile[] getInstallationFilesLocked() {
        InstallationFile[] result = new InstallationFile[this.mFiles.size()];
        Iterator<FileEntry> it = this.mFiles.iterator();
        while (it.hasNext()) {
            FileEntry fileEntry = it.next();
            result[fileEntry.getIndex()] = fileEntry.getFile();
        }
        return result;
    }

    private static ArrayList<File> filterFiles(File parent, String[] names, FileFilter filter) {
        ArrayList<File> result = new ArrayList<>(names.length);
        for (String name : names) {
            File file = new File(parent, name);
            if (filter.accept(file)) {
                result.add(file);
            }
        }
        return result;
    }

    private List<File> getAddedApksLocked() {
        String[] names = getNamesLocked();
        return filterFiles(this.stageDir, names, sAddedApkFilter);
    }

    private List<File> getRemovedFilesLocked() {
        String[] names = getNamesLocked();
        return filterFiles(this.stageDir, names, sRemovedFilter);
    }

    public void setChecksums(String name, Checksum[] checksums, byte[] signature) {
        String installerPackageName;
        if (checksums.length == 0) {
            return;
        }
        if (!TextUtils.isEmpty(getInstallSource().initiatingPackageName)) {
            installerPackageName = getInstallSource().initiatingPackageName;
        } else {
            installerPackageName = getInstallSource().installerPackageName;
        }
        if (TextUtils.isEmpty(installerPackageName)) {
            throw new IllegalStateException("Installer package is empty.");
        }
        AppOpsManager appOps = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        appOps.checkPackage(Binder.getCallingUid(), installerPackageName);
        PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        AndroidPackage callingInstaller = pmi.getPackage(installerPackageName);
        if (callingInstaller == null) {
            throw new IllegalStateException("Can't obtain calling installer's package.");
        }
        if (signature != null && signature.length != 0) {
            try {
                ApkChecksums.verifySignature(checksums, signature);
            } catch (IOException | NoSuchAlgorithmException | SignatureException e) {
                throw new IllegalArgumentException("Can't verify signature", e);
            }
        }
        assertCallerIsOwnerOrRoot();
        synchronized (this.mLock) {
            assertPreparedAndNotCommittedOrDestroyedLocked("addChecksums");
            if (this.mChecksums.containsKey(name)) {
                throw new IllegalStateException("Duplicate checksums.");
            }
            this.mChecksums.put(name, new PerFileChecksum(checksums, signature));
        }
    }

    public void requestChecksums(String name, int optional, int required, List trustedInstallers, IOnChecksumsReadyListener onChecksumsReadyListener) {
        assertCallerIsOwnerRootOrVerifier();
        File file = new File(this.stageDir, name);
        String installerPackageName = getInstallSource().initiatingPackageName;
        try {
            this.mPm.requestFileChecksums(file, installerPackageName, optional, required, trustedInstallers, onChecksumsReadyListener);
        } catch (FileNotFoundException e) {
            throw new ParcelableException(e);
        }
    }

    public void removeSplit(String splitName) {
        if (isDataLoaderInstallation()) {
            throw new IllegalStateException("Cannot remove splits in a data loader installation session.");
        }
        if (TextUtils.isEmpty(this.params.appPackageName)) {
            throw new IllegalStateException("Must specify package name to remove a split");
        }
        assertCallerIsOwnerOrRoot();
        synchronized (this.mLock) {
            assertPreparedAndNotCommittedOrDestroyedLocked("removeSplit");
            try {
                createRemoveSplitMarkerLocked(splitName);
            } catch (IOException e) {
                throw ExceptionUtils.wrap(e);
            }
        }
    }

    private static String getRemoveMarkerName(String name) {
        String markerName = name + REMOVE_MARKER_EXTENSION;
        if (!FileUtils.isValidExtFilename(markerName)) {
            throw new IllegalArgumentException("Invalid marker: " + markerName);
        }
        return markerName;
    }

    private void createRemoveSplitMarkerLocked(String splitName) throws IOException {
        try {
            File target = new File(this.stageDir, getRemoveMarkerName(splitName));
            target.createNewFile();
            Os.chmod(target.getAbsolutePath(), 0);
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    private void assertShellOrSystemCalling(String operation) {
        switch (Binder.getCallingUid()) {
            case 0:
            case 1000:
            case 2000:
                return;
            default:
                throw new SecurityException(operation + " only supported from shell or system");
        }
    }

    private void assertCanWrite(boolean reverseMode) {
        if (isDataLoaderInstallation()) {
            throw new IllegalStateException("Cannot write regular files in a data loader installation session.");
        }
        assertCallerIsOwnerOrRoot();
        synchronized (this.mLock) {
            assertPreparedAndNotSealedLocked("assertCanWrite");
        }
        if (reverseMode) {
            assertShellOrSystemCalling("Reverse mode");
        }
    }

    public ParcelFileDescriptor openWrite(String name, long offsetBytes, long lengthBytes) {
        assertCanWrite(false);
        try {
            return doWriteInternal(name, offsetBytes, lengthBytes, null);
        } catch (IOException e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    public void write(String name, long offsetBytes, long lengthBytes, ParcelFileDescriptor fd) {
        assertCanWrite(fd != null);
        try {
            doWriteInternal(name, offsetBytes, lengthBytes, fd);
        } catch (IOException e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    public void stageViaHardLink(String path) {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 1000) {
            throw new SecurityException("link() can only be run by the system");
        }
        try {
            File target = new File(path);
            File source = new File(this.stageDir, target.getName());
            try {
                Os.link(path, source.getAbsolutePath());
                Os.chmod(source.getAbsolutePath(), FrameworkStatsLog.VBMETA_DIGEST_REPORTED);
            } catch (ErrnoException e) {
                e.rethrowAsIOException();
            }
            if (!SELinux.restorecon(source)) {
                throw new IOException("Can't relabel file: " + source);
            }
        } catch (IOException e2) {
            throw ExceptionUtils.wrap(e2);
        }
    }

    private ParcelFileDescriptor openTargetInternal(String path, int flags, int mode) throws IOException, ErrnoException {
        FileDescriptor fd = Os.open(path, flags, mode);
        return new ParcelFileDescriptor(fd);
    }

    private ParcelFileDescriptor createRevocableFdInternal(RevocableFileDescriptor fd, ParcelFileDescriptor pfd) throws IOException {
        int releasedFdInt = pfd.detachFd();
        FileDescriptor releasedFd = new FileDescriptor();
        releasedFd.setInt$(releasedFdInt);
        fd.init(this.mContext, releasedFd);
        return fd.getRevocableFileDescriptor();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1554=4] */
    /* JADX INFO: Access modifiers changed from: private */
    public ParcelFileDescriptor doWriteInternal(String name, long offsetBytes, long lengthBytes, ParcelFileDescriptor incomingFd) throws IOException {
        RevocableFileDescriptor fd;
        FileBridge bridge;
        ParcelFileDescriptor targetPfd;
        synchronized (this.mLock) {
            if (PackageInstaller.ENABLE_REVOCABLE_FD) {
                RevocableFileDescriptor fd2 = new RevocableFileDescriptor();
                this.mFds.add(fd2);
                fd = fd2;
                bridge = null;
            } else {
                FileBridge bridge2 = new FileBridge();
                this.mBridges.add(bridge2);
                fd = null;
                bridge = bridge2;
            }
        }
        try {
            if (!FileUtils.isValidExtFilename(name)) {
                throw new IllegalArgumentException("Invalid name: " + name);
            }
            long identity = Binder.clearCallingIdentity();
            File target = new File(this.stageDir, name);
            Binder.restoreCallingIdentity(identity);
            ParcelFileDescriptor targetPfd2 = openTargetInternal(target.getAbsolutePath(), OsConstants.O_CREAT | OsConstants.O_WRONLY, FrameworkStatsLog.VBMETA_DIGEST_REPORTED);
            Os.chmod(target.getAbsolutePath(), FrameworkStatsLog.VBMETA_DIGEST_REPORTED);
            if (this.stageDir != null && lengthBytes > 0) {
                ((StorageManager) this.mContext.getSystemService(StorageManager.class)).allocateBytes(targetPfd2.getFileDescriptor(), lengthBytes, InstallLocationUtils.translateAllocateFlags(this.params.installFlags));
            }
            if (offsetBytes > 0) {
                Os.lseek(targetPfd2.getFileDescriptor(), offsetBytes, OsConstants.SEEK_SET);
            }
            if (incomingFd == null) {
                if (PackageInstaller.ENABLE_REVOCABLE_FD) {
                    return createRevocableFdInternal(fd, targetPfd2);
                }
                bridge.setTargetFile(targetPfd2);
                bridge.start();
                return bridge.getClientSocket();
            }
            try {
                final Int64Ref last = new Int64Ref(0L);
                targetPfd = targetPfd2;
                try {
                    FileUtils.copy(incomingFd.getFileDescriptor(), targetPfd2.getFileDescriptor(), lengthBytes, null, new BatteryExternalStatsWorker$$ExternalSyntheticLambda4(), new FileUtils.ProgressListener() { // from class: com.android.server.pm.PackageInstallerSession$$ExternalSyntheticLambda6
                        @Override // android.os.FileUtils.ProgressListener
                        public final void onProgress(long j) {
                            PackageInstallerSession.this.m5505xee337a00(last, j);
                        }
                    });
                    IoUtils.closeQuietly(targetPfd);
                    IoUtils.closeQuietly(incomingFd);
                    synchronized (this.mLock) {
                        try {
                            if (PackageInstaller.ENABLE_REVOCABLE_FD) {
                                this.mFds.remove(fd);
                            } else {
                                bridge.forceClose();
                                this.mBridges.remove(bridge);
                            }
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                    return null;
                } catch (Throwable th2) {
                    th = th2;
                    IoUtils.closeQuietly(targetPfd);
                    IoUtils.closeQuietly(incomingFd);
                    synchronized (this.mLock) {
                        try {
                            if (PackageInstaller.ENABLE_REVOCABLE_FD) {
                                this.mFds.remove(fd);
                            } else {
                                bridge.forceClose();
                                this.mBridges.remove(bridge);
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            throw th;
                        }
                    }
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
                targetPfd = targetPfd2;
            }
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$doWriteInternal$0$com-android-server-pm-PackageInstallerSession  reason: not valid java name */
    public /* synthetic */ void m5505xee337a00(Int64Ref last, long progress) {
        if (this.params.sizeBytes > 0) {
            long delta = progress - last.value;
            last.value = progress;
            synchronized (this.mProgressLock) {
                setClientProgressLocked(this.mClientProgress + (((float) delta) / ((float) this.params.sizeBytes)));
            }
        }
    }

    public ParcelFileDescriptor openRead(String name) {
        ParcelFileDescriptor openReadInternalLocked;
        if (isDataLoaderInstallation()) {
            throw new IllegalStateException("Cannot read regular files in a data loader installation session.");
        }
        assertCallerIsOwnerOrRoot();
        synchronized (this.mLock) {
            assertPreparedAndNotCommittedOrDestroyedLocked("openRead");
            try {
                openReadInternalLocked = openReadInternalLocked(name);
            } catch (IOException e) {
                throw ExceptionUtils.wrap(e);
            }
        }
        return openReadInternalLocked;
    }

    private ParcelFileDescriptor openReadInternalLocked(String name) throws IOException {
        try {
            if (!FileUtils.isValidExtFilename(name)) {
                throw new IllegalArgumentException("Invalid name: " + name);
            }
            File target = new File(this.stageDir, name);
            FileDescriptor targetFd = Os.open(target.getAbsolutePath(), OsConstants.O_RDONLY, 0);
            return new ParcelFileDescriptor(targetFd);
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    private void assertCallerIsOwnerRootOrVerifier() {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 0 || callingUid == this.mInstallerUid) {
            return;
        }
        if (isSealed() && this.mContext.checkCallingOrSelfPermission("android.permission.PACKAGE_VERIFICATION_AGENT") == 0) {
            return;
        }
        throw new SecurityException("Session does not belong to uid " + callingUid);
    }

    private void assertCallerIsOwnerOrRoot() {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 0 && callingUid != this.mInstallerUid) {
            throw new SecurityException("Session does not belong to uid " + callingUid);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void assertCallerIsOwnerOrRootOrSystem() {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 0 && callingUid != this.mInstallerUid && callingUid != 1000) {
            throw new SecurityException("Session does not belong to uid " + callingUid);
        }
    }

    private void assertNoWriteFileTransfersOpenLocked() {
        Iterator<RevocableFileDescriptor> it = this.mFds.iterator();
        while (it.hasNext()) {
            RevocableFileDescriptor fd = it.next();
            if (!fd.isRevoked()) {
                throw new SecurityException("Files still open");
            }
        }
        Iterator<FileBridge> it2 = this.mBridges.iterator();
        while (it2.hasNext()) {
            FileBridge bridge = it2.next();
            if (!bridge.isClosed()) {
                throw new SecurityException("Files still open");
            }
        }
    }

    public void commit(IntentSender statusReceiver, boolean forTransfer) {
        if (Build.IS_DEBUG_ENABLE) {
            try {
                int testCallingUid = Binder.getCallingUid();
                int testPid = Binder.getCallingPid();
                Slog.i(TAG, "commit PackageInstallerSession,sessionId =" + this.sessionId + ",Calling uid =" + testCallingUid + ", pid =" + testPid, new Throwable());
            } catch (Exception e) {
                Slog.e(TAG, "Error: can't get pid before boot completed:" + e.toString());
            }
        }
        if (hasParentSessionId()) {
            throw new IllegalStateException("Session " + this.sessionId + " is a child of multi-package session " + getParentSessionId() + " and may not be committed directly.");
        }
        if (!markAsSealed(statusReceiver, forTransfer)) {
            return;
        }
        if (isMultiPackage()) {
            synchronized (this.mLock) {
                boolean sealFailed = false;
                for (int i = this.mChildSessions.size() - 1; i >= 0; i--) {
                    if (!this.mChildSessions.valueAt(i).markAsSealed(null, forTransfer)) {
                        sealFailed = true;
                    }
                }
                if (sealFailed) {
                    return;
                }
            }
        }
        dispatchSessionSealed();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchSessionSealed() {
        this.mHandler.obtainMessage(1).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSessionSealed() {
        assertSealed("dispatchSessionSealed");
        this.mCallback.onSessionSealedBlocking(this);
        dispatchStreamValidateAndCommit();
    }

    private void dispatchStreamValidateAndCommit() {
        this.mHandler.obtainMessage(2).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleStreamValidateAndCommit() {
        boolean allSessionsReady = true;
        try {
            for (PackageInstallerSession child : getChildSessions()) {
                allSessionsReady &= child.streamValidateAndCommit();
            }
            if (allSessionsReady && streamValidateAndCommit()) {
                this.mHandler.obtainMessage(3).sendToTarget();
            }
        } catch (PackageManagerException e) {
            destroy();
            String msg = ExceptionUtils.getCompleteMessage(e);
            dispatchSessionFinished(e.error, msg, null);
            maybeFinishChildSessions(e.error, msg);
        }
    }

    /* loaded from: classes2.dex */
    private final class FileSystemConnector extends IPackageInstallerSessionFileSystemConnector.Stub {
        final Set<String> mAddedFiles = new ArraySet();

        FileSystemConnector(List<InstallationFileParcel> addedFiles) {
            for (InstallationFileParcel file : addedFiles) {
                this.mAddedFiles.add(file.name);
            }
        }

        public void writeData(String name, long offsetBytes, long lengthBytes, ParcelFileDescriptor incomingFd) {
            if (incomingFd == null) {
                throw new IllegalArgumentException("incomingFd can't be null");
            }
            if (!this.mAddedFiles.contains(name)) {
                throw new SecurityException("File name is not in the list of added files.");
            }
            try {
                PackageInstallerSession.this.doWriteInternal(name, offsetBytes, lengthBytes, incomingFd);
            } catch (IOException e) {
                throw ExceptionUtils.wrap(e);
            }
        }
    }

    private static boolean isSecureFrpInstallAllowed(Context context, int callingUid) {
        PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        String[] systemInstaller = pmi.getKnownPackageNames(2, 0);
        AndroidPackage callingInstaller = pmi.getPackage(callingUid);
        return (callingInstaller == null || !ArrayUtils.contains(systemInstaller, callingInstaller.getPackageName())) && context.checkCallingOrSelfPermission("android.permission.INSTALL_PACKAGES") == 0;
    }

    private static boolean isIncrementalInstallationAllowed(String packageName) {
        PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        PackageStateInternal existingPkgSetting = pmi.getPackageStateInternal(packageName);
        if (existingPkgSetting == null || existingPkgSetting.getPkg() == null) {
            return true;
        }
        return (existingPkgSetting.getPkg().isSystem() || existingPkgSetting.getTransientState().isUpdatedSystemApp()) ? false : true;
    }

    private boolean markAsSealed(IntentSender statusReceiver, boolean forTransfer) {
        Preconditions.checkState(statusReceiver != null || hasParentSessionId(), "statusReceiver can't be null for the root session");
        assertCallerIsOwnerOrRoot();
        synchronized (this.mLock) {
            assertPreparedAndNotDestroyedLocked("commit of session " + this.sessionId);
            assertNoWriteFileTransfersOpenLocked();
            boolean isSecureFrpEnabled = Settings.Secure.getInt(this.mContext.getContentResolver(), "secure_frp_mode", 0) == 1;
            if (isSecureFrpEnabled && !isSecureFrpInstallAllowed(this.mContext, Binder.getCallingUid())) {
                boolean isAllowed = IPackageManagerServiceLice.Instance().isSecureFrpInstallAllowed(this.params, Binder.getCallingUid());
                if (!isAllowed) {
                    throw new SecurityException("Can't install packages while in secure FRP");
                }
            }
            if (forTransfer) {
                this.mContext.enforceCallingOrSelfPermission("android.permission.INSTALL_PACKAGES", null);
                if (this.mInstallerUid == this.mOriginalInstallerUid) {
                    throw new IllegalArgumentException("Session has not been transferred");
                }
            } else if (this.mInstallerUid != this.mOriginalInstallerUid) {
                throw new IllegalArgumentException("Session has been transferred");
            }
            setRemoteStatusReceiver(statusReceiver);
            if (this.mSealed) {
                return true;
            }
            try {
                sealLocked();
                return true;
            } catch (PackageManagerException e) {
                return false;
            }
        }
    }

    private boolean streamValidateAndCommit() throws PackageManagerException {
        try {
            synchronized (this.mLock) {
                if (this.mCommitted.get()) {
                    return true;
                }
                if (!this.params.isMultiPackage) {
                    if (!prepareDataLoaderLocked()) {
                        return false;
                    }
                    if (isApexSession()) {
                        validateApexInstallLocked();
                    } else {
                        validateApkInstallLocked();
                    }
                }
                if (this.mDestroyed) {
                    throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Session destroyed");
                }
                if (!isIncrementalInstallation()) {
                    synchronized (this.mProgressLock) {
                        this.mClientProgress = 1.0f;
                        computeProgressLocked(true);
                    }
                }
                this.mActiveCount.incrementAndGet();
                if (!this.mCommitted.compareAndSet(false, true)) {
                    throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, TextUtils.formatSimple("The mCommitted of session %d should be false originally", new Object[]{Integer.valueOf(this.sessionId)}));
                }
                this.committedMillis = System.currentTimeMillis();
                return true;
            }
        } catch (PackageManagerException e) {
            throw e;
        } catch (Throwable e2) {
            throw new PackageManagerException(e2);
        }
    }

    private List<PackageInstallerSession> getChildSessionsLocked() {
        List<PackageInstallerSession> childSessions = Collections.EMPTY_LIST;
        if (isMultiPackage()) {
            int size = this.mChildSessions.size();
            childSessions = new ArrayList(size);
            for (int i = 0; i < size; i++) {
                childSessions.add(this.mChildSessions.valueAt(i));
            }
        }
        return childSessions;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<PackageInstallerSession> getChildSessions() {
        List<PackageInstallerSession> childSessionsLocked;
        synchronized (this.mLock) {
            childSessionsLocked = getChildSessionsLocked();
        }
        return childSessionsLocked;
    }

    private void sealLocked() throws PackageManagerException {
        try {
            assertNoWriteFileTransfersOpenLocked();
            assertPreparedAndNotDestroyedLocked("sealing of session " + this.sessionId);
            this.mSealed = true;
        } catch (Throwable e) {
            throw onSessionValidationFailure(new PackageManagerException(e));
        }
    }

    private PackageManagerException onSessionValidationFailure(PackageManagerException e) {
        onSessionValidationFailure(e.error, ExceptionUtils.getCompleteMessage(e));
        return e;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSessionValidationFailure(int error, String detailMessage) {
        destroyInternal();
        dispatchSessionFinished(error, detailMessage, null);
    }

    private void onSessionVerificationFailure(int error, String msg) {
        Slog.e(TAG, "Failed to verify session " + this.sessionId);
        dispatchSessionFinished(error, msg, null);
        maybeFinishChildSessions(error, msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSystemDataLoaderUnrecoverable() {
        final DeletePackageHelper deletePackageHelper = new DeletePackageHelper(this.mPm);
        final String packageName = getPackageName();
        if (TextUtils.isEmpty(packageName)) {
            return;
        }
        this.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageInstallerSession$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                PackageInstallerSession.lambda$onSystemDataLoaderUnrecoverable$1(DeletePackageHelper.this, packageName);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$onSystemDataLoaderUnrecoverable$1(DeletePackageHelper deletePackageHelper, String packageName) {
        if (deletePackageHelper.deletePackageX(packageName, -1L, 0, 2, true) != 1) {
            Slog.e(TAG, "Failed to uninstall package with failed dataloader: " + packageName);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAfterSessionRead(SparseArray<PackageInstallerSession> allSessions) {
        PackageInstallerSession root;
        synchronized (this.mLock) {
            for (int i = this.mChildSessions.size() - 1; i >= 0; i--) {
                int childSessionId = this.mChildSessions.keyAt(i);
                PackageInstallerSession childSession = allSessions.get(childSessionId);
                if (childSession != null) {
                    this.mChildSessions.setValueAt(i, childSession);
                } else {
                    Slog.e(TAG, "Child session not existed: " + childSessionId);
                    this.mChildSessions.removeAt(i);
                }
            }
            if (!this.mShouldBeSealed || isStagedAndInTerminalState()) {
                return;
            }
            try {
                sealLocked();
            } catch (PackageManagerException e) {
                Slog.e(TAG, "Package not valid", e);
            }
            if (!isMultiPackage() && isStaged() && isCommitted()) {
                if (hasParentSessionId()) {
                    root = allSessions.get(getParentSessionId());
                } else {
                    root = this;
                }
                if (root != null && !root.isStagedAndInTerminalState()) {
                    if (isApexSession()) {
                        validateApexInstallLocked();
                    } else {
                        validateApkInstallLocked();
                    }
                }
            }
        }
    }

    public void markUpdated() {
        synchronized (this.mLock) {
            this.updatedMillis = System.currentTimeMillis();
        }
    }

    public void transfer(String packageName) {
        Preconditions.checkArgument(!TextUtils.isEmpty(packageName));
        Computer snapshot = this.mPm.snapshotComputer();
        ApplicationInfo newOwnerAppInfo = snapshot.getApplicationInfo(packageName, 0L, this.userId);
        if (newOwnerAppInfo == null) {
            throw new ParcelableException(new PackageManager.NameNotFoundException(packageName));
        }
        if (snapshot.checkUidPermission("android.permission.INSTALL_PACKAGES", newOwnerAppInfo.uid) != 0) {
            throw new SecurityException("Destination package " + packageName + " does not have the android.permission.INSTALL_PACKAGES permission");
        }
        if (!this.params.areHiddenOptionsSet()) {
            throw new SecurityException("Can only transfer sessions that use public options");
        }
        synchronized (this.mLock) {
            assertCallerIsOwnerOrRoot();
            assertPreparedAndNotSealedLocked("transfer");
            try {
                sealLocked();
                this.mInstallerUid = newOwnerAppInfo.uid;
                this.mInstallSource = InstallSource.create(packageName, null, packageName, null, this.params.packageSource);
            } catch (PackageManagerException e) {
                throw new IllegalStateException("Package is not valid", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean checkUserActionRequirement(PackageInstallerSession session, IntentSender target) {
        int validatedTargetSdk;
        if (session.isMultiPackage()) {
            return false;
        }
        int userActionRequirement = session.computeUserActionRequirement();
        if (userActionRequirement == 1) {
            session.sendPendingUserActionIntent(target);
            return true;
        }
        if (!session.isApexSession() && userActionRequirement == 2) {
            synchronized (session.mLock) {
                validatedTargetSdk = session.mValidatedTargetSdk;
            }
            if (validatedTargetSdk != Integer.MAX_VALUE && validatedTargetSdk < 30) {
                session.sendPendingUserActionIntent(target);
                return true;
            } else if (session.params.requireUserAction == 2) {
                if (!session.mSilentUpdatePolicy.isSilentUpdateAllowed(session.getInstallerPackageName(), session.getPackageName())) {
                    session.sendPendingUserActionIntent(target);
                    return true;
                }
                session.mSilentUpdatePolicy.track(session.getInstallerPackageName(), session.getPackageName());
            }
        }
        return false;
    }

    private boolean sendPendingUserActionIntentIfNeeded() {
        assertNotChild("PackageInstallerSession#sendPendingUserActionIntentIfNeeded");
        final IntentSender statusReceiver = getRemoteStatusReceiver();
        return sessionContains(new Predicate() { // from class: com.android.server.pm.PackageInstallerSession$$ExternalSyntheticLambda7
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean checkUserActionRequirement;
                checkUserActionRequirement = PackageInstallerSession.checkUserActionRequirement((PackageInstallerSession) obj, statusReceiver);
                return checkUserActionRequirement;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleInstall() {
        if (isInstallerDeviceOwnerOrAffiliatedProfileOwner()) {
            DevicePolicyEventLogger.createEvent(112).setAdmin(getInstallSource().installerPackageName).write();
        }
        if (sendPendingUserActionIntentIfNeeded()) {
            return;
        }
        if (this.params.isStaged) {
            this.mStagedSession.verifySession();
        } else {
            verify();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void verify() {
        try {
            List<PackageInstallerSession> children = getChildSessions();
            if (isMultiPackage()) {
                for (PackageInstallerSession child : children) {
                    child.prepareInheritedFiles();
                    child.parseApkAndExtractNativeLibraries();
                }
            } else {
                prepareInheritedFiles();
                parseApkAndExtractNativeLibraries();
            }
            verifyNonStaged();
        } catch (PackageManagerException e) {
            String completeMsg = ExceptionUtils.getCompleteMessage(e);
            String errorMsg = PackageManager.installStatusToString(e.error, completeMsg);
            setSessionFailed(e.error, errorMsg);
            onSessionVerificationFailure(e.error, errorMsg);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public IntentSender getRemoteStatusReceiver() {
        IntentSender intentSender;
        synchronized (this.mLock) {
            intentSender = this.mRemoteStatusReceiver;
        }
        return intentSender;
    }

    private void setRemoteStatusReceiver(IntentSender remoteStatusReceiver) {
        synchronized (this.mLock) {
            this.mRemoteStatusReceiver = remoteStatusReceiver;
        }
    }

    private void prepareInheritedFiles() throws PackageManagerException {
        if (isApexSession() || this.params.mode != 2) {
            return;
        }
        synchronized (this.mLock) {
            if (this.mStageDirInUse) {
                throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Session files in use");
            }
            if (this.mDestroyed) {
                throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Session destroyed");
            }
            if (!this.mSealed) {
                throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Session not sealed");
            }
            try {
                List<File> fromFiles = this.mResolvedInheritedFiles;
                File toDir = this.stageDir;
                String tempPackageName = toDir.getName();
                Slog.d(TAG, "Inherited files: " + this.mResolvedInheritedFiles);
                if (!this.mResolvedInheritedFiles.isEmpty() && this.mInheritedFilesBase == null) {
                    throw new IllegalStateException("mInheritedFilesBase == null");
                }
                if (isLinkPossible(fromFiles, toDir)) {
                    if (!this.mResolvedInstructionSets.isEmpty()) {
                        File oatDir = new File(toDir, "oat");
                        createOatDirs(tempPackageName, this.mResolvedInstructionSets, oatDir);
                    }
                    if (!this.mResolvedNativeLibPaths.isEmpty()) {
                        for (String libPath : this.mResolvedNativeLibPaths) {
                            int splitIndex = libPath.lastIndexOf(47);
                            if (splitIndex >= 0 && splitIndex < libPath.length() - 1) {
                                String libDirPath = libPath.substring(1, splitIndex);
                                File libDir = new File(toDir, libDirPath);
                                if (!libDir.exists()) {
                                    NativeLibraryHelper.createNativeLibrarySubdir(libDir);
                                }
                                String archDirPath = libPath.substring(splitIndex + 1);
                                NativeLibraryHelper.createNativeLibrarySubdir(new File(libDir, archDirPath));
                            }
                            Slog.e(TAG, "Skipping native library creation for linking due to invalid path: " + libPath);
                        }
                    }
                    linkFiles(tempPackageName, fromFiles, toDir, this.mInheritedFilesBase);
                } else {
                    copyFiles(fromFiles, toDir);
                }
            } catch (IOException e) {
                throw new PackageManagerException(-4, "Failed to inherit existing install", e);
            }
        }
    }

    private void markStageDirInUseLocked() throws PackageManagerException {
        if (this.mDestroyed) {
            throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Session destroyed");
        }
        this.mStageDirInUse = true;
    }

    private void parseApkAndExtractNativeLibraries() throws PackageManagerException {
        PackageLite result;
        synchronized (this.mLock) {
            if (this.mStageDirInUse) {
                throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Session files in use");
            }
            if (this.mDestroyed) {
                throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Session destroyed");
            }
            if (!this.mSealed) {
                throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Session not sealed");
            }
            Objects.requireNonNull(this.mPackageName);
            Objects.requireNonNull(this.mSigningDetails);
            Objects.requireNonNull(this.mResolvedBaseFile);
            if (!isApexSession()) {
                result = getOrParsePackageLiteLocked(this.stageDir, 0);
            } else {
                result = getOrParsePackageLiteLocked(this.mResolvedBaseFile, 0);
            }
            if (result != null) {
                this.mPackageLite = result;
                if (!isApexSession()) {
                    synchronized (this.mProgressLock) {
                        this.mInternalProgress = 0.5f;
                        computeProgressLocked(true);
                    }
                    extractNativeLibraries(this.mPackageLite, this.stageDir, this.params.abiOverride, mayInheritNativeLibs());
                }
            }
        }
    }

    private void verifyNonStaged() throws PackageManagerException {
        synchronized (this.mLock) {
            markStageDirInUseLocked();
        }
        this.mSessionProvider.getSessionVerifier().verify(this, new PackageSessionVerifier.Callback() { // from class: com.android.server.pm.PackageInstallerSession$$ExternalSyntheticLambda0
            @Override // com.android.server.pm.PackageSessionVerifier.Callback
            public final void onResult(int i, String str) {
                PackageInstallerSession.this.m5508xb95b21ed(i, str);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$verifyNonStaged$4$com-android-server-pm-PackageInstallerSession  reason: not valid java name */
    public /* synthetic */ void m5508xb95b21ed(final int error, final String msg) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageInstallerSession$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                PackageInstallerSession.this.m5507x43e0fbac(error, msg);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$verifyNonStaged$3$com-android-server-pm-PackageInstallerSession  reason: not valid java name */
    public /* synthetic */ void m5507x43e0fbac(int error, String msg) {
        if (dispatchPendingAbandonCallback()) {
            return;
        }
        if (error == 1) {
            onVerificationComplete();
        } else {
            onSessionVerificationFailure(error, msg);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class InstallResult {
        public final Bundle extras;
        public final PackageInstallerSession session;

        InstallResult(PackageInstallerSession session, Bundle extras) {
            this.session = session;
            this.extras = extras;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public CompletableFuture<Void> install() {
        final List<CompletableFuture<InstallResult>> futures = installNonStaged();
        CompletableFuture<InstallResult>[] arr = new CompletableFuture[futures.size()];
        return CompletableFuture.allOf((CompletableFuture[]) futures.toArray(arr)).whenComplete(new BiConsumer() { // from class: com.android.server.pm.PackageInstallerSession$$ExternalSyntheticLambda3
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                PackageInstallerSession.this.m5506lambda$install$5$comandroidserverpmPackageInstallerSession(futures, (Void) obj, (Throwable) obj2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$install$5$com-android-server-pm-PackageInstallerSession  reason: not valid java name */
    public /* synthetic */ void m5506lambda$install$5$comandroidserverpmPackageInstallerSession(List futures, Void r, Throwable t) {
        if (t == null) {
            setSessionApplied();
            Iterator it = futures.iterator();
            while (it.hasNext()) {
                CompletableFuture<InstallResult> f = (CompletableFuture) it.next();
                InstallResult result = f.join();
                result.session.dispatchSessionFinished(1, "Session installed", result.extras);
            }
            return;
        }
        PackageManagerException e = (PackageManagerException) t.getCause();
        setSessionFailed(e.error, PackageManager.installStatusToString(e.error, e.getMessage()));
        dispatchSessionFinished(e.error, e.getMessage(), null);
        maybeFinishChildSessions(e.error, e.getMessage());
    }

    private List<CompletableFuture<InstallResult>> installNonStaged() {
        try {
            List<CompletableFuture<InstallResult>> futures = new ArrayList<>();
            CompletableFuture<InstallResult> future = new CompletableFuture<>();
            futures.add(future);
            InstallParams installingSession = makeInstallParams(future);
            if (isMultiPackage()) {
                List<PackageInstallerSession> childSessions = getChildSessions();
                List<InstallParams> installingChildSessions = new ArrayList<>(childSessions.size());
                for (int i = 0; i < childSessions.size(); i++) {
                    PackageInstallerSession session = childSessions.get(i);
                    CompletableFuture<InstallResult> future2 = new CompletableFuture<>();
                    futures.add(future2);
                    InstallParams installingChildSession = session.makeInstallParams(future2);
                    if (installingChildSession != null) {
                        installingChildSessions.add(installingChildSession);
                    }
                }
                if (!installingChildSessions.isEmpty()) {
                    installingSession.installStage(installingChildSessions);
                }
            } else if (installingSession != null) {
                installingSession.installStage();
                return futures;
            }
            return futures;
        } catch (PackageManagerException e) {
            List<CompletableFuture<InstallResult>> futures2 = new ArrayList<>();
            futures2.add(CompletableFuture.failedFuture(e));
            return futures2;
        }
    }

    private void sendPendingUserActionIntent(IntentSender target) {
        Intent intent = new Intent("android.content.pm.action.CONFIRM_INSTALL");
        intent.setPackage(this.mPm.getPackageInstallerPackageName());
        intent.putExtra("android.content.pm.extra.SESSION_ID", this.sessionId);
        sendOnUserActionRequired(this.mContext, target, this.sessionId, intent);
        closeInternal(false);
    }

    private void onVerificationComplete() {
        if (isStaged()) {
            this.mStagingManager.commitSession(this.mStagedSession);
            sendUpdateToRemoteStatusReceiver(1, "Session staged", null);
            return;
        }
        install();
    }

    private InstallParams makeInstallParams(final CompletableFuture<InstallResult> future) throws PackageManagerException {
        UserHandle user;
        InstallParams installParams;
        synchronized (this.mLock) {
            if (!this.mSealed) {
                throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Session not sealed");
            }
            markStageDirInUseLocked();
        }
        if (isMultiPackage()) {
            future.complete(new InstallResult(this, null));
        } else if (isApexSession() && this.params.isStaged) {
            future.complete(new InstallResult(this, null));
            return null;
        }
        IPackageInstallObserver2.Stub stub = new IPackageInstallObserver2.Stub() { // from class: com.android.server.pm.PackageInstallerSession.5
            public void onUserActionRequired(Intent intent) {
                throw new IllegalStateException();
            }

            public void onPackageInstalled(String basePackageName, int returnCode, String msg, Bundle extras) {
                if (returnCode == 1) {
                    future.complete(new InstallResult(PackageInstallerSession.this, extras));
                } else {
                    future.completeExceptionally(new PackageManagerException(returnCode, msg));
                }
            }
        };
        if ((this.params.installFlags & 64) != 0) {
            user = UserHandle.ALL;
        } else {
            user = new UserHandle(this.userId);
        }
        if (this.params.isStaged) {
            this.params.installFlags |= 2097152;
        }
        if (!isMultiPackage() && !isApexSession()) {
            synchronized (this.mLock) {
                if (this.mPackageLite == null) {
                    Slog.wtf(TAG, "Session: " + this.sessionId + ". Don't have a valid PackageLite.");
                }
                this.mPackageLite = getOrParsePackageLiteLocked(this.stageDir, 0);
            }
        }
        synchronized (this.mLock) {
            installParams = new InstallParams(this.stageDir, stub, this.params, this.mInstallSource, user, this.mSigningDetails, this.mInstallerUid, this.mPackageLite, this.mPm);
        }
        return installParams;
    }

    private PackageLite getOrParsePackageLiteLocked(File packageFile, int flags) throws PackageManagerException {
        PackageLite packageLite = this.mPackageLite;
        if (packageLite != null) {
            return packageLite;
        }
        ParseTypeImpl input = ParseTypeImpl.forDefaultParsing();
        ParseResult<PackageLite> result = ApkLiteParseUtils.parsePackageLite(input, packageFile, flags);
        if (result.isError()) {
            throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, result.getErrorMessage(), result.getException());
        }
        return (PackageLite) result.getResult();
    }

    private static void maybeRenameFile(File from, File to) throws PackageManagerException {
        if (!from.equals(to) && !from.renameTo(to)) {
            throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Could not rename file " + from + " to " + to);
        }
    }

    private void logDataLoaderInstallationSession(int returnCode) {
        int packageUid;
        String packageName = getPackageName();
        String packageNameToLog = (this.params.installFlags & 32) == 0 ? packageName : "";
        long currentTimestamp = System.currentTimeMillis();
        if (returnCode != 1) {
            packageUid = -1;
        } else {
            packageUid = this.mPm.snapshotComputer().getPackageUid(packageName, 0L, this.userId);
        }
        FrameworkStatsLog.write(263, isIncrementalInstallation(), packageNameToLog, currentTimestamp - this.createdMillis, returnCode, getApksSize(packageName), packageUid);
    }

    private long getApksSize(String packageName) {
        File apkDirOrPath;
        PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        PackageStateInternal ps = pmi.getPackageStateInternal(packageName);
        if (ps == null || (apkDirOrPath = ps.getPath()) == null) {
            return 0L;
        }
        if (apkDirOrPath.isFile() && apkDirOrPath.getName().toLowerCase().endsWith(".apk")) {
            return apkDirOrPath.length();
        }
        if (!apkDirOrPath.isDirectory()) {
            return 0L;
        }
        File[] files = apkDirOrPath.listFiles();
        long apksSize = 0;
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().toLowerCase().endsWith(".apk")) {
                apksSize += files[i].length();
            }
        }
        return apksSize;
    }

    private boolean mayInheritNativeLibs() {
        return SystemProperties.getBoolean(PROPERTY_NAME_INHERIT_NATIVE, true) && this.params.mode == 2 && (this.params.installFlags & 1) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isApexSession() {
        return (this.params.installFlags & 131072) != 0;
    }

    boolean sessionContains(Predicate<PackageInstallerSession> filter) {
        List<PackageInstallerSession> childSessions;
        if (!isMultiPackage()) {
            return filter.test(this);
        }
        synchronized (this.mLock) {
            childSessions = getChildSessionsLocked();
        }
        for (PackageInstallerSession child : childSessions) {
            if (filter.test(child)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$containsApkSession$6(PackageInstallerSession s) {
        return !s.isApexSession();
    }

    boolean containsApkSession() {
        return sessionContains(new Predicate() { // from class: com.android.server.pm.PackageInstallerSession$$ExternalSyntheticLambda5
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return PackageInstallerSession.lambda$containsApkSession$6((PackageInstallerSession) obj);
            }
        });
    }

    private void validateApexInstallLocked() throws PackageManagerException {
        String targetName;
        List<File> addedFiles = getAddedApksLocked();
        if (addedFiles.isEmpty()) {
            throw new PackageManagerException(-2, TextUtils.formatSimple("Session: %d. No packages staged in %s", new Object[]{Integer.valueOf(this.sessionId), this.stageDir.getAbsolutePath()}));
        }
        if (ArrayUtils.size(addedFiles) > 1) {
            throw new PackageManagerException(-2, "Too many files for apex install");
        }
        File addedFile = addedFiles.get(0);
        String sourceName = addedFile.getName();
        if (!sourceName.endsWith(APEX_FILE_EXTENSION)) {
            targetName = sourceName + APEX_FILE_EXTENSION;
        } else {
            targetName = sourceName;
        }
        if (!FileUtils.isValidExtFilename(targetName)) {
            throw new PackageManagerException(-2, "Invalid filename: " + targetName);
        }
        File targetFile = new File(this.stageDir, targetName);
        resolveAndStageFileLocked(addedFile, targetFile, null);
        this.mResolvedBaseFile = targetFile;
        this.mPackageName = null;
        ParseTypeImpl input = ParseTypeImpl.forDefaultParsing();
        ParseResult<ApkLite> ret = ApkLiteParseUtils.parseApkLite(input.reset(), this.mResolvedBaseFile, 32);
        if (ret.isError()) {
            throw new PackageManagerException(ret.getErrorCode(), ret.getErrorMessage(), ret.getException());
        }
        ApkLite apk = (ApkLite) ret.getResult();
        if (this.mPackageName == null) {
            this.mPackageName = apk.getPackageName();
            this.mVersionCode = apk.getLongVersionCode();
        }
        this.mSigningDetails = apk.getSigningDetails();
        this.mHasDeviceAdminReceiver = apk.isHasDeviceAdminReceiver();
    }

    private PackageLite validateApkInstallLocked() throws PackageManagerException {
        boolean z;
        PackageLite packageLite;
        File[] libDirs;
        File packageInstallDir;
        int i;
        List<String> libDirsToInherit;
        List<File> libFilesToInherit;
        File[] fileArr;
        File packageInstallDir2;
        int i2;
        List<String> libDirsToInherit2;
        List<File> libFilesToInherit2;
        int i3;
        ApkLite baseApk;
        List<File> addedFiles;
        ArraySet<String> stagedSplits;
        List<File> removedFiles;
        this.mPackageLite = null;
        this.mPackageName = null;
        this.mVersionCode = -1L;
        this.mSigningDetails = SigningDetails.UNKNOWN;
        this.mResolvedBaseFile = null;
        this.mResolvedStagedFiles.clear();
        this.mResolvedInheritedFiles.clear();
        PackageInfo pkgInfo = this.mPm.snapshotComputer().getPackageInfo(this.params.appPackageName, 67108928L, this.userId);
        if (this.params.mode != 2 || (pkgInfo != null && pkgInfo.applicationInfo != null)) {
            this.mVerityFoundForApks = PackageManagerServiceUtils.isApkVerityEnabled() && this.params.mode == 2 && VerityUtils.hasFsverity(pkgInfo.applicationInfo.getBaseCodePath());
            List<File> removedFiles2 = getRemovedFilesLocked();
            List<String> removeSplitList = new ArrayList<>();
            if (!removedFiles2.isEmpty()) {
                for (File removedFile : removedFiles2) {
                    String fileName = removedFile.getName();
                    removeSplitList.add(fileName.substring(0, fileName.length() - REMOVE_MARKER_EXTENSION.length()));
                }
            }
            List<File> addedFiles2 = getAddedApksLocked();
            if (addedFiles2.isEmpty() && removeSplitList.size() == 0) {
                throw new PackageManagerException(-2, TextUtils.formatSimple("Session: %d. No packages staged in %s", new Object[]{Integer.valueOf(this.sessionId), this.stageDir.getAbsolutePath()}));
            }
            ArraySet<String> stagedSplits2 = new ArraySet<>();
            ArraySet<String> stagedSplitTypes = new ArraySet<>();
            ArraySet<String> requiredSplitTypes = new ArraySet<>();
            ArrayMap<String, ApkLite> splitApks = new ArrayMap<>();
            ParseTypeImpl input = ParseTypeImpl.forDefaultParsing();
            ApkLite baseApk2 = null;
            for (File addedFile : addedFiles2) {
                ParseResult<ApkLite> result = ApkLiteParseUtils.parseApkLite(input.reset(), addedFile, 32);
                if (result.isError()) {
                    throw new PackageManagerException(result.getErrorCode(), result.getErrorMessage(), result.getException());
                }
                ApkLite apk = (ApkLite) result.getResult();
                if (!stagedSplits2.add(apk.getSplitName())) {
                    throw new PackageManagerException(-2, "Split " + apk.getSplitName() + " was defined multiple times");
                }
                if (this.mPackageName == null) {
                    this.mPackageName = apk.getPackageName();
                    addedFiles = addedFiles2;
                    stagedSplits = stagedSplits2;
                    this.mVersionCode = apk.getLongVersionCode();
                } else {
                    addedFiles = addedFiles2;
                    stagedSplits = stagedSplits2;
                }
                if (this.mSigningDetails == SigningDetails.UNKNOWN) {
                    this.mSigningDetails = apk.getSigningDetails();
                }
                this.mHasDeviceAdminReceiver = apk.isHasDeviceAdminReceiver();
                assertApkConsistentLocked(String.valueOf(addedFile), apk);
                String targetName = ApkLiteParseUtils.splitNameToFileName(apk);
                if (!FileUtils.isValidExtFilename(targetName)) {
                    throw new PackageManagerException(-2, "Invalid filename: " + targetName);
                }
                if (apk.getInstallLocation() == -1) {
                    removedFiles = removedFiles2;
                } else {
                    String installerPackageName = getInstallerPackageName();
                    if (installerPackageName == null) {
                        removedFiles = removedFiles2;
                    } else {
                        removedFiles = removedFiles2;
                        if (this.params.installLocation != apk.getInstallLocation()) {
                            Slog.wtf(TAG, installerPackageName + " drops manifest attribute android:installLocation in " + targetName + " for " + this.mPackageName);
                        }
                    }
                }
                File targetFile = new File(this.stageDir, targetName);
                resolveAndStageFileLocked(addedFile, targetFile, apk.getSplitName());
                if (apk.getSplitName() == null) {
                    this.mResolvedBaseFile = targetFile;
                    baseApk2 = apk;
                } else {
                    splitApks.put(apk.getSplitName(), apk);
                }
                CollectionUtils.addAll(requiredSplitTypes, apk.getRequiredSplitTypes());
                CollectionUtils.addAll(stagedSplitTypes, apk.getSplitTypes());
                addedFiles2 = addedFiles;
                stagedSplits2 = stagedSplits;
                removedFiles2 = removedFiles;
            }
            ArraySet<String> stagedSplits3 = stagedSplits2;
            if (removeSplitList.size() > 0) {
                if (pkgInfo == null) {
                    throw new PackageManagerException(-2, "Missing existing base package for " + this.mPackageName);
                }
                for (String splitName : removeSplitList) {
                    if (!ArrayUtils.contains(pkgInfo.splitNames, splitName)) {
                        throw new PackageManagerException(-2, "Split not found: " + splitName);
                    }
                }
                if (this.mPackageName == null) {
                    this.mPackageName = pkgInfo.packageName;
                    this.mVersionCode = pkgInfo.getLongVersionCode();
                }
                if (this.mSigningDetails == SigningDetails.UNKNOWN) {
                    this.mSigningDetails = unsafeGetCertsWithoutVerification(pkgInfo.applicationInfo.sourceDir);
                }
            }
            if (!isIncrementalInstallation() || isIncrementalInstallationAllowed(this.mPackageName)) {
                if (this.mInstallerUid != this.mOriginalInstallerUid && (TextUtils.isEmpty(this.mPackageName) || !this.mPackageName.equals(this.mOriginalInstallerPackageName))) {
                    throw new PackageManagerException(-23, "Can only transfer sessions that update the original installer");
                }
                if (!this.mChecksums.isEmpty()) {
                    throw new PackageManagerException(-116, "Invalid checksum name(s): " + String.join(",", this.mChecksums.keySet()));
                }
                if (this.params.mode == 1) {
                    if (!stagedSplits3.contains(null)) {
                        throw new PackageManagerException(-2, "Full install must include a base package");
                    }
                    if ((this.params.installFlags & 4096) != 0) {
                        EventLog.writeEvent(1397638484, "219044664");
                        this.params.setDontKillApp(false);
                    }
                    if (!baseApk2.isSplitRequired() || (stagedSplits3.size() > 1 && stagedSplitTypes.containsAll(requiredSplitTypes))) {
                        ParseResult<PackageLite> pkgLiteResult = ApkLiteParseUtils.composePackageLiteFromApks(input.reset(), this.stageDir, baseApk2, splitApks, true);
                        if (!pkgLiteResult.isError()) {
                            this.mPackageLite = (PackageLite) pkgLiteResult.getResult();
                            packageLite = this.mPackageLite;
                            z = true;
                        } else {
                            throw new PackageManagerException(pkgLiteResult.getErrorCode(), pkgLiteResult.getErrorMessage(), pkgLiteResult.getException());
                        }
                    } else {
                        throw new PackageManagerException(-28, "Missing split for " + this.mPackageName);
                    }
                } else {
                    ApkLite baseApk3 = baseApk2;
                    ApplicationInfo appInfo = pkgInfo.applicationInfo;
                    ParseResult<PackageLite> pkgLiteResult2 = ApkLiteParseUtils.parsePackageLite(input.reset(), new File(appInfo.getCodePath()), 0);
                    if (pkgLiteResult2.isError()) {
                        throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, pkgLiteResult2.getErrorMessage(), pkgLiteResult2.getException());
                    }
                    PackageLite existing = (PackageLite) pkgLiteResult2.getResult();
                    assertPackageConsistentLocked("Existing", existing.getPackageName(), existing.getLongVersionCode());
                    SigningDetails signingDetails = unsafeGetCertsWithoutVerification(existing.getBaseApkPath());
                    if (!this.mSigningDetails.signaturesMatchExactly(signingDetails)) {
                        throw new PackageManagerException(-2, "Existing signatures are inconsistent");
                    }
                    if (this.mResolvedBaseFile == null) {
                        File file = new File(appInfo.getBaseCodePath());
                        this.mResolvedBaseFile = file;
                        inheritFileLocked(file);
                        CollectionUtils.addAll(requiredSplitTypes, existing.getBaseRequiredSplitTypes());
                    } else if ((this.params.installFlags & 4096) != 0) {
                        EventLog.writeEvent(1397638484, "219044664");
                        this.params.setDontKillApp(false);
                    }
                    if (!ArrayUtils.isEmpty(existing.getSplitNames())) {
                        for (int i4 = 0; i4 < existing.getSplitNames().length; i4++) {
                            String splitName2 = existing.getSplitNames()[i4];
                            File splitFile = new File(existing.getSplitApkPaths()[i4]);
                            boolean splitRemoved = removeSplitList.contains(splitName2);
                            if (!stagedSplits3.contains(splitName2) && !splitRemoved) {
                                inheritFileLocked(splitFile);
                                CollectionUtils.addAll(requiredSplitTypes, existing.getRequiredSplitTypes()[i4]);
                                CollectionUtils.addAll(stagedSplitTypes, existing.getSplitTypes()[i4]);
                            }
                        }
                    }
                    File packageInstallDir3 = new File(appInfo.getBaseCodePath()).getParentFile();
                    this.mInheritedFilesBase = packageInstallDir3;
                    File oatDir = new File(packageInstallDir3, "oat");
                    if (oatDir.exists()) {
                        File[] archSubdirs = oatDir.listFiles();
                        if (archSubdirs != null && archSubdirs.length > 0) {
                            String[] instructionSets = InstructionSets.getAllDexCodeInstructionSets();
                            int length = archSubdirs.length;
                            int i5 = 0;
                            while (i5 < length) {
                                File archSubDir = archSubdirs[i5];
                                File[] archSubdirs2 = archSubdirs;
                                if (!ArrayUtils.contains(instructionSets, archSubDir.getName())) {
                                    i3 = length;
                                    baseApk = baseApk3;
                                } else {
                                    File[] files = archSubDir.listFiles();
                                    if (files != null) {
                                        i3 = length;
                                        if (files.length == 0) {
                                            baseApk = baseApk3;
                                        } else {
                                            baseApk = baseApk3;
                                            this.mResolvedInstructionSets.add(archSubDir.getName());
                                            this.mResolvedInheritedFiles.addAll(Arrays.asList(files));
                                        }
                                    } else {
                                        i3 = length;
                                        baseApk = baseApk3;
                                    }
                                }
                                i5++;
                                archSubdirs = archSubdirs2;
                                length = i3;
                                baseApk3 = baseApk;
                            }
                        }
                    }
                    if (mayInheritNativeLibs() && removeSplitList.isEmpty()) {
                        File[] libDirs2 = {new File(packageInstallDir3, "lib"), new File(packageInstallDir3, "lib64")};
                        int length2 = libDirs2.length;
                        int i6 = 0;
                        while (i6 < length2) {
                            File libDir = libDirs2[i6];
                            if (!libDir.exists()) {
                                libDirs = libDirs2;
                                packageInstallDir = packageInstallDir3;
                                i = length2;
                            } else if (!libDir.isDirectory()) {
                                libDirs = libDirs2;
                                packageInstallDir = packageInstallDir3;
                                i = length2;
                            } else {
                                List<String> libDirsToInherit3 = new ArrayList<>();
                                List<File> libFilesToInherit3 = new ArrayList<>();
                                File[] listFiles = libDir.listFiles();
                                int length3 = listFiles.length;
                                libDirs = libDirs2;
                                int i7 = 0;
                                while (i7 < length3) {
                                    int i8 = length3;
                                    File archSubDir2 = listFiles[i7];
                                    if (!archSubDir2.isDirectory()) {
                                        fileArr = listFiles;
                                        packageInstallDir2 = packageInstallDir3;
                                        i2 = length2;
                                        libDirsToInherit2 = libDirsToInherit3;
                                        libFilesToInherit2 = libFilesToInherit3;
                                    } else {
                                        try {
                                            String relLibPath = getRelativePath(archSubDir2, packageInstallDir3);
                                            fileArr = listFiles;
                                            File[] files2 = archSubDir2.listFiles();
                                            if (files2 != null) {
                                                packageInstallDir2 = packageInstallDir3;
                                                if (files2.length == 0) {
                                                    i2 = length2;
                                                    libDirsToInherit2 = libDirsToInherit3;
                                                    libFilesToInherit2 = libFilesToInherit3;
                                                } else {
                                                    libDirsToInherit2 = libDirsToInherit3;
                                                    libDirsToInherit2.add(relLibPath);
                                                    i2 = length2;
                                                    libFilesToInherit2 = libFilesToInherit3;
                                                    libFilesToInherit2.addAll(Arrays.asList(files2));
                                                }
                                            } else {
                                                packageInstallDir2 = packageInstallDir3;
                                                i2 = length2;
                                                libDirsToInherit2 = libDirsToInherit3;
                                                libFilesToInherit2 = libFilesToInherit3;
                                            }
                                        } catch (IOException e) {
                                            packageInstallDir = packageInstallDir3;
                                            i = length2;
                                            libDirsToInherit = libDirsToInherit3;
                                            libFilesToInherit = libFilesToInherit3;
                                            Slog.e(TAG, "Skipping linking of native library directory!", e);
                                            libDirsToInherit.clear();
                                            libFilesToInherit.clear();
                                        }
                                    }
                                    i7++;
                                    libDirsToInherit3 = libDirsToInherit2;
                                    libFilesToInherit3 = libFilesToInherit2;
                                    length3 = i8;
                                    listFiles = fileArr;
                                    packageInstallDir3 = packageInstallDir2;
                                    length2 = i2;
                                }
                                packageInstallDir = packageInstallDir3;
                                i = length2;
                                libDirsToInherit = libDirsToInherit3;
                                libFilesToInherit = libFilesToInherit3;
                                for (String subDir : libDirsToInherit) {
                                    if (!this.mResolvedNativeLibPaths.contains(subDir)) {
                                        this.mResolvedNativeLibPaths.add(subDir);
                                    }
                                }
                                this.mResolvedInheritedFiles.addAll(libFilesToInherit);
                            }
                            i6++;
                            libDirs2 = libDirs;
                            packageInstallDir3 = packageInstallDir;
                            length2 = i;
                        }
                    }
                    if (!existing.isSplitRequired()) {
                        z = true;
                    } else {
                        int existingSplits = ArrayUtils.size(existing.getSplitNames());
                        boolean allSplitsRemoved = existingSplits == removeSplitList.size();
                        z = true;
                        boolean onlyBaseFileStaged = stagedSplits3.size() == 1 && stagedSplits3.contains(null);
                        if ((allSplitsRemoved && (stagedSplits3.isEmpty() || onlyBaseFileStaged)) || !stagedSplitTypes.containsAll(requiredSplitTypes)) {
                            throw new PackageManagerException(-28, "Missing split for " + this.mPackageName);
                        }
                    }
                    packageLite = existing;
                }
                if (packageLite.isUseEmbeddedDex()) {
                    for (File file2 : this.mResolvedStagedFiles) {
                        if (file2.getName().endsWith(".apk") && !DexManager.auditUncompressedDexInApk(file2.getPath())) {
                            throw new PackageManagerException(-2, "Some dex are not uncompressed and aligned correctly for " + this.mPackageName);
                        }
                    }
                }
                boolean isInstallerShell = this.mInstallerUid == 2000 ? z : false;
                if (isInstallerShell && isIncrementalInstallation() && this.mIncrementalFileStorages != null && !packageLite.isDebuggable() && !packageLite.isProfileableByShell()) {
                    this.mIncrementalFileStorages.disallowReadLogs();
                }
                this.mValidatedTargetSdk = packageLite.getTargetSdk();
                return packageLite;
            }
            throw new PackageManagerException(-116, "Incremental installation of this package is not allowed.");
        }
        throw new PackageManagerException(-2, "Missing existing base package");
    }

    private void stageFileLocked(File origFile, File targetFile) throws PackageManagerException {
        this.mResolvedStagedFiles.add(targetFile);
        maybeRenameFile(origFile, targetFile);
    }

    private void maybeStageFsveritySignatureLocked(File origFile, File targetFile, boolean fsVerityRequired) throws PackageManagerException {
        File originalSignature = new File(VerityUtils.getFsveritySignatureFilePath(origFile.getPath()));
        if (originalSignature.exists()) {
            File stagedSignature = new File(VerityUtils.getFsveritySignatureFilePath(targetFile.getPath()));
            stageFileLocked(originalSignature, stagedSignature);
        } else if (fsVerityRequired) {
            throw new PackageManagerException(-118, "Missing corresponding fs-verity signature to " + origFile);
        }
    }

    private void maybeStageDexMetadataLocked(File origFile, File targetFile) throws PackageManagerException {
        File dexMetadataFile = DexMetadataHelper.findDexMetadataForFile(origFile);
        if (dexMetadataFile == null) {
            return;
        }
        if (!FileUtils.isValidExtFilename(dexMetadataFile.getName())) {
            throw new PackageManagerException(-2, "Invalid filename: " + dexMetadataFile);
        }
        File targetDexMetadataFile = new File(this.stageDir, DexMetadataHelper.buildDexMetadataPathForApk(targetFile.getName()));
        stageFileLocked(dexMetadataFile, targetDexMetadataFile);
        maybeStageFsveritySignatureLocked(dexMetadataFile, targetDexMetadataFile, DexMetadataHelper.isFsVerityRequired());
    }

    private IncrementalFileStorages getIncrementalFileStorages() {
        IncrementalFileStorages incrementalFileStorages;
        synchronized (this.mLock) {
            incrementalFileStorages = this.mIncrementalFileStorages;
        }
        return incrementalFileStorages;
    }

    private void storeBytesToInstallationFile(String localPath, String absolutePath, byte[] bytes) throws IOException {
        IncrementalFileStorages incrementalFileStorages = getIncrementalFileStorages();
        if (!isIncrementalInstallation() || incrementalFileStorages == null) {
            FileUtils.bytesToFile(absolutePath, bytes);
        } else {
            incrementalFileStorages.makeFile(localPath, bytes);
        }
    }

    private void maybeStageDigestsLocked(File origFile, File targetFile, String splitName) throws PackageManagerException {
        PerFileChecksum perFileChecksum = this.mChecksums.get(origFile.getName());
        if (perFileChecksum == null) {
            return;
        }
        this.mChecksums.remove(origFile.getName());
        Checksum[] checksums = perFileChecksum.getChecksums();
        if (checksums.length == 0) {
            return;
        }
        String targetDigestsPath = ApkChecksums.buildDigestsPathForApk(targetFile.getName());
        File targetDigestsFile = new File(this.stageDir, targetDigestsPath);
        try {
            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                try {
                    ApkChecksums.writeChecksums(os, checksums);
                    byte[] signature = perFileChecksum.getSignature();
                    if (signature != null && signature.length > 0) {
                        ApkChecksums.verifySignature(checksums, signature);
                    }
                    storeBytesToInstallationFile(targetDigestsPath, targetDigestsFile.getAbsolutePath(), os.toByteArray());
                    stageFileLocked(targetDigestsFile, targetDigestsFile);
                    if (signature != null && signature.length != 0) {
                        String targetDigestsSignaturePath = ApkChecksums.buildSignaturePathForDigests(targetDigestsPath);
                        File targetDigestsSignatureFile = new File(this.stageDir, targetDigestsSignaturePath);
                        storeBytesToInstallationFile(targetDigestsSignaturePath, targetDigestsSignatureFile.getAbsolutePath(), signature);
                        stageFileLocked(targetDigestsSignatureFile, targetDigestsSignatureFile);
                        os.close();
                        return;
                    }
                    os.close();
                } catch (Throwable th) {
                    try {
                        os.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            } catch (NoSuchAlgorithmException | SignatureException e) {
                throw new PackageManagerException(GnssNative.GeofenceCallbacks.GEOFENCE_STATUS_ERROR_INVALID_TRANSITION, "Failed to verify digests' signature for " + this.mPackageName, e);
            }
        } catch (IOException e2) {
            throw new PackageManagerException(-4, "Failed to store digests for " + this.mPackageName, e2);
        }
    }

    private boolean isFsVerityRequiredForApk(File origFile, File targetFile) throws PackageManagerException {
        if (this.mVerityFoundForApks) {
            return true;
        }
        File originalSignature = new File(VerityUtils.getFsveritySignatureFilePath(origFile.getPath()));
        if (!originalSignature.exists()) {
            return false;
        }
        this.mVerityFoundForApks = true;
        for (File file : this.mResolvedStagedFiles) {
            if (file.getName().endsWith(".apk") && !targetFile.getName().equals(file.getName())) {
                throw new PackageManagerException(-118, "Previously staged apk is missing fs-verity signature");
            }
        }
        return true;
    }

    private void resolveAndStageFileLocked(File origFile, File targetFile, String splitName) throws PackageManagerException {
        stageFileLocked(origFile, targetFile);
        maybeStageFsveritySignatureLocked(origFile, targetFile, isFsVerityRequiredForApk(origFile, targetFile));
        maybeStageDexMetadataLocked(origFile, targetFile);
        maybeStageDigestsLocked(origFile, targetFile, splitName);
    }

    private void maybeInheritFsveritySignatureLocked(File origFile) {
        File fsveritySignatureFile = new File(VerityUtils.getFsveritySignatureFilePath(origFile.getPath()));
        if (fsveritySignatureFile.exists()) {
            this.mResolvedInheritedFiles.add(fsveritySignatureFile);
        }
    }

    private void inheritFileLocked(File origFile) {
        this.mResolvedInheritedFiles.add(origFile);
        maybeInheritFsveritySignatureLocked(origFile);
        File dexMetadataFile = DexMetadataHelper.findDexMetadataForFile(origFile);
        if (dexMetadataFile != null) {
            this.mResolvedInheritedFiles.add(dexMetadataFile);
            maybeInheritFsveritySignatureLocked(dexMetadataFile);
        }
        File digestsFile = ApkChecksums.findDigestsForFile(origFile);
        if (digestsFile != null) {
            this.mResolvedInheritedFiles.add(digestsFile);
            File signatureFile = ApkChecksums.findSignatureForDigests(digestsFile);
            if (signatureFile != null) {
                this.mResolvedInheritedFiles.add(signatureFile);
            }
        }
    }

    private void assertApkConsistentLocked(String tag, ApkLite apk) throws PackageManagerException {
        assertPackageConsistentLocked(tag, apk.getPackageName(), apk.getLongVersionCode());
        if (!this.mSigningDetails.signaturesMatchExactly(apk.getSigningDetails())) {
            throw new PackageManagerException(-2, tag + " signatures are inconsistent");
        }
    }

    private void assertPackageConsistentLocked(String tag, String packageName, long versionCode) throws PackageManagerException {
        if (!this.mPackageName.equals(packageName)) {
            throw new PackageManagerException(-2, tag + " package " + packageName + " inconsistent with " + this.mPackageName);
        }
        if (this.params.appPackageName != null && !this.params.appPackageName.equals(packageName)) {
            throw new PackageManagerException(-2, tag + " specified package " + this.params.appPackageName + " inconsistent with " + packageName);
        }
        if (this.mVersionCode != versionCode) {
            throw new PackageManagerException(-2, tag + " version code " + versionCode + " inconsistent with " + this.mVersionCode);
        }
    }

    private SigningDetails unsafeGetCertsWithoutVerification(String path) throws PackageManagerException {
        ParseTypeImpl input = ParseTypeImpl.forDefaultParsing();
        ParseResult<SigningDetails> result = ApkSignatureVerifier.unsafeGetCertsWithoutVerification(input, path, 1);
        if (result.isError()) {
            throw new PackageManagerException(-2, "Couldn't obtain signatures from APK : " + path);
        }
        return (SigningDetails) result.getResult();
    }

    private static boolean isLinkPossible(List<File> fromFiles, File toDir) {
        try {
            StructStat toStat = Os.stat(toDir.getAbsolutePath());
            for (File fromFile : fromFiles) {
                StructStat fromStat = Os.stat(fromFile.getAbsolutePath());
                if (fromStat.st_dev != toStat.st_dev) {
                    return false;
                }
            }
            return true;
        } catch (ErrnoException e) {
            Slog.w(TAG, "Failed to detect if linking possible: " + e);
            return false;
        }
    }

    public int getInstallerUid() {
        int i;
        synchronized (this.mLock) {
            i = this.mInstallerUid;
        }
        return i;
    }

    public String getPackageName() {
        String str;
        synchronized (this.mLock) {
            str = this.mPackageName;
        }
        return str;
    }

    public long getUpdatedMillis() {
        long j;
        synchronized (this.mLock) {
            j = this.updatedMillis;
        }
        return j;
    }

    long getCommittedMillis() {
        long j;
        synchronized (this.mLock) {
            j = this.committedMillis;
        }
        return j;
    }

    String getInstallerPackageName() {
        return getInstallSource().installerPackageName;
    }

    String getInstallerAttributionTag() {
        return getInstallSource().installerAttributionTag;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InstallSource getInstallSource() {
        InstallSource installSource;
        synchronized (this.mLock) {
            installSource = this.mInstallSource;
        }
        return installSource;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SigningDetails getSigningDetails() {
        SigningDetails signingDetails;
        synchronized (this.mLock) {
            signingDetails = this.mSigningDetails;
        }
        return signingDetails;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageLite getPackageLite() {
        PackageLite packageLite;
        synchronized (this.mLock) {
            packageLite = this.mPackageLite;
        }
        return packageLite;
    }

    private static String getRelativePath(File file, File base) throws IOException {
        String pathStr = file.getAbsolutePath();
        String baseStr = base.getAbsolutePath();
        if (pathStr.contains("/.")) {
            throw new IOException("Invalid path (was relative) : " + pathStr);
        }
        if (pathStr.startsWith(baseStr)) {
            return pathStr.substring(baseStr.length());
        }
        throw new IOException("File: " + pathStr + " outside base: " + baseStr);
    }

    private void createOatDirs(String packageName, List<String> instructionSets, File fromDir) throws PackageManagerException {
        for (String instructionSet : instructionSets) {
            try {
                this.mInstaller.createOatDir(packageName, fromDir.getAbsolutePath(), instructionSet);
            } catch (Installer.InstallerException e) {
                throw PackageManagerException.from(e);
            }
        }
    }

    private void linkFile(String packageName, String relativePath, String fromBase, String toBase) throws IOException {
        try {
            IncrementalFileStorages incrementalFileStorages = getIncrementalFileStorages();
            if (incrementalFileStorages != null && incrementalFileStorages.makeLink(relativePath, fromBase, toBase)) {
                return;
            }
            this.mInstaller.linkFile(packageName, relativePath, fromBase, toBase);
        } catch (Installer.InstallerException | IOException e) {
            throw new IOException("failed linkOrCreateDir(" + relativePath + ", " + fromBase + ", " + toBase + ")", e);
        }
    }

    private void linkFiles(String packageName, List<File> fromFiles, File toDir, File fromDir) throws IOException {
        for (File fromFile : fromFiles) {
            String relativePath = getRelativePath(fromFile, fromDir);
            String fromBase = fromDir.getAbsolutePath();
            String toBase = toDir.getAbsolutePath();
            linkFile(packageName, relativePath, fromBase, toBase);
        }
        Slog.d(TAG, "Linked " + fromFiles.size() + " files into " + toDir);
    }

    private static void copyFiles(List<File> fromFiles, File toDir) throws IOException {
        File[] listFiles;
        for (File file : toDir.listFiles()) {
            if (file.getName().endsWith(".tmp")) {
                file.delete();
            }
        }
        for (File fromFile : fromFiles) {
            File tmpFile = File.createTempFile("inherit", ".tmp", toDir);
            Slog.d(TAG, "Copying " + fromFile + " to " + tmpFile);
            if (!FileUtils.copyFile(fromFile, tmpFile)) {
                throw new IOException("Failed to copy " + fromFile + " to " + tmpFile);
            }
            try {
                Os.chmod(tmpFile.getAbsolutePath(), FrameworkStatsLog.VBMETA_DIGEST_REPORTED);
                File toFile = new File(toDir, fromFile.getName());
                Slog.d(TAG, "Renaming " + tmpFile + " to " + toFile);
                if (!tmpFile.renameTo(toFile)) {
                    throw new IOException("Failed to rename " + tmpFile + " to " + toFile);
                }
            } catch (ErrnoException e) {
                throw new IOException("Failed to chmod " + tmpFile);
            }
        }
        Slog.d(TAG, "Copied " + fromFiles.size() + " files into " + toDir);
    }

    private void extractNativeLibraries(PackageLite packageLite, File packageDir, String abiOverride, boolean inherit) throws PackageManagerException {
        Objects.requireNonNull(packageLite);
        File libDir = new File(packageDir, "lib");
        if (!inherit) {
            NativeLibraryHelper.removeNativeBinariesFromDirLI(libDir, true);
        }
        NativeLibraryHelper.Handle handle = null;
        try {
            try {
                handle = NativeLibraryHelper.Handle.create(packageLite);
                int res = NativeLibraryHelper.copyNativeBinariesWithOverride(handle, libDir, abiOverride, isIncrementalInstallation());
                if (res != 1) {
                    throw new PackageManagerException(res, "Failed to extract native libraries, res=" + res);
                }
            } catch (IOException e) {
                throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Failed to extract native libraries", e);
            }
        } finally {
            IoUtils.closeQuietly(handle);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPermissionsResult(boolean accepted) {
        if (!isSealed()) {
            throw new SecurityException("Must be sealed to accept permissions");
        }
        PackageInstallerSession root = hasParentSessionId() ? this.mSessionProvider.getSession(getParentSessionId()) : this;
        if (accepted) {
            synchronized (this.mLock) {
                this.mPermissionsManuallyAccepted = true;
            }
            root.mHandler.obtainMessage(3).sendToTarget();
            return;
        }
        root.destroy();
        root.dispatchSessionFinished(-115, "User rejected permissions", null);
        root.maybeFinishChildSessions(-115, "User rejected permissions");
    }

    public void open() throws IOException {
        boolean wasPrepared;
        if (this.mActiveCount.getAndIncrement() == 0) {
            this.mCallback.onSessionActiveChanged(this, true);
        }
        synchronized (this.mLock) {
            wasPrepared = this.mPrepared;
            if (!wasPrepared) {
                File file = this.stageDir;
                if (file != null) {
                    PackageInstallerService.prepareStageDir(file);
                } else if (!this.params.isMultiPackage) {
                    throw new IllegalArgumentException("stageDir must be set");
                }
                this.mPrepared = true;
            }
        }
        if (!wasPrepared) {
            this.mCallback.onSessionPrepared(this);
        }
    }

    public void close() {
        closeInternal(true);
    }

    private void closeInternal(boolean checkCaller) {
        int activeCount;
        synchronized (this.mLock) {
            if (checkCaller) {
                assertCallerIsOwnerOrRoot();
            }
            activeCount = this.mActiveCount.decrementAndGet();
        }
        if (activeCount == 0) {
            this.mCallback.onSessionActiveChanged(this, false);
        }
    }

    private void maybeFinishChildSessions(int returnCode, String msg) {
        for (PackageInstallerSession child : getChildSessions()) {
            child.dispatchSessionFinished(returnCode, msg, null);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void assertNotChild(String cookie) {
        if (hasParentSessionId()) {
            throw new IllegalStateException(cookie + " can't be called on a child session, id=" + this.sessionId + " parentId=" + getParentSessionId());
        }
    }

    private boolean dispatchPendingAbandonCallback() {
        Runnable callback;
        synchronized (this.mLock) {
            Preconditions.checkState(this.mStageDirInUse);
            this.mStageDirInUse = false;
            callback = this.mPendingAbandonCallback;
            this.mPendingAbandonCallback = null;
        }
        if (callback == null) {
            return false;
        }
        callback.run();
        return true;
    }

    public void abandon() {
        synchronized (this.mLock) {
            assertNotChild("abandon");
            assertCallerIsOwnerOrRootOrSystem();
            if (isInTerminalState()) {
                return;
            }
            this.mDestroyed = true;
            Runnable r = new Runnable() { // from class: com.android.server.pm.PackageInstallerSession$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    PackageInstallerSession.this.m5504lambda$abandon$7$comandroidserverpmPackageInstallerSession();
                }
            };
            if (this.mStageDirInUse) {
                this.mPendingAbandonCallback = r;
                this.mCallback.onSessionChanged(this);
                return;
            }
            r.run();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$abandon$7$com-android-server-pm-PackageInstallerSession  reason: not valid java name */
    public /* synthetic */ void m5504lambda$abandon$7$comandroidserverpmPackageInstallerSession() {
        assertNotLocked("abandonStaged");
        if (isStaged() && this.mCommitted.get()) {
            this.mStagingManager.abortCommittedSession(this.mStagedSession);
        }
        destroy();
        dispatchSessionFinished(-115, "Session was abandoned", null);
        maybeFinishChildSessions(-115, "Session was abandoned because the parent session is abandoned");
    }

    public boolean isMultiPackage() {
        return this.params.isMultiPackage;
    }

    public boolean isStaged() {
        return this.params.isStaged;
    }

    public int getInstallFlags() {
        return this.params.installFlags;
    }

    public DataLoaderParamsParcel getDataLoaderParams() {
        this.mContext.enforceCallingOrSelfPermission("com.android.permission.USE_INSTALLER_V2", null);
        if (this.params.dataLoaderParams != null) {
            return this.params.dataLoaderParams.getData();
        }
        return null;
    }

    public void addFile(int location, String name, long lengthBytes, byte[] metadata, byte[] signature) {
        this.mContext.enforceCallingOrSelfPermission("com.android.permission.USE_INSTALLER_V2", null);
        if (!isDataLoaderInstallation()) {
            throw new IllegalStateException("Cannot add files to non-data loader installation session.");
        }
        if (isStreamingInstallation() && location != 0) {
            throw new IllegalArgumentException("Non-incremental installation only supports /data/app placement: " + name);
        }
        if (metadata == null) {
            throw new IllegalArgumentException("DataLoader installation requires valid metadata: " + name);
        }
        if (!FileUtils.isValidExtFilename(name)) {
            throw new IllegalArgumentException("Invalid name: " + name);
        }
        synchronized (this.mLock) {
            assertCallerIsOwnerOrRoot();
            assertPreparedAndNotSealedLocked("addFile");
            ArraySet<FileEntry> arraySet = this.mFiles;
            if (!arraySet.add(new FileEntry(arraySet.size(), new InstallationFile(location, name, lengthBytes, metadata, signature)))) {
                throw new IllegalArgumentException("File already added: " + name);
            }
        }
    }

    public void removeFile(int location, String name) {
        this.mContext.enforceCallingOrSelfPermission("com.android.permission.USE_INSTALLER_V2", null);
        if (!isDataLoaderInstallation()) {
            throw new IllegalStateException("Cannot add files to non-data loader installation session.");
        }
        if (TextUtils.isEmpty(this.params.appPackageName)) {
            throw new IllegalStateException("Must specify package name to remove a split");
        }
        synchronized (this.mLock) {
            assertCallerIsOwnerOrRoot();
            assertPreparedAndNotSealedLocked("removeFile");
            ArraySet<FileEntry> arraySet = this.mFiles;
            if (!arraySet.add(new FileEntry(arraySet.size(), new InstallationFile(location, getRemoveMarkerName(name), -1L, (byte[]) null, (byte[]) null)))) {
                throw new IllegalArgumentException("File already removed: " + name);
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:40:0x0123  */
    /* JADX WARN: Removed duplicated region for block: B:58:0x00f3 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private boolean prepareDataLoaderLocked() throws PackageManagerException {
        File parentFile;
        IncrementalFileStorages incrementalFileStorages;
        boolean z;
        if (!isDataLoaderInstallation() || this.mDataLoaderFinished) {
            return true;
        }
        final List<InstallationFileParcel> addedFiles = new ArrayList<>();
        final List<String> removedFiles = new ArrayList<>();
        InstallationFile[] files = getInstallationFilesLocked();
        for (InstallationFile file : files) {
            if (!sAddedFilter.accept(new File(this.stageDir, file.getName()))) {
                if (sRemovedFilter.accept(new File(this.stageDir, file.getName()))) {
                    String name = file.getName().substring(0, file.getName().length() - REMOVE_MARKER_EXTENSION.length());
                    removedFiles.add(name);
                }
            } else {
                addedFiles.add(file.getData());
            }
        }
        final DataLoaderParams params = this.params.dataLoaderParams;
        final boolean manualStartAndDestroy = !isIncrementalInstallation();
        final boolean systemDataLoader = isSystemDataLoaderInstallation();
        IDataLoaderStatusListener.Stub stub = new IDataLoaderStatusListener.Stub() { // from class: com.android.server.pm.PackageInstallerSession.6
            public void onStatusChanged(int dataLoaderId, int status) {
                switch (status) {
                    case 0:
                    case 1:
                    case 5:
                        return;
                    default:
                        if (PackageInstallerSession.this.mDestroyed || PackageInstallerSession.this.mDataLoaderFinished) {
                            switch (status) {
                                case 9:
                                    if (systemDataLoader) {
                                        PackageInstallerSession.this.onSystemDataLoaderUnrecoverable();
                                        return;
                                    }
                                    return;
                                default:
                                    return;
                            }
                        }
                        try {
                            switch (status) {
                                case 2:
                                    if (manualStartAndDestroy) {
                                        FileSystemControlParcel control = new FileSystemControlParcel();
                                        control.callback = new FileSystemConnector(addedFiles);
                                        PackageInstallerSession.this.getDataLoader(dataLoaderId).create(dataLoaderId, params.getData(), control, this);
                                        return;
                                    }
                                    return;
                                case 3:
                                    if (manualStartAndDestroy) {
                                        PackageInstallerSession.this.getDataLoader(dataLoaderId).start(dataLoaderId);
                                        return;
                                    }
                                    return;
                                case 4:
                                    IDataLoader dataLoader = PackageInstallerSession.this.getDataLoader(dataLoaderId);
                                    List list = addedFiles;
                                    List list2 = removedFiles;
                                    dataLoader.prepareImage(dataLoaderId, (InstallationFileParcel[]) list.toArray(new InstallationFileParcel[list.size()]), (String[]) list2.toArray(new String[list2.size()]));
                                    return;
                                case 5:
                                default:
                                    return;
                                case 6:
                                    PackageInstallerSession.this.mDataLoaderFinished = true;
                                    if (PackageInstallerSession.this.hasParentSessionId()) {
                                        PackageInstallerSession.this.mSessionProvider.getSession(PackageInstallerSession.this.getParentSessionId()).dispatchSessionSealed();
                                    } else {
                                        PackageInstallerSession.this.dispatchSessionSealed();
                                    }
                                    if (manualStartAndDestroy) {
                                        PackageInstallerSession.this.getDataLoader(dataLoaderId).destroy(dataLoaderId);
                                        return;
                                    }
                                    return;
                                case 7:
                                    PackageInstallerSession.this.mDataLoaderFinished = true;
                                    PackageInstallerSession.this.dispatchSessionValidationFailure(-20, "Failed to prepare image.");
                                    if (manualStartAndDestroy) {
                                        PackageInstallerSession.this.getDataLoader(dataLoaderId).destroy(dataLoaderId);
                                        return;
                                    }
                                    return;
                                case 8:
                                    PackageInstallerSession.sendPendingStreaming(PackageInstallerSession.this.mContext, PackageInstallerSession.this.getRemoteStatusReceiver(), PackageInstallerSession.this.sessionId, "DataLoader unavailable");
                                    return;
                                case 9:
                                    throw new PackageManagerException(-20, "DataLoader reported unrecoverable failure.");
                            }
                        } catch (RemoteException e) {
                            PackageInstallerSession.sendPendingStreaming(PackageInstallerSession.this.mContext, PackageInstallerSession.this.getRemoteStatusReceiver(), PackageInstallerSession.this.sessionId, e.getMessage());
                            return;
                        } catch (PackageManagerException e2) {
                            PackageInstallerSession.this.mDataLoaderFinished = true;
                            PackageInstallerSession.this.dispatchSessionValidationFailure(e2.error, ExceptionUtils.getCompleteMessage(e2));
                            return;
                        }
                }
            }
        };
        if (manualStartAndDestroy) {
            if (getDataLoaderManager().bindToDataLoader(this.sessionId, params.getData(), 0L, stub)) {
                return false;
            }
            throw new PackageManagerException(-20, "Failed to initialize data loader");
        }
        PackageManagerService packageManagerService = this.mPm;
        PerUidReadTimeouts[] perUidReadTimeouts = packageManagerService.getPerUidReadTimeouts(packageManagerService.snapshotComputer());
        StorageHealthCheckParams healthCheckParams = new StorageHealthCheckParams();
        healthCheckParams.blockedTimeoutMs = 2000;
        healthCheckParams.unhealthyTimeoutMs = INCREMENTAL_STORAGE_UNHEALTHY_TIMEOUT_MS;
        healthCheckParams.unhealthyMonitoringMs = 60000;
        IStorageHealthListener.Stub stub2 = new IStorageHealthListener.Stub() { // from class: com.android.server.pm.PackageInstallerSession.7
            public void onHealthStatus(int storageId, int status) {
                if (PackageInstallerSession.this.mDestroyed || PackageInstallerSession.this.mDataLoaderFinished) {
                    return;
                }
                switch (status) {
                    case 0:
                    default:
                        return;
                    case 1:
                    case 2:
                        if (systemDataLoader) {
                            return;
                        }
                        break;
                    case 3:
                        break;
                }
                PackageInstallerSession.this.mDataLoaderFinished = true;
                PackageInstallerSession.this.dispatchSessionValidationFailure(-20, "Image is missing pages required for installation.");
            }
        };
        try {
            PackageInfo pkgInfo = this.mPm.snapshotComputer().getPackageInfo(this.params.appPackageName, 0L, this.userId);
            try {
                if (pkgInfo != null) {
                    try {
                        if (pkgInfo.applicationInfo != null) {
                            parentFile = new File(pkgInfo.applicationInfo.getCodePath()).getParentFile();
                            File inheritedDir = parentFile;
                            incrementalFileStorages = this.mIncrementalFileStorages;
                            if (incrementalFileStorages != null) {
                                try {
                                    z = false;
                                } catch (IOException e) {
                                    e = e;
                                }
                                try {
                                    this.mIncrementalFileStorages = IncrementalFileStorages.initialize(this.mContext, this.stageDir, inheritedDir, params, stub, healthCheckParams, stub2, addedFiles, perUidReadTimeouts, new IPackageLoadingProgressCallback.Stub() { // from class: com.android.server.pm.PackageInstallerSession.8
                                        public void onPackageLoadingProgressChanged(float progress) {
                                            synchronized (PackageInstallerSession.this.mProgressLock) {
                                                PackageInstallerSession.this.mIncrementalProgress = progress;
                                                PackageInstallerSession.this.computeProgressLocked(true);
                                            }
                                        }
                                    });
                                } catch (IOException e2) {
                                    e = e2;
                                    throw new PackageManagerException(-20, e.getMessage(), e.getCause());
                                }
                            } else {
                                z = false;
                                try {
                                    incrementalFileStorages.startLoading(params, stub, healthCheckParams, stub2, perUidReadTimeouts);
                                } catch (IOException e3) {
                                    e = e3;
                                    throw new PackageManagerException(-20, e.getMessage(), e.getCause());
                                }
                            }
                            return z;
                        }
                    } catch (IOException e4) {
                        e = e4;
                        throw new PackageManagerException(-20, e.getMessage(), e.getCause());
                    }
                }
                incrementalFileStorages = this.mIncrementalFileStorages;
                if (incrementalFileStorages != null) {
                }
                return z;
            } catch (IOException e5) {
                e = e5;
            }
            parentFile = null;
            File inheritedDir2 = parentFile;
        } catch (IOException e6) {
            e = e6;
        }
    }

    private DataLoaderManager getDataLoaderManager() throws PackageManagerException {
        DataLoaderManager dataLoaderManager = (DataLoaderManager) this.mContext.getSystemService(DataLoaderManager.class);
        if (dataLoaderManager == null) {
            throw new PackageManagerException(-20, "Failed to find data loader manager service");
        }
        return dataLoaderManager;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public IDataLoader getDataLoader(int dataLoaderId) throws PackageManagerException {
        IDataLoader dataLoader = getDataLoaderManager().getDataLoader(dataLoaderId);
        if (dataLoader == null) {
            throw new PackageManagerException(-20, "Failure to obtain data loader");
        }
        return dataLoader;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchSessionValidationFailure(int error, String detailMessage) {
        this.mHandler.obtainMessage(5, error, -1, detailMessage).sendToTarget();
    }

    private int[] getChildSessionIdsLocked() {
        int size = this.mChildSessions.size();
        if (size == 0) {
            return EMPTY_CHILD_SESSION_ARRAY;
        }
        int[] childSessionIds = new int[size];
        for (int i = 0; i < size; i++) {
            childSessionIds[i] = this.mChildSessions.keyAt(i);
        }
        return childSessionIds;
    }

    public int[] getChildSessionIds() {
        int[] childSessionIdsLocked;
        synchronized (this.mLock) {
            childSessionIdsLocked = getChildSessionIdsLocked();
        }
        return childSessionIdsLocked;
    }

    private boolean canBeAddedAsChild(int parentCandidate) {
        boolean z;
        synchronized (this.mLock) {
            z = ((hasParentSessionId() && this.mParentSessionId != parentCandidate) || this.mCommitted.get() || this.mDestroyed) ? false : true;
        }
        return z;
    }

    private void acquireTransactionLock() {
        if (!this.mTransactionLock.compareAndSet(false, true)) {
            throw new UnsupportedOperationException("Concurrent access not supported");
        }
    }

    private void releaseTransactionLock() {
        this.mTransactionLock.compareAndSet(true, false);
    }

    public void addChildSessionId(int childSessionId) {
        if (!this.params.isMultiPackage) {
            throw new IllegalStateException("Single-session " + this.sessionId + " can't have child.");
        }
        PackageInstallerSession childSession = this.mSessionProvider.getSession(childSessionId);
        if (childSession == null) {
            throw new IllegalStateException("Unable to add child session " + childSessionId + " as it does not exist.");
        }
        if (childSession.params.isMultiPackage) {
            throw new IllegalStateException("Multi-session " + childSessionId + " can't be a child.");
        }
        if (this.params.isStaged != childSession.params.isStaged) {
            throw new IllegalStateException("Multipackage Inconsistency: session " + childSession.sessionId + " and session " + this.sessionId + " have inconsistent staged settings");
        }
        if (this.params.getEnableRollback() != childSession.params.getEnableRollback()) {
            throw new IllegalStateException("Multipackage Inconsistency: session " + childSession.sessionId + " and session " + this.sessionId + " have inconsistent rollback settings");
        }
        boolean hasAPEX = false;
        boolean hasAPK = containsApkSession() || !childSession.isApexSession();
        if (sessionContains(new Predicate() { // from class: com.android.server.pm.PackageInstallerSession$$ExternalSyntheticLambda1
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean isApexSession;
                isApexSession = ((PackageInstallerSession) obj).isApexSession();
                return isApexSession;
            }
        }) || childSession.isApexSession()) {
            hasAPEX = true;
        }
        if (!this.params.isStaged && hasAPK && hasAPEX) {
            throw new IllegalStateException("Mix of APK and APEX is not supported for non-staged multi-package session");
        }
        try {
            acquireTransactionLock();
            childSession.acquireTransactionLock();
            if (!childSession.canBeAddedAsChild(this.sessionId)) {
                throw new IllegalStateException("Unable to add child session " + childSessionId + " as it is in an invalid state.");
            }
            synchronized (this.mLock) {
                assertCallerIsOwnerOrRoot();
                assertPreparedAndNotSealedLocked("addChildSessionId");
                int indexOfSession = this.mChildSessions.indexOfKey(childSessionId);
                if (indexOfSession >= 0) {
                    return;
                }
                childSession.setParentSessionId(this.sessionId);
                this.mChildSessions.put(childSessionId, childSession);
            }
        } finally {
            releaseTransactionLock();
            childSession.releaseTransactionLock();
        }
    }

    public void removeChildSessionId(int sessionId) {
        synchronized (this.mLock) {
            assertCallerIsOwnerOrRoot();
            assertPreparedAndNotSealedLocked("removeChildSessionId");
            int indexOfSession = this.mChildSessions.indexOfKey(sessionId);
            if (indexOfSession < 0) {
                return;
            }
            PackageInstallerSession session = this.mChildSessions.valueAt(indexOfSession);
            acquireTransactionLock();
            session.acquireTransactionLock();
            session.setParentSessionId(-1);
            this.mChildSessions.removeAt(indexOfSession);
            releaseTransactionLock();
            session.releaseTransactionLock();
        }
    }

    void setParentSessionId(int parentSessionId) {
        synchronized (this.mLock) {
            if (parentSessionId != -1) {
                if (this.mParentSessionId != -1) {
                    throw new IllegalStateException("The parent of " + this.sessionId + " is alreadyset to " + this.mParentSessionId);
                }
            }
            this.mParentSessionId = parentSessionId;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasParentSessionId() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mParentSessionId != -1;
        }
        return z;
    }

    public int getParentSessionId() {
        int i;
        synchronized (this.mLock) {
            i = this.mParentSessionId;
        }
        return i;
    }

    private void dispatchSessionFinished(int returnCode, String msg, Bundle extras) {
        sendUpdateToRemoteStatusReceiver(returnCode, msg, extras);
        synchronized (this.mLock) {
            this.mFinalStatus = returnCode;
            this.mFinalMessage = msg;
        }
        boolean isNewInstall = false;
        boolean success = returnCode == 1;
        if (extras == null || !extras.getBoolean("android.intent.extra.REPLACING")) {
            isNewInstall = true;
        }
        if (success && isNewInstall && this.mPm.mInstallerService.okToSendBroadcasts()) {
            this.mPm.sendSessionCommitBroadcast(generateInfoScrubbed(true), this.userId);
        }
        this.mCallback.onSessionFinished(this, success);
        if (isDataLoaderInstallation()) {
            logDataLoaderInstallationSession(returnCode);
        }
    }

    private void sendUpdateToRemoteStatusReceiver(int returnCode, String msg, Bundle extras) {
        IntentSender statusReceiver = getRemoteStatusReceiver();
        if (statusReceiver != null) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = getPackageName();
            args.arg2 = msg;
            args.arg3 = extras;
            args.arg4 = statusReceiver;
            args.argi1 = returnCode;
            this.mHandler.obtainMessage(4, args).sendToTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSessionReady() {
        synchronized (this.mLock) {
            if (!this.mDestroyed && !this.mSessionFailed) {
                this.mSessionReady = true;
                this.mSessionApplied = false;
                this.mSessionFailed = false;
                this.mSessionErrorCode = 0;
                this.mSessionErrorMessage = "";
                this.mCallback.onSessionChanged(this);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSessionFailed(int errorCode, String errorMessage) {
        synchronized (this.mLock) {
            if (!this.mDestroyed && !this.mSessionFailed) {
                this.mSessionReady = false;
                this.mSessionApplied = false;
                this.mSessionFailed = true;
                this.mSessionErrorCode = errorCode;
                this.mSessionErrorMessage = errorMessage;
                Slog.d(TAG, "Marking session " + this.sessionId + " as failed: " + errorMessage);
                destroy();
                this.mCallback.onSessionChanged(this);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setSessionApplied() {
        synchronized (this.mLock) {
            if (!this.mDestroyed && !this.mSessionFailed) {
                this.mSessionReady = false;
                this.mSessionApplied = true;
                this.mSessionFailed = false;
                this.mSessionErrorCode = 1;
                this.mSessionErrorMessage = "";
                Slog.d(TAG, "Marking session " + this.sessionId + " as applied");
                destroy();
                this.mCallback.onSessionChanged(this);
            }
        }
    }

    boolean isSessionReady() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mSessionReady;
        }
        return z;
    }

    boolean isSessionApplied() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mSessionApplied;
        }
        return z;
    }

    boolean isSessionFailed() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mSessionFailed;
        }
        return z;
    }

    int getSessionErrorCode() {
        int i;
        synchronized (this.mLock) {
            i = this.mSessionErrorCode;
        }
        return i;
    }

    String getSessionErrorMessage() {
        String str;
        synchronized (this.mLock) {
            str = this.mSessionErrorMessage;
        }
        return str;
    }

    private void destroy() {
        destroyInternal();
        for (PackageInstallerSession child : getChildSessions()) {
            child.destroyInternal();
        }
    }

    private void destroyInternal() {
        IncrementalFileStorages incrementalFileStorages;
        synchronized (this.mLock) {
            this.mSealed = true;
            if (!this.params.isStaged) {
                this.mDestroyed = true;
            }
            Iterator<RevocableFileDescriptor> it = this.mFds.iterator();
            while (it.hasNext()) {
                RevocableFileDescriptor fd = it.next();
                fd.revoke();
            }
            Iterator<FileBridge> it2 = this.mBridges.iterator();
            while (it2.hasNext()) {
                FileBridge bridge = it2.next();
                bridge.forceClose();
            }
            incrementalFileStorages = this.mIncrementalFileStorages;
            this.mIncrementalFileStorages = null;
        }
        if (incrementalFileStorages != null) {
            try {
                incrementalFileStorages.cleanUpAndMarkComplete();
            } catch (Installer.InstallerException e) {
                return;
            }
        }
        File file = this.stageDir;
        if (file != null) {
            String tempPackageName = file.getName();
            this.mInstaller.rmPackageDir(tempPackageName, this.stageDir.getAbsolutePath());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            dumpLocked(pw);
        }
    }

    private void dumpLocked(IndentingPrintWriter pw) {
        float clientProgress;
        float progress;
        pw.println("Session " + this.sessionId + ":");
        pw.increaseIndent();
        pw.printPair("userId", Integer.valueOf(this.userId));
        pw.printPair("mOriginalInstallerUid", Integer.valueOf(this.mOriginalInstallerUid));
        pw.printPair("mOriginalInstallerPackageName", this.mOriginalInstallerPackageName);
        pw.printPair(ATTR_INSTALLER_PACKAGE_NAME, this.mInstallSource.installerPackageName);
        pw.printPair(ATTR_INITIATING_PACKAGE_NAME, this.mInstallSource.initiatingPackageName);
        pw.printPair(ATTR_ORIGINATING_PACKAGE_NAME, this.mInstallSource.originatingPackageName);
        pw.printPair("mInstallerUid", Integer.valueOf(this.mInstallerUid));
        pw.printPair(ATTR_CREATED_MILLIS, Long.valueOf(this.createdMillis));
        pw.printPair(ATTR_UPDATED_MILLIS, Long.valueOf(this.updatedMillis));
        pw.printPair(ATTR_COMMITTED_MILLIS, Long.valueOf(this.committedMillis));
        pw.printPair("stageDir", this.stageDir);
        pw.printPair("stageCid", this.stageCid);
        pw.println();
        this.params.dump(pw);
        synchronized (this.mProgressLock) {
            clientProgress = this.mClientProgress;
            progress = this.mProgress;
        }
        pw.printPair("mClientProgress", Float.valueOf(clientProgress));
        pw.printPair("mProgress", Float.valueOf(progress));
        pw.printPair("mCommitted", this.mCommitted);
        pw.printPair("mSealed", Boolean.valueOf(this.mSealed));
        pw.printPair("mPermissionsManuallyAccepted", Boolean.valueOf(this.mPermissionsManuallyAccepted));
        pw.printPair("mStageDirInUse", Boolean.valueOf(this.mStageDirInUse));
        pw.printPair("mDestroyed", Boolean.valueOf(this.mDestroyed));
        pw.printPair("mFds", Integer.valueOf(this.mFds.size()));
        pw.printPair("mBridges", Integer.valueOf(this.mBridges.size()));
        pw.printPair("mFinalStatus", Integer.valueOf(this.mFinalStatus));
        pw.printPair("mFinalMessage", this.mFinalMessage);
        pw.printPair("params.isMultiPackage", Boolean.valueOf(this.params.isMultiPackage));
        pw.printPair("params.isStaged", Boolean.valueOf(this.params.isStaged));
        pw.printPair("mParentSessionId", Integer.valueOf(this.mParentSessionId));
        pw.printPair("mChildSessionIds", getChildSessionIdsLocked());
        pw.printPair("mSessionApplied", Boolean.valueOf(this.mSessionApplied));
        pw.printPair("mSessionFailed", Boolean.valueOf(this.mSessionFailed));
        pw.printPair("mSessionReady", Boolean.valueOf(this.mSessionReady));
        pw.printPair("mSessionErrorCode", Integer.valueOf(this.mSessionErrorCode));
        pw.printPair("mSessionErrorMessage", this.mSessionErrorMessage);
        pw.println();
        pw.decreaseIndent();
    }

    private static void sendOnUserActionRequired(Context context, IntentSender target, int sessionId, Intent intent) {
        Intent fillIn = new Intent();
        fillIn.putExtra("android.content.pm.extra.SESSION_ID", sessionId);
        fillIn.putExtra("android.content.pm.extra.STATUS", -1);
        fillIn.putExtra("android.intent.extra.INTENT", intent);
        try {
            BroadcastOptions options = BroadcastOptions.makeBasic();
            options.setPendingIntentBackgroundActivityLaunchAllowed(false);
            target.sendIntent(context, 0, fillIn, null, null, null, options.toBundle());
        } catch (IntentSender.SendIntentException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void sendOnPackageInstalled(Context context, IntentSender target, int sessionId, boolean showNotification, int userId, String basePackageName, int returnCode, String msg, Bundle extras) {
        boolean update = true;
        if (1 == returnCode && showNotification) {
            if (extras == null || !extras.getBoolean("android.intent.extra.REPLACING")) {
                update = false;
            }
            Notification notification = PackageInstallerService.buildSuccessNotification(context, getDeviceOwnerInstalledPackageMsg(context, update), basePackageName, userId);
            if (notification != null) {
                NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
                notificationManager.notify(basePackageName, 21, notification);
            }
        }
        Intent fillIn = new Intent();
        fillIn.putExtra("android.content.pm.extra.PACKAGE_NAME", basePackageName);
        fillIn.putExtra("android.content.pm.extra.SESSION_ID", sessionId);
        fillIn.putExtra("android.content.pm.extra.STATUS", PackageManager.installStatusToPublicStatus(returnCode));
        fillIn.putExtra("android.content.pm.extra.STATUS_MESSAGE", PackageManager.installStatusToString(returnCode, msg));
        fillIn.putExtra("android.content.pm.extra.LEGACY_STATUS", returnCode);
        if (extras != null) {
            String existing = extras.getString("android.content.pm.extra.FAILURE_EXISTING_PACKAGE");
            if (!TextUtils.isEmpty(existing)) {
                fillIn.putExtra("android.content.pm.extra.OTHER_PACKAGE_NAME", existing);
            }
        }
        try {
            BroadcastOptions options = BroadcastOptions.makeBasic();
            options.setPendingIntentBackgroundActivityLaunchAllowed(false);
            target.sendIntent(context, 0, fillIn, null, null, null, options.toBundle());
        } catch (IntentSender.SendIntentException e) {
        }
    }

    private static String getDeviceOwnerInstalledPackageMsg(final Context context, boolean update) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(DevicePolicyManager.class);
        if (update) {
            return dpm.getResources().getString("Core.PACKAGE_UPDATED_BY_DO", new Supplier() { // from class: com.android.server.pm.PackageInstallerSession$$ExternalSyntheticLambda9
                @Override // java.util.function.Supplier
                public final Object get() {
                    String string;
                    string = context.getString(17040951);
                    return string;
                }
            });
        }
        return dpm.getResources().getString("Core.PACKAGE_INSTALLED_BY_DO", new Supplier() { // from class: com.android.server.pm.PackageInstallerSession$$ExternalSyntheticLambda10
            @Override // java.util.function.Supplier
            public final Object get() {
                String string;
                string = context.getString(17040950);
                return string;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void sendPendingStreaming(Context context, IntentSender target, int sessionId, String cause) {
        if (target == null) {
            Slog.e(TAG, "Missing receiver for pending streaming status.");
            return;
        }
        Intent intent = new Intent();
        intent.putExtra("android.content.pm.extra.SESSION_ID", sessionId);
        intent.putExtra("android.content.pm.extra.STATUS", -2);
        if (!TextUtils.isEmpty(cause)) {
            intent.putExtra("android.content.pm.extra.STATUS_MESSAGE", "Staging Image Not Ready [" + cause + "]");
        } else {
            intent.putExtra("android.content.pm.extra.STATUS_MESSAGE", "Staging Image Not Ready");
        }
        try {
            BroadcastOptions options = BroadcastOptions.makeBasic();
            options.setPendingIntentBackgroundActivityLaunchAllowed(false);
            target.sendIntent(context, 0, intent, null, null, null, options.toBundle());
        } catch (IntentSender.SendIntentException e) {
        }
    }

    private static void writeGrantedRuntimePermissionsLocked(TypedXmlSerializer out, String[] grantedRuntimePermissions) throws IOException {
        if (grantedRuntimePermissions != null) {
            for (String permission : grantedRuntimePermissions) {
                out.startTag((String) null, TAG_GRANTED_RUNTIME_PERMISSION);
                XmlUtils.writeStringAttribute(out, "name", permission);
                out.endTag((String) null, TAG_GRANTED_RUNTIME_PERMISSION);
            }
        }
    }

    private static void writeWhitelistedRestrictedPermissionsLocked(TypedXmlSerializer out, List<String> whitelistedRestrictedPermissions) throws IOException {
        if (whitelistedRestrictedPermissions != null) {
            int permissionCount = whitelistedRestrictedPermissions.size();
            for (int i = 0; i < permissionCount; i++) {
                out.startTag((String) null, TAG_WHITELISTED_RESTRICTED_PERMISSION);
                XmlUtils.writeStringAttribute(out, "name", whitelistedRestrictedPermissions.get(i));
                out.endTag((String) null, TAG_WHITELISTED_RESTRICTED_PERMISSION);
            }
        }
    }

    private static void writeAutoRevokePermissionsMode(TypedXmlSerializer out, int mode) throws IOException {
        out.startTag((String) null, TAG_AUTO_REVOKE_PERMISSIONS_MODE);
        out.attributeInt((String) null, "mode", mode);
        out.endTag((String) null, TAG_AUTO_REVOKE_PERMISSIONS_MODE);
    }

    private static File buildAppIconFile(int sessionId, File sessionsDir) {
        return new File(sessionsDir, "app_icon." + sessionId + ".png");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void write(TypedXmlSerializer out, File sessionsDir) throws IOException {
        synchronized (this.mLock) {
            if (!this.mDestroyed || this.params.isStaged) {
                out.startTag((String) null, TAG_SESSION);
                out.attributeInt((String) null, ATTR_SESSION_ID, this.sessionId);
                out.attributeInt((String) null, "userId", this.userId);
                XmlUtils.writeStringAttribute(out, ATTR_INSTALLER_PACKAGE_NAME, this.mInstallSource.installerPackageName);
                XmlUtils.writeStringAttribute(out, ATTR_INSTALLER_ATTRIBUTION_TAG, this.mInstallSource.installerAttributionTag);
                out.attributeInt((String) null, ATTR_INSTALLER_UID, this.mInstallerUid);
                XmlUtils.writeStringAttribute(out, ATTR_INITIATING_PACKAGE_NAME, this.mInstallSource.initiatingPackageName);
                XmlUtils.writeStringAttribute(out, ATTR_ORIGINATING_PACKAGE_NAME, this.mInstallSource.originatingPackageName);
                out.attributeLong((String) null, ATTR_CREATED_MILLIS, this.createdMillis);
                out.attributeLong((String) null, ATTR_UPDATED_MILLIS, this.updatedMillis);
                out.attributeLong((String) null, ATTR_COMMITTED_MILLIS, this.committedMillis);
                File file = this.stageDir;
                if (file != null) {
                    XmlUtils.writeStringAttribute(out, ATTR_SESSION_STAGE_DIR, file.getAbsolutePath());
                }
                String str = this.stageCid;
                if (str != null) {
                    XmlUtils.writeStringAttribute(out, ATTR_SESSION_STAGE_CID, str);
                }
                XmlUtils.writeBooleanAttribute(out, ATTR_PREPARED, this.mPrepared);
                XmlUtils.writeBooleanAttribute(out, ATTR_COMMITTED, this.mCommitted.get());
                XmlUtils.writeBooleanAttribute(out, ATTR_DESTROYED, this.mDestroyed);
                XmlUtils.writeBooleanAttribute(out, ATTR_SEALED, this.mSealed);
                XmlUtils.writeBooleanAttribute(out, ATTR_MULTI_PACKAGE, this.params.isMultiPackage);
                XmlUtils.writeBooleanAttribute(out, ATTR_STAGED_SESSION, this.params.isStaged);
                XmlUtils.writeBooleanAttribute(out, ATTR_IS_READY, this.mSessionReady);
                XmlUtils.writeBooleanAttribute(out, ATTR_IS_FAILED, this.mSessionFailed);
                XmlUtils.writeBooleanAttribute(out, ATTR_IS_APPLIED, this.mSessionApplied);
                out.attributeInt((String) null, ATTR_PACKAGE_SOURCE, this.params.packageSource);
                out.attributeInt((String) null, ATTR_SESSION_ERROR_CODE, this.mSessionErrorCode);
                XmlUtils.writeStringAttribute(out, ATTR_SESSION_ERROR_MESSAGE, this.mSessionErrorMessage);
                out.attributeInt((String) null, ATTR_PARENT_SESSION_ID, this.mParentSessionId);
                out.attributeInt((String) null, "mode", this.params.mode);
                out.attributeInt((String) null, ATTR_INSTALL_FLAGS, this.params.installFlags);
                out.attributeInt((String) null, ATTR_INSTALL_LOCATION, this.params.installLocation);
                out.attributeLong((String) null, ATTR_SIZE_BYTES, this.params.sizeBytes);
                XmlUtils.writeStringAttribute(out, ATTR_APP_PACKAGE_NAME, this.params.appPackageName);
                XmlUtils.writeStringAttribute(out, ATTR_APP_LABEL, this.params.appLabel);
                XmlUtils.writeUriAttribute(out, ATTR_ORIGINATING_URI, this.params.originatingUri);
                out.attributeInt((String) null, ATTR_ORIGINATING_UID, this.params.originatingUid);
                XmlUtils.writeUriAttribute(out, ATTR_REFERRER_URI, this.params.referrerUri);
                XmlUtils.writeStringAttribute(out, ATTR_ABI_OVERRIDE, this.params.abiOverride);
                XmlUtils.writeStringAttribute(out, ATTR_VOLUME_UUID, this.params.volumeUuid);
                out.attributeInt((String) null, ATTR_INSTALL_REASON, this.params.installReason);
                boolean isDataLoader = this.params.dataLoaderParams != null;
                XmlUtils.writeBooleanAttribute(out, ATTR_IS_DATALOADER, isDataLoader);
                if (isDataLoader) {
                    out.attributeInt((String) null, ATTR_DATALOADER_TYPE, this.params.dataLoaderParams.getType());
                    XmlUtils.writeStringAttribute(out, ATTR_DATALOADER_PACKAGE_NAME, this.params.dataLoaderParams.getComponentName().getPackageName());
                    XmlUtils.writeStringAttribute(out, ATTR_DATALOADER_CLASS_NAME, this.params.dataLoaderParams.getComponentName().getClassName());
                    XmlUtils.writeStringAttribute(out, ATTR_DATALOADER_ARGUMENTS, this.params.dataLoaderParams.getArguments());
                }
                writeGrantedRuntimePermissionsLocked(out, this.params.grantedRuntimePermissions);
                writeWhitelistedRestrictedPermissionsLocked(out, this.params.whitelistedRestrictedPermissions);
                writeAutoRevokePermissionsMode(out, this.params.autoRevokePermissionsMode);
                File appIconFile = buildAppIconFile(this.sessionId, sessionsDir);
                if (this.params.appIcon == null && appIconFile.exists()) {
                    appIconFile.delete();
                } else if (this.params.appIcon != null && appIconFile.lastModified() != this.params.appIconLastModified) {
                    Slog.w(TAG, "Writing changed icon " + appIconFile);
                    FileOutputStream os = null;
                    try {
                        os = new FileOutputStream(appIconFile);
                        this.params.appIcon.compress(Bitmap.CompressFormat.PNG, 90, os);
                    } catch (IOException e) {
                        Slog.w(TAG, "Failed to write icon " + appIconFile + ": " + e.getMessage());
                    }
                    IoUtils.closeQuietly(os);
                    this.params.appIconLastModified = appIconFile.lastModified();
                }
                int[] childSessionIds = getChildSessionIdsLocked();
                for (int childSessionId : childSessionIds) {
                    out.startTag((String) null, TAG_CHILD_SESSION);
                    out.attributeInt((String) null, ATTR_SESSION_ID, childSessionId);
                    out.endTag((String) null, TAG_CHILD_SESSION);
                }
                InstallationFile[] files = getInstallationFilesLocked();
                for (InstallationFile file2 : files) {
                    out.startTag((String) null, TAG_SESSION_FILE);
                    out.attributeInt((String) null, ATTR_LOCATION, file2.getLocation());
                    XmlUtils.writeStringAttribute(out, "name", file2.getName());
                    out.attributeLong((String) null, ATTR_LENGTH_BYTES, file2.getLengthBytes());
                    XmlUtils.writeByteArrayAttribute(out, ATTR_METADATA, file2.getMetadata());
                    XmlUtils.writeByteArrayAttribute(out, ATTR_SIGNATURE, file2.getSignature());
                    out.endTag((String) null, TAG_SESSION_FILE);
                }
                int isize = this.mChecksums.size();
                for (int i = 0; i < isize; i++) {
                    String fileName = this.mChecksums.keyAt(i);
                    PerFileChecksum perFileChecksum = this.mChecksums.valueAt(i);
                    Checksum[] checksums = perFileChecksum.getChecksums();
                    int length = checksums.length;
                    int i2 = 0;
                    while (i2 < length) {
                        Checksum checksum = checksums[i2];
                        out.startTag((String) null, TAG_SESSION_CHECKSUM);
                        XmlUtils.writeStringAttribute(out, "name", fileName);
                        out.attributeInt((String) null, ATTR_CHECKSUM_KIND, checksum.getType());
                        XmlUtils.writeByteArrayAttribute(out, ATTR_CHECKSUM_VALUE, checksum.getValue());
                        out.endTag((String) null, TAG_SESSION_CHECKSUM);
                        i2++;
                        isDataLoader = isDataLoader;
                    }
                }
                int isize2 = this.mChecksums.size();
                for (int i3 = 0; i3 < isize2; i3++) {
                    String fileName2 = this.mChecksums.keyAt(i3);
                    PerFileChecksum perFileChecksum2 = this.mChecksums.valueAt(i3);
                    byte[] signature = perFileChecksum2.getSignature();
                    if (signature != null && signature.length != 0) {
                        out.startTag((String) null, TAG_SESSION_CHECKSUM_SIGNATURE);
                        XmlUtils.writeStringAttribute(out, "name", fileName2);
                        XmlUtils.writeByteArrayAttribute(out, ATTR_SIGNATURE, signature);
                        out.endTag((String) null, TAG_SESSION_CHECKSUM_SIGNATURE);
                    }
                }
                out.endTag((String) null, TAG_SESSION);
            }
        }
    }

    private static boolean isStagedSessionStateValid(boolean isReady, boolean isApplied, boolean isFailed) {
        return ((isReady || isApplied || isFailed) && (!isReady || isApplied || isFailed) && ((isReady || !isApplied || isFailed) && (isReady || isApplied || !isFailed))) ? false : true;
    }

    public static PackageInstallerSession readFromXml(TypedXmlPullParser in, PackageInstallerService.InternalCallback callback, Context context, PackageManagerService pm, Looper installerThread, StagingManager stagingManager, File sessionsDir, PackageSessionProvider sessionProvider, SilentUpdatePolicy silentUpdatePolicy) throws IOException, XmlPullParserException {
        boolean isReady;
        boolean isFailed;
        String installOriginatingPackageName;
        int userId;
        int sessionId;
        IntArray childSessionIds;
        List<InstallationFile> list;
        ArrayMap<String, List<Checksum>> checksums;
        ArrayMap<String, byte[]> signatures;
        int type;
        int[] childSessionIdsArray;
        IntArray childSessionIds2;
        int autoRevokePermissionsMode;
        List<String> list2;
        List<String> list3;
        ArrayMap<String, byte[]> signatures2;
        ArrayMap<String, PerFileChecksum> checksumsMap;
        int outerDepth;
        String str;
        String str2;
        String installOriginatingPackageName2;
        int sessionId2;
        IntArray childSessionIds3;
        List<InstallationFile> list4;
        ArrayMap<String, byte[]> signatures3;
        ArrayMap<String, List<Checksum>> checksums2;
        String str3;
        IntArray childSessionIds4;
        String str4;
        int autoRevokePermissionsMode2;
        String str5;
        int sessionId3;
        List<InstallationFile> files;
        String installOriginatingPackageName3;
        ArrayMap<String, List<Checksum>> checksums3;
        ArrayMap<String, byte[]> signatures4;
        String str6 = ATTR_SESSION_ID;
        int sessionId4 = in.getAttributeInt((String) null, ATTR_SESSION_ID);
        int userId2 = in.getAttributeInt((String) null, "userId");
        String installerPackageName = XmlUtils.readStringAttribute(in, ATTR_INSTALLER_PACKAGE_NAME);
        String installerAttributionTag = XmlUtils.readStringAttribute(in, ATTR_INSTALLER_ATTRIBUTION_TAG);
        int installerUid = in.getAttributeInt((String) null, ATTR_INSTALLER_UID, pm.snapshotComputer().getPackageUid(installerPackageName, 8192L, userId2));
        String installInitiatingPackageName = XmlUtils.readStringAttribute(in, ATTR_INITIATING_PACKAGE_NAME);
        String installOriginatingPackageName4 = XmlUtils.readStringAttribute(in, ATTR_ORIGINATING_PACKAGE_NAME);
        long createdMillis = in.getAttributeLong((String) null, ATTR_CREATED_MILLIS);
        in.getAttributeLong((String) null, ATTR_UPDATED_MILLIS);
        long committedMillis = in.getAttributeLong((String) null, ATTR_COMMITTED_MILLIS, 0L);
        String stageDirRaw = XmlUtils.readStringAttribute(in, ATTR_SESSION_STAGE_DIR);
        File stageDir = stageDirRaw != null ? new File(stageDirRaw) : null;
        String stageCid = XmlUtils.readStringAttribute(in, ATTR_SESSION_STAGE_CID);
        boolean prepared = in.getAttributeBoolean((String) null, ATTR_PREPARED, true);
        boolean committed = in.getAttributeBoolean((String) null, ATTR_COMMITTED, false);
        boolean destroyed = in.getAttributeBoolean((String) null, ATTR_DESTROYED, false);
        boolean sealed = in.getAttributeBoolean((String) null, ATTR_SEALED, false);
        int parentSessionId = in.getAttributeInt((String) null, ATTR_PARENT_SESSION_ID, -1);
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(-1);
        params.isMultiPackage = in.getAttributeBoolean((String) null, ATTR_MULTI_PACKAGE, false);
        params.isStaged = in.getAttributeBoolean((String) null, ATTR_STAGED_SESSION, false);
        String str7 = "mode";
        params.mode = in.getAttributeInt((String) null, "mode");
        params.installFlags = in.getAttributeInt((String) null, ATTR_INSTALL_FLAGS);
        params.installLocation = in.getAttributeInt((String) null, ATTR_INSTALL_LOCATION);
        params.sizeBytes = in.getAttributeLong((String) null, ATTR_SIZE_BYTES);
        params.appPackageName = XmlUtils.readStringAttribute(in, ATTR_APP_PACKAGE_NAME);
        params.appIcon = XmlUtils.readBitmapAttribute(in, ATTR_APP_ICON);
        params.appLabel = XmlUtils.readStringAttribute(in, ATTR_APP_LABEL);
        params.originatingUri = XmlUtils.readUriAttribute(in, ATTR_ORIGINATING_URI);
        params.originatingUid = in.getAttributeInt((String) null, ATTR_ORIGINATING_UID, -1);
        params.referrerUri = XmlUtils.readUriAttribute(in, ATTR_REFERRER_URI);
        params.abiOverride = XmlUtils.readStringAttribute(in, ATTR_ABI_OVERRIDE);
        params.volumeUuid = XmlUtils.readStringAttribute(in, ATTR_VOLUME_UUID);
        params.installReason = in.getAttributeInt((String) null, ATTR_INSTALL_REASON);
        params.packageSource = in.getAttributeInt((String) null, ATTR_PACKAGE_SOURCE);
        if (in.getAttributeBoolean((String) null, ATTR_IS_DATALOADER, false)) {
            params.dataLoaderParams = new DataLoaderParams(in.getAttributeInt((String) null, ATTR_DATALOADER_TYPE), new ComponentName(XmlUtils.readStringAttribute(in, ATTR_DATALOADER_PACKAGE_NAME), XmlUtils.readStringAttribute(in, ATTR_DATALOADER_CLASS_NAME)), XmlUtils.readStringAttribute(in, ATTR_DATALOADER_ARGUMENTS));
        }
        File appIconFile = buildAppIconFile(sessionId4, sessionsDir);
        if (appIconFile.exists()) {
            params.appIcon = BitmapFactory.decodeFile(appIconFile.getAbsolutePath());
            params.appIconLastModified = appIconFile.lastModified();
        }
        boolean isReady2 = in.getAttributeBoolean((String) null, ATTR_IS_READY, false);
        boolean isFailed2 = in.getAttributeBoolean((String) null, ATTR_IS_FAILED, false);
        boolean isApplied = in.getAttributeBoolean((String) null, ATTR_IS_APPLIED, false);
        int sessionErrorCode = in.getAttributeInt((String) null, ATTR_SESSION_ERROR_CODE, 0);
        String sessionErrorMessage = XmlUtils.readStringAttribute(in, ATTR_SESSION_ERROR_MESSAGE);
        if (isStagedSessionStateValid(isReady2, isApplied, isFailed2)) {
            List<String> grantedRuntimePermissions = new ArrayList<>();
            List<String> whitelistedRestrictedPermissions = new ArrayList<>();
            int autoRevokePermissionsMode3 = 3;
            IntArray childSessionIds5 = new IntArray();
            List<InstallationFile> files2 = new ArrayList<>();
            ArrayMap<String, List<Checksum>> checksums4 = new ArrayMap<>();
            ArrayMap<String, byte[]> signatures5 = new ArrayMap<>();
            int outerDepth2 = in.getDepth();
            while (true) {
                isReady = isReady2;
                int type2 = in.next();
                isFailed = isFailed2;
                if (type2 == 1) {
                    installOriginatingPackageName = installOriginatingPackageName4;
                    userId = userId2;
                    sessionId = sessionId4;
                    childSessionIds = childSessionIds5;
                    list = files2;
                    checksums = checksums4;
                    signatures = signatures5;
                    type = type2;
                    break;
                }
                userId = userId2;
                if (type2 == 3 && in.getDepth() <= outerDepth2) {
                    installOriginatingPackageName = installOriginatingPackageName4;
                    sessionId = sessionId4;
                    childSessionIds = childSessionIds5;
                    list = files2;
                    signatures = signatures5;
                    type = type2;
                    checksums = checksums4;
                    break;
                }
                if (type2 == 3) {
                    outerDepth = outerDepth2;
                    str = str6;
                    str2 = str7;
                    installOriginatingPackageName2 = installOriginatingPackageName4;
                    sessionId2 = sessionId4;
                    childSessionIds3 = childSessionIds5;
                    list4 = files2;
                    signatures3 = signatures5;
                    checksums2 = checksums4;
                } else if (type2 == 4) {
                    outerDepth = outerDepth2;
                    str = str6;
                    str2 = str7;
                    installOriginatingPackageName2 = installOriginatingPackageName4;
                    sessionId2 = sessionId4;
                    childSessionIds3 = childSessionIds5;
                    list4 = files2;
                    checksums2 = checksums4;
                    signatures3 = signatures5;
                } else {
                    if (TAG_GRANTED_RUNTIME_PERMISSION.equals(in.getName())) {
                        grantedRuntimePermissions.add(XmlUtils.readStringAttribute(in, "name"));
                    }
                    int outerDepth3 = outerDepth2;
                    if (TAG_WHITELISTED_RESTRICTED_PERMISSION.equals(in.getName())) {
                        whitelistedRestrictedPermissions.add(XmlUtils.readStringAttribute(in, "name"));
                    }
                    if (TAG_AUTO_REVOKE_PERMISSIONS_MODE.equals(in.getName())) {
                        autoRevokePermissionsMode3 = in.getAttributeInt((String) null, str7);
                    }
                    if (!TAG_CHILD_SESSION.equals(in.getName())) {
                        str3 = str7;
                        childSessionIds4 = childSessionIds5;
                    } else {
                        str3 = str7;
                        childSessionIds4 = childSessionIds5;
                        childSessionIds4.add(in.getAttributeInt((String) null, str6, -1));
                    }
                    if (!TAG_SESSION_FILE.equals(in.getName())) {
                        str4 = str6;
                        autoRevokePermissionsMode2 = autoRevokePermissionsMode3;
                        str5 = "name";
                        sessionId3 = sessionId4;
                        files = files2;
                    } else {
                        str4 = str6;
                        autoRevokePermissionsMode2 = autoRevokePermissionsMode3;
                        str5 = "name";
                        sessionId3 = sessionId4;
                        files = files2;
                        files.add(new InstallationFile(in.getAttributeInt((String) null, ATTR_LOCATION, 0), XmlUtils.readStringAttribute(in, "name"), in.getAttributeLong((String) null, ATTR_LENGTH_BYTES, -1L), XmlUtils.readByteArrayAttribute(in, ATTR_METADATA), XmlUtils.readByteArrayAttribute(in, ATTR_SIGNATURE)));
                    }
                    if (!TAG_SESSION_CHECKSUM.equals(in.getName())) {
                        installOriginatingPackageName3 = installOriginatingPackageName4;
                        checksums3 = checksums4;
                    } else {
                        String fileName = XmlUtils.readStringAttribute(in, str5);
                        installOriginatingPackageName3 = installOriginatingPackageName4;
                        Checksum checksum = new Checksum(in.getAttributeInt((String) null, ATTR_CHECKSUM_KIND, 0), XmlUtils.readByteArrayAttribute(in, ATTR_CHECKSUM_VALUE));
                        checksums3 = checksums4;
                        List<Checksum> fileChecksums = checksums3.get(fileName);
                        if (fileChecksums == null) {
                            fileChecksums = new ArrayList<>();
                            checksums3.put(fileName, fileChecksums);
                        }
                        fileChecksums.add(checksum);
                    }
                    if (!TAG_SESSION_CHECKSUM_SIGNATURE.equals(in.getName())) {
                        signatures4 = signatures5;
                    } else {
                        String fileName2 = XmlUtils.readStringAttribute(in, str5);
                        byte[] signature = XmlUtils.readByteArrayAttribute(in, ATTR_SIGNATURE);
                        signatures4 = signatures5;
                        signatures4.put(fileName2, signature);
                    }
                    childSessionIds5 = childSessionIds4;
                    signatures5 = signatures4;
                    checksums4 = checksums3;
                    files2 = files;
                    userId2 = userId;
                    installOriginatingPackageName4 = installOriginatingPackageName3;
                    str7 = str3;
                    isReady2 = isReady;
                    isFailed2 = isFailed;
                    str6 = str4;
                    autoRevokePermissionsMode3 = autoRevokePermissionsMode2;
                    outerDepth2 = outerDepth3;
                    sessionId4 = sessionId3;
                }
                childSessionIds5 = childSessionIds3;
                signatures5 = signatures3;
                checksums4 = checksums2;
                files2 = list4;
                userId2 = userId;
                installOriginatingPackageName4 = installOriginatingPackageName2;
                str7 = str2;
                isReady2 = isReady;
                isFailed2 = isFailed;
                str6 = str;
                outerDepth2 = outerDepth;
                sessionId4 = sessionId2;
            }
            int outerDepth4 = grantedRuntimePermissions.size();
            if (outerDepth4 > 0) {
                params.grantedRuntimePermissions = (String[]) grantedRuntimePermissions.toArray(EmptyArray.STRING);
            }
            if (whitelistedRestrictedPermissions.size() > 0) {
                params.whitelistedRestrictedPermissions = whitelistedRestrictedPermissions;
            }
            params.autoRevokePermissionsMode = autoRevokePermissionsMode3;
            if (childSessionIds.size() > 0) {
                childSessionIdsArray = new int[childSessionIds.size()];
                int size = childSessionIds.size();
                for (int i = 0; i < size; i++) {
                    childSessionIdsArray[i] = childSessionIds.get(i);
                }
            } else {
                childSessionIdsArray = EMPTY_CHILD_SESSION_ARRAY;
            }
            InstallationFile[] fileArray = null;
            if (!list.isEmpty()) {
                fileArray = (InstallationFile[]) list.toArray(EMPTY_INSTALLATION_FILE_ARRAY);
            }
            if (checksums.isEmpty()) {
                childSessionIds2 = childSessionIds;
                autoRevokePermissionsMode = autoRevokePermissionsMode3;
                list2 = whitelistedRestrictedPermissions;
                list3 = grantedRuntimePermissions;
                signatures2 = signatures;
                checksumsMap = null;
            } else {
                ArrayMap<String, PerFileChecksum> checksumsMap2 = new ArrayMap<>(checksums.size());
                int i2 = 0;
                int isize = checksums.size();
                while (i2 < isize) {
                    IntArray childSessionIds6 = childSessionIds;
                    String fileName3 = checksums.keyAt(i2);
                    int autoRevokePermissionsMode4 = autoRevokePermissionsMode3;
                    List<Checksum> perFileChecksum = checksums.valueAt(i2);
                    List<String> whitelistedRestrictedPermissions2 = whitelistedRestrictedPermissions;
                    byte[] perFileSignature = signatures.get(fileName3);
                    checksumsMap2.put(fileName3, new PerFileChecksum((Checksum[]) perFileChecksum.toArray(new Checksum[perFileChecksum.size()]), perFileSignature));
                    i2++;
                    grantedRuntimePermissions = grantedRuntimePermissions;
                    childSessionIds = childSessionIds6;
                    autoRevokePermissionsMode3 = autoRevokePermissionsMode4;
                    whitelistedRestrictedPermissions = whitelistedRestrictedPermissions2;
                    signatures = signatures;
                }
                childSessionIds2 = childSessionIds;
                autoRevokePermissionsMode = autoRevokePermissionsMode3;
                list2 = whitelistedRestrictedPermissions;
                list3 = grantedRuntimePermissions;
                signatures2 = signatures;
                checksumsMap = checksumsMap2;
            }
            InstallSource installSource = InstallSource.create(installInitiatingPackageName, installOriginatingPackageName, installerPackageName, installerAttributionTag, params.packageSource);
            return new PackageInstallerSession(callback, context, pm, sessionProvider, silentUpdatePolicy, installerThread, stagingManager, sessionId, userId, installerUid, installSource, params, createdMillis, committedMillis, stageDir, stageCid, fileArray, checksumsMap, prepared, committed, destroyed, sealed, childSessionIdsArray, parentSessionId, isReady, isFailed, isApplied, sessionErrorCode, sessionErrorMessage);
        }
        throw new IllegalArgumentException("Can't restore staged session with invalid state.");
    }
}
