package com.android.server.pm;

import android.apex.ApexInfo;
import android.apex.ApexInfoList;
import android.apex.ApexSessionInfo;
import android.apex.ApexSessionParams;
import android.apex.CompressedApexInfoList;
import android.apex.IApexService;
import android.content.pm.PackageInfo;
import android.content.pm.SigningDetails;
import android.content.pm.parsing.result.ParseResult;
import android.content.pm.parsing.result.ParseTypeImpl;
import android.hardware.biometrics.fingerprint.V2_1.RequestStatus;
import android.os.Binder;
import android.os.Environment;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.Trace;
import android.sysprop.ApexProperties;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.PrintWriterPrinter;
import android.util.Singleton;
import android.util.Slog;
import android.util.SparseArray;
import android.util.apk.ApkSignatureVerifier;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.modules.utils.build.UnboundedSdkLevel;
import com.android.server.pm.ParallelPackageParser;
import com.android.server.pm.parsing.PackageParser2;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.ParsedPackage;
import com.android.server.pm.pkg.component.ParsedApexSystemService;
import com.android.server.pm.pkg.parsing.PackageInfoWithoutStateUtils;
import com.android.server.utils.TimingsTraceAndSlog;
import com.google.android.collect.Lists;
import java.io.File;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
/* loaded from: classes2.dex */
public abstract class ApexManager {
    public static final int MATCH_ACTIVE_PACKAGE = 1;
    static final int MATCH_FACTORY_PACKAGE = 2;
    private static final String TAG = "ApexManager";
    private static final String VNDK_APEX_MODULE_NAME_PREFIX = "com.android.vndk.";
    private static final Singleton<ApexManager> sApexManagerSingleton = new Singleton<ApexManager>() { // from class: com.android.server.pm.ApexManager.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        /* renamed from: create */
        public ApexManager m5367create() {
            if (((Boolean) ApexProperties.updatable().orElse(false)).booleanValue()) {
                return new ApexManagerImpl();
            }
            return new ApexManagerFlattenedApex();
        }
    };

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface PackageInfoFlags {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract boolean abortStagedSession(int i);

    public abstract long calculateSizeForCompressedApex(CompressedApexInfoList compressedApexInfoList) throws RemoteException;

    public abstract boolean destroyCeSnapshots(int i, int i2);

    public abstract boolean destroyCeSnapshotsNotSpecified(int i, int[] iArr);

    public abstract boolean destroyDeSnapshots(int i);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract void dump(PrintWriter printWriter, String str);

    public abstract List<ActiveApexInfo> getActiveApexInfos();

    public abstract String getActiveApexPackageNameContainingPackage(String str);

    public abstract String getActivePackageNameForApexModuleName(String str);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract List<PackageInfo> getActivePackages();

    public abstract String getApexModuleNameForPackageName(String str);

    public abstract List<ApexSystemServiceInfo> getApexSystemServices();

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract String getApkInApexInstallError(String str);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract List<String> getApksInApex(String str);

    public abstract File getBackingApexFile(File file);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract List<PackageInfo> getFactoryPackages();

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract List<PackageInfo> getInactivePackages();

    public abstract PackageInfo getPackageInfo(String str, int i);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract SparseArray<ApexSessionInfo> getSessions();

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract ApexInfo[] getStagedApexInfos(ApexSessionParams apexSessionParams);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract ApexSessionInfo getStagedSessionInfo(int i);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract void installPackage(File file, PackageParser2 packageParser2) throws PackageManagerException;

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract boolean isApexPackage(String str);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract boolean isApexSupported();

    public abstract void markBootCompleted();

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract void markStagedSessionReady(int i) throws PackageManagerException;

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract void markStagedSessionSuccessful(int i);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract void registerApkInApex(AndroidPackage androidPackage);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract void reportErrorWithApkInApex(String str, String str2);

    public abstract void reserveSpaceForCompressedApex(CompressedApexInfoList compressedApexInfoList) throws RemoteException;

    public abstract boolean restoreCeData(int i, int i2, String str);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract boolean revertActiveSessions();

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract void scanApexPackagesTraced(PackageParser2 packageParser2, ExecutorService executorService);

    public abstract boolean snapshotCeData(int i, int i2, String str);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract ApexInfoList submitStagedSession(ApexSessionParams apexSessionParams) throws PackageManagerException;

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract boolean uninstallApex(String str);

    public static ApexManager getInstance() {
        return (ApexManager) sApexManagerSingleton.get();
    }

    /* loaded from: classes2.dex */
    public static class ActiveApexInfo {
        public final boolean activeApexChanged;
        public final File apexDirectory;
        public final File apexFile;
        public final String apexModuleName;
        public final File preInstalledApexPath;

        private ActiveApexInfo(File apexDirectory, File preInstalledApexPath, File apexFile) {
            this(null, apexDirectory, preInstalledApexPath, apexFile, false);
        }

        private ActiveApexInfo(String apexModuleName, File apexDirectory, File preInstalledApexPath, File apexFile, boolean activeApexChanged) {
            this.apexModuleName = apexModuleName;
            this.apexDirectory = apexDirectory;
            this.preInstalledApexPath = preInstalledApexPath;
            this.apexFile = apexFile;
            this.activeApexChanged = activeApexChanged;
        }

        private ActiveApexInfo(ApexInfo apexInfo) {
            this(apexInfo.moduleName, new File(Environment.getApexDirectory() + File.separator + apexInfo.moduleName), new File(apexInfo.preinstalledModulePath), new File(apexInfo.modulePath), apexInfo.activeApexChanged);
        }
    }

    public static boolean isFactory(PackageInfo packageInfo) {
        return (packageInfo.applicationInfo.flags & 1) != 0;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes2.dex */
    public static class ApexManagerImpl extends ApexManager {
        private Set<ActiveApexInfo> mActiveApexInfosCache;
        private List<PackageInfo> mAllPackagesCache;
        private ArrayMap<String, String> mApexModuleNameToActivePackageName;
        private ArrayMap<String, String> mPackageNameToApexModuleName;
        private final Object mLock = new Object();
        private List<ApexSystemServiceInfo> mApexSystemServices = new ArrayList();
        private ArrayMap<String, List<String>> mApksInApex = new ArrayMap<>();
        private Map<String, String> mErrorWithApkInApex = new ArrayMap();

        protected ApexManagerImpl() {
        }

        private static boolean isActive(PackageInfo packageInfo) {
            return (packageInfo.applicationInfo.flags & 8388608) != 0;
        }

        protected IApexService waitForApexService() {
            return IApexService.Stub.asInterface(Binder.allowBlocking(ServiceManager.waitForService("apexservice")));
        }

        @Override // com.android.server.pm.ApexManager
        public List<ActiveApexInfo> getActiveApexInfos() {
            TimingsTraceAndSlog t = new TimingsTraceAndSlog("ApexManagerTiming", 262144L);
            synchronized (this.mLock) {
                if (this.mActiveApexInfosCache == null) {
                    t.traceBegin("getActiveApexInfos_noCache");
                    try {
                        this.mActiveApexInfosCache = new ArraySet();
                        ApexInfo[] activePackages = waitForApexService().getActivePackages();
                        for (ApexInfo apexInfo : activePackages) {
                            this.mActiveApexInfosCache.add(new ActiveApexInfo(apexInfo));
                        }
                    } catch (RemoteException e) {
                        Slog.e(ApexManager.TAG, "Unable to retrieve packages from apexservice", e);
                    }
                    t.traceEnd();
                }
                if (this.mActiveApexInfosCache != null) {
                    return new ArrayList(this.mActiveApexInfosCache);
                }
                return Collections.emptyList();
            }
        }

        @Override // com.android.server.pm.ApexManager
        void scanApexPackagesTraced(PackageParser2 packageParser, ExecutorService executorService) {
            Trace.traceBegin(262144L, "scanApexPackagesTraced");
            try {
                synchronized (this.mLock) {
                    scanApexPackagesInternalLocked(packageParser, executorService);
                }
            } finally {
                Trace.traceEnd(262144L);
            }
        }

        private void scanApexPackagesInternalLocked(PackageParser2 packageParser, ExecutorService executorService) {
            ApexInfo[] allPkgs;
            int flags;
            ArrayMap<File, ApexInfo> parsingApexInfo;
            ParallelPackageParser parallelPackageParser;
            ArrayMap<File, ApexInfo> parsingApexInfo2;
            ParallelPackageParser parallelPackageParser2;
            try {
                this.mAllPackagesCache = new ArrayList();
                this.mPackageNameToApexModuleName = new ArrayMap<>();
                this.mApexModuleNameToActivePackageName = new ArrayMap<>();
                ApexInfo[] allPkgs2 = waitForApexService().getAllPackages();
                if (allPkgs2.length == 0) {
                    return;
                }
                int flags2 = 192;
                ArrayMap<File, ApexInfo> parsingApexInfo3 = new ArrayMap<>();
                ParallelPackageParser parallelPackageParser3 = new ParallelPackageParser(packageParser, executorService);
                for (ApexInfo ai : allPkgs2) {
                    File apexFile = new File(ai.modulePath);
                    parallelPackageParser3.submit(apexFile, 32);
                    parsingApexInfo3.put(apexFile, ai);
                }
                HashSet<String> activePackagesSet = new HashSet<>();
                HashSet<String> factoryPackagesSet = new HashSet<>();
                int i = 0;
                while (i < parsingApexInfo3.size()) {
                    ParallelPackageParser.ParseResult parseResult = parallelPackageParser3.take();
                    Throwable throwable = parseResult.throwable;
                    ApexInfo ai2 = parsingApexInfo3.get(parseResult.scanFile);
                    if (throwable == null) {
                        parseResult.parsedPackage.hideAsFinal();
                        PackageInfo packageInfo = PackageInfoWithoutStateUtils.generate(parseResult.parsedPackage, ai2, 134217920);
                        if (packageInfo == null) {
                            throw new IllegalStateException("Unable to generate package info: " + ai2.modulePath);
                        }
                        this.mAllPackagesCache.add(packageInfo);
                        for (ParsedApexSystemService service : parseResult.parsedPackage.getApexSystemServices()) {
                            String minSdkVersion = service.getMinSdkVersion();
                            ApexInfo[] allPkgs3 = allPkgs2;
                            if (minSdkVersion == null || UnboundedSdkLevel.isAtLeast(minSdkVersion)) {
                                int flags3 = flags2;
                                String maxSdkVersion = service.getMaxSdkVersion();
                                if (maxSdkVersion != null && !UnboundedSdkLevel.isAtMost(maxSdkVersion)) {
                                    Slog.d(ApexManager.TAG, String.format("ApexSystemService %s with max_sdk_version=%s is skipped", service.getName(), service.getMaxSdkVersion()));
                                    allPkgs2 = allPkgs3;
                                    flags2 = flags3;
                                } else {
                                    if (!ai2.isActive) {
                                        parsingApexInfo2 = parsingApexInfo3;
                                        parallelPackageParser2 = parallelPackageParser3;
                                    } else {
                                        String name = service.getName();
                                        int j = 0;
                                        while (true) {
                                            parsingApexInfo2 = parsingApexInfo3;
                                            if (j >= this.mApexSystemServices.size()) {
                                                parallelPackageParser2 = parallelPackageParser3;
                                                this.mApexSystemServices.add(new ApexSystemServiceInfo(service.getName(), service.getJarPath(), service.getInitOrder()));
                                                break;
                                            }
                                            ApexSystemServiceInfo info = this.mApexSystemServices.get(j);
                                            ParallelPackageParser parallelPackageParser4 = parallelPackageParser3;
                                            if (!info.getName().equals(name)) {
                                                j++;
                                                parsingApexInfo3 = parsingApexInfo2;
                                                parallelPackageParser3 = parallelPackageParser4;
                                            } else {
                                                throw new IllegalStateException(TextUtils.formatSimple("Duplicate apex-system-service %s from %s, %s", new Object[]{name, info.mJarPath, service.getJarPath()}));
                                            }
                                        }
                                    }
                                    allPkgs2 = allPkgs3;
                                    flags2 = flags3;
                                    parsingApexInfo3 = parsingApexInfo2;
                                    parallelPackageParser3 = parallelPackageParser2;
                                }
                            } else {
                                Slog.d(ApexManager.TAG, String.format("ApexSystemService %s with min_sdk_version=%s is skipped", service.getName(), service.getMinSdkVersion()));
                                allPkgs2 = allPkgs3;
                                flags2 = flags2;
                            }
                        }
                        allPkgs = allPkgs2;
                        flags = flags2;
                        parsingApexInfo = parsingApexInfo3;
                        parallelPackageParser = parallelPackageParser3;
                        Collections.sort(this.mApexSystemServices);
                        this.mPackageNameToApexModuleName.put(packageInfo.packageName, ai2.moduleName);
                        if (ai2.isActive) {
                            if (activePackagesSet.contains(packageInfo.packageName)) {
                                throw new IllegalStateException("Two active packages have the same name: " + packageInfo.packageName);
                            }
                            activePackagesSet.add(packageInfo.packageName);
                            if (this.mApexModuleNameToActivePackageName.containsKey(ai2.moduleName)) {
                                throw new IllegalStateException("Two active packages have the same APEX module name: " + ai2.moduleName);
                            }
                            this.mApexModuleNameToActivePackageName.put(ai2.moduleName, packageInfo.packageName);
                        }
                        if (!ai2.isFactory) {
                            continue;
                        } else if (factoryPackagesSet.contains(packageInfo.packageName) && !ai2.moduleName.startsWith(ApexManager.VNDK_APEX_MODULE_NAME_PREFIX)) {
                            throw new IllegalStateException("Two factory packages have the same name: " + packageInfo.packageName);
                        } else {
                            factoryPackagesSet.add(packageInfo.packageName);
                        }
                    } else {
                        allPkgs = allPkgs2;
                        flags = flags2;
                        parsingApexInfo = parsingApexInfo3;
                        parallelPackageParser = parallelPackageParser3;
                        if (throwable instanceof PackageManagerException) {
                            PackageManagerException e = (PackageManagerException) throwable;
                            if (e.error == -123) {
                                Slog.w(ApexManager.TAG, "Scan apex failed, not a coreApp:" + ai2.modulePath);
                            } else {
                                throw new IllegalStateException("Unable to parse: " + ai2.modulePath, throwable);
                            }
                        } else {
                            throw new IllegalStateException("Unexpected exception occurred while parsing " + ai2.modulePath, throwable);
                        }
                    }
                    i++;
                    allPkgs2 = allPkgs;
                    flags2 = flags;
                    parsingApexInfo3 = parsingApexInfo;
                    parallelPackageParser3 = parallelPackageParser;
                }
            } catch (RemoteException re) {
                Slog.e(ApexManager.TAG, "Unable to retrieve packages from apexservice: " + re.toString());
                throw new RuntimeException(re);
            }
        }

        @Override // com.android.server.pm.ApexManager
        public PackageInfo getPackageInfo(String packageName, int flags) {
            synchronized (this.mLock) {
                Preconditions.checkState(this.mAllPackagesCache != null, "APEX packages have not been scanned");
                boolean matchActive = (flags & 1) != 0;
                boolean matchFactory = (flags & 2) != 0;
                int size = this.mAllPackagesCache.size();
                for (int i = 0; i < size; i++) {
                    PackageInfo packageInfo = this.mAllPackagesCache.get(i);
                    if (packageInfo.packageName.equals(packageName) && ((matchActive && isActive(packageInfo)) || (matchFactory && isFactory(packageInfo)))) {
                        return packageInfo;
                    }
                }
                return null;
            }
        }

        @Override // com.android.server.pm.ApexManager
        List<PackageInfo> getActivePackages() {
            List<PackageInfo> activePackages;
            synchronized (this.mLock) {
                Preconditions.checkState(this.mAllPackagesCache != null, "APEX packages have not been scanned");
                activePackages = new ArrayList<>();
                for (int i = 0; i < this.mAllPackagesCache.size(); i++) {
                    PackageInfo packageInfo = this.mAllPackagesCache.get(i);
                    if (isActive(packageInfo)) {
                        activePackages.add(packageInfo);
                    }
                }
            }
            return activePackages;
        }

        @Override // com.android.server.pm.ApexManager
        List<PackageInfo> getFactoryPackages() {
            List<PackageInfo> factoryPackages;
            synchronized (this.mLock) {
                Preconditions.checkState(this.mAllPackagesCache != null, "APEX packages have not been scanned");
                factoryPackages = new ArrayList<>();
                for (int i = 0; i < this.mAllPackagesCache.size(); i++) {
                    PackageInfo packageInfo = this.mAllPackagesCache.get(i);
                    if (isFactory(packageInfo)) {
                        factoryPackages.add(packageInfo);
                    }
                }
            }
            return factoryPackages;
        }

        @Override // com.android.server.pm.ApexManager
        List<PackageInfo> getInactivePackages() {
            List<PackageInfo> inactivePackages;
            synchronized (this.mLock) {
                Preconditions.checkState(this.mAllPackagesCache != null, "APEX packages have not been scanned");
                inactivePackages = new ArrayList<>();
                for (int i = 0; i < this.mAllPackagesCache.size(); i++) {
                    PackageInfo packageInfo = this.mAllPackagesCache.get(i);
                    if (!isActive(packageInfo)) {
                        inactivePackages.add(packageInfo);
                    }
                }
            }
            return inactivePackages;
        }

        @Override // com.android.server.pm.ApexManager
        boolean isApexPackage(String packageName) {
            if (isApexSupported()) {
                synchronized (this.mLock) {
                    Preconditions.checkState(this.mAllPackagesCache != null, "APEX packages have not been scanned");
                    int size = this.mAllPackagesCache.size();
                    for (int i = 0; i < size; i++) {
                        PackageInfo packageInfo = this.mAllPackagesCache.get(i);
                        if (packageInfo.packageName.equals(packageName)) {
                            return true;
                        }
                    }
                    return false;
                }
            }
            return false;
        }

        @Override // com.android.server.pm.ApexManager
        public String getActiveApexPackageNameContainingPackage(String containedPackageName) {
            Objects.requireNonNull(containedPackageName);
            synchronized (this.mLock) {
                Preconditions.checkState(this.mPackageNameToApexModuleName != null, "APEX packages have not been scanned");
                int numApksInApex = this.mApksInApex.size();
                for (int apkInApexNum = 0; apkInApexNum < numApksInApex; apkInApexNum++) {
                    if (this.mApksInApex.valueAt(apkInApexNum).contains(containedPackageName)) {
                        String apexModuleName = this.mApksInApex.keyAt(apkInApexNum);
                        int numApexPkgs = this.mPackageNameToApexModuleName.size();
                        for (int apexPkgNum = 0; apexPkgNum < numApexPkgs; apexPkgNum++) {
                            if (this.mPackageNameToApexModuleName.valueAt(apexPkgNum).equals(apexModuleName)) {
                                return this.mPackageNameToApexModuleName.keyAt(apexPkgNum);
                            }
                        }
                        continue;
                    }
                }
                return null;
            }
        }

        @Override // com.android.server.pm.ApexManager
        ApexSessionInfo getStagedSessionInfo(int sessionId) {
            try {
                ApexSessionInfo apexSessionInfo = waitForApexService().getStagedSessionInfo(sessionId);
                if (apexSessionInfo.isUnknown) {
                    return null;
                }
                return apexSessionInfo;
            } catch (RemoteException re) {
                Slog.e(ApexManager.TAG, "Unable to contact apexservice", re);
                throw new RuntimeException(re);
            }
        }

        @Override // com.android.server.pm.ApexManager
        SparseArray<ApexSessionInfo> getSessions() {
            try {
                ApexSessionInfo[] sessions = waitForApexService().getSessions();
                SparseArray<ApexSessionInfo> result = new SparseArray<>(sessions.length);
                for (int i = 0; i < sessions.length; i++) {
                    result.put(sessions[i].sessionId, sessions[i]);
                }
                return result;
            } catch (RemoteException re) {
                Slog.e(ApexManager.TAG, "Unable to contact apexservice", re);
                throw new RuntimeException(re);
            }
        }

        @Override // com.android.server.pm.ApexManager
        ApexInfoList submitStagedSession(ApexSessionParams params) throws PackageManagerException {
            try {
                ApexInfoList apexInfoList = new ApexInfoList();
                waitForApexService().submitStagedSession(params, apexInfoList);
                return apexInfoList;
            } catch (RemoteException re) {
                Slog.e(ApexManager.TAG, "Unable to contact apexservice", re);
                throw new RuntimeException(re);
            } catch (Exception e) {
                throw new PackageManagerException(-22, "apexd verification failed : " + e.getMessage());
            }
        }

        @Override // com.android.server.pm.ApexManager
        ApexInfo[] getStagedApexInfos(ApexSessionParams params) {
            try {
                return waitForApexService().getStagedApexInfos(params);
            } catch (RemoteException re) {
                Slog.w(ApexManager.TAG, "Unable to contact apexservice" + re.getMessage());
                throw new RuntimeException(re);
            } catch (Exception e) {
                Slog.w(ApexManager.TAG, "Failed to collect staged apex infos" + e.getMessage());
                return new ApexInfo[0];
            }
        }

        @Override // com.android.server.pm.ApexManager
        void markStagedSessionReady(int sessionId) throws PackageManagerException {
            try {
                waitForApexService().markStagedSessionReady(sessionId);
            } catch (RemoteException re) {
                Slog.e(ApexManager.TAG, "Unable to contact apexservice", re);
                throw new RuntimeException(re);
            } catch (Exception e) {
                throw new PackageManagerException(-22, "Failed to mark apexd session as ready : " + e.getMessage());
            }
        }

        @Override // com.android.server.pm.ApexManager
        void markStagedSessionSuccessful(int sessionId) {
            try {
                waitForApexService().markStagedSessionSuccessful(sessionId);
            } catch (RemoteException re) {
                Slog.e(ApexManager.TAG, "Unable to contact apexservice", re);
                throw new RuntimeException(re);
            } catch (Exception e) {
                Slog.e(ApexManager.TAG, "Failed to mark session " + sessionId + " as successful", e);
            }
        }

        @Override // com.android.server.pm.ApexManager
        boolean isApexSupported() {
            return true;
        }

        @Override // com.android.server.pm.ApexManager
        boolean revertActiveSessions() {
            try {
                waitForApexService().revertActiveSessions();
                return true;
            } catch (RemoteException re) {
                Slog.e(ApexManager.TAG, "Unable to contact apexservice", re);
                return false;
            } catch (Exception e) {
                Slog.e(ApexManager.TAG, e.getMessage(), e);
                return false;
            }
        }

        @Override // com.android.server.pm.ApexManager
        boolean abortStagedSession(int sessionId) {
            try {
                waitForApexService().abortStagedSession(sessionId);
                return true;
            } catch (Exception e) {
                Slog.e(ApexManager.TAG, e.getMessage(), e);
                return false;
            }
        }

        @Override // com.android.server.pm.ApexManager
        boolean uninstallApex(String apexPackagePath) {
            try {
                waitForApexService().unstagePackages(Collections.singletonList(apexPackagePath));
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @Override // com.android.server.pm.ApexManager
        void registerApkInApex(AndroidPackage pkg) {
            synchronized (this.mLock) {
                for (ActiveApexInfo aai : this.mActiveApexInfosCache) {
                    if (pkg.getBaseApkPath().startsWith(aai.apexDirectory.getAbsolutePath() + File.separator)) {
                        List<String> apks = this.mApksInApex.get(aai.apexModuleName);
                        if (apks == null) {
                            apks = Lists.newArrayList();
                            this.mApksInApex.put(aai.apexModuleName, apks);
                        }
                        Slog.i(ApexManager.TAG, "Registering " + pkg.getPackageName() + " as apk-in-apex of " + aai.apexModuleName);
                        apks.add(pkg.getPackageName());
                    }
                }
            }
        }

        @Override // com.android.server.pm.ApexManager
        void reportErrorWithApkInApex(String scanDirPath, String errorMsg) {
            synchronized (this.mLock) {
                for (ActiveApexInfo aai : this.mActiveApexInfosCache) {
                    if (scanDirPath.startsWith(aai.apexDirectory.getAbsolutePath())) {
                        this.mErrorWithApkInApex.put(aai.apexModuleName, errorMsg);
                    }
                }
            }
        }

        @Override // com.android.server.pm.ApexManager
        String getApkInApexInstallError(String apexPackageName) {
            synchronized (this.mLock) {
                Preconditions.checkState(this.mPackageNameToApexModuleName != null, "APEX packages have not been scanned");
                String moduleName = this.mPackageNameToApexModuleName.get(apexPackageName);
                if (moduleName == null) {
                    return null;
                }
                return this.mErrorWithApkInApex.get(moduleName);
            }
        }

        @Override // com.android.server.pm.ApexManager
        List<String> getApksInApex(String apexPackageName) {
            synchronized (this.mLock) {
                Preconditions.checkState(this.mPackageNameToApexModuleName != null, "APEX packages have not been scanned");
                String moduleName = this.mPackageNameToApexModuleName.get(apexPackageName);
                if (moduleName == null) {
                    return Collections.emptyList();
                }
                return this.mApksInApex.getOrDefault(moduleName, Collections.emptyList());
            }
        }

        @Override // com.android.server.pm.ApexManager
        public String getApexModuleNameForPackageName(String apexPackageName) {
            String str;
            synchronized (this.mLock) {
                Preconditions.checkState(this.mPackageNameToApexModuleName != null, "APEX packages have not been scanned");
                str = this.mPackageNameToApexModuleName.get(apexPackageName);
            }
            return str;
        }

        @Override // com.android.server.pm.ApexManager
        public String getActivePackageNameForApexModuleName(String apexModuleName) {
            String str;
            synchronized (this.mLock) {
                Preconditions.checkState(this.mApexModuleNameToActivePackageName != null, "APEX packages have not been scanned");
                str = this.mApexModuleNameToActivePackageName.get(apexModuleName);
            }
            return str;
        }

        @Override // com.android.server.pm.ApexManager
        public boolean snapshotCeData(int userId, int rollbackId, String apexPackageName) {
            String apexModuleName;
            synchronized (this.mLock) {
                Preconditions.checkState(this.mPackageNameToApexModuleName != null, "APEX packages have not been scanned");
                apexModuleName = this.mPackageNameToApexModuleName.get(apexPackageName);
            }
            if (apexModuleName == null) {
                Slog.e(ApexManager.TAG, "Invalid apex package name: " + apexPackageName);
                return false;
            }
            try {
                waitForApexService().snapshotCeData(userId, rollbackId, apexModuleName);
                return true;
            } catch (Exception e) {
                Slog.e(ApexManager.TAG, e.getMessage(), e);
                return false;
            }
        }

        @Override // com.android.server.pm.ApexManager
        public boolean restoreCeData(int userId, int rollbackId, String apexPackageName) {
            String apexModuleName;
            synchronized (this.mLock) {
                Preconditions.checkState(this.mPackageNameToApexModuleName != null, "APEX packages have not been scanned");
                apexModuleName = this.mPackageNameToApexModuleName.get(apexPackageName);
            }
            if (apexModuleName == null) {
                Slog.e(ApexManager.TAG, "Invalid apex package name: " + apexPackageName);
                return false;
            }
            try {
                waitForApexService().restoreCeData(userId, rollbackId, apexModuleName);
                return true;
            } catch (Exception e) {
                Slog.e(ApexManager.TAG, e.getMessage(), e);
                return false;
            }
        }

        @Override // com.android.server.pm.ApexManager
        public boolean destroyDeSnapshots(int rollbackId) {
            try {
                waitForApexService().destroyDeSnapshots(rollbackId);
                return true;
            } catch (Exception e) {
                Slog.e(ApexManager.TAG, e.getMessage(), e);
                return false;
            }
        }

        @Override // com.android.server.pm.ApexManager
        public boolean destroyCeSnapshots(int userId, int rollbackId) {
            try {
                waitForApexService().destroyCeSnapshots(userId, rollbackId);
                return true;
            } catch (Exception e) {
                Slog.e(ApexManager.TAG, e.getMessage(), e);
                return false;
            }
        }

        @Override // com.android.server.pm.ApexManager
        public boolean destroyCeSnapshotsNotSpecified(int userId, int[] retainRollbackIds) {
            try {
                waitForApexService().destroyCeSnapshotsNotSpecified(userId, retainRollbackIds);
                return true;
            } catch (Exception e) {
                Slog.e(ApexManager.TAG, e.getMessage(), e);
                return false;
            }
        }

        @Override // com.android.server.pm.ApexManager
        public void markBootCompleted() {
            try {
                waitForApexService().markBootCompleted();
            } catch (RemoteException re) {
                Slog.e(ApexManager.TAG, "Unable to contact apexservice", re);
            }
        }

        @Override // com.android.server.pm.ApexManager
        public long calculateSizeForCompressedApex(CompressedApexInfoList infoList) throws RemoteException {
            return waitForApexService().calculateSizeForCompressedApex(infoList);
        }

        @Override // com.android.server.pm.ApexManager
        public void reserveSpaceForCompressedApex(CompressedApexInfoList infoList) throws RemoteException {
            waitForApexService().reserveSpaceForCompressedApex(infoList);
        }

        private SigningDetails getSigningDetails(PackageInfo pkg) throws PackageManagerException {
            int minSignatureScheme = ApkSignatureVerifier.getMinimumSignatureSchemeVersionForTargetSdk(pkg.applicationInfo.targetSdkVersion);
            ParseTypeImpl input = ParseTypeImpl.forDefaultParsing();
            ParseResult<SigningDetails> result = ApkSignatureVerifier.verify(input, pkg.applicationInfo.sourceDir, minSignatureScheme);
            if (result.isError()) {
                throw new PackageManagerException(result.getErrorCode(), result.getErrorMessage(), result.getException());
            }
            return (SigningDetails) result.getResult();
        }

        private void checkApexSignature(PackageInfo existingApexPkg, PackageInfo newApexPkg) throws PackageManagerException {
            SigningDetails existingSigningDetails = getSigningDetails(existingApexPkg);
            SigningDetails newSigningDetails = getSigningDetails(newApexPkg);
            if (!newSigningDetails.checkCapability(existingSigningDetails, 1)) {
                throw new PackageManagerException(-118, "APK container signature of " + newApexPkg.applicationInfo.sourceDir + " is not compatible with currently installed on device");
            }
        }

        @Override // com.android.server.pm.ApexManager
        void installPackage(File apexFile, PackageParser2 packageParser) throws PackageManagerException {
            try {
                ParsedPackage parsedPackage = packageParser.parsePackage(apexFile, 134217920, false);
                PackageInfo newApexPkg = PackageInfoWithoutStateUtils.generate(parsedPackage, null, 134217920);
                if (newApexPkg == null) {
                    throw new PackageManagerException(-2, "Failed to generate package info for " + apexFile.getAbsolutePath());
                }
                PackageInfo existingApexPkg = getPackageInfo(newApexPkg.packageName, 1);
                if (existingApexPkg == null) {
                    Slog.w(ApexManager.TAG, "Attempting to install new APEX package " + newApexPkg.packageName);
                    throw new PackageManagerException(-23, "It is forbidden to install new APEX packages");
                }
                checkApexSignature(existingApexPkg, newApexPkg);
                ApexInfo apexInfo = waitForApexService().installAndActivatePackage(apexFile.getAbsolutePath());
                ParsedPackage parsedPackage2 = packageParser.parsePackage(new File(apexInfo.modulePath), 134217920, false);
                PackageInfo finalApexPkg = PackageInfoWithoutStateUtils.generate(parsedPackage2, apexInfo, 134217920);
                synchronized (this.mLock) {
                    if (isFactory(existingApexPkg)) {
                        existingApexPkg.applicationInfo.flags &= -8388609;
                        this.mAllPackagesCache.add(finalApexPkg);
                    } else {
                        int i = 0;
                        int size = this.mAllPackagesCache.size();
                        while (true) {
                            if (i >= size) {
                                break;
                            } else if (!this.mAllPackagesCache.get(i).equals(existingApexPkg)) {
                                i++;
                            } else {
                                this.mAllPackagesCache.set(i, finalApexPkg);
                                break;
                            }
                        }
                    }
                }
            } catch (RemoteException e) {
                throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "apexservice not available");
            } catch (PackageManagerException e2) {
                throw e2;
            } catch (Exception e3) {
                throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, e3.getMessage());
            }
        }

        @Override // com.android.server.pm.ApexManager
        public List<ApexSystemServiceInfo> getApexSystemServices() {
            List<ApexSystemServiceInfo> list;
            synchronized (this.mLock) {
                Preconditions.checkState(this.mApexSystemServices != null, "APEX packages have not been scanned");
                list = this.mApexSystemServices;
            }
            return list;
        }

        @Override // com.android.server.pm.ApexManager
        public File getBackingApexFile(File file) {
            Path path = file.toPath();
            if (path.startsWith(Environment.getApexDirectory().toPath()) && path.getNameCount() >= 2) {
                String moduleName = file.toPath().getName(1).toString();
                List<ActiveApexInfo> apexes = getActiveApexInfos();
                for (int i = 0; i < apexes.size(); i++) {
                    if (apexes.get(i).apexModuleName.equals(moduleName)) {
                        return apexes.get(i).apexFile;
                    }
                }
                return null;
            }
            return null;
        }

        void dumpFromPackagesCache(List<PackageInfo> packagesCache, String packageName, IndentingPrintWriter ipw) {
            ipw.println();
            ipw.increaseIndent();
            int size = packagesCache.size();
            for (int i = 0; i < size; i++) {
                PackageInfo pi = packagesCache.get(i);
                if (packageName == null || packageName.equals(pi.packageName)) {
                    ipw.println(pi.packageName);
                    ipw.increaseIndent();
                    ipw.println("Version: " + pi.versionCode);
                    ipw.println("VersionName: " + pi.versionName);
                    ipw.println("Path: " + pi.applicationInfo.sourceDir);
                    ipw.println("IsActive: " + isActive(pi));
                    ipw.println("IsFactory: " + isFactory(pi));
                    ipw.println("ApplicationInfo: ");
                    ipw.increaseIndent();
                    pi.applicationInfo.dump(new PrintWriterPrinter(ipw), "");
                    ipw.decreaseIndent();
                    ipw.decreaseIndent();
                }
            }
            ipw.decreaseIndent();
            ipw.println();
        }

        @Override // com.android.server.pm.ApexManager
        void dump(PrintWriter pw, String packageName) {
            IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ", 120);
            try {
                ipw.println();
                ipw.println("APEX session state:");
                ipw.increaseIndent();
                ApexSessionInfo[] sessions = waitForApexService().getSessions();
                for (ApexSessionInfo si : sessions) {
                    ipw.println("Session ID: " + si.sessionId);
                    ipw.increaseIndent();
                    if (si.isUnknown) {
                        ipw.println("State: UNKNOWN");
                    } else if (si.isVerified) {
                        ipw.println("State: VERIFIED");
                    } else if (si.isStaged) {
                        ipw.println("State: STAGED");
                    } else if (si.isActivated) {
                        ipw.println("State: ACTIVATED");
                    } else if (si.isActivationFailed) {
                        ipw.println("State: ACTIVATION FAILED");
                    } else if (si.isSuccess) {
                        ipw.println("State: SUCCESS");
                    } else if (si.isRevertInProgress) {
                        ipw.println("State: REVERT IN PROGRESS");
                    } else if (si.isReverted) {
                        ipw.println("State: REVERTED");
                    } else if (si.isRevertFailed) {
                        ipw.println("State: REVERT FAILED");
                    }
                    ipw.decreaseIndent();
                }
                ipw.decreaseIndent();
                ipw.println();
                synchronized (this.mLock) {
                    if (this.mAllPackagesCache == null) {
                        ipw.println("APEX packages have not been scanned");
                        return;
                    }
                    ipw.println("Active APEX packages:");
                    dumpFromPackagesCache(getActivePackages(), packageName, ipw);
                    ipw.println("Inactive APEX packages:");
                    dumpFromPackagesCache(getInactivePackages(), packageName, ipw);
                    ipw.println("Factory APEX packages:");
                    dumpFromPackagesCache(getFactoryPackages(), packageName, ipw);
                }
            } catch (RemoteException e) {
                ipw.println("Couldn't communicate with apexd.");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static final class ApexManagerFlattenedApex extends ApexManager {
        ApexManagerFlattenedApex() {
        }

        @Override // com.android.server.pm.ApexManager
        public List<ActiveApexInfo> getActiveApexInfos() {
            File[] files;
            List<ActiveApexInfo> result = new ArrayList<>();
            File apexDir = Environment.getApexDirectory();
            if (apexDir.isDirectory() && (files = apexDir.listFiles()) != null) {
                for (File file : files) {
                    if (file.isDirectory() && !file.getName().contains("@") && !file.getName().equals("com.android.art.debug")) {
                        result.add(new ActiveApexInfo(file, Environment.getRootDirectory(), file));
                    }
                }
            }
            return result;
        }

        @Override // com.android.server.pm.ApexManager
        void scanApexPackagesTraced(PackageParser2 packageParser, ExecutorService executorService) {
        }

        @Override // com.android.server.pm.ApexManager
        public PackageInfo getPackageInfo(String packageName, int flags) {
            return null;
        }

        @Override // com.android.server.pm.ApexManager
        List<PackageInfo> getActivePackages() {
            return Collections.emptyList();
        }

        @Override // com.android.server.pm.ApexManager
        List<PackageInfo> getFactoryPackages() {
            return Collections.emptyList();
        }

        @Override // com.android.server.pm.ApexManager
        List<PackageInfo> getInactivePackages() {
            return Collections.emptyList();
        }

        @Override // com.android.server.pm.ApexManager
        boolean isApexPackage(String packageName) {
            return false;
        }

        @Override // com.android.server.pm.ApexManager
        public String getActiveApexPackageNameContainingPackage(String containedPackageName) {
            Objects.requireNonNull(containedPackageName);
            return null;
        }

        @Override // com.android.server.pm.ApexManager
        ApexSessionInfo getStagedSessionInfo(int sessionId) {
            throw new UnsupportedOperationException();
        }

        @Override // com.android.server.pm.ApexManager
        SparseArray<ApexSessionInfo> getSessions() {
            return new SparseArray<>(0);
        }

        @Override // com.android.server.pm.ApexManager
        ApexInfoList submitStagedSession(ApexSessionParams params) throws PackageManagerException {
            throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Device doesn't support updating APEX");
        }

        @Override // com.android.server.pm.ApexManager
        ApexInfo[] getStagedApexInfos(ApexSessionParams params) {
            throw new UnsupportedOperationException();
        }

        @Override // com.android.server.pm.ApexManager
        void markStagedSessionReady(int sessionId) {
            throw new UnsupportedOperationException();
        }

        @Override // com.android.server.pm.ApexManager
        void markStagedSessionSuccessful(int sessionId) {
            throw new UnsupportedOperationException();
        }

        @Override // com.android.server.pm.ApexManager
        boolean isApexSupported() {
            return false;
        }

        @Override // com.android.server.pm.ApexManager
        boolean revertActiveSessions() {
            throw new UnsupportedOperationException();
        }

        @Override // com.android.server.pm.ApexManager
        boolean abortStagedSession(int sessionId) {
            throw new UnsupportedOperationException();
        }

        @Override // com.android.server.pm.ApexManager
        boolean uninstallApex(String apexPackagePath) {
            throw new UnsupportedOperationException();
        }

        @Override // com.android.server.pm.ApexManager
        void registerApkInApex(AndroidPackage pkg) {
        }

        @Override // com.android.server.pm.ApexManager
        void reportErrorWithApkInApex(String scanDirPath, String errorMsg) {
        }

        @Override // com.android.server.pm.ApexManager
        String getApkInApexInstallError(String apexPackageName) {
            return null;
        }

        @Override // com.android.server.pm.ApexManager
        List<String> getApksInApex(String apexPackageName) {
            return Collections.emptyList();
        }

        @Override // com.android.server.pm.ApexManager
        public String getApexModuleNameForPackageName(String apexPackageName) {
            return null;
        }

        @Override // com.android.server.pm.ApexManager
        public String getActivePackageNameForApexModuleName(String apexModuleName) {
            return null;
        }

        @Override // com.android.server.pm.ApexManager
        public boolean snapshotCeData(int userId, int rollbackId, String apexPackageName) {
            throw new UnsupportedOperationException();
        }

        @Override // com.android.server.pm.ApexManager
        public boolean restoreCeData(int userId, int rollbackId, String apexPackageName) {
            throw new UnsupportedOperationException();
        }

        @Override // com.android.server.pm.ApexManager
        public boolean destroyDeSnapshots(int rollbackId) {
            throw new UnsupportedOperationException();
        }

        @Override // com.android.server.pm.ApexManager
        public boolean destroyCeSnapshots(int userId, int rollbackId) {
            return true;
        }

        @Override // com.android.server.pm.ApexManager
        public boolean destroyCeSnapshotsNotSpecified(int userId, int[] retainRollbackIds) {
            return true;
        }

        @Override // com.android.server.pm.ApexManager
        public void markBootCompleted() {
        }

        @Override // com.android.server.pm.ApexManager
        public long calculateSizeForCompressedApex(CompressedApexInfoList infoList) {
            throw new UnsupportedOperationException();
        }

        @Override // com.android.server.pm.ApexManager
        public void reserveSpaceForCompressedApex(CompressedApexInfoList infoList) {
            throw new UnsupportedOperationException();
        }

        @Override // com.android.server.pm.ApexManager
        void installPackage(File apexFile, PackageParser2 packageParser) {
            throw new UnsupportedOperationException("APEX updates are not supported");
        }

        @Override // com.android.server.pm.ApexManager
        public List<ApexSystemServiceInfo> getApexSystemServices() {
            return Collections.emptyList();
        }

        @Override // com.android.server.pm.ApexManager
        public File getBackingApexFile(File file) {
            return null;
        }

        @Override // com.android.server.pm.ApexManager
        void dump(PrintWriter pw, String packageName) {
        }
    }
}
