package com.android.server.restrictions;

import android.app.AppGlobals;
import android.app.admin.IDevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.IRestrictionsManager;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IUserManager;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.UserHandle;
import com.android.internal.util.ArrayUtils;
import com.android.server.SystemService;
/* loaded from: classes2.dex */
public final class RestrictionsManagerService extends SystemService {
    static final boolean DEBUG = false;
    static final String LOG_TAG = "RestrictionsManagerService";
    private final RestrictionsManagerImpl mRestrictionsManagerImpl;

    public RestrictionsManagerService(Context context) {
        super(context);
        this.mRestrictionsManagerImpl = new RestrictionsManagerImpl(context);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("restrictions", this.mRestrictionsManagerImpl);
    }

    /* loaded from: classes2.dex */
    class RestrictionsManagerImpl extends IRestrictionsManager.Stub {
        final Context mContext;
        private final IDevicePolicyManager mDpm;
        private final IUserManager mUm;

        public RestrictionsManagerImpl(Context context) {
            this.mContext = context;
            this.mUm = RestrictionsManagerService.this.getBinderService("user");
            this.mDpm = RestrictionsManagerService.this.getBinderService("device_policy");
        }

        public Bundle getApplicationRestrictions(String packageName) throws RemoteException {
            return this.mUm.getApplicationRestrictions(packageName);
        }

        public boolean hasRestrictionsProvider() throws RemoteException {
            int userHandle = UserHandle.getCallingUserId();
            if (this.mDpm != null) {
                long ident = Binder.clearCallingIdentity();
                try {
                    return this.mDpm.getRestrictionsProvider(userHandle) != null;
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
            return false;
        }

        public void requestPermission(String packageName, String requestType, String requestId, PersistableBundle requestData) throws RemoteException {
            int callingUid = Binder.getCallingUid();
            int userHandle = UserHandle.getUserId(callingUid);
            if (this.mDpm != null) {
                long ident = Binder.clearCallingIdentity();
                try {
                    ComponentName restrictionsProvider = this.mDpm.getRestrictionsProvider(userHandle);
                    if (restrictionsProvider == null) {
                        throw new IllegalStateException("Cannot request permission without a restrictions provider registered");
                    }
                    enforceCallerMatchesPackage(callingUid, packageName, "Package name does not match caller ");
                    Intent intent = new Intent("android.content.action.REQUEST_PERMISSION");
                    intent.setComponent(restrictionsProvider);
                    intent.putExtra("android.content.extra.PACKAGE_NAME", packageName);
                    intent.putExtra("android.content.extra.REQUEST_TYPE", requestType);
                    intent.putExtra("android.content.extra.REQUEST_ID", requestId);
                    intent.putExtra("android.content.extra.REQUEST_BUNDLE", requestData);
                    this.mContext.sendBroadcastAsUser(intent, new UserHandle(userHandle));
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        }

        public Intent createLocalApprovalIntent() throws RemoteException {
            int userHandle = UserHandle.getCallingUserId();
            if (this.mDpm != null) {
                long ident = Binder.clearCallingIdentity();
                try {
                    ComponentName restrictionsProvider = this.mDpm.getRestrictionsProvider(userHandle);
                    if (restrictionsProvider == null) {
                        throw new IllegalStateException("Cannot request permission without a restrictions provider registered");
                    }
                    String providerPackageName = restrictionsProvider.getPackageName();
                    Intent intent = new Intent("android.content.action.REQUEST_LOCAL_APPROVAL");
                    intent.setPackage(providerPackageName);
                    ResolveInfo ri = AppGlobals.getPackageManager().resolveIntent(intent, (String) null, 0L, userHandle);
                    if (ri != null && ri.activityInfo != null && ri.activityInfo.exported) {
                        intent.setComponent(new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name));
                        return intent;
                    }
                    return null;
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
            return null;
        }

        public void notifyPermissionResponse(String packageName, PersistableBundle response) throws RemoteException {
            int callingUid = Binder.getCallingUid();
            int userHandle = UserHandle.getUserId(callingUid);
            if (this.mDpm != null) {
                long ident = Binder.clearCallingIdentity();
                try {
                    ComponentName permProvider = this.mDpm.getRestrictionsProvider(userHandle);
                    if (permProvider == null) {
                        throw new SecurityException("No restrictions provider registered for user");
                    }
                    enforceCallerMatchesPackage(callingUid, permProvider.getPackageName(), "Restrictions provider does not match caller ");
                    Intent responseIntent = new Intent("android.content.action.PERMISSION_RESPONSE_RECEIVED");
                    responseIntent.setPackage(packageName);
                    responseIntent.putExtra("android.content.extra.RESPONSE_BUNDLE", response);
                    this.mContext.sendBroadcastAsUser(responseIntent, new UserHandle(userHandle));
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        }

        private void enforceCallerMatchesPackage(int callingUid, String packageName, String message) {
            try {
                String[] pkgs = AppGlobals.getPackageManager().getPackagesForUid(callingUid);
                if (pkgs != null && !ArrayUtils.contains(pkgs, packageName)) {
                    throw new SecurityException(message + callingUid);
                }
            } catch (RemoteException e) {
            }
        }
    }
}
