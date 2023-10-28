package android.net.networkstack.aidl;

import android.net.LinkProperties;
import android.net.NetworkAgentConfig;
import android.net.NetworkCapabilities;
import android.os.BadParcelableException;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;
/* loaded from: classes.dex */
public class NetworkMonitorParameters implements Parcelable {
    public static final Parcelable.Creator<NetworkMonitorParameters> CREATOR = new Parcelable.Creator<NetworkMonitorParameters>() { // from class: android.net.networkstack.aidl.NetworkMonitorParameters.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public NetworkMonitorParameters createFromParcel(Parcel _aidl_source) {
            NetworkMonitorParameters _aidl_out = new NetworkMonitorParameters();
            _aidl_out.readFromParcel(_aidl_source);
            return _aidl_out;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public NetworkMonitorParameters[] newArray(int _aidl_size) {
            return new NetworkMonitorParameters[_aidl_size];
        }
    };
    public LinkProperties linkProperties;
    public NetworkAgentConfig networkAgentConfig;
    public NetworkCapabilities networkCapabilities;

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel _aidl_parcel, int _aidl_flag) {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.writeInt(0);
        _aidl_parcel.writeTypedObject(this.networkAgentConfig, _aidl_flag);
        _aidl_parcel.writeTypedObject(this.networkCapabilities, _aidl_flag);
        _aidl_parcel.writeTypedObject(this.linkProperties, _aidl_flag);
        int _aidl_end_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.setDataPosition(_aidl_start_pos);
        _aidl_parcel.writeInt(_aidl_end_pos - _aidl_start_pos);
        _aidl_parcel.setDataPosition(_aidl_end_pos);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [47=6, 48=5, 50=5] */
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
            this.networkAgentConfig = (NetworkAgentConfig) _aidl_parcel.readTypedObject(NetworkAgentConfig.CREATOR);
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.networkCapabilities = (NetworkCapabilities) _aidl_parcel.readTypedObject(NetworkCapabilities.CREATOR);
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.linkProperties = (LinkProperties) _aidl_parcel.readTypedObject(LinkProperties.CREATOR);
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
        _aidl_sj.add("networkAgentConfig: " + Objects.toString(this.networkAgentConfig));
        _aidl_sj.add("networkCapabilities: " + Objects.toString(this.networkCapabilities));
        _aidl_sj.add("linkProperties: " + Objects.toString(this.linkProperties));
        return "android.net.networkstack.aidl.NetworkMonitorParameters" + _aidl_sj.toString();
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || !(other instanceof NetworkMonitorParameters)) {
            return false;
        }
        NetworkMonitorParameters that = (NetworkMonitorParameters) other;
        if (Objects.deepEquals(this.networkAgentConfig, that.networkAgentConfig) && Objects.deepEquals(this.networkCapabilities, that.networkCapabilities) && Objects.deepEquals(this.linkProperties, that.linkProperties)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Arrays.deepHashCode(Arrays.asList(this.networkAgentConfig, this.networkCapabilities, this.linkProperties).toArray());
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        int _mask = 0 | describeContents(this.networkAgentConfig);
        return _mask | describeContents(this.networkCapabilities) | describeContents(this.linkProperties);
    }

    private int describeContents(Object _v) {
        if (_v == null || !(_v instanceof Parcelable)) {
            return 0;
        }
        return ((Parcelable) _v).describeContents();
    }
}
