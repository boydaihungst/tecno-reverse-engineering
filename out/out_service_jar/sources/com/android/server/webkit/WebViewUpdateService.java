package com.android.server.webkit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManagerInternal;
import android.os.Binder;
import android.os.Process;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.UserHandle;
import android.util.Slog;
import android.webkit.IWebViewUpdateService;
import android.webkit.WebViewProviderInfo;
import android.webkit.WebViewProviderResponse;
import com.android.internal.util.DumpUtils;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
/* loaded from: classes2.dex */
public class WebViewUpdateService extends SystemService {
    static final int PACKAGE_ADDED = 1;
    static final int PACKAGE_ADDED_REPLACED = 2;
    static final int PACKAGE_CHANGED = 0;
    static final int PACKAGE_REMOVED = 3;
    private static final String TAG = "WebViewUpdateService";
    private WebViewUpdateServiceImpl mImpl;
    private BroadcastReceiver mWebViewUpdatedReceiver;

    public WebViewUpdateService(Context context) {
        super(context);
        this.mImpl = new WebViewUpdateServiceImpl(context, SystemImpl.getInstance());
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        WebViewProviderInfo[] webViewPackages;
        this.mWebViewUpdatedReceiver = new BroadcastReceiver() { // from class: com.android.server.webkit.WebViewUpdateService.1
            /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                char c;
                int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                String action = intent.getAction();
                switch (action.hashCode()) {
                    case -2061058799:
                        if (action.equals("android.intent.action.USER_REMOVED")) {
                            c = 4;
                            break;
                        }
                        c = 65535;
                        break;
                    case -755112654:
                        if (action.equals("android.intent.action.USER_STARTED")) {
                            c = 3;
                            break;
                        }
                        c = 65535;
                        break;
                    case 172491798:
                        if (action.equals("android.intent.action.PACKAGE_CHANGED")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    case 525384130:
                        if (action.equals("android.intent.action.PACKAGE_REMOVED")) {
                            c = 0;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1544582882:
                        if (action.equals("android.intent.action.PACKAGE_ADDED")) {
                            c = 2;
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
                        if (intent.getExtras().getBoolean("android.intent.extra.REPLACING")) {
                            return;
                        }
                        WebViewUpdateService.this.mImpl.packageStateChanged(WebViewUpdateService.packageNameFromIntent(intent), 3, userId);
                        return;
                    case 1:
                        if (WebViewUpdateService.entirePackageChanged(intent)) {
                            WebViewUpdateService.this.mImpl.packageStateChanged(WebViewUpdateService.packageNameFromIntent(intent), 0, userId);
                            return;
                        }
                        return;
                    case 2:
                        WebViewUpdateService.this.mImpl.packageStateChanged(WebViewUpdateService.packageNameFromIntent(intent), intent.getExtras().getBoolean("android.intent.extra.REPLACING") ? 2 : 1, userId);
                        return;
                    case 3:
                        WebViewUpdateService.this.mImpl.handleNewUser(userId);
                        return;
                    case 4:
                        WebViewUpdateService.this.mImpl.handleUserRemoved(userId);
                        return;
                    default:
                        return;
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addAction("android.intent.action.PACKAGE_CHANGED");
        filter.addDataScheme("package");
        for (WebViewProviderInfo provider : this.mImpl.getWebViewPackages()) {
            filter.addDataSchemeSpecificPart(provider.packageName, 0);
        }
        getContext().registerReceiverAsUser(this.mWebViewUpdatedReceiver, UserHandle.ALL, filter, null, null);
        IntentFilter userAddedFilter = new IntentFilter();
        userAddedFilter.addAction("android.intent.action.USER_STARTED");
        userAddedFilter.addAction("android.intent.action.USER_REMOVED");
        getContext().registerReceiverAsUser(this.mWebViewUpdatedReceiver, UserHandle.ALL, userAddedFilter, null, null);
        publishBinderService("webviewupdate", new BinderService(), true);
    }

    public void prepareWebViewInSystemServer() {
        this.mImpl.prepareWebViewInSystemServer();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String packageNameFromIntent(Intent intent) {
        return intent.getDataString().substring("package:".length());
    }

    public static boolean entirePackageChanged(Intent intent) {
        String[] componentList = intent.getStringArrayExtra("android.intent.extra.changed_component_name_list");
        return Arrays.asList(componentList).contains(intent.getDataString().substring("package:".length()));
    }

    /* loaded from: classes2.dex */
    private class BinderService extends IWebViewUpdateService.Stub {
        private BinderService() {
        }

        /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.webkit.WebViewUpdateService$BinderService */
        /* JADX WARN: Multi-variable type inference failed */
        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new WebViewUpdateServiceShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
        }

        public void notifyRelroCreationCompleted() {
            if (Binder.getCallingUid() != 1037 && Binder.getCallingUid() != 1000) {
                return;
            }
            long callingId = Binder.clearCallingIdentity();
            try {
                WebViewUpdateService.this.mImpl.notifyRelroCreationCompleted();
            } finally {
                Binder.restoreCallingIdentity(callingId);
            }
        }

        public WebViewProviderResponse waitForAndGetProvider() {
            if (Binder.getCallingPid() == Process.myPid()) {
                throw new IllegalStateException("Cannot create a WebView from the SystemServer");
            }
            WebViewProviderResponse webViewProviderResponse = WebViewUpdateService.this.mImpl.waitForAndGetProvider();
            if (webViewProviderResponse.packageInfo != null) {
                grantVisibilityToCaller(webViewProviderResponse.packageInfo.packageName, Binder.getCallingUid());
            }
            return webViewProviderResponse;
        }

        private void grantVisibilityToCaller(String webViewPackageName, int callingUid) {
            PackageManagerInternal pmInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            int webviewUid = pmInternal.getPackageUid(webViewPackageName, 0L, UserHandle.getUserId(callingUid));
            pmInternal.grantImplicitAccess(UserHandle.getUserId(callingUid), null, UserHandle.getAppId(callingUid), webviewUid, true);
        }

        public String changeProviderAndSetting(String newProvider) {
            if (WebViewUpdateService.this.getContext().checkCallingPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
                String msg = "Permission Denial: changeProviderAndSetting() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.WRITE_SECURE_SETTINGS";
                Slog.w(WebViewUpdateService.TAG, msg);
                throw new SecurityException(msg);
            }
            long callingId = Binder.clearCallingIdentity();
            try {
                return WebViewUpdateService.this.mImpl.changeProviderAndSetting(newProvider);
            } finally {
                Binder.restoreCallingIdentity(callingId);
            }
        }

        public WebViewProviderInfo[] getValidWebViewPackages() {
            return WebViewUpdateService.this.mImpl.getValidWebViewPackages();
        }

        public WebViewProviderInfo[] getAllWebViewPackages() {
            return WebViewUpdateService.this.mImpl.getWebViewPackages();
        }

        public String getCurrentWebViewPackageName() {
            PackageInfo pi = getCurrentWebViewPackage();
            if (pi == null) {
                return null;
            }
            return pi.packageName;
        }

        public PackageInfo getCurrentWebViewPackage() {
            PackageInfo currentWebViewPackage = WebViewUpdateService.this.mImpl.getCurrentWebViewPackage();
            if (currentWebViewPackage != null) {
                grantVisibilityToCaller(currentWebViewPackage.packageName, Binder.getCallingUid());
            }
            return currentWebViewPackage;
        }

        public boolean isMultiProcessEnabled() {
            return WebViewUpdateService.this.mImpl.isMultiProcessEnabled();
        }

        public void enableMultiProcess(boolean enable) {
            if (WebViewUpdateService.this.getContext().checkCallingPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
                String msg = "Permission Denial: enableMultiProcess() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.WRITE_SECURE_SETTINGS";
                Slog.w(WebViewUpdateService.TAG, msg);
                throw new SecurityException(msg);
            }
            long callingId = Binder.clearCallingIdentity();
            try {
                WebViewUpdateService.this.mImpl.enableMultiProcess(enable);
            } finally {
                Binder.restoreCallingIdentity(callingId);
            }
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(WebViewUpdateService.this.getContext(), WebViewUpdateService.TAG, pw)) {
                WebViewUpdateService.this.mImpl.dumpState(pw);
            }
        }
    }
}
