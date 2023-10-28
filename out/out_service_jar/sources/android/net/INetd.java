package android.net;

import android.net.INetdUnsolicitedEventListener;
import android.net.netd.aidl.NativeUidRangeConfig;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface INetd extends IInterface {
    public static final int CLAT_MARK = -559038041;
    public static final int CONF = 1;
    public static final int DUMMY_NET_ID = 51;
    public static final int FIREWALL_ALLOWLIST = 0;
    @Deprecated
    public static final int FIREWALL_BLACKLIST = 1;
    public static final int FIREWALL_CHAIN_DOZABLE = 1;
    public static final int FIREWALL_CHAIN_NONE = 0;
    public static final int FIREWALL_CHAIN_POWERSAVE = 3;
    public static final int FIREWALL_CHAIN_RESTRICTED = 4;
    public static final int FIREWALL_CHAIN_STANDBY = 2;
    public static final int FIREWALL_DENYLIST = 1;
    public static final int FIREWALL_RULE_ALLOW = 1;
    public static final int FIREWALL_RULE_DENY = 2;
    @Deprecated
    public static final int FIREWALL_WHITELIST = 0;
    public static final String HASH = "3943383e838f39851675e3640fcdf27b42f8c9fc";
    public static final String IF_FLAG_BROADCAST = "broadcast";
    public static final String IF_FLAG_LOOPBACK = "loopback";
    public static final String IF_FLAG_MULTICAST = "multicast";
    public static final String IF_FLAG_POINTOPOINT = "point-to-point";
    public static final String IF_FLAG_RUNNING = "running";
    public static final String IF_STATE_DOWN = "down";
    public static final String IF_STATE_UP = "up";
    public static final String IPSEC_INTERFACE_PREFIX = "ipsec";
    public static final int IPV4 = 4;
    public static final int IPV6 = 6;
    public static final int IPV6_ADDR_GEN_MODE_DEFAULT = 0;
    public static final int IPV6_ADDR_GEN_MODE_EUI64 = 0;
    public static final int IPV6_ADDR_GEN_MODE_NONE = 1;
    public static final int IPV6_ADDR_GEN_MODE_RANDOM = 3;
    public static final int IPV6_ADDR_GEN_MODE_STABLE_PRIVACY = 2;
    public static final int LOCAL_NET_ID = 99;
    public static final int NEIGH = 2;
    public static final String NEXTHOP_NONE = "";
    public static final String NEXTHOP_THROW = "throw";
    public static final String NEXTHOP_UNREACHABLE = "unreachable";
    public static final int NO_PERMISSIONS = 0;
    public static final int PENALTY_POLICY_ACCEPT = 1;
    public static final int PENALTY_POLICY_LOG = 2;
    public static final int PENALTY_POLICY_REJECT = 3;
    public static final int PERMISSION_INTERNET = 4;
    public static final int PERMISSION_NETWORK = 1;
    public static final int PERMISSION_NONE = 0;
    public static final int PERMISSION_SYSTEM = 2;
    public static final int PERMISSION_UNINSTALLED = -1;
    public static final int PERMISSION_UPDATE_DEVICE_STATS = 8;
    public static final int UNREACHABLE_NET_ID = 52;
    public static final int VERSION = 10;

    void bandwidthAddNaughtyApp(int i) throws RemoteException;

    void bandwidthAddNiceApp(int i) throws RemoteException;

    boolean bandwidthEnableDataSaver(boolean z) throws RemoteException;

    void bandwidthRemoveInterfaceAlert(String str) throws RemoteException;

    void bandwidthRemoveInterfaceQuota(String str) throws RemoteException;

    void bandwidthRemoveNaughtyApp(int i) throws RemoteException;

    void bandwidthRemoveNiceApp(int i) throws RemoteException;

    void bandwidthSetGlobalAlert(long j) throws RemoteException;

    void bandwidthSetInterfaceAlert(String str, long j) throws RemoteException;

    void bandwidthSetInterfaceQuota(String str, long j) throws RemoteException;

    @Deprecated
    String clatdStart(String str, String str2) throws RemoteException;

    @Deprecated
    void clatdStop(String str) throws RemoteException;

    void firewallAddUidInterfaceRules(String str, int[] iArr) throws RemoteException;

    void firewallEnableChildChain(int i, boolean z) throws RemoteException;

    void firewallRemoveUidInterfaceRules(int[] iArr) throws RemoteException;

    boolean firewallReplaceUidChain(String str, boolean z, int[] iArr) throws RemoteException;

    void firewallSetFirewallType(int i) throws RemoteException;

    void firewallSetInterfaceRule(String str, int i) throws RemoteException;

    void firewallSetUidRule(int i, int i2, int i3) throws RemoteException;

    MarkMaskParcel getFwmarkForNetwork(int i) throws RemoteException;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    IBinder getOemNetd() throws RemoteException;

    String getProcSysNet(int i, int i2, String str, String str2) throws RemoteException;

    void idletimerAddInterface(String str, int i, String str2) throws RemoteException;

    void idletimerRemoveInterface(String str, int i, String str2) throws RemoteException;

    void interfaceAddAddress(String str, String str2, int i) throws RemoteException;

    void interfaceClearAddrs(String str) throws RemoteException;

    void interfaceDelAddress(String str, String str2, int i) throws RemoteException;

    InterfaceConfigurationParcel interfaceGetCfg(String str) throws RemoteException;

    String[] interfaceGetList() throws RemoteException;

    void interfaceSetCfg(InterfaceConfigurationParcel interfaceConfigurationParcel) throws RemoteException;

    void interfaceSetEnableIPv6(String str, boolean z) throws RemoteException;

    void interfaceSetIPv6PrivacyExtensions(String str, boolean z) throws RemoteException;

    void interfaceSetMtu(String str, int i) throws RemoteException;

    void ipSecAddSecurityAssociation(int i, int i2, String str, String str2, int i3, int i4, int i5, int i6, String str3, byte[] bArr, int i7, String str4, byte[] bArr2, int i8, String str5, byte[] bArr3, int i9, int i10, int i11, int i12, int i13) throws RemoteException;

    void ipSecAddSecurityPolicy(int i, int i2, int i3, String str, String str2, int i4, int i5, int i6, int i7) throws RemoteException;

    void ipSecAddTunnelInterface(String str, String str2, String str3, int i, int i2, int i3) throws RemoteException;

    int ipSecAllocateSpi(int i, String str, String str2, int i2) throws RemoteException;

    void ipSecApplyTransportModeTransform(ParcelFileDescriptor parcelFileDescriptor, int i, int i2, String str, String str2, int i3) throws RemoteException;

    void ipSecDeleteSecurityAssociation(int i, String str, String str2, int i2, int i3, int i4, int i5) throws RemoteException;

    void ipSecDeleteSecurityPolicy(int i, int i2, int i3, int i4, int i5, int i6) throws RemoteException;

    void ipSecRemoveTransportModeTransform(ParcelFileDescriptor parcelFileDescriptor) throws RemoteException;

    void ipSecRemoveTunnelInterface(String str) throws RemoteException;

    void ipSecSetEncapSocketOwner(ParcelFileDescriptor parcelFileDescriptor, int i) throws RemoteException;

    void ipSecUpdateSecurityPolicy(int i, int i2, int i3, String str, String str2, int i4, int i5, int i6, int i7) throws RemoteException;

    void ipSecUpdateTunnelInterface(String str, String str2, String str3, int i, int i2, int i3) throws RemoteException;

    void ipfwdAddInterfaceForward(String str, String str2) throws RemoteException;

    void ipfwdDisableForwarding(String str) throws RemoteException;

    void ipfwdEnableForwarding(String str) throws RemoteException;

    boolean ipfwdEnabled() throws RemoteException;

    String[] ipfwdGetRequesterList() throws RemoteException;

    void ipfwdRemoveInterfaceForward(String str, String str2) throws RemoteException;

    boolean isAlive() throws RemoteException;

    void networkAddInterface(int i, String str) throws RemoteException;

    void networkAddLegacyRoute(int i, String str, String str2, String str3, int i2) throws RemoteException;

    void networkAddRoute(int i, String str, String str2, String str3) throws RemoteException;

    void networkAddRouteParcel(int i, RouteInfoParcel routeInfoParcel) throws RemoteException;

    void networkAddUidRanges(int i, UidRangeParcel[] uidRangeParcelArr) throws RemoteException;

    void networkAddUidRangesParcel(NativeUidRangeConfig nativeUidRangeConfig) throws RemoteException;

    boolean networkCanProtect(int i) throws RemoteException;

    void networkClearDefault() throws RemoteException;

    void networkClearPermissionForUser(int[] iArr) throws RemoteException;

    void networkCreate(NativeNetworkConfig nativeNetworkConfig) throws RemoteException;

    @Deprecated
    void networkCreatePhysical(int i, int i2) throws RemoteException;

    @Deprecated
    void networkCreateVpn(int i, boolean z) throws RemoteException;

    void networkDestroy(int i) throws RemoteException;

    int networkGetDefault() throws RemoteException;

    void networkRejectNonSecureVpn(boolean z, UidRangeParcel[] uidRangeParcelArr) throws RemoteException;

    void networkRemoveInterface(int i, String str) throws RemoteException;

    void networkRemoveLegacyRoute(int i, String str, String str2, String str3, int i2) throws RemoteException;

    void networkRemoveRoute(int i, String str, String str2, String str3) throws RemoteException;

    void networkRemoveRouteParcel(int i, RouteInfoParcel routeInfoParcel) throws RemoteException;

    void networkRemoveUidRanges(int i, UidRangeParcel[] uidRangeParcelArr) throws RemoteException;

    void networkRemoveUidRangesParcel(NativeUidRangeConfig nativeUidRangeConfig) throws RemoteException;

    void networkSetDefault(int i) throws RemoteException;

    void networkSetPermissionForNetwork(int i, int i2) throws RemoteException;

    void networkSetPermissionForUser(int i, int[] iArr) throws RemoteException;

    void networkSetProtectAllow(int i) throws RemoteException;

    void networkSetProtectDeny(int i) throws RemoteException;

    void networkUpdateRouteParcel(int i, RouteInfoParcel routeInfoParcel) throws RemoteException;

    void registerUnsolicitedEventListener(INetdUnsolicitedEventListener iNetdUnsolicitedEventListener) throws RemoteException;

    void setIPv6AddrGenMode(String str, int i) throws RemoteException;

    void setProcSysNet(int i, int i2, String str, String str2, String str3) throws RemoteException;

    void setTcpRWmemorySize(String str, String str2) throws RemoteException;

    void socketDestroy(UidRangeParcel[] uidRangeParcelArr, int[] iArr) throws RemoteException;

    void strictUidCleartextPenalty(int i, int i2) throws RemoteException;

    void tetherAddForward(String str, String str2) throws RemoteException;

    boolean tetherApplyDnsInterfaces() throws RemoteException;

    String[] tetherDnsList() throws RemoteException;

    void tetherDnsSet(int i, String[] strArr) throws RemoteException;

    TetherStatsParcel[] tetherGetStats() throws RemoteException;

    void tetherInterfaceAdd(String str) throws RemoteException;

    String[] tetherInterfaceList() throws RemoteException;

    void tetherInterfaceRemove(String str) throws RemoteException;

    boolean tetherIsEnabled() throws RemoteException;

    @Deprecated
    TetherStatsParcel tetherOffloadGetAndClearStats(int i) throws RemoteException;

    @Deprecated
    TetherStatsParcel[] tetherOffloadGetStats() throws RemoteException;

    @Deprecated
    void tetherOffloadRuleAdd(TetherOffloadRuleParcel tetherOffloadRuleParcel) throws RemoteException;

    @Deprecated
    void tetherOffloadRuleRemove(TetherOffloadRuleParcel tetherOffloadRuleParcel) throws RemoteException;

    @Deprecated
    void tetherOffloadSetInterfaceQuota(int i, long j) throws RemoteException;

    void tetherRemoveForward(String str, String str2) throws RemoteException;

    void tetherStart(String[] strArr) throws RemoteException;

    void tetherStartWithConfiguration(TetherConfigParcel tetherConfigParcel) throws RemoteException;

    void tetherStop() throws RemoteException;

    void trafficSetNetPermForUids(int i, int[] iArr) throws RemoteException;

    void trafficSwapActiveStatsMap() throws RemoteException;

    void wakeupAddInterface(String str, String str2, int i, int i2) throws RemoteException;

    void wakeupDelInterface(String str, String str2, int i, int i2) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements INetd {
        @Override // android.net.INetd
        public boolean isAlive() throws RemoteException {
            return false;
        }

        @Override // android.net.INetd
        public boolean firewallReplaceUidChain(String chainName, boolean isAllowlist, int[] uids) throws RemoteException {
            return false;
        }

        @Override // android.net.INetd
        public boolean bandwidthEnableDataSaver(boolean enable) throws RemoteException {
            return false;
        }

        @Override // android.net.INetd
        public void networkCreatePhysical(int netId, int permission) throws RemoteException {
        }

        @Override // android.net.INetd
        public void networkCreateVpn(int netId, boolean secure) throws RemoteException {
        }

        @Override // android.net.INetd
        public void networkDestroy(int netId) throws RemoteException {
        }

        @Override // android.net.INetd
        public void networkAddInterface(int netId, String iface) throws RemoteException {
        }

        @Override // android.net.INetd
        public void networkRemoveInterface(int netId, String iface) throws RemoteException {
        }

        @Override // android.net.INetd
        public void networkAddUidRanges(int netId, UidRangeParcel[] uidRanges) throws RemoteException {
        }

        @Override // android.net.INetd
        public void networkRemoveUidRanges(int netId, UidRangeParcel[] uidRanges) throws RemoteException {
        }

        @Override // android.net.INetd
        public void networkRejectNonSecureVpn(boolean add, UidRangeParcel[] uidRanges) throws RemoteException {
        }

        @Override // android.net.INetd
        public void socketDestroy(UidRangeParcel[] uidRanges, int[] exemptUids) throws RemoteException {
        }

        @Override // android.net.INetd
        public boolean tetherApplyDnsInterfaces() throws RemoteException {
            return false;
        }

        @Override // android.net.INetd
        public TetherStatsParcel[] tetherGetStats() throws RemoteException {
            return null;
        }

        @Override // android.net.INetd
        public void interfaceAddAddress(String ifName, String addrString, int prefixLength) throws RemoteException {
        }

        @Override // android.net.INetd
        public void interfaceDelAddress(String ifName, String addrString, int prefixLength) throws RemoteException {
        }

        @Override // android.net.INetd
        public String getProcSysNet(int ipversion, int which, String ifname, String parameter) throws RemoteException {
            return null;
        }

        @Override // android.net.INetd
        public void setProcSysNet(int ipversion, int which, String ifname, String parameter, String value) throws RemoteException {
        }

        @Override // android.net.INetd
        public void ipSecSetEncapSocketOwner(ParcelFileDescriptor socket, int newUid) throws RemoteException {
        }

        @Override // android.net.INetd
        public int ipSecAllocateSpi(int transformId, String sourceAddress, String destinationAddress, int spi) throws RemoteException {
            return 0;
        }

        @Override // android.net.INetd
        public void ipSecAddSecurityAssociation(int transformId, int mode, String sourceAddress, String destinationAddress, int underlyingNetId, int spi, int markValue, int markMask, String authAlgo, byte[] authKey, int authTruncBits, String cryptAlgo, byte[] cryptKey, int cryptTruncBits, String aeadAlgo, byte[] aeadKey, int aeadIcvBits, int encapType, int encapLocalPort, int encapRemotePort, int interfaceId) throws RemoteException {
        }

        @Override // android.net.INetd
        public void ipSecDeleteSecurityAssociation(int transformId, String sourceAddress, String destinationAddress, int spi, int markValue, int markMask, int interfaceId) throws RemoteException {
        }

        @Override // android.net.INetd
        public void ipSecApplyTransportModeTransform(ParcelFileDescriptor socket, int transformId, int direction, String sourceAddress, String destinationAddress, int spi) throws RemoteException {
        }

        @Override // android.net.INetd
        public void ipSecRemoveTransportModeTransform(ParcelFileDescriptor socket) throws RemoteException {
        }

        @Override // android.net.INetd
        public void ipSecAddSecurityPolicy(int transformId, int selAddrFamily, int direction, String tmplSrcAddress, String tmplDstAddress, int spi, int markValue, int markMask, int interfaceId) throws RemoteException {
        }

        @Override // android.net.INetd
        public void ipSecUpdateSecurityPolicy(int transformId, int selAddrFamily, int direction, String tmplSrcAddress, String tmplDstAddress, int spi, int markValue, int markMask, int interfaceId) throws RemoteException {
        }

        @Override // android.net.INetd
        public void ipSecDeleteSecurityPolicy(int transformId, int selAddrFamily, int direction, int markValue, int markMask, int interfaceId) throws RemoteException {
        }

        @Override // android.net.INetd
        public void ipSecAddTunnelInterface(String deviceName, String localAddress, String remoteAddress, int iKey, int oKey, int interfaceId) throws RemoteException {
        }

        @Override // android.net.INetd
        public void ipSecUpdateTunnelInterface(String deviceName, String localAddress, String remoteAddress, int iKey, int oKey, int interfaceId) throws RemoteException {
        }

        @Override // android.net.INetd
        public void ipSecRemoveTunnelInterface(String deviceName) throws RemoteException {
        }

        @Override // android.net.INetd
        public void wakeupAddInterface(String ifName, String prefix, int mark, int mask) throws RemoteException {
        }

        @Override // android.net.INetd
        public void wakeupDelInterface(String ifName, String prefix, int mark, int mask) throws RemoteException {
        }

        @Override // android.net.INetd
        public void setIPv6AddrGenMode(String ifName, int mode) throws RemoteException {
        }

        @Override // android.net.INetd
        public void idletimerAddInterface(String ifName, int timeout, String classLabel) throws RemoteException {
        }

        @Override // android.net.INetd
        public void idletimerRemoveInterface(String ifName, int timeout, String classLabel) throws RemoteException {
        }

        @Override // android.net.INetd
        public void strictUidCleartextPenalty(int uid, int policyPenalty) throws RemoteException {
        }

        @Override // android.net.INetd
        public String clatdStart(String ifName, String nat64Prefix) throws RemoteException {
            return null;
        }

        @Override // android.net.INetd
        public void clatdStop(String ifName) throws RemoteException {
        }

        @Override // android.net.INetd
        public boolean ipfwdEnabled() throws RemoteException {
            return false;
        }

        @Override // android.net.INetd
        public String[] ipfwdGetRequesterList() throws RemoteException {
            return null;
        }

        @Override // android.net.INetd
        public void ipfwdEnableForwarding(String requester) throws RemoteException {
        }

        @Override // android.net.INetd
        public void ipfwdDisableForwarding(String requester) throws RemoteException {
        }

        @Override // android.net.INetd
        public void ipfwdAddInterfaceForward(String fromIface, String toIface) throws RemoteException {
        }

        @Override // android.net.INetd
        public void ipfwdRemoveInterfaceForward(String fromIface, String toIface) throws RemoteException {
        }

        @Override // android.net.INetd
        public void bandwidthSetInterfaceQuota(String ifName, long bytes) throws RemoteException {
        }

        @Override // android.net.INetd
        public void bandwidthRemoveInterfaceQuota(String ifName) throws RemoteException {
        }

        @Override // android.net.INetd
        public void bandwidthSetInterfaceAlert(String ifName, long bytes) throws RemoteException {
        }

        @Override // android.net.INetd
        public void bandwidthRemoveInterfaceAlert(String ifName) throws RemoteException {
        }

        @Override // android.net.INetd
        public void bandwidthSetGlobalAlert(long bytes) throws RemoteException {
        }

        @Override // android.net.INetd
        public void bandwidthAddNaughtyApp(int uid) throws RemoteException {
        }

        @Override // android.net.INetd
        public void bandwidthRemoveNaughtyApp(int uid) throws RemoteException {
        }

        @Override // android.net.INetd
        public void bandwidthAddNiceApp(int uid) throws RemoteException {
        }

        @Override // android.net.INetd
        public void bandwidthRemoveNiceApp(int uid) throws RemoteException {
        }

        @Override // android.net.INetd
        public void tetherStart(String[] dhcpRanges) throws RemoteException {
        }

        @Override // android.net.INetd
        public void tetherStop() throws RemoteException {
        }

        @Override // android.net.INetd
        public boolean tetherIsEnabled() throws RemoteException {
            return false;
        }

        @Override // android.net.INetd
        public void tetherInterfaceAdd(String ifName) throws RemoteException {
        }

        @Override // android.net.INetd
        public void tetherInterfaceRemove(String ifName) throws RemoteException {
        }

        @Override // android.net.INetd
        public String[] tetherInterfaceList() throws RemoteException {
            return null;
        }

        @Override // android.net.INetd
        public void tetherDnsSet(int netId, String[] dnsAddrs) throws RemoteException {
        }

        @Override // android.net.INetd
        public String[] tetherDnsList() throws RemoteException {
            return null;
        }

        @Override // android.net.INetd
        public void networkAddRoute(int netId, String ifName, String destination, String nextHop) throws RemoteException {
        }

        @Override // android.net.INetd
        public void networkRemoveRoute(int netId, String ifName, String destination, String nextHop) throws RemoteException {
        }

        @Override // android.net.INetd
        public void networkAddLegacyRoute(int netId, String ifName, String destination, String nextHop, int uid) throws RemoteException {
        }

        @Override // android.net.INetd
        public void networkRemoveLegacyRoute(int netId, String ifName, String destination, String nextHop, int uid) throws RemoteException {
        }

        @Override // android.net.INetd
        public int networkGetDefault() throws RemoteException {
            return 0;
        }

        @Override // android.net.INetd
        public void networkSetDefault(int netId) throws RemoteException {
        }

        @Override // android.net.INetd
        public void networkClearDefault() throws RemoteException {
        }

        @Override // android.net.INetd
        public void networkSetPermissionForNetwork(int netId, int permission) throws RemoteException {
        }

        @Override // android.net.INetd
        public void networkSetPermissionForUser(int permission, int[] uids) throws RemoteException {
        }

        @Override // android.net.INetd
        public void networkClearPermissionForUser(int[] uids) throws RemoteException {
        }

        @Override // android.net.INetd
        public void trafficSetNetPermForUids(int permission, int[] uids) throws RemoteException {
        }

        @Override // android.net.INetd
        public void networkSetProtectAllow(int uid) throws RemoteException {
        }

        @Override // android.net.INetd
        public void networkSetProtectDeny(int uid) throws RemoteException {
        }

        @Override // android.net.INetd
        public boolean networkCanProtect(int uid) throws RemoteException {
            return false;
        }

        @Override // android.net.INetd
        public void firewallSetFirewallType(int firewalltype) throws RemoteException {
        }

        @Override // android.net.INetd
        public void firewallSetInterfaceRule(String ifName, int firewallRule) throws RemoteException {
        }

        @Override // android.net.INetd
        public void firewallSetUidRule(int childChain, int uid, int firewallRule) throws RemoteException {
        }

        @Override // android.net.INetd
        public void firewallEnableChildChain(int childChain, boolean enable) throws RemoteException {
        }

        @Override // android.net.INetd
        public String[] interfaceGetList() throws RemoteException {
            return null;
        }

        @Override // android.net.INetd
        public InterfaceConfigurationParcel interfaceGetCfg(String ifName) throws RemoteException {
            return null;
        }

        @Override // android.net.INetd
        public void interfaceSetCfg(InterfaceConfigurationParcel cfg) throws RemoteException {
        }

        @Override // android.net.INetd
        public void interfaceSetIPv6PrivacyExtensions(String ifName, boolean enable) throws RemoteException {
        }

        @Override // android.net.INetd
        public void interfaceClearAddrs(String ifName) throws RemoteException {
        }

        @Override // android.net.INetd
        public void interfaceSetEnableIPv6(String ifName, boolean enable) throws RemoteException {
        }

        @Override // android.net.INetd
        public void interfaceSetMtu(String ifName, int mtu) throws RemoteException {
        }

        @Override // android.net.INetd
        public void tetherAddForward(String intIface, String extIface) throws RemoteException {
        }

        @Override // android.net.INetd
        public void tetherRemoveForward(String intIface, String extIface) throws RemoteException {
        }

        @Override // android.net.INetd
        public void setTcpRWmemorySize(String rmemValues, String wmemValues) throws RemoteException {
        }

        @Override // android.net.INetd
        public void registerUnsolicitedEventListener(INetdUnsolicitedEventListener listener) throws RemoteException {
        }

        @Override // android.net.INetd
        public void firewallAddUidInterfaceRules(String ifName, int[] uids) throws RemoteException {
        }

        @Override // android.net.INetd
        public void firewallRemoveUidInterfaceRules(int[] uids) throws RemoteException {
        }

        @Override // android.net.INetd
        public void trafficSwapActiveStatsMap() throws RemoteException {
        }

        @Override // android.net.INetd
        public IBinder getOemNetd() throws RemoteException {
            return null;
        }

        @Override // android.net.INetd
        public void tetherStartWithConfiguration(TetherConfigParcel config) throws RemoteException {
        }

        @Override // android.net.INetd
        public MarkMaskParcel getFwmarkForNetwork(int netId) throws RemoteException {
            return null;
        }

        @Override // android.net.INetd
        public void networkAddRouteParcel(int netId, RouteInfoParcel routeInfo) throws RemoteException {
        }

        @Override // android.net.INetd
        public void networkUpdateRouteParcel(int netId, RouteInfoParcel routeInfo) throws RemoteException {
        }

        @Override // android.net.INetd
        public void networkRemoveRouteParcel(int netId, RouteInfoParcel routeInfo) throws RemoteException {
        }

        @Override // android.net.INetd
        public void tetherOffloadRuleAdd(TetherOffloadRuleParcel rule) throws RemoteException {
        }

        @Override // android.net.INetd
        public void tetherOffloadRuleRemove(TetherOffloadRuleParcel rule) throws RemoteException {
        }

        @Override // android.net.INetd
        public TetherStatsParcel[] tetherOffloadGetStats() throws RemoteException {
            return null;
        }

        @Override // android.net.INetd
        public void tetherOffloadSetInterfaceQuota(int ifIndex, long quotaBytes) throws RemoteException {
        }

        @Override // android.net.INetd
        public TetherStatsParcel tetherOffloadGetAndClearStats(int ifIndex) throws RemoteException {
            return null;
        }

        @Override // android.net.INetd
        public void networkCreate(NativeNetworkConfig config) throws RemoteException {
        }

        @Override // android.net.INetd
        public void networkAddUidRangesParcel(NativeUidRangeConfig uidRangesConfig) throws RemoteException {
        }

        @Override // android.net.INetd
        public void networkRemoveUidRangesParcel(NativeUidRangeConfig uidRangesConfig) throws RemoteException {
        }

        @Override // android.net.INetd
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // android.net.INetd
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements INetd {
        public static final String DESCRIPTOR = "android$net$INetd".replace('$', '.');
        static final int TRANSACTION_bandwidthAddNaughtyApp = 50;
        static final int TRANSACTION_bandwidthAddNiceApp = 52;
        static final int TRANSACTION_bandwidthEnableDataSaver = 3;
        static final int TRANSACTION_bandwidthRemoveInterfaceAlert = 48;
        static final int TRANSACTION_bandwidthRemoveInterfaceQuota = 46;
        static final int TRANSACTION_bandwidthRemoveNaughtyApp = 51;
        static final int TRANSACTION_bandwidthRemoveNiceApp = 53;
        static final int TRANSACTION_bandwidthSetGlobalAlert = 49;
        static final int TRANSACTION_bandwidthSetInterfaceAlert = 47;
        static final int TRANSACTION_bandwidthSetInterfaceQuota = 45;
        static final int TRANSACTION_clatdStart = 37;
        static final int TRANSACTION_clatdStop = 38;
        static final int TRANSACTION_firewallAddUidInterfaceRules = 91;
        static final int TRANSACTION_firewallEnableChildChain = 79;
        static final int TRANSACTION_firewallRemoveUidInterfaceRules = 92;
        static final int TRANSACTION_firewallReplaceUidChain = 2;
        static final int TRANSACTION_firewallSetFirewallType = 76;
        static final int TRANSACTION_firewallSetInterfaceRule = 77;
        static final int TRANSACTION_firewallSetUidRule = 78;
        static final int TRANSACTION_getFwmarkForNetwork = 96;
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_getOemNetd = 94;
        static final int TRANSACTION_getProcSysNet = 17;
        static final int TRANSACTION_idletimerAddInterface = 34;
        static final int TRANSACTION_idletimerRemoveInterface = 35;
        static final int TRANSACTION_interfaceAddAddress = 15;
        static final int TRANSACTION_interfaceClearAddrs = 84;
        static final int TRANSACTION_interfaceDelAddress = 16;
        static final int TRANSACTION_interfaceGetCfg = 81;
        static final int TRANSACTION_interfaceGetList = 80;
        static final int TRANSACTION_interfaceSetCfg = 82;
        static final int TRANSACTION_interfaceSetEnableIPv6 = 85;
        static final int TRANSACTION_interfaceSetIPv6PrivacyExtensions = 83;
        static final int TRANSACTION_interfaceSetMtu = 86;
        static final int TRANSACTION_ipSecAddSecurityAssociation = 21;
        static final int TRANSACTION_ipSecAddSecurityPolicy = 25;
        static final int TRANSACTION_ipSecAddTunnelInterface = 28;
        static final int TRANSACTION_ipSecAllocateSpi = 20;
        static final int TRANSACTION_ipSecApplyTransportModeTransform = 23;
        static final int TRANSACTION_ipSecDeleteSecurityAssociation = 22;
        static final int TRANSACTION_ipSecDeleteSecurityPolicy = 27;
        static final int TRANSACTION_ipSecRemoveTransportModeTransform = 24;
        static final int TRANSACTION_ipSecRemoveTunnelInterface = 30;
        static final int TRANSACTION_ipSecSetEncapSocketOwner = 19;
        static final int TRANSACTION_ipSecUpdateSecurityPolicy = 26;
        static final int TRANSACTION_ipSecUpdateTunnelInterface = 29;
        static final int TRANSACTION_ipfwdAddInterfaceForward = 43;
        static final int TRANSACTION_ipfwdDisableForwarding = 42;
        static final int TRANSACTION_ipfwdEnableForwarding = 41;
        static final int TRANSACTION_ipfwdEnabled = 39;
        static final int TRANSACTION_ipfwdGetRequesterList = 40;
        static final int TRANSACTION_ipfwdRemoveInterfaceForward = 44;
        static final int TRANSACTION_isAlive = 1;
        static final int TRANSACTION_networkAddInterface = 7;
        static final int TRANSACTION_networkAddLegacyRoute = 64;
        static final int TRANSACTION_networkAddRoute = 62;
        static final int TRANSACTION_networkAddRouteParcel = 97;
        static final int TRANSACTION_networkAddUidRanges = 9;
        static final int TRANSACTION_networkAddUidRangesParcel = 106;
        static final int TRANSACTION_networkCanProtect = 75;
        static final int TRANSACTION_networkClearDefault = 68;
        static final int TRANSACTION_networkClearPermissionForUser = 71;
        static final int TRANSACTION_networkCreate = 105;
        static final int TRANSACTION_networkCreatePhysical = 4;
        static final int TRANSACTION_networkCreateVpn = 5;
        static final int TRANSACTION_networkDestroy = 6;
        static final int TRANSACTION_networkGetDefault = 66;
        static final int TRANSACTION_networkRejectNonSecureVpn = 11;
        static final int TRANSACTION_networkRemoveInterface = 8;
        static final int TRANSACTION_networkRemoveLegacyRoute = 65;
        static final int TRANSACTION_networkRemoveRoute = 63;
        static final int TRANSACTION_networkRemoveRouteParcel = 99;
        static final int TRANSACTION_networkRemoveUidRanges = 10;
        static final int TRANSACTION_networkRemoveUidRangesParcel = 107;
        static final int TRANSACTION_networkSetDefault = 67;
        static final int TRANSACTION_networkSetPermissionForNetwork = 69;
        static final int TRANSACTION_networkSetPermissionForUser = 70;
        static final int TRANSACTION_networkSetProtectAllow = 73;
        static final int TRANSACTION_networkSetProtectDeny = 74;
        static final int TRANSACTION_networkUpdateRouteParcel = 98;
        static final int TRANSACTION_registerUnsolicitedEventListener = 90;
        static final int TRANSACTION_setIPv6AddrGenMode = 33;
        static final int TRANSACTION_setProcSysNet = 18;
        static final int TRANSACTION_setTcpRWmemorySize = 89;
        static final int TRANSACTION_socketDestroy = 12;
        static final int TRANSACTION_strictUidCleartextPenalty = 36;
        static final int TRANSACTION_tetherAddForward = 87;
        static final int TRANSACTION_tetherApplyDnsInterfaces = 13;
        static final int TRANSACTION_tetherDnsList = 61;
        static final int TRANSACTION_tetherDnsSet = 60;
        static final int TRANSACTION_tetherGetStats = 14;
        static final int TRANSACTION_tetherInterfaceAdd = 57;
        static final int TRANSACTION_tetherInterfaceList = 59;
        static final int TRANSACTION_tetherInterfaceRemove = 58;
        static final int TRANSACTION_tetherIsEnabled = 56;
        static final int TRANSACTION_tetherOffloadGetAndClearStats = 104;
        static final int TRANSACTION_tetherOffloadGetStats = 102;
        static final int TRANSACTION_tetherOffloadRuleAdd = 100;
        static final int TRANSACTION_tetherOffloadRuleRemove = 101;
        static final int TRANSACTION_tetherOffloadSetInterfaceQuota = 103;
        static final int TRANSACTION_tetherRemoveForward = 88;
        static final int TRANSACTION_tetherStart = 54;
        static final int TRANSACTION_tetherStartWithConfiguration = 95;
        static final int TRANSACTION_tetherStop = 55;
        static final int TRANSACTION_trafficSetNetPermForUids = 72;
        static final int TRANSACTION_trafficSwapActiveStatsMap = 93;
        static final int TRANSACTION_wakeupAddInterface = 31;
        static final int TRANSACTION_wakeupDelInterface = 32;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static INetd asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof INetd)) {
                return (INetd) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(descriptor);
            }
            switch (code) {
                case TRANSACTION_getInterfaceHash /* 16777214 */:
                    reply.writeNoException();
                    reply.writeString(getInterfaceHash());
                    return true;
                case 16777215:
                    reply.writeNoException();
                    reply.writeInt(getInterfaceVersion());
                    return true;
                case 1598968902:
                    reply.writeString(descriptor);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            boolean _result = isAlive();
                            reply.writeNoException();
                            reply.writeBoolean(_result);
                            return true;
                        case 2:
                            String _arg0 = data.readString();
                            boolean _arg1 = data.readBoolean();
                            int[] _arg2 = data.createIntArray();
                            boolean _result2 = firewallReplaceUidChain(_arg0, _arg1, _arg2);
                            reply.writeNoException();
                            reply.writeBoolean(_result2);
                            return true;
                        case 3:
                            boolean _arg02 = data.readBoolean();
                            boolean _result3 = bandwidthEnableDataSaver(_arg02);
                            reply.writeNoException();
                            reply.writeBoolean(_result3);
                            return true;
                        case 4:
                            int _arg03 = data.readInt();
                            int _arg12 = data.readInt();
                            networkCreatePhysical(_arg03, _arg12);
                            reply.writeNoException();
                            return true;
                        case 5:
                            int _arg04 = data.readInt();
                            boolean _arg13 = data.readBoolean();
                            networkCreateVpn(_arg04, _arg13);
                            reply.writeNoException();
                            return true;
                        case 6:
                            int _arg05 = data.readInt();
                            networkDestroy(_arg05);
                            reply.writeNoException();
                            return true;
                        case 7:
                            int _arg06 = data.readInt();
                            String _arg14 = data.readString();
                            networkAddInterface(_arg06, _arg14);
                            reply.writeNoException();
                            return true;
                        case 8:
                            int _arg07 = data.readInt();
                            String _arg15 = data.readString();
                            networkRemoveInterface(_arg07, _arg15);
                            reply.writeNoException();
                            return true;
                        case 9:
                            int _arg08 = data.readInt();
                            UidRangeParcel[] _arg16 = (UidRangeParcel[]) data.createTypedArray(UidRangeParcel.CREATOR);
                            networkAddUidRanges(_arg08, _arg16);
                            reply.writeNoException();
                            return true;
                        case 10:
                            int _arg09 = data.readInt();
                            UidRangeParcel[] _arg17 = (UidRangeParcel[]) data.createTypedArray(UidRangeParcel.CREATOR);
                            networkRemoveUidRanges(_arg09, _arg17);
                            reply.writeNoException();
                            return true;
                        case 11:
                            boolean _arg010 = data.readBoolean();
                            UidRangeParcel[] _arg18 = (UidRangeParcel[]) data.createTypedArray(UidRangeParcel.CREATOR);
                            networkRejectNonSecureVpn(_arg010, _arg18);
                            reply.writeNoException();
                            return true;
                        case 12:
                            UidRangeParcel[] _arg011 = (UidRangeParcel[]) data.createTypedArray(UidRangeParcel.CREATOR);
                            int[] _arg19 = data.createIntArray();
                            socketDestroy(_arg011, _arg19);
                            reply.writeNoException();
                            return true;
                        case 13:
                            boolean _result4 = tetherApplyDnsInterfaces();
                            reply.writeNoException();
                            reply.writeBoolean(_result4);
                            return true;
                        case 14:
                            TetherStatsParcel[] _result5 = tetherGetStats();
                            reply.writeNoException();
                            reply.writeTypedArray(_result5, 1);
                            return true;
                        case 15:
                            String _arg012 = data.readString();
                            String _arg110 = data.readString();
                            int _arg22 = data.readInt();
                            interfaceAddAddress(_arg012, _arg110, _arg22);
                            reply.writeNoException();
                            return true;
                        case 16:
                            String _arg013 = data.readString();
                            String _arg111 = data.readString();
                            int _arg23 = data.readInt();
                            interfaceDelAddress(_arg013, _arg111, _arg23);
                            reply.writeNoException();
                            return true;
                        case 17:
                            int _arg014 = data.readInt();
                            int _arg112 = data.readInt();
                            String _arg24 = data.readString();
                            String _arg3 = data.readString();
                            String _result6 = getProcSysNet(_arg014, _arg112, _arg24, _arg3);
                            reply.writeNoException();
                            reply.writeString(_result6);
                            return true;
                        case 18:
                            int _arg015 = data.readInt();
                            int _arg113 = data.readInt();
                            String _arg25 = data.readString();
                            String _arg32 = data.readString();
                            String _arg4 = data.readString();
                            setProcSysNet(_arg015, _arg113, _arg25, _arg32, _arg4);
                            reply.writeNoException();
                            return true;
                        case 19:
                            ParcelFileDescriptor _arg016 = (ParcelFileDescriptor) data.readTypedObject(ParcelFileDescriptor.CREATOR);
                            int _arg114 = data.readInt();
                            ipSecSetEncapSocketOwner(_arg016, _arg114);
                            reply.writeNoException();
                            return true;
                        case 20:
                            int _arg017 = data.readInt();
                            String _arg115 = data.readString();
                            String _arg26 = data.readString();
                            int _arg33 = data.readInt();
                            int _result7 = ipSecAllocateSpi(_arg017, _arg115, _arg26, _arg33);
                            reply.writeNoException();
                            reply.writeInt(_result7);
                            return true;
                        case 21:
                            int _arg018 = data.readInt();
                            int _arg116 = data.readInt();
                            String _arg27 = data.readString();
                            String _arg34 = data.readString();
                            int _arg42 = data.readInt();
                            int _arg5 = data.readInt();
                            int _arg6 = data.readInt();
                            int _arg7 = data.readInt();
                            String _arg8 = data.readString();
                            byte[] _arg9 = data.createByteArray();
                            int _arg10 = data.readInt();
                            String _arg11 = data.readString();
                            byte[] _arg122 = data.createByteArray();
                            int _arg132 = data.readInt();
                            String _arg142 = data.readString();
                            byte[] _arg152 = data.createByteArray();
                            int _arg162 = data.readInt();
                            int _arg172 = data.readInt();
                            int _arg182 = data.readInt();
                            int _arg192 = data.readInt();
                            int _arg20 = data.readInt();
                            ipSecAddSecurityAssociation(_arg018, _arg116, _arg27, _arg34, _arg42, _arg5, _arg6, _arg7, _arg8, _arg9, _arg10, _arg11, _arg122, _arg132, _arg142, _arg152, _arg162, _arg172, _arg182, _arg192, _arg20);
                            reply.writeNoException();
                            return true;
                        case 22:
                            int _arg019 = data.readInt();
                            String _arg117 = data.readString();
                            String _arg28 = data.readString();
                            int _arg35 = data.readInt();
                            int _arg43 = data.readInt();
                            int _arg52 = data.readInt();
                            int _arg62 = data.readInt();
                            ipSecDeleteSecurityAssociation(_arg019, _arg117, _arg28, _arg35, _arg43, _arg52, _arg62);
                            reply.writeNoException();
                            return true;
                        case 23:
                            ParcelFileDescriptor _arg020 = (ParcelFileDescriptor) data.readTypedObject(ParcelFileDescriptor.CREATOR);
                            int _arg118 = data.readInt();
                            int _arg29 = data.readInt();
                            String _arg36 = data.readString();
                            String _arg44 = data.readString();
                            int _arg53 = data.readInt();
                            ipSecApplyTransportModeTransform(_arg020, _arg118, _arg29, _arg36, _arg44, _arg53);
                            reply.writeNoException();
                            return true;
                        case 24:
                            ParcelFileDescriptor _arg021 = (ParcelFileDescriptor) data.readTypedObject(ParcelFileDescriptor.CREATOR);
                            ipSecRemoveTransportModeTransform(_arg021);
                            reply.writeNoException();
                            return true;
                        case 25:
                            int _arg022 = data.readInt();
                            int _arg119 = data.readInt();
                            int _arg210 = data.readInt();
                            String _arg37 = data.readString();
                            String _arg45 = data.readString();
                            int _arg54 = data.readInt();
                            int _arg63 = data.readInt();
                            int _arg72 = data.readInt();
                            int _arg82 = data.readInt();
                            ipSecAddSecurityPolicy(_arg022, _arg119, _arg210, _arg37, _arg45, _arg54, _arg63, _arg72, _arg82);
                            reply.writeNoException();
                            return true;
                        case 26:
                            int _arg023 = data.readInt();
                            int _arg120 = data.readInt();
                            int _arg211 = data.readInt();
                            String _arg38 = data.readString();
                            String _arg46 = data.readString();
                            int _arg55 = data.readInt();
                            int _arg64 = data.readInt();
                            int _arg73 = data.readInt();
                            int _arg83 = data.readInt();
                            ipSecUpdateSecurityPolicy(_arg023, _arg120, _arg211, _arg38, _arg46, _arg55, _arg64, _arg73, _arg83);
                            reply.writeNoException();
                            return true;
                        case 27:
                            int _arg024 = data.readInt();
                            int _arg121 = data.readInt();
                            int _arg212 = data.readInt();
                            int _arg39 = data.readInt();
                            int _arg47 = data.readInt();
                            int _arg56 = data.readInt();
                            ipSecDeleteSecurityPolicy(_arg024, _arg121, _arg212, _arg39, _arg47, _arg56);
                            reply.writeNoException();
                            return true;
                        case 28:
                            String _arg025 = data.readString();
                            String _arg123 = data.readString();
                            String _arg213 = data.readString();
                            int _arg310 = data.readInt();
                            int _arg48 = data.readInt();
                            int _arg57 = data.readInt();
                            ipSecAddTunnelInterface(_arg025, _arg123, _arg213, _arg310, _arg48, _arg57);
                            reply.writeNoException();
                            return true;
                        case 29:
                            String _arg026 = data.readString();
                            String _arg124 = data.readString();
                            String _arg214 = data.readString();
                            int _arg311 = data.readInt();
                            int _arg49 = data.readInt();
                            int _arg58 = data.readInt();
                            ipSecUpdateTunnelInterface(_arg026, _arg124, _arg214, _arg311, _arg49, _arg58);
                            reply.writeNoException();
                            return true;
                        case 30:
                            String _arg027 = data.readString();
                            ipSecRemoveTunnelInterface(_arg027);
                            reply.writeNoException();
                            return true;
                        case 31:
                            String _arg028 = data.readString();
                            String _arg125 = data.readString();
                            int _arg215 = data.readInt();
                            int _arg312 = data.readInt();
                            wakeupAddInterface(_arg028, _arg125, _arg215, _arg312);
                            reply.writeNoException();
                            return true;
                        case 32:
                            String _arg029 = data.readString();
                            String _arg126 = data.readString();
                            int _arg216 = data.readInt();
                            int _arg313 = data.readInt();
                            wakeupDelInterface(_arg029, _arg126, _arg216, _arg313);
                            reply.writeNoException();
                            return true;
                        case 33:
                            String _arg030 = data.readString();
                            int _arg127 = data.readInt();
                            setIPv6AddrGenMode(_arg030, _arg127);
                            reply.writeNoException();
                            return true;
                        case 34:
                            String _arg031 = data.readString();
                            int _arg128 = data.readInt();
                            String _arg217 = data.readString();
                            idletimerAddInterface(_arg031, _arg128, _arg217);
                            reply.writeNoException();
                            return true;
                        case 35:
                            String _arg032 = data.readString();
                            int _arg129 = data.readInt();
                            String _arg218 = data.readString();
                            idletimerRemoveInterface(_arg032, _arg129, _arg218);
                            reply.writeNoException();
                            return true;
                        case 36:
                            int _arg033 = data.readInt();
                            int _arg130 = data.readInt();
                            strictUidCleartextPenalty(_arg033, _arg130);
                            reply.writeNoException();
                            return true;
                        case 37:
                            String _arg034 = data.readString();
                            String _arg131 = data.readString();
                            String _result8 = clatdStart(_arg034, _arg131);
                            reply.writeNoException();
                            reply.writeString(_result8);
                            return true;
                        case 38:
                            String _arg035 = data.readString();
                            clatdStop(_arg035);
                            reply.writeNoException();
                            return true;
                        case 39:
                            boolean _result9 = ipfwdEnabled();
                            reply.writeNoException();
                            reply.writeBoolean(_result9);
                            return true;
                        case 40:
                            String[] _result10 = ipfwdGetRequesterList();
                            reply.writeNoException();
                            reply.writeStringArray(_result10);
                            return true;
                        case 41:
                            String _arg036 = data.readString();
                            ipfwdEnableForwarding(_arg036);
                            reply.writeNoException();
                            return true;
                        case 42:
                            String _arg037 = data.readString();
                            ipfwdDisableForwarding(_arg037);
                            reply.writeNoException();
                            return true;
                        case 43:
                            String _arg038 = data.readString();
                            String _arg133 = data.readString();
                            ipfwdAddInterfaceForward(_arg038, _arg133);
                            reply.writeNoException();
                            return true;
                        case 44:
                            String _arg039 = data.readString();
                            String _arg134 = data.readString();
                            ipfwdRemoveInterfaceForward(_arg039, _arg134);
                            reply.writeNoException();
                            return true;
                        case 45:
                            String _arg040 = data.readString();
                            long _arg135 = data.readLong();
                            bandwidthSetInterfaceQuota(_arg040, _arg135);
                            reply.writeNoException();
                            return true;
                        case 46:
                            String _arg041 = data.readString();
                            bandwidthRemoveInterfaceQuota(_arg041);
                            reply.writeNoException();
                            return true;
                        case 47:
                            String _arg042 = data.readString();
                            long _arg136 = data.readLong();
                            bandwidthSetInterfaceAlert(_arg042, _arg136);
                            reply.writeNoException();
                            return true;
                        case 48:
                            String _arg043 = data.readString();
                            bandwidthRemoveInterfaceAlert(_arg043);
                            reply.writeNoException();
                            return true;
                        case 49:
                            long _arg044 = data.readLong();
                            bandwidthSetGlobalAlert(_arg044);
                            reply.writeNoException();
                            return true;
                        case 50:
                            int _arg045 = data.readInt();
                            bandwidthAddNaughtyApp(_arg045);
                            reply.writeNoException();
                            return true;
                        case 51:
                            int _arg046 = data.readInt();
                            bandwidthRemoveNaughtyApp(_arg046);
                            reply.writeNoException();
                            return true;
                        case 52:
                            int _arg047 = data.readInt();
                            bandwidthAddNiceApp(_arg047);
                            reply.writeNoException();
                            return true;
                        case 53:
                            int _arg048 = data.readInt();
                            bandwidthRemoveNiceApp(_arg048);
                            reply.writeNoException();
                            return true;
                        case 54:
                            String[] _arg049 = data.createStringArray();
                            tetherStart(_arg049);
                            reply.writeNoException();
                            return true;
                        case 55:
                            tetherStop();
                            reply.writeNoException();
                            return true;
                        case 56:
                            boolean _result11 = tetherIsEnabled();
                            reply.writeNoException();
                            reply.writeBoolean(_result11);
                            return true;
                        case 57:
                            String _arg050 = data.readString();
                            tetherInterfaceAdd(_arg050);
                            reply.writeNoException();
                            return true;
                        case 58:
                            String _arg051 = data.readString();
                            tetherInterfaceRemove(_arg051);
                            reply.writeNoException();
                            return true;
                        case 59:
                            String[] _result12 = tetherInterfaceList();
                            reply.writeNoException();
                            reply.writeStringArray(_result12);
                            return true;
                        case 60:
                            int _arg052 = data.readInt();
                            String[] _arg137 = data.createStringArray();
                            tetherDnsSet(_arg052, _arg137);
                            reply.writeNoException();
                            return true;
                        case 61:
                            String[] _result13 = tetherDnsList();
                            reply.writeNoException();
                            reply.writeStringArray(_result13);
                            return true;
                        case 62:
                            int _arg053 = data.readInt();
                            String _arg138 = data.readString();
                            String _arg219 = data.readString();
                            String _arg314 = data.readString();
                            networkAddRoute(_arg053, _arg138, _arg219, _arg314);
                            reply.writeNoException();
                            return true;
                        case 63:
                            int _arg054 = data.readInt();
                            String _arg139 = data.readString();
                            String _arg220 = data.readString();
                            String _arg315 = data.readString();
                            networkRemoveRoute(_arg054, _arg139, _arg220, _arg315);
                            reply.writeNoException();
                            return true;
                        case 64:
                            int _arg055 = data.readInt();
                            String _arg140 = data.readString();
                            String _arg221 = data.readString();
                            String _arg316 = data.readString();
                            int _arg410 = data.readInt();
                            networkAddLegacyRoute(_arg055, _arg140, _arg221, _arg316, _arg410);
                            reply.writeNoException();
                            return true;
                        case 65:
                            int _arg056 = data.readInt();
                            String _arg141 = data.readString();
                            String _arg222 = data.readString();
                            String _arg317 = data.readString();
                            int _arg411 = data.readInt();
                            networkRemoveLegacyRoute(_arg056, _arg141, _arg222, _arg317, _arg411);
                            reply.writeNoException();
                            return true;
                        case 66:
                            int _result14 = networkGetDefault();
                            reply.writeNoException();
                            reply.writeInt(_result14);
                            return true;
                        case 67:
                            int _arg057 = data.readInt();
                            networkSetDefault(_arg057);
                            reply.writeNoException();
                            return true;
                        case 68:
                            networkClearDefault();
                            reply.writeNoException();
                            return true;
                        case 69:
                            int _arg058 = data.readInt();
                            int _arg143 = data.readInt();
                            networkSetPermissionForNetwork(_arg058, _arg143);
                            reply.writeNoException();
                            return true;
                        case 70:
                            int _arg059 = data.readInt();
                            int[] _arg144 = data.createIntArray();
                            networkSetPermissionForUser(_arg059, _arg144);
                            reply.writeNoException();
                            return true;
                        case 71:
                            int[] _arg060 = data.createIntArray();
                            networkClearPermissionForUser(_arg060);
                            reply.writeNoException();
                            return true;
                        case 72:
                            int _arg061 = data.readInt();
                            int[] _arg145 = data.createIntArray();
                            trafficSetNetPermForUids(_arg061, _arg145);
                            reply.writeNoException();
                            return true;
                        case 73:
                            int _arg062 = data.readInt();
                            networkSetProtectAllow(_arg062);
                            reply.writeNoException();
                            return true;
                        case 74:
                            int _arg063 = data.readInt();
                            networkSetProtectDeny(_arg063);
                            reply.writeNoException();
                            return true;
                        case 75:
                            int _arg064 = data.readInt();
                            boolean _result15 = networkCanProtect(_arg064);
                            reply.writeNoException();
                            reply.writeBoolean(_result15);
                            return true;
                        case 76:
                            int _arg065 = data.readInt();
                            firewallSetFirewallType(_arg065);
                            reply.writeNoException();
                            return true;
                        case 77:
                            String _arg066 = data.readString();
                            int _arg146 = data.readInt();
                            firewallSetInterfaceRule(_arg066, _arg146);
                            reply.writeNoException();
                            return true;
                        case 78:
                            int _arg067 = data.readInt();
                            int _arg147 = data.readInt();
                            int _arg223 = data.readInt();
                            firewallSetUidRule(_arg067, _arg147, _arg223);
                            reply.writeNoException();
                            return true;
                        case 79:
                            int _arg068 = data.readInt();
                            boolean _arg148 = data.readBoolean();
                            firewallEnableChildChain(_arg068, _arg148);
                            reply.writeNoException();
                            return true;
                        case 80:
                            String[] _result16 = interfaceGetList();
                            reply.writeNoException();
                            reply.writeStringArray(_result16);
                            return true;
                        case 81:
                            String _arg069 = data.readString();
                            InterfaceConfigurationParcel _result17 = interfaceGetCfg(_arg069);
                            reply.writeNoException();
                            reply.writeTypedObject(_result17, 1);
                            return true;
                        case 82:
                            InterfaceConfigurationParcel _arg070 = (InterfaceConfigurationParcel) data.readTypedObject(InterfaceConfigurationParcel.CREATOR);
                            interfaceSetCfg(_arg070);
                            reply.writeNoException();
                            return true;
                        case 83:
                            String _arg071 = data.readString();
                            boolean _arg149 = data.readBoolean();
                            interfaceSetIPv6PrivacyExtensions(_arg071, _arg149);
                            reply.writeNoException();
                            return true;
                        case 84:
                            String _arg072 = data.readString();
                            interfaceClearAddrs(_arg072);
                            reply.writeNoException();
                            return true;
                        case 85:
                            String _arg073 = data.readString();
                            boolean _arg150 = data.readBoolean();
                            interfaceSetEnableIPv6(_arg073, _arg150);
                            reply.writeNoException();
                            return true;
                        case 86:
                            String _arg074 = data.readString();
                            int _arg151 = data.readInt();
                            interfaceSetMtu(_arg074, _arg151);
                            reply.writeNoException();
                            return true;
                        case 87:
                            String _arg075 = data.readString();
                            String _arg153 = data.readString();
                            tetherAddForward(_arg075, _arg153);
                            reply.writeNoException();
                            return true;
                        case 88:
                            String _arg076 = data.readString();
                            String _arg154 = data.readString();
                            tetherRemoveForward(_arg076, _arg154);
                            reply.writeNoException();
                            return true;
                        case 89:
                            String _arg077 = data.readString();
                            String _arg155 = data.readString();
                            setTcpRWmemorySize(_arg077, _arg155);
                            reply.writeNoException();
                            return true;
                        case 90:
                            INetdUnsolicitedEventListener _arg078 = INetdUnsolicitedEventListener.Stub.asInterface(data.readStrongBinder());
                            registerUnsolicitedEventListener(_arg078);
                            reply.writeNoException();
                            return true;
                        case 91:
                            String _arg079 = data.readString();
                            int[] _arg156 = data.createIntArray();
                            firewallAddUidInterfaceRules(_arg079, _arg156);
                            reply.writeNoException();
                            return true;
                        case 92:
                            int[] _arg080 = data.createIntArray();
                            firewallRemoveUidInterfaceRules(_arg080);
                            reply.writeNoException();
                            return true;
                        case 93:
                            trafficSwapActiveStatsMap();
                            reply.writeNoException();
                            return true;
                        case 94:
                            IBinder _result18 = getOemNetd();
                            reply.writeNoException();
                            reply.writeStrongBinder(_result18);
                            return true;
                        case 95:
                            TetherConfigParcel _arg081 = (TetherConfigParcel) data.readTypedObject(TetherConfigParcel.CREATOR);
                            tetherStartWithConfiguration(_arg081);
                            reply.writeNoException();
                            return true;
                        case 96:
                            int _arg082 = data.readInt();
                            MarkMaskParcel _result19 = getFwmarkForNetwork(_arg082);
                            reply.writeNoException();
                            reply.writeTypedObject(_result19, 1);
                            return true;
                        case 97:
                            int _arg083 = data.readInt();
                            RouteInfoParcel _arg157 = (RouteInfoParcel) data.readTypedObject(RouteInfoParcel.CREATOR);
                            networkAddRouteParcel(_arg083, _arg157);
                            reply.writeNoException();
                            return true;
                        case 98:
                            int _arg084 = data.readInt();
                            RouteInfoParcel _arg158 = (RouteInfoParcel) data.readTypedObject(RouteInfoParcel.CREATOR);
                            networkUpdateRouteParcel(_arg084, _arg158);
                            reply.writeNoException();
                            return true;
                        case 99:
                            int _arg085 = data.readInt();
                            RouteInfoParcel _arg159 = (RouteInfoParcel) data.readTypedObject(RouteInfoParcel.CREATOR);
                            networkRemoveRouteParcel(_arg085, _arg159);
                            reply.writeNoException();
                            return true;
                        case 100:
                            TetherOffloadRuleParcel _arg086 = (TetherOffloadRuleParcel) data.readTypedObject(TetherOffloadRuleParcel.CREATOR);
                            tetherOffloadRuleAdd(_arg086);
                            reply.writeNoException();
                            return true;
                        case 101:
                            TetherOffloadRuleParcel _arg087 = (TetherOffloadRuleParcel) data.readTypedObject(TetherOffloadRuleParcel.CREATOR);
                            tetherOffloadRuleRemove(_arg087);
                            reply.writeNoException();
                            return true;
                        case 102:
                            TetherStatsParcel[] _result20 = tetherOffloadGetStats();
                            reply.writeNoException();
                            reply.writeTypedArray(_result20, 1);
                            return true;
                        case 103:
                            int _arg088 = data.readInt();
                            long _arg160 = data.readLong();
                            tetherOffloadSetInterfaceQuota(_arg088, _arg160);
                            reply.writeNoException();
                            return true;
                        case 104:
                            int _arg089 = data.readInt();
                            TetherStatsParcel _result21 = tetherOffloadGetAndClearStats(_arg089);
                            reply.writeNoException();
                            reply.writeTypedObject(_result21, 1);
                            return true;
                        case 105:
                            NativeNetworkConfig _arg090 = (NativeNetworkConfig) data.readTypedObject(NativeNetworkConfig.CREATOR);
                            networkCreate(_arg090);
                            reply.writeNoException();
                            return true;
                        case 106:
                            NativeUidRangeConfig _arg091 = (NativeUidRangeConfig) data.readTypedObject(NativeUidRangeConfig.CREATOR);
                            networkAddUidRangesParcel(_arg091);
                            reply.writeNoException();
                            return true;
                        case 107:
                            NativeUidRangeConfig _arg092 = (NativeUidRangeConfig) data.readTypedObject(NativeUidRangeConfig.CREATOR);
                            networkRemoveUidRangesParcel(_arg092);
                            reply.writeNoException();
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements INetd {
            private IBinder mRemote;
            private int mCachedVersion = -1;
            private String mCachedHash = "-1";

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override // android.net.INetd
            public boolean isAlive() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(1, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method isAlive is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public boolean firewallReplaceUidChain(String chainName, boolean isAllowlist, int[] uids) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(chainName);
                    _data.writeBoolean(isAllowlist);
                    _data.writeIntArray(uids);
                    boolean _status = this.mRemote.transact(2, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method firewallReplaceUidChain is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public boolean bandwidthEnableDataSaver(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(enable);
                    boolean _status = this.mRemote.transact(3, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method bandwidthEnableDataSaver is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void networkCreatePhysical(int netId, int permission) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeInt(permission);
                    boolean _status = this.mRemote.transact(4, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method networkCreatePhysical is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void networkCreateVpn(int netId, boolean secure) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeBoolean(secure);
                    boolean _status = this.mRemote.transact(5, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method networkCreateVpn is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void networkDestroy(int netId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    boolean _status = this.mRemote.transact(6, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method networkDestroy is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void networkAddInterface(int netId, String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeString(iface);
                    boolean _status = this.mRemote.transact(7, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method networkAddInterface is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void networkRemoveInterface(int netId, String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeString(iface);
                    boolean _status = this.mRemote.transact(8, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method networkRemoveInterface is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void networkAddUidRanges(int netId, UidRangeParcel[] uidRanges) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeTypedArray(uidRanges, 0);
                    boolean _status = this.mRemote.transact(9, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method networkAddUidRanges is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void networkRemoveUidRanges(int netId, UidRangeParcel[] uidRanges) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeTypedArray(uidRanges, 0);
                    boolean _status = this.mRemote.transact(10, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method networkRemoveUidRanges is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void networkRejectNonSecureVpn(boolean add, UidRangeParcel[] uidRanges) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(add);
                    _data.writeTypedArray(uidRanges, 0);
                    boolean _status = this.mRemote.transact(11, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method networkRejectNonSecureVpn is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void socketDestroy(UidRangeParcel[] uidRanges, int[] exemptUids) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedArray(uidRanges, 0);
                    _data.writeIntArray(exemptUids);
                    boolean _status = this.mRemote.transact(12, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method socketDestroy is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public boolean tetherApplyDnsInterfaces() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(13, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method tetherApplyDnsInterfaces is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public TetherStatsParcel[] tetherGetStats() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(14, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method tetherGetStats is unimplemented.");
                    }
                    _reply.readException();
                    TetherStatsParcel[] _result = (TetherStatsParcel[]) _reply.createTypedArray(TetherStatsParcel.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void interfaceAddAddress(String ifName, String addrString, int prefixLength) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeString(addrString);
                    _data.writeInt(prefixLength);
                    boolean _status = this.mRemote.transact(15, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method interfaceAddAddress is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void interfaceDelAddress(String ifName, String addrString, int prefixLength) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeString(addrString);
                    _data.writeInt(prefixLength);
                    boolean _status = this.mRemote.transact(16, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method interfaceDelAddress is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public String getProcSysNet(int ipversion, int which, String ifname, String parameter) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(ipversion);
                    _data.writeInt(which);
                    _data.writeString(ifname);
                    _data.writeString(parameter);
                    boolean _status = this.mRemote.transact(17, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method getProcSysNet is unimplemented.");
                    }
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void setProcSysNet(int ipversion, int which, String ifname, String parameter, String value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(ipversion);
                    _data.writeInt(which);
                    _data.writeString(ifname);
                    _data.writeString(parameter);
                    _data.writeString(value);
                    boolean _status = this.mRemote.transact(18, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method setProcSysNet is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void ipSecSetEncapSocketOwner(ParcelFileDescriptor socket, int newUid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(socket, 0);
                    _data.writeInt(newUid);
                    boolean _status = this.mRemote.transact(19, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method ipSecSetEncapSocketOwner is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public int ipSecAllocateSpi(int transformId, String sourceAddress, String destinationAddress, int spi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(transformId);
                    _data.writeString(sourceAddress);
                    _data.writeString(destinationAddress);
                    _data.writeInt(spi);
                    boolean _status = this.mRemote.transact(20, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method ipSecAllocateSpi is unimplemented.");
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void ipSecAddSecurityAssociation(int transformId, int mode, String sourceAddress, String destinationAddress, int underlyingNetId, int spi, int markValue, int markMask, String authAlgo, byte[] authKey, int authTruncBits, String cryptAlgo, byte[] cryptKey, int cryptTruncBits, String aeadAlgo, byte[] aeadKey, int aeadIcvBits, int encapType, int encapLocalPort, int encapRemotePort, int interfaceId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(transformId);
                    _data.writeInt(mode);
                    try {
                        _data.writeString(sourceAddress);
                        try {
                            _data.writeString(destinationAddress);
                            try {
                                _data.writeInt(underlyingNetId);
                                try {
                                    _data.writeInt(spi);
                                    try {
                                        _data.writeInt(markValue);
                                        try {
                                            _data.writeInt(markMask);
                                        } catch (Throwable th) {
                                            th = th;
                                            _reply.recycle();
                                            _data.recycle();
                                            throw th;
                                        }
                                    } catch (Throwable th2) {
                                        th = th2;
                                        _reply.recycle();
                                        _data.recycle();
                                        throw th;
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                    _reply.recycle();
                                    _data.recycle();
                                    throw th;
                                }
                            } catch (Throwable th4) {
                                th = th4;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th7) {
                    th = th7;
                }
                try {
                    _data.writeString(authAlgo);
                    try {
                        _data.writeByteArray(authKey);
                        try {
                            _data.writeInt(authTruncBits);
                            try {
                                _data.writeString(cryptAlgo);
                                try {
                                    _data.writeByteArray(cryptKey);
                                    _data.writeInt(cryptTruncBits);
                                    _data.writeString(aeadAlgo);
                                    _data.writeByteArray(aeadKey);
                                    _data.writeInt(aeadIcvBits);
                                    _data.writeInt(encapType);
                                    _data.writeInt(encapLocalPort);
                                    _data.writeInt(encapRemotePort);
                                    _data.writeInt(interfaceId);
                                    boolean _status = this.mRemote.transact(21, _data, _reply, 0);
                                    if (!_status) {
                                        throw new RemoteException("Method ipSecAddSecurityAssociation is unimplemented.");
                                    }
                                    _reply.readException();
                                    _reply.recycle();
                                    _data.recycle();
                                } catch (Throwable th8) {
                                    th = th8;
                                    _reply.recycle();
                                    _data.recycle();
                                    throw th;
                                }
                            } catch (Throwable th9) {
                                th = th9;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th10) {
                            th = th10;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th11) {
                        th = th11;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th12) {
                    th = th12;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }

            @Override // android.net.INetd
            public void ipSecDeleteSecurityAssociation(int transformId, String sourceAddress, String destinationAddress, int spi, int markValue, int markMask, int interfaceId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(transformId);
                    _data.writeString(sourceAddress);
                    _data.writeString(destinationAddress);
                    _data.writeInt(spi);
                    _data.writeInt(markValue);
                    _data.writeInt(markMask);
                    _data.writeInt(interfaceId);
                    boolean _status = this.mRemote.transact(22, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method ipSecDeleteSecurityAssociation is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void ipSecApplyTransportModeTransform(ParcelFileDescriptor socket, int transformId, int direction, String sourceAddress, String destinationAddress, int spi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(socket, 0);
                    _data.writeInt(transformId);
                    _data.writeInt(direction);
                    _data.writeString(sourceAddress);
                    _data.writeString(destinationAddress);
                    _data.writeInt(spi);
                    boolean _status = this.mRemote.transact(23, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method ipSecApplyTransportModeTransform is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void ipSecRemoveTransportModeTransform(ParcelFileDescriptor socket) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(socket, 0);
                    boolean _status = this.mRemote.transact(24, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method ipSecRemoveTransportModeTransform is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void ipSecAddSecurityPolicy(int transformId, int selAddrFamily, int direction, String tmplSrcAddress, String tmplDstAddress, int spi, int markValue, int markMask, int interfaceId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(transformId);
                    _data.writeInt(selAddrFamily);
                    _data.writeInt(direction);
                    _data.writeString(tmplSrcAddress);
                    _data.writeString(tmplDstAddress);
                    _data.writeInt(spi);
                    _data.writeInt(markValue);
                    _data.writeInt(markMask);
                    _data.writeInt(interfaceId);
                    boolean _status = this.mRemote.transact(25, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method ipSecAddSecurityPolicy is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void ipSecUpdateSecurityPolicy(int transformId, int selAddrFamily, int direction, String tmplSrcAddress, String tmplDstAddress, int spi, int markValue, int markMask, int interfaceId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(transformId);
                    _data.writeInt(selAddrFamily);
                    _data.writeInt(direction);
                    _data.writeString(tmplSrcAddress);
                    _data.writeString(tmplDstAddress);
                    _data.writeInt(spi);
                    _data.writeInt(markValue);
                    _data.writeInt(markMask);
                    _data.writeInt(interfaceId);
                    boolean _status = this.mRemote.transact(26, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method ipSecUpdateSecurityPolicy is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void ipSecDeleteSecurityPolicy(int transformId, int selAddrFamily, int direction, int markValue, int markMask, int interfaceId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(transformId);
                    _data.writeInt(selAddrFamily);
                    _data.writeInt(direction);
                    _data.writeInt(markValue);
                    _data.writeInt(markMask);
                    _data.writeInt(interfaceId);
                    boolean _status = this.mRemote.transact(27, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method ipSecDeleteSecurityPolicy is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void ipSecAddTunnelInterface(String deviceName, String localAddress, String remoteAddress, int iKey, int oKey, int interfaceId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(deviceName);
                    _data.writeString(localAddress);
                    _data.writeString(remoteAddress);
                    _data.writeInt(iKey);
                    _data.writeInt(oKey);
                    _data.writeInt(interfaceId);
                    boolean _status = this.mRemote.transact(28, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method ipSecAddTunnelInterface is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void ipSecUpdateTunnelInterface(String deviceName, String localAddress, String remoteAddress, int iKey, int oKey, int interfaceId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(deviceName);
                    _data.writeString(localAddress);
                    _data.writeString(remoteAddress);
                    _data.writeInt(iKey);
                    _data.writeInt(oKey);
                    _data.writeInt(interfaceId);
                    boolean _status = this.mRemote.transact(29, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method ipSecUpdateTunnelInterface is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void ipSecRemoveTunnelInterface(String deviceName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(deviceName);
                    boolean _status = this.mRemote.transact(30, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method ipSecRemoveTunnelInterface is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void wakeupAddInterface(String ifName, String prefix, int mark, int mask) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeString(prefix);
                    _data.writeInt(mark);
                    _data.writeInt(mask);
                    boolean _status = this.mRemote.transact(31, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method wakeupAddInterface is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void wakeupDelInterface(String ifName, String prefix, int mark, int mask) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeString(prefix);
                    _data.writeInt(mark);
                    _data.writeInt(mask);
                    boolean _status = this.mRemote.transact(32, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method wakeupDelInterface is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void setIPv6AddrGenMode(String ifName, int mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeInt(mode);
                    boolean _status = this.mRemote.transact(33, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method setIPv6AddrGenMode is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void idletimerAddInterface(String ifName, int timeout, String classLabel) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeInt(timeout);
                    _data.writeString(classLabel);
                    boolean _status = this.mRemote.transact(34, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method idletimerAddInterface is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void idletimerRemoveInterface(String ifName, int timeout, String classLabel) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeInt(timeout);
                    _data.writeString(classLabel);
                    boolean _status = this.mRemote.transact(35, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method idletimerRemoveInterface is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void strictUidCleartextPenalty(int uid, int policyPenalty) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(policyPenalty);
                    boolean _status = this.mRemote.transact(36, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method strictUidCleartextPenalty is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public String clatdStart(String ifName, String nat64Prefix) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeString(nat64Prefix);
                    boolean _status = this.mRemote.transact(37, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method clatdStart is unimplemented.");
                    }
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void clatdStop(String ifName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    boolean _status = this.mRemote.transact(38, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method clatdStop is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public boolean ipfwdEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(39, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method ipfwdEnabled is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public String[] ipfwdGetRequesterList() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(40, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method ipfwdGetRequesterList is unimplemented.");
                    }
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void ipfwdEnableForwarding(String requester) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(requester);
                    boolean _status = this.mRemote.transact(41, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method ipfwdEnableForwarding is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void ipfwdDisableForwarding(String requester) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(requester);
                    boolean _status = this.mRemote.transact(42, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method ipfwdDisableForwarding is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void ipfwdAddInterfaceForward(String fromIface, String toIface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(fromIface);
                    _data.writeString(toIface);
                    boolean _status = this.mRemote.transact(43, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method ipfwdAddInterfaceForward is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void ipfwdRemoveInterfaceForward(String fromIface, String toIface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(fromIface);
                    _data.writeString(toIface);
                    boolean _status = this.mRemote.transact(44, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method ipfwdRemoveInterfaceForward is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void bandwidthSetInterfaceQuota(String ifName, long bytes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeLong(bytes);
                    boolean _status = this.mRemote.transact(45, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method bandwidthSetInterfaceQuota is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void bandwidthRemoveInterfaceQuota(String ifName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    boolean _status = this.mRemote.transact(46, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method bandwidthRemoveInterfaceQuota is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void bandwidthSetInterfaceAlert(String ifName, long bytes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeLong(bytes);
                    boolean _status = this.mRemote.transact(47, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method bandwidthSetInterfaceAlert is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void bandwidthRemoveInterfaceAlert(String ifName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    boolean _status = this.mRemote.transact(48, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method bandwidthRemoveInterfaceAlert is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void bandwidthSetGlobalAlert(long bytes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(bytes);
                    boolean _status = this.mRemote.transact(49, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method bandwidthSetGlobalAlert is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void bandwidthAddNaughtyApp(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    boolean _status = this.mRemote.transact(50, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method bandwidthAddNaughtyApp is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void bandwidthRemoveNaughtyApp(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    boolean _status = this.mRemote.transact(51, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method bandwidthRemoveNaughtyApp is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void bandwidthAddNiceApp(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    boolean _status = this.mRemote.transact(52, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method bandwidthAddNiceApp is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void bandwidthRemoveNiceApp(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    boolean _status = this.mRemote.transact(53, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method bandwidthRemoveNiceApp is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void tetherStart(String[] dhcpRanges) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringArray(dhcpRanges);
                    boolean _status = this.mRemote.transact(54, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method tetherStart is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void tetherStop() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(55, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method tetherStop is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public boolean tetherIsEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(56, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method tetherIsEnabled is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void tetherInterfaceAdd(String ifName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    boolean _status = this.mRemote.transact(57, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method tetherInterfaceAdd is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void tetherInterfaceRemove(String ifName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    boolean _status = this.mRemote.transact(58, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method tetherInterfaceRemove is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public String[] tetherInterfaceList() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(59, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method tetherInterfaceList is unimplemented.");
                    }
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void tetherDnsSet(int netId, String[] dnsAddrs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeStringArray(dnsAddrs);
                    boolean _status = this.mRemote.transact(60, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method tetherDnsSet is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public String[] tetherDnsList() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(61, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method tetherDnsList is unimplemented.");
                    }
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void networkAddRoute(int netId, String ifName, String destination, String nextHop) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeString(ifName);
                    _data.writeString(destination);
                    _data.writeString(nextHop);
                    boolean _status = this.mRemote.transact(62, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method networkAddRoute is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void networkRemoveRoute(int netId, String ifName, String destination, String nextHop) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeString(ifName);
                    _data.writeString(destination);
                    _data.writeString(nextHop);
                    boolean _status = this.mRemote.transact(63, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method networkRemoveRoute is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void networkAddLegacyRoute(int netId, String ifName, String destination, String nextHop, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeString(ifName);
                    _data.writeString(destination);
                    _data.writeString(nextHop);
                    _data.writeInt(uid);
                    boolean _status = this.mRemote.transact(64, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method networkAddLegacyRoute is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void networkRemoveLegacyRoute(int netId, String ifName, String destination, String nextHop, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeString(ifName);
                    _data.writeString(destination);
                    _data.writeString(nextHop);
                    _data.writeInt(uid);
                    boolean _status = this.mRemote.transact(65, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method networkRemoveLegacyRoute is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public int networkGetDefault() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(66, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method networkGetDefault is unimplemented.");
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void networkSetDefault(int netId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    boolean _status = this.mRemote.transact(67, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method networkSetDefault is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void networkClearDefault() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(68, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method networkClearDefault is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void networkSetPermissionForNetwork(int netId, int permission) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeInt(permission);
                    boolean _status = this.mRemote.transact(69, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method networkSetPermissionForNetwork is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void networkSetPermissionForUser(int permission, int[] uids) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(permission);
                    _data.writeIntArray(uids);
                    boolean _status = this.mRemote.transact(70, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method networkSetPermissionForUser is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void networkClearPermissionForUser(int[] uids) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeIntArray(uids);
                    boolean _status = this.mRemote.transact(71, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method networkClearPermissionForUser is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void trafficSetNetPermForUids(int permission, int[] uids) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(permission);
                    _data.writeIntArray(uids);
                    boolean _status = this.mRemote.transact(72, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method trafficSetNetPermForUids is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void networkSetProtectAllow(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    boolean _status = this.mRemote.transact(73, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method networkSetProtectAllow is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void networkSetProtectDeny(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    boolean _status = this.mRemote.transact(74, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method networkSetProtectDeny is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public boolean networkCanProtect(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    boolean _status = this.mRemote.transact(75, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method networkCanProtect is unimplemented.");
                    }
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void firewallSetFirewallType(int firewalltype) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(firewalltype);
                    boolean _status = this.mRemote.transact(76, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method firewallSetFirewallType is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void firewallSetInterfaceRule(String ifName, int firewallRule) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeInt(firewallRule);
                    boolean _status = this.mRemote.transact(77, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method firewallSetInterfaceRule is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void firewallSetUidRule(int childChain, int uid, int firewallRule) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(childChain);
                    _data.writeInt(uid);
                    _data.writeInt(firewallRule);
                    boolean _status = this.mRemote.transact(78, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method firewallSetUidRule is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void firewallEnableChildChain(int childChain, boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(childChain);
                    _data.writeBoolean(enable);
                    boolean _status = this.mRemote.transact(79, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method firewallEnableChildChain is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public String[] interfaceGetList() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(80, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method interfaceGetList is unimplemented.");
                    }
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public InterfaceConfigurationParcel interfaceGetCfg(String ifName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    boolean _status = this.mRemote.transact(81, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method interfaceGetCfg is unimplemented.");
                    }
                    _reply.readException();
                    InterfaceConfigurationParcel _result = (InterfaceConfigurationParcel) _reply.readTypedObject(InterfaceConfigurationParcel.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void interfaceSetCfg(InterfaceConfigurationParcel cfg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(cfg, 0);
                    boolean _status = this.mRemote.transact(82, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method interfaceSetCfg is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void interfaceSetIPv6PrivacyExtensions(String ifName, boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeBoolean(enable);
                    boolean _status = this.mRemote.transact(83, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method interfaceSetIPv6PrivacyExtensions is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void interfaceClearAddrs(String ifName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    boolean _status = this.mRemote.transact(84, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method interfaceClearAddrs is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void interfaceSetEnableIPv6(String ifName, boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeBoolean(enable);
                    boolean _status = this.mRemote.transact(85, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method interfaceSetEnableIPv6 is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void interfaceSetMtu(String ifName, int mtu) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeInt(mtu);
                    boolean _status = this.mRemote.transact(86, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method interfaceSetMtu is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void tetherAddForward(String intIface, String extIface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(intIface);
                    _data.writeString(extIface);
                    boolean _status = this.mRemote.transact(87, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method tetherAddForward is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void tetherRemoveForward(String intIface, String extIface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(intIface);
                    _data.writeString(extIface);
                    boolean _status = this.mRemote.transact(88, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method tetherRemoveForward is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void setTcpRWmemorySize(String rmemValues, String wmemValues) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(rmemValues);
                    _data.writeString(wmemValues);
                    boolean _status = this.mRemote.transact(89, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method setTcpRWmemorySize is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void registerUnsolicitedEventListener(INetdUnsolicitedEventListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    boolean _status = this.mRemote.transact(90, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method registerUnsolicitedEventListener is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void firewallAddUidInterfaceRules(String ifName, int[] uids) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeIntArray(uids);
                    boolean _status = this.mRemote.transact(91, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method firewallAddUidInterfaceRules is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void firewallRemoveUidInterfaceRules(int[] uids) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeIntArray(uids);
                    boolean _status = this.mRemote.transact(92, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method firewallRemoveUidInterfaceRules is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void trafficSwapActiveStatsMap() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(93, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method trafficSwapActiveStatsMap is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public IBinder getOemNetd() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(94, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method getOemNetd is unimplemented.");
                    }
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void tetherStartWithConfiguration(TetherConfigParcel config) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(config, 0);
                    boolean _status = this.mRemote.transact(95, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method tetherStartWithConfiguration is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public MarkMaskParcel getFwmarkForNetwork(int netId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    boolean _status = this.mRemote.transact(96, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method getFwmarkForNetwork is unimplemented.");
                    }
                    _reply.readException();
                    MarkMaskParcel _result = (MarkMaskParcel) _reply.readTypedObject(MarkMaskParcel.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void networkAddRouteParcel(int netId, RouteInfoParcel routeInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeTypedObject(routeInfo, 0);
                    boolean _status = this.mRemote.transact(97, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method networkAddRouteParcel is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void networkUpdateRouteParcel(int netId, RouteInfoParcel routeInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeTypedObject(routeInfo, 0);
                    boolean _status = this.mRemote.transact(98, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method networkUpdateRouteParcel is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void networkRemoveRouteParcel(int netId, RouteInfoParcel routeInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeTypedObject(routeInfo, 0);
                    boolean _status = this.mRemote.transact(99, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method networkRemoveRouteParcel is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void tetherOffloadRuleAdd(TetherOffloadRuleParcel rule) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(rule, 0);
                    boolean _status = this.mRemote.transact(100, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method tetherOffloadRuleAdd is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void tetherOffloadRuleRemove(TetherOffloadRuleParcel rule) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(rule, 0);
                    boolean _status = this.mRemote.transact(101, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method tetherOffloadRuleRemove is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public TetherStatsParcel[] tetherOffloadGetStats() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(102, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method tetherOffloadGetStats is unimplemented.");
                    }
                    _reply.readException();
                    TetherStatsParcel[] _result = (TetherStatsParcel[]) _reply.createTypedArray(TetherStatsParcel.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void tetherOffloadSetInterfaceQuota(int ifIndex, long quotaBytes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(ifIndex);
                    _data.writeLong(quotaBytes);
                    boolean _status = this.mRemote.transact(103, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method tetherOffloadSetInterfaceQuota is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public TetherStatsParcel tetherOffloadGetAndClearStats(int ifIndex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(ifIndex);
                    boolean _status = this.mRemote.transact(104, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method tetherOffloadGetAndClearStats is unimplemented.");
                    }
                    _reply.readException();
                    TetherStatsParcel _result = (TetherStatsParcel) _reply.readTypedObject(TetherStatsParcel.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void networkCreate(NativeNetworkConfig config) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(config, 0);
                    boolean _status = this.mRemote.transact(105, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method networkCreate is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void networkAddUidRangesParcel(NativeUidRangeConfig uidRangesConfig) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(uidRangesConfig, 0);
                    boolean _status = this.mRemote.transact(106, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method networkAddUidRangesParcel is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public void networkRemoveUidRangesParcel(NativeUidRangeConfig uidRangesConfig) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(uidRangesConfig, 0);
                    boolean _status = this.mRemote.transact(107, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method networkRemoveUidRangesParcel is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.INetd
            public int getInterfaceVersion() throws RemoteException {
                if (this.mCachedVersion == -1) {
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    try {
                        data.writeInterfaceToken(Stub.DESCRIPTOR);
                        this.mRemote.transact(16777215, data, reply, 0);
                        reply.readException();
                        this.mCachedVersion = reply.readInt();
                    } finally {
                        reply.recycle();
                        data.recycle();
                    }
                }
                return this.mCachedVersion;
            }

            @Override // android.net.INetd
            public synchronized String getInterfaceHash() throws RemoteException {
                if ("-1".equals(this.mCachedHash)) {
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getInterfaceHash, data, reply, 0);
                    reply.readException();
                    this.mCachedHash = reply.readString();
                    reply.recycle();
                    data.recycle();
                }
                return this.mCachedHash;
            }
        }
    }
}
