package com.android.server.pm;

import android.os.Binder;
import android.os.Message;
import android.os.UserHandle;
import com.android.server.DeviceIdleInternal;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.verify.domain.DomainVerificationManagerInternal;
import com.android.server.pm.verify.domain.proxy.DomainVerificationProxyV1;
import com.android.server.pm.verify.domain.proxy.DomainVerificationProxyV2;
/* loaded from: classes2.dex */
public final class DomainVerificationConnection implements DomainVerificationManagerInternal.Connection, DomainVerificationProxyV1.Connection, DomainVerificationProxyV2.Connection {
    final PackageManagerService mPm;
    final UserManagerInternal mUmInternal;

    /* JADX INFO: Access modifiers changed from: package-private */
    public DomainVerificationConnection(PackageManagerService pm) {
        this.mPm = pm;
        this.mUmInternal = (UserManagerInternal) pm.mInjector.getLocalService(UserManagerInternal.class);
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal.Connection
    public void scheduleWriteSettings() {
        this.mPm.scheduleWriteSettings();
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal.Connection
    public int getCallingUid() {
        return Binder.getCallingUid();
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal.Connection
    public int getCallingUserId() {
        return UserHandle.getCallingUserId();
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal.Connection, com.android.server.pm.verify.domain.proxy.DomainVerificationProxy.BaseConnection
    public void schedule(int code, Object object) {
        Message message = this.mPm.mHandler.obtainMessage(27);
        message.arg1 = code;
        message.obj = object;
        this.mPm.mHandler.sendMessage(message);
    }

    @Override // com.android.server.pm.verify.domain.proxy.DomainVerificationProxy.BaseConnection
    public long getPowerSaveTempWhitelistAppDuration() {
        return VerificationUtils.getDefaultVerificationTimeout(this.mPm.mContext);
    }

    @Override // com.android.server.pm.verify.domain.proxy.DomainVerificationProxy.BaseConnection
    public DeviceIdleInternal getDeviceIdleInternal() {
        return (DeviceIdleInternal) this.mPm.mInjector.getLocalService(DeviceIdleInternal.class);
    }

    @Override // com.android.server.pm.verify.domain.proxy.DomainVerificationProxy.BaseConnection
    public boolean isCallerPackage(int callingUid, String packageName) {
        int callingUserId = UserHandle.getUserId(callingUid);
        return callingUid == this.mPm.snapshotComputer().getPackageUid(packageName, 0L, callingUserId);
    }

    @Override // com.android.server.pm.verify.domain.proxy.DomainVerificationProxyV1.Connection
    public AndroidPackage getPackage(String packageName) {
        return this.mPm.snapshotComputer().getPackage(packageName);
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationEnforcer.Callback
    public boolean filterAppAccess(String packageName, int callingUid, int userId) {
        return this.mPm.snapshotComputer().filterAppAccess(packageName, callingUid, userId);
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal.Connection
    public int[] getAllUserIds() {
        return this.mUmInternal.getUserIds();
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationEnforcer.Callback
    public boolean doesUserExist(int userId) {
        return this.mUmInternal.exists(userId);
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal.Connection
    public Computer snapshot() {
        return this.mPm.snapshotComputer();
    }
}
