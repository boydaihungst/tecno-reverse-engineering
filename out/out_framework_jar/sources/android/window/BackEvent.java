package android.window;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.RemoteAnimationTarget;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/* loaded from: classes4.dex */
public class BackEvent implements Parcelable {
    public static final Parcelable.Creator<BackEvent> CREATOR = new Parcelable.Creator<BackEvent>() { // from class: android.window.BackEvent.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public BackEvent createFromParcel(Parcel in) {
            return new BackEvent(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public BackEvent[] newArray(int size) {
            return new BackEvent[size];
        }
    };
    public static final int EDGE_LEFT = 0;
    public static final int EDGE_RIGHT = 1;
    private final RemoteAnimationTarget mDepartingAnimationTarget;
    private final float mProgress;
    private final int mSwipeEdge;
    private final float mTouchX;
    private final float mTouchY;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes4.dex */
    public @interface SwipeEdge {
    }

    public BackEvent(float touchX, float touchY, float progress, int swipeEdge, RemoteAnimationTarget departingAnimationTarget) {
        this.mTouchX = touchX;
        this.mTouchY = touchY;
        this.mProgress = progress;
        this.mSwipeEdge = swipeEdge;
        this.mDepartingAnimationTarget = departingAnimationTarget;
    }

    private BackEvent(Parcel in) {
        this.mTouchX = in.readFloat();
        this.mTouchY = in.readFloat();
        this.mProgress = in.readFloat();
        this.mSwipeEdge = in.readInt();
        this.mDepartingAnimationTarget = (RemoteAnimationTarget) in.readTypedObject(RemoteAnimationTarget.CREATOR);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(this.mTouchX);
        dest.writeFloat(this.mTouchY);
        dest.writeFloat(this.mProgress);
        dest.writeInt(this.mSwipeEdge);
        dest.writeTypedObject(this.mDepartingAnimationTarget, flags);
    }

    public float getProgress() {
        return this.mProgress;
    }

    public float getTouchX() {
        return this.mTouchX;
    }

    public float getTouchY() {
        return this.mTouchY;
    }

    public int getSwipeEdge() {
        return this.mSwipeEdge;
    }

    public RemoteAnimationTarget getDepartingAnimationTarget() {
        return this.mDepartingAnimationTarget;
    }

    public String toString() {
        return "BackEvent{mTouchX=" + this.mTouchX + ", mTouchY=" + this.mTouchY + ", mProgress=" + this.mProgress + ", mSwipeEdge" + this.mSwipeEdge + ", mDepartingAnimationTarget" + this.mDepartingAnimationTarget + "}";
    }
}
