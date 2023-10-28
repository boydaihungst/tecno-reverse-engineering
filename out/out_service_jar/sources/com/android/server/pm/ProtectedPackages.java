package com.android.server.pm;

import android.content.Context;
import android.util.ArraySet;
import android.util.SparseArray;
import java.util.List;
import java.util.Set;
/* loaded from: classes2.dex */
public class ProtectedPackages {
    private final Context mContext;
    private String mDeviceOwnerPackage;
    private int mDeviceOwnerUserId;
    private final String mDeviceProvisioningPackage;
    private final SparseArray<Set<String>> mOwnerProtectedPackages = new SparseArray<>();
    private SparseArray<String> mProfileOwnerPackages;

    public ProtectedPackages(Context context) {
        this.mContext = context;
        this.mDeviceProvisioningPackage = context.getResources().getString(17039954);
    }

    public synchronized void setDeviceAndProfileOwnerPackages(int deviceOwnerUserId, String deviceOwnerPackage, SparseArray<String> profileOwnerPackages) {
        this.mDeviceOwnerUserId = deviceOwnerUserId;
        SparseArray<String> sparseArray = null;
        this.mDeviceOwnerPackage = deviceOwnerUserId == -10000 ? null : deviceOwnerPackage;
        if (profileOwnerPackages != null) {
            sparseArray = profileOwnerPackages.clone();
        }
        this.mProfileOwnerPackages = sparseArray;
    }

    public synchronized void setOwnerProtectedPackages(int userId, List<String> packageNames) {
        if (packageNames.isEmpty()) {
            this.mOwnerProtectedPackages.remove(userId);
        } else {
            this.mOwnerProtectedPackages.put(userId, new ArraySet(packageNames));
        }
    }

    private synchronized boolean hasDeviceOwnerOrProfileOwner(int userId, String packageName) {
        if (packageName == null) {
            return false;
        }
        String str = this.mDeviceOwnerPackage;
        if (str != null && this.mDeviceOwnerUserId == userId && packageName.equals(str)) {
            return true;
        }
        SparseArray<String> sparseArray = this.mProfileOwnerPackages;
        if (sparseArray != null) {
            if (packageName.equals(sparseArray.get(userId))) {
                return true;
            }
        }
        return false;
    }

    public synchronized String getDeviceOwnerOrProfileOwnerPackage(int userId) {
        if (this.mDeviceOwnerUserId == userId) {
            return this.mDeviceOwnerPackage;
        }
        SparseArray<String> sparseArray = this.mProfileOwnerPackages;
        if (sparseArray == null) {
            return null;
        }
        return sparseArray.get(userId);
    }

    /* JADX WARN: Code restructure failed: missing block: B:7:0x000f, code lost:
        if (isOwnerProtectedPackage(r2, r3) != false) goto L13;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private synchronized boolean isProtectedPackage(int userId, String packageName) {
        boolean z;
        if (packageName != null) {
            if (!packageName.equals(this.mDeviceProvisioningPackage)) {
            }
            z = true;
        }
        z = false;
        return z;
    }

    private synchronized boolean isOwnerProtectedPackage(int userId, String packageName) {
        boolean z;
        if (!isPackageProtectedForUser(-1, packageName)) {
            z = isPackageProtectedForUser(userId, packageName);
        }
        return z;
    }

    private synchronized boolean isPackageProtectedForUser(int userId, String packageName) {
        boolean z;
        int userIdx = this.mOwnerProtectedPackages.indexOfKey(userId);
        if (userIdx >= 0) {
            z = this.mOwnerProtectedPackages.valueAt(userIdx).contains(packageName);
        }
        return z;
    }

    public boolean isPackageStateProtected(int userId, String packageName) {
        return hasDeviceOwnerOrProfileOwner(userId, packageName) || isProtectedPackage(userId, packageName);
    }

    public boolean isPackageDataProtected(int userId, String packageName) {
        return hasDeviceOwnerOrProfileOwner(userId, packageName) || isProtectedPackage(userId, packageName);
    }
}
