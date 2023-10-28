package android.net.ip;

import android.net.NattKeepalivePacketData;
import android.net.ProxyInfo;
import android.net.TcpKeepalivePacketData;
import android.net.TcpKeepalivePacketDataParcelable;
import android.net.shared.Layer2Information;
import android.net.shared.ProvisioningConfiguration;
import android.net.util.KeepalivePacketDataUtil;
import android.os.Binder;
import android.os.RemoteException;
import android.util.Log;
/* loaded from: classes.dex */
public class IpClientManager {
    private final IIpClient mIpClient;
    private final String mTag;

    public IpClientManager(IIpClient ipClient, String tag) {
        this.mIpClient = ipClient;
        this.mTag = tag;
    }

    public IpClientManager(IIpClient ipClient) {
        this(ipClient, IpClientManager.class.getSimpleName());
    }

    private void log(String s, Throwable e) {
        Log.e(this.mTag, s, e);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [76=4] */
    public boolean completedPreDhcpAction() {
        long token = Binder.clearCallingIdentity();
        try {
            this.mIpClient.completedPreDhcpAction();
            return true;
        } catch (RemoteException e) {
            log("Error completing PreDhcpAction", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [92=4] */
    public boolean confirmConfiguration() {
        long token = Binder.clearCallingIdentity();
        try {
            this.mIpClient.confirmConfiguration();
            return true;
        } catch (RemoteException e) {
            log("Error confirming IpClient configuration", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [108=4] */
    public boolean readPacketFilterComplete(byte[] data) {
        long token = Binder.clearCallingIdentity();
        try {
            this.mIpClient.readPacketFilterComplete(data);
            return true;
        } catch (RemoteException e) {
            log("Error notifying IpClient of packet filter read", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [124=4] */
    public boolean shutdown() {
        long token = Binder.clearCallingIdentity();
        try {
            this.mIpClient.shutdown();
            return true;
        } catch (RemoteException e) {
            log("Error shutting down IpClient", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [140=4] */
    public boolean startProvisioning(ProvisioningConfiguration prov) {
        long token = Binder.clearCallingIdentity();
        try {
            this.mIpClient.startProvisioning(prov.toStableParcelable());
            return true;
        } catch (RemoteException e) {
            log("Error starting IpClient provisioning", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [158=4] */
    public boolean stop() {
        long token = Binder.clearCallingIdentity();
        try {
            this.mIpClient.stop();
            return true;
        } catch (RemoteException e) {
            log("Error stopping IpClient", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [177=4] */
    public boolean setTcpBufferSizes(String tcpBufferSizes) {
        long token = Binder.clearCallingIdentity();
        try {
            this.mIpClient.setTcpBufferSizes(tcpBufferSizes);
            return true;
        } catch (RemoteException e) {
            log("Error setting IpClient TCP buffer sizes", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [196=4] */
    public boolean setHttpProxy(ProxyInfo proxyInfo) {
        long token = Binder.clearCallingIdentity();
        try {
            this.mIpClient.setHttpProxy(proxyInfo);
            return true;
        } catch (RemoteException e) {
            log("Error setting IpClient proxy", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [213=4] */
    public boolean setMulticastFilter(boolean enabled) {
        long token = Binder.clearCallingIdentity();
        try {
            this.mIpClient.setMulticastFilter(enabled);
            return true;
        } catch (RemoteException e) {
            log("Error setting multicast filter", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public boolean addKeepalivePacketFilter(int slot, TcpKeepalivePacketData pkt) {
        return addKeepalivePacketFilter(slot, KeepalivePacketDataUtil.toStableParcelable(pkt));
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [240=4] */
    @Deprecated
    public boolean addKeepalivePacketFilter(int slot, TcpKeepalivePacketDataParcelable pkt) {
        long token = Binder.clearCallingIdentity();
        try {
            this.mIpClient.addKeepalivePacketFilter(slot, pkt);
            return true;
        } catch (RemoteException e) {
            log("Error adding Keepalive Packet Filter ", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [257=4] */
    public boolean addKeepalivePacketFilter(int slot, NattKeepalivePacketData pkt) {
        long token = Binder.clearCallingIdentity();
        try {
            this.mIpClient.addNattKeepalivePacketFilter(slot, KeepalivePacketDataUtil.toStableParcelable(pkt));
            return true;
        } catch (RemoteException e) {
            log("Error adding NAT-T Keepalive Packet Filter ", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [273=4] */
    public boolean removeKeepalivePacketFilter(int slot) {
        long token = Binder.clearCallingIdentity();
        try {
            this.mIpClient.removeKeepalivePacketFilter(slot);
            return true;
        } catch (RemoteException e) {
            log("Error removing Keepalive Packet Filter ", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [289=4] */
    public boolean setL2KeyAndGroupHint(String l2Key, String groupHint) {
        long token = Binder.clearCallingIdentity();
        try {
            this.mIpClient.setL2KeyAndGroupHint(l2Key, groupHint);
            return true;
        } catch (RemoteException e) {
            log("Failed setL2KeyAndGroupHint", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [307=4] */
    public boolean notifyPreconnectionComplete(boolean success) {
        long token = Binder.clearCallingIdentity();
        try {
            this.mIpClient.notifyPreconnectionComplete(success);
            return true;
        } catch (RemoteException e) {
            log("Error notifying IpClient Preconnection completed", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [323=4] */
    public boolean updateLayer2Information(Layer2Information info) {
        long token = Binder.clearCallingIdentity();
        try {
            this.mIpClient.updateLayer2Information(info.toStableParcelable());
            return true;
        } catch (RemoteException e) {
            log("Error updating layer2 information", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }
}
