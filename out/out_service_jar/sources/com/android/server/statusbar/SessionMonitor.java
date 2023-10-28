package com.android.server.statusbar;

import android.app.StatusBarManager;
import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.logging.InstanceId;
import com.android.internal.statusbar.ISessionListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
/* loaded from: classes2.dex */
public class SessionMonitor {
    private static final String TAG = "SessionMonitor";
    private final Context mContext;
    private final Map<Integer, Set<ISessionListener>> mSessionToListeners = new HashMap();

    public SessionMonitor(Context context) {
        this.mContext = context;
        for (Integer num : StatusBarManager.ALL_SESSIONS) {
            int session = num.intValue();
            this.mSessionToListeners.put(Integer.valueOf(session), new HashSet());
        }
    }

    public void registerSessionListener(int sessionFlags, ISessionListener listener) {
        requireListenerPermissions(sessionFlags);
        synchronized (this.mSessionToListeners) {
            for (Integer num : StatusBarManager.ALL_SESSIONS) {
                int sessionType = num.intValue();
                if ((sessionFlags & sessionType) != 0) {
                    this.mSessionToListeners.get(Integer.valueOf(sessionType)).add(listener);
                }
            }
        }
    }

    public void unregisterSessionListener(int sessionFlags, ISessionListener listener) {
        synchronized (this.mSessionToListeners) {
            for (Integer num : StatusBarManager.ALL_SESSIONS) {
                int sessionType = num.intValue();
                if ((sessionFlags & sessionType) != 0) {
                    this.mSessionToListeners.get(Integer.valueOf(sessionType)).remove(listener);
                }
            }
        }
    }

    public void onSessionStarted(int sessionType, InstanceId instanceId) {
        requireSetterPermissions(sessionType);
        if (!isValidSessionType(sessionType)) {
            Log.e(TAG, "invalid onSessionStarted sessionType=" + sessionType);
            return;
        }
        synchronized (this.mSessionToListeners) {
            for (ISessionListener listener : this.mSessionToListeners.get(Integer.valueOf(sessionType))) {
                try {
                    listener.onSessionStarted(sessionType, instanceId);
                } catch (RemoteException e) {
                    Log.e(TAG, "unable to send session start to listener=" + listener, e);
                }
            }
        }
    }

    public void onSessionEnded(int sessionType, InstanceId instanceId) {
        requireSetterPermissions(sessionType);
        if (!isValidSessionType(sessionType)) {
            Log.e(TAG, "invalid onSessionEnded sessionType=" + sessionType);
            return;
        }
        synchronized (this.mSessionToListeners) {
            for (ISessionListener listener : this.mSessionToListeners.get(Integer.valueOf(sessionType))) {
                try {
                    listener.onSessionEnded(sessionType, instanceId);
                } catch (RemoteException e) {
                    Log.e(TAG, "unable to send session end to listener=" + listener, e);
                }
            }
        }
    }

    private boolean isValidSessionType(int sessionType) {
        return StatusBarManager.ALL_SESSIONS.contains(Integer.valueOf(sessionType));
    }

    private void requireListenerPermissions(int sessionFlags) {
        if ((sessionFlags & 1) != 0) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_BIOMETRIC", "StatusBarManagerService.SessionMonitor");
        }
        if ((sessionFlags & 2) != 0) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_BIOMETRIC", "StatusBarManagerService.SessionMonitor");
        }
    }

    private void requireSetterPermissions(int sessionFlags) {
        if ((sessionFlags & 1) != 0) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.CONTROL_KEYGUARD", "StatusBarManagerService.SessionMonitor");
        }
        if ((sessionFlags & 2) != 0) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE", "StatusBarManagerService.SessionMonitor");
        }
    }
}
