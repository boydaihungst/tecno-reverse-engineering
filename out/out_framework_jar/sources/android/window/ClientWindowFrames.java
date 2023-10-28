package android.window;

import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
/* loaded from: classes4.dex */
public class ClientWindowFrames implements Parcelable {
    public static final Parcelable.Creator<ClientWindowFrames> CREATOR = new Parcelable.Creator<ClientWindowFrames>() { // from class: android.window.ClientWindowFrames.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ClientWindowFrames createFromParcel(Parcel in) {
            return new ClientWindowFrames(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ClientWindowFrames[] newArray(int size) {
            return new ClientWindowFrames[size];
        }
    };
    public final Rect displayFrame;
    public final Rect frame;
    public boolean isParentFrameClippedByDisplayCutout;
    public final Rect parentFrame;

    public ClientWindowFrames() {
        this.frame = new Rect();
        this.displayFrame = new Rect();
        this.parentFrame = new Rect();
    }

    public ClientWindowFrames(ClientWindowFrames other) {
        Rect rect = new Rect();
        this.frame = rect;
        Rect rect2 = new Rect();
        this.displayFrame = rect2;
        Rect rect3 = new Rect();
        this.parentFrame = rect3;
        rect.set(other.frame);
        rect2.set(other.displayFrame);
        rect3.set(other.parentFrame);
        this.isParentFrameClippedByDisplayCutout = other.isParentFrameClippedByDisplayCutout;
    }

    private ClientWindowFrames(Parcel in) {
        this.frame = new Rect();
        this.displayFrame = new Rect();
        this.parentFrame = new Rect();
        readFromParcel(in);
    }

    public void readFromParcel(Parcel in) {
        this.frame.readFromParcel(in);
        this.displayFrame.readFromParcel(in);
        this.parentFrame.readFromParcel(in);
        this.isParentFrameClippedByDisplayCutout = in.readBoolean();
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        this.frame.writeToParcel(dest, flags);
        this.displayFrame.writeToParcel(dest, flags);
        this.parentFrame.writeToParcel(dest, flags);
        dest.writeBoolean(this.isParentFrameClippedByDisplayCutout);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        return "ClientWindowFrames{frame=" + this.frame.toShortString(sb) + " display=" + this.displayFrame.toShortString(sb) + " parentFrame=" + this.parentFrame.toShortString(sb) + " parentClippedByDisplayCutout=" + this.isParentFrameClippedByDisplayCutout + "}";
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}
