package android.net.shared;

import android.net.InformationElementParcelable;
import android.net.Network;
import android.net.ProvisioningConfigurationParcelable;
import android.net.ScanResultInfoParcelable;
import android.net.StaticIpConfiguration;
import android.net.apf.ApfCapabilities;
import android.net.networkstack.aidl.dhcp.DhcpOption;
import android.net.shared.ProvisioningConfiguration;
import android.util.Log;
import com.android.server.UiModeManagerService;
import com.android.server.timezonedetector.ServiceConfigAccessor;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Function;
/* loaded from: classes.dex */
public class ProvisioningConfiguration {
    private static final int DEFAULT_TIMEOUT_MS = 18000;
    public static final int IPV6_ADDR_GEN_MODE_EUI64 = 0;
    public static final int IPV6_ADDR_GEN_MODE_STABLE_PRIVACY = 2;
    private static final String TAG = "ProvisioningConfiguration";
    public static final int VERSION_ADDED_PROVISIONING_ENUM = 12;
    public ApfCapabilities mApfCapabilities;
    public List<DhcpOption> mDhcpOptions;
    public String mDisplayName;
    public boolean mEnablePreconnection;
    public int mIPv4ProvisioningMode;
    public int mIPv6AddrGenMode;
    public int mIPv6ProvisioningMode;
    public InitialConfiguration mInitialConfig;
    public Layer2Information mLayer2Info;
    public Network mNetwork;
    public int mProvisioningTimeoutMs;
    public int mRequestedPreDhcpActionMs;
    public ScanResultInfo mScanResultInfo;
    public StaticIpConfiguration mStaticIpConfig;
    public boolean mUsingIpReachabilityMonitor;
    public boolean mUsingMultinetworkPolicyTracker;

    /* loaded from: classes.dex */
    public static class Builder {
        protected ProvisioningConfiguration mConfig = new ProvisioningConfiguration();

        public Builder withoutIPv4() {
            this.mConfig.mIPv4ProvisioningMode = 0;
            return this;
        }

        public Builder withoutIPv6() {
            this.mConfig.mIPv6ProvisioningMode = 0;
            return this;
        }

        public Builder withoutMultinetworkPolicyTracker() {
            this.mConfig.mUsingMultinetworkPolicyTracker = false;
            return this;
        }

        public Builder withoutIpReachabilityMonitor() {
            this.mConfig.mUsingIpReachabilityMonitor = false;
            return this;
        }

        public Builder withPreDhcpAction() {
            this.mConfig.mRequestedPreDhcpActionMs = ProvisioningConfiguration.DEFAULT_TIMEOUT_MS;
            return this;
        }

        public Builder withPreDhcpAction(int dhcpActionTimeoutMs) {
            this.mConfig.mRequestedPreDhcpActionMs = dhcpActionTimeoutMs;
            return this;
        }

        public Builder withPreconnection() {
            this.mConfig.mEnablePreconnection = true;
            return this;
        }

        public Builder withInitialConfiguration(InitialConfiguration initialConfig) {
            this.mConfig.mInitialConfig = initialConfig;
            return this;
        }

        public Builder withStaticConfiguration(StaticIpConfiguration staticConfig) {
            this.mConfig.mIPv4ProvisioningMode = 1;
            this.mConfig.mStaticIpConfig = staticConfig;
            return this;
        }

        public Builder withApfCapabilities(ApfCapabilities apfCapabilities) {
            this.mConfig.mApfCapabilities = apfCapabilities;
            return this;
        }

        public Builder withProvisioningTimeoutMs(int timeoutMs) {
            this.mConfig.mProvisioningTimeoutMs = timeoutMs;
            return this;
        }

        public Builder withRandomMacAddress() {
            this.mConfig.mIPv6AddrGenMode = 0;
            return this;
        }

        public Builder withStableMacAddress() {
            this.mConfig.mIPv6AddrGenMode = 2;
            return this;
        }

        public Builder withNetwork(Network network) {
            this.mConfig.mNetwork = network;
            return this;
        }

        public Builder withDisplayName(String displayName) {
            this.mConfig.mDisplayName = displayName;
            return this;
        }

        public Builder withScanResultInfo(ScanResultInfo scanResultInfo) {
            this.mConfig.mScanResultInfo = scanResultInfo;
            return this;
        }

        public Builder withLayer2Information(Layer2Information layer2Info) {
            this.mConfig.mLayer2Info = layer2Info;
            return this;
        }

        public Builder withDhcpOptions(List<DhcpOption> options) {
            this.mConfig.mDhcpOptions = options;
            return this;
        }

        public Builder withIpv6LinkLocalOnly() {
            this.mConfig.mIPv6ProvisioningMode = 2;
            return this;
        }

        public ProvisioningConfiguration build() {
            if (this.mConfig.mIPv6ProvisioningMode == 2 && this.mConfig.mIPv4ProvisioningMode != 0) {
                throw new IllegalArgumentException("IPv4 must be disabled in IPv6 link-localonly mode.");
            }
            return new ProvisioningConfiguration(this.mConfig);
        }
    }

    /* loaded from: classes.dex */
    public static class ScanResultInfo {
        private final String mBssid;
        private final List<InformationElement> mInformationElements;
        private final String mSsid;

        /* loaded from: classes.dex */
        public static class InformationElement {
            private final int mId;
            private final byte[] mPayload;

            public InformationElement(int id, ByteBuffer payload) {
                this.mId = id;
                this.mPayload = ScanResultInfo.convertToByteArray(payload.asReadOnlyBuffer());
            }

            public int getId() {
                return this.mId;
            }

            public ByteBuffer getPayload() {
                return ByteBuffer.wrap(this.mPayload).asReadOnlyBuffer();
            }

            public boolean equals(Object o) {
                if (o == this) {
                    return true;
                }
                if (o instanceof InformationElement) {
                    InformationElement other = (InformationElement) o;
                    return this.mId == other.mId && Arrays.equals(this.mPayload, other.mPayload);
                }
                return false;
            }

            public int hashCode() {
                return Objects.hash(Integer.valueOf(this.mId), this.mPayload);
            }

            public String toString() {
                return "ID: " + this.mId + ", " + Arrays.toString(this.mPayload);
            }

            public InformationElementParcelable toStableParcelable() {
                InformationElementParcelable p = new InformationElementParcelable();
                p.id = this.mId;
                byte[] bArr = this.mPayload;
                p.payload = bArr != null ? (byte[]) bArr.clone() : null;
                return p;
            }

            public static InformationElement fromStableParcelable(InformationElementParcelable p) {
                if (p == null) {
                    return null;
                }
                return new InformationElement(p.id, ByteBuffer.wrap((byte[]) p.payload.clone()).asReadOnlyBuffer());
            }
        }

        public ScanResultInfo(String ssid, String bssid, List<InformationElement> informationElements) {
            Objects.requireNonNull(ssid, "ssid must not be null.");
            Objects.requireNonNull(bssid, "bssid must not be null.");
            this.mSsid = ssid;
            this.mBssid = bssid;
            this.mInformationElements = Collections.unmodifiableList(new ArrayList(informationElements));
        }

        public String getSsid() {
            return this.mSsid;
        }

        public String getBssid() {
            return this.mBssid;
        }

        public List<InformationElement> getInformationElements() {
            return this.mInformationElements;
        }

        public String toString() {
            StringBuffer str = new StringBuffer();
            str.append("SSID: ").append(this.mSsid);
            str.append(", BSSID: ").append(this.mBssid);
            str.append(", Information Elements: {");
            for (InformationElement ie : this.mInformationElements) {
                str.append("[").append(ie.toString()).append("]");
            }
            str.append("}");
            return str.toString();
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof ScanResultInfo) {
                ScanResultInfo other = (ScanResultInfo) o;
                return Objects.equals(this.mSsid, other.mSsid) && Objects.equals(this.mBssid, other.mBssid) && this.mInformationElements.equals(other.mInformationElements);
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(this.mSsid, this.mBssid, this.mInformationElements);
        }

        public ScanResultInfoParcelable toStableParcelable() {
            ScanResultInfoParcelable p = new ScanResultInfoParcelable();
            p.ssid = this.mSsid;
            p.bssid = this.mBssid;
            p.informationElements = (InformationElementParcelable[]) ParcelableUtil.toParcelableArray(this.mInformationElements, new Function() { // from class: android.net.shared.ProvisioningConfiguration$ScanResultInfo$$ExternalSyntheticLambda0
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return ((ProvisioningConfiguration.ScanResultInfo.InformationElement) obj).toStableParcelable();
                }
            }, InformationElementParcelable.class);
            return p;
        }

        public static ScanResultInfo fromStableParcelable(ScanResultInfoParcelable p) {
            if (p == null) {
                return null;
            }
            List<InformationElement> ies = new ArrayList<>();
            ies.addAll(ParcelableUtil.fromParcelableArray(p.informationElements, new Function() { // from class: android.net.shared.ProvisioningConfiguration$ScanResultInfo$$ExternalSyntheticLambda1
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return ProvisioningConfiguration.ScanResultInfo.InformationElement.fromStableParcelable((InformationElementParcelable) obj);
                }
            }));
            return new ScanResultInfo(p.ssid, p.bssid, ies);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static byte[] convertToByteArray(ByteBuffer buffer) {
            byte[] bytes = new byte[buffer.limit()];
            ByteBuffer copy = buffer.asReadOnlyBuffer();
            try {
                try {
                    copy.position(0);
                    copy.get(bytes);
                    return bytes;
                } catch (BufferUnderflowException e) {
                    Log.wtf(ProvisioningConfiguration.TAG, "Buffer under flow exception should never happen.");
                    return bytes;
                }
            } catch (Throwable th) {
                return bytes;
            }
        }
    }

    public ProvisioningConfiguration() {
        this.mEnablePreconnection = false;
        this.mUsingMultinetworkPolicyTracker = true;
        this.mUsingIpReachabilityMonitor = true;
        this.mProvisioningTimeoutMs = DEFAULT_TIMEOUT_MS;
        this.mIPv6AddrGenMode = 2;
        this.mNetwork = null;
        this.mDisplayName = null;
        this.mIPv4ProvisioningMode = 2;
        this.mIPv6ProvisioningMode = 1;
    }

    public ProvisioningConfiguration(ProvisioningConfiguration other) {
        this.mEnablePreconnection = false;
        this.mUsingMultinetworkPolicyTracker = true;
        this.mUsingIpReachabilityMonitor = true;
        this.mProvisioningTimeoutMs = DEFAULT_TIMEOUT_MS;
        this.mIPv6AddrGenMode = 2;
        this.mNetwork = null;
        this.mDisplayName = null;
        this.mIPv4ProvisioningMode = 2;
        this.mIPv6ProvisioningMode = 1;
        this.mEnablePreconnection = other.mEnablePreconnection;
        this.mUsingMultinetworkPolicyTracker = other.mUsingMultinetworkPolicyTracker;
        this.mUsingIpReachabilityMonitor = other.mUsingIpReachabilityMonitor;
        this.mRequestedPreDhcpActionMs = other.mRequestedPreDhcpActionMs;
        this.mInitialConfig = InitialConfiguration.copy(other.mInitialConfig);
        this.mStaticIpConfig = other.mStaticIpConfig != null ? new StaticIpConfiguration(other.mStaticIpConfig) : null;
        this.mApfCapabilities = other.mApfCapabilities;
        this.mProvisioningTimeoutMs = other.mProvisioningTimeoutMs;
        this.mIPv6AddrGenMode = other.mIPv6AddrGenMode;
        this.mNetwork = other.mNetwork;
        this.mDisplayName = other.mDisplayName;
        this.mScanResultInfo = other.mScanResultInfo;
        this.mLayer2Info = other.mLayer2Info;
        this.mDhcpOptions = other.mDhcpOptions;
        this.mIPv4ProvisioningMode = other.mIPv4ProvisioningMode;
        this.mIPv6ProvisioningMode = other.mIPv6ProvisioningMode;
    }

    public ProvisioningConfigurationParcelable toStableParcelable() {
        StaticIpConfiguration staticIpConfiguration;
        ProvisioningConfigurationParcelable p = new ProvisioningConfigurationParcelable();
        p.enableIPv4 = this.mIPv4ProvisioningMode != 0;
        p.ipv4ProvisioningMode = this.mIPv4ProvisioningMode;
        p.enableIPv6 = this.mIPv6ProvisioningMode != 0;
        p.ipv6ProvisioningMode = this.mIPv6ProvisioningMode;
        p.enablePreconnection = this.mEnablePreconnection;
        p.usingMultinetworkPolicyTracker = this.mUsingMultinetworkPolicyTracker;
        p.usingIpReachabilityMonitor = this.mUsingIpReachabilityMonitor;
        p.requestedPreDhcpActionMs = this.mRequestedPreDhcpActionMs;
        InitialConfiguration initialConfiguration = this.mInitialConfig;
        p.initialConfig = initialConfiguration == null ? null : initialConfiguration.toStableParcelable();
        if (this.mStaticIpConfig == null) {
            staticIpConfiguration = null;
        } else {
            staticIpConfiguration = new StaticIpConfiguration(this.mStaticIpConfig);
        }
        p.staticIpConfig = staticIpConfiguration;
        p.apfCapabilities = this.mApfCapabilities;
        p.provisioningTimeoutMs = this.mProvisioningTimeoutMs;
        p.ipv6AddrGenMode = this.mIPv6AddrGenMode;
        p.network = this.mNetwork;
        p.displayName = this.mDisplayName;
        ScanResultInfo scanResultInfo = this.mScanResultInfo;
        p.scanResultInfo = scanResultInfo == null ? null : scanResultInfo.toStableParcelable();
        Layer2Information layer2Information = this.mLayer2Info;
        p.layer2Info = layer2Information == null ? null : layer2Information.toStableParcelable();
        p.options = this.mDhcpOptions != null ? new ArrayList(this.mDhcpOptions) : null;
        return p;
    }

    public static ProvisioningConfiguration fromStableParcelable(ProvisioningConfigurationParcelable p, int interfaceVersion) {
        StaticIpConfiguration staticIpConfiguration;
        if (p == null) {
            return null;
        }
        ProvisioningConfiguration config = new ProvisioningConfiguration();
        config.mEnablePreconnection = p.enablePreconnection;
        config.mUsingMultinetworkPolicyTracker = p.usingMultinetworkPolicyTracker;
        config.mUsingIpReachabilityMonitor = p.usingIpReachabilityMonitor;
        config.mRequestedPreDhcpActionMs = p.requestedPreDhcpActionMs;
        config.mInitialConfig = InitialConfiguration.fromStableParcelable(p.initialConfig);
        if (p.staticIpConfig == null) {
            staticIpConfiguration = null;
        } else {
            staticIpConfiguration = new StaticIpConfiguration(p.staticIpConfig);
        }
        config.mStaticIpConfig = staticIpConfiguration;
        config.mApfCapabilities = p.apfCapabilities;
        config.mProvisioningTimeoutMs = p.provisioningTimeoutMs;
        config.mIPv6AddrGenMode = p.ipv6AddrGenMode;
        config.mNetwork = p.network;
        config.mDisplayName = p.displayName;
        config.mScanResultInfo = ScanResultInfo.fromStableParcelable(p.scanResultInfo);
        config.mLayer2Info = Layer2Information.fromStableParcelable(p.layer2Info);
        config.mDhcpOptions = p.options != null ? new ArrayList(p.options) : null;
        if (interfaceVersion < 12) {
            config.mIPv4ProvisioningMode = p.enableIPv4 ? 2 : 0;
            config.mIPv6ProvisioningMode = p.enableIPv6 ? 1 : 0;
        } else {
            config.mIPv4ProvisioningMode = p.ipv4ProvisioningMode;
            config.mIPv6ProvisioningMode = p.ipv6ProvisioningMode;
        }
        return config;
    }

    static String ipv4ProvisioningModeToString(int mode) {
        switch (mode) {
            case 0:
                return ServiceConfigAccessor.PROVIDER_MODE_DISABLED;
            case 1:
                return "static";
            case 2:
                return "dhcp";
            default:
                return UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
        }
    }

    static String ipv6ProvisioningModeToString(int mode) {
        switch (mode) {
            case 0:
                return ServiceConfigAccessor.PROVIDER_MODE_DISABLED;
            case 1:
                return "slaac";
            case 2:
                return "link-local";
            default:
                return UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
        }
    }

    public String toString() {
        String ipv4ProvisioningMode = ipv4ProvisioningModeToString(this.mIPv4ProvisioningMode);
        String ipv6ProvisioningMode = ipv6ProvisioningModeToString(this.mIPv6ProvisioningMode);
        return new StringJoiner(", ", getClass().getSimpleName() + "{", "}").add("mEnablePreconnection: " + this.mEnablePreconnection).add("mUsingMultinetworkPolicyTracker: " + this.mUsingMultinetworkPolicyTracker).add("mUsingIpReachabilityMonitor: " + this.mUsingIpReachabilityMonitor).add("mRequestedPreDhcpActionMs: " + this.mRequestedPreDhcpActionMs).add("mInitialConfig: " + this.mInitialConfig).add("mStaticIpConfig: " + this.mStaticIpConfig).add("mApfCapabilities: " + this.mApfCapabilities).add("mProvisioningTimeoutMs: " + this.mProvisioningTimeoutMs).add("mIPv6AddrGenMode: " + this.mIPv6AddrGenMode).add("mNetwork: " + this.mNetwork).add("mDisplayName: " + this.mDisplayName).add("mScanResultInfo: " + this.mScanResultInfo).add("mLayer2Info: " + this.mLayer2Info).add("mDhcpOptions: " + this.mDhcpOptions).add("mIPv4ProvisioningMode: " + ipv4ProvisioningMode).add("mIPv6ProvisioningMode: " + ipv6ProvisioningMode).toString();
    }

    private static boolean dhcpOptionEquals(DhcpOption obj1, DhcpOption obj2) {
        if (obj1 == obj2) {
            return true;
        }
        if (obj1 == null || obj2 == null) {
            return false;
        }
        if (obj1.type == obj2.type && Arrays.equals(obj1.value, obj2.value)) {
            return true;
        }
        return false;
    }

    private static boolean dhcpOptionListEquals(List<DhcpOption> l1, List<DhcpOption> l2) {
        if (l1 == l2) {
            return true;
        }
        if (l1 == null || l2 == null || l1.size() != l2.size()) {
            return false;
        }
        for (int i = 0; i < l1.size(); i++) {
            if (!dhcpOptionEquals(l1.get(i), l2.get(i))) {
                return false;
            }
        }
        return true;
    }

    public boolean equals(Object obj) {
        if (obj instanceof ProvisioningConfiguration) {
            ProvisioningConfiguration other = (ProvisioningConfiguration) obj;
            return this.mEnablePreconnection == other.mEnablePreconnection && this.mUsingMultinetworkPolicyTracker == other.mUsingMultinetworkPolicyTracker && this.mUsingIpReachabilityMonitor == other.mUsingIpReachabilityMonitor && this.mRequestedPreDhcpActionMs == other.mRequestedPreDhcpActionMs && Objects.equals(this.mInitialConfig, other.mInitialConfig) && Objects.equals(this.mStaticIpConfig, other.mStaticIpConfig) && Objects.equals(this.mApfCapabilities, other.mApfCapabilities) && this.mProvisioningTimeoutMs == other.mProvisioningTimeoutMs && this.mIPv6AddrGenMode == other.mIPv6AddrGenMode && Objects.equals(this.mNetwork, other.mNetwork) && Objects.equals(this.mDisplayName, other.mDisplayName) && Objects.equals(this.mScanResultInfo, other.mScanResultInfo) && Objects.equals(this.mLayer2Info, other.mLayer2Info) && dhcpOptionListEquals(this.mDhcpOptions, other.mDhcpOptions) && this.mIPv4ProvisioningMode == other.mIPv4ProvisioningMode && this.mIPv6ProvisioningMode == other.mIPv6ProvisioningMode;
        }
        return false;
    }

    public boolean isValid() {
        InitialConfiguration initialConfiguration = this.mInitialConfig;
        return initialConfiguration == null || initialConfiguration.isValid();
    }
}
