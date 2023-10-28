package com.android.server.om;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.om.IOverlayManager;
import android.content.om.OverlayIdentifier;
import android.content.om.OverlayInfo;
import android.content.om.OverlayManagerTransaction;
import android.content.om.OverlayableInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.UserInfo;
import android.content.pm.overlay.OverlayPaths;
import android.content.res.ApkAssets;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.FabricatedOverlayInternal;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.content.om.OverlayConfig;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.FunctionalUtils;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.SystemConfig;
import com.android.server.SystemService;
import com.android.server.om.OverlayManagerService;
import com.android.server.om.OverlayManagerServiceImpl;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserManagerService;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.pkg.parsing.ParsingPackageUtils;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import libcore.util.EmptyArray;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public final class OverlayManagerService extends SystemService {
    static final boolean DEBUG = false;
    private static final String DEFAULT_OVERLAYS_PROP = "ro.boot.vendor.overlay.theme";
    static final String TAG = "OverlayManager";
    private final boolean DEBUG_FOR_NAVIGATION_CHANGE;
    private final OverlayActorEnforcer mActorEnforcer;
    private final OverlayManagerServiceImpl mImpl;
    private final Object mLock;
    private final PackageManagerHelperImpl mPackageManager;
    private final IBinder mService;
    private final OverlayManagerSettings mSettings;
    private final AtomicFile mSettingsFile;
    private final UserManagerService mUserManager;

    public OverlayManagerService(Context context) {
        super(context);
        this.DEBUG_FOR_NAVIGATION_CHANGE = true;
        this.mLock = new Object();
        IOverlayManager.Stub anonymousClass1 = new AnonymousClass1();
        this.mService = anonymousClass1;
        try {
            Trace.traceBegin(67108864L, "OMS#OverlayManagerService");
            this.mSettingsFile = new AtomicFile(new File(Environment.getDataSystemDirectory(), "overlays.xml"), "overlays");
            try {
                PackageManagerHelperImpl packageManagerHelperImpl = new PackageManagerHelperImpl(context);
                this.mPackageManager = packageManagerHelperImpl;
                this.mUserManager = UserManagerService.getInstance();
                IdmapManager im = new IdmapManager(IdmapDaemon.getInstance(), packageManagerHelperImpl);
                OverlayManagerSettings overlayManagerSettings = new OverlayManagerSettings();
                this.mSettings = overlayManagerSettings;
                this.mImpl = new OverlayManagerServiceImpl(packageManagerHelperImpl, im, overlayManagerSettings, OverlayConfig.getSystemInstance(), getDefaultOverlayPackages());
                this.mActorEnforcer = new OverlayActorEnforcer(packageManagerHelperImpl);
                HandlerThread packageReceiverThread = new HandlerThread(TAG);
                packageReceiverThread.start();
                IntentFilter packageFilter = new IntentFilter();
                packageFilter.addAction("android.intent.action.PACKAGE_ADDED");
                packageFilter.addAction("android.intent.action.PACKAGE_CHANGED");
                packageFilter.addAction("android.intent.action.PACKAGE_REMOVED");
                packageFilter.addDataScheme("package");
                getContext().registerReceiverAsUser(new PackageReceiver(), UserHandle.ALL, packageFilter, null, packageReceiverThread.getThreadHandler());
                IntentFilter userFilter = new IntentFilter();
                userFilter.addAction("android.intent.action.USER_ADDED");
                userFilter.addAction("android.intent.action.USER_REMOVED");
                getContext().registerReceiverAsUser(new UserReceiver(), UserHandle.ALL, userFilter, null, null);
                restoreSettings();
                final String shellPkgName = TextUtils.emptyIfNull(getContext().getString(17039402));
                overlayManagerSettings.removeIf(new Predicate() { // from class: com.android.server.om.OverlayManagerService$$ExternalSyntheticLambda3
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return OverlayManagerService.lambda$new$0(shellPkgName, (OverlayInfo) obj);
                    }
                });
                initIfNeeded();
                onSwitchUser(0);
                publishBinderService(ParsingPackageUtils.TAG_OVERLAY, anonymousClass1);
                publishLocalService(OverlayManagerService.class, this);
                Trace.traceEnd(67108864L);
            } catch (Throwable th) {
                th = th;
                Trace.traceEnd(67108864L);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$new$0(String shellPkgName, OverlayInfo overlayInfo) {
        return overlayInfo.isFabricated && shellPkgName.equals(overlayInfo.packageName);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
    }

    private void initIfNeeded() {
        UserManager um = (UserManager) getContext().getSystemService(UserManager.class);
        List<UserInfo> users = um.getAliveUsers();
        synchronized (this.mLock) {
            int userCount = users.size();
            for (int i = 0; i < userCount; i++) {
                UserInfo userInfo = users.get(i);
                if (!userInfo.supportsSwitchTo() && userInfo.id != 0) {
                    updatePackageManagerLocked(this.mImpl.updateOverlaysForUser(users.get(i).id));
                }
            }
        }
    }

    @Override // com.android.server.SystemService
    public void onUserSwitching(SystemService.TargetUser from, SystemService.TargetUser to) {
        onSwitchUser(to.getUserIdentifier());
    }

    private void onSwitchUser(int newUserId) {
        try {
            Trace.traceBegin(67108864L, "OMS#onSwitchUser " + newUserId);
            synchronized (this.mLock) {
                updateTargetPackagesLocked(this.mImpl.updateOverlaysForUser(newUserId));
            }
        } finally {
            Trace.traceEnd(67108864L);
        }
    }

    private static String[] getDefaultOverlayPackages() {
        String[] split;
        String str = SystemProperties.get(DEFAULT_OVERLAYS_PROP);
        if (TextUtils.isEmpty(str)) {
            return EmptyArray.STRING;
        }
        ArraySet<String> defaultPackages = new ArraySet<>();
        for (String packageName : str.split(";")) {
            if (!TextUtils.isEmpty(packageName)) {
                defaultPackages.add(packageName);
            }
        }
        return (String[]) defaultPackages.toArray(new String[defaultPackages.size()]);
    }

    /* loaded from: classes2.dex */
    private final class PackageReceiver extends BroadcastReceiver {
        private PackageReceiver() {
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        /* JADX WARN: Code restructure failed: missing block: B:18:0x0052, code lost:
            if (r0.equals("android.intent.action.PACKAGE_ADDED") != false) goto L17;
         */
        @Override // android.content.BroadcastReceiver
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                Slog.e(OverlayManagerService.TAG, "Cannot handle package broadcast with null action");
                return;
            }
            Uri data = intent.getData();
            if (data == null) {
                Slog.e(OverlayManagerService.TAG, "Cannot handle package broadcast with null data");
                return;
            }
            String packageName = data.getSchemeSpecificPart();
            char c = 0;
            boolean replacing = intent.getBooleanExtra("android.intent.extra.REPLACING", false);
            int extraUid = intent.getIntExtra("android.intent.extra.UID", -10000);
            int[] userIds = extraUid == -10000 ? OverlayManagerService.this.mUserManager.getUserIds() : new int[]{UserHandle.getUserId(extraUid)};
            switch (action.hashCode()) {
                case 172491798:
                    if (action.equals("android.intent.action.PACKAGE_CHANGED")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 525384130:
                    if (action.equals("android.intent.action.PACKAGE_REMOVED")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 1544582882:
                    break;
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                    if (replacing) {
                        onPackageReplaced(packageName, userIds);
                        return;
                    } else {
                        onPackageAdded(packageName, userIds);
                        return;
                    }
                case 1:
                    if (!"android.intent.action.OVERLAY_CHANGED".equals(intent.getStringExtra("android.intent.extra.REASON"))) {
                        onPackageChanged(packageName, userIds);
                        return;
                    }
                    return;
                case 2:
                    if (replacing) {
                        onPackageReplacing(packageName, userIds);
                        return;
                    } else {
                        onPackageRemoved(packageName, userIds);
                        return;
                    }
                default:
                    return;
            }
        }

        private void onPackageAdded(String packageName, int[] userIds) {
            try {
                Trace.traceBegin(67108864L, "OMS#onPackageAdded " + packageName);
                for (int userId : userIds) {
                    synchronized (OverlayManagerService.this.mLock) {
                        AndroidPackage pkg = OverlayManagerService.this.mPackageManager.onPackageAdded(packageName, userId);
                        if (pkg != null && !OverlayManagerService.this.mPackageManager.isInstantApp(packageName, userId)) {
                            try {
                                OverlayManagerService overlayManagerService = OverlayManagerService.this;
                                overlayManagerService.updateTargetPackagesLocked(overlayManagerService.mImpl.onPackageAdded(packageName, userId));
                            } catch (OverlayManagerServiceImpl.OperationFailedException e) {
                                Slog.e(OverlayManagerService.TAG, "onPackageAdded internal error", e);
                            }
                        }
                    }
                }
            } finally {
                Trace.traceEnd(67108864L);
            }
        }

        private void onPackageChanged(String packageName, int[] userIds) {
            try {
                Trace.traceBegin(67108864L, "OMS#onPackageChanged " + packageName);
                for (int userId : userIds) {
                    synchronized (OverlayManagerService.this.mLock) {
                        AndroidPackage pkg = OverlayManagerService.this.mPackageManager.onPackageUpdated(packageName, userId);
                        if (pkg != null && !OverlayManagerService.this.mPackageManager.isInstantApp(packageName, userId)) {
                            try {
                                OverlayManagerService overlayManagerService = OverlayManagerService.this;
                                overlayManagerService.updateTargetPackagesLocked(overlayManagerService.mImpl.onPackageChanged(packageName, userId));
                            } catch (OverlayManagerServiceImpl.OperationFailedException e) {
                                Slog.e(OverlayManagerService.TAG, "onPackageChanged internal error", e);
                            }
                        }
                    }
                }
            } finally {
                Trace.traceEnd(67108864L);
            }
        }

        private void onPackageReplacing(String packageName, int[] userIds) {
            try {
                Trace.traceBegin(67108864L, "OMS#onPackageReplacing " + packageName);
                for (int userId : userIds) {
                    synchronized (OverlayManagerService.this.mLock) {
                        AndroidPackage pkg = OverlayManagerService.this.mPackageManager.onPackageUpdated(packageName, userId);
                        if (pkg != null && !OverlayManagerService.this.mPackageManager.isInstantApp(packageName, userId)) {
                            try {
                                OverlayManagerService overlayManagerService = OverlayManagerService.this;
                                overlayManagerService.updateTargetPackagesLocked(overlayManagerService.mImpl.onPackageReplacing(packageName, userId));
                            } catch (OverlayManagerServiceImpl.OperationFailedException e) {
                                Slog.e(OverlayManagerService.TAG, "onPackageReplacing internal error", e);
                            }
                        }
                    }
                }
            } finally {
                Trace.traceEnd(67108864L);
            }
        }

        private void onPackageReplaced(String packageName, int[] userIds) {
            try {
                Trace.traceBegin(67108864L, "OMS#onPackageReplaced " + packageName);
                for (int userId : userIds) {
                    synchronized (OverlayManagerService.this.mLock) {
                        AndroidPackage pkg = OverlayManagerService.this.mPackageManager.onPackageUpdated(packageName, userId);
                        if (pkg != null && !OverlayManagerService.this.mPackageManager.isInstantApp(packageName, userId)) {
                            try {
                                OverlayManagerService overlayManagerService = OverlayManagerService.this;
                                overlayManagerService.updateTargetPackagesLocked(overlayManagerService.mImpl.onPackageReplaced(packageName, userId));
                            } catch (OverlayManagerServiceImpl.OperationFailedException e) {
                                Slog.e(OverlayManagerService.TAG, "onPackageReplaced internal error", e);
                            }
                        }
                    }
                }
            } finally {
                Trace.traceEnd(67108864L);
            }
        }

        private void onPackageRemoved(String packageName, int[] userIds) {
            try {
                Trace.traceBegin(67108864L, "OMS#onPackageRemoved " + packageName);
                for (int userId : userIds) {
                    synchronized (OverlayManagerService.this.mLock) {
                        OverlayManagerService.this.mPackageManager.onPackageRemoved(packageName, userId);
                        OverlayManagerService overlayManagerService = OverlayManagerService.this;
                        overlayManagerService.updateTargetPackagesLocked(overlayManagerService.mImpl.onPackageRemoved(packageName, userId));
                    }
                }
            } finally {
                Trace.traceEnd(67108864L);
            }
        }
    }

    /* loaded from: classes2.dex */
    private final class UserReceiver extends BroadcastReceiver {
        private UserReceiver() {
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            char c;
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
            String action = intent.getAction();
            switch (action.hashCode()) {
                case -2061058799:
                    if (action.equals("android.intent.action.USER_REMOVED")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 1121780209:
                    if (action.equals("android.intent.action.USER_ADDED")) {
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
                    if (userId != -10000) {
                        try {
                            Trace.traceBegin(67108864L, "OMS ACTION_USER_ADDED");
                            synchronized (OverlayManagerService.this.mLock) {
                                OverlayManagerService overlayManagerService = OverlayManagerService.this;
                                overlayManagerService.updatePackageManagerLocked(overlayManagerService.mImpl.updateOverlaysForUser(userId));
                            }
                            return;
                        } finally {
                        }
                    }
                    return;
                case 1:
                    if (userId != -10000) {
                        try {
                            Trace.traceBegin(67108864L, "OMS ACTION_USER_REMOVED");
                            synchronized (OverlayManagerService.this.mLock) {
                                OverlayManagerService.this.mImpl.onUserRemoved(userId);
                                OverlayManagerService.this.mPackageManager.forgetAllPackageInfos(userId);
                            }
                            return;
                        } finally {
                        }
                    }
                    return;
                default:
                    return;
            }
        }
    }

    /* renamed from: com.android.server.om.OverlayManagerService$1  reason: invalid class name */
    /* loaded from: classes2.dex */
    class AnonymousClass1 extends IOverlayManager.Stub {
        AnonymousClass1() {
        }

        public Map<String, List<OverlayInfo>> getAllOverlays(int userIdArg) {
            Map<String, List<OverlayInfo>> overlaysForUser;
            try {
                Trace.traceBegin(67108864L, "OMS#getAllOverlays " + userIdArg);
                int realUserId = handleIncomingUser(userIdArg, "getAllOverlays");
                synchronized (OverlayManagerService.this.mLock) {
                    overlaysForUser = OverlayManagerService.this.mImpl.getOverlaysForUser(realUserId);
                }
                return overlaysForUser;
            } finally {
                Trace.traceEnd(67108864L);
            }
        }

        public List<OverlayInfo> getOverlayInfosForTarget(String targetPackageName, int userIdArg) {
            List<OverlayInfo> overlayInfosForTarget;
            if (targetPackageName == null) {
                return Collections.emptyList();
            }
            try {
                Trace.traceBegin(67108864L, "OMS#getOverlayInfosForTarget " + targetPackageName);
                int realUserId = handleIncomingUser(userIdArg, "getOverlayInfosForTarget");
                synchronized (OverlayManagerService.this.mLock) {
                    overlayInfosForTarget = OverlayManagerService.this.mImpl.getOverlayInfosForTarget(targetPackageName, realUserId);
                }
                return overlayInfosForTarget;
            } finally {
                Trace.traceEnd(67108864L);
            }
        }

        public OverlayInfo getOverlayInfo(String packageName, int userIdArg) {
            return getOverlayInfoByIdentifier(new OverlayIdentifier(packageName), userIdArg);
        }

        public OverlayInfo getOverlayInfoByIdentifier(OverlayIdentifier overlay, int userIdArg) {
            OverlayInfo overlayInfo;
            if (overlay == null || overlay.getPackageName() == null) {
                return null;
            }
            try {
                Trace.traceBegin(67108864L, "OMS#getOverlayInfo " + overlay);
                int realUserId = handleIncomingUser(userIdArg, "getOverlayInfo");
                synchronized (OverlayManagerService.this.mLock) {
                    overlayInfo = OverlayManagerService.this.mImpl.getOverlayInfo(overlay, realUserId);
                }
                return overlayInfo;
            } finally {
                Trace.traceEnd(67108864L);
            }
        }

        public boolean setEnabled(String packageName, boolean enable, int userIdArg) {
            if (packageName == null) {
                return false;
            }
            try {
                Trace.traceBegin(67108864L, "OMS#setEnabled " + packageName + " " + enable);
                OverlayIdentifier overlay = new OverlayIdentifier(packageName);
                int realUserId = handleIncomingUser(userIdArg, "setEnabled");
                enforceActor(overlay, "setEnabled", realUserId);
                long ident = Binder.clearCallingIdentity();
                synchronized (OverlayManagerService.this.mLock) {
                    try {
                        OverlayManagerService overlayManagerService = OverlayManagerService.this;
                        overlayManagerService.updateTargetPackagesLocked(overlayManagerService.mImpl.setEnabled(overlay, enable, realUserId));
                    } catch (OverlayManagerServiceImpl.OperationFailedException e) {
                        Binder.restoreCallingIdentity(ident);
                        return false;
                    }
                }
                Binder.restoreCallingIdentity(ident);
                return true;
            } finally {
                Trace.traceEnd(67108864L);
            }
        }

        public boolean setEnabledExclusive(String packageName, boolean enable, int userIdArg) {
            if (packageName == null || !enable) {
                return false;
            }
            try {
                Trace.traceBegin(67108864L, "OMS#setEnabledExclusive " + packageName + " " + enable);
                OverlayIdentifier overlay = new OverlayIdentifier(packageName);
                int realUserId = handleIncomingUser(userIdArg, "setEnabledExclusive");
                enforceActor(overlay, "setEnabledExclusive", realUserId);
                long ident = Binder.clearCallingIdentity();
                synchronized (OverlayManagerService.this.mLock) {
                    try {
                        Optional<PackageAndUser> enabledExclusive = OverlayManagerService.this.mImpl.setEnabledExclusive(overlay, false, realUserId);
                        final OverlayManagerService overlayManagerService = OverlayManagerService.this;
                        enabledExclusive.ifPresent(new Consumer() { // from class: com.android.server.om.OverlayManagerService$1$$ExternalSyntheticLambda0
                            @Override // java.util.function.Consumer
                            public final void accept(Object obj) {
                                OverlayManagerService.this.updateTargetPackagesLocked((PackageAndUser) obj);
                            }
                        });
                    } catch (OverlayManagerServiceImpl.OperationFailedException e) {
                        Binder.restoreCallingIdentity(ident);
                        return false;
                    }
                }
                Binder.restoreCallingIdentity(ident);
                return true;
            } finally {
                Trace.traceEnd(67108864L);
            }
        }

        public boolean setEnabledExclusiveInCategory(String packageName, int userIdArg) {
            Log.i(OverlayManagerService.TAG, "[RetestTagOfQyx OMS setEnabledExclusiveInCategory] packageName=" + packageName + ", userIdArg=" + userIdArg);
            if (packageName == null) {
                return false;
            }
            try {
                Trace.traceBegin(67108864L, "OMS#setEnabledExclusiveInCategory " + packageName);
                OverlayIdentifier overlay = new OverlayIdentifier(packageName);
                int realUserId = handleIncomingUser(userIdArg, "setEnabledExclusiveInCategory");
                enforceActor(overlay, "setEnabledExclusiveInCategory", realUserId);
                long ident = Binder.clearCallingIdentity();
                synchronized (OverlayManagerService.this.mLock) {
                    try {
                        Optional<PackageAndUser> enabledExclusive = OverlayManagerService.this.mImpl.setEnabledExclusive(overlay, true, realUserId);
                        final OverlayManagerService overlayManagerService = OverlayManagerService.this;
                        enabledExclusive.ifPresent(new Consumer() { // from class: com.android.server.om.OverlayManagerService$1$$ExternalSyntheticLambda3
                            @Override // java.util.function.Consumer
                            public final void accept(Object obj) {
                                OverlayManagerService.this.updateTargetPackagesLocked((PackageAndUser) obj);
                            }
                        });
                        Log.i(OverlayManagerService.TAG, "[RetestTagOfQyx OMS setEnabledExclusiveInCategory] after mImpl.setEnabledExclusive.ifPresent, return true");
                    } catch (OverlayManagerServiceImpl.OperationFailedException e) {
                        Log.i(OverlayManagerService.TAG, "[RetestTagOfQyx OMS setEnabledExclusiveInCategory] exception in mImpl.setEnabledExclusive.ifPresent, return false");
                        Binder.restoreCallingIdentity(ident);
                        return false;
                    }
                }
                Binder.restoreCallingIdentity(ident);
                return true;
            } finally {
                Trace.traceEnd(67108864L);
            }
        }

        public boolean setPriority(String packageName, String parentPackageName, int userIdArg) {
            if (packageName == null || parentPackageName == null) {
                return false;
            }
            try {
                Trace.traceBegin(67108864L, "OMS#setPriority " + packageName + " " + parentPackageName);
                OverlayIdentifier overlay = new OverlayIdentifier(packageName);
                OverlayIdentifier parentOverlay = new OverlayIdentifier(parentPackageName);
                int realUserId = handleIncomingUser(userIdArg, "setPriority");
                enforceActor(overlay, "setPriority", realUserId);
                long ident = Binder.clearCallingIdentity();
                synchronized (OverlayManagerService.this.mLock) {
                    try {
                        Optional<PackageAndUser> priority = OverlayManagerService.this.mImpl.setPriority(overlay, parentOverlay, realUserId);
                        final OverlayManagerService overlayManagerService = OverlayManagerService.this;
                        priority.ifPresent(new Consumer() { // from class: com.android.server.om.OverlayManagerService$1$$ExternalSyntheticLambda1
                            @Override // java.util.function.Consumer
                            public final void accept(Object obj) {
                                OverlayManagerService.this.updateTargetPackagesLocked((PackageAndUser) obj);
                            }
                        });
                    } catch (OverlayManagerServiceImpl.OperationFailedException e) {
                        Binder.restoreCallingIdentity(ident);
                        return false;
                    }
                }
                Binder.restoreCallingIdentity(ident);
                return true;
            } finally {
                Trace.traceEnd(67108864L);
            }
        }

        public boolean setHighestPriority(String packageName, int userIdArg) {
            if (packageName == null) {
                return false;
            }
            try {
                Trace.traceBegin(67108864L, "OMS#setHighestPriority " + packageName);
                OverlayIdentifier overlay = new OverlayIdentifier(packageName);
                int realUserId = handleIncomingUser(userIdArg, "setHighestPriority");
                enforceActor(overlay, "setHighestPriority", realUserId);
                long ident = Binder.clearCallingIdentity();
                synchronized (OverlayManagerService.this.mLock) {
                    try {
                        OverlayManagerService overlayManagerService = OverlayManagerService.this;
                        overlayManagerService.updateTargetPackagesLocked(overlayManagerService.mImpl.setHighestPriority(overlay, realUserId));
                    } catch (OverlayManagerServiceImpl.OperationFailedException e) {
                        Binder.restoreCallingIdentity(ident);
                        return false;
                    }
                }
                Binder.restoreCallingIdentity(ident);
                return true;
            } finally {
                Trace.traceEnd(67108864L);
            }
        }

        public boolean setLowestPriority(String packageName, int userIdArg) {
            if (packageName == null) {
                return false;
            }
            try {
                Trace.traceBegin(67108864L, "OMS#setLowestPriority " + packageName);
                OverlayIdentifier overlay = new OverlayIdentifier(packageName);
                int realUserId = handleIncomingUser(userIdArg, "setLowestPriority");
                enforceActor(overlay, "setLowestPriority", realUserId);
                long ident = Binder.clearCallingIdentity();
                synchronized (OverlayManagerService.this.mLock) {
                    try {
                        Optional<PackageAndUser> lowestPriority = OverlayManagerService.this.mImpl.setLowestPriority(overlay, realUserId);
                        final OverlayManagerService overlayManagerService = OverlayManagerService.this;
                        lowestPriority.ifPresent(new Consumer() { // from class: com.android.server.om.OverlayManagerService$1$$ExternalSyntheticLambda2
                            @Override // java.util.function.Consumer
                            public final void accept(Object obj) {
                                OverlayManagerService.this.updateTargetPackagesLocked((PackageAndUser) obj);
                            }
                        });
                    } catch (OverlayManagerServiceImpl.OperationFailedException e) {
                        Binder.restoreCallingIdentity(ident);
                        return false;
                    }
                }
                Binder.restoreCallingIdentity(ident);
                return true;
            } finally {
                Trace.traceEnd(67108864L);
            }
        }

        public String[] getDefaultOverlayPackages() {
            String[] defaultOverlayPackages;
            try {
                Trace.traceBegin(67108864L, "OMS#getDefaultOverlayPackages");
                OverlayManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.MODIFY_THEME_OVERLAY", null);
                long ident = Binder.clearCallingIdentity();
                synchronized (OverlayManagerService.this.mLock) {
                    defaultOverlayPackages = OverlayManagerService.this.mImpl.getDefaultOverlayPackages();
                }
                Binder.restoreCallingIdentity(ident);
                return defaultOverlayPackages;
            } finally {
                Trace.traceEnd(67108864L);
            }
        }

        public void invalidateCachesForOverlay(String packageName, int userIdArg) {
            if (packageName == null) {
                return;
            }
            OverlayIdentifier overlay = new OverlayIdentifier(packageName);
            int realUserId = handleIncomingUser(userIdArg, "invalidateCachesForOverlay");
            enforceActor(overlay, "invalidateCachesForOverlay", realUserId);
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (OverlayManagerService.this.mLock) {
                    try {
                        OverlayManagerService.this.mImpl.removeIdmapForOverlay(overlay, realUserId);
                    } catch (OverlayManagerServiceImpl.OperationFailedException e) {
                        Slog.w(OverlayManagerService.TAG, "invalidate caches for overlay '" + overlay + "' failed", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void commit(OverlayManagerTransaction transaction) throws RemoteException {
            try {
                Trace.traceBegin(67108864L, "OMS#commit " + transaction);
                try {
                    executeAllRequests(transaction);
                } catch (Exception e) {
                    long ident = Binder.clearCallingIdentity();
                    OverlayManagerService.this.restoreSettings();
                    Binder.restoreCallingIdentity(ident);
                    Slog.d(OverlayManagerService.TAG, "commit failed: " + e.getMessage(), e);
                    throw new SecurityException("commit failed");
                }
            } finally {
                Trace.traceEnd(67108864L);
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [980=5] */
        private Set<PackageAndUser> executeRequest(OverlayManagerTransaction.Request request) throws OverlayManagerServiceImpl.OperationFailedException {
            int realUserId;
            Objects.requireNonNull(request, "Transaction contains a null request");
            Objects.requireNonNull(request.overlay, "Transaction overlay identifier must be non-null");
            int callingUid = Binder.getCallingUid();
            if (request.type != 2 && request.type != 3) {
                realUserId = handleIncomingUser(request.userId, request.typeToString());
                enforceActor(request.overlay, request.typeToString(), realUserId);
            } else if (request.userId != -1) {
                throw new IllegalArgumentException(request.typeToString() + " unsupported for user " + request.userId);
            } else {
                if (callingUid == 2000) {
                    EventLog.writeEvent(1397638484, "202768292", -1, "");
                    throw new IllegalArgumentException("Non-root shell cannot fabricate overlays");
                }
                realUserId = -1;
                String pkgName = request.overlay.getPackageName();
                if (callingUid != 0 && !ArrayUtils.contains(OverlayManagerService.this.mPackageManager.getPackagesForUid(callingUid), pkgName)) {
                    throw new IllegalArgumentException("UID " + callingUid + " does own packagename " + pkgName);
                }
            }
            long ident = Binder.clearCallingIdentity();
            try {
                switch (request.type) {
                    case 0:
                        Set<PackageAndUser> result = CollectionUtils.addAll((Set) null, OverlayManagerService.this.mImpl.setEnabled(request.overlay, true, realUserId));
                        return CollectionUtils.emptyIfNull(CollectionUtils.addAll(result, OverlayManagerService.this.mImpl.setHighestPriority(request.overlay, realUserId)));
                    case 1:
                        return OverlayManagerService.this.mImpl.setEnabled(request.overlay, false, realUserId);
                    case 2:
                        FabricatedOverlayInternal fabricated = request.extras.getParcelable("fabricated_overlay");
                        Objects.requireNonNull(fabricated, "no fabricated overlay attached to request");
                        return OverlayManagerService.this.mImpl.registerFabricatedOverlay(fabricated);
                    case 3:
                        return OverlayManagerService.this.mImpl.unregisterFabricatedOverlay(request.overlay);
                    default:
                        throw new IllegalArgumentException("unsupported request: " + request);
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        private void executeAllRequests(OverlayManagerTransaction transaction) throws OverlayManagerServiceImpl.OperationFailedException {
            if (transaction == null) {
                throw new IllegalArgumentException("null transaction");
            }
            synchronized (OverlayManagerService.this.mLock) {
                Set<PackageAndUser> affectedPackagesToUpdate = null;
                Iterator it = transaction.iterator();
                while (it.hasNext()) {
                    OverlayManagerTransaction.Request request = (OverlayManagerTransaction.Request) it.next();
                    affectedPackagesToUpdate = CollectionUtils.addAll(affectedPackagesToUpdate, executeRequest(request));
                }
                long ident = Binder.clearCallingIdentity();
                OverlayManagerService.this.updateTargetPackagesLocked(affectedPackagesToUpdate);
                Binder.restoreCallingIdentity(ident);
            }
        }

        /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.om.OverlayManagerService$1 */
        /* JADX WARN: Multi-variable type inference failed */
        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new OverlayManagerShellCommand(OverlayManagerService.this.getContext(), this).exec(this, in, out, err, args, callback, resultReceiver);
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            String opt;
            DumpState dumpState = new DumpState();
            char c = 65535;
            dumpState.setUserId(-1);
            int opti = 0;
            while (opti < args.length && (opt = args[opti]) != null && opt.length() > 0 && opt.charAt(0) == '-') {
                opti++;
                if ("-h".equals(opt)) {
                    pw.println("dump [-h] [--verbose] [--user USER_ID] [[FIELD] PACKAGE]");
                    pw.println("  Print debugging information about the overlay manager.");
                    pw.println("  With optional parameter PACKAGE, limit output to the specified");
                    pw.println("  package. With optional parameter FIELD, limit output to");
                    pw.println("  the value of that SettingsItem field. Field names are");
                    pw.println("  case insensitive and out.println the m prefix can be omitted,");
                    pw.println("  so the following are equivalent: mState, mstate, State, state.");
                    return;
                } else if ("--user".equals(opt)) {
                    if (opti >= args.length) {
                        pw.println("Error: user missing argument");
                        return;
                    }
                    try {
                        dumpState.setUserId(Integer.parseInt(args[opti]));
                        opti++;
                    } catch (NumberFormatException e) {
                        pw.println("Error: user argument is not a number: " + args[opti]);
                        return;
                    }
                } else if ("--verbose".equals(opt)) {
                    dumpState.setVerbose(true);
                } else {
                    pw.println("Unknown argument: " + opt + "; use -h for help");
                }
            }
            if (opti < args.length) {
                String arg = args[opti];
                opti++;
                switch (arg.hashCode()) {
                    case -1750736508:
                        if (arg.equals("targetoverlayablename")) {
                            c = 3;
                            break;
                        }
                        break;
                    case -1248283232:
                        if (arg.equals("targetpackagename")) {
                            c = 2;
                            break;
                        }
                        break;
                    case -1165461084:
                        if (arg.equals("priority")) {
                            c = '\b';
                            break;
                        }
                        break;
                    case -836029914:
                        if (arg.equals("userid")) {
                            c = 1;
                            break;
                        }
                        break;
                    case -831052100:
                        if (arg.equals("ismutable")) {
                            c = 7;
                            break;
                        }
                        break;
                    case 50511102:
                        if (arg.equals("category")) {
                            c = '\t';
                            break;
                        }
                        break;
                    case 109757585:
                        if (arg.equals("state")) {
                            c = 5;
                            break;
                        }
                        break;
                    case 440941271:
                        if (arg.equals("isenabled")) {
                            c = 6;
                            break;
                        }
                        break;
                    case 909712337:
                        if (arg.equals("packagename")) {
                            c = 0;
                            break;
                        }
                        break;
                    case 1693907299:
                        if (arg.equals("basecodepath")) {
                            c = 4;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    case '\b':
                    case '\t':
                        dumpState.setField(arg);
                        break;
                    default:
                        dumpState.setOverlyIdentifier(arg);
                        break;
                }
            }
            if (dumpState.getPackageName() == null && opti < args.length) {
                dumpState.setOverlyIdentifier(args[opti]);
                int i = opti + 1;
            }
            enforceDumpPermission("dump");
            synchronized (OverlayManagerService.this.mLock) {
                OverlayManagerService.this.mImpl.dump(pw, dumpState);
                if (dumpState.getPackageName() == null) {
                    OverlayManagerService.this.mPackageManager.dump(pw, dumpState);
                }
            }
        }

        private int handleIncomingUser(int userId, String message) {
            return ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, message, null);
        }

        private void enforceDumpPermission(String message) {
            OverlayManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.DUMP", message);
        }

        private void enforceActor(OverlayIdentifier overlay, String methodName, int realUserId) throws SecurityException {
            OverlayInfo overlayInfo = OverlayManagerService.this.mImpl.getOverlayInfo(overlay, realUserId);
            if (overlayInfo == null) {
                throw new IllegalArgumentException("Unable to retrieve overlay information for " + overlay);
            }
            int callingUid = Binder.getCallingUid();
            OverlayManagerService.this.mActorEnforcer.enforceActor(overlayInfo, methodName, callingUid, realUserId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class PackageManagerHelperImpl implements PackageManagerHelper {
        private static final String TAB1 = "    ";
        private final Context mContext;
        private final ArrayMap<String, AndroidPackageUsers> mCache = new ArrayMap<>();
        private final Set<Integer> mInitializedUsers = new ArraySet();
        private final IPackageManager mPackageManager = AppGlobals.getPackageManager();
        private final PackageManagerInternal mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public static class AndroidPackageUsers {
            private final Set<Integer> mInstalledUsers;
            private AndroidPackage mPackage;

            private AndroidPackageUsers(AndroidPackage pkg) {
                this.mInstalledUsers = new ArraySet();
                this.mPackage = pkg;
            }
        }

        PackageManagerHelperImpl(Context context) {
            this.mContext = context;
        }

        @Override // com.android.server.om.PackageManagerHelper
        public ArrayMap<String, AndroidPackage> initializeForUser(final int userId) {
            if (!this.mInitializedUsers.contains(Integer.valueOf(userId))) {
                this.mInitializedUsers.add(Integer.valueOf(userId));
                this.mPackageManagerInternal.forEachInstalledPackage(new Consumer() { // from class: com.android.server.om.OverlayManagerService$PackageManagerHelperImpl$$ExternalSyntheticLambda0
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        OverlayManagerService.PackageManagerHelperImpl.this.m5240xaeb45001(userId, (AndroidPackage) obj);
                    }
                }, userId);
            }
            ArrayMap<String, AndroidPackage> userPackages = new ArrayMap<>();
            int n = this.mCache.size();
            for (int i = 0; i < n; i++) {
                AndroidPackageUsers pkg = this.mCache.valueAt(i);
                if (pkg.mInstalledUsers.contains(Integer.valueOf(userId))) {
                    userPackages.put(this.mCache.keyAt(i), pkg.mPackage);
                }
            }
            return userPackages;
        }

        @Override // com.android.server.om.PackageManagerHelper
        public AndroidPackage getPackageForUser(String packageName, int userId) {
            AndroidPackageUsers pkg = this.mCache.get(packageName);
            if (pkg != null && pkg.mInstalledUsers.contains(Integer.valueOf(userId))) {
                return pkg.mPackage;
            }
            try {
                if (!this.mPackageManager.isPackageAvailable(packageName, userId)) {
                    return null;
                }
                return addPackageUser(packageName, userId);
            } catch (RemoteException e) {
                Slog.w(OverlayManagerService.TAG, "Failed to check availability of package '" + packageName + "' for user " + userId, e);
                return null;
            }
        }

        private AndroidPackage addPackageUser(String packageName, int user) {
            AndroidPackage pkg = this.mPackageManagerInternal.getPackage(packageName);
            if (pkg == null) {
                Slog.w(OverlayManagerService.TAG, "Android package for '" + packageName + "' could not be found; continuing as if package was never added", new Throwable());
                return null;
            }
            return m5240xaeb45001(pkg, user);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: private */
        /* renamed from: addPackageUser */
        public AndroidPackage m5240xaeb45001(AndroidPackage pkg, int user) {
            AndroidPackageUsers pkgUsers = this.mCache.get(pkg.getPackageName());
            if (pkgUsers == null) {
                pkgUsers = new AndroidPackageUsers(pkg);
                this.mCache.put(pkg.getPackageName(), pkgUsers);
            } else {
                pkgUsers.mPackage = pkg;
            }
            pkgUsers.mInstalledUsers.add(Integer.valueOf(user));
            return pkgUsers.mPackage;
        }

        private void removePackageUser(String packageName, int user) {
            AndroidPackageUsers pkgUsers = this.mCache.get(packageName);
            if (pkgUsers == null) {
                return;
            }
            removePackageUser(pkgUsers, user);
        }

        private void removePackageUser(AndroidPackageUsers pkg, int user) {
            pkg.mInstalledUsers.remove(Integer.valueOf(user));
            if (pkg.mInstalledUsers.isEmpty()) {
                this.mCache.remove(pkg.mPackage.getPackageName());
            }
        }

        public AndroidPackage onPackageAdded(String packageName, int userId) {
            return addPackageUser(packageName, userId);
        }

        public AndroidPackage onPackageUpdated(String packageName, int userId) {
            return addPackageUser(packageName, userId);
        }

        public void onPackageRemoved(String packageName, int userId) {
            removePackageUser(packageName, userId);
        }

        @Override // com.android.server.om.PackageManagerHelper
        public boolean isInstantApp(String packageName, int userId) {
            return this.mPackageManagerInternal.isInstantApp(packageName, userId);
        }

        @Override // com.android.server.om.PackageManagerHelper
        public Map<String, Map<String, String>> getNamedActors() {
            return SystemConfig.getInstance().getNamedActors();
        }

        @Override // com.android.server.om.PackageManagerHelper
        public boolean signaturesMatching(String packageName1, String packageName2, int userId) {
            try {
                return this.mPackageManager.checkSignatures(packageName1, packageName2) == 0;
            } catch (RemoteException e) {
                return false;
            }
        }

        @Override // com.android.server.om.PackageManagerHelper
        public String getConfigSignaturePackage() {
            String[] pkgs = this.mPackageManagerInternal.getKnownPackageNames(13, 0);
            if (pkgs.length == 0) {
                return null;
            }
            return pkgs[0];
        }

        @Override // com.android.server.om.PackageManagerHelper
        public OverlayableInfo getOverlayableForTarget(String packageName, String targetOverlayableName, int userId) throws IOException {
            AndroidPackage packageInfo = getPackageForUser(packageName, userId);
            if (packageInfo == null) {
                throw new IOException("Unable to get target package");
            }
            ApkAssets apkAssets = null;
            try {
                apkAssets = ApkAssets.loadFromPath(packageInfo.getBaseApkPath());
                return apkAssets.getOverlayableInfo(targetOverlayableName);
            } finally {
                if (apkAssets != null) {
                    try {
                        apkAssets.close();
                    } catch (Throwable th) {
                    }
                }
            }
        }

        @Override // com.android.server.om.PackageManagerHelper
        public boolean doesTargetDefineOverlayable(String targetPackageName, int userId) throws IOException {
            AndroidPackage packageInfo = getPackageForUser(targetPackageName, userId);
            if (packageInfo == null) {
                throw new IOException("Unable to get target package");
            }
            ApkAssets apkAssets = null;
            try {
                apkAssets = ApkAssets.loadFromPath(packageInfo.getBaseApkPath());
                return apkAssets.definesOverlayable();
            } finally {
                if (apkAssets != null) {
                    try {
                        apkAssets.close();
                    } catch (Throwable th) {
                    }
                }
            }
        }

        @Override // com.android.server.om.PackageManagerHelper
        public void enforcePermission(String permission, String message) throws SecurityException {
            this.mContext.enforceCallingOrSelfPermission(permission, message);
        }

        public void forgetAllPackageInfos(int userId) {
            for (int i = this.mCache.size() - 1; i >= 0; i--) {
                removePackageUser(this.mCache.valueAt(i), userId);
            }
        }

        @Override // com.android.server.om.PackageManagerHelper
        public String[] getPackagesForUid(int uid) {
            try {
                return this.mPackageManager.getPackagesForUid(uid);
            } catch (RemoteException e) {
                return null;
            }
        }

        public void dump(PrintWriter pw, DumpState dumpState) {
            pw.println("AndroidPackage cache");
            if (!dumpState.isVerbose()) {
                pw.println(TAB1 + this.mCache.size() + " package(s)");
            } else if (this.mCache.size() == 0) {
                pw.println("    <empty>");
            } else {
                int n = this.mCache.size();
                for (int i = 0; i < n; i++) {
                    String packageName = this.mCache.keyAt(i);
                    AndroidPackageUsers pkg = this.mCache.valueAt(i);
                    pw.print(TAB1 + packageName + ": " + pkg.mPackage + " users=");
                    pw.println(TextUtils.join(", ", pkg.mInstalledUsers));
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateTargetPackagesLocked(PackageAndUser updatedTarget) {
        if (updatedTarget != null) {
            updateTargetPackagesLocked(Set.of(updatedTarget));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateTargetPackagesLocked(Set<PackageAndUser> updatedTargets) {
        if (updatedTargets != null) {
            Log.i(TAG, "[RetestTagOfQyx OMS updateTargetPackagesLocked] updatedTargets=" + updatedTargets);
        }
        if (CollectionUtils.isEmpty(updatedTargets)) {
            return;
        }
        persistSettingsLocked();
        SparseArray<ArraySet<String>> userTargets = groupTargetsByUserId(updatedTargets);
        int n = userTargets.size();
        for (int i = 0; i < n; i++) {
            final ArraySet<String> targets = userTargets.valueAt(i);
            final int userId = userTargets.keyAt(i);
            final List<String> affectedPackages = updatePackageManagerLocked(targets, userId);
            if (!affectedPackages.isEmpty()) {
                FgThread.getHandler().post(new Runnable() { // from class: com.android.server.om.OverlayManagerService$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        OverlayManagerService.this.m5239xf2158d20(affectedPackages, userId, targets);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateTargetPackagesLocked$1$com-android-server-om-OverlayManagerService  reason: not valid java name */
    public /* synthetic */ void m5239xf2158d20(List affectedPackages, int userId, ArraySet targets) {
        updateActivityManager(affectedPackages, userId);
        Log.i(TAG, "[RetestTagOfQyx OMS broadcastActionOverlayChanged] now in handler, going to broadcastActionOverlayChanged, targetIsNull=" + (targets == null) + ", userId=" + userId);
        broadcastActionOverlayChanged(targets, userId);
    }

    private static SparseArray<ArraySet<String>> groupTargetsByUserId(Set<PackageAndUser> targetsAndUsers) {
        final SparseArray<ArraySet<String>> userTargets = new SparseArray<>();
        CollectionUtils.forEach(targetsAndUsers, new FunctionalUtils.ThrowingConsumer() { // from class: com.android.server.om.OverlayManagerService$$ExternalSyntheticLambda2
            public final void acceptOrThrow(Object obj) {
                OverlayManagerService.lambda$groupTargetsByUserId$2(userTargets, (PackageAndUser) obj);
            }
        });
        return userTargets;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$groupTargetsByUserId$2(SparseArray userTargets, PackageAndUser target) throws Exception {
        ArraySet<String> targets = (ArraySet) userTargets.get(target.userId);
        if (targets == null) {
            targets = new ArraySet<>();
            userTargets.put(target.userId, targets);
        }
        targets.add(target.packageName);
    }

    private static void broadcastActionOverlayChanged(Set<String> targetPackages, final int userId) {
        CollectionUtils.forEach(targetPackages, new FunctionalUtils.ThrowingConsumer() { // from class: com.android.server.om.OverlayManagerService$$ExternalSyntheticLambda0
            public final void acceptOrThrow(Object obj) {
                OverlayManagerService.lambda$broadcastActionOverlayChanged$3(userId, (String) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$broadcastActionOverlayChanged$3(int userId, String target) throws Exception {
        Intent intent = new Intent("android.intent.action.OVERLAY_CHANGED", Uri.fromParts("package", target, null));
        intent.setFlags(67108864);
        try {
            ActivityManager.getService().broadcastIntent((IApplicationThread) null, intent, (String) null, (IIntentReceiver) null, 0, (String) null, (Bundle) null, (String[]) null, -1, (Bundle) null, false, false, userId);
        } catch (RemoteException e) {
            Slog.e(TAG, "broadcastActionOverlayChanged remote exception", e);
        }
    }

    private void updateActivityManager(List<String> targetPackageNames, int userId) {
        IActivityManager am = ActivityManager.getService();
        try {
            am.scheduleApplicationInfoChanged(targetPackageNames, userId);
        } catch (RemoteException e) {
            Slog.e(TAG, "updateActivityManager remote exception", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public SparseArray<List<String>> updatePackageManagerLocked(Set<PackageAndUser> targets) {
        if (CollectionUtils.isEmpty(targets)) {
            return new SparseArray<>();
        }
        SparseArray<List<String>> affectedTargets = new SparseArray<>();
        SparseArray<ArraySet<String>> userTargets = groupTargetsByUserId(targets);
        int n = userTargets.size();
        for (int i = 0; i < n; i++) {
            int userId = userTargets.keyAt(i);
            affectedTargets.put(userId, updatePackageManagerLocked(userTargets.valueAt(i), userId));
        }
        return affectedTargets;
    }

    private List<String> updatePackageManagerLocked(Collection<String> targetPackageNames, int userId) {
        PackageManagerInternal pm;
        Collection<String> targetPackageNames2 = targetPackageNames;
        try {
            Trace.traceBegin(67108864L, "OMS#updatePackageManagerLocked " + targetPackageNames2);
            pm = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            boolean updateFrameworkRes = targetPackageNames2.contains(PackageManagerService.PLATFORM_PACKAGE_NAME);
            if (updateFrameworkRes) {
                targetPackageNames2 = pm.getTargetPackageNames(userId);
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            Map<String, OverlayPaths> pendingChanges = new ArrayMap<>(targetPackageNames2.size());
            synchronized (this.mLock) {
                OverlayPaths frameworkOverlays = this.mImpl.getEnabledOverlayPaths(PackageManagerService.PLATFORM_PACKAGE_NAME, userId);
                OverlayPaths frameworkOverlaysExcludeGesture = this.mImpl.getEnabledOverlayPathsExcludeSpRRO(PackageManagerService.PLATFORM_PACKAGE_NAME, userId, "com.android.internal.systemui.navbar.gestural");
                for (String targetPackageName : targetPackageNames2) {
                    OverlayPaths.Builder list = new OverlayPaths.Builder();
                    if (!PackageManagerService.PLATFORM_PACKAGE_NAME.equals(targetPackageName)) {
                        if (!"com.facebook.lite".equals(targetPackageName) && !"com.instagram.lite".equals(targetPackageName)) {
                            list.addAll(frameworkOverlays);
                        }
                        list.addAll(frameworkOverlaysExcludeGesture);
                    }
                    list.addAll(this.mImpl.getEnabledOverlayPaths(targetPackageName, userId));
                    pendingChanges.put(targetPackageName, list.build());
                }
            }
            HashSet<String> updatedPackages = new HashSet<>();
            for (String targetPackageName2 : targetPackageNames2) {
                if (!pm.setEnabledOverlayPackages(userId, targetPackageName2, pendingChanges.get(targetPackageName2), updatedPackages)) {
                    Slog.e(TAG, String.format("Failed to change enabled overlays for %s user %d", targetPackageName2, Integer.valueOf(userId)));
                }
                if (userId != 999 && !pm.setEnabledOverlayPackages(999, targetPackageName2, pendingChanges.get(targetPackageName2), updatedPackages)) {
                    Slog.e(TAG, String.format("Failed to change enabled overlays for %s user %d", targetPackageName2, 999));
                }
            }
            ArrayList arrayList = new ArrayList(updatedPackages);
            Trace.traceEnd(67108864L);
            return arrayList;
        } catch (Throwable th2) {
            th = th2;
            Trace.traceEnd(67108864L);
            throw th;
        }
    }

    private void persistSettingsLocked() {
        FileOutputStream stream = null;
        try {
            stream = this.mSettingsFile.startWrite();
            this.mSettings.persist(stream);
            this.mSettingsFile.finishWrite(stream);
        } catch (IOException | XmlPullParserException e) {
            this.mSettingsFile.failWrite(stream);
            Slog.e(TAG, "failed to persist overlay state", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void restoreSettings() {
        int[] users;
        try {
            Trace.traceBegin(67108864L, "OMS#restoreSettings");
            synchronized (this.mLock) {
                if (!this.mSettingsFile.getBaseFile().exists()) {
                    return;
                }
                try {
                    FileInputStream stream = this.mSettingsFile.openRead();
                    try {
                        this.mSettings.restore(stream);
                        List<UserInfo> liveUsers = this.mUserManager.getUsers(true);
                        int[] liveUserIds = new int[liveUsers.size()];
                        for (int i = 0; i < liveUsers.size(); i++) {
                            liveUserIds[i] = liveUsers.get(i).getUserHandle().getIdentifier();
                        }
                        Arrays.sort(liveUserIds);
                        for (int userId : this.mSettings.getUsers()) {
                            if (Arrays.binarySearch(liveUserIds, userId) < 0) {
                                this.mSettings.removeUser(userId);
                            }
                        }
                        if (stream != null) {
                            stream.close();
                        }
                    } catch (Throwable th) {
                        if (stream != null) {
                            try {
                                stream.close();
                            } catch (Throwable th2) {
                                th.addSuppressed(th2);
                            }
                        }
                        throw th;
                    }
                } catch (IOException | XmlPullParserException e) {
                    Slog.e(TAG, "failed to restore overlay state", e);
                }
            }
        } finally {
            Trace.traceEnd(67108864L);
        }
    }
}
