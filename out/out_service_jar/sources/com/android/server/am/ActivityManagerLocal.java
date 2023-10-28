package com.android.server.am;

import android.annotation.SystemApi;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.RemoteException;
@SystemApi(client = SystemApi.Client.SYSTEM_SERVER)
/* loaded from: classes.dex */
public interface ActivityManagerLocal {
    boolean bindSdkSandboxService(Intent intent, ServiceConnection serviceConnection, int i, String str, String str2, int i2) throws RemoteException;

    boolean canAllowWhileInUsePermissionInFgs(int i, int i2, String str);

    boolean canStartForegroundService(int i, int i2, String str);

    void tempAllowWhileInUsePermissionInFgs(int i, long j);
}
