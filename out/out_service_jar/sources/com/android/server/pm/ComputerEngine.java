package com.android.server.pm;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.PermissionChecker;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.AuxiliaryResolveInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.InstallSourceInfo;
import android.content.pm.InstantAppRequest;
import android.content.pm.InstantAppResolveInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.KeySet;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.ProcessInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.SharedLibraryInfo;
import android.content.pm.Signature;
import android.content.pm.SigningDetails;
import android.content.pm.SigningInfo;
import android.content.pm.UserInfo;
import android.content.pm.VersionedPackage;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelableException;
import android.os.Process;
import android.os.Trace;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.LogPrinter;
import android.util.LongSparseLongArray;
import android.util.MathUtils;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import android.util.proto.ProtoOutputStream;
import com.android.internal.app.IntentForwarderActivity;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.internal.util.function.QuadFunction;
import com.android.server.am.HostingRecord;
import com.android.server.pm.CompilerStats;
import com.android.server.pm.ComputerEngine;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.dex.DexManager;
import com.android.server.pm.dex.PackageDexUsage;
import com.android.server.pm.parsing.PackageInfoUtils;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.AndroidPackageUtils;
import com.android.server.pm.permission.PermissionManagerServiceInternal;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.PackageStateUtils;
import com.android.server.pm.pkg.PackageUserStateInternal;
import com.android.server.pm.pkg.PackageUserStateUtils;
import com.android.server.pm.pkg.SharedUserApi;
import com.android.server.pm.pkg.component.ParsedActivity;
import com.android.server.pm.pkg.component.ParsedInstrumentation;
import com.android.server.pm.pkg.component.ParsedIntentInfo;
import com.android.server.pm.pkg.component.ParsedMainComponent;
import com.android.server.pm.pkg.component.ParsedProvider;
import com.android.server.pm.pkg.component.ParsedService;
import com.android.server.pm.pkg.parsing.PackageInfoWithoutStateUtils;
import com.android.server.pm.resolution.ComponentResolver;
import com.android.server.pm.resolution.ComponentResolverApi;
import com.android.server.pm.snapshot.PackageDataSnapshot;
import com.android.server.pm.verify.domain.DomainVerificationManagerInternal;
import com.android.server.pm.verify.domain.DomainVerificationUtils;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.uri.UriGrantsManagerInternal;
import com.android.server.utils.WatchedArrayMap;
import com.android.server.utils.WatchedLongSparseArray;
import com.android.server.utils.WatchedSparseBooleanArray;
import com.android.server.utils.WatchedSparseIntArray;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.transsion.hubcore.server.pm.ITranPackageManagerService;
import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import libcore.util.EmptyArray;
/* loaded from: classes2.dex */
public class ComputerEngine implements Computer {
    private static final Comparator<ProviderInfo> sProviderInitOrderSorter;
    private final ApexManager mApexManager;
    private final String mAppPredictionServicePackage;
    private final AppsFilterSnapshot mAppsFilter;
    private final BackgroundDexOptService mBackgroundDexOptService;
    private final CompilerStats mCompilerStats;
    private final ComponentResolverApi mComponentResolver;
    private final Context mContext;
    private final DefaultAppProvider mDefaultAppProvider;
    private final DexManager mDexManager;
    private final DomainVerificationManagerInternal mDomainVerificationManager;
    private final PackageManagerInternal.ExternalSourcesPolicy mExternalSourcesPolicy;
    private final WatchedArrayMap<String, Integer> mFrozenPackages;
    private final PackageManagerServiceInjector mInjector;
    private final ResolveInfo mInstantAppInstallerInfo;
    private final InstantAppRegistry mInstantAppRegistry;
    private final InstantAppResolverConnection mInstantAppResolverConnection;
    private final WatchedArrayMap<ComponentName, ParsedInstrumentation> mInstrumentation;
    private final WatchedSparseIntArray mIsolatedOwners;
    private final ApplicationInfo mLocalAndroidApplication;
    private final ActivityInfo mLocalInstantAppInstallerActivity;
    private final ComponentName mLocalResolveComponentName;
    private final PackageDexOptimizer mPackageDexOptimizer;
    private final WatchedArrayMap<String, AndroidPackage> mPackages;
    private final PermissionManagerServiceInternal mPermissionManager;
    private final ActivityInfo mResolveActivity;
    protected final PackageManagerService mService;
    protected final Settings mSettings;
    private final SharedLibrariesRead mSharedLibraries;
    private int mUsed = 0;
    private final UserManagerService mUserManager;
    private final int mVersion;
    private final WatchedSparseBooleanArray mWebInstantAppsDisabled;
    private static ArrayList<String> sCrossDualprofileIntent = new ArrayList<>();
    private static ArrayList<String> sDualprofileDataSchemeWhiteList = new ArrayList<>();
    private static ArrayList<String> sDualprofileAddUserAuthorityBlackList = new ArrayList<>();

    static {
        sCrossDualprofileIntent.add("android.intent.action.MAIN");
        sCrossDualprofileIntent.add("android.intent.action.VIEW");
        sCrossDualprofileIntent.add("android.intent.action.SEND");
        sCrossDualprofileIntent.add("android.intent.action.SENDTO");
        sCrossDualprofileIntent.add("android.intent.action.DIAL");
        sCrossDualprofileIntent.add("android.intent.action.PICK");
        sCrossDualprofileIntent.add("android.intent.action.INSERT");
        sCrossDualprofileIntent.add("android.intent.action.GET_CONTENT");
        sCrossDualprofileIntent.add("android.media.action.IMAGE_CAPTURE");
        sCrossDualprofileIntent.add("android.media.action.IMAGE_CAPTURE_SECURE");
        sCrossDualprofileIntent.add("android.media.action.VIDEO_CAPTURE");
        sCrossDualprofileIntent.add("android.provider.MediaStore.RECORD_SOUND");
        sCrossDualprofileIntent.add("android.media.action.STILL_IMAGE_CAMERA");
        sCrossDualprofileIntent.add("android.media.action.STILL_IMAGE_CAMERA_SECURE");
        sCrossDualprofileIntent.add("android.media.action.VIDEO_CAMERA");
        sCrossDualprofileIntent.add("android.intent.action.SET_ALARM");
        sCrossDualprofileIntent.add("android.intent.action.SHOW_ALARMS");
        sCrossDualprofileIntent.add("android.intent.action.SET_TIMER");
        sCrossDualprofileIntent.add("android.settings.DATA_ROAMING_SETTINGS");
        sCrossDualprofileIntent.add("android.settings.NETWORK_OPERATOR_SETTINGS");
        sCrossDualprofileIntent.add("android.speech.action.RECOGNIZE_SPEECH");
        sCrossDualprofileIntent.add("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        sCrossDualprofileIntent.add("android.hardware.usb.action.USB_ACCESSORY_ATTACHED");
        sCrossDualprofileIntent.add("android.intent.action.OPEN_DOCUMENT");
        sDualprofileDataSchemeWhiteList.add(ActivityTaskManagerInternal.ASSIST_KEY_CONTENT);
        sDualprofileDataSchemeWhiteList.add("http");
        sDualprofileDataSchemeWhiteList.add("https");
        sDualprofileDataSchemeWhiteList.add("file");
        sDualprofileDataSchemeWhiteList.add("ftp");
        sDualprofileDataSchemeWhiteList.add("ed2k");
        sDualprofileDataSchemeWhiteList.add("geo");
        sDualprofileDataSchemeWhiteList.add("sms");
        sDualprofileDataSchemeWhiteList.add("smsto");
        sDualprofileDataSchemeWhiteList.add("mms");
        sDualprofileDataSchemeWhiteList.add("mmsto");
        sDualprofileAddUserAuthorityBlackList.add("com.android.contacts");
        sProviderInitOrderSorter = new Comparator() { // from class: com.android.server.pm.ComputerEngine$$ExternalSyntheticLambda0
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return ComputerEngine.lambda$static$0((ProviderInfo) obj, (ProviderInfo) obj2);
            }
        };
    }

    private int resolveUserId(Intent intent, String resolvedType, int userId, int filterCallingUid) {
        long callingId = Binder.clearCallingIdentity();
        try {
            boolean isDualProfile = this.mUserManager.isDualProfile(userId);
            if (isDualProfile && intent.getComponent() == null && sCrossDualprofileIntent.contains(intent.getAction())) {
                if (!"android.intent.action.VIEW".equals(intent.getAction()) || intent.getData() == null || intent.getData().getScheme() == null || sDualprofileDataSchemeWhiteList.contains(intent.getData().getScheme())) {
                    if (!"android.intent.action.MAIN".equals(intent.getAction()) || intent.hasCategory("android.intent.category.HOME")) {
                        if ("android.intent.action.VIEW".equals(intent.getAction()) && "application/vnd.android.package-archive".equals(resolvedType)) {
                            String[] uidPackages = getPackagesForUid(filterCallingUid);
                            if (uidPackages != null && uidPackages.length > 0) {
                                int originUid = getPackageUidInternal(uidPackages[0], 0L, 0, Binder.getCallingUid());
                                Slog.w("PackageManager", "install for dualprofile by " + uidPackages[0] + " ,originUid " + originUid);
                                intent.putExtra("android.intent.extra.ORIGINATING_UID", originUid);
                            } else {
                                Slog.w("PackageManager", "can not find uid package for uid: " + filterCallingUid);
                            }
                        }
                        if (intent.getData() != null && !sDualprofileAddUserAuthorityBlackList.contains(intent.getData().getAuthority())) {
                            intent.prepareToLeaveUser(userId);
                        }
                        try {
                            if (new Intent(intent).hasExtra("transsion.intent.extra.USER_ID")) {
                                return 0;
                            }
                            intent.putExtra("transsion.intent.extra.USER_ID", userId);
                            return 0;
                        } catch (Exception e) {
                            Slog.w("PackageManager", "Private intent: ", e);
                            return 0;
                        }
                    }
                    return userId;
                }
                return userId;
            }
            return userId;
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes2.dex */
    public class Settings {
        private final com.android.server.pm.Settings mSettings;

        public ArrayMap<String, ? extends PackageStateInternal> getPackages() {
            return this.mSettings.getPackagesLocked().untrackedStorage();
        }

        public Settings(com.android.server.pm.Settings settings) {
            this.mSettings = settings;
        }

        public PackageStateInternal getPackage(String packageName) {
            return this.mSettings.getPackageLPr(packageName);
        }

        public PackageStateInternal getDisabledSystemPkg(String packageName) {
            return this.mSettings.getDisabledSystemPkgLPr(packageName);
        }

        public boolean isEnabledAndMatch(ComponentInfo componentInfo, int flags, int userId) {
            PackageStateInternal pkgState = getPackage(componentInfo.packageName);
            if (pkgState == null) {
                return false;
            }
            return PackageUserStateUtils.isMatch(pkgState.getUserStateOrDefault(userId), componentInfo, flags);
        }

        public boolean isEnabledAndMatch(AndroidPackage pkg, ParsedMainComponent component, long flags, int userId) {
            PackageStateInternal pkgState = getPackage(component.getPackageName());
            if (pkgState == null) {
                return false;
            }
            return PackageUserStateUtils.isMatch(pkgState.getUserStateOrDefault(userId), pkg.isSystem(), pkg.isEnabled(), component, flags);
        }

        public CrossProfileIntentResolver getCrossProfileIntentResolver(int userId) {
            return this.mSettings.getCrossProfileIntentResolver(userId);
        }

        public SettingBase getSettingBase(int appId) {
            return this.mSettings.getSettingLPr(appId);
        }

        public String getRenamedPackageLPr(String packageName) {
            return this.mSettings.getRenamedPackageLPr(packageName);
        }

        public PersistentPreferredIntentResolver getPersistentPreferredActivities(int userId) {
            return this.mSettings.getPersistentPreferredActivities(userId);
        }

        public void dumpVersionLPr(IndentingPrintWriter indentingPrintWriter) {
            this.mSettings.dumpVersionLPr(indentingPrintWriter);
        }

        public void dumpPreferred(PrintWriter pw, DumpState dumpState, String packageName) {
            this.mSettings.dumpPreferred(pw, dumpState, packageName);
        }

        public void writePreferredActivitiesLPr(TypedXmlSerializer serializer, int userId, boolean full) throws IllegalArgumentException, IllegalStateException, IOException {
            this.mSettings.writePreferredActivitiesLPr(serializer, userId, full);
        }

        public PreferredIntentResolver getPreferredActivities(int userId) {
            return this.mSettings.getPreferredActivities(userId);
        }

        public SharedUserSetting getSharedUserFromId(String name) {
            try {
                return this.mSettings.getSharedUserLPw(name, 0, 0, false);
            } catch (PackageManagerException ignored) {
                throw new RuntimeException(ignored);
            }
        }

        public boolean getBlockUninstall(int userId, String packageName) {
            return this.mSettings.getBlockUninstallLPr(userId, packageName);
        }

        public int getApplicationEnabledSetting(String packageName, int userId) throws PackageManager.NameNotFoundException {
            return this.mSettings.getApplicationEnabledSettingLPr(packageName, userId);
        }

        public int getComponentEnabledSetting(ComponentName component, int userId) throws PackageManager.NameNotFoundException {
            return this.mSettings.getComponentEnabledSettingLPr(component, userId);
        }

        public KeySetManagerService getKeySetManagerService() {
            return this.mSettings.getKeySetManagerService();
        }

        public Collection<SharedUserSetting> getAllSharedUsers() {
            return this.mSettings.getAllSharedUsersLPw();
        }

        public SharedUserApi getSharedUserFromPackageName(String packageName) {
            return this.mSettings.getSharedUserSettingLPr(packageName);
        }

        public SharedUserApi getSharedUserFromAppId(int sharedUserAppId) {
            return (SharedUserSetting) this.mSettings.getSettingLPr(sharedUserAppId);
        }

        public ArraySet<PackageStateInternal> getSharedUserPackages(int sharedUserAppId) {
            ArraySet<PackageStateInternal> res = new ArraySet<>();
            SharedUserSetting sharedUserSetting = (SharedUserSetting) this.mSettings.getSettingLPr(sharedUserAppId);
            if (sharedUserSetting != null) {
                ArraySet<? extends PackageStateInternal> sharedUserPackages = sharedUserSetting.getPackageStates();
                Iterator<? extends PackageStateInternal> it = sharedUserPackages.iterator();
                while (it.hasNext()) {
                    PackageStateInternal ps = it.next();
                    res.add(ps);
                }
            }
            return res;
        }

        public void dumpPackagesProto(ProtoOutputStream proto) {
            this.mSettings.dumpPackagesProto(proto);
        }

        public void dumpPermissions(PrintWriter pw, String packageName, ArraySet<String> permissionNames, DumpState dumpState) {
            this.mSettings.dumpPermissions(pw, packageName, permissionNames, dumpState);
        }

        public void dumpPackages(PrintWriter pw, String packageName, ArraySet<String> permissionNames, DumpState dumpState, boolean checkin) {
            this.mSettings.dumpPackagesLPr(pw, packageName, permissionNames, dumpState, checkin);
        }

        public void dumpKeySet(PrintWriter pw, String packageName, DumpState dumpState) {
            this.mSettings.getKeySetManagerService().dumpLPr(pw, packageName, dumpState);
        }

        public void dumpSharedUsers(PrintWriter pw, String packageName, ArraySet<String> permissionNames, DumpState dumpState, boolean checkin) {
            this.mSettings.dumpSharedUsersLPr(pw, packageName, permissionNames, dumpState, checkin);
        }

        public void dumpReadMessages(PrintWriter pw, DumpState dumpState) {
            this.mSettings.dumpReadMessages(pw, dumpState);
        }

        public void dumpSharedUsersProto(ProtoOutputStream proto) {
            this.mSettings.dumpSharedUsersProto(proto);
        }

        public List<? extends PackageStateInternal> getVolumePackages(String volumeUuid) {
            return this.mSettings.getVolumePackagesLPr(volumeUuid);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ int lambda$static$0(ProviderInfo p1, ProviderInfo p2) {
        int v1 = p1.initOrder;
        int v2 = p2.initOrder;
        if (v1 > v2) {
            return -1;
        }
        return v1 < v2 ? 1 : 0;
    }

    private boolean safeMode() {
        return this.mService.getSafeMode();
    }

    protected ComponentName resolveComponentName() {
        return this.mLocalResolveComponentName;
    }

    protected ActivityInfo instantAppInstallerActivity() {
        return this.mLocalInstantAppInstallerActivity;
    }

    protected ApplicationInfo androidApplication() {
        return this.mLocalAndroidApplication;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ComputerEngine(PackageManagerService.Snapshot args, int version) {
        this.mVersion = version;
        this.mSettings = new Settings(args.settings);
        this.mIsolatedOwners = args.isolatedOwners;
        this.mPackages = args.packages;
        this.mSharedLibraries = args.sharedLibraries;
        this.mInstrumentation = args.instrumentation;
        this.mWebInstantAppsDisabled = args.webInstantAppsDisabled;
        this.mLocalResolveComponentName = args.resolveComponentName;
        this.mResolveActivity = args.resolveActivity;
        this.mLocalInstantAppInstallerActivity = args.instantAppInstallerActivity;
        this.mInstantAppInstallerInfo = args.instantAppInstallerInfo;
        this.mInstantAppRegistry = args.instantAppRegistry;
        this.mLocalAndroidApplication = args.androidApplication;
        this.mAppsFilter = args.appsFilter;
        this.mFrozenPackages = args.frozenPackages;
        this.mComponentResolver = args.componentResolver;
        this.mAppPredictionServicePackage = args.appPredictionServicePackage;
        this.mPermissionManager = args.service.mPermissionManager;
        this.mUserManager = args.service.mUserManager;
        this.mContext = args.service.mContext;
        this.mInjector = args.service.mInjector;
        this.mApexManager = args.service.mApexManager;
        this.mInstantAppResolverConnection = args.service.mInstantAppResolverConnection;
        this.mDefaultAppProvider = args.service.getDefaultAppProvider();
        this.mDomainVerificationManager = args.service.mDomainVerificationManager;
        this.mPackageDexOptimizer = args.service.mPackageDexOptimizer;
        this.mDexManager = args.service.getDexManager();
        this.mCompilerStats = args.service.mCompilerStats;
        this.mBackgroundDexOptService = args.service.mBackgroundDexOptService;
        this.mExternalSourcesPolicy = args.service.mExternalSourcesPolicy;
        this.mService = args.service;
    }

    @Override // com.android.server.pm.Computer
    public int getVersion() {
        return this.mVersion;
    }

    @Override // com.android.server.pm.Computer
    public final Computer use() {
        this.mUsed++;
        return this;
    }

    @Override // com.android.server.pm.Computer
    public final int getUsed() {
        return this.mUsed;
    }

    @Override // com.android.server.pm.Computer
    public final List<ResolveInfo> queryIntentActivitiesInternal(Intent intent, String resolvedType, long flags, long privateResolveFlags, int filterCallingUid, int userId, boolean resolveForStart, boolean allowDynamicSplits) {
        Intent intent2;
        Intent originalIntent;
        ComponentName comp;
        if (this.mUserManager.exists(userId)) {
            String instantAppPkgName = getInstantAppPackageName(filterCallingUid);
            enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "query intent activities");
            String pkgName = intent.getPackage();
            ComponentName comp2 = intent.getComponent();
            if (comp2 == null && intent.getSelector() != null) {
                Intent intent3 = intent.getSelector();
                originalIntent = intent;
                comp = intent3.getComponent();
                intent2 = intent3;
            } else {
                intent2 = intent;
                originalIntent = null;
                comp = comp2;
            }
            boolean blockNormalResolution = false;
            long flags2 = updateFlagsForResolve(flags, userId, filterCallingUid, resolveForStart, (comp == null && pkgName == null) ? false : true, isImplicitImageCaptureIntentAndNotSetByDpc(intent2, userId, resolvedType, flags));
            List<ResolveInfo> list = Collections.emptyList();
            boolean skipPostResolution = false;
            if (comp != null) {
                ActivityInfo ai = getActivityInfo(comp, flags2, userId);
                if (ai != null) {
                    boolean matchInstantApp = (8388608 & flags2) != 0;
                    boolean matchVisibleToInstantAppOnly = (16777216 & flags2) != 0;
                    boolean matchExplicitlyVisibleOnly = (33554432 & flags2) != 0;
                    boolean isCallerInstantApp = instantAppPkgName != null;
                    boolean isTargetSameInstantApp = comp.getPackageName().equals(instantAppPkgName);
                    boolean isTargetInstantApp = (ai.applicationInfo.privateFlags & 128) != 0;
                    boolean isTargetVisibleToInstantApp = (ai.flags & 1048576) != 0;
                    boolean isTargetExplicitlyVisibleToInstantApp = isTargetVisibleToInstantApp && (ai.flags & 2097152) == 0;
                    boolean isTargetHiddenFromInstantApp = !isTargetVisibleToInstantApp || (matchExplicitlyVisibleOnly && !isTargetExplicitlyVisibleToInstantApp);
                    boolean blockInstantResolution = !isTargetSameInstantApp && (!(matchInstantApp || isCallerInstantApp || !isTargetInstantApp) || (matchVisibleToInstantAppOnly && isCallerInstantApp && isTargetHiddenFromInstantApp));
                    if (!resolveForStart && !isTargetInstantApp && !isCallerInstantApp && shouldFilterApplication(getPackageStateInternal(ai.applicationInfo.packageName, 1000), filterCallingUid, userId)) {
                        blockNormalResolution = true;
                    }
                    if (!blockInstantResolution && !blockNormalResolution) {
                        ResolveInfo ri = new ResolveInfo();
                        ri.activityInfo = ai;
                        List<ResolveInfo> list2 = new ArrayList<>(1);
                        list2.add(ri);
                        PackageManagerServiceUtils.applyEnforceIntentFilterMatching(this.mInjector.getCompatibility(), this.mComponentResolver, list2, false, intent2, resolvedType, filterCallingUid);
                        list = list2;
                    }
                }
            } else {
                QueryIntentActivitiesResult lockedResult = queryIntentActivitiesInternalBody(intent2, resolvedType, flags2, filterCallingUid, userId, resolveForStart, allowDynamicSplits, pkgName, instantAppPkgName);
                if (lockedResult.answer != null) {
                    skipPostResolution = true;
                    list = lockedResult.answer;
                } else {
                    if (lockedResult.addInstant) {
                        String callingPkgName = getInstantAppPackageName(filterCallingUid);
                        boolean isRequesterInstantApp = isInstantApp(callingPkgName, userId);
                        lockedResult.result = maybeAddInstantAppInstaller(lockedResult.result, intent2, resolvedType, flags2, userId, resolveForStart, isRequesterInstantApp);
                    }
                    if (lockedResult.sortResult) {
                        lockedResult.result.sort(ComponentResolver.RESOLVE_PRIORITY_SORTER);
                    }
                    list = lockedResult.result;
                }
            }
            if (originalIntent != null) {
                PackageManagerServiceUtils.applyEnforceIntentFilterMatching(this.mInjector.getCompatibility(), this.mComponentResolver, list, false, originalIntent, resolvedType, filterCallingUid);
            }
            return skipPostResolution ? list : applyPostResolutionFilter(list, instantAppPkgName, allowDynamicSplits, filterCallingUid, resolveForStart, userId, intent2);
        }
        return Collections.emptyList();
    }

    @Override // com.android.server.pm.Computer
    public final List<ResolveInfo> queryIntentActivitiesInternal(Intent intent, String resolvedType, long flags, int filterCallingUid, int userId) {
        return queryIntentActivitiesInternal(intent, resolvedType, flags, 0L, filterCallingUid, userId, false, true);
    }

    @Override // com.android.server.pm.Computer
    public final List<ResolveInfo> queryIntentActivitiesInternal(Intent intent, String resolvedType, long flags, int userId) {
        return queryIntentActivitiesInternal(intent, resolvedType, flags, 0L, Binder.getCallingUid(), userId, false, true);
    }

    @Override // com.android.server.pm.Computer
    public final List<ResolveInfo> queryIntentServicesInternal(Intent intent, String resolvedType, long flags, int userId, int callingUid, boolean includeInstantApps) {
        Intent intent2;
        Intent originalIntent;
        ComponentName comp;
        if (this.mUserManager.exists(userId)) {
            enforceCrossUserOrProfilePermission(callingUid, userId, false, false, "query intent receivers");
            String instantAppPkgName = getInstantAppPackageName(callingUid);
            long flags2 = updateFlagsForResolve(flags, userId, callingUid, includeInstantApps, false);
            ComponentName comp2 = intent.getComponent();
            if (comp2 == null && intent.getSelector() != null) {
                Intent intent3 = intent.getSelector();
                originalIntent = intent;
                comp = intent3.getComponent();
                intent2 = intent3;
            } else {
                intent2 = intent;
                originalIntent = null;
                comp = comp2;
            }
            List<ResolveInfo> list = Collections.emptyList();
            if (comp != null) {
                ServiceInfo si = getServiceInfo(comp, flags2, userId);
                if (si != null) {
                    boolean z = false;
                    boolean matchInstantApp = (8388608 & flags2) != 0;
                    boolean matchVisibleToInstantAppOnly = (flags2 & 16777216) != 0;
                    boolean isCallerInstantApp = instantAppPkgName != null;
                    boolean isTargetSameInstantApp = comp.getPackageName().equals(instantAppPkgName);
                    boolean isTargetInstantApp = (si.applicationInfo.privateFlags & 128) != 0;
                    boolean isTargetHiddenFromInstantApp = (si.flags & 1048576) == 0;
                    boolean blockInstantResolution = !isTargetSameInstantApp && (!(matchInstantApp || isCallerInstantApp || !isTargetInstantApp) || (matchVisibleToInstantAppOnly && isCallerInstantApp && isTargetHiddenFromInstantApp));
                    if (!isTargetInstantApp && !isCallerInstantApp && shouldFilterApplication(getPackageStateInternal(si.applicationInfo.packageName, 1000), callingUid, userId)) {
                        z = true;
                    }
                    boolean blockNormalResolution = z;
                    if (!blockInstantResolution && !blockNormalResolution) {
                        ResolveInfo ri = new ResolveInfo();
                        ri.serviceInfo = si;
                        List<ResolveInfo> list2 = new ArrayList<>(1);
                        list2.add(ri);
                        list = list2;
                        PackageManagerServiceUtils.applyEnforceIntentFilterMatching(this.mInjector.getCompatibility(), this.mComponentResolver, list2, false, intent2, resolvedType, callingUid);
                    }
                }
            } else {
                list = queryIntentServicesInternalBody(intent2, resolvedType, flags2, userId, callingUid, instantAppPkgName);
            }
            if (originalIntent != null) {
                PackageManagerServiceUtils.applyEnforceIntentFilterMatching(this.mInjector.getCompatibility(), this.mComponentResolver, list, false, originalIntent, resolvedType, callingUid);
            }
            return list;
        }
        return Collections.emptyList();
    }

    protected List<ResolveInfo> queryIntentServicesInternalBody(Intent intent, String resolvedType, long flags, int userId, int callingUid, String instantAppPkgName) {
        String pkgName = intent.getPackage();
        if (pkgName == null) {
            List<ResolveInfo> resolveInfos = this.mComponentResolver.queryServices(this, intent, resolvedType, flags, userId);
            if (resolveInfos == null) {
                return Collections.emptyList();
            }
            return applyPostServiceResolutionFilter(resolveInfos, instantAppPkgName, userId, callingUid);
        }
        AndroidPackage pkg = this.mPackages.get(pkgName);
        if (pkg != null) {
            List<ResolveInfo> resolveInfos2 = this.mComponentResolver.queryServices(this, intent, resolvedType, flags, pkg.getServices(), userId);
            if (resolveInfos2 == null) {
                return Collections.emptyList();
            }
            return applyPostServiceResolutionFilter(resolveInfos2, instantAppPkgName, userId, callingUid);
        }
        return Collections.emptyList();
    }

    @Override // com.android.server.pm.Computer
    public QueryIntentActivitiesResult queryIntentActivitiesInternalBody(Intent intent, String resolvedType, long flags, int filterCallingUid, int userId, boolean resolveForStart, boolean allowDynamicSplits, String pkgName, String instantAppPkgName) {
        List<ResolveInfo> result;
        CrossProfileDomainInfo prioritizedXpInfo;
        boolean sortResult = false;
        boolean addInstant = false;
        if (pkgName == null) {
            int userId2 = resolveUserId(intent, resolvedType, userId, filterCallingUid);
            List<CrossProfileIntentFilter> matchingFilters = getMatchingCrossProfileIntentFilters(intent, resolvedType, userId2);
            ResolveInfo skipProfileInfo = querySkipCurrentProfileIntents(matchingFilters, intent, resolvedType, flags, userId2);
            if (skipProfileInfo == null) {
                List<ResolveInfo> result2 = filterIfNotSystemUser(this.mComponentResolver.queryActivities(this, intent, resolvedType, flags, userId2), userId2);
                addInstant = isInstantAppResolutionAllowed(intent, result2, userId2, false, flags);
                boolean hasNonNegativePriorityResult = hasNonNegativePriority(result2);
                List<ResolveInfo> result3 = result2;
                CrossProfileDomainInfo specificXpInfo = queryCrossProfileIntents(matchingFilters, intent, resolvedType, flags, userId2, hasNonNegativePriorityResult);
                if (intent.hasWebURI()) {
                    CrossProfileDomainInfo generalXpInfo = null;
                    UserInfo parent = getProfileParent(userId2);
                    if (parent != null) {
                        generalXpInfo = getCrossProfileDomainPreferredLpr(intent, resolvedType, flags, userId2, parent.id);
                    }
                    CrossProfileDomainInfo prioritizedXpInfo2 = generalXpInfo != null ? generalXpInfo : specificXpInfo;
                    if (addInstant) {
                        prioritizedXpInfo = prioritizedXpInfo2;
                    } else if (!result3.isEmpty() || prioritizedXpInfo2 == null) {
                        prioritizedXpInfo = prioritizedXpInfo2;
                        if (result3.size() > 1 || prioritizedXpInfo != null) {
                            result3 = result3;
                        } else {
                            return new QueryIntentActivitiesResult(applyPostResolutionFilter(result3, instantAppPkgName, allowDynamicSplits, filterCallingUid, resolveForStart, userId2, intent));
                        }
                    } else {
                        result3.add(prioritizedXpInfo2.mResolveInfo);
                        return new QueryIntentActivitiesResult(applyPostResolutionFilter(result3, instantAppPkgName, allowDynamicSplits, filterCallingUid, resolveForStart, userId2, intent));
                    }
                    result = filterCandidatesWithDomainPreferredActivitiesLPr(intent, flags, result3, prioritizedXpInfo, userId2);
                    sortResult = true;
                } else if (specificXpInfo == null) {
                    result = result3;
                } else {
                    result3.add(specificXpInfo.mResolveInfo);
                    sortResult = true;
                    result = result3;
                }
            } else {
                List<ResolveInfo> xpResult = new ArrayList<>(1);
                xpResult.add(skipProfileInfo);
                return new QueryIntentActivitiesResult(applyPostResolutionFilter(filterIfNotSystemUser(xpResult, userId2), instantAppPkgName, allowDynamicSplits, filterCallingUid, resolveForStart, userId2, intent));
            }
        } else {
            PackageStateInternal setting = getPackageStateInternal(pkgName, 1000);
            if (setting != null && setting.getAndroidPackage() != null && (resolveForStart || !shouldFilterApplication(setting, filterCallingUid, userId))) {
                result = filterIfNotSystemUser(this.mComponentResolver.queryActivities(this, intent, resolvedType, flags, setting.getAndroidPackage().getActivities(), userId), userId);
            } else {
                result = null;
            }
            if (result == null || result.size() == 0) {
                addInstant = isInstantAppResolutionAllowed(intent, null, userId, true, flags);
                if (result == null) {
                    result = new ArrayList();
                }
            }
        }
        return new QueryIntentActivitiesResult(sortResult, addInstant, result);
    }

    private ComponentName findInstallFailureActivity(String packageName, int filterCallingUid, int userId) {
        Intent failureActivityIntent = new Intent("android.intent.action.INSTALL_FAILURE");
        failureActivityIntent.setPackage(packageName);
        List<ResolveInfo> result = queryIntentActivitiesInternal(failureActivityIntent, null, 0L, 0L, filterCallingUid, userId, false, false);
        int numResults = result.size();
        if (numResults > 0) {
            for (int i = 0; i < numResults; i++) {
                ResolveInfo info = result.get(i);
                if (info.activityInfo.splitName == null) {
                    return new ComponentName(packageName, info.activityInfo.name);
                }
            }
            return null;
        }
        return null;
    }

    @Override // com.android.server.pm.Computer
    public final ActivityInfo getActivityInfo(ComponentName component, long flags, int userId) {
        return getActivityInfoInternal(component, flags, Binder.getCallingUid(), userId);
    }

    @Override // com.android.server.pm.Computer
    public final ActivityInfo getActivityInfoInternal(ComponentName component, long flags, int filterCallingUid, int userId) {
        if (this.mUserManager.exists(userId)) {
            long flags2 = updateFlagsForComponent(flags, userId);
            if (!isRecentsAccessingChildProfiles(Binder.getCallingUid(), userId)) {
                enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "get activity info");
            }
            return getActivityInfoInternalBody(component, flags2, filterCallingUid, userId);
        }
        return null;
    }

    protected ActivityInfo getActivityInfoInternalBody(ComponentName component, long flags, int filterCallingUid, int userId) {
        long flags2;
        AndroidPackage androidPackage;
        ParsedActivity a = this.mComponentResolver.getActivity(component);
        if (!this.mUserManager.isDualProfile(UserHandle.getUserId(Binder.getCallingUid()))) {
            flags2 = flags;
        } else {
            flags2 = flags | 4202496;
        }
        if (PackageManagerService.DEBUG_PACKAGE_INFO) {
            Log.v("PackageManager", "getActivityInfo " + component + ": " + a);
        }
        if (a != null) {
            androidPackage = this.mPackages.get(a.getPackageName());
        } else {
            androidPackage = null;
        }
        AndroidPackage pkg = androidPackage;
        if (pkg == null || !this.mSettings.isEnabledAndMatch(pkg, a, flags2, userId)) {
            long flags3 = flags2;
            if (!resolveComponentName().equals(component)) {
                return null;
            }
            return PackageInfoWithoutStateUtils.generateDelegateActivityInfo(this.mResolveActivity, flags3, PackageUserStateInternal.DEFAULT, userId);
        }
        PackageStateInternal ps = this.mSettings.getPackage(component.getPackageName());
        if (ps == null || shouldFilterApplication(ps, filterCallingUid, component, 1, userId)) {
            return null;
        }
        return PackageInfoUtils.generateActivityInfo(pkg, a, flags2, ps.getUserStateOrDefault(userId), userId, ps);
    }

    @Override // com.android.server.pm.Computer, com.android.server.pm.snapshot.PackageDataSnapshot
    public AndroidPackage getPackage(String packageName) {
        return this.mPackages.get(resolveInternalPackageName(packageName, -1L));
    }

    @Override // com.android.server.pm.Computer
    public AndroidPackage getPackage(int uid) {
        String[] packageNames = getPackagesForUidInternal(uid, 1000);
        AndroidPackage pkg = null;
        int numPackages = packageNames == null ? 0 : packageNames.length;
        for (int i = 0; pkg == null && i < numPackages; i++) {
            AndroidPackage pkg2 = this.mPackages.get(packageNames[i]);
            pkg = pkg2;
        }
        return pkg;
    }

    @Override // com.android.server.pm.Computer
    public final ApplicationInfo generateApplicationInfoFromSettings(String packageName, long flags, int filterCallingUid, int userId) {
        PackageStateInternal ps;
        if (!this.mUserManager.exists(userId) || (ps = this.mSettings.getPackage(packageName)) == null || filterSharedLibPackage(ps, filterCallingUid, userId, flags) || shouldFilterApplication(ps, filterCallingUid, userId)) {
            return null;
        }
        if (ps.getAndroidPackage() == null) {
            PackageInfo pInfo = generatePackageInfo(ps, flags, userId);
            if (pInfo != null) {
                return pInfo.applicationInfo;
            }
            return null;
        }
        ApplicationInfo ai = PackageInfoUtils.generateApplicationInfo(ps.getPkg(), flags, ps.getUserStateOrDefault(userId), userId, ps);
        if (ai != null) {
            ai.packageName = resolveExternalPackageName(ps.getPkg());
        }
        return ai;
    }

    @Override // com.android.server.pm.Computer
    public final ApplicationInfo getApplicationInfo(String packageName, long flags, int userId) {
        return getApplicationInfoInternal(packageName, flags, Binder.getCallingUid(), userId);
    }

    @Override // com.android.server.pm.Computer
    public final ApplicationInfo getApplicationInfoInternal(String packageName, long flags, int filterCallingUid, int userId) {
        if (this.mUserManager.exists(userId)) {
            long flags2 = updateFlagsForApplication(flags, userId);
            if (!isRecentsAccessingChildProfiles(Binder.getCallingUid(), userId)) {
                enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "get application info");
            }
            return getApplicationInfoInternalBody(packageName, flags2, filterCallingUid, userId);
        }
        return null;
    }

    protected ApplicationInfo getApplicationInfoInternalBody(String packageName, long flags, int filterCallingUid, int userId) {
        String packageName2 = resolveInternalPackageName(packageName, -1L);
        AndroidPackage p = this.mPackages.get(packageName2);
        if (PackageManagerService.DEBUG_PACKAGE_INFO) {
            Log.v("PackageManager", "getApplicationInfo " + packageName2 + ": " + p);
        }
        if (p != null) {
            PackageStateInternal ps = this.mSettings.getPackage(packageName2);
            if (ps == null || filterSharedLibPackage(ps, filterCallingUid, userId, flags) || shouldFilterApplication(ps, filterCallingUid, userId)) {
                return null;
            }
            ApplicationInfo ai = PackageInfoUtils.generateApplicationInfo(p, flags, ps.getUserStateOrDefault(userId), userId, ps);
            if (ai != null) {
                ai.packageName = resolveExternalPackageName(p);
            } else if (UserHandle.getUserId(Binder.getCallingUid()) == 999) {
                ai = generateApplicationInfoFromSettings(packageName2, flags | 4202496, filterCallingUid, userId);
            }
            return PackageManagerService.sPmsExt.updateApplicationInfoForRemovable(ai);
        } else if ((flags & 1073741824) != 0) {
            int apexFlags = 1;
            if ((flags & 1048576) != 0) {
                apexFlags = 2;
            }
            PackageInfo pi = this.mApexManager.getPackageInfo(packageName2, apexFlags);
            if (pi == null) {
                return null;
            }
            return pi.applicationInfo;
        } else if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(packageName2) || HostingRecord.HOSTING_TYPE_SYSTEM.equals(packageName2)) {
            return androidApplication();
        } else {
            if ((flags & 4202496) != 0) {
                return PackageManagerService.sPmsExt.updateApplicationInfoForRemovable(generateApplicationInfoFromSettings(packageName2, flags, filterCallingUid, userId));
            }
            return null;
        }
    }

    protected ArrayList<ResolveInfo> filterCandidatesWithDomainPreferredActivitiesLPrBody(Intent intent, long matchFlags, List<ResolveInfo> candidates, CrossProfileDomainInfo xpDomainInfo, int userId, boolean debug) {
        ArrayList<ResolveInfo> undefinedList;
        ArrayList<ResolveInfo> result = new ArrayList<>();
        ArrayList<ResolveInfo> matchAllList = new ArrayList<>();
        ArrayList<ResolveInfo> undefinedList2 = new ArrayList<>();
        boolean blockInstant = intent.isWebIntent() && areWebInstantAppsDisabled(userId);
        int count = candidates.size();
        for (int n = 0; n < count; n++) {
            ResolveInfo info = candidates.get(n);
            if (!blockInstant || (!info.isInstantAppAvailable && !isInstantAppInternal(info.activityInfo.packageName, userId, 1000))) {
                if (info.handleAllWebDataURI) {
                    matchAllList.add(info);
                } else {
                    undefinedList2.add(info);
                }
            }
        }
        boolean includeBrowser = false;
        if (!DomainVerificationUtils.isDomainVerificationIntent(intent, matchFlags)) {
            result.addAll(undefinedList2);
            if (xpDomainInfo != null && xpDomainInfo.mHighestApprovalLevel > 0) {
                result.add(xpDomainInfo.mResolveInfo);
            }
            includeBrowser = true;
        } else {
            DomainVerificationManagerInternal domainVerificationManagerInternal = this.mDomainVerificationManager;
            final Settings settings = this.mSettings;
            Objects.requireNonNull(settings);
            Pair<List<ResolveInfo>, Integer> infosAndLevel = domainVerificationManagerInternal.filterToApprovedApp(intent, undefinedList2, userId, new Function() { // from class: com.android.server.pm.ComputerEngine$$ExternalSyntheticLambda2
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return ComputerEngine.Settings.this.getPackage((String) obj);
                }
            });
            List<ResolveInfo> approvedInfos = (List) infosAndLevel.first;
            Integer highestApproval = (Integer) infosAndLevel.second;
            if (approvedInfos.isEmpty()) {
                includeBrowser = true;
                if (xpDomainInfo != null && xpDomainInfo.mHighestApprovalLevel > 0) {
                    result.add(xpDomainInfo.mResolveInfo);
                }
            } else {
                result.addAll(approvedInfos);
                if (xpDomainInfo != null && xpDomainInfo.mHighestApprovalLevel > highestApproval.intValue()) {
                    result.add(xpDomainInfo.mResolveInfo);
                }
            }
        }
        if (includeBrowser) {
            if (PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
                Slog.v("PackageManager", "   ...including browsers in candidate set");
            }
            if ((matchFlags & 131072) != 0) {
                result.addAll(matchAllList);
            } else {
                String defaultBrowserPackageName = this.mDefaultAppProvider.getDefaultBrowser(userId);
                int maxMatchPrio = 0;
                ResolveInfo defaultBrowserMatch = null;
                int numCandidates = matchAllList.size();
                int n2 = 0;
                while (n2 < numCandidates) {
                    ResolveInfo info2 = matchAllList.get(n2);
                    if (info2.priority > maxMatchPrio) {
                        maxMatchPrio = info2.priority;
                    }
                    if (!info2.activityInfo.packageName.equals(defaultBrowserPackageName)) {
                        undefinedList = undefinedList2;
                    } else {
                        if (defaultBrowserMatch != null) {
                            undefinedList = undefinedList2;
                            if (defaultBrowserMatch.priority >= info2.priority) {
                            }
                        } else {
                            undefinedList = undefinedList2;
                        }
                        if (debug) {
                            Slog.v("PackageManager", "Considering default browser match " + info2);
                        }
                        defaultBrowserMatch = info2;
                    }
                    n2++;
                    undefinedList2 = undefinedList;
                }
                if (defaultBrowserMatch != null && defaultBrowserMatch.priority >= maxMatchPrio && !TextUtils.isEmpty(defaultBrowserPackageName)) {
                    if (debug) {
                        Slog.v("PackageManager", "Default browser match " + defaultBrowserMatch);
                    }
                    result.add(defaultBrowserMatch);
                } else {
                    result.addAll(matchAllList);
                }
            }
            if (result.size() == 0) {
                result.addAll(candidates);
            }
        }
        return result;
    }

    @Override // com.android.server.pm.Computer
    public final ComponentName getDefaultHomeActivity(int userId) {
        List<ResolveInfo> allHomeCandidates = new ArrayList<>();
        ComponentName cn = getHomeActivitiesAsUser(allHomeCandidates, userId);
        if (cn != null) {
            return cn;
        }
        Slog.w("PackageManager", "Default package for ROLE_HOME is not set in RoleManager");
        int lastPriority = Integer.MIN_VALUE;
        ComponentName lastComponent = null;
        int size = allHomeCandidates.size();
        for (int i = 0; i < size; i++) {
            ResolveInfo ri = allHomeCandidates.get(i);
            if (ri.priority > lastPriority) {
                lastComponent = ri.activityInfo.getComponentName();
                lastPriority = ri.priority;
            } else if (ri.priority == lastPriority) {
                lastComponent = null;
            }
        }
        return lastComponent;
    }

    /* JADX WARN: Removed duplicated region for block: B:18:0x005e A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:19:0x005f  */
    @Override // com.android.server.pm.Computer
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public final ComponentName getHomeActivitiesAsUser(List<ResolveInfo> allHomeCandidates, int userId) {
        String packageName;
        Intent intent = getHomeIntent();
        List<ResolveInfo> resolveInfos = queryIntentActivitiesInternal(intent, null, 128L, userId);
        allHomeCandidates.clear();
        if (resolveInfos == null) {
            return null;
        }
        allHomeCandidates.addAll(resolveInfos);
        String packageName2 = this.mDefaultAppProvider.getDefaultHome(userId);
        if (packageName2 == null) {
            int appId = UserHandle.getAppId(Binder.getCallingUid());
            boolean filtered = appId >= 10000;
            PackageManagerService.FindPreferredActivityBodyResult result = findPreferredActivityInternal(intent, null, 0L, resolveInfos, true, false, false, userId, filtered);
            ResolveInfo preferredResolveInfo = result.mPreferredResolveInfo;
            if (preferredResolveInfo != null && preferredResolveInfo.activityInfo != null) {
                packageName = preferredResolveInfo.activityInfo.packageName;
                if (packageName != null) {
                    return null;
                }
                int resolveInfosSize = resolveInfos.size();
                for (int i = 0; i < resolveInfosSize; i++) {
                    ResolveInfo resolveInfo = resolveInfos.get(i);
                    if (resolveInfo.activityInfo != null && TextUtils.equals(resolveInfo.activityInfo.packageName, packageName)) {
                        return new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
                    }
                }
                return null;
            }
        }
        packageName = packageName2;
        if (packageName != null) {
        }
    }

    @Override // com.android.server.pm.Computer
    public final CrossProfileDomainInfo getCrossProfileDomainPreferredLpr(Intent intent, String resolvedType, long flags, int sourceUserId, int parentUserId) {
        List<ResolveInfo> resultTargetUser;
        if (this.mUserManager.hasUserRestriction("allow_parent_profile_app_linking", sourceUserId) && (resultTargetUser = this.mComponentResolver.queryActivities(this, intent, resolvedType, flags, parentUserId)) != null && !resultTargetUser.isEmpty()) {
            CrossProfileDomainInfo result = null;
            int size = resultTargetUser.size();
            for (int i = 0; i < size; i++) {
                ResolveInfo riTargetUser = resultTargetUser.get(i);
                if (!riTargetUser.handleAllWebDataURI) {
                    String packageName = riTargetUser.activityInfo.packageName;
                    PackageStateInternal ps = this.mSettings.getPackage(packageName);
                    if (ps != null) {
                        int approvalLevel = this.mDomainVerificationManager.approvalLevelForDomain(ps, intent, flags, parentUserId);
                        if (result == null) {
                            result = new CrossProfileDomainInfo(createForwardingResolveInfoUnchecked(new WatchedIntentFilter(), sourceUserId, parentUserId), approvalLevel);
                        } else {
                            result.mHighestApprovalLevel = Math.max(approvalLevel, result.mHighestApprovalLevel);
                        }
                    }
                }
            }
            if (result == null || result.mHighestApprovalLevel > 0) {
                return result;
            }
            return null;
        }
        return null;
    }

    @Override // com.android.server.pm.Computer
    public final Intent getHomeIntent() {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        intent.addCategory("android.intent.category.DEFAULT");
        return intent;
    }

    /* JADX DEBUG: Type inference failed for r1v2. Raw type applied. Possible types: java.util.List<R>, java.util.List<com.android.server.pm.CrossProfileIntentFilter> */
    @Override // com.android.server.pm.Computer
    public final List<CrossProfileIntentFilter> getMatchingCrossProfileIntentFilters(Intent intent, String resolvedType, int userId) {
        CrossProfileIntentResolver resolver = this.mSettings.getCrossProfileIntentResolver(userId);
        if (resolver != null) {
            return resolver.queryIntent(this, intent, resolvedType, false, userId);
        }
        return null;
    }

    @Override // com.android.server.pm.Computer
    public final List<ResolveInfo> applyPostResolutionFilter(List<ResolveInfo> resolveInfos, String ephemeralPkgName, boolean allowDynamicSplits, int filterCallingUid, boolean resolveForStart, int userId, Intent intent) {
        boolean blockInstant = intent.isWebIntent() && areWebInstantAppsDisabled(userId);
        for (int i = resolveInfos.size() - 1; i >= 0; i--) {
            ResolveInfo info = resolveInfos.get(i);
            if (info.isInstantAppAvailable && blockInstant) {
                resolveInfos.remove(i);
            } else {
                if (allowDynamicSplits && info.activityInfo != null && info.activityInfo.splitName != null) {
                    if (!ArrayUtils.contains(info.activityInfo.applicationInfo.splitNames, info.activityInfo.splitName)) {
                        if (instantAppInstallerActivity() == null) {
                            if (PackageManagerService.DEBUG_INSTALL) {
                                Slog.v("PackageManager", "No installer - not adding it to the ResolveInfo list");
                            }
                            resolveInfos.remove(i);
                        } else if (blockInstant && isInstantAppInternal(info.activityInfo.packageName, userId, 1000)) {
                            resolveInfos.remove(i);
                        } else {
                            if (PackageManagerService.DEBUG_INSTALL) {
                                Slog.v("PackageManager", "Adding installer to the ResolveInfo list");
                            }
                            ResolveInfo installerInfo = new ResolveInfo(this.mInstantAppInstallerInfo);
                            ComponentName installFailureActivity = findInstallFailureActivity(info.activityInfo.packageName, filterCallingUid, userId);
                            installerInfo.auxiliaryInfo = new AuxiliaryResolveInfo(installFailureActivity, info.activityInfo.packageName, info.activityInfo.applicationInfo.longVersionCode, info.activityInfo.splitName);
                            installerInfo.filter = new IntentFilter();
                            installerInfo.resolvePackageName = info.getComponentInfo().packageName;
                            installerInfo.labelRes = info.resolveLabelResId();
                            installerInfo.icon = info.resolveIconResId();
                            installerInfo.isInstantAppAvailable = true;
                            resolveInfos.set(i, installerInfo);
                        }
                    }
                }
                if (ephemeralPkgName == null) {
                    SettingBase callingSetting = this.mSettings.getSettingBase(UserHandle.getAppId(filterCallingUid));
                    PackageStateInternal resolvedSetting = getPackageStateInternal(info.activityInfo.packageName, 0);
                    if (!resolveForStart) {
                        if (!this.mAppsFilter.shouldFilterApplication(this, filterCallingUid, callingSetting, resolvedSetting, userId)) {
                        }
                        resolveInfos.remove(i);
                    }
                } else if (!ephemeralPkgName.equals(info.activityInfo.packageName)) {
                    if (resolveForStart) {
                        if ((intent.isWebIntent() || (intent.getFlags() & 2048) != 0) && intent.getPackage() == null && intent.getComponent() == null) {
                        }
                    }
                    if ((info.activityInfo.flags & 1048576) != 0 && !info.activityInfo.applicationInfo.isInstantApp()) {
                    }
                    resolveInfos.remove(i);
                }
            }
        }
        return resolveInfos;
    }

    private List<ResolveInfo> applyPostServiceResolutionFilter(List<ResolveInfo> resolveInfos, String instantAppPkgName, int userId, int filterCallingUid) {
        for (int i = resolveInfos.size() - 1; i >= 0; i--) {
            ResolveInfo info = resolveInfos.get(i);
            if (instantAppPkgName == null) {
                SettingBase callingSetting = this.mSettings.getSettingBase(UserHandle.getAppId(filterCallingUid));
                PackageStateInternal resolvedSetting = getPackageStateInternal(info.serviceInfo.packageName, 0);
                if (!this.mAppsFilter.shouldFilterApplication(this, filterCallingUid, callingSetting, resolvedSetting, userId)) {
                }
            }
            boolean isEphemeralApp = info.serviceInfo.applicationInfo.isInstantApp();
            if (isEphemeralApp && instantAppPkgName.equals(info.serviceInfo.packageName)) {
                if (info.serviceInfo.splitName != null && !ArrayUtils.contains(info.serviceInfo.applicationInfo.splitNames, info.serviceInfo.splitName)) {
                    if (instantAppInstallerActivity() == null) {
                        if (PackageManagerService.DEBUG_INSTANT) {
                            Slog.v("PackageManager", "No installer - not adding it to the ResolveInfolist");
                        }
                        resolveInfos.remove(i);
                    } else {
                        if (PackageManagerService.DEBUG_INSTANT) {
                            Slog.v("PackageManager", "Adding ephemeral installer to the ResolveInfo list");
                        }
                        ResolveInfo installerInfo = new ResolveInfo(this.mInstantAppInstallerInfo);
                        installerInfo.auxiliaryInfo = new AuxiliaryResolveInfo((ComponentName) null, info.serviceInfo.packageName, info.serviceInfo.applicationInfo.longVersionCode, info.serviceInfo.splitName);
                        installerInfo.filter = new IntentFilter();
                        installerInfo.resolvePackageName = info.getComponentInfo().packageName;
                        resolveInfos.set(i, installerInfo);
                    }
                }
            } else if (isEphemeralApp || (info.serviceInfo.flags & 1048576) == 0) {
                resolveInfos.remove(i);
            }
        }
        return resolveInfos;
    }

    private List<ResolveInfo> filterCandidatesWithDomainPreferredActivitiesLPr(Intent intent, long matchFlags, List<ResolveInfo> candidates, CrossProfileDomainInfo xpDomainInfo, int userId) {
        boolean debug = (intent.getFlags() & 8) != 0;
        if (PackageManagerService.DEBUG_PREFERRED || PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
            Slog.v("PackageManager", "Filtering results with preferred activities. Candidates count: " + candidates.size());
        }
        ArrayList<ResolveInfo> result = filterCandidatesWithDomainPreferredActivitiesLPrBody(intent, matchFlags, candidates, xpDomainInfo, userId, debug);
        if (PackageManagerService.DEBUG_PREFERRED || PackageManagerService.DEBUG_DOMAIN_VERIFICATION) {
            Slog.v("PackageManager", "Filtered results with preferred activities. New candidates count: " + result.size());
            Iterator<ResolveInfo> it = result.iterator();
            while (it.hasNext()) {
                ResolveInfo info = it.next();
                Slog.v("PackageManager", "  + " + info.activityInfo);
            }
        }
        return result;
    }

    private List<ResolveInfo> filterIfNotSystemUser(List<ResolveInfo> resolveInfos, int userId) {
        if (userId == 0) {
            return resolveInfos;
        }
        for (int i = CollectionUtils.size(resolveInfos) - 1; i >= 0; i--) {
            ResolveInfo info = resolveInfos.get(i);
            if ((info.activityInfo.flags & 536870912) != 0) {
                resolveInfos.remove(i);
            }
        }
        return resolveInfos;
    }

    /* JADX WARN: Removed duplicated region for block: B:27:0x00bb  */
    /* JADX WARN: Removed duplicated region for block: B:30:0x0118  */
    /* JADX WARN: Removed duplicated region for block: B:42:0x016c  */
    /* JADX WARN: Removed duplicated region for block: B:50:0x019e  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private List<ResolveInfo> maybeAddInstantAppInstaller(List<ResolveInfo> result, Intent intent, String resolvedType, long flags, int userId, boolean resolveForStart, boolean isRequesterInstantApp) {
        String str;
        ResolveInfo localInstantApp;
        boolean blockResolution;
        AuxiliaryResolveInfo auxiliaryResponse;
        String str2;
        PackageStateInternal ps;
        ResolveInfo ephemeralInstaller;
        boolean alreadyResolvedLocally = (flags & 8388608) != 0;
        if (!alreadyResolvedLocally) {
            str = "PackageManager";
            List<ResolveInfo> instantApps = this.mComponentResolver.queryActivities(this, intent, resolvedType, 8388608 | flags | 64 | 16777216, userId);
            for (int i = instantApps.size() - 1; i >= 0; i--) {
                ResolveInfo info = instantApps.get(i);
                String packageName = info.activityInfo.packageName;
                PackageStateInternal ps2 = this.mSettings.getPackage(packageName);
                if (ps2.getUserStateOrDefault(userId).isInstantApp()) {
                    if (PackageManagerServiceUtils.hasAnyDomainApproval(this.mDomainVerificationManager, ps2, intent, flags, userId)) {
                        if (PackageManagerService.DEBUG_INSTANT) {
                            Slog.v(str, "Instant app approved for intent; pkg: " + packageName);
                        }
                        localInstantApp = info;
                        blockResolution = false;
                    } else {
                        if (PackageManagerService.DEBUG_INSTANT) {
                            Slog.v(str, "Instant app not approved for intent; pkg: " + packageName);
                        }
                        localInstantApp = null;
                        blockResolution = true;
                    }
                    auxiliaryResponse = null;
                    if (!blockResolution) {
                        str2 = str;
                    } else if (localInstantApp == null) {
                        Trace.traceBegin(262144L, "resolveEphemeral");
                        String token = UUID.randomUUID().toString();
                        InstantAppResolveInfo.InstantAppDigest digest = InstantAppResolver.parseDigest(intent);
                        str2 = str;
                        InstantAppRequest requestObject = new InstantAppRequest((AuxiliaryResolveInfo) null, intent, resolvedType, (String) null, (String) null, isRequesterInstantApp, userId, (Bundle) null, resolveForStart, digest.getDigestPrefixSecure(), token);
                        auxiliaryResponse = InstantAppResolver.doInstantAppResolutionPhaseOne(this, this.mUserManager, this.mInstantAppResolverConnection, requestObject);
                        Trace.traceEnd(262144L);
                    } else {
                        str2 = str;
                        ApplicationInfo ai = localInstantApp.activityInfo.applicationInfo;
                        auxiliaryResponse = new AuxiliaryResolveInfo((ComponentName) null, ai.packageName, ai.longVersionCode, (String) null);
                    }
                    if ((intent.isWebIntent() || auxiliaryResponse != null) && (ps = this.mSettings.getPackage(instantAppInstallerActivity().packageName)) != null && PackageUserStateUtils.isEnabled(ps.getUserStateOrDefault(userId), instantAppInstallerActivity(), 0L)) {
                        ephemeralInstaller = new ResolveInfo(this.mInstantAppInstallerInfo);
                        ephemeralInstaller.activityInfo = PackageInfoWithoutStateUtils.generateDelegateActivityInfo(instantAppInstallerActivity(), 0L, ps.getUserStateOrDefault(userId), userId);
                        ephemeralInstaller.match = 5799936;
                        ephemeralInstaller.filter = new IntentFilter();
                        if (intent.getAction() != null) {
                            ephemeralInstaller.filter.addAction(intent.getAction());
                        }
                        if (intent.getData() != null && intent.getData().getPath() != null) {
                            ephemeralInstaller.filter.addDataPath(intent.getData().getPath(), 0);
                        }
                        ephemeralInstaller.isInstantAppAvailable = true;
                        ephemeralInstaller.isDefault = true;
                        ephemeralInstaller.auxiliaryInfo = auxiliaryResponse;
                        if (PackageManagerService.DEBUG_INSTANT) {
                            Slog.v(str2, "Adding ephemeral installer to the ResolveInfo list");
                        }
                        result.add(ephemeralInstaller);
                        return result;
                    }
                    return result;
                }
            }
        } else {
            str = "PackageManager";
        }
        localInstantApp = null;
        blockResolution = false;
        auxiliaryResponse = null;
        if (!blockResolution) {
        }
        if (intent.isWebIntent()) {
        }
        ephemeralInstaller = new ResolveInfo(this.mInstantAppInstallerInfo);
        ephemeralInstaller.activityInfo = PackageInfoWithoutStateUtils.generateDelegateActivityInfo(instantAppInstallerActivity(), 0L, ps.getUserStateOrDefault(userId), userId);
        ephemeralInstaller.match = 5799936;
        ephemeralInstaller.filter = new IntentFilter();
        if (intent.getAction() != null) {
        }
        if (intent.getData() != null) {
            ephemeralInstaller.filter.addDataPath(intent.getData().getPath(), 0);
        }
        ephemeralInstaller.isInstantAppAvailable = true;
        ephemeralInstaller.isDefault = true;
        ephemeralInstaller.auxiliaryInfo = auxiliaryResponse;
        if (PackageManagerService.DEBUG_INSTANT) {
        }
        result.add(ephemeralInstaller);
        return result;
    }

    @Override // com.android.server.pm.Computer
    public final PackageInfo generatePackageInfo(PackageStateInternal ps, long flags, int userId) {
        long flags2;
        Set<String> permissions;
        if (this.mUserManager.exists(userId) && ps != null) {
            int callingUid = Binder.getCallingUid();
            if (shouldFilterApplication(ps, callingUid, userId)) {
                return null;
            }
            if ((flags & 8192) != 0 && ps.isSystem()) {
                flags2 = flags | 4194304;
            } else {
                flags2 = flags;
            }
            PackageUserStateInternal state = ps.getUserStateOrDefault(userId);
            AndroidPackage p = ps.getPkg();
            if (p == null) {
                long flags3 = flags2;
                if ((8192 & flags3) != 0 && PackageUserStateUtils.isAvailable(state, flags3)) {
                    PackageInfo pi = new PackageInfo();
                    pi.packageName = ps.getPackageName();
                    pi.setLongVersionCode(ps.getVersionCode());
                    SharedUserApi sharedUser = this.mSettings.getSharedUserFromPackageName(pi.packageName);
                    pi.sharedUserId = sharedUser != null ? sharedUser.getName() : null;
                    pi.firstInstallTime = state.getFirstInstallTime();
                    pi.lastUpdateTime = ps.getLastUpdateTime();
                    ApplicationInfo ai = new ApplicationInfo();
                    ai.packageName = ps.getPackageName();
                    ai.uid = UserHandle.getUid(userId, ps.getAppId());
                    ai.primaryCpuAbi = ps.getPrimaryCpuAbi();
                    ai.secondaryCpuAbi = ps.getSecondaryCpuAbi();
                    ai.setVersionCode(ps.getVersionCode());
                    ai.flags = ps.getFlags();
                    ai.privateFlags = ps.getPrivateFlags();
                    pi.applicationInfo = PackageInfoWithoutStateUtils.generateDelegateApplicationInfo(ai, flags3, state, userId);
                    if (PackageManagerService.DEBUG_PACKAGE_INFO) {
                        Log.v("PackageManager", "ps.pkg is n/a for [" + ps.getPackageName() + "]. Provides a minimum info.");
                    }
                    return pi;
                }
                return null;
            }
            int[] gids = (256 & flags2) == 0 ? PackageManagerService.EMPTY_INT_ARRAY : this.mPermissionManager.getGidsForUid(UserHandle.getUid(userId, ps.getAppId()));
            if ((4096 & flags2) == 0 || ArrayUtils.isEmpty(p.getRequestedPermissions())) {
                permissions = Collections.emptySet();
            } else {
                permissions = this.mPermissionManager.getGrantedPermissions(ps.getPackageName(), userId);
            }
            PackageInfo packageInfo = PackageInfoUtils.generate(p, gids, flags2, state.getFirstInstallTime(), ps.getLastUpdateTime(), permissions, state, userId, ps);
            if (packageInfo == null) {
                return null;
            }
            ApplicationInfo applicationInfo = packageInfo.applicationInfo;
            String resolveExternalPackageName = resolveExternalPackageName(p);
            applicationInfo.packageName = resolveExternalPackageName;
            packageInfo.packageName = resolveExternalPackageName;
            return PackageManagerService.sPmsExt.updatePackageInfoForRemovable(packageInfo);
        }
        return null;
    }

    @Override // com.android.server.pm.Computer
    public final PackageInfo getPackageInfo(String packageName, long flags, int userId) {
        return getPackageInfoInternal(packageName, -1L, flags, Binder.getCallingUid(), userId);
    }

    @Override // com.android.server.pm.Computer
    public final PackageInfo getPackageInfoInternal(String packageName, long versionCode, long flags, int filterCallingUid, int userId) {
        if (this.mUserManager.exists(userId)) {
            long flags2 = updateFlagsForPackage(flags, userId);
            enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "get package info");
            return getPackageInfoInternalBody(packageName, versionCode, flags2, filterCallingUid, userId);
        }
        return null;
    }

    protected PackageInfo getPackageInfoInternalBody(String packageName, long versionCode, long flags, int filterCallingUid, int userId) {
        String packageName2 = resolveInternalPackageName(packageName, versionCode);
        boolean matchFactoryOnly = (2097152 & flags) != 0;
        if (matchFactoryOnly) {
            if ((flags & 1073741824) != 0) {
                return this.mApexManager.getPackageInfo(packageName2, 2);
            }
            PackageStateInternal ps = this.mSettings.getDisabledSystemPkg(packageName2);
            if (ps != null) {
                if (filterSharedLibPackage(ps, filterCallingUid, userId, flags) || shouldFilterApplication(ps, filterCallingUid, userId)) {
                    return null;
                }
                return generatePackageInfo(ps, flags, userId);
            }
        }
        AndroidPackage p = this.mPackages.get(packageName2);
        if (!matchFactoryOnly || p == null || p.isSystem()) {
            if (PackageManagerService.DEBUG_PACKAGE_INFO) {
                Log.v("PackageManager", "getPackageInfo " + packageName2 + ": " + p);
            }
            if (p != null) {
                PackageStateInternal ps2 = getPackageStateInternal(p.getPackageName());
                if (filterSharedLibPackage(ps2, filterCallingUid, userId, flags)) {
                    return null;
                }
                if (ps2 == null || !shouldFilterApplication(ps2, filterCallingUid, userId)) {
                    PackageInfo pi = generatePackageInfo(ps2, flags, userId);
                    if (pi == null && this.mUserManager.isDualProfile(UserHandle.getUserId(Binder.getCallingUid()))) {
                        return generatePackageInfo(this.mSettings.getPackage(packageName2), 4202496 | flags, userId);
                    }
                    return pi;
                }
                return null;
            } else if (!matchFactoryOnly && (4202496 & flags) != 0) {
                PackageStateInternal ps3 = this.mSettings.getPackage(packageName2);
                if (ps3 == null || filterSharedLibPackage(ps3, filterCallingUid, userId, flags) || shouldFilterApplication(ps3, filterCallingUid, userId)) {
                    return null;
                }
                return generatePackageInfo(ps3, flags, userId);
            } else if ((1073741824 & flags) != 0) {
                return this.mApexManager.getPackageInfo(packageName2, 1);
            } else {
                return null;
            }
        }
        return null;
    }

    @Override // com.android.server.pm.Computer
    public String[] getAllAvailablePackageNames() {
        return (String[]) this.mPackages.keySet().toArray(new String[0]);
    }

    @Override // com.android.server.pm.Computer
    public final PackageStateInternal getPackageStateInternal(String packageName) {
        return getPackageStateInternal(packageName, Binder.getCallingUid());
    }

    @Override // com.android.server.pm.Computer
    public PackageStateInternal getPackageStateInternal(String packageName, int callingUid) {
        return this.mSettings.getPackage(resolveInternalPackageNameInternalLocked(packageName, -1L, callingUid));
    }

    @Override // com.android.server.pm.Computer
    public final ParceledListSlice<PackageInfo> getInstalledPackages(long flags, int userId) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return ParceledListSlice.emptyList();
        }
        if (this.mUserManager.exists(userId)) {
            long flags2 = updateFlagsForPackage(flags, userId);
            enforceCrossUserPermission(callingUid, userId, false, false, "get installed packages");
            return getInstalledPackagesBody(flags2, userId, callingUid);
        }
        return ParceledListSlice.emptyList();
    }

    /* JADX WARN: Removed duplicated region for block: B:79:0x0095 A[SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:80:0x0092 A[SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:93:0x0116 A[SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:94:0x0113 A[SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    protected ParceledListSlice<PackageInfo> getInstalledPackagesBody(long flags, int userId, int callingUid) {
        ArrayList<PackageInfo> list;
        PackageStateInternal ps;
        PackageStateInternal ps2;
        boolean listUninstalled = (4202496 & flags) != 0;
        boolean listApex = (1073741824 & flags) != 0;
        boolean listFactory = (2097152 & flags) != 0;
        if (listUninstalled) {
            list = new ArrayList<>(this.mSettings.getPackages().size());
            Iterator<? extends PackageStateInternal> it = this.mSettings.getPackages().values().iterator();
            while (it.hasNext()) {
                PackageStateInternal ps3 = it.next();
                if (listFactory) {
                    if (ps3.isSystem() && !PackageManagerService.sPmsExt.isRemovableSysApp(ps3.getPackageName())) {
                        PackageStateInternal psDisabled = this.mSettings.getDisabledSystemPkg(ps3.getPackageName());
                        if (psDisabled != null) {
                            ps2 = psDisabled;
                            Iterator<? extends PackageStateInternal> it2 = it;
                            PackageStateInternal ps4 = ps2;
                            if (!filterSharedLibPackage(ps2, callingUid, userId, flags)) {
                                it = it2;
                            } else if (shouldFilterApplication(ps4, callingUid, userId)) {
                                it = it2;
                            } else {
                                PackageInfo pi = generatePackageInfo(ps4, flags, userId);
                                if (pi != null) {
                                    list.add(pi);
                                }
                                it = it2;
                            }
                        }
                    }
                }
                ps2 = ps3;
                Iterator<? extends PackageStateInternal> it22 = it;
                PackageStateInternal ps42 = ps2;
                if (!filterSharedLibPackage(ps2, callingUid, userId, flags)) {
                }
            }
        } else {
            list = new ArrayList<>(this.mPackages.size());
            Iterator<AndroidPackage> it3 = this.mPackages.values().iterator();
            while (it3.hasNext()) {
                AndroidPackage p = it3.next();
                PackageStateInternal ps5 = getPackageStateInternal(p.getPackageName());
                if (listFactory) {
                    if (p.isSystem() && !PackageManagerService.sPmsExt.isRemovableSysApp(p.getPackageName())) {
                        PackageStateInternal psDisabled2 = ps5 == null ? null : this.mSettings.getDisabledSystemPkg(ps5.getPackageName());
                        if (psDisabled2 != null) {
                            ps = psDisabled2;
                            Iterator<AndroidPackage> it4 = it3;
                            PackageStateInternal ps6 = ps;
                            if (!filterSharedLibPackage(ps, callingUid, userId, flags)) {
                                it3 = it4;
                            } else if (shouldFilterApplication(ps6, callingUid, userId)) {
                                it3 = it4;
                            } else {
                                PackageInfo pi2 = generatePackageInfo(ps6, flags, userId);
                                if (pi2 != null) {
                                    list.add(pi2);
                                }
                                it3 = it4;
                            }
                        }
                    }
                }
                ps = ps5;
                Iterator<AndroidPackage> it42 = it3;
                PackageStateInternal ps62 = ps;
                if (!filterSharedLibPackage(ps, callingUid, userId, flags)) {
                }
            }
        }
        if (listApex) {
            if (listFactory) {
                list.addAll(this.mApexManager.getFactoryPackages());
            } else {
                list.addAll(this.mApexManager.getActivePackages());
                if (listUninstalled) {
                    list.addAll(this.mApexManager.getInactivePackages());
                }
            }
        }
        return new ParceledListSlice<>(list);
    }

    private CrossProfileDomainInfo createForwardingResolveInfo(CrossProfileIntentFilter filter, Intent intent, String resolvedType, long flags, int sourceUserId) {
        ResolveInfo forwardingInfo;
        int targetUserId = filter.getTargetUserId();
        if (isUserEnabled(targetUserId)) {
            List<ResolveInfo> resultTargetUser = this.mComponentResolver.queryActivities(this, intent, resolvedType, flags, targetUserId);
            if (CollectionUtils.isEmpty(resultTargetUser)) {
                return null;
            }
            int i = resultTargetUser.size() - 1;
            while (true) {
                if (i < 0) {
                    forwardingInfo = null;
                    break;
                }
                ResolveInfo targetUserResolveInfo = resultTargetUser.get(i);
                if ((targetUserResolveInfo.activityInfo.applicationInfo.flags & 1073741824) == 0) {
                    ResolveInfo forwardingInfo2 = createForwardingResolveInfoUnchecked(filter, sourceUserId, targetUserId);
                    forwardingInfo = forwardingInfo2;
                    break;
                }
                i--;
            }
            if (forwardingInfo == null) {
                return null;
            }
            int size = resultTargetUser.size();
            int highestApprovalLevel = 0;
            for (int i2 = 0; i2 < size; i2++) {
                ResolveInfo riTargetUser = resultTargetUser.get(i2);
                if (!riTargetUser.handleAllWebDataURI) {
                    String packageName = riTargetUser.activityInfo.packageName;
                    PackageStateInternal ps = this.mSettings.getPackage(packageName);
                    if (ps != null) {
                        highestApprovalLevel = Math.max(highestApprovalLevel, this.mDomainVerificationManager.approvalLevelForDomain(ps, intent, flags, targetUserId));
                    }
                }
            }
            return new CrossProfileDomainInfo(forwardingInfo, highestApprovalLevel);
        }
        return null;
    }

    @Override // com.android.server.pm.Computer
    public final ResolveInfo createForwardingResolveInfoUnchecked(WatchedIntentFilter filter, int sourceUserId, int targetUserId) {
        String className;
        ResolveInfo forwardingResolveInfo = new ResolveInfo();
        long ident = Binder.clearCallingIdentity();
        try {
            boolean targetIsProfile = this.mUserManager.getUserInfo(targetUserId).isManagedProfile();
            if (targetIsProfile) {
                className = IntentForwarderActivity.FORWARD_INTENT_TO_MANAGED_PROFILE;
            } else {
                className = IntentForwarderActivity.FORWARD_INTENT_TO_PARENT;
            }
            ComponentName forwardingActivityComponentName = new ComponentName(androidApplication().packageName, className);
            ActivityInfo forwardingActivityInfo = getActivityInfo(forwardingActivityComponentName, 0L, sourceUserId);
            if (!targetIsProfile) {
                forwardingActivityInfo.showUserIcon = targetUserId;
                forwardingResolveInfo.noResourceId = true;
            }
            forwardingResolveInfo.activityInfo = forwardingActivityInfo;
            forwardingResolveInfo.priority = 0;
            forwardingResolveInfo.preferredOrder = 0;
            forwardingResolveInfo.match = 0;
            forwardingResolveInfo.isDefault = true;
            forwardingResolveInfo.filter = new IntentFilter(filter.getIntentFilter());
            forwardingResolveInfo.targetUserId = targetUserId;
            return forwardingResolveInfo;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private CrossProfileDomainInfo queryCrossProfileIntents(List<CrossProfileIntentFilter> matchingFilters, Intent intent, String resolvedType, long flags, int sourceUserId, boolean matchInCurrentProfile) {
        if (matchingFilters == null) {
            return null;
        }
        SparseBooleanArray alreadyTriedUserIds = new SparseBooleanArray();
        CrossProfileDomainInfo resultInfo = null;
        int size = matchingFilters.size();
        int i = 0;
        while (true) {
            if (i >= size) {
                break;
            }
            CrossProfileIntentFilter filter = matchingFilters.get(i);
            int targetUserId = filter.getTargetUserId();
            boolean skipCurrentProfile = (filter.getFlags() & 2) != 0;
            boolean skipCurrentProfileIfNoMatchFound = (filter.getFlags() & 4) != 0;
            if (!skipCurrentProfile && !alreadyTriedUserIds.get(targetUserId) && (!skipCurrentProfileIfNoMatchFound || !matchInCurrentProfile)) {
                CrossProfileDomainInfo info = createForwardingResolveInfo(filter, intent, resolvedType, flags, sourceUserId);
                if (info != null) {
                    resultInfo = info;
                    break;
                }
                alreadyTriedUserIds.put(targetUserId, true);
            }
            i++;
        }
        if (resultInfo != null) {
            ResolveInfo forwardingResolveInfo = resultInfo.mResolveInfo;
            if (isUserEnabled(forwardingResolveInfo.targetUserId)) {
                List<ResolveInfo> filteredResult = filterIfNotSystemUser(Collections.singletonList(forwardingResolveInfo), sourceUserId);
                if (filteredResult.isEmpty()) {
                    return null;
                }
                return resultInfo;
            }
            return null;
        }
        return null;
    }

    private ResolveInfo querySkipCurrentProfileIntents(List<CrossProfileIntentFilter> matchingFilters, Intent intent, String resolvedType, long flags, int sourceUserId) {
        CrossProfileDomainInfo info;
        if (matchingFilters != null) {
            int size = matchingFilters.size();
            for (int i = 0; i < size; i++) {
                CrossProfileIntentFilter filter = matchingFilters.get(i);
                if ((filter.getFlags() & 2) != 0 && (info = createForwardingResolveInfo(filter, intent, resolvedType, flags, sourceUserId)) != null) {
                    return info.mResolveInfo;
                }
            }
            return null;
        }
        return null;
    }

    @Override // com.android.server.pm.Computer
    public final ServiceInfo getServiceInfo(ComponentName component, long flags, int userId) {
        if (this.mUserManager.exists(userId)) {
            int callingUid = Binder.getCallingUid();
            long flags2 = updateFlagsForComponent(flags, userId);
            enforceCrossUserOrProfilePermission(callingUid, userId, false, false, "get service info");
            return getServiceInfoBody(component, flags2, userId, callingUid);
        }
        return null;
    }

    protected ServiceInfo getServiceInfoBody(ComponentName component, long flags, int userId, int callingUid) {
        long flags2;
        PackageStateInternal ps;
        ParsedService s = this.mComponentResolver.getService(component);
        if (PackageManagerService.DEBUG_PACKAGE_INFO) {
            Log.v("PackageManager", "getServiceInfo " + component + ": " + s);
        }
        if (s == null) {
            return null;
        }
        if (!this.mUserManager.isDualProfile(UserHandle.getUserId(Binder.getCallingUid()))) {
            flags2 = flags;
        } else {
            flags2 = flags | 4202496;
        }
        AndroidPackage pkg = this.mPackages.get(s.getPackageName());
        if (!this.mSettings.isEnabledAndMatch(pkg, s, flags2, userId) || (ps = this.mSettings.getPackage(component.getPackageName())) == null || shouldFilterApplication(ps, callingUid, component, 3, userId)) {
            return null;
        }
        return PackageInfoUtils.generateServiceInfo(pkg, s, flags2, ps.getUserStateOrDefault(userId), userId, ps);
    }

    @Override // com.android.server.pm.Computer
    public final SharedLibraryInfo getSharedLibraryInfo(String name, long version) {
        return this.mSharedLibraries.getSharedLibraryInfo(name, version);
    }

    @Override // com.android.server.pm.Computer
    public String getInstantAppPackageName(int callingUid) {
        if (Process.isIsolated(callingUid)) {
            callingUid = getIsolatedOwner(callingUid);
        }
        int appId = UserHandle.getAppId(callingUid);
        Object obj = this.mSettings.getSettingBase(appId);
        if (obj instanceof PackageStateInternal) {
            PackageStateInternal ps = (PackageStateInternal) obj;
            boolean isInstantApp = ps.getUserStateOrDefault(UserHandle.getUserId(callingUid)).isInstantApp();
            if (isInstantApp) {
                return ps.getPkg().getPackageName();
            }
            return null;
        }
        return null;
    }

    private int getIsolatedOwner(int isolatedUid) {
        int ownerUid = this.mIsolatedOwners.get(isolatedUid, -1);
        if (ownerUid == -1) {
            throw new IllegalStateException("No owner UID found for isolated UID " + isolatedUid);
        }
        return ownerUid;
    }

    @Override // com.android.server.pm.Computer
    public final String resolveExternalPackageName(AndroidPackage pkg) {
        if (pkg.getStaticSharedLibName() != null) {
            return pkg.getManifestPackageName();
        }
        return pkg.getPackageName();
    }

    private String resolveInternalPackageNameInternalLocked(String packageName, long versionCode, int callingUid) {
        String normalizedPackageName = this.mSettings.getRenamedPackageLPr(packageName);
        String packageName2 = normalizedPackageName != null ? normalizedPackageName : packageName;
        WatchedLongSparseArray<SharedLibraryInfo> versionedLib = this.mSharedLibraries.getStaticLibraryInfos(packageName2);
        if (versionedLib != null && versionedLib.size() > 0) {
            LongSparseLongArray versionsCallerCanSee = null;
            int callingAppId = UserHandle.getAppId(callingUid);
            if (callingAppId != 1000 && callingAppId != 2000 && callingAppId != 0) {
                versionsCallerCanSee = new LongSparseLongArray();
                String libName = versionedLib.valueAt(0).getName();
                String[] uidPackages = getPackagesForUidInternal(callingUid, callingUid);
                if (uidPackages != null) {
                    for (String uidPackage : uidPackages) {
                        PackageStateInternal ps = this.mSettings.getPackage(uidPackage);
                        int libIdx = ArrayUtils.indexOf(ps.getUsesStaticLibraries(), libName);
                        if (libIdx >= 0) {
                            long libVersion = ps.getUsesStaticLibrariesVersions()[libIdx];
                            versionsCallerCanSee.append(libVersion, libVersion);
                        }
                    }
                }
            }
            if (versionsCallerCanSee != null && versionsCallerCanSee.size() <= 0) {
                return packageName2;
            }
            SharedLibraryInfo highestVersion = null;
            int versionCount = versionedLib.size();
            for (int i = 0; i < versionCount; i++) {
                SharedLibraryInfo libraryInfo = versionedLib.valueAt(i);
                if (versionsCallerCanSee == null || versionsCallerCanSee.indexOfKey(libraryInfo.getLongVersion()) >= 0) {
                    long libVersionCode = libraryInfo.getDeclaringPackage().getLongVersionCode();
                    if (versionCode != -1) {
                        if (libVersionCode == versionCode) {
                            return libraryInfo.getPackageName();
                        }
                    } else if (highestVersion == null) {
                        highestVersion = libraryInfo;
                    } else if (libVersionCode > highestVersion.getDeclaringPackage().getLongVersionCode()) {
                        highestVersion = libraryInfo;
                    }
                }
            }
            if (highestVersion != null) {
                return highestVersion.getPackageName();
            }
            return packageName2;
        }
        return packageName2;
    }

    @Override // com.android.server.pm.Computer
    public final String resolveInternalPackageName(String packageName, long versionCode) {
        int callingUid = Binder.getCallingUid();
        return resolveInternalPackageNameInternalLocked(packageName, versionCode, callingUid);
    }

    @Override // com.android.server.pm.Computer
    public final String[] getPackagesForUid(int uid) {
        return getPackagesForUidInternal(uid, Binder.getCallingUid());
    }

    private String[] getPackagesForUidInternal(int uid, int callingUid) {
        boolean isCallerInstantApp = getInstantAppPackageName(callingUid) != null;
        int userId = UserHandle.getUserId(uid);
        if (Process.isSdkSandboxUid(uid)) {
            uid = getBaseSdkSandboxUid();
        }
        int appId = UserHandle.getAppId(uid);
        return getPackagesForUidInternalBody(callingUid, userId, appId, isCallerInstantApp);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public String[] getPackagesForUidInternalBody(int callingUid, int userId, int appId, boolean isCallerInstantApp) {
        Object obj = this.mSettings.getSettingBase(appId);
        if (obj instanceof SharedUserSetting) {
            if (isCallerInstantApp) {
                return null;
            }
            SharedUserSetting sus = (SharedUserSetting) obj;
            ArraySet<? extends PackageStateInternal> packageStates = sus.getPackageStates();
            int n = packageStates.size();
            String[] res = new String[n];
            int i = 0;
            for (int index = 0; index < n; index++) {
                PackageStateInternal ps = packageStates.valueAt(index);
                if (ps.getUserStateOrDefault(userId).isInstalled() && !shouldFilterApplication(ps, callingUid, userId)) {
                    res[i] = ps.getPackageName();
                    i++;
                }
            }
            return (String[]) ArrayUtils.trimToSize(res, i);
        }
        if (obj instanceof PackageStateInternal) {
            PackageStateInternal ps2 = (PackageStateInternal) obj;
            if (ps2.getUserStateOrDefault(userId).isInstalled() && !shouldFilterApplication(ps2, callingUid, userId)) {
                return new String[]{ps2.getPackageName()};
            }
        }
        return null;
    }

    @Override // com.android.server.pm.Computer
    public final UserInfo getProfileParent(int userId) {
        long identity = Binder.clearCallingIdentity();
        try {
            return this.mUserManager.getProfileParent(userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private boolean areWebInstantAppsDisabled(int userId) {
        return this.mWebInstantAppsDisabled.get(userId);
    }

    @Override // com.android.server.pm.Computer
    public final boolean canViewInstantApps(int callingUid, int userId) {
        if (callingUid < 10000 || this.mContext.checkCallingOrSelfPermission("android.permission.ACCESS_INSTANT_APPS") == 0) {
            return true;
        }
        if (this.mContext.checkCallingOrSelfPermission("android.permission.VIEW_INSTANT_APPS") == 0) {
            ComponentName homeComponent = getDefaultHomeActivity(userId);
            if (homeComponent != null && isCallerSameApp(homeComponent.getPackageName(), callingUid)) {
                return true;
            }
            String str = this.mAppPredictionServicePackage;
            return str != null && isCallerSameApp(str, callingUid);
        }
        return false;
    }

    private boolean filterStaticSharedLibPackage(PackageStateInternal ps, int uid, int userId, long flags) {
        SharedLibraryInfo libraryInfo;
        int index;
        if ((flags & 67108864) != 0) {
            int appId = UserHandle.getAppId(uid);
            if (appId != 1000 && appId != 2000) {
                if (appId == 0 || checkUidPermission("android.permission.INSTALL_PACKAGES", uid) == 0) {
                    return false;
                }
            }
            return false;
        }
        if (ps != null && ps.getPkg() != null) {
            if (ps.getPkg().isStaticSharedLibrary() && (libraryInfo = getSharedLibraryInfo(ps.getPkg().getStaticSharedLibName(), ps.getPkg().getStaticSharedLibVersion())) != null) {
                int resolvedUid = UserHandle.getUid(userId, UserHandle.getAppId(uid));
                String[] uidPackageNames = getPackagesForUid(resolvedUid);
                if (uidPackageNames == null) {
                    return true;
                }
                for (String uidPackageName : uidPackageNames) {
                    if (ps.getPackageName().equals(uidPackageName)) {
                        return false;
                    }
                    PackageStateInternal uidPs = this.mSettings.getPackage(uidPackageName);
                    if (uidPs != null && (index = ArrayUtils.indexOf(uidPs.getUsesStaticLibraries(), libraryInfo.getName())) >= 0 && uidPs.getPkg().getUsesStaticLibrariesVersions()[index] == libraryInfo.getLongVersion()) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean filterSdkLibPackage(PackageStateInternal ps, int uid, int userId, long flags) {
        SharedLibraryInfo libraryInfo;
        int index;
        if ((flags & 67108864) != 0) {
            int appId = UserHandle.getAppId(uid);
            if (appId != 1000 && appId != 2000) {
                if (appId == 0 || checkUidPermission("android.permission.INSTALL_PACKAGES", uid) == 0) {
                    return false;
                }
            }
            return false;
        }
        if (ps != null && ps.getPkg() != null) {
            if (ps.getPkg().isSdkLibrary() && (libraryInfo = getSharedLibraryInfo(ps.getPkg().getSdkLibName(), ps.getPkg().getSdkLibVersionMajor())) != null) {
                int resolvedUid = UserHandle.getUid(userId, UserHandle.getAppId(uid));
                String[] uidPackageNames = getPackagesForUid(resolvedUid);
                if (uidPackageNames == null) {
                    return true;
                }
                for (String uidPackageName : uidPackageNames) {
                    if (ps.getPackageName().equals(uidPackageName)) {
                        return false;
                    }
                    PackageStateInternal uidPs = this.mSettings.getPackage(uidPackageName);
                    if (uidPs != null && (index = ArrayUtils.indexOf(uidPs.getUsesSdkLibraries(), libraryInfo.getName())) >= 0 && uidPs.getPkg().getUsesSdkLibrariesVersionsMajor()[index] == libraryInfo.getLongVersion()) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
        return false;
    }

    @Override // com.android.server.pm.Computer
    public final boolean filterSharedLibPackage(PackageStateInternal ps, int uid, int userId, long flags) {
        return filterStaticSharedLibPackage(ps, uid, userId, flags) || filterSdkLibPackage(ps, uid, userId, flags);
    }

    private boolean hasCrossUserPermission(int callingUid, int callingUserId, int userId, boolean requireFullPermission, boolean requirePermissionWhenSameUser) {
        if (!requirePermissionWhenSameUser && userId == callingUserId) {
            return true;
        }
        if ((!requirePermissionWhenSameUser && this.mInjector.getUserManagerInternal().isDualProfile(callingUserId)) || callingUid == 1000 || callingUid == 0) {
            return true;
        }
        if (requireFullPermission) {
            return hasPermission("android.permission.INTERACT_ACROSS_USERS_FULL");
        }
        return hasPermission("android.permission.INTERACT_ACROSS_USERS_FULL") || hasPermission("android.permission.INTERACT_ACROSS_USERS");
    }

    private boolean hasNonNegativePriority(List<ResolveInfo> resolveInfos) {
        return resolveInfos.size() > 0 && resolveInfos.get(0).priority >= 0;
    }

    private boolean hasPermission(String permission) {
        return this.mContext.checkCallingOrSelfPermission(permission) == 0;
    }

    @Override // com.android.server.pm.Computer
    public final boolean isCallerSameApp(String packageName, int uid) {
        if (Process.isSdkSandboxUid(uid)) {
            return packageName != null && packageName.equals(this.mService.getSdkSandboxPackageName());
        }
        AndroidPackage pkg = this.mPackages.get(packageName);
        return pkg != null && UserHandle.getAppId(uid) == pkg.getUid();
    }

    @Override // com.android.server.pm.Computer
    public final boolean isComponentVisibleToInstantApp(ComponentName component) {
        if (isComponentVisibleToInstantApp(component, 1) || isComponentVisibleToInstantApp(component, 3)) {
            return true;
        }
        return isComponentVisibleToInstantApp(component, 4);
    }

    @Override // com.android.server.pm.Computer
    public final boolean isComponentVisibleToInstantApp(ComponentName component, int type) {
        if (type == 1) {
            ParsedActivity activity = this.mComponentResolver.getActivity(component);
            if (activity == null) {
                return false;
            }
            boolean visibleToInstantApp = (1048576 & activity.getFlags()) != 0;
            boolean explicitlyVisibleToInstantApp = (2097152 & activity.getFlags()) == 0;
            return visibleToInstantApp && explicitlyVisibleToInstantApp;
        } else if (type == 2) {
            ParsedActivity activity2 = this.mComponentResolver.getReceiver(component);
            if (activity2 == null) {
                return false;
            }
            boolean visibleToInstantApp2 = (1048576 & activity2.getFlags()) != 0;
            boolean explicitlyVisibleToInstantApp2 = (2097152 & activity2.getFlags()) == 0;
            return visibleToInstantApp2 && !explicitlyVisibleToInstantApp2;
        } else if (type == 3) {
            ParsedService service = this.mComponentResolver.getService(component);
            return (service == null || (1048576 & service.getFlags()) == 0) ? false : true;
        } else if (type == 4) {
            ParsedProvider provider = this.mComponentResolver.getProvider(component);
            return (provider == null || (1048576 & provider.getFlags()) == 0) ? false : true;
        } else if (type == 0) {
            return isComponentVisibleToInstantApp(component);
        } else {
            return false;
        }
    }

    @Override // com.android.server.pm.Computer
    public final boolean isImplicitImageCaptureIntentAndNotSetByDpc(Intent intent, int userId, String resolvedType, long flags) {
        return intent.isImplicitImageCaptureIntent() && !isPersistentPreferredActivitySetByDpm(intent, userId, resolvedType, flags);
    }

    @Override // com.android.server.pm.Computer
    public final boolean isInstantApp(String packageName, int userId) {
        int callingUid = Binder.getCallingUid();
        enforceCrossUserPermission(callingUid, userId, true, false, "isInstantApp");
        return isInstantAppInternal(packageName, userId, callingUid);
    }

    @Override // com.android.server.pm.Computer
    public final boolean isInstantAppInternal(String packageName, int userId, int callingUid) {
        return isInstantAppInternalBody(packageName, userId, callingUid);
    }

    protected boolean isInstantAppInternalBody(String packageName, int userId, int callingUid) {
        if (Process.isIsolated(callingUid)) {
            callingUid = getIsolatedOwner(callingUid);
        }
        PackageStateInternal ps = this.mSettings.getPackage(packageName);
        boolean returnAllowed = ps != null && (isCallerSameApp(packageName, callingUid) || canViewInstantApps(callingUid, userId) || this.mInstantAppRegistry.isInstantAccessGranted(userId, UserHandle.getAppId(callingUid), ps.getAppId()));
        if (!returnAllowed) {
            return false;
        }
        return ps.getUserStateOrDefault(userId).isInstantApp();
    }

    private boolean isInstantAppResolutionAllowed(Intent intent, List<ResolveInfo> resolvedActivities, int userId, boolean skipPackageCheck, long flags) {
        if (this.mInstantAppResolverConnection != null && instantAppInstallerActivity() != null && intent.getComponent() == null && (intent.getFlags() & 512) == 0) {
            if (skipPackageCheck || intent.getPackage() == null) {
                if (!intent.isWebIntent()) {
                    if ((resolvedActivities != null && resolvedActivities.size() != 0) || (intent.getFlags() & 2048) == 0) {
                        return false;
                    }
                } else if (intent.getData() == null || TextUtils.isEmpty(intent.getData().getHost()) || areWebInstantAppsDisabled(userId)) {
                    return false;
                }
                return isInstantAppResolutionAllowedBody(intent, resolvedActivities, userId, skipPackageCheck, flags);
            }
            return false;
        }
        return false;
    }

    protected boolean isInstantAppResolutionAllowedBody(Intent intent, List<ResolveInfo> resolvedActivities, int userId, boolean skipPackageCheck, long flags) {
        int count = resolvedActivities == null ? 0 : resolvedActivities.size();
        for (int n = 0; n < count; n++) {
            ResolveInfo info = resolvedActivities.get(n);
            String packageName = info.activityInfo.packageName;
            PackageStateInternal ps = this.mSettings.getPackage(packageName);
            if (ps != null) {
                if (!info.handleAllWebDataURI && PackageManagerServiceUtils.hasAnyDomainApproval(this.mDomainVerificationManager, ps, intent, flags, userId)) {
                    if (PackageManagerService.DEBUG_INSTANT) {
                        Slog.v("PackageManager", "DENY instant app; pkg: " + packageName + ", approved");
                    }
                    return false;
                } else if (ps.getUserStateOrDefault(userId).isInstantApp()) {
                    if (PackageManagerService.DEBUG_INSTANT) {
                        Slog.v("PackageManager", "DENY instant app installed; pkg: " + packageName);
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isPersistentPreferredActivitySetByDpm(Intent intent, int userId, String resolvedType, long flags) {
        List<PersistentPreferredActivity> arrayList;
        PersistentPreferredIntentResolver ppir = this.mSettings.getPersistentPreferredActivities(userId);
        if (ppir != null) {
            arrayList = ppir.queryIntent(this, intent, resolvedType, (65536 & flags) != 0, userId);
        } else {
            arrayList = new ArrayList();
        }
        for (PersistentPreferredActivity ppa : arrayList) {
            if (ppa.mIsSetByDpm) {
                return true;
            }
        }
        return false;
    }

    private boolean isRecentsAccessingChildProfiles(int callingUid, int targetUserId) {
        if (((ActivityTaskManagerInternal) this.mInjector.getLocalService(ActivityTaskManagerInternal.class)).isCallerRecents(callingUid)) {
            long token = Binder.clearCallingIdentity();
            try {
                int callingUserId = UserHandle.getUserId(callingUid);
                if (ActivityManager.getCurrentUser() != callingUserId) {
                    return false;
                }
                return this.mUserManager.isSameProfileGroup(callingUserId, targetUserId);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
        return false;
    }

    @Override // com.android.server.pm.Computer
    public final boolean isSameProfileGroup(int callerUserId, int userId) {
        long identity = Binder.clearCallingIdentity();
        try {
            return UserManagerService.getInstance().isSameProfileGroup(callerUserId, userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private boolean isUserEnabled(int userId) {
        boolean z;
        long callingId = Binder.clearCallingIdentity();
        try {
            UserInfo userInfo = this.mUserManager.getUserInfo(userId);
            if (userInfo != null) {
                if (userInfo.isEnabled()) {
                    z = true;
                    return z;
                }
            }
            z = false;
            return z;
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    @Override // com.android.server.pm.Computer
    public final boolean shouldFilterApplication(PackageStateInternal ps, int callingUid, ComponentName component, int componentType, int userId) {
        if (Process.isSdkSandboxUid(callingUid)) {
            int clientAppUid = Process.getAppUidForSdkSandboxUid(callingUid);
            if (ps != null && clientAppUid == UserHandle.getUid(userId, ps.getAppId())) {
                return false;
            }
        }
        if (Process.isIsolated(callingUid)) {
            callingUid = getIsolatedOwner(callingUid);
        }
        String instantAppPkgName = getInstantAppPackageName(callingUid);
        boolean callerIsInstantApp = instantAppPkgName != null;
        if (ps == null) {
            return callerIsInstantApp || Process.isSdkSandboxUid(callingUid);
        } else if (isCallerSameApp(ps.getPackageName(), callingUid)) {
            return false;
        } else {
            if (callerIsInstantApp) {
                if (ps.getUserStateOrDefault(userId).isInstantApp()) {
                    return true;
                }
                if (component == null) {
                    return !ps.getPkg().isVisibleToInstantApps();
                }
                ParsedInstrumentation instrumentation = this.mInstrumentation.get(component);
                if (instrumentation == null || !isCallerSameApp(instrumentation.getTargetPackage(), callingUid)) {
                    return !isComponentVisibleToInstantApp(component, componentType);
                }
                return false;
            } else if (ps.getUserStateOrDefault(userId).isInstantApp()) {
                if (canViewInstantApps(callingUid, userId)) {
                    return false;
                }
                if (component != null) {
                    return true;
                }
                return !this.mInstantAppRegistry.isInstantAccessGranted(userId, UserHandle.getAppId(callingUid), ps.getAppId());
            } else {
                int appId = UserHandle.getAppId(callingUid);
                SettingBase callingPs = this.mSettings.getSettingBase(appId);
                return this.mAppsFilter.shouldFilterApplication(this, callingUid, callingPs, ps, userId);
            }
        }
    }

    @Override // com.android.server.pm.Computer
    public final boolean shouldFilterApplication(PackageStateInternal ps, int callingUid, int userId) {
        return shouldFilterApplication(ps, callingUid, null, 0, userId);
    }

    @Override // com.android.server.pm.Computer
    public final boolean shouldFilterApplication(SharedUserSetting sus, int callingUid, int userId) {
        boolean filterApp = true;
        ArraySet<? extends PackageStateInternal> packageStates = sus.getPackageStates();
        for (int index = packageStates.size() - 1; index >= 0 && filterApp; index--) {
            filterApp &= shouldFilterApplication(packageStates.valueAt(index), callingUid, null, 0, userId);
        }
        return filterApp;
    }

    private int bestDomainVerificationStatus(int status1, int status2) {
        if (status1 == 3) {
            return status2;
        }
        if (status2 == 3) {
            return status1;
        }
        return (int) MathUtils.max(status1, status2);
    }

    @Override // com.android.server.pm.Computer
    public final int checkUidPermission(String permName, int uid) {
        if (PackageManagerService.isAppopsState()) {
            return 0;
        }
        if ("android.permission.MANAGE_DOCUMENTS".equals(permName) && this.mUserManager.isDualProfile(UserHandle.getUserId(uid))) {
            return 0;
        }
        return this.mPermissionManager.checkUidPermission(uid, permName);
    }

    @Override // com.android.server.pm.Computer
    public int getPackageUidInternal(String packageName, long flags, int userId, int callingUid) {
        PackageStateInternal ps;
        PackageStateInternal ps2;
        AndroidPackage p = this.mPackages.get(packageName);
        if (p != null && AndroidPackageUtils.isMatchForSystemOnly(p, flags) && (ps2 = getPackageStateInternal(p.getPackageName(), callingUid)) != null && ps2.getUserStateOrDefault(userId).isInstalled() && !shouldFilterApplication(ps2, callingUid, userId)) {
            return UserHandle.getUid(userId, p.getUid());
        }
        if ((4202496 & flags) != 0 && (ps = this.mSettings.getPackage(packageName)) != null && PackageStateUtils.isMatch(ps, flags) && !shouldFilterApplication(ps, callingUid, userId)) {
            return UserHandle.getUid(userId, ps.getAppId());
        }
        return -1;
    }

    private long updateFlags(long flags, int userId) {
        if ((flags & 786432) == 0) {
            UserManagerInternal umInternal = this.mInjector.getUserManagerInternal();
            if (umInternal.isUserUnlockingOrUnlocked(userId)) {
                return flags | 786432;
            }
            return flags | 524288;
        }
        return flags;
    }

    @Override // com.android.server.pm.Computer
    public final long updateFlagsForApplication(long flags, int userId) {
        return updateFlagsForPackage(flags, userId);
    }

    @Override // com.android.server.pm.Computer
    public final long updateFlagsForComponent(long flags, int userId) {
        return updateFlags(flags, userId);
    }

    @Override // com.android.server.pm.Computer
    public final long updateFlagsForPackage(long flags, int userId) {
        long flags2;
        boolean isCallerSystemUser = UserHandle.getCallingUserId() == 0;
        if ((flags & 4194304) != 0) {
            enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, !isRecentsAccessingChildProfiles(Binder.getCallingUid(), userId), "MATCH_ANY_USER flag requires INTERACT_ACROSS_USERS permission");
        } else if ((8192 & flags) != 0 && isCallerSystemUser && this.mUserManager.hasProfile(0)) {
            flags2 = flags | 4194304;
            return updateFlags(flags2, userId);
        }
        flags2 = flags;
        return updateFlags(flags2, userId);
    }

    @Override // com.android.server.pm.Computer
    public final long updateFlagsForResolve(long flags, int userId, int callingUid, boolean wantInstantApps, boolean isImplicitImageCaptureIntentAndNotSetByDpc) {
        return updateFlagsForResolve(flags, userId, callingUid, wantInstantApps, false, isImplicitImageCaptureIntentAndNotSetByDpc);
    }

    @Override // com.android.server.pm.Computer
    public final long updateFlagsForResolve(long flags, int userId, int callingUid, boolean wantInstantApps, boolean onlyExposedExplicitly, boolean isImplicitImageCaptureIntentAndNotSetByDpc) {
        long flags2;
        if (safeMode() || isImplicitImageCaptureIntentAndNotSetByDpc) {
            flags |= 1048576;
        }
        if (getInstantAppPackageName(callingUid) != null) {
            if (onlyExposedExplicitly) {
                flags |= 33554432;
            }
            flags2 = flags | 16777216 | 8388608;
        } else {
            boolean allowMatchInstant = true;
            boolean wantMatchInstant = (flags & 8388608) != 0;
            if (!wantInstantApps && (!wantMatchInstant || !canViewInstantApps(callingUid, userId))) {
                allowMatchInstant = false;
            }
            flags2 = flags & (-50331649);
            if (!allowMatchInstant) {
                flags2 &= -8388609;
            }
        }
        return updateFlagsForComponent(flags2, userId);
    }

    @Override // com.android.server.pm.Computer
    public final void enforceCrossUserOrProfilePermission(int callingUid, int userId, boolean requireFullPermission, boolean checkShell, String message) {
        if (userId < 0) {
            throw new IllegalArgumentException("Invalid userId " + userId);
        }
        if (checkShell) {
            PackageManagerServiceUtils.enforceShellRestriction(this.mInjector.getUserManagerInternal(), "no_debugging_features", callingUid, userId);
        }
        int callingUserId = UserHandle.getUserId(callingUid);
        if (hasCrossUserPermission(callingUid, callingUserId, userId, requireFullPermission, false)) {
            return;
        }
        boolean isSameProfileGroup = isSameProfileGroup(callingUserId, userId);
        if (isSameProfileGroup && PermissionChecker.checkPermissionForPreflight(this.mContext, "android.permission.INTERACT_ACROSS_PROFILES", -1, callingUid, getPackage(callingUid).getPackageName()) == 0) {
            return;
        }
        String errorMessage = buildInvalidCrossUserOrProfilePermissionMessage(callingUid, userId, message, requireFullPermission, isSameProfileGroup);
        Slog.w("PackageManager", errorMessage);
        throw new SecurityException(errorMessage);
    }

    private static String buildInvalidCrossUserOrProfilePermissionMessage(int callingUid, int userId, String message, boolean requireFullPermission, boolean isSameProfileGroup) {
        StringBuilder builder = new StringBuilder();
        if (message != null) {
            builder.append(message);
            builder.append(": ");
        }
        builder.append("UID ");
        builder.append(callingUid);
        builder.append(" requires ");
        builder.append("android.permission.INTERACT_ACROSS_USERS_FULL");
        if (!requireFullPermission) {
            builder.append(" or ");
            builder.append("android.permission.INTERACT_ACROSS_USERS");
            if (isSameProfileGroup) {
                builder.append(" or ");
                builder.append("android.permission.INTERACT_ACROSS_PROFILES");
            }
        }
        builder.append(" to access user ");
        builder.append(userId);
        builder.append(".");
        return builder.toString();
    }

    @Override // com.android.server.pm.Computer
    public final void enforceCrossUserPermission(int callingUid, int userId, boolean requireFullPermission, boolean checkShell, String message) {
        enforceCrossUserPermission(callingUid, userId, requireFullPermission, checkShell, false, message);
    }

    @Override // com.android.server.pm.Computer
    public final void enforceCrossUserPermission(int callingUid, int userId, boolean requireFullPermission, boolean checkShell, boolean requirePermissionWhenSameUser, String message) {
        if (userId < 0) {
            throw new IllegalArgumentException("Invalid userId " + userId);
        }
        if (checkShell) {
            PackageManagerServiceUtils.enforceShellRestriction(this.mInjector.getUserManagerInternal(), "no_debugging_features", callingUid, userId);
        }
        int callingUserId = UserHandle.getUserId(callingUid);
        if (hasCrossUserPermission(callingUid, callingUserId, userId, requireFullPermission, requirePermissionWhenSameUser)) {
            return;
        }
        String errorMessage = buildInvalidCrossUserPermissionMessage(callingUid, userId, message, requireFullPermission);
        Slog.w("PackageManager", errorMessage);
        throw new SecurityException(errorMessage);
    }

    private static String buildInvalidCrossUserPermissionMessage(int callingUid, int userId, String message, boolean requireFullPermission) {
        StringBuilder builder = new StringBuilder();
        if (message != null) {
            builder.append(message);
            builder.append(": ");
        }
        builder.append("UID ");
        builder.append(callingUid);
        builder.append(" requires ");
        builder.append("android.permission.INTERACT_ACROSS_USERS_FULL");
        if (!requireFullPermission) {
            builder.append(" or ");
            builder.append("android.permission.INTERACT_ACROSS_USERS");
        }
        builder.append(" to access user ");
        builder.append(userId);
        builder.append(".");
        return builder.toString();
    }

    @Override // com.android.server.pm.Computer
    public SigningDetails getSigningDetails(String packageName) {
        AndroidPackage p = this.mPackages.get(packageName);
        if (p == null) {
            return null;
        }
        return p.getSigningDetails();
    }

    @Override // com.android.server.pm.Computer
    public SigningDetails getSigningDetails(int uid) {
        int appId = UserHandle.getAppId(uid);
        Object obj = this.mSettings.getSettingBase(appId);
        if (obj != null) {
            if (obj instanceof SharedUserSetting) {
                return ((SharedUserSetting) obj).signatures.mSigningDetails;
            }
            if (obj instanceof PackageStateInternal) {
                PackageStateInternal ps = (PackageStateInternal) obj;
                return ps.getSigningDetails();
            }
        }
        return SigningDetails.UNKNOWN;
    }

    @Override // com.android.server.pm.Computer
    public boolean filterAppAccess(AndroidPackage pkg, int callingUid, int userId) {
        PackageStateInternal ps = getPackageStateInternal(pkg.getPackageName());
        return shouldFilterApplication(ps, callingUid, userId);
    }

    @Override // com.android.server.pm.Computer
    public boolean filterAppAccess(String packageName, int callingUid, int userId) {
        PackageStateInternal ps = getPackageStateInternal(packageName);
        return shouldFilterApplication(ps, callingUid, userId);
    }

    @Override // com.android.server.pm.Computer
    public boolean filterAppAccess(int uid, int callingUid) {
        if (Process.isSdkSandboxUid(uid)) {
            if (callingUid == uid) {
                return false;
            }
            int clientAppUid = Process.getAppUidForSdkSandboxUid(uid);
            return clientAppUid != uid;
        }
        int userId = UserHandle.getUserId(uid);
        int appId = UserHandle.getAppId(uid);
        Object setting = this.mSettings.getSettingBase(appId);
        if (setting instanceof SharedUserSetting) {
            return shouldFilterApplication((SharedUserSetting) setting, callingUid, userId);
        }
        if (setting == null || (setting instanceof PackageStateInternal)) {
            return shouldFilterApplication((PackageStateInternal) setting, callingUid, userId);
        }
        return false;
    }

    @Override // com.android.server.pm.Computer
    public void dump(int type, FileDescriptor fd, PrintWriter pw, DumpState dumpState) {
        Collection<? extends PackageStateInternal> pkgSettings;
        Collection<? extends PackageStateInternal> pkgSettings2;
        String packageName = dumpState.getTargetPackageName();
        PackageStateInternal setting = this.mSettings.getPackage(packageName);
        dumpState.isCheckIn();
        if (packageName == null || setting != null) {
            switch (type) {
                case 1:
                    this.mSharedLibraries.dump(pw, dumpState);
                    return;
                case 512:
                    this.mSettings.dumpReadMessages(pw, dumpState);
                    return;
                case 4096:
                    this.mSettings.dumpPreferred(pw, dumpState, packageName);
                    return;
                case 8192:
                    pw.flush();
                    FileOutputStream fout = new FileOutputStream(fd);
                    BufferedOutputStream str = new BufferedOutputStream(fout);
                    TypedXmlSerializer serializer = Xml.newFastSerializer();
                    try {
                        serializer.setOutput(str, StandardCharsets.UTF_8.name());
                        serializer.startDocument((String) null, true);
                        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                        this.mSettings.writePreferredActivitiesLPr(serializer, 0, dumpState.isFullPreferred());
                        serializer.endDocument();
                        serializer.flush();
                        return;
                    } catch (IOException e) {
                        pw.println("Failed writing: " + e);
                        return;
                    } catch (IllegalArgumentException e2) {
                        pw.println("Failed writing: " + e2);
                        return;
                    } catch (IllegalStateException e3) {
                        pw.println("Failed writing: " + e3);
                        return;
                    }
                case 32768:
                    if (dumpState.onTitlePrinted()) {
                        pw.println();
                    }
                    pw.println("Database versions:");
                    this.mSettings.dumpVersionLPr(new IndentingPrintWriter(pw, "  "));
                    return;
                case 262144:
                    android.util.IndentingPrintWriter writer = new android.util.IndentingPrintWriter(pw);
                    if (dumpState.onTitlePrinted()) {
                        pw.println();
                    }
                    writer.println("Domain verification status:");
                    writer.increaseIndent();
                    try {
                        this.mDomainVerificationManager.printState(this, writer, packageName, -1);
                    } catch (Exception e4) {
                        pw.println("Failure printing domain verification information");
                        Slog.e("PackageManager", "Failure printing domain verification information", e4);
                    }
                    writer.decreaseIndent();
                    return;
                case 524288:
                    if (dumpState.onTitlePrinted()) {
                        pw.println();
                    }
                    IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ", 120);
                    ipw.println();
                    ipw.println("Frozen packages:");
                    ipw.increaseIndent();
                    if (this.mFrozenPackages.size() == 0) {
                        ipw.println("(none)");
                    } else {
                        for (int i = 0; i < this.mFrozenPackages.size(); i++) {
                            ipw.print("package=");
                            ipw.print(this.mFrozenPackages.keyAt(i));
                            ipw.print(", refCounts=");
                            ipw.println(this.mFrozenPackages.valueAt(i));
                        }
                    }
                    ipw.decreaseIndent();
                    return;
                case 1048576:
                    IndentingPrintWriter ipw2 = new IndentingPrintWriter(pw, "  ");
                    if (dumpState.onTitlePrinted()) {
                        pw.println();
                    }
                    ipw2.println("Dexopt state:");
                    ipw2.increaseIndent();
                    if (setting != null) {
                        pkgSettings = Collections.singletonList(setting);
                    } else {
                        pkgSettings = this.mSettings.getPackages().values();
                    }
                    for (PackageStateInternal pkgSetting : pkgSettings) {
                        AndroidPackage pkg = pkgSetting.getPkg();
                        if (pkg != null) {
                            String pkgName = pkg.getPackageName();
                            ipw2.println("[" + pkgName + "]");
                            ipw2.increaseIndent();
                            this.mPackageDexOptimizer.dumpDexoptState(ipw2, pkg, pkgSetting, this.mDexManager.getPackageUseInfoOrDefault(pkgName));
                            ipw2.decreaseIndent();
                        }
                    }
                    ipw2.println("BgDexopt state:");
                    ipw2.increaseIndent();
                    this.mBackgroundDexOptService.dump(ipw2);
                    ipw2.decreaseIndent();
                    return;
                case 2097152:
                    IndentingPrintWriter ipw3 = new IndentingPrintWriter(pw, "  ");
                    if (dumpState.onTitlePrinted()) {
                        pw.println();
                    }
                    ipw3.println("Compiler stats:");
                    ipw3.increaseIndent();
                    if (setting != null) {
                        pkgSettings2 = Collections.singletonList(setting);
                    } else {
                        pkgSettings2 = this.mSettings.getPackages().values();
                    }
                    for (PackageStateInternal pkgSetting2 : pkgSettings2) {
                        AndroidPackage pkg2 = pkgSetting2.getPkg();
                        if (pkg2 != null) {
                            String pkgName2 = pkg2.getPackageName();
                            ipw3.println("[" + pkgName2 + "]");
                            ipw3.increaseIndent();
                            CompilerStats.PackageStats stats = this.mCompilerStats.getPackageStats(pkgName2);
                            if (stats == null) {
                                ipw3.println("(No recorded stats)");
                            } else {
                                stats.dump(ipw3);
                            }
                            ipw3.decreaseIndent();
                        }
                    }
                    return;
                case 67108864:
                    Integer filteringAppId = setting != null ? Integer.valueOf(setting.getAppId()) : null;
                    this.mAppsFilter.dumpQueries(pw, filteringAppId, dumpState, this.mUserManager.getUserIds(), new QuadFunction() { // from class: com.android.server.pm.ComputerEngine$$ExternalSyntheticLambda1
                        public final Object apply(Object obj, Object obj2, Object obj3, Object obj4) {
                            return ComputerEngine.this.getPackagesForUidInternalBody(((Integer) obj).intValue(), ((Integer) obj2).intValue(), ((Integer) obj3).intValue(), ((Boolean) obj4).booleanValue());
                        }
                    });
                    return;
                default:
                    return;
            }
        }
    }

    protected PackageManagerService.FindPreferredActivityBodyResult findPreferredActivityBody(Intent intent, String resolvedType, long flags, List<ResolveInfo> query, boolean always, boolean removeMatches, boolean debug, int userId, boolean queryMayBeFiltered, int callingUid, boolean isDeviceProvisioned) {
        List list;
        int i;
        long flags2;
        int match;
        int n;
        boolean z;
        PackageManagerService.FindPreferredActivityBodyResult result = new PackageManagerService.FindPreferredActivityBodyResult();
        long flags3 = updateFlagsForResolve(flags, userId, callingUid, false, isImplicitImageCaptureIntentAndNotSetByDpc(intent, userId, resolvedType, flags));
        Intent intent2 = PackageManagerServiceUtils.updateIntentForResolve(intent);
        result.mPreferredResolveInfo = findPersistentPreferredActivity(intent2, resolvedType, flags3, query, debug, userId);
        if (result.mPreferredResolveInfo != null) {
            return result;
        }
        PreferredIntentResolver pir = this.mSettings.getPreferredActivities(userId);
        if (PackageManagerService.DEBUG_PREFERRED || debug) {
            Slog.v("PackageManager", "Looking for preferred activities...");
        }
        if (pir != null) {
            list = pir.queryIntent(this, intent2, resolvedType, (65536 & flags3) != 0, userId);
        } else {
            list = null;
        }
        List list2 = list;
        if (list2 != null && list2.size() > 0) {
            int match2 = 0;
            if (PackageManagerService.DEBUG_PREFERRED || debug) {
                Slog.v("PackageManager", "Figuring out best match...");
            }
            int n2 = query.size();
            for (int j = 0; j < n2; j++) {
                ResolveInfo ri = query.get(j);
                if (PackageManagerService.DEBUG_PREFERRED || debug) {
                    Slog.v("PackageManager", "Match for " + ri.activityInfo + ": 0x" + Integer.toHexString(ri.match));
                }
                if (ri.match > match2) {
                    match2 = ri.match;
                }
            }
            if (PackageManagerService.DEBUG_PREFERRED || debug) {
                Slog.v("PackageManager", "Best match: 0x" + Integer.toHexString(match2));
            }
            int match3 = match2 & 268369920;
            int m = list2.size();
            int i2 = 0;
            while (i2 < m) {
                PreferredActivity pa = (PreferredActivity) list2.get(i2);
                List list3 = list2;
                int m2 = m;
                if (PackageManagerService.DEBUG_PREFERRED || debug) {
                    Slog.v("PackageManager", "Checking PreferredActivity ds=" + (pa.countDataSchemes() > 0 ? pa.getDataScheme(0) : "<none>") + "\n  component=" + pa.mPref.mComponent);
                    i = i2;
                    pa.dump(new LogPrinter(2, "PackageManager", 3), "  ");
                } else {
                    i = i2;
                }
                if (pa.mPref.mMatch != match3) {
                    if (PackageManagerService.DEBUG_PREFERRED || debug) {
                        Slog.v("PackageManager", "Skipping bad match " + Integer.toHexString(pa.mPref.mMatch));
                        match = match3;
                        n = n2;
                        flags2 = flags3;
                    } else {
                        match = match3;
                        n = n2;
                        flags2 = flags3;
                    }
                } else if (!always || pa.mPref.mAlways) {
                    flags2 = flags3;
                    ActivityInfo ai = getActivityInfo(pa.mPref.mComponent, flags3 | 512 | 524288 | 262144, userId);
                    if (PackageManagerService.DEBUG_PREFERRED || debug) {
                        Slog.v("PackageManager", "Found preferred activity:");
                        if (ai != null) {
                            ai.dump(new LogPrinter(2, "PackageManager", 3), "  ");
                        } else {
                            Slog.v("PackageManager", "  null");
                        }
                    }
                    boolean excludeSetupWizardHomeActivity = isHomeIntent(intent2) && !isDeviceProvisioned;
                    boolean allowSetMutation = (excludeSetupWizardHomeActivity || queryMayBeFiltered) ? false : true;
                    if (ai == null) {
                        if (allowSetMutation) {
                            Slog.w("PackageManager", "Removing dangling preferred activity: " + pa.mPref.mComponent);
                            pir.removeFilter((PreferredIntentResolver) pa);
                            result.mChanged = true;
                            match = match3;
                            n = n2;
                        } else {
                            match = match3;
                            n = n2;
                        }
                    } else {
                        int j2 = 0;
                        while (true) {
                            if (j2 >= n2) {
                                match = match3;
                                n = n2;
                                break;
                            }
                            ResolveInfo ri2 = query.get(j2);
                            match = match3;
                            if (!ri2.activityInfo.applicationInfo.packageName.equals(ai.applicationInfo.packageName) || !ri2.activityInfo.name.equals(ai.name)) {
                                j2++;
                                match3 = match;
                            } else if (removeMatches && allowSetMutation) {
                                pir.removeFilter((PreferredIntentResolver) pa);
                                result.mChanged = true;
                                if (PackageManagerService.DEBUG_PREFERRED) {
                                    Slog.v("PackageManager", "Removing match " + pa.mPref.mComponent);
                                    n = n2;
                                } else {
                                    n = n2;
                                }
                            } else {
                                if (always && !pa.mPref.sameSet(query, excludeSetupWizardHomeActivity, userId)) {
                                    if (pa.mPref.isSuperset(query, excludeSetupWizardHomeActivity)) {
                                        if (allowSetMutation) {
                                            if (PackageManagerService.DEBUG_PREFERRED) {
                                                z = true;
                                                Slog.i("PackageManager", "Result set changed, but PreferredActivity is still valid as only non-preferred components were removed for " + intent2 + " type " + resolvedType);
                                            } else {
                                                z = true;
                                            }
                                            if (intent2.getAction() != null && "android.intent.action.WEB_SEARCH".equals(intent2.getAction()) && ("com.google.android.googlequicksearchbox".equals(pa.mPref.mComponent.getPackageName()) || "com.google.android.apps.searchlite".equals(pa.mPref.mComponent.getPackageName()))) {
                                                result.mPreferredResolveInfo = ri2;
                                                return result;
                                            }
                                            PreferredActivity freshPa = new PreferredActivity(pa, pa.mPref.mMatch, pa.mPref.discardObsoleteComponents(query), pa.mPref.mComponent, pa.mPref.mAlways);
                                            pir.removeFilter((PreferredIntentResolver) pa);
                                            pir.addFilter((PackageDataSnapshot) this, (ComputerEngine) freshPa);
                                            result.mChanged = z;
                                        } else if (PackageManagerService.DEBUG_PREFERRED) {
                                            Slog.i("PackageManager", "Do not remove preferred activity");
                                        }
                                    } else {
                                        if (allowSetMutation) {
                                            Slog.i("PackageManager", "Result set changed, dropping preferred activity for " + intent2 + " type " + resolvedType);
                                            if (PackageManagerService.DEBUG_PREFERRED) {
                                                Slog.v("PackageManager", "Removing preferred activity since set changed " + pa.mPref.mComponent);
                                            }
                                            if (intent2.getAction() != null && "android.intent.action.VIEW".equals(intent2.getAction()) && resolvedType != null && resolvedType.startsWith("image/")) {
                                                PreferredActivity freshPa2 = new PreferredActivity((WatchedIntentFilter) pa, pa.mPref.mMatch, pa.mPref.discardObsoleteComponents(query), pa.mPref.mComponent, true);
                                                pir.removeFilter((PreferredIntentResolver) pa);
                                                pir.addFilter((PackageDataSnapshot) this, (ComputerEngine) freshPa2);
                                                result.mChanged = true;
                                                result.mPreferredResolveInfo = ri2;
                                                return result;
                                            } else if (intent2.getAction() != null && "android.intent.action.VIEW".equals(intent2.getAction()) && resolvedType != null && resolvedType.startsWith("audio/")) {
                                                PreferredActivity freshPa3 = new PreferredActivity((WatchedIntentFilter) pa, pa.mPref.mMatch, pa.mPref.discardObsoleteComponents(query), pa.mPref.mComponent, true);
                                                pir.removeFilter((PreferredIntentResolver) pa);
                                                pir.addFilter((PackageDataSnapshot) this, (ComputerEngine) freshPa3);
                                                result.mChanged = true;
                                                result.mPreferredResolveInfo = ri2;
                                                return result;
                                            } else if (intent2.getAction() != null && "android.intent.action.WEB_SEARCH".equals(intent2.getAction()) && ("com.google.android.googlequicksearchbox".equals(pa.mPref.mComponent.getPackageName()) || "com.google.android.apps.searchlite".equals(pa.mPref.mComponent.getPackageName()))) {
                                                result.mPreferredResolveInfo = ri2;
                                                return result;
                                            } else {
                                                pir.removeFilter((PreferredIntentResolver) pa);
                                                PreferredActivity lastChosen = new PreferredActivity((WatchedIntentFilter) pa, pa.mPref.mMatch, (ComponentName[]) null, pa.mPref.mComponent, false);
                                                pir.addFilter((PackageDataSnapshot) this, (ComputerEngine) lastChosen);
                                                result.mChanged = true;
                                            }
                                        }
                                        result.mPreferredResolveInfo = null;
                                        return result;
                                    }
                                }
                                if (PackageManagerService.DEBUG_PREFERRED || debug) {
                                    Slog.v("PackageManager", "Returning preferred activity: " + ri2.activityInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + ri2.activityInfo.name);
                                }
                                result.mPreferredResolveInfo = ri2;
                                return result;
                            }
                        }
                    }
                } else {
                    if (PackageManagerService.DEBUG_PREFERRED || debug) {
                        Slog.v("PackageManager", "Skipping mAlways=false entry");
                    }
                    match = match3;
                    n = n2;
                    flags2 = flags3;
                }
                i2 = i + 1;
                list2 = list3;
                m = m2;
                match3 = match;
                flags3 = flags2;
                n2 = n;
            }
        }
        return result;
    }

    private static boolean isHomeIntent(Intent intent) {
        return "android.intent.action.MAIN".equals(intent.getAction()) && intent.hasCategory("android.intent.category.HOME") && intent.hasCategory("android.intent.category.DEFAULT");
    }

    @Override // com.android.server.pm.Computer
    public final PackageManagerService.FindPreferredActivityBodyResult findPreferredActivityInternal(Intent intent, String resolvedType, long flags, List<ResolveInfo> query, boolean always, boolean removeMatches, boolean debug, int userId, boolean queryMayBeFiltered) {
        int callingUid = Binder.getCallingUid();
        boolean isDeviceProvisioned = Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) == 1;
        return findPreferredActivityBody(intent, resolvedType, flags, query, always, removeMatches, debug, userId, queryMayBeFiltered, callingUid, isDeviceProvisioned);
    }

    @Override // com.android.server.pm.Computer
    public final ResolveInfo findPersistentPreferredActivity(Intent intent, String resolvedType, long flags, List<ResolveInfo> query, boolean debug, int userId) {
        List list;
        int n = query.size();
        PersistentPreferredIntentResolver ppir = this.mSettings.getPersistentPreferredActivities(userId);
        if (PackageManagerService.DEBUG_PREFERRED || debug) {
            Slog.v("PackageManager", "Looking for persistent preferred activities...");
        }
        int i = 0;
        if (ppir != null) {
            list = ppir.queryIntent(this, intent, resolvedType, (flags & 65536) != 0, userId);
        } else {
            list = null;
        }
        if (list != null && list.size() > 0) {
            int m = list.size();
            int i2 = 0;
            while (i2 < m) {
                PersistentPreferredActivity ppa = (PersistentPreferredActivity) list.get(i2);
                if (PackageManagerService.DEBUG_PREFERRED || debug) {
                    Slog.v("PackageManager", "Checking PersistentPreferredActivity ds=" + (ppa.countDataSchemes() > 0 ? ppa.getDataScheme(i) : "<none>") + "\n  component=" + ppa.mComponent);
                    ppa.dump(new LogPrinter(2, "PackageManager", 3), "  ");
                }
                ActivityInfo ai = getActivityInfo(ppa.mComponent, flags | 512, userId);
                if (PackageManagerService.DEBUG_PREFERRED || debug) {
                    Slog.v("PackageManager", "Found persistent preferred activity:");
                    if (ai != null) {
                        ai.dump(new LogPrinter(2, "PackageManager", 3), "  ");
                    } else {
                        Slog.v("PackageManager", "  null");
                    }
                }
                if (ai != null) {
                    for (int j = 0; j < n; j++) {
                        ResolveInfo ri = query.get(j);
                        if (ri.activityInfo.applicationInfo.packageName.equals(ai.applicationInfo.packageName) && ri.activityInfo.name.equals(ai.name)) {
                            if (PackageManagerService.DEBUG_PREFERRED || debug) {
                                Slog.v("PackageManager", "Returning persistent preferred activity: " + ri.activityInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + ri.activityInfo.name);
                            }
                            return ri;
                        }
                    }
                    continue;
                }
                i2++;
                i = 0;
            }
            return null;
        }
        return null;
    }

    @Override // com.android.server.pm.Computer
    public PreferredIntentResolver getPreferredActivities(int userId) {
        return this.mSettings.getPreferredActivities(userId);
    }

    @Override // com.android.server.pm.Computer, com.android.server.pm.snapshot.PackageDataSnapshot
    public ArrayMap<String, ? extends PackageStateInternal> getPackageStates() {
        return this.mSettings.getPackages();
    }

    @Override // com.android.server.pm.Computer
    public String getRenamedPackage(String packageName) {
        return this.mSettings.getRenamedPackageLPr(packageName);
    }

    @Override // com.android.server.pm.Computer
    public WatchedArrayMap<String, WatchedLongSparseArray<SharedLibraryInfo>> getSharedLibraries() {
        return this.mSharedLibraries.getAll();
    }

    @Override // com.android.server.pm.Computer
    public ArraySet<String> getNotifyPackagesForReplacedReceived(String[] packages) {
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getUserId(callingUid);
        ArraySet<String> packagesToNotify = new ArraySet<>();
        for (String packageName : packages) {
            PackageStateInternal packageState = getPackageStateInternal(packageName);
            if (!shouldFilterApplication(packageState, callingUid, callingUserId)) {
                packagesToNotify.add(packageName);
            }
        }
        return packagesToNotify;
    }

    @Override // com.android.server.pm.Computer
    public int getPackageStartability(boolean safeMode, String packageName, int callingUid, int userId) {
        boolean userKeyUnlocked = StorageManager.isUserKeyUnlocked(userId);
        PackageStateInternal ps = getPackageStateInternal(packageName);
        if (ps == null || shouldFilterApplication(ps, callingUid, userId) || !ps.getUserStateOrDefault(userId).isInstalled()) {
            return 1;
        }
        if (safeMode && !ps.isSystem()) {
            return 2;
        }
        if (this.mFrozenPackages.containsKey(packageName)) {
            return 3;
        }
        if (!userKeyUnlocked && !AndroidPackageUtils.isEncryptionAware(ps.getPkg())) {
            return 4;
        }
        return 0;
    }

    @Override // com.android.server.pm.Computer
    public boolean isPackageAvailable(String packageName, int userId) {
        PackageUserStateInternal state;
        if (this.mUserManager.exists(userId)) {
            int callingUid = Binder.getCallingUid();
            enforceCrossUserPermission(callingUid, userId, false, false, "is package available");
            PackageStateInternal ps = getPackageStateInternal(packageName);
            if (ps == null || ps.getPkg() == null || shouldFilterApplication(ps, callingUid, userId) || (state = ps.getUserStateOrDefault(userId)) == null) {
                return false;
            }
            if (this.mUserManager.isDualProfile(UserHandle.getUserId(callingUid))) {
                return true;
            }
            return PackageUserStateUtils.isAvailable(state, 0L);
        }
        return false;
    }

    @Override // com.android.server.pm.Computer
    public String[] currentToCanonicalPackageNames(String[] names) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return names;
        }
        String[] out = new String[names.length];
        int callingUserId = UserHandle.getUserId(callingUid);
        boolean canViewInstantApps = canViewInstantApps(callingUid, callingUserId);
        for (int i = names.length - 1; i >= 0; i--) {
            PackageStateInternal ps = getPackageStateInternal(names[i]);
            boolean translateName = false;
            if (ps != null && ps.getRealName() != null) {
                boolean targetIsInstantApp = ps.getUserStateOrDefault(callingUserId).isInstantApp();
                translateName = !targetIsInstantApp || canViewInstantApps || this.mInstantAppRegistry.isInstantAccessGranted(callingUserId, UserHandle.getAppId(callingUid), ps.getAppId());
            }
            out[i] = translateName ? ps.getRealName() : names[i];
        }
        return out;
    }

    @Override // com.android.server.pm.Computer
    public String[] canonicalToCurrentPackageNames(String[] names) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return names;
        }
        String[] out = new String[names.length];
        int callingUserId = UserHandle.getUserId(callingUid);
        boolean canViewInstantApps = canViewInstantApps(callingUid, callingUserId);
        for (int i = names.length - 1; i >= 0; i--) {
            String cur = getRenamedPackage(names[i]);
            boolean translateName = false;
            if (cur != null) {
                PackageStateInternal ps = getPackageStateInternal(names[i]);
                boolean z = false;
                boolean targetIsInstantApp = ps != null && ps.getUserStateOrDefault(callingUserId).isInstantApp();
                translateName = (!targetIsInstantApp || canViewInstantApps || this.mInstantAppRegistry.isInstantAccessGranted(callingUserId, UserHandle.getAppId(callingUid), ps.getAppId())) ? true : true;
            }
            out[i] = translateName ? cur : names[i];
        }
        return out;
    }

    @Override // com.android.server.pm.Computer
    public int[] getPackageGids(String packageName, long flags, int userId) {
        if (this.mUserManager.exists(userId)) {
            int callingUid = Binder.getCallingUid();
            long flags2 = updateFlagsForPackage(flags, userId);
            enforceCrossUserPermission(callingUid, userId, false, false, "getPackageGids");
            PackageStateInternal ps = getPackageStateInternal(packageName);
            if (ps == null) {
                return null;
            }
            if (ps.getPkg() != null && AndroidPackageUtils.isMatchForSystemOnly(ps.getPkg(), flags2) && ps.getUserStateOrDefault(userId).isInstalled() && !shouldFilterApplication(ps, callingUid, userId)) {
                return this.mPermissionManager.getGidsForUid(UserHandle.getUid(userId, ps.getAppId()));
            }
            if ((4202496 & flags2) == 0 || !PackageStateUtils.isMatch(ps, flags2) || shouldFilterApplication(ps, callingUid, userId)) {
                return null;
            }
            return this.mPermissionManager.getGidsForUid(UserHandle.getUid(userId, ps.getAppId()));
        }
        return null;
    }

    @Override // com.android.server.pm.Computer
    public int getTargetSdkVersion(String packageName) {
        PackageStateInternal ps = getPackageStateInternal(packageName);
        if (ps == null || ps.getPkg() == null || shouldFilterApplication(ps, Binder.getCallingUid(), UserHandle.getCallingUserId())) {
            return -1;
        }
        return ps.getPkg().getTargetSdkVersion();
    }

    @Override // com.android.server.pm.Computer
    public boolean activitySupportsIntent(ComponentName resolveComponentName, ComponentName component, Intent intent, String resolvedType) {
        PackageStateInternal ps;
        if (component.equals(resolveComponentName)) {
            return true;
        }
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getUserId(callingUid);
        ParsedActivity a = this.mComponentResolver.getActivity(component);
        if (a == null || (ps = getPackageStateInternal(component.getPackageName())) == null || shouldFilterApplication(ps, callingUid, component, 1, callingUserId)) {
            return false;
        }
        for (int i = 0; i < a.getIntents().size(); i++) {
            if (a.getIntents().get(i).getIntentFilter().match(intent.getAction(), resolvedType, intent.getScheme(), intent.getData(), intent.getCategories(), "PackageManager") >= 0) {
                return true;
            }
        }
        return false;
    }

    @Override // com.android.server.pm.Computer
    public ActivityInfo getReceiverInfo(ComponentName component, long flags, int userId) {
        PackageStateInternal ps;
        if (this.mUserManager.exists(userId)) {
            int callingUid = Binder.getCallingUid();
            long flags2 = updateFlagsForComponent(flags, userId);
            enforceCrossUserPermission(callingUid, userId, false, false, "get receiver info");
            ParsedActivity a = this.mComponentResolver.getReceiver(component);
            if (PackageManagerService.DEBUG_PACKAGE_INFO) {
                Log.v("PackageManager", "getReceiverInfo " + component + ": " + a);
            }
            if (a == null || (ps = getPackageStateInternal(a.getPackageName())) == null || ps.getPkg() == null || !PackageStateUtils.isEnabledAndMatches(ps, a, flags2, userId) || shouldFilterApplication(ps, callingUid, component, 2, userId)) {
                return null;
            }
            return PackageInfoUtils.generateActivityInfo(ps.getPkg(), a, flags2, ps.getUserStateOrDefault(userId), userId, ps);
        }
        return null;
    }

    /* JADX WARN: Code restructure failed: missing block: B:17:0x0054, code lost:
        if (r40.mContext.checkCallingOrSelfPermission("android.permission.ACCESS_SHARED_LIBRARIES") != 0) goto L18;
     */
    @Override // com.android.server.pm.Computer
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public ParceledListSlice<SharedLibraryInfo> getSharedLibraries(String packageName, long flags, int userId) {
        int i;
        int libCount;
        WatchedArrayMap<String, WatchedLongSparseArray<SharedLibraryInfo>> sharedLibraries;
        int versionCount;
        int j;
        int i2;
        WatchedLongSparseArray<SharedLibraryInfo> versionedLib;
        int libCount2;
        WatchedArrayMap<String, WatchedLongSparseArray<SharedLibraryInfo>> sharedLibraries2;
        ArrayList arrayList;
        List<SharedLibraryInfo> result;
        if (this.mUserManager.exists(userId)) {
            Preconditions.checkArgumentNonnegative(userId, "userId must be >= 0");
            int callingUid = Binder.getCallingUid();
            if (getInstantAppPackageName(callingUid) != null) {
                return null;
            }
            long flags2 = updateFlagsForPackage(flags, userId);
            boolean z = false;
            if (this.mContext.checkCallingOrSelfPermission("android.permission.INSTALL_PACKAGES") != 0) {
                if (this.mContext.checkCallingOrSelfPermission("android.permission.DELETE_PACKAGES") != 0) {
                    if (!canRequestPackageInstalls(packageName, callingUid, userId, false)) {
                        if (this.mContext.checkCallingOrSelfPermission("android.permission.REQUEST_DELETE_PACKAGES") != 0) {
                        }
                    }
                }
            }
            z = true;
            boolean canSeeStaticAndSdkLibraries = z;
            WatchedArrayMap<String, WatchedLongSparseArray<SharedLibraryInfo>> sharedLibraries3 = getSharedLibraries();
            List<SharedLibraryInfo> result2 = null;
            int libCount3 = sharedLibraries3.size();
            int i3 = 0;
            while (i3 < libCount3) {
                WatchedLongSparseArray<SharedLibraryInfo> versionedLib2 = sharedLibraries3.valueAt(i3);
                if (versionedLib2 == null) {
                    i = i3;
                    libCount = libCount3;
                    sharedLibraries = sharedLibraries3;
                } else {
                    int versionCount2 = versionedLib2.size();
                    List<SharedLibraryInfo> result3 = result2;
                    int j2 = 0;
                    while (true) {
                        if (j2 >= versionCount2) {
                            i = i3;
                            libCount = libCount3;
                            sharedLibraries = sharedLibraries3;
                            break;
                        }
                        SharedLibraryInfo libInfo = versionedLib2.valueAt(j2);
                        if (!canSeeStaticAndSdkLibraries) {
                            if (!libInfo.isStatic()) {
                                if (libInfo.isSdk()) {
                                    i = i3;
                                    libCount = libCount3;
                                    sharedLibraries = sharedLibraries3;
                                    break;
                                }
                            } else {
                                i = i3;
                                libCount = libCount3;
                                sharedLibraries = sharedLibraries3;
                                break;
                            }
                        }
                        long identity = Binder.clearCallingIdentity();
                        VersionedPackage declaringPackage = libInfo.getDeclaringPackage();
                        try {
                            versionCount = versionCount2;
                            j = j2;
                            i2 = i3;
                            versionedLib = versionedLib2;
                            libCount2 = libCount3;
                            sharedLibraries2 = sharedLibraries3;
                        } catch (Throwable th) {
                            th = th;
                        }
                        try {
                            PackageInfo packageInfo = getPackageInfoInternal(declaringPackage.getPackageName(), declaringPackage.getLongVersionCode(), flags2 | 67108864, Binder.getCallingUid(), userId);
                            if (packageInfo == null) {
                                Binder.restoreCallingIdentity(identity);
                            } else {
                                Binder.restoreCallingIdentity(identity);
                                String path = libInfo.getPath();
                                String packageName2 = libInfo.getPackageName();
                                List allCodePaths = libInfo.getAllCodePaths();
                                String name = libInfo.getName();
                                long longVersion = libInfo.getLongVersion();
                                int type = libInfo.getType();
                                List<VersionedPackage> packagesUsingSharedLibrary = getPackagesUsingSharedLibrary(libInfo, flags2, callingUid, userId);
                                if (libInfo.getDependencies() == null) {
                                    arrayList = null;
                                } else {
                                    arrayList = new ArrayList(libInfo.getDependencies());
                                }
                                SharedLibraryInfo resLibInfo = new SharedLibraryInfo(path, packageName2, allCodePaths, name, longVersion, type, declaringPackage, packagesUsingSharedLibrary, arrayList, libInfo.isNative());
                                if (result3 != null) {
                                    result = result3;
                                } else {
                                    result = new ArrayList<>();
                                }
                                result.add(resLibInfo);
                                result3 = result;
                            }
                            j2 = j + 1;
                            versionCount2 = versionCount;
                            i3 = i2;
                            versionedLib2 = versionedLib;
                            libCount3 = libCount2;
                            sharedLibraries3 = sharedLibraries2;
                        } catch (Throwable th2) {
                            th = th2;
                            Binder.restoreCallingIdentity(identity);
                            throw th;
                        }
                    }
                    result2 = result3;
                }
                i3 = i + 1;
                libCount3 = libCount;
                sharedLibraries3 = sharedLibraries;
            }
            if (result2 != null) {
                return new ParceledListSlice<>(result2);
            }
            return null;
        }
        return null;
    }

    @Override // com.android.server.pm.Computer
    public boolean canRequestPackageInstalls(String packageName, int callingUid, int userId, boolean throwIfPermNotDeclared) {
        AndroidPackage pkg;
        int uid = getPackageUidInternal(packageName, 0L, userId, callingUid);
        if (callingUid != uid && callingUid != 0 && callingUid != 1000) {
            throw new SecurityException("Caller uid " + callingUid + " does not own package " + packageName);
        }
        if (isInstantAppInternal(packageName, userId, 1000) || (pkg = this.mPackages.get(packageName)) == null || pkg.getTargetSdkVersion() < 26) {
            return false;
        }
        if (pkg.getRequestedPermissions().contains("android.permission.REQUEST_INSTALL_PACKAGES")) {
            return !isInstallDisabledForPackage(packageName, uid, userId);
        }
        if (!throwIfPermNotDeclared) {
            Slog.e("PackageManager", "Need to declare android.permission.REQUEST_INSTALL_PACKAGES to call this api");
            return false;
        }
        throw new SecurityException("Need to declare android.permission.REQUEST_INSTALL_PACKAGES to call this api");
    }

    @Override // com.android.server.pm.Computer
    public final boolean isInstallDisabledForPackage(String packageName, int uid, int userId) {
        if (this.mUserManager.hasUserRestriction("no_install_unknown_sources", userId) || this.mUserManager.hasUserRestriction("no_install_unknown_sources_globally", userId)) {
            return true;
        }
        PackageManagerInternal.ExternalSourcesPolicy externalSourcesPolicy = this.mExternalSourcesPolicy;
        if (externalSourcesPolicy != null) {
            int isTrusted = externalSourcesPolicy.getPackageTrustedToInstallApps(packageName, uid);
            return isTrusted != 0;
        }
        return false;
    }

    @Override // com.android.server.pm.Computer
    public List<VersionedPackage> getPackagesUsingSharedLibrary(SharedLibraryInfo libInfo, long flags, int callingUid, int userId) {
        ComputerEngine computerEngine = this;
        int i = callingUid;
        List<VersionedPackage> versionedPackages = null;
        ArrayMap<String, ? extends PackageStateInternal> packageStates = getPackageStates();
        int packageCount = packageStates.size();
        int i2 = 0;
        while (i2 < packageCount) {
            PackageStateInternal ps = packageStates.valueAt(i2);
            if (ps != null && PackageUserStateUtils.isAvailable(ps.getUserStateOrDefault(userId), flags)) {
                String libName = libInfo.getName();
                if (libInfo.isStatic() || libInfo.isSdk()) {
                    String[] libs = libInfo.isStatic() ? ps.getUsesStaticLibraries() : ps.getUsesSdkLibraries();
                    long[] libsVersions = libInfo.isStatic() ? ps.getUsesStaticLibrariesVersions() : ps.getUsesSdkLibrariesVersionsMajor();
                    int libIdx = ArrayUtils.indexOf(libs, libName);
                    if (libIdx >= 0 && libsVersions[libIdx] == libInfo.getLongVersion() && !computerEngine.shouldFilterApplication(ps, i, userId)) {
                        if (versionedPackages == null) {
                            versionedPackages = new ArrayList<>();
                        }
                        String dependentPackageName = ps.getPackageName();
                        if (ps.getPkg() != null && ps.getPkg().isStaticSharedLibrary()) {
                            dependentPackageName = ps.getPkg().getManifestPackageName();
                        }
                        versionedPackages.add(new VersionedPackage(dependentPackageName, ps.getVersionCode()));
                    }
                } else if (ps.getPkg() != null && ((ArrayUtils.contains(ps.getPkg().getUsesLibraries(), libName) || ArrayUtils.contains(ps.getPkg().getUsesOptionalLibraries(), libName)) && !computerEngine.shouldFilterApplication(ps, i, userId))) {
                    if (versionedPackages == null) {
                        versionedPackages = new ArrayList<>();
                    }
                    versionedPackages.add(new VersionedPackage(ps.getPackageName(), ps.getVersionCode()));
                }
            }
            i2++;
            computerEngine = this;
            i = callingUid;
        }
        return versionedPackages;
    }

    @Override // com.android.server.pm.Computer
    public ParceledListSlice<SharedLibraryInfo> getDeclaredSharedLibraries(String packageName, long flags, int userId) {
        int i;
        int versionCount;
        int j;
        WatchedLongSparseArray<SharedLibraryInfo> versionedLibrary;
        int i2;
        List<SharedLibraryInfo> result;
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_SHARED_LIBRARIES", "getDeclaredSharedLibraries");
        int callingUid = Binder.getCallingUid();
        enforceCrossUserPermission(callingUid, userId, true, false, "getDeclaredSharedLibraries");
        Preconditions.checkNotNull(packageName, "packageName cannot be null");
        Preconditions.checkArgumentNonnegative(userId, "userId must be >= 0");
        if (this.mUserManager.exists(userId) && getInstantAppPackageName(callingUid) == null) {
            WatchedArrayMap<String, WatchedLongSparseArray<SharedLibraryInfo>> sharedLibraries = getSharedLibraries();
            List<SharedLibraryInfo> result2 = null;
            int libraryCount = sharedLibraries.size();
            int i3 = 0;
            while (i3 < libraryCount) {
                WatchedLongSparseArray<SharedLibraryInfo> versionedLibrary2 = sharedLibraries.valueAt(i3);
                if (versionedLibrary2 == null) {
                    i = i3;
                } else {
                    int versionCount2 = versionedLibrary2.size();
                    List<SharedLibraryInfo> result3 = result2;
                    int j2 = 0;
                    while (j2 < versionCount2) {
                        SharedLibraryInfo libraryInfo = versionedLibrary2.valueAt(j2);
                        VersionedPackage declaringPackage = libraryInfo.getDeclaringPackage();
                        if (!Objects.equals(declaringPackage.getPackageName(), packageName)) {
                            versionCount = versionCount2;
                            j = j2;
                            versionedLibrary = versionedLibrary2;
                            i2 = i3;
                        } else {
                            long identity = Binder.clearCallingIdentity();
                            try {
                                versionCount = versionCount2;
                                j = j2;
                                versionedLibrary = versionedLibrary2;
                                i2 = i3;
                                try {
                                    PackageInfo packageInfo = getPackageInfoInternal(declaringPackage.getPackageName(), declaringPackage.getLongVersionCode(), flags | 67108864, Binder.getCallingUid(), userId);
                                    if (packageInfo == null) {
                                        Binder.restoreCallingIdentity(identity);
                                    } else {
                                        Binder.restoreCallingIdentity(identity);
                                        SharedLibraryInfo resultLibraryInfo = new SharedLibraryInfo(libraryInfo.getPath(), libraryInfo.getPackageName(), libraryInfo.getAllCodePaths(), libraryInfo.getName(), libraryInfo.getLongVersion(), libraryInfo.getType(), libraryInfo.getDeclaringPackage(), getPackagesUsingSharedLibrary(libraryInfo, flags, callingUid, userId), libraryInfo.getDependencies() == null ? null : new ArrayList(libraryInfo.getDependencies()), libraryInfo.isNative());
                                        if (result3 != null) {
                                            result = result3;
                                        } else {
                                            result = new ArrayList<>();
                                        }
                                        result.add(resultLibraryInfo);
                                        result3 = result;
                                    }
                                } catch (Throwable th) {
                                    th = th;
                                    Binder.restoreCallingIdentity(identity);
                                    throw th;
                                }
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        }
                        j2 = j + 1;
                        versionedLibrary2 = versionedLibrary;
                        i3 = i2;
                        versionCount2 = versionCount;
                    }
                    i = i3;
                    result2 = result3;
                }
                i3 = i + 1;
            }
            if (result2 != null) {
                return new ParceledListSlice<>(result2);
            }
            return null;
        }
        return null;
    }

    @Override // com.android.server.pm.Computer
    public ProviderInfo getProviderInfo(ComponentName component, long flags, int userId) {
        PackageStateInternal ps;
        PackageUserStateInternal state;
        ApplicationInfo appInfo;
        if (this.mUserManager.exists(userId)) {
            int callingUid = Binder.getCallingUid();
            long flags2 = updateFlagsForComponent(flags, userId);
            enforceCrossUserPermission(callingUid, userId, false, false, "get provider info");
            ParsedProvider p = this.mComponentResolver.getProvider(component);
            if (PackageManagerService.DEBUG_PACKAGE_INFO) {
                Log.v("PackageManager", "getProviderInfo " + component + ": " + p);
            }
            if (p == null || (ps = getPackageStateInternal(p.getPackageName())) == null || ps.getPkg() == null || !PackageStateUtils.isEnabledAndMatches(ps, p, flags2, userId) || shouldFilterApplication(ps, callingUid, component, 4, userId) || (appInfo = PackageInfoUtils.generateApplicationInfo(ps.getPkg(), flags2, (state = ps.getUserStateOrDefault(userId)), userId, ps)) == null) {
                return null;
            }
            return PackageInfoUtils.generateProviderInfo(ps.getPkg(), p, flags2, state, appInfo, userId, ps);
        }
        return null;
    }

    @Override // com.android.server.pm.Computer
    public String[] getSystemSharedLibraryNames() {
        WatchedArrayMap<String, WatchedLongSparseArray<SharedLibraryInfo>> sharedLibraries = getSharedLibraries();
        Set<String> libs = null;
        int libCount = sharedLibraries.size();
        for (int i = 0; i < libCount; i++) {
            WatchedLongSparseArray<SharedLibraryInfo> versionedLib = sharedLibraries.valueAt(i);
            if (versionedLib != null) {
                int versionCount = versionedLib.size();
                int j = 0;
                while (true) {
                    if (j < versionCount) {
                        SharedLibraryInfo libraryInfo = versionedLib.valueAt(j);
                        if (!libraryInfo.isStatic()) {
                            if (libs == null) {
                                libs = new ArraySet<>();
                            }
                            libs.add(libraryInfo.getName());
                        } else {
                            PackageStateInternal ps = getPackageStateInternal(libraryInfo.getPackageName());
                            if (ps == null || filterSharedLibPackage(ps, Binder.getCallingUid(), UserHandle.getUserId(Binder.getCallingUid()), 67108864L)) {
                                j++;
                            } else {
                                if (libs == null) {
                                    libs = new ArraySet<>();
                                }
                                libs.add(libraryInfo.getName());
                            }
                        }
                    }
                }
            }
        }
        if (libs != null) {
            String[] libsArray = new String[libs.size()];
            libs.toArray(libsArray);
            return libsArray;
        }
        return null;
    }

    @Override // com.android.server.pm.Computer
    public PackageStateInternal getPackageStateFiltered(String packageName, int callingUid, int userId) {
        PackageStateInternal packageState = getPackageStateInternal(packageName);
        if (packageState == null || shouldFilterApplication(packageState, callingUid, userId)) {
            return null;
        }
        return packageState;
    }

    @Override // com.android.server.pm.Computer
    public int checkSignatures(String pkg1, String pkg2) {
        AndroidPackage p1 = this.mPackages.get(pkg1);
        AndroidPackage p2 = this.mPackages.get(pkg2);
        PackageStateInternal ps1 = p1 == null ? null : getPackageStateInternal(p1.getPackageName());
        PackageStateInternal ps2 = p2 != null ? getPackageStateInternal(p2.getPackageName()) : null;
        if (p1 == null || ps1 == null || p2 == null || ps2 == null) {
            return -4;
        }
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getUserId(callingUid);
        if (shouldFilterApplication(ps1, callingUid, callingUserId) || shouldFilterApplication(ps2, callingUid, callingUserId)) {
            return -4;
        }
        return checkSignaturesInternal(p1.getSigningDetails(), p2.getSigningDetails());
    }

    @Override // com.android.server.pm.Computer
    public int checkUidSignatures(int uid1, int uid2) {
        SigningDetails p1SigningDetails;
        SigningDetails p2SigningDetails;
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getUserId(callingUid);
        int appId1 = UserHandle.getAppId(uid1);
        int appId2 = UserHandle.getAppId(uid2);
        Object obj = this.mSettings.getSettingBase(appId1);
        if (obj == null) {
            return -4;
        }
        if (obj instanceof SharedUserSetting) {
            SharedUserSetting sus = (SharedUserSetting) obj;
            if (shouldFilterApplication(sus, callingUid, callingUserId)) {
                return -4;
            }
            p1SigningDetails = sus.signatures.mSigningDetails;
        } else if (!(obj instanceof PackageSetting)) {
            return -4;
        } else {
            PackageSetting ps = (PackageSetting) obj;
            if (shouldFilterApplication(ps, callingUid, callingUserId)) {
                return -4;
            }
            p1SigningDetails = ps.getSigningDetails();
        }
        Object obj2 = this.mSettings.getSettingBase(appId2);
        if (obj2 == null) {
            return -4;
        }
        if (obj2 instanceof SharedUserSetting) {
            SharedUserSetting sus2 = (SharedUserSetting) obj2;
            if (shouldFilterApplication(sus2, callingUid, callingUserId)) {
                return -4;
            }
            p2SigningDetails = sus2.signatures.mSigningDetails;
        } else if (!(obj2 instanceof PackageSetting)) {
            return -4;
        } else {
            PackageSetting ps2 = (PackageSetting) obj2;
            if (shouldFilterApplication(ps2, callingUid, callingUserId)) {
                return -4;
            }
            p2SigningDetails = ps2.getSigningDetails();
        }
        return checkSignaturesInternal(p1SigningDetails, p2SigningDetails);
    }

    private int checkSignaturesInternal(SigningDetails p1SigningDetails, SigningDetails p2SigningDetails) {
        if (p1SigningDetails == null) {
            if (p2SigningDetails == null) {
                return 1;
            }
            return -1;
        } else if (p2SigningDetails == null) {
            return -2;
        } else {
            int result = PackageManagerServiceUtils.compareSignatures(p1SigningDetails.getSignatures(), p2SigningDetails.getSignatures());
            if (result == 0) {
                return result;
            }
            if (p1SigningDetails.hasPastSigningCertificates() || p2SigningDetails.hasPastSigningCertificates()) {
                Signature[] p1Signatures = p1SigningDetails.hasPastSigningCertificates() ? new Signature[]{p1SigningDetails.getPastSigningCertificates()[0]} : p1SigningDetails.getSignatures();
                Signature[] p2Signatures = p2SigningDetails.hasPastSigningCertificates() ? new Signature[]{p2SigningDetails.getPastSigningCertificates()[0]} : p2SigningDetails.getSignatures();
                return PackageManagerServiceUtils.compareSignatures(p1Signatures, p2Signatures);
            }
            return result;
        }
    }

    @Override // com.android.server.pm.Computer
    public boolean hasSigningCertificate(String packageName, byte[] certificate, int type) {
        PackageStateInternal ps;
        AndroidPackage p = this.mPackages.get(packageName);
        if (p == null || (ps = getPackageStateInternal(p.getPackageName())) == null) {
            return false;
        }
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getUserId(callingUid);
        if (shouldFilterApplication(ps, callingUid, callingUserId)) {
            return false;
        }
        switch (type) {
            case 0:
                return p.getSigningDetails().hasCertificate(certificate);
            case 1:
                return p.getSigningDetails().hasSha256Certificate(certificate);
            default:
                return false;
        }
    }

    @Override // com.android.server.pm.Computer
    public boolean hasUidSigningCertificate(int uid, byte[] certificate, int type) {
        SigningDetails signingDetails;
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getUserId(callingUid);
        int appId = UserHandle.getAppId(uid);
        Object obj = this.mSettings.getSettingBase(appId);
        if (obj == null) {
            return false;
        }
        if (obj instanceof SharedUserSetting) {
            SharedUserSetting sus = (SharedUserSetting) obj;
            if (shouldFilterApplication(sus, callingUid, callingUserId)) {
                return false;
            }
            signingDetails = sus.signatures.mSigningDetails;
        } else if (!(obj instanceof PackageSetting)) {
            return false;
        } else {
            PackageSetting ps = (PackageSetting) obj;
            if (shouldFilterApplication(ps, callingUid, callingUserId)) {
                return false;
            }
            signingDetails = ps.getSigningDetails();
        }
        switch (type) {
            case 0:
                return signingDetails.hasCertificate(certificate);
            case 1:
                return signingDetails.hasSha256Certificate(certificate);
            default:
                return false;
        }
    }

    @Override // com.android.server.pm.Computer
    public List<String> getAllPackages() {
        if (Binder.getCallingUid() != 1071) {
            PackageManagerServiceUtils.enforceSystemOrRootOrShell("getAllPackages is limited to privileged callers");
        }
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getUserId(callingUid);
        if (canViewInstantApps(callingUid, callingUserId)) {
            return new ArrayList(this.mPackages.keySet());
        }
        String instantAppPkgName = getInstantAppPackageName(callingUid);
        List<String> result = new ArrayList<>();
        if (instantAppPkgName != null) {
            for (AndroidPackage pkg : this.mPackages.values()) {
                if (pkg.isVisibleToInstantApps()) {
                    result.add(pkg.getPackageName());
                }
            }
        } else {
            for (AndroidPackage pkg2 : this.mPackages.values()) {
                PackageStateInternal ps = getPackageStateInternal(pkg2.getPackageName());
                if (ps == null || !ps.getUserStateOrDefault(callingUserId).isInstantApp() || this.mInstantAppRegistry.isInstantAccessGranted(callingUserId, UserHandle.getAppId(callingUid), ps.getAppId())) {
                    result.add(pkg2.getPackageName());
                }
            }
        }
        return result;
    }

    @Override // com.android.server.pm.Computer
    public String getNameForUid(int uid) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return null;
        }
        if (Process.isSdkSandboxUid(uid)) {
            uid = getBaseSdkSandboxUid();
        }
        int callingUserId = UserHandle.getUserId(callingUid);
        int appId = UserHandle.getAppId(uid);
        Object obj = this.mSettings.getSettingBase(appId);
        if (obj instanceof SharedUserSetting) {
            SharedUserSetting sus = (SharedUserSetting) obj;
            if (shouldFilterApplication(sus, callingUid, callingUserId)) {
                return null;
            }
            return sus.name + ":" + sus.mAppId;
        } else if (obj instanceof PackageSetting) {
            PackageSetting ps = (PackageSetting) obj;
            if (shouldFilterApplication(ps, callingUid, callingUserId)) {
                return null;
            }
            return ps.getPackageName();
        } else {
            return null;
        }
    }

    @Override // com.android.server.pm.Computer
    public String[] getNamesForUids(int[] uids) {
        if (uids == null || uids.length == 0) {
            return null;
        }
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return null;
        }
        int callingUserId = UserHandle.getUserId(callingUid);
        String[] names = new String[uids.length];
        for (int i = uids.length - 1; i >= 0; i--) {
            int uid = uids[i];
            if (Process.isSdkSandboxUid(uid)) {
                uid = getBaseSdkSandboxUid();
            }
            int appId = UserHandle.getAppId(uid);
            Object obj = this.mSettings.getSettingBase(appId);
            if (obj instanceof SharedUserSetting) {
                SharedUserSetting sus = (SharedUserSetting) obj;
                if (shouldFilterApplication(sus, callingUid, callingUserId)) {
                    names[i] = null;
                } else {
                    names[i] = "shared:" + sus.name;
                }
            } else if (obj instanceof PackageSetting) {
                PackageSetting ps = (PackageSetting) obj;
                if (shouldFilterApplication(ps, callingUid, callingUserId)) {
                    names[i] = null;
                } else {
                    names[i] = ps.getPackageName();
                }
            } else {
                names[i] = null;
            }
        }
        return names;
    }

    @Override // com.android.server.pm.Computer
    public int getUidForSharedUser(String sharedUserName) {
        SharedUserSetting suid;
        if (sharedUserName == null) {
            return -1;
        }
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null || (suid = this.mSettings.getSharedUserFromId(sharedUserName)) == null || shouldFilterApplication(suid, callingUid, UserHandle.getUserId(callingUid))) {
            return -1;
        }
        return suid.mAppId;
    }

    @Override // com.android.server.pm.Computer
    public int getFlagsForUid(int uid) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return 0;
        }
        if (Process.isSdkSandboxUid(uid)) {
            uid = getBaseSdkSandboxUid();
        }
        int callingUserId = UserHandle.getUserId(callingUid);
        int appId = UserHandle.getAppId(uid);
        Object obj = this.mSettings.getSettingBase(appId);
        if (obj instanceof SharedUserSetting) {
            SharedUserSetting sus = (SharedUserSetting) obj;
            if (shouldFilterApplication(sus, callingUid, callingUserId)) {
                return 0;
            }
            return sus.getFlags();
        } else if (obj instanceof PackageSetting) {
            PackageSetting ps = (PackageSetting) obj;
            if (shouldFilterApplication(ps, callingUid, callingUserId)) {
                return 0;
            }
            return ps.getFlags();
        } else {
            return 0;
        }
    }

    @Override // com.android.server.pm.Computer
    public int getPrivateFlagsForUid(int uid) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return 0;
        }
        if (Process.isSdkSandboxUid(uid)) {
            uid = getBaseSdkSandboxUid();
        }
        int callingUserId = UserHandle.getUserId(callingUid);
        int appId = UserHandle.getAppId(uid);
        Object obj = this.mSettings.getSettingBase(appId);
        if (obj instanceof SharedUserSetting) {
            SharedUserSetting sus = (SharedUserSetting) obj;
            if (shouldFilterApplication(sus, callingUid, callingUserId)) {
                return 0;
            }
            return sus.getPrivateFlags();
        } else if (obj instanceof PackageSetting) {
            PackageSetting ps = (PackageSetting) obj;
            if (shouldFilterApplication(ps, callingUid, callingUserId)) {
                return 0;
            }
            return ps.getPrivateFlags();
        } else {
            return 0;
        }
    }

    @Override // com.android.server.pm.Computer
    public boolean isUidPrivileged(int uid) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return false;
        }
        if (Process.isSdkSandboxUid(uid)) {
            uid = getBaseSdkSandboxUid();
        }
        int appId = UserHandle.getAppId(uid);
        Object obj = this.mSettings.getSettingBase(appId);
        if (obj instanceof SharedUserSetting) {
            SharedUserSetting sus = (SharedUserSetting) obj;
            ArraySet<? extends PackageStateInternal> packageStates = sus.getPackageStates();
            int numPackages = packageStates.size();
            for (int index = 0; index < numPackages; index++) {
                PackageStateInternal ps = packageStates.valueAt(index);
                if (ps.isPrivileged()) {
                    return true;
                }
            }
        } else if (obj instanceof PackageSetting) {
            PackageSetting ps2 = (PackageSetting) obj;
            return ps2.isPrivileged();
        }
        return false;
    }

    @Override // com.android.server.pm.Computer
    public String[] getAppOpPermissionPackages(String permissionName) {
        if (permissionName == null) {
            return EmptyArray.STRING;
        }
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return EmptyArray.STRING;
        }
        int callingUserId = UserHandle.getUserId(callingUid);
        ArraySet<String> packageNames = new ArraySet<>(this.mPermissionManager.getAppOpPermissionPackages(permissionName));
        for (int i = packageNames.size() - 1; i >= 0; i--) {
            String packageName = packageNames.valueAt(i);
            if (shouldFilterApplication(this.mSettings.getPackage(packageName), callingUid, callingUserId)) {
                packageNames.removeAt(i);
            }
        }
        int i2 = packageNames.size();
        return (String[]) packageNames.toArray(new String[i2]);
    }

    @Override // com.android.server.pm.Computer
    public ParceledListSlice<PackageInfo> getPackagesHoldingPermissions(String[] permissions, long flags, int userId) {
        if (this.mUserManager.exists(userId)) {
            long flags2 = updateFlagsForPackage(flags, userId);
            enforceCrossUserPermission(Binder.getCallingUid(), userId, true, false, "get packages holding permissions");
            boolean listUninstalled = (4202496 & flags2) != 0;
            ArrayList<PackageInfo> list = new ArrayList<>();
            boolean[] tmpBools = new boolean[permissions.length];
            for (PackageStateInternal ps : getPackageStates().values()) {
                if (ps.getPkg() != null || listUninstalled) {
                    addPackageHoldingPermissions(list, ps, permissions, tmpBools, flags2, userId);
                }
            }
            return new ParceledListSlice<>(list);
        }
        return ParceledListSlice.emptyList();
    }

    private void addPackageHoldingPermissions(ArrayList<PackageInfo> list, PackageStateInternal ps, String[] permissions, boolean[] tmp, long flags, int userId) {
        PackageInfo pi;
        int numMatch = 0;
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            if (this.mPermissionManager.checkPermission(ps.getPackageName(), permission, userId) == 0) {
                tmp[i] = true;
                numMatch++;
            } else {
                tmp[i] = false;
            }
        }
        if (numMatch != 0 && (pi = generatePackageInfo(ps, flags, userId)) != null) {
            if ((4096 & flags) == 0) {
                if (numMatch == permissions.length) {
                    pi.requestedPermissions = permissions;
                } else {
                    pi.requestedPermissions = new String[numMatch];
                    int numMatch2 = 0;
                    for (int i2 = 0; i2 < permissions.length; i2++) {
                        if (tmp[i2]) {
                            pi.requestedPermissions[numMatch2] = permissions[i2];
                            numMatch2++;
                        }
                    }
                }
            }
            list.add(pi);
        }
    }

    @Override // com.android.server.pm.Computer
    public List<ApplicationInfo> getInstalledApplications(long flags, int userId, int callingUid) {
        ArrayList<ApplicationInfo> list;
        ApplicationInfo ai;
        long effectiveFlags;
        ApplicationInfo ai2;
        if (getInstantAppPackageName(callingUid) != null) {
            return Collections.emptyList();
        }
        if (this.mUserManager.exists(userId)) {
            long flags2 = updateFlagsForApplication(flags, userId);
            boolean listUninstalled = (4202496 & flags2) != 0;
            enforceCrossUserPermission(callingUid, userId, false, false, "get installed application info");
            ArrayMap<String, ? extends PackageStateInternal> packageStates = getPackageStates();
            if (listUninstalled) {
                list = new ArrayList<>(packageStates.size());
                for (PackageStateInternal ps : packageStates.values()) {
                    if (ps.isSystem()) {
                        long effectiveFlags2 = flags2 | 4194304;
                        effectiveFlags = effectiveFlags2;
                    } else {
                        effectiveFlags = flags2;
                    }
                    if (ps.getPkg() != null) {
                        if (!filterSharedLibPackage(ps, callingUid, userId, flags2) && !shouldFilterApplication(ps, callingUid, userId)) {
                            ai2 = PackageInfoUtils.generateApplicationInfo(ps.getPkg(), effectiveFlags, ps.getUserStateOrDefault(userId), userId, ps);
                            if (ai2 != null) {
                                ai2.packageName = resolveExternalPackageName(ps.getPkg());
                            }
                        }
                    } else {
                        ai2 = generateApplicationInfoFromSettings(ps.getPackageName(), effectiveFlags, callingUid, userId);
                    }
                    if (ai2 != null && (ai2.enabled || ai2.packageName == null || !ITranPackageManagerService.Instance().needSkipAppInfo(ai2.packageName, userId))) {
                        ApplicationInfo ai3 = PackageManagerService.sPmsExt.updateApplicationInfoForRemovable(ai2);
                        if (!PackageManagerService.sPmsExt.needSkipAppInfo(ai3)) {
                            list.add(ai3);
                        }
                    }
                }
            } else {
                list = new ArrayList<>(this.mPackages.size());
                for (PackageStateInternal packageState : packageStates.values()) {
                    AndroidPackage pkg = packageState.getPkg();
                    if (pkg != null && !filterSharedLibPackage(packageState, Binder.getCallingUid(), userId, flags2) && !shouldFilterApplication(packageState, callingUid, userId) && (ai = PackageInfoUtils.generateApplicationInfo(pkg, flags2, packageState.getUserStateOrDefault(userId), userId, packageState)) != null && (ai.enabled || ai.packageName == null || !ITranPackageManagerService.Instance().needSkipAppInfo(ai.packageName, userId))) {
                        ApplicationInfo ai4 = PackageManagerService.sPmsExt.updateApplicationInfoForRemovable(ai);
                        if (!PackageManagerService.sPmsExt.needSkipAppInfo(ai4)) {
                            ai4.packageName = resolveExternalPackageName(pkg);
                            list.add(ai4);
                        }
                    }
                }
            }
            return list;
        }
        return Collections.emptyList();
    }

    /* JADX WARN: Removed duplicated region for block: B:14:0x0045  */
    /* JADX WARN: Removed duplicated region for block: B:38:0x009f  */
    /* JADX WARN: Removed duplicated region for block: B:41:0x00af  */
    /* JADX WARN: Removed duplicated region for block: B:44:0x00bc A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:45:0x00bd  */
    @Override // com.android.server.pm.Computer
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public ProviderInfo resolveContentProvider(String name, long flags, int userId, int callingUid) {
        boolean checkedGrants;
        int tempUserId;
        PackageStateInternal packageState;
        if (!this.mUserManager.exists(userId)) {
            return null;
        }
        long flags2 = updateFlagsForComponent(flags, userId);
        ProviderInfo providerInfo = this.mComponentResolver.queryProvider(this, name, flags2, userId);
        if (providerInfo != null && userId != UserHandle.getUserId(callingUid)) {
            UriGrantsManagerInternal ugmInternal = (UriGrantsManagerInternal) this.mInjector.getLocalService(UriGrantsManagerInternal.class);
            boolean checkedGrants2 = ugmInternal.checkAuthorityGrants(callingUid, providerInfo, userId, true);
            checkedGrants = checkedGrants2;
            if (!checkedGrants) {
                boolean enforceCrossUser = true;
                if (ContentProvider.isAuthorityRedirectedForCloneProfile(name)) {
                    UserManagerInternal umInternal = this.mInjector.getUserManagerInternal();
                    UserInfo userInfo = umInternal.getUserInfo(UserHandle.getUserId(callingUid));
                    if (userInfo != null && userInfo.isCloneProfile() && userInfo.profileGroupId == userId) {
                        enforceCrossUser = false;
                    }
                }
                if ((this.mUserManager.isDualProfile(UserHandle.getUserId(callingUid)) || this.mUserManager.isDualProfile(userId)) ? false : enforceCrossUser) {
                    enforceCrossUserPermission(callingUid, userId, false, false, "resolveContentProvider");
                }
            }
            if (providerInfo == null || this.mUserManager.isDualProfile(userId)) {
                if (providerInfo == null) {
                    tempUserId = userId;
                } else {
                    tempUserId = 0;
                    providerInfo = this.mComponentResolver.queryProvider(this, name, flags2, 0);
                    if (providerInfo == null) {
                        return null;
                    }
                }
                packageState = getPackageStateInternal(providerInfo.packageName);
                if (PackageStateUtils.isEnabledAndMatches(packageState, providerInfo, flags2, tempUserId)) {
                    return null;
                }
                ComponentName component = new ComponentName(providerInfo.packageName, providerInfo.name);
                if (shouldFilterApplication(packageState, callingUid, component, 4, userId)) {
                    return null;
                }
                return providerInfo;
            }
            return null;
        }
        checkedGrants = false;
        if (!checkedGrants) {
        }
        if (providerInfo == null) {
        }
        if (providerInfo == null) {
        }
        packageState = getPackageStateInternal(providerInfo.packageName);
        if (PackageStateUtils.isEnabledAndMatches(packageState, providerInfo, flags2, tempUserId)) {
        }
    }

    @Override // com.android.server.pm.Computer
    public ProviderInfo getGrantImplicitAccessProviderInfo(int recipientUid, String visibleAuthority) {
        int callingUid = Binder.getCallingUid();
        int recipientUserId = UserHandle.getUserId(recipientUid);
        ProviderInfo contactsProvider = resolveContentProvider("com.android.contacts", 0L, UserHandle.getUserId(callingUid), callingUid);
        if (contactsProvider == null || contactsProvider.applicationInfo == null || !UserHandle.isSameApp(contactsProvider.applicationInfo.uid, callingUid)) {
            throw new SecurityException(callingUid + " is not allow to call grantImplicitAccess");
        }
        long token = Binder.clearCallingIdentity();
        try {
            return resolveContentProvider(visibleAuthority, 0L, recipientUserId, callingUid);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override // com.android.server.pm.Computer
    @Deprecated
    public void querySyncProviders(boolean safeMode, List<String> outNames, List<ProviderInfo> outInfo) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return;
        }
        List<String> names = new ArrayList<>();
        List<ProviderInfo> infos = new ArrayList<>();
        int callingUserId = UserHandle.getCallingUserId();
        this.mComponentResolver.querySyncProviders(this, names, infos, safeMode, callingUserId);
        for (int i = infos.size() - 1; i >= 0; i--) {
            ProviderInfo providerInfo = infos.get(i);
            PackageStateInternal ps = this.mSettings.getPackage(providerInfo.packageName);
            ComponentName component = new ComponentName(providerInfo.packageName, providerInfo.name);
            if (shouldFilterApplication(ps, Binder.getCallingUid(), component, 4, callingUserId)) {
                infos.remove(i);
                names.remove(i);
            }
        }
        if (!names.isEmpty()) {
            outNames.addAll(names);
        }
        if (!infos.isEmpty()) {
            outInfo.addAll(infos);
        }
    }

    @Override // com.android.server.pm.Computer
    public ParceledListSlice<ProviderInfo> queryContentProviders(String processName, int uid, long flags, String metaDataKey) {
        int callingUid = Binder.getCallingUid();
        int userId = processName != null ? UserHandle.getUserId(uid) : UserHandle.getCallingUserId();
        if (this.mUserManager.exists(userId)) {
            long flags2 = updateFlagsForComponent(flags, userId);
            List<ProviderInfo> matchList = this.mComponentResolver.queryProviders(this, processName, metaDataKey, uid, flags2, userId);
            int listSize = matchList == null ? 0 : matchList.size();
            ArrayList<ProviderInfo> finalList = null;
            for (int i = 0; i < listSize; i++) {
                ProviderInfo providerInfo = matchList.get(i);
                if (PackageStateUtils.isEnabledAndMatches(this.mSettings.getPackage(providerInfo.packageName), providerInfo, flags2, userId)) {
                    PackageStateInternal ps = this.mSettings.getPackage(providerInfo.packageName);
                    ComponentName component = new ComponentName(providerInfo.packageName, providerInfo.name);
                    if (!shouldFilterApplication(ps, callingUid, component, 4, userId)) {
                        if (finalList == null) {
                            finalList = new ArrayList<>(listSize - i);
                        }
                        finalList.add(providerInfo);
                    }
                }
            }
            if (finalList != null) {
                finalList.sort(sProviderInitOrderSorter);
                return new ParceledListSlice<>(finalList);
            }
            return ParceledListSlice.emptyList();
        }
        return ParceledListSlice.emptyList();
    }

    @Override // com.android.server.pm.Computer
    public InstrumentationInfo getInstrumentationInfo(ComponentName component, int flags) {
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getUserId(callingUid);
        String packageName = component.getPackageName();
        PackageStateInternal ps = this.mSettings.getPackage(packageName);
        AndroidPackage pkg = this.mPackages.get(packageName);
        if (ps == null || pkg == null || shouldFilterApplication(ps, callingUid, component, 0, callingUserId)) {
            return null;
        }
        ParsedInstrumentation i = this.mInstrumentation.get(component);
        return PackageInfoUtils.generateInstrumentationInfo(i, pkg, flags, callingUserId, ps);
    }

    @Override // com.android.server.pm.Computer
    public ParceledListSlice<InstrumentationInfo> queryInstrumentation(String targetPackage, int flags) {
        InstrumentationInfo ii;
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getUserId(callingUid);
        PackageStateInternal ps = this.mSettings.getPackage(targetPackage);
        if (shouldFilterApplication(ps, callingUid, callingUserId)) {
            return ParceledListSlice.emptyList();
        }
        ArrayList<InstrumentationInfo> finalList = new ArrayList<>();
        int numInstrumentations = this.mInstrumentation.size();
        for (int index = 0; index < numInstrumentations; index++) {
            ParsedInstrumentation p = this.mInstrumentation.valueAt(index);
            if (targetPackage == null || targetPackage.equals(p.getTargetPackage())) {
                String packageName = p.getPackageName();
                AndroidPackage pkg = this.mPackages.get(packageName);
                PackageStateInternal pkgSetting = getPackageStateInternal(packageName);
                if (pkg != null && (ii = PackageInfoUtils.generateInstrumentationInfo(p, pkg, flags, callingUserId, pkgSetting)) != null) {
                    finalList.add(ii);
                }
            }
        }
        return new ParceledListSlice<>(finalList);
    }

    @Override // com.android.server.pm.Computer
    public List<PackageStateInternal> findSharedNonSystemLibraries(PackageStateInternal pkgSetting) {
        List<SharedLibraryInfo> deps = SharedLibraryUtils.findSharedLibraries(pkgSetting);
        if (!deps.isEmpty()) {
            List<PackageStateInternal> retValue = new ArrayList<>();
            for (SharedLibraryInfo info : deps) {
                PackageStateInternal depPackageSetting = getPackageStateInternal(info.getPackageName());
                if (depPackageSetting != null && depPackageSetting.getPkg() != null) {
                    retValue.add(depPackageSetting);
                }
            }
            return retValue;
        }
        return Collections.emptyList();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [5237=4] */
    @Override // com.android.server.pm.Computer
    public boolean getApplicationHiddenSettingAsUser(String packageName, int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USERS", null);
        int callingUid = Binder.getCallingUid();
        enforceCrossUserPermission(callingUid, userId, true, false, "getApplicationHidden for user " + userId);
        boolean isHidden = IPackageManagerServiceLice.Instance().needSkipAppInfo(packageName, userId);
        if (isHidden) {
            return true;
        }
        long callingId = Binder.clearCallingIdentity();
        try {
            PackageStateInternal ps = this.mSettings.getPackage(packageName);
            if (ps == null) {
                return true;
            }
            if (shouldFilterApplication(ps, callingUid, userId)) {
                return true;
            }
            return ps.getUserStateOrDefault(userId).isHidden();
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    @Override // com.android.server.pm.Computer
    public boolean isPackageSuspendedForUser(String packageName, int userId) {
        int callingUid = Binder.getCallingUid();
        enforceCrossUserPermission(callingUid, userId, true, false, "isPackageSuspendedForUser for user " + userId);
        PackageStateInternal ps = this.mSettings.getPackage(packageName);
        if (ps == null || shouldFilterApplication(ps, callingUid, userId)) {
            throw new IllegalArgumentException("Unknown target package: " + packageName);
        }
        return ps.getUserStateOrDefault(userId).isSuspended();
    }

    @Override // com.android.server.pm.Computer
    public boolean isSuspendingAnyPackages(String suspendingPackage, int userId) {
        for (PackageStateInternal packageState : getPackageStates().values()) {
            PackageUserStateInternal state = packageState.getUserStateOrDefault(userId);
            if (state.getSuspendParams() != null && state.getSuspendParams().containsKey(suspendingPackage)) {
                return true;
            }
        }
        return false;
    }

    @Override // com.android.server.pm.Computer
    public ParceledListSlice<IntentFilter> getAllIntentFilters(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return ParceledListSlice.emptyList();
        }
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getUserId(callingUid);
        PackageStateInternal ps = getPackageStateInternal(packageName);
        AndroidPackage pkg = ps == null ? null : ps.getPkg();
        if (pkg == null || ArrayUtils.isEmpty(pkg.getActivities())) {
            return ParceledListSlice.emptyList();
        }
        if (shouldFilterApplication(ps, callingUid, callingUserId)) {
            return ParceledListSlice.emptyList();
        }
        int count = ArrayUtils.size(pkg.getActivities());
        ArrayList<IntentFilter> result = new ArrayList<>();
        for (int n = 0; n < count; n++) {
            ParsedActivity activity = pkg.getActivities().get(n);
            List<ParsedIntentInfo> intentInfos = activity.getIntents();
            for (int index = 0; index < intentInfos.size(); index++) {
                result.add(new IntentFilter(intentInfos.get(index).getIntentFilter()));
            }
        }
        return new ParceledListSlice<>(result);
    }

    @Override // com.android.server.pm.Computer
    public boolean getBlockUninstallForUser(String packageName, int userId) {
        PackageStateInternal ps = this.mSettings.getPackage(packageName);
        if (ps == null || shouldFilterApplication(ps, Binder.getCallingUid(), userId)) {
            return false;
        }
        return this.mSettings.getBlockUninstall(userId, packageName);
    }

    @Override // com.android.server.pm.Computer
    public SparseArray<int[]> getBroadcastAllowList(String packageName, int[] userIds, boolean isInstantApp) {
        PackageStateInternal setting;
        if (isInstantApp || (setting = getPackageStateInternal(packageName, 1000)) == null) {
            return null;
        }
        return this.mAppsFilter.getVisibilityAllowList(this, setting, userIds, getPackageStates());
    }

    @Override // com.android.server.pm.Computer
    public String getInstallerPackageName(String packageName) {
        int callingUid = Binder.getCallingUid();
        InstallSource installSource = getInstallSource(packageName, callingUid);
        if (installSource == null) {
            throw new IllegalArgumentException("Unknown package: " + packageName);
        }
        String installerPackageName = installSource.installerPackageName;
        if (installerPackageName != null) {
            PackageStateInternal ps = this.mSettings.getPackage(installerPackageName);
            if (ps == null || shouldFilterApplication(ps, callingUid, UserHandle.getUserId(callingUid))) {
                return null;
            }
            return installerPackageName;
        }
        return installerPackageName;
    }

    private InstallSource getInstallSource(String packageName, int callingUid) {
        PackageStateInternal ps = this.mSettings.getPackage(packageName);
        if (ps == null && this.mApexManager.isApexPackage(packageName)) {
            return InstallSource.EMPTY;
        }
        if (ps == null || shouldFilterApplication(ps, callingUid, UserHandle.getUserId(callingUid))) {
            return null;
        }
        return ps.getInstallSource();
    }

    @Override // com.android.server.pm.Computer
    public InstallSourceInfo getInstallSourceInfo(String packageName) {
        String initiatingPackageName;
        String originatingPackageName;
        SigningInfo initiatingPackageSigningInfo;
        PackageStateInternal ps;
        String initiatingPackageName2;
        PackageStateInternal ps2;
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getUserId(callingUid);
        InstallSource installSource = getInstallSource(packageName, callingUid);
        if (installSource == null) {
            return null;
        }
        String installerPackageName = installSource.installerPackageName;
        if (installerPackageName != null && ((ps2 = this.mSettings.getPackage(installerPackageName)) == null || shouldFilterApplication(ps2, callingUid, userId))) {
            installerPackageName = null;
        }
        if (installSource.isInitiatingPackageUninstalled) {
            boolean isInstantApp = getInstantAppPackageName(callingUid) != null;
            if (!isInstantApp && isCallerSameApp(packageName, callingUid)) {
                initiatingPackageName2 = installSource.initiatingPackageName;
            } else {
                initiatingPackageName2 = null;
            }
            initiatingPackageName = initiatingPackageName2;
        } else if (Objects.equals(installSource.initiatingPackageName, installSource.installerPackageName)) {
            initiatingPackageName = installerPackageName;
        } else {
            String initiatingPackageName3 = installSource.initiatingPackageName;
            PackageStateInternal ps3 = this.mSettings.getPackage(initiatingPackageName3);
            initiatingPackageName = (ps3 == null || shouldFilterApplication(ps3, callingUid, userId)) ? null : initiatingPackageName3;
        }
        String originatingPackageName2 = installSource.originatingPackageName;
        if (originatingPackageName2 != null && ((ps = this.mSettings.getPackage(originatingPackageName2)) == null || shouldFilterApplication(ps, callingUid, userId))) {
            originatingPackageName2 = null;
        }
        if (originatingPackageName2 != null && this.mContext.checkCallingOrSelfPermission("android.permission.INSTALL_PACKAGES") != 0) {
            originatingPackageName = null;
        } else {
            originatingPackageName = originatingPackageName2;
        }
        PackageSignatures signatures = installSource.initiatingPackageSignatures;
        if (initiatingPackageName != null && signatures != null && signatures.mSigningDetails != SigningDetails.UNKNOWN) {
            initiatingPackageSigningInfo = new SigningInfo(signatures.mSigningDetails);
        } else {
            initiatingPackageSigningInfo = null;
        }
        return new InstallSourceInfo(initiatingPackageName, initiatingPackageSigningInfo, originatingPackageName, installerPackageName, installSource.packageSource);
    }

    @Override // com.android.server.pm.Computer
    public int getApplicationEnabledSetting(String packageName, int userId) {
        if (this.mUserManager.exists(userId)) {
            int callingUid = Binder.getCallingUid();
            enforceCrossUserPermission(callingUid, userId, false, false, "get enabled");
            try {
                if (shouldFilterApplication(this.mSettings.getPackage(packageName), callingUid, userId)) {
                    throw new PackageManager.NameNotFoundException(packageName);
                }
                if (IPackageManagerServiceLice.Instance().needSkipAppInfo(packageName, userId)) {
                    String[] callerPackageNames = getPackagesForUid(callingUid);
                    String currentHomePackageName = this.mDefaultAppProvider.getDefaultHome(userId);
                    if (ArrayUtils.contains(callerPackageNames, currentHomePackageName)) {
                        return 12;
                    }
                }
                return this.mSettings.getApplicationEnabledSetting(packageName, userId);
            } catch (PackageManager.NameNotFoundException e) {
                throw new IllegalArgumentException("Unknown package: " + packageName);
            }
        }
        return 2;
    }

    @Override // com.android.server.pm.Computer
    public int getComponentEnabledSetting(ComponentName component, int callingUid, int userId) {
        enforceCrossUserPermission(callingUid, userId, false, false, "getComponentEnabled");
        return getComponentEnabledSettingInternal(component, callingUid, userId);
    }

    @Override // com.android.server.pm.Computer
    public int getComponentEnabledSettingInternal(ComponentName component, int callingUid, int userId) {
        if (component == null) {
            return 0;
        }
        if (this.mUserManager.exists(userId)) {
            try {
                if (shouldFilterApplication(this.mSettings.getPackage(component.getPackageName()), callingUid, component, 0, userId)) {
                    throw new PackageManager.NameNotFoundException(component.getPackageName());
                }
                return this.mSettings.getComponentEnabledSetting(component, userId);
            } catch (PackageManager.NameNotFoundException e) {
                throw new IllegalArgumentException("Unknown component: " + component);
            }
        }
        return 2;
    }

    @Override // com.android.server.pm.Computer
    public boolean isComponentEffectivelyEnabled(ComponentInfo componentInfo, int userId) {
        try {
            String packageName = componentInfo.packageName;
            int appEnabledSetting = this.mSettings.getApplicationEnabledSetting(packageName, userId);
            if (appEnabledSetting == 0) {
                if (!componentInfo.applicationInfo.enabled) {
                    return false;
                }
            } else if (appEnabledSetting != 1) {
                return false;
            }
            int componentEnabledSetting = this.mSettings.getComponentEnabledSetting(componentInfo.getComponentName(), userId);
            if (componentEnabledSetting == 0) {
                return componentInfo.isEnabled();
            }
            if (componentEnabledSetting != 1) {
                return false;
            }
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override // com.android.server.pm.Computer
    public KeySet getKeySetByAlias(String packageName, String alias) {
        if (packageName == null || alias == null) {
            return null;
        }
        AndroidPackage pkg = this.mPackages.get(packageName);
        if (pkg == null || shouldFilterApplication(getPackageStateInternal(pkg.getPackageName()), Binder.getCallingUid(), UserHandle.getCallingUserId())) {
            Slog.w("PackageManager", "KeySet requested for unknown package: " + packageName);
            throw new IllegalArgumentException("Unknown package: " + packageName);
        }
        KeySetManagerService ksms = this.mSettings.getKeySetManagerService();
        return new KeySet(ksms.getKeySetByAliasAndPackageNameLPr(packageName, alias));
    }

    @Override // com.android.server.pm.Computer
    public KeySet getSigningKeySet(String packageName) {
        if (packageName == null) {
            return null;
        }
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getUserId(callingUid);
        AndroidPackage pkg = this.mPackages.get(packageName);
        if (pkg == null || shouldFilterApplication(getPackageStateInternal(pkg.getPackageName()), callingUid, callingUserId)) {
            Slog.w("PackageManager", "KeySet requested for unknown package: " + packageName + ", uid:" + callingUid);
            throw new IllegalArgumentException("Unknown package: " + packageName);
        } else if (pkg.getUid() != callingUid && 1000 != callingUid) {
            throw new SecurityException("May not access signing KeySet of other apps.");
        } else {
            KeySetManagerService ksms = this.mSettings.getKeySetManagerService();
            return new KeySet(ksms.getSigningKeySetByPackageNameLPr(packageName));
        }
    }

    @Override // com.android.server.pm.Computer
    public boolean isPackageSignedByKeySet(String packageName, KeySet ks) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null || packageName == null || ks == null) {
            return false;
        }
        AndroidPackage pkg = this.mPackages.get(packageName);
        if (pkg == null || shouldFilterApplication(getPackageStateInternal(pkg.getPackageName()), callingUid, UserHandle.getUserId(callingUid))) {
            Slog.w("PackageManager", "KeySet requested for unknown package: " + packageName);
            throw new IllegalArgumentException("Unknown package: " + packageName);
        }
        IBinder ksh = ks.getToken();
        if (ksh instanceof KeySetHandle) {
            KeySetManagerService ksms = this.mSettings.getKeySetManagerService();
            return ksms.packageIsSignedByLPr(packageName, (KeySetHandle) ksh);
        }
        return false;
    }

    @Override // com.android.server.pm.Computer
    public boolean isPackageSignedByKeySetExactly(String packageName, KeySet ks) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null || packageName == null || ks == null) {
            return false;
        }
        AndroidPackage pkg = this.mPackages.get(packageName);
        if (pkg == null || shouldFilterApplication(getPackageStateInternal(pkg.getPackageName()), callingUid, UserHandle.getUserId(callingUid))) {
            Slog.w("PackageManager", "KeySet requested for unknown package: " + packageName);
            throw new IllegalArgumentException("Unknown package: " + packageName);
        }
        IBinder ksh = ks.getToken();
        if (ksh instanceof KeySetHandle) {
            KeySetManagerService ksms = this.mSettings.getKeySetManagerService();
            return ksms.packageIsSignedByExactlyLPr(packageName, (KeySetHandle) ksh);
        }
        return false;
    }

    @Override // com.android.server.pm.Computer
    public int[] getVisibilityAllowList(String packageName, int userId) {
        SparseArray<int[]> visibilityAllowList;
        PackageStateInternal ps = getPackageStateInternal(packageName, 1000);
        if (ps == null || (visibilityAllowList = this.mAppsFilter.getVisibilityAllowList(this, ps, new int[]{userId}, getPackageStates())) == null) {
            return null;
        }
        return visibilityAllowList.get(userId);
    }

    @Override // com.android.server.pm.Computer
    public boolean canQueryPackage(int callingUid, String targetPackageName) {
        if (callingUid == 0 || targetPackageName == null) {
            return true;
        }
        Object setting = this.mSettings.getSettingBase(UserHandle.getAppId(callingUid));
        if (setting == null) {
            return false;
        }
        int userId = UserHandle.getUserId(callingUid);
        int targetAppId = UserHandle.getAppId(getPackageUid(targetPackageName, 0L, userId));
        if (targetAppId != -1) {
            Object targetSetting = this.mSettings.getSettingBase(targetAppId);
            return targetSetting instanceof PackageSetting ? true ^ shouldFilterApplication((PackageSetting) targetSetting, callingUid, userId) : true ^ shouldFilterApplication((SharedUserSetting) targetSetting, callingUid, userId);
        } else if (setting instanceof PackageSetting) {
            AndroidPackage pkg = ((PackageSetting) setting).getPkg();
            if (pkg != null && this.mAppsFilter.canQueryPackage(pkg, targetPackageName)) {
                return true;
            }
            return false;
        } else {
            ArraySet<? extends PackageStateInternal> packageStates = ((SharedUserSetting) setting).getPackageStates();
            for (int i = packageStates.size() - 1; i >= 0; i--) {
                AndroidPackage pkg2 = packageStates.valueAt(i).getPkg();
                if (pkg2 != null && this.mAppsFilter.canQueryPackage(pkg2, targetPackageName)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override // com.android.server.pm.Computer
    public int getPackageUid(String packageName, long flags, int userId) {
        if (this.mUserManager.exists(userId)) {
            int callingUid = Binder.getCallingUid();
            long flags2 = updateFlagsForPackage(flags, userId);
            enforceCrossUserPermission(callingUid, userId, false, false, "getPackageUid");
            return getPackageUidInternal(packageName, flags2, userId, callingUid);
        }
        return -1;
    }

    @Override // com.android.server.pm.Computer
    public boolean canAccessComponent(int callingUid, ComponentName component, int userId) {
        PackageStateInternal packageState = getPackageStateInternal(component.getPackageName());
        return (packageState == null || shouldFilterApplication(packageState, callingUid, component, 0, userId)) ? false : true;
    }

    @Override // com.android.server.pm.Computer
    public boolean isCallerInstallerOfRecord(AndroidPackage pkg, int callingUid) {
        PackageStateInternal packageState;
        PackageStateInternal installerPackageState;
        return (pkg == null || (packageState = getPackageStateInternal(pkg.getPackageName())) == null || (installerPackageState = getPackageStateInternal(packageState.getInstallSource().installerPackageName)) == null || !UserHandle.isSameApp(installerPackageState.getAppId(), callingUid)) ? false : true;
    }

    @Override // com.android.server.pm.Computer
    public int getInstallReason(String packageName, int userId) {
        int callingUid = Binder.getCallingUid();
        enforceCrossUserPermission(callingUid, userId, true, false, "get install reason");
        PackageStateInternal ps = this.mSettings.getPackage(packageName);
        if (shouldFilterApplication(ps, callingUid, userId) || ps == null) {
            return 0;
        }
        return ps.getUserStateOrDefault(userId).getInstallReason();
    }

    @Override // com.android.server.pm.Computer
    public boolean canPackageQuery(String sourcePackageName, String targetPackageName, int userId) {
        boolean z = false;
        if (this.mUserManager.exists(userId)) {
            int callingUid = Binder.getCallingUid();
            enforceCrossUserPermission(callingUid, userId, false, false, "may package query");
            PackageStateInternal sourceSetting = getPackageStateInternal(sourcePackageName);
            PackageStateInternal targetSetting = getPackageStateInternal(targetPackageName);
            boolean throwException = sourceSetting == null || targetSetting == null;
            if (!throwException) {
                boolean filterSource = shouldFilterApplication(sourceSetting, callingUid, userId);
                boolean filterTarget = shouldFilterApplication(targetSetting, callingUid, userId);
                if (filterSource || filterTarget) {
                    z = true;
                }
                throwException = z;
            }
            if (throwException) {
                throw new ParcelableException(new PackageManager.NameNotFoundException("Package(s) " + sourcePackageName + " and/or " + targetPackageName + " not found."));
            }
            int sourcePackageUid = UserHandle.getUid(userId, sourceSetting.getAppId());
            return true ^ shouldFilterApplication(targetSetting, sourcePackageUid, userId);
        }
        return false;
    }

    @Override // com.android.server.pm.Computer
    public boolean canForwardTo(Intent intent, String resolvedType, int sourceUserId, int targetUserId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", null);
        List<CrossProfileIntentFilter> matches = getMatchingCrossProfileIntentFilters(intent, resolvedType, sourceUserId);
        if (matches != null) {
            int size = matches.size();
            for (int i = 0; i < size; i++) {
                if (matches.get(i).getTargetUserId() == targetUserId) {
                    return true;
                }
            }
        }
        if (intent.hasWebURI()) {
            int callingUid = Binder.getCallingUid();
            UserInfo parent = getProfileParent(sourceUserId);
            if (parent == null) {
                return false;
            }
            long flags = updateFlagsForResolve(0L, parent.id, callingUid, false, isImplicitImageCaptureIntentAndNotSetByDpc(intent, parent.id, resolvedType, 0L));
            CrossProfileDomainInfo xpDomainInfo = getCrossProfileDomainPreferredLpr(intent, resolvedType, flags | 65536, sourceUserId, parent.id);
            return xpDomainInfo != null;
        }
        return false;
    }

    @Override // com.android.server.pm.Computer
    public List<ApplicationInfo> getPersistentApplications(boolean safeMode, int flags) {
        PackageStateInternal ps;
        ApplicationInfo ai;
        ArrayList<ApplicationInfo> finalList = new ArrayList<>();
        int numPackages = this.mPackages.size();
        int userId = UserHandle.getCallingUserId();
        for (int index = 0; index < numPackages; index++) {
            AndroidPackage p = this.mPackages.valueAt(index);
            boolean z = false;
            boolean matchesUnaware = ((262144 & flags) == 0 || p.isDirectBootAware()) ? false : true;
            if ((524288 & flags) != 0 && p.isDirectBootAware()) {
                z = true;
            }
            boolean matchesAware = z;
            if (p.isPersistent() && ((!safeMode || p.isSystem()) && ((matchesUnaware || matchesAware) && (ps = this.mSettings.getPackage(p.getPackageName())) != null && (ai = PackageInfoUtils.generateApplicationInfo(p, flags, ps.getUserStateOrDefault(userId), userId, ps)) != null))) {
                finalList.add(ai);
            }
        }
        return finalList;
    }

    @Override // com.android.server.pm.Computer
    public SparseArray<String> getAppsWithSharedUserIds() {
        SparseArray<String> sharedUserIds = new SparseArray<>();
        for (SharedUserSetting setting : this.mSettings.getAllSharedUsers()) {
            sharedUserIds.put(UserHandle.getAppId(setting.mAppId), setting.name);
        }
        return sharedUserIds;
    }

    @Override // com.android.server.pm.Computer
    public String[] getSharedUserPackagesForPackage(String packageName, int userId) {
        PackageStateInternal packageSetting = this.mSettings.getPackage(packageName);
        if (packageSetting == null || this.mSettings.getSharedUserFromPackageName(packageName) == null) {
            return EmptyArray.STRING;
        }
        ArraySet<? extends PackageStateInternal> packages = this.mSettings.getSharedUserFromPackageName(packageName).getPackageStates();
        int numPackages = packages.size();
        String[] res = new String[numPackages];
        int i = 0;
        for (int index = 0; index < numPackages; index++) {
            PackageStateInternal ps = packages.valueAt(index);
            if (ps.getUserStateOrDefault(userId).isInstalled()) {
                res[i] = ps.getPackageName();
                i++;
            }
        }
        String[] res2 = (String[]) ArrayUtils.trimToSize(res, i);
        return res2 != null ? res2 : EmptyArray.STRING;
    }

    @Override // com.android.server.pm.Computer
    public Set<String> getUnusedPackages(long downgradeTimeThresholdMillis) {
        int index;
        Set<String> unusedPackages = new ArraySet<>();
        long currentTimeInMillis = System.currentTimeMillis();
        ArrayMap<String, ? extends PackageStateInternal> packageStates = this.mSettings.getPackages();
        int index2 = 0;
        while (index2 < packageStates.size()) {
            PackageStateInternal packageState = packageStates.valueAt(index2);
            if (packageState.getPkg() == null) {
                index = index2;
            } else {
                PackageDexUsage.PackageUseInfo packageUseInfo = this.mDexManager.getPackageUseInfoOrDefault(packageState.getPackageName());
                index = index2;
                if (PackageManagerServiceUtils.isUnusedSinceTimeInMillis(PackageStateUtils.getEarliestFirstInstallTime(packageState.getUserStates()), currentTimeInMillis, downgradeTimeThresholdMillis, packageUseInfo, packageState.getTransientState().getLatestPackageUseTimeInMills(), packageState.getTransientState().getLatestForegroundPackageUseTimeInMills())) {
                    unusedPackages.add(packageState.getPackageName());
                }
            }
            index2 = index + 1;
        }
        return unusedPackages;
    }

    @Override // com.android.server.pm.Computer
    public CharSequence getHarmfulAppWarning(String packageName, int userId) {
        int callingUid = Binder.getCallingUid();
        int callingAppId = UserHandle.getAppId(callingUid);
        enforceCrossUserPermission(callingUid, userId, true, true, "getHarmfulAppInfo");
        if (callingAppId != 1000 && callingAppId != 0 && checkUidPermission("android.permission.SET_HARMFUL_APP_WARNINGS", callingUid) != 0) {
            throw new SecurityException("Caller must have the android.permission.SET_HARMFUL_APP_WARNINGS permission.");
        }
        PackageStateInternal packageState = getPackageStateInternal(packageName);
        if (packageState == null) {
            throw new IllegalArgumentException("Unknown package: " + packageName);
        }
        return packageState.getUserStateOrDefault(userId).getHarmfulAppWarning();
    }

    @Override // com.android.server.pm.Computer
    public String[] filterOnlySystemPackages(String... pkgNames) {
        if (pkgNames == null) {
            return (String[]) ArrayUtils.emptyArray(String.class);
        }
        ArrayList<String> systemPackageNames = new ArrayList<>(pkgNames.length);
        for (String pkgName : pkgNames) {
            if (pkgName != null) {
                AndroidPackage pkg = getPackage(pkgName);
                if (pkg == null) {
                    Log.w("PackageManager", "Could not find package " + pkgName);
                } else if (!pkg.isSystem()) {
                    Log.w("PackageManager", pkgName + " is not system");
                } else {
                    systemPackageNames.add(pkgName);
                }
            }
        }
        return (String[]) systemPackageNames.toArray(new String[0]);
    }

    @Override // com.android.server.pm.Computer
    public List<AndroidPackage> getPackagesForAppId(int appId) {
        SettingBase settingBase = this.mSettings.getSettingBase(appId);
        if (settingBase instanceof SharedUserSetting) {
            SharedUserSetting sus = (SharedUserSetting) settingBase;
            return sus.getPackages();
        } else if (settingBase instanceof PackageSetting) {
            PackageSetting ps = (PackageSetting) settingBase;
            return List.of(ps.getPkg());
        } else {
            return Collections.emptyList();
        }
    }

    @Override // com.android.server.pm.Computer
    public int getUidTargetSdkVersion(int uid) {
        int v;
        if (Process.isSdkSandboxUid(uid)) {
            uid = getBaseSdkSandboxUid();
        }
        int appId = UserHandle.getAppId(uid);
        SettingBase settingBase = this.mSettings.getSettingBase(appId);
        if (settingBase instanceof SharedUserSetting) {
            SharedUserSetting sus = (SharedUserSetting) settingBase;
            ArraySet<? extends PackageStateInternal> packageStates = sus.getPackageStates();
            int vers = 10000;
            int numPackages = packageStates.size();
            for (int index = 0; index < numPackages; index++) {
                PackageStateInternal ps = packageStates.valueAt(index);
                if (ps.getPkg() != null && (v = ps.getPkg().getTargetSdkVersion()) < vers) {
                    vers = v;
                }
            }
            return vers;
        } else if (settingBase instanceof PackageSetting) {
            PackageSetting ps2 = (PackageSetting) settingBase;
            if (ps2.getPkg() != null) {
                return ps2.getPkg().getTargetSdkVersion();
            }
            return 10000;
        } else {
            return 10000;
        }
    }

    @Override // com.android.server.pm.Computer
    public ArrayMap<String, ProcessInfo> getProcessesForUid(int uid) {
        if (Process.isSdkSandboxUid(uid)) {
            uid = getBaseSdkSandboxUid();
        }
        int appId = UserHandle.getAppId(uid);
        SettingBase settingBase = this.mSettings.getSettingBase(appId);
        if (settingBase instanceof SharedUserSetting) {
            SharedUserSetting sus = (SharedUserSetting) settingBase;
            return PackageInfoUtils.generateProcessInfo(sus.processes, 0L);
        } else if (settingBase instanceof PackageSetting) {
            PackageSetting ps = (PackageSetting) settingBase;
            return PackageInfoUtils.generateProcessInfo(ps.getPkg().getProcesses(), 0L);
        } else {
            return null;
        }
    }

    @Override // com.android.server.pm.Computer
    public boolean getBlockUninstall(int userId, String packageName) {
        return this.mSettings.getBlockUninstall(userId, packageName);
    }

    @Override // com.android.server.pm.Computer
    public Pair<PackageStateInternal, SharedUserApi> getPackageOrSharedUser(int appId) {
        SettingBase settingBase = this.mSettings.getSettingBase(appId);
        if (settingBase instanceof SharedUserSetting) {
            return Pair.create(null, (SharedUserApi) settingBase);
        }
        if (settingBase instanceof PackageSetting) {
            return Pair.create((PackageStateInternal) settingBase, null);
        }
        return null;
    }

    private int getBaseSdkSandboxUid() {
        return getPackage(this.mService.getSdkSandboxPackageName()).getUid();
    }

    @Override // com.android.server.pm.Computer
    public SharedUserApi getSharedUser(int sharedUserAppId) {
        return this.mSettings.getSharedUserFromAppId(sharedUserAppId);
    }

    @Override // com.android.server.pm.Computer
    public ArraySet<PackageStateInternal> getSharedUserPackages(int sharedUserAppId) {
        return this.mSettings.getSharedUserPackages(sharedUserAppId);
    }

    @Override // com.android.server.pm.Computer
    public ComponentResolverApi getComponentResolver() {
        return this.mComponentResolver;
    }

    @Override // com.android.server.pm.Computer
    public PackageStateInternal getDisabledSystemPackage(String packageName) {
        return this.mSettings.getDisabledSystemPkg(packageName);
    }

    @Override // com.android.server.pm.Computer
    public ResolveInfo getInstantAppInstallerInfo() {
        return this.mInstantAppInstallerInfo;
    }

    @Override // com.android.server.pm.Computer
    public WatchedArrayMap<String, Integer> getFrozenPackages() {
        return this.mFrozenPackages;
    }

    @Override // com.android.server.pm.Computer
    public void checkPackageFrozen(String packageName) {
        if (!this.mFrozenPackages.containsKey(packageName)) {
            Slog.wtf("PackageManager", "Expected " + packageName + " to be frozen!", new Throwable());
        }
    }

    @Override // com.android.server.pm.Computer
    public ComponentName getInstantAppInstallerComponent() {
        ActivityInfo activityInfo = this.mLocalInstantAppInstallerActivity;
        if (activityInfo == null) {
            return null;
        }
        return activityInfo.getComponentName();
    }

    @Override // com.android.server.pm.Computer
    public void dumpPermissions(PrintWriter pw, String packageName, ArraySet<String> permissionNames, DumpState dumpState) {
        this.mSettings.dumpPermissions(pw, packageName, permissionNames, dumpState);
    }

    @Override // com.android.server.pm.Computer
    public void dumpPackages(PrintWriter pw, String packageName, ArraySet<String> permissionNames, DumpState dumpState, boolean checkin) {
        this.mSettings.dumpPackages(pw, packageName, permissionNames, dumpState, checkin);
    }

    @Override // com.android.server.pm.Computer
    public void dumpKeySet(PrintWriter pw, String packageName, DumpState dumpState) {
        this.mSettings.dumpKeySet(pw, packageName, dumpState);
    }

    @Override // com.android.server.pm.Computer
    public void dumpSharedUsers(PrintWriter pw, String packageName, ArraySet<String> permissionNames, DumpState dumpState, boolean checkin) {
        this.mSettings.dumpSharedUsers(pw, packageName, permissionNames, dumpState, checkin);
    }

    @Override // com.android.server.pm.Computer
    public void dumpSharedUsersProto(ProtoOutputStream proto) {
        this.mSettings.dumpSharedUsersProto(proto);
    }

    @Override // com.android.server.pm.Computer
    public void dumpPackagesProto(ProtoOutputStream proto) {
        this.mSettings.dumpPackagesProto(proto);
    }

    @Override // com.android.server.pm.Computer
    public void dumpSharedLibrariesProto(ProtoOutputStream proto) {
        this.mSharedLibraries.dumpProto(proto);
    }

    @Override // com.android.server.pm.Computer
    public List<? extends PackageStateInternal> getVolumePackages(String volumeUuid) {
        return this.mSettings.getVolumePackages(volumeUuid);
    }

    @Override // com.android.server.pm.Computer, com.android.server.pm.snapshot.PackageDataSnapshot
    public Collection<SharedUserSetting> getAllSharedUsers() {
        return this.mSettings.getAllSharedUsers();
    }

    @Override // com.android.server.pm.Computer, com.android.server.pm.snapshot.PackageDataSnapshot
    public UserInfo[] getUserInfos() {
        return this.mInjector.getUserManagerInternal().getUserInfos();
    }
}
