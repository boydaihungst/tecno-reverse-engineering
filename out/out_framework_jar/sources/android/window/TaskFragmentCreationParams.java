package android.window;

import android.graphics.Rect;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
/* loaded from: classes4.dex */
public final class TaskFragmentCreationParams implements Parcelable {
    public static final Parcelable.Creator<TaskFragmentCreationParams> CREATOR = new Parcelable.Creator<TaskFragmentCreationParams>() { // from class: android.window.TaskFragmentCreationParams.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TaskFragmentCreationParams createFromParcel(Parcel in) {
            return new TaskFragmentCreationParams(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TaskFragmentCreationParams[] newArray(int size) {
            return new TaskFragmentCreationParams[size];
        }
    };
    private final IBinder mFragmentToken;
    private final Rect mInitialBounds;
    private final TaskFragmentOrganizerToken mOrganizer;
    private final IBinder mOwnerToken;
    private int mWindowingMode;

    private TaskFragmentCreationParams(TaskFragmentOrganizerToken organizer, IBinder fragmentToken, IBinder ownerToken) {
        this.mInitialBounds = new Rect();
        this.mWindowingMode = 0;
        this.mOrganizer = organizer;
        this.mFragmentToken = fragmentToken;
        this.mOwnerToken = ownerToken;
    }

    public TaskFragmentOrganizerToken getOrganizer() {
        return this.mOrganizer;
    }

    public IBinder getFragmentToken() {
        return this.mFragmentToken;
    }

    public IBinder getOwnerToken() {
        return this.mOwnerToken;
    }

    public Rect getInitialBounds() {
        return this.mInitialBounds;
    }

    public int getWindowingMode() {
        return this.mWindowingMode;
    }

    private TaskFragmentCreationParams(Parcel in) {
        Rect rect = new Rect();
        this.mInitialBounds = rect;
        this.mWindowingMode = 0;
        this.mOrganizer = TaskFragmentOrganizerToken.CREATOR.createFromParcel(in);
        this.mFragmentToken = in.readStrongBinder();
        this.mOwnerToken = in.readStrongBinder();
        rect.readFromParcel(in);
        this.mWindowingMode = in.readInt();
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        this.mOrganizer.writeToParcel(dest, flags);
        dest.writeStrongBinder(this.mFragmentToken);
        dest.writeStrongBinder(this.mOwnerToken);
        this.mInitialBounds.writeToParcel(dest, flags);
        dest.writeInt(this.mWindowingMode);
    }

    public String toString() {
        return "TaskFragmentCreationParams{ organizer=" + this.mOrganizer + " fragmentToken=" + this.mFragmentToken + " ownerToken=" + this.mOwnerToken + " initialBounds=" + this.mInitialBounds + " windowingMode=" + this.mWindowingMode + "}";
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    /* loaded from: classes4.dex */
    public static final class Builder {
        private final IBinder mFragmentToken;
        private final TaskFragmentOrganizerToken mOrganizer;
        private final IBinder mOwnerToken;
        private final Rect mInitialBounds = new Rect();
        private int mWindowingMode = 0;

        public Builder(TaskFragmentOrganizerToken organizer, IBinder fragmentToken, IBinder ownerToken) {
            this.mOrganizer = organizer;
            this.mFragmentToken = fragmentToken;
            this.mOwnerToken = ownerToken;
        }

        public Builder setInitialBounds(Rect bounds) {
            this.mInitialBounds.set(bounds);
            return this;
        }

        public Builder setWindowingMode(int windowingMode) {
            this.mWindowingMode = windowingMode;
            return this;
        }

        public TaskFragmentCreationParams build() {
            TaskFragmentCreationParams result = new TaskFragmentCreationParams(this.mOrganizer, this.mFragmentToken, this.mOwnerToken);
            result.mInitialBounds.set(this.mInitialBounds);
            result.mWindowingMode = this.mWindowingMode;
            return result;
        }
    }
}
