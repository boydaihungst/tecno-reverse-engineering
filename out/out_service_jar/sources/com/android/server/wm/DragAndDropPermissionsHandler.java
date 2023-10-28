package com.android.server.wm;

import android.app.UriGrantsManager;
import android.content.ClipData;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.view.IDragAndDropPermissions;
import com.android.server.LocalServices;
import com.android.server.uri.UriGrantsManagerInternal;
import java.util.ArrayList;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class DragAndDropPermissionsHandler extends IDragAndDropPermissions.Stub {
    private static final boolean DEBUG = false;
    private static final String TAG = "DragAndDrop";
    private IBinder mActivityToken;
    private final WindowManagerGlobalLock mGlobalLock;
    private final int mMode;
    private IBinder mPermissionOwnerToken;
    private final int mSourceUid;
    private final int mSourceUserId;
    private boolean mStopFinalize;
    private final String mTargetPackage;
    private final int mTargetUserId;
    private final ArrayList<Uri> mUris;

    /* JADX INFO: Access modifiers changed from: package-private */
    public DragAndDropPermissionsHandler(WindowManagerGlobalLock lock, ClipData clipData, int sourceUid, String targetPackage, int mode, int sourceUserId, int targetUserId) {
        ArrayList<Uri> arrayList = new ArrayList<>();
        this.mUris = arrayList;
        this.mActivityToken = null;
        this.mPermissionOwnerToken = null;
        this.mGlobalLock = lock;
        this.mSourceUid = sourceUid;
        this.mTargetPackage = targetPackage;
        this.mMode = mode;
        this.mSourceUserId = sourceUserId;
        this.mTargetUserId = targetUserId;
        clipData.collectUris(arrayList);
    }

    public void take(IBinder activityToken) throws RemoteException {
        if (this.mActivityToken != null || this.mPermissionOwnerToken != null) {
            return;
        }
        this.mActivityToken = activityToken;
        IBinder permissionOwner = getUriPermissionOwnerForActivity(activityToken);
        doTake(permissionOwner);
    }

    private void doTake(IBinder permissionOwner) throws RemoteException {
        long origId = Binder.clearCallingIdentity();
        for (int i = 0; i < this.mUris.size(); i++) {
            try {
                UriGrantsManager.getService().grantUriPermissionFromOwner(permissionOwner, this.mSourceUid, this.mTargetPackage, this.mUris.get(i), this.mMode, this.mSourceUserId, this.mTargetUserId);
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }
    }

    public void takeTransient() throws RemoteException {
        if (this.mActivityToken != null || this.mPermissionOwnerToken != null) {
            return;
        }
        IBinder newUriPermissionOwner = ((UriGrantsManagerInternal) LocalServices.getService(UriGrantsManagerInternal.class)).newUriPermissionOwner("drop");
        this.mPermissionOwnerToken = newUriPermissionOwner;
        doTake(newUriPermissionOwner);
    }

    public void release() throws RemoteException {
        IBinder permissionOwner;
        IBinder iBinder = this.mActivityToken;
        if (iBinder == null && this.mPermissionOwnerToken == null) {
            return;
        }
        if (iBinder != null) {
            try {
                permissionOwner = getUriPermissionOwnerForActivity(iBinder);
            } catch (Exception e) {
                return;
            } finally {
                this.mActivityToken = null;
            }
        } else {
            permissionOwner = this.mPermissionOwnerToken;
            this.mPermissionOwnerToken = null;
        }
        UriGrantsManagerInternal ugm = (UriGrantsManagerInternal) LocalServices.getService(UriGrantsManagerInternal.class);
        for (int i = 0; i < this.mUris.size(); i++) {
            ugm.revokeUriPermissionFromOwner(permissionOwner, this.mUris.get(i), this.mMode, this.mSourceUserId);
        }
    }

    private IBinder getUriPermissionOwnerForActivity(IBinder activityToken) {
        Binder externalToken;
        ActivityTaskManagerService.enforceNotIsolatedCaller("getUriPermissionOwnerForActivity");
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInRootTaskLocked(activityToken);
                if (r == null) {
                    throw new IllegalArgumentException("Activity does not exist; token=" + activityToken);
                }
                externalToken = r.getUriPermissionsLocked().getExternalToken();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return externalToken;
    }

    protected void finalize() throws Throwable {
        IBinder iBinder;
        if (this.mActivityToken != null || (iBinder = this.mPermissionOwnerToken) == null) {
            return;
        }
        if (this.mStopFinalize && iBinder != null) {
            Log.w(TAG, "force stop finalize");
        } else {
            release();
        }
    }

    public void setFinalize(boolean stopFinalize) {
        Log.v(TAG, "force setFinalize " + stopFinalize);
        this.mStopFinalize = stopFinalize;
    }
}
