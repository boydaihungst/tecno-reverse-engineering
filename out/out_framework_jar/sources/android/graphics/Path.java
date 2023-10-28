package android.graphics;

import android.graphics.Region;
import dalvik.annotation.optimization.CriticalNative;
import dalvik.annotation.optimization.FastNative;
import libcore.util.NativeAllocationRegistry;
/* loaded from: classes.dex */
public class Path {
    public boolean isSimplePath;
    private Direction mLastDirection;
    public final long mNativePath;
    public Region rects;
    private static final NativeAllocationRegistry sRegistry = NativeAllocationRegistry.createMalloced(Path.class.getClassLoader(), nGetFinalizer());
    static final FillType[] sFillTypeArray = {FillType.WINDING, FillType.EVEN_ODD, FillType.INVERSE_WINDING, FillType.INVERSE_EVEN_ODD};

    /* loaded from: classes.dex */
    public enum Op {
        DIFFERENCE,
        INTERSECT,
        UNION,
        XOR,
        REVERSE_DIFFERENCE
    }

    private static native void nAddArc(long j, float f, float f2, float f3, float f4, float f5, float f6);

    private static native void nAddCircle(long j, float f, float f2, float f3, int i);

    private static native void nAddOval(long j, float f, float f2, float f3, float f4, int i);

    private static native void nAddPath(long j, long j2);

    private static native void nAddPath(long j, long j2, float f, float f2);

    private static native void nAddPath(long j, long j2, long j3);

    private static native void nAddRect(long j, float f, float f2, float f3, float f4, int i);

    private static native void nAddRoundRect(long j, float f, float f2, float f3, float f4, float f5, float f6, int i);

    private static native void nAddRoundRect(long j, float f, float f2, float f3, float f4, float[] fArr, int i);

    private static native float[] nApproximate(long j, float f);

    private static native void nArcTo(long j, float f, float f2, float f3, float f4, float f5, float f6, boolean z);

    private static native void nClose(long j);

    private static native void nComputeBounds(long j, RectF rectF);

    private static native void nCubicTo(long j, float f, float f2, float f3, float f4, float f5, float f6);

    @CriticalNative
    private static native int nGetFillType(long j);

    private static native long nGetFinalizer();

    private static native void nIncReserve(long j, int i);

    private static native long nInit();

    private static native long nInit(long j);

    @CriticalNative
    private static native boolean nIsConvex(long j);

    @CriticalNative
    private static native boolean nIsEmpty(long j);

    @FastNative
    private static native boolean nIsRect(long j, RectF rectF);

    private static native void nLineTo(long j, float f, float f2);

    private static native void nMoveTo(long j, float f, float f2);

    private static native void nOffset(long j, float f, float f2);

    private static native boolean nOp(long j, long j2, int i, long j3);

    private static native void nQuadTo(long j, float f, float f2, float f3, float f4);

    private static native void nRCubicTo(long j, float f, float f2, float f3, float f4, float f5, float f6);

    private static native void nRLineTo(long j, float f, float f2);

    private static native void nRMoveTo(long j, float f, float f2);

    private static native void nRQuadTo(long j, float f, float f2, float f3, float f4);

    @CriticalNative
    private static native void nReset(long j);

    @CriticalNative
    private static native void nRewind(long j);

    private static native void nSet(long j, long j2);

    @CriticalNative
    private static native void nSetFillType(long j, int i);

    private static native void nSetLastPoint(long j, float f, float f2);

    private static native void nTransform(long j, long j2);

    private static native void nTransform(long j, long j2, long j3);

    public Path() {
        this.isSimplePath = true;
        this.mLastDirection = null;
        long nInit = nInit();
        this.mNativePath = nInit;
        sRegistry.registerNativeAllocation(this, nInit);
    }

    public Path(Path src) {
        this.isSimplePath = true;
        this.mLastDirection = null;
        long valNative = 0;
        if (src != null) {
            valNative = src.mNativePath;
            this.isSimplePath = src.isSimplePath;
            if (src.rects != null) {
                this.rects = new Region(src.rects);
            }
        }
        long nInit = nInit(valNative);
        this.mNativePath = nInit;
        sRegistry.registerNativeAllocation(this, nInit);
    }

    public void reset() {
        this.isSimplePath = true;
        this.mLastDirection = null;
        Region region = this.rects;
        if (region != null) {
            region.setEmpty();
        }
        FillType fillType = getFillType();
        nReset(this.mNativePath);
        setFillType(fillType);
    }

    public void rewind() {
        this.isSimplePath = true;
        this.mLastDirection = null;
        Region region = this.rects;
        if (region != null) {
            region.setEmpty();
        }
        nRewind(this.mNativePath);
    }

    public void set(Path src) {
        Region region;
        if (this == src) {
            return;
        }
        this.isSimplePath = src.isSimplePath;
        nSet(this.mNativePath, src.mNativePath);
        if (!this.isSimplePath) {
            return;
        }
        Region region2 = this.rects;
        if (region2 != null && (region = src.rects) != null) {
            region2.set(region);
        } else if (region2 != null && src.rects == null) {
            region2.setEmpty();
        } else if (src.rects != null) {
            this.rects = new Region(src.rects);
        }
    }

    public boolean op(Path path, Op op) {
        return op(this, path, op);
    }

    public boolean op(Path path1, Path path2, Op op) {
        if (nOp(path1.mNativePath, path2.mNativePath, op.ordinal(), this.mNativePath)) {
            this.isSimplePath = false;
            this.rects = null;
            return true;
        }
        return false;
    }

    @Deprecated
    public boolean isConvex() {
        return nIsConvex(this.mNativePath);
    }

    /* loaded from: classes.dex */
    public enum FillType {
        WINDING(0),
        EVEN_ODD(1),
        INVERSE_WINDING(2),
        INVERSE_EVEN_ODD(3);
        
        final int nativeInt;

        FillType(int ni) {
            this.nativeInt = ni;
        }
    }

    public FillType getFillType() {
        return sFillTypeArray[nGetFillType(this.mNativePath)];
    }

    public void setFillType(FillType ft) {
        nSetFillType(this.mNativePath, ft.nativeInt);
    }

    public boolean isInverseFillType() {
        int ft = nGetFillType(this.mNativePath);
        return (FillType.INVERSE_WINDING.nativeInt & ft) != 0;
    }

    public void toggleInverseFillType() {
        int ft = nGetFillType(this.mNativePath);
        nSetFillType(this.mNativePath, ft ^ FillType.INVERSE_WINDING.nativeInt);
    }

    public boolean isEmpty() {
        return nIsEmpty(this.mNativePath);
    }

    public boolean isRect(RectF rect) {
        return nIsRect(this.mNativePath, rect);
    }

    public void computeBounds(RectF bounds, boolean exact) {
        nComputeBounds(this.mNativePath, bounds);
    }

    public void incReserve(int extraPtCount) {
        nIncReserve(this.mNativePath, extraPtCount);
    }

    public void moveTo(float x, float y) {
        nMoveTo(this.mNativePath, x, y);
    }

    public void rMoveTo(float dx, float dy) {
        nRMoveTo(this.mNativePath, dx, dy);
    }

    public void lineTo(float x, float y) {
        this.isSimplePath = false;
        nLineTo(this.mNativePath, x, y);
    }

    public void rLineTo(float dx, float dy) {
        this.isSimplePath = false;
        nRLineTo(this.mNativePath, dx, dy);
    }

    public void quadTo(float x1, float y1, float x2, float y2) {
        this.isSimplePath = false;
        nQuadTo(this.mNativePath, x1, y1, x2, y2);
    }

    public void rQuadTo(float dx1, float dy1, float dx2, float dy2) {
        this.isSimplePath = false;
        nRQuadTo(this.mNativePath, dx1, dy1, dx2, dy2);
    }

    public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        this.isSimplePath = false;
        nCubicTo(this.mNativePath, x1, y1, x2, y2, x3, y3);
    }

    public void rCubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        this.isSimplePath = false;
        nRCubicTo(this.mNativePath, x1, y1, x2, y2, x3, y3);
    }

    public void arcTo(RectF oval, float startAngle, float sweepAngle, boolean forceMoveTo) {
        arcTo(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle, forceMoveTo);
    }

    public void arcTo(RectF oval, float startAngle, float sweepAngle) {
        arcTo(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle, false);
    }

    public void arcTo(float left, float top, float right, float bottom, float startAngle, float sweepAngle, boolean forceMoveTo) {
        this.isSimplePath = false;
        nArcTo(this.mNativePath, left, top, right, bottom, startAngle, sweepAngle, forceMoveTo);
    }

    public void close() {
        this.isSimplePath = false;
        nClose(this.mNativePath);
    }

    /* loaded from: classes.dex */
    public enum Direction {
        CW(0),
        CCW(1);
        
        final int nativeInt;

        Direction(int ni) {
            this.nativeInt = ni;
        }
    }

    private void detectSimplePath(float left, float top, float right, float bottom, Direction dir) {
        if (this.mLastDirection == null) {
            this.mLastDirection = dir;
        }
        if (this.mLastDirection != dir) {
            this.isSimplePath = false;
            return;
        }
        if (this.rects == null) {
            this.rects = new Region();
        }
        this.rects.op((int) left, (int) top, (int) right, (int) bottom, Region.Op.UNION);
    }

    public void addRect(RectF rect, Direction dir) {
        addRect(rect.left, rect.top, rect.right, rect.bottom, dir);
    }

    public void addRect(float left, float top, float right, float bottom, Direction dir) {
        detectSimplePath(left, top, right, bottom, dir);
        nAddRect(this.mNativePath, left, top, right, bottom, dir.nativeInt);
    }

    public void addOval(RectF oval, Direction dir) {
        addOval(oval.left, oval.top, oval.right, oval.bottom, dir);
    }

    public void addOval(float left, float top, float right, float bottom, Direction dir) {
        this.isSimplePath = false;
        nAddOval(this.mNativePath, left, top, right, bottom, dir.nativeInt);
    }

    public void addCircle(float x, float y, float radius, Direction dir) {
        this.isSimplePath = false;
        nAddCircle(this.mNativePath, x, y, radius, dir.nativeInt);
    }

    public void addArc(RectF oval, float startAngle, float sweepAngle) {
        addArc(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle);
    }

    public void addArc(float left, float top, float right, float bottom, float startAngle, float sweepAngle) {
        this.isSimplePath = false;
        nAddArc(this.mNativePath, left, top, right, bottom, startAngle, sweepAngle);
    }

    public void addRoundRect(RectF rect, float rx, float ry, Direction dir) {
        addRoundRect(rect.left, rect.top, rect.right, rect.bottom, rx, ry, dir);
    }

    public void addRoundRect(float left, float top, float right, float bottom, float rx, float ry, Direction dir) {
        this.isSimplePath = false;
        nAddRoundRect(this.mNativePath, left, top, right, bottom, rx, ry, dir.nativeInt);
    }

    public void addRoundRect(RectF rect, float[] radii, Direction dir) {
        if (rect == null) {
            throw new NullPointerException("need rect parameter");
        }
        addRoundRect(rect.left, rect.top, rect.right, rect.bottom, radii, dir);
    }

    public void addRoundRect(float left, float top, float right, float bottom, float[] radii, Direction dir) {
        if (radii.length < 8) {
            throw new ArrayIndexOutOfBoundsException("radii[] needs 8 values");
        }
        this.isSimplePath = false;
        nAddRoundRect(this.mNativePath, left, top, right, bottom, radii, dir.nativeInt);
    }

    public void addPath(Path src, float dx, float dy) {
        this.isSimplePath = false;
        nAddPath(this.mNativePath, src.mNativePath, dx, dy);
    }

    public void addPath(Path src) {
        this.isSimplePath = false;
        nAddPath(this.mNativePath, src.mNativePath);
    }

    public void addPath(Path src, Matrix matrix) {
        if (!src.isSimplePath) {
            this.isSimplePath = false;
        }
        nAddPath(this.mNativePath, src.mNativePath, matrix.ni());
    }

    public void offset(float dx, float dy, Path dst) {
        if (dst != null) {
            dst.set(this);
        } else {
            dst = this;
        }
        dst.offset(dx, dy);
    }

    public void offset(float dx, float dy) {
        boolean z = this.isSimplePath;
        if (z && this.rects == null) {
            return;
        }
        if (z && dx == Math.rint(dx) && dy == Math.rint(dy)) {
            this.rects.translate((int) dx, (int) dy);
        } else {
            this.isSimplePath = false;
        }
        nOffset(this.mNativePath, dx, dy);
    }

    public void setLastPoint(float dx, float dy) {
        this.isSimplePath = false;
        nSetLastPoint(this.mNativePath, dx, dy);
    }

    public void transform(Matrix matrix, Path dst) {
        long dstNative = 0;
        if (dst != null) {
            dst.isSimplePath = false;
            dstNative = dst.mNativePath;
        }
        nTransform(this.mNativePath, matrix.ni(), dstNative);
    }

    public void transform(Matrix matrix) {
        this.isSimplePath = false;
        nTransform(this.mNativePath, matrix.ni());
    }

    public final long readOnlyNI() {
        return this.mNativePath;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final long mutateNI() {
        this.isSimplePath = false;
        return this.mNativePath;
    }

    public float[] approximate(float acceptableError) {
        if (acceptableError < 0.0f) {
            throw new IllegalArgumentException("AcceptableError must be greater than or equal to 0");
        }
        return nApproximate(this.mNativePath, acceptableError);
    }
}
