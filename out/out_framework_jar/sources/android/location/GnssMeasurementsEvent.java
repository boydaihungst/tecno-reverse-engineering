package android.location;

import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
/* loaded from: classes2.dex */
public final class GnssMeasurementsEvent implements Parcelable {
    public static final Parcelable.Creator<GnssMeasurementsEvent> CREATOR = new Parcelable.Creator<GnssMeasurementsEvent>() { // from class: android.location.GnssMeasurementsEvent.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public GnssMeasurementsEvent createFromParcel(Parcel in) {
            GnssClock clock = (GnssClock) in.readParcelable(getClass().getClassLoader(), GnssClock.class);
            List<GnssMeasurement> measurements = in.createTypedArrayList(GnssMeasurement.CREATOR);
            List<GnssAutomaticGainControl> agcs = in.createTypedArrayList(GnssAutomaticGainControl.CREATOR);
            return new GnssMeasurementsEvent(clock, measurements, agcs);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public GnssMeasurementsEvent[] newArray(int size) {
            return new GnssMeasurementsEvent[size];
        }
    };
    private final GnssClock mClock;
    private final List<GnssAutomaticGainControl> mGnssAgcs;
    private final List<GnssMeasurement> mMeasurements;

    /* loaded from: classes2.dex */
    public static abstract class Callback {
        @Deprecated
        public static final int STATUS_LOCATION_DISABLED = 2;
        @Deprecated
        public static final int STATUS_NOT_ALLOWED = 3;
        @Deprecated
        public static final int STATUS_NOT_SUPPORTED = 0;
        @Deprecated
        public static final int STATUS_READY = 1;

        @Retention(RetentionPolicy.SOURCE)
        @Deprecated
        /* loaded from: classes2.dex */
        public @interface GnssMeasurementsStatus {
        }

        public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs) {
        }

        @Deprecated
        public void onStatusChanged(int status) {
        }
    }

    private GnssMeasurementsEvent(GnssClock clock, List<GnssMeasurement> measurements, List<GnssAutomaticGainControl> agcs) {
        this.mMeasurements = measurements;
        this.mGnssAgcs = agcs;
        this.mClock = clock;
    }

    public GnssClock getClock() {
        return this.mClock;
    }

    public Collection<GnssMeasurement> getMeasurements() {
        return this.mMeasurements;
    }

    public Collection<GnssAutomaticGainControl> getGnssAutomaticGainControls() {
        return this.mGnssAgcs;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeParcelable(this.mClock, flags);
        parcel.writeTypedList(this.mMeasurements);
        parcel.writeTypedList(this.mGnssAgcs);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder("GnssMeasurementsEvent[");
        builder.append(this.mClock);
        builder.append(' ').append(this.mMeasurements.toString());
        builder.append(' ').append(this.mGnssAgcs.toString());
        builder.append(NavigationBarInflaterView.SIZE_MOD_END);
        return builder.toString();
    }

    /* loaded from: classes2.dex */
    public static final class Builder {
        private GnssClock mClock;
        private List<GnssAutomaticGainControl> mGnssAgcs;
        private List<GnssMeasurement> mMeasurements;

        public Builder() {
            this.mClock = new GnssClock();
            this.mMeasurements = new ArrayList();
            this.mGnssAgcs = new ArrayList();
        }

        public Builder(GnssMeasurementsEvent event) {
            this.mClock = event.getClock();
            this.mMeasurements = (List) event.getMeasurements();
            this.mGnssAgcs = (List) event.getGnssAutomaticGainControls();
        }

        public Builder setClock(GnssClock clock) {
            Preconditions.checkNotNull(clock);
            this.mClock = clock;
            return this;
        }

        public Builder setMeasurements(GnssMeasurement... measurements) {
            this.mMeasurements = measurements == null ? Collections.emptyList() : Arrays.asList(measurements);
            return this;
        }

        public Builder setMeasurements(Collection<GnssMeasurement> measurements) {
            this.mMeasurements = new ArrayList(measurements);
            return this;
        }

        public Builder setGnssAutomaticGainControls(GnssAutomaticGainControl... agcs) {
            this.mGnssAgcs = agcs == null ? Collections.emptyList() : Arrays.asList(agcs);
            return this;
        }

        public Builder setGnssAutomaticGainControls(Collection<GnssAutomaticGainControl> agcs) {
            this.mGnssAgcs = new ArrayList(agcs);
            return this;
        }

        public GnssMeasurementsEvent build() {
            return new GnssMeasurementsEvent(this.mClock, this.mMeasurements, this.mGnssAgcs);
        }
    }
}
