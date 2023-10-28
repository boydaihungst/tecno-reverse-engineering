package android.window;

import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.SurfaceControl;
/* loaded from: classes4.dex */
public final class StartingWindowRemovalInfo implements Parcelable {
    public static final Parcelable.Creator<StartingWindowRemovalInfo> CREATOR = new Parcelable.Creator<StartingWindowRemovalInfo>() { // from class: android.window.StartingWindowRemovalInfo.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public StartingWindowRemovalInfo createFromParcel(Parcel source) {
            return new StartingWindowRemovalInfo(source);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public StartingWindowRemovalInfo[] newArray(int size) {
            return new StartingWindowRemovalInfo[size];
        }
    };
    public boolean deferRemoveForIme;
    public Rect mainFrame;
    public boolean playRevealAnimation;
    public int taskId;
    public SurfaceControl windowAnimationLeash;

    public StartingWindowRemovalInfo() {
    }

    private StartingWindowRemovalInfo(Parcel source) {
        readFromParcel(source);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    void readFromParcel(Parcel source) {
        this.taskId = source.readInt();
        this.windowAnimationLeash = (SurfaceControl) source.readTypedObject(SurfaceControl.CREATOR);
        this.mainFrame = (Rect) source.readTypedObject(Rect.CREATOR);
        this.playRevealAnimation = source.readBoolean();
        this.deferRemoveForIme = source.readBoolean();
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.taskId);
        dest.writeTypedObject(this.windowAnimationLeash, flags);
        dest.writeTypedObject(this.mainFrame, flags);
        dest.writeBoolean(this.playRevealAnimation);
        dest.writeBoolean(this.deferRemoveForIme);
    }

    public String toString() {
        return "StartingWindowRemovalInfo{taskId=" + this.taskId + " frame=" + this.mainFrame + " playRevealAnimation=" + this.playRevealAnimation + " deferRemoveForIme=" + this.deferRemoveForIme + "}";
    }
}
