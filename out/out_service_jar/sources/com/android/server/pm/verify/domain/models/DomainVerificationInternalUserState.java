package com.android.server.pm.verify.domain.models;

import android.annotation.NonNull;
import android.annotation.UserIdInt;
import android.util.ArraySet;
import com.android.internal.util.AnnotationValidations;
import java.util.Objects;
import java.util.Set;
/* loaded from: classes2.dex */
public class DomainVerificationInternalUserState {
    private final ArraySet<String> mEnabledHosts;
    private boolean mLinkHandlingAllowed;
    private final int mUserId;

    public DomainVerificationInternalUserState(int userId) {
        this.mLinkHandlingAllowed = true;
        this.mUserId = userId;
        this.mEnabledHosts = new ArraySet<>();
    }

    public DomainVerificationInternalUserState addHosts(ArraySet<String> newHosts) {
        this.mEnabledHosts.addAll((ArraySet<? extends String>) newHosts);
        return this;
    }

    public DomainVerificationInternalUserState addHosts(Set<String> newHosts) {
        this.mEnabledHosts.addAll(newHosts);
        return this;
    }

    public DomainVerificationInternalUserState removeHost(String host) {
        this.mEnabledHosts.remove(host);
        return this;
    }

    public DomainVerificationInternalUserState removeHosts(ArraySet<String> newHosts) {
        this.mEnabledHosts.removeAll((ArraySet<? extends String>) newHosts);
        return this;
    }

    public DomainVerificationInternalUserState removeHosts(Set<String> newHosts) {
        this.mEnabledHosts.removeAll(newHosts);
        return this;
    }

    public DomainVerificationInternalUserState retainHosts(Set<String> hosts) {
        this.mEnabledHosts.retainAll(hosts);
        return this;
    }

    public DomainVerificationInternalUserState(int userId, ArraySet<String> enabledHosts, boolean linkHandlingAllowed) {
        this.mLinkHandlingAllowed = true;
        this.mUserId = userId;
        AnnotationValidations.validate(UserIdInt.class, (UserIdInt) null, userId);
        this.mEnabledHosts = enabledHosts;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, enabledHosts);
        this.mLinkHandlingAllowed = linkHandlingAllowed;
    }

    public int getUserId() {
        return this.mUserId;
    }

    public ArraySet<String> getEnabledHosts() {
        return this.mEnabledHosts;
    }

    public boolean isLinkHandlingAllowed() {
        return this.mLinkHandlingAllowed;
    }

    public DomainVerificationInternalUserState setLinkHandlingAllowed(boolean value) {
        this.mLinkHandlingAllowed = value;
        return this;
    }

    public String toString() {
        return "DomainVerificationInternalUserState { userId = " + this.mUserId + ", enabledHosts = " + this.mEnabledHosts + ", linkHandlingAllowed = " + this.mLinkHandlingAllowed + " }";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DomainVerificationInternalUserState that = (DomainVerificationInternalUserState) o;
        if (this.mUserId == that.mUserId && Objects.equals(this.mEnabledHosts, that.mEnabledHosts) && this.mLinkHandlingAllowed == that.mLinkHandlingAllowed) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int _hash = (1 * 31) + this.mUserId;
        return (((_hash * 31) + Objects.hashCode(this.mEnabledHosts)) * 31) + Boolean.hashCode(this.mLinkHandlingAllowed);
    }

    @Deprecated
    private void __metadata() {
    }
}
