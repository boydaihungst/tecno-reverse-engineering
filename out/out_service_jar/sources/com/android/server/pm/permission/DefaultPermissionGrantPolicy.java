package com.android.server.pm.permission;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.permission.PermissionManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TypedXmlPullParser;
import android.util.Xml;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.backup.UserBackupManagerService;
import com.android.server.pm.PackageList;
import com.android.server.pm.permission.LegacyPermissionManagerInternal;
import com.mediatek.cta.CtaManager;
import com.mediatek.cta.CtaManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public final class DefaultPermissionGrantPolicy {
    private static final String ACTION_TRACK = "com.android.fitness.TRACK";
    private static final Set<String> ACTIVITY_RECOGNITION_PERMISSIONS;
    private static final Set<String> ALWAYS_LOCATION_PERMISSIONS;
    private static final String ATTR_FIXED = "fixed";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_PACKAGE = "package";
    private static final String ATTR_WHITELISTED = "whitelisted";
    private static final String AUDIO_MIME_TYPE = "audio/mpeg";
    private static final Set<String> CALENDAR_PERMISSIONS;
    private static final Set<String> CAMERA_PERMISSIONS;
    private static final Set<String> COARSE_BACKGROUND_LOCATION_PERMISSIONS;
    private static final Set<String> CONTACTS_PERMISSIONS;
    private static final CtaManager CTA_MANAGER;
    private static final boolean DEBUG = false;
    private static final int DEFAULT_INTENT_QUERY_FLAGS = 794624;
    private static final int DEFAULT_PACKAGE_INFO_QUERY_FLAGS = 536915968;
    private static final Set<String> FOREGROUND_LOCATION_PERMISSIONS;
    private static final String HERE_NLP = "com.here.network.location.provider";
    private static final boolean HERE_NLP_SUPPORT;
    private static final Set<String> MICROPHONE_PERMISSIONS;
    private static final int MSG_READ_DEFAULT_PERMISSION_EXCEPTIONS = 1;
    private static final Set<String> NEARBY_DEVICES_PERMISSIONS;
    private static final Set<String> NOTIFICATION_PERMISSIONS;
    private static final Set<String> PHONE_PERMISSIONS;
    private static final Set<String> SENSORS_PERMISSIONS;
    private static final Set<String> SMS_PERMISSIONS;
    private static final Set<String> STORAGE_PERMISSIONS;
    private static final String TAG = "DefaultPermGrantPolicy";
    private static final String TAG_EXCEPTION = "exception";
    private static final String TAG_EXCEPTIONS = "exceptions";
    private static final String TAG_PERMISSION = "permission";
    private final Context mContext;
    private LegacyPermissionManagerInternal.PackagesProvider mDialerAppPackagesProvider;
    private ArrayMap<String, List<DefaultPermissionGrant>> mGrantExceptions;
    private final Handler mHandler;
    private HerePermissionGrantPolicy mHerePolicy;
    private LegacyPermissionManagerInternal.PackagesProvider mLocationExtraPackagesProvider;
    private LegacyPermissionManagerInternal.PackagesProvider mLocationPackagesProvider;
    private final PackageManagerInternal mServiceInternal;
    private LegacyPermissionManagerInternal.PackagesProvider mSimCallManagerPackagesProvider;
    private LegacyPermissionManagerInternal.PackagesProvider mSmsAppPackagesProvider;
    private LegacyPermissionManagerInternal.SyncAdapterPackagesProvider mSyncAdapterPackagesProvider;
    private LegacyPermissionManagerInternal.PackagesProvider mUseOpenWifiAppPackagesProvider;
    private LegacyPermissionManagerInternal.PackagesProvider mVoiceInteractionPackagesProvider;
    private final Object mLock = new Object();
    private final PackageManagerWrapper NO_PM_CACHE = new PackageManagerWrapper() { // from class: com.android.server.pm.permission.DefaultPermissionGrantPolicy.1
        @Override // com.android.server.pm.permission.DefaultPermissionGrantPolicy.PackageManagerWrapper
        public int getPermissionFlags(String permission, PackageInfo pkg, UserHandle user) {
            return DefaultPermissionGrantPolicy.this.mContext.getPackageManager().getPermissionFlags(permission, pkg.packageName, user);
        }

        @Override // com.android.server.pm.permission.DefaultPermissionGrantPolicy.PackageManagerWrapper
        public void updatePermissionFlags(String permission, PackageInfo pkg, int flagMask, int flagValues, UserHandle user) {
            DefaultPermissionGrantPolicy.this.mContext.getPackageManager().updatePermissionFlags(permission, pkg.packageName, flagMask, flagValues, user);
        }

        @Override // com.android.server.pm.permission.DefaultPermissionGrantPolicy.PackageManagerWrapper
        public void grantPermission(String permission, PackageInfo pkg, UserHandle user) {
            DefaultPermissionGrantPolicy.this.mContext.getPackageManager().grantRuntimePermission(pkg.packageName, permission, user);
        }

        @Override // com.android.server.pm.permission.DefaultPermissionGrantPolicy.PackageManagerWrapper
        public void revokePermission(String permission, PackageInfo pkg, UserHandle user) {
            DefaultPermissionGrantPolicy.this.mContext.getPackageManager().revokeRuntimePermission(pkg.packageName, permission, user);
        }

        @Override // com.android.server.pm.permission.DefaultPermissionGrantPolicy.PackageManagerWrapper
        public boolean isGranted(String permission, PackageInfo pkg, UserHandle user) {
            return DefaultPermissionGrantPolicy.this.mContext.createContextAsUser(user, 0).getPackageManager().checkPermission(permission, pkg.packageName) == 0;
        }

        @Override // com.android.server.pm.permission.DefaultPermissionGrantPolicy.PackageManagerWrapper
        public PermissionInfo getPermissionInfo(String permissionName) {
            if (permissionName == null) {
                return null;
            }
            try {
                return DefaultPermissionGrantPolicy.this.mContext.getPackageManager().getPermissionInfo(permissionName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                Slog.w(DefaultPermissionGrantPolicy.TAG, "Permission not found: " + permissionName);
                return null;
            }
        }

        @Override // com.android.server.pm.permission.DefaultPermissionGrantPolicy.PackageManagerWrapper
        public PackageInfo getPackageInfo(String pkg) {
            if (pkg == null) {
                return null;
            }
            try {
                return DefaultPermissionGrantPolicy.this.mContext.getPackageManager().getPackageInfo(pkg, DefaultPermissionGrantPolicy.DEFAULT_PACKAGE_INFO_QUERY_FLAGS);
            } catch (PackageManager.NameNotFoundException e) {
                Slog.e(DefaultPermissionGrantPolicy.TAG, "Package not found: " + pkg);
                return null;
            }
        }
    };

    static {
        HERE_NLP_SUPPORT = 1 == SystemProperties.getInt("ro.tran_here_nlp_support", 0);
        ArraySet arraySet = new ArraySet();
        PHONE_PERMISSIONS = arraySet;
        arraySet.add("android.permission.READ_PHONE_STATE");
        arraySet.add("android.permission.CALL_PHONE");
        arraySet.add("android.permission.READ_CALL_LOG");
        arraySet.add("android.permission.WRITE_CALL_LOG");
        arraySet.add("com.android.voicemail.permission.ADD_VOICEMAIL");
        arraySet.add("android.permission.USE_SIP");
        arraySet.add("android.permission.PROCESS_OUTGOING_CALLS");
        ArraySet arraySet2 = new ArraySet();
        CONTACTS_PERMISSIONS = arraySet2;
        arraySet2.add("android.permission.READ_CONTACTS");
        arraySet2.add("android.permission.WRITE_CONTACTS");
        arraySet2.add("android.permission.GET_ACCOUNTS");
        ArraySet arraySet3 = new ArraySet();
        ALWAYS_LOCATION_PERMISSIONS = arraySet3;
        arraySet3.add("android.permission.ACCESS_FINE_LOCATION");
        arraySet3.add("android.permission.ACCESS_COARSE_LOCATION");
        arraySet3.add("android.permission.ACCESS_BACKGROUND_LOCATION");
        ArraySet arraySet4 = new ArraySet();
        FOREGROUND_LOCATION_PERMISSIONS = arraySet4;
        arraySet4.add("android.permission.ACCESS_FINE_LOCATION");
        arraySet4.add("android.permission.ACCESS_COARSE_LOCATION");
        ArraySet arraySet5 = new ArraySet();
        COARSE_BACKGROUND_LOCATION_PERMISSIONS = arraySet5;
        arraySet5.add("android.permission.ACCESS_COARSE_LOCATION");
        arraySet5.add("android.permission.ACCESS_BACKGROUND_LOCATION");
        ArraySet arraySet6 = new ArraySet();
        ACTIVITY_RECOGNITION_PERMISSIONS = arraySet6;
        arraySet6.add("android.permission.ACTIVITY_RECOGNITION");
        ArraySet arraySet7 = new ArraySet();
        CALENDAR_PERMISSIONS = arraySet7;
        arraySet7.add("android.permission.READ_CALENDAR");
        arraySet7.add("android.permission.WRITE_CALENDAR");
        ArraySet arraySet8 = new ArraySet();
        SMS_PERMISSIONS = arraySet8;
        arraySet8.add("android.permission.SEND_SMS");
        arraySet8.add("android.permission.RECEIVE_SMS");
        arraySet8.add("android.permission.READ_SMS");
        arraySet8.add("android.permission.RECEIVE_WAP_PUSH");
        arraySet8.add("android.permission.RECEIVE_MMS");
        arraySet8.add("android.permission.READ_CELL_BROADCASTS");
        ArraySet arraySet9 = new ArraySet();
        MICROPHONE_PERMISSIONS = arraySet9;
        arraySet9.add("android.permission.RECORD_AUDIO");
        ArraySet arraySet10 = new ArraySet();
        CAMERA_PERMISSIONS = arraySet10;
        arraySet10.add("android.permission.CAMERA");
        ArraySet arraySet11 = new ArraySet();
        SENSORS_PERMISSIONS = arraySet11;
        arraySet11.add("android.permission.BODY_SENSORS");
        arraySet11.add("android.permission.BODY_SENSORS_BACKGROUND");
        ArraySet arraySet12 = new ArraySet();
        STORAGE_PERMISSIONS = arraySet12;
        arraySet12.add("android.permission.READ_EXTERNAL_STORAGE");
        arraySet12.add("android.permission.WRITE_EXTERNAL_STORAGE");
        arraySet12.add("android.permission.ACCESS_MEDIA_LOCATION");
        arraySet12.add("android.permission.READ_MEDIA_AUDIO");
        arraySet12.add("android.permission.READ_MEDIA_VIDEO");
        arraySet12.add("android.permission.READ_MEDIA_IMAGES");
        ArraySet arraySet13 = new ArraySet();
        NEARBY_DEVICES_PERMISSIONS = arraySet13;
        arraySet13.add("android.permission.BLUETOOTH_ADVERTISE");
        arraySet13.add("android.permission.BLUETOOTH_CONNECT");
        arraySet13.add("android.permission.BLUETOOTH_SCAN");
        arraySet13.add("android.permission.UWB_RANGING");
        arraySet13.add("android.permission.NEARBY_WIFI_DEVICES");
        ArraySet arraySet14 = new ArraySet();
        NOTIFICATION_PERMISSIONS = arraySet14;
        arraySet14.add("android.permission.POST_NOTIFICATIONS");
        CtaManager makeCtaManager = CtaManagerFactory.getInstance().makeCtaManager();
        CTA_MANAGER = makeCtaManager;
        if (makeCtaManager.isCtaSupported()) {
            arraySet.add("com.mediatek.permission.CTA_CONFERENCE_CALL");
            arraySet.add("com.mediatek.permission.CTA_CALL_FORWARD");
            arraySet8.add("com.mediatek.permission.CTA_SEND_MMS");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DefaultPermissionGrantPolicy(Context context) {
        this.mContext = context;
        HandlerThread handlerThread = new ServiceThread(TAG, 10, true);
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper()) { // from class: com.android.server.pm.permission.DefaultPermissionGrantPolicy.2
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    synchronized (DefaultPermissionGrantPolicy.this.mLock) {
                        if (DefaultPermissionGrantPolicy.this.mGrantExceptions == null) {
                            DefaultPermissionGrantPolicy defaultPermissionGrantPolicy = DefaultPermissionGrantPolicy.this;
                            defaultPermissionGrantPolicy.mGrantExceptions = defaultPermissionGrantPolicy.readDefaultPermissionExceptionsLocked(defaultPermissionGrantPolicy.NO_PM_CACHE);
                        }
                    }
                    if (DefaultPermissionGrantPolicy.HERE_NLP_SUPPORT) {
                        DefaultPermissionGrantPolicy.this.mHerePolicy.checkOnSystemStart();
                    }
                }
            }
        };
        this.mHandler = handler;
        this.mHerePolicy = new HerePermissionGrantPolicy(context, handler);
        this.mServiceInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
    }

    public void setLocationPackagesProvider(LegacyPermissionManagerInternal.PackagesProvider provider) {
        synchronized (this.mLock) {
            this.mLocationPackagesProvider = provider;
        }
    }

    public void setLocationExtraPackagesProvider(LegacyPermissionManagerInternal.PackagesProvider provider) {
        synchronized (this.mLock) {
            this.mLocationExtraPackagesProvider = provider;
        }
    }

    public void setVoiceInteractionPackagesProvider(LegacyPermissionManagerInternal.PackagesProvider provider) {
        synchronized (this.mLock) {
            this.mVoiceInteractionPackagesProvider = provider;
        }
    }

    public void setSmsAppPackagesProvider(LegacyPermissionManagerInternal.PackagesProvider provider) {
        synchronized (this.mLock) {
            this.mSmsAppPackagesProvider = provider;
        }
    }

    public void setDialerAppPackagesProvider(LegacyPermissionManagerInternal.PackagesProvider provider) {
        synchronized (this.mLock) {
            this.mDialerAppPackagesProvider = provider;
        }
    }

    public void setSimCallManagerPackagesProvider(LegacyPermissionManagerInternal.PackagesProvider provider) {
        synchronized (this.mLock) {
            this.mSimCallManagerPackagesProvider = provider;
        }
    }

    public void setUseOpenWifiAppPackagesProvider(LegacyPermissionManagerInternal.PackagesProvider provider) {
        synchronized (this.mLock) {
            this.mUseOpenWifiAppPackagesProvider = provider;
        }
    }

    public void setSyncAdapterPackagesProvider(LegacyPermissionManagerInternal.SyncAdapterPackagesProvider provider) {
        synchronized (this.mLock) {
            this.mSyncAdapterPackagesProvider = provider;
        }
    }

    public void grantDefaultPermissions(int userId) {
        DelayingPackageManagerCache pm = new DelayingPackageManagerCache();
        grantPermissionsToSysComponentsAndPrivApps(pm, userId);
        grantDefaultSystemHandlerPermissions(pm, userId);
        grantSignatureAppsNotificationPermissions(pm, userId);
        grantDefaultPermissionExceptions(pm, userId);
        if (HERE_NLP_SUPPORT) {
            Log.v(TAG, "grantDefaultPermissions: starting HERE policy");
            this.mHerePolicy.start();
        }
        pm.apply();
    }

    private void grantSignatureAppsNotificationPermissions(PackageManagerWrapper pm, int userId) {
        Log.i(TAG, "Granting Notification permissions to platform signature apps for user " + userId);
        List<PackageInfo> packages = this.mContext.getPackageManager().getInstalledPackagesAsUser(DEFAULT_PACKAGE_INFO_QUERY_FLAGS, 0);
        for (PackageInfo pkg : packages) {
            if (pkg != null && pkg.applicationInfo.isSystemApp() && pkg.applicationInfo.isSignedWithPlatformKey()) {
                grantRuntimePermissionsForSystemPackage(pm, userId, pkg, NOTIFICATION_PERMISSIONS);
            }
        }
    }

    private void grantRuntimePermissionsForSystemPackage(PackageManagerWrapper pm, int userId, PackageInfo pkg) {
        grantRuntimePermissionsForSystemPackage(pm, userId, pkg, null);
    }

    private void grantRuntimePermissionsForSystemPackage(PackageManagerWrapper pm, int userId, PackageInfo pkg, Set<String> filterPermissions) {
        String[] strArr;
        if (ArrayUtils.isEmpty(pkg.requestedPermissions)) {
            return;
        }
        Set<String> permissions = new ArraySet<>();
        for (String permission : pkg.requestedPermissions) {
            PermissionInfo perm = pm.getPermissionInfo(permission);
            if (perm != null && ((filterPermissions == null || filterPermissions.contains(permission)) && perm.isRuntime())) {
                permissions.add(permission);
            }
        }
        if (!permissions.isEmpty()) {
            grantRuntimePermissions(pm, pkg, permissions, true, userId);
        }
    }

    public void scheduleReadDefaultPermissionExceptions() {
        this.mHandler.sendEmptyMessage(1);
    }

    private void grantPermissionsToSysComponentsAndPrivApps(DelayingPackageManagerCache pm, int userId) {
        Log.i(TAG, "Granting permissions to platform components for user " + userId);
        List<PackageInfo> packages = this.mContext.getPackageManager().getInstalledPackagesAsUser(DEFAULT_PACKAGE_INFO_QUERY_FLAGS, 0);
        for (PackageInfo pkg : packages) {
            if (pkg != null) {
                pm.addPackageInfo(pkg.packageName, pkg);
                if (pm.isSysComponentOrPersistentPlatformSignedPrivApp(pkg) && doesPackageSupportRuntimePermissions(pkg) && !ArrayUtils.isEmpty(pkg.requestedPermissions)) {
                    grantRuntimePermissionsForSystemPackage(pm, userId, pkg);
                }
            }
        }
        if (!CTA_MANAGER.isCtaSupported()) {
            for (PackageInfo pkg2 : packages) {
                if (pkg2 != null && doesPackageSupportRuntimePermissions(pkg2) && !ArrayUtils.isEmpty(pkg2.requestedPermissions) && pm.isGranted("android.permission.READ_PRIVILEGED_PHONE_STATE", pkg2, UserHandle.of(userId)) && pm.isGranted("android.permission.READ_PHONE_STATE", pkg2, UserHandle.of(userId)) && !pm.isSysComponentOrPersistentPlatformSignedPrivApp(pkg2)) {
                    pm.updatePermissionFlags("android.permission.READ_PHONE_STATE", pkg2, 16, 0, UserHandle.of(userId));
                }
            }
        }
    }

    @SafeVarargs
    private final void grantIgnoringSystemPackage(PackageManagerWrapper pm, String packageName, int userId, Set<String>... permissionGroups) {
        grantPermissionsToPackage(pm, packageName, userId, true, true, permissionGroups);
    }

    @SafeVarargs
    private final void grantSystemFixedPermissionsToSystemPackage(PackageManagerWrapper pm, String packageName, int userId, Set<String>... permissionGroups) {
        grantPermissionsToSystemPackage(pm, packageName, userId, true, permissionGroups);
    }

    @SafeVarargs
    private final void grantPermissionsToSystemPackage(PackageManagerWrapper pm, String packageName, int userId, Set<String>... permissionGroups) {
        grantPermissionsToSystemPackage(pm, packageName, userId, false, permissionGroups);
    }

    @SafeVarargs
    private final void grantPermissionsToSystemPackage(PackageManagerWrapper pm, String packageName, int userId, boolean systemFixed, Set<String>... permissionGroups) {
        if (!pm.isSystemPackage(packageName)) {
            return;
        }
        grantPermissionsToPackage(pm, pm.getSystemPackageInfo(packageName), userId, systemFixed, false, true, permissionGroups);
    }

    @SafeVarargs
    private final void grantPermissionsToPackage(PackageManagerWrapper pm, String packageName, int userId, boolean ignoreSystemPackage, boolean whitelistRestrictedPermissions, Set<String>... permissionGroups) {
        grantPermissionsToPackage(pm, pm.getPackageInfo(packageName), userId, false, ignoreSystemPackage, whitelistRestrictedPermissions, permissionGroups);
    }

    @SafeVarargs
    private final void grantPermissionsToPackage(PackageManagerWrapper pm, PackageInfo packageInfo, int userId, boolean systemFixed, boolean ignoreSystemPackage, boolean whitelistRestrictedPermissions, Set<String>... permissionGroups) {
        if (packageInfo != null && doesPackageSupportRuntimePermissions(packageInfo)) {
            for (Set<String> permissionGroup : permissionGroups) {
                grantRuntimePermissions(pm, packageInfo, permissionGroup, systemFixed, ignoreSystemPackage, whitelistRestrictedPermissions, userId);
            }
        }
    }

    private void grantDefaultSystemHandlerPermissions(PackageManagerWrapper pm, int userId) {
        LegacyPermissionManagerInternal.PackagesProvider locationPackagesProvider;
        LegacyPermissionManagerInternal.PackagesProvider locationExtraPackagesProvider;
        LegacyPermissionManagerInternal.PackagesProvider voiceInteractionPackagesProvider;
        LegacyPermissionManagerInternal.PackagesProvider smsAppPackagesProvider;
        LegacyPermissionManagerInternal.PackagesProvider dialerAppPackagesProvider;
        LegacyPermissionManagerInternal.PackagesProvider simCallManagerPackagesProvider;
        LegacyPermissionManagerInternal.PackagesProvider useOpenWifiAppPackagesProvider;
        LegacyPermissionManagerInternal.SyncAdapterPackagesProvider syncAdapterPackagesProvider;
        String browserPackage;
        String[] locationPackageNames;
        char c;
        char c2;
        String[] locationExtraPackageNames;
        String[] voiceInteractPackageNames;
        int i;
        char c3;
        String[] knownPackages;
        Intent voiceRecoIntent;
        char c4;
        Log.i(TAG, "Granting permissions to default platform handlers for user " + userId);
        synchronized (this.mLock) {
            locationPackagesProvider = this.mLocationPackagesProvider;
            locationExtraPackagesProvider = this.mLocationExtraPackagesProvider;
            voiceInteractionPackagesProvider = this.mVoiceInteractionPackagesProvider;
            smsAppPackagesProvider = this.mSmsAppPackagesProvider;
            dialerAppPackagesProvider = this.mDialerAppPackagesProvider;
            simCallManagerPackagesProvider = this.mSimCallManagerPackagesProvider;
            useOpenWifiAppPackagesProvider = this.mUseOpenWifiAppPackagesProvider;
            syncAdapterPackagesProvider = this.mSyncAdapterPackagesProvider;
        }
        String[] voiceInteractPackageNames2 = voiceInteractionPackagesProvider != null ? voiceInteractionPackagesProvider.getPackages(userId) : null;
        String[] locationPackageNames2 = locationPackagesProvider != null ? locationPackagesProvider.getPackages(userId) : null;
        String[] locationExtraPackageNames2 = locationExtraPackagesProvider != null ? locationExtraPackagesProvider.getPackages(userId) : null;
        String[] smsAppPackageNames = smsAppPackagesProvider != null ? smsAppPackagesProvider.getPackages(userId) : null;
        String[] dialerAppPackageNames = dialerAppPackagesProvider != null ? dialerAppPackagesProvider.getPackages(userId) : null;
        String[] simCallManagerPackageNames = simCallManagerPackagesProvider != null ? simCallManagerPackagesProvider.getPackages(userId) : null;
        String[] useOpenWifiAppPackageNames = useOpenWifiAppPackagesProvider != null ? useOpenWifiAppPackagesProvider.getPackages(userId) : null;
        String[] contactsSyncAdapterPackages = syncAdapterPackagesProvider != null ? syncAdapterPackagesProvider.getPackages("com.android.contacts", userId) : null;
        String[] calendarSyncAdapterPackages = syncAdapterPackagesProvider != null ? syncAdapterPackagesProvider.getPackages("com.android.calendar", userId) : null;
        String permissionControllerPackageName = this.mContext.getPackageManager().getPermissionControllerPackageName();
        Set<String> set = NOTIFICATION_PERMISSIONS;
        grantSystemFixedPermissionsToSystemPackage(pm, permissionControllerPackageName, userId, set);
        Set<String> set2 = STORAGE_PERMISSIONS;
        grantSystemFixedPermissionsToSystemPackage(pm, (String) ArrayUtils.firstOrNull(getKnownPackages(2, userId)), userId, set2, set);
        String verifier = (String) ArrayUtils.firstOrNull(getKnownPackages(4, userId));
        grantSystemFixedPermissionsToSystemPackage(pm, verifier, userId, set2);
        Set<String> set3 = PHONE_PERMISSIONS;
        grantPermissionsToSystemPackage(pm, verifier, userId, set3, SMS_PERMISSIONS, set);
        String setupWizardPackage = (String) ArrayUtils.firstOrNull(getKnownPackages(1, userId));
        Set<String> set4 = CAMERA_PERMISSIONS;
        grantPermissionsToSystemPackage(pm, setupWizardPackage, userId, set3, CONTACTS_PERMISSIONS, ALWAYS_LOCATION_PERMISSIONS, set4, NEARBY_DEVICES_PERMISSIONS);
        grantSystemFixedPermissionsToSystemPackage(pm, setupWizardPackage, userId, set);
        grantPermissionsToSystemPackage(pm, getDefaultSearchSelectorPackage(), userId, set);
        grantPermissionsToSystemPackage(pm, getDefaultCaptivePortalLoginPackage(), userId, set);
        String defaultSystemHandlerActivityPackage = getDefaultSystemHandlerActivityPackage(pm, "android.media.action.IMAGE_CAPTURE", userId);
        Set<String> set5 = MICROPHONE_PERMISSIONS;
        grantPermissionsToSystemPackage(pm, defaultSystemHandlerActivityPackage, userId, set4, set5, set2);
        grantPermissionsToSystemPackage(pm, getDefaultSystemHandlerActivityPackage(pm, "android.provider.MediaStore.RECORD_SOUND", userId), userId, set5);
        grantSystemFixedPermissionsToSystemPackage(pm, getDefaultProviderAuthorityPackage("media", userId), userId, set2, set);
        grantSystemFixedPermissionsToSystemPackage(pm, getDefaultProviderAuthorityPackage("downloads", userId), userId, set2, set);
        grantSystemFixedPermissionsToSystemPackage(pm, getDefaultSystemHandlerActivityPackage(pm, "android.intent.action.VIEW_DOWNLOADS", userId), userId, set2);
        grantSystemFixedPermissionsToSystemPackage(pm, getDefaultProviderAuthorityPackage("com.android.externalstorage.documents", userId), userId, set2);
        grantSystemFixedPermissionsToSystemPackage(pm, getDefaultSystemHandlerActivityPackage(pm, "android.credentials.INSTALL", userId), userId, set2);
        if (dialerAppPackageNames == null) {
            String dialerPackage = getDefaultSystemHandlerActivityPackage(pm, "android.intent.action.DIAL", userId);
            grantDefaultPermissionsToDefaultSystemDialerApp(pm, dialerPackage, userId);
        } else {
            for (String dialerAppPackageName : dialerAppPackageNames) {
                grantDefaultPermissionsToDefaultSystemDialerApp(pm, dialerAppPackageName, userId);
            }
        }
        if (simCallManagerPackageNames != null) {
            int length = simCallManagerPackageNames.length;
            int i2 = 0;
            while (i2 < length) {
                int i3 = length;
                String simCallManagerPackageName = simCallManagerPackageNames[i2];
                grantDefaultPermissionsToDefaultSystemSimCallManager(pm, simCallManagerPackageName, userId);
                i2++;
                length = i3;
            }
        }
        if (useOpenWifiAppPackageNames != null) {
            int length2 = useOpenWifiAppPackageNames.length;
            int i4 = 0;
            while (i4 < length2) {
                int i5 = length2;
                String useOpenWifiPackageName = useOpenWifiAppPackageNames[i4];
                grantDefaultPermissionsToDefaultSystemUseOpenWifiApp(pm, useOpenWifiPackageName, userId);
                i4++;
                length2 = i5;
            }
        }
        if (smsAppPackageNames == null) {
            String smsPackage = getDefaultSystemHandlerActivityPackageForCategory(pm, "android.intent.category.APP_MESSAGING", userId);
            grantDefaultPermissionsToDefaultSystemSmsApp(pm, smsPackage, userId);
        } else {
            int length3 = smsAppPackageNames.length;
            int i6 = 0;
            while (i6 < length3) {
                int i7 = length3;
                String smsPackage2 = smsAppPackageNames[i6];
                grantDefaultPermissionsToDefaultSystemSmsApp(pm, smsPackage2, userId);
                i6++;
                length3 = i7;
            }
        }
        String defaultSystemHandlerActivityPackage2 = getDefaultSystemHandlerActivityPackage(pm, "android.provider.Telephony.SMS_CB_RECEIVED", userId);
        Set<String> set6 = SMS_PERMISSIONS;
        Set<String> set7 = NOTIFICATION_PERMISSIONS;
        grantSystemFixedPermissionsToSystemPackage(pm, defaultSystemHandlerActivityPackage2, userId, set6, NEARBY_DEVICES_PERMISSIONS, set7);
        grantPermissionsToSystemPackage(pm, getDefaultSystemHandlerServicePackage(pm, "android.provider.Telephony.SMS_CARRIER_PROVISION", userId), userId, set6);
        String defaultSystemHandlerActivityPackageForCategory = getDefaultSystemHandlerActivityPackageForCategory(pm, "android.intent.category.APP_CALENDAR", userId);
        Set<String> set8 = CALENDAR_PERMISSIONS;
        Set<String> set9 = CONTACTS_PERMISSIONS;
        grantPermissionsToSystemPackage(pm, defaultSystemHandlerActivityPackageForCategory, userId, set8, set9, set7);
        String calendarProvider = getDefaultProviderAuthorityPackage("com.android.calendar", userId);
        Set<String> set10 = STORAGE_PERMISSIONS;
        grantPermissionsToSystemPackage(pm, calendarProvider, userId, set9, set10);
        grantSystemFixedPermissionsToSystemPackage(pm, calendarProvider, userId, set8);
        if (calendarSyncAdapterPackages != null) {
            grantPermissionToEachSystemPackage(pm, getHeadlessSyncAdapterPackages(pm, calendarSyncAdapterPackages, userId), userId, set8);
        }
        String defaultSystemHandlerActivityPackageForCategory2 = getDefaultSystemHandlerActivityPackageForCategory(pm, "android.intent.category.APP_CONTACTS", userId);
        Set<String> set11 = PHONE_PERMISSIONS;
        grantPermissionsToSystemPackage(pm, defaultSystemHandlerActivityPackageForCategory2, userId, set9, set11);
        if (contactsSyncAdapterPackages != null) {
            grantPermissionToEachSystemPackage(pm, getHeadlessSyncAdapterPackages(pm, contactsSyncAdapterPackages, userId), userId, set9);
        }
        String contactsProviderPackage = getDefaultProviderAuthorityPackage("com.android.contacts", userId);
        grantSystemFixedPermissionsToSystemPackage(pm, contactsProviderPackage, userId, set9, set11);
        grantPermissionsToSystemPackage(pm, contactsProviderPackage, userId, set10);
        grantPermissionsToSystemPackage(pm, getDefaultSystemHandlerActivityPackage(pm, "android.app.action.PROVISION_MANAGED_DEVICE", userId), userId, set9, set7);
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive", 0)) {
            grantPermissionsToSystemPackage(pm, getDefaultSystemHandlerActivityPackageForCategory(pm, "android.intent.category.APP_MAPS", userId), userId, FOREGROUND_LOCATION_PERMISSIONS);
        }
        grantPermissionsToSystemPackage(pm, getDefaultSystemHandlerActivityPackageForCategory(pm, "android.intent.category.APP_EMAIL", userId), userId, set9, set8);
        String browserPackage2 = (String) ArrayUtils.firstOrNull(getKnownPackages(5, userId));
        if (browserPackage2 == null) {
            String browserPackage3 = getDefaultSystemHandlerActivityPackageForCategory(pm, "android.intent.category.APP_BROWSER", userId);
            if (pm.isSystemPackage(browserPackage3)) {
                browserPackage = browserPackage3;
            } else {
                browserPackage = null;
            }
        } else {
            browserPackage = browserPackage2;
        }
        if (!SystemProperties.get("ro.transsion.default_browser_support", "0").equals("1")) {
            locationExtraPackageNames = locationExtraPackageNames2;
            locationPackageNames = locationPackageNames2;
            voiceInteractPackageNames = voiceInteractPackageNames2;
            c = 4;
            c2 = 5;
            grantPermissionsToPackage(pm, browserPackage, userId, false, true, FOREGROUND_LOCATION_PERMISSIONS);
        } else {
            locationPackageNames = locationPackageNames2;
            c = 4;
            c2 = 5;
            locationExtraPackageNames = locationExtraPackageNames2;
            voiceInteractPackageNames = voiceInteractPackageNames2;
        }
        int i8 = 8;
        if (voiceInteractPackageNames == null) {
            i = 3;
        } else {
            int length4 = voiceInteractPackageNames.length;
            int i9 = 0;
            while (i9 < length4) {
                String voiceInteractPackageName = voiceInteractPackageNames[i9];
                Set<String>[] setArr = new Set[i8];
                setArr[0] = CONTACTS_PERMISSIONS;
                setArr[1] = CALENDAR_PERMISSIONS;
                setArr[2] = MICROPHONE_PERMISSIONS;
                setArr[3] = PHONE_PERMISSIONS;
                setArr[c] = SMS_PERMISSIONS;
                setArr[c2] = ALWAYS_LOCATION_PERMISSIONS;
                setArr[6] = NEARBY_DEVICES_PERMISSIONS;
                setArr[7] = NOTIFICATION_PERMISSIONS;
                grantPermissionsToSystemPackage(pm, voiceInteractPackageName, userId, setArr);
                i9++;
                i8 = 8;
            }
            i = 3;
        }
        if (ActivityManager.isLowRamDeviceStatic()) {
            String defaultSystemHandlerActivityPackage3 = getDefaultSystemHandlerActivityPackage(pm, "android.search.action.GLOBAL_SEARCH", userId);
            Set<String>[] setArr2 = new Set[i];
            setArr2[0] = MICROPHONE_PERMISSIONS;
            setArr2[1] = ALWAYS_LOCATION_PERMISSIONS;
            setArr2[2] = NOTIFICATION_PERMISSIONS;
            grantPermissionsToSystemPackage(pm, defaultSystemHandlerActivityPackage3, userId, setArr2);
        }
        Intent voiceRecoIntent2 = new Intent("android.speech.RecognitionService").addCategory("android.intent.category.DEFAULT");
        grantPermissionsToSystemPackage(pm, getDefaultSystemHandlerServicePackage(pm, voiceRecoIntent2, userId), userId, MICROPHONE_PERMISSIONS);
        String[] locationPackageNames3 = locationPackageNames;
        if (locationPackageNames3 != null) {
            int length5 = locationPackageNames3.length;
            int i10 = 0;
            while (i10 < length5) {
                String packageName = locationPackageNames3[i10];
                if (!HERE_NLP_SUPPORT || !HERE_NLP.equals(packageName)) {
                    voiceRecoIntent = voiceRecoIntent2;
                    Set<String>[] setArr3 = new Set[10];
                    setArr3[0] = CONTACTS_PERMISSIONS;
                    setArr3[1] = CALENDAR_PERMISSIONS;
                    setArr3[2] = MICROPHONE_PERMISSIONS;
                    setArr3[3] = PHONE_PERMISSIONS;
                    setArr3[c] = SMS_PERMISSIONS;
                    setArr3[c2] = CAMERA_PERMISSIONS;
                    setArr3[6] = SENSORS_PERMISSIONS;
                    c4 = 7;
                    setArr3[7] = STORAGE_PERMISSIONS;
                    setArr3[8] = NEARBY_DEVICES_PERMISSIONS;
                    setArr3[9] = NOTIFICATION_PERMISSIONS;
                    grantPermissionsToSystemPackage(pm, packageName, userId, setArr3);
                    grantSystemFixedPermissionsToSystemPackage(pm, packageName, userId, ALWAYS_LOCATION_PERMISSIONS, ACTIVITY_RECOGNITION_PERMISSIONS);
                } else {
                    voiceRecoIntent = voiceRecoIntent2;
                    Log.i(TAG, "Skipping location package: " + packageName);
                    c4 = 7;
                }
                i10++;
                voiceRecoIntent2 = voiceRecoIntent;
            }
        }
        if (locationExtraPackageNames != null) {
            for (String packageName2 : locationExtraPackageNames) {
                grantPermissionsToSystemPackage(pm, packageName2, userId, ALWAYS_LOCATION_PERMISSIONS, NEARBY_DEVICES_PERMISSIONS);
                grantSystemFixedPermissionsToSystemPackage(pm, packageName2, userId, ACTIVITY_RECOGNITION_PERMISSIONS);
            }
        }
        Intent musicIntent = new Intent("android.intent.action.VIEW").addCategory("android.intent.category.DEFAULT").setDataAndType(Uri.fromFile(new File("foo.mp3")), AUDIO_MIME_TYPE);
        String defaultSystemHandlerActivityPackage4 = getDefaultSystemHandlerActivityPackage(pm, musicIntent, userId);
        Set<String> set12 = STORAGE_PERMISSIONS;
        grantPermissionsToSystemPackage(pm, defaultSystemHandlerActivityPackage4, userId, set12);
        Intent homeIntent = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME").addCategory("android.intent.category.LAUNCHER_APP");
        String defaultSystemHandlerActivityPackage5 = getDefaultSystemHandlerActivityPackage(pm, homeIntent, userId);
        Set<String> set13 = ALWAYS_LOCATION_PERMISSIONS;
        Set<String> set14 = NOTIFICATION_PERMISSIONS;
        grantPermissionsToSystemPackage(pm, defaultSystemHandlerActivityPackage5, userId, set13, set14);
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.watch", 0)) {
            String wearPackage = getDefaultSystemHandlerActivityPackageForCategory(pm, "android.intent.category.HOME_MAIN", userId);
            grantPermissionsToSystemPackage(pm, wearPackage, userId, CONTACTS_PERMISSIONS, MICROPHONE_PERMISSIONS, set13);
            grantSystemFixedPermissionsToSystemPackage(pm, wearPackage, userId, PHONE_PERMISSIONS);
            if (this.mContext.getResources().getBoolean(17891801)) {
                Log.d(TAG, "Wear: Skipping permission grant for Default fitness tracker app : " + wearPackage);
                c3 = 0;
            } else {
                c3 = 0;
                grantPermissionsToSystemPackage(pm, getDefaultSystemHandlerActivityPackage(pm, ACTION_TRACK, userId), userId, SENSORS_PERMISSIONS);
            }
        } else {
            c3 = 0;
        }
        Set<String>[] setArr4 = new Set[2];
        setArr4[c3] = set13;
        setArr4[1] = set14;
        grantSystemFixedPermissionsToSystemPackage(pm, "com.android.printspooler", userId, setArr4);
        String defaultSystemHandlerActivityPackage6 = getDefaultSystemHandlerActivityPackage(pm, "android.telephony.action.EMERGENCY_ASSISTANCE", userId);
        Set<String>[] setArr5 = new Set[2];
        Set<String> set15 = CONTACTS_PERMISSIONS;
        setArr5[c3] = set15;
        Set<String> set16 = PHONE_PERMISSIONS;
        setArr5[1] = set16;
        grantSystemFixedPermissionsToSystemPackage(pm, defaultSystemHandlerActivityPackage6, userId, setArr5);
        Intent nfcTagIntent = new Intent("android.intent.action.VIEW").setType("vnd.android.cursor.item/ndef_msg");
        grantPermissionsToSystemPackage(pm, getDefaultSystemHandlerActivityPackage(pm, nfcTagIntent, userId), userId, set15, set16);
        grantSystemFixedPermissionsToSystemPackage(pm, getDefaultSystemHandlerActivityPackage(pm, "android.os.storage.action.MANAGE_STORAGE", userId), userId, set12);
        grantSystemFixedPermissionsToSystemPackage(pm, "com.android.companiondevicemanager", userId, set13, NEARBY_DEVICES_PERMISSIONS);
        grantSystemFixedPermissionsToSystemPackage(pm, getDefaultSystemHandlerActivityPackage(pm, "android.intent.action.RINGTONE_PICKER", userId), userId, set12);
        for (String textClassifierPackage : getKnownPackages(6, userId)) {
            grantPermissionsToSystemPackage(pm, textClassifierPackage, userId, COARSE_BACKGROUND_LOCATION_PERMISSIONS, CONTACTS_PERMISSIONS);
        }
        grantSystemFixedPermissionsToSystemPackage(pm, UserBackupManagerService.SHARED_BACKUP_AGENT_PACKAGE, userId, STORAGE_PERMISSIONS);
        grantSystemFixedPermissionsToSystemPackage(pm, "com.android.bluetoothmidiservice", userId, NEARBY_DEVICES_PERMISSIONS);
        grantPermissionsToSystemPackage(pm, getDefaultSystemHandlerServicePackage(pm, "android.adservices.AD_SERVICES_COMMON_SERVICE", userId), userId, NOTIFICATION_PERMISSIONS);
    }

    private String getDefaultSystemHandlerActivityPackageForCategory(PackageManagerWrapper pm, String category, int userId) {
        return getDefaultSystemHandlerActivityPackage(pm, new Intent("android.intent.action.MAIN").addCategory(category), userId);
    }

    private String getDefaultSearchSelectorPackage() {
        return this.mContext.getString(17039941);
    }

    private String getDefaultCaptivePortalLoginPackage() {
        return this.mContext.getString(17039924);
    }

    @SafeVarargs
    private final void grantPermissionToEachSystemPackage(PackageManagerWrapper pm, ArrayList<String> packages, int userId, Set<String>... permissions) {
        if (packages == null) {
            return;
        }
        int count = packages.size();
        for (int i = 0; i < count; i++) {
            grantPermissionsToSystemPackage(pm, packages.get(i), userId, permissions);
        }
    }

    private String[] getKnownPackages(int knownPkgId, int userId) {
        return this.mServiceInternal.getKnownPackageNames(knownPkgId, userId);
    }

    private void grantDefaultPermissionsToDefaultSystemDialerApp(PackageManagerWrapper pm, String dialerPackage, int userId) {
        if (dialerPackage == null) {
            return;
        }
        boolean isPhonePermFixed = this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.watch", 0);
        if (isPhonePermFixed) {
            grantSystemFixedPermissionsToSystemPackage(pm, dialerPackage, userId, PHONE_PERMISSIONS, NOTIFICATION_PERMISSIONS);
        } else {
            grantPermissionsToSystemPackage(pm, dialerPackage, userId, PHONE_PERMISSIONS);
        }
        grantPermissionsToSystemPackage(pm, dialerPackage, userId, CONTACTS_PERMISSIONS, SMS_PERMISSIONS, MICROPHONE_PERMISSIONS, CAMERA_PERMISSIONS, NOTIFICATION_PERMISSIONS);
        boolean isAndroidAutomotive = this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive", 0);
        if (isAndroidAutomotive) {
            grantPermissionsToSystemPackage(pm, dialerPackage, userId, NEARBY_DEVICES_PERMISSIONS);
        }
    }

    private void grantDefaultPermissionsToDefaultSystemSmsApp(PackageManagerWrapper pm, String smsPackage, int userId) {
        grantPermissionsToSystemPackage(pm, smsPackage, userId, PHONE_PERMISSIONS, CONTACTS_PERMISSIONS, SMS_PERMISSIONS, STORAGE_PERMISSIONS, MICROPHONE_PERMISSIONS, CAMERA_PERMISSIONS, NOTIFICATION_PERMISSIONS);
    }

    private void grantDefaultPermissionsToDefaultSystemUseOpenWifiApp(PackageManagerWrapper pm, String useOpenWifiPackage, int userId) {
        grantPermissionsToSystemPackage(pm, useOpenWifiPackage, userId, ALWAYS_LOCATION_PERMISSIONS);
    }

    public void grantDefaultPermissionsToDefaultUseOpenWifiApp(String packageName, int userId) {
        Log.i(TAG, "Granting permissions to default Use Open WiFi app for user:" + userId);
        grantIgnoringSystemPackage(this.NO_PM_CACHE, packageName, userId, ALWAYS_LOCATION_PERMISSIONS);
    }

    public void grantDefaultPermissionsToDefaultSimCallManager(String packageName, int userId) {
        grantDefaultPermissionsToDefaultSimCallManager(this.NO_PM_CACHE, packageName, userId);
    }

    private void grantDefaultPermissionsToDefaultSimCallManager(PackageManagerWrapper pm, String packageName, int userId) {
        if (packageName == null) {
            return;
        }
        Log.i(TAG, "Granting permissions to sim call manager for user:" + userId);
        grantPermissionsToPackage(pm, packageName, userId, false, true, PHONE_PERMISSIONS, MICROPHONE_PERMISSIONS);
    }

    private void grantDefaultPermissionsToDefaultSystemSimCallManager(PackageManagerWrapper pm, String packageName, int userId) {
        if (pm.isSystemPackage(packageName)) {
            grantDefaultPermissionsToDefaultSimCallManager(pm, packageName, userId);
        }
    }

    public void grantDefaultPermissionsToEnabledCarrierApps(String[] packageNames, int userId) {
        Log.i(TAG, "Granting permissions to enabled carrier apps for user:" + userId);
        if (packageNames == null) {
            return;
        }
        for (String packageName : packageNames) {
            grantPermissionsToSystemPackage(this.NO_PM_CACHE, packageName, userId, PHONE_PERMISSIONS, ALWAYS_LOCATION_PERMISSIONS, SMS_PERMISSIONS);
        }
    }

    public void grantDefaultPermissionsToEnabledImsServices(String[] packageNames, int userId) {
        Log.i(TAG, "Granting permissions to enabled ImsServices for user:" + userId);
        if (packageNames == null) {
            return;
        }
        for (String packageName : packageNames) {
            grantPermissionsToSystemPackage(this.NO_PM_CACHE, packageName, userId, PHONE_PERMISSIONS, MICROPHONE_PERMISSIONS, ALWAYS_LOCATION_PERMISSIONS, CAMERA_PERMISSIONS, CONTACTS_PERMISSIONS);
        }
    }

    public void grantDefaultPermissionsToEnabledTelephonyDataServices(String[] packageNames, int userId) {
        Log.i(TAG, "Granting permissions to enabled data services for user:" + userId);
        if (packageNames == null) {
            return;
        }
        for (String packageName : packageNames) {
            grantSystemFixedPermissionsToSystemPackage(this.NO_PM_CACHE, packageName, userId, PHONE_PERMISSIONS, ALWAYS_LOCATION_PERMISSIONS);
        }
    }

    public void revokeDefaultPermissionsFromDisabledTelephonyDataServices(String[] packageNames, int userId) {
        Log.i(TAG, "Revoking permissions from disabled data services for user:" + userId);
        if (packageNames == null) {
            return;
        }
        for (String packageName : packageNames) {
            PackageInfo pkg = this.NO_PM_CACHE.getSystemPackageInfo(packageName);
            if (this.NO_PM_CACHE.isSystemPackage(pkg) && doesPackageSupportRuntimePermissions(pkg)) {
                revokeRuntimePermissions(this.NO_PM_CACHE, packageName, PHONE_PERMISSIONS, true, userId);
                revokeRuntimePermissions(this.NO_PM_CACHE, packageName, ALWAYS_LOCATION_PERMISSIONS, true, userId);
            }
        }
    }

    public void grantDefaultPermissionsToActiveLuiApp(String packageName, int userId) {
        Log.i(TAG, "Granting permissions to active LUI app for user:" + userId);
        grantSystemFixedPermissionsToSystemPackage(this.NO_PM_CACHE, packageName, userId, CAMERA_PERMISSIONS);
    }

    public void revokeDefaultPermissionsFromLuiApps(String[] packageNames, int userId) {
        Log.i(TAG, "Revoke permissions from LUI apps for user:" + userId);
        if (packageNames == null) {
            return;
        }
        for (String packageName : packageNames) {
            PackageInfo pkg = this.NO_PM_CACHE.getSystemPackageInfo(packageName);
            if (this.NO_PM_CACHE.isSystemPackage(pkg) && doesPackageSupportRuntimePermissions(pkg)) {
                revokeRuntimePermissions(this.NO_PM_CACHE, packageName, CAMERA_PERMISSIONS, true, userId);
            }
        }
    }

    public void grantDefaultPermissionsToCarrierServiceApp(String packageName, int userId) {
        Log.i(TAG, "Grant permissions to Carrier Service app " + packageName + " for user:" + userId);
        grantPermissionsToPackage(this.NO_PM_CACHE, packageName, userId, false, true, NOTIFICATION_PERMISSIONS);
    }

    private String getDefaultSystemHandlerActivityPackage(PackageManagerWrapper pm, String intentAction, int userId) {
        return getDefaultSystemHandlerActivityPackage(pm, new Intent(intentAction), userId);
    }

    private String getDefaultSystemHandlerActivityPackage(PackageManagerWrapper pm, Intent intent, int userId) {
        ResolveInfo handler = this.mContext.getPackageManager().resolveActivityAsUser(intent, DEFAULT_INTENT_QUERY_FLAGS, userId);
        if (handler == null || handler.activityInfo == null || this.mServiceInternal.isResolveActivityComponent(handler.activityInfo)) {
            return null;
        }
        String packageName = handler.activityInfo.packageName;
        if (pm.isSystemPackage(packageName)) {
            return packageName;
        }
        return null;
    }

    private String getDefaultSystemHandlerServicePackage(PackageManagerWrapper pm, String intentAction, int userId) {
        return getDefaultSystemHandlerServicePackage(pm, new Intent(intentAction), userId);
    }

    private String getDefaultSystemHandlerServicePackage(PackageManagerWrapper pm, Intent intent, int userId) {
        List<ResolveInfo> handlers = this.mContext.getPackageManager().queryIntentServicesAsUser(intent, DEFAULT_INTENT_QUERY_FLAGS, userId);
        if (handlers == null) {
            return null;
        }
        int handlerCount = handlers.size();
        for (int i = 0; i < handlerCount; i++) {
            ResolveInfo handler = handlers.get(i);
            String handlerPackage = handler.serviceInfo.packageName;
            if (pm.isSystemPackage(handlerPackage)) {
                return handlerPackage;
            }
        }
        return null;
    }

    private ArrayList<String> getHeadlessSyncAdapterPackages(PackageManagerWrapper pm, String[] syncAdapterPackageNames, int userId) {
        ArrayList<String> syncAdapterPackages = new ArrayList<>();
        Intent homeIntent = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.LAUNCHER");
        for (String syncAdapterPackageName : syncAdapterPackageNames) {
            homeIntent.setPackage(syncAdapterPackageName);
            ResolveInfo homeActivity = this.mContext.getPackageManager().resolveActivityAsUser(homeIntent, DEFAULT_INTENT_QUERY_FLAGS, userId);
            if (homeActivity == null && pm.isSystemPackage(syncAdapterPackageName)) {
                syncAdapterPackages.add(syncAdapterPackageName);
            }
        }
        return syncAdapterPackages;
    }

    private String getDefaultProviderAuthorityPackage(String authority, int userId) {
        ProviderInfo provider = this.mContext.getPackageManager().resolveContentProviderAsUser(authority, DEFAULT_INTENT_QUERY_FLAGS, userId);
        if (provider != null) {
            return provider.packageName;
        }
        return null;
    }

    private void grantRuntimePermissions(PackageManagerWrapper pm, PackageInfo pkg, Set<String> permissions, boolean systemFixed, int userId) {
        grantRuntimePermissions(pm, pkg, permissions, systemFixed, false, true, userId);
    }

    private void revokeRuntimePermissions(PackageManagerWrapper pm, String packageName, Set<String> permissions, boolean systemFixed, int userId) {
        PackageInfo pkg = pm.getSystemPackageInfo(packageName);
        if (pkg == null || ArrayUtils.isEmpty(pkg.requestedPermissions)) {
            return;
        }
        Set<String> revokablePermissions = new ArraySet<>(Arrays.asList(pkg.requestedPermissions));
        for (String permission : permissions) {
            if (revokablePermissions.contains(permission)) {
                UserHandle user = UserHandle.of(userId);
                int flags = pm.getPermissionFlags(permission, pm.getPackageInfo(packageName), user);
                if ((flags & 32) != 0 && (flags & 4) == 0 && ((flags & 16) == 0 || systemFixed)) {
                    pm.revokePermission(permission, pkg, user);
                    pm.updatePermissionFlags(permission, pkg, 32, 0, user);
                }
            }
        }
    }

    private boolean isFixedOrUserSet(int flags) {
        return (flags & 23) != 0;
    }

    /* JADX WARN: Removed duplicated region for block: B:44:0x00db  */
    /* JADX WARN: Removed duplicated region for block: B:51:0x00fd  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void grantRuntimePermissions(PackageManagerWrapper pm, PackageInfo pkg, Set<String> permissionsWithoutSplits, boolean systemFixed, boolean ignoreSystemPackage, boolean whitelistRestrictedPermissions, int userId) {
        String[] requestedPermissions;
        Set<String> grantablePermissions;
        int numRequestedPermissions;
        int numForeground;
        int requestedPermissionNum;
        String[] sortedRequestedPermissions;
        int numRequestedPermissions2;
        Set<String> grantablePermissions2;
        String[] requestedPermissions2;
        int requestedPermissionNum2;
        String permission;
        int flags;
        String permission2;
        PackageInfo disabledPkg;
        int newFlags;
        UserHandle user = UserHandle.of(userId);
        if (pkg == null) {
            return;
        }
        String[] requestedPermissions3 = pkg.requestedPermissions;
        if (ArrayUtils.isEmpty(requestedPermissions3)) {
            return;
        }
        String[] requestedByNonSystemPackage = pm.getPackageInfo(pkg.packageName).requestedPermissions;
        int size = requestedPermissions3.length;
        for (int i = 0; i < size; i++) {
            if (!ArrayUtils.contains(requestedByNonSystemPackage, requestedPermissions3[i])) {
                requestedPermissions3[i] = null;
            }
        }
        String[] requestedPermissions4 = (String[]) ArrayUtils.filterNotNull(requestedPermissions3, new IntFunction() { // from class: com.android.server.pm.permission.DefaultPermissionGrantPolicy$$ExternalSyntheticLambda0
            @Override // java.util.function.IntFunction
            public final Object apply(int i2) {
                return DefaultPermissionGrantPolicy.lambda$grantRuntimePermissions$0(i2);
            }
        });
        ArraySet<String> permissions = new ArraySet<>(permissionsWithoutSplits);
        ApplicationInfo applicationInfo = pkg.applicationInfo;
        int newFlags2 = 32;
        if (systemFixed) {
            newFlags2 = 32 | 16;
        }
        List<PermissionManager.SplitPermissionInfo> splitPermissions = ((PermissionManager) this.mContext.getSystemService(PermissionManager.class)).getSplitPermissions();
        int numSplitPerms = splitPermissions.size();
        int splitPermNum = 0;
        while (splitPermNum < numSplitPerms) {
            PermissionManager.SplitPermissionInfo splitPerm = splitPermissions.get(splitPermNum);
            if (applicationInfo == null) {
                newFlags = newFlags2;
            } else {
                int i2 = applicationInfo.targetSdkVersion;
                newFlags = newFlags2;
                int newFlags3 = splitPerm.getTargetSdk();
                if (i2 < newFlags3 && permissionsWithoutSplits.contains(splitPerm.getSplitPermission())) {
                    permissions.addAll(splitPerm.getNewPermissions());
                }
            }
            splitPermNum++;
            newFlags2 = newFlags;
        }
        int newFlags4 = newFlags2;
        if (!ignoreSystemPackage && applicationInfo != null && applicationInfo.isUpdatedSystemApp() && (disabledPkg = pm.getSystemPackageInfo(this.mServiceInternal.getDisabledSystemPackageName(pkg.packageName))) != null) {
            if (ArrayUtils.isEmpty(disabledPkg.requestedPermissions)) {
                return;
            }
            if (!Arrays.equals(requestedPermissions4, disabledPkg.requestedPermissions)) {
                grantablePermissions = new ArraySet<>(Arrays.asList(requestedPermissions4));
                requestedPermissions = disabledPkg.requestedPermissions;
                numRequestedPermissions = requestedPermissions.length;
                String[] sortedRequestedPermissions2 = new String[numRequestedPermissions];
                int numForeground2 = 0;
                numForeground = 0;
                int numOther = 0;
                while (numForeground < numRequestedPermissions) {
                    int numSplitPerms2 = numSplitPerms;
                    String permission3 = requestedPermissions[numForeground];
                    if (pm.getBackgroundPermission(permission3) != null) {
                        sortedRequestedPermissions2[numForeground2] = permission3;
                        numForeground2++;
                    } else {
                        sortedRequestedPermissions2[(numRequestedPermissions - 1) - numOther] = permission3;
                        numOther++;
                    }
                    numForeground++;
                    numSplitPerms = numSplitPerms2;
                }
                requestedPermissionNum = 0;
                while (requestedPermissionNum < numRequestedPermissions) {
                    String permission4 = requestedPermissions[requestedPermissionNum];
                    if (grantablePermissions != null && !grantablePermissions.contains(permission4)) {
                        sortedRequestedPermissions = sortedRequestedPermissions2;
                        numRequestedPermissions2 = numRequestedPermissions;
                        grantablePermissions2 = grantablePermissions;
                        requestedPermissions2 = requestedPermissions;
                        requestedPermissionNum2 = requestedPermissionNum;
                    } else if (permissions.contains(permission4)) {
                        requestedPermissions2 = requestedPermissions;
                        int flags2 = pm.getPermissionFlags(permission4, pkg, user);
                        if (HERE_NLP_SUPPORT) {
                            sortedRequestedPermissions = sortedRequestedPermissions2;
                            if (ALWAYS_LOCATION_PERMISSIONS.contains(permission4)) {
                                numRequestedPermissions2 = numRequestedPermissions;
                                if (HERE_NLP.equals(pkg.packageName)) {
                                    Log.i(TAG, "Skipping: " + permission4 + " for: " + pkg);
                                    grantablePermissions2 = grantablePermissions;
                                    requestedPermissionNum2 = requestedPermissionNum;
                                }
                            } else {
                                numRequestedPermissions2 = numRequestedPermissions;
                            }
                        } else {
                            sortedRequestedPermissions = sortedRequestedPermissions2;
                            numRequestedPermissions2 = numRequestedPermissions;
                        }
                        boolean changingGrantForSystemFixed = systemFixed && (flags2 & 16) != 0;
                        if (isFixedOrUserSet(flags2) && !ignoreSystemPackage && !changingGrantForSystemFixed) {
                            permission2 = permission4;
                            grantablePermissions2 = grantablePermissions;
                            flags = flags2;
                            requestedPermissionNum2 = requestedPermissionNum;
                        } else if ((flags2 & 4) != 0) {
                            grantablePermissions2 = grantablePermissions;
                            requestedPermissionNum2 = requestedPermissionNum;
                        } else {
                            newFlags4 |= flags2 & 14336;
                            if (!whitelistRestrictedPermissions || !pm.isPermissionRestricted(permission4)) {
                                permission = permission4;
                                grantablePermissions2 = grantablePermissions;
                                flags = flags2;
                                requestedPermissionNum2 = requestedPermissionNum;
                            } else {
                                permission = permission4;
                                grantablePermissions2 = grantablePermissions;
                                flags = flags2;
                                requestedPermissionNum2 = requestedPermissionNum;
                                pm.updatePermissionFlags(permission4, pkg, 4096, 4096, user);
                            }
                            if (changingGrantForSystemFixed) {
                                pm.updatePermissionFlags(permission, pkg, flags, flags & (-17), user);
                            }
                            String permission5 = permission;
                            if (!pm.isGranted(permission5, pkg, user)) {
                                pm.grantPermission(permission5, pkg, user);
                            }
                            int flagMask = newFlags4 | 64;
                            permission2 = permission5;
                            pm.updatePermissionFlags(permission5, pkg, flagMask, newFlags4, user);
                        }
                        if ((flags & 32) != 0 && (flags & 16) != 0 && !systemFixed) {
                            pm.updatePermissionFlags(permission2, pkg, 16, 0, user);
                        }
                    } else {
                        sortedRequestedPermissions = sortedRequestedPermissions2;
                        numRequestedPermissions2 = numRequestedPermissions;
                        grantablePermissions2 = grantablePermissions;
                        requestedPermissions2 = requestedPermissions;
                        requestedPermissionNum2 = requestedPermissionNum;
                    }
                    requestedPermissionNum = requestedPermissionNum2 + 1;
                    requestedPermissions = requestedPermissions2;
                    sortedRequestedPermissions2 = sortedRequestedPermissions;
                    numRequestedPermissions = numRequestedPermissions2;
                    grantablePermissions = grantablePermissions2;
                }
            }
        }
        requestedPermissions = requestedPermissions4;
        grantablePermissions = null;
        numRequestedPermissions = requestedPermissions.length;
        String[] sortedRequestedPermissions22 = new String[numRequestedPermissions];
        int numForeground22 = 0;
        numForeground = 0;
        int numOther2 = 0;
        while (numForeground < numRequestedPermissions) {
        }
        requestedPermissionNum = 0;
        while (requestedPermissionNum < numRequestedPermissions) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ String[] lambda$grantRuntimePermissions$0(int x$0) {
        return new String[x$0];
    }

    private void grantDefaultPermissionExceptions(PackageManagerWrapper pm, int userId) {
        int j;
        int permissionGrantCount;
        this.mHandler.removeMessages(1);
        synchronized (this.mLock) {
            if (this.mGrantExceptions == null) {
                this.mGrantExceptions = readDefaultPermissionExceptionsLocked(pm);
            }
        }
        Set<String> permissions = null;
        int exceptionCount = this.mGrantExceptions.size();
        for (int i = 0; i < exceptionCount; i++) {
            String packageName = this.mGrantExceptions.keyAt(i);
            PackageInfo pkg = pm.getSystemPackageInfo(packageName);
            List<DefaultPermissionGrant> permissionGrants = this.mGrantExceptions.valueAt(i);
            int permissionGrantCount2 = permissionGrants.size();
            int j2 = 0;
            while (j2 < permissionGrantCount2) {
                DefaultPermissionGrant permissionGrant = permissionGrants.get(j2);
                if (!pm.isPermissionDangerous(permissionGrant.name)) {
                    Log.w(TAG, "Ignoring permission " + permissionGrant.name + " which isn't dangerous");
                    j = j2;
                    permissionGrantCount = permissionGrantCount2;
                } else {
                    if (permissions == null) {
                        permissions = new ArraySet<>();
                    } else {
                        permissions.clear();
                    }
                    permissions.add(permissionGrant.name);
                    j = j2;
                    permissionGrantCount = permissionGrantCount2;
                    grantRuntimePermissions(pm, pkg, permissions, permissionGrant.fixed, permissionGrant.whitelisted, true, userId);
                }
                j2 = j + 1;
                permissionGrantCount2 = permissionGrantCount;
            }
        }
    }

    private File[] getDefaultPermissionFiles() {
        ArrayList<File> ret = new ArrayList<>();
        File dir = new File(Environment.getRootDirectory(), "etc/default-permissions");
        if (dir.isDirectory() && dir.canRead()) {
            Collections.addAll(ret, dir.listFiles());
        }
        File dir2 = new File(Environment.getVendorDirectory(), "etc/default-permissions");
        if (dir2.isDirectory() && dir2.canRead()) {
            Collections.addAll(ret, dir2.listFiles());
        }
        File dir3 = new File(Environment.getOdmDirectory(), "etc/default-permissions");
        if (dir3.isDirectory() && dir3.canRead()) {
            Collections.addAll(ret, dir3.listFiles());
        }
        File dir4 = new File(Environment.getProductDirectory(), "etc/default-permissions");
        if (dir4.isDirectory() && dir4.canRead()) {
            Collections.addAll(ret, dir4.listFiles());
        }
        File dir5 = new File(Environment.getSystemExtDirectory(), "etc/default-permissions");
        if (dir5.isDirectory() && dir5.canRead()) {
            Collections.addAll(ret, dir5.listFiles());
        }
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.embedded", 0)) {
            File dir6 = new File(Environment.getOemDirectory(), "etc/default-permissions");
            if (dir6.isDirectory() && dir6.canRead()) {
                Collections.addAll(ret, dir6.listFiles());
            }
        }
        if (Build.TRAN_EXTEND_PARTITION_SUPPORT) {
            getTrDefaultPermissionFiles(ret);
        }
        if (ret.isEmpty()) {
            return null;
        }
        return (File[]) ret.toArray(new File[0]);
    }

    private void getTrDefaultPermissionFiles(ArrayList<File> ret) {
        File dir = new File(Environment.getTrProductDirectory(), "etc/default-permissions");
        if (dir.isDirectory() && dir.canRead()) {
            Collections.addAll(ret, dir.listFiles());
        }
        File dir2 = new File(Environment.getTrPreloadDirectory(), "etc/default-permissions");
        if (dir2.isDirectory() && dir2.canRead()) {
            Collections.addAll(ret, dir2.listFiles());
        }
        File dir3 = new File(Environment.getTrCompanyDirectory(), "etc/default-permissions");
        if (dir3.isDirectory() && dir3.canRead()) {
            Collections.addAll(ret, dir3.listFiles());
        }
        File dir4 = new File(Environment.getTrRegionDirectory(), "etc/default-permissions");
        if (dir4.isDirectory() && dir4.canRead()) {
            Collections.addAll(ret, dir4.listFiles());
        }
        File dir5 = new File(Environment.getTrMiDirectory(), "etc/default-permissions");
        if (dir5.isDirectory() && dir5.canRead()) {
            Collections.addAll(ret, dir5.listFiles());
        }
        File dir6 = new File(Environment.getTrCarrierDirectory(), "etc/default-permissions");
        if (dir6.isDirectory() && dir6.canRead()) {
            Collections.addAll(ret, dir6.listFiles());
        }
        File dir7 = new File(Environment.getTrThemeDirectory(), "etc/default-permissions");
        if (dir7.isDirectory() && dir7.canRead()) {
            Collections.addAll(ret, dir7.listFiles());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ArrayMap<String, List<DefaultPermissionGrant>> readDefaultPermissionExceptionsLocked(PackageManagerWrapper pm) {
        File[] files = getDefaultPermissionFiles();
        if (files == null) {
            return new ArrayMap<>(0);
        }
        ArrayMap<String, List<DefaultPermissionGrant>> grantExceptions = new ArrayMap<>();
        for (File file : files) {
            if (!file.getPath().endsWith(".xml")) {
                Slog.i(TAG, "Non-xml file " + file + " in " + file.getParent() + " directory, ignoring");
            } else if (!file.canRead()) {
                Slog.w(TAG, "Default permissions file " + file + " cannot be read");
            } else {
                try {
                    InputStream str = new FileInputStream(file);
                    TypedXmlPullParser parser = Xml.resolvePullParser(str);
                    parse(pm, parser, grantExceptions);
                    str.close();
                } catch (IOException | XmlPullParserException e) {
                    Slog.w(TAG, "Error reading default permissions file " + file, e);
                }
            }
        }
        return grantExceptions;
    }

    private void parse(PackageManagerWrapper pm, TypedXmlPullParser parser, Map<String, List<DefaultPermissionGrant>> outGrantExceptions) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        if (TAG_EXCEPTIONS.equals(parser.getName())) {
                            parseExceptions(pm, parser, outGrantExceptions);
                        } else {
                            Log.e(TAG, "Unknown tag " + parser.getName());
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void parseExceptions(PackageManagerWrapper pm, TypedXmlPullParser parser, Map<String, List<DefaultPermissionGrant>> outGrantExceptions) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        if (TAG_EXCEPTION.equals(parser.getName())) {
                            String packageName = parser.getAttributeValue((String) null, "package");
                            List<DefaultPermissionGrant> packageExceptions = outGrantExceptions.get(packageName);
                            if (packageExceptions == null) {
                                PackageInfo packageInfo = pm.getSystemPackageInfo(packageName);
                                if (packageInfo == null) {
                                    Log.w(TAG, "No such package:" + packageName);
                                    XmlUtils.skipCurrentTag(parser);
                                } else if (!pm.isSystemPackage(packageInfo)) {
                                    Log.w(TAG, "Unknown system package:" + packageName);
                                    XmlUtils.skipCurrentTag(parser);
                                } else if (!doesPackageSupportRuntimePermissions(packageInfo)) {
                                    Log.w(TAG, "Skipping non supporting runtime permissions package:" + packageName);
                                    XmlUtils.skipCurrentTag(parser);
                                } else {
                                    packageExceptions = new ArrayList();
                                    outGrantExceptions.put(packageName, packageExceptions);
                                }
                            }
                            parsePermission(parser, packageExceptions);
                        } else {
                            Log.e(TAG, "Unknown tag " + parser.getName() + "under <exceptions>");
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void parsePermission(TypedXmlPullParser parser, List<DefaultPermissionGrant> outPackageExceptions) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        if ("permission".contains(parser.getName())) {
                            String name = parser.getAttributeValue((String) null, "name");
                            if (name != null) {
                                boolean fixed = parser.getAttributeBoolean((String) null, ATTR_FIXED, false);
                                boolean whitelisted = parser.getAttributeBoolean((String) null, ATTR_WHITELISTED, false);
                                DefaultPermissionGrant exception = new DefaultPermissionGrant(name, fixed, whitelisted);
                                outPackageExceptions.add(exception);
                            } else {
                                Log.w(TAG, "Mandatory name attribute missing for permission tag");
                                XmlUtils.skipCurrentTag(parser);
                            }
                        } else {
                            Log.e(TAG, "Unknown tag " + parser.getName() + "under <exception>");
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private static boolean doesPackageSupportRuntimePermissions(PackageInfo pkg) {
        return pkg.applicationInfo != null && pkg.applicationInfo.targetSdkVersion > 22;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public abstract class PackageManagerWrapper {
        abstract PackageInfo getPackageInfo(String str);

        abstract int getPermissionFlags(String str, PackageInfo packageInfo, UserHandle userHandle);

        abstract PermissionInfo getPermissionInfo(String str);

        abstract void grantPermission(String str, PackageInfo packageInfo, UserHandle userHandle);

        abstract boolean isGranted(String str, PackageInfo packageInfo, UserHandle userHandle);

        abstract void revokePermission(String str, PackageInfo packageInfo, UserHandle userHandle);

        abstract void updatePermissionFlags(String str, PackageInfo packageInfo, int i, int i2, UserHandle userHandle);

        private PackageManagerWrapper() {
        }

        PackageInfo getSystemPackageInfo(String pkg) {
            PackageInfo pi = getPackageInfo(pkg);
            if (pi == null || !pi.applicationInfo.isSystemApp()) {
                return null;
            }
            return pi;
        }

        boolean isPermissionRestricted(String name) {
            PermissionInfo pi = getPermissionInfo(name);
            if (pi == null) {
                return false;
            }
            return pi.isRestricted();
        }

        boolean isPermissionDangerous(String name) {
            PermissionInfo pi = getPermissionInfo(name);
            return pi != null && pi.getProtection() == 1;
        }

        String getBackgroundPermission(String permission) {
            PermissionInfo pi = getPermissionInfo(permission);
            if (pi == null) {
                return null;
            }
            return pi.backgroundPermission;
        }

        boolean isSystemPackage(String packageName) {
            return isSystemPackage(getPackageInfo(packageName));
        }

        boolean isSystemPackage(PackageInfo pkg) {
            return (pkg == null || !pkg.applicationInfo.isSystemApp() || isSysComponentOrPersistentPlatformSignedPrivApp(pkg)) ? false : true;
        }

        boolean isSysComponentOrPersistentPlatformSignedPrivApp(PackageInfo pkg) {
            if (UserHandle.getAppId(pkg.applicationInfo.uid) < 10000) {
                return true;
            }
            if (pkg.applicationInfo.isPrivilegedApp()) {
                PackageInfo disabledPkg = getSystemPackageInfo(DefaultPermissionGrantPolicy.this.mServiceInternal.getDisabledSystemPackageName(pkg.applicationInfo.packageName));
                if (disabledPkg != null) {
                    ApplicationInfo disabledPackageAppInfo = disabledPkg.applicationInfo;
                    if (disabledPackageAppInfo != null && (disabledPackageAppInfo.flags & 8) == 0) {
                        return false;
                    }
                } else if ((pkg.applicationInfo.flags & 8) == 0) {
                    return false;
                }
                return DefaultPermissionGrantPolicy.this.mServiceInternal.isPlatformSigned(pkg.packageName);
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class DelayingPackageManagerCache extends PackageManagerWrapper {
        private SparseArray<ArrayMap<String, PermissionState>> mDelayedPermissionState;
        private ArrayMap<String, PackageInfo> mPackageInfos;
        private ArrayMap<String, PermissionInfo> mPermissionInfos;
        private SparseArray<Context> mUserContexts;

        private DelayingPackageManagerCache() {
            super();
            this.mDelayedPermissionState = new SparseArray<>();
            this.mUserContexts = new SparseArray<>();
            this.mPermissionInfos = new ArrayMap<>();
            this.mPackageInfos = new ArrayMap<>();
        }

        void apply() {
            PackageManager.corkPackageInfoCache();
            for (int uidIdx = 0; uidIdx < this.mDelayedPermissionState.size(); uidIdx++) {
                for (int permIdx = 0; permIdx < this.mDelayedPermissionState.valueAt(uidIdx).size(); permIdx++) {
                    try {
                        this.mDelayedPermissionState.valueAt(uidIdx).valueAt(permIdx).apply();
                    } catch (IllegalArgumentException e) {
                        Slog.w(DefaultPermissionGrantPolicy.TAG, "Cannot set permission " + this.mDelayedPermissionState.valueAt(uidIdx).keyAt(permIdx) + " of uid " + this.mDelayedPermissionState.keyAt(uidIdx), e);
                    }
                }
            }
            PackageManager.uncorkPackageInfoCache();
        }

        void addPackageInfo(String packageName, PackageInfo pkg) {
            this.mPackageInfos.put(packageName, pkg);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Context createContextAsUser(UserHandle user) {
            int index = this.mUserContexts.indexOfKey(user.getIdentifier());
            if (index >= 0) {
                return this.mUserContexts.valueAt(index);
            }
            Context uc = DefaultPermissionGrantPolicy.this.mContext.createContextAsUser(user, 0);
            this.mUserContexts.put(user.getIdentifier(), uc);
            return uc;
        }

        private PermissionState getPermissionState(String permission, PackageInfo pkg, UserHandle user) {
            ArrayMap<String, PermissionState> uidState;
            int uid = UserHandle.getUid(user.getIdentifier(), UserHandle.getAppId(pkg.applicationInfo.uid));
            int uidIdx = this.mDelayedPermissionState.indexOfKey(uid);
            if (uidIdx >= 0) {
                uidState = this.mDelayedPermissionState.valueAt(uidIdx);
            } else {
                uidState = new ArrayMap<>();
                this.mDelayedPermissionState.put(uid, uidState);
            }
            int permIdx = uidState.indexOfKey(permission);
            if (permIdx >= 0) {
                return uidState.valueAt(permIdx);
            }
            PermissionState permState = new PermissionState(permission, pkg, user);
            uidState.put(permission, permState);
            return permState;
        }

        @Override // com.android.server.pm.permission.DefaultPermissionGrantPolicy.PackageManagerWrapper
        public int getPermissionFlags(String permission, PackageInfo pkg, UserHandle user) {
            PermissionState state = getPermissionState(permission, pkg, user);
            state.initFlags();
            return state.newFlags.intValue();
        }

        @Override // com.android.server.pm.permission.DefaultPermissionGrantPolicy.PackageManagerWrapper
        public void updatePermissionFlags(String permission, PackageInfo pkg, int flagMask, int flagValues, UserHandle user) {
            PermissionState state = getPermissionState(permission, pkg, user);
            state.initFlags();
            state.newFlags = Integer.valueOf((state.newFlags.intValue() & (~flagMask)) | (flagValues & flagMask));
        }

        @Override // com.android.server.pm.permission.DefaultPermissionGrantPolicy.PackageManagerWrapper
        public void grantPermission(String permission, PackageInfo pkg, UserHandle user) {
            PermissionState state = getPermissionState(permission, pkg, user);
            state.initGranted();
            state.newGranted = true;
        }

        @Override // com.android.server.pm.permission.DefaultPermissionGrantPolicy.PackageManagerWrapper
        public void revokePermission(String permission, PackageInfo pkg, UserHandle user) {
            PermissionState state = getPermissionState(permission, pkg, user);
            state.initGranted();
            state.newGranted = false;
        }

        @Override // com.android.server.pm.permission.DefaultPermissionGrantPolicy.PackageManagerWrapper
        public boolean isGranted(String permission, PackageInfo pkg, UserHandle user) {
            PermissionState state = getPermissionState(permission, pkg, user);
            state.initGranted();
            return state.newGranted.booleanValue();
        }

        @Override // com.android.server.pm.permission.DefaultPermissionGrantPolicy.PackageManagerWrapper
        public PermissionInfo getPermissionInfo(String permissionName) {
            int index = this.mPermissionInfos.indexOfKey(permissionName);
            if (index >= 0) {
                return this.mPermissionInfos.valueAt(index);
            }
            PermissionInfo pi = DefaultPermissionGrantPolicy.this.NO_PM_CACHE.getPermissionInfo(permissionName);
            this.mPermissionInfos.put(permissionName, pi);
            return pi;
        }

        @Override // com.android.server.pm.permission.DefaultPermissionGrantPolicy.PackageManagerWrapper
        public PackageInfo getPackageInfo(String pkg) {
            int index = this.mPackageInfos.indexOfKey(pkg);
            if (index >= 0) {
                return this.mPackageInfos.valueAt(index);
            }
            PackageInfo pi = DefaultPermissionGrantPolicy.this.NO_PM_CACHE.getPackageInfo(pkg);
            this.mPackageInfos.put(pkg, pi);
            return pi;
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public class PermissionState {
            private Integer mOriginalFlags;
            private Boolean mOriginalGranted;
            private final String mPermission;
            private final PackageInfo mPkgRequestingPerm;
            private final UserHandle mUser;
            Integer newFlags;
            Boolean newGranted;

            private PermissionState(String permission, PackageInfo pkgRequestingPerm, UserHandle user) {
                this.mPermission = permission;
                this.mPkgRequestingPerm = pkgRequestingPerm;
                this.mUser = user;
            }

            void apply() {
                int flagsToRemove;
                int flagsToAdd = 0;
                Integer num = this.newFlags;
                if (num == null) {
                    flagsToRemove = 0;
                } else {
                    flagsToAdd = num.intValue() & (~this.mOriginalFlags.intValue());
                    int flagsToRemove2 = this.mOriginalFlags.intValue() & (~this.newFlags.intValue());
                    flagsToRemove = flagsToRemove2;
                }
                if (flagsToRemove != 0) {
                    DefaultPermissionGrantPolicy.this.NO_PM_CACHE.updatePermissionFlags(this.mPermission, this.mPkgRequestingPerm, flagsToRemove, 0, this.mUser);
                }
                if ((flagsToAdd & 14336) != 0) {
                    int newRestrictionExcemptFlags = flagsToAdd & 14336;
                    DefaultPermissionGrantPolicy.this.NO_PM_CACHE.updatePermissionFlags(this.mPermission, this.mPkgRequestingPerm, newRestrictionExcemptFlags, -1, this.mUser);
                }
                Boolean bool = this.newGranted;
                if (bool != null && bool != this.mOriginalGranted) {
                    if (bool.booleanValue()) {
                        DefaultPermissionGrantPolicy.this.NO_PM_CACHE.grantPermission(this.mPermission, this.mPkgRequestingPerm, this.mUser);
                    } else {
                        DefaultPermissionGrantPolicy.this.NO_PM_CACHE.revokePermission(this.mPermission, this.mPkgRequestingPerm, this.mUser);
                    }
                }
                if ((flagsToAdd & (-14337)) != 0) {
                    int newFlags = flagsToAdd & (-14337);
                    DefaultPermissionGrantPolicy.this.NO_PM_CACHE.updatePermissionFlags(this.mPermission, this.mPkgRequestingPerm, newFlags, -1, this.mUser);
                }
            }

            void initFlags() {
                if (this.newFlags == null) {
                    Integer valueOf = Integer.valueOf(DefaultPermissionGrantPolicy.this.NO_PM_CACHE.getPermissionFlags(this.mPermission, this.mPkgRequestingPerm, this.mUser));
                    this.mOriginalFlags = valueOf;
                    this.newFlags = valueOf;
                }
            }

            void initGranted() {
                if (this.newGranted == null) {
                    Boolean valueOf = Boolean.valueOf(DelayingPackageManagerCache.this.createContextAsUser(this.mUser).getPackageManager().checkPermission(this.mPermission, this.mPkgRequestingPerm.packageName) == 0);
                    this.mOriginalGranted = valueOf;
                    this.newGranted = valueOf;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class DefaultPermissionGrant {
        final boolean fixed;
        final String name;
        final boolean whitelisted;

        public DefaultPermissionGrant(String name, boolean fixed, boolean whitelisted) {
            this.name = name;
            this.fixed = fixed;
            this.whitelisted = whitelisted;
        }
    }

    public void grantCtaPermToPreInstalledPackage(int userId) {
        Log.d(TAG, "grantCtaPermToPreInstalledPackage userId = " + userId);
        synchronized (this.mLock) {
            PackageList packageList = this.mServiceInternal.getPackageList();
            for (String packageName : packageList.getPackageNames()) {
                PackageInfo pi = this.NO_PM_CACHE.getPackageInfo(packageName);
                if (doesPackageSupportRuntimePermissions(pi) && pi.requestedPermissions.length != 0) {
                    Set<String> permissions = new ArraySet<>();
                    for (int i = 0; i < pi.requestedPermissions.length; i++) {
                        String permission = pi.requestedPermissions[i];
                        PermissionInfo bp = this.NO_PM_CACHE.getPermissionInfo(permission);
                        if (bp != null && bp.isRuntime() && CtaManagerFactory.getInstance().makeCtaManager().isCtaOnlyPermission(permission)) {
                            permissions.add(permission);
                        }
                    }
                    if (!permissions.isEmpty()) {
                        PackageManagerWrapper packageManagerWrapper = this.NO_PM_CACHE;
                        grantRuntimePermissions(packageManagerWrapper, pi, permissions, packageManagerWrapper.isSysComponentOrPersistentPlatformSignedPrivApp(pi), userId);
                    }
                }
            }
        }
    }
}
