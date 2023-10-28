package android.util;

import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
/* loaded from: classes3.dex */
public abstract class Spline {
    public abstract float interpolate(float f);

    public static Spline createSpline(float[] x, float[] y) {
        if (!isStrictlyIncreasing(x)) {
            throw new IllegalArgumentException("The control points must all have strictly increasing X values.");
        }
        if (isMonotonic(y)) {
            return createMonotoneCubicSpline(x, y);
        }
        return createLinearSpline(x, y);
    }

    public static Spline createMonotoneCubicSpline(float[] x, float[] y) {
        return new MonotoneCubicSpline(x, y);
    }

    public static Spline createLinearSpline(float[] x, float[] y) {
        return new LinearSpline(x, y);
    }

    private static boolean isStrictlyIncreasing(float[] x) {
        if (x == null || x.length < 2) {
            throw new IllegalArgumentException("There must be at least two control points.");
        }
        float prev = x[0];
        for (int i = 1; i < x.length; i++) {
            float curr = x[i];
            if (curr <= prev) {
                return false;
            }
            prev = curr;
        }
        return true;
    }

    private static boolean isMonotonic(float[] x) {
        if (x == null || x.length < 2) {
            throw new IllegalArgumentException("There must be at least two control points.");
        }
        float prev = x[0];
        for (int i = 1; i < x.length; i++) {
            float curr = x[i];
            if (curr < prev) {
                return false;
            }
            prev = curr;
        }
        return true;
    }

    /* loaded from: classes3.dex */
    public static class MonotoneCubicSpline extends Spline {
        private float[] mM;
        private float[] mX;
        private float[] mY;

        public MonotoneCubicSpline(float[] x, float[] y) {
            if (x == null || y == null || x.length != y.length || x.length < 2) {
                throw new IllegalArgumentException("There must be at least two control points and the arrays must be of equal length.");
            }
            int n = x.length;
            float[] d = new float[n - 1];
            float[] m = new float[n];
            for (int i = 0; i < n - 1; i++) {
                float h = x[i + 1] - x[i];
                if (h <= 0.0f) {
                    throw new IllegalArgumentException("The control points must all have strictly increasing X values.");
                }
                d[i] = (y[i + 1] - y[i]) / h;
            }
            m[0] = d[0];
            for (int i2 = 1; i2 < n - 1; i2++) {
                m[i2] = (d[i2 - 1] + d[i2]) * 0.5f;
            }
            int i3 = n - 1;
            m[i3] = d[n - 2];
            for (int i4 = 0; i4 < n - 1; i4++) {
                if (d[i4] == 0.0f) {
                    m[i4] = 0.0f;
                    m[i4 + 1] = 0.0f;
                } else {
                    float a = m[i4] / d[i4];
                    float b = m[i4 + 1] / d[i4];
                    if (a < 0.0f || b < 0.0f) {
                        throw new IllegalArgumentException("The control points must have monotonic Y values.");
                    }
                    float h2 = (float) Math.hypot(a, b);
                    if (h2 > 3.0f) {
                        float t = 3.0f / h2;
                        m[i4] = m[i4] * t;
                        int i5 = i4 + 1;
                        m[i5] = m[i5] * t;
                    }
                }
            }
            this.mX = x;
            this.mY = y;
            this.mM = m;
        }

        @Override // android.util.Spline
        public float interpolate(float x) {
            float[] fArr;
            int n = this.mX.length;
            if (Float.isNaN(x)) {
                return x;
            }
            float[] fArr2 = this.mX;
            if (x <= fArr2[0]) {
                return this.mY[0];
            }
            if (x >= fArr2[n - 1]) {
                return this.mY[n - 1];
            }
            int i = 0;
            do {
                fArr = this.mX;
                if (x >= fArr[i + 1]) {
                    i++;
                } else {
                    float f = fArr[i + 1];
                    float f2 = fArr[i];
                    float h = f - f2;
                    float t = (x - f2) / h;
                    float[] fArr3 = this.mY;
                    float f3 = fArr3[i] * ((t * 2.0f) + 1.0f);
                    float[] fArr4 = this.mM;
                    return ((f3 + (fArr4[i] * h * t)) * (1.0f - t) * (1.0f - t)) + (((fArr3[i + 1] * (3.0f - (2.0f * t))) + (fArr4[i + 1] * h * (t - 1.0f))) * t * t);
                }
            } while (x != fArr[i]);
            return this.mY[i];
        }

        public String toString() {
            StringBuilder str = new StringBuilder();
            int n = this.mX.length;
            str.append("MonotoneCubicSpline{[");
            for (int i = 0; i < n; i++) {
                if (i != 0) {
                    str.append(", ");
                }
                str.append(NavigationBarInflaterView.KEY_CODE_START).append(this.mX[i]);
                str.append(", ").append(this.mY[i]);
                str.append(": ").append(this.mM[i]).append(NavigationBarInflaterView.KEY_CODE_END);
            }
            str.append("]}");
            return str.toString();
        }
    }

    /* loaded from: classes3.dex */
    public static class LinearSpline extends Spline {
        private final float[] mM;
        private final float[] mX;
        private final float[] mY;

        public LinearSpline(float[] x, float[] y) {
            if (x == null || y == null || x.length != y.length || x.length < 2) {
                throw new IllegalArgumentException("There must be at least two control points and the arrays must be of equal length.");
            }
            int N = x.length;
            this.mM = new float[N - 1];
            for (int i = 0; i < N - 1; i++) {
                this.mM[i] = (y[i + 1] - y[i]) / (x[i + 1] - x[i]);
            }
            this.mX = x;
            this.mY = y;
        }

        @Override // android.util.Spline
        public float interpolate(float x) {
            float[] fArr;
            int n = this.mX.length;
            if (Float.isNaN(x)) {
                return x;
            }
            float[] fArr2 = this.mX;
            if (x <= fArr2[0]) {
                return this.mY[0];
            }
            if (x >= fArr2[n - 1]) {
                return this.mY[n - 1];
            }
            int i = 0;
            do {
                fArr = this.mX;
                if (x >= fArr[i + 1]) {
                    i++;
                } else {
                    return this.mY[i] + (this.mM[i] * (x - fArr[i]));
                }
            } while (x != fArr[i]);
            return this.mY[i];
        }

        public String toString() {
            StringBuilder str = new StringBuilder();
            int n = this.mX.length;
            str.append("LinearSpline{[");
            for (int i = 0; i < n; i++) {
                if (i != 0) {
                    str.append(", ");
                }
                str.append(NavigationBarInflaterView.KEY_CODE_START).append(this.mX[i]);
                str.append(", ").append(this.mY[i]);
                if (i < n - 1) {
                    str.append(": ").append(this.mM[i]);
                }
                str.append(NavigationBarInflaterView.KEY_CODE_END);
            }
            str.append("]}");
            return str.toString();
        }
    }
}
