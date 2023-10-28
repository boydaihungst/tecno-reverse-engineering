package android.net;

import android.os.BadParcelableException;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;
/* loaded from: classes.dex */
public class NativeNetworkConfig implements Parcelable {
    public static final Parcelable.Creator<NativeNetworkConfig> CREATOR = new Parcelable.Creator<NativeNetworkConfig>() { // from class: android.net.NativeNetworkConfig.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public NativeNetworkConfig createFromParcel(Parcel _aidl_source) {
            return NativeNetworkConfig.internalCreateFromParcel(_aidl_source);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public NativeNetworkConfig[] newArray(int _aidl_size) {
            return new NativeNetworkConfig[_aidl_size];
        }
    };
    public final boolean excludeLocalRoutes;
    public final int netId;
    public final int networkType;
    public final int permission;
    public final boolean secure;
    public final int vpnType;

    /* loaded from: classes.dex */
    public static final class Builder {
        private int netId = 0;
        private int networkType = 0;
        private int permission = 0;
        private boolean secure = false;
        private int vpnType = 2;
        private boolean excludeLocalRoutes = false;

        public Builder setNetId(int netId) {
            this.netId = netId;
            return this;
        }

        public Builder setNetworkType(int networkType) {
            this.networkType = networkType;
            return this;
        }

        public Builder setPermission(int permission) {
            this.permission = permission;
            return this;
        }

        public Builder setSecure(boolean secure) {
            this.secure = secure;
            return this;
        }

        public Builder setVpnType(int vpnType) {
            this.vpnType = vpnType;
            return this;
        }

        public Builder setExcludeLocalRoutes(boolean excludeLocalRoutes) {
            this.excludeLocalRoutes = excludeLocalRoutes;
            return this;
        }

        public NativeNetworkConfig build() {
            return new NativeNetworkConfig(this.netId, this.networkType, this.permission, this.secure, this.vpnType, this.excludeLocalRoutes);
        }
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel _aidl_parcel, int _aidl_flag) {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.writeInt(0);
        _aidl_parcel.writeInt(this.netId);
        _aidl_parcel.writeInt(this.networkType);
        _aidl_parcel.writeInt(this.permission);
        _aidl_parcel.writeBoolean(this.secure);
        _aidl_parcel.writeInt(this.vpnType);
        _aidl_parcel.writeBoolean(this.excludeLocalRoutes);
        int _aidl_end_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.setDataPosition(_aidl_start_pos);
        _aidl_parcel.writeInt(_aidl_end_pos - _aidl_start_pos);
        _aidl_parcel.setDataPosition(_aidl_end_pos);
    }

    public NativeNetworkConfig(int netId, int networkType, int permission, boolean secure, int vpnType, boolean excludeLocalRoutes) {
        this.netId = netId;
        this.networkType = networkType;
        this.permission = permission;
        this.secure = secure;
        this.vpnType = vpnType;
        this.excludeLocalRoutes = excludeLocalRoutes;
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[ARITH]}, finally: {[ARITH, CONSTRUCTOR, IF] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [116=8, 117=8, 119=8, 120=6] */
    /* JADX INFO: Access modifiers changed from: private */
    public static NativeNetworkConfig internalCreateFromParcel(Parcel _aidl_parcel) {
        int i;
        Builder _aidl_parcelable_builder = new Builder();
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        int _aidl_parcelable_size = _aidl_parcel.readInt();
        try {
        } finally {
            if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                BadParcelableException badParcelableException = new BadParcelableException("Overflow in the size of parcelable");
            }
            _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
            return _aidl_parcelable_builder.build();
        }
        if (_aidl_parcelable_size >= 4) {
            _aidl_parcelable_builder.build();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                _aidl_parcelable_builder.build();
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
            } else {
                int _aidl_temp_netId = _aidl_parcel.readInt();
                _aidl_parcelable_builder.setNetId(_aidl_temp_netId);
                if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                    _aidl_parcelable_builder.build();
                    if (_aidl_start_pos <= Integer.MAX_VALUE - _aidl_parcelable_size) {
                        _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                        return _aidl_parcelable_builder.build();
                    }
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                int _aidl_temp_networkType = _aidl_parcel.readInt();
                _aidl_parcelable_builder.setNetworkType(_aidl_temp_networkType);
                if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                    _aidl_parcelable_builder.build();
                    if (_aidl_start_pos <= Integer.MAX_VALUE - _aidl_parcelable_size) {
                        _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                        return _aidl_parcelable_builder.build();
                    }
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                int _aidl_temp_permission = _aidl_parcel.readInt();
                _aidl_parcelable_builder.setPermission(_aidl_temp_permission);
                if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                    _aidl_parcelable_builder.build();
                    if (_aidl_start_pos <= Integer.MAX_VALUE - _aidl_parcelable_size) {
                        _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                        return _aidl_parcelable_builder.build();
                    }
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                boolean _aidl_temp_secure = _aidl_parcel.readBoolean();
                _aidl_parcelable_builder.setSecure(_aidl_temp_secure);
                if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                    _aidl_parcelable_builder.build();
                    if (_aidl_start_pos <= Integer.MAX_VALUE - _aidl_parcelable_size) {
                        _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                        return _aidl_parcelable_builder.build();
                    }
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                int _aidl_temp_vpnType = _aidl_parcel.readInt();
                _aidl_parcelable_builder.setVpnType(_aidl_temp_vpnType);
                if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                    _aidl_parcelable_builder.build();
                    if (_aidl_start_pos <= Integer.MAX_VALUE - _aidl_parcelable_size) {
                        _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                        return _aidl_parcelable_builder.build();
                    }
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                boolean _aidl_temp_excludeLocalRoutes = _aidl_parcel.readBoolean();
                _aidl_parcelable_builder.setExcludeLocalRoutes(_aidl_temp_excludeLocalRoutes);
                if (_aidl_start_pos > i) {
                    throw new BadParcelableException(r4);
                }
            }
            _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
            return _aidl_parcelable_builder.build();
        }
        throw new BadParcelableException("Parcelable too small");
    }

    public String toString() {
        StringJoiner _aidl_sj = new StringJoiner(", ", "{", "}");
        _aidl_sj.add("netId: " + this.netId);
        _aidl_sj.add("networkType: " + this.networkType);
        _aidl_sj.add("permission: " + this.permission);
        _aidl_sj.add("secure: " + this.secure);
        _aidl_sj.add("vpnType: " + this.vpnType);
        _aidl_sj.add("excludeLocalRoutes: " + this.excludeLocalRoutes);
        return "android.net.NativeNetworkConfig" + _aidl_sj.toString();
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || !(other instanceof NativeNetworkConfig)) {
            return false;
        }
        NativeNetworkConfig that = (NativeNetworkConfig) other;
        if (Objects.deepEquals(Integer.valueOf(this.netId), Integer.valueOf(that.netId)) && Objects.deepEquals(Integer.valueOf(this.networkType), Integer.valueOf(that.networkType)) && Objects.deepEquals(Integer.valueOf(this.permission), Integer.valueOf(that.permission)) && Objects.deepEquals(Boolean.valueOf(this.secure), Boolean.valueOf(that.secure)) && Objects.deepEquals(Integer.valueOf(this.vpnType), Integer.valueOf(that.vpnType)) && Objects.deepEquals(Boolean.valueOf(this.excludeLocalRoutes), Boolean.valueOf(that.excludeLocalRoutes))) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Arrays.deepHashCode(Arrays.asList(Integer.valueOf(this.netId), Integer.valueOf(this.networkType), Integer.valueOf(this.permission), Boolean.valueOf(this.secure), Integer.valueOf(this.vpnType), Boolean.valueOf(this.excludeLocalRoutes)).toArray());
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}
