package com.android.server.notification.toast;

import android.app.ITransientNotification;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import com.android.internal.util.Preconditions;
import com.android.server.notification.NotificationManagerService;
import com.android.server.slice.SliceClientPermissions;
/* loaded from: classes2.dex */
public class CustomToastRecord extends ToastRecord {
    private static final String TAG = "NotificationService";
    public final ITransientNotification callback;

    public CustomToastRecord(NotificationManagerService notificationManager, int uid, int pid, String packageName, boolean isSystemToast, IBinder token, ITransientNotification callback, int duration, Binder windowToken, int displayId) {
        super(notificationManager, uid, pid, packageName, isSystemToast, token, duration, windowToken, displayId);
        this.callback = (ITransientNotification) Preconditions.checkNotNull(callback);
    }

    @Override // com.android.server.notification.toast.ToastRecord
    public boolean show() {
        if (NotificationManagerService.DBG) {
            Slog.d("NotificationService", "Show pkg=" + this.pkg + " callback=" + this.callback);
        }
        try {
            this.callback.show(this.windowToken);
            return true;
        } catch (RemoteException e) {
            Slog.w("NotificationService", "Object died trying to show custom toast " + this.token + " in package " + this.pkg);
            this.mNotificationManager.keepProcessAliveForToastIfNeeded(this.pid);
            return false;
        }
    }

    @Override // com.android.server.notification.toast.ToastRecord
    public void hide() {
        try {
            this.callback.hide();
        } catch (RemoteException e) {
            Slog.w("NotificationService", "Object died trying to hide custom toast " + this.token + " in package " + this.pkg);
        }
    }

    @Override // com.android.server.notification.toast.ToastRecord
    public boolean keepProcessAlive() {
        return true;
    }

    @Override // com.android.server.notification.toast.ToastRecord
    public boolean isAppRendered() {
        return true;
    }

    public String toString() {
        return "CustomToastRecord{" + Integer.toHexString(System.identityHashCode(this)) + " " + this.pid + ":" + this.pkg + SliceClientPermissions.SliceAuthority.DELIMITER + UserHandle.formatUid(this.uid) + " isSystemToast=" + this.isSystemToast + " token=" + this.token + " callback=" + this.callback + " duration=" + getDuration() + "}";
    }
}
