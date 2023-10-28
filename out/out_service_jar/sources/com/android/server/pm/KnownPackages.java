package com.android.server.pm;

import android.text.TextUtils;
import com.android.internal.util.ArrayUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/* loaded from: classes2.dex */
public final class KnownPackages {
    public static final int LAST_KNOWN_PACKAGE = 18;
    public static final int PACKAGE_AMBIENT_CONTEXT_DETECTION = 18;
    public static final int PACKAGE_APP_PREDICTOR = 12;
    public static final int PACKAGE_BROWSER = 5;
    public static final int PACKAGE_COMPANION = 15;
    public static final int PACKAGE_CONFIGURATOR = 10;
    public static final int PACKAGE_DOCUMENTER = 9;
    public static final int PACKAGE_INCIDENT_REPORT_APPROVER = 11;
    public static final int PACKAGE_INSTALLER = 2;
    public static final int PACKAGE_OVERLAY_CONFIG_SIGNATURE = 13;
    public static final int PACKAGE_PERMISSION_CONTROLLER = 7;
    public static final int PACKAGE_RECENTS = 17;
    public static final int PACKAGE_RETAIL_DEMO = 16;
    public static final int PACKAGE_SETUP_WIZARD = 1;
    public static final int PACKAGE_SYSTEM = 0;
    public static final int PACKAGE_SYSTEM_TEXT_CLASSIFIER = 6;
    public static final int PACKAGE_UNINSTALLER = 3;
    public static final int PACKAGE_VERIFIER = 4;
    public static final int PACKAGE_WELLBEING = 8;
    public static final int PACKAGE_WIFI = 14;
    private final String mAmbientContextDetectionPackage;
    private final String mAppPredictionServicePackage;
    private final String mCompanionPackage;
    private final String mConfiguratorPackage;
    private final DefaultAppProvider mDefaultAppProvider;
    private final String mDefaultTextClassifierPackage;
    private final String mIncidentReportApproverPackage;
    private final String mOverlayConfigSignaturePackage;
    private final String mRecentsPackage;
    private final String mRequiredInstallerPackage;
    private final String mRequiredPermissionControllerPackage;
    private final String mRequiredUninstallerPackage;
    private final String mRequiredVerifierPackage;
    private final String mRetailDemoPackage;
    private final String mSetupWizardPackage;
    private final String mSystemTextClassifierPackageName;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface KnownPackage {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public KnownPackages(DefaultAppProvider defaultAppProvider, String requiredInstallerPackage, String requiredUninstallerPackage, String setupWizardPackage, String requiredVerifierPackage, String defaultTextClassifierPackage, String systemTextClassifierPackageName, String requiredPermissionControllerPackage, String configuratorPackage, String incidentReportApproverPackage, String ambientContextDetectionPackage, String appPredictionServicePackage, String companionPackageName, String retailDemoPackage, String overlayConfigSignaturePackage, String recentsPackage) {
        this.mDefaultAppProvider = defaultAppProvider;
        this.mRequiredInstallerPackage = requiredInstallerPackage;
        this.mRequiredUninstallerPackage = requiredUninstallerPackage;
        this.mSetupWizardPackage = setupWizardPackage;
        this.mRequiredVerifierPackage = requiredVerifierPackage;
        this.mDefaultTextClassifierPackage = defaultTextClassifierPackage;
        this.mSystemTextClassifierPackageName = systemTextClassifierPackageName;
        this.mRequiredPermissionControllerPackage = requiredPermissionControllerPackage;
        this.mConfiguratorPackage = configuratorPackage;
        this.mIncidentReportApproverPackage = incidentReportApproverPackage;
        this.mAmbientContextDetectionPackage = ambientContextDetectionPackage;
        this.mAppPredictionServicePackage = appPredictionServicePackage;
        this.mCompanionPackage = companionPackageName;
        this.mRetailDemoPackage = retailDemoPackage;
        this.mOverlayConfigSignaturePackage = overlayConfigSignaturePackage;
        this.mRecentsPackage = recentsPackage;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String knownPackageToString(int knownPackage) {
        switch (knownPackage) {
            case 0:
                return "System";
            case 1:
                return "Setup Wizard";
            case 2:
                return "Installer";
            case 3:
                return "Uninstaller";
            case 4:
                return "Verifier";
            case 5:
                return "Browser";
            case 6:
                return "System Text Classifier";
            case 7:
                return "Permission Controller";
            case 8:
                return "Wellbeing";
            case 9:
                return "Documenter";
            case 10:
                return "Configurator";
            case 11:
                return "Incident Report Approver";
            case 12:
                return "App Predictor";
            case 13:
                return "Overlay Config Signature";
            case 14:
                return "Wi-Fi";
            case 15:
                return "Companion";
            case 16:
                return "Retail Demo";
            case 17:
                return "Recents";
            case 18:
                return "Ambient Context Detection";
            default:
                return "Unknown";
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String[] getKnownPackageNames(Computer snapshot, int knownPackage, int userId) {
        switch (knownPackage) {
            case 0:
                return new String[]{PackageManagerService.PLATFORM_PACKAGE_NAME};
            case 1:
                return snapshot.filterOnlySystemPackages(this.mSetupWizardPackage);
            case 2:
                return snapshot.filterOnlySystemPackages(this.mRequiredInstallerPackage);
            case 3:
                return snapshot.filterOnlySystemPackages(this.mRequiredUninstallerPackage);
            case 4:
                return snapshot.filterOnlySystemPackages(this.mRequiredVerifierPackage);
            case 5:
                return new String[]{this.mDefaultAppProvider.getDefaultBrowser(userId)};
            case 6:
                return snapshot.filterOnlySystemPackages(this.mDefaultTextClassifierPackage, this.mSystemTextClassifierPackageName);
            case 7:
                return snapshot.filterOnlySystemPackages(this.mRequiredPermissionControllerPackage);
            case 8:
            case 9:
            case 14:
            default:
                return (String[]) ArrayUtils.emptyArray(String.class);
            case 10:
                return snapshot.filterOnlySystemPackages(this.mConfiguratorPackage);
            case 11:
                return snapshot.filterOnlySystemPackages(this.mIncidentReportApproverPackage);
            case 12:
                return snapshot.filterOnlySystemPackages(this.mAppPredictionServicePackage);
            case 13:
                return snapshot.filterOnlySystemPackages(this.mOverlayConfigSignaturePackage);
            case 15:
                return snapshot.filterOnlySystemPackages(this.mCompanionPackage);
            case 16:
                return TextUtils.isEmpty(this.mRetailDemoPackage) ? (String[]) ArrayUtils.emptyArray(String.class) : new String[]{this.mRetailDemoPackage};
            case 17:
                return snapshot.filterOnlySystemPackages(this.mRecentsPackage);
            case 18:
                return snapshot.filterOnlySystemPackages(this.mAmbientContextDetectionPackage);
        }
    }
}
