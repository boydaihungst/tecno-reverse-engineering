package android.animation.dynamicanimation;

import android.animation.dynamicanimation.AnimationHandler;
import android.animation.dynamicanimation.DynamicAnimation;
import android.os.Build;
import android.util.AndroidRuntimeException;
import android.view.View;
import android.widget.BounceEdgeEffect;
import java.util.ArrayList;
/* loaded from: classes.dex */
public abstract class DynamicAnimation<T extends DynamicAnimation<T>> implements AnimationHandler.AnimationFrameCallback {
    public static final float MIN_VISIBLE_CHANGE_ALPHA = 0.00390625f;
    public static final float MIN_VISIBLE_CHANGE_PIXELS = 1.0f;
    public static final float MIN_VISIBLE_CHANGE_ROTATION_DEGREES = 0.1f;
    public static final float MIN_VISIBLE_CHANGE_SCALE = 0.002f;
    private static final float THRESHOLD_MULTIPLIER = 0.75f;
    private static final float UNSET = Float.MAX_VALUE;
    private AnimationHandler mAnimationHandler;
    private final ArrayList<OnAnimationEndListener> mEndListeners;
    private long mLastFrameTime;
    float mMaxValue;
    float mMinValue;
    private float mMinVisibleChange;
    final FloatPropertyCompat mProperty;
    boolean mRunning;
    boolean mShouldUpdateFirstValue;
    boolean mStartValueIsSet;
    final Object mTarget;
    private final ArrayList<OnAnimationUpdateListener> mUpdateListeners;
    float mValue;
    float mVelocity;
    public static final ViewProperty TRANSLATION_X = new ViewProperty("translationX") { // from class: android.animation.dynamicanimation.DynamicAnimation.1
        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.dynamicanimation.FloatPropertyCompat
        public void setValue(View view, float value) {
            view.setTranslationX(value);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.dynamicanimation.FloatPropertyCompat
        public float getValue(View view) {
            return view.getTranslationX();
        }
    };
    public static final ViewProperty TRANSLATION_Y = new ViewProperty("translationY") { // from class: android.animation.dynamicanimation.DynamicAnimation.2
        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.dynamicanimation.FloatPropertyCompat
        public void setValue(View view, float value) {
            view.setTranslationY(value);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.dynamicanimation.FloatPropertyCompat
        public float getValue(View view) {
            return view.getTranslationY();
        }
    };
    public static final ViewProperty TRANSLATION_Z = new ViewProperty("translationZ") { // from class: android.animation.dynamicanimation.DynamicAnimation.3
        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.dynamicanimation.FloatPropertyCompat
        public void setValue(View view, float value) {
            view.setTranslationZ(value);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.dynamicanimation.FloatPropertyCompat
        public float getValue(View view) {
            return view.getTranslationZ();
        }
    };
    public static final ViewProperty SCALE_X = new ViewProperty("scaleX") { // from class: android.animation.dynamicanimation.DynamicAnimation.4
        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.dynamicanimation.FloatPropertyCompat
        public void setValue(View view, float value) {
            view.setScaleX(value);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.dynamicanimation.FloatPropertyCompat
        public float getValue(View view) {
            return view.getScaleX();
        }
    };
    public static final ViewProperty SCALE_Y = new ViewProperty("scaleY") { // from class: android.animation.dynamicanimation.DynamicAnimation.5
        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.dynamicanimation.FloatPropertyCompat
        public void setValue(View view, float value) {
            view.setScaleY(value);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.dynamicanimation.FloatPropertyCompat
        public float getValue(View view) {
            return view.getScaleY();
        }
    };
    public static final ViewProperty ROTATION = new ViewProperty("rotation") { // from class: android.animation.dynamicanimation.DynamicAnimation.6
        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.dynamicanimation.FloatPropertyCompat
        public void setValue(View view, float value) {
            view.setRotation(value);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.dynamicanimation.FloatPropertyCompat
        public float getValue(View view) {
            return view.getRotation();
        }
    };
    public static final ViewProperty ROTATION_X = new ViewProperty("rotationX") { // from class: android.animation.dynamicanimation.DynamicAnimation.7
        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.dynamicanimation.FloatPropertyCompat
        public void setValue(View view, float value) {
            view.setRotationX(value);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.dynamicanimation.FloatPropertyCompat
        public float getValue(View view) {
            return view.getRotationX();
        }
    };
    public static final ViewProperty ROTATION_Y = new ViewProperty("rotationY") { // from class: android.animation.dynamicanimation.DynamicAnimation.8
        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.dynamicanimation.FloatPropertyCompat
        public void setValue(View view, float value) {
            view.setRotationY(value);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.dynamicanimation.FloatPropertyCompat
        public float getValue(View view) {
            return view.getRotationY();
        }
    };
    public static final ViewProperty X = new ViewProperty("x") { // from class: android.animation.dynamicanimation.DynamicAnimation.9
        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.dynamicanimation.FloatPropertyCompat
        public void setValue(View view, float value) {
            view.setX(value);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.dynamicanimation.FloatPropertyCompat
        public float getValue(View view) {
            return view.getX();
        }
    };
    public static final ViewProperty Y = new ViewProperty("y") { // from class: android.animation.dynamicanimation.DynamicAnimation.10
        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.dynamicanimation.FloatPropertyCompat
        public void setValue(View view, float value) {
            view.setY(value);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.dynamicanimation.FloatPropertyCompat
        public float getValue(View view) {
            return view.getY();
        }
    };
    public static final ViewProperty Z = new ViewProperty("z") { // from class: android.animation.dynamicanimation.DynamicAnimation.11
        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.dynamicanimation.FloatPropertyCompat
        public void setValue(View view, float value) {
            if (Build.VERSION.SDK_INT >= 21) {
                view.setZ(value);
            }
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.dynamicanimation.FloatPropertyCompat
        public float getValue(View view) {
            if (Build.VERSION.SDK_INT >= 21) {
                return view.getZ();
            }
            return 0.0f;
        }
    };
    public static final ViewProperty ALPHA = new ViewProperty("alpha") { // from class: android.animation.dynamicanimation.DynamicAnimation.12
        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.dynamicanimation.FloatPropertyCompat
        public void setValue(View view, float value) {
            view.setAlpha(value);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.dynamicanimation.FloatPropertyCompat
        public float getValue(View view) {
            return view.getAlpha();
        }
    };
    public static final ViewProperty SCROLL_X = new ViewProperty("scrollX") { // from class: android.animation.dynamicanimation.DynamicAnimation.13
        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.dynamicanimation.FloatPropertyCompat
        public void setValue(View view, float value) {
            view.setScrollX((int) value);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.dynamicanimation.FloatPropertyCompat
        public float getValue(View view) {
            return view.getScrollX();
        }
    };
    public static final ViewProperty SCROLL_Y = new ViewProperty("scrollY") { // from class: android.animation.dynamicanimation.DynamicAnimation.14
        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.dynamicanimation.FloatPropertyCompat
        public void setValue(View view, float value) {
            view.setScrollY((int) value);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.dynamicanimation.FloatPropertyCompat
        public float getValue(View view) {
            return view.getScrollY();
        }
    };

    /* loaded from: classes.dex */
    static class MassState {
        float mValue;
        float mVelocity;
    }

    /* loaded from: classes.dex */
    public interface OnAnimationEndListener {
        void onAnimationEnd(DynamicAnimation dynamicAnimation, boolean z, float f, float f2);
    }

    /* loaded from: classes.dex */
    public interface OnAnimationUpdateListener {
        void onAnimationUpdate(DynamicAnimation dynamicAnimation, float f, float f2);
    }

    abstract float getAcceleration(float f, float f2);

    abstract boolean isAtEquilibrium(float f, float f2);

    abstract void setValueThreshold(float f);

    abstract boolean updateValueAndVelocity(long j);

    /* loaded from: classes.dex */
    public static abstract class ViewProperty extends FloatPropertyCompat<View> {
        private ViewProperty(String name) {
            super(name);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DynamicAnimation(final FloatValueHolder floatValueHolder) {
        this.mVelocity = 0.0f;
        this.mValue = Float.MAX_VALUE;
        this.mStartValueIsSet = false;
        this.mRunning = false;
        this.mMaxValue = Float.MAX_VALUE;
        this.mMinValue = -Float.MAX_VALUE;
        this.mLastFrameTime = 0L;
        this.mEndListeners = new ArrayList<>();
        this.mUpdateListeners = new ArrayList<>();
        this.mShouldUpdateFirstValue = false;
        this.mTarget = null;
        this.mProperty = new FloatPropertyCompat("FloatValueHolder") { // from class: android.animation.dynamicanimation.DynamicAnimation.15
            @Override // android.animation.dynamicanimation.FloatPropertyCompat
            public float getValue(Object object) {
                return floatValueHolder.getValue();
            }

            @Override // android.animation.dynamicanimation.FloatPropertyCompat
            public void setValue(Object object, float value) {
                floatValueHolder.setValue(value);
            }
        };
        this.mMinVisibleChange = 1.0f;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public <K> DynamicAnimation(K object, FloatPropertyCompat<K> property) {
        this.mVelocity = 0.0f;
        this.mValue = Float.MAX_VALUE;
        this.mStartValueIsSet = false;
        this.mRunning = false;
        this.mMaxValue = Float.MAX_VALUE;
        this.mMinValue = -Float.MAX_VALUE;
        this.mLastFrameTime = 0L;
        this.mEndListeners = new ArrayList<>();
        this.mUpdateListeners = new ArrayList<>();
        this.mShouldUpdateFirstValue = false;
        this.mTarget = object;
        this.mProperty = property;
        if (property == ROTATION || property == ROTATION_X || property == ROTATION_Y) {
            this.mMinVisibleChange = 0.1f;
        } else if (property == ALPHA) {
            this.mMinVisibleChange = 0.00390625f;
        } else if (property == SCALE_X || property == SCALE_Y) {
            this.mMinVisibleChange = 0.002f;
        } else {
            this.mMinVisibleChange = 1.0f;
        }
    }

    public T setStartValue(float startValue) {
        this.mValue = startValue;
        this.mStartValueIsSet = true;
        return this;
    }

    public T setStartVelocity(float startVelocity) {
        this.mVelocity = startVelocity;
        return this;
    }

    public T setMaxValue(float max) {
        this.mMaxValue = max;
        return this;
    }

    public T setMinValue(float min) {
        this.mMinValue = min;
        return this;
    }

    public T addEndListener(OnAnimationEndListener listener) {
        if (!this.mEndListeners.contains(listener)) {
            this.mEndListeners.add(listener);
        }
        return this;
    }

    public void removeEndListener(OnAnimationEndListener listener) {
        removeEntry(this.mEndListeners, listener);
    }

    public T addUpdateListener(OnAnimationUpdateListener listener) {
        if (isRunning()) {
            throw new UnsupportedOperationException("Error: Update listeners must be added beforethe animation.");
        }
        if (!this.mUpdateListeners.contains(listener)) {
            this.mUpdateListeners.add(listener);
        }
        return this;
    }

    public void removeUpdateListener(OnAnimationUpdateListener listener) {
        removeEntry(this.mUpdateListeners, listener);
    }

    public T setMinimumVisibleChange(float minimumVisibleChange) {
        if (minimumVisibleChange <= 0.0f) {
            throw new IllegalArgumentException("Minimum visible change must be positive.");
        }
        this.mMinVisibleChange = minimumVisibleChange;
        setValueThreshold(0.75f * minimumVisibleChange);
        return this;
    }

    public float getMinimumVisibleChange() {
        return this.mMinVisibleChange;
    }

    private static <T> void removeNullEntries(ArrayList<T> list) {
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i) == null) {
                list.remove(i);
            }
        }
    }

    private static <T> void removeEntry(ArrayList<T> list, T entry) {
        int id = list.indexOf(entry);
        if (id >= 0) {
            list.set(id, null);
        }
    }

    public void start() {
        if (!getAnimationHandler().isCurrentThread()) {
            throw new AndroidRuntimeException("Animations may only be started on the same thread as the animation handler");
        }
        if (!this.mRunning) {
            startAnimationInternal();
        }
    }

    public void cancel() {
        if (!getAnimationHandler().isCurrentThread()) {
            throw new AndroidRuntimeException("Animations may only be canceled from the same thread as the animation handler");
        }
        if (this.mRunning) {
            endAnimationInternal(true);
        }
    }

    public boolean isRunning() {
        return this.mRunning;
    }

    private void startAnimationInternal() {
        if (!this.mRunning) {
            this.mRunning = true;
            if (!this.mStartValueIsSet) {
                this.mValue = getPropertyValue();
            }
            float f = this.mValue;
            if (f > this.mMaxValue || f < this.mMinValue) {
                throw new IllegalArgumentException("Starting value need to be in between min value and max value");
            }
            getAnimationHandler().addAnimationFrameCallback(this, 0L);
        }
    }

    @Override // android.animation.dynamicanimation.AnimationHandler.AnimationFrameCallback
    public boolean doAnimationFrame(long frameTime) {
        long j = this.mLastFrameTime;
        if (j == 0) {
            this.mLastFrameTime = frameTime;
            float f = this.mValue;
            if (f == 0.0f && this.mShouldUpdateFirstValue) {
                this.mShouldUpdateFirstValue = false;
                long deltaT = 1000.0f / BounceEdgeEffect.mCurrentRefreshRate;
                boolean finished = updateValueAndVelocity(deltaT);
                float min = Math.min(this.mValue, this.mMaxValue);
                this.mValue = min;
                float max = Math.max(min, this.mMinValue);
                this.mValue = max;
                setPropertyValue(max);
                if (finished) {
                    endAnimationInternal(false);
                }
                return finished;
            }
            setPropertyValue(f);
            return false;
        }
        long deltaT2 = frameTime - j;
        this.mLastFrameTime = frameTime;
        boolean finished2 = updateValueAndVelocity(deltaT2);
        float min2 = Math.min(this.mValue, this.mMaxValue);
        this.mValue = min2;
        float max2 = Math.max(min2, this.mMinValue);
        this.mValue = max2;
        setPropertyValue(max2);
        if (finished2) {
            endAnimationInternal(false);
        }
        return finished2;
    }

    private void endAnimationInternal(boolean canceled) {
        this.mRunning = false;
        getAnimationHandler().removeCallback(this);
        this.mLastFrameTime = 0L;
        this.mStartValueIsSet = false;
        for (int i = 0; i < this.mEndListeners.size(); i++) {
            if (this.mEndListeners.get(i) != null) {
                this.mEndListeners.get(i).onAnimationEnd(this, canceled, this.mValue, this.mVelocity);
            }
        }
        removeNullEntries(this.mEndListeners);
    }

    void setPropertyValue(float value) {
        this.mProperty.setValue(this.mTarget, value);
        for (int i = 0; i < this.mUpdateListeners.size(); i++) {
            if (this.mUpdateListeners.get(i) != null) {
                this.mUpdateListeners.get(i).onAnimationUpdate(this, this.mValue, this.mVelocity);
            }
        }
        removeNullEntries(this.mUpdateListeners);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float getValueThreshold() {
        return this.mMinVisibleChange * 0.75f;
    }

    private float getPropertyValue() {
        return this.mProperty.getValue(this.mTarget);
    }

    public AnimationHandler getAnimationHandler() {
        if (this.mAnimationHandler == null) {
            this.mAnimationHandler = AnimationHandler.getInstance();
        }
        return this.mAnimationHandler;
    }

    public void setAnimationHandler(AnimationHandler animationHandler) {
        if (this.mRunning) {
            throw new AndroidRuntimeException("Animations are still running and the animationhandler should not be set at this timming");
        }
        this.mAnimationHandler = animationHandler;
    }
}
