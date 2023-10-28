package com.android.server.pm.pkg;

import android.content.pm.SharedLibraryInfo;
import android.content.pm.SigningInfo;
import android.content.pm.overlay.OverlayPaths;
import android.util.ArraySet;
import android.util.SparseArray;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
/* loaded from: classes2.dex */
public class PackageStateImpl implements PackageState {
    private final AndroidPackageApi mAndroidPackage;
    private final int mAppId;
    private int mBooleans;
    private final int mCategoryOverride;
    private final String mCpuAbiOverride;
    private final boolean mHasSharedUser;
    private final long mLastModifiedTime;
    private final long[] mLastPackageUsageTime;
    private final long mLastUpdateTime;
    private final long mLongVersionCode;
    private final Map<String, Set<String>> mMimeGroups;
    private final String mPackageName;
    private final File mPath;
    private final String mPrimaryCpuAbi;
    private final String mSecondaryCpuAbi;
    private final int mSharedUserAppId;
    private final SigningInfo mSigningInfo;
    private final SparseArray<PackageUserState> mUserStates;
    private final List<String> mUsesLibraryFiles;
    private final List<SharedLibraryInfo> mUsesLibraryInfos;
    private final String[] mUsesSdkLibraries;
    private final long[] mUsesSdkLibrariesVersionsMajor;
    private final String[] mUsesStaticLibraries;
    private final long[] mUsesStaticLibrariesVersions;
    private final String mVolumeUuid;

    public static PackageState copy(PackageStateInternal pkgSetting) {
        return new PackageStateImpl(pkgSetting, pkgSetting.getPkg());
    }

    /* loaded from: classes2.dex */
    private static class Booleans {
        private static final int EXTERNAL_STORAGE = 2;
        private static final int FORCE_QUERYABLE_OVERRIDE = 512;
        private static final int HIDDEN_UNTIL_INSTALLED = 1024;
        private static final int INSTALL_PERMISSIONS_FIXED = 2048;
        private static final int ODM = 256;
        private static final int OEM = 8;
        private static final int PRIVILEGED = 4;
        private static final int PRODUCT = 32;
        private static final int REQUIRED_FOR_SYSTEM_USER = 128;
        private static final int SYSTEM = 1;
        private static final int SYSTEM_EXT = 64;
        private static final int UPDATED_SYSTEM_APP = 8192;
        private static final int UPDATE_AVAILABLE = 4096;
        private static final int VENDOR = 16;

        /* loaded from: classes2.dex */
        public @interface Flags {
        }

        private Booleans() {
        }
    }

    private void setBoolean(int flag, boolean value) {
        if (value) {
            this.mBooleans |= flag;
        } else {
            this.mBooleans &= ~flag;
        }
    }

    private boolean getBoolean(int flag) {
        return (this.mBooleans & flag) != 0;
    }

    private PackageStateImpl(PackageState pkgState, AndroidPackage pkg) {
        this.mAndroidPackage = pkg;
        setBoolean(1, pkgState.isSystem());
        setBoolean(2, pkgState.isExternalStorage());
        setBoolean(4, pkgState.isPrivileged());
        setBoolean(8, pkgState.isOem());
        setBoolean(16, pkgState.isVendor());
        setBoolean(32, pkgState.isProduct());
        setBoolean(64, pkgState.isSystemExt());
        setBoolean(128, pkgState.isRequiredForSystemUser());
        setBoolean(256, pkgState.isOdm());
        this.mPackageName = pkgState.getPackageName();
        this.mVolumeUuid = pkgState.getVolumeUuid();
        this.mAppId = pkgState.getAppId();
        this.mCategoryOverride = pkgState.getCategoryOverride();
        this.mCpuAbiOverride = pkgState.getCpuAbiOverride();
        this.mLastModifiedTime = pkgState.getLastModifiedTime();
        this.mLastUpdateTime = pkgState.getLastUpdateTime();
        this.mLongVersionCode = pkgState.getVersionCode();
        this.mMimeGroups = pkgState.getMimeGroups();
        this.mPath = pkgState.getPath();
        this.mPrimaryCpuAbi = pkgState.getPrimaryCpuAbi();
        this.mSecondaryCpuAbi = pkgState.getSecondaryCpuAbi();
        this.mHasSharedUser = pkgState.hasSharedUser();
        this.mSharedUserAppId = pkgState.getSharedUserAppId();
        this.mUsesSdkLibraries = pkgState.getUsesSdkLibraries();
        this.mUsesSdkLibrariesVersionsMajor = pkgState.getUsesSdkLibrariesVersionsMajor();
        this.mUsesStaticLibraries = pkgState.getUsesStaticLibraries();
        this.mUsesStaticLibrariesVersions = pkgState.getUsesStaticLibrariesVersions();
        this.mUsesLibraryInfos = pkgState.getUsesLibraryInfos();
        this.mUsesLibraryFiles = pkgState.getUsesLibraryFiles();
        setBoolean(512, pkgState.isForceQueryableOverride());
        setBoolean(1024, pkgState.isHiddenUntilInstalled());
        setBoolean(2048, pkgState.isInstallPermissionsFixed());
        setBoolean(4096, pkgState.isUpdateAvailable());
        this.mLastPackageUsageTime = pkgState.getLastPackageUsageTime();
        setBoolean(8192, pkgState.isUpdatedSystemApp());
        this.mSigningInfo = pkgState.getSigningInfo();
        SparseArray<? extends PackageUserState> userStates = pkgState.getUserStates();
        int userStatesSize = userStates.size();
        this.mUserStates = new SparseArray<>(userStatesSize);
        for (int index = 0; index < userStatesSize; index++) {
            SparseArray<PackageUserState> sparseArray = this.mUserStates;
            sparseArray.put(sparseArray.keyAt(index), UserStateImpl.copy(this.mUserStates.valueAt(index)));
        }
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean isExternalStorage() {
        return getBoolean(2);
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean isForceQueryableOverride() {
        return getBoolean(512);
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean isHiddenUntilInstalled() {
        return getBoolean(1024);
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean isInstallPermissionsFixed() {
        return getBoolean(2048);
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean isOdm() {
        return getBoolean(256);
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean isOem() {
        return getBoolean(8);
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean isPrivileged() {
        return getBoolean(4);
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean isProduct() {
        return getBoolean(32);
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean isRequiredForSystemUser() {
        return getBoolean(128);
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean isSystem() {
        return getBoolean(1);
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean isSystemExt() {
        return getBoolean(64);
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean isUpdateAvailable() {
        return getBoolean(4096);
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean isUpdatedSystemApp() {
        return getBoolean(8192);
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean isVendor() {
        return getBoolean(16);
    }

    @Override // com.android.server.pm.pkg.PackageState
    public long getVersionCode() {
        return this.mLongVersionCode;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean hasSharedUser() {
        return this.mHasSharedUser;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public int getSharedUserAppId() {
        return this.mSharedUserAppId;
    }

    /* loaded from: classes2.dex */
    public static class UserStateImpl implements PackageUserState {
        private int mBooleans;
        private final long mCeDataInode;
        private final ArraySet<String> mDisabledComponents;
        private final int mDistractionFlags;
        private final ArraySet<String> mEnabledComponents;
        private final int mEnabledState;
        private final long mFirstInstallTime;
        private final String mHarmfulAppWarning;
        private final int mInstallReason;
        private final String mLastDisableAppCaller;
        private final OverlayPaths mOverlayPaths;
        private final Map<String, OverlayPaths> mSharedLibraryOverlayPaths;
        private final String mSplashScreenTheme;
        private final int mUninstallReason;

        public static PackageUserState copy(PackageUserState state) {
            return new UserStateImpl(state);
        }

        /* loaded from: classes2.dex */
        private static class Booleans {
            private static final int HIDDEN = 1;
            private static final int INSTALLED = 2;
            private static final int INSTANT_APP = 4;
            private static final int NOT_LAUNCHED = 8;
            private static final int STOPPED = 16;
            private static final int SUSPENDED = 32;
            private static final int VIRTUAL_PRELOAD = 64;

            /* loaded from: classes2.dex */
            public @interface Flags {
            }

            private Booleans() {
            }
        }

        private void setBoolean(int flag, boolean value) {
            if (value) {
                this.mBooleans |= flag;
            } else {
                this.mBooleans &= ~flag;
            }
        }

        private boolean getBoolean(int flag) {
            return (this.mBooleans & flag) != 0;
        }

        private UserStateImpl(PackageUserState userState) {
            this.mCeDataInode = userState.getCeDataInode();
            this.mDisabledComponents = userState.getDisabledComponents();
            this.mDistractionFlags = userState.getDistractionFlags();
            this.mEnabledComponents = userState.getEnabledComponents();
            this.mEnabledState = userState.getEnabledState();
            this.mHarmfulAppWarning = userState.getHarmfulAppWarning();
            this.mInstallReason = userState.getInstallReason();
            this.mLastDisableAppCaller = userState.getLastDisableAppCaller();
            this.mOverlayPaths = userState.getOverlayPaths();
            this.mSharedLibraryOverlayPaths = userState.getSharedLibraryOverlayPaths();
            this.mUninstallReason = userState.getUninstallReason();
            this.mSplashScreenTheme = userState.getSplashScreenTheme();
            setBoolean(1, userState.isHidden());
            setBoolean(2, userState.isInstalled());
            setBoolean(4, userState.isInstantApp());
            setBoolean(8, userState.isNotLaunched());
            setBoolean(16, userState.isStopped());
            setBoolean(32, userState.isSuspended());
            setBoolean(64, userState.isVirtualPreload());
            this.mFirstInstallTime = userState.getFirstInstallTime();
        }

        @Override // com.android.server.pm.pkg.PackageUserState
        public boolean isHidden() {
            return getBoolean(1);
        }

        @Override // com.android.server.pm.pkg.PackageUserState
        public boolean isInstalled() {
            return getBoolean(2);
        }

        @Override // com.android.server.pm.pkg.PackageUserState
        public boolean isInstantApp() {
            return getBoolean(4);
        }

        @Override // com.android.server.pm.pkg.PackageUserState
        public boolean isNotLaunched() {
            return getBoolean(8);
        }

        @Override // com.android.server.pm.pkg.PackageUserState
        public boolean isStopped() {
            return getBoolean(16);
        }

        @Override // com.android.server.pm.pkg.PackageUserState
        public boolean isSuspended() {
            return getBoolean(32);
        }

        @Override // com.android.server.pm.pkg.PackageUserState
        public boolean isVirtualPreload() {
            return getBoolean(64);
        }

        @Override // com.android.server.pm.pkg.PackageUserState
        public boolean isComponentEnabled(String componentName) {
            return this.mEnabledComponents.contains(componentName);
        }

        @Override // com.android.server.pm.pkg.PackageUserState
        public boolean isComponentDisabled(String componentName) {
            return this.mDisabledComponents.contains(componentName);
        }

        @Override // com.android.server.pm.pkg.PackageUserState
        public OverlayPaths getAllOverlayPaths() {
            if (this.mOverlayPaths == null && this.mSharedLibraryOverlayPaths == null) {
                return null;
            }
            OverlayPaths.Builder newPaths = new OverlayPaths.Builder();
            newPaths.addAll(this.mOverlayPaths);
            Map<String, OverlayPaths> map = this.mSharedLibraryOverlayPaths;
            if (map != null) {
                for (OverlayPaths libOverlayPaths : map.values()) {
                    newPaths.addAll(libOverlayPaths);
                }
            }
            return newPaths.build();
        }

        public int getBooleans() {
            return this.mBooleans;
        }

        @Override // com.android.server.pm.pkg.PackageUserState
        public long getCeDataInode() {
            return this.mCeDataInode;
        }

        @Override // com.android.server.pm.pkg.PackageUserState
        public ArraySet<String> getDisabledComponents() {
            return this.mDisabledComponents;
        }

        @Override // com.android.server.pm.pkg.PackageUserState
        public int getDistractionFlags() {
            return this.mDistractionFlags;
        }

        @Override // com.android.server.pm.pkg.PackageUserState
        public ArraySet<String> getEnabledComponents() {
            return this.mEnabledComponents;
        }

        @Override // com.android.server.pm.pkg.PackageUserState
        public int getEnabledState() {
            return this.mEnabledState;
        }

        @Override // com.android.server.pm.pkg.PackageUserState
        public String getHarmfulAppWarning() {
            return this.mHarmfulAppWarning;
        }

        @Override // com.android.server.pm.pkg.PackageUserState
        public int getInstallReason() {
            return this.mInstallReason;
        }

        @Override // com.android.server.pm.pkg.PackageUserState
        public String getLastDisableAppCaller() {
            return this.mLastDisableAppCaller;
        }

        @Override // com.android.server.pm.pkg.PackageUserState
        public OverlayPaths getOverlayPaths() {
            return this.mOverlayPaths;
        }

        @Override // com.android.server.pm.pkg.PackageUserState
        public Map<String, OverlayPaths> getSharedLibraryOverlayPaths() {
            return this.mSharedLibraryOverlayPaths;
        }

        @Override // com.android.server.pm.pkg.PackageUserState
        public int getUninstallReason() {
            return this.mUninstallReason;
        }

        @Override // com.android.server.pm.pkg.PackageUserState
        public String getSplashScreenTheme() {
            return this.mSplashScreenTheme;
        }

        @Override // com.android.server.pm.pkg.PackageUserState
        public long getFirstInstallTime() {
            return this.mFirstInstallTime;
        }

        public UserStateImpl setBooleans(int value) {
            this.mBooleans = value;
            return this;
        }

        @Deprecated
        private void __metadata() {
        }
    }

    public int getBooleans() {
        return this.mBooleans;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public AndroidPackageApi getAndroidPackage() {
        return this.mAndroidPackage;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public String getPackageName() {
        return this.mPackageName;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public String getVolumeUuid() {
        return this.mVolumeUuid;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public int getAppId() {
        return this.mAppId;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public int getCategoryOverride() {
        return this.mCategoryOverride;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public String getCpuAbiOverride() {
        return this.mCpuAbiOverride;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public long getLastModifiedTime() {
        return this.mLastModifiedTime;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public long getLastUpdateTime() {
        return this.mLastUpdateTime;
    }

    public long getLongVersionCode() {
        return this.mLongVersionCode;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public Map<String, Set<String>> getMimeGroups() {
        return this.mMimeGroups;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public File getPath() {
        return this.mPath;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public String getPrimaryCpuAbi() {
        return this.mPrimaryCpuAbi;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public String getSecondaryCpuAbi() {
        return this.mSecondaryCpuAbi;
    }

    public boolean isHasSharedUser() {
        return this.mHasSharedUser;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public String[] getUsesSdkLibraries() {
        return this.mUsesSdkLibraries;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public long[] getUsesSdkLibrariesVersionsMajor() {
        return this.mUsesSdkLibrariesVersionsMajor;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public String[] getUsesStaticLibraries() {
        return this.mUsesStaticLibraries;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public long[] getUsesStaticLibrariesVersions() {
        return this.mUsesStaticLibrariesVersions;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public List<SharedLibraryInfo> getUsesLibraryInfos() {
        return this.mUsesLibraryInfos;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public List<String> getUsesLibraryFiles() {
        return this.mUsesLibraryFiles;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public long[] getLastPackageUsageTime() {
        return this.mLastPackageUsageTime;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public SigningInfo getSigningInfo() {
        return this.mSigningInfo;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public SparseArray<PackageUserState> getUserStates() {
        return this.mUserStates;
    }

    public PackageStateImpl setBooleans(int value) {
        this.mBooleans = value;
        return this;
    }

    @Deprecated
    private void __metadata() {
    }
}
