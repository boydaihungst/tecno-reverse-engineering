package android.companion.virtual;

import android.annotation.SystemApi;
import android.content.ComponentName;
import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import android.util.ArraySet;
import com.android.internal.util.Preconditions;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
@SystemApi
/* loaded from: classes.dex */
public final class VirtualDeviceParams implements Parcelable {
    public static final int ACTIVITY_POLICY_DEFAULT_ALLOWED = 0;
    public static final int ACTIVITY_POLICY_DEFAULT_BLOCKED = 1;
    public static final Parcelable.Creator<VirtualDeviceParams> CREATOR = new Parcelable.Creator<VirtualDeviceParams>() { // from class: android.companion.virtual.VirtualDeviceParams.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public VirtualDeviceParams createFromParcel(Parcel in) {
            return new VirtualDeviceParams(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public VirtualDeviceParams[] newArray(int size) {
            return new VirtualDeviceParams[size];
        }
    };
    public static final int LOCK_STATE_ALWAYS_UNLOCKED = 1;
    public static final int LOCK_STATE_DEFAULT = 0;
    public static final int NAVIGATION_POLICY_DEFAULT_ALLOWED = 0;
    public static final int NAVIGATION_POLICY_DEFAULT_BLOCKED = 1;
    private final ArraySet<ComponentName> mAllowedActivities;
    private final ArraySet<ComponentName> mAllowedCrossTaskNavigations;
    private final ArraySet<ComponentName> mBlockedActivities;
    private final ArraySet<ComponentName> mBlockedCrossTaskNavigations;
    private final int mDefaultActivityPolicy;
    private final int mDefaultNavigationPolicy;
    private final int mLockState;
    private final ArraySet<UserHandle> mUsersWithMatchingAccounts;

    @Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface ActivityPolicy {
    }

    @Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface LockState {
    }

    @Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface NavigationPolicy {
    }

    private VirtualDeviceParams(int lockState, Set<UserHandle> usersWithMatchingAccounts, Set<ComponentName> allowedCrossTaskNavigations, Set<ComponentName> blockedCrossTaskNavigations, int defaultNavigationPolicy, Set<ComponentName> allowedActivities, Set<ComponentName> blockedActivities, int defaultActivityPolicy) {
        Preconditions.checkNotNull(usersWithMatchingAccounts);
        Preconditions.checkNotNull(allowedCrossTaskNavigations);
        Preconditions.checkNotNull(blockedCrossTaskNavigations);
        Preconditions.checkNotNull(allowedActivities);
        Preconditions.checkNotNull(blockedActivities);
        this.mLockState = lockState;
        this.mUsersWithMatchingAccounts = new ArraySet<>(usersWithMatchingAccounts);
        this.mAllowedCrossTaskNavigations = new ArraySet<>(allowedCrossTaskNavigations);
        this.mBlockedCrossTaskNavigations = new ArraySet<>(blockedCrossTaskNavigations);
        this.mDefaultNavigationPolicy = defaultNavigationPolicy;
        this.mAllowedActivities = new ArraySet<>(allowedActivities);
        this.mBlockedActivities = new ArraySet<>(blockedActivities);
        this.mDefaultActivityPolicy = defaultActivityPolicy;
    }

    /* JADX DEBUG: Type inference failed for r0v2. Raw type applied. Possible types: android.util.ArraySet<? extends java.lang.Object>, android.util.ArraySet<android.content.ComponentName> */
    /* JADX DEBUG: Type inference failed for r1v0. Raw type applied. Possible types: android.util.ArraySet<? extends java.lang.Object>, android.util.ArraySet<android.os.UserHandle> */
    /* JADX DEBUG: Type inference failed for r1v1. Raw type applied. Possible types: android.util.ArraySet<? extends java.lang.Object>, android.util.ArraySet<android.content.ComponentName> */
    /* JADX DEBUG: Type inference failed for r1v2. Raw type applied. Possible types: android.util.ArraySet<? extends java.lang.Object>, android.util.ArraySet<android.content.ComponentName> */
    /* JADX DEBUG: Type inference failed for r1v4. Raw type applied. Possible types: android.util.ArraySet<? extends java.lang.Object>, android.util.ArraySet<android.content.ComponentName> */
    private VirtualDeviceParams(Parcel parcel) {
        this.mLockState = parcel.readInt();
        this.mUsersWithMatchingAccounts = parcel.readArraySet(null);
        this.mAllowedCrossTaskNavigations = parcel.readArraySet(null);
        this.mBlockedCrossTaskNavigations = parcel.readArraySet(null);
        this.mDefaultNavigationPolicy = parcel.readInt();
        this.mAllowedActivities = parcel.readArraySet(null);
        this.mBlockedActivities = parcel.readArraySet(null);
        this.mDefaultActivityPolicy = parcel.readInt();
    }

    public int getLockState() {
        return this.mLockState;
    }

    public Set<UserHandle> getUsersWithMatchingAccounts() {
        return Collections.unmodifiableSet(this.mUsersWithMatchingAccounts);
    }

    public Set<ComponentName> getAllowedCrossTaskNavigations() {
        return Collections.unmodifiableSet(this.mAllowedCrossTaskNavigations);
    }

    public Set<ComponentName> getBlockedCrossTaskNavigations() {
        return Collections.unmodifiableSet(this.mBlockedCrossTaskNavigations);
    }

    public int getDefaultNavigationPolicy() {
        return this.mDefaultNavigationPolicy;
    }

    public Set<ComponentName> getAllowedActivities() {
        return Collections.unmodifiableSet(this.mAllowedActivities);
    }

    public Set<ComponentName> getBlockedActivities() {
        return Collections.unmodifiableSet(this.mBlockedActivities);
    }

    public int getDefaultActivityPolicy() {
        return this.mDefaultActivityPolicy;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mLockState);
        dest.writeArraySet(this.mUsersWithMatchingAccounts);
        dest.writeArraySet(this.mAllowedCrossTaskNavigations);
        dest.writeArraySet(this.mBlockedCrossTaskNavigations);
        dest.writeInt(this.mDefaultNavigationPolicy);
        dest.writeArraySet(this.mAllowedActivities);
        dest.writeArraySet(this.mBlockedActivities);
        dest.writeInt(this.mDefaultActivityPolicy);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof VirtualDeviceParams) {
            VirtualDeviceParams that = (VirtualDeviceParams) o;
            return this.mLockState == that.mLockState && this.mUsersWithMatchingAccounts.equals(that.mUsersWithMatchingAccounts) && Objects.equals(this.mAllowedCrossTaskNavigations, that.mAllowedCrossTaskNavigations) && Objects.equals(this.mBlockedCrossTaskNavigations, that.mBlockedCrossTaskNavigations) && this.mDefaultNavigationPolicy == that.mDefaultNavigationPolicy && Objects.equals(this.mAllowedActivities, that.mAllowedActivities) && Objects.equals(this.mBlockedActivities, that.mBlockedActivities) && this.mDefaultActivityPolicy == that.mDefaultActivityPolicy;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mLockState), this.mUsersWithMatchingAccounts, this.mAllowedCrossTaskNavigations, this.mBlockedCrossTaskNavigations, Integer.valueOf(this.mDefaultNavigationPolicy), this.mAllowedActivities, this.mBlockedActivities, Integer.valueOf(this.mDefaultActivityPolicy));
    }

    public String toString() {
        return "VirtualDeviceParams( mLockState=" + this.mLockState + " mUsersWithMatchingAccounts=" + this.mUsersWithMatchingAccounts + " mAllowedCrossTaskNavigations=" + this.mAllowedCrossTaskNavigations + " mBlockedCrossTaskNavigations=" + this.mBlockedCrossTaskNavigations + " mDefaultNavigationPolicy=" + this.mDefaultNavigationPolicy + " mAllowedActivities=" + this.mAllowedActivities + " mBlockedActivities=" + this.mBlockedActivities + " mDefaultActivityPolicy=" + this.mDefaultActivityPolicy + NavigationBarInflaterView.KEY_CODE_END;
    }

    /* loaded from: classes.dex */
    public static final class Builder {
        private int mLockState = 0;
        private Set<UserHandle> mUsersWithMatchingAccounts = Collections.emptySet();
        private Set<ComponentName> mAllowedCrossTaskNavigations = Collections.emptySet();
        private Set<ComponentName> mBlockedCrossTaskNavigations = Collections.emptySet();
        private int mDefaultNavigationPolicy = 0;
        private boolean mDefaultNavigationPolicyConfigured = false;
        private Set<ComponentName> mBlockedActivities = Collections.emptySet();
        private Set<ComponentName> mAllowedActivities = Collections.emptySet();
        private int mDefaultActivityPolicy = 0;
        private boolean mDefaultActivityPolicyConfigured = false;

        public Builder setLockState(int lockState) {
            this.mLockState = lockState;
            return this;
        }

        public Builder setUsersWithMatchingAccounts(Set<UserHandle> usersWithMatchingAccounts) {
            Preconditions.checkNotNull(usersWithMatchingAccounts);
            this.mUsersWithMatchingAccounts = usersWithMatchingAccounts;
            return this;
        }

        public Builder setAllowedCrossTaskNavigations(Set<ComponentName> allowedCrossTaskNavigations) {
            Preconditions.checkNotNull(allowedCrossTaskNavigations);
            if (this.mDefaultNavigationPolicyConfigured && this.mDefaultNavigationPolicy != 1) {
                throw new IllegalArgumentException("Allowed cross task navigation and blocked task navigation cannot  both be set.");
            }
            this.mDefaultNavigationPolicy = 1;
            this.mDefaultNavigationPolicyConfigured = true;
            this.mAllowedCrossTaskNavigations = allowedCrossTaskNavigations;
            return this;
        }

        public Builder setBlockedCrossTaskNavigations(Set<ComponentName> blockedCrossTaskNavigations) {
            Preconditions.checkNotNull(blockedCrossTaskNavigations);
            if (this.mDefaultNavigationPolicyConfigured && this.mDefaultNavigationPolicy != 0) {
                throw new IllegalArgumentException("Allowed cross task navigation and blocked task navigation cannot  be set.");
            }
            this.mDefaultNavigationPolicy = 0;
            this.mDefaultNavigationPolicyConfigured = true;
            this.mBlockedCrossTaskNavigations = blockedCrossTaskNavigations;
            return this;
        }

        public Builder setAllowedActivities(Set<ComponentName> allowedActivities) {
            Preconditions.checkNotNull(allowedActivities);
            if (this.mDefaultActivityPolicyConfigured && this.mDefaultActivityPolicy != 1) {
                throw new IllegalArgumentException("Allowed activities and Blocked activities cannot both be set.");
            }
            this.mDefaultActivityPolicy = 1;
            this.mDefaultActivityPolicyConfigured = true;
            this.mAllowedActivities = allowedActivities;
            return this;
        }

        public Builder setBlockedActivities(Set<ComponentName> blockedActivities) {
            Preconditions.checkNotNull(blockedActivities);
            if (this.mDefaultActivityPolicyConfigured && this.mDefaultActivityPolicy != 0) {
                throw new IllegalArgumentException("Allowed activities and Blocked activities cannot both be set.");
            }
            this.mDefaultActivityPolicy = 0;
            this.mDefaultActivityPolicyConfigured = true;
            this.mBlockedActivities = blockedActivities;
            return this;
        }

        public VirtualDeviceParams build() {
            return new VirtualDeviceParams(this.mLockState, this.mUsersWithMatchingAccounts, this.mAllowedCrossTaskNavigations, this.mBlockedCrossTaskNavigations, this.mDefaultNavigationPolicy, this.mAllowedActivities, this.mBlockedActivities, this.mDefaultActivityPolicy);
        }
    }
}
