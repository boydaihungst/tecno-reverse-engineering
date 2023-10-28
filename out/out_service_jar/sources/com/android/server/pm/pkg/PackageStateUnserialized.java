package com.android.server.pm.pkg;

import android.content.pm.SharedLibraryInfo;
import com.android.server.pm.PackageSetting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
/* loaded from: classes2.dex */
public class PackageStateUnserialized {
    private boolean hiddenUntilInstalled;
    private volatile long[] lastPackageUsageTimeInMills;
    private final PackageSetting mPackageSetting;
    private String overrideSeInfo;
    private boolean updatedSystemApp;
    private List<SharedLibraryInfo> usesLibraryInfos = Collections.emptyList();
    private List<String> usesLibraryFiles = Collections.emptyList();

    public PackageStateUnserialized(PackageSetting packageSetting) {
        this.mPackageSetting = packageSetting;
    }

    private long[] lazyInitLastPackageUsageTimeInMills() {
        return new long[8];
    }

    public PackageStateUnserialized setLastPackageUsageTimeInMills(int reason, long time) {
        if (reason < 0) {
            return this;
        }
        if (reason >= 8) {
            return this;
        }
        getLastPackageUsageTimeInMills()[reason] = time;
        return this;
    }

    public long getLatestPackageUseTimeInMills() {
        long[] lastPackageUsageTimeInMills;
        long latestUse = 0;
        for (long use : getLastPackageUsageTimeInMills()) {
            latestUse = Math.max(latestUse, use);
        }
        return latestUse;
    }

    public long getLatestForegroundPackageUseTimeInMills() {
        int[] foregroundReasons = {0, 2};
        long latestUse = 0;
        for (int reason : foregroundReasons) {
            latestUse = Math.max(latestUse, getLastPackageUsageTimeInMills()[reason]);
        }
        return latestUse;
    }

    public void updateFrom(PackageStateUnserialized other) {
        this.hiddenUntilInstalled = other.hiddenUntilInstalled;
        if (!other.usesLibraryInfos.isEmpty()) {
            this.usesLibraryInfos = new ArrayList(other.usesLibraryInfos);
        }
        if (!other.usesLibraryFiles.isEmpty()) {
            this.usesLibraryFiles = new ArrayList(other.usesLibraryFiles);
        }
        this.updatedSystemApp = other.updatedSystemApp;
        this.lastPackageUsageTimeInMills = other.lastPackageUsageTimeInMills;
        this.overrideSeInfo = other.overrideSeInfo;
        this.mPackageSetting.onChanged();
    }

    public List<SharedLibraryInfo> getNonNativeUsesLibraryInfos() {
        return (List) getUsesLibraryInfos().stream().filter(new Predicate() { // from class: com.android.server.pm.pkg.PackageStateUnserialized$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return PackageStateUnserialized.lambda$getNonNativeUsesLibraryInfos$0((SharedLibraryInfo) obj);
            }
        }).collect(Collectors.toList());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getNonNativeUsesLibraryInfos$0(SharedLibraryInfo l) {
        return !l.isNative();
    }

    public PackageStateUnserialized setHiddenUntilInstalled(boolean value) {
        this.hiddenUntilInstalled = value;
        this.mPackageSetting.onChanged();
        return this;
    }

    public PackageStateUnserialized setUsesLibraryInfos(List<SharedLibraryInfo> value) {
        this.usesLibraryInfos = value;
        this.mPackageSetting.onChanged();
        return this;
    }

    public PackageStateUnserialized setUsesLibraryFiles(List<String> value) {
        this.usesLibraryFiles = value;
        this.mPackageSetting.onChanged();
        return this;
    }

    public PackageStateUnserialized setUpdatedSystemApp(boolean value) {
        this.updatedSystemApp = value;
        this.mPackageSetting.onChanged();
        return this;
    }

    public PackageStateUnserialized setLastPackageUsageTimeInMills(long... value) {
        this.lastPackageUsageTimeInMills = value;
        this.mPackageSetting.onChanged();
        return this;
    }

    public PackageStateUnserialized setOverrideSeInfo(String value) {
        this.overrideSeInfo = value;
        this.mPackageSetting.onChanged();
        return this;
    }

    public boolean isHiddenUntilInstalled() {
        return this.hiddenUntilInstalled;
    }

    public List<SharedLibraryInfo> getUsesLibraryInfos() {
        return this.usesLibraryInfos;
    }

    public List<String> getUsesLibraryFiles() {
        return this.usesLibraryFiles;
    }

    public boolean isUpdatedSystemApp() {
        return this.updatedSystemApp;
    }

    public long[] getLastPackageUsageTimeInMills() {
        long[] _lastPackageUsageTimeInMills = this.lastPackageUsageTimeInMills;
        if (_lastPackageUsageTimeInMills == null) {
            synchronized (this) {
                _lastPackageUsageTimeInMills = this.lastPackageUsageTimeInMills;
                if (_lastPackageUsageTimeInMills == null) {
                    long[] lazyInitLastPackageUsageTimeInMills = lazyInitLastPackageUsageTimeInMills();
                    this.lastPackageUsageTimeInMills = lazyInitLastPackageUsageTimeInMills;
                    _lastPackageUsageTimeInMills = lazyInitLastPackageUsageTimeInMills;
                }
            }
        }
        return _lastPackageUsageTimeInMills;
    }

    public String getOverrideSeInfo() {
        return this.overrideSeInfo;
    }

    public PackageSetting getPackageSetting() {
        return this.mPackageSetting;
    }

    @Deprecated
    private void __metadata() {
    }
}
