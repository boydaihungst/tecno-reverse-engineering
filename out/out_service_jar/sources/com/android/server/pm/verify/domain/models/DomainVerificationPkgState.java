package com.android.server.pm.verify.domain.models;

import android.annotation.NonNull;
import android.util.ArrayMap;
import android.util.SparseArray;
import com.android.internal.util.AnnotationValidations;
import java.util.Objects;
import java.util.UUID;
/* loaded from: classes2.dex */
public class DomainVerificationPkgState {
    private final String mBackupSignatureHash;
    private final boolean mHasAutoVerifyDomains;
    private UUID mId;
    private final String mPackageName;
    private final ArrayMap<String, Integer> mStateMap;
    private final SparseArray<DomainVerificationInternalUserState> mUserStates;

    public DomainVerificationPkgState(String packageName, UUID id, boolean hasAutoVerifyDomains) {
        this(packageName, id, hasAutoVerifyDomains, new ArrayMap(0), new SparseArray(0), null);
    }

    public DomainVerificationPkgState(DomainVerificationPkgState pkgState, UUID id, boolean hasAutoVerifyDomains) {
        this(pkgState.getPackageName(), id, hasAutoVerifyDomains, pkgState.getStateMap(), pkgState.getUserStates(), null);
    }

    public DomainVerificationInternalUserState getUserState(int userId) {
        return this.mUserStates.get(userId);
    }

    public DomainVerificationInternalUserState getOrCreateUserState(int userId) {
        DomainVerificationInternalUserState userState = this.mUserStates.get(userId);
        if (userState == null) {
            DomainVerificationInternalUserState userState2 = new DomainVerificationInternalUserState(userId);
            this.mUserStates.put(userId, userState2);
            return userState2;
        }
        return userState;
    }

    public void removeUser(int userId) {
        this.mUserStates.remove(userId);
    }

    public void removeAllUsers() {
        this.mUserStates.clear();
    }

    private int userStatesHashCode() {
        return this.mUserStates.contentHashCode();
    }

    private boolean userStatesEquals(SparseArray<DomainVerificationInternalUserState> other) {
        return this.mUserStates.contentEquals(other);
    }

    public DomainVerificationPkgState(String packageName, UUID id, boolean hasAutoVerifyDomains, ArrayMap<String, Integer> stateMap, SparseArray<DomainVerificationInternalUserState> userStates, String backupSignatureHash) {
        this.mPackageName = packageName;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, packageName);
        this.mId = id;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, id);
        this.mHasAutoVerifyDomains = hasAutoVerifyDomains;
        this.mStateMap = stateMap;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, stateMap);
        this.mUserStates = userStates;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, userStates);
        this.mBackupSignatureHash = backupSignatureHash;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public UUID getId() {
        return this.mId;
    }

    public boolean isHasAutoVerifyDomains() {
        return this.mHasAutoVerifyDomains;
    }

    public ArrayMap<String, Integer> getStateMap() {
        return this.mStateMap;
    }

    public SparseArray<DomainVerificationInternalUserState> getUserStates() {
        return this.mUserStates;
    }

    public String getBackupSignatureHash() {
        return this.mBackupSignatureHash;
    }

    public String toString() {
        return "DomainVerificationPkgState { packageName = " + this.mPackageName + ", id = " + this.mId + ", hasAutoVerifyDomains = " + this.mHasAutoVerifyDomains + ", stateMap = " + this.mStateMap + ", userStates = " + this.mUserStates + ", backupSignatureHash = " + this.mBackupSignatureHash + " }";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DomainVerificationPkgState that = (DomainVerificationPkgState) o;
        if (Objects.equals(this.mPackageName, that.mPackageName) && Objects.equals(this.mId, that.mId) && this.mHasAutoVerifyDomains == that.mHasAutoVerifyDomains && Objects.equals(this.mStateMap, that.mStateMap) && userStatesEquals(that.mUserStates) && Objects.equals(this.mBackupSignatureHash, that.mBackupSignatureHash)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int _hash = (1 * 31) + Objects.hashCode(this.mPackageName);
        return (((((((((_hash * 31) + Objects.hashCode(this.mId)) * 31) + Boolean.hashCode(this.mHasAutoVerifyDomains)) * 31) + Objects.hashCode(this.mStateMap)) * 31) + userStatesHashCode()) * 31) + Objects.hashCode(this.mBackupSignatureHash);
    }

    @Deprecated
    private void __metadata() {
    }
}
