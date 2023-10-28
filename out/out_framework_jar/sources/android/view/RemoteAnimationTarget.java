package android.view;

import android.app.ActivityManager;
import android.app.WindowConfiguration;
import android.graphics.GraphicsProtos;
import android.graphics.Point;
import android.graphics.Rect;
import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.proto.ProtoOutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/* loaded from: classes3.dex */
public class RemoteAnimationTarget implements Parcelable {
    public static final Parcelable.Creator<RemoteAnimationTarget> CREATOR = new Parcelable.Creator<RemoteAnimationTarget>() { // from class: android.view.RemoteAnimationTarget.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public RemoteAnimationTarget createFromParcel(Parcel in) {
            return new RemoteAnimationTarget(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public RemoteAnimationTarget[] newArray(int size) {
            return new RemoteAnimationTarget[size];
        }
    };
    public static final int MODE_CHANGING = 2;
    public static final int MODE_CLOSING = 1;
    public static final int MODE_OPENING = 0;
    public boolean allowEnterPip;
    public int backgroundColor;
    public final Rect clipRect;
    public final Rect contentInsets;
    public boolean hasAnimatingParent;
    public boolean isNotInRecents;
    public final boolean isTranslucent;
    public final SurfaceControl leash;
    public final Rect localBounds;
    public final int mode;
    @Deprecated
    public final Point position;
    @Deprecated
    public final int prefixOrderIndex;
    public final Rect screenSpaceBounds;
    @Deprecated
    public final Rect sourceContainerBounds;
    public final Rect startBounds;
    public final SurfaceControl startLeash;
    public final int taskId;
    public ActivityManager.RunningTaskInfo taskInfo;
    public final WindowConfiguration windowConfiguration;
    public final int windowType;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes3.dex */
    public @interface Mode {
    }

    public RemoteAnimationTarget(int taskId, int mode, SurfaceControl leash, boolean isTranslucent, Rect clipRect, Rect contentInsets, int prefixOrderIndex, Point position, Rect localBounds, Rect screenSpaceBounds, WindowConfiguration windowConfig, boolean isNotInRecents, SurfaceControl startLeash, Rect startBounds, ActivityManager.RunningTaskInfo taskInfo, boolean allowEnterPip) {
        this(taskId, mode, leash, isTranslucent, clipRect, contentInsets, prefixOrderIndex, position, localBounds, screenSpaceBounds, windowConfig, isNotInRecents, startLeash, startBounds, taskInfo, allowEnterPip, -1);
    }

    public RemoteAnimationTarget(int taskId, int mode, SurfaceControl leash, boolean isTranslucent, Rect clipRect, Rect contentInsets, int prefixOrderIndex, Point position, Rect localBounds, Rect screenSpaceBounds, WindowConfiguration windowConfig, boolean isNotInRecents, SurfaceControl startLeash, Rect startBounds, ActivityManager.RunningTaskInfo taskInfo, boolean allowEnterPip, int windowType) {
        this.mode = mode;
        this.taskId = taskId;
        this.leash = leash;
        this.isTranslucent = isTranslucent;
        this.clipRect = new Rect(clipRect);
        this.contentInsets = new Rect(contentInsets);
        this.prefixOrderIndex = prefixOrderIndex;
        this.position = position == null ? new Point() : new Point(position);
        this.localBounds = new Rect(localBounds);
        this.sourceContainerBounds = new Rect(screenSpaceBounds);
        this.screenSpaceBounds = new Rect(screenSpaceBounds);
        this.windowConfiguration = windowConfig;
        this.isNotInRecents = isNotInRecents;
        this.startLeash = startLeash;
        this.startBounds = startBounds == null ? null : new Rect(startBounds);
        this.taskInfo = taskInfo;
        this.allowEnterPip = allowEnterPip;
        this.windowType = windowType;
    }

    public RemoteAnimationTarget(Parcel in) {
        this.taskId = in.readInt();
        this.mode = in.readInt();
        this.leash = (SurfaceControl) in.readTypedObject(SurfaceControl.CREATOR);
        this.isTranslucent = in.readBoolean();
        this.clipRect = (Rect) in.readTypedObject(Rect.CREATOR);
        this.contentInsets = (Rect) in.readTypedObject(Rect.CREATOR);
        this.prefixOrderIndex = in.readInt();
        this.position = (Point) in.readTypedObject(Point.CREATOR);
        this.localBounds = (Rect) in.readTypedObject(Rect.CREATOR);
        this.sourceContainerBounds = (Rect) in.readTypedObject(Rect.CREATOR);
        this.screenSpaceBounds = (Rect) in.readTypedObject(Rect.CREATOR);
        this.windowConfiguration = (WindowConfiguration) in.readTypedObject(WindowConfiguration.CREATOR);
        this.isNotInRecents = in.readBoolean();
        this.startLeash = (SurfaceControl) in.readTypedObject(SurfaceControl.CREATOR);
        this.startBounds = (Rect) in.readTypedObject(Rect.CREATOR);
        this.taskInfo = (ActivityManager.RunningTaskInfo) in.readTypedObject(ActivityManager.RunningTaskInfo.CREATOR);
        this.allowEnterPip = in.readBoolean();
        this.windowType = in.readInt();
        this.hasAnimatingParent = in.readBoolean();
        this.backgroundColor = in.readInt();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.taskId);
        dest.writeInt(this.mode);
        dest.writeTypedObject(this.leash, 0);
        dest.writeBoolean(this.isTranslucent);
        dest.writeTypedObject(this.clipRect, 0);
        dest.writeTypedObject(this.contentInsets, 0);
        dest.writeInt(this.prefixOrderIndex);
        dest.writeTypedObject(this.position, 0);
        dest.writeTypedObject(this.localBounds, 0);
        dest.writeTypedObject(this.sourceContainerBounds, 0);
        dest.writeTypedObject(this.screenSpaceBounds, 0);
        dest.writeTypedObject(this.windowConfiguration, 0);
        dest.writeBoolean(this.isNotInRecents);
        dest.writeTypedObject(this.startLeash, 0);
        dest.writeTypedObject(this.startBounds, 0);
        dest.writeTypedObject(this.taskInfo, 0);
        dest.writeBoolean(this.allowEnterPip);
        dest.writeInt(this.windowType);
        dest.writeBoolean(this.hasAnimatingParent);
        dest.writeInt(this.backgroundColor);
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("mode=");
        pw.print(this.mode);
        pw.print(" taskId=");
        pw.print(this.taskId);
        pw.print(" isTranslucent=");
        pw.print(this.isTranslucent);
        pw.print(" clipRect=");
        this.clipRect.printShortString(pw);
        pw.print(" contentInsets=");
        this.contentInsets.printShortString(pw);
        pw.print(" prefixOrderIndex=");
        pw.print(this.prefixOrderIndex);
        pw.print(" position=");
        printPoint(this.position, pw);
        pw.print(" sourceContainerBounds=");
        this.sourceContainerBounds.printShortString(pw);
        pw.print(" screenSpaceBounds=");
        this.screenSpaceBounds.printShortString(pw);
        pw.print(" localBounds=");
        this.localBounds.printShortString(pw);
        pw.println();
        pw.print(prefix);
        pw.print("windowConfiguration=");
        pw.println(this.windowConfiguration);
        pw.print(prefix);
        pw.print("leash=");
        pw.println(this.leash);
        pw.print(prefix);
        pw.print("taskInfo=");
        pw.println(this.taskInfo);
        pw.print(prefix);
        pw.print("allowEnterPip=");
        pw.println(this.allowEnterPip);
        pw.print(prefix);
        pw.print("windowType=");
        pw.print(this.windowType);
        pw.print(prefix);
        pw.print("hasAnimatingParent=");
        pw.print(this.hasAnimatingParent);
        pw.print(prefix);
        pw.print("backgroundColor=");
        pw.print(this.backgroundColor);
    }

    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1120986464257L, this.taskId);
        proto.write(1120986464258L, this.mode);
        this.leash.dumpDebug(proto, 1146756268035L);
        proto.write(1133871366148L, this.isTranslucent);
        this.clipRect.dumpDebug(proto, 1146756268037L);
        this.contentInsets.dumpDebug(proto, 1146756268038L);
        proto.write(1120986464263L, this.prefixOrderIndex);
        GraphicsProtos.dumpPointProto(this.position, proto, 1146756268040L);
        this.sourceContainerBounds.dumpDebug(proto, 1146756268041L);
        this.screenSpaceBounds.dumpDebug(proto, 1146756268046L);
        this.localBounds.dumpDebug(proto, 1146756268045L);
        this.windowConfiguration.dumpDebug(proto, 1146756268042L);
        SurfaceControl surfaceControl = this.startLeash;
        if (surfaceControl != null) {
            surfaceControl.dumpDebug(proto, 1146756268043L);
        }
        Rect rect = this.startBounds;
        if (rect != null) {
            rect.dumpDebug(proto, 1146756268044L);
        }
        proto.end(token);
    }

    private static void printPoint(Point p, PrintWriter pw) {
        pw.print(NavigationBarInflaterView.SIZE_MOD_START);
        pw.print(p.x);
        pw.print(",");
        pw.print(p.y);
        pw.print(NavigationBarInflaterView.SIZE_MOD_END);
    }
}
