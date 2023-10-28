package android.content.pm;

import android.content.ComponentName;
import android.os.Parcel;
import android.os.Parcelable;
/* loaded from: classes.dex */
public class LauncherActivityInfoInternal implements Parcelable {
    public static final Parcelable.Creator<LauncherActivityInfoInternal> CREATOR = new Parcelable.Creator<LauncherActivityInfoInternal>() { // from class: android.content.pm.LauncherActivityInfoInternal.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public LauncherActivityInfoInternal createFromParcel(Parcel source) {
            return new LauncherActivityInfoInternal(source);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public LauncherActivityInfoInternal[] newArray(int size) {
            return new LauncherActivityInfoInternal[size];
        }
    };
    private ActivityInfo mActivityInfo;
    private ComponentName mComponentName;
    private IncrementalStatesInfo mIncrementalStatesInfo;

    public LauncherActivityInfoInternal(ActivityInfo info, IncrementalStatesInfo incrementalStatesInfo) {
        this.mActivityInfo = info;
        this.mComponentName = new ComponentName(info.packageName, info.name);
        this.mIncrementalStatesInfo = incrementalStatesInfo;
    }

    public LauncherActivityInfoInternal(Parcel source) {
        this.mActivityInfo = (ActivityInfo) source.readParcelable(ActivityInfo.class.getClassLoader(), ActivityInfo.class);
        this.mComponentName = new ComponentName(this.mActivityInfo.packageName, this.mActivityInfo.name);
        this.mIncrementalStatesInfo = (IncrementalStatesInfo) source.readParcelable(IncrementalStatesInfo.class.getClassLoader(), IncrementalStatesInfo.class);
    }

    public ComponentName getComponentName() {
        return this.mComponentName;
    }

    public ActivityInfo getActivityInfo() {
        return this.mActivityInfo;
    }

    public IncrementalStatesInfo getIncrementalStatesInfo() {
        return this.mIncrementalStatesInfo;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mActivityInfo, 0);
        dest.writeParcelable(this.mIncrementalStatesInfo, 0);
    }
}
