package android.view;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.IMultiTaskRemoteAnimationRunner;
/* loaded from: classes3.dex */
public class MultiTaskRemoteAnimationAdapter implements Parcelable {
    public static final Parcelable.Creator<MultiTaskRemoteAnimationAdapter> CREATOR = new Parcelable.Creator<MultiTaskRemoteAnimationAdapter>() { // from class: android.view.MultiTaskRemoteAnimationAdapter.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public MultiTaskRemoteAnimationAdapter createFromParcel(Parcel in) {
            return new MultiTaskRemoteAnimationAdapter(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public MultiTaskRemoteAnimationAdapter[] newArray(int size) {
            return new MultiTaskRemoteAnimationAdapter[size];
        }
    };
    private int mCallingPid;
    private int mCallingUid;
    private final IMultiTaskRemoteAnimationRunner mRunner;

    public MultiTaskRemoteAnimationAdapter(IMultiTaskRemoteAnimationRunner runner) {
        this.mRunner = runner;
    }

    public MultiTaskRemoteAnimationAdapter(Parcel in) {
        this.mRunner = IMultiTaskRemoteAnimationRunner.Stub.asInterface(in.readStrongBinder());
    }

    public IMultiTaskRemoteAnimationRunner getRunner() {
        return this.mRunner;
    }

    public void setCallingPidUid(int pid, int uid) {
        this.mCallingPid = pid;
        this.mCallingUid = uid;
    }

    public int getCallingPid() {
        return this.mCallingPid;
    }

    public int getCallingUid() {
        return this.mCallingUid;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStrongInterface(this.mRunner);
    }
}
