package android.window;

import android.app.ActivityManager;
import android.content.pm.ActivityInfo;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.InsetsState;
import android.view.InsetsVisibilities;
import android.view.WindowManager;
/* loaded from: classes4.dex */
public final class StartingWindowInfo implements Parcelable {
    public static final Parcelable.Creator<StartingWindowInfo> CREATOR = new Parcelable.Creator<StartingWindowInfo>() { // from class: android.window.StartingWindowInfo.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public StartingWindowInfo createFromParcel(Parcel source) {
            return new StartingWindowInfo(source);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public StartingWindowInfo[] newArray(int size) {
            return new StartingWindowInfo[size];
        }
    };
    public static final int STARTING_WINDOW_TYPE_LEGACY_SPLASH_SCREEN = 4;
    public static final int STARTING_WINDOW_TYPE_NONE = 0;
    public static final int STARTING_WINDOW_TYPE_SNAPSHOT = 2;
    public static final int STARTING_WINDOW_TYPE_SOLID_COLOR_SPLASH_SCREEN = 3;
    public static final int STARTING_WINDOW_TYPE_SPLASH_SCREEN = 1;
    public static final int TYPE_PARAMETER_ACTIVITY_CREATED = 16;
    public static final int TYPE_PARAMETER_ACTIVITY_DRAWN = 64;
    public static final int TYPE_PARAMETER_ALLOW_HANDLE_SOLID_COLOR_SCREEN = 128;
    public static final int TYPE_PARAMETER_ALLOW_TASK_SNAPSHOT = 8;
    public static final int TYPE_PARAMETER_LEGACY_SPLASH_SCREEN = Integer.MIN_VALUE;
    public static final int TYPE_PARAMETER_NEW_TASK = 1;
    public static final int TYPE_PARAMETER_PROCESS_RUNNING = 4;
    public static final int TYPE_PARAMETER_TASK_SWITCH = 2;
    public static final int TYPE_PARAMETER_USE_SOLID_COLOR_SPLASH_SCREEN = 32;
    public boolean isKeyguardOccluded;
    public WindowManager.LayoutParams mainWindowLayoutParams;
    public final InsetsVisibilities requestedVisibilities;
    public int splashScreenThemeResId;
    public int startingWindowTypeParameter;
    public ActivityInfo targetActivityInfo;
    public ActivityManager.RunningTaskInfo taskInfo;
    public TaskSnapshot taskSnapshot;
    public InsetsState topOpaqueWindowInsetsState;
    public WindowManager.LayoutParams topOpaqueWindowLayoutParams;

    /* loaded from: classes4.dex */
    public @interface StartingTypeParams {
    }

    /* loaded from: classes4.dex */
    public @interface StartingWindowType {
    }

    public StartingWindowInfo() {
        this.isKeyguardOccluded = false;
        this.requestedVisibilities = new InsetsVisibilities();
    }

    private StartingWindowInfo(Parcel source) {
        this.isKeyguardOccluded = false;
        this.requestedVisibilities = new InsetsVisibilities();
        readFromParcel(source);
    }

    public boolean allowHandleSolidColorSplashScreen() {
        return (this.startingWindowTypeParameter & 128) != 0;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedObject(this.taskInfo, flags);
        dest.writeTypedObject(this.targetActivityInfo, flags);
        dest.writeInt(this.startingWindowTypeParameter);
        dest.writeTypedObject(this.topOpaqueWindowInsetsState, flags);
        dest.writeTypedObject(this.topOpaqueWindowLayoutParams, flags);
        dest.writeTypedObject(this.mainWindowLayoutParams, flags);
        dest.writeInt(this.splashScreenThemeResId);
        dest.writeBoolean(this.isKeyguardOccluded);
        dest.writeTypedObject(this.taskSnapshot, flags);
        this.requestedVisibilities.writeToParcel(dest, flags);
    }

    void readFromParcel(Parcel source) {
        this.taskInfo = (ActivityManager.RunningTaskInfo) source.readTypedObject(ActivityManager.RunningTaskInfo.CREATOR);
        this.targetActivityInfo = (ActivityInfo) source.readTypedObject(ActivityInfo.CREATOR);
        this.startingWindowTypeParameter = source.readInt();
        this.topOpaqueWindowInsetsState = (InsetsState) source.readTypedObject(InsetsState.CREATOR);
        this.topOpaqueWindowLayoutParams = (WindowManager.LayoutParams) source.readTypedObject(WindowManager.LayoutParams.CREATOR);
        this.mainWindowLayoutParams = (WindowManager.LayoutParams) source.readTypedObject(WindowManager.LayoutParams.CREATOR);
        this.splashScreenThemeResId = source.readInt();
        this.isKeyguardOccluded = source.readBoolean();
        this.taskSnapshot = (TaskSnapshot) source.readTypedObject(TaskSnapshot.CREATOR);
        this.requestedVisibilities.readFromParcel(source);
    }

    public String toString() {
        return "StartingWindowInfo{taskId=" + this.taskInfo.taskId + " targetActivityInfo=" + this.targetActivityInfo + " displayId=" + this.taskInfo.displayId + " topActivityType=" + this.taskInfo.topActivityType + " preferredStartingWindowType=" + Integer.toHexString(this.startingWindowTypeParameter) + " insetsState=" + this.topOpaqueWindowInsetsState + " topWindowLayoutParams=" + this.topOpaqueWindowLayoutParams + " mainWindowLayoutParams=" + this.mainWindowLayoutParams + " splashScreenThemeResId " + Integer.toHexString(this.splashScreenThemeResId);
    }
}
