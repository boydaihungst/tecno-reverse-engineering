package com.android.server.pm.permission;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.os.Binder;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
/* loaded from: classes2.dex */
public final class HerePermissionGrantPolicy {
    private static final String COLLECT_RADIO_SIGNALS = "com.here.consent.COLLECT_RADIO_SIGNALS";
    private static final int DENIED = 0;
    private static final boolean LOG = false;
    private static final int PERMISSION_FLAGS = 769;
    private final Context mContext;
    private final ContentObserver mObserver;
    private final ContentResolver mResolver;
    private UserHandle mUser;
    private static final String TAG = HerePermissionGrantPolicy.class.getSimpleName();
    private static final String[] HERE_POSITIONING_PACKAGES = {"com.here.network.location.provider"};
    private static final String[] ALWAYS_LOCATION_PERMISSIONS = {"android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"};

    public HerePermissionGrantPolicy(Context context, Handler handler) {
        this.mContext = context;
        this.mResolver = context.getContentResolver();
        this.mObserver = new ContentObserver(handler) { // from class: com.android.server.pm.permission.HerePermissionGrantPolicy.1
            Integer mCollectRadioSignalsValue;

            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                Integer value = HerePermissionGrantPolicy.this.getValue(HerePermissionGrantPolicy.COLLECT_RADIO_SIGNALS);
                if (value != this.mCollectRadioSignalsValue) {
                    this.mCollectRadioSignalsValue = value;
                    if (value != null && value.intValue() == 0) {
                        HerePermissionGrantPolicy.this.onResetPermissions();
                    }
                }
            }
        };
    }

    public synchronized void checkOnSystemStart() {
        Context context = this.mContext;
        String[] strArr = HERE_POSITIONING_PACKAGES;
        if (areHerePackagesAvailable(context, strArr)) {
            PackageManager pm = this.mContext.getPackageManager();
            for (String packageName : strArr) {
                String[] strArr2 = ALWAYS_LOCATION_PERMISSIONS;
                int length = strArr2.length;
                int i = 0;
                while (true) {
                    if (i < length) {
                        String permission = strArr2[i];
                        if (pm.checkPermission(permission, packageName) == 0) {
                            i++;
                        } else {
                            setValue(COLLECT_RADIO_SIGNALS, 0);
                            break;
                        }
                    }
                }
            }
        }
    }

    public void start() {
        if (!areHerePackagesAvailable(this.mContext, HERE_POSITIONING_PACKAGES)) {
            return;
        }
        this.mUser = UserHandle.getUserHandleForUid(this.mContext.getUserId());
        try {
            this.mResolver.registerContentObserver(Settings.Secure.getUriFor(COLLECT_RADIO_SIGNALS), false, this.mObserver);
        } catch (SecurityException ex) {
            Log.e(TAG, "start", ex);
        }
    }

    public void stop() {
        if (this.mUser == null) {
            return;
        }
        this.mResolver.unregisterContentObserver(this.mObserver);
        this.mUser = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Integer getValue(String key) {
        try {
            return Integer.valueOf(Settings.Secure.getInt(this.mResolver, key));
        } catch (Settings.SettingNotFoundException e) {
            return null;
        }
    }

    private boolean setValue(String key, int value) {
        try {
            return Settings.Secure.putInt(this.mResolver, key, value);
        } catch (Exception e) {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onResetPermissions() {
        String[] strArr;
        PackageManager pm = this.mContext.getPackageManager();
        for (String packageName : HERE_POSITIONING_PACKAGES) {
            try {
                PackageInfo pkg = pm.getPackageInfo(packageName, 131072);
                resetPermissions(pm, pkg, ALWAYS_LOCATION_PERMISSIONS, this.mUser);
                killUid(pkg.applicationInfo.uid);
            } catch (PackageManager.NameNotFoundException ex) {
                Log.e(TAG, "onResetPermissions: " + packageName, ex);
            }
        }
    }

    private void resetPermissions(PackageManager pm, PackageInfo pkg, String[] permissions, UserHandle user) {
        for (String permission : permissions) {
            pm.updatePermissionFlags(permission, pkg.packageName, 769, 0, user);
            pm.revokeRuntimePermission(pkg.packageName, permission, user);
        }
    }

    private static void killUid(int appId) {
        long identity = Binder.clearCallingIdentity();
        try {
            IActivityManager am = ActivityManager.getService();
            if (am != null) {
                try {
                    am.killUidForPermissionChange(appId, UserHandle.myUserId(), "permissions revoked");
                } catch (RemoteException e) {
                }
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private static boolean areHerePackagesAvailable(Context context, String[] packageNames) {
        PackageManager pm = context.getPackageManager();
        for (String packageName : packageNames) {
            try {
                pm.getPackageInfo(packageName, 131072);
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }
        return true;
    }
}
