package android.location;

import android.annotation.SystemApi;
import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.os.Parcel;
import android.os.Parcelable;
import android.security.keystore.KeyProperties;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
/* loaded from: classes2.dex */
public final class GnssCapabilities implements Parcelable {
    public static final Parcelable.Creator<GnssCapabilities> CREATOR = new Parcelable.Creator<GnssCapabilities>() { // from class: android.location.GnssCapabilities.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public GnssCapabilities createFromParcel(Parcel in) {
            return new GnssCapabilities(in.readInt(), in.readInt(), in.readInt());
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public GnssCapabilities[] newArray(int size) {
            return new GnssCapabilities[size];
        }
    };
    public static final int SUB_HAL_MEASUREMENT_CORRECTIONS_CAPABILITY_EXCESS_PATH_LENGTH = 2;
    public static final int SUB_HAL_MEASUREMENT_CORRECTIONS_CAPABILITY_LOS_SATS = 1;
    public static final int SUB_HAL_MEASUREMENT_CORRECTIONS_CAPABILITY_REFLECTING_PLANE = 4;
    public static final int SUB_HAL_POWER_CAPABILITY_MULTIBAND_ACQUISITION = 16;
    public static final int SUB_HAL_POWER_CAPABILITY_MULTIBAND_TRACKING = 4;
    public static final int SUB_HAL_POWER_CAPABILITY_OTHER_MODES = 32;
    public static final int SUB_HAL_POWER_CAPABILITY_SINGLEBAND_ACQUISITION = 8;
    public static final int SUB_HAL_POWER_CAPABILITY_SINGLEBAND_TRACKING = 2;
    public static final int SUB_HAL_POWER_CAPABILITY_TOTAL = 1;
    public static final int TOP_HAL_CAPABILITY_ANTENNA_INFO = 2048;
    public static final int TOP_HAL_CAPABILITY_CORRELATION_VECTOR = 4096;
    public static final int TOP_HAL_CAPABILITY_GEOFENCING = 32;
    public static final int TOP_HAL_CAPABILITY_LOW_POWER_MODE = 256;
    public static final int TOP_HAL_CAPABILITY_MEASUREMENTS = 64;
    public static final int TOP_HAL_CAPABILITY_MEASUREMENT_CORRECTIONS = 1024;
    public static final int TOP_HAL_CAPABILITY_MEASUREMENT_CORRECTIONS_FOR_DRIVING = 16384;
    public static final int TOP_HAL_CAPABILITY_MSA = 4;
    public static final int TOP_HAL_CAPABILITY_MSB = 2;
    public static final int TOP_HAL_CAPABILITY_NAV_MESSAGES = 128;
    public static final int TOP_HAL_CAPABILITY_ON_DEMAND_TIME = 16;
    public static final int TOP_HAL_CAPABILITY_SATELLITE_BLOCKLIST = 512;
    public static final int TOP_HAL_CAPABILITY_SATELLITE_PVT = 8192;
    public static final int TOP_HAL_CAPABILITY_SCHEDULING = 1;
    public static final int TOP_HAL_CAPABILITY_SINGLE_SHOT = 8;
    private final int mMeasurementCorrectionsFlags;
    private final int mPowerFlags;
    private final int mTopFlags;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface SubHalMeasurementCorrectionsCapabilityFlags {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface SubHalPowerCapabilityFlags {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface TopHalCapabilityFlags {
    }

    public static GnssCapabilities empty() {
        return new GnssCapabilities(0, 0, 0);
    }

    private GnssCapabilities(int topFlags, int measurementCorrectionsFlags, int powerFlags) {
        this.mTopFlags = topFlags;
        this.mMeasurementCorrectionsFlags = measurementCorrectionsFlags;
        this.mPowerFlags = powerFlags;
    }

    public GnssCapabilities withTopHalFlags(int flags) {
        if (this.mTopFlags == flags) {
            return this;
        }
        return new GnssCapabilities(flags, this.mMeasurementCorrectionsFlags, this.mPowerFlags);
    }

    public GnssCapabilities withSubHalMeasurementCorrectionsFlags(int flags) {
        if (this.mMeasurementCorrectionsFlags == flags) {
            return this;
        }
        return new GnssCapabilities(this.mTopFlags, flags, this.mPowerFlags);
    }

    public GnssCapabilities withSubHalPowerFlags(int flags) {
        if (this.mPowerFlags == flags) {
            return this;
        }
        return new GnssCapabilities(this.mTopFlags, this.mMeasurementCorrectionsFlags, flags);
    }

    public boolean hasScheduling() {
        return (this.mTopFlags & 1) != 0;
    }

    public boolean hasMsb() {
        return (this.mTopFlags & 2) != 0;
    }

    public boolean hasMsa() {
        return (this.mTopFlags & 4) != 0;
    }

    public boolean hasSingleShot() {
        return (this.mTopFlags & 8) != 0;
    }

    public boolean hasOnDemandTime() {
        return (this.mTopFlags & 16) != 0;
    }

    @SystemApi
    public boolean hasGeofencing() {
        return (this.mTopFlags & 32) != 0;
    }

    public boolean hasMeasurements() {
        return (this.mTopFlags & 64) != 0;
    }

    @SystemApi
    @Deprecated
    public boolean hasNavMessages() {
        return hasNavigationMessages();
    }

    public boolean hasNavigationMessages() {
        return (this.mTopFlags & 128) != 0;
    }

    @SystemApi
    public boolean hasLowPowerMode() {
        return (this.mTopFlags & 256) != 0;
    }

    @SystemApi
    @Deprecated
    public boolean hasSatelliteBlacklist() {
        return (this.mTopFlags & 512) != 0;
    }

    @SystemApi
    public boolean hasSatelliteBlocklist() {
        return (this.mTopFlags & 512) != 0;
    }

    @SystemApi
    public boolean hasSatellitePvt() {
        return (this.mTopFlags & 8192) != 0;
    }

    @SystemApi
    public boolean hasMeasurementCorrections() {
        return (this.mTopFlags & 1024) != 0;
    }

    @Deprecated
    public boolean hasGnssAntennaInfo() {
        return hasAntennaInfo();
    }

    public boolean hasAntennaInfo() {
        return (this.mTopFlags & 2048) != 0;
    }

    @SystemApi
    public boolean hasMeasurementCorrelationVectors() {
        return (this.mTopFlags & 4096) != 0;
    }

    @SystemApi
    public boolean hasMeasurementCorrectionsForDriving() {
        return (this.mTopFlags & 16384) != 0;
    }

    @SystemApi
    public boolean hasMeasurementCorrectionsLosSats() {
        return (this.mMeasurementCorrectionsFlags & 1) != 0;
    }

    @SystemApi
    public boolean hasMeasurementCorrectionsExcessPathLength() {
        return (this.mMeasurementCorrectionsFlags & 2) != 0;
    }

    @SystemApi
    public boolean hasMeasurementCorrectionsReflectingPane() {
        return hasMeasurementCorrectionsReflectingPlane();
    }

    @SystemApi
    public boolean hasMeasurementCorrectionsReflectingPlane() {
        return (this.mMeasurementCorrectionsFlags & 4) != 0;
    }

    public boolean hasPowerTotal() {
        return (this.mPowerFlags & 1) != 0;
    }

    public boolean hasPowerSinglebandTracking() {
        return (this.mPowerFlags & 2) != 0;
    }

    public boolean hasPowerMultibandTracking() {
        return (this.mPowerFlags & 4) != 0;
    }

    public boolean hasPowerSinglebandAcquisition() {
        return (this.mPowerFlags & 8) != 0;
    }

    public boolean hasPowerMultibandAcquisition() {
        return (this.mPowerFlags & 16) != 0;
    }

    public boolean hasPowerOtherModes() {
        return (this.mPowerFlags & 32) != 0;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof GnssCapabilities) {
            GnssCapabilities that = (GnssCapabilities) o;
            return this.mTopFlags == that.mTopFlags && this.mMeasurementCorrectionsFlags == that.mMeasurementCorrectionsFlags && this.mPowerFlags == that.mPowerFlags;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mTopFlags), Integer.valueOf(this.mMeasurementCorrectionsFlags), Integer.valueOf(this.mPowerFlags));
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(this.mTopFlags);
        parcel.writeInt(this.mMeasurementCorrectionsFlags);
        parcel.writeInt(this.mPowerFlags);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(NavigationBarInflaterView.SIZE_MOD_START);
        if (hasScheduling()) {
            builder.append("SCHEDULING ");
        }
        if (hasMsb()) {
            builder.append("MSB ");
        }
        if (hasMsa()) {
            builder.append("MSA ");
        }
        if (hasSingleShot()) {
            builder.append("SINGLE_SHOT ");
        }
        if (hasOnDemandTime()) {
            builder.append("ON_DEMAND_TIME ");
        }
        if (hasGeofencing()) {
            builder.append("GEOFENCING ");
        }
        if (hasMeasurementCorrections()) {
            builder.append("MEASUREMENTS ");
        }
        if (hasNavigationMessages()) {
            builder.append("NAVIGATION_MESSAGES ");
        }
        if (hasLowPowerMode()) {
            builder.append("LOW_POWER_MODE ");
        }
        if (hasSatelliteBlocklist()) {
            builder.append("SATELLITE_BLOCKLIST ");
        }
        if (hasSatellitePvt()) {
            builder.append("SATELLITE_PVT ");
        }
        if (hasMeasurementCorrections()) {
            builder.append("MEASUREMENT_CORRECTIONS ");
        }
        if (hasAntennaInfo()) {
            builder.append("ANTENNA_INFO ");
        }
        if (hasMeasurementCorrelationVectors()) {
            builder.append("MEASUREMENT_CORRELATION_VECTORS ");
        }
        if (hasMeasurementCorrectionsForDriving()) {
            builder.append("MEASUREMENT_CORRECTIONS_FOR_DRIVING ");
        }
        if (hasMeasurementCorrectionsLosSats()) {
            builder.append("LOS_SATS ");
        }
        if (hasMeasurementCorrectionsExcessPathLength()) {
            builder.append("EXCESS_PATH_LENGTH ");
        }
        if (hasMeasurementCorrectionsReflectingPlane()) {
            builder.append("REFLECTING_PLANE ");
        }
        if (hasPowerTotal()) {
            builder.append("TOTAL_POWER ");
        }
        if (hasPowerSinglebandTracking()) {
            builder.append("SINGLEBAND_TRACKING_POWER ");
        }
        if (hasPowerMultibandTracking()) {
            builder.append("MULTIBAND_TRACKING_POWER ");
        }
        if (hasPowerSinglebandAcquisition()) {
            builder.append("SINGLEBAND_ACQUISITION_POWER ");
        }
        if (hasPowerMultibandAcquisition()) {
            builder.append("MULTIBAND_ACQUISITION_POWER ");
        }
        if (hasPowerOtherModes()) {
            builder.append("OTHER_MODES_POWER ");
        }
        if (builder.length() > 1) {
            builder.setLength(builder.length() - 1);
        } else {
            builder.append(KeyProperties.DIGEST_NONE);
        }
        builder.append(NavigationBarInflaterView.SIZE_MOD_END);
        return builder.toString();
    }

    /* loaded from: classes2.dex */
    public static final class Builder {
        private int mMeasurementCorrectionsFlags;
        private int mPowerFlags;
        private int mTopFlags;

        public Builder() {
            this.mTopFlags = 0;
            this.mMeasurementCorrectionsFlags = 0;
            this.mPowerFlags = 0;
        }

        public Builder(GnssCapabilities capabilities) {
            this.mTopFlags = capabilities.mTopFlags;
            this.mMeasurementCorrectionsFlags = capabilities.mMeasurementCorrectionsFlags;
            this.mPowerFlags = capabilities.mPowerFlags;
        }

        public Builder setHasScheduling(boolean capable) {
            this.mTopFlags = setFlag(this.mTopFlags, 1, capable);
            return this;
        }

        public Builder setHasMsb(boolean capable) {
            this.mTopFlags = setFlag(this.mTopFlags, 2, capable);
            return this;
        }

        public Builder setHasMsa(boolean capable) {
            this.mTopFlags = setFlag(this.mTopFlags, 4, capable);
            return this;
        }

        public Builder setHasSingleShot(boolean capable) {
            this.mTopFlags = setFlag(this.mTopFlags, 8, capable);
            return this;
        }

        public Builder setHasOnDemandTime(boolean capable) {
            this.mTopFlags = setFlag(this.mTopFlags, 16, capable);
            return this;
        }

        @SystemApi
        public Builder setHasGeofencing(boolean capable) {
            this.mTopFlags = setFlag(this.mTopFlags, 32, capable);
            return this;
        }

        public Builder setHasMeasurements(boolean capable) {
            this.mTopFlags = setFlag(this.mTopFlags, 64, capable);
            return this;
        }

        public Builder setHasNavigationMessages(boolean capable) {
            this.mTopFlags = setFlag(this.mTopFlags, 128, capable);
            return this;
        }

        @SystemApi
        public Builder setHasLowPowerMode(boolean capable) {
            this.mTopFlags = setFlag(this.mTopFlags, 256, capable);
            return this;
        }

        @SystemApi
        public Builder setHasSatelliteBlocklist(boolean capable) {
            this.mTopFlags = setFlag(this.mTopFlags, 512, capable);
            return this;
        }

        @SystemApi
        public Builder setHasSatellitePvt(boolean capable) {
            this.mTopFlags = setFlag(this.mTopFlags, 8192, capable);
            return this;
        }

        @SystemApi
        public Builder setHasMeasurementCorrections(boolean capable) {
            this.mTopFlags = setFlag(this.mTopFlags, 1024, capable);
            return this;
        }

        public Builder setHasAntennaInfo(boolean capable) {
            this.mTopFlags = setFlag(this.mTopFlags, 2048, capable);
            return this;
        }

        @SystemApi
        public Builder setHasMeasurementCorrelationVectors(boolean capable) {
            this.mTopFlags = setFlag(this.mTopFlags, 4096, capable);
            return this;
        }

        @SystemApi
        public Builder setHasMeasurementCorrectionsForDriving(boolean capable) {
            this.mTopFlags = setFlag(this.mTopFlags, 16384, capable);
            return this;
        }

        @SystemApi
        public Builder setHasMeasurementCorrectionsLosSats(boolean capable) {
            this.mMeasurementCorrectionsFlags = setFlag(this.mMeasurementCorrectionsFlags, 1, capable);
            return this;
        }

        @SystemApi
        public Builder setHasMeasurementCorrectionsExcessPathLength(boolean capable) {
            this.mMeasurementCorrectionsFlags = setFlag(this.mMeasurementCorrectionsFlags, 2, capable);
            return this;
        }

        @SystemApi
        public Builder setHasMeasurementCorrectionsReflectingPlane(boolean capable) {
            this.mMeasurementCorrectionsFlags = setFlag(this.mMeasurementCorrectionsFlags, 4, capable);
            return this;
        }

        public Builder setHasPowerTotal(boolean capable) {
            this.mPowerFlags = setFlag(this.mPowerFlags, 1, capable);
            return this;
        }

        public Builder setHasPowerSinglebandTracking(boolean capable) {
            this.mPowerFlags = setFlag(this.mPowerFlags, 2, capable);
            return this;
        }

        public Builder setHasPowerMultibandTracking(boolean capable) {
            this.mPowerFlags = setFlag(this.mPowerFlags, 4, capable);
            return this;
        }

        public Builder setHasPowerSinglebandAcquisition(boolean capable) {
            this.mPowerFlags = setFlag(this.mPowerFlags, 8, capable);
            return this;
        }

        public Builder setHasPowerMultibandAcquisition(boolean capable) {
            this.mPowerFlags = setFlag(this.mPowerFlags, 16, capable);
            return this;
        }

        public Builder setHasPowerOtherModes(boolean capable) {
            this.mPowerFlags = setFlag(this.mPowerFlags, 32, capable);
            return this;
        }

        public GnssCapabilities build() {
            return new GnssCapabilities(this.mTopFlags, this.mMeasurementCorrectionsFlags, this.mPowerFlags);
        }

        private static int setFlag(int value, int flag, boolean set) {
            if (set) {
                return value | flag;
            }
            return (~flag) & value;
        }
    }
}
