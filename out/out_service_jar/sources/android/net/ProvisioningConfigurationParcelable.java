package android.net;

import android.net.apf.ApfCapabilities;
import android.net.networkstack.aidl.dhcp.DhcpOption;
import android.os.BadParcelableException;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
/* loaded from: classes.dex */
public class ProvisioningConfigurationParcelable implements Parcelable {
    public static final Parcelable.Creator<ProvisioningConfigurationParcelable> CREATOR = new Parcelable.Creator<ProvisioningConfigurationParcelable>() { // from class: android.net.ProvisioningConfigurationParcelable.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ProvisioningConfigurationParcelable createFromParcel(Parcel _aidl_source) {
            ProvisioningConfigurationParcelable _aidl_out = new ProvisioningConfigurationParcelable();
            _aidl_out.readFromParcel(_aidl_source);
            return _aidl_out;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ProvisioningConfigurationParcelable[] newArray(int _aidl_size) {
            return new ProvisioningConfigurationParcelable[_aidl_size];
        }
    };
    public ApfCapabilities apfCapabilities;
    public String displayName;
    public InitialConfigurationParcelable initialConfig;
    public Layer2InformationParcelable layer2Info;
    public Network network;
    public List<DhcpOption> options;
    public ScanResultInfoParcelable scanResultInfo;
    public StaticIpConfiguration staticIpConfig;
    @Deprecated
    public boolean enableIPv4 = false;
    @Deprecated
    public boolean enableIPv6 = false;
    public boolean usingMultinetworkPolicyTracker = false;
    public boolean usingIpReachabilityMonitor = false;
    public int requestedPreDhcpActionMs = 0;
    public int provisioningTimeoutMs = 0;
    public int ipv6AddrGenMode = 0;
    public boolean enablePreconnection = false;
    public int ipv4ProvisioningMode = 0;
    public int ipv6ProvisioningMode = 0;

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel _aidl_parcel, int _aidl_flag) {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.writeInt(0);
        _aidl_parcel.writeBoolean(this.enableIPv4);
        _aidl_parcel.writeBoolean(this.enableIPv6);
        _aidl_parcel.writeBoolean(this.usingMultinetworkPolicyTracker);
        _aidl_parcel.writeBoolean(this.usingIpReachabilityMonitor);
        _aidl_parcel.writeInt(this.requestedPreDhcpActionMs);
        _aidl_parcel.writeTypedObject(this.initialConfig, _aidl_flag);
        _aidl_parcel.writeTypedObject(this.staticIpConfig, _aidl_flag);
        _aidl_parcel.writeTypedObject(this.apfCapabilities, _aidl_flag);
        _aidl_parcel.writeInt(this.provisioningTimeoutMs);
        _aidl_parcel.writeInt(this.ipv6AddrGenMode);
        _aidl_parcel.writeTypedObject(this.network, _aidl_flag);
        _aidl_parcel.writeString(this.displayName);
        _aidl_parcel.writeBoolean(this.enablePreconnection);
        _aidl_parcel.writeTypedObject(this.scanResultInfo, _aidl_flag);
        _aidl_parcel.writeTypedObject(this.layer2Info, _aidl_flag);
        _aidl_parcel.writeTypedList(this.options);
        _aidl_parcel.writeInt(this.ipv4ProvisioningMode);
        _aidl_parcel.writeInt(this.ipv6ProvisioningMode);
        int _aidl_end_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.setDataPosition(_aidl_start_pos);
        _aidl_parcel.writeInt(_aidl_end_pos - _aidl_start_pos);
        _aidl_parcel.setDataPosition(_aidl_end_pos);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [111=21, 112=20, 114=20] */
    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    public final void readFromParcel(Parcel _aidl_parcel) {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        int _aidl_parcelable_size = _aidl_parcel.readInt();
        try {
            if (_aidl_parcelable_size < 4) {
                throw new BadParcelableException("Parcelable too small");
            }
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.enableIPv4 = _aidl_parcel.readBoolean();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.enableIPv6 = _aidl_parcel.readBoolean();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.usingMultinetworkPolicyTracker = _aidl_parcel.readBoolean();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.usingIpReachabilityMonitor = _aidl_parcel.readBoolean();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.requestedPreDhcpActionMs = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.initialConfig = (InitialConfigurationParcelable) _aidl_parcel.readTypedObject(InitialConfigurationParcelable.CREATOR);
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.staticIpConfig = (StaticIpConfiguration) _aidl_parcel.readTypedObject(StaticIpConfiguration.CREATOR);
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.apfCapabilities = (ApfCapabilities) _aidl_parcel.readTypedObject(ApfCapabilities.CREATOR);
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.provisioningTimeoutMs = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.ipv6AddrGenMode = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.network = (Network) _aidl_parcel.readTypedObject(Network.CREATOR);
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.displayName = _aidl_parcel.readString();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.enablePreconnection = _aidl_parcel.readBoolean();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.scanResultInfo = (ScanResultInfoParcelable) _aidl_parcel.readTypedObject(ScanResultInfoParcelable.CREATOR);
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.layer2Info = (Layer2InformationParcelable) _aidl_parcel.readTypedObject(Layer2InformationParcelable.CREATOR);
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.options = _aidl_parcel.createTypedArrayList(DhcpOption.CREATOR);
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.ipv4ProvisioningMode = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.ipv6ProvisioningMode = _aidl_parcel.readInt();
            if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                throw new BadParcelableException("Overflow in the size of parcelable");
            }
            _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
        } catch (Throwable th) {
            if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                throw new BadParcelableException("Overflow in the size of parcelable");
            }
            _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
            throw th;
        }
    }

    public String toString() {
        StringJoiner _aidl_sj = new StringJoiner(", ", "{", "}");
        _aidl_sj.add("enableIPv4: " + this.enableIPv4);
        _aidl_sj.add("enableIPv6: " + this.enableIPv6);
        _aidl_sj.add("usingMultinetworkPolicyTracker: " + this.usingMultinetworkPolicyTracker);
        _aidl_sj.add("usingIpReachabilityMonitor: " + this.usingIpReachabilityMonitor);
        _aidl_sj.add("requestedPreDhcpActionMs: " + this.requestedPreDhcpActionMs);
        _aidl_sj.add("initialConfig: " + Objects.toString(this.initialConfig));
        _aidl_sj.add("staticIpConfig: " + Objects.toString(this.staticIpConfig));
        _aidl_sj.add("apfCapabilities: " + Objects.toString(this.apfCapabilities));
        _aidl_sj.add("provisioningTimeoutMs: " + this.provisioningTimeoutMs);
        _aidl_sj.add("ipv6AddrGenMode: " + this.ipv6AddrGenMode);
        _aidl_sj.add("network: " + Objects.toString(this.network));
        _aidl_sj.add("displayName: " + Objects.toString(this.displayName));
        _aidl_sj.add("enablePreconnection: " + this.enablePreconnection);
        _aidl_sj.add("scanResultInfo: " + Objects.toString(this.scanResultInfo));
        _aidl_sj.add("layer2Info: " + Objects.toString(this.layer2Info));
        _aidl_sj.add("options: " + Objects.toString(this.options));
        _aidl_sj.add("ipv4ProvisioningMode: " + this.ipv4ProvisioningMode);
        _aidl_sj.add("ipv6ProvisioningMode: " + this.ipv6ProvisioningMode);
        return "android.net.ProvisioningConfigurationParcelable" + _aidl_sj.toString();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        int _mask = 0 | describeContents(this.initialConfig);
        return _mask | describeContents(this.staticIpConfig) | describeContents(this.apfCapabilities) | describeContents(this.network) | describeContents(this.scanResultInfo) | describeContents(this.layer2Info) | describeContents(this.options);
    }

    private int describeContents(Object _v) {
        if (_v == null) {
            return 0;
        }
        if (_v instanceof Collection) {
            int _mask = 0;
            for (Object o : (Collection) _v) {
                _mask |= describeContents(o);
            }
            return _mask;
        } else if (!(_v instanceof Parcelable)) {
            return 0;
        } else {
            return ((Parcelable) _v).describeContents();
        }
    }
}
