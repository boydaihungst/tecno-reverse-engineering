package com.android.server.locales;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.LocaleList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class SystemAppUpdateTracker {
    private static final String ATTR_NAME = "name";
    private static final String PACKAGE_XML_TAG = "package";
    private static final String SYSTEM_APPS_XML_TAG = "system_apps";
    private static final String TAG = "SystemAppUpdateTracker";
    private final Context mContext;
    private final Object mFileLock;
    private final LocaleManagerService mLocaleManagerService;
    private final Set<String> mUpdatedApps;
    private final AtomicFile mUpdatedAppsFile;

    /* JADX INFO: Access modifiers changed from: package-private */
    public SystemAppUpdateTracker(LocaleManagerService localeManagerService) {
        this(localeManagerService.mContext, localeManagerService, new AtomicFile(new File(Environment.getDataSystemDirectory(), "locale_manager_service_updated_system_apps.xml")));
    }

    SystemAppUpdateTracker(Context context, LocaleManagerService localeManagerService, AtomicFile file) {
        this.mFileLock = new Object();
        this.mUpdatedApps = new HashSet();
        this.mContext = context;
        this.mLocaleManagerService = localeManagerService;
        this.mUpdatedAppsFile = file;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void init() {
        loadUpdatedSystemApps();
    }

    private void loadUpdatedSystemApps() {
        if (!this.mUpdatedAppsFile.getBaseFile().exists()) {
            return;
        }
        InputStream updatedAppNamesInputStream = null;
        try {
            try {
                updatedAppNamesInputStream = this.mUpdatedAppsFile.openRead();
                readFromXml(updatedAppNamesInputStream);
            } catch (IOException | XmlPullParserException e) {
                Slog.e(TAG, "loadUpdatedSystemApps: Could not parse storage file ", e);
            }
        } finally {
            IoUtils.closeQuietly(updatedAppNamesInputStream);
        }
    }

    private void readFromXml(InputStream updateInfoInputStream) throws XmlPullParserException, IOException {
        TypedXmlPullParser parser = Xml.newFastPullParser();
        parser.setInput(updateInfoInputStream, StandardCharsets.UTF_8.name());
        XmlUtils.beginDocument(parser, SYSTEM_APPS_XML_TAG);
        int depth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, depth)) {
            if (parser.getName().equals("package")) {
                String packageName = parser.getAttributeValue((String) null, "name");
                if (!TextUtils.isEmpty(packageName)) {
                    this.mUpdatedApps.add(packageName);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackageUpdateFinished(String packageName, int uid) {
        try {
            if (!this.mUpdatedApps.contains(packageName) && isUpdatedSystemApp(packageName)) {
                String installingPackageName = this.mLocaleManagerService.getInstallingPackageName(packageName);
                if (installingPackageName == null) {
                    return;
                }
                try {
                    int userId = UserHandle.getUserId(uid);
                    LocaleList appLocales = this.mLocaleManagerService.getApplicationLocales(packageName, userId);
                    if (!appLocales.isEmpty()) {
                        this.mLocaleManagerService.notifyInstallerOfAppWhoseLocaleChanged(packageName, userId, appLocales);
                    }
                } catch (RemoteException e) {
                }
                updateBroadcastedAppsList(packageName);
            }
        } catch (Exception e2) {
            Slog.e(TAG, "Exception in onPackageUpdateFinished.", e2);
        }
    }

    private void updateBroadcastedAppsList(String packageName) {
        synchronized (this.mFileLock) {
            this.mUpdatedApps.add(packageName);
            writeUpdatedAppsFileLocked();
        }
    }

    private void writeUpdatedAppsFileLocked() {
        FileOutputStream stream = null;
        try {
            stream = this.mUpdatedAppsFile.startWrite();
            writeToXmlLocked(stream);
            this.mUpdatedAppsFile.finishWrite(stream);
        } catch (IOException e) {
            this.mUpdatedAppsFile.failWrite(stream);
            Slog.e(TAG, "Failed to persist the updated apps list", e);
        }
    }

    private void writeToXmlLocked(OutputStream stream) throws IOException {
        TypedXmlSerializer xml = Xml.newFastSerializer();
        xml.setOutput(stream, StandardCharsets.UTF_8.name());
        xml.startDocument((String) null, true);
        xml.startTag((String) null, SYSTEM_APPS_XML_TAG);
        for (String packageName : this.mUpdatedApps) {
            xml.startTag((String) null, "package");
            xml.attribute((String) null, "name", packageName);
            xml.endTag((String) null, "package");
        }
        xml.endTag((String) null, SYSTEM_APPS_XML_TAG);
        xml.endDocument();
    }

    private boolean isUpdatedSystemApp(String packageName) {
        ApplicationInfo appInfo = null;
        try {
            appInfo = this.mContext.getPackageManager().getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(1048576L));
        } catch (PackageManager.NameNotFoundException e) {
        }
        return (appInfo == null || (appInfo.flags & 128) == 0) ? false : true;
    }

    Set<String> getUpdatedApps() {
        return this.mUpdatedApps;
    }
}
