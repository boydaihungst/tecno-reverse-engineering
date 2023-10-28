package com.android.server.pm;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.BroadcastOptions;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PackageDeleteObserver;
import android.app.admin.DevicePolicyEventLogger;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyManagerInternal;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageInstaller;
import android.content.pm.IPackageInstallerCallback;
import android.content.pm.IPackageInstallerSession;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.VersionedPackage;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.system.ErrnoException;
import android.system.Os;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.ExceptionUtils;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.content.InstallLocationUtils;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.ImageUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.IoThread;
import com.android.server.LocalServices;
import com.android.server.SystemConfig;
import com.android.server.SystemService;
import com.android.server.SystemServiceManager;
import com.android.server.am.HostingRecord;
import com.android.server.pm.PackageInstallerService;
import com.android.server.pm.StagingManager;
import com.android.server.pm.parsing.PackageParser2;
import com.android.server.pm.utils.RequestThrottle;
import com.android.server.usb.descriptors.UsbEndpointDescriptor;
import com.mediatek.server.MtkSystemServiceFactory;
import com.mediatek.server.powerhal.PowerHalManager;
import com.transsion.hubcore.sru.ITranSruManager;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class PackageInstallerService extends IPackageInstaller.Stub implements PackageSessionProvider {
    private static final int ADB_DEV_MODE = 36;
    private static final long MAX_ACTIVE_SESSIONS_NO_PERMISSION = 50;
    private static final long MAX_ACTIVE_SESSIONS_WITH_PERMISSION = 1024;
    private static final long MAX_AGE_MILLIS = 259200000;
    private static final long MAX_HISTORICAL_SESSIONS = 1048576;
    private static final long MAX_SESSION_AGE_ON_LOW_STORAGE_MILLIS = 28800000;
    private static final long MAX_TIME_SINCE_UPDATE_MILLIS = 1814400000;
    private static final String TAG_SESSIONS = "sessions";
    private final ApexManager mApexManager;
    private AppOpsManager mAppOps;
    private final Callbacks mCallbacks;
    private final Context mContext;
    private final Handler mInstallHandler;
    private final HandlerThread mInstallThread;
    private final PackageManagerService mPm;
    private final PackageSessionVerifier mSessionVerifier;
    private final File mSessionsDir;
    private final AtomicFile mSessionsFile;
    private final StagingManager mStagingManager;
    private static final String TAG = "PackageInstaller";
    private static final boolean LOGD = Log.isLoggable(TAG, 3);
    private static final boolean DEBUG = Build.IS_DEBUGGABLE;
    private static final FilenameFilter sStageFilter = new FilenameFilter() { // from class: com.android.server.pm.PackageInstallerService.1
        @Override // java.io.FilenameFilter
        public boolean accept(File dir, String name) {
            return PackageInstallerService.isStageName(name);
        }
    };
    private PowerHalManager mPowerHalManager = MtkSystemServiceFactory.getInstance().makePowerHalManager();
    private volatile boolean mOkToSendBroadcasts = false;
    private volatile boolean mBypassNextStagedInstallerCheck = false;
    private volatile boolean mBypassNextAllowedApexUpdateCheck = false;
    private final InternalCallback mInternalCallback = new InternalCallback();
    private final Random mRandom = new SecureRandom();
    private final SparseBooleanArray mAllocatedSessions = new SparseBooleanArray();
    private final SparseArray<PackageInstallerSession> mSessions = new SparseArray<>();
    private final List<String> mHistoricalSessions = new ArrayList();
    private final SparseIntArray mHistoricalSessionsByInstaller = new SparseIntArray();
    private final SparseBooleanArray mLegacySessions = new SparseBooleanArray();
    private final SilentUpdatePolicy mSilentUpdatePolicy = new SilentUpdatePolicy();
    private final RequestThrottle mSettingsWriteRequest = new RequestThrottle(IoThread.getHandler(), new Supplier() { // from class: com.android.server.pm.PackageInstallerService$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return PackageInstallerService.this.m5467lambda$new$0$comandroidserverpmPackageInstallerService();
        }
    });

    /* loaded from: classes2.dex */
    private static final class Lifecycle extends SystemService {
        private final PackageInstallerService mPackageInstallerService;

        Lifecycle(Context context, PackageInstallerService service) {
            super(context);
            this.mPackageInstallerService = service;
        }

        @Override // com.android.server.SystemService
        public void onStart() {
        }

        @Override // com.android.server.SystemService
        public void onBootPhase(int phase) {
            if (phase == 550) {
                this.mPackageInstallerService.onBroadcastReady();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-pm-PackageInstallerService  reason: not valid java name */
    public /* synthetic */ Boolean m5467lambda$new$0$comandroidserverpmPackageInstallerService() {
        Boolean valueOf;
        synchronized (this.mSessions) {
            valueOf = Boolean.valueOf(writeSessionsLocked());
        }
        return valueOf;
    }

    public PackageInstallerService(Context context, PackageManagerService pm, Supplier<PackageParser2> apexParserSupplier) {
        this.mContext = context;
        this.mPm = pm;
        HandlerThread handlerThread = new HandlerThread(TAG);
        this.mInstallThread = handlerThread;
        handlerThread.start();
        this.mInstallHandler = new Handler(handlerThread.getLooper());
        this.mCallbacks = new Callbacks(handlerThread.getLooper());
        this.mSessionsFile = new AtomicFile(new File(Environment.getDataSystemDirectory(), "install_sessions.xml"), "package-session");
        File file = new File(Environment.getDataSystemDirectory(), "install_sessions");
        this.mSessionsDir = file;
        file.mkdirs();
        ApexManager apexManager = ApexManager.getInstance();
        this.mApexManager = apexManager;
        this.mStagingManager = new StagingManager(context);
        this.mSessionVerifier = new PackageSessionVerifier(context, pm, apexManager, apexParserSupplier, handlerThread.getLooper());
        ((SystemServiceManager) LocalServices.getService(SystemServiceManager.class)).startService(new Lifecycle(context, this));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public StagingManager getStagingManager() {
        return this.mStagingManager;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean okToSendBroadcasts() {
        return this.mOkToSendBroadcasts;
    }

    public void systemReady() {
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        this.mStagingManager.systemReady();
        synchronized (this.mSessions) {
            readSessionsLocked();
            expireSessionsLocked();
            reconcileStagesLocked(StorageManager.UUID_PRIVATE_INTERNAL);
            ArraySet<File> unclaimedIcons = newArraySet(this.mSessionsDir.listFiles());
            for (int i = 0; i < this.mSessions.size(); i++) {
                PackageInstallerSession session = this.mSessions.valueAt(i);
                unclaimedIcons.remove(buildAppIconFile(session.sessionId));
            }
            Iterator<File> it = unclaimedIcons.iterator();
            while (it.hasNext()) {
                File icon = it.next();
                Slog.w(TAG, "Deleting orphan icon " + icon);
                icon.delete();
            }
            this.mSettingsWriteRequest.runNow();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onBroadcastReady() {
        this.mOkToSendBroadcasts = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void restoreAndApplyStagedSessionIfNeeded() {
        List<StagingManager.StagedSession> stagedSessionsToRestore = new ArrayList<>();
        synchronized (this.mSessions) {
            for (int i = 0; i < this.mSessions.size(); i++) {
                PackageInstallerSession session = this.mSessions.valueAt(i);
                if (session.isStaged()) {
                    StagingManager.StagedSession stagedSession = session.mStagedSession;
                    if (!stagedSession.isInTerminalState() && stagedSession.hasParentSessionId() && getSession(stagedSession.getParentSessionId()) == null) {
                        stagedSession.setSessionFailed(UsbEndpointDescriptor.MASK_ENDPOINT_DIRECTION, "An orphan staged session " + stagedSession.sessionId() + " is found, parent " + stagedSession.getParentSessionId() + " is missing");
                    } else if (!stagedSession.hasParentSessionId() && stagedSession.isCommitted() && !stagedSession.isInTerminalState()) {
                        stagedSessionsToRestore.add(stagedSession);
                    }
                }
            }
        }
        this.mStagingManager.restoreSessions(stagedSessionsToRestore, this.mPm.isDeviceUpgrading());
    }

    private void reconcileStagesLocked(String volumeUuid) {
        ArraySet<File> unclaimedStages = getStagingDirsOnVolume(volumeUuid);
        for (int i = 0; i < this.mSessions.size(); i++) {
            PackageInstallerSession session = this.mSessions.valueAt(i);
            unclaimedStages.remove(session.stageDir);
        }
        removeStagingDirs(unclaimedStages);
    }

    private ArraySet<File> getStagingDirsOnVolume(String volumeUuid) {
        File stagingDir = getTmpSessionDir(volumeUuid);
        ArraySet<File> stagingDirs = newArraySet(stagingDir.listFiles(sStageFilter));
        File stagedSessionStagingDir = Environment.getDataStagingDirectory(volumeUuid);
        stagingDirs.addAll(newArraySet(stagedSessionStagingDir.listFiles()));
        return stagingDirs;
    }

    private void removeStagingDirs(ArraySet<File> stagingDirsToRemove) {
        RemovePackageHelper removePackageHelper = new RemovePackageHelper(this.mPm);
        Iterator<File> it = stagingDirsToRemove.iterator();
        while (it.hasNext()) {
            File stage = it.next();
            Slog.w(TAG, "Deleting orphan stage " + stage);
            synchronized (this.mPm.mInstallLock) {
                removePackageHelper.removeCodePathLI(stage);
            }
        }
    }

    public void onPrivateVolumeMounted(String volumeUuid) {
        synchronized (this.mSessions) {
            reconcileStagesLocked(volumeUuid);
        }
    }

    public void freeStageDirs(String volumeUuid) {
        ArraySet<File> unclaimedStagingDirsOnVolume = getStagingDirsOnVolume(volumeUuid);
        long currentTimeMillis = System.currentTimeMillis();
        synchronized (this.mSessions) {
            for (int i = 0; i < this.mSessions.size(); i++) {
                PackageInstallerSession session = this.mSessions.valueAt(i);
                if (unclaimedStagingDirsOnVolume.contains(session.stageDir)) {
                    long age = currentTimeMillis - session.createdMillis;
                    if (age >= MAX_SESSION_AGE_ON_LOW_STORAGE_MILLIS) {
                        PackageInstallerSession root = !session.hasParentSessionId() ? session : this.mSessions.get(session.getParentSessionId());
                        if (root == null) {
                            Slog.e(TAG, "freeStageDirs: found an orphaned session: " + session.sessionId + " parent=" + session.getParentSessionId());
                        } else if (!root.isDestroyed()) {
                            root.abandon();
                        }
                    } else {
                        unclaimedStagingDirsOnVolume.remove(session.stageDir);
                    }
                }
            }
        }
        removeStagingDirs(unclaimedStagingDirsOnVolume);
    }

    @Deprecated
    public File allocateStageDirLegacy(String volumeUuid, boolean isEphemeral) throws IOException {
        File sessionStageDir;
        synchronized (this.mSessions) {
            try {
                try {
                    int sessionId = allocateSessionIdLocked();
                    this.mLegacySessions.put(sessionId, true);
                    sessionStageDir = buildTmpSessionDir(sessionId, volumeUuid);
                    prepareStageDir(sessionStageDir);
                } catch (IllegalStateException e) {
                    throw new IOException(e);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return sessionStageDir;
    }

    @Deprecated
    public String allocateExternalStageCidLegacy() {
        String str;
        synchronized (this.mSessions) {
            int sessionId = allocateSessionIdLocked();
            this.mLegacySessions.put(sessionId, true);
            str = "smdl" + sessionId + ".tmp";
        }
        return str;
    }

    private void readSessionsLocked() {
        if (LOGD) {
            Slog.v(TAG, "readSessionsLocked()");
        }
        this.mSessions.clear();
        FileInputStream fis = null;
        try {
            try {
                try {
                    fis = this.mSessionsFile.openRead();
                    TypedXmlPullParser in = Xml.resolvePullParser(fis);
                    while (true) {
                        int type = in.next();
                        if (type == 1) {
                            break;
                        } else if (type == 2) {
                            String tag = in.getName();
                            if ("session".equals(tag)) {
                                try {
                                    PackageInstallerSession session = PackageInstallerSession.readFromXml(in, this.mInternalCallback, this.mContext, this.mPm, this.mInstallThread.getLooper(), this.mStagingManager, this.mSessionsDir, this, this.mSilentUpdatePolicy);
                                    this.mSessions.put(session.sessionId, session);
                                    this.mAllocatedSessions.put(session.sessionId, true);
                                } catch (Exception e) {
                                    Slog.e(TAG, "Could not read session", e);
                                }
                            }
                        }
                    }
                } catch (FileNotFoundException e2) {
                }
            } catch (IOException | XmlPullParserException e3) {
                Slog.wtf(TAG, "Failed reading install sessions", e3);
            }
            IoUtils.closeQuietly(fis);
            for (int i = 0; i < this.mSessions.size(); i++) {
                this.mSessions.valueAt(i).onAfterSessionRead(this.mSessions);
            }
        } catch (Throwable th) {
            IoUtils.closeQuietly(fis);
            throw th;
        }
    }

    private void expireSessionsLocked() {
        boolean valid;
        SparseArray<PackageInstallerSession> tmp = this.mSessions.clone();
        int n = tmp.size();
        for (int i = 0; i < n; i++) {
            PackageInstallerSession session = tmp.valueAt(i);
            if (!session.hasParentSessionId()) {
                long age = System.currentTimeMillis() - session.createdMillis;
                long timeSinceUpdate = System.currentTimeMillis() - session.getUpdatedMillis();
                if (session.isStaged()) {
                    valid = !session.isStagedAndInTerminalState() || timeSinceUpdate < MAX_TIME_SINCE_UPDATE_MILLIS;
                } else if (age >= MAX_AGE_MILLIS) {
                    Slog.w(TAG, "Abandoning old session created at " + session.createdMillis);
                    valid = false;
                } else {
                    valid = true;
                }
                if (!valid) {
                    Slog.w(TAG, "Remove old session: " + session.sessionId);
                    removeActiveSession(session);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeActiveSession(PackageInstallerSession session) {
        this.mSessions.remove(session.sessionId);
        addHistoricalSessionLocked(session);
        for (PackageInstallerSession child : session.getChildSessions()) {
            this.mSessions.remove(child.sessionId);
            addHistoricalSessionLocked(child);
        }
    }

    private void addHistoricalSessionLocked(PackageInstallerSession session) {
        CharArrayWriter writer = new CharArrayWriter();
        IndentingPrintWriter pw = new IndentingPrintWriter(writer, "    ");
        session.dump(pw);
        this.mHistoricalSessions.add(writer.toString());
        int installerUid = session.getInstallerUid();
        SparseIntArray sparseIntArray = this.mHistoricalSessionsByInstaller;
        sparseIntArray.put(installerUid, sparseIntArray.get(installerUid) + 1);
    }

    private boolean writeSessionsLocked() {
        if (LOGD) {
            Slog.v(TAG, "writeSessionsLocked()");
        }
        FileOutputStream fos = null;
        try {
            fos = this.mSessionsFile.startWrite();
            TypedXmlSerializer out = Xml.resolveSerializer(fos);
            out.startDocument((String) null, true);
            out.startTag((String) null, TAG_SESSIONS);
            int size = this.mSessions.size();
            for (int i = 0; i < size; i++) {
                PackageInstallerSession session = this.mSessions.valueAt(i);
                session.write(out, this.mSessionsDir);
            }
            out.endTag((String) null, TAG_SESSIONS);
            out.endDocument();
            this.mSessionsFile.finishWrite(fos);
            return true;
        } catch (IOException e) {
            if (fos != null) {
                this.mSessionsFile.failWrite(fos);
                return false;
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public File buildAppIconFile(int sessionId) {
        return new File(this.mSessionsDir, "app_icon." + sessionId + ".png");
    }

    public int createSession(PackageInstaller.SessionParams params, String installerPackageName, String callingAttributionTag, int userId) {
        try {
            return createSessionInternal(params, installerPackageName, callingAttributionTag, userId);
        } catch (IOException e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [851=5] */
    private int createSessionInternal(PackageInstaller.SessionParams params, String installerPackageName, String installerAttributionTag, int userId) throws IOException {
        String installerPackageName2;
        String originatingPackageName;
        File stageDir;
        String stageCid;
        String[] packages;
        int callingUid = Binder.getCallingUid();
        Computer snapshot = this.mPm.snapshotComputer();
        snapshot.enforceCrossUserPermission(callingUid, userId, true, true, "createSession");
        if (this.mPm.isUserRestricted(userId, "no_install_apps")) {
            boolean ignore = IPackageManagerServiceLice.Instance().ignoreInstallUserRestriction(params.appPackageName, callingUid, false);
            if (!ignore) {
                throw new SecurityException("User restriction prevents installing");
            }
        }
        if (params.dataLoaderParams == null || this.mContext.checkCallingOrSelfPermission("com.android.permission.USE_INSTALLER_V2") == 0) {
            if (params.installReason != 5 || this.mContext.checkCallingOrSelfPermission("android.permission.MANAGE_ROLLBACKS") == 0 || this.mContext.checkCallingOrSelfPermission("android.permission.TEST_MANAGE_ROLLBACKS") == 0) {
                if (params.appPackageName != null && params.appPackageName.length() > 255) {
                    params.appPackageName = null;
                }
                params.appLabel = (String) TextUtils.trimToSize(params.appLabel, 1000);
                String requestedInstallerPackageName = (params.installerPackageName == null || params.installerPackageName.length() >= 255) ? installerPackageName : params.installerPackageName;
                if (callingUid == 2000 || callingUid == 0 || PackageInstallerSession.isSystemDataLoaderInstallation(params)) {
                    params.installFlags |= 32;
                    installerPackageName2 = null;
                } else {
                    if (callingUid != 1000) {
                        this.mAppOps.checkPackage(callingUid, installerPackageName);
                    }
                    if (!TextUtils.equals(requestedInstallerPackageName, installerPackageName) && this.mContext.checkCallingOrSelfPermission("android.permission.INSTALL_PACKAGES") != 0) {
                        this.mAppOps.checkPackage(callingUid, requestedInstallerPackageName);
                    }
                    params.installFlags &= -33;
                    params.installFlags &= -65;
                    params.installFlags |= 2;
                    if ((params.installFlags & 65536) != 0 && !this.mPm.isCallerVerifier(snapshot, callingUid)) {
                        params.installFlags &= -65537;
                    }
                    if (this.mContext.checkCallingOrSelfPermission("android.permission.INSTALL_TEST_ONLY_PACKAGE") != 0) {
                        params.installFlags &= -5;
                    }
                    installerPackageName2 = installerPackageName;
                }
                if (params.originatingUid == -1 || params.originatingUid == callingUid || (packages = snapshot.getPackagesForUid(params.originatingUid)) == null || packages.length <= 0) {
                    originatingPackageName = null;
                } else {
                    String originatingPackageName2 = packages[0];
                    originatingPackageName = originatingPackageName2;
                }
                if (Build.IS_DEBUGGABLE || isCalledBySystemOrShell(callingUid)) {
                    params.installFlags |= 1048576;
                } else {
                    params.installFlags &= -1048577;
                    params.installFlags &= -129;
                }
                if ((params.installFlags & 36) != 36) {
                    params.installFlags &= -524289;
                }
                boolean isApex = (params.installFlags & 131072) != 0;
                if (isApex) {
                    if (this.mContext.checkCallingOrSelfPermission("android.permission.INSTALL_PACKAGE_UPDATES") == -1 && this.mContext.checkCallingOrSelfPermission("android.permission.INSTALL_PACKAGES") == -1) {
                        throw new SecurityException("Not allowed to perform APEX updates");
                    }
                } else if (params.isStaged) {
                    this.mContext.enforceCallingOrSelfPermission("android.permission.INSTALL_PACKAGES", TAG);
                }
                if (isApex) {
                    if (!this.mApexManager.isApexSupported()) {
                        throw new IllegalArgumentException("This device doesn't support the installation of APEX files");
                    }
                    if (params.isMultiPackage) {
                        throw new IllegalArgumentException("A multi-session can't be set as APEX.");
                    }
                    if (isCalledBySystemOrShell(callingUid) || this.mBypassNextAllowedApexUpdateCheck) {
                        params.installFlags |= 4194304;
                    } else {
                        params.installFlags &= -4194305;
                    }
                }
                if ((params.installFlags & 2048) == 0 || isCalledBySystemOrShell(callingUid) || (snapshot.getFlagsForUid(callingUid) & 1) != 0) {
                    if (!params.isStaged || isCalledBySystemOrShell(callingUid) || this.mBypassNextStagedInstallerCheck || isStagedInstallerAllowed(requestedInstallerPackageName)) {
                        if (!isApex || isCalledBySystemOrShell(callingUid) || this.mBypassNextStagedInstallerCheck || isStagedInstallerAllowed(requestedInstallerPackageName)) {
                            this.mBypassNextStagedInstallerCheck = false;
                            this.mBypassNextAllowedApexUpdateCheck = false;
                            if (!params.isMultiPackage) {
                                if ((params.installFlags & 256) == 0 || this.mContext.checkCallingOrSelfPermission("android.permission.INSTALL_GRANT_RUNTIME_PERMISSIONS") != -1) {
                                    if (params.appIcon != null) {
                                        ActivityManager am = (ActivityManager) this.mContext.getSystemService(HostingRecord.HOSTING_TYPE_ACTIVITY);
                                        int iconSize = am.getLauncherLargeIconSize();
                                        if (params.appIcon.getWidth() > iconSize * 2 || params.appIcon.getHeight() > iconSize * 2) {
                                            params.appIcon = Bitmap.createScaledBitmap(params.appIcon, iconSize, iconSize, true);
                                        }
                                    }
                                    switch (params.mode) {
                                        case 1:
                                        case 2:
                                            if ((params.installFlags & 16) != 0) {
                                                if (!InstallLocationUtils.fitsOnInternal(this.mContext, params)) {
                                                    throw new IOException("No suitable internal storage available");
                                                }
                                            } else if ((params.installFlags & 512) != 0) {
                                                params.installFlags |= 16;
                                                break;
                                            } else {
                                                params.installFlags |= 16;
                                                long ident = Binder.clearCallingIdentity();
                                                try {
                                                    params.volumeUuid = InstallLocationUtils.resolveInstallVolume(this.mContext, params);
                                                    break;
                                                } finally {
                                                    Binder.restoreCallingIdentity(ident);
                                                }
                                            }
                                            break;
                                        default:
                                            throw new IllegalArgumentException("Invalid install mode: " + params.mode);
                                    }
                                } else {
                                    throw new SecurityException("You need the android.permission.INSTALL_GRANT_RUNTIME_PERMISSIONS permission to use the PackageManager.INSTALL_GRANT_RUNTIME_PERMISSIONS flag");
                                }
                            }
                            synchronized (this.mSessions) {
                                try {
                                    try {
                                        int activeCount = getSessionCount(this.mSessions, callingUid);
                                        if (this.mContext.checkCallingOrSelfPermission("android.permission.INSTALL_PACKAGES") == 0) {
                                            if (activeCount >= 1024) {
                                                try {
                                                    throw new IllegalStateException("Too many active sessions for UID " + callingUid);
                                                } catch (Throwable th) {
                                                    th = th;
                                                }
                                            }
                                        } else if (activeCount >= MAX_ACTIVE_SESSIONS_NO_PERMISSION) {
                                            throw new IllegalStateException("Too many active sessions for UID " + callingUid);
                                        }
                                        int historicalCount = this.mHistoricalSessionsByInstaller.get(callingUid);
                                        if (historicalCount < MAX_HISTORICAL_SESSIONS) {
                                            try {
                                                int sessionId = allocateSessionIdLocked();
                                                long createdMillis = System.currentTimeMillis();
                                                if (params.isMultiPackage) {
                                                    stageDir = null;
                                                    stageCid = null;
                                                } else if ((params.installFlags & 16) != 0) {
                                                    File stageDir2 = buildSessionDir(sessionId, params);
                                                    stageDir = stageDir2;
                                                    stageCid = null;
                                                } else {
                                                    String stageCid2 = buildExternalStageCid(sessionId);
                                                    stageDir = null;
                                                    stageCid = stageCid2;
                                                }
                                                if (params.forceQueryableOverride && callingUid != 2000 && callingUid != 0) {
                                                    params.forceQueryableOverride = false;
                                                }
                                                InstallSource installSource = InstallSource.create(installerPackageName2, originatingPackageName, requestedInstallerPackageName, installerAttributionTag, params.packageSource);
                                                PackageInstallerSession session = new PackageInstallerSession(this.mInternalCallback, this.mContext, this.mPm, this, this.mSilentUpdatePolicy, this.mInstallThread.getLooper(), this.mStagingManager, sessionId, userId, callingUid, installSource, params, createdMillis, 0L, stageDir, stageCid, null, null, false, false, false, false, null, -1, false, false, false, 0, "");
                                                synchronized (this.mSessions) {
                                                    try {
                                                    } catch (Throwable th2) {
                                                        th = th2;
                                                    }
                                                    try {
                                                        this.mSessions.put(sessionId, session);
                                                        this.mPm.addInstallerPackageName(session.getInstallSource());
                                                        this.mCallbacks.notifySessionCreated(session.sessionId, session.userId);
                                                        this.mSettingsWriteRequest.schedule();
                                                        if (LOGD) {
                                                            Slog.d(TAG, "Created session id=" + sessionId + " staged=" + params.isStaged);
                                                        }
                                                        return sessionId;
                                                    } catch (Throwable th3) {
                                                        th = th3;
                                                        while (true) {
                                                            try {
                                                                throw th;
                                                            } catch (Throwable th4) {
                                                                th = th4;
                                                            }
                                                        }
                                                    }
                                                }
                                            } catch (Throwable th5) {
                                                th = th5;
                                            }
                                        } else {
                                            try {
                                                throw new IllegalStateException("Too many historical sessions for UID " + callingUid);
                                            } catch (Throwable th6) {
                                                th = th6;
                                            }
                                        }
                                    } catch (Throwable th7) {
                                        th = th7;
                                    }
                                } catch (Throwable th8) {
                                    th = th8;
                                }
                                throw th;
                            }
                        }
                        throw new SecurityException("Installer not allowed to commit non-staged APEX install");
                    }
                    throw new SecurityException("Installer not allowed to commit staged install");
                }
                throw new SecurityException("Only system apps could use the PackageManager.INSTALL_INSTANT_APP flag.");
            }
            throw new SecurityException("INSTALL_REASON_ROLLBACK requires the MANAGE_ROLLBACKS permission or the TEST_MANAGE_ROLLBACKS permission");
        }
        throw new SecurityException("You need the com.android.permission.USE_INSTALLER_V2 permission to use a data loader");
    }

    private boolean isCalledBySystemOrShell(int callingUid) {
        return callingUid == 1000 || callingUid == 0 || callingUid == 2000;
    }

    private boolean isStagedInstallerAllowed(String installerName) {
        return SystemConfig.getInstance().getWhitelistedStagedInstallers().contains(installerName) || ITranSruManager.Instance().isValidCallingUid(installerName);
    }

    public void updateSessionAppIcon(int sessionId, Bitmap appIcon) {
        synchronized (this.mSessions) {
            PackageInstallerSession session = this.mSessions.get(sessionId);
            if (session == null || !isCallingUidOwner(session)) {
                throw new SecurityException("Caller has no access to session " + sessionId);
            }
            if (appIcon != null) {
                ActivityManager am = (ActivityManager) this.mContext.getSystemService(HostingRecord.HOSTING_TYPE_ACTIVITY);
                int iconSize = am.getLauncherLargeIconSize();
                if (appIcon.getWidth() > iconSize * 2 || appIcon.getHeight() > iconSize * 2) {
                    appIcon = Bitmap.createScaledBitmap(appIcon, iconSize, iconSize, true);
                }
            }
            session.params.appIcon = appIcon;
            session.params.appIconLastModified = -1L;
            this.mInternalCallback.onSessionBadgingChanged(session);
        }
    }

    public void updateSessionAppLabel(int sessionId, String appLabel) {
        synchronized (this.mSessions) {
            PackageInstallerSession session = this.mSessions.get(sessionId);
            if (session == null || !isCallingUidOwner(session)) {
                throw new SecurityException("Caller has no access to session " + sessionId);
            }
            session.params.appLabel = appLabel;
            this.mInternalCallback.onSessionBadgingChanged(session);
        }
    }

    public void abandonSession(int sessionId) {
        synchronized (this.mSessions) {
            PackageInstallerSession session = this.mSessions.get(sessionId);
            if (session == null || !isCallingUidOwner(session)) {
                throw new SecurityException("Caller has no access to session " + sessionId);
            }
            session.abandon();
        }
    }

    public IPackageInstallerSession openSession(int sessionId) {
        try {
            PowerHalManager powerHalManager = this.mPowerHalManager;
            if (powerHalManager != null) {
                powerHalManager.setInstallationBoost(true);
            }
            return openSessionInternal(sessionId);
        } catch (IOException e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    private boolean checkOpenSessionAccess(PackageInstallerSession session) {
        if (session == null) {
            return false;
        }
        if (isCallingUidOwner(session)) {
            return true;
        }
        return session.isSealed() && this.mContext.checkCallingOrSelfPermission("android.permission.PACKAGE_VERIFICATION_AGENT") == 0;
    }

    private IPackageInstallerSession openSessionInternal(int sessionId) throws IOException {
        PackageInstallerSession session;
        synchronized (this.mSessions) {
            session = this.mSessions.get(sessionId);
            if (!checkOpenSessionAccess(session)) {
                throw new SecurityException("Caller has no access to session " + sessionId);
            }
            session.open();
        }
        return session;
    }

    private int allocateSessionIdLocked() {
        int n = 0;
        while (true) {
            int sessionId = this.mRandom.nextInt(2147483646) + 1;
            if (!this.mAllocatedSessions.get(sessionId, false)) {
                this.mAllocatedSessions.put(sessionId, true);
                return sessionId;
            }
            int n2 = n + 1;
            if (n >= 32) {
                throw new IllegalStateException("Failed to allocate session ID");
            }
            n = n2;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isStageName(String name) {
        boolean isFile = name.startsWith("vmdl") && name.endsWith(".tmp");
        boolean isContainer = name.startsWith("smdl") && name.endsWith(".tmp");
        boolean isLegacyContainer = name.startsWith("smdl2tmp");
        return isFile || isContainer || isLegacyContainer;
    }

    static int tryParseSessionId(String tmpSessionDir) throws IllegalArgumentException {
        if (!tmpSessionDir.startsWith("vmdl") || !tmpSessionDir.endsWith(".tmp")) {
            throw new IllegalArgumentException("Not a temporary session directory");
        }
        String sessionId = tmpSessionDir.substring("vmdl".length(), tmpSessionDir.length() - ".tmp".length());
        return Integer.parseInt(sessionId);
    }

    private File getTmpSessionDir(String volumeUuid) {
        return Environment.getDataAppDirectory(volumeUuid);
    }

    private File buildTmpSessionDir(int sessionId, String volumeUuid) {
        File sessionStagingDir = getTmpSessionDir(volumeUuid);
        return new File(sessionStagingDir, "vmdl" + sessionId + ".tmp");
    }

    private File buildSessionDir(int sessionId, PackageInstaller.SessionParams params) {
        if (params.isStaged || (params.installFlags & 131072) != 0) {
            File sessionStagingDir = Environment.getDataStagingDirectory(params.volumeUuid);
            return new File(sessionStagingDir, "session_" + sessionId);
        }
        File result = buildTmpSessionDir(sessionId, params.volumeUuid);
        if (DEBUG && !Objects.equals(Integer.valueOf(tryParseSessionId(result.getName())), Integer.valueOf(sessionId))) {
            throw new RuntimeException("session folder format is off: " + result.getName() + " (" + sessionId + ")");
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void prepareStageDir(File stageDir) throws IOException {
        if (stageDir.exists()) {
            throw new IOException("Session dir already exists: " + stageDir);
        }
        try {
            Os.mkdir(stageDir.getAbsolutePath(), 509);
            Os.chmod(stageDir.getAbsolutePath(), 509);
            if (!SELinux.restorecon(stageDir)) {
                String path = stageDir.getCanonicalPath();
                String ctx = SELinux.fileSelabelLookup(path);
                boolean success = SELinux.setFileContext(path, ctx);
                Slog.e(TAG, "Failed to SELinux.restorecon session dir, path: [" + path + "], ctx: [" + ctx + "]. Retrying via SELinux.fileSelabelLookup/SELinux.setFileContext: " + (success ? "SUCCESS" : "FAILURE"));
                if (!success) {
                    throw new IOException("Failed to restorecon session dir: " + stageDir);
                }
            }
        } catch (ErrnoException e) {
            throw new IOException("Failed to prepare session dir: " + stageDir, e);
        }
    }

    private String buildExternalStageCid(int sessionId) {
        return "smdl" + sessionId + ".tmp";
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: shouldFilterSession */
    public boolean m5466xed66ff88(Computer snapshot, int uid, PackageInstaller.SessionInfo info) {
        return (info == null || uid == info.getInstallerUid() || snapshot.canQueryPackage(uid, info.getAppPackageName())) ? false : true;
    }

    public PackageInstaller.SessionInfo getSessionInfo(int sessionId) {
        PackageInstaller.SessionInfo sessionInfo;
        PackageInstaller.SessionInfo result;
        int callingUid = Binder.getCallingUid();
        synchronized (this.mSessions) {
            PackageInstallerSession session = this.mSessions.get(sessionId);
            if (session != null && (!session.isStaged() || !session.isDestroyed())) {
                sessionInfo = session.generateInfoForCaller(true, callingUid);
            } else {
                sessionInfo = null;
            }
            result = sessionInfo;
        }
        if (m5466xed66ff88(this.mPm.snapshotComputer(), callingUid, result)) {
            return null;
        }
        return result;
    }

    public ParceledListSlice<PackageInstaller.SessionInfo> getStagedSessions() {
        final int callingUid = Binder.getCallingUid();
        List<PackageInstaller.SessionInfo> result = new ArrayList<>();
        synchronized (this.mSessions) {
            for (int i = 0; i < this.mSessions.size(); i++) {
                PackageInstallerSession session = this.mSessions.valueAt(i);
                if (session.isStaged() && !session.isDestroyed()) {
                    result.add(session.generateInfoForCaller(false, callingUid));
                }
            }
        }
        final Computer snapshot = this.mPm.snapshotComputer();
        result.removeIf(new Predicate() { // from class: com.android.server.pm.PackageInstallerService$$ExternalSyntheticLambda3
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return PackageInstallerService.this.m5466xed66ff88(snapshot, callingUid, (PackageInstaller.SessionInfo) obj);
            }
        });
        return new ParceledListSlice<>(result);
    }

    public ParceledListSlice<PackageInstaller.SessionInfo> getAllSessions(int userId) {
        final int callingUid = Binder.getCallingUid();
        final Computer snapshot = this.mPm.snapshotComputer();
        snapshot.enforceCrossUserPermission(callingUid, userId, true, false, "getAllSessions");
        List<PackageInstaller.SessionInfo> result = new ArrayList<>();
        synchronized (this.mSessions) {
            for (int i = 0; i < this.mSessions.size(); i++) {
                PackageInstallerSession session = this.mSessions.valueAt(i);
                if (session.userId == userId && !session.hasParentSessionId() && (!session.isStaged() || !session.isDestroyed())) {
                    result.add(session.generateInfoForCaller(false, callingUid));
                }
            }
        }
        result.removeIf(new Predicate() { // from class: com.android.server.pm.PackageInstallerService$$ExternalSyntheticLambda2
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return PackageInstallerService.this.m5465x63875532(snapshot, callingUid, (PackageInstaller.SessionInfo) obj);
            }
        });
        return new ParceledListSlice<>(result);
    }

    public ParceledListSlice<PackageInstaller.SessionInfo> getMySessions(String installerPackageName, int userId) {
        Computer snapshot = this.mPm.snapshotComputer();
        int callingUid = Binder.getCallingUid();
        snapshot.enforceCrossUserPermission(callingUid, userId, true, false, "getMySessions");
        this.mAppOps.checkPackage(callingUid, installerPackageName);
        List<PackageInstaller.SessionInfo> result = new ArrayList<>();
        synchronized (this.mSessions) {
            for (int i = 0; i < this.mSessions.size(); i++) {
                PackageInstallerSession session = this.mSessions.valueAt(i);
                PackageInstaller.SessionInfo info = session.generateInfoForCaller(false, 1000);
                if (Objects.equals(info.getInstallerPackageName(), installerPackageName) && session.userId == userId && !session.hasParentSessionId() && isCallingUidOwner(session)) {
                    result.add(info);
                }
            }
        }
        return new ParceledListSlice<>(result);
    }

    public void uninstall(VersionedPackage versionedPackage, String callerPackageName, int flags, IntentSender statusReceiver, int userId) {
        Computer snapshot = this.mPm.snapshotComputer();
        int callingUid = Binder.getCallingUid();
        snapshot.enforceCrossUserPermission(callingUid, userId, true, true, "uninstall");
        if (Build.IS_DEBUG_ENABLE) {
            try {
                int testCallingUid = Binder.getCallingUid();
                int testPid = Binder.getCallingPid();
                Slog.i(TAG, "uninstall,callerPackageName =" + callerPackageName + "Calling uid =" + testCallingUid + ", pid =" + testPid, new Throwable());
                if (versionedPackage != null) {
                    Slog.i(TAG, "versionedPackage =" + versionedPackage.toString());
                }
            } catch (Exception e) {
                Slog.e(TAG, "Error: can't get pid before boot completed:" + e.toString());
            }
        }
        if (callingUid != 2000 && callingUid != 0) {
            this.mAppOps.checkPackage(callingUid, callerPackageName);
        }
        DevicePolicyManagerInternal dpmi = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        boolean canSilentlyInstallPackage = dpmi != null && dpmi.canSilentlyInstallPackage(callerPackageName, callingUid);
        PackageDeleteObserverAdapter adapter = new PackageDeleteObserverAdapter(this.mContext, statusReceiver, versionedPackage.getPackageName(), canSilentlyInstallPackage, userId);
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DELETE_PACKAGES") == 0) {
            this.mPm.deletePackageVersioned(versionedPackage, adapter.getBinder(), userId, flags);
        } else if (canSilentlyInstallPackage) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mPm.deletePackageVersioned(versionedPackage, adapter.getBinder(), userId, flags);
                Binder.restoreCallingIdentity(ident);
                DevicePolicyEventLogger.createEvent(113).setAdmin(callerPackageName).write();
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        } else {
            ApplicationInfo appInfo = snapshot.getApplicationInfo(callerPackageName, 0L, userId);
            if (appInfo.targetSdkVersion >= 28) {
                this.mContext.enforceCallingOrSelfPermission("android.permission.REQUEST_DELETE_PACKAGES", null);
            }
            Intent intent = new Intent("android.intent.action.UNINSTALL_PACKAGE");
            intent.setData(Uri.fromParts("package", versionedPackage.getPackageName(), null));
            intent.putExtra("android.content.pm.extra.CALLBACK", adapter.getBinder().asBinder());
            adapter.onUserActionRequired(intent);
        }
    }

    public void uninstallExistingPackage(VersionedPackage versionedPackage, String callerPackageName, IntentSender statusReceiver, int userId) {
        int callingUid = Binder.getCallingUid();
        this.mContext.enforceCallingOrSelfPermission("android.permission.DELETE_PACKAGES", null);
        Computer snapshot = this.mPm.snapshotComputer();
        snapshot.enforceCrossUserPermission(callingUid, userId, true, true, "uninstall");
        if (callingUid != 2000 && callingUid != 0) {
            this.mAppOps.checkPackage(callingUid, callerPackageName);
        }
        PackageDeleteObserverAdapter adapter = new PackageDeleteObserverAdapter(this.mContext, statusReceiver, versionedPackage.getPackageName(), false, userId);
        this.mPm.deleteExistingPackageAsUser(versionedPackage, adapter.getBinder(), userId);
    }

    public void installExistingPackage(String packageName, int installFlags, int installReason, IntentSender statusReceiver, int userId, List<String> allowListedPermissions) {
        InstallPackageHelper installPackageHelper = new InstallPackageHelper(this.mPm);
        installPackageHelper.installExistingPackageAsUser(packageName, userId, installFlags, installReason, allowListedPermissions, statusReceiver);
    }

    public void setPermissionsResult(int sessionId, boolean accepted) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INSTALL_PACKAGES", TAG);
        synchronized (this.mSessions) {
            PackageInstallerSession session = this.mSessions.get(sessionId);
            if (session != null) {
                session.setPermissionsResult(accepted);
            }
        }
    }

    public void registerCallback(IPackageInstallerCallback callback, final int userId) {
        Computer snapshot = this.mPm.snapshotComputer();
        snapshot.enforceCrossUserPermission(Binder.getCallingUid(), userId, true, false, "registerCallback");
        registerCallback(callback, new IntPredicate() { // from class: com.android.server.pm.PackageInstallerService$$ExternalSyntheticLambda1
            @Override // java.util.function.IntPredicate
            public final boolean test(int i) {
                return PackageInstallerService.lambda$registerCallback$3(userId, i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$registerCallback$3(int userId, int eventUserId) {
        return userId == eventUserId;
    }

    public void registerCallback(IPackageInstallerCallback callback, IntPredicate userCheck) {
        this.mCallbacks.register(callback, new BroadcastCookie(Binder.getCallingUid(), userCheck));
    }

    public void unregisterCallback(IPackageInstallerCallback callback) {
        this.mCallbacks.unregister(callback);
    }

    @Override // com.android.server.pm.PackageSessionProvider
    public PackageInstallerSession getSession(int sessionId) {
        PackageInstallerSession packageInstallerSession;
        synchronized (this.mSessions) {
            packageInstallerSession = this.mSessions.get(sessionId);
        }
        return packageInstallerSession;
    }

    @Override // com.android.server.pm.PackageSessionProvider
    public PackageSessionVerifier getSessionVerifier() {
        return this.mSessionVerifier;
    }

    public void bypassNextStagedInstallerCheck(boolean value) {
        if (!isCalledBySystemOrShell(Binder.getCallingUid())) {
            throw new SecurityException("Caller not allowed to bypass staged installer check");
        }
        this.mBypassNextStagedInstallerCheck = value;
    }

    public void bypassNextAllowedApexUpdateCheck(boolean value) {
        if (!isCalledBySystemOrShell(Binder.getCallingUid())) {
            throw new SecurityException("Caller not allowed to bypass allowed apex update check");
        }
        this.mBypassNextAllowedApexUpdateCheck = value;
    }

    public void setAllowUnlimitedSilentUpdates(String installerPackageName) {
        if (!isCalledBySystemOrShell(Binder.getCallingUid())) {
            throw new SecurityException("Caller not allowed to unlimite silent updates");
        }
        this.mSilentUpdatePolicy.setAllowUnlimitedSilentUpdates(installerPackageName);
    }

    public void setSilentUpdatesThrottleTime(long throttleTimeInSeconds) {
        if (!isCalledBySystemOrShell(Binder.getCallingUid())) {
            throw new SecurityException("Caller not allowed to set silent updates throttle time");
        }
        this.mSilentUpdatePolicy.setSilentUpdatesThrottleTime(throttleTimeInSeconds);
    }

    private static int getSessionCount(SparseArray<PackageInstallerSession> sessions, int installerUid) {
        int count = 0;
        int size = sessions.size();
        for (int i = 0; i < size; i++) {
            PackageInstallerSession session = sessions.valueAt(i);
            if (session.getInstallerUid() == installerUid) {
                count++;
            }
        }
        return count;
    }

    private boolean isCallingUidOwner(PackageInstallerSession session) {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 0) {
            return true;
        }
        return session != null && callingUid == session.getInstallerUid();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean shouldFilterSession(Computer snapshot, int uid, int sessionId) {
        PackageInstallerSession session = getSession(sessionId);
        return (session == null || uid == session.getInstallerUid() || snapshot.canQueryPackage(uid, session.getPackageName())) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class PackageDeleteObserverAdapter extends PackageDeleteObserver {
        private final Context mContext;
        private final Notification mNotification;
        private final String mPackageName;
        private final IntentSender mTarget;

        public PackageDeleteObserverAdapter(Context context, IntentSender target, String packageName, boolean showNotification, int userId) {
            this.mContext = context;
            this.mTarget = target;
            this.mPackageName = packageName;
            if (showNotification) {
                this.mNotification = PackageInstallerService.buildSuccessNotification(context, getDeviceOwnerDeletedPackageMsg(), packageName, userId);
            } else {
                this.mNotification = null;
            }
        }

        private String getDeviceOwnerDeletedPackageMsg() {
            DevicePolicyManager dpm = (DevicePolicyManager) this.mContext.getSystemService(DevicePolicyManager.class);
            return dpm.getResources().getString("Core.PACKAGE_DELETED_BY_DO", new Supplier() { // from class: com.android.server.pm.PackageInstallerService$PackageDeleteObserverAdapter$$ExternalSyntheticLambda0
                @Override // java.util.function.Supplier
                public final Object get() {
                    return PackageInstallerService.PackageDeleteObserverAdapter.this.m5472xe553de71();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getDeviceOwnerDeletedPackageMsg$0$com-android-server-pm-PackageInstallerService$PackageDeleteObserverAdapter  reason: not valid java name */
        public /* synthetic */ String m5472xe553de71() {
            return this.mContext.getString(17040949);
        }

        public void onUserActionRequired(Intent intent) {
            if (this.mTarget == null) {
                return;
            }
            Intent fillIn = new Intent();
            fillIn.putExtra("android.content.pm.extra.PACKAGE_NAME", this.mPackageName);
            fillIn.putExtra("android.content.pm.extra.STATUS", -1);
            fillIn.putExtra("android.intent.extra.INTENT", intent);
            try {
                BroadcastOptions options = BroadcastOptions.makeBasic();
                options.setPendingIntentBackgroundActivityLaunchAllowed(false);
                this.mTarget.sendIntent(this.mContext, 0, fillIn, null, null, null, options.toBundle());
            } catch (IntentSender.SendIntentException e) {
            }
        }

        public void onPackageDeleted(String basePackageName, int returnCode, String msg) {
            if (1 == returnCode && this.mNotification != null) {
                NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
                notificationManager.notify(basePackageName, 21, this.mNotification);
            }
            if (this.mTarget == null) {
                return;
            }
            Intent fillIn = new Intent();
            fillIn.putExtra("android.content.pm.extra.PACKAGE_NAME", this.mPackageName);
            fillIn.putExtra("android.content.pm.extra.STATUS", PackageManager.deleteStatusToPublicStatus(returnCode));
            fillIn.putExtra("android.content.pm.extra.STATUS_MESSAGE", PackageManager.deleteStatusToString(returnCode, msg));
            fillIn.putExtra("android.content.pm.extra.LEGACY_STATUS", returnCode);
            try {
                BroadcastOptions options = BroadcastOptions.makeBasic();
                options.setPendingIntentBackgroundActivityLaunchAllowed(false);
                this.mTarget.sendIntent(this.mContext, 0, fillIn, null, null, null, options.toBundle());
            } catch (IntentSender.SendIntentException e) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Notification buildSuccessNotification(Context context, String contentText, String basePackageName, int userId) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = AppGlobals.getPackageManager().getPackageInfo(basePackageName, 67108864L, userId);
        } catch (RemoteException e) {
        }
        if (packageInfo == null || packageInfo.applicationInfo == null) {
            Slog.w(TAG, "Notification not built for package: " + basePackageName);
            return null;
        }
        PackageManager pm = context.getPackageManager();
        Bitmap packageIcon = ImageUtils.buildScaledBitmap(packageInfo.applicationInfo.loadIcon(pm), context.getResources().getDimensionPixelSize(17104901), context.getResources().getDimensionPixelSize(17104902));
        CharSequence packageLabel = packageInfo.applicationInfo.loadLabel(pm);
        return new Notification.Builder(context, SystemNotificationChannels.DEVICE_ADMIN).setSmallIcon(17302389).setColor(context.getResources().getColor(17170460)).setContentTitle(packageLabel).setContentText(contentText).setStyle(new Notification.BigTextStyle().bigText(contentText)).setLargeIcon(packageIcon).build();
    }

    public static <E> ArraySet<E> newArraySet(E... elements) {
        ArraySet<E> set = new ArraySet<>();
        if (elements != null) {
            set.ensureCapacity(elements.length);
            Collections.addAll(set, elements);
        }
        return set;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class BroadcastCookie {
        public final int callingUid;
        public final IntPredicate userCheck;

        BroadcastCookie(int callingUid, IntPredicate userCheck) {
            this.callingUid = callingUid;
            this.userCheck = userCheck;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class Callbacks extends Handler {
        private static final int MSG_SESSION_ACTIVE_CHANGED = 3;
        private static final int MSG_SESSION_BADGING_CHANGED = 2;
        private static final int MSG_SESSION_CREATED = 1;
        private static final int MSG_SESSION_FINISHED = 5;
        private static final int MSG_SESSION_PROGRESS_CHANGED = 4;
        private final RemoteCallbackList<IPackageInstallerCallback> mCallbacks;

        public Callbacks(Looper looper) {
            super(looper);
            this.mCallbacks = new RemoteCallbackList<>();
        }

        public void register(IPackageInstallerCallback callback, BroadcastCookie cookie) {
            this.mCallbacks.register(callback, cookie);
        }

        public void unregister(IPackageInstallerCallback callback) {
            this.mCallbacks.unregister(callback);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int sessionId = msg.arg1;
            int userId = msg.arg2;
            int n = this.mCallbacks.beginBroadcast();
            Computer snapshot = PackageInstallerService.this.mPm.snapshotComputer();
            for (int i = 0; i < n; i++) {
                IPackageInstallerCallback callback = this.mCallbacks.getBroadcastItem(i);
                BroadcastCookie cookie = (BroadcastCookie) this.mCallbacks.getBroadcastCookie(i);
                if (cookie.userCheck.test(userId) && !PackageInstallerService.this.shouldFilterSession(snapshot, cookie.callingUid, sessionId)) {
                    try {
                        invokeCallback(callback, msg);
                    } catch (RemoteException e) {
                    }
                }
            }
            this.mCallbacks.finishBroadcast();
        }

        private void invokeCallback(IPackageInstallerCallback callback, Message msg) throws RemoteException {
            int sessionId = msg.arg1;
            switch (msg.what) {
                case 1:
                    callback.onSessionCreated(sessionId);
                    return;
                case 2:
                    callback.onSessionBadgingChanged(sessionId);
                    return;
                case 3:
                    callback.onSessionActiveChanged(sessionId, ((Boolean) msg.obj).booleanValue());
                    return;
                case 4:
                    callback.onSessionProgressChanged(sessionId, ((Float) msg.obj).floatValue());
                    return;
                case 5:
                    callback.onSessionFinished(sessionId, ((Boolean) msg.obj).booleanValue());
                    return;
                default:
                    return;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void notifySessionCreated(int sessionId, int userId) {
            obtainMessage(1, sessionId, userId).sendToTarget();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void notifySessionBadgingChanged(int sessionId, int userId) {
            obtainMessage(2, sessionId, userId).sendToTarget();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void notifySessionActiveChanged(int sessionId, int userId, boolean active) {
            obtainMessage(3, sessionId, userId, Boolean.valueOf(active)).sendToTarget();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void notifySessionProgressChanged(int sessionId, int userId, float progress) {
            obtainMessage(4, sessionId, userId, Float.valueOf(progress)).sendToTarget();
        }

        public void notifySessionFinished(int sessionId, int userId, boolean success) {
            obtainMessage(5, sessionId, userId, Boolean.valueOf(success)).sendToTarget();
        }
    }

    /* loaded from: classes2.dex */
    static class ParentChildSessionMap {
        private final Comparator<PackageInstallerSession> mSessionCreationComparator;
        private TreeMap<PackageInstallerSession, TreeSet<PackageInstallerSession>> mSessionMap;

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ long lambda$new$0(PackageInstallerSession sess) {
            if (sess != null) {
                return sess.createdMillis;
            }
            return -1L;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ int lambda$new$1(PackageInstallerSession sess) {
            if (sess != null) {
                return sess.sessionId;
            }
            return -1;
        }

        ParentChildSessionMap() {
            Comparator<PackageInstallerSession> thenComparingInt = Comparator.comparingLong(new ToLongFunction() { // from class: com.android.server.pm.PackageInstallerService$ParentChildSessionMap$$ExternalSyntheticLambda0
                @Override // java.util.function.ToLongFunction
                public final long applyAsLong(Object obj) {
                    return PackageInstallerService.ParentChildSessionMap.lambda$new$0((PackageInstallerSession) obj);
                }
            }).thenComparingInt(new ToIntFunction() { // from class: com.android.server.pm.PackageInstallerService$ParentChildSessionMap$$ExternalSyntheticLambda1
                @Override // java.util.function.ToIntFunction
                public final int applyAsInt(Object obj) {
                    return PackageInstallerService.ParentChildSessionMap.lambda$new$1((PackageInstallerSession) obj);
                }
            });
            this.mSessionCreationComparator = thenComparingInt;
            this.mSessionMap = new TreeMap<>((Comparator<? super PackageInstallerSession>) thenComparingInt);
        }

        boolean containsSession() {
            return !this.mSessionMap.isEmpty();
        }

        private void addParentSession(PackageInstallerSession session) {
            if (!this.mSessionMap.containsKey(session)) {
                this.mSessionMap.put(session, new TreeSet<>((Comparator<? super PackageInstallerSession>) this.mSessionCreationComparator));
            }
        }

        private void addChildSession(PackageInstallerSession session, PackageInstallerSession parentSession) {
            addParentSession(parentSession);
            this.mSessionMap.get(parentSession).add(session);
        }

        void addSession(PackageInstallerSession session, PackageInstallerSession parentSession) {
            if (session.hasParentSessionId()) {
                addChildSession(session, parentSession);
            } else {
                addParentSession(session);
            }
        }

        void dump(String tag, IndentingPrintWriter pw) {
            pw.println(tag + " install sessions:");
            pw.increaseIndent();
            for (Map.Entry<PackageInstallerSession, TreeSet<PackageInstallerSession>> entry : this.mSessionMap.entrySet()) {
                PackageInstallerSession parentSession = entry.getKey();
                if (parentSession != null) {
                    pw.print(tag + " ");
                    parentSession.dump(pw);
                    pw.println();
                    pw.increaseIndent();
                }
                Iterator<PackageInstallerSession> it = entry.getValue().iterator();
                while (it.hasNext()) {
                    PackageInstallerSession childSession = it.next();
                    pw.print(tag + " Child ");
                    childSession.dump(pw);
                    pw.println();
                }
                pw.decreaseIndent();
            }
            pw.println();
            pw.decreaseIndent();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(IndentingPrintWriter pw) {
        PackageInstallerSession rootSession;
        synchronized (this.mSessions) {
            ParentChildSessionMap activeSessionMap = new ParentChildSessionMap();
            ParentChildSessionMap orphanedChildSessionMap = new ParentChildSessionMap();
            ParentChildSessionMap finalizedSessionMap = new ParentChildSessionMap();
            int N = this.mSessions.size();
            for (int i = 0; i < N; i++) {
                PackageInstallerSession session = this.mSessions.valueAt(i);
                if (session.hasParentSessionId()) {
                    rootSession = getSession(session.getParentSessionId());
                } else {
                    rootSession = session;
                }
                if (rootSession == null) {
                    orphanedChildSessionMap.addSession(session, rootSession);
                } else if (rootSession.isStagedAndInTerminalState()) {
                    finalizedSessionMap.addSession(session, rootSession);
                } else {
                    activeSessionMap.addSession(session, rootSession);
                }
            }
            activeSessionMap.dump("Active", pw);
            if (orphanedChildSessionMap.containsSession()) {
                orphanedChildSessionMap.dump("Orphaned", pw);
            }
            finalizedSessionMap.dump("Finalized", pw);
            pw.println("Historical install sessions:");
            pw.increaseIndent();
            int N2 = this.mHistoricalSessions.size();
            for (int i2 = 0; i2 < N2; i2++) {
                pw.print(this.mHistoricalSessions.get(i2));
                pw.println();
            }
            pw.println();
            pw.decreaseIndent();
            pw.println("Legacy install sessions:");
            pw.increaseIndent();
            pw.println(this.mLegacySessions.toString());
            pw.println();
            pw.decreaseIndent();
        }
        this.mSilentUpdatePolicy.dump(pw);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class InternalCallback {
        InternalCallback() {
        }

        public void onSessionBadgingChanged(PackageInstallerSession session) {
            PackageInstallerService.this.mCallbacks.notifySessionBadgingChanged(session.sessionId, session.userId);
            PackageInstallerService.this.mSettingsWriteRequest.schedule();
        }

        public void onSessionActiveChanged(PackageInstallerSession session, boolean active) {
            PackageInstallerService.this.mCallbacks.notifySessionActiveChanged(session.sessionId, session.userId, active);
        }

        public void onSessionProgressChanged(PackageInstallerSession session, float progress) {
            PackageInstallerService.this.mCallbacks.notifySessionProgressChanged(session.sessionId, session.userId, progress);
        }

        public void onSessionChanged(PackageInstallerSession session) {
            session.markUpdated();
            PackageInstallerService.this.mSettingsWriteRequest.schedule();
            if (PackageInstallerService.this.mOkToSendBroadcasts && !session.isDestroyed() && session.isStaged()) {
                PackageInstallerService.this.sendSessionUpdatedBroadcast(session.generateInfoForCaller(false, 1000), session.userId);
            }
        }

        public void onSessionFinished(final PackageInstallerSession session, final boolean success) {
            PackageInstallerService.this.mCallbacks.notifySessionFinished(session.sessionId, session.userId, success);
            PackageInstallerService.this.mInstallHandler.post(new Runnable() { // from class: com.android.server.pm.PackageInstallerService.InternalCallback.1
                /* JADX WARN: Removed duplicated region for block: B:21:0x004a A[Catch: all -> 0x0076, TryCatch #0 {, blocks: (B:9:0x0024, B:11:0x002c, B:13:0x0034, B:15:0x003c, B:21:0x004a, B:22:0x0053, B:24:0x0065, B:25:0x0068, B:26:0x0074), top: B:31:0x0024 }] */
                @Override // java.lang.Runnable
                /*
                    Code decompiled incorrectly, please refer to instructions dump.
                */
                public void run() {
                    boolean shouldRemove;
                    if (session.isStaged() && !success) {
                        PackageInstallerService.this.mStagingManager.abortSession(session.mStagedSession);
                    }
                    synchronized (PackageInstallerService.this.mSessions) {
                        if (!session.hasParentSessionId()) {
                            if (session.isStaged() && !session.isDestroyed() && session.isCommitted()) {
                                shouldRemove = false;
                                if (shouldRemove) {
                                    PackageInstallerService.this.removeActiveSession(session);
                                }
                            }
                            shouldRemove = true;
                            if (shouldRemove) {
                            }
                        }
                        File appIconFile = PackageInstallerService.this.buildAppIconFile(session.sessionId);
                        if (appIconFile.exists()) {
                            appIconFile.delete();
                        }
                        PackageInstallerService.this.mSettingsWriteRequest.runNow();
                    }
                }
            });
        }

        public void onSessionPrepared(PackageInstallerSession session) {
            PackageInstallerService.this.mSettingsWriteRequest.schedule();
        }

        public void onSessionSealedBlocking(PackageInstallerSession session) {
            PackageInstallerService.this.mSettingsWriteRequest.runNow();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendSessionUpdatedBroadcast(PackageInstaller.SessionInfo sessionInfo, int userId) {
        if (TextUtils.isEmpty(sessionInfo.installerPackageName)) {
            return;
        }
        Intent sessionUpdatedIntent = new Intent("android.content.pm.action.SESSION_UPDATED").putExtra("android.content.pm.extra.SESSION", sessionInfo).setPackage(sessionInfo.installerPackageName);
        this.mContext.sendBroadcastAsUser(sessionUpdatedIntent, UserHandle.of(userId));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onInstallerPackageDeleted(int installerAppId, int userId) {
        synchronized (this.mSessions) {
            for (int i = 0; i < this.mSessions.size(); i++) {
                PackageInstallerSession session = this.mSessions.valueAt(i);
                if (matchesInstaller(session, installerAppId, userId)) {
                    PackageInstallerSession root = !session.hasParentSessionId() ? session : this.mSessions.get(session.getParentSessionId());
                    if (root != null && matchesInstaller(root, installerAppId, userId) && !root.isDestroyed()) {
                        root.abandon();
                    }
                }
            }
        }
    }

    private boolean matchesInstaller(PackageInstallerSession session, int installerAppId, int userId) {
        int installerUid = session.getInstallerUid();
        return installerAppId == -1 ? UserHandle.getAppId(installerUid) == installerAppId : UserHandle.getUid(userId, installerAppId) == installerUid;
    }
}
