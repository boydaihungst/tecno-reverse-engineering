package com.android.server.media;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.media.IMediaResourceMonitor;
import android.os.Binder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import com.android.server.SystemService;
import java.util.List;
/* loaded from: classes2.dex */
public class MediaResourceMonitorService extends SystemService {
    private static final String SERVICE_NAME = "media_resource_monitor";
    private final MediaResourceMonitorImpl mMediaResourceMonitorImpl;
    private static final String TAG = "MediaResourceMonitor";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    public MediaResourceMonitorService(Context context) {
        super(context);
        this.mMediaResourceMonitorImpl = new MediaResourceMonitorImpl();
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService(SERVICE_NAME, this.mMediaResourceMonitorImpl);
    }

    /* loaded from: classes2.dex */
    class MediaResourceMonitorImpl extends IMediaResourceMonitor.Stub {
        MediaResourceMonitorImpl() {
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [80=4] */
        public void notifyResourceGranted(int pid, int type) throws RemoteException {
            if (MediaResourceMonitorService.DEBUG) {
                Log.d(MediaResourceMonitorService.TAG, "notifyResourceGranted(pid=" + pid + ", type=" + type + ")");
            }
            long identity = Binder.clearCallingIdentity();
            try {
                String[] pkgNames = getPackageNamesFromPid(pid);
                if (pkgNames == null) {
                    return;
                }
                UserManager manager = (UserManager) MediaResourceMonitorService.this.getContext().createContextAsUser(UserHandle.of(ActivityManager.getCurrentUser()), 0).getSystemService(UserManager.class);
                List<UserHandle> enabledProfiles = manager.getEnabledProfiles();
                if (enabledProfiles.isEmpty()) {
                    return;
                }
                Intent intent = new Intent("android.intent.action.MEDIA_RESOURCE_GRANTED");
                intent.putExtra("android.intent.extra.PACKAGES", pkgNames);
                intent.putExtra("android.intent.extra.MEDIA_RESOURCE_TYPE", type);
                for (UserHandle userHandle : enabledProfiles) {
                    MediaResourceMonitorService.this.getContext().sendBroadcastAsUser(intent, userHandle, "android.permission.RECEIVE_MEDIA_RESOURCE_USAGE");
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        private String[] getPackageNamesFromPid(int pid) {
            ActivityManager manager = (ActivityManager) MediaResourceMonitorService.this.getContext().getSystemService(ActivityManager.class);
            for (ActivityManager.RunningAppProcessInfo proc : manager.getRunningAppProcesses()) {
                if (proc.pid == pid) {
                    return proc.pkgList;
                }
            }
            return null;
        }
    }
}
