package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Xfermode;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
/* loaded from: classes4.dex */
public class RoundHelper {
    private final float DEFAULT_RADIUS_DP = 12.0f;
    private boolean isCircle;
    private Context mContext;
    private int mHeight;
    private Paint mPaint;
    private Path mPath;
    private float[] mRadii;
    private float mRadiusBottomLeft;
    private float mRadiusBottomRight;
    private float mRadiusTopLeft;
    private float mRadiusTopRight;
    private RectF mRectF;
    private int mStrokeColor;
    private float[] mStrokeRadii;
    private RectF mStrokeRectF;
    private float mStrokeWidth;
    private Path mTempPath;
    private View mView;
    private int mWidth;
    private Xfermode mXfermode;

    public void init(Context context, AttributeSet attrs, View view) {
        if ((view instanceof ViewGroup) && view.getBackground() == null) {
            view.setBackgroundColor(Color.parseColor("#00000000"));
        }
        this.mContext = context;
        this.mView = view;
        this.mRadii = new float[8];
        this.mStrokeRadii = new float[8];
        this.mPaint = new Paint();
        this.mRectF = new RectF();
        this.mStrokeRectF = new RectF();
        this.mPath = new Path();
        this.mTempPath = new Path();
        this.mXfermode = new PorterDuffXfermode(Build.VERSION.SDK_INT >= 23 ? PorterDuff.Mode.DST_OUT : PorterDuff.Mode.DST_IN);
        this.mStrokeColor = -1;
        float defaultRadius = dip2px(this.mContext, 12.0f);
        this.mRadiusTopLeft = defaultRadius;
        this.mRadiusTopRight = defaultRadius;
        this.mRadiusBottomLeft = defaultRadius;
        this.mRadiusBottomRight = defaultRadius;
        this.mStrokeWidth = 0.0f;
        this.mStrokeColor = this.mStrokeColor;
        if (!this.isCircle) {
            setRadius();
        }
    }

    private void setRadius() {
        float[] fArr = this.mRadii;
        float f = this.mRadiusTopLeft;
        float f2 = this.mStrokeWidth;
        float f3 = f - f2;
        fArr[1] = f3;
        fArr[0] = f3;
        float f4 = this.mRadiusTopRight;
        float f5 = f4 - f2;
        fArr[3] = f5;
        fArr[2] = f5;
        float f6 = this.mRadiusBottomRight;
        float f7 = f6 - f2;
        fArr[5] = f7;
        fArr[4] = f7;
        float f8 = this.mRadiusBottomLeft;
        float f9 = f8 - f2;
        fArr[7] = f9;
        fArr[6] = f9;
        float[] fArr2 = this.mStrokeRadii;
        fArr2[1] = f;
        fArr2[0] = f;
        fArr2[3] = f4;
        fArr2[2] = f4;
        fArr2[5] = f6;
        fArr2[4] = f6;
        fArr2[7] = f8;
        fArr2[6] = f8;
    }

    public void onSizeChanged(int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
        if (this.isCircle) {
            float radius = ((Math.min(height, width) * 1.0f) / 2.0f) - this.mStrokeWidth;
            this.mRadiusTopLeft = radius;
            this.mRadiusTopRight = radius;
            this.mRadiusBottomRight = radius;
            this.mRadiusBottomLeft = radius;
            setRadius();
        }
        RectF rectF = this.mRectF;
        if (rectF != null) {
            rectF.set(0.0f, 0.0f, width, height);
        }
        RectF rectF2 = this.mStrokeRectF;
        if (rectF2 != null) {
            float f = this.mStrokeWidth;
            rectF2.set(f / 2.0f, f / 2.0f, width - (f / 2.0f), height - (f / 2.0f));
        }
    }

    public void preDraw(Canvas canvas) {
        canvas.saveLayer(this.mRectF, null, 31);
        float f = this.mStrokeWidth;
        if (f > 0.0f) {
            int i = this.mWidth;
            float sx = (i - (f * 2.0f)) / i;
            int i2 = this.mHeight;
            float sy = (i2 - (f * 2.0f)) / i2;
            canvas.scale(sx, sy, i / 2.0f, i2 / 2.0f);
        }
    }

    public void drawPath(Canvas canvas) {
        this.mPaint.reset();
        this.mPath.reset();
        this.mPaint.setAntiAlias(true);
        this.mPaint.setStyle(Paint.Style.FILL);
        this.mPaint.setXfermode(this.mXfermode);
        this.mPath.addRoundRect(this.mRectF, this.mRadii, Path.Direction.CCW);
        if (Build.VERSION.SDK_INT >= 23) {
            this.mTempPath.reset();
            this.mTempPath.addRect(this.mRectF, Path.Direction.CCW);
            this.mTempPath.op(this.mPath, Path.Op.DIFFERENCE);
            canvas.drawPath(this.mTempPath, this.mPaint);
        } else {
            canvas.drawPath(this.mPath, this.mPaint);
        }
        this.mPaint.setXfermode(null);
        canvas.restore();
        if (this.mStrokeWidth > 0.0f) {
            this.mPaint.setStyle(Paint.Style.STROKE);
            this.mPaint.setStrokeWidth(this.mStrokeWidth);
            this.mPaint.setColor(this.mStrokeColor);
            this.mPath.reset();
            this.mPath.addRoundRect(this.mStrokeRectF, this.mStrokeRadii, Path.Direction.CCW);
            canvas.drawPath(this.mPath, this.mPaint);
        }
    }

    public void setCircle(boolean isCircle) {
        this.isCircle = isCircle;
    }

    public void setRadius(float radiusDp) {
        Context context = this.mContext;
        if (context == null) {
            return;
        }
        float radiusPx = dip2px(context, radiusDp);
        this.mRadiusTopLeft = radiusPx;
        this.mRadiusTopRight = radiusPx;
        this.mRadiusBottomLeft = radiusPx;
        this.mRadiusBottomRight = radiusPx;
        View view = this.mView;
        if (view != null) {
            view.invalidate();
        }
    }

    public void setRadius(float radiusTopLeftDp, float radiusTopRightDp, float radiusBottomLeftDp, float radiusBottomRightDp) {
        Context context = this.mContext;
        if (context == null) {
            return;
        }
        this.mRadiusTopLeft = dip2px(context, radiusTopLeftDp);
        this.mRadiusTopRight = dip2px(this.mContext, radiusTopRightDp);
        this.mRadiusBottomLeft = dip2px(this.mContext, radiusBottomLeftDp);
        this.mRadiusBottomRight = dip2px(this.mContext, radiusBottomRightDp);
        View view = this.mView;
        if (view != null) {
            view.invalidate();
        }
    }

    public void setRadiusLeft(float radiusDp) {
        Context context = this.mContext;
        if (context == null) {
            return;
        }
        float radiusPx = dip2px(context, radiusDp);
        this.mRadiusTopLeft = radiusPx;
        this.mRadiusBottomLeft = radiusPx;
        View view = this.mView;
        if (view != null) {
            view.invalidate();
        }
    }

    public void setRadiusRight(float radiusDp) {
        Context context = this.mContext;
        if (context == null) {
            return;
        }
        float radiusPx = dip2px(context, radiusDp);
        this.mRadiusTopRight = radiusPx;
        this.mRadiusBottomRight = radiusPx;
        View view = this.mView;
        if (view != null) {
            view.invalidate();
        }
    }

    public void setRadiusTop(float radiusDp) {
        Context context = this.mContext;
        if (context == null) {
            return;
        }
        float radiusPx = dip2px(context, radiusDp);
        this.mRadiusTopLeft = radiusPx;
        this.mRadiusTopRight = radiusPx;
        View view = this.mView;
        if (view != null) {
            view.invalidate();
        }
    }

    public void setRadiusBottom(float radiusDp) {
        Context context = this.mContext;
        if (context == null) {
            return;
        }
        float radiusPx = dip2px(context, radiusDp);
        this.mRadiusBottomLeft = radiusPx;
        this.mRadiusBottomRight = radiusPx;
        View view = this.mView;
        if (view != null) {
            view.invalidate();
        }
    }

    public void setRadiusTopLeft(float radiusDp) {
        Context context = this.mContext;
        if (context == null) {
            return;
        }
        this.mRadiusTopLeft = dip2px(context, radiusDp);
        View view = this.mView;
        if (view != null) {
            view.invalidate();
        }
    }

    public void setRadiusTopRight(float radiusDp) {
        Context context = this.mContext;
        if (context == null) {
            return;
        }
        this.mRadiusTopRight = dip2px(context, radiusDp);
        View view = this.mView;
        if (view != null) {
            view.invalidate();
        }
    }

    public void setRadiusBottomLeft(float radiusDp) {
        Context context = this.mContext;
        if (context == null) {
            return;
        }
        this.mRadiusBottomLeft = dip2px(context, radiusDp);
        View view = this.mView;
        if (view != null) {
            view.invalidate();
        }
    }

    public void setRadiusBottomRight(float radiusDp) {
        Context context = this.mContext;
        if (context == null) {
            return;
        }
        this.mRadiusBottomRight = dip2px(context, radiusDp);
        View view = this.mView;
        if (view != null) {
            view.invalidate();
        }
    }

    public void setStrokeWidth(float widthDp) {
        Context context = this.mContext;
        if (context == null) {
            return;
        }
        this.mStrokeWidth = dip2px(context, widthDp);
        if (this.mView != null) {
            setRadius();
            onSizeChanged(this.mWidth, this.mHeight);
            this.mView.invalidate();
        }
    }

    public void setStrokeColor(int color) {
        this.mStrokeColor = color;
        View view = this.mView;
        if (view != null) {
            view.invalidate();
        }
    }

    public void setStrokeWidthColor(float widthDp, int color) {
        Context context = this.mContext;
        if (context == null) {
            return;
        }
        this.mStrokeWidth = dip2px(context, widthDp);
        this.mStrokeColor = color;
        if (this.mView != null) {
            setRadius();
            onSizeChanged(this.mWidth, this.mHeight);
            this.mView.invalidate();
        }
    }

    private float dip2px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return dpValue * scale;
    }
}
