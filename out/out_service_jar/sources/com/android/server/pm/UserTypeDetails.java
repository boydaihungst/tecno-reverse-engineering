package com.android.server.pm;

import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserManager;
import com.android.internal.util.Preconditions;
import com.android.server.BundleUtils;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/* loaded from: classes2.dex */
public final class UserTypeDetails {
    public static final int UNLIMITED_NUMBER_OF_USERS = -1;
    private final int[] mBadgeColors;
    private final int[] mBadgeLabels;
    private final int mBadgeNoBackground;
    private final int mBadgePlain;
    private final int mBaseType;
    private final int[] mDarkThemeBadgeColors;
    private final List<DefaultCrossProfileIntentFilter> mDefaultCrossProfileIntentFilters;
    private final Bundle mDefaultRestrictions;
    private final Bundle mDefaultSecureSettings;
    private final Bundle mDefaultSystemSettings;
    private final int mDefaultUserInfoPropertyFlags;
    private final boolean mEnabled;
    private final int mIconBadge;
    private final boolean mIsCredentialSharableWithParent;
    private final boolean mIsMediaSharedWithParent;
    private final int mLabel;
    private final int mMaxAllowed;
    private final int mMaxAllowedPerParent;
    private final String mName;

    private UserTypeDetails(String name, boolean enabled, int maxAllowed, int baseType, int defaultUserInfoPropertyFlags, int label, int maxAllowedPerParent, int iconBadge, int badgePlain, int badgeNoBackground, int[] badgeLabels, int[] badgeColors, int[] darkThemeBadgeColors, Bundle defaultRestrictions, Bundle defaultSystemSettings, Bundle defaultSecureSettings, List<DefaultCrossProfileIntentFilter> defaultCrossProfileIntentFilters, boolean isMediaSharedWithParent, boolean isCredentialSharableWithParent) {
        this.mName = name;
        this.mEnabled = enabled;
        this.mMaxAllowed = maxAllowed;
        this.mMaxAllowedPerParent = maxAllowedPerParent;
        this.mBaseType = baseType;
        this.mDefaultUserInfoPropertyFlags = defaultUserInfoPropertyFlags;
        this.mDefaultRestrictions = defaultRestrictions;
        this.mDefaultSystemSettings = defaultSystemSettings;
        this.mDefaultSecureSettings = defaultSecureSettings;
        this.mDefaultCrossProfileIntentFilters = defaultCrossProfileIntentFilters;
        this.mIconBadge = iconBadge;
        this.mBadgePlain = badgePlain;
        this.mBadgeNoBackground = badgeNoBackground;
        this.mLabel = label;
        this.mBadgeLabels = badgeLabels;
        this.mBadgeColors = badgeColors;
        this.mDarkThemeBadgeColors = darkThemeBadgeColors;
        this.mIsMediaSharedWithParent = isMediaSharedWithParent;
        this.mIsCredentialSharableWithParent = isCredentialSharableWithParent;
    }

    public String getName() {
        return this.mName;
    }

    public boolean isEnabled() {
        return this.mEnabled;
    }

    public int getMaxAllowed() {
        return this.mMaxAllowed;
    }

    public int getMaxAllowedPerParent() {
        return this.mMaxAllowedPerParent;
    }

    public int getDefaultUserInfoFlags() {
        return this.mDefaultUserInfoPropertyFlags | this.mBaseType;
    }

    public int getLabel() {
        return this.mLabel;
    }

    public boolean hasBadge() {
        return this.mIconBadge != 0;
    }

    public int getIconBadge() {
        return this.mIconBadge;
    }

    public int getBadgePlain() {
        return this.mBadgePlain;
    }

    public int getBadgeNoBackground() {
        return this.mBadgeNoBackground;
    }

    public int getBadgeLabel(int badgeIndex) {
        int[] iArr = this.mBadgeLabels;
        if (iArr == null || iArr.length == 0 || badgeIndex < 0) {
            return 0;
        }
        return iArr[Math.min(badgeIndex, iArr.length - 1)];
    }

    public int getBadgeColor(int badgeIndex) {
        int[] iArr = this.mBadgeColors;
        if (iArr == null || iArr.length == 0 || badgeIndex < 0) {
            return 0;
        }
        return iArr[Math.min(badgeIndex, iArr.length - 1)];
    }

    public int getDarkThemeBadgeColor(int badgeIndex) {
        int[] iArr = this.mDarkThemeBadgeColors;
        if (iArr == null || iArr.length == 0 || badgeIndex < 0) {
            return getBadgeColor(badgeIndex);
        }
        return iArr[Math.min(badgeIndex, iArr.length - 1)];
    }

    public boolean isProfile() {
        return (this.mBaseType & 4096) != 0;
    }

    public boolean isFull() {
        return (this.mBaseType & 1024) != 0;
    }

    public boolean isSystem() {
        return (this.mBaseType & 2048) != 0;
    }

    public boolean isMediaSharedWithParent() {
        return this.mIsMediaSharedWithParent;
    }

    public boolean isCredentialSharableWithParent() {
        return this.mIsCredentialSharableWithParent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Bundle getDefaultRestrictions() {
        return BundleUtils.clone(this.mDefaultRestrictions);
    }

    public void addDefaultRestrictionsTo(Bundle currentRestrictions) {
        UserRestrictionsUtils.merge(currentRestrictions, this.mDefaultRestrictions);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Bundle getDefaultSystemSettings() {
        return BundleUtils.clone(this.mDefaultSystemSettings);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Bundle getDefaultSecureSettings() {
        return BundleUtils.clone(this.mDefaultSecureSettings);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<DefaultCrossProfileIntentFilter> getDefaultCrossProfileIntentFilters() {
        if (this.mDefaultCrossProfileIntentFilters != null) {
            return new ArrayList(this.mDefaultCrossProfileIntentFilters);
        }
        return Collections.emptyList();
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("mName: ");
        pw.println(this.mName);
        pw.print(prefix);
        pw.print("mBaseType: ");
        pw.println(UserInfo.flagsToString(this.mBaseType));
        pw.print(prefix);
        pw.print("mEnabled: ");
        pw.println(this.mEnabled);
        pw.print(prefix);
        pw.print("mMaxAllowed: ");
        pw.println(this.mMaxAllowed);
        pw.print(prefix);
        pw.print("mMaxAllowedPerParent: ");
        pw.println(this.mMaxAllowedPerParent);
        pw.print(prefix);
        pw.print("mDefaultUserInfoFlags: ");
        pw.println(UserInfo.flagsToString(this.mDefaultUserInfoPropertyFlags));
        pw.print(prefix);
        pw.print("mLabel: ");
        pw.println(this.mLabel);
        String restrictionsPrefix = prefix + "    ";
        if (isSystem()) {
            pw.print(prefix);
            pw.println("config_defaultFirstUserRestrictions: ");
            try {
                Bundle restrictions = new Bundle();
                String[] defaultFirstUserRestrictions = Resources.getSystem().getStringArray(17236019);
                for (String userRestriction : defaultFirstUserRestrictions) {
                    if (UserRestrictionsUtils.isValidRestriction(userRestriction)) {
                        restrictions.putBoolean(userRestriction, true);
                    }
                }
                UserRestrictionsUtils.dumpRestrictions(pw, restrictionsPrefix, restrictions);
            } catch (Resources.NotFoundException e) {
                pw.print(restrictionsPrefix);
                pw.println("none - resource not found");
            }
        } else {
            pw.print(prefix);
            pw.println("mDefaultRestrictions: ");
            UserRestrictionsUtils.dumpRestrictions(pw, restrictionsPrefix, this.mDefaultRestrictions);
        }
        pw.print(prefix);
        pw.print("mIconBadge: ");
        pw.println(this.mIconBadge);
        pw.print(prefix);
        pw.print("mBadgePlain: ");
        pw.println(this.mBadgePlain);
        pw.print(prefix);
        pw.print("mBadgeNoBackground: ");
        pw.println(this.mBadgeNoBackground);
        pw.print(prefix);
        pw.print("mBadgeLabels.length: ");
        int[] iArr = this.mBadgeLabels;
        pw.println(iArr != null ? Integer.valueOf(iArr.length) : "0(null)");
        pw.print(prefix);
        pw.print("mBadgeColors.length: ");
        int[] iArr2 = this.mBadgeColors;
        pw.println(iArr2 != null ? Integer.valueOf(iArr2.length) : "0(null)");
        pw.print(prefix);
        pw.print("mDarkThemeBadgeColors.length: ");
        int[] iArr3 = this.mDarkThemeBadgeColors;
        pw.println(iArr3 != null ? Integer.valueOf(iArr3.length) : "0(null)");
    }

    /* loaded from: classes2.dex */
    public static final class Builder {
        private int mBaseType;
        private String mName;
        private int mMaxAllowed = -1;
        private int mMaxAllowedPerParent = -1;
        private int mDefaultUserInfoPropertyFlags = 0;
        private Bundle mDefaultRestrictions = null;
        private Bundle mDefaultSystemSettings = null;
        private Bundle mDefaultSecureSettings = null;
        private List<DefaultCrossProfileIntentFilter> mDefaultCrossProfileIntentFilters = null;
        private int mEnabled = 1;
        private int mLabel = 0;
        private int[] mBadgeLabels = null;
        private int[] mBadgeColors = null;
        private int[] mDarkThemeBadgeColors = null;
        private int mIconBadge = 0;
        private int mBadgePlain = 0;
        private int mBadgeNoBackground = 0;
        private boolean mIsMediaSharedWithParent = false;
        private boolean mIsCredentialSharableWithParent = false;

        public Builder setName(String name) {
            this.mName = name;
            return this;
        }

        public Builder setEnabled(int enabled) {
            this.mEnabled = enabled;
            return this;
        }

        public Builder setMaxAllowed(int maxAllowed) {
            this.mMaxAllowed = maxAllowed;
            return this;
        }

        public Builder setMaxAllowedPerParent(int maxAllowedPerParent) {
            this.mMaxAllowedPerParent = maxAllowedPerParent;
            return this;
        }

        public Builder setBaseType(int baseType) {
            this.mBaseType = baseType;
            return this;
        }

        public Builder setDefaultUserInfoPropertyFlags(int flags) {
            this.mDefaultUserInfoPropertyFlags = flags;
            return this;
        }

        public Builder setBadgeLabels(int... badgeLabels) {
            this.mBadgeLabels = badgeLabels;
            return this;
        }

        public Builder setBadgeColors(int... badgeColors) {
            this.mBadgeColors = badgeColors;
            return this;
        }

        public Builder setDarkThemeBadgeColors(int... darkThemeBadgeColors) {
            this.mDarkThemeBadgeColors = darkThemeBadgeColors;
            return this;
        }

        public Builder setIconBadge(int badgeIcon) {
            this.mIconBadge = badgeIcon;
            return this;
        }

        public Builder setBadgePlain(int badgePlain) {
            this.mBadgePlain = badgePlain;
            return this;
        }

        public Builder setBadgeNoBackground(int badgeNoBackground) {
            this.mBadgeNoBackground = badgeNoBackground;
            return this;
        }

        public Builder setLabel(int label) {
            this.mLabel = label;
            return this;
        }

        public Builder setDefaultRestrictions(Bundle restrictions) {
            this.mDefaultRestrictions = restrictions;
            return this;
        }

        public Builder setDefaultSystemSettings(Bundle settings) {
            this.mDefaultSystemSettings = settings;
            return this;
        }

        public Builder setDefaultSecureSettings(Bundle settings) {
            this.mDefaultSecureSettings = settings;
            return this;
        }

        public Builder setDefaultCrossProfileIntentFilters(List<DefaultCrossProfileIntentFilter> intentFilters) {
            this.mDefaultCrossProfileIntentFilters = intentFilters;
            return this;
        }

        public Builder setIsMediaSharedWithParent(boolean isMediaSharedWithParent) {
            this.mIsMediaSharedWithParent = isMediaSharedWithParent;
            return this;
        }

        public Builder setIsCredentialSharableWithParent(boolean isCredentialSharableWithParent) {
            this.mIsCredentialSharableWithParent = isCredentialSharableWithParent;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public int getBaseType() {
            return this.mBaseType;
        }

        public UserTypeDetails createUserTypeDetails() {
            Preconditions.checkArgument(this.mName != null, "Cannot create a UserTypeDetails with no name.");
            Preconditions.checkArgument(hasValidBaseType(), "UserTypeDetails " + this.mName + " has invalid baseType: " + this.mBaseType);
            Preconditions.checkArgument(hasValidPropertyFlags(), "UserTypeDetails " + this.mName + " has invalid flags: " + Integer.toHexString(this.mDefaultUserInfoPropertyFlags));
            if (hasBadge()) {
                int[] iArr = this.mBadgeLabels;
                Preconditions.checkArgument((iArr == null || iArr.length == 0) ? false : true, "UserTypeDetails " + this.mName + " has badge but no badgeLabels.");
                int[] iArr2 = this.mBadgeColors;
                Preconditions.checkArgument((iArr2 == null || iArr2.length == 0) ? false : true, "UserTypeDetails " + this.mName + " has badge but no badgeColors.");
            }
            if (!isProfile()) {
                List<DefaultCrossProfileIntentFilter> list = this.mDefaultCrossProfileIntentFilters;
                Preconditions.checkArgument(list == null || list.isEmpty(), "UserTypeDetails %s has a non empty defaultCrossProfileIntentFilters", new Object[]{this.mName});
            }
            String str = this.mName;
            boolean z = this.mEnabled != 0;
            int i = this.mMaxAllowed;
            int i2 = this.mBaseType;
            int i3 = this.mDefaultUserInfoPropertyFlags;
            int i4 = this.mLabel;
            int i5 = this.mMaxAllowedPerParent;
            int i6 = this.mIconBadge;
            int i7 = this.mBadgePlain;
            int i8 = this.mBadgeNoBackground;
            int[] iArr3 = this.mBadgeLabels;
            int[] iArr4 = this.mBadgeColors;
            int[] iArr5 = this.mDarkThemeBadgeColors;
            return new UserTypeDetails(str, z, i, i2, i3, i4, i5, i6, i7, i8, iArr3, iArr4, iArr5 == null ? iArr4 : iArr5, this.mDefaultRestrictions, this.mDefaultSystemSettings, this.mDefaultSecureSettings, this.mDefaultCrossProfileIntentFilters, this.mIsMediaSharedWithParent, this.mIsCredentialSharableWithParent);
        }

        private boolean hasBadge() {
            return this.mIconBadge != 0;
        }

        private boolean isProfile() {
            return (this.mBaseType & 4096) != 0;
        }

        private boolean hasValidBaseType() {
            int i = this.mBaseType;
            return i == 1024 || i == 4096 || i == 2048 || i == 3072;
        }

        private boolean hasValidPropertyFlags() {
            return (this.mDefaultUserInfoPropertyFlags & 7315) == 0;
        }
    }

    public boolean isManagedProfile() {
        return UserManager.isUserTypeManagedProfile(this.mName);
    }
}
