package com.android.server.pm.pkg.parsing;

import android.apex.ApexInfo;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.Attribution;
import android.content.pm.ComponentInfo;
import android.content.pm.ConfigurationInfo;
import android.content.pm.FallbackCategoryProvider;
import android.content.pm.FeatureGroupInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.PathPermission;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.Signature;
import android.content.pm.SigningDetails;
import android.content.pm.SigningInfo;
import android.content.pm.overlay.OverlayPaths;
import android.hardware.usb.gadget.V1_2.GadgetFunction;
import android.os.Bundle;
import android.os.Environment;
import android.os.PatternMatcher;
import android.os.UserHandle;
import com.android.internal.util.ArrayUtils;
import com.android.server.am.HostingRecord;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.pkg.PackageUserState;
import com.android.server.pm.pkg.PackageUserStateUtils;
import com.android.server.pm.pkg.SELinuxUtil;
import com.android.server.pm.pkg.component.ComponentParseUtils;
import com.android.server.pm.pkg.component.ParsedActivity;
import com.android.server.pm.pkg.component.ParsedAttribution;
import com.android.server.pm.pkg.component.ParsedComponent;
import com.android.server.pm.pkg.component.ParsedInstrumentation;
import com.android.server.pm.pkg.component.ParsedMainComponent;
import com.android.server.pm.pkg.component.ParsedPermission;
import com.android.server.pm.pkg.component.ParsedPermissionGroup;
import com.android.server.pm.pkg.component.ParsedProvider;
import com.android.server.pm.pkg.component.ParsedService;
import com.android.server.pm.pkg.component.ParsedUsesPermission;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import libcore.util.EmptyArray;
/* loaded from: classes2.dex */
public class PackageInfoWithoutStateUtils {
    public static final String SYSTEM_DATA_PATH = Environment.getDataDirectoryPath() + File.separator + HostingRecord.HOSTING_TYPE_SYSTEM;

    public static PackageInfo generate(ParsingPackageRead pkg, int[] gids, long flags, long firstInstallTime, long lastUpdateTime, Set<String> grantedPermissions, PackageUserState state, int userId) {
        return generateWithComponents(pkg, gids, flags, firstInstallTime, lastUpdateTime, grantedPermissions, state, userId, null);
    }

    public static PackageInfo generate(ParsingPackageRead pkg, ApexInfo apexInfo, int flags) {
        return generateWithComponents(pkg, EmptyArray.INT, flags, 0L, 0L, Collections.emptySet(), PackageUserState.DEFAULT, UserHandle.getCallingUserId(), apexInfo);
    }

    private static PackageInfo generateWithComponents(ParsingPackageRead pkg, int[] gids, long flags, long firstInstallTime, long lastUpdateTime, Set<String> grantedPermissions, PackageUserState state, int userId, ApexInfo apexInfo) {
        PackageInfo info;
        int N;
        int size;
        int i;
        int size2;
        int i2;
        int size3;
        int i3;
        int N2;
        int i4;
        ApplicationInfo applicationInfo = generateApplicationInfo(pkg, flags, state, userId);
        if (applicationInfo != null && (info = generateWithoutComponents(pkg, gids, flags, firstInstallTime, lastUpdateTime, grantedPermissions, state, userId, apexInfo, applicationInfo)) != null) {
            if ((1 & flags) != 0 && (N2 = pkg.getActivities().size()) > 0) {
                ActivityInfo[] res = new ActivityInfo[N2];
                int num = 0;
                int i5 = 0;
                while (i5 < N2) {
                    ParsedActivity a = pkg.getActivities().get(i5);
                    if (!ComponentParseUtils.isMatch(state, false, pkg.isEnabled(), a, flags)) {
                        i4 = i5;
                    } else if (PackageManager.APP_DETAILS_ACTIVITY_CLASS_NAME.equals(a.getName())) {
                        i4 = i5;
                    } else {
                        i4 = i5;
                        res[num] = generateActivityInfo(pkg, a, flags, state, applicationInfo, userId);
                        num++;
                    }
                    i5 = i4 + 1;
                }
                info.activities = (ActivityInfo[]) ArrayUtils.trimToSize(res, num);
            }
            if ((2 & flags) != 0 && (size3 = pkg.getReceivers().size()) > 0) {
                ActivityInfo[] res2 = new ActivityInfo[size3];
                int num2 = 0;
                int i6 = 0;
                while (i6 < size3) {
                    ParsedActivity a2 = pkg.getReceivers().get(i6);
                    if (!ComponentParseUtils.isMatch(state, false, pkg.isEnabled(), a2, flags)) {
                        i3 = i6;
                    } else {
                        i3 = i6;
                        res2[num2] = generateActivityInfo(pkg, a2, flags, state, applicationInfo, userId);
                        num2++;
                    }
                    i6 = i3 + 1;
                }
                info.receivers = (ActivityInfo[]) ArrayUtils.trimToSize(res2, num2);
            }
            if ((4 & flags) != 0 && (size2 = pkg.getServices().size()) > 0) {
                ServiceInfo[] res3 = new ServiceInfo[size2];
                int num3 = 0;
                int i7 = 0;
                while (i7 < size2) {
                    ParsedService s = pkg.getServices().get(i7);
                    if (!ComponentParseUtils.isMatch(state, false, pkg.isEnabled(), s, flags)) {
                        i2 = i7;
                    } else {
                        i2 = i7;
                        res3[num3] = generateServiceInfo(pkg, s, flags, state, applicationInfo, userId);
                        num3++;
                    }
                    i7 = i2 + 1;
                }
                info.services = (ServiceInfo[]) ArrayUtils.trimToSize(res3, num3);
            }
            if ((8 & flags) != 0 && (size = pkg.getProviders().size()) > 0) {
                ProviderInfo[] res4 = new ProviderInfo[size];
                int num4 = 0;
                int i8 = 0;
                while (i8 < size) {
                    ParsedProvider pr = pkg.getProviders().get(i8);
                    if (!ComponentParseUtils.isMatch(state, false, pkg.isEnabled(), pr, flags)) {
                        i = i8;
                    } else {
                        i = i8;
                        res4[num4] = generateProviderInfo(pkg, pr, flags, state, applicationInfo, userId);
                        num4++;
                    }
                    i8 = i + 1;
                }
                info.providers = (ProviderInfo[]) ArrayUtils.trimToSize(res4, num4);
            }
            if ((16 & flags) != 0 && (N = pkg.getInstrumentations().size()) > 0) {
                info.instrumentation = new InstrumentationInfo[N];
                for (int i9 = 0; i9 < N; i9++) {
                    info.instrumentation[i9] = generateInstrumentationInfo(pkg.getInstrumentations().get(i9), pkg, flags, userId, true);
                }
            }
            return info;
        }
        return null;
    }

    public static PackageInfo generateWithoutComponents(ParsingPackageRead pkg, int[] gids, long flags, long firstInstallTime, long lastUpdateTime, Set<String> grantedPermissions, PackageUserState state, int userId, ApexInfo apexInfo, ApplicationInfo applicationInfo) {
        if (!checkUseInstalled(pkg, state, flags)) {
            return null;
        }
        return generateWithoutComponentsUnchecked(pkg, gids, flags, firstInstallTime, lastUpdateTime, grantedPermissions, state, userId, apexInfo, applicationInfo);
    }

    public static PackageInfo generateWithoutComponentsUnchecked(ParsingPackageRead pkg, int[] gids, long flags, long firstInstallTime, long lastUpdateTime, Set<String> grantedPermissions, PackageUserState state, int userId, ApexInfo apexInfo, ApplicationInfo applicationInfo) {
        PackageInfo pi = new PackageInfo();
        pi.packageName = pkg.getPackageName();
        pi.splitNames = pkg.getSplitNames();
        pi.versionCode = ((ParsingPackageHidden) pkg).getVersionCode();
        pi.versionCodeMajor = ((ParsingPackageHidden) pkg).getVersionCodeMajor();
        pi.baseRevisionCode = pkg.getBaseRevisionCode();
        pi.splitRevisionCodes = pkg.getSplitRevisionCodes();
        pi.versionName = pkg.getVersionName();
        pi.sharedUserId = pkg.getSharedUserId();
        pi.sharedUserLabel = pkg.getSharedUserLabel();
        pi.applicationInfo = applicationInfo;
        pi.installLocation = pkg.getInstallLocation();
        if ((pi.applicationInfo.flags & 1) != 0 || (pi.applicationInfo.flags & 128) != 0) {
            pi.requiredForAllUsers = pkg.isRequiredForAllUsers();
        }
        pi.restrictedAccountType = pkg.getRestrictedAccountType();
        pi.requiredAccountType = pkg.getRequiredAccountType();
        pi.overlayTarget = pkg.getOverlayTarget();
        pi.targetOverlayableName = pkg.getOverlayTargetOverlayableName();
        pi.overlayCategory = pkg.getOverlayCategory();
        pi.overlayPriority = pkg.getOverlayPriority();
        pi.mOverlayIsStatic = pkg.isOverlayIsStatic();
        pi.compileSdkVersion = pkg.getCompileSdkVersion();
        pi.compileSdkVersionCodename = pkg.getCompileSdkVersionCodeName();
        pi.firstInstallTime = firstInstallTime;
        pi.lastUpdateTime = lastUpdateTime;
        if ((256 & flags) != 0) {
            pi.gids = gids;
        }
        if ((16384 & flags) != 0) {
            int size = pkg.getConfigPreferences().size();
            if (size > 0) {
                pi.configPreferences = new ConfigurationInfo[size];
                pkg.getConfigPreferences().toArray(pi.configPreferences);
            }
            int size2 = pkg.getRequestedFeatures().size();
            if (size2 > 0) {
                pi.reqFeatures = new FeatureInfo[size2];
                pkg.getRequestedFeatures().toArray(pi.reqFeatures);
            }
            int size3 = pkg.getFeatureGroups().size();
            if (size3 > 0) {
                pi.featureGroups = new FeatureGroupInfo[size3];
                pkg.getFeatureGroups().toArray(pi.featureGroups);
            }
        }
        if ((4096 & flags) != 0) {
            int size4 = ArrayUtils.size(pkg.getPermissions());
            if (size4 > 0) {
                pi.permissions = new PermissionInfo[size4];
                for (int i = 0; i < size4; i++) {
                    pi.permissions[i] = generatePermissionInfo(pkg.getPermissions().get(i), flags);
                }
            }
            List<ParsedUsesPermission> usesPermissions = pkg.getUsesPermissions();
            int size5 = usesPermissions.size();
            if (size5 > 0) {
                pi.requestedPermissions = new String[size5];
                pi.requestedPermissionsFlags = new int[size5];
                for (int i2 = 0; i2 < size5; i2++) {
                    ParsedUsesPermission usesPermission = usesPermissions.get(i2);
                    pi.requestedPermissions[i2] = usesPermission.getName();
                    int[] iArr = pi.requestedPermissionsFlags;
                    iArr[i2] = iArr[i2] | 1;
                    if (grantedPermissions != null && grantedPermissions.contains(usesPermission.getName())) {
                        int[] iArr2 = pi.requestedPermissionsFlags;
                        iArr2[i2] = iArr2[i2] | 2;
                    }
                    if ((usesPermission.getUsesPermissionFlags() & 65536) != 0) {
                        int[] iArr3 = pi.requestedPermissionsFlags;
                        iArr3[i2] = iArr3[i2] | 65536;
                    }
                }
            }
        }
        if (((-2147483648L) & flags) != 0) {
            int size6 = ArrayUtils.size(pkg.getAttributions());
            if (size6 > 0) {
                pi.attributions = new Attribution[size6];
                for (int i3 = 0; i3 < size6; i3++) {
                    pi.attributions[i3] = generateAttribution(pkg.getAttributions().get(i3));
                }
            }
            if (pkg.areAttributionsUserVisible()) {
                pi.applicationInfo.privateFlagsExt |= 4;
            } else {
                pi.applicationInfo.privateFlagsExt &= -5;
            }
        } else {
            pi.applicationInfo.privateFlagsExt &= -5;
        }
        if (apexInfo != null) {
            File apexFile = new File(apexInfo.modulePath);
            pi.applicationInfo.sourceDir = apexFile.getPath();
            pi.applicationInfo.publicSourceDir = apexFile.getPath();
            if (apexInfo.isFactory) {
                pi.applicationInfo.flags |= 1;
                pi.applicationInfo.flags &= -129;
            } else {
                pi.applicationInfo.flags &= -2;
                pi.applicationInfo.flags |= 128;
            }
            if (apexInfo.isActive) {
                pi.applicationInfo.flags |= 8388608;
            } else {
                pi.applicationInfo.flags &= -8388609;
            }
            pi.isApex = true;
        }
        SigningDetails signingDetails = pkg.getSigningDetails();
        if ((64 & flags) != 0) {
            if (signingDetails.hasPastSigningCertificates()) {
                pi.signatures = new Signature[1];
                pi.signatures[0] = signingDetails.getPastSigningCertificates()[0];
            } else if (signingDetails.hasSignatures()) {
                int numberOfSigs = signingDetails.getSignatures().length;
                pi.signatures = new Signature[numberOfSigs];
                System.arraycopy(signingDetails.getSignatures(), 0, pi.signatures, 0, numberOfSigs);
            }
        }
        if ((134217728 & flags) != 0) {
            if (signingDetails != SigningDetails.UNKNOWN) {
                pi.signingInfo = new SigningInfo(signingDetails);
            } else {
                pi.signingInfo = null;
            }
        }
        return pi;
    }

    public static ApplicationInfo generateApplicationInfo(ParsingPackageRead pkg, long flags, PackageUserState state, int userId) {
        if (pkg == null || !checkUseInstalled(pkg, state, flags)) {
            return null;
        }
        return generateApplicationInfoUnchecked(pkg, flags, state, userId, true);
    }

    public static ApplicationInfo generateApplicationInfoUnchecked(ParsingPackageRead pkg, long flags, PackageUserState state, int userId, boolean assignUserFields) {
        ApplicationInfo ai = ((ParsingPackageHidden) pkg).toAppInfoWithoutState();
        if (assignUserFields) {
            assignUserFields(pkg, ai, userId);
        }
        updateApplicationInfo(ai, flags, state);
        return ai;
    }

    private static void updateApplicationInfo(ApplicationInfo ai, long flags, PackageUserState state) {
        if ((128 & flags) == 0) {
            ai.metaData = null;
        }
        if ((GadgetFunction.NCM & flags) == 0) {
            ai.sharedLibraryFiles = null;
            ai.sharedLibraryInfos = null;
        }
        if (!ParsingPackageUtils.sCompatibilityModeEnabled) {
            ai.disableCompatibilityMode();
        }
        ai.flags |= flag(state.isStopped(), 2097152) | flag(state.isInstalled(), 8388608) | flag(state.isSuspended(), 1073741824);
        ai.privateFlags |= flag(state.isInstantApp(), 128) | flag(state.isVirtualPreload(), 65536) | flag(state.isHidden(), 1);
        if (state.getEnabledState() == 1) {
            ai.enabled = true;
        } else if (state.getEnabledState() == 4) {
            ai.enabled = (32768 & flags) != 0;
        } else if (state.getEnabledState() == 2 || state.getEnabledState() == 3) {
            ai.enabled = false;
        }
        ai.enabledSetting = state.getEnabledState();
        if (ai.category == -1) {
            ai.category = FallbackCategoryProvider.getFallbackCategory(ai.packageName);
        }
        ai.seInfoUser = SELinuxUtil.getSeinfoUser(state);
        OverlayPaths overlayPaths = state.getAllOverlayPaths();
        if (overlayPaths != null) {
            ai.resourceDirs = (String[]) overlayPaths.getResourceDirs().toArray(new String[0]);
            ai.overlayPaths = (String[]) overlayPaths.getOverlayPaths().toArray(new String[0]);
        }
    }

    public static ApplicationInfo generateDelegateApplicationInfo(ApplicationInfo ai, long flags, PackageUserState state, int userId) {
        if (ai == null || !checkUseInstalledOrHidden(flags, state, ai)) {
            return null;
        }
        ApplicationInfo ai2 = new ApplicationInfo(ai);
        ai2.initForUser(userId);
        ai2.icon = (!ParsingPackageUtils.sUseRoundIcon || ai2.roundIconRes == 0) ? ai2.iconRes : ai2.roundIconRes;
        updateApplicationInfo(ai2, flags, state);
        return ai2;
    }

    public static ActivityInfo generateDelegateActivityInfo(ActivityInfo a, long flags, PackageUserState state, int userId) {
        if (a == null || !checkUseInstalledOrHidden(flags, state, a.applicationInfo)) {
            return null;
        }
        ActivityInfo ai = new ActivityInfo(a);
        ai.applicationInfo = generateDelegateApplicationInfo(ai.applicationInfo, flags, state, userId);
        return ai;
    }

    public static ActivityInfo generateActivityInfo(ParsingPackageRead pkg, ParsedActivity a, long flags, PackageUserState state, ApplicationInfo applicationInfo, int userId) {
        if (a == null || !checkUseInstalled(pkg, state, flags)) {
            return null;
        }
        if (applicationInfo == null) {
            applicationInfo = generateApplicationInfo(pkg, flags, state, userId);
        }
        if (applicationInfo == null) {
            return null;
        }
        return generateActivityInfoUnchecked(a, flags, applicationInfo);
    }

    public static ActivityInfo generateActivityInfoUnchecked(ParsedActivity a, long flags, ApplicationInfo applicationInfo) {
        ActivityInfo ai = new ActivityInfo();
        assignSharedFieldsForComponentInfo(ai, a);
        ai.targetActivity = a.getTargetActivity();
        ai.processName = a.getProcessName();
        ai.exported = a.isExported();
        ai.theme = a.getTheme();
        ai.uiOptions = a.getUiOptions();
        ai.parentActivityName = a.getParentActivityName();
        ai.permission = a.getPermission();
        ai.taskAffinity = a.getTaskAffinity();
        ai.flags = a.getFlags();
        ai.privateFlags = a.getPrivateFlags();
        ai.launchMode = a.getLaunchMode();
        ai.documentLaunchMode = a.getDocumentLaunchMode();
        ai.maxRecents = a.getMaxRecents();
        ai.configChanges = a.getConfigChanges();
        ai.softInputMode = a.getSoftInputMode();
        ai.persistableMode = a.getPersistableMode();
        ai.lockTaskLaunchMode = a.getLockTaskLaunchMode();
        ai.screenOrientation = a.getScreenOrientation();
        ai.resizeMode = a.getResizeMode();
        ai.setMaxAspectRatio(a.getMaxAspectRatio());
        ai.setMinAspectRatio(a.getMinAspectRatio());
        ai.supportsSizeChanges = a.isSupportsSizeChanges();
        ai.requestedVrComponent = a.getRequestedVrComponent();
        ai.rotationAnimation = a.getRotationAnimation();
        ai.colorMode = a.getColorMode();
        ai.windowLayout = a.getWindowLayout();
        ai.attributionTags = a.getAttributionTags();
        if ((128 & flags) != 0) {
            Bundle metaData = a.getMetaData();
            ai.metaData = metaData.isEmpty() ? null : metaData;
        }
        ai.applicationInfo = applicationInfo;
        ai.setKnownActivityEmbeddingCerts(a.getKnownActivityEmbeddingCerts());
        return ai;
    }

    public static ActivityInfo generateActivityInfo(ParsingPackageRead pkg, ParsedActivity a, long flags, PackageUserState state, int userId) {
        return generateActivityInfo(pkg, a, flags, state, null, userId);
    }

    public static ServiceInfo generateServiceInfo(ParsingPackageRead pkg, ParsedService s, long flags, PackageUserState state, ApplicationInfo applicationInfo, int userId) {
        if (s == null || !checkUseInstalled(pkg, state, flags)) {
            return null;
        }
        if (applicationInfo == null) {
            applicationInfo = generateApplicationInfo(pkg, flags, state, userId);
        }
        if (applicationInfo == null) {
            return null;
        }
        return generateServiceInfoUnchecked(s, flags, applicationInfo);
    }

    public static ServiceInfo generateServiceInfoUnchecked(ParsedService s, long flags, ApplicationInfo applicationInfo) {
        ServiceInfo si = new ServiceInfo();
        assignSharedFieldsForComponentInfo(si, s);
        si.exported = s.isExported();
        si.flags = s.getFlags();
        si.permission = s.getPermission();
        si.processName = s.getProcessName();
        si.mForegroundServiceType = s.getForegroundServiceType();
        si.applicationInfo = applicationInfo;
        if ((128 & flags) != 0) {
            Bundle metaData = s.getMetaData();
            si.metaData = metaData.isEmpty() ? null : metaData;
        }
        return si;
    }

    public static ServiceInfo generateServiceInfo(ParsingPackageRead pkg, ParsedService s, long flags, PackageUserState state, int userId) {
        return generateServiceInfo(pkg, s, flags, state, null, userId);
    }

    public static ProviderInfo generateProviderInfo(ParsingPackageRead pkg, ParsedProvider p, long flags, PackageUserState state, ApplicationInfo applicationInfo, int userId) {
        if (p == null || !checkUseInstalled(pkg, state, flags)) {
            return null;
        }
        if (applicationInfo == null) {
            applicationInfo = generateApplicationInfo(pkg, flags, state, userId);
        }
        if (applicationInfo == null) {
            return null;
        }
        return generateProviderInfoUnchecked(p, flags, applicationInfo);
    }

    public static ProviderInfo generateProviderInfoUnchecked(ParsedProvider p, long flags, ApplicationInfo applicationInfo) {
        ProviderInfo pi = new ProviderInfo();
        assignSharedFieldsForComponentInfo(pi, p);
        pi.exported = p.isExported();
        pi.flags = p.getFlags();
        pi.processName = p.getProcessName();
        pi.authority = p.getAuthority();
        pi.isSyncable = p.isSyncable();
        pi.readPermission = p.getReadPermission();
        pi.writePermission = p.getWritePermission();
        pi.grantUriPermissions = p.isGrantUriPermissions();
        pi.forceUriPermissions = p.isForceUriPermissions();
        pi.multiprocess = p.isMultiProcess();
        pi.initOrder = p.getInitOrder();
        pi.uriPermissionPatterns = (PatternMatcher[]) p.getUriPermissionPatterns().toArray(new PatternMatcher[0]);
        pi.pathPermissions = (PathPermission[]) p.getPathPermissions().toArray(new PathPermission[0]);
        if ((2048 & flags) == 0) {
            pi.uriPermissionPatterns = null;
        }
        if ((128 & flags) != 0) {
            Bundle metaData = p.getMetaData();
            pi.metaData = metaData.isEmpty() ? null : metaData;
        }
        pi.applicationInfo = applicationInfo;
        return pi;
    }

    public static ProviderInfo generateProviderInfo(ParsingPackageRead pkg, ParsedProvider p, long flags, PackageUserState state, int userId) {
        return generateProviderInfo(pkg, p, flags, state, null, userId);
    }

    public static InstrumentationInfo generateInstrumentationInfo(ParsedInstrumentation i, ParsingPackageRead pkg, long flags, int userId, boolean assignUserFields) {
        if (i == null) {
            return null;
        }
        InstrumentationInfo ii = new InstrumentationInfo();
        assignSharedFieldsForPackageItemInfo(ii, i);
        ii.targetPackage = i.getTargetPackage();
        ii.targetProcesses = i.getTargetProcesses();
        ii.handleProfiling = i.isHandleProfiling();
        ii.functionalTest = i.isFunctionalTest();
        ii.sourceDir = pkg.getBaseApkPath();
        ii.publicSourceDir = pkg.getBaseApkPath();
        ii.splitNames = pkg.getSplitNames();
        ii.splitSourceDirs = pkg.getSplitCodePaths().length == 0 ? null : pkg.getSplitCodePaths();
        ii.splitPublicSourceDirs = pkg.getSplitCodePaths().length == 0 ? null : pkg.getSplitCodePaths();
        ii.splitDependencies = pkg.getSplitDependencies().size() == 0 ? null : pkg.getSplitDependencies();
        if (assignUserFields) {
            assignUserFields(pkg, ii, userId);
        }
        if ((128 & flags) == 0) {
            return ii;
        }
        Bundle metaData = i.getMetaData();
        ii.metaData = metaData.isEmpty() ? null : metaData;
        return ii;
    }

    public static PermissionInfo generatePermissionInfo(ParsedPermission p, long flags) {
        if (p == null) {
            return null;
        }
        PermissionInfo pi = new PermissionInfo(p.getBackgroundPermission());
        assignSharedFieldsForPackageItemInfo(pi, p);
        pi.group = p.getGroup();
        pi.requestRes = p.getRequestRes();
        pi.protectionLevel = p.getProtectionLevel();
        pi.descriptionRes = p.getDescriptionRes();
        pi.flags = p.getFlags();
        pi.knownCerts = p.getKnownCerts();
        if ((128 & flags) == 0) {
            return pi;
        }
        Bundle metaData = p.getMetaData();
        pi.metaData = metaData.isEmpty() ? null : metaData;
        return pi;
    }

    public static PermissionGroupInfo generatePermissionGroupInfo(ParsedPermissionGroup pg, long flags) {
        if (pg == null) {
            return null;
        }
        PermissionGroupInfo pgi = new PermissionGroupInfo(pg.getRequestDetailRes(), pg.getBackgroundRequestRes(), pg.getBackgroundRequestDetailRes());
        assignSharedFieldsForPackageItemInfo(pgi, pg);
        pgi.descriptionRes = pg.getDescriptionRes();
        pgi.priority = pg.getPriority();
        pgi.requestRes = pg.getRequestRes();
        pgi.flags = pg.getFlags();
        if ((128 & flags) == 0) {
            return pgi;
        }
        Bundle metaData = pg.getMetaData();
        pgi.metaData = metaData.isEmpty() ? null : metaData;
        return pgi;
    }

    public static Attribution generateAttribution(ParsedAttribution pa) {
        if (pa == null) {
            return null;
        }
        return new Attribution(pa.getTag(), pa.getLabel());
    }

    private static boolean checkUseInstalledOrHidden(long flags, PackageUserState state, ApplicationInfo appInfo) {
        if ((flags & 536870912) != 0 || state.isInstalled() || appInfo == null || !appInfo.hiddenUntilInstalled) {
            if (!PackageUserStateUtils.isAvailable(state, flags)) {
                if (appInfo == null || !appInfo.isSystemApp()) {
                    return false;
                }
                if ((4202496 & flags) == 0 && (536870912 & flags) == 0) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static void assignSharedFieldsForComponentInfo(ComponentInfo componentInfo, ParsedMainComponent mainComponent) {
        assignSharedFieldsForPackageItemInfo(componentInfo, mainComponent);
        componentInfo.descriptionRes = mainComponent.getDescriptionRes();
        componentInfo.directBootAware = mainComponent.isDirectBootAware();
        componentInfo.enabled = mainComponent.isEnabled();
        componentInfo.splitName = mainComponent.getSplitName();
        componentInfo.attributionTags = mainComponent.getAttributionTags();
    }

    private static void assignSharedFieldsForPackageItemInfo(PackageItemInfo packageItemInfo, ParsedComponent component) {
        packageItemInfo.nonLocalizedLabel = ComponentParseUtils.getNonLocalizedLabel(component);
        packageItemInfo.icon = ComponentParseUtils.getIcon(component);
        packageItemInfo.banner = component.getBanner();
        packageItemInfo.labelRes = component.getLabelRes();
        packageItemInfo.logo = component.getLogo();
        packageItemInfo.name = component.getName();
        packageItemInfo.packageName = component.getPackageName();
    }

    private static int flag(boolean hasFlag, int flag) {
        if (hasFlag) {
            return flag;
        }
        return 0;
    }

    public static int appInfoFlags(ParsingPackageRead pkg) {
        return flag(pkg.isExternalStorage(), 262144) | flag(pkg.isBaseHardwareAccelerated(), 536870912) | flag(pkg.isAllowBackup(), 32768) | flag(pkg.isKillAfterRestore(), 65536) | flag(pkg.isRestoreAnyVersion(), 131072) | flag(pkg.isFullBackupOnly(), 67108864) | flag(pkg.isPersistent(), 8) | flag(pkg.isDebuggable(), 2) | flag(pkg.isVmSafeMode(), 16384) | flag(pkg.isHasCode(), 4) | flag(pkg.isAllowTaskReparenting(), 32) | flag(pkg.isAllowClearUserData(), 64) | flag(pkg.isLargeHeap(), 1048576) | flag(pkg.isUsesCleartextTraffic(), 134217728) | flag(pkg.isSupportsRtl(), 4194304) | flag(pkg.isTestOnly(), 256) | flag(pkg.isMultiArch(), Integer.MIN_VALUE) | flag(pkg.isExtractNativeLibs(), 268435456) | flag(pkg.isGame(), 33554432) | flag(pkg.isSupportsSmallScreens(), 512) | flag(pkg.isSupportsNormalScreens(), 1024) | flag(pkg.isSupportsLargeScreens(), 2048) | flag(pkg.isSupportsExtraLargeScreens(), 524288) | flag(pkg.isResizeable(), 4096) | flag(pkg.isAnyDensity(), 8192);
    }

    public static int appInfoPrivateFlags(ParsingPackageRead pkg) {
        int privateFlags = flag(pkg.isStaticSharedLibrary(), 16384) | flag(pkg.isOverlay(), 268435456) | flag(pkg.isIsolatedSplitLoading(), 32768) | flag(pkg.isHasDomainUrls(), 16) | flag(pkg.isProfileableByShell(), 8388608) | flag(pkg.isBackupInForeground(), 8192) | flag(pkg.isUseEmbeddedDex(), 33554432) | flag(pkg.isDefaultToDeviceProtectedStorage(), 32) | flag(pkg.isDirectBootAware(), 64) | flag(pkg.isPartiallyDirectBootAware(), 256) | flag(pkg.isAllowClearUserDataOnFailedRestore(), 67108864) | flag(pkg.isAllowAudioPlaybackCapture(), 134217728) | flag(pkg.isRequestLegacyExternalStorage(), 536870912) | flag(pkg.isUsesNonSdkApi(), 4194304) | flag(pkg.isHasFragileUserData(), 16777216) | flag(pkg.isCantSaveState(), 2) | flag(pkg.isResizeableActivityViaSdkVersion(), 4096) | flag(pkg.isAllowNativeHeapPointerTagging(), Integer.MIN_VALUE);
        Boolean resizeableActivity = pkg.getResizeableActivity();
        if (resizeableActivity != null) {
            if (resizeableActivity.booleanValue()) {
                return privateFlags | 1024;
            }
            return privateFlags | 2048;
        }
        return privateFlags;
    }

    public static int appInfoPrivateFlagsExt(ParsingPackageRead pkg) {
        int privateFlagsExt = flag(pkg.isProfileable(), 1) | flag(pkg.hasRequestForegroundServiceExemption(), 2) | flag(pkg.areAttributionsUserVisible(), 4) | flag(pkg.isOnBackInvokedCallbackEnabled(), 8);
        return privateFlagsExt;
    }

    private static boolean checkUseInstalled(ParsingPackageRead pkg, PackageUserState state, long flags) {
        return PackageUserStateUtils.isAvailable(state, flags);
    }

    public static File getDataDir(ParsingPackageRead pkg, int userId) {
        if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(pkg.getPackageName())) {
            return Environment.getDataSystemDirectory();
        }
        if (pkg.isDefaultToDeviceProtectedStorage()) {
            return getDeviceProtectedDataDir(pkg, userId);
        }
        return getCredentialProtectedDataDir(pkg, userId);
    }

    public static File getDeviceProtectedDataDir(ParsingPackageRead pkg, int userId) {
        return Environment.getDataUserDePackageDirectory(pkg.getVolumeUuid(), userId, pkg.getPackageName());
    }

    public static File getCredentialProtectedDataDir(ParsingPackageRead pkg, int userId) {
        return Environment.getDataUserCePackageDirectory(pkg.getVolumeUuid(), userId, pkg.getPackageName());
    }

    private static void assignUserFields(ParsingPackageRead pkg, ApplicationInfo info, int userId) {
        info.uid = UserHandle.getUid(userId, UserHandle.getAppId(info.uid));
        String pkgName = pkg.getPackageName();
        if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(pkgName)) {
            info.dataDir = SYSTEM_DATA_PATH;
            return;
        }
        String baseDataDirPrefix = Environment.getDataDirectoryPath(pkg.getVolumeUuid()) + File.separator;
        String userIdPkgSuffix = File.separator + userId + File.separator + pkgName;
        info.credentialProtectedDataDir = baseDataDirPrefix + "user" + userIdPkgSuffix;
        info.deviceProtectedDataDir = baseDataDirPrefix + "user_de" + userIdPkgSuffix;
        if (pkg.isDefaultToDeviceProtectedStorage()) {
            info.dataDir = info.deviceProtectedDataDir;
        } else {
            info.dataDir = info.credentialProtectedDataDir;
        }
    }

    private static void assignUserFields(ParsingPackageRead pkg, InstrumentationInfo info, int userId) {
        String pkgName = pkg.getPackageName();
        if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(pkgName)) {
            info.dataDir = SYSTEM_DATA_PATH;
            return;
        }
        String baseDataDirPrefix = Environment.getDataDirectoryPath(pkg.getVolumeUuid()) + File.separator;
        String userIdPkgSuffix = File.separator + userId + File.separator + pkgName;
        info.credentialProtectedDataDir = baseDataDirPrefix + "user" + userIdPkgSuffix;
        info.deviceProtectedDataDir = baseDataDirPrefix + "user_de" + userIdPkgSuffix;
        if (pkg.isDefaultToDeviceProtectedStorage()) {
            info.dataDir = info.deviceProtectedDataDir;
        } else {
            info.dataDir = info.credentialProtectedDataDir;
        }
    }
}
