package android.hardware.camera2.params;

import android.hardware.camera2.utils.HashCodeHelpers;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
/* loaded from: classes.dex */
public final class DeviceStateSensorOrientationMap {
    public static final long FOLDED = 4;
    public static final long NORMAL = 0;
    private final HashMap<Long, Integer> mDeviceStateOrientationMap = new HashMap<>();
    private final long[] mElements;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface DeviceState {
    }

    public DeviceStateSensorOrientationMap(long[] elements) {
        this.mElements = (long[]) Objects.requireNonNull(elements, "elements must not be null");
        if (elements.length % 2 != 0) {
            throw new IllegalArgumentException("Device state sensor orientation map length " + elements.length + " is not even!");
        }
        for (int i = 0; i < elements.length; i += 2) {
            if (elements[i + 1] % 90 != 0) {
                throw new IllegalArgumentException("Sensor orientation not divisible by 90: " + elements[i + 1]);
            }
            this.mDeviceStateOrientationMap.put(Long.valueOf(elements[i]), Integer.valueOf(Math.toIntExact(elements[i + 1])));
        }
    }

    public int getSensorOrientation(long deviceState) {
        if (!this.mDeviceStateOrientationMap.containsKey(Long.valueOf(deviceState))) {
            throw new IllegalArgumentException("Invalid device state: " + deviceState);
        }
        return this.mDeviceStateOrientationMap.get(Long.valueOf(deviceState)).intValue();
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DeviceStateSensorOrientationMap)) {
            return false;
        }
        DeviceStateSensorOrientationMap other = (DeviceStateSensorOrientationMap) obj;
        return Arrays.equals(this.mElements, other.mElements);
    }

    public int hashCode() {
        return HashCodeHelpers.hashCodeGeneric(this.mElements);
    }
}
