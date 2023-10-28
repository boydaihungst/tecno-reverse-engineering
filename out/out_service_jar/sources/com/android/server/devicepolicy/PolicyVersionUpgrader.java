package com.android.server.devicepolicy;

import android.content.ComponentName;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.JournaledFile;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
/* loaded from: classes.dex */
public class PolicyVersionUpgrader {
    private static final String LOG_TAG = "DevicePolicyManager";
    private static final boolean VERBOSE_LOG = false;
    private final PolicyPathProvider mPathProvider;
    private final PolicyUpgraderDataProvider mProvider;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PolicyVersionUpgrader(PolicyUpgraderDataProvider provider, PolicyPathProvider pathProvider) {
        this.mProvider = provider;
        this.mPathProvider = pathProvider;
    }

    public void upgradePolicy(int dpmsVersion) {
        int oldVersion = readVersion();
        if (oldVersion >= dpmsVersion) {
            Slog.i(LOG_TAG, String.format("Current version %d, latest version %d, not upgrading.", Integer.valueOf(oldVersion), Integer.valueOf(dpmsVersion)));
            return;
        }
        int[] allUsers = this.mProvider.getUsersForUpgrade();
        OwnersData ownersData = loadOwners(allUsers);
        SparseArray<DevicePolicyData> allUsersData = loadAllUsersData(allUsers, oldVersion, ownersData);
        int currentVersion = oldVersion;
        if (currentVersion == 0) {
            Slog.i(LOG_TAG, String.format("Upgrading from version %d", Integer.valueOf(currentVersion)));
            currentVersion = 1;
        }
        if (currentVersion == 1) {
            Slog.i(LOG_TAG, String.format("Upgrading from version %d", Integer.valueOf(currentVersion)));
            upgradeSensorPermissionsAccess(allUsers, ownersData, allUsersData);
            currentVersion = 2;
        }
        if (currentVersion == 2) {
            Slog.i(LOG_TAG, String.format("Upgrading from version %d", Integer.valueOf(currentVersion)));
            upgradeProtectedPackages(ownersData, allUsersData);
            currentVersion = 3;
        }
        writePoliciesAndVersion(allUsers, allUsersData, ownersData, currentVersion);
    }

    private void upgradeSensorPermissionsAccess(int[] allUsers, OwnersData ownersData, SparseArray<DevicePolicyData> allUsersData) {
        for (int userId : allUsers) {
            DevicePolicyData userData = allUsersData.get(userId);
            if (userData != null) {
                Iterator<ActiveAdmin> it = userData.mAdminList.iterator();
                while (it.hasNext()) {
                    ActiveAdmin admin = it.next();
                    if (ownersData.mDeviceOwnerUserId == userId && ownersData.mDeviceOwner != null && ownersData.mDeviceOwner.admin.equals(admin.info.getComponent())) {
                        Slog.i(LOG_TAG, String.format("Marking Device Owner in user %d for permission grant ", Integer.valueOf(userId)));
                        admin.mAdminCanGrantSensorsPermissions = true;
                    }
                }
            }
        }
    }

    private void upgradeProtectedPackages(OwnersData ownersData, SparseArray<DevicePolicyData> allUsersData) {
        if (ownersData.mDeviceOwner == null) {
            return;
        }
        List<String> protectedPackages = null;
        DevicePolicyData doUserData = allUsersData.get(ownersData.mDeviceOwnerUserId);
        if (doUserData == null) {
            Slog.e(LOG_TAG, "No policy data for do user");
            return;
        }
        if (ownersData.mDeviceOwnerProtectedPackages != null) {
            List<String> protectedPackages2 = ownersData.mDeviceOwnerProtectedPackages.get(ownersData.mDeviceOwner.packageName);
            protectedPackages = protectedPackages2;
            if (protectedPackages != null) {
                Slog.i(LOG_TAG, "Found protected packages in Owners");
            }
            ownersData.mDeviceOwnerProtectedPackages = null;
        } else if (doUserData.mUserControlDisabledPackages != null) {
            Slog.i(LOG_TAG, "Found protected packages in DevicePolicyData");
            protectedPackages = doUserData.mUserControlDisabledPackages;
            doUserData.mUserControlDisabledPackages = null;
        }
        ActiveAdmin doAdmin = doUserData.mAdminMap.get(ownersData.mDeviceOwner.admin);
        if (doAdmin == null) {
            Slog.e(LOG_TAG, "DO admin not found in DO user");
        } else if (protectedPackages != null) {
            doAdmin.protectedPackages = new ArrayList(protectedPackages);
        }
    }

    private OwnersData loadOwners(int[] allUsers) {
        OwnersData ownersData = new OwnersData(this.mPathProvider);
        ownersData.load(allUsers);
        return ownersData;
    }

    private void writePoliciesAndVersion(int[] allUsers, SparseArray<DevicePolicyData> allUsersData, OwnersData ownersData, int currentVersion) {
        boolean allWritesSuccessful = true;
        int length = allUsers.length;
        int i = 0;
        while (true) {
            boolean z = true;
            if (i >= length) {
                break;
            }
            int user = allUsers[i];
            if (!allWritesSuccessful || !writeDataForUser(user, allUsersData.get(user))) {
                z = false;
            }
            allWritesSuccessful = z;
            i++;
        }
        boolean allWritesSuccessful2 = allWritesSuccessful && ownersData.writeDeviceOwner();
        for (int user2 : allUsers) {
            allWritesSuccessful2 = allWritesSuccessful2 && ownersData.writeProfileOwner(user2);
        }
        if (allWritesSuccessful2) {
            writeVersion(currentVersion);
        } else {
            Slog.e(LOG_TAG, String.format("Error: Failed upgrading policies to version %d", Integer.valueOf(currentVersion)));
        }
    }

    private SparseArray<DevicePolicyData> loadAllUsersData(int[] allUsers, int loadVersion, OwnersData ownersData) {
        SparseArray<DevicePolicyData> allUsersData = new SparseArray<>();
        for (int user : allUsers) {
            ComponentName owner = getOwnerForUser(ownersData, user);
            allUsersData.append(user, loadDataForUser(user, loadVersion, owner));
        }
        return allUsersData;
    }

    private ComponentName getOwnerForUser(OwnersData ownersData, int user) {
        if (ownersData.mDeviceOwnerUserId == user && ownersData.mDeviceOwner != null) {
            ComponentName owner = ownersData.mDeviceOwner.admin;
            return owner;
        } else if (!ownersData.mProfileOwners.containsKey(Integer.valueOf(user))) {
            return null;
        } else {
            ComponentName owner2 = ownersData.mProfileOwners.get(Integer.valueOf(user)).admin;
            return owner2;
        }
    }

    private DevicePolicyData loadDataForUser(int userId, int loadVersion, ComponentName ownerComponent) {
        DevicePolicyData policy = new DevicePolicyData(userId);
        DevicePolicyData.load(policy, !this.mProvider.storageManagerIsFileBasedEncryptionEnabled(), this.mProvider.makeDevicePoliciesJournaledFile(userId), this.mProvider.getAdminInfoSupplier(userId), ownerComponent);
        return policy;
    }

    private boolean writeDataForUser(int userId, DevicePolicyData policy) {
        return DevicePolicyData.store(policy, this.mProvider.makeDevicePoliciesJournaledFile(userId), !this.mProvider.storageManagerIsFileBasedEncryptionEnabled());
    }

    private JournaledFile getVersionFile() {
        return this.mProvider.makePoliciesVersionJournaledFile(0);
    }

    private int readVersion() {
        JournaledFile versionFile = getVersionFile();
        File file = versionFile.chooseForRead();
        try {
            String versionString = Files.readAllLines(file.toPath(), Charset.defaultCharset()).get(0);
            return Integer.parseInt(versionString);
        } catch (IOException | IndexOutOfBoundsException | NumberFormatException e) {
            Slog.e(LOG_TAG, "Error reading version", e);
            return 0;
        }
    }

    private void writeVersion(int version) {
        JournaledFile versionFile = getVersionFile();
        File file = versionFile.chooseForWrite();
        try {
            byte[] versionBytes = String.format("%d", Integer.valueOf(version)).getBytes();
            Files.write(file.toPath(), versionBytes, new OpenOption[0]);
            versionFile.commit();
        } catch (IOException e) {
            Slog.e(LOG_TAG, String.format("Writing version %d failed", Integer.valueOf(version)), e);
            versionFile.rollback();
        }
    }
}
