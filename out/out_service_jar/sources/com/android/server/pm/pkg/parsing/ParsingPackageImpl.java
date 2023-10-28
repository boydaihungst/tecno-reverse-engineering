package com.android.server.pm.pkg.parsing;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.ConfigurationInfo;
import android.content.pm.FeatureGroupInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.SigningDetails;
import android.content.res.TypedArray;
import android.hardware.usb.gadget.V1_2.GadgetFunction;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Pair;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.Parcelling;
import com.android.server.pm.pkg.component.ParsedActivity;
import com.android.server.pm.pkg.component.ParsedActivityImpl;
import com.android.server.pm.pkg.component.ParsedApexSystemService;
import com.android.server.pm.pkg.component.ParsedApexSystemServiceImpl;
import com.android.server.pm.pkg.component.ParsedAttribution;
import com.android.server.pm.pkg.component.ParsedAttributionImpl;
import com.android.server.pm.pkg.component.ParsedComponent;
import com.android.server.pm.pkg.component.ParsedInstrumentation;
import com.android.server.pm.pkg.component.ParsedInstrumentationImpl;
import com.android.server.pm.pkg.component.ParsedIntentInfo;
import com.android.server.pm.pkg.component.ParsedMainComponent;
import com.android.server.pm.pkg.component.ParsedPermission;
import com.android.server.pm.pkg.component.ParsedPermissionGroup;
import com.android.server.pm.pkg.component.ParsedPermissionGroupImpl;
import com.android.server.pm.pkg.component.ParsedPermissionImpl;
import com.android.server.pm.pkg.component.ParsedProcess;
import com.android.server.pm.pkg.component.ParsedProvider;
import com.android.server.pm.pkg.component.ParsedProviderImpl;
import com.android.server.pm.pkg.component.ParsedService;
import com.android.server.pm.pkg.component.ParsedServiceImpl;
import com.android.server.pm.pkg.component.ParsedUsesPermission;
import com.android.server.pm.pkg.component.ParsedUsesPermissionImpl;
import com.android.server.pm.pkg.parsing.ParsingUtils;
import java.security.PublicKey;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import libcore.util.EmptyArray;
/* loaded from: classes2.dex */
public class ParsingPackageImpl implements ParsingPackage, ParsingPackageHidden, Parcelable {
    protected List<ParsedActivity> activities;
    protected List<String> adoptPermissions;
    private Boolean anyDensity;
    protected List<ParsedApexSystemService> apexSystemServices;
    private String appComponentFactory;
    private List<ParsedAttribution> attributions;
    private int autoRevokePermissions;
    private String backupAgentName;
    private int banner;
    private int baseRevisionCode;
    private int category;
    private String classLoaderName;
    private String className;
    private int compatibleWidthLimitDp;
    private int compileSdkVersion;
    private String compileSdkVersionCodeName;
    private List<ConfigurationInfo> configPreferences;
    private int dataExtractionRules;
    private int descriptionRes;
    private List<FeatureGroupInfo> featureGroups;
    private int fullBackupContent;
    private int gwpAsanMode;
    private int iconRes;
    private List<String> implicitPermissions;
    private int installLocation;
    protected List<ParsedInstrumentation> instrumentations;
    private Map<String, ArraySet<PublicKey>> keySetMapping;
    private int labelRes;
    private int largestWidthLimitDp;
    private List<String> libraryNames;
    private int logo;
    protected String mBaseApkPath;
    private long mBooleans;
    private Set<String> mKnownActivityEmbeddingCerts;
    private int mLocaleConfigRes;
    private long mLongVersionCode;
    protected String mPath;
    private Map<String, PackageManager.Property> mProperties;
    private UUID mStorageUuid;
    private String manageSpaceActivityName;
    private float maxAspectRatio;
    private int maxSdkVersion;
    private int memtagMode;
    private Bundle metaData;
    private ArraySet<String> mimeGroups;
    private float minAspectRatio;
    private SparseIntArray minExtensionVersions;
    private int minSdkVersion;
    private int nativeHeapZeroInitialized;
    private int networkSecurityConfigRes;
    private CharSequence nonLocalizedLabel;
    protected List<String> originalPackages;
    private String overlayCategory;
    private int overlayPriority;
    private String overlayTarget;
    private String overlayTargetOverlayableName;
    private Map<String, String> overlayables;
    protected String packageName;
    private String permission;
    protected List<ParsedPermissionGroup> permissionGroups;
    protected List<ParsedPermission> permissions;
    private List<Pair<String, ParsedIntentInfo>> preferredActivityFilters;
    private String processName;
    private Map<String, ParsedProcess> processes;
    protected List<String> protectedBroadcasts;
    protected List<ParsedProvider> providers;
    private List<Intent> queriesIntents;
    private List<String> queriesPackages;
    private Set<String> queriesProviders;
    protected List<ParsedActivity> receivers;
    private List<FeatureInfo> reqFeatures;
    private Boolean requestRawExternalStorageAccess;
    @Deprecated
    protected List<String> requestedPermissions;
    private String requiredAccountType;
    private int requiresSmallestWidthDp;
    private Boolean resizeable;
    private Boolean resizeableActivity;
    private byte[] restrictUpdateHash;
    private String restrictedAccountType;
    private int roundIconRes;
    private String sdkLibName;
    private int sdkLibVersionMajor;
    protected List<ParsedService> services;
    private String sharedUserId;
    private int sharedUserLabel;
    private SigningDetails signingDetails;
    private String[] splitClassLoaderNames;
    protected String[] splitCodePaths;
    private SparseArray<int[]> splitDependencies;
    private int[] splitFlags;
    private String[] splitNames;
    private int[] splitRevisionCodes;
    private String staticSharedLibName;
    private long staticSharedLibVersion;
    private Boolean supportsExtraLargeScreens;
    private Boolean supportsLargeScreens;
    private Boolean supportsNormalScreens;
    private Boolean supportsSmallScreens;
    private int targetSandboxVersion;
    private int targetSdkVersion;
    private String taskAffinity;
    private int theme;
    public int themedIcon;
    private int uiOptions;
    private Set<String> upgradeKeySets;
    protected List<String> usesLibraries;
    protected List<String> usesNativeLibraries;
    protected List<String> usesOptionalLibraries;
    protected List<String> usesOptionalNativeLibraries;
    private List<ParsedUsesPermission> usesPermissions;
    private List<String> usesSdkLibraries;
    private String[][] usesSdkLibrariesCertDigests;
    private long[] usesSdkLibrariesVersionsMajor;
    private List<String> usesStaticLibraries;
    private String[][] usesStaticLibrariesCertDigests;
    private long[] usesStaticLibrariesVersions;
    protected int versionCode;
    protected int versionCodeMajor;
    private String versionName;
    protected String volumeUuid;
    private String zygotePreloadName;
    private static final SparseArray<int[]> EMPTY_INT_ARRAY_SPARSE_ARRAY = new SparseArray<>();
    public static Parcelling.BuiltIn.ForBoolean sForBoolean = Parcelling.Cache.getOrCreate(Parcelling.BuiltIn.ForBoolean.class);
    public static Parcelling.BuiltIn.ForInternedString sForInternedString = Parcelling.Cache.getOrCreate(Parcelling.BuiltIn.ForInternedString.class);
    public static Parcelling.BuiltIn.ForInternedStringArray sForInternedStringArray = Parcelling.Cache.getOrCreate(Parcelling.BuiltIn.ForInternedStringArray.class);
    public static Parcelling.BuiltIn.ForInternedStringList sForInternedStringList = Parcelling.Cache.getOrCreate(Parcelling.BuiltIn.ForInternedStringList.class);
    public static Parcelling.BuiltIn.ForInternedStringValueMap sForInternedStringValueMap = Parcelling.Cache.getOrCreate(Parcelling.BuiltIn.ForInternedStringValueMap.class);
    public static Parcelling.BuiltIn.ForStringSet sForStringSet = Parcelling.Cache.getOrCreate(Parcelling.BuiltIn.ForStringSet.class);
    public static Parcelling.BuiltIn.ForInternedStringSet sForInternedStringSet = Parcelling.Cache.getOrCreate(Parcelling.BuiltIn.ForInternedStringSet.class);
    protected static ParsingUtils.StringPairListParceler sForIntentInfoPairs = new ParsingUtils.StringPairListParceler();
    private static final Comparator<ParsedMainComponent> ORDER_COMPARATOR = new Comparator() { // from class: com.android.server.pm.pkg.parsing.ParsingPackageImpl$$ExternalSyntheticLambda0
        @Override // java.util.Comparator
        public final int compare(Object obj, Object obj2) {
            int compare;
            compare = Integer.compare(((ParsedMainComponent) obj2).getOrder(), ((ParsedMainComponent) obj).getOrder());
            return compare;
        }
    };
    public static final Parcelable.Creator<ParsingPackageImpl> CREATOR = new Parcelable.Creator<ParsingPackageImpl>() { // from class: com.android.server.pm.pkg.parsing.ParsingPackageImpl.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ParsingPackageImpl createFromParcel(Parcel source) {
            return new ParsingPackageImpl(source);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ParsingPackageImpl[] newArray(int size) {
            return new ParsingPackageImpl[size];
        }
    };

    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public /* bridge */ /* synthetic */ ParsingPackage asSplit(String[] strArr, String[] strArr2, int[] iArr, SparseArray sparseArray) {
        return asSplit(strArr, strArr2, iArr, (SparseArray<int[]>) sparseArray);
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public /* bridge */ /* synthetic */ ParsingPackage setProcesses(Map map) {
        return setProcesses((Map<String, ParsedProcess>) map);
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public /* bridge */ /* synthetic */ ParsingPackage setUpgradeKeySets(Set set) {
        return setUpgradeKeySets((Set<String>) set);
    }

    /* loaded from: classes2.dex */
    protected static class Booleans {
        private static final long ALLOW_AUDIO_PLAYBACK_CAPTURE = 2147483648L;
        private static final long ALLOW_BACKUP = 4;
        private static final long ALLOW_CLEAR_USER_DATA = 2048;
        private static final long ALLOW_CLEAR_USER_DATA_ON_FAILED_RESTORE = 1073741824;
        private static final long ALLOW_NATIVE_HEAP_POINTER_TAGGING = 68719476736L;
        private static final long ALLOW_TASK_REPARENTING = 1024;
        private static final long ATTRIBUTIONS_ARE_USER_VISIBLE = 140737488355328L;
        private static final long BACKUP_IN_FOREGROUND = 16777216;
        private static final long BASE_HARDWARE_ACCELERATED = 2;
        private static final long CANT_SAVE_STATE = 34359738368L;
        private static final long CROSS_PROFILE = 8796093022208L;
        private static final long DEBUGGABLE = 128;
        private static final long DEFAULT_TO_DEVICE_PROTECTED_STORAGE = 67108864;
        private static final long DIRECT_BOOT_AWARE = 134217728;
        private static final long DISALLOW_PROFILING = 35184372088832L;
        private static final long ENABLED = 17592186044416L;
        private static final long ENABLE_ON_BACK_INVOKED_CALLBACK = 1125899906842624L;
        private static final long EXTERNAL_STORAGE = 1;
        private static final long EXTRACT_NATIVE_LIBS = 131072;
        private static final long FORCE_QUERYABLE = 4398046511104L;
        private static final long FULL_BACKUP_ONLY = 32;
        private static final long GAME = 262144;
        private static final long HAS_CODE = 512;
        private static final long HAS_DOMAIN_URLS = 4194304;
        private static final long HAS_FRAGILE_USER_DATA = 17179869184L;
        private static final long ISOLATED_SPLIT_LOADING = 2097152;
        private static final long KILL_AFTER_RESTORE = 8;
        private static final long LARGE_HEAP = 4096;
        private static final long LEAVING_SHARED_UID = 2251799813685248L;
        private static final long MULTI_ARCH = 65536;
        private static final long OVERLAY = 1048576;
        private static final long OVERLAY_IS_STATIC = 549755813888L;
        private static final long PARTIALLY_DIRECT_BOOT_AWARE = 268435456;
        private static final long PERSISTENT = 64;
        private static final long PRESERVE_LEGACY_EXTERNAL_STORAGE = 137438953472L;
        private static final long PROFILEABLE_BY_SHELL = 8388608;
        private static final long REQUEST_FOREGROUND_SERVICE_EXEMPTION = 70368744177664L;
        private static final long REQUEST_LEGACY_EXTERNAL_STORAGE = 4294967296L;
        private static final long REQUIRED_FOR_ALL_USERS = 274877906944L;
        private static final long RESET_ENABLED_SETTINGS_ON_APP_DATA_CLEARED = 281474976710656L;
        private static final long RESIZEABLE_ACTIVITY_VIA_SDK_VERSION = 536870912;
        private static final long RESTORE_ANY_VERSION = 16;
        private static final long SDK_LIBRARY = 562949953421312L;
        private static final long STATIC_SHARED_LIBRARY = 524288;
        private static final long SUPPORTS_RTL = 16384;
        private static final long TEST_ONLY = 32768;
        private static final long USES_CLEARTEXT_TRAFFIC = 8192;
        private static final long USES_NON_SDK_API = 8589934592L;
        private static final long USE_32_BIT_ABI = 1099511627776L;
        private static final long USE_EMBEDDED_DEX = 33554432;
        private static final long VISIBLE_TO_INSTANT_APPS = 2199023255552L;
        private static final long VM_SAFE_MODE = 256;

        /* loaded from: classes2.dex */
        public @interface Values {
        }

        protected Booleans() {
        }
    }

    private ParsingPackageImpl setBoolean(long flag, boolean value) {
        if (value) {
            this.mBooleans |= flag;
        } else {
            this.mBooleans &= ~flag;
        }
        return this;
    }

    private boolean getBoolean(long flag) {
        return (this.mBooleans & flag) != 0;
    }

    public ParsingPackageImpl(String packageName, String baseApkPath, String path, TypedArray manifestArray) {
        this.overlayables = Collections.emptyMap();
        this.libraryNames = Collections.emptyList();
        this.usesLibraries = Collections.emptyList();
        this.usesOptionalLibraries = Collections.emptyList();
        this.usesNativeLibraries = Collections.emptyList();
        this.usesOptionalNativeLibraries = Collections.emptyList();
        this.usesStaticLibraries = Collections.emptyList();
        this.usesSdkLibraries = Collections.emptyList();
        this.configPreferences = Collections.emptyList();
        this.reqFeatures = Collections.emptyList();
        this.featureGroups = Collections.emptyList();
        this.originalPackages = Collections.emptyList();
        this.adoptPermissions = Collections.emptyList();
        this.requestedPermissions = Collections.emptyList();
        this.usesPermissions = Collections.emptyList();
        this.implicitPermissions = Collections.emptyList();
        this.upgradeKeySets = Collections.emptySet();
        this.keySetMapping = Collections.emptyMap();
        this.protectedBroadcasts = Collections.emptyList();
        this.activities = Collections.emptyList();
        this.apexSystemServices = Collections.emptyList();
        this.receivers = Collections.emptyList();
        this.services = Collections.emptyList();
        this.providers = Collections.emptyList();
        this.attributions = Collections.emptyList();
        this.permissions = Collections.emptyList();
        this.permissionGroups = Collections.emptyList();
        this.instrumentations = Collections.emptyList();
        this.preferredActivityFilters = Collections.emptyList();
        this.processes = Collections.emptyMap();
        this.mProperties = Collections.emptyMap();
        this.queriesIntents = Collections.emptyList();
        this.queriesPackages = Collections.emptyList();
        this.queriesProviders = Collections.emptySet();
        this.category = -1;
        this.installLocation = -1;
        this.minSdkVersion = 1;
        this.maxSdkVersion = Integer.MAX_VALUE;
        this.targetSdkVersion = 0;
        this.mBooleans = 17592186044416L;
        this.packageName = TextUtils.safeIntern(packageName);
        this.mBaseApkPath = baseApkPath;
        this.mPath = path;
        if (manifestArray != null) {
            this.versionCode = manifestArray.getInteger(1, 0);
            this.versionCodeMajor = manifestArray.getInteger(11, 0);
            setBaseRevisionCode(manifestArray.getInteger(5, 0));
            setVersionName(manifestArray.getNonConfigurationString(2, 0));
            setCompileSdkVersion(manifestArray.getInteger(9, 0));
            setCompileSdkVersionCodeName(manifestArray.getNonConfigurationString(10, 0));
            setIsolatedSplitLoading(manifestArray.getBoolean(6, false));
        }
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isSupportsSmallScreens() {
        Boolean bool = this.supportsSmallScreens;
        if (bool == null) {
            return this.targetSdkVersion >= 4;
        }
        return bool.booleanValue();
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isSupportsNormalScreens() {
        Boolean bool = this.supportsNormalScreens;
        return bool == null || bool.booleanValue();
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isSupportsLargeScreens() {
        Boolean bool = this.supportsLargeScreens;
        if (bool == null) {
            return this.targetSdkVersion >= 4;
        }
        return bool.booleanValue();
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isSupportsExtraLargeScreens() {
        Boolean bool = this.supportsExtraLargeScreens;
        if (bool == null) {
            return this.targetSdkVersion >= 9;
        }
        return bool.booleanValue();
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isResizeable() {
        Boolean bool = this.resizeable;
        if (bool == null) {
            return this.targetSdkVersion >= 4;
        }
        return bool.booleanValue();
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isAnyDensity() {
        Boolean bool = this.anyDensity;
        if (bool == null) {
            return this.targetSdkVersion >= 4;
        }
        return bool.booleanValue();
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl sortActivities() {
        Collections.sort(this.activities, ORDER_COMPARATOR);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl sortReceivers() {
        Collections.sort(this.receivers, ORDER_COMPARATOR);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl sortServices() {
        Collections.sort(this.services, ORDER_COMPARATOR);
        return this;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public Object hideAsParsed() {
        assignDerivedFields();
        return this;
    }

    private void assignDerivedFields() {
        this.mStorageUuid = StorageManager.convert(this.volumeUuid);
        this.mLongVersionCode = PackageInfo.composeLongVersionCode(this.versionCodeMajor, this.versionCode);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl addConfigPreference(ConfigurationInfo configPreference) {
        this.configPreferences = CollectionUtils.add(this.configPreferences, configPreference);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl addReqFeature(FeatureInfo reqFeature) {
        this.reqFeatures = CollectionUtils.add(this.reqFeatures, reqFeature);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl addFeatureGroup(FeatureGroupInfo featureGroup) {
        this.featureGroups = CollectionUtils.add(this.featureGroups, featureGroup);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl addProperty(PackageManager.Property property) {
        if (property == null) {
            return this;
        }
        this.mProperties = CollectionUtils.add(this.mProperties, property.getName(), property);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl addProtectedBroadcast(String protectedBroadcast) {
        if (!this.protectedBroadcasts.contains(protectedBroadcast)) {
            this.protectedBroadcasts = CollectionUtils.add(this.protectedBroadcasts, TextUtils.safeIntern(protectedBroadcast));
        }
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl addInstrumentation(ParsedInstrumentation instrumentation) {
        this.instrumentations = CollectionUtils.add(this.instrumentations, instrumentation);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl addOriginalPackage(String originalPackage) {
        this.originalPackages = CollectionUtils.add(this.originalPackages, originalPackage);
        return this;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackage addOverlayable(String overlayableName, String actorName) {
        this.overlayables = CollectionUtils.add(this.overlayables, overlayableName, TextUtils.safeIntern(actorName));
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl addAdoptPermission(String adoptPermission) {
        this.adoptPermissions = CollectionUtils.add(this.adoptPermissions, TextUtils.safeIntern(adoptPermission));
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl addPermission(ParsedPermission permission) {
        this.permissions = CollectionUtils.add(this.permissions, permission);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl addPermissionGroup(ParsedPermissionGroup permissionGroup) {
        this.permissionGroups = CollectionUtils.add(this.permissionGroups, permissionGroup);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl addUsesPermission(ParsedUsesPermission permission) {
        this.usesPermissions = CollectionUtils.add(this.usesPermissions, permission);
        this.requestedPermissions = CollectionUtils.add(this.requestedPermissions, permission.getName());
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl addImplicitPermission(String permission) {
        addUsesPermission((ParsedUsesPermission) new ParsedUsesPermissionImpl(permission, 0));
        this.implicitPermissions = CollectionUtils.add(this.implicitPermissions, TextUtils.safeIntern(permission));
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl addKeySet(String keySetName, PublicKey publicKey) {
        ArraySet<PublicKey> publicKeys = this.keySetMapping.get(keySetName);
        if (publicKeys == null) {
            publicKeys = new ArraySet<>();
        }
        publicKeys.add(publicKey);
        this.keySetMapping = CollectionUtils.add(this.keySetMapping, keySetName, publicKeys);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl addActivity(ParsedActivity parsedActivity) {
        this.activities = CollectionUtils.add(this.activities, parsedActivity);
        addMimeGroupsFromComponent(parsedActivity);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public final ParsingPackageImpl addApexSystemService(ParsedApexSystemService parsedApexSystemService) {
        this.apexSystemServices = CollectionUtils.add(this.apexSystemServices, parsedApexSystemService);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl addReceiver(ParsedActivity parsedReceiver) {
        this.receivers = CollectionUtils.add(this.receivers, parsedReceiver);
        addMimeGroupsFromComponent(parsedReceiver);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl addService(ParsedService parsedService) {
        this.services = CollectionUtils.add(this.services, parsedService);
        addMimeGroupsFromComponent(parsedService);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl addProvider(ParsedProvider parsedProvider) {
        this.providers = CollectionUtils.add(this.providers, parsedProvider);
        addMimeGroupsFromComponent(parsedProvider);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl addAttribution(ParsedAttribution attribution) {
        this.attributions = CollectionUtils.add(this.attributions, attribution);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl addLibraryName(String libraryName) {
        this.libraryNames = CollectionUtils.add(this.libraryNames, TextUtils.safeIntern(libraryName));
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl addUsesOptionalLibrary(String libraryName) {
        this.usesOptionalLibraries = CollectionUtils.add(this.usesOptionalLibraries, TextUtils.safeIntern(libraryName));
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl addUsesLibrary(String libraryName) {
        this.usesLibraries = CollectionUtils.add(this.usesLibraries, TextUtils.safeIntern(libraryName));
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage, com.android.server.pm.parsing.pkg.ParsedPackage
    public ParsingPackageImpl removeUsesOptionalLibrary(String libraryName) {
        this.usesOptionalLibraries = CollectionUtils.remove(this.usesOptionalLibraries, libraryName);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public final ParsingPackageImpl addUsesOptionalNativeLibrary(String libraryName) {
        this.usesOptionalNativeLibraries = CollectionUtils.add(this.usesOptionalNativeLibraries, TextUtils.safeIntern(libraryName));
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public final ParsingPackageImpl addUsesNativeLibrary(String libraryName) {
        this.usesNativeLibraries = CollectionUtils.add(this.usesNativeLibraries, TextUtils.safeIntern(libraryName));
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl removeUsesOptionalNativeLibrary(String libraryName) {
        this.usesOptionalNativeLibraries = CollectionUtils.remove(this.usesOptionalNativeLibraries, libraryName);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl addUsesSdkLibrary(String libraryName, long versionMajor, String[] certSha256Digests) {
        this.usesSdkLibraries = CollectionUtils.add(this.usesSdkLibraries, TextUtils.safeIntern(libraryName));
        this.usesSdkLibrariesVersionsMajor = ArrayUtils.appendLong(this.usesSdkLibrariesVersionsMajor, versionMajor, true);
        this.usesSdkLibrariesCertDigests = (String[][]) ArrayUtils.appendElement(String[].class, this.usesSdkLibrariesCertDigests, certSha256Digests, true);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl addUsesStaticLibrary(String libraryName, long version, String[] certSha256Digests) {
        this.usesStaticLibraries = CollectionUtils.add(this.usesStaticLibraries, TextUtils.safeIntern(libraryName));
        this.usesStaticLibrariesVersions = ArrayUtils.appendLong(this.usesStaticLibrariesVersions, version, true);
        this.usesStaticLibrariesCertDigests = (String[][]) ArrayUtils.appendElement(String[].class, this.usesStaticLibrariesCertDigests, certSha256Digests, true);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl addPreferredActivityFilter(String className, ParsedIntentInfo intentInfo) {
        this.preferredActivityFilters = CollectionUtils.add(this.preferredActivityFilters, Pair.create(className, intentInfo));
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl addQueriesIntent(Intent intent) {
        this.queriesIntents = CollectionUtils.add(this.queriesIntents, intent);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl addQueriesPackage(String packageName) {
        this.queriesPackages = CollectionUtils.add(this.queriesPackages, TextUtils.safeIntern(packageName));
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl addQueriesProvider(String authority) {
        this.queriesProviders = CollectionUtils.add(this.queriesProviders, authority);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setSupportsSmallScreens(int supportsSmallScreens) {
        if (supportsSmallScreens == 1) {
            return this;
        }
        this.supportsSmallScreens = Boolean.valueOf(supportsSmallScreens < 0);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setSupportsNormalScreens(int supportsNormalScreens) {
        if (supportsNormalScreens == 1) {
            return this;
        }
        this.supportsNormalScreens = Boolean.valueOf(supportsNormalScreens < 0);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setSupportsLargeScreens(int supportsLargeScreens) {
        if (supportsLargeScreens == 1) {
            return this;
        }
        this.supportsLargeScreens = Boolean.valueOf(supportsLargeScreens < 0);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setSupportsExtraLargeScreens(int supportsExtraLargeScreens) {
        if (supportsExtraLargeScreens == 1) {
            return this;
        }
        this.supportsExtraLargeScreens = Boolean.valueOf(supportsExtraLargeScreens < 0);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setResizeable(int resizeable) {
        if (resizeable == 1) {
            return this;
        }
        this.resizeable = Boolean.valueOf(resizeable < 0);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setAnyDensity(int anyDensity) {
        if (anyDensity == 1) {
            return this;
        }
        this.anyDensity = Boolean.valueOf(anyDensity < 0);
        return this;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl asSplit(String[] splitNames, String[] splitCodePaths, int[] splitRevisionCodes, SparseArray<int[]> splitDependencies) {
        this.splitNames = splitNames;
        this.splitCodePaths = splitCodePaths;
        this.splitRevisionCodes = splitRevisionCodes;
        this.splitDependencies = splitDependencies;
        int count = splitNames.length;
        this.splitFlags = new int[count];
        this.splitClassLoaderNames = new String[count];
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setSplitHasCode(int splitIndex, boolean splitHasCode) {
        int i;
        int[] iArr = this.splitFlags;
        if (splitHasCode) {
            i = iArr[splitIndex] | 4;
        } else {
            i = iArr[splitIndex] & (-5);
        }
        iArr[splitIndex] = i;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setSplitClassLoaderName(int splitIndex, String classLoaderName) {
        this.splitClassLoaderNames[splitIndex] = classLoaderName;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setRequiredAccountType(String requiredAccountType) {
        this.requiredAccountType = TextUtils.nullIfEmpty(requiredAccountType);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setOverlayTarget(String overlayTarget) {
        this.overlayTarget = TextUtils.safeIntern(overlayTarget);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setVolumeUuid(String volumeUuid) {
        this.volumeUuid = TextUtils.safeIntern(volumeUuid);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setStaticSharedLibName(String staticSharedLibName) {
        this.staticSharedLibName = TextUtils.safeIntern(staticSharedLibName);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setSharedUserId(String sharedUserId) {
        this.sharedUserId = TextUtils.safeIntern(sharedUserId);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setNonLocalizedLabel(CharSequence value) {
        this.nonLocalizedLabel = value == null ? null : value.toString().trim();
        return this;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public String getProcessName() {
        String str = this.processName;
        return str != null ? str : this.packageName;
    }

    public String toString() {
        return "Package{" + Integer.toHexString(System.identityHashCode(this)) + " " + this.packageName + "}";
    }

    @Deprecated
    public ApplicationInfo toAppInfoWithoutState() {
        ApplicationInfo appInfo = toAppInfoWithoutStateWithoutFlags();
        appInfo.flags = PackageInfoWithoutStateUtils.appInfoFlags(this);
        appInfo.privateFlags = PackageInfoWithoutStateUtils.appInfoPrivateFlags(this);
        appInfo.privateFlagsExt = PackageInfoWithoutStateUtils.appInfoPrivateFlagsExt(this);
        return appInfo;
    }

    public ApplicationInfo toAppInfoWithoutStateWithoutFlags() {
        int i;
        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.appComponentFactory = this.appComponentFactory;
        appInfo.backupAgentName = this.backupAgentName;
        appInfo.banner = this.banner;
        appInfo.category = this.category;
        appInfo.classLoaderName = this.classLoaderName;
        appInfo.className = this.className;
        appInfo.compatibleWidthLimitDp = this.compatibleWidthLimitDp;
        appInfo.compileSdkVersion = this.compileSdkVersion;
        appInfo.compileSdkVersionCodename = this.compileSdkVersionCodeName;
        appInfo.crossProfile = isCrossProfile();
        appInfo.descriptionRes = this.descriptionRes;
        appInfo.enabled = getBoolean(17592186044416L);
        appInfo.fullBackupContent = this.fullBackupContent;
        appInfo.dataExtractionRulesRes = this.dataExtractionRules;
        if (!ParsingPackageUtils.sUseRoundIcon || (i = this.roundIconRes) == 0) {
            i = this.iconRes;
        }
        appInfo.icon = i;
        appInfo.iconRes = this.iconRes;
        appInfo.roundIconRes = this.roundIconRes;
        appInfo.installLocation = this.installLocation;
        appInfo.labelRes = this.labelRes;
        appInfo.largestWidthLimitDp = this.largestWidthLimitDp;
        appInfo.logo = this.logo;
        appInfo.manageSpaceActivityName = this.manageSpaceActivityName;
        appInfo.maxAspectRatio = this.maxAspectRatio;
        appInfo.metaData = this.metaData;
        appInfo.minAspectRatio = this.minAspectRatio;
        appInfo.minSdkVersion = this.minSdkVersion;
        appInfo.name = this.className;
        appInfo.networkSecurityConfigRes = this.networkSecurityConfigRes;
        appInfo.nonLocalizedLabel = this.nonLocalizedLabel;
        appInfo.packageName = this.packageName;
        appInfo.permission = this.permission;
        appInfo.processName = getProcessName();
        appInfo.requiresSmallestWidthDp = this.requiresSmallestWidthDp;
        appInfo.splitClassLoaderNames = this.splitClassLoaderNames;
        SparseArray<int[]> sparseArray = this.splitDependencies;
        appInfo.splitDependencies = (sparseArray == null || sparseArray.size() == 0) ? null : this.splitDependencies;
        appInfo.splitNames = this.splitNames;
        appInfo.storageUuid = this.mStorageUuid;
        appInfo.targetSandboxVersion = this.targetSandboxVersion;
        appInfo.targetSdkVersion = this.targetSdkVersion;
        appInfo.taskAffinity = this.taskAffinity;
        appInfo.theme = this.theme;
        appInfo.uiOptions = this.uiOptions;
        appInfo.volumeUuid = this.volumeUuid;
        appInfo.zygotePreloadName = this.zygotePreloadName;
        appInfo.setGwpAsanMode(this.gwpAsanMode);
        appInfo.setMemtagMode(this.memtagMode);
        appInfo.setNativeHeapZeroInitialized(this.nativeHeapZeroInitialized);
        appInfo.setRequestRawExternalStorageAccess(this.requestRawExternalStorageAccess);
        appInfo.setBaseCodePath(this.mBaseApkPath);
        appInfo.setBaseResourcePath(this.mBaseApkPath);
        appInfo.setCodePath(this.mPath);
        appInfo.setResourcePath(this.mPath);
        appInfo.setSplitCodePaths(ArrayUtils.size(this.splitCodePaths) == 0 ? null : this.splitCodePaths);
        appInfo.setSplitResourcePaths(ArrayUtils.size(this.splitCodePaths) != 0 ? this.splitCodePaths : null);
        appInfo.setVersionCode(this.mLongVersionCode);
        appInfo.setAppClassNamesByProcess(buildAppClassNamesByProcess());
        appInfo.setLocaleConfigRes(this.mLocaleConfigRes);
        Set<String> set = this.mKnownActivityEmbeddingCerts;
        if (set != null) {
            appInfo.setKnownActivityEmbeddingCerts(set);
        }
        return appInfo;
    }

    private ArrayMap<String, String> buildAppClassNamesByProcess() {
        if (ArrayUtils.size(this.processes) == 0) {
            return null;
        }
        ArrayMap<String, String> ret = new ArrayMap<>(4);
        for (String processName : this.processes.keySet()) {
            ParsedProcess process = this.processes.get(processName);
            ArrayMap<String, String> appClassesByPackage = process.getAppClassNamesByPackage();
            for (int i = 0; i < appClassesByPackage.size(); i++) {
                String packageName = appClassesByPackage.keyAt(i);
                if (this.packageName.equals(packageName)) {
                    String appClassName = appClassesByPackage.valueAt(i);
                    if (!TextUtils.isEmpty(appClassName)) {
                        ret.put(processName, appClassName);
                    }
                }
            }
        }
        return ret;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        sForBoolean.parcel(this.supportsSmallScreens, dest, flags);
        sForBoolean.parcel(this.supportsNormalScreens, dest, flags);
        sForBoolean.parcel(this.supportsLargeScreens, dest, flags);
        sForBoolean.parcel(this.supportsExtraLargeScreens, dest, flags);
        sForBoolean.parcel(this.resizeable, dest, flags);
        sForBoolean.parcel(this.anyDensity, dest, flags);
        dest.writeInt(this.versionCode);
        dest.writeInt(this.versionCodeMajor);
        dest.writeInt(this.baseRevisionCode);
        sForInternedString.parcel(this.versionName, dest, flags);
        dest.writeInt(this.compileSdkVersion);
        dest.writeString(this.compileSdkVersionCodeName);
        sForInternedString.parcel(this.packageName, dest, flags);
        dest.writeString(this.mBaseApkPath);
        dest.writeString(this.restrictedAccountType);
        dest.writeString(this.requiredAccountType);
        sForInternedString.parcel(this.overlayTarget, dest, flags);
        dest.writeString(this.overlayTargetOverlayableName);
        dest.writeString(this.overlayCategory);
        dest.writeInt(this.overlayPriority);
        sForInternedStringValueMap.parcel(this.overlayables, dest, flags);
        sForInternedString.parcel(this.sdkLibName, dest, flags);
        dest.writeInt(this.sdkLibVersionMajor);
        sForInternedString.parcel(this.staticSharedLibName, dest, flags);
        dest.writeLong(this.staticSharedLibVersion);
        sForInternedStringList.parcel(this.libraryNames, dest, flags);
        sForInternedStringList.parcel(this.usesLibraries, dest, flags);
        sForInternedStringList.parcel(this.usesOptionalLibraries, dest, flags);
        sForInternedStringList.parcel(this.usesNativeLibraries, dest, flags);
        sForInternedStringList.parcel(this.usesOptionalNativeLibraries, dest, flags);
        sForInternedStringList.parcel(this.usesStaticLibraries, dest, flags);
        dest.writeLongArray(this.usesStaticLibrariesVersions);
        String[][] strArr = this.usesStaticLibrariesCertDigests;
        if (strArr == null) {
            dest.writeInt(-1);
        } else {
            dest.writeInt(strArr.length);
            int index = 0;
            while (true) {
                String[][] strArr2 = this.usesStaticLibrariesCertDigests;
                if (index >= strArr2.length) {
                    break;
                }
                dest.writeStringArray(strArr2[index]);
                index++;
            }
        }
        sForInternedStringList.parcel(this.usesSdkLibraries, dest, flags);
        dest.writeLongArray(this.usesSdkLibrariesVersionsMajor);
        String[][] strArr3 = this.usesSdkLibrariesCertDigests;
        if (strArr3 == null) {
            dest.writeInt(-1);
        } else {
            dest.writeInt(strArr3.length);
            int index2 = 0;
            while (true) {
                String[][] strArr4 = this.usesSdkLibrariesCertDigests;
                if (index2 >= strArr4.length) {
                    break;
                }
                dest.writeStringArray(strArr4[index2]);
                index2++;
            }
        }
        sForInternedString.parcel(this.sharedUserId, dest, flags);
        dest.writeInt(this.sharedUserLabel);
        dest.writeTypedList(this.configPreferences);
        dest.writeTypedList(this.reqFeatures);
        dest.writeTypedList(this.featureGroups);
        dest.writeByteArray(this.restrictUpdateHash);
        dest.writeStringList(this.originalPackages);
        sForInternedStringList.parcel(this.adoptPermissions, dest, flags);
        sForInternedStringList.parcel(this.requestedPermissions, dest, flags);
        ParsingUtils.writeParcelableList(dest, this.usesPermissions);
        sForInternedStringList.parcel(this.implicitPermissions, dest, flags);
        sForStringSet.parcel(this.upgradeKeySets, dest, flags);
        ParsingPackageUtils.writeKeySetMapping(dest, this.keySetMapping);
        sForInternedStringList.parcel(this.protectedBroadcasts, dest, flags);
        ParsingUtils.writeParcelableList(dest, this.activities);
        ParsingUtils.writeParcelableList(dest, this.apexSystemServices);
        ParsingUtils.writeParcelableList(dest, this.receivers);
        ParsingUtils.writeParcelableList(dest, this.services);
        ParsingUtils.writeParcelableList(dest, this.providers);
        ParsingUtils.writeParcelableList(dest, this.attributions);
        ParsingUtils.writeParcelableList(dest, this.permissions);
        ParsingUtils.writeParcelableList(dest, this.permissionGroups);
        ParsingUtils.writeParcelableList(dest, this.instrumentations);
        sForIntentInfoPairs.parcel(this.preferredActivityFilters, dest, flags);
        dest.writeMap(this.processes);
        dest.writeBundle(this.metaData);
        sForInternedString.parcel(this.volumeUuid, dest, flags);
        dest.writeParcelable(this.signingDetails, flags);
        dest.writeString(this.mPath);
        dest.writeTypedList(this.queriesIntents, flags);
        sForInternedStringList.parcel(this.queriesPackages, dest, flags);
        sForInternedStringSet.parcel(this.queriesProviders, dest, flags);
        dest.writeString(this.appComponentFactory);
        dest.writeString(this.backupAgentName);
        dest.writeInt(this.banner);
        dest.writeInt(this.category);
        dest.writeString(this.classLoaderName);
        dest.writeString(this.className);
        dest.writeInt(this.compatibleWidthLimitDp);
        dest.writeInt(this.descriptionRes);
        dest.writeInt(this.fullBackupContent);
        dest.writeInt(this.dataExtractionRules);
        dest.writeInt(this.iconRes);
        dest.writeInt(this.installLocation);
        dest.writeInt(this.labelRes);
        dest.writeInt(this.largestWidthLimitDp);
        dest.writeInt(this.logo);
        dest.writeString(this.manageSpaceActivityName);
        dest.writeFloat(this.maxAspectRatio);
        dest.writeFloat(this.minAspectRatio);
        dest.writeInt(this.minSdkVersion);
        dest.writeInt(this.maxSdkVersion);
        dest.writeInt(this.networkSecurityConfigRes);
        dest.writeCharSequence(this.nonLocalizedLabel);
        dest.writeString(this.permission);
        dest.writeString(this.processName);
        dest.writeInt(this.requiresSmallestWidthDp);
        dest.writeInt(this.roundIconRes);
        dest.writeInt(this.targetSandboxVersion);
        dest.writeInt(this.targetSdkVersion);
        dest.writeString(this.taskAffinity);
        dest.writeInt(this.theme);
        dest.writeInt(this.uiOptions);
        dest.writeString(this.zygotePreloadName);
        dest.writeStringArray(this.splitClassLoaderNames);
        dest.writeStringArray(this.splitCodePaths);
        dest.writeSparseArray(this.splitDependencies);
        dest.writeIntArray(this.splitFlags);
        dest.writeStringArray(this.splitNames);
        dest.writeIntArray(this.splitRevisionCodes);
        sForBoolean.parcel(this.resizeableActivity, dest, flags);
        dest.writeInt(this.autoRevokePermissions);
        dest.writeArraySet(this.mimeGroups);
        dest.writeInt(this.gwpAsanMode);
        dest.writeSparseIntArray(this.minExtensionVersions);
        dest.writeLong(this.mBooleans);
        dest.writeMap(this.mProperties);
        dest.writeInt(this.memtagMode);
        dest.writeInt(this.nativeHeapZeroInitialized);
        sForBoolean.parcel(this.requestRawExternalStorageAccess, dest, flags);
        dest.writeInt(this.mLocaleConfigRes);
        sForStringSet.parcel(this.mKnownActivityEmbeddingCerts, dest, flags);
    }

    public ParsingPackageImpl(Parcel in) {
        this.overlayables = Collections.emptyMap();
        this.libraryNames = Collections.emptyList();
        this.usesLibraries = Collections.emptyList();
        this.usesOptionalLibraries = Collections.emptyList();
        this.usesNativeLibraries = Collections.emptyList();
        this.usesOptionalNativeLibraries = Collections.emptyList();
        this.usesStaticLibraries = Collections.emptyList();
        this.usesSdkLibraries = Collections.emptyList();
        this.configPreferences = Collections.emptyList();
        this.reqFeatures = Collections.emptyList();
        this.featureGroups = Collections.emptyList();
        this.originalPackages = Collections.emptyList();
        this.adoptPermissions = Collections.emptyList();
        this.requestedPermissions = Collections.emptyList();
        this.usesPermissions = Collections.emptyList();
        this.implicitPermissions = Collections.emptyList();
        this.upgradeKeySets = Collections.emptySet();
        this.keySetMapping = Collections.emptyMap();
        this.protectedBroadcasts = Collections.emptyList();
        this.activities = Collections.emptyList();
        this.apexSystemServices = Collections.emptyList();
        this.receivers = Collections.emptyList();
        this.services = Collections.emptyList();
        this.providers = Collections.emptyList();
        this.attributions = Collections.emptyList();
        this.permissions = Collections.emptyList();
        this.permissionGroups = Collections.emptyList();
        this.instrumentations = Collections.emptyList();
        this.preferredActivityFilters = Collections.emptyList();
        this.processes = Collections.emptyMap();
        this.mProperties = Collections.emptyMap();
        this.queriesIntents = Collections.emptyList();
        this.queriesPackages = Collections.emptyList();
        this.queriesProviders = Collections.emptySet();
        this.category = -1;
        this.installLocation = -1;
        this.minSdkVersion = 1;
        this.maxSdkVersion = Integer.MAX_VALUE;
        this.targetSdkVersion = 0;
        this.mBooleans = 17592186044416L;
        ClassLoader boot = Object.class.getClassLoader();
        this.supportsSmallScreens = sForBoolean.unparcel(in);
        this.supportsNormalScreens = sForBoolean.unparcel(in);
        this.supportsLargeScreens = sForBoolean.unparcel(in);
        this.supportsExtraLargeScreens = sForBoolean.unparcel(in);
        this.resizeable = sForBoolean.unparcel(in);
        this.anyDensity = sForBoolean.unparcel(in);
        this.versionCode = in.readInt();
        this.versionCodeMajor = in.readInt();
        this.baseRevisionCode = in.readInt();
        this.versionName = sForInternedString.unparcel(in);
        this.compileSdkVersion = in.readInt();
        this.compileSdkVersionCodeName = in.readString();
        this.packageName = sForInternedString.unparcel(in);
        this.mBaseApkPath = in.readString();
        this.restrictedAccountType = in.readString();
        this.requiredAccountType = in.readString();
        this.overlayTarget = sForInternedString.unparcel(in);
        this.overlayTargetOverlayableName = in.readString();
        this.overlayCategory = in.readString();
        this.overlayPriority = in.readInt();
        this.overlayables = sForInternedStringValueMap.unparcel(in);
        this.sdkLibName = sForInternedString.unparcel(in);
        this.sdkLibVersionMajor = in.readInt();
        this.staticSharedLibName = sForInternedString.unparcel(in);
        this.staticSharedLibVersion = in.readLong();
        this.libraryNames = sForInternedStringList.unparcel(in);
        this.usesLibraries = sForInternedStringList.unparcel(in);
        this.usesOptionalLibraries = sForInternedStringList.unparcel(in);
        this.usesNativeLibraries = sForInternedStringList.unparcel(in);
        this.usesOptionalNativeLibraries = sForInternedStringList.unparcel(in);
        this.usesStaticLibraries = sForInternedStringList.unparcel(in);
        this.usesStaticLibrariesVersions = in.createLongArray();
        int digestsSize = in.readInt();
        if (digestsSize >= 0) {
            this.usesStaticLibrariesCertDigests = new String[digestsSize];
            for (int index = 0; index < digestsSize; index++) {
                this.usesStaticLibrariesCertDigests[index] = sForInternedStringArray.unparcel(in);
            }
        }
        this.usesSdkLibraries = sForInternedStringList.unparcel(in);
        this.usesSdkLibrariesVersionsMajor = in.createLongArray();
        int digestsSize2 = in.readInt();
        if (digestsSize2 >= 0) {
            this.usesSdkLibrariesCertDigests = new String[digestsSize2];
            for (int index2 = 0; index2 < digestsSize2; index2++) {
                this.usesSdkLibrariesCertDigests[index2] = sForInternedStringArray.unparcel(in);
            }
        }
        this.sharedUserId = sForInternedString.unparcel(in);
        this.sharedUserLabel = in.readInt();
        this.configPreferences = in.createTypedArrayList(ConfigurationInfo.CREATOR);
        this.reqFeatures = in.createTypedArrayList(FeatureInfo.CREATOR);
        this.featureGroups = in.createTypedArrayList(FeatureGroupInfo.CREATOR);
        this.restrictUpdateHash = in.createByteArray();
        this.originalPackages = in.createStringArrayList();
        this.adoptPermissions = sForInternedStringList.unparcel(in);
        this.requestedPermissions = sForInternedStringList.unparcel(in);
        this.usesPermissions = ParsingUtils.createTypedInterfaceList(in, ParsedUsesPermissionImpl.CREATOR);
        this.implicitPermissions = sForInternedStringList.unparcel(in);
        this.upgradeKeySets = sForStringSet.unparcel(in);
        this.keySetMapping = ParsingPackageUtils.readKeySetMapping(in);
        this.protectedBroadcasts = sForInternedStringList.unparcel(in);
        this.activities = ParsingUtils.createTypedInterfaceList(in, ParsedActivityImpl.CREATOR);
        this.apexSystemServices = ParsingUtils.createTypedInterfaceList(in, ParsedApexSystemServiceImpl.CREATOR);
        this.receivers = ParsingUtils.createTypedInterfaceList(in, ParsedActivityImpl.CREATOR);
        this.services = ParsingUtils.createTypedInterfaceList(in, ParsedServiceImpl.CREATOR);
        this.providers = ParsingUtils.createTypedInterfaceList(in, ParsedProviderImpl.CREATOR);
        this.attributions = ParsingUtils.createTypedInterfaceList(in, ParsedAttributionImpl.CREATOR);
        this.permissions = ParsingUtils.createTypedInterfaceList(in, ParsedPermissionImpl.CREATOR);
        this.permissionGroups = ParsingUtils.createTypedInterfaceList(in, ParsedPermissionGroupImpl.CREATOR);
        this.instrumentations = ParsingUtils.createTypedInterfaceList(in, ParsedInstrumentationImpl.CREATOR);
        this.preferredActivityFilters = sForIntentInfoPairs.unparcel(in);
        this.processes = in.readHashMap(ParsedProcess.class.getClassLoader());
        this.metaData = in.readBundle(boot);
        this.volumeUuid = sForInternedString.unparcel(in);
        this.signingDetails = in.readParcelable(boot);
        this.mPath = in.readString();
        this.queriesIntents = in.createTypedArrayList(Intent.CREATOR);
        this.queriesPackages = sForInternedStringList.unparcel(in);
        this.queriesProviders = sForInternedStringSet.unparcel(in);
        this.appComponentFactory = in.readString();
        this.backupAgentName = in.readString();
        this.banner = in.readInt();
        this.category = in.readInt();
        this.classLoaderName = in.readString();
        this.className = in.readString();
        this.compatibleWidthLimitDp = in.readInt();
        this.descriptionRes = in.readInt();
        this.fullBackupContent = in.readInt();
        this.dataExtractionRules = in.readInt();
        this.iconRes = in.readInt();
        this.installLocation = in.readInt();
        this.labelRes = in.readInt();
        this.largestWidthLimitDp = in.readInt();
        this.logo = in.readInt();
        this.manageSpaceActivityName = in.readString();
        this.maxAspectRatio = in.readFloat();
        this.minAspectRatio = in.readFloat();
        this.minSdkVersion = in.readInt();
        this.maxSdkVersion = in.readInt();
        this.networkSecurityConfigRes = in.readInt();
        this.nonLocalizedLabel = in.readCharSequence();
        this.permission = in.readString();
        this.processName = in.readString();
        this.requiresSmallestWidthDp = in.readInt();
        this.roundIconRes = in.readInt();
        this.targetSandboxVersion = in.readInt();
        this.targetSdkVersion = in.readInt();
        this.taskAffinity = in.readString();
        this.theme = in.readInt();
        this.uiOptions = in.readInt();
        this.zygotePreloadName = in.readString();
        this.splitClassLoaderNames = in.createStringArray();
        this.splitCodePaths = in.createStringArray();
        this.splitDependencies = in.readSparseArray(boot);
        this.splitFlags = in.createIntArray();
        this.splitNames = in.createStringArray();
        this.splitRevisionCodes = in.createIntArray();
        this.resizeableActivity = sForBoolean.unparcel(in);
        this.autoRevokePermissions = in.readInt();
        this.mimeGroups = in.readArraySet(boot);
        this.gwpAsanMode = in.readInt();
        this.minExtensionVersions = in.readSparseIntArray();
        this.mBooleans = in.readLong();
        this.mProperties = in.readHashMap(boot);
        this.memtagMode = in.readInt();
        this.nativeHeapZeroInitialized = in.readInt();
        this.requestRawExternalStorageAccess = sForBoolean.unparcel(in);
        this.mLocaleConfigRes = in.readInt();
        this.mKnownActivityEmbeddingCerts = sForStringSet.unparcel(in);
        assignDerivedFields();
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageHidden
    public int getVersionCode() {
        return this.versionCode;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageHidden
    public int getVersionCodeMajor() {
        return this.versionCodeMajor;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo, com.android.server.pm.pkg.parsing.PkgWithoutStatePackageInfo, com.android.server.pm.pkg.AndroidPackageApi
    public long getLongVersionCode() {
        return this.mLongVersionCode;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStatePackageInfo
    public int getBaseRevisionCode() {
        return this.baseRevisionCode;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStatePackageInfo
    public String getVersionName() {
        return this.versionName;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo, com.android.server.pm.pkg.parsing.PkgWithoutStatePackageInfo
    public int getCompileSdkVersion() {
        return this.compileSdkVersion;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo, com.android.server.pm.pkg.parsing.PkgWithoutStatePackageInfo
    public String getCompileSdkVersionCodeName() {
        return this.compileSdkVersionCodeName;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStatePackageInfo
    public String getPackageName() {
        return this.packageName;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public String getBaseApkPath() {
        return this.mBaseApkPath;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStatePackageInfo
    public boolean isRequiredForAllUsers() {
        return getBoolean(274877906944L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStatePackageInfo
    public String getRestrictedAccountType() {
        return this.restrictedAccountType;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStatePackageInfo
    public String getRequiredAccountType() {
        return this.requiredAccountType;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageInternal
    public String getOverlayTarget() {
        return this.overlayTarget;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageInternal
    public String getOverlayTargetOverlayableName() {
        return this.overlayTargetOverlayableName;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageInternal
    public String getOverlayCategory() {
        return this.overlayCategory;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageInternal
    public int getOverlayPriority() {
        return this.overlayPriority;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageInternal
    public boolean isOverlayIsStatic() {
        return getBoolean(549755813888L);
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public Map<String, String> getOverlayables() {
        return this.overlayables;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public String getSdkLibName() {
        return this.sdkLibName;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public int getSdkLibVersionMajor() {
        return this.sdkLibVersionMajor;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public String getStaticSharedLibName() {
        return this.staticSharedLibName;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public long getStaticSharedLibVersion() {
        return this.staticSharedLibVersion;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public List<String> getLibraryNames() {
        return this.libraryNames;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public List<String> getUsesLibraries() {
        return this.usesLibraries;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public List<String> getUsesOptionalLibraries() {
        return this.usesOptionalLibraries;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public List<String> getUsesNativeLibraries() {
        return this.usesNativeLibraries;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public List<String> getUsesOptionalNativeLibraries() {
        return this.usesOptionalNativeLibraries;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public List<String> getUsesStaticLibraries() {
        return this.usesStaticLibraries;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public long[] getUsesStaticLibrariesVersions() {
        return this.usesStaticLibrariesVersions;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public String[][] getUsesStaticLibrariesCertDigests() {
        return this.usesStaticLibrariesCertDigests;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public List<String> getUsesSdkLibraries() {
        return this.usesSdkLibraries;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public long[] getUsesSdkLibrariesVersionsMajor() {
        return this.usesSdkLibrariesVersionsMajor;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public String[][] getUsesSdkLibrariesCertDigests() {
        return this.usesSdkLibrariesCertDigests;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStatePackageInfo
    public String getSharedUserId() {
        return this.sharedUserId;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStatePackageInfo
    public int getSharedUserLabel() {
        return this.sharedUserLabel;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStatePackageInfo
    public List<ConfigurationInfo> getConfigPreferences() {
        return this.configPreferences;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStatePackageInfo
    public List<FeatureInfo> getRequestedFeatures() {
        return this.reqFeatures;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStatePackageInfo
    public List<FeatureGroupInfo> getFeatureGroups() {
        return this.featureGroups;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public byte[] getRestrictUpdateHash() {
        return this.restrictUpdateHash;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public List<String> getOriginalPackages() {
        return this.originalPackages;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public List<String> getAdoptPermissions() {
        return this.adoptPermissions;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStatePackageInfo
    @Deprecated
    public List<String> getRequestedPermissions() {
        return this.requestedPermissions;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public List<ParsedUsesPermission> getUsesPermissions() {
        return this.usesPermissions;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public List<String> getImplicitPermissions() {
        return this.implicitPermissions;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public Map<String, PackageManager.Property> getProperties() {
        return this.mProperties;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public Set<String> getUpgradeKeySets() {
        return this.upgradeKeySets;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public Map<String, ArraySet<PublicKey>> getKeySetMapping() {
        return this.keySetMapping;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public List<String> getProtectedBroadcasts() {
        return this.protectedBroadcasts;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStatePackageInfo
    public List<ParsedActivity> getActivities() {
        return this.activities;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public List<ParsedApexSystemService> getApexSystemServices() {
        return this.apexSystemServices;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStatePackageInfo
    public List<ParsedActivity> getReceivers() {
        return this.receivers;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStatePackageInfo
    public List<ParsedService> getServices() {
        return this.services;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStatePackageInfo
    public List<ParsedProvider> getProviders() {
        return this.providers;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public List<ParsedAttribution> getAttributions() {
        return this.attributions;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStatePackageInfo
    public List<ParsedPermission> getPermissions() {
        return this.permissions;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public List<ParsedPermissionGroup> getPermissionGroups() {
        return this.permissionGroups;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStatePackageInfo
    public List<ParsedInstrumentation> getInstrumentations() {
        return this.instrumentations;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public List<Pair<String, ParsedIntentInfo>> getPreferredActivityFilters() {
        return this.preferredActivityFilters;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public Map<String, ParsedProcess> getProcesses() {
        return this.processes;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public Bundle getMetaData() {
        return this.metaData;
    }

    private void addMimeGroupsFromComponent(ParsedComponent component) {
        for (int i = component.getIntents().size() - 1; i >= 0; i--) {
            IntentFilter filter = component.getIntents().get(i).getIntentFilter();
            for (int groupIndex = filter.countMimeGroups() - 1; groupIndex >= 0; groupIndex--) {
                ArraySet<String> arraySet = this.mimeGroups;
                if (arraySet != null && arraySet.size() > 500) {
                    throw new IllegalStateException("Max limit on number of MIME Groups reached");
                }
                this.mimeGroups = ArrayUtils.add(this.mimeGroups, filter.getMimeGroup(groupIndex));
            }
        }
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public Set<String> getMimeGroups() {
        return this.mimeGroups;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public String getVolumeUuid() {
        return this.volumeUuid;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public SigningDetails getSigningDetails() {
        return this.signingDetails;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public String getPath() {
        return this.mPath;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public boolean isUse32BitAbi() {
        return getBoolean(1099511627776L);
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public boolean isVisibleToInstantApps() {
        return getBoolean(2199023255552L);
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public boolean isForceQueryable() {
        return getBoolean(4398046511104L);
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public List<Intent> getQueriesIntents() {
        return this.queriesIntents;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public List<String> getQueriesPackages() {
        return this.queriesPackages;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public Set<String> getQueriesProviders() {
        return this.queriesProviders;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public String[] getSplitClassLoaderNames() {
        String[] strArr = this.splitClassLoaderNames;
        return strArr == null ? EmptyArray.STRING : strArr;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public String[] getSplitCodePaths() {
        String[] strArr = this.splitCodePaths;
        return strArr == null ? EmptyArray.STRING : strArr;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public SparseArray<int[]> getSplitDependencies() {
        SparseArray<int[]> sparseArray = this.splitDependencies;
        return sparseArray == null ? EMPTY_INT_ARRAY_SPARSE_ARRAY : sparseArray;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public int[] getSplitFlags() {
        return this.splitFlags;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStatePackageInfo
    public String[] getSplitNames() {
        String[] strArr = this.splitNames;
        return strArr == null ? EmptyArray.STRING : strArr;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStatePackageInfo
    public int[] getSplitRevisionCodes() {
        int[] iArr = this.splitRevisionCodes;
        return iArr == null ? EmptyArray.INT : iArr;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public String getAppComponentFactory() {
        return this.appComponentFactory;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public String getBackupAgentName() {
        return this.backupAgentName;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public int getBanner() {
        return this.banner;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public int getCategory() {
        return this.category;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public String getClassLoaderName() {
        return this.classLoaderName;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public String getClassName() {
        return this.className;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public int getCompatibleWidthLimitDp() {
        return this.compatibleWidthLimitDp;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public int getDescriptionRes() {
        return this.descriptionRes;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isEnabled() {
        return getBoolean(17592186044416L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isCrossProfile() {
        return getBoolean(8796093022208L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public int getFullBackupContent() {
        return this.fullBackupContent;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public int getDataExtractionRules() {
        return this.dataExtractionRules;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public int getIconRes() {
        return this.iconRes;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public int getInstallLocation() {
        return this.installLocation;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public int getLabelRes() {
        return this.labelRes;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public int getLargestWidthLimitDp() {
        return this.largestWidthLimitDp;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public int getLogo() {
        return this.logo;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public String getManageSpaceActivityName() {
        return this.manageSpaceActivityName;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public float getMaxAspectRatio() {
        return this.maxAspectRatio;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public float getMinAspectRatio() {
        return this.minAspectRatio;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public SparseIntArray getMinExtensionVersions() {
        return this.minExtensionVersions;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public int getMinSdkVersion() {
        return this.minSdkVersion;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public int getMaxSdkVersion() {
        return this.maxSdkVersion;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public int getNetworkSecurityConfigRes() {
        return this.networkSecurityConfigRes;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public CharSequence getNonLocalizedLabel() {
        return this.nonLocalizedLabel;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public String getPermission() {
        return this.permission;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public int getRequiresSmallestWidthDp() {
        return this.requiresSmallestWidthDp;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public int getRoundIconRes() {
        return this.roundIconRes;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public int getTargetSdkVersion() {
        return this.targetSdkVersion;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public int getTargetSandboxVersion() {
        return this.targetSandboxVersion;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public String getTaskAffinity() {
        return this.taskAffinity;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public int getTheme() {
        return this.theme;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public int getUiOptions() {
        return this.uiOptions;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public String getZygotePreloadName() {
        return this.zygotePreloadName;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isExternalStorage() {
        return getBoolean(1L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isBaseHardwareAccelerated() {
        return getBoolean(2L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isAllowBackup() {
        return getBoolean(4L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isKillAfterRestore() {
        return getBoolean(8L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isRestoreAnyVersion() {
        return getBoolean(16L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isFullBackupOnly() {
        return getBoolean(32L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isPersistent() {
        return getBoolean(64L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isDebuggable() {
        return getBoolean(128L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isVmSafeMode() {
        return getBoolean(256L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isHasCode() {
        return getBoolean(512L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isAllowTaskReparenting() {
        return getBoolean(GadgetFunction.NCM);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isAllowClearUserData() {
        return getBoolean(2048L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isLargeHeap() {
        return getBoolean(4096L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isUsesCleartextTraffic() {
        return getBoolean(8192L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isSupportsRtl() {
        return getBoolean(16384L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isTestOnly() {
        return getBoolean(32768L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isMultiArch() {
        return getBoolean(65536L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isExtractNativeLibs() {
        return getBoolean(131072L);
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public boolean isGame() {
        return getBoolean(262144L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public Boolean getResizeableActivity() {
        return this.resizeableActivity;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isStaticSharedLibrary() {
        return getBoolean(524288L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isSdkLibrary() {
        return getBoolean(562949953421312L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isOverlay() {
        return getBoolean(1048576L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isIsolatedSplitLoading() {
        return getBoolean(2097152L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isHasDomainUrls() {
        return getBoolean(4194304L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isProfileableByShell() {
        return isProfileable() && getBoolean(8388608L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isProfileable() {
        return !getBoolean(35184372088832L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isBackupInForeground() {
        return getBoolean(16777216L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isUseEmbeddedDex() {
        return getBoolean(33554432L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isDefaultToDeviceProtectedStorage() {
        return getBoolean(67108864L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isDirectBootAware() {
        return getBoolean(134217728L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public int getGwpAsanMode() {
        return this.gwpAsanMode;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public int getMemtagMode() {
        return this.memtagMode;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public int getNativeHeapZeroInitialized() {
        return this.nativeHeapZeroInitialized;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public int getLocaleConfigRes() {
        return this.mLocaleConfigRes;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public Boolean hasRequestRawExternalStorageAccess() {
        return this.requestRawExternalStorageAccess;
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isPartiallyDirectBootAware() {
        return getBoolean(268435456L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isResizeableActivityViaSdkVersion() {
        return getBoolean(536870912L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isAllowClearUserDataOnFailedRestore() {
        return getBoolean(1073741824L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isAllowAudioPlaybackCapture() {
        return getBoolean(2147483648L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isRequestLegacyExternalStorage() {
        return getBoolean(4294967296L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isUsesNonSdkApi() {
        return getBoolean(8589934592L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isHasFragileUserData() {
        return getBoolean(17179869184L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isCantSaveState() {
        return getBoolean(34359738368L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean isAllowNativeHeapPointerTagging() {
        return getBoolean(68719476736L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public int getAutoRevokePermissions() {
        return this.autoRevokePermissions;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public boolean hasPreserveLegacyExternalStorage() {
        return getBoolean(137438953472L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean hasRequestForegroundServiceExemption() {
        return getBoolean(70368744177664L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public boolean areAttributionsUserVisible() {
        return getBoolean(140737488355328L);
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public boolean isResetEnabledSettingsOnAppDataCleared() {
        return getBoolean(281474976710656L);
    }

    @Override // com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo
    public Set<String> getKnownActivityEmbeddingCerts() {
        Set<String> set = this.mKnownActivityEmbeddingCerts;
        return set == null ? Collections.emptySet() : set;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public boolean isOnBackInvokedCallbackEnabled() {
        return getBoolean(1125899906842624L);
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageRead
    public boolean isLeavingSharedUid() {
        return getBoolean(2251799813685248L);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setBaseRevisionCode(int value) {
        this.baseRevisionCode = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setCompileSdkVersion(int value) {
        this.compileSdkVersion = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setRequiredForAllUsers(boolean value) {
        return setBoolean(274877906944L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setOverlayPriority(int value) {
        this.overlayPriority = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setOverlayIsStatic(boolean value) {
        return setBoolean(549755813888L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setStaticSharedLibVersion(long value) {
        this.staticSharedLibVersion = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setSharedUserLabel(int value) {
        this.sharedUserLabel = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage, com.android.server.pm.parsing.pkg.ParsedPackage
    public ParsingPackageImpl setRestrictUpdateHash(byte... value) {
        this.restrictUpdateHash = value;
        return this;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setUpgradeKeySets(Set<String> value) {
        this.upgradeKeySets = value;
        return this;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setProcesses(Map<String, ParsedProcess> value) {
        this.processes = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setMetaData(Bundle value) {
        this.metaData = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage, com.android.server.pm.parsing.pkg.ParsedPackage
    public ParsingPackageImpl setSigningDetails(SigningDetails value) {
        this.signingDetails = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setUse32BitAbi(boolean value) {
        return setBoolean(1099511627776L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setVisibleToInstantApps(boolean value) {
        return setBoolean(2199023255552L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setForceQueryable(boolean value) {
        return setBoolean(4398046511104L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setBanner(int value) {
        this.banner = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setCategory(int value) {
        this.category = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setCompatibleWidthLimitDp(int value) {
        this.compatibleWidthLimitDp = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setDescriptionRes(int value) {
        this.descriptionRes = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setEnabled(boolean value) {
        return setBoolean(17592186044416L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setCrossProfile(boolean value) {
        return setBoolean(8796093022208L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setFullBackupContent(int value) {
        this.fullBackupContent = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setDataExtractionRules(int value) {
        this.dataExtractionRules = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setIconRes(int value) {
        this.iconRes = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setInstallLocation(int value) {
        this.installLocation = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setLeavingSharedUid(boolean value) {
        return setBoolean(2251799813685248L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setLabelRes(int value) {
        this.labelRes = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setLargestWidthLimitDp(int value) {
        this.largestWidthLimitDp = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setLogo(int value) {
        this.logo = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setMaxAspectRatio(float value) {
        this.maxAspectRatio = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setMinAspectRatio(float value) {
        this.minAspectRatio = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setMinExtensionVersions(SparseIntArray value) {
        this.minExtensionVersions = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setMinSdkVersion(int value) {
        this.minSdkVersion = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setMaxSdkVersion(int value) {
        this.maxSdkVersion = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setNetworkSecurityConfigRes(int value) {
        this.networkSecurityConfigRes = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setRequiresSmallestWidthDp(int value) {
        this.requiresSmallestWidthDp = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setRoundIconRes(int value) {
        this.roundIconRes = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setTargetSandboxVersion(int value) {
        this.targetSandboxVersion = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setTargetSdkVersion(int value) {
        this.targetSdkVersion = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setTheme(int value) {
        this.theme = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setRequestForegroundServiceExemption(boolean value) {
        return setBoolean(70368744177664L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setUiOptions(int value) {
        this.uiOptions = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setExternalStorage(boolean value) {
        return setBoolean(1L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setBaseHardwareAccelerated(boolean value) {
        return setBoolean(2L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setAllowBackup(boolean value) {
        return setBoolean(4L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setKillAfterRestore(boolean value) {
        return setBoolean(8L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setRestoreAnyVersion(boolean value) {
        return setBoolean(16L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setFullBackupOnly(boolean value) {
        return setBoolean(32L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage, com.android.server.pm.parsing.pkg.ParsedPackage
    public ParsingPackageImpl setPersistent(boolean value) {
        return setBoolean(64L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setDebuggable(boolean value) {
        return setBoolean(128L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setVmSafeMode(boolean value) {
        return setBoolean(256L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setHasCode(boolean value) {
        return setBoolean(512L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setAllowTaskReparenting(boolean value) {
        return setBoolean(GadgetFunction.NCM, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setAllowClearUserData(boolean value) {
        return setBoolean(2048L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setLargeHeap(boolean value) {
        return setBoolean(4096L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setUsesCleartextTraffic(boolean value) {
        return setBoolean(8192L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setSupportsRtl(boolean value) {
        return setBoolean(16384L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setTestOnly(boolean value) {
        return setBoolean(32768L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setMultiArch(boolean value) {
        return setBoolean(65536L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setExtractNativeLibs(boolean value) {
        return setBoolean(131072L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setGame(boolean value) {
        return setBoolean(262144L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setResizeableActivity(Boolean value) {
        this.resizeableActivity = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setSdkLibName(String sdkLibName) {
        this.sdkLibName = TextUtils.safeIntern(sdkLibName);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setSdkLibVersionMajor(int sdkLibVersionMajor) {
        this.sdkLibVersionMajor = sdkLibVersionMajor;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setSdkLibrary(boolean value) {
        return setBoolean(562949953421312L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setStaticSharedLibrary(boolean value) {
        return setBoolean(524288L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setOverlay(boolean value) {
        return setBoolean(1048576L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setIsolatedSplitLoading(boolean value) {
        return setBoolean(2097152L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setHasDomainUrls(boolean value) {
        return setBoolean(4194304L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setProfileableByShell(boolean value) {
        return setBoolean(8388608L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setProfileable(boolean value) {
        return setBoolean(35184372088832L, !value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setBackupInForeground(boolean value) {
        return setBoolean(16777216L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setUseEmbeddedDex(boolean value) {
        return setBoolean(33554432L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage, com.android.server.pm.parsing.pkg.ParsedPackage
    public ParsingPackageImpl setDefaultToDeviceProtectedStorage(boolean value) {
        return setBoolean(67108864L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage, com.android.server.pm.parsing.pkg.ParsedPackage
    public ParsingPackageImpl setDirectBootAware(boolean value) {
        return setBoolean(134217728L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setGwpAsanMode(int value) {
        this.gwpAsanMode = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setMemtagMode(int value) {
        this.memtagMode = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setNativeHeapZeroInitialized(int value) {
        this.nativeHeapZeroInitialized = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setRequestRawExternalStorageAccess(Boolean value) {
        this.requestRawExternalStorageAccess = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setPartiallyDirectBootAware(boolean value) {
        return setBoolean(268435456L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setResizeableActivityViaSdkVersion(boolean value) {
        return setBoolean(536870912L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setAllowClearUserDataOnFailedRestore(boolean value) {
        return setBoolean(1073741824L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setAllowAudioPlaybackCapture(boolean value) {
        return setBoolean(2147483648L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setRequestLegacyExternalStorage(boolean value) {
        return setBoolean(4294967296L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setUsesNonSdkApi(boolean value) {
        return setBoolean(8589934592L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setHasFragileUserData(boolean value) {
        return setBoolean(17179869184L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setCantSaveState(boolean value) {
        return setBoolean(34359738368L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setAllowNativeHeapPointerTagging(boolean value) {
        return setBoolean(68719476736L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setAutoRevokePermissions(int value) {
        this.autoRevokePermissions = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setPreserveLegacyExternalStorage(boolean value) {
        return setBoolean(137438953472L, value);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setVersionName(String versionName) {
        this.versionName = versionName;
        return this;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackage setCompileSdkVersionCodeName(String compileSdkVersionCodeName) {
        this.compileSdkVersionCodeName = compileSdkVersionCodeName;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setProcessName(String processName) {
        this.processName = processName;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setRestrictedAccountType(String restrictedAccountType) {
        this.restrictedAccountType = restrictedAccountType;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setOverlayTargetOverlayableName(String overlayTargetOverlayableName) {
        this.overlayTargetOverlayableName = overlayTargetOverlayableName;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setOverlayCategory(String overlayCategory) {
        this.overlayCategory = overlayCategory;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setAppComponentFactory(String appComponentFactory) {
        this.appComponentFactory = appComponentFactory;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setBackupAgentName(String backupAgentName) {
        this.backupAgentName = backupAgentName;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setClassLoaderName(String classLoaderName) {
        this.classLoaderName = classLoaderName;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setClassName(String className) {
        this.className = className == null ? null : className.trim();
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setManageSpaceActivityName(String manageSpaceActivityName) {
        this.manageSpaceActivityName = manageSpaceActivityName;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setPermission(String permission) {
        this.permission = permission;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setTaskAffinity(String taskAffinity) {
        this.taskAffinity = taskAffinity;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setZygotePreloadName(String zygotePreloadName) {
        this.zygotePreloadName = zygotePreloadName;
        return this;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackage setAttributionsAreUserVisible(boolean attributionsAreUserVisible) {
        setBoolean(140737488355328L, attributionsAreUserVisible);
        return this;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackage setResetEnabledSettingsOnAppDataCleared(boolean resetEnabledSettingsOnAppDataCleared) {
        setBoolean(281474976710656L, resetEnabledSettingsOnAppDataCleared);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackageImpl setLocaleConfigRes(int value) {
        this.mLocaleConfigRes = value;
        return this;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackage setKnownActivityEmbeddingCerts(Set<String> knownEmbeddingCerts) {
        this.mKnownActivityEmbeddingCerts = knownEmbeddingCerts;
        return this;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsingPackage setOnBackInvokedCallbackEnabled(boolean value) {
        setBoolean(1125899906842624L, value);
        return this;
    }
}
