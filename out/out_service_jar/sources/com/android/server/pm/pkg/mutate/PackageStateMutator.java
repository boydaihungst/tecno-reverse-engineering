package com.android.server.pm.pkg.mutate;

import android.content.ComponentName;
import android.content.pm.overlay.OverlayPaths;
import android.util.ArraySet;
import com.android.server.pm.PackageSetting;
import com.android.server.pm.pkg.PackageUserStateImpl;
import com.android.server.pm.pkg.SuspendParams;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
/* loaded from: classes2.dex */
public class PackageStateMutator {
    private static final AtomicLong sStateChangeSequence = new AtomicLong();
    private final Function<String, PackageSetting> mActiveStateFunction;
    private final Function<String, PackageSetting> mDisabledStateFunction;
    private final StateWriteWrapper mStateWrite = new StateWriteWrapper();

    public PackageStateMutator(Function<String, PackageSetting> activeStateFunction, Function<String, PackageSetting> disabledStateFunction) {
        this.mActiveStateFunction = activeStateFunction;
        this.mDisabledStateFunction = disabledStateFunction;
    }

    public static void onPackageStateChanged() {
        sStateChangeSequence.incrementAndGet();
    }

    public PackageStateWrite forPackage(String packageName) {
        return this.mStateWrite.setState(this.mActiveStateFunction.apply(packageName));
    }

    public PackageStateWrite forPackageNullable(String packageName) {
        PackageSetting packageState = this.mActiveStateFunction.apply(packageName);
        this.mStateWrite.setState(packageState);
        if (packageState == null) {
            return null;
        }
        return this.mStateWrite.setState(packageState);
    }

    public PackageStateWrite forDisabledSystemPackage(String packageName) {
        return this.mStateWrite.setState(this.mDisabledStateFunction.apply(packageName));
    }

    public PackageStateWrite forDisabledSystemPackageNullable(String packageName) {
        PackageSetting packageState = this.mDisabledStateFunction.apply(packageName);
        if (packageState == null) {
            return null;
        }
        return this.mStateWrite.setState(packageState);
    }

    public InitialState initialState(int changedPackagesSequenceNumber) {
        return new InitialState(changedPackagesSequenceNumber, sStateChangeSequence.get());
    }

    public Result generateResult(InitialState state, int changedPackagesSequenceNumber) {
        if (state == null) {
            return Result.SUCCESS;
        }
        boolean packagesChanged = changedPackagesSequenceNumber != state.mPackageSequence;
        boolean stateChanged = sStateChangeSequence.get() != state.mStateSequence;
        if (packagesChanged && stateChanged) {
            return Result.PACKAGES_AND_STATE_CHANGED;
        }
        if (packagesChanged) {
            return Result.PACKAGES_CHANGED;
        }
        if (stateChanged) {
            return Result.STATE_CHANGED;
        }
        return Result.SUCCESS;
    }

    /* loaded from: classes2.dex */
    public static class InitialState {
        private final int mPackageSequence;
        private final long mStateSequence;

        public InitialState(int packageSequence, long stateSequence) {
            this.mPackageSequence = packageSequence;
            this.mStateSequence = stateSequence;
        }
    }

    /* loaded from: classes2.dex */
    public static class Result {
        private final boolean mCommitted;
        private final boolean mPackagesChanged;
        private final boolean mSpecificPackageNull;
        private final boolean mStateChanged;
        public static final Result SUCCESS = new Result(true, false, false, false);
        public static final Result PACKAGES_CHANGED = new Result(false, true, false, false);
        public static final Result STATE_CHANGED = new Result(false, false, true, false);
        public static final Result PACKAGES_AND_STATE_CHANGED = new Result(false, true, true, false);
        public static final Result SPECIFIC_PACKAGE_NULL = new Result(false, false, true, true);

        public Result(boolean committed, boolean packagesChanged, boolean stateChanged, boolean specificPackageNull) {
            this.mCommitted = committed;
            this.mPackagesChanged = packagesChanged;
            this.mStateChanged = stateChanged;
            this.mSpecificPackageNull = specificPackageNull;
        }

        public boolean isCommitted() {
            return this.mCommitted;
        }

        public boolean isPackagesChanged() {
            return this.mPackagesChanged;
        }

        public boolean isStateChanged() {
            return this.mStateChanged;
        }

        public boolean isSpecificPackageNull() {
            return this.mSpecificPackageNull;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class StateWriteWrapper implements PackageStateWrite {
        private PackageSetting mState;
        private final UserStateWriteWrapper mUserStateWrite;

        private StateWriteWrapper() {
            this.mUserStateWrite = new UserStateWriteWrapper();
        }

        public StateWriteWrapper setState(PackageSetting state) {
            this.mState = state;
            return this;
        }

        @Override // com.android.server.pm.pkg.mutate.PackageStateWrite
        public PackageUserStateWrite userState(int userId) {
            UserStateWriteWrapper userStateWriteWrapper = this.mUserStateWrite;
            PackageSetting packageSetting = this.mState;
            return userStateWriteWrapper.setStates(packageSetting == null ? null : packageSetting.getOrCreateUserState(userId));
        }

        @Override // com.android.server.pm.pkg.mutate.PackageStateWrite
        public void onChanged() {
            PackageSetting packageSetting = this.mState;
            if (packageSetting != null) {
                packageSetting.onChanged();
            }
        }

        @Override // com.android.server.pm.pkg.mutate.PackageStateWrite
        public PackageStateWrite setLastPackageUsageTime(int reason, long timeInMillis) {
            PackageSetting packageSetting = this.mState;
            if (packageSetting != null) {
                packageSetting.getTransientState().setLastPackageUsageTimeInMills(reason, timeInMillis);
            }
            return this;
        }

        @Override // com.android.server.pm.pkg.mutate.PackageStateWrite
        public PackageStateWrite setHiddenUntilInstalled(boolean value) {
            PackageSetting packageSetting = this.mState;
            if (packageSetting != null) {
                packageSetting.getTransientState().setHiddenUntilInstalled(value);
            }
            return this;
        }

        @Override // com.android.server.pm.pkg.mutate.PackageStateWrite
        public PackageStateWrite setRequiredForSystemUser(boolean requiredForSystemUser) {
            PackageSetting packageSetting = this.mState;
            if (packageSetting != null) {
                if (requiredForSystemUser) {
                    packageSetting.setPrivateFlags(packageSetting.getPrivateFlags() | 512);
                } else {
                    packageSetting.setPrivateFlags(packageSetting.getPrivateFlags() & (-513));
                }
            }
            return this;
        }

        @Override // com.android.server.pm.pkg.mutate.PackageStateWrite
        public PackageStateWrite setMimeGroup(String mimeGroup, ArraySet<String> mimeTypes) {
            PackageSetting packageSetting = this.mState;
            if (packageSetting != null) {
                packageSetting.setMimeGroup(mimeGroup, mimeTypes);
            }
            return this;
        }

        @Override // com.android.server.pm.pkg.mutate.PackageStateWrite
        public PackageStateWrite setCategoryOverride(int category) {
            PackageSetting packageSetting = this.mState;
            if (packageSetting != null) {
                packageSetting.setCategoryOverride(category);
            }
            return this;
        }

        @Override // com.android.server.pm.pkg.mutate.PackageStateWrite
        public PackageStateWrite setUpdateAvailable(boolean updateAvailable) {
            PackageSetting packageSetting = this.mState;
            if (packageSetting != null) {
                packageSetting.setUpdateAvailable(updateAvailable);
            }
            return this;
        }

        @Override // com.android.server.pm.pkg.mutate.PackageStateWrite
        public PackageStateWrite setLoadingProgress(float progress) {
            PackageSetting packageSetting = this.mState;
            if (packageSetting != null) {
                packageSetting.setLoadingProgress(progress);
            }
            return this;
        }

        @Override // com.android.server.pm.pkg.mutate.PackageStateWrite
        public PackageStateWrite setOverrideSeInfo(String newSeInfo) {
            PackageSetting packageSetting = this.mState;
            if (packageSetting != null) {
                packageSetting.getTransientState().setOverrideSeInfo(newSeInfo);
            }
            return this;
        }

        @Override // com.android.server.pm.pkg.mutate.PackageStateWrite
        public PackageStateWrite setInstaller(String installerPackageName) {
            PackageSetting packageSetting = this.mState;
            if (packageSetting != null) {
                packageSetting.setInstallerPackageName(installerPackageName);
            }
            return this;
        }

        /* loaded from: classes2.dex */
        private static class UserStateWriteWrapper implements PackageUserStateWrite {
            private PackageUserStateImpl mUserState;

            private UserStateWriteWrapper() {
            }

            public UserStateWriteWrapper setStates(PackageUserStateImpl userState) {
                this.mUserState = userState;
                return this;
            }

            @Override // com.android.server.pm.pkg.mutate.PackageUserStateWrite
            public PackageUserStateWrite setInstalled(boolean installed) {
                PackageUserStateImpl packageUserStateImpl = this.mUserState;
                if (packageUserStateImpl != null) {
                    packageUserStateImpl.setInstalled(installed);
                }
                return this;
            }

            @Override // com.android.server.pm.pkg.mutate.PackageUserStateWrite
            public PackageUserStateWrite setUninstallReason(int reason) {
                PackageUserStateImpl packageUserStateImpl = this.mUserState;
                if (packageUserStateImpl != null) {
                    packageUserStateImpl.setUninstallReason(reason);
                }
                return this;
            }

            @Override // com.android.server.pm.pkg.mutate.PackageUserStateWrite
            public PackageUserStateWrite setDistractionFlags(int restrictionFlags) {
                PackageUserStateImpl packageUserStateImpl = this.mUserState;
                if (packageUserStateImpl != null) {
                    packageUserStateImpl.setDistractionFlags(restrictionFlags);
                }
                return this;
            }

            @Override // com.android.server.pm.pkg.mutate.PackageUserStateWrite
            public PackageUserStateWrite putSuspendParams(String suspendingPackage, SuspendParams suspendParams) {
                PackageUserStateImpl packageUserStateImpl = this.mUserState;
                if (packageUserStateImpl != null) {
                    packageUserStateImpl.putSuspendParams(suspendingPackage, suspendParams);
                }
                return this;
            }

            @Override // com.android.server.pm.pkg.mutate.PackageUserStateWrite
            public PackageUserStateWrite removeSuspension(String suspendingPackage) {
                PackageUserStateImpl packageUserStateImpl = this.mUserState;
                if (packageUserStateImpl != null) {
                    packageUserStateImpl.removeSuspension(suspendingPackage);
                }
                return this;
            }

            @Override // com.android.server.pm.pkg.mutate.PackageUserStateWrite
            public PackageUserStateWrite setHidden(boolean hidden) {
                PackageUserStateImpl packageUserStateImpl = this.mUserState;
                if (packageUserStateImpl != null) {
                    packageUserStateImpl.setHidden(hidden);
                }
                return this;
            }

            @Override // com.android.server.pm.pkg.mutate.PackageUserStateWrite
            public PackageUserStateWrite setStopped(boolean stopped) {
                PackageUserStateImpl packageUserStateImpl = this.mUserState;
                if (packageUserStateImpl != null) {
                    packageUserStateImpl.setStopped(stopped);
                }
                return this;
            }

            @Override // com.android.server.pm.pkg.mutate.PackageUserStateWrite
            public PackageUserStateWrite setNotLaunched(boolean notLaunched) {
                PackageUserStateImpl packageUserStateImpl = this.mUserState;
                if (packageUserStateImpl != null) {
                    packageUserStateImpl.setNotLaunched(notLaunched);
                }
                return this;
            }

            @Override // com.android.server.pm.pkg.mutate.PackageUserStateWrite
            public PackageUserStateWrite setOverlayPaths(OverlayPaths overlayPaths) {
                PackageUserStateImpl packageUserStateImpl = this.mUserState;
                if (packageUserStateImpl != null) {
                    packageUserStateImpl.setOverlayPaths(overlayPaths);
                }
                return this;
            }

            @Override // com.android.server.pm.pkg.mutate.PackageUserStateWrite
            public PackageUserStateWrite setOverlayPathsForLibrary(String libraryName, OverlayPaths overlayPaths) {
                PackageUserStateImpl packageUserStateImpl = this.mUserState;
                if (packageUserStateImpl != null) {
                    packageUserStateImpl.setSharedLibraryOverlayPaths(libraryName, overlayPaths);
                }
                return this;
            }

            @Override // com.android.server.pm.pkg.mutate.PackageUserStateWrite
            public PackageUserStateWrite setHarmfulAppWarning(String warning) {
                PackageUserStateImpl packageUserStateImpl = this.mUserState;
                if (packageUserStateImpl != null) {
                    packageUserStateImpl.setHarmfulAppWarning(warning);
                }
                return this;
            }

            @Override // com.android.server.pm.pkg.mutate.PackageUserStateWrite
            public PackageUserStateWrite setSplashScreenTheme(String theme) {
                PackageUserStateImpl packageUserStateImpl = this.mUserState;
                if (packageUserStateImpl != null) {
                    packageUserStateImpl.setSplashScreenTheme(theme);
                }
                return this;
            }

            @Override // com.android.server.pm.pkg.mutate.PackageUserStateWrite
            public PackageUserStateWrite setComponentLabelIcon(ComponentName componentName, String nonLocalizedLabel, Integer icon) {
                PackageUserStateImpl packageUserStateImpl = this.mUserState;
                if (packageUserStateImpl != null) {
                    packageUserStateImpl.overrideLabelAndIcon(componentName, nonLocalizedLabel, icon);
                    return null;
                }
                return null;
            }
        }
    }
}
