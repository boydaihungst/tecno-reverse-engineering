package android.net.ip;

import android.net.DhcpResultsParcelable;
import android.net.Layer2PacketParcelable;
import android.net.LinkProperties;
import android.net.networkstack.aidl.ip.ReachabilityLossInfoParcelable;
import java.util.List;
/* loaded from: classes.dex */
public class IpClientCallbacks {
    public void onIpClientCreated(IIpClient ipClient) {
    }

    public void onPreDhcpAction() {
    }

    public void onPostDhcpAction() {
    }

    public void onNewDhcpResults(DhcpResultsParcelable dhcpResults) {
    }

    public void onProvisioningSuccess(LinkProperties newLp) {
    }

    public void onProvisioningFailure(LinkProperties newLp) {
    }

    public void onLinkPropertiesChange(LinkProperties newLp) {
    }

    public void onReachabilityLost(String logMsg) {
    }

    public void onQuit() {
    }

    public void installPacketFilter(byte[] filter) {
    }

    public void startReadPacketFilter() {
    }

    public void setFallbackMulticastFilter(boolean enabled) {
    }

    public void setNeighborDiscoveryOffload(boolean enable) {
    }

    public void onPreconnectionStart(List<Layer2PacketParcelable> packets) {
    }

    public void onReachabilityFailure(ReachabilityLossInfoParcelable lossInfo) {
        onReachabilityLost(lossInfo.message);
    }
}
