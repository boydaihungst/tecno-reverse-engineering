package com.android.server.webkit;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.AsyncTask;
import android.os.Trace;
import android.util.Slog;
import android.webkit.UserPackage;
import android.webkit.WebViewFactory;
import android.webkit.WebViewProviderInfo;
import android.webkit.WebViewProviderResponse;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class WebViewUpdateServiceImpl {
    private static final int MULTIPROCESS_SETTING_OFF_VALUE = Integer.MIN_VALUE;
    private static final int MULTIPROCESS_SETTING_ON_VALUE = Integer.MAX_VALUE;
    private static final long NS_PER_MS = 1000000;
    private static final int NUMBER_OF_RELROS_UNKNOWN = Integer.MAX_VALUE;
    private static final String TAG = WebViewUpdateServiceImpl.class.getSimpleName();
    private static final int VALIDITY_INCORRECT_SDK_VERSION = 1;
    private static final int VALIDITY_INCORRECT_SIGNATURE = 3;
    private static final int VALIDITY_INCORRECT_VERSION_CODE = 2;
    private static final int VALIDITY_NO_LIBRARY_FLAG = 4;
    private static final int VALIDITY_OK = 0;
    private static final int WAIT_TIMEOUT_MS = 1000;
    private final Context mContext;
    private final SystemInterface mSystemInterface;
    private long mMinimumVersionCode = -1;
    private int mNumRelroCreationsStarted = 0;
    private int mNumRelroCreationsFinished = 0;
    private boolean mWebViewPackageDirty = false;
    private boolean mAnyWebViewInstalled = false;
    private PackageInfo mCurrentWebViewPackage = null;
    private final Object mLock = new Object();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class WebViewPackageMissingException extends Exception {
        WebViewPackageMissingException(String message) {
            super(message);
        }

        WebViewPackageMissingException(Exception e) {
            super(e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WebViewUpdateServiceImpl(Context context, SystemInterface systemInterface) {
        this.mContext = context;
        this.mSystemInterface = systemInterface;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void packageStateChanged(String packageName, int changedState, int userId) {
        WebViewProviderInfo[] webViewPackages;
        boolean z = false;
        for (WebViewProviderInfo provider : this.mSystemInterface.getWebViewPackages()) {
            String webviewPackage = provider.packageName;
            if (webviewPackage.equals(packageName)) {
                boolean updateWebView = false;
                boolean removedOrChangedOldPackage = false;
                String oldProviderName = null;
                synchronized (this.mLock) {
                    try {
                        PackageInfo newPackage = findPreferredWebViewPackage();
                        PackageInfo packageInfo = this.mCurrentWebViewPackage;
                        if (packageInfo != null) {
                            oldProviderName = packageInfo.packageName;
                        }
                        updateWebView = (provider.packageName.equals(newPackage.packageName) || provider.packageName.equals(oldProviderName) || this.mCurrentWebViewPackage == null) ? true : true;
                        removedOrChangedOldPackage = provider.packageName.equals(oldProviderName);
                        if (updateWebView) {
                            onWebViewProviderChanged(newPackage);
                        }
                    } catch (WebViewPackageMissingException e) {
                        this.mCurrentWebViewPackage = null;
                        Slog.e(TAG, "Could not find valid WebView package to create relro with " + e);
                    }
                }
                if (updateWebView && !removedOrChangedOldPackage && oldProviderName != null) {
                    this.mSystemInterface.killPackageDependents(oldProviderName);
                    return;
                }
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void prepareWebViewInSystemServer() {
        this.mSystemInterface.notifyZygote(isMultiProcessEnabled());
        try {
            synchronized (this.mLock) {
                this.mCurrentWebViewPackage = findPreferredWebViewPackage();
                String userSetting = this.mSystemInterface.getUserChosenWebViewProvider(this.mContext);
                if (userSetting != null && !userSetting.equals(this.mCurrentWebViewPackage.packageName)) {
                    this.mSystemInterface.updateUserSetting(this.mContext, this.mCurrentWebViewPackage.packageName);
                }
                onWebViewProviderChanged(this.mCurrentWebViewPackage);
            }
        } catch (Throwable t) {
            Slog.e(TAG, "error preparing webview provider from system server", t);
        }
        if (getCurrentWebViewPackage() == null) {
            WebViewProviderInfo[] webviewProviders = this.mSystemInterface.getWebViewPackages();
            WebViewProviderInfo fallbackProvider = getFallbackProvider(webviewProviders);
            if (fallbackProvider != null) {
                Slog.w(TAG, "No valid provider, trying to enable " + fallbackProvider.packageName);
                this.mSystemInterface.enablePackageForAllUsers(this.mContext, fallbackProvider.packageName, true);
                return;
            }
            Slog.e(TAG, "No valid provider and no fallback available.");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startZygoteWhenReady() {
        waitForAndGetProvider();
        this.mSystemInterface.ensureZygoteStarted();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleNewUser(int userId) {
        if (userId == 0) {
            return;
        }
        handleUserChange();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleUserRemoved(int userId) {
        handleUserChange();
    }

    private void handleUserChange() {
        updateCurrentWebViewPackage(null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyRelroCreationCompleted() {
        synchronized (this.mLock) {
            this.mNumRelroCreationsFinished++;
            checkIfRelrosDoneLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WebViewProviderResponse waitForAndGetProvider() {
        boolean webViewReady;
        PackageInfo webViewPackage;
        long timeoutTimeMs = (System.nanoTime() / NS_PER_MS) + 1000;
        int webViewStatus = 0;
        synchronized (this.mLock) {
            webViewReady = webViewIsReadyLocked();
            while (!webViewReady) {
                long timeNowMs = System.nanoTime() / NS_PER_MS;
                if (timeNowMs >= timeoutTimeMs) {
                    break;
                }
                try {
                    this.mLock.wait(timeoutTimeMs - timeNowMs);
                } catch (InterruptedException e) {
                }
                webViewReady = webViewIsReadyLocked();
            }
            webViewPackage = this.mCurrentWebViewPackage;
            if (!webViewReady) {
                if (!this.mAnyWebViewInstalled) {
                    webViewStatus = 4;
                } else {
                    webViewStatus = 3;
                    String timeoutError = "Timed out waiting for relro creation, relros started " + this.mNumRelroCreationsStarted + " relros finished " + this.mNumRelroCreationsFinished + " package dirty? " + this.mWebViewPackageDirty;
                    Slog.e(TAG, timeoutError);
                    Trace.instant(64L, timeoutError);
                }
            }
        }
        if (!webViewReady) {
            Slog.w(TAG, "creating relro file timed out");
        }
        return new WebViewProviderResponse(webViewPackage, webViewStatus);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String changeProviderAndSetting(String newProviderName) {
        PackageInfo newPackage = updateCurrentWebViewPackage(newProviderName);
        return newPackage == null ? "" : newPackage.packageName;
    }

    /* JADX WARN: Removed duplicated region for block: B:16:0x002b A[Catch: all -> 0x0059, TRY_ENTER, TryCatch #1 {, blocks: (B:4:0x0006, B:6:0x000b, B:7:0x0012, B:9:0x0019, B:16:0x002b, B:17:0x002e, B:24:0x003d, B:25:0x0057), top: B:32:0x0006, inners: #0 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private PackageInfo updateCurrentWebViewPackage(String newProviderName) {
        PackageInfo oldPackage;
        PackageInfo newPackage;
        boolean z;
        boolean providerChanged;
        synchronized (this.mLock) {
            oldPackage = this.mCurrentWebViewPackage;
            if (newProviderName != null) {
                this.mSystemInterface.updateUserSetting(this.mContext, newProviderName);
            }
            try {
                newPackage = findPreferredWebViewPackage();
                if (oldPackage != null) {
                    if (newPackage.packageName.equals(oldPackage.packageName)) {
                        z = false;
                        providerChanged = z;
                        if (providerChanged) {
                            onWebViewProviderChanged(newPackage);
                        }
                    }
                }
                z = true;
                providerChanged = z;
                if (providerChanged) {
                }
            } catch (WebViewPackageMissingException e) {
                this.mCurrentWebViewPackage = null;
                Slog.e(TAG, "Couldn't find WebView package to use " + e);
                return null;
            }
        }
        if (providerChanged && oldPackage != null) {
            this.mSystemInterface.killPackageDependents(oldPackage.packageName);
        }
        return newPackage;
    }

    private void onWebViewProviderChanged(PackageInfo newPackage) {
        synchronized (this.mLock) {
            this.mAnyWebViewInstalled = true;
            if (this.mNumRelroCreationsStarted == this.mNumRelroCreationsFinished) {
                this.mCurrentWebViewPackage = newPackage;
                this.mNumRelroCreationsStarted = Integer.MAX_VALUE;
                this.mNumRelroCreationsFinished = 0;
                this.mNumRelroCreationsStarted = this.mSystemInterface.onWebViewProviderChanged(newPackage);
                checkIfRelrosDoneLocked();
            } else {
                this.mWebViewPackageDirty = true;
            }
        }
        if (isMultiProcessEnabled()) {
            AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() { // from class: com.android.server.webkit.WebViewUpdateServiceImpl$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    WebViewUpdateServiceImpl.this.startZygoteWhenReady();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WebViewProviderInfo[] getValidWebViewPackages() {
        ProviderAndPackageInfo[] providersAndPackageInfos = getValidWebViewPackagesAndInfos();
        WebViewProviderInfo[] providers = new WebViewProviderInfo[providersAndPackageInfos.length];
        for (int n = 0; n < providersAndPackageInfos.length; n++) {
            providers[n] = providersAndPackageInfos[n].provider;
        }
        return providers;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class ProviderAndPackageInfo {
        public final PackageInfo packageInfo;
        public final WebViewProviderInfo provider;

        ProviderAndPackageInfo(WebViewProviderInfo provider, PackageInfo packageInfo) {
            this.provider = provider;
            this.packageInfo = packageInfo;
        }
    }

    private ProviderAndPackageInfo[] getValidWebViewPackagesAndInfos() {
        WebViewProviderInfo[] allProviders = this.mSystemInterface.getWebViewPackages();
        List<ProviderAndPackageInfo> providers = new ArrayList<>();
        for (int n = 0; n < allProviders.length; n++) {
            try {
                PackageInfo packageInfo = this.mSystemInterface.getPackageInfoForProvider(allProviders[n]);
                if (validityResult(allProviders[n], packageInfo) == 0) {
                    providers.add(new ProviderAndPackageInfo(allProviders[n], packageInfo));
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        int n2 = providers.size();
        return (ProviderAndPackageInfo[]) providers.toArray(new ProviderAndPackageInfo[n2]);
    }

    private PackageInfo findPreferredWebViewPackage() throws WebViewPackageMissingException {
        ProviderAndPackageInfo[] providers = getValidWebViewPackagesAndInfos();
        String userChosenProvider = this.mSystemInterface.getUserChosenWebViewProvider(this.mContext);
        for (ProviderAndPackageInfo providerAndPackage : providers) {
            if (providerAndPackage.provider.packageName.equals(userChosenProvider)) {
                List<UserPackage> userPackages = this.mSystemInterface.getPackageInfoForProviderAllUsers(this.mContext, providerAndPackage.provider);
                if (isInstalledAndEnabledForAllUsers(userPackages)) {
                    return providerAndPackage.packageInfo;
                }
            }
        }
        for (ProviderAndPackageInfo providerAndPackage2 : providers) {
            if (providerAndPackage2.provider.availableByDefault) {
                List<UserPackage> userPackages2 = this.mSystemInterface.getPackageInfoForProviderAllUsers(this.mContext, providerAndPackage2.provider);
                if (isInstalledAndEnabledForAllUsers(userPackages2)) {
                    return providerAndPackage2.packageInfo;
                }
            }
        }
        this.mAnyWebViewInstalled = false;
        throw new WebViewPackageMissingException("Could not find a loadable WebView package");
    }

    /* JADX WARN: Removed duplicated region for block: B:5:0x000a  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private static boolean isInstalledAndEnabledForAllUsers(List<UserPackage> userPackages) {
        for (UserPackage userPackage : userPackages) {
            if (!userPackage.isInstalledPackage() || !userPackage.isEnabledPackage()) {
                return false;
            }
            while (r0.hasNext()) {
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WebViewProviderInfo[] getWebViewPackages() {
        return this.mSystemInterface.getWebViewPackages();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageInfo getCurrentWebViewPackage() {
        PackageInfo packageInfo;
        synchronized (this.mLock) {
            packageInfo = this.mCurrentWebViewPackage;
        }
        return packageInfo;
    }

    private boolean webViewIsReadyLocked() {
        return !this.mWebViewPackageDirty && this.mNumRelroCreationsStarted == this.mNumRelroCreationsFinished && this.mAnyWebViewInstalled;
    }

    private void checkIfRelrosDoneLocked() {
        if (this.mNumRelroCreationsStarted == this.mNumRelroCreationsFinished) {
            if (this.mWebViewPackageDirty) {
                this.mWebViewPackageDirty = false;
                try {
                    PackageInfo newPackage = findPreferredWebViewPackage();
                    onWebViewProviderChanged(newPackage);
                    return;
                } catch (WebViewPackageMissingException e) {
                    this.mCurrentWebViewPackage = null;
                    return;
                }
            }
            this.mLock.notifyAll();
        }
    }

    private int validityResult(WebViewProviderInfo configInfo, PackageInfo packageInfo) {
        if (!UserPackage.hasCorrectTargetSdkVersion(packageInfo)) {
            return 1;
        }
        if (!versionCodeGE(packageInfo.getLongVersionCode(), getMinimumVersionCode()) && !this.mSystemInterface.systemIsDebuggable()) {
            return 2;
        }
        if (!providerHasValidSignature(configInfo, packageInfo, this.mSystemInterface)) {
            return 3;
        }
        if (WebViewFactory.getWebViewLibrary(packageInfo.applicationInfo) == null) {
            return 4;
        }
        return 0;
    }

    private static boolean versionCodeGE(long versionCode1, long versionCode2) {
        long v1 = versionCode1 / 100000;
        long v2 = versionCode2 / 100000;
        return v1 >= v2;
    }

    private long getMinimumVersionCode() {
        WebViewProviderInfo[] webViewPackages;
        long j = this.mMinimumVersionCode;
        if (j > 0) {
            return j;
        }
        long minimumVersionCode = -1;
        for (WebViewProviderInfo provider : this.mSystemInterface.getWebViewPackages()) {
            if (provider.availableByDefault) {
                try {
                    long versionCode = this.mSystemInterface.getFactoryPackageVersion(provider.packageName);
                    if (minimumVersionCode < 0 || versionCode < minimumVersionCode) {
                        minimumVersionCode = versionCode;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
        }
        this.mMinimumVersionCode = minimumVersionCode;
        return minimumVersionCode;
    }

    private static boolean providerHasValidSignature(WebViewProviderInfo provider, PackageInfo packageInfo, SystemInterface systemInterface) {
        Signature[] signatureArr;
        if (systemInterface.systemIsDebuggable() || packageInfo.applicationInfo.isSystemApp()) {
            return true;
        }
        if (packageInfo.signatures.length != 1) {
            return false;
        }
        for (Signature signature : provider.signatures) {
            if (signature.equals(packageInfo.signatures[0])) {
                return true;
            }
        }
        return false;
    }

    private static WebViewProviderInfo getFallbackProvider(WebViewProviderInfo[] webviewPackages) {
        for (WebViewProviderInfo provider : webviewPackages) {
            if (provider.isFallback) {
                return provider;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isMultiProcessEnabled() {
        int settingValue = this.mSystemInterface.getMultiProcessSetting(this.mContext);
        return this.mSystemInterface.isMultiProcessDefaultEnabled() ? settingValue > Integer.MIN_VALUE : settingValue >= Integer.MAX_VALUE;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void enableMultiProcess(boolean enable) {
        PackageInfo current = getCurrentWebViewPackage();
        this.mSystemInterface.setMultiProcessSetting(this.mContext, enable ? Integer.MAX_VALUE : Integer.MIN_VALUE);
        this.mSystemInterface.notifyZygote(enable);
        if (current != null) {
            this.mSystemInterface.killPackageDependents(current.packageName);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpState(PrintWriter pw) {
        pw.println("Current WebView Update Service state");
        pw.println(String.format("  Multiprocess enabled: %b", Boolean.valueOf(isMultiProcessEnabled())));
        synchronized (this.mLock) {
            PackageInfo packageInfo = this.mCurrentWebViewPackage;
            if (packageInfo == null) {
                pw.println("  Current WebView package is null");
            } else {
                pw.println(String.format("  Current WebView package (name, version): (%s, %s)", packageInfo.packageName, this.mCurrentWebViewPackage.versionName));
            }
            pw.println(String.format("  Minimum targetSdkVersion: %d", 33));
            pw.println(String.format("  Minimum WebView version code: %d", Long.valueOf(this.mMinimumVersionCode)));
            pw.println(String.format("  Number of relros started: %d", Integer.valueOf(this.mNumRelroCreationsStarted)));
            pw.println(String.format("  Number of relros finished: %d", Integer.valueOf(this.mNumRelroCreationsFinished)));
            pw.println(String.format("  WebView package dirty: %b", Boolean.valueOf(this.mWebViewPackageDirty)));
            pw.println(String.format("  Any WebView package installed: %b", Boolean.valueOf(this.mAnyWebViewInstalled)));
            try {
                PackageInfo preferredWebViewPackage = findPreferredWebViewPackage();
                pw.println(String.format("  Preferred WebView package (name, version): (%s, %s)", preferredWebViewPackage.packageName, preferredWebViewPackage.versionName));
            } catch (WebViewPackageMissingException e) {
                pw.println(String.format("  Preferred WebView package: none", new Object[0]));
            }
            dumpAllPackageInformationLocked(pw);
        }
    }

    private void dumpAllPackageInformationLocked(PrintWriter pw) {
        WebViewProviderInfo[] allProviders = this.mSystemInterface.getWebViewPackages();
        pw.println("  WebView packages:");
        for (WebViewProviderInfo provider : allProviders) {
            List<UserPackage> userPackages = this.mSystemInterface.getPackageInfoForProviderAllUsers(this.mContext, provider);
            PackageInfo systemUserPackageInfo = userPackages.get(0).getPackageInfo();
            if (systemUserPackageInfo == null) {
                pw.println(String.format("    %s is NOT installed.", provider.packageName));
            } else {
                int validity = validityResult(provider, systemUserPackageInfo);
                String packageDetails = String.format("versionName: %s, versionCode: %d, targetSdkVersion: %d", systemUserPackageInfo.versionName, Long.valueOf(systemUserPackageInfo.getLongVersionCode()), Integer.valueOf(systemUserPackageInfo.applicationInfo.targetSdkVersion));
                if (validity == 0) {
                    boolean installedForAllUsers = isInstalledAndEnabledForAllUsers(this.mSystemInterface.getPackageInfoForProviderAllUsers(this.mContext, provider));
                    Object[] objArr = new Object[3];
                    objArr[0] = systemUserPackageInfo.packageName;
                    objArr[1] = packageDetails;
                    objArr[2] = installedForAllUsers ? "" : "NOT";
                    pw.println(String.format("    Valid package %s (%s) is %s installed/enabled for all users", objArr));
                } else {
                    pw.println(String.format("    Invalid package %s (%s), reason: %s", systemUserPackageInfo.packageName, packageDetails, getInvalidityReason(validity)));
                }
            }
        }
    }

    private static String getInvalidityReason(int invalidityReason) {
        switch (invalidityReason) {
            case 1:
                return "SDK version too low";
            case 2:
                return "Version code too low";
            case 3:
                return "Incorrect signature";
            case 4:
                return "No WebView-library manifest flag";
            default:
                return "Unexcepted validity-reason";
        }
    }
}
