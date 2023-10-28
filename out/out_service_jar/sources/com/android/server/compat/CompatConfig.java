package com.android.server.compat;

import android.app.compat.ChangeIdStateCache;
import android.app.compat.PackageOverride;
import android.compat.Compatibility;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.text.TextUtils;
import android.util.LongArray;
import android.util.Slog;
import com.android.internal.compat.AndroidBuildClassifier;
import com.android.internal.compat.CompatibilityChangeConfig;
import com.android.internal.compat.CompatibilityChangeInfo;
import com.android.internal.compat.CompatibilityOverrideConfig;
import com.android.internal.compat.CompatibilityOverridesByPackageConfig;
import com.android.internal.compat.CompatibilityOverridesToRemoveByPackageConfig;
import com.android.internal.compat.CompatibilityOverridesToRemoveConfig;
import com.android.internal.compat.IOverrideValidator;
import com.android.internal.compat.OverrideAllowedState;
import com.android.server.compat.CompatChange;
import com.android.server.compat.config.Change;
import com.android.server.compat.config.Config;
import com.android.server.compat.config.XmlParser;
import com.android.server.compat.overrides.ChangeOverrides;
import com.android.server.compat.overrides.Overrides;
import com.android.server.compat.overrides.XmlWriter;
import com.android.server.pm.ApexManager;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class CompatConfig {
    private static final String APP_COMPAT_DATA_DIR = "/data/misc/appcompat";
    private static final String OVERRIDES_FILE = "compat_framework_overrides.xml";
    private static final String STATIC_OVERRIDES_PRODUCT_DIR = "/product/etc/appcompat";
    private static final String TAG = "CompatConfig";
    private final AndroidBuildClassifier mAndroidBuildClassifier;
    private File mBackupOverridesFile;
    private Context mContext;
    private final OverrideValidatorImpl mOverrideValidator;
    private File mOverridesFile;
    private final ConcurrentHashMap<Long, CompatChange> mChanges = new ConcurrentHashMap<>();
    private final Object mOverridesFileLock = new Object();

    CompatConfig(AndroidBuildClassifier androidBuildClassifier, Context context) {
        this.mOverrideValidator = new OverrideValidatorImpl(androidBuildClassifier, context, this);
        this.mAndroidBuildClassifier = androidBuildClassifier;
        this.mContext = context;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static CompatConfig create(AndroidBuildClassifier androidBuildClassifier, Context context) {
        CompatConfig config = new CompatConfig(androidBuildClassifier, context);
        config.initConfigFromLib(Environment.buildPath(Environment.getRootDirectory(), new String[]{"etc", "compatconfig"}));
        config.initConfigFromLib(Environment.buildPath(Environment.getRootDirectory(), new String[]{"system_ext", "etc", "compatconfig"}));
        List<ApexManager.ActiveApexInfo> apexes = ApexManager.getInstance().getActiveApexInfos();
        for (ApexManager.ActiveApexInfo apex : apexes) {
            config.initConfigFromLib(Environment.buildPath(apex.apexDirectory, new String[]{"etc", "compatconfig"}));
        }
        config.initOverrides();
        config.invalidateCache();
        return config;
    }

    void addChange(CompatChange change) {
        this.mChanges.put(Long.valueOf(change.getId()), change);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long[] getDisabledChanges(ApplicationInfo app) {
        LongArray disabled = new LongArray();
        for (CompatChange c : this.mChanges.values()) {
            if (!c.isEnabled(app, this.mAndroidBuildClassifier)) {
                disabled.add(c.getId());
            }
        }
        long[] sortedChanges = disabled.toArray();
        Arrays.sort(sortedChanges);
        return sortedChanges;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long lookupChangeId(String name) {
        for (CompatChange c : this.mChanges.values()) {
            if (TextUtils.equals(c.getName(), name)) {
                return c.getId();
            }
        }
        return -1L;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isChangeEnabled(long changeId, ApplicationInfo app) {
        CompatChange c = this.mChanges.get(Long.valueOf(changeId));
        if (c == null) {
            return true;
        }
        return c.isEnabled(app, this.mAndroidBuildClassifier);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean willChangeBeEnabled(long changeId, String packageName) {
        CompatChange c = this.mChanges.get(Long.valueOf(changeId));
        if (c == null) {
            return true;
        }
        return c.willBeEnabled(packageName);
    }

    synchronized boolean addOverride(long changeId, String packageName, boolean enabled) {
        boolean alreadyKnown;
        alreadyKnown = addOverrideUnsafe(changeId, packageName, new PackageOverride.Builder().setEnabled(enabled).build());
        saveOverrides();
        invalidateCache();
        return alreadyKnown;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void addAllPackageOverrides(CompatibilityOverridesByPackageConfig overridesByPackage, boolean skipUnknownChangeIds) {
        for (String packageName : overridesByPackage.packageNameToOverrides.keySet()) {
            addPackageOverridesWithoutSaving((CompatibilityOverrideConfig) overridesByPackage.packageNameToOverrides.get(packageName), packageName, skipUnknownChangeIds);
        }
        saveOverrides();
        invalidateCache();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void addPackageOverrides(CompatibilityOverrideConfig overrides, String packageName, boolean skipUnknownChangeIds) {
        addPackageOverridesWithoutSaving(overrides, packageName, skipUnknownChangeIds);
        saveOverrides();
        invalidateCache();
    }

    private void addPackageOverridesWithoutSaving(CompatibilityOverrideConfig overrides, String packageName, boolean skipUnknownChangeIds) {
        for (Long changeId : overrides.overrides.keySet()) {
            if (skipUnknownChangeIds && !isKnownChangeId(changeId.longValue())) {
                Slog.w(TAG, "Trying to add overrides for unknown Change ID " + changeId + ". Skipping Change ID.");
            } else {
                addOverrideUnsafe(changeId.longValue(), packageName, (PackageOverride) overrides.overrides.get(changeId));
            }
        }
    }

    private boolean addOverrideUnsafe(final long changeId, String packageName, PackageOverride overrides) {
        final AtomicBoolean alreadyKnown = new AtomicBoolean(true);
        OverrideAllowedState allowedState = this.mOverrideValidator.getOverrideAllowedState(changeId, packageName);
        allowedState.enforce(changeId, packageName);
        Long versionCode = getVersionCodeOrNull(packageName);
        CompatChange c = this.mChanges.computeIfAbsent(Long.valueOf(changeId), new Function() { // from class: com.android.server.compat.CompatConfig$$ExternalSyntheticLambda1
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return CompatConfig.lambda$addOverrideUnsafe$0(alreadyKnown, changeId, (Long) obj);
            }
        });
        c.addPackageOverride(packageName, overrides, allowedState, versionCode);
        invalidateCache();
        return alreadyKnown.get();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ CompatChange lambda$addOverrideUnsafe$0(AtomicBoolean alreadyKnown, long changeId, Long key) {
        alreadyKnown.set(false);
        return new CompatChange(changeId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isKnownChangeId(long changeId) {
        return this.mChanges.containsKey(Long.valueOf(changeId));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int maxTargetSdkForChangeIdOptIn(long changeId) {
        CompatChange c = this.mChanges.get(Long.valueOf(changeId));
        if (c == null || c.getEnableSinceTargetSdk() == -1) {
            return -1;
        }
        return c.getEnableSinceTargetSdk() - 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isLoggingOnly(long changeId) {
        CompatChange c = this.mChanges.get(Long.valueOf(changeId));
        return c != null && c.getLoggingOnly();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDisabled(long changeId) {
        CompatChange c = this.mChanges.get(Long.valueOf(changeId));
        return c != null && c.getDisabled();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isOverridable(long changeId) {
        CompatChange c = this.mChanges.get(Long.valueOf(changeId));
        return c != null && c.getOverridable();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized boolean removeOverride(long changeId, String packageName) {
        boolean overrideExists;
        overrideExists = removeOverrideUnsafe(changeId, packageName);
        if (overrideExists) {
            saveOverrides();
            invalidateCache();
        }
        return overrideExists;
    }

    private boolean removeOverrideUnsafe(long changeId, String packageName) {
        Long versionCode = getVersionCodeOrNull(packageName);
        CompatChange c = this.mChanges.get(Long.valueOf(changeId));
        if (c != null) {
            return removeOverrideUnsafe(c, packageName, versionCode);
        }
        return false;
    }

    private boolean removeOverrideUnsafe(CompatChange change, String packageName, Long versionCode) {
        long changeId = change.getId();
        OverrideAllowedState allowedState = this.mOverrideValidator.getOverrideAllowedState(changeId, packageName);
        return change.removePackageOverride(packageName, allowedState, versionCode);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void removeAllPackageOverrides(CompatibilityOverridesToRemoveByPackageConfig overridesToRemoveByPackage) {
        boolean shouldInvalidateCache = false;
        for (String packageName : overridesToRemoveByPackage.packageNameToOverridesToRemove.keySet()) {
            shouldInvalidateCache |= removePackageOverridesWithoutSaving((CompatibilityOverridesToRemoveConfig) overridesToRemoveByPackage.packageNameToOverridesToRemove.get(packageName), packageName);
        }
        if (shouldInvalidateCache) {
            saveOverrides();
            invalidateCache();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void removePackageOverrides(String packageName) {
        Long versionCode = getVersionCodeOrNull(packageName);
        boolean shouldInvalidateCache = false;
        for (CompatChange change : this.mChanges.values()) {
            shouldInvalidateCache |= removeOverrideUnsafe(change, packageName, versionCode);
        }
        if (shouldInvalidateCache) {
            saveOverrides();
            invalidateCache();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void removePackageOverrides(CompatibilityOverridesToRemoveConfig overridesToRemove, String packageName) {
        boolean shouldInvalidateCache = removePackageOverridesWithoutSaving(overridesToRemove, packageName);
        if (shouldInvalidateCache) {
            saveOverrides();
            invalidateCache();
        }
    }

    private boolean removePackageOverridesWithoutSaving(CompatibilityOverridesToRemoveConfig overridesToRemove, String packageName) {
        boolean shouldInvalidateCache = false;
        for (Long changeId : overridesToRemove.changeIds) {
            if (!isKnownChangeId(changeId.longValue())) {
                Slog.w(TAG, "Trying to remove overrides for unknown Change ID " + changeId + ". Skipping Change ID.");
            } else {
                shouldInvalidateCache |= removeOverrideUnsafe(changeId.longValue(), packageName);
            }
        }
        return shouldInvalidateCache;
    }

    private long[] getAllowedChangesSinceTargetSdkForPackage(String packageName, int targetSdkVersion) {
        LongArray allowed = new LongArray();
        for (CompatChange change : this.mChanges.values()) {
            if (change.getEnableSinceTargetSdk() == targetSdkVersion) {
                OverrideAllowedState allowedState = this.mOverrideValidator.getOverrideAllowedState(change.getId(), packageName);
                if (allowedState.state == 0) {
                    allowed.add(change.getId());
                }
            }
        }
        return allowed.toArray();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int enableTargetSdkChangesForPackage(String packageName, int targetSdkVersion) {
        long[] changes = getAllowedChangesSinceTargetSdkForPackage(packageName, targetSdkVersion);
        boolean shouldInvalidateCache = false;
        for (long changeId : changes) {
            shouldInvalidateCache |= addOverrideUnsafe(changeId, packageName, new PackageOverride.Builder().setEnabled(true).build());
        }
        if (shouldInvalidateCache) {
            saveOverrides();
            invalidateCache();
        }
        return changes.length;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int disableTargetSdkChangesForPackage(String packageName, int targetSdkVersion) {
        long[] changes = getAllowedChangesSinceTargetSdkForPackage(packageName, targetSdkVersion);
        boolean shouldInvalidateCache = false;
        for (long changeId : changes) {
            shouldInvalidateCache |= addOverrideUnsafe(changeId, packageName, new PackageOverride.Builder().setEnabled(false).build());
        }
        if (shouldInvalidateCache) {
            saveOverrides();
            invalidateCache();
        }
        return changes.length;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean registerListener(final long changeId, CompatChange.ChangeListener listener) {
        final AtomicBoolean alreadyKnown = new AtomicBoolean(true);
        CompatChange c = this.mChanges.computeIfAbsent(Long.valueOf(changeId), new Function() { // from class: com.android.server.compat.CompatConfig$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return CompatConfig.this.m2741lambda$registerListener$1$comandroidservercompatCompatConfig(alreadyKnown, changeId, (Long) obj);
            }
        });
        c.registerListener(listener);
        return alreadyKnown.get();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$registerListener$1$com-android-server-compat-CompatConfig  reason: not valid java name */
    public /* synthetic */ CompatChange m2741lambda$registerListener$1$comandroidservercompatCompatConfig(AtomicBoolean alreadyKnown, long changeId, Long key) {
        alreadyKnown.set(false);
        invalidateCache();
        return new CompatChange(changeId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean defaultChangeIdValue(long changeId) {
        CompatChange c = this.mChanges.get(Long.valueOf(changeId));
        if (c == null) {
            return true;
        }
        return c.defaultValue();
    }

    void forceNonDebuggableFinalForTest(boolean value) {
        this.mOverrideValidator.forceNonDebuggableFinalForTest(value);
    }

    void clearChanges() {
        this.mChanges.clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpConfig(PrintWriter pw) {
        if (this.mChanges.size() == 0) {
            pw.println("No compat overrides.");
            return;
        }
        for (CompatChange c : this.mChanges.values()) {
            pw.println(c.toString());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public CompatibilityChangeConfig getAppConfig(ApplicationInfo applicationInfo) {
        Set<Long> enabled = new HashSet<>();
        Set<Long> disabled = new HashSet<>();
        for (CompatChange c : this.mChanges.values()) {
            if (c.isEnabled(applicationInfo, this.mAndroidBuildClassifier)) {
                enabled.add(Long.valueOf(c.getId()));
            } else {
                disabled.add(Long.valueOf(c.getId()));
            }
        }
        return new CompatibilityChangeConfig(new Compatibility.ChangeConfig(enabled, disabled));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public CompatibilityChangeInfo[] dumpChanges() {
        CompatibilityChangeInfo[] changeInfos = new CompatibilityChangeInfo[this.mChanges.size()];
        int i = 0;
        for (CompatChange change : this.mChanges.values()) {
            changeInfos[i] = new CompatibilityChangeInfo(change);
            i++;
        }
        return changeInfos;
    }

    void initConfigFromLib(File libraryDir) {
        File[] listFiles;
        if (!libraryDir.exists() || !libraryDir.isDirectory()) {
            Slog.d(TAG, "No directory " + libraryDir + ", skipping");
            return;
        }
        for (File f : libraryDir.listFiles()) {
            Slog.d(TAG, "Found a config file: " + f.getPath());
            readConfig(f);
        }
    }

    private void readConfig(File configFile) {
        InputStream in;
        try {
            try {
                in = new BufferedInputStream(new FileInputStream(configFile));
            } catch (IOException | DatatypeConfigurationException | XmlPullParserException e) {
                Slog.e(TAG, "Encountered an error while reading/parsing compat config file", e);
            }
            try {
                Config config = XmlParser.read(in);
                for (Change change : config.getCompatChange()) {
                    Slog.d(TAG, "Adding: " + change.toString());
                    this.mChanges.put(Long.valueOf(change.getId()), new CompatChange(change));
                }
                in.close();
            } catch (Throwable th) {
                try {
                    in.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } finally {
            invalidateCache();
        }
    }

    private void initOverrides() {
        initOverrides(new File(APP_COMPAT_DATA_DIR, OVERRIDES_FILE), new File(STATIC_OVERRIDES_PRODUCT_DIR, OVERRIDES_FILE));
    }

    void initOverrides(File dynamicOverridesFile, File staticOverridesFile) {
        for (CompatChange c : this.mChanges.values()) {
            c.clearOverrides();
        }
        loadOverrides(staticOverridesFile);
        synchronized (this.mOverridesFileLock) {
            this.mOverridesFile = dynamicOverridesFile;
            File makeBackupFile = makeBackupFile(dynamicOverridesFile);
            this.mBackupOverridesFile = makeBackupFile;
            if (makeBackupFile.exists()) {
                this.mOverridesFile.delete();
                this.mBackupOverridesFile.renameTo(this.mOverridesFile);
            }
            loadOverrides(this.mOverridesFile);
        }
        if (staticOverridesFile.exists()) {
            saveOverrides();
        }
    }

    private File makeBackupFile(File overridesFile) {
        return new File(overridesFile.getPath() + ".bak");
    }

    private void loadOverrides(File overridesFile) {
        if (!overridesFile.exists()) {
            return;
        }
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(overridesFile));
            Overrides overrides = com.android.server.compat.overrides.XmlParser.read(in);
            if (overrides == null) {
                Slog.w(TAG, "Parsing " + overridesFile.getPath() + " failed");
                in.close();
                return;
            }
            for (ChangeOverrides changeOverrides : overrides.getChangeOverrides()) {
                long changeId = changeOverrides.getChangeId();
                CompatChange compatChange = this.mChanges.get(Long.valueOf(changeId));
                if (compatChange == null) {
                    Slog.w(TAG, "Change ID " + changeId + " not found. Skipping overrides for it.");
                } else {
                    compatChange.loadOverrides(changeOverrides);
                }
            }
            in.close();
        } catch (IOException | DatatypeConfigurationException | XmlPullParserException e) {
            Slog.w(TAG, "Error processing " + overridesFile + " " + e.toString());
        }
    }

    void saveOverrides() {
        synchronized (this.mOverridesFileLock) {
            if (this.mOverridesFile != null && this.mBackupOverridesFile != null) {
                Overrides overrides = new Overrides();
                List<ChangeOverrides> changeOverridesList = overrides.getChangeOverrides();
                for (CompatChange c : this.mChanges.values()) {
                    ChangeOverrides changeOverrides = c.saveOverrides();
                    if (changeOverrides != null) {
                        changeOverridesList.add(changeOverrides);
                    }
                }
                if (this.mOverridesFile.exists()) {
                    if (this.mBackupOverridesFile.exists()) {
                        this.mOverridesFile.delete();
                    } else if (!this.mOverridesFile.renameTo(this.mBackupOverridesFile)) {
                        Slog.e(TAG, "Couldn't rename file " + this.mOverridesFile + " to " + this.mBackupOverridesFile);
                        return;
                    }
                }
                try {
                    this.mOverridesFile.createNewFile();
                    try {
                        PrintWriter out = new PrintWriter(this.mOverridesFile);
                        try {
                            XmlWriter writer = new XmlWriter(out);
                            XmlWriter.write(writer, overrides);
                            out.close();
                        } catch (Throwable th) {
                            try {
                                out.close();
                            } catch (Throwable th2) {
                                th.addSuppressed(th2);
                            }
                            throw th;
                        }
                    } catch (IOException e) {
                        Slog.e(TAG, e.toString());
                    }
                    this.mBackupOverridesFile.delete();
                } catch (IOException e2) {
                    Slog.e(TAG, "Could not create override config file: " + e2.toString());
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IOverrideValidator getOverrideValidator() {
        return this.mOverrideValidator;
    }

    private void invalidateCache() {
        ChangeIdStateCache.invalidate();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void recheckOverrides(String packageName) {
        Long versionCode = getVersionCodeOrNull(packageName);
        boolean shouldInvalidateCache = false;
        for (CompatChange c : this.mChanges.values()) {
            OverrideAllowedState allowedState = this.mOverrideValidator.getOverrideAllowedStateForRecheck(c.getId(), packageName);
            shouldInvalidateCache |= c.recheckOverride(packageName, allowedState, versionCode);
        }
        if (shouldInvalidateCache) {
            invalidateCache();
        }
    }

    private Long getVersionCodeOrNull(String packageName) {
        try {
            ApplicationInfo applicationInfo = this.mContext.getPackageManager().getApplicationInfo(packageName, 4194304);
            return Long.valueOf(applicationInfo.longVersionCode);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerContentObserver() {
        this.mOverrideValidator.registerContentObserver();
    }
}
