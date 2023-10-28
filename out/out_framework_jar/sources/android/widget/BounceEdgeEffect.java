package android.widget;

import android.animation.dynamicanimation.SpringAnimation;
import android.animation.dynamicanimation.SpringForce;
import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import android.view.WindowManager;
/* loaded from: classes4.dex */
public class BounceEdgeEffect extends EdgeEffect {
    public static final int DIRECTION_BOTTOM = 3;
    public static final int DIRECTION_LEFT = 0;
    public static final int DIRECTION_RIGHT = 2;
    public static final int DIRECTION_TOP = 1;
    public static final String TAG = "BounceEdgeEffect";
    private static final float VELOCITY_MULTIPLIER = 1.0f;
    public static float mCurrentRefreshRate = 60.0f;
    private SpringAnimation mTranslationAnimation;
    private float mVelocityMultiplier;

    public BounceEdgeEffect(View view, int direction) {
        super(view.getContext());
        this.mVelocityMultiplier = 0.0f;
        mCurrentRefreshRate = ((WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRefreshRate();
        boolean isVertical = false;
        switch (direction) {
            case 0:
                this.mVelocityMultiplier = 1.0f;
                isVertical = false;
                break;
            case 1:
                this.mVelocityMultiplier = 1.0f;
                isVertical = true;
                break;
            case 2:
                this.mVelocityMultiplier = -1.0f;
                isVertical = false;
                break;
            case 3:
                this.mVelocityMultiplier = -1.0f;
                isVertical = true;
                break;
        }
        if (isVertical) {
            this.mTranslationAnimation = new SpringAnimation(view, SpringAnimation.TRANSLATION_Y);
        } else {
            this.mTranslationAnimation = new SpringAnimation(view, SpringAnimation.TRANSLATION_X);
        }
        this.mTranslationAnimation.setSpring(new SpringForce(0.0f).setStiffness(200.0f).setDampingRatio(1.0f));
    }

    public void cancelBounceAnimation() {
        this.mTranslationAnimation.cancel();
    }

    @Override // android.widget.EdgeEffect
    public void onPull(float deltaDistance) {
    }

    @Override // android.widget.EdgeEffect
    public void onPull(float deltaDistance, float displacement) {
    }

    @Override // android.widget.EdgeEffect
    public boolean draw(Canvas canvas) {
        return false;
    }

    @Override // android.widget.EdgeEffect
    public void onRelease() {
    }

    @Override // android.widget.EdgeEffect
    public void onAbsorb(int velocity) {
        this.mTranslationAnimation.setStartVelocity(velocity * this.mVelocityMultiplier);
        this.mTranslationAnimation.startBounceAnimation();
    }
}
