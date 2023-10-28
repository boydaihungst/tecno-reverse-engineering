package com.android.server.locales;

import android.app.backup.BackupManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.HandlerThread;
import android.os.LocaleList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class LocaleManagerBackupHelper {
    private static final String ATTR_CREATION_TIME_MILLIS = "creationTimeMillis";
    private static final String ATTR_LOCALES = "locales";
    private static final String ATTR_PACKAGE_NAME = "name";
    private static final String LOCALES_XML_TAG = "locales";
    private static final String PACKAGE_XML_TAG = "package";
    private static final Duration STAGE_DATA_RETENTION_PERIOD = Duration.ofDays(3);
    private static final String SYSTEM_BACKUP_PACKAGE_KEY = "android";
    private static final String TAG = "LocaleManagerBkpHelper";
    private final Clock mClock;
    private final Context mContext;
    private final LocaleManagerService mLocaleManagerService;
    private final PackageManager mPackageManager;
    private final SparseArray<StagedData> mStagedData;
    private final Object mStagedDataLock;
    private final BroadcastReceiver mUserMonitor;

    /* JADX INFO: Access modifiers changed from: package-private */
    public LocaleManagerBackupHelper(LocaleManagerService localeManagerService, PackageManager packageManager, HandlerThread broadcastHandlerThread) {
        this(localeManagerService.mContext, localeManagerService, packageManager, Clock.systemUTC(), new SparseArray(), broadcastHandlerThread);
    }

    LocaleManagerBackupHelper(Context context, LocaleManagerService localeManagerService, PackageManager packageManager, Clock clock, SparseArray<StagedData> stagedData, HandlerThread broadcastHandlerThread) {
        this.mStagedDataLock = new Object();
        this.mContext = context;
        this.mLocaleManagerService = localeManagerService;
        this.mPackageManager = packageManager;
        this.mClock = clock;
        this.mStagedData = stagedData;
        UserMonitor userMonitor = new UserMonitor();
        this.mUserMonitor = userMonitor;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_REMOVED");
        context.registerReceiverAsUser(userMonitor, UserHandle.ALL, filter, null, broadcastHandlerThread.getThreadHandler());
    }

    BroadcastReceiver getUserMonitor() {
        return this.mUserMonitor;
    }

    public byte[] getBackupPayload(int userId) {
        synchronized (this.mStagedDataLock) {
            cleanStagedDataForOldEntriesLocked();
        }
        HashMap<String, String> pkgStates = new HashMap<>();
        for (ApplicationInfo appInfo : this.mPackageManager.getInstalledApplicationsAsUser(PackageManager.ApplicationInfoFlags.of(0L), userId)) {
            try {
                LocaleList appLocales = this.mLocaleManagerService.getApplicationLocales(appInfo.packageName, userId);
                if (!appLocales.isEmpty()) {
                    pkgStates.put(appInfo.packageName, appLocales.toLanguageTags());
                }
            } catch (RemoteException | IllegalArgumentException e) {
                Slog.e(TAG, "Exception when getting locales for package: " + appInfo.packageName, e);
            }
        }
        if (pkgStates.isEmpty()) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            writeToXml(out, pkgStates);
            return out.toByteArray();
        } catch (IOException e2) {
            Slog.e(TAG, "Could not write to xml for backup ", e2);
            return null;
        }
    }

    private void cleanStagedDataForOldEntriesLocked() {
        for (int i = 0; i < this.mStagedData.size(); i++) {
            int userId = this.mStagedData.keyAt(i);
            StagedData stagedData = this.mStagedData.get(userId);
            if (stagedData.mCreationTimeMillis < this.mClock.millis() - STAGE_DATA_RETENTION_PERIOD.toMillis()) {
                deleteStagedDataLocked(userId);
            }
        }
    }

    public void stageAndApplyRestoredPayload(byte[] payload, int userId) {
        if (payload == null) {
            Slog.e(TAG, "stageAndApplyRestoredPayload: no payload to restore for user " + userId);
            return;
        }
        ByteArrayInputStream inputStream = new ByteArrayInputStream(payload);
        try {
            TypedXmlPullParser parser = Xml.newFastPullParser();
            parser.setInput(inputStream, StandardCharsets.UTF_8.name());
            XmlUtils.beginDocument(parser, "locales");
            HashMap<String, String> pkgStates = readFromXml(parser);
            synchronized (this.mStagedDataLock) {
                StagedData stagedData = new StagedData(this.mClock.millis(), new HashMap());
                for (String pkgName : pkgStates.keySet()) {
                    String languageTags = pkgStates.get(pkgName);
                    if (isPackageInstalledForUser(pkgName, userId)) {
                        checkExistingLocalesAndApplyRestore(pkgName, languageTags, userId);
                    } else {
                        stagedData.mPackageStates.put(pkgName, languageTags);
                    }
                }
                if (!stagedData.mPackageStates.isEmpty()) {
                    this.mStagedData.put(userId, stagedData);
                }
            }
        } catch (IOException | XmlPullParserException e) {
            Slog.e(TAG, "Could not parse payload ", e);
        }
    }

    public void notifyBackupManager() {
        BackupManager.dataChanged("android");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackageAdded(String packageName, int uid) {
        try {
            synchronized (this.mStagedDataLock) {
                cleanStagedDataForOldEntriesLocked();
                int userId = UserHandle.getUserId(uid);
                if (this.mStagedData.contains(userId)) {
                    doLazyRestoreLocked(packageName, userId);
                }
            }
        } catch (Exception e) {
            Slog.e(TAG, "Exception in onPackageAdded.", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackageDataCleared() {
        try {
            notifyBackupManager();
        } catch (Exception e) {
            Slog.e(TAG, "Exception in onPackageDataCleared.", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackageRemoved() {
        try {
            notifyBackupManager();
        } catch (Exception e) {
            Slog.e(TAG, "Exception in onPackageRemoved.", e);
        }
    }

    private boolean isPackageInstalledForUser(String packageName, int userId) {
        PackageInfo pkgInfo = null;
        try {
            pkgInfo = this.mContext.getPackageManager().getPackageInfoAsUser(packageName, 0, userId);
        } catch (PackageManager.NameNotFoundException e) {
        }
        return pkgInfo != null;
    }

    private void checkExistingLocalesAndApplyRestore(String pkgName, String languageTags, int userId) {
        try {
            LocaleList currLocales = this.mLocaleManagerService.getApplicationLocales(pkgName, userId);
            if (!currLocales.isEmpty()) {
                return;
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Could not check for current locales before restoring", e);
        }
        try {
            this.mLocaleManagerService.setApplicationLocales(pkgName, userId, LocaleList.forLanguageTags(languageTags));
        } catch (RemoteException | IllegalArgumentException e2) {
            Slog.e(TAG, "Could not restore locales for " + pkgName, e2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void deleteStagedDataLocked(int userId) {
        this.mStagedData.remove(userId);
    }

    private HashMap<String, String> readFromXml(XmlPullParser parser) throws IOException, XmlPullParserException {
        HashMap<String, String> packageStates = new HashMap<>();
        int depth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, depth)) {
            if (parser.getName().equals("package")) {
                String packageName = parser.getAttributeValue(null, "name");
                String languageTags = parser.getAttributeValue(null, "locales");
                if (!TextUtils.isEmpty(packageName) && !TextUtils.isEmpty(languageTags)) {
                    packageStates.put(packageName, languageTags);
                }
            }
        }
        return packageStates;
    }

    private static void writeToXml(OutputStream stream, HashMap<String, String> pkgStates) throws IOException {
        if (pkgStates.isEmpty()) {
            return;
        }
        TypedXmlSerializer out = Xml.newFastSerializer();
        out.setOutput(stream, StandardCharsets.UTF_8.name());
        out.startDocument((String) null, true);
        out.startTag((String) null, "locales");
        for (String pkg : pkgStates.keySet()) {
            out.startTag((String) null, "package");
            out.attribute((String) null, "name", pkg);
            out.attribute((String) null, "locales", pkgStates.get(pkg));
            out.endTag((String) null, "package");
        }
        out.endTag((String) null, "locales");
        out.endDocument();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class StagedData {
        final long mCreationTimeMillis;
        final HashMap<String, String> mPackageStates;

        StagedData(long creationTimeMillis, HashMap<String, String> pkgStates) {
            this.mCreationTimeMillis = creationTimeMillis;
            this.mPackageStates = pkgStates;
        }
    }

    /* loaded from: classes.dex */
    private final class UserMonitor extends BroadcastReceiver {
        private UserMonitor() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                if (action.equals("android.intent.action.USER_REMOVED")) {
                    int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                    synchronized (LocaleManagerBackupHelper.this.mStagedDataLock) {
                        LocaleManagerBackupHelper.this.deleteStagedDataLocked(userId);
                    }
                }
            } catch (Exception e) {
                Slog.e(LocaleManagerBackupHelper.TAG, "Exception in user monitor.", e);
            }
        }
    }

    private void doLazyRestoreLocked(String packageName, int userId) {
        if (!isPackageInstalledForUser(packageName, userId)) {
            Slog.e(TAG, packageName + " not installed for user " + userId + ". Could not restore locales from stage data");
            return;
        }
        StagedData stagedData = this.mStagedData.get(userId);
        for (String pkgName : stagedData.mPackageStates.keySet()) {
            String languageTags = stagedData.mPackageStates.get(pkgName);
            if (pkgName.equals(packageName)) {
                checkExistingLocalesAndApplyRestore(pkgName, languageTags, userId);
                stagedData.mPackageStates.remove(pkgName);
                if (stagedData.mPackageStates.isEmpty()) {
                    this.mStagedData.remove(userId);
                    return;
                }
                return;
            }
        }
    }
}
