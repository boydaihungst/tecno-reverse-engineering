package android.os;

import android.content.IntentSender;
import android.content.pm.UserInfo;
import android.graphics.Bitmap;
import android.os.IUserRestrictionsListener;
import android.os.UserManager;
import java.util.List;
/* loaded from: classes2.dex */
public interface IUserManager extends IInterface {
    void addUserRestrictionsListener(IUserRestrictionsListener iUserRestrictionsListener) throws RemoteException;

    boolean canAddMoreManagedProfiles(int i, boolean z) throws RemoteException;

    boolean canAddMoreProfilesToUser(String str, int i, boolean z) throws RemoteException;

    boolean canAddMoreUsersOfType(String str) throws RemoteException;

    boolean canHaveRestrictedProfile(int i) throws RemoteException;

    void clearSeedAccountData(int i) throws RemoteException;

    UserInfo createProfileForUserEvenWhenDisallowedWithThrow(String str, String str2, int i, int i2, String[] strArr) throws RemoteException;

    UserInfo createProfileForUserWithThrow(String str, String str2, int i, int i2, String[] strArr) throws RemoteException;

    UserInfo createRestrictedProfileWithThrow(String str, int i) throws RemoteException;

    UserHandle createUserWithAttributes(String str, String str2, int i, Bitmap bitmap, String str3, String str4, PersistableBundle persistableBundle) throws RemoteException;

    UserInfo createUserWithThrow(String str, String str2, int i) throws RemoteException;

    void evictCredentialEncryptionKey(int i) throws RemoteException;

    UserInfo findCurrentGuestUser() throws RemoteException;

    Bundle getApplicationRestrictions(String str) throws RemoteException;

    Bundle getApplicationRestrictionsForUser(String str, int i) throws RemoteException;

    int getCredentialOwnerProfile(int i) throws RemoteException;

    Bundle getDefaultGuestRestrictions() throws RemoteException;

    String[] getPreInstallableSystemPackages(String str) throws RemoteException;

    UserInfo getPrimaryUser() throws RemoteException;

    int[] getProfileIds(int i, boolean z) throws RemoteException;

    UserInfo getProfileParent(int i) throws RemoteException;

    int getProfileParentId(int i) throws RemoteException;

    String getProfileType(int i) throws RemoteException;

    List<UserInfo> getProfiles(int i, boolean z) throws RemoteException;

    int getRemainingCreatableProfileCount(String str, int i) throws RemoteException;

    int getRemainingCreatableUserCount(String str) throws RemoteException;

    String getSeedAccountName(int i) throws RemoteException;

    PersistableBundle getSeedAccountOptions(int i) throws RemoteException;

    String getSeedAccountType(int i) throws RemoteException;

    String getUserAccount(int i) throws RemoteException;

    int getUserBadgeColorResId(int i) throws RemoteException;

    int getUserBadgeDarkColorResId(int i) throws RemoteException;

    int getUserBadgeLabelResId(int i) throws RemoteException;

    int getUserBadgeNoBackgroundResId(int i) throws RemoteException;

    int getUserBadgeResId(int i) throws RemoteException;

    long getUserCreationTime(int i) throws RemoteException;

    int getUserHandle(int i) throws RemoteException;

    ParcelFileDescriptor getUserIcon(int i) throws RemoteException;

    int getUserIconBadgeResId(int i) throws RemoteException;

    UserInfo getUserInfo(int i) throws RemoteException;

    String getUserName() throws RemoteException;

    int getUserRestrictionSource(String str, int i) throws RemoteException;

    List<UserManager.EnforcingUser> getUserRestrictionSources(String str, int i) throws RemoteException;

    Bundle getUserRestrictions(int i) throws RemoteException;

    int getUserSerialNumber(int i) throws RemoteException;

    long getUserStartRealtime() throws RemoteException;

    long getUserUnlockRealtime() throws RemoteException;

    List<UserInfo> getUsers(boolean z, boolean z2, boolean z3) throws RemoteException;

    boolean hasBadge(int i) throws RemoteException;

    boolean hasBaseUserRestriction(String str, int i) throws RemoteException;

    boolean hasRestrictedProfiles(int i) throws RemoteException;

    boolean hasUserRestriction(String str, int i) throws RemoteException;

    boolean hasUserRestrictionOnAnyUser(String str) throws RemoteException;

    boolean isCredentialSharableWithParent(int i) throws RemoteException;

    boolean isDemoUser(int i) throws RemoteException;

    boolean isDualProfile(int i) throws RemoteException;

    boolean isMediaSharedWithParent(int i) throws RemoteException;

    boolean isPreCreated(int i) throws RemoteException;

    boolean isQuietModeEnabled(int i) throws RemoteException;

    boolean isRestricted(int i) throws RemoteException;

    boolean isSameProfileGroup(int i, int i2) throws RemoteException;

    boolean isSettingRestrictedForUser(String str, int i, String str2, int i2) throws RemoteException;

    boolean isUserForeground(int i) throws RemoteException;

    boolean isUserNameSet(int i) throws RemoteException;

    boolean isUserOfType(int i, String str) throws RemoteException;

    boolean isUserRunning(int i) throws RemoteException;

    boolean isUserTypeEnabled(String str) throws RemoteException;

    boolean isUserUnlocked(int i) throws RemoteException;

    boolean isUserUnlockingOrUnlocked(int i) throws RemoteException;

    boolean markGuestForDeletion(int i) throws RemoteException;

    UserInfo preCreateUserWithThrow(String str) throws RemoteException;

    boolean removeUser(int i) throws RemoteException;

    boolean removeUserEvenWhenDisallowed(int i) throws RemoteException;

    int removeUserWhenPossible(int i, boolean z) throws RemoteException;

    boolean requestQuietModeEnabled(String str, boolean z, int i, IntentSender intentSender, int i2) throws RemoteException;

    void setApplicationRestrictions(String str, Bundle bundle, int i) throws RemoteException;

    void setDefaultGuestRestrictions(Bundle bundle) throws RemoteException;

    void setSeedAccountData(int i, String str, String str2, PersistableBundle persistableBundle, boolean z) throws RemoteException;

    void setUserAccount(int i, String str) throws RemoteException;

    void setUserAdmin(int i) throws RemoteException;

    void setUserEnabled(int i) throws RemoteException;

    void setUserIcon(int i, Bitmap bitmap) throws RemoteException;

    void setUserName(int i, String str) throws RemoteException;

    void setUserRestriction(String str, boolean z, int i) throws RemoteException;

    boolean someUserHasAccount(String str, String str2) throws RemoteException;

    boolean someUserHasSeedAccount(String str, String str2) throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements IUserManager {
        @Override // android.os.IUserManager
        public int getCredentialOwnerProfile(int userId) throws RemoteException {
            return 0;
        }

        @Override // android.os.IUserManager
        public int getProfileParentId(int userId) throws RemoteException {
            return 0;
        }

        @Override // android.os.IUserManager
        public UserInfo createUserWithThrow(String name, String userType, int flags) throws RemoteException {
            return null;
        }

        @Override // android.os.IUserManager
        public UserInfo preCreateUserWithThrow(String userType) throws RemoteException {
            return null;
        }

        @Override // android.os.IUserManager
        public UserInfo createProfileForUserWithThrow(String name, String userType, int flags, int userId, String[] disallowedPackages) throws RemoteException {
            return null;
        }

        @Override // android.os.IUserManager
        public UserInfo createRestrictedProfileWithThrow(String name, int parentUserHandle) throws RemoteException {
            return null;
        }

        @Override // android.os.IUserManager
        public String[] getPreInstallableSystemPackages(String userType) throws RemoteException {
            return null;
        }

        @Override // android.os.IUserManager
        public void setUserEnabled(int userId) throws RemoteException {
        }

        @Override // android.os.IUserManager
        public void setUserAdmin(int userId) throws RemoteException {
        }

        @Override // android.os.IUserManager
        public void evictCredentialEncryptionKey(int userId) throws RemoteException {
        }

        @Override // android.os.IUserManager
        public boolean removeUser(int userId) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public boolean removeUserEvenWhenDisallowed(int userId) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public void setUserName(int userId, String name) throws RemoteException {
        }

        @Override // android.os.IUserManager
        public void setUserIcon(int userId, Bitmap icon) throws RemoteException {
        }

        @Override // android.os.IUserManager
        public ParcelFileDescriptor getUserIcon(int userId) throws RemoteException {
            return null;
        }

        @Override // android.os.IUserManager
        public UserInfo getPrimaryUser() throws RemoteException {
            return null;
        }

        @Override // android.os.IUserManager
        public List<UserInfo> getUsers(boolean excludePartial, boolean excludeDying, boolean excludePreCreated) throws RemoteException {
            return null;
        }

        @Override // android.os.IUserManager
        public List<UserInfo> getProfiles(int userId, boolean enabledOnly) throws RemoteException {
            return null;
        }

        @Override // android.os.IUserManager
        public int[] getProfileIds(int userId, boolean enabledOnly) throws RemoteException {
            return null;
        }

        @Override // android.os.IUserManager
        public boolean isUserTypeEnabled(String userType) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public boolean canAddMoreUsersOfType(String userType) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public int getRemainingCreatableUserCount(String userType) throws RemoteException {
            return 0;
        }

        @Override // android.os.IUserManager
        public int getRemainingCreatableProfileCount(String userType, int userId) throws RemoteException {
            return 0;
        }

        @Override // android.os.IUserManager
        public boolean canAddMoreProfilesToUser(String userType, int userId, boolean allowedToRemoveOne) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public boolean canAddMoreManagedProfiles(int userId, boolean allowedToRemoveOne) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public UserInfo getProfileParent(int userId) throws RemoteException {
            return null;
        }

        @Override // android.os.IUserManager
        public boolean isSameProfileGroup(int userId, int otherUserHandle) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public boolean isUserOfType(int userId, String userType) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public UserInfo getUserInfo(int userId) throws RemoteException {
            return null;
        }

        @Override // android.os.IUserManager
        public String getUserAccount(int userId) throws RemoteException {
            return null;
        }

        @Override // android.os.IUserManager
        public void setUserAccount(int userId, String accountName) throws RemoteException {
        }

        @Override // android.os.IUserManager
        public long getUserCreationTime(int userId) throws RemoteException {
            return 0L;
        }

        @Override // android.os.IUserManager
        public boolean isRestricted(int userId) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public boolean canHaveRestrictedProfile(int userId) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public int getUserSerialNumber(int userId) throws RemoteException {
            return 0;
        }

        @Override // android.os.IUserManager
        public int getUserHandle(int userSerialNumber) throws RemoteException {
            return 0;
        }

        @Override // android.os.IUserManager
        public int getUserRestrictionSource(String restrictionKey, int userId) throws RemoteException {
            return 0;
        }

        @Override // android.os.IUserManager
        public List<UserManager.EnforcingUser> getUserRestrictionSources(String restrictionKey, int userId) throws RemoteException {
            return null;
        }

        @Override // android.os.IUserManager
        public Bundle getUserRestrictions(int userId) throws RemoteException {
            return null;
        }

        @Override // android.os.IUserManager
        public boolean hasBaseUserRestriction(String restrictionKey, int userId) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public boolean hasUserRestriction(String restrictionKey, int userId) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public boolean hasUserRestrictionOnAnyUser(String restrictionKey) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public boolean isSettingRestrictedForUser(String setting, int userId, String value, int callingUid) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public void addUserRestrictionsListener(IUserRestrictionsListener listener) throws RemoteException {
        }

        @Override // android.os.IUserManager
        public void setUserRestriction(String key, boolean value, int userId) throws RemoteException {
        }

        @Override // android.os.IUserManager
        public void setApplicationRestrictions(String packageName, Bundle restrictions, int userId) throws RemoteException {
        }

        @Override // android.os.IUserManager
        public Bundle getApplicationRestrictions(String packageName) throws RemoteException {
            return null;
        }

        @Override // android.os.IUserManager
        public Bundle getApplicationRestrictionsForUser(String packageName, int userId) throws RemoteException {
            return null;
        }

        @Override // android.os.IUserManager
        public void setDefaultGuestRestrictions(Bundle restrictions) throws RemoteException {
        }

        @Override // android.os.IUserManager
        public Bundle getDefaultGuestRestrictions() throws RemoteException {
            return null;
        }

        @Override // android.os.IUserManager
        public int removeUserWhenPossible(int userId, boolean overrideDevicePolicy) throws RemoteException {
            return 0;
        }

        @Override // android.os.IUserManager
        public boolean markGuestForDeletion(int userId) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public UserInfo findCurrentGuestUser() throws RemoteException {
            return null;
        }

        @Override // android.os.IUserManager
        public boolean isQuietModeEnabled(int userId) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public UserHandle createUserWithAttributes(String userName, String userType, int flags, Bitmap userIcon, String accountName, String accountType, PersistableBundle accountOptions) throws RemoteException {
            return null;
        }

        @Override // android.os.IUserManager
        public void setSeedAccountData(int userId, String accountName, String accountType, PersistableBundle accountOptions, boolean persist) throws RemoteException {
        }

        @Override // android.os.IUserManager
        public String getSeedAccountName(int userId) throws RemoteException {
            return null;
        }

        @Override // android.os.IUserManager
        public String getSeedAccountType(int userId) throws RemoteException {
            return null;
        }

        @Override // android.os.IUserManager
        public PersistableBundle getSeedAccountOptions(int userId) throws RemoteException {
            return null;
        }

        @Override // android.os.IUserManager
        public void clearSeedAccountData(int userId) throws RemoteException {
        }

        @Override // android.os.IUserManager
        public boolean someUserHasSeedAccount(String accountName, String accountType) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public boolean someUserHasAccount(String accountName, String accountType) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public String getProfileType(int userId) throws RemoteException {
            return null;
        }

        @Override // android.os.IUserManager
        public boolean isMediaSharedWithParent(int userId) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public boolean isCredentialSharableWithParent(int userId) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public boolean isDemoUser(int userId) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public boolean isPreCreated(int userId) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public UserInfo createProfileForUserEvenWhenDisallowedWithThrow(String name, String userType, int flags, int userId, String[] disallowedPackages) throws RemoteException {
            return null;
        }

        @Override // android.os.IUserManager
        public boolean isUserUnlockingOrUnlocked(int userId) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public int getUserIconBadgeResId(int userId) throws RemoteException {
            return 0;
        }

        @Override // android.os.IUserManager
        public int getUserBadgeResId(int userId) throws RemoteException {
            return 0;
        }

        @Override // android.os.IUserManager
        public int getUserBadgeNoBackgroundResId(int userId) throws RemoteException {
            return 0;
        }

        @Override // android.os.IUserManager
        public int getUserBadgeLabelResId(int userId) throws RemoteException {
            return 0;
        }

        @Override // android.os.IUserManager
        public int getUserBadgeColorResId(int userId) throws RemoteException {
            return 0;
        }

        @Override // android.os.IUserManager
        public int getUserBadgeDarkColorResId(int userId) throws RemoteException {
            return 0;
        }

        @Override // android.os.IUserManager
        public boolean hasBadge(int userId) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public boolean isUserUnlocked(int userId) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public boolean isUserRunning(int userId) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public boolean isUserForeground(int userId) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public boolean isUserNameSet(int userId) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public boolean hasRestrictedProfiles(int userId) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public boolean requestQuietModeEnabled(String callingPackage, boolean enableQuietMode, int userId, IntentSender target, int flags) throws RemoteException {
            return false;
        }

        @Override // android.os.IUserManager
        public String getUserName() throws RemoteException {
            return null;
        }

        @Override // android.os.IUserManager
        public long getUserStartRealtime() throws RemoteException {
            return 0L;
        }

        @Override // android.os.IUserManager
        public long getUserUnlockRealtime() throws RemoteException {
            return 0L;
        }

        @Override // android.os.IUserManager
        public boolean isDualProfile(int userId) throws RemoteException {
            return false;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements IUserManager {
        public static final String DESCRIPTOR = "android.os.IUserManager";
        static final int TRANSACTION_addUserRestrictionsListener = 44;
        static final int TRANSACTION_canAddMoreManagedProfiles = 25;
        static final int TRANSACTION_canAddMoreProfilesToUser = 24;
        static final int TRANSACTION_canAddMoreUsersOfType = 21;
        static final int TRANSACTION_canHaveRestrictedProfile = 34;
        static final int TRANSACTION_clearSeedAccountData = 60;
        static final int TRANSACTION_createProfileForUserEvenWhenDisallowedWithThrow = 68;
        static final int TRANSACTION_createProfileForUserWithThrow = 5;
        static final int TRANSACTION_createRestrictedProfileWithThrow = 6;
        static final int TRANSACTION_createUserWithAttributes = 55;
        static final int TRANSACTION_createUserWithThrow = 3;
        static final int TRANSACTION_evictCredentialEncryptionKey = 10;
        static final int TRANSACTION_findCurrentGuestUser = 53;
        static final int TRANSACTION_getApplicationRestrictions = 47;
        static final int TRANSACTION_getApplicationRestrictionsForUser = 48;
        static final int TRANSACTION_getCredentialOwnerProfile = 1;
        static final int TRANSACTION_getDefaultGuestRestrictions = 50;
        static final int TRANSACTION_getPreInstallableSystemPackages = 7;
        static final int TRANSACTION_getPrimaryUser = 16;
        static final int TRANSACTION_getProfileIds = 19;
        static final int TRANSACTION_getProfileParent = 26;
        static final int TRANSACTION_getProfileParentId = 2;
        static final int TRANSACTION_getProfileType = 63;
        static final int TRANSACTION_getProfiles = 18;
        static final int TRANSACTION_getRemainingCreatableProfileCount = 23;
        static final int TRANSACTION_getRemainingCreatableUserCount = 22;
        static final int TRANSACTION_getSeedAccountName = 57;
        static final int TRANSACTION_getSeedAccountOptions = 59;
        static final int TRANSACTION_getSeedAccountType = 58;
        static final int TRANSACTION_getUserAccount = 30;
        static final int TRANSACTION_getUserBadgeColorResId = 74;
        static final int TRANSACTION_getUserBadgeDarkColorResId = 75;
        static final int TRANSACTION_getUserBadgeLabelResId = 73;
        static final int TRANSACTION_getUserBadgeNoBackgroundResId = 72;
        static final int TRANSACTION_getUserBadgeResId = 71;
        static final int TRANSACTION_getUserCreationTime = 32;
        static final int TRANSACTION_getUserHandle = 36;
        static final int TRANSACTION_getUserIcon = 15;
        static final int TRANSACTION_getUserIconBadgeResId = 70;
        static final int TRANSACTION_getUserInfo = 29;
        static final int TRANSACTION_getUserName = 83;
        static final int TRANSACTION_getUserRestrictionSource = 37;
        static final int TRANSACTION_getUserRestrictionSources = 38;
        static final int TRANSACTION_getUserRestrictions = 39;
        static final int TRANSACTION_getUserSerialNumber = 35;
        static final int TRANSACTION_getUserStartRealtime = 84;
        static final int TRANSACTION_getUserUnlockRealtime = 85;
        static final int TRANSACTION_getUsers = 17;
        static final int TRANSACTION_hasBadge = 76;
        static final int TRANSACTION_hasBaseUserRestriction = 40;
        static final int TRANSACTION_hasRestrictedProfiles = 81;
        static final int TRANSACTION_hasUserRestriction = 41;
        static final int TRANSACTION_hasUserRestrictionOnAnyUser = 42;
        static final int TRANSACTION_isCredentialSharableWithParent = 65;
        static final int TRANSACTION_isDemoUser = 66;
        static final int TRANSACTION_isDualProfile = 86;
        static final int TRANSACTION_isMediaSharedWithParent = 64;
        static final int TRANSACTION_isPreCreated = 67;
        static final int TRANSACTION_isQuietModeEnabled = 54;
        static final int TRANSACTION_isRestricted = 33;
        static final int TRANSACTION_isSameProfileGroup = 27;
        static final int TRANSACTION_isSettingRestrictedForUser = 43;
        static final int TRANSACTION_isUserForeground = 79;
        static final int TRANSACTION_isUserNameSet = 80;
        static final int TRANSACTION_isUserOfType = 28;
        static final int TRANSACTION_isUserRunning = 78;
        static final int TRANSACTION_isUserTypeEnabled = 20;
        static final int TRANSACTION_isUserUnlocked = 77;
        static final int TRANSACTION_isUserUnlockingOrUnlocked = 69;
        static final int TRANSACTION_markGuestForDeletion = 52;
        static final int TRANSACTION_preCreateUserWithThrow = 4;
        static final int TRANSACTION_removeUser = 11;
        static final int TRANSACTION_removeUserEvenWhenDisallowed = 12;
        static final int TRANSACTION_removeUserWhenPossible = 51;
        static final int TRANSACTION_requestQuietModeEnabled = 82;
        static final int TRANSACTION_setApplicationRestrictions = 46;
        static final int TRANSACTION_setDefaultGuestRestrictions = 49;
        static final int TRANSACTION_setSeedAccountData = 56;
        static final int TRANSACTION_setUserAccount = 31;
        static final int TRANSACTION_setUserAdmin = 9;
        static final int TRANSACTION_setUserEnabled = 8;
        static final int TRANSACTION_setUserIcon = 14;
        static final int TRANSACTION_setUserName = 13;
        static final int TRANSACTION_setUserRestriction = 45;
        static final int TRANSACTION_someUserHasAccount = 62;
        static final int TRANSACTION_someUserHasSeedAccount = 61;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IUserManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IUserManager)) {
                return (IUserManager) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public static String getDefaultTransactionName(int transactionCode) {
            switch (transactionCode) {
                case 1:
                    return "getCredentialOwnerProfile";
                case 2:
                    return "getProfileParentId";
                case 3:
                    return "createUserWithThrow";
                case 4:
                    return "preCreateUserWithThrow";
                case 5:
                    return "createProfileForUserWithThrow";
                case 6:
                    return "createRestrictedProfileWithThrow";
                case 7:
                    return "getPreInstallableSystemPackages";
                case 8:
                    return "setUserEnabled";
                case 9:
                    return "setUserAdmin";
                case 10:
                    return "evictCredentialEncryptionKey";
                case 11:
                    return "removeUser";
                case 12:
                    return "removeUserEvenWhenDisallowed";
                case 13:
                    return "setUserName";
                case 14:
                    return "setUserIcon";
                case 15:
                    return "getUserIcon";
                case 16:
                    return "getPrimaryUser";
                case 17:
                    return "getUsers";
                case 18:
                    return "getProfiles";
                case 19:
                    return "getProfileIds";
                case 20:
                    return "isUserTypeEnabled";
                case 21:
                    return "canAddMoreUsersOfType";
                case 22:
                    return "getRemainingCreatableUserCount";
                case 23:
                    return "getRemainingCreatableProfileCount";
                case 24:
                    return "canAddMoreProfilesToUser";
                case 25:
                    return "canAddMoreManagedProfiles";
                case 26:
                    return "getProfileParent";
                case 27:
                    return "isSameProfileGroup";
                case 28:
                    return "isUserOfType";
                case 29:
                    return "getUserInfo";
                case 30:
                    return "getUserAccount";
                case 31:
                    return "setUserAccount";
                case 32:
                    return "getUserCreationTime";
                case 33:
                    return "isRestricted";
                case 34:
                    return "canHaveRestrictedProfile";
                case 35:
                    return "getUserSerialNumber";
                case 36:
                    return "getUserHandle";
                case 37:
                    return "getUserRestrictionSource";
                case 38:
                    return "getUserRestrictionSources";
                case 39:
                    return "getUserRestrictions";
                case 40:
                    return "hasBaseUserRestriction";
                case 41:
                    return "hasUserRestriction";
                case 42:
                    return "hasUserRestrictionOnAnyUser";
                case 43:
                    return "isSettingRestrictedForUser";
                case 44:
                    return "addUserRestrictionsListener";
                case 45:
                    return "setUserRestriction";
                case 46:
                    return "setApplicationRestrictions";
                case 47:
                    return "getApplicationRestrictions";
                case 48:
                    return "getApplicationRestrictionsForUser";
                case 49:
                    return "setDefaultGuestRestrictions";
                case 50:
                    return "getDefaultGuestRestrictions";
                case 51:
                    return "removeUserWhenPossible";
                case 52:
                    return "markGuestForDeletion";
                case 53:
                    return "findCurrentGuestUser";
                case 54:
                    return "isQuietModeEnabled";
                case 55:
                    return "createUserWithAttributes";
                case 56:
                    return "setSeedAccountData";
                case 57:
                    return "getSeedAccountName";
                case 58:
                    return "getSeedAccountType";
                case 59:
                    return "getSeedAccountOptions";
                case 60:
                    return "clearSeedAccountData";
                case 61:
                    return "someUserHasSeedAccount";
                case 62:
                    return "someUserHasAccount";
                case 63:
                    return "getProfileType";
                case 64:
                    return "isMediaSharedWithParent";
                case 65:
                    return "isCredentialSharableWithParent";
                case 66:
                    return "isDemoUser";
                case 67:
                    return "isPreCreated";
                case 68:
                    return "createProfileForUserEvenWhenDisallowedWithThrow";
                case 69:
                    return "isUserUnlockingOrUnlocked";
                case 70:
                    return "getUserIconBadgeResId";
                case 71:
                    return "getUserBadgeResId";
                case 72:
                    return "getUserBadgeNoBackgroundResId";
                case 73:
                    return "getUserBadgeLabelResId";
                case 74:
                    return "getUserBadgeColorResId";
                case 75:
                    return "getUserBadgeDarkColorResId";
                case 76:
                    return "hasBadge";
                case 77:
                    return "isUserUnlocked";
                case 78:
                    return "isUserRunning";
                case 79:
                    return "isUserForeground";
                case 80:
                    return "isUserNameSet";
                case 81:
                    return "hasRestrictedProfiles";
                case 82:
                    return "requestQuietModeEnabled";
                case 83:
                    return "getUserName";
                case 84:
                    return "getUserStartRealtime";
                case 85:
                    return "getUserUnlockRealtime";
                case 86:
                    return "isDualProfile";
                default:
                    return null;
            }
        }

        @Override // android.os.Binder
        public String getTransactionName(int transactionCode) {
            return getDefaultTransactionName(transactionCode);
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            int _arg0 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result = getCredentialOwnerProfile(_arg0);
                            reply.writeNoException();
                            reply.writeInt(_result);
                            break;
                        case 2:
                            int _arg02 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result2 = getProfileParentId(_arg02);
                            reply.writeNoException();
                            reply.writeInt(_result2);
                            break;
                        case 3:
                            String _arg03 = data.readString();
                            String _arg1 = data.readString();
                            int _arg2 = data.readInt();
                            data.enforceNoDataAvail();
                            UserInfo _result3 = createUserWithThrow(_arg03, _arg1, _arg2);
                            reply.writeNoException();
                            reply.writeTypedObject(_result3, 1);
                            break;
                        case 4:
                            String _arg04 = data.readString();
                            data.enforceNoDataAvail();
                            UserInfo _result4 = preCreateUserWithThrow(_arg04);
                            reply.writeNoException();
                            reply.writeTypedObject(_result4, 1);
                            break;
                        case 5:
                            String _arg05 = data.readString();
                            String _arg12 = data.readString();
                            int _arg22 = data.readInt();
                            int _arg3 = data.readInt();
                            String[] _arg4 = data.createStringArray();
                            data.enforceNoDataAvail();
                            UserInfo _result5 = createProfileForUserWithThrow(_arg05, _arg12, _arg22, _arg3, _arg4);
                            reply.writeNoException();
                            reply.writeTypedObject(_result5, 1);
                            break;
                        case 6:
                            String _arg06 = data.readString();
                            int _arg13 = data.readInt();
                            data.enforceNoDataAvail();
                            UserInfo _result6 = createRestrictedProfileWithThrow(_arg06, _arg13);
                            reply.writeNoException();
                            reply.writeTypedObject(_result6, 1);
                            break;
                        case 7:
                            String _arg07 = data.readString();
                            data.enforceNoDataAvail();
                            String[] _result7 = getPreInstallableSystemPackages(_arg07);
                            reply.writeNoException();
                            reply.writeStringArray(_result7);
                            break;
                        case 8:
                            int _arg08 = data.readInt();
                            data.enforceNoDataAvail();
                            setUserEnabled(_arg08);
                            reply.writeNoException();
                            break;
                        case 9:
                            int _arg09 = data.readInt();
                            data.enforceNoDataAvail();
                            setUserAdmin(_arg09);
                            reply.writeNoException();
                            break;
                        case 10:
                            int _arg010 = data.readInt();
                            data.enforceNoDataAvail();
                            evictCredentialEncryptionKey(_arg010);
                            reply.writeNoException();
                            break;
                        case 11:
                            int _arg011 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result8 = removeUser(_arg011);
                            reply.writeNoException();
                            reply.writeBoolean(_result8);
                            break;
                        case 12:
                            int _arg012 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result9 = removeUserEvenWhenDisallowed(_arg012);
                            reply.writeNoException();
                            reply.writeBoolean(_result9);
                            break;
                        case 13:
                            int _arg013 = data.readInt();
                            String _arg14 = data.readString();
                            data.enforceNoDataAvail();
                            setUserName(_arg013, _arg14);
                            reply.writeNoException();
                            break;
                        case 14:
                            int _arg014 = data.readInt();
                            Bitmap _arg15 = (Bitmap) data.readTypedObject(Bitmap.CREATOR);
                            data.enforceNoDataAvail();
                            setUserIcon(_arg014, _arg15);
                            reply.writeNoException();
                            break;
                        case 15:
                            int _arg015 = data.readInt();
                            data.enforceNoDataAvail();
                            ParcelFileDescriptor _result10 = getUserIcon(_arg015);
                            reply.writeNoException();
                            reply.writeTypedObject(_result10, 1);
                            break;
                        case 16:
                            UserInfo _result11 = getPrimaryUser();
                            reply.writeNoException();
                            reply.writeTypedObject(_result11, 1);
                            break;
                        case 17:
                            boolean _arg016 = data.readBoolean();
                            boolean _arg16 = data.readBoolean();
                            boolean _arg23 = data.readBoolean();
                            data.enforceNoDataAvail();
                            List<UserInfo> _result12 = getUsers(_arg016, _arg16, _arg23);
                            reply.writeNoException();
                            reply.writeTypedList(_result12);
                            break;
                        case 18:
                            int _arg017 = data.readInt();
                            boolean _arg17 = data.readBoolean();
                            data.enforceNoDataAvail();
                            List<UserInfo> _result13 = getProfiles(_arg017, _arg17);
                            reply.writeNoException();
                            reply.writeTypedList(_result13);
                            break;
                        case 19:
                            int _arg018 = data.readInt();
                            boolean _arg18 = data.readBoolean();
                            data.enforceNoDataAvail();
                            int[] _result14 = getProfileIds(_arg018, _arg18);
                            reply.writeNoException();
                            reply.writeIntArray(_result14);
                            break;
                        case 20:
                            String _arg019 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result15 = isUserTypeEnabled(_arg019);
                            reply.writeNoException();
                            reply.writeBoolean(_result15);
                            break;
                        case 21:
                            String _arg020 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result16 = canAddMoreUsersOfType(_arg020);
                            reply.writeNoException();
                            reply.writeBoolean(_result16);
                            break;
                        case 22:
                            String _arg021 = data.readString();
                            data.enforceNoDataAvail();
                            int _result17 = getRemainingCreatableUserCount(_arg021);
                            reply.writeNoException();
                            reply.writeInt(_result17);
                            break;
                        case 23:
                            String _arg022 = data.readString();
                            int _arg19 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result18 = getRemainingCreatableProfileCount(_arg022, _arg19);
                            reply.writeNoException();
                            reply.writeInt(_result18);
                            break;
                        case 24:
                            String _arg023 = data.readString();
                            int _arg110 = data.readInt();
                            boolean _arg24 = data.readBoolean();
                            data.enforceNoDataAvail();
                            boolean _result19 = canAddMoreProfilesToUser(_arg023, _arg110, _arg24);
                            reply.writeNoException();
                            reply.writeBoolean(_result19);
                            break;
                        case 25:
                            int _arg024 = data.readInt();
                            boolean _arg111 = data.readBoolean();
                            data.enforceNoDataAvail();
                            boolean _result20 = canAddMoreManagedProfiles(_arg024, _arg111);
                            reply.writeNoException();
                            reply.writeBoolean(_result20);
                            break;
                        case 26:
                            int _arg025 = data.readInt();
                            data.enforceNoDataAvail();
                            UserInfo _result21 = getProfileParent(_arg025);
                            reply.writeNoException();
                            reply.writeTypedObject(_result21, 1);
                            break;
                        case 27:
                            int _arg026 = data.readInt();
                            int _arg112 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result22 = isSameProfileGroup(_arg026, _arg112);
                            reply.writeNoException();
                            reply.writeBoolean(_result22);
                            break;
                        case 28:
                            int _arg027 = data.readInt();
                            String _arg113 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result23 = isUserOfType(_arg027, _arg113);
                            reply.writeNoException();
                            reply.writeBoolean(_result23);
                            break;
                        case 29:
                            int _arg028 = data.readInt();
                            data.enforceNoDataAvail();
                            UserInfo _result24 = getUserInfo(_arg028);
                            reply.writeNoException();
                            reply.writeTypedObject(_result24, 1);
                            break;
                        case 30:
                            int _arg029 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result25 = getUserAccount(_arg029);
                            reply.writeNoException();
                            reply.writeString(_result25);
                            break;
                        case 31:
                            int _arg030 = data.readInt();
                            String _arg114 = data.readString();
                            data.enforceNoDataAvail();
                            setUserAccount(_arg030, _arg114);
                            reply.writeNoException();
                            break;
                        case 32:
                            int _arg031 = data.readInt();
                            data.enforceNoDataAvail();
                            long _result26 = getUserCreationTime(_arg031);
                            reply.writeNoException();
                            reply.writeLong(_result26);
                            break;
                        case 33:
                            int _arg032 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result27 = isRestricted(_arg032);
                            reply.writeNoException();
                            reply.writeBoolean(_result27);
                            break;
                        case 34:
                            int _arg033 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result28 = canHaveRestrictedProfile(_arg033);
                            reply.writeNoException();
                            reply.writeBoolean(_result28);
                            break;
                        case 35:
                            int _arg034 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result29 = getUserSerialNumber(_arg034);
                            reply.writeNoException();
                            reply.writeInt(_result29);
                            break;
                        case 36:
                            int _arg035 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result30 = getUserHandle(_arg035);
                            reply.writeNoException();
                            reply.writeInt(_result30);
                            break;
                        case 37:
                            String _arg036 = data.readString();
                            int _arg115 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result31 = getUserRestrictionSource(_arg036, _arg115);
                            reply.writeNoException();
                            reply.writeInt(_result31);
                            break;
                        case 38:
                            String _arg037 = data.readString();
                            int _arg116 = data.readInt();
                            data.enforceNoDataAvail();
                            List<UserManager.EnforcingUser> _result32 = getUserRestrictionSources(_arg037, _arg116);
                            reply.writeNoException();
                            reply.writeTypedList(_result32);
                            break;
                        case 39:
                            int _arg038 = data.readInt();
                            data.enforceNoDataAvail();
                            Bundle _result33 = getUserRestrictions(_arg038);
                            reply.writeNoException();
                            reply.writeTypedObject(_result33, 1);
                            break;
                        case 40:
                            String _arg039 = data.readString();
                            int _arg117 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result34 = hasBaseUserRestriction(_arg039, _arg117);
                            reply.writeNoException();
                            reply.writeBoolean(_result34);
                            break;
                        case 41:
                            String _arg040 = data.readString();
                            int _arg118 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result35 = hasUserRestriction(_arg040, _arg118);
                            reply.writeNoException();
                            reply.writeBoolean(_result35);
                            break;
                        case 42:
                            String _arg041 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result36 = hasUserRestrictionOnAnyUser(_arg041);
                            reply.writeNoException();
                            reply.writeBoolean(_result36);
                            break;
                        case 43:
                            String _arg042 = data.readString();
                            int _arg119 = data.readInt();
                            String _arg25 = data.readString();
                            int _arg32 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result37 = isSettingRestrictedForUser(_arg042, _arg119, _arg25, _arg32);
                            reply.writeNoException();
                            reply.writeBoolean(_result37);
                            break;
                        case 44:
                            IUserRestrictionsListener _arg043 = IUserRestrictionsListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            addUserRestrictionsListener(_arg043);
                            reply.writeNoException();
                            break;
                        case 45:
                            String _arg044 = data.readString();
                            boolean _arg120 = data.readBoolean();
                            int _arg26 = data.readInt();
                            data.enforceNoDataAvail();
                            setUserRestriction(_arg044, _arg120, _arg26);
                            reply.writeNoException();
                            break;
                        case 46:
                            String _arg045 = data.readString();
                            Bundle _arg121 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            int _arg27 = data.readInt();
                            data.enforceNoDataAvail();
                            setApplicationRestrictions(_arg045, _arg121, _arg27);
                            reply.writeNoException();
                            break;
                        case 47:
                            String _arg046 = data.readString();
                            data.enforceNoDataAvail();
                            Bundle _result38 = getApplicationRestrictions(_arg046);
                            reply.writeNoException();
                            reply.writeTypedObject(_result38, 1);
                            break;
                        case 48:
                            String _arg047 = data.readString();
                            int _arg122 = data.readInt();
                            data.enforceNoDataAvail();
                            Bundle _result39 = getApplicationRestrictionsForUser(_arg047, _arg122);
                            reply.writeNoException();
                            reply.writeTypedObject(_result39, 1);
                            break;
                        case 49:
                            Bundle _arg048 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            setDefaultGuestRestrictions(_arg048);
                            reply.writeNoException();
                            break;
                        case 50:
                            Bundle _result40 = getDefaultGuestRestrictions();
                            reply.writeNoException();
                            reply.writeTypedObject(_result40, 1);
                            break;
                        case 51:
                            int _arg049 = data.readInt();
                            boolean _arg123 = data.readBoolean();
                            data.enforceNoDataAvail();
                            int _result41 = removeUserWhenPossible(_arg049, _arg123);
                            reply.writeNoException();
                            reply.writeInt(_result41);
                            break;
                        case 52:
                            int _arg050 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result42 = markGuestForDeletion(_arg050);
                            reply.writeNoException();
                            reply.writeBoolean(_result42);
                            break;
                        case 53:
                            UserInfo _result43 = findCurrentGuestUser();
                            reply.writeNoException();
                            reply.writeTypedObject(_result43, 1);
                            break;
                        case 54:
                            int _arg051 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result44 = isQuietModeEnabled(_arg051);
                            reply.writeNoException();
                            reply.writeBoolean(_result44);
                            break;
                        case 55:
                            String _arg052 = data.readString();
                            String _arg124 = data.readString();
                            int _arg28 = data.readInt();
                            Bitmap _arg33 = (Bitmap) data.readTypedObject(Bitmap.CREATOR);
                            String _arg42 = data.readString();
                            String _arg5 = data.readString();
                            PersistableBundle _arg6 = (PersistableBundle) data.readTypedObject(PersistableBundle.CREATOR);
                            data.enforceNoDataAvail();
                            UserHandle _result45 = createUserWithAttributes(_arg052, _arg124, _arg28, _arg33, _arg42, _arg5, _arg6);
                            reply.writeNoException();
                            reply.writeTypedObject(_result45, 1);
                            break;
                        case 56:
                            int _arg053 = data.readInt();
                            String _arg125 = data.readString();
                            String _arg29 = data.readString();
                            PersistableBundle _arg34 = (PersistableBundle) data.readTypedObject(PersistableBundle.CREATOR);
                            boolean _arg43 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setSeedAccountData(_arg053, _arg125, _arg29, _arg34, _arg43);
                            reply.writeNoException();
                            break;
                        case 57:
                            int _arg054 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result46 = getSeedAccountName(_arg054);
                            reply.writeNoException();
                            reply.writeString(_result46);
                            break;
                        case 58:
                            int _arg055 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result47 = getSeedAccountType(_arg055);
                            reply.writeNoException();
                            reply.writeString(_result47);
                            break;
                        case 59:
                            int _arg056 = data.readInt();
                            data.enforceNoDataAvail();
                            PersistableBundle _result48 = getSeedAccountOptions(_arg056);
                            reply.writeNoException();
                            reply.writeTypedObject(_result48, 1);
                            break;
                        case 60:
                            int _arg057 = data.readInt();
                            data.enforceNoDataAvail();
                            clearSeedAccountData(_arg057);
                            reply.writeNoException();
                            break;
                        case 61:
                            String _arg058 = data.readString();
                            String _arg126 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result49 = someUserHasSeedAccount(_arg058, _arg126);
                            reply.writeNoException();
                            reply.writeBoolean(_result49);
                            break;
                        case 62:
                            String _arg059 = data.readString();
                            String _arg127 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result50 = someUserHasAccount(_arg059, _arg127);
                            reply.writeNoException();
                            reply.writeBoolean(_result50);
                            break;
                        case 63:
                            int _arg060 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result51 = getProfileType(_arg060);
                            reply.writeNoException();
                            reply.writeString(_result51);
                            break;
                        case 64:
                            int _arg061 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result52 = isMediaSharedWithParent(_arg061);
                            reply.writeNoException();
                            reply.writeBoolean(_result52);
                            break;
                        case 65:
                            int _arg062 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result53 = isCredentialSharableWithParent(_arg062);
                            reply.writeNoException();
                            reply.writeBoolean(_result53);
                            break;
                        case 66:
                            int _arg063 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result54 = isDemoUser(_arg063);
                            reply.writeNoException();
                            reply.writeBoolean(_result54);
                            break;
                        case 67:
                            int _arg064 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result55 = isPreCreated(_arg064);
                            reply.writeNoException();
                            reply.writeBoolean(_result55);
                            break;
                        case 68:
                            String _arg065 = data.readString();
                            String _arg128 = data.readString();
                            int _arg210 = data.readInt();
                            int _arg35 = data.readInt();
                            String[] _arg44 = data.createStringArray();
                            data.enforceNoDataAvail();
                            UserInfo _result56 = createProfileForUserEvenWhenDisallowedWithThrow(_arg065, _arg128, _arg210, _arg35, _arg44);
                            reply.writeNoException();
                            reply.writeTypedObject(_result56, 1);
                            break;
                        case 69:
                            int _arg066 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result57 = isUserUnlockingOrUnlocked(_arg066);
                            reply.writeNoException();
                            reply.writeBoolean(_result57);
                            break;
                        case 70:
                            int _arg067 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result58 = getUserIconBadgeResId(_arg067);
                            reply.writeNoException();
                            reply.writeInt(_result58);
                            break;
                        case 71:
                            int _arg068 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result59 = getUserBadgeResId(_arg068);
                            reply.writeNoException();
                            reply.writeInt(_result59);
                            break;
                        case 72:
                            int _arg069 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result60 = getUserBadgeNoBackgroundResId(_arg069);
                            reply.writeNoException();
                            reply.writeInt(_result60);
                            break;
                        case 73:
                            int _arg070 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result61 = getUserBadgeLabelResId(_arg070);
                            reply.writeNoException();
                            reply.writeInt(_result61);
                            break;
                        case 74:
                            int _arg071 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result62 = getUserBadgeColorResId(_arg071);
                            reply.writeNoException();
                            reply.writeInt(_result62);
                            break;
                        case 75:
                            int _arg072 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result63 = getUserBadgeDarkColorResId(_arg072);
                            reply.writeNoException();
                            reply.writeInt(_result63);
                            break;
                        case 76:
                            int _arg073 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result64 = hasBadge(_arg073);
                            reply.writeNoException();
                            reply.writeBoolean(_result64);
                            break;
                        case 77:
                            int _arg074 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result65 = isUserUnlocked(_arg074);
                            reply.writeNoException();
                            reply.writeBoolean(_result65);
                            break;
                        case 78:
                            int _arg075 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result66 = isUserRunning(_arg075);
                            reply.writeNoException();
                            reply.writeBoolean(_result66);
                            break;
                        case 79:
                            int _arg076 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result67 = isUserForeground(_arg076);
                            reply.writeNoException();
                            reply.writeBoolean(_result67);
                            break;
                        case 80:
                            int _arg077 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result68 = isUserNameSet(_arg077);
                            reply.writeNoException();
                            reply.writeBoolean(_result68);
                            break;
                        case 81:
                            int _arg078 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result69 = hasRestrictedProfiles(_arg078);
                            reply.writeNoException();
                            reply.writeBoolean(_result69);
                            break;
                        case 82:
                            String _arg079 = data.readString();
                            boolean _arg129 = data.readBoolean();
                            int _arg211 = data.readInt();
                            IntentSender _arg36 = (IntentSender) data.readTypedObject(IntentSender.CREATOR);
                            int _arg45 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result70 = requestQuietModeEnabled(_arg079, _arg129, _arg211, _arg36, _arg45);
                            reply.writeNoException();
                            reply.writeBoolean(_result70);
                            break;
                        case 83:
                            String _result71 = getUserName();
                            reply.writeNoException();
                            reply.writeString(_result71);
                            break;
                        case 84:
                            long _result72 = getUserStartRealtime();
                            reply.writeNoException();
                            reply.writeLong(_result72);
                            break;
                        case 85:
                            long _result73 = getUserUnlockRealtime();
                            reply.writeNoException();
                            reply.writeLong(_result73);
                            break;
                        case 86:
                            int _arg080 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result74 = isDualProfile(_arg080);
                            reply.writeNoException();
                            reply.writeBoolean(_result74);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements IUserManager {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override // android.os.IUserManager
            public int getCredentialOwnerProfile(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public int getProfileParentId(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public UserInfo createUserWithThrow(String name, String userType, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeString(userType);
                    _data.writeInt(flags);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    UserInfo _result = (UserInfo) _reply.readTypedObject(UserInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public UserInfo preCreateUserWithThrow(String userType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(userType);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    UserInfo _result = (UserInfo) _reply.readTypedObject(UserInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public UserInfo createProfileForUserWithThrow(String name, String userType, int flags, int userId, String[] disallowedPackages) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeString(userType);
                    _data.writeInt(flags);
                    _data.writeInt(userId);
                    _data.writeStringArray(disallowedPackages);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    UserInfo _result = (UserInfo) _reply.readTypedObject(UserInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public UserInfo createRestrictedProfileWithThrow(String name, int parentUserHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeInt(parentUserHandle);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    UserInfo _result = (UserInfo) _reply.readTypedObject(UserInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public String[] getPreInstallableSystemPackages(String userType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(userType);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public void setUserEnabled(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public void setUserAdmin(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public void evictCredentialEncryptionKey(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean removeUser(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean removeUserEvenWhenDisallowed(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public void setUserName(int userId, String name) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeString(name);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public void setUserIcon(int userId, Bitmap icon) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeTypedObject(icon, 0);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public ParcelFileDescriptor getUserIcon(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    ParcelFileDescriptor _result = (ParcelFileDescriptor) _reply.readTypedObject(ParcelFileDescriptor.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public UserInfo getPrimaryUser() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    UserInfo _result = (UserInfo) _reply.readTypedObject(UserInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public List<UserInfo> getUsers(boolean excludePartial, boolean excludeDying, boolean excludePreCreated) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(excludePartial);
                    _data.writeBoolean(excludeDying);
                    _data.writeBoolean(excludePreCreated);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    List<UserInfo> _result = _reply.createTypedArrayList(UserInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public List<UserInfo> getProfiles(int userId, boolean enabledOnly) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeBoolean(enabledOnly);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    List<UserInfo> _result = _reply.createTypedArrayList(UserInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public int[] getProfileIds(int userId, boolean enabledOnly) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeBoolean(enabledOnly);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean isUserTypeEnabled(String userType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(userType);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean canAddMoreUsersOfType(String userType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(userType);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public int getRemainingCreatableUserCount(String userType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(userType);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public int getRemainingCreatableProfileCount(String userType, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(userType);
                    _data.writeInt(userId);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean canAddMoreProfilesToUser(String userType, int userId, boolean allowedToRemoveOne) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(userType);
                    _data.writeInt(userId);
                    _data.writeBoolean(allowedToRemoveOne);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean canAddMoreManagedProfiles(int userId, boolean allowedToRemoveOne) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeBoolean(allowedToRemoveOne);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public UserInfo getProfileParent(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    UserInfo _result = (UserInfo) _reply.readTypedObject(UserInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean isSameProfileGroup(int userId, int otherUserHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeInt(otherUserHandle);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean isUserOfType(int userId, String userType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeString(userType);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public UserInfo getUserInfo(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    UserInfo _result = (UserInfo) _reply.readTypedObject(UserInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public String getUserAccount(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public void setUserAccount(int userId, String accountName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeString(accountName);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public long getUserCreationTime(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean isRestricted(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean canHaveRestrictedProfile(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public int getUserSerialNumber(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public int getUserHandle(int userSerialNumber) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userSerialNumber);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public int getUserRestrictionSource(String restrictionKey, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(restrictionKey);
                    _data.writeInt(userId);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public List<UserManager.EnforcingUser> getUserRestrictionSources(String restrictionKey, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(restrictionKey);
                    _data.writeInt(userId);
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                    List<UserManager.EnforcingUser> _result = _reply.createTypedArrayList(UserManager.EnforcingUser.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public Bundle getUserRestrictions(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                    Bundle _result = (Bundle) _reply.readTypedObject(Bundle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean hasBaseUserRestriction(String restrictionKey, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(restrictionKey);
                    _data.writeInt(userId);
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean hasUserRestriction(String restrictionKey, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(restrictionKey);
                    _data.writeInt(userId);
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean hasUserRestrictionOnAnyUser(String restrictionKey) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(restrictionKey);
                    this.mRemote.transact(42, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean isSettingRestrictedForUser(String setting, int userId, String value, int callingUid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(setting);
                    _data.writeInt(userId);
                    _data.writeString(value);
                    _data.writeInt(callingUid);
                    this.mRemote.transact(43, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public void addUserRestrictionsListener(IUserRestrictionsListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(44, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public void setUserRestriction(String key, boolean value, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(key);
                    _data.writeBoolean(value);
                    _data.writeInt(userId);
                    this.mRemote.transact(45, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public void setApplicationRestrictions(String packageName, Bundle restrictions, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeTypedObject(restrictions, 0);
                    _data.writeInt(userId);
                    this.mRemote.transact(46, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public Bundle getApplicationRestrictions(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(47, _data, _reply, 0);
                    _reply.readException();
                    Bundle _result = (Bundle) _reply.readTypedObject(Bundle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public Bundle getApplicationRestrictionsForUser(String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(48, _data, _reply, 0);
                    _reply.readException();
                    Bundle _result = (Bundle) _reply.readTypedObject(Bundle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public void setDefaultGuestRestrictions(Bundle restrictions) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(restrictions, 0);
                    this.mRemote.transact(49, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public Bundle getDefaultGuestRestrictions() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(50, _data, _reply, 0);
                    _reply.readException();
                    Bundle _result = (Bundle) _reply.readTypedObject(Bundle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public int removeUserWhenPossible(int userId, boolean overrideDevicePolicy) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeBoolean(overrideDevicePolicy);
                    this.mRemote.transact(51, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean markGuestForDeletion(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(52, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public UserInfo findCurrentGuestUser() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(53, _data, _reply, 0);
                    _reply.readException();
                    UserInfo _result = (UserInfo) _reply.readTypedObject(UserInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean isQuietModeEnabled(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(54, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public UserHandle createUserWithAttributes(String userName, String userType, int flags, Bitmap userIcon, String accountName, String accountType, PersistableBundle accountOptions) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(userName);
                    _data.writeString(userType);
                    _data.writeInt(flags);
                    _data.writeTypedObject(userIcon, 0);
                    _data.writeString(accountName);
                    _data.writeString(accountType);
                    _data.writeTypedObject(accountOptions, 0);
                    this.mRemote.transact(55, _data, _reply, 0);
                    _reply.readException();
                    UserHandle _result = (UserHandle) _reply.readTypedObject(UserHandle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public void setSeedAccountData(int userId, String accountName, String accountType, PersistableBundle accountOptions, boolean persist) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeString(accountName);
                    _data.writeString(accountType);
                    _data.writeTypedObject(accountOptions, 0);
                    _data.writeBoolean(persist);
                    this.mRemote.transact(56, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public String getSeedAccountName(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(57, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public String getSeedAccountType(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(58, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public PersistableBundle getSeedAccountOptions(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(59, _data, _reply, 0);
                    _reply.readException();
                    PersistableBundle _result = (PersistableBundle) _reply.readTypedObject(PersistableBundle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public void clearSeedAccountData(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(60, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean someUserHasSeedAccount(String accountName, String accountType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(accountName);
                    _data.writeString(accountType);
                    this.mRemote.transact(61, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean someUserHasAccount(String accountName, String accountType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(accountName);
                    _data.writeString(accountType);
                    this.mRemote.transact(62, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public String getProfileType(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(63, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean isMediaSharedWithParent(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(64, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean isCredentialSharableWithParent(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(65, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean isDemoUser(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(66, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean isPreCreated(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(67, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public UserInfo createProfileForUserEvenWhenDisallowedWithThrow(String name, String userType, int flags, int userId, String[] disallowedPackages) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeString(userType);
                    _data.writeInt(flags);
                    _data.writeInt(userId);
                    _data.writeStringArray(disallowedPackages);
                    this.mRemote.transact(68, _data, _reply, 0);
                    _reply.readException();
                    UserInfo _result = (UserInfo) _reply.readTypedObject(UserInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean isUserUnlockingOrUnlocked(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(69, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public int getUserIconBadgeResId(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(70, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public int getUserBadgeResId(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(71, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public int getUserBadgeNoBackgroundResId(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(72, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public int getUserBadgeLabelResId(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(73, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public int getUserBadgeColorResId(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(74, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public int getUserBadgeDarkColorResId(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(75, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean hasBadge(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(76, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean isUserUnlocked(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(77, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean isUserRunning(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(78, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean isUserForeground(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(79, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean isUserNameSet(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(80, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean hasRestrictedProfiles(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(81, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean requestQuietModeEnabled(String callingPackage, boolean enableQuietMode, int userId, IntentSender target, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeBoolean(enableQuietMode);
                    _data.writeInt(userId);
                    _data.writeTypedObject(target, 0);
                    _data.writeInt(flags);
                    this.mRemote.transact(82, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public String getUserName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(83, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public long getUserStartRealtime() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(84, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public long getUserUnlockRealtime() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(85, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IUserManager
            public boolean isDualProfile(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(86, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 85;
        }
    }
}
