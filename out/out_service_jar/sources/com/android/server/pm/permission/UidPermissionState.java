package com.android.server.pm.permission;

import android.content.pm.PackageManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IntArray;
import com.mediatek.cta.CtaManagerFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
/* loaded from: classes2.dex */
public final class UidPermissionState {
    private boolean isNeedReview = true;
    private boolean mMissing;
    private ArrayMap<String, PermissionState> mPermissions;

    public UidPermissionState() {
    }

    public UidPermissionState(UidPermissionState other) {
        this.mMissing = other.mMissing;
        if (other.mPermissions != null) {
            this.mPermissions = new ArrayMap<>();
            int permissionsSize = other.mPermissions.size();
            for (int i = 0; i < permissionsSize; i++) {
                String name = other.mPermissions.keyAt(i);
                PermissionState permissionState = other.mPermissions.valueAt(i);
                this.mPermissions.put(name, new PermissionState(permissionState));
            }
        }
    }

    public void reset() {
        this.mMissing = false;
        this.mPermissions = null;
        invalidateCache();
    }

    public boolean isMissing() {
        return this.mMissing;
    }

    public void setMissing(boolean missing) {
        this.mMissing = missing;
    }

    @Deprecated
    public boolean hasPermissionState(String name) {
        ArrayMap<String, PermissionState> arrayMap = this.mPermissions;
        return arrayMap != null && arrayMap.containsKey(name);
    }

    @Deprecated
    public boolean hasPermissionState(ArraySet<String> names) {
        if (this.mPermissions == null) {
            return false;
        }
        int namesSize = names.size();
        for (int i = 0; i < namesSize; i++) {
            String name = names.valueAt(i);
            if (this.mPermissions.containsKey(name)) {
                return true;
            }
        }
        return false;
    }

    public PermissionState getPermissionState(String name) {
        ArrayMap<String, PermissionState> arrayMap = this.mPermissions;
        if (arrayMap == null) {
            return null;
        }
        return arrayMap.get(name);
    }

    private PermissionState getOrCreatePermissionState(Permission permission) {
        if (this.mPermissions == null) {
            this.mPermissions = new ArrayMap<>();
        }
        String name = permission.getName();
        PermissionState permissionState = this.mPermissions.get(name);
        if (permissionState == null) {
            PermissionState permissionState2 = new PermissionState(permission);
            this.mPermissions.put(name, permissionState2);
            return permissionState2;
        }
        return permissionState;
    }

    public List<PermissionState> getPermissionStates() {
        if (this.mPermissions == null) {
            return Collections.emptyList();
        }
        return new ArrayList(this.mPermissions.values());
    }

    public void putPermissionState(Permission permission, boolean granted, int flags) {
        String name = permission.getName();
        ArrayMap<String, PermissionState> arrayMap = this.mPermissions;
        if (arrayMap == null) {
            this.mPermissions = new ArrayMap<>();
        } else {
            arrayMap.remove(name);
        }
        PermissionState permissionState = new PermissionState(permission);
        if (granted) {
            permissionState.grant();
        }
        permissionState.updateFlags(flags, flags);
        this.mPermissions.put(name, permissionState);
    }

    public boolean removePermissionState(String name) {
        ArrayMap<String, PermissionState> arrayMap = this.mPermissions;
        if (arrayMap == null) {
            return false;
        }
        boolean changed = arrayMap.remove(name) != null;
        if (changed && this.mPermissions.isEmpty()) {
            this.mPermissions = null;
        }
        return changed;
    }

    public boolean isPermissionGranted(String name) {
        PermissionState permissionState = getPermissionState(name);
        return permissionState != null && permissionState.isGranted();
    }

    public Set<String> getGrantedPermissions() {
        if (this.mPermissions == null) {
            return Collections.emptySet();
        }
        Set<String> permissions = new ArraySet<>(this.mPermissions.size());
        int permissionsSize = this.mPermissions.size();
        for (int i = 0; i < permissionsSize; i++) {
            PermissionState permissionState = this.mPermissions.valueAt(i);
            if (permissionState.isGranted()) {
                permissions.add(permissionState.getName());
            }
        }
        return permissions;
    }

    public boolean grantPermission(Permission permission) {
        PermissionState permissionState = getOrCreatePermissionState(permission);
        return permissionState.grant();
    }

    public boolean revokePermission(Permission permission) {
        String name = permission.getName();
        PermissionState permissionState = getPermissionState(name);
        if (permissionState == null) {
            return false;
        }
        boolean changed = permissionState.revoke();
        if (changed && permissionState.isDefault()) {
            removePermissionState(name);
        }
        return changed;
    }

    public int getPermissionFlags(String name) {
        PermissionState permissionState = getPermissionState(name);
        if (permissionState == null) {
            return 0;
        }
        return permissionState.getFlags();
    }

    public boolean updatePermissionFlags(Permission permission, int flagMask, int flagValues) {
        if (flagMask == 0) {
            return false;
        }
        PermissionState permissionState = getOrCreatePermissionState(permission);
        boolean changed = permissionState.updateFlags(flagMask, flagValues);
        if (changed && permissionState.isDefault()) {
            removePermissionState(permission.getName());
        }
        return changed;
    }

    public boolean updatePermissionFlagsForAllPermissions(int flagMask, int flagValues) {
        ArrayMap<String, PermissionState> arrayMap;
        if (flagMask == 0 || (arrayMap = this.mPermissions) == null) {
            return false;
        }
        boolean anyChanged = false;
        for (int i = arrayMap.size() - 1; i >= 0; i--) {
            PermissionState permissionState = this.mPermissions.valueAt(i);
            boolean changed = permissionState.updateFlags(flagMask, flagValues);
            if (changed && permissionState.isDefault()) {
                this.mPermissions.removeAt(i);
            }
            anyChanged |= changed;
        }
        return anyChanged;
    }

    public boolean isPermissionsReviewRequired() {
        ArrayMap<String, PermissionState> arrayMap = this.mPermissions;
        if (arrayMap == null) {
            return false;
        }
        int permissionsSize = arrayMap.size();
        if (CtaManagerFactory.getInstance().makeCtaManager().isCtaSupported()) {
            this.isNeedReview = true;
            for (int i = 0; i < permissionsSize; i++) {
                PermissionState permission = this.mPermissions.valueAt(i);
                if (CtaManagerFactory.getInstance().makeCtaManager().isCtaSupported() && this.isNeedReview && (permission.getFlags() & 64) != 0) {
                    return true;
                }
            }
        } else {
            for (int i2 = 0; i2 < permissionsSize; i2++) {
                PermissionState permission2 = this.mPermissions.valueAt(i2);
                if ((permission2.getFlags() & 64) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public int[] computeGids(int[] globalGids, int userId) {
        IntArray gids = IntArray.wrap(globalGids);
        ArrayMap<String, PermissionState> arrayMap = this.mPermissions;
        if (arrayMap == null) {
            return gids.toArray();
        }
        int permissionsSize = arrayMap.size();
        for (int i = 0; i < permissionsSize; i++) {
            PermissionState permissionState = this.mPermissions.valueAt(i);
            if (permissionState.isGranted()) {
                int[] permissionGids = permissionState.computeGids(userId);
                if (permissionGids.length != 0) {
                    gids.addAll(permissionGids);
                }
            }
        }
        return gids.toArray();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void invalidateCache() {
        PackageManager.invalidatePackageInfoCache();
    }
}
