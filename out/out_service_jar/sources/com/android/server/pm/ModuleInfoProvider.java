package com.android.server.pm;

import android.content.Context;
import android.content.pm.IPackageManager;
import android.content.pm.ModuleInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.util.XmlUtils;
import com.android.server.pm.verify.domain.DomainVerificationLegacySettings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class ModuleInfoProvider {
    private static final String MODULE_METADATA_KEY = "android.content.pm.MODULE_METADATA";
    private static final String TAG = "PackageManager.ModuleInfoProvider";
    private final ApexManager mApexManager;
    private final Context mContext;
    private volatile boolean mMetadataLoaded;
    private final Map<String, ModuleInfo> mModuleInfo;
    private IPackageManager mPackageManager;
    private volatile String mPackageName;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ModuleInfoProvider(Context context) {
        this.mContext = context;
        this.mApexManager = ApexManager.getInstance();
        this.mModuleInfo = new ArrayMap();
    }

    public ModuleInfoProvider(XmlResourceParser metadata, Resources resources, ApexManager apexManager) {
        this.mContext = null;
        this.mApexManager = apexManager;
        this.mModuleInfo = new ArrayMap();
        loadModuleMetadata(metadata, resources);
    }

    private IPackageManager getPackageManager() {
        if (this.mPackageManager == null) {
            this.mPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        }
        return this.mPackageManager;
    }

    public void systemReady() {
        this.mPackageName = this.mContext.getResources().getString(17039929);
        if (TextUtils.isEmpty(this.mPackageName)) {
            Slog.w(TAG, "No configured module metadata provider.");
            return;
        }
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(this.mPackageName, 128L, 0);
            Context packageContext = this.mContext.createPackageContext(this.mPackageName, 0);
            Resources packageResources = packageContext.getResources();
            XmlResourceParser parser = packageResources.getXml(pi.applicationInfo.metaData.getInt(MODULE_METADATA_KEY));
            loadModuleMetadata(parser, packageResources);
        } catch (PackageManager.NameNotFoundException | RemoteException e) {
            Slog.w(TAG, "Unable to discover metadata package: " + this.mPackageName, e);
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:9:0x0020, code lost:
        android.util.Slog.w(com.android.server.pm.ModuleInfoProvider.TAG, "Unexpected metadata element: " + r8.getName());
        r7.mModuleInfo.clear();
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void loadModuleMetadata(XmlResourceParser parser, Resources packageResources) {
        try {
            try {
                XmlUtils.beginDocument(parser, "module-metadata");
                while (true) {
                    XmlUtils.nextElement(parser);
                    if (parser.getEventType() == 1) {
                        break;
                    } else if (!"module".equals(parser.getName())) {
                        break;
                    } else {
                        CharSequence moduleName = packageResources.getText(Integer.parseInt(parser.getAttributeValue(null, "name").substring(1)));
                        String modulePackageName = XmlUtils.readStringAttribute(parser, DomainVerificationLegacySettings.ATTR_PACKAGE_NAME);
                        boolean isHidden = XmlUtils.readBooleanAttribute(parser, "isHidden");
                        ModuleInfo mi = new ModuleInfo();
                        mi.setHidden(isHidden);
                        mi.setPackageName(modulePackageName);
                        mi.setName(moduleName);
                        mi.setApexModuleName(this.mApexManager.getApexModuleNameForPackageName(modulePackageName));
                        this.mModuleInfo.put(modulePackageName, mi);
                    }
                }
            } catch (IOException | XmlPullParserException e) {
                Slog.w(TAG, "Error parsing module metadata", e);
                this.mModuleInfo.clear();
            }
        } finally {
            parser.close();
            this.mMetadataLoaded = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<ModuleInfo> getInstalledModules(int flags) {
        if (!this.mMetadataLoaded) {
            throw new IllegalStateException("Call to getInstalledModules before metadata loaded");
        }
        if ((131072 & flags) != 0) {
            return new ArrayList(this.mModuleInfo.values());
        }
        try {
            List<PackageInfo> allPackages = getPackageManager().getInstalledPackages(1073741824 | flags, UserHandle.getCallingUserId()).getList();
            ArrayList<ModuleInfo> installedModules = new ArrayList<>(allPackages.size());
            for (PackageInfo p : allPackages) {
                ModuleInfo m = this.mModuleInfo.get(p.packageName);
                if (m != null) {
                    installedModules.add(m);
                }
            }
            return installedModules;
        } catch (RemoteException e) {
            Slog.w(TAG, "Unable to retrieve all package names", e);
            return Collections.emptyList();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ModuleInfo getModuleInfo(String name, int flags) {
        if (!this.mMetadataLoaded) {
            throw new IllegalStateException("Call to getModuleInfo before metadata loaded");
        }
        if ((flags & 1) != 0) {
            for (ModuleInfo moduleInfo : this.mModuleInfo.values()) {
                if (name.equals(moduleInfo.getApexModuleName())) {
                    return moduleInfo;
                }
            }
            return null;
        }
        return this.mModuleInfo.get(name);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getPackageName() {
        if (!this.mMetadataLoaded) {
            throw new IllegalStateException("Call to getVersion before metadata loaded");
        }
        return this.mPackageName;
    }
}
