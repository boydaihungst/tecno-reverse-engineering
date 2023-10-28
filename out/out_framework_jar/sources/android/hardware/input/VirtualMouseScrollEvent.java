package android.hardware.input;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.Preconditions;
@SystemApi
/* loaded from: classes2.dex */
public final class VirtualMouseScrollEvent implements Parcelable {
    public static final Parcelable.Creator<VirtualMouseScrollEvent> CREATOR = new Parcelable.Creator<VirtualMouseScrollEvent>() { // from class: android.hardware.input.VirtualMouseScrollEvent.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public VirtualMouseScrollEvent createFromParcel(Parcel source) {
            return new VirtualMouseScrollEvent(source);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public VirtualMouseScrollEvent[] newArray(int size) {
            return new VirtualMouseScrollEvent[size];
        }
    };
    private final float mXAxisMovement;
    private final float mYAxisMovement;

    private VirtualMouseScrollEvent(float xAxisMovement, float yAxisMovement) {
        this.mXAxisMovement = xAxisMovement;
        this.mYAxisMovement = yAxisMovement;
    }

    private VirtualMouseScrollEvent(Parcel parcel) {
        this.mXAxisMovement = parcel.readFloat();
        this.mYAxisMovement = parcel.readFloat();
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int parcelableFlags) {
        parcel.writeFloat(this.mXAxisMovement);
        parcel.writeFloat(this.mYAxisMovement);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public float getXAxisMovement() {
        return this.mXAxisMovement;
    }

    public float getYAxisMovement() {
        return this.mYAxisMovement;
    }

    /* loaded from: classes2.dex */
    public static final class Builder {
        private float mXAxisMovement;
        private float mYAxisMovement;

        public VirtualMouseScrollEvent build() {
            return new VirtualMouseScrollEvent(this.mXAxisMovement, this.mYAxisMovement);
        }

        public Builder setXAxisMovement(float xAxisMovement) {
            Preconditions.checkArgumentInRange(xAxisMovement, -1.0f, 1.0f, "xAxisMovement");
            this.mXAxisMovement = xAxisMovement;
            return this;
        }

        public Builder setYAxisMovement(float yAxisMovement) {
            Preconditions.checkArgumentInRange(yAxisMovement, -1.0f, 1.0f, "yAxisMovement");
            this.mYAxisMovement = yAxisMovement;
            return this;
        }
    }
}
