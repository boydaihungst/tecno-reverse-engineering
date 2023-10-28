package android.window;

import android.app.ActivityManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.os.Parcel;
import android.os.Parcelable;
import android.security.keystore.KeyProperties;
import android.telecom.Logging.Session;
import android.view.SurfaceControl;
import android.view.WindowManager;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes4.dex */
public final class TransitionInfo implements Parcelable {
    public static final Parcelable.Creator<TransitionInfo> CREATOR = new Parcelable.Creator<TransitionInfo>() { // from class: android.window.TransitionInfo.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TransitionInfo createFromParcel(Parcel in) {
            return new TransitionInfo(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TransitionInfo[] newArray(int size) {
            return new TransitionInfo[size];
        }
    };
    public static final int FLAG_DISPLAY_HAS_ALERT_WINDOWS = 128;
    public static final int FLAG_FIRST_CUSTOM = 256;
    public static final int FLAG_IS_DISPLAY = 32;
    public static final int FLAG_IS_VOICE_INTERACTION = 16;
    public static final int FLAG_IS_WALLPAPER = 2;
    public static final int FLAG_NONE = 0;
    public static final int FLAG_OCCLUDES_KEYGUARD = 64;
    public static final int FLAG_SHOW_WALLPAPER = 1;
    public static final int FLAG_STARTING_WINDOW_TRANSFER_RECIPIENT = 8;
    public static final int FLAG_TRANSLUCENT = 4;
    private final ArrayList<Change> mChanges;
    private final int mFlags;
    private AnimationOptions mOptions;
    private SurfaceControl mRootLeash;
    private final Point mRootOffset;
    private final int mType;

    /* loaded from: classes4.dex */
    public @interface ChangeFlags {
    }

    /* loaded from: classes4.dex */
    public @interface TransitionMode {
    }

    public TransitionInfo(int type, int flags) {
        this.mChanges = new ArrayList<>();
        this.mRootOffset = new Point();
        this.mType = type;
        this.mFlags = flags;
    }

    private TransitionInfo(Parcel in) {
        ArrayList<Change> arrayList = new ArrayList<>();
        this.mChanges = arrayList;
        Point point = new Point();
        this.mRootOffset = point;
        this.mType = in.readInt();
        this.mFlags = in.readInt();
        in.readTypedList(arrayList, Change.CREATOR);
        SurfaceControl surfaceControl = new SurfaceControl();
        this.mRootLeash = surfaceControl;
        surfaceControl.readFromParcel(in);
        point.readFromParcel(in);
        this.mOptions = (AnimationOptions) in.readTypedObject(AnimationOptions.CREATOR);
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mType);
        dest.writeInt(this.mFlags);
        dest.writeTypedList(this.mChanges);
        this.mRootLeash.writeToParcel(dest, flags);
        this.mRootOffset.writeToParcel(dest, flags);
        dest.writeTypedObject(this.mOptions, flags);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public void setRootLeash(SurfaceControl leash, int offsetLeft, int offsetTop) {
        this.mRootLeash = leash;
        this.mRootOffset.set(offsetLeft, offsetTop);
    }

    public void setAnimationOptions(AnimationOptions options) {
        this.mOptions = options;
    }

    public int getType() {
        return this.mType;
    }

    public int getFlags() {
        return this.mFlags;
    }

    public SurfaceControl getRootLeash() {
        SurfaceControl surfaceControl = this.mRootLeash;
        if (surfaceControl == null) {
            throw new IllegalStateException("Trying to get a leash which wasn't set");
        }
        return surfaceControl;
    }

    public Point getRootOffset() {
        return this.mRootOffset;
    }

    public AnimationOptions getAnimationOptions() {
        return this.mOptions;
    }

    public List<Change> getChanges() {
        return this.mChanges;
    }

    public Change getChange(WindowContainerToken token) {
        for (int i = this.mChanges.size() - 1; i >= 0; i--) {
            if (token.equals(this.mChanges.get(i).mContainer)) {
                return this.mChanges.get(i);
            }
        }
        return null;
    }

    public void addChange(Change change) {
        this.mChanges.add(change);
    }

    public boolean isKeyguardGoingAway() {
        return (this.mFlags & 256) != 0;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{t=" + WindowManager.transitTypeToString(this.mType) + " f=0x" + Integer.toHexString(this.mFlags) + " ro=" + this.mRootOffset + " c=[");
        for (int i = 0; i < this.mChanges.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(this.mChanges.get(i));
        }
        sb.append("]}");
        return sb.toString();
    }

    public static String modeToString(int mode) {
        switch (mode) {
            case 0:
                return KeyProperties.DIGEST_NONE;
            case 1:
                return "OPEN";
            case 2:
                return "CLOSE";
            case 3:
                return "SHOW";
            case 4:
                return "HIDE";
            case 5:
            default:
                return "<unknown:" + mode + ">";
            case 6:
                return "CHANGE";
        }
    }

    public static String flagsToString(int flags) {
        if (flags == 0) {
            return KeyProperties.DIGEST_NONE;
        }
        StringBuilder sb = new StringBuilder();
        if ((flags & 1) != 0) {
            sb.append("SHOW_WALLPAPER");
        }
        if ((flags & 2) != 0) {
            sb.append("IS_WALLPAPER");
        }
        if ((flags & 4) != 0) {
            sb.append((sb.length() == 0 ? "" : "|") + "TRANSLUCENT");
        }
        if ((flags & 8) != 0) {
            sb.append((sb.length() == 0 ? "" : "|") + "STARTING_WINDOW_TRANSFER");
        }
        if ((flags & 16) != 0) {
            sb.append((sb.length() == 0 ? "" : "|") + "IS_VOICE_INTERACTION");
        }
        if ((flags & 32) != 0) {
            sb.append((sb.length() == 0 ? "" : "|") + "IS_DISPLAY");
        }
        if ((flags & 64) != 0) {
            sb.append((sb.length() == 0 ? "" : "|") + "OCCLUDES_KEYGUARD");
        }
        if ((flags & 128) != 0) {
            sb.append((sb.length() == 0 ? "" : "|") + "DISPLAY_HAS_ALERT_WINDOWS");
        }
        if ((flags & 256) != 0) {
            sb.append((sb.length() != 0 ? "|" : "") + "FIRST_CUSTOM");
        }
        return sb.toString();
    }

    public static boolean isIndependent(Change change, TransitionInfo info) {
        if (change.getParent() == null) {
            return true;
        }
        if (change.getMode() == 6) {
            return false;
        }
        Change parentChg = info.getChange(change.getParent());
        while (parentChg != null && parentChg.getMode() == 6) {
            if (parentChg.getParent() == null) {
                return true;
            }
            parentChg = info.getChange(parentChg.getParent());
        }
        return false;
    }

    /* loaded from: classes4.dex */
    public static final class Change implements Parcelable {
        public static final Parcelable.Creator<Change> CREATOR = new Parcelable.Creator<Change>() { // from class: android.window.TransitionInfo.Change.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public Change createFromParcel(Parcel in) {
                return new Change(in);
            }

            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public Change[] newArray(int size) {
                return new Change[size];
            }
        };
        private boolean mAllowEnterPip;
        private int mBackgroundColor;
        private final WindowContainerToken mContainer;
        private final Rect mEndAbsBounds;
        private int mEndFixedRotation;
        private final Point mEndRelOffset;
        private int mEndRotation;
        private int mFlags;
        private final SurfaceControl mLeash;
        private int mMode;
        private WindowContainerToken mParent;
        private int mRotationAnimation;
        private final Rect mStartAbsBounds;
        private int mStartRotation;
        private ActivityManager.RunningTaskInfo mTaskInfo;

        public Change(WindowContainerToken container, SurfaceControl leash) {
            this.mMode = 0;
            this.mFlags = 0;
            this.mStartAbsBounds = new Rect();
            this.mEndAbsBounds = new Rect();
            this.mEndRelOffset = new Point();
            this.mTaskInfo = null;
            this.mStartRotation = -1;
            this.mEndRotation = -1;
            this.mEndFixedRotation = -1;
            this.mRotationAnimation = -1;
            this.mContainer = container;
            this.mLeash = leash;
        }

        private Change(Parcel in) {
            this.mMode = 0;
            this.mFlags = 0;
            Rect rect = new Rect();
            this.mStartAbsBounds = rect;
            Rect rect2 = new Rect();
            this.mEndAbsBounds = rect2;
            Point point = new Point();
            this.mEndRelOffset = point;
            this.mTaskInfo = null;
            this.mStartRotation = -1;
            this.mEndRotation = -1;
            this.mEndFixedRotation = -1;
            this.mRotationAnimation = -1;
            this.mContainer = (WindowContainerToken) in.readTypedObject(WindowContainerToken.CREATOR);
            this.mParent = (WindowContainerToken) in.readTypedObject(WindowContainerToken.CREATOR);
            SurfaceControl surfaceControl = new SurfaceControl();
            this.mLeash = surfaceControl;
            surfaceControl.readFromParcel(in);
            this.mMode = in.readInt();
            this.mFlags = in.readInt();
            rect.readFromParcel(in);
            rect2.readFromParcel(in);
            point.readFromParcel(in);
            this.mTaskInfo = (ActivityManager.RunningTaskInfo) in.readTypedObject(ActivityManager.RunningTaskInfo.CREATOR);
            this.mAllowEnterPip = in.readBoolean();
            this.mStartRotation = in.readInt();
            this.mEndRotation = in.readInt();
            this.mEndFixedRotation = in.readInt();
            this.mRotationAnimation = in.readInt();
            this.mBackgroundColor = in.readInt();
        }

        public void setParent(WindowContainerToken parent) {
            this.mParent = parent;
        }

        public void setMode(int mode) {
            this.mMode = mode;
        }

        public void setFlags(int flags) {
            this.mFlags = flags;
        }

        public void setStartAbsBounds(Rect rect) {
            this.mStartAbsBounds.set(rect);
        }

        public void setEndAbsBounds(Rect rect) {
            this.mEndAbsBounds.set(rect);
        }

        public void setEndRelOffset(int left, int top) {
            this.mEndRelOffset.set(left, top);
        }

        public void setTaskInfo(ActivityManager.RunningTaskInfo taskInfo) {
            this.mTaskInfo = taskInfo;
        }

        public void setAllowEnterPip(boolean allowEnterPip) {
            this.mAllowEnterPip = allowEnterPip;
        }

        public void setRotation(int start, int end) {
            this.mStartRotation = start;
            this.mEndRotation = end;
        }

        public void setEndFixedRotation(int endFixedRotation) {
            this.mEndFixedRotation = endFixedRotation;
        }

        public void setRotationAnimation(int anim) {
            this.mRotationAnimation = anim;
        }

        public void setBackgroundColor(int backgroundColor) {
            this.mBackgroundColor = backgroundColor;
        }

        public WindowContainerToken getContainer() {
            return this.mContainer;
        }

        public WindowContainerToken getParent() {
            return this.mParent;
        }

        public int getMode() {
            return this.mMode;
        }

        public int getFlags() {
            return this.mFlags;
        }

        public Rect getStartAbsBounds() {
            return this.mStartAbsBounds;
        }

        public Rect getEndAbsBounds() {
            return this.mEndAbsBounds;
        }

        public Point getEndRelOffset() {
            return this.mEndRelOffset;
        }

        public SurfaceControl getLeash() {
            return this.mLeash;
        }

        public ActivityManager.RunningTaskInfo getTaskInfo() {
            return this.mTaskInfo;
        }

        public boolean getAllowEnterPip() {
            return this.mAllowEnterPip;
        }

        public int getStartRotation() {
            return this.mStartRotation;
        }

        public int getEndRotation() {
            return this.mEndRotation;
        }

        public int getEndFixedRotation() {
            return this.mEndFixedRotation;
        }

        public int getRotationAnimation() {
            return this.mRotationAnimation;
        }

        public int getBackgroundColor() {
            return this.mBackgroundColor;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeTypedObject(this.mContainer, flags);
            dest.writeTypedObject(this.mParent, flags);
            this.mLeash.writeToParcel(dest, flags);
            dest.writeInt(this.mMode);
            dest.writeInt(this.mFlags);
            this.mStartAbsBounds.writeToParcel(dest, flags);
            this.mEndAbsBounds.writeToParcel(dest, flags);
            this.mEndRelOffset.writeToParcel(dest, flags);
            dest.writeTypedObject(this.mTaskInfo, flags);
            dest.writeBoolean(this.mAllowEnterPip);
            dest.writeInt(this.mStartRotation);
            dest.writeInt(this.mEndRotation);
            dest.writeInt(this.mEndFixedRotation);
            dest.writeInt(this.mRotationAnimation);
            dest.writeInt(this.mBackgroundColor);
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        public String toString() {
            return "{" + this.mContainer + NavigationBarInflaterView.KEY_CODE_START + this.mParent + ") leash=" + this.mLeash + " m=" + TransitionInfo.modeToString(this.mMode) + " f=" + TransitionInfo.flagsToString(this.mFlags) + " sb=" + this.mStartAbsBounds + " eb=" + this.mEndAbsBounds + " eo=" + this.mEndRelOffset + " r=" + this.mStartRotation + Session.SUBSESSION_SEPARATION_CHAR + this.mEndRotation + ":" + this.mRotationAnimation + " endFixedRotation=" + this.mEndFixedRotation + "}";
        }
    }

    /* loaded from: classes4.dex */
    public static final class AnimationOptions implements Parcelable {
        public static final Parcelable.Creator<AnimationOptions> CREATOR = new Parcelable.Creator<AnimationOptions>() { // from class: android.window.TransitionInfo.AnimationOptions.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public AnimationOptions createFromParcel(Parcel in) {
                return new AnimationOptions(in);
            }

            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public AnimationOptions[] newArray(int size) {
                return new AnimationOptions[size];
            }
        };
        private int mAnimations;
        private int mBackgroundColor;
        private int mEnterResId;
        private int mExitResId;
        private boolean mOverrideTaskTransition;
        private String mPackageName;
        private HardwareBuffer mThumbnail;
        private final Rect mTransitionBounds;
        private int mType;

        private AnimationOptions(int type) {
            this.mTransitionBounds = new Rect();
            this.mType = type;
        }

        public AnimationOptions(Parcel in) {
            Rect rect = new Rect();
            this.mTransitionBounds = rect;
            this.mType = in.readInt();
            this.mEnterResId = in.readInt();
            this.mExitResId = in.readInt();
            this.mBackgroundColor = in.readInt();
            this.mOverrideTaskTransition = in.readBoolean();
            this.mPackageName = in.readString();
            rect.readFromParcel(in);
            this.mThumbnail = (HardwareBuffer) in.readTypedObject(HardwareBuffer.CREATOR);
            this.mAnimations = in.readInt();
        }

        public static AnimationOptions makeAnimOptionsFromLayoutParameters(WindowManager.LayoutParams lp) {
            AnimationOptions options = new AnimationOptions(14);
            options.mPackageName = lp.packageName;
            options.mAnimations = lp.windowAnimations;
            return options;
        }

        public static AnimationOptions makeCustomAnimOptions(String packageName, int enterResId, int exitResId, int backgroundColor, boolean overrideTaskTransition) {
            AnimationOptions options = new AnimationOptions(1);
            options.mPackageName = packageName;
            options.mEnterResId = enterResId;
            options.mExitResId = exitResId;
            options.mBackgroundColor = backgroundColor;
            options.mOverrideTaskTransition = overrideTaskTransition;
            return options;
        }

        public static AnimationOptions makeClipRevealAnimOptions(int startX, int startY, int width, int height) {
            AnimationOptions options = new AnimationOptions(11);
            options.mTransitionBounds.set(startX, startY, startX + width, startY + height);
            return options;
        }

        public static AnimationOptions makeScaleUpAnimOptions(int startX, int startY, int width, int height) {
            AnimationOptions options = new AnimationOptions(2);
            options.mTransitionBounds.set(startX, startY, startX + width, startY + height);
            return options;
        }

        public static AnimationOptions makeThumbnailAnimOptions(HardwareBuffer srcThumb, int startX, int startY, boolean scaleUp) {
            AnimationOptions options = new AnimationOptions(scaleUp ? 3 : 4);
            options.mTransitionBounds.set(startX, startY, startX, startY);
            options.mThumbnail = srcThumb;
            return options;
        }

        public static AnimationOptions makeCrossProfileAnimOptions() {
            AnimationOptions options = new AnimationOptions(12);
            return options;
        }

        public int getType() {
            return this.mType;
        }

        public int getEnterResId() {
            return this.mEnterResId;
        }

        public int getExitResId() {
            return this.mExitResId;
        }

        public int getBackgroundColor() {
            return this.mBackgroundColor;
        }

        public boolean getOverrideTaskTransition() {
            return this.mOverrideTaskTransition;
        }

        public String getPackageName() {
            return this.mPackageName;
        }

        public Rect getTransitionBounds() {
            return this.mTransitionBounds;
        }

        public HardwareBuffer getThumbnail() {
            return this.mThumbnail;
        }

        public int getAnimations() {
            return this.mAnimations;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.mType);
            dest.writeInt(this.mEnterResId);
            dest.writeInt(this.mExitResId);
            dest.writeInt(this.mBackgroundColor);
            dest.writeBoolean(this.mOverrideTaskTransition);
            dest.writeString(this.mPackageName);
            this.mTransitionBounds.writeToParcel(dest, flags);
            dest.writeTypedObject(this.mThumbnail, flags);
            dest.writeInt(this.mAnimations);
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        private static String typeToString(int mode) {
            switch (mode) {
                case 1:
                    return "ANIM_CUSTOM";
                case 2:
                    return "ANIM_SCALE_UP";
                case 3:
                    return "ANIM_THUMBNAIL_SCALE_UP";
                case 4:
                    return "ANIM_THUMBNAIL_SCALE_DOWN";
                case 11:
                    return "ANIM_CLIP_REVEAL";
                case 12:
                    return "ANIM_OPEN_CROSS_PROFILE_APPS";
                default:
                    return "<unknown:" + mode + ">";
            }
        }

        public String toString() {
            return "{ AnimationOptions type= " + typeToString(this.mType) + " package=" + this.mPackageName + " override=" + this.mOverrideTaskTransition + " b=" + this.mTransitionBounds + "}";
        }
    }
}
