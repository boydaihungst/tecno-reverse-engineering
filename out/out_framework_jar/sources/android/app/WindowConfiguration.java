package android.app;

import android.content.Context;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.proto.ProtoInputStream;
import android.util.proto.ProtoOutputStream;
import android.util.proto.WireTypeMismatchException;
import android.view.Surface;
import com.transsion.hubcore.server.app.ITranWindowConfiguration;
import java.io.IOException;
import java.util.Objects;
/* loaded from: classes.dex */
public class WindowConfiguration implements Parcelable, Comparable<WindowConfiguration> {
    public static final int ACTIVITY_TYPE_ASSISTANT = 4;
    public static final int ACTIVITY_TYPE_DREAM = 5;
    public static final int ACTIVITY_TYPE_HOME = 2;
    public static final int ACTIVITY_TYPE_RECENTS = 3;
    public static final int ACTIVITY_TYPE_STANDARD = 1;
    public static final int ACTIVITY_TYPE_UNDEFINED = 0;
    private static final int ALWAYS_ON_TOP_OFF = 2;
    private static final int ALWAYS_ON_TOP_ON = 1;
    private static final int ALWAYS_ON_TOP_UNDEFINED = 0;
    public static final Parcelable.Creator<WindowConfiguration> CREATOR = new Parcelable.Creator<WindowConfiguration>() { // from class: android.app.WindowConfiguration.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public WindowConfiguration createFromParcel(Parcel in) {
            return new WindowConfiguration(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public WindowConfiguration[] newArray(int size) {
            return new WindowConfiguration[size];
        }
    };
    public static final int MULTI_WINDOW_BIG = 16;
    public static final int MULTI_WINDOW_HOR = 4;
    public static final int MULTI_WINDOW_ID_NORMAL = 0;
    public static final int MULTI_WINDOW_ID_UNDEFINED = -1;
    public static final int MULTI_WINDOW_INLARGESCREEN = 0;
    public static final int MULTI_WINDOW_INLARGESCREEN_UNDEFINED = -1;
    public static final int MULTI_WINDOW_INTERACTIVE = 8;
    public static final int MULTI_WINDOW_MODE_LIGHTING = 2;
    public static final int MULTI_WINDOW_MODE_NORMAL = 1;
    public static final int MULTI_WINDOW_MODE_SPLIT_SCREEN_PRIMARY = 32768;
    public static final int MULTI_WINDOW_MODE_SPLIT_SCREEN_SECOND = 65536;
    public static final int MULTI_WINDOW_MODE_THUNDERBACK = 4;
    public static final int MULTI_WINDOW_MODE_THUNDERBACK_HOR = 8;
    public static final int MULTI_WINDOW_MODE_THUNDERBACK_INTERACTIVE = 2;
    public static final int MULTI_WINDOW_MODE_THUNDERBACK_INTERACTIVE_HOR = 6;
    public static final int MULTI_WINDOW_MODE_THUNDERBACK_INTERACTIVE_TOUCH = 3;
    public static final int MULTI_WINDOW_MODE_THUNDERBACK_INTERACTIVE_TOUCH_HOR = 7;
    public static final int MULTI_WINDOW_MODE_THUNDERBACK_TOUCH = 5;
    public static final int MULTI_WINDOW_MODE_THUNDERBACK_TOUCH_HOR = 9;
    public static final int MULTI_WINDOW_MODE_THUNDERBACK_WELT = 10;
    public static final int MULTI_WINDOW_MODE_UNDEFINED = 0;
    public static final int MULTI_WINDOW_NOTINLARGESCREEN = 1;
    public static final int MULTI_WINDOW_RESIZABLE = 2;
    public static final int MULTI_WINDOW_RESIZABLE_UNDEFINED = -1;
    public static final int MULTI_WINDOW_SHOWCAPTION = 4;
    public static final int MULTI_WINDOW_SHOW_LINE = 128;
    public static final int MULTI_WINDOW_SHOW_MASK = 256;
    public static final int MULTI_WINDOW_SMALL = 32;
    public static final int MULTI_WINDOW_WELT = 64;
    public static final int ROTATION_UNDEFINED = -1;
    public static final int TRAN_MULTI_WINDOW_NOTSHOWCAPTION = 0;
    public static final int TRAN_MULTI_WINDOW_SHOWCAPTION = 1;
    public static final String TRAN_MULTI_WINDOW_SHOWCAPTION_STRING = "com.transsion.multiwindow.showcaption";
    public static final int TRIGGER_MODE_FOUR_FINGER = 9;
    public static final int TRIGGER_MODE_FULLSCREEN_MULTITASK_BTN = 13;
    public static final int TRIGGER_MODE_GESTURE_NAVIGATION = 8;
    public static final int TRIGGER_MODE_NOTIFICATION = 6;
    public static final int TRIGGER_MODE_RECENT = 1001;
    public static final int TRIGGER_MODE_RECENT_BTN = 7;
    public static final int TRIGGER_MODE_RECENT_KEY_LONG_PRESS = 3;
    public static final int TRIGGER_MODE_SHARE = 5;
    public static final int TRIGGER_MODE_SMARTPANEL = 2;
    public static final int TRIGGER_MODE_SMART_PANEL_APP = 4;
    public static final int TRIGGER_MODE_SPLITSCREEN_DRAG = 11;
    public static final int TRIGGER_MODE_SPLITSCREEN_MULTITASK_BTN = 12;
    public static final int TRIGGER_MODE_STATUSBAR_LONG_PRESS = 10;
    public static final int TRIGGER_MODE_THREE_FINGER = 1;
    public static final int WINDOWING_MODE_FREEFORM = 5;
    public static final int WINDOWING_MODE_FULLSCREEN = 1;
    public static final int WINDOWING_MODE_MULTI_WINDOW = 6;
    public static final int WINDOWING_MODE_PINNED = 2;
    public static final int WINDOWING_MODE_SPLIT_SCREEN_PRIMARY = 3;
    public static final int WINDOWING_MODE_SPLIT_SCREEN_SECONDARY = 4;
    public static final int WINDOWING_MODE_UNDEFINED = 0;
    public static final int WINDOW_CONFIG_ACTIVITY_TYPE = 16;
    public static final int WINDOW_CONFIG_ALWAYS_ON_TOP = 32;
    public static final int WINDOW_CONFIG_APP_BOUNDS = 2;
    public static final int WINDOW_CONFIG_BOUNDS = 1;
    public static final int WINDOW_CONFIG_DISPLAY_ROTATION = 256;
    public static final int WINDOW_CONFIG_DISPLAY_WINDOWING_MODE = 128;
    public static final int WINDOW_CONFIG_MAX_BOUNDS = 4;
    public static final int WINDOW_CONFIG_MULTI_WINDOW_ID = 2048;
    public static final int WINDOW_CONFIG_MULTI_WINDOW_INLARGESCREEN = 8192;
    public static final int WINDOW_CONFIG_MULTI_WINDOW_MODE = 1024;
    public static final int WINDOW_CONFIG_MULTI_WINDOW_RESIZABLE = 4096;
    public static final int WINDOW_CONFIG_ROTATION = 64;
    public static final int WINDOW_CONFIG_WINDOWING_MODE = 8;
    private int mActivityType;
    private int mAlwaysOnTop;
    private Rect mAppBounds;
    private final Rect mBounds;
    private long mDisplayPhysicalId;
    private int mDisplayRotation;
    private int mDisplayWindowingMode;
    private int mInLargeScreen;
    public boolean mIsThunderbackActivity;
    private final Rect mMaxBounds;
    private int mMultiWindowId;
    private int mMultiWindowMode;
    private int mRotation;
    private int mWindowResizable;
    private int mWindowingMode;

    /* loaded from: classes.dex */
    public @interface ActivityType {
    }

    /* loaded from: classes.dex */
    private @interface AlwaysOnTop {
    }

    /* loaded from: classes.dex */
    public @interface WindowConfig {
    }

    /* loaded from: classes.dex */
    public @interface WindowingMode {
    }

    public WindowConfiguration() {
        this.mBounds = new Rect();
        this.mMaxBounds = new Rect();
        this.mDisplayRotation = -1;
        this.mRotation = -1;
        this.mMultiWindowMode = 0;
        this.mIsThunderbackActivity = false;
        this.mMultiWindowId = -1;
        this.mWindowResizable = -1;
        this.mInLargeScreen = -1;
        unset();
    }

    public WindowConfiguration(WindowConfiguration configuration) {
        this.mBounds = new Rect();
        this.mMaxBounds = new Rect();
        this.mDisplayRotation = -1;
        this.mRotation = -1;
        this.mMultiWindowMode = 0;
        this.mIsThunderbackActivity = false;
        this.mMultiWindowId = -1;
        this.mWindowResizable = -1;
        this.mInLargeScreen = -1;
        setTo(configuration);
    }

    private WindowConfiguration(Parcel in) {
        this.mBounds = new Rect();
        this.mMaxBounds = new Rect();
        this.mDisplayRotation = -1;
        this.mRotation = -1;
        this.mMultiWindowMode = 0;
        this.mIsThunderbackActivity = false;
        this.mMultiWindowId = -1;
        this.mWindowResizable = -1;
        this.mInLargeScreen = -1;
        readFromParcel(in);
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        this.mBounds.writeToParcel(dest, flags);
        dest.writeTypedObject(this.mAppBounds, flags);
        this.mMaxBounds.writeToParcel(dest, flags);
        dest.writeInt(this.mWindowingMode);
        dest.writeInt(this.mActivityType);
        dest.writeInt(this.mAlwaysOnTop);
        dest.writeInt(this.mRotation);
        dest.writeInt(this.mDisplayWindowingMode);
        dest.writeInt(this.mDisplayRotation);
        dest.writeInt(this.mMultiWindowMode);
        dest.writeInt(this.mMultiWindowId);
        dest.writeInt(this.mWindowResizable);
        dest.writeInt(this.mInLargeScreen);
        dest.writeLong(this.mDisplayPhysicalId);
    }

    public void readFromParcel(Parcel source) {
        this.mBounds.readFromParcel(source);
        this.mAppBounds = (Rect) source.readTypedObject(Rect.CREATOR);
        this.mMaxBounds.readFromParcel(source);
        this.mWindowingMode = source.readInt();
        this.mActivityType = source.readInt();
        this.mAlwaysOnTop = source.readInt();
        this.mRotation = source.readInt();
        this.mDisplayWindowingMode = source.readInt();
        this.mDisplayRotation = source.readInt();
        this.mMultiWindowMode = source.readInt();
        this.mMultiWindowId = source.readInt();
        this.mWindowResizable = source.readInt();
        this.mInLargeScreen = source.readInt();
        this.mDisplayPhysicalId = source.readLong();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public void setBounds(Rect rect) {
        if (rect == null) {
            this.mBounds.setEmpty();
        } else {
            this.mBounds.set(rect);
        }
    }

    public void setAppBounds(Rect rect) {
        if (rect == null) {
            this.mAppBounds = null;
        } else {
            setAppBounds(rect.left, rect.top, rect.right, rect.bottom);
        }
    }

    public void setMaxBounds(Rect rect) {
        if (rect == null) {
            this.mMaxBounds.setEmpty();
        } else {
            this.mMaxBounds.set(rect);
        }
    }

    public void setMaxBounds(int left, int top, int right, int bottom) {
        this.mMaxBounds.set(left, top, right, bottom);
    }

    public void setDisplayRotation(int rotation) {
        this.mDisplayRotation = rotation;
    }

    public void setAlwaysOnTop(boolean alwaysOnTop) {
        this.mAlwaysOnTop = alwaysOnTop ? 1 : 2;
    }

    public void unsetAlwaysOnTop() {
        this.mAlwaysOnTop = 0;
    }

    private void setAlwaysOnTop(int alwaysOnTop) {
        this.mAlwaysOnTop = alwaysOnTop;
    }

    public void setAppBounds(int left, int top, int right, int bottom) {
        if (this.mAppBounds == null) {
            this.mAppBounds = new Rect();
        }
        this.mAppBounds.set(left, top, right, bottom);
    }

    public Rect getAppBounds() {
        return this.mAppBounds;
    }

    public Rect getBounds() {
        return this.mBounds;
    }

    public Rect getMaxBounds() {
        return this.mMaxBounds;
    }

    public int getDisplayRotation() {
        return this.mDisplayRotation;
    }

    public int getRotation() {
        return this.mRotation;
    }

    public void setRotation(int rotation) {
        this.mRotation = rotation;
    }

    public void setWindowingMode(int windowingMode) {
        this.mWindowingMode = windowingMode;
    }

    public int getWindowingMode() {
        return this.mWindowingMode;
    }

    public void setDisplayWindowingMode(int windowingMode) {
        this.mDisplayWindowingMode = windowingMode;
    }

    public int getDisplayWindowingMode() {
        return this.mDisplayWindowingMode;
    }

    public void setActivityType(int activityType) {
        if (this.mActivityType == activityType) {
            return;
        }
        if (ActivityThread.isSystem() && this.mActivityType != 0 && activityType != 0) {
            throw new IllegalStateException("Can't change activity type once set: " + this + " activityType=" + activityTypeToString(activityType));
        }
        this.mActivityType = activityType;
    }

    public int getActivityType() {
        return this.mActivityType;
    }

    public void setTo(WindowConfiguration other) {
        setBounds(other.mBounds);
        setAppBounds(other.mAppBounds);
        setMaxBounds(other.mMaxBounds);
        setDisplayRotation(other.mDisplayRotation);
        setWindowingMode(other.mWindowingMode);
        setActivityType(other.mActivityType);
        setAlwaysOnTop(other.mAlwaysOnTop);
        setRotation(other.mRotation);
        setDisplayWindowingMode(other.mDisplayWindowingMode);
        ITranWindowConfiguration.Instance().setMultiWindowMode(this, other.mMultiWindowMode);
        ITranWindowConfiguration.Instance().setMultiWindowId(this, other.mMultiWindowId);
        ITranWindowConfiguration.Instance().setWindowResizable(this, other.mWindowResizable);
        ITranWindowConfiguration.Instance().setInLargeScreen(this, other.mInLargeScreen);
        setDisplayPhysicalId(other.mDisplayPhysicalId);
    }

    public void unset() {
        setToDefaults();
    }

    public void setToDefaults() {
        setAppBounds(null);
        setBounds(null);
        setMaxBounds(null);
        setDisplayRotation(-1);
        setWindowingMode(0);
        setActivityType(0);
        setAlwaysOnTop(0);
        setRotation(-1);
        setDisplayWindowingMode(0);
    }

    public int updateFrom(WindowConfiguration delta) {
        int changed = 0;
        if (!delta.mBounds.isEmpty() && !delta.mBounds.equals(this.mBounds)) {
            changed = 0 | 1;
            setBounds(delta.mBounds);
        }
        Rect rect = delta.mAppBounds;
        if (rect != null && !rect.equals(this.mAppBounds)) {
            changed |= 2;
            setAppBounds(delta.mAppBounds);
        }
        if (!delta.mMaxBounds.isEmpty() && !delta.mMaxBounds.equals(this.mMaxBounds)) {
            changed |= 4;
            setMaxBounds(delta.mMaxBounds);
        }
        int i = delta.mWindowingMode;
        if (i != 0 && this.mWindowingMode != i) {
            changed |= 8;
            setWindowingMode(i);
        }
        int i2 = delta.mActivityType;
        if (i2 != 0 && this.mActivityType != i2) {
            changed |= 16;
            setActivityType(i2);
        }
        int i3 = delta.mAlwaysOnTop;
        if (i3 != 0 && this.mAlwaysOnTop != i3) {
            changed |= 32;
            setAlwaysOnTop(i3);
        }
        int i4 = delta.mRotation;
        if (i4 != -1 && i4 != this.mRotation) {
            changed |= 64;
            setRotation(i4);
        }
        int i5 = delta.mDisplayWindowingMode;
        if (i5 != 0 && this.mDisplayWindowingMode != i5) {
            changed |= 128;
            setDisplayWindowingMode(i5);
        }
        int i6 = delta.mDisplayRotation;
        if (i6 != -1 && i6 != this.mDisplayRotation) {
            changed |= 256;
            setDisplayRotation(i6);
        }
        long j = delta.mDisplayPhysicalId;
        if (j != 0) {
            setDisplayPhysicalId(j);
        }
        return ITranWindowConfiguration.Instance().updateFrom(this, delta, changed);
    }

    public void setTo(WindowConfiguration delta, int mask) {
        if ((mask & 1) != 0) {
            setBounds(delta.mBounds);
        }
        if ((mask & 2) != 0) {
            setAppBounds(delta.mAppBounds);
        }
        if ((mask & 4) != 0) {
            setMaxBounds(delta.mMaxBounds);
        }
        if ((mask & 8) != 0) {
            setWindowingMode(delta.mWindowingMode);
        }
        if ((mask & 16) != 0) {
            setActivityType(delta.mActivityType);
        }
        if ((mask & 32) != 0) {
            setAlwaysOnTop(delta.mAlwaysOnTop);
        }
        if ((mask & 64) != 0) {
            setRotation(delta.mRotation);
        }
        if ((mask & 128) != 0) {
            setDisplayWindowingMode(delta.mDisplayWindowingMode);
        }
        if ((mask & 256) != 0) {
            setDisplayRotation(delta.mDisplayRotation);
        }
        ITranWindowConfiguration.Instance().setTo(this, delta, mask);
    }

    public long diff(WindowConfiguration other, boolean compareUndefined) {
        Rect rect;
        Rect rect2;
        long changes = this.mBounds.equals(other.mBounds) ? 0L : 0 | 1;
        if ((compareUndefined || other.mAppBounds != null) && (rect = this.mAppBounds) != (rect2 = other.mAppBounds) && (rect == null || !rect.equals(rect2))) {
            changes |= 2;
        }
        if (!this.mMaxBounds.equals(other.mMaxBounds)) {
            changes |= 4;
        }
        if ((compareUndefined || other.mWindowingMode != 0) && this.mWindowingMode != other.mWindowingMode) {
            changes |= 8;
        }
        if ((compareUndefined || other.mActivityType != 0) && this.mActivityType != other.mActivityType) {
            changes |= 16;
        }
        if ((compareUndefined || other.mAlwaysOnTop != 0) && this.mAlwaysOnTop != other.mAlwaysOnTop) {
            changes |= 32;
        }
        if ((compareUndefined || other.mRotation != -1) && this.mRotation != other.mRotation) {
            changes |= 64;
        }
        if ((compareUndefined || other.mDisplayWindowingMode != 0) && this.mDisplayWindowingMode != other.mDisplayWindowingMode) {
            changes |= 128;
        }
        if ((compareUndefined || other.mDisplayRotation != -1) && this.mDisplayRotation != other.mDisplayRotation) {
            changes |= 256;
        }
        return ITranWindowConfiguration.Instance().diff(other, this, compareUndefined, changes);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // java.lang.Comparable
    public int compareTo(WindowConfiguration that) {
        Rect rect = this.mAppBounds;
        if (rect == null && that.mAppBounds != null) {
            return 1;
        }
        if (rect != null && that.mAppBounds == null) {
            return -1;
        }
        if (rect != null && that.mAppBounds != null) {
            int n = rect.left - that.mAppBounds.left;
            if (n != 0) {
                return n;
            }
            int n2 = this.mAppBounds.top - that.mAppBounds.top;
            if (n2 != 0) {
                return n2;
            }
            int n3 = this.mAppBounds.right - that.mAppBounds.right;
            if (n3 != 0) {
                return n3;
            }
            int n4 = this.mAppBounds.bottom - that.mAppBounds.bottom;
            if (n4 != 0) {
                return n4;
            }
        }
        int n5 = this.mMaxBounds.left - that.mMaxBounds.left;
        if (n5 != 0) {
            return n5;
        }
        int n6 = this.mMaxBounds.top - that.mMaxBounds.top;
        if (n6 != 0) {
            return n6;
        }
        int n7 = this.mMaxBounds.right - that.mMaxBounds.right;
        if (n7 != 0) {
            return n7;
        }
        int n8 = this.mMaxBounds.bottom - that.mMaxBounds.bottom;
        if (n8 != 0) {
            return n8;
        }
        int n9 = this.mBounds.left - that.mBounds.left;
        if (n9 != 0) {
            return n9;
        }
        int n10 = this.mBounds.top - that.mBounds.top;
        if (n10 != 0) {
            return n10;
        }
        int n11 = this.mBounds.right - that.mBounds.right;
        if (n11 != 0) {
            return n11;
        }
        int n12 = this.mBounds.bottom - that.mBounds.bottom;
        if (n12 != 0) {
            return n12;
        }
        int n13 = this.mWindowingMode - that.mWindowingMode;
        if (n13 != 0) {
            return n13;
        }
        int n14 = this.mActivityType - that.mActivityType;
        if (n14 != 0) {
            return n14;
        }
        int n15 = this.mAlwaysOnTop - that.mAlwaysOnTop;
        if (n15 != 0) {
            return n15;
        }
        int n16 = this.mRotation - that.mRotation;
        if (n16 != 0) {
            return n16;
        }
        int n17 = this.mDisplayWindowingMode - that.mDisplayWindowingMode;
        if (n17 != 0) {
            return n17;
        }
        int n18 = this.mDisplayRotation - that.mDisplayRotation;
        return n18 != 0 ? n18 : ITranWindowConfiguration.Instance().compareTo(this, that);
    }

    public boolean equals(Object that) {
        if (that == null) {
            return false;
        }
        if (that == this) {
            return true;
        }
        if (!(that instanceof WindowConfiguration) || compareTo((WindowConfiguration) that) != 0) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int result = (0 * 31) + Objects.hashCode(this.mAppBounds);
        return (((((((((((((((((((((((((result * 31) + Objects.hashCode(this.mBounds)) * 31) + Objects.hashCode(this.mMaxBounds)) * 31) + this.mWindowingMode) * 31) + this.mActivityType) * 31) + this.mAlwaysOnTop) * 31) + this.mRotation) * 31) + this.mDisplayWindowingMode) * 31) + this.mDisplayRotation) * 31) + this.mMultiWindowMode) * 31) + this.mMultiWindowId) * 31) + this.mWindowResizable) * 31) + this.mInLargeScreen) * 31) + Objects.hashCode(Long.valueOf(this.mDisplayPhysicalId));
    }

    public String toString() {
        StringBuilder append = new StringBuilder().append("{ mBounds=").append(this.mBounds).append(" mAppBounds=").append(this.mAppBounds).append(" mMaxBounds=").append(this.mMaxBounds).append(" mDisplayRotation=").append(this.mRotation == -1 ? "undefined" : Surface.rotationToString(this.mDisplayRotation)).append(" mWindowingMode=").append(windowingModeToString(this.mWindowingMode)).append(" mDisplayWindowingMode=").append(windowingModeToString(this.mDisplayWindowingMode)).append(" mMultiWindowMode=").append(multiWindowModeToString(this.mMultiWindowMode)).append(" mMultiWindowId=").append(this.mMultiWindowId).append(" mWindowResiable=").append(this.mWindowResizable).append(" mInLargeScreen=").append(this.mInLargeScreen).append(" mActivityType=").append(activityTypeToString(this.mActivityType)).append(" mAlwaysOnTop=").append(alwaysOnTopToString(this.mAlwaysOnTop)).append(" mRotation=");
        int i = this.mRotation;
        return append.append(i != -1 ? Surface.rotationToString(i) : "undefined").append(" mDisplayUniqueId=").append(this.mDisplayPhysicalId).append("}").toString();
    }

    public void dumpDebug(ProtoOutputStream protoOutputStream, long fieldId) {
        long token = protoOutputStream.start(fieldId);
        Rect rect = this.mAppBounds;
        if (rect != null) {
            rect.dumpDebug(protoOutputStream, 1146756268033L);
        }
        protoOutputStream.write(1120986464258L, this.mWindowingMode);
        protoOutputStream.write(1120986464259L, this.mActivityType);
        this.mBounds.dumpDebug(protoOutputStream, 1146756268036L);
        this.mMaxBounds.dumpDebug(protoOutputStream, 1146756268037L);
        protoOutputStream.end(token);
    }

    public void readFromProto(ProtoInputStream proto, long fieldId) throws IOException, WireTypeMismatchException {
        long token = proto.start(fieldId);
        while (proto.nextField() != -1) {
            try {
                switch (proto.getFieldNumber()) {
                    case 1:
                        Rect rect = new Rect();
                        this.mAppBounds = rect;
                        rect.readFromProto(proto, 1146756268033L);
                        break;
                    case 2:
                        this.mWindowingMode = proto.readInt(1120986464258L);
                        break;
                    case 3:
                        this.mActivityType = proto.readInt(1120986464259L);
                        break;
                    case 4:
                        this.mBounds.readFromProto(proto, 1146756268036L);
                        break;
                    case 5:
                        this.mMaxBounds.readFromProto(proto, 1146756268037L);
                        break;
                }
            } finally {
                proto.end(token);
            }
        }
    }

    public boolean hasWindowShadow() {
        return this.mWindowingMode != 6 && tasksAreFloating();
    }

    public boolean hasWindowDecorCaptionV3() {
        return (this.mActivityType == 1 && (this.mWindowingMode == 5 || this.mDisplayWindowingMode == 5)) || this.mIsThunderbackActivity;
    }

    public boolean hasWindowDecorCaptionV4() {
        return (this.mActivityType == 1 && (this.mWindowingMode == 5 || this.mDisplayWindowingMode == 5)) || isThunderbackWindow();
    }

    public boolean hasWindowDecorCaption() {
        if (ThunderbackConfig.isVersion3()) {
            return hasWindowDecorCaptionV3();
        }
        if (hasWindowDecorCaptionV4()) {
            return hasWindowDecorCaptionV4();
        }
        return false;
    }

    public boolean canResizeTask() {
        int i = this.mWindowingMode;
        return i == 5 || i == 6;
    }

    public boolean persistTaskBounds() {
        return this.mWindowingMode == 5;
    }

    public boolean tasksAreFloating() {
        return isFloating(this.mWindowingMode);
    }

    public static boolean isFloating(int windowingMode) {
        return windowingMode == 5 || windowingMode == 2;
    }

    public static boolean inMultiWindowMode(int windowingMode) {
        return (windowingMode == 1 || windowingMode == 0) ? false : true;
    }

    public boolean canReceiveKeys() {
        return this.mWindowingMode != 2;
    }

    public boolean isAlwaysOnTop() {
        int i = this.mWindowingMode;
        if (i == 2 || this.mActivityType == 5) {
            return true;
        }
        if (this.mAlwaysOnTop != 1) {
            return false;
        }
        return i == 5 || i == 6;
    }

    public boolean keepVisibleDeadAppWindowOnScreen() {
        return this.mWindowingMode != 2;
    }

    public boolean useWindowFrameForBackdrop() {
        int i = this.mWindowingMode;
        return i == 5 || i == 2;
    }

    public boolean hasMovementAnimations() {
        return this.mWindowingMode != 2;
    }

    public boolean supportSplitScreenWindowingMode() {
        return supportSplitScreenWindowingMode(this.mActivityType);
    }

    public static boolean supportSplitScreenWindowingMode(int activityType) {
        return (activityType == 4 || activityType == 5) ? false : true;
    }

    public static String windowingModeToString(int windowingMode) {
        switch (windowingMode) {
            case 0:
                return "undefined";
            case 1:
                return "fullscreen";
            case 2:
                return ContactsContract.ContactOptionsColumns.PINNED;
            case 3:
                return "split-screen-primary";
            case 4:
                return "split-screen-secondary";
            case 5:
                return "freeform";
            case 6:
                return "multi-window";
            default:
                return String.valueOf(windowingMode);
        }
    }

    public static String activityTypeToString(int applicationType) {
        switch (applicationType) {
            case 0:
                return "undefined";
            case 1:
                return "standard";
            case 2:
                return "home";
            case 3:
                return "recents";
            case 4:
                return Settings.Secure.ASSISTANT;
            case 5:
                return Context.DREAM_SERVICE;
            default:
                return String.valueOf(applicationType);
        }
    }

    public static String alwaysOnTopToString(int alwaysOnTop) {
        switch (alwaysOnTop) {
            case 0:
                return "undefined";
            case 1:
                return "on";
            case 2:
                return "off";
            default:
                return String.valueOf(alwaysOnTop);
        }
    }

    public void setDisplayPhysicalId(long id) {
        this.mDisplayPhysicalId = id;
    }

    public long getDisplayPhysicalId() {
        return this.mDisplayPhysicalId;
    }

    public void setMultiWindowMode(int multiWindowMode) {
        this.mMultiWindowMode = multiWindowMode;
    }

    public int getMultiWindowingMode() {
        return this.mMultiWindowMode;
    }

    public void setMultiWindowId(int multiWindowId) {
        this.mMultiWindowId = multiWindowId;
    }

    public int getMultiWindowingId() {
        return this.mMultiWindowId;
    }

    public void setWindowResizable(int windowResizable) {
        this.mWindowResizable = windowResizable;
    }

    public int getWindowResizable() {
        return this.mWindowResizable;
    }

    public void setInLargeScreen(int largeScreen) {
        this.mInLargeScreen = largeScreen;
    }

    public int getInLargeScreen() {
        return this.mInLargeScreen;
    }

    public boolean isThunderbackWindow() {
        return ITranWindowConfiguration.Instance().isThunderbackWindow(this);
    }

    public boolean isSplitScreenWindow() {
        return (this.mMultiWindowMode & 98304) != 0;
    }

    public boolean isThunderbackWindowInteractive() {
        return ITranWindowConfiguration.Instance().isThunderbackWindowInteractive(this);
    }

    public boolean isThunderbackWindowNonInteractive() {
        return ITranWindowConfiguration.Instance().isThunderbackWindowNonInteractive(this);
    }

    public static String multiWindowModeToString(int multiWindowMode) {
        return ITranWindowConfiguration.Instance().multiWindowModeToString(multiWindowMode);
    }
}
