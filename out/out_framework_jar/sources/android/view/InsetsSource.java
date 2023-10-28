package android.view;

import android.graphics.Insets;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.proto.ProtoOutputStream;
import java.io.PrintWriter;
import java.util.Objects;
/* loaded from: classes3.dex */
public class InsetsSource implements Parcelable {
    public static final Parcelable.Creator<InsetsSource> CREATOR = new Parcelable.Creator<InsetsSource>() { // from class: android.view.InsetsSource.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public InsetsSource createFromParcel(Parcel in) {
            return new InsetsSource(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public InsetsSource[] newArray(int size) {
            return new InsetsSource[size];
        }
    };
    private final Rect mFrame;
    private boolean mInsetsRoundedCornerFrame;
    private final Rect mTmpFrame;
    private final int mType;
    private boolean mVisible;
    private Rect mVisibleFrame;

    public InsetsSource(int type) {
        this.mTmpFrame = new Rect();
        this.mType = type;
        this.mFrame = new Rect();
        this.mVisible = InsetsState.getDefaultVisibility(type);
    }

    public InsetsSource(InsetsSource other) {
        Rect rect;
        this.mTmpFrame = new Rect();
        this.mType = other.mType;
        this.mFrame = new Rect(other.mFrame);
        this.mVisible = other.mVisible;
        if (other.mVisibleFrame != null) {
            rect = new Rect(other.mVisibleFrame);
        } else {
            rect = null;
        }
        this.mVisibleFrame = rect;
        this.mInsetsRoundedCornerFrame = other.mInsetsRoundedCornerFrame;
    }

    public void set(InsetsSource other) {
        Rect rect;
        this.mFrame.set(other.mFrame);
        this.mVisible = other.mVisible;
        if (other.mVisibleFrame != null) {
            rect = new Rect(other.mVisibleFrame);
        } else {
            rect = null;
        }
        this.mVisibleFrame = rect;
        this.mInsetsRoundedCornerFrame = other.mInsetsRoundedCornerFrame;
    }

    public void setFrame(int left, int top, int right, int bottom) {
        this.mFrame.set(left, top, right, bottom);
    }

    public void setFrame(Rect frame) {
        this.mFrame.set(frame);
    }

    public void setVisibleFrame(Rect visibleFrame) {
        this.mVisibleFrame = visibleFrame != null ? new Rect(visibleFrame) : visibleFrame;
    }

    public void setVisible(boolean visible) {
        this.mVisible = visible;
    }

    public int getType() {
        return this.mType;
    }

    public Rect getFrame() {
        return this.mFrame;
    }

    public Rect getVisibleFrame() {
        return this.mVisibleFrame;
    }

    public boolean isVisible() {
        return this.mVisible;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isUserControllable() {
        Rect rect = this.mVisibleFrame;
        return rect == null || !rect.isEmpty();
    }

    public boolean getInsetsRoundedCornerFrame() {
        return this.mInsetsRoundedCornerFrame;
    }

    public void setInsetsRoundedCornerFrame(boolean insetsRoundedCornerFrame) {
        this.mInsetsRoundedCornerFrame = insetsRoundedCornerFrame;
    }

    public Insets calculateInsets(Rect relativeFrame, boolean ignoreVisibility) {
        return calculateInsets(relativeFrame, this.mFrame, ignoreVisibility);
    }

    public Insets calculateVisibleInsets(Rect relativeFrame) {
        Rect rect = this.mVisibleFrame;
        if (rect == null) {
            rect = this.mFrame;
        }
        return calculateInsets(relativeFrame, rect, false);
    }

    private Insets calculateInsets(Rect relativeFrame, Rect frame, boolean ignoreVisibility) {
        boolean hasIntersection;
        if (!ignoreVisibility && !this.mVisible) {
            return Insets.NONE;
        }
        if (getType() == 2) {
            return Insets.of(0, frame.height(), 0, 0);
        }
        if (relativeFrame.isEmpty()) {
            hasIntersection = getIntersection(frame, relativeFrame, this.mTmpFrame);
        } else {
            hasIntersection = this.mTmpFrame.setIntersect(frame, relativeFrame);
        }
        if (!hasIntersection) {
            return Insets.NONE;
        }
        if (getType() == 19) {
            return Insets.of(0, 0, 0, this.mTmpFrame.height());
        }
        if (this.mTmpFrame.width() == relativeFrame.width()) {
            if (this.mTmpFrame.top == relativeFrame.top) {
                return Insets.of(0, this.mTmpFrame.height(), 0, 0);
            }
            if (this.mTmpFrame.bottom == relativeFrame.bottom) {
                return Insets.of(0, 0, 0, this.mTmpFrame.height());
            }
            if (this.mTmpFrame.top == 0) {
                return Insets.of(0, this.mTmpFrame.height(), 0, 0);
            }
        } else if (this.mTmpFrame.height() == relativeFrame.height()) {
            if (this.mTmpFrame.left == relativeFrame.left) {
                return Insets.of(this.mTmpFrame.width(), 0, 0, 0);
            }
            if (this.mTmpFrame.right == relativeFrame.right) {
                return Insets.of(0, 0, this.mTmpFrame.width(), 0);
            }
        }
        return Insets.NONE;
    }

    private static boolean getIntersection(Rect a, Rect b, Rect out) {
        if (a.left <= b.right && b.left <= a.right && a.top <= b.bottom && b.top <= a.bottom) {
            out.left = Math.max(a.left, b.left);
            out.top = Math.max(a.top, b.top);
            out.right = Math.min(a.right, b.right);
            out.bottom = Math.min(a.bottom, b.bottom);
            return true;
        }
        out.setEmpty();
        return false;
    }

    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1138166333441L, InsetsState.typeToString(this.mType));
        this.mFrame.dumpDebug(proto, 1146756268034L);
        Rect rect = this.mVisibleFrame;
        if (rect != null) {
            rect.dumpDebug(proto, 1146756268035L);
        }
        proto.write(1133871366148L, this.mVisible);
        proto.end(token);
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("InsetsSource type=");
        pw.print(InsetsState.typeToString(this.mType));
        pw.print(" frame=");
        pw.print(this.mFrame.toShortString());
        if (this.mVisibleFrame != null) {
            pw.print(" visibleFrame=");
            pw.print(this.mVisibleFrame.toShortString());
        }
        pw.print(" visible=");
        pw.print(this.mVisible);
        pw.print(" insetsRoundedCornerFrame=");
        pw.print(this.mInsetsRoundedCornerFrame);
        pw.println();
    }

    public boolean equals(Object o) {
        return equals(o, false);
    }

    public boolean equals(Object o, boolean excludeInvisibleImeFrames) {
        boolean z;
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InsetsSource that = (InsetsSource) o;
        int i = this.mType;
        if (i != that.mType || (z = this.mVisible) != that.mVisible) {
            return false;
        }
        if (excludeInvisibleImeFrames && !z && i == 19) {
            return true;
        }
        if (!Objects.equals(this.mVisibleFrame, that.mVisibleFrame) || this.mInsetsRoundedCornerFrame != that.mInsetsRoundedCornerFrame) {
            return false;
        }
        return this.mFrame.equals(that.mFrame);
    }

    public int hashCode() {
        int result = this.mType;
        int result2 = ((result * 31) + this.mFrame.hashCode()) * 31;
        Rect rect = this.mVisibleFrame;
        return ((((result2 + (rect != null ? rect.hashCode() : 0)) * 31) + (this.mVisible ? 1 : 0)) * 31) + (this.mInsetsRoundedCornerFrame ? 1 : 0);
    }

    public InsetsSource(Parcel in) {
        this.mTmpFrame = new Rect();
        this.mType = in.readInt();
        this.mFrame = Rect.CREATOR.createFromParcel(in);
        if (in.readInt() != 0) {
            this.mVisibleFrame = Rect.CREATOR.createFromParcel(in);
        } else {
            this.mVisibleFrame = null;
        }
        this.mVisible = in.readBoolean();
        this.mInsetsRoundedCornerFrame = in.readBoolean();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mType);
        this.mFrame.writeToParcel(dest, 0);
        if (this.mVisibleFrame != null) {
            dest.writeInt(1);
            this.mVisibleFrame.writeToParcel(dest, 0);
        } else {
            dest.writeInt(0);
        }
        dest.writeBoolean(this.mVisible);
        dest.writeBoolean(this.mInsetsRoundedCornerFrame);
    }

    public String toString() {
        return "InsetsSource: {mType=" + InsetsState.typeToString(this.mType) + ", mFrame=" + this.mFrame.toShortString() + ", mVisible=" + this.mVisible + ", mInsetsRoundedCornerFrame=" + this.mInsetsRoundedCornerFrame + "}";
    }
}
