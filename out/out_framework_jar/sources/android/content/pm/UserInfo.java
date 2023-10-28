package android.content.pm;

import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.DebugUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/* loaded from: classes.dex */
public class UserInfo implements Parcelable {
    public static final Parcelable.Creator<UserInfo> CREATOR = new Parcelable.Creator<UserInfo>() { // from class: android.content.pm.UserInfo.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public UserInfo createFromParcel(Parcel source) {
            return new UserInfo(source);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public UserInfo[] newArray(int size) {
            return new UserInfo[size];
        }
    };
    public static final int FLAG_ADMIN = 2;
    @Deprecated
    public static final int FLAG_DEMO = 512;
    public static final int FLAG_DISABLED = 64;
    public static final int FLAG_DUAL_PROFILE = 32768;
    public static final int FLAG_EPHEMERAL = 256;
    public static final int FLAG_FULL = 1024;
    @Deprecated
    public static final int FLAG_GUEST = 4;
    public static final int FLAG_INITIALIZED = 16;
    @Deprecated
    public static final int FLAG_MANAGED_PROFILE = 32;
    public static final int FLAG_PRIMARY = 1;
    public static final int FLAG_PROFILE = 4096;
    public static final int FLAG_QUIET_MODE = 128;
    @Deprecated
    public static final int FLAG_RESTRICTED = 8;
    public static final int FLAG_SYSTEM = 2048;
    public static final int NO_PROFILE_GROUP_ID = -10000;
    public boolean convertedFromPreCreated;
    public long creationTime;
    public int flags;
    public boolean guestToRemove;
    public String iconPath;
    public int id;
    public String lastLoggedInFingerprint;
    public long lastLoggedInTime;
    public String name;
    public boolean partial;
    public boolean preCreated;
    public int profileBadge;
    public int profileGroupId;
    public int restrictedProfileParentId;
    public int serialNumber;
    public String userType;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface UserInfoFlag {
    }

    public UserInfo(int id, String name, int flags) {
        this(id, name, null, flags);
    }

    public UserInfo(int id, String name, String iconPath, int flags) {
        this(id, name, iconPath, flags, getDefaultUserType(flags));
    }

    public UserInfo(int id, String name, String iconPath, int flags, String userType) {
        this.id = id;
        this.name = name;
        this.flags = flags;
        this.userType = userType;
        this.iconPath = iconPath;
        this.profileGroupId = -10000;
        this.restrictedProfileParentId = -10000;
    }

    public static String getDefaultUserType(int userInfoFlag) {
        if ((userInfoFlag & 2048) != 0) {
            throw new IllegalArgumentException("Cannot getDefaultUserType for flags " + Integer.toHexString(userInfoFlag) + " because it corresponds to a SYSTEM user type.");
        }
        switch (userInfoFlag & 556) {
            case 0:
                return UserManager.USER_TYPE_FULL_SECONDARY;
            case 4:
                return UserManager.USER_TYPE_FULL_GUEST;
            case 8:
                return UserManager.USER_TYPE_FULL_RESTRICTED;
            case 32:
                return UserManager.USER_TYPE_PROFILE_MANAGED;
            case 512:
                return UserManager.USER_TYPE_FULL_DEMO;
            default:
                throw new IllegalArgumentException("Cannot getDefaultUserType for flags " + Integer.toHexString(userInfoFlag) + " because it doesn't correspond to a valid user type.");
        }
    }

    public boolean isPrimary() {
        return (this.flags & 1) == 1;
    }

    public boolean isAdmin() {
        return (this.flags & 2) == 2;
    }

    public boolean isGuest() {
        return UserManager.isUserTypeGuest(this.userType);
    }

    public boolean isRestricted() {
        return UserManager.isUserTypeRestricted(this.userType);
    }

    public boolean isProfile() {
        return (this.flags & 4096) != 0;
    }

    public boolean isManagedProfile() {
        return UserManager.isUserTypeManagedProfile(this.userType);
    }

    public boolean isCloneProfile() {
        return UserManager.isUserTypeCloneProfile(this.userType);
    }

    public boolean isEnabled() {
        return (this.flags & 64) != 64;
    }

    public boolean isQuietModeEnabled() {
        return (this.flags & 128) == 128;
    }

    public boolean isEphemeral() {
        return (this.flags & 256) == 256;
    }

    public boolean isInitialized() {
        return (this.flags & 16) == 16;
    }

    public boolean isDemo() {
        return UserManager.isUserTypeDemo(this.userType) || (this.flags & 512) != 0;
    }

    public boolean isFull() {
        return (this.flags & 1024) == 1024;
    }

    public boolean isDualProfile() {
        return UserManager.isUserTypeDualProfile(this.userType);
    }

    public boolean isSystemOnly() {
        return isSystemOnly(this.id);
    }

    public static boolean isSystemOnly(int userId) {
        return userId == 0 && UserManager.isSplitSystemUser();
    }

    public boolean supportsSwitchTo() {
        if (this.partial || !isEnabled() || this.preCreated) {
            return false;
        }
        return !isProfile();
    }

    public boolean supportsSwitchToByUser() {
        boolean hideSystemUser = UserManager.isHeadlessSystemUserMode();
        return !(hideSystemUser && this.id == 0) && supportsSwitchTo();
    }

    public boolean canHaveProfile() {
        if (isProfile() || isGuest() || isRestricted()) {
            return false;
        }
        return (UserManager.isSplitSystemUser() || UserManager.isHeadlessSystemUserMode()) ? this.id != 0 : this.id == 0;
    }

    @Deprecated
    public UserInfo() {
    }

    public UserInfo(UserInfo orig) {
        this.name = orig.name;
        this.iconPath = orig.iconPath;
        this.id = orig.id;
        this.flags = orig.flags;
        this.userType = orig.userType;
        this.serialNumber = orig.serialNumber;
        this.creationTime = orig.creationTime;
        this.lastLoggedInTime = orig.lastLoggedInTime;
        this.lastLoggedInFingerprint = orig.lastLoggedInFingerprint;
        this.partial = orig.partial;
        this.preCreated = orig.preCreated;
        this.convertedFromPreCreated = orig.convertedFromPreCreated;
        this.profileGroupId = orig.profileGroupId;
        this.restrictedProfileParentId = orig.restrictedProfileParentId;
        this.guestToRemove = orig.guestToRemove;
        this.profileBadge = orig.profileBadge;
    }

    public UserHandle getUserHandle() {
        return UserHandle.of(this.id);
    }

    public String toString() {
        return "UserInfo{" + this.id + ":" + this.name + ":" + Integer.toHexString(this.flags) + "}";
    }

    public String toFullString() {
        return "UserInfo[id=" + this.id + ", name=" + this.name + ", type=" + this.userType + ", flags=" + flagsToString(this.flags) + (this.preCreated ? " (pre-created)" : "") + (this.convertedFromPreCreated ? " (converted)" : "") + (this.partial ? " (partial)" : "") + NavigationBarInflaterView.SIZE_MOD_END;
    }

    public static String flagsToString(int flags) {
        return DebugUtils.flagsToString(UserInfo.class, "FLAG_", flags);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int parcelableFlags) {
        dest.writeInt(this.id);
        dest.writeString8(this.name);
        dest.writeString8(this.iconPath);
        dest.writeInt(this.flags);
        dest.writeString8(this.userType);
        dest.writeInt(this.serialNumber);
        dest.writeLong(this.creationTime);
        dest.writeLong(this.lastLoggedInTime);
        dest.writeString8(this.lastLoggedInFingerprint);
        dest.writeBoolean(this.partial);
        dest.writeBoolean(this.preCreated);
        dest.writeInt(this.profileGroupId);
        dest.writeBoolean(this.guestToRemove);
        dest.writeInt(this.restrictedProfileParentId);
        dest.writeInt(this.profileBadge);
    }

    private UserInfo(Parcel source) {
        this.id = source.readInt();
        this.name = source.readString8();
        this.iconPath = source.readString8();
        this.flags = source.readInt();
        this.userType = source.readString8();
        this.serialNumber = source.readInt();
        this.creationTime = source.readLong();
        this.lastLoggedInTime = source.readLong();
        this.lastLoggedInFingerprint = source.readString8();
        this.partial = source.readBoolean();
        this.preCreated = source.readBoolean();
        this.profileGroupId = source.readInt();
        this.guestToRemove = source.readBoolean();
        this.restrictedProfileParentId = source.readInt();
        this.profileBadge = source.readInt();
    }
}
