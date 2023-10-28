package com.android.server.gpu;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.text.TextUtils;
import android.updatabledriver.UpdatableDriverProto;
import android.util.Base64;
import com.android.framework.protobuf.InvalidProtocolBufferException;
import com.android.server.SystemService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class GpuService extends SystemService {
    private static final int BASE64_FLAGS = 3;
    public static final boolean DEBUG = false;
    private static final String DEV_DRIVER_PROPERTY = "ro.gfx.driver.1";
    private static final String PROD_DRIVER_PROPERTY = "ro.gfx.driver.0";
    public static final String TAG = "GpuService";
    private static final String UPDATABLE_DRIVER_PRODUCTION_ALLOWLIST_FILENAME = "allowlist.txt";
    private ContentResolver mContentResolver;
    private final Context mContext;
    private UpdatableDriverProto.Denylists mDenylists;
    private final String mDevDriverPackageName;
    private DeviceConfigListener mDeviceConfigListener;
    private final Object mDeviceConfigLock;
    private final boolean mHasDevDriver;
    private final boolean mHasProdDriver;
    private final Object mLock;
    private final PackageManager mPackageManager;
    private final String mProdDriverPackageName;
    private long mProdDriverVersionCode;
    private SettingsObserver mSettingsObserver;

    private static native void nSetUpdatableDriverPath(String str);

    public GpuService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mDeviceConfigLock = new Object();
        this.mContext = context;
        String str = SystemProperties.get(PROD_DRIVER_PROPERTY);
        this.mProdDriverPackageName = str;
        this.mProdDriverVersionCode = -1L;
        String str2 = SystemProperties.get(DEV_DRIVER_PROPERTY);
        this.mDevDriverPackageName = str2;
        this.mPackageManager = context.getPackageManager();
        boolean z = !TextUtils.isEmpty(str);
        this.mHasProdDriver = z;
        boolean z2 = !TextUtils.isEmpty(str2);
        this.mHasDevDriver = z2;
        if (z2 || z) {
            IntentFilter packageFilter = new IntentFilter();
            packageFilter.addAction("android.intent.action.PACKAGE_ADDED");
            packageFilter.addAction("android.intent.action.PACKAGE_CHANGED");
            packageFilter.addAction("android.intent.action.PACKAGE_REMOVED");
            packageFilter.addDataScheme("package");
            getContext().registerReceiverAsUser(new PackageReceiver(), UserHandle.ALL, packageFilter, null, null);
        }
    }

    @Override // com.android.server.SystemService
    public void onStart() {
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 1000) {
            this.mContentResolver = this.mContext.getContentResolver();
            if (!this.mHasProdDriver && !this.mHasDevDriver) {
                return;
            }
            this.mSettingsObserver = new SettingsObserver();
            this.mDeviceConfigListener = new DeviceConfigListener();
            fetchProductionDriverPackageProperties();
            processDenylists();
            setDenylist();
            fetchPrereleaseDriverPackageProperties();
        }
    }

    /* loaded from: classes.dex */
    private final class SettingsObserver extends ContentObserver {
        private final Uri mProdDriverDenylistsUri;

        SettingsObserver() {
            super(new Handler());
            Uri uriFor = Settings.Global.getUriFor("updatable_driver_production_denylists");
            this.mProdDriverDenylistsUri = uriFor;
            GpuService.this.mContentResolver.registerContentObserver(uriFor, false, this, -1);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (uri != null && this.mProdDriverDenylistsUri.equals(uri)) {
                GpuService.this.processDenylists();
                GpuService.this.setDenylist();
            }
        }
    }

    /* loaded from: classes.dex */
    private final class DeviceConfigListener implements DeviceConfig.OnPropertiesChangedListener {
        DeviceConfigListener() {
            DeviceConfig.addOnPropertiesChangedListener("game_driver", GpuService.this.mContext.getMainExecutor(), this);
        }

        public void onPropertiesChanged(DeviceConfig.Properties properties) {
            synchronized (GpuService.this.mDeviceConfigLock) {
                if (properties.getKeyset().contains("updatable_driver_production_denylists")) {
                    GpuService.this.parseDenylists(properties.getString("updatable_driver_production_denylists", ""));
                    GpuService.this.setDenylist();
                }
            }
        }
    }

    /* loaded from: classes.dex */
    private final class PackageReceiver extends BroadcastReceiver {
        private PackageReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            Uri data = intent.getData();
            String packageName = data.getSchemeSpecificPart();
            boolean isProdDriver = packageName.equals(GpuService.this.mProdDriverPackageName);
            boolean isDevDriver = packageName.equals(GpuService.this.mDevDriverPackageName);
            if (!isProdDriver && !isDevDriver) {
                return;
            }
            String action = intent.getAction();
            char c = 65535;
            switch (action.hashCode()) {
                case 172491798:
                    if (action.equals("android.intent.action.PACKAGE_CHANGED")) {
                        c = 1;
                        break;
                    }
                    break;
                case 525384130:
                    if (action.equals("android.intent.action.PACKAGE_REMOVED")) {
                        c = 2;
                        break;
                    }
                    break;
                case 1544582882:
                    if (action.equals("android.intent.action.PACKAGE_ADDED")) {
                        c = 0;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                case 1:
                case 2:
                    if (isProdDriver) {
                        GpuService.this.fetchProductionDriverPackageProperties();
                        GpuService.this.setDenylist();
                        return;
                    } else if (isDevDriver) {
                        GpuService.this.fetchPrereleaseDriverPackageProperties();
                        return;
                    } else {
                        return;
                    }
                default:
                    return;
            }
        }
    }

    private static void assetToSettingsGlobal(Context context, Context driverContext, String fileName, String settingsGlobal, CharSequence delimiter) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(driverContext.getAssets().open(fileName)));
            ArrayList<String> assetStrings = new ArrayList<>();
            while (true) {
                String assetString = reader.readLine();
                if (assetString != null) {
                    assetStrings.add(assetString);
                } else {
                    Settings.Global.putString(context.getContentResolver(), settingsGlobal, String.join(delimiter, assetStrings));
                    return;
                }
            }
        } catch (IOException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void fetchProductionDriverPackageProperties() {
        try {
            ApplicationInfo driverInfo = this.mPackageManager.getApplicationInfo(this.mProdDriverPackageName, 1048576);
            if (driverInfo.targetSdkVersion < 26) {
                return;
            }
            Settings.Global.putString(this.mContentResolver, "updatable_driver_production_allowlist", "");
            this.mProdDriverVersionCode = driverInfo.longVersionCode;
            try {
                Context driverContext = this.mContext.createPackageContext(this.mProdDriverPackageName, 4);
                assetToSettingsGlobal(this.mContext, driverContext, UPDATABLE_DRIVER_PRODUCTION_ALLOWLIST_FILENAME, "updatable_driver_production_allowlist", ",");
            } catch (PackageManager.NameNotFoundException e) {
            }
        } catch (PackageManager.NameNotFoundException e2) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void processDenylists() {
        String base64String = DeviceConfig.getProperty("game_driver", "updatable_driver_production_denylists");
        if (base64String == null) {
            base64String = Settings.Global.getString(this.mContentResolver, "updatable_driver_production_denylists");
        }
        parseDenylists(base64String != null ? base64String : "");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void parseDenylists(String base64String) {
        synchronized (this.mLock) {
            this.mDenylists = null;
            try {
                this.mDenylists = UpdatableDriverProto.Denylists.parseFrom(Base64.decode(base64String, 3));
            } catch (IllegalArgumentException e) {
            } catch (InvalidProtocolBufferException e2) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDenylist() {
        Settings.Global.putString(this.mContentResolver, "updatable_driver_production_denylist", "");
        synchronized (this.mLock) {
            UpdatableDriverProto.Denylists denylists = this.mDenylists;
            if (denylists == null) {
                return;
            }
            List<UpdatableDriverProto.Denylist> denylists2 = denylists.getDenylistsList();
            for (UpdatableDriverProto.Denylist denylist : denylists2) {
                if (denylist.getVersionCode() == this.mProdDriverVersionCode) {
                    Settings.Global.putString(this.mContentResolver, "updatable_driver_production_denylist", String.join(",", denylist.getPackageNamesList()));
                    return;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void fetchPrereleaseDriverPackageProperties() {
        try {
            ApplicationInfo driverInfo = this.mPackageManager.getApplicationInfo(this.mDevDriverPackageName, 1048576);
            if (driverInfo.targetSdkVersion < 26) {
                return;
            }
            setUpdatableDriverPath(driverInfo);
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    private void setUpdatableDriverPath(ApplicationInfo ai) {
        if (ai.primaryCpuAbi == null) {
            nSetUpdatableDriverPath("");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(ai.sourceDir).append("!/lib/");
        nSetUpdatableDriverPath(sb.toString());
    }
}
