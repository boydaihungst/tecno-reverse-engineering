package android.accessibilityservice;

import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/* loaded from: classes.dex */
public final class MagnificationConfig implements Parcelable {
    public static final Parcelable.Creator<MagnificationConfig> CREATOR = new Parcelable.Creator<MagnificationConfig>() { // from class: android.accessibilityservice.MagnificationConfig.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public MagnificationConfig createFromParcel(Parcel parcel) {
            return new MagnificationConfig(parcel);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public MagnificationConfig[] newArray(int size) {
            return new MagnificationConfig[size];
        }
    };
    public static final int MAGNIFICATION_MODE_DEFAULT = 0;
    public static final int MAGNIFICATION_MODE_FULLSCREEN = 1;
    public static final int MAGNIFICATION_MODE_WINDOW = 2;
    private float mCenterX;
    private float mCenterY;
    private int mMode;
    private float mScale;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    @interface MagnificationMode {
    }

    private MagnificationConfig() {
        this.mMode = 0;
        this.mScale = Float.NaN;
        this.mCenterX = Float.NaN;
        this.mCenterY = Float.NaN;
    }

    private MagnificationConfig(Parcel parcel) {
        this.mMode = 0;
        this.mScale = Float.NaN;
        this.mCenterX = Float.NaN;
        this.mCenterY = Float.NaN;
        this.mMode = parcel.readInt();
        this.mScale = parcel.readFloat();
        this.mCenterX = parcel.readFloat();
        this.mCenterY = parcel.readFloat();
    }

    public int getMode() {
        return this.mMode;
    }

    public float getScale() {
        return this.mScale;
    }

    public float getCenterX() {
        return this.mCenterX;
    }

    public float getCenterY() {
        return this.mCenterY;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("MagnificationConfig[");
        stringBuilder.append("mode: ").append(getMode());
        stringBuilder.append(", ");
        stringBuilder.append("scale: ").append(getScale());
        stringBuilder.append(", ");
        stringBuilder.append("centerX: ").append(getCenterX());
        stringBuilder.append(", ");
        stringBuilder.append("centerY: ").append(getCenterY());
        stringBuilder.append("] ");
        return stringBuilder.toString();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(this.mMode);
        parcel.writeFloat(this.mScale);
        parcel.writeFloat(this.mCenterX);
        parcel.writeFloat(this.mCenterY);
    }

    /* loaded from: classes.dex */
    public static final class Builder {
        private int mMode = 0;
        private float mScale = Float.NaN;
        private float mCenterX = Float.NaN;
        private float mCenterY = Float.NaN;

        public Builder setMode(int mode) {
            this.mMode = mode;
            return this;
        }

        public Builder setScale(float scale) {
            this.mScale = scale;
            return this;
        }

        public Builder setCenterX(float centerX) {
            this.mCenterX = centerX;
            return this;
        }

        public Builder setCenterY(float centerY) {
            this.mCenterY = centerY;
            return this;
        }

        public MagnificationConfig build() {
            MagnificationConfig magnificationConfig = new MagnificationConfig();
            magnificationConfig.mMode = this.mMode;
            magnificationConfig.mScale = this.mScale;
            magnificationConfig.mCenterX = this.mCenterX;
            magnificationConfig.mCenterY = this.mCenterY;
            return magnificationConfig;
        }
    }
}
