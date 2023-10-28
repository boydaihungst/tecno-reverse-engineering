package com.android.server.pm.verify.domain;

import android.content.Context;
import android.os.Binder;
import com.android.server.pm.verify.domain.proxy.DomainVerificationProxy;
/* loaded from: classes2.dex */
public class DomainVerificationEnforcer {
    private Callback mCallback;
    private final Context mContext;

    /* loaded from: classes2.dex */
    public interface Callback {
        boolean doesUserExist(int i);

        boolean filterAppAccess(String str, int i, int i2);
    }

    public DomainVerificationEnforcer(Context context) {
        this.mContext = context;
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    public void assertInternal(int callingUid) {
        switch (callingUid) {
            case 0:
            case 1000:
            case 2000:
                return;
            default:
                throw new SecurityException("Caller " + callingUid + " is not allowed to change internal state");
        }
    }

    public void assertApprovedQuerent(int callingUid, DomainVerificationProxy proxy) {
        switch (callingUid) {
            case 0:
            case 1000:
            case 2000:
                return;
            default:
                if (!proxy.isCallerVerifier(callingUid)) {
                    this.mContext.enforcePermission("android.permission.DUMP", Binder.getCallingPid(), callingUid, "Caller " + callingUid + " is not allowed to query domain verification state");
                    return;
                } else {
                    this.mContext.enforcePermission("android.permission.QUERY_ALL_PACKAGES", Binder.getCallingPid(), callingUid, "Caller " + callingUid + " does not hold android.permission.QUERY_ALL_PACKAGES");
                    return;
                }
        }
    }

    public void assertApprovedVerifier(int callingUid, DomainVerificationProxy proxy) throws SecurityException {
        boolean isAllowed;
        switch (callingUid) {
            case 0:
            case 1000:
            case 2000:
                isAllowed = true;
                break;
            default:
                int callingPid = Binder.getCallingPid();
                boolean isLegacyVerificationAgent = false;
                if (this.mContext.checkPermission("android.permission.DOMAIN_VERIFICATION_AGENT", callingPid, callingUid) != 0) {
                    isLegacyVerificationAgent = this.mContext.checkPermission("android.permission.INTENT_FILTER_VERIFICATION_AGENT", callingPid, callingUid) == 0;
                    if (!isLegacyVerificationAgent) {
                        throw new SecurityException("Caller " + callingUid + " does not hold android.permission.DOMAIN_VERIFICATION_AGENT");
                    }
                }
                if (!isLegacyVerificationAgent) {
                    this.mContext.enforcePermission("android.permission.QUERY_ALL_PACKAGES", callingPid, callingUid, "Caller " + callingUid + " does not hold android.permission.QUERY_ALL_PACKAGES");
                }
                isAllowed = proxy.isCallerVerifier(callingUid);
                break;
        }
        if (!isAllowed) {
            throw new SecurityException("Caller " + callingUid + " is not the approved domain verification agent");
        }
    }

    public boolean assertApprovedUserStateQuerent(int callingUid, int callingUserId, String packageName, int targetUserId) throws SecurityException {
        if (callingUserId != targetUserId) {
            this.mContext.enforcePermission("android.permission.INTERACT_ACROSS_USERS", Binder.getCallingPid(), callingUid, "Caller is not allowed to edit other users");
        }
        if (!this.mCallback.doesUserExist(callingUserId)) {
            throw new SecurityException("User " + callingUserId + " does not exist");
        }
        if (!this.mCallback.doesUserExist(targetUserId)) {
            throw new SecurityException("User " + targetUserId + " does not exist");
        }
        return !this.mCallback.filterAppAccess(packageName, callingUid, targetUserId);
    }

    public boolean assertApprovedUserSelector(int callingUid, int callingUserId, String packageName, int targetUserId) throws SecurityException {
        if (callingUserId != targetUserId) {
            this.mContext.enforcePermission("android.permission.INTERACT_ACROSS_USERS", Binder.getCallingPid(), callingUid, "Caller is not allowed to edit other users");
        }
        this.mContext.enforcePermission("android.permission.UPDATE_DOMAIN_VERIFICATION_USER_SELECTION", Binder.getCallingPid(), callingUid, "Caller is not allowed to edit user selections");
        if (!this.mCallback.doesUserExist(callingUserId)) {
            throw new SecurityException("User " + callingUserId + " does not exist");
        }
        if (this.mCallback.doesUserExist(targetUserId)) {
            if (packageName == null) {
                return true;
            }
            return true ^ this.mCallback.filterAppAccess(packageName, callingUid, targetUserId);
        }
        throw new SecurityException("User " + targetUserId + " does not exist");
    }

    public boolean callerIsLegacyUserSelector(int callingUid, int callingUserId, String packageName, int targetUserId) {
        this.mContext.enforcePermission("android.permission.SET_PREFERRED_APPLICATIONS", Binder.getCallingPid(), callingUid, "Caller is not allowed to edit user state");
        if (callingUserId != targetUserId && this.mContext.checkPermission("android.permission.INTERACT_ACROSS_USERS", Binder.getCallingPid(), callingUid) != 0) {
            return false;
        }
        if (!this.mCallback.doesUserExist(callingUserId)) {
            throw new SecurityException("User " + callingUserId + " does not exist");
        }
        if (!this.mCallback.doesUserExist(targetUserId)) {
            throw new SecurityException("User " + targetUserId + " does not exist");
        }
        return !this.mCallback.filterAppAccess(packageName, callingUid, targetUserId);
    }

    public boolean callerIsLegacyUserQuerent(int callingUid, int callingUserId, String packageName, int targetUserId) {
        if (callingUserId != targetUserId) {
            this.mContext.enforcePermission("android.permission.INTERACT_ACROSS_USERS_FULL", Binder.getCallingPid(), callingUid, "Caller is not allowed to edit other users");
        }
        if (!this.mCallback.doesUserExist(callingUserId)) {
            throw new SecurityException("User " + callingUserId + " does not exist");
        }
        if (!this.mCallback.doesUserExist(targetUserId)) {
            throw new SecurityException("User " + targetUserId + " does not exist");
        }
        return !this.mCallback.filterAppAccess(packageName, callingUid, targetUserId);
    }

    public void assertOwnerQuerent(int callingUid, int callingUserId, int targetUserId) {
        int callingPid = Binder.getCallingPid();
        if (callingUserId != targetUserId) {
            this.mContext.enforcePermission("android.permission.INTERACT_ACROSS_USERS", callingPid, callingUid, "Caller is not allowed to query other users");
        }
        this.mContext.enforcePermission("android.permission.QUERY_ALL_PACKAGES", callingPid, callingUid, "Caller " + callingUid + " does not hold android.permission.QUERY_ALL_PACKAGES");
        this.mContext.enforcePermission("android.permission.UPDATE_DOMAIN_VERIFICATION_USER_SELECTION", callingPid, callingUid, "Caller is not allowed to query user selections");
        if (!this.mCallback.doesUserExist(callingUserId)) {
            throw new SecurityException("User " + callingUserId + " does not exist");
        }
        if (!this.mCallback.doesUserExist(targetUserId)) {
            throw new SecurityException("User " + targetUserId + " does not exist");
        }
    }
}
