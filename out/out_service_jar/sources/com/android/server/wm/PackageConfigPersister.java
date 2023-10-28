package com.android.server.wm;

import android.os.Environment;
import android.os.LocaleList;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.PackageConfigPersister;
import com.android.server.wm.PersisterQueue;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class PackageConfigPersister {
    private static final String ATTR_LOCALES = "locale_list";
    private static final String ATTR_NIGHT_MODE = "night_mode";
    private static final String ATTR_PACKAGE_NAME = "package_name";
    private static final boolean DEBUG = false;
    private static final String PACKAGE_DIRNAME = "package_configs";
    private static final String SUFFIX_FILE_NAME = "_config.xml";
    private static final String TAG = PackageConfigPersister.class.getSimpleName();
    private static final String TAG_CONFIG = "config";
    private final ActivityTaskManagerService mAtm;
    private final PersisterQueue mPersisterQueue;
    private final Object mLock = new Object();
    private final SparseArray<HashMap<String, PackageConfigRecord>> mPendingWrite = new SparseArray<>();
    private final SparseArray<HashMap<String, PackageConfigRecord>> mModified = new SparseArray<>();

    /* JADX INFO: Access modifiers changed from: private */
    public static File getUserConfigsDir(int userId) {
        return new File(Environment.getDataSystemCeDirectory(userId), PACKAGE_DIRNAME);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageConfigPersister(PersisterQueue queue, ActivityTaskManagerService atm) {
        this.mPersisterQueue = queue;
        this.mAtm = atm;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void loadUserPackages(int userId) {
        char c;
        synchronized (this.mLock) {
            try {
                try {
                    File userConfigsDir = getUserConfigsDir(userId);
                    File[] configFiles = userConfigsDir.listFiles();
                    if (configFiles == null) {
                        Slog.v(TAG, "loadPackages: empty list files from " + userConfigsDir);
                        return;
                    }
                    for (File configFile : configFiles) {
                        if (configFile.getName().endsWith(SUFFIX_FILE_NAME)) {
                            try {
                                InputStream is = new FileInputStream(configFile);
                                try {
                                    TypedXmlPullParser in = Xml.resolvePullParser(is);
                                    String packageName = null;
                                    Integer nightMode = null;
                                    LocaleList locales = null;
                                    while (true) {
                                        int event = in.next();
                                        if (event != 1 && event != 3) {
                                            String name = in.getName();
                                            if (event == 2 && TAG_CONFIG.equals(name)) {
                                                for (int attIdx = in.getAttributeCount() - 1; attIdx >= 0; attIdx--) {
                                                    String attrName = in.getAttributeName(attIdx);
                                                    String attrValue = in.getAttributeValue(attIdx);
                                                    switch (attrName.hashCode()) {
                                                        case -1877165340:
                                                            if (attrName.equals(ATTR_PACKAGE_NAME)) {
                                                                c = 0;
                                                                break;
                                                            }
                                                            c = 65535;
                                                            break;
                                                        case -601793174:
                                                            if (attrName.equals(ATTR_NIGHT_MODE)) {
                                                                c = 1;
                                                                break;
                                                            }
                                                            c = 65535;
                                                            break;
                                                        case 1912882019:
                                                            if (attrName.equals(ATTR_LOCALES)) {
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
                                                            packageName = attrValue;
                                                            break;
                                                        case 1:
                                                            nightMode = Integer.valueOf(Integer.parseInt(attrValue));
                                                            break;
                                                        case 2:
                                                            locales = LocaleList.forLanguageTags(attrValue);
                                                            break;
                                                    }
                                                }
                                            }
                                            XmlUtils.skipCurrentTag(in);
                                        }
                                    }
                                    if (packageName != null) {
                                        try {
                                            PackageConfigRecord initRecord = findRecordOrCreate(this.mModified, packageName, userId);
                                            initRecord.mNightMode = nightMode;
                                            initRecord.mLocales = locales;
                                        } catch (Throwable th) {
                                            th = th;
                                            Throwable th2 = th;
                                            try {
                                                is.close();
                                            } catch (Throwable th3) {
                                                th2.addSuppressed(th3);
                                            }
                                            throw th2;
                                        }
                                    }
                                    try {
                                        is.close();
                                    } catch (FileNotFoundException e) {
                                        e = e;
                                        e.printStackTrace();
                                    } catch (IOException e2) {
                                        e = e2;
                                        e.printStackTrace();
                                    } catch (XmlPullParserException e3) {
                                        e = e3;
                                        e.printStackTrace();
                                    }
                                } catch (Throwable th4) {
                                    th = th4;
                                }
                            } catch (FileNotFoundException e4) {
                                e = e4;
                            } catch (IOException e5) {
                                e = e5;
                            } catch (XmlPullParserException e6) {
                                e = e6;
                            }
                        }
                    }
                } catch (Throwable th5) {
                    th = th5;
                    throw th;
                }
            } catch (Throwable th6) {
                th = th6;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateConfigIfNeeded(ConfigurationContainer container, int userId, String packageName) {
        synchronized (this.mLock) {
            PackageConfigRecord modifiedRecord = findRecord(this.mModified, packageName, userId);
            if (modifiedRecord != null) {
                container.applyAppSpecificConfig(modifiedRecord.mNightMode, LocaleOverlayHelper.combineLocalesIfOverlayExists(modifiedRecord.mLocales, this.mAtm.getGlobalConfiguration().getLocales()));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateFromImpl(String packageName, int userId, PackageConfigurationUpdaterImpl impl) {
        PackageConfigRecord writeRecord;
        synchronized (this.mLock) {
            boolean isRecordPresent = false;
            PackageConfigRecord record = findRecord(this.mModified, packageName, userId);
            if (record != null) {
                isRecordPresent = true;
            } else {
                record = findRecordOrCreate(this.mModified, packageName, userId);
            }
            boolean isNightModeChanged = updateNightMode(impl.getNightMode(), record);
            boolean isLocalesChanged = updateLocales(impl.getLocales(), record);
            if ((record.mNightMode != null && !record.isResetNightMode()) || (record.mLocales != null && !record.mLocales.isEmpty())) {
                if (!isNightModeChanged && !isLocalesChanged) {
                    return false;
                }
                PackageConfigRecord pendingRecord = findRecord(this.mPendingWrite, record.mName, record.mUserId);
                if (pendingRecord == null) {
                    writeRecord = findRecordOrCreate(this.mPendingWrite, record.mName, record.mUserId);
                } else {
                    writeRecord = pendingRecord;
                }
                if (updateNightMode(record.mNightMode, writeRecord) || updateLocales(record.mLocales, writeRecord)) {
                    this.mPersisterQueue.addItem(new WriteProcessItem(writeRecord), false);
                    return true;
                }
                return false;
            }
            removePackage(packageName, userId);
            return isRecordPresent;
        }
    }

    private boolean updateNightMode(Integer requestedNightMode, PackageConfigRecord record) {
        if (requestedNightMode == null || requestedNightMode.equals(record.mNightMode)) {
            return false;
        }
        record.mNightMode = requestedNightMode;
        return true;
    }

    private boolean updateLocales(LocaleList requestedLocaleList, PackageConfigRecord record) {
        if (requestedLocaleList == null || requestedLocaleList.equals(record.mLocales)) {
            return false;
        }
        record.mLocales = requestedLocaleList;
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeUser(int userId) {
        synchronized (this.mLock) {
            HashMap<String, PackageConfigRecord> modifyRecords = this.mModified.get(userId);
            HashMap<String, PackageConfigRecord> writeRecords = this.mPendingWrite.get(userId);
            if ((modifyRecords != null && modifyRecords.size() != 0) || (writeRecords != null && writeRecords.size() != 0)) {
                HashMap<String, PackageConfigRecord> tempList = new HashMap<>(modifyRecords);
                tempList.forEach(new BiConsumer() { // from class: com.android.server.wm.PackageConfigPersister$$ExternalSyntheticLambda1
                    @Override // java.util.function.BiConsumer
                    public final void accept(Object obj, Object obj2) {
                        PackageConfigPersister.this.m8126lambda$removeUser$0$comandroidserverwmPackageConfigPersister((String) obj, (PackageConfigPersister.PackageConfigRecord) obj2);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$removeUser$0$com-android-server-wm-PackageConfigPersister  reason: not valid java name */
    public /* synthetic */ void m8126lambda$removeUser$0$comandroidserverwmPackageConfigPersister(String name, PackageConfigRecord record) {
        removePackage(record.mName, record.mUserId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackageUninstall(String packageName, int userId) {
        synchronized (this.mLock) {
            removePackage(packageName, userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackageDataCleared(String packageName, int userId) {
        synchronized (this.mLock) {
            removePackage(packageName, userId);
        }
    }

    private void removePackage(String packageName, int userId) {
        final PackageConfigRecord record = findRecord(this.mPendingWrite, packageName, userId);
        if (record != null) {
            removeRecord(this.mPendingWrite, record);
            this.mPersisterQueue.removeItems(new Predicate() { // from class: com.android.server.wm.PackageConfigPersister$$ExternalSyntheticLambda0
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return PackageConfigPersister.lambda$removePackage$1(PackageConfigPersister.PackageConfigRecord.this, (PackageConfigPersister.WriteProcessItem) obj);
                }
            }, WriteProcessItem.class);
        }
        PackageConfigRecord modifyRecord = findRecord(this.mModified, packageName, userId);
        if (modifyRecord != null) {
            removeRecord(this.mModified, modifyRecord);
            this.mPersisterQueue.addItem(new DeletePackageItem(userId, packageName), false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$removePackage$1(PackageConfigRecord record, WriteProcessItem item) {
        return item.mRecord.mName == record.mName && item.mRecord.mUserId == record.mUserId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityTaskManagerInternal.PackageConfig findPackageConfiguration(String packageName, int userId) {
        synchronized (this.mLock) {
            PackageConfigRecord packageConfigRecord = findRecord(this.mModified, packageName, userId);
            if (packageConfigRecord == null) {
                Slog.w(TAG, "App-specific configuration not found for packageName: " + packageName + " and userId: " + userId);
                return null;
            }
            return new ActivityTaskManagerInternal.PackageConfig(packageConfigRecord.mNightMode, packageConfigRecord.mLocales);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, int userId) {
        pw.println("INSTALLED PACKAGES HAVING APP-SPECIFIC CONFIGURATIONS");
        pw.println("Current user ID : " + userId);
        synchronized (this.mLock) {
            HashMap<String, PackageConfigRecord> persistedPackageConfigMap = this.mModified.get(userId);
            if (persistedPackageConfigMap != null) {
                for (PackageConfigRecord packageConfig : persistedPackageConfigMap.values()) {
                    pw.println();
                    pw.println("    PackageName : " + packageConfig.mName);
                    pw.println("        NightMode : " + packageConfig.mNightMode);
                    pw.println("        Locales : " + packageConfig.mLocales);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class PackageConfigRecord {
        LocaleList mLocales;
        final String mName;
        Integer mNightMode;
        final int mUserId;

        PackageConfigRecord(String name, int userId) {
            this.mName = name;
            this.mUserId = userId;
        }

        boolean isResetNightMode() {
            return this.mNightMode.intValue() == 0;
        }

        public String toString() {
            return "PackageConfigRecord package name: " + this.mName + " userId " + this.mUserId + " nightMode " + this.mNightMode + " locales " + this.mLocales;
        }
    }

    private PackageConfigRecord findRecordOrCreate(SparseArray<HashMap<String, PackageConfigRecord>> list, String name, int userId) {
        HashMap<String, PackageConfigRecord> records = list.get(userId);
        if (records == null) {
            records = new HashMap<>();
            list.put(userId, records);
        }
        PackageConfigRecord record = records.get(name);
        if (record != null) {
            return record;
        }
        PackageConfigRecord record2 = new PackageConfigRecord(name, userId);
        records.put(name, record2);
        return record2;
    }

    private PackageConfigRecord findRecord(SparseArray<HashMap<String, PackageConfigRecord>> list, String name, int userId) {
        HashMap<String, PackageConfigRecord> packages = list.get(userId);
        if (packages == null) {
            return null;
        }
        return packages.get(name);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeRecord(SparseArray<HashMap<String, PackageConfigRecord>> list, PackageConfigRecord record) {
        HashMap<String, PackageConfigRecord> processes = list.get(record.mUserId);
        if (processes != null) {
            processes.remove(record.mName);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class DeletePackageItem implements PersisterQueue.WriteQueueItem {
        final String mPackageName;
        final int mUserId;

        DeletePackageItem(int userId, String packageName) {
            this.mUserId = userId;
            this.mPackageName = packageName;
        }

        @Override // com.android.server.wm.PersisterQueue.WriteQueueItem
        public void process() {
            File userConfigsDir = PackageConfigPersister.getUserConfigsDir(this.mUserId);
            if (!userConfigsDir.isDirectory()) {
                return;
            }
            AtomicFile atomicFile = new AtomicFile(new File(userConfigsDir, this.mPackageName + PackageConfigPersister.SUFFIX_FILE_NAME));
            if (atomicFile.exists()) {
                atomicFile.delete();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class WriteProcessItem implements PersisterQueue.WriteQueueItem {
        final PackageConfigRecord mRecord;

        WriteProcessItem(PackageConfigRecord record) {
            this.mRecord = record;
        }

        @Override // com.android.server.wm.PersisterQueue.WriteQueueItem
        public void process() {
            byte[] data = null;
            synchronized (PackageConfigPersister.this.mLock) {
                try {
                    data = saveToXml();
                } catch (Exception e) {
                }
                PackageConfigPersister packageConfigPersister = PackageConfigPersister.this;
                packageConfigPersister.removeRecord(packageConfigPersister.mPendingWrite, this.mRecord);
            }
            if (data != null) {
                AtomicFile atomicFile = null;
                try {
                    File userConfigsDir = PackageConfigPersister.getUserConfigsDir(this.mRecord.mUserId);
                    if (!userConfigsDir.isDirectory() && !userConfigsDir.mkdirs()) {
                        Slog.e(PackageConfigPersister.TAG, "Failure creating tasks directory for user " + this.mRecord.mUserId + ": " + userConfigsDir);
                        return;
                    }
                    AtomicFile atomicFile2 = new AtomicFile(new File(userConfigsDir, this.mRecord.mName + PackageConfigPersister.SUFFIX_FILE_NAME));
                    FileOutputStream file = atomicFile2.startWrite();
                    file.write(data);
                    atomicFile2.finishWrite(file);
                } catch (IOException e2) {
                    if (0 != 0) {
                        atomicFile.failWrite(null);
                    }
                    Slog.e(PackageConfigPersister.TAG, "Unable to open " + ((Object) null) + " for persisting. " + e2);
                }
            }
        }

        private byte[] saveToXml() throws IOException {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            TypedXmlSerializer xmlSerializer = Xml.resolveSerializer(os);
            xmlSerializer.startDocument((String) null, true);
            xmlSerializer.startTag((String) null, PackageConfigPersister.TAG_CONFIG);
            xmlSerializer.attribute((String) null, PackageConfigPersister.ATTR_PACKAGE_NAME, this.mRecord.mName);
            if (this.mRecord.mNightMode != null) {
                xmlSerializer.attributeInt((String) null, PackageConfigPersister.ATTR_NIGHT_MODE, this.mRecord.mNightMode.intValue());
            }
            if (this.mRecord.mLocales != null) {
                xmlSerializer.attribute((String) null, PackageConfigPersister.ATTR_LOCALES, this.mRecord.mLocales.toLanguageTags());
            }
            xmlSerializer.endTag((String) null, PackageConfigPersister.TAG_CONFIG);
            xmlSerializer.endDocument();
            xmlSerializer.flush();
            return os.toByteArray();
        }
    }
}
