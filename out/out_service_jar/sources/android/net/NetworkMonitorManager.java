package android.net;

import android.net.networkstack.aidl.NetworkMonitorParameters;
import android.os.Binder;
import android.os.RemoteException;
import android.util.Log;
import com.android.modules.utils.build.SdkLevel;
/* loaded from: classes.dex */
public class NetworkMonitorManager {
    private final INetworkMonitor mNetworkMonitor;
    private final String mTag;

    public NetworkMonitorManager(INetworkMonitor networkMonitorManager, String tag) {
        this.mNetworkMonitor = networkMonitorManager;
        this.mTag = tag;
    }

    public NetworkMonitorManager(INetworkMonitor networkMonitorManager) {
        this(networkMonitorManager, NetworkMonitorManager.class.getSimpleName());
    }

    private void log(String s, Throwable e) {
        Log.e(this.mTag, s, e);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [71=4] */
    public boolean start() {
        long token = Binder.clearCallingIdentity();
        try {
            this.mNetworkMonitor.start();
            return true;
        } catch (RemoteException e) {
            log("Error in start", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [84=4] */
    public boolean launchCaptivePortalApp() {
        long token = Binder.clearCallingIdentity();
        try {
            this.mNetworkMonitor.launchCaptivePortalApp();
            return true;
        } catch (RemoteException e) {
            log("Error in launchCaptivePortalApp", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [97=4] */
    public boolean notifyCaptivePortalAppFinished(int response) {
        long token = Binder.clearCallingIdentity();
        try {
            this.mNetworkMonitor.notifyCaptivePortalAppFinished(response);
            return true;
        } catch (RemoteException e) {
            log("Error in notifyCaptivePortalAppFinished", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [110=4] */
    public boolean setAcceptPartialConnectivity() {
        long token = Binder.clearCallingIdentity();
        try {
            this.mNetworkMonitor.setAcceptPartialConnectivity();
            return true;
        } catch (RemoteException e) {
            log("Error in setAcceptPartialConnectivity", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [123=4] */
    public boolean forceReevaluation(int uid) {
        long token = Binder.clearCallingIdentity();
        try {
            this.mNetworkMonitor.forceReevaluation(uid);
            return true;
        } catch (RemoteException e) {
            log("Error in forceReevaluation", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [136=4] */
    public boolean notifyPrivateDnsChanged(PrivateDnsConfigParcel config) {
        long token = Binder.clearCallingIdentity();
        try {
            this.mNetworkMonitor.notifyPrivateDnsChanged(config);
            return true;
        } catch (RemoteException e) {
            log("Error in notifyPrivateDnsChanged", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [149=4] */
    public boolean notifyDnsResponse(int returnCode) {
        long token = Binder.clearCallingIdentity();
        try {
            this.mNetworkMonitor.notifyDnsResponse(returnCode);
            return true;
        } catch (RemoteException e) {
            log("Error in notifyDnsResponse", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [167=4] */
    public boolean notifyNetworkConnected(NetworkMonitorParameters params) {
        long token = Binder.clearCallingIdentity();
        try {
            if (SdkLevel.isAtLeastT()) {
                this.mNetworkMonitor.notifyNetworkConnectedParcel(params);
            } else {
                this.mNetworkMonitor.notifyNetworkConnected(params.linkProperties, params.networkCapabilities);
            }
            return true;
        } catch (RemoteException e) {
            log("Error in notifyNetworkConnected", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [180=4] */
    public boolean notifyNetworkDisconnected() {
        long token = Binder.clearCallingIdentity();
        try {
            this.mNetworkMonitor.notifyNetworkDisconnected();
            return true;
        } catch (RemoteException e) {
            log("Error in notifyNetworkDisconnected", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [193=4] */
    public boolean notifyLinkPropertiesChanged(LinkProperties lp) {
        long token = Binder.clearCallingIdentity();
        try {
            this.mNetworkMonitor.notifyLinkPropertiesChanged(lp);
            return true;
        } catch (RemoteException e) {
            log("Error in notifyLinkPropertiesChanged", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [206=4] */
    public boolean notifyNetworkCapabilitiesChanged(NetworkCapabilities nc) {
        long token = Binder.clearCallingIdentity();
        try {
            this.mNetworkMonitor.notifyNetworkCapabilitiesChanged(nc);
            return true;
        } catch (RemoteException e) {
            log("Error in notifyNetworkCapabilitiesChanged", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }
}
