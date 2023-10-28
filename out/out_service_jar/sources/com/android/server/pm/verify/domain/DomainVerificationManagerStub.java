package com.android.server.pm.verify.domain;

import android.content.pm.PackageManager;
import android.content.pm.verify.domain.DomainOwner;
import android.content.pm.verify.domain.DomainSet;
import android.content.pm.verify.domain.DomainVerificationInfo;
import android.content.pm.verify.domain.DomainVerificationUserState;
import android.content.pm.verify.domain.IDomainVerificationManager;
import android.os.ServiceSpecificException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
/* loaded from: classes2.dex */
public class DomainVerificationManagerStub extends IDomainVerificationManager.Stub {
    private final DomainVerificationService mService;

    public DomainVerificationManagerStub(DomainVerificationService service) {
        this.mService = service;
    }

    public List<String> queryValidVerificationPackageNames() {
        try {
            return this.mService.queryValidVerificationPackageNames();
        } catch (Exception e) {
            throw rethrow(e);
        }
    }

    public DomainVerificationInfo getDomainVerificationInfo(String packageName) {
        try {
            return this.mService.getDomainVerificationInfo(packageName);
        } catch (Exception e) {
            throw rethrow(e);
        }
    }

    public int setDomainVerificationStatus(String domainSetId, DomainSet domainSet, int state) {
        try {
            return this.mService.setDomainVerificationStatus(UUID.fromString(domainSetId), domainSet.getDomains(), state);
        } catch (Exception e) {
            throw rethrow(e);
        }
    }

    public void setDomainVerificationLinkHandlingAllowed(String packageName, boolean allowed, int userId) {
        try {
            this.mService.setDomainVerificationLinkHandlingAllowed(packageName, allowed, userId);
        } catch (Exception e) {
            throw rethrow(e);
        }
    }

    public int setDomainVerificationUserSelection(String domainSetId, DomainSet domainSet, boolean enabled, int userId) {
        try {
            return this.mService.setDomainVerificationUserSelection(UUID.fromString(domainSetId), domainSet.getDomains(), enabled, userId);
        } catch (Exception e) {
            throw rethrow(e);
        }
    }

    public DomainVerificationUserState getDomainVerificationUserState(String packageName, int userId) {
        try {
            return this.mService.getDomainVerificationUserState(packageName, userId);
        } catch (Exception e) {
            throw rethrow(e);
        }
    }

    public List<DomainOwner> getOwnersForDomain(String domain, int userId) {
        try {
            Objects.requireNonNull(domain);
            return this.mService.getOwnersForDomain(domain, userId);
        } catch (Exception e) {
            throw rethrow(e);
        }
    }

    private RuntimeException rethrow(Exception exception) throws RuntimeException {
        if (exception instanceof PackageManager.NameNotFoundException) {
            return new ServiceSpecificException(1);
        }
        if (exception instanceof RuntimeException) {
            return (RuntimeException) exception;
        }
        return new RuntimeException(exception);
    }
}
