package android.net.ip;

import android.content.Context;
import android.net.DhcpResultsParcelable;
import android.net.Layer2PacketParcelable;
import android.net.LinkProperties;
import android.net.ip.IIpClientCallbacks;
import android.net.networkstack.ModuleNetworkStackClient;
import android.net.networkstack.aidl.ip.ReachabilityLossInfoParcelable;
import android.os.ConditionVariable;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;
/* loaded from: classes.dex */
public class IpClientUtil {
    public static final String DUMP_ARG = "ipclient";

    /* loaded from: classes.dex */
    public static class WaitForProvisioningCallbacks extends IpClientCallbacks {
        private final ConditionVariable mCV = new ConditionVariable();
        private LinkProperties mCallbackLinkProperties;

        public LinkProperties waitForProvisioning() {
            this.mCV.block();
            return this.mCallbackLinkProperties;
        }

        @Override // android.net.ip.IpClientCallbacks
        public void onProvisioningSuccess(LinkProperties newLp) {
            this.mCallbackLinkProperties = newLp;
            this.mCV.open();
        }

        @Override // android.net.ip.IpClientCallbacks
        public void onProvisioningFailure(LinkProperties newLp) {
            this.mCallbackLinkProperties = null;
            this.mCV.open();
        }
    }

    public static void makeIpClient(Context context, String ifName, IpClientCallbacks callback) {
        ModuleNetworkStackClient.getInstance(context).makeIpClient(ifName, new IpClientCallbacksProxy(callback));
    }

    /* loaded from: classes.dex */
    private static class IpClientCallbacksProxy extends IIpClientCallbacks.Stub {
        protected final IpClientCallbacks mCb;

        IpClientCallbacksProxy(IpClientCallbacks cb) {
            this.mCb = cb;
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void onIpClientCreated(IIpClient ipClient) {
            this.mCb.onIpClientCreated(ipClient);
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void onPreDhcpAction() {
            this.mCb.onPreDhcpAction();
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void onPostDhcpAction() {
            this.mCb.onPostDhcpAction();
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void onNewDhcpResults(DhcpResultsParcelable dhcpResults) {
            this.mCb.onNewDhcpResults(dhcpResults);
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void onProvisioningSuccess(LinkProperties newLp) {
            this.mCb.onProvisioningSuccess(new LinkProperties(newLp));
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void onProvisioningFailure(LinkProperties newLp) {
            this.mCb.onProvisioningFailure(new LinkProperties(newLp));
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void onLinkPropertiesChange(LinkProperties newLp) {
            this.mCb.onLinkPropertiesChange(new LinkProperties(newLp));
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void onReachabilityLost(String logMsg) {
            this.mCb.onReachabilityLost(logMsg);
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void onQuit() {
            this.mCb.onQuit();
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void installPacketFilter(byte[] filter) {
            this.mCb.installPacketFilter(filter);
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void startReadPacketFilter() {
            this.mCb.startReadPacketFilter();
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void setFallbackMulticastFilter(boolean enabled) {
            this.mCb.setFallbackMulticastFilter(enabled);
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void setNeighborDiscoveryOffload(boolean enable) {
            this.mCb.setNeighborDiscoveryOffload(enable);
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void onPreconnectionStart(List<Layer2PacketParcelable> packets) {
            this.mCb.onPreconnectionStart(packets);
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void onReachabilityFailure(ReachabilityLossInfoParcelable lossInfo) {
            this.mCb.onReachabilityFailure(lossInfo);
        }

        @Override // android.net.ip.IIpClientCallbacks
        public int getInterfaceVersion() {
            return 15;
        }

        @Override // android.net.ip.IIpClientCallbacks
        public String getInterfaceHash() {
            return "c7a085b65072b36dc02239895cac021b6daee530";
        }
    }

    public static void dumpIpClient(IIpClient connector, FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("IpClient logs have moved to dumpsys network_stack");
    }
}
