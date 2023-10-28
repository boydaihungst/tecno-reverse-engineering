package android.hardware.biometrics;

import android.graphics.Rect;
import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.os.Parcel;
import android.os.Parcelable;
/* loaded from: classes.dex */
public class SensorLocationInternal implements Parcelable {
    public final String displayId;
    public final int sensorLocationX;
    public final int sensorLocationY;
    public final int sensorRadius;
    public static final SensorLocationInternal DEFAULT = new SensorLocationInternal("", 0, 0, 0);
    public static final Parcelable.Creator<SensorLocationInternal> CREATOR = new Parcelable.Creator<SensorLocationInternal>() { // from class: android.hardware.biometrics.SensorLocationInternal.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SensorLocationInternal createFromParcel(Parcel in) {
            return new SensorLocationInternal(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SensorLocationInternal[] newArray(int size) {
            return new SensorLocationInternal[size];
        }
    };

    public SensorLocationInternal(String displayId, int sensorLocationX, int sensorLocationY, int sensorRadius) {
        this.displayId = displayId != null ? displayId : "";
        this.sensorLocationX = sensorLocationX;
        this.sensorLocationY = sensorLocationY;
        this.sensorRadius = sensorRadius;
    }

    protected SensorLocationInternal(Parcel in) {
        this.displayId = in.readString16NoHelper();
        this.sensorLocationX = in.readInt();
        this.sensorLocationY = in.readInt();
        this.sensorRadius = in.readInt();
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.displayId);
        dest.writeInt(this.sensorLocationX);
        dest.writeInt(this.sensorLocationY);
        dest.writeInt(this.sensorRadius);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public String toString() {
        return "[id: " + this.displayId + ", x: " + this.sensorLocationX + ", y: " + this.sensorLocationY + ", r: " + this.sensorRadius + NavigationBarInflaterView.SIZE_MOD_END;
    }

    public Rect getRect() {
        int i = this.sensorLocationX;
        int i2 = this.sensorRadius;
        int i3 = this.sensorLocationY;
        return new Rect(i - i2, i3 - i2, i + i2, i3 + i2);
    }
}
