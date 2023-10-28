package com.android.server.integrity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.integrity.AppInstallMetadata;
import android.content.integrity.IAppIntegrityManager;
import android.content.integrity.IntegrityUtils;
import android.content.integrity.Rule;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.Signature;
import android.content.pm.SigningDetails;
import android.content.pm.SigningInfo;
import android.content.pm.parsing.result.ParseResult;
import android.content.pm.parsing.result.ParseTypeImpl;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;
import android.util.apk.SourceStampVerificationResult;
import android.util.apk.SourceStampVerifier;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.LocalServices;
import com.android.server.integrity.AppIntegrityManagerServiceImpl;
import com.android.server.integrity.engine.RuleEvaluationEngine;
import com.android.server.integrity.model.IntegrityCheckResult;
import com.android.server.integrity.model.RuleMetadata;
import com.android.server.pm.parsing.PackageInfoUtils;
import com.android.server.pm.parsing.PackageParser2;
import com.android.server.pm.parsing.pkg.ParsedPackage;
import com.android.server.pm.pkg.PackageUserStateInternal;
import com.android.server.pm.pkg.parsing.ParsingPackageUtils;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
/* loaded from: classes.dex */
public class AppIntegrityManagerServiceImpl extends IAppIntegrityManager.Stub {
    public static final String ADB_INSTALLER = "adb";
    private static final String ALLOWED_INSTALLERS_METADATA_NAME = "allowed-installers";
    private static final String ALLOWED_INSTALLER_DELIMITER = ",";
    private static final String BASE_APK_FILE = "base.apk";
    public static final boolean DEBUG_INTEGRITY_COMPONENT = false;
    private static final String INSTALLER_PACKAGE_CERT_DELIMITER = "\\|";
    private static final Set<String> PACKAGE_INSTALLER = new HashSet(Arrays.asList("com.google.android.packageinstaller", "com.android.packageinstaller"));
    private static final String PACKAGE_MIME_TYPE = "application/vnd.android.package-archive";
    private static final String TAG = "AppIntegrityManagerServiceImpl";
    private static final String UNKNOWN_INSTALLER = "";
    private final Context mContext;
    private final RuleEvaluationEngine mEvaluationEngine;
    private final Handler mHandler;
    private final IntegrityFileManager mIntegrityFileManager;
    private final PackageManagerInternal mPackageManagerInternal;
    private final Supplier<PackageParser2> mParserSupplier;

    public static AppIntegrityManagerServiceImpl create(Context context) {
        HandlerThread handlerThread = new HandlerThread("AppIntegrityManagerServiceHandler");
        handlerThread.start();
        return new AppIntegrityManagerServiceImpl(context, (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class), new Supplier() { // from class: com.android.server.integrity.AppIntegrityManagerServiceImpl$$ExternalSyntheticLambda0
            @Override // java.util.function.Supplier
            public final Object get() {
                return PackageParser2.forParsingFileWithDefaults();
            }
        }, RuleEvaluationEngine.getRuleEvaluationEngine(), IntegrityFileManager.getInstance(), handlerThread.getThreadHandler());
    }

    AppIntegrityManagerServiceImpl(Context context, PackageManagerInternal packageManagerInternal, Supplier<PackageParser2> parserSupplier, RuleEvaluationEngine evaluationEngine, IntegrityFileManager integrityFileManager, Handler handler) {
        this.mContext = context;
        this.mPackageManagerInternal = packageManagerInternal;
        this.mParserSupplier = parserSupplier;
        this.mEvaluationEngine = evaluationEngine;
        this.mIntegrityFileManager = integrityFileManager;
        this.mHandler = handler;
        IntentFilter integrityVerificationFilter = new IntentFilter();
        integrityVerificationFilter.addAction("android.intent.action.PACKAGE_NEEDS_INTEGRITY_VERIFICATION");
        try {
            integrityVerificationFilter.addDataType(PACKAGE_MIME_TYPE);
            context.registerReceiver(new AnonymousClass1(), integrityVerificationFilter, null, handler);
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("Mime type malformed: should never happen.", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.integrity.AppIntegrityManagerServiceImpl$1  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass1 extends BroadcastReceiver {
        AnonymousClass1() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, final Intent intent) {
            if (!"android.intent.action.PACKAGE_NEEDS_INTEGRITY_VERIFICATION".equals(intent.getAction())) {
                return;
            }
            AppIntegrityManagerServiceImpl.this.mHandler.post(new Runnable() { // from class: com.android.server.integrity.AppIntegrityManagerServiceImpl$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    AppIntegrityManagerServiceImpl.AnonymousClass1.this.m4042xc00f06f0(intent);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onReceive$0$com-android-server-integrity-AppIntegrityManagerServiceImpl$1  reason: not valid java name */
        public /* synthetic */ void m4042xc00f06f0(Intent intent) {
            AppIntegrityManagerServiceImpl.this.handleIntegrityVerification(intent);
        }
    }

    public void updateRuleSet(final String version, final ParceledListSlice<Rule> rules, final IntentSender statusReceiver) {
        final String ruleProvider = getCallerPackageNameOrThrow(Binder.getCallingUid());
        this.mHandler.post(new Runnable() { // from class: com.android.server.integrity.AppIntegrityManagerServiceImpl$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                AppIntegrityManagerServiceImpl.this.m4041x15682a9c(version, ruleProvider, rules, statusReceiver);
            }
        });
    }

    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: boolean */
    /* JADX DEBUG: Multi-variable search result rejected for r1v1, resolved type: boolean */
    /* JADX DEBUG: Multi-variable search result rejected for r1v2, resolved type: boolean */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Multi-variable type inference failed */
    /* renamed from: lambda$updateRuleSet$0$com-android-server-integrity-AppIntegrityManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ void m4041x15682a9c(String version, String ruleProvider, ParceledListSlice rules, IntentSender statusReceiver) {
        boolean success = 1;
        try {
            this.mIntegrityFileManager.writeRules(version, ruleProvider, rules.getList());
        } catch (Exception e) {
            Slog.e(TAG, "Error writing rules.", e);
            success = 0;
        }
        FrameworkStatsLog.write((int) FrameworkStatsLog.INTEGRITY_RULES_PUSHED, success, ruleProvider, version);
        Intent intent = new Intent();
        intent.putExtra("android.content.integrity.extra.STATUS", !success);
        try {
            statusReceiver.sendIntent(this.mContext, 0, intent, null, null);
        } catch (Exception e2) {
            Slog.e(TAG, "Error sending status feedback.", e2);
        }
    }

    public String getCurrentRuleSetVersion() {
        getCallerPackageNameOrThrow(Binder.getCallingUid());
        RuleMetadata ruleMetadata = this.mIntegrityFileManager.readMetadata();
        if (ruleMetadata != null && ruleMetadata.getVersion() != null) {
            return ruleMetadata.getVersion();
        }
        return "";
    }

    public String getCurrentRuleSetProvider() {
        getCallerPackageNameOrThrow(Binder.getCallingUid());
        RuleMetadata ruleMetadata = this.mIntegrityFileManager.readMetadata();
        if (ruleMetadata != null && ruleMetadata.getRuleProvider() != null) {
            return ruleMetadata.getRuleProvider();
        }
        return "";
    }

    public ParceledListSlice<Rule> getCurrentRules() {
        List<Rule> rules = Collections.emptyList();
        try {
            rules = this.mIntegrityFileManager.readRules(null);
        } catch (Exception e) {
            Slog.e(TAG, "Error getting current rules", e);
        }
        return new ParceledListSlice<>(rules);
    }

    public List<String> getWhitelistedRuleProviders() {
        return getAllowedRuleProviderSystemApps();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleIntegrityVerification(Intent intent) {
        int i;
        int verificationId = intent.getIntExtra("android.content.pm.extra.VERIFICATION_ID", -1);
        try {
            String installerPackageName = getInstallerPackageName(intent);
            if (!integrityCheckIncludesRuleProvider() && isRuleProvider(installerPackageName)) {
                this.mPackageManagerInternal.setIntegrityVerificationResult(verificationId, 1);
                return;
            }
            String packageName = intent.getStringExtra("android.intent.extra.PACKAGE_NAME");
            PackageInfo packageInfo = getPackageArchiveInfo(intent.getData());
            if (packageInfo == null) {
                Slog.w(TAG, "Cannot parse package " + packageName);
                this.mPackageManagerInternal.setIntegrityVerificationResult(verificationId, 1);
                return;
            }
            List<String> appCertificates = getCertificateFingerprint(packageInfo);
            List<String> appCertificateLineage = getCertificateLineage(packageInfo);
            List<String> installerCertificates = getInstallerCertificateFingerprint(installerPackageName);
            AppInstallMetadata.Builder builder = new AppInstallMetadata.Builder();
            builder.setPackageName(getPackageNameNormalized(packageName));
            builder.setAppCertificates(appCertificates);
            builder.setAppCertificateLineage(appCertificateLineage);
            builder.setVersionCode(intent.getLongExtra("android.intent.extra.LONG_VERSION_CODE", -1L));
            builder.setInstallerName(getPackageNameNormalized(installerPackageName));
            builder.setInstallerCertificates(installerCertificates);
            builder.setIsPreInstalled(isSystemApp(packageName));
            builder.setAllowedInstallersAndCert(getAllowedInstallers(packageInfo));
            extractSourceStamp(intent.getData(), builder);
            AppInstallMetadata appInstallMetadata = builder.build();
            IntegrityCheckResult result = this.mEvaluationEngine.evaluate(appInstallMetadata);
            if (!result.getMatchedRules().isEmpty()) {
                Slog.i(TAG, String.format("Integrity check of %s result: %s due to %s", packageName, result.getEffect(), result.getMatchedRules()));
            }
            FrameworkStatsLog.write((int) FrameworkStatsLog.INTEGRITY_CHECK_RESULT_REPORTED, packageName, appCertificates.toString(), appInstallMetadata.getVersionCode(), installerPackageName, result.getLoggingResponse(), result.isCausedByAppCertRule(), result.isCausedByInstallerRule());
            PackageManagerInternal packageManagerInternal = this.mPackageManagerInternal;
            if (result.getEffect() == IntegrityCheckResult.Effect.ALLOW) {
                i = 1;
            } else {
                i = 0;
            }
            packageManagerInternal.setIntegrityVerificationResult(verificationId, i);
        } catch (IllegalArgumentException e) {
            Slog.e(TAG, "Invalid input to integrity verification", e);
            this.mPackageManagerInternal.setIntegrityVerificationResult(verificationId, 0);
        } catch (Exception e2) {
            Slog.e(TAG, "Error handling integrity verification", e2);
            this.mPackageManagerInternal.setIntegrityVerificationResult(verificationId, 1);
        }
    }

    private String getInstallerPackageName(Intent intent) {
        String installer = intent.getStringExtra("android.content.pm.extra.VERIFICATION_INSTALLER_PACKAGE");
        if (installer == null) {
            return ADB_INSTALLER;
        }
        int installerUid = intent.getIntExtra("android.content.pm.extra.VERIFICATION_INSTALLER_UID", -1);
        if (installerUid < 0) {
            Slog.e(TAG, "Installer cannot be determined: installer: " + installer + " installer UID: " + installerUid);
            return "";
        } else if (getPackageListForUid(installerUid).contains(installer)) {
            if (PACKAGE_INSTALLER.contains(installer)) {
                int originatingUid = intent.getIntExtra("android.intent.extra.ORIGINATING_UID", -1);
                if (originatingUid < 0) {
                    Slog.e(TAG, "Installer is package installer but originating UID not found.");
                    return "";
                }
                List<String> installerPackages = getPackageListForUid(originatingUid);
                if (installerPackages.isEmpty()) {
                    Slog.e(TAG, "No package found associated with originating UID " + originatingUid);
                    return "";
                }
                return installerPackages.get(0);
            }
            return installer;
        } else {
            return "";
        }
    }

    private String getPackageNameNormalized(String packageName) {
        if (packageName.length() <= 32) {
            return packageName;
        }
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = messageDigest.digest(packageName.getBytes(StandardCharsets.UTF_8));
            return IntegrityUtils.getHexDigest(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private List<String> getInstallerCertificateFingerprint(String installer) {
        if (installer.equals(ADB_INSTALLER) || installer.equals("")) {
            return Collections.emptyList();
        }
        try {
            PackageInfo installerInfo = this.mContext.getPackageManager().getPackageInfo(installer, 134217728);
            return getCertificateFingerprint(installerInfo);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.w(TAG, "Installer package " + installer + " not found.");
            return Collections.emptyList();
        }
    }

    private List<String> getCertificateFingerprint(PackageInfo packageInfo) {
        Signature[] signatures;
        ArrayList<String> certificateFingerprints = new ArrayList<>();
        for (Signature signature : getSignatures(packageInfo)) {
            certificateFingerprints.add(getFingerprint(signature));
        }
        return certificateFingerprints;
    }

    private List<String> getCertificateLineage(PackageInfo packageInfo) {
        Signature[] signatureLineage;
        ArrayList<String> certificateLineage = new ArrayList<>();
        for (Signature signature : getSignatureLineage(packageInfo)) {
            certificateLineage.add(getFingerprint(signature));
        }
        return certificateLineage;
    }

    private Map<String, String> getAllowedInstallers(PackageInfo packageInfo) {
        Map<String, String> packageCertMap = new HashMap<>();
        if (packageInfo.applicationInfo != null && packageInfo.applicationInfo.metaData != null) {
            Bundle metaData = packageInfo.applicationInfo.metaData;
            String allowedInstallers = metaData.getString(ALLOWED_INSTALLERS_METADATA_NAME);
            if (allowedInstallers != null) {
                String[] installerCertPairs = allowedInstallers.split(ALLOWED_INSTALLER_DELIMITER);
                for (String packageCertPair : installerCertPairs) {
                    String[] packageAndCert = packageCertPair.split(INSTALLER_PACKAGE_CERT_DELIMITER);
                    if (packageAndCert.length == 2) {
                        String packageName = getPackageNameNormalized(packageAndCert[0]);
                        String cert = packageAndCert[1];
                        packageCertMap.put(packageName, cert);
                    } else if (packageAndCert.length == 1) {
                        packageCertMap.put(getPackageNameNormalized(packageAndCert[0]), "");
                    }
                }
            }
        }
        return packageCertMap;
    }

    private void extractSourceStamp(Uri dataUri, AppInstallMetadata.Builder appInstallMetadata) {
        SourceStampVerificationResult sourceStampVerificationResult;
        File installationPath = getInstallationPath(dataUri);
        if (installationPath == null) {
            throw new IllegalArgumentException("Installation path is null, package not found");
        }
        if (installationPath.isDirectory()) {
            try {
                Stream<Path> filesList = Files.list(installationPath.toPath());
                List<String> apkFiles = (List) filesList.map(new Function() { // from class: com.android.server.integrity.AppIntegrityManagerServiceImpl$$ExternalSyntheticLambda2
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        String path;
                        path = ((Path) obj).toAbsolutePath().toString();
                        return path;
                    }
                }).collect(Collectors.toList());
                sourceStampVerificationResult = SourceStampVerifier.verify(apkFiles);
                if (filesList != null) {
                    filesList.close();
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not read APK directory");
            }
        } else {
            sourceStampVerificationResult = SourceStampVerifier.verify(installationPath.getAbsolutePath());
        }
        appInstallMetadata.setIsStampPresent(sourceStampVerificationResult.isPresent());
        appInstallMetadata.setIsStampVerified(sourceStampVerificationResult.isVerified());
        appInstallMetadata.setIsStampTrusted(sourceStampVerificationResult.isVerified());
        if (sourceStampVerificationResult.isVerified()) {
            X509Certificate sourceStampCertificate = (X509Certificate) sourceStampVerificationResult.getCertificate();
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] certificateDigest = digest.digest(sourceStampCertificate.getEncoded());
                appInstallMetadata.setStampCertificateHash(IntegrityUtils.getHexDigest(certificateDigest));
            } catch (NoSuchAlgorithmException | CertificateEncodingException e2) {
                throw new IllegalArgumentException("Error computing source stamp certificate digest", e2);
            }
        }
    }

    private static Signature[] getSignatures(PackageInfo packageInfo) {
        SigningInfo signingInfo = packageInfo.signingInfo;
        if (signingInfo == null || signingInfo.getApkContentsSigners().length < 1) {
            throw new IllegalArgumentException("Package signature not found in " + packageInfo);
        }
        return signingInfo.getApkContentsSigners();
    }

    private static Signature[] getSignatureLineage(PackageInfo packageInfo) {
        SigningInfo signingInfo = packageInfo.signingInfo;
        if (signingInfo == null) {
            throw new IllegalArgumentException("Package signature not found in " + packageInfo);
        }
        Signature[] signatureLineage = getSignatures(packageInfo);
        if (!signingInfo.hasMultipleSigners() && signingInfo.hasPastSigningCertificates()) {
            Signature[] pastSignatures = signingInfo.getSigningCertificateHistory();
            Signature[] allSignatures = new Signature[signatureLineage.length + pastSignatures.length];
            int i = 0;
            while (i < signatureLineage.length) {
                allSignatures[i] = signatureLineage[i];
                i++;
            }
            for (Signature signature : pastSignatures) {
                allSignatures[i] = signature;
                i++;
            }
            return allSignatures;
        }
        return signatureLineage;
    }

    private static String getFingerprint(Signature cert) {
        InputStream input = new ByteArrayInputStream(cert.toByteArray());
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X509");
            X509Certificate certificate = null;
            if (factory != null) {
                try {
                    certificate = (X509Certificate) factory.generateCertificate(input);
                } catch (CertificateException e) {
                    throw new RuntimeException("Error getting X509Certificate", e);
                }
            }
            if (certificate == null) {
                throw new RuntimeException("X509 Certificate not found");
            }
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] publicKey = digest.digest(certificate.getEncoded());
                return IntegrityUtils.getHexDigest(publicKey);
            } catch (NoSuchAlgorithmException | CertificateEncodingException e2) {
                throw new IllegalArgumentException("Error error computing fingerprint", e2);
            }
        } catch (CertificateException e3) {
            throw new RuntimeException("Error getting CertificateFactory", e3);
        }
    }

    private PackageInfo getPackageArchiveInfo(Uri dataUri) {
        File installationPath = getInstallationPath(dataUri);
        if (installationPath != null) {
            try {
                PackageParser2 parser = this.mParserSupplier.get();
                ParsedPackage pkg = parser.parsePackage(installationPath, 0, false);
                ParseTypeImpl input = ParseTypeImpl.forDefaultParsing();
                ParseResult<SigningDetails> result = ParsingPackageUtils.getSigningDetails(input, pkg, true);
                if (result.isError()) {
                    Slog.w(TAG, result.getErrorMessage(), result.getException());
                    if (parser != null) {
                        parser.close();
                    }
                    return null;
                }
                pkg.setSigningDetails((SigningDetails) result.getResult());
                PackageInfo generate = PackageInfoUtils.generate(pkg, null, 134217856, 0L, 0L, null, PackageUserStateInternal.DEFAULT, UserHandle.getCallingUserId(), null);
                if (parser != null) {
                    parser.close();
                }
                return generate;
            } catch (Exception e) {
                Slog.w(TAG, "Exception reading " + dataUri, e);
                return null;
            }
        }
        throw new IllegalArgumentException("Installation path is null, package not found");
    }

    private PackageInfo getMultiApkInfo(File multiApkDirectory) {
        File baseFile = new File(multiApkDirectory, BASE_APK_FILE);
        PackageInfo basePackageInfo = this.mContext.getPackageManager().getPackageArchiveInfo(baseFile.getAbsolutePath(), 134217856);
        if (basePackageInfo == null) {
            File[] listFiles = multiApkDirectory.listFiles();
            int length = listFiles.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                File apkFile = listFiles[i];
                if (!apkFile.isDirectory()) {
                    try {
                        basePackageInfo = this.mContext.getPackageManager().getPackageArchiveInfo(apkFile.getAbsolutePath(), 134217856);
                    } catch (Exception e) {
                        Slog.w(TAG, "Exception reading " + apkFile, e);
                    }
                    if (basePackageInfo != null) {
                        Slog.i(TAG, "Found package info from " + apkFile);
                        break;
                    }
                }
                i++;
            }
        }
        if (basePackageInfo == null) {
            throw new IllegalArgumentException("Base package info cannot be found from installation directory");
        }
        return basePackageInfo;
    }

    private File getInstallationPath(Uri dataUri) {
        if (dataUri == null) {
            throw new IllegalArgumentException("Null data uri");
        }
        String scheme = dataUri.getScheme();
        if (!"file".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Unsupported scheme for " + dataUri);
        }
        File installationPath = new File(dataUri.getPath());
        if (!installationPath.exists()) {
            throw new IllegalArgumentException("Cannot find file for " + dataUri);
        }
        if (!installationPath.canRead()) {
            throw new IllegalArgumentException("Cannot read file for " + dataUri);
        }
        return installationPath;
    }

    private String getCallerPackageNameOrThrow(int callingUid) {
        String callerPackageName = getCallingRulePusherPackageName(callingUid);
        if (callerPackageName == null) {
            throw new SecurityException("Only system packages specified in config_integrityRuleProviderPackages are allowed to call this method.");
        }
        return callerPackageName;
    }

    private String getCallingRulePusherPackageName(int callingUid) {
        List<String> allowedRuleProviders = getAllowedRuleProviderSystemApps();
        List<String> callingPackageNames = getPackageListForUid(callingUid);
        List<String> allowedCallingPackages = new ArrayList<>();
        for (String packageName : callingPackageNames) {
            if (allowedRuleProviders.contains(packageName)) {
                allowedCallingPackages.add(packageName);
            }
        }
        if (allowedCallingPackages.isEmpty()) {
            return null;
        }
        return allowedCallingPackages.get(0);
    }

    private boolean isRuleProvider(String installerPackageName) {
        for (String ruleProvider : getAllowedRuleProviderSystemApps()) {
            if (ruleProvider.matches(installerPackageName)) {
                return true;
            }
        }
        return false;
    }

    private List<String> getAllowedRuleProviderSystemApps() {
        List<String> integrityRuleProviders = Arrays.asList(this.mContext.getResources().getStringArray(17236079));
        List<String> systemAppRuleProviders = new ArrayList<>();
        for (String ruleProvider : integrityRuleProviders) {
            if (isSystemApp(ruleProvider)) {
                systemAppRuleProviders.add(ruleProvider);
            }
        }
        return systemAppRuleProviders;
    }

    private boolean isSystemApp(String packageName) {
        try {
            PackageInfo existingPackageInfo = this.mContext.getPackageManager().getPackageInfo(packageName, 0);
            if (existingPackageInfo.applicationInfo != null) {
                return existingPackageInfo.applicationInfo.isSystemApp();
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean integrityCheckIncludesRuleProvider() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "verify_integrity_for_rule_provider", 0) == 1;
    }

    private List<String> getPackageListForUid(int uid) {
        try {
            return Arrays.asList(this.mContext.getPackageManager().getPackagesForUid(uid));
        } catch (NullPointerException e) {
            Slog.w(TAG, String.format("No packages were found for uid: %d", Integer.valueOf(uid)));
            return List.of();
        }
    }
}
