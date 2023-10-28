package com.android.server.appbinding;

import android.app.AppGlobals;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageManager;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.util.SparseBooleanArray;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.server.SystemService;
import com.android.server.am.PersistentConnection;
import com.android.server.appbinding.finders.AppServiceFinder;
import com.android.server.appbinding.finders.CarrierMessagingClientServiceFinder;
import com.transsion.hubcore.server.appbinding.ITranAppBindingService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public class AppBindingService extends Binder {
    public static final boolean DEBUG = false;
    public static final String TAG = "AppBindingService";
    private final ArrayList<AppServiceFinder> mApps;
    private final ArrayList<AppServiceConnection> mConnections;
    private AppBindingConstants mConstants;
    private final Context mContext;
    private final Handler mHandler;
    private final IPackageManager mIPackageManager;
    private final Injector mInjector;
    private final Object mLock;
    final BroadcastReceiver mPackageUserMonitor;
    private final SparseBooleanArray mRunningUsers;
    private final ContentObserver mSettingsObserver;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class Injector {
        Injector() {
        }

        public IPackageManager getIPackageManager() {
            return AppGlobals.getPackageManager();
        }

        public String getGlobalSettingString(ContentResolver resolver, String key) {
            return Settings.Global.getString(resolver, key);
        }
    }

    /* loaded from: classes.dex */
    public static class Lifecycle extends SystemService {
        final AppBindingService mService;

        public Lifecycle(Context context) {
            this(context, new Injector());
        }

        Lifecycle(Context context, Injector injector) {
            super(context);
            this.mService = new AppBindingService(injector, context);
        }

        @Override // com.android.server.SystemService
        public void onStart() {
            publishBinderService("app_binding", this.mService);
        }

        @Override // com.android.server.SystemService
        public void onBootPhase(int phase) {
            this.mService.onBootPhase(phase);
        }

        @Override // com.android.server.SystemService
        public void onUserStarting(SystemService.TargetUser user) {
            this.mService.onStartUser(user.getUserIdentifier());
        }

        @Override // com.android.server.SystemService
        public void onUserUnlocking(SystemService.TargetUser user) {
            this.mService.onUnlockUser(user.getUserIdentifier());
        }

        @Override // com.android.server.SystemService
        public void onUserStopping(SystemService.TargetUser user) {
            this.mService.onStopUser(user.getUserIdentifier());
        }
    }

    private AppBindingService(Injector injector, Context context) {
        this.mLock = new Object();
        this.mRunningUsers = new SparseBooleanArray(2);
        ArrayList<AppServiceFinder> arrayList = new ArrayList<>();
        this.mApps = arrayList;
        this.mConnections = new ArrayList<>();
        this.mSettingsObserver = new ContentObserver(null) { // from class: com.android.server.appbinding.AppBindingService.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                AppBindingService.this.refreshConstants();
            }
        };
        this.mPackageUserMonitor = new BroadcastReceiver() { // from class: com.android.server.appbinding.AppBindingService.2
            /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
            /* JADX WARN: Code restructure failed: missing block: B:22:0x0071, code lost:
                if (r1.equals("android.intent.action.PACKAGE_ADDED") != false) goto L20;
             */
            @Override // android.content.BroadcastReceiver
            /*
                Code decompiled incorrectly, please refer to instructions dump.
            */
            public void onReceive(Context context2, Intent intent) {
                int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                if (userId == -10000) {
                    Slog.w(AppBindingService.TAG, "Intent broadcast does not contain user handle: " + intent);
                    return;
                }
                String action = intent.getAction();
                if ("android.intent.action.USER_REMOVED".equals(action)) {
                    AppBindingService.this.onUserRemoved(userId);
                    return;
                }
                Uri intentUri = intent.getData();
                String packageName = intentUri != null ? intentUri.getSchemeSpecificPart() : null;
                if (packageName == null) {
                    Slog.w(AppBindingService.TAG, "Intent broadcast does not contain package name: " + intent);
                    return;
                }
                boolean z = false;
                boolean replacing = intent.getBooleanExtra("android.intent.extra.REPLACING", false);
                switch (action.hashCode()) {
                    case 172491798:
                        if (action.equals("android.intent.action.PACKAGE_CHANGED")) {
                            z = true;
                            break;
                        }
                        z = true;
                        break;
                    case 1544582882:
                        break;
                    default:
                        z = true;
                        break;
                }
                switch (z) {
                    case false:
                        if (replacing) {
                            AppBindingService.this.handlePackageAddedReplacing(packageName, userId);
                            return;
                        }
                        return;
                    case true:
                        AppBindingService.this.handlePackageAddedReplacing(packageName, userId);
                        return;
                    default:
                        return;
                }
            }
        };
        this.mInjector = injector;
        this.mContext = context;
        this.mIPackageManager = injector.getIPackageManager();
        Handler handler = BackgroundThread.getHandler();
        this.mHandler = handler;
        arrayList.add(new CarrierMessagingClientServiceFinder(context, new BiConsumer() { // from class: com.android.server.appbinding.AppBindingService$$ExternalSyntheticLambda4
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                AppBindingService.this.onAppChanged((AppServiceFinder) obj, ((Integer) obj2).intValue());
            }
        }, handler));
        ITranAppBindingService.Instance().onConstruct(arrayList, context);
        this.mConstants = AppBindingConstants.initializeFromString("");
    }

    private void forAllAppsLocked(Consumer<AppServiceFinder> consumer) {
        for (int i = 0; i < this.mApps.size(); i++) {
            consumer.accept(this.mApps.get(i));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onBootPhase(int phase) {
        switch (phase) {
            case SystemService.PHASE_ACTIVITY_MANAGER_READY /* 550 */:
                onPhaseActivityManagerReady();
                return;
            case 600:
                onPhaseThirdPartyAppsCanStart();
                return;
            default:
                return;
        }
    }

    private void onPhaseActivityManagerReady() {
        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction("android.intent.action.PACKAGE_ADDED");
        packageFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        packageFilter.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this.mPackageUserMonitor, UserHandle.ALL, packageFilter, null, this.mHandler);
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiverAsUser(this.mPackageUserMonitor, UserHandle.ALL, userFilter, null, this.mHandler);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("app_binding_constants"), false, this.mSettingsObserver);
        refreshConstants();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refreshConstants() {
        String newSetting = this.mInjector.getGlobalSettingString(this.mContext.getContentResolver(), "app_binding_constants");
        synchronized (this.mLock) {
            if (TextUtils.equals(this.mConstants.sourceSettings, newSetting)) {
                return;
            }
            Slog.i(TAG, "Updating constants with: " + newSetting);
            this.mConstants = AppBindingConstants.initializeFromString(newSetting);
            rebindAllLocked("settings update");
        }
    }

    private void onPhaseThirdPartyAppsCanStart() {
        synchronized (this.mLock) {
            forAllAppsLocked(new Consumer() { // from class: com.android.server.appbinding.AppBindingService$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((AppServiceFinder) obj).startMonitoring();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onStartUser(int userId) {
        synchronized (this.mLock) {
            this.mRunningUsers.append(userId, true);
            bindServicesLocked(userId, null, "user start");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUnlockUser(int userId) {
        synchronized (this.mLock) {
            bindServicesLocked(userId, null, "user unlock");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onStopUser(int userId) {
        synchronized (this.mLock) {
            unbindServicesLocked(userId, null, "user stop");
            this.mRunningUsers.delete(userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUserRemoved(final int userId) {
        synchronized (this.mLock) {
            forAllAppsLocked(new Consumer() { // from class: com.android.server.appbinding.AppBindingService$$ExternalSyntheticLambda2
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((AppServiceFinder) obj).onUserRemoved(userId);
                }
            });
            this.mRunningUsers.delete(userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onAppChanged(AppServiceFinder finder, int userId) {
        synchronized (this.mLock) {
            String reason = finder.getAppDescription() + " changed";
            unbindServicesLocked(userId, finder, reason);
            bindServicesLocked(userId, finder, reason);
        }
    }

    private AppServiceFinder findFinderLocked(int userId, String packageName) {
        for (int i = 0; i < this.mApps.size(); i++) {
            AppServiceFinder app = this.mApps.get(i);
            if (packageName.equals(app.getTargetPackage(userId))) {
                return app;
            }
        }
        return null;
    }

    private AppServiceConnection findConnectionLock(int userId, AppServiceFinder target) {
        for (int i = 0; i < this.mConnections.size(); i++) {
            AppServiceConnection conn = this.mConnections.get(i);
            if (conn.getUserId() == userId && conn.getFinder() == target) {
                return conn;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePackageAddedReplacing(String packageName, int userId) {
        synchronized (this.mLock) {
            AppServiceFinder finder = findFinderLocked(userId, packageName);
            if (finder != null) {
                unbindServicesLocked(userId, finder, "package update");
                bindServicesLocked(userId, finder, "package update");
            }
        }
    }

    private void rebindAllLocked(String reason) {
        for (int i = 0; i < this.mRunningUsers.size(); i++) {
            if (this.mRunningUsers.valueAt(i)) {
                int userId = this.mRunningUsers.keyAt(i);
                unbindServicesLocked(userId, null, reason);
                bindServicesLocked(userId, null, reason);
            }
        }
    }

    private void bindServicesLocked(int userId, AppServiceFinder target, String reasonForLog) {
        for (int i = 0; i < this.mApps.size(); i++) {
            AppServiceFinder app = this.mApps.get(i);
            if (target == null || target == app) {
                AppServiceConnection existingConn = findConnectionLock(userId, app);
                if (existingConn != null) {
                    unbindServicesLocked(userId, target, reasonForLog);
                }
                ServiceInfo service = app.findService(userId, this.mIPackageManager, this.mConstants);
                if (service != null) {
                    AppServiceConnection conn = new AppServiceConnection(this.mContext, userId, this.mConstants, this.mHandler, app, service.getComponentName());
                    this.mConnections.add(conn);
                    conn.bind();
                }
            }
        }
    }

    private void unbindServicesLocked(int userId, AppServiceFinder target, String reasonForLog) {
        for (int i = this.mConnections.size() - 1; i >= 0; i--) {
            AppServiceConnection conn = this.mConnections.get(i);
            if (conn.getUserId() == userId && (target == null || conn.getFinder() == target)) {
                this.mConnections.remove(i);
                conn.unbind();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class AppServiceConnection extends PersistentConnection<IInterface> {
        private final AppBindingConstants mConstants;
        private final AppServiceFinder mFinder;

        AppServiceConnection(Context context, int userId, AppBindingConstants constants, Handler handler, AppServiceFinder finder, ComponentName componentName) {
            super(AppBindingService.TAG, context, handler, userId, componentName, constants.SERVICE_RECONNECT_BACKOFF_SEC, constants.SERVICE_RECONNECT_BACKOFF_INCREASE, constants.SERVICE_RECONNECT_MAX_BACKOFF_SEC, constants.SERVICE_STABLE_CONNECTION_THRESHOLD_SEC);
            this.mFinder = finder;
            this.mConstants = constants;
        }

        @Override // com.android.server.am.PersistentConnection
        protected int getBindFlags() {
            return this.mFinder.getBindFlags(this.mConstants);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // com.android.server.am.PersistentConnection
        public IInterface asInterface(IBinder obj) {
            return this.mFinder.asInterface(obj);
        }

        public AppServiceFinder getFinder() {
            return this.mFinder;
        }
    }

    @Override // android.os.Binder
    public void dump(FileDescriptor fd, final PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            if (args.length > 0 && "-s".equals(args[0])) {
                dumpSimple(pw);
                return;
            }
            synchronized (this.mLock) {
                this.mConstants.dump("  ", pw);
                pw.println();
                pw.print("  Running users:");
                for (int i = 0; i < this.mRunningUsers.size(); i++) {
                    if (this.mRunningUsers.valueAt(i)) {
                        pw.print(" ");
                        pw.print(this.mRunningUsers.keyAt(i));
                    }
                }
                pw.println();
                pw.println("  Connections:");
                for (int i2 = 0; i2 < this.mConnections.size(); i2++) {
                    AppServiceConnection conn = this.mConnections.get(i2);
                    pw.print("    App type: ");
                    pw.print(conn.getFinder().getAppDescription());
                    pw.println();
                    conn.dump("      ", pw);
                }
                if (this.mConnections.size() == 0) {
                    pw.println("    None:");
                }
                pw.println();
                pw.println("  Finders:");
                forAllAppsLocked(new Consumer() { // from class: com.android.server.appbinding.AppBindingService$$ExternalSyntheticLambda0
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((AppServiceFinder) obj).dump("    ", pw);
                    }
                });
            }
        }
    }

    private void dumpSimple(final PrintWriter pw) {
        synchronized (this.mLock) {
            for (int i = 0; i < this.mConnections.size(); i++) {
                AppServiceConnection conn = this.mConnections.get(i);
                pw.print("conn,");
                pw.print(conn.getFinder().getAppDescription());
                pw.print(",");
                pw.print(conn.getUserId());
                pw.print(",");
                pw.print(conn.getComponentName().getPackageName());
                pw.print(",");
                pw.print(conn.getComponentName().getClassName());
                pw.print(",");
                pw.print(conn.isBound() ? "bound" : "not-bound");
                pw.print(",");
                pw.print(conn.isConnected() ? "connected" : "not-connected");
                pw.print(",#con=");
                pw.print(conn.getNumConnected());
                pw.print(",#dis=");
                pw.print(conn.getNumDisconnected());
                pw.print(",#died=");
                pw.print(conn.getNumBindingDied());
                pw.print(",backoff=");
                pw.print(conn.getNextBackoffMs());
                pw.println();
            }
            forAllAppsLocked(new Consumer() { // from class: com.android.server.appbinding.AppBindingService$$ExternalSyntheticLambda3
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((AppServiceFinder) obj).dumpSimple(pw);
                }
            });
        }
    }

    AppBindingConstants getConstantsForTest() {
        return this.mConstants;
    }
}
