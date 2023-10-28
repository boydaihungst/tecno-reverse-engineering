package com.android.server.pm.pkg.parsing;

import android.app.ActivityThread;
import android.app.ResourcesManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.ConfigurationInfo;
import android.content.pm.FeatureGroupInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningDetails;
import android.content.pm.parsing.ApkLiteParseUtils;
import android.content.pm.parsing.FrameworkParsingPackageUtils;
import android.content.pm.parsing.PackageLite;
import android.content.pm.parsing.result.ParseInput;
import android.content.pm.parsing.result.ParseResult;
import android.content.pm.parsing.result.ParseTypeImpl;
import android.content.pm.split.SplitDependencyLoader;
import android.content.res.ApkAssets;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.ext.SdkExtensions;
import android.permission.PermissionManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.util.apk.ApkSignatureVerifier;
import com.android.internal.R;
import com.android.internal.os.ClassLoaderFactory;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import com.android.server.am.HostingRecord;
import com.android.server.location.gnss.hal.GnssNative;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.SharedUidMigration;
import com.android.server.pm.permission.CompatibilityPermissionInfo;
import com.android.server.pm.pkg.component.ComponentMutateUtils;
import com.android.server.pm.pkg.component.ComponentParseUtils;
import com.android.server.pm.pkg.component.ParsedActivity;
import com.android.server.pm.pkg.component.ParsedActivityUtils;
import com.android.server.pm.pkg.component.ParsedApexSystemService;
import com.android.server.pm.pkg.component.ParsedApexSystemServiceUtils;
import com.android.server.pm.pkg.component.ParsedAttribution;
import com.android.server.pm.pkg.component.ParsedAttributionUtils;
import com.android.server.pm.pkg.component.ParsedComponent;
import com.android.server.pm.pkg.component.ParsedInstrumentation;
import com.android.server.pm.pkg.component.ParsedInstrumentationUtils;
import com.android.server.pm.pkg.component.ParsedIntentInfo;
import com.android.server.pm.pkg.component.ParsedIntentInfoImpl;
import com.android.server.pm.pkg.component.ParsedIntentInfoUtils;
import com.android.server.pm.pkg.component.ParsedPermission;
import com.android.server.pm.pkg.component.ParsedPermissionGroup;
import com.android.server.pm.pkg.component.ParsedPermissionUtils;
import com.android.server.pm.pkg.component.ParsedProcess;
import com.android.server.pm.pkg.component.ParsedProcessUtils;
import com.android.server.pm.pkg.component.ParsedProvider;
import com.android.server.pm.pkg.component.ParsedProviderUtils;
import com.android.server.pm.pkg.component.ParsedService;
import com.android.server.pm.pkg.component.ParsedServiceUtils;
import com.android.server.pm.pkg.component.ParsedUsesPermission;
import com.android.server.pm.pkg.component.ParsedUsesPermissionImpl;
import com.android.server.pm.split.DefaultSplitAssetLoader;
import com.android.server.pm.split.SplitAssetDependencyLoader;
import com.android.server.pm.split.SplitAssetLoader;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.mediatek.server.cta.CtaLinkManagerFactory;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import libcore.io.IoUtils;
import libcore.util.EmptyArray;
import libcore.util.HexEncoding;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class ParsingPackageUtils {
    public static final String ANDROID_MANIFEST_FILENAME = "AndroidManifest.xml";
    public static final float ASPECT_RATIO_NOT_SET = -1.0f;
    public static final boolean DEBUG_BACKUP = false;
    public static final boolean DEBUG_JAR = false;
    public static final float DEFAULT_PRE_O_MAX_ASPECT_RATIO = 1.86f;
    private static final int MAX_NUM_COMPONENTS = 30000;
    private static final String MAX_NUM_COMPONENTS_ERR_MSG = "Total number of components has exceeded the maximum number: 30000";
    private static final int MAX_PERMISSION_NAME_LENGTH = 512;
    public static final String METADATA_ACTIVITY_LAUNCH_MODE = "android.activity.launch_mode";
    public static final String METADATA_ACTIVITY_WINDOW_LAYOUT_AFFINITY = "android.activity_window_layout_affinity";
    public static final String METADATA_CAN_DISPLAY_ON_REMOTE_DEVICES = "android.can_display_on_remote_devices";
    public static final String METADATA_MAX_ASPECT_RATIO = "android.max_aspect";
    public static final String METADATA_SUPPORTS_CONFIG_CHANGES_ASSETS_PATHS = "android.supports_config_changes_assets_paths";
    public static final String METADATA_SUPPORTS_SIZE_CHANGES = "android.supports_size_changes";
    public static final String MNT_EXPAND = "/mnt/expand/";
    public static final int PARSE_APK_IN_APEX = 512;
    public static final int PARSE_CHATTY = Integer.MIN_VALUE;
    public static final int PARSE_COLLECT_CERTIFICATES = 32;
    public static final int PARSE_DEFAULT_INSTALL_LOCATION = -1;
    public static final int PARSE_DEFAULT_TARGET_SANDBOX = 1;
    public static final int PARSE_ENFORCE_CODE = 64;
    public static final int PARSE_EXTERNAL_STORAGE = 8;
    public static final int PARSE_FRAMEWORK_RES_SPLITS = 256;
    public static final int PARSE_IGNORE_OVERLAY_REQUIRED_SYSTEM_PROPERTY = 128;
    public static final int PARSE_IGNORE_PROCESSES = 2;
    public static final int PARSE_IS_SYSTEM_DIR = 16;
    public static final int PARSE_MUST_BE_APK = 1;
    public static final boolean RIGID_PARSER = false;
    private static final String TAG = "PackageParsing";
    public static final String TAG_ADOPT_PERMISSIONS = "adopt-permissions";
    public static final String TAG_APPLICATION = "application";
    public static final String TAG_ATTRIBUTION = "attribution";
    public static final String TAG_COMPATIBLE_SCREENS = "compatible-screens";
    public static final String TAG_EAT_COMMENT = "eat-comment";
    public static final String TAG_FEATURE_GROUP = "feature-group";
    public static final String TAG_INSTRUMENTATION = "instrumentation";
    public static final String TAG_KEY_SETS = "key-sets";
    public static final String TAG_MANIFEST = "manifest";
    public static final String TAG_ORIGINAL_PACKAGE = "original-package";
    public static final String TAG_OVERLAY = "overlay";
    public static final String TAG_PACKAGE = "package";
    public static final String TAG_PACKAGE_VERIFIER = "package-verifier";
    public static final String TAG_PERMISSION = "permission";
    public static final String TAG_PERMISSION_GROUP = "permission-group";
    public static final String TAG_PERMISSION_TREE = "permission-tree";
    public static final String TAG_PROFILEABLE = "profileable";
    public static final String TAG_PROTECTED_BROADCAST = "protected-broadcast";
    public static final String TAG_QUERIES = "queries";
    public static final String TAG_RECEIVER = "receiver";
    public static final String TAG_RESTRICT_UPDATE = "restrict-update";
    public static final String TAG_SUPPORTS_INPUT = "supports-input";
    public static final String TAG_SUPPORT_SCREENS = "supports-screens";
    public static final String TAG_USES_CONFIGURATION = "uses-configuration";
    public static final String TAG_USES_FEATURE = "uses-feature";
    public static final String TAG_USES_GL_TEXTURE = "uses-gl-texture";
    public static final String TAG_USES_PERMISSION = "uses-permission";
    public static final String TAG_USES_PERMISSION_SDK_23 = "uses-permission-sdk-23";
    public static final String TAG_USES_PERMISSION_SDK_M = "uses-permission-sdk-m";
    public static final String TAG_USES_SDK = "uses-sdk";
    public static final String TAG_USES_SPLIT = "uses-split";
    private Callback mCallback;
    private DisplayMetrics mDisplayMetrics;
    private boolean mOnlyCoreApps;
    private String[] mSeparateProcesses;
    private List<PermissionManager.SplitPermissionInfo> mSplitPermissionInfos;
    public static final int SDK_VERSION = Build.VERSION.SDK_INT;
    public static final String[] SDK_CODENAMES = Build.VERSION.ACTIVE_CODENAMES;
    public static boolean sCompatibilityModeEnabled = true;
    public static boolean sUseRoundIcon = false;

    /* loaded from: classes2.dex */
    public interface Callback {
        boolean hasFeature(String str);

        ParsingPackage startParsingPackage(String str, String str2, String str3, TypedArray typedArray, boolean z);
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface ParseFlags {
    }

    public static ParseResult<ParsingPackage> parseDefaultOneTime(File file, int parseFlags, List<PermissionManager.SplitPermissionInfo> splitPermissions, boolean collectCertificates) {
        ParseInput input = ParseTypeImpl.forDefaultParsing().reset();
        return parseDefault(input, file, parseFlags, splitPermissions, collectCertificates);
    }

    public static ParseResult<ParsingPackage> parseDefault(ParseInput input, File file, int parseFlags, List<PermissionManager.SplitPermissionInfo> splitPermissions, boolean collectCertificates) {
        ParsingPackageUtils parser = new ParsingPackageUtils(false, null, null, splitPermissions, new Callback() { // from class: com.android.server.pm.pkg.parsing.ParsingPackageUtils.1
            @Override // com.android.server.pm.pkg.parsing.ParsingPackageUtils.Callback
            public boolean hasFeature(String feature) {
                return false;
            }

            @Override // com.android.server.pm.pkg.parsing.ParsingPackageUtils.Callback
            public ParsingPackage startParsingPackage(String packageName, String baseApkPath, String path, TypedArray manifestArray, boolean isCoreApp) {
                return new ParsingPackageImpl(packageName, baseApkPath, path, manifestArray);
            }
        });
        ParseResult<ParsingPackage> result = parser.parsePackage(input, file, parseFlags, null);
        if (result.isError()) {
            return input.error(result);
        }
        ParsingPackage pkg = (ParsingPackage) result.getResult();
        if (collectCertificates) {
            ParseResult<SigningDetails> ret = getSigningDetails(input, pkg, false);
            if (ret.isError()) {
                return input.error(ret);
            }
            pkg.setSigningDetails((SigningDetails) ret.getResult());
        }
        pkg.hideAsParsed();
        return input.success(pkg);
    }

    public ParsingPackageUtils(boolean onlyCoreApps, String[] separateProcesses, DisplayMetrics displayMetrics, List<PermissionManager.SplitPermissionInfo> splitPermissions, Callback callback) {
        this.mOnlyCoreApps = onlyCoreApps;
        this.mSeparateProcesses = separateProcesses;
        this.mDisplayMetrics = displayMetrics;
        this.mSplitPermissionInfos = splitPermissions;
        this.mCallback = callback;
    }

    public ParseResult<ParsingPackage> parsePackage(ParseInput input, File packageFile, int flags, List<File> frameworkSplits) {
        if ((flags & 256) != 0 && frameworkSplits.size() > 0 && packageFile.getAbsolutePath().endsWith("/framework-res.apk")) {
            return parseClusterPackage(input, packageFile, frameworkSplits, flags);
        }
        if (packageFile.isDirectory()) {
            return parseClusterPackage(input, packageFile, null, flags);
        }
        return parseMonolithicPackage(input, packageFile, flags);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [482=5, 486=9] */
    /* JADX WARN: Removed duplicated region for block: B:80:0x01c8  */
    /* JADX WARN: Removed duplicated region for block: B:81:0x01ca  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private ParseResult<ParsingPackage> parseClusterPackage(ParseInput input, File packageDir, List<File> frameworkSplits, int flags) {
        SplitAssetLoader assetLoader;
        SparseArray<int[]> splitDependencies;
        SplitAssetLoader assetLoader2;
        SplitAssetLoader assetLoader3;
        PackageLite lite;
        int liteParseFlags = (flags & 256) != 0 ? flags : 0;
        ParseResult<PackageLite> liteResult = ApkLiteParseUtils.parseClusterPackageLite(input, packageDir, frameworkSplits, (flags & 512) != 0 ? liteParseFlags | 512 : liteParseFlags);
        if (liteResult.isError()) {
            return input.error(liteResult);
        }
        PackageLite lite2 = (PackageLite) liteResult.getResult();
        if (this.mOnlyCoreApps && !lite2.isCoreApp()) {
            return input.error(-123, "Not a coreApp: " + packageDir);
        }
        if (!lite2.isIsolatedSplits() || ArrayUtils.isEmpty(lite2.getSplitNames())) {
            assetLoader = new DefaultSplitAssetLoader(lite2, flags);
            splitDependencies = null;
        } else {
            try {
                SparseArray<int[]> splitDependencies2 = SplitAssetDependencyLoader.createDependenciesFromPackage(lite2);
                assetLoader = new SplitAssetDependencyLoader(lite2, splitDependencies2, flags);
                splitDependencies = splitDependencies2;
            } catch (SplitDependencyLoader.IllegalDependencyException e) {
                return input.error((int) GnssNative.GeofenceCallbacks.GEOFENCE_STATUS_ERROR_ID_EXISTS, e.getMessage());
            }
        }
        try {
            File baseApk = new File(lite2.getBaseApkPath());
            if (Build.IS_DEBUG_ENABLE) {
                try {
                    try {
                        Slog.i("PackageParsing", "Scanning base APK: " + lite2.getPackageName() + ",patch =" + lite2.getPath() + ",apkFile CanonicalPath:" + baseApk.getCanonicalPath() + ",apkFile AbsolutePath:" + baseApk.getAbsolutePath());
                    } catch (Exception e2) {
                        try {
                            Slog.e("PackageParsing", "a error has happened :" + e2.toString());
                        } catch (IllegalArgumentException e3) {
                            e = e3;
                            assetLoader2 = assetLoader;
                            ParseResult<ParsingPackage> error = input.error(e.getCause() instanceof IOException ? -2 : -100, e.getMessage(), e);
                            IoUtils.closeQuietly(assetLoader2);
                            return error;
                        }
                    }
                } catch (Throwable th) {
                    e = th;
                    assetLoader2 = assetLoader;
                    IoUtils.closeQuietly(assetLoader2);
                    throw e;
                }
            }
            SparseArray<int[]> splitDependencies3 = splitDependencies;
            try {
                ParseResult<ParsingPackage> result = parseBaseApk(input, baseApk, lite2.getPath(), assetLoader, flags);
                if (result.isError()) {
                    try {
                        ParseResult<ParsingPackage> error2 = input.error(result);
                        IoUtils.closeQuietly(assetLoader);
                        return error2;
                    } catch (IllegalArgumentException e4) {
                        e = e4;
                        assetLoader2 = assetLoader;
                        ParseResult<ParsingPackage> error3 = input.error(e.getCause() instanceof IOException ? -2 : -100, e.getMessage(), e);
                        IoUtils.closeQuietly(assetLoader2);
                        return error3;
                    } catch (Throwable th2) {
                        e = th2;
                        assetLoader2 = assetLoader;
                        IoUtils.closeQuietly(assetLoader2);
                        throw e;
                    }
                }
                ParsingPackage pkg = (ParsingPackage) result.getResult();
                if (ArrayUtils.isEmpty(lite2.getSplitNames())) {
                    assetLoader3 = assetLoader;
                    lite = lite2;
                } else {
                    pkg.asSplit(lite2.getSplitNames(), lite2.getSplitApkPaths(), lite2.getSplitRevisionCodes(), splitDependencies3);
                    int num = lite2.getSplitNames().length;
                    int i = 0;
                    while (i < num) {
                        AssetManager splitAssets = assetLoader.getSplitAssetManager(i);
                        assetLoader2 = assetLoader;
                        int i2 = i;
                        PackageLite lite3 = lite2;
                        try {
                            try {
                                ParseResult<ParsingPackage> split = parseSplitApk(input, pkg, i2, splitAssets, flags);
                                if (split.isError()) {
                                    ParseResult<ParsingPackage> error4 = input.error(split);
                                    IoUtils.closeQuietly(assetLoader2);
                                    return error4;
                                }
                                i = i2 + 1;
                                assetLoader = assetLoader2;
                                lite2 = lite3;
                            } catch (IllegalArgumentException e5) {
                                e = e5;
                                ParseResult<ParsingPackage> error32 = input.error(e.getCause() instanceof IOException ? -2 : -100, e.getMessage(), e);
                                IoUtils.closeQuietly(assetLoader2);
                                return error32;
                            }
                        } catch (Throwable th3) {
                            e = th3;
                            IoUtils.closeQuietly(assetLoader2);
                            throw e;
                        }
                    }
                    assetLoader3 = assetLoader;
                    lite = lite2;
                }
                pkg.setUse32BitAbi(lite.isUse32bitAbi());
                ParseResult<ParsingPackage> success = input.success(pkg);
                IoUtils.closeQuietly(assetLoader3);
                return success;
            } catch (IllegalArgumentException e6) {
                e = e6;
                assetLoader2 = assetLoader;
            } catch (Throwable th4) {
                e = th4;
                assetLoader2 = assetLoader;
            }
        } catch (IllegalArgumentException e7) {
            e = e7;
            assetLoader2 = assetLoader;
        } catch (Throwable th5) {
            e = th5;
            assetLoader2 = assetLoader;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [536=5] */
    private ParseResult<ParsingPackage> parseMonolithicPackage(ParseInput input, File apkFile, int flags) {
        ParseResult<PackageLite> liteResult = ApkLiteParseUtils.parseMonolithicPackageLite(input, apkFile, flags);
        if (liteResult.isError()) {
            return input.error(liteResult);
        }
        PackageLite lite = (PackageLite) liteResult.getResult();
        if (!this.mOnlyCoreApps || lite.isCoreApp()) {
            SplitAssetLoader assetLoader = new DefaultSplitAssetLoader(lite, flags);
            try {
                if (Build.IS_DEBUG_ENABLE) {
                    try {
                        Slog.i("PackageParsing", "Scanning base APK: " + lite.getPackageName() + ",patch =" + lite.getPath() + ",apkFile CanonicalPath:" + apkFile.getCanonicalPath() + ",apkFile AbsolutePath:" + apkFile.getAbsolutePath());
                    } catch (Exception e) {
                        Slog.e("PackageParsing", "a error has happened :" + e.toString());
                    }
                }
                ParseResult<ParsingPackage> result = parseBaseApk(input, apkFile, apkFile.getCanonicalPath(), assetLoader, flags);
                return result.isError() ? input.error(result) : input.success(((ParsingPackage) result.getResult()).setUse32BitAbi(lite.isUse32bitAbi()));
            } catch (IOException e2) {
                return input.error((int) GnssNative.GeofenceCallbacks.GEOFENCE_STATUS_ERROR_ID_UNKNOWN, "Failed to get path: " + apkFile, e2);
            } finally {
                IoUtils.closeQuietly(assetLoader);
            }
        }
        return input.error(-123, "Not a coreApp: " + apkFile);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [626=5] */
    private ParseResult<ParsingPackage> parseBaseApk(ParseInput input, File apkFile, String codePath, SplitAssetLoader assetLoader, int flags) {
        String volumeUuid;
        XmlResourceParser parser;
        ParseResult<ParsingPackage> result;
        String apkPath = apkFile.getAbsolutePath();
        if (apkPath.startsWith(MNT_EXPAND)) {
            int end = apkPath.indexOf(47, MNT_EXPAND.length());
            String volumeUuid2 = apkPath.substring(MNT_EXPAND.length(), end);
            volumeUuid = volumeUuid2;
        } else {
            volumeUuid = null;
        }
        try {
            AssetManager assets = assetLoader.getBaseAssetManager();
            int cookie = assets.findCookieForPath(apkPath);
            if (cookie == 0) {
                return input.error((int) GnssNative.GeofenceCallbacks.GEOFENCE_STATUS_ERROR_ID_EXISTS, "Failed adding asset path: " + apkPath);
            }
            try {
                parser = assets.openXmlResourceParser(cookie, ANDROID_MANIFEST_FILENAME);
            } catch (Exception e) {
                e = e;
            }
            try {
                try {
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    Resources res = new Resources(assets, this.mDisplayMetrics, null);
                    ParseResult<ParsingPackage> result2 = parseBaseApk(input, apkPath, codePath, res, parser, flags);
                    if (result2.isError()) {
                        ParseResult<ParsingPackage> error = input.error(result2.getErrorCode(), apkPath + " (at " + parser.getPositionDescription() + "): " + result2.getErrorMessage());
                        if (parser != null) {
                            parser.close();
                        }
                        return error;
                    }
                    ParsingPackage pkg = (ParsingPackage) result2.getResult();
                    if (assets.containsAllocatedTable()) {
                        ParseResult<?> deferResult = input.deferError("Targeting R+ (version 30 and above) requires the resources.arsc of installed APKs to be stored uncompressed and aligned on a 4-byte boundary", 132742131L);
                        if (deferResult.isError()) {
                            ParseResult<ParsingPackage> error2 = input.error(-124, deferResult.getErrorMessage());
                            if (parser != null) {
                                parser.close();
                            }
                            return error2;
                        }
                    }
                    ApkAssets apkAssets = assetLoader.getBaseApkAssets();
                    boolean definesOverlayable = false;
                    try {
                        definesOverlayable = apkAssets.definesOverlayable();
                    } catch (IOException e2) {
                    }
                    if (definesOverlayable) {
                        SparseArray<String> packageNames = assets.getAssignedPackageIdentifiers();
                        int size = packageNames.size();
                        int index = 0;
                        while (index < size) {
                            String packageName = packageNames.valueAt(index);
                            SparseArray<String> packageNames2 = packageNames;
                            Map<String, String> overlayableToActor = assets.getOverlayableMap(packageName);
                            if (overlayableToActor == null || overlayableToActor.isEmpty()) {
                                result = result2;
                            } else {
                                for (String overlayable : overlayableToActor.keySet()) {
                                    pkg.addOverlayable(overlayable, overlayableToActor.get(overlayable));
                                    result2 = result2;
                                    overlayableToActor = overlayableToActor;
                                }
                                result = result2;
                            }
                            index++;
                            packageNames = packageNames2;
                            result2 = result;
                        }
                    }
                    pkg.setVolumeUuid(volumeUuid);
                    if ((flags & 32) != 0) {
                        ParseResult<SigningDetails> ret = getSigningDetails(input, pkg, false);
                        if (ret.isError()) {
                            ParseResult<ParsingPackage> error3 = input.error(ret);
                            if (parser != null) {
                                parser.close();
                            }
                            return error3;
                        }
                        pkg.setSigningDetails((SigningDetails) ret.getResult());
                    } else {
                        pkg.setSigningDetails(SigningDetails.UNKNOWN);
                    }
                    ParseResult<ParsingPackage> success = input.success(pkg);
                    if (parser != null) {
                        parser.close();
                    }
                    return success;
                } catch (Throwable th2) {
                    th = th2;
                    Throwable th3 = th;
                    if (parser != null) {
                        parser.close();
                    }
                    throw th3;
                }
            } catch (Exception e3) {
                e = e3;
                return input.error((int) GnssNative.GeofenceCallbacks.GEOFENCE_STATUS_ERROR_ID_UNKNOWN, "Failed to read manifest from " + apkPath, e);
            }
        } catch (IllegalArgumentException e4) {
            return input.error(e4.getCause() instanceof IOException ? -2 : -100, e4.getMessage(), e4);
        }
    }

    private ParseResult<ParsingPackage> parseSplitApk(ParseInput input, ParsingPackage pkg, int splitIndex, AssetManager assets, int flags) {
        String apkPath = pkg.getSplitCodePaths()[splitIndex];
        int cookie = assets.findCookieForPath(apkPath);
        if (cookie == 0) {
            return input.error((int) GnssNative.GeofenceCallbacks.GEOFENCE_STATUS_ERROR_ID_EXISTS, "Failed adding asset path: " + apkPath);
        }
        try {
            XmlResourceParser parser = assets.openXmlResourceParser(cookie, ANDROID_MANIFEST_FILENAME);
            Resources res = new Resources(assets, this.mDisplayMetrics, null);
            ParseResult<ParsingPackage> parseResult = parseSplitApk(input, pkg, res, parser, flags, splitIndex);
            if (parseResult.isError()) {
                ParseResult<ParsingPackage> error = input.error(parseResult.getErrorCode(), apkPath + " (at " + parser.getPositionDescription() + "): " + parseResult.getErrorMessage());
                if (parser != null) {
                    parser.close();
                }
                return error;
            }
            if (parser != null) {
                parser.close();
            }
            return parseResult;
        } catch (Exception e) {
            return input.error((int) GnssNative.GeofenceCallbacks.GEOFENCE_STATUS_ERROR_ID_UNKNOWN, "Failed to read manifest from " + apkPath, e);
        }
    }

    private ParseResult<ParsingPackage> parseBaseApk(ParseInput input, String apkPath, String codePath, Resources res, XmlResourceParser parser, int flags) throws XmlPullParserException, IOException {
        ParseResult<Pair<String, String>> packageSplitResult = ApkLiteParseUtils.parsePackageSplitNames(input, parser);
        if (packageSplitResult.isError()) {
            return input.error(packageSplitResult);
        }
        Pair<String, String> packageSplit = (Pair) packageSplitResult.getResult();
        String pkgName = (String) packageSplit.first;
        String splitName = (String) packageSplit.second;
        if (!TextUtils.isEmpty(splitName)) {
            return input.error(-106, "Expected base APK, but found split " + splitName);
        }
        TypedArray manifestArray = res.obtainAttributes(parser, R.styleable.AndroidManifest);
        try {
            boolean isCoreApp = parser.getAttributeBooleanValue(null, "coreApp", false);
            ParsingPackage pkg = this.mCallback.startParsingPackage(pkgName, apkPath, codePath, manifestArray, isCoreApp);
            try {
                ParseResult<ParsingPackage> result = parseBaseApkTags(input, pkg, manifestArray, res, parser, flags);
                if (!result.isError()) {
                    ParseResult<ParsingPackage> success = input.success(pkg);
                    manifestArray.recycle();
                    return success;
                }
                manifestArray.recycle();
                return result;
            } catch (Throwable th) {
                th = th;
                manifestArray.recycle();
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private ParseResult<ParsingPackage> parseSplitApk(ParseInput input, ParsingPackage pkg, Resources res, XmlResourceParser parser, int flags, int splitIndex) throws XmlPullParserException, IOException {
        ParseResult result;
        ParseResult<Pair<String, String>> packageSplitResult = ApkLiteParseUtils.parsePackageSplitNames(input, parser);
        if (packageSplitResult.isError()) {
            return input.error(packageSplitResult);
        }
        boolean foundApp = false;
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (outerDepth + 1 >= parser.getDepth() && type == 2) {
                    String tagName = parser.getName();
                    if (TAG_APPLICATION.equals(tagName)) {
                        if (foundApp) {
                            Slog.w("PackageParsing", "<manifest> has more than one <application>");
                            result = input.success((Object) null);
                        } else {
                            foundApp = true;
                            result = parseSplitApplication(input, pkg, res, parser, flags, splitIndex);
                        }
                    } else {
                        result = ParsingUtils.unknownTag("<manifest>", pkg, parser, input);
                    }
                    if (result.isError()) {
                        return input.error(result);
                    }
                }
            } else {
                if (!foundApp) {
                    ParseResult<?> deferResult = input.deferError("<manifest> does not contain an <application>", 150776642L);
                    if (deferResult.isError()) {
                        return input.error(deferResult);
                    }
                }
                return input.success(pkg);
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [800=4] */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:39:0x00b2, code lost:
        if (r5.equals("provider") != false) goto L26;
     */
    /* JADX WARN: Removed duplicated region for block: B:63:0x01ad  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private ParseResult<ParsingPackage> parseSplitApplication(ParseInput input, ParsingPackage pkg, Resources res, XmlResourceParser parser, int flags, int splitIndex) throws XmlPullParserException, IOException {
        TypedArray sa;
        int depth;
        int i;
        TypedArray sa2;
        ParsingPackage parsingPackage;
        ParseInput parseInput;
        ParseResult<ParsedActivity> activityResult;
        ParseResult<ParsedActivity> result;
        ParseInput parseInput2;
        ParseInput parseInput3 = input;
        ParsingPackage parsingPackage2 = pkg;
        TypedArray sa3 = res.obtainAttributes(parser, R.styleable.AndroidManifestApplication);
        int i2 = 1;
        try {
            parsingPackage2.setSplitHasCode(splitIndex, sa3.getBoolean(7, true));
            String classLoaderName = sa3.getString(46);
            if (classLoaderName != null) {
                try {
                    if (!ClassLoaderFactory.isValidClassLoaderName(classLoaderName)) {
                        ParseResult<ParsingPackage> error = parseInput3.error("Invalid class loader name: " + classLoaderName);
                        sa3.recycle();
                        return error;
                    }
                } catch (Throwable th) {
                    th = th;
                    sa = sa3;
                    sa.recycle();
                    throw th;
                }
            }
            parsingPackage2.setSplitClassLoaderName(splitIndex, classLoaderName);
            sa3.recycle();
            String defaultSplitName = pkg.getSplitNames()[splitIndex];
            int depth2 = parser.getDepth();
            while (true) {
                int type = parser.next();
                if (type != i2) {
                    int i3 = 3;
                    if (type != 3 || parser.getDepth() > depth2) {
                        if (type == 2) {
                            String tagName = parser.getName();
                            boolean isActivity = false;
                            switch (tagName.hashCode()) {
                                case -1655966961:
                                    if (tagName.equals(HostingRecord.HOSTING_TYPE_ACTIVITY)) {
                                        i3 = 0;
                                        break;
                                    }
                                    i3 = -1;
                                    break;
                                case -987494927:
                                    break;
                                case -808719889:
                                    if (tagName.equals(TAG_RECEIVER)) {
                                        i3 = i2;
                                        break;
                                    }
                                    i3 = -1;
                                    break;
                                case 790287890:
                                    if (tagName.equals("activity-alias")) {
                                        i3 = 4;
                                        break;
                                    }
                                    i3 = -1;
                                    break;
                                case 1984153269:
                                    if (tagName.equals(HostingRecord.HOSTING_TYPE_SERVICE)) {
                                        i3 = 2;
                                        break;
                                    }
                                    i3 = -1;
                                    break;
                                default:
                                    i3 = -1;
                                    break;
                            }
                            switch (i3) {
                                case 0:
                                    depth = depth2;
                                    i = i2;
                                    sa2 = sa3;
                                    parsingPackage = parsingPackage2;
                                    parseInput = parseInput3;
                                    isActivity = true;
                                    activityResult = ParsedActivityUtils.parseActivityOrReceiver(this.mSeparateProcesses, pkg, res, parser, flags, sUseRoundIcon, defaultSplitName, input);
                                    if (activityResult.isSuccess()) {
                                        ParsedActivity activity = (ParsedActivity) activityResult.getResult();
                                        if (isActivity) {
                                            parsingPackage.addActivity(activity);
                                        } else {
                                            parsingPackage.addReceiver(activity);
                                        }
                                    }
                                    result = activityResult;
                                    parseInput2 = parseInput;
                                    break;
                                case 1:
                                    depth = depth2;
                                    i = i2;
                                    sa2 = sa3;
                                    parsingPackage = parsingPackage2;
                                    parseInput = parseInput3;
                                    activityResult = ParsedActivityUtils.parseActivityOrReceiver(this.mSeparateProcesses, pkg, res, parser, flags, sUseRoundIcon, defaultSplitName, input);
                                    if (activityResult.isSuccess()) {
                                    }
                                    result = activityResult;
                                    parseInput2 = parseInput;
                                    break;
                                case 2:
                                    depth = depth2;
                                    i = i2;
                                    sa2 = sa3;
                                    ParsingPackage parsingPackage3 = parsingPackage2;
                                    ParseInput parseInput4 = parseInput3;
                                    ParseResult<ParsedService> serviceResult = ParsedServiceUtils.parseService(this.mSeparateProcesses, pkg, res, parser, flags, sUseRoundIcon, defaultSplitName, input);
                                    if (serviceResult.isSuccess()) {
                                        ParsedService service = (ParsedService) serviceResult.getResult();
                                        parsingPackage3.addService(service);
                                    }
                                    result = serviceResult;
                                    parseInput2 = parseInput4;
                                    break;
                                case 3:
                                    depth = depth2;
                                    i = i2;
                                    sa2 = sa3;
                                    ParsingPackage parsingPackage4 = parsingPackage2;
                                    ParseInput parseInput5 = parseInput3;
                                    ParseResult<ParsedProvider> providerResult = ParsedProviderUtils.parseProvider(this.mSeparateProcesses, pkg, res, parser, flags, sUseRoundIcon, defaultSplitName, input);
                                    if (providerResult.isSuccess()) {
                                        ParsedProvider provider = (ParsedProvider) providerResult.getResult();
                                        parsingPackage4.addProvider(provider);
                                    }
                                    result = providerResult;
                                    parseInput2 = parseInput5;
                                    break;
                                case 4:
                                    ParseResult<ParsedActivity> activityResult2 = ParsedActivityUtils.parseActivityAlias(pkg, res, parser, sUseRoundIcon, defaultSplitName, input);
                                    if (activityResult2.isSuccess()) {
                                        parsingPackage2.addActivity((ParsedActivity) activityResult2.getResult());
                                    }
                                    result = activityResult2;
                                    depth = depth2;
                                    i = i2;
                                    sa2 = sa3;
                                    parseInput2 = parseInput3;
                                    break;
                                default:
                                    depth = depth2;
                                    i = i2;
                                    sa2 = sa3;
                                    parseInput2 = parseInput3;
                                    result = parseSplitBaseAppChildTags(input, tagName, pkg, res, parser);
                                    break;
                            }
                            if (result.isError()) {
                                return parseInput2.error(result);
                            }
                            if (hasTooManyComponents(pkg)) {
                                return parseInput2.error(MAX_NUM_COMPONENTS_ERR_MSG);
                            }
                            parsingPackage2 = pkg;
                            parseInput3 = parseInput2;
                            depth2 = depth;
                            i2 = i;
                            sa3 = sa2;
                        }
                    }
                }
            }
            return input.success(pkg);
        } catch (Throwable th2) {
            th = th2;
            sa = sa3;
        }
    }

    private static boolean hasTooManyComponents(ParsingPackage pkg) {
        return (pkg.getActivities().size() + pkg.getServices().size()) + pkg.getProviders().size() > 30000;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private ParseResult parseSplitBaseAppChildTags(ParseInput input, String tag, ParsingPackage pkg, Resources res, XmlResourceParser parser) throws IOException, XmlPullParserException {
        char c;
        switch (tag.hashCode()) {
            case -1608941274:
                if (tag.equals("uses-native-library")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case -1521117785:
                if (tag.equals("uses-sdk-library")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -1356765254:
                if (tag.equals("uses-library")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case -1115949454:
                if (tag.equals("meta-data")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case -993141291:
                if (tag.equals("property")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 8960125:
                if (tag.equals("uses-static-library")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 1964930885:
                if (tag.equals("uses-package")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                ParseResult<PackageManager.Property> metaDataResult = parseMetaData(pkg, null, res, parser, "<meta-data>", input);
                if (metaDataResult.isSuccess() && metaDataResult.getResult() != null) {
                    pkg.setMetaData(((PackageManager.Property) metaDataResult.getResult()).toBundle(pkg.getMetaData()));
                }
                return metaDataResult;
            case 1:
                ParseResult<PackageManager.Property> propertyResult = parseMetaData(pkg, null, res, parser, "<property>", input);
                if (propertyResult.isSuccess()) {
                    pkg.addProperty((PackageManager.Property) propertyResult.getResult());
                }
                return propertyResult;
            case 2:
                return parseUsesSdkLibrary(input, pkg, res, parser);
            case 3:
                return parseUsesStaticLibrary(input, pkg, res, parser);
            case 4:
                return parseUsesLibrary(input, pkg, res, parser);
            case 5:
                return parseUsesNativeLibrary(input, pkg, res, parser);
            case 6:
                return input.success((Object) null);
            default:
                return ParsingUtils.unknownTag("<application>", pkg, parser, input);
        }
    }

    private ParseResult<ParsingPackage> parseBaseApkTags(ParseInput input, ParsingPackage pkg, TypedArray sa, Resources res, XmlResourceParser parser, int flags) throws XmlPullParserException, IOException {
        ParseResult result;
        ParseResult<ParsingPackage> sharedUserResult = parseSharedUser(input, pkg, sa);
        if (sharedUserResult.isError()) {
            return sharedUserResult;
        }
        pkg.setInstallLocation(anInteger(-1, 4, sa)).setTargetSandboxVersion(anInteger(1, 7, sa)).setExternalStorage((flags & 8) != 0);
        int depth = parser.getDepth();
        boolean foundApp = false;
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= depth)) {
                break;
            } else if (type == 2) {
                String tagName = parser.getName();
                if (TAG_APPLICATION.equals(tagName)) {
                    if (foundApp) {
                        Slog.w("PackageParsing", "<manifest> has more than one <application>");
                        result = input.success((Object) null);
                    } else {
                        foundApp = true;
                        result = parseBaseApplication(input, pkg, res, parser, flags);
                    }
                } else {
                    result = parseBaseApkTag(tagName, input, pkg, res, parser, flags);
                }
                if (result.isError()) {
                    return input.error(result);
                }
            }
        }
        if (!foundApp && ArrayUtils.size(pkg.getInstrumentations()) == 0) {
            ParseResult<?> deferResult = input.deferError("<manifest> does not contain an <application> or <instrumentation>", 150776642L);
            if (deferResult.isError()) {
                return input.error(deferResult);
            }
        }
        if (!ParsedAttributionUtils.isCombinationValid(pkg.getAttributions())) {
            return input.error((int) GnssNative.GeofenceCallbacks.GEOFENCE_STATUS_ERROR_ID_EXISTS, "Combination <attribution> tags are not valid");
        }
        if (ParsedPermissionUtils.declareDuplicatePermission(pkg)) {
            return input.error(-108, "Found duplicate permission with a different attribute value.");
        }
        convertCompatPermissions(pkg);
        convertSplitPermissions(pkg);
        if (pkg.getTargetSdkVersion() < 4 || (!pkg.isSupportsSmallScreens() && !pkg.isSupportsNormalScreens() && !pkg.isSupportsLargeScreens() && !pkg.isSupportsExtraLargeScreens() && !pkg.isResizeable() && !pkg.isAnyDensity())) {
            adjustPackageToBeUnresizeableAndUnpipable(pkg);
        }
        CtaLinkManagerFactory.getInstance().makeCtaLinkManager().linkCtaPermissions(pkg);
        return input.success(pkg);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private ParseResult parseBaseApkTag(String tag, ParseInput input, ParsingPackage pkg, Resources res, XmlResourceParser parser, int flags) throws IOException, XmlPullParserException {
        char c;
        switch (tag.hashCode()) {
            case -1773650763:
                if (tag.equals(TAG_USES_CONFIGURATION)) {
                    c = '\n';
                    break;
                }
                c = 65535;
                break;
            case -1667688228:
                if (tag.equals(TAG_PERMISSION_TREE)) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case -1108197302:
                if (tag.equals(TAG_ORIGINAL_PACKAGE)) {
                    c = 17;
                    break;
                }
                c = 65535;
                break;
            case -1091287984:
                if (tag.equals(TAG_OVERLAY)) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case -998269702:
                if (tag.equals(TAG_RESTRICT_UPDATE)) {
                    c = 23;
                    break;
                }
                c = 65535;
                break;
            case -979207434:
                if (tag.equals("feature")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -517618225:
                if (tag.equals(TAG_PERMISSION)) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case -309882753:
                if (tag.equals(TAG_ATTRIBUTION)) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case -266709319:
                if (tag.equals(TAG_USES_SDK)) {
                    c = '\r';
                    break;
                }
                c = 65535;
                break;
            case -170723071:
                if (tag.equals(TAG_PERMISSION_GROUP)) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case -129269526:
                if (tag.equals(TAG_EAT_COMMENT)) {
                    c = 22;
                    break;
                }
                c = 65535;
                break;
            case 119109844:
                if (tag.equals(TAG_USES_GL_TEXTURE)) {
                    c = 19;
                    break;
                }
                c = 65535;
                break;
            case 349565761:
                if (tag.equals(TAG_SUPPORTS_INPUT)) {
                    c = 21;
                    break;
                }
                c = 65535;
                break;
            case 454915839:
                if (tag.equals(TAG_KEY_SETS)) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 544550766:
                if (tag.equals(TAG_INSTRUMENTATION)) {
                    c = 16;
                    break;
                }
                c = 65535;
                break;
            case 599862896:
                if (tag.equals(TAG_USES_PERMISSION)) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case 632228327:
                if (tag.equals(TAG_ADOPT_PERMISSIONS)) {
                    c = 18;
                    break;
                }
                c = 65535;
                break;
            case 655087462:
                if (tag.equals(TAG_QUERIES)) {
                    c = 24;
                    break;
                }
                c = 65535;
                break;
            case 896788286:
                if (tag.equals(TAG_SUPPORT_SCREENS)) {
                    c = 14;
                    break;
                }
                c = 65535;
                break;
            case 1343942321:
                if (tag.equals(TAG_USES_PERMISSION_SDK_23)) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case 1439495522:
                if (tag.equals(TAG_PROTECTED_BROADCAST)) {
                    c = 15;
                    break;
                }
                c = 65535;
                break;
            case 1682371816:
                if (tag.equals(TAG_FEATURE_GROUP)) {
                    c = '\f';
                    break;
                }
                c = 65535;
                break;
            case 1705921021:
                if (tag.equals(TAG_USES_PERMISSION_SDK_M)) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case 1792785909:
                if (tag.equals(TAG_USES_FEATURE)) {
                    c = 11;
                    break;
                }
                c = 65535;
                break;
            case 1818228622:
                if (tag.equals(TAG_COMPATIBLE_SCREENS)) {
                    c = 20;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                return parseOverlay(input, pkg, res, parser);
            case 1:
                return parseKeySets(input, pkg, res, parser);
            case 2:
            case 3:
                return parseAttribution(input, pkg, res, parser);
            case 4:
                return parsePermissionGroup(input, pkg, res, parser);
            case 5:
                return parsePermission(input, pkg, res, parser);
            case 6:
                return parsePermissionTree(input, pkg, res, parser);
            case 7:
            case '\b':
            case '\t':
                return parseUsesPermission(input, pkg, res, parser);
            case '\n':
                return parseUsesConfiguration(input, pkg, res, parser);
            case 11:
                return parseUsesFeature(input, pkg, res, parser);
            case '\f':
                return parseFeatureGroup(input, pkg, res, parser);
            case '\r':
                return parseUsesSdk(input, pkg, res, parser, flags);
            case 14:
                return parseSupportScreens(input, pkg, res, parser);
            case 15:
                return parseProtectedBroadcast(input, pkg, res, parser);
            case 16:
                return parseInstrumentation(input, pkg, res, parser);
            case 17:
                return parseOriginalPackage(input, pkg, res, parser);
            case 18:
                return parseAdoptPermissions(input, pkg, res, parser);
            case 19:
            case 20:
            case 21:
            case 22:
                XmlUtils.skipCurrentTag(parser);
                return input.success(pkg);
            case 23:
                return parseRestrictUpdateHash(flags, input, pkg, res, parser);
            case 24:
                return parseQueries(input, pkg, res, parser);
            default:
                return ParsingUtils.unknownTag("<manifest>", pkg, parser, input);
        }
    }

    private static ParseResult<ParsingPackage> parseSharedUser(ParseInput input, ParsingPackage pkg, TypedArray sa) {
        boolean z = false;
        String str = nonConfigString(0, 0, sa);
        if (TextUtils.isEmpty(str)) {
            return input.success(pkg);
        }
        if (!PackageManagerService.PLATFORM_PACKAGE_NAME.equals(pkg.getPackageName())) {
            ParseResult<?> nameResult = FrameworkParsingPackageUtils.validateName(input, str, true, true);
            if (nameResult.isError()) {
                return input.error(-107, "<manifest> specifies bad sharedUserId name \"" + str + "\": " + nameResult.getErrorMessage());
            }
        }
        boolean leaving = false;
        if (!SharedUidMigration.isDisabled()) {
            int max = anInteger(0, 13, sa);
            if (max != 0 && max < Build.VERSION.RESOURCES_SDK_INT) {
                z = true;
            }
            leaving = z;
        }
        return input.success(pkg.setLeavingSharedUid(leaving).setSharedUserId(str.intern()).setSharedUserLabel(resId(3, sa)));
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1202=7] */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:105:0x0234, code lost:
        r0 = r22.getPackageName();
        r5 = r7.keySet();
     */
    /* JADX WARN: Code restructure failed: missing block: B:106:0x0246, code lost:
        if (r5.removeAll(r9.keySet()) == false) goto L15;
     */
    /* JADX WARN: Code restructure failed: missing block: B:108:0x0263, code lost:
        return r21.error("Package" + r0 + " AndroidManifest.xml 'key-set' and 'public-key' names must be distinct.");
     */
    /* JADX WARN: Code restructure failed: missing block: B:109:0x0264, code lost:
        r6 = r9.entrySet().iterator();
     */
    /* JADX WARN: Code restructure failed: missing block: B:111:0x0270, code lost:
        if (r6.hasNext() == false) goto L38;
     */
    /* JADX WARN: Code restructure failed: missing block: B:112:0x0272, code lost:
        r14 = r6.next();
        r15 = r14.getKey();
     */
    /* JADX WARN: Code restructure failed: missing block: B:113:0x028a, code lost:
        if (r14.getValue().size() != 0) goto L20;
     */
    /* JADX WARN: Code restructure failed: missing block: B:114:0x028c, code lost:
        android.util.Slog.w("PackageParsing", "Package" + r0 + " AndroidManifest.xml 'key-set' " + r15 + " has no valid associated 'public-key'. Not including in package's defined key-sets.");
     */
    /* JADX WARN: Code restructure failed: missing block: B:116:0x02b7, code lost:
        if (r10.contains(r15) == false) goto L23;
     */
    /* JADX WARN: Code restructure failed: missing block: B:117:0x02b9, code lost:
        android.util.Slog.w("PackageParsing", "Package" + r0 + " AndroidManifest.xml 'key-set' " + r15 + " contained improper 'public-key' tags. Not including in package's defined key-sets.");
     */
    /* JADX WARN: Code restructure failed: missing block: B:118:0x02e0, code lost:
        r3 = r14.getValue().iterator();
     */
    /* JADX WARN: Code restructure failed: missing block: B:120:0x02ee, code lost:
        if (r3.hasNext() == false) goto L28;
     */
    /* JADX WARN: Code restructure failed: missing block: B:121:0x02f0, code lost:
        r4 = r3.next();
        r22.addKeySet(r15, r7.get(r4));
        r3 = r3;
     */
    /* JADX WARN: Code restructure failed: missing block: B:124:0x0318, code lost:
        if (r22.getKeySetMapping().keySet().containsAll(r8) == false) goto L43;
     */
    /* JADX WARN: Code restructure failed: missing block: B:125:0x031a, code lost:
        r22.setUpgradeKeySets(r8);
     */
    /* JADX WARN: Code restructure failed: missing block: B:126:0x0321, code lost:
        return r21.success(r22);
     */
    /* JADX WARN: Code restructure failed: missing block: B:128:0x033d, code lost:
        return r21.error("Package" + r0 + " AndroidManifest.xml does not define all 'upgrade-key-set's .");
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private static ParseResult<ParsingPackage> parseKeySets(ParseInput input, ParsingPackage pkg, Resources res, XmlResourceParser parser) throws XmlPullParserException, IOException {
        int outerDepth;
        int currentKeySetDepth;
        char c;
        TypedArray sa;
        String publicKeyName;
        String encodedKey;
        int outerDepth2 = parser.getDepth();
        ArrayMap<String, PublicKey> publicKeys = new ArrayMap<>();
        ArraySet<String> upgradeKeySets = new ArraySet<>();
        ArrayMap<String, ArraySet<String>> definedKeySets = new ArrayMap<>();
        ArraySet<String> improperKeySets = new ArraySet<>();
        String currentKeySet = null;
        int currentKeySetDepth2 = -1;
        while (true) {
            int type = parser.next();
            if (type != 1 && (type != 3 || parser.getDepth() > outerDepth2)) {
                if (type != 3) {
                    String tagName = parser.getName();
                    switch (tagName.hashCode()) {
                        case -1369233085:
                            if (tagName.equals("upgrade-key-set")) {
                                c = 2;
                                break;
                            }
                            c = 65535;
                            break;
                        case -816609292:
                            if (tagName.equals("key-set")) {
                                c = 0;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1903323387:
                            if (tagName.equals("public-key")) {
                                c = 1;
                                break;
                            }
                            c = 65535;
                            break;
                        default:
                            c = 65535;
                            break;
                    }
                    switch (c) {
                        case 0:
                            outerDepth = outerDepth2;
                            if (currentKeySet != null) {
                                return input.error("Improperly nested 'key-set' tag at " + parser.getPositionDescription());
                            }
                            sa = res.obtainAttributes(parser, R.styleable.AndroidManifestKeySet);
                            try {
                                String keysetName = sa.getNonResourceString(0);
                                definedKeySets.put(keysetName, new ArraySet<>());
                                currentKeySet = keysetName;
                                int currentKeySetDepth3 = parser.getDepth();
                                sa.recycle();
                                currentKeySetDepth2 = currentKeySetDepth3;
                                outerDepth2 = outerDepth;
                                break;
                            } finally {
                            }
                        case 1:
                            if (currentKeySet != null) {
                                sa = res.obtainAttributes(parser, R.styleable.AndroidManifestPublicKey);
                                try {
                                    publicKeyName = nonResString(0, sa);
                                    outerDepth = outerDepth2;
                                    try {
                                        encodedKey = nonResString(1, sa);
                                        if (encodedKey == null) {
                                            try {
                                                if (publicKeys.get(publicKeyName) == null) {
                                                    try {
                                                        return input.error("'public-key' " + publicKeyName + " must define a public-key value on first use at " + parser.getPositionDescription());
                                                    } catch (Throwable th) {
                                                        th = th;
                                                    }
                                                }
                                            } catch (Throwable th2) {
                                                th = th2;
                                            }
                                        }
                                        currentKeySetDepth = currentKeySetDepth2;
                                    } catch (Throwable th3) {
                                        th = th3;
                                    }
                                } catch (Throwable th4) {
                                    th = th4;
                                }
                                if (encodedKey != null) {
                                    try {
                                        PublicKey currentKey = FrameworkParsingPackageUtils.parsePublicKey(encodedKey);
                                        if (currentKey == null) {
                                            try {
                                                Slog.w("PackageParsing", "No recognized valid key in 'public-key' tag at " + parser.getPositionDescription() + " key-set " + currentKeySet + " will not be added to the package's defined key-sets.");
                                                improperKeySets.add(currentKeySet);
                                                XmlUtils.skipCurrentTag(parser);
                                                currentKeySetDepth2 = currentKeySetDepth;
                                                outerDepth2 = outerDepth;
                                                break;
                                            } catch (Throwable th5) {
                                                th = th5;
                                            }
                                        } else {
                                            if (publicKeys.get(publicKeyName) != null && !publicKeys.get(publicKeyName).equals(currentKey)) {
                                                return input.error("Value of 'public-key' " + publicKeyName + " conflicts with previously defined value at " + parser.getPositionDescription());
                                            }
                                            publicKeys.put(publicKeyName, currentKey);
                                        }
                                        th = th5;
                                    } catch (Throwable th6) {
                                        th = th6;
                                    }
                                    throw th;
                                }
                                definedKeySets.get(currentKeySet).add(publicKeyName);
                                XmlUtils.skipCurrentTag(parser);
                                currentKeySetDepth2 = currentKeySetDepth;
                                outerDepth2 = outerDepth;
                                break;
                            } else {
                                return input.error("Improperly nested 'key-set' tag at " + parser.getPositionDescription());
                            }
                        case 2:
                            sa = res.obtainAttributes(parser, R.styleable.AndroidManifestUpgradeKeySet);
                            try {
                                String name = sa.getNonResourceString(0);
                                upgradeKeySets.add(name);
                                XmlUtils.skipCurrentTag(parser);
                                sa.recycle();
                                outerDepth = outerDepth2;
                                currentKeySetDepth = currentKeySetDepth2;
                                currentKeySetDepth2 = currentKeySetDepth;
                                outerDepth2 = outerDepth;
                                break;
                            } finally {
                            }
                        default:
                            outerDepth = outerDepth2;
                            currentKeySetDepth = currentKeySetDepth2;
                            ParseResult result = ParsingUtils.unknownTag("<key-sets>", pkg, parser, input);
                            if (result.isError()) {
                                return input.error(result);
                            }
                            currentKeySetDepth2 = currentKeySetDepth;
                            outerDepth2 = outerDepth;
                            break;
                    }
                } else if (parser.getDepth() == currentKeySetDepth2) {
                    currentKeySet = null;
                    currentKeySetDepth2 = -1;
                } else {
                    outerDepth = outerDepth2;
                    currentKeySetDepth = currentKeySetDepth2;
                    currentKeySetDepth2 = currentKeySetDepth;
                    outerDepth2 = outerDepth;
                }
            }
        }
    }

    private static ParseResult<ParsingPackage> parseAttribution(ParseInput input, ParsingPackage pkg, Resources res, XmlResourceParser parser) throws IOException, XmlPullParserException {
        ParseResult<ParsedAttribution> result = ParsedAttributionUtils.parseAttribution(res, parser, input);
        if (result.isError()) {
            return input.error(result);
        }
        return input.success(pkg.addAttribution((ParsedAttribution) result.getResult()));
    }

    private static ParseResult<ParsingPackage> parsePermissionGroup(ParseInput input, ParsingPackage pkg, Resources res, XmlResourceParser parser) throws XmlPullParserException, IOException {
        ParseResult<ParsedPermissionGroup> result = ParsedPermissionUtils.parsePermissionGroup(pkg, res, parser, sUseRoundIcon, input);
        if (result.isError()) {
            return input.error(result);
        }
        return input.success(pkg.addPermissionGroup((ParsedPermissionGroup) result.getResult()));
    }

    private static ParseResult<ParsingPackage> parsePermission(ParseInput input, ParsingPackage pkg, Resources res, XmlResourceParser parser) throws XmlPullParserException, IOException {
        ParseResult<ParsedPermission> result = ParsedPermissionUtils.parsePermission(pkg, res, parser, sUseRoundIcon, input);
        if (result.isError()) {
            return input.error(result);
        }
        return input.success(pkg.addPermission((ParsedPermission) result.getResult()));
    }

    private static ParseResult<ParsingPackage> parsePermissionTree(ParseInput input, ParsingPackage pkg, Resources res, XmlResourceParser parser) throws XmlPullParserException, IOException {
        ParseResult<ParsedPermission> result = ParsedPermissionUtils.parsePermissionTree(pkg, res, parser, sUseRoundIcon, input);
        if (result.isError()) {
            return input.error(result);
        }
        return input.success(pkg.addPermission((ParsedPermission) result.getResult()));
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1441=9] */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private ParseResult<ParsingPackage> parseUsesPermission(ParseInput input, ParsingPackage pkg, Resources res, XmlResourceParser parser) throws IOException, XmlPullParserException {
        int type;
        int maxSdkVersion;
        int i;
        int outerDepth;
        char c;
        ParseResult<?> result;
        ParseInput parseInput = input;
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestUsesPermission);
        try {
            String name = sa.getNonResourceString(0);
            if (TextUtils.length(name) > 512) {
                return parseInput.error(-108, "The name in the <uses-permission> is greater than 512");
            }
            int maxSdkVersion2 = 0;
            int i2 = 1;
            TypedValue val = sa.peekValue(1);
            if (val != null && val.type >= 16 && val.type <= 31) {
                maxSdkVersion2 = val.data;
            }
            ArraySet<String> requiredFeatures = new ArraySet<>();
            String feature = sa.getNonConfigurationString(2, 0);
            if (feature != null) {
                requiredFeatures.add(feature);
            }
            ArraySet<String> requiredNotFeatures = new ArraySet<>();
            int i3 = 3;
            String feature2 = sa.getNonConfigurationString(3, 0);
            if (feature2 != null) {
                requiredNotFeatures.add(feature2);
            }
            int usesPermissionFlags = sa.getInt(4, 0);
            int outerDepth2 = parser.getDepth();
            while (true) {
                int type2 = parser.next();
                if (type2 != i2) {
                    type = type2;
                    if (type == i3) {
                        outerDepth = outerDepth2;
                        if (parser.getDepth() > outerDepth) {
                        }
                    } else {
                        outerDepth = outerDepth2;
                    }
                    if (type == i3) {
                        outerDepth2 = outerDepth;
                        i2 = 1;
                        i3 = 3;
                    } else if (type == 4) {
                        outerDepth2 = outerDepth;
                        i2 = 1;
                    } else {
                        String name2 = parser.getName();
                        switch (name2.hashCode()) {
                            case 874138830:
                                if (name2.equals("required-not-feature")) {
                                    c = 1;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 1693350600:
                                if (name2.equals("required-feature")) {
                                    c = 0;
                                    break;
                                }
                                c = 65535;
                                break;
                            default:
                                c = 65535;
                                break;
                        }
                        switch (c) {
                            case 0:
                                result = parseRequiredFeature(parseInput, res, parser);
                                if (result.isSuccess()) {
                                    requiredFeatures.add((String) result.getResult());
                                    break;
                                }
                                break;
                            case 1:
                                result = parseRequiredNotFeature(parseInput, res, parser);
                                if (result.isSuccess()) {
                                    requiredNotFeatures.add((String) result.getResult());
                                    break;
                                }
                                break;
                            default:
                                result = ParsingUtils.unknownTag("<uses-permission>", pkg, parser, parseInput);
                                break;
                        }
                        if (result.isError()) {
                            return parseInput.error(result);
                        }
                        outerDepth2 = outerDepth;
                        i2 = 1;
                        i3 = 3;
                    }
                } else {
                    type = type2;
                }
            }
            ParseResult<ParsingPackage> success = input.success(pkg);
            if (name == null) {
                return success;
            }
            if (maxSdkVersion2 == 0 || maxSdkVersion2 >= Build.VERSION.RESOURCES_SDK_INT) {
                if (this.mCallback != null) {
                    int i4 = requiredFeatures.size() - 1;
                    while (i4 >= 0) {
                        int type3 = type;
                        if (!this.mCallback.hasFeature(requiredFeatures.valueAt(i4))) {
                            return success;
                        }
                        i4--;
                        type = type3;
                    }
                    for (int i5 = requiredNotFeatures.size() - 1; i5 >= 0; i5--) {
                        if (this.mCallback.hasFeature(requiredNotFeatures.valueAt(i5))) {
                            return success;
                        }
                    }
                }
                List<ParsedUsesPermission> usesPermissions = pkg.getUsesPermissions();
                int size = usesPermissions.size();
                int i6 = 0;
                while (true) {
                    if (i6 < size) {
                        ParsedUsesPermission usesPermission = usesPermissions.get(i6);
                        if (Objects.equals(usesPermission.getName(), name)) {
                            maxSdkVersion = usesPermissionFlags;
                            if (usesPermission.getUsesPermissionFlags() != maxSdkVersion) {
                                return parseInput.error("Conflicting uses-permissions flags: " + name + " in package: " + pkg.getPackageName() + " at: " + parser.getPositionDescription());
                            }
                            Slog.w("PackageParsing", "Ignoring duplicate uses-permissions/uses-permissions-sdk-m: " + name + " in package: " + pkg.getPackageName() + " at: " + parser.getPositionDescription());
                            i = 1;
                        } else {
                            i6++;
                            parseInput = input;
                            usesPermissionFlags = usesPermissionFlags;
                            maxSdkVersion2 = maxSdkVersion2;
                        }
                    } else {
                        maxSdkVersion = usesPermissionFlags;
                        i = 0;
                    }
                }
                if (i == 0) {
                    pkg.addUsesPermission(new ParsedUsesPermissionImpl(name, maxSdkVersion));
                }
                return success;
            }
            return success;
        } finally {
            sa.recycle();
        }
    }

    private ParseResult<String> parseRequiredFeature(ParseInput input, Resources res, AttributeSet attrs) {
        ParseResult<String> success;
        TypedArray sa = res.obtainAttributes(attrs, R.styleable.AndroidManifestRequiredFeature);
        try {
            String featureName = sa.getString(0);
            if (TextUtils.isEmpty(featureName)) {
                success = input.error("Feature name is missing from <required-feature> tag.");
            } else {
                success = input.success(featureName);
            }
            return success;
        } finally {
            sa.recycle();
        }
    }

    private ParseResult<String> parseRequiredNotFeature(ParseInput input, Resources res, AttributeSet attrs) {
        ParseResult<String> success;
        TypedArray sa = res.obtainAttributes(attrs, R.styleable.AndroidManifestRequiredNotFeature);
        try {
            String featureName = sa.getString(0);
            if (TextUtils.isEmpty(featureName)) {
                success = input.error("Feature name is missing from <required-not-feature> tag.");
            } else {
                success = input.success(featureName);
            }
            return success;
        } finally {
            sa.recycle();
        }
    }

    private static ParseResult<ParsingPackage> parseUsesConfiguration(ParseInput input, ParsingPackage pkg, Resources res, XmlResourceParser parser) {
        ConfigurationInfo cPref = new ConfigurationInfo();
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestUsesConfiguration);
        try {
            cPref.reqTouchScreen = sa.getInt(0, 0);
            cPref.reqKeyboardType = sa.getInt(1, 0);
            if (sa.getBoolean(2, false)) {
                cPref.reqInputFeatures = 1 | cPref.reqInputFeatures;
            }
            cPref.reqNavigation = sa.getInt(3, 0);
            if (sa.getBoolean(4, false)) {
                cPref.reqInputFeatures |= 2;
            }
            pkg.addConfigPreference(cPref);
            return input.success(pkg);
        } finally {
            sa.recycle();
        }
    }

    private static ParseResult<ParsingPackage> parseUsesFeature(ParseInput input, ParsingPackage pkg, Resources res, XmlResourceParser parser) {
        FeatureInfo fi = parseFeatureInfo(res, parser);
        pkg.addReqFeature(fi);
        if (fi.name == null) {
            ConfigurationInfo cPref = new ConfigurationInfo();
            cPref.reqGlEsVersion = fi.reqGlEsVersion;
            pkg.addConfigPreference(cPref);
        }
        return input.success(pkg);
    }

    private static FeatureInfo parseFeatureInfo(Resources res, AttributeSet attrs) {
        FeatureInfo fi = new FeatureInfo();
        TypedArray sa = res.obtainAttributes(attrs, R.styleable.AndroidManifestUsesFeature);
        try {
            fi.name = sa.getNonResourceString(0);
            fi.version = sa.getInt(3, 0);
            if (fi.name == null) {
                fi.reqGlEsVersion = sa.getInt(1, 0);
            }
            if (sa.getBoolean(2, true)) {
                fi.flags |= 1;
            }
            return fi;
        } finally {
            sa.recycle();
        }
    }

    private static ParseResult<ParsingPackage> parseFeatureGroup(ParseInput input, ParsingPackage pkg, Resources res, XmlResourceParser parser) throws IOException, XmlPullParserException {
        FeatureGroupInfo group = new FeatureGroupInfo();
        ArrayList<FeatureInfo> features = null;
        int depth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= depth)) {
                break;
            } else if (type == 2) {
                String innerTagName = parser.getName();
                if (innerTagName.equals(TAG_USES_FEATURE)) {
                    FeatureInfo featureInfo = parseFeatureInfo(res, parser);
                    featureInfo.flags = 1 | featureInfo.flags;
                    features = ArrayUtils.add(features, featureInfo);
                } else {
                    Slog.w("PackageParsing", "Unknown element under <feature-group>: " + innerTagName + " at " + pkg.getBaseApkPath() + " " + parser.getPositionDescription());
                }
            }
        }
        if (features != null) {
            group.features = new FeatureInfo[features.size()];
            group.features = (FeatureInfo[]) features.toArray(group.features);
        }
        pkg.addFeatureGroup(group);
        return input.success(pkg);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1690=7] */
    private static ParseResult<ParsingPackage> parseUsesSdk(ParseInput input, ParsingPackage pkg, Resources res, XmlResourceParser parser, int flags) throws IOException, XmlPullParserException {
        int innerDepth;
        ParseResult result;
        int i = SDK_VERSION;
        if (i > 0) {
            boolean isApkInApex = (flags & 512) != 0;
            TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestUsesSdk);
            int minVers = 1;
            String minCode = null;
            boolean minAssigned = false;
            int targetVers = 0;
            String targetCode = null;
            int maxVers = Integer.MAX_VALUE;
            try {
                TypedValue val = sa.peekValue(0);
                if (val != null) {
                    try {
                        if (val.type != 3 || val.string == null) {
                            minVers = val.data;
                            minAssigned = true;
                        } else {
                            minCode = val.string.toString();
                            minAssigned = !TextUtils.isEmpty(minCode);
                        }
                    } catch (Throwable th) {
                        th = th;
                        sa.recycle();
                        throw th;
                    }
                }
                TypedValue val2 = sa.peekValue(1);
                if (val2 == null) {
                    targetVers = minVers;
                    targetCode = minCode;
                } else if (val2.type != 3 || val2.string == null) {
                    targetVers = val2.data;
                } else {
                    targetCode = val2.string.toString();
                    if (!minAssigned) {
                        minCode = targetCode;
                    }
                }
                if (isApkInApex && (val2 = sa.peekValue(2)) != null) {
                    maxVers = val2.data;
                }
                String[] strArr = SDK_CODENAMES;
                ParseResult<Integer> targetSdkVersionResult = FrameworkParsingPackageUtils.computeTargetSdkVersion(targetVers, targetCode, strArr, input, isApkInApex);
                if (targetSdkVersionResult.isError()) {
                    ParseResult<ParsingPackage> error = input.error(targetSdkVersionResult);
                    sa.recycle();
                    return error;
                }
                int targetSdkVersion = ((Integer) targetSdkVersionResult.getResult()).intValue();
                ParseResult<?> deferResult = input.enableDeferredError(pkg.getPackageName(), targetSdkVersion);
                if (deferResult.isError()) {
                    ParseResult<ParsingPackage> error2 = input.error(deferResult);
                    sa.recycle();
                    return error2;
                }
                ParseResult<Integer> minSdkVersionResult = FrameworkParsingPackageUtils.computeMinSdkVersion(minVers, minCode, i, strArr, input);
                if (minSdkVersionResult.isError()) {
                    ParseResult<ParsingPackage> error3 = input.error(minSdkVersionResult);
                    sa.recycle();
                    return error3;
                }
                int type = ((Integer) minSdkVersionResult.getResult()).intValue();
                pkg.setMinSdkVersion(type).setTargetSdkVersion(targetSdkVersion);
                if (isApkInApex) {
                    ParseResult<Integer> maxSdkVersionResult = FrameworkParsingPackageUtils.computeMaxSdkVersion(maxVers, i, input);
                    if (maxSdkVersionResult.isError()) {
                        ParseResult<ParsingPackage> error4 = input.error(maxSdkVersionResult);
                        sa.recycle();
                        return error4;
                    }
                    int maxSdkVersion = ((Integer) maxSdkVersionResult.getResult()).intValue();
                    pkg.setMaxSdkVersion(maxSdkVersion);
                }
                int innerDepth2 = parser.getDepth();
                SparseIntArray minExtensionVersions = null;
                while (true) {
                    int minSdkVersion = type;
                    int type2 = parser.next();
                    boolean isApkInApex2 = isApkInApex;
                    if (type2 == 1) {
                        break;
                    }
                    if (type2 == 3) {
                        try {
                            if (parser.getDepth() <= innerDepth2) {
                                break;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            sa.recycle();
                            throw th;
                        }
                    }
                    if (type2 == 3) {
                        innerDepth = innerDepth2;
                    } else if (type2 == 4) {
                        innerDepth = innerDepth2;
                    } else {
                        int innerDepth3 = innerDepth2;
                        if (parser.getName().equals("extension-sdk")) {
                            if (minExtensionVersions == null) {
                                minExtensionVersions = new SparseIntArray();
                            }
                            result = parseExtensionSdk(input, res, parser, minExtensionVersions);
                            XmlUtils.skipCurrentTag(parser);
                        } else {
                            result = ParsingUtils.unknownTag("<uses-sdk>", pkg, parser, input);
                        }
                        if (result.isError()) {
                            ParseResult<ParsingPackage> error5 = input.error(result);
                            sa.recycle();
                            return error5;
                        }
                        type = minSdkVersion;
                        innerDepth2 = innerDepth3;
                        isApkInApex = isApkInApex2;
                    }
                    type = minSdkVersion;
                    innerDepth2 = innerDepth;
                    isApkInApex = isApkInApex2;
                }
                pkg.setMinExtensionVersions(exactSizedCopyOfSparseArray(minExtensionVersions));
                sa.recycle();
            } catch (Throwable th3) {
                th = th3;
            }
        }
        return input.success(pkg);
    }

    private static SparseIntArray exactSizedCopyOfSparseArray(SparseIntArray input) {
        if (input == null) {
            return null;
        }
        SparseIntArray output = new SparseIntArray(input.size());
        for (int i = 0; i < input.size(); i++) {
            output.put(input.keyAt(i), input.valueAt(i));
        }
        return output;
    }

    private static ParseResult<SparseIntArray> parseExtensionSdk(ParseInput input, Resources res, XmlResourceParser parser, SparseIntArray minExtensionVersions) {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestExtensionSdk);
        try {
            int sdkVersion = sa.getInt(0, -1);
            int minVersion = sa.getInt(1, -1);
            sa.recycle();
            if (sdkVersion < 0) {
                return input.error(-108, "<extension-sdk> must specify an sdkVersion >= 0");
            }
            if (minVersion < 0) {
                return input.error(-108, "<extension-sdk> must specify minExtensionVersion >= 0");
            }
            try {
                int version = SdkExtensions.getExtensionVersion(sdkVersion);
                if (version < minVersion) {
                    return input.error(-12, "Package requires " + sdkVersion + " extension version " + minVersion + " which exceeds device version " + version);
                }
                minExtensionVersions.put(sdkVersion, minVersion);
                return input.success(minExtensionVersions);
            } catch (RuntimeException e) {
                return input.error(-108, "Specified sdkVersion " + sdkVersion + " is not valid");
            }
        } catch (Throwable th) {
            sa.recycle();
            throw th;
        }
    }

    private static ParseResult<ParsingPackage> parseRestrictUpdateHash(int flags, ParseInput input, ParsingPackage pkg, Resources res, XmlResourceParser parser) {
        if ((flags & 16) != 0) {
            TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestRestrictUpdate);
            try {
                String hash = sa.getNonConfigurationString(0, 0);
                if (hash != null) {
                    int hashLength = hash.length();
                    byte[] hashBytes = new byte[hashLength / 2];
                    for (int i = 0; i < hashLength; i += 2) {
                        hashBytes[i / 2] = (byte) ((Character.digit(hash.charAt(i), 16) << 4) + Character.digit(hash.charAt(i + 1), 16));
                    }
                    pkg.setRestrictUpdateHash(hashBytes);
                } else {
                    pkg.setRestrictUpdateHash(null);
                }
            } finally {
                sa.recycle();
            }
        }
        return input.success(pkg);
    }

    /* JADX WARN: Code restructure failed: missing block: B:86:0x01ca, code lost:
        return r20.success(r21);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private static ParseResult<ParsingPackage> parseQueries(ParseInput input, ParsingPackage pkg, Resources res, XmlResourceParser parser) throws IOException, XmlPullParserException {
        int i;
        String dataType;
        int depth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= depth)) {
                break;
            } else if (type == 2) {
                if (parser.getName().equals("intent")) {
                    ParseResult<ParsedIntentInfoImpl> result = ParsedIntentInfoUtils.parseIntentInfo(null, pkg, res, parser, true, true, input);
                    if (result.isError()) {
                        return input.error(result);
                    }
                    IntentFilter intentInfo = ((ParsedIntentInfoImpl) result.getResult()).getIntentFilter();
                    Uri data = null;
                    String dataType2 = null;
                    String host = null;
                    int numActions = intentInfo.countActions();
                    int numSchemes = intentInfo.countDataSchemes();
                    int numTypes = intentInfo.countDataTypes();
                    int numHosts = intentInfo.getHosts().length;
                    if (numSchemes == 0 && numTypes == 0 && numActions == 0) {
                        return input.error("intent tags must contain either an action or data.");
                    }
                    if (numActions > 1) {
                        return input.error("intent tag may have at most one action.");
                    }
                    if (numTypes > 1) {
                        return input.error("intent tag may have at most one data type.");
                    }
                    if (numSchemes > 1) {
                        return input.error("intent tag may have at most one data scheme.");
                    }
                    if (numHosts > 1) {
                        return input.error("intent tag may have at most one data host.");
                    }
                    Intent intent = new Intent();
                    int max = intentInfo.countCategories();
                    int i2 = 0;
                    while (i2 < max) {
                        intent.addCategory(intentInfo.getCategory(i2));
                        i2++;
                        data = data;
                        dataType2 = dataType2;
                    }
                    Uri data2 = data;
                    String dataType3 = dataType2;
                    if (numHosts != 1) {
                        i = 0;
                    } else {
                        i = 0;
                        host = intentInfo.getHosts()[0];
                    }
                    Uri data3 = numSchemes == 1 ? new Uri.Builder().scheme(intentInfo.getDataScheme(i)).authority(host).path("/*").build() : data2;
                    if (numTypes == 1) {
                        dataType = intentInfo.getDataType(0);
                        if (!dataType.contains(SliceClientPermissions.SliceAuthority.DELIMITER)) {
                            dataType = dataType + "/*";
                        }
                        if (data3 == null) {
                            data3 = new Uri.Builder().scheme(ActivityTaskManagerInternal.ASSIST_KEY_CONTENT).authority("*").path("/*").build();
                        }
                    } else {
                        dataType = dataType3;
                    }
                    intent.setDataAndType(data3, dataType);
                    if (numActions == 1) {
                        intent.setAction(intentInfo.getAction(0));
                    }
                    pkg.addQueriesIntent(intent);
                } else if (parser.getName().equals("package")) {
                    String packageName = res.obtainAttributes(parser, R.styleable.AndroidManifestQueriesPackage).getNonConfigurationString(0, 0);
                    if (TextUtils.isEmpty(packageName)) {
                        return input.error("Package name is missing from package tag.");
                    }
                    pkg.addQueriesPackage(packageName.intern());
                } else if (parser.getName().equals("provider")) {
                    TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestQueriesProvider);
                    try {
                        String authorities = sa.getNonConfigurationString(0, 0);
                        if (TextUtils.isEmpty(authorities)) {
                            return input.error(-108, "Authority missing from provider tag.");
                        }
                        StringTokenizer authoritiesTokenizer = new StringTokenizer(authorities, ";");
                        while (authoritiesTokenizer.hasMoreElements()) {
                            pkg.addQueriesProvider(authoritiesTokenizer.nextToken());
                        }
                    } finally {
                        sa.recycle();
                    }
                } else {
                    continue;
                }
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2115=14] */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Removed duplicated region for block: B:226:0x04d4  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private ParseResult<ParsingPackage> parseBaseApplication(ParseInput input, ParsingPackage pkg, Resources res, XmlResourceParser parser, int flags) throws XmlPullParserException, IOException {
        TypedArray sa;
        String taskAffinity;
        ParsingPackage parsingPackage;
        ParseInput parseInput;
        char c;
        boolean z;
        char c2;
        int depth;
        TypedArray sa2;
        int targetSdk;
        String pkgName;
        ParsingPackage parsingPackage2;
        boolean z2;
        ParseResult activityResult;
        ParseResult result;
        ParsingPackage parsingPackage3;
        ParseInput parseInput2;
        boolean z3;
        ParseInput parseInput3 = input;
        ParsingPackage parsingPackage4 = pkg;
        Resources resources = res;
        XmlResourceParser xmlResourceParser = parser;
        String pkgName2 = pkg.getPackageName();
        int targetSdk2 = pkg.getTargetSdkVersion();
        TypedArray sa3 = resources.obtainAttributes(xmlResourceParser, R.styleable.AndroidManifestApplication);
        try {
            if (sa3 == null) {
                ParseResult<ParsingPackage> error = parseInput3.error("<application> does not contain any attributes");
                sa3.recycle();
                return error;
            }
            try {
                String name = sa3.getNonConfigurationString(3, 0);
                if (name != null) {
                    String packageName = pkg.getPackageName();
                    String outInfoName = ParsingUtils.buildClassName(packageName, name);
                    if (PackageManager.APP_DETAILS_ACTIVITY_CLASS_NAME.equals(outInfoName)) {
                        ParseResult<ParsingPackage> error2 = parseInput3.error("<application> invalid android:name");
                        sa3.recycle();
                        return error2;
                    } else if (outInfoName == null) {
                        ParseResult<ParsingPackage> error3 = parseInput3.error("Empty class name in package " + packageName);
                        sa3.recycle();
                        return error3;
                    } else {
                        parsingPackage4.setClassName(outInfoName);
                    }
                }
                TypedValue labelValue = sa3.peekValue(1);
                if (labelValue != null) {
                    parsingPackage4.setLabelRes(labelValue.resourceId);
                    if (labelValue.resourceId == 0) {
                        parsingPackage4.setNonLocalizedLabel(labelValue.coerceToString());
                    }
                }
                parseBaseAppBasicFlags(parsingPackage4, sa3);
                String manageSpaceActivity = nonConfigString(1024, 4, sa3);
                if (manageSpaceActivity != null) {
                    String manageSpaceActivityName = ParsingUtils.buildClassName(pkgName2, manageSpaceActivity);
                    if (manageSpaceActivityName == null) {
                        ParseResult<ParsingPackage> error4 = parseInput3.error("Empty class name in package " + pkgName2);
                        sa3.recycle();
                        return error4;
                    }
                    parsingPackage4.setManageSpaceActivityName(manageSpaceActivityName);
                }
                if (pkg.isAllowBackup()) {
                    String backupAgent = nonConfigString(1024, 16, sa3);
                    if (backupAgent != null) {
                        String backupAgentName = ParsingUtils.buildClassName(pkgName2, backupAgent);
                        if (backupAgentName == null) {
                            ParseResult<ParsingPackage> error5 = parseInput3.error("Empty class name in package " + pkgName2);
                            sa3.recycle();
                            return error5;
                        }
                        parsingPackage4.setBackupAgentName(backupAgentName).setKillAfterRestore(bool(true, 18, sa3)).setRestoreAnyVersion(bool(false, 21, sa3)).setFullBackupOnly(bool(false, 32, sa3)).setBackupInForeground(bool(false, 40, sa3));
                    }
                    TypedValue v = sa3.peekValue(35);
                    if (v != null) {
                        int fullBackupContent = v.resourceId;
                        if (v.resourceId == 0) {
                            fullBackupContent = v.data == 0 ? -1 : 0;
                        }
                        parsingPackage4.setFullBackupContent(fullBackupContent);
                    }
                }
                if (sa3.getBoolean(8, false)) {
                    String requiredFeature = sa3.getNonResourceString(45);
                    if (requiredFeature != null && !this.mCallback.hasFeature(requiredFeature)) {
                        z3 = false;
                        parsingPackage4.setPersistent(z3);
                    }
                    z3 = true;
                    parsingPackage4.setPersistent(z3);
                }
                if (sa3.hasValueOrEmpty(37)) {
                    parsingPackage4.setResizeableActivity(Boolean.valueOf(sa3.getBoolean(37, true)));
                } else {
                    parsingPackage4.setResizeableActivityViaSdkVersion(targetSdk2 >= 24);
                }
                if (targetSdk2 >= 8) {
                    taskAffinity = sa3.getNonConfigurationString(12, 1024);
                } else {
                    String taskAffinity2 = sa3.getNonResourceString(12);
                    taskAffinity = taskAffinity2;
                }
                ParseResult<String> taskAffinityResult = ComponentParseUtils.buildTaskAffinityName(pkgName2, pkgName2, taskAffinity, parseInput3);
                if (taskAffinityResult.isError()) {
                    ParseResult<ParsingPackage> error6 = parseInput3.error(taskAffinityResult);
                    sa3.recycle();
                    return error6;
                }
                parsingPackage4.setTaskAffinity((String) taskAffinityResult.getResult());
                String factory = sa3.getNonResourceString(48);
                if (factory != null) {
                    String appComponentFactory = ParsingUtils.buildClassName(pkgName2, factory);
                    if (appComponentFactory == null) {
                        ParseResult<ParsingPackage> error7 = parseInput3.error("Empty class name in package " + pkgName2);
                        sa3.recycle();
                        return error7;
                    }
                    parsingPackage4.setAppComponentFactory(appComponentFactory);
                }
                ParseResult<String> processNameResult = ComponentParseUtils.buildProcessName(pkgName2, null, targetSdk2 >= 8 ? sa3.getNonConfigurationString(11, 1024) : sa3.getNonResourceString(11), flags, this.mSeparateProcesses, input);
                if (processNameResult.isError()) {
                    ParseResult<ParsingPackage> error8 = parseInput3.error(processNameResult);
                    sa3.recycle();
                    return error8;
                }
                String processName = (String) processNameResult.getResult();
                parsingPackage4.setProcessName(processName);
                if (pkg.isCantSaveState() && processName != null && !processName.equals(pkgName2)) {
                    ParseResult<ParsingPackage> error9 = parseInput3.error("cantSaveState applications can not use custom processes");
                    sa3.recycle();
                    return error9;
                }
                String classLoaderName = pkg.getClassLoaderName();
                if (classLoaderName != null && !ClassLoaderFactory.isValidClassLoaderName(classLoaderName)) {
                    ParseResult<ParsingPackage> error10 = parseInput3.error("Invalid class loader name: " + classLoaderName);
                    sa3.recycle();
                    return error10;
                }
                char c3 = 65535;
                parsingPackage4.setGwpAsanMode(sa3.getInt(62, -1));
                parsingPackage4.setMemtagMode(sa3.getInt(64, -1));
                if (sa3.hasValue(65)) {
                    parsingPackage4.setNativeHeapZeroInitialized(sa3.getBoolean(65, false) ? 1 : 0);
                }
                if (sa3.hasValue(67)) {
                    parsingPackage4.setRequestRawExternalStorageAccess(Boolean.valueOf(sa3.getBoolean(67, false)));
                }
                if (sa3.hasValue(68)) {
                    parsingPackage4.setRequestForegroundServiceExemption(sa3.getBoolean(68, false));
                }
                ParseResult<Set<String>> knownActivityEmbeddingCertsResult = ParsingUtils.parseKnownActivityEmbeddingCerts(sa3, resources, 72, parseInput3);
                if (knownActivityEmbeddingCertsResult.isError()) {
                    ParseResult<ParsingPackage> error11 = parseInput3.error(knownActivityEmbeddingCertsResult);
                    sa3.recycle();
                    return error11;
                }
                Set<String> knownActivityEmbeddingCerts = (Set) knownActivityEmbeddingCertsResult.getResult();
                if (knownActivityEmbeddingCerts != null) {
                    parsingPackage4.setKnownActivityEmbeddingCerts(knownActivityEmbeddingCerts);
                }
                sa3.recycle();
                boolean hasActivityOrder = false;
                int depth2 = parser.getDepth();
                boolean hasReceiverOrder = false;
                boolean hasServiceOrder = false;
                while (true) {
                    int type = parser.next();
                    if (type == 1) {
                        parsingPackage = parsingPackage4;
                        parseInput = parseInput3;
                    } else if (type == 3 && parser.getDepth() <= depth2) {
                        parsingPackage = parsingPackage4;
                        parseInput = parseInput3;
                    } else if (type == 2) {
                        String tagName = parser.getName();
                        boolean isActivity = false;
                        switch (tagName.hashCode()) {
                            case -1655966961:
                                if (tagName.equals(HostingRecord.HOSTING_TYPE_ACTIVITY)) {
                                    c = 0;
                                    break;
                                }
                                c = c3;
                                break;
                            case -1572095710:
                                if (tagName.equals("apex-system-service")) {
                                    c = 5;
                                    break;
                                }
                                c = c3;
                                break;
                            case -987494927:
                                if (tagName.equals("provider")) {
                                    c = 3;
                                    break;
                                }
                                c = c3;
                                break;
                            case -808719889:
                                if (tagName.equals(TAG_RECEIVER)) {
                                    c = 1;
                                    break;
                                }
                                c = c3;
                                break;
                            case 790287890:
                                if (tagName.equals("activity-alias")) {
                                    c = 4;
                                    break;
                                }
                                c = c3;
                                break;
                            case 1984153269:
                                if (tagName.equals(HostingRecord.HOSTING_TYPE_SERVICE)) {
                                    c = 2;
                                    break;
                                }
                                c = c3;
                                break;
                            default:
                                c = c3;
                                break;
                        }
                        switch (c) {
                            case 0:
                                z = true;
                                c2 = c3;
                                depth = depth2;
                                sa2 = sa3;
                                targetSdk = targetSdk2;
                                pkgName = pkgName2;
                                parsingPackage2 = parsingPackage4;
                                z2 = false;
                                isActivity = true;
                                activityResult = ParsedActivityUtils.parseActivityOrReceiver(this.mSeparateProcesses, pkg, res, parser, flags, sUseRoundIcon, null, input);
                                if (activityResult.isSuccess()) {
                                    ParsedActivity activity = (ParsedActivity) activityResult.getResult();
                                    if (isActivity) {
                                        hasActivityOrder |= activity.getOrder() != 0 ? z : z2;
                                        parsingPackage2.addActivity(activity);
                                    } else {
                                        hasReceiverOrder |= activity.getOrder() != 0 ? z : z2;
                                        parsingPackage2.addReceiver(activity);
                                    }
                                }
                                result = activityResult;
                                parsingPackage3 = parsingPackage2;
                                parseInput2 = parseInput3;
                                break;
                            case 1:
                                z = true;
                                c2 = c3;
                                depth = depth2;
                                sa2 = sa3;
                                targetSdk = targetSdk2;
                                pkgName = pkgName2;
                                parsingPackage2 = parsingPackage4;
                                z2 = false;
                                activityResult = ParsedActivityUtils.parseActivityOrReceiver(this.mSeparateProcesses, pkg, res, parser, flags, sUseRoundIcon, null, input);
                                if (activityResult.isSuccess()) {
                                }
                                result = activityResult;
                                parsingPackage3 = parsingPackage2;
                                parseInput2 = parseInput3;
                                break;
                            case 2:
                                c2 = c3;
                                depth = depth2;
                                sa2 = sa3;
                                targetSdk = targetSdk2;
                                pkgName = pkgName2;
                                ParsingPackage parsingPackage5 = parsingPackage4;
                                ParseResult serviceResult = ParsedServiceUtils.parseService(this.mSeparateProcesses, pkg, res, parser, flags, sUseRoundIcon, null, input);
                                if (serviceResult.isSuccess()) {
                                    ParsedService service = (ParsedService) serviceResult.getResult();
                                    hasServiceOrder |= service.getOrder() != 0;
                                    parsingPackage5.addService(service);
                                }
                                result = serviceResult;
                                parsingPackage3 = parsingPackage5;
                                parseInput2 = parseInput3;
                                break;
                            case 3:
                                depth = depth2;
                                c2 = c3;
                                sa2 = sa3;
                                targetSdk = targetSdk2;
                                pkgName = pkgName2;
                                ParsingPackage parsingPackage6 = parsingPackage4;
                                ParseResult providerResult = ParsedProviderUtils.parseProvider(this.mSeparateProcesses, pkg, res, parser, flags, sUseRoundIcon, null, input);
                                if (providerResult.isSuccess()) {
                                    parsingPackage6.addProvider((ParsedProvider) providerResult.getResult());
                                }
                                result = providerResult;
                                parsingPackage3 = parsingPackage6;
                                parseInput2 = parseInput3;
                                break;
                            case 4:
                                char c4 = c3;
                                ParseResult activityResult2 = ParsedActivityUtils.parseActivityAlias(pkg, res, parser, sUseRoundIcon, null, input);
                                if (activityResult2.isSuccess()) {
                                    ParsedActivity activity2 = (ParsedActivity) activityResult2.getResult();
                                    hasActivityOrder |= activity2.getOrder() != 0;
                                    parsingPackage4.addActivity(activity2);
                                }
                                result = activityResult2;
                                depth = depth2;
                                sa2 = sa3;
                                targetSdk = targetSdk2;
                                pkgName = pkgName2;
                                parsingPackage3 = parsingPackage4;
                                c2 = c4;
                                parseInput2 = parseInput3;
                                break;
                            case 5:
                                ParseResult<ParsedApexSystemService> systemServiceResult = ParsedApexSystemServiceUtils.parseApexSystemService(resources, xmlResourceParser, parseInput3);
                                if (systemServiceResult.isSuccess()) {
                                    ParsedApexSystemService systemService = (ParsedApexSystemService) systemServiceResult.getResult();
                                    parsingPackage4.addApexSystemService(systemService);
                                }
                                result = systemServiceResult;
                                c2 = c3;
                                depth = depth2;
                                sa2 = sa3;
                                targetSdk = targetSdk2;
                                pkgName = pkgName2;
                                parsingPackage3 = parsingPackage4;
                                parseInput2 = parseInput3;
                                break;
                            default:
                                c2 = c3;
                                depth = depth2;
                                sa2 = sa3;
                                targetSdk = targetSdk2;
                                pkgName = pkgName2;
                                parsingPackage3 = parsingPackage4;
                                parseInput2 = parseInput3;
                                result = parseBaseAppChildTag(input, tagName, pkg, res, parser, flags);
                                break;
                        }
                        if (result.isError()) {
                            return parseInput2.error(result);
                        }
                        if (hasTooManyComponents(pkg)) {
                            return parseInput2.error(MAX_NUM_COMPONENTS_ERR_MSG);
                        }
                        resources = res;
                        xmlResourceParser = parser;
                        parsingPackage4 = parsingPackage3;
                        parseInput3 = parseInput2;
                        depth2 = depth;
                        c3 = c2;
                        sa3 = sa2;
                        targetSdk2 = targetSdk;
                        pkgName2 = pkgName;
                    }
                }
                if (TextUtils.isEmpty(pkg.getStaticSharedLibName()) && TextUtils.isEmpty(pkg.getSdkLibName())) {
                    ParseResult<ParsedActivity> a = generateAppDetailsHiddenActivity(input, pkg);
                    if (a.isError()) {
                        return parseInput.error(a);
                    }
                    parsingPackage.addActivity((ParsedActivity) a.getResult());
                }
                if (hasActivityOrder) {
                    pkg.sortActivities();
                }
                if (hasReceiverOrder) {
                    pkg.sortReceivers();
                }
                if (hasServiceOrder) {
                    pkg.sortServices();
                }
                setMaxAspectRatio(pkg);
                setMinAspectRatio(parsingPackage);
                setSupportsSizeChanges(parsingPackage);
                addConfigChangesAssetsPaths(parsingPackage);
                parsingPackage.setHasDomainUrls(hasDomainURLs(pkg));
                return input.success(pkg);
            } catch (Throwable th) {
                th = th;
                sa = sa3;
                sa.recycle();
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            sa = sa3;
        }
    }

    private void parseBaseAppBasicFlags(ParsingPackage pkg, TypedArray sa) {
        boolean z;
        boolean z2;
        boolean z3;
        int targetSdk = pkg.getTargetSdkVersion();
        boolean z4 = true;
        ParsingPackage onBackInvokedCallbackEnabled = pkg.setAllowBackup(bool(true, 17, sa)).setAllowClearUserData(bool(true, 5, sa)).setAllowClearUserDataOnFailedRestore(bool(true, 54, sa)).setAllowNativeHeapPointerTagging(bool(true, 59, sa)).setEnabled(bool(true, 9, sa)).setExtractNativeLibs(bool(true, 34, sa)).setHasCode(bool(true, 7, sa)).setAllowTaskReparenting(bool(false, 14, sa)).setCantSaveState(bool(false, 47, sa)).setCrossProfile(bool(false, 58, sa)).setDebuggable(bool(false, 10, sa)).setDefaultToDeviceProtectedStorage(bool(false, 38, sa)).setDirectBootAware(bool(false, 39, sa)).setForceQueryable(bool(false, 57, sa)).setGame(bool(false, 31, sa)).setHasFragileUserData(bool(false, 50, sa)).setLargeHeap(bool(false, 24, sa)).setMultiArch(bool(false, 33, sa)).setPreserveLegacyExternalStorage(bool(false, 61, sa)).setRequiredForAllUsers(bool(false, 27, sa)).setSupportsRtl(bool(false, 26, sa)).setTestOnly(bool(false, 15, sa)).setUseEmbeddedDex(bool(false, 53, sa)).setUsesNonSdkApi(bool(false, 49, sa)).setVmSafeMode(bool(false, 20, sa)).setAutoRevokePermissions(anInt(60, sa)).setAttributionsAreUserVisible(bool(false, 69, sa)).setResetEnabledSettingsOnAppDataCleared(bool(false, 70, sa)).setOnBackInvokedCallbackEnabled(bool(false, 73, sa));
        if (targetSdk >= 29) {
            z = true;
        } else {
            z = false;
        }
        ParsingPackage allowAudioPlaybackCapture = onBackInvokedCallbackEnabled.setAllowAudioPlaybackCapture(bool(z, 55, sa));
        if (targetSdk >= 14) {
            z2 = true;
        } else {
            z2 = false;
        }
        ParsingPackage baseHardwareAccelerated = allowAudioPlaybackCapture.setBaseHardwareAccelerated(bool(z2, 23, sa));
        if (targetSdk < 29) {
            z3 = true;
        } else {
            z3 = false;
        }
        ParsingPackage requestLegacyExternalStorage = baseHardwareAccelerated.setRequestLegacyExternalStorage(bool(z3, 56, sa));
        if (targetSdk >= 28) {
            z4 = false;
        }
        requestLegacyExternalStorage.setUsesCleartextTraffic(bool(z4, 36, sa)).setUiOptions(anInt(25, sa)).setCategory(anInt(-1, 43, sa)).setMaxAspectRatio(aFloat(44, sa)).setMinAspectRatio(aFloat(51, sa)).setBanner(resId(30, sa)).setDescriptionRes(resId(13, sa)).setIconRes(resId(2, sa)).setLogo(resId(22, sa)).setNetworkSecurityConfigRes(resId(41, sa)).setRoundIconRes(resId(42, sa)).setTheme(resId(0, sa)).setDataExtractionRules(resId(66, sa)).setLocaleConfigRes(resId(71, sa)).setClassLoaderName(string(46, sa)).setRequiredAccountType(string(29, sa)).setRestrictedAccountType(string(28, sa)).setZygotePreloadName(string(52, sa)).setPermission(nonConfigString(0, 6, sa));
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private ParseResult parseBaseAppChildTag(ParseInput input, String tag, ParsingPackage pkg, Resources res, XmlResourceParser parser, int flags) throws IOException, XmlPullParserException {
        char c;
        switch (tag.hashCode()) {
            case -1803294168:
                if (tag.equals("sdk-library")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -1608941274:
                if (tag.equals("uses-native-library")) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case -1521117785:
                if (tag.equals("uses-sdk-library")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case -1356765254:
                if (tag.equals("uses-library")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case -1115949454:
                if (tag.equals("meta-data")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case -1094759587:
                if (tag.equals("processes")) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case -1056667556:
                if (tag.equals("static-library")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case -993141291:
                if (tag.equals("property")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 8960125:
                if (tag.equals("uses-static-library")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case 166208699:
                if (tag.equals("library")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 178070147:
                if (tag.equals(TAG_PROFILEABLE)) {
                    c = 11;
                    break;
                }
                c = 65535;
                break;
            case 1964930885:
                if (tag.equals("uses-package")) {
                    c = '\n';
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                ParseResult<PackageManager.Property> metaDataResult = parseMetaData(pkg, null, res, parser, "<meta-data>", input);
                if (metaDataResult.isSuccess() && metaDataResult.getResult() != null) {
                    pkg.setMetaData(((PackageManager.Property) metaDataResult.getResult()).toBundle(pkg.getMetaData()));
                }
                return metaDataResult;
            case 1:
                ParseResult<PackageManager.Property> propertyResult = parseMetaData(pkg, null, res, parser, "<property>", input);
                if (propertyResult.isSuccess()) {
                    pkg.addProperty((PackageManager.Property) propertyResult.getResult());
                }
                return propertyResult;
            case 2:
                return parseSdkLibrary(pkg, res, parser, input);
            case 3:
                return parseStaticLibrary(pkg, res, parser, input);
            case 4:
                return parseLibrary(pkg, res, parser, input);
            case 5:
                return parseUsesSdkLibrary(input, pkg, res, parser);
            case 6:
                return parseUsesStaticLibrary(input, pkg, res, parser);
            case 7:
                return parseUsesLibrary(input, pkg, res, parser);
            case '\b':
                return parseUsesNativeLibrary(input, pkg, res, parser);
            case '\t':
                return parseProcesses(input, pkg, res, parser, this.mSeparateProcesses, flags);
            case '\n':
                return input.success((Object) null);
            case 11:
                return parseProfileable(input, pkg, res, parser);
            default:
                return ParsingUtils.unknownTag("<application>", pkg, parser, input);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2420=5] */
    private static ParseResult<ParsingPackage> parseSdkLibrary(ParsingPackage pkg, Resources res, XmlResourceParser parser, ParseInput input) {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestSdkLibrary);
        try {
            String lname = sa.getNonResourceString(0);
            int versionMajor = sa.getInt(1, -1);
            if (lname != null && versionMajor >= 0) {
                return pkg.getSharedUserId() != null ? input.error(-107, "sharedUserId not allowed in SDK library") : pkg.getSdkLibName() != null ? input.error("Multiple SDKs for package " + pkg.getPackageName()) : input.success(pkg.setSdkLibName(lname.intern()).setSdkLibVersionMajor(versionMajor).setSdkLibrary(true));
            }
            return input.error("Bad sdk-library declaration name: " + lname + " version: " + versionMajor);
        } finally {
            sa.recycle();
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2459=5] */
    private static ParseResult<ParsingPackage> parseStaticLibrary(ParsingPackage pkg, Resources res, XmlResourceParser parser, ParseInput input) {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestStaticLibrary);
        try {
            String lname = sa.getNonResourceString(0);
            int version = sa.getInt(1, -1);
            int versionMajor = sa.getInt(2, 0);
            if (lname != null && version >= 0) {
                return pkg.getSharedUserId() != null ? input.error(-107, "sharedUserId not allowed in static shared library") : pkg.getStaticSharedLibName() != null ? input.error("Multiple static-shared libs for package " + pkg.getPackageName()) : input.success(pkg.setStaticSharedLibName(lname.intern()).setStaticSharedLibVersion(PackageInfo.composeLongVersionCode(versionMajor, version)).setStaticSharedLibrary(true));
            }
            return input.error("Bad static-library declaration name: " + lname + " version: " + version);
        } finally {
            sa.recycle();
        }
    }

    private static ParseResult<ParsingPackage> parseLibrary(ParsingPackage pkg, Resources res, XmlResourceParser parser, ParseInput input) {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestLibrary);
        try {
            String lname = sa.getNonResourceString(0);
            if (lname != null) {
                String lname2 = lname.intern();
                if (!ArrayUtils.contains(pkg.getLibraryNames(), lname2)) {
                    pkg.addLibraryName(lname2);
                }
            }
            return input.success(pkg);
        } finally {
            sa.recycle();
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2543=5] */
    private static ParseResult<ParsingPackage> parseUsesSdkLibrary(ParseInput input, ParsingPackage pkg, Resources res, XmlResourceParser parser) throws XmlPullParserException, IOException {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestUsesSdkLibrary);
        try {
            String lname = sa.getNonResourceString(0);
            int versionMajor = sa.getInt(2, -1);
            String certSha256Digest = sa.getNonResourceString(1);
            if (lname != null && versionMajor >= 0 && certSha256Digest != null) {
                List<String> usesSdkLibraries = pkg.getUsesSdkLibraries();
                if (usesSdkLibraries.contains(lname)) {
                    return input.error("Depending on multiple versions of SDK library " + lname);
                }
                String lname2 = lname.intern();
                String certSha256Digest2 = certSha256Digest.replace(":", "").toLowerCase();
                if ("".equals(certSha256Digest2)) {
                    certSha256Digest2 = SystemProperties.get("debug.pm.uses_sdk_library_default_cert_digest", "");
                    try {
                        HexEncoding.decode(certSha256Digest2, false);
                    } catch (IllegalArgumentException e) {
                        certSha256Digest2 = "";
                    }
                }
                ParseResult<String[]> certResult = parseAdditionalCertificates(input, res, parser);
                if (certResult.isError()) {
                    return input.error(certResult);
                }
                String[] additionalCertSha256Digests = (String[]) certResult.getResult();
                String[] certSha256Digests = new String[additionalCertSha256Digests.length + 1];
                certSha256Digests[0] = certSha256Digest2;
                System.arraycopy(additionalCertSha256Digests, 0, certSha256Digests, 1, additionalCertSha256Digests.length);
                return input.success(pkg.addUsesSdkLibrary(lname2, versionMajor, certSha256Digests));
            }
            return input.error("Bad uses-sdk-library declaration name: " + lname + " version: " + versionMajor + " certDigest" + certSha256Digest);
        } finally {
            sa.recycle();
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2597=5] */
    private static ParseResult<ParsingPackage> parseUsesStaticLibrary(ParseInput input, ParsingPackage pkg, Resources res, XmlResourceParser parser) throws XmlPullParserException, IOException {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestUsesStaticLibrary);
        try {
            String lname = sa.getNonResourceString(0);
            int version = sa.getInt(1, -1);
            String certSha256Digest = sa.getNonResourceString(2);
            if (lname != null && version >= 0 && certSha256Digest != null) {
                List<String> usesStaticLibraries = pkg.getUsesStaticLibraries();
                if (usesStaticLibraries.contains(lname)) {
                    return input.error("Depending on multiple versions of static library " + lname);
                }
                String lname2 = lname.intern();
                String certSha256Digest2 = certSha256Digest.replace(":", "").toLowerCase();
                String[] additionalCertSha256Digests = EmptyArray.STRING;
                if (pkg.getTargetSdkVersion() >= 27) {
                    ParseResult<String[]> certResult = parseAdditionalCertificates(input, res, parser);
                    if (certResult.isError()) {
                        return input.error(certResult);
                    }
                    additionalCertSha256Digests = (String[]) certResult.getResult();
                }
                String[] certSha256Digests = new String[additionalCertSha256Digests.length + 1];
                certSha256Digests[0] = certSha256Digest2;
                System.arraycopy(additionalCertSha256Digests, 0, certSha256Digests, 1, additionalCertSha256Digests.length);
                return input.success(pkg.addUsesStaticLibrary(lname2, version, certSha256Digests));
            }
            return input.error("Bad uses-static-library declaration name: " + lname + " version: " + version + " certDigest" + certSha256Digest);
        } finally {
            sa.recycle();
        }
    }

    private static ParseResult<ParsingPackage> parseUsesLibrary(ParseInput input, ParsingPackage pkg, Resources res, XmlResourceParser parser) {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestUsesLibrary);
        try {
            String lname = sa.getNonResourceString(0);
            boolean req = sa.getBoolean(1, true);
            if (lname != null) {
                String lname2 = lname.intern();
                if (req) {
                    pkg.addUsesLibrary(lname2).removeUsesOptionalLibrary(lname2);
                } else if (!ArrayUtils.contains(pkg.getUsesLibraries(), lname2)) {
                    pkg.addUsesOptionalLibrary(lname2);
                }
            }
            return input.success(pkg);
        } finally {
            sa.recycle();
        }
    }

    private static ParseResult<ParsingPackage> parseUsesNativeLibrary(ParseInput input, ParsingPackage pkg, Resources res, XmlResourceParser parser) {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestUsesNativeLibrary);
        try {
            String lname = sa.getNonResourceString(0);
            boolean req = sa.getBoolean(1, true);
            if (lname != null) {
                if (req) {
                    pkg.addUsesNativeLibrary(lname).removeUsesOptionalNativeLibrary(lname);
                } else if (!ArrayUtils.contains(pkg.getUsesNativeLibraries(), lname)) {
                    pkg.addUsesOptionalNativeLibrary(lname);
                }
            }
            return input.success(pkg);
        } finally {
            sa.recycle();
        }
    }

    private static ParseResult<ParsingPackage> parseProcesses(ParseInput input, ParsingPackage pkg, Resources res, XmlResourceParser parser, String[] separateProcesses, int flags) throws IOException, XmlPullParserException {
        ParseResult<ArrayMap<String, ParsedProcess>> result = ParsedProcessUtils.parseProcesses(separateProcesses, pkg, res, parser, flags, input);
        if (result.isError()) {
            return input.error(result);
        }
        return input.success(pkg.setProcesses((Map) result.getResult()));
    }

    private static ParseResult<ParsingPackage> parseProfileable(ParseInput input, ParsingPackage pkg, Resources res, XmlResourceParser parser) {
        boolean z;
        ParsingPackage newPkg;
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestProfileable);
        try {
            boolean z2 = false;
            if (!pkg.isProfileableByShell() && !bool(false, 1, sa)) {
                z = false;
                newPkg = pkg.setProfileableByShell(z);
                if (newPkg.isProfileable() && bool(true, 0, sa)) {
                    z2 = true;
                }
                return input.success(newPkg.setProfileable(z2));
            }
            z = true;
            newPkg = pkg.setProfileableByShell(z);
            if (newPkg.isProfileable()) {
                z2 = true;
            }
            return input.success(newPkg.setProfileable(z2));
        } finally {
            sa.recycle();
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:27:0x0077, code lost:
        return r8.success(r0);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private static ParseResult<String[]> parseAdditionalCertificates(ParseInput input, Resources resources, XmlResourceParser parser) throws XmlPullParserException, IOException {
        String[] certSha256Digests = EmptyArray.STRING;
        int depth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= depth)) {
                break;
            } else if (type == 2) {
                String nodeName = parser.getName();
                if (nodeName.equals("additional-certificate")) {
                    TypedArray sa = resources.obtainAttributes(parser, R.styleable.AndroidManifestAdditionalCertificate);
                    try {
                        String certSha256Digest = sa.getNonResourceString(0);
                        if (TextUtils.isEmpty(certSha256Digest)) {
                            return input.error("Bad additional-certificate declaration with empty certDigest:" + certSha256Digest);
                        }
                        certSha256Digests = (String[]) ArrayUtils.appendElement(String.class, certSha256Digests, certSha256Digest.replace(":", "").toLowerCase());
                    } finally {
                        sa.recycle();
                    }
                } else {
                    continue;
                }
            }
        }
    }

    private static ParseResult<ParsedActivity> generateAppDetailsHiddenActivity(ParseInput input, ParsingPackage pkg) {
        String packageName = pkg.getPackageName();
        ParseResult<String> result = ComponentParseUtils.buildTaskAffinityName(packageName, packageName, ":app_details", input);
        if (result.isError()) {
            return input.error(result);
        }
        String taskAffinity = (String) result.getResult();
        return input.success(ParsedActivity.makeAppDetailsActivity(packageName, pkg.getProcessName(), pkg.getUiOptions(), taskAffinity, pkg.isBaseHardwareAccelerated()));
    }

    private static boolean hasDomainURLs(ParsingPackage pkg) {
        List<ParsedActivity> activities = pkg.getActivities();
        int activitiesSize = activities.size();
        for (int index = 0; index < activitiesSize; index++) {
            ParsedActivity activity = activities.get(index);
            List<ParsedIntentInfo> filters = activity.getIntents();
            int filtersSize = filters.size();
            for (int filtersIndex = 0; filtersIndex < filtersSize; filtersIndex++) {
                IntentFilter aii = filters.get(filtersIndex).getIntentFilter();
                if (aii.hasAction("android.intent.action.VIEW") && aii.hasAction("android.intent.action.VIEW") && (aii.hasDataScheme("http") || aii.hasDataScheme("https"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void setMaxAspectRatio(ParsingPackage pkg) {
        float maxAspectRatio = pkg.getTargetSdkVersion() < 26 ? 1.86f : 0.0f;
        float packageMaxAspectRatio = pkg.getMaxAspectRatio();
        if (packageMaxAspectRatio != 0.0f) {
            maxAspectRatio = packageMaxAspectRatio;
        } else {
            Bundle appMetaData = pkg.getMetaData();
            if (appMetaData != null && appMetaData.containsKey(METADATA_MAX_ASPECT_RATIO)) {
                maxAspectRatio = appMetaData.getFloat(METADATA_MAX_ASPECT_RATIO, maxAspectRatio);
            }
        }
        List<ParsedActivity> activities = pkg.getActivities();
        int activitiesSize = activities.size();
        for (int index = 0; index < activitiesSize; index++) {
            ParsedActivity activity = activities.get(index);
            if (activity.getMaxAspectRatio() == -1.0f) {
                float activityAspectRatio = activity.getMetaData().getFloat(METADATA_MAX_ASPECT_RATIO, maxAspectRatio);
                ComponentMutateUtils.setMaxAspectRatio(activity, activity.getResizeMode(), activityAspectRatio);
            }
        }
    }

    private void setMinAspectRatio(ParsingPackage pkg) {
        float minAspectRatio = pkg.getMinAspectRatio();
        List<ParsedActivity> activities = pkg.getActivities();
        int activitiesSize = activities.size();
        for (int index = 0; index < activitiesSize; index++) {
            ParsedActivity activity = activities.get(index);
            if (activity.getMinAspectRatio() == -1.0f) {
                ComponentMutateUtils.setMinAspectRatio(activity, activity.getResizeMode(), minAspectRatio);
            }
        }
    }

    private void setSupportsSizeChanges(ParsingPackage pkg) {
        Bundle appMetaData = pkg.getMetaData();
        boolean supportsSizeChanges = appMetaData != null && appMetaData.getBoolean(METADATA_SUPPORTS_SIZE_CHANGES, false);
        List<ParsedActivity> activities = pkg.getActivities();
        int activitiesSize = activities.size();
        for (int index = 0; index < activitiesSize; index++) {
            ParsedActivity activity = activities.get(index);
            if (supportsSizeChanges || activity.getMetaData().getBoolean(METADATA_SUPPORTS_SIZE_CHANGES, false)) {
                ComponentMutateUtils.setSupportsSizeChanges(activity, true);
            }
        }
    }

    private void addConfigChangesAssetsPaths(ParsingPackage pkg) {
        Bundle appMetaData = pkg.getMetaData();
        boolean supportsConfigChangesAssetsPaths = appMetaData != null && appMetaData.getBoolean(METADATA_SUPPORTS_CONFIG_CHANGES_ASSETS_PATHS, false);
        List<ParsedActivity> activities = pkg.getActivities();
        int activitiesSize = activities.size();
        for (int index = 0; index < activitiesSize; index++) {
            ParsedActivity activity = activities.get(index);
            if (supportsConfigChangesAssetsPaths || activity.getMetaData().getBoolean(METADATA_SUPPORTS_CONFIG_CHANGES_ASSETS_PATHS, false)) {
                ComponentMutateUtils.addConfigChanges(activity, Integer.MIN_VALUE);
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2912=5] */
    private static ParseResult<ParsingPackage> parseOverlay(ParseInput input, ParsingPackage pkg, Resources res, XmlResourceParser parser) {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestResourceOverlay);
        try {
            String target = sa.getString(1);
            int priority = anInt(0, 0, sa);
            if (target == null) {
                return input.error("<overlay> does not specify a target package");
            }
            if (priority < 0 || priority > 9999) {
                return input.error("<overlay> priority must be between 0 and 9999");
            }
            String propName = sa.getString(5);
            String propValue = sa.getString(6);
            if (FrameworkParsingPackageUtils.checkRequiredSystemProperties(propName, propValue)) {
                return input.success(pkg.setOverlay(true).setOverlayTarget(target).setOverlayPriority(priority).setOverlayTargetOverlayableName(sa.getString(3)).setOverlayCategory(sa.getString(2)).setOverlayIsStatic(bool(false, 4, sa)));
            }
            String message = "Skipping target and overlay pair " + target + " and " + pkg.getBaseApkPath() + ": overlay ignored due to required system property: " + propName + " with value: " + propValue;
            Slog.i("PackageParsing", message);
            return input.skip(message);
        } finally {
            sa.recycle();
        }
    }

    private static ParseResult<ParsingPackage> parseProtectedBroadcast(ParseInput input, ParsingPackage pkg, Resources res, XmlResourceParser parser) {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestProtectedBroadcast);
        try {
            String name = nonResString(0, sa);
            if (name != null) {
                pkg.addProtectedBroadcast(name);
            }
            return input.success(pkg);
        } finally {
            sa.recycle();
        }
    }

    private static ParseResult<ParsingPackage> parseSupportScreens(ParseInput input, ParsingPackage pkg, Resources res, XmlResourceParser parser) {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestSupportsScreens);
        try {
            int requiresSmallestWidthDp = anInt(0, 6, sa);
            int compatibleWidthLimitDp = anInt(0, 7, sa);
            int largestWidthLimitDp = anInt(0, 8, sa);
            return input.success(pkg.setSupportsSmallScreens(anInt(1, 1, sa)).setSupportsNormalScreens(anInt(1, 2, sa)).setSupportsLargeScreens(anInt(1, 3, sa)).setSupportsExtraLargeScreens(anInt(1, 5, sa)).setResizeable(anInt(1, 4, sa)).setAnyDensity(anInt(1, 0, sa)).setRequiresSmallestWidthDp(requiresSmallestWidthDp).setCompatibleWidthLimitDp(compatibleWidthLimitDp).setLargestWidthLimitDp(largestWidthLimitDp));
        } finally {
            sa.recycle();
        }
    }

    private static ParseResult<ParsingPackage> parseInstrumentation(ParseInput input, ParsingPackage pkg, Resources res, XmlResourceParser parser) throws XmlPullParserException, IOException {
        ParseResult<ParsedInstrumentation> result = ParsedInstrumentationUtils.parseInstrumentation(pkg, res, parser, sUseRoundIcon, input);
        if (result.isError()) {
            return input.error(result);
        }
        return input.success(pkg.addInstrumentation((ParsedInstrumentation) result.getResult()));
    }

    private static ParseResult<ParsingPackage> parseOriginalPackage(ParseInput input, ParsingPackage pkg, Resources res, XmlResourceParser parser) {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestOriginalPackage);
        try {
            String orig = sa.getNonConfigurationString(0, 0);
            if (!pkg.getPackageName().equals(orig)) {
                pkg.addOriginalPackage(orig);
            }
            return input.success(pkg);
        } finally {
            sa.recycle();
        }
    }

    private static ParseResult<ParsingPackage> parseAdoptPermissions(ParseInput input, ParsingPackage pkg, Resources res, XmlResourceParser parser) {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestOriginalPackage);
        try {
            String name = nonConfigString(0, 0, sa);
            if (name != null) {
                pkg.addAdoptPermission(name);
            }
            return input.success(pkg);
        } finally {
            sa.recycle();
        }
    }

    private static void convertCompatPermissions(ParsingPackage pkg) {
        int size = CompatibilityPermissionInfo.COMPAT_PERMS.length;
        for (int i = 0; i < size; i++) {
            CompatibilityPermissionInfo info = CompatibilityPermissionInfo.COMPAT_PERMS[i];
            if (pkg.getTargetSdkVersion() < info.getSdkVersion()) {
                if (!pkg.getRequestedPermissions().contains(info.getName())) {
                    pkg.addImplicitPermission(info.getName());
                }
            } else {
                return;
            }
        }
    }

    private void convertSplitPermissions(ParsingPackage pkg) {
        int listSize = this.mSplitPermissionInfos.size();
        for (int is = 0; is < listSize; is++) {
            PermissionManager.SplitPermissionInfo spi = this.mSplitPermissionInfos.get(is);
            List<String> requestedPermissions = pkg.getRequestedPermissions();
            if (pkg.getTargetSdkVersion() < spi.getTargetSdk() && requestedPermissions.contains(spi.getSplitPermission())) {
                List<String> newPerms = spi.getNewPermissions();
                for (int in = 0; in < newPerms.size(); in++) {
                    String perm = newPerms.get(in);
                    if (!requestedPermissions.contains(perm)) {
                        pkg.addImplicitPermission(perm);
                    }
                }
            }
        }
    }

    private static void adjustPackageToBeUnresizeableAndUnpipable(ParsingPackage pkg) {
        List<ParsedActivity> activities = pkg.getActivities();
        int activitiesSize = activities.size();
        for (int index = 0; index < activitiesSize; index++) {
            ParsedActivity activity = activities.get(index);
            ComponentMutateUtils.setResizeMode(activity, 0);
            ComponentMutateUtils.setExactFlags(activity, activity.getFlags() & (-4194305));
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3110=4] */
    public static ParseResult<PackageManager.Property> parseMetaData(ParsingPackage pkg, ParsedComponent component, Resources res, XmlResourceParser parser, String tagName, ParseInput input) {
        PackageManager.Property property;
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestMetaData);
        try {
            String name = TextUtils.safeIntern(nonConfigString(0, 0, sa));
            if (name == null) {
                return input.error(tagName + " requires an android:name attribute");
            }
            String packageName = pkg.getPackageName();
            String className = component != null ? component.getName() : null;
            TypedValue v = sa.peekValue(2);
            if (v == null || v.resourceId == 0) {
                TypedValue v2 = sa.peekValue(1);
                if (v2 == null) {
                    return input.error(tagName + " requires an android:value or android:resource attribute");
                }
                if (v2.type == 3) {
                    CharSequence cs = v2.coerceToString();
                    String stringValue = cs != null ? cs.toString() : null;
                    property = new PackageManager.Property(name, stringValue, packageName, className);
                } else if (v2.type == 18) {
                    property = new PackageManager.Property(name, v2.data != 0, packageName, className);
                } else if (v2.type >= 16 && v2.type <= 31) {
                    property = new PackageManager.Property(name, v2.data, false, packageName, className);
                } else if (v2.type == 4) {
                    property = new PackageManager.Property(name, v2.getFloat(), packageName, className);
                } else {
                    Slog.w("PackageParsing", tagName + " only supports string, integer, float, color, boolean, and resource reference types: " + parser.getName() + " at " + pkg.getBaseApkPath() + " " + parser.getPositionDescription());
                    property = null;
                }
            } else {
                property = new PackageManager.Property(name, v.resourceId, true, packageName, className);
            }
            return input.success(property);
        } finally {
            sa.recycle();
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3164=4] */
    public static ParseResult<SigningDetails> getSigningDetails(ParseInput input, ParsingPackageRead pkg, boolean skipVerify) {
        SigningDetails signingDetails = SigningDetails.UNKNOWN;
        Trace.traceBegin(262144L, "collectCertificates");
        try {
            ParseResult<SigningDetails> result = getSigningDetails(input, pkg.getBaseApkPath(), skipVerify, pkg.isStaticSharedLibrary(), signingDetails, pkg.getTargetSdkVersion());
            if (result.isError()) {
                ParseResult<SigningDetails> error = input.error(result);
                Trace.traceEnd(262144L);
                return error;
            }
            SigningDetails signingDetails2 = (SigningDetails) result.getResult();
            try {
                File frameworkRes = new File(Environment.getRootDirectory(), "framework/framework-res.apk");
                boolean isFrameworkResSplit = frameworkRes.getAbsolutePath().equals(pkg.getBaseApkPath());
                String[] splitCodePaths = pkg.getSplitCodePaths();
                if (!ArrayUtils.isEmpty(splitCodePaths) && !isFrameworkResSplit) {
                    for (String str : splitCodePaths) {
                        result = getSigningDetails(input, str, skipVerify, pkg.isStaticSharedLibrary(), signingDetails2, pkg.getTargetSdkVersion());
                        if (result.isError()) {
                            ParseResult<SigningDetails> error2 = input.error(result);
                            Trace.traceEnd(262144L);
                            return error2;
                        }
                    }
                }
                Trace.traceEnd(262144L);
                return result;
            } catch (Throwable th) {
                th = th;
                Trace.traceEnd(262144L);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public static ParseResult<SigningDetails> getSigningDetails(ParseInput input, String baseCodePath, boolean skipVerify, boolean isStaticSharedLibrary, SigningDetails existingSigningDetails, int targetSdk) {
        ParseResult<SigningDetails> verified;
        int minSignatureScheme = ApkSignatureVerifier.getMinimumSignatureSchemeVersionForTargetSdk(targetSdk);
        if (isStaticSharedLibrary) {
            minSignatureScheme = 2;
        }
        if (skipVerify) {
            verified = ApkSignatureVerifier.unsafeGetCertsWithoutVerification(input, baseCodePath, minSignatureScheme);
        } else {
            verified = ApkSignatureVerifier.verify(input, baseCodePath, minSignatureScheme);
        }
        if (verified.isError()) {
            return input.error(verified);
        }
        if (existingSigningDetails == SigningDetails.UNKNOWN) {
            return verified;
        }
        if (!Signature.areExactMatch(existingSigningDetails.getSignatures(), ((SigningDetails) verified.getResult()).getSignatures())) {
            return input.error(-104, baseCodePath + " has mismatched certificates");
        }
        return input.success(existingSigningDetails);
    }

    public static void setCompatibilityModeEnabled(boolean compatibilityModeEnabled) {
        sCompatibilityModeEnabled = compatibilityModeEnabled;
    }

    public static void readConfigUseRoundIcon(Resources r) {
        if (r != null) {
            sUseRoundIcon = r.getBoolean(17891814);
            return;
        }
        try {
            ApplicationInfo androidAppInfo = ActivityThread.getPackageManager().getApplicationInfo(PackageManagerService.PLATFORM_PACKAGE_NAME, 0L, UserHandle.myUserId());
            Resources systemResources = Resources.getSystem();
            Resources overlayableRes = ResourcesManager.getInstance().getResources((IBinder) null, (String) null, (String[]) null, androidAppInfo.resourceDirs, androidAppInfo.overlayPaths, androidAppInfo.sharedLibraryFiles, (Integer) null, (Configuration) null, systemResources.getCompatibilityInfo(), systemResources.getClassLoader(), (List) null);
            sUseRoundIcon = overlayableRes.getBoolean(17891814);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private static boolean bool(boolean defaultValue, int attribute, TypedArray sa) {
        return sa.getBoolean(attribute, defaultValue);
    }

    private static float aFloat(float defaultValue, int attribute, TypedArray sa) {
        return sa.getFloat(attribute, defaultValue);
    }

    private static float aFloat(int attribute, TypedArray sa) {
        return sa.getFloat(attribute, 0.0f);
    }

    private static int anInt(int defaultValue, int attribute, TypedArray sa) {
        return sa.getInt(attribute, defaultValue);
    }

    private static int anInteger(int defaultValue, int attribute, TypedArray sa) {
        return sa.getInteger(attribute, defaultValue);
    }

    private static int anInt(int attribute, TypedArray sa) {
        return sa.getInt(attribute, 0);
    }

    private static int resId(int attribute, TypedArray sa) {
        return sa.getResourceId(attribute, 0);
    }

    private static String string(int attribute, TypedArray sa) {
        return sa.getString(attribute);
    }

    private static String nonConfigString(int allowedChangingConfigs, int attribute, TypedArray sa) {
        return sa.getNonConfigurationString(attribute, allowedChangingConfigs);
    }

    private static String nonResString(int index, TypedArray sa) {
        return sa.getNonResourceString(index);
    }

    public static void writeKeySetMapping(Parcel dest, Map<String, ArraySet<PublicKey>> keySetMapping) {
        if (keySetMapping == null) {
            dest.writeInt(-1);
            return;
        }
        int N = keySetMapping.size();
        dest.writeInt(N);
        for (String key : keySetMapping.keySet()) {
            dest.writeString(key);
            ArraySet<PublicKey> keys = keySetMapping.get(key);
            if (keys == null) {
                dest.writeInt(-1);
            } else {
                int M = keys.size();
                dest.writeInt(M);
                for (int j = 0; j < M; j++) {
                    dest.writeSerializable(keys.valueAt(j));
                }
            }
        }
    }

    public static ArrayMap<String, ArraySet<PublicKey>> readKeySetMapping(Parcel in) {
        int N = in.readInt();
        if (N == -1) {
            return null;
        }
        ArrayMap<String, ArraySet<PublicKey>> keySetMapping = new ArrayMap<>();
        for (int i = 0; i < N; i++) {
            String key = in.readString();
            int M = in.readInt();
            if (M == -1) {
                keySetMapping.put(key, null);
            } else {
                ArraySet<PublicKey> keys = new ArraySet<>(M);
                for (int j = 0; j < M; j++) {
                    PublicKey pk = (PublicKey) in.readSerializable();
                    keys.add(pk);
                }
                keySetMapping.put(key, keys);
            }
        }
        return keySetMapping;
    }
}
