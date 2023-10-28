package com.android.server.pm.permission;

import android.content.pm.PermissionInfo;
import android.permission.PermissionManagerInternal;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
/* loaded from: classes2.dex */
public interface PermissionManagerServiceInternal extends PermissionManagerInternal, LegacyPermissionDataProvider {

    /* loaded from: classes2.dex */
    public interface HotwordDetectionServiceProvider {
        int getUid();
    }

    /* loaded from: classes2.dex */
    public interface OnRuntimePermissionStateChangedListener {
        void onRuntimePermissionStateChanged(String str, int i);
    }

    void addOnRuntimePermissionStateChangedListener(OnRuntimePermissionStateChangedListener onRuntimePermissionStateChangedListener);

    int checkPermission(String str, String str2, int i);

    int checkUidPermission(int i, String str);

    ArrayList<PermissionInfo> getAllPermissionsWithProtection(int i);

    ArrayList<PermissionInfo> getAllPermissionsWithProtectionFlags(int i);

    String[] getAppOpPermissionPackages(String str);

    DefaultPermissionGrantPolicy getDefaultPermissionGrantPolicy();

    List<String> getDelegatedShellPermissions();

    Set<String> getGrantedPermissions(String str, int i);

    HotwordDetectionServiceProvider getHotwordDetectionServiceProvider();

    int[] getPermissionGids(String str, int i);

    Permission getPermissionTEMP(String str);

    void grantRequestedRuntimePermissionsInternal(AndroidPackage androidPackage, List<String> list, int i);

    boolean isPermissionsReviewRequired(String str, int i);

    void onPackageAdded(AndroidPackage androidPackage, boolean z, AndroidPackage androidPackage2);

    void onPackageInstalled(AndroidPackage androidPackage, int i, PackageInstalledParams packageInstalledParams, int i2);

    void onPackageRemoved(AndroidPackage androidPackage);

    void onPackageUninstalled(String str, int i, AndroidPackage androidPackage, List<AndroidPackage> list, int i2);

    void onStorageVolumeMounted(String str, boolean z);

    void onSystemReady();

    void onUserCreated(int i);

    void onUserRemoved(int i);

    void readLegacyPermissionStateTEMP();

    void readLegacyPermissionsTEMP(LegacyPermissionSettings legacyPermissionSettings);

    void removeOnRuntimePermissionStateChangedListener(OnRuntimePermissionStateChangedListener onRuntimePermissionStateChangedListener);

    void resetRuntimePermissions(AndroidPackage androidPackage, int i);

    void setHotwordDetectionServiceProvider(HotwordDetectionServiceProvider hotwordDetectionServiceProvider);

    void startShellPermissionIdentityDelegation(int i, String str, List<String> list);

    void stopShellPermissionIdentityDelegation();

    void writeLegacyPermissionStateTEMP();

    void writeLegacyPermissionsTEMP(LegacyPermissionSettings legacyPermissionSettings);

    /* loaded from: classes2.dex */
    public static final class PackageInstalledParams {
        public static final PackageInstalledParams DEFAULT = new Builder().build();
        private final List<String> mAllowlistedRestrictedPermissions;
        private final int mAutoRevokePermissionsMode;
        private final List<String> mGrantedPermissions;

        private PackageInstalledParams(List<String> grantedPermissions, List<String> allowlistedRestrictedPermissions, int autoRevokePermissionsMode) {
            this.mGrantedPermissions = grantedPermissions;
            this.mAllowlistedRestrictedPermissions = allowlistedRestrictedPermissions;
            this.mAutoRevokePermissionsMode = autoRevokePermissionsMode;
        }

        public List<String> getGrantedPermissions() {
            return this.mGrantedPermissions;
        }

        public List<String> getAllowlistedRestrictedPermissions() {
            return this.mAllowlistedRestrictedPermissions;
        }

        public int getAutoRevokePermissionsMode() {
            return this.mAutoRevokePermissionsMode;
        }

        /* loaded from: classes2.dex */
        public static final class Builder {
            private List<String> mGrantedPermissions = Collections.emptyList();
            private List<String> mAllowlistedRestrictedPermissions = Collections.emptyList();
            private int mAutoRevokePermissionsMode = 3;

            public void setGrantedPermissions(List<String> grantedPermissions) {
                Objects.requireNonNull(grantedPermissions);
                this.mGrantedPermissions = new ArrayList(grantedPermissions);
            }

            public void setAllowlistedRestrictedPermissions(List<String> allowlistedRestrictedPermissions) {
                Objects.requireNonNull(this.mGrantedPermissions);
                this.mAllowlistedRestrictedPermissions = new ArrayList(allowlistedRestrictedPermissions);
            }

            public void setAutoRevokePermissionsMode(int autoRevokePermissionsMode) {
                this.mAutoRevokePermissionsMode = autoRevokePermissionsMode;
            }

            public PackageInstalledParams build() {
                return new PackageInstalledParams(this.mGrantedPermissions, this.mAllowlistedRestrictedPermissions, this.mAutoRevokePermissionsMode);
            }
        }
    }
}
