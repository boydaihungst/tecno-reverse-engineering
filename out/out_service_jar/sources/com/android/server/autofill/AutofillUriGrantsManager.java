package com.android.server.autofill;

import android.app.IUriGrantsManager;
import android.app.UriGrantsManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.wm.ActivityTaskManagerInternal;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class AutofillUriGrantsManager {
    private static final String TAG = AutofillUriGrantsManager.class.getSimpleName();
    private final int mSourceUid;
    private final int mSourceUserId;
    private final ActivityTaskManagerInternal mActivityTaskMgrInternal = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
    private final IUriGrantsManager mUgm = UriGrantsManager.getService();

    /* JADX INFO: Access modifiers changed from: package-private */
    public AutofillUriGrantsManager(int serviceUid) {
        this.mSourceUid = serviceUid;
        this.mSourceUserId = UserHandle.getUserId(serviceUid);
    }

    public void grantUriPermissions(ComponentName targetActivity, IBinder targetActivityToken, int targetUserId, ClipData clip) {
        String targetPkg = targetActivity.getPackageName();
        IBinder permissionOwner = this.mActivityTaskMgrInternal.getUriPermissionOwnerForActivity(targetActivityToken);
        if (permissionOwner == null) {
            Slog.w(TAG, "Can't grant URI permissions, because the target activity token is invalid: clip=" + clip + ", targetActivity=" + targetActivity + ", targetUserId=" + targetUserId + ", targetActivityToken=" + Integer.toHexString(targetActivityToken.hashCode()));
            return;
        }
        for (int i = 0; i < clip.getItemCount(); i++) {
            ClipData.Item item = clip.getItemAt(i);
            Uri uri = item.getUri();
            if (uri != null && ActivityTaskManagerInternal.ASSIST_KEY_CONTENT.equals(uri.getScheme())) {
                grantUriPermissions(uri, targetPkg, targetUserId, permissionOwner);
            }
        }
    }

    private void grantUriPermissions(Uri uri, String targetPkg, int targetUserId, IBinder permissionOwner) {
        String str;
        String str2;
        String str3;
        String str4;
        int sourceUserId = ContentProvider.getUserIdFromUri(uri, this.mSourceUserId);
        if (Helper.sVerbose) {
            Slog.v(TAG, "Granting URI permissions: uri=" + uri + ", sourceUid=" + this.mSourceUid + ", sourceUserId=" + sourceUserId + ", targetPkg=" + targetPkg + ", targetUserId=" + targetUserId + ", permissionOwner=" + Integer.toHexString(permissionOwner.hashCode()));
        }
        Uri uriWithoutUserId = ContentProvider.getUriWithoutUserId(uri);
        long ident = Binder.clearCallingIdentity();
        try {
            try {
                str3 = ", permissionOwner=";
                str4 = ", sourceUid=";
                str = ", sourceUserId=";
                str2 = ", targetPkg=";
            } catch (RemoteException e) {
                e = e;
                str = ", sourceUserId=";
                str2 = ", targetPkg=";
                str3 = ", permissionOwner=";
                str4 = ", sourceUid=";
            }
            try {
                this.mUgm.grantUriPermissionFromOwner(permissionOwner, this.mSourceUid, targetPkg, uriWithoutUserId, 1, sourceUserId, targetUserId);
                Binder.restoreCallingIdentity(ident);
            } catch (RemoteException e2) {
                e = e2;
                try {
                    Slog.e(TAG, "Granting URI permissions failed: uri=" + uri + str4 + this.mSourceUid + str + sourceUserId + str2 + targetPkg + ", targetUserId=" + targetUserId + str3 + Integer.toHexString(permissionOwner.hashCode()), e);
                    Binder.restoreCallingIdentity(ident);
                } catch (Throwable th) {
                    th = th;
                    Binder.restoreCallingIdentity(ident);
                    throw th;
                }
            }
        } catch (Throwable th2) {
            th = th2;
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }
}
