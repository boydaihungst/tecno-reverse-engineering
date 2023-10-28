package com.android.server.usb;

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.SparseArray;
import com.android.internal.util.dump.DualDumpOutputStream;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class UsbSettingsManager {
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = UsbSettingsManager.class.getSimpleName();
    private final Context mContext;
    private UsbHandlerManager mUsbHandlerManager;
    final UsbService mUsbService;
    private UserManager mUserManager;
    private final SparseArray<UsbUserSettingsManager> mSettingsByUser = new SparseArray<>();
    private final SparseArray<UsbProfileGroupSettingsManager> mSettingsByProfileGroup = new SparseArray<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    public UsbSettingsManager(Context context, UsbService usbService) {
        this.mContext = context;
        this.mUsbService = usbService;
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mUsbHandlerManager = new UsbHandlerManager(context);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UsbUserSettingsManager getSettingsForUser(int userId) {
        UsbUserSettingsManager settings;
        synchronized (this.mSettingsByUser) {
            settings = this.mSettingsByUser.get(userId);
            if (settings == null) {
                settings = new UsbUserSettingsManager(this.mContext, UserHandle.of(userId));
                this.mSettingsByUser.put(userId, settings);
            }
        }
        return settings;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UsbProfileGroupSettingsManager getSettingsForProfileGroup(UserHandle user) {
        UserHandle parentUser;
        UsbProfileGroupSettingsManager settings;
        UserInfo parentUserInfo = this.mUserManager.getProfileParent(user.getIdentifier());
        if (parentUserInfo != null) {
            parentUser = parentUserInfo.getUserHandle();
        } else {
            parentUser = user;
        }
        synchronized (this.mSettingsByProfileGroup) {
            settings = this.mSettingsByProfileGroup.get(parentUser.getIdentifier());
            if (settings == null) {
                settings = new UsbProfileGroupSettingsManager(this.mContext, parentUser, this, this.mUsbHandlerManager);
                this.mSettingsByProfileGroup.put(parentUser.getIdentifier(), settings);
            }
        }
        return settings;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void remove(UserHandle userToRemove) {
        synchronized (this.mSettingsByUser) {
            this.mSettingsByUser.remove(userToRemove.getIdentifier());
        }
        synchronized (this.mSettingsByProfileGroup) {
            if (this.mSettingsByProfileGroup.indexOfKey(userToRemove.getIdentifier()) >= 0) {
                this.mSettingsByProfileGroup.get(userToRemove.getIdentifier()).unregisterReceivers();
                this.mSettingsByProfileGroup.remove(userToRemove.getIdentifier());
            } else {
                int numProfileGroups = this.mSettingsByProfileGroup.size();
                for (int i = 0; i < numProfileGroups; i++) {
                    this.mSettingsByProfileGroup.valueAt(i).removeUser(userToRemove);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(DualDumpOutputStream dump, String idName, long id) {
        long token = dump.start(idName, id);
        synchronized (this.mSettingsByUser) {
            List<UserInfo> users = this.mUserManager.getUsers();
            int numUsers = users.size();
            for (int i = 0; i < numUsers; i++) {
                getSettingsForUser(users.get(i).id).dump(dump, "user_settings", CompanionAppsPermissions.APP_PERMISSIONS);
            }
        }
        synchronized (this.mSettingsByProfileGroup) {
            int numProfileGroups = this.mSettingsByProfileGroup.size();
            for (int i2 = 0; i2 < numProfileGroups; i2++) {
                this.mSettingsByProfileGroup.valueAt(i2).dump(dump, "profile_group_settings", 2246267895810L);
            }
        }
        dump.end(token);
    }
}
