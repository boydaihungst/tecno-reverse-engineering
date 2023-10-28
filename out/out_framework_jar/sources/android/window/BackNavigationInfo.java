package android.window;

import android.app.WindowConfiguration;
import android.hardware.HardwareBuffer;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteCallback;
import android.view.RemoteAnimationTarget;
import android.view.SurfaceControl;
import android.window.IOnBackInvokedCallback;
/* loaded from: classes4.dex */
public final class BackNavigationInfo implements Parcelable {
    public static final Parcelable.Creator<BackNavigationInfo> CREATOR = new Parcelable.Creator<BackNavigationInfo>() { // from class: android.window.BackNavigationInfo.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public BackNavigationInfo createFromParcel(Parcel in) {
            return new BackNavigationInfo(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public BackNavigationInfo[] newArray(int size) {
            return new BackNavigationInfo[size];
        }
    };
    public static final String KEY_TRIGGER_BACK = "TriggerBack";
    public static final int TYPE_CALLBACK = 4;
    public static final int TYPE_CROSS_ACTIVITY = 2;
    public static final int TYPE_CROSS_TASK = 3;
    public static final int TYPE_DIALOG_CLOSE = 0;
    public static final int TYPE_RETURN_TO_HOME = 1;
    public static final int TYPE_UNDEFINED = -1;
    private final RemoteAnimationTarget mDepartingAnimationTarget;
    private final IOnBackInvokedCallback mOnBackInvokedCallback;
    private final RemoteCallback mOnBackNavigationDone;
    private final HardwareBuffer mScreenshotBuffer;
    private final SurfaceControl mScreenshotSurface;
    private final WindowConfiguration mTaskWindowConfiguration;
    private final int mType;

    /* loaded from: classes4.dex */
    @interface BackTargetType {
    }

    public BackNavigationInfo(int type, RemoteAnimationTarget departingAnimationTarget, SurfaceControl screenshotSurface, HardwareBuffer screenshotBuffer, WindowConfiguration taskWindowConfiguration, RemoteCallback onBackNavigationDone, IOnBackInvokedCallback onBackInvokedCallback) {
        this.mType = type;
        this.mDepartingAnimationTarget = departingAnimationTarget;
        this.mScreenshotSurface = screenshotSurface;
        this.mScreenshotBuffer = screenshotBuffer;
        this.mTaskWindowConfiguration = taskWindowConfiguration;
        this.mOnBackNavigationDone = onBackNavigationDone;
        this.mOnBackInvokedCallback = onBackInvokedCallback;
    }

    private BackNavigationInfo(Parcel in) {
        this.mType = in.readInt();
        this.mDepartingAnimationTarget = (RemoteAnimationTarget) in.readTypedObject(RemoteAnimationTarget.CREATOR);
        this.mScreenshotSurface = (SurfaceControl) in.readTypedObject(SurfaceControl.CREATOR);
        this.mScreenshotBuffer = (HardwareBuffer) in.readTypedObject(HardwareBuffer.CREATOR);
        this.mTaskWindowConfiguration = (WindowConfiguration) in.readTypedObject(WindowConfiguration.CREATOR);
        this.mOnBackNavigationDone = (RemoteCallback) in.readTypedObject(RemoteCallback.CREATOR);
        this.mOnBackInvokedCallback = IOnBackInvokedCallback.Stub.asInterface(in.readStrongBinder());
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mType);
        dest.writeTypedObject(this.mDepartingAnimationTarget, flags);
        dest.writeTypedObject(this.mScreenshotSurface, flags);
        dest.writeTypedObject(this.mScreenshotBuffer, flags);
        dest.writeTypedObject(this.mTaskWindowConfiguration, flags);
        dest.writeTypedObject(this.mOnBackNavigationDone, flags);
        dest.writeStrongInterface(this.mOnBackInvokedCallback);
    }

    public int getType() {
        return this.mType;
    }

    public RemoteAnimationTarget getDepartingAnimationTarget() {
        return this.mDepartingAnimationTarget;
    }

    public SurfaceControl getScreenshotSurface() {
        return this.mScreenshotSurface;
    }

    public HardwareBuffer getScreenshotHardwareBuffer() {
        return this.mScreenshotBuffer;
    }

    public WindowConfiguration getTaskWindowConfiguration() {
        return this.mTaskWindowConfiguration;
    }

    public IOnBackInvokedCallback getOnBackInvokedCallback() {
        return this.mOnBackInvokedCallback;
    }

    public void onBackNavigationFinished(boolean triggerBack) {
        if (this.mOnBackNavigationDone != null) {
            Bundle result = new Bundle();
            result.putBoolean(KEY_TRIGGER_BACK, triggerBack);
            this.mOnBackNavigationDone.sendResult(result);
        }
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public String toString() {
        return "BackNavigationInfo{mType=" + typeToString(this.mType) + " (" + this.mType + "), mDepartingAnimationTarget=" + this.mDepartingAnimationTarget + ", mScreenshotSurface=" + this.mScreenshotSurface + ", mTaskWindowConfiguration= " + this.mTaskWindowConfiguration + ", mScreenshotBuffer=" + this.mScreenshotBuffer + ", mOnBackNavigationDone=" + this.mOnBackNavigationDone + ", mOnBackInvokedCallback=" + this.mOnBackInvokedCallback + '}';
    }

    public static String typeToString(int type) {
        switch (type) {
            case -1:
                return "TYPE_UNDEFINED";
            case 0:
                return "TYPE_DIALOG_CLOSE";
            case 1:
                return "TYPE_RETURN_TO_HOME";
            case 2:
                return "TYPE_CROSS_ACTIVITY";
            case 3:
                return "TYPE_CROSS_TASK";
            case 4:
                return "TYPE_CALLBACK";
            default:
                return String.valueOf(type);
        }
    }

    /* loaded from: classes4.dex */
    public static class Builder {
        private int mType = -1;
        private RemoteAnimationTarget mDepartingAnimationTarget = null;
        private SurfaceControl mScreenshotSurface = null;
        private HardwareBuffer mScreenshotBuffer = null;
        private WindowConfiguration mTaskWindowConfiguration = null;
        private RemoteCallback mOnBackNavigationDone = null;
        private IOnBackInvokedCallback mOnBackInvokedCallback = null;

        public Builder setType(int type) {
            this.mType = type;
            return this;
        }

        public Builder setDepartingAnimationTarget(RemoteAnimationTarget departingAnimationTarget) {
            this.mDepartingAnimationTarget = departingAnimationTarget;
            return this;
        }

        public Builder setScreenshotSurface(SurfaceControl screenshotSurface) {
            this.mScreenshotSurface = screenshotSurface;
            return this;
        }

        public Builder setScreenshotBuffer(HardwareBuffer screenshotBuffer) {
            this.mScreenshotBuffer = screenshotBuffer;
            return this;
        }

        public Builder setTaskWindowConfiguration(WindowConfiguration taskWindowConfiguration) {
            this.mTaskWindowConfiguration = taskWindowConfiguration;
            return this;
        }

        public Builder setOnBackNavigationDone(RemoteCallback onBackNavigationDone) {
            this.mOnBackNavigationDone = onBackNavigationDone;
            return this;
        }

        public Builder setOnBackInvokedCallback(IOnBackInvokedCallback onBackInvokedCallback) {
            this.mOnBackInvokedCallback = onBackInvokedCallback;
            return this;
        }

        public BackNavigationInfo build() {
            return new BackNavigationInfo(this.mType, this.mDepartingAnimationTarget, this.mScreenshotSurface, this.mScreenshotBuffer, this.mTaskWindowConfiguration, this.mOnBackNavigationDone, this.mOnBackInvokedCallback);
        }
    }
}
