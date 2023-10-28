package android.hardware.health.V2_1;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import com.android.internal.util.FrameworkStatsLog;
import java.util.ArrayList;
import java.util.Objects;
/* loaded from: classes.dex */
public final class HealthInfo {
    public android.hardware.health.V2_0.HealthInfo legacy = new android.hardware.health.V2_0.HealthInfo();
    public int batteryCapacityLevel = 0;
    public long batteryChargeTimeToFullNowSeconds = 0;
    public int batteryFullChargeDesignCapacityUah = 0;

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != HealthInfo.class) {
            return false;
        }
        HealthInfo other = (HealthInfo) otherObject;
        if (HidlSupport.deepEquals(this.legacy, other.legacy) && this.batteryCapacityLevel == other.batteryCapacityLevel && this.batteryChargeTimeToFullNowSeconds == other.batteryChargeTimeToFullNowSeconds && this.batteryFullChargeDesignCapacityUah == other.batteryFullChargeDesignCapacityUah) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.legacy)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.batteryCapacityLevel))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.batteryChargeTimeToFullNowSeconds))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.batteryFullChargeDesignCapacityUah))));
    }

    public final String toString() {
        return "{.legacy = " + this.legacy + ", .batteryCapacityLevel = " + BatteryCapacityLevel.toString(this.batteryCapacityLevel) + ", .batteryChargeTimeToFullNowSeconds = " + this.batteryChargeTimeToFullNowSeconds + ", .batteryFullChargeDesignCapacityUah = " + this.batteryFullChargeDesignCapacityUah + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        HwBlob blob = parcel.readBuffer(136L);
        readEmbeddedFromParcel(parcel, blob, 0L);
    }

    public static final ArrayList<HealthInfo> readVectorFromParcel(HwParcel parcel) {
        ArrayList<HealthInfo> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16L);
        int _hidl_vec_size = _hidl_blob.getInt32(8L);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_MANAGED_PROFILE_MAXIMUM_TIME_OFF, _hidl_blob.handle(), 0L, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            HealthInfo _hidl_vec_element = new HealthInfo();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_MANAGED_PROFILE_MAXIMUM_TIME_OFF);
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        this.legacy.readEmbeddedFromParcel(parcel, _hidl_blob, 0 + _hidl_offset);
        this.batteryCapacityLevel = _hidl_blob.getInt32(112 + _hidl_offset);
        this.batteryChargeTimeToFullNowSeconds = _hidl_blob.getInt64(120 + _hidl_offset);
        this.batteryFullChargeDesignCapacityUah = _hidl_blob.getInt32(128 + _hidl_offset);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob((int) FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_MANAGED_PROFILE_MAXIMUM_TIME_OFF);
        writeEmbeddedToBlob(_hidl_blob, 0L);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<HealthInfo> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8L, _hidl_vec_size);
        _hidl_blob.putBool(12L, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_MANAGED_PROFILE_MAXIMUM_TIME_OFF);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_MANAGED_PROFILE_MAXIMUM_TIME_OFF);
        }
        _hidl_blob.putBlob(0L, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        this.legacy.writeEmbeddedToBlob(_hidl_blob, 0 + _hidl_offset);
        _hidl_blob.putInt32(112 + _hidl_offset, this.batteryCapacityLevel);
        _hidl_blob.putInt64(120 + _hidl_offset, this.batteryChargeTimeToFullNowSeconds);
        _hidl_blob.putInt32(128 + _hidl_offset, this.batteryFullChargeDesignCapacityUah);
    }
}
