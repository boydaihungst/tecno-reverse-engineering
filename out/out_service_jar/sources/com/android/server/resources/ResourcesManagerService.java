package com.android.server.resources;

import android.content.Context;
import android.content.res.IResourcesManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteCallback;
import android.os.RemoteException;
import com.android.server.SystemService;
import com.android.server.am.ActivityManagerService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
public class ResourcesManagerService extends SystemService {
    private ActivityManagerService mActivityManagerService;
    private final IBinder mService;

    public ResourcesManagerService(Context context) {
        super(context);
        IResourcesManager.Stub stub = new IResourcesManager.Stub() { // from class: com.android.server.resources.ResourcesManagerService.1
            public boolean dumpResources(String process, ParcelFileDescriptor fd, RemoteCallback callback) throws RemoteException {
                int callingUid = Binder.getCallingUid();
                if (callingUid != 0 && callingUid != 2000) {
                    callback.sendResult((Bundle) null);
                    throw new SecurityException("dump should only be called by shell");
                }
                return ResourcesManagerService.this.mActivityManagerService.dumpResources(process, fd, callback);
            }

            protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
                try {
                    ResourcesManagerService.this.mActivityManagerService.dumpAllResources(ParcelFileDescriptor.dup(fd), pw);
                } catch (Exception e) {
                    pw.println("Exception while trying to dump all resources: " + e.getMessage());
                    e.printStackTrace(pw);
                }
            }

            /* JADX DEBUG: Multi-variable search result rejected for r6v0, resolved type: com.android.server.resources.ResourcesManagerService$1 */
            /* JADX WARN: Multi-variable type inference failed */
            public int handleShellCommand(ParcelFileDescriptor in, ParcelFileDescriptor out, ParcelFileDescriptor err, String[] args) {
                return new ResourcesManagerShellCommand(this).exec(this, in.getFileDescriptor(), out.getFileDescriptor(), err.getFileDescriptor(), args);
            }
        };
        this.mService = stub;
        publishBinderService("resources", stub);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
    }

    public void setActivityManagerService(ActivityManagerService activityManagerService) {
        this.mActivityManagerService = activityManagerService;
    }
}
