package com.android.server.pm.parsing;

import android.app.ActivityThread;
import android.content.pm.ApplicationInfo;
import android.content.pm.parsing.result.ParseInput;
import android.content.pm.parsing.result.ParseResult;
import android.content.pm.parsing.result.ParseTypeImpl;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.permission.PermissionManager;
import android.util.DisplayMetrics;
import android.util.Slog;
import com.android.internal.compat.IPlatformCompat;
import com.android.server.pm.PackageManagerException;
import com.android.server.pm.parsing.pkg.PackageImpl;
import com.android.server.pm.parsing.pkg.ParsedPackage;
import com.android.server.pm.pkg.parsing.ParsingPackage;
import com.android.server.pm.pkg.parsing.ParsingPackageUtils;
import java.io.File;
import java.util.List;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public class PackageParser2 implements AutoCloseable {
    private static final boolean LOG_PARSE_TIMINGS = Build.IS_DEBUGGABLE;
    private static final int LOG_PARSE_TIMINGS_THRESHOLD_MS = 100;
    private static final String TAG = "PackageParsing";
    protected PackageCacher mCacher;
    private ThreadLocal<ApplicationInfo> mSharedAppInfo = ThreadLocal.withInitial(new Supplier() { // from class: com.android.server.pm.parsing.PackageParser2$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return PackageParser2.lambda$new$0();
        }
    });
    private ThreadLocal<ParseTypeImpl> mSharedResult;
    private ParsingPackageUtils parsingUtils;

    public static PackageParser2 forParsingFileWithDefaults() {
        final IPlatformCompat platformCompat = IPlatformCompat.Stub.asInterface(ServiceManager.getService("platform_compat"));
        return new PackageParser2(null, false, null, null, new Callback() { // from class: com.android.server.pm.parsing.PackageParser2.1
            @Override // com.android.server.pm.parsing.PackageParser2.Callback
            public boolean isChangeEnabled(long changeId, ApplicationInfo appInfo) {
                try {
                    return platformCompat.isChangeEnabled(changeId, appInfo);
                } catch (Exception e) {
                    Slog.wtf("PackageParsing", "IPlatformCompat query failed", e);
                    return true;
                }
            }

            @Override // com.android.server.pm.pkg.parsing.ParsingPackageUtils.Callback
            public boolean hasFeature(String feature) {
                return false;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ ApplicationInfo lambda$new$0() {
        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.uid = -1;
        return appInfo;
    }

    public PackageParser2(String[] separateProcesses, boolean onlyCoreApps, DisplayMetrics displayMetrics, File cacheDir, final Callback callback) {
        if (displayMetrics == null) {
            displayMetrics = new DisplayMetrics();
            displayMetrics.setToDefaults();
        }
        PermissionManager permissionManager = (PermissionManager) ActivityThread.currentApplication().getSystemService(PermissionManager.class);
        List<PermissionManager.SplitPermissionInfo> splitPermissions = permissionManager.getSplitPermissions();
        this.mCacher = cacheDir == null ? null : new PackageCacher(cacheDir);
        this.parsingUtils = new ParsingPackageUtils(onlyCoreApps, separateProcesses, displayMetrics, splitPermissions, callback);
        final ParseInput.Callback enforcementCallback = new ParseInput.Callback() { // from class: com.android.server.pm.parsing.PackageParser2$$ExternalSyntheticLambda1
            public final boolean isChangeEnabled(long j, String str, int i) {
                return PackageParser2.this.m5781lambda$new$1$comandroidserverpmparsingPackageParser2(callback, j, str, i);
            }
        };
        this.mSharedResult = ThreadLocal.withInitial(new Supplier() { // from class: com.android.server.pm.parsing.PackageParser2$$ExternalSyntheticLambda2
            @Override // java.util.function.Supplier
            public final Object get() {
                return PackageParser2.lambda$new$2(enforcementCallback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$1$com-android-server-pm-parsing-PackageParser2  reason: not valid java name */
    public /* synthetic */ boolean m5781lambda$new$1$comandroidserverpmparsingPackageParser2(Callback callback, long changeId, String packageName, int targetSdkVersion) {
        ApplicationInfo appInfo = this.mSharedAppInfo.get();
        appInfo.packageName = packageName;
        appInfo.targetSdkVersion = targetSdkVersion;
        return callback.isChangeEnabled(changeId, appInfo);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ ParseTypeImpl lambda$new$2(ParseInput.Callback enforcementCallback) {
        return new ParseTypeImpl(enforcementCallback);
    }

    public ParsedPackage parsePackage(File packageFile, int flags, boolean useCaches) throws PackageManagerException {
        return parsePackage(packageFile, flags, useCaches, null);
    }

    public ParsedPackage parsePackage(File packageFile, int flags, boolean useCaches, List<File> frameworkSplits) throws PackageManagerException {
        PackageCacher packageCacher;
        ParsedPackage parsed;
        if (useCaches && (packageCacher = this.mCacher) != null && (parsed = packageCacher.getCachedResult(packageFile, flags)) != null) {
            return parsed;
        }
        boolean z = LOG_PARSE_TIMINGS;
        long parseTime = z ? SystemClock.uptimeMillis() : 0L;
        ParseInput input = this.mSharedResult.get().reset();
        ParseResult<ParsingPackage> result = this.parsingUtils.parsePackage(input, packageFile, flags, frameworkSplits);
        if (result.isError()) {
            throw new PackageManagerException(result.getErrorCode(), result.getErrorMessage(), result.getException());
        }
        ParsedPackage parsed2 = (ParsedPackage) ((ParsingPackage) result.getResult()).hideAsParsed();
        long cacheTime = z ? SystemClock.uptimeMillis() : 0L;
        PackageCacher packageCacher2 = this.mCacher;
        if (packageCacher2 != null) {
            packageCacher2.cacheResult(packageFile, flags, parsed2);
        }
        if (z) {
            long parseTime2 = cacheTime - parseTime;
            long cacheTime2 = SystemClock.uptimeMillis() - cacheTime;
            if (parseTime2 + cacheTime2 > 100) {
                Slog.i("PackageParsing", "Parse times for '" + packageFile + "': parse=" + parseTime2 + "ms, update_cache=" + cacheTime2 + " ms");
            }
        }
        return parsed2;
    }

    @Override // java.lang.AutoCloseable
    public void close() {
        this.mSharedResult.remove();
        this.mSharedAppInfo.remove();
    }

    /* loaded from: classes2.dex */
    public static abstract class Callback implements ParsingPackageUtils.Callback {
        public abstract boolean isChangeEnabled(long j, ApplicationInfo applicationInfo);

        @Override // com.android.server.pm.pkg.parsing.ParsingPackageUtils.Callback
        public final ParsingPackage startParsingPackage(String packageName, String baseCodePath, String codePath, TypedArray manifestArray, boolean isCoreApp) {
            return PackageImpl.forParsing(packageName, baseCodePath, codePath, manifestArray, isCoreApp);
        }
    }
}
