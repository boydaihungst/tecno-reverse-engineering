package android.animation.dynamicanimation;

import android.animation.dynamicanimation.DynamicAnimation;
/* loaded from: classes.dex */
public final class FlingAnimation extends DynamicAnimation<FlingAnimation> {
    private final DragForce mFlingForce;

    public FlingAnimation(FloatValueHolder floatValueHolder) {
        super(floatValueHolder);
        DragForce dragForce = new DragForce();
        this.mFlingForce = dragForce;
        dragForce.setValueThreshold(getValueThreshold());
    }

    public <K> FlingAnimation(K object, FloatPropertyCompat<K> property) {
        super(object, property);
        DragForce dragForce = new DragForce();
        this.mFlingForce = dragForce;
        dragForce.setValueThreshold(getValueThreshold());
    }

    public FlingAnimation setFriction(float friction) {
        if (friction <= 0.0f) {
            throw new IllegalArgumentException("Friction must be positive");
        }
        this.mFlingForce.setFrictionScalar(friction);
        return this;
    }

    public float getFriction() {
        return this.mFlingForce.getFrictionScalar();
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // android.animation.dynamicanimation.DynamicAnimation
    public FlingAnimation setMinValue(float minValue) {
        super.setMinValue(minValue);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // android.animation.dynamicanimation.DynamicAnimation
    public FlingAnimation setMaxValue(float maxValue) {
        super.setMaxValue(maxValue);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // android.animation.dynamicanimation.DynamicAnimation
    public FlingAnimation setStartVelocity(float startVelocity) {
        super.setStartVelocity(startVelocity);
        return this;
    }

    @Override // android.animation.dynamicanimation.DynamicAnimation
    boolean updateValueAndVelocity(long deltaT) {
        DynamicAnimation.MassState state = this.mFlingForce.updateValueAndVelocity(this.mValue, this.mVelocity, deltaT);
        this.mValue = state.mValue;
        this.mVelocity = state.mVelocity;
        if (this.mValue < this.mMinValue) {
            this.mValue = this.mMinValue;
            return true;
        } else if (this.mValue <= this.mMaxValue) {
            return isAtEquilibrium(this.mValue, this.mVelocity);
        } else {
            this.mValue = this.mMaxValue;
            return true;
        }
    }

    @Override // android.animation.dynamicanimation.DynamicAnimation
    float getAcceleration(float value, float velocity) {
        return this.mFlingForce.getAcceleration(value, velocity);
    }

    @Override // android.animation.dynamicanimation.DynamicAnimation
    boolean isAtEquilibrium(float value, float velocity) {
        return value >= this.mMaxValue || value <= this.mMinValue || this.mFlingForce.isAtEquilibrium(value, velocity);
    }

    @Override // android.animation.dynamicanimation.DynamicAnimation
    void setValueThreshold(float threshold) {
        this.mFlingForce.setValueThreshold(threshold);
    }

    public float getDestination() {
        return (this.mValue - (this.mVelocity / this.mFlingForce.mFriction)) + ((Math.signum(this.mVelocity) * this.mFlingForce.mVelocityThreshold) / this.mFlingForce.mFriction);
    }

    public float getDuration() {
        float velocity = Math.signum(this.mVelocity) * this.mFlingForce.mVelocityThreshold;
        return (float) ((Math.log(velocity / this.mVelocity) * 1000.0d) / this.mFlingForce.mFriction);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class DragForce implements Force {
        private static final float DEFAULT_FRICTION = -4.2f;
        private static final float VELOCITY_THRESHOLD_MULTIPLIER = 62.5f;
        private float mFriction = DEFAULT_FRICTION;
        private final DynamicAnimation.MassState mMassState = new DynamicAnimation.MassState();
        private float mVelocityThreshold;

        DragForce() {
        }

        void setFrictionScalar(float frictionScalar) {
            this.mFriction = DEFAULT_FRICTION * frictionScalar;
        }

        float getFrictionScalar() {
            return this.mFriction / DEFAULT_FRICTION;
        }

        DynamicAnimation.MassState updateValueAndVelocity(float value, float velocity, long deltaT) {
            this.mMassState.mVelocity = (float) (velocity * Math.exp((((float) deltaT) / 1000.0f) * this.mFriction));
            DynamicAnimation.MassState massState = this.mMassState;
            massState.mValue = ((massState.mVelocity - velocity) / this.mFriction) + value;
            if (isAtEquilibrium(this.mMassState.mValue, this.mMassState.mVelocity)) {
                this.mMassState.mVelocity = 0.0f;
            }
            return this.mMassState;
        }

        @Override // android.animation.dynamicanimation.Force
        public float getAcceleration(float position, float velocity) {
            return this.mFriction * velocity;
        }

        @Override // android.animation.dynamicanimation.Force
        public boolean isAtEquilibrium(float value, float velocity) {
            return Math.abs(velocity) < this.mVelocityThreshold;
        }

        void setValueThreshold(float threshold) {
            this.mVelocityThreshold = VELOCITY_THRESHOLD_MULTIPLIER * threshold;
        }
    }
}
