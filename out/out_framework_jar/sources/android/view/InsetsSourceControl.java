package android.view;

import android.graphics.Insets;
import android.graphics.Point;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.proto.ProtoOutputStream;
import java.io.PrintWriter;
import java.util.function.Consumer;
/* loaded from: classes3.dex */
public class InsetsSourceControl implements Parcelable {
    private Insets mInsetsHint;
    private final SurfaceControl mLeash;
    private int mParcelableFlags;
    private boolean mSkipAnimationOnce;
    private final Point mSurfacePosition;
    private final int mType;
    public static final Insets INVALID_HINTS = Insets.of(-1, -1, -1, -1);
    public static final Parcelable.Creator<InsetsSourceControl> CREATOR = new Parcelable.Creator<InsetsSourceControl>() { // from class: android.view.InsetsSourceControl.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public InsetsSourceControl createFromParcel(Parcel in) {
            return new InsetsSourceControl(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public InsetsSourceControl[] newArray(int size) {
            return new InsetsSourceControl[size];
        }
    };

    public InsetsSourceControl(int type, SurfaceControl leash, Point surfacePosition, Insets insetsHint) {
        this.mType = type;
        this.mLeash = leash;
        this.mSurfacePosition = surfacePosition;
        this.mInsetsHint = insetsHint;
    }

    public InsetsSourceControl(InsetsSourceControl other) {
        this.mType = other.mType;
        if (other.mLeash != null) {
            this.mLeash = new SurfaceControl(other.mLeash, "InsetsSourceControl");
        } else {
            this.mLeash = null;
        }
        this.mSurfacePosition = new Point(other.mSurfacePosition);
        this.mInsetsHint = other.mInsetsHint;
        this.mSkipAnimationOnce = other.getAndClearSkipAnimationOnce();
    }

    public InsetsSourceControl(Parcel in) {
        this.mType = in.readInt();
        this.mLeash = (SurfaceControl) in.readTypedObject(SurfaceControl.CREATOR);
        this.mSurfacePosition = (Point) in.readTypedObject(Point.CREATOR);
        this.mInsetsHint = (Insets) in.readTypedObject(Insets.CREATOR);
        this.mSkipAnimationOnce = in.readBoolean();
    }

    public int getType() {
        return this.mType;
    }

    public SurfaceControl getLeash() {
        return this.mLeash;
    }

    public boolean setSurfacePosition(int left, int top) {
        if (this.mSurfacePosition.equals(left, top)) {
            return false;
        }
        this.mSurfacePosition.set(left, top);
        return true;
    }

    public Point getSurfacePosition() {
        return this.mSurfacePosition;
    }

    public void setInsetsHint(Insets insets) {
        this.mInsetsHint = insets;
    }

    public void setInsetsHint(int left, int top, int right, int bottom) {
        this.mInsetsHint = Insets.of(left, top, right, bottom);
    }

    public Insets getInsetsHint() {
        return this.mInsetsHint;
    }

    public void setSkipAnimationOnce(boolean skipAnimation) {
        this.mSkipAnimationOnce = skipAnimation;
    }

    public boolean getAndClearSkipAnimationOnce() {
        boolean result = this.mSkipAnimationOnce;
        this.mSkipAnimationOnce = false;
        return result;
    }

    public void setParcelableFlags(int parcelableFlags) {
        this.mParcelableFlags = parcelableFlags;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mType);
        dest.writeTypedObject(this.mLeash, this.mParcelableFlags);
        dest.writeTypedObject(this.mSurfacePosition, this.mParcelableFlags);
        dest.writeTypedObject(this.mInsetsHint, this.mParcelableFlags);
        dest.writeBoolean(this.mSkipAnimationOnce);
    }

    public void release(Consumer<SurfaceControl> surfaceReleaseConsumer) {
        SurfaceControl surfaceControl = this.mLeash;
        if (surfaceControl != null) {
            surfaceReleaseConsumer.accept(surfaceControl);
        }
    }

    public boolean equals(Object o) {
        SurfaceControl surfaceControl;
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InsetsSourceControl that = (InsetsSourceControl) o;
        SurfaceControl thatLeash = that.mLeash;
        if (this.mType == that.mType && (((surfaceControl = this.mLeash) == thatLeash || (surfaceControl != null && thatLeash != null && surfaceControl.isSameSurface(thatLeash))) && this.mSurfacePosition.equals(that.mSurfacePosition) && this.mInsetsHint.equals(that.mInsetsHint) && this.mSkipAnimationOnce == that.mSkipAnimationOnce)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int result = this.mType;
        int i = result * 31;
        SurfaceControl surfaceControl = this.mLeash;
        int result2 = i + (surfaceControl != null ? surfaceControl.hashCode() : 0);
        return (((((result2 * 31) + this.mSurfacePosition.hashCode()) * 31) + this.mInsetsHint.hashCode()) * 31) + (this.mSkipAnimationOnce ? 1 : 0);
    }

    public String toString() {
        return "InsetsSourceControl: {type=" + InsetsState.typeToString(this.mType) + ", mSurfacePosition=" + this.mSurfacePosition + ", mInsetsHint=" + this.mInsetsHint + "}";
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("InsetsSourceControl type=");
        pw.print(InsetsState.typeToString(this.mType));
        pw.print(" mLeash=");
        pw.print(this.mLeash);
        pw.print(" mSurfacePosition=");
        pw.print(this.mSurfacePosition);
        pw.print(" mInsetsHint=");
        pw.print(this.mInsetsHint);
        pw.print(" mSkipAnimationOnce=");
        pw.print(this.mSkipAnimationOnce);
        pw.println();
    }

    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1138166333441L, InsetsState.typeToString(this.mType));
        long surfaceToken = proto.start(1146756268034L);
        proto.write(1120986464257L, this.mSurfacePosition.x);
        proto.write(1120986464258L, this.mSurfacePosition.y);
        proto.end(surfaceToken);
        SurfaceControl surfaceControl = this.mLeash;
        if (surfaceControl != null) {
            surfaceControl.dumpDebug(proto, 1146756268035L);
        }
        proto.end(token);
    }
}
