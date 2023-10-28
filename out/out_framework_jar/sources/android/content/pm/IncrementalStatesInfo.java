package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;
/* loaded from: classes.dex */
public class IncrementalStatesInfo implements Parcelable {
    public static final Parcelable.Creator<IncrementalStatesInfo> CREATOR = new Parcelable.Creator<IncrementalStatesInfo>() { // from class: android.content.pm.IncrementalStatesInfo.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public IncrementalStatesInfo createFromParcel(Parcel source) {
            return new IncrementalStatesInfo(source);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public IncrementalStatesInfo[] newArray(int size) {
            return new IncrementalStatesInfo[size];
        }
    };
    private boolean mIsLoading;
    private float mProgress;

    public IncrementalStatesInfo(boolean isLoading, float progress) {
        this.mIsLoading = isLoading;
        this.mProgress = progress;
    }

    private IncrementalStatesInfo(Parcel source) {
        this.mIsLoading = source.readBoolean();
        this.mProgress = source.readFloat();
    }

    public boolean isLoading() {
        return this.mIsLoading;
    }

    public float getProgress() {
        return this.mProgress;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBoolean(this.mIsLoading);
        dest.writeFloat(this.mProgress);
    }
}
